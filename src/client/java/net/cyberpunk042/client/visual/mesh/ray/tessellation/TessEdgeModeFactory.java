package net.cyberpunk042.client.visual.mesh.ray.tessellation;

import net.cyberpunk042.visual.shape.EdgeTransitionMode;

/**
 * Factory for computing edge transition effects based on EdgeTransitionMode.
 * 
 * <h2>EdgeTransitionMode</h2>
 * <p>Controls how the ray segment appears at its edges during animation:</p>
 * <ul>
 *   <li><b>CLIP:</b> Portions of the shape are geometrically clipped at edges</li>
 *   <li><b>SCALE:</b> The shape uniformly shrinks as it approaches edges</li>
 *   <li><b>FADE:</b> The shape fades with gradient alpha based on edge proximity</li>
 * </ul>
 * 
 * <h2>Edge Intensity</h2>
 * <p>The intensity parameter (0-1) controls how strong the edge effect is:</p>
 * <ul>
 *   <li><b>0:</b> No edge effect - shape is fully visible regardless of position</li>
 *   <li><b>1:</b> Full edge effect - maximum clipping/scaling/fading at edges</li>
 * </ul>
 * 
 * <h2>Usage with RadiativeInteraction</h2>
 * <p>RadiativeInteraction determines WHERE the segment is positioned on the ray (clipStart/clipEnd).
 * EdgeTransitionMode determines HOW the shape looks at boundaries as it moves.</p>
 * 
 * <h2>Key Insight</h2>
 * <p>SCALE and FADE are based on <b>shape center proximity to edges</b>, NOT on segment size.
 * When the shape center (midpoint of clipRange) is at t=0.5, effects are minimal.
 * When the shape center approaches t=0 or t=1, effects are maximal.</p>
 * 
 * @see EdgeTransitionMode
 * @see RadiativeInteractionFactory
 */
public final class TessEdgeModeFactory {
    
    private TessEdgeModeFactory() {} // Utility class
    
    /**
     * Compute edge effect based on EdgeTransitionMode, clip range, and intensity.
     * 
     * @param edgeMode The edge transition mode (CLIP/SCALE/FADE)
     * @param clipStart Start of visible segment (0-1), from RadiativeInteraction
     * @param clipEnd End of visible segment (0-1), from RadiativeInteraction
     * @param intensity Edge effect intensity (0=no effect, 1=full effect)
     * @return Edge result with appropriate effect applied
     */
    public static TessEdgeResult compute(EdgeTransitionMode edgeMode, float clipStart, float clipEnd, float intensity) {
        if (edgeMode == null) {
            edgeMode = EdgeTransitionMode.CLIP;
        }
        
        // Clamp intensity minimum to 0 (no upper limit - values > 1 create exaggerated effects)
        intensity = Math.max(0f, intensity);
        
        // Compute the shape center position (where the 3D shape is placed on the ray)
        float shapeCenter = (clipStart + clipEnd) * 0.5f;
        
        // Compute edge proximity: how close is the shape center to either edge?
        // At t=0 or t=1: proximity = 0 (at edge)
        // At t=0.5: proximity = 1 (center of ray, farthest from edges)
        float distanceToStart = shapeCenter;           // Distance from t=0
        float distanceToEnd = 1.0f - shapeCenter;      // Distance from t=1
        float edgeProximity = Math.min(distanceToStart, distanceToEnd) * 2.0f; // Normalize to [0,1]
        edgeProximity = Math.clamp(edgeProximity, 0f, 1f);
        
        // Apply intensity to edge proximity (blend between 1.0 and actual proximity)
        // At intensity=0: proximity = 1 (no edge effect)
        // At intensity=1: proximity = actual edge proximity (full effect)
        float effectiveProximity = 1.0f - (1.0f - edgeProximity) * intensity;
        
        // Apply intensity to clip range (blend between [0,1] and actual clip)
        float effectiveClipStart = clipStart * intensity;
        float effectiveClipEnd = 1.0f - (1.0f - clipEnd) * intensity;
        
        return switch (edgeMode) {
            case CLIP -> {
                // CLIP mode: geometrically clip the visible range
                // At intensity=0: full visibility [0,1]
                // At intensity=1: actual clip range [clipStart, clipEnd]
                yield new TessEdgeResult(effectiveClipStart, effectiveClipEnd, 1f, 1f, distanceToStart, distanceToEnd);
            }
            case SCALE -> {
                // SCALE mode: full visibility (no clipping), but uniform scale based on edge proximity
                // At intensity=0: scale = 1 (no scaling)
                // At intensity=1: scale = edgeProximity (full scaling effect)
                yield new TessEdgeResult(0f, 1f, effectiveProximity, 1f, distanceToStart, distanceToEnd);
            }
            case FADE -> {
                // FADE mode: full visibility, full size, gradient alpha based on edge proximity
                // The effectiveProximity modulates the edge distances used for fade gradient
                // At intensity=0: no fade (full alpha everywhere)
                // At intensity=1: full fade based on edge proximity
                float fadeDistStart = 1.0f - (1.0f - distanceToStart) * intensity;
                float fadeDistEnd = 1.0f - (1.0f - distanceToEnd) * intensity;
                yield new TessEdgeResult(0f, 1f, 1f, 1f, fadeDistStart, fadeDistEnd);
            }
        };
    }
    
    /**
     * Compute edge effect with default intensity of 1.0 (full effect).
     */
    public static TessEdgeResult compute(EdgeTransitionMode edgeMode, float clipStart, float clipEnd) {
        return compute(edgeMode, clipStart, clipEnd, 1.0f);
    }
    
    /**
     * Compute edge effect from ClipRange (from RadiativeInteractionFactory).
     * 
     * @param edgeMode The edge transition mode
     * @param clipRange The clip range from RadiativeInteractionFactory
     * @param intensity Edge effect intensity (0=no effect, 1=full effect)
     * @return Edge result with appropriate effect applied
     */
    public static TessEdgeResult compute(EdgeTransitionMode edgeMode, RadiativeInteractionFactory.ClipRange clipRange, float intensity) {
        if (clipRange == null) {
            return TessEdgeResult.FULL;
        }
        return compute(edgeMode, clipRange.start(), clipRange.end(), intensity);
    }
    
    /**
     * Compute edge effect from ClipRange with default intensity of 1.0.
     */
    public static TessEdgeResult compute(EdgeTransitionMode edgeMode, RadiativeInteractionFactory.ClipRange clipRange) {
        return compute(edgeMode, clipRange, 1.0f);
    }
}
