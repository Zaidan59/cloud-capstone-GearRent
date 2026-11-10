#!/usr/bin/env bash
source "$(dirname "$0")/_common.sh"
"${COMPOSE[@]}" --profile observability up -d prometheus grafana
"${COMPOSE[@]}" --profile observability ps prometheus grafana
printf '%s\n' 'Prometheus: http://127.0.0.1:9090' 'Grafana: http://127.0.0.1:3000'
