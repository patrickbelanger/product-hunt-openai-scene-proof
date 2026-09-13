# Product

SceneProof: your AI continuity supervisor for generative film.
Primary line: **Keep every shot in character.** Landing: **Make your film remember itself.**

## Vocabulary and journeys

A **Project** is a sequence under review. Its **Reference Bible** records the visual
and textual truth. A **Shot** is an ordered piece of footage; a **Frame** supplies
inspectable visual evidence. A **Finding** is a contextual continuity concern, not
a pixel difference. An **Intentional change** is creator-supplied context, not an
automatic dismissal of every visually similar issue. A **Correction prompt** guides
the creator's next generation outside SceneProof.

Implemented: landing/library → create project → import media → explicit API analysis
→ persisted finding → compare original evidence and copy correction. The library is
persistent and paginated. PR4 adds declare intentional change → save explanation and
scope → independent targeted Astra judgement → inspect the updated judgement and
original history. Astra may accept the explanation, maintain the issue, or report
insufficient evidence. Resolve records a creator correction; dismiss records a
creator decision not to treat the finding. Neither claims model agreement.

PR5 makes the Reference Bible explicit: editable rules and optional visual images
with title and creator guidance. A reference is declared truth; shots and frames
are observed evidence. Astra independently judges context and cites zero or more
references. Angle, lighting, occlusion, narrative change and intent still matter.
Difference alone is not continuity error.

Add/inspect/edit/archive from the workspace. Eight active images maximum, all used
in the next explicitly started sequence analysis. Images never change in place:
replace by archive plus new upload. Text edits affect future analysis; historical
findings retain original submitted metadata/image, including archives. Rules only,
references only, both and neither are supported. Editing never initiates inference.

## MVP and future

PR6 adds an optional quick tour of the existing workspace: Reference Bible → media
and timeline → findings and evidence → resolve and steer. The first workspace visit
offers Start tour or Skip; it never automatically opens a modal. Completion or Skip
is remembered in this browser, and Quick tour always restarts it. Empty projects are
supported without invented findings or actions. The tour never edits or analyzes.
Creator intent adds context; it does not force Astra to agree.

Later P1 slices add curated demo, progress and polished
failure states. The final first-run page will expose both Try the demo film and
Create a project; the demo action is deliberately absent until it works.
Future collaboration, export and generation integrations are not built this sprint.
See [BRD](BRD.md) for acceptance and [STATUS](STATUS.md) for actual delivery state.
