package net.cyberpunk042.visual.animation;

/**
 * Defines waveform shapes for animations.
 * 
 * <p>Used by PulseConfig and AlphaPulseConfig to control
 * the shape of the animation curve.</p>
 * 
 * <h3>Visual Characteristics</h3>
 * <ul>
 *   <li>SINE - Smooth oscillation (DEFAULT)</li>
 *   <li>SQUARE - Instant on/off</li>
 *   <li>TRIANGLE_WAVE - Linear up/down</li>
 *   <li>SAWTOOTH - Linear up, instant reset</li>
 * </ul>
 * 
 * @see PulseConfig
 * @see AlphaPulseConfig
 */
public enum Waveform {
    /** Smooth sinusoidal oscillation (DEFAULT) */
    SINE,
    
    /** Instant on/off switching */
    SQUARE,
    
    /** Linear up and down - named to avoid confusion with TrianglePattern */
    TRIANGLE_WAVE,
    
    /** Linear increase, instant reset to minimum */
    SAWTOOTH;
    
    /**
     * Evaluates the waveform at time t.
     * 
     * @param t Time value, typically 0-1 for one cycle
     * @return Value between 0 and 1
     */
    public float evaluate(float t) {
        // Normalize t to 0-1 range
        t = t - (float) Math.floor(t);
        
        return switch (this) {
            case SINE -> (float) (Math.sin(t * Math.PI * 2) * 0.5 + 0.5);
            case SQUARE -> t < 0.5f ? 1.0f : 0.0f;
            case TRIANGLE_WAVE -> t < 0.5f ? t * 2 : 2 - t * 2;
            case SAWTOOTH -> t;
        };
    }

    
    /**
     * Parse from string (case-insensitive).
     * @param id The string identifier
     * @return Matching Waveform, or SINE if not found
     */
    public static Waveform fromId(String id) {
        if (id == null || id.isEmpty()) return SINE;
        try {
            return valueOf(id.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return SINE;
        }
    }
}
