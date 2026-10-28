# ADR 0007: Android home dashboard and study session

## Status

Accepted

## Context

After onboarding and content bootstrap, the application needs a useful offline-first destination. Users must be able to confirm their selected language and federal state, see that content is installed, and begin studying without another network request.

Questions contain official German text, localized study text, optional images, a correct answer, and an explanation. The interface must remain readable for older users and work in every supported application locale.

## Decision

- Replace the completed content-bootstrap destination with a home destination.
- Show the installed question count and the persisted language and federal state on the home dashboard.
- Start a study session from the active Room release. The eligible set contains general questions plus questions for the selected federal state.
- Randomize unanswered questions whenever a new study session is created, while keeping the in-memory order stable for the lifetime of that session.
- Keep already answered questions before the randomized remainder so resumed study starts at a random unanswered question without repeating completed work.
- Load each question and its selected-language study content from Room through `LocalContentRepository`.
- Present German and localized question and answer text together when they differ.
- Require an answer selection before submission, then show correctness and the localized explanation before allowing navigation to the next question.
- Use Coil 3 for optional question and answer images. Coil may reuse its disk cache, while question text and answers remain fully available from Room.
- Keep navigation state in Navigation 3 and screen state in a lifecycle-aware `ViewModel`.
- Provide localized home and study interface strings for all 21 supported languages.
- Preserve the dark, high-contrast design with large controls and touch targets.
- Keep progress neutral and show both correct and wrong answer counts in one summary.
- Use warm amber for wrong-answer review while work remains, success green only when the list is empty, and deep blue for the timed mock exam.
- Present wrong-answer review as a full-width, left-aligned action row with a trailing arrow instead of refresh icons.

## Consequences

- A successful bootstrap leads directly to a functional home screen instead of a terminal status screen.
- Study sessions work from verified local content and do not depend on a Firestore read for each question.
- Image availability on a device without a network connection depends on whether Coil has already cached that image.
- Session order, scoring, bookmarks, and persisted progress can be added independently in later features.
