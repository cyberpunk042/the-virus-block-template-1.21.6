package net.cyberpunk042.field.force.zone;

import com.google.gson.JsonObject;
import net.cyberpunk042.field.force.falloff.FalloffFunction;
import net.cyberpunk042.field.force.falloff.FalloffFunctions;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;

/**
 * Configuration for a single force zone within a field.
 * 
 * <p>A force zone defines the force behavior within a specific radius.
 * Multiple zones can be layered to create complex force effects
 * (e.g., weak pull at 15 blocks, medium at 8 blocks, strong at 3 blocks).
 * 
 * <p>Zones are evaluated from outermost to innermost. An entity at a given
 * distance will be affected by the zone whose radius contains that distance.
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * {
 *   "radius": 15,
 *   "strength": 0.1,
 *   "falloff": "linear"
 * }
 * </pre>
 * 
 * @param radius Maximum radius of this zone (blocks)
 * @param strength Base force strength for this zone
 * @param falloffName Name of falloff function (linear, quadratic, gaussian, etc.)
 */
public record ForceZone(
    float radius,
    float strength,
    @JsonField(skipIfDefault = true, defaultValue = "linear") String falloffName
) {
    
    /**
     * Compact constructor with validation.
     */
    public ForceZone {
        radius = Math.max(0.1f, radius);
        strength = Math.max(0, strength);
        if (falloffName == null || falloffName.isEmpty()) falloffName = "linear";
    }
    
    /**
     * Returns the FalloffFunction for this zone.
     */
    public FalloffFunction falloff() {
        return FalloffFunctions.fromName(falloffName);
    }
    
    /**
     * Returns true if the given distance is within this zone.
     * 
     * @param distance Distance from force field center
     */
    public boolean containsDistance(float distance) {
        return distance >= 0 && distance <= radius;
    }
    
    /**
     * Calculates the force strength at a given distance within this zone.
     * 
     * @param distance Distance from force field center
     * @return Force strength (0 if outside zone)
     */
    public float strengthAt(float distance) {
        if (!containsDistance(distance)) {
            return 0f;
        }
        float falloffMultiplier = falloff().apply(distance, radius);
        return strength * falloffMultiplier;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Factory Methods
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a zone with linear falloff.
     */
    public static ForceZone linear(float radius, float strength) {
        return new ForceZone(radius, strength, "linear");
    }
    
    /**
     * Creates a zone with quadratic (gravity-like) falloff.
     */
    public static ForceZone quadratic(float radius, float strength) {
        return new ForceZone(radius, strength, "quadratic");
    }
    
    /**
     * Creates a zone with constant strength (no falloff).
     */
    public static ForceZone constant(float radius, float strength) {
        return new ForceZone(radius, strength, "constant");
    }
    
    /**
     * Creates a zone with gaussian (bell curve) falloff.
     */
    public static ForceZone gaussian(float radius, float strength) {
        return new ForceZone(radius, strength, "gaussian");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Parses from JSON.
     */
    public static ForceZone fromJson(JsonObject json) {
        if (json == null) {
            return linear(10, 0.1f);
        }
        
        float radius = json.has("radius") ? json.get("radius").getAsFloat() : 10;
        float strength = json.has("strength") ? json.get("strength").getAsFloat() : 0.1f;
        
        // Support both "falloff" and "falloffName"
        String falloff = "linear";
        if (json.has("falloffName")) {
            falloff = json.get("falloffName").getAsString();
        } else if (json.has("falloff")) {
            falloff = json.get("falloff").getAsString();
        }
        
        return new ForceZone(radius, strength, falloff);
    }
    
    /**
     * Serializes to JSON.
     */
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
        private float strength = 0.1f;
        private String falloffName = "linear";
        
        public Builder radius(float r) { this.radius = r; return this; }
        public Builder strength(float s) { this.strength = s; return this; }
        public Builder falloff(String f) { this.falloffName = f; return this; }
        
        public ForceZone build() {
            return new ForceZone(radius, strength, falloffName);
        }
    }
}
