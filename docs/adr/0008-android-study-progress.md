# ADR 0008: Persist Android study progress locally

## Status

Accepted

## Context

The first study-session implementation loaded verified Schema 3 content from
Room, but answers existed only in `StudySessionViewModel` memory. Leaving the
screen or restarting the process discarded the learner's progress and the home
dashboard could not report meaningful study status.

Progress belongs to the user and must remain independent from localized content
package replacement. It must still be scoped to a content release, selected
language, and selected federal state so that incompatible question sets are not
mixed.

## Decision

- Add `study_question_progress` in Room schema version 2.
- Store one current answer record per release, language, state, and question.
- Preserve the latest selected answer, correctness, timestamp, and attempt count.
- Normalize state codes before persistence and queries.
- Resume a session at the first unanswered eligible question.
- Keep completed progress visible on the home dashboard until the learner
  explicitly starts another session; starting again clears that completed set.
- Calculate dashboard totals from the same eligible-question query used by the
  study session.
- Apply an explicit `1 -> 2` migration without destructive fallback.

## Consequences

- Process death and ordinary navigation no longer lose completed answers.
- Home progress updates reactively from Room.
- A content release deletion removes its progress through a foreign-key cascade.
- A future account-sync feature can map the local progress model to a remote
  representation without coupling study content installation to user history.
