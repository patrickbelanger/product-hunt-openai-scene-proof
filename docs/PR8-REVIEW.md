# PR8 review — AI Film Understanding / Multimodal Continuity Discovery

Date: September 14, 2026 (Toronto). Branch: `feat/p1-ai-film-understanding`.
Exact base: `ed6066283d27da97484425f900b6bccb447b0911`, verified PR7 merge #7.
Final pushed HEAD is reported in the review handoff rather than embedding a
self-referential commit SHA in this file. Do not merge or start PR9.

## Recovery and plan reconciliation

Recovery inspected the actual clean worktree, fetched origin, fast-forwarded local
main to the exact verified origin/main SHA, checked merge parents/recent commits,
V6 demo/reset, media/storage, Astra lifecycle, Reference Bible, finding/history,
OpenAPI/client and baseline checks. Only the authorized PR8 branch was created.
The interrupted implementation was resumed in place without reverting or re-scaffolding.

`docs/intial-context.md` retains its original filename and unrelated historical
material. Updated priorities, detailed branch scope and merge order now say:
PR7 demo merged → PR8 Film Understanding plus essential truthful analysis progress
→ PR9 general UX → optional P2 launch cosmetics. The September 14 evolution records
the two real-film needs: AI work was invisible and continuity needs visual plus
narrative/audio context. “This order is a plan, not a prison” remains.

The six existing documentation occurrences of `feat/p1-analysis-progress` were
reconciled: initial context, development plan, implementation plan, STATUS twice and
PR7 review. They remain only as explicitly historical/superseded explanations.
There is no standalone progress branch. Product/domain/architecture/UX/Astra/media,
demo/launch docs, decisions, ADR-0008, plans and README describe the actual PR8 split.

## Source, persistence and APIs

- SourceFilm: one immutable project-owned original, hash/size/name/duration; same-byte
  upload recovers identity, a different source requires another project. MP4/H.264,
  100 MiB, 120 seconds, existing dimension/fps limits, no new NLE/source-limit scope.
- Real FFmpeg scene-change/periodic extraction, max side 768, at most 24 frames grouped
  deterministically into eight segments. Normal Shot/Frame storage and APIs are reused.
  Segment/source times are explicit; groups are not claimed to be editorial cuts.
  Separate imported clips are retained, but source segments take precedence in later
  sequence analysis. Up to 16 clips may be visible in an understood PR7 demo copy.
- V7: `source_films`, `film_understanding_runs`, `film_understanding_stages`,
  `film_segments`, `film_transcript_segments`, `film_candidates`; findings gain
  `film_evidence`. Strict bounded nested discovery remains JSON, not dozens of tables.
- V8: immutable original proposals/terminal creator decisions/completed results;
  confirmed-only same-project, uniquely linked reference provenance. Source,
  segment and transcript evidence is immutable. Existing V1–V6 remain unchanged.
- Eight film paths under `/api/v1/projects/{projectId}/film`: GET intelligence;
  POST `/source`; POST `/runs` with UUID/source/paidConsent; GET `/runs/{runId}`;
  GET run `/transcript` and `/candidates`; POST candidate `/decision` and `/reference`.
  Film reads use no-store and project ownership. Finding evidence and reference
  provenance extend existing APIs; generated TypeScript/OpenAPI are checked in.

## Provider, timestamps and bounded cost

AudioTranscriptionPort / OpenAiAudioTranscriptionAdapter use the verified current
`/v1/audio/transcriptions`, `whisper-1`, verbose JSON and segment timestamps.
FFmpeg extracts mono PCM s16le/16 kHz, max 120 seconds/4 MB, common container origin.
The actual PCM sample duration, audio hash and transcription request ID are stored
on successful transcription; temporary WAV is deleted. No audio stream makes no
transcription call. Failed transcription prevents Film Understanding.

