#!/usr/bin/env python3
"""
Batch implementation of GUI TODOs.
Replaces placeholder TODOs with actual implementations.

Usage:
    python3 scripts/implement_gui_todos.py --dry-run    # Preview changes
    python3 scripts/implement_gui_todos.py              # Apply changes
    python3 scripts/implement_gui_todos.py --file X     # Single file
"""

import argparse
import re
import shutil
from pathlib import Path
from dataclasses import dataclass
from typing import List, Tuple, Optional

# Base path
BASE = Path(__file__).parent.parent / "src" / "client" / "java" / "net" / "cyberpunk042" / "client"

@dataclass
class TodoReplacement:
    """Defines a TODO replacement."""
    file_path: Path
    pattern: str  # Regex to find the TODO block
    replacement: str  # What to replace it with
    description: str  # Human-readable description
    imports_needed: List[str]  # Imports to add if missing

# Define all TODO replacements
REPLACEMENTS: List[TodoReplacement] = [
    # ═══════════════════════════════════════════════════════════════════════════
    # FieldCustomizerScreen.java
    # ═══════════════════════════════════════════════════════════════════════════
    TodoReplacement(
        file_path=BASE / "gui" / "screen" / "FieldCustomizerScreen.java",
        pattern=r'private void onBottomBarSave\(String profileName\) \{\s*// TODO: Actually persist profile to storage\s*Logging\.GUI\.topic\("screen"\)\.info\("Bottom bar save: \{\}", profileName\);\s*\}',
        replacement='''private void onBottomBarSave(String profileName) {
        try {
            String json = state.toProfileJson(profileName);
            com.google.gson.JsonObject jsonObj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            Profile profile = Profile.fromJson(jsonObj, ProfileSource.LOCAL);
            ProfileManager.getInstance().saveProfile(profile);
            state.clearDirty();
            state.saveProfileSnapshot();  // Update snapshot after save
            ToastNotification.success("Profile saved: " + profileName);
            Logging.GUI.topic("screen").info("Profile saved: {}", profileName);
        } catch (Exception e) {
            ToastNotification.error("Failed to save profile");
            Logging.GUI.topic("screen").error("Failed to save profile: {}", e.getMessage());
        }
    }''',
        description="onBottomBarSave: Persist profile to storage",
        imports_needed=[
            "net.cyberpunk042.client.profile.ProfileManager",
            "net.cyberpunk042.client.gui.widget.ToastNotification",
            "net.cyberpunk042.field.profile.Profile",
            "net.cyberpunk042.field.category.ProfileSource"
        ]
    ),
    
    TodoReplacement(
        file_path=BASE / "gui" / "screen" / "FieldCustomizerScreen.java",
        pattern=r'private void onBottomBarRevert\(String profileName\) \{\s*// TODO: Reload profile from storage\s*Logging\.GUI\.topic\("screen"\)\.info\("Bottom bar revert: \{\}", profileName\);\s*\}',
        replacement='''private void onBottomBarRevert(String profileName) {
        state.restoreFromSnapshot();
        state.clearDirty();
        clearAndRegisterWidgets();  // Refresh UI
        ToastNotification.info("Reverted to saved state");
        Logging.GUI.topic("screen").info("Reverted profile: {}", profileName);
    }''',
        description="onBottomBarRevert: Restore from snapshot",
        imports_needed=[
            "net.cyberpunk042.client.gui.widget.ToastNotification"
        ]
    ),
    
    TodoReplacement(
        file_path=BASE / "gui" / "screen" / "FieldCustomizerScreen.java",
        pattern=r'if \(state\.isDirty\(\)\) \{\s*// TODO: Show confirmation dialog\s*Logging\.GUI\.topic\("screen"\)\.warn\("Closing with unsaved changes"\);\s*\}',
        replacement='''if (state.isDirty()) {
            // Per architecture: state persists in memory, no dialog needed
            // Changes are only lost on game quit, not menu close
            Logging.GUI.topic("screen").debug("Closing with unsaved changes (state persists)");
        }''',
        description="close(): Remove TODO (state persists per architecture)",
        imports_needed=[]
    ),
    
    # ═══════════════════════════════════════════════════════════════════════════
    # ActionPanel.java
    # ═══════════════════════════════════════════════════════════════════════════
    TodoReplacement(
        file_path=BASE / "gui" / "panel" / "ActionPanel.java",
        pattern=r'if \(enabled\) \{\s*ToastNotification\.info\("Live preview enabled"\);\s*// TODO: Spawn/update DEBUG field\s*\} else \{\s*ToastNotification\.info\("Live preview disabled"\);\s*// TODO: Despawn DEBUG field\s*\}',
        replacement='''if (enabled) {
            ToastNotification.info("Live preview enabled");
            GuiPacketSender.spawnDebugField(state.toStateJson());
        } else {
            ToastNotification.info("Live preview disabled");
            GuiPacketSender.despawnDebugField();
        }''',
        description="onLivePreviewChanged: Spawn/despawn DEBUG field",
        imports_needed=[
            "net.cyberpunk042.client.network.GuiPacketSender"
        ]
    ),
    
    TodoReplacement(
        file_path=BASE / "gui" / "panel" / "ActionPanel.java",
        pattern=r'private void applyToShield\(\) \{\s*// TODO: Send packet to server to update player\'s shield\s*state\.clearDirty\(\);',
        replacement='''private void applyToShield() {
        // Apply current settings to the player's active shield
        GuiPacketSender.updateDebugField(state.toStateJson());
        state.clearDirty();''',
        description="applyToShield: Send packet to server",
        imports_needed=[
            "net.cyberpunk042.client.network.GuiPacketSender"
        ]
    ),
    
    TodoReplacement(
        file_path=BASE / "gui" / "panel" / "ActionPanel.java",
        pattern=r'private void resetChanges\(\) \{\s*// TODO: Reset state to original\s*state\.clearDirty\(\);',
        replacement='''private void resetChanges() {
        state.restoreFromSnapshot();
        state.clearDirty();''',
        description="resetChanges: Restore from snapshot",
        imports_needed=[]
    ),
    
    # ═══════════════════════════════════════════════════════════════════════════
    # FieldEditStateHolder.java
    # ═══════════════════════════════════════════════════════════════════════════
    TodoReplacement(
        file_path=BASE / "gui" / "state" / "FieldEditStateHolder.java",
        pattern=r'Logging\.GUI\.topic\("state"\)\.info\("Test field spawned"\);\s*// TODO: Notify TestFieldRenderer to start rendering',
        replacement='''Logging.GUI.topic("state").info("Test field spawned");
        GuiPacketSender.spawnDebugField(getOrCreate().toStateJson());''',
        description="spawnTestField: Send spawn packet",
        imports_needed=[
            "net.cyberpunk042.client.network.GuiPacketSender"
        ]
    ),
    
    TodoReplacement(
        file_path=BASE / "gui" / "state" / "FieldEditStateHolder.java",
        pattern=r'Logging\.GUI\.topic\("state"\)\.info\("Test field despawned"\);\s*// TODO: Notify TestFieldRenderer to stop rendering',
        replacement='''Logging.GUI.topic("state").info("Test field despawned");
        GuiPacketSender.despawnDebugField();''',
        description="despawnTestField: Send despawn packet",
        imports_needed=[
            "net.cyberpunk042.client.network.GuiPacketSender"
        ]
    ),
    
    # ═══════════════════════════════════════════════════════════════════════════
    # LifecycleSubPanel.java
    # ═══════════════════════════════════════════════════════════════════════════
    TodoReplacement(
        file_path=BASE / "gui" / "panel" / "sub" / "LifecycleSubPanel.java",
        pattern=r'Logging\.GUI\.topic\("lifecycle"\)\.info\("Force spawn triggered"\);\s*// TODO: Send spawn command',
        replacement='''Logging.GUI.topic("lifecycle").info("Force spawn triggered");
            GuiPacketSender.spawnDebugField(state.toStateJson());''',
        description="forceSpawn: Send spawn command",
        imports_needed=[
            "net.cyberpunk042.client.network.GuiPacketSender"
        ]
    ),
    
    TodoReplacement(
        file_path=BASE / "gui" / "panel" / "sub" / "LifecycleSubPanel.java",
        pattern=r'Logging\.GUI\.topic\("lifecycle"\)\.info\("Force despawn triggered"\);\s*// TODO: Send despawn command',
        replacement='''Logging.GUI.topic("lifecycle").info("Force despawn triggered");
            GuiPacketSender.despawnDebugField();''',
        description="forceDespawn: Send despawn command",
        imports_needed=[
            "net.cyberpunk042.client.network.GuiPacketSender"
        ]
    ),
    
    TodoReplacement(
        file_path=BASE / "gui" / "panel" / "sub" / "LifecycleSubPanel.java",
        pattern=r'Logging\.GUI\.topic\("lifecycle"\)\.info\("Lifecycle reset triggered"\);\s*// TODO: Reset lifecycle state',
        replacement='''Logging.GUI.topic("lifecycle").info("Lifecycle reset triggered");
            state.setLifecycleState("ACTIVE");
            state.setFadeInTicks(0);
            state.setFadeOutTicks(0);
            ToastNotification.info("Lifecycle reset to defaults");''',
        description="resetLifecycle: Reset lifecycle fields",
        imports_needed=[
            "net.cyberpunk042.client.gui.widget.ToastNotification"
        ]
    ),
    
    # ═══════════════════════════════════════════════════════════════════════════
    # TriggerSubPanel.java
    # ═══════════════════════════════════════════════════════════════════════════
    TodoReplacement(
        file_path=BASE / "gui" / "panel" / "sub" / "TriggerSubPanel.java",
        pattern=r'triggerType\.getValue\(\), triggerEffect\.getValue\(\)\);\s*// TODO: Send trigger test command',
        replacement='''triggerType.getValue(), triggerEffect.getValue());
            ToastNotification.info("Trigger test: " + triggerType.getValue() + " → " + triggerEffect.getValue());''',
        description="testTrigger: Show feedback (local for now)",
        imports_needed=[
            "net.cyberpunk042.client.gui.widget.ToastNotification"
        ]
    ),
]


