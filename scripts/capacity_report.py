"""Build the M8 worksheet and JPG from saved measurement results."""
import argparse
from datetime import datetime, timedelta, timezone
import json
import math
from pathlib import Path

from capacity import cost_model, cpu_millicores, memory_mib


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--results', required=True)
    parser.add_argument('--model', default='docs/capacity/cost-assumptions.json')
    parser.add_argument('--report', default='docs/capacity/REPORT.md')
    parser.add_argument('--jpg', default='docs/capacity/results/2026-11-23/baseline-summary.jpg')
    args = parser.parse_args()
    root = Path(args.results)
    records = [json.loads((root / phase / 'results.json').read_text(encoding='utf-8'))
               for phase in ['idle', 'load-2', 'load-8']]
    if not all(r['deployment_unchanged'] and not r['metrics_errors'] and r['summary']['sample_count'] >= 3
               for r in records):
        raise ValueError('Measurements changed deployment, lost metrics, or contain too few samples')
    first = records[0]
    image = first['before']['containers'][0]['image']
    if not all(r['source'] == first['source'] and r['before']['containers'][0]['image'] == image
               for r in records):
        raise ValueError('Source/image differs across phases')
    resources = first['before']['containers'][0]['resources']
    cpu_request = cpu_millicores(resources['requests']['cpu'])
    cpu_limit = cpu_millicores(resources['limits']['cpu'])
    memory_request = memory_mib(resources['requests']['memory'])
    memory_limit = memory_mib(resources['limits']['memory'])
    peak_cpu = max(r['summary']['cpu_max_m'] for r in records)
    peak_memory = max(r['summary']['memory_max_mib'] for r in records)
    memory_proposal = max(memory_request, math.ceil(peak_memory * 1.25 / 64) * 64)
    model = json.loads(Path(args.model).read_text(encoding='utf-8'))
    costs = cost_model(model)
    rows = []
    for name, record in zip(['Idle', '2 worker', '8 worker'], records):
        s = record['summary']
        p95 = '-' if s['client_p95_ms'] is None else f"{s['client_p95_ms']:.1f}"
        rows.append(f"| {name} | {record['workload']['elapsed_seconds']:.1f} | {s['attempts']} | "
                    f"{s['successful']} | {s['actual_rps']:.2f} | {p95} | {s['cpu_max_m']:.1f} | "
                    f"{s['memory_max_mib']:.1f} | {s['sample_count']} |")
    target = records[-1]['summary']
    latency_result = 'memenuhi' if target['client_p95_ms'] <= 2000 else 'belum memenuhi'
    started = datetime.fromisoformat(first['started_utc']).astimezone(timezone(timedelta(hours=7)))
    finished = datetime.fromisoformat(records[-1]['finished_utc']).astimezone(timezone(timedelta(hours=7)))
    text = f'''# Capacity dan FinOps GearRent A02

Muhammad Kevin Rasendriya, NIM 2210512104. Group A02.

## Versi dan metode

Pengujian lokal berbantuan otomasi pada laptop Muhammad Kevin Rasendriya.
RAM 16 GB. Window {started:%d-%m-%Y %H:%M:%S} sampai {finished:%H:%M:%S} WIB.
Source workload `{first['source']}`. Commit skrip pengukuran `{first['measurement_branch_commit']}`.
Image aktif `{image}`. Cluster `{first['environment']['context']}`, namespace
`{first['environment']['namespace']}`, Deployment `gearrent`, satu replika.

Ini baseline aplikasi yang sudah terpasang dengan `trust-headers=true`.
Integrasi JWT dan versi terbaru dari anggota lain perlu diukur ulang.
JSON menyimpan UID pod, imageID, resource, respons per request, timestamp, dan sampel Metrics API.
Token, password, serta body transaksi tidak disimpan dalam evidence.

Workload browsing read-only menggunakan 75% GET `/api/equipments` dan 25% GET
`/api/equipments/available`. Dua fase load masing-masing berdurasi 120 detik,
dengan 2 dan 8 worker. Setiap worker dibatasi satu request/detik, closed-loop.
Respons lambat menurunkan laju aktual. Angka worker adalah concurrency client,
dan tidak boleh ditafsirkan sebagai jumlah pengguna maksimum.

Idle diukur selama 60 detik. Metrics API diambil setiap 15 detik dan memiliki
window averaging sendiri. Puncak CPU di bawah adalah maksimum sampel rata-rata
window, sehingga lonjakan singkat dapat terlewat. Memory dari Metrics API adalah
working set. Sampel idle pertama dapat dipengaruhi warm-up pilot sebelumnya.
Satu GET inventaris dijalankan sebagai preflight di luar hitungan beban.
Fase dijalankan berurutan tanpa reset pod dan tanpa cooldown khusus.
Percentile memakai nearest rank. Latency client mencakup jaringan lokal, ingress,
API, database, dan pembacaan body. Perhitungan p95 tabel hanya memakai respons 2xx.
Status non-2xx dan kegagalan transport tetap masuk denominator seluruh percobaan.

## Worksheet hasil aktual

| Fase | Window aktual s | Attempt | 2xx | RPS aktual | p95 client ms | CPU max m | RAM max MiB | Sampel |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
{chr(10).join(rows)}

Fase 8 worker menghasilkan {target['success_percent']:.2f}% respons 2xx.
Terhadap kandidat target p95 client 2 detik, hasil fase tersebut {latency_result} target.
Latency client tertinggi fase tersebut mencapai {target['client_max_ms']:.1f} ms,
sehingga percentile yang memenuhi target tidak menjamin semua request di bawah 2 detik.
Target ini pembanding uji lokal. Hasil belum membuktikan kapasitas produksi,
beban tulis reservasi, upload foto, throughput maksimum, atau target SLO bulanan.

## Headroom dan keputusan resource

Resource aktif per pod adalah request CPU {cpu_request:g}m / RAM {memory_request:g}Mi,
limit CPU {cpu_limit:g}m / RAM {memory_limit:g}Mi.

| Ukuran | CPU | RAM |
|---|---:|---:|
| Maksimum sampel seluruh fase | {peak_cpu:.2f}m | {peak_memory:.2f}MiB |
| Pemakaian terhadap request | {peak_cpu / cpu_request * 100:.2f}% | {peak_memory / memory_request * 100:.2f}% |
| Sisa terhadap limit | {cpu_limit - peak_cpu:.2f}m | {memory_limit - peak_memory:.2f}MiB |
| Headroom terhadap limit | {(1 - peak_cpu / cpu_limit) * 100:.2f}% | {(1 - peak_memory / memory_limit) * 100:.2f}% |

Rumus headroom adalah `(limit - maksimum sampel) / limit * 100%`.
Request mengatur reservasi kapasitas scheduling. Penggunaan di atas request
masih diperbolehkan selama kondisi node memungkinkan dan limit belum tercapai.
Headroom pod tidak sama dengan kapasitas bebas seluruh node.

Usulan pengujian berikutnya adalah request memory **{memory_proposal:g}Mi**,
dihitung dari maksimum working set x 1,25 lalu dibulatkan ke kelipatan 64Mi.
Pertahankan request CPU 100m dan limits 500m/512Mi untuk uji ulang.
Usulan memory ini {'melebihi limit aktif dan membutuhkan review limit sebelum diterapkan' if memory_proposal > memory_limit else 'masih berada di bawah limit aktif'}.
Usulan belum diterapkan ke manifest agar baseline dan hasil tetap konsisten.
Mengubah request pod tidak langsung mengubah tagihan VM berukuran tetap.
Penghematan membutuhkan perubahan offering, jumlah VM, atau konsolidasi yang diuji.
Keputusan akhir menunggu startup peak, uji write/upload, JWT, soak test lebih panjang,
CPU throttling, dan pemakaian database. Sampling ini tidak mengukur throttling.

## Model biaya ilustratif

Tarif berikut merupakan angka asumsi tugas, bukan harga vendor atau tagihan aktual.
Bandingkan dengan tarif resmi atau biaya internal lab sebelum dipakai untuk anggaran.
Model satu VM {model['vm_vcpu']} vCPU, RAM {model['vm_memory_gib']} GiB,
disk {model['storage_gib']} GiB, {model['hours_per_month']} jam/bulan.
Tarif CPU Rp{model['vcpu_hour_idr']:,}/vCPU-jam, RAM Rp{model['gib_hour_idr']:,}/GiB-jam,
storage Rp{model['storage_gib_month_idr']:,}/GiB-bulan.

`biaya = jam x (vCPU x tarif_CPU + GiB_RAM x tarif_RAM) + GiB_disk x tarif_disk + biaya_tetap`

Total model adalah **Rp{costs['monthly_total_idr']:,.0f}/bulan**.
Dengan asumsi {model['monthly_successful_requests']:,} request sukses/bulan,
unit-cost adalah **Rp{costs['idr_per_1000_successful_requests']:,.0f}/1.000 request sukses**.
Ini alokasi biaya infrastruktur bulanan terhadap volume asumsi, bukan tagihan per request.
Volume bulanan belum divalidasi dan tidak diekstrapolasi sebagai kapasitas maksimum.

| Asumsi request sukses/bulan | Model Rp per 1.000 request |
|---:|---:|
| 10.000 | {costs['monthly_total_idr'] / 10000 * 1000:,.0f} |
| 100.000 | {costs['monthly_total_idr'] / 100000 * 1000:,.0f} |
| 1.000.000 | {costs['monthly_total_idr'] / 1000000 * 1000:,.0f} |

Tabel sensitivitas mengasumsikan VM yang sama, sehingga kecukupan kapasitas pada
setiap volume masih perlu diuji. Model tidak memasukkan {', '.join(model['excluded'])}.
Model VM 2 vCPU/4GiB merupakan kandidat penempatan k3s/aplikasi/database untuk lab,
bukan ukuran yang sudah dibuktikan di CloudStack. Pengukuran pod aplikasi tidak
mencakup semua komponen VM, DB, control plane, atau tooling delivery.

## Roadmap CloudStack

1. Inventaris kebutuhan seluruh node, PostgreSQL, disk, dan retensi. Satu node saat ini
   berbagi layanan lab sehingga metrik pod saja belum cukup memilih ukuran VM.
2. Pilih compute offering untuk CPU/RAM dan disk offering untuk penyimpanan persisten.
   Kandidat VM awal pada model biaya perlu dibuktikan dengan pengukuran seluruh stack.
3. Buat Linux VM, pasang k3s, deploy manifest aplikasi dengan DNS database dan Secret
   untuk lingkungan tersebut. Ganti IP Docker lokal, alamat registry, dan akses Ingress.
4. Simpan PostgreSQL pada data disk persisten. Uji backup/restore bersama pemilik 08.
   Penyimpanan foto perlu dipindahkan ke volume persisten atau object storage yang diuji.
5. Tetapkan firewall dan akses privat database, HTTPS, identitas OIDC, serta registry.
   Pisahkan tooling CI/observability pada perhitungan biaya bila ikut dimigrasikan.
6. Ulangi pengukuran beban dan pemulihan di VM. Sesuaikan offering dan budget berdasarkan
   hasil tersebut. CloudStack simulator tidak membuktikan performa VM nyata.

Pemetaan ini adalah rencana konseptual. Migrasi CloudStack belum dijalankan.
Dokumentasi compute/disk offerings menjelaskan pemisahan CPU/RAM dan disk.
[Apache CloudStack service offerings](https://docs.cloudstack.apache.org/en/latest/adminguide/service_offerings.html)

## Batas dan tindak lanjut

Hasil berlaku untuk dua endpoint baca, data demo kecil, satu pod, satu laptop,
dan kondisi lokal saat uji. Kesehatan cluster serta deployment dicek sebelum/sesudah.
Baseline tidak menyimpulkan concurrency maksimum atau efisiensi produksi.
Jangan menjalankan uji hapus pod/deployment bersamaan dengan pengukuran 09.
Ulangi fase yang sama dengan token valid setelah 06 selesai, dan catat commit/digest baru.
Jika JWT kedaluwarsa, request 401 tetap dihitung sebagai gagal dan harus dijelaskan.

Detail request dan metrik tersimpan di `results/2026-11-23/<fase>/results.json`.
JPG merupakan ringkasan visual dari data tersebut. Konfigurasi dasar Kubernetes
dan kode aplikasi tidak diubah oleh pekerjaan 09 ini.
'''
    report_path = Path(args.report)
    demo_heading = '\n\n## Demo langsung oleh Kevin'
    if report_path.exists():
        previous = report_path.read_text(encoding='utf-8')
        if demo_heading in previous:
            text += demo_heading + previous.split(demo_heading, 1)[1]
    report_path.write_text(text, encoding='utf-8')
    worksheet = {'source': first['source'], 'image': image,
                 'peak_cpu_m': peak_cpu, 'peak_memory_mib': peak_memory,
                 'cpu_limit_headroom_percent': (1 - peak_cpu / cpu_limit) * 100,
                 'memory_limit_headroom_percent': (1 - peak_memory / memory_limit) * 100,
                 'proposed_memory_request_mib': memory_proposal, 'costs': costs}
    (root / 'worksheet.json').write_text(json.dumps(worksheet, indent=2), encoding='utf-8')
    render_jpg(args.jpg, records, worksheet, started, finished)
    print(args.report)
    print(args.jpg)


