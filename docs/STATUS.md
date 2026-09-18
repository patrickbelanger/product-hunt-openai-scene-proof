# SceneProof Status

## Final Privacy/Terms cleanup — September 17, 2026

Branch remains `bugfix/continuity-analysis-cta`, from clean consolidated `dacf0dd`.
Public legal pages no longer contain TODO/draft/operator-review presentation.
Re-inspected ProjectDeletionService, V10, DemoService and MediaStorage: public copy
distinguishes ordinary DB-first deletion and retryable media cleanup/tombstones
from demo replacement with old content/media/history retained and no automatic GC.
Operational metadata wording is concise. Hosting facts stay limited to the VPS and
operator-managed infrastructure; unknown facts remain internal. OpenAI disclosures,
upload-only behavior and external-mail boundaries are preserved.

Required production inputs: `VITE_LEGAL_OPERATOR` (actual authorized public identity)
and `VITE_PRIVACY_CONTACT_EMAIL` (confirmed monitored mailbox). Patrick is configuring
privacy@lxp-technologies.com; it is not a runtime default or verified operational here.
Vite builds fail for missing/blank identity, malformed/missing email or TODO fields;
production rendering shares validation. Optional absent facts are omitted. CI/local
verification uses explicit synthetic identity/email; its bundle is not for deployment.
Production must be rebuilt with confirmed real values. No SMTP behavior is added.

Verified: focused legal/findings 37/37 (14 legal, 23 findings); final full frontend
123/123 across 13 files after the type correction; typecheck and production build
pass (existing 559.67 kB entry warning). Four production-bundle legal-route checks
pass at desktop/mobile sizes with no API/external requests, and the focused
Continuity Findings Chromium regression passes (CTA tab/POST, saved reload/evidence,
desktop/tablet/mobile). No backend unit rerun: backend is unchanged.
Initial TypeScript environment-type mismatch was corrected before final validation.
A subsequent build with blank identity/email correctly fails and names both required
variables. Verification builds use synthetic settings, not production identity.

Git diff confirms no changes to projects/Continuity Findings, backend, migrations or
API client. No live providers, new branch/worktree, push, main merge or deployment.
The privacy worktree/branch remain untouched. Internal operational and license/access
review notes remain in PRIVACY-DISCLOSURE-REVIEW; external Caddy is not modified.
Ready for the authorized local cleanup commit; exact commit and clean status are
reported in the Git handoff. Production identity/mailbox confirmation remains pending.

Exact cleanup files: `.github/workflows/ci.yml`, `README.md`,
`apps/web/src/legal/LegalPages.tsx`, `apps/web/src/legal/disclosures.ts`,
`apps/web/src/legal/legal.test.tsx`, `apps/web/vite.config.ts`,
`docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/DEPLOYMENT.md`,
`docs/IMPLEMENTATION-PLAN.md`, `docs/PRIVACY-DISCLOSURE-REVIEW.md`,
`docs/PRODUCT.md`, `docs/STATUS.md`, `docs/UX.md`, `tests/e2e/legal.spec.ts`.

Earlier consolidation/disclosure handoffs below are historical.

## Consolidated launch recovery — September 17, 2026

Final target: `bugfix/continuity-analysis-cta`. Starting main worktree was clean at
CTA commit `03b90b6`; privacy was uncommitted on `fix/privacy-terms-disclosures`
at `826bf99`. All privacy work was preserved unchanged in `59be1d9`, then integrated
into the existing target branch. Four documentation conflicts retain both subjects.
Exact initial status, preservation, conflicts and final file inventory:
[LAUNCH-RECOVERY](LAUNCH-RECOVERY.md). Prior handoffs below are historical.

Implemented together: Continuity Findings CTA via existing createAnalysis POST,
pending/duplicate protection, manual same-requestId retry and GET-only reload;
explicit Analysis complete / No continuity issues were found; new-project guidance;
Privacy Policy, Terms of Use, global Privacy/Terms/GitHub footer and all three upload
disclosures. Legal runtime files match the original privacy checkpoint unchanged.
Without a linked analysis, the existing API cannot identify old zero-finding runs;
the UI explains that limitation instead of inventing a never-run state.

Verified: 116 frontend tests / 13 files, including 23 findings and seven legal/upload
tests; typecheck, production build and OpenAPI drift pass. Production bundle direct
/privacy and /terms navigation/reload passes four desktop/mobile Chromium checks,
with no API/external requests, cookies or storage writes. Existing >500 kB warning
remains (559.87 kB entry). Backend/API/provider source is unchanged; no backend unit
rerun was needed. Final isolated Chromium regression run passes 26/26 (3.3 minutes):
continuity POST/tab placement/GET reload, Film Intelligence, legal routes, all upload
notices, evidence/steering, demo lifecycle, security and desktop/tablet/mobile flows.
Mobile findings capture was inspected with the CTA and footer visible and no clipping.
No live OpenAI/Astra/provider calls.

Initial unit run: 115/116, one stale old-copy assertion corrected before the passing
full rerun. Java/Chromium sandbox access failed and was rerun with operator access.
First executable full browser run: 25/26, one trace-cleanup ENOENT from overlapping
Playwright output directories; functional assertions passed. A subsequent attempt
was canceled when earlier sandbox-run cleanup stopped its reused Vite server.
The passing final rerun uses its own server and output directory; no assertions or timeouts
were weakened. These interrupted/failed attempts are not counted as passes.

Production gates remain: confirmed operator/privacy contact and other disclosure
settings, public shared-project exposure review, truncated LICENSE clarification,
and actual-host SPA/API routing checks. Caddy stays external; demo reset retains
old copies/media/history. See PRIVACY-DISCLOSURE-REVIEW and DEPLOYMENT.
No new branch/worktree, deletion, main merge, push or deployment is authorized.
The privacy branch/worktree remain intact. Verification is complete; the local merge
commit contains both original parents plus the verified UX/docs reconciliation.
The exact final merge hash and post-commit clean status are reported in the Git handoff.

## Continuity Findings release-blocker fix — September 16, 2026

Branch: `bugfix/continuity-analysis-cta`, from verified merged PR10 main `826bf99`.
Recovery found clean PR10 feature HEAD and stale local main/origin refs; fetching
confirmed PR #10 merged. No backend/provider/API/security code is changed.

