#!/usr/bin/env python3
"""
Fix compilation errors in fromJson() methods.
"""

import os

BASE_PATH = "/mnt/c/Users/Jean/the-virus-block-template-1.21.6"

FIXES = [
    # SimplePrimitive - use SphereShape.fromJson() instead of constructor
    {
        "file": "src/main/java/net/cyberpunk042/field/loader/SimplePrimitive.java",
        "find": '            shape = new net.cyberpunk042.visual.shape.SphereShape(1.0f, 32, 64, 0f, 1f, "LAT_LON");',
        "replace": '            shape = net.cyberpunk042.visual.shape.SphereShape.builder().radius(1.0f).latSteps(32).lonSteps(64).build();'
    },
    {
        "file": "src/main/java/net/cyberpunk042/field/loader/SimplePrimitive.java",
        "find": '            default -> new net.cyberpunk042.visual.shape.SphereShape(1.0f, 32, 64, 0f, 1f, "LAT_LON");',
        "replace": '            default -> net.cyberpunk042.visual.shape.SphereShape.builder().radius(1.0f).latSteps(32).lonSteps(64).build();'
    },
    
    # VisibilityMask - correct method names
    {
        "file": "src/main/java/net/cyberpunk042/visual/visibility/VisibilityMask.java",
        "find": '        if (obj.has("animated")) builder.animated(obj.get("animated").getAsBoolean());',
        "replace": '        if (obj.has("animate")) builder.animate(obj.get("animate").getAsBoolean());'
    },
    {
        "file": "src/main/java/net/cyberpunk042/visual/visibility/VisibilityMask.java",
        "find": '        if (obj.has("animateSpeed")) builder.animateSpeed(obj.get("animateSpeed").getAsFloat());',
        "replace": '        if (obj.has("animSpeed")) builder.animSpeed(obj.get("animSpeed").getAsFloat());'
    },
    
    # ArrangementConfig - remove PatternConfig reference (not available/needed)
    {
        "file": "src/main/java/net/cyberpunk042/visual/pattern/ArrangementConfig.java",
        "find": '''        if (obj.has("patterns") && obj.get("patterns").isJsonObject()) {
            JsonObject patterns = obj.getAsJsonObject("patterns");
            for (String key : patterns.keySet()) {
                if (patterns.get(key).isJsonObject()) {
                    builder.pattern(key, PatternConfig.fromJson(patterns.getAsJsonObject(key)));
                }
            }
        }''',
        "replace": '''        // Note: Per-pattern configs would need PatternConfig.fromJson()
        // For now, patterns are parsed via defaultPattern only'''
    },
    
    # Appearance - remove pattern() call (not in Builder)
    {
        "file": "src/main/java/net/cyberpunk042/visual/appearance/Appearance.java",
        "find": '''        if (json.has("pattern") && json.get("pattern").isJsonObject()) {
            builder.pattern(PatternConfig.fromJson(json.getAsJsonObject("pattern")));
        }''',
        "replace": '''        // Note: PatternConfig is handled separately via ArrangementConfig'''
    },
]

def apply_fix(fix):
    filepath = os.path.join(BASE_PATH, fix["file"])
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if fix["find"] not in content:
        print(f"  ⚠️  {fix['file']} - pattern not found")
        return False
    
    content = content.replace(fix["find"], fix["replace"])
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"  ✅ {fix['file']}")
    return True

def main():
    print("=" * 60)
    print("Fixing fromJson() compilation errors")
    print("=" * 60)
    
    success = 0
    for fix in FIXES:
        if apply_fix(fix):
            success += 1
    
    print()
    print(f"Done: {success}/{len(FIXES)} fixes applied")

if __name__ == "__main__":
    main()














