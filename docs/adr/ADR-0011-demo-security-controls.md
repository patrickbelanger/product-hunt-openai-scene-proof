# ADR-0011: Durable demo admission and bounded local security controls

Status: Accepted within Patrick's explicit PR10 approval
Date: 2026-09-15

## Context and decision

Per-project paid-run limits can be bypassed by project creation/deletion/demo reset.
Patrick approves configurable deployment-wide rolling ceilings (10/hour, 100/24h)
and a default-enabled PAID_RUNS_ENABLED switch. These count logical admitted runs,
not actual provider dispatches or money. Failures after admission consume a slot.
Film Intelligence can make at most two calls in one slot; continuity/targeted one.

V11 stores UUID/time reservations without project foreign keys or creator content.
A transaction-scoped PostgreSQL advisory lock serializes admission. Reservation and
new durable run commit in the same transaction; rollback consumes neither. Existing
run-request replay occurs before reservation/switch checks, so reload/replay never
dispatches again. All instances must share database/schema and operator settings.
Rolling windows use database wall time. No limiter state is kept only in memory.

Reservations remain through project deletion/reset and application restart. Entries
older than 24 hours are pruned on successful new admission; retained run records
continue enforcing replay indefinitely. V11 starts accounting when PR10 is installed;
historical pre-PR10 attempts are not retroactively reserved. No Retry-After is emitted:
window expiry does not promise admission under concurrent callers/operator changes.
No automatic retry/refund or monetary guarantee. Switch changes require API restart;
already executing work is not canceled and must be allowed to finish.

## Complementary local controls

Global process request buckets bound reads, writes and uploads/demo creation before
MVC/body parsing. They deliberately do not trust X-Forwarded-For or claim per-user
fairness. One multipart/demo request reaches parsing/preparation per process.
The existing media gate bounds decoders; a shared worker gate covers actual paid
workflow execution even when durable stale-run recovery has marked a worker failed.
Deletion holds this gate; reset refuses active durable runs under the admission lock.

Storage operations reject canonical aliases and link/junction redirection. A minimum
512 MiB free-space reserve covers one bounded input/spool plus extracted PNGs and
temporary normalization; it is a pressure watermark, not an atomic filesystem quota.
Do not share writable storage with untrusted processes. TOCTOU/hard-link attacks by
host administrators and multi-process filesystem coordination remain outside scope.

## Alternatives and consequences

Per-project-only controls leave cost churn open. Authentication, a billing service,
distributed queues, filesystem sandbox redesign and fair client quotas exceed PR10.
The small reservation table reuses existing database/transaction boundaries.
Process request limits reset on restart; paid limits do not. An unauthenticated
caller can exhaust shared limits or access other projects. IDs are provenance, not
authorization. Public multi-user deployment still needs a separate decision.

## Delivery impact

No new service/provider/product feature or September 18 scope change. Deterministic
tests use mocked providers and isolated schemas; PR10 never invokes live providers.
Review is required before merge. Verification and operational settings: PR10-REVIEW.
