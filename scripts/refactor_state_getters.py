#!/usr/bin/env python3
"""
Refactor state getter methods to use path-based accessors.

Usage:
    python scripts/refactor_state_getters.py                    # Dry run
    python scripts/refactor_state_getters.py --apply            # Apply changes
    python scripts/refactor_state_getters.py --coverage         # Show coverage analysis

Transforms:
    state.getSphereLatSteps()  →  state.getInt("sphere.latSteps")
    state.getSpinSpeed()       →  state.getFloat("spin.speed")
    state.isSpinEnabled()      →  state.spin().isActive()
    state.getBool("spin.enabled") → state.spin().isActive()  # Fix incorrect path
    state.getShapeType()       →  state.getString("shapeType")
"""

import os
import re
import sys
from typing import Dict, Set, Tuple, Optional
from collections import defaultdict

# ═══════════════════════════════════════════════════════════════════════════════
# PATH MAPPINGS - getter method name to (path, type)
# type: "int", "float", "bool", "string", "object"
# ═══════════════════════════════════════════════════════════════════════════════

# Mapping: method_name -> (path, return_type)
GETTER_MAPPINGS: Dict[str, Tuple[str, str]] = {
    # Shape type and radius (direct fields)
    'getShapeType': ('shapeType', 'string'),
    'getRadius': ('radius', 'float'),
    
    # Appearance (via appearance record)
    'getColor': ('appearance.color', 'int'),
    'getAlpha': ('appearance.alpha', 'float'),
    'getGlow': ('appearance.glow', 'float'),
    'getEmissive': ('appearance.emissive', 'float'),
    'getSaturation': ('appearance.saturation', 'float'),
    'getPrimaryColor': ('appearance.primaryColor', 'int'),
    'getSecondaryColor': ('appearance.secondaryColor', 'int'),
    
    # Sphere shape
    'getSphereLatSteps': ('sphere.latSteps', 'int'),
    'getSphereLonSteps': ('sphere.lonSteps', 'int'),
    'getSphereLatStart': ('sphere.latStart', 'float'),
    'getSphereLatEnd': ('sphere.latEnd', 'float'),
    'getSphereAlgorithm': ('sphere.algorithm', 'string'),
    
    # Ring shape
    'getRingInnerRadius': ('ring.innerRadius', 'float'),
    'getRingOuterRadius': ('ring.outerRadius', 'float'),
    'getRingSegments': ('ring.segments', 'int'),
    'getRingHeight': ('ring.height', 'float'),
    'getRingY': ('ring.y', 'float'),
    'getRingArcStart': ('ring.arcStart', 'float'),
    'getRingArcEnd': ('ring.arcEnd', 'float'),
    'getRingTwist': ('ring.twist', 'float'),
    
    # Disc shape
    'getDiscRadius': ('disc.radius', 'float'),
    'getDiscSegments': ('disc.segments', 'int'),
    'getDiscY': ('disc.y', 'float'),
    'getDiscInnerRadius': ('disc.innerRadius', 'float'),
    'getDiscArcStart': ('disc.arcStart', 'float'),
    'getDiscArcEnd': ('disc.arcEnd', 'float'),
    'getDiscRings': ('disc.rings', 'int'),
    
    # Prism shape
    'getPrismSides': ('prism.sides', 'int'),
    'getPrismRadius': ('prism.radius', 'float'),
    'getPrismHeight': ('prism.height', 'float'),
    'getPrismTopRadius': ('prism.topRadius', 'float'),
    'getPrismTwist': ('prism.twist', 'float'),
    'getPrismHeightSegments': ('prism.heightSegments', 'int'),
    'isPrismCapTop': ('prism.capTop', 'bool'),
    'isPrismCapBottom': ('prism.capBottom', 'bool'),
    
    # Cylinder shape
    'getCylinderRadius': ('cylinder.radius', 'float'),
    'getCylinderHeight': ('cylinder.height', 'float'),
    'getCylinderSegments': ('cylinder.segments', 'int'),
    'getCylinderTopRadius': ('cylinder.topRadius', 'float'),
    'getCylinderArc': ('cylinder.arc', 'float'),
    'getCylinderHeightSegments': ('cylinder.heightSegments', 'int'),
    'isCylinderCapTop': ('cylinder.capTop', 'bool'),
    'isCylinderCapBottom': ('cylinder.capBottom', 'bool'),
    'isCylinderOpenEnded': ('cylinder.openEnded', 'bool'),
    
    # Polyhedron shape
    'getPolyType': ('polyhedron.type', 'string'),
    'getPolyRadius': ('polyhedron.radius', 'float'),
    'getPolySubdivisions': ('polyhedron.subdivisions', 'int'),
    
    # Transform
    'getScale': ('transform.scale', 'float'),
    'getOffsetX': ('transform.offset.x', 'float'),
    'getOffsetY': ('transform.offset.y', 'float'),
    'getOffsetZ': ('transform.offset.z', 'float'),
    'getRotationX': ('transform.rotation.x', 'float'),
    'getRotationY': ('transform.rotation.y', 'float'),
    'getRotationZ': ('transform.rotation.z', 'float'),
    
    # Orbit (HAS enabled field)
    'isOrbitEnabled': ('orbit.enabled', 'bool'),
    'getOrbitSpeed': ('orbit.speed', 'float'),
    'getOrbitRadius': ('orbit.radius', 'float'),
    'getOrbitPhase': ('orbit.phase', 'float'),
    'getOrbitAxis': ('orbit.axis', 'string'),
    
    # Fill config
    'getFillMode': ('fill.mode', 'string'),
    'getWireThickness': ('fill.wireThickness', 'float'),
    'isDoubleSided': ('fill.doubleSided', 'bool'),
    'isDepthTest': ('fill.depthTest', 'bool'),
    'isDepthWrite': ('fill.depthWrite', 'bool'),
    
    # Mask/Visibility
    'getMaskType': ('mask.type', 'string'),
    'getMaskThickness': ('mask.thickness', 'float'),
    'getMaskFeather': ('mask.feather', 'float'),
    'getMaskOffset': ('mask.offset', 'float'),
    'getMaskCount': ('mask.count', 'int'),
    'isMaskAnimated': ('mask.animate', 'bool'),
    'isMaskInverted': ('mask.invert', 'bool'),
    'getMaskAnimateSpeed': ('mask.animSpeed', 'float'),
    
    # Arrangement
    'getQuadPattern': ('arrangement.quadPattern', 'string'),
    'getSegmentPattern': ('arrangement.segmentPattern', 'string'),
    'getSectorPattern': ('arrangement.sectorPattern', 'string'),
    'isMultiPartArrangement': ('arrangement.multiPart', 'bool'),
    
    # Spin animation - isSpinEnabled uses isActive(), not a field
    'getSpinSpeed': ('spin.speed', 'float'),
    'getSpinAxis': ('spin.axis', 'string'),
    
    # Pulse animation - isPulseEnabled uses isActive(), not a field
    'getPulseFrequency': ('pulse.speed', 'float'),  # Note: GUI calls it frequency, record calls it speed
    'getPulseAmplitude': ('pulse.amplitude', 'float'),
    'getPulseMode': ('pulse.mode', 'string'),
    
    # Alpha pulse - isAlphaFadeEnabled uses isActive(), not a field
    'getAlphaMin': ('alphaPulse.min', 'float'),
    'getAlphaMax': ('alphaPulse.max', 'float'),
    
    # Wobble - isWobbleEnabled uses isActive(), not a field
    'getWobbleAmplitude': ('wobble.amplitude', 'float'),
    'getWobbleSpeed': ('wobble.speed', 'float'),
    
    # Modifiers
    'getModifierBobbing': ('modifiers.bobbing', 'float'),
    'getModifierBreathing': ('modifiers.breathing', 'float'),
    'getWobbleSpeed': ('wobble.speed', 'float'),
    
    # Wave - isWaveEnabled uses isActive(), not a field
    'getWaveAmplitude': ('wave.amplitude', 'float'),
    'getWaveFrequency': ('wave.frequency', 'float'),
    'getWaveDirection': ('wave.direction', 'string'),
    
    # Color cycle - isColorCycleEnabled uses isActive(), not a field
    'getColorCycleSpeed': ('colorCycle.speed', 'float'),
    'isColorCycleBlend': ('colorCycle.blend', 'bool'),
    
    # Beam (HAS enabled field)
    'isBeamEnabled': ('beam.enabled', 'bool'),
    'getBeamColor': ('beam.color', 'int'),
    'getBeamGlow': ('beam.glow', 'float'),
    'getBeamHeight': ('beam.height', 'float'),
    'getBeamInnerRadius': ('beam.innerRadius', 'float'),
    'getBeamOuterRadius': ('beam.outerRadius', 'float'),
    # beam.pulse is PulseConfig - uses isActive(), no enabled field
    'getBeamPulseSpeed': ('beam.pulse.speed', 'float'),
    'getBeamPulseMin': ('beam.pulse.min', 'float'),
    'getBeamPulseMax': ('beam.pulse.max', 'float'),
    'getBeamPulseScale': ('beam.pulse.scale', 'float'),
    'getBeamPulseWaveform': ('beam.pulse.waveform', 'string'),
    
    # Follow mode (direct fields)
    'getFollowMode': ('followMode', 'string'),
    'isFollowEnabled': ('followEnabled', 'bool'),
    
    # Prediction
    'isPredictionEnabled': ('predictionEnabled', 'bool'),
    'getPredictionLeadTicks': ('prediction.leadTicks', 'int'),
    'getPredictionMaxDistance': ('prediction.maxDistance', 'float'),
    'getPredictionLookAhead': ('prediction.lookAhead', 'float'),
    'getPredictionVerticalBoost': ('prediction.verticalBoost', 'float'),
    
    # Lifecycle (direct fields)
    'getFadeInTicks': ('fadeInTicks', 'int'),
    'getFadeOutTicks': ('fadeOutTicks', 'int'),
    
    # Trigger (direct fields)
    'getTriggerDuration': ('triggerDuration', 'float'),
    'getTriggerIntensity': ('triggerIntensity', 'float'),
    
    # Linking (direct fields)
    'getMirrorAxis': ('mirrorAxis', 'string'),
    'getPhaseOffset': ('phaseOffset', 'float'),
    'getRadiusOffset': ('radiusOffset', 'float'),
    'getPrimitiveId': ('primitiveId', 'string'),
    'isFollowLinked': ('followLinked', 'bool'),
    'isScaleWithLinked': ('scaleWithLinked', 'bool'),
    
    # Settings (direct fields)
    'isLivePreviewEnabled': ('livePreviewEnabled', 'bool'),
    'isAutoSaveEnabled': ('autoSaveEnabled', 'bool'),
    'isDebugUnlocked': ('debugUnlocked', 'bool'),
    
    # Lifecycle (direct fields)
    'getLifecycleState': ('lifecycleState', 'string'),
    
    # Trigger (direct fields) - additional
    'getTriggerType': ('triggerType', 'string'),
    'getTriggerEffect': ('triggerEffect', 'string'),
}

