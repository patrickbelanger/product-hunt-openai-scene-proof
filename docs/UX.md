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

## Planned

Demo entry opens a populated film without upload. Tour: Reference Bible → timeline
→ findings → resolve/steer, always skippable and restartable. Shot selection updates
the inspector; finding selection highlights affected shots and presents two relevant
images, expected/observed, confidence, explanation and correction. Copy reports success
or clipboard failure. Intentional change asks for an explanation and reflects real
re-analysis state. Progress stages must originate in backend work; no fake percentages.

Before launch verify normal laptop/tablet, keyboard-only flow, empty/loading/error
states, readable evidence images, contrast and an under-one-minute demo journey.
