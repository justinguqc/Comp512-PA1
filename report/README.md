# Report draft and submission evidence

`PA1-report.tex` is a standalone English draft, organized into five planned pages.
The actual page count has not been verified: the Codex built-in compiler returned
`Unable to find standard directories for platform` on both attempts. The editable
source was opened in the built-in LaTeX editor. No PDF compilation success is claimed.

Requirements were rechecked against `COMP512-p1-2026.pdf`, especially page 4:

| Required content | Draft location |
| --- | --- |
| 3–5 pages describing architecture/design of both transports | Planned pages 1–3 |
| TCP message passing and concurrency, with most technical detail on TCP | Sections 2–3 |
| Customer and bundle choices | Sections 1 and 3 |
| RMI version selection and reasons | Section 1.3; real comparison still needed |
| One page listing tests and troublesome update/query sequences | Planned page 4, Section 4 |
| Estimated total AI tokens | Section 5, based on exported implementation sessions |
| All code-generation conversations as attachments | `AI-conversations.json`, plus any missing exports |
| Detailed individual contributions and collaboration as the final section | Section 6; team details still needed |

The deadline stated in the supplied handout is October 5. Code is due by the demo.
Five distinct machines and both members' attendance are demo requirements; five JVMs
on localhost are only local acceptance evidence.

## Evidence and remaining questions

Implementation claims describe commit `637aebb`; accepted RMI is tagged `rmi-complete`.
Fourteen suites and the TCP launcher acceptance passed during implementation. This
report-writing pass inspected code/test sources rather than rerunning those suites.

`AI-conversations.json` converts three existing JSONL exports under `sessions/` to
a JSON document, retaining every record grouped by source filename. The originals
are unchanged. `AI-token-summary.json` sums each distinct `token_usage_record`'s
per-response `usage`, deduplicating `response_id`. It does not sum cumulative counters
or duplicate the parallel `event_msg` token counters. Cached input is part of input,
and reasoning output is part of output.

The resulting estimate is **14,763,681 tokens**: 14,678,090 input (14,190,976 cached)
and 85,591 output. The scope includes the main coding thread and two automatic-review
threads, through 2026-10-03 08:12:58 UTC. It excludes later report drafting and other
unprovided sessions. These are processed tokens, not unique text or cost estimates.

Replace every `\pending{...}` with confirmed information before submitting:

- Group number, both members' names and student IDs.
- Who independently used AI / avoided AI for RMI; dates/durations; preliminary alternative file.
- Actual RMI comparison, choice, and reasons.
- Contributions and extent for RMI, TCP, customer/bundle functionality, and report.
- Source-understanding/comparison meetings and later collaboration dates/durations or message counts.
- Completeness of AI exports and whether there were additional AI sessions.
- Any additional manual or five-host tests; otherwise keep the physical deployment limitation.

After filling the unknowns, compile and confirm 3–5 pages, with the tests fitting on one
page and collaboration remaining the final section. Attach AI logs separately from the
report body. Refresh the usage estimate if more exported sessions are included.
