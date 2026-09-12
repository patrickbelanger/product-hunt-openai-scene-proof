# SceneProof

**Keep every shot in character.** Your AI continuity supervisor for generative film.

Current slice: P0 foundation — create a project with continuity rules, persist it
in PostgreSQL and reopen its visual workspace. Media ingestion, GPT-6 Astra
analysis and the curated demo are upcoming; there are no simulated model findings.

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
is loaded yet. FFmpeg/FFprobe are needed beginning with media ingestion, not for
foundation. No OpenAI key is required for the current flow.

## Run locally

Requirements: Java 25, Node from `.nvmrc`, Docker with Compose. Run from repository root.
On this Windows machine, select Java 25 in each backend terminal:

```powershell
$env:JAVA_HOME = 'C:\Users\patri\.jdks\temurin-25.0.2'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
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
at production. For browser/contract tests, keep the API running first:

```powershell
npx.cmd playwright install chromium
npm.cmd run test:e2e
```

Playwright starts Vite automatically. Browser tests create named verification
projects in the local database; they do not delete existing projects. Screenshots
are under `test-results/`. The GitHub workflow verifies builds, tests and contracts
against PostgreSQL, then runs the real browser flow. Remote CI requires a push.

## API and dependency maintenance

Edit `packages/api-client/openapi.json`, update Kotlin behavior/tests in the same
change and run `npm run api:generate`. Generated TS is checked in and CI checks drift.
API: POST/GET `/api/v1/projects`, GET `/api/v1/projects/{id}`. Lists accept `page`
(20 items, newest first); failures use `application/problem+json`.

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
