# Astra integration

## PR5 declared visual references

The [Astra model card](https://developers.openai.com/api/docs/models/gpt-6-astra),
[image guide](https://developers.openai.com/api/docs/guides/images-vision) and
[Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs)
were checked for PR5. Existing Responses image items and strict JSON Schema support
the extension; no model/SDK/dependency change. Sequence stays LOW, targeted HIGH.

AnalysisContext includes typed AnalysisReference values: project/reference UUID,
title, guidance, normalized dimensions, SHA-256 and in-memory PNG. Sequence assembly
includes all active references in creation/UUID order, at most eight. No selection
engine, remote URLs or mandatory references. The 8 shots/3 frames per shot/24 frames
limits remain. References share 8 MiB/image, 16 MiB combined binary, 24 MiB serialized
request, 256 KiB response, 6000 output tokens, 120 seconds and zero automatic retries.
There is no second implicit image budget.

Each REFERENCE metadata block precedes its image, followed by marked sequence shots
and frame/image pairs. Reference titles/guidance and image text are untrusted scene
data. The prompt distinguishes declared truth from observed evidence and independent
judgement. Angle, lighting, occlusion, narrative changes and intent can explain
differences. No tools/navigation. Cite only supplied relevant reference UUIDs.

Continuity schema version 1 now admits zero-to-eight relevantReferenceIds. Citations
must be distinct and submitted in this context/project; zero is valid even with
references. Shot/frame invariants and server-generated finding IDs remain intact.
The run context and analysis_references preserve metadata/hash, no base64, bytes,
paths or raw provider request. Composite links retain exact submitted finding evidence.

Targeted PR4 uses original cited reference snapshots, including archives, with
original title/guidance/hash. No substitution with current/replacement references.
Original rules are separately identified from current rules, alongside original
finding/frames, creator scope, previous judgement and neighbors. Its output schema
is unchanged. Required missing/corrupt reference content fails closed before provider
work. Edits/archive after context assembly cannot alter its frozen inputs.

Normal tests use deterministic ports. The separate smoke writes its request UUID
before the sole authorized inference; replay reuses it and restart verification uses
GET only. Outcome, usage and cost: [PR5 review](PR5-REVIEW.md). Citations are not forced.

## PR4 targeted re-evaluation

The official model card and Structured Outputs guide were rechecked for PR4.
The existing Responses HTTP transport, image input and strict text.format suffice;
no SDK/dependency migration is required. Targeted calls pin `gpt-6-astra` and
`reasoning.effort=high`; the PR2 sequence operation retains `low`.

`ContinuityAnalysisPort.reanalyze(TargetedContext)` returns a typed TargetedCompletion.
TargetedContext carries the complete original finding, expected/observed states,
original explanation, original evidence, current creator explanation/scope and
preceding successful targeted judgement when present. Project rules/description and
shot order accompany the images. All creator/project strings and image text are
untrusted data. The system prompt explicitly permits disagreement and uncertainty;
creator intent is never an instruction to approve or erase the finding.

Select the original affected shots and all their original evidence frames. Add
immediate READY neighbors deterministically up to the same eight-shot limit, in
timeline order; each neighbor uses first/middle/last sampling. At most three frames
per shot, 24 total, the same per-frame/aggregate/request byte bounds. A large project
does not trigger whole-project sampling/rejection when the targeted scope fits.
Warnings identify targeted scope and temporal limitations. The run snapshot strategy
is `original-evidence-neighbors-v1`, retaining metadata/hashes without image copies.

`targeted-result.schema.json` version 1 requires projectId, originalFindingId,
outcome, summary, explanation, evaluatedScope, affectedShotIds, evidenceFrameIds,
inspectedShots, remainingIssue and suggestedCorrection. Allowed outcomes are
INTENT_ACCEPTED, ISSUE_REMAINS and INSUFFICIENT_EVIDENCE. Acceptance requires empty
remainingIssue/correction; a maintained issue requires both. The validator rejects
unknown fields, duplicate JSON keys, trailing JSON, foreign IDs, incomplete coverage,
duplicate IDs, omitted original evidence and oversized/blank required text. Every
submitted image must appear in inspectedShots. Provider-generated durable IDs are
not part of the schema. Refusal/incomplete/malformed output is durable failure.

INTENTIONAL_CHANGE and its TARGETED AnalysisRun commit before image/provider work.
All paid work shares PR2's global RUNNING admission, 100-run project limit, 120-second
deadline, zero retries, safe failures, usage and provider IDs. Resolve/dismiss have
no run or provider call. Validated result plus SUCCEEDED is one transaction. Status
is projected only from successful actions; failed/rolled-back completion leaves
the previous judgement effective. Provider work holds no database transaction.

Same request UUID/payload returns the existing action, including RUNNING/FAILED;
changed payload conflicts. No automatic provider retry, even after timeout. A new
explicit UUID authorizes another attempt; prior failure remains visible. Database
unavailability can prevent recording the failure; five-minute lazy run recovery
then exposes an interruption without retrying. Timeout can still incur provider cost.

The separately invoked `scripts/astra-targeted-smoke.mjs` reuses a persisted original
synthetic PR2 finding, avoiding a second full-sequence paid analysis. Its ignored
manifest records the request before POST and makes reruns replay the same UUID.
`--verify` reads only and checks preservation after restart. No desired outcome is
asserted: any valid independent outcome proves transport/durability. Normal tests
use fake ports and never execute this smoke. Final evidence belongs in PR4-REVIEW.

## PR2 implementation baseline — 2026-09-12 (PR5 extension above)

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
UUIDs and OPEN status are server-assigned. Reference IDs were empty in PR2;
PR5 extends that constraint as described above.
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

### PR7 real film validation

The demo uses the same production assembler/adapter. Only authored rules, actual
media and reference metadata enter context; template identity and evaluation notes
do not. Gradle packages `demo/runtime` only. DemoApiTest checks both AnalysisContext
and the real adapter's serialized request for evaluation keys/case IDs/questions.
There is no demo-specific prompt or injected Finding row. Sequence reasoning remains
LOW and targeted reasoning HIGH; PR7 changes neither. Eight shots, 24 selected frames,
five references and all existing byte/duration/shot bounds remain unchanged.

Deterministic preflight exposed unnecessary FFmpeg upscaling of small videos. PR7
corrects the scaler to decrease only, consistent with still-image normalization;
the 1600-pixel maximum is unchanged. Authored clips are 432×768, references 540×960.
Final selected image payload is 14,985,129 bytes, below the existing 16 MiB bound.

After all deterministic checks passed, the sole authorized real call succeeded on
2026-09-14 UTC (September 13 Toronto), run `9b30f01d-54e6-4586-b7ac-63c0f2eb6880`.
It returned zero findings, explicitly contextualized the apartment transition and
warned about sampling, small UI and device-identity ambiguity. No repeated call seeks
the historical five-issue target. No finding-level citation/evidence quality can be
scored from an empty result. GET-only verification confirms the persisted result.
Usage: 16,861 input, 1,176 output, 18,037 total; zero cache hits, 16,858 cache-write,
26 reasoning tokens already included in output. Duration 28.709 seconds; standard-rate
estimate USD $0.269555, not a billing receipt. Exact provenance and evaluation are in
[PR7 review](PR7-REVIEW.md); this run is not installed as a recorded demo baseline.
