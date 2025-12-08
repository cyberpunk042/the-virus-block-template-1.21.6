package net.cyberpunk042.client.visual.mesh;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.pattern.SegmentPattern;
import net.cyberpunk042.visual.shape.RingShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Tessellates ring shapes into triangle meshes.
 * 
 * <h2>Geometry</h2>
 * <p>A ring is a flat band with inner and outer radii, positioned at a Y height.
 * Each segment is a quad connecting inner and outer edges.</p>
 * 
 * <h2>Partial Arcs</h2>
 * <p>Supports partial arcs via arcStart/arcEnd (degrees).</p>
 * 
 * <h2>3D Rings</h2>
 * <p>When height > 0, creates a 3D ring (tube section) with:</p>
 * <ul>
 *   <li>Top ring face</li>
 *   <li>Bottom ring face</li>
 *   <li>Outer cylinder wall</li>
 *   <li>Inner cylinder wall</li>
 * </ul>
 * <p>Ring is centered at y (extends from y-height/2 to y+height/2).</p>
 * 
 * <h2>Twist</h2>
 * <p>When twist != 0, the top is rotated relative to bottom (Möbius-like effect).</p>
 * 
 * @see RingShape
 * @see SegmentPattern
 * @see GeometryMath#ringPoint
 */
public final class RingTessellator {
    
    private RingTessellator() {}
    
    /**
     * Tessellates a ring shape into a mesh.
     * 
     * @param shape The ring shape definition
     * @param pattern Vertex pattern (null = SegmentPattern.DEFAULT)
     * @param visibility Visibility mask (null = all visible)
     * @return Generated mesh
     */
    public static Mesh tessellate(RingShape shape, VertexPattern pattern,
                                   VisibilityMask visibility) {
        if (shape == null) {
            throw new IllegalArgumentException("RingShape cannot be null");
        }
        
        // Use default pattern if none provided
        if (pattern == null) {
            pattern = SegmentPattern.DEFAULT;
        }
        
        Logging.RENDER.topic("tessellate")
            .kv("shape", "ring")
            .kv("inner", shape.innerRadius())
            .kv("outer", shape.outerRadius())
            .kv("segments", shape.segments())
            .kv("height", shape.height())
            .debug("Tessellating ring");
        
        if (shape.height() > 0) {
            return tessellate3DRing(shape, pattern, visibility);
        }
        return tessellateFlatRing(shape, pattern, visibility);
    }
    
    public static Mesh tessellate(RingShape shape) {
        return tessellate(shape, null, null);
    }
    
    // =========================================================================
    // Flat Ring (height = 0)
    // =========================================================================
    
    private static Mesh tessellateFlatRing(RingShape shape, VertexPattern pattern,
                                            VisibilityMask visibility) {
        MeshBuilder builder = MeshBuilder.triangles();
        
        int segments = shape.segments();
        float innerR = shape.innerRadius();
        float outerR = shape.outerRadius();
        float y = shape.y();
        float arcStart = GeometryMath.toRadians(shape.arcStart());
        float arcEnd = GeometryMath.toRadians(shape.arcEnd());
        float arcRange = arcEnd - arcStart;
        
        // Generate vertices for inner and outer edges
        int[] innerIndices = new int[segments + 1];
        int[] outerIndices = new int[segments + 1];
        
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float angle = arcStart + t * arcRange;
            
            Vertex inner = GeometryMath.ringInnerPoint(angle, innerR, y);
            Vertex outer = GeometryMath.ringOuterPoint(angle, outerR, y);
            
            innerIndices[i] = builder.addVertex(inner);
            outerIndices[i] = builder.addVertex(outer);
        }
        
        // Generate quads between inner and outer edges
        for (int i = 0; i < segments; i++) {
            float segFrac = i / (float) segments;
            
            // Check visibility
            if (visibility != null && !visibility.isVisible(0.5f, segFrac)) {
                continue;
            }
            
            // Check pattern
            if (!pattern.shouldRender(i, segments)) {
                continue;
            }
            
            // Quad: inner[i], inner[i+1], outer[i+1], outer[i]
            int i0 = innerIndices[i];
            int i1 = innerIndices[i + 1];
            int o0 = outerIndices[i];
            int o1 = outerIndices[i + 1];
            
            builder.triangle(i0, o0, o1);
            builder.triangle(i0, o1, i1);
        }
        
