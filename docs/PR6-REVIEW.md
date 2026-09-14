# PR6 review handoff — 2026-09-13

Historical handoff: PR6 subsequently merged through GitHub PR #6 at
`1e3f28fb162bcdc9d608c40d61d7fcfbcad97909`. PR7 recovery verified that exact main
and the PR6 implementation parent `8923cea`. Review/stop instructions below describe
the original handoff; Patrick has now authorized PR7.

## Branch and recovery

- Branch: `feat/p1-guided-tour`.
- Exact base/main: `3184af6cf0119be1fa2949ca1fb60a2584483a56`.
- Fetched origin, verified PR #5 merge (parents `0901f4b` / `5071eb4`), clean worktree,
  fast-forwarded local main and verified main equals origin/main at the requested SHA.
  Created only the authorized PR6 branch from that commit.
- Read recovery sections 34–44, required product/technical docs, applicable ADRs,
  actual workspace components, tests and installed Mantine modal interfaces.
- PR5 pending-review wording is explicitly historical in STATUS and PR5-REVIEW;
  delivery and implementation plans now record the merge. Original context is intact.
- Implementation and this handoff travel together in the PR6 commit. The final
  handoff reports its exact HEAD. Review required; do not merge or start PR7.

## Behavior and scope

The first successfully opened workspace offers a small “Take the quick tour”
invitation with Start tour and Skip. Nothing automatically opens or takes focus.
Quick tour stays in the workspace toolbar and always starts at step one.

Exactly four conceptual steps:

| Step | Stable target | What it explains |
| --- | --- | --- |
| 1 — Reference Bible | `data-tour="reference-bible"` | Continuity rules and visual references declare truth; editing never starts analysis. |
| 2 — Media and timeline | `data-tour="media"` | Imported stills/video supply inspectable ordered shots and representative frame evidence. |
| 3 — Findings and evidence | `data-tour="findings"` | Saved concerns after analysis, evidence comparison and actionable correction; difference alone is not error. |
| 4 — Resolve and steer | Same stable Findings panel | Open-finding Resolve, Dismiss and intentional-change targeted review; creator context does not force Astra agreement. |

Empty findings work through all four steps. The tour does not manufacture data or
action controls, open an action form, select a finding/frame, change URL selection,
save a draft, import media, allocate request UUIDs or start/replay analysis. The
real OPEN finding actions stay available after closing the tour.

Persistence is only localStorage key `sceneproof.guided-tour.v1`, containing the
bounded string `completed` or `skipped`. Done stores completed; invitation Skip,
tour Skip and Escape store skipped. Completed/skipped suppress later invitations
across reloads and projects on the same origin. Restart ignores the saved value.
Unknown values invite again. No project identifier, API DTO, backend field or
migration is involved. Reading storage failure suppresses automatic invitation;
manual Quick tour still works. Failed writes dismiss in the mounted workspace.

## Implementation, accessibility and layout

`GuidedTour.tsx` owns four fixed copy entries, local preference and navigation state,
target highlighting, one navigation scroll and focus restoration. Workspace adds
the control; ReferenceBible, MediaWorkspace and FindingsWorkspace add one explicit
semantic marker each. No third-party dependency or general onboarding framework.

Mantine's compound Modal supplies a correctly labelled modal dialog/body, Escape
and focus trap. Its actual h2 title receives initial focus and focus after every
Next/Back, includes “Step N of 4”, and has a visible outline. Tab reaches Skip,
Back when applicable and Next/Done; Shift+Tab wraps inside the dialog. Background
controls receive neither tour-navigation focus nor pointer actions. Closing restores
the original Quick tour button, or falls back to Quick tour when Start was removed
with the invitation. Backdrop clicks do not accidentally dismiss. No hand-written
focus trap or automatic focus on invitation.

