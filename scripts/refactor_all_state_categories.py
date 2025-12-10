#!/usr/bin/env python3
"""
Refactor FieldEditState to use records directly (no wrapper classes, no forwarding).
Replace individual fields with the actual record + simple getter/setter.
"""

import re
import sys

TARGET_FILE = "src/client/java/net/cyberpunk042/client/gui/state/FieldEditState.java"

# ============================================================================
# Categories to refactor - simple record replacement
# ============================================================================

CATEGORIES = [
    {
        "name": "Shape",
        "section_start": "// SHAPE STATE",
        "section_end": "// BEAM CONFIG",
        "imports": [],  # Already imported
        "replacement": '''    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE STATE - Direct record usage (no wrapper class)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private SphereShape sphere = SphereShape.DEFAULT;
    private RingShape ring = RingShape.DEFAULT;
    private DiscShape disc = DiscShape.DEFAULT;
    private PrismShape prism = PrismShape.builder().build();
    private CylinderShape cylinder = CylinderShape.builder().build();
    private PolyhedronShape polyhedron = PolyhedronShape.DEFAULT;
    
    public SphereShape sphere() { return sphere; }
    public void setSphere(SphereShape s) { sphere = s; markDirty(); }
    public void updateSphere(java.util.function.Function<SphereShape.Builder, SphereShape.Builder> mod) {
        sphere = mod.apply(sphere.toBuilder()).build(); markDirty();
    }
    
    public RingShape ring() { return ring; }
    public void setRing(RingShape r) { ring = r; markDirty(); }
    public void updateRing(java.util.function.Function<RingShape.Builder, RingShape.Builder> mod) {
        ring = mod.apply(ring.toBuilder()).build(); markDirty();
    }
    
    public DiscShape disc() { return disc; }
    public void setDisc(DiscShape d) { disc = d; markDirty(); }
    public void updateDisc(java.util.function.Function<DiscShape.Builder, DiscShape.Builder> mod) {
        disc = mod.apply(disc.toBuilder()).build(); markDirty();
    }
    
    public PrismShape prism() { return prism; }
    public void setPrism(PrismShape p) { prism = p; markDirty(); }
    public void updatePrism(java.util.function.Function<PrismShape.Builder, PrismShape.Builder> mod) {
        prism = mod.apply(prism.toBuilder()).build(); markDirty();
    }
    
    public CylinderShape cylinder() { return cylinder; }
    public void setCylinder(CylinderShape c) { cylinder = c; markDirty(); }
    public void updateCylinder(java.util.function.Function<CylinderShape.Builder, CylinderShape.Builder> mod) {
        cylinder = mod.apply(cylinder.toBuilder()).build(); markDirty();
    }
    
    public PolyhedronShape polyhedron() { return polyhedron; }
    public void setPolyhedron(PolyhedronShape p) { polyhedron = p; markDirty(); }
    public void updatePolyhedron(java.util.function.Function<PolyhedronShape.Builder, PolyhedronShape.Builder> mod) {
        polyhedron = mod.apply(polyhedron.toBuilder()).build(); markDirty();
    }
    
    /** Get current shape based on shapeType field. */
    public Shape currentShape() {
        return switch (shapeType.toLowerCase()) {
            case "sphere" -> sphere;
            case "ring" -> ring;
            case "disc" -> disc;
            case "prism" -> prism;
            case "cylinder" -> cylinder;
            case "polyhedron" -> polyhedron;
            default -> sphere;
        };
    }

''',
    },
    {
        "name": "Transform",
        "section_start": "// TRANSFORM STATE",
        "section_end": "// ORBIT STATE",
        "imports": [
            "net.cyberpunk042.visual.transform.Transform",
        ],
        "replacement": '''    // ═══════════════════════════════════════════════════════════════════════════
    // TRANSFORM STATE - Uses Transform record directly
    // ═══════════════════════════════════════════════════════════════════════════
    
    private Transform transform = Transform.IDENTITY;
    
    public Transform transform() { return transform; }
    public void setTransform(Transform t) { transform = t; markDirty(); }
    
    /** Helper to update transform with a modifier function. */
    public void updateTransform(java.util.function.Function<Transform.Builder, Transform.Builder> modifier) {
        transform = modifier.apply(transform.toBuilder()).build();
        markDirty();
    }

''',
    },
    {
        "name": "Orbit",
        "section_start": "// ORBIT STATE (G-ORBIT)",
        "section_end": "// VISIBILITY MASK STATE",
        "imports": [
            "net.cyberpunk042.visual.transform.OrbitConfig",
        ],
        "replacement": '''    // ═══════════════════════════════════════════════════════════════════════════
    // ORBIT STATE - Uses OrbitConfig record directly
    // ═══════════════════════════════════════════════════════════════════════════
    
    private OrbitConfig orbit = OrbitConfig.NONE;
    
    public OrbitConfig orbit() { return orbit; }
    public void setOrbit(OrbitConfig o) { orbit = o; markDirty(); }
    
    public void updateOrbit(java.util.function.Function<OrbitConfig.Builder, OrbitConfig.Builder> modifier) {
        orbit = modifier.apply(orbit.toBuilder()).build();
        markDirty();
    }

''',
    },
    {
        "name": "Visibility",
        "section_start": "// VISIBILITY MASK STATE",
        "section_end": "// ARRANGEMENT STATE",
        "imports": [
            "net.cyberpunk042.visual.visibility.VisibilityMask",
        ],
        "replacement": '''    // ═══════════════════════════════════════════════════════════════════════════
    // VISIBILITY MASK STATE - Uses VisibilityMask record directly
    // ═══════════════════════════════════════════════════════════════════════════
    
    private VisibilityMask mask = VisibilityMask.FULL;
    
    public VisibilityMask mask() { return mask; }
    public void setMask(VisibilityMask m) { mask = m; markDirty(); }
    
    public void updateMask(java.util.function.Function<VisibilityMask.Builder, VisibilityMask.Builder> modifier) {
        mask = modifier.apply(mask.toBuilder()).build();
        markDirty();
    }

''',
    },
    {
        "name": "Arrangement",
        "section_start": "// ARRANGEMENT STATE",
        "section_end": "// FILL STATE",
        "imports": [
            "net.cyberpunk042.visual.pattern.ArrangementConfig",
        ],
        "replacement": '''    // ═══════════════════════════════════════════════════════════════════════════
    // ARRANGEMENT STATE - Uses ArrangementConfig record directly
    // ═══════════════════════════════════════════════════════════════════════════
    
    private ArrangementConfig arrangement = ArrangementConfig.DEFAULT;
    
    public ArrangementConfig arrangement() { return arrangement; }
    public void setArrangement(ArrangementConfig a) { arrangement = a; markDirty(); }
    
    public void updateArrangement(java.util.function.Function<ArrangementConfig.Builder, ArrangementConfig.Builder> modifier) {
        arrangement = modifier.apply(arrangement.toBuilder()).build();
        markDirty();
    }

''',
    },
    {
        "name": "Fill",
        "section_start": "// FILL STATE",
        "section_end": "// LINKING STATE",
        "imports": [
            "net.cyberpunk042.visual.fill.FillConfig",
        ],
        "replacement": '''    // ═══════════════════════════════════════════════════════════════════════════
    // FILL STATE - Uses FillConfig record directly
    // ═══════════════════════════════════════════════════════════════════════════
    
    private FillConfig fill = FillConfig.SOLID;
    
    public FillConfig fill() { return fill; }
    public void setFill(FillConfig f) { fill = f; markDirty(); }
    
    public void updateFill(java.util.function.Function<FillConfig.Builder, FillConfig.Builder> modifier) {
        fill = modifier.apply(fill.toBuilder()).build();
        markDirty();
    }

''',
    },
    {
        "name": "Linking",
        "section_start": "// LINKING STATE",
        "section_end": "// EXTENDED PREDICTION STATE",
        "imports": [
            "net.cyberpunk042.field.primitive.PrimitiveLink",
        ],
        "replacement": '''    // ═══════════════════════════════════════════════════════════════════════════
    // LINKING STATE - Uses PrimitiveLink record directly
    // ═══════════════════════════════════════════════════════════════════════════
    
    private PrimitiveLink link = null;
    
    public PrimitiveLink link() { return link; }
    public void setLink(PrimitiveLink l) { link = l; markDirty(); }

''',
    },
    {
        "name": "Prediction",
        "section_start": "// EXTENDED PREDICTION STATE",
        "section_end": "// FOLLOW MODE STATE",
        "imports": [
            "net.cyberpunk042.field.instance.PredictionConfig",
        ],
        "replacement": '''    // ═══════════════════════════════════════════════════════════════════════════
    // PREDICTION STATE - Uses PredictionConfig record directly
    // ═══════════════════════════════════════════════════════════════════════════
    
    private PredictionConfig prediction = PredictionConfig.DEFAULT;
    
    public PredictionConfig prediction() { return prediction; }
    public void setPrediction(PredictionConfig p) { prediction = p; markDirty(); }
    
    public void updatePrediction(java.util.function.Function<PredictionConfig.Builder, PredictionConfig.Builder> modifier) {
        prediction = modifier.apply(prediction.toBuilder()).build();
        markDirty();
    }

''',
    },
    {
        "name": "Follow",
        "section_start": "// FOLLOW MODE STATE",
        "section_end": "// ═══════════════════════════════════════════════════════════════════════════\n    // LAYER MANAGEMENT",
        "imports": [
            "net.cyberpunk042.field.instance.FollowModeConfig",
        ],
        "replacement": '''    // ═══════════════════════════════════════════════════════════════════════════
    // FOLLOW MODE STATE - Uses FollowModeConfig record directly
    // ═══════════════════════════════════════════════════════════════════════════
    
    private FollowModeConfig followConfig = FollowModeConfig.DEFAULT;
    
    public FollowModeConfig followConfig() { return followConfig; }
    public void setFollowConfig(FollowModeConfig f) { followConfig = f; markDirty(); }

''',
    },
]


