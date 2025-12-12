package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.TorusTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.TorusShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Renders torus (donut) primitives.
 * 
 * @see TorusShape
 * @see TorusTessellator
 */
public final class TorusRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "torus";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive) {
        if (!(primitive.shape() instanceof TorusShape shape)) {
            return null;
        }
        
        // Get pattern from arrangement config
        VertexPattern pattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            pattern = arrangement.resolvePattern("surface", shape.primaryCellType());
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        
        return TorusTessellator.tessellate(shape, pattern, visibility);
    }
}


