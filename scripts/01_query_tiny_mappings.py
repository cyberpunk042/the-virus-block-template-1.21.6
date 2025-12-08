#!/usr/bin/env python3
"""
Quick helper to inspect Fabric tiny mapping files.

Usage:
    python3 scripts/query_tiny_mappings.py --class net.minecraft.server.network.ServerPlayNetworkHandler
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable, Optional


@dataclass
class MethodEntry:
    descriptor: str
    named: str
    official: str
    intermediary: str


@dataclass
class ClassEntry:
    named: str
    official: str
    intermediary: str
    methods: list[MethodEntry] = field(default_factory=list)


def parse_tiny(path: Path) -> list[ClassEntry]:
    classes: list[ClassEntry] = []
    current: Optional[ClassEntry] = None
    with path.open("r", encoding="utf-8") as fh:
        for raw in fh:
            line = raw.rstrip("\n")
            if not line:
                continue
            stripped = line.lstrip("\t")
            if not stripped or stripped.startswith("#"):
                continue
            parts = stripped.split("\t")
            tag, payload = parts[0], parts[1:]
            if tag == "c":
                current = ClassEntry(*payload[:3])
                classes.append(current)
            elif tag == "m" and current is not None:
                current.methods.append(MethodEntry(*payload[:4]))
    return classes


def find_classes(classes: Iterable[ClassEntry], needle: str) -> list[ClassEntry]:
    needle_lower = needle.lower()
    matches: list[ClassEntry] = []
    for entry in classes:
        if (
            needle_lower in entry.named.lower()
            or needle_lower in entry.official.lower()
            or needle_lower in entry.intermediary.lower()
        ):
            matches.append(entry)
    return matches


def main() -> None:
    parser = argparse.ArgumentParser(description="Query Fabric tiny mappings for class/method info.")
    parser.add_argument("--mapping-file", type=Path, default=Path(".gradle/loom-cache/source_mappings/077c1ca8a67f63c999b4b3c5adee19230b53302c.tiny"))
    parser.add_argument("--class", dest="class_name", required=True, help="Named class to search for (substring match).")
    parser.add_argument("--max-methods", type=int, default=50, help="Limit method output per class (0 for unlimited).")
    args = parser.parse_args()

    if not args.mapping_file.exists():
        parser.error(f"Mapping file not found: {args.mapping_file}")

    all_classes = parse_tiny(args.mapping_file)
    classes = find_classes(all_classes, args.class_name)
    if not classes:
        print(f"No classes matched '{args.class_name}' in {args.mapping_file}")
        return

    for entry in classes:
        print(f"Class: {entry.named}")
        print(f"  Official: {entry.official}")
        print(f"  Intermediary: {entry.intermediary}")
        if not entry.methods:
            print("  (no methods recorded)")
            print()
            continue

        limit = args.max_methods
        for idx, method in enumerate(entry.methods):
            if limit and idx >= limit:
                remaining = len(entry.methods) - idx
                print(f"    ... {remaining} more method(s) omitted (raise --max-methods to show all)")
                break
            print(f"    {method.named} {method.descriptor}")
            print(f"      Official: {method.official}")
            print(f"      Intermediary: {method.intermediary}")
        print()


if __name__ == "__main__":
    main()


