package net.cyberpunk042.client.visual._legacy.mesh;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.client.visual.mesh.Vertex;

/**
 * Tessellates a prism (cylinder with polygon cross-section).
 * 
 * <p>Can create cylinders, hexagonal prisms, cubes (4 sides), etc.
 */
public final class PrismTessellator_old implements Tessellator {
    
    private final int sides;
    private final float radius;
    private final float height;
    private final float yOffset;
    private final boolean caps;
    private final boolean wireframe;
    
    private PrismTessellator_old(Builder builder) {
        this.sides = builder.sides;
        this.radius = builder.radius;
        this.height = builder.height;
        this.yOffset = builder.yOffset;
        this.caps = builder.caps;
        this.wireframe = builder.wireframe;
    }
    
    public static Builder create() {
        return new Builder();
    }
    
    /**
     * Quick cylinder (high side count).
     */
    public static Mesh cylinder(float radius, float height, int segments) {
        return create().sides(segments).radius(radius).height(height).tessellate(0);
    }
    
    /**
     * Quick hexagonal prism.
     */
    public static Mesh hexagon(float radius, float height) {
        return create().sides(6).radius(radius).height(height).tessellate(0);
    }
    
    @Override
    public Mesh tessellate(int detail) {
        return wireframe ? tessellateWireframe() : tessellateSolid();
    }
    
    private Mesh tessellateSolid() {
        MeshBuilder builder = MeshBuilder.triangles();
        
        float halfHeight = height / 2;
        float yTop = yOffset + halfHeight;
        float yBottom = yOffset - halfHeight;
        
        // Generate vertices for top and bottom rings
        int[] topIndices = new int[sides];
        int[] bottomIndices = new int[sides];
        
        for (int i = 0; i < sides; i++) {
            float angle = (float) (2 * Math.PI * i / sides);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float x = radius * cos;
            float z = radius * sin;
            
            // Top vertex
            topIndices[i] = builder.addVertex(new Vertex(
                x, yTop, z,
                cos, 0, sin,  // Normal points outward
                (float) i / sides, 0
            ));
            
            // Bottom vertex
            bottomIndices[i] = builder.addVertex(new Vertex(
                x, yBottom, z,
                cos, 0, sin,
                (float) i / sides, 1
            ));
        }
        
        // Side faces (quads as two triangles each)
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            builder.quadAsTriangles(
                topIndices[i],
                topIndices[next],
                bottomIndices[next],
                bottomIndices[i]
            );
        }
        
        // Caps
        if (caps) {
            // Top cap center
            int topCenter = builder.addVertex(new Vertex(
                0, yTop, 0,
                0, 1, 0,
                0.5f, 0.5f
            ));
            
            // Bottom cap center
            int bottomCenter = builder.addVertex(new Vertex(
                0, yBottom, 0,
                0, -1, 0,
                0.5f, 0.5f
            ));
            
            for (int i = 0; i < sides; i++) {
                int next = (i + 1) % sides;
                
                // Top cap triangle (counter-clockwise from above)
                builder.triangle(topCenter, topIndices[i], topIndices[next]);
                
                // Bottom cap triangle (counter-clockwise from below)
                builder.triangle(bottomCenter, bottomIndices[next], bottomIndices[i]);
            }
        }
        
        return builder.build();
    }
    
    private Mesh tessellateWireframe() {
        MeshBuilder builder = MeshBuilder.lines();
        
        float halfHeight = height / 2;
        float yTop = yOffset + halfHeight;
        float yBottom = yOffset - halfHeight;
        
        int[] topIndices = new int[sides];
        int[] bottomIndices = new int[sides];
        
        for (int i = 0; i < sides; i++) {
            float angle = (float) (2 * Math.PI * i / sides);
            float x = radius * (float) Math.cos(angle);
            float z = radius * (float) Math.sin(angle);
            
            topIndices[i] = builder.addVertex(Vertex.pos(x, yTop, z));
            bottomIndices[i] = builder.addVertex(Vertex.pos(x, yBottom, z));
        }
        
        // Top ring
        for (int i = 0; i < sides; i++) {
            builder.line(topIndices[i], topIndices[(i + 1) % sides]);
        }
        
        // Bottom ring
        for (int i = 0; i < sides; i++) {
            builder.line(bottomIndices[i], bottomIndices[(i + 1) % sides]);
        }
        
        // Vertical edges
        for (int i = 0; i < sides; i++) {
            builder.line(topIndices[i], bottomIndices[i]);
        }
        
        return builder.build();
    }
    
    public static final class Builder {
        private int sides = 8;
        private float radius = 1.0f;
        private float height = 1.0f;
        private float yOffset = 0.0f;
        private boolean caps = true;
        private boolean wireframe = false;
        
        private Builder() {}
        
        public Builder sides(int sides) {
            this.sides = Math.max(3, sides);
            return this;
        }
        
        public Builder radius(float radius) {
            this.radius = radius;
            return this;
        }
        
        public Builder height(float height) {
            this.height = height;
            return this;
        }
        
        public Builder yOffset(float offset) {
            this.yOffset = offset;
            return this;
        }
        
        public Builder caps(boolean caps) {
            this.caps = caps;
            return this;
        }
        
        public Builder wireframe(boolean wireframe) {
            this.wireframe = wireframe;
            return this;
        }
        
        public PrismTessellator_old build() {
            return new PrismTessellator_old(this);
        }
        
        public Mesh tessellate(int detail) {
            return build().tessellate(detail);
        }
    }
}
