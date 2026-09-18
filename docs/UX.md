# UX

## Public disclosures

The shared footer provides Privacy, Terms and GitHub links without changing the
workspace layout. `/privacy` and `/terms` use the existing narrow page style,
semantic headings/main, a last-updated date and responsive wrapping. Each upload
form has one small rights/AI-processing notice; its privacy link opens a new tab
to preserve the current upload draft. No cookie banner: inspected browser storage
is functional, and no application tracking was found. Public copy has no TODOs;
production builds fail on missing operator identity/email or placeholder values.
Development-only missing configuration is labelled as a local instance. Ordinary
deletion, retryable media cleanup and retained demo history are distinct. No invented
contact details or compliance claims. See PRIVACY-DISCLOSURE-REVIEW.

## PR9 UX polish and project lifecycle

### Final clarity addendum — September 15

Quick tour remains four steps but starts with Film Intelligence: understand the
whole film through sampled visuals/transcript context, then inspect and let the
creator decide. It names recurring entities, narrative context, candidate anchors
and potential concerns without treating proposals as rules or questions as errors.
Subsequent steps cover the Reference Bible, media/timeline and Findings/evidence
with the existing creator actions. Each step reveals its appropriate mounted tab;
Back returns to Film Intelligence, and focus/Skip/Escape/preferences are preserved.

Source name, actual analysis-source duration and fixed-source state are visible.
Source details retains identity/master/derived provenance and explicitly explains
that replacement is unavailable: create a new project for another film. This
matches the existing primary-source constraint; no upload/replacement API changed.

Advanced inspection is collapsed by default. It contains the existing range rail,
In/Out handles/buttons and selected duration. Frame/time and keyboard shortcuts
remain visible outside it. I/O opens the disclosure when setting a valid mark;
collapsing does not discard marks. Ranges remain local navigation state only.
The repeated Film Intelligence introduction is removed; concerns offer supporting
evidence before detailed rationale. All original evidence/model wording remains.

### Explanatory roadmap only — after PR10 unless reprioritized

The final PR9 [architecture gate](ANALYZE-SELECTION-INVESTIGATION.md) defers Analyze
Selection: reusable continuity analysis still needs an immutable source-range
contract, bounded transcript policy and explicit re-evaluation scope. Advanced
inspection remains local inspection/navigation only; I/O never starts analysis.

Small native disclosures keep planned capabilities subordinate to current work:
- **Analyze Selection:** inside Advanced inspection; In/Out → explicit paid consent
  and analysis request → bounded visual/transcript evidence → result persisted with
  immutable range provenance. Not an active or disabled CTA.
- **Source Monitor ↔ Evidence Frame:** Findings' Why this exists explains today's
  concern-to-exact-evidence connection. Future video-assisted inspection would place
  source video at the finding timestamp beside the cited frame, allow jumping
  between context/evidence, then creator review/correction. No playback is implemented.
- **Fix Assist:** Finding → correction proposal → creator review → export / optional
  generation. Current copy-correction remains real; export/generation are planned
  only and require separately scoped decisions. No automatic correction or canon.

Current principle: Understand → Inspect → Creator decides. Future: Focus → Correct.
Difference is not a continuity error; none of this help starts analysis or changes
provider scope, request inputs, models, security architecture or creator authority.

Film Intelligence is the default primary workspace tab: understand visual/narrative
context, then confirm what should stay consistent. Continuity Findings groups the
Reference Bible, shot import, frame inspector/timeline and saved continuity review.
Its purpose and empty state distinguish saved findings from discovery proposals;
Run continuity analysis starts the existing API operation from this tab.
Import a shot is for precise frame-level inspection/focused visual comparison, not a
prerequisite for Film Intelligence. Both tab panels stay mounted: polling, drafts,
evidence selection and local marks survive tab changes. Arrow keys on the tab list
move focus/activate tabs; Tab enters visible content. Linked findings/analyses open
their review tab; film links open discovery. Tour steps reveal their Film Intelligence
or Findings targets without selecting a frame or discarding a draft.

Film Understanding shows five persistent workflow tracks from actual backend stages:
source verification, visual structure, audio transcription, film understanding and
candidate saving. Step X of Y is workflow position, never a completion percentage.
Recorded timestamps determine completion; failure marks the last real stage failed,
even when its timestamp is closed. Text and symbols accompany color. Only the active
stage animates; reduced-motion disables that animation. Started/stage-update times
and elapsed seconds remain factual. No ETA is claimed without timing evidence.

GET polling remains 1.5 seconds while active/reconciling. A reserved compact live
line says Syncing or Live/updated age; Refresh now has separate manual loading state.
Terminal states stop polling and elapsed updates. Read errors preserve saved state.
Transcript/evidence/candidates appear only from persisted data; no thought simulation.