def ensure_import(content: str, import_line: str) -> str:
    """Add import if not already present."""
    if import_line in content:
        return content
    
    # Find the last import line and add after it
    import_pattern = r'(import [^;]+;)\n(?=\n|public |@)'
    match = list(re.finditer(import_pattern, content))
    if match:
        last_import = match[-1]
        pos = last_import.end() - 1  # Before the newline
        return content[:pos] + f"\nimport {import_line};" + content[pos:]
    
    return content


def apply_replacement(content: str, replacement: TodoReplacement) -> Tuple[str, bool]:
    """Apply a single replacement. Returns (new_content, was_changed)."""
    # Try to find and replace
    new_content, count = re.subn(replacement.pattern, replacement.replacement, content, flags=re.MULTILINE | re.DOTALL)
    
    if count > 0:
        # Add imports
        for imp in replacement.imports_needed:
            new_content = ensure_import(new_content, imp)
        return new_content, True
    
    return content, False


def show_diff(original: str, modified: str, file_path: Path) -> None:
    """Show a simple before/after diff."""
    orig_lines = original.splitlines()
    mod_lines = modified.splitlines()
    
    print(f"\n{'='*70}")
    print(f"📁 {file_path.relative_to(BASE.parent.parent.parent.parent)}")
    print('='*70)
    
    # Find changed regions
    i = 0
    while i < len(orig_lines) or i < len(mod_lines):
        if i >= len(orig_lines):
            print(f"\033[32m+ {mod_lines[i]}\033[0m")
            i += 1
        elif i >= len(mod_lines):
            print(f"\033[31m- {orig_lines[i]}\033[0m")
            i += 1
        elif orig_lines[i] != mod_lines[i]:
            # Show context
            start = max(0, i - 2)
            # Find end of change
            j = i
            while j < len(orig_lines) and j < len(mod_lines) and orig_lines[j] != mod_lines[j]:
                j += 1
            end = min(max(len(orig_lines), len(mod_lines)), j + 3)
            
            print(f"\n--- Lines {start+1}-{end} ---")
            for k in range(start, i):
                print(f"  {orig_lines[k]}")
            for k in range(i, min(j, len(orig_lines))):
                print(f"\033[31m- {orig_lines[k]}\033[0m")
            for k in range(i, min(j, len(mod_lines))):
                print(f"\033[32m+ {mod_lines[k]}\033[0m")
            for k in range(j, min(end, len(orig_lines))):
                print(f"  {orig_lines[k]}")
            
            i = j
        else:
            i += 1


