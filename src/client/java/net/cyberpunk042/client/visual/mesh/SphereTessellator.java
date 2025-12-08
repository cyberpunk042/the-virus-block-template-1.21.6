package net.cyberpunk042.client.visual.mesh;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.pattern.QuadPattern;
import net.cyberpunk042.visual.shape.SphereShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Tessellates sphere shapes into triangle meshes.
 * 
 * <h2>Algorithm</h2>
 * <p>Uses latitude/longitude subdivision to create a grid of quads that are
 * then triangulated. Vertices are generated using spherical coordinates.</p>
 * 
 * <h2>Partial Spheres</h2>
 * <p>Supports partial spheres via latStart/latEnd and lonStart/lonEnd parameters:
 * <ul>
 *   <li>Hemisphere: latStart=0, latEnd=0.5 (top half)</li>
 *   <li>Band: latStart=0.3, latEnd=0.7</li>
 *   <li>Arc: lonStart=0, lonEnd=0.5 (half sphere around Y)</li>
 * </ul>
 * 
 * <h2>Patterns</h2>
 * <p>Supports vertex patterns via {@link VertexPattern} for visual variety.
 * Pattern's {@code shouldRender()} determines which cells are visible.</p>
 * 
 * <h2>Usage</h2>
 * <pre>
 * Mesh mesh = SphereTessellator.tessellate(sphereShape, QuadPattern.FILLED_1, null);
 * </pre>
 * 
 * @see SphereShape
 * @see QuadPattern
 * @see Vertex#spherical(float, float, float)
 */
public final class SphereTessellator {
    
    private SphereTessellator() {} // Static utility class
    
    // =========================================================================
    // Main API
    // =========================================================================
    
    /**
     * Tessellates a sphere shape into a mesh.
     * 
     * @param shape The sphere shape definition
     * @param pattern Vertex pattern for visual variety (null = default)
     * @param visibility Visibility mask for cell filtering (null = all visible)
     * @return Generated mesh
     */
    public static Mesh tessellate(SphereShape shape, VertexPattern pattern, 
                                   VisibilityMask visibility) {
        if (shape == null) {
            throw new IllegalArgumentException("SphereShape cannot be null");
        }
        
        // Use default pattern if none provided
        if (pattern == null) {
            pattern = QuadPattern.FILLED_1;
        }
        
        Logging.RENDER.topic("tessellate")
            .kv("shape", "sphere")
            .kv("radius", shape.radius())
            .kv("latSteps", shape.latSteps())
            .kv("lonSteps", shape.lonSteps())
            .debug("Tessellating sphere");
        
        return tessellateSolid(shape, pattern, visibility);
    }
    
    /**
     * Tessellates a sphere with default pattern and no visibility mask.
     */
    public static Mesh tessellate(SphereShape shape) {
        return tessellate(shape, null, null);
    }
    
    // =========================================================================
    // Implementation
    // =========================================================================
    
    private static Mesh tessellateSolid(SphereShape shape, VertexPattern pattern,
                                         VisibilityMask visibility) {
        MeshBuilder builder = MeshBuilder.triangles();
        
        int latSteps = shape.latSteps();
        int lonSteps = shape.lonSteps();
        float radius = shape.radius();
        float latStart = shape.latStart();
        float latEnd = shape.latEnd();
        float lonStart = shape.lonStart();
        float lonEnd = shape.lonEnd();
        
        float latRange = latEnd - latStart;
        float lonRange = lonEnd - lonStart;
        
        // Generate vertex grid
        // vertices[lat][lon] = vertex index in builder
        int[][] vertexIndices = new int[latSteps + 1][lonSteps + 1];
        
        for (int lat = 0; lat <= latSteps; lat++) {
            float latNorm = latStart + (lat / (float) latSteps) * latRange;
            float theta = latNorm * GeometryMath.PI;  // 0 to PI (top to bottom)
            
            for (int lon = 0; lon <= lonSteps; lon++) {
                float lonNorm = lonStart + (lon / (float) lonSteps) * lonRange;
                float phi = lonNorm * GeometryMath.TWO_PI;  // 0 to 2PI (around)
                
                Vertex v = Vertex.spherical(theta, phi, radius);
                vertexIndices[lat][lon] = builder.addVertex(v);
            }
        }
        
        // Generate triangles for each quad cell
        for (int lat = 0; lat < latSteps; lat++) {
            float latFrac = lat / (float) latSteps;
            
            for (int lon = 0; lon < lonSteps; lon++) {
                float lonFrac = lon / (float) lonSteps;
                
                // Check visibility mask
                if (visibility != null && !visibility.isVisible(latFrac, lonFrac)) {
                    continue;
                }
                
                // Check pattern
                if (!pattern.shouldRender(lat * lonSteps + lon, latSteps * lonSteps)) {
                    continue;
                }
                
                // Get quad corner indices
                int topLeft = vertexIndices[lat][lon];
                int topRight = vertexIndices[lat][lon + 1];
                int bottomLeft = vertexIndices[lat + 1][lon];
                int bottomRight = vertexIndices[lat + 1][lon + 1];
                
                // Emit triangles using pattern's vertex order
                if (pattern instanceof QuadPattern qp) {
                    builder.quadAsTriangles(topLeft, topRight, bottomRight, bottomLeft, qp);
                } else {
                    // Default triangulation
                    builder.triangle(topLeft, topRight, bottomRight);
                    builder.triangle(topLeft, bottomRight, bottomLeft);
                }
            }
        }
        
        Mesh mesh = builder.build();
        
        Logging.RENDER.topic("tessellate")
            .kv("vertices", mesh.vertexCount())
            .kv("indices", mesh.indexCount())
            .trace("Sphere tessellation complete");
        
        return mesh;
    }
}
