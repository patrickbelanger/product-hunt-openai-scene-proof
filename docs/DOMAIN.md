# Domain

## Implemented

**Project** (`dev.sceneproof.project.Project`) is the foundation aggregate.
UUID is server-generated. Required name is trimmed and limited to 120
characters; optional description to 2,000 and rules to 8,000. Empty optional fields
persist as empty strings. Creation and update instants start equal and use microsecond
precision to round-trip through PostgreSQL unchanged; no update/delete
API is yet exposed. HTTP validation rejects blank names before persistence, and
PostgreSQL reinforces non-empty names. Projects are independently retrievable by ID.

Project creation/read has no lifecycle state machine. The API returns immutable
views instead of JPA entities. Lists contain at most 20 projects, sorted by creation
and UUID descending. Page input is 0–10,000. Later deletion must own media cleanup.

**Shot** is a completed import attempt, ordered from zero within a project.
The service caps attempts at 100 per project and locks the project while assigning
positions. READY contains normalized frames; FAILED contains a safe failure code
and no frames. A retry creates another attempt. Display names are sanitized
basenames, kind is IMAGE/VIDEO or UNKNOWN on failure, video duration is bounded.

**Frame** has immutable UUID, shot, position, normalized dimensions and internal
storage key. Images have one frame with no timestamp. Videos have 1–32 frames
with strictly increasing elapsed source presentation timestamps in milliseconds.
Content retrieval checks project membership. V2 enforces foreign keys, position
and timestamp uniqueness, and metadata bounds.

## Implemented continuity analysis (PR2)

AnalysisRun has UUID/project/request UUID, RUNNING → SUCCEEDED or FAILED, start/end
timestamps, fixed OPENAI/gpt-6-astra metadata, safe failure code/message, selected
shot/frame counts, optional provider IDs/token usage, summary/warnings and a bounded
context snapshot. No PENDING stage is needed for synchronous work. A repeated
project/request UUID returns the same run; an explicit new request creates a new
auditable attempt. Requests older than five minutes still RUNNING are lazily failed
on read/start. Failed runs never contain committed findings.

Finding has server UUID/run UUID, category, LOW/MEDIUM/HIGH severity, finite 0–1
confidence, title/summary, expected/observed state, explanation, correction prompt,
affected shot IDs, evidence frame IDs and OPEN status. PR2 uses the 16 requested
continuity categories including OTHER. References are empty; no reference table is
invented. The model's inspected manifest must exactly cover the selected inputs.
V3 composite foreign keys prevent cross-project shot links and enforce evidence
frame ownership by an affected shot. Every affected shot needs submitted evidence.
All findings and SUCCEEDED are atomic. Historical analyses coexist. Findings reads are paginated (20),
optionally filtered by analysis ID, with newest analysis first.

## Implemented creator steering (PR4)

FindingAction is immutable: server UUID, project/finding/original-run UUIDs, client
request UUID, INTENTIONAL_CHANGE / RESOLVE / DISMISS, required explanation (2,000),
required narrative scope (1,000), exact original affected-shot IDs, creation timestamp,
optional targeted run and preceding effective action. Inputs are trimmed. V4 composite
foreign keys tie the original finding/run/project and declared shots together; triggers
reject updates/deletes of actions, scopes and targeted results. No images are copied.
There is a maximum of 100 actions per finding and the PR2 100-run project limit.

The original finding row remains OPEN with its initial reasoning and evidence.
`finding_current_state` projects the effective status from the latest successful
action; the existing Finding API `status` returns this projection. Every other field
continues to describe the original finding. `supersedesActionId` links the preceding
effective judgement. It takes effect only when the action succeeds. A failed/pending
action never supersedes it. There are no duplicate replacement finding/evidence rows.

| Action/result | Effective state | Meaning |
| --- | --- | --- |
| Initial finding | OPEN | Original Astra concern |
| Intent pending/failed | unchanged | Creator context exists; no successful new judgement |
| INTENT_ACCEPTED | INTENTIONAL | Model found the narrative explanation coherent |
| ISSUE_REMAINS | OPEN | Model still finds a problem, with new reasoning/correction |
| INSUFFICIENT_EVIDENCE | OPEN | Uncertainty is explicit; no resolution is inferred |
| RESOLVE | RESOLVED | Creator reports correction; no model verification |
| DISMISS | DISMISSED | Creator chooses not to treat the concern; no model agreement |

Only OPEN admits actions. Pending intent prevents another action for that finding.
Terminal states do not reopen in PR4. Failed intent permits a separately confirmed
new attempt or resolve/dismiss. A repeated project/request UUID with identical action
payload returns its existing result, even after the finding becomes terminal. Changed
payload conflicts. A failed UUID never reruns inference; a new confirmed UUID is a
separate paid attempt. RUNNING recovery shares the PR2 five-minute deadline.

TargetedResult has outcome, summary, explanation, evaluatedScope, original finding
and project IDs, original affected-shot coverage, used evidence frame IDs, complete
submitted inspection manifest, remainingIssue, suggestedCorrection and schemaVersion.
The result and targeted run success commit together. The previous effective judgement
remains visible if final persistence rolls back. AnalysisRun.kind distinguishes
SEQUENCE from TARGETED. Targeted runs contain no new independent findings.

## Planned for successive slices — no corresponding tables yet

Project → References. Rules currently
remain bounded project text. Later reference evidence will link valid project
reference IDs, never arbitrary URLs.

Future queued/progress stages require explicit worker/recovery design.
Creator context does not erase prior analysis.

A recognized difference is not automatically a finding; the analysis prompt uses
rules and sequence context and asks for evidence-backed continuity problems.
