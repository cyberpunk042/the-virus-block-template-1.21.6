package net.cyberpunk042.visual.animation;

/**
 * Animation mode controlling TRAVEL of energy/visibility along the ray.
 * 
 * <p>These modes animate WHERE the visible/bright portion of the ray is positioned,
 * creating the illusion of energy, particles or waves moving along the ray path.</p>
 * 
 * <h2>Modes</h2>
 * <ul>
 *   <li><b>NONE</b>: No travel animation</li>
 *   <li><b>CHASE</b>: Discrete "particles" travel along the ray (can have multiple)</li>
 *   <li><b>SCROLL</b>: Continuous gradient slides along the ray (like UV scrolling)</li>
 *   <li><b>COMET</b>: Bright head with fading tail travels along ray</li>
 *   <li><b>SPARK</b>: Random sparks shoot along the ray</li>
 *   <li><b>PULSE_WAVE</b>: Brightness pulses travel along the ray</li>
 *   <li><b>REVERSE_CHASE</b>: Chase particles moving inward (toward center)</li>
 * </ul>
 * 
 * <h2>Mathematical Basis</h2>
 * <p>All modes work by offsetting the parametric t-value:</p>
 * <pre>
 * t_animated = (t + time * speed) mod 1.0
 * </pre>
 * 
 * @see RayFlowConfig
 */
public enum TravelMode {
    /** No travel animation. */
    NONE("None"),
    
    /** Discrete particles/packets travel along the ray. */
    CHASE("Chase"),
    
    /** Continuous gradient scrolls along the ray. */
    SCROLL("Scroll"),
    
    /** Bright head with fading tail travels along the ray. Like a shooting star. */
    COMET("Comet"),
    
    /** Random sparks shoot along the ray. Occasional flash bursts. */
    SPARK("Spark"),
    
    /** Brightness pulse waves travel along the ray. Rhythmic wave pattern. */
    PULSE_WAVE("Pulse Wave"),
    
    /** Same as CHASE but particles move inward toward center. */
    REVERSE_CHASE("Reverse Chase");
    
    private final String displayName;
    
    TravelMode(String displayName) {
        this.displayName = displayName;
    }
    
    public String displayName() {
        return displayName;
    }
    
    /**
     * Whether this mode is active (not NONE).
     */
    public boolean isActive() {
        return this != NONE;
    }
    
    /**
     * Whether this mode travels outward (away from center) or inward.
     */
    public boolean isOutward() {
        return this != REVERSE_CHASE && this != NONE;
    }
    
    /**
     * Parses from string, case-insensitive.
     * @param value String to parse
     * @return Matching mode or NONE if not found
     */
    public static TravelMode fromString(String value) {
        if (value == null) return NONE;
        try {
            return valueOf(value.toUpperCase().replace(" ", "_").replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}

