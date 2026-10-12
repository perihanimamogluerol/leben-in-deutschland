# ADR 0006: Bootstrap verified content without blocking offline use

- Status: Accepted
- Date: 2026-10-12

## Context

The content sync engine validates Schema 3 packages before activating them in
Room, but the Android application did not initialize Firebase, run that engine,
or expose first-install and retry states. Re-downloading 966 immutable chunk
documents on every launch would also waste bandwidth and delay the user.

The Firebase client configuration is environment-specific. It must be present
for a real device build while remaining outside source control and optional for
pull-request CI.

## Decision

- Add an application-scoped container for onboarding preferences, Room, and a
  lazily created content synchronizer.
- Apply the Google Services Gradle plugin only when the ignored
  `app/google-services.json` file is present. CI can still compile and test
  without credentials or Firebase configuration.
- Reject Firebase initialization unless the configured project ID exactly
  matches `leben-in-deutschland-38a07`.
- Route completed onboarding to a content-bootstrap screen.
- Treat Room as the source of truth. If German and the selected-language
  package already exist, expose that content immediately while checking the
  server manifest in the background.
- Skip release and package downloads when the immutable remote release ID is
  already active locally with both required packages.
- Keep usable local content active when refresh fails. Show retry only when no
  usable local content exists.
- Localize all bootstrap states for the same 21 languages supported by the
  onboarding flow.
- Version Firestore rules that grant public read-only access to the immutable
  content manifest and release hierarchy while denying all client writes and
  every unrelated document path.

## Consequences

- Returning users are not blocked by connectivity and do not repeatedly
  download unchanged packages.
- First installation has explicit loading, failure, and retry states.
- A configuration copied from another Firebase project cannot silently read or
  write the wrong backend.
- `google-services.json` remains a local build input and service-account keys
  are never included in the Android application.
- Content uploads continue through the trusted Admin SDK, which is not subject
  to client security rules.
- The ready state is intentionally a temporary destination until the home and
  study navigation is implemented.