# Special isActive() mappings - these ANIMATION configs use isActive() method, NOT enabled field
# Transform: isSpinEnabled() → state.spin().isActive()
# These configs determine "enabled" by checking if speed/values are non-zero
IS_ACTIVE_MAPPINGS: Dict[str, str] = {
    'isSpinEnabled': 'spin',
    'isPulseEnabled': 'pulse',
    'isAlphaFadeEnabled': 'alphaPulse',
    'isWobbleEnabled': 'wobble',
    'isWaveEnabled': 'wave',
    'isColorCycleEnabled': 'colorCycle',
    'isBeamPulseEnabled': 'beam().pulse',  # beam.pulse is PulseConfig
}
# Note: BeamConfig, OrbitConfig, PredictionConfig have actual 'enabled' fields - use GETTER_MAPPINGS

# getBool("xxx.enabled") patterns that need to be fixed to xxx().isActive()
# These are incorrectly using path-based access for configs without "enabled" field
GETBOOL_ENABLED_FIX: Dict[str, str] = {
    'spin.enabled': 'spin',
    'pulse.enabled': 'pulse',
    'alphaPulse.enabled': 'alphaPulse',
    'wobble.enabled': 'wobble',
    'wave.enabled': 'wave',
    'colorCycle.enabled': 'colorCycle',
    'beam.pulse.enabled': 'beam().pulse',
}

