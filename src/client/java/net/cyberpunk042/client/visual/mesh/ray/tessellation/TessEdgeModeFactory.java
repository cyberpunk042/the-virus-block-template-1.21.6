package net.cyberpunk042.client.visual.mesh.ray.tessellation;

import net.cyberpunk042.visual.shape.EdgeTransitionMode;
import net.cyberpunk042.visual.shape.RayFlowStage;
import net.cyberpunk042.visual.shape.ShapeState;

/**
 * Factory for computing edge mode results from ShapeState.
 * 
 * <h2>Stage/Phase Model</h2>
 * ALL stages use phase - phase controls the animation effect.
 * 
 * <h2>How Phase Works Per Stage</h2>
 * <ul>
 *   <li><b>DORMANT:</b> Hidden, phase irrelevant</li>
 *   <li><b>SPAWNING:</b> phase 0→1 = ray appears from edge</li>
 *   <li><b>ACTIVE:</b> phase 0→1 = MAIN ANIMATION (radiation/absorption cycle)</li>
 *   <li><b>DESPAWNING:</b> phase 0→1 = ray disappears from edge</li>
 * </ul>
 * 
 * <h2>Output</h2>
 * <ul>
 *   <li><b>clipStart:</b> Start of visible region (0-1)</li>
 *   <li><b>clipEnd:</b> End of visible region (0-1)</li>
 *   <li><b>scale:</b> Width scale multiplier</li>
 *   <li><b>alpha:</b> Alpha multiplier</li>
 * </ul>
 */
public final class TessEdgeModeFactory {
    
    private TessEdgeModeFactory() {}
    
    /**
     * Compute edge result from ShapeState.
     */
    public static TessEdgeResult compute(ShapeState<RayFlowStage> state) {
        if (state == null) {
            return TessEdgeResult.FULL;
        }
        
        RayFlowStage stage = state.stage();
        float phase = state.phase();
        EdgeTransitionMode edgeMode = state.edgeMode();
        
        if (stage == null) {
            return TessEdgeResult.FULL;
        }
        
        return switch (stage) {
            case DORMANT -> TessEdgeResult.HIDDEN;
            case SPAWNING -> computeSpawnResult(phase, edgeMode);
            case ACTIVE -> computeActiveResult(phase, edgeMode);
            case DESPAWNING -> computeDespawnResult(phase, edgeMode);
        };
    }
    
    /**
     * Compute edge result for SPAWNING stage.
     * phase 0 = just starting to appear
     * phase 1 = fully visible (ready for ACTIVE)
     */
    private static TessEdgeResult computeSpawnResult(float phase, EdgeTransitionMode edgeMode) {
        if (edgeMode == null) edgeMode = EdgeTransitionMode.CLIP;
        
        return switch (edgeMode) {
            case CLIP -> new TessEdgeResult(0f, phase, 1f, 1f);  // Clip from 0 to phase
            case SCALE -> new TessEdgeResult(0f, 1f, phase, 1f);  // Scale width
            case FADE -> new TessEdgeResult(0f, 1f, 1f, phase);   // Fade alpha
        };
    }
    
    /**
     * Compute edge result for ACTIVE stage.
     * 
     * <p>For ACTIVE stage, the base visibility from EdgeMode is:</p>
     * <ul>
     *   <li>CLIP: Full visibility (segment positioning is handled by 
     *       RadiativeInteractionFactory using segmentLength + phase)</li>
     *   <li>SCALE: Phase controls width scale</li>
     *   <li>FADE: Phase controls alpha</li>
     * </ul>
     * 
     * <p>Note: The actual visible segment (based on phase, segmentLength, 
     * startFullLength) is computed in RadiativeInteractionFactory, which
     * produces the final clipStart/clipEnd values for the moving segment.</p>
     */
    private static TessEdgeResult computeActiveResult(float phase, EdgeTransitionMode edgeMode) {
        if (edgeMode == null) edgeMode = EdgeTransitionMode.CLIP;
        
        return switch (edgeMode) {
            case CLIP -> {
                // Full base visibility - segment positioning is handled by 
                // RadiativeInteractionFactory which knows segmentLength
                yield TessEdgeResult.FULL;
            }
            case SCALE -> {
                // Scale the ray width based on phase
                yield new TessEdgeResult(0f, 1f, phase, 1f);
            }
            case FADE -> {
                // Fade alpha based on phase
                yield new TessEdgeResult(0f, 1f, 1f, phase);
            }
        };
    }
    
    /**
     * Compute edge result for DESPAWNING stage.
     * phase 0 = fully visible (just started despawning)
     * phase 1 = gone
     */
    private static TessEdgeResult computeDespawnResult(float phase, EdgeTransitionMode edgeMode) {
        if (edgeMode == null) edgeMode = EdgeTransitionMode.CLIP;
        
        float visibility = 1f - phase;  // Reverse: 1 -> 0
        
        return switch (edgeMode) {
            case CLIP -> new TessEdgeResult(phase, 1f, 1f, 1f);  // Clip from phase to 1
            case SCALE -> new TessEdgeResult(0f, 1f, visibility, 1f);  // Scale width
            case FADE -> new TessEdgeResult(0f, 1f, 1f, visibility);   // Fade alpha
        };
    }
    
    /**
     * Compute using raw parameters (for legacy/testing).
     */
    public static TessEdgeResult compute(
            RayFlowStage stage, 
            float phase, 
            EdgeTransitionMode edgeMode) {
        
        if (stage == null) {
            return TessEdgeResult.FULL;
        }
        
        return switch (stage) {
            case DORMANT -> TessEdgeResult.HIDDEN;
            case SPAWNING -> computeSpawnResult(phase, edgeMode);
            case ACTIVE -> computeActiveResult(phase, edgeMode);
            case DESPAWNING -> computeDespawnResult(phase, edgeMode);
        };
    }
}
