#!/usr/bin/env python3
"""
Fix widget field shadowing in subpanels.

Problem: Subpanels declare their own `widgets` field which shadows
AbstractPanel.widgets, causing getWidgets() to return empty list.

Solution: Remove the local field declaration - use inherited field.
"""

import os
import re
from pathlib import Path

PANEL_DIR = Path("src/client/java/net/cyberpunk042/client/gui/panel/sub")

# Pattern to match the shadowing field declaration
SHADOW_PATTERNS = [
    r'\s*private final List<net\.minecraft\.client\.gui\.widget\.ClickableWidget> widgets = new ArrayList<>\(\);?\n',
    r'\s*private final List<ClickableWidget> widgets = new ArrayList<>\(\);?\n',
]

def fix_file(filepath: Path) -> bool:
    """Remove shadowing widgets field from a panel file."""
    content = filepath.read_text(encoding='utf-8')
    original = content
    
    for pattern in SHADOW_PATTERNS:
        content = re.sub(pattern, '', content)
    
    if content != original:
        filepath.write_text(content, encoding='utf-8')
        return True
    return False

def main():
    if not PANEL_DIR.exists():
        print(f"Directory not found: {PANEL_DIR}")
        return
    
    fixed = []
    for filepath in PANEL_DIR.glob("*.java"):
        if filepath.name.endswith(".bak"):
            continue
        if fix_file(filepath):
            fixed.append(filepath.name)
            print(f"Fixed: {filepath.name}")
    
    print(f"\nFixed {len(fixed)} files")
    if fixed:
        print("Files modified:")
        for f in sorted(fixed):
            print(f"  - {f}")

if __name__ == "__main__":
    main()

