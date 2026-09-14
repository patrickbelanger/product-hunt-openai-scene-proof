# Demo film

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
Audio is omitted because current analysis and media inspection use extracted pictures.

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

No public deployment, anonymous production isolation, spend quota or progress transport
is added. Those remain prerequisites for a public launch. PR7 baseline is empty;
a future recorded baseline must originate from a validated real run and be labelled.
