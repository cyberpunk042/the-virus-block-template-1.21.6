package net.cyberpunk042.client.visual.mesh.ray.tessellation;

/**
 * Result of tessellation edge computation.
 * 
 * <h2>What Each Value Means</h2>
 * <ul>
 *   <li><b>clipStart:</b> Parametric start of visible region (0-1). 
 *       Geometry before this is clipped/hidden.</li>
 *   <li><b>clipEnd:</b> Parametric end of visible region (0-1).
 *       Geometry after this is clipped/hidden.</li>
 *   <li><b>scale:</b> Width scale multiplier (0-1).
 *       Used by SCALE edge mode to shrink width at edges.</li>
 *   <li><b>alpha:</b> Alpha multiplier (0-1).
 *       Used by FADE edge mode to fade at edges.</li>
 * </ul>
 * 
 * @param clipStart Start of visible t-range (0-1)
 * @param clipEnd End of visible t-range (0-1)
 * @param scale Width scale multiplier
 * @param alpha Alpha multiplier
 */
public record TessEdgeResult(
    float clipStart,
    float clipEnd,
    float scale,
    float alpha
) {
    /**
     * Fully visible, no modifications.
     */
    public static final TessEdgeResult FULL = new TessEdgeResult(0f, 1f, 1f, 1f);
    
    /**
     * Completely hidden.
     */
    public static final TessEdgeResult HIDDEN = new TessEdgeResult(0f, 0f, 0f, 0f);
    
    public TessEdgeResult {
        clipStart = Math.clamp(clipStart, 0f, 1f);
        clipEnd = Math.clamp(clipEnd, 0f, 1f);
        scale = Math.clamp(scale, 0f, 1f);
        alpha = Math.clamp(alpha, 0f, 1f);
    }
    
    /**
     * Whether anything is visible.
     */
    public boolean isVisible() {
        return clipEnd > clipStart && (scale > 0 || alpha > 0);
    }
    
    /**
     * Whether this uses geometric clipping.
     */
    public boolean hasClipping() {
        return clipStart > 0.001f || clipEnd < 0.999f;
    }
    
    /**
     * Whether this uses width scaling.
     */
    public boolean hasScaling() {
        return scale < 0.999f;
    }
    
    /**
     * Whether this uses alpha fading.
     */
    public boolean hasFading() {
        return alpha < 0.999f;
    }
    
    /**
     * Visible t-range (for iteration).
     */
    public float visibleRange() {
        return clipEnd - clipStart;
    }
}