Implemented: primary Run continuity analysis after at least one READY imported shot,
accessible synchronous loading and duplicate-click guard, same-UUID manual retry
after request failure, explicit API status/detail errors, returned analysis URL with
stale finding/page/frame cleared, and secondary GET-only Reload saved findings.
Difference != continuity error, evidence, Reference Bible and targeted steering
remain intact. Request UUID retention covers the mounted workspace (including tab
switches); full reload recovery is not introduced. No automatic or live provider calls.

Verified: 22 focused findings tests; full frontend 108 tests / 12 files with one
worker; TypeScript, Vite production build and OpenAPI drift check. Gradle build and
fresh backend regression execution pass: 152 tests, zero failures/errors/skips.
Existing Vite >500 kB warning remains.
First full frontend run alongside backend compilation had four timing/lookup
failures in unchanged project/demo tests; isolated serial rerun passed without
assertion/timeout changes. Initial test-fixture TypeScript mismatch was corrected.
Java and Chromium require operator access outside the sandbox; sandbox failures
are not passes. Chromium full suite: 21/22 pass, including UI-started continuity,
saved-only reload, evidence, references, steering, security and responsive checks.
The unchanged Film Intelligence test hit two simultaneous Reject anchor matches;
its first isolated rerun hit HTTP 500 at the shared database advisory lock while
backend tests were active. After backend completion, both Film Intelligence tests
pass in isolation without code/assertion/timeout changes. Thus all browser scenarios
have passing evidence, but the initial full run was not wholly green. Desktop/mobile
Findings captures were inspected; existing browser checks cover tablet/narrow overflow.

Verification is complete; stop for review. No commit, push or merge requested.
Earlier PR10/PR9 handoffs below are historical; PR10 is merged.

## Privacy / terms launch disclosure draft — September 17, 2026

Branch: `fix/privacy-terms-disclosures`; worktree: `.local/privacy-disclosures`.
Fetched main remains `826bf99` (PR10 merge). The previous uncommitted continuity
CTA fix stays intact on its original branch/worktree; this patch is based on main.

Implemented: public `/privacy` and `/terms` React routes, permanent Privacy / Terms /
GitHub footer, one notice per source-film/shot/new-reference upload form, and public
build-time disclosure settings with explicit TODOs. No backend/provider/API contract,
retention, security, consent or tracking changes. No live provider calls.

Patrick confirms public deployment via external `~/caddy-edge`: API proxy separate
from web-container SPA fallback. Production edge/config was not inspected here; no
Caddy/deployment config was added or changed. Local production-bundle route/reload
checks passed at desktop/mobile sizes. Email infrastructure is external; the supplied
candidate privacy mailbox is unconfirmed and intentionally not committed or published.

Verified so far: seven focused legal/upload tests; 109 frontend tests / 13 files;
TypeScript and Vite build pass (existing >500 kB chunk warning; 557.73 kB entry).
Four production-bundle Chromium direct-route/reload checks pass with no API/external
requests, cookies or storage writes. Initial new-test typing/read-timing mistakes and
the upload notice's unnecessary router dependency were corrected; full suite rerun
passes without weakening existing tests. Full deterministic browser regressions are
in progress. Backend source unchanged; backend unit suite is not rerun for this patch.

Launch disclosure blockers/TODOs: confirmed operator/privacy role and working contact,
host/provider geography/settings, retention/logs/backups and applicable legal grounds/
transfer arrangements. Main has no per-user project access isolation; public exposure
requires operator review. LICENSE appears truncated; do not invent licensing terms.
Demo reset retains old copies/media/history; cleanup is a follow-up, not implemented.
This is a practical disclosure draft, not legal advice or a compliance representation.

Evidence/configuration: [disclosure review](PRIVACY-DISCLOSURE-REVIEW.md) and
[deployment notes](DEPLOYMENT.md). Complete verification, then stop for review.
No commit, push, merge or deployment. Earlier handoffs below are historical.

## PR10 security handoff — September 15, 2026

Branch: `feat/p1-security-hardening`; base/unchanged HEAD at recovery:
`bcc7c7c06d2d18af49ef1ec93bdaa01b36c56a82` (merged PR9).
Existing uncommitted work recovered in place; nothing reset, discarded or re-scaffolded.
Security only. No provider calls, PR9 UX work, Analyze Selection or merge.

Implemented and verified: V11 durable global paid-run reservations,
default 10/hour and 100/day, PAID_RUNS_ENABLED operator switch, replay exemption;
canonical storage/alias checks, staging cleanup, disk-pressure admission, bounded
HTTP body/concurrency/rate controls, safe provider identifiers and browser headers.
One process-local worker gate also protects against overlapping stale workers and
destructive cleanup; reset refuses active runs. See PR10-REVIEW and ADR-0011.

Final verification: 152 backend tests / 18 suites, zero failures or skips; Gradle build
and bootJar pass. Frontend 102 tests / 12 files, TypeScript/Vite build and OpenAPI
generation/drift check pass. Chromium 22 flows pass, plus three security/lifecycle
flows against the restarted final API. All providers were deterministic test doubles.
npm audit reports zero advisories for 273 dependencies; backend applicability and
deferred native/deployment risks are documented in PR10-REVIEW. Final 283-file
known-key/pattern scan found no matches; no production source maps or test providers
were found in production artifacts. No absolute security or public tenancy claim.
Interrupted command was recovered as a Jackson 3 test-compilation failure and fixed;
the prior java.security error was sandbox access, not an application defect.
Java 25.0.2 verified under the operator account. No skipped check is claimed to pass.
Ready for CI/reviewer review of the operator-controlled demo scope; stop before merge.
Commit/push and clean-worktree identity are recorded in the final handoff, not here.

Below is the historical PR9 handoff, superseded by PR9 merge and current PR10 work.

Last updated: 2026-09-15
Current branch: `feat/p1-ux-polish`
Current milestone: PR9 — UX Polish + Project Lifecycle Usability (implemented, verified; awaiting review).
PR9 base: `ba7076fef8a2b70083ca434214a4c92d08ba0dc7` (merged PR8).
Recovered clean pushed checkpoint: `b5334f00be63ebc4f360299a18974d4ca6738b12`.
Current task: close focused Analyze Selection gate with documentation-only deferral; PR9 ready for merge review, no merge or PR10 start.
Existing PR9 work preserved; no restart, backend/API changes or PR8 reinterpretation.

