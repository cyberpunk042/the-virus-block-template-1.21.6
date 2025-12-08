package net.cyberpunk042.client.visual._legacy.mesh;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.shape.DiscShape;
import net.cyberpunk042.visual.pattern.SectorPattern;

/**
 * Tessellates a flat circular disc (filled circle) in the XZ plane.
 * 
 * <h2>Geometry</h2>
 * <p>Uses a triangle fan from center vertex to edge vertices:
 * <ul>
 *   <li>Center vertex at (0, y, 0) with normal (0, 1, 0)</li>
 *   <li>Edge vertices distributed around the circle</li>
 *   <li>All triangles share the center vertex</li>
 * </ul>
 * 
 * <h2>Vertex Count</h2>
 * <ul>
 *   <li>Solid: segments + 1 (center + edge)</li>
 *   <li>Wireframe: segments + 2 (edges + center for spokes)</li>
 * </ul>
 * 
 * <h2>UV Mapping</h2>
 * <p>UVs map to a unit circle centered at (0.5, 0.5):
 * <pre>
 * u = 0.5 + 0.5 * cos(angle)
 * v = 0.5 + 0.5 * sin(angle)
 * </pre>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // From shape
 * Mesh mesh = DiscTessellator_old.fromShape(discShape);
 * 
 * // Builder pattern
 * Mesh mesh = DiscTessellator_old.create()
 *     .y(0.5f)
 *     .radius(2.0f)
 *     .segments(48)
 *     .tessellate(0);
 * </pre>
 * 
 * @see DiscShape
 * @see RingTessellator_old
 */
public final class DiscTessellator_old implements Tessellator {
    
    private final float y;
    private final float radius;
    private final int segments;
    private final boolean wireframe;
    private final SectorPattern pattern;
    
    private DiscTessellator_old(Builder builder) {
        this.y = builder.y;
        this.radius = builder.radius;
        this.segments = builder.segments;
        this.wireframe = builder.wireframe;
        this.pattern = builder.pattern;
    }
    
    public static Builder create() {
        return new Builder();
    }
    
    /**
     * Quick disc from shape.
     */
    public static Mesh fromShape(DiscShape shape) {
        return create()
            .y(shape.y())
            .radius(shape.radius())
            .segments(shape.segments())
            .tessellate(0);
    }
    
    /**
     * Quick disc with parameters.
     */
    public static Mesh disc(float y, float radius, int segments) {
        return create()
            .y(y)
            .radius(radius)
            .segments(segments)
            .tessellate(0);
    }
    
    @Override
    public Mesh tessellate(int detail) {
        Mesh mesh = wireframe ? tessellateWireframe() : tessellateSolid();
        
        Logging.RENDER.topic("tessellate").trace(
            "Disc: y={:.2f} radius={:.2f} segs={} verts={} {}",
            y, radius, segments, mesh.vertexCount(),
            wireframe ? "wireframe" : "solid");
        
        return mesh;
    }
    
    private Mesh tessellateSolid() {
        MeshBuilder builder = MeshBuilder.triangles();
        
        // Center vertex
        int centerIdx = builder.vertex(0, y, 0, 0, 1, 0, 0.5f, 0.5f);
        
        // Edge vertices
        int[] edgeIndices = new int[segments + 1];
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2 * Math.PI * i / segments);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            
            float x = radius * cos;
            float z = radius * sin;
            
            // UV maps to unit circle
            float u = 0.5f + 0.5f * cos;
            float v = 0.5f + 0.5f * sin;
            
            edgeIndices[i] = builder.vertex(x, y, z, 0, 1, 0, u, v);
        }
        
        // Triangle fan from center - apply pattern to determine which sectors to render
        for (int i = 0; i < segments; i++) {
            // Check if this sector should be rendered based on pattern
            if (pattern != null && !pattern.shouldRender(i, segments)) {
                continue; // Skip this sector
            }
            
            builder.triangle(centerIdx, edgeIndices[i], edgeIndices[i + 1]);
        }
        
        return builder.build();
    }
    
    private Mesh tessellateWireframe() {
        MeshBuilder builder = MeshBuilder.lines();
        
        // Edge vertices only (no center for wireframe)
        int[] edgeIndices = new int[segments + 1];
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2 * Math.PI * i / segments);
            float x = radius * (float) Math.cos(angle);
            float z = radius * (float) Math.sin(angle);
            
            edgeIndices[i] = builder.vertex(x, y, z);
        }
        
        // Circle outline
        for (int i = 0; i < segments; i++) {
            builder.line(edgeIndices[i], edgeIndices[i + 1]);
        }
        
        // Optional: radial spokes from center
        int centerIdx = builder.vertex(0, y, 0);
        int spokeInterval = Math.max(1, segments / 8);
        for (int i = 0; i < segments; i += spokeInterval) {
            builder.line(centerIdx, edgeIndices[i]);
        }
        
        return builder.build();
    }
    
    @Override
    public int defaultDetail() {
        return 32;
    }
    
    public static final class Builder {
        private float y = 0.0f;
        private float radius = 1.0f;
        private int segments = 32;
        private boolean wireframe = false;
        private SectorPattern pattern = SectorPattern.DEFAULT;
        
        private Builder() {}
        
        public Builder y(float y) {
            this.y = y;
            return this;
        }
        
        public Builder radius(float radius) {
            this.radius = radius;
            return this;
        }
        
        public Builder segments(int segments) {
            this.segments = Math.max(4, segments);
            return this;
        }
        
        public Builder wireframe(boolean wireframe) {
            this.wireframe = wireframe;
            return this;
        }
        
        /**
         * Sets the sector pattern (controls which sectors are rendered).
         */
        public Builder pattern(SectorPattern pattern) {
            this.pattern = pattern != null ? pattern : SectorPattern.DEFAULT;
            return this;
        }
        
        public Builder fromShape(DiscShape shape) {
            this.y = shape.y();
            this.radius = shape.radius();
            this.segments = shape.segments();
            return this;
        }
        
        public DiscTessellator_old build() {
            return new DiscTessellator_old(this);
        }
        
        public Mesh tessellate(int detail) {
            return build().tessellate(detail);
        }
    }
}

