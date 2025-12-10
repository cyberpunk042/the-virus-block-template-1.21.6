#!/usr/bin/env python3
"""
Clean FieldEditState.java:
1. KEEP coordination methods (addLayer, removeLayer, swapLayers, etc.)
2. KEEP selection methods with clamping
3. KEEP simple index getters
4. ADD getLayers() method for direct LayerState access
5. REMOVE only simple property accessors (pollution)
6. ADD typed record accessors for fluent API
"""

import re

FILE_PATH = "src/client/java/net/cyberpunk042/client/gui/state/FieldEditState.java"

def clean_file(dry_run=True):
    with open(FILE_PATH, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_lines = len(content.split('\n'))
    
    # ONLY remove simple property accessors (the TRUE pollution)
    # These are methods that just read/write a single field on LayerState
    methods_to_remove = [
        # Layer property accessors (callers should use getLayers().get(i).property)
        r'\s*public boolean isLayerVisible\(int index\)[^}]+\}',
        r'\s*public boolean toggleLayerVisibility\(int index\)[\s\S]+?return layers\.get[^}]+\}',
        r'\s*public String getLayerBlendMode\(int index\)[^}]+\}',
        r'\s*public void setLayerBlendMode\(int index, String mode\)[^}]+markDirty\(\);\s*\}',
        r'\s*public int getLayerOrder\(int index\)[^}]+\}',
        r'\s*public void setLayerOrder\(int index, int order\)[^}]+markDirty\(\);\s*\}',
        r'\s*public float getLayerAlpha\(int index\)[^}]+\}',
        r'\s*public void setLayerAlpha\(int index, float alpha\)[^}]+markDirty\(\);\s*\}',
        r'\s*public String getLayerName\(int index\)[^}]+\}',
        r'\s*public void setLayerName\(int index, String name\)[^}]+markDirty\(\);\s*\}',
        # Layer utility methods that can be done by caller
        r'\s*public List<String> getLayerNames\(\)[\s\S]+?return names;\s*\}',
        r'\s*public int findLayerByName\(String name\)[\s\S]+?return -1;\s*\}',
        r'\s*public int addLayerWithName\(String baseName\)[\s\S]+?return layers\.size\(\) - 1;\s*\}',
        r'\s*private String resolveLayerName\(String baseName\)[\s\S]+?return baseName \+ " " \+ counter;\s*\}',
        # Primitive property accessors
        r'\s*public String getPrimitiveName\(int layerIndex, int primitiveIndex\)[^}]+\}',
        r'\s*public void renamePrimitive\(int layerIndex, int primitiveIndex, String newId\)[^}]+markDirty\(\);\s*\}',
        # Primitive utility methods that can be done by caller
        r'\s*public int findPrimitiveById\(int layerIndex, String id\)[\s\S]+?return -1;\s*\}',
        r'\s*public int addPrimitiveWithId\(int layerIndex, String baseId\)[\s\S]+?return list\.size\(\) - 1;\s*\}',
        r'\s*private String resolvePrimitiveId\(int layerIndex, String baseId\)[\s\S]+?return baseId \+ "_" \+ counter;\s*\}',
    ]
    
    # NOTE: We KEEP these coordination methods:
    # - addLayer(), removeLayer(), swapLayers()
    # - addPrimitive(), removePrimitive(), swapPrimitives()
    # - getSelectedLayerIndex(), setSelectedLayerIndex()
    # - getSelectedPrimitiveIndex(), setSelectedPrimitiveIndex()
    # - getLayerCount(), getPrimitiveCount()
    # - clampPrimitiveSelection(), clampPrimitiveIndex(), isValidPrimitiveIndex()
    
    removed_count = 0
    for pattern in methods_to_remove:
        matches = re.findall(pattern, content, re.DOTALL)
        if matches:
            content = re.sub(pattern, '', content, flags=re.DOTALL)
            removed_count += len(matches)
    
    # Fix methods that got merged onto same line (}public, }private, }//)
    content = re.sub(r'\}(public|private|//)', r'}\n\n    \1', content)
    
    # Clean up multiple blank lines
    while '\n\n\n' in content:
        content = content.replace('\n\n\n', '\n\n')
    
    # Add getLayers() method after getLayerCount() if not already present
    if 'public List<LayerState> getLayers()' not in content:
        get_layer_count_pattern = r'(public int getLayerCount\(\) \{ return layers\.size\(\); \})'
        match = re.search(get_layer_count_pattern, content)
        if match:
            insert_pos = match.end()
            content = content[:insert_pos] + '\n    public List<LayerState> getLayers() { return layers; }' + content[insert_pos:]
            print("Added getLayers() method")
    
    # Add getPrimitivesForLayer() method if not already present  
    if 'public List<String> getPrimitivesForLayer(' not in content:
        get_prim_count_pattern = r'(public int getPrimitiveCount\(int layerIndex\)[^}]+\})'
        match = re.search(get_prim_count_pattern, content)
        if match:
            insert_pos = match.end()
            content = content[:insert_pos] + '\n    public List<String> getPrimitivesForLayer(int layerIndex) { return layerIndex >= 0 && layerIndex < primitivesPerLayer.size() ? primitivesPerLayer.get(layerIndex) : java.util.Collections.emptyList(); }' + content[insert_pos:]
            print("Added getPrimitivesForLayer() method")
    
    # Add typed record accessors after update() method if not already present
    if 'public SphereShape sphere()' not in content:
        accessors = '''
    // ═══════════════════════════════════════════════════════════════════════════
    // TYPED RECORD ACCESSORS (for fluent API: state.sphere().latSteps())
    // ═══════════════════════════════════════════════════════════════════════════

    public SphereShape sphere() { return sphere; }
    public RingShape ring() { return ring; }
    public DiscShape disc() { return disc; }
    public PrismShape prism() { return prism; }
    public CylinderShape cylinder() { return cylinder; }
    public PolyhedronShape polyhedron() { return polyhedron; }

    public Transform transform() { return transform; }
    public OrbitConfig orbit() { return orbit; }

    public FillConfig fill() { return fill; }
    public VisibilityMask mask() { return mask; }
    public ArrangementConfig arrangement() { return arrangement; }

    public SpinConfig spin() { return spin; }
    public PulseConfig pulse() { return pulse; }
    public AlphaPulseConfig alphaPulse() { return alphaPulse; }
    public WobbleConfig wobble() { return wobble; }
    public WaveConfig wave() { return wave; }
    public ColorCycleConfig colorCycle() { return colorCycle; }

    public FollowModeConfig followConfig() { return followConfig; }
    public PredictionConfig prediction() { return prediction; }
    public BeamConfig beam() { return beam; }
    public PrimitiveLink link() { return link; }
'''
        # Find where to insert (after update method)
        update_pattern = r'(public <T> void update\(String name, java\.util\.function\.Function<T, T> modifier\) \{\s*StateAccessor\.update\(this, name, modifier\);\s*markDirty\(\);\s*\})'
        match = re.search(update_pattern, content)
        if match:
            insert_pos = match.end()
            content = content[:insert_pos] + accessors + content[insert_pos:]
            print("Added typed record accessors")
        else:
            print("WARNING: Could not find update() method to insert accessors after")
    
    # Final cleanup of multiple blank lines
    while '\n\n\n' in content:
        content = content.replace('\n\n\n', '\n\n')
    
    new_lines = len(content.split('\n'))
    
    print(f"Removed {removed_count} simple property accessor methods")
    print(f"Line count: {original_lines} -> {new_lines} ({new_lines - original_lines:+d})")
    
    if dry_run:
        print("\n[DRY RUN] No changes written")
        print("\n--- Full preview ---")
        for i, line in enumerate(content.split('\n'), 1):
            print(f"{i:4d}|{line}")
    else:
        with open(FILE_PATH, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"\nChanges written to {FILE_PATH}")

if __name__ == "__main__":
    import sys
    dry_run = "--apply" not in sys.argv
    clean_file(dry_run=dry_run)
