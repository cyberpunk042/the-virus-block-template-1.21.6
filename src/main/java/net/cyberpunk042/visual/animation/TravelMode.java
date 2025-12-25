package net.cyberpunk042.visual.animation;

/**
 * Animation mode controlling TRAVEL of visibility along the ray.
 * 
 * <p>These modes animate WHERE the visible portion of the ray is positioned,
 * creating the illusion of energy or particles moving along the ray path.</p>
 * 
 * <h2>Modes</h2>
 * <ul>
 *   <li><b>NONE</b>: No travel animation</li>
 *   <li><b>CHASE</b>: Discrete "particles" travel along the ray (can have multiple)</li>
 *   <li><b>SCROLL</b>: Continuous gradient slides along the ray (like UV scrolling)</li>
 * </ul>
 * 
 * <h2>Mathematical Basis</h2>
 * <p>Both modes work by offsetting the parametric t-value:</p>
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
    SCROLL("Scroll");
    
    private final String displayName;
    
    TravelMode(String displayName) {
        this.displayName = displayName;
    }
    
    public String displayName() {
        return displayName;
    }
    
    /**
     * Parses from string, case-insensitive.
     * @param value String to parse
     * @return Matching mode or NONE if not found
     */
    public static TravelMode fromString(String value) {
        if (value == null) return NONE;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
