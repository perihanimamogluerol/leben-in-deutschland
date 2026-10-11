#!/usr/bin/env python3
"""Safely upload, verify, and activate a validated schema-3 Firestore release."""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable

from build_schema3 import read_json
from validate_schema3 import ValidationError, validate_release


RELEASES_COLLECTION = "contentReleases"
ACTIVE_MANIFEST_PATH = "contentManifest/current"
MAX_BATCH_WRITES = 300
MAX_SAFE_DOCUMENT_JSON_BYTES = 950_000
SAFE_DOCUMENT_ID = re.compile(r"^[A-Za-z0-9._-]+$")


class UploadError(RuntimeError):
    """Raised when an upload operation is unsafe or cannot be verified."""


@dataclass(frozen=True)
class DocumentSpec:
    """One deterministic Firestore document generated from the local bundle."""

    path: str
    data: dict[str, Any]
    kind: str


@dataclass(frozen=True)
class ReleaseBundle:
    """A fully validated local release and its deterministic document mapping."""

    root: Path
    release: dict[str, Any]
    package_documents: tuple[DocumentSpec, ...]
    chunk_documents: tuple[DocumentSpec, ...]

    @property
    def release_id(self) -> str:
        return self.release["releaseId"]

    @property
    def release_document(self) -> DocumentSpec:
        return DocumentSpec(
            path=f"{RELEASES_COLLECTION}/{self.release_id}",
            data=self.release,
            kind="release",
        )

    @property
    def content_documents(self) -> tuple[DocumentSpec, ...]:
        return self.package_documents + self.chunk_documents

    @property
    def all_documents(self) -> tuple[DocumentSpec, ...]:
        return self.content_documents + (self.release_document,)


def canonical_json(value: Any) -> str:
    return json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    )


def validate_document_id(value: Any, label: str) -> str:
    if not isinstance(value, str) or not value or not SAFE_DOCUMENT_ID.fullmatch(value):
        raise UploadError(f"Unsafe {label}: {value!r}")
    return value


def validate_document_size(spec: DocumentSpec) -> None:
    size = len(canonical_json(spec.data).encode("utf-8"))
    if size > MAX_SAFE_DOCUMENT_JSON_BYTES:
        raise UploadError(
            f"Document {spec.path} is {size} JSON bytes; the safe limit is "
            f"{MAX_SAFE_DOCUMENT_JSON_BYTES}."
        )


def load_bundle(
    input_dir: Path,
    *,
    expected_question_count: int = 460,
) -> ReleaseBundle:
    """Validate a production bundle and map every file to one Firestore document."""

    root = input_dir.expanduser().resolve()
    if not root.is_dir():
        raise UploadError(f"Schema-3 directory does not exist: {root}")

    try:
        validation = validate_release(
            root,
            expected_question_count=expected_question_count,
        )
    except (ValidationError, RuntimeError) as exc:
        raise UploadError(f"Local schema-3 validation failed: {exc}") from exc

    if not validation.get("readyForUpload"):
        raise UploadError("The local release is not ready for upload.")

    release = read_json(root / "release.json")
    release_id = validate_document_id(release.get("releaseId"), "release ID")
    if release.get("status") != "READY":
        raise UploadError("Only a release with status READY can be uploaded.")

    languages = release.get("languages")
    if not isinstance(languages, list) or not languages:
        raise UploadError("Release languages must be a non-empty array.")
    if len(set(languages)) != len(languages):
        raise UploadError("Release languages contain duplicate values.")
    if release.get("languageCount") != len(languages):
        raise UploadError("Release languageCount does not match languages.")

    language_pack_root = root / "language-packs"
    observed_language_dirs = {
        path.name for path in language_pack_root.iterdir() if path.is_dir()
    }
    expected_language_dirs = set(languages)
    if observed_language_dirs != expected_language_dirs:
        raise UploadError(
            "Language package directories do not exactly match release.json: "
            f"expected={sorted(expected_language_dirs)}, "
            f"observed={sorted(observed_language_dirs)}"
        )

    package_documents: list[DocumentSpec] = []
    chunk_documents: list[DocumentSpec] = []

    for language_value in languages:
        language_code = validate_document_id(language_value, "language code")
        package_dir = language_pack_root / language_code
        package = read_json(package_dir / "package.json")
        chunk_count = package.get("chunkCount")
        if not isinstance(chunk_count, int) or chunk_count <= 0:
            raise UploadError(f"Invalid chunkCount for {language_code}.")

        package_path = (
            f"{RELEASES_COLLECTION}/{release_id}/languagePacks/{language_code}"
        )
        package_documents.append(
            DocumentSpec(path=package_path, data=package, kind="package")
        )

        chunks_dir = package_dir / "chunks"
        expected_chunk_names = {f"{index:04d}.json" for index in range(chunk_count)}
        observed_chunk_names = {
            path.name for path in chunks_dir.iterdir() if path.is_file()
        }
        if observed_chunk_names != expected_chunk_names:
            raise UploadError(
                f"Chunk files for {language_code} do not exactly match package.json."
            )

        for chunk_index in range(chunk_count):
            chunk_id = f"{chunk_index:04d}"
            chunk = read_json(chunks_dir / f"{chunk_id}.json")
            chunk_documents.append(
                DocumentSpec(
                    path=f"{package_path}/chunks/{chunk_id}",
                    data=chunk,
                    kind="chunk",
                )
            )

    bundle = ReleaseBundle(
        root=root,
        release=release,
        package_documents=tuple(package_documents),
        chunk_documents=tuple(chunk_documents),
    )
    for spec in bundle.all_documents:
        validate_document_size(spec)
    return bundle


