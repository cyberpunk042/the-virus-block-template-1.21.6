package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.CylinderTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.CellType;
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
    protected Mesh tessellate(Primitive primitive, net.cyberpunk042.visual.animation.WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof CylinderShape shape)) {
            return null;
        }
        
        // Get separate patterns for sides and caps
        VertexPattern sidesPattern = null;
        VertexPattern capPattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            // Sides use QUAD cells
            sidesPattern = arrangement.resolvePattern("sides", CellType.QUAD);
            // Caps use SECTOR cells
            capPattern = arrangement.resolvePattern("capTop", CellType.SECTOR);
            
            Logging.RENDER.topic("tessellate")
                .kv("sidesPattern", sidesPattern != null ? sidesPattern.toString() : "null")
                .kv("capPattern", capPattern != null ? capPattern.toString() : "null")
                .debug("[CYLINDER] Resolved per-part patterns");
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        
        // Tessellate with separate patterns for sides and caps
    return CylinderTessellator.tessellate(shape, sidesPattern, capPattern, visibility, wave, time);
    }
}
