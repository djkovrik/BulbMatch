#!/usr/bin/env python3
"""Verify privacy and production-catalog invariants in Android release archives."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile


PRODUCTION_CATALOG = "bulbmatch-catalog-production.json"
TEST_AD_TOKENS = (b"demo-banner-yandex", b"demo-interstitial-yandex")
FORBIDDEN_ANALYTICS_TOKENS = (
    b"com.google.firebase:firebase-analytics",
    b"com/google/android/gms/measurement",
    b"com.google.android.gms.measurement",
    b"play-services-measurement",
)


def fail(message: str) -> None:
    raise SystemExit(f"release verification failed: {message}")


def verify_archive(archive_path: Path, source_bytes: bytes) -> None:
    if not archive_path.is_file():
        fail(f"artifact is missing: {archive_path}")

    with ZipFile(archive_path) as archive:
        names = archive.namelist()
        shipping_catalogs = [
            name
            for name in names
            if Path(name).name.startswith("bulbmatch-catalog-")
            and Path(name).suffix == ".json"
        ]
        production_matches = [
            name for name in shipping_catalogs if Path(name).name == PRODUCTION_CATALOG
        ]
        if len(shipping_catalogs) != 1 or len(production_matches) != 1:
            fail(
                f"{archive_path.name} must contain exactly one production catalog; "
                f"found {shipping_catalogs}"
            )
        packaged_bytes = archive.read(production_matches[0])
        if packaged_bytes != source_bytes:
            fail(
                f"{archive_path.name}:{production_matches[0]} does not byte-match "
                "the signed source catalog"
            )

        found_forbidden: dict[str, set[str]] = {}
        for name in names:
            if name.endswith("/"):
                continue
            content = archive.read(name)
            for token in TEST_AD_TOKENS + FORBIDDEN_ANALYTICS_TOKENS:
                if token in content:
                    found_forbidden.setdefault(token.decode("ascii"), set()).add(name)
        if found_forbidden:
            locations = {
                token: sorted(entries) for token, entries in sorted(found_forbidden.items())
            }
            fail(
                f"{archive_path.name} contains forbidden release tokens: "
                f"{locations}"
            )

    print(
        f"verified {archive_path.name}: bytes={archive_path.stat().st_size} "
        f"catalogEntry={production_matches[0]}"
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--aab", required=True, type=Path)
    parser.add_argument("--catalog", required=True, type=Path)
    parser.add_argument("--expected-canonical-hash", required=True)
    args = parser.parse_args()

    source_bytes = args.catalog.read_bytes()
    source_document = json.loads(source_bytes.decode("utf-8"))
    actual_canonical_hash = source_document.get("contentHash")
    if actual_canonical_hash != args.expected_canonical_hash:
        fail(
            "source catalog contentHash mismatch: "
            f"expected {args.expected_canonical_hash}, got {actual_canonical_hash}"
        )

    verify_archive(args.apk, source_bytes)
    verify_archive(args.aab, source_bytes)
    print(
        "sourceCatalogSha256="
        f"{hashlib.sha256(source_bytes).hexdigest()} "
        f"canonicalHash={actual_canonical_hash}"
    )


if __name__ == "__main__":
    main()
