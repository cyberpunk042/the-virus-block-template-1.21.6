package net.cyberpunk042.visual.layer;

/**
 * Defines how a layer blends with layers behind it.
 * 
 * <p>Phase 1 supports NORMAL and ADD. Phase 2 will add
 * MULTIPLY and SCREEN (requires custom shaders).</p>
 * 
 * <h3>Phase 1</h3>
 * <ul>
 *   <li>NORMAL - Standard alpha blending</li>
 *   <li>ADD - Additive blending (glowing effects)</li>
 * </ul>
 * 
 * <h3>Phase 2 (Future - requires custom shaders)</h3>
 * <ul>
 *   <li>MULTIPLY - Darkening blend</li>
 *   <li>SCREEN - Brightening blend</li>
 * </ul>
 * 
 * @see FieldLayer
 */
public enum BlendMode {
    /** Standard alpha blending (DEFAULT) */
    NORMAL,
    
    /** Additive blending - colors add together (glow effects) */
    ADD,
    
    /** Multiply blend - darkens (Phase 2 - requires custom shader) */
    MULTIPLY,
    
    /** Screen blend - brightens (Phase 2 - requires custom shader) */
    SCREEN;
    
    /**
     * Returns true if this blend mode is supported in Phase 1.
     */
    public boolean isPhase1Supported() {
        return this == NORMAL || this == ADD;
    }
}
