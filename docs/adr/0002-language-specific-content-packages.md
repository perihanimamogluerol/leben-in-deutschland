# ADR 0002: Language-Specific Offline Content Packages

- Status: Accepted
- Date: 2026-10-10
- Decision Owners: Mobile Engineering
- Related Document: `docs/product-specification.md`

## Context

The application supports 21 languages and 460 German source questions.

Users must be able to study offline after the initial content download. However, downloading every translation is unnecessary because most users will study with German and one selected language.

The active `schema-2` Firestore release stores the complete German source and all 20 translations inside each question object.

The uploader groups 10 complete question objects into each Firestore chunk. This produces 46 chunk documents.

Firestore returns complete documents. A mobile client cannot request only the German and Turkish fields from a chunk that also contains English, Arabic, French, and the other translations.

Therefore, the current document structure cannot provide a true language-specific download.

## Observed Schema 2 Structure

A simplified `schema-2` question has the following structure:

```text
question
├── id
├── metadata
├── correctAnswerId
├── german
└── localizations
    ├── en
    ├── tr
    ├── uk
    ├── ar
    └── all other supported translations
```

The uploader places 10 complete questions into each chunk without removing unused languages.

A local inspection of the current catalog produced these approximate compact JSON sizes:

| Content | Approximate Size |
|---|---:|
| All questions with 21 languages | 8.54 MB |
| German content only | 0.36 MB |
| German and Turkish content | 0.77 MB |
| Turkish localization only | 0.42 MB |

German and Turkish together require approximately 91 percent less question data than the complete multilingual catalog.

The exact size differs between languages because writing systems and explanation lengths differ.

## Decision Drivers

The selected design must:

- Download only the content required by the user
- Preserve the original German question and answer wording
- Include one selected translation and its AI-generated explanation
- Work offline after the initial download
- Support all 460 questions
- Allow the selected state to change without another content download
- Avoid partially installed content
- Support validation before activation
- Preserve immutable releases
- Support rollback to a previous release
- Keep Android and iOS content behaviour consistent
- Avoid unnecessary Firestore reads and network transfer
- Avoid duplicating binary question images
- Remain understandable and maintainable

## Considered Options

### Option 1: Keep All Languages in Every Chunk

Each question continues to contain German and all 20 translations.

Advantages:

- No backend migration
- Simple existing uploader
- One set of 46 chunks

Disadvantages:

- Every user downloads every language
- Approximately 91 percent of transferred question data may be unnecessary
- Slower initial setup
- Higher device storage usage
- Higher network usage
- Does not satisfy the selected-language download requirement

This option is rejected.

### Option 2: Separate German Base Chunks and Translation Chunks

German source content is stored once. Every selected translation is stored in a separate package.

A client downloads:

- 46 German chunks
- 46 selected-language chunks

Advantages:

- German source content is not duplicated in Firestore
- Translation packages remain small

Disadvantages:

- Approximately 92 document reads are required for the initial installation
- The client must join two independently downloaded datasets
- Missing or mismatched chunks can produce incomplete content
- Atomic validation and activation are more complicated
- Android and iOS require additional merge logic

This option is rejected for the first release.

### Option 3: Self-Contained Language Packages

Every language package contains:

- The original German source content
- One selected study language
- The selected language’s AI-generated explanation
- Question metadata
- Correct-answer identifiers
- References to common image assets

A user selecting Turkish downloads only the Turkish package. That package already contains the original German content required by the application.

Advantages:

- Only one package must be downloaded and validated
- Approximately 46 chunk reads are required
- No client-side dataset join is required
- The package is independently usable offline
- Atomic local import is simpler
- Android and iOS use the same package contract
- Rollback and validation are straightforward

Disadvantages:

- German text and question metadata are duplicated across language packages
- A content correction may require regenerating every affected package
- Firestore stores more total data than the separate-base approach

The duplicated text is small compared with the operational simplicity and reduced client complexity.