def main():
    dry_run = '--dry-run' in sys.argv
    
    print("=" * 70)
    print("Refactoring FieldEditState - Direct Record Usage (No Wrappers)")
    if dry_run:
        print("🔍 DRY RUN MODE - No changes will be made")
    print("=" * 70)
    
    with open(TARGET_FILE, 'r', encoding='utf-8') as f:
        content = f.read()
    original_content = content
    original_lines = content.count('\n')
    
    print(f"\nProcessing {len(CATEGORIES)} categories:")
    
    changes_made = []
    lines_removed = 0
    lines_added = 0
    
    for cat in CATEGORIES:
        print(f"\n📦 {cat['name']}...")
        
        start_pattern = cat["section_start"]
        end_pattern = cat["section_end"]
        
        # Find section boundaries
        start_match = re.search(r'    // ═+\n    ' + re.escape(start_pattern), content)
        if not start_match:
            print(f"   ⚠️ Could not find: {start_pattern}")
            continue
        
        # Handle special case for Follow (last section before LAYER)
        if "LAYER MANAGEMENT" in end_pattern:
            end_match = re.search(r'// ═+\n    // LAYER MANAGEMENT', content[start_match.start():])
        else:
            end_match = re.search(r'    // ═+\n    ' + re.escape(end_pattern), content[start_match.start():])
        
        if not end_match:
            print(f"   ⚠️ Could not find end: {end_pattern}")
            continue
        
        section_start = start_match.start()
        section_end = start_match.start() + end_match.start()
        
        old_section = content[section_start:section_end]
        old_line_count = old_section.count('\n')
        new_line_count = cat["replacement"].count('\n')
        
        lines_removed += old_line_count
        lines_added += new_line_count
        
        print(f"   📝 {old_line_count} lines → {new_line_count} lines ({new_line_count - old_line_count:+d})")
        
        content = content[:section_start] + cat["replacement"] + content[section_end:]
        changes_made.append(cat['name'])
    
    # Collect and add imports
    all_imports = set()
    for cat in CATEGORIES:
        all_imports.update(cat.get("imports", []))
    
    import_lines = "\n".join(f"import {imp};" for imp in sorted(all_imports))
    import_insert = content.find("import java.util.ArrayList;")
    if import_insert != -1:
        content = content[:import_insert] + import_lines + "\n" + content[import_insert:]
        print(f"\n✅ Added {len(all_imports)} imports")
    
    # Summary
    new_lines = content.count('\n')
    
    print("\n" + "=" * 70)
    print("SUMMARY")
    print("=" * 70)
    print(f"📊 Line count: {original_lines} → {new_lines} ({new_lines - original_lines:+d})")
    print(f"   Removed: {lines_removed} lines")
    print(f"   Added:   {lines_added} lines")
    print(f"📦 Categories: {', '.join(changes_made)}")
    
    if dry_run:
        print("\n🔍 DRY RUN - No files modified")
    else:
        with open(TARGET_FILE + '.bak2', 'w', encoding='utf-8') as f:
            f.write(original_content)
        print(f"\n✅ Backup: {TARGET_FILE}.bak2")
        
        with open(TARGET_FILE, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"✅ Updated: {TARGET_FILE}")
        print("\n⚠️  NOTE: Callers need updating!")
        print("   Old: state.getScale()")
        print("   New: state.transform().scale()")


if __name__ == "__main__":
    main()
