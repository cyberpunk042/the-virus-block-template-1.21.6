package net.cyberpunk042.field.force.mode;

import com.google.gson.JsonObject;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;

/**
 * Configuration for PUSH force mode.
 * 
 * <p>Simple repulsion from the force field center with optional vertical boost
 * and burst behavior.
 * 
 * <h2>Parameters</h2>
 * <ul>
 *   <li><b>strength</b>: Base push force</li>
 *   <li><b>falloff</b>: How strength changes with distance</li>
 *   <li><b>radius</b>: Maximum effect range</li>
 *   <li><b>verticalBoost</b>: Additional upward force</li>
 *   <li><b>burstMode</b>: If true, applies full force instantly then fades</li>
 *   <li><b>burstDecay</b>: How fast burst force decays (0-1, higher = faster decay)</li>
 * </ul>
 * 
 * @param strength Base push strength
 * @param falloff Falloff type
 * @param radius Maximum effect radius
 * @param verticalBoost Upward force component
 * @param burstMode Whether to apply as instant burst vs sustained
 * @param burstDecay Decay rate for burst mode (0-1)
 */
public record PushModeConfig(
    @JsonField(skipIfDefault = true, defaultValue = "0.2") float strength,
    @JsonField(skipIfDefault = true, defaultValue = "linear") String falloff,
    @JsonField(skipIfDefault = true, defaultValue = "15.0") float radius,
    @JsonField(skipIfDefault = true, defaultValue = "0.3") float verticalBoost,
    @JsonField(skipIfDefault = true) boolean burstMode,
    @JsonField(skipIfDefault = true, defaultValue = "0.1") float burstDecay
) {
    
    /** Default push configuration. */
    public static final PushModeConfig DEFAULT = new PushModeConfig(
        0.2f, "linear", 15f, 0.3f, false, 0.1f);
    
    /**
     * Compact constructor with validation.
     */
    public PushModeConfig {
        strength = Math.max(0.01f, strength);
        radius = Math.max(1f, radius);
        if (falloff == null || falloff.isBlank()) falloff = "linear";
        verticalBoost = Math.max(0f, verticalBoost);
        burstDecay = Math.max(0f, Math.min(1f, burstDecay));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Builder
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private float strength = 0.2f;
        private String falloff = "linear";
        private float radius = 15f;
        private float verticalBoost = 0.3f;
        private boolean burstMode = false;
        private float burstDecay = 0.1f;
        
        public Builder strength(float v) { this.strength = v; return this; }
        public Builder falloff(String v) { this.falloff = v; return this; }
        public Builder radius(float v) { this.radius = v; return this; }
        public Builder verticalBoost(float v) { this.verticalBoost = v; return this; }
        public Builder burstMode(boolean v) { this.burstMode = v; return this; }
        public Builder burstDecay(float v) { this.burstDecay = v; return this; }
        
        public PushModeConfig build() {
            return new PushModeConfig(strength, falloff, radius, verticalBoost, burstMode, burstDecay);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static PushModeConfig fromJson(JsonObject json) {
        if (json == null) return DEFAULT;
        
        return new PushModeConfig(
            json.has("strength") ? json.get("strength").getAsFloat() : 0.2f,
            json.has("falloff") ? json.get("falloff").getAsString() : "linear",
            json.has("radius") ? json.get("radius").getAsFloat() : 15f,
            json.has("verticalBoost") ? json.get("verticalBoost").getAsFloat() : 0.3f,
            json.has("burstMode") && json.get("burstMode").getAsBoolean(),
            json.has("burstDecay") ? json.get("burstDecay").getAsFloat() : 0.1f
        );
    }
    
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
}
