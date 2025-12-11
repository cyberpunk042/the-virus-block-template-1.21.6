#!/usr/bin/env python3
"""
Smart utility to refactor state setter methods to dot notation.

Usage:
    python scripts/refactor_state_accessors.py                    # Dry run
    python scripts/refactor_state_accessors.py --apply            # Apply changes

Transforms:
    state.setSphereLatSteps(v)  →  state.set("sphere.latSteps", v)
    state.setSpinSpeed(v)       →  state.set("spin.speed", v)
    state.setBeamPulseSpeed(v)  →  state.set("beam.pulse.speed", v)
    state.setColor(v)           →  state.set("color", v)
"""

import os
import re
import sys
import difflib
from typing import List, Tuple, Optional, Set, Dict

# ═══════════════════════════════════════════════════════════════════════════════
# SMART AUTO-DETECTION CONFIGURATION
# ═══════════════════════════════════════════════════════════════════════════════

# Known record prefixes (from @StateField in FieldEditState)
# Maps method prefix to record field name
# IMPORTANT: Longer prefixes must come first to match correctly
RECORD_PREFIXES: Dict[str, str] = {
    # Shapes
    "Sphere": "sphere",
    "Ring": "ring",
    "Disc": "disc",
    "Prism": "prism",
    "Cylinder": "cylinder",
    "Poly": "polyhedron",
    # Transform - but NOT Scale/Offset alone (those could be direct)
    "Transform": "transform",
    "Orbit": "orbit",
    # Animation - AlphaFade/AlphaMin/AlphaMax are alphaPulse, but NOT Alpha alone
    "AlphaFade": "alphaPulse",
    "AlphaMin": "alphaPulse", 
    "AlphaMax": "alphaPulse",
    "Spin": "spin",
    "Pulse": "pulse",
    "Wobble": "wobble",
    "Wave": "wave",
    "ColorCycle": "colorCycle",
    # Fill
    "Fill": "fill",
    "WireThickness": "fill",
    "DoubleSided": "fill",
    "DepthTest": "fill",
    "DepthWrite": "fill",
    # Mask
    "Mask": "mask",
    # Arrangement - NOTE: specific patterns handled in EXACT_MAPPINGS
    # "Quad/Segment/Sector/MultiPart" patterns go to EXACT_MAPPINGS for precise property names
    # Prediction
    "Prediction": "prediction",
    # Beam
    "Beam": "beam",
    # Other
    "Link": "link",
    "Modifier": "modifier",
}

# Nested record paths (e.g., BeamPulse -> beam.pulse)
NESTED_PREFIXES: Dict[str, str] = {
    "BeamPulse": "beam.pulse",
}

# Property name fixes (method suffix -> actual record field name)
# NOTE: These apply to ALL records, so be careful!
PROPERTY_FIXES: Dict[str, str] = {
    # "Frequency" NOT here - PulseConfig uses "speed", WaveConfig uses "frequency"
    "AnimateSpeed": "animSpeed", # setMaskAnimateSpeed -> mask.animSpeed
    "Inverted": "invert",        # setMaskInverted -> mask.invert
    "Animated": "animate",       # setMaskAnimated -> mask.animate
}

# Full method name -> exact path (for special cases)
EXACT_MAPPINGS: Dict[str, str] = {
    # Transform properties
    "setAnchor": "transform.anchor",
    "setScale": "transform.scale",
    "setOffset": "transform.offset",        # Also needs VECTOR3_METHODS
    "setRotation": "transform.rotation",    # Also needs VECTOR3_METHODS
    "setFacing": "transform.facing",
    "setBillboard": "transform.billboard",
    # Fill properties (full method name -> exact path)
    "setWireThickness": "fill.wireThickness",
    "setDoubleSided": "fill.doubleSided",
    "setDepthTest": "fill.depthTest",
    "setDepthWrite": "fill.depthWrite",
    # AlphaPulse
    "setAlphaFadeEnabled": "alphaPulse.enabled",
    "setAlphaMin": "alphaPulse.min",
    "setAlphaMax": "alphaPulse.max",
    # Pulse uses "speed", Wave uses "frequency"
    "setPulseFrequency": "pulse.speed",
    # Arrangement patterns (each has its own property)
    "setQuadPattern": "arrangement.quadPattern",
    "setSegmentPattern": "arrangement.segmentPattern",
    "setSectorPattern": "arrangement.sectorPattern",
    "setMultiPartArrangement": "arrangement.multiPart",
}

