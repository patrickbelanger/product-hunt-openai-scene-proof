# Product

## Current milestone — PR9

PR8 is merged. PR9 delivers creator-facing UX polish and ordinary-project deletion;
PR10 Security Hardening is next. Historical handoff sections below preserve their
original evidence rather than reopening PR8 architecture.

Film Intelligence is the hero/default workspace tab. Continuity Findings is the
separate frame-level review workspace with the Reference Bible, optional shot
import and evidence inspector. Import supports precise frame inspection or focused
visual comparison; it is not required for whole-film discovery. Saved findings
come from continuity analysis (currently started through the API), not automatic
promotion of Film Intelligence concerns. Creators inspect evidence and explicitly
resolve, dismiss or request re-evaluation using the existing actions.

Real workflow progress, quiet automatic polling, truthful elapsed time, a clearer
Understanding summary/concern hierarchy and compact inspectable evidence make the
existing discovery understandable. Difference != continuity error. Observation →
candidate invariant → creator confirmation → continuity enforcement remains intact.

Keyboard inspection and local Mark In/Out are navigation conveniences only, with
source/clip timestamps and an explicit selected duration. Deletion permanently
removes an ordinary project's owned runtime records/media after deliberate exact-name
confirmation; demo copies use Reset and retain history. Source duration always
describes the current project's source, not an assumed master.

After PR10 unless reprioritized: selected-range Film Understanding. Creator In/Out
→ Analyze selection → explicit paid consent → immutable source-time range input
→ bounded visuals/transcript → provider analysis → saved range provenance/results.
This needs new domain semantics and is not implemented in PR9.

## PR8 final checkpoint

The visual-plus-transcript provider flow is validated end to end on the historical
92.458667-second master. Three discovered anchors remain pending creator confirmation;
concerns are questions, not confirmed errors. New/reset demos use the separately
verified 36.291667-second derivative, without a claimed live success on that asset.
See [final evidence and PR9-only UX backlog](PR8-FINAL-VALIDATION.md). PR8 awaits merge
review; PR9 UX polish is next and is not implemented by this handoff.

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

PR7 adds Try the demo film beside Create a project. It opens the real product with
eight authored video clips, five references and rules from the approved Between the
Line film by Laurie and Patrick. Preparation is explicit and recoverable; no upload
or form is required. A restrained Demo project indicator and confirmed Reset demo
identify working copies. Reset returns a fresh authored copy and retains old history.
The initial findings state is empty: no model result is staged or preloaded.
The real optional tour behaves as in ordinary projects. PR8 adds Film Understanding
stages; public-demo protections remain separate. The app is local and unauthenticated.
Future collaboration, export and generation integrations are not built this sprint.
See [BRD](BRD.md) for acceptance and [STATUS](STATUS.md) for actual delivery state.

## PR8: Film Understanding

Create project → upload a primary source film → explicitly confirm **Understand film**
→ inspect visual + transcript/narrative discovery → Accept / Edit / Reject proposed
anchors → run continuity review using creator-confirmed memory. Uploading, reading,
candidate decisions and Reference Bible promotion never call a model.

Observation → candidate invariant → creator confirmation → continuity enforcement.
AI-discovered entities, narrative interpretations and potential concerns are not
declared truth or findings. Lyrics/dialogue are fallible context; difference is not
automatically an error. Zero supported candidates or concerns is a valid outcome.

The source is immutable per project, at most 120 seconds/100 MiB, MP4/H.264. Up to
24 sampled frames in eight deterministic analysis segments bound discovery, not an
NLE or exhaustive scene detector. Separate imported clips remain available; once
source segments exist, new sequence reviews use those segments rather than mixing
two versions of the film. A different source requires a new project.

Film Intelligence presents actual saved stages, counts, evidence, history and
failures. Polling/reload recovers the durable run; a failed attempt is never silently
replayed. Each new attempt requires fresh paid consent. General loading/error/empty,
responsive/accessibility and workspace polish remain PR9; optional P2 cannot delay
release. See [ADR-0008](adr/ADR-0008-film-understanding.md) and [PR8 review](PR8-REVIEW.md).
