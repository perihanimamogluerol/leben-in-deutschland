#!/usr/bin/env python3
"""Validate a generated schema-3 release before upload or activation."""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

from build_schema3 import (
    PACKAGE_STRATEGY,
    REPEATED_EXPLANATION_LIMIT,
    SCHEMA_VERSION,
    json_size,
    read_json,
    sha256_json,
)


class ValidationError(RuntimeError):
    """Raised when generated content is structurally invalid."""


def add_issue(
    target: list[dict[str, Any]],
    code: str,
    message: str,
    *,
    language_code: str | None = None,
    question_id: str | None = None,
) -> None:
    value: dict[str, Any] = {"code": code, "message": message}
    if language_code is not None:
        value["languageCode"] = language_code
    if question_id is not None:
        value["questionId"] = question_id
    target.append(value)


def validate_question(
    question: Any,
    language_code: str,
    structural: list[dict[str, Any]],
    quality: list[dict[str, Any]],
) -> None:
    if not isinstance(question, dict):
        add_issue(structural, "INVALID_QUESTION", "Question must be an object.")
        return

    question_id = question.get("id")
    if not isinstance(question_id, str) or not question_id:
        add_issue(structural, "MISSING_QUESTION_ID", "Stable question ID is missing.")
        return

    if "localizations" in question:
        add_issue(
            structural,
            "UNEXPECTED_LOCALIZATIONS",
            "Schema-3 questions must not include all localizations.",
            language_code=language_code,
            question_id=question_id,
        )

    german = question.get("german")
    study = question.get("study")
    if not isinstance(german, dict) or not isinstance(study, dict):
        add_issue(
            structural,
            "MISSING_CONTENT",
            "Question must contain german and study objects.",
            language_code=language_code,
            question_id=question_id,
        )
        return

    if study.get("languageCode") != language_code:
        add_issue(
            structural,
            "LANGUAGE_MISMATCH",
            "Study content language does not match its package.",
            language_code=language_code,
            question_id=question_id,
        )

    german_answers = german.get("answers")
    study_answers = study.get("answers")
    if not isinstance(german_answers, list) or not isinstance(study_answers, list):
        add_issue(
            structural,
            "INVALID_ANSWERS",
            "German and study answers must be arrays.",
            language_code=language_code,
            question_id=question_id,
        )
        return

    german_ids = [answer.get("id") for answer in german_answers if isinstance(answer, dict)]
    study_ids = [answer.get("id") for answer in study_answers if isinstance(answer, dict)]
    if german_ids != study_ids or len(german_ids) != 4:
        add_issue(
            structural,
            "ANSWER_ID_MISMATCH",
            "German and study answer IDs must match and contain four answers.",
            language_code=language_code,
            question_id=question_id,
        )

    if question.get("correctAnswerId") not in german_ids:
        add_issue(
            structural,
            "INVALID_CORRECT_ANSWER",
            "Correct answer ID is not present in German answers.",
            language_code=language_code,
            question_id=question_id,
        )

    explanation = study.get("explanation")
    if not isinstance(explanation, str) or not explanation.strip():
        add_issue(
            quality,
            "MISSING_EXPLANATION",
            "AI explanation is missing.",
            language_code=language_code,
            question_id=question_id,
        )