def build_plan(bundle: ReleaseBundle) -> dict[str, Any]:
    chunk_count = len(bundle.chunk_documents)
    package_count = len(bundle.package_documents)
    return {
        "mode": "DRY_RUN",
        "releaseId": bundle.release_id,
        "schemaVersion": bundle.release.get("schemaVersion"),
        "status": bundle.release.get("status"),
        "questionCount": bundle.release.get("questionCount"),
        "languages": bundle.release.get("languages"),
        "languageCount": package_count,
        "packageDocuments": package_count,
        "chunkDocuments": chunk_count,
        "releaseDocuments": 1,
        "uploadDocuments": package_count + chunk_count + 1,
        "activationDocuments": 1,
        "readyForUpload": True,
        "writesPerformed": 0,
    }


def require_confirmation(actual: str, confirmation: str | None, label: str) -> None:
    if confirmation != actual:
        raise UploadError(
            f"{label} confirmation does not match. Expected the exact value: {actual}"
        )


def service_account_path_and_project(project_id: str) -> Path:
    raw_path = os.environ.get("GOOGLE_APPLICATION_CREDENTIALS")
    if not raw_path:
        raise UploadError(
            "GOOGLE_APPLICATION_CREDENTIALS is not configured. Point it to the "
            "Firebase service-account JSON file."
        )

    credential_path = Path(raw_path).expanduser().resolve()
    if not credential_path.is_file():
        raise UploadError(
            f"GOOGLE_APPLICATION_CREDENTIALS does not point to a file: {credential_path}"
        )

    try:
        credential_data = read_json(credential_path)
    except (OSError, json.JSONDecodeError, RuntimeError) as exc:
        raise UploadError("The service-account JSON file cannot be read.") from exc

    credential_project = credential_data.get("project_id")
    if credential_project != project_id:
        raise UploadError(
            "Service-account project mismatch: "
            f"expected {project_id}, found {credential_project!r}."
        )
    if credential_data.get("type") != "service_account":
        raise UploadError("Credential JSON is not a service-account credential.")
    return credential_path


def connect_firestore(project_id: str) -> tuple[Any, Any]:
    """Create a named Firebase Admin app without exposing credential contents."""

    credential_path = service_account_path_and_project(project_id)
    try:
        import firebase_admin
        from firebase_admin import credentials, firestore
        from google.cloud import firestore as google_firestore
    except ImportError as exc:
        raise UploadError(
            "firebase-admin is not installed. Run: "
            "python -m pip install -r scripts/content/requirements-firebase.txt"
        ) from exc

    try:
        app_name = f"schema3-uploader-{os.getpid()}"
        credential = credentials.Certificate(str(credential_path))
        app = firebase_admin.initialize_app(
            credential,
            {"projectId": project_id},
            name=app_name,
        )
        return firestore.client(app=app), google_firestore
    except Exception as exc:
        raise UploadError(
            "Firebase Admin initialization failed. Check the service-account "
            "file and the target project configuration."
        ) from exc


def expected_map(specs: Iterable[DocumentSpec]) -> dict[str, dict[str, Any]]:
    return {spec.path: spec.data for spec in specs}


