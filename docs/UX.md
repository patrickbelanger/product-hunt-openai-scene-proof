# UX

## Implemented

Dark, restrained visual workspace. The header shows SceneProof and local workspace
status. Landing explains the continuity workflow with a simple geometric illustration,
offers Try the demo film and Create a project and lists saved projects. The demo uses
the approved original film; no external stock imagery or fabricated findings.

Creation asks for name, description and textual rules. Blank-name validation is
immediate; submit shows pending state and prevents duplicate submission. Failure
retains entered fields for retry. Success opens the persisted project workspace.

Workspace: Reference Bible/rules on the left, import and frame inspector centrally,
findings on the right, scrolling timeline below the inspector. Import takes one
still or short video and shows real pending work. Errors retain the selected file
for retry; completed failures remain visible after reload. Timeline buttons select
actual PNG frames, showing video elapsed seconds or "Still image". Empty states
remain truthful; sequence analysis remains explicitly API-started.
Project names and rules wrap, including long unbroken content.

Loading is announced through status roles; errors offer retry/navigation. Forms have
labels, keyboard focus is visible, and a skip link targets main content. At tablet
width findings move below the inspector; at small width panels stack. The library
uses responsive cards and pagination instead of an unbounded list.

## Findings inspection (PR3)

The findings panel reads persisted PR2 results, twenty per page, newest analysis
first. Selection scopes the URL to that analysis and finding, preserving it through
reload and later analyses. All saved findings returns to history. Historical runs
are not combined into a claim about the latest project state. Without a linked
analysis ID, an empty list means only no saved findings; PR2 cannot enumerate runs.
A linked run distinguishes SUCCEEDED with no findings, RUNNING and FAILED. Read
errors remain errors, with explicit refresh/retry; no read initiates analysis.

Finding details show category, LOW/MEDIUM/HIGH severity, model-reported confidence,
title, summary, expected state, observed state, explanation and full correction.
Severity has text as well as color. Confidence is explicitly not a guarantee.
Copy correction awaits clipboard.writeText, announces actual success or failure,
and retains selectable prompt text. Enter/Space and visible focus work on buttons.

Affected shots have subtle page-scoped markers and a distinct selected-finding
highlight. Compact affected-shot buttons keep their identities visible even in
long timelines; navigation scrolls to the shot and focuses its actual evidence
frame control. View evidence comparison focuses the viewer heading, useful when
findings sit below the viewer on tablets. Other frame selection temporarily opens
the normal inspector; Back to finding evidence restores the comparison.

Evidence maps only relevantFrameIds belonging to persisted affected shots, sorted
by shot position, frame position and ID. The first is anchored left; the right
defaults to the first evidence from another shot, or the next frame of the same
shot. Additional evidence buttons replace the right side. A reload restores this
deterministic pair, not the temporary alternate frame. One image remains useful;
missing metadata and image-load failures are announced without substituting media.
Images use contain, with no crop, recoloring or overlays; captions identify shot,
frame and actual timestamp (or Still image). Import collapses during inspection.

Verified at 1280/1440 desktop, 820 tablet and 390 mobile: no horizontal overflow.
Findings move below the viewer at widths up to 1100px; panels stack below 700px.
Critical Chromium verifies persisted evidence pixels, keyboard selection/copy,
clipboard contents/failure, reload, GET-only UI actions and responsive widths.

## Creator context and history (PR4)

An OPEN finding offers This change is intentional, Resolve and Dismiss. Intent
opens a compact form requiring explanation and narrative scope across exactly the
affected shots. Confirm intent & re-evaluate explicitly authorizes one targeted
paid request. The UI explains that Astra may accept, disagree or remain uncertain.
Resolve/dismiss require an audit note and clearly state that no Astra call occurs.

Pending work shows actual re-evaluation text, with no percentage or optimistic
status. Session storage saves the request UUID/payload before POST. Connection loss
preserves the original inspection and offers Recover same request; it replays the
same UUID, preventing duplicate inference. A saved FAILED attempt stays failed on
replay. A fresh form/confirmation creates a separate paid attempt. On reload, GET
history restores running/failed/completed state; selection and refresh never infer.

The latest successful model judgement has its own summary, explanation, evaluated
scope and remaining concern/correction. The original assessment is explicitly
identified underneath. All saved findings remain selectable with OPEN / INTENTIONAL /
RESOLVED / DISMISSED labels. Counts and timeline issue markers include only OPEN
findings on the page; they do not claim a latest whole-project assessment. Original
evidence selection remains available even for a historical/non-open finding.

A compact View immutable history disclosure shows original analysis identity,
creator explanations/scopes, timestamps, action UUIDs, actual failures and the
preceding judgement link. New model judgement replaces only the effective assessment,
never the historical record. Terminal creator actions explain their distinct meaning.
Refresh history is GET-only. PR4 added no reference editor, demo, tour or hosting.

## Reference Bible and cited visual truth (PR5)

The left panel has a rules textarea and explicit Save rules, with real saving/saved/
error states. Failure preserves the draft. Rules load with the project and may be
empty. Project-load errors retain the existing retry/navigation flow.

Visual references show normalized thumbnails, title and guidance. Add opens a compact
modal with JPEG/PNG selection, title, optional guidance and Save/Cancel. No fake
progress. Upload/validation disables duplicate actions and premature dismissal;
failure retains file/text. Success displays the actual normalized backend image.
Eight active images disable addition; list failure has its own retry and leaves
rules/findings usable. No reference edit initiates inference.

