# Domain

## Implemented

**Project** (`dev.sceneproof.project.Project`) is the only persisted aggregate in
foundation. UUID is server-generated. Required name is trimmed and limited to 120
characters; optional description to 2,000 and rules to 8,000. Empty optional fields
persist as empty strings. Creation and update instants start equal and use microsecond
precision to round-trip through PostgreSQL unchanged; no update/delete
API is yet exposed. HTTP validation rejects blank names before persistence, and
PostgreSQL reinforces non-empty names. Projects are independently retrievable by ID.

Project creation/read has no lifecycle state machine. The API returns immutable
views instead of JPA entities. Lists contain at most 20 projects, sorted by creation
and UUID descending. Page input is 0–10,000. Later deletion must own media cleanup.

## Planned for successive slices — no corresponding tables yet

Project → References/rules, ordered Shots → Frames, AnalysisRuns → Findings,
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
