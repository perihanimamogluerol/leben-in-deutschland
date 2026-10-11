# ADR 0003: Android Application Architecture

- Status: Accepted
- Date: 2026-10-11
- Decision Owners: Mobile Engineering
- Related Documents: `docs/product-specification.md`, `docs/adr/0001-android-as-primary-platform.md`, `docs/adr/0002-language-specific-content-packages.md`

## Context

The Android application is the primary implementation of a multilingual,
offline-first study product. It must support content download, practice,
wrong-answer review, timed mock examinations, anonymous progress, and reliable
operation after the initial download.

The architecture should demonstrate production-minded Android engineering
without adding module boundaries that the current team size and build do not
yet justify.

## Decision

### Application shape

- Use a single-activity application built with Jetpack Compose.
- Use the stable Navigation 3 API with serializable, type-safe navigation keys.
- Keep `MainActivity` limited to hosting the application composition root.

### Module strategy

- Start with one `app` Gradle module.
- Organize code by feature and responsibility inside the module.
- Preserve boundaries that can later become Gradle modules without rewriting
  business logic.
- Introduce additional modules only when ownership, reuse, build performance,
  or isolation provides a measurable benefit.

Initial package direction:

```text
com.nextdaymobilestudio.lebenindeutschland
├── app
├── core
│   ├── designsystem
│   ├── model
│   └── navigation
├── data
│   ├── local
│   ├── remote
│   └── repository
├── domain
└── feature
    ├── languageselection
    ├── stateselection
    ├── home
    ├── practice
    ├── wronganswers
    └── mockexam
```

### State and data flow

- Follow unidirectional data flow.
- Each screen-level ViewModel exposes one immutable `UiState` stream.
- UI actions enter through explicit ViewModel functions or typed actions.
- Compose screens render state and do not access databases, Firebase, or other
  data sources directly.
- Collect state with lifecycle awareness.

### Layers

- The UI layer contains composables, screen state, and screen-level ViewModels.
- The data layer contains repositories and local or remote data sources.
- Repositories are the only entry points from ViewModels to application data.
- Add domain use cases only for shared or sufficiently complex business rules;
  do not create pass-through use cases for every repository function.

### Offline-first behavior

- Room will be the source of truth for downloaded questions and study progress.
- Firestore is the remote source for versioned content packages and optional
  progress synchronization.
- DataStore will hold small user preferences such as selected language and
  state.
- Screens observe local data and do not wait on Firestore for normal reads
  after the initial synchronization.

### Dependency injection

- Use constructor injection.
- Introduce Hilt when repositories and data sources are added.
- Keep interfaces focused on behavior that needs substitution or establishes a
  layer boundary; avoid interfaces created only for ceremony.

### Testing

- Unit-test ViewModels, repositories, mappers, and business rules.
- Test Compose screens using state-driven semantics rather than visual
  implementation details.
- Add integration tests for Room migrations and content synchronization.
- Keep the existing build, lint, and unit-test workflow as the minimum pull
  request gate.

## Consequences

### Positive

- Feature code remains easy to find and review.
- UI behavior can be tested without Firebase or Android framework data access.
- Offline behavior has an explicit source of truth.
- The project demonstrates deliberate architecture without premature
  modularization.
- Navigation and screen state can survive configuration changes and process
  recreation.

### Trade-offs

- A single module provides less build isolation than a multi-module project.
- Package boundaries rely on engineering discipline until modules are needed.
- Navigation 3 is newer than Navigation 2, so its usage must remain covered by
  tests and isolated behind the app navigation package.

## Revisit Criteria

Reconsider module boundaries when at least one of these conditions is met:

- incremental build time becomes a measurable development problem;
- multiple developers need independent feature ownership;
- a component is reused by both Android and another Kotlin target;
- dependency boundaries are repeatedly violated despite package-level rules;
- a feature requires independent delivery or testing infrastructure.
