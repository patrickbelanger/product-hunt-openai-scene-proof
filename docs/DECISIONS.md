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

PR5: [ADR-0006](adr/ADR-0006-reference-bible-history.md) records the explicitly
delegated model choice: immutable reference images, editable current text, submitted
metadata/hash snapshots and archive; eight active references share analysis budgets.

## PR7 accepted decisions — September 13

- Patrick supplied `demo/between the lines - demo.mp4` and confirmed it is his
  original/generated work using his generated references, authorized for SceneProof
  and the public Product Hunt demo. Required attribution: **Laurie and Patrick**.
  Actual source SHA-256 and its alternate conversational name are in `demo/curation.json`.
- The original specification's black blazer is superseded by the film's dark/navy
  metro polo. Apartment clothing is a deliberate scene/time transition. Patrick
  approved up to eight useful real clips, retaining the existing analysis bounds.
  Unsupported historical cases are not expected detections. Current evaluation is
  grounded in visible character/device/UI/spatial questions, outside model context.
- [ADR-0007](adr/ADR-0007-demo-template-and-replacement.md): packaged immutable
  authored inputs, local working instance identity, atomic creation and replacement
  reset with historical copies retained. No recorded findings baseline is shipped.

## PR8 accepted scope and implementation decisions — September 14

- Patrick supersedes the old standalone progress branch: PR7 merged → PR8 Film
  Understanding plus truthful durable stages → PR9 general UX → optional P2.
  `docs/intial-context.md` priorities, branch plan and merge order now agree; historical
  references are labelled superseded. No unrelated historical context is rewritten.
- [ADR-0008](adr/ADR-0008-film-understanding.md) records bounded immutable SourceFilm,
  separate audio/discovery ports, Whisper segment estimates, Astra medium, strict
  evidence validation, candidate authority, normal reference promotion and polling.
  These are within Patrick's explicit PR8 delegation, not a new product-scope change.
- Polling is chosen over SSE for the smallest reliable durable-state recovery; no
  model thought or invented progress. Worker process loss becomes a visible failed
  attempt after a ten-minute deadline, never automatic paid replay.
- V7/V8 preserve original media/discovery/creator provenance. Confirmed anchors are
  bounded to 24 and one-way decisions; editing an already confirmed anchor is a later
  lifecycle feature, not silent mutation. Ten film attempts/project, one globally active.
- September 18 is unchanged. No NLE, public tenancy, deployment or PR9 work is added.
- Patrick's focused rejection review authorizes bounded sanitized diagnostics, not a
  live reroll. Existing failure detail/request-ID fields suffice; no request/provider
  architecture change. Original rejection remains INSUFFICIENT_EVIDENCE, while the
  observability defect is fixed and tested. See [focused review](PR8-TRANSCRIPTION-REVIEW.md).
- Patrick subsequently authorized exactly one isolated corrective transcription-only
  dispatch on corrected HEAD `3900996`, explicitly prohibiting Astra, original-run
  mutation and retry. Validation `87b0d170-3387-4959-8711-a2ef647f20ca` returned
  TRANSCRIPTION_UNAVAILABLE without upstream status/request ID; INSUFFICIENT_EVIDENCE.
  Authorization is consumed, original history preserved, no application code change.
  Real acceptance remains unverified; stop for review, not another request or merge.

## DECISION REQUIRED format (for future decisions)

Current technology/choice; problem; proposed alternative; alternatives considered;
recommendation; implementation impact; deadline impact. Record the response and
update an ADR where significant. Continue independent authorized work meanwhile.
