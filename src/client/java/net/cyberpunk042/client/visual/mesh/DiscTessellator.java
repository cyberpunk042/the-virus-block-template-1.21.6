package net.cyberpunk042.client.visual.mesh;

import net.cyberpunk042.client.visual.animation.WaveDeformer;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.WaveConfig;
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
     * Tessellates a disc shape into a mesh with optional wave deformation.
     * 
     * @param shape The disc shape definition
     * @param pattern Vertex pattern (null = SectorPattern.DEFAULT)
     * @param visibility Visibility mask (null = all visible)
     * @param wave Wave configuration for CPU deformation (null = no wave)
     * @param time Current time for wave animation
     * @return Generated mesh
     */
    public static Mesh tessellate(DiscShape shape, VertexPattern pattern,
                                   VisibilityMask visibility,
                                   WaveConfig wave, float time) {
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
            .kv("wave", wave != null && wave.isActive())
            .debug("Tessellating disc");
        
        if (shape.hasHole()) {
            return tessellateDonut(shape, pattern, visibility, wave, time);
        }
        return tessellateSolid(shape, pattern, visibility, wave, time);
    }
    
    /**
     * Tessellates a disc shape into a mesh (backward compatible).
     */
    public static Mesh tessellate(DiscShape shape, VertexPattern pattern,
                                   VisibilityMask visibility) {
        return tessellate(shape, pattern, visibility, null, 0);
    }
    
    public static Mesh tessellate(DiscShape shape) {
        return tessellate(shape, null, null, null, 0);
    }
    
    // =========================================================================
    // Solid Disc
    // =========================================================================
    
    private static Mesh tessellateSolid(DiscShape shape, VertexPattern pattern,
                                         VisibilityMask visibility,
                                         WaveConfig wave, float time) {
        MeshBuilder builder = MeshBuilder.triangles();
        
        int segments = shape.segments();
        float radius = shape.radius();
        float y = shape.y();
        float arcStart = GeometryMath.toRadians(shape.arcStart());
        float arcEnd = GeometryMath.toRadians(shape.arcEnd());
        float arcRange = arcEnd - arcStart;
        
        boolean applyWave = wave != null && wave.isActive() && wave.isCpuMode();
        
        // Center vertex
        Vertex center = GeometryMath.discCenter(y);
        if (applyWave) {
            center = WaveDeformer.applyToVertex(center, wave, time);
        }
        int centerIdx = builder.addVertex(center);
        
        // Edge vertices
        int[] edgeIndices = new int[segments + 1];
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float angle = arcStart + t * arcRange;
            Vertex v = GeometryMath.discPoint(angle, radius, y);
            if (applyWave) {
                v = WaveDeformer.applyToVertex(v, wave, time);
            }
            edgeIndices[i] = builder.addVertex(v);
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
            
            // Build cell vertices: [center, edge0, edge1]
            // Winding order: discPoint generates vertices CCW from BELOW (-Y)
            // Disc normal points UP (+Y), so we view from above
            // Therefore: reverse order for CCW from above: center → edge[i+1] → edge[i]
            int[] cellVertices = { centerIdx, edgeIndices[i + 1], edgeIndices[i] };
            builder.emitCellFromPattern(cellVertices, pattern);
        }
        
        return builder.build();
    }
    
    // =========================================================================
    // Donut (with inner hole)
    // =========================================================================
    
    private static Mesh tessellateDonut(DiscShape shape, VertexPattern pattern,
                                         VisibilityMask visibility,
                                         WaveConfig wave, float time) {
        // Disc with inner hole is essentially a ring
        MeshBuilder builder = MeshBuilder.triangles();
        
        int segments = shape.segments();
        float outerR = shape.radius();
        float innerR = shape.innerRadius();
        float y = shape.y();
        float arcStart = GeometryMath.toRadians(shape.arcStart());
        float arcEnd = GeometryMath.toRadians(shape.arcEnd());
        float arcRange = arcEnd - arcStart;
        
        boolean applyWave = wave != null && wave.isActive() && wave.isCpuMode();
        
        // Generate inner and outer edge vertices
        int[] innerIndices = new int[segments + 1];
        int[] outerIndices = new int[segments + 1];
        
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float angle = arcStart + t * arcRange;
            
            Vertex inner = GeometryMath.discPoint(angle, innerR, y);
            Vertex outer = GeometryMath.discPoint(angle, outerR, y);
            
            if (applyWave) {
                inner = WaveDeformer.applyToVertex(inner, wave, time);
                outer = WaveDeformer.applyToVertex(outer, wave, time);
            }
            
            innerIndices[i] = builder.addVertex(inner);
            outerIndices[i] = builder.addVertex(outer);
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
            
            // Donut quad - CCW when viewed from above
            // inner[i] → inner[i+1] → outer[i+1], and inner[i] → outer[i+1] → outer[i]
            builder.triangle(innerIndices[i], innerIndices[i + 1], outerIndices[i + 1]);
            builder.triangle(innerIndices[i], outerIndices[i + 1], outerIndices[i]);
        }
        
        return builder.build();
    }
}
