package net.cyberpunk042.visual.energy;

/**
 * Energy travel modes - how energy/particles move along the shape.
 * 
 * <p>Renamed from {@code TravelMode} for consistency with energy naming.</p>
 * 
 * <h2>Modes</h2>
 * <ul>
 *   <li><b>NONE</b>: No travel animation</li>
 *   <li><b>CHASE</b>: Discrete particles travel along the shape</li>
 *   <li><b>SCROLL</b>: Continuous gradient scrolls along the shape</li>
 *   <li><b>COMET</b>: Bright head with fading tail</li>
 *   <li><b>SPARK</b>: Random sparks shoot along the shape</li>
 *   <li><b>PULSE_WAVE</b>: Brightness pulses travel along the shape</li>
 *   <li><b>REVERSE_CHASE</b>: Chase particles moving inward</li>
 * </ul>
 * 
 * @see EnergyInteractionType
 */
public enum EnergyTravel {
    /** No travel animation. */
    NONE("None"),
    
    /** Discrete particles/packets travel along the shape. */
    CHASE("Chase"),
    
    /** Continuous gradient scrolls along the shape. */
    SCROLL("Scroll"),
    
    /** Bright head with fading tail travels along the shape. */
    COMET("Comet"),
    
    /** Random sparks shoot along the shape. */
    SPARK("Spark"),
    
    /** Brightness pulse waves travel along the shape. */
    PULSE_WAVE("Pulse Wave"),
    
    /** Chase particles moving inward toward center. */
    REVERSE_CHASE("Reverse Chase"),
    
    /** Bipolar jet - two jets traveling from center outward in opposite directions. 
     *  Phase is mirrored: when one side peaks, the other peaks too. */
    BIPOLAR("Bipolar"),
    
    /** Bipolar jet but with alternating phase - one side leads while the other trails. 
     *  Creates a "ping-pong" effect for more dynamic visuals. */
    BIPOLAR_ALTERNATE("Bipolar Alt");
    
    private final String displayName;
    
    EnergyTravel(String displayName) {
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
     * Whether this mode travels outward (away from center).
     */
    public boolean isOutward() {
        return this != REVERSE_CHASE && this != NONE;
    }
    
    /**
     * Whether this is a bipolar mode (two opposing jets from center).
     */
    public boolean isBipolar() {
        return this == BIPOLAR || this == BIPOLAR_ALTERNATE;
    }
    
    /**
     * Parses from string, case-insensitive.
     */
    public static EnergyTravel fromString(String value) {
        if (value == null) return NONE;
        try {
            return valueOf(value.toUpperCase().replace(" ", "_").replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