This option is accepted.

## Decision

A new immutable `schema-3` release will use self-contained language packages.

Each package will include all 460 questions so the user can change the selected federal state without downloading another package.

The application will download only the package for the selected study language.

For example, the Turkish package will contain:

- German original question text
- German original answer options
- Turkish question translation
- Turkish answer translations
- Turkish AI-generated explanation
- Correct-answer identifier
- General or state scope
- State code
- Stable question identifier
- References to required common image assets

## Firestore Structure

The proposed Firestore structure is:

```text
contentManifest/current

contentReleases/{releaseId}

contentReleases/{releaseId}/languagePacks/{languageCode}

contentReleases/{releaseId}/languagePacks/{languageCode}/chunks/{chunkId}
```

Example:

```text
contentReleases/lid-2026-10-xx-schema-3
└── languagePacks
    ├── de
    │   └── chunks
    │       ├── 0000
    │       └── ...
    ├── en
    │   └── chunks
    │       ├── 0000
    │       └── ...
    └── tr
        └── chunks
            ├── 0000
            └── ...
```

Each package will normally contain 46 chunks with a maximum of 10 questions per chunk.

The final uploader must also enforce a safe maximum document size below Firestore’s document limit.

## Active Manifest

`contentManifest/current` identifies the active release.

It must contain at least:

- Active release identifier
- Schema version
- Dataset version
- Supported language codes
- Question count
- Package strategy
- Updated timestamp
- Release-level catalog hash

Example package strategy:

```text
SELF_CONTAINED_LANGUAGE_PACK
```

The active manifest must be updated only after the complete release has passed validation.

## Release Metadata

`contentReleases/{releaseId}` must contain:

- Release identifier
- Schema version
- Dataset version
- Total question count
- Supported languages
- Package strategy
- Source catalog hash
- Creation timestamp
- Release status

A release can become active only when its status is `READY`.

Published releases are immutable.

Corrections require a new release identifier.

## Language Package Metadata

`contentReleases/{releaseId}/languagePacks/{languageCode}` must contain:

- Language code
- Schema version
- Question count
- Chunk count
- Package hash
- Approximate package byte size
- Content readiness status
- Explanation readiness status
- Text direction
- Creation timestamp
- Package status

A package can be used only when its status is `READY`.

## Chunk Contract

Each chunk document must contain:

- Chunk index
- Language code
- First question identifier
- Last question identifier
- Question count
- Questions array
- Canonical questions hash
- Approximate JSON byte size

Every question in a non-German package must contain only:

- Common metadata
- German source content
- The package language’s study content
- Image references

It must not contain localizations for any other language.

## German Package

The German package follows the same client-facing structure as the other language packages.

It contains:

- German original questions and answers
- German AI-generated explanations
- German study content
- Common metadata
- Image references

German AI-generated explanations must be clearly marked as supporting content. They are not authoritative source wording.

Using a consistent package contract reduces platform-specific branching in Android and iOS.

## Image Assets

The current catalog contains references to 100 unique question or answer images:

- 24 question-image references
- 76 answer-image references

The binary image data must not be duplicated inside every language package.

Language packages contain only image metadata:

- Storage path
- SHA-256 hash
- MIME type
- Width
- Height

Image files remain common, language-independent assets.

The client must:

1. Discover required image references while installing a package.
2. Download missing images.
3. Verify their hashes.
4. Store them in a local offline cache.
5. Mark installation complete only when required content and images are valid.

An already verified image must not be downloaded again for another language package.

Image licensing and distribution rights must be verified before production release.

## UI Translations

Question-language packages and application-interface translations are separate concerns.

The mobile applications should bundle the required interface strings for the supported languages so the language-selection and download screens can be displayed before the question package is installed.

The language-package download supplies question content and explanations, not the application’s complete interface implementation.

## Client Installation Flow

The client installation process is:

