package net.cyberpunk042.field.force.mode;

import com.google.gson.JsonObject;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;

/**
 * Configuration for EXPLOSION force mode.
 * 
 * <p>Powerful radial outward blast with optional vertical boost.
 * Can be configured for instant burst or sustained push.
 * 
 * <h2>Parameters</h2>
 * <ul>
 *   <li><b>blastStrength</b>: Initial push force</li>
 *   <li><b>blastRadius</b>: Maximum effect range</li>
 *   <li><b>verticalBoost</b>: Upward force component</li>
 *   <li><b>falloff</b>: How force decreases with distance</li>
 *   <li><b>burstDuration</b>: Ticks over which force is applied (1 = instant)</li>
 *   <li><b>decayCurve</b>: How force decays over burst duration</li>
 * </ul>
 * 
 * @param blastStrength Initial/maximum push force
 * @param blastRadius Maximum effect radius
 * @param verticalBoost Upward force multiplier (0-1 of blast strength)
 * @param falloff Distance falloff type
 * @param burstDuration Ticks to apply force (1 = instant knockback)
 * @param decayCurve How force decays over duration (1 = linear, 2 = quadratic)
 * @param knockbackResistanceIgnore Percentage of knockback resistance to ignore (0-1)
 */
public record ExplosionModeConfig(
    @JsonField(skipIfDefault = true, defaultValue = "0.5") float blastStrength,
    @JsonField(skipIfDefault = true, defaultValue = "15.0") float blastRadius,
    @JsonField(skipIfDefault = true, defaultValue = "0.4") float verticalBoost,
    @JsonField(skipIfDefault = true, defaultValue = "linear") String falloff,
    @JsonField(skipIfDefault = true, defaultValue = "1") int burstDuration,
    @JsonField(skipIfDefault = true, defaultValue = "1.0") float decayCurve,
    @JsonField(skipIfDefault = true) float knockbackResistanceIgnore
) {
    
    /** Default explosion configuration. */
    public static final ExplosionModeConfig DEFAULT = new ExplosionModeConfig(
        0.5f, 15f, 0.4f, "linear", 1, 1f, 0f);
    
    /**
     * Compact constructor with validation.
     */
    public ExplosionModeConfig {
        blastStrength = Math.max(0.1f, blastStrength);
        blastRadius = Math.max(1f, blastRadius);
        verticalBoost = Math.max(0f, Math.min(1f, verticalBoost));
        if (falloff == null || falloff.isBlank()) falloff = "linear";
        burstDuration = Math.max(1, burstDuration);
        decayCurve = Math.max(0.5f, Math.min(3f, decayCurve));
        knockbackResistanceIgnore = Math.max(0f, Math.min(1f, knockbackResistanceIgnore));
    }
    
    /**
     * Whether this is an instant knockback (single tick).
     */
    public boolean isInstant() {
        return burstDuration <= 1;
    }
    
    /**
     * Calculates force strength at a given distance.
     */
    public float strengthAt(float distance) {
        if (distance >= blastRadius) return 0f;
        
        float normalized = distance / blastRadius;
        
        return switch (falloff.toLowerCase()) {
            case "constant" -> blastStrength;
            case "quadratic" -> blastStrength * (1f - normalized * normalized);
            case "inverse" -> blastStrength * normalized; // Stronger at edge
            case "smooth" -> blastStrength * smoothstep(1f - normalized);
            default -> blastStrength * (1f - normalized); // Linear
        };
    }
    
    /**
     * Calculates force multiplier at a given tick in the burst.
     * 
     * @param tickInBurst Current tick (0 to burstDuration-1)
     * @return Force multiplier (1.0 at start, decays toward 0)
     */
    public float burstMultiplier(int tickInBurst) {
        if (burstDuration <= 1) return 1f;
        
        float progress = (float) tickInBurst / (burstDuration - 1);
        return 1f - (float) Math.pow(progress, decayCurve);
    }
    
    /**
     * Calculates vertical force component.
     */
    public float verticalForce() {
        return blastStrength * verticalBoost;
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
        private float blastStrength = 0.5f;
        private float blastRadius = 15f;
        private float verticalBoost = 0.4f;
        private String falloff = "linear";
        private int burstDuration = 1;
        private float decayCurve = 1f;
        private float knockbackResistanceIgnore = 0f;
        
        public Builder blastStrength(float v) { this.blastStrength = v; return this; }
        public Builder blastRadius(float v) { this.blastRadius = v; return this; }
        public Builder verticalBoost(float v) { this.verticalBoost = v; return this; }
        public Builder falloff(String v) { this.falloff = v; return this; }
        public Builder burstDuration(int v) { this.burstDuration = v; return this; }
        public Builder decayCurve(float v) { this.decayCurve = v; return this; }
        public Builder knockbackResistanceIgnore(float v) { this.knockbackResistanceIgnore = v; return this; }
        
        public ExplosionModeConfig build() {
            return new ExplosionModeConfig(blastStrength, blastRadius, verticalBoost,
                falloff, burstDuration, decayCurve, knockbackResistanceIgnore);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static ExplosionModeConfig fromJson(JsonObject json) {
        if (json == null) return DEFAULT;
        
        // Parse knockbackResistanceIgnore carefully (can be boolean or float)
        float kbIgnore = 0f;
        if (json.has("knockbackResistanceIgnore")) {
            var elem = json.get("knockbackResistanceIgnore");
            if (elem.isJsonPrimitive()) {
                var prim = elem.getAsJsonPrimitive();
                if (prim.isBoolean()) {
                    kbIgnore = prim.getAsBoolean() ? 1f : 0f;
                } else if (prim.isNumber()) {
                    kbIgnore = prim.getAsFloat();
                }
            }
        }
        
        return new ExplosionModeConfig(
            json.has("blastStrength") ? json.get("blastStrength").getAsFloat() : 0.5f,
            json.has("blastRadius") ? json.get("blastRadius").getAsFloat() : 15f,
            json.has("verticalBoost") ? json.get("verticalBoost").getAsFloat() : 0.4f,
            json.has("falloff") ? json.get("falloff").getAsString() : "linear",
            json.has("burstDuration") ? json.get("burstDuration").getAsInt() : 1,
            json.has("decayCurve") ? json.get("decayCurve").getAsFloat() : 1f,
            kbIgnore
        );
    }
    
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
}
