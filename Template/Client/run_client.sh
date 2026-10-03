#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
# Same console/host/name contract; settings match the middleware registry.
exec java "-Dcomp512.rmi.port=${RMI_PORT:-1099}" "-Dcomp512.rmi.prefix=${RMI_PREFIX:-group_xx_}" \
    -cp ../Server/RMIInterface.jar:. Client.RMIClient "${1:-localhost}" "${2:-Middleware}"
