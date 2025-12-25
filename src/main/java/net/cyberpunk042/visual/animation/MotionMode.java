package net.cyberpunk042.visual.animation;

/**
 * Animation mode controlling ray GEOMETRY motion in space.
 * 
 * <p>These modes transform vertex positions to create physical movement
 * of the rays themselves (not just visibility changes).</p>
 * 
 * <h2>Modes</h2>
 * <ul>
 *   <li><b>NONE</b>: No geometry motion</li>
 *   <li><b>LINEAR</b>: Rays translate in a direction (drifting)</li>
 *   <li><b>OSCILLATE</b>: Rays move back-and-forth (pendulum/wave)</li>
 *   <li><b>SPIRAL</b>: Rays rotate around an axis while optionally moving along it</li>
 *   <li><b>RIPPLE</b>: Radial wave propagation displacing rays outward/inward</li>
 * </ul>
 * 
 * <h2>Mathematical Basis</h2>
 * <pre>
 * LINEAR:    pos += direction * time * speed
 * OSCILLATE: pos += direction * sin(time * frequency) * amplitude
 * SPIRAL:    pos = rotate(pos, axis, time * speed)
 * RIPPLE:    pos += normal * sin(distance - time * speed) * amplitude
 * </pre>
 * 
 * @see RayMotionConfig
 */
public enum MotionMode {
    /** No geometry motion. */
    NONE("None"),
    
    /** Linear translation in a direction. */
    LINEAR("Linear"),
    
    /** Oscillating back-and-forth motion. */
    OSCILLATE("Oscillate"),
    
    /** Helical/spiral rotation around an axis. */
    SPIRAL("Spiral"),
    
    /** Radial wave propagation. */
    RIPPLE("Ripple");
    
    private final String displayName;
    
    MotionMode(String displayName) {
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
    public static MotionMode fromString(String value) {
        if (value == null) return NONE;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