Understanding summary retains model wording and separates observed arc, recurring
elements, narrative titles and compact statistics. Potential concerns precede
candidate anchors; concise cards expose uncertainty/rationale on demand. Evidence
uses small source-time thumbnails with full-size modal inspection and transcript
provenance. Recurring entity/narrative details, original warnings and Methodology &
limitations remain discoverable. No concern becomes a Finding or confirmed rule.

The timeline is focusable and supports Left/Right sampled frames, Shift+Left/Right
shot/analysis segments, Home/End, I/O and Escape. Inputs/editors preserve native keys.
The selected frame/playhead and source time are visible; imported clips lacking a
source mapping say Clip, never invented whole-film time. Playback is unavailable,
so Space keeps normal browser/button behavior. Mouse/touch selection remains.
Selection changes and revealing the inspection tab check the active thumbnail
against the internal timeline viewport. Already-visible frames do not scroll;
out-of-view frames move only the minimum offset, without scrolling the page.
Instant movement avoids animation queues on rapid Arrow/Shift+Arrow input and
also respects reduced motion. Unrelated/polling renders do not reset scroll.

I/O or explicit buttons set/adjust local inspection marks on sampled frames. Handles
navigate back to each mark; a bordered highlight and text show In, Out and duration.
Reversed ranges are rejected with an explanation. Each range belongs to the actual
analysis source or individual imported clip; cross-clip source provenance is never
invented. Marks do not change paid request inputs, and reset on component remount.

Analysis source displays the current persisted duration. Source details uses the
packaged manifest only when SHA-256 matches, showing master/range or historical
master identity. A generic upload gets no invented master provenance.

Delete project is a secondary danger action for ordinary projects. Confirmation
names the project, warns of irreversible history/media removal and requires typing
its exact name. Keep project receives initial focus; Escape/cancel restores focus.
Pending submission cannot duplicate/dismiss. Active analysis and demos refuse
deletion. Success returns to the library; cleanup errors stay retryable in the dialog.
Current/retired demos retain the existing Reset replacement policy.

Responsive progress stacks at narrow widths, evidence retains contain sizing, and
focus outlines/visible labels support keyboard inspection. Final screenshot/check
results are recorded in STATUS and PR9-REVIEW; implementation is not a test claim.

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
remain truthful. Run continuity analysis is the primary action in Continuity Findings,
enabled after at least one READY imported shot. Its synchronous request announces
Analyzing continuity… and prevents duplicate clicks. Request errors display the API
message/status; explicit retry reuses the UUID, never automatically retries. A returned
run selects analysisId and clears finding/page/frame selection before saved findings
load. Reload saved findings is secondary and only reads persisted results.
Successful linked runs announce Analysis complete; a successful empty result says
No continuity issues were found. A new project without shots says no continuity
analysis has been run yet and shows the CTA with the import prerequisite. For an
unfiltered empty history with shots, the existing API cannot enumerate runs: explain
how to start a first analysis or reopen a saved analysis link without inferring
that an older zero-finding analysis never happened.
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
The four concise steps cover Film Intelligence, Reference Bible, media/timeline,
and findings/evidence with resolve/steer. Findings steps use the stable panel even when empty, describe
actions as appearing on an open finding, and never manufacture findings or controls.
Editing the Bible does not start analysis; creator intent does not force agreement.

Mantine supplies modal semantics, labelled heading/body, focus trapping and Escape.
Mantine alone owns opening autofocus. Navigation between already-open steps focuses
the updated heading without scheduling a later callback that could steal Tab focus.
Every step focuses its heading (“Step N of 4” plus title), then Tab reaches Skip,
Back when applicable, and Next/Done. Closing restores the initiating Quick tour
button; if the invitation's Start control disappeared, Quick tour is the fallback.
Escape is equivalent to Skip. Clicking the backdrop does not dismiss accidentally.
The focus outline and textual step labels avoid reliance on color. Background
controls cannot receive tour keyboard navigation or pointer actions.

The card sits at the bottom right on desktop/tablet and spans the available width
on mobile, with bounded height and internal scrolling. Explicit `data-tour` targets
mark stable Film Intelligence, Reference Bible, media and Findings containers. Active panels have an
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

## Reported PR9 follow-up — not implemented in PR8

- Polling/manual refresh flashes and feels like a page refresh.
- No polished visible active-run progress bar; loading/current-stage hierarchy needs improvement.
- Results are clogged; summary, warnings, concerns and evidence compete for attention.
- Evidence cards take too much vertical space.
- Clarify historical master (92.46 s) versus new/reset analysis source (36.29 s), without relabelling old evidence.
- Project deletion is missing; define history/media retention before implementing it.

PR9 may improve stage presentation, never invent percentages or model activity.
These are Patrick's reported UX observations, not new finalization screenshot tests.
