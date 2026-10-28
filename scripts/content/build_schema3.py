#!/usr/bin/env python3
"""Build immutable schema-3 language packages from a schema-2 catalog."""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import re
import shutil
import sys
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


SCHEMA_VERSION = 3
PACKAGE_STRATEGY = "SELF_CONTAINED_LANGUAGE_PACK"
DEFAULT_CHUNK_SIZE = 10
MAX_CHUNK_BYTES = 800_000
REPEATED_EXPLANATION_LIMIT = 20
ALLOWED_EXPLANATION_STATUSES = {
    "AI_GENERATED_NEEDS_REVIEW",
    "HUMAN_REVIEWED",
}
ALLOWED_MEMORY_KEY_STATUSES = ALLOWED_EXPLANATION_STATUSES
MAX_MEMORY_KEY_LENGTH = 80
MAX_MEMORY_KEY_WORDS = 8


class BuildError(RuntimeError):
    """Raised when source content cannot produce a valid release."""


def canonical_bytes(value: Any) -> bytes:
    return json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")


def sha256_json(value: Any) -> str:
    return hashlib.sha256(canonical_bytes(value)).hexdigest()


def json_size(value: Any) -> int:
    return len(canonical_bytes(value))


def read_json(path: Path) -> dict[str, Any]:
    try:
        with path.open("r", encoding="utf-8") as handle:
            value = json.load(handle)
    except FileNotFoundError as exc:
        raise BuildError(f"Input file does not exist: {path}") from exc
    except json.JSONDecodeError as exc:
        raise BuildError(f"Input is not valid JSON: {path}: {exc}") from exc

    if not isinstance(value, dict):
        raise BuildError("Catalog root must be a JSON object.")
    return value


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(value, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def safe_identifier(value: str) -> str:
    safe = re.sub(r"[^A-Za-z0-9_-]+", "-", value).strip("-")
    if not safe:
        raise BuildError("Dataset version cannot produce a valid release identifier.")
    return safe


def get_languages(catalog: dict[str, Any]) -> list[str]:
    metadata = catalog.get("catalog")
    if not isinstance(metadata, dict):
        raise BuildError("Missing catalog metadata.")

    languages = metadata.get("includedContentLanguages")
    if not isinstance(languages, list) or not languages:
        raise BuildError("catalog.includedContentLanguages must be a non-empty array.")

    if any(not isinstance(code, str) or not code for code in languages):
        raise BuildError("Every included language code must be a non-empty string.")

    if len(languages) != len(set(languages)):
        raise BuildError("Included language codes must be unique.")

    if "de" not in languages:
        raise BuildError("The German source language 'de' is required.")

    return languages


def get_questions(catalog: dict[str, Any]) -> list[dict[str, Any]]:
    questions = catalog.get("questions")
    if not isinstance(questions, list) or not questions:
        raise BuildError("questions must be a non-empty array.")
    if any(not isinstance(question, dict) for question in questions):
        raise BuildError("Every question must be a JSON object.")
    return questions


def answer_ids(answers: Any) -> list[str]:
    if not isinstance(answers, list):
        return []
    result: list[str] = []
    for answer in answers:
        if isinstance(answer, dict) and isinstance(answer.get("id"), str):
            result.append(answer["id"])
    return result


def normalized_memory_key_pair(question_cue: str, answer_cue: str) -> str:
    return "|".join(
        re.sub(r"[^\wäöüß]+", " ", value.casefold()).strip()
        for value in (question_cue, answer_cue)
    )


def validate_memory_cue(value: Any) -> bool:
    if not isinstance(value, str) or not value.strip():
        return False
    cue = value.strip()
    words = re.findall(r"[\wÄÖÜäöüß-]+", cue)
    return (
        len(cue) <= MAX_MEMORY_KEY_LENGTH
        and 1 <= len(words) <= MAX_MEMORY_KEY_WORDS
        and "→" not in cue
        and "->" not in cue
        and "\n" not in cue
        and "\r" not in cue
    )


def issue(
    code: str,
    message: str,
    *,
    language_code: str | None = None,
    question_id: str | None = None,
) -> dict[str, Any]:
    value: dict[str, Any] = {"code": code, "message": message}
    if language_code is not None:
        value["languageCode"] = language_code
    if question_id is not None:
        value["questionId"] = question_id
    return value


def inspect_source(
    catalog: dict[str, Any],
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    """Return structural issues and content-quality issues."""

    languages = get_languages(catalog)
    questions = get_questions(catalog)
    structural: list[dict[str, Any]] = []
    quality: list[dict[str, Any]] = []
    seen_ids: set[str] = set()
    explanations: dict[str, list[tuple[str, str]]] = defaultdict(list)
    memory_key_enabled = any(
        isinstance(question.get("german"), dict)
        and question["german"].get("memoryKey") is not None
        for question in questions
    )
    memory_key_pairs: dict[str, str] = {}

    for index, question in enumerate(questions):
        question_id = question.get("id")
        if not isinstance(question_id, str) or not question_id:
            structural.append(
                issue(
                    "MISSING_QUESTION_ID",
                    f"Question at index {index} has no stable identifier.",
                )
            )
            continue

        if question_id in seen_ids:
            structural.append(
                issue(
                    "DUPLICATE_QUESTION_ID",
                    "Question identifier is duplicated.",
                    question_id=question_id,
                )
            )
        seen_ids.add(question_id)

        german = question.get("german")
        if not isinstance(german, dict):
            structural.append(
                issue(
                    "MISSING_GERMAN_CONTENT",
                    "German source content is missing.",
                    question_id=question_id,
                )
            )
            continue

        if not isinstance(german.get("questionText"), str) or not german.get(
            "questionText", ""
        ).strip():
            structural.append(
                issue(
                    "MISSING_GERMAN_QUESTION",
                    "German question text is missing.",
                    question_id=question_id,
                )
            )

        german_answer_ids = answer_ids(german.get("answers"))
        if len(german_answer_ids) != 4 or len(german_answer_ids) != len(
            set(german_answer_ids)
        ):
            structural.append(
                issue(
                    "INVALID_GERMAN_ANSWERS",
                    "German content must contain four uniquely identified answers.",
                    question_id=question_id,
                )
            )

        correct_answer_id = question.get("correctAnswerId")
        if correct_answer_id not in german_answer_ids:
            structural.append(
                issue(
                    "INVALID_CORRECT_ANSWER",
                    "Correct answer identifier is not present in German answers.",
                    question_id=question_id,
                )
            )

        correct_answer = next(
            (
                answer
                for answer in german.get("answers", [])
                if isinstance(answer, dict) and answer.get("id") == correct_answer_id
            ),
            None,
        )
        correct_answer_has_image = (
            isinstance(correct_answer, dict)
            and isinstance(correct_answer.get("image"), dict)
            and bool(correct_answer["image"])
        )
        memory_key = german.get("memoryKey")
        if memory_key is not None:
            if not isinstance(memory_key, dict):
                structural.append(
                    issue(
                        "INVALID_MEMORY_KEY",
                        "German memory key must be an object.",
                        language_code="de",
                        question_id=question_id,
                    )
                )
            elif correct_answer_has_image:
                structural.append(
                    issue(
                        "MEMORY_KEY_FOR_IMAGE_ANSWER",
                        "Image-answer questions must not contain a memory key.",
                        language_code="de",
                        question_id=question_id,
                    )
                )
            else:
                question_cue = memory_key.get("questionCue")
                answer_cue = memory_key.get("answerCue")
                if not validate_memory_cue(question_cue) or not validate_memory_cue(
                    answer_cue
                ):
                    structural.append(
                        issue(
                            "INVALID_MEMORY_KEY_CUE",
                            "Memory-key cues must contain 1-8 words and at most 80 characters.",
                            language_code="de",
                            question_id=question_id,
                        )
                    )
                else:
                    pair = normalized_memory_key_pair(question_cue, answer_cue)
                    previous = memory_key_pairs.get(pair)
                    if previous is not None:
                        structural.append(
                            issue(
                                "DUPLICATE_MEMORY_KEY_PAIR",
                                f"Memory-key pair duplicates {previous}.",
                                language_code="de",
                                question_id=question_id,
                            )
                        )
                    memory_key_pairs[pair] = question_id
                if memory_key.get("status") not in ALLOWED_MEMORY_KEY_STATUSES:
                    quality.append(
                        issue(
                            "UNVERIFIED_MEMORY_KEY_PROVENANCE",
                            "German memory-key provenance is not verified.",
                            language_code="de",
                            question_id=question_id,
                        )
                    )
        elif memory_key_enabled and not correct_answer_has_image:
            quality.append(
                issue(
                    "MISSING_MEMORY_KEY",
                    "German memory key is missing for a text-answer question.",
                    language_code="de",
                    question_id=question_id,
                )
            )

        german_explanation = german.get("explanation")
        if not isinstance(german_explanation, str) or not german_explanation.strip():
            quality.append(
                issue(
                    "MISSING_EXPLANATION",
                    "German AI explanation is missing.",
                    language_code="de",
                    question_id=question_id,
                )
            )
        else:
            explanations["de"].append((question_id, german_explanation.strip()))
            if german.get("explanationStatus") not in ALLOWED_EXPLANATION_STATUSES:
                quality.append(
                    issue(
                        "UNVERIFIED_EXPLANATION_PROVENANCE",
                        "German explanation provenance is not verified.",
                        language_code="de",
                        question_id=question_id,
                    )
                )

        localizations = question.get("localizations")
        if not isinstance(localizations, dict):
            structural.append(
                issue(
                    "MISSING_LOCALIZATIONS",
                    "Question localizations object is missing.",
                    question_id=question_id,
                )
            )
            continue

        for language_code in languages:
            if language_code == "de":
                continue

            localized = localizations.get(language_code)
            if not isinstance(localized, dict):
                structural.append(
                    issue(
                        "MISSING_LOCALIZATION",
                        "Required localization is missing.",
                        language_code=language_code,
                        question_id=question_id,
                    )
                )
                continue

            if not isinstance(localized.get("questionText"), str) or not localized.get(
                "questionText", ""
            ).strip():
                structural.append(
                    issue(
                        "MISSING_LOCALIZED_QUESTION",
                        "Localized question text is missing.",
                        language_code=language_code,
                        question_id=question_id,
                    )
                )

            localized_answer_ids = answer_ids(localized.get("answers"))
            if localized_answer_ids != german_answer_ids:
                structural.append(
                    issue(
                        "LOCALIZED_ANSWER_MISMATCH",
                        "Localized answer identifiers do not match German answers.",
                        language_code=language_code,
                        question_id=question_id,
                    )
                )

            explanation = localized.get("explanation")
            if not isinstance(explanation, str) or not explanation.strip():
                quality.append(
                    issue(
                        "MISSING_EXPLANATION",
                        "Localized AI explanation is missing.",
                        language_code=language_code,
                        question_id=question_id,
                    )
                )
            else:
                explanations[language_code].append(
                    (question_id, explanation.strip())
                )
                if (
                    localized.get("explanationStatus")
                    not in ALLOWED_EXPLANATION_STATUSES
                ):
                    quality.append(
                        issue(
                            "UNVERIFIED_EXPLANATION_PROVENANCE",
                            "Localized explanation provenance is not verified.",
                            language_code=language_code,
                            question_id=question_id,
                        )
                    )

    for language_code, values in explanations.items():
        counts = Counter(text for _, text in values)
        repeated_values = {
            text: count
            for text, count in counts.items()
            if count > REPEATED_EXPLANATION_LIMIT
        }
        for question_id, text in values:
            repeated_count = repeated_values.get(text)
            if repeated_count is not None:
                quality.append(
                    issue(
                        "REPEATED_PLACEHOLDER_EXPLANATION",
                        (
                            "The same explanation appears "
                            f"{repeated_count} times and is treated as a placeholder."
                        ),
                        language_code=language_code,
                        question_id=question_id,
                    )
                )

    return structural, quality


def german_source(question: dict[str, Any]) -> dict[str, Any]:
    german = question["german"]
    result = {
        "questionText": german.get("questionText"),
        "questionImage": copy.deepcopy(german.get("questionImage")),
        "answers": copy.deepcopy(german.get("answers")),
    }
    if "memoryKey" in german:
        result["memoryKey"] = copy.deepcopy(german["memoryKey"])
    return result


def study_content(
    question: dict[str, Any], language_code: str
) -> dict[str, Any]:
    if language_code == "de":
        german = question["german"]
        return {
            "languageCode": "de",
            "questionText": german.get("questionText"),
            "explanation": german.get("explanation"),
            "answers": [
                {"id": answer.get("id"), "text": answer.get("text")}
                for answer in german.get("answers", [])
            ],
            "textStatus": "SOURCE",
            "explanationStatus": german.get("explanationStatus"),
            "reviewed": False,
        }

    localized = question["localizations"][language_code]
    result = {
        "languageCode": language_code,
        "questionText": localized.get("questionText"),
        "explanation": localized.get("explanation"),
        "answers": copy.deepcopy(localized.get("answers")),
        "translationStatus": localized.get("translationStatus"),
        "explanationStatus": localized.get("explanationStatus"),
        "reviewed": bool(localized.get("reviewed", False)),
    }
    if "audit" in localized:
        result["audit"] = copy.deepcopy(localized["audit"])
    return result


def transform_question(
    question: dict[str, Any], language_code: str
) -> dict[str, Any]:
    metadata_keys = (
        "sourceId",
        "officialNumber",
        "scope",
        "stateCode",
        "category",
        "categoryStatus",
        "sourceVersion",
        "sourcePdfSha256",
        "source",
    )
    metadata = {
        key: copy.deepcopy(question.get(key))
        for key in metadata_keys
        if key in question
    }
    return {
        "id": question["id"],
        "metadata": metadata,
        "correctAnswerId": question.get("correctAnswerId"),
        "german": german_source(question),
        "study": study_content(question, language_code),
    }


def build_chunks(
    questions: list[dict[str, Any]],
    language_code: str,
    chunk_size: int,
) -> list[dict[str, Any]]:
    chunks: list[dict[str, Any]] = []
    for start in range(0, len(questions), chunk_size):
        items = questions[start : start + chunk_size]
        chunk: dict[str, Any] = {
            "index": len(chunks),
            "languageCode": language_code,
            "firstQuestionId": items[0]["id"],
            "lastQuestionId": items[-1]["id"],
            "questionCount": len(items),
            "questions": items,
        }
        chunk["sha256"] = sha256_json(chunk["questions"])
        chunk["jsonBytes"] = json_size(chunk)
        if chunk["jsonBytes"] > MAX_CHUNK_BYTES:
            raise BuildError(
                f"Chunk {language_code}/{chunk['index']:04d} is too large: "
                f"{chunk['jsonBytes']} bytes."
            )
        chunks.append(chunk)
    return chunks


def prepare_output_directory(output_dir: Path, force: bool) -> Path:
    if output_dir.exists():
        if not force:
            raise BuildError(
                f"Output directory already exists: {output_dir}. Use --force to replace it."
            )
        if output_dir.resolve() in {Path("/").resolve(), Path.home().resolve()}:
            raise BuildError("Refusing to replace an unsafe output directory.")

    temporary = output_dir.with_name(f"{output_dir.name}.tmp")
    if temporary.exists():
        if temporary.resolve() in {Path("/").resolve(), Path.home().resolve()}:
            raise BuildError("Refusing to clean an unsafe temporary directory.")
        shutil.rmtree(temporary)
    temporary.mkdir(parents=True, exist_ok=False)
    return temporary


def build_release(
    catalog: dict[str, Any],
    output_dir: Path,
    *,
    allow_incomplete: bool = False,
    force: bool = False,
    chunk_size: int = DEFAULT_CHUNK_SIZE,
    dataset_version: str | None = None,
) -> dict[str, Any]:
    if chunk_size <= 0:
        raise BuildError("Chunk size must be greater than zero.")

    languages = get_languages(catalog)
    questions = get_questions(catalog)
    structural_issues, quality_issues = inspect_source(catalog)

    if structural_issues:
        raise BuildError(
            f"Source catalog has {len(structural_issues)} structural issue(s). "
            f"First issue: {structural_issues[0]}"
        )

    if quality_issues and not allow_incomplete:
        raise BuildError(
            f"Source catalog has {len(quality_issues)} content-quality issue(s). "
            "Use --allow-incomplete only for a non-production DRAFT build."
        )

    metadata = catalog["catalog"]
    effective_version = dataset_version or metadata.get("datasetVersion")
    if not isinstance(effective_version, str) or not effective_version:
        raise BuildError("A dataset version is required.")

    release_id = f"lid-{safe_identifier(effective_version)}-schema-{SCHEMA_VERSION}"
    release_status = "READY" if not quality_issues else "DRAFT"
    source_hash = sha256_json(catalog)
    memory_key_count = sum(
        1
        for question in questions
        if isinstance(question["german"].get("memoryKey"), dict)
    )
    generated_at = utc_now()
    temporary = prepare_output_directory(output_dir, force)
    package_hashes: dict[str, str] = {}
    package_summaries: dict[str, dict[str, Any]] = {}

    issues_by_language: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for item in quality_issues:
        code = item.get("languageCode")
        if isinstance(code, str):
            issues_by_language[code].append(item)

    try:
        for language_code in languages:
            transformed = [
                transform_question(question, language_code) for question in questions
            ]
            chunks = build_chunks(transformed, language_code, chunk_size)
            package_hash = sha256_json(transformed)
            package_hashes[language_code] = package_hash
            language_issues = issues_by_language.get(language_code, [])
            package_status = "READY" if not language_issues else "DRAFT"
            package_dir = temporary / "language-packs" / language_code

            for chunk in chunks:
                write_json(
                    package_dir / "chunks" / f"{chunk['index']:04d}.json",
                    chunk,
                )

            package_metadata = {
                "releaseId": release_id,
                "schemaVersion": SCHEMA_VERSION,
                "packageStrategy": PACKAGE_STRATEGY,
                "languageCode": language_code,
                "questionCount": len(transformed),
                "chunkCount": len(chunks),
                "chunkSize": chunk_size,
                "packageSha256": package_hash,
                "jsonBytes": json_size(transformed),
                "qualityIssueCount": len(language_issues),
                "explanationReady": not language_issues,
                "memoryKeyCount": memory_key_count,
                "status": package_status,
                "createdAt": generated_at,
            }
            write_json(package_dir / "package.json", package_metadata)
            package_summaries[language_code] = package_metadata

        release_hash = sha256_json(package_hashes)
        release_metadata = {
            "releaseId": release_id,
            "schemaVersion": SCHEMA_VERSION,
            "datasetVersion": effective_version,
            "packageStrategy": PACKAGE_STRATEGY,
            "questionCount": len(questions),
            "languages": languages,
            "languageCount": len(languages),
            "chunkSize": chunk_size,
            "sourceCatalogSha256": source_hash,
            "catalogSha256": release_hash,
            "packageHashes": package_hashes,
            "qualityIssueCount": len(quality_issues),
            "memoryKeyCount": memory_key_count,
            "status": release_status,
            "createdAt": generated_at,
        }
        write_json(temporary / "release.json", release_metadata)

        report = {
            "releaseId": release_id,
            "schemaVersion": SCHEMA_VERSION,
            "status": release_status,
            "questionCount": len(questions),
            "languageCount": len(languages),
            "qualityIssueCount": len(quality_issues),
            "memoryKeyCount": memory_key_count,
            "qualityIssues": quality_issues,
            "packages": package_summaries,
        }
        write_json(temporary / "build-report.json", report)

        if output_dir.exists():
            shutil.rmtree(output_dir)
        temporary.rename(output_dir)
    except Exception:
        if temporary.exists():
            shutil.rmtree(temporary)
        raise

    return {
        "releaseId": release_id,
        "schemaVersion": SCHEMA_VERSION,
        "status": release_status,
        "questionCount": len(questions),
        "languageCount": len(languages),
        "qualityIssueCount": len(quality_issues),
        "memoryKeyCount": memory_key_count,
        "output": str(output_dir),
    }


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Build schema-3 self-contained language packages."
    )
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--dataset-version")
    parser.add_argument("--chunk-size", type=int, default=DEFAULT_CHUNK_SIZE)
    parser.add_argument(
        "--allow-incomplete",
        action="store_true",
        help="Create a DRAFT build when explanations are incomplete.",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Replace an existing output directory after a successful build.",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    try:
        catalog = read_json(args.input)
        result = build_release(
            catalog,
            args.output,
            allow_incomplete=args.allow_incomplete,
            force=args.force,
            chunk_size=args.chunk_size,
            dataset_version=args.dataset_version,
        )
    except BuildError as exc:
        print(json.dumps({"ok": False, "error": str(exc)}, indent=2))
        return 1

    print(json.dumps({"ok": True, **result}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
