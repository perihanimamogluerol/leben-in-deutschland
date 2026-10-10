#!/usr/bin/env python3
"""Prepare, submit, download, and apply OpenAI explanation batches safely.

The source catalog is never edited in place. Network access is isolated to the
submit, status, and download commands; prepare and apply are deterministic local
operations that can be tested without the OpenAI SDK.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from build_schema3 import BuildError, get_questions, inspect_source, read_json, sha256_json


PIPELINE_VERSION = 1
ENDPOINT = "/v1/responses"
GERMAN_STAGE = "german"
TRANSLATION_STAGE = "translations"
GERMAN_PROMPT_VERSION = "de-explanation-v1"
TRANSLATION_PROMPT_VERSION = "explanation-translation-v1"
DEFAULT_GERMAN_MODEL = "gpt-6.1-sol"
DEFAULT_TRANSLATION_MODEL = "gpt-6-luna"
QUALITY_CODES_TO_REPLACE = {
    "MISSING_EXPLANATION",
    "REPEATED_PLACEHOLDER_EXPLANATION",
}

LANGUAGE_NAMES = {
    "en": "English",
    "tr": "Turkish",
    "uk": "Ukrainian",
    "ar": "Arabic",
    "ro": "Romanian",
    "pl": "Polish",
    "ru": "Russian",
    "bg": "Bulgarian",
    "hr": "Croatian",
    "bs": "Bosnian",
    "sr-Latn": "Serbian written only in the Latin alphabet",
    "el": "Greek",
    "sq": "Albanian",
    "fa": "Persian (Farsi/Dari), using broadly understandable wording",
    "ps": "Pashto",
    "ku": "Kurdish (Kurmanji)",
    "it": "Italian",
    "es": "Spanish",
    "hu": "Hungarian",
    "fr": "French",
}

GERMAN_INSTRUCTIONS = """You create concise study explanations for the German
Leben in Deutschland and naturalization-test question catalog. Use only the
provided question, answer options, and authoritative correct answer. Explain in
clear German why the supplied answer is correct. Write one to three sentences,
normally no more than 70 words. Do not use Markdown. Do not claim that the
explanation is official. Do not change the question or answer. Do not invent
dates, names, laws, or visual details that are not supported by the input."""

TRANSLATION_INSTRUCTIONS = """Translate the supplied German study explanation
faithfully into the requested target language. Preserve meaning, names, legal
terms, and factual scope. Use natural, accessible language for learners. Do not
add or remove facts. Do not use Markdown. Return only the required structured
data."""


class PipelineError(RuntimeError):
    """Raised when an explanation pipeline operation is unsafe or invalid."""


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def clean_text(value: Any) -> str | None:
    if not isinstance(value, str):
        return None
    cleaned = " ".join(value.split())
    return cleaned or None


def ensure_writable(path: Path, force: bool) -> None:
    if path.exists() and not force:
        raise PipelineError(f"Output already exists: {path}. Use --force to replace it.")
    path.parent.mkdir(parents=True, exist_ok=True)


def ensure_distinct(*paths: Path) -> None:
    resolved = [path.resolve() for path in paths]
    if len(resolved) != len(set(resolved)):
        raise PipelineError("Input, output, manifest, and state paths must be distinct.")


def atomic_write_text(path: Path, text: str, *, force: bool = False) -> None:
    ensure_writable(path, force)
    temporary = path.with_name(f"{path.name}.tmp")
    with temporary.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write(text)
    temporary.replace(path)


def atomic_write_json(path: Path, value: Any, *, force: bool = False) -> None:
    text = json.dumps(value, ensure_ascii=False, indent=2) + "\n"
    atomic_write_text(path, text, force=force)


def default_manifest_path(request_path: Path) -> Path:
    return request_path.with_suffix(".manifest.json")


def question_by_id(catalog: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {question["id"]: question for question in get_questions(catalog)}


def authoritative_answer(question: dict[str, Any]) -> dict[str, Any]:
    correct_id = question.get("correctAnswerId")
    german = question.get("german")
    if not isinstance(german, dict):
        raise PipelineError(f"Question {question.get('id')} has no German content.")
    answers = german.get("answers")
    if not isinstance(answers, list):
        raise PipelineError(f"Question {question.get('id')} has no German answers.")
    for answer in answers:
        if isinstance(answer, dict) and answer.get("id") == correct_id:
            return answer
    raise PipelineError(f"Question {question.get('id')} has no authoritative answer.")


def image_description(value: Any) -> str | None:
    if not isinstance(value, dict):
        return None
    for key in ("altText", "description", "label", "title"):
        text = clean_text(value.get(key))
        if text:
            return text
    return None


def german_target_payload(question: dict[str, Any]) -> tuple[dict[str, Any] | None, str | None]:
    german = question["german"]
    correct = authoritative_answer(question)
    correct_text = clean_text(correct.get("text")) or image_description(correct.get("image"))
    if not correct_text:
        return None, "The correct answer is image-only and has no textual description."

    answers: list[dict[str, str]] = []
    for answer in german.get("answers", []):
        if not isinstance(answer, dict):
            continue
        answer_text = clean_text(answer.get("text")) or image_description(answer.get("image"))
        answers.append(
            {
                "id": str(answer.get("id", "")),
                "text": answer_text or "[image answer without description]",
            }
        )

    return (
        {
            "questionId": question["id"],
            "scope": question.get("scope"),
            "stateCode": question.get("stateCode"),
            "question": german.get("questionText"),
            "answers": answers,
            "correctAnswerId": question.get("correctAnswerId"),
            "correctAnswerText": correct_text,
        },
        None,
    )


def response_schema(stage: str) -> dict[str, Any]:
    properties: dict[str, Any] = {
        "questionId": {"type": "string"},
        "explanation": {"type": "string"},
    }
    required = ["questionId", "explanation"]
    if stage == TRANSLATION_STAGE:
        properties["languageCode"] = {"type": "string"}
        required.insert(1, "languageCode")
    return {
        "type": "object",
        "properties": properties,
        "required": required,
        "additionalProperties": False,
    }


def batch_request(
    *,
    custom_id: str,
    model: str,
    instructions: str,
    payload: dict[str, Any],
    stage: str,
) -> dict[str, Any]:
    return {
        "custom_id": custom_id,
        "method": "POST",
        "url": ENDPOINT,
        "body": {
            "model": model,
            "store": False,
            "instructions": instructions,
            "input": json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
            "reasoning": {"effort": "low"},
            "text": {
                "format": {
                    "type": "json_schema",
                    "name": "lid_explanation",
                    "description": "A study explanation mapped to its catalog target.",
                    "strict": True,
                    "schema": response_schema(stage),
                }
            },
            "max_output_tokens": 800,
        },
    }


def target_issues(catalog: dict[str, Any]) -> list[dict[str, Any]]:
    structural, quality = inspect_source(catalog)
    if structural:
        raise PipelineError(
            f"Catalog has {len(structural)} structural issue(s). First issue: {structural[0]}"
        )
    return [item for item in quality if item.get("code") in QUALITY_CODES_TO_REPLACE]


def write_requests_and_manifest(
    *,
    catalog: dict[str, Any],
    request_path: Path,
    manifest_path: Path,
    requests: list[dict[str, Any]],
    targets: list[dict[str, str]],
    blocked: list[dict[str, str]],
    stage: str,
    model: str,
    prompt_version: str,
    force: bool,
) -> dict[str, Any]:
    ensure_distinct(request_path, manifest_path)
    ensure_writable(request_path, force)
    ensure_writable(manifest_path, force)
    request_text = "".join(
        json.dumps(item, ensure_ascii=False, separators=(",", ":")) + "\n"
        for item in requests
    )
    atomic_write_text(request_path, request_text, force=force)
    manifest = {
        "pipelineVersion": PIPELINE_VERSION,
        "stage": stage,
        "promptVersion": prompt_version,
        "model": model,
        "endpoint": ENDPOINT,
        "sourceCatalogSha256": sha256_json(catalog),
        "requestFile": request_path.name,
        "requestFileSha256": sha256_file(request_path),
        "requestCount": len(requests),
        "targetCount": len(targets) + len(blocked),
        "readyForSubmit": bool(requests) and not blocked,
        "targets": targets,
        "blockedTargets": blocked,
        "createdAt": utc_now(),
    }
    atomic_write_json(manifest_path, manifest, force=force)
    return {
        "stage": stage,
        "model": model,
        "requestCount": len(requests),
        "blockedTargetCount": len(blocked),
        "readyForSubmit": manifest["readyForSubmit"],
        "requests": str(request_path),
        "manifest": str(manifest_path),
    }


def prepare_german(
    catalog: dict[str, Any],
    request_path: Path,
    manifest_path: Path,
    *,
    model: str = DEFAULT_GERMAN_MODEL,
    force: bool = False,
) -> dict[str, Any]:
    issues = target_issues(catalog)
    target_ids = {
        item["questionId"]
        for item in issues
        if item.get("languageCode") == "de" and isinstance(item.get("questionId"), str)
    }
    requests: list[dict[str, Any]] = []
    targets: list[dict[str, str]] = []
    blocked: list[dict[str, str]] = []

    for question in get_questions(catalog):
        question_id = question["id"]
        if question_id not in target_ids:
            continue
        custom_id = f"german--{question_id}"
        payload, reason = german_target_payload(question)
        if payload is None:
            blocked.append(
                {
                    "customId": custom_id,
                    "questionId": question_id,
                    "languageCode": "de",
                    "reason": reason or "Unknown input limitation.",
                }
            )
            continue
        requests.append(
            batch_request(
                custom_id=custom_id,
                model=model,
                instructions=GERMAN_INSTRUCTIONS,
                payload=payload,
                stage=GERMAN_STAGE,
            )
        )
        targets.append(
            {"customId": custom_id, "questionId": question_id, "languageCode": "de"}
        )

    return write_requests_and_manifest(
        catalog=catalog,
        request_path=request_path,
        manifest_path=manifest_path,
        requests=requests,
        targets=targets,
        blocked=blocked,
        stage=GERMAN_STAGE,
        model=model,
        prompt_version=GERMAN_PROMPT_VERSION,
        force=force,
    )


def prepare_translations(
    catalog: dict[str, Any],
    request_path: Path,
    manifest_path: Path,
    *,
    model: str = DEFAULT_TRANSLATION_MODEL,
    force: bool = False,
) -> dict[str, Any]:
    issues = target_issues(catalog)
    targets_to_replace = {
        (item.get("languageCode"), item.get("questionId"))
        for item in issues
        if item.get("languageCode") != "de"
    }
    requests: list[dict[str, Any]] = []
    targets: list[dict[str, str]] = []
    blocked: list[dict[str, str]] = []

    for question in get_questions(catalog):
        question_id = question["id"]
        german_explanation = clean_text(question["german"].get("explanation"))
        for language_code in catalog["catalog"]["includedContentLanguages"]:
            if language_code == "de" or (language_code, question_id) not in targets_to_replace:
                continue
            custom_id = f"translation--{language_code}--{question_id}"
            target_name = LANGUAGE_NAMES.get(language_code)
            localized = question.get("localizations", {}).get(language_code)
            if not german_explanation:
                reason = "German explanation must be completed before translation."
            elif not target_name:
                reason = "Target language is not configured in the pipeline."
            elif not isinstance(localized, dict):
                reason = "Localized question content is missing."
            else:
                reason = None
            if reason:
                blocked.append(
                    {
                        "customId": custom_id,
                        "questionId": question_id,
                        "languageCode": language_code,
                        "reason": reason,
                    }
                )
                continue

            payload = {
                "questionId": question_id,
                "languageCode": language_code,
                "targetLanguage": target_name,
                "germanExplanation": german_explanation,
                "localizedQuestion": localized.get("questionText"),
            }
            requests.append(
                batch_request(
                    custom_id=custom_id,
                    model=model,
                    instructions=TRANSLATION_INSTRUCTIONS,
                    payload=payload,
                    stage=TRANSLATION_STAGE,
                )
            )
            targets.append(
                {
                    "customId": custom_id,
                    "questionId": question_id,
                    "languageCode": language_code,
                }
            )

    return write_requests_and_manifest(
        catalog=catalog,
        request_path=request_path,
        manifest_path=manifest_path,
        requests=requests,
        targets=targets,
        blocked=blocked,
        stage=TRANSLATION_STAGE,
        model=model,
        prompt_version=TRANSLATION_PROMPT_VERSION,
        force=force,
    )


def read_manifest(path: Path) -> dict[str, Any]:
    value = read_json(path)
    if value.get("pipelineVersion") != PIPELINE_VERSION:
        raise PipelineError("Unsupported explanation-pipeline manifest version.")
    if value.get("stage") not in {GERMAN_STAGE, TRANSLATION_STAGE}:
        raise PipelineError("Manifest has an invalid stage.")
    return value


def require_openai() -> Any:
    if not os.environ.get("OPENAI_API_KEY"):
        raise PipelineError("OPENAI_API_KEY is not configured in this terminal session.")
    try:
        from openai import OpenAI
    except ImportError as exc:
        raise PipelineError(
            "The OpenAI SDK is not installed. Activate the local virtual environment first."
        ) from exc
    return OpenAI


def submit_batch(
    request_path: Path,
    manifest_path: Path,
    state_path: Path,
    *,
    confirmation: str,
    force: bool = False,
) -> dict[str, Any]:
    if confirmation != "SUBMIT":
        raise PipelineError("Refusing API submission without --confirm-submit SUBMIT.")
    ensure_distinct(request_path, manifest_path, state_path)
    manifest = read_manifest(manifest_path)
    if not manifest.get("readyForSubmit"):
        raise PipelineError("Manifest is not ready for submission; resolve blocked targets first.")
    if sha256_file(request_path) != manifest.get("requestFileSha256"):
        raise PipelineError("Request JSONL hash does not match its manifest.")
    line_count = sum(1 for line in request_path.read_text(encoding="utf-8").splitlines() if line)
    if line_count != manifest.get("requestCount"):
        raise PipelineError("Request JSONL count does not match its manifest.")
    ensure_writable(state_path, force)

    OpenAI = require_openai()
    client = OpenAI()
    with request_path.open("rb") as handle:
        uploaded = client.files.create(file=handle, purpose="batch")
    batch = client.batches.create(
        input_file_id=uploaded.id,
        endpoint=ENDPOINT,
        completion_window="24h",
        metadata={
            "pipeline": "lid-explanations",
            "stage": str(manifest["stage"]),
            "prompt_version": str(manifest["promptVersion"]),
        },
    )
    state = {
        "pipelineVersion": PIPELINE_VERSION,
        "stage": manifest["stage"],
        "model": manifest["model"],
        "manifest": str(manifest_path),
        "requestFile": str(request_path),
        "inputFileId": uploaded.id,
        "batchId": batch.id,
        "status": batch.status,
        "submittedAt": utc_now(),
    }
    atomic_write_json(state_path, state, force=force)
    return {**state, "stateFile": str(state_path)}


def update_status(state_path: Path) -> dict[str, Any]:
    state = read_json(state_path)
    batch_id = state.get("batchId")
    if not isinstance(batch_id, str) or not batch_id:
        raise PipelineError("Batch state has no batchId.")
    OpenAI = require_openai()
    batch = OpenAI().batches.retrieve(batch_id)
    state.update(
        {
            "status": batch.status,
            "outputFileId": batch.output_file_id,
            "errorFileId": batch.error_file_id,
            "requestCounts": (
                batch.request_counts.model_dump()
                if batch.request_counts is not None
                else None
            ),
            "checkedAt": utc_now(),
        }
    )
    atomic_write_json(state_path, state, force=True)
    return state


def download_batch(state_path: Path, output_path: Path, *, force: bool = False) -> dict[str, Any]:
    ensure_distinct(state_path, output_path)
    state = update_status(state_path)
    if state.get("status") != "completed":
        raise PipelineError(f"Batch is not completed. Current status: {state.get('status')}")
    output_file_id = state.get("outputFileId")
    if not isinstance(output_file_id, str) or not output_file_id:
        raise PipelineError("Completed batch has no output file.")
    OpenAI = require_openai()
    content = OpenAI().files.content(output_file_id)
    atomic_write_text(output_path, content.text, force=force)
    return {
        "batchId": state["batchId"],
        "status": state["status"],
        "output": str(output_path),
        "sha256": sha256_file(output_path),
    }


def output_text(body: dict[str, Any]) -> str:
    pieces: list[str] = []
    for item in body.get("output", []):
        if not isinstance(item, dict) or item.get("type") != "message":
            continue
        for content in item.get("content", []):
            if not isinstance(content, dict):
                continue
            if content.get("type") == "refusal":
                raise PipelineError(f"Model refused a request: {content.get('refusal')}")
            if content.get("type") == "output_text" and isinstance(content.get("text"), str):
                pieces.append(content["text"])
    if not pieces:
        raise PipelineError("Batch response has no output text.")
    return "".join(pieces)


def load_batch_output(path: Path) -> dict[str, dict[str, Any]]:
    results: dict[str, dict[str, Any]] = {}
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        if not line.strip():
            continue
        try:
            item = json.loads(line)
        except json.JSONDecodeError as exc:
            raise PipelineError(f"Invalid JSONL at output line {line_number}: {exc}") from exc
        custom_id = item.get("custom_id")
        if not isinstance(custom_id, str) or not custom_id:
            raise PipelineError(f"Output line {line_number} has no custom_id.")
        if custom_id in results:
            raise PipelineError(f"Duplicate output custom_id: {custom_id}")
        if item.get("error") is not None:
            raise PipelineError(f"Batch request {custom_id} failed: {item['error']}")
        response = item.get("response")
        if not isinstance(response, dict) or response.get("status_code") != 200:
            raise PipelineError(f"Batch request {custom_id} did not return HTTP 200.")
        body = response.get("body")
        if not isinstance(body, dict):
            raise PipelineError(f"Batch request {custom_id} has no response body.")
        try:
            parsed = json.loads(output_text(body))
        except json.JSONDecodeError as exc:
            raise PipelineError(f"Batch request {custom_id} returned invalid JSON.") from exc
        if not isinstance(parsed, dict):
            raise PipelineError(f"Batch request {custom_id} output must be an object.")
        results[custom_id] = parsed
    return results


def validate_explanation(value: Any, custom_id: str) -> str:
    explanation = clean_text(value)
    if not explanation or len(explanation) < 20:
        raise PipelineError(f"Explanation is too short for {custom_id}.")
    if len(explanation) > 1_200:
        raise PipelineError(f"Explanation is too long for {custom_id}.")
    return explanation


def apply_results(
    catalog: dict[str, Any],
    manifest: dict[str, Any],
    batch_output_path: Path,
) -> tuple[dict[str, Any], dict[str, Any]]:
    if sha256_json(catalog) != manifest.get("sourceCatalogSha256"):
        raise PipelineError("Source catalog hash does not match the batch manifest.")
    targets = manifest.get("targets")
    if not isinstance(targets, list):
        raise PipelineError("Manifest targets are missing.")
    expected = {
        item["customId"]: item
        for item in targets
        if isinstance(item, dict) and isinstance(item.get("customId"), str)
    }
    results = load_batch_output(batch_output_path)
    missing = sorted(set(expected) - set(results))
    unexpected = sorted(set(results) - set(expected))
    if missing or unexpected:
        raise PipelineError(
            f"Batch output target mismatch. Missing: {len(missing)}; unexpected: {len(unexpected)}."
        )

    updated = copy.deepcopy(catalog)
    questions = question_by_id(updated)
    stage = manifest["stage"]
    for custom_id, target in expected.items():
        result = results[custom_id]
        question_id = target["questionId"]
        language_code = target["languageCode"]
        if result.get("questionId") != question_id:
            raise PipelineError(f"Question ID mismatch for {custom_id}.")
        if stage == TRANSLATION_STAGE and result.get("languageCode") != language_code:
            raise PipelineError(f"Language code mismatch for {custom_id}.")
        explanation = validate_explanation(result.get("explanation"), custom_id)
        question = questions.get(question_id)
        if question is None:
            raise PipelineError(f"Question no longer exists: {question_id}")
        if stage == GERMAN_STAGE:
            question["german"]["explanation"] = explanation
            question["german"]["explanationStatus"] = "AI_GENERATED_NEEDS_REVIEW"
        else:
            localized = question["localizations"][language_code]
            localized["explanation"] = explanation
            localized["explanationStatus"] = "AI_GENERATED_NEEDS_REVIEW"

    return updated, {
        "stage": stage,
        "appliedCount": len(expected),
        "model": manifest["model"],
        "promptVersion": manifest["promptVersion"],
    }


def apply_batch(
    input_path: Path,
    manifest_path: Path,
    batch_output_path: Path,
    output_path: Path,
    *,
    force: bool = False,
) -> dict[str, Any]:
    ensure_distinct(input_path, manifest_path, batch_output_path, output_path)
    ensure_writable(output_path, force)
    catalog = read_json(input_path)
    manifest = read_manifest(manifest_path)
    updated, summary = apply_results(catalog, manifest, batch_output_path)
    atomic_write_json(output_path, updated, force=force)
    return {**summary, "output": str(output_path), "catalogSha256": sha256_json(updated)}


def add_prepare_arguments(parser: argparse.ArgumentParser, default_model: str) -> None:
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--manifest", type=Path)
    parser.add_argument("--model", default=default_model)
    parser.add_argument("--force", action="store_true")


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Manage AI explanation Batch API jobs safely.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    prepare_de = subparsers.add_parser("prepare-german")
    add_prepare_arguments(prepare_de, DEFAULT_GERMAN_MODEL)

    prepare_tr = subparsers.add_parser("prepare-translations")
    add_prepare_arguments(prepare_tr, DEFAULT_TRANSLATION_MODEL)

    submit = subparsers.add_parser("submit")
    submit.add_argument("--input", required=True, type=Path)
    submit.add_argument("--manifest", required=True, type=Path)
    submit.add_argument("--state", required=True, type=Path)
    submit.add_argument("--confirm-submit", required=True)
    submit.add_argument("--force", action="store_true")

    status = subparsers.add_parser("status")
    status.add_argument("--state", required=True, type=Path)

    download = subparsers.add_parser("download")
    download.add_argument("--state", required=True, type=Path)
    download.add_argument("--output", required=True, type=Path)
    download.add_argument("--force", action="store_true")

    apply_parser = subparsers.add_parser("apply")
    apply_parser.add_argument("--input", required=True, type=Path)
    apply_parser.add_argument("--manifest", required=True, type=Path)
    apply_parser.add_argument("--batch-output", required=True, type=Path)
    apply_parser.add_argument("--output", required=True, type=Path)
    apply_parser.add_argument("--force", action="store_true")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    try:
        if args.command in {"prepare-german", "prepare-translations"}:
            catalog = read_json(args.input)
            manifest_path = args.manifest or default_manifest_path(args.output)
            if args.command == "prepare-german":
                result = prepare_german(
                    catalog,
                    args.output,
                    manifest_path,
                    model=args.model,
                    force=args.force,
                )
            else:
                result = prepare_translations(
                    catalog,
                    args.output,
                    manifest_path,
                    model=args.model,
                    force=args.force,
                )
        elif args.command == "submit":
            result = submit_batch(
                args.input,
                args.manifest,
                args.state,
                confirmation=args.confirm_submit,
                force=args.force,
            )
        elif args.command == "status":
            result = update_status(args.state)
        elif args.command == "download":
            result = download_batch(args.state, args.output, force=args.force)
        else:
            result = apply_batch(
                args.input,
                args.manifest,
                args.batch_output,
                args.output,
                force=args.force,
            )
    except (BuildError, PipelineError, OSError, ValueError) as exc:
        print(json.dumps({"ok": False, "error": str(exc)}, ensure_ascii=False, indent=2))
        return 1

    print(json.dumps({"ok": True, **result}, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
