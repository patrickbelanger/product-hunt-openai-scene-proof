# PR7 review — populated demo film

Date: 2026-09-13 (Toronto; live validation on September 14 UTC).
Branch: `feat/p1-demo-project`.
Exact base: `1e3f28fb162bcdc9d608c40d61d7fcfbcad97909`, the verified PR6 merge.
Verified implementation HEAD: `5eac0b9ba3fc9b2a91b0e9467102eb8e11c781ac`, committed
and pushed to origin. This documentation-only follow-up records that immutable review
point; the final handoff reports the exact pushed tip including this follow-up.
No merge or PR8 work is authorized by this handoff.

## Post-review planning reconciliation

Starting from pushed HEAD `9659236fd9dc8cf62c46cb899d7742018a4e5462`, Patrick accepts
the honest zero-finding validation as PR7 evidence of independent analysis alongside
the populated-demo/reset behavior. PR7 runtime, assets, results, acceptance and
recorded verification are unchanged; this is not an accuracy claim or a finding
showcase, and it triggers no provider call or reroll.

After PR7 merge, the next planned slice is **PR8 — AI Film Understanding / Multimodal
Continuity Discovery**, branch `feat/p1-ai-film-understanding`. It absorbs minimal
real analysis-stage visibility previously assigned to `feat/p1-analysis-progress`:
durable backend stages, truthful UI state, active-run reload/recovery and pipeline
failure visibility. No fake percentages; backend state stays authoritative.
The old standalone progress slice is superseded, not silently retained as next.
Generalized retries, unrelated failure polish and public/launch hardening do not
automatically move into PR8. This follow-up changes planning only, not PR7 scope.
PR8 must not start until PR7 is merged; this handoff does not merge PR7.

## Recovery and scope

Recovered actual Git state, fetched origin, inspected recent commits, required docs,
recovery sections 34–44, ADRs and existing backend/frontend/media/test implementation.
The initial worktree was clean. Patrick then supplied/staged the source film; that
exact asset was preserved across clean main fast-forward and creation of only PR7.
Main was verified at the exact requested SHA before branching. PR6's original review
facts remain historical; current documents explicitly recognize its merge.

This is the real application populated from a controlled template, not frontend fake
DTOs or hand-authored AI results. No upload, project form, authentication or provider
key is required to enter it. Existing analysis/steering, Reference Bible and optional
tour remain the real product. No public deployment, tenancy, spend controls, progress
transport, upload-limit changes or post-PR7 scene-management features were added.

## Approved assets and exact authored content

Template: **between-the-line-v1**. Title: **Between the Line — Continuity Study**.
Source: `demo/between the lines - demo.mp4`, 42,842,998 bytes, SHA-256
`ac9b29c47eeb399dbd1ac5cdfcde19273e0e4e68f5df8fd67fedbef3d284a7a7`.
Patrick explicitly confirmed this is his original/generated film using references he
generated, authorized for SceneProof and the public Product Hunt demo. Attribution:
**Laurie and Patrick**. The conversational filename `Between the lines - 4(1).mp4`
refers to this supplied source; identity is pinned by the actual path/hash.
No arbitrary web imagery, neighboring creative files or third-party project code.

The original 1080×1920, 24 fps H.264 source lasts 92.458667 seconds; picture ends at
36.291667 followed by black. The unmodified source is retained. Eight H.264 derivatives
exclude that tail, omit audio, preserve source order and use full-frame 432×768 scaling.
Five full-frame PNG references use 540×960. No visual objects, text or drift were
manufactured. `demo/curation.json` records boundaries, provenance and human notes;
`scripts/prepare-demo.mjs` verifies the source and generates the hash-pinned runtime
manifest/assets. FFmpeg build used: N-120856-g9893d66add-20250831. Other FFmpeg builds
may produce different bytes; review regenerated outputs rather than silently swapping
assets in an existing template version.

| Order / runtime file | Persisted clip | Source seconds | Frames in validated copy |
| --- | --- | --- | --- |
| 1 / shot-01.mp4 | Metro platform | 0–3.208333 | 20 |
| 2 / shot-02.mp4 | Inside the carriage | 3.208333–8.916667 | 23 |
| 3 / shot-03.mp4 | Phone and passenger intercut | 8.916667–15.541667 | 23 |
| 4 / shot-04.mp4 | Patrick with his phone | 15.541667–17.541667 | 24 |
| 5 / shot-05.mp4 | City transition | 17.541667–20.5 | 24 |
| 6 / shot-06.mp4 | At home | 20.5–24.791667 | 21 |
| 7 / shot-07.mp4 | Device on the sofa | 24.791667–27.375 | 21 |
| 8 / shot-08.mp4 | At the workstation | 27.375–36.291667 | 24 |

