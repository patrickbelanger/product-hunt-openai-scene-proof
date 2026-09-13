# Decisions

| ID / date | Status | Decision |
| --- | --- | --- |
| D-001 / Sep 12 | Accepted by Patrick | Initial `Spring Boot 4.11` means **4.1.1**. Java 25 and Kotlin 2.3+ remain mandatory. |
| [ADR-0001](adr/ADR-0001-foundation-stack.md) | Accepted | Implement mandated monorepo, relational persistence and pinned build baseline. |
| [ADR-0002](adr/ADR-0002-openapi-client.md) | Accepted | OpenAPI contract generates TypeScript types used by typed fetch client. |
| [ADR-0003](adr/ADR-0003-local-media-ingestion.md) | Accepted, authorized PR1 scope | Local storage, bounded synchronous FFmpeg ingestion, short metadata transactions and persisted failure history. |
| [ADR-0004](adr/ADR-0004-astra-responses-api-integration.md) | Accepted within Patrick's explicit PR2 authorization | JDK HTTP Responses behind the analysis port; strict schema, bounded whole-sequence sampling and durable request deduplication. |
| D-002 / Sep 12 | Accepted by Patrick's PR2 instruction | Finding persistence and read API belong to PR2; findings workspace UI remains PR3. No steering or reference editor. |
| [ADR-0005](adr/ADR-0005-immutable-finding-steering.md) | Accepted within Patrick's PR4 authorization | Immutable creator actions and targeted results supersede effective judgement; original findings/evidence remain unchanged. Reuse PR2 admission/transport, extend the typed port. Resolve/dismiss are creator actions with no provider work. |

Ordinary implementation choices: npm workspaces with a committed lockfile;
loopback-only local services; backend port 8085 (8080 already occupied locally), web 5173, database host port 55432;
English product UI and repository documentation. These do not alter scope.

Foundation verification corrections: PostgreSQL timestamps use microseconds;
generic RFC 9457 errors may omit `type` (implicit `about:blank`). Exact dependency
updates to Vite 7.3.6, Vitest 4.1.11, Ajv 8.20.0 and compatible esbuild address
registry security advisories before the baseline is frozen. npm 11.6.2 resolves
updates; npm 10.9.8 clean installs from the resulting lockfile were verified.

Pending future material decisions: public hosting/persistent media, anonymous
project isolation and concrete live-demo budget. Astra transport is resolved by
ADR-0004; PR4 explicitly authorizes a minimal targeted paid smoke after deterministic checks.

## DECISION REQUIRED format

Current technology/choice; problem; proposed alternative; alternatives considered;
recommendation; implementation impact; deadline impact. Record the response and
update an ADR where significant. Continue independent authorized work meanwhile.
