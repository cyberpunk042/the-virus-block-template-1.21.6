package net.cyberpunk042.field.force.mode;

import com.google.gson.JsonObject;
import net.cyberpunk042.field.force.ForceAxis;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;

/**
 * Configuration for TORNADO force mode.
 * 
 * <p>Combines vertical lift with horizontal rotation, creating an
 * upward-spiraling motion like a tornado or dust devil.
 * 
 * <h2>Parameters</h2>
 * <ul>
 *   <li><b>liftSpeed</b>: Upward velocity</li>
 *   <li><b>spinSpeed</b>: Horizontal rotation speed</li>
 *   <li><b>spinAxis</b>: Vertical axis (typically Y)</li>
 *   <li><b>funnelFactor</b>: How much radius increases with height</li>
 *   <li><b>baseRadius</b>: Radius at ground level</li>
 *   <li><b>height</b>: Maximum effect height</li>
 *   <li><b>suckRadius</b>: Horizontal pull range at ground level</li>
 * </ul>
 * 
 * @param liftSpeed Vertical lift force
 * @param spinSpeed Horizontal rotation speed
 * @param spinAxis Axis of rotation (typically Y)
 * @param clockwise Spin direction
 * @param funnelFactor Radius expansion with height (0 = cylinder, 1 = cone)
 * @param baseRadius Tornado radius at base
 * @param height Maximum effect height
 * @param suckRadius Ground-level horizontal suction range
 * @param groundPull Force pulling entities toward tornado base
 */
public record TornadoModeConfig(
    @JsonField(skipIfDefault = true, defaultValue = "0.15") float liftSpeed,
    @JsonField(skipIfDefault = true, defaultValue = "0.2") float spinSpeed,
    @JsonField(skipIfDefault = true, defaultValue = "Y") ForceAxis spinAxis,
    @JsonField(skipIfDefault = true, defaultValue = "true") boolean clockwise,
    @JsonField(skipIfDefault = true, defaultValue = "0.3") float funnelFactor,
    @JsonField(skipIfDefault = true, defaultValue = "3.0") float baseRadius,
    @JsonField(skipIfDefault = true, defaultValue = "20.0") float height,
    @JsonField(skipIfDefault = true, defaultValue = "10.0") float suckRadius,
    @JsonField(skipIfDefault = true, defaultValue = "0.1") float groundPull
) {
    
    /** Default tornado configuration. */
    public static final TornadoModeConfig DEFAULT = new TornadoModeConfig(
        0.15f, 0.2f, ForceAxis.Y, true, 0.3f, 3f, 20f, 10f, 0.1f);
    
    /**
     * Compact constructor with validation.
     */
    public TornadoModeConfig {
        liftSpeed = Math.max(0.01f, liftSpeed);
        spinSpeed = Math.max(0.01f, spinSpeed);
        if (spinAxis == null) spinAxis = ForceAxis.Y;
        funnelFactor = Math.max(0f, Math.min(2f, funnelFactor));
        baseRadius = Math.max(0.5f, baseRadius);
        height = Math.max(1f, height);
        suckRadius = Math.max(baseRadius, suckRadius);
        groundPull = Math.max(0f, groundPull);
    }
    
    /**
     * Gets the tornado radius at a given height.
     */
    public float radiusAtHeight(float y) {
        if (y < 0) return suckRadius;
        float heightFactor = Math.min(1f, y / height);
        return baseRadius + (baseRadius * funnelFactor * heightFactor);
    }
    
    /**
     * Returns signed spin speed based on direction.
     */
    public float signedSpinSpeed() {
        return clockwise ? spinSpeed : -spinSpeed;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Builder
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private float liftSpeed = 0.15f;
        private float spinSpeed = 0.2f;
        private ForceAxis spinAxis = ForceAxis.Y;
        private boolean clockwise = true;
        private float funnelFactor = 0.3f;
        private float baseRadius = 3f;
        private float height = 20f;
        private float suckRadius = 10f;
        private float groundPull = 0.1f;
        
        public Builder liftSpeed(float v) { this.liftSpeed = v; return this; }
        public Builder spinSpeed(float v) { this.spinSpeed = v; return this; }
        public Builder spinAxis(ForceAxis v) { this.spinAxis = v; return this; }
        public Builder clockwise(boolean v) { this.clockwise = v; return this; }
        public Builder funnelFactor(float v) { this.funnelFactor = v; return this; }
        public Builder baseRadius(float v) { this.baseRadius = v; return this; }
        public Builder height(float v) { this.height = v; return this; }
        public Builder suckRadius(float v) { this.suckRadius = v; return this; }
        public Builder groundPull(float v) { this.groundPull = v; return this; }
        
        public TornadoModeConfig build() {
            return new TornadoModeConfig(liftSpeed, spinSpeed, spinAxis, clockwise,
                funnelFactor, baseRadius, height, suckRadius, groundPull);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static TornadoModeConfig fromJson(JsonObject json) {
        if (json == null) return DEFAULT;
        
        return new TornadoModeConfig(
            json.has("liftSpeed") ? json.get("liftSpeed").getAsFloat() : 0.15f,
            json.has("spinSpeed") ? json.get("spinSpeed").getAsFloat() : 0.2f,
            json.has("spinAxis") ? ForceAxis.fromId(json.get("spinAxis").getAsString()) : ForceAxis.Y,
            !json.has("clockwise") || json.get("clockwise").getAsBoolean(),
            json.has("funnelFactor") ? json.get("funnelFactor").getAsFloat() : 0.3f,
            json.has("baseRadius") ? json.get("baseRadius").getAsFloat() : 3f,
            json.has("height") ? json.get("height").getAsFloat() : 20f,
            json.has("suckRadius") ? json.get("suckRadius").getAsFloat() : 10f,
            json.has("groundPull") ? json.get("groundPull").getAsFloat() : 0.1f
        );
    }
    
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
}
