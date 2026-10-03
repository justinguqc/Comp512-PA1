# Implementation changelog

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
