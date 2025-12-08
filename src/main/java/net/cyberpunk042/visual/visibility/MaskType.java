package net.cyberpunk042.visual.visibility;

/**
 * Defines visibility mask patterns for primitives.
 * 
 * <p>Masks control which cells of a shape are visible, enabling
 * effects like bands, stripes, and checkerboard patterns.</p>
 * 
 * <h3>Phase 1 Parameters</h3>
 * <ul>
 *   <li>mask - The MaskType</li>
 *   <li>count - Number of divisions</li>
 *   <li>thickness - Band/stripe thickness (0-1)</li>
 * </ul>
 * 
 * <h3>Phase 2 Parameters (Future)</h3>
 * <ul>
 *   <li>offset - Pattern offset/phase</li>
 *   <li>invert - Invert visibility</li>
 *   <li>feather - Edge softness</li>
 *   <li>animate - Animate pattern</li>
 *   <li>animSpeed - Animation speed</li>
 * </ul>
 * 
 * @see VisibilityMask
 */
public enum MaskType {
    /** All cells visible (DEFAULT) */
    FULL,
    
    /** Horizontal stripes (latitude-based) */
    BANDS,
    
    /** Vertical stripes (longitude-based) */
    STRIPES,
    
    /** Checkerboard pattern */
    CHECKER,
    
    /** Radial gradient visibility (Phase 2) */
    RADIAL,
    
    /** Linear gradient visibility (Phase 2) */
    GRADIENT,
    
    /** Custom mask function (Future) */
    CUSTOM;
    
    /**
     * Parse from string (case-insensitive).
     * @param id The string identifier
     * @return Matching MaskType, or FULL if not found
     */
    public static MaskType fromId(String id) {
        if (id == null || id.isEmpty()) return FULL;
        try {
            return valueOf(id.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return FULL;
        }
    }
}
