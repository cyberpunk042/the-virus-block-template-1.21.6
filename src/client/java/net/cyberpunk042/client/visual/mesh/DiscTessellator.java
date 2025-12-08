package net.cyberpunk042.client.visual.mesh;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.pattern.SectorPattern;
import net.cyberpunk042.visual.shape.DiscShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Tessellates disc shapes into triangle meshes.
 * 
 * <h2>Geometry</h2>
 * <p>A disc is a filled circle, tessellated as triangular sectors from center.
 * Supports partial arcs (pac-man) and inner cutout (donut).</p>
 * 
 * <h2>Concentric Rings</h2>
 * <p>When rings > 1, creates concentric bands for more detailed patterns.</p>
 * 
 * @see DiscShape
 * @see SectorPattern
 * @see GeometryMath#discPoint
 */
public final class DiscTessellator {
    
    private DiscTessellator() {}
    
    /**
     * Tessellates a disc shape into a mesh.
     * 
     * @param shape The disc shape definition
     * @param pattern Vertex pattern (null = SectorPattern.DEFAULT)
     * @param visibility Visibility mask (null = all visible)
     * @return Generated mesh
     */
    public static Mesh tessellate(DiscShape shape, VertexPattern pattern,
                                   VisibilityMask visibility) {
        if (shape == null) {
            throw new IllegalArgumentException("DiscShape cannot be null");
        }
        
        // Use default pattern if none provided
        if (pattern == null) {
            pattern = SectorPattern.DEFAULT;
        }
        
        Logging.RENDER.topic("tessellate")
            .kv("shape", "disc")
            .kv("radius", shape.radius())
            .kv("segments", shape.segments())
            .debug("Tessellating disc");
        
        if (shape.hasHole()) {
            return tessellateDonut(shape, pattern, visibility);
        }
        return tessellateSolid(shape, pattern, visibility);
    }
    
    public static Mesh tessellate(DiscShape shape) {
        return tessellate(shape, null, null);
    }
    
    // =========================================================================
    // Solid Disc
    // =========================================================================
    
    private static Mesh tessellateSolid(DiscShape shape, VertexPattern pattern,
                                         VisibilityMask visibility) {
        MeshBuilder builder = MeshBuilder.triangles();
        
        int segments = shape.segments();
        float radius = shape.radius();
        float y = shape.y();
        float arcStart = GeometryMath.toRadians(shape.arcStart());
        float arcEnd = GeometryMath.toRadians(shape.arcEnd());
        float arcRange = arcEnd - arcStart;
        
        // Center vertex
        int centerIdx = builder.addVertex(GeometryMath.discCenter(y));
        
        // Edge vertices
        int[] edgeIndices = new int[segments + 1];
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float angle = arcStart + t * arcRange;
            edgeIndices[i] = builder.addVertex(GeometryMath.discPoint(angle, radius, y));
        }
        
        // Triangular sectors from center to edge
        for (int i = 0; i < segments; i++) {
            float segFrac = i / (float) segments;
            
            if (visibility != null && !visibility.isVisible(0.5f, segFrac)) {
                continue;
            }
            
            if (!pattern.shouldRender(i, segments)) {
                continue;
            }
            
            builder.triangle(centerIdx, edgeIndices[i], edgeIndices[i + 1]);
        }
        
        return builder.build();
    }
    
    // =========================================================================
    // Donut (with inner hole)
    // =========================================================================
    
    private static Mesh tessellateDonut(DiscShape shape, VertexPattern pattern,
                                         VisibilityMask visibility) {
        // Disc with inner hole is essentially a ring
        MeshBuilder builder = MeshBuilder.triangles();
        
        int segments = shape.segments();
        float outerR = shape.radius();
        float innerR = shape.innerRadius();
        float y = shape.y();
        float arcStart = GeometryMath.toRadians(shape.arcStart());
        float arcEnd = GeometryMath.toRadians(shape.arcEnd());
        float arcRange = arcEnd - arcStart;
        
        // Generate inner and outer edge vertices
        int[] innerIndices = new int[segments + 1];
        int[] outerIndices = new int[segments + 1];
        
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float angle = arcStart + t * arcRange;
            
            innerIndices[i] = builder.addVertex(GeometryMath.discPoint(angle, innerR, y));
            outerIndices[i] = builder.addVertex(GeometryMath.discPoint(angle, outerR, y));
        }
        
        // Quads between inner and outer
        for (int i = 0; i < segments; i++) {
            float segFrac = i / (float) segments;
            
            if (visibility != null && !visibility.isVisible(0.5f, segFrac)) {
                continue;
            }
            
            if (!pattern.shouldRender(i, segments)) {
                continue;
            }
            
            builder.triangle(innerIndices[i], outerIndices[i], outerIndices[i + 1]);
            builder.triangle(innerIndices[i], outerIndices[i + 1], innerIndices[i + 1]);
        }
        
        return builder.build();
    }
}
