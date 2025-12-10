#!/usr/bin/env python3
"""
Fix the category system scripts - correct all wrong package references.
Fixes: impl_category_system_phase2.py, impl_category_system_phase3.py
"""

from pathlib import Path

SCRIPTS_TO_FIX = [
    "scripts/impl_category_system_phase2.py",
    "scripts/impl_category_system_phase3.py",
]

# Wrong → Correct replacements
REPLACEMENTS = [
    # Package declarations
    ("package net.cyberpunk042.client.gui.category;", "package net.cyberpunk042.field.category;"),
    ("package net.cyberpunk042.client.gui.profile;", "package net.cyberpunk042.field.profile;"),
    
    # Import statements
    ("import net.cyberpunk042.client.gui.category.PresetCategory;", "import net.cyberpunk042.field.category.PresetCategory;"),
    ("import net.cyberpunk042.client.gui.category.ProfileCategory;", "import net.cyberpunk042.field.category.ProfileCategory;"),
    ("import net.cyberpunk042.client.gui.category.ProfileSource;", "import net.cyberpunk042.field.category.ProfileSource;"),
    ("import net.cyberpunk042.client.gui.profile.Profile;", "import net.cyberpunk042.field.profile.Profile;"),
    ("import net.cyberpunk042.client.gui.profile.ProfileManager;", "import net.cyberpunk042.field.profile.ProfileManager;"),
    
    # Path definitions in Phase 2
    ('PROFILE_PACKAGE = GUI_PACKAGE / "profile"', 'PROFILE_PACKAGE = MAIN_PACKAGE / "field" / "profile"'),
]

# Additional path fixes for phase 2
PHASE2_PATH_FIX = '''# Paths
MAIN_PACKAGE = Path("src/main/java/net/cyberpunk042")
FIELD_PACKAGE = MAIN_PACKAGE / "field"
GUI_PACKAGE = Path("src/client/java/net/cyberpunk042/client/gui")
UTIL_PACKAGE = GUI_PACKAGE / "util"
PROFILE_PACKAGE = FIELD_PACKAGE / "profile"'''

PHASE2_OLD_PATH = '''# Paths
GUI_PACKAGE = Path("src/client/java/net/cyberpunk042/client/gui")
UTIL_PACKAGE = GUI_PACKAGE / "util"
PROFILE_PACKAGE = GUI_PACKAGE / "profile"'''

def fix_script(filepath: str):
    """Fix a single script."""
    path = Path(filepath)
    if not path.exists():
        print(f"⚠️  Script not found: {filepath}")
        return False
    
    content = path.read_text(encoding='utf-8')
    original = content
    
    # Apply all replacements
    for old, new in REPLACEMENTS:
        content = content.replace(old, new)
    
    # Special fix for phase 2 paths
    if "phase2" in filepath:
        content = content.replace(PHASE2_OLD_PATH, PHASE2_PATH_FIX)
    
    if content != original:
        path.write_text(content, encoding='utf-8')
        print(f"✅ Fixed: {filepath}")
        return True
    else:
        print(f"⚠️  No changes needed: {filepath}")
        return False


def main():
    print("=" * 60)
    print("FIXING CATEGORY SYSTEM SCRIPTS")
    print("=" * 60)
    
    fixed = 0
    for script in SCRIPTS_TO_FIX:
        if fix_script(script):
            fixed += 1
    
    print()
    print("=" * 60)
    print(f"Fixed {fixed}/{len(SCRIPTS_TO_FIX)} scripts")
    print("=" * 60)
    print()
    print("Next: Re-run audit to verify fixes")


if __name__ == "__main__":
    main()

