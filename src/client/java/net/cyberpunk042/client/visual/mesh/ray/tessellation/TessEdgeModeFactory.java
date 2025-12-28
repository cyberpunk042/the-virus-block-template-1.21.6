package net.cyberpunk042.client.visual.mesh.ray.tessellation;

import net.cyberpunk042.visual.shape.EdgeTransitionMode;

/**
 * Factory for computing edge transition effects based on EdgeTransitionMode.
 * 
 * <h2>EdgeTransitionMode</h2>
 * <p>Controls how the ray segment appears at its edges during animation:</p>
 * <ul>
 *   <li><b>CLIP:</b> Portions of the ray are clipped (hidden) at edges</li>
 *   <li><b>SCALE:</b> The ray size changes at edges</li>
 *   <li><b>FADE:</b> The ray alpha fades at edges</li>
 * </ul>
 * 
 * <h2>Usage with RadiativeInteraction</h2>
 * <p>RadiativeInteraction determines WHERE the segment is positioned on the ray.
 * EdgeTransitionMode determines HOW it looks at its edges as it moves.</p>
 * 
 * <h2>Output (TessEdgeResult)</h2>
 * <ul>
 *   <li><b>clipStart/clipEnd:</b> Visible range (0-1) for CLIP mode</li>
 *   <li><b>scale:</b> Size multiplier for SCALE mode</li>
 *   <li><b>alpha:</b> Alpha multiplier for FADE mode</li>
 * </ul>
 * 
 * @see EdgeTransitionMode
 * @see RadiativeInteractionFactory
 */
public final class TessEdgeModeFactory {
    
    private TessEdgeModeFactory() {} // Utility class
    
    /**
     * Compute edge effect based on EdgeTransitionMode and edge proximity.
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
        
        return switch (edgeMode) {
            case CLIP -> {
                // CLIP mode: use clipStart/clipEnd directly
                yield new TessEdgeResult(clipStart, clipEnd, 1f, 1f);
            }
            case SCALE -> {
                // SCALE mode: full visibility, but scale based on segment size
                float segmentSize = clipEnd - clipStart;
                yield new TessEdgeResult(0f, 1f, segmentSize, 1f);
            }
            case FADE -> {
                // FADE mode: full visibility, but alpha based on segment size  
                float segmentSize = clipEnd - clipStart;
                yield new TessEdgeResult(0f, 1f, 1f, segmentSize);
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
