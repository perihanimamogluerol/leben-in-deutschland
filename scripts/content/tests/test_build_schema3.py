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

from build_schema3 import BuildError, build_release, read_json, write_json
from validate_schema3 import ValidationError, validate_release


FIXTURE = Path(__file__).parent / "fixtures" / "schema2_minimal.json"


class Schema3PipelineTest(unittest.TestCase):
    def setUp(self) -> None:
        self.catalog = read_json(FIXTURE)
        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def test_builds_self_contained_language_packages(self) -> None:
        output = self.root / "release"

        result = build_release(self.catalog, output)

        self.assertEqual(result["status"], "READY")
        self.assertEqual(result["questionCount"], 2)
        self.assertEqual(result["languageCount"], 2)

        turkish_chunk = read_json(
            output / "language-packs" / "tr" / "chunks" / "0000.json"
        )
        question = turkish_chunk["questions"][0]

        self.assertNotIn("localizations", question)
        self.assertEqual(question["study"]["languageCode"], "tr")
        self.assertEqual(
            question["german"]["questionText"],
            "Warum dürfen Menschen ihre Meinung frei äußern?",
        )

        validation = validate_release(
            output,
            expected_question_count=2,
        )
        self.assertTrue(validation["readyForUpload"])

    def test_incomplete_explanation_requires_draft_flag(self) -> None:
        catalog = copy.deepcopy(self.catalog)
        catalog["questions"][0]["german"]["explanation"] = None
        output = self.root / "release"

        with self.assertRaises(BuildError):
            build_release(catalog, output)

        result = build_release(catalog, output, allow_incomplete=True)

        self.assertEqual(result["status"], "DRAFT")
        self.assertGreater(result["qualityIssueCount"], 0)

        validation = validate_release(
            output,
            allow_draft=True,
            expected_question_count=2,
        )
        self.assertFalse(validation["readyForUpload"])

    def test_validator_rejects_tampered_chunk(self) -> None:
        output = self.root / "release"
        build_release(self.catalog, output)
        chunk_path = output / "language-packs" / "tr" / "chunks" / "0000.json"
        chunk = read_json(chunk_path)
        chunk["questions"][0]["study"]["questionText"] = "Tampered"
        write_json(chunk_path, chunk)

        with self.assertRaises(ValidationError):
            validate_release(output, expected_question_count=2)


if __name__ == "__main__":
    unittest.main()
