# PR8 transcription rejection review

Date: September 14, 2026. Branch: `feat/p1-ai-film-understanding`.
Reviewed implementation: `af42338d90934a9fdb6d304e43d0e418ae8d6484`.
No new OpenAI inference/API request, reroll, merge or PR9 work is authorized or performed.
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

## Offline contract review

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
to bypass the no-reroll record. None is performed in this review.

Without it, real transcription acceptance remains unverified and the primary
multimodal flow may fail for this account/environment. A successful response's
timestamp parsing and downstream Astra discovery remain live-unverified as well.
Recommend resolving that functional acceptance risk before merge; deterministic
diagnostic correctness is not proof of provider compatibility. A rejected future
call should now yield bounded safe evidence instead of another opaque failure.
