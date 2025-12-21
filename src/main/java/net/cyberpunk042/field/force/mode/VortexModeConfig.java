package net.cyberpunk042.field.force.mode;

import com.google.gson.JsonObject;
import net.cyberpunk042.field.force.ForceAxis;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;

/**
 * Configuration for VORTEX force mode.
 * 
 * <p>Creates a spiral inward motion by combining radial pull with tangential
 * rotation. Entities are pulled toward the center while simultaneously
 * spinning around it.
 * 
 * <h2>Parameters</h2>
 * <ul>
 *   <li><b>radialStrength</b>: Inward pull force</li>
 *   <li><b>tangentialStrength</b>: Rotational force</li>
 *   <li><b>spinAxis</b>: Axis of rotation (X, Y, Z)</li>
 *   <li><b>clockwise</b>: Rotation direction</li>
 *   <li><b>tightness</b>: How fast the spiral contracts</li>
 *   <li><b>radius</b>: Maximum effect range</li>
 * </ul>
 * 
 * @param radialStrength Inward pull strength
 * @param tangentialStrength Rotation strength
 * @param spinAxis Axis of rotation
 * @param clockwise True for clockwise, false for counter-clockwise
 * @param tightness Spiral contraction rate (0 = pure orbit, 1 = tight spiral)
 * @param radius Maximum effect radius
 * @param falloff Falloff type for both radial and tangential
 */
public record VortexModeConfig(
    @JsonField(skipIfDefault = true, defaultValue = "0.1") float radialStrength,
    @JsonField(skipIfDefault = true, defaultValue = "0.15") float tangentialStrength,
    @JsonField(skipIfDefault = true, defaultValue = "Y") ForceAxis spinAxis,
    @JsonField(skipIfDefault = true, defaultValue = "true") boolean clockwise,
    @JsonField(skipIfDefault = true, defaultValue = "0.5") float tightness,
    @JsonField(skipIfDefault = true, defaultValue = "15.0") float radius,
    @JsonField(skipIfDefault = true, defaultValue = "quadratic") String falloff
) {
    
    /** Default vortex configuration. */
    public static final VortexModeConfig DEFAULT = new VortexModeConfig(
        0.1f, 0.15f, ForceAxis.Y, true, 0.5f, 15f, "quadratic");
    
    /**
     * Compact constructor with validation.
     */
    public VortexModeConfig {
        radialStrength = Math.max(0f, radialStrength);
        tangentialStrength = Math.max(0.01f, tangentialStrength);
        if (spinAxis == null) spinAxis = ForceAxis.Y;
        tightness = Math.max(0f, Math.min(1f, tightness));
        radius = Math.max(1f, radius);
        if (falloff == null || falloff.isBlank()) falloff = "quadratic";
    }
    
    /**
     * Effective radial strength based on tightness.
     * Higher tightness = stronger radial pull.
     */
    public float effectiveRadialStrength() {
        return radialStrength * tightness;
    }
    
    /**
     * Returns tangential strength with direction sign.
     */
    public float signedTangentialStrength() {
        return clockwise ? tangentialStrength : -tangentialStrength;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Builder
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private float radialStrength = 0.1f;
        private float tangentialStrength = 0.15f;
        private ForceAxis spinAxis = ForceAxis.Y;
        private boolean clockwise = true;
        private float tightness = 0.5f;
        private float radius = 15f;
        private String falloff = "quadratic";
        
        public Builder radialStrength(float v) { this.radialStrength = v; return this; }
        public Builder tangentialStrength(float v) { this.tangentialStrength = v; return this; }
        public Builder spinAxis(ForceAxis v) { this.spinAxis = v; return this; }
        public Builder clockwise(boolean v) { this.clockwise = v; return this; }
        public Builder tightness(float v) { this.tightness = v; return this; }
        public Builder radius(float v) { this.radius = v; return this; }
        public Builder falloff(String v) { this.falloff = v; return this; }
        
        public VortexModeConfig build() {
            return new VortexModeConfig(radialStrength, tangentialStrength, spinAxis, 
                clockwise, tightness, radius, falloff);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static VortexModeConfig fromJson(JsonObject json) {
        if (json == null) return DEFAULT;
        
        return new VortexModeConfig(
            json.has("radialStrength") ? json.get("radialStrength").getAsFloat() : 0.1f,
            json.has("tangentialStrength") ? json.get("tangentialStrength").getAsFloat() : 0.15f,
            json.has("spinAxis") ? ForceAxis.fromId(json.get("spinAxis").getAsString()) : ForceAxis.Y,
            !json.has("clockwise") || json.get("clockwise").getAsBoolean(),
            json.has("tightness") ? json.get("tightness").getAsFloat() : 0.5f,
            json.has("radius") ? json.get("radius").getAsFloat() : 15f,
            json.has("falloff") ? json.get("falloff").getAsString() : "quadratic"
        );
    }
    
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
}
