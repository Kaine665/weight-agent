#!/usr/bin/env python3
"""
Bump defaultConfig versionCode (+1) and versionName (semver patch +1) in app/build.gradle.kts.
Appends version_name= and version_code= to GITHUB_OUTPUT when set.
"""
import os
import re
import sys


def bump_version_name(name: str) -> str:
    s = name.strip()
    m = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)(?:-([A-Za-z0-9.-]+))?", s)
    if m:
        a, b, c = int(m.group(1)), int(m.group(2)), int(m.group(3))
        suffix = m.group(4)
        core = f"{a}.{b}.{c + 1}"
        return f"{core}-{suffix}" if suffix else core
    m2 = re.search(r"(\d+)\s*$", s)
    if m2:
        return s[: m2.start()] + str(int(m2.group(1)) + 1)
    return f"{s}.1" if s else "0.0.1"


def main() -> int:
    path = sys.argv[1] if len(sys.argv) > 1 else "app/build.gradle.kts"
    text = open(path, encoding="utf-8").read()

    m_code = re.search(r"versionCode\s*=\s*(\d+)", text)
    m_name = re.search(r'versionName\s*=\s*"([^"]*)"', text)
    if not m_code or not m_name:
        print("versionCode or versionName not found", file=sys.stderr)
        return 1

    old_code = int(m_code.group(1))
    old_name = m_name.group(1)
    new_code = old_code + 1
    new_name = bump_version_name(old_name)

    text = re.sub(r"versionCode\s*=\s*\d+", f"versionCode = {new_code}", text, count=1)
    text = re.sub(
        r'versionName\s*=\s*"[^"]*"',
        f'versionName = "{new_name}"',
        text,
        count=1,
    )
    open(path, "w", encoding="utf-8").write(text)

    gh_out = os.environ.get("GITHUB_OUTPUT")
    if gh_out:
        with open(gh_out, "a", encoding="utf-8") as out:
            out.write(f"version_name={new_name}\n")
            out.write(f"version_code={new_code}\n")

    print(f"Bumped versionName {old_name} -> {new_name}, versionCode {old_code} -> {new_code}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
