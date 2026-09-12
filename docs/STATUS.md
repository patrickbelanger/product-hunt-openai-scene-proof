# SceneProof Status

Last updated: 2026-09-12
Current branch: `feat/p0-foundation`
Current milestone: P0 foundation complete, ready for review
Current task: handoff of project creation → PostgreSQL → React workspace.

## Recovery completed

Initial repository: one commit (`ccb43f2`), README, license, gitignore and supplied
context only. No applications, plans, ADRs, migrations or hosting configuration.
Existing user edits to `.gitignore` and staged/unstaged context are preserved.

## Working / most recently completed

Documentation, plans and two ADRs established. Java 25 / Boot 4.1.1 backend,
PostgreSQL migration, generated API client and React workspace implemented.
Project creation, rules, persistence, listing and reload verified in Chromium.
Timestamp precision now round-trips through PostgreSQL, protected by a genuine
database re-read regression test. Live API schema validation covers projects,
pagination and RFC 9457 errors. Screenshots reviewed at desktop/tablet widths.
No media ingestion, live Astra analysis or launch demo exists yet.

## Next

Review and integrate `feat/p0-foundation` before creating the next branch; no commit,
push or merge was performed. Smallest next implementation: image ingestion with
bounded validation, MediaStorage and project-scoped shot/frame persistence on
`feat/p0-media-ingestion`, then FFmpeg, Astra analysis and findings inspection.

## Blockers / debt

No foundation blocker. Current app is local and unauthenticated. Public hosting,
isolation and live-demo spend budget need a concrete decision before public launch.
Model account access remains untested. Browser tests leave clearly named verification
projects in the local DB. GitHub CI is configured but has not run remotely.

## Recent decisions

Patrick confirmed Spring Boot **4.1.1** on September 12; `4.11` in original context
is a typo. Java 25.0.2 is installed; shell JAVA_HOME currently selects Java 21.
Backend uses free port 8085; 8080 belongs to another local application.
Security fixes selected Vite 7.3.6, Vitest 4.1.11, patched esbuild and Ajv 8.20.0.
Generic problem `type` is optional as specified by RFC 9457. See [decisions](DECISIONS.md).

## Verification / running

Backend: `gradlew build` passed, 4 integration tests, no skips, real PostgreSQL 17.9.
Frontend: 4 Vitest/RTL tests passed; strict TypeScript and Vite production build passed.
Chromium: 2 E2E tests passed (live JSON-schema contract + create/reload/reopen/tablet).
OpenAPI generation drift check and `git diff --check`: passed.
Clean `npm ci`: passed with npm 10.9.8; audit: 0 known vulnerabilities.
Dependency updates use npm 11.6.2 to avoid npm 10's peer-resolver bug.
Screenshots: `test-results/landing-desktop.png`, `workspace-desktop.png`, `workspace-tablet.png`.
Run: `docker compose up -d --wait postgres`, select Java 25, `npm ci`,
`./gradlew.bat :apps:api:bootRun` and `npm run dev`; full commands in README.