Analyze Selection investigation at clean `2c0e3f3050e416b0cbdea8bd28b8888ac8e8d21c`:
DECISION REQUIRED — defer Analyze Selection. Existing provider/findings and sampled
frames are reusable, but immutable range/replay provenance, transcript-boundary
policy and targeted re-evaluation scope need material domain work. No new model or
transcription subsystem is inherently necessary. See ANALYZE-SELECTION-INVESTIGATION.md.
Advanced inspection remains inspection-only; Analyze Selection is a post-PR10
candidate. No runtime/test/API changes or provider calls. Documentation diff check
only for this investigation; prior verified test/build baseline below is unchanged.

Final clarity addendum resumed clean from `b64d3552877248854e8a52f53096418593d68dad`.
DONE: four-step Film Intelligence-first tour with tab-aware targets; visible source
name/fixed-state and explicit replacement guidance; collapsed Advanced inspection
with intact I/O marks; small contextual planned-feature disclosures. Removed repeated
Film Intelligence intro and put concern evidence before detailed rationale. No model
wording/evidence removed. Current capabilities remain dominant; planned text is not a CTA.
Future notes: Source Monitor ↔ Evidence Frame, Analyze Selection and Fix Assist are
explanatory only, after PR10 unless reprioritized. No playback/export/generation added.
Addendum verification: 66 affected frontend tests across eight files; six Chromium
tour/UX flows; TypeScript/Vite build pass. Entry JS 541.37 kB, existing warning only.
Desktop/tablet/mobile (1280/820/390) captures reviewed for both workflows, tour,
source details, Advanced inspection collapsed/expanded and contextual help. Native
disclosure Enter/Space verified in Chromium; unit tests use click for jsdom's native
details limitations. No full suite/backend/OpenAPI rerun for this frontend-only addendum;
prior baseline is recorded below. Zero external provider calls. No remaining addendum blockers.

DONE: real five-stage Film Understanding progress, quiet polling, results/evidence
hierarchy, source labels, keyboard inspection/local marks, project deletion and
PR10 planning. Final slice adds default Film Intelligence / Continuity Findings
tabs, import/findings purpose and empty-state guidance, selection-driven scrolling
and responsive/accessibility review. PARTIAL: none in the approved PR9 slice.
NOT STARTED: PR10 implementation and post-PR10 Analyze Selection (intentionally deferred).

Progress uses backend stages and saved timestamps, Step X of Y, completed/current/
future/failed text and symbols. Only the active stage animates; reduced motion stops
animation. Factual elapsed time only, no unsupported ETA/percentage. Quiet live age
and Syncing state do not animate Refresh now; terminal polling stops.
Both tab panels remain mounted: polling, drafts, evidence/frame and In/Out survive
tab switches. Arrow keys activate tabs; findings/analysis links open review, film
links open discovery. Clearing a findings filter stays in Findings. The guided tour
reveals its existing targets without changing selected evidence or drafts.
Import a shot explains precise frame-level inspection/focused comparison and is
not required for Film Intelligence. Empty findings explain persisted concerns,
API-started continuity analysis and existing creator actions; no invented UI action.

Timeline Left/Right = sampled frame; Shift+Left/Right = segment; Home/End, I/O and
Escape remain. Typing retains native keys. Selection changes/tab reveal scroll only
an out-of-view thumbnail by its minimal internal viewport offset. Already-visible
frames and polling/unrelated renders do not scroll. Instant movement avoids queued
animation and respects reduced motion. Marks remain inspection-only and local.
Source labeling uses actual duration, with hash-matched master/analysis provenance:
36.29s derivative versus 92.46s master; historical PR8 master validation is unchanged.
Deletion retains exact-name irreversible confirmation, demo/active-run protection,
owned relational cascade, safe owned-media cleanup and retryable pending cleanup.
Demo Reset remains the lifecycle; packaged assets are never deleted.

Pre-addendum verification: full frontend suite 101/101; TypeScript/Vite production build passes.
Pre-addendum entry JS 539.29 kB (was 531.50 kB, +1.47%); no bundle refactor.
Backend/API untouched in this slice: checkpoint full build/127 tests and OpenAPI
drift remain the baseline, not newly rerun checks. Pre-addendum full Chromium: 20/20 pass
(2.2 minutes). Final diff whitespace check passes.
Earlier 54 focused tests, then 29 final tab/timeline/tour tests passed. Browser review
fixed the legacy media-reload tab navigation assertion; a subsequent reference-image
request hit net::ERR_CONNECTION_TIMED_OUT at the local Vite proxy. Trace inspected;
no assertion weakening, retries or timeout increases were added.
Screenshots cover progress/results/concern/evidence/playhead/range/delete and both
tabs at desktop 1280, tablet 820 and narrow 390, including empty/populated findings.
Reviewed hierarchy, wrapping, focus, controls and no horizontal page overflow.
No physical-device or screen-reader certification is claimed.

Known limits: sampled frames/no playback; marks reset on reload/project remount;
dense rail ticks are supplemented by keyboard navigation and full thumbnail buttons;
no ETA/Analyze Selection; local unauthenticated deployment. Deletion DB/filesystem
cleanup is not atomic; manual retry and small durable tombstones remain.
Blockers: none. Ready for PR9 reviewer/CI approval; final commit SHA is in the Git handoff.
Next milestone: PR10 Security Hardening (HIGH), preparation only in
`docs/PR10-SECURITY-PLAN.md`. Do not start it or merge automatically.
Zero external provider calls; no generation/Fix Assist or Analyze Selection implemented.

## Historical PR8 handoff (superseded by verified merge and PR9 authorization)
Recovered exact HEAD: `f1024fb2c686d82b3e471af4c44e267840598afe`; finalization is documentation-only.
Verified implementation HEAD: `8f3c689925b537e7678d1f356900df877cb95445`. Final pushed documentation SHA is in the handoff (this file cannot contain its own commit hash).
Current task: PR8 guided-tour focus race reproduced and fixed; awaiting CI/merge review.
Next milestone: PR9 UX polish — not started.
Do not merge. Do not start PR9.

## CI guided-tour focus follow-up

