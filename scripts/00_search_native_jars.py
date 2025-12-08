#!/usr/bin/env python3
"""
Search recursively through Minecraft native jar files for ASCII patterns.

Usage:
    python3 scripts/search_native_jars.py /mnt/f/minecraftNativeJars getReachDistance
"""
from __future__ import annotations

import argparse
import shlex
import subprocess
import sys
from pathlib import Path
from typing import Iterable, Sequence
from zipfile import BadZipFile, ZipFile


def find_jars(root: Path) -> Iterable[Path]:
    if root.is_file():
        if root.suffix == ".jar":
            yield root
        return
    for path in root.rglob("*.jar"):
        if path.is_file():
            yield path


def search_jar(
    jar_path: Path,
    patterns: Sequence[bytes],
    *,
    case_sensitive: bool,
) -> list[tuple[str, str]]:
    matches: list[tuple[str, str]] = []
    try:
        with ZipFile(jar_path) as jar:
            for entry in jar.infolist():
                if entry.is_dir() or not entry.filename.endswith(".class"):
                    continue
                with jar.open(entry) as fp:
                    data = fp.read()
                hay = data if case_sensitive else data.lower()
                for needle in patterns:
                    ned = needle if case_sensitive else needle.lower()
                    if ned in hay:
                        matches.append((entry.filename, needle.decode("utf-8", "ignore")))
                        break
    except BadZipFile:
        print(f"[warn] Skipping unreadable jar: {jar_path}", file=sys.stderr)
    return matches


def entry_to_class(entry_name: str) -> str:
    return entry_name[:-6].replace("/", ".")


def disassemble_with_javap(jar_path: Path, class_name: str, javap_opts: list[str]) -> None:
    cmd = ["javap", "-classpath", str(jar_path)]
    if javap_opts:
        cmd.extend(javap_opts)
    cmd.append(class_name)
    print(f"[javap] {' '.join(cmd)}", flush=True)
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, check=False)
    except FileNotFoundError:
        print("[javap] javap command not found. Install a JDK or adjust PATH.", file=sys.stderr)
        return
    if result.stdout:
        print(result.stdout.rstrip())
    if result.stderr:
        print(result.stderr.rstrip(), file=sys.stderr)


def class_exists_in_jar(jar_path: Path, class_name: str) -> bool:
    entry = class_name.replace(".", "/") + ".class"
    try:
        with ZipFile(jar_path) as jar:
            return entry in jar.namelist()
    except BadZipFile:
        return False


def main() -> None:
    parser = argparse.ArgumentParser(description="Search Minecraft native jars for byte patterns.")
    parser.add_argument(
        "--javap",
        nargs="?",
        const="__MATCHES__",
        metavar="CLASS",
        help="Run javap. Without CLASS, dumps every matching class; with CLASS, dumps that specific type.",
    )
    parser.add_argument("--javap-opts", default="-c", help='Options to pass to javap (default: "-c").')
    parser.add_argument("--case-sensitive", action="store_true", help="Treat patterns as case-sensitive.")
    parser.add_argument("root", type=Path, help="Directory or jar containing classes to scan.")
    parser.add_argument("patterns", nargs="+", help="ASCII patterns to search for (e.g. method names).")
    args = parser.parse_args()

    root = args.root
    if not root.exists():
        parser.error(f"{root} does not exist")

    needles = [pat.encode("utf-8") for pat in args.patterns]
    javap_opts = shlex.split(args.javap_opts) if args.javap_opts else []
    per_match_disassembly = args.javap == "__MATCHES__"
    specific_class = None if not args.javap or per_match_disassembly else args.javap

    print(f"Searching {root} for {', '.join(args.patterns)}...", flush=True)
    jar_paths = list(find_jars(root))
    if not jar_paths:
        print("No jar files found under the specified root.", file=sys.stderr)
        sys.exit(1)

    jar_count = 0
    total = 0
    for jar_path in jar_paths:
        jar_count += 1
        hits = search_jar(jar_path, needles, case_sensitive=args.case_sensitive)
        for entry_name, needle_str in hits:
            print(f"{jar_path}:{entry_name} -> {needle_str}")
            total += 1
            if per_match_disassembly:
                disassemble_with_javap(jar_path, entry_to_class(entry_name), javap_opts)

    summary = f"Scanned {jar_count} jar(s). Matches: {total}."
    print(summary)
    if total == 0:
        joined = ", ".join(args.patterns)
        print(f"No matches found for: {joined}")

    if specific_class:
        target_jar = None
        if len(jar_paths) == 1:
            target_jar = jar_paths[0]
        else:
            for jar_path in jar_paths:
                if class_exists_in_jar(jar_path, specific_class):
                    target_jar = jar_path
                    break
        if target_jar is None:
            print(f"[javap] Could not locate {specific_class} in scanned jars.", file=sys.stderr)
        else:
            disassemble_with_javap(target_jar, specific_class, javap_opts)


if __name__ == "__main__":
    main()



