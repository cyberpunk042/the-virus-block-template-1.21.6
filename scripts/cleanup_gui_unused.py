#!/usr/bin/env python3
"""
GUI Unused Code Cleanup Script

Based on the audit from audit_gui_unused.py, this script can:
1. Remove backup files (safe)
2. Remove/deprecate unused v2 panel classes
3. Optionally remove LayoutPanel if v2 is removed

Run with --dry-run first to see what would be deleted:
  python scripts/cleanup_gui_unused.py --dry-run

Then actually perform cleanup:
  python scripts/cleanup_gui_unused.py --execute
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
from pathlib import Path
from datetime import datetime

PROJECT_ROOT = Path(__file__).parent.parent
GUI_DIR = PROJECT_ROOT / "src/client/java/net/cyberpunk042/client/gui"
REPORT_PATH = PROJECT_ROOT / "scripts/gui_unused_audit_report.json"
ARCHIVE_DIR = PROJECT_ROOT / "scripts/deprecated_gui_archive"


def load_audit_report() -> dict:
    """Load the audit report JSON"""
    if not REPORT_PATH.exists():
        print("❌ Audit report not found! Run audit_gui_unused.py first.")
        exit(1)
    
    with open(REPORT_PATH, 'r', encoding='utf-8') as f:
        return json.load(f)


def remove_backup_files(report: dict, dry_run: bool) -> int:
    """Remove .bak files that have originals"""
    count = 0
    seen = set()  # Deduplicate the backup file list
    
    print("\n🗑️  REMOVING BACKUP FILES")
    print("-" * 40)
    
    for backup in report.get('backup_files', []):
        path = PROJECT_ROOT / backup['path']
        if str(path) in seen:
            continue
        seen.add(str(path))
        
        if not path.exists():
            continue
        
        if backup['original_exists']:
            if dry_run:
                print(f"   [DRY-RUN] Would remove: {backup['path']}")
            else:
                path.unlink()
                print(f"   ✓ Removed: {backup['path']}")
            count += 1
        else:
            print(f"   ⚠️ Skipping (no original): {backup['path']}")
    
    print(f"\n   Total: {count} backup files {'would be' if dry_run else ''} removed")
    return count


def archive_v2_panels(report: dict, dry_run: bool) -> int:
    """Archive the entire v2 panel directory"""
    v2_dir = GUI_DIR / "panel/v2"
    
    print("\n📦 ARCHIVING V2 PANELS (unused)")
    print("-" * 40)
    
    if not v2_dir.exists():
        print("   ✓ v2/ directory doesn't exist")
        return 0
    
    # Calculate lines
    total_lines = sum(u['line_count'] for u in report.get('unused_classes', []) 
                      if 'panel/v2/' in u.get('path', ''))
    file_count = len([u for u in report.get('unused_classes', []) 
                      if 'panel/v2/' in u.get('path', '')])
    
    print(f"   Found {file_count} unused v2 panel classes ({total_lines} lines)")
    
    if dry_run:
        print(f"   [DRY-RUN] Would archive {v2_dir.relative_to(PROJECT_ROOT)}")
        for f in v2_dir.glob('*.java'):
            print(f"      - {f.name}")
    else:
        # Create archive directory with timestamp
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        archive_path = ARCHIVE_DIR / f"panel_v2_{timestamp}"
        archive_path.mkdir(parents=True, exist_ok=True)
        
        # Move files to archive
        for f in v2_dir.glob('*.java'):
            shutil.move(str(f), str(archive_path / f.name))
            print(f"   ✓ Archived: {f.name}")
        
        # Remove the now-empty v2 directory
        if not any(v2_dir.iterdir()):
            v2_dir.rmdir()
            print(f"   ✓ Removed empty directory: panel/v2/")
        
        print(f"\n   Archived to: {archive_path.relative_to(PROJECT_ROOT)}")
    
    return file_count


def check_layout_panel_usage(dry_run: bool) -> bool:
    """Check if LayoutPanel can also be removed after v2 cleanup"""
    layout_panel = GUI_DIR / "layout/LayoutPanel.java"
    
    print("\n🔍 CHECKING LAYOUTPANEL USAGE")
    print("-" * 40)
    
    if not layout_panel.exists():
        print("   LayoutPanel.java not found")
        return False
    
    # After v2 removal, check if anything else uses LayoutPanel
    v2_dir = GUI_DIR / "panel/v2"
    has_other_usage = False
    
    for java_file in GUI_DIR.rglob('*.java'):
        if '.bak' in str(java_file):
            continue
        if 'panel/v2/' in str(java_file):
            continue
        if java_file.name == 'LayoutPanel.java':
            continue
        
        try:
            content = java_file.read_text(encoding='utf-8', errors='ignore')
            if 'extends LayoutPanel' in content:
                print(f"   ⚠️ LayoutPanel extended by: {java_file.relative_to(GUI_DIR)}")
                has_other_usage = True
            elif 'LayoutPanel' in content and 'import' not in content.split('LayoutPanel')[0][-50:]:
                # Check for actual usage (not just import)
                if 'new LayoutPanel' in content or 'LayoutPanel ' in content:
                    has_other_usage = True
        except:
            pass
    
    if has_other_usage:
        print("   ❌ LayoutPanel is used by files outside v2/, cannot remove")
        return False
    else:
        print("   ✅ LayoutPanel is only used by v2 panels, can be deprecated")
        if not dry_run:
            # Just add deprecation annotation for now
            print("   💡 Consider adding @Deprecated to LayoutPanel.java")
        return True


def update_subtabpane_imports(dry_run: bool):
    """Remove LayoutPanel import from SubTabPane if v2 is removed"""
    subtabpane = GUI_DIR / "widget/SubTabPane.java"
    
    print("\n🔧 UPDATING SUBTABPANE")
    print("-" * 40)
    
    if not subtabpane.exists():
        print("   SubTabPane.java not found")
        return
    
    try:
        content = subtabpane.read_text(encoding='utf-8', errors='ignore')
        if 'import net.cyberpunk042.client.gui.layout.LayoutPanel;' in content:
            print("   Found LayoutPanel import in SubTabPane")
            if dry_run:
                print("   [DRY-RUN] Would need to update SubTabPane to remove v2 support")
            else:
                print("   💡 SubTabPane has LayoutPanel support - can be refactored later")
    except Exception as e:
        print(f"   Error: {e}")


def generate_summary(report: dict, backups_removed: int, v2_archived: int, dry_run: bool):
    """Print final summary"""
    print("\n" + "=" * 60)
    print("📊 CLEANUP SUMMARY")
    print("=" * 60)
    
    mode = "[DRY-RUN]" if dry_run else "[EXECUTED]"
    
    print(f"""
   {mode}
   
   Backup files removed:     {backups_removed}
   v2 panels archived:       {v2_archived}
   
   Space saved:
   - Backup files:           ~{report['summary']['backup_files_size_kb']:.1f} KB
   - Unused classes:         ~{report['summary']['unused_classes_lines']} lines
   
   Total cleanup:            ~{report['summary']['cleanup_potential_lines']} lines