def render_jpg(filename, records, sheet, started, finished):
    from PIL import Image, ImageDraw, ImageFont
    image = Image.new('RGB', (1800, 1450), '#f5f7f8')
    draw = ImageDraw.Draw(image)
    fonts = {}

    def text(x, y, value, size=27, bold=False, color='#19334a'):
        key = (size, bold)
        if key not in fonts:
            fonts[key] = ImageFont.truetype(f"C:/Windows/Fonts/{'arialbd' if bold else 'arial'}.ttf", size)
        if draw.textbbox((0, 0), value, font=fonts[key])[2] > 1670 - max(0, x - 65):
            raise ValueError('Evidence text exceeds the image width')
        draw.text((x, y), value, font=fonts[key], fill=color)

    def panel(y, height, title):
        draw.rounded_rectangle((45, y, 1755, y + height), radius=16, fill='white', outline='#cdd9df', width=2)
        text(65, y + 18, title, 31, True)

    draw.rectangle((0, 0, 1800, 158), fill='#19334a')
    text(60, 24, 'GearRent A02 | 09 Capacity & FinOps', 43, True, 'white')
    text(60, 91, f'{started:%d-%m-%Y %H:%M} - {finished:%H:%M} WIB | source {sheet["source"][:7]} | RAM 16 GB', 27, color='white')
    panel(183, 322, 'Pengukuran aktual | Kubernetes, satu replika, browsing read-only')
    columns = [70, 310, 480, 700, 920, 1130, 1390]
    labels = ['Fase', 'Detik aktual', '2xx / total', 'RPS', 'p95 ms', 'CPU max m', 'RAM max MiB']
    for x, label in zip(columns, labels):
        text(x, 252, label, 26, True)
    for i, (name, r) in enumerate(zip(['Idle', '2 worker', '8 worker'], records)):
        s = r['summary']
        vals = [name, f"{r['workload']['elapsed_seconds']:.1f}", f"{s['successful']}/{s['attempts']}",
                f"{s['actual_rps']:.2f}", '-' if s['client_p95_ms'] is None else f"{s['client_p95_ms']:.0f}",
                f"{s['cpu_max_m']:.1f}", f"{s['memory_max_mib']:.1f}"]
        for x, val in zip(columns, vals):
            text(x, 302 + i * 49, val, 29)
    text(70, 451, 'CPU/RAM dari Metrics API tiap 15 dtk. p95 client hanya respons 2xx. Rate per worker dibatasi 1 RPS.', 24)
    text(70, 480, f"Latency client tertinggi fase 8 worker: {records[-1]['summary']['client_max_ms']:.0f} ms.", 22)
    panel(530, 238, 'Headroom dan usulan pengujian berikutnya')
    text(70, 595, 'Aktif: requests 100m / 192Mi | limits 500m / 512Mi', 30)
    text(70, 643, f"Headroom limit: CPU {sheet['cpu_limit_headroom_percent']:.1f}% | memory {sheet['memory_limit_headroom_percent']:.1f}%", 30)
    text(70, 693, f"Usulan request memory {sheet['proposed_memory_request_mib']:g}Mi; CPU dan limits dipertahankan. Usulan belum diterapkan.", 27)
    panel(793, 231, 'Biaya ilustratif | asumsi tugas, bukan tarif vendor')
    text(70, 857, 'Satu VM 2 vCPU / 4 GiB, disk 20 GiB, 720 jam/bulan.', 30)
    text(70, 906, f"Model Rp{sheet['costs']['monthly_total_idr']:,.0f}/bulan; volume asumsi 100.000 request sukses/bulan.", 29)
    text(70, 955, f"Rp{sheet['costs']['idr_per_1000_successful_requests']:,.0f}/1.000 request sukses. Pajak, egress, backup, dan HA belum termasuk.", 27)
    panel(1049, 220, 'Cakupan dan batas hasil')
    text(70, 1112, 'Baseline header demo, 2 endpoint baca, satu cluster lokal. JWT dan beban tulis belum diukur.', 26)
    text(70, 1158, 'CloudStack masih rencana: Linux VM -> k3s -> workload; storage dan resource seluruh VM perlu diuji.', 26)
    text(70, 1204, 'Nilai maksimum merupakan maksimum sampel. Uji singkat belum menunjukkan kapasitas maksimum.', 26)
    text(60, 1302, 'Muhammad Kevin Rasendriya | 2210512104 | Pengujian lokal berbantuan otomasi', 26)
    text(60, 1348, 'Ringkasan hasil aktual. Scope, metode, data mentah, asumsi biaya, dan keputusan tersedia di docs/capacity.', 25)
    output = Path(filename)
    output.parent.mkdir(parents=True, exist_ok=True)
    image.save(output, quality=95, subsampling=0)


if __name__ == '__main__':
    main()
