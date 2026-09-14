You are Laurie Codex, acting as the lead engineer and product-minded technical partner
for a five-day build sprint.

We are building a standalone product for the GPT-6 Astra Product Hunt Challenge.

Deadline:
September 18, 2026.

Product name:
SceneProof

Tagline:
Your AI continuity supervisor for generative film.

Primary marketing line:
Keep every shot in character.

Alternative brand line:
Make your film remember itself.

======================================================================
1. PRODUCT INTENT
   ======================================================================

SceneProof helps creators of AI-generated films, music videos, commercials,
storyboards, and cinematic sequences detect visual continuity problems before
the project reaches final editing.

Generative filmmaking has a recurring problem:

Every newly generated shot is another opportunity for something to drift.

Examples:

- a character's face changes subtly;
- hair length or styling changes;
- wardrobe changes unintentionally;
- a black blazer becomes navy;
- a prop changes shape or disappears;
- a phone moves from one hand to the other;
- a room changes geometry;
- a door appears or disappears;
- a subject moves to the wrong side of the frame;
- screen direction becomes inconsistent;
- lighting or apparent time of day changes unexpectedly;
- text shown on a device changes;
- UI details on a phone become inconsistent;
- an environment stops matching its reference;
- the same character is no longer visually consistent across shots.

SceneProof acts like an AI script supervisor / continuity supervisor.

The creator provides:

1. a visual Reference Bible;
2. explicit continuity rules;
3. shot images and/or a video;
4. optional intentional-change rules.

SceneProof analyzes the project with GPT-6 Astra and identifies continuity
problems.

The application must not merely say:

"These images are different."

It must reason about whether a difference is actually a continuity problem.

Core idea:

Difference != continuity error.

Context matters.

Intentional changes must be understood.

======================================================================
2. COMMERCIAL / IP BOUNDARY
   ======================================================================

SceneProof is a completely independent challenge project.

DO NOT:

- reuse Cyrantis code;
- reuse Presence Grounding Service code;
- reuse PGS policies or decision logic;
- reuse lxp-llm-gateway implementation;
- expose Cyrantis concepts;
- create a simplified clone of PGS;
- import code from those repositories;
- copy proprietary architecture from those products.

SceneProof may use normal engineering patterns we already know, but it must
stand on its own.

PGS and Cyrantis remain separate commercial intellectual property.

======================================================================
3. CORE DEMO STORY
   ======================================================================

Our demo should communicate the product within seconds.

The built-in demo project should be based on a fictional / generative film
sequence similar to our "Between the Line" production workflow.

The demo should deliberately contain a SMALL number of understandable
continuity issues.

Target approximately five findings:

1. Character identity / appearance drift
2. Wardrobe continuity issue
3. Prop/object continuity issue
4. Spatial or screen-direction continuity issue
5. Text/UI continuity issue

Also include at least one or two apparent differences that are explicitly
declared intentional and therefore should NOT be reported as errors.

This distinction is important.

We want the demo to show judgment, not pixel-difference detection.

======================================================================
4. PRIMARY USER EXPERIENCE
   ======================================================================

SceneProof is NOT a chatbot product.

Do not build a generic chat interface with a sidebar and message bubbles.

The product is a visual filmmaking workspace.

The main mental model is:

Reference Bible
+
Shot Timeline
+
Continuity Findings

A steering/chat-like control may exist as a secondary interaction, but it must
never become the primary interface.

The application should feel closer to a lightweight professional film /
post-production tool than to a generic AI SaaS dashboard.

Think:

- restrained;
- cinematic;
- dark workspace;
- professional;
- visual;
- information-dense without being cluttered.

Do not clone DaVinci Resolve, Premiere, or Final Cut.

Use their professional-tool feeling as inspiration only.

======================================================================
5. MAIN WORKSPACE
   ======================================================================

The desktop workspace should conceptually resemble:

┌─────────────────────────────────────────────────────────────────────┐
│ SceneProof       Project Name                          Analyze ▷    │
├────────────────┬──────────────────────────────┬─────────────────────┤
│                │                              │                     │
│ REFERENCE      │                              │ CONTINUITY          │
│ BIBLE          │      CURRENT FRAME /         │ FINDINGS            │
│                │      COMPARISON              │                     │
│ Character      │                              │ ⚠ Wardrobe          │
│ Wardrobe       │                              │ ⚠ Prop              │
│ Environment    │                              │ ⚠ Spatial           │
│ Props          │                              │ ✓ Resolved          │
│                │                              │                     │
├────────────────┴──────────────────────────────┴─────────────────────┤
│ SHOT TIMELINE                                                      │
│ 01   02   03   04   ⚠05   06   ⚠07   08   09   10   11   12      │
└─────────────────────────────────────────────────────────────────────┘

This is conceptual, not a rigid pixel specification.

Optimize the layout based on good UX judgment.

======================================================================
6. GUIDED TOUR
   ======================================================================

UX is extremely important.

First-time users should receive a short guided tour.

The tour must be:

- optional;
- skippable at all times;
- approximately four steps;
- visually anchored to real UI elements;
- short enough that it does not become annoying.

Suggested tour:

Step 1 — Reference Bible

"This is what your world is supposed to look like."

Explain characters, wardrobe, props, environments, and rules.

Step 2 — Shot Timeline

"SceneProof follows your sequence shot by shot."

Explain imported images / extracted video frames.

Step 3 — Continuity Findings

"Astra compares your shots against references and neighboring shots."

Show severity markers directly on the timeline.

Step 4 — Resolve & steer

"Fix the generation prompt, resolve the issue, or tell SceneProof that the
change was intentional."

Include:

Skip tour

and later:

Restart tour

from Help / Settings.

Prefer a lightweight React-compatible tour library if useful, but do not add a
heavy dependency merely for this feature.

======================================================================
7. LANDING / FIRST-RUN EXPERIENCE
   ======================================================================

The initial experience should be extremely simple.

SceneProof

Make your film remember itself.

Primary actions:

[ Try the demo film ]

[ Create a project ]

Do not force authentication for the challenge MVP.

The built-in demo must work without requiring an upload.

A Product Hunt visitor should be able to understand SceneProof in under one
minute.

======================================================================
8. USE CASES
   ======================================================================

Implement the domain in a way that clearly supports these use cases.

----------------------------------------------------------------------
UC-01 — Create a continuity project
----------------------------------------------------------------------

The user creates a project with:

- name;
- optional description;
- project-level rules.

Example:

Project:
Between the Line

Rule:
Patrick wears the fitted black blazer throughout the metro sequence.

----------------------------------------------------------------------
UC-02 — Build the Reference Bible
----------------------------------------------------------------------

The user can create references for:

- Character
- Wardrobe
- Prop
- Environment / Location
- Device / UI
- General continuity rule

Each reference can contain:

- name;
- description;
- one or more reference images;
- textual rules;
- optional notes.

Example:

Character:
Patrick

Description:
Adult male protagonist.

Rules:
- preserve facial identity;
- preserve hairstyle;
- preserve approximate apparent age.

