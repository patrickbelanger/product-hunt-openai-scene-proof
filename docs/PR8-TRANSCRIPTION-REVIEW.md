# PR8 transcription rejection review

## Final recovery — successful master flow

The subsequent UI run `104fed03-5210-4269-a6e8-169ced53f2ce` successfully transcribed
the 92.458667-second master using `gpt-4o-transcribe-diarize`, with six validated
source-relative segments (13.050–47.366 s), then completed Astra Film Understanding.
It does not validate the derived source live or reconstruct earlier errors.
[Final validation](PR8-FINAL-VALIDATION.md) records the exact IDs, additional historical
UI failures, unknown transcription billing and unretained exact success HTTP status.
Historical classifications/locks below remain unchanged. No finalization provider call.

Date: September 14, 2026. Branch: `feat/p1-ai-film-understanding`.
Reviewed implementation: `af42338d90934a9fdb6d304e43d0e418ae8d6484`.
The initial diagnostic review authorized/performed no new OpenAI inference/API request.
Patrick subsequently authorized one isolated corrective transcription-only attempt;
its separate outcome is recorded below. No retry, Astra call, merge or PR9 work follows.
Official documentation retrieval is separate from provider execution.

## Classification: INSUFFICIENT_EVIDENCE

Original run: `f8a8960c-a396-4524-b810-5b38661ed176`.
Request ID: `req_94b48bab6c4c4fd78c601da555e0bdc2`.
The immutable database row, saved `.local/pr8-film-result.json`, attempt manifest and
local provider-process/validation logs agree: TRANSCRIPTION_REJECTED, FAILED,
8.453 seconds end-to-end, no transcript/result/usage, zero subsequent Astra calls.
The original adapter generated that code only after receiving a non-2xx HTTP response.
Its timestamp/transcript parser runs only for 2xx, so a successful-response parsing
error or an ordinary pre-header timeout cannot explain this recorded rejection code.

The exact HTTP status and provider error body were discarded before failure
persistence. Logs contain the generic code, not a hidden copy of that response.
The temporary WAV was deleted by the existing `finally` cleanup. The original error
cannot be reconstructed from these artifacts or the local database; its status,
type/code/message, underlying cause and billed cost must not be invented.
Original run/artifacts/attempt lock remain unchanged.

**Confirmed implementation defect:** insufficient rejection observability. This is
not evidence that request construction caused the provider rejection. There is no
established basis to classify the original outcome as IMPLEMENTATION_DEFECT,
PROVIDER_OR_ACCOUNT_RESTRICTION or TRANSIENT_PROVIDER_FAILURE.
Whisper availability is verified by official documentation and Patrick's Playground
check; model unavailability is not treated as a likely explanation.

## Historical Whisper offline contract review

| Item | Observed implementation / conclusion |
| --- | --- |
| Endpoint/method | POST `https://api.openai.com/v1/audio/transcriptions`; unchanged. |
| Multipart | Generated ASCII boundary is shared by header/body; CRLF framing and closing delimiter; byte-array publisher preserves binary bytes and computes length. Exact deterministic byte comparison added. |
| File field | Exactly `file`; binary file object, not a path or a text filename. |
| Filename/MIME | `source.wav`, `audio/wav`; extension and format metadata present. |
| Model | `whisper-1`; no substitution or model-access assumption. |
| Format/timestamps | `response_format=verbose_json`, `timestamp_granularities[]=segment`; documented Whisper shape. |
| Audio | Original-source offline re-extraction using the existing arguments yields WAV PCM s16le, mono, 16 kHz, 16-bit, 92.458688 s, 2,958,756 bytes. Metadata only inspected; temporary audit audio deleted. This confirms reproducibility, not byte-for-byte proof of the deleted original submission. |
| Bounds | 4,000,000-byte PCM cap plus small multipart overhead is below the documented 25 MB file limit; source is ≤120 s. |
| Authorization | Server-side Bearer header, no secret exposed. No explicit organization/project override. Documentation describes these overrides for multi-organization/legacy-key routing; account routing cannot be proven from this failure. Their absence alone does not establish a defect. |
| Timeouts | 10 s connect / 120 s HTTP and completion deadline; redirects disabled, no retries. The original rejection is distinct from the unavailable/interrupted branches. |
| Response handling | Non-2xx previously collapsed to one generic failure and request ID. Successful responses still use bounded strict JSON and timestamp validation, unchanged. |

