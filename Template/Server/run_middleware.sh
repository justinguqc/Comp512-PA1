#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
# Usage: bash run_middleware.sh Flights-host[:port] Cars-host[:port] Rooms-host[:port] [registry-port [prefix [object-port]]]
if (( $# < 3 || $# > 6 )); then
    echo "Usage: $0 Flights-host[:port] Cars-host[:port] Rooms-host[:port] [registry-port [prefix [object-port]]]" >&2
    exit 1
fi
options=()
if [[ -n "${RMI_HOSTNAME:-}" ]]; then options+=("-Djava.rmi.server.hostname=$RMI_HOSTNAME"); fi
exec java "${options[@]}" -cp . Server.RMI.RMIMiddleware \
    "$1" "$2" "$3" "${4:-${RMI_PORT:-1099}}" "${5:-${RMI_PREFIX:-group_xx_}}" "${6:-${RMI_OBJECT_PORT:-0}}"
