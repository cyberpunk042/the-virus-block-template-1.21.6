package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.PrismTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.PrismShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Renders prism primitives.
 * 
 * <p>Passes arrangement pattern and visibility mask to tessellator
 * for proper cell filtering and vertex arrangement.</p>
 * 
 * @see PrismShape
 * @see PrismTessellator
 */
public final class PrismRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "prism";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive) {
        if (!(primitive.shape() instanceof PrismShape shape)) {
            return null;
        }
        
        // Get pattern from arrangement config with CellType validation
        VertexPattern pattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            // Validate pattern is compatible with prism's QUAD cells (sides)
            pattern = arrangement.resolvePattern("sides", shape.primaryCellType());
            // Don't fail on pattern mismatch - continue without pattern
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        
        // Tessellate with full config
        return PrismTessellator.tessellate(shape, pattern, visibility);
    }
}
