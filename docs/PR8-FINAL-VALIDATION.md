# PR8 final validation recovery

September 14, 2026. Branch: `feat/p1-ai-film-understanding`.
PR7 base: `ed6066283d27da97484425f900b6bccb447b0911`.
Recovered clean HEAD: `f1024fb2c686d82b3e471af4c44e267840598afe`.
Application/source implementation: `8f3c689925b537e7678d1f356900df877cb95445`.
Finalization changes documentation only; final pushed SHA is supplied in the handoff.
No inference request, retry, reroll, database write, merge or PR9 work occurs here.

## Successful run and critical source distinction

Read-only recovery of public PostgreSQL records found a subsequent SUCCEEDED run.
Patrick reports the successful UI flow after credential correction; durable provider
IDs and validated results establish success, but credential changes and the exact
running binary's Git SHA were not recorded in the run and are not reconstructed.
No new `.local` paid lock is fabricated for this UI-originated historical event.

| Field | Persisted evidence |
| --- | --- |
| Project | `69d24a43-905a-4ba1-9935-6509526023f5` |
| Run | `104fed03-5210-4269-a6e8-169ced53f2ce` |
| SourceFilm | `aec45966-70ac-45a1-8d8c-b93a0b0e7fed` |
| Source SHA-256 | `ac9b29c47eeb399dbd1ac5cdfcde19273e0e4e68f5df8fd67fedbef3d284a7a7` |
| Source | Original **92.458667-second master**, persisted rounded duration 92,459 ms |
| Extracted audio duration | 92,458 ms |
| Started / completed UTC | `2026-09-14T16:19:57.011639Z` / `2026-09-14T16:21:34.072707Z` |
| Total elapsed | 97.061068 seconds |
| Final state | SUCCEEDED; audio TRANSCRIBED |

This proves the historical master received successful end-to-end provider processing.
It does **not** prove live acceptance of the 36.291667-second derivative. Transcript
evidence beyond 36.291667 seconds belongs to the master and is not a derivative-bound
validation failure. New/reset demo sources still use derivative SHA-256
`2205c5c0a9ddc98fe7de897bc960775539095f75ee30e567f31ccca8570de835`.
Both checked-in hashes were reverified during finalization; neither asset nor any
historical project/run was changed. See [derived-source evidence](PR8-DERIVED-SOURCE.md).

## Audio Transcriptions

- Production adapter endpoint: POST `https://api.openai.com/v1/audio/transcriptions`.
- Persisted model: `gpt-4o-transcribe-diarize`; request ID
  `req_5c3d01a72ef84d6baacb04e511f95020`.
- Current request uses binary `source.wav` / `audio/wav`, `diarized_json`, and
  `chunking_strategy=auto`. No Chat Completions or Responses transcription.
- Successful parsing and TRANSCRIBED state establish an accepted 2xx response;
  the exact upstream success HTTP number is not persisted. Do not invent HTTP 200.
- Six persisted segments, source-relative provider estimates, ordered and positive,
  all within the 92,458 ms extracted audio. Origin is
  `OPENAI_DIARIZED_SEGMENT_ESTIMATE_SOURCE_START`; no speaker authority is retained.
- Bounds in milliseconds: 13050–16450; 18700–22400; 25716–28866; 29616–36066;
  38016–43566; 44616–47366. No full transcript is copied into documentation.
- TRANSCRIBING_AUDIO: `16:19:58.024208Z`–`16:20:19.149665Z`, **21.125457 s**.
  This includes extraction/persistence, not isolated HTTP latency.
- Transcription usage and billed cost are not retained by TranscriptionCompletion.
  They remain unknown; Astra token usage is not audio usage or a billing substitute.

## Astra Film Understanding

- Model `gpt-6-astra`, reasoning **medium**; Responses is used for visual/text
  understanding only, after valid timed transcription. No change to sequence LOW
  or targeted HIGH reasoning.
- Provider request `req_c54480ef71ca4cf295c6971e42802427`;
  response `resp_0da26236945b3b18016aa81ec4eaa887d2a832894f8ae1e5e3`.
- Persisted usage: **13,140 input / 3,796 output / 16,936 total tokens**;
  reasoning 44, cached input 0, cache writes 13,137. Reasoning is not added to output again.
- UNDERSTANDING_FILM: `16:20:19.150759Z`–`16:21:34.062081Z`, **74.911322 s**.
  This is stage elapsed, not separately instrumented provider latency.
