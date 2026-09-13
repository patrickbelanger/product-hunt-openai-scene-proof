# PR4 review handoff — 2026-09-12

## Branch and recovery

- Branch: `feat/p0-intentional-change-steering`.
- Base: `a821d89c4f7aa0396cf447ec9951f189d42617f2`.
- HEAD remains the base: implementation is a local, uncommitted review patch.
  No commit, push, merge or P1 branch was created by this session.
- Fetched origin, inspected worktree/branches/commits, fast-forwarded clean main
  and verified exact equality with origin/main before creating the PR4 branch.
  PR3 merge has parents `602cf1c` and `3312550`. Prior pending-review/merge documents
  now have historical context; their original handoff facts remain preserved.
- Read required product/technical docs, ADRs and recovery sections 34–44, then
  inspected actual PR2/PR3 code, migrations, contract and tests. No re-scaffolding.

## Product semantics and durable model

Difference does not imply error; creator intent does not imply model agreement.
V4 `finding_steering` adds immutable finding_actions, finding_action_shots and
targeted_results, original-finding/run composite ownership, AnalysisRun.kind and
the finding_current_state read projection. PostgreSQL triggers reject history
update/delete. Scope references existing affected shots; image bytes are never copied.

Every action has server UUID, project/finding/original-run UUIDs, action type,
required explanation, required scope, exact affected-shot set, creation timestamp,
request UUID and optional targeted run / preceding effective action. Strings are
trimmed and bounded to 2,000/1,000 characters. There are at most 100 actions/finding.

| Action or judgement | Effective status | Authority |
| --- | --- | --- |
| Original finding | OPEN | Initial model assessment |
| Intent running/failed | unchanged | No completed new judgement |
| INTENT_ACCEPTED | INTENTIONAL | Independent model re-evaluation |
| ISSUE_REMAINS | OPEN | Model still finds a problem with new reasoning/correction |
| INSUFFICIENT_EVIDENCE | OPEN | Model cannot settle the concern |
| RESOLVE | RESOLVED | Creator reports correction, without model verification |
| DISMISS | DISMISSED | Creator chooses not to treat it, without model agreement |

Only OPEN admits new actions. Pending intent blocks competing actions for the same
finding. Non-open findings do not reopen in PR4. Failed intent permits a fresh
explicit attempt or creator resolve/dismiss.

The original finding row, original reasoning and evidence links never change.
Finding API `status` is a projection of the latest successful action; all other
Finding fields remain the original assessment. `supersedesActionId` identifies the
preceding effective action, or null for the original judgement. Pending/failed
actions do not make that supersession effective. Targeted result and SUCCEEDED
commit atomically, so rollback leaves the prior judgement intact. No duplicate
replacement findings/evidence are needed; stable finding IDs preserve URLs.

## API and provider boundary

- POST `/api/v1/projects/{projectId}/findings/{findingId}/actions`: requestId, type
  (INTENTIONAL_CHANGE / RESOLVE / DISMISS), explanation, scope, affectedShotIds.
- GET the same path: immutable history, oldest first, bounded to 100 actions.
- GET the same path plus `/{actionId}`: one action and its current run/result.
- Existing findings endpoint returns effective status. Existing run DTO adds
  `kind: SEQUENCE | TARGETED`; the UI never labels a targeted run as a clean full review.
- OpenAPI is the source contract; generated TS and typed client functions travel
  with the backend change. No parallel frontend DTOs.

The existing ContinuityAnalysisPort gains `reanalyze(TargetedContext)` returning
TargetedCompletion. The Astra adapter reuses the existing bounded JDK Responses
HTTP transport/envelope handling. PR2 analyze prompt/schema and low reasoning remain
unchanged. Targeted inference pins gpt-6-astra with HIGH reasoning, store=false,
strict JSON, no tools, no streaming and no retries.

Context contains the original finding (expected/observed/explanation/correction),
all original evidence, affected shots, project rules/description, creator explanation
and narrative scope, plus the preceding successful targeted judgement when present.
Immediate READY neighbors are included in order up to eight total shots. Original
affected shots retain original evidence; neighbors use first/middle/last sampling.
At most three frames/shot and 24 total. No whole-project rerun or all-pairs comparison.
Project strings, creator explanation and image text are untrusted narrative data.
The prompt explicitly permits acceptance, disagreement and insufficient evidence.