def validate_release(
    output_dir: Path,
    *,
    allow_draft: bool = False,
    expected_question_count: int = 460,
) -> dict[str, Any]:
    structural: list[dict[str, Any]] = []
    quality: list[dict[str, Any]] = []
    release_path = output_dir / "release.json"
    if not release_path.exists():
        raise ValidationError(f"Missing release metadata: {release_path}")

    release = read_json(release_path)
    if release.get("schemaVersion") != SCHEMA_VERSION:
        add_issue(structural, "INVALID_SCHEMA", "Release schemaVersion must be 3.")
    if release.get("packageStrategy") != PACKAGE_STRATEGY:
        add_issue(
            structural,
            "INVALID_PACKAGE_STRATEGY",
            f"Release packageStrategy must be {PACKAGE_STRATEGY}.",
        )

    languages = release.get("languages")
    if not isinstance(languages, list) or not languages:
        add_issue(structural, "INVALID_LANGUAGES", "Release languages are missing.")
        languages = []

    if release.get("questionCount") != expected_question_count:
        add_issue(
            structural,
            "QUESTION_COUNT_MISMATCH",
            (
                f"Expected {expected_question_count} questions, "
                f"found {release.get('questionCount')}."
            ),
        )

    reference_ids: set[str] | None = None
    observed_package_hashes: dict[str, str] = {}

    for language_code in languages:
        package_dir = output_dir / "language-packs" / language_code
        package_path = package_dir / "package.json"
        if not package_path.exists():
            add_issue(
                structural,
                "MISSING_PACKAGE",
                "Language package metadata is missing.",
                language_code=language_code,
            )
            continue

        package = read_json(package_path)
        if package.get("languageCode") != language_code:
            add_issue(
                structural,
                "PACKAGE_LANGUAGE_MISMATCH",
                "Package language code is incorrect.",
                language_code=language_code,
            )

        chunk_count = package.get("chunkCount")
        if not isinstance(chunk_count, int) or chunk_count <= 0:
            add_issue(
                structural,
                "INVALID_CHUNK_COUNT",
                "Package chunkCount must be a positive integer.",
                language_code=language_code,
            )
            continue

        package_questions: list[dict[str, Any]] = []
        package_ids: set[str] = set()
        explanations: list[tuple[str, str]] = []

        for chunk_index in range(chunk_count):
            chunk_path = package_dir / "chunks" / f"{chunk_index:04d}.json"
            if not chunk_path.exists():
                add_issue(
                    structural,
                    "MISSING_CHUNK",
                    f"Chunk {chunk_index:04d} is missing.",
                    language_code=language_code,
                )
                continue

            chunk = read_json(chunk_path)
            if chunk.get("index") != chunk_index:
                add_issue(
                    structural,
                    "CHUNK_INDEX_MISMATCH",
                    f"Chunk {chunk_index:04d} contains the wrong index.",
                    language_code=language_code,
                )
            if chunk.get("languageCode") != language_code:
                add_issue(
                    structural,
                    "CHUNK_LANGUAGE_MISMATCH",
                    f"Chunk {chunk_index:04d} contains the wrong language.",
                    language_code=language_code,
                )

            questions = chunk.get("questions")
            if not isinstance(questions, list):
                add_issue(
                    structural,
                    "INVALID_CHUNK_QUESTIONS",
                    f"Chunk {chunk_index:04d} questions must be an array.",
                    language_code=language_code,
                )
                continue

            if chunk.get("questionCount") != len(questions):
                add_issue(
                    structural,
                    "CHUNK_QUESTION_COUNT_MISMATCH",
                    f"Chunk {chunk_index:04d} question count is incorrect.",
                    language_code=language_code,
                )

            if chunk.get("sha256") != sha256_json(questions):
                add_issue(
                    structural,
                    "CHUNK_HASH_MISMATCH",
                    f"Chunk {chunk_index:04d} hash is incorrect.",
                    language_code=language_code,
                )

            chunk_without_size = dict(chunk)
            observed_size = chunk_without_size.pop("jsonBytes", None)
            if observed_size != json_size(chunk_without_size):
                add_issue(
                    structural,
                    "CHUNK_SIZE_MISMATCH",
                    f"Chunk {chunk_index:04d} jsonBytes is incorrect.",
                    language_code=language_code,
                )

            for question in questions:
                validate_question(question, language_code, structural, quality)
                if isinstance(question, dict) and isinstance(question.get("id"), str):
                    question_id = question["id"]
                    if question_id in package_ids:
                        add_issue(
                            structural,
                            "DUPLICATE_PACKAGE_QUESTION",
                            "Question ID is duplicated in the package.",
                            language_code=language_code,
                            question_id=question_id,
                        )
                    package_ids.add(question_id)
                    study = question.get("study")
                    if isinstance(study, dict):
                        explanation = study.get("explanation")
                        if isinstance(explanation, str) and explanation.strip():
                            explanations.append((question_id, explanation.strip()))
                if isinstance(question, dict):
                    package_questions.append(question)

        counts = Counter(text for _, text in explanations)
        repeated = {
            text: count
            for text, count in counts.items()
            if count > REPEATED_EXPLANATION_LIMIT
        }
        for question_id, text in explanations:
            if text in repeated:
                add_issue(
                    quality,
                    "REPEATED_PLACEHOLDER_EXPLANATION",
                    (
                        "The same explanation appears "
                        f"{repeated[text]} times and is treated as a placeholder."
                    ),
                    language_code=language_code,
                    question_id=question_id,
                )

        if len(package_questions) != expected_question_count:
            add_issue(
                structural,
                "PACKAGE_QUESTION_COUNT_MISMATCH",
                (
                    f"Expected {expected_question_count} questions, "
                    f"found {len(package_questions)}."
                ),
                language_code=language_code,
            )

        package_hash = sha256_json(package_questions)
        observed_package_hashes[language_code] = package_hash
        if package.get("packageSha256") != package_hash:
            add_issue(
                structural,
                "PACKAGE_HASH_MISMATCH",
                "Package hash is incorrect.",
                language_code=language_code,
            )

        if reference_ids is None:
            reference_ids = package_ids
        elif package_ids != reference_ids:
            add_issue(
                structural,
                "PACKAGE_ID_SET_MISMATCH",
                "Language packages do not contain the same question IDs.",
                language_code=language_code,
            )

    if release.get("packageHashes") != observed_package_hashes:
        add_issue(
            structural,
            "RELEASE_PACKAGE_HASH_MISMATCH",
            "Release package hash map does not match generated packages.",
        )

    if release.get("catalogSha256") != sha256_json(observed_package_hashes):
        add_issue(
            structural,
            "RELEASE_HASH_MISMATCH",
            "Release catalog hash is incorrect.",
        )

    if structural:
        raise ValidationError(
            f"Release has {len(structural)} structural issue(s). "
            f"First issue: {structural[0]}"
        )

    if quality and not allow_draft:
        raise ValidationError(
            f"Release has {len(quality)} content-quality issue(s). "
            "Use --allow-draft only for local inspection."
        )

    if release.get("status") != "READY" and not allow_draft:
        raise ValidationError("Production validation requires release status READY.")

    return {
        "valid": True,
        "releaseId": release.get("releaseId"),
        "schemaVersion": release.get("schemaVersion"),
        "status": release.get("status"),
        "questionCount": release.get("questionCount"),
        "languageCount": len(languages),
        "qualityIssueCount": len(quality),
        "readyForUpload": not quality and release.get("status") == "READY",
    }


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate a schema-3 build.")
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--expected-question-count", type=int, default=460)
    parser.add_argument(
        "--allow-draft",
        action="store_true",
        help="Accept missing or placeholder explanations for local inspection.",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    try:
        result = validate_release(
            args.input,
            allow_draft=args.allow_draft,
            expected_question_count=args.expected_question_count,
        )
    except (ValidationError, RuntimeError) as exc:
        print(json.dumps({"valid": False, "error": str(exc)}, indent=2))
        return 1

    print(json.dumps(result, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
