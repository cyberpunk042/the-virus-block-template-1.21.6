package net.cyberpunk042.client.visual._legacy.mesh;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.client.visual.mesh.PrimitiveType;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.visual.appearance.PatternConfig;
import net.cyberpunk042.visual.pattern.DynamicQuadPattern;
import net.cyberpunk042.visual.pattern.QuadPattern;
import net.cyberpunk042.visual.shape.SphereShape;

/**
 * Tessellates a sphere using latitude/longitude subdivision.
 * 
 * <h2>Geometry</h2>
 * <ul>
 *   <li>Poles at y = ±radius</li>
 *   <li>Equator at y = 0</li>
 *   <li>Latitude lines from pole to pole</li>
 *   <li>Longitude lines around the equator</li>
 * </ul>
 * 
 * <h2>Partial Spheres</h2>
 * <p>Supports hemispheres, bands, and arcs via latStart/End and lonStart/End.
 * 
 * <h2>Patterns</h2>
 * <p>Supports banded and checker patterns via {@link PatternConfig}.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // From SphereShape
 * Mesh mesh = SphereTessellator_old.fromShape(sphereShape);
 * 
 * // With builder
 * Mesh sphere = SphereTessellator_old.create()
 *     .latSteps(32)
 *     .lonSteps(64)
 *     .radius(5.0f)
 *     .pattern(PatternConfig.bands(4, 0.3f))
 *     .tessellate(0);
 * </pre>
 * 
 * @see SphereShape
 * @see PatternConfig
 */
public final class SphereTessellator_old implements Tessellator {
    
    private final int latSteps;
    private final int lonSteps;
    private final float radius;
    private final float latStart;
    private final float latEnd;
    private final float lonStart;
    private final float lonEnd;
    private final boolean wireframe;
    private final PatternConfig pattern;
    
    private SphereTessellator_old(Builder builder) {
        this.latSteps = builder.latSteps;
        this.lonSteps = builder.lonSteps;
        this.radius = builder.radius;
        this.latStart = builder.latStart;
        this.latEnd = builder.latEnd;
        this.lonStart = builder.lonStart;
        this.lonEnd = builder.lonEnd;
        this.wireframe = builder.wireframe;
        this.pattern = builder.pattern;
    }
    
    // =========================================================================
    // Static Factory Methods
    // =========================================================================
    
    /**
     * Creates a new builder with default settings.
     */
    public static Builder create() {
        return new Builder();
    }
    
    /**
     * Creates a tessellator from a SphereShape.
     * 
     * <p>Note: TYPE_A and TYPE_E algorithms are handled at render time by 
     * {@link SphereRenderer_old}, not here. This method always uses LAT_LON tessellation.
     * 
     * @param shape The shape definition
     * @return Mesh of the sphere using LAT_LON tessellation
     */
    public static Mesh fromShape(SphereShape shape) {
        // LAT_LON tessellation - TypeA/TypeE are handled by SphereRenderer_old directly
        return create()
            .latSteps(shape.latSteps())
            .lonSteps(shape.lonSteps())
            .radius(shape.radius())
            .latRange(shape.latStart(), shape.latEnd())
            .lonRange(shape.lonStart(), shape.lonEnd())
            .tessellate(0);
    }
    
    /**
     * Creates a tessellator from a SphereShape with pattern.
     * @param shape The shape definition
     * @param pattern The surface pattern
     * @return Mesh of the sphere with pattern applied
     */
    public static Mesh fromShape(SphereShape shape, PatternConfig pattern) {
        return create()
            .latSteps(shape.latSteps())
            .lonSteps(shape.lonSteps())
            .radius(shape.radius())
            .latRange(shape.latStart(), shape.latEnd())
            .lonRange(shape.lonStart(), shape.lonEnd())
            .pattern(pattern)
            .tessellate(0);
    }
    
    /**
     * Quick sphere with specified lat/lon steps.
     */
    public static Mesh sphere(int latSteps, int lonSteps) {
        return create().latSteps(latSteps).lonSteps(lonSteps).tessellate(0);
    }
    
    /**
     * Quick sphere with specified lat/lon steps and radius.
     */
    public static Mesh sphere(int latSteps, int lonSteps, float radius) {
        return create().latSteps(latSteps).lonSteps(lonSteps).radius(radius).tessellate(0);
    }
    