The version-1 targeted schema requires outcome, summary, explanation, evaluatedScope,
project/original-finding IDs, exact affected-shot coverage, evidence IDs, complete
inspection manifest, remainingIssue and suggestedCorrection. Local validation rejects
invented IDs, omitted original evidence, duplicate IDs/keys, unknown fields, trailing
JSON, malformed/oversized text and invalid enum/outcome combinations. ISSUE_REMAINS
requires a remaining issue and correction; acceptance requires both empty. Refusal
or incomplete output fails safely. Durable decision/finding IDs are server-assigned.

## Idempotency, concurrency and failure

Admission persists action plus TARGETED run in one short transaction, using PR2's
global advisory lock/unique RUNNING index. No database transaction remains open
during image/provider work. All sequence/targeted inference shares one RUNNING slot,
100-run project limit, 120-second deadline and existing image/request/output bounds.
Resolve/dismiss never create an AnalysisRun or call the port.

Identical project/request UUID and payload returns the existing action. Changed
payload conflicts. Concurrent replay returns RUNNING without a second inference.
The frontend stores unresolved payload/UUID in sessionStorage before POST and uses
the same request for network recovery. A saved failed UUID is never rerun; a new
explicit confirmation/UUID authorizes a separate attempt and retains the earlier one.
There is no automatic provider retry, refresh inference or selection inference.

Provider failure retains creator context and safe code/message, IDs and usage when
available. Validation/finalization failure never changes effective finding status.
Database unavailability may prevent recording FAILED; five-minute lazy recovery
then marks interruption without replay. A provider timeout may still be billable.
Admission/persistence errors do not log creator text or raw provider diagnostics.

## UX and verification

The existing cinematic workspace remains. OPEN actions use a compact explanation /
narrative-scope form and explicit paid confirmation. Resolve/dismiss ask for an audit
note and state their distinct creator semantics. Pending/error states are real.
Current judgement is separate from the initial assessment. The history disclosure
includes explanations, scopes, outcomes, previous-judgement links, failures and old
corrections. Non-open findings remain selectable; only OPEN contributes to page
issue counts/markers. Original evidence, clipboard, URL/reload and keyboard behavior
remain available. History/selection/refresh use GET only.

| Check | Result |
| --- | --- |
| Gradle build/backend | PASS: 52 tests, 0 failures, 0 skipped |
| Vitest/RTL | PASS: 31 tests (21 existing + 10 steering) |
| Chromium | PASS: 8 tests (5 existing + 3 steering paths) |
| TypeScript/Vite build | PASS |
| OpenAPI generated drift | PASS |
| git diff --check | PASS |
| Real Astra / restart | PASS, one successful targeted inference; details below |
| Remote CI | Not run: no commit/push in this session |

The 18 new backend tests cover immutable creation/scope, all three outcomes,
supersession, evidence preservation, targeted neighbor bounds, strict parsing,
foreign IDs/coverage, refusal/incomplete/failure/timeout, durable failure/usage,
finalization rollback, resolve/dismiss, invalid transitions, cross-project isolation,
concurrent same-request admission and zero second inference on replay. Provider work
is asserted outside transactions. PR4 tests use sceneproof_steering_test and clean
only their dedicated fixture tables; normal tests never spend OpenAI credit.

Chromium proves project → original finding → intent → targeted persisted outcome →
UI/reload/history/evidence for both acceptance and issue-remains. The latter then
dismisses; a third path resolves. Contract validation, request replay, keyboard form
activation, read-only refresh, tablet layout and 390px overflow checks pass. Separate
real-result Chromium verification blocks all API writes and uses a restarted normal
jar without any OpenAI key. Desktop/tablet screenshots are local ignored artifacts.

