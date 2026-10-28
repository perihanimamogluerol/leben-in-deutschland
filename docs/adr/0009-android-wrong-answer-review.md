# ADR 0009: Review wrong answers from persisted progress

## Status

Accepted

## Context

Learners need a focused way to revisit mistakes without repeating every
eligible question. Room schema version 2 already stores the latest correctness
and attempt count for each release, language, state, and question.

Creating another table or copying question content would introduce competing
sources of truth and make content-release cleanup harder.

## Decision

- Derive the wrong-answer set from persisted `study_question_progress` rows
  whose latest answer is incorrect.
- Randomize the current incorrect-question set whenever a new review session is created.
- Reuse the existing study screen and answer-persistence path with an explicit
  `WRONG_ANSWERS` session mode.
- Replace the current progress row on every attempt. A correct retry therefore
  removes the question from the wrong-answer set; another incorrect retry keeps
  it in the set and increments its attempt count.
- Show the current wrong-answer count and review entry point on the home screen.
- Provide explicit empty and completion states instead of treating an empty
  review set as an error.
- Keep Room schema version 2 because no stored structure changes.

## Consequences

- Wrong-answer review stays consistent across process death and app restarts.
- Corrected questions disappear automatically without a separate cleanup job.
- Review sessions remain scoped to the active content release, selected
  language, and selected federal state.
- Future spaced-repetition logic can build on the same attempt and correctness
  records without duplicating content.
