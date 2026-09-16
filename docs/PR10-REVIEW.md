# PR10 security review

Implemented and verified, September 15, 2026. Base: `bcc7c7c06d2d18af49ef1ec93bdaa01b36c56a82`.
PR9 is merged; its historical STATUS handoff is superseded by this authorized slice.
No provider validation is authorized. Product behavior is frozen.

## Inventory reconciliation before implementation

The endpoint/classification inventory in PR10-SECURITY-PLAN remains applicable.
Actual repositories enforce 20-item project/finding pages, 100 shot attempts,
100 reference lifetime/eight active entries, 100 actions per finding, ten film
runs and 100 continuity/targeted runs per project. Film result/transcript arrays
are validator-bounded. Action history is bounded by admission, not SQL pagination.

| Action | Admission/concurrency | Failure/persistence/replay | Finding |
| --- | --- | --- | --- |
| Project create/rules | Validated text; no global creation bound | Transactional writes; create has no replay UUID | Churn bypasses project limits |
| Project delete | Exact name, rejects demo and active runs; media gate and DB analysis lock | DB cascade/tombstone before disk; repeat DELETE retries cleanup | Root/alias checks need strengthening; local gate is not cross-process |
| Demo open/reset | Instance advisory lock; individual assets take media gate | Atomic publication; rollback asset compensation; retained old copies; UUID/replacement replay | No aggregate disk bound; unrelated instances can hold DB connections; reset does not reject active runs |
| Shot/source/reference upload | One process-local media gate; bounded copy/decode | Shot failure persisted; source hash replay; reference cleanup; abrupt orphan risk | Multipart spooling precedes service gate; cumulative disk unbounded |
| Film run | Shared DB admission lock; globally active check; ten/project | Durable stages, request UUID, asynchronous worker, ten-minute stale expiry, no retry | Project churn bypass; extracted staging/copy failure files remain |
| Continuity/targeted | Shared DB lock/global active check; 100/project; sequence semaphore | Durable run/action/context; UUID replay; five-minute expiry; no retry | Project churn bypass; elapsed expiry does not prove old worker stopped |
| Candidate decision/promotion | One-way decisions; 24 confirmed; promotion uses reference gate | Transactional decision/provenance; immutable source image | No new provider surface |
| Reads/poll/history/content | Relational UUID membership; bounded parents/pages | Film/analysis GET can expire stale runs | No user authorization; frame response streams file without byte/decode validation |

Other verified controls: file-only FFmpeg protocols, no shell/redirects/provider
retries, image dimensions checked before decode, bounded subprocess/response bytes,
fixed provider origins, safe literal error classification, loopback bind, no wildcard
CORS, health-only actuator. Responses response IDs accept arbitrary short strings;
request IDs accept secret-shaped strings. API-wide browser headers/admission absent.

## Recovery checkpoint

DONE: clean-main recovery/branch and endpoint inventory. PARTIAL: storage checks,
staging cleanup, identifier sanitization, HTTP filter and durable paid-run integration.
NOT STARTED at recovery: admission-specific verification, full regression/build/browser
checks, dependency audit and final docs. BLOCKED: none after Java 25 verification.
The interrupted narrow command failed at test compilation (Jackson 3 deepCopy API),
not test execution. The earlier java.security access failure was sandbox-specific;
Java 25.0.2 runs under the operator account. No prior test pass is inferred.

Final classification: DONE — approved admission, P0/P1 hardening, deterministic
verification, dependency applicability review and documentation. No unfinished
implementation or verification blocker remains within the approved scope. P2 and
deployment limitations below are explicitly deferred, not claimed implemented.

The 10,000/hour/day test-only overrides isolate pre-existing fixture-heavy suites
from deployment spend ceilings; browser fixtures also persist across suite runs.
They do not validate admission behavior. Dedicated paid-test schema uses 2/hour,
3/day to exercise rejection, concurrency, rollback, persistence and replay cheaply.
Production defaults remain 10/100. No test provider/config is packaged in the jar.

## Approved decision

Patrick approved 10/hour and 100/24 hours with a default-enabled operator kill switch,
durable reservations surviving project deletion and restart, and no monetary guarantee.
Existing deployment/privacy decision remains:
reachable clients can read and modify every project; public tenancy is out of scope.

## Verification

