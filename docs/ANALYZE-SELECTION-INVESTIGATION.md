# Analyze Selection — focused PR9 implementation gate

Investigated 2026-09-15 at `2c0e3f3050e416b0cbdea8bd28b8888ac8e8d21c`,
clean `feat/p1-ux-polish`. Static code investigation only; no provider calls.

## DECISION REQUIRED — defer Analyze Selection

Current choice: Advanced inspection is local navigation state, not an analysis input.
Problem: a paid selection request needs an immutable source-range identity and an
end-to-end evidence boundary that the existing sequence request does not define.
Recommendation: follow the approved gate and defer to a post-PR10 candidate. PR9
remains ready for merge review without this feature. No implementation was started.

This does **not** require a new model or inherently require a parallel analysis
subsystem. Much of the existing pipeline is reusable. The blocker is material
request/domain/provenance behavior across admission, evidence assembly, persistence
and subsequent creator re-evaluation—not the number of UI controls or DB columns.

## Answers and repository evidence

1. **Range acceptance: not currently supported.**
   `apps/api/src/main/kotlin/dev/sceneproof/analysis/AnalysisController.kt:11`
   accepts only `requestId`. `AnalysisContextAssembler.kt:14` selects ready source
   segments when present, otherwise ready project imports, then first/middle/last
   frames per shot. It calculates source frame timestamps but accepts no In/Out.
   Neither UI marks nor submitted evidence IDs are current sequence-run inputs.

2. **Bounded visuals: reusable frames, not an existing range extractor.**
   `apps/api/src/main/kotlin/dev/sceneproof/film/SourceFilmService.kt:50` prepares
   whole-source structure, reuses existing project segments and caps the result at
   24 frames. `FfmpegAdapter.kt:39` has no range parameters; it samples the bounded
   whole input. Filtering existing frames by segment start plus frame timestamp
   could support a sampled-evidence-only selection without any FFmpeg change. It
   must not claim dense/exhaustive coverage; empty/sparse evidence needs a policy.
   Fresh in-range sampling would additionally require range-aware extraction,
   timestamp rebasing/validation, persisted ownership and cleanup tests.
   Current UI marks can belong to an individual imported clip, not just the primary
   source: clip-relative time must never silently become primary-source time.

3. **Findings reuse: yes, but range provenance is missing.**
   `apps/api/src/main/kotlin/dev/sceneproof/analysis/AnalysisRepository.kt:32`
   reserves a run by project/request UUID. Replay does not bind source/range inputs.
   `recordContext` snapshots frames, hashes, timestamps, references and film memory
   only after assembly; an assembly failure can precede this snapshot. The existing
   JSON context could carry future provenance without a parallel findings store,
   but the request identity, frozen input, failure history and read API must explicitly
   expose it. `AnalysisRunView` currently contains no selected source/In/Out/duration.
   Existing `succeed`, finding links and creator actions can be reused. Empty findings
   are already valid (`continuity-result.schema.json` findings minItems = 0); no
   synthetic finding is needed. Selection-specific zero-result copy requires the
   saved run scope, not whatever marks happen to remain in the UI.

4. **Provider reuse: yes; safe PR9 wiring-only implementation: no.**
   `AstraContinuityAnalysisAdapter.kt:41` sends text context and normalized PNG
   images, not raw source video/audio. The same continuity port/model and result
   validator can serve a properly scoped context. Existing admission includes the
   service semaphore, DB advisory lock/global running-run guard, Film Understanding
   exclusion, 100 project attempts and request replay. No automatic AI retry exists.
   Preserve 8 shots, 3 selected frames per shot/24 overall, 8 references and the
   shared 16 MiB image budget; never bypass trusted storage/media processing limits.
   Crucially, `FindingActionService.kt:21` rebuilds original evidence plus bounded
   neighboring shots and restores original film memory. Neighbors may lie outside a
   selection. A future feature must explicitly preserve or separately identify that
   scope, not silently widen the creator's paid selection on re-evaluation.

5. **Transcript reuse: possible without new transcription, with policy work.**
   `apps/api/src/main/kotlin/dev/sceneproof/film/FilmRepository.kt:105` reads immutable
   timestamped transcript segments associated with a run. `memory` currently uses
   the latest successful Film Understanding run's entire transcript plus narrative
   cues, and confirmed anchors. A selection must freeze its chosen source/run and
   filter context; it cannot call `memory(projectId)` unchanged and claim boundedness.
   Fully contained segments can be included with their original IDs/text/times.
   Overlapping segments may contain words outside the range; clamping timestamps
   does not trim text or create word alignment. Decide whether to omit them or use
   a separately disclosed context policy. Narrative cues have no interval and may
   cite out-of-range frame/transcript IDs, so their dependencies must be checked or
   cues omitted. Keep creator-confirmed rules distinct from bounded observations.
   Missing transcript should be explicit; no automatic transcription is implied.

## Alternatives and delivery impact

- **Small-looking UI/assembler filter now:** rejected; leaves replay/failure range
  identity, transcript boundaries and targeted neighbor scope undefined.
- **Sampled-evidence-only selection through existing continuity analysis later:**
  preferred first candidate; avoids new extraction/transcription while introducing
  the required explicit domain contract and deterministic coverage.
- **Fresh range extraction now:** wider media/persistence work, not a PR9 quick win.

Deferral changes documentation only and protects the September 18 delivery; no
schedule extension is introduced. Implementing now adds unestimated contract/media/
history regression work to a verified PR9. Reprioritization needs separate approval.

## Intended future workflow and acceptance boundary

In/Out → explicit Analyze selection and paid consent → validate source ownership,
time origin and bounds → reserve immutable source/range/request provenance before
evidence work → bounded frames/transcript context → existing continuity provider
and validator → existing persisted run/findings → View findings (including an honest
zero-findings result). Duration derives from saved Out minus In. Subsequent UI mark
changes cannot alter historical inputs. No automatic tab switch or I/O provider call.

Before implementation: define boundary inclusivity, clip versus primary-source
eligibility, sparse/empty visual and overlapping/missing transcript behavior,
immutable replay conflict handling and re-evaluation scope. Test invalid/out-of-range
requests, evidence containment, context snapshots, admission/deletion interaction,
zero findings and durable failures with deterministic providers only. A future live
check, if desired, requires Patrick's explicit authorization; none was needed or made
for this investigation. Film Intelligence behavior remains unchanged.

Advanced inspection stays inspection-only. Source Monitor / Evidence Frame and Fix
Assist also remain future work. No PR10 implementation or merge is authorized here.