These are authored import segments, not a claim of automatic edit detection; clip 3
retains the original intercuts. The validated copy contains 180 persisted frames;
unchanged analysis selects first/middle/last per clip, 24 frames total. Frame timestamps
are decoder-selected clip-relative values. IDs and asset ownership are fresh per copy.

| Runtime reference | Title | Source second | Guidance |
| --- | --- | --- | --- |
| reference-01.png | Patrick — metro appearance | 4 | Principal character in the metro scene. |
| reference-02.png | Metro platform | 1 | Location reference for the opening scene. |
| reference-03.png | Handheld phone | 10 | Device and visible screen in the metro scene. |
| reference-04.png | Apartment sitting area | 22 | Location and character reference for the apartment scene. |
| reference-05.png | Apartment workstation | 32 | Workstation and screens in the apartment. |

All 13 files live in `demo/runtime`; their exact hashes are in `manifest.json`.
Normal normalization produces the persisted reference PNGs/hashes; these need not
equal the encoded source PNG hashes. The Bible uses the normal persisted reference API.

Authored description: “A journey through the metro and into an apartment, told through
Patrick and the screens around him. Original film by Laurie and Patrick.”

Authored rules: “Patrick is the principal character throughout the sequence. The metro
and apartment are separate scenes in his journey. Use the Reference Bible as visual
context for the character, locations and devices.” No rules state expected defects.
Actual dark/navy metro polo supersedes the historical black-blazer assumption; the
apartment clothing is a deliberate scene/time transition, not a manufactured error.

## Lifecycle, database and API

ADR-0007 chooses immutable packaged inputs plus a normal Project working copy. A
sessionStorage UUID is the local instance handle, not a public authorization boundary.
Same-handle creation serializes/replays; distinct handles have distinct current copies.
The template has no write endpoint and is validated by version, allowed filenames and
content hashes. A new authored version cannot silently reset an older version.

V6 `V6__demo_instances.sql` adds explicit instance UUID, template version and retirement
flag to Project, complete-identity checks, one-current-copy partial uniqueness and
the source/replacement mapping. Reference creation uses `clock_timestamp()` to preserve
authored order inside the outer seed transaction. Ordinary project identity stays null.

- POST `/api/v1/demo`, body `{ "requestId": "<UUID>" }`: create/recover a ready copy.
- POST `/api/v1/projects/{projectId}/demo/reset`: replace/recover that demo generation.
- Project response: nullable `demo: { instanceId, templateVersion, retired }`.
- Invalid identity: 400; ordinary-project reset: 409; preparation failure: safe 503.
- OpenAPI is updated and the TypeScript client regenerated; no generic delete API.

The server locks the instance and performs retirement, normal Project/Reference/Media
service ingestion and replacement mapping in one bounded 180-second transaction.
Partial seed metadata never appears ready. Failure rolls back retirement and the new
copy; retry retains the same handle. Only new assets confirmed to have no persisted
project are compensated; uncertain commit outcomes retain files rather than risk
deleting valid media. Existing media normalization/FFmpeg/persistence are reused.

Reset creates a fresh project UUID with original rules, five active references,
ordered shots/frames and no analysis/actions. Creator changes do not enter the new
copy. Old findings/actions and cited reference evidence remain with the retired copy,
hidden from the library but readable by URL. Reset retries, including retries after
later generations, return the current replacement. No ordinary/other instance or
immutable template is modified. History is not rewritten into new results.

## Landing and workspace UX

Both **Try the demo film** and **Create a project** are visible. Launch persists its
handle before POST, announces real preparation, prevents double-click duplication,
only navigates on ready data and retains the handle for actionable retry. Failed
session storage prevents an untracked launch. Cold preparation measured 28.252 seconds
on this local machine; there is no invented percentage or claim of instant decoding.

The real workspace shows a restrained Demo project badge, the film description and
honest no-recorded-findings explanation. Import is collapsed, not required. Typed
identity controls the badge/reset, never name matching. Confirmation describes lost
working-copy edits and retained history; safe cancellation has initial focus, pending
reset prevents dismissal/duplicates, failure can recover the same reset. Ordinary
projects have no Reset demo. The fresh heading receives keyboard focus after navigation.
PR6 invitation remains optional and independent of demo creation/storage.

No preloaded findings or cached analysis baseline ships. The one validation result
stays in its own local project; opening/resetting another demo never copies it.
Initial analysis remains the existing explicit API operation, not a new UI button.

## Evaluation-only separation