# Methods that take Vector3f components (v.x, v.y, v.z) and should pass v directly
VECTOR3_METHODS: Set[str] = {
    "setOffset",
    "setRotation",
}

# Methods to SKIP (coordination methods, not simple setters)
SKIP_METHODS: Set[str] = {
    "setSelectedLayerIndex",
    "setSelectedPrimitiveIndex", 
    "setCurrentProfile",
    "setCurrentProfileName",
    # Layer/primitive management (handled by update_layer_panel.py)
    "setLayerAlpha",
    "setLayerBlendMode",
    "setLayerOrder",
    "setLayerName",
    "setLayerVisible",
    # These might set whole objects, need special handling
    "set",
}

# Direct fields (no record prefix, just @StateField primitives)
DIRECT_FIELDS: Set[str] = {
    "radius", "shapeType",
    "followMode", "followEnabled", "predictionEnabled",
    "livePreviewEnabled", "autoSaveEnabled", "debugUnlocked",
    "lifecycleState", "fadeInTicks", "fadeOutTicks",
    "triggerType", "triggerEffect", "triggerIntensity", "triggerDuration",
    "primitiveId", "radiusOffset", "phaseOffset", "mirrorAxis",
    "followLinked", "scaleWithLinked",
    # Note: cageLatCount, cageLonCount, pointSize handled by CageOptionsAdapter
}

# Appearance record fields (routed to appearance.fieldName)
APPEARANCE_FIELDS: Set[str] = {
    "color", "alpha", "glow", "emissive", "saturation",
    "primaryColor", "secondaryColor",
}

CLIENT_DIR = "src/client/java/net/cyberpunk042/client"


# ═══════════════════════════════════════════════════════════════════════════════
# SMART DETECTION ENGINE  
# ═══════════════════════════════════════════════════════════════════════════════

def camel_to_lower(name: str) -> str:
    """Convert CamelCase to camelCase (first letter lowercase)."""
    if not name:
        return name
    return name[0].lower() + name[1:]


def parse_setter_method(method_name: str) -> Optional[str]:
    """
    Parse a setter method name and return the dot-notation path.
    
    Examples:
        setSphereLatSteps -> sphere.latSteps
        setSpinSpeed -> spin.speed
        setBeamPulseSpeed -> beam.pulse.speed
        setColor -> color
        
    Returns None if method should be skipped.
    """
    if method_name in SKIP_METHODS:
        return None
    
    if not method_name.startswith("set"):
        return None
    
    # Check exact mappings first (handles special cases)
    if method_name in EXACT_MAPPINGS:
        return EXACT_MAPPINGS[method_name]
    
    # Remove "set" prefix
    remainder = method_name[3:]
    if not remainder:
        return None
    
    # Check nested prefixes first (e.g., BeamPulse)
    for prefix, path in sorted(NESTED_PREFIXES.items(), key=lambda x: -len(x[0])):
        if remainder.startswith(prefix):
            prop = remainder[len(prefix):]
            prop = camel_to_lower(prop)
            prop = PROPERTY_FIXES.get(prop, prop)
            if prop:
                return f"{path}.{prop}"
    
    # Check direct fields BEFORE record prefixes (e.g., setAlpha -> alpha, not alphaPulse.xxx)
    field_name = camel_to_lower(remainder)
    if field_name in DIRECT_FIELDS:
        return field_name
    
    # Check appearance fields (routed to appearance.fieldName)
    if field_name in APPEARANCE_FIELDS:
        return f"appearance.{field_name}"
    
    # Check record prefixes (longest first to match "ColorCycle" before "Color")
    for prefix, record in sorted(RECORD_PREFIXES.items(), key=lambda x: -len(x[0])):
        if remainder.startswith(prefix):
            prop = remainder[len(prefix):]
            if not prop:
                # No property suffix, skip (e.g., setMask with no property)
                return None
            # Apply PROPERTY_FIXES before lowercasing (keys are CamelCase)
            prop = PROPERTY_FIXES.get(prop, prop)
            prop = camel_to_lower(prop)
            return f"{record}.{prop}"
    
    # Unknown setter - return None (will be skipped)
    return None


