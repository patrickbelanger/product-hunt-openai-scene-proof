# SceneProof

**Keep every shot in character.** Your AI continuity supervisor for generative film.

Current slice: PR9 UX Polish + Project Lifecycle Usability, based on merged PR8.
Choose **Try the demo film** to open **Between the Line — Continuity Study** with
eight real video clips and five persisted visual references. Original film by Laurie
and Patrick, explicitly authorized by Patrick. **Create a project** remains available.
Reset demo confirms replacement of only the current copy; original analysis history
is retained. The demo starts with no recorded findings. Initial analysis is explicitly
started through the backend API; opening, refreshing and resetting never call OpenAI.

**Film Intelligence** accepts one primary MP4/H.264 film (120 seconds/100 MiB).
New/reset demo copies use the derived 0–36.291667-second analysis source; the immutable
92.458667-second master and historical evidence remain unchanged. **Understand film** explicitly
consents to at most one transcription plus one Astra discovery call per attempt.
Real persisted stages survive reload; candidates require Accept/Edit/Reject before
becoming continuity memory. Visual promotion uses the ordinary Reference Bible.
No fake progress, automatic truth, benchmark hints or paid retries. PR9 adds a visible
workflow bar, quiet polling, compact results/evidence and keyboard timeline inspection.
I/O sets local inspection marks; it never changes analysis scope. Ordinary-project
deletion requires typing the exact name and removes owned runtime records/media;
demos retain Reset. See [PR9 review](docs/PR9-REVIEW.md) and the documentation-only
[PR10 security plan](docs/PR10-SECURITY-PLAN.md). No PR9 live provider calls.

Start a new session with [STATUS](docs/STATUS.md), then follow the recovery protocol
in [AGENTS.md](AGENTS.md). Product requirements: [BRD](docs/BRD.md). Delivery:
[Development Plan](docs/DEVELOPMENT-PLAN.md), [Implementation Plan](docs/IMPLEMENTATION-PLAN.md).

## Locked stack

| Component | Version |
| --- | --- |
| Java / JVM target | 25 (verified with Temurin 25.0.2) |
| Kotlin | 2.3.21 |
| Spring Boot | 4.1.1 |
| Gradle Wrapper | 9.1.0 |
| PostgreSQL | 17.9-alpine |
| Node | 22.23.2 LTS (`.nvmrc`) |
| npm | 11.6.2 for dependency updates; npm 10.9.8 can run locked installs/scripts |
| React / React DOM | 19.2.4 |
| Mantine core/hooks | 8.3.14 |
| TypeScript | 5.9.3 |
| Vite / React plugin | 7.3.6 / 5.1.4 |
| React Router DOM | 7.18.3 |
| TanStack Query | 5.102.8 |
| Vitest / React Testing Library | 4.1.11 / 16.3.3 |
| OpenAPI TypeScript / fetch | 7.13.0 / 0.17.0 |
| Playwright | 1.63.0 |

Spring Data, Flyway, Jackson, PostgreSQL JDBC and JUnit are managed by the pinned
Spring Boot BOM. Spring AI 2.0.x compatibility is documented but no AI dependency
is loaded yet. FFmpeg/FFprobe are now required for video ingestion and backend
tests (including libx264 for generated fixtures). Project/media features and normal
tests need no OpenAI key; paid analysis requires a server-side OPENAI_API_KEY.
Local verification uses FFmpeg N-120856-g9893d66add-20250831; CI installs Ubuntu's
FFmpeg package. See [Media pipeline](docs/MEDIA-PIPELINE.md) for limits and arguments.

## Run locally

Requirements: Java 25, Node from `.nvmrc`, Docker with Compose. Run from repository root.
On this Windows machine, select Java 25 in each backend terminal:

```powershell
$env:JAVA_HOME = 'C:\Users\patri\.jdks\temurin-25.0.2'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:MEDIA_ROOT = Join-Path (Get-Location) '.local\media'
docker compose up -d --wait postgres
npm.cmd ci
.\gradlew.bat :apps:api:bootRun
```

In another terminal:

```powershell
npm.cmd run dev
```

