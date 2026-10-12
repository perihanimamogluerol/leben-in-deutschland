# ADR 0004: Store downloaded content in a normalized Room database

- Status: Accepted
- Date: 2026-10-11

## Context

The Android application downloads the active Schema 3 German package and the
user's selected study-language package. A package contains 460 questions,
localized answers and explanations, optional question or answer images, and
translation provenance. The app must work offline and later support filtered
practice, wrong-answer review, mock exams, and safe content updates.

Keeping whole chunks as JSON blobs would make those queries expensive and would
tie application features to the transport format. Replacing rows without a
release boundary could also expose a partially installed update.

## Decision

Use Room as the local source of truth and normalize Schema 3 into these groups:

- release and installed-package metadata;
- German questions and answers, including image metadata;
- per-language study text, answers, and explanations;
- translation provenance and per-answer matching evidence.

Every content row includes `release_id`. A package is replaced in one Room
transaction. A release can be activated only when its German package and the
required study-language package are both installed. Activating a release is a
separate transaction, and the currently active release cannot be deleted.

Question selection includes all general questions plus questions for the
chosen German state. Ordering is deterministic in the database; feature code
may shuffle the resulting IDs when the user requests it.

Room exports versioned schema JSON into `android/app/schemas`. These files are
committed so future migrations can be reviewed and tested.

## Consequences

- Practice and exam features can query content without parsing JSON at runtime.
- A new release can be installed alongside the active release and switched
  atomically after verification.
- Firestore download and Schema 3 JSON mapping remain separate responsibilities
  and can be implemented without changing the database contract.
- Database changes require explicit Room migrations and updated exported schema
  files.
