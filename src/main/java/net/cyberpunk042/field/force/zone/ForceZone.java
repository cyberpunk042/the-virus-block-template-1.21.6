package net.cyberpunk042.field.force.zone;

import com.google.gson.JsonObject;
import net.cyberpunk042.field.force.ForceAxis;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;

/**
 * Represents a force zone within a force field (CUSTOM mode).
 * 
 * <p>A zone defines a radius-based region with configurable force components
 * for radial, tangential, and lift forces. Force fields can have multiple
 * nested zones with different behaviors.
 * 
 * <h2>Force Components</h2>
 * <ul>
 *   <li><b>Radial</b>: Toward/away from center (+ = pull, - = push)</li>
 *   <li><b>Tangential</b>: Perpendicular rotation (+ = clockwise)</li>
 *   <li><b>Lift</b>: Vertical force component</li>
 * </ul>
 * 
 * <h2>Falloff Types</h2>
 * <ul>
 *   <li><b>linear</b>: Force decreases linearly from center to edge</li>
 *   <li><b>quadratic</b>: Force decreases quadratically (faster at edges)</li>
 *   <li><b>constant</b>: Uniform force throughout the zone</li>
 *   <li><b>inverse</b>: Force increases toward edges</li>
 *   <li><b>smooth</b>: Smooth step falloff</li>
 * </ul>
 * 
 * @param radius Maximum radius of this zone (outer edge)
 * @param innerRadius Inner radius (for ring zones, 0 = from center)
 * @param radialStrength Radial force (+ = pull toward center, - = push away)
 * @param tangentialStrength Tangential force (+ = clockwise rotation)
 * @param liftStrength Vertical force (+ = up, - = down)
 * @param falloff Falloff type for all force components
 * @param tangentAxis Axis for tangential rotation
 */