Sources checked: official [file transcription guide](https://developers.openai.com/api/docs/guides/speech-to-text),
[Whisper timestamps](https://developers.openai.com/api/docs/guides/speech-to-text#timestamps),
[transcription method reference](https://developers.openai.com/api/reference/resources/audio/subresources/transcriptions/methods/create),
and [authentication](https://developers.openai.com/api/reference/overview#authentication).
No request-format or audio-encoding defect was identified; architecture/request
parameters were not changed to try to force acceptance.

## Diagnostic correction

The adapter now preserves actual HTTP status, recognized safe error type/code and
an exact allowlisted safe provider message in the existing durable `failureMessage`
(≤500 characters). The existing `providerRequestId` carries the bounded `x-request-id`.
The local application's failure HTTP status remains 502; it is not confused with
the upstream status explicitly labelled `HTTP ...` in the saved detail.
No migration, new API field, provider interface, model, timestamp strategy or retry
policy is needed. Existing worker propagation, repository failure persistence and
GET/UI failure display carry the details unchanged.

Diagnostics parse at most 8 KiB of complete error JSON within the existing 256 KiB
transport body cap. Malformed, duplicate-key, trailing, non-JSON or oversized bodies
cannot erase a received status/request ID. Headers are captured before body reading,
so rejection metadata also survives a body-size failure, read timeout or interruption.
If no response headers arrived, no provider status is invented.

Privacy is fail-closed: only recognized bounded type/code tokens and exact approved
literal messages survive. Unknown identifiers/free-form messages, parameter values,
headers other than request ID, secrets, echoed audio/transcript, HTML and body prefixes
are withheld, not truncated into potentially sensitive fragments. Unknown details
are labelled unavailable/withheld, not replaced with a fictional provider message.
Request IDs are bounded ASCII `req_` values and reject control characters/key material.
No raw error body or exception cause is persisted/logged. This intentionally cannot
preserve every future provider diagnostic; new safe literals require reviewed tests.

## Deterministic validation

Added representative 400/401/403/429 (quota and rate limit)/500/503 diagnostics;
safe-message allowlisting, secret/content suppression, malformed/oversized JSON,
header preservation after body failures, and exact request/header/multipart tests.
A database/worker regression verifies persisted diagnostics, GET/replay identity
and no Film Understanding call after rejection. Tests never contact OpenAI.
Executed gates: **32 focused tests** (12 diagnostic, 6 existing adapter, 14 film
integration), **115 full backend tests**, zero failures/errors/skips; full Gradle
build, OpenAPI drift and diff checks pass. No frontend/API schema or UI change;
frontend/Chromium suites were not rerun for this backend-only correction. Original
PR8's 78 frontend/19 Chromium results remain historical, not newly claimed checks.

## Corrective validation and merge risk

One **isolated transcription-only** corrective live validation is justified after
explicit authorization, using the same request/audio contract and new diagnostics.
It must not silently continue into Astra or remove/reuse the original attempt lock
to bypass the no-reroll record. None was performed during the initial diagnostic review.

Without it, real transcription acceptance remains unverified and the primary
multimodal flow may fail for this account/environment. A successful response's
timestamp parsing and downstream Astra discovery remain live-unverified as well.
Recommend resolving that functional acceptance risk before merge; deterministic
diagnostic correctness is not proof of provider compatibility. A rejected future
call should now yield bounded safe evidence instead of another opaque failure.

## Separately authorized corrective attempt — September 14

Patrick explicitly authorized exactly one transcription-only validation after the
diagnostic fix. Tested HEAD: `39009964927604cfe0a7e15e3dae99d2f4c2c909`, branch
`feat/p1-ai-film-understanding`; worktree was clean immediately before dispatch.
Server-side key presence was checked without printing it. No application code change
was needed. An ignored, compiled local Java harness directly invoked the built
`OpenAiAudioTranscriptionAdapter.transcribe` exactly once, without starting Spring,
the Film Understanding worker or any Astra adapter. It used an exclusive new lock,
not the original attempt identity, lock, database row or Film Understanding endpoint.

Preflight re-extraction matched the reviewed source/audio contract: original SHA-256
`ac9b29c47eeb399dbd1ac5cdfcde19273e0e4e68f5df8fd67fedbef3d284a7a7`, source duration
92.458667 s (domain 92,459 ms), WAV PCM s16le/mono/16 kHz/16-bit, 2,958,756 bytes,
92.458688 s (existing PCM-duration calculation supplies 92,458 ms). Audio SHA-256:
`2711f31198d9d7e974af4e416ecec2ba07c1b01e7382e2c5605148e163fbc199`.
The current adapter's request construction and corrected diagnostics were used
unchanged. The harness only records safe completion/failure metadata, never audio
or transcript text in its evidence output.

| Evidence | Actual result |
| --- | --- |
| New validation identity | `87b0d170-3387-4959-8711-a2ef647f20ca`; isolated validation, not an application FilmUnderstandingRun. |
| Started / completed UTC | `2026-09-14T11:24:13.284144300Z` / `2026-09-14T11:24:13.728034900Z`. |
| Adapter-call latency | 423 ms. |
| Outcome | FAILED, `TRANSCRIPTION_UNAVAILABLE`. |
| Safe detail | Transcription timed out or could not be reached. No automatic retry was made. |
| Upstream HTTP status | Not exposed/unknown. The application's 503 is not a provider status. |
| Provider request ID | None captured. |
| Provider error type/code/message | No provider rejection diagnostics captured; safe detail above is application-authored. |
| Transcript segments/timestamps | No completion returned; count unavailable and parsing/timestamp verification not reached. |
| Usage / billed cost | Unavailable/unknown, not claimed zero. |
| Dispatches / Astra calls | One adapter dispatch invocation, no retries; zero Astra calls. Provider receipt cannot be confirmed. |

**Classification: INSUFFICIENT_EVIDENCE.** Unlike the original recorded non-2xx
rejection, this result is the adapter's unavailable branch. It does not establish
an upstream rejection, account restriction, successful transport, parser defect or
transient provider failure. No network exception cause is exposed by this branch;
the 423 ms elapsed time alone cannot diagnose its cause. Do not label this a proven
120-second timeout, infer a 401/403/429, or invent a provider request ID.

The new permanent lock and sanitized result are
`.local/pr8-corrective-transcription-87b0d170-3387-4959-8711-a2ef647f20ca.lock` and
the same basename with `.json`. Original attempt/result/lock SHA-256 values were
recorded before dispatch and verified unchanged afterward. No database writes or
original run mutations were made. The temporary extracted WAV was deleted; no
transcript was persisted. The authorization is consumed even though provider receipt
is unconfirmed; do not execute the harness again or allocate another identity.

No application code, API, model, configuration or architecture changed. Deterministic
tests/builds were not rerun for this local validation and documentation follow-up;
the preceding 32 focused/115 full backend tests, build and OpenAPI passes remain
historical verified gates for the same application code. Documentation diff check passes.

**Merge impact:** the acceptance gap is not closed. Real transcription transport,
successful response parsing/timestamps and real Astra Film Understanding remain
unverified. Stop for Patrick's review; no further provider request is authorized.

## One-call diarized migration and transport investigation — current

Patrick then requested a current transcription model, safe pre-response diagnostics
and an actual local HTTP capture. `gpt-transcribe` was verified as the general
completed-file recommendation, but its JSON response does not provide the required
Whisper timestamp contract. Four local 30-second chunks were proposed as a material
call-bound decision, not implemented. Patrick instead preferred evaluation of
`gpt-4o-transcribe-diarize`, which supplies timed segments in one Audio Transcriptions
request. That option is selected; no material format/duration incompatibility was
identified for the existing 120-second/4 MB PCM contract.

Exact request: **POST https://api.openai.com/v1/audio/transcriptions**; multipart
`model=gpt-4o-transcribe-diarize`, `response_format=diarized_json`,
`chunking_strategy=auto`, binary `file` / `source.wav` / `audio/wav`. No
`timestamp_granularities[]`, `verbose_json`, prompts, keywords, speaker references,
Responses/Chat endpoint or local multi-request orchestration. The provider handles
its own audio chunking within the one request. Official contracts, language/context
restrictions, usage alternatives, limits and cost comparison are in
[ADR-0009](adr/ADR-0009-timed-audio-transcription.md).

SceneProof retains only validated timed text: finite/ordered/positive segment seconds
rounded to source-relative milliseconds within decoded PCM duration. Speaker and
provider segment IDs are discarded, not adopted as entity/creator truth. Missing
times fail instead of receiving whole-film or synthetic replacements. V9 changes
only defaults for new model/provenance records; immutable old records remain Whisper.
No public API field or source extraction change, and no new transcription/Astra call.

Transport exceptions now preserve only DNS_FAILURE, CONNECT_FAILURE, TLS_FAILURE,
HTTP_TIMEOUT, IO_FAILURE, INTERRUPTED or UNKNOWN_TRANSPORT_FAILURE. Cause traversal
is cycle-safe/bounded; no messages, stacks, secrets, paths or suppressed content are
persisted. Synchronous send and asynchronous completion failures both use this path.
Actual received headers/status/request ID remain distinct from pre-header failure;
non-2xx bodies retain the prior reviewed allowlisted diagnostics. No proxy, TLS,
HTTP-version or retry workaround is made without evidence.

**Historical reconstruction:** the original non-2xx status/body and later transport
exception cause were discarded at their respective tested HEADs. Existing safe
artifacts contain no recoverable copy. Both failures remain INSUFFICIENT_EVIDENCE;
the later 423 ms result does not prove DNS, TLS, timeout or account denial. Patrick
reports Audio Transcriptions dashboard usage of zero requests/seconds; unrelated
Astra Responses/Chat usage must not be counted as transcription. An accepted future
request belongs to Audio Transcriptions for the selected model; neither dashboard
accounting nor model replacement is used to fabricate acceptance or diagnose history.

The local capture exercises the actual adapter send path with a dummy key and
synthetic WAVs, including the full 120-second PCM payload. It checks POST/audio-only
path, matching boundary, exact four-part binary body, extension/MIME and delivered
Content-Length. Redirects to Responses are rejected without a second request;
external/Chat/Responses test endpoints are rejected before dispatch. No real OpenAI
or Astra call occurs in normal tests.

Early offline checks found two test-fixture issues: a generic IOException wrapper
correctly classified as IO_FAILURE rather than unknown, and Mockito disallowing a
checked exception in a synchronous stub. Both fixtures were corrected, not counted
as passing gates. While Patrick refined the model choice, only the verified-empty
disposable film-test schema was rebuilt to apply the final uncommitted V9 cleanly;
the public database/original runs were never changed.

Validation: **38 focused tests** (6 transport/capture, 12 diagnostics, 6 adapter,
14 film integration); **121 full backend tests**, zero failures/errors/skips.
Final Gradle build, frontend TypeScript/Vite build, OpenAPI drift and diff checks
pass. Frontend/Chromium suites were not rerun for the unchanged UI/API shape; previous
results remain historical. The existing ~519 kB Vite entry-chunk warning remains.
Public readback still shows the original FAILED Whisper run/request ID and V8 as its
last migration; V9 has only been applied to isolated test schemas in this task.
Original artifact hash verification and presence of both consumed locks pass.
One new isolated transcription-only live validation is justified **only after**
green gates and new explicit authorization. Both previous locks remain consumed.
Real timed transcription and Astra Film Understanding are still separately
unvalidated; no merge or PR9 is authorized.

## Subsequent authorized derived-source result

After Patrick explicitly authorized a separate 0–36.291667-second source derivative
and confirmed conditional execution, one request at HEAD `8f3c689` returned **HTTP 401**,
type `invalid_request_error`, code **invalid_api_key**, provider request ID
`req_c02bddf06cbb41a789eece448c0f6f25`. Run `87e8fdfb-7abd-4d21-ba10-4a5975a47038`
failed before Astra; no retry or model fallback. This new failure is classified
PROVIDER_OR_ACCOUNT_RESTRICTION (authentication rejection of the supplied credential),
not a retrospective diagnosis of either older unknown failure. No raw error/key or
transcript is exposed. [Derived-source review](PR8-DERIVED-SOURCE.md) records the
immutable master mapping, 39 focused/122 full backend gates, exact safe evidence,
timing/cost limitations and consumed authorization. Correct credentials and obtain
fresh approval before another request. Successful live transcription/Astra remain
unverified; no merge or PR9.
