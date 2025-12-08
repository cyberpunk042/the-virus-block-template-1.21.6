package net.cyberpunk042.client.visual._legacy.mesh;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.shape.PolyhedronShape;

/**
 * Tessellates polyhedron shapes (cube, octahedron, icosahedron).
 * 
 * <h2>Supported Types</h2>
 * <ul>
 *   <li>CUBE - 6 faces, 8 vertices</li>
 *   <li>OCTAHEDRON - 8 faces, 6 vertices</li>
 *   <li>ICOSAHEDRON - 20 faces, 12 vertices</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * Mesh cube = PolyhedraTessellator_old.builder()
 *     .type(PolyhedronShape.Type.CUBE)
 *     .radius(1.0f)
 *     .build()
 *     .tessellate();
 * </pre>
 */
public final class PolyhedraTessellator_old  {
    
    private final PolyhedronShape.Type type;
    private final float radius;
    
    private PolyhedraTessellator_old(Builder builder) {
        this.type = builder.type;
        this.radius = builder.radius;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public Mesh tessellate() {
        Logging.RENDER.topic("tessellate").debug(
            "Tessellating polyhedron: type={}, radius={}", type, radius);
        
        return switch (type) {
            case CUBE -> tessellateCube();
            case OCTAHEDRON -> tessellateOctahedron();
            case ICOSAHEDRON -> tessellateIcosahedron();
            case TETRAHEDRON -> tessellateTetrahedron();
            case DODECAHEDRON -> tessellateDodecahedron();
        };
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Cube (6 faces)
    // ─────────────────────────────────────────────────────────────────────────
    
    private Mesh tessellateCube() {
        MeshBuilder builder = MeshBuilder.triangles();
        float r = radius;
        
        // 8 vertices of a cube
        float[][] verts = {
            {-r, -r, -r}, {r, -r, -r}, {r, r, -r}, {-r, r, -r},  // back
            {-r, -r,  r}, {r, -r,  r}, {r, r,  r}, {-r, r,  r}   // front
        };
        
        // 6 faces (each as 2 triangles)
        int[][] faces = {
            {0, 1, 2, 3}, // back
            {5, 4, 7, 6}, // front
            {4, 0, 3, 7}, // left
            {1, 5, 6, 2}, // right
            {3, 2, 6, 7}, // top
            {4, 5, 1, 0}  // bottom
        };
        
        float[][] normals = {
            {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}, {0, 1, 0}, {0, -1, 0}
        };
        
        for (int f = 0; f < 6; f++) {
            int[] face = faces[f];
            float[] n = normals[f];
            
            // Triangle 1
            addVertex(builder, verts[face[0]], n, 0, 0);
            addVertex(builder, verts[face[1]], n, 1, 0);
            addVertex(builder, verts[face[2]], n, 1, 1);
            
            // Triangle 2
            addVertex(builder, verts[face[0]], n, 0, 0);
            addVertex(builder, verts[face[2]], n, 1, 1);
            addVertex(builder, verts[face[3]], n, 0, 1);
        }
        
        return builder.build();
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Octahedron (8 faces)
    // ─────────────────────────────────────────────────────────────────────────
    
    private Mesh tessellateOctahedron() {
        MeshBuilder builder = MeshBuilder.triangles();
        float r = radius;
        
        // 6 vertices of an octahedron (along axes)
        float[][] verts = {
            {0, r, 0},   // top
            {0, -r, 0},  // bottom
            {r, 0, 0},   // +X
            {-r, 0, 0},  // -X
            {0, 0, r},   // +Z
            {0, 0, -r}   // -Z
        };
        
        // 8 triangular faces
        int[][] faces = {
            {0, 4, 2}, {0, 2, 5}, {0, 5, 3}, {0, 3, 4}, // top 4
            {1, 2, 4}, {1, 5, 2}, {1, 3, 5}, {1, 4, 3}  // bottom 4
        };
        
        for (int[] face : faces) {
            float[] v0 = verts[face[0]];
            float[] v1 = verts[face[1]];
            float[] v2 = verts[face[2]];
            
            // Calculate face normal
            float[] n = crossProduct(
                subtract(v1, v0),
                subtract(v2, v0)
            );
            normalize(n);
            
            addVertex(builder, v0, n, 0.5f, 0);
            addVertex(builder, v1, n, 0, 1);
            addVertex(builder, v2, n, 1, 1);
        }
        
        return builder.build();
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Icosahedron (20 faces)
    // ─────────────────────────────────────────────────────────────────────────
    
    private Mesh tessellateIcosahedron() {
        MeshBuilder builder = MeshBuilder.triangles();
        
        // Golden ratio
        float phi = (1.0f + (float)Math.sqrt(5)) / 2.0f;
        float r = radius / (float)Math.sqrt(1 + phi * phi);
        float a = r;
        float b = r * phi;
        
        // 12 vertices
        float[][] verts = {
            {0, a, b}, {0, a, -b}, {0, -a, b}, {0, -a, -b},
            {a, b, 0}, {a, -b, 0}, {-a, b, 0}, {-a, -b, 0},
            {b, 0, a}, {-b, 0, a}, {b, 0, -a}, {-b, 0, -a}
        };
        
        // 20 triangular faces
        int[][] faces = {
            {0, 8, 2}, {0, 2, 9}, {0, 9, 6}, {0, 6, 4}, {0, 4, 8},
            {1, 10, 4}, {1, 4, 6}, {1, 6, 11}, {1, 11, 3}, {1, 3, 10},
            {2, 8, 5}, {2, 5, 7}, {2, 7, 9}, {3, 11, 7}, {3, 7, 5},
            {3, 5, 10}, {4, 10, 8}, {5, 8, 10}, {6, 9, 11}, {7, 11, 9}
        };
        
        for (int[] face : faces) {
            float[] v0 = verts[face[0]];
            float[] v1 = verts[face[1]];
            float[] v2 = verts[face[2]];
            
            float[] n = crossProduct(
                subtract(v1, v0),
                subtract(v2, v0)
            );
            normalize(n);
            
            addVertex(builder, v0, n, 0.5f, 0);
            addVertex(builder, v1, n, 0, 1);
            addVertex(builder, v2, n, 1, 1);
        }
        
        return builder.build();
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Tetrahedron (4 faces)
    // ─────────────────────────────────────────────────────────────────────────
    
    private Mesh tessellateTetrahedron() {
        MeshBuilder builder = MeshBuilder.triangles();
        
        // Tetrahedron vertices (centered, normalized to radius)
        float s = radius / (float)Math.sqrt(3);
        float[][] verts = {
            { s,  s,  s},   // 0
            { s, -s, -s},   // 1
            {-s,  s, -s},   // 2
            {-s, -s,  s}    // 3
        };
        
        // 4 triangular faces
        int[][] faces = {
            {0, 1, 2},
            {0, 3, 1},
            {0, 2, 3},
            {1, 3, 2}
        };
        
        for (int[] face : faces) {
            float[] v0 = verts[face[0]];
            float[] v1 = verts[face[1]];
            float[] v2 = verts[face[2]];
            
            float[] n = crossProduct(subtract(v1, v0), subtract(v2, v0));
            normalize(n);
            
            addVertex(builder, v0, n, 0.5f, 0);
            addVertex(builder, v1, n, 0, 1);
            addVertex(builder, v2, n, 1, 1);
        }
        
        return builder.build();
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Dodecahedron (12 faces) - Simplified as scaled icosahedron for now
    // ─────────────────────────────────────────────────────────────────────────
    
    private Mesh tessellateDodecahedron() {
        // Dodecahedron is complex (pentagonal faces). 
        // For now, return a scaled icosahedron as approximation
        Logging.RENDER.topic("tessellate").debug(
            "Dodecahedron approximated with icosahedron");
        return tessellateIcosahedron();
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────────────────
    
    private void addVertex(MeshBuilder builder, float[] pos, float[] normal, float u, float v) {
        builder.vertex(pos[0], pos[1], pos[2], normal[0], normal[1], normal[2], u, v);
    }
    
    private float[] subtract(float[] a, float[] b) {
        return new float[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }
    
    private float[] crossProduct(float[] a, float[] b) {
        return new float[]{
            a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0]
        };
    }
    
    private void normalize(float[] v) {
        float len = (float)Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        if (len > 0.0001f) {
            v[0] /= len;
            v[1] /= len;
            v[2] /= len;
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Builder
    // ─────────────────────────────────────────────────────────────────────────
    
    public static final class Builder {
        private PolyhedronShape.Type type = PolyhedronShape.Type.CUBE;
        private float radius = 1.0f;
        
        public Builder type(PolyhedronShape.Type type) {
            this.type = type;
            return this;
        }
        
        public Builder radius(float radius) {
            this.radius = radius;
            return this;
        }
        
        public Builder fromShape(PolyhedronShape shape) {
            this.type = shape.type();
            this.radius = shape.size();
            return this;
        }
        
        public PolyhedraTessellator_old build() {
            return new PolyhedraTessellator_old(this);
        }
    }
}