Expanded narrow checkpoint: 62 tests passed across admission, disabled configuration,
HTTP, storage, Responses, demo and Film Understanding suites. Final replay/rollback
assertions passed in the 29-test admission/film slice. Final full backend: 152 tests
across 18 suites, zero failures/errors/skips; Gradle build/bootJar pass. Full frontend: 102 tests in
12 files with two workers; TypeScript/Vite production build and OpenAPI generation/
drift check pass. Entry JS remains 541.37 kB with the existing chunk warning.
Chromium: all 22 flows pass, including same-origin writes, upload boundaries,
demo reset and destructive lifecycle. After restarting the final API, all three
security/lifecycle flows passed again. The sandbox cannot access installed Chromium;
the initial browser-launch failure run is not counted as product test evidence.
The first launched-browser run exposed a real Host-rewriting proxy mismatch
(12 passed/ten failed); preserving Host fixed it without weakening Origin checks.
The new deletion-gate unit assertion initially expected detail rather than the safe
exception code; its assertion was corrected and the final full suite passed.
An initial frontend run did not forward its worker cap and timed out one unchanged
steering test (101 passed/one failed); bounded-worker verification is recorded below
in the passing two-worker run above. Earlier PR9 results are historical, not PR10 evidence.
Production jar listing excludes test providers/configuration/evaluation resources;
production source-map count is zero. The final 283-file current-tree/build/artifact
scan found no known configured key or credential-pattern matches (not a full entropy,
Git-history or screenshot OCR audit). Provider calls: zero.

Final commands: `gradlew.bat build`; `npm run test --workspace @sceneproof/web -- --maxWorkers=2`;
`npm run build`; `npm run api:generate`; `npm run api:check`; `npm run test:e2e`;
then `npm run test:e2e -- tests/e2e/security.spec.ts tests/e2e/ux-polish.spec.ts`
against the restarted API. Local logs are in ignored `.local/pr10-*.log`; JUnit XML
and browser artifacts remain local. CI has not been claimed green or a merge authorized.

## P0 findings and mitigations

- Project churn could bypass paid-run ceilings: V11/PaidRunAdmission now atomically
  accounts across film, sequence and targeted work. Defaults 10/hour, 100/24h.
- Expired durable states do not imply the actual worker stopped: PaidWorkGate serializes
  live work within one API process and blocks deletion while a worker remains active.
  Deletion checks that gate before acquiring media admission, so a rejected deletion
  cannot steal the media permit from a live worker preparing evidence.
  Conditional durable completion/stage updates still reject late results.
- Storage aliases/junctions could violate project ownership assumptions: canonical
  identity checks reject redirected roots/project/asset paths, nonregular files and
  traversal/absolute/alternate-stream keys. Cleanup cannot create missing trees.
  Deletion retains project-name/demo checks, UUID-only targeting and durable retry.
- Film extraction left staging and unpublished copied frames: compensation removes
  staging and confirmed-uncommitted assets; uncertain DB outcomes defer cleanup.
- Multipart spooling preceded service admission: the filter now serializes upload/demo
  parsing, while existing decoder gates and media limits remain. Free-space pressure
  rejects new ingestion before spooling and before creating asset directories.
- Provider response IDs allowed arbitrary short text: prefix/ASCII/length and key
  exclusion now apply. Upstream error bodies remain withheld; safe codes/IDs persist.

## P1 findings and mitigations

- API requests lacked broad bounds: 64 KiB non-multipart mutation bodies, bounded
  rolling request buckets and Tomcat thread/connection/admission queue defaults.
- Cross-origin form/multipart writes were insufficiently protected by CORS alone:
  unsafe methods reject mismatched/null Origin and same-site/cross-site Fetch Metadata.
  CLI requests without browser-origin headers remain supported; this is not auth.
- Browser/privacy headers now cover API errors and data: no-store, nosniff, DENY,
  no-referrer, restrictive API CSP and disabled camera/microphone/geolocation.
  Vite binds loopback explicitly with compatible frame/object/base restrictions.
  Its API proxy explicitly preserves Host and disables development CORS. Chromium
  caught Vite's string-proxy default rewriting Host and rejecting legitimate browser
  writes; the corrected proxy is covered by real browser same-origin POST coverage.
- Database pool acquisition/locks are bounded. ORM SQL-error loggers are disabled
  because database error detail can include row contents; application diagnostics
  retain exception class/stack location or generated IDs without messages/causes.
- Existing list bounds were verified: 20 projects/findings per page, page 0..10000,
  100 shots/references/actions per owning scope, ten film runs, 200 transcript segments,
  24 frames and bounded schema results. No pagination/schema redesign was needed.

## Operating controls and rationale

| Setting | Default | Semantics |
| --- | --- | --- |
| PAID_RUNS_ENABLED | true | Restart after changing; false blocks new paid runs, not reads/replays or already-running work |
| PAID_RUNS_HOURLY_LIMIT | 10 | New logical runs in database rolling hour |
| PAID_RUNS_DAILY_LIMIT | 100 | New logical runs in database rolling 24 hours |
| HTTP_READS_PER_MINUTE | 600 | Shared process rolling minute; accommodates polling plus frame bursts for a small demo |
| HTTP_WRITES_PER_MINUTE | 120 | Shared process rolling minute for ordinary mutations; two/second sustained ceiling |
| HTTP_MEDIA_PER_MINUTE | 30 | Shared process rolling minute for multipart/demo preparation; at most one concurrently |
| MEDIA_MINIMUM_FREE_BYTES | 536870912 | Minimum 512 MiB on media/JVM-temp filesystems; can be raised |

