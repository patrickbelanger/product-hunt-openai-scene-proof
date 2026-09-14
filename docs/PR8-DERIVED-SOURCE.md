# PR8 derived Film Intelligence source and authorized validation

## Final source-validation distinction

The later successful UI run `104fed03-5210-4269-a6e8-169ced53f2ce` used the immutable
92.458667-second master (persisted SHA `ac9b29c47eeb399dbd1ac5cdfcde19273e0e4e68f5df8fd67fedbef3d284a7a7`).
It validates the provider path, **not live acceptance of this derivative**. The
36.291667-second derivative remains deterministically verified; its earlier 401 is
unchanged. New/reset copies use the derivative; old copies retain the master.
[Final recovery](PR8-FINAL-VALIDATION.md) supersedes only the earlier claim that no
successful provider flow exists. All historical evidence below remains intact.

Date: September 14, 2026. Branch: `feat/p1-ai-film-understanding`.

## Explicit immutable provenance

- Master: `demo/between the lines - demo.mp4`, **92.458667 s**, SHA-256
  `ac9b29c47eeb399dbd1ac5cdfcde19273e0e4e68f5df8fd67fedbef3d284a7a7`.
- Derived source: `demo/runtime/analysis-source.mp4`, **36.291667 s**, 32,003,046 bytes,
  SHA-256 `2205c5c0a9ddc98fe7de897bc960775539095f75ee30e567f31ccca8570de835`.
- Mapping: master **[0.000000, 36.291667)** seconds → derived source starting at zero.
  Microsecond boundaries: 0 and 36,291,667; no cut chosen from a transcript or AI result.
  This is the meaningful-picture end already documented by PR7, explicitly selected
  by Patrick. The long black/music-only tail is excluded, not reinterpreted.

The master is not modified or overwritten. The runtime manifest links the derived
hash to master path, packaged `source-film.mp4`, master hash/duration and source range.
SourceFilm rows retain their own immutable bytes/hash/duration; derived-time plus zero
maps to master time. No historic source row, FilmUnderstandingRun, transcript, finding
or paid-attempt lock is rewritten. Existing copies recover as before; only new/reset
copies use the derivative. The v1 authored PR7 clips/references/rules remain unchanged.
No DB/API schema change is needed for this asset provenance; the checked-in manifest
and Git history identify the derivation by its hash.

## Deterministic generation

`node scripts/prepare-analysis-source.mjs` verifies the approved master hash, duration,
24 fps and 48 kHz zero-based audio before generation. FFmpeg uses explicit video/audio
mapping, strips metadata/chapters, normalizes timestamps, fixed libx264 CRF 18/medium/
two video threads/yuv420p, bitexact flags and a 24,000 video track timescale.
The cut is 871 picture frames and 1,742,000 original decoded audio samples, both
36.291666… seconds (sub-microsecond representation of the documented boundary).
Frame/sample cuts avoid accidentally including the first black frame due to decimal
rounding. H.264/MP4 preserves original 1080×1920/24 fps; ALAC preserves the selected
decoded audio losslessly at 48 kHz without adding lossy encoder-delay padding.
The existing pipeline decodes that audio to its usual mono 16 kHz PCM WAV.

Reproducibility is scoped to the recorded FFmpeg build:
`N-120856-g9893d66add-20250831`; hashes across different encoder builds are not promised.
The generation helper rejects overwriting the master and verifies its hash afterward.
`scripts/prepare-demo.mjs` uses the same helper and manifest provenance.

## Offline verification

FFprobe reports H.264 picture and ALAC audio both starting at 0.000000 and ending at
36.291667 seconds; 871 video frames. Final frame 870 (36.250 s) was visually inspected:
actual workstation/monitor film content, not the long black tail.
Production-equivalent extraction yields 16 kHz mono PCM s16le, **36.291688 s**,
**1,161,412 bytes**, SHA-256
`db5283c7f1a488a03932d1f9a7b74aa27538992afa34ccd6d85e1c95b4731c9c`.
The 21-microsecond resampling difference is normal sample-grid tolerance, not source
content beyond the cut; the existing PCM duration bound is 36,291 ms.

Deterministic tests cover actual packaged new/reset copies, master/source-history
preservation, repeated visual timestamp/frame-hash equality, repeated PCM byte equality,
at most eight segments/24 frames, all bounds within the derived source, temporary
audio cleanup and zero provider interactions. Existing transcription-before-Astra,
strict timed provenance, local HTTP capture and failure/retry guards remain required.
Two independent encodes produced the exact derived SHA-256 above. The overwrite
guard also rejects the master under case-varied Windows paths before FFmpeg runs.
**39 focused / 122 full backend tests** pass with zero failures/errors/skips.
Full Gradle build, frontend TypeScript/Vite build, OpenAPI drift, script syntax and
diff checks pass. Frontend/Chromium suites were not rerun for this backend/asset
change; prior UI test results remain historical. The existing ~519 kB Vite warning
remains. The temporary repeat MP4 and offline WAV were deleted after verification.

