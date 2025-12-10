#!/usr/bin/env python3
"""
Update all files to use direct LayerState access pattern.

LayerState has public mutable fields, so we use:
- state.getLayers().get(i).alpha instead of state.getLayerAlpha(i)
- state.getLayers().get(i).alpha = v; state.markDirty() instead of state.setLayerAlpha(i, v)
"""

import os
import re

CLIENT_DIR = "src/client/java"

# Getter replacements: method call -> direct field access
# Note: We need to handle nested method calls like state.getSelectedLayerIndex()
# by matching balanced parentheses
GETTER_REPLACEMENTS = [
    # state.getLayerAlpha(expr) -> state.getLayers().get(expr).alpha
    (r'state\.getLayerAlpha\((state\.getSelectedLayerIndex\(\))\)', r'state.getLayers().get(\1).alpha'),
    (r'state\.getLayerAlpha\((\w+)\)', r'state.getLayers().get(\1).alpha'),
    # state.getLayerBlendMode(expr) -> state.getLayers().get(expr).blendMode
    (r'state\.getLayerBlendMode\((state\.getSelectedLayerIndex\(\))\)', r'state.getLayers().get(\1).blendMode'),
    (r'state\.getLayerBlendMode\((\w+)\)', r'state.getLayers().get(\1).blendMode'),
    # state.isLayerVisible(expr) -> state.getLayers().get(expr).visible
    (r'state\.isLayerVisible\((state\.getSelectedLayerIndex\(\))\)', r'state.getLayers().get(\1).visible'),
    (r'state\.isLayerVisible\((\w+)\)', r'state.getLayers().get(\1).visible'),
    # state.getLayerName(expr) -> state.getLayers().get(expr).name
    (r'state\.getLayerName\((state\.getSelectedLayerIndex\(\))\)', r'state.getLayers().get(\1).name'),
    (r'state\.getLayerName\((\w+)\)', r'state.getLayers().get(\1).name'),
    # state.getLayerOrder(expr) -> state.getLayers().get(expr).order
    (r'state\.getLayerOrder\((state\.getSelectedLayerIndex\(\))\)', r'state.getLayers().get(\1).order'),
    (r'state\.getLayerOrder\((\w+)\)', r'state.getLayers().get(\1).order'),
]

# Setter replacements: full statement -> direct assignment + markDirty
# Handle lambda context: v -> state.setXxx(...) becomes v -> { ... }
SETTER_REPLACEMENTS = [
    # Lambda: v -> state.setLayerAlpha(index, value) -> v -> { state.getLayers().get(index).alpha = value; state.markDirty(); }
    (r'(\w+)\s*->\s*state\.setLayerAlpha\((state\.getSelectedLayerIndex\(\)),\s*(\w+)\)', 
     r'\1 -> { state.getLayers().get(\2).alpha = \3; state.markDirty(); }'),
    (r'(\w+)\s*->\s*state\.setLayerAlpha\((\w+),\s*(\w+)\)', 
     r'\1 -> { state.getLayers().get(\2).alpha = \3; state.markDirty(); }'),
    # Lambda: v -> state.setLayerBlendMode(index, v.name())
    # Special case for v.name() which contains parentheses
    (r'(\w+)\s*->\s*state\.setLayerBlendMode\((state\.getSelectedLayerIndex\(\)),\s*(\w+)\.name\(\)\)',
     r'\1 -> { state.getLayers().get(\2).blendMode = \3.name(); state.markDirty(); }'),
    (r'(\w+)\s*->\s*state\.setLayerBlendMode\((\w+),\s*(\w+)\.name\(\)\)',
     r'\1 -> { state.getLayers().get(\2).blendMode = \3.name(); state.markDirty(); }'),
    # General case for simple values
    (r'(\w+)\s*->\s*state\.setLayerBlendMode\((state\.getSelectedLayerIndex\(\)),\s*(\w+)\)',
     r'\1 -> { state.getLayers().get(\2).blendMode = \3; state.markDirty(); }'),
    # Non-lambda setters
    (r'state\.setLayerName\((state\.getSelectedLayerIndex\(\)),\s*([^)]+)\)', 
     r'state.getLayers().get(\1).name = \2; state.markDirty()'),
    (r'state\.setLayerOrder\((state\.getSelectedLayerIndex\(\)),\s*([^)]+)\)', 
     r'state.getLayers().get(\1).order = \2; state.markDirty()'),
]

# Additional patterns for non-lambda setters (like in PresetRegistry)
# These handle complex values like json.get("x").getAsString()
# For if-statements: "if (cond) stmt;" -> "if (cond) { stmts; }" - no trailing semicolon!
NON_LAMBDA_SETTER_REPLACEMENTS = [
    # state.setLayerBlendMode(layerIndex, json.get("x").getAsString())
    (r'state\.setLayerBlendMode\((\w+),\s*(json\.get\([^)]+\)\.getAsString\(\))\);', 
     r'{ state.getLayers().get(\1).blendMode = \2; state.markDirty(); }'),
    # state.setLayerAlpha(layerIndex, json.get("x").getAsFloat())
    (r'state\.setLayerAlpha\((\w+),\s*(json\.get\([^)]+\)\.getAsFloat\(\))\);', 
     r'{ state.getLayers().get(\1).alpha = \2; state.markDirty(); }'),
    # state.setLayerOrder(layerIndex, json.get("x").getAsInt())
    (r'state\.setLayerOrder\((\w+),\s*(json\.get\([^)]+\)\.getAsInt\(\))\);', 
     r'{ state.getLayers().get(\1).order = \2; state.markDirty(); }'),
    # Simple values (with trailing semicolon)
    (r'state\.setLayerBlendMode\((\w+),\s*(\w+)\);', 
     r'{ state.getLayers().get(\1).blendMode = \2; state.markDirty(); }'),
    (r'state\.setLayerAlpha\((\w+),\s*(\w+)\);', 
     r'{ state.getLayers().get(\1).alpha = \2; state.markDirty(); }'),
    (r'state\.setLayerName\((\w+),\s*(\w+)\);', 
     r'{ state.getLayers().get(\1).name = \2; state.markDirty(); }'),
    (r'state\.setLayerOrder\((\w+),\s*(\w+)\);', 
     r'{ state.getLayers().get(\1).order = \2; state.markDirty(); }'),
]

