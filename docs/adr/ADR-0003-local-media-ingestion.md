# ADR-0003: Bounded local media ingestion

Status: Accepted (implementation of the authorized PR1 plan)
Date: 2026-09-12

## Context

The accepted plan reserves MediaStorage, a local adapter, relational shot/frame
metadata and controlled FFmpeg before Astra. PR0 is merged as fefb7db.

## Decision

Implement synchronous ingestion with one active decoder per API process. Store
originals and normalized PNG frames under generated project/shot keys outside
PostgreSQL. Use short Spring JDBC transactions and a project row lock for final
READY/FAILED shot state and ordered frames. Existing Project persistence remains
JPA; both share the datasource and transaction manager. Decode holds no DB lock.

Select scene changes with periodic fallback and minimum spacing in one bounded
FFmpeg pass. Persist actual presentation timestamps, null for stills. Operational
limits and exact arguments are documented in MEDIA-PIPELINE.

## Alternatives considered

Binary database storage violates the planned boundary. A broker/worker fleet or
cloud storage adds infrastructure before the local continuity slice. Uniform-only
sampling omits planned scene detection; extracting everything is unbounded.
Persisted in-progress jobs require recovery semantics for the later progress slice.

## Consequences

FFmpeg is a runtime and CI dependency; image uploads work without it. Normal
failure cleanup compensates filesystem writes, but abrupt termination may leave
orphan directories. Public deployment still needs the pending safeguards decision.
One video is one imported shot with representative frames, not automatic shot splitting.

## Delivery impact

Implements the authorized PR1 prerequisites without new services or paid calls.
September 18 target and branch order are unchanged.

## References

- [Media pipeline](../MEDIA-PIPELINE.md)
- [Implementation plan](../IMPLEMENTATION-PLAN.md)