- Standard-rate estimate: `(3 × $10 + 13,137 × $12.50 + 3,796 × $50) / 1,000,000`
  = **USD $0.3540425**. Cache-write tokens replace the corresponding ordinary-input
  rate, not double billing. This assumes Standard pricing, not an observed invoice;
  service-tier/billing details are not retained. Rates checked September 14 against
  [official Astra pricing](https://developers.openai.com/api/docs/models/gpt-6-astra).
- 8 inspected visual segments / 24 frames; **3 recurring entities, 3 narrative cues,
  3 candidate anchors, 2 potential concerns, 3 warnings**. All three candidate rows
  are PENDING at recovery; none is creator-confirmed or enforced from this run.

### Saved final summary (verbatim model output, not verified film truth)

> The samples suggest a journey from metro commuting to a nighttime home setting: a bespectacled man uses a phone, a woman appears on a phone display, and a similar-looking woman later fills his desktop monitor. The transcript evokes restlessness and private digital expression rather than establishing literal actions or speaker identity. Supporting evidence is detailed below. All supplied samples from 36.292 seconds onward appear black; intervening footage and the ending cannot be reconstructed.

Entities are the bespectacled man, woman depicted on screens, and window-side
workstation. Candidate anchors concern home-sequence appearance, cross-device screen
persona, and workstation arrangement. Concerns ask for phone hardware identity
confirmation and review of black later samples; neither is a confirmed error.

Warnings state: only 24 sparse stills were inspected, not exhaustive shots/motion;
audio context contains six estimated transcript excerpts, not direct audio inspection
or proof of silence outside them; proposed invariants require creator confirmation,
and differences/questions are not established continuity errors.

**Screen-opening evaluation:** naturally discussed in the saved narrative cue
“Private expression through technology,” associating screen-opening/writing language
with desk imagery and keyboard activity. The model also treats the dark display
becoming populated as a plausible state change, not an invariant violation. This is
a contextual relationship, not proof of exact lyric/action synchronization or a
detected continuity error. The prompt has no benchmark answer or film-specific hint;
no tuning, reroll or reinterpretation was performed during finalization.

## Separate historical events

1. Original run `f8a8960c-a396-4524-b810-5b38661ed176`: rejected Whisper request,
   insufficient retained diagnostics; **INSUFFICIENT_EVIDENCE**, unchanged.
2. Corrective transport identity `87b0d170-3387-4959-8711-a2ef647f20ca`:
   TRANSCRIPTION_UNAVAILABLE without upstream evidence; **INSUFFICIENT_EVIDENCE**.
3. Derived run `87e8fdfb-7abd-4d21-ba10-4a5975a47038`: HTTP 401 invalid_api_key,
   **PROVIDER_OR_ACCOUNT_RESTRICTION** (authentication); no Astra.
4. Subsequent successful master/UI run above: validated transcription and Astra.
   No assertion that earlier operator/UI attempts did not occur: readback also found
   master run `814db7b6-e701-4b6e-975c-840b035c441d` at 11:33 UTC with generic rejection
   (INSUFFICIENT_EVIDENCE), and master run `84e2eece-62bf-4c02-8628-ee3e434b3505` at
   16:15 UTC with HTTP 401 invalid_api_key (authentication rejection). Both remain
   distinct and unchanged. They are not automatic retries performed by finalization.
5. Derived-source deterministic validation: repeatable 36.291667-second asset,
   matching audio, frame bounds, new/reset provenance; no successful live derivative claim.

Original, corrective and derived `.local` locks/manifests/results remain untouched.
Database recovery reads existing records only; no historical classification is
retroactively inferred from the later successful credential state.

## Product and final gates

Code and persisted stages confirm source verification → visual preparation → Audio
Transcriptions → validated timed text → Astra → validated candidates → SUCCEEDED.
The adapter fails closed on invalid transcript timing before Astra. Provider work
has no application retry; new attempts require a new explicit consent/identity.
Strict evidence validation, candidate Accept/Edit/Reject, normal ReferenceService
promotion and confirmed-only continuity memory remain unchanged. Difference is not
a continuity error. Stages are backend-authoritative; polling is not fabricated progress.

No application code changed since the derivative gates. Retained evidence:
**39 focused / 122 full backend tests**, zero failures/errors/skips, Gradle build,
frontend TypeScript/Vite build and OpenAPI drift pass. Unchanged frontend has **78
tests** and **19 Chromium tests** in the earlier successful logs
`.local/pr8-verified-web-tests.log` and `.local/pr8-verified-e2e.log`; other failed
exploratory logs are not counted as passes. These suites are historical verification,
not rerun in documentation-only finalization. Normal tests use deterministic ports
or loopback capture, never OpenAI. Final documentation links/diff checks and a fresh
`npm run api:check` pass. The existing approximately 519 kB Vite chunk warning remains.
API 8085 health and frontend 5173 both respond successfully to read-only checks;
user services are left running. Remote CI is not checked.

## Merge assessment and PR9 boundary

PR8 is ready for merge review with disclosed limitations, not an automatic merge.
The master success validates the end-to-end provider path; derivative processing is
deterministically verified and its live success remains untested. This is an evidence
limitation, not an established implementation blocker requiring another paid request.
No inference is authorized in finalization. Timestamp completeness/accuracy, precise
HTTP latency, transcription billing, sparse sampling, local-only tenancy, one-way
anchor decisions and lack of media garbage collection remain limits.

PR9 UX polish is the next milestone, **not started**. Capture these reported issues
without implementing them here: polling/manual refresh flashes; no polished visible
active-run progress bar; weak loading/current-stage hierarchy; cluttered results;
summary/warnings/concerns/evidence compete; overly tall evidence cards; clearer
master-vs-analysis duration/source presentation; missing project deletion. Any future
progress treatment must still use actual stages, not invented completion percentages.
Deletion requires deliberate history/media semantics, not silent removal of evidence.
