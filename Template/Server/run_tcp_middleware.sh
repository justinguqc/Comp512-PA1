#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
if (( $# < 4 || $# > 5 )); then
    echo "Usage: $0 listen-port Flights-host:port Cars-host:port Rooms-host:port [timeout-ms]" >&2
    exit 1
fi
exec java -cp . Server.TCP.TCPMiddleware "$@"
