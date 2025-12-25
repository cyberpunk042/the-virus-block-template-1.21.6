package net.cyberpunk042.visual.appearance;

/**
 * Color rendering mode for primitives.
 * 
 * <p>Determines how colors are applied to vertices:</p>
 * <ul>
 *   <li>{@link #SOLID} - Single color for entire primitive</li>
 *   <li>{@link #GRADIENT} - Blend from primary to secondary color along geometry</li>
 *   <li>{@link #RAINBOW} - Cycle through hue spectrum over time</li>
 * </ul>
 */
public enum ColorMode {
    /**
     * Single solid color for the entire primitive.
     * Uses the primary color from Appearance.
     */
    SOLID,
    
    /**
     * Gradient from primary to secondary color.
     * For rays/lines: transitions along the length.
     * For surfaces: transitions based on position (e.g., radial from center).
     */
    GRADIENT,
    
    /**
     * Animated rainbow effect cycling through hue spectrum.
     * Speed controlled by rainbowSpeed in Appearance.
     */
    RAINBOW;
    
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
}
