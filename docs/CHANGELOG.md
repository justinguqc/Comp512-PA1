# Implementation changelog

## RMI deployment and acceptance (2026-10-03)

- Replaced the middleware launch placeholder and updated inventory/client launchers to manage
  registry ports, unique prefixes, advertised hostnames, and exported object ports.
- Kept the client interface and host/name CLI intact; optional client JVM properties select
  registry port and prefix. The original console dispatcher reaches middleware without an
  architectural client rewrite.
- Updated Make targets for the new interfaces/classes, added a Bash test runner, and enforced
  LF endings for shell scripts. Added `RUNNING.md` and `TESTING.md`; updated the plan and README.
- Added real-RMI concurrency checks for independent-customer progress, same-customer updates,
  and concurrent generated IDs. Extended failure checks for retained compensation recovery.
- TDD: the five-JVM console acceptance initially failed because the client ignored the custom
  registry/prefix; it passed after the optional property configuration was added.
- Verification: all eight suites pass on Java 17; server and client Make targets succeed;
  Bash syntax checks and the Bash test runner smoke check pass. The five-JVM console scenario
  also passes through the actual Bash launchers using the Make-built interface JAR/classes.
- The first launcher-mode check exposed missing Unix utilities in the test harness's direct
  Git Bash PATH; the harness now initializes that PATH explicitly and cleans up child processes.
- This completes the local RMI milestone. Actual five-machine lab connectivity remains untested;
  TCP, report, meeting records, and the teammate's independently developed version are excluded.

## Stage 4 - Bundles and failure compensation (2026-10-03)

- Added bundles with repeated-flight quantities and optional cars/rooms. Validate flight
  numbers before mutation; publish the customer's trip only after all resources succeed.
- Shared individual/bundle acquisition logic cancels every attempted token on sold-out
  inventory or a remote failure, retaining failed cleanup for the next customer operation.
- Interrupted customer deletion blocks new bookings and bill queries until deletion is
  resumed, avoiding a misleading bill after an uncertain release.
- TDD: success test failed against the bundle stub; sold-out-later-item test then exposed
  leaked inventory; injected lost reserve reply exposed missing compensation; injected lost
  release reply exposed new bookings being accepted during incomplete deletion. Each failed
  before its corresponding correction and now passes.
- Verified repeated flights, flights-only bundles, combined bills, invalid bundles, preservation
  of pre-existing reservations, full bundled deletion, RMI serialization, and lost-reply retry.
- Compensation is not a durable distributed transaction: state and idempotency records are
  in memory, and concurrent inventory queries can observe temporary bundle acquisitions.

## Stage 3 - RMI middleware and customer lifecycle (2026-10-03)

- Added shared `Middleware` implementing the unchanged client interface. It routes Flights,
  Cars, and Rooms operations to independent managers and owns all customer records.
- Serialized customer operations with stable per-customer monitors; unrelated customers and
  inventory CRUD do not wait on a middleware-wide network lock.
- Added collision-safe generated IDs, combined bills, and customer deletion with token-based
  release. Confirmed releases are removed immediately so interrupted deletion can resume.
- Added configurable inventory/middleware RMI hosts and shared export/registry lifecycle.
  Backend endpoints accept hostname:port; binding prefixes and object ports are configurable.
- TDD: middleware tests failed before the service existed; deletion assertions failed before
  deletion was implemented; real RMI test failed before the endpoint adapters existed.
  `MiddlewareTest` and `RmiIntegrationTest` now pass, including calls through both RMI layers.
- Bundle coordination and uncertain-reservation cleanup are the next stage; TCP remains pending.

## Stage 2 - Atomic inventory service (2026-10-03)

- Added the internal `IInventoryManager` contract and serializable reservation receipts;
  reserving inventory returns quantity and booked price without a backend customer record.
- Added booking-ID deduplication and idempotent release. Cancellation tombstones prevent a
  delayed reserve from consuming inventory after its release has already arrived.
- Serialized complete backend state transitions, including additions and deletion, using
  the service monitor. No backend lock waits for another server.
- Rejected negative quantity additions to preserve availability invariants.
- TDD: inventory tests initially failed because the reserve API did not exist; release/replay
  tests then failed because release did not exist; negative-count test failed before guards.
  All now pass, including 12 simultaneous last-seat contenders and cancellation-before-reserve.
- Receipts and cancellation tombstones are in-memory and retained for the server lifetime.
  This supports retries while a backend remains alive, not recovery after backend restart.

## Stage 1 - Starter repair and behavioral test runner (2026-10-03)

- Removed the embedded U+0003 that prevented the supplied console from compiling.
- Added deterministic bill ordering and a long-valued total; retained the starter's
  latest-price convention for grouped reservations.
- Reconciled bundle flags: accept 0/1, Y/N, and true/false; reject invalid flags.
- Added an offline Java behavioral test runner for PowerShell. Tests use public boundaries
  confirmed by the user: client service, internal inventory API, and real RMI/console.
- TDD evidence: initial run failed compilation; after the repair the bill-total assertion
  failed; after adding totals it passed. The documented-flags assertion then failed and
  passed after the parser correction. `StarterTest` now passes on Java 17.
- Middleware and inventory distribution follow in the next stages.

## Stage 0 - Inspection and planning (2026-10-03)

### Changes

- Initialized a local Git repository on `main`; preserved assignment handouts, logging
  examples, and untouched starter sources in commit `3021762`.
- Added ignore rules for Java build outputs, the CodeGraph database, scratch files, and editor files.
- Initialized CodeGraph and mapped all 15 Java files using its CLI.
- Added the requirements matrix, starter findings, architecture proposal, staged implementation
  sequence, behavioral test proposal, and deployment acceptance conditions.
- Recorded the requested workflow in `WORKFLOW.md`.

### Verification

- Read all three primary handouts and reviewed rendered pages.
- Compiled all server sources successfully with Java 17.0.11 into ignored `build/baseline/`.
- Attempted a full server/client compilation: it failed at `Client.java:380` because of
  U+0003. This is an existing starter defect; it has not been changed in this planning stage.
- CodeGraph reports 15 files, 241 nodes, 540 edges, and an up-to-date index.

### Features and limitations

- No application features have been implemented and no TDD tests have been written yet.
  The current task is inspection/setup/planning; test boundaries are proposed for the next stage.
- RMI middleware, TCP, and bundles remain unimplemented; concurrency and billing issues remain.
- The baseline preserves the supplied code; report and meeting work are excluded.
