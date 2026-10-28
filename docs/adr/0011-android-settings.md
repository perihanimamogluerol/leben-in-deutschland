# ADR 0011: Android language and federal-state settings

## Status

Accepted

## Context

Language and federal-state choices were previously available only during onboarding. Users need to correct or change either choice without deleting their learning history.

Study progress is already scoped by content release, language and federal state. Mock-exam snapshots are not safely reusable after either selection changes because their question set and localized content may no longer match the active configuration.

## Decision

- Add a settings destination opened from the home-screen header.
- Let users change language and federal state independently through the existing selection screens.
- Preserve study progress for every language and federal-state combination. Returning to an earlier combination restores its progress.
- Clear the saved mock exam only when the selected language or federal state actually changes.
- Re-run content bootstrap after a language change so the selected translation is available and verified.
- Do not re-download content after only a federal-state change because the installed content package already includes every state question.
- Keep Back non-destructive: leaving a selection screen without continuing changes nothing.
- Localize the settings surface for every supported language and retain RTL behavior.
- Use the same top bar, typography, spacing and neutral selected-row treatment on both selection screens.
- Position an existing federal-state selection inside the visible list viewport so it is not clipped by the fixed continuation panel.

## Consequences

Users can safely switch their study context while keeping historical progress. An active or completed mock exam is discarded when its context becomes invalid, and language changes may briefly show content preparation before returning home.