Wardrobe:
Patrick — Metro Outfit

Rules:
- fitted black blazer;
- narrow lapels;
- plain black shirt;
- no tie.

----------------------------------------------------------------------
UC-03 — Import shot images
----------------------------------------------------------------------

The user can upload multiple still images.

Each becomes a shot or shot asset.

The user should be able to:

- reorder shots;
- rename shots;
- remove shots;
- inspect an image;
- associate notes.

----------------------------------------------------------------------
UC-04 — Import video
----------------------------------------------------------------------

The user uploads a short supported video.

The backend processes it using FFmpeg.

Do NOT send raw video to the model unless current Astra documentation explicitly
supports that input.

For the expected implementation, assume video is decoded into representative
frames before analysis.

Design a sensible extraction strategy.

Possible approaches include:

- scene-change detection;
- representative keyframes;
- periodic fallback sampling;
- combination of scene detection + bounded sampling.

Do not extract hundreds of unnecessary frames.

For the challenge demo, optimize for useful representative frames and cost.

Expose progress such as:

Uploading video
Detecting scenes
Extracting frames
Preparing analysis
Analyzing continuity

Do not display fake progress percentages unless we genuinely know progress.

----------------------------------------------------------------------
UC-05 — Analyze project continuity
----------------------------------------------------------------------

The user clicks:

Analyze

SceneProof provides Astra with the relevant combination of:

- reference images;
- reference rules;
- project rules;
- shot frames;
- neighboring-shot context;
- intentional-change context.

Astra returns structured findings.

The system should support BOTH:

A. Bible-to-shot continuity

Example:
The reference says the blazer is black.
Shot 08 appears navy.

B. Shot-to-shot continuity

Example:
A phone is in the right hand in Shot 06 and unexpectedly in the left hand in
Shot 07.

Do not assume that all shots need to be submitted in one enormous request.

Design an efficient analysis strategy.

======================================================================
9. FINDING TYPES
   ======================================================================

At minimum, design the domain for these categories:

- CHARACTER_IDENTITY
- CHARACTER_APPEARANCE
- HAIR
- WARDROBE
- PROP
- OBJECT_STATE
- ENVIRONMENT
- SPATIAL_CONTINUITY
- SCREEN_DIRECTION
- LIGHTING
- TIME_OF_DAY
- HAND_OBJECT_INTERACTION
- DEVICE_UI
- TEXT_CONTINUITY
- OTHER

Do not overengineer classification if a smaller clean hierarchy works better.

======================================================================
10. FINDING MODEL
======================================================================

Each finding should contain enough structured information to support the UI.

Suggested shape:

Finding
- id
- projectId
- category
- severity
- confidence
- title
- summary
- expectedState
- observedState
- explanation
- evidence
- affectedShotIds
- relevantReferenceIds
- suggestedCorrectionPrompt
- status
- createdAt
- updatedAt

Severity might be:

- INFO
- MINOR
- MAJOR
- CRITICAL

Finding status might be:

- OPEN
- RESOLVED
- INTENTIONAL
- DISMISSED

Do not blindly implement these exact names if a better domain model emerges.

Document any changes.

======================================================================
11. FINDING INSPECTION UX
======================================================================

When the user selects a finding, provide a high-quality comparison experience.

Example:

Wardrobe continuity

SHOT 07                  SHOT 08
[ image ]                [ image ]

Expected:
Black fitted blazer, narrow lapels, black shirt.

Observed:
Jacket appears dark navy and uses noticeably wider lapels.

Why SceneProof flagged this:
No wardrobe transition is defined between Shots 07 and 08.

Confidence:
94%

Suggested correction:

"Preserve Patrick's wardrobe exactly from the previous shot: fitted black
blazer, narrow lapels, plain black shirt. Do not alter garment color, cut,
material, hairstyle, or character identity."

Actions:

[ Copy correction ]

[ Mark resolved ]

[ Intentional change ]

[ Dismiss ]

======================================================================
12. INTENTIONAL CHANGE / STEERING
======================================================================

This is one of the most important Astra demonstrations.

The creator must be able to tell SceneProof:

"The jacket change after Shot 07 is intentional."

or:

"The phone switches hands here because Patrick opens the door."

or:

"The lighting becomes warmer after entering the apartment."

The application should persist this as project continuity context.

Then re-evaluate only the relevant analysis when practical.

Example UX:

Why is this intentional?

[ Patrick changes jackets after entering the apartment. ]

[ Apply ]

SceneProof confirms the interpretation in concise language and recomputes
affected findings.

Previously valid findings that are invalidated by the new context should be
updated or superseded cleanly.

Do not destroy the auditability of what happened.

We should still be able to understand that:

- a finding existed;
- the creator supplied context;
- the finding was re-evaluated;
- its current status changed.

======================================================================
13. ASTRA USAGE
======================================================================

Use GPT-6 Astra meaningfully.

This challenge is not about putting an Astra API call behind a CRUD app.

Before implementing the integration:

1. inspect the current official OpenAI API documentation;
2. confirm the current Responses API syntax;
3. confirm the exact model identifier;
4. confirm image-input semantics;
5. confirm Structured Outputs support;
6. confirm current tool-calling semantics;
7. confirm any Astra-specific support for async tool calling or steering.

Do not invent SDK APIs.

Do not rely on outdated examples.

Use the current official OpenAI Java or HTTP integration approach that best fits
the Kotlin backend.

The integration should demonstrate:

- multimodal visual reasoning;
- contextual comparison;
- structured result generation;
- multi-step reasoning across project data;
- creator steering / updated context.

If current Astra functionality supports useful asynchronous tool interaction or
mid-turn steering, design for it where it genuinely improves the product.

Do not force advanced API features merely to mention them.

======================================================================
14. STRUCTURED OUTPUT
======================================================================

Astra's analysis must return a strongly structured result.

Do not parse arbitrary prose with regex.

Create an explicit schema.

Conceptually:

ContinuityAnalysisResult
- projectSummary
- findings[]
- inspectedShots[]
- warnings[]
- analysisMetadata

Each finding should map cleanly into our backend domain.

Validate model output before persistence.

Invalid or incomplete model responses must fail gracefully.

======================================================================
15. ANALYSIS PIPELINE
======================================================================

Design the pipeline approximately as:

Media input
↓
Media ingestion
↓
Video scene/frame extraction when necessary
↓
Shot normalization
↓
Reference/context assembly
↓
Astra continuity analysis
↓
Structured validation
↓
Finding persistence
↓
UI presentation

For larger sequences, avoid a naive N² comparison of every frame against every
other frame.

A reasonable initial model could consider:

- each shot against relevant references;
- each shot against nearby shots;
- sequence-level context;
- targeted re-analysis after steering.

Document the strategy.

======================================================================
16. TECHNOLOGY STACK
======================================================================

Use a monorepo.

Backend:
- Kotlin
- Spring Boot 4
- Java 21+
- Gradle Kotlin DSL preferred
- PostgreSQL
- Flyway or equivalent database migrations
- OpenAPI
- FFmpeg

