package net.cyberpunk042.visual.appearance;

import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;

/**
 * Represents a range of alpha (opacity) values for pulsing effects.
 * 
 * <h2>Usage</h2>
 * <ul>
 *   <li>Static alpha: min == max (no pulsing)</li>
 *   <li>Pulsing alpha: min < max (oscillates between values)</li>
 * </ul>
 * 
 * <h2>Examples</h2>
 * <pre>
 * AlphaRange.OPAQUE          // Fully opaque, no pulse
 * AlphaRange.TRANSLUCENT     // 0.4 to 0.8 range
 * AlphaRange.of(0.2f, 0.6f)  // Custom range
 * </pre>
 * 
 * @see Appearance
 * @see net.cyberpunk042.visual.animation.Animation#getAlphaMultiplier(float)
 */
public record AlphaRange(float min, float max) {
    
    // =========================================================================
    // Presets
    // =========================================================================
    
    /** Fully opaque, no pulsing. */
    public static final AlphaRange OPAQUE = new AlphaRange(1.0f, 1.0f);
    
    /** Standard translucent range (0.4 - 0.8). */
    public static final AlphaRange TRANSLUCENT = new AlphaRange(0.4f, 0.8f);
    
    /** Subtle/faint visibility (0.1 - 0.3). */
    public static final AlphaRange FAINT = new AlphaRange(0.1f, 0.3f);
    
    /** Ghost-like appearance (0.05 - 0.15). */
    public static final AlphaRange GHOST = new AlphaRange(0.05f, 0.15f);
    
    /** Strong presence (0.6 - 0.95). */
    public static final AlphaRange STRONG = new AlphaRange(0.6f, 0.95f);
    
    // =========================================================================
    // Compact Constructor (Validation)
    // =========================================================================
    
    public AlphaRange {
        // Clamp values to valid range
        min = Math.max(0.0f, Math.min(1.0f, min));
        max = Math.max(0.0f, Math.min(1.0f, max));
        
        // Ensure min <= max
        if (min > max) {
            float temp = min;
            min = max;
            max = temp;
        }
    }
    
    // =========================================================================
    // Factory Methods
    // =========================================================================
    
    /**
     * Creates an alpha range with the given bounds.
     */
    public static AlphaRange of(float min, float max) {
        return new AlphaRange(min, max);
    }
    
    /**
     * Creates a static (non-pulsing) alpha value.
     */
    public static AlphaRange fixed(float alpha) {
        return new AlphaRange(alpha, alpha);
    }
    
    // =========================================================================
    // Computed Properties
    // =========================================================================
    
    /**
     * Interpolates within the range.
     * @param t Interpolation factor (0.0 = min, 1.0 = max)
     * @return Alpha value at position t
     */
    public float at(float t) {
        float clamped = Math.max(0.0f, Math.min(1.0f, t));
        return min + (max - min) * clamped;
    }
    
    /**
     * Returns the range span (max - min).
     */
    public float range() {
        return max - min;
    }
    
    /**
     * Returns the midpoint of the range.
     */
    public float midpoint() {
        return (min + max) / 2.0f;
    }
    
    /**
     * Checks if this range pulses (min != max).
     */
    public boolean isPulsing() {
        return Math.abs(max - min) > 0.001f;
    }
    
    /**
     * Checks if this is fully opaque.
     */
    public boolean isOpaque() {
        return min >= 0.999f && max >= 0.999f;
    }
    
    // =========================================================================
    // Builder-style
    // =========================================================================
    
    public AlphaRange withMin(float newMin) {
        return new AlphaRange(newMin, max);
    }
    
    public AlphaRange withMax(float newMax) {
        return new AlphaRange(min, newMax);
    }
    
    /**
     * Scales the range by a multiplier.
     */
    public AlphaRange scaled(float multiplier) {
        return new AlphaRange(min * multiplier, max * multiplier);
    }
    
    // =========================================================================
    // JSON Serialization
    // =========================================================================
    
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("min", min);
        json.addProperty("max", max);
        return json;
    }
    
    public static AlphaRange fromJson(JsonObject json) {
        if (json == null) {
            return TRANSLUCENT;
        }
        
        float min = json.has("min") ? json.get("min").getAsFloat() : 0.5f;
        float max = json.has("max") ? json.get("max").getAsFloat() : min;
        
        AlphaRange result = new AlphaRange(min, max);
        
        Logging.RENDER.topic("alpha").trace(
            "Parsed AlphaRange: {:.2f} - {:.2f} (pulsing={})",
            result.min(), result.max(), result.isPulsing());
        
        return result;
    }
    
    /**
     * Parses from either a single float or an object with min/max.
     */
    public static AlphaRange fromJsonElement(com.google.gson.JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return TRANSLUCENT;
        }
        if (element.isJsonPrimitive()) {
            float value = element.getAsFloat();
            return fixed(value);
        }
        if (element.isJsonObject()) {
            return fromJson(element.getAsJsonObject());
        }
        return TRANSLUCENT;
    }
}
