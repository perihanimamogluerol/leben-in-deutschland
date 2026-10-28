#!/usr/bin/env python3
"""Prepare, submit, download, apply, and validate German memory-key batches."""

from __future__ import annotations

import argparse
import copy
import json
import os
import re
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from threading import Lock
from typing import Any

from build_schema3 import BuildError, get_questions, inspect_source, read_json, sha256_json
from explanation_pipeline import (
    PipelineError as SharedPipelineError,
    atomic_write_json,
    atomic_write_text,
    clean_text,
    ensure_distinct,
    ensure_writable,
    output_text,
    require_openai,
    sha256_file,
    utc_now,
)


PIPELINE_VERSION = 1
PIPELINE_KIND = "memory-keys"
ENDPOINT = "/v1/responses"
PROMPT_VERSION = "de-memory-key-v1"
DEFAULT_MODEL = "gpt-6.1-sol"
STATUS = "AI_GENERATED_NEEDS_REVIEW"
SKIP_REASON = "IMAGE_ANSWER_SKIPPED"
MAX_CUE_LENGTH = 80
MAX_CUE_WORDS = 8
DIRECT_MAX_OUTPUT_TOKENS = 800
DIRECT_CONTENT_ATTEMPTS = 5

INSTRUCTIONS = """Create one concise German memory-key pair for the supplied
Leben in Deutschland question and its authoritative correct answer. The pair
helps a learner recall the answer after answering a practice question.

Rules:
- questionCue must be a distinctive 1-8 word German phrase that identifies the
  decisive part of the question.
- answerCue must be a precise 1-8 word German phrase that recalls only the
  supplied correct answer.
- Preserve decisive negation such as nicht, kein, or keine.
- Prefer wording from the supplied question and correct answer. A shorter
  grammatical phrase is allowed, but do not invent facts.
- Do not use an answer letter, Bild number, arrow, Markdown, quotation marks,
  sentence-ending punctuation, or explanatory prose.
- Avoid vague cues such as Deutschland, Frage, Antwort, or richtig when a more
  distinctive phrase is available.
- Return only the required structured data."""


class MemoryKeyPipelineError(RuntimeError):
    """Raised when a memory-key pipeline operation is unsafe or invalid."""


def default_manifest_path(request_path: Path) -> Path:
    return request_path.with_suffix(".manifest.json")


def authoritative_answer(question: dict[str, Any]) -> dict[str, Any]:
    correct_id = question.get("correctAnswerId")
    german = question.get("german")
    if not isinstance(german, dict):
        raise MemoryKeyPipelineError(
            f"Question {question.get('id')} has no German content."
        )
    answers = german.get("answers")
    if not isinstance(answers, list):
        raise MemoryKeyPipelineError(
            f"Question {question.get('id')} has no German answers."
        )
    matches = [
        answer
        for answer in answers
        if isinstance(answer, dict) and answer.get("id") == correct_id
    ]
    if len(matches) != 1:
        raise MemoryKeyPipelineError(
            f"Question {question.get('id')} has no unique authoritative answer."
        )
    return matches[0]


def has_image(value: Any) -> bool:
    return isinstance(value, dict) and bool(value)


def should_skip(question: dict[str, Any]) -> bool:
    return has_image(authoritative_answer(question).get("image"))


def response_schema() -> dict[str, Any]:
    return {
        "type": "object",
        "properties": {
            "questionId": {"type": "string"},
            "questionCue": {"type": "string"},
            "answerCue": {"type": "string"},
        },
        "required": ["questionId", "questionCue", "answerCue"],
        "additionalProperties": False,
    }