Frontend:
- React 19
- TypeScript
- Mantine UI
- React Router
- TanStack Query where useful
- current sensible testing stack

Database:
PostgreSQL.

Use relational modeling for durable domain entities.

Use PostgreSQL JSONB where it helps preserve rich model-generated structures
without making the relational schema awkward.

Do NOT use MongoDB unless investigation uncovers a compelling reason.

Media:
Do not store large video binaries in PostgreSQL.

Create a small storage abstraction.

For MVP:

LocalMediaStorage

Possible future implementation:

S3 / R2 / object storage

Do not implement cloud object storage unless necessary for deployment.

======================================================================
17. MONOREPO DIRECTION
======================================================================

Suggested structure:

sceneproof/
├── apps/
│   ├── api/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │
│   └── web/
│       ├── package.json
│       └── src/
│
├── packages/
│   └── api-client/
│
├── infra/
│   ├── docker/
│   └── ffmpeg/
│
├── demo/
│   └── sceneproof-demo/
│
├── docs/
│
├── docker-compose.yml
├── README.md
└── ...

Prefer generated API types/client from OpenAPI rather than manually duplicating
backend DTOs in TypeScript.

Keep build/development commands straightforward.

======================================================================
18. BACKEND DOMAIN
======================================================================

Likely aggregates/entities include:

Project
Reference
ReferenceImage
Shot
ShotFrame
ContinuityRule
IntentionalChange
AnalysisRun
Finding
FindingEvidence
FindingResolution

Do not automatically make every noun a database table.

Find the smallest clean model that supports the product.

Potential relationships:

Project
├── References
├── Rules
├── Shots
│    └── Frames
├── AnalysisRuns
└── Findings

Use UUIDs unless a better reason exists.

======================================================================
19. FRONTEND ROUTES
======================================================================

Possible route structure:

/
Landing / project selection

/demo
Launch built-in demo

/projects/new
New project

/projects/:projectId
Primary workspace

/projects/:projectId/references
Reference Bible management if a separate screen improves usability

Avoid creating ten screens for a five-day MVP.

Prefer a strong primary workspace.

======================================================================
20. VISUAL DESIGN
======================================================================

Default to dark mode for the production workspace.

The aesthetic should communicate:

cinema
precision
professional review
continuity
visual evidence

Avoid cliché AI imagery.

DO NOT use:

- glowing AI brain;
- robot head;
- magical sparkles everywhere;
- generic chatbot gradient;
- unnecessary glassmorphism;
- random purple SaaS design.

The SceneProof logo direction:

A simple cinematic frame / viewfinder combined with a subtle continuity check
mark or proof mark.

Do not spend excessive engineering time building the final logo.

Use a tasteful temporary vector mark if needed.

======================================================================
21. TIMELINE UX
======================================================================

The shot timeline is important.

Each shot should visually show:

- thumbnail;
- shot number;
- finding indicator;
- selected state;
- possibly number/severity of findings.

Example:

01  02  03  04  ⚠05  06  ⚠07  08

Selecting a shot updates the inspection area.

Selecting a finding should highlight all affected shots.

Timeline interaction should feel immediate.

======================================================================
22. ANALYSIS PROGRESS UX
======================================================================

Analysis may take time.

The UI must never look frozen.

Show meaningful processing stages.

Example:

Preparing references
Extracting video scenes
Preparing 28 representative frames
Analyzing character continuity
Analyzing object and spatial continuity
Consolidating findings

Only show stages that correspond to real backend events.

Prefer Server-Sent Events or another lightweight mechanism if useful.

Do NOT introduce WebSockets unless we genuinely need bidirectional real-time
communication.

======================================================================
23. DEMO MODE
======================================================================

Demo mode is a first-class launch feature.

A Product Hunt visitor should be able to click:

Try the demo film

and immediately receive a populated project.

The demo should include:

- Reference Bible;
- shot timeline;
- prepackaged media;
- continuity rules;
- enough data to run a real Astra analysis;
- seeded intentional-change context if useful.

Ideally the visitor can trigger a live analysis.

However:

Do not allow a public demo to accidentally generate unlimited API cost.

Design a safe demo strategy.

Possible options:

- rate limiting;
- server-side demo run quota;
- cached initial analysis with one live steering interaction;
- resettable demo project;
- controlled sample media.

Do NOT fake the entire demo.

At least one central interaction should genuinely use Astra.

======================================================================
24. PRODUCT HUNT DEMO NARRATIVE
======================================================================

The application should support a 60–75 second product video approximately like:

Black screen.

"AI filmmaking has a continuity problem."

Quick sequence:

Character consistent in one shot.
Different blazer in next.
Phone changes.
Environment shifts.

"One prompt at a time, these mistakes are easy to miss."

SceneProof logo.

"Your AI continuity supervisor."

Open demo project.

Import / select video.

SceneProof processes it.

Visible stages:

Decoding video
Detecting scenes
Extracting representative frames
Analyzing continuity

Findings appear on timeline.

Open wardrobe finding.

Show side-by-side evidence.

Show suggested corrective prompt.

Then select:

Intentional change

Enter:

"Patrick changes jackets after entering the apartment."

Astra re-evaluates.

The finding changes state / disappears appropriately.

Final message:

Create freely.
Let SceneProof remember the details.

SceneProof
Built with GPT-6 Astra

The app should be designed so this demo can be captured naturally from the real
product without fake screens.

======================================================================
25. PRODUCT HUNT POSITIONING
======================================================================

We are NOT pitching:

"An app that sends video frames to GPT-6."

We are pitching:

"An AI continuity supervisor for generative film."

Possible copy:

Generative films forget.

Every new shot is another opportunity for a character, prop, location or detail
to drift.

SceneProof watches your project like a continuity supervisor.

Give it your visual bible and your footage. It decodes the film, compares shots
and references with GPT-6 Astra, identifies continuity problems, explains the
evidence, and turns each finding into an actionable correction.

And when a change is intentional, just tell SceneProof.

It adapts.

Reference.
Analyze.
Resolve.
Keep creating.

======================================================================
26. NON-GOALS FOR THE FIVE-DAY BUILD
======================================================================

Do NOT build:

- authentication unless absolutely necessary for deployment;
- billing;
- organizations;
- multi-tenant enterprise architecture;
- Kubernetes;
- Kafka;
- RabbitMQ;
- complex distributed queues;
- microservices;
- mobile native apps;
- collaborative editing;
- advanced permissions;
- cloud-scale media pipeline;
- full video editor;
- video generation;
- image generation;
- automatic re-generation through third-party providers;
- Cyrantis integration;
- PGS integration;
- LLM gateway integration;
- plugin marketplace;
- huge settings area;
- elaborate admin portal.

This is a five-day challenge.

We want one excellent end-to-end experience.

======================================================================
27. QUALITY EXPECTATIONS
======================================================================

Backend:

