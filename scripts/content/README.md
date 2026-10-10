# Schema 3 Content Pipeline

This directory contains dependency-free Python tooling for converting the
existing schema-2 catalog into self-contained schema-3 language packages.

## Safety

- Source catalogs and generated releases belong under `.local/`.
- `.local/` must remain ignored by Git.
- Production builds reject missing or repeated placeholder explanations.
- `--allow-incomplete` and `--allow-draft` are for local inspection only.
- These scripts do not connect to Firebase and cannot activate a release.

## Run the Tests

From the repository root:

```bash
python3 -m unittest discover \
  -s scripts/content/tests \
  -p "test_*.py" \
  -v
```

## Build the Current Catalog as a Draft

The current schema-2 catalog contains incomplete explanations, so the first
build must be explicitly marked as incomplete:

```bash
python3 scripts/content/build_schema3.py \
  --input .local/content/schema-2.json \
  --output .local/generated/schema-3 \
  --allow-incomplete
```

The command writes:

```text
.local/generated/schema-3/
├── release.json
├── build-report.json
└── language-packs/
    └── {languageCode}/
        ├── package.json
        └── chunks/
            ├── 0000.json
            └── ...
```

## Validate a Draft Build

```bash
python3 scripts/content/validate_schema3.py \
  --input .local/generated/schema-3 \
  --allow-draft
```

Draft validation confirms structural integrity but returns
`readyForUpload: false` while quality issues remain.

## Production Validation

Do not use draft flags for production:

```bash
python3 scripts/content/build_schema3.py \
  --input .local/content/schema-2.json \
  --output .local/generated/schema-3

python3 scripts/content/validate_schema3.py \
  --input .local/generated/schema-3
```

A production build succeeds only when every package is structurally valid and
all required explanations pass the readiness checks.

## Complete AI Explanations Safely

`explanation_pipeline.py` prepares and applies OpenAI Batch API jobs without
editing the source catalog in place. Request files, manifests, batch state, and
generated catalogs must remain under `.local/`.

The workflow deliberately has two stages:

1. Generate missing German explanations from the authoritative German question
   and correct answer.
2. Translate those German explanations only into localization fields that are
   missing or contain a repeated placeholder.

The prepare and apply commands are local and make no API calls. Submission
requires both `OPENAI_API_KEY` and the explicit confirmation value `SUBMIT`.

Create and activate an ignored local virtual environment:

```bash
python3 -m venv .local/venv
source .local/venv/bin/activate
python -m pip install -r scripts/content/requirements-openai.txt
```

Prepare the German request file:

```bash
python scripts/content/explanation_pipeline.py prepare-german \
  --input .local/content/schema-2.json \
  --output .local/explanations/german-requests.jsonl
```

Review the summary before submitting. `readyForSubmit` must be `true` and
`blockedTargetCount` must be `0`. Image-only correct answers without textual
descriptions are blocked instead of allowing the model to invent visual facts.

Submit only after review:

```bash
python scripts/content/explanation_pipeline.py submit \
  --input .local/explanations/german-requests.jsonl \
  --manifest .local/explanations/german-requests.manifest.json \
  --state .local/explanations/german-batch.json \
  --confirm-submit SUBMIT
```

Check status and download a completed batch:

```bash
python scripts/content/explanation_pipeline.py status \
  --state .local/explanations/german-batch.json

python scripts/content/explanation_pipeline.py download \
  --state .local/explanations/german-batch.json \
  --output .local/explanations/german-output.jsonl
```

Apply results to a new catalog after the batch completes:

```bash
python scripts/content/explanation_pipeline.py apply \
  --input .local/content/schema-2.json \
  --manifest .local/explanations/german-requests.manifest.json \
  --batch-output .local/explanations/german-output.jsonl \
  --output .local/content/schema-2-with-german-explanations.json
```

Prepare translations from the completed German explanations:

```bash
python scripts/content/explanation_pipeline.py prepare-translations \
  --input .local/content/schema-2-with-german-explanations.json \
  --output .local/explanations/translation-requests.jsonl
```

Use the same submit, status, download, and apply commands for the translation
manifest. After applying translations, rebuild schema 3 without
`--allow-incomplete` and run production validation. AI-generated explanations
remain marked as needing human review even when structural validation passes.
