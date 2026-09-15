# PR10 Security Hardening — preparation inventory

Prepared during PR9, September 14, 2026. **Planning only; PR10 has not started.**
PR8 Film Understanding is complete; PR9 UX/project lifecycle is current; PR10 is
next and uses HIGH reasoning. September 18 remains the delivery target.

## Trust boundaries and assumptions

Current supported topology is a local, unauthenticated workspace: browser → Vite
same-origin proxy → loopback Spring MVC → PostgreSQL and local MediaStorage →
FFmpeg child processes / fixed OpenAI origins. UUID membership checks enforce
relational project provenance, not user authorization. Anyone who can reach the API
can access project data. Public hosting and anonymous isolation remain separate
decisions; a rate limiter alone cannot make this a public multi-tenant service.

Sensitive assets: server API key, uploaded originals/frames, transcript text,
creator rules/references, immutable analysis/decision history, local validation
artifacts and provider budget. Packaged authored demo/master assets are outside
project-owned runtime storage. Untrusted boundaries include HTTP bodies, uploaded
media, provider responses and filesystem configuration. Host administrators remain
trusted; protection against a malicious process mutating storage concurrently is
not claimed by PR9.

## Endpoint inventory

Paths below are relative to `/api/v1/projects/{projectId}` unless stated otherwise.
R = read, W = write, D = destructive, E = expensive/provider-backed, L = large input,
F = filesystem-affecting. Decoding is expensive even when no provider is involved.

| Surface | Classification | Current controls / PR10 review |
| --- | --- | --- |
| GET `/api/v1/projects`, GET project | R | 20-item project pages, bounded page number; no user ownership |
| POST `/api/v1/projects`, PUT `/rules` | W | Validated bounded text; no creation/client frequency quota |
| DELETE project | W D F | PR9 exact-name confirmation, demo refusal, active-run refusal, safe UUID cleanup and retry; review reset/delete/create abuse |
| GET `/shots`, `/frames/{id}/content` | R F | Project membership; normalized PNG; review list/disk read bounds and cache headers |
| POST `/shots` | W L F | One decoder/process, bounded copy/signature/dimensions/process; review upload frequency and disk capacity |
| POST `/film/source` | W L F | Immutable source, same-hash replay, MP4/H.264 bounds; review interrupted and concurrent upload cleanup |
| GET `/film`, `/film/runs/{id}` | R | Ten-run limit, lazy stale-run failure writes; review polling/query pressure |
| POST `/film/runs` | W E F | Consent, UUID dedupe, ten attempts/project, global admission, no retries; project recreation bypasses lifetime budget |
| GET `/film/runs/{id}/transcript`, `/candidates` | R | Project/run checks, bounded evidence, no-store film responses |
| POST `/film/candidates/{id}/decision` | W | One-way explicit creator decisions, 24 confirmed anchors/project |
| POST `/film/candidates/{id}/reference` | W F | Confirmed visual provenance, normal bounded reference ingestion; no inference |
| POST `/analyses`, GET `/analyses/{id}` | W E / R | 100 attempts/project, UUID dedupe, global RUNNING admission and timeout recovery |
| GET `/findings` | R | 20-item pages, optional immutable analysis scope |
| POST `/findings/{id}/actions` | W E for intent; W for resolve/dismiss | Exact affected scope, immutable audit, dedupe; intent performs targeted provider call |
| GET `/findings/{id}/actions[/{actionId}]` | R | Project/finding membership; inspect total history bounds |
| GET/POST `/references` | R / W L F | Eight active/100 lifetime, image validation; review archive/create churn |
| GET/PUT `/references/{id}` | R / W | Bounded metadata, immutable image identity |
| POST `/references/{id}/archive` | W destructive to current Bible | History retained, idempotent; no provider |
| GET `/references/{id}/content`, `/findings/{id}/references` | R F | Historical provenance; review caching/privacy/read amplification |
| POST `/api/v1/demo`, POST `/demo/reset` | W F, expensive decoding; reset replaces | Instance lock, atomic publication, old-copy retention; unbounded handles/generations consume disk |
| GET `/actuator/health`, `/openapi.json` | R | Health-only exposure/no details; inspect production routing/static assets |

Sources inspected: all controllers, application.yml, MediaStorage, MediaProcess,
FfmpegAdapter, SourceFilmService, AnalysisRepository/ContextAssembler,
ReferenceRepository, FilmRepository, DemoService, API contract and Vite/build config.

## Expensive operations, limits and gaps

- Uploads: servlet 100 MB/file, 101 MB/request, disk spooling; service 100 MiB video
  and 10 MiB images. Media: 120 seconds, 3840×2160, 60 fps; image 16 MP/8192 side,
  normalized to at most 1600 side. Review multipart temp placement, disk exhaustion,
  malformed containers, oversized headers and decode bombs before allocation.
- FFmpeg: file-only protocols, explicit MOV demuxer, external references disabled,
  no shell, no stdin, bounded execution/output and descendant termination. Image
  normalization and frame/audio extraction share one process-local ingestion gate.
  Review every argument path, executable configuration, dimensions/fps and cleanup
  after timeout/process death. Global decoder protection across API instances is absent.
- Film Understanding: 24 frames/eight deterministic segments, 768-side discovery,
  16 MiB images/24 MiB serialized requests, 120-second/4 MB PCM, at most 200 transcript
  segments/24,000 characters, one transcription plus one discovery request. Provider
  deadlines are 120 seconds; no application retries. Stage recovery expires at ten
  minutes without rerunning. Review late workers, concurrent read recovery and cost
  admission under restart; timestamps do not establish a financial quota.