Decoded video times account for video-stream/container-start offset. Transcript
seconds are validated, rounded to ms and assigned server UUIDs; provenance labels
them approximate Whisper segment estimates, never exact alignment, speaker proof
or literal evidence that a lyric happened. Source frames supplied to later continuity
also use common source time. Narrative interpretation is separately labelled.

FilmUnderstandingPort / AstraFilmUnderstandingAdapter use Responses `gpt-6-astra`,
reasoning **medium**, 6,000 output tokens, strict schema, store=false, no tools or
automatic retry. Existing sequence **low** and targeted **high** are unchanged.
OpenAiResponsesTransport is shared HTTP plumbing, not a merged provider abstraction.

Strict output includes summary, exact project/source/inspected IDs, recurring
entities, proposed anchors, narrative cues, potential concerns and warnings.
Unknown/duplicate IDs, wrong coverage, unsupported evidence, invalid JSON and
oversized results fail closed. Max 12 entries/category, 200 transcript segments /
24,000 characters, 16 MiB selected PNGs, 24 MiB request. Both providers have bounded
120-second requests; no recursive orchestration or background retry loop.

All transcript/visible text is untrusted content. Discovery receives no project
rules, curation notes, evaluation answer or demo filename. Lyrics are context, not
automatic truth. Raw transcript/result artifacts stay in ignored local storage;
GETs are no-store and exception logging excludes sensitive messages/causes.
OpenAI data controls still apply; store=false does not promise zero retention.

## Real stages and authority

PREPARING_SOURCE → DETECTING_STRUCTURE → TRANSCRIBING_AUDIO → UNDERSTANDING_FILM
→ BUILDING_CANDIDATES → SUCCEEDED, or FAILED at the actual failure boundary.
Each real stage is committed before its work. Candidates and success commit atomically.
Backend timestamps/state are authoritative; there are no percentages, simulated
thoughts, arbitrary completion timers or private chain-of-thought displays.

Short reservation transactions and shared DB admission permit one globally active
analysis; a virtual-thread worker runs outside the transaction. Same UUID/source
replays return the existing attempt. Ten film attempts/project. Browser storage
records consent/request identity before POST; active GET polling every 1.5 seconds
restores state after reload/connection loss. SSE was unnecessary complexity here.
Process loss does not resume inference: GET/start expires the durable run after ten
minutes, visibly FAILED, with no paid replay. New attempts require fresh consent.

Observation → candidate invariant → creator confirmation → continuity enforcement.
Pending/rejected proposals never become rules. Accept preserves proposal wording;
Edit records creator title/rule/scope; Reject retains the original proposal/evidence.
Decisions are one-way and replay-safe. At most 24 accepted/edited anchors/project.
Visual promotion creates an ordinary reference from a cited frame through normal
ReferenceService, with explicit scope/provenance and existing eight-active limit.
Reference edits/archives do not rewrite the originating candidate or earlier analysis.

Later continuity snapshots confirmed anchors and the latest successful transcript/
narrative evidence. Findings store cited cross-modal snapshots; targeted review uses
the original memory even after another discovery. Discovery concerns are not
automatically Findings and zero candidates/concerns is a valid result.

## Demo and deterministic evidence

New/reset `between-the-line-v1` copies include the complete approved source in
addition to unchanged authored clips/references/rules. Old PR7 copies are not silently
mutated: reset or upload the original. Reset allocates fresh identities without runs,
transcripts, anchors or findings; retired copies keep history. No model baseline is
installed. The original black tail and complete audio are not trimmed for evaluation.

Backend: **102 tests**, zero failures/errors/skips; full Gradle build passes, including
V7/V8 migration and concurrent reference-provenance reads. Frontend: **78 tests**;
TypeScript/Vite production build and OpenAPI generation/drift pass. **19 Chromium
tests pass** in the final complete run. PR8 1280/820/390 screenshots were inspected
for legibility, genuine state/evidence and no horizontal overflow. They use clearly
labelled deterministic test output, never a claimed real-provider demonstration.
Normal tests use mocks/test-only ports and an empty key: zero OpenAI calls.
The production jar contains the source but excludes curation/evaluation/test providers.

