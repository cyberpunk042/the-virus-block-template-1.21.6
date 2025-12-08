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
        if (!(primitive.shape() instanceof PolyhedronShape shape)) {
            return null;
        }
        
        // Use PolyhedronTessellator's builder pattern
        // Detail level 0 = no subdivision (raw Platonic solid)
        return PolyhedronTessellator
            .fromShape(shape)
            .tessellate(0);
    }
}