Open **http://127.0.0.1:5173**. API health:
http://127.0.0.1:8085/actuator/health. OpenAPI:
http://127.0.0.1:8085/openapi.json. Port 8085 avoids an existing local service on 8080.
On macOS/Linux, set JAVA_HOME to a JDK 25 and use `bash gradlew` and `npm`.

Default local database: `sceneproof`, user `sceneproof`, password `sceneproof-local`,
host port 55432. These are local development credentials. All services bind loopback.
The Compose volume survives `docker compose down`; do not use `down -v` unless you
intend to erase project data. Stop Java/Vite with Ctrl+C, then `docker compose down`.

Keep MEDIA_ROOT absolute and identical between IDE, Gradle and packaged-jar runs;
otherwise relative storage paths resolve against each process working directory.
Media files survive application restart and are ignored by Git. FFmpeg/FFprobe
must be on PATH, or set FFMPEG_PATH / FFPROBE_PATH to their executable paths.
Imports are synchronous: one active import per API process, up to 32 frames per
video and 100 attempts per project. Failed attempts remain visible for diagnosis.

`.env.example` lists configuration. Compose automatically reads a root `.env`;
directly launched Spring Boot does **not**. Export overrides in the backend shell
or IDE. Keep DATABASE_PASSWORD consistent with the initialized database; changing
the Compose variable does not change an existing PostgreSQL user's password.
Never expose this unauthenticated foundation publicly. See the pending deployment
decision in [DECISIONS](docs/DECISIONS.md).

## Verify

With PostgreSQL running and Java 25 selected:

```powershell
.\gradlew.bat build
npm.cmd run api:check
npm.cmd test
npm.cmd run build
```

Backend integration tests use a dedicated `sceneproof_test` schema and roll back
or clean their writes. PR4 uses `sceneproof_steering_test` and truncates its own
fixture tables between tests, preserving immutable-history rules in normal paths.
PR5 uses `sceneproof_reference_test` with the same isolated cleanup discipline.
STEERING_TEST_DATABASE_URL may override its JDBC URL, but must select that dedicated
schema. TEST_DATABASE_URL may override the other test JDBC URL; never point tests
at production. For browser/contract tests, start the isolated test API in a separate terminal:

```powershell
npx.cmd playwright install chromium
.\gradlew.bat :apps:api:browserTestServer
```

Then, in another terminal:

```powershell
npm.cmd run test:e2e
```

If an IDE instance already occupies the defaults, run a separate API with
`SERVER_PORT=8086`, set `API_PROXY_TARGET=http://127.0.0.1:8086` and
`E2E_WEB_PORT=5174` in the E2E terminal. This keeps browser verification pointed at
the freshly built API without stopping the IDE. Restart an existing IDE API after
pulling PR1 so that its loaded classes include the new media endpoints.

The browser test server uses real controllers, persistence and media with a
deterministic ContinuityAnalysisPort on the **test classpath only**. It uses the
separate `sceneproof_browser` schema and repository `.local/browser-media`.
`BROWSER_DATABASE_URL` / `BROWSER_MEDIA_ROOT` can override these test locations.
No test provider, marker endpoint or browser profile is packaged in the production jar.
Finding fixture creation requires a test-provider marker; never replace this server
with a live provider for the full E2E suite. CI starts this same test server.

Playwright starts Vite automatically. Browser tests create named verification
projects in the browser-test schema; they do not delete existing projects. Screenshots
are under `test-results/`. The GitHub workflow verifies builds, tests and contracts
against PostgreSQL, then runs the real browser flow. Remote CI requires a push.

## API and dependency maintenance

### Demo film (PR7)

Approved assets are checked in under `demo/runtime` and packaged by Gradle. No upload,
project form or key is needed to open the demo. The first launch normalizes references
and extracts video frames through the normal media services; pending state is real.
Later launches in the same tab recover its working copy. Session storage must work
before a creation request is sent. This is local convenience, not public isolation.

POST `/api/v1/demo` with `{ "requestId": "<client UUID>" }` creates or recovers the
current copy. Reuse the UUID after failure. POST
`/api/v1/projects/{projectId}/demo/reset` atomically returns a fresh project UUID.
Repeating reset for the same source UUID recovers its replacement. Project DTO `demo`
is null for ordinary projects, otherwise `{ instanceId, templateVersion, retired }`.
Old copies leave the library and retain their history/media at their original URLs.

