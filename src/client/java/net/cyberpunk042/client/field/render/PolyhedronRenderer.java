package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.PolyhedronTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.shape.PolyhedronShape;

/**
 * Renders polyhedron primitives (Platonic solids).
 * 
 * <p>Supports:
 * <ul>
 *   <li>Tetrahedron (4 faces)</li>
 *   <li>Cube (6 faces)</li>
 *   <li>Octahedron (8 faces)</li>
 *   <li>Dodecahedron (12 faces)</li>
 *   <li>Icosahedron (20 faces)</li>
 * </ul>
 * 
 * <p>Uses {@link PolyhedronTessellator}'s builder pattern for tessellation.</p>
 * 
 * @see PolyhedronShape
 * @see PolyhedronTessellator
 */
public final class PolyhedronRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "polyhedron";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive) {
        net.cyberpunk042.log.Logging.RENDER.topic("tessellate")
            .kv("primType", primitive.type())
            .kv("shapeClass", primitive.shape() != null ? primitive.shape().getClass().getSimpleName() : "null")
            .debug("[POLY] Entering tessellate");
        
        if (!(primitive.shape() instanceof PolyhedronShape shape)) {
            net.cyberpunk042.log.Logging.RENDER.topic("tessellate")
                .reason("Shape mismatch")
                .kv("expected", "PolyhedronShape")
                .kv("actual", primitive.shape() != null ? primitive.shape().getClass().getName() : "null")
                .warn("[POLY] Shape is not PolyhedronShape!");
            return null;
        }
        
        net.cyberpunk042.log.Logging.RENDER.topic("tessellate")
            .kv("polyType", shape.polyType())
            .kv("radius", shape.radius())
            .debug("[POLY] Tessellating polyhedron");
        
        // Use PolyhedronTessellator's builder pattern
        // Detail level 0 = no subdivision (raw Platonic solid)
        Mesh mesh = PolyhedronTessellator
            .fromShape(shape)
            .tessellate(0);
        
        net.cyberpunk042.log.Logging.RENDER.topic("tessellate")
            .kv("vertexCount", mesh != null ? mesh.vertexCount() : 0)
            .kv("isEmpty", mesh != null ? mesh.isEmpty() : true)
            .debug("[POLY] Tessellation result");
        
        return mesh;
    }
}
