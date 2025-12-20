package net.cyberpunk042.field.force.zone;

import com.google.gson.JsonObject;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;

/**
 * Represents a force zone within a force field.
 * 
 * <p>A zone defines a radius-based region with a specific force strength
 * and falloff behavior. Force fields can have multiple nested zones with
 * different strengths.
 * 
 * <h2>Falloff Types</h2>
 * <ul>
 *   <li><b>linear</b>: Strength decreases linearly from center to edge</li>
 *   <li><b>quadratic</b>: Strength decreases quadratically (faster at edges)</li>
 *   <li><b>constant</b>: Uniform strength throughout the zone</li>
 *   <li><b>inverse</b>: Strength increases toward edges</li>
 * </ul>
 * 
 * @param radius Maximum radius of this zone
 * @param strength Base force strength at center
 * @param falloff Falloff type ("linear", "quadratic", "constant", "inverse")
 */
public record ForceZone(
    float radius,
    float strength,
    @JsonField(skipIfDefault = true, defaultValue = "linear") String falloff
) {
    
    /** Default falloff type. */
    public static final String DEFAULT_FALLOFF = "linear";
    
    /**
     * Creates a linear falloff zone.
     */
    public static ForceZone linear(float radius, float strength) {
        return new ForceZone(radius, strength, "linear");
    }
    
    /**
     * Creates a quadratic falloff zone.
     */
    public static ForceZone quadratic(float radius, float strength) {
        return new ForceZone(radius, strength, "quadratic");
    }
    
    /**
     * Creates a constant strength zone (no falloff).
     */
    public static ForceZone constant(float radius, float strength) {
        return new ForceZone(radius, strength, "constant");
    }
    
    /**
     * Creates an inverse falloff zone (stronger at edges).
     */
    public static ForceZone inverse(float radius, float strength) {
        return new ForceZone(radius, strength, "inverse");
    }
    
    /**
     * Compact constructor with default falloff.
     */
    public ForceZone {
        if (radius <= 0) radius = 1f;
        if (strength < 0) strength = 0f;
        if (falloff == null || falloff.isBlank()) falloff = DEFAULT_FALLOFF;
    }
    
    /**
     * Alias for {@link #falloff()} for GUI compatibility.
     */
    public String falloffName() {
        return falloff;
    }
    
    /**
     * Checks if the given distance is within this zone's radius.
     */
    public boolean containsDistance(float distance) {
        return distance <= radius && distance >= 0;
    }
    
    /**
     * Calculates the force strength at the given distance.
     * Applies falloff based on the zone's falloff type.
     * 
     * @param distance Distance from force field center
     * @return Effective strength at this distance
     */
    public float strengthAt(float distance) {
        if (distance < 0 || distance > radius) {
            return 0f;
        }
        
        float normalizedDist = distance / radius; // 0 at center, 1 at edge
        
        return switch (falloff.toLowerCase()) {
            case "constant" -> strength;
            case "linear" -> strength * (1f - normalizedDist);
            case "quadratic" -> strength * (1f - normalizedDist * normalizedDist);
            case "inverse" -> strength * normalizedDist; // Stronger at edges
            case "smooth" -> strength * smoothstep(1f - normalizedDist);
            default -> strength * (1f - normalizedDist); // Default to linear
        };
    }
    
    /**
     * Smooth step function for smooth falloff.
     */
    private static float smoothstep(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Parses from JSON.
     */
    public static ForceZone fromJson(JsonObject json) {
        if (json == null) {
            return linear(10, 0.15f);
        }
        
        float radius = json.has("radius") ? json.get("radius").getAsFloat() : 10f;
        float strength = json.has("strength") ? json.get("strength").getAsFloat() : 0.15f;
        String falloff = json.has("falloff") ? json.get("falloff").getAsString() : DEFAULT_FALLOFF;
        
        return new ForceZone(radius, strength, falloff);
    }
    
    /**
     * Serializes to JSON.
     */
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
}
