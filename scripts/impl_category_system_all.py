#!/usr/bin/env python3
"""
Master script: Run all phases of the category system implementation.
"""

import subprocess
import sys
from pathlib import Path

SCRIPTS = [
    "scripts/impl_category_system_phase1.py",
    "scripts/impl_category_system_phase2.py", 
    "scripts/impl_category_system_phase3.py",
]

def main():
    print("=" * 70)
    print("CATEGORY SYSTEM IMPLEMENTATION - ALL PHASES")
    print("=" * 70)
    
    for i, script in enumerate(SCRIPTS, 1):
        print(f"\n{'─' * 70}")
        print(f"Running Phase {i}: {script}")
        print("─" * 70)
        
        result = subprocess.run([sys.executable, script], capture_output=False)
        if result.returncode != 0:
            print(f"\n❌ Phase {i} failed!")
            sys.exit(1)
    
    print("\n" + "=" * 70)
    print("✅ ALL PHASES COMPLETE!")
    print("=" * 70)
    print()
    print("Files created/updated:")
    print()
    print("Phase 1 - Enums & Data:")
    print("  ✅ client/gui/category/PresetCategory.java (new)")
    print("  ✅ client/gui/category/ProfileCategory.java (new)")
    print("  ✅ client/gui/category/ProfileSource.java (new)")
    print("  ✅ client/gui/profile/Profile.java (rewritten)")
    print()
    print("Phase 2 - Registries:")
    print("  ✅ client/gui/util/PresetRegistry.java (rewritten)")
    print("  ✅ client/gui/profile/ProfileManager.java (new)")
    print()
    print("Phase 3 - GUI Components:")
    print("  ✅ client/gui/widget/BottomActionBar.java (rewritten)")
    print("  ✅ client/gui/panel/ProfilesPanel.java (rewritten)")
    print()
    print("Next steps:")
    print("  1. ./gradlew build  (check for compile errors)")
    print("  2. Fix any import issues")
    print("  3. Create preset JSON files in config/the-virus-block/field_presets/")
    print("  4. Test in-game")


if __name__ == "__main__":
    main()