# Additional getter patterns for simple variable index
SIMPLE_GETTER_REPLACEMENTS = [
    (r'state\.getLayerName\((\w+)\)', r'state.getLayers().get(\1).name'),
    (r'state\.isLayerVisible\((\w+)\)', r'state.getLayers().get(\1).visible'),
]

def find_java_files(directory):
    """Find all Java files in directory."""
    java_files = []
    for root, dirs, files in os.walk(directory):
        # Skip FieldEditState.java
        for f in files:
            if f.endswith('.java') and f != 'FieldEditState.java':
                java_files.append(os.path.join(root, f))
    return java_files

def update_file(filepath, dry_run=True):
    """Update a single file."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    changes = []
    
    # Apply getter replacements
    for pattern, replacement in GETTER_REPLACEMENTS:
        matches = re.findall(pattern, content)
        if matches:
            new_content = re.sub(pattern, replacement, content)
            if new_content != content:
                changes.append(f"Getter: {len(matches)} occurrences")
                content = new_content
    
    # Apply simple getter replacements
    for pattern, replacement in SIMPLE_GETTER_REPLACEMENTS:
        matches = re.findall(pattern, content)
        if matches:
            new_content = re.sub(pattern, replacement, content)
            if new_content != content:
                changes.append(f"SimpleGetter: {len(matches)} occurrences")
                content = new_content
    
    # Apply lambda setter replacements
    for pattern, replacement in SETTER_REPLACEMENTS:
        matches = re.findall(pattern, content)
        if matches:
            new_content = re.sub(pattern, replacement, content)
            if new_content != content:
                changes.append(f"LambdaSetter: {len(matches)} occurrences")
                content = new_content
    
    # Apply non-lambda setter replacements
    for pattern, replacement in NON_LAMBDA_SETTER_REPLACEMENTS:
        matches = re.findall(pattern, content)
        if matches:
            new_content = re.sub(pattern, replacement, content)
            if new_content != content:
                changes.append(f"Setter: {len(matches)} occurrences")
                content = new_content
    
    # Handle toggleLayerVisibility with assignment (LayerPanel case)
    # Pattern: boolean visible = state.toggleLayerVisibility(index);
    # We need to: get layer, toggle, markDirty, extract result
    toggle_pattern = r'boolean visible = state\.toggleLayerVisibility\(([^)]+)\);'
    toggle_matches = re.findall(toggle_pattern, content)
    if toggle_matches:
        def toggle_replacement(m):
            idx = m.group(1)
            # Proper multi-line replacement with consistent 8-space indent
            return (f'var layer = state.getLayers().get({idx});\n'
                    f'        layer.visible = !layer.visible;\n'
                    f'        state.markDirty();\n'
                    f'        boolean visible = layer.visible;')
        content = re.sub(toggle_pattern, toggle_replacement, content)
        changes.append(f"toggleLayerVisibility: {len(toggle_matches)} occurrences")
    
    # Handle state.toggleLayerVisibility(index) without assignment (PresetRegistry case)
    # Context: inside block "if (...) { state.toggleLayerVisibility(...); }"
    # The "visible" variable in scope holds the desired value, so we SET directly (not toggle)
    toggle_call_pattern = r'state\.toggleLayerVisibility\((\w+)\);'
    toggle_call_matches = re.findall(toggle_call_pattern, content)
    if toggle_call_matches:
        def toggle_call_replacement(m):
            idx = m.group(1)
            # No outer braces - we're already inside a block
            return f'state.getLayers().get({idx}).visible = visible; state.markDirty();'
        content = re.sub(toggle_call_pattern, toggle_call_replacement, content)
        changes.append(f"toggleLayerVisibility (call): {len(toggle_call_matches)} occurrences")
    
    if content == original:
        return None  # No changes
    
    return (filepath, original, content, changes)

def main(dry_run=True):
    """Process all files."""
    java_files = find_java_files(CLIENT_DIR)
    
    results = []
    for filepath in java_files:
        result = update_file(filepath, dry_run)
        if result:
            results.append(result)
    
    if not results:
        print("No changes needed in any file")
        return
    
    print(f"\n{'='*60}")
    print(f"Files to update: {len(results)}")
    print(f"{'='*60}\n")
    
    for filepath, original, content, changes in results:
        rel_path = os.path.relpath(filepath)
        print(f"\n📁 {rel_path}")
        for change in changes:
            print(f"   - {change}")
        
        if dry_run:
            import difflib
            diff = list(difflib.unified_diff(
                original.split('\n'),
                content.split('\n'),
                fromfile=f'{rel_path} (original)',
                tofile=f'{rel_path} (modified)',
                lineterm=''
            ))
            if diff:
                print("   --- Diff ---")
                for line in diff[:50]:  # Show first 50 lines of diff per file
                    print(f"   {line}")
                if len(diff) > 50:
                    print(f"   ... ({len(diff) - 50} more lines)")
    
    if dry_run:
        print(f"\n[DRY RUN] No changes written")
    else:
        for filepath, original, content, changes in results:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"✓ Written: {os.path.relpath(filepath)}")

if __name__ == "__main__":
    import sys
    dry_run = "--apply" not in sys.argv
    main(dry_run=dry_run)

