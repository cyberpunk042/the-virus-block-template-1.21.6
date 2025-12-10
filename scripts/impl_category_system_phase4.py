#!/usr/bin/env python3
"""
Phase 4: Fix callers that use the refactored classes.
Fixes imports and API calls in:
- FieldCustomizerScreen.java
- FieldEditState.java
- PresetConfirmDialog.java
"""

from pathlib import Path
import re

GUI_PACKAGE = Path("src/client/java/net/cyberpunk042/client/gui")

FILES_TO_FIX = [
    GUI_PACKAGE / "screen/FieldCustomizerScreen.java",
    GUI_PACKAGE / "state/FieldEditState.java",
    GUI_PACKAGE / "widget/PresetConfirmDialog.java",
]

# Import fixes: old -> new
IMPORT_FIXES = [
    # Profile moved to field package
    ("import net.cyberpunk042.client.gui.profile.Profile;", 
     "import net.cyberpunk042.field.profile.Profile;"),
    ("import net.cyberpunk042.client.gui.profile.ProfileManager;",
     "import net.cyberpunk042.field.profile.ProfileManager;"),
    # Category enums moved to field package  
    ("import net.cyberpunk042.client.gui.category.PresetCategory;",
     "import net.cyberpunk042.field.category.PresetCategory;"),
    ("import net.cyberpunk042.client.gui.category.ProfileCategory;",
     "import net.cyberpunk042.field.category.ProfileCategory;"),
    ("import net.cyberpunk042.client.gui.category.ProfileSource;",
     "import net.cyberpunk042.field.category.ProfileSource;"),
]

def fix_file(filepath: Path):
    """Fix imports and API calls in a file."""
    if not filepath.exists():
        print(f"⚠️  File not found: {filepath}")
        return False
    
    content = filepath.read_text(encoding='utf-8')
    original = content
    changes = []
    
    # Apply import fixes
    for old, new in IMPORT_FIXES:
        if old in content:
            content = content.replace(old, new)
            changes.append(f"  - Fixed import: {old.split('.')[-1]}")
    
    # Fix PresetRegistry API changes (if this file uses it)
    # Old: PresetRegistry.listPresets() -> New: PresetRegistry.getCategories() + getPresets(cat)
    if "PresetRegistry.listPresets()" in content:
        # This needs manual review - the API changed significantly
        changes.append("  ⚠️  MANUAL: PresetRegistry.listPresets() needs review")
    
    # Old: PresetRegistry.applyPreset(state, name) still exists but signature may differ
    # Old: PresetRegistry.getAffectedCategories(name) still exists
    
    if content != original:
        filepath.write_text(content, encoding='utf-8')
        print(f"✅ Fixed: {filepath}")
        for c in changes:
            print(c)
        return True
    else:
        print(f"ℹ️  No changes needed: {filepath}")
        return False


def add_missing_imports(filepath: Path):
    """Add imports that might be missing after refactor."""
    if not filepath.exists():
        return
    
    content = filepath.read_text(encoding='utf-8')
    
    # Check if file uses Profile but doesn't have import
    if "Profile" in content and "import net.cyberpunk042.field.profile.Profile;" not in content:
        # Find import section and add
        if "import net.cyberpunk042" in content:
            # Add after first cyberpunk042 import
            content = re.sub(
                r"(import net\.cyberpunk042\.[^;]+;)",
                r"\1\nimport net.cyberpunk042.field.profile.Profile;",
                content,
                count=1
            )
            filepath.write_text(content, encoding='utf-8')
            print(f"  + Added Profile import to {filepath.name}")
    
    # Check if file uses ProfileManager but doesn't have import
    if "ProfileManager" in content and "import net.cyberpunk042.field.profile.ProfileManager;" not in content:
        if "import net.cyberpunk042" in content:
            content = re.sub(
                r"(import net\.cyberpunk042\.[^;]+;)",
                r"\1\nimport net.cyberpunk042.field.profile.ProfileManager;",
                content,
                count=1
            )
            filepath.write_text(content, encoding='utf-8')
            print(f"  + Added ProfileManager import to {filepath.name}")


def main():
    print("=" * 60)
    print("PHASE 4: Fix Caller Files")
    print("=" * 60)
    
    fixed = 0
    for filepath in FILES_TO_FIX:
        if fix_file(filepath):
            fixed += 1
        add_missing_imports(filepath)
    
    print()
    print("=" * 60)
    print(f"Phase 4 Complete - Fixed {fixed} files")
    print("=" * 60)
    print()
    print("Next: ./gradlew build to check for remaining errors")
    print()
    print("Expected issues to fix manually:")
    print("  - PresetRegistry API changed (listPresets -> getCategories/getPresets)")
    print("  - BottomActionBar constructor changed")
    print("  - ProfilesPanel constructor changed")


if __name__ == "__main__":
    main()

