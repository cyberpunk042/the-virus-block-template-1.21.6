package net.cyberpunk042.visual.appearance;

import java.util.Random;

/**
 * Context for per-vertex color calculation in tessellators.
 * 
 * <p>Passed to tessellators when ColorMode requires per-vertex coloring
 * (MESH_GRADIENT, MESH_RAINBOW, RANDOM).</p>
 */
public record ColorContext(
    ColorMode mode,
    int primaryColor,
    int secondaryColor,
    ColorSet colorSet,
    GradientDirection direction,
    ColorDistribution distribution,
    float timePhase,
    float time,
    float shapeRadius,
    float shapeHeight
) {
    // Random generator seeded per-frame for consistent random colors within a frame
    private static final ThreadLocal<Random> RANDOM = ThreadLocal.withInitial(() -> new Random());
    
    /**
     * Creates a ColorContext from an Appearance and render parameters.
     */
    public static ColorContext from(Appearance appearance, int resolvedPrimary, int resolvedSecondary, 
                                    float time, float shapeRadius, float shapeHeight) {
        return new ColorContext(
            appearance.effectiveColorMode(),
            resolvedPrimary,
            resolvedSecondary,
            appearance.effectiveColorSet(),
            appearance.effectiveDirection(),
            appearance.effectiveDistribution(),
            appearance.timePhase(),
            time,
            shapeRadius,
            shapeHeight
        );
    }
    
    /**
     * Creates a simple context for SOLID mode (single color).
     */
    public static ColorContext solid(int color) {
        return new ColorContext(
            ColorMode.SOLID, color, color, 
            ColorSet.RAINBOW, GradientDirection.Y_AXIS, ColorDistribution.UNIFORM,
            0f, 0f, 1f, 1f
        );
    }
    
    /**
     * Whether this context requires per-vertex color calculation.
     */
    public boolean isPerVertex() {
        return mode.isPerVertex();
    }
    
    /**
     * Calculate color for a vertex at the given position.
     * 
     * @param x Vertex X position (local coordinates)
     * @param y Vertex Y position
     * @param z Vertex Z position
     * @param cellIndex Index of the cell (quad/triangle) this vertex belongs to
     * @return ARGB color for this vertex
     */
    public int calculateColor(float x, float y, float z, int cellIndex) {
        return switch (mode) {
            case SOLID -> primaryColor;
            case GRADIENT -> blendColors(primaryColor, secondaryColor, 0.5f);
            case CYCLING -> primaryColor; // Already handled in renderer
            case MESH_GRADIENT -> calculateMeshGradient(x, y, z, cellIndex);
            case MESH_RAINBOW -> calculateMeshRainbow(x, y, z, cellIndex);
            case RANDOM -> calculateRandom(cellIndex);
        };
    }
    
    /**
     * Calculate color for MESH_GRADIENT mode.
     * Interpolates from primary to secondary based on position or cell index.
     */
    private int calculateMeshGradient(float x, float y, float z, int cellIndex) {
        float t;
        
        if (distribution == ColorDistribution.PER_CELL) {
            // Each cell gets a color based on its index
            // Use a hash function to get a pseudo-random but consistent t value per cell
            t = (cellIndex * 0.618033988749895f) % 1f; // Golden ratio for good distribution
        } else {
            // UNIFORM: Smooth gradient based on vertex position
            t = direction.calculateT(x, y, z, shapeRadius, shapeHeight);
        }
        
        // Add time-based animation via timePhase
        t = (t + timePhase * time / 20f) % 1f;
        if (t < 0) t += 1f;
        
        return blendColors(primaryColor, secondaryColor, t);
    }
    
    /**
     * Calculate color for MESH_RAINBOW mode.
     * Uses full hue spectrum based on position or cell index.
     */
    private int calculateMeshRainbow(float x, float y, float z, int cellIndex) {
        float hue;
        
        if (distribution == ColorDistribution.PER_CELL) {
            // Each cell gets a color based on its index
            hue = (cellIndex * 0.618033988749895f) % 1f; // Golden ratio for good distribution
        } else {
            // UNIFORM: Smooth rainbow based on vertex position
            hue = direction.calculateT(x, y, z, shapeRadius, shapeHeight);
        }
        
        // Add time-based animation via timePhase
        hue = (hue + timePhase * time / 20f) % 1f;
        if (hue < 0) hue += 1f;
        
        return hsbToArgb(hue, 1f, 1f);
    }
    
    /**
     * Calculate color for RANDOM mode.
     * Uses the selected ColorSet.
     */
    private int calculateRandom(int cellIndex) {
        if (distribution == ColorDistribution.UNIFORM) {
            // Same random color for entire shape (seeded by time phase + frame)
            RANDOM.get().setSeed((long)(timePhase * 1000) + (long)(time * 10));
            return colorSet.random(RANDOM.get());
        } else {
            // Different color per cell from the ColorSet
            RANDOM.get().setSeed(cellIndex * 31L + (long)(timePhase * 1000));
            return colorSet.random(RANDOM.get());
        }
    }
    
    /**
     * Blend two ARGB colors.
     */
    private static int blendColors(int color1, int color2, float t) {
        t = Math.max(0, Math.min(1, t));
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int)(a1 + (a2 - a1) * t);
        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Convert HSB to ARGB.
     */
    private static int hsbToArgb(float hue, float saturation, float brightness) {
        int rgb = java.awt.Color.HSBtoRGB(hue, saturation, brightness);
        return 0xFF000000 | (rgb & 0x00FFFFFF); // Ensure full alpha
    }
}
