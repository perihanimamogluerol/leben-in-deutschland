# ADR 0010: Timed Android mock exam

## Status

Accepted

## Context

The Android app already provides self-paced study, persistent progress, and a
wrong-answer review. Learners also need a realistic way to rehearse the official
test conditions and turn mistakes from the exam into focused follow-up study.

The official format used by the app is 33 questions in 60 minutes: 30 general
questions and 3 questions for the learner's selected federal state. The passing
threshold is 15 correct answers for the *Leben in Deutschland* test and 17 for
the naturalization test.

## Decision

- Add a dedicated mock-exam destination reachable from the home dashboard.
- Start one shared mock exam instead of asking the learner to choose a test
  type. Both official outcomes use the same 33-question format.
- Build every exam from 30 unique random general questions and 3 unique random
  questions for the selected federal state.
- Start a 60-minute deadline when the exam is created. The deadline is stored as
  wall-clock time, so closing the app does not pause the exam.
- Persist the full active exam snapshot in a dedicated Preferences DataStore:
  question order, selected answers, current question, start time, deadline, and
  completion result.
- Restore the snapshot only when its content release, language, and federal state
  still match the current app selection. Clear stale snapshots.
- Allow previous/next navigation and unanswered questions. Require confirmation
  before an early submission and submit automatically when time expires.
- Do not reveal correctness or explanations during the exam.
- Keep answers isolated while an exam is active. On submission, record only
  answered questions in study progress as one database transaction: incorrect
  answers enter wrong-answer review, correct answers clear an existing wrong
  status, and unanswered questions leave existing progress unchanged.
- Show correct, wrong, and unanswered counts after submission. Evaluate the
  same result against both thresholds: 15 correct for *Leben in Deutschland*
  and 17 correct for proof of naturalization knowledge.
- Localize the complete flow for all 21 supported languages and preserve RTL
  behavior through the existing locale configuration.

## Consequences

- A running exam survives activity recreation, process death, and app restarts.
- The remaining time reflects real elapsed time rather than only foreground time.
- No Room migration is needed because the single resumable exam is user state,
  not relational study history.
- Completed results remain available from the home dashboard until the learner
  starts a new exam.
- Learners see both qualifications on one result screen, avoiding a misleading
  choice between two otherwise identical exam sessions.
- Wrong answers from completed mock exams become available for focused review,
  while abandoned exams and unanswered questions do not change study history.
- Changing language, state, or active content release invalidates the saved exam
  instead of mixing incompatible questions and translations.