Captures: `test-results/steering-accepted-tablet.png`,
`test-results/steering-remains-tablet.png`,
`test-results/steering-real-astra-desktop.png` and
`test-results/steering-real-astra-tablet.png`.
The final review workspace is available locally at `http://127.0.0.1:15180`,
proxying the final normal jar on 8093 (PID 76152), with no OpenAI key loaded.
The deterministic API on 8092 and stale sandbox Vite on 15178 were closed after
verification. Existing user IDE services were preserved. Named browser fixtures and
the original real smoke history remain in their respective databases/media storage.

Earlier failures are not counted as passes: sandbox JDK/browser/Docker access;
an old frontend mock missing history; an overly specific Mantine test selector;
one frontend timing failure under simultaneous builds; initial PR4 fixture pollution
of the shared test schema (cleaned after verifying all 30 rows belonged to this
session, then isolated); and a test counter increment during Mockito restubbing
(corrected using doAnswer). No product success was fabricated around these failures.

## Real Astra smoke and cost

Reused original synthetic PR2 project `dc4ee12a-b72d-43a3-a072-9a7fe091ccdf`,
finding `9e9fd139-241f-4136-a931-61d5d4c0b613`, original run
`ce4d940d-a3c0-4f26-88e2-44a108e9767c`. No new whole-sequence analysis.

The first attempt failed authentication because an inherited OPENAI_API_KEY masked
the ignored .env key. Only presence/equality booleans were inspected. Correcting
the smoke process environment and one non-inference model-access GET returned 200.
Failed action `ac72748f-083a-4fd0-a666-ad90f8fe970c`, run
`d0e8e967-1c07-42a7-b79a-48acf9137f8b`, remains in history without changing OPEN.
Its manifest was archived before an explicitly authorized new request UUID.

Successful action: `2be395df-2e9e-4891-87ca-1c7bcbcfaec5`.
Targeted run: `04beb368-a347-46c2-aceb-294861fde67f`.
Request: `363c4242-5cf2-4f67-a704-7e9bfce6f594`.
Outcome: **ISSUE_REMAINS**, effective finding remains **OPEN**.
Astra considered repainting narratively plausible, but maintained the finding because
the existing project rule still required red throughout. It explicitly distinguished
that unresolved rule from the mere absence of visible off-screen repainting. This
demonstrates independent judgement, not automatic agreement. No additional paid
call was made to seek an accepted verdict.

Two original frames, strict output/IDs validated, result/usage durably stored,
same-request replay unchanged. Original finding/evidence preserved. After stopping
and restarting the API without a provider key, GET verification still returned the
same action/result and original evidence; browser reload/history likewise worked.

Usage: 1,472 input / 1,591 output / 3,063 total tokens; 0 cached input,
1,469 cache-write and 929 reasoning tokens (already included in output).
Client smoke duration including persistence/replay checks: **31.662 seconds**;
run start-to-completion: about 31.122 seconds.
Estimated standard-rate cost: **USD $0.09794**, not a billing receipt, using the
[official model rates](https://developers.openai.com/api/docs/models/gpt-6-astra).
Authentication failure returned no usage; one successful paid inference in PR4.

## Limits and reviewer focus

- Scope is the original affected-shot set plus bounded immediate neighbor context;
  no project-wide intent propagation, reference editor, worker or full rerun.
- Original finding fields remain historical; consumers must read action history for
  the newer judgement. Independent sequence analyses are not globally reconciled.
- Terminal states cannot reopen. Failed/uncertain intent remains OPEN; user-authorized
  new attempts cost separately. Timeouts/persistence outages cannot prove zero charge.
- The model may recommend updating project rules, but rule editing remains P1; PR4
  does not silently rewrite them to secure agreement.
- DB triggers protect history updates/deletes through ordinary SQL; database-owner
  administrative TRUNCATE/drop is not a security boundary. Test cleanup is isolated.
- Review V4 ownership/projection, action admission/finalization, prompt independence,
  status-vs-original-fields contract, replay UX, and history accessibility first.

Docs synchronized: README, STATUS, DOMAIN, PRODUCT, UX, ARCHITECTURE,
ASTRA-INTEGRATION, IMPLEMENTATION-PLAN, DEVELOPMENT-PLAN, DECISIONS, historical
PR3-REVIEW and ADR index. ADR-0005 records the authorized persistence/port decision.
September 18 target unchanged. Stop for review; do not merge or start P1.