# Methods to skip (they are real methods, not field accessors)
SKIP_METHODS: Set[str] = {
    'isDirty',
    'getProfiles',
    'getCurrentShapeFragmentName',
    'getCurrentFillFragmentName',
    'getCurrentAnimationFragmentName',
    'restoreFromSnapshot',
    'markDirty',
    'clearDirty',
    'getSelectedLayerIndex',
    'getProfileSnapshot',
    'getCageLatCount',
    'getCageLonCount',
    'getClass',
    'getLayerCount',
    'getSelectedPrimitiveIndex',
    'getCurrentProfileName',
    'isCurrentProfileServerSourced',
    'getPointSize',  # Handled by CageOptionsAdapter
    'getLayers',  # Real method - returns layer list
    'getLayerAlpha',  # Real method with index param
    'getLayerBlendMode',  # Real method with index param
    'isLayerVisible',  # Real method with index param
}

CLIENT_DIR = "src/client/java/net/cyberpunk042/client"


def get_accessor_method(return_type: str) -> str:
    """Get the appropriate accessor method name for a return type."""
    return {
        'int': 'getInt',
        'float': 'getFloat',
        'bool': 'getBool',
        'string': 'getString',
        'object': 'get',
    }.get(return_type, 'get')


def transform_getter(match: re.Match, state_var: str) -> Tuple[str, Optional[str], bool]:
    """
    Transform a getter call to use path-based accessor.
    Returns (replacement, old_call, should_skip).
    """
    method_name = match.group(1)
    old_call = f"{state_var}.{method_name}()"
    
    if method_name in SKIP_METHODS:
        return old_call, old_call, True
    
    if method_name in GETTER_MAPPINGS:
        path, return_type = GETTER_MAPPINGS[method_name]
        accessor = get_accessor_method(return_type)
        return f'{state_var}.{accessor}("{path}")', old_call, False
    
    # Unknown getter
    return old_call, old_call, False


