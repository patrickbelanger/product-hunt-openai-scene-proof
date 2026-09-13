# PR5 review handoff — 2026-09-13

## Branch and scope

- Branch: `feat/p1-reference-bible`.
- Base/main: `0901f4b7e4aa183c5f4fe5d1f2b85c07edee0737`.
- PR #4 merge verified after fetch, parents a821d89/a711d4a; clean main fast-forwarded
  to the exact requested SHA before creating only PR5. Prior handoffs remain historical.
- Committed and pushed implementation: `72c84821cca402f642a041aef596944c511acf88`.
  Documentation recovery verified a clean worktree and matching origin branch at
  that SHA, two commits ahead of main and zero behind, after fetching origin.
- This documentation-only follow-up corrects the original uncommitted/unpushed
  handoff state. Historical verification below is preserved; tests, builds and the
  paid Astra smoke were not rerun. Only documentation/diff and Git-state checks apply
  to this follow-up. Main remains unchanged; PR5 is not merged.
- Review required. Do not merge, start PR6, or add discovery, NLE, larger-video upload,
  demo/tour, hosting, auth, workers, progress or general Product Hunt polish.

## Model, persistence and storage

V5 `V5__reference_bible.sql` adds visual_references, analysis_references and
finding_references. Reference UUID is server-generated and belongs to exactly one
project. Title (120) is required/trimmed; guidance (2000) is optional/trimmed. No
taxonomy. Image identity, dimensions, SHA-256 and createdAt are immutable. Active
text edits affect future sequence analyses only. Archive is irreversible, freezes
metadata, and removes the image from the current Bible while preserving inspection.
Replace by archive plus new reference UUID; no hard-delete API or in-place replacement.

Submitted metadata is copied into append-only relational analysis_references and
AnalysisRun.context. Findings link their own run's submitted references using composite
foreign keys, preserving run/project ownership. Snapshots record IDs/title/guidance/
dimensions/hash only, never base64, bytes, storage paths or raw provider requests.
The read endpoint for finding references returns original submitted metadata, with
current archive status as an annotation. Existing zero-reference histories work.

Reuse PR1 MediaStorage/LocalMediaStorage, ImageNormalizer, shared ImageContent bounded
copy/signatures/normalized reads, and MediaIngestionGate. Files are generated UUID
directories `<MEDIA_ROOT>/<project>/<reference>/original.bin` and `00.png`. Reference
is not a Shot/Frame. Normalized delivery is project-scoped, hash/decode/dimension
checked, bounded, PNG with nosniff. Binaries never enter PostgreSQL. Normal failures
compensate files and persist no reference; abrupt termination can leave an orphan.

## API and budgets

OpenAPI remains authoritative; the TypeScript client is regenerated and used by UI.
All paths start `/api/v1/projects/{projectId}` (rules uses `id` in OpenAPI):

| Operation | Path / behavior |
| --- | --- |
| PUT | `/rules`, only bounded rules update; empty permitted |
| GET / POST | `/references`, active list / JPEG-PNG multipart file/title/guidance |
| GET / PUT | `/references/{referenceId}`, inspect / edit active title and guidance |
| POST | `/references/{referenceId}/archive`, idempotent permanent archive |
| GET | `/references/{referenceId}/content`, immutable normalized PNG including archives |
| GET | `/findings/{findingId}/references`, original cited metadata and image identity |

Eight active, 100 successfully persisted lifetime references per project; archive
counts toward lifetime capacity. Project row lock serializes admission. Active order
is creation time then UUID. All active references at assembly are submitted; no
selection engine. Metadata edits/archive after assembly do not change its inputs.
Rules only, references only, both and neither are supported project configurations.
No edit triggers analysis. Initial analysis remains explicitly API-started.

JPEG/PNG input: 10 MiB, 16 MP, 8192 pixels/side. Normalized PNG: 1600/side. Shared
shot/reference ingestion admission: one per process. Existing video limits unchanged.
Analysis retains eight shots, three frames/shot, 24 frames; reference images add at
most eight images and share 8 MiB/image, 16 MiB aggregate, 24 MiB serialized request.
Response 256 KiB, structured output 128 KiB, 6000 output tokens, 120-second provider
deadline, one RUNNING analysis globally and zero automatic retries remain unchanged.

