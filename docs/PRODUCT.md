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

## MVP and future

P1 adds visual reference editing, curated demo, optional tour, progress and polished
failure states. The final first-run page will expose both Try the demo film and
Create a project; the demo action is deliberately absent until it works.
Future collaboration, export and generation integrations are not built this sprint.
See [BRD](BRD.md) for acceptance and [STATUS](STATUS.md) for actual delivery state.
