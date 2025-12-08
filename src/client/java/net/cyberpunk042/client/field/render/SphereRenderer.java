package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.SphereTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.SphereShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Renders sphere primitives.
 * 
 * <p>Passes arrangement pattern and visibility mask to tessellator
 * for proper cell filtering and vertex arrangement.</p>
 * 
 * @see SphereShape
 * @see SphereTessellator
 */
public final class SphereRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "sphere";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive) {
        if (!(primitive.shape() instanceof SphereShape shape)) {
            return null;
        }
        
        // Get pattern from arrangement config
        VertexPattern pattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            pattern = arrangement.resolvePattern("main");
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        
        // Tessellate with full config
        return SphereTessellator.tessellate(shape, pattern, visibility);
    }
}
