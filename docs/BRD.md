# SceneProof — Business Requirements

## Executive summary and problem

SceneProof is an independent AI continuity supervisor for generative film. Every
generated shot can drift in identity, wardrobe, props, space or visible text.
Creators need contextual evidence and actionable correction before final editing.
A visual difference is not necessarily an error: declared intentional changes matter.

## Users and value proposition

Primary persona: an independent AI filmmaker reviewing a short sequence before edit.
Secondary personas: music-video creators, advertising creatives and storyboard artists.
SceneProof joins a Reference Bible, shot timeline and evidence-based findings in a
restrained cinematic workspace. It reduces manual continuity review and turns
observed drift into a reusable correction prompt; it does not regenerate footage.

## Goals and success criteria

- Challenge: present a credible standalone GPT-6 Astra product by September 18, 2026.
- A first-time Product Hunt visitor understands the value within one minute.
- A 60–75 second recording shows a real analysis, evidence and intentional-change revision.
- A supplied sequence yields at least one real, validated, persisted finding.
- The curated demo targets about five understandable issues and one or two intentional differences.
- A creator can explain an intentional change and see an auditable re-evaluation.

No revenue, user-count or model-accuracy target is invented for this sprint.

## Scope and functional requirements

| ID | Priority | Requirement / acceptance |
| --- | --- | --- |
| FR-01 | P0 | Create a named project with optional description and continuity rules; survive reload/restart. |
| FR-02 | P0/P1 | Supply reference images/rules for character, wardrobe, prop, environment and device/UI; polished editor follows the first slice. |
| FR-03 | P0 | Import validated stills and a bounded short video; preserve shot order and extraction timestamps. |
| FR-04 | P0 | Compare shots with relevant references and neighbors using real GPT-6 Astra multimodal reasoning. |
| FR-05 | P0 | Validate and persist structured findings; select one to inspect relevant images, expected/observed states, explanation and correction. |
| FR-06 | P0 | Resolve, dismiss or declare a change intentional; preserve creator context and prior finding history. |
| FR-07 | P0 | Re-analyze affected context and update/supersede findings without fabricating a successful outcome. |
| FR-08 | P1 | Offer a deterministic demo without mandatory upload or authentication, with bounded paid interactions. |
| FR-09 | P1 | Show real analysis stages, clear failure/retry states and a four-step skippable/restartable tour. |

Major use cases are create project, build Reference Bible, import stills/video,
analyze continuity, inspect/copy corrections, resolve findings and steer re-analysis.

## Non-functional requirements and UX

Durable PostgreSQL state, migrations, validated boundaries, bounded media and model
work, safe filenames/process invocation, server-only secrets, useful redacted logs,
typed API contracts and tests around failure paths. Laptop/tablet layouts and
keyboard access are required. The primary interface is a visual workspace, never
a chat transcript. Loading, empty and error states must be truthful.

## Constraints, assumptions and dependencies

Five build days September 12–16, September 17 buffer/freeze and September 18 launch.
Kotlin 2.3+, Java 25, Spring Boot 4.1.1 (confirmed clarification), React 19, Mantine,
PostgreSQL, Gradle and FFmpeg are constraints. Astra access, original or licensed
demo media, Docker/local PostgreSQL and a public hosting decision are dependencies.
Public anonymous use requires isolation and cost limits before deployment.

## Out of scope

Billing, organizations, enterprise tenancy, collaboration, full video editing,
generation/regeneration, microservices, queues/brokers, Kubernetes, native mobile,
proprietary product integration and automatic third-party generation workflows.

## Risks and release acceptance

Model latency/cost, false positives, unreadable evidence, unsuitable media,
platform compatibility and public upload exposure are principal risks. Mitigate
with small early live analysis, explicit intentional-change examples, bounded
frames/requests and a freeze buffer. Ship only when the first two vertical slices
work on the curated sequence, critical tests/builds pass, setup is reproducible,
public deployment safeguards are verified and the recording matches actual behavior.