1. Read `contentManifest/current`.
2. Verify that the client supports the active schema version.
3. Read metadata for the selected language package.
4. Download the package chunks.
5. Verify chunk indexes and language codes.
6. Verify individual chunk hashes.
7. Verify the final package hash.
8. Verify the expected question count.
9. Verify the stable question identifier set.
10. Verify that required German and selected-language fields exist.
11. Reject placeholder or missing explanations.
12. Download and validate missing image assets.
13. Import content into temporary local tables.
14. Activate the new content in one local transaction.
15. Remove temporary installation data.

The currently active local package remains available until the new package has been validated and activated successfully.

## Language Change Flow

When a user changes language:

1. Keep the current language active.
2. Check whether the requested package is installed.
3. Require an internet connection if it is not installed.
4. Download and validate the requested package.
5. Import the package atomically.
6. Switch the active language only after successful installation.

A failed installation must not create a partially translated interface or remove the previous valid content.

Offline switching to a language that has not been installed is not required in the first release.

## State Change Flow

All 460 questions are included in the selected language package.

Changing the selected state does not require another download.

The client filters the installed package into:

- 300 general questions
- 10 questions belonging to the selected state
- 310 active questions

Progress remains connected to stable question identifiers.

## Release Publication

The administrative publication process must:

1. Validate the source catalog.
2. Generate all 21 language packages.
3. Verify that every package contains exactly 460 questions.
4. Verify that every package contains the same stable question identifiers.
5. Validate translations and explanations.
6. Reject placeholder explanations.
7. Validate referenced image metadata.
8. Upload missing common image assets.
9. Upload package chunks and metadata.
10. Upload release metadata.
11. Mark the release as `READY`.
12. Update `contentManifest/current` as the final operation.

The publication process must support dry-run mode.

It must require explicit project and activation confirmation.

## Content Readiness

The current `schema-2` content cannot be copied blindly into a production `schema-3` release.

The inspection found:

- All 460 German explanations are empty.
- 229 English explanations are generic placeholders.
- 229 Turkish explanations are generic placeholders.
- Similar placeholder explanations may exist in other languages.

The migration tool must identify these values and prevent production activation until the content satisfies the explanation requirements.

The existing translations can be reorganized without being generated again. Only missing or inadequate explanations require new generation and review.

## Compatibility

Clients must check the schema version before downloading a package.

A client that does not support the active schema must display an update-required state instead of attempting to import unknown content.

The current `schema-2` release must not be deleted.

It remains available for:

- Rollback
- Audit
- Migration verification
- Historical traceability

## Security

Mobile clients may read active published content.

Mobile clients must not be allowed to:

- Create releases
- Change release status
- Upload package chunks
- Activate a manifest
- Change translations
- Delete published content

Only trusted administrative tooling using protected credentials may publish releases.

Service-account credentials and OpenAI API keys must never be included in mobile applications or committed to the repository.

## Consequences

### Positive Consequences

- Initial question download becomes substantially smaller.
- Users download only the language they need.
- Each package can be validated independently.
- Android and iOS use the same content contract.
- State changes continue to work offline.
- Client implementation avoids cross-package joins.
- Existing release rollback remains possible.
- Content releases remain immutable and auditable.

### Negative Consequences

- German content is duplicated across language packages.
- The uploader and validators must support package generation.
- A full release contains 21 sets of package metadata and chunks.
- Every production release requires validation of all supported packages.
- Missing explanations must be completed before production activation.

### Accepted Trade-Off

A small increase in backend storage is accepted in exchange for:

- Lower user download size
- Fewer client reads than the separate-base approach
- Simpler atomic installation
- Lower mobile implementation complexity
- More reliable offline behaviour

## Alternatives for Future Evaluation

If catalog size or Firestore read volume grows significantly, language-package payloads may be moved to compressed immutable files in Firebase Storage while keeping release and package metadata in Firestore.

That change is not required for the first release and would require a separate architecture decision record.
