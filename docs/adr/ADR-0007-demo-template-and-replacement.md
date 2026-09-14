# ADR-0007: Authored demo template and atomic replacement copies

Status: Accepted within Patrick's explicit PR7 lifecycle-choice authorization
Date: 2026-09-13

## Context and DECISION REQUIRED record

Current choice: ordinary projects, immutable analysis/actions and immutable reference
images; no demo lifecycle. Problem: resetting in place would require deleting or
rewriting audit history and could expose a partially seeded project.
Proposed alternative and recommendation: immutable packaged authored inputs and a
normal Project working copy. Reset retires that copy and creates a new one from the
same template version. Patrick explicitly delegated the choice between transactional
reset and replacement; this implements replacement without deleting historical data.
Alternatives: destructive row rewrites violate PR4/PR5 history; one global mutable
demo cannot isolate edits; production sessions/workers are outside PR7.

## Decision

The approved `between-the-line-v1` manifest and 13 media assets live in `demo/runtime`.
Gradle packages only that directory under classpath `demo/`. The source film,
curation/provenance notes and evaluation manifest are outside runtime resources.
Asset SHA-256 values are verified before ingestion. There is no template mutation API.

V6 adds nullable instance UUID/template version and a retirement flag to Project,
a unique partial index for one current copy per instance, and a source-to-replacement
mapping. The client persists an opaque creation request UUID in sessionStorage before
POST. It is the local instance handle, not an authentication credential or public
isolation boundary. Distinct handles get distinct project/reference/shot/frame UUIDs.
Reopening a handle returns its current copy. Retired copies leave the library but
remain readable by URL, preserving their evidence and immutable action history.

POST `/api/v1/demo` creates/recovers; POST project `/demo/reset` replaces. Reset takes
no generic delete target or caller-owned asset IDs. It verifies explicit demo identity,
locks the instance and checks the current copy. The original project UUID itself
deduplicates reset: retries and concurrent resets recover the current replacement,
even after later generations. Ordinary projects are rejected. No provider operation
occurs during creation/reset. Fresh copies intentionally have no analysis or findings.

## Atomicity and bounds

One transaction advisory lock per instance serializes lifecycle operations. One
180-second Spring transaction publishes all seed metadata and the replacement link
atomically, using existing ProjectService, ReferenceService and MediaService. Reference
normalization, ingestion, FFmpeg and frame persistence are reused. Project creation
flushes before JDBC services read it. No partially prepared project is visible in the
library or through GET. Failure rolls back both retirement and seed metadata; the same
request can retry. No durable PREPARING state or job/progress system is introduced.

Unlike ordinary single-file ingestion, this bounded curated multi-asset operation
holds a database transaction while decoding. This is a deliberate local tradeoff:
eight short authored clips, five stills, unchanged decoder limits and a 180-second
transaction timeout. It is not a design for general uploads or public concurrency.
The existing decoder gate can reject simultaneous operations; retry is explicit.
Reference creation defaults to `clock_timestamp()` so successive seed references keep
authored order inside the single transaction instead of sharing a transaction timestamp.

Filesystem writes are compensated only for newly created assets whose project is
confirmed absent after failure. If commit outcome/database access is uncertain, files
are retained, never deleted speculatively. Abrupt termination can leave orphan files,
as in PR1. Retired copies deliberately retain files and consume disk; automatic
retention/deletion is not implemented. Existing immutable-history triggers remain.

## Delivery impact and limits

This delivers local repeatable creation/reset without a tenancy rewrite. PR8 can bind
the instance handle to an isolated public session without changing template identity
or replacement semantics. No public authorization, rate limit, quota or deployment is
claimed. A changed installed template version refuses to reset an older instance;
restore the original version rather than silently changing its authored baseline.
September 18 target is unchanged. Original/generated source and attribution were
explicitly approved by Patrick; the eight-shot curation decision is in DECISIONS.
