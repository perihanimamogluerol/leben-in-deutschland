#!/usr/bin/env python3

from __future__ import annotations

import copy
import json
import sys
import tempfile
import unittest
from pathlib import Path


CONTENT_DIR = Path(__file__).resolve().parents[1]
if str(CONTENT_DIR) not in sys.path:
    sys.path.insert(0, str(CONTENT_DIR))

from build_schema3 import read_json
from explanation_pipeline import (
    GERMAN_STAGE,
    PipelineError,
    TRANSLATION_STAGE,
    apply_results,
    apply_batch,
    default_manifest_path,
    prepare_german,
    prepare_translations,
    read_manifest,
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


class ExplanationPipelineTest(unittest.TestCase):
    def setUp(self) -> None:
        self.catalog = read_json(FIXTURE)
        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def test_prepares_german_requests_without_network_access(self) -> None:
        catalog = copy.deepcopy(self.catalog)
        for question in catalog["questions"]:
            question["german"]["explanation"] = None
        requests = self.root / "german.jsonl"
        manifest_path = default_manifest_path(requests)

        result = prepare_german(catalog, requests, manifest_path)

        self.assertEqual(result["requestCount"], 2)
        self.assertTrue(result["readyForSubmit"])
        manifest = read_manifest(manifest_path)
        self.assertEqual(manifest["stage"], GERMAN_STAGE)
        lines = [json.loads(line) for line in requests.read_text().splitlines()]
        self.assertEqual(len(lines), 2)
        self.assertTrue(all(line["url"] == "/v1/responses" for line in lines))
        self.assertTrue(all(line["body"]["store"] is False for line in lines))

    def test_applies_out_of_order_german_results_by_custom_id(self) -> None:
        catalog = copy.deepcopy(self.catalog)
        for question in catalog["questions"]:
            question["german"]["explanation"] = None
        requests = self.root / "german.jsonl"
        manifest_path = default_manifest_path(requests)
        prepare_german(catalog, requests, manifest_path)
        manifest = read_manifest(manifest_path)
        batch_output = self.root / "german-output.jsonl"
        batch_output.write_text(
            "\n".join(
                [
                    fake_batch_line(
                        "german--state_he_001",
                        {
                            "questionId": "state_he_001",
                            "explanation": "Wiesbaden ist die Landeshauptstadt des Landes Hessen.",
                        },
                    ),
                    fake_batch_line(
                        "german--general_001",
                        {
                            "questionId": "general_001",
                            "explanation": "Die Meinungsfreiheit ist ein geschütztes Grundrecht.",
                        },
                    ),
                ]
            )
            + "\n",
            encoding="utf-8",
        )

        updated, summary = apply_results(catalog, manifest, batch_output)

        self.assertEqual(summary["appliedCount"], 2)
        self.assertEqual(
            updated["questions"][0]["german"]["explanation"],
            "Die Meinungsfreiheit ist ein geschütztes Grundrecht.",
        )
        self.assertIsNone(catalog["questions"][0]["german"]["explanation"])

    def test_prepares_only_missing_translation_explanations(self) -> None:
        catalog = copy.deepcopy(self.catalog)
        catalog["questions"][0]["localizations"]["tr"]["explanation"] = None
        requests = self.root / "translations.jsonl"
        manifest_path = default_manifest_path(requests)

        result = prepare_translations(catalog, requests, manifest_path)

        self.assertEqual(result["requestCount"], 1)
        self.assertTrue(result["readyForSubmit"])
        manifest = read_manifest(manifest_path)
        self.assertEqual(manifest["stage"], TRANSLATION_STAGE)
        self.assertEqual(manifest["targets"][0]["languageCode"], "tr")

    def test_prepares_translation_when_explanation_provenance_is_unverified(self) -> None:
        catalog = copy.deepcopy(self.catalog)
        del catalog["questions"][0]["localizations"]["tr"]["explanationStatus"]
        requests = self.root / "translations.jsonl"
        manifest_path = default_manifest_path(requests)

        result = prepare_translations(catalog, requests, manifest_path)

        self.assertEqual(result["requestCount"], 1)
        manifest = read_manifest(manifest_path)
        self.assertEqual(manifest["targets"][0]["questionId"], "general_001")
        line = json.loads(requests.read_text().splitlines()[0])
        payload = json.loads(line["body"]["input"])
        self.assertEqual(
            payload["germanExplanation"],
            "Die Meinungsfreiheit ist ein geschütztes Grundrecht.",
        )

    def test_applies_translation_and_records_explanation_provenance(self) -> None:
        catalog = copy.deepcopy(self.catalog)
        del catalog["questions"][0]["localizations"]["tr"]["explanationStatus"]
        requests = self.root / "translations.jsonl"
        manifest_path = default_manifest_path(requests)
        prepare_translations(catalog, requests, manifest_path)
        manifest = read_manifest(manifest_path)
        batch_output = self.root / "translation-output.jsonl"
        batch_output.write_text(
            fake_batch_line(
                "translation--tr--general_001",
                {
                    "questionId": "general_001",
                    "languageCode": "tr",
                    "explanation": "İfade özgürlüğü, korunan temel bir haktır.",
                },
            )
            + "\n",
            encoding="utf-8",
        )

        updated, summary = apply_results(catalog, manifest, batch_output)
        localized = updated["questions"][0]["localizations"]["tr"]

        self.assertEqual(summary["appliedCount"], 1)
        self.assertEqual(
            localized["explanationStatus"],
            "AI_GENERATED_NEEDS_REVIEW",
        )

    def test_rejects_incomplete_batch_output(self) -> None:
        catalog = copy.deepcopy(self.catalog)
        for question in catalog["questions"]:
            question["german"]["explanation"] = None
        requests = self.root / "german.jsonl"
        manifest_path = default_manifest_path(requests)
        prepare_german(catalog, requests, manifest_path)
        manifest = read_manifest(manifest_path)
        batch_output = self.root / "incomplete.jsonl"
        batch_output.write_text(
            fake_batch_line(
                "german--general_001",
                {
                    "questionId": "general_001",
                    "explanation": "Die Meinungsfreiheit ist ein geschütztes Grundrecht.",
                },
            )
            + "\n",
            encoding="utf-8",
        )

        with self.assertRaises(PipelineError):
            apply_results(catalog, manifest, batch_output)

    def test_refuses_to_overwrite_source_catalog_in_place(self) -> None:
        manifest_path = self.root / "manifest.json"
        batch_output = self.root / "output.jsonl"
        source = self.root / "source.json"
        source.write_text(json.dumps(self.catalog), encoding="utf-8")

        with self.assertRaises(PipelineError):
            apply_batch(
                source,
                manifest_path,
                batch_output,
                source,
                force=True,
            )


if __name__ == "__main__":
    unittest.main()