## Live authorization and result

Patrick's prior authorization was unconsumed: preflight stopped on the 92-second
master before any provider dispatch. He explicitly extends it to this corrected
derived source after deterministic gates: exactly one `gpt-4o-transcribe-diarize`
request to `https://api.openai.com/v1/audio/transcriptions`; only successful validated
timed text permits one `gpt-6-astra` Film Understanding request with prepared frames.
No Chat/Responses transcription, model fallback, local multi-call chunks, retry or
reroll. The existing original and corrective attempt locks remain preserved; the
derived validation uses a distinct `pr8-derived-film` lock/identity.

Executed once from clean HEAD `8f3c689925b537e7678d1f356900df877cb95445` after all
gates above, against the current production jar on an isolated local API port.
The server key was present. Pre-dispatch output recorded master/source durations,
derived hash, exact Audio Transcriptions endpoint/model and no Chat/Responses
transcription. The application created a new independent demo/project/run; no
previous paid identity or lock was reused.

| Evidence | Actual result |
| --- | --- |
| Project | `d145e66c-0a4c-4444-a2ba-3a1977cac673` |
| Validation/run | `87e8fdfb-7abd-4d21-ba10-4a5975a47038` |
| Client request identity | `ae4002b3-0c45-4d45-95fe-69d85926a897` |
| SourceFilm | `f2581347-32c0-47b9-b3d4-f2668afe2022`; 36,292 ms domain duration; derived hash above |
| Started / completed UTC | `2026-09-14T13:56:27.404685Z` / `2026-09-14T13:56:38.544431Z` |
| Run elapsed | 11.140 s, including local preparation |
| Transcription stage elapsed | 3.749 s, including local audio extraction and failure persistence; not isolated HTTP latency |
| Outcome | FAILED / TRANSCRIPTION_REJECTED |
| Upstream HTTP | **401** |
| OpenAI error type / code | `invalid_request_error` / **`invalid_api_key`** |
| Provider request ID | `req_c02bddf06cbb41a789eece448c0f6f25` |
| Provider message | Withheld by the safe-message allowlist; no raw key-containing error retained |
| Visual preparation | 8 deterministic segments / 24 frames, source-bounded |
| Transcription model / endpoint | `gpt-4o-transcribe-diarize` / POST `/v1/audio/transcriptions` |
| Transcript / timestamp validation | No successful transcript, zero segments; provider-time validation not reached |
| Astra calls / output | **Zero**; no summary, entities, candidates, concerns or warnings from Film Understanding |
| Usage / charged cost | Transcription usage/billed charge unknown; absent response usage is not a zero-charge claim. Astra: zero calls/tokens/cost |

Classification of this new failure: **PROVIDER_OR_ACCOUNT_RESTRICTION**, specifically
provider authentication rejection of the supplied server credential (`invalid_api_key`).
The exact credential issue (for example revoked versus incorrectly configured) is
not established. Do not infer insufficient credit, model unavailability or a broken
audio format. The HTTP response/request ID establish provider contact, not successful
audio decoding/transcription. Previous attempts retain their original
INSUFFICIENT_EVIDENCE classifications; this response does not reconstruct those errors.

Only PREPARING_SOURCE, DETECTING_STRUCTURE, TRANSCRIBING_AUDIO and FAILED stages
occurred. No UNDERSTANDING_FILM stage or Astra request followed. The screen-opening
relationship was not evaluated, detected, missed or injected. No fallback or reroll.

GET-only recovery confirms the identical durable failure. Original attempt/result/
lock SHA-256 values remain unchanged, as does the master hash; both earlier locks and
the new `.local/pr8-derived-film-paid-attempt.lock` remain preserved. Safe result and
identity are retained privately in `.local/pr8-derived-film-result.json` and
`.local/pr8-derived-film-attempt.json`. Temporary extracted audio is absent. The
owned validation API was stopped after completion; user services were not touched.

**Merge readiness:** derivative preparation, deterministic processing and truthful
failure diagnostics are verified. Successful live timed transcription and Astra
Film Understanding remain unvalidated. The supplied server credential must be
corrected before any newly authorized future request; this authorization is consumed.
Stop for Patrick's review. No merge or PR9.

## Interruption recovery verification

Recovery found the source implementation committed at `8f3c689925b537e7678d1f356900df877cb95445`
and only the outcome documentation uncommitted. The retained attempt manifest, paid
lock, result and dispatch log confirm the request above occurred before interruption;
authorization is consumed. Recovery rechecked both asset hashes, derivative stream
durations/zero timestamps, manifest provenance and the 122-test XML totals (zero
failures/errors/skips). Existing focused/full build logs confirm successful gates.
No implementation changed or provider request was made during recovery. Only outcome
documentation was finalized; unchanged deterministic suites were not rerun.
