# PR2 review handoff — 2026-09-12

## Branch and scope

- Branch: `feat/p0-astra-analysis`.
- Base: `7275f17a8c3c50b56056487b5a5fceac1a4fa8e9`.
- Reviewed, pushed implementation HEAD: `850e48071713c1dd727f0cbf8479958a26cb47df`.
- PR2 code review passed. This documentation-only correction follows the reviewed
  implementation commit; final merge approval remains pending. No merge or PR3 work.
- Recovery inspected the clean worktree, exact main SHA, recent commits, PR1 code,
  authoritative documents and relevant ADRs before adding anything.
- PR2 owns durable findings/read API by Patrick's explicit instruction. No React UI,
  intentional-change steering, Reference Bible editor or PR3 functionality was added.

## Integration

Application → ContinuityAnalysisPort → AstraContinuityAnalysisAdapter →
Java 25 HttpClient → `POST https://api.openai.com/v1/responses`.
Exact model: `gpt-6-astra`. Kotlin/Spring Boot/React/PostgreSQL/Gradle remain intact.
Spring AI 2.0.1 documents vision and native schema via its OpenAI Chat integration,
but the inspected documentation did not establish the required complete Responses
transport. Official Java SDK 4.63.1 was verified as an alternative. JDK HTTP is the
smallest one-endpoint integration without an extra dependency/implicit SDK retry
layer. ADR-0004 records the choice under the user's explicit transport authorization.

Inputs are ordered project/rules, shot metadata and adjacent shot IDs, then explicit
frame IDs/timestamps/dimensions followed by high-detail inline normalized PNG.
At most 8 READY shots, first/middle/last (up to 3) representative frames each,
24 total; no hidden prefix truncation, all-pairs calls or chunk consolidation.
The snapshot stores text/identifiers/hashes, not binary/base64 duplicates.

Structured Outputs uses strict `text.format` JSON Schema. The same schema's
supported subset validates local parsing, with duplicate keys/trailing JSON rejected.
Typed result mapping and application checks enforce exact inspected input coverage,
finite confidence, bounded strings/categories, valid affected shots/evidence owners,
and empty reference IDs. Server-generated IDs/status prevent model-invented entities.

## Persistence and API

V3 adds `analysis_runs`, `findings`, `finding_shots`, `finding_frames` plus composite
ownership constraints on existing shot/frame IDs. RUNNING commits before inference;
SUCCEEDED and all findings commit atomically. Safe FAILED states retain available
usage/provider IDs. No PostgreSQL transaction spans the provider request.

- POST `/api/v1/projects/{projectId}/analyses`: required `{requestId: UUID}`;
  synchronous 200 run on success/replay, Problem with analysisRunId on failure.
- GET `/api/v1/projects/{projectId}/analyses/{analysisId}`: scoped durable state.
- GET `/api/v1/projects/{projectId}/findings`: optional analysisId/page, 20 per page.

Project/request UUID uniqueness prevents duplicate paid work; replay returns
RUNNING or the original terminal result. A new UUID is a new authorized attempt.
The OpenAPI document, generated TS and typed client include these endpoints and
analysisRunId on errors. No handwritten frontend API DTOs were added.

## Verification executed

| Check | Final result |
| --- | --- |
| `gradlew.bat build` | Passed, 34 backend tests, zero failures/errors/skips |
| Existing project/media tests | 13 passed, real PostgreSQL and FFmpeg |
| Structured output/context tests | 8 passed |
| Adapter request/parser/errors/usage/bounds tests | 6 passed |
| Analysis lifecycle/API/atomicity tests | 7 passed, real PostgreSQL |
| `npm.cmd test` | 6 frontend tests passed |
| `npm.cmd run build` | TypeScript and Vite passed |
| `npm.cmd run api:check` | Passed |
| `npm.cmd run test:e2e` | 4 Chromium tests passed against final restarted jar |
| Read-only real run/finding Ajv validation | Passed after API restart |
| Smoke script syntax checks | Passed |

