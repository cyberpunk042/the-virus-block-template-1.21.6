package net.cyberpunk042.client.visual.mesh;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.pattern.QuadPattern;
import net.cyberpunk042.visual.shape.SphereAlgorithm;
import net.cyberpunk042.visual.shape.SphereShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

import java.util.HashMap;
import java.util.Map;

/**
 * Tessellates sphere shapes into triangle meshes.
 * 
 * <h2>Algorithms</h2>
 * <p>Supports multiple tessellation algorithms via {@link SphereAlgorithm}:</p>
 * <ul>
 *   <li><b>LAT_LON</b>: Latitude/longitude grid. Best for patterns and partial spheres.</li>
 *   <li><b>UV_SPHERE</b>: Standard UV mapping with shared pole vertices.</li>
 *   <li><b>ICO_SPHERE</b>: Icosahedron subdivision. Uniform distribution, no pole artifacts.</li>
 *   <li><b>TYPE_A/TYPE_E</b>: Direct rendering (handled by SphereRenderer, not tessellation).</li>
 * </ul>
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
 * @see SphereAlgorithm
 * @see QuadPattern
 * @see Vertex#spherical(float, float, float)
 */
public final class SphereTessellator {
    
    private SphereTessellator() {} // Static utility class
    
    // Golden ratio for icosahedron
    private static final float PHI = (1.0f + (float) Math.sqrt(5.0)) / 2.0f;
    
    // =========================================================================
    // Main API
    // =========================================================================
    
    /**
     * Tessellates a sphere shape into a mesh.
     * 
     * <p>Dispatches to the appropriate algorithm based on {@link SphereShape#algorithm()}.</p>
     * 
     * <p><b>Note:</b> TYPE_A and TYPE_E are not mesh-based algorithms - they use
     * direct rendering with overlapping cubes. For those algorithms, this method
     * falls back to LAT_LON tessellation. Use {@link SphereRenderer} for proper
     * TYPE_A/TYPE_E rendering.</p>
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
        
        SphereAlgorithm algorithm = shape.algorithm();
        
        Logging.RENDER.topic("tessellate")
            .kv("shape", "sphere")
            .kv("algorithm", algorithm)
            .kv("radius", shape.radius())
            .debug("Tessellating sphere");
        
        // Dispatch based on algorithm
        return switch (algorithm) {
            case LAT_LON -> tessellateLatLon(shape, pattern, visibility);
            case UV_SPHERE -> tessellateUvSphere(shape, pattern, visibility);
            case ICO_SPHERE -> tessellateIcoSphere(shape);
            // TYPE_A and TYPE_E are direct rendering, not mesh tessellation
            // Fall back to LAT_LON for mesh generation
            case TYPE_A, TYPE_E -> {
                Logging.RENDER.topic("tessellate")
                    .kv("algorithm", algorithm)
                    .warn("TYPE_A/TYPE_E use direct rendering. Falling back to LAT_LON for mesh.");
                yield tessellateLatLon(shape, pattern, visibility);
            }
        };
    }
    
    /**
     * Tessellates a sphere with default pattern and no visibility mask.
     */
    public static Mesh tessellate(SphereShape shape) {
        return tessellate(shape, null, null);
    }
    
    /**
     * Check if the algorithm requires direct rendering (TYPE_A/TYPE_E).
     * These algorithms cannot be tessellated into a mesh.
     */
    public static boolean requiresDirectRendering(SphereAlgorithm algorithm) {
        return algorithm == SphereAlgorithm.TYPE_A || algorithm == SphereAlgorithm.TYPE_E;
    }
    
    // =========================================================================
    // LAT_LON Algorithm (Default)
    // =========================================================================
    