PR8 deterministic coverage includes real FFmpeg audio/video, strict schema/evidence,
ownership/consent, deduplication/concurrency, interruption/late writes, audio failure,
silent sources, candidate decisions/promotion, cached structure on new attempts,
immutable cross-modal finding/context provenance and frontend recovery/failures.
Chromium exercises real full-source upload, reload during backend stages, evidence,
creator confirmation/promotion and demo reset; existing flows are regression tested.

Corrections during verification: integration-test truncation dependencies after V7;
Kotlin test helper naming; frontend modal timing/duplicate text and updated mocks;
unbounded root npm forwarding caused timeout failures, then explicit workspace
`--maxWorkers=2` passed; unused test import removed for TypeScript; legacy E2E upload
locator scoped to Media workspace after the second file input was added. A real
concurrent reference-read pool exhaustion exposed nested JDBC connection acquisition;
read-only transaction reuse and a concurrent regression check fix the cause.
Sandbox Chromium launch failed; successful browser runs use approved unrestricted
Chromium, still against the deterministic local API. Failed runs are not counted as passes.

## Single real provider validation

All deterministic gates were green before the single authorized attempt.
`scripts/astra-film-validation.mjs --execute-paid` requires explicit invocation,
creates an exclusive retained attempt lock/manifest before paid work and never
resubmits. `--verify` uses GET only. Local results include actual stages, transcript,
provider identifiers and usage where available, not a staged answer.

- Project: `69d24a43-905a-4ba1-9935-6509526023f5`.
- Source: `aec45966-70ac-45a1-8d8c-b93a0b0e7fed`, approved original SHA-256
  `ac9b29c47eeb399dbd1ac5cdfcde19273e0e4e68f5df8fd67fedbef3d284a7a7`.
- Request: `fc9ed577-faf3-4352-bcd5-6c93e6107144`.
- Run: `f8a8960c-a396-4524-b810-5b38661ed176`.
- Started `2026-09-14T04:36:47.184156Z`; failed `2026-09-14T04:36:55.637440Z`
  (8.453 seconds). Source verified and eight segments/24 frames prepared. Actual
  stage history is PREPARING_SOURCE, DETECTING_STRUCTURE, TRANSCRIBING_AUDIO, FAILED.
- **One transcription HTTP attempt; OpenAI returned a non-success response.**
  Failure code `TRANSCRIPTION_REJECTED`. Provider request ID
  `req_94b48bab6c4c4fd78c601da555e0bdc2` is retained in the run's generic
  `providerRequestId` field for this upstream failure, not an Astra request.
- **Zero Astra Film Understanding calls**, zero transcript segments/candidates,
  null provider response/usage. No candidates were accepted/promoted on this real
  copy and no continuity analysis or targeted reanalysis was invoked.
- GET-only `--verify` confirms the same failed run and empty output; it exits nonzero
  deliberately because provider validation did not succeed. No second POST, reroll,
  synthetic transcript, hard-coded finding or visual-only bypass was attempted.

The original `af42338` adapter did not log/retain raw error bodies or persist the
rejection's HTTP status separately. This evidence cannot distinguish access, credit,
format or another provider rejection reason. Do not claim a diagnosed cause.
Successful real transcription and real Astra multimodal schema acceptance therefore
remain **unverified**, despite green deterministic tests. Investigate the recorded
request ID with the provider before requesting authorization for any future attempt.

### Between the Line observations and costs

The original full 92.459-second source (including its black tail) passed real local
ingestion/structure extraction. There is **no real AI film summary or multimodal
observation to evaluate** because transcription failed first. The screen-opening
relationship was **not evaluated**, not naturally identified and not injected. Do
not label it a model miss, detection, proven inconsistency or accuracy score.
PR7's historical zero-finding visual validation remains a separate result.

