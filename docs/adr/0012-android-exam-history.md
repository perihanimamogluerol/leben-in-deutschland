# ADR 0012: Persist Android mock-exam history

## Status

Accepted

## Context

The timed mock exam previously kept only one active or completed snapshot in
DataStore. Starting another exam replaced the previous result, so users could
not compare attempts or inspect earlier answers. Existing study progress must
continue to count submitted mock-exam answers, while a restored result from an
older app version must not increment that progress a second time.

## Decision

Store submitted exams in Room using two tables:

- `mock_exam_attempts` stores the immutable attempt summary, timestamps,
  language, state, score, unanswered count, and timeout status.
- `mock_exam_question_results` stores the ordered question ID, selected answer
  ID, and correct answer ID for every one of the 33 questions.

The attempt ID is deterministic for one started exam. Inserting an already
archived attempt is therefore a no-op. A normal submission stores history and
updates study progress in one transaction. When a completed DataStore snapshot
from the previous app version is restored, it is archived without updating
study progress because the previous version already recorded those answers.

History rows deliberately do not have a foreign key to a content release. This
keeps aggregate results and answer outcomes when an obsolete content release is
removed. The detail screen resolves question text from installed content when
available and displays a clear fallback when that content no longer exists.

Changing the selected language or federal state may clear the resumable
DataStore exam, but it does not delete Room-backed history.

## Consequences

- Every submitted exam is retained and shown newest first.
- Users can inspect correct, wrong, and unanswered questions for an attempt.
- Submission is idempotent and cannot double-count study progress after a
  retry.
- Room schema version increases from 2 to 3 and migration `2 -> 3` is required.
- Historical question wording depends on the corresponding content release
  remaining installed; answer IDs and result summaries remain available even
  after content cleanup.