Inspect opens the preserved image and editable metadata, plus archive confirmation.
Archive permanently removes it from the active Bible and retains historical evidence.
Replace an image by archive then new upload. Keyboard activation, trapped/restored
modal focus and responsive stacking remain available.

Cited findings display a separate Reference evidence block; shot A/B remains intact.
Original title/guidance/image are shown, with archived status explaining absence from
the current Bible. Zero citations produce no block. Incomplete historical data and
image failure are explicit and never substituted with current references.

## Planned later slices

Demo entry is implemented in PR7 and opens a populated film without upload.
PR8 implements real Film Understanding stages below; generalized UX remains PR9.

Before launch verify normal laptop/tablet, keyboard-only flow, empty/loading/error
states, readable evidence images, contrast and an under-one-minute demo journey.

## Populated demo (PR7)

Try the demo film is a primary landing button alongside Create a project. Real
preparation has an announced pending state; repeated clicks cannot launch duplicate
work. Session storage records the request before POST, preserving retry after network
loss/reload. Storage failure sends no request. Server failure remains actionable.

The demo's import panel starts collapsed so the film comes first; users can open it
to add footage. Launch/reset focuses the populated workspace heading for keyboard
orientation without starting the tour.
The workspace displays Demo project from typed identity and a compact description
with film attribution. Five real Bible images and eight video imports are immediately
inspectable once preparation completes. Findings start empty and are never invented.
Optional first-workspace tour invitation remains independent and does not auto-open.

Reset demo appears only on current demo copies. Its modal explains removal of edits
from the working copy and retention of history with the old copy. Keep my changes is
the initial focus; Escape cancels before submission. During reset, dismissal and
duplicate submission are disabled; success navigates to the ready new project.
Failure keeps the current workspace and allows retry for the same source project.
Cancel restores opener focus through Mantine. Old URLs show Previous demo copy and
no reset control. No fake React project/media DTOs are used in runtime paths.

## Optional guided tour (PR6, unchanged)

The first successfully opened workspace offers a compact “Take the quick tour”
invitation with Start tour / Skip. There is no automatic modal or focus movement.
Quick tour stays in the workspace toolbar, including after completion or Skip.
The four concise steps cover Reference Bible, media/timeline, findings/evidence,
and resolve/steer. Findings steps use the stable panel even when empty, describe
actions as appearing on an open finding, and never manufacture findings or controls.
Editing the Bible does not start analysis; creator intent does not force agreement.

Mantine supplies modal semantics, labelled heading/body, focus trapping and Escape.
Every step focuses its heading (“Step N of 4” plus title), then Tab reaches Skip,
Back when applicable, and Next/Done. Closing restores the initiating Quick tour
button; if the invitation's Start control disappeared, Quick tour is the fallback.
Escape is equivalent to Skip. Clicking the backdrop does not dismiss accidentally.
The focus outline and textual step labels avoid reliance on color. Background
controls cannot receive tour keyboard navigation or pointer actions.

The card sits at the bottom right on desktop/tablet and spans the available width
on mobile, with bounded height and internal scrolling. Explicit `data-tour` targets
mark stable Reference Bible, media and Findings containers. Active panels have an
outline and textual step marker. A step may scroll its panel into view once; it
never selects a frame or finding, and resize/ordinary scrolling does not trigger
another scroll. Temporary bottom space lets even a short last panel clear the card;
it disappears when the tour closes. Hidden/missing targets use location text and a
truthful unavailable-area message. There are no transitions or smooth-scroll motion.

Only `sceneproof.guided-tour.v1` in localStorage is added, storing `completed` or
`skipped`, scoped to browser/origin rather than project. Closing suppresses the
invitation in the mounted workspace even if writes fail. If reading storage fails,
only manual Quick tour is offered; if saving fails, dismissal cannot survive reload.
Restart ignores the preference. Clearing storage offers onboarding again. Tour
navigation has no API dependency, backend writes, request UUIDs or provider calls.

## PR8 Film Intelligence

The workspace adds a source-film panel with bounded upload, explicit paid-action
confirmation and saved understanding history. Only backend stage records are shown:
verifying source, preparing visual structure, extracting/transcribing audio,
understanding visual/narrative context, validating/saving candidates, complete/failed.
Stage timestamps are actual recorded times, never predicted progress or private thought.

Refresh/reload recovers durable state. Connection loss retains last-known content
with an explicit stale-state error. Unacknowledged POSTs offer **Recover same film
request**; new attempts remain disabled until reconciled. A failed run stays in
history and requires fresh confirmation to try again. Browser storage write failure
prevents sending a paid request. Polling stops for terminal runs.

Dynamic summary counts reflect actual output. Entity/cue/concern disclosures show
decoded frames at source times and original approximate transcript passages.
Potential concerns are review questions, not findings. Candidate cards distinguish
proposals from confirmed memory and preserve original evidence after edits/rejection.
Accept/Edit/Reject and confirmed-frame promotion are free local operations. Normal
Reference Bible inspection links back to the originating discovery. Cross-modal
finding evidence labels transcript versus uncertain narrative interpretation.

The PR8 surface is tested at desktop/tablet/mobile widths; this does not claim the
PR9 workspace-wide accessibility/responsive sweep is complete.