Astra usage/cost: no request, no tokens, **$0**. Transcription charged cost is
**unknown**, not reported as zero: the response was rejected and no usage/billing
receipt is available. For budget context only, published Whisper pricing is
$0.006/minute; a successful 92.459-second submission would be approximately
$0.009246, not a charge observed here. Verified standard short-context Astra rates
are $10/M input, $1/M cached input, $12.50/M cache writes and $50/M output; no Astra
estimate is applied to the nonexistent call. Source: official
[API pricing](https://developers.openai.com/api/docs/pricing), checked September 14.

Private attempt lock/manifest/result and process logs remain ignored under `.local/`.
No transcript is committed or logged. Do not delete the lock to create another run.

## Known limits and reviewer focus

Sampled images can miss short actions, small UI text and brief transitions; a long
black tail consumes part of the unchanged sample budget. Transcript may hallucinate,
mishear lyrics or misalign estimates. No claim of perfect film understanding/accuracy.
Creator decisions cannot yet be revoked/edited after confirmation. Source replacement,
NLE editing, multi-hour orchestration and public auth/tenancy/billing are out of scope.
Ten-minute lazy interruption recovery is not a durable distributed job queue.
Stored originals/transcripts/history have no automated retention purge; staged PNGs
can remain after failures. Local unauthenticated privacy is not public isolation.
Vite reports a ~519 kB minified entry chunk warning; no cosmetic chunk-limit override.
Remote CI and public deployment are not verified by local checks.

Review especially: audio/video timestamp origin, finite evidence bounds and strict
output coverage; candidate versus confirmed authority and immutable reference/history
provenance; stage transaction boundaries and no-reroll recovery; transcript privacy;
sampling limitations versus the honest Between the Line observation; PR9 separation.

## Rejection-review follow-up

[Focused transcription review](PR8-TRANSCRIPTION-REVIEW.md) classifies the original
failure as **INSUFFICIENT_EVIDENCE**. Request/audio contract review identifies no
proven rejection-causing defect; availability is verified, not presumed absent.
The confirmed observability defect is corrected with bounded allowlisted rejection
details using existing durable fields and deterministic coverage. No original
failure was rewritten and no new live request was made during that diagnostic review.
Patrick's subsequent, separately authorized corrective attempt is recorded below.

## Single authorized corrective transcription-only validation

Tested clean HEAD `39009964927604cfe0a7e15e3dae99d2f4c2c909`; new isolated identity
`87b0d170-3387-4959-8711-a2ef647f20ca`. The current corrected adapter was invoked
exactly once with the reviewed bounded WAV, after verifying the server key and audio
metadata. No application worker/FilmUnderstandingRun was started; Astra could not
follow. The original failed run and attempt artifacts/lock remain unchanged.

Result: **TRANSCRIPTION_UNAVAILABLE**, **423 ms**, starting
`2026-09-14T11:24:13.284144300Z`. No upstream HTTP status or request ID was exposed;
local 503 is an application status only. Provider receipt is unconfirmed. No transcript
completion/segment count/timestamp validation; usage and charged cost unknown.
Classification: **INSUFFICIENT_EVIDENCE**, not a proven rejection or parser defect.
Exactly one invocation, no retry, **zero Astra calls**. The single authorization is
consumed. Safe metadata and a new permanent lock are retained privately; WAV deleted.

No application code changes and no deterministic test/build rerun; prior green gates
apply to the unchanged code. Documentation diff check passes. Full details and
artifact names: [transcription review](PR8-TRANSCRIPTION-REVIEW.md).
**Merge readiness remains limited:** real transcription transport/parsing and real
Astra Film Understanding are still unverified. Stop for Patrick's review, not another
request, merge or PR9. No film-discovery or screen-opening observation is claimed.
