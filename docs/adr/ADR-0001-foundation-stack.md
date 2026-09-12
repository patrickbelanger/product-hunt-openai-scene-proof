# ADR-0001: Implement the mandated monorepo foundation

Status: Accepted
Date: 2026-09-12

## Context

Repository inspection found no application. Initial context fixes the stack;
Patrick explicitly clarified Boot 4.11 as 4.1.1. Existing source is not replaced.

## Decision

Use one Kotlin/Spring Boot application under apps/api and React under apps/web.
Use Java 25, Kotlin 2.3.21, Boot 4.1.1, Gradle 9.1.0, Spring Data JPA, Flyway and
PostgreSQL. Backend versions are fixed by plugins and the Boot BOM. Use a Node 22
LTS-compatible frontend with React 19 and Mantine, exact direct versions and npm
lockfile. Spring AI 2.0.x supports Boot 4.1.x; defer the model dependency until the
analysis adapter actually needs it. No model implementation is claimed in foundation.

## Alternatives considered

Boot 4.11 cannot be selected from verified stable documentation. Changing language,
framework, database or monorepo violates product constraints. An in-memory runtime
would fail durability acceptance. Dockerizing web/API is unnecessary locally.

## Consequences

Local development needs Java 25 and PostgreSQL. Schema migrations own DDL; Hibernate
validates it. Domain state remains relational; later binary media stays outside DB.
No authentication is implemented; default services bind loopback. Public deployment
requires its own isolation and hosting decision.

## Delivery impact

Build an immediately testable persistence flow within Day 1 and freeze versions
after green verification. This avoids consuming the five-day sprint on platform work.

## References

- [Spring Boot requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Kotlin/Gradle compatibility](https://kotlinlang.org/docs/gradle-configure-project.html)
- [Gradle/Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html)
- [Spring AI compatibility](https://docs.spring.io/spring-ai/reference/getting-started.html)
- [Mantine Vite setup](https://mantine.dev/guides/vite/)
