#!/usr/bin/env python3
"""Validates the versioned synthetic OCR corpus without modifying it."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "manifest.json"


def main() -> None:
    payload = json.loads(MANIFEST.read_text(encoding="utf-8"))
    fixtures = payload["fixtures"]
    assert payload["schemaVersion"] == 1
    assert payload["license"] == "project-owned-synthetic"
    assert payload["fixtureCount"] == len(fixtures)
    assert 60 <= len(fixtures) <= 100

    declared: set[Path] = set()
    variants: set[str] = set()
    cases: set[str] = set()
    for fixture in fixtures:
        relative = Path(fixture["file"])
        assert not relative.is_absolute() and ".." not in relative.parts
        path = ROOT / relative
        assert path.is_file(), f"missing fixture: {relative}"
        assert path.suffix.lower() == ".png"
        actual_hash = hashlib.sha256(path.read_bytes()).hexdigest()
        assert actual_hash == fixture["sha256"], f"hash mismatch: {relative}"
        assert path not in declared, f"duplicate fixture: {relative}"
        declared.add(path)
        variants.add(fixture["variant"])
        cases.add(fixture["case"])

    actual = set((ROOT / "images").glob("*.png"))
    assert actual == declared, "manifest/image set mismatch"
    assert variants == {"clean", "rotated", "low_contrast", "small", "curved", "blurred"}
    assert {"latin_e27", "cyrillic_e27", "confusable_negative", "no_text"} <= cases
    print(f"Validated {len(fixtures)} OCR fixtures.")


if __name__ == "__main__":
    main()
