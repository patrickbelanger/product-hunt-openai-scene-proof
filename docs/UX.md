# UX

## Implemented

Dark, restrained visual workspace. The header shows SceneProof and local workspace
status. Landing explains the continuity workflow with a simple geometric illustration,
offers Create a project and lists saved projects. No external imagery or fake findings.

Creation asks for name, description and textual rules. Blank-name validation is
immediate; submit shows pending state and prevents duplicate submission. Failure
retains entered fields for retry. Success opens the persisted project workspace.

Workspace: Reference Bible/rules on the left, import and frame inspector centrally,
findings on the right, scrolling timeline below the inspector. Import takes one
still or short video and shows real pending work. Errors retain the selected file
for retry; completed failures remain visible after reload. Timeline buttons select
actual PNG frames, showing video elapsed seconds or "Still image". Empty states
remain truthful; no Analyze/Demo controls imply unimplemented behavior.
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

## Planned

Demo entry opens a populated film without upload. Tour: Reference Bible → timeline
→ findings → resolve/steer, always skippable and restartable.
Intentional change asks for an explanation and reflects real
re-analysis state. Progress stages must originate in backend work; no fake percentages.

Before launch verify normal laptop/tablet, keyboard-only flow, empty/loading/error
states, readable evidence images, contrast and an under-one-minute demo journey.
