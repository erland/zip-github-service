\
#!/usr/bin/env python3
"""Verify that a distributable zip-github source archive has the exact release shape."""
from __future__ import annotations
import sys
import zipfile
from pathlib import PurePosixPath

FORBIDDEN_PARTS = {"node_modules", "target", "dist", "coverage", "__pycache__"}
FORBIDDEN_WORK_PREFIXES = ("rc", "work")


def fail(message: str) -> "NoReturn":
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: scripts/verify-package.py <archive.zip>", file=sys.stderr)
        return 2

    archive = sys.argv[1]
    with zipfile.ZipFile(archive) as zf:
        names = [name for name in zf.namelist() if name and not name.endswith("/")]
        bad_member = zf.testzip()
        if bad_member:
            fail(f"ZIP integrity failure at {bad_member}")

    roots = {PurePosixPath(name).parts[0] for name in names if PurePosixPath(name).parts}
    if roots != {"zip-github"}:
        fail(f"release archive must contain exactly one top-level 'zip-github/' tree; found {sorted(roots)}")

    info_by_name = {info.filename: info for info in zf.infolist() if not info.is_dir()}

    for name in names:
        parts = PurePosixPath(name).parts
        if any(part in FORBIDDEN_PARTS for part in parts):
            fail(f"generated directory is forbidden in release archive: {name}")
        # Catch accidental scratch copies such as rc104work/ even if nested later.
        for part in parts[1:]:
            lower = part.lower()
            if lower.startswith("rc") and lower.endswith("work"):
                fail(f"transient work directory is forbidden in release archive: {name}")

        # Release archives are also responsible for preserving executable Unix modes.
        # Every shell script and the Maven wrapper are part of the executable contract.
        if name.endswith(".sh") or name == "zip-github/backend/mvnw":
            mode = (info_by_name[name].external_attr >> 16) & 0o7777
            if not (mode & 0o111):
                fail(f"required executable lost its execute bit: {name} (mode {mode:o})")

    print(f"Release package verified: {archive}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
