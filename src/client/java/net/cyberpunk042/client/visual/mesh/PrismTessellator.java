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
     * Tessellates a prism shape into a mesh with separate patterns for parts.
     * 
     * @param shape The prism shape
     * @param sidesPattern Pattern for side walls (QUAD)
     * @param capPattern Pattern for top/bottom caps (SECTOR)
     * @param visibility Visibility mask
     */
    public static Mesh tessellate(PrismShape shape, VertexPattern sidesPattern,
                                   VertexPattern capPattern, VisibilityMask visibility) {
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
        
        // === SIDE FACES (uses sidesPattern) ===
        tessellateSides(builder, sides, bottomR, topR, height, twist, heightSegs, 
                        yBase, sidesPattern, visibility);
        
        // === CAPS (uses capPattern) ===
        if (shape.capBottom()) {
            tessellateCap(builder, sides, bottomR, yBase, false, twist, height, yBase, capPattern);
        }
        if (shape.capTop()) {
            tessellateCap(builder, sides, topR, yTop, true, twist, height, yBase, capPattern);
        }
        
        return builder.build();
    }
    
    /**
     * Tessellates with single pattern for all parts (legacy compatibility).
     */
    public static Mesh tessellate(PrismShape shape, VertexPattern pattern,
                                   VisibilityMask visibility) {
        return tessellate(shape, pattern, pattern, visibility);
    }
    
    public static Mesh tessellate(PrismShape shape) {
        return tessellate(shape, null, null, null);
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
                
                // Indices: i00=BL(bot,s), i10=BR(bot,s+1), i01=TL(top,s), i11=TR(top,s+1)
                // Use quadAsTrianglesFromPattern for pattern support
                // TL=i01, TR=i11, BR=i10, BL=i00
                builder.quadAsTrianglesFromPattern(i01, i11, i10, i00, pattern);
            }
        }
    }
    
    // =========================================================================
    // Caps (Top/Bottom)
    // =========================================================================
    
    private static void tessellateCap(MeshBuilder builder, int sides, float radius,
                                       float y, boolean isTop, float twist, 
                                       float height, float yBase, VertexPattern pattern) {
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
        
        // Triangulate from center using pattern
        for (int s = 0; s < sides; s++) {
            // Check pattern visibility for this sector
            if (pattern != null && !pattern.shouldRender(s, sides)) {
                continue;
            }
            
            int next = (s + 1) % sides;
            
            // Build cell vertices array: [center, edge0, edge1]
            // Winding order explanation:
            // - prismCorner generates vertices CCW when viewed from BELOW (-Y)
            // - For TOP cap (viewed from +Y), we need CCW from above
            // - Therefore: center → edge[next] → edge[s] (reverse order)
            // - For BOTTOM cap (viewed from -Y), CCW from below is what prismCorner gives
            // - Therefore: center → edge[s] → edge[next] (natural order)
            int[] cellVertices;
            if (isTop) {
                // Top cap: reverse vertex order for CCW from above
                cellVertices = new int[] { centerIdx, edgeIndices[next], edgeIndices[s] };
            } else {
                // Bottom cap: natural order for CCW from below
                cellVertices = new int[] { centerIdx, edgeIndices[s], edgeIndices[next] };
            }
            
            // Use generic pattern-based emission
            builder.emitCellFromPattern(cellVertices, pattern);
        }
    }
}
