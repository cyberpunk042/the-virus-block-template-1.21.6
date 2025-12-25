package net.cyberpunk042.visual.animation;

/**
 * Animation mode controlling FLICKER/alpha overlay on rays.
 * 
 * <p>These modes add random or rhythmic alpha variation to create
 * twinkling, sparkling, strobing, or flickering effects. Flicker is applied
 * as a multiplier on top of other alpha calculations.</p>
 * 
 * <h2>Modes</h2>
 * <ul>
 *   <li><b>NONE</b>: No flicker animation</li>
 *   <li><b>SCINTILLATION</b>: Random per-ray flickering (star twinkling)</li>
 *   <li><b>STROBE</b>: Rhythmic on/off blinking (synchronized)</li>
 *   <li><b>FADE_PULSE</b>: Smooth fade in/out (breathing alpha)</li>
 *   <li><b>FLICKER</b>: Candlelight-style unstable flickering</li>
 *   <li><b>LIGHTNING</b>: Flash bright then fade (spike and decay)</li>
 *   <li><b>HEARTBEAT</b>: Double-pulse rhythm pattern</li>
 * </ul>
 * 
 * <h2>Mathematical Basis</h2>
 * <pre>
 * SCINTILLATION: alpha *= noise(rayIndex, time * frequency) * intensity
 * STROBE: alpha *= step(sin(time * frequency), 0) * intensity
 * FADE_PULSE: alpha *= (sin(time * frequency) + 1) / 2 * intensity
 * FLICKER: alpha *= noise(time) * intensity
 * LIGHTNING: alpha *= max(0, 1 - decayRate * timeSinceFlash) * intensity
 * HEARTBEAT: alpha *= doublePulse(time, frequency) * intensity
 * </pre>
 * 
 * @see RayFlowConfig
 */
public enum FlickerMode {
    /** No flicker animation. */
    NONE("None"),
    
    /** Random per-ray flickering (star-like twinkling). */
    SCINTILLATION("Scintillation"),
    
    /** Rhythmic synchronized strobing. On/off blinking. */
    STROBE("Strobe"),
    
    /** Smooth fade in/out. Breathing alpha effect. */
    FADE_PULSE("Fade Pulse"),
    
    /** Candlelight-style unstable flickering. Random alpha noise. */
    FLICKER("Flicker"),
    
    /** Flash bright then fade. Spike and decay pattern. */
    LIGHTNING("Lightning"),
    
    /** Double-pulse rhythm. Two quick pulses followed by pause. */
    HEARTBEAT("Heartbeat");
    
    private final String displayName;
    
    FlickerMode(String displayName) {
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
     * Whether this mode is random/per-ray or synchronized across all rays.
     */
    public boolean isPerRay() {
        return this == SCINTILLATION || this == FLICKER;
    }
    
    /**
     * Parses from string, case-insensitive.
     * @param value String to parse
     * @return Matching mode or NONE if not found
     */
    public static FlickerMode fromString(String value) {
        if (value == null) return NONE;
        try {
            return valueOf(value.toUpperCase().replace(" ", "_").replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}