`demo/evaluation/expectations.json` contains current open questions for character,
device, legible UI and metro geography, plus the intentional apartment transition.
All seven original candidates remain historical with superseded/unconfirmed/unsupported
status; neither blazer drift nor the absent door/hand-switch is an expected detection.
There is no pre-established defect count, pre-accepted intent or forced model answer.

Gradle packages **only** `demo/runtime`: manifest plus 13 assets. Jar inspection confirms
no evaluation manifest, curation notes or test-provider classes. Runtime DemoTemplate
has only project/ref/shot authored inputs. AnalysisContextAssembler selects normal
project/media/reference fields, not demo/evaluation identity. Deterministic tests inspect
both serialized AnalysisContext and the real Astra request for evaluation keys and
every current case ID/question. They are absent; genuine rules/guidance remain present.
The actual packaged manifest's title/order/rules/hashes are independently checked.
The paid validation script never reads evaluation files. Provider/prompt/result code
is unchanged and no expected result is inserted as a Finding.

## Sole real Astra validation

All deterministic suites/builds passed before dispatch. Exactly **one** new full-
sequence provider request was made; no targeted request, reroll or post-result tuning.
Normal local API: 8098, PostgreSQL normal schema, key only in server environment.
The durable ignored request manifest was saved before dispatch and refuses another
`--run`; `--verify` subsequently retrieved the same persisted result using GET only.

| Field | Recorded value |
| --- | --- |
| Project | cd81cf05-6a42-4f83-9061-2201b85183c1 |
| Instance | 2cdad8a1-4007-4033-90d1-e7a6b550062f |
| Analysis run | 9b30f01d-54e6-4586-b7ac-63c0f2eb6880 |
| Analysis request | 570694fe-6d81-45dd-845d-1809944d0494 |
| Provider / model / kind | OPENAI / gpt-6-astra / SEQUENCE |
| Provider response | resp_08d5bba2226bab01016aa758b8b4c087d2b446cbdf63a3fb2b |
| Provider request | req_9926b6718b3a49f4ab2f500828740046 |
| Started UTC | 2026-09-14T02:15:12.458526Z |
| Completed UTC | 2026-09-14T02:15:41.167224Z |
| Status / findings | SUCCEEDED / 0 |
| Inputs | 8 shots, 24 frames, 5 references; 14,985,129 image bytes |
| Tokens | 16,861 input / 1,176 output / 18,037 total |
| Detail tokens | 0 cached input / 16,858 cache-write / 26 reasoning (within output) |
| Run duration | 28.709 seconds |
| Estimated standard cost | USD $0.269555 (approximately $0.26956), not a billing receipt |

