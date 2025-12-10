package net.cyberpunk042.visual.appearance;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;

/**
 * Visual appearance configuration for a primitive.
 * 
 * <p>Appearance controls colors, transparency, and effects.
 * Animation is handled separately by Animation record.</p>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "appearance": {
 *   "color": "@primary",
 *   "alpha": { "min": 0.6, "max": 0.8 },
 *   "glow": 0.5,
 *   "emissive": 0.0,
 *   "saturation": 1.0,
 *   "brightness": 1.0,
 *   "hueShift": 0.0,
 *   "secondaryColor": "@secondary",
 *   "colorBlend": 0.0
 * }
 * </pre>
 * 
 * @see AlphaRange
 * @see net.cyberpunk042.visual.animation.Animation
 */
public record Appearance(
    String color,
    AlphaRange alpha,
    @Range(ValueRange.ALPHA) float glow,
    @Range(ValueRange.ALPHA) float emissive,
    @Range(ValueRange.ALPHA) float saturation,
    @Range(ValueRange.ALPHA) float brightness,
    @Range(ValueRange.DEGREES) float hueShift,
    @Nullable String secondaryColor,
    @Range(ValueRange.ALPHA) float colorBlend
) {
    /** Default appearance. */
    public static Appearance defaults() { return DEFAULT; }
    
    public static Appearance wireframe(String color) { 
        return builder().color(color).build();
    }
    
    public static Appearance translucent(String color, float alpha) { 
        return builder().color(color).alpha(AlphaRange.of(alpha)).build();
    }
    
    public static final Appearance DEFAULT = new Appearance(
        "@primary", AlphaRange.DEFAULT, 0, 0, 1, 1, 0, null, 0);
    
    /** Glowing appearance. */
    public static final Appearance GLOWING = new Appearance(
        "@primary", AlphaRange.DEFAULT, 0.8f, 0.5f, 1, 1, 0, null, 0);
    
    /** Translucent appearance. */
    public static final Appearance TRANSLUCENT = new Appearance(
        "@primary", AlphaRange.of(0.3f), 0, 0, 1, 1, 0, null, 0);
    
    /**
     * Creates a simple solid-color appearance.
     * @param color Color reference (e.g., "@primary", "#FF0000")
     */
    public static Appearance color(String color) {
        return new Appearance(color, AlphaRange.OPAQUE, 0, 0, 1, 1, 0, null, 0);
    }
    
    /**
     * Creates an appearance with glow.
     * @param color Color reference
     * @param glow Glow intensity (0-1)
     */
    public static Appearance glowing(String color, @Range(ValueRange.ALPHA) float glow) {
        return new Appearance(color, AlphaRange.DEFAULT, glow, glow * 0.5f, 1, 1, 0, null, 0);
    }
    
    /** Whether glow is active. */
    public boolean hasGlow() { return glow > 0; }
    
    /** Whether emissive is active. */
    public boolean hasEmissive() { return emissive > 0; }
    
    /** Whether secondary color is used. */
    public boolean hasSecondaryColor() { return secondaryColor != null && colorBlend > 0; }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    // =========================================================================
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
        // Note: PatternConfig is handled separately via ArrangementConfig
        
        return builder.build();
    }

    public static Builder builder() { return new Builder(); }
    /**
     * Serializes this appearance to JSON.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (color != null) json.addProperty("color", color);
        if (secondaryColor != null) json.addProperty("secondaryColor", secondaryColor);
        if (alpha != null) json.add("alpha", alpha.toJson());
        if (glow != 0) json.addProperty("glow", glow);
        return json;
    }


    
    public static class Builder {
        private String color = "@primary";
        private AlphaRange alpha = AlphaRange.DEFAULT;
        private @Range(ValueRange.ALPHA) float glow = 0;
        private @Range(ValueRange.ALPHA) float emissive = 0;
        private @Range(ValueRange.ALPHA) float saturation = 1;
        private @Range(ValueRange.ALPHA) float brightness = 1;
        private @Range(ValueRange.DEGREES) float hueShift = 0;
        private String secondaryColor = null;
        private @Range(ValueRange.ALPHA) float colorBlend = 0;
        
        public Builder color(String c) { this.color = c; return this; }
        public Builder alpha(AlphaRange a) { this.alpha = a; return this; }
        public Builder alpha(float a) { this.alpha = AlphaRange.of(a); return this; }
        public Builder glow(float g) { this.glow = g; return this; }
        public Builder emissive(float e) { this.emissive = e; return this; }
        public Builder saturation(float s) { this.saturation = s; return this; }
        public Builder brightness(float b) { this.brightness = b; return this; }
        public Builder hueShift(float h) { this.hueShift = h; return this; }
        public Builder secondaryColor(String c) { this.secondaryColor = c; return this; }
        public Builder colorBlend(float b) { this.colorBlend = b; return this; }
        
        public Appearance build() {
            return new Appearance(color, alpha, glow, emissive, saturation, brightness, 
                hueShift, secondaryColor, colorBlend);
        }
    }

    // =========================================================================
    // Immutable Modifiers
    // =========================================================================
    
    /**
     * Returns a copy with the specified alpha range.
     */
    public Appearance withAlpha(AlphaRange newAlpha) {
        return new Appearance(color, newAlpha, glow, emissive, saturation, brightness, hueShift, secondaryColor, colorBlend);
    }
    
    /**
     * Returns a copy with the specified color.
     */
    public Appearance withColor(String newColor) {
        return new Appearance(newColor, alpha, glow, emissive, saturation, brightness, hueShift, secondaryColor, colorBlend);
    }

}
