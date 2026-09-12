# ADR-0004: Bounded Astra Responses integration

Status: Accepted within Patrick's explicit PR2 transport authorization
Date: 2026-09-12

## Context and DECISION REQUIRED record

Current choice: Spring AI is selected in the stack, with no dependency loaded.
Problem: the inspected Spring AI 2.0.1 OpenAI Chat documentation exposes Chat
Completions options; it does not establish a complete Responses adapter with
image input, text.format and incomplete/refusal handling. JSON Schema and vision
support alone do not establish Responses support.
Proposed alternative and recommendation: Java 25 HTTP behind
ContinuityAnalysisPort / AstraContinuityAnalysisAdapter. Patrick explicitly
authorizes the simplest verified HTTP or official-client integration when Spring
AI cannot cleanly provide the required path; this decision uses that authorization.

## Decision

Use POST https://api.openai.com/v1/responses, gpt-6-astra, inline normalized PNG,
strict text.format JSON Schema, no tools, no automatic retries, no streaming,
store=false. Preserve the existing Spring/Kotlin stack. No transport types cross
the application port. Establish a tiny original-image paid spike before integration.

One synchronous request inspects up to 8 READY shots, up to 3 deterministically
selected first/middle/last frames per shot (24 total). Reject larger projects
explicitly; never silently analyze a prefix. Include all selected shots in sequence
order and their neighbors in one context. No chunking or all-pairs requests.
Cap images at 8 MiB each, aggregate image bytes at 16 MiB, serialized request at
24 MiB, response at 256 KiB, output at 6000 tokens, timeout at 120 seconds,
connection at 10 seconds, and concurrency at one analysis per API process.
Use a durable client request UUID to deduplicate retries, plus a database-enforced
single RUNNING run globally. Final findings and SUCCEEDED commit atomically.

## Alternatives considered

Official openai-java 4.63.1 supports the documented Responses/image/schema builders
and is usable from Kotlin. For this one endpoint, JDK HTTP avoids another JSON
stack/dependency and implicit SDK retries. Spring AI Chat Completions would change
the required transport; an undocumented Responses workaround is rejected.
Distributed workers, window consolidation and progress transport exceed PR2.

## Consequences and delivery impact

The adapter owns bounded HTTP parsing/error handling and requires focused tests.
Temporal subsampling can miss brief changes. Oversized projects need a smaller
demo project until explicit chunk consolidation is designed. Timeout may still
incur provider cost; duplicate client request IDs never automatically retry it.
Public hosting/isolation/budgets remain pending. No PR3 UI or steering is added.
This keeps the September 18 critical path focused on a real persisted finding;
dates are unchanged. Runtime and smoke evidence are recorded in STATUS.

## References

- [OpenAI model](https://developers.openai.com/api/docs/models/gpt-6-astra)
- [OpenAI libraries](https://developers.openai.com/api/docs/libraries)
- [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs)
- [Spring AI OpenAI Chat](https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html)
- [Astra integration](../ASTRA-INTEGRATION.md)