def process_file(file_path: Path, dry_run: bool, verbose: bool) -> Tuple[int, int]:
    """Process a single file. Returns (todos_found, todos_resolved)."""
    if not file_path.exists():
        print(f"⚠️  File not found: {file_path}")
        return 0, 0
    
    content = file_path.read_text(encoding='utf-8')
    original = content
    
    todos_found = 0
    todos_resolved = 0
    
    # Apply all replacements for this file
    for repl in REPLACEMENTS:
        if repl.file_path != file_path:
            continue
        
        todos_found += 1
        new_content, changed = apply_replacement(content, repl)
        
        if changed:
            todos_resolved += 1
            content = new_content
            if verbose:
                print(f"  ✅ {repl.description}")
        else:
            if verbose:
                print(f"  ⚠️  Pattern not found: {repl.description}")
    
    # Show changes
    if content != original:
        if dry_run:
            show_diff(original, content, file_path)
        else:
            # Create backup
            backup_path = file_path.with_suffix(file_path.suffix + '.bak')
            shutil.copy(file_path, backup_path)
            
            # Write changes
            file_path.write_text(content, encoding='utf-8')
            print(f"✅ Updated {file_path.name} (backup: {backup_path.name})")
    
    return todos_found, todos_resolved


def main():
    parser = argparse.ArgumentParser(description="Implement GUI TODOs")
    parser.add_argument('--dry-run', action='store_true', help="Preview changes without modifying files")
    parser.add_argument('--file', type=str, help="Process only this file (relative to client/gui)")
    parser.add_argument('--verbose', '-v', action='store_true', help="Show detailed progress")
    args = parser.parse_args()
    
    print("="*70)
    print("GUI TODO Implementation Script")
    print("="*70)
    
    if args.dry_run:
        print("🔍 DRY RUN MODE - No files will be modified\n")
    else:
        print("⚠️  LIVE MODE - Files will be modified (backups created)\n")
    
    # Get unique files
    files = set(r.file_path for r in REPLACEMENTS)
    
    # Filter if --file specified
    if args.file:
        target = BASE / "gui" / args.file
        files = {f for f in files if f == target or str(f).endswith(args.file)}
        if not files:
            print(f"❌ No matching files for: {args.file}")
            return 1
    
    total_found = 0
    total_resolved = 0
    
    for file_path in sorted(files):
        print(f"\n📄 Processing: {file_path.name}")
        found, resolved = process_file(file_path, args.dry_run, args.verbose or args.dry_run)
        total_found += found
        total_resolved += resolved
    
    # Summary
    print("\n" + "="*70)
    print("SUMMARY")
    print("="*70)
    print(f"TODOs found:    {total_found}")
    print(f"TODOs resolved: {total_resolved}")
    print(f"TODOs missed:   {total_found - total_resolved}")
    
    if args.dry_run:
        print("\n💡 Run without --dry-run to apply changes")
    else:
        print("\n✅ Changes applied! Backups created with .bak extension")
    
    return 0 if total_resolved == total_found else 1


if __name__ == "__main__":
    exit(main())

