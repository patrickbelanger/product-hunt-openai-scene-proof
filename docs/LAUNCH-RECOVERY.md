# Consolidated launch recovery — September 17, 2026

## Starting state (before any mutation)

`git worktree list` reported exactly:

```text
C:/Data/Workspace/Kotlin/product-hunt-openai-scene-proof                             03b90b6 [bugfix/continuity-analysis-cta]
C:/Data/Workspace/Kotlin/product-hunt-openai-scene-proof/.local/privacy-disclosures  826bf99 [fix/privacy-terms-disclosures]
```

The main worktree was clean: `git status --short`, `git diff --stat` and
`git ls-files --others --exclude-standard` all returned no entries. Its HEAD was
`03b90b67d4b73086da1f9c1f627b29ecb8699bc0` (the existing CTA commit).

The privacy worktree HEAD was `826bf997cfbac27473fb52089c346656d58ac97c`.
It had no staged files, 12 modified tracked files (126 insertions, no deletions),
and nine untracked files. The exact modified-file diff statistics were:

```text
README.md                                  |  8 +++++++
apps/web/src/App.tsx                        |  9 ++++++++
apps/web/src/projects/FilmIntelligence.tsx   |  2 ++
apps/web/src/projects/MediaWorkspace.tsx     |  2 ++
apps/web/src/projects/ReferenceBible.tsx     |  2 ++
apps/web/src/styles.css                     | 10 +++++++++
docs/ARCHITECTURE.md                        |  9 ++++++++
docs/DECISIONS.md                           | 16 +++++++++++++
docs/IMPLEMENTATION-PLAN.md                 | 11 +++++++++
docs/PRODUCT.md                             | 11 +++++++++
docs/STATUS.md                              | 36 ++++++++++++++++++++++++++++++
docs/UX.md                                  | 10 +++++++++
```

Untracked files:

```text
apps/web/.env.example
apps/web/src/legal/LegalPages.tsx
apps/web/src/legal/UploadDisclosure.tsx
apps/web/src/legal/disclosures.ts
apps/web/src/legal/legal.test.tsx
docs/DEPLOYMENT.md
docs/PRIVACY-DISCLOSURE-REVIEW.md
playwright.legal.config.ts
tests/e2e/legal.spec.ts
```

Initial sandbox Git reads failed the ownership check; inspection succeeded in the
operator context without changing global Git configuration or repository files.

## Preservation and integration

The existing CTA commit remains intact. All 21 privacy files were committed,
unchanged, on their original branch as
`59be1d970b7949b7a5ce9783bc79e83606d80bfb`
(`feat: preserve privacy and terms launch disclosures`). Its worktree became clean.

The target branch integrated that commit using `git merge --no-ff --no-commit`.
Conflicts occurred only in DECISIONS, IMPLEMENTATION-PLAN, PRODUCT and STATUS.
Both subjects and their historical verification records were retained; PRODUCT's
stale PR9 heading was reconciled to the combined post-PR10 milestone. Current
recovery sections supersede historical stop-before-commit and incomplete-check text.
README, ARCHITECTURE and UX merged automatically, then stale API-only guidance was
corrected. No code conflict or dropped legal implementation.

The merge also clarifies analysis completion/zero-result wording, adds a new-project
empty state and adjusts affected assertions. Browser assertions verify that the CTA
is hidden in Film Intelligence, visible in Continuity Findings, and uses the existing
POST. Upload notices are checked on source-film, shot and reference forms; opening
privacy details preserves the reference dialog. Backend/API/provider code is unchanged.

No branch/worktree was created or removed. No reset, clean, checkout-overwrite,
main merge, push or production deployment was performed. The privacy branch and
`.local/privacy-disclosures` remain preserved for explicit later instructions.

## Verification

Final results are recorded in [STATUS](STATUS.md). Unit tests use API mocks. Browser
regressions use `:apps:api:browserTestServer`, Java 25, port 8086, the isolated
`sceneproof_browser` schema and an empty API key. Its test-only provider marker was
verified before running the suite. No live OpenAI/Astra/provider requests.

Final gates pass: 116 frontend tests (23 findings, seven legal/upload), typecheck,
production build, OpenAPI drift, four production-bundle legal-route tests, and the
complete isolated 26-test Chromium suite. The existing bundle-size warning remains.

Commands used for the final gates:

```powershell
npm.cmd run test --workspace @sceneproof/web -- --maxWorkers=1
npm.cmd run typecheck
npm.cmd run build
npm.cmd run api:check
npx.cmd playwright test --config playwright.legal.config.ts
$env:API_PROXY_TARGET = 'http://127.0.0.1:8086'
$env:E2E_WEB_PORT = '5176'
npx.cmd playwright test --output .local/launch-recovery-final-e2e
```

The backend unit suite was not rerun: backend source, migrations, generated client
and contract have no changes relative to merged PR10. The browser suite exercises
real API/persistence/media, security controls, Film Intelligence and continuity with
test-only providers. Full details of initial failures/reruns are retained in STATUS.

## Remaining deployment gates

The existing API has no run-list/latest-run endpoint. A linked successful empty
analysis is shown as complete, including after reload. An unfiltered empty findings
list with imported shots cannot prove whether an older zero-finding run happened;
the UI explicitly explains that limitation and offers the CTA/saved-link guidance.
No new backend semantics or inferred model results were introduced.

Resolve the operator/privacy contact and other public build-time disclosure TODOs,
review the public shared-project access boundary, clarify the truncated LICENSE,
and verify direct legal-route reloads plus API routing on the actual deployed host.
Demo reset retains old copies, media and history. Caddy remains outside the repo.
See [disclosure review](PRIVACY-DISCLOSURE-REVIEW.md) and [deployment](DEPLOYMENT.md).

## Final changed files relative to merged PR10 (`826bf99`)

```text
README.md
apps/web/.env.example
apps/web/src/App.tsx
apps/web/src/legal/LegalPages.tsx
apps/web/src/legal/UploadDisclosure.tsx
apps/web/src/legal/disclosures.ts
apps/web/src/legal/legal.test.tsx
apps/web/src/projects/FilmIntelligence.tsx
apps/web/src/projects/FindingsWorkspace.tsx
apps/web/src/projects/MediaWorkspace.tsx
apps/web/src/projects/ReferenceBible.tsx
apps/web/src/projects/findings.test.tsx
apps/web/src/projects/guided-tour.test.tsx
apps/web/src/projects/projects.test.tsx
apps/web/src/styles.css
docs/ARCHITECTURE.md
docs/DECISIONS.md
docs/DEPLOYMENT.md
docs/DEVELOPMENT-PLAN.md
docs/IMPLEMENTATION-PLAN.md
docs/LAUNCH-RECOVERY.md
docs/PRIVACY-DISCLOSURE-REVIEW.md
docs/PRODUCT.md
docs/STATUS.md
docs/UX.md
playwright.legal.config.ts
tests/e2e/findings.spec.ts
tests/e2e/guided-tour.spec.ts
tests/e2e/legal.spec.ts
tests/e2e/project.spec.ts
tests/e2e/references.spec.ts
tests/e2e/steering.spec.ts
tests/e2e/ux-polish.spec.ts
```
