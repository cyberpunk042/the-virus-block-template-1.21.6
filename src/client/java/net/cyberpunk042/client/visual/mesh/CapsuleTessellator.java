package net.cyberpunk042.client.visual.mesh;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.QuadPattern;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.CapsuleShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Tessellates capsule shapes (cylinder with hemispherical caps) into triangle meshes.
 * 
 * <h2>Structure</h2>
 * <p>A capsule consists of three parts:</p>
 * <ul>
 *   <li>Top hemisphere (rings from pole to equator)</li>
 *   <li>Cylindrical body (if height > 2*radius)</li>
 *   <li>Bottom hemisphere (rings from equator to pole)</li>
 * </ul>
 * 
 * @see CapsuleShape
 */
public final class CapsuleTessellator {
    
    private CapsuleTessellator() {}
    
    /**
     * Tessellates a capsule shape into a mesh.
     * 
     * @param shape The capsule shape definition
     * @param pattern Optional vertex pattern (can be null)
     * @param visibility Optional visibility mask (can be null)
     * @return Generated mesh
     */
    public static Mesh tessellate(CapsuleShape shape, VertexPattern pattern,
                                   VisibilityMask visibility) {
        if (shape == null) {
            throw new IllegalArgumentException("CapsuleShape cannot be null");
        }
        
        if (pattern == null) {
            pattern = QuadPattern.FILLED_1;
        }
        
        Logging.RENDER.topic("tessellate")
            .kv("shape", "capsule")
            .kv("radius", shape.radius())
            .kv("height", shape.height())
            .kv("segments", shape.segments())
            .kv("rings", shape.rings())
            .debug("Tessellating capsule");
        
        MeshBuilder builder = MeshBuilder.triangles();
        
        float radius = shape.radius();
        float totalHeight = shape.height();
        float cylinderHeight = shape.cylinderHeight();
        int segments = shape.segments();
        int rings = shape.rings();
        
        // Y positions
        float topCapCenter = cylinderHeight / 2;
        float bottomCapCenter = -cylinderHeight / 2;
        
        // =================================================================
        // Top Hemisphere
        // =================================================================
        int topPoleIdx = builder.addVertex(new Vertex(0, topCapCenter + radius, 0, 0, 1, 0, 0.5f, 0));
        
        // Rings from near-pole to equator
        int[][] topHemiVerts = new int[rings][segments + 1];
        for (int ring = 0; ring < rings; ring++) {
            // theta goes from near 0 (pole) to PI/2 (equator)
            float theta = GeometryMath.HALF_PI * (ring + 1) / rings;
            float y = topCapCenter + radius * (float) Math.cos(theta);
            float ringRadius = radius * (float) Math.sin(theta);
            
            for (int seg = 0; seg <= segments; seg++) {
                float phi = GeometryMath.TWO_PI * seg / segments;
                float x = ringRadius * (float) Math.cos(phi);
                float z = ringRadius * (float) Math.sin(phi);
                
                // Normal points outward from hemisphere center
                float nx = (float) Math.sin(theta) * (float) Math.cos(phi);
                float ny = (float) Math.cos(theta);
                float nz = (float) Math.sin(theta) * (float) Math.sin(phi);
                
                float u = seg / (float) segments;
                float v = (ring + 1) / (float) (rings * 2 + (cylinderHeight > 0 ? 2 : 0));
                
                topHemiVerts[ring][seg] = builder.addVertex(new Vertex(x, y, z, nx, ny, nz, u, v));
            }
        }
        
        // Top pole triangles
        for (int seg = 0; seg < segments; seg++) {
            float segFrac = seg / (float) segments;
            if (visibility != null && !visibility.isVisible(0f, segFrac)) continue;
            if (!pattern.shouldRender(seg, segments)) continue;
            
            builder.triangle(topPoleIdx, topHemiVerts[0][seg], topHemiVerts[0][seg + 1]);
        }
        
        // Top hemisphere quads
        for (int ring = 0; ring < rings - 1; ring++) {
            for (int seg = 0; seg < segments; seg++) {
                float segFrac = seg / (float) segments;
                float ringFrac = ring / (float) rings;
                if (visibility != null && !visibility.isVisible(ringFrac, segFrac)) continue;
                if (!pattern.shouldRender(ring * segments + seg, rings * segments)) continue;
                
                int i0 = topHemiVerts[ring][seg];
                int i1 = topHemiVerts[ring][seg + 1];
                int i2 = topHemiVerts[ring + 1][seg + 1];
                int i3 = topHemiVerts[ring + 1][seg];
                
                builder.triangle(i0, i3, i2);
                builder.triangle(i0, i2, i1);
            }
        }
        
        // =================================================================
        // Cylinder Body (if present)
        // =================================================================
        int[] topRingVerts = topHemiVerts[rings - 1];  // Last ring of top hemisphere
        int[] bottomRingVerts;
        
        if (cylinderHeight > 0.001f) {
            // Create bottom ring of cylinder
            bottomRingVerts = new int[segments + 1];
            for (int seg = 0; seg <= segments; seg++) {
                float phi = GeometryMath.TWO_PI * seg / segments;
                float x = radius * (float) Math.cos(phi);
                float z = radius * (float) Math.sin(phi);
                
                float nx = (float) Math.cos(phi);
                float nz = (float) Math.sin(phi);
                
                float u = seg / (float) segments;
                float v = 0.5f;
                
                bottomRingVerts[seg] = builder.addVertex(new Vertex(x, bottomCapCenter, z, nx, 0, nz, u, v));
            }
            
            // Cylinder quads
            for (int seg = 0; seg < segments; seg++) {
                float segFrac = seg / (float) segments;
                if (visibility != null && !visibility.isVisible(0.5f, segFrac)) continue;
                if (!pattern.shouldRender(seg, segments)) continue;
                
                int i0 = topRingVerts[seg];
                int i1 = topRingVerts[seg + 1];
                int i2 = bottomRingVerts[seg + 1];
                int i3 = bottomRingVerts[seg];
                
                builder.triangle(i0, i3, i2);
                builder.triangle(i0, i2, i1);
            }
        } else {
            bottomRingVerts = topRingVerts;
        }
        
        // =================================================================
        // Bottom Hemisphere
        // =================================================================
        int[][] bottomHemiVerts = new int[rings][segments + 1];
        for (int ring = 0; ring < rings; ring++) {
            // theta goes from PI/2 (equator) to near PI (pole)
            float theta = GeometryMath.HALF_PI + GeometryMath.HALF_PI * (ring + 1) / rings;
            float y = bottomCapCenter + radius * (float) Math.cos(theta);
            float ringRadius = radius * (float) Math.abs(Math.sin(theta));
            
            for (int seg = 0; seg <= segments; seg++) {
                float phi = GeometryMath.TWO_PI * seg / segments;
                float x = ringRadius * (float) Math.cos(phi);
                float z = ringRadius * (float) Math.sin(phi);
                
                float nx = (float) Math.sin(theta) * (float) Math.cos(phi);
                float ny = (float) Math.cos(theta);
                float nz = (float) Math.sin(theta) * (float) Math.sin(phi);
                
                float u = seg / (float) segments;
                float v = 0.5f + (ring + 1) / (float) (rings * 2);
                
                bottomHemiVerts[ring][seg] = builder.addVertex(new Vertex(x, y, z, nx, ny, nz, u, v));
            }
        }
        
        // Connect bottom of cylinder/top hemisphere to bottom hemisphere
        for (int seg = 0; seg < segments; seg++) {
            float segFrac = seg / (float) segments;
            if (visibility != null && !visibility.isVisible(0.5f, segFrac)) continue;
            if (!pattern.shouldRender(seg, segments)) continue;
            
            int i0 = bottomRingVerts[seg];
            int i1 = bottomRingVerts[seg + 1];
            int i2 = bottomHemiVerts[0][seg + 1];
            int i3 = bottomHemiVerts[0][seg];
            
            builder.triangle(i0, i3, i2);
            builder.triangle(i0, i2, i1);
        }
        
        // Bottom hemisphere quads
        for (int ring = 0; ring < rings - 1; ring++) {
            for (int seg = 0; seg < segments; seg++) {
                float segFrac = seg / (float) segments;
                float ringFrac = (ring + rings) / (float) (rings * 2);
                if (visibility != null && !visibility.isVisible(ringFrac, segFrac)) continue;
                if (!pattern.shouldRender(ring * segments + seg, rings * segments)) continue;
                
                int i0 = bottomHemiVerts[ring][seg];
                int i1 = bottomHemiVerts[ring][seg + 1];
                int i2 = bottomHemiVerts[ring + 1][seg + 1];
                int i3 = bottomHemiVerts[ring + 1][seg];
                
                builder.triangle(i0, i3, i2);
                builder.triangle(i0, i2, i1);
            }
        }
        
        // Bottom pole triangles
        int bottomPoleIdx = builder.addVertex(new Vertex(0, bottomCapCenter - radius, 0, 0, -1, 0, 0.5f, 1));
        int[] lastBottomRing = bottomHemiVerts[rings - 1];
        
        for (int seg = 0; seg < segments; seg++) {
            float segFrac = seg / (float) segments;
            if (visibility != null && !visibility.isVisible(1f, segFrac)) continue;
            if (!pattern.shouldRender(seg, segments)) continue;
            
            builder.triangle(lastBottomRing[seg], bottomPoleIdx, lastBottomRing[seg + 1]);
        }
        
        Logging.RENDER.topic("tessellate")
            .kv("vertices", builder.vertexCount())
            .trace("Capsule tessellation complete");
        
        return builder.build();
    }
    
    public static Mesh tessellate(CapsuleShape shape) {
        return tessellate(shape, null, null);
    }
}


