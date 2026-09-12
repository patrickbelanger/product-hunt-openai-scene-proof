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

## Planned for successive slices — no corresponding tables yet

Project → References/rules, AnalysisRuns → Findings,
IntentionalChanges and resolution history. A frame has source shot, timestamp and
storage key. Evidence links valid project frame/reference IDs, never arbitrary URLs.
An analysis snapshot records the inputs/context version used.

Candidate AnalysisRun lifecycle: queued → running → succeeded/failed. Retries create
auditable attempts; final mapping to real progress stages is defined with media/AI.
Candidate finding statuses: OPEN, RESOLVED, INTENTIONAL, DISMISSED, with prior results
preserved during supersession. Creator context does not erase prior analysis.

Findings will include category, severity, bounded confidence, expected/observed state,
explanation, affected shots, relevant references, evidence and correction prompt.
Categories in the original context remain the target vocabulary; implement only
semantics that support the actual analysis. Validate identifiers and scope before
persisting model output. A recognized difference is not automatically a finding.
