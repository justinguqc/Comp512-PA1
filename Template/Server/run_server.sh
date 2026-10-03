#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
# RMI deployment: the host creates or joins its registry; no separate rmiregistry is needed.
# Usage: bash run_server.sh [name [registry-port [prefix [object-port]]]]
options=()
if [[ -n "${RMI_HOSTNAME:-}" ]]; then options+=("-Djava.rmi.server.hostname=$RMI_HOSTNAME"); fi
exec java "${options[@]}" -cp . Server.RMI.RMIResourceManager \
    "${1:-Server}" "${2:-${RMI_PORT:-1099}}" "${3:-${RMI_PREFIX:-group_xx_}}" "${4:-${RMI_OBJECT_PORT:-0}}"