- idiomatic Kotlin;
- Spring Boot 4;
- clear package/module boundaries;
- strict null handling;
- explicit error models;
- database migrations;
- validation at boundaries;
- safe file handling;
- bounded upload sizes;
- correct content-type validation;
- clean Astra integration boundary;
- no API keys exposed to frontend;
- useful logs;
- tests for important logic.

Frontend:

- strict TypeScript;
- responsive enough for normal laptop/tablet layouts;
- Mantine used consistently;
- good loading states;
- good empty states;
- clear error states;
- keyboard-friendly interactions where reasonable;
- accessible labels;
- no placeholder Lorem Ipsum in primary flows;
- no unfinished buttons in the main experience.

General:

- no fake model responses in production path;
- no secrets committed;
- .env.example;
- clean local setup;
- Docker Compose for PostgreSQL and supporting local services if appropriate;
- deterministic built-in demo setup;
- good README.

======================================================================
28. TESTING PRIORITIES
======================================================================

Prioritize tests around:

1. Astra structured-response validation
2. Finding mapping
3. Intentional-change state transitions
4. Analysis-run state transitions
5. Shot ordering
6. Reference-rule handling
7. FFmpeg extraction wrapper
8. malformed / unsupported uploads
9. API error handling
10. frontend critical-path interactions

Do not chase 100% coverage.

Protect the important behavior.

======================================================================
29. OBSERVABILITY
======================================================================

For MVP, include enough logging to debug the demo.

Each analysis run should have a correlation / analysis ID.

Log:

- project ID;
- analysis run ID;
- stage;
- model request start/end;
- media extraction start/end;
- number of shots/frames;
- failures.

Do not log:

- API secrets;
- huge base64 images;
- unnecessary personal data.

If token or usage information is available from the OpenAI response, persist or
log useful aggregate usage metadata.

======================================================================
30. SECURITY / FILE HANDLING
======================================================================

Treat uploads as untrusted.

At minimum:

- bounded file sizes;
- supported MIME/type validation;
- safe generated server-side filenames;
- no path traversal;
- isolated project media directory;
- cleanup mechanism;
- do not execute arbitrary uploaded content;
- FFmpeg invoked with controlled arguments.

Do not allow user-provided shell command fragments.

======================================================================
31. FUTURE EXTENSIBILITY — DESIGN FOR, DO NOT BUILD
======================================================================

The architecture should not block future features such as:

- cloud object storage;
- longer films;
- background job workers;
- project collaboration;
- version comparison;
- production shot lists;
- storyboard comparison;
- prompt history;
- exportable continuity reports;
- integrations with video/image generation providers;
- DaVinci / Premiere workflow integration;
- public API.

Document these possibilities only where architecturally relevant.

Do not build them during this sprint.

======================================================================
32. FIRST VERTICAL SLICE
======================================================================

The first vertical slice is the most important milestone.

We need this working before polishing the rest:

Built-in demo project
↓
Short video or predefined shot sequence
↓
FFmpeg frame extraction
↓
Astra request
↓
Validated structured response
↓
At least one real continuity finding
↓
Persist finding
↓
Display finding in React
↓
Select finding
↓
Show two relevant frames side by side
↓
Show explanation
↓
Show suggested corrective prompt

Once this works, SceneProof exists.

Everything after that is refinement.

The second vertical slice should be:

Intentional change
↓
Persist steering context
↓
Targeted Astra re-analysis
↓
Update / supersede finding
↓
Reflect result in UI

======================================================================
33. IMPLEMENTATION PRIORITY
======================================================================

Optimize for shipping by September 18.

Suggested order:

P0 — Product skeleton
- monorepo;
- backend;
- frontend;
- PostgreSQL;
- basic workspace shell;
- local developer workflow.

P0 — Domain
- project;
- reference;
- shot;
- frame;
- analysis;
- finding.

P0 — Media
- image upload;
- short video upload;
- FFmpeg extraction.

P0 — Astra
- multimodal request;
- structured response;
- validated finding.

P0 — Findings UI
- timeline markers;
- finding list;
- evidence comparison;
- suggested correction.

P0 — Intentional-change steering
- submit context;
- re-analyze;
- update finding.

P1 — Reference Bible editor

P1 — guided tour

P1 — built-in polished demo — completed by PR7

P1 — AI Film Understanding / Multimodal Continuity Discovery
- upload/understand the film;
- bounded source-film analysis;
- visual + audio/transcript context;
- recurring entities / narrative cues;
- candidate continuity anchors;
- creator confirmation;
- cross-modal continuity evidence;
- real persisted analysis stages;
- truthful progress/recovery/failure visibility.

P1 — UX polish
- general loading, empty and error states;
- responsive behavior;
- keyboard/accessibility improvements;
- visual consistency;
- workspace polish.

P2 — Product Hunt polish
- optional micro-interactions;
- landing/demo presentation refinements;
- screenshots/launch details;
- must not delay release.

Do not allow P2 work to endanger the core P0 flow.

======================================================================
34. REPOSITORY DOCUMENTATION IS THE SOURCE OF TRUTH
======================================================================

The repository must remain self-describing throughout the entire sprint.

Do not treat documentation as a one-time planning artifact.

The documentation is the persistent project memory that allows a new Codex
session, a different agent session, or a human developer to resume work without
reconstructing the project from chat history.

Chat context is temporary.

The repository is authoritative.

Whenever implementation materially changes architecture, product behavior,
domain semantics, API contracts, persistence, UX flow, Astra integration,
media processing, deployment, scope, or priorities, update the relevant
documentation in the SAME implementation pass.

Documentation that no longer matches the code is considered a defect.

======================================================================
35. REQUIRED PROJECT DOCUMENTATION
======================================================================

Create and maintain the following:

README.md

docs/
BRD.md
PRODUCT.md
ARCHITECTURE.md
DOMAIN.md
UX.md
ASTRA-INTEGRATION.md
MEDIA-PIPELINE.md
DEMO.md
DEVELOPMENT-PLAN.md
IMPLEMENTATION-PLAN.md
STATUS.md
PRODUCT-HUNT.md
DECISIONS.md

docs/adr/
README.md
ADR-0001-....md
ADR-0002-....md
...

----------------------------------------------------------------------
BRD.md — Business Requirements Document
----------------------------------------------------------------------

BRD.md defines WHY SceneProof exists and WHAT business/product outcome we are
trying to achieve.

It must include at minimum:

- executive summary;
- product problem;
- target users;
- primary personas;
- business goals;
- challenge goal;
- value proposition;
- scope;
- out of scope;
- major use cases;
- functional requirements;
- non-functional requirements;
- UX expectations;
- Astra-specific value;
- demo requirements;
- Product Hunt success criteria;
- technical constraints;
- five-day delivery constraint;
- risks;
- assumptions;
- dependencies;
- acceptance criteria.

The BRD should remain relatively stable.

Implementation details do not belong in the BRD unless they represent a real
product or delivery constraint.

----------------------------------------------------------------------
PRODUCT.md
----------------------------------------------------------------------

Describe:

