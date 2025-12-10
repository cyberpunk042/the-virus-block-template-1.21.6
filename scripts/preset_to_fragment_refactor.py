#!/usr/bin/env python3
"""
Preset → Fragment Refactor Analysis & Execution

This script:
1. Analyzes files that need renaming (Preset → Fragment for single-scope)
2. Identifies what stays as "Preset" (new multi-scope concept)
3. Generates a report
4. Optionally executes the refactor

Taxonomy:
- Profile = Complete field definition (no change)
- Preset = Multi-scope partial merge (NEW concept)
- Fragment = Single-scope $ref target (was "Preset")
"""

import os
import re
import json
from pathlib import Path
from collections import defaultdict
from dataclasses import dataclass, field
from typing import List, Set, Dict, Tuple

# Project root
PROJECT_ROOT = Path(__file__).parent.parent
SRC_DIR = PROJECT_ROOT / "src"
CONFIG_DIR = PROJECT_ROOT / "config" / "the-virus-block"
DOCS_DIR = PROJECT_ROOT / "docs" / "field-system" / "NEW_REFACTORING_NEW_PHASES"

@dataclass
class RefactorItem:
    file_path: Path
    line_number: int
    old_text: str
    new_text: str
    context: str  # surrounding code for review

@dataclass
class AnalysisReport:
    # Files to refactor (Preset → Fragment)
    fragment_renames: List[RefactorItem] = field(default_factory=list)
    
    # Files that should keep "Preset" (new multi-scope concept)
    preset_keeps: List[Tuple[Path, str]] = field(default_factory=list)
    
    # New files to create
    new_files: List[Path] = field(default_factory=list)
    
    # Documentation updates needed
    doc_updates: List[Path] = field(default_factory=list)

def analyze_java_file(file_path: Path, report: AnalysisReport):
    """Analyze a Java file for Preset references."""
    content = file_path.read_text(encoding='utf-8')
    lines = content.split('\n')
    
    # Patterns for single-scope (should become Fragment)
    single_scope_patterns = [
        r'listShapePresets?',
        r'listFillPresets?',
        r'listVisibilityPresets?',
        r'listArrangementPresets?',
        r'listAnimationPresets?',
        r'listBeamPresets?',
        r'listFollowPresets?',
        r'applyShapePreset',
        r'applyFillPreset',
        r'applyVisibilityPreset',
        r'applyArrangementPreset',
        r'applyAnimationPreset',
        r'applyBeamPreset',
        r'applyFollowPreset',
        r'currentPreset',  # in sub-panels
        r'presetDropdown',
        r'applyingPreset',
        r'ShapePreset',
        r'FillPreset',
        r'VisibilityPreset',
        r'ArrangementPreset',
        r'AnimationPreset',
        r'BeamPreset',
        r'FollowPreset',
    ]
    
    for i, line in enumerate(lines, 1):
        for pattern in single_scope_patterns:
            if re.search(pattern, line):
                # Determine the replacement
                old = re.search(pattern, line).group()
                new = old.replace('Preset', 'Fragment').replace('preset', 'fragment')
                
                # Get context (3 lines before and after)
                start = max(0, i - 4)
                end = min(len(lines), i + 3)
                context = '\n'.join(lines[start:end])
                
                report.fragment_renames.append(RefactorItem(
                    file_path=file_path,
                    line_number=i,
                    old_text=old,
                    new_text=new,
                    context=context
                ))