The next CI failure exposed a runtime race, not an assertion to weaken: Mantine
autofocused the heading, then the tour's separate animation-frame callback could
steal focus back after Tab reached Skip. A controlled delayed-frame regression
reproduces that exact failure before the fix. Mantine now owns opening autofocus;
the tour focuses headings synchronously only when an already-open step changes.
Tab order, Back, Escape and opener restoration remain covered with real focus assertions.
No sleeps, longer timeouts or disabled assertions. Verified: 16 guided-tour tests,
79 full frontend tests with two workers, five Chromium tour tests (desktop/tablet/mobile),
TypeScript/Vite build, OpenAPI drift and diff checks. Existing 519 kB chunk warning
remains. Backend tests are unchanged and were not rerun. Browser tests used the
isolated deterministic server/schema, never OpenAI. No PR9 work or merge; CI must
confirm the pushed fix. Earlier implementation HEAD/test counts below are historical.

## CI reset-test follow-up

CI reported 77/78 frontend tests passing: a synchronous `getByRole` queried
"Reset this copy" before Mantine's animation-frame-driven modal became accessible.
Reset interactions now await `findByRole`; no sleeps, disabled animations or runtime
changes. Existing confirmation, pending/double-click and same-source retry assertions
remain. Focused demo tests: 9/9 pass; full suite with `--maxWorkers=2`: 78/78 pass;
TypeScript/Vite build passes with the existing 519 kB chunk warning. The initial
uncapped local full run had three failures in unchanged project/steering tests,
including two 5-second timeouts; it is not counted as a pass. No timeout/config change
was made. Backend/OpenAPI/Chromium were not rerun for this test-only correction.
No provider call or PR9 work; remote CI must confirm the pushed correction.

## Final live validation — authoritative current state

Run `104fed03-5210-4269-a6e8-169ced53f2ce` is SUCCEEDED: `gpt-4o-transcribe-diarize`
Audio Transcriptions → six validated timed segments → `gpt-6-astra` medium → three
entities, three narrative cues, three PENDING anchors and two concerns. It used the
historical **92.458667-second master**, proven by saved source UUID/hash/duration.
The **36.291667-second derivative** remains deterministically verified and used by
new/reset demos, but has no successful live validation. Earlier 401 and unknown
failures are historical, distinct and unchanged; the sections below record them.
No remaining demonstrated functional blocker. Merge review must accept sparse-sample,
timestamp/completeness, transcription billing and untested-live-derivative limitations.
PR9 owns refresh flashing, stage hierarchy, result/evidence density, master/source
clarity and missing deletion; no UX implementation is added here.

Retained gates: 39 focused / 122 backend tests; 78 frontend / 19 Chromium earlier
passes; Gradle/frontend builds and OpenAPI drift pass. Unchanged code means no suite
rerun in finalization; documentation links/diff and fresh OpenAPI drift checks pass.
Normal tests use no OpenAI.
Read-only API/web health checks pass; user services remain untouched. Exact IDs,
usage/cost estimate, summary/warnings and historical evidence: [final validation](PR8-FINAL-VALIDATION.md).
No provider request was made during finalization. Earlier unvalidated statements
below apply at their historical checkpoint, not to the recovered master success.

## Derived source gate and historical authorized 401

The immutable master remains 92.458667 seconds / `ac9b29c47eeb399dbd1ac5cdfcde19273e0e4e68f5df8fd67fedbef3d284a7a7`.
New/reset demo Film Intelligence uses the 0–36.291667-second derivative, SHA-256
`2205c5c0a9ddc98fe7de897bc960775539095f75ee30e567f31ccca8570de835`, with explicit
master/range provenance. Repeated generation matches exactly; matching audio and
last picture frame are verified. 39 focused / 122 full backend tests, Gradle/frontend
builds, OpenAPI drift, script syntax and diff checks pass. Prior attempt locks/history
remain intact. The one authorized validation ran at clean HEAD `8f3c689925b537e7678d1f356900df877cb95445`.
Run `87e8fdfb-7abd-4d21-ba10-4a5975a47038` prepared eight segments/24 frames, then
failed TRANSCRIPTION_REJECTED: **HTTP 401**, `invalid_request_error`, `invalid_api_key`,
request ID `req_c02bddf06cbb41a789eece448c0f6f25`. No retry, transcript or Astra call.
Classification: PROVIDER_OR_ACCOUNT_RESTRICTION (credential authentication rejection);
no inference about why that credential is invalid. Earlier unknown failures remain
unchanged. Authorization is consumed; successful live transcription/Astra remain
unverified. Correct server credentials before any separately authorized future request.
See [derived-source verification](PR8-DERIVED-SOURCE.md) for complete evidence/cost limits.

## PR8 recovery — current

Recovery began with a clean worktree; fetched origin and verified PR #7 merge with parents `1e3f28f` /
`dfc4adc`. Local main fast-forwarded to exactly
`ed6066283d27da97484425f900b6bccb447b0911`, equal to origin/main; created only the
authorized PR8 branch from that SHA. V6 and PR7 demo/reset are present. Required
recovery/product/architecture documents and existing implementation inspected.
Implemented V7/V8 source-film/run/stage/segment/transcript/candidate persistence,
FFmpeg audio/video provenance, separate purpose-built transcription/Astra discovery ports, strict schema,
creator-confirmed memory and normal Reference Bible promotion/provenance. The UI
exposes only durable real stages using polling, recovery, failures and explicit consent.
Cross-modal findings/history preserve original context. New/reset demos include the
derived analysis source with immutable master provenance, never preloaded model results.

Verified at the initial PR8 handoff: **102 backend tests**, **78 frontend tests**, **19 Chromium tests**, full
Gradle and TypeScript/Vite builds, OpenAPI generation/drift, production jar boundaries
and diff checks. Desktop/tablet/mobile PR8 screenshots inspected. Normal tests made
zero OpenAI calls. The concurrent reference-read connection-pool issue is fixed and
regression tested. The ~519 kB Vite entry chunk warning remains visible.

One real transcription attempt was rejected by OpenAI. Durable run
`f8a8960c-a396-4524-b810-5b38661ed176` failed with TRANSCRIPTION_REJECTED after source
verification and eight segments/24 frames. **Zero Astra Film Understanding calls**,
zero transcript/candidates/AI film observations. No reroll, fabricated fallback or
claimed screen-opening discovery. GET-only recovery verifies the saved failure.
The rejection's specific cause/charged amount are not established; usage is absent.
Successful real multimodal provider acceptance remains unverified. Full evidence,
cost qualifications and review focus: [PR8 review](PR8-REVIEW.md).

Patrick explicitly reconciles `docs/intial-context.md`: PR7 merged → PR8 Film
Understanding + required truthful stages → PR9 UX polish → optional P2 launch polish.
Historical/superseded `feat/p1-analysis-progress` is absorbed into PR8, never a separate
next branch. The pre-merge handoff below is historical, not a blocker.

