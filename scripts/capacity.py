"""Measure one GearRent deployment with bounded, read-only API traffic."""
import argparse
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
import json
import math
from pathlib import Path
import re
import subprocess
import threading
import time
import urllib.error
import urllib.request


def cpu_millicores(value):
    match = re.fullmatch(r'([0-9.]+)(n|u|m)?', value)
    if not match:
        raise ValueError('Unsupported CPU quantity')
    return float(match[1]) * {'n': 0.000001, 'u': 0.001, 'm': 1, None: 1000}[match[2]]


def memory_mib(value):
    match = re.fullmatch(r'([0-9.]+)(Ki|Mi|Gi|Ti|K|M|G|T|k)?', value)
    if not match:
        raise ValueError('Unsupported memory quantity')
    factors = {'Ki': 1024, 'Mi': 1024**2, 'Gi': 1024**3, 'Ti': 1024**4,
               'K': 1000, 'k': 1000, 'M': 1000**2, 'G': 1000**3,
               'T': 1000**4, None: 1}
    return float(match[1]) * factors[match[2]] / 1048576


def percentile(values, rank):
    values = sorted(values)
    if not values:
        return None
    return values[max(0, math.ceil(len(values) * rank / 100) - 1)]


def cost_model(model):
    if model['monthly_successful_requests'] <= 0:
        raise ValueError('Monthly successful requests must be positive')
    total = (model['hours_per_month'] * (
        model['vm_vcpu'] * model['vcpu_hour_idr'] +
        model['vm_memory_gib'] * model['gib_hour_idr']) +
        model['storage_gib'] * model['storage_gib_month_idr'] +
        model['fixed_monthly_idr'])
    return {'monthly_total_idr': total,
            'idr_per_1000_successful_requests': total / model['monthly_successful_requests'] * 1000}


def utc_now():
    return datetime.now(timezone.utc).isoformat()


def kubectl(args, command):
    result = subprocess.run(['kubectl', '--kubeconfig', args.kubeconfig,
                             '--context', args.context, *command],
                            capture_output=True, text=True, timeout=25)
    if result.returncode:
        # Return a bounded operational error without command arguments or headers.
        raise RuntimeError(result.stderr.strip()[:400])
    return json.loads(result.stdout)


def snapshot(args):
    deployment = kubectl(args, ['-n', args.namespace, 'get', 'deployment', args.deployment, '-o', 'json'])
    pods = kubectl(args, ['-n', args.namespace, 'get', 'pods', '-l', args.selector, '-o', 'json'])
    containers = deployment['spec']['template']['spec']['containers']
    return {'deployment': args.deployment, 'namespace': args.namespace,
            'generation': deployment['metadata']['generation'],
            'replicas': deployment['spec']['replicas'],
            'available': deployment['status'].get('availableReplicas', 0),
            'containers': [{key: item.get(key) for key in ['name', 'image', 'resources']}
                           for item in containers],
            'pods': [{'name': item['metadata']['name'], 'uid': item['metadata']['uid'],
                      'containers': [{'name': c['name'], 'ready': c['ready'],
                                      'restarts': c['restartCount'], 'imageID': c.get('imageID')}
                                     for c in item['status'].get('containerStatuses', [])]}
                     for item in pods['items']]}


def sample_metrics(args):
    pods = kubectl(args, ['get', '--raw',
                          f'/apis/metrics.k8s.io/v1beta1/namespaces/{args.namespace}/pods'])
    return {'time': utc_now(), 'pods': [
        {'name': pod['metadata']['name'], 'metrics_time': pod['timestamp'], 'window': pod['window'],
         'containers': [{'name': c['name'], 'cpu_m': cpu_millicores(c['usage']['cpu']),
                         'memory_mib': memory_mib(c['usage']['memory'])}
                        for c in pod['containers']]}
        for pod in pods['items'] if pod['metadata']['name'].startswith(args.deployment + '-')]
    }