def scan_release_tree(
    db: Any,
    release_id: str,
    expected_languages: Iterable[str],
) -> dict[str, dict[str, Any]]:
    """Read the release document and every package/chunk document below it."""

    release_ref = db.collection(RELEASES_COLLECTION).document(release_id)
    observed: dict[str, dict[str, Any]] = {}

    release_snapshot = release_ref.get()
    if release_snapshot.exists:
        observed[release_ref.path] = release_snapshot.to_dict()

    package_refs: dict[str, Any] = {}
    for package_snapshot in release_ref.collection("languagePacks").stream():
        observed[package_snapshot.reference.path] = package_snapshot.to_dict()
        package_refs[package_snapshot.id] = package_snapshot.reference

    for language_code in expected_languages:
        package_refs.setdefault(
            language_code,
            release_ref.collection("languagePacks").document(language_code),
        )

    for package_ref in package_refs.values():
        for chunk_snapshot in package_ref.collection("chunks").stream():
            observed[chunk_snapshot.reference.path] = chunk_snapshot.to_dict()
    return observed


def compare_documents(
    expected: dict[str, dict[str, Any]],
    observed: dict[str, dict[str, Any]],
) -> dict[str, list[str]]:
    expected_paths = set(expected)
    observed_paths = set(observed)
    mismatched = sorted(
        path
        for path in expected_paths & observed_paths
        if canonical_json(expected[path]) != canonical_json(observed[path])
    )
    return {
        "missing": sorted(expected_paths - observed_paths),
        "mismatched": mismatched,
        "extra": sorted(observed_paths - expected_paths),
    }


def require_clean_comparison(
    comparison: dict[str, list[str]],
    *,
    operation: str,
) -> None:
    if not any(comparison.values()):
        return
    summary = {key: values[:5] for key, values in comparison.items() if values}
    raise UploadError(f"Remote {operation} verification failed: {summary}")


def verify_remote(bundle: ReleaseBundle, db: Any) -> dict[str, Any]:
    expected = expected_map(bundle.all_documents)
    observed = scan_release_tree(
        db,
        bundle.release_id,
        bundle.release["languages"],
    )
    comparison = compare_documents(expected, observed)
    require_clean_comparison(comparison, operation="release")
    return {
        "verified": True,
        "releaseId": bundle.release_id,
        "documentCount": len(expected),
        "packageDocuments": len(bundle.package_documents),
        "chunkDocuments": len(bundle.chunk_documents),
    }


def commit_specs(db: Any, specs: list[DocumentSpec]) -> int:
    commits = 0
    for offset in range(0, len(specs), MAX_BATCH_WRITES):
        batch = db.batch()
        for spec in specs[offset : offset + MAX_BATCH_WRITES]:
            batch.set(db.document(spec.path), spec.data)
        batch.commit()
        commits += 1
    return commits


def upload_release(bundle: ReleaseBundle, db: Any, project_id: str) -> dict[str, Any]:
    """Upload immutable content first and create the release document last."""

    observed = scan_release_tree(
        db,
        bundle.release_id,
        bundle.release["languages"],
    )
    release_path = bundle.release_document.path

    if release_path in observed:
        verification = verify_remote(bundle, db)
        return {
            "uploaded": True,
            "alreadyUploaded": True,
            "activated": False,
            "projectId": project_id,
            **verification,
        }

    expected_content = expected_map(bundle.content_documents)
    observed_content = {
        path: data for path, data in observed.items() if path != release_path
    }
    preflight = compare_documents(expected_content, observed_content)
    if preflight["mismatched"] or preflight["extra"]:
        require_clean_comparison(preflight, operation="preflight")

    missing_paths = set(preflight["missing"])
    missing_specs = [
        spec for spec in bundle.content_documents if spec.path in missing_paths
    ]
    batch_commits = commit_specs(db, missing_specs)

    staged = scan_release_tree(
        db,
        bundle.release_id,
        bundle.release["languages"],
    )
    staged_content = {
        path: data for path, data in staged.items() if path != release_path
    }
    require_clean_comparison(
        compare_documents(expected_content, staged_content),
        operation="staged-content",
    )

    try:
        db.document(release_path).create(bundle.release)
    except Exception as exc:
        raise UploadError(
            "Release metadata could not be created after content upload. "
            "No activation was attempted; rerun upload to verify or resume safely."
        ) from exc

    verification = verify_remote(bundle, db)
    return {
        "uploaded": True,
        "alreadyUploaded": False,
        "activated": False,
        "projectId": project_id,
        "uploadedThisRun": len(missing_specs) + 1,
        "batchCommits": batch_commits,
        **verification,
    }


