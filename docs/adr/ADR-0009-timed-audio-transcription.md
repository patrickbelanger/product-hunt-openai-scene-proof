# ADR-0009: One-call timed Audio Transcriptions and safe transport diagnostics

Status: Accepted under Patrick's explicit PR8 model-comparison instruction
Date: 2026-09-14

## Decision and alternatives

The initial PR8 Whisper integration used the correct Audio Transcriptions endpoint,
but discarded useful transport causes. Patrick requires purpose-built transcription,
separate from Astra, and asked to migrate to the current completed-file model.
Official documentation confirms `gpt-transcribe` returns JSON text without Whisper's
`timestamp_granularities[]` contract. Local 30-second chunks would need up to four
requests per 120-second film. That call-bound proposal was raised for decision, not
implemented or authorized for live execution. Patrick instead requested comparison
with the single-request diarized timed-segment API and prefers it if compatible.

Select `gpt-4o-transcribe-diarize` through **POST
https://api.openai.com/v1/audio/transcriptions**, with `response_format=diarized_json`
and `chunking_strategy=auto`. Server-side chunking is required for this model above
30 seconds; it does not add application HTTP calls. No local four-call orchestration,
Responses, Chat Completions, Realtime or general conversational transcription.

The guide recommends `gpt-transcribe` for ordinary file transcription and positions
the diarized model for speaker-aware recordings. SceneProof's specific reason to use
this specialized response is its documented timed text segments without additional
requests, not a claim that it is the preferred general ASR model. No documented
format/duration incompatibility with the existing bounded WAV was identified.
Speaker IDs/names and provider segment IDs are discarded. No speaker references,
prompt, keywords, language hints or benchmark answers are submitted.

## Contract and provenance

The existing FFmpeg extraction remains mono PCM s16le, 16 kHz, at most 120 seconds
and 4,000,000 bytes, below the documented 25 MB upload limit. Multipart contains
exactly `model`, `response_format`, `chunking_strategy`, and a binary `file` part
named `source.wav` with `audio/wav`. A byte-array publisher supplies Content-Length.
The optional legacy timestamp parameter is **not sent**; neither is `verbose_json`.
The API reference accepts flac, mp3, mp4, mpeg, mpga, m4a, ogg, wav and webm; PR8
continues to submit WAV only. Diarized-model output formats include json, text and
diarized_json; only the latter supplies this integration's segment annotations.
Prompts/logprobs/Whisper granularities are unsupported for this model and omitted.
No hardcoded language or context hints are needed; no speaker-reference enrollment.

Parse `text` and `segments[].text/start/end`; round finite seconds to milliseconds
against the original extracted PCM origin. Require ordered starts, positive segment
length, source-audio bounds, at most 200 segments, 2,000 characters each/24,000 total,
strict duplicate/trailing JSON and the existing 256 KiB response limit. Overlapping
segments are not inherently invalid. Missing/invalid times fail closed, never become
whole-film or synthetic timings. Speaker metadata is optional to SceneProof and is
not retained. Provenance is `OPENAI_DIARIZED_SEGMENT_ESTIMATE_SOURCE_START`: provider
estimates, not frame-exact alignment, identity truth or proof of on-screen action.

V9 changes defaults only for new run/transcript records. Historical Whisper model
names and timestamp origins, failed runs, immutable evidence and both attempt locks
are preserved. API fields, source limits and application call bounds do not change.
AudioTranscriptionPort remains separate from FilmUnderstandingPort; any transcription
failure prevents the Astra request. Astra model/reasoning/transport are unchanged.

## Transport and privacy

The production origin/path are fixed and redirects disabled. An internal test-only
factory permits only literal loopback HTTP and the same exact audio path, with no
query, fragment or userinfo; it rejects Responses/Chat paths and external origins.
Tests run the production request builder and send path against a local HTTP server,
using synthetic WAV and a dummy key, never the real API or an environment key.

Before-response exceptions now retain only reviewed categories: DNS_FAILURE,
CONNECT_FAILURE, TLS_FAILURE, HTTP_TIMEOUT, IO_FAILURE, INTERRUPTED,
UNKNOWN_TRANSPORT_FAILURE. At most 16 cause links are inspected with cycle protection;
messages, stacks, suppressed exceptions, file paths, headers, secrets and raw content
are not retained. Synchronous send failures and asynchronous failures share this path;
interrupt status is preserved. Received HTTP status/request ID remain distinct from
pre-header failures. Existing sanitized non-2xx diagnostics remain authoritative.
No automatic application retry or new timeout/proxy/TLS workaround is introduced.

## Cost, limits and delivery

One transcription HTTP invocation per film, zero without audio, followed by at most
one Astra call only after valid transcription. Official pricing lists an estimated
$0.006/minute for the diarized model (the same published minute estimate as Whisper),
with token prices $2.50/M input and $10/M output. This is not a billing receipt or a
guaranteed charge. The Audio API describes optional `usage`: `type=tokens` with
`input_tokens`, `output_tokens`, `total_tokens` and optional `input_token_details`
(`audio_tokens`, `text_tokens`), or `type=duration` with `seconds`. Existing
TranscriptionCompletion does not expose that optional usage, a remaining observability
limit. Never substitute Astra Responses/Chat usage for Audio Transcription usage.

The model card lists a 16,000-token context and 2,000-token output cap. Dense speech,
many turns, music/lyrics and silence may affect completeness, segmentation or accuracy.
Strict parsing rejects malformed/truncated responses, but cannot prove that every
spoken word was captured. Real acceptance/quality still requires separate explicit
authorization for one isolated validation; none is authorized or made here.
September 18 scope is unchanged; no PR9, merge, extra provider step or fake progress.

## Sources checked September 14

Finalization evidence update: a subsequent historical-master run successfully
completed this transcription contract and Astra; the shorter derivative remains
deterministically verified only. This does not alter the decision, request bounds
or earlier failure classifications. See [final validation recovery](../PR8-FINAL-VALIDATION.md).

- [File transcription and diarization](https://developers.openai.com/api/docs/guides/speech-to-text#speaker-diarization)
- [Whisper timestamp-only parameter](https://developers.openai.com/api/docs/guides/speech-to-text#timestamps)
- [GPT-Transcribe migration](https://developers.openai.com/cookbook/examples/migrating_from_whisper_to_gpt_transcribe)
- [Diarized model card](https://developers.openai.com/api/docs/models/gpt-4o-transcribe-diarize)
- [Audio Transcriptions request/response reference](https://developers.openai.com/api/reference/resources/audio/subresources/transcriptions/methods/create)
- [Official pricing](https://developers.openai.com/api/docs/pricing)
