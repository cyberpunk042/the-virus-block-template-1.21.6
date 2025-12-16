package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.DiscTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.DiscShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Renders disc primitives.
 * 
 * <p>Passes arrangement pattern and visibility mask to tessellator
 * for proper cell filtering and vertex arrangement.</p>
 * 
 * @see DiscShape
 * @see DiscTessellator
 */
public final class DiscRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "disc";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, net.cyberpunk042.visual.animation.WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof DiscShape shape)) {
            return null;
        }
        
        // Get pattern from arrangement config with CellType validation
        VertexPattern pattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            // Try to resolve pattern - if it fails, continue without pattern
            pattern = arrangement.resolvePattern("surface", shape.primaryCellType());
            // Don't return null if pattern fails - render without pattern instead
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        
        // Tessellate with full config including wave
    return DiscTessellator.tessellate(shape, pattern, visibility, wave, time);
    }
}
