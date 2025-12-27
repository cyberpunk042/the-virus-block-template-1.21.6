package net.cyberpunk042.visual.animation;

/**
 * Controls how 3D ray shapes behave at spawn/despawn edges.
 * 
 * <p>When a 3D ray (droplet, egg, etc.) spawns at the inner edge or despawns
 * at the outer edge during RADIATE/ABSORB animations, this mode determines
 * what visual effect is used.</p>
 * 
 * <h2>Modes</h2>
 * <ul>
 *   <li><b>SCALE</b>: Entire shape scales up/down uniformly</li>
 *   <li><b>CLIP</b>: Shape is progressively "eaten" from one end (like trimming geometry)</li>
 *   <li><b>FADE</b>: Shape fades in/out via alpha without changing geometry</li>
 * </ul>
 * 
 * <h2>How CLIP Works</h2>
 * <p>Each vertex of the 3D shape has an implicit t-value (0 at base, 1 at tip)
 * based on its position along the shape's axis. Vertices outside the visible
 * t-range have their alpha set to 0, creating the illusion of the shape being
 * consumed from one end.</p>
 * 
 * @see RayFlowConfig
 */
public enum EdgeTransitionMode {
    /** Entire shape scales uniformly at edges. */
    SCALE("Scale"),
    
    /** Shape is progressively clipped/eaten from edge. */
    CLIP("Clip"),
    
    /** Shape fades in/out via alpha without changing geometry. */
    FADE("Fade");
    
    private final String displayName;
    
    EdgeTransitionMode(String displayName) {
        this.displayName = displayName;
    }
    
    public String displayName() {
        return displayName;
    }
    
    /**
     * Parses from string, case-insensitive.
     * @param value String to parse
     * @return Matching mode or SCALE if not found
     */
    public static EdgeTransitionMode fromString(String value) {
        if (value == null) return SCALE;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SCALE;
        }
    }
}
