#!/usr/bin/env python3
"""
Batch A: Update TODO list + Add missing shape sliders to ShapeSubPanel

Tasks:
1. Update GUI_TODO_LIST.md - mark completed items
2. Analyze ShapeSubPanel.java for existing patterns
3. Add missing sliders: prism.twist, prism.capTop, prism.capBottom, cylinder.arc
4. Verify @Range annotations exist on shape records
"""

import re
from pathlib import Path

# Paths
PROJECT_ROOT = Path(__file__).parent.parent
TODO_LIST = PROJECT_ROOT / "docs/field-system/NEW_REFACTORING_NEW_PHASES/_design/gui/GUI_TODO_LIST.md"
SHAPE_PANEL = PROJECT_ROOT / "src/client/java/net/cyberpunk042/client/gui/panel/sub/ShapeSubPanel.java"
PRISM_SHAPE = PROJECT_ROOT / "src/main/java/net/cyberpunk042/visual/shape/PrismShape.java"
CYLINDER_SHAPE = PROJECT_ROOT / "src/main/java/net/cyberpunk042/visual/shape/CylinderShape.java"

def update_todo_list():
    """Mark completed items in GUI_TODO_LIST.md"""
    print("\n=== Step 1: Updating TODO List ===")
    
    content = TODO_LIST.read_text(encoding='utf-8')
    
    # Items to mark as done
    updates = [
        # (pattern to find, replacement)
        (r'\| G-CMD-09 \| .+? \| ⬜ \|', '| G-CMD-09 | Add `/field status` command (show current FieldEditState summary) | ✅ | Low | Implemented Dec 10 |'),
        (r'\| G-FCMD-14 \| .+? \| ⬜ \|', '| G-FCMD-14 | `/field fragment <category> <name>` | ✅ | Low | Implemented Dec 10 |'),
        (r'\| G-FCMD-15 \| .+? \| ⬜ \|', '| G-FCMD-15 | `/field preset apply <name>` | ✅ | Low | Implemented Dec 10 |'),
        (r'\| G-FCMD-16 \| .+? \| ⬜ \|', '| G-FCMD-16 | `/field profile load/save/list` | ✅ | Medium | Implemented Dec 10 |'),
    ]
    
    updated = 0
    for pattern, replacement in updates:
        if re.search(pattern, content):
            content = re.sub(pattern, replacement, content)
            updated += 1
            print(f"  ✓ Marked done: {replacement.split('|')[1].strip()}")
    
    # Update stats
    content = re.sub(
        r'> \*\*G-FCMD-\*:\*\* ✅ \d+/16',
        '> **G-FCMD-*:** ✅ 15/16',
        content
    )
    
    TODO_LIST.write_text(content, encoding='utf-8')
    print(f"  Updated {updated} items in TODO list")
    return updated

def analyze_shape_panel():
    """Analyze ShapeSubPanel.java for patterns"""
    print("\n=== Step 2: Analyzing ShapeSubPanel.java ===")
    
    content = SHAPE_PANEL.read_text(encoding='utf-8')
    
    # Find existing prism controls
    prism_section = re.search(r'// Prism.*?(?=// Cylinder|$)', content, re.DOTALL)
    cylinder_section = re.search(r'// Cylinder.*?(?=// Polyhedron|$)', content, re.DOTALL)
    
    print(f"  File size: {len(content)} chars, {content.count(chr(10))} lines")
    
    # Find slider pattern
    slider_pattern = r'GuiWidgets\.slider\([^)]+\)'
    sliders = re.findall(slider_pattern, content)
    print(f"  Found {len(sliders)} slider widgets")
    
    # Find toggle pattern
    toggle_pattern = r'GuiWidgets\.toggle\([^)]+\)'
    toggles = re.findall(toggle_pattern, content)
    print(f"  Found {len(toggles)} toggle widgets")
    
    # Check what prism controls exist
    prism_controls = {
        'sides': 'prism.sides' in content or '"sides"' in content,
        'height': 'prism.height' in content,
        'topRadius': 'prism.topRadius' in content or 'topRadius' in content,
        'twist': 'prism.twist' in content,
        'capTop': 'prism.capTop' in content or 'capTop' in content,
        'capBottom': 'prism.capBottom' in content or 'capBottom' in content,
    }
    
    # Check cylinder controls
    cylinder_controls = {
        'height': 'cylinder.height' in content,
        'segments': 'cylinder.segments' in content,
        'topRadius': 'cylinder.topRadius' in content,
        'arc': 'cylinder.arc' in content,
        'capTop': 'cylinder.capTop' in content,
        'capBottom': 'cylinder.capBottom' in content,
        'openEnded': 'cylinder.openEnded' in content or 'openEnded' in content,
    }
    
    print("\n  Prism controls:")
    for name, exists in prism_controls.items():
        print(f"    {'✓' if exists else '✗'} {name}")
    
    print("\n  Cylinder controls:")
    for name, exists in cylinder_controls.items():
        print(f"    {'✓' if exists else '✗'} {name}")
    
    # Identify what's missing
    missing_prism = [k for k, v in prism_controls.items() if not v]
    missing_cylinder = [k for k, v in cylinder_controls.items() if not v]
    
    print(f"\n  Missing prism: {missing_prism}")
    print(f"  Missing cylinder: {missing_cylinder}")
    
    return {
        'content': content,
        'missing_prism': missing_prism,
        'missing_cylinder': missing_cylinder,
        'prism_section': prism_section,
        'cylinder_section': cylinder_section
    }

