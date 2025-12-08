package net.cyberpunk042.visual.shape;

import net.minecraft.util.math.Box;

/**
 * Base interface for all field geometry shapes.
 * 
 * <h2>Shape Hierarchy</h2>
 * <pre>
 * Shape (sealed interface)
 * ├── SphereShape     - 3D sphere with lat/lon tessellation
 * ├── RingShape       - Horizontal ring/torus at Y level
 * ├── PrismShape      - Vertical prism with N sides
 * ├── PolyhedronShape - Platonic solids (cube, octahedron, icosahedron)
 * ├── DiscShape       - Flat circular disc
 * └── CylinderShape       - Vertical cylindrical beam
 * </pre>
 * 
 * <h2>Usage</h2>
 * <p>Shapes define geometry parameters. Use with a {@link net.cyberpunk042.client.visual.mesh.Tessellator}
 * to generate actual mesh data.
 * 
 * @see net.cyberpunk042.client.visual.mesh.Tessellator
 */
public sealed interface Shape permits 
        SphereShape, RingShape, PrismShape, PolyhedronShape, DiscShape, CylinderShape {
    
    /**
     * Returns the shape type identifier.
     * <p>Used for serialization and renderer lookup.
     * 
     * @return type string (e.g., "sphere", "ring", "prism")
     */
    String getType();
    
    /**
     * Returns the bounding box for this shape.
     * <p>Used for culling and collision detection.
     * 
     * @return axis-aligned bounding box
     */
    Box getBounds();
    
    /**
     * Returns estimated vertex count for this shape.
     * <p>Used for pre-allocating buffers.
     */
    default int estimateVertexCount() {
        return 64; // Default estimate
    }
    
    /**
     * Returns estimated triangle count for this shape.
     */
    default int estimateTriangleCount() {
        return 32; // Default estimate
    }
}
