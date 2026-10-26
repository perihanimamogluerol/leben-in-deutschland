# ADR 0011: Settings for language and federal state

- Status: Accepted
- Date: 2026-10-26

## Context

The product specification lets learners change their application language
(section 8) and federal state (section 9) after onboarding. Onboarding already
tells learners that they can change both later in Settings, but the Android app
had no Settings destination.

Changing the language is not a plain preference write: the selected language
needs a validated content package on the device. A failed download must never
leave the app in a mixed-language or incomplete state.

## Decision

- Add a Settings destination reachable from the Home screen. It shows the
  current language and federal state and offers a change action for each.
- Use dedicated navigation keys for the change flows instead of reusing the
  onboarding keys, so onboarding's "persist and continue" behavior is not
  triggered from Settings.
- Language change reuses the existing language picker. After the learner
  confirms a language, a `LanguageChangeCoordinator` first checks whether the
  active release already contains the German and the selected-language package.
  If so, the switch happens immediately and works offline. Otherwise it runs the
  existing content synchronizer, which validates every package before writing.
- Persist the new language code and apply the application locale only after the
  coordinator reports success. While the download runs, a blocking dialog shows
  progress with a cancel action. On failure, the dialog offers retry or cancel
  and the previous language and content remain active.
- Federal state change reuses the existing state picker and only persists the
  new state code. Study progress is keyed by stable question IDs, so general
  progress is kept and state-specific progress becomes available again if the
  learner returns to that state. A saved mock exam is invalidated by the existing
  rule that requires a matching content release, language and state.
- Reuse existing localized strings for labels and retry. Add nine new strings
  localized for all 21 supported languages.

## Consequences

- Learners can change language and state without reinstalling or re-onboarding.
- A language that is already installed switches without a network connection;
  a missing one requires a connection, as required by the specification.
- The change flow cannot leave the app between languages: persistence happens
  after validation, and cancellation is side-effect free.
- The Settings screen is the natural home for the AI content notice and further
  preferences added later.
