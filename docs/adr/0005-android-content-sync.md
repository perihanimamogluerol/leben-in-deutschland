# ADR 0005: Verify Schema 3 content before local activation

- Status: Accepted
- Date: 2026-10-12

## Context

The Android app must download the active Schema 3 content release from
Firestore, remain usable offline, and avoid exposing partially downloaded or
tampered data. A study-language package is not self-sufficient because the app
also uses canonical German question and answer data.

Firestore stores the active pointer at `contentManifest/current`. Release,
language-package, and chunk documents are immutable descendants of
`contentReleases/{releaseId}`. The local Room database supports installing
packages beside the currently active release and activating a release later.

## Decision

Introduce three separate responsibilities:

- `FirestoreContentDataSource` reads only server-confirmed manifest, release,
  package, and chunk documents;
- `ContentPackageMapper` validates Schema 3 metadata, counts, document IDs,
  language codes, readiness flags, JSON byte sizes, and SHA-256 hashes before
  creating normalized Room snapshots;
- `ContentSyncCoordinator` downloads German plus the user's selected language,
  validates every required package before the first local write, installs both
  packages, and activates the release only after installation succeeds.

The active manifest and release document must agree on release ID, schema,
dataset, package strategy, counts, languages, catalog hashes, source hash, and
creation time. Only Schema 3 documents with `READY` status, zero quality
issues, and ready explanations are accepted.

Firebase initialization, scheduling, retry policy, and user-facing sync state
belong to the application integration layer and are intentionally outside this
change.

## Consequences

- A malformed or incomplete remote package does not replace usable local
  content or change the active local release.
- German and selected-language packages are guaranteed to belong to the same
  immutable release.
- Hash verification mirrors the canonical JSON rules used by the content
  publishing pipeline.
- Firestore access is testable behind a small data-source interface.
- A later application-integration change must supply a configured
  `FirebaseFirestore` instance and decide when synchronization runs.
