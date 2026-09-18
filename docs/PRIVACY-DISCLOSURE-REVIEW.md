# Privacy and terms disclosure review

Date: 2026-09-17. Draft for operator review, not legal advice or a representation
of GDPR/Loi 25 compliance. Base: fetched `origin/main` at `826bf99` (merged PR10).
Branch: `fix/privacy-terms-disclosures`, isolated in `.local/privacy-disclosures`.
At final recovery the root CTA patch was already committed as `03b90b6`.
The disclosure patch was preserved unchanged as `59be1d9` on its original branch,
then integrated into `bugfix/continuity-analysis-cta`. Both histories are retained.

## Delivered user path

- Public application routes: `/privacy` (Privacy Policy), `/terms` (Terms of Use).
- Permanent Privacy / Terms / GitHub footer on library, project and legal pages.
- One contextual notice in each source-film, shot and new-reference upload form.
  Privacy details open a separate tab so the upload draft is not discarded.
- Static legal pages read no project data and initiate no provider/API requests.
- No new tracking, cookies, consent banner, authentication or lifecycle behavior.

## Verified data flows

| Behavior | Code inspected | Disclosure consequence |
| --- | --- | --- |
| Project creation/list/read | `project/ProjectController.kt`, `ProjectRepository.kt`, V1 | Names, descriptions, rules, IDs and timestamps; shared library, no per-user authorization. |
| Shot/image/reference ingestion | `media/MediaService.kt`, `reference/ReferenceService.kt`, `media/MediaStorage.kt`, V2/V5 | Selected upload bytes and sanitized filename where used; originals and normalized PNG/frames on server disk, metadata in PostgreSQL. Uploading does not invoke a provider. |
| Source film and derived audio/frames | `film/SourceFilmService.kt`, `FilmUnderstandingService.kt`, V7/V9 | Original source retained; FFmpeg extracts bounded images/audio. Temporary WAV/staging deletion attempted after normal processing; interruptions/cleanup errors may leave files. |
| Transcription | `film/OpenAiAudioTranscriptionAdapter.kt` | Extracted PCM WAV goes to `https://api.openai.com/v1/audio/transcriptions` using `gpt-4o-transcribe-diarize`. Timed transcript is stored; speaker labels are not retained as an application speaker model. |
| Film understanding | `film/AstraFilmUnderstandingAdapter.kt` | Sampled images, timed transcript, source/segment/frame identifiers and timing go to OpenAI Responses. Returned summaries, candidates, concerns and evidence are stored. |
| Sequence/targeted analysis | `analysis/AnalysisContextAssembler.kt`, `AstraContinuityAnalysisAdapter.kt`, `FindingActionService.kt` | OpenAI Responses receives selected images, reference images/guidance, project name/description/rules, available confirmed film memory; targeted review adds original finding and creator explanation/scope. |
| Provider transport and diagnostics | `analysis/OpenAiResponsesTransport.kt`, transcription adapter | Fixed external HTTPS endpoints, server-only credentials, bounded requests; Responses requests use `store=false`. This does not prove zero retention, training opt-out/account settings or a specific processing jurisdiction. No provider deletion request is wired to project deletion. |
| Ordinary deletion | `project/ProjectDeletionService.kt`, `media/MediaStorage.kt`, V10 | Exact-name confirmation; work/admission gates; DB cascade first, owned media cleanup second; explicit retryable pending cleanup. UUID/timestamp tombstones and filesystem deletion markers remain. No automatic general cleanup worker. |
| Demo reset | `demo/DemoService.kt`, V6 | New working copy, old copy retired from library but retained/readable by identifier with media and history. Ordinary deletion refuses demos. No retired-demo expiry/GC. |
| References/creator history | V4/V5/V8 and reference/action services | Archive is not deletion; immutable submitted reference snapshots and prior judgements survive edits/steering. |
| Usage protection | `analysis/PaidRunAdmission.kt`, V11 | Run UUID/time reservations survive project deletion/reset. Older-than-24h reservations pruned on a later successful admission; not a timed retention guarantee. |
| Logs/request information | `api/ApiErrors.kt`, `HttpSafetyFilter.kt`, services, `application.yml`, migrations | Safe IDs/codes/error classes and sanitized traces; no custom visitor IP/user-agent persistence found. Connection information reaches the server. No configured application access log found; edge/host logs and actual production overrides are unknown. |

Paths in this table are under `apps/api/src/main/kotlin/dev/sceneproof` and
`apps/api/src/main/resources/db/migration` unless noted. These are code findings,
not an audit of the running public service or infrastructure.

## Browser storage and tracking inspection

Inspected `apps/web/index.html`, runtime imports/dependencies, `providers.tsx`,
`GuidedTour.tsx`, `DemoControls.tsx`, `FilmIntelligence.tsx`, `FindingActions.tsx`,
`DeleteProject.tsx` and the API/filter/migrations. No analytics, ad SDK, tracking
pixel, external font request or application cookie write found. No consent banner
added; functional request recovery is not treated as advertising consent.