## Astra and PR4 compatibility

AnalysisContext adds typed AnalysisReference values. Sequence assembly loads every
active reference, validates image content/hash and applies the combined byte budget.
Missing/corrupt required images fail the run before provider work. Request content
places explicit REFERENCE metadata/images before marked SEQUENCE_SHOT/frame evidence.
Titles/guidance/image text remain untrusted scene data. The prompt explains declared
truth, independent judgement, relevant comparison and contextual differences: angle,
light, occlusion, narrative changes and creator intent. No tools or external navigation.

Continuity schema v1 now accepts zero-to-eight distinct relevantReferenceIds, only
from the actual submitted context/project. Invented, foreign, archived/unsubmitted,
duplicate or excess citations fail. Shot/frame evidence invariants and server IDs
remain unchanged. Finding/reference association and SUCCEEDED persist atomically.
Provider failure/replay safeguards remain PR2/PR4, including usage and durable failures.

Targeted PR4 uses original cited reference metadata/hash/image, including archives,
not replacement/current references. It retains original finding/frame evidence,
creator explanation/scope, previous judgement and bounded neighbors. Original rules
are separately identified from current rules and recorded in targeted context. The
targeted output schema is unchanged. Sequence reasoning LOW; targeted reasoning HIGH.

## Workspace UX

The existing dark Reference Bible now has rules editing/Save, truthful pending/saved/
errors and draft retention, normalized visual cards and an Add modal. Add takes file,
title/guidance; real upload/validation pending prevents duplicate work or premature
dismissal. Failure retains input. Inspect edits metadata or confirms permanent archive.
Close restores focus to the invoking card/button, or Add if archive removed the card.
List failure has independent retry; editing never calls the provider.

Findings with citations show a separate Reference evidence block with original image,
title/guidance and archive annotation. The primary shot A/B comparison stays unchanged.
No citations means no fabricated block. Historical list/image failures are explicit,
never replaced by current Bible content. URL selection and reload preserve association.

## Deterministic verification

| Check | Result |
| --- | --- |
| Gradle build/backend | PASS: 71 tests, 0 failed, 0 skipped |
| Frontend Vitest/RTL | PASS: 44 tests, including 13 Reference Bible tests |
| Chromium | PASS: 10 tests, including two PR5 flows |
| TypeScript/Vite build | PASS after final focus correction |
| OpenAPI generated drift | PASS |
| Git diff whitespace | PASS |
| Remote CI | Not checked in this follow-up; not run at the original unpushed handoff |

Backend adds 16 reference API/persistence tests, one schema citation test and two
adapter/budget tests. They cover PNG/JPEG/corruption/spoofing, bounds, storage naming,
ownership, metadata/archive, capacity, rules persistence/empty, snapshots, accepted
and invalid citations, DB links, replay, zero references, targeted original references/
rules/previous judgement and missing content. Existing PR1/PR2/PR4 tests stay green.
PR5 uses only sceneproof_reference_test cleanup; PR4 cleanup includes the new FK
tables in its own dedicated schema. No normal test uses OpenAI.

Chromium creates a real PNG, edits rules, uploads, reloads, creates real shot evidence,
calls only the test-classpath deterministic port, reads a cited finding, edits metadata,
archives, reloads and re-evaluates the original referenced finding. It validates OpenAPI,
actual image decoding, zero browser inference on edits, tablet/mobile overflow and
keyboard focus. The second PR5 flow checks rules failure, corrupt upload and modal
focus restoration. Desktop/tablet screenshots were inspected under test-results.

Earlier failures are not counted as passes: sandbox JDK/Chromium access; nested
Mockito matcher in the new fixture; React sibling key collisions; a keyboard test
attempting to focus before active-list load; and an actual modal focus-restoration
defect found by Chromium and corrected. Final Chromium suite passes all ten tests.