        return builder.build();
    }
    
    // =========================================================================
    // 3D Ring (height > 0)
    // =========================================================================
    
    /**
     * Tessellates a 3D ring (tube section) with 4 surfaces:
     * - Top face
     * - Bottom face
     * - Outer wall
     * - Inner wall
     */
    private static Mesh tessellate3DRing(RingShape shape, VertexPattern pattern,
                                          VisibilityMask visibility) {
        MeshBuilder builder = MeshBuilder.triangles();
        
        int segments = shape.segments();
        float innerR = shape.innerRadius();
        float outerR = shape.outerRadius();
        float height = shape.height();
        float yCenter = shape.y();
        float yBottom = yCenter - height / 2;
        float yTop = yCenter + height / 2;
        float twist = GeometryMath.toRadians(shape.twist());
        float arcStart = GeometryMath.toRadians(shape.arcStart());
        float arcEnd = GeometryMath.toRadians(shape.arcEnd());
        float arcRange = arcEnd - arcStart;
        boolean isFullRing = shape.isFullRing();
        
        // =====================================================================
        // Generate all vertices
        // =====================================================================
        
        // Bottom ring vertices (inner and outer)
        int[] bottomInner = new int[segments + 1];
        int[] bottomOuter = new int[segments + 1];
        
        // Top ring vertices (inner and outer) - with twist applied
        int[] topInner = new int[segments + 1];
        int[] topOuter = new int[segments + 1];
        
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float baseAngle = arcStart + t * arcRange;
            float topAngle = baseAngle + twist;  // Apply twist to top
            
            // Bottom vertices (normal pointing down for bottom face)
            bottomInner[i] = builder.addVertex(ringVertex(baseAngle, innerR, yBottom, 0, -1, 0));
            bottomOuter[i] = builder.addVertex(ringVertex(baseAngle, outerR, yBottom, 0, -1, 0));
            
            // Top vertices (normal pointing up for top face)
            topInner[i] = builder.addVertex(ringVertex(topAngle, innerR, yTop, 0, 1, 0));
            topOuter[i] = builder.addVertex(ringVertex(topAngle, outerR, yTop, 0, 1, 0));
        }
        
        // =====================================================================
        // Top and Bottom Ring Faces
        // =====================================================================
        
        for (int i = 0; i < segments; i++) {
            float segFrac = i / (float) segments;
            
            if (visibility != null && !visibility.isVisible(0.5f, segFrac)) {
                continue;
            }
            if (!pattern.shouldRender(i, segments)) {
                continue;
            }
            
            // Top face (CCW when viewed from above)
            builder.triangle(topInner[i], topOuter[i], topOuter[i + 1]);
            builder.triangle(topInner[i], topOuter[i + 1], topInner[i + 1]);
            
            // Bottom face (CCW when viewed from below = CW from above)
            builder.triangle(bottomInner[i], bottomOuter[i + 1], bottomOuter[i]);
            builder.triangle(bottomInner[i], bottomInner[i + 1], bottomOuter[i + 1]);
        }
        
        // =====================================================================
        // Outer Wall (connects outer edges of top and bottom)
        // =====================================================================
        
        // Need separate vertices with outward-facing normals
        int[] outerWallBottom = new int[segments + 1];
        int[] outerWallTop = new int[segments + 1];
        
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float baseAngle = arcStart + t * arcRange;
            float topAngle = baseAngle + twist;
            
            // Normal points outward (radial direction)
            float nx = (float) Math.cos(baseAngle);
            float nz = (float) Math.sin(baseAngle);
            outerWallBottom[i] = builder.addVertex(ringVertex(baseAngle, outerR, yBottom, nx, 0, nz));
            
            float nxTop = (float) Math.cos(topAngle);
            float nzTop = (float) Math.sin(topAngle);
            outerWallTop[i] = builder.addVertex(ringVertex(topAngle, outerR, yTop, nxTop, 0, nzTop));
        }
        
        for (int i = 0; i < segments; i++) {
            float segFrac = i / (float) segments;
            if (visibility != null && !visibility.isVisible(1.0f, segFrac)) continue;
            if (!pattern.shouldRender(i, segments)) continue;
            
            // Outer wall quad (CCW when viewed from outside)
            builder.triangle(outerWallBottom[i], outerWallTop[i], outerWallTop[i + 1]);
            builder.triangle(outerWallBottom[i], outerWallTop[i + 1], outerWallBottom[i + 1]);
        }
        
        // =====================================================================
        // Inner Wall (connects inner edges of top and bottom)
        // =====================================================================
        
        // Need separate vertices with inward-facing normals
        int[] innerWallBottom = new int[segments + 1];
        int[] innerWallTop = new int[segments + 1];
        
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float baseAngle = arcStart + t * arcRange;
            float topAngle = baseAngle + twist;
            
            // Normal points inward (negative radial direction)
            float nx = -(float) Math.cos(baseAngle);
            float nz = -(float) Math.sin(baseAngle);
            innerWallBottom[i] = builder.addVertex(ringVertex(baseAngle, innerR, yBottom, nx, 0, nz));
            
            float nxTop = -(float) Math.cos(topAngle);
            float nzTop = -(float) Math.sin(topAngle);
            innerWallTop[i] = builder.addVertex(ringVertex(topAngle, innerR, yTop, nxTop, 0, nzTop));
        }
        
        for (int i = 0; i < segments; i++) {
            float segFrac = i / (float) segments;
            if (visibility != null && !visibility.isVisible(0.0f, segFrac)) continue;
            if (!pattern.shouldRender(i, segments)) continue;
            
            // Inner wall quad (CCW when viewed from inside = reversed winding)
            builder.triangle(innerWallBottom[i], innerWallTop[i + 1], innerWallTop[i]);
            builder.triangle(innerWallBottom[i], innerWallBottom[i + 1], innerWallTop[i + 1]);
        }
        
        // =====================================================================
        // Arc End Caps (only if not a full ring)
        // =====================================================================
        
        if (!isFullRing) {
            // Start cap (at arcStart angle)
            tessellateArcEndCap(builder, arcStart, innerR, outerR, yBottom, yTop, true);
            
            // End cap (at arcEnd angle + twist)
            float endAngle = arcEnd + twist;
            tessellateArcEndCap(builder, endAngle, innerR, outerR, yBottom, yTop, false);
        }
        
        Logging.RENDER.topic("tessellate")
            .kv("vertices", builder.vertexCount())
            .trace("3D ring tessellation complete");
        
        return builder.build();
    }
    
    /**
     * Creates a vertex at a ring position with custom normal.
     */
    private static Vertex ringVertex(float angle, float radius, float y, 
                                      float nx, float ny, float nz) {
        float x = (float) Math.cos(angle) * radius;
        float z = (float) Math.sin(angle) * radius;
        float u = angle / GeometryMath.TWO_PI;
        float v = (ny > 0) ? 1 : 0;  // Top = 1, bottom = 0, walls use angle
        return new Vertex(x, y, z, nx, ny, nz, u, v);
    }
    
    /**
     * Tessellates an end cap for a partial arc (rectangular face).
     * 
     * @param builder Mesh builder
     * @param angle Angle of the end cap
     * @param innerR Inner radius
     * @param outerR Outer radius
     * @param yBottom Bottom Y
     * @param yTop Top Y
     * @param isStart true for start cap (normal points backward), false for end cap
     */
    private static void tessellateArcEndCap(MeshBuilder builder, float angle,
                                             float innerR, float outerR,
                                             float yBottom, float yTop, boolean isStart) {
        // Normal perpendicular to the arc at this angle
        // For start cap: points in -tangent direction
        // For end cap: points in +tangent direction
        float tangentAngle = angle + (isStart ? -GeometryMath.HALF_PI : GeometryMath.HALF_PI);
        float nx = (float) Math.cos(tangentAngle);
        float nz = (float) Math.sin(tangentAngle);
        
        // Four corners of the rectangular cap
        float cosA = (float) Math.cos(angle);
        float sinA = (float) Math.sin(angle);
        
        Vertex innerBottom = new Vertex(cosA * innerR, yBottom, sinA * innerR, nx, 0, nz, 0, 0);
        Vertex outerBottom = new Vertex(cosA * outerR, yBottom, sinA * outerR, nx, 0, nz, 1, 0);
        Vertex innerTop = new Vertex(cosA * innerR, yTop, sinA * innerR, nx, 0, nz, 0, 1);
        Vertex outerTop = new Vertex(cosA * outerR, yTop, sinA * outerR, nx, 0, nz, 1, 1);
        
        int iIB = builder.addVertex(innerBottom);
        int iOB = builder.addVertex(outerBottom);
        int iIT = builder.addVertex(innerTop);
        int iOT = builder.addVertex(outerTop);
        
        // Two triangles for the quad
        if (isStart) {
            // Start cap faces backward along arc
            builder.triangle(iIB, iOB, iOT);
            builder.triangle(iIB, iOT, iIT);
        } else {
            // End cap faces forward along arc
            builder.triangle(iIB, iOT, iOB);
            builder.triangle(iIB, iIT, iOT);
        }
    }
}