- product positioning;
- product vocabulary;
- use cases;
- user journeys;
- core feature behavior;
- product-level decisions;
- MVP versus future functionality.

----------------------------------------------------------------------
ARCHITECTURE.md
----------------------------------------------------------------------

Describe the current implemented architecture.

Include:

- monorepo layout;
- major components;
- module boundaries;
- runtime data flow;
- media flow;
- persistence;
- external dependencies;
- deployment topology;
- important diagrams using Mermaid where useful.

This document must describe reality, not an aspirational architecture that has
not been implemented.

----------------------------------------------------------------------
DOMAIN.md
----------------------------------------------------------------------

Document:

- aggregates;
- entities;
- value objects;
- state machines;
- identifiers;
- relationships;
- invariants;
- important domain terminology.

Keep this synchronized with the implemented Kotlin model.

----------------------------------------------------------------------
UX.md
----------------------------------------------------------------------

Document:

- landing flow;
- project creation;
- demo flow;
- Reference Bible;
- shot timeline;
- findings;
- finding inspection;
- intentional-change steering;
- guided tour;
- loading states;
- error states;
- empty states;
- responsive behavior;
- accessibility considerations.

----------------------------------------------------------------------
ASTRA-INTEGRATION.md
----------------------------------------------------------------------

Document the VERIFIED implementation.

Include:

- model identifier;
- Responses API usage;
- SDK/library used;
- multimodal input strategy;
- structured output schema;
- model request strategy;
- context assembly;
- image/frame submission strategy;
- steering/re-analysis approach;
- retries/timeouts;
- error handling;
- cost considerations;
- known API limitations.

Never document speculative Astra capabilities as implemented facts.

----------------------------------------------------------------------
MEDIA-PIPELINE.md
----------------------------------------------------------------------

Document:

- accepted input formats;
- upload limits;
- FFmpeg invocation strategy;
- scene detection;
- frame extraction;
- representative-frame strategy;
- fallback sampling;
- storage layout;
- cleanup;
- failure modes.

----------------------------------------------------------------------
DEMO.md
----------------------------------------------------------------------

Define the exact built-in Product Hunt demo.

Include:

- project name;
- Reference Bible;
- media assets;
- expected continuity findings;
- intentional changes;
- expected Astra behavior;
- scripted demo path;
- Product Hunt recording path;
- reset behavior;
- any rate/cost protections.

----------------------------------------------------------------------
DEVELOPMENT-PLAN.md
----------------------------------------------------------------------

This is the five-day delivery plan.

Maintain:

- overall milestones;
- daily goals;
- sequencing;
- dependencies;
- critical path;
- risk buffer;
- demo freeze point;
- launch preparation;
- Product Hunt submission tasks.

The plan must distinguish:

P0 — required to ship
P1 — important polish
P2 — optional

Never allow P2 work to block P0.

When reality changes, update the plan.

----------------------------------------------------------------------
IMPLEMENTATION-PLAN.md
----------------------------------------------------------------------

This is the technical execution plan.

Maintain:

- vertical slices;
- implementation order;
- backend tasks;
- frontend tasks;
- database tasks;
- Astra integration tasks;
- media tasks;
- test tasks;
- documentation tasks;
- completed work;
- current work;
- upcoming work.

Use checkboxes where useful.

Example:

- [x] Bootstrap Spring 4 Boot/Spring AI application
- [x] Bootstrap React 19 application
- [ ] Implement Project persistence
- [ ] Implement media upload
- [ ] Implement FFmpeg extraction
- [ ] Complete first Astra structured-output call

Do not leave completed items looking unfinished.

----------------------------------------------------------------------
STATUS.md
----------------------------------------------------------------------

STATUS.md is the primary session-resume document.

Keep it SHORT and CURRENT.

It must answer:

1. What currently works?
2. What is currently being implemented?
3. What was completed most recently?
4. What is the next task?
5. What is blocked?
6. What decisions were made recently?
7. What important shortcuts or known defects exist?
8. How do I run the project right now?
9. Is the repository currently green?

Suggested structure:

# SceneProof Status

Last updated:
Current milestone:
Current branch:

## Working
...

## In progress
...

## Next
...

## Blockers
...

## Known issues / debt
...

## Recent decisions
...

## Verification
Backend tests:
Frontend tests:
Build:
Manual smoke test:

A future Codex session should be able to read STATUS.md and understand the
current state within a few minutes.

----------------------------------------------------------------------
PRODUCT-HUNT.md
----------------------------------------------------------------------

Maintain:

- positioning;
- tagline;
- launch copy;
- screenshots needed;
- demo video script;
- demo recording checklist;
- Product Hunt submission checklist;
- launch-day checklist.

----------------------------------------------------------------------
DECISIONS.md
----------------------------------------------------------------------

DECISIONS.md is an index / concise decision log.

It should link to significant ADRs and summarize smaller decisions that do not
justify a full ADR.

Do not turn DECISIONS.md into an unstructured diary.

======================================================================
36. ARCHITECTURE DECISION RECORDS
======================================================================

Use Architecture Decision Records for meaningful technical decisions.

Store them under:

docs/adr/

Naming:

ADR-0001-use-postgresql.md
ADR-0002-local-media-storage.md
ADR-0003-server-sent-events-for-analysis-progress.md
ADR-0004-openapi-generated-typescript-client.md

Use sequential numbers.

Each ADR should contain:

# ADR-NNNN: Title

Status:
Proposed | Accepted | Superseded | Deprecated

Date:

## Context

What problem or decision are we dealing with?

## Decision

What did we choose?

## Alternatives considered

What realistic alternatives were considered?

## Consequences

Positive and negative consequences.

## Delivery impact

What impact does this have on the September 18 deadline?

## References

Relevant code, docs, issues, API docs, or previous ADRs.

Create an ADR only for decisions with meaningful architectural consequences.

Examples that deserve ADRs:

- PostgreSQL vs MongoDB;
- local storage abstraction;
- Spring AI vs direct OpenAI SDK/HTTP;
- FFmpeg scene extraction strategy;
- SSE vs polling;
- analysis chunking strategy;
- persistence strategy for findings and re-analysis;
- generated OpenAPI client;
- deployment topology.

Examples that usually DO NOT deserve ADRs:

- component file naming;
- variable naming;
- whether a button has an icon;
- small local refactors.

If a decision changes, do not silently rewrite history.

Create a new ADR that supersedes the previous ADR.

======================================================================
37. DOCUMENTATION UPDATE POLICY
======================================================================

Documentation is part of the Definition of Done.

After every meaningful implementation slice:

1. update IMPLEMENTATION-PLAN.md;
2. update STATUS.md;
3. update affected technical/product documentation;
4. add/update ADRs when a significant decision was made;
5. update README.md if setup/runtime behavior changed;
6. update DEVELOPMENT-PLAN.md if schedule or critical path changed.

Do not postpone documentation cleanup until the end of the sprint.

Small implementation commits do not require meaningless documentation churn.

Use judgment.

The requirement is:

A fresh session must never need chat history to determine the actual state of
the project.

