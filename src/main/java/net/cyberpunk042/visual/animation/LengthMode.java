package net.cyberpunk042.visual.animation;

/**
 * Animation mode controlling ray LENGTH over time.
 * 
 * <p>These modes animate how much of the ray is visible (from 0% to 100% of its full length).
 * The effect is achieved by modulating the visible t-range [tMin, tMax] over time.</p>
 * 
 * <h2>Modes</h2>
 * <ul>
 *   <li><b>NONE</b>: Full ray visible at all times</li>
 *   <li><b>RADIATE</b>: Ray grows from inner→outer (emission/explosion)</li>
 *   <li><b>ABSORB</b>: Ray shrinks from outer→inner (absorption/implosion)</li>
 *   <li><b>PULSE</b>: Ray length oscillates (breathing effect)</li>
 * </ul>
 * 
 * @see RayFlowConfig
 */
public enum LengthMode {
    /** No length animation - full ray visible. */
    NONE("None"),
    
    /** Ray grows outward from origin (emission effect). */
    RADIATE("Radiate"),
    
    /** Ray shrinks inward toward origin (absorption effect). */
    ABSORB("Absorb"),
    
    /** Ray length oscillates (breathing/pulsing). */
    PULSE("Pulse");
    
    private final String displayName;
    
    LengthMode(String displayName) {
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
    public static LengthMode fromString(String value) {
        if (value == null) return NONE;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