def extract_vector3_var(args: str) -> Optional[Tuple[str, bool]]:
    """
    Check if args is a Vector3f pattern and return (replacement, needs_new).
    
    Patterns:
    - 'v.x, v.y, v.z' -> ('v', False) - already a Vector3f
    - 'x, y, z' or 'ox, oy, oz' -> ('new Vector3f(x, y, z)', True) - needs construction
    
    Returns None if not a Vector3f pattern.
    """
    args = args.strip()
    
    # Pattern 1: varname.x, varname.y, varname.z (already Vector3f)
    match = re.match(r'^(\w+)\.x\s*,\s*\1\.y\s*,\s*\1\.z$', args)
    if match:
        return (match.group(1), False)
    
    # Pattern 2: three separate variables (need to construct Vector3f)
    # Match: x, y, z or ox, oy, oz or offsetX, offsetY, offsetZ
    match = re.match(r'^(\w+)\s*,\s*(\w+)\s*,\s*(\w+)$', args)
    if match:
        x, y, z = match.group(1), match.group(2), match.group(3)
        return (f'new Vector3f({x}, {y}, {z})', True)
    
    return None


def add_vector3f_import(content: str) -> str:
    """Add Vector3f import if not present and new Vector3f is used."""
    if 'new Vector3f(' not in content:
        return content
    if 'import org.joml.Vector3f;' in content:
        return content
    
    # Find the package line and add import after it
    lines = content.split('\n')
    for i, line in enumerate(lines):
        if line.startswith('package '):
            # Find end of imports block
            insert_pos = i + 1
            while insert_pos < len(lines) and (lines[insert_pos].startswith('import ') or lines[insert_pos].strip() == ''):
                insert_pos += 1
            # Insert before first non-import line
            lines.insert(insert_pos, 'import org.joml.Vector3f;')
            break
    
    return '\n'.join(lines)