## Real Astra smoke

One real `gpt-6-astra` sequence call, after green deterministic checks. Original red
reference plus two original blue sequence PNGs; empty rules prove the references-only
configuration. The manifest was written before inference; no reroll or second request
UUID. Astra returned one valid finding citing the actual submitted reference.

- Project: `8c0481a2-4391-4e3c-88d1-b52264e5b310`.
- Request: `ffb28bdf-ff1a-4235-973e-dac2d7ab4905`.
- Analysis: `e7902a2e-40ad-4654-a41e-ca8f304f4272`, SUCCEEDED.
- Reference: `0a19a096-4a27-4168-9a8a-25b50bd69de7`.
- Finding: `f0658e4c-31eb-46a6-87e8-aed4dc2dd955`.
- Image SHA-256: `12785b79e1548bc4aa3c4cb4707b212b70caecb2a4c09bea271dc65ceaba563b`.
- Usage: 1,360 input / 653 output / 2,013 total; 0 cached input, 1,357 cache-write,
  0 reported reasoning tokens. Sequence reasoning remains LOW.
- Duration: 12.878 seconds including persistence/replay checks; database run duration
  12.726971 seconds.
- Estimated standard-rate cost: **USD $0.0496425**, approximately $0.050, not a receipt.
  Formula: `(3 × $10 + 1357 × $12.50 + 653 × $50) / 1,000,000`, using the
  [official model rates](https://developers.openai.com/api/docs/models/gpt-6-astra).

Real strict output/IDs validated, association persisted, same-request replay returned
the same run without another inference. PostgreSQL read confirmed submitted UUID,
title/guidance/dimensions/hash and only one run for this smoke project. After stopping
the key-bearing jar, restarted the normal jar with OPENAI_API_KEY removed before Java
launch. GET-only `--verify` recovered the same finding/citation and image hash in 785 ms.
No second paid call was made. This proves the small integration path, not film accuracy.

Separate Chromium inspection of that real result after the no-key restart decoded
both blue shot images and the red cited reference, restored the association after
reload, and checked tablet overflow. API writes were blocked; zero attempted writes
and zero browser errors were observed. Desktop/tablet captures were visually inspected:
`test-results/references-real-astra-desktop.png` and
`test-results/references-real-astra-tablet.png`. These are local ignored artifacts.
The normal review workspace remains on `http://127.0.0.1:15183`, proxying jar 8095
(PID 27504), with no OpenAI key. The deterministic API 8094 was stopped.
Final whitespace checks include new untracked source files. The production jar
contains no browser provider, browser/reference test profile or ReferenceApiTest.

The full frontend suite passed 44 tests before the final focus correction; the
13 reference tests and production build were rerun after it. The complete ten-test
Chromium suite also passed after that correction. No skipped check is counted as passed.

## Limits and reviewer focus

- Local/unauthenticated; project-scoped ownership is not authentication or public tenancy.
- Eight active references/100 lifetime records, no archive browser or restore/delete;
  archived items remain available by ID and through cited findings.
- Metadata snapshots are analysis-scoped, not a CMS editing journal. Last saved current
  text wins; concurrent multi-user editing is outside this local slice.
- Archive then upload is two explicit operations. A failed replacement upload leaves
  the old item archived and the error visible; historical evidence is still preserved.
- Filesystem/DB are not one transaction. No orphan collector, public disk quota or
  historical media garbage collection. Missing historical bytes fail safely.
- References consume the existing byte budget and may make a formerly fitting sequence
  too large; rejection is explicit. Sampling/vision accuracy limitations remain.
- Review V5 composite ownership/immutability, submitted-vs-current metadata joins,
  targeted reference selection, shared byte limits, prompt independence and focus first.

ADR-0006 documents the delegated material choice, alternatives and unchanged September
18 target. README, STATUS, PRODUCT, DOMAIN, ARCHITECTURE, UX, ASTRA-INTEGRATION,
MEDIA-PIPELINE, plans, DECISIONS, ADR index and historical PR4 review are synchronized.