    /**
     * Tessellates using latitude/longitude grid.
     * Best for patterns, partial spheres, and visibility masks.
     * Has pole singularities (vertices cluster at poles).
     */
    private static Mesh tessellateLatLon(SphereShape shape, VertexPattern pattern,
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
                    builder.triangle(topLeft, topRight, bottomRight);
                    builder.triangle(topLeft, bottomRight, bottomLeft);
                }
            }
        }
        
        return logAndBuild(builder, "LAT_LON");
    }
    
    // =========================================================================
    // UV_SPHERE Algorithm
    // =========================================================================
    
    /**
     * Tessellates using UV sphere with shared pole vertices.
     * Similar to LAT_LON but with proper pole handling.
     */
    private static Mesh tessellateUvSphere(SphereShape shape, VertexPattern pattern,
                                            VisibilityMask visibility) {
        MeshBuilder builder = MeshBuilder.triangles();
        
        int rings = shape.latSteps();
        int segments = shape.lonSteps();
        float radius = shape.radius();
        
        // Top pole vertex (single shared vertex)
        int topPole = builder.addVertex(Vertex.pos(0, radius, 0).withNormal(0, 1, 0));
        
        // Ring vertices (excluding poles)
        int[] ringStartIndices = new int[rings - 1];
        for (int ring = 1; ring < rings; ring++) {
            float theta = GeometryMath.PI * ring / rings;
            float y = (float) Math.cos(theta) * radius;
            float ringRadius = (float) Math.sin(theta) * radius;
            
            ringStartIndices[ring - 1] = builder.vertexCount();
            
            for (int seg = 0; seg < segments; seg++) {
                float phi = GeometryMath.TWO_PI * seg / segments;
                float x = ringRadius * (float) Math.cos(phi);
                float z = ringRadius * (float) Math.sin(phi);
                
                // Normal = normalized position
                float nx = (float) Math.sin(theta) * (float) Math.cos(phi);
                float ny = (float) Math.cos(theta);
                float nz = (float) Math.sin(theta) * (float) Math.sin(phi);
                
                builder.addVertex(Vertex.pos(x, y, z).withNormal(nx, ny, nz));
            }
        }
        
        // Bottom pole vertex
        int bottomPole = builder.addVertex(Vertex.pos(0, -radius, 0).withNormal(0, -1, 0));
        
        // Top cap triangles (pole to first ring)
        int firstRing = ringStartIndices[0];
        for (int seg = 0; seg < segments; seg++) {
            int nextSeg = (seg + 1) % segments;
            builder.triangle(topPole, firstRing + nextSeg, firstRing + seg);
        }
        
        // Middle quads (between rings)
        for (int ringIdx = 0; ringIdx < ringStartIndices.length - 1; ringIdx++) {
            int ringA = ringStartIndices[ringIdx];
            int ringB = ringStartIndices[ringIdx + 1];
            
            for (int seg = 0; seg < segments; seg++) {
                int nextSeg = (seg + 1) % segments;
                
                // Check pattern
                int cellIdx = ringIdx * segments + seg;
                if (!pattern.shouldRender(cellIdx, (rings - 2) * segments)) {
                    continue;
                }
                
                // Quad as two triangles
                builder.triangle(ringA + seg, ringA + nextSeg, ringB + nextSeg);
                builder.triangle(ringA + seg, ringB + nextSeg, ringB + seg);
            }
        }
        
        // Bottom cap triangles (last ring to pole)
        int lastRing = ringStartIndices[ringStartIndices.length - 1];
        for (int seg = 0; seg < segments; seg++) {
            int nextSeg = (seg + 1) % segments;
            builder.triangle(lastRing + seg, lastRing + nextSeg, bottomPole);
        }
        
        return logAndBuild(builder, "UV_SPHERE");
    }
    
    // =========================================================================
    // ICO_SPHERE Algorithm
    // =========================================================================
    
    /**
     * Tessellates using icosahedron subdivision.
     * Uniform vertex distribution, no pole artifacts.
     * Does NOT support partial spheres or patterns.
     * 
     * <p>Subdivision levels:</p>
     * <ul>
     *   <li>0: 12 vertices, 20 triangles (base icosahedron)</li>
     *   <li>1: 42 vertices, 80 triangles</li>
     *   <li>2: 162 vertices, 320 triangles</li>
     *   <li>3: 642 vertices, 1280 triangles</li>
     * </ul>
     */
    private static Mesh tessellateIcoSphere(SphereShape shape) {
        MeshBuilder builder = MeshBuilder.triangles();
        float radius = shape.radius();
        
        // Derive subdivision level from latSteps (approximate mapping)
        // latSteps 8-16 → subdiv 1, 17-32 → subdiv 2, 33-64 → subdiv 3, etc.
        int subdivisions = Math.max(0, Math.min(4, 
            (int) (Math.log(shape.latSteps() / 8.0) / Math.log(2)) + 1));
        
        // Icosahedron base vertices (12 vertices)
        float[][] icoVerts = {
            {-1,  PHI, 0}, { 1,  PHI, 0}, {-1, -PHI, 0}, { 1, -PHI, 0},
            { 0, -1,  PHI}, { 0,  1,  PHI}, { 0, -1, -PHI}, { 0,  1, -PHI},
            { PHI, 0, -1}, { PHI, 0,  1}, {-PHI, 0, -1}, {-PHI, 0,  1},
        };
        
        // Add normalized and scaled vertices
        int[] vertexIndices = new int[12];
        for (int i = 0; i < 12; i++) {
            float x = icoVerts[i][0];
            float y = icoVerts[i][1];
            float z = icoVerts[i][2];
            float len = (float) Math.sqrt(x*x + y*y + z*z);
            x = x/len * radius;
            y = y/len * radius;
            z = z/len * radius;
            vertexIndices[i] = builder.addVertex(
                Vertex.pos(x, y, z).withNormal(x/radius, y/radius, z/radius));
        }
        
        // Icosahedron faces (20 triangles)
        int[][] icoFaces = {
            {0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11},
            {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6}, {7, 1, 8},
            {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9},
            {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1},
        };
        
        // Build initial triangle list
        int[][] triangles = new int[icoFaces.length][3];
        for (int i = 0; i < icoFaces.length; i++) {
            triangles[i] = new int[]{
                vertexIndices[icoFaces[i][0]],
                vertexIndices[icoFaces[i][1]],
                vertexIndices[icoFaces[i][2]]
            };
        }
        
        // Subdivision with midpoint cache
        Map<Long, Integer> midpointCache = new HashMap<>();
        
        for (int level = 0; level < subdivisions; level++) {
            int[][] newTriangles = new int[triangles.length * 4][3];
            midpointCache.clear();
            
            int newIdx = 0;
            for (int[] tri : triangles) {
                int a = getMidpoint(builder, midpointCache, tri[0], tri[1], radius);
                int b = getMidpoint(builder, midpointCache, tri[1], tri[2], radius);
                int c = getMidpoint(builder, midpointCache, tri[2], tri[0], radius);
                
                newTriangles[newIdx++] = new int[]{tri[0], a, c};
                newTriangles[newIdx++] = new int[]{tri[1], b, a};
                newTriangles[newIdx++] = new int[]{tri[2], c, b};
                newTriangles[newIdx++] = new int[]{a, b, c};
            }
            
            triangles = newTriangles;
        }
        
        // Add triangles to mesh
        for (int[] tri : triangles) {
            builder.triangle(tri[0], tri[1], tri[2]);
        }
        
        return logAndBuild(builder, "ICO_SPHERE (subdiv=" + subdivisions + ")");
    }
    
    /**
     * Get or create midpoint vertex between two vertices, projected to sphere surface.
     */
    private static int getMidpoint(MeshBuilder builder, Map<Long, Integer> cache,
                                    int v0, int v1, float radius) {
        // Canonical key (smaller index first)
        long key = Math.min(v0, v1) * 100000L + Math.max(v0, v1);
        
        Integer cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        
        Vertex vert0 = builder.getVertex(v0);
        Vertex vert1 = builder.getVertex(v1);
        
        // Midpoint
        float mx = (vert0.x() + vert1.x()) / 2;
        float my = (vert0.y() + vert1.y()) / 2;
        float mz = (vert0.z() + vert1.z()) / 2;
        
        // Project to sphere surface
        float len = (float) Math.sqrt(mx*mx + my*my + mz*mz);
        mx = mx/len * radius;
        my = my/len * radius;
        mz = mz/len * radius;
        
        int idx = builder.addVertex(
            Vertex.pos(mx, my, mz).withNormal(mx/radius, my/radius, mz/radius));
        cache.put(key, idx);
        return idx;
    }
    
    // =========================================================================
    // Utilities
    // =========================================================================
    
    private static Mesh logAndBuild(MeshBuilder builder, String algorithm) {
        Mesh mesh = builder.build();
        Logging.RENDER.topic("tessellate")
            .kv("algorithm", algorithm)
            .kv("vertices", mesh.vertexCount())
            .kv("triangles", mesh.indexCount() / 3)
            .trace("Sphere tessellation complete");
        return mesh;
    }
}
