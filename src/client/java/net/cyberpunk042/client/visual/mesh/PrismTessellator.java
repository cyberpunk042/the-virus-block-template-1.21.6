package net.cyberpunk042.client.visual.mesh;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.PrismShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Tessellates prism shapes into triangle meshes.
 * 
 * <h2>Geometry</h2>
 * <p>A prism is an extruded N-sided polygon. Consists of:</p>
 * <ul>
 *   <li>N side faces (quads)</li>
 *   <li>Top cap (N-gon, optional)</li>
 *   <li>Bottom cap (N-gon, optional)</li>
 * </ul>
 * 
 * <h2>Twist</h2>
 * <p>When twist != 0, the top is rotated relative to bottom.</p>
 * 
 * <h2>Taper</h2>
 * <p>When topRadius != radius, creates a pyramid-like shape.</p>
 * 
 * @see PrismShape
 * @see GeometryMath#prismCorner
 */
public final class PrismTessellator {
    
    private PrismTessellator() {}
    
    /**
     * Tessellates a prism shape into a mesh.
     */
    public static Mesh tessellate(PrismShape shape, VertexPattern pattern,
                                   VisibilityMask visibility) {
        if (shape == null) {
            throw new IllegalArgumentException("PrismShape cannot be null");
        }
        
        Logging.RENDER.topic("tessellate")
            .kv("shape", "prism")
            .kv("sides", shape.sides())
            .kv("radius", shape.radius())
            .kv("height", shape.height())
            .debug("Tessellating prism");
        
        MeshBuilder builder = MeshBuilder.triangles();
        
        int sides = shape.sides();
        float bottomR = shape.radius();
        float topR = shape.topRadius();
        float height = shape.height();
        float twist = GeometryMath.toRadians(shape.twist());
        int heightSegs = shape.heightSegments();
        
        float yBase = -height / 2;
        float yTop = height / 2;
        
        // === SIDE FACES ===
        tessellateSides(builder, sides, bottomR, topR, height, twist, heightSegs, 
                        yBase, pattern, visibility);
        
        // === CAPS ===
        if (shape.capBottom()) {
            tessellateCap(builder, sides, bottomR, yBase, false, twist, height, yBase);
        }
        if (shape.capTop()) {
            tessellateCap(builder, sides, topR, yTop, true, twist, height, yBase);
        }
        
        return builder.build();
    }
    
    public static Mesh tessellate(PrismShape shape) {
        return tessellate(shape, null, null);
    }
    
    // =========================================================================
    // Side Faces
    // =========================================================================
    
    private static void tessellateSides(MeshBuilder builder, int sides, 
                                         float bottomR, float topR, float height,
                                         float twist, int heightSegs,
                                         float yBase, VertexPattern pattern,
                                         VisibilityMask visibility) {
        // For each height segment
        for (int h = 0; h < heightSegs; h++) {
            float t0 = h / (float) heightSegs;
            float t1 = (h + 1) / (float) heightSegs;
            float y0 = yBase + t0 * height;
            float y1 = yBase + t1 * height;
            float r0 = GeometryMath.lerp(bottomR, topR, t0);
            float r1 = GeometryMath.lerp(bottomR, topR, t1);
            
            // For each side
            for (int s = 0; s < sides; s++) {
                float sideFrac = s / (float) sides;
                
                if (visibility != null && !visibility.isVisible(t0, sideFrac)) {
                    continue;
                }
                
                if (pattern != null && !pattern.shouldRender(h * sides + s, heightSegs * sides)) {
                    continue;
                }
                
                // Four corners of this quad
                Vertex v00 = GeometryMath.prismCorner(s, sides, y0, r0, twist, height, yBase);
                Vertex v10 = GeometryMath.prismCorner(s + 1, sides, y0, r0, twist, height, yBase);
                Vertex v01 = GeometryMath.prismCorner(s, sides, y1, r1, twist, height, yBase);
                Vertex v11 = GeometryMath.prismCorner(s + 1, sides, y1, r1, twist, height, yBase);
                
                int i00 = builder.addVertex(v00);
                int i10 = builder.addVertex(v10);
                int i01 = builder.addVertex(v01);
                int i11 = builder.addVertex(v11);
                
                // Two triangles per quad
                builder.triangle(i00, i10, i11);
                builder.triangle(i00, i11, i01);
            }
        }
    }
    
    // =========================================================================
    // Caps (Top/Bottom)
    // =========================================================================
    
    private static void tessellateCap(MeshBuilder builder, int sides, float radius,
                                       float y, boolean isTop, float twist, 
                                       float height, float yBase) {
        // Center vertex
        Vertex center = GeometryMath.prismFaceCenter(sides, y, radius);
        int centerIdx = builder.addVertex(center);
        
        // Edge vertices
        int[] edgeIndices = new int[sides];
        for (int s = 0; s < sides; s++) {
            Vertex v = GeometryMath.prismCorner(s, sides, y, radius, twist, height, yBase);
            // Adjust normal for cap (up or down)
            v = new Vertex(v.x(), v.y(), v.z(), 0, isTop ? 1 : -1, 0, v.u(), v.v());
            edgeIndices[s] = builder.addVertex(v);
        }
        
        // Triangulate from center
        for (int s = 0; s < sides; s++) {
            int next = (s + 1) % sides;
            if (isTop) {
                builder.triangle(centerIdx, edgeIndices[s], edgeIndices[next]);
            } else {
                builder.triangle(centerIdx, edgeIndices[next], edgeIndices[s]);
            }
        }
    }
}
