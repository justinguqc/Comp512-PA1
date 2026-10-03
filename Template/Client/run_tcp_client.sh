#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
exec java -cp ../Server:. Client.TCPClient "${1:-localhost}" "${2:-4004}" "${3:-30000}"
