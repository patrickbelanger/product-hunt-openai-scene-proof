# PR9 final review — UX Polish + Project Lifecycle Usability

Branch: `feat/p1-ux-polish`.
Base: `ba7076fef8a2b70083ca434214a4c92d08ba0dc7` (merged PR8).
Recovered clean pushed checkpoint: `b5334f00be63ebc4f360299a18974d4ca6738b12`.
Exact final HEAD is reported in the Git handoff; this document cannot contain its
own commit hash. No existing PR9 work was discarded or reimplemented.

## Delivered

- Five real backend workflow tracks with Step X of Y, completed/current/future and
  exact failure states. Saved timestamps drive elapsed time; no percentage or ETA.
  Active-only animation respects reduced motion. Quiet polling/live age does not
  animate the separate Refresh now control; terminal runs stop polling.
- Film Intelligence is the default hero tab; Continuity Findings contains the
  Reference Bible, import, inspector/timeline and saved review. Both panels remain
  mounted, preserving polling, drafts, selected evidence/frame and local In/Out.
  Arrow-key tab navigation, visible focus and existing guided-tour targeting work.
  Deep links reveal their workflow; clearing a findings filter stays in Findings.
- Concise workflow/import purpose copy explains why precise shot inspection remains
  available beside whole-film discovery. Empty findings explain how persisted
  concerns arise (continuity analysis currently starts through the API), evidence
  comparison and existing resolve/dismiss/re-evaluation actions.
- Summary, compact key statistics, concern/candidate hierarchy and expandable
  evidence/methodology keep model wording, uncertainty and creator authority intact.
  Concern != confirmed error; candidate != rule; transcript != visual proof.
- Timeline Left/Right frames, Shift+Left/Right segments, Home/End, I/O and Escape.
  Text inputs retain native keys; no playback, so Space retains native behavior.
  Playhead and source/clip time remain visible. Selection-driven scrolling checks
  the internal viewport: no scroll if visible, otherwise minimum instant offset.
  Polling/unrelated renders do not reset scroll; tab reveal rechecks visibility.
- In/Out handles, selected range and duration remain local inspection/navigation
  state, never provider inputs. Switching tabs preserves them; reload/remount does not.
- Current actual source duration and hash-matched packaged master provenance:
  derivative 36.291667s, master 92.458667s. Historical PR8 live validation used the
  master and is unchanged. Generic uploads get no invented master provenance.
- Secondary project deletion with exact-name irreversible confirmation, cancel/focus
  recovery and library navigation. Ordinary-project owned runtime rows/history/media
  are cleaned coherently; demo and active-run deletion are refused. Packaged assets
  and unrelated files are excluded. Pending media cleanup can retry the same UUID.
- PR10 attack-surface/threat inventory in `PR10-SECURITY-PLAN.md`; preparation only.
  ADR-0010 records the narrow deletion extension. No other backend domain expansion.

## Verification

- Final frontend: 101 tests across 12 files passed. Focused runs: 54 affected tests,
  then 29 final tab/timeline/tour regressions, including filter-clear tab retention.
- TypeScript and Vite production build passed. Entry JS 539.29 kB versus 531.50 kB
  checkpoint (+1.47%); existing >500 kB warning remains, no broad bundle refactor.
- Final full Chromium suite: 20/20 passed (2.2 minutes).
- Backend/API unchanged in this final slice. Checkpoint full Gradle build/127 tests
  and OpenAPI drift remain valid prior evidence, not newly rerun verification.
- Earlier browser failures: the legacy media test needed Findings navigation after
  reload; corrected without weakening visibility assertions. Another full run hit a
  local reference-image `net::ERR_CONNECTION_TIMED_OUT`; network trace inspected,
  no production fix, timeout increase or test retry policy added.

## Visual / accessibility review

Captures at 1280/820/390:
`test-results/pr9-{active,results,concern,evidence,playhead,range,delete,findings-empty}-{width}.png`.
Populated findings: `findings-{desktop,tablet,mobile}.png`; tour and demo captures
also cover all three layouts. These are ignored local artifacts, reproducible with
the committed E2E tests. All model outputs in browser fixtures are deterministic
test-only content, not new live AI validation.

Reviewed tabs, progress/summary hierarchy, concern/evidence inspection, playhead and
range controls, empty/populated findings, source labels and destructive dialogs.
No horizontal page overflow in tested layouts; images retain aspect ratio, text
wraps, tabs remain usable and focus is visible. E2E exercises keyboard tab activation,
tour focus/return, retained selection/range, end/start auto-scroll and deletion
confirmation/cancel/success. Unit tests cover no scroll for visible frames, minimum
offsets, unrelated rerenders, input shortcut suppression and progression semantics.
This is Chromium/responsive review, not screen-reader or physical-device certification.

## Reviewer focus / known limits

Review V10 history deletion exceptions, safe ownership/path boundaries, cleanup
recovery and local-filesystem assumptions. DB/filesystem cleanup is not one atomic
transaction; interrupted cleanup requires retry, and small UUID tombstones remain.
Demo copies use Reset, not Delete. No public tenancy/auth claim.

Sampled inspection has no playback or durable range persistence. Dense rail ticks
are supplemented by keyboard navigation and separate thumbnail buttons. Findings
analysis still starts through the API. No ETA, Analyze Selection, generation/Fix
Assist, provider-model/orchestration change, PR10 implementation or merge occurred.
PR10 Security Hardening (HIGH) is next; Analyze Selection follows PR10 unless
separately reprioritized. Zero external provider calls in PR9.

Docs synchronized for this slice: STATUS, PR9-REVIEW, UX, PRODUCT and both plans.
Earlier PR9 updates to ARCHITECTURE, DEMO, DECISIONS, README, ADR index/ADR-0010,
PR10-SECURITY-PLAN and `intial-context.md` remain; the intentional filename is preserved.

Recommendation: ready for PR9 review/merge consideration, subject to reviewer/CI
approval. Do not merge automatically or begin PR10.
