package net.cyberpunk042.field.force.mode;

import com.google.gson.JsonObject;
import net.cyberpunk042.field.force.ForceAxis;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;

/**
 * Configuration for ORBIT force mode.
 * 
 * <p>Creates stable circular motion at fixed radii. Entities orbit around
 * the field center without being pulled in or pushed out, maintaining
 * their orbital distance.
 * 
 * <h2>Parameters</h2>
 * <ul>
 *   <li><b>ringCount</b>: Number of distinct orbit rings</li>
 *   <li><b>baseRadius</b>: Radius of innermost ring</li>
 *   <li><b>ringSpacing</b>: Distance between rings</li>
 *   <li><b>orbitSpeed</b>: Tangential velocity</li>
 *   <li><b>orbitAxis</b>: Axis of rotation</li>
 *   <li><b>clockwise</b>: Rotation direction</li>
 *   <li><b>stability</b>: How strongly orbit is maintained (0-1)</li>
 *   <li><b>entryForce</b>: Force pulling entities to nearest ring</li>
 * </ul>
 * 
 * @param ringCount Number of orbit rings
 * @param baseRadius Innermost ring radius
 * @param ringSpacing Distance between consecutive rings
 * @param orbitSpeed Tangential orbit speed
 * @param orbitAxis Axis of rotation
 * @param clockwise Rotation direction
 * @param stability How strongly entities are kept at orbit radius (0-1)
 * @param entryForce Force pulling entities toward nearest orbit lane
 * @param alternateDirection If true, alternate rings spin opposite directions
 */
public record OrbitModeConfig(
    @JsonField(skipIfDefault = true, defaultValue = "3") int ringCount,
    @JsonField(skipIfDefault = true, defaultValue = "5.0") float baseRadius,
    @JsonField(skipIfDefault = true, defaultValue = "3.0") float ringSpacing,
    @JsonField(skipIfDefault = true, defaultValue = "0.15") float orbitSpeed,
    @JsonField(skipIfDefault = true, defaultValue = "Y") ForceAxis orbitAxis,
    @JsonField(skipIfDefault = true, defaultValue = "true") boolean clockwise,
    @JsonField(skipIfDefault = true, defaultValue = "0.8") float stability,
    @JsonField(skipIfDefault = true, defaultValue = "0.12") float entryForce,
    @JsonField(skipIfDefault = true) boolean alternateDirection
) {
    
    /** Default orbit configuration. */
    public static final OrbitModeConfig DEFAULT = new OrbitModeConfig(
        3, 5f, 3f, 0.15f, ForceAxis.Y, true, 0.8f, 0.12f, false);
    
    /**
     * Compact constructor with validation.
     */
    public OrbitModeConfig {
        ringCount = Math.max(1, Math.min(10, ringCount));
        baseRadius = Math.max(1f, baseRadius);
        ringSpacing = Math.max(0.5f, ringSpacing);
        orbitSpeed = Math.max(0.01f, orbitSpeed);
        if (orbitAxis == null) orbitAxis = ForceAxis.Y;
        stability = Math.max(0f, Math.min(1f, stability));
        entryForce = Math.max(0f, entryForce);
    }
    
    /**
     * Gets the radius of a specific ring (0-indexed).
     */
    public float ringRadius(int ringIndex) {
        return baseRadius + (ringIndex * ringSpacing);
    }
    
    /**
     * Gets the maximum radius (outermost ring).
     */
    public float maxRadius() {
        return ringRadius(ringCount - 1) + ringSpacing; // Add buffer
    }
    
    /**
     * Finds the nearest ring to a given distance.
     */
    public int nearestRing(float distance) {
        int nearest = 0;
        float nearestDist = Math.abs(distance - baseRadius);
        
        for (int i = 1; i < ringCount; i++) {
            float ringDist = Math.abs(distance - ringRadius(i));
            if (ringDist < nearestDist) {
                nearestDist = ringDist;
                nearest = i;
            }
        }
        return nearest;
    }
    
    /**
     * Gets the signed orbit speed for a ring (accounts for alternating direction).
     */
    public float signedOrbitSpeed(int ringIndex) {
        boolean isClockwise = clockwise;
        if (alternateDirection && (ringIndex % 2 == 1)) {
            isClockwise = !isClockwise;
        }
        return isClockwise ? orbitSpeed : -orbitSpeed;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Builder
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int ringCount = 3;
        private float baseRadius = 5f;
        private float ringSpacing = 3f;
        private float orbitSpeed = 0.15f;
        private ForceAxis orbitAxis = ForceAxis.Y;
        private boolean clockwise = true;
        private float stability = 0.8f;
        private float entryForce = 0.12f;
        private boolean alternateDirection = false;
        
        public Builder ringCount(int v) { this.ringCount = v; return this; }
        public Builder baseRadius(float v) { this.baseRadius = v; return this; }
        public Builder ringSpacing(float v) { this.ringSpacing = v; return this; }
        public Builder orbitSpeed(float v) { this.orbitSpeed = v; return this; }
        public Builder orbitAxis(ForceAxis v) { this.orbitAxis = v; return this; }
        public Builder clockwise(boolean v) { this.clockwise = v; return this; }
        public Builder stability(float v) { this.stability = v; return this; }
        public Builder entryForce(float v) { this.entryForce = v; return this; }
        public Builder alternateDirection(boolean v) { this.alternateDirection = v; return this; }
        
        public OrbitModeConfig build() {
            return new OrbitModeConfig(ringCount, baseRadius, ringSpacing, orbitSpeed,
                orbitAxis, clockwise, stability, entryForce, alternateDirection);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static OrbitModeConfig fromJson(JsonObject json) {
        if (json == null) return DEFAULT;
        
        return new OrbitModeConfig(
            json.has("ringCount") ? json.get("ringCount").getAsInt() : 3,
            json.has("baseRadius") ? json.get("baseRadius").getAsFloat() : 5f,
            json.has("ringSpacing") ? json.get("ringSpacing").getAsFloat() : 3f,
            json.has("orbitSpeed") ? json.get("orbitSpeed").getAsFloat() : 0.15f,
            json.has("orbitAxis") ? ForceAxis.fromId(json.get("orbitAxis").getAsString()) : ForceAxis.Y,
            !json.has("clockwise") || json.get("clockwise").getAsBoolean(),
            json.has("stability") ? json.get("stability").getAsFloat() : 0.8f,
            json.has("entryForce") ? json.get("entryForce").getAsFloat() : 0.12f,
            json.has("alternateDirection") && json.get("alternateDirection").getAsBoolean()
        );
    }
    
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
}
