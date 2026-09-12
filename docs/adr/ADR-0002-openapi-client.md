# ADR-0002: Generate frontend types from an OpenAPI contract

Status: Accepted
Date: 2026-09-12

## Context

The mandated API foundation should avoid manually duplicating backend DTOs in TS.
The first slice has three small project operations and stable problem responses.

## Decision

Keep a versioned OpenAPI document in packages/api-client. Generate TypeScript with
openapi-typescript; use openapi-fetch for typed operations. Serve the same document
from the API. Backend integration tests verify representative responses and errors
against the contract; CI verifies generated files do not drift.

## Alternatives considered

Handwritten TS DTOs invite divergence. Runtime annotation-based generation adds
another Boot compatibility dependency before it is needed. A generated Kotlin
server would add substantial machinery for three endpoints.

## Consequences

Kotlin request/response classes must remain aligned with the contract and integration
tests. Contract-first changes and regeneration travel together. No frontend runtime
dependency on the Spring server is needed to generate types.

## Delivery impact

A small repeatable generation command provides a useful boundary on Day 1.

## References

- [openapi-typescript](https://openapi-ts.dev/introduction)
- [openapi-fetch](https://openapi-ts.dev/openapi-fetch/)
- [Implementation plan](../IMPLEMENTATION-PLAN.md)