Card placement is bottom right on desktop/tablet, available width on mobile, with
viewport-bounded height and internal overflow. Panel outlines and textual step
markers identify the area independently of color. A target outside the upper
visible area is scrolled once on step navigation; ordinary scroll and resize do
not repeatedly scroll. Temporary tour-only bottom space lets short final panels
clear the card. It disappears on close. Missing/hidden targets retain location
text and explain unavailability without selecting other data or pointing elsewhere.
Transitions are disabled and scrolling is instant, including reduced-motion mode.

## Verification

| Check | Final result |
| --- | --- |
| Full Vitest/RTL suite | PASS — 59 tests, including 15 tour cases |
| Production TypeScript/Vite build | PASS on final source |
| Complete Chromium critical suite | PASS — 15 tests, including 5 tour flows |
| Gradle build | PASS; unchanged backend outputs reused |
| Fresh backend regression run | PASS — 71 tests, 0 failures/errors/skips (`:apps:api:test --rerun`) |
| OpenAPI generation drift | PASS; contract/generated client unchanged |
| Git whitespace/scope | PASS; no backend, API-client or lockfile changes |
| Real OpenAI/Astra calls | **0** |
| Remote CI | Not checked; local checks above are the verification evidence |

Frontend tests reset localStorage independently and cover invitation, start,
completion/reload, skip at every step, restart, exact step order and boundaries,
keyboard/focus/Escape, storage read/write failure, missing/hidden target fallback,
highlight cleanup, no scroll loop, unchanged unsaved rules, no project mutations,
no fetch/provider invocation and no request UUID creation.

Browser tests use real persisted projects on the existing deterministic test-only
API. They verify the complete keyboard flow at 1280, 820 and 390px, focus trapping
and restoration, Escape, restart after reload/completion/skip, draft preservation,
readable visible target placement, no horizontal overflow, reduced motion, storage
getter failure, hidden anchor fallback at 390×640, and long project/reference text.
A populated fixture verifies selected real shot evidence and URL selection remain
unchanged and actual PR4 actions remain available. Test fixture setup alone invokes
the deterministic analysis port; tour-time API writes are intercepted, recorded and
blocked with **zero attempted writes**. No paid provider or fixture semantics changed.
Existing PR1–PR5 browser flows pass unchanged.

Screenshots captured for all four steps at each tested width under
`test-results/tour-{1280,820,390}-step-{1,2,3,4}.png` (ignored local artifacts).
Desktop step 1, tablet step 3 and mobile step 4 were visually inspected.
Test API used port 8096 with OPENAI_API_KEY removed; final Playwright Vite used
15185 with explicit API_PROXY_TARGET and CI=true, avoiding user IDE servers.

Earlier unsuccessful attempts are not counted as passes: sandbox Git ownership,
JDK configuration and browser-cache access required escalated execution; the initial
unit run detected nested modal headings and a test removing a React-owned node;
the initial browser run caught tablet card overlap on a short last panel and an
oversized project-name fixture. The actual Mantine title, safe missing-marker test,
temporary tour scroll space and bounded fixture corrected these. Final complete
suites above passed after corrections.

## Known limits and reviewer focus

- Preference is browser/origin-local, not cross-device. Clearing it invites again.
  When writes fail, a new page may invite again; when reads fail, use Quick tour.
- Step 4 intentionally targets the stable Findings panel, not a specific finding
  or action button. Missing data never blocks the tour. Manual screen-reader/device
  testing beyond automated dialog semantics and keyboard verification was not run.
- Temporary bottom space is visible during the tour on short workspaces. It allows
  panel visibility without modifying frame selection or the normal closed layout.
- Review concise/truthful copy, optional invitation, heading/return focus, hidden
  target fallback and tablet/mobile behavior. Tour interaction has no API dependency.
- No backend/provider/budget/schema/prompt changes, demo, progress, upload-limit
  changes, hosting, automatic discovery or later Product Hunt polish. No ADR needed.

Updated README, STATUS, UX, PRODUCT, ARCHITECTURE, IMPLEMENTATION-PLAN,
DEVELOPMENT-PLAN and historical PR5-REVIEW. Scope ends here for review.
