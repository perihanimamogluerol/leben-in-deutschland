# ADR 0013: Add German question-to-answer memory keys

## Status

Accepted

## Context

Explanations help users understand an answer, but they do not provide a short
recall signal for repeated practice. The production catalog contains 460
questions. Of these, 441 have a textual correct answer and 19 require selecting
an image. Inventing a textual description for an authoritative image answer
would introduce avoidable content risk.

The currently active remote content format is schema 3. Existing app versions
must continue to synchronize new releases even if they do not display memory
keys.

## Decision

Add an optional German `memoryKey` object to each schema-3 question:

```json
{
  "questionCue": "Regierung kritisieren",
  "answerCue": "Meinungsfreiheit",
  "status": "AI_GENERATED_NEEDS_REVIEW"
}
```

This is an additive, backward-compatible schema-3 field. Older clients ignore
it. The Android Room schema advances from version 3 to 4 and stores the two
cues and their review status as nullable columns on `questions`. Old releases
therefore migrate with null values and remain usable.

Memory-key generation and publication enforce the following rules:

- Every textual correct answer has one German key pair when a release enables
  memory keys.
- Correct answers represented by images have no memory key.
- Each cue contains 1–8 words, is at most 80 characters, and does not contain
  an arrow or newline.
- Normalized question/answer pairs are unique within the catalog.
- AI output remains marked `AI_GENERATED_NEEDS_REVIEW` until human review.

The practice and wrong-answer screens place a collapsed memory-key disclosure
between progress and the question. A learner can deliberately open it before
or after answering, while every new question starts with the disclosure
collapsed. Questions without a memory key omit the control. The timed mock
exam never displays it, including while an exam is running or being reviewed.

After submission, one always-expanded explanation card appears below the
answers. It includes the correctness icon and label in the same card and uses
the success or error color scheme. There is no separate result card and no
automatic scrolling changes the learner's position.

## Consequences

- Users receive an optional German recall association at a predictable place.
- Opening the disclosure before submitting can reveal the correct-answer cue;
  this is an explicit learner action in practice modes.
- The same key is available regardless of the selected translation language.
- The 19 image-answer questions intentionally show no memory-key card.
- Activating a release without memory keys remains safe; the UI hides the card
  when the optional data is absent.
- Room migration `3 -> 4` and an exported version-4 schema are required.