Backend tests mock only the analysis port for application work; adapter parsing
tests never call HTTP. Browser analysis test uses a no-shot project to verify a
durable failure without paid inference. Initial authentication and Mockito setup
failures were resolved before final passing runs. Remote CI was not executed.

## Real Astra evidence

The one explicit smoke script has transport and application modes. Original fixture
PNGs are generated from code; no private/personal media or API keys are committed.

| Evidence | Transport spike | Application smoke |
| --- | --- | --- |
| Result | Red square recognized; strict JSON parsed | One validated real persisted finding |
| Input images | 1 original PNG | 2 images imported/normalized through PR1 |
| Latency | 3.45 s | 11.54 s including result checks |
| Input/output/total tokens | 73 / 20 / 93 | 1078 / 572 / 1650 |
| Cached input/reasoning tokens | 0 / 0 | 0 / 0 |
| Cache-write tokens | 0 | 1075 |

Application project: `dc4ee12a-b72d-43a3-a072-9a7fe091ccdf`.
Run: `ce4d940d-a3c0-4f26-88e2-44a108e9767c`.
Finding: `9e9fd139-241f-4136-a931-61d5d4c0b613`.
Real title: **Central square changes from red to blue**, category PROP, severity
HIGH, model confidence 1.0 (not an independently calibrated accuracy score).
The explanation cites the red-prop rule and absence of a narrative replacement;
the correction preserves square size/position/background while restoring red.
Both shot IDs and normalized frame IDs map to real stored PR1 entities.
Same-request replay was identical; run/finding survived restart. No paid replay
was needed for the subsequent contract/restart checks.

At the model card's standard USD rates ($10/M input, $1/M cached input, $12.50/M
cache write, $50/M output), estimated smoke spend is $0.00173 + $0.04207 = **$0.04380**.
This treats cache-write tokens as a subset of input; actual account/tier billing
is not verified. [Published model pricing](https://developers.openai.com/api/docs/models/gpt-6-astra).

## Limits and review risks

- One provider request per admitted valid analysis; zero application retries;
  10 s connect / 120 s total HTTP deadline. Timeouts can still incur provider cost.
- 8 MiB/frame, 16 MiB aggregate image bytes, 24 MiB request, 256 KiB response,
  128 KiB structured text, 6000 output tokens (including reasoning), 20 findings.
- One in-process permit and one globally RUNNING database row; 100 attempts/project.
  Stale RUNNING attempts fail lazily after five minutes; no worker/replay loop.
- Database unavailability can prevent failed-state recording. Atomicity tests prove
  partial finding insertion rolls back; later GET/start provides stale-run recovery.
- Sampling may miss brief changes and cannot establish general film-analysis quality.
  Projects with more than 8 READY shots are rejected; chunking awaits explicit design.
- The API is local/unauthenticated. Public isolation/quotas/hosting remain undecided.
- Reference IDs are empty, findings stay OPEN, older runs coexist. PR3 will consume
  these results; steering and supersession remain separate work.
- Unexpected application logs preserve stack frames while suppressing exception
  messages; provider bodies, prompts, base64 and secrets are never logged by PR2.

## Review setup and documentation

The final packaged API is available at `http://127.0.0.1:8088` (PID 125900), with
absolute repository `.local/media`; the user's IDE services were preserved.
Use GETs on the project/run above for free inspection. The existing React workspace
shows its imported media; findings are available through the API only.
README describes environment loading, startup, request IDs and paid smoke commands.

Updated: README, .env.example, STATUS, ASTRA-INTEGRATION, ARCHITECTURE, DOMAIN,
IMPLEMENTATION-PLAN, DEVELOPMENT-PLAN, DECISIONS and ADR index. Added ADR-0004 and
this handoff. Original `docs/intial-context.md` remains unchanged.
September 18 target and branch order remain unchanged. Next action: final merge
approval after this documentation correction; do not merge or start PR3 beforehand.
