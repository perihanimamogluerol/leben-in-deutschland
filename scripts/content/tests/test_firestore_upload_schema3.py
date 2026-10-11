#!/usr/bin/env python3

from __future__ import annotations

import json
import os
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


CONTENT_DIR = Path(__file__).resolve().parents[1]
if str(CONTENT_DIR) not in sys.path:
    sys.path.insert(0, str(CONTENT_DIR))

from build_schema3 import build_release, read_json
from firestore_upload_schema3 import (
    UploadError,
    build_active_manifest,
    build_plan,
    compare_documents,
    load_bundle,
    require_confirmation,
    service_account_path_and_project,
)


FIXTURE = Path(__file__).parent / "fixtures" / "schema2_minimal.json"


class FirestoreSchema3UploaderTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name)
        self.release_dir = self.root / "release"
        build_release(read_json(FIXTURE), self.release_dir)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def test_plan_is_deterministic_and_performs_no_remote_work(self) -> None:
        bundle = load_bundle(self.release_dir, expected_question_count=2)
        plan = build_plan(bundle)

        self.assertEqual(plan["mode"], "DRY_RUN")
        self.assertEqual(plan["questionCount"], 2)
        self.assertEqual(plan["languageCount"], 2)
        self.assertEqual(plan["packageDocuments"], 2)
        self.assertEqual(plan["chunkDocuments"], 2)
        self.assertEqual(plan["uploadDocuments"], 5)
        self.assertEqual(plan["writesPerformed"], 0)

    def test_loader_rejects_an_unexpected_chunk_file(self) -> None:
        extra = self.release_dir / "language-packs" / "de" / "chunks" / "9999.json"
        extra.write_text("{}\n", encoding="utf-8")

        with self.assertRaises(UploadError):
            load_bundle(self.release_dir, expected_question_count=2)

    def test_confirmation_must_match_exactly(self) -> None:
        require_confirmation("project-a", "project-a", "Project ID")
        with self.assertRaises(UploadError):
            require_confirmation("project-a", "Project A", "Project ID")

    def test_document_comparison_reports_every_difference(self) -> None:
        expected = {
            "c/a": {"value": 1},
            "c/b": {"value": 2},
        }
        observed = {
            "c/a": {"value": 9},
            "c/c": {"value": 3},
        }

        result = compare_documents(expected, observed)

        self.assertEqual(result["missing"], ["c/b"])
        self.assertEqual(result["mismatched"], ["c/a"])
        self.assertEqual(result["extra"], ["c/c"])

    def test_active_manifest_preserves_previous_release(self) -> None:
        bundle = load_bundle(self.release_dir, expected_question_count=2)

        manifest = build_active_manifest(
            bundle.release,
            previous_release_id="schema-2-release",
        )

        self.assertEqual(manifest["activeReleaseId"], bundle.release_id)
        self.assertEqual(manifest["previousReleaseId"], "schema-2-release")
        self.assertEqual(manifest["languageCount"], 2)

    def test_service_account_project_must_match_target(self) -> None:
        credential_path = self.root / "service-account.json"
        credential_path.write_text(
            json.dumps(
                {
                    "type": "service_account",
                    "project_id": "wrong-project",
                }
            ),
            encoding="utf-8",
        )

        with patch.dict(
            os.environ,
            {"GOOGLE_APPLICATION_CREDENTIALS": str(credential_path)},
            clear=False,
        ):
            with self.assertRaises(UploadError):
                service_account_path_and_project("expected-project")


if __name__ == "__main__":
    unittest.main()
