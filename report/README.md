# Report draft and submission evidence

`PA1-report.tex` is a standalone English draft, organized into four planned pages.
The actual page count has not been verified: the Codex built-in compiler returned
`Unable to find standard directories for platform` on both attempts. The editable
source was opened in the built-in LaTeX editor. No PDF compilation success is claimed.

Requirements were rechecked against `COMP512-p1-2026.pdf`, especially page 4:

| Required content | Draft location |
| --- | --- |
| 3–5 pages describing architecture/design of both transports | Planned pages 1–2 |
| TCP message passing and concurrency, with most technical detail on TCP | Section 2 |
| Customer and bundle choices | Section 1 |
| RMI version selection and reasons | Section 1.3; error-handling rationale supplied by Zuojun |
| One page listing tests and troublesome update/query sequences | Planned page 3, Section 3 |
| Estimated total AI tokens | Section 4, based on exported implementation sessions |
| All code-generation conversations as attachments | `AI-conversations.json`, plus any missing exports |
| Detailed individual contributions and collaboration as the final section | Section 5; roles and two meetings filled, remaining details marked |

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

No factual placeholders remain in the LaTeX body. The team confirms the AI logs are
complete and only one Codex conversation was used; the three exports contain its main
thread and associated automatic-review records. The token estimate remains the recorded
implementation snapshot and does not include later report drafting.

Remaining submission/verification work:

- Include Yinkun's preliminary non-AI RMI version as the required alternative code attachment.
  Its submission location/file is not yet recorded here; this is not a missing report field.
- Compile the LaTeX and confirm 3–5 pages, with one dedicated test page and collaboration last.
  The built-in compiler environment error remains unresolved.
- Attach `AI-conversations.json` and the completed code alongside the final report.
- Conduct the required five-machine demo experiment and choose the actual hostnames/demo slot.
  The report accurately states that five-machine testing has not yet been performed.

Team information confirmed by Zuojun: Group 30, Zuojun Gu and Yinkun Zhou. Student IDs
are intentionally omitted at the user's request. Zuojun developed the AI-assisted version
and subsequent implementation with AI; Yinkun ran real tests and performed final code
verification. The AI version was selected for more detailed exception/error handling.
The first meeting (October 2, 21:45) covered document reading and division of work;
the second (October 3, approximately 03:00) compared RMI versions and selected the AI version.

Further confirmation: Yinkun's preliminary RMI version was developed without AI. Both
meetings lasted approximately 30 minutes and use Montreal local time. Yinkun's local
process tests all passed; five-machine experiments have not been performed. Both members
jointly wrote the report.

Style revision: shortened the report and used straightforward first-person plural English.
The four planned pages cover RMI/design, TCP, tests, and AI use plus collaboration.
Detailed queue sizes, repetitive submission advice, and duplicated explanations were removed.
Required design choices, concrete test cases, AI disclosure, and confirmed contributions remain.
The revised source was sent to the built-in compiler; the same environment error persists.

Five-machine update (October 3): TCP deployment, bundles/bills, deletion/restoration,
failed-bundle compensation, and independent Cars progress during a paused Flights request
were confirmed by the user. Details are in `../docs/FIVE_MACHINE_TEST.md`. The report
now records those results; five-host RMI remains pending. Earlier status notes above
reflect the prior state. The accepted RMI-only archive is `../releases/rmi-complete.zip`.
