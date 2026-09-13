# ADR-0006: Immutable reference images and submitted metadata snapshots

Status: Accepted within Patrick's explicit PR5 model-choice authorization
Date: 2026-09-13

## Context and DECISION REQUIRED record

Current choice: Project.rules text; no visual reference entity or citations.
Problem: changing the Bible must never change what a historical analysis saw.
Proposed alternative and recommendation: one project-owned reference UUID with an
immutable normalized image/hash, editable title/guidance, and irreversible archive.
Each submitted analysis reference captures its exact title/guidance, dimensions and
hash in a relational snapshot and AnalysisRun.context. Findings link that submitted
snapshot. Patrick explicitly delegates the minimal historical model in PR5.
Alternatives: a logical reference with image versions adds a CMS-like identity layer;
fully immutable text requires replacing images to correct wording; mutable images
without versions falsify historical evidence and are rejected.

## Decision

V5 adds visual_references, analysis_references and finding_references. No categories.
Images never update; replacing means archive and upload a new reference UUID.
Metadata edits affect future sequence analyses only. Historical finding reads and
targeted PR4 contexts use original submitted metadata and the original image/hash,
including archived references. No deletion API or file cleanup touches persisted
references; foreign keys retain historical usage. Archive cannot be reversed.

Reuse PR1 generated UUID storage directories, bounded copy, signatures and PNG
normalization. Reference UUIDs are asset directory keys, never Shot rows. Binary
bytes stay on local storage; only metadata and SHA-256 enter PostgreSQL.

Eight active references and 100 lifetime persisted references per project, enforced under the
project row lock. Submit all active references ordered by creation time and UUID;
no implicit selection engine. References share the existing 8 MiB/image, 16 MiB
aggregate and 24 MiB serialized request budget with frames. No other limits or
reasoning settings change. Reference editing never initiates inference.

## Consequences and delivery impact

Historical reads need a submitted-reference endpoint rather than current Bible
metadata. A small immutable-image trigger protects image identity and archive state;
snapshot rows are append-only. Application owners can still administer the database.
Local filesystem and DB are not one transaction; failed uploads compensate files,
abrupt termination may leave orphans. No public quota/auth/hosting claim.
This bounded implementation retains September 18 and adds no later PR scope.
