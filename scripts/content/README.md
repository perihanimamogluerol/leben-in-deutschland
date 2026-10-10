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