def process_file_smart(filepath: str) -> Optional[Tuple[str, str, str, List[str], List[str]]]:
    """Process a file using smart auto-detection. Returns (path, orig, new, changes, skipped)."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    changes = []
    skipped = []
    needs_vector3f_import = False
    
    # Find all state.setXxx( calls
    pattern = r'state\.(set\w+)\(([^)]+)\)'
    
    def replacement(m):
        nonlocal needs_vector3f_import
        method = m.group(1)
        args = m.group(2)
        
        path = parse_setter_method(method)
        if path is None:
            skipped.append(method)
            return m.group(0)  # Keep unchanged
        
        # Check if this is a Vector3f method with component args
        if method in VECTOR3_METHODS:
            result = extract_vector3_var(args)
            if result:
                vec_replacement, needs_new = result
                if needs_new:
                    needs_vector3f_import = True
                changes.append(f'{method}({args}) → set("{path}", {vec_replacement})')
                return f'state.set("{path}", {vec_replacement})'
        
        changes.append(f'{method} → "{path}"')
        return f'state.set("{path}", {args})'
    
    content = re.sub(pattern, replacement, content)
    
    # Second pass: Update existing state.set("old_path", ...) to state.set("new_path", ...)
    # for appearance fields that need the appearance. prefix
    appearance_path_fixes = {
        '"color"': '"appearance.color"',
        '"alpha"': '"appearance.alpha"',
        '"glow"': '"appearance.glow"',
        '"emissive"': '"appearance.emissive"',
        '"saturation"': '"appearance.saturation"',
        '"primaryColor"': '"appearance.primaryColor"',
        '"secondaryColor"': '"appearance.secondaryColor"',
    }
    
    for old_path, new_path in appearance_path_fixes.items():
        old_pattern = rf'state\.set\({old_path},'
        new_replacement = f'state.set({new_path},'
        if re.search(old_pattern, content):
            content = re.sub(old_pattern, new_replacement, content)
            changes.append(f'state.set({old_path}, ...) → state.set({new_path}, ...)')
    
    # Add Vector3f import if needed
    if needs_vector3f_import:
        content = add_vector3f_import(content)
        changes.append("+ import org.joml.Vector3f")
    
    if content == original:
        return None
    
    # Deduplicate
    unique_changes = list(dict.fromkeys(changes))
    unique_skipped = list(dict.fromkeys(skipped))
    return (filepath, original, content, unique_changes, unique_skipped)


def find_java_files(directory: str) -> List[str]:
    """Find all Java files in directory."""
    java_files = []
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith('.java'):
                java_files.append(os.path.join(root, file))
    return java_files


def show_diff(original: str, modified: str, filepath: str) -> str:
    """Generate unified diff."""
    original_lines = original.splitlines(keepends=True)
    modified_lines = modified.splitlines(keepends=True)
    diff = difflib.unified_diff(original_lines, modified_lines,
                                 fromfile=f"{filepath} (original)",
                                 tofile=f"{filepath} (modified)")
    return ''.join(diff)


def main():
    import argparse
    
    parser = argparse.ArgumentParser(description="Smart refactor state setters to dot notation")
    parser.add_argument('--apply', action='store_true',
                        help="Apply changes (default: dry run)")
    parser.add_argument('--file', type=str,
                        help="Process only this file (for testing)")
    parser.add_argument('--show-skipped', action='store_true',
                        help="Show methods that were skipped")
    args = parser.parse_args()
    
    dry_run = not args.apply
    
    print(f"\n{'=' * 60}")
    print(f"Smart State Setter Refactoring")
    print(f"Transforms: state.setXxxYyy(v) → state.set(\"xxx.yyy\", v)")
    print(f"Mode: {'DRY RUN' if dry_run else 'APPLYING CHANGES'}")
    print(f"{'=' * 60}\n")
    
    if args.file:
        java_files = [args.file]
    else:
        java_files = find_java_files(CLIENT_DIR)
    
    results = []
    all_skipped = set()
    
    for filepath in java_files:
        result = process_file_smart(filepath)
        if result:
            results.append(result)
            all_skipped.update(result[4])
    
    if not results:
        print("No files need changes.")
        return
    
    print(f"Files to update: {len(results)}\n")
    
    total_changes = 0
    for filepath, original, modified, changes, skipped in results:
        rel_path = os.path.relpath(filepath)
        print(f"📁 {rel_path}")
        for change in changes:
            print(f"   - {change}")
        total_changes += len(changes)
        
        diff = show_diff(original, modified, rel_path)
        if diff:
            print("   --- Diff ---")
            diff_lines = diff.split('\n')
            for line in diff_lines[:40]:
                print(f"   {line}")
            if len(diff_lines) > 40:
                print(f"   ... ({len(diff_lines) - 40} more lines)")
        print()
        
        if not dry_run:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(modified)
    
    print(f"\nTotal transformations: {total_changes}")
    
    if args.show_skipped and all_skipped:
        print(f"\nSkipped methods ({len(all_skipped)}):")
        for m in sorted(all_skipped):
            print(f"   - {m}")
    
    if dry_run:
        print(f"\n[DRY RUN] No changes written")
        print(f"To apply: python {sys.argv[0]} --apply")
    else:
        print(f"\n[APPLIED] {len(results)} files updated")


if __name__ == "__main__":
    main()