def analyze_codebase():
    """Analyze the entire codebase for refactoring needs."""
    report = AnalysisReport()
    
    print("=" * 80)
    print("PRESET → FRAGMENT REFACTOR ANALYSIS")
    print("=" * 80)
    
    # 1. Analyze Java files
    print("\n[1/4] Analyzing Java files...")
    java_files = list(SRC_DIR.rglob("*.java"))
    
    files_with_changes = defaultdict(list)
    
    for java_file in java_files:
        try:
            content = java_file.read_text(encoding='utf-8')
            
            # Skip legacy files
            if '_legacy' in str(java_file):
                continue
            
            # Check for preset-related code
            if 'Preset' in content or 'preset' in content:
                analyze_java_file(java_file, report)
                
        except Exception as e:
            print(f"  Error reading {java_file}: {e}")
    
    # 2. Identify files that need the NEW PresetRegistry (multi-scope)
    print("\n[2/4] Identifying new Preset (multi-scope) locations...")
    
    # BottomActionBar should get a Preset dropdown
    bottom_bar = SRC_DIR / "client" / "java" / "net" / "cyberpunk042" / "client" / "gui" / "widget" / "BottomActionBar.java"
    if bottom_bar.exists():
        report.preset_keeps.append((bottom_bar, "Add Preset dropdown (multi-scope)"))
    
    # 3. New files to create
    print("\n[3/4] Planning new files...")
    
    new_files = [
        CONFIG_DIR / "field_presets" / "README.md",
        CONFIG_DIR / "field_presets" / "ethereal_glow.json",
        CONFIG_DIR / "field_presets" / "tech_grid.json",
        CONFIG_DIR / "field_presets" / "shield_ring.json",
        SRC_DIR / "client" / "java" / "net" / "cyberpunk042" / "client" / "gui" / "util" / "MultiScopePresetRegistry.java",
    ]
    report.new_files = new_files
    
    # 4. Documentation updates
    print("\n[4/4] Identifying documentation updates...")
    
    doc_files = [
        DOCS_DIR / "_design" / "gui" / "GUI_DESIGN.md",
        DOCS_DIR / "_design" / "gui" / "GUI_ARCHITECTURE.md",
        DOCS_DIR / "_design" / "gui" / "GUI_CLASS_DIAGRAM.md",
        DOCS_DIR / "_design" / "gui" / "GUI_TODO_LIST.md",
        DOCS_DIR / "TODO_LIST.md",
    ]
    for doc in doc_files:
        if doc.exists():
            report.doc_updates.append(doc)
    
    return report

def print_report(report: AnalysisReport):
    """Print the analysis report."""
    
    print("\n" + "=" * 80)
    print("REFACTOR SUMMARY")
    print("=" * 80)
    
    # Group renames by file
    by_file = defaultdict(list)
    for item in report.fragment_renames:
        by_file[item.file_path].append(item)
    
    print(f"\n📁 FILES NEEDING RENAME (Preset → Fragment): {len(by_file)}")
    print("-" * 60)
    
    total_changes = 0
    for file_path, items in sorted(by_file.items()):
        rel_path = file_path.relative_to(PROJECT_ROOT)
        unique_patterns = set(item.old_text for item in items)
        total_changes += len(items)
        print(f"  {rel_path}")
        print(f"    Changes: {len(items)} | Patterns: {', '.join(sorted(unique_patterns)[:5])}")
    
    print(f"\n  TOTAL RENAME OPERATIONS: {total_changes}")
    
    print(f"\n📦 NEW FILES TO CREATE: {len(report.new_files)}")
    print("-" * 60)
    for f in report.new_files:
        try:
            rel = f.relative_to(PROJECT_ROOT)
        except ValueError:
            rel = f
        print(f"  + {rel}")
    
    print(f"\n📄 DOCUMENTATION TO UPDATE: {len(report.doc_updates)}")
    print("-" * 60)
    for f in report.doc_updates:
        rel = f.relative_to(PROJECT_ROOT)
        print(f"  ~ {rel}")
    
    print("\n" + "=" * 80)
    print("REFACTOR PLAN")
    print("=" * 80)
    
    print("""
PHASE 1: Rename Single-Scope (Preset → Fragment)
─────────────────────────────────────────────────
  1.1 Rename PresetRegistry.java → FragmentRegistry.java
  1.2 Rename all methods: listXxxPresets → listXxxFragments
  1.3 Rename all methods: applyXxxPreset → applyXxxFragment
  1.4 Update all sub-panels: presetDropdown → fragmentDropdown
  1.5 Update all sub-panels: currentPreset → currentFragment
  1.6 Update GuiState: preset name fields → fragment name fields

PHASE 2: Create Multi-Scope Preset System (NEW)
───────────────────────────────────────────────
  2.1 Create field_presets/ folder with examples
  2.2 Create PresetRegistry.java (multi-scope version)
  2.3 Add preset dropdown to BottomActionBar
  2.4 Implement preset loading and merging logic

PHASE 3: Update Documentation
─────────────────────────────
  3.1 Update GUI_DESIGN.md with new terminology
  3.2 Update GUI_ARCHITECTURE.md
  3.3 Update GUI_CLASS_DIAGRAM.md
  3.4 Update TODO lists

PHASE 4: Testing
────────────────
  4.1 Verify all fragments load correctly
  4.2 Test multi-scope preset application
  4.3 Verify no naming conflicts
""")

