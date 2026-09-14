# Domain

## Implemented

**Project** (`dev.sceneproof.project.Project`) is the foundation aggregate.
UUID is server-generated. Required name is trimmed and limited to 120
characters; optional description to 2,000 and rules to 8,000. Empty optional fields
persist as empty strings. Creation and update instants start equal and use microsecond
precision to round-trip through PostgreSQL unchanged. PR5 exposes rules update only,
preserving trim/bounds and updating updatedAt; no general project edit or deletion.
HTTP validation rejects blank names before persistence, and
PostgreSQL reinforces non-empty names. Projects are independently retrievable by ID.

Ordinary project creation/read has no lifecycle state machine; PR7 demo copies have
the explicit replacement/retirement identity described below. The API returns immutable
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
continuity categories including OTHER. PR2 originally required empty references;
PR5 admits zero to eight submitted reference UUIDs. The inspected-shot manifest
must exactly cover the selected sequence inputs.
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

## Implemented Reference Bible (PR5)

VisualReference belongs to one Project: server UUID, required trimmed title (120),
optional trimmed guidance (2000), immutable normalized dimensions/SHA-256, createdAt
and optional archivedAt. No categories. Binary originals/PNG stay outside PostgreSQL.
V5 forbids changes to image identity, project, dimensions/hash and creation time.
Active metadata is editable; irreversible archive freezes it too. No deletion API.
Eight active and 100 persisted lifetime references are admitted under a project row
lock. Active order is createdAt then UUID; archives remain inspectable by ID.

AnalysisReference is typed submitted context with metadata/in-memory PNG. The run
snapshot and append-only analysis_references rows retain IDs, title/guidance,
dimensions and hash, never bytes or paths. finding_references uses composite foreign
keys to link the exact finding/run/project to a submitted reference. Invalid or
duplicate citations fail; unsubmitted/cross-project DB links fail. Legacy histories
and zero-reference findings remain valid.

Historical finding reads use submitted metadata and current archivedAt only as an
annotation. Targeted PR4 uses original cited reference snapshots/image hashes,
never current replacements. Original rules and current rules are separately identified.
Edits/archive after context assembly do not alter its selected inputs.
See [ADR-0006](adr/ADR-0006-reference-bible-history.md).

## Demo template and working instances (PR7)

DemoTemplate is controlled packaged authored input, not a mutable Project. Its
explicit version and per-asset SHA-256 identify the baseline. Project has nullable
demoInstanceId/demoTemplateVersion plus demoRetired; the public `demo` object is
null on ordinary projects. Each instance has at most one current Project under a
V6 unique index. Project/reference/shot/frame UUIDs are independently allocated.

Creation publishes all authored rules, references and shots atomically. Reset retires
the old project and seeds a new one under the same instance/version. A replacement
mapping keyed by source project deduplicates repeated resets. History remains in the
old copy; new copies have no analysis/findings/actions. Retired copies are excluded
from the library but retain direct historical reads. No ordinary project can reset.
Version mismatch refuses reset rather than silently adopting new authored content.
Expected evaluation outcomes are not domain fields. See ADR-0007 for failure/locking.

## PR8 Film Understanding domain (V7 / V8)

SourceFilm is a project-owned immutable original identity/hash/size/duration, distinct
from uploaded Shot. FilmSegment links immutable source start/end and position to an
ordinary Shot with decoded Frame evidence. At most eight segments/24 frames are
created once; later understanding attempts reuse their IDs and bytes.

FilmUnderstandingRun records request/source/project identity, stage history, audio
status/hash/duration, transcription request ID, Astra IDs/usage and bounded structured
result. For new/reset demos the SourceFilm is the derived 0–36.291667-second asset;
the manifest links its hash to the immutable master and range. Since the source
start is zero, its times map directly to master time. Historical sources keep their
own recorded bytes/hash/duration and are never retroactively relabelled.
One global active run shares admission with continuity analysis; ten lifetime
attempts per project. Terminal failure/success is durable. V8 protects completed
runs and creator decisions; V7 protects source/segment/transcript evidence.

TranscriptSegment has server UUID, run/project, ordinal, text and approximate source
start/end with `OPENAI_DIARIZED_SEGMENT_ESTIMATE_SOURCE_START` provenance for new runs;
historical `WHISPER_SEGMENT_ESTIMATE_SOURCE_START` records are unchanged. No word alignment,
speaker identity or literal-event guarantee is inferred. Narrative cue IDs are stable
server-derived UUIDs within a saved run and explicitly denote model interpretation.

FilmCandidate retains the original structured proposal/evidence and a one-way
PENDING → ACCEPTED / EDITED / REJECTED creator decision. Same decision replay is
idempotent; changing a recorded decision is refused. Confirmed title/rule/scope are
separate from the proposal. Only accepted/edited candidates enter confirmed memory
(24 lifetime anchors/project); pending/rejected candidates never become rules.

Visual promotion requires confirmation and a cited frame, creates an ordinary
Reference via ReferenceService and links immutable candidate/run/source provenance.
The normal eight-active/100-lifetime reference limits apply. Metadata may later be
edited/archived without rewriting the original candidate decision. Promotion is
optional and makes no provider call.

Continuity AnalysisRun snapshots confirmed anchors and latest successful transcript/
narrative context. Findings retain selected cross-modal evidence snapshots, source/
run IDs and timestamp origins. Targeted review uses the original memory snapshot,
not later discovery. Creator context does not erase prior analysis.
Creator context does not erase prior analysis.

A recognized difference is not automatically a finding; the analysis prompt uses
rules and sequence context and asks for evidence-backed continuity problems.
