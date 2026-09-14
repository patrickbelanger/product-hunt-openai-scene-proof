# Demo film

## Final PR8 evidence

New/reset Film Intelligence sources use the 36.291667-second derivative. The successful
UI run used an existing historical master-source project (92.458667 seconds); its
six transcript segments extend to 47.366 seconds legitimately on that source.
Do not relabel it as a derivative validation or preload its results into new demos.
[Final recovery](PR8-FINAL-VALIDATION.md) preserves the live result and prior failures.

PR7 implements **Between the Line — Continuity Study**, template **between-the-line-v1**.
Try the demo film opens a real populated project without upload, forms or authentication.
First preparation uses the actual ingestion services; subsequent launch recovers the
current working copy. New/reset copies contain no recorded analysis or findings.

## Approved source and curation

Patrick supplied `demo/between the lines - demo.mp4` and explicitly confirmed his
original/generated work, using his generated references, is authorized for SceneProof
and the public Product Hunt demo. Attribution: **Laurie and Patrick**. He also called
the supplied film `Between the lines - 4(1).mp4` in conversation; the actual checked-in
source is identified by SHA-256
`ac9b29c47eeb399dbd1ac5cdfcde19273e0e4e68f5df8fd67fedbef3d284a7a7`.
No neighboring/private creative files, web stock or third-party project code was used.

Actual source: 42,842,998 bytes, H.264, 1080×1920, 24 fps, 92.458667 seconds.
Picture ends at 36.291667; the remaining tail is black. The original file is preserved.
The authored clips exclude that tail, retain source order and use full-frame 432×768
derivatives; references use 540×960. This fits existing image budgets without
manufacturing objects/text/drift.
Audio was omitted from PR7's curated clips. PR8 additionally ingests the complete
original source and its audio without changing those clips; see below.

| Order | Persisted clip title | Source seconds |
| --- | --- | --- |
| 1 | Metro platform | 0–3.208333 |
| 2 | Inside the carriage | 3.208333–8.916667 |
| 3 | Phone and passenger intercut | 8.916667–15.541667 |
| 4 | Patrick with his phone | 15.541667–17.541667 |
| 5 | City transition | 17.541667–20.5 |
| 6 | At home | 20.5–24.791667 |
| 7 | Device on the sofa | 24.791667–27.375 |
| 8 | At the workstation | 27.375–36.291667 |

These are eight authored import segments, not eight asserted automatic cuts. Clip 3
contains the original phone/passenger intercut. Persisted video timestamps are actual
FFmpeg-selected times relative to each clip. IDs are generated per copy, never global.
Existing whole-sequence limits remain eight shots and first/middle/last frames each.

Five PNG visual references, derived from full source frames at 4, 1, 10, 22 and 32
seconds respectively: Patrick — metro appearance; Metro platform; Handheld phone;
Apartment sitting area; Apartment workstation. Guidance identifies scene/subject only.
Rules identify Patrick and the separate metro/apartment scenes; they do not reveal
expected errors. Normal reference normalization and ownership invariants apply.

`demo/curation.json` records source/provenance, segment boundaries and human notes.
`scripts/prepare-demo.mjs` verifies source hash and reproduces derivatives plus
`demo/runtime/manifest.json`. Gradle packages only runtime inputs and 13 assets.
Authored content changes require a new template version and reviewed hashes.

## Evaluation separation and truthful judgement

Patrick explicitly superseded the old black-blazer assumption: actual metro wardrobe
is a dark/navy polo. Later apartment clothing is a deliberate scene/time transition.
The earlier 10–12-shot target is replaced by up to eight useful clips for PR7.

`demo/evaluation/expectations.json` contains independent review questions for actual
character consistency, devices, legible UI/text and metro geography, plus evaluation
of the deliberate apartment transition. These are not established defects or promises
of five detections. The original seven candidate cases remain historical; unsupported
blazer/door cases are not expected detections for this version.

The evaluation manifest is outside runtime resources. DemoTemplate contains only
authored project data, references and shots. AnalysisContextAssembler explicitly
selects normal project/rules/media/reference data. Tests inspect the real adapter's
serialized request and prove evaluation case IDs/questions/fields are absent.
No expected outcomes are placed in guidance, filenames, metadata or Finding rows.
No intent action is pre-accepted. Actual independent findings, including possible
false positives or missed issues, are recorded in PR7-REVIEW for the single validation.

That run succeeded with zero findings. It contextualized the apartment wardrobe and
lighting transition, noted the phone difference without assuming the same device,
and warned that sampling and small UI text limit verification. No established defects
were labelled in advance, so a recall score would be invented. The footage is a real
populated-project demonstration, not a validated multi-finding showcase. No further
paid call or asset tuning followed this result.

## Working copy and reset

Session storage preserves one creation UUID for the local tab. A distinct UUID creates
an independent normal Project copy. Reset explicitly confirms replacement, restores
original rules/references/ordered media and starts with no analysis/actions. It keeps
old findings/actions/reference evidence with the retired old project, outside the
library but accessible at its old URL. Repeated requests recover the current copy;
failed creation/reset cannot publish partial state. No ordinary/other demo is touched.
The template has no mutation route. See ADR-0007 for transaction and retention limits.

No public deployment, anonymous production isolation or spend quota is added.
Those remain prerequisites for a public launch. PR7 baseline is empty;
a future recorded baseline must originate from a validated real run and be labelled.

## PR8 derived-source understanding

The `between-the-line-v1` authored clips/references/rules remain unchanged. An additive
manifest sourceFilm entry now references `analysis-source.mp4`, derived from master
range 0–36.291667 seconds. `sourceProvenance` records the immutable master path/hash,
92.458667-second duration and exact range in microseconds. Gradle still packages the
unchanged master as `demo/source-film.mp4`, alongside the derived runtime asset, not
evaluation/curation notes. New/reset copies ingest the derivative through SourceFilmService.
Existing PR7/PR8 source records and analyses are not mutated; resetting creates a
new project, preserving the retired copy's original source and historical evidence.

No understanding, transcription, candidate or finding is seeded on launch/reset.
Only explicit **Understand film** consent starts the two bounded provider stages.
Reset gives independent source/shot/reference IDs and no film runs/anchors; the old
copy retains its discoveries, decisions, transcripts and findings for historical reads.

Historical PR8 attempts used all 92.459 seconds, including the black tail and original
audio. Patrick now authorizes only the already-documented meaningful picture interval
0–36.291667 seconds for Film Intelligence, with corresponding audio and zero-based
timestamps. This does not splice in a desired answer or tailor sampling to a lyric. The
screen-opening relationship is an evaluation question only, never prompt input.
Actual observations, uncertainty, costs and missed relationships are recorded in
[PR8 review](PR8-REVIEW.md). Test-only synthetic discovery is never runtime demo data.

The initial real PR8 attempt prepared eight segments/24 frames, then OpenAI rejected
transcription. No Astra discovery call or transcript/candidate result exists. The
screen-opening question is unassessed, not a claimed detection or miss. The failed
run remains inspectable; no reroll or recorded demo baseline was added.

Derivation command: `node scripts/prepare-analysis-source.mjs`. Full demo regeneration
also derives this file through `scripts/prepare-demo.mjs`; curated PR7 clips/references
are unchanged. [Derived-source verification](PR8-DERIVED-SOURCE.md) records the hash,
frame/sample cut, determinism/preflight and separately authorized live result.
The authorized derived-source run also prepared eight segments/24 frames, then
received HTTP 401 invalid_api_key. No transcript or Astra discovery followed; this
authentication failure is not a film-quality result. No retry or recorded baseline.