def run(args):
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=False)
    headers = {'Host': args.host}
    if args.demo_headers:
        headers.update({'X-Tenant-Id': 'tenant-a', 'X-User-Role': 'user', 'X-User-Id': 'latest-demo'})
    else:
        token = Path(args.token_file).read_text(encoding='utf-8').strip()
        if not token or '\n' in token:
            raise ValueError('Token file must contain one bearer token')
        headers['Authorization'] = 'Bearer ' + token
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    # Probe the same authenticated endpoint used for load, before starting the clock.
    request = urllib.request.Request(args.url.rstrip('/') + '/api/equipments', headers=headers)
    with opener.open(request, timeout=10) as response:
        equipment = json.load(response)
        if not isinstance(equipment, list) or not equipment:
            raise RuntimeError('Equipment probe did not return demo data')
    before = snapshot(args)
    first_metric = sample_metrics(args)
    if not first_metric['pods']:
        raise RuntimeError('No workload metrics available')
    samples, errors, requests = [first_metric], [], []
    stop = threading.Event()

    def sampler():
        while not stop.wait(args.sample_seconds):
            try:
                samples.append(sample_metrics(args))
            except Exception as error:
                errors.append({'time': utc_now(), 'kind': type(error).__name__, 'message': str(error)[:400]})

    tomorrow = (datetime.now(timezone.utc).date() + timedelta(days=1))
    routes = ['/api/equipments'] * 3 + [
        f'/api/equipments/available?startDate={tomorrow}&endDate={tomorrow + timedelta(days=2)}']
    begin = time.monotonic()
    deadline = begin + args.seconds
    started = utc_now()

    def worker(worker_id):
        sequence = worker_id
        due = begin + worker_id / max(args.workers, 1) / args.per_worker_rps
        local_opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
        rows = []
        while due < deadline:
            time.sleep(max(0, due - time.monotonic()))
            if time.monotonic() >= deadline:
                break
            path = routes[sequence % len(routes)]
            request_start = time.monotonic()
            status, failure = 0, None
            try:
                req = urllib.request.Request(args.url.rstrip('/') + path, headers=headers)
                with local_opener.open(req, timeout=args.timeout) as result:
                    result.read()
                    status = result.status
            except urllib.error.HTTPError as error:
                status = error.code
                failure = 'http_error'
            except Exception as error:
                failure = type(error).__name__
            rows.append({'offset_s': round(request_start - begin, 4), 'path': path,
                         'status': status, 'error': failure,
                         'latency_ms': round((time.monotonic() - request_start) * 1000, 3)})
            sequence += 1
            # Closed-loop bounded pacing. Slow responses lower throughput, never create a backlog.
            due = max(due + 1 / args.per_worker_rps, time.monotonic())
        return rows

    thread = threading.Thread(target=sampler, daemon=True)
    thread.start()
    try:
        with ThreadPoolExecutor(max_workers=max(1, args.workers)) as pool:
            if args.workers:
                for rows in pool.map(worker, range(args.workers)):
                    requests.extend(rows)
            else:
                time.sleep(max(0, deadline - time.monotonic()))
    finally:
        stop.set()
        thread.join(timeout=30)
    finished = time.monotonic()
    after = snapshot(args)
    successful = [r for r in requests if 200 <= r['status'] < 300]
    cpu = [sum(c['cpu_m'] for p in s['pods'] for c in p['containers']) for s in samples]
    memory = [sum(c['memory_mib'] for p in s['pods'] for c in p['containers']) for s in samples]
    result = {
        'started_utc': started, 'finished_utc': utc_now(),
        'source': args.source, 'measurement_branch_commit': subprocess.check_output(
            ['git', 'rev-parse', 'HEAD'], text=True).strip(),
        'executor': args.executor, 'scope': 'Local k3d, read-only inventory browsing',
        'environment': {'ram': '16 GB', 'context': args.context, 'namespace': args.namespace},
        'auth_mode': 'trusted_demo_headers' if args.demo_headers else 'bearer_token',
        'workload': {'workers': args.workers, 'per_worker_rps_ceiling': args.per_worker_rps,
                     'configured_seconds': args.seconds, 'elapsed_seconds': round(finished - begin, 3),
                     'timeout_seconds': args.timeout, 'sample_seconds': args.sample_seconds,
                     'mix': '75% GET equipment, 25% GET available; closed-loop pacing'},
        'before': before, 'after': after,
        'deployment_unchanged': before == after,
        'requests': sorted(requests, key=lambda r: r['offset_s']),
        'metrics_samples': samples, 'metrics_errors': errors,
        'summary': {'attempts': len(requests), 'successful': len(successful),
                    'statuses': dict(Counter(str(r['status']) for r in requests)),
                    'actual_rps': round(len(requests) / (finished - begin), 4),
                    'success_percent': round(100 * len(successful) / len(requests), 4) if requests else None,
                    'client_p95_ms': percentile([r['latency_ms'] for r in successful], 95),
                    'client_max_ms': max((r['latency_ms'] for r in successful), default=None),
                    'sample_count': len(samples), 'cpu_p95_m': percentile(cpu, 95),
                    'cpu_max_m': max(cpu), 'memory_p95_mib': percentile(memory, 95),
                    'memory_max_mib': max(memory)}
    }
    (out / 'results.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
    print(json.dumps(result['summary'], indent=2))
    if not result['deployment_unchanged'] or errors:
        raise SystemExit('Run saved, but deployment changed or metrics samples failed. Review before using.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--kubeconfig', required=True)
    parser.add_argument('--context', default='k3d-gearrent-a02')
    parser.add_argument('--namespace', default='gearrent-latest')
    parser.add_argument('--deployment', default='gearrent')
    parser.add_argument('--selector', default='app=gearrent-latest')
    parser.add_argument('--url', default='http://127.0.0.1:8087')
    parser.add_argument('--host', default='gearrent.localhost')
    parser.add_argument('--source', required=True)
    parser.add_argument('--executor', required=True)
    auth = parser.add_mutually_exclusive_group(required=True)
    auth.add_argument('--demo-headers', action='store_true')
    auth.add_argument('--token-file')
    parser.add_argument('--seconds', type=int, default=120)
    parser.add_argument('--workers', type=int, default=2)
    parser.add_argument('--per-worker-rps', type=float, default=1)
    parser.add_argument('--timeout', type=float, default=5)
    parser.add_argument('--sample-seconds', type=int, default=15)
    parser.add_argument('--out', required=True)
    args = parser.parse_args()
    if args.seconds <= 0 or not 0 <= args.workers <= 32 or args.per_worker_rps <= 0 or args.sample_seconds < 1 or args.timeout <= 0:
        parser.error('Use positive timing/rate values and 0-32 workers')
    run(args)


if __name__ == '__main__':
    main()
