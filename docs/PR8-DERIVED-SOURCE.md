# PR8 derived Film Intelligence source and authorized validation

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

No live dispatch has occurred at this documentation checkpoint. The final result,
safe diagnostics/IDs, latency, output counts and usage limitations will be recorded
after the one authorized execution. No merge or PR9 is authorized.