""")
    
    if dry_run:
        print("   Run with --execute to perform cleanup")
    else:
        print("   ✅ Cleanup complete!")
    
    print("=" * 60)


def main():
    parser = argparse.ArgumentParser(description='Clean up unused GUI code')
    parser.add_argument('--dry-run', action='store_true', default=True,
                        help='Show what would be done without making changes (default)')
    parser.add_argument('--execute', action='store_true',
                        help='Actually perform the cleanup')
    
    args = parser.parse_args()
    dry_run = not args.execute
    
    print("=" * 60)
    print("🧹 GUI UNUSED CODE CLEANUP")
    print("=" * 60)
    
    if dry_run:
        print("\n🔍 DRY-RUN MODE - No changes will be made")
    else:
        print("\n⚡ EXECUTE MODE - Changes will be made!")
        response = input("   Continue? [y/N]: ")
        if response.lower() != 'y':
            print("   Aborted.")
            return
    
    report = load_audit_report()
    
    # Step 1: Remove backup files
    backups_removed = remove_backup_files(report, dry_run)
    
    # Step 2: Archive v2 panels
    v2_archived = archive_v2_panels(report, dry_run)
    
    # Step 3: Check LayoutPanel
    check_layout_panel_usage(dry_run)
    
    # Step 4: Note SubTabPane update
    update_subtabpane_imports(dry_run)
    
    # Summary
    generate_summary(report, backups_removed, v2_archived, dry_run)


if __name__ == "__main__":
    main()