## Historical transcription rejection review

Classification: **INSUFFICIENT_EVIDENCE**. Local PostgreSQL, saved artifacts and logs
retain only the generic rejection/request ID, not the original HTTP status/error.
Offline request-contract inspection and original-source PCM/WAV re-extraction found
no proven rejection-causing defect. Whisper availability is verified by official
documentation and Patrick's Playground, not considered a likely cause.

The confirmed observability defect is fixed: future non-2xx failures preserve actual
HTTP status, allowlisted type/code/safe message and sanitized request ID in existing
durable fields, including header metadata when body reading fails. Unknown/free-form
content is withheld; no secrets/audio/transcript or raw error body is retained.
No migration, API change, request-shape/model/reasoning/retry change or new live call.

Verified follow-up: **32 focused tests**, **115 full backend tests**, Gradle build,
OpenAPI drift and diff checks pass. Frontend/Chromium are unchanged and were not
rerun in this backend-only review; their prior 78/19 passes remain historical checks.
Original failed run and attempt lock are unchanged. The separately authorized
corrective attempt is recorded below. Real provider acceptance remains a merge risk;
see [focused review](PR8-TRANSCRIPTION-REVIEW.md).

## Historical authorized corrective transcription validation

Patrick authorized exactly one isolated transcription-only dispatch using corrected
HEAD `39009964927604cfe0a7e15e3dae99d2f4c2c909`. Clean branch/worktree, server key
presence, built adapter and bounded original-source WAV were verified before dispatch.
Validation identity: `87b0d170-3387-4959-8711-a2ef647f20ca` (not a FilmUnderstandingRun).
At `2026-09-14T11:24:13.284144300Z`, the single adapter invocation returned
**TRANSCRIPTION_UNAVAILABLE** in **423 ms**. No upstream HTTP status/request ID/error
fields were exposed; local 503 is not an OpenAI HTTP status. Receipt by OpenAI is
unconfirmed. Classification: **INSUFFICIENT_EVIDENCE**, not a proven provider rejection,
account restriction or implementation defect. No retry; authorization is consumed.

No transcript completion, segment count or timestamp validation; usage/billed cost
unknown. **Zero Astra calls**. Original failed artifacts/lock hashes are unchanged;
no database/application run was created or mutated. New isolated lock and safe
metadata-only result remain under ignored `.local/`; temporary WAV deleted.
No application code changed. No deterministic suites/builds were rerun for this
validation/documentation-only follow-up; the prior 115 backend tests/build/OpenAPI
passes remain the last applicable checks. Real transcription transport and real
Astra Film Understanding acceptance both remain unverified. Do not merge or start PR9.

## One-call timed transcription migration — current

Patrick's follow-up prefers the verified `gpt-4o-transcribe-diarize` timed-segment
contract over a proposed four-call `gpt-transcribe` fallback. Production remains
POST `/v1/audio/transcriptions`, now with `diarized_json` and `chunking_strategy=auto`.
One bounded WAV/request per film, unchanged source limits and no application retry.
No Responses/Chat transcription, local four-call orchestration or speaker authority.
V9 changes new-record defaults only; historical Whisper records/attempts stay intact.

Safe transport classification and production-path loopback capture cover request
delivery, endpoint restrictions and no-redirect behavior. The discarded historical
exception cannot be reconstructed; neither the dashboard's reported zero Audio
Transcription usage nor unrelated Astra usage establishes a specific network cause.
The historical failure classifications remain INSUFFICIENT_EVIDENCE. No new live
OpenAI request is made. [ADR-0009](adr/ADR-0009-timed-audio-transcription.md) records
the exact contract, source-time semantics, alternatives, pricing and remaining limits.
Verified: **38 focused / 121 full backend tests**, zero failures/errors/skips;
full Gradle build, frontend TypeScript/Vite build, OpenAPI drift and diff checks pass.
Frontend/Chromium test suites were not rerun because UI/API shape did not change;
their prior results remain historical. The existing ~519 kB Vite warning remains.
Public DB readback confirms the original FAILED Whisper run and V8 state unchanged;
V9 is verified in isolated tests, not applied to the real-run schema. Both locks and
original evidence hashes are preserved. One isolated Audio Transcriptions validation
is justified only after fresh explicit authorization; real acceptance remains unverified.

## Historical post-PR7 planning (superseded by PR8 authorization)

After PR7 merge, the next authorized branch is `feat/p1-ai-film-understanding`:
**PR8 — AI Film Understanding / Multimodal Continuity Discovery**. This supersedes
`feat/p1-analysis-progress` as the next standalone branch. PR8 absorbs necessary
durable backend stages, truthful UI visibility, active-run reload/recovery and
Film Understanding failure visibility; backend state remains authoritative, with
no fake percentages or invented progress. General/transversal launch hardening is
not automatically included; see the development and implementation plans.

Patrick accepts PR7's honest zero-finding validation on populated-demo/reset and
independent-validation merits. No runtime, assets or provider evidence change, no
accuracy claim and no new provider call. The current result alone still cannot
support a finding-focused recording. Previously recorded green checks remain valid;
this follow-up runs only documentation/scope/diff checks, not the full suites.

## PR7 recovery and current behavior

Fetched origin; verified PR6 merge and exact main SHA
`1e3f28fb162bcdc9d608c40d61d7fcfbcad97909` (parents `3184af6` / `8923cea`).
Initial worktree was clean. Patrick supplied/staged the demo video during recovery;
it was preserved across clean main fast-forward and PR7 branch creation, then restored.
No existing implementation was re-scaffolded. Original recovery context is unchanged.
PR6 pending-review instructions below are historical handoff facts, superseded by
its verified merge and Patrick's explicit PR7 authorization.

Implemented: immutable `between-the-line-v1` runtime manifest, eight approved real
video clips and five visual references; server-side atomic creation/recovery and
replacement reset; typed demo identity, landing entry and confirmed reset UX.
Source is Patrick's original/generated film, attributed to Laurie and Patrick.
Dark/navy metro polo replaces the historical blazer assumption; apartment clothing
is a deliberate transition. No invented findings or preloaded model baseline.