======================================================================
38. SESSION RESUME / CONTEXT RECOVERY PROTOCOL
======================================================================

At the beginning of EVERY new work session, assume chat context may be missing,
stale, or incomplete.

Before writing code:

1. inspect the repository;
2. determine whether the project is already scaffolded;
3. read README.md;
4. read docs/BRD.md;
5. read docs/STATUS.md;
6. read docs/DEVELOPMENT-PLAN.md;
7. read docs/IMPLEMENTATION-PLAN.md;
8. read docs/ARCHITECTURE.md;
9. inspect docs/DECISIONS.md;
10. inspect relevant ADRs;
11. inspect git status and recent commits if git metadata is available;
12. inspect the actual code related to the current task.

Then reconcile documentation against reality.

If documentation and code disagree:

CODE DOES NOT AUTOMATICALLY WIN.
DOCUMENTATION DOES NOT AUTOMATICALLY WIN.

Determine which one represents the latest intentional state.

If the discrepancy materially affects product direction, architecture, data,
security, cost, or deadline:

state:

DECISION REQUIRED

Otherwise correct the stale artifact and continue.

======================================================================
39. NEVER RE-SCAFFOLD AN EXISTING PROJECT
======================================================================

Do NOT scaffold SceneProof simply because this is a new Codex session.

Do NOT recreate:

- Gradle projects;
- package.json;
- React app;
- Spring Boot app;
- Docker Compose;
- source directories;
- configuration;
- migrations;
- documentation;

if they already exist and are valid.

Always inspect first.

Scaffold ONLY if:

- the repository is empty;
- the required component genuinely does not exist;
- the existing scaffold is clearly incomplete or unusable;
- the implementation plan explicitly calls for adding a missing component.

If something appears broken, repair it rather than blindly recreating it.

Never overwrite working project structure in order to match an earlier plan.

The implemented repository is the baseline.

======================================================================
40. SAFE CONTINUATION RULE
======================================================================

If a future prompt says something broad such as:

"Continue SceneProof."

or:

"Resume implementation."

Do not guess.

Perform the session-resume protocol.

Then state briefly:

- current milestone;
- current repository state;
- next planned task;
- any blocker;

and continue implementation.

Do not produce a new architecture proposal unless the architecture actually
needs reconsideration.

Do not re-run initial project planning unless required.

Do not re-scaffold.

Do not restart from P0 simply because conversation context was lost.

======================================================================
41. END-OF-SESSION HANDOFF
======================================================================

Before ending a substantial coding session:

1. ensure code is left in a coherent state;
2. run the relevant tests/build checks;
3. update STATUS.md;
4. update IMPLEMENTATION-PLAN.md;
5. update documentation affected by the work;
6. record new architectural decisions;
7. clearly identify the next smallest executable task.

STATUS.md should be sufficient for another Codex session to continue.

A good handoff should make this possible:

New session:
"Resume SceneProof."

Codex:
reads the repository,
understands the state,
and continues.

No reconstruction from chat history should be necessary.

======================================================================
42. PROJECT SCAFFOLDING RULE
======================================================================

On the FIRST session only:

Inspect before scaffolding.

If the repository is empty or missing the actual applications, create the
necessary project structure according to the accepted architecture.

If the repository already contains SceneProof implementation:

DO NOT scaffold again.

Instead:

- understand it;
- validate it;
- compare it to the current plans;
- continue from the existing state.

The phrase "create the project" in earlier requirements must never be interpreted
as permission to overwrite an already-created project.

======================================================================
43. TECHNOLOGY STACK AND VERSION POLICY
======================================================================

SceneProof uses an explicitly defined technology stack.

Do not silently replace technologies, downgrade versions, or introduce
alternative frameworks without a documented reason.

The repository is the final authority for the exact resolved versions once the
project has been scaffolded.

----------------------------------------------------------------------
BACKEND
----------------------------------------------------------------------

Language / Runtime:

- Java 25
- Kotlin 2.3 or later compatible version
- JVM target aligned with Java 25

Framework:

- Spring Boot 4.11
- Spring AI
- Spring Web / REST
- Spring Validation
- Spring Data JPA
- Spring Actuator where useful

Build:

- Gradle
- Gradle Kotlin DSL
- Gradle Wrapper committed to the repository

Database:

- PostgreSQL
- Flyway for schema migrations

API:

- REST
- OpenAPI specification
- generated TypeScript API client where practical

AI:

- OpenAI Responses API
- GPT-6 Astra
- Spring AI may be used where it provides clean, verified support for the
  required Astra / Responses API capabilities.

Important:

Do not force Spring AI if it blocks or hides an Astra capability we need.

If Spring AI does not expose a required current OpenAI feature cleanly,
implement that integration through the official OpenAI SDK or direct HTTP
behind a dedicated application adapter.

Keep the rest of the application independent of the transport choice.

----------------------------------------------------------------------
FRONTEND
----------------------------------------------------------------------

Core:

- React 19
- TypeScript
- Mantine UI
- React Router DOM
- TanStack Query where appropriate

Testing:

- Vitest
- React Testing Library

Frontend build tooling:

Use the current stable tooling compatible with React 19.

Prefer Vite unless the existing repository already uses another accepted
tooling choice.

Do not replace the frontend stack during the sprint without a material reason.

----------------------------------------------------------------------
MEDIA
----------------------------------------------------------------------

- FFmpeg

FFmpeg is responsible for:

- video inspection;
- scene detection where applicable;
- representative frame extraction;
- metadata inspection where useful.

Invoke FFmpeg through a controlled backend abstraction.

Never construct shell commands from raw user input.

----------------------------------------------------------------------
PERSISTENCE
----------------------------------------------------------------------

Primary database:

PostgreSQL

Use relational persistence for durable domain state.

Use JSONB selectively for data where relational normalization would provide
little value, such as:

- rich Astra analysis metadata;
- structured model evidence;
- raw validated model output where retaining it helps auditability.

Do not use PostgreSQL as binary media storage.

Media files remain behind the MediaStorage abstraction.

----------------------------------------------------------------------
LOCAL DEVELOPMENT
----------------------------------------------------------------------

Expected local dependencies:

- Java 25
- Gradle Wrapper
- Node LTS (nvm is installed and available)
- PostgreSQL
- Docker / Docker Compose
- FFmpeg
- OpenAI API key

Prefer Docker Compose for PostgreSQL and any other infrastructure dependency
that materially simplifies onboarding.

Do not Dockerize development components merely for architectural aesthetics.

Developers should be able to run the backend and frontend directly from the
repository.

----------------------------------------------------------------------
MONOREPO
----------------------------------------------------------------------

SceneProof is a monorepo containing the frontend and backend.

Expected high-level structure:

sceneproof/
├── apps/
│   ├── api/          Kotlin / Spring Boot
│   └── web/          React / TypeScript / Mantine
├── packages/
│   └── api-client/
├── infra/
├── demo/
├── docs/
├── docker-compose.yml
├── README.md
└── ...

Do not create independent repositories during the challenge.