public record ForceZone(
    float radius,
    @JsonField(skipIfDefault = true) float innerRadius,
    @JsonField(name = "strength") float radialStrength,
    @JsonField(skipIfDefault = true) float tangentialStrength,
    @JsonField(skipIfDefault = true) float liftStrength,
    @JsonField(skipIfDefault = true, defaultValue = "linear") String falloff,
    @JsonField(skipIfDefault = true, defaultValue = "Y") ForceAxis tangentAxis
) {
    
    /** Default falloff type. */
    public static final String DEFAULT_FALLOFF = "linear";
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Factory Methods
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a simple radial zone (backward compatible signature).
     */
    public static ForceZone linear(float radius, float strength) {
        return new ForceZone(radius, 0f, strength, 0f, 0f, "linear", ForceAxis.Y);
    }
    
    /**
     * Creates a quadratic falloff zone.
     */
    public static ForceZone quadratic(float radius, float strength) {
        return new ForceZone(radius, 0f, strength, 0f, 0f, "quadratic", ForceAxis.Y);
    }
    
    /**
     * Creates a constant strength zone (no falloff).
     */
    public static ForceZone constant(float radius, float strength) {
        return new ForceZone(radius, 0f, strength, 0f, 0f, "constant", ForceAxis.Y);
    }
    
    /**
     * Creates an inverse falloff zone (stronger at edges).
     */
    public static ForceZone inverse(float radius, float strength) {
        return new ForceZone(radius, 0f, strength, 0f, 0f, "inverse", ForceAxis.Y);
    }
    
    /**
     * Creates a vortex zone with both radial and tangential forces.
     */
    public static ForceZone vortex(float radius, float radial, float tangent) {
        return new ForceZone(radius, 0f, radial, tangent, 0f, "quadratic", ForceAxis.Y);
    }
    
    /**
     * Creates a ring zone (inner radius to outer radius).
     */
    public static ForceZone ring(float innerRadius, float outerRadius, float tangent) {
        return new ForceZone(outerRadius, innerRadius, 0f, tangent, 0f, "constant", ForceAxis.Y);
    }
    
    /**
     * Creates a repulsion zone (pushes outward).
     */
    public static ForceZone repulsor(float radius, float strength) {
        return new ForceZone(radius, 0f, -strength, 0f, 0f, "linear", ForceAxis.Y);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Compact Constructor
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Compact constructor with validation.
     */
    public ForceZone {
        radius = Math.max(0.1f, radius);
        innerRadius = Math.max(0f, Math.min(radius - 0.1f, innerRadius));
        if (falloff == null || falloff.isBlank()) falloff = DEFAULT_FALLOFF;
        if (tangentAxis == null) tangentAxis = ForceAxis.Y;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Legacy Compatibility
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Legacy alias for radialStrength.
     */
    public float strength() {
        return radialStrength;
    }
    
    /**
     * Alias for falloff() for GUI compatibility.
     */
    public String falloffName() {
        return falloff;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Distance Checks
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Checks if the given distance is within this zone.
     */
    public boolean containsDistance(float distance) {
        return distance >= innerRadius && distance <= radius;
    }
    
    /**
     * Whether this is a ring zone (has inner radius).
     */
    public boolean isRing() {
        return innerRadius > 0;
    }
    
    /**
     * Whether this zone has tangential forces.
     */
    public boolean hasTangential() {
        return Math.abs(tangentialStrength) > 0.001f;
    }
    
    /**
     * Whether this zone has lift forces.
     */
    public boolean hasLift() {
        return Math.abs(liftStrength) > 0.001f;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Force Calculations
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Calculates the falloff multiplier at a given distance.
     */
    public float falloffAt(float distance) {
        if (!containsDistance(distance)) {
            return 0f;
        }
        
        // Normalize: 0 at inner edge, 1 at outer edge
        float range = radius - innerRadius;
        if (range <= 0) return 1f;
        
        float normalizedDist = (distance - innerRadius) / range;
        
        return switch (falloff.toLowerCase()) {
            case "constant" -> 1f;
            case "linear" -> 1f - normalizedDist; // Stronger at inner
            case "quadratic" -> 1f - normalizedDist * normalizedDist;
            case "inverse" -> normalizedDist; // Stronger at outer
            case "smooth" -> smoothstep(1f - normalizedDist);
            case "centered" -> { // Strongest at middle of zone
                float mid = 0.5f;
                float dist = Math.abs(normalizedDist - mid) * 2f;
                yield 1f - dist;
            }
            default -> 1f - normalizedDist;
        };
    }
    
    /**
     * Calculates radial force strength at a given distance.
     */
    public float radialStrengthAt(float distance) {
        return radialStrength * falloffAt(distance);
    }
    
    /**
     * Calculates tangential force strength at a given distance.
     */
    public float tangentialStrengthAt(float distance) {
        return tangentialStrength * falloffAt(distance);
    }
    
    /**
     * Calculates lift force strength at a given distance.
     */
    public float liftStrengthAt(float distance) {
        return liftStrength * falloffAt(distance);
    }
    
    /**
     * Legacy method - calculates strength at distance (radial only).
     */
    public float strengthAt(float distance) {
        return radialStrengthAt(distance);
    }
    
    private static float smoothstep(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3 - 2 * t);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static ForceZone fromJson(JsonObject json) {
        if (json == null) {
            return linear(10, 0.15f);
        }
        
        float radius = json.has("radius") ? json.get("radius").getAsFloat() : 10f;
        float innerRadius = json.has("innerRadius") ? json.get("innerRadius").getAsFloat() : 0f;
        
        // Support both "strength" and "radialStrength"
        float radialStrength = 0.15f;
        if (json.has("radialStrength")) {
            radialStrength = json.get("radialStrength").getAsFloat();
        } else if (json.has("strength")) {
            radialStrength = json.get("strength").getAsFloat();
        }
        
        float tangentialStrength = json.has("tangentialStrength") 
            ? json.get("tangentialStrength").getAsFloat() : 0f;
        float liftStrength = json.has("liftStrength") 
            ? json.get("liftStrength").getAsFloat() : 0f;
        
        String falloff = json.has("falloff") 
            ? json.get("falloff").getAsString() : DEFAULT_FALLOFF;
        ForceAxis tangentAxis = json.has("tangentAxis") 
            ? ForceAxis.fromId(json.get("tangentAxis").getAsString()) : ForceAxis.Y;
        
        return new ForceZone(radius, innerRadius, radialStrength, 
            tangentialStrength, liftStrength, falloff, tangentAxis);
    }
    
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Builder
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private float radius = 10f;
        private float innerRadius = 0f;
        private float radialStrength = 0.15f;
        private float tangentialStrength = 0f;
        private float liftStrength = 0f;
        private String falloff = DEFAULT_FALLOFF;
        private ForceAxis tangentAxis = ForceAxis.Y;
        
        public Builder radius(float v) { this.radius = v; return this; }
        public Builder innerRadius(float v) { this.innerRadius = v; return this; }
        public Builder radialStrength(float v) { this.radialStrength = v; return this; }
        public Builder tangentialStrength(float v) { this.tangentialStrength = v; return this; }
        public Builder liftStrength(float v) { this.liftStrength = v; return this; }
        public Builder falloff(String v) { this.falloff = v; return this; }
        public Builder tangentAxis(ForceAxis v) { this.tangentAxis = v; return this; }
        
        /** Alias for radialStrength for backward compatibility. */
        public Builder strength(float v) { this.radialStrength = v; return this; }
        
        public ForceZone build() {
            return new ForceZone(radius, innerRadius, radialStrength, 
                tangentialStrength, liftStrength, falloff, tangentAxis);
        }
    }
}