Verified: Gradle build and **83 backend tests** (zero failures/errors/skips), **68
frontend tests**, **17 Chromium tests**, frontend production build, OpenAPI drift and
diff checks. Browser verification uses deterministic API 8097, no key, isolated
browser schema; source and final 1280/820/390 screenshots were inspected.
Earlier failed attempts and their corrections are recorded in the review, not counted
as passes. Normal tests made zero paid calls.

Exactly one real full-sequence Astra validation succeeded: eight shots, 24 selected
frames, five references, **zero findings**. The model contextualized the apartment
wardrobe/lighting transition and noted device identity ambiguity and unreadable small
UI. This does not prove the film error-free or establish a finding showcase.
Run `9b30f01d-54e6-4586-b7ac-63c0f2eb6880`: 16,861 input / 1,176 output tokens,
28.709 seconds, estimated USD **$0.26956** (not a billing receipt). GET-only verification
passed; no reroll, targeted paid call or recorded baseline was added.
Implementation `5eac0b9ba3fc9b2a91b0e9467102eb8e11c781ac` is committed/pushed.
This documentation-only follow-up records the review point; final handoff reports tip HEAD.
Remote CI is not checked. Final evidence: [PR7 review](PR7-REVIEW.md).
Run/setup remains in README. ADR-0007 records transaction/disk-retention limitations.

## Historical PR6 recovery and handoff

Fetched origin and verified PR #5 merge `3184af6cf0119be1fa2949ca1fb60a2584483a56`
(parents `0901f4b` / `5071eb4`). Clean local main fast-forwarded to that exact SHA,
matching origin/main, before creating only `feat/p1-guided-tour`. Inspected recovery
protocol, product/technical docs, ADRs, actual workspace components and test setup.
PR5 awaiting-review statements below are historical handoff facts; PR5 is merged.

PR6 adds an optional first-workspace invitation and persistent Quick tour control.
Exactly four steps: Reference Bible → media/timeline → findings/evidence → resolve/
steer. Uses real stable panels, including empty findings; no fabricated content.
Only new persistence is localStorage `sceneproof.guided-tour.v1` with `skipped` or
`completed`. Storage failure leaves manual restart usable. No project/API/model work
is triggered. Existing drafts, finding selection and evidence remain intact.

Mantine modal supplies dialog semantics, focus trap and Escape; heading receives
focus at every step, closing restores the opener or persistent Quick tour button.
Bottom card, explicit panel outline/step label, bounded viewport layout and temporary
tour-only scroll space keep targets visible. No animation or repeated scroll loop.
No backend, DB, OpenAPI, provider configuration or dependency change. No paid calls.

Verified: **59 frontend**, **15 Chromium**, **71 backend** tests pass, no backend
failures/errors/skips. Production frontend build, Gradle build, OpenAPI drift and
diff checks pass. Backend tests were freshly rerun; build reused unchanged outputs.
Desktop/tablet/mobile screenshots inspected. **Zero real provider calls; zero tour
API writes.** Earlier failed attempts are recorded in PR6-REVIEW, not counted as passes.
PR6 implementation and handoff are committed together; final handoff reports HEAD.
Remote CI was not checked. Deterministic verification API: 8096, no provider key.
Run/setup remains in README. Detailed current handoff: [PR6 review](PR6-REVIEW.md).
Known limits: local browser/origin preference; clearing storage resets onboarding;
when storage cannot save, dismissal cannot survive a fresh page. No demo/progress/
hosting or later video/discovery work. Review and merge approval remain required.

## Historical PR5 recovery

Documentation recovery verified committed/pushed PR5 implementation at
`72c84821cca402f642a041aef596944c511acf88`: clean worktree, matching origin branch,
two commits ahead of main and zero behind after fetch. This follow-up corrects the
original uncommitted/unpushed handoff statements. Main remains at the base SHA below.
Historical tests/builds/smoke evidence is preserved and was not rerun for this
documentation-only correction; only documentation/diff and Git-state checks apply.

Fetched origin, verified PR #4 merge (parents a821d89 and a711d4a), fast-forwarded
clean main to exactly `0901f4b7e4aa183c5f4fe5d1f2b85c07edee0737`, confirmed
main equals origin/main, and created only feat/p1-reference-bible. The worktree was
clean. Recovery protocol, required docs, ADRs and existing implementation inspected.
PR4 pending-merge statements below preserve the historical handoff; PR4 is merged.

Implemented: durable workspace rules editing; optional JPEG/PNG references with
title/guidance; immutable images and permanent archive; original metadata snapshots;
bounded Astra reference context and validated citations; finding reference evidence;
PR4 targeted use of original cited references and separately identified original rules.
Eight active/100 lifetime persisted references; existing frame/image byte budgets
remain shared. Editing never calls the provider. ADR-0006 records the delegated model.

Verified: **71 backend tests**, zero failed/skipped; **44 frontend tests**;
**10 Chromium tests**; Gradle build, TypeScript/Vite build, OpenAPI drift and diff
checks. Final focus fix was rechecked in 13 reference frontend tests and the complete
10-test Chromium suite; unchanged 31 existing frontend tests retain their full-suite
pass. Normal tests use only deterministic ports. Desktop/tablet captures inspected.

One successful real Astra call: project `8c0481a2-4391-4e3c-88d1-b52264e5b310`,
run `e7902a2e-40ad-4654-a41e-ca8f304f4272`, finding
`f0658e4c-31eb-46a6-87e8-aed4dc2dd955`, citing reference
`0a19a096-4a27-4168-9a8a-25b50bd69de7`. Original synthetic red reference, two blue
observations, empty rules. Usage 1360 input / 653 output / 2013 total, 1357 cache-write;
12.878s; estimated USD $0.04964, not a billing receipt. Same-request replay made no
second inference. Database snapshot confirms the original hash. Restarted normal jar
without an OpenAI key: GET verification restored the same finding/citation/image.

Review runtime: normal jar 8095 (PID 27504), no key; Vite 15183 proxies it.
Deterministic API 8094 was stopped; existing user IDE services were preserved.
`.local/pr5-smoke-request.json` retains the paid request UUID; never delete it to reroll.
Known limits: local/no auth, no archive browser/restore, two-step archive/new upload,
100 lifetime persisted records, last-save-wins metadata, no orphan collector. Missing
required content fails closed. No public hosting, video-limit change, discovery or
next PR work. Remote CI was not run at the original unpushed handoff and was not
checked in this documentation-only follow-up.
Full contract, checks, failures, smoke and reviewer risks: [PR5 review](PR5-REVIEW.md).

