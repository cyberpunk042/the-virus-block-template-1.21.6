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
     * Compute edge effect based on EdgeTransitionMode and clip range.
     * 
     * @param edgeMode The edge transition mode (CLIP/SCALE/FADE)
     * @param clipStart Start of visible segment (0-1), from RadiativeInteraction
     * @param clipEnd End of visible segment (0-1), from RadiativeInteraction
     * @return Edge result with appropriate effect applied
     */
    public static TessEdgeResult compute(EdgeTransitionMode edgeMode, float clipStart, float clipEnd) {
        if (edgeMode == null) {
            edgeMode = EdgeTransitionMode.CLIP;
        }
        
        // Compute the shape center position (where the 3D shape is placed on the ray)
        float shapeCenter = (clipStart + clipEnd) * 0.5f;
        
        // Compute edge proximity: how close is the shape center to either edge?
        // At t=0 or t=1: proximity = 0 (at edge)
        // At t=0.5: proximity = 1 (center of ray, farthest from edges)
        float distanceToStart = shapeCenter;           // Distance from t=0
        float distanceToEnd = 1.0f - shapeCenter;      // Distance from t=1
        float edgeProximity = Math.min(distanceToStart, distanceToEnd) * 2.0f; // Normalize to [0,1]
        edgeProximity = Math.clamp(edgeProximity, 0f, 1f);
        
        return switch (edgeMode) {
            case CLIP -> {
                // CLIP mode: geometrically clip the visible range
                // The shape is rendered normally but portions outside [clipStart, clipEnd] are hidden
                yield new TessEdgeResult(clipStart, clipEnd, 1f, 1f, distanceToStart, distanceToEnd);
            }
            case SCALE -> {
                // SCALE mode: full visibility (no clipping), but uniform scale based on edge proximity
                // Shape scales from 0 (at edge) to 1 (at center)
                // Full visibility range [0,1], shape uniformly scaled by edgeProximity
                yield new TessEdgeResult(0f, 1f, edgeProximity, 1f, distanceToStart, distanceToEnd);
            }
            case FADE -> {
                // FADE mode: full visibility, full size, gradient alpha based on edge proximity
                // Base alpha is 1.0 - the per-vertex gradient is applied in the generator
                // using edgeDistanceStart/End to create smooth fade at edges
                yield new TessEdgeResult(0f, 1f, 1f, 1f, distanceToStart, distanceToEnd);
            }
        };
    }
    
    /**
     * Compute edge effect from ClipRange (from RadiativeInteractionFactory).
     * 
     * @param edgeMode The edge transition mode
     * @param clipRange The clip range from RadiativeInteractionFactory
     * @return Edge result with appropriate effect applied
     */
    public static TessEdgeResult compute(EdgeTransitionMode edgeMode, RadiativeInteractionFactory.ClipRange clipRange) {
        if (clipRange == null) {
            return TessEdgeResult.FULL;
        }
        return compute(edgeMode, clipRange.start(), clipRange.end());
    }
}
