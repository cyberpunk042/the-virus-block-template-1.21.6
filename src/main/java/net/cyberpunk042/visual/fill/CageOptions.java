package net.cyberpunk042.visual.fill;

import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;

/**
 * Base interface for cage rendering options.
 * 
 * <p>Cage mode renders a structured grid rather than all tessellation edges.
 * Each shape type has its own implementation with shape-specific options.</p>
 * 
 * <h2>Implementations</h2>
 * <ul>
 *   <li>{@link SphereCageOptions} - Latitude/longitude grid</li>
 *   <li>{@link PrismCageOptions} - Vertical lines and horizontal rings</li>
 *   <li>{@link CylinderCageOptions} - Vertical lines and horizontal rings</li>
 *   <li>{@link PolyhedronCageOptions} - Edges and face outlines</li>
 * </ul>
 * 
 * @see FillConfig
 * @see FillMode#CAGE
 */
public sealed interface CageOptions 
    permits SphereCageOptions, PrismCageOptions, CylinderCageOptions, PolyhedronCageOptions {
    
    /** Line width for cage rendering. */
    @Range(ValueRange.POSITIVE_NONZERO) 
    float lineWidth();
    
    /** Whether to show all structural edges. */
    boolean showEdges();
    
    /** Default line width. */
    float DEFAULT_LINE_WIDTH = 1.0f;
}