Sequence reasoning remains the existing LOW configuration; targeted remains HIGH.
Cost uses 3 ordinary input tokens × $10/M, 16,858 cache-write tokens × $12.50/M,
and 1,176 output tokens × $50/M; reasoning is not double-counted. Rates verified against
the [official Astra model card](https://developers.openai.com/api/docs/models/gpt-6-astra).
Account/service-tier pricing may differ.

Exact returned summary:

> Reviewed 24 sampled frames across eight shots against the five supplied references.
> Patrick’s metro and apartment appearances remain consistent with their respective
> references. The city transition supports the change of location, lighting and wardrobe.
> No sufficiently evidenced continuity problems were identified.

Returned warnings:

- Temporal subsampling may miss brief continuity changes.
- Small interface text and partially occluded details cannot be fully verified at the supplied resolution.
- The apartment phone differs visibly from the metro phone, but the separate scenes do not establish that they must be the same device.

| Evaluation question | Actual behavior / reviewer interpretation |
| --- | --- |
| Character consistency | Summary judges scene-specific appearances consistent with references; no finding. No independently labelled defect exists to count as a miss. |
| Device consistency | Explicit warning notices a difference but does not assume identical device ownership across scenes; no defect asserted. This is a contextual observation, not a Finding. |
| Visible UI/text | Warns that small/occluded details cannot be fully verified; no text error detected. This question remains unresolved, not passed. |
| Metro geography | No spatial finding or specific geography conclusion beyond the overall summary. Unresolved rather than proven consistent. |
| Intentional apartment transition | Explicitly contextualizes wardrobe, lighting and location via city transition. No intentional difference incorrectly flagged. |
| Historical unsupported cases | No blazer/door case manufactured. Historical five-issue expectation is not a scoring denominator. |

True expected defect detections: none established. Expected established issues missed:
not computable without labelled supported defects. Unexpected findings/possible false
positives: none returned. This does not establish recall or accuracy. All selected
shot/frame coverage passes the existing result validator. The summary reports five
references used; no finding-level reference citations or comparative evidence were
returned, so evidence quality/reference-citation quality cannot be assessed here.
Actual input frames/reference images decode and remain associated with their project.
Full local run/shots/references/result are preserved in ignored
`.local/pr7-validation-result.json`; no raw secret/provider request is committed.

## Deterministic verification

| Check | Final result |
| --- | --- |
| Gradle `build`, Java 25 | PASS; 83 backend tests, 0 failures/errors/skips |
| Vitest `run --maxWorkers=2` in web workspace | PASS; 68 tests |
| `npm run test:e2e` | PASS; 17 Chromium tests, all PR1–PR6 flows retained |
| `npm run build` | PASS; TypeScript/Vite production build |
| `npm run api:check` | PASS; regenerated contract/client, no drift |
| `node --check` on both new scripts | PASS |
| Git diff whitespace check | PASS |
| 1280 / 820 / 390 widths | PASS; actual images decode, keyboard flow and no horizontal overflow; screenshots inspected |
| Packaged jar boundary | PASS; runtime manifest + 13 approved assets, no evaluation or test provider |
| Real-run GET-only/OpenAPI verification | PASS; same durable successful run, zero findings |
| Remote CI | Not checked; do not infer remote status from local checks |

Twelve new backend demo tests cover populated seed, exact rules/reference order and
content, explicit/versioned identity, concurrent idempotency, failed seed/retry,
replacement/history/ownership, ordinary-project rejection, repeat generations,
failed reset preserving edits, version mismatch and evaluation separation. Tiny
programmatically generated PNGs are test-only, never production demo substitutes.
The real packaged asset test verifies hashes and authored metadata without inference.

Nine frontend demo tests cover both landing paths, pending/duplicate prevention,
successful navigation, failure/retry identity, storage failure, typed identity,
confirmation/cancellation and reset success/failure. Existing tour tests stay green.
Two new Chromium tests cover the fresh-browser real film flow and API boundaries;
the full suite checks all prior workflows. The demo flow verifies actual reference
URLs/dimensions, eight READY videos, ordered frame selection, empty baseline, rules/
reference changes, confirmed reset/reload/recovery and an untouched ordinary project.
An explicit deterministic analysis exercises all eight clips/24 selected frames.
Browser API 8097 uses its isolated schema and test-only port, with no provider key.
Normal automated tests make **zero OpenAI calls**.

Earlier attempts are not counted as passes: sandbox JDK/browser access failures;
a Mockito restub error corrected with doReturn; a Jackson node iterable assertion
corrected to a list; existing frontend timeout under excessive parallel load rechecked
with two workers; and a steering E2E focus race fixed by waiting for the history-loaded
button to become enabled before keyboard input. No steering product behavior changed.
Deterministic real-film analysis first exposed unnecessary FFmpeg upscaling and image
budget overflow. The scaler now only decreases dimensions, consistent with existing
still normalization, and a 160×90 video regression test guards this. Derivatives were
resized before the paid call. No analysis bound was increased to make the demo pass.

## Review risks and launch decision

- Initial preparation holds a bounded DB transaction during decoding (~28 seconds locally); not public-concurrency architecture.
- Retired copies preserve audit/media and consume disk. Abrupt termination may leave orphan files; no generic retention/deletion system is introduced.
- The tab handle is local convenience, not anonymous production security. Different handles isolate data; possession of project IDs is not authorization.
- Eight segments/three sampled frames each can miss brief intercut events. Resizing limits fine UI text. References are derived contextual views, not separate canonical truth.
- Reset promises the installed original version and refuses version drift; no multi-version CMS or migration is implemented.
- Mobile is a vertically stacked workspace with the existing timeline design, not an NLE redesign. Initial analysis is still API-started.
- Zero findings means this film does not yet supply a demonstrated finding/evidence/steering sequence for a Product Hunt recording.

**DECISION REQUIRED — finding-focused launch recording (not a PR7 runtime blocker).**
Current choice: keep the approved film and truthful empty baseline. Problem: the sole
real run found no sufficiently supported issue, so the planned finding-focused recording
has no finding from this version. Proposed alternative: Patrick may approve a separate
future curation/asset-production and paid-validation plan. Alternatives: present this
version strictly as the populated workspace demonstration, or supply a different
explicitly authorized film for later review. Recommendation: review/accept PR7 on its
honest populated-project/reset merits; do not promise five findings or spend again
without approval. Implementation impact: no further PR7 runtime or prompt change.
September 18 impact: finding-focused recording remains dependent on that review;
public deployment/isolation/spend protection are still separate planned prerequisites.

Reviewer focus: asset authorization/curation; outer transaction and rollback/file
compensation; per-instance reset replay/history preservation; evaluation exclusion;
truthful empty findings and real validation limitations. No merge and no PR8 work.
