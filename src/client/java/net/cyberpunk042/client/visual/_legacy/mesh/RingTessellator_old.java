package net.cyberpunk042.client.visual._legacy.mesh;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.client.visual.mesh.PrimitiveType;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.visual.pattern.SegmentPattern;

/**
 * Tessellates a flat ring (annulus) in the XZ plane.
 * 
 * <p>The ring is generated at the specified Y coordinate with
 * inner and outer radii defining the ring thickness.
 * 
 * <h2>Pattern Support</h2>
 * <p>Supports {@link SegmentPattern} to control which segments are rendered:
 * <ul>
 *   <li>ALTERNATING - every other segment (dashed look)</li>
 *   <li>SPARSE - every third segment</li>
 *   <li>QUARTER - every fourth segment</li>
 * </ul>
 */
public final class RingTessellator_old implements Tessellator {
    
    private final float y;
    private final float innerRadius;
    private final float outerRadius;
    private final int segments;
    private final boolean wireframe;
    private final SegmentPattern pattern;
    
    private RingTessellator_old(Builder builder) {
        this.y = builder.y;
        this.innerRadius = builder.innerRadius;
        this.outerRadius = builder.outerRadius;
        this.segments = builder.segments;
        this.wireframe = builder.wireframe;
        this.pattern = builder.pattern;
    }
    
    /**
     * Creates a new builder with default settings.
     */
    public static Builder create() {
        return new Builder();
    }
    
    /**
     * Quick ring with specified parameters.
     */
    public static Mesh ring(float y, float innerRadius, float outerRadius, int segments) {
        return create()
            .y(y)
            .innerRadius(innerRadius)
            .outerRadius(outerRadius)
            .segments(segments)
            .tessellate(0);
    }
    
    @Override
    public Mesh tessellate(int detail) {
        Mesh mesh = wireframe ? tessellateWireframe() : tessellateSolid();
        
        Logging.RENDER.topic("tessellate").trace(
            "Ring: y={:.2f} inner={:.2f} outer={:.2f} segs={} verts={} indices={} {}",
            y, innerRadius, outerRadius, segments,
            mesh.vertexCount(), mesh.indexCount(),
            wireframe ? "wireframe" : "solid");
        
        return mesh;
    }
    
    private Mesh tessellateSolid() {
        MeshBuilder builder = MeshBuilder.quads();
        
        // Generate vertices: alternating inner/outer around the ring
        int[] innerIndices = new int[segments + 1];
        int[] outerIndices = new int[segments + 1];
        
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2 * Math.PI * i / segments);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            
            // Inner vertex
            float ix = innerRadius * cos;
            float iz = innerRadius * sin;
            innerIndices[i] = builder.addVertex(new Vertex(
                ix, y, iz,
                0, 1, 0,  // Normal pointing up
                (float) i / segments, 0  // UV
            ));
            
            // Outer vertex
            float ox = outerRadius * cos;
            float oz = outerRadius * sin;
            outerIndices[i] = builder.addVertex(new Vertex(
                ox, y, oz,
                0, 1, 0,  // Normal pointing up
                (float) i / segments, 1  // UV
            ));
        }
        
        // Generate quads - apply pattern to determine which segments to render
        for (int i = 0; i < segments; i++) {
            // Check if this segment should be rendered based on pattern
            if (pattern != null && !pattern.shouldRender(i)) {
                continue; // Skip this segment
            }
            
            builder.quad(
                innerIndices[i],
                innerIndices[i + 1],
                outerIndices[i + 1],
                outerIndices[i]
            );
        }
        
        return builder.build();
    }
    
    private Mesh tessellateWireframe() {
        MeshBuilder builder = MeshBuilder.lines();
        
        int[] innerIndices = new int[segments + 1];
        int[] outerIndices = new int[segments + 1];
        
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2 * Math.PI * i / segments);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            
            float ix = innerRadius * cos;
            float iz = innerRadius * sin;
            innerIndices[i] = builder.addVertex(Vertex.pos(ix, y, iz));
            
            float ox = outerRadius * cos;
            float oz = outerRadius * sin;
            outerIndices[i] = builder.addVertex(Vertex.pos(ox, y, oz));
        }
        
        // Inner circle
        for (int i = 0; i < segments; i++) {
            builder.line(innerIndices[i], innerIndices[i + 1]);
        }
        
        // Outer circle
        for (int i = 0; i < segments; i++) {
            builder.line(outerIndices[i], outerIndices[i + 1]);
        }
        
        // Radial spokes (optional - every Nth segment)
        int spokeInterval = Math.max(1, segments / 8);
        for (int i = 0; i < segments; i += spokeInterval) {
            builder.line(innerIndices[i], outerIndices[i]);
        }
        
        return builder.build();
    }
    
    @Override
    public int defaultDetail() {
        return 48;
    }
    
    public static final class Builder {
        private float y = 0.0f;
        private float innerRadius = 0.5f;
        private float outerRadius = 1.0f;
        private int segments = 48;
        private boolean wireframe = false;
        private SegmentPattern pattern = SegmentPattern.DEFAULT;
        
        private Builder() {}
        
        public Builder y(float y) {
            this.y = y;
            return this;
        }
        
        public Builder innerRadius(float radius) {
            this.innerRadius = radius;
            return this;
        }
        
        public Builder outerRadius(float radius) {
            this.outerRadius = radius;
            return this;
        }
        
        public Builder thickness(float thickness) {
            // Convenience: set outer radius based on inner + thickness
            this.outerRadius = this.innerRadius + thickness;
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
         * Sets the segment pattern (controls which segments are rendered).
         */
        public Builder pattern(SegmentPattern pattern) {
            this.pattern = pattern != null ? pattern : SegmentPattern.DEFAULT;
            return this;
        }
        
        public RingTessellator_old build() {
            return new RingTessellator_old(this);
        }
        
        public Mesh tessellate(int detail) {
            return build().tessellate(detail);
        }
    }
}
