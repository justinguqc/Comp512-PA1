# Implementation changelog

## Demo command sheet and five-host RMI confirmation (2026-10-03)

- Saved TCP and standalone RMI startup commands for the five actual McGill hosts in
  `docs/DEMO_COMMANDS.md`, with shared-directory compilation, console scenarios,
  controlled TCP concurrency demonstration, and shutdown order.
- Recorded user-confirmed standalone RMI five-host startup, Help, bundle bill $310,
  and remaining inventory. RMI five-host testing ended after that scenario; deletion
  and failure compensation remain available for subsequent demonstration.
- No application code, saved RMI archive, or existing report PDF was changed.

## Five-host TCP confirmation and RMI snapshot export (2026-10-03)

- Recorded user-confirmed TCP testing on five named McGill hosts: startup, bundles/bills,
  deletion/restoration, failed-bundle compensation, and independent Cars progress while
  Flights was paused, followed by successful resumed reservation.
- Updated the report's deployment status; five-machine RMI testing remains pending.
- Exported the accepted `rmi-complete` tag (`6c17981`) as `releases/rmi-complete.zip`,
  containing RMI-only sources, eight tests, scripts, and documentation. Verified its contents.
- Added extraction/build and host-specific RMI commands. The snapshot is the accepted
  AI-assisted solution and does not replace the non-AI preliminary submission.
- Report compilation still encounters the known built-in compiler environment error;
  the existing local PDF was not modified or uploaded.

## Report style revision (2026-10-03)

- Shortened the English report into four planned pages and changed the prose to a direct,
  first-person student tone at the user's request.
- Removed repetitive submission advice and excessive implementation parameters while
  keeping both transports, TCP messaging/concurrency, customer/bundle choices, one test
  page, AI usage, and the final contributions/collaboration section.
- Source checks pass; the built-in compiler still reports the same environment error,
  so PDF layout and actual page count remain unverified.

## Report evidence confirmation (2026-10-03)

- Confirmed that the AI logs are complete and belong to one Codex conversation; clarified
  that the three exports are its main thread and associated automatic-review threads.
- Removed all remaining report placeholders. Kept the token estimate explicitly scoped
  to the exported implementation snapshot, excluding later report drafting.
- Remaining work is submission of the alternative preliminary RMI code, PDF compilation/
  layout verification, and physical five-machine demo validation rather than missing prose.

## Report team details (2026-10-03)

- Filled Group 30 and member names Zuojun Gu and Yinkun Zhou; omitted student IDs as requested.
- Recorded Zuojun's AI-assisted implementation role and Yinkun's real testing/final code
  verification responsibilities without inventing contribution percentages.
- Added the supplied RMI selection rationale: more thorough error/exception handling.
- Recorded October 2 at 21:45 document-reading/work-allocation meeting and October 3 around
  03:00 RMI-comparison/selection meeting, each approximately 30 minutes in Montreal local time.
- Confirmed Yinkun's independent RMI version used no AI; all local process tests passed,
  five-machine testing has not been performed, and both members co-authored the report.
- English LaTeX source remains editable. Compilation still fails with the same built-in
  compiler environment error; actual PDF layout/page count is not verified.

## Report draft (2026-10-03)

- At the user's subsequent request, re-read all assignment handouts, especially the report
  requirements on page 4; report work is now explicitly within scope.
- Added a standalone English LaTeX draft with five planned pages, a dedicated test page,
  technical implementation content, AI usage, and contributions/collaboration as the final section.
- Converted three existing session JSONL exports into a JSON attachment and calculated
  14,763,681 processed tokens from deduplicated per-response usage, including cached input.
  Original exports are unchanged; later report drafting is outside that snapshot.
- Marked unknown identities, contribution/meeting information, RMI comparison reasons, and
  extra session/deployment evidence for user confirmation rather than fabricating them.
- Opened the source in the built-in LaTeX editor. Both compilation attempts failed with
  `Unable to find standard directories for platform`; PDF output and actual page count
  remain unverified. No application code was changed.

## Stage 7 - TCP deployment and console acceptance (2026-10-03)

- Added standalone TCP inventory/middleware entry points and a blocking TCP console client.
  All client operations use the general service proxy and shared protocol wrapper.
- Added configurable TCP host/port/timeout launchers, shutdown cleanup, and Make/test runner
  support for both transports. The process main thread waits for shutdown; middleware
  dispatch remains asynchronous while inventory replies are outstanding.
- TDD: the five-process acceptance failed with missing TCP entry points before implementation;
  it passes after the standalone roles and client adapter were added.
- Verified all 14 suites on JDK 17.0.11, server/client Make build, Bash syntax and TCP runner
  smoke tests, plus the five-JVM TCP console scenario through the actual Bash launchers.
- Documented local/native Windows/five-host commands, general client service usage, queue/frame
  bounds, timeout/disconnect behavior, and in-memory recovery limitations. Updated the plan.
- Local RMI/TCP implementation is complete. The accepted RMI snapshot remains tagged
  `rmi-complete`; actual five-host lab connectivity still requires the user's chosen machines.
  Report and meeting work are excluded.

## Stage 6 - Asynchronous TCP services and shared coordination (2026-10-03)

- Added multiplexed TCP channels with pending request IDs, separate readers/writers, bounded
  queues, timeouts, disconnect cleanup, and no automatic mutation retries.
- Added concurrent inventory servers with four execution workers and bounded work queues.
  Middleware servers accept/read requests and enqueue responses without waiting for backends.
- Moved customer ownership, billing, bundles, compensation, and deletion into `AsyncMiddleware`.
  Ordered per-customer future chains preserve consistency without blocking an execution worker.
  RMI's `Middleware` facade adapts this same service to the existing synchronous interface.
- `Services.blocking` creates a general synchronous proxy for a client; every method uses
  `TcpChannel.invoke` and the same protocol wrapper, with no method-specific message formats.
- TDD: transport/core tests failed before implementations existed. The first full TCP bundle
  run exposed blocking entropy initialization during booking-ID creation; process identity and
  monotonic booking IDs now avoid that wait on the dispatch path.
- Passing tests cover two TCP layers, bundles/deletion, 40 held Flights requests with independent
  Cars progress, out-of-order replies on one channel, ordered customer updates, errors,
  compensation recovery, timeout, and disconnect. RMI behavior/failure/concurrency regressions pass.
- Client entry points, standalone deployment, and five-JVM TCP acceptance are the final stage.

## Stage 5 - General TCP wrapping (2026-10-03)

- Saved the partner-reviewed RMI solution as annotated tag `rmi-complete` at `6c17981`.
- Added a general typed request/response codec with protocol version, length framing,
  request IDs, operation signatures, arguments, result/error, and explicit size bounds.
- Added an interface-derived operation allowlist, including both customer-creation overloads.
  TCP uses binary data streams, not RMI or Java object deserialization.
- TDD: protocol test failed before the codec existed, then passed. Verified every interface
  signature, Unicode/multiline values, receipts, split/joined frames, and oversized-frame rejection.
- Async transport and shared asynchronous customer coordination follow in Stage 6.

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
