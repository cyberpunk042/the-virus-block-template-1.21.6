#!/usr/bin/env python3
"""
Remove ExpandableSection from sub-tab panels.

These panels are inside sub-tabs, so the section header is redundant.
The tab already provides the grouping context.
"""

import re
from pathlib import Path

PANEL_DIR = Path("src/client/java/net/cyberpunk042/client/gui/panel/sub")

# Panels that ARE inside sub-tabs and should NOT have ExpandableSection
SUBTAB_PANELS = [
    "AppearanceSubPanel.java",
    "VisibilitySubPanel.java",
    "AnimationSubPanel.java",
    "TransformSubPanel.java",
    "OrbitSubPanel.java",
    "TriggerSubPanel.java",
    "LifecycleSubPanel.java",
    "ModifiersSubPanel.java",
    "ArrangementSubPanel.java",
    # Already fixed: FillSubPanel.java
    # These need manual review due to complexity:
    # - PredictionSubPanel.java
    # - BeamSubPanel.java
]

def process_panel(filepath: Path) -> bool:
    """Process a single panel file to remove ExpandableSection."""
    content = filepath.read_text(encoding='utf-8')
    original = content
    
    # Check if it has an ExpandableSection
    if "ExpandableSection" not in content:
        print(f"  SKIP: {filepath.name} - no ExpandableSection")
        return False
    
    # 1. Remove section field declaration
    content = re.sub(
        r'\n    private ExpandableSection section;',
        '',
        content
    )
    
    # 2. Remove section creation block (multi-line)
    content = re.sub(
        r'\n        section = new ExpandableSection\(\s*\n[^;]+\);',
        '',
        content,
        flags=re.DOTALL
    )
    # Single line version
    content = re.sub(
        r'\n        section = new ExpandableSection\([^)]+\);',
        '',
        content
    )
    
    # 3. Remove registerSection call
    content = re.sub(
        r'\n        registerSection\(section\);',
        '',
        content
    )
    
    # 4. Replace section.getContentY() with startY + padding
    content = re.sub(
        r'section\.getContentY\(\)',
        'startY + GuiConstants.PADDING',
        content
    )
    
    # 5. Remove section.setContentHeight(...) - replace with contentHeight assignment
    content = re.sub(
        r'section\.setContentHeight\(([^)]+)\);',
        r'contentHeight = \1;',
        content
    )
    
    # 6. Simplify render() - remove section.render() call
    content = re.sub(
        r'section\.render\([^)]+\);',
        '// Render all widgets directly',
        content
    )
    
    # 7. Remove if (section.isExpanded()) wrapper - just keep the body
    content = re.sub(
        r'if \(section\.isExpanded\(\)\) \{\s*\n            for \(var widget : widgets\) widget\.render\([^)]+\);\s*\n        \}',
        'for (var widget : widgets) widget.render(context, mouseX, mouseY, delta);',
        content
    )
    
    # 8. Simplify getWidgets() - remove header button
    content = re.sub(
        r'all\.add\(section\.getHeaderButton\(\)\);',
        '// No header button (direct content)',
        content
    )
    
    # 9. Update getHeight() - remove section reference
    content = re.sub(
        r'return section\.getTotalHeight\(\);',
        'return contentHeight;',
        content
    )
    
    # 10. Remove import if no longer used
    if 'ExpandableSection' not in content.replace('// No header', '').replace('// Render all', ''):
        content = re.sub(
            r'\nimport net\.cyberpunk042\.client\.gui\.widget\.ExpandableSection;',
            '',
            content
        )
    
    if content != original:
        filepath.write_text(content, encoding='utf-8')
        print(f"  MODIFIED: {filepath.name}")
        return True
    else:
        print(f"  UNCHANGED: {filepath.name}")
        return False


def main():
    print("Removing ExpandableSections from sub-tab panels...\n")
    
    modified = 0
    for panel_name in SUBTAB_PANELS:
        filepath = PANEL_DIR / panel_name
        if filepath.exists():
            try:
                if process_panel(filepath):
                    modified += 1
            except Exception as e:
                print(f"  ERROR: {panel_name} - {e}")
        else:
            print(f"  NOT FOUND: {panel_name}")
    
    print(f"\nModified {modified} files")
    print("\nManual review recommended for complex patterns")


if __name__ == "__main__":
    main()
