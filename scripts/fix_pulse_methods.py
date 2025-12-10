#!/usr/bin/env python3
"""
Fix broken PulseConfig method names from the first script run.
PulseConfig uses: scale, speed (not amplitude, frequency)
"""

import os
import re

CLIENT_DIR = "src/client/java/net/cyberpunk042/client"

FIXES = [
    # Getters
    (r'\.pulse\(\)\.frequency\(\)', '.pulse().speed()'),
    (r'\.pulse\(\)\.amplitude\(\)', '.pulse().scale()'),
    # Builder setters
    (r'\.pulse\(\)\.toBuilder\(\)\.frequency\(', '.pulse().toBuilder().speed('),
    (r'\.pulse\(\)\.toBuilder\(\)\.amplitude\(', '.pulse().toBuilder().scale('),
]

def fix_file(filepath, dry_run=True):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    changes = []
    
    for pattern, replacement in FIXES:
        matches = re.findall(pattern, content)
        if matches:
            for m in matches:
                changes.append((re.search(pattern, content).group(0), replacement))
            content = re.sub(pattern, replacement, content)
    
    if changes and not dry_run:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
    
    return changes

def main():
    import sys
    dry_run = '--dry-run' in sys.argv
    
    print("=" * 60)
    print("Fixing broken PulseConfig method names")
    if dry_run:
        print("🔍 DRY RUN MODE")
    print("=" * 60)
    
    total = 0
    for root, dirs, files in os.walk(CLIENT_DIR):
        for f in files:
            if f.endswith('.java') and not f.endswith('.bak'):
                filepath = os.path.join(root, f)
                changes = fix_file(filepath, dry_run)
                if changes:
                    print(f"\n📁 {filepath}: {len(changes)} fixes")
                    for old, new in changes:
                        print(f"    - {old}")
                        print(f"    + {new}")
                    total += len(changes)
    
    print(f"\n📊 Total: {total} fixes")
    if dry_run:
        print("🔍 DRY RUN - No files modified")

if __name__ == "__main__":
    main()

