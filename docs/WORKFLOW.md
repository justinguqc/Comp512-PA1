# Project workflow

These preferences come from the user's instructions for this group assignment.

1. Implement in reviewable stages and make local Git commits for each completed stage.
2. Use TDD: establish the public test boundaries, then write and run a failing behavioral test
   before the corresponding implementation. Repeat in small vertical slices.
3. Add concise source annotations explaining each stage's meaningful changes, invariants,
   and concurrency/transport decisions. Preserve starter author attribution.
4. Update `CHANGELOG.md` with main changes, new features, verification results, and limitations
   in the same commit as each implementation stage. Keep setup and test instructions current.
5. Use CodeGraph before textual code search or file reads when `.codegraph/` exists.
   Use `codegraph sync` after code changes and `codegraph explore` / `codegraph node` to inspect.
6. Leave the report and meeting material to the user. Do not manufacture collaboration evidence
   or treat a technical changelog as a complete AI interaction log.
7. Keep the RMI client contract compatible and ensure TCP is used on both network layers.
8. Run relevant checks before committing. Distinguish passing tests from planned tests and
   record any failed check accurately. Do not commit generated classes or the local code index.
9. Commit locally; publishing, remote setup, and pushing are outside the current request.

See `IMPLEMENTATION_PLAN.md` for requirements, test proposals, and the staged implementation order.
