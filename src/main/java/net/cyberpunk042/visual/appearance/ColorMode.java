package net.cyberpunk042.visual.appearance;

/**
 * Color rendering mode for primitives.
 * 
 * <p>Determines how colors are applied to vertices:</p>
 * <ul>
 *   <li>{@link #SOLID} - Single uniform color (uses primary)</li>
 *   <li>{@link #GRADIENT} - Global blend from primary to secondary (via colorBlend)</li>
 *   <li>{@link #CYCLING} - Animate through a color set over time</li>
 *   <li>{@link #MESH_GRADIENT} - Per-vertex gradient from primary to secondary</li>
 *   <li>{@link #MESH_RAINBOW} - Per-vertex rainbow spectrum across mesh</li>
 *   <li>{@link #RANDOM} - Random color(s) from a color set</li>
 * </ul>
 */
public enum ColorMode {
    /**
     * Single solid color for the entire primitive.
     * Uses the primary color from Appearance.
     */
    SOLID,
    
    /**
     * Global blend from primary to secondary color.
     * Blend ratio controlled by colorBlend slider.
     * Same color for all vertices at any given moment.
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
        return SOLID;
    }
    
    /**
     * Parse from string (case-insensitive).
     */
    public static ColorMode fromString(String s) {
        if (s == null) return SOLID;
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SOLID;
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
        return this == CYCLING || this == RANDOM;
    }
    
    /**
     * Whether this mode uses gradient direction.
     */
    public boolean usesDirection() {
        return this == MESH_GRADIENT || this == MESH_RAINBOW;
    }
}
