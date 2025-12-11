#!/usr/bin/env python3
"""
Fix widget field shadowing in subpanels - v2.

Only removes the field from classes that extend AbstractPanel.
Restores the field for standalone classes.
"""

import re
from pathlib import Path

PANEL_DIR = Path("src/client/java/net/cyberpunk042/client/gui/panel/sub")

# Panels that DON'T extend AbstractPanel (need their own widgets field)
STANDALONE_PANELS = [
    "LinkingSubPanel.java",
    "PredictionSubPanel.java", 
    "FollowModeSubPanel.java",
    "BindingsSubPanel.java",
    "BeamSubPanel.java",
]

# Field declaration to restore
WIDGETS_FIELD = "    private final List<ClickableWidget> widgets = new ArrayList<>();\n"

def needs_widgets_field(content: str) -> bool:
    """Check if file needs a widgets field (doesn't extend AbstractPanel)."""
    return "extends AbstractPanel" not in content

def has_widgets_field(content: str) -> bool:
    """Check if file already has a widgets field."""
    return re.search(r'List.*ClickableWidget.*widgets\s*=\s*new ArrayList', content) is not None

def find_insert_point(content: str) -> int:
    """Find where to insert the widgets field (after class declaration)."""
    # Find first occurrence of class and opening brace
    match = re.search(r'public class \w+[^{]*\{', content)
    if match:
        return match.end()
    return -1

def restore_widgets_field(filepath: Path) -> bool:
    """Restore widgets field to a standalone panel."""
    content = filepath.read_text(encoding='utf-8')
    
    if has_widgets_field(content):
        print(f"  {filepath.name}: already has widgets field")
        return False
    
    if not needs_widgets_field(content):
        print(f"  {filepath.name}: extends AbstractPanel, skip")
        return False
    
    insert_pos = find_insert_point(content)
    if insert_pos == -1:
        print(f"  {filepath.name}: couldn't find insert point")
        return False
    
    # Insert the field after the class declaration
    new_content = content[:insert_pos] + "\n" + WIDGETS_FIELD + content[insert_pos:]
    filepath.write_text(new_content, encoding='utf-8')
    return True

def main():
    if not PANEL_DIR.exists():
        print(f"Directory not found: {PANEL_DIR}")
        return
    
    print("Restoring widgets field to standalone panels...")
    restored = []
    
    for filename in STANDALONE_PANELS:
        filepath = PANEL_DIR / filename
        if filepath.exists():
            if restore_widgets_field(filepath):
                restored.append(filename)
                print(f"Restored: {filename}")
        else:
            print(f"Not found: {filename}")
    
    print(f"\nRestored {len(restored)} files")

if __name__ == "__main__":
    main()

