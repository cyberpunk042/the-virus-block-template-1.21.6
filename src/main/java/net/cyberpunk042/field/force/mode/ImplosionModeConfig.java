package net.cyberpunk042.field.force.mode;

import com.google.gson.JsonObject;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;

/**
 * Configuration for IMPLOSION force mode.
 * 
 * <p>Intense gravitational collapse toward the center. The pull force
 * accelerates as entities approach the core, creating a black hole-like
 * effect.
 * 
 * <h2>Parameters</h2>
 * <ul>
 *   <li><b>coreStrength</b>: Force strength at the core</li>
 *   <li><b>suctionRadius</b>: Maximum pull range</li>
 *   <li><b>suctionStrength</b>: Force strength at outer edge</li>
 *   <li><b>accelerationCurve</b>: How sharply force increases near center</li>
 *   <li><b>eventHorizon</b>: Distance below which pull is maximum constant</li>
 *   <li><b>crushDamage</b>: Damage per tick in the core</li>
 * </ul>
 * 
 * @param coreStrength Force at center (maximum pull)
 * @param suctionRadius Maximum effect range
 * @param suctionStrength Force at outer edge (minimum pull)
 * @param accelerationCurve Exponent for force increase (1 = linear, 2 = quadratic, 3 = cubic)
 * @param eventHorizon Radius of constant max force zone
 * @param crushDamage Damage per tick within event horizon (0 = no damage)
 * @param preventEscape If true, entities in core cannot escape
 */
public record ImplosionModeConfig(
    @JsonField(skipIfDefault = true, defaultValue = "0.4") float coreStrength,
    @JsonField(skipIfDefault = true, defaultValue = "20.0") float suctionRadius,
    @JsonField(skipIfDefault = true, defaultValue = "0.05") float suctionStrength,
    @JsonField(skipIfDefault = true, defaultValue = "2.0") float accelerationCurve,
    @JsonField(skipIfDefault = true, defaultValue = "2.0") float eventHorizon,
    @JsonField(skipIfDefault = true) float crushDamage,
    @JsonField(skipIfDefault = true) boolean preventEscape
) {
    
    /** Default implosion configuration. */
    public static final ImplosionModeConfig DEFAULT = new ImplosionModeConfig(
        0.4f, 20f, 0.05f, 2f, 2f, 0f, false);
    
    /**
     * Compact constructor with validation.
     */
    public ImplosionModeConfig {
        coreStrength = Math.max(0.01f, coreStrength);
        suctionRadius = Math.max(eventHorizon + 1f, suctionRadius);
        suctionStrength = Math.max(0.01f, suctionStrength);
        accelerationCurve = Math.max(0.5f, Math.min(5f, accelerationCurve));
        eventHorizon = Math.max(0.5f, eventHorizon);
        crushDamage = Math.max(0f, crushDamage);
    }
    
    /**
     * Calculates pull strength at a given distance.
     * Force increases as entities approach the center.
     */
    public float strengthAt(float distance) {
        if (distance <= eventHorizon) {
            return coreStrength;
        }
        if (distance >= suctionRadius) {
            return 0f;
        }
        
        // Normalized distance (0 at event horizon, 1 at max radius)
        float normalized = (distance - eventHorizon) / (suctionRadius - eventHorizon);
        
        // Apply acceleration curve (higher = faster acceleration near center)
        float factor = 1f - (float) Math.pow(normalized, 1.0 / accelerationCurve);
        
        // Interpolate between suction strength and core strength
        return suctionStrength + (coreStrength - suctionStrength) * factor;
    }
    
    /**
     * Checks if an entity is within the escape prevention zone.
     */
    public boolean isTrapped(float distance) {
        return preventEscape && distance <= eventHorizon;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Builder
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private float coreStrength = 0.4f;
        private float suctionRadius = 20f;
        private float suctionStrength = 0.05f;
        private float accelerationCurve = 2f;
        private float eventHorizon = 2f;
        private float crushDamage = 0f;
        private boolean preventEscape = false;
        
        public Builder coreStrength(float v) { this.coreStrength = v; return this; }
        public Builder suctionRadius(float v) { this.suctionRadius = v; return this; }
        public Builder suctionStrength(float v) { this.suctionStrength = v; return this; }
        public Builder accelerationCurve(float v) { this.accelerationCurve = v; return this; }
        public Builder eventHorizon(float v) { this.eventHorizon = v; return this; }
        public Builder crushDamage(float v) { this.crushDamage = v; return this; }
        public Builder preventEscape(boolean v) { this.preventEscape = v; return this; }
        
        public ImplosionModeConfig build() {
            return new ImplosionModeConfig(coreStrength, suctionRadius, suctionStrength,
                accelerationCurve, eventHorizon, crushDamage, preventEscape);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static ImplosionModeConfig fromJson(JsonObject json) {
        if (json == null) return DEFAULT;
        
        return new ImplosionModeConfig(
            json.has("coreStrength") ? json.get("coreStrength").getAsFloat() : 0.4f,
            json.has("suctionRadius") ? json.get("suctionRadius").getAsFloat() : 20f,
            json.has("suctionStrength") ? json.get("suctionStrength").getAsFloat() : 0.05f,
            json.has("accelerationCurve") ? json.get("accelerationCurve").getAsFloat() : 2f,
            json.has("eventHorizon") ? json.get("eventHorizon").getAsFloat() : 2f,
            json.has("crushDamage") ? json.get("crushDamage").getAsFloat() : 0f,
            json.has("preventEscape") && json.get("preventEscape").getAsBoolean()
        );
    }
    
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
}