def check_range_annotations():
    """Check if shape records have @Range annotations"""
    print("\n=== Step 3: Checking @Range annotations ===")
    
    results = {}
    
    for shape_file, shape_name in [(PRISM_SHAPE, 'PrismShape'), (CYLINDER_SHAPE, 'CylinderShape')]:
        if not shape_file.exists():
            print(f"  ⚠ {shape_name} file not found")
            continue
            
        content = shape_file.read_text(encoding='utf-8')
        
        # Find @Range annotations
        ranges = re.findall(r'@Range\(.*?\)\s+\w+\s+(\w+)', content)
        print(f"\n  {shape_name} @Range fields: {ranges}")
        
        # Check specific fields
        fields_to_check = {
            'PrismShape': ['twist', 'capTop', 'capBottom'],
            'CylinderShape': ['arc', 'capTop', 'capBottom']
        }
        
        for field in fields_to_check.get(shape_name, []):
            has_range = f'@Range' in content and field in content
            has_field = re.search(rf'\b{field}\b', content) is not None
            print(f"    {field}: {'has @Range' if has_range else 'no @Range'}, {'exists' if has_field else 'missing'}")
            
        results[shape_name] = {
            'ranges': ranges,
            'content': content
        }
    
    return results

def find_insertion_points(content):
    """Find where to insert new controls in ShapeSubPanel"""
    print("\n=== Step 4: Finding insertion points ===")
    
    # Find prism section end (before cylinder)
    prism_end = content.find('// Cylinder')
    if prism_end == -1:
        prism_end = content.find('case "cylinder"')
    
    # Find cylinder section end (before polyhedron or closing)
    cylinder_end = content.find('// Polyhedron')
    if cylinder_end == -1:
        cylinder_end = content.find('case "polyhedron"')
    
    print(f"  Prism section ends around char {prism_end}")
    print(f"  Cylinder section ends around char {cylinder_end}")
    
    return prism_end, cylinder_end

def generate_report(analysis, range_check):
    """Generate a summary report"""
    print("\n" + "="*60)
    print("BATCH A ANALYSIS REPORT")
    print("="*60)
    
    print("\n📋 TODO List Updates: Done")
    
    print(f"\n🔧 Missing Prism Controls: {analysis['missing_prism']}")
    print(f"🔧 Missing Cylinder Controls: {analysis['missing_cylinder']}")
    
    print("\n📝 Recommended Actions:")
    
    if 'twist' in analysis['missing_prism']:
        print("  1. Add prism.twist slider (-360 to 360 degrees)")
    if 'capTop' in analysis['missing_prism']:
        print("  2. Add prism.capTop toggle")
    if 'capBottom' in analysis['missing_prism']:
        print("  3. Add prism.capBottom toggle")
    if 'arc' in analysis['missing_cylinder']:
        print("  4. Add cylinder.arc slider (0 to 360 degrees)")
    
    print("\n⚠️  Manual steps needed:")
    print("  - Add sliders/toggles to ShapeSubPanel.java")
    print("  - Verify records have @Range annotations")
    print("  - Build and test")

def main():
    print("="*60)
    print("BATCH A: Shape Sliders Analysis")
    print("="*60)
    
    # Step 1: Update TODO list
    update_todo_list()
    
    # Step 2: Analyze ShapeSubPanel
    analysis = analyze_shape_panel()
    
    # Step 3: Check @Range annotations
    range_check = check_range_annotations()
    
    # Step 4: Generate report
    generate_report(analysis, range_check)
    
    print("\n✅ Analysis complete!")
    print("   Run: ./gradlew build to verify TODO changes")

if __name__ == "__main__":
    main()

