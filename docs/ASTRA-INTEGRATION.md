# Astra integration

## PR2 current implementation — 2026-09-12

The first real transport spike succeeded before adapter integration: gpt-6-astra
identified the red square in an original programmatically generated PNG, returned
strict structured output, and reported 73 input / 20 output / 93 total tokens in
3.45 seconds. An earlier HTTP 401 demonstrated the authentication failure path;
the local configuration was corrected without displaying or committing the key.
The application smoke also succeeded: two PR1-normalized frames produced one real
PROP/HIGH finding, 1078 input / 572 output / 1650 total tokens in 11.54 seconds.
The same requestId returned the same run without another call. The run and finding
survived API restart and passed OpenAPI validation. Full evidence is in
[PR2 review](PR2-REVIEW.md) and [STATUS](STATUS.md).

### Verified official API and transport choice

Re-read the [model card](https://developers.openai.com/api/docs/models/gpt-6-astra),
[image guide](https://developers.openai.com/api/docs/guides/images-vision),
[Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs),
[libraries](https://developers.openai.com/api/docs/libraries),
[reasoning/usage](https://developers.openai.com/api/docs/guides/reasoning), and
[error codes](https://developers.openai.com/api/docs/guides/error-codes) this session.
The reference page fetch exposed only a generated placeholder; concrete request
fields were checked against the above guides and the successful live request.

Exact model: `gpt-6-astra`. Responses accepts `input` messages with `role` and a
`content` array of `input_text` and `input_image`; images use `image_url` containing
a `data:image/png;base64,...` URL. SceneProof sends high-detail normalized frames.
Output uses `text.format = {type: json_schema, name, strict: true, schema}`.
The schema makes every field required and sets additionalProperties=false on all
objects. No regex/prose extraction, tools, video upload, streaming or conversation
state. `store=false`, `reasoning.effort=low`, `max_output_tokens=6000`.

The official Java SDK documented version is `com.openai:openai-java:4.63.1`;
the current guides provide Responses, ResponseInputImage and strict schema builders.
Kotlin can call it through JVM interop; the listed Kotlin-specific SDK is community
maintained. The [Spring AI 2.0.1 OpenAI Chat page](https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html)
documents an official-Java-SDK-backed Chat Completions integration, native JSON Schema
and image content, but does not establish the required complete Responses path.
Choose JDK 25 HttpClient behind ContinuityAnalysisPort, with no added dependency.
See [ADR-0004](adr/ADR-0004-astra-responses-api-integration.md) for the authorized
decision, alternatives and delivery impact. Do not claim Spring AI has no vision
or structured output support; the unresolved capability was its Responses transport.

### Frame/context and validation strategy

Include project UUID/name/description/rules; each shot's UUID/name/order/kind/duration,
previous/next shot UUIDs; and each frame UUID/position/timestamp/dimensions directly
before its image. All READY shots are ordered together in one request. Reject more
than 8 READY shots; choose indices 0, floor((count-1)/2), count-1, deduplicated, per
shot. At most 3 frames per shot / 24 total, no chunk consolidation or N-squared calls.
Skipped failed imports and temporal subsampling yield explicit warnings. Missing,
corrupt, oversized or dimension-mismatched selected PNG aborts the entire run.
Fine transient changes, detailed text and motion can be missed by this strategy.

`continuity-result.schema.json` defines a typed ContinuityAnalysisResult containing
projectSummary, findings, inspectedShots, warnings and analysisMetadata. The local
validator enforces this schema's object/array/string/enum/UUID/numeric subset,
rejects duplicate JSON keys and trailing JSON, and checks exact inspected coverage.
Every finding's frame must belong to a supplied affected shot; every affected shot
must have evidence. Unknown project/shot/frame IDs, non-finite confidence, blank or
oversized content and invented references fail before any finding commit. Finding
UUIDs and OPEN status are server-assigned. Reference IDs are empty in PR2.
Transport failures/refusals/incomplete output never become an empty successful result.

### Durable lifecycle, limits and errors

POST analyses takes a client-generated requestId UUID. Reuse it after connection
loss: a project/request uniqueness constraint returns the original run, including
RUNNING or FAILED, without more provider work. A new UUID explicitly starts a new
attempt. Synchronous processing releases DB transactions before image/model work.
RUNNING is committed first; metadata snapshot includes selected IDs/timestamps,
rules and SHA-256 hashes, with no base64 or binary duplication. SUCCEEDED plus all
findings/evidence commit in one transaction. FAILED retains safe code/message and
available provider identifiers/usage. DB unavailability can prevent failure recording.

One semaphore per API process plus a PostgreSQL unique RUNNING index/advisory
transaction lock bounds concurrency across local processes. Start/GET lazily marks
RUNNING older than five minutes FAILED/ANALYSIS_INTERRUPTED; no job is replayed.
A late completion cannot overwrite a terminal run. There is no background worker.
Limits: 100 run attempts/project; 8 MiB per frame; 16 MiB total image bytes; 24 MiB
serialized request; 256 KiB response; 128 KiB structured JSON; 20 findings;
10-second connection and 120-second total HTTP deadline; zero automatic retries;
one provider request per newly admitted valid analysis. Count fields record selected
frames/shots, not proof of inspection on failed runs; complete coverage is required
for success. A lost HTTP response/timeout may still incur provider charges.

Usage preserves nullable inputTokens/outputTokens/totalTokens, cachedInputTokens,
reasoningTokens and cacheWriteTokens (the last was observed in the live response).
Reasoning tokens are included in output usage, not extra tokens to add twice.
HTTP 401/403 maps to safe configuration/access failure, 429 to rate/credit limit,
5xx/network to unavailable, timeout to timeout, other rejection to provider request
failure. OpenAI distinguishes billing/credit and request-rate limits within 429;
SceneProof deliberately groups them and performs no retry. Retry-After is not
automatically acted on. Refusal/incomplete/malformed responses retain usage when
available. Unexpected application failures retain stack traces with exception
messages removed; no keys, images, prompts or provider bodies are logged.

### Run and cost evidence

Normal Gradle/Vitest/Playwright suites never invoke paid analysis. Unit tests inspect
the real adapter's serialization/parser; application tests replace the port.
The sole explicit smoke script supports `--transport` (one tiny paid request) and
`--application` (two original PNG imports, one paid backend analysis, GET findings,
and replay of the same requestId). Running it again creates a new paid attempt.
See README for safe environment loading and local API setup.

The model card currently lists USD $10/M input, $1/M cached input, $12.50/M cache
write and $50/M output at standard rates. The tiny transport spike is approximately
$0.00173 at those rates, not a billing receipt. Report usage rather than assuming
account pricing/service tier. Public quotas/isolation remain a separate decision.

The application smoke recorded 1075 cache-write tokens within 1078 input tokens.
At the published standard rates, its estimate is approximately $0.04207; both
successful smoke calls total approximately $0.04380. Actual billing may differ
with account/service-tier settings. No further paid call was made for restart or
contract verification. The smoke proves the pipeline on a tiny original fixture;
it is not an accuracy benchmark for complex film continuity.