def process_file(filepath: str, apply: bool = False) -> Tuple[int, list, list, list]:
    """
    Process a single file and return (change_count, changes, skipped, unknown).
    """
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    changes = []
    skipped = []
    unknown = []
    
    # Find all getter calls on state variables
    # Pattern: state.getXxx() or state.isXxx()
    pattern = r'\bstate\.(get[A-Z]\w*|is[A-Z]\w*)\(\)'
    
    def replacer(match):
        method_name = match.group(1)
        full_match = match.group(0)
        
        if method_name in SKIP_METHODS:
            skipped.append(method_name)
            return full_match
        
        # Handle isXxxEnabled() → state.xxx().isActive()
        if method_name in IS_ACTIVE_MAPPINGS:
            config_name = IS_ACTIVE_MAPPINGS[method_name]
            replacement = f'state.{config_name}().isActive()'
            changes.append((method_name, replacement))
            return replacement
        
        if method_name in GETTER_MAPPINGS:
            path, return_type = GETTER_MAPPINGS[method_name]
            accessor = get_accessor_method(return_type)
            replacement = f'state.{accessor}("{path}")'
            changes.append((method_name, replacement))
            return replacement
        
        unknown.append(method_name)
        return full_match
    
    content = re.sub(pattern, replacer, content)
    
    # Fix incorrect getBool("xxx.enabled") → state.xxx().isActive()
    for bad_path, config_name in GETBOOL_ENABLED_FIX.items():
        bad_pattern = rf'state\.getBool\("{re.escape(bad_path)}"\)'
        replacement = f'state.{config_name}().isActive()'
        matches = re.findall(bad_pattern, content)
        for _ in matches:
            changes.append((f'getBool("{bad_path}")', replacement))
        content = re.sub(bad_pattern, replacement, content)
    
    if apply and content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
    
    return len(changes), changes, skipped, unknown


