package net.cyberpunk042.visual.animation;

/**
 * Animation mode controlling FLICKER overlay on rays.
 * 
 * <p>These modes add random or rhythmic alpha variation to create
 * twinkling, sparkling, or strobing effects. Flicker is applied
 * as a multiplier on top of other alpha calculations.</p>
 * 
 * <h2>Modes</h2>
 * <ul>
 *   <li><b>NONE</b>: No flicker animation</li>
 *   <li><b>SCINTILLATION</b>: Random per-ray flickering (star twinkling)</li>
 *   <li><b>STROBE</b>: Rhythmic on/off blinking (synchronized)</li>
 * </ul>
 * 
 * <h2>Mathematical Basis</h2>
 * <pre>
 * SCINTILLATION: alpha *= noise(rayIndex, time * frequency) * intensity
 * STROBE: alpha *= step(sin(time * frequency), 0) * intensity
 * </pre>
 * 
 * @see RayFlowConfig
 */
public enum FlickerMode {
    /** No flicker animation. */
    NONE("None"),
    
    /** Random per-ray flickering (star-like twinkling). */
    SCINTILLATION("Scintillation"),
    
    /** Rhythmic synchronized strobing. */
    STROBE("Strobe");
    
    private final String displayName;
    
    FlickerMode(String displayName) {
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
    public static FlickerMode fromString(String value) {
        if (value == null) return NONE;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