def batch_request(
    question: dict[str, Any],
    *,
    model: str,
) -> dict[str, Any]:
    question_id = question["id"]
    german = question["german"]
    correct = authoritative_answer(question)
    payload = {
        "questionId": question_id,
        "scope": question.get("scope"),
        "stateCode": question.get("stateCode"),
        "question": german.get("questionText"),
        "correctAnswer": correct.get("text"),
        "allAnswers": [
            {
                "id": answer.get("id"),
                "text": answer.get("text"),
            }
            for answer in german.get("answers", [])
            if isinstance(answer, dict)
        ],
        "explanation": german.get("explanation"),
        "hasQuestionImage": has_image(german.get("questionImage")),
    }
    return {
        "custom_id": f"memory-key--{question_id}",
        "method": "POST",
        "url": ENDPOINT,
        "body": {
            "model": model,
            "store": False,
            "instructions": INSTRUCTIONS,
            "input": json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
            "reasoning": {"effort": "low"},
            "text": {
                "format": {
                    "type": "json_schema",
                    "name": "lid_memory_key",
                    "description": "A German mnemonic pair for one catalog question.",
                    "strict": True,
                    "schema": response_schema(),
                }
            },
            "max_output_tokens": 240,
        },
    }


def prepare(
    catalog: dict[str, Any],
    request_path: Path,
    manifest_path: Path,
    *,
    model: str = DEFAULT_MODEL,
    force: bool = False,
) -> dict[str, Any]:
    structural, _ = inspect_source(catalog)
    if structural:
        raise MemoryKeyPipelineError(
            f"Catalog has {len(structural)} structural issue(s). First: {structural[0]}"
        )
    ensure_distinct(request_path, manifest_path)
    ensure_writable(request_path, force)
    ensure_writable(manifest_path, force)

    requests: list[dict[str, Any]] = []
    targets: list[dict[str, str]] = []
    skipped: list[dict[str, str]] = []
    questions = get_questions(catalog)
    for question in questions:
        question_id = question["id"]
        custom_id = f"memory-key--{question_id}"
        if should_skip(question):
            skipped.append({"questionId": question_id, "reason": SKIP_REASON})
            continue
        requests.append(batch_request(question, model=model))
        targets.append({"customId": custom_id, "questionId": question_id})

    if len(requests) + len(skipped) != len(questions):
        raise MemoryKeyPipelineError("Prepared target counts do not cover the catalog.")
    request_text = "".join(
        json.dumps(item, ensure_ascii=False, separators=(",", ":")) + "\n"
        for item in requests
    )
    atomic_write_text(request_path, request_text, force=force)
    manifest = {
        "pipelineVersion": PIPELINE_VERSION,
        "pipelineKind": PIPELINE_KIND,
        "promptVersion": PROMPT_VERSION,
        "model": model,
        "endpoint": ENDPOINT,
        "sourceCatalogSha256": sha256_json(catalog),
        "requestFile": request_path.name,
        "requestFileSha256": sha256_file(request_path),
        "questionCount": len(questions),
        "requestCount": len(requests),
        "skippedCount": len(skipped),
        "readyForSubmit": bool(requests),
        "targets": targets,
        "skippedTargets": skipped,
        "createdAt": utc_now(),
    }
    atomic_write_json(manifest_path, manifest, force=force)
    return {
        "model": model,
        "questionCount": manifest["questionCount"],
        "requestCount": manifest["requestCount"],
        "skippedCount": manifest["skippedCount"],
        "readyForSubmit": manifest["readyForSubmit"],
        "requests": str(request_path),
        "manifest": str(manifest_path),
    }


def read_manifest(path: Path) -> dict[str, Any]:
    manifest = read_json(path)
    if manifest.get("pipelineVersion") != PIPELINE_VERSION:
        raise MemoryKeyPipelineError("Unsupported memory-key manifest version.")
    if manifest.get("pipelineKind") != PIPELINE_KIND:
        raise MemoryKeyPipelineError("Manifest is not a memory-key manifest.")
    return manifest