def find_java_files(directory: str) -> list:
    """Find all Java files in directory."""
    java_files = []
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith('.java'):
                java_files.append(os.path.join(root, file))
    return java_files


def main():
    apply = '--apply' in sys.argv
    coverage = '--coverage' in sys.argv
    
    java_files = find_java_files(CLIENT_DIR)
    
    total_changes = 0
    files_changed = 0
    all_changes = defaultdict(int)
    all_skipped = defaultdict(int)
    all_unknown = defaultdict(list)
    
    print("=" * 60)
    if coverage:
        print("COVERAGE ANALYSIS")
    elif apply:
        print("APPLYING CHANGES")
    else:
        print("DRY RUN - No changes will be made")
        print("Use --apply to apply changes")
    print("=" * 60)
    print()
    
    for filepath in java_files:
        change_count, changes, skipped, unknown = process_file(filepath, apply)
        
        if change_count > 0 or unknown:
            files_changed += 1
            total_changes += change_count
            
            for method, repl in changes:
                all_changes[method] += 1
            for method in skipped:
                all_skipped[method] += 1
            for method in unknown:
                all_unknown[method].append(filepath)
            
            if not coverage:
                rel_path = filepath.replace(CLIENT_DIR + "/", "")
                print(f"📁 {rel_path} ({change_count} changes)")
                for method, repl in changes[:5]:
                    # For getBool patterns, method already includes ()
                    if method.startswith('getBool('):
                        print(f"   state.{method} → {repl}")
                    else:
                        print(f"   state.{method}() → {repl}")
                if len(changes) > 5:
                    print(f"   ... and {len(changes) - 5} more")
    
    # Coverage analysis
    if coverage:
        print("✅ HANDLED ({} unique getters):".format(len(all_changes)))
        for method in sorted(all_changes.keys()):
            if method in GETTER_MAPPINGS:
                path, ret_type = GETTER_MAPPINGS[method]
                accessor = get_accessor_method(ret_type)
                print(f'   {method} ({all_changes[method]}x) → state.{accessor}("{path}")')
            elif method in IS_ACTIVE_MAPPINGS:
                config = IS_ACTIVE_MAPPINGS[method]
                print(f'   {method} ({all_changes[method]}x) → state.{config}().isActive()')
            elif method.startswith('getBool('):
                # getBool("xxx.enabled") patterns
                print(f'   {method} ({all_changes[method]}x) → isActive() fix')
            else:
                print(f'   {method} ({all_changes[method]}x)')
        
        print()
        print("⏭️  SKIPPED ({} unique getters):".format(len(all_skipped)))
        for method in sorted(all_skipped.keys()):
            print(f"   {method} ({all_skipped[method]}x) - SKIPPED (real method)")
        
        if all_unknown:
            print()
            print("❌ UNKNOWN ({} unique getters) - NEED TO ADD:".format(len(all_unknown)))
            for method in sorted(all_unknown.keys()):
                files = all_unknown[method]
                print(f"   {method} ({len(files)}x)")
                for f in files[:2]:
                    print(f"      in: {f.replace(CLIENT_DIR + '/', '')}")
        else:
            print()
            print("✅ NO UNKNOWN GETTERS - Full coverage!")
    
    print()
    print("=" * 60)
    print(f"Summary: {total_changes} changes in {files_changed} files")
    if not apply and not coverage:
        print("Run with --apply to apply these changes")
    print("=" * 60)


if __name__ == '__main__':
    main()
