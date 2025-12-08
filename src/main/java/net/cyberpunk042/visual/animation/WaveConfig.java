package net.cyberpunk042.visual.animation;

import com.google.gson.JsonObject;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;

/**
 * Configuration for surface wave/ripple animation (FUTURE).
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "wave": {
 *   "amplitude": 0.1,
 *   "frequency": 2.0,
 *   "direction": "Y"
 * }
 * </pre>
 * 
 * @see Animation
 * @see Axis
 */
public record WaveConfig(
    @Range(ValueRange.POSITIVE) float amplitude,
    @Range(ValueRange.POSITIVE) float frequency,
    Axis direction
) {
    /**
     * Parses WaveConfig from JSON.
     * 
     * @param json JSON object with amplitude, frequency, direction fields
     * @return parsed config or NONE if invalid
     */
    public static WaveConfig fromJson(com.google.gson.JsonObject json) {
        if (json == null) return NONE;
        
        // Parse amplitude and frequency
        float amplitude = json.has("amplitude") ? json.get("amplitude").getAsFloat() : 0.1f;
        float frequency = json.has("frequency") ? json.get("frequency").getAsFloat() : 2.0f;
        
        // Parse direction axis
        Axis direction = Axis.Y;
        if (json.has("direction")) {
            String dirStr = json.get("direction").getAsString().toUpperCase();
            try {
                direction = Axis.valueOf(dirStr);
            } catch (IllegalArgumentException e) {
                direction = Axis.Y; // Default to Y
            }
        }
        
        // Return NONE if amplitude is zero
        if (amplitude <= 0) return NONE;
        
        return new WaveConfig(amplitude, frequency, direction);
    }
    
    public static final WaveConfig NONE = new WaveConfig(0, 0, Axis.Y);
    
    /** Default gentle wave. */
    public static final WaveConfig DEFAULT = new WaveConfig(0.1f, 2.0f, Axis.Y);
    
    /** Strong wave. */
    public static final WaveConfig STRONG = new WaveConfig(0.3f, 3.0f, Axis.Y);
    
    /** Horizontal ripple. */
    public static final WaveConfig RIPPLE = new WaveConfig(0.05f, 4.0f, Axis.Y);
    
    /**
     * Creates a vertical wave.
     * @param amplitude Wave height
     * @param frequency Wave frequency
     */
    public static WaveConfig vertical(@Range(ValueRange.POSITIVE) float amplitude, @Range(ValueRange.POSITIVE) float frequency) {
        return new WaveConfig(amplitude, frequency, Axis.Y);
    }
    
    /** Whether wave is active. */
    public boolean isActive() {
        return amplitude > 0 && frequency > 0;
    }
    
    /**
     * Serializes this wave config to JSON.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("amplitude", amplitude);
        json.addProperty("frequency", frequency);
        json.addProperty("direction", direction.name());
        return json;
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private @Range(ValueRange.POSITIVE) float amplitude = 0.1f;
        private @Range(ValueRange.POSITIVE) float frequency = 2.0f;
        private Axis direction = Axis.Y;
        
        public Builder amplitude(float a) { this.amplitude = a; return this; }
        public Builder frequency(float f) { this.frequency = f; return this; }
        public Builder direction(Axis d) { this.direction = d; return this; }
        
        public WaveConfig build() {
            return new WaveConfig(amplitude, frequency, direction);
        }
    }
}