`demo/curation.json` records exact source hash, attribution, clip ranges and reference
times. `node scripts/prepare-demo.mjs` reproduces packaged derivatives with FFmpeg;
it verifies the authorized source hash first. It is an authoring tool, not a visitor
step. Different FFmpeg builds can change derivative hashes; commit reviewed outputs
with their manifest and change the template version when authored content changes.
Evaluation notes in `demo/evaluation` are never packaged or submitted to Astra.
The old black-blazer candidate is superseded by the actual dark/navy polo footage.
Details: [Demo](docs/DEMO.md), [ADR-0007](docs/adr/ADR-0007-demo-template-and-replacement.md).

PR7 backend tests use the dedicated `sceneproof_demo_test` schema and tiny generated
images. Browser tests use the approved film with the existing test-only provider.
No test provider or evaluation answers are packaged in the normal jar.

PR7 verification: 83 backend, 68 frontend and 17 Chromium tests; builds and OpenAPI
drift pass. The one real film analysis succeeded with zero findings, contextualized
the apartment transition and warned about device ambiguity/small UI. It is not a
claim of an error-free film or a preloaded baseline. Full evidence: [PR7 review](docs/PR7-REVIEW.md).

The operator-only validation script requires a normal local API (default 8098),
server-side credentials and explicit authorization for a paid call. PR7's authorized
call is already spent; use only `node scripts/astra-demo-validation.mjs --verify`
to GET the existing local run. On a separately authorized new environment, `--run`
persists `.local/pr7-validation-request.json` before creation/inference and refuses
another attempt after dispatch. Keep that ignored manifest, even on failure. It never
reads the evaluation manifest, and no validation result is imported into new demos.

### Real analysis (PR2)

Configure OPENAI_API_KEY only in the backend environment. The model is pinned to
`gpt-6-astra`; no React/VITE variable carries credentials. To load an ignored root
`.env` automatically without displaying its contents, build the jar and use Node's
environment-file support (existing environment variables take precedence):

```powershell
$env:JAVA_HOME = 'C:\Users\patri\.jdks\temurin-25.0.2'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :apps:api:bootJar
node --env-file=.env scripts/run-api.mjs
```

If a stale OPENAI_API_KEY is already exported, remove that variable from this shell
before the command so Node can use the local file. Do not print either value.
`scripts/run-api.mjs` supplies an absolute default MEDIA_ROOT. Existing bootRun/IDE
launches continue to use exported configuration and do not automatically read .env.

To start analysis, use the typed client or HTTP; PR3 adds saved-result inspection:

```powershell
$projectId = '<existing project UUID>'
$requestId = [guid]::NewGuid().ToString()
$requestBody = @{ requestId = $requestId } | ConvertTo-Json
$run = Invoke-RestMethod "http://127.0.0.1:8085/api/v1/projects/$projectId/analyses" -Method Post -ContentType 'application/json' -Body $requestBody
Invoke-RestMethod "http://127.0.0.1:8085/api/v1/projects/$projectId/analyses/$($run.id)"
Invoke-RestMethod "http://127.0.0.1:8085/api/v1/projects/$projectId/findings?analysisId=$($run.id)"
```

Reuse the **same requestId** after a connection failure; the API returns its existing
RUNNING/SUCCEEDED/FAILED run without another paid call. A different UUID authorizes
a new attempt. A failed first POST returns safe Problem JSON with analysisRunId;
GET retrieves the durable failure. Findings lists accept optional analysisId and
page (20 per page). Requests are synchronous, up to 120 seconds for OpenAI.

Open `/projects/<project UUID>` to inspect saved findings. Selection records
`?analysisId=<run UUID>&finding=<finding UUID>` in the URL, so reload restores the
same analysis and finding even if newer analyses are saved. All saved findings
returns to paginated history. `?analysisId=<run UUID>` also displays that run's
SUCCEEDED / RUNNING / FAILED state. PR2 has no latest-run/list-runs endpoint, so
an unfiltered empty list does not prove success or reveal the latest failed run.
Refresh findings is read-only. Copy correction reports actual clipboard success
or failure; the full prompt remains selectable for manual copying.

