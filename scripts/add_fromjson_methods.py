#!/usr/bin/env python3
"""
Add fromJson() methods to classes that are missing them.
Based on existing toJson() methods and FieldLoader parsing logic.
"""

import os
import re

BASE_PATH = "/mnt/c/Users/Jean/the-virus-block-template-1.21.6"

# Define the fromJson implementations for each class
FROMJSON_IMPLEMENTATIONS = {
    # FillConfig - already done manually, skip
    
    # VisibilityMask
    "src/main/java/net/cyberpunk042/visual/visibility/VisibilityMask.java": {
        "find": "    public static Builder builder() { return new Builder(); }",
        "insert_before": """    // =========================================================================
    // Serialization
    // =========================================================================
    
    /**
     * Parses VisibilityMask from JSON.
     */
    public static VisibilityMask fromJson(com.google.gson.JsonElement json) {
        // String shorthand: "visibility": "BANDS"
        if (json.isJsonPrimitive()) {
            try {
                MaskType mask = MaskType.valueOf(json.getAsString().toUpperCase());
                return builder().mask(mask).build();
            } catch (Exception e) {
                return FULL;
            }
        }
        
        // Full object
        JsonObject obj = json.getAsJsonObject();
        Builder builder = builder();
        
        if (obj.has("mask")) {
            try {
                builder.mask(MaskType.valueOf(obj.get("mask").getAsString().toUpperCase()));
            } catch (Exception ignored) {}
        }
        if (obj.has("count")) builder.count(obj.get("count").getAsInt());
        if (obj.has("thickness")) builder.thickness(obj.get("thickness").getAsFloat());
        if (obj.has("offset")) builder.offset(obj.get("offset").getAsFloat());
        if (obj.has("invert")) builder.invert(obj.get("invert").getAsBoolean());
        if (obj.has("animated")) builder.animated(obj.get("animated").getAsBoolean());
        if (obj.has("animateSpeed")) builder.animateSpeed(obj.get("animateSpeed").getAsFloat());
        if (obj.has("centerX")) builder.centerX(obj.get("centerX").getAsFloat());
        if (obj.has("centerY")) builder.centerY(obj.get("centerY").getAsFloat());
        
        return builder.build();
    }

"""
    },
    
    # ArrangementConfig
    "src/main/java/net/cyberpunk042/visual/pattern/ArrangementConfig.java": {
        "find": "    public static Builder builder() { return new Builder(); }",
        "insert_before": """    // =========================================================================
    // Serialization
    // =========================================================================
    
    /**
     * Parses ArrangementConfig from JSON.
     */
    public static ArrangementConfig fromJson(com.google.gson.JsonElement json) {
        // String shorthand: "arrangement": "wave_1"
        if (json.isJsonPrimitive()) {
            return builder().defaultPattern(json.getAsString()).build();
        }
        
        // Full object
        JsonObject obj = json.getAsJsonObject();
        Builder builder = builder();
        
        if (obj.has("default")) {
            builder.defaultPattern(obj.get("default").getAsString());
        }
        if (obj.has("defaultPattern")) {
            builder.defaultPattern(obj.get("defaultPattern").getAsString());
        }
        if (obj.has("patterns") && obj.get("patterns").isJsonObject()) {
            JsonObject patterns = obj.getAsJsonObject("patterns");
            for (String key : patterns.keySet()) {
                if (patterns.get(key).isJsonObject()) {
                    builder.pattern(key, PatternConfig.fromJson(patterns.getAsJsonObject(key)));
                }
            }
        }
        
        return builder.build();
    }

"""
    },
    
    # Appearance
    "src/main/java/net/cyberpunk042/visual/appearance/Appearance.java": {
        "find": "    public static Builder builder() { return new Builder(); }",
        "insert_before": """    // =========================================================================
    // Serialization
    // =========================================================================
    
    /**
     * Parses Appearance from JSON.
     */
    public static Appearance fromJson(JsonObject json) {
        Builder builder = builder();
        
        if (json.has("color")) {
            builder.color(json.get("color").getAsString());
        }
        if (json.has("alpha")) {
            com.google.gson.JsonElement alphaEl = json.get("alpha");
            if (alphaEl.isJsonPrimitive()) {
                builder.alpha(AlphaRange.of(alphaEl.getAsFloat()));
            } else if (alphaEl.isJsonObject()) {
                JsonObject alphaObj = alphaEl.getAsJsonObject();
                float min = alphaObj.has("min") ? alphaObj.get("min").getAsFloat() : 1.0f;
                float max = alphaObj.has("max") ? alphaObj.get("max").getAsFloat() : min;
                builder.alpha(AlphaRange.of(min, max));
            }
        }
        if (json.has("glow")) builder.glow(json.get("glow").getAsFloat());
        if (json.has("emissive")) builder.emissive(json.get("emissive").getAsFloat());
        if (json.has("saturation")) builder.saturation(json.get("saturation").getAsFloat());
        if (json.has("pattern") && json.get("pattern").isJsonObject()) {
            builder.pattern(PatternConfig.fromJson(json.getAsJsonObject("pattern")));
        }
        
        return builder.build();
    }

"""
    },
    
    # FieldLayer
    "src/main/java/net/cyberpunk042/field/FieldLayer.java": {
        "find": "    public static Builder builder(String id) {",
        "insert_before": """    // =========================================================================
    // Serialization
    // =========================================================================
    
    /**
     * Parses FieldLayer from JSON.
     * Delegates to FieldLoader for full parsing with $ref resolution.
     */
    public static FieldLayer fromJson(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : "layer";
        boolean visible = !json.has("visible") || json.get("visible").getAsBoolean();
        float alpha = json.has("alpha") ? json.get("alpha").getAsFloat() : 1.0f;
        
        BlendMode blendMode = BlendMode.NORMAL;
        if (json.has("blendMode")) {
            try {
                blendMode = BlendMode.valueOf(json.get("blendMode").getAsString().toUpperCase());
            } catch (Exception ignored) {}
        }
        
        Transform transform = json.has("transform") 
            ? Transform.fromJson(json.getAsJsonObject("transform"))
            : Transform.IDENTITY;
        
        Animation animation = json.has("animation")
            ? Animation.fromJson(json.getAsJsonObject("animation"))
            : Animation.NONE;
        
        // Note: primitives require FieldLoader for full parsing with $ref
        // For direct fromJson, we create empty list - use FieldLoader.parseDefinition() for full parsing
        java.util.List<Primitive> primitives = java.util.Collections.emptyList();
        
        return new FieldLayer(id, primitives, transform, animation, alpha, visible, blendMode);
    }

"""
    },
    
    # SimplePrimitive
    "src/main/java/net/cyberpunk042/field/loader/SimplePrimitive.java": {
        "find": "    /**\n     * Serializes this primitive to JSON.\n     */\n    public JsonObject toJson() {",
        "insert_before": """    // =========================================================================
    // Serialization
    // =========================================================================
    
    /**
     * Parses SimplePrimitive from JSON.
     * Note: For full parsing with $ref resolution, use FieldLoader.
     */
    public static SimplePrimitive fromJson(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : "primitive";
        String type = json.has("type") ? json.get("type").getAsString() : "sphere";
        
        // Shape - needs type-specific parsing
        Shape shape = null;
        if (json.has("shape") && json.get("shape").isJsonObject()) {
            JsonObject shapeJson = json.getAsJsonObject("shape");
            shape = parseShape(type, shapeJson);
        }
        if (shape == null) {
            shape = new net.cyberpunk042.visual.shape.SphereShape(1.0f, 32, 64, 0f, 1f, "LAT_LON");
        }
        
        Transform transform = json.has("transform")
            ? Transform.fromJson(json.getAsJsonObject("transform"))
            : Transform.IDENTITY;
        
        FillConfig fill = json.has("fill")
            ? FillConfig.fromJson(json.get("fill"))
            : FillConfig.SOLID;
        
        VisibilityMask visibility = json.has("visibility")
            ? VisibilityMask.fromJson(json.get("visibility"))
            : VisibilityMask.FULL;
        
        ArrangementConfig arrangement = json.has("arrangement")
            ? ArrangementConfig.fromJson(json.get("arrangement"))
            : ArrangementConfig.DEFAULT;
        
        Appearance appearance = json.has("appearance")
            ? Appearance.fromJson(json.getAsJsonObject("appearance"))
            : Appearance.DEFAULT;
        
        Animation animation = json.has("animation")
            ? Animation.fromJson(json.getAsJsonObject("animation"))
            : Animation.NONE;
        
        PrimitiveLink link = json.has("link")
            ? PrimitiveLink.fromJson(json.getAsJsonObject("link"))
            : PrimitiveLink.NONE;
        
        return new SimplePrimitive(id, type, shape, transform, fill, visibility, arrangement, appearance, animation, link);
    }
    
    private static Shape parseShape(String type, JsonObject json) {
        return switch (type.toLowerCase()) {
            case "sphere" -> net.cyberpunk042.visual.shape.SphereShape.fromJson(json);
            case "ring" -> net.cyberpunk042.visual.shape.RingShape.builder()
                .innerRadius(json.has("innerRadius") ? json.get("innerRadius").getAsFloat() : 0.8f)
                .outerRadius(json.has("outerRadius") ? json.get("outerRadius").getAsFloat() : 1.0f)
                .segments(json.has("segments") ? json.get("segments").getAsInt() : 32)
                .build();
            case "disc" -> net.cyberpunk042.visual.shape.DiscShape.builder()
                .radius(json.has("radius") ? json.get("radius").getAsFloat() : 1.0f)
                .segments(json.has("segments") ? json.get("segments").getAsInt() : 32)
                .build();
            case "prism" -> net.cyberpunk042.visual.shape.PrismShape.builder()
                .sides(json.has("sides") ? json.get("sides").getAsInt() : 6)
                .radius(json.has("radius") ? json.get("radius").getAsFloat() : 1.0f)
                .height(json.has("height") ? json.get("height").getAsFloat() : 2.0f)
                .build();
            case "cylinder" -> net.cyberpunk042.visual.shape.CylinderShape.builder()
                .radius(json.has("radius") ? json.get("radius").getAsFloat() : 1.0f)
                .height(json.has("height") ? json.get("height").getAsFloat() : 2.0f)
                .segments(json.has("segments") ? json.get("segments").getAsInt() : 32)
                .build();
            case "polyhedron" -> net.cyberpunk042.visual.shape.PolyhedronShape.builder()
                .polyType(json.has("polyType") 
                    ? net.cyberpunk042.visual.shape.PolyType.valueOf(json.get("polyType").getAsString().toUpperCase())
                    : net.cyberpunk042.visual.shape.PolyType.ICOSAHEDRON)
                .radius(json.has("radius") ? json.get("radius").getAsFloat() : 1.0f)
                .build();
            default -> new net.cyberpunk042.visual.shape.SphereShape(1.0f, 32, 64, 0f, 1f, "LAT_LON");
        };
    }

"""
    },
}

