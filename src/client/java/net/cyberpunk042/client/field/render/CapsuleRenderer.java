package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.CapsuleTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.CapsuleShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Renders capsule (cylinder with hemispherical caps) primitives.
 * 
 * @see CapsuleShape
 * @see CapsuleTessellator
 */
public final class CapsuleRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "capsule";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, net.cyberpunk042.visual.animation.WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof CapsuleShape shape)) {
            return null;
        }
        
        // Get pattern from arrangement config
        VertexPattern pattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            pattern = arrangement.resolvePattern("cylinder", shape.primaryCellType());
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        
        return CapsuleTessellator.tessellate(shape, pattern, visibility);
    }
}


