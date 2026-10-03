#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
exec java -cp . Server.TCP.TCPResourceManager "${1:-Flights}" "${2:-4001}"