Final real-result Chromium inspection after restart decoded both shot images and
the cited reference, survived reload, and showed zero attempted API writes/browser
errors. Desktop/tablet screenshots inspected. New-file whitespace checks pass;
the production jar contains no test-only browser provider or test profiles.

## Historical PR4 recovery

Fetched origin and fast-forwarded clean main to exactly
`a821d89c4f7aa0396cf447ec9951f189d42617f2`, the GitHub PR3 merge of `3312550`
into `602cf1c`. Verified main equals origin/main, then created only the PR4 branch.
Read the recovery protocol, product/technical docs, existing ADRs and actual PR2/PR3
implementation. The PR3 review/merge-pending statements below describe the historical
handoff, not the current repository state. Patrick explicitly authorizes PR4 now.

PR4 implements V4 immutable creator actions/scopes/results, targeted re-evaluation
through the existing Astra port/Responses transport, atomic effective-judgement
supersession, and creator resolve/dismiss without inference. Original finding rows,
reasoning and evidence remain intact; API status projects the effective judgement.
Strict typed outcomes permit accepted intent, maintained issue or insufficient
evidence. The UI preserves history, original evidence/selection, real pending/errors,
and unresolved request UUIDs across reload for replay-safe recovery.

Verified: **52 backend tests**, zero failed/skipped; **31 frontend tests**;
**8 Chromium E2E tests**; Kotlin/Gradle build, TypeScript/Vite build, OpenAPI drift
and git diff checks. Chromium covers acceptance, issue-remains then dismiss, resolve,
reload/history, keyboard activation, real images and tablet/mobile layout. PR4
persistence fixtures now use a dedicated sceneproof_steering_test schema; initial
test-data pollution and other failed setup/test attempts are recorded in PR4-REVIEW,
not counted as successful checks. No normal test calls paid OpenAI.

One successful paid targeted smoke reused the original synthetic PR2 finding.
Astra independently returned **ISSUE_REMAINS** because the unchanged red-throughout
rule conflicts with the new repainting intent. Run `04beb368-a347-46c2-aceb-294861fde67f`,
action `2be395df-2e9e-4891-87ca-1c7bcbcfaec5`. Usage 1,472 input / 1,591 output /
3,063 total, including 1,469 cache-write and 929 reasoning tokens. About 31.662s and
USD $0.09794 at published standard rates; not a billing receipt. Replay produced
no second inference. A prior authentication failure caused by a stale inherited key
remains auditable; .env was loaded only server-side and keys were never displayed.

The successful action, prior failure and original evidence survived restart of the
normal jar without any OpenAI key. Separate Chromium inspection blocked API writes,
loaded both original PNGs, restored the real judgement/history on reload and observed
zero writes. Desktop/tablet captures were inspected. No further paid call was made.

ADR-0005 records the authorized immutable-history/projection choice. Full review
contract, IDs, verification, cost and limits: [PR4 review](PR4-REVIEW.md).
Committed and pushed implementation: `7446a40728fdf8579924bea8a880ce5a659e6116`;
main remains at the base SHA above. Code review passed with no implementation blocker.
This follow-up corrects documentation only; implementation tests and the Astra smoke
were not rerun. Remote CI was not checked in this follow-up.
Scope stays local/synchronous; no terminal reopen, global
cross-analysis reconciliation, Reference Bible editor, demo, tour or hosting.
Await final merge approval. Do not merge or start P1.

## Historical PR3 recovery and handoff

PR2 is merged as `602cf1c2a1dac4c6dcd563d01a3e580c53caef59` (merge of
`fe89f16` into `7275f17`). Fetched origin, fast-forwarded local main, verified
clean main equals origin/main at that exact SHA, then created only the PR3 branch.
The older PR2 pending-merge statements below are historical handoff evidence.
PR3 consumes existing persisted findings and shot/frame types; display, selection
and refresh use GET only. No model orchestration, steering or reference editor.
Implemented: paginated findings, URL-scoped selection/reload, affected-shot markers
and navigation, deterministic evidence comparison, detail and real clipboard feedback.
Verified: **34 backend tests**, zero failed/skipped; **21 frontend tests**;
**5 Chromium E2E tests**; Kotlin build, TypeScript/Vite build, OpenAPI drift and
git diff --check. Final Gradle build reused the successful test outputs; it did
not rerun unchanged tests. Earlier failed setup/test attempts are not counted as passes.

Chromium uses a deterministic test-only port, real PR2 persistence/media and a
separate sceneproof_browser schema. No new paid call. Separately verified the
actual PR2 finding `9e9fd139-241f-4136-a931-61d5d4c0b613` through the normal jar
with no provider key: two real images, clipboard content, reload and zero browser
write requests. Desktop/tablet captures were inspected; mobile overflow is tested.

Known limits: analysis initiation remains API-only; no latest-run/list-runs API
exists, so FAILED/RUNNING state requires a linked analysisId. Unfiltered emptiness
never claims successful review. Markers cover the current findings page. The first
evidence frame stays left, alternatives replace the right; reload resets that pair.
No reference editor, steering, demo, tour or hosting is implemented in PR3.

PR3 implementation is committed and pushed at
`fa0dbc9cb25e56612bb5d7b4891ae272f4f2179f`, based on the exact main SHA above.
Review preparation confirmed a clean worktree and the same SHA on GitHub; this
documentation-only follow-up corrects the previous uncommitted/unpushed handoff.
The first follow-up push was rejected with HTTP 403 for the wrong credential
account. Patrick requires `patrickbelanger` for GitHub operations. Git now selects
that account explicitly, and the review follow-up `1cdfe85` was successfully pushed.
The authentication blocker is resolved; external code review remains pending.
Main remains unchanged. Review detail/files/screenshots: [PR3 review](PR3-REVIEW.md).
During implementation verification: Vite 15177 → normal jar 8090 (PID 150108), no provider key.
The old PR2 API 8088 was no longer running. Existing IDE services were preserved.
Review preparation reran 21 frontend tests, TypeScript/Vite build and OpenAPI drift:
all passed. Backend 34 and Chromium 5 passing results remain from the unchanged
implementation verification; these suites were not rerun for this documentation change.
Remote CI status could not be verified: GitHub CLI returned HTTP 401 Bad credentials.
This is not a CI failure or a CI pass claim. Stop here for Patrick's review;
do not merge or start feat/p0-intentional-change-steering.

## Historical PR2 recovery and verification

