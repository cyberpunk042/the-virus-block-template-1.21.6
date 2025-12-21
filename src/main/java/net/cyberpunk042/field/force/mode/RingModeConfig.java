package net.cyberpunk042.field.force.mode;

import com.google.gson.JsonObject;
import net.cyberpunk042.field.force.ForceAxis;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;

/**
 * Configuration for RING force mode.
 * 
 * <p>Creates a stable orbit band - entities inside the ring are pushed out,
 * entities outside are pulled in, creating a stable "orbit lane" at a
 * specific radius.
 * 
 * <h2>Parameters</h2>
 * <ul>
 *   <li><b>ringRadius</b>: Center of the stable orbit band</li>
 *   <li><b>ringWidth</b>: Width of the stable zone</li>
 *   <li><b>innerPush</b>: Outward force inside the ring</li>
 *   <li><b>outerPull</b>: Inward force outside the ring</li>
 *   <li><b>orbitSpeed</b>: Tangential velocity within the ring</li>
 *   <li><b>orbitAxis</b>: Axis of rotation</li>
 * </ul>
 * 
 * @param ringRadius Radius of the stable orbit band center
 * @param ringWidth Width of the stable zone (entities within are not pushed/pulled)
 * @param innerPush Outward force for entities too close to center
 * @param outerPull Inward force for entities too far from center
 * @param orbitSpeed Tangential velocity within the ring band
 * @param orbitAxis Axis of rotation
 * @param clockwise Rotation direction
 * @param transitionSmooth Smooth transition at ring edges vs sharp
 */
public record RingModeConfig(
    @JsonField(skipIfDefault = true, defaultValue = "8.0") float ringRadius,
    @JsonField(skipIfDefault = true, defaultValue = "2.0") float ringWidth,
    @JsonField(skipIfDefault = true, defaultValue = "0.15") float innerPush,
    @JsonField(skipIfDefault = true, defaultValue = "0.12") float outerPull,
    @JsonField(skipIfDefault = true, defaultValue = "0.1") float orbitSpeed,
    @JsonField(skipIfDefault = true, defaultValue = "Y") ForceAxis orbitAxis,
    @JsonField(skipIfDefault = true, defaultValue = "true") boolean clockwise,
    @JsonField(skipIfDefault = true, defaultValue = "true") boolean transitionSmooth
) {
    
    /** Default ring configuration. */
    public static final RingModeConfig DEFAULT = new RingModeConfig(
        8f, 2f, 0.15f, 0.12f, 0.1f, ForceAxis.Y, true, true);
    
    /**
     * Compact constructor with validation.
     */
    public RingModeConfig {
        ringRadius = Math.max(1f, ringRadius);
        ringWidth = Math.max(0.1f, ringWidth);
        innerPush = Math.max(0f, innerPush);
        outerPull = Math.max(0f, outerPull);
        orbitSpeed = Math.max(0f, orbitSpeed);
        if (orbitAxis == null) orbitAxis = ForceAxis.Y;
    }
    
    /**
     * Inner edge of the stable ring.
     */
    public float innerRadius() {
        return Math.max(0f, ringRadius - ringWidth / 2);
    }
    
    /**
     * Outer edge of the stable ring.
     */
    public float outerRadius() {
        return ringRadius + ringWidth / 2;
    }
    
    /**
     * Maximum effect radius (includes pull zone).
     */
    public float maxRadius() {
        return ringRadius * 2; // Generous pull range
    }
    
    /**
     * Checks if a distance is within the stable band.
     */
    public boolean isInStableZone(float distance) {
        return distance >= innerRadius() && distance <= outerRadius();
    }
    
    /**
     * Gets the radial force at a given distance.
     * Negative = push outward, Positive = pull inward.
     */
    public float radialForceAt(float distance) {
        if (isInStableZone(distance)) {
            return 0f; // Stable zone
        }
        
        if (distance < innerRadius()) {
            // Inside ring - push outward
            float depth = innerRadius() - distance;
            float normalized = transitionSmooth 
                ? smoothstep(depth / innerRadius())
                : depth / innerRadius();
            return -innerPush * normalized;
        } else {
            // Outside ring - pull inward
            float excess = distance - outerRadius();
            float range = maxRadius() - outerRadius();
            float normalized = transitionSmooth
                ? smoothstep(1f - Math.min(1f, excess / range))
                : 1f - Math.min(1f, excess / range);
            return outerPull * normalized;
        }
    }
    
    /**
     * Returns signed orbit speed based on direction.
     */
    public float signedOrbitSpeed() {
        return clockwise ? orbitSpeed : -orbitSpeed;
    }
    
    private static float smoothstep(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3 - 2 * t);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Builder
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private float ringRadius = 8f;
        private float ringWidth = 2f;
        private float innerPush = 0.15f;
        private float outerPull = 0.12f;
        private float orbitSpeed = 0.1f;
        private ForceAxis orbitAxis = ForceAxis.Y;
        private boolean clockwise = true;
        private boolean transitionSmooth = true;
        
        public Builder ringRadius(float v) { this.ringRadius = v; return this; }
        public Builder ringWidth(float v) { this.ringWidth = v; return this; }
        public Builder innerPush(float v) { this.innerPush = v; return this; }
        public Builder outerPull(float v) { this.outerPull = v; return this; }
        public Builder orbitSpeed(float v) { this.orbitSpeed = v; return this; }
        public Builder orbitAxis(ForceAxis v) { this.orbitAxis = v; return this; }
        public Builder clockwise(boolean v) { this.clockwise = v; return this; }
        public Builder transitionSmooth(boolean v) { this.transitionSmooth = v; return this; }
        
        public RingModeConfig build() {
            return new RingModeConfig(ringRadius, ringWidth, innerPush, outerPull,
                orbitSpeed, orbitAxis, clockwise, transitionSmooth);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static RingModeConfig fromJson(JsonObject json) {
        if (json == null) return DEFAULT;
        
        return new RingModeConfig(
            json.has("ringRadius") ? json.get("ringRadius").getAsFloat() : 8f,
            json.has("ringWidth") ? json.get("ringWidth").getAsFloat() : 2f,
            json.has("innerPush") ? json.get("innerPush").getAsFloat() : 0.15f,
            json.has("outerPull") ? json.get("outerPull").getAsFloat() : 0.12f,
            json.has("orbitSpeed") ? json.get("orbitSpeed").getAsFloat() : 0.1f,
            json.has("orbitAxis") ? ForceAxis.fromId(json.get("orbitAxis").getAsString()) : ForceAxis.Y,
            !json.has("clockwise") || json.get("clockwise").getAsBoolean(),
            !json.has("transitionSmooth") || json.get("transitionSmooth").getAsBoolean()
        );
    }
    
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
}
