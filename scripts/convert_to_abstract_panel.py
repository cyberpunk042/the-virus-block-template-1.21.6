#!/usr/bin/env python3
"""
Convert standalone panels to extend AbstractPanel.

This is a complex refactoring - we'll do it manually for each file
since they have different structures. This script just adds the
necessary imports and extends clause.
"""

import re
from pathlib import Path

PANEL_DIR = Path("src/client/java/net/cyberpunk042/client/gui/panel/sub")

# Files to convert
STANDALONE_PANELS = {
    "BeamSubPanel.java": "BeamSubPanel",
    "LinkingSubPanel.java": "LinkingSubPanel", 
    "PredictionSubPanel.java": "PredictionSubPanel",
    "FollowModeSubPanel.java": "FollowModeSubPanel",
    "BindingsSubPanel.java": "BindingsSubPanel",
}

# Import to add
ABSTRACT_PANEL_IMPORT = "import net.cyberpunk042.client.gui.panel.AbstractPanel;\nimport net.minecraft.client.gui.screen.Screen;\n"

def convert_panel(filepath: Path) -> bool:
    """Convert a standalone panel to extend AbstractPanel."""
    content = filepath.read_text(encoding='utf-8')
    original = content
    
    # Skip if already extends AbstractPanel
    if "extends AbstractPanel" in content:
        print(f"  {filepath.name}: already extends AbstractPanel")
        return False
    
    # Remove the local widgets field if present (AbstractPanel has it)
    content = re.sub(
        r'\s*private final List<ClickableWidget> widgets = new ArrayList<>\(\);\s*\n',
        '\n',
        content
    )
    
    # Add AbstractPanel import after package statement
    if "import net.cyberpunk042.client.gui.panel.AbstractPanel;" not in content:
        content = re.sub(
            r'(package [^;]+;\n)',
            r'\1\n' + ABSTRACT_PANEL_IMPORT,
            content
        )
    
    # Change class declaration to extend AbstractPanel
    content = re.sub(
        r'public class (\w+SubPanel) \{',
        r'public class \1 extends AbstractPanel {',
        content
    )
    
    if content != original:
        filepath.write_text(content, encoding='utf-8')
        return True
    return False

def main():
    print("Converting standalone panels to extend AbstractPanel...")
    
    converted = []
    for filename in STANDALONE_PANELS:
        filepath = PANEL_DIR / filename
        if filepath.exists():
            if convert_panel(filepath):
                converted.append(filename)
                print(f"Converted: {filename}")
        else:
            print(f"Not found: {filename}")
    
    print(f"\nConverted {len(converted)} files")
    print("\nNOTE: Manual fixes still needed:")
    print("  1. Add 'Screen parent' parameter to constructor")
    print("  2. Call super(parent, state) in constructor")
    print("  3. Use inherited 'widgets' field instead of local")
    print("  4. Adapt init() method signature")

if __name__ == "__main__":
    main()

