# SceneProof

**Keep every shot in character.** Your AI continuity supervisor for generative film.

Current slice: PR3 findings workspace — open an analyzed project, select saved
findings, compare their real evidence frames and copy the suggested correction.
PR2 analysis runs, validated findings, evidence and usage persist in PostgreSQL.
Analysis is still explicitly started through the backend API; viewing or refreshing
the workspace never calls OpenAI. The curated demo remains upcoming.

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
their writes. TEST_DATABASE_URL may override the test JDBC URL; never point tests
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
and zero retries. Larger sequences are explicitly rejected. No reference editing
or intentional-change steering yet. See [Astra integration](docs/ASTRA-INTEGRATION.md)
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
The source context remains intact in `docs/intial-context.md`; accepted clarifications
live in the decision records. SceneProof is independent of other commercial projects.
