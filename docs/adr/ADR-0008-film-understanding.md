# ADR-0008: Bounded Film Understanding with creator-confirmed memory

Status: Accepted within Patrick's explicit PR8 implementation authorization
Date: 2026-09-14

## Context and decision record

Current choice: PR7 curated clips and references, image-only sequence analysis and
synchronous RUNNING state. Problem: creators do too much setup, narrative/audio
context is absent and actual AI work is invisible. Proposed alternative and
recommendation: a bounded primary SourceFilm, separate transcription and discovery
ports, durable stages, candidate review and explicit confirmed memory. Patrick's
PR8 instruction authorizes these responsibilities and permits polling.

## Decision

One immutable primary source per project in this slice, with the existing MP4/H.264,
100 MiB/120 second/dimension/fps limits. Originals remain behind MediaStorage.
Understanding uses bounded FFmpeg scene-change/periodic samples and deterministic
analysis segments, not editable scenes or claimed exhaustive cut detection.
Provider timestamps are approximate evidence; decoded picture timestamps and
transcription timestamps retain their distinct provenance and common source origin.

Use AudioTranscriptionPort with OpenAI `/v1/audio/transcriptions`, `whisper-1`,
`verbose_json`, segment timestamps. Current official documentation recommends this
specialized model for timestamps. Extract bounded mono PCM audio; no lyric or
benchmark hint enters transcription. Lyrics/dialogue describe context, never prove
on-screen action or establish a continuity invariant. No public transcript logging.

FilmUnderstandingPort uses Astra Responses with strict structured output, MEDIUM
reasoning, no tools, store=false and zero retries. Preserve existing sequence LOW
and targeted HIGH. Validate every evidence/segment/entity identifier before commit.
Observation → candidate invariant → creator confirmation → continuity enforcement.
Accept/Edit/Reject never calls a provider; visual promotion uses ReferenceService.

Durable run/stage records are committed before each real operation; one local worker
per admitted run with database admission and deadlines. GET polling restores run
identity/state after reload. Interruption becomes FAILED and never automatically
replays a potentially charged request. Same UUID/payload recovers; new explicit
consent/UUID creates a new attempt. No synthetic percentages or thinking narrative.

## Alternatives and delivery impact

SSE adds connection/event recovery complexity without improving this bounded flow;
polling reuses TanStack Query and durable GET state. General task queues and NLE
editing exceed PR8. Automatic promotion would mistake observations for truth.
`gpt-transcribe` is the current general transcription recommendation, but Whisper
supplies documented segment timestamps without inventing forced alignment.
This keeps September 18 unchanged; PR9 general UX and optional P2 stay separate.
Local unauthenticated storage, retained history and orphan-file limits remain.

## Implemented bounds and integrity

V7 adds source, run, stage, segment, transcript and candidate tables plus finding
cross-modal snapshots. V8 protects completed results/decisions and same-project
reference provenance. Analysis context snapshots preserve original film memory for
targeted review. Candidate decisions are one-way; editing means editing before
confirmation, not rewriting an earlier accepted decision. Normal reference metadata
can evolve without changing the originating candidate/evidence.

At most eight groups/24 frames, maximum side 768, 16 MiB PNGs/24 MiB request,
120-second/4 MB mono PCM, 200 transcript segments/24,000 characters, 6,000 Astra
output tokens, ten film attempts and 24 confirmed anchors per project. Normal
references retain eight-active/100-lifetime limits. A new film needs a new project.
Existing authored demo inputs retain version v1; original-source support is additive
on new/reset copies, with no automatic alteration of old copies.

Each provider request has a 120-second deadline; an interrupted process is recovered
as FAILED at ten minutes through GET/start, not automatically resumed or replayed.
Source picture samples and transcript use a verified common container origin, with
decoded picture versus approximate transcription provenance retained separately.

## References

- [File transcription and timestamps](https://developers.openai.com/api/docs/guides/speech-to-text#timestamps)
- [Astra model](https://developers.openai.com/api/docs/models/gpt-6-astra)
- [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs)
