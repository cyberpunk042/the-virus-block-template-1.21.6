package net.cyberpunk042.visual.appearance;

/**
 * Distribution mode for color application.
 * 
 * <p>Determines how colors are distributed across cells/vertices:</p>
 * <ul>
 *   <li>{@link #UNIFORM} - Same color for the entire shape</li>
 *   <li>{@link #PER_CELL} - Each cell/quad/triangle gets its own color</li>
 * </ul>
 * 
 * <p>Primarily used with RANDOM and MESH_* color modes.</p>
 */
public enum ColorDistribution {
    /**
     * Uniform color distribution.
     * The entire shape uses the same color value.
     * For RANDOM: one random color for whole shape.
     * For MESH_RAINBOW: all vertices at same phase (animate uniformly).
     */
    UNIFORM,
    
    /**
     * Per-cell color distribution.
     * Each cell (quad, triangle, ray) gets its own color.
     * For RANDOM: each cell picks its own random color.
     * For MESH_RAINBOW: colors spread across cells.
     */
    PER_CELL;
    
    /**
     * Default distribution.
     */
    public static ColorDistribution defaultDistribution() {
        return UNIFORM;
    }
    
    /**
     * Parse from string (case-insensitive).
     */
    public static ColorDistribution fromString(String s) {
        if (s == null) return UNIFORM;
        try {
            return valueOf(s.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return UNIFORM;
        }
    }
}
