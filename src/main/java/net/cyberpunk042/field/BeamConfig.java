package net.cyberpunk042.field;

import com.google.gson.JsonObject;
import net.cyberpunk042.visual.color.ColorMath;

/**
 * Configuration for a beacon-style beam that extends upward from a field.
 * 
 * <p>Used primarily for anti-virus shields and similar effects to mark
 * protected areas with a visible light column.
 * 
 * <h2>Properties</h2>
 * <ul>
 *   <li><b>enabled</b>: Whether the beam should render</li>
 *   <li><b>innerRadius</b>: Radius of the inner bright beam</li>
 *   <li><b>outerRadius</b>: Radius of the outer glow</li>
 *   <li><b>color</b>: ARGB color of the beam</li>
 * </ul>
 * 
 * @see FieldDefinition
 */
public record BeamConfig(
    boolean enabled,
    float innerRadius,
    float outerRadius,
    int color
) {
    
    /** Default disabled beam config */
    public static final BeamConfig DISABLED = new BeamConfig(false, 0.0f, 0.0f, 0xFFFFFFFF);
    
    /** Default enabled beam config (white, medium size) */
    public static final BeamConfig DEFAULT = new BeamConfig(true, 0.04f, 0.06f, 0xFFFFFFFF);
    
    /**
     * Creates a beam config with custom color.
     */
    public static BeamConfig colored(int color) {
        return new BeamConfig(true, 0.04f, 0.06f, color);
    }
    
    /**
     * Creates a beam config with custom size and color.
     */
    public static BeamConfig custom(float innerRadius, float outerRadius, int color) {
        return new BeamConfig(true, innerRadius, outerRadius, color);
    }
    
    /**
     * Parses beam config from JSON.
     * 
     * <p>Expected format:
     * <pre>
     * {
     *   "enabled": true,
     *   "innerRadius": 0.04,
     *   "outerRadius": 0.06,
     *   "color": "#FFFFFFFF"
     * }
     * </pre>
     */
    public static BeamConfig fromJson(JsonObject json) {
        if (json == null) {
            return DISABLED;
        }
        
        boolean enabled = json.has("enabled") && json.get("enabled").getAsBoolean();
        if (!enabled) {
            return DISABLED;
        }
        
        float innerRadius = json.has("innerRadius") 
            ? json.get("innerRadius").getAsFloat() 
            : json.has("inner_radius") ? json.get("inner_radius").getAsFloat() : 0.04f;
        
        float outerRadius = json.has("outerRadius") 
            ? json.get("outerRadius").getAsFloat() 
            : json.has("outer_radius") ? json.get("outer_radius").getAsFloat() : 0.06f;
        
        int color = 0xFFFFFFFF;
        if (json.has("color")) {
            String colorStr = json.get("color").getAsString();
            color = ColorMath.parseHex(colorStr);
        }
        
        return new BeamConfig(true, innerRadius, outerRadius, color);
    }
    
    /**
     * Serializes this config to JSON.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);
        if (enabled) {
            json.addProperty("innerRadius", innerRadius);
            json.addProperty("outerRadius", outerRadius);
            json.addProperty("color", String.format("#%08X", color));
        }
        return json;
    }
    
    /**
     * Returns the color with inner alpha applied (brighter).
     */
    public int innerColor() {
        // Inner beam is 40% alpha version of the main color
        int a = 0x64; // ~40% alpha
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