Limits: 8 READY shots, first/middle/last representative frames (24 total), 16 MiB
images, one active analysis globally, 100 attempts/project, one provider request
and zero retries. Larger whole-sequence requests are explicitly rejected. Targeted
steering uses the bounded affected scope and immediate neighbors. Up to eight active
references share the image budget; targeted review retains original cited references.
See [Astra integration](docs/ASTRA-INTEGRATION.md)
for exact request/response bounds, failure recovery and cost limitations.

One separately executable **paid** smoke path uses generated original PNG fixtures:

```powershell
node --env-file=.env scripts/astra-smoke.mjs --transport
$env:SCENEPROOF_API_URL = 'http://127.0.0.1:8085'
node scripts/astra-smoke.mjs --application
```

Transport performs one tiny direct request. Application mode needs the running API:
it imports two original frames, performs one real analysis, checks persisted findings
and replays the same requestId to verify deduplication. Each new invocation spends
credits and leaves a named local project/media for review. No private fixture/key
is committed. Normal tests never call paid OpenAI endpoints.

### Creator intent, resolve and dismiss (PR4)

Select an OPEN finding and choose **This change is intentional**. Supply a required
explanation and narrative scope, then explicitly confirm one targeted paid Astra
re-evaluation. Creator intent is context, not model agreement. The current judgement
appears separately from the original assessment; immutable history remains available.
Resolve records that the creator corrected the issue. Dismiss records a decision not
to treat it. Both require an audit note and perform no model call.

REST uses POST/GET `/api/v1/projects/{projectId}/findings/{findingId}/actions` and
GET the same path with `/{actionId}`. POST body:

```json
{
  "requestId": "<client UUID>",
  "type": "INTENTIONAL_CHANGE",
  "explanation": "The character changes clothes after arriving home.",
  "scope": "The transition between these two affected shots.",
  "affectedShotIds": ["<original affected shot UUIDs>"]
}
```

The affected-shot set must match the original finding exactly. Other types are
RESOLVE and DISMISS; their scope can describe the finding as a whole. Same requestId
and payload returns the existing action, including failed/running work, without a
second inference. Changed payload conflicts. After network loss use **Recover same
request**; the browser retains the unresolved request in sessionStorage. A failed
attempt stays failed on replay. Only a new explicit confirmation/UUID authorizes a
new attempt. Reload/history/refresh/selection perform no provider work.

Finding `status` is the effective OPEN/INTENTIONAL/RESOLVED/DISMISSED state; original
reasoning/evidence fields remain intact. Read action history for the newer judgement.
AnalysisRun `kind` distinguishes SEQUENCE from TARGETED. Non-open findings remain
selectable but are excluded from page open counts and issue markers. Terminal
findings cannot reopen in this slice. See [PR4 review](docs/PR4-REVIEW.md).

The targeted paid smoke reuses the original synthetic PR2 finding, avoiding another
full analysis. With a newly built normal API on 8093 and its server-side key loaded:

```powershell
$env:SCENEPROOF_API_URL = 'http://127.0.0.1:8093'
node scripts/astra-targeted-smoke.mjs --project dc4ee12a-b72d-43a3-a072-9a7fe091ccdf --finding 9e9fd139-241f-4136-a931-61d5d4c0b613
node scripts/astra-targeted-smoke.mjs --verify
```

Those IDs exist only in the original local smoke database; pass your own original
synthetic fixture IDs on another machine. The first invocation writes an ignored
`.local/pr4-smoke-request.json` manifest before POST; further invocations reuse it.
`--verify` makes GETs only. Any valid independent outcome is accepted by the smoke;
it never loops to obtain approval. Exact recorded result/cost: PR4-REVIEW.

### Reference Bible (PR5)

Edit **Continuity rules** in the project workspace, then **Save rules**. Empty is
valid. **Add visual reference** accepts JPEG/PNG with required title and optional
creator guidance. The Bible displays the actual normalized backend image. Inspect
a thumbnail to edit text or archive it. Replace an image by archiving and uploading
a new reference. Archive is permanent; original cited evidence stays inspectable.
Rules only, references only, both or neither remain valid. Editing never calls OpenAI.

