package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.CylinderTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.CylinderShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Renders cylinder primitives.
 * 
 * <p>Passes arrangement pattern and visibility mask to tessellator
 * for proper cell filtering and vertex arrangement.</p>
 * 
 * @see CylinderShape
 * @see CylinderTessellator
 */
public final class CylinderRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "cylinder";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive) {
        if (!(primitive.shape() instanceof CylinderShape shape)) {
            return null;
        }
        
        // Get pattern from arrangement config with CellType validation
        VertexPattern pattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            // Validate pattern is compatible with cylinder's QUAD cells (sides)
            pattern = arrangement.resolvePattern("sides", shape.primaryCellType());
            if (pattern == null) {
                return null; // Mismatch logged to chat
            }
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        
        // Tessellate with full config
        return CylinderTessellator.tessellate(shape, pattern, visibility);
    }
}
