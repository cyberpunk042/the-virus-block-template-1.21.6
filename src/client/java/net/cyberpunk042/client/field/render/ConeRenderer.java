package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.ConeTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.ConeShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Renders cone and frustum primitives.
 * 
 * @see ConeShape
 * @see ConeTessellator
 */
public final class ConeRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "cone";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, net.cyberpunk042.visual.animation.WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof ConeShape shape)) {
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
        
        return ConeTessellator.tessellate(shape, pattern, visibility);
    }
}


