# Astra integration

Status: documentation verification only on 2026-09-12. **No API adapter or paid
analysis call is implemented in P0 foundation.** No claim of tested account access.

## Verified official contract

- Model identifier is `gpt-6-astra`; model card lists text/image input, text output,
  Responses, Structured Outputs and function calling.
- Responses accepts a message content array with `input_text` and `input_image`.
  Images can use a URL, base64 data URL or file ID; multiple images consume tokens.
- Structured Outputs for Responses uses `text.format` with a strict JSON schema,
  rather than parsing prose. Refusals and incomplete outputs require explicit handling.
- Raw video is not an input modality on the verified model card. SceneProof will
  extract representative frames with FFmpeg as required by the initial context.

Sources read: [model card](https://developers.openai.com/api/docs/models/gpt-6-astra),
[image inputs](https://developers.openai.com/api/docs/guides/images-vision),
[Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs).
Re-verify before adapter implementation. Function calling schemas, asynchronous
tool semantics and mid-turn steering are **not yet independently verified for this
implementation** and must not be advertised as implemented.

## Planned implementation gate

Use ContinuityAnalysisPort at the application boundary. Spring AI 2.0.x documents
Boot 4.1.x compatibility; verify its concrete Responses capabilities, then select
Spring AI, official Java SDK or HTTP in an ADR. Do not add an unused SDK now.

Request strategy: bounded adjacent-shot windows plus relevant references and rules;
stable identifiers alongside each image; intentional-change context scoped to affected
shots; no all-pairs N² comparison. Final limits and chunk strategy belong to the
analysis slice and must be measured against quality, latency and budget.

Result contract will include summary, inspected shots, findings, warnings and usage
metadata. Validate schema, project membership, affected/reference IDs, confidence and
evidence before persistence. Retain a failed AnalysisRun for refusal, truncation or
invalid output. Retry only classified transient failures with bounded attempts;
never present empty findings as a successful failed analysis. Timeouts and quotas
remain to be implemented and measured, not assumed.

Steering creates persisted context and targeted new analysis; preserve prior findings
and show supersession. A regular follow-up request is sufficient for this product
flow unless verified mid-turn support demonstrably improves it.

All keys stay server-side. No keys or images in logs. Record run ID, stages, durations,
counts and aggregate usage. Public live-demo budget and isolation require a concrete
decision before exposure; there are no model calls in foundation and therefore no
foundation model spend.