Eight active images and 100 successfully persisted references per project lifetime;
archives count toward the latter. All active references are selected in creation/UUID
order at context assembly. Input: 10 MiB, 16 MP, 8192 per side; normalized PNG at
most 1600 per side. References and frames share 8 MiB/image, 16 MiB aggregate and
24 MiB serialized request limits. Sequence analysis remains explicitly API-started.

Endpoints under `/api/v1/projects/{projectId}`:
- PUT `/rules` with `{ "rules": "..." }` (OpenAPI names the path variable `id`).
- GET/POST `/references`; multipart `file`, `title`, `guidance` (empty allowed).
- GET/PUT `/references/{referenceId}`; PUT edits title/guidance only.
- POST `/references/{referenceId}/archive`, idempotent and without inference.
- GET `/references/{referenceId}/content`, normalized PNG including archives.
- GET `/findings/{findingId}/references`, original submitted metadata/image identity.

One separate paid PR5 smoke uses original synthetic PNGs and a normal local API
with server-side configuration. Only run after deterministic checks:

```powershell
$env:SCENEPROOF_API_URL = 'http://127.0.0.1:8095'
node scripts/astra-reference-smoke.mjs
node scripts/astra-reference-smoke.mjs --verify
```

The ignored `.local/pr5-smoke-request.json` is written before inference. Reruns
reuse that UUID; `--verify` performs GET only. Keep the manifest. No loop seeks
a desired citation or judgement. Evidence and limits: [PR5 review](docs/PR5-REVIEW.md).

### Quick tour (PR6)

Open any project and choose **Start tour** in the optional first-visit invitation,
or **Quick tour** in the workspace toolbar at any time. Four short steps introduce
the Reference Bible, media/timeline, findings/evidence and resolve/steer workflow.
Use Skip or Escape to leave, Back/Next to navigate and Done to finish. The browser
remembers completion/Skip in `sceneproof.guided-tour.v1`; no project data is changed
and no analysis is started. If browser storage is unavailable, manual restart works.

### Contract maintenance

Edit `packages/api-client/openapi.json`, update Kotlin behavior/tests in the same
change and run `npm run api:generate`. Generated TS is checked in and CI checks drift.
API: POST/GET `/api/v1/projects`, GET `/api/v1/projects/{id}`. Lists accept `page`
(20 items, newest first); failures use `application/problem+json`.
Media: POST multipart / GET `/api/v1/projects/{projectId}/shots`; GET normalized
PNG `/api/v1/projects/{projectId}/frames/{frameId}/content`. See the OpenAPI contract.

Keep exact versions and lockfile stable. The original npm 10 peer resolver fails
when updating Vitest in this workspace; use `npx --yes npm@11.6.2 install` for
dependency changes. Do not delete the lockfile or use force/legacy peer resolution.
Vite and Vitest patch/minor updates during foundation address actual npm advisories.

## Project memory

[Architecture](docs/ARCHITECTURE.md) · [Domain](docs/DOMAIN.md) · [UX](docs/UX.md) ·
[Astra](docs/ASTRA-INTEGRATION.md) · [Media](docs/MEDIA-PIPELINE.md) ·
[Demo](docs/DEMO.md) · [Launch](docs/PRODUCT-HUNT.md) · [ADRs](docs/adr/README.md).

Use short-lived feature branches; keep main runnable and review before integration.
The source context keeps its original filename/history in `docs/intial-context.md`;
PR8's scoped planning reconciliation and decision records agree. SceneProof is
independent of other commercial projects.

PR8 deterministic checks: `./gradlew build`,
`npm run test --workspace @sceneproof/web -- --maxWorkers=2`, `npm run build`,
`npm run api:check`, and Chromium against `:apps:api:browserTestServer` with an empty
key (see existing E2E setup). Do not run normal automated tests against a paid provider.
The one real PR8 transcription was rejected; no Astra discovery call followed.
Successful live multimodal acceptance remains unverified. Recover that recorded
failure with `node scripts/astra-film-validation.mjs --verify` and the same local
database/API on `SCENEPROOF_API_URL`; never remove its local lock/manifest to reroll.
