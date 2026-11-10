#!/usr/bin/env bash
set -u
printf '%s\n' '== Cloud Capstone Infrastructure Doctor =='
fail=0
check() {
  local label="$1"; shift
  printf '%-18s' "$label"
  if "$@" >/tmp/capstone-doctor.out 2>/tmp/capstone-doctor.err; then
    echo OK
    head -n 1 /tmp/capstone-doctor.out | sed 's/^/  /'
  else
    echo MISSING/FAILED
    fail=1
  fi
}
check Docker docker version
check Compose docker compose version
check Git git --version
check curl curl --version
check jq jq --version
check kubectl kubectl version --client
check k3d k3d version

echo
if docker info >/dev/null 2>&1; then echo 'Docker engine: RUNNING'; else echo 'Docker engine: NOT RUNNING'; fail=1; fi

echo
echo 'Common ports (availability before startup):'
for port in 3001 2222 5432 8081 5001 8088 8443 8082 9090 3000; do
  if command -v ss >/dev/null 2>&1 && ss -ltn 2>/dev/null | grep -q ":$port "; then
    echo "  $port: IN USE"
  else
    echo "  $port: available/not detected"
  fi
done

echo
if [ "$fail" -eq 0 ]; then echo 'Doctor result: READY'; else echo 'Doctor result: ACTION REQUIRED'; fi
exit "$fail"
