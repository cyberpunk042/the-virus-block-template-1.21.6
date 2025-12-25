package net.cyberpunk042.visual.appearance;

/**
 * Color rendering mode for primitives.
 * 
 * <p>Determines how colors are applied to vertices:</p>
 * <ul>
 *   <li>{@link #GRADIENT} - Uniform blend from primary to secondary (via colorBlend)</li>
 *   <li>{@link #CYCLING} - Animate through a color set over time</li>
 *   <li>{@link #MESH_GRADIENT} - Per-vertex gradient from primary to secondary</li>
 *   <li>{@link #MESH_RAINBOW} - Per-vertex rainbow spectrum across mesh</li>
 *   <li>{@link #RANDOM} - Random color(s) from a color set</li>
 * </ul>
 */
public enum ColorMode {
    /**
     * Uniform blend from primary to secondary color.
     * Blend ratio controlled by colorBlend slider.
     * colorBlend=0 → pure primary, colorBlend=1 → pure secondary.
     */
    GRADIENT,
    
    /**
     * Animated cycling through a color set.
     * Cycles through colors in the selected ColorSet over time.
     * Uses timePhase for animation offset.
     */
    CYCLING,
    
    /**
     * Per-vertex gradient from primary to secondary color.
     * Color varies across the mesh based on gradient direction.
     * Creates smooth color transitions across the geometry.
     */
    MESH_GRADIENT,
    
    /**
     * Per-vertex rainbow spectrum across the mesh.
     * Full hue range distributed along gradient direction.
     * Can animate with timePhase for scrolling rainbow effect.
     */
    MESH_RAINBOW,
    
    /**
     * Random color selection from a color set.
     * Distribution controls whether uniform (one random for all)
     * or per-cell (each cell gets its own random color).
     */
    RANDOM;
    
    /**
     * Default color mode.
     */
    public static ColorMode defaultMode() {
        return GRADIENT;
    }
    
    /**
     * Parse from string (case-insensitive, handles legacy SOLID).
     */
    public static ColorMode fromString(String s) {
        if (s == null) return GRADIENT;
        String upper = s.toUpperCase();
        // Backward compatibility: SOLID → GRADIENT
        if ("SOLID".equals(upper)) return GRADIENT;
        try {
            return valueOf(upper);
        } catch (IllegalArgumentException e) {
            return GRADIENT;
        }
    }
    
    /**
     * Whether this mode requires per-vertex color calculation.
     */
    public boolean isPerVertex() {
        return this == MESH_GRADIENT || this == MESH_RAINBOW || 
               (this == RANDOM); // RANDOM can be per-cell
    }
    
    /**
     * Whether this mode uses animation/time.
     */
    public boolean isAnimated() {
        return this == CYCLING || this == MESH_RAINBOW;
    }
    
    /**
     * Whether this mode uses a color set.
     */
    public boolean usesColorSet() {
        return this == CYCLING || this == RANDOM || this == MESH_RAINBOW;
    }
    
    /**
     * Whether this mode uses gradient direction.
     */
    public boolean usesDirection() {
        return this == MESH_GRADIENT || this == MESH_RAINBOW;
    }
}