- Continuity/targeted: eight shots/24 representative frames, reference images share
  payload limits; one RUNNING admission, 100 attempts/project, five-minute stale
  recovery. Targeted scope uses affected shots and bounded neighbors.
- Demo reset, project creation/deletion and reference churn can bypass project-local
  limits. Retired demos retain disk/history. No user/client spend quota, aggregate
  disk budget, upload frequency control or automatic orphan retention exists.

## PR10 implementation priorities

| Priority / slice | Proposed work and acceptance |
| --- | --- |
| P0 — destructive/path safety | Audit trusted canonical roots, symlink/junction and TOCTOU assumptions; race deletion against ingestion, reads and stale workers; verify no external path or packaged asset can be removed; exercise cleanup retry/restart |
| P0 — provider-cost admission | Define local/client/project budget policy before dispatch; aggregate quota must survive project churn/restarts; deterministic tests prove rejected requests call no provider |
| P0 — resource exhaustion/media | Bound concurrent expensive work and queued requests, spooled bytes/temp/disk, decoder process resources and large responses; hostile fixtures and timeout cleanup tests |
| P0 — secret/config leakage | Audit committed files and production config without printing secrets; test sanitized diagnostics for secret-like payloads and exceptions |
| P1 — API rate limits | Per-client request policy with explicit trusted-proxy assumptions; separate read/poll, write, upload, expensive and delete/reset buckets; return actionable 429/Retry-After; never implicitly retry paid actions |
| P1 — HTTP/static security | Review CSP against Mantine styles/media, clickjacking, referrer policy, content types/nosniff, cache/no-store coverage and CORS; validate behavior before adding headers |
| P1 — privacy/logging | Audit transcripts, uploaded-frame bytes, raw provider responses, SQL exceptions, request IDs and local artifact retention; enforce minimal diagnostic exposure |
| P1 — dependency/config | Review locked npm/Gradle graphs and known advisories, dev tooling exposure, source maps, production routing and error pages; targeted upgrades only |
| P2 — defense in depth | Retention operations, monitoring/redacted abuse diagnostics, operational recovery/runbooks and final launch verification |

## Filesystem and deletion review

PR9 uses generated UUID directory keys under canonical MEDIA_ROOT. DELETE accepts
no path. The service reserves a UUID-only deletion record and removes relational
state in a transaction. LocalMediaStorage checks containment, rejects symlinks and
nonregular entries before removal, walks without following links and retains a tiny
`<uuid>.deleted` marker to reject stale directory recreation. Packaged master/demo
assets are never cleanup targets. Failed physical cleanup is reported as 503 and
can be retried against the same deleted UUID; no automatic collector is implemented.

Review permissions and symlink/junction swaps between checks and operations, root
configuration pointing at inappropriate locations, abrupt termination between DB
commit and marker creation, and cross-process concurrent writes. The local gate is
not a distributed lock. UUID-only tombstones and empty filesystem markers deliberately
retain no film content; define their long-term retention in PR10.

## HTTP, secrets, privacy and database checklist

- Server binds 127.0.0.1 by default; no wildcard CORS configured. Only actuator health
  is exposed. Vite proxies API/health/OpenAPI and test-only routes during development.
  Verify the production serving/proxy design, origin checks and CSP compatibility;
  do not assume dev-server behavior is production hardening.
- OPENAI_API_KEY enters server configuration; no VITE key. Direct Spring startup
  reads environment/properties; Node `--env-file` launcher gives existing environment
  precedence. `.env` and `.local` are ignored, but ignored files can still contain
  sensitive media/results. Review property overrides, IDE/service startup and exports.
- Provider diagnostics use allowlisted status/type/code/request IDs; unknown provider
  text is withheld. Generic errors omit original exception messages/causes. Review
  all remaining log sites and stack frames, content-bearing DB exceptions, transcript
  reads, browser errors, screenshots and operator scripts. Do not log raw keys/media.
- Hikari pool size is five. JPA open-in-view is off; short transactions usually
  surround metadata. Demo preparation holds a bounded 180-second transaction during
  decoding; review pool exhaustion and advisory-lock waits. PR9 deletion has a
  30-second metadata transaction and separate disk cleanup. Inventory unpaginated
  histories, bounded parent limits, query plans, orphan state and transaction retries.
- UUID project membership is not access control. Authentication/public tenancy needs
  a separate product decision; do not imply existing IDs protect confidential users.
- Normal tests use isolated PostgreSQL schemas and mocked/test-only provider ports;
  browser provider classes are excluded from production jars. Verify this remains
  true in packaged release artifacts and production config.

## Explicit boundaries

PR10 is secure-coding, abuse/resource/cost controls, HTTP/media/path/config/privacy,
dependency review and provider/API failure hardening. It is not an authentication
platform, billing system, public multi-tenancy, enterprise IAM, Kubernetes or WAF
project. Those require separate DECISION REQUIRED records. No broad security work
is implemented in PR9 beyond what is needed for safe project deletion.

Selected-range Film Understanding remains future work **after PR10** unless Patrick
reprioritizes it: In/Out → Analyze selection → explicit paid consent → immutable
source-time range input → bounded visual/transcript evidence → provider processing
→ persisted range provenance and results tied to the exact selection. PR9 marks
are inspection-only React state; no provider, retry or clipping domain is added.
