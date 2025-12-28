package net.cyberpunk042.visual.shape;


/**
 * Immutable state of an animatable shape.
 * 
 * <h2>User Controls</h2>
 * The user sets all three values in UI:
 * <ul>
 *   <li><b>Stage:</b> Which lifecycle stage (e.g., SPAWNING, ACTIVE)</li>
 *   <li><b>Phase:</b> 0-1 progress within the stage</li>
 *   <li><b>EdgeMode:</b> How edges appear during transitions (CLIP, SCALE, FADE)</li>
 * </ul>
 * 
 * <h2>Animation Behavior</h2>
 * Animation "plays with" phase (modifies it over time) while respecting 
 * stage and edgeMode (reads them but doesn't change them).
 * 
 * <h2>Preview Capability</h2>
 * User can set stage=SPAWNING, phase=0.5, edgeMode=CLIP and see exactly 
 * what the shape looks like at 50% spawn with clip effect - WITHOUT 
 * running any animation.
 * 
 * @param stage The current stage (user controlled)
 * @param phase Progress within the stage (user controlled, animation modifies)
 * @param edgeMode How edges appear during transitional stages (user controlled)
 */

public record ShapeState<S extends ShapeStage>(
    S stage,
    float phase,
    EdgeTransitionMode edgeMode
) {
    
    /**
     * Compact constructor with validation.
     */
    public ShapeState {
        if (edgeMode == null) edgeMode = EdgeTransitionMode.CLIP;
        phase = Math.clamp(phase, 0f, 1f);
    }
    
    // ─────────────────── Immutable "setters" ───────────────────
    
    public ShapeState<S> withStage(S newStage) {
        return new ShapeState<>(newStage, phase, edgeMode);
    }
    
    public ShapeState<S> withPhase(float newPhase) {
        return new ShapeState<>(stage, newPhase, edgeMode);
    }
    
    public ShapeState<S> withEdgeMode(EdgeTransitionMode newMode) {
        return new ShapeState<>(stage, phase, newMode);
    }
    
    // ─────────────────── Convenience queries ───────────────────
    
    /**
     * Returns true if the shape should be visible in this state.
     */
    public boolean isVisible() {
        return stage != null && stage.isVisible();
    }
    
    /**
     * Returns true if this is a transitional state.
     */
    public boolean isTransitional() {
        return stage != null && stage.isTransitional();
    }
}