def build_active_manifest(
    release: dict[str, Any],
    *,
    previous_release_id: str | None,
) -> dict[str, Any]:
    return {
        "activeReleaseId": release["releaseId"],
        "previousReleaseId": previous_release_id,
        "schemaVersion": release["schemaVersion"],
        "datasetVersion": release.get("datasetVersion"),
        "packageStrategy": release["packageStrategy"],
        "questionCount": release["questionCount"],
        "languages": release["languages"],
        "languageCount": release["languageCount"],
        "catalogSha256": release["catalogSha256"],
        "sourceCatalogSha256": release.get("sourceCatalogSha256"),
        "releaseCreatedAt": release.get("createdAt"),
    }


def activate_release(
    bundle: ReleaseBundle,
    db: Any,
    firestore_module: Any,
    project_id: str,
) -> dict[str, Any]:
    """Atomically change only the active manifest after complete remote verification."""

    verification = verify_remote(bundle, db)
    release_ref = db.document(bundle.release_document.path)
    manifest_ref = db.document(ACTIVE_MANIFEST_PATH)
    transaction = db.transaction()

    @firestore_module.transactional
    def update_manifest(transaction_value: Any) -> dict[str, Any]:
        release_snapshot = release_ref.get(transaction=transaction_value)
        if not release_snapshot.exists:
            raise UploadError("The release disappeared before activation.")
        if canonical_json(release_snapshot.to_dict()) != canonical_json(bundle.release):
            raise UploadError("Remote release changed before activation.")

        current_snapshot = manifest_ref.get(transaction=transaction_value)
        current = current_snapshot.to_dict() if current_snapshot.exists else {}
        current_release_id = current.get("activeReleaseId")
        if current_release_id == bundle.release_id:
            return {
                "alreadyActive": True,
                "previousReleaseId": current.get("previousReleaseId"),
            }

        manifest = build_active_manifest(
            bundle.release,
            previous_release_id=current_release_id,
        )
        manifest["updatedAt"] = firestore_module.SERVER_TIMESTAMP
        transaction_value.set(manifest_ref, manifest)
        return {
            "alreadyActive": False,
            "previousReleaseId": current_release_id,
        }

    activation = update_manifest(transaction)
    return {
        "activated": True,
        "projectId": project_id,
        "releaseId": bundle.release_id,
        **activation,
        **verification,
    }


def add_local_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--expected-question-count", type=int, default=460)


def add_remote_arguments(parser: argparse.ArgumentParser) -> None:
    add_local_arguments(parser)
    parser.add_argument("--project-id", required=True)


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Safely publish a validated schema-3 release to Firestore."
    )
    commands = parser.add_subparsers(dest="command", required=True)

    plan_parser = commands.add_parser("plan", help="Validate locally; never connect.")
    add_local_arguments(plan_parser)

    upload_parser = commands.add_parser(
        "upload", help="Upload and verify without activation."
    )
    add_remote_arguments(upload_parser)
    upload_parser.add_argument("--confirm-project", required=True)
    upload_parser.add_argument("--confirm-release", required=True)

    verify_parser = commands.add_parser("verify", help="Read and compare remote data.")
    add_remote_arguments(verify_parser)

    activate_parser = commands.add_parser(
        "activate", help="Atomically switch the active manifest."
    )
    add_remote_arguments(activate_parser)
    activate_parser.add_argument("--confirm-project", required=True)
    activate_parser.add_argument("--confirm-release", required=True)
    activate_parser.add_argument("--confirm-activate", required=True)
    return parser.parse_args(argv)


def run(args: argparse.Namespace) -> dict[str, Any]:
    bundle = load_bundle(
        args.input,
        expected_question_count=args.expected_question_count,
    )
    if args.command == "plan":
        return build_plan(bundle)

    project_id = validate_document_id(args.project_id, "Firebase project ID")
    if args.command in {"upload", "activate"}:
        require_confirmation(project_id, args.confirm_project, "Project ID")
        require_confirmation(bundle.release_id, args.confirm_release, "Release ID")
    if args.command == "activate" and args.confirm_activate != "ACTIVATE":
        raise UploadError("Activation requires the exact confirmation ACTIVATE.")

    db, firestore_module = connect_firestore(project_id)
    if args.command == "verify":
        return {
            "projectId": project_id,
            **verify_remote(bundle, db),
        }
    if args.command == "upload":
        return upload_release(bundle, db, project_id)
    if args.command == "activate":
        return activate_release(bundle, db, firestore_module, project_id)
    raise UploadError(f"Unsupported command: {args.command}")


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    try:
        result = run(args)
    except (UploadError, ValidationError, RuntimeError, OSError) as exc:
        print(json.dumps({"ok": False, "error": str(exc)}, indent=2))
        return 1

    print(json.dumps({"ok": True, **result}, indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
