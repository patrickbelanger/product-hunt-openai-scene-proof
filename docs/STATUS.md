# SceneProof Status

Last updated: 2026-09-12
Current branch: `feat/p0-findings-workspace`
Current milestone: PR3 implemented and locally verified; ready for review, not merged.
Current task: Review `feat/p0-findings-workspace`; no steering work authorized here.

## PR3 recovery

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

Branch is uncommitted and unpushed, based on exact main SHA above. Main remains
unchanged. Review detail/files/screenshots: [PR3 review](PR3-REVIEW.md).
Local review: Vite 15177 → normal jar 8090 (PID 150108), no provider key.
The old PR2 API 8088 was no longer running. Existing IDE services were preserved.
Remote CI has not run for these unpushed changes. Stop here for Patrick's review;
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