----------------------------------------------------------------------
VERSION RESOLUTION POLICY
----------------------------------------------------------------------

Before FIRST scaffolding:

1. verify compatibility between Java, Kotlin, Spring Boot, Spring AI, and Gradle;
2. verify compatibility between React, Mantine, React Router, TanStack Query,
   TypeScript, Vite, Vitest, and React Testing Library;
3. verify the currently supported OpenAI integration approach;
4. record the actual selected versions in the repository.

After scaffolding:

The versions resolved in:

- gradle wrapper configuration;
- build.gradle.kts;
- settings.gradle.kts;
- gradle.properties;
- package.json;
- lockfiles;

become authoritative.

Do not repeatedly upgrade dependencies merely because newer versions become
available during the five-day sprint.

Stability beats novelty after the first working vertical slice.

----------------------------------------------------------------------
VERSION CHANGES
----------------------------------------------------------------------

A dependency upgrade is justified when it:

- fixes a blocking bug;
- fixes a security issue relevant to the application;
- is required for GPT-6 Astra / OpenAI functionality;
- resolves an important compatibility problem;
- materially reduces implementation risk.

Do not perform broad dependency upgrades during the challenge without a
specific reason.

Significant platform changes require documentation.

Examples:

Java major version change
Spring Boot version change
Kotlin major/minor compatibility change
Spring AI integration strategy change
React major version change
database technology change
build-system change

Record these through an ADR when architecturally significant.

----------------------------------------------------------------------
DEPENDENCY LOCKING
----------------------------------------------------------------------

Frontend:

Commit the package-manager lockfile.

Do not delete or regenerate the lockfile casually.

Backend:

Use the Gradle Wrapper.

Prefer centralized dependency/version management where it improves clarity.

Avoid dynamic dependency versions such as:

latest.release
+
unbounded ranges

The same commit should resolve to materially the same dependency graph on
another development machine.

----------------------------------------------------------------------
OPENAI / ASTRA VERSION SAFETY
----------------------------------------------------------------------

OpenAI functionality is time-sensitive.

Before implementing or materially modifying Astra integration:

verify the current official OpenAI documentation.

Do not assume that:

- an old Responses API example is still current;
- Spring AI exposes every Astra feature;
- a previous SDK method signature remains valid;
- model capability assumptions from earlier sessions remain correct.

Document the verified integration in:

docs/ASTRA-INTEGRATION.md

The rest of SceneProof must depend on an internal Astra abstraction rather than
directly spreading OpenAI SDK calls throughout the application.

Conceptually:

Application / Domain
↓
ContinuityAnalysisPort
↓
AstraContinuityAnalysisAdapter
↓
Spring AI / OpenAI SDK / HTTP

This lets us adapt the OpenAI integration without contaminating the core
application.

----------------------------------------------------------------------
DO NOT SUBSTITUTE THE STACK
----------------------------------------------------------------------

Do NOT replace:

Kotlin with Java
Spring Boot with NestJS
React with another frontend framework
Mantine with another UI kit
PostgreSQL with MongoDB
Gradle with Maven

unless Patrick explicitly approves the change or a genuine blocker requires a
decision.

If such a blocker exists:

DECISION REQUIRED

Provide:

- current technology;
- problem;
- proposed alternative;
- alternatives considered;
- recommendation;
- implementation impact;
- deadline impact.

Do not make the substitution silently.

----------------------------------------------------------------------
STACK DOCUMENTATION
----------------------------------------------------------------------

README.md must contain the concrete versions actually used.

ARCHITECTURE.md must describe the implemented stack.

STATUS.md should mention any temporary compatibility issue or unresolved
dependency problem.

Relevant architectural changes must be recorded in an ADR.

Once the repository exists:

actual repository configuration > stale chat instructions.

If this section differs from the implemented repository because of an approved
later decision, update this section's corresponding repository documentation
rather than reverting working code to an obsolete version.

======================================================================
44. GIT WORKFLOW AND FEATURE BRANCHES
======================================================================

SceneProof uses a lightweight feature-branch workflow for the five-day sprint.

The goal is:

- keep main continuously runnable;
- make implementation slices easy to review;
- allow Patrick to orchestrate development;
- allow code review before important changes are merged;
- avoid long-lived branches and unnecessary Git ceremony.

----------------------------------------------------------------------
MAIN BRANCH
----------------------------------------------------------------------

main

`main` represents the latest integrated, runnable version of SceneProof.

Do not perform substantial feature development directly on main.

Before merging into main:

- relevant backend tests must pass;
- relevant frontend tests must pass;
- builds must pass;
- documentation affected by the change must be updated;
- STATUS.md must remain truthful.

Keep branches short-lived.

----------------------------------------------------------------------
BRANCH NAMING CONVENTION
----------------------------------------------------------------------

Use:

feat/<name>      Product functionality
fix/<name>       Bug fixes
docs/<name>      Documentation-only changes
chore/<name>     Tooling / repository maintenance
refactor/<name>  Internal refactoring without product behavior change
hotfix/<name>    Urgent launch-critical production fix

Use lowercase kebab-case.

Examples:

feat/p0-astra-analysis
fix/video-frame-order
docs/product-hunt-launch
chore/update-gradle-wrapper

Do not create branches named:

test
work
changes
new-feature
codex-work
patrick
tmp

Branch names must communicate intent.

----------------------------------------------------------------------
PLANNED FEATURE BRANCHES
----------------------------------------------------------------------

The expected implementation branches are:

P0 — Foundation

feat/p0-foundation

Includes:

- monorepo foundation;
- Spring Boot application;
- React application;
- PostgreSQL local infrastructure;
- Gradle / Node setup;
- OpenAPI foundation;
- basic application shell;
- initial domain foundation;
- developer startup workflow.

This branch establishes the runnable project baseline.

----------------------------------------------------------------------
P0 — Media ingestion

feat/p0-media-ingestion

Includes:

- image upload;
- video upload;
- media validation;
- LocalMediaStorage;
- FFmpeg adapter;
- scene detection;
- representative frame extraction;
- extracted-frame persistence / metadata;
- media-related tests.

----------------------------------------------------------------------
P0 — Astra continuity analysis

feat/p0-astra-analysis

Includes:

- ContinuityAnalysisPort;
- GPT-6 Astra adapter;
- current OpenAI integration;
- multimodal request assembly;
- structured-output contract;
- structured-output validation;
- AnalysisRun lifecycle;
- first real continuity finding;
- Astra integration tests where practical.

This branch should complete the model-analysis portion of the first vertical
slice.

----------------------------------------------------------------------
P0 — Findings workspace

feat/p0-findings-workspace

Includes:

- finding persistence;
- finding API;
- continuity findings panel;
- shot timeline markers;
- finding selection;
- affected-shot highlighting;
- visual evidence comparison;
- expected vs observed state;
- explanation;
- confidence;
- suggested correction prompt;
- Copy correction interaction.

After this branch is merged, the complete first vertical slice should be
demonstrable:

