package net.cyberpunk042.visual.animation;

/**
 * Rotation axis for spin animations.
 * 
 * <h2>Axes</h2>
 * <ul>
 *   <li>{@link #X} - Pitch rotation (forward/back tilt)</li>
 *   <li>{@link #Y} - Yaw rotation (horizontal spin) - default</li>
 *   <li>{@link #Z} - Roll rotation (barrel roll)</li>
 * </ul>
 * 
 * @see Animation
 */
public enum Axis {
    /** Pitch rotation (forward/back tilt). */
    X("x"),
    
    /** Yaw rotation (horizontal spin). Default for most effects. */
    Y("y"),
    
    /** Roll rotation (barrel roll). */
    Z("z");
    
    private final String id;
    
    Axis(String id) {
        this.id = id;
    }
    
    public String id() {
        return id;
    }
    
    /**
     * Parses from string, defaults to Y.
     */
    public static Axis fromId(String id) {
        if (id == null) return Y;
        String lower = id.toLowerCase();
        for (Axis axis : values()) {
            if (axis.id.equals(lower)) {
                return axis;
            }
        }
        return Y;
    }
}