def submit_batch(
    request_path: Path,
    manifest_path: Path,
    state_path: Path,
    *,
    confirmation: str,
    force: bool = False,
) -> dict[str, Any]:
    if confirmation != "SUBMIT":
        raise MemoryKeyPipelineError(
            "Refusing API submission without --confirm-submit SUBMIT."
        )
    ensure_distinct(request_path, manifest_path, state_path)
    manifest = read_manifest(manifest_path)
    if not manifest.get("readyForSubmit"):
        raise MemoryKeyPipelineError("Manifest is not ready for submission.")
    if sha256_file(request_path) != manifest.get("requestFileSha256"):
        raise MemoryKeyPipelineError("Request JSONL hash does not match its manifest.")
    lines = [line for line in request_path.read_text(encoding="utf-8").splitlines() if line]
    if len(lines) != manifest.get("requestCount"):
        raise MemoryKeyPipelineError("Request JSONL count does not match its manifest.")
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
            "pipeline": "lid-memory-keys",
            "prompt_version": str(manifest["promptVersion"]),
        },
    )
    state = {
        "pipelineVersion": PIPELINE_VERSION,
        "pipelineKind": PIPELINE_KIND,
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
    if state.get("pipelineKind") != PIPELINE_KIND:
        raise MemoryKeyPipelineError("State is not a memory-key batch state.")
    batch_id = state.get("batchId")
    if not isinstance(batch_id, str) or not batch_id:
        raise MemoryKeyPipelineError("Batch state has no batchId.")
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


def cancel_batch(
    state_path: Path,
    *,
    confirmation: str,
) -> dict[str, Any]:
    if confirmation != "CANCEL":
        raise MemoryKeyPipelineError(
            "Refusing batch cancellation without --confirm-cancel CANCEL."
        )
    state = read_json(state_path)
    if state.get("pipelineKind") != PIPELINE_KIND:
        raise MemoryKeyPipelineError("State is not a memory-key batch state.")
    batch_id = state.get("batchId")
    if not isinstance(batch_id, str) or not batch_id:
        raise MemoryKeyPipelineError("Batch state has no batchId.")

    OpenAI = require_openai()
    client = OpenAI()
    batch = client.batches.retrieve(batch_id)
    if batch.status in {"completed", "failed", "expired", "cancelled"}:
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
    else:
        batch = client.batches.cancel(batch_id)
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
                "cancelRequestedAt": utc_now(),
                "checkedAt": utc_now(),
            }
        )
    atomic_write_json(state_path, state, force=True)
    return state


def download_batch(
    state_path: Path,
    output_path: Path,
    *,
    force: bool = False,
) -> dict[str, Any]:
    ensure_distinct(state_path, output_path)
    state = update_status(state_path)
    if state.get("status") != "completed":
        raise MemoryKeyPipelineError(
            f"Batch is not completed. Current status: {state.get('status')}"
        )
    output_file_id = state.get("outputFileId")
    if not isinstance(output_file_id, str) or not output_file_id:
        raise MemoryKeyPipelineError("Completed batch has no output file.")
    OpenAI = require_openai()
    content = OpenAI().files.content(output_file_id)
    atomic_write_text(output_path, content.text, force=force)
    return {
        "batchId": state["batchId"],
        "status": state["status"],
        "output": str(output_path),
        "sha256": sha256_file(output_path),
    }


def parse_output_item(item: Any, line_number: int) -> tuple[str, dict[str, Any]]:
    if not isinstance(item, dict):
        raise MemoryKeyPipelineError(f"Output line {line_number} must be an object.")
    custom_id = item.get("custom_id")
    if not isinstance(custom_id, str) or not custom_id:
        raise MemoryKeyPipelineError(f"Output line {line_number} has no custom_id.")
    if item.get("error") is not None:
        raise MemoryKeyPipelineError(
            f"Batch request {custom_id} failed: {item['error']}"
        )
    response = item.get("response")
    if not isinstance(response, dict) or response.get("status_code") != 200:
        raise MemoryKeyPipelineError(
            f"Batch request {custom_id} did not return HTTP 200."
        )
    body = response.get("body")
    if not isinstance(body, dict):
        raise MemoryKeyPipelineError(
            f"Batch request {custom_id} has no response body."
        )
    try:
        text = output_text(body)
    except SharedPipelineError as exc:
        raise MemoryKeyPipelineError(
            f"Batch request {custom_id} has no output text."
        ) from exc
    try:
        parsed = json.loads(text)
    except json.JSONDecodeError as exc:
        raise MemoryKeyPipelineError(
            f"Batch request {custom_id} returned invalid JSON."
        ) from exc
    if not isinstance(parsed, dict):
        raise MemoryKeyPipelineError(
            f"Batch request {custom_id} output must be an object."
        )
    return custom_id, parsed


def load_batch_output(path: Path) -> dict[str, dict[str, Any]]:
    results: dict[str, dict[str, Any]] = {}
    for line_number, line in enumerate(
        path.read_text(encoding="utf-8").splitlines(), start=1
    ):
        if not line.strip():
            continue
        try:
            item = json.loads(line)
        except json.JSONDecodeError as exc:
            raise MemoryKeyPipelineError(
                f"Invalid JSONL at output line {line_number}: {exc}"
            ) from exc
        custom_id, parsed = parse_output_item(item, line_number)
        if custom_id in results:
            raise MemoryKeyPipelineError(f"Duplicate output custom_id: {custom_id}")
        results[custom_id] = parsed
    return results


def repair_direct_output(path: Path) -> dict[str, Any]:
    if not path.exists():
        return {"keptCount": 0, "rejectedCount": 0, "rejectedOutput": None}
    valid_lines: list[str] = []
    rejected_lines: list[str] = []
    seen: set[str] = set()
    seen_pairs: set[str] = set()
    for line_number, line in enumerate(
        path.read_text(encoding="utf-8").splitlines(), start=1
    ):
        if not line.strip():
            continue
        try:
            item = json.loads(line)
            custom_id, parsed = parse_output_item(item, line_number)
            if custom_id in seen:
                raise MemoryKeyPipelineError(
                    f"Duplicate output custom_id: {custom_id}"
                )
            expected_question_id = custom_id.removeprefix("memory-key--")
            if parsed.get("questionId") != expected_question_id:
                raise MemoryKeyPipelineError(
                    f"Question ID mismatch for {custom_id}."
                )
            question_cue = validate_cue(
                parsed.get("questionCue"), "questionCue", custom_id
            )
            answer_cue = validate_cue(
                parsed.get("answerCue"), "answerCue", custom_id
            )
            pair = normalized_pair(question_cue, answer_cue)
            if pair in seen_pairs:
                raise MemoryKeyPipelineError(
                    f"Duplicate memory-key pair for {custom_id}."
                )
        except (json.JSONDecodeError, MemoryKeyPipelineError):
            rejected_lines.append(line)
        else:
            seen.add(custom_id)
            seen_pairs.add(pair)
            valid_lines.append(line)

    rejected_path: Path | None = None
    if rejected_lines:
        rejected_path = path.with_name(f"{path.stem}.rejected{path.suffix}")
        previous = (
            rejected_path.read_text(encoding="utf-8")
            if rejected_path.exists()
            else ""
        )
        rejected_text = previous + "".join(line + "\n" for line in rejected_lines)
        atomic_write_text(rejected_path, rejected_text, force=True)
        valid_text = "".join(line + "\n" for line in valid_lines)
        atomic_write_text(path, valid_text, force=True)
    return {
        "keptCount": len(valid_lines),
        "rejectedCount": len(rejected_lines),
        "rejectedOutput": str(rejected_path) if rejected_path is not None else None,
    }


def load_direct_requests(
    request_path: Path,
    manifest: dict[str, Any],
) -> list[dict[str, Any]]:
    if sha256_file(request_path) != manifest.get("requestFileSha256"):
        raise MemoryKeyPipelineError("Request JSONL hash does not match its manifest.")
    requests: list[dict[str, Any]] = []
    custom_ids: set[str] = set()
    for line_number, line in enumerate(
        request_path.read_text(encoding="utf-8").splitlines(), start=1
    ):
        if not line.strip():
            continue
        try:
            request = json.loads(line)
        except json.JSONDecodeError as exc:
            raise MemoryKeyPipelineError(
                f"Invalid request JSONL at line {line_number}: {exc}"
            ) from exc
        custom_id = request.get("custom_id")
        if not isinstance(custom_id, str) or not custom_id:
            raise MemoryKeyPipelineError(
                f"Request line {line_number} has no custom_id."
            )
        if custom_id in custom_ids:
            raise MemoryKeyPipelineError(f"Duplicate request custom_id: {custom_id}")
        if request.get("method") != "POST" or request.get("url") != ENDPOINT:
            raise MemoryKeyPipelineError(
                f"Request {custom_id} does not target POST {ENDPOINT}."
            )
        body = request.get("body")
        if not isinstance(body, dict) or body.get("model") != manifest.get("model"):
            raise MemoryKeyPipelineError(
                f"Request {custom_id} has an invalid body or model."
            )
        custom_ids.add(custom_id)
        requests.append(request)
    if len(requests) != manifest.get("requestCount"):
        raise MemoryKeyPipelineError("Request JSONL count does not match its manifest.")
    return requests


def direct_output_line(
    custom_id: str,
    response_id: str,
    response_body: dict[str, Any],
) -> str:
    return json.dumps(
        {
            "id": f"direct-{response_id}",
            "custom_id": custom_id,
            "response": {
                "status_code": 200,
                "request_id": response_id,
                "body": response_body,
            },
            "error": None,
        },
        ensure_ascii=False,
        separators=(",", ":"),
    )


def run_direct(
    request_path: Path,
    manifest_path: Path,
    output_path: Path,
    *,
    batch_state_path: Path | None,
    workers: int,
    confirmation: str,
) -> dict[str, Any]:
    if confirmation != "RUN_DIRECT":
        raise MemoryKeyPipelineError(
            "Refusing direct API spend without --confirm-run RUN_DIRECT."
        )
    if not 1 <= workers <= 10:
        raise MemoryKeyPipelineError("--workers must be between 1 and 10.")
    paths = [request_path, manifest_path, output_path]
    if batch_state_path is not None:
        paths.append(batch_state_path)
    ensure_distinct(*paths)

    if batch_state_path is not None:
        batch_state = update_status(batch_state_path)
        batch_status = batch_state.get("status")
        if batch_status not in {"cancelled", "failed", "expired"}:
            raise MemoryKeyPipelineError(
                "Direct processing requires the previous batch to be cancelled, "
                f"failed, or expired. Current status: {batch_status}"
            )

    manifest = read_manifest(manifest_path)
    requests = load_direct_requests(request_path, manifest)
    repair = repair_direct_output(output_path)
    if repair["rejectedCount"]:
        print(
            f"Repaired direct output: kept {repair['keptCount']}, "
            f"rejected {repair['rejectedCount']} to {repair['rejectedOutput']}",
            file=sys.stderr,
            flush=True,
        )
    completed = load_batch_output(output_path) if output_path.exists() else {}
    occupied_pairs = {
        normalized_pair(
            validate_cue(result.get("questionCue"), "questionCue", custom_id),
            validate_cue(result.get("answerCue"), "answerCue", custom_id),
        )
        for custom_id, result in completed.items()
    }
    pair_lock = Lock()
    expected_ids = {request["custom_id"] for request in requests}
    unexpected = sorted(set(completed) - expected_ids)
    if unexpected:
        raise MemoryKeyPipelineError(
            f"Direct output contains {len(unexpected)} unexpected custom_id value(s)."
        )
    pending = [
        request for request in requests if request["custom_id"] not in completed
    ]
    if not pending:
        return {
            "status": "completed",
            "requestCount": len(requests),
            "completedCount": len(completed),
            "remainingCount": 0,
            "output": str(output_path),
            "repair": repair,
        }

    output_path.parent.mkdir(parents=True, exist_ok=True)
    OpenAI = require_openai()
    client = OpenAI(max_retries=5, timeout=120.0)

    def execute(request: dict[str, Any]) -> tuple[str, str]:
        body = copy.deepcopy(request["body"])
        body["max_output_tokens"] = max(
            int(body.get("max_output_tokens", 0)),
            DIRECT_MAX_OUTPUT_TOKENS,
        )
        expected_question_id = json.loads(body["input"])["questionId"]
        validation_error: Exception | None = None
        previous_invalid_output: dict[str, Any] | None = None
        for content_attempt in range(1, DIRECT_CONTENT_ATTEMPTS + 1):
            attempt_body = copy.deepcopy(body)
            if content_attempt > 1:
                previous_text = json.dumps(
                    previous_invalid_output,
                    ensure_ascii=False,
                    separators=(",", ":"),
                )
                attempt_body["instructions"] = (
                    body["instructions"]
                    + "\nCorrection retry: The previous output below violated a "
                    "length, identity, or uniqueness rule. Create a more distinctive "
                    "alternative. Rewrite each cue using at most 5 words, even when "
                    "paraphrasing is required. Do not repeat the previous pair. Count "
                    "every space-separated token. Preserve the exact questionId and "
                    "return only schema-valid JSON. "
                    f"Previous invalid output: {previous_text}"
                )
            response = client.responses.create(**attempt_body)
            response_body = response.model_dump(mode="json")
            parsed: Any = None
            try:
                parsed = json.loads(output_text(response_body))
                if (
                    not isinstance(parsed, dict)
                    or parsed.get("questionId") != expected_question_id
                ):
                    raise MemoryKeyPipelineError(
                        f"Direct response question ID mismatch for {request['custom_id']}."
                    )
                question_cue = validate_cue(
                    parsed.get("questionCue"), "questionCue", request["custom_id"]
                )
                answer_cue = validate_cue(
                    parsed.get("answerCue"), "answerCue", request["custom_id"]
                )
                pair = normalized_pair(question_cue, answer_cue)
                with pair_lock:
                    if pair in occupied_pairs:
                        raise MemoryKeyPipelineError(
                            f"Duplicate memory-key pair for {request['custom_id']}."
                        )
                    occupied_pairs.add(pair)
            except (
                SharedPipelineError,
                json.JSONDecodeError,
                MemoryKeyPipelineError,
            ) as exc:
                validation_error = exc
                if isinstance(parsed, dict):
                    previous_invalid_output = parsed
                continue
            return request["custom_id"], direct_output_line(
                request["custom_id"],
                response.id,
                response_body,
            )
        raise MemoryKeyPipelineError(
            f"Direct response for {request['custom_id']} remained invalid after "
            f"{DIRECT_CONTENT_ATTEMPTS} attempts: {validation_error}"
        )

    failures: list[tuple[str, str]] = []
    mode = "a" if output_path.exists() else "w"
    with output_path.open(mode, encoding="utf-8") as output_file:
        with ThreadPoolExecutor(max_workers=workers) as executor:
            future_requests = {
                executor.submit(execute, request): request for request in pending
            }
            for future in as_completed(future_requests):
                request = future_requests[future]
                custom_id = request["custom_id"]
                try:
                    completed_id, line = future.result()
                except Exception as exc:  # SDK errors vary by transport and status.
                    failures.append((custom_id, str(exc)))
                else:
                    output_file.write(line + "\n")
                    output_file.flush()
                    os.fsync(output_file.fileno())
                    completed[completed_id] = {}
                print(
                    f"Progress: {len(completed)}/{len(requests)} "
                    f"(failed this run: {len(failures)})",
                    file=sys.stderr,
                    flush=True,
                )

    if failures:
        sample_id, sample_error = failures[0]
        raise MemoryKeyPipelineError(
            f"Direct run completed with {len(failures)} retryable failure(s). "
            f"Re-run the same command to resume. First: {sample_id}: {sample_error}"
        )
    post_repair = repair_direct_output(output_path)
    if post_repair["rejectedCount"]:
        raise MemoryKeyPipelineError(
            f"Generated output still contained {post_repair['rejectedCount']} invalid "
            "or duplicate result(s). Re-run the same command to resume them."
        )
    load_batch_output(output_path)
    return {
        "status": "completed",
        "requestCount": len(requests),
        "completedCount": len(completed),
        "remainingCount": 0,
        "workers": workers,
        "output": str(output_path),
        "sha256": sha256_file(output_path),
        "repair": repair,
    }


def validate_cue(value: Any, label: str, custom_id: str) -> str:
    cue = clean_text(value)
    if not cue:
        raise MemoryKeyPipelineError(f"{label} is blank for {custom_id}.")
    if len(cue) > MAX_CUE_LENGTH:
        raise MemoryKeyPipelineError(f"{label} is too long for {custom_id}.")
    words = re.findall(r"[\wÄÖÜäöüß-]+", cue, flags=re.UNICODE)
    if not 1 <= len(words) <= MAX_CUE_WORDS:
        raise MemoryKeyPipelineError(
            f"{label} must contain 1-{MAX_CUE_WORDS} words for {custom_id}."
        )
    if "→" in cue or "->" in cue or "\n" in cue or "\r" in cue:
        raise MemoryKeyPipelineError(
            f"{label} contains forbidden formatting for {custom_id}."
        )
    return cue.rstrip(".!?;:")


def normalized_pair(question_cue: str, answer_cue: str) -> str:
    return "|".join(
        re.sub(r"[^\wäöüß]+", " ", value.casefold(), flags=re.UNICODE).strip()
        for value in (question_cue, answer_cue)
    )


def validate_catalog(catalog: dict[str, Any]) -> dict[str, Any]:
    structural, _ = inspect_source(catalog)
    if structural:
        raise MemoryKeyPipelineError(
            f"Catalog has {len(structural)} structural issue(s). First: {structural[0]}"
        )
    pairs: dict[str, str] = {}
    generated = 0
    skipped = 0
    questions = get_questions(catalog)
    for question in questions:
        question_id = question["id"]
        memory_key = question["german"].get("memoryKey")
        if should_skip(question):
            if memory_key is not None:
                raise MemoryKeyPipelineError(
                    f"Image-answer question must not have a memory key: {question_id}"
                )
            skipped += 1
            continue
        if not isinstance(memory_key, dict):
            raise MemoryKeyPipelineError(f"Missing memory key: {question_id}")
        question_cue = validate_cue(
            memory_key.get("questionCue"), "questionCue", question_id
        )
        answer_cue = validate_cue(memory_key.get("answerCue"), "answerCue", question_id)
        if memory_key.get("status") not in {
            "AI_GENERATED_NEEDS_REVIEW",
            "HUMAN_REVIEWED",
        }:
            raise MemoryKeyPipelineError(f"Invalid memory-key status: {question_id}")
        pair = normalized_pair(question_cue, answer_cue)
        previous = pairs.get(pair)
        if previous is not None:
            raise MemoryKeyPipelineError(
                f"Duplicate memory-key pair: {previous} and {question_id}"
            )
        pairs[pair] = question_id
        generated += 1
    return {
        "questionCount": len(questions),
        "memoryKeyCount": generated,
        "imageAnswerSkippedCount": skipped,
        "uniquePairCount": len(pairs),
        "valid": True,
    }


def apply_results(
    catalog: dict[str, Any],
    manifest: dict[str, Any],
    batch_output_path: Path,
) -> tuple[dict[str, Any], dict[str, Any]]:
    if sha256_json(catalog) != manifest.get("sourceCatalogSha256"):
        raise MemoryKeyPipelineError(
            "Source catalog hash does not match the batch manifest."
        )
    targets = manifest.get("targets")
    if not isinstance(targets, list):
        raise MemoryKeyPipelineError("Manifest targets are missing.")
    expected = {
        item["customId"]: item
        for item in targets
        if isinstance(item, dict) and isinstance(item.get("customId"), str)
    }
    results = load_batch_output(batch_output_path)
    missing = sorted(set(expected) - set(results))
    unexpected = sorted(set(results) - set(expected))
    if missing or unexpected:
        raise MemoryKeyPipelineError(
            f"Batch output mismatch. Missing: {len(missing)}; unexpected: {len(unexpected)}."
        )

    updated = copy.deepcopy(catalog)
    questions = {question["id"]: question for question in get_questions(updated)}
    for custom_id, target in expected.items():
        result = results[custom_id]
        question_id = target["questionId"]
        if result.get("questionId") != question_id:
            raise MemoryKeyPipelineError(f"Question ID mismatch for {custom_id}.")
        question_cue = validate_cue(
            result.get("questionCue"), "questionCue", custom_id
        )
        answer_cue = validate_cue(result.get("answerCue"), "answerCue", custom_id)
        questions[question_id]["german"]["memoryKey"] = {
            "questionCue": question_cue,
            "answerCue": answer_cue,
            "status": STATUS,
        }

    validation = validate_catalog(updated)
    return updated, {
        "appliedCount": len(expected),
        "skippedCount": manifest.get("skippedCount"),
        "model": manifest["model"],
        "promptVersion": manifest["promptVersion"],
        "validation": validation,
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


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Manage German memory-key batches safely.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    prepare_parser = subparsers.add_parser("prepare")
    prepare_parser.add_argument("--input", required=True, type=Path)
    prepare_parser.add_argument("--output", required=True, type=Path)
    prepare_parser.add_argument("--manifest", type=Path)
    prepare_parser.add_argument("--model", default=DEFAULT_MODEL)
    prepare_parser.add_argument("--force", action="store_true")

    submit = subparsers.add_parser("submit")
    submit.add_argument("--input", required=True, type=Path)
    submit.add_argument("--manifest", required=True, type=Path)
    submit.add_argument("--state", required=True, type=Path)
    submit.add_argument("--confirm-submit", required=True)
    submit.add_argument("--force", action="store_true")

    status = subparsers.add_parser("status")
    status.add_argument("--state", required=True, type=Path)

    cancel = subparsers.add_parser("cancel")
    cancel.add_argument("--state", required=True, type=Path)
    cancel.add_argument("--confirm-cancel", required=True)

    download = subparsers.add_parser("download")
    download.add_argument("--state", required=True, type=Path)
    download.add_argument("--output", required=True, type=Path)
    download.add_argument("--force", action="store_true")

    direct = subparsers.add_parser("run-direct")
    direct.add_argument("--input", required=True, type=Path)
    direct.add_argument("--manifest", required=True, type=Path)
    direct.add_argument("--output", required=True, type=Path)
    direct.add_argument("--batch-state", type=Path)
    direct.add_argument("--workers", type=int, default=5)
    direct.add_argument("--confirm-run", required=True)

    apply_parser = subparsers.add_parser("apply")
    apply_parser.add_argument("--input", required=True, type=Path)
    apply_parser.add_argument("--manifest", required=True, type=Path)
    apply_parser.add_argument("--batch-output", required=True, type=Path)
    apply_parser.add_argument("--output", required=True, type=Path)
    apply_parser.add_argument("--force", action="store_true")

    validate = subparsers.add_parser("validate")
    validate.add_argument("--input", required=True, type=Path)
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    try:
        if args.command == "prepare":
            catalog = read_json(args.input)
            manifest = args.manifest or default_manifest_path(args.output)
            result = prepare(
                catalog,
                args.output,
                manifest,
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
        elif args.command == "cancel":
            result = cancel_batch(
                args.state,
                confirmation=args.confirm_cancel,
            )
        elif args.command == "download":
            result = download_batch(args.state, args.output, force=args.force)
        elif args.command == "run-direct":
            result = run_direct(
                args.input,
                args.manifest,
                args.output,
                batch_state_path=args.batch_state,
                workers=args.workers,
                confirmation=args.confirm_run,
            )
        elif args.command == "apply":
            result = apply_batch(
                args.input,
                args.manifest,
                args.batch_output,
                args.output,
                force=args.force,
            )
        else:
            result = validate_catalog(read_json(args.input))
    except (
        BuildError,
        MemoryKeyPipelineError,
        SharedPipelineError,
        OSError,
        ValueError,
    ) as exc:
        print(json.dumps({"ok": False, "error": str(exc)}, ensure_ascii=False, indent=2))
        return 1

    print(json.dumps({"ok": True, **result}, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
