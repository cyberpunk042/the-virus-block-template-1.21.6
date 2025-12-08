package net.cyberpunk042.visual.appearance;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;

/**
 * Visual appearance properties for a primitive.
 * 
 * <h2>Components</h2>
 * <ul>
 *   <li><b>color</b>: Color reference (theme role like "@primary" or hex "#FF0000")</li>
 *   <li><b>alpha</b>: Opacity range for pulsing effects</li>
 *   <li><b>fill</b>: Whether to render as solid fill</li>
 *   <li><b>pattern</b>: Surface pattern (bands, checker)</li>
 *   <li><b>glow</b>: Glow/emissive intensity (0.0 - 1.0)</li>
 *   <li><b>wireThickness</b>: Line thickness for wireframe mode</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * <pre>
 * Appearance.solid("@primary")
 * Appearance.translucent("@glow", 0.4f, 0.8f)
 * Appearance.banded("@secondary", 4, 0.3f)
 * Appearance.wireframe("@wire", 1.5f)
 * </pre>
 * 
 * @see AlphaRange
 * @see PatternConfig
 */
public record Appearance(
        String color,
        AlphaRange alpha,
        FillMode fill,
        PatternConfig pattern,
        float glow,
        float wireThickness
) {
    
    // =========================================================================
    // Presets
    // =========================================================================
    
    /** Default appearance: primary color, semi-transparent, filled. */
    public static final Appearance DEFAULT = new Appearance(
        "@primary", AlphaRange.TRANSLUCENT, FillMode.SOLID, PatternConfig.NONE, 0.0f, 1.0f);
    
    // =========================================================================
    // Factory Methods
    // =========================================================================
    
    public static Appearance defaults() {
        return DEFAULT;
    }
    
    /**
     * Solid opaque appearance.
     */
    public static Appearance solid(String color) {
        return new Appearance(color, AlphaRange.OPAQUE, FillMode.SOLID, PatternConfig.NONE, 0.0f, 1.0f);
    }
    
    /**
     * Translucent appearance with pulsing alpha.
     */
    public static Appearance translucent(String color, float alphaMin, float alphaMax) {
        return new Appearance(color, AlphaRange.of(alphaMin, alphaMax), FillMode.SOLID, PatternConfig.NONE, 0.0f, 1.0f);
    }
    
    /**
     * Translucent appearance with fixed alpha.
     */
    public static Appearance translucent(String color, float alpha) {
        return new Appearance(color, AlphaRange.fixed(alpha), FillMode.SOLID, PatternConfig.NONE, 0.0f, 1.0f);
    }
    
    /**
     * Wireframe appearance.
     */
    public static Appearance wireframe(String color) {
        return new Appearance(color, AlphaRange.OPAQUE, FillMode.WIREFRAME, PatternConfig.NONE, 0.0f, 1.0f);
    }
    
    /**
     * Wireframe with custom thickness.
     */
    public static Appearance wireframe(String color, float thickness) {
        return new Appearance(color, AlphaRange.OPAQUE, FillMode.WIREFRAME, PatternConfig.NONE, 0.0f, thickness);
    }
    
    /**
     * Glowing appearance.
     */
    public static Appearance glowing(String color, float glow) {
        return new Appearance(color, AlphaRange.STRONG, FillMode.SOLID, PatternConfig.NONE, glow, 1.0f);
    }
    
    /**
     * Banded appearance with stripes.
     */
    public static Appearance banded(String color, int bandCount, float bandThickness) {
        return new Appearance(color, AlphaRange.TRANSLUCENT, FillMode.SOLID, 
            PatternConfig.bands(bandCount, bandThickness), 0.0f, 1.0f);
    }
    
    /**
     * Checker pattern appearance.
     */
    public static Appearance checker(String color, int divisions) {
        return new Appearance(color, AlphaRange.TRANSLUCENT, FillMode.SOLID,
            PatternConfig.checker(divisions), 0.0f, 1.0f);
    }
    
    // =========================================================================
    // Builder-style
    // =========================================================================
    
    public Appearance withColor(String newColor) {
        return new Appearance(newColor, alpha, fill, pattern, glow, wireThickness);
    }
    
    public Appearance withAlpha(AlphaRange newAlpha) {
        return new Appearance(color, newAlpha, fill, pattern, glow, wireThickness);
    }
    
    public Appearance withAlpha(float min, float max) {
        return new Appearance(color, AlphaRange.of(min, max), fill, pattern, glow, wireThickness);
    }
    
    public Appearance withFill(FillMode newFill) {
        return new Appearance(color, alpha, newFill, pattern, glow, wireThickness);
    }
    
    public Appearance withPattern(PatternConfig newPattern) {
        return new Appearance(color, alpha, fill, newPattern, glow, wireThickness);
    }
    
    public Appearance withGlow(float newGlow) {
        return new Appearance(color, alpha, fill, pattern, newGlow, wireThickness);
    }
    
    public Appearance withWireThickness(float thickness) {
        return new Appearance(color, alpha, fill, pattern, glow, thickness);
    }
    
    // =========================================================================
    // Computed Properties
    // =========================================================================
    
    /**
     * Gets the current alpha value (midpoint of range).
     * For animated alpha, use {@link AlphaRange#at(float)}.
     */
    public float currentAlpha() {
        return alpha.midpoint();
    }
    
    /**
     * Checks if this appearance has any visible pattern.
     */
    public boolean hasPattern() {
        return pattern.hasPattern();
    }
    
    /**
     * Checks if alpha is pulsing.
     */
    public boolean isPulsing() {
        return alpha.isPulsing();
    }
    
    // =========================================================================
    // JSON Serialization
    // =========================================================================
    
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("color", color);
        
        // Alpha - compact format if not pulsing
        if (alpha.isPulsing()) {
            json.add("alpha", alpha.toJson());
        } else {
            json.addProperty("alpha", alpha.min());
        }
        
        json.addProperty("fill", fill.id());
        
        // Pattern - only include if has effect
        if (pattern.hasPattern()) {
            json.add("pattern", pattern.toJson());
        }
        
        if (glow > 0) {
            json.addProperty("glow", glow);
        }
        if (wireThickness != 1.0f) {
            json.addProperty("wireThickness", wireThickness);
        }
        
        return json;
    }
    
    public static Appearance fromJson(JsonObject json) {
        if (json == null) {
            return DEFAULT;
        }
        
        String color = json.has("color") ? json.get("color").getAsString() : "@primary";
        
        // Parse alpha (can be float or object)
        AlphaRange alpha;
        if (json.has("alpha")) {
            alpha = AlphaRange.fromJsonElement(json.get("alpha"));
        } else {
            alpha = AlphaRange.TRANSLUCENT;
        }
        
        FillMode fill;
        if (json.has("fill")) {
            JsonElement fillElem = json.get("fill");
            if (fillElem.isJsonPrimitive() && fillElem.getAsJsonPrimitive().isBoolean()) {
                fill = fillElem.getAsBoolean() ? FillMode.SOLID : FillMode.WIREFRAME;
            } else {
                fill = FillMode.fromId(fillElem.getAsString());
            }
        } else {
            fill = FillMode.SOLID;
        }
        
        PatternConfig pattern = json.has("pattern") 
            ? PatternConfig.fromJson(json.getAsJsonObject("pattern"))
            : PatternConfig.NONE;
        
        float glow = json.has("glow") ? json.get("glow").getAsFloat() : 0.0f;
        float wireThickness = json.has("wireThickness") ? json.get("wireThickness").getAsFloat() : 1.0f;
        
        Appearance result = new Appearance(color, alpha, fill, pattern, glow, wireThickness);
        
        Logging.RENDER.topic("appearance").trace(
            "Parsed Appearance: color={}, alpha={:.2f}-{:.2f}, fill={}, pattern={}",
            color, alpha.min(), alpha.max(), fill, pattern.type().id());
        
        return result;
    }
}
