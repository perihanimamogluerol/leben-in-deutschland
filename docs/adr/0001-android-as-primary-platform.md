# ADR 0001: Android as the Primary Platform

- Status: Accepted
- Date: 2026-10-10

## Context

The product will be available as native Android and iOS applications.

Developing both platforms with equal scope from the beginning would increase complexity, slow down architectural validation, and make it harder to demonstrate deep platform expertise.

The primary career goal of this project is to demonstrate senior-level Android engineering while also building practical native iOS experience.

## Decision

Android will be the primary platform and the main engineering showcase of the project.

The Android application will receive new features, architectural improvements, performance work, and comprehensive testing first.

The iOS application will be developed as a native SwiftUI companion. It will follow the same product rules while using platform-appropriate Apple technologies and design patterns.

Feature parity between Android and iOS is a goal, but it is not required for every development milestone.

Shared business logic or Kotlin Multiplatform will not be introduced during the initial implementation. Each platform will remain native until real duplication or maintenance costs justify reconsidering this decision.

## Rationale

This decision allows the project to:

- Demonstrate advanced Android architecture and platform knowledge
- Keep the initial implementation scope manageable
- Validate product and domain decisions on one platform first
- Build meaningful Swift and SwiftUI experience without hiding platform differences
- Compare native Android and iOS implementations
- Avoid premature cross-platform abstraction

## Consequences

### Positive

- Android can demonstrate greater architectural depth
- Product decisions can be validated before implementing them twice
- Each application can follow its platform's conventions
- Technical trade-offs will be easier to explain during interviews
- iOS development remains part of the project without blocking Android progress

### Trade-offs

- Android and iOS may temporarily have different feature coverage
- Some domain and data-layer code may be duplicated
- Changes validated on Android may require a separate iOS implementation
- Maintaining two native applications requires additional development time

## Reconsideration Criteria

This decision should be reviewed if:

- Duplicated business logic becomes difficult to maintain
- Release synchronization becomes a product requirement
- The project gains additional contributors
- Kotlin Multiplatform provides a measurable maintenance benefit
- Both native implementations become sufficiently stable for comparison