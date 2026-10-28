#!/usr/bin/env python3

from __future__ import annotations

import copy
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


CONTENT_DIR = Path(__file__).resolve().parents[1]
if str(CONTENT_DIR) not in sys.path:
    sys.path.insert(0, str(CONTENT_DIR))

from build_schema3 import read_json
from memory_key_pipeline import (
    MemoryKeyPipelineError,
    apply_results,
    default_manifest_path,
    direct_output_line,
    load_batch_output,
    prepare,
    read_manifest,
    repair_direct_output,
    run_direct,
    validate_catalog,
)


FIXTURE = Path(__file__).parent / "fixtures" / "schema2_minimal.json"


def fake_batch_line(custom_id: str, payload: dict[str, str]) -> str:
    return json.dumps(
        {
            "id": f"result-{custom_id}",
            "custom_id": custom_id,
            "response": {
                "status_code": 200,
                "request_id": f"request-{custom_id}",
                "body": {
                    "output": [
                        {
                            "type": "message",
                            "role": "assistant",
                            "content": [
                                {
                                    "type": "output_text",
                                    "text": json.dumps(payload, ensure_ascii=False),
                                }
                            ],
                        }
                    ]
                },
            },
            "error": None,
        },
        ensure_ascii=False,
    )


class MemoryKeyPipelineTest(unittest.TestCase):
    def setUp(self) -> None:
        self.catalog = read_json(FIXTURE)
        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def prepare_job(self, catalog: dict | None = None) -> tuple[Path, dict]:
        requests = self.root / "requests.jsonl"
        manifest_path = default_manifest_path(requests)
        prepare(catalog or self.catalog, requests, manifest_path)
        return requests, read_manifest(manifest_path)

    def test_prepares_text_answer_requests_without_network(self) -> None:
        requests, manifest = self.prepare_job()

        self.assertEqual(manifest["requestCount"], 2)
        self.assertEqual(manifest["skippedCount"], 0)
        lines = [json.loads(line) for line in requests.read_text().splitlines()]
        self.assertEqual(len(lines), 2)
        self.assertTrue(all(line["url"] == "/v1/responses" for line in lines))
        self.assertTrue(all(line["body"]["store"] is False for line in lines))

    def test_skips_a_question_when_the_correct_answer_has_an_image(self) -> None:
        catalog = copy.deepcopy(self.catalog)
        catalog["questions"][0]["german"]["answers"][3]["image"] = {
            "sourcePath": "images/answer.png"
        }

        _, manifest = self.prepare_job(catalog)

        self.assertEqual(manifest["requestCount"], 1)
        self.assertEqual(manifest["skippedCount"], 1)
        self.assertEqual(
            manifest["skippedTargets"][0]["reason"],
            "IMAGE_ANSWER_SKIPPED",
        )

    def test_applies_out_of_order_results_and_validates_unique_pairs(self) -> None:
        _, manifest = self.prepare_job()
        output = self.root / "output.jsonl"
        output.write_text(
            "\n".join(
                [
                    fake_batch_line(
                        "memory-key--state_he_001",
                        {
                            "questionId": "state_he_001",
                            "questionCue": "Hessens Hauptstadt",
                            "answerCue": "Wiesbaden",
                        },
                    ),
                    fake_batch_line(
                        "memory-key--general_001",
                        {
                            "questionId": "general_001",
                            "questionCue": "Meinung frei äußern",
                            "answerCue": "Meinungsfreiheit",
                        },
                    ),
                ]
            )
            + "\n",
            encoding="utf-8",
        )

        updated, summary = apply_results(self.catalog, manifest, output)

        self.assertEqual(summary["appliedCount"], 2)
        self.assertEqual(summary["validation"]["uniquePairCount"], 2)
        self.assertEqual(
            updated["questions"][0]["german"]["memoryKey"]["answerCue"],
            "Meinungsfreiheit",
        )
        self.assertNotIn("memoryKey", self.catalog["questions"][0]["german"])

    def test_rejects_duplicate_memory_key_pairs(self) -> None:
        _, manifest = self.prepare_job()
        output = self.root / "duplicates.jsonl"
        lines = []
        for question_id in ("general_001", "state_he_001"):
            lines.append(
                fake_batch_line(
                    f"memory-key--{question_id}",
                    {
                        "questionId": question_id,
                        "questionCue": "gleicher Hinweis",
                        "answerCue": "gleiche Antwort",
                    },
                )
            )
        output.write_text("\n".join(lines) + "\n", encoding="utf-8")

        with self.assertRaises(MemoryKeyPipelineError):
            apply_results(self.catalog, manifest, output)

    def test_direct_output_is_batch_compatible(self) -> None:
        body = {
            "output": [
                {
                    "type": "message",
                    "role": "assistant",
                    "content": [
                        {
                            "type": "output_text",
                            "text": json.dumps(
                                {
                                    "questionId": "general_001",
                                    "questionCue": "Meinung frei äußern",
                                    "answerCue": "Meinungsfreiheit",
                                },
                                ensure_ascii=False,
                            ),
                        }
                    ],
                }
            ]
        }
        output = self.root / "direct.jsonl"
        output.write_text(
            direct_output_line("memory-key--general_001", "response-1", body)
            + "\n",
            encoding="utf-8",
        )

        results = load_batch_output(output)

        self.assertEqual(
            results["memory-key--general_001"]["answerCue"],
            "Meinungsfreiheit",
        )

    def test_direct_run_resumes_existing_successes(self) -> None:
        requests, manifest = self.prepare_job()
        manifest_path = default_manifest_path(requests)
        output = self.root / "direct-output.jsonl"
        output.write_text(
            fake_batch_line(
                "memory-key--general_001",
                {
                    "questionId": "general_001",
                    "questionCue": "Meinung frei äußern",
                    "answerCue": "Meinungsfreiheit",
                },
            )
            + "\n",
            encoding="utf-8",
        )

        calls: list[str] = []

        class FakeResponse:
            id = "response-direct"

            def model_dump(self, *, mode: str) -> dict:
                self.assert_mode(mode)
                return {
                    "output": [
                        {
                            "type": "message",
                            "role": "assistant",
                            "content": [
                                {
                                    "type": "output_text",
                                    "text": json.dumps(
                                        {
                                            "questionId": "state_he_001",
                                            "questionCue": "Hessens Hauptstadt",
                                            "answerCue": "Wiesbaden",
                                        }
                                    ),
                                }
                            ],
                        }
                    ]
                }

            @staticmethod
            def assert_mode(mode: str) -> None:
                if mode != "json":
                    raise AssertionError(mode)

        class FakeResponses:
            @staticmethod
            def create(**body):
                payload = json.loads(body["input"])
                calls.append(payload["questionId"])
                return FakeResponse()

        class FakeClient:
            responses = FakeResponses()

        class FakeOpenAI:
            def __new__(cls, **kwargs):
                del kwargs
                return FakeClient()

        with patch("memory_key_pipeline.require_openai", return_value=FakeOpenAI):
            result = run_direct(
                requests,
                manifest_path,
                output,
                batch_state_path=None,
                workers=2,
                confirmation="RUN_DIRECT",
            )

        self.assertEqual(calls, ["state_he_001"])
        self.assertEqual(result["completedCount"], manifest["requestCount"])
        self.assertEqual(len(load_batch_output(output)), manifest["requestCount"])

    def test_direct_run_retries_a_duplicate_pair(self) -> None:
        requests, manifest = self.prepare_job()
        manifest_path = default_manifest_path(requests)
        output = self.root / "direct-output.jsonl"
        output.write_text(
            fake_batch_line(
                "memory-key--general_001",
                {
                    "questionId": "general_001",
                    "questionCue": "Meinung frei äußern",
                    "answerCue": "Meinungsfreiheit",
                },
            )
            + "\n",
            encoding="utf-8",
        )
        calls = 0

        class FakeResponse:
            id = "response-direct"

            def __init__(self, payload: dict[str, str]) -> None:
                self.payload = payload

            def model_dump(self, *, mode: str) -> dict:
                if mode != "json":
                    raise AssertionError(mode)
                return {
                    "output": [
                        {
                            "type": "message",
                            "role": "assistant",
                            "content": [
                                {
                                    "type": "output_text",
                                    "text": json.dumps(self.payload),
                                }
                            ],
                        }
                    ]
                }

        class FakeResponses:
            @staticmethod
            def create(**body):
                nonlocal calls
                calls += 1
                question_id = json.loads(body["input"])["questionId"]
                if calls == 1:
                    return FakeResponse(
                        {
                            "questionId": question_id,
                            "questionCue": "Meinung frei äußern",
                            "answerCue": "Meinungsfreiheit",
                        }
                    )
                return FakeResponse(
                    {
                        "questionId": question_id,
                        "questionCue": "Hessens Hauptstadt",
                        "answerCue": "Wiesbaden",
                    }
                )

        class FakeClient:
            responses = FakeResponses()

        class FakeOpenAI:
            def __new__(cls, **kwargs):
                del kwargs
                return FakeClient()

        with patch("memory_key_pipeline.require_openai", return_value=FakeOpenAI):
            result = run_direct(
                requests,
                manifest_path,
                output,
                batch_state_path=None,
                workers=1,
                confirmation="RUN_DIRECT",
            )

        self.assertEqual(calls, 2)
        self.assertEqual(result["completedCount"], manifest["requestCount"])

    def test_repair_direct_output_preserves_valid_and_rejects_incomplete(self) -> None:
        output = self.root / "direct-output.jsonl"
        output.write_text(
            "\n".join(
                [
                    fake_batch_line(
                        "memory-key--general_001",
                        {
                            "questionId": "general_001",
                            "questionCue": "Meinung frei äußern",
                            "answerCue": "Meinungsfreiheit",
                        },
                    ),
                    direct_output_line(
                        "memory-key--state_he_001",
                        "response-incomplete",
                        {
                            "status": "incomplete",
                            "incomplete_details": {"reason": "max_output_tokens"},
                            "output": [],
                        },
                    ),
                ]
            )
            + "\n",
            encoding="utf-8",
        )

        result = repair_direct_output(output)

        self.assertEqual(result["keptCount"], 1)
        self.assertEqual(result["rejectedCount"], 1)
        self.assertTrue(Path(result["rejectedOutput"]).exists())
        self.assertEqual(set(load_batch_output(output)), {"memory-key--general_001"})

    def test_repair_rejects_invalid_cues_and_duplicate_pairs(self) -> None:
        output = self.root / "direct-output.jsonl"
        output.write_text(
            "\n".join(
                [
                    fake_batch_line(
                        "memory-key--general_001",
                        {
                            "questionId": "general_001",
                            "questionCue": "Meinung frei äußern",
                            "answerCue": "Meinungsfreiheit",
                        },
                    ),
                    fake_batch_line(
                        "memory-key--state_he_001",
                        {
                            "questionId": "state_he_001",
                            "questionCue": "eins zwei drei vier fünf sechs sieben acht neun",
                            "answerCue": "Wiesbaden",
                        },
                    ),
                    fake_batch_line(
                        "memory-key--general_002",
                        {
                            "questionId": "general_002",
                            "questionCue": "Meinung frei äußern",
                            "answerCue": "Meinungsfreiheit",
                        },
                    ),
                ]
            )
            + "\n",
            encoding="utf-8",
        )

        result = repair_direct_output(output)

        self.assertEqual(result["keptCount"], 1)
        self.assertEqual(result["rejectedCount"], 2)
        self.assertEqual(set(load_batch_output(output)), {"memory-key--general_001"})

    def test_catalog_validation_requires_all_text_answer_keys(self) -> None:
        with self.assertRaises(MemoryKeyPipelineError):
            validate_catalog(self.catalog)


if __name__ == "__main__":
    unittest.main()
