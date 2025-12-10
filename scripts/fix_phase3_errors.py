#!/usr/bin/env python3
"""
Fix Phase 3 errors - the rewrites broke API compatibility.
Strategy: Restore original files from git, then selectively add category imports.
"""

import subprocess
from pathlib import Path

FILES_TO_RESTORE = [
    "src/client/java/net/cyberpunk042/client/gui/panel/ProfilesPanel.java",
    "src/client/java/net/cyberpunk042/client/gui/widget/BottomActionBar.java",
]

# PresetRegistry needs getDescription method added back
PRESET_REGISTRY_PATH = Path("src/client/java/net/cyberpunk042/client/gui/util/PresetRegistry.java")

def restore_from_git():
    """Try to restore files from git."""
    print("=== Restoring Original Files from Git ===")
    
    for filepath in FILES_TO_RESTORE:
        try:
            result = subprocess.run(
                ["git", "checkout", "HEAD~1", "--", filepath],
                capture_output=True, text=True
            )
            if result.returncode == 0:
                print(f"✅ Restored: {filepath}")
            else:
                print(f"⚠️  Could not restore {filepath}: {result.stderr}")
        except Exception as e:
            print(f"❌ Error restoring {filepath}: {e}")


def fix_preset_registry():
    """Add missing getDescription method to PresetRegistry."""
    print("\n=== Fixing PresetRegistry ===")
    
    if not PRESET_REGISTRY_PATH.exists():
        print("⚠️  PresetRegistry not found")
        return
    
    content = PRESET_REGISTRY_PATH.read_text(encoding='utf-8')
    
    # Check if getDescription already exists
    if "public static String getDescription(" in content:
        print("✅ getDescription already exists")
        return
    
    # Add getDescription method before the last closing brace
    method = '''
    /**
     * Get description for a preset.
     */
    public static String getDescription(String presetId) {
        return getPreset(presetId)
            .map(PresetEntry::description)
            .orElse("");
    }
'''
    
    # Find the last } and insert before it
    last_brace = content.rfind("}")
    if last_brace > 0:
        content = content[:last_brace] + method + "\n}\n"
        PRESET_REGISTRY_PATH.write_text(content, encoding='utf-8')
        print("✅ Added getDescription method")


def main():
    print("=" * 60)
    print("FIX PHASE 3 ERRORS")
    print("=" * 60)
    
    restore_from_git()
    fix_preset_registry()
    
    print()
    print("=" * 60)
    print("Done!")
    print("=" * 60)
    print()
    print("Next: ./gradlew build")
    print()
    print("Note: If git restore didn't work, manually revert")
    print("ProfilesPanel.java and BottomActionBar.java")


if __name__ == "__main__":
    main()