| Storage | Purpose and contents | Lifecycle |
| --- | --- | --- |
| `localStorage`: `sceneproof.guided-tour.v1` | `skipped` / `completed` onboarding preference | Until browser/site-data clearing or overwrite; no scheduled expiry. |
| Mantine `mantine-color-scheme-value` | Default manager reads a preference if present; app forces dark mode | No color-toggle writes in current app. Legal route tests check no new browser storage. |
| `sessionStorage`: `sceneproof.demo.instance.v1` | Demo creation/recovery UUID | Browser tab/session behavior; clearing does not delete server copies. |
| `sessionStorage`: `sceneproof.film-request.v1.<project>` | Request UUID, source UUID, paidConsent | Cleared when a matching saved run is observed or ordinary deletion succeeds; otherwise recoverable in session. |
| `sessionStorage`: `sceneproof-action:<project>:<finding>` | Unresolved action including request ID, type, explanation/scope | Cleared on terminal response; may remain after failures. Ordinary deletion does not explicitly clear these action keys. |
| React state/query cache and URL | Drafts, loaded results, evidence/range selection; run/finding IDs in URL | Functional navigation, no analytics. |

Infrastructure-injected scripts, edge cookies/logs and deployed environment settings
were not inspectable from main. No non-essential tracking was found in this repo.

## Public configuration and operator TODOs

`apps/web/.env.example` lists blank **public build-time** Vite settings. Supply
confirmed values in the frontend build environment or ignored
`apps/web/.env.production.local`, then rebuild. Do not place secrets here. The
UI renders these values as plain text and keeps explicit TODOs for missing values.

| Setting | Required operator input |
| --- | --- |
| `VITE_LEGAL_OPERATOR` | Actual service operator identity/contact details; do not infer a corporation from the repository owner. |
| `VITE_PRIVACY_CONTACT` | Responsible person's name or role, title and confirmed contact details. |
| `VITE_PRIVACY_CONTACT_EMAIL` | Confirmed operational monitored private mailbox only. Patrick supplied an unverified candidate privately; it is intentionally not committed or displayed. No SMTP infrastructure exists in the app. |
| `VITE_PRIVACY_HOSTING` | Actual host/storage/database/infrastructure recipients, locations, request logging and relevant metadata. |
| `VITE_PRIVACY_RETENTION` | Actual log/backup/demo/residual-file retention and deletion arrangements; no invented number of days. |
| `VITE_PRIVACY_TRANSFERS` | Actual OpenAI account/location/retention settings and applicable transfer arrangements; no inferred residency or zero-retention assurance. |
| `VITE_PRIVACY_LEGAL_BASIS` | Operator-reviewed applicable grounds per purpose, applicable representative details if needed. Paid-action consent is not a blanket legal conclusion. |

Missing contact details and other TODOs are visible in the draft. Do not describe
this draft as completed launch legal coverage while those facts remain unresolved.
Rights requests use the configured private channel, not public GitHub issues.

## Material discrepancies / DECISION REQUIRED

**Current choice:** main is the PR10 shared, unauthenticated workspace; Patrick
reports a public deployment. The external edge proxies routes but its access/logging
controls are not in the repo. **Problem:** application clients can read/modify other
projects; a disclosure page cannot create confidentiality. **Proposed alternative:**
confirm and document the actual exposure/access boundary and keep confidential or
personal uploads out of the public MVP until that boundary is acceptable.
**Alternatives:** operator-restricted access, public demo-only scope, or separately
approved isolation work. **Recommendation:** Patrick reviews and resolves this as a
launch gate; do not imply private accounts. **Implementation impact:** this patch
only discloses the verified limitation and preserves all behavior; no auth/redesign.
**September 18 impact:** operator acceptance/access decision is needed before launch
with user personal/confidential media; this disclosure patch cannot clear that gate.

Also requiring operator resolution:

- No confirmed operational privacy mailbox, legal operator/role, host geography,
  backup/log retention or provider account/transfer settings can be inferred from main.
- `LICENSE` begins mid-sentence in the contribution/patent section, followed by
  sections 4–9 and an Apache boilerplate appendix with placeholder owner/year.
  It appears truncated. Terms link the actual file and identify the limitation;
  this task does not choose or replace a license. Clarify before asserting complete
  open-source license terms to visitors.
- Reset retention is disclosed accurately. Retired-demo cleanup is a follow-up,
  not implemented or promised here; an operational privacy-request response is needed.
- Production fallback is operator-confirmed, not verified by repo inspection. See
  [deployment notes](DEPLOYMENT.md); no Caddy/deployment config is added or changed.

## Sources and boundaries

The draft's rights/contact structure was checked against the
[Québec CAI privacy-responsibility guidance](https://www.cai.gouv.qc.ca/protection-renseignements-personnels/information-entreprises-privees/responsable-protection-renseignements-personnels-entreprise)
and [European Commission information for individuals](https://commission.europa.eu/law/law-topic/data-protection/information-individuals_en).
They do not establish this operator's identity, legal grounds or compliance.
[OpenAI API data controls](https://developers.openai.com/api/docs/guides/your-data)
distinguish application-state settings, provider retention controls and residency;
account-specific facts remain unverified. No live provider request was used.

## Verification

Combined recovery verification is recorded in STATUS and LAUNCH-RECOVERY.
Tests use mock API calls and existing deterministic browser-test providers only.
Local production-bundle navigation uses Vite preview, not the external production edge.