def execute_phase1_renames(report: AnalysisReport, dry_run: bool = True):
    """Execute Phase 1: Rename Preset → Fragment for single-scope."""
    
    print("\n" + "=" * 80)
    print(f"EXECUTING PHASE 1 {'(DRY RUN)' if dry_run else '(LIVE)'}")
    print("=" * 80)
    
    # Mapping of old → new for file-wide replacements
    replacements = {
        # Class/file names
        'PresetRegistry': 'FragmentRegistry',
        
        # Method names - list
        'listShapePresets': 'listShapeFragments',
        'listFillPresets': 'listFillFragments',
        'listVisibilityPresets': 'listVisibilityFragments',
        'listArrangementPresets': 'listArrangementFragments',
        'listAnimationPresets': 'listAnimationFragments',
        'listBeamPresets': 'listBeamFragments',
        'listFollowPresets': 'listFollowFragments',
        
        # Method names - apply
        'applyShapePreset': 'applyShapeFragment',
        'applyFillPreset': 'applyFillFragment',
        'applyVisibilityPreset': 'applyVisibilityFragment',
        'applyArrangementPreset': 'applyArrangementFragment',
        'applyAnimationPreset': 'applyAnimationFragment',
        'applyBeamPreset': 'applyBeamFragment',
        'applyFollowPreset': 'applyFollowFragment',
        
        # Internal types
        'ShapePreset': 'ShapeFragment',
        'FillPreset': 'FillFragment',
        'VisibilityPreset': 'VisibilityFragment',
        'ArrangementPreset': 'ArrangementFragment',
        'AnimationPreset': 'AnimationFragment',
        'BeamPreset': 'BeamFragment',
        'FollowPreset': 'FollowFragment',
        
        # Variables in sub-panels
        'presetDropdown': 'fragmentDropdown',
        'currentPreset': 'currentFragment',
        'applyingPreset': 'applyingFragment',
    }
    
    # Get unique files
    files_to_process = set(item.file_path for item in report.fragment_renames)
    
    for file_path in sorted(files_to_process):
        rel_path = file_path.relative_to(PROJECT_ROOT)
        
        # Skip legacy
        if '_legacy' in str(file_path):
            print(f"  SKIP (legacy): {rel_path}")
            continue
        
        try:
            content = file_path.read_text(encoding='utf-8')
            original = content
            
            # Apply all replacements
            for old, new in replacements.items():
                content = content.replace(old, new)
            
            if content != original:
                if dry_run:
                    print(f"  WOULD MODIFY: {rel_path}")
                else:
                    file_path.write_text(content, encoding='utf-8')
                    print(f"  MODIFIED: {rel_path}")
            else:
                print(f"  NO CHANGES: {rel_path}")
                
        except Exception as e:
            print(f"  ERROR: {rel_path} - {e}")
    
    # Rename the file itself
    old_file = SRC_DIR / "client" / "java" / "net" / "cyberpunk042" / "client" / "gui" / "util" / "PresetRegistry.java"
    new_file = SRC_DIR / "client" / "java" / "net" / "cyberpunk042" / "client" / "gui" / "util" / "FragmentRegistry.java"
    
    if old_file.exists():
        if dry_run:
            print(f"\n  WOULD RENAME FILE: PresetRegistry.java → FragmentRegistry.java")
        else:
            # First update the content
            content = old_file.read_text(encoding='utf-8')
            for old, new in replacements.items():
                content = content.replace(old, new)
            # Also update class declaration
            content = content.replace('class PresetRegistry', 'class FragmentRegistry')
            content = content.replace('PresetRegistry()', 'FragmentRegistry()')
            
            new_file.write_text(content, encoding='utf-8')
            old_file.unlink()
            print(f"\n  RENAMED FILE: PresetRegistry.java → FragmentRegistry.java")

def main():
    """Main entry point."""
    import sys
    
    # Analyze
    report = analyze_codebase()
    print_report(report)
    
    # Check for --execute flag
    if len(sys.argv) > 1 and sys.argv[1] == '--execute':
        print("\n⚠️  EXECUTING LIVE REFACTOR...")
        execute_phase1_renames(report, dry_run=False)
    else:
        print("\n💡 To execute the refactor, run: python scripts/preset_to_fragment_refactor.py --execute")
        print("   Running dry-run to preview changes...\n")
        execute_phase1_renames(report, dry_run=True)

if __name__ == "__main__":
    main()