HTTP buckets are separate, bounded-memory queues keyed only by action class; no IP
or forwarded-header identity. Every admitted HTTP request, including replay/failure,
counts in its process bucket. Bucket state resets at process restart. Durable paid
reservations do not. Global limits intentionally permit shared-denial-of-service by
an unauthenticated caller; no fairness or per-person quota is implied.

One Film Intelligence admission covers at most transcription plus discovery; sequence/
targeted covers one provider call. Provider failure after commit still consumes a slot;
transaction rollback before commit consumes neither run nor reservation. Replays of
durable request identity bypass the paid switch and reservation checks. No automatic
refund/retry or Retry-After promise. Counts are not cost estimates or monetary guarantees.
All API instances must share the database/schema and operator configuration.

The reservation table contains only UUID/time and no content/project FK. It survives
deletion/reset/restart; successful admission may prune rows older than 24 hours.
Durable run/request identity continues preventing replay after pruning. Pre-PR10 runs
are not retroactively counted. Switch configuration requires API restart in this MVP.

The 512 MiB watermark covers one maximum 101 MiB spool, 100 MiB input, up to 32
1600-side RGB frames and normalization staging. It does not reserve disk atomically.
Tomcat uses 32 workers, 64 connections, queue 32 and 20-second connection timeout;
Hikari remains five connections with five-second acquisition/lock waits and 180-second
statement timeout. Existing demo transaction is bounded at 180 seconds. These are
small-process limits, not protection against transport-layer denial of service.

## Privacy and retention

Routine logs contain generated IDs, error classes and safe literals; no prompts,
frames, transcripts, keys or raw provider payloads. No client source reads OPENAI_API_KEY.
Direct Spring launch uses its normal exported properties/environment precedence;
the existing Node --env-file launcher lets existing environment values win.
No new logging/diagnostic endpoint, test route or frontend secret variable is added.

Originals, normalized evidence, transcripts, creator context, validated model results
and usage remain intentionally persisted while the project exists. Demo reset retains
retired copies. Ordinary deletion cascades content then retries physical cleanup if
needed. UUID deletion records/markers remain indefinitely without film content.
Temporary WAV/staging cleanup is immediate after normal work; abrupt process loss
and uncertain DB failure can leave orphans for stopped-service operator reconciliation.
No compliance or zero-provider-retention claim is made.

## Dependency/configuration review and deferred risks

npm audit on September 15 reports zero advisories across 273 locked dependencies.
Resolved backend: Boot 4.1.1, Spring MVC 7.0.9, Tomcat 11.0.24, Jackson 3.1.5,
Hibernate 7.4.5.Final, pgJDBC 42.7.13, SnakeYAML 2.6. No broad upgrades performed.
The [pgJDBC release](https://jdbc.postgresql.org/changelogs/2026-07-06-42.7.13-release/)
and [Spring JPA advisory](https://spring.io/security/cve-2026-47834/) were reviewed:
resolved Spring Data JPA 4.1.1 includes the fix, and SceneProof accepts no
client-controlled Sort/native-query expression.

Tomcat 11.0.24 is in affected ranges for advisories fixed in 11.0.25. The reviewed
paths involve HTTP/2 (CVE-2026-68763, CVE-2026-65637), container authentication/role
constraints, RewriteValve, authenticated WebSockets/examples and Unix sockets.
None is enabled by this application's default embedded HTTP/1.1 topology. Keep those
features disabled; patch-version upgrade and packaged deployment validation remain
an explicit dependency follow-up before changing topology. This is an applicability
assessment, not a clean backend CVE scan. [Apache advisory](https://tomcat.apache.org/security-11).

P2/deferred: automated backend/native dependency scanning, host FFmpeg/JDK updates,
periodic disk/orphan monitoring, deletion-marker retention and multi-process media
coordination. FFmpeg protocol/size/time/stream constraints reduce input exposure but
do not sandbox native decoder vulnerabilities. Root/parents must be operator-owned;
malicious local link swaps/hard links and external disk writers are outside the trust
boundary. Disk pressure rejects ingestion but is not a cumulative content quota.

## Demo/deployment boundary

Supported release remains one operator-controlled API process with trusted local
storage and a shared workspace. Anyone reaching it can read/modify project data and
consume shared admission. Public multi-user access still requires a separate
authentication/isolation/deployment decision; PR10 does not silently implement it.
For September 18, use trusted demo access and the operator switch where appropriate.
Longer-term isolation, fair quotas and persistent-host operations are outside this PR.

Vite is a loopback development server. Its headers are not embedded into dist assets.
A production static host/reverse proxy must set its own no-sniff, no-referrer, frame
denial and CSP headers, preserve the public Host, and provide a trusted scheme setup
for same-origin checks. Do not trust arbitrary forwarded headers. API CSP intentionally
allows no scripts; the static UI needs its own reviewed script/style/image policy
(Mantine uses inline styles). TLS/HSTS/public proxy setup is not implemented here.
Build output has no production source maps by default; packaged test routes/config
and source/key scans are included in final artifact verification.
