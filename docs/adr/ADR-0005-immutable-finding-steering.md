# ADR-0005: Immutable creator actions and superseding judgements

Status: Accepted within Patrick's explicit PR4 implementation authorization
Date: 2026-09-12

## Context and DECISION REQUIRED record

Current choice: immutable PR2 findings with OPEN status and independent analysis runs.
Problem: intent must inform judgement while retaining original evidence and reasoning.
Proposed alternative and recommendation: append-only creator actions, targeted results
and a read projection of the current finding status. Reuse AnalysisRun admission,
deduplication, usage and failure recovery for targeted work. Extend the existing port
with one typed re-evaluation operation and reuse the Responses transport.
Alternatives: rewrite findings (loses original reasoning); copy findings/evidence for
every judgement (unnecessary duplication); introduce workers/event infrastructure
(outside this slice). Patrick explicitly delegates this model choice in PR4.

## Decision

Keep each original finding and evidence immutable. An action records its original
finding/run, explanation, affected-shot scope, request UUID, timestamp and preceding
successful action. INTENTIONAL_CHANGE links a separate targeted AnalysisRun; RESOLVE
and DISMISS finish without provider work. A validated targeted result atomically
commits with its run success, superseding the previous effective judgement.
INTENT_ACCEPTED projects INTENTIONAL; ISSUE_REMAINS and INSUFFICIENT_EVIDENCE project
OPEN. Resolve projects RESOLVED, dismiss DISMISSED. Terminal findings reject new
actions; pending intent blocks concurrent actions. Failed actions change no judgement.

Scope is exactly the original affected shots; required text explains its narrative
boundary. Include original evidence and bounded immediate neighboring shots, never
rerun the entire project. Context stores identifiers/hashes, no duplicated images.
All work shares the existing global RUNNING admission limit and timeout/recovery.
Every request UUID replays its saved action; changed payload is a conflict. A failed
request never retries inference automatically, even on replay. A new explicit attempt
uses a new UUID and preserves the failed context. Network recovery uses the same UUID.

## Consequences and delivery impact

History is a small action/result journal, not a general event platform. Finding IDs
remain stable for URL/evidence selection. Original fields describe the first finding;
the current judgement is explicitly separate. Existing historical analyses coexist
and are never described as a global latest-project assessment. Database triggers
enforce action/result immutability. No worker, auth, hosting or P1 scope is introduced.
September 18 delivery target and branch order remain unchanged.
