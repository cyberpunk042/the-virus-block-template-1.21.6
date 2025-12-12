package net.cyberpunk042.client.visual.mesh;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.QuadPattern;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.ConeShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Tessellates cone shapes into triangle meshes.
 * 
 * <h2>Structure</h2>
 * <p>Supports both true cones (pointed top) and frustums (flat top).</p>
 * <ul>
 *   <li>True cone: triangles from base to apex</li>
 *   <li>Frustum: quads from bottom circle to top circle</li>
 * </ul>
 * 
 * @see ConeShape
 */
public final class ConeTessellator {
    
    private ConeTessellator() {}
    
    /**
     * Tessellates a cone shape into a mesh.
     * 
     * @param shape The cone shape definition
     * @param pattern Optional vertex pattern (can be null)
     * @param visibility Optional visibility mask (can be null)
     * @return Generated mesh
     */
    public static Mesh tessellate(ConeShape shape, VertexPattern pattern,
                                   VisibilityMask visibility) {
        if (shape == null) {
            throw new IllegalArgumentException("ConeShape cannot be null");
        }
        
        if (pattern == null) {
            pattern = QuadPattern.FILLED_1;
        }
        
        Logging.RENDER.topic("tessellate")
            .kv("shape", "cone")
            .kv("bottomR", shape.bottomRadius())
            .kv("topR", shape.topRadius())
            .kv("height", shape.height())
            .kv("segments", shape.segments())
            .debug("Tessellating cone");
        
        MeshBuilder builder = MeshBuilder.triangles();
        
        float bottomR = shape.bottomRadius();
        float topR = shape.topRadius();
        float height = shape.height();
        int segments = shape.segments();
        boolean isPointed = shape.isPointed();
        
        float halfHeight = height / 2;
        float bottomY = -halfHeight;
        float topY = halfHeight;
        
        // Calculate slant for normals
        float slantAngle = (float) Math.atan2(bottomR - topR, height);
        float normalY = (float) Math.cos(slantAngle);
        float normalXZScale = (float) Math.sin(slantAngle);
        
        // =================================================================
        // Generate vertices
        // =================================================================
        
        // Bottom ring vertices
        int[] bottomVerts = new int[segments + 1];
        for (int seg = 0; seg <= segments; seg++) {
            float phi = GeometryMath.TWO_PI * seg / segments;
            float x = bottomR * (float) Math.cos(phi);
            float z = bottomR * (float) Math.sin(phi);
            
            // Normal points outward along slant
            float nx = normalXZScale * (float) Math.cos(phi);
            float ny = normalY;
            float nz = normalXZScale * (float) Math.sin(phi);
            
            float u = seg / (float) segments;
            bottomVerts[seg] = builder.addVertex(new Vertex(x, bottomY, z, nx, ny, nz, u, 1));
        }
        
        if (isPointed) {
            // =============================================================
            // True Cone - triangles to apex
            // =============================================================
            int apexIdx = builder.addVertex(new Vertex(0, topY, 0, 0, 1, 0, 0.5f, 0));
            
            for (int seg = 0; seg < segments; seg++) {
                float segFrac = seg / (float) segments;
                if (visibility != null && !visibility.isVisible(0.5f, segFrac)) continue;
                if (!pattern.shouldRender(seg, segments)) continue;
                
                builder.triangle(bottomVerts[seg], apexIdx, bottomVerts[seg + 1]);
            }
        } else {
            // =============================================================
            // Frustum - quads to top ring
            // =============================================================
            int[] topVerts = new int[segments + 1];
            for (int seg = 0; seg <= segments; seg++) {
                float phi = GeometryMath.TWO_PI * seg / segments;
                float x = topR * (float) Math.cos(phi);
                float z = topR * (float) Math.sin(phi);
                
                float nx = normalXZScale * (float) Math.cos(phi);
                float ny = normalY;
                float nz = normalXZScale * (float) Math.sin(phi);
                
                float u = seg / (float) segments;
                topVerts[seg] = builder.addVertex(new Vertex(x, topY, z, nx, ny, nz, u, 0));
            }
            
            // Side quads
            for (int seg = 0; seg < segments; seg++) {
                float segFrac = seg / (float) segments;
                if (visibility != null && !visibility.isVisible(0.5f, segFrac)) continue;
                if (!pattern.shouldRender(seg, segments)) continue;
                
                int b0 = bottomVerts[seg];
                int b1 = bottomVerts[seg + 1];
                int t0 = topVerts[seg];
                int t1 = topVerts[seg + 1];
                
                builder.triangle(b0, t0, t1);
                builder.triangle(b0, t1, b1);
            }
            
            // Top cap (if not open)
            if (!shape.openTop()) {
                int topCenterIdx = builder.addVertex(new Vertex(0, topY, 0, 0, 1, 0, 0.5f, 0.5f));
                for (int seg = 0; seg < segments; seg++) {
                    float segFrac = seg / (float) segments;
                    if (visibility != null && !visibility.isVisible(0f, segFrac)) continue;
                    if (!pattern.shouldRender(seg, segments)) continue;
                    
                    builder.triangle(topVerts[seg], topCenterIdx, topVerts[seg + 1]);
                }
            }
        }
        
        // =================================================================
        // Bottom cap (if not open)
        // =================================================================
        if (!shape.openBottom()) {
            int bottomCenterIdx = builder.addVertex(new Vertex(0, bottomY, 0, 0, -1, 0, 0.5f, 0.5f));
            for (int seg = 0; seg < segments; seg++) {
                float segFrac = seg / (float) segments;
                if (visibility != null && !visibility.isVisible(1f, segFrac)) continue;
                if (!pattern.shouldRender(seg, segments)) continue;
                
                // Reverse winding for bottom face
                builder.triangle(bottomVerts[seg + 1], bottomCenterIdx, bottomVerts[seg]);
            }
        }
        
        Logging.RENDER.topic("tessellate")
            .kv("vertices", builder.vertexCount())
            .trace("Cone tessellation complete");
        
        return builder.build();
    }
    
    public static Mesh tessellate(ConeShape shape) {
        return tessellate(shape, null, null);
    }
}