def add_fromjson(filepath, config):
    """Add fromJson method to a file."""
    full_path = os.path.join(BASE_PATH, filepath)
    
    if not os.path.exists(full_path):
        print(f"  ❌ File not found: {filepath}")
        return False
    
    with open(full_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    find_text = config["find"]
    insert_text = config["insert_before"]
    
    if "fromJson" in content and "public static" in content and "fromJson(" in content:
        print(f"  ⚠️  {filepath} - fromJson already exists, skipping")
        return True
    
    if find_text not in content:
        print(f"  ❌ {filepath} - Could not find insertion point")
        print(f"     Looking for: {find_text[:60]}...")
        return False
    
    new_content = content.replace(find_text, insert_text + find_text)
    
    with open(full_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    
    print(f"  ✅ {filepath}")
    return True

def main():
    print("=" * 60)
    print("Adding fromJson() methods")
    print("=" * 60)
    
    success_count = 0
    for filepath, config in FROMJSON_IMPLEMENTATIONS.items():
        if add_fromjson(filepath, config):
            success_count += 1
    
    print()
    print(f"Done: {success_count}/{len(FROMJSON_IMPLEMENTATIONS)} files updated")
    
    # Also need to add imports where missing
    print()
    print("Checking imports...")
    
    # VisibilityMask needs JsonObject import
    vis_path = os.path.join(BASE_PATH, "src/main/java/net/cyberpunk042/visual/visibility/VisibilityMask.java")
    with open(vis_path, 'r') as f:
        content = f.read()
    if "import com.google.gson.JsonObject;" not in content:
        content = content.replace(
            "package net.cyberpunk042.visual.visibility;",
            "package net.cyberpunk042.visual.visibility;\n\nimport com.google.gson.JsonObject;"
        )
        with open(vis_path, 'w') as f:
            f.write(content)
        print("  ✅ Added JsonObject import to VisibilityMask")
    
    # ArrangementConfig needs JsonObject import
    arr_path = os.path.join(BASE_PATH, "src/main/java/net/cyberpunk042/visual/pattern/ArrangementConfig.java")
    with open(arr_path, 'r') as f:
        content = f.read()
    if "import com.google.gson.JsonObject;" not in content:
        content = content.replace(
            "package net.cyberpunk042.visual.pattern;",
            "package net.cyberpunk042.visual.pattern;\n\nimport com.google.gson.JsonObject;"
        )
        with open(arr_path, 'w') as f:
            f.write(content)
        print("  ✅ Added JsonObject import to ArrangementConfig")

if __name__ == "__main__":
    main()