    /**
     * Quick wireframe sphere.
     */
    public static Mesh wireframeSphere(int latSteps, int lonSteps, float radius) {
        return create().latSteps(latSteps).lonSteps(lonSteps).radius(radius).wireframe(true).tessellate(0);
    }
    
    /**
     * Quick hemisphere (dome or bowl).
     * @param top true for upper half, false for lower half
     */
    public static Mesh hemisphere(int latSteps, int lonSteps, float radius, boolean top) {
        return create()
            .latSteps(latSteps)
            .lonSteps(lonSteps)
            .radius(radius)
            .latRange(top ? 0.0f : 0.5f, top ? 0.5f : 1.0f)
            .tessellate(0);
    }
    
    @Override
    public Mesh tessellate(int detail) {
        // Detail parameter is ignored - we use lat/lon steps instead
        // This allows the interface to be satisfied while using explicit steps
        Mesh mesh = wireframe ? tessellateWireframe() : tessellateSolid();
        
        // Log with partial sphere and pattern info
        String partialInfo = isFullSphere() ? "" : String.format(" partial[%.2f-%.2f,%.2f-%.2f]", 
            latStart, latEnd, lonStart, lonEnd);
        String patternInfo = (pattern != null && pattern.hasPattern()) 
            ? " pattern:" + pattern.type().id() 
            : "";
        
        Logging.RENDER.topic("tessellate").trace(
            "Sphere: lat={} lon={} radius={:.2f} verts={} indices={} {}{}{}",
            latSteps, lonSteps, radius, 
            mesh.vertexCount(), mesh.indexCount(),
            wireframe ? "wireframe" : "solid",
            partialInfo, patternInfo);
        
        return mesh;
    }
    
    /**
     * Checks if this tessellates a full sphere (no partial parameters).
     */
    private boolean isFullSphere() {
        return latStart <= 0.001f && latEnd >= 0.999f && 
               lonStart <= 0.001f && lonEnd >= 0.999f;
    }
    
    private Mesh tessellateSolid() {
        MeshBuilder builder = MeshBuilder.triangles();
        
        float latRange = latEnd - latStart;
        float lonRange = lonEnd - lonStart;
        
        // Generate vertices in a grid
        // vertices[lat][lon] = vertex index
        int[][] vertexIndices = new int[latSteps + 1][lonSteps + 1];
        
        for (int lat = 0; lat <= latSteps; lat++) {
            float latNorm = latStart + (lat / (float) latSteps) * latRange;
            float theta = latNorm * (float) Math.PI;
            
            for (int lon = 0; lon <= lonSteps; lon++) {
                float lonNorm = lonStart + (lon / (float) lonSteps) * lonRange;
                float phi = lonNorm * (float) Math.PI * 2;
                
                Vertex v = Vertex.spherical(theta, phi, radius);
                vertexIndices[lat][lon] = builder.addVertex(v);
            }
        }
        
        // Generate triangles (with pattern filtering)
        boolean hasPattern = pattern != null && pattern.hasPattern();
        
        for (int lat = 0; lat < latSteps; lat++) {
            float latFrac = lat / (float) latSteps;
            
            for (int lon = 0; lon < lonSteps; lon++) {
                float lonFrac = lon / (float) lonSteps;
                
                // Skip cells that don't match the pattern
                if (hasPattern && !pattern.shouldRender(latFrac, lonFrac)) {
                    continue;
                }
                
                int topLeft = vertexIndices[lat][lon];
                int topRight = vertexIndices[lat][lon + 1];
                int bottomLeft = vertexIndices[lat + 1][lon];
                int bottomRight = vertexIndices[lat + 1][lon + 1];
                
                // Two triangles per grid cell, using the pattern's vertex arrangement
                if (pattern != null && pattern.vertexPattern() instanceof DynamicQuadPattern dqp) {
                    // Dynamic pattern from shuffle exploration
                    builder.quadAsTriangles(topLeft, topRight, bottomRight, bottomLeft, dqp.triangles());
                } else {
                    // Static QuadPattern enum
                    builder.quadAsTriangles(topLeft, topRight, bottomRight, bottomLeft, 
                        pattern != null ? pattern.quadPattern() : null);
                }
            }
        }
        
        return builder.build();
    }
    