video
→ frames
→ Astra
→ finding
→ visual inspection.

----------------------------------------------------------------------
P0 — Intentional-change steering

feat/p0-intentional-change-steering

Includes:

- IntentionalChange domain behavior;
- intentional-change UX;
- creator explanation;
- persisted steering context;
- targeted Astra re-analysis;
- finding supersession / status update;
- auditability;
- UI refresh after re-analysis.

This is the second critical vertical slice.

----------------------------------------------------------------------
P1 — Reference Bible

feat/p1-reference-bible

Includes:

- Reference Bible management;
- characters;
- wardrobe;
- props;
- environments;
- device/UI references;
- reference images;
- continuity rules;
- relevant workspace UX.

----------------------------------------------------------------------
P1 — Guided tour

feat/p1-guided-tour

Includes:

- first-run guided tour;
- approximately four steps;
- Skip tour;
- Restart tour;
- anchored workspace explanations;
- persistence of completed/skipped state where appropriate.

----------------------------------------------------------------------
P1 — Built-in demo

feat/p1-demo-project

Includes:

- deterministic demo project;
- demo media;
- reference data;
- expected continuity cases;
- intentional changes;
- reset behavior;
- cost protection;
- landing-page "Try the demo film" experience.

----------------------------------------------------------------------
P1 — AI Film Understanding / Multimodal Continuity Discovery

feat/p1-ai-film-understanding

Includes:

- primary source-film concept;
- bounded visual structure analysis;
- audio extraction and transcription;
- visual + transcript/narrative composition;
- FilmUnderstandingPort;
- strict Astra structured output;
- recurring entities and narrative cues;
- AI-discovered candidate continuity anchors;
- Accept / Edit / Reject;
- confirmed continuity memory;
- Reference Bible promotion/provenance;
- cross-modal continuity concerns/evidence;
- durable FilmUnderstandingRun;
- real backend processing stages and frontend stage presentation;
- reload/recovery and failure visibility;
- no fake progress.

This branch absorbs the essential scope previously planned as
feat/p1-analysis-progress (historical/superseded; no separate branch).
General UX polish remains separate. Polling is sufficient if it is the smallest
reliable implementation; SSE is optional. Backend state is authoritative.

----------------------------------------------------------------------
P1 — UX polish

feat/p1-ux-polish

Includes:

- loading states;
- empty states;
- error states;
- responsive behavior;
- keyboard/accessibility improvements;
- visual consistency;
- workspace polish.

Do not mix major new functionality into this branch.

----------------------------------------------------------------------
P2 — Product Hunt launch polish

feat/p2-product-hunt-polish

Includes only optional final refinements such as:

- micro-interactions;
- final landing-page polish;
- demo presentation refinements;
- launch screenshots support;
- final Product Hunt-specific details.
- must respect Law 25 (Québec), GDPR

This branch must never delay a shippable release.

----------------------------------------------------------------------
BUG FIXES
----------------------------------------------------------------------

Create focused bug-fix branches when a fix should be reviewed independently.

Examples:

fix/ffmpeg-scene-detection
fix/astra-schema-validation
fix/timeline-frame-order
fix/demo-reset
fix/upload-cleanup

If a bug is discovered while its feature branch is still open and the fix
belongs exclusively to that work, fix it on the existing feature branch.

Do not create unnecessary branches.

----------------------------------------------------------------------
DOCUMENTATION BRANCHES
----------------------------------------------------------------------

Documentation required by a feature should normally travel WITH that feature
branch.

Do not create a separate docs branch merely to update documentation required by
the Definition of Done.

Use docs/* only for standalone documentation work.

Examples:

docs/product-hunt-launch
docs/demo-recording-guide

----------------------------------------------------------------------
PR / REVIEW BOUNDARIES
----------------------------------------------------------------------

Each planned feature branch should produce a coherent review unit.

Before requesting review:

1. implementation is complete for the branch scope;
2. relevant tests pass;
3. backend/frontend builds pass as applicable;
4. documentation is synchronized;
5. STATUS.md is updated;
6. IMPLEMENTATION-PLAN.md is updated;
7. new architectural decisions have ADRs;
8. known limitations are documented.

Provide a concise review summary containing:

- branch name;
- objective;
- major files/components changed;
- architecture decisions;
- tests executed;
- manual verification performed;
- known limitations;
- screenshots for meaningful UX changes when useful.

Do not bury unrelated refactors in a feature PR.

----------------------------------------------------------------------
MERGE ORDER
----------------------------------------------------------------------

Expected initial merge order:

1. feat/p0-foundation
2. feat/p0-media-ingestion
3. feat/p0-astra-analysis
4. feat/p0-findings-workspace
5. feat/p0-intentional-change-steering
6. feat/p1-reference-bible
7. feat/p1-guided-tour
8. feat/p1-demo-project — merged
9. feat/p1-ai-film-understanding
10. feat/p1-ux-polish
11. feat/p2-product-hunt-polish — optional

This order is a plan, not a prison.

Patrick's September 14 plan evolved after PR7 real-film validation showed two
product needs: AI work was too invisible / under the hood, and continuity
understanding needs visual + narrative/audio context, not isolated image comparison
alone. PR8 owns Film Understanding and its essential truthful processing stages;
PR9 owns general UX polish. Other historical context remains preserved.

September 14 final PR8 checkpoint: successful Film Intelligence provider processing
is recovered on the historical 92.458667-second master, not the 36.291667-second
derivative. Derived generation/new-reset behavior is deterministically verified.
PR8 awaits merge review; next milestone is PR9 UX polish, not started. Refresh
flashing, active-stage presentation, result/evidence density, master-vs-analysis
clarity and missing project deletion are PR9 follow-up, never fake progress.
Exact evidence and qualifications: docs/PR8-FINAL-VALIDATION.md.

Branches may be reordered when dependencies or delivery risk justify it.

If the merge order changes materially, update DEVELOPMENT-PLAN.md and
IMPLEMENTATION-PLAN.md.

----------------------------------------------------------------------
NO LONG-LIVED DEVELOP BRANCH
----------------------------------------------------------------------

Do not introduce a permanent `develop` branch for this challenge.

For a five-day sprint:

feature branch
↓
review
↓
main

is sufficient.

Avoid GitFlow ceremony.

----------------------------------------------------------------------
SESSION CONTINUITY
----------------------------------------------------------------------

STATUS.md must contain:

Current branch:
Current milestone:
Current task:

When resuming a session, Laurie Codex must inspect the actual Git branch and
git status before continuing.

Never assume the branch from chat context.

If the current branch does not correspond to the documented current task,
reconcile the discrepancy before making substantial changes.

----------------------------------------------------------------------
BRANCH CREATION RULE
----------------------------------------------------------------------

Before starting the next planned implementation slice:

1. ensure the previous work is in a coherent state;
2. ensure main contains the expected prerequisite work;
3. create the appropriate planned feature branch;
4. update STATUS.md with the current branch and task;
5. implement the slice.

Do not create all planned branches in advance.

Create them when their work begins.
