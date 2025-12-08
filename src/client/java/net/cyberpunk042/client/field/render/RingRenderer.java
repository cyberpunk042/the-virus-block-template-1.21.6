package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.RingTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.RingShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Renders ring primitives.
 * 
 * <p>Passes arrangement pattern and visibility mask to tessellator
 * for proper cell filtering and vertex arrangement.</p>
 * 
 * @see RingShape
 * @see RingTessellator
 */
public final class RingRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "ring";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive) {
        if (!(primitive.shape() instanceof RingShape shape)) {
            return null;
        }
        
        // Get pattern from arrangement config
        VertexPattern pattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            pattern = arrangement.resolvePattern("surface");
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        
        // Tessellate with full config
        return RingTessellator.tessellate(shape, pattern, visibility);
    }
}
