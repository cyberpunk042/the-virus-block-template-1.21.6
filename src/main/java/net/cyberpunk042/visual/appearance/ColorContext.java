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
    float colorBlend,
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
            appearance.colorBlend(),
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
            ColorSet.RAINBOW, GradientDirection.Y_AXIS, ColorDistribution.GRADIENT,
            0f, 0f, 0f, 1f, 1f
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
            case GRADIENT -> blendColors(primaryColor, secondaryColor, colorBlend);
            case CYCLING -> primaryColor; // Already handled in renderer
            case MESH_GRADIENT -> calculateMeshGradient(x, y, z, cellIndex);
            case MESH_RAINBOW -> calculateMeshRainbow(x, y, z, cellIndex);
            case RANDOM -> calculateRandom(cellIndex);
        };
    }
    
    /**
     * Calculate color for MESH_GRADIENT mode.
     * Interpolates from primary to (primary→secondary blended by colorBlend) based on position.
     */
    private int calculateMeshGradient(float x, float y, float z, int cellIndex) {
        float t;
        
        switch (distribution) {
            case INDEXED -> {
                // Each cell gets a color based on its index (stepped pattern)
                t = (cellIndex * 0.618033988749895f) % 1f;
            }
            case RANDOM -> {
                // Each cell gets a random t value (animated by time)
                long seed = cellIndex * 31L + (long)(timePhase * 1000) + (long)(time / 40f);
                RANDOM.get().setSeed(seed);
                t = RANDOM.get().nextFloat();
            }
            default -> {
                // GRADIENT: Smooth gradient based on vertex position
                t = direction.calculateT(x, y, z, shapeRadius, shapeHeight);
            }
        }
        
        // Add time-based animation via timePhase
        t = (t + timePhase * time / 20f) % 1f;
        if (t < 0) t += 1f;
        
        // Use colorBlend to control how far towards secondary the gradient goes
        // colorBlend=0 → all primary, colorBlend=1 → full primary→secondary gradient
        int targetColor = blendColors(primaryColor, secondaryColor, colorBlend);
        return blendColors(primaryColor, targetColor, t);
    }
    
    /**
     * Calculate color for MESH_RAINBOW mode.
     * Uses ColorSet's spectrum - full HSB for RAINBOW, palette interpolation for others.
     */
    private int calculateMeshRainbow(float x, float y, float z, int cellIndex) {
        switch (distribution) {
            case INDEXED -> {
                // Each cell gets a color based on its index (stepped pattern)
                float t = (cellIndex * 0.618033988749895f) % 1f;
                t = (t + timePhase * time / 20f) % 1f;
                if (t < 0) t += 1f;
                return colorSet.interpolateSpectrum(t);
            }
            case RANDOM -> {
                // Each cell gets a random color from the palette
                // Use time-based seed that changes to animate
                long seed = cellIndex * 31L + (long)(timePhase * 1000) + (long)(time / 40f);
                RANDOM.get().setSeed(seed);
                return colorSet.random(RANDOM.get());
            }
            default -> {
                // GRADIENT: Smooth spectrum based on vertex position
                float t = direction.calculateT(x, y, z, shapeRadius, shapeHeight);
                t = (t + timePhase * time / 20f) % 1f;
                if (t < 0) t += 1f;
                return colorSet.interpolateSpectrum(t);
            }
        }
    }
    
    /**
     * Calculate color for RANDOM mode.
     * Uses the selected ColorSet.
     */
    private int calculateRandom(int cellIndex) {
        switch (distribution) {
            case INDEXED -> {
                // Each cell gets a color based on index from the palette
                return colorSet.color(cellIndex);
            }
            case RANDOM -> {
                // Each cell gets a random color from the ColorSet (animated)
                long seed = cellIndex * 31L + (long)(timePhase * 1000) + (long)(time / 40f);
                RANDOM.get().setSeed(seed);
                return colorSet.random(RANDOM.get());
            }
            default -> {
                // GRADIENT: Same random color for entire shape
                RANDOM.get().setSeed((long)(timePhase * 1000) + (long)(time * 10));
                return colorSet.random(RANDOM.get());
            }
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