    private Mesh tessellateWireframe() {
        MeshBuilder builder = MeshBuilder.lines();
        
        float latRange = latEnd - latStart;
        float lonRange = lonEnd - lonStart;
        
        // Generate vertices
        int[][] vertexIndices = new int[latSteps + 1][lonSteps + 1];
        
        for (int lat = 0; lat <= latSteps; lat++) {
            float latNorm = latStart + (lat / (float) latSteps) * latRange;
            float theta = latNorm * (float) Math.PI;
            
            for (int lon = 0; lon <= lonSteps; lon++) {
                float lonNorm = lonStart + (lon / (float) lonSteps) * lonRange;
                float phi = lonNorm * (float) Math.PI * 2;
                
                Vertex v = Vertex.spherical(theta, phi, radius);
                vertexIndices[lat][lon] = builder.addVertex(v);
            }
        }
        
        // Generate latitude lines (horizontal rings)
        for (int lat = 0; lat <= latSteps; lat++) {
            for (int lon = 0; lon < lonSteps; lon++) {
                builder.line(vertexIndices[lat][lon], vertexIndices[lat][lon + 1]);
            }
        }
        
        // Generate longitude lines (vertical arcs)
        for (int lon = 0; lon <= lonSteps; lon++) {
            for (int lat = 0; lat < latSteps; lat++) {
                builder.line(vertexIndices[lat][lon], vertexIndices[lat + 1][lon]);
            }
        }
        
        return builder.build();
    }
    
    @Override
    public int defaultDetail() {
        return 32;
    }
    
    public static final class Builder {
        private int latSteps = 16;
        private int lonSteps = 32;
        private float radius = 1.0f;
        private float latStart = 0.0f;
        private float latEnd = 1.0f;
        private float lonStart = 0.0f;
        private float lonEnd = 1.0f;
        private boolean wireframe = false;
        private PatternConfig pattern = PatternConfig.NONE;
        
        private Builder() {}
        
        /**
         * Sets the number of latitude steps (vertical divisions).
         */
        public Builder latSteps(int steps) {
            this.latSteps = Math.max(2, steps);
            return this;
        }
        
        /**
         * Sets the number of longitude steps (horizontal divisions).
         */
        public Builder lonSteps(int steps) {
            this.lonSteps = Math.max(4, steps);
            return this;
        }
        
        /**
         * Sets both lat and lon steps to the same value.
         */
        public Builder detail(int steps) {
            this.latSteps = Math.max(2, steps);
            this.lonSteps = Math.max(4, steps * 2);
            return this;
        }
        
        /**
         * Sets the radius of the sphere.
         */
        public Builder radius(float radius) {
            this.radius = radius;
            return this;
        }
        
        /**
         * Sets the latitude range (0.0 = top, 1.0 = bottom).
         */
        public Builder latRange(float start, float end) {
            this.latStart = start;
            this.latEnd = end;
            return this;
        }
        
        /**
         * Sets the longitude range (0.0 to 1.0 = full circle).
         */
        public Builder lonRange(float start, float end) {
            this.lonStart = start;
            this.lonEnd = end;
            return this;
        }
        
        /**
         * Generates a wireframe instead of solid triangles.
         */
        public Builder wireframe(boolean wireframe) {
            this.wireframe = wireframe;
            return this;
        }
        
        /**
         * Sets the surface pattern (bands, checker, etc.).
         * @see PatternConfig
         */
        public Builder pattern(PatternConfig pattern) {
            this.pattern = pattern != null ? pattern : PatternConfig.NONE;
            return this;
        }
        
        /**
         * Configures from a SphereShape.
         */
        public Builder fromShape(SphereShape shape) {
            this.latSteps = shape.latSteps();
            this.lonSteps = shape.lonSteps();
            this.radius = shape.radius();
            this.latStart = shape.latStart();
            this.latEnd = shape.latEnd();
            this.lonStart = shape.lonStart();
            this.lonEnd = shape.lonEnd();
            return this;
        }
        
        /**
         * Builds the tessellator.
         */
        public SphereTessellator_old build() {
            return new SphereTessellator_old(this);
        }
        
        /**
         * Builds and immediately tessellates.
         */
        public Mesh tessellate(int detail) {
            return build().tessellate(detail);
        }
    }
}
