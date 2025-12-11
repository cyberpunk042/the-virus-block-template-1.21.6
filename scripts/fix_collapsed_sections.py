#!/usr/bin/env python3
"""
Fix ExpandableSection panels that start collapsed.
Change false to true for the initiallyExpanded parameter.
"""

import re
from pathlib import Path

PANEL_DIR = Path("src/client/java/net/cyberpunk042/client/gui/panel/sub")

# Panels that need to start expanded (used in sub-tabs)
PANELS_TO_FIX = [
    "AnimationSubPanel.java",
    "AppearanceSubPanel.java",
    "ArrangementSubPanel.java",
    "FillSubPanel.java",
    "TransformSubPanel.java",
    "TriggerSubPanel.java",
    "VisibilitySubPanel.java",
]

def fix_panel(filepath: Path) -> bool:
    """Change ExpandableSection's initiallyExpanded from false to true."""
    content = filepath.read_text(encoding='utf-8')
    original = content
    
    # Pattern to match ExpandableSection constructor with false at the end
    # Match: "SectionName", false  (possibly with comment)
    pattern = r'(new ExpandableSection\([^)]+,\s*"[^"]+"),\s*false(\s*//[^\n]*)?\)'
    replacement = r'\1, true\2)'
    
    content = re.sub(pattern, replacement, content, flags=re.MULTILINE | re.DOTALL)
    
    if content != original:
        filepath.write_text(content, encoding='utf-8')
        return True
    return False

def main():
    print("Fixing collapsed ExpandableSections to start expanded...")
    
    fixed = []
    for filename in PANELS_TO_FIX:
        filepath = PANEL_DIR / filename
        if filepath.exists():
            if fix_panel(filepath):
                fixed.append(filename)
                print(f"Fixed: {filename}")
            else:
                print(f"No change: {filename}")
        else:
            print(f"Not found: {filename}")
    
    print(f"\nFixed {len(fixed)} files")

if __name__ == "__main__":
    main()

