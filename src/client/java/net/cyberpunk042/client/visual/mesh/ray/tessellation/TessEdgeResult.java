package net.cyberpunk042.client.visual.mesh.ray.tessellation;

/**
 * Result of tessellation edge computation.
 * 
 * <h2>What Each Value Means</h2>
 * <ul>
 *   <li><b>clipStart:</b> Parametric start of visible region (0-1). 
 *       Geometry before this is clipped/hidden. Used by CLIP mode.</li>
 *   <li><b>clipEnd:</b> Parametric end of visible region (0-1).
 *       Geometry after this is clipped/hidden. Used by CLIP mode.</li>
 *   <li><b>scale:</b> Uniform size multiplier (0-1).
 *       Used by SCALE mode - shape shrinks uniformly as it approaches edges.</li>
 *   <li><b>alpha:</b> Base alpha multiplier (0-1).
 *       Used by FADE mode - shape fades as it approaches edges.</li>
 *   <li><b>edgeDistanceStart:</b> Distance from shape center to t=0 boundary (0-1).
 *       Used for gradient fading - 0=at start edge, 1=far from start.</li>
 *   <li><b>edgeDistanceEnd:</b> Distance from shape center to t=1 boundary (0-1).
 *       Used for gradient fading - 0=at end edge, 1=far from end.</li>
 * </ul>
 * 
 * @param clipStart Start of visible t-range (0-1)
 * @param clipEnd End of visible t-range (0-1)
 * @param scale Uniform size multiplier (0-1)
 * @param alpha Base alpha multiplier (0-1)
 * @param edgeDistanceStart Distance from shape to start edge (for fade gradient)
 * @param edgeDistanceEnd Distance from shape to end edge (for fade gradient)
 */
public record TessEdgeResult(
    float clipStart,
    float clipEnd,
    float scale,
    float alpha,
    float edgeDistanceStart,
    float edgeDistanceEnd
) {
    /**
     * Fully visible, no modifications.
     */
    public static final TessEdgeResult FULL = new TessEdgeResult(0f, 1f, 1f, 1f, 0.5f, 0.5f);
    
    /**
     * Completely hidden.
     */
    public static final TessEdgeResult HIDDEN = new TessEdgeResult(0f, 0f, 0f, 0f, 0f, 0f);
    
    public TessEdgeResult {
        clipStart = Math.clamp(clipStart, 0f, 1f);
        clipEnd = Math.clamp(clipEnd, 0f, 1f);
        scale = Math.clamp(scale, 0f, 1f);
        alpha = Math.clamp(alpha, 0f, 1f);
        edgeDistanceStart = Math.clamp(edgeDistanceStart, 0f, 1f);
        edgeDistanceEnd = Math.clamp(edgeDistanceEnd, 0f, 1f);
    }
    
    /**
     * Backward compatibility constructor (without edge distances).
     */
    public TessEdgeResult(float clipStart, float clipEnd, float scale, float alpha) {
        this(clipStart, clipEnd, scale, alpha, 
             (clipStart + clipEnd) * 0.5f,  // Distance to start = center position
             1.0f - (clipStart + clipEnd) * 0.5f);  // Distance to end
    }
    
    /**
     * Whether anything is visible.
     */
    public boolean isVisible() {
        return clipEnd > clipStart && (scale > 0.001f || alpha > 0.001f);
    }
    
    /**
     * Whether this uses geometric clipping.
     */
    public boolean hasClipping() {
        return clipStart > 0.001f || clipEnd < 0.999f;
    }
    
    /**
     * Whether this uses size scaling.
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
    
    /**
     * Computes alpha for a vertex at position t based on edge proximity.
     * For FADE mode, creates a gradient where vertices closer to edges are more transparent.
     * 
     * @param t Vertex position along the shape (0=base, 1=tip)
     * @param baseAlpha Base alpha to multiply
     * @return Computed alpha with edge fade applied
     */
    public float computeFadeAlpha(float t, float baseAlpha) {
        // If no edge fade needed (both edges far away), return base alpha
        if (edgeDistanceStart > 0.4f && edgeDistanceEnd > 0.4f) {
            return baseAlpha;
        }
        
        // Gradient fade based on vertex position and edge proximity
        // When shape is near start edge (edgeDistanceStart small), 
        //   vertices with low t (base) should be more transparent
        // When shape is near end edge (edgeDistanceEnd small),
        //   vertices with high t (tip) should be more transparent
        
        float startFade = 1.0f;
        float endFade = 1.0f;
        
        // Fade at start edge: affects vertices with low t
        if (edgeDistanceStart < 0.4f) {
            // How much are we at the start edge? (0=far, 1=at edge)
            float startEdgeFactor = 1.0f - (edgeDistanceStart / 0.4f);
            // Vertices with low t are affected more
            float vertexFactor = 1.0f - t;  // 1 at base, 0 at tip
            startFade = 1.0f - (startEdgeFactor * vertexFactor);
        }
        
        // Fade at end edge: affects vertices with high t
        if (edgeDistanceEnd < 0.4f) {
            // How much are we at the end edge? (0=far, 1=at edge)
            float endEdgeFactor = 1.0f - (edgeDistanceEnd / 0.4f);
            // Vertices with high t are affected more
            float vertexFactor = t;  // 0 at base, 1 at tip
            endFade = 1.0f - (endEdgeFactor * vertexFactor);
        }
        
        return baseAlpha * startFade * endFade;
    }
}
