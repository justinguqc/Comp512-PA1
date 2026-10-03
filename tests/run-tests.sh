#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
mkdir -p build/tests
javac -encoding UTF-8 -d build/tests Template/Server/Server/{Interface,Common,RMI}/*.java Template/Client/Client/*.java tests/Tests/*.java
suites=(StarterTest InventoryTest MiddlewareTest BundleTest RmiIntegrationTest RmiFailureTest RmiConcurrencyTest ProcessRmiTest)
if (( $# > 0 )); then suites=("$@"); fi
for suite in "${suites[@]}"; do java -cp build/tests "Tests.$suite"; done