Recovered a clean main at the exact requested SHA
`7275f17a8c3c50b56056487b5a5fceac1a4fa8e9`, inspected PR1 implementation,
ADRs and recovery sections 34–44. Created only feat/p0-astra-analysis from that
base. Git metadata writes required sandbox escalation; no global Git config changed.
Patrick explicitly moves durable findings/API into PR2; only findings UI remains PR3.

Real transport smoke passed after correcting local authentication: gpt-6-astra,
one original PNG, strict JSON, 73 input / 20 output / 93 total tokens, 3.45 seconds.
The earlier 401 was not counted as successful; secrets were never displayed.
JDK HTTP adapter, context selection, schema validator, V3 runs/findings/evidence,
deduplicated API and generated client are implemented and verified.
The initial Kotlin Mockito matcher failures were corrected; they are not counted
as successful checks. Final Gradle build passes **34 backend tests**, zero failures
or skipped tests: 13 existing, 8 schema/context, 6 adapter, 7 PostgreSQL/API.
Frontend passes **6 tests**; TypeScript/Vite build and OpenAPI drift pass.
Final **4 Chromium E2E tests** pass against the restarted final jar on 8088 with
isolated Vite on 15174. Real success payloads also pass Ajv OpenAPI validation.

Application smoke: two original PNG imports → two normalized PR1 frames → one real
PROP/HIGH finding. Run `ce4d940d-a3c0-4f26-88e2-44a108e9767c`, project
`dc4ee12a-b72d-43a3-a072-9a7fe091ccdf`. Usage: 1078 input / 572 output / 1650 total,
1075 cache-write tokens, 11.54 seconds. Same-request replay was deduplicated; real
run/finding survived API restart. Two successful paid calls total approximately
$0.04380 at published standard rates, not a billing receipt.

PR2 implementation was committed and pushed at reviewed HEAD
`850e48071713c1dd727f0cbf8479958a26cb47df`, based on the SHA above.
Code review passed; this documentation-only follow-up corrects the handoff state.
At that handoff, final merge approval remained pending. PR2 is now merged. Historical review contract and risks:
[PR2-REVIEW](PR2-REVIEW.md). Local API 8088 (PID 125900) remains available for review;
user IDE services were preserved. `.env` is ignored and loaded only by the backend
launcher; never printed. Named smoke/browser projects remain in local storage.

Limits: 8 READY shots/24 frames, no chunking, synchronous bounded HTTP, local-only
access, lazy five-minute interrupted-run recovery. PostgreSQL unavailability may
prevent recording failure; timeout may incur charges; retry must reuse requestId.
Findings UI, steering, Reference Bible editing and public deployment remain planned.
That approval and merge are now confirmed by PR3 recovery above.

## Historical PR1 handoff

Targeted pre-merge correction verified: remove the major-brand allowlist while
retaining the `ftyp` signature gate and requiring FFprobe MP4/QuickTime format,
H.264 and all existing video limits. Real `iso6` H.264 MP4 imports successfully;
a corrupt `ftyp`-only file still fails. Architecture and PR1 scope are unchanged.

### Recovery and working state

PR0 merge verified at `fefb7db` on origin/main after fetch. The initial worktree
was clean on feat/p0-foundation. Local main was fast-forwarded and PR1 branched
from that merge. Existing components and historical context were preserved.
Correction recovery found a clean worktree at `b373b20`, tracking
`origin/feat/p0-media-ingestion`; the PR1 baseline is now committed.
Final merge recovery found a clean, synchronized branch at `95ee2f9` (including
the compatibility correction). Patrick approved integration. Main incorporates
that branch through a merge commit with only handoff documentation additions.
The merge is local; origin/main has not been pushed by this session.

Project creation/rules/reload remain working. PR1 adds JPEG/PNG and bounded
MP4/H.264 upload, local MediaStorage, controlled FFprobe/FFmpeg scene/fallback
sampling, durable ordered shots/frames, normalized PNG delivery scoped by project,
and failed import history. React supports import, real pending/error/retry states,
frame selection and reload. No Astra calls, findings or demo are implemented.

### Verification

- Gradle build --rerun-tasks: passed immediately before merge, 13 backend tests
  (4 foundation + 9 media), zero failed/skipped,
  real PostgreSQL 17.9 and FFmpeg/ImageIO. V2 applied successfully to test and local DB.
- Frontend: 6 Vitest/RTL tests passed; TypeScript and Vite production build passed.
- OpenAPI generation drift check: passed.
- Chromium: 3 E2E tests rerun and passed against the corrected packaged API,
  including actual PNG
  load, multipart contract, project scoping, reload and persistent failure history.
- Desktop/tablet screenshots inspected; no horizontal overflow.
- git diff --check: passed. Remote CI status has not been checked in this session.

An early test failure came from unflushed JPA fixtures in enclosing test transactions;
fixtures now flush before JDBC reads. Browser execution required access outside the
sandbox to the installed Chromium cache. Skipped/failed initial attempts are not
counted as passing verification.

### Running

README contains setup and limits; set Java 25 and an absolute MEDIA_ROOT. PostgreSQL
is running on 55432. Existing user IDE API/Vite on 8085/5173 were preserved.
Final merge verification used the freshly built API on 8087 with repository
.local/media and Playwright-managed Vite on 15173 (API_PROXY_TARGET + E2E_WEB_PORT
overrides and CI=true to prevent reusing another local server). An initial run
on 5175 reached a different existing server; the isolated rerun passed all tests.
Restart the IDE API to load PR1 classes before testing on the default ports.
No API key is needed. FFmpeg/FFprobe must be on PATH for video and backend tests.

### Known limits / blockers

No local PR1 blocker. Synchronous imports allow one active operation per process,
32 frames per video, 100 attempts per project. No automatic orphan collector:
abrupt process termination may leave unreferenced files. Normal failures clean
their directories; database failure may prevent recording failed history.
No deletion/reorder API, EXIF rotation or advanced video color management in PR1.

Public hosting, durable media, anonymous isolation and paid-demo budget remain
pending material decisions before deployment. The app stays local/unauthenticated.
Tests leave clearly named browser verification projects/media in local storage.
See ADR-0003 and MEDIA-PIPELINE for precise boundaries.

### Next at the PR1 handoff

Stop after the approved PR1 merge. No PR2 branch or implementation was started.
The next planned slice remains `feat/p0-astra-analysis`, awaiting a new instruction.
September 18 target and planned branch order remain unchanged.
