package net.cyberpunk042.field;

import com.google.gson.JsonObject;

/**
 * Configuration for movement prediction on personal fields.
 * 
 * <p>Prediction allows the field to anticipate player movement,
 * creating a smoother visual experience.
 */
public record PredictionConfig(
        boolean enabled,
        int leadTicks,
        float maxDistance,
        float lookAhead,
        float verticalBoost
) {
    // =========================================================================
    // Defaults (the "sweetspot" values)
    // =========================================================================
    
    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_LEAD_TICKS = 2;
    public static final float DEFAULT_MAX_DISTANCE = 1.5f;
    public static final float DEFAULT_LOOK_AHEAD = 0.0f;
    public static final float DEFAULT_VERTICAL_BOOST = 0.0f;
    
    /**
     * Returns the default prediction configuration (enabled with sweetspot values).
     */
    public static PredictionConfig defaults() {
        return new PredictionConfig(
            DEFAULT_ENABLED,
            DEFAULT_LEAD_TICKS,
            DEFAULT_MAX_DISTANCE,
            DEFAULT_LOOK_AHEAD,
            DEFAULT_VERTICAL_BOOST
        );
    }
    
    /**
     * Returns a disabled prediction configuration.
     */
    public static PredictionConfig disabled() {
        return new PredictionConfig(false, 0, 0, 0, 0);
    }
    
    // =========================================================================
    // With methods
    // =========================================================================
    
    public PredictionConfig withEnabled(boolean enabled) {
        return new PredictionConfig(enabled, leadTicks, maxDistance, lookAhead, verticalBoost);
    }
    
    public PredictionConfig withLeadTicks(int ticks) {
        return new PredictionConfig(enabled, ticks, maxDistance, lookAhead, verticalBoost);
    }
    
    public PredictionConfig withMaxDistance(float distance) {
        return new PredictionConfig(enabled, leadTicks, distance, lookAhead, verticalBoost);
    }
    
    public PredictionConfig withLookAhead(float offset) {
        return new PredictionConfig(enabled, leadTicks, maxDistance, offset, verticalBoost);
    }
    
    public PredictionConfig withVerticalBoost(float boost) {
        return new PredictionConfig(enabled, leadTicks, maxDistance, lookAhead, boost);
    }
    
    // =========================================================================
    // JSON
    // =========================================================================
    
    public static PredictionConfig fromJson(JsonObject json) {
        if (json == null) {
            return defaults();
        }
        
        boolean enabled = json.has("enabled") ? json.get("enabled").getAsBoolean() : DEFAULT_ENABLED;
        int leadTicks = json.has("leadTicks") ? json.get("leadTicks").getAsInt() : DEFAULT_LEAD_TICKS;
        float maxDistance = json.has("maxDistance") ? json.get("maxDistance").getAsFloat() : DEFAULT_MAX_DISTANCE;
        float lookAhead = json.has("lookAhead") ? json.get("lookAhead").getAsFloat() : DEFAULT_LOOK_AHEAD;
        float verticalBoost = json.has("verticalBoost") ? json.get("verticalBoost").getAsFloat() : DEFAULT_VERTICAL_BOOST;
        
        return new PredictionConfig(enabled, leadTicks, maxDistance, lookAhead, verticalBoost);
    }
    
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);
        if (leadTicks != DEFAULT_LEAD_TICKS) {
            json.addProperty("leadTicks", leadTicks);
        }
        if (maxDistance != DEFAULT_MAX_DISTANCE) {
            json.addProperty("maxDistance", maxDistance);
        }
        if (lookAhead != DEFAULT_LOOK_AHEAD) {
            json.addProperty("lookAhead", lookAhead);
        }
        if (verticalBoost != DEFAULT_VERTICAL_BOOST) {
            json.addProperty("verticalBoost", verticalBoost);
        }
        return json;
    }
}
