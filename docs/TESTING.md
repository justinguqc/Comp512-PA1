# RMI and TCP test suites

The user confirmed the public test boundaries: client-facing `IResourceManager`, internal
inventory reserve/release, protocol encode/decode/invocation, and actual RMI/TCP/console endpoints. Tests observe return values,
bills, and availability. They do not inspect private storage. Everything runs offline on
JDK 17 with no downloaded test framework.

From the repository root:

```powershell
./tests/run-tests.ps1
./tests/run-tests.ps1 -Suites BundleTest,RmiFailureTest
```

```bash
bash tests/run-tests.sh
bash tests/run-tests.sh BundleTest RmiFailureTest
```

Both runners compile all sources and stop at the first failing suite. The compiled output
is ignored under `build/tests`. The separate-process acceptance test retains logs under
`build/rmi-process-*` or `build/tcp-process-*` and prints the directory; those logs are also ignored.

After building with Make, also exercise the actual Bash launchers:

```bash
java -cp build/tests Tests.ProcessRmiTest scripts
java -cp build/tests Tests.ProcessTcpTest scripts
```

On Windows this mode defaults to Git Bash at `C:/Program Files/Git/bin/bash.exe`; set
`PA1_BASH` to a different Bash executable if needed. It uses the classes and client interface
JAR built by Make, and cleans up its own child processes after success or failure.

| Suite | Behavior verified |
| --- | --- |
| `StarterTest` | Duplicate addition, zero-price update, reservation, reserved-item deletion rejection, bill total, documented bundle flags |
| `InventoryTest` | Customer-free inventory booking, returned price, booking replay, idempotent release, cancellation before delayed reserve, last-seat contention, negative count rejection |
| `MiddlewareTest` | Routing to independent managers, central customer ownership, duplicate customer rejection, combined bill, complete deletion, generated-ID collision avoidance |
| `BundleTest` | Repeated flight quantities, optional resources, sold-out compensation, preservation of previous reservations, invalid/empty bundles, missing customers, flights-only bundle, full deletion |
| `RmiIntegrationTest` | Registry lookups and calls through both RMI layers, receipt/bundle serialization, remote bills and deletion |
| `RmiFailureTest` | Lost reserve reply after mutation, unavailable compensation retained for retry, lost release reply, resumed deletion without double release, rejection of new bookings during incomplete deletion |
| `RmiConcurrencyTest` | An unrelated customer completes while a Flights reply is held; 12 simultaneous bookings for one customer retain every ledger entry; 100 concurrent generated IDs are distinct |
| `ProcessRmiTest` | Five separate JVMs and four independent registries, original console dispatcher, all inventory types, bundle 0/1 flags, totals, reserved-inventory rejection, deletion/restoration, unique port/prefix settings |

| `TcpProtocolTest` | All signatures and overloads, Unicode/multiline values, receipts, fragmented/coalesced frames, frame bounds |
| `TcpTransportTest` | General blocking proxy over a real inventory socket, CRUD, typed receipts, release |
| `TcpMiddlewareTest` | Real sockets on both links, central customers, repeated-flight bundles, sold-out compensation, deletion |
| `TcpConcurrencyTest` | 40 held Flights requests; independent Cars queries finish on the same and different channels; correct out-of-order replies; 12 ordered bookings for one customer |
| `TcpFailureTest` | Error after mutation, retained failed compensation, recovery/deletion, rejected method/internal operation, timeout and disconnect completion |
| `ProcessTcpTest` | Five separate JVMs, blocking original console commands, CRUD/prices, individual reservations, bundle, bills, restored inventory; optional actual Bash launcher mode |

Concurrency tests coordinate starts/delays with latches and bounded futures. Process startup
uses short readiness polling, not sleeps to infer race correctness. Failure tests wrap a real
inventory service at an exported RMI boundary: they deliberately lose replies or reject
cancellations and check public service effects after recovery.

The TDD red/green history is recorded by stage in `CHANGELOG.md`. Commits contain passing
stages rather than leaving the repository with intentionally failing tests.

The process suite proves a distributed process topology on loopback. It does not establish
that five physical lab machines, their DNS, or their firewalls are ready; follow `RUNNING.md`
and repeat the console sequence on the chosen hosts before the demo.

Verification on 2026-10-03: all 14 suites pass with JDK 17.0.11. Make server/client builds,
Bash syntax validation, Bash runner TCP smoke tests, and the five-JVM TCP Bash-launcher
scenario pass. The accepted preliminary RMI solution remains tagged `rmi-complete`.
