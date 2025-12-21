package net.cyberpunk042.field.force.mode;

import com.google.gson.JsonObject;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;

/**
 * Configuration for PULL force mode.
 * 
 * <p>Simple attraction toward the force field center with configurable
 * strength and falloff characteristics.
 * 
 * <h2>Parameters</h2>
 * <ul>
 *   <li><b>strength</b>: Base pull force (higher = stronger attraction)</li>
 *   <li><b>falloff</b>: How strength changes with distance</li>
 *   <li><b>radius</b>: Maximum effect range</li>
 *   <li><b>centerOffset</b>: Offset from field center (for asymmetric pulls)</li>
 *   <li><b>groundLift</b>: Small upward force to prevent ground dragging</li>
 * </ul>
 * 
 * @param strength Base pull strength (0.05-0.5 typical)
 * @param falloff Falloff type: "linear", "quadratic", "constant", "inverse", "smooth"
 * @param radius Maximum effect radius
 * @param centerOffsetY Vertical offset from geometric center
 * @param groundLift Upward force applied when entity is on ground
 */
public record PullModeConfig(
    @JsonField(skipIfDefault = true, defaultValue = "0.15") float strength,
    @JsonField(skipIfDefault = true, defaultValue = "linear") String falloff,
    @JsonField(skipIfDefault = true, defaultValue = "15.0") float radius,
    @JsonField(skipIfDefault = true) float centerOffsetY,
    @JsonField(skipIfDefault = true, defaultValue = "0.04") float groundLift
) {
    
    /** Default pull configuration. */
    public static final PullModeConfig DEFAULT = new PullModeConfig(
        0.15f, "linear", 15f, 0f, 0.04f);
    
    /**
     * Compact constructor with validation.
     */
    public PullModeConfig {
        strength = Math.max(0.01f, strength);
        radius = Math.max(1f, radius);
        if (falloff == null || falloff.isBlank()) falloff = "linear";
        groundLift = Math.max(0f, groundLift);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Builder
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private float strength = 0.15f;
        private String falloff = "linear";
        private float radius = 15f;
        private float centerOffsetY = 0f;
        private float groundLift = 0.04f;
        
        public Builder strength(float v) { this.strength = v; return this; }
        public Builder falloff(String v) { this.falloff = v; return this; }
        public Builder radius(float v) { this.radius = v; return this; }
        public Builder centerOffsetY(float v) { this.centerOffsetY = v; return this; }
        public Builder groundLift(float v) { this.groundLift = v; return this; }
        
        public PullModeConfig build() {
            return new PullModeConfig(strength, falloff, radius, centerOffsetY, groundLift);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static PullModeConfig fromJson(JsonObject json) {
        if (json == null) return DEFAULT;
        
        return new PullModeConfig(
            json.has("strength") ? json.get("strength").getAsFloat() : 0.15f,
            json.has("falloff") ? json.get("falloff").getAsString() : "linear",
            json.has("radius") ? json.get("radius").getAsFloat() : 15f,
            json.has("centerOffsetY") ? json.get("centerOffsetY").getAsFloat() : 0f,
            json.has("groundLift") ? json.get("groundLift").getAsFloat() : 0.04f
        );
    }
    
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
}
