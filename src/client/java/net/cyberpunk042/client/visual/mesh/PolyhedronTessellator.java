package net.cyberpunk042.client.visual.mesh;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.pattern.TrianglePattern;
import net.cyberpunk042.visual.shape.PolyhedronShape;
import net.cyberpunk042.visual.shape.PolyType;

/**
 * Tessellates Platonic solid shapes into triangle meshes.
 * 
 * <h2>The Five Platonic Solids</h2>
 * <p>A Platonic solid is a 3D shape where every face is an identical regular polygon,
 * and the same number of faces meet at every vertex.</p>
 * 
 * <table>
 *   <tr><th>Type</th><th>Faces</th><th>Edges</th><th>Vertices</th><th>Face Shape</th></tr>
 *   <tr><td>Tetrahedron</td><td>4</td><td>6</td><td>4</td><td>Triangle</td></tr>
 *   <tr><td>Cube</td><td>6</td><td>12</td><td>8</td><td>Square</td></tr>
 *   <tr><td>Octahedron</td><td>8</td><td>12</td><td>6</td><td>Triangle</td></tr>
 *   <tr><td>Dodecahedron</td><td>12</td><td>30</td><td>20</td><td>Pentagon</td></tr>
 *   <tr><td>Icosahedron</td><td>20</td><td>30</td><td>12</td><td>Triangle</td></tr>
 * </table>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // From shape
 * Mesh mesh = PolyhedronTessellator.fromShape(polyhedronShape).tessellate(0);
 * 
 * // From builder
 * Mesh mesh = PolyhedronTessellator.builder()
 *     .polyType(PolyType.ICOSAHEDRON)
 *     .radius(1.0f)
 *     .build()
 *     .tessellate(0);
 * </pre>
 * 
 * @see PolyhedronShape
 * @see PolyType
 */
public final class PolyhedronTessellator implements Tessellator {
    
    // =========================================================================
    // Mathematical Constants
    // =========================================================================
    
    /** Golden ratio φ = (1 + √5) / 2 ≈ 1.618 */
    public static final float PHI = (1.0f + (float) Math.sqrt(5)) / 2.0f;
    
    /** Inverse golden ratio 1/φ ≈ 0.618 */
    public static final float INV_PHI = 1.0f / PHI;
    
    /** √3 - Used for tetrahedron scaling */
    public static final float SQRT_3 = (float) Math.sqrt(3);
    
    /** √(1 + φ²) - Used for icosahedron vertex normalization */
    public static final float ICOSA_NORM = (float) Math.sqrt(1 + PHI * PHI);
    
    // =========================================================================
    // Vertex Index Constants (for readability)
    // =========================================================================
    
    /**
     * Vertex indices for an octahedron.
     * <pre>
     *       TOP (0)
     *        /|\
     *       / | \
     *    +X(2)-+-+Z(4)
     *       \ | /
     *        \|/
     *     BOTTOM (1)
     * 
     * Axis vertices:
     *   0 = +Y (top)
     *   1 = -Y (bottom)  
     *   2 = +X
     *   3 = -X
     *   4 = +Z
     *   5 = -Z
     * </pre>
     */
    private enum OctaVertex {
        TOP(0), BOTTOM(1), PLUS_X(2), MINUS_X(3), PLUS_Z(4), MINUS_Z(5);
        
        final int index;
        OctaVertex(int index) { this.index = index; }
    }
    
    /**
     * Cube vertex positions.
     * <pre>
     *     3────7
     *    /|   /|
     *   2─+──6 |
     *   | 0──+-4
     *   |/   |/
     *   1────5
     * 
     * Back face (z-): 0,1,2,3
     * Front face (z+): 4,5,6,7
     * </pre>
     */
    private enum CubeVertex {
        BACK_BOTTOM_LEFT(0),   // -x, -y, -z
        BACK_BOTTOM_RIGHT(1),  // +x, -y, -z
        BACK_TOP_RIGHT(2),     // +x, +y, -z
        BACK_TOP_LEFT(3),      // -x, +y, -z
        FRONT_BOTTOM_LEFT(4),  // -x, -y, +z
        FRONT_BOTTOM_RIGHT(5), // +x, -y, +z
        FRONT_TOP_RIGHT(6),    // +x, +y, +z
        FRONT_TOP_LEFT(7);     // -x, +y, +z
        
        final int index;
        CubeVertex(int index) { this.index = index; }
    }
    
    /**
     * Cube face identifiers with their normal directions.
     */
    private enum CubeFace {
        BACK(0, 0, -1),    // -Z
        FRONT(0, 0, 1),    // +Z
        LEFT(-1, 0, 0),    // -X
        RIGHT(1, 0, 0),    // +X
        TOP(0, 1, 0),      // +Y
        BOTTOM(0, -1, 0);  // -Y
        
        final float nx, ny, nz;
        CubeFace(float nx, float ny, float nz) {
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
        }
        
        float[] normal() { return new float[]{nx, ny, nz}; }
    }
    
    // =========================================================================
    // Fields
    // =========================================================================
    
    private final PolyType polyType;
    private final float radius;
    private final int subdivisions;
    private final VertexPattern pattern;
    
    // =========================================================================
    // Constructor & Factory
    // =========================================================================
    
    private PolyhedronTessellator(Builder builder) {
        this.polyType = builder.polyType;
        this.radius = builder.radius;
        this.subdivisions = builder.subdivisions;
        this.pattern = builder.pattern != null ? builder.pattern : TrianglePattern.DEFAULT;
    }
    
    /**
     * Creates a builder for configuring the tessellator.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a tessellator directly from a shape definition.
     * @param shape The polyhedron shape to tessellate
     * @return Configured tessellator
     */
    public static PolyhedronTessellator fromShape(PolyhedronShape shape) {
        return builder()
            .polyType(shape.polyType())
            .radius(shape.radius())
            .subdivisions(shape.subdivisions())
            .build();
    }
    
    // =========================================================================
    // Tessellator Interface
    // =========================================================================
    
    @Override
    public Mesh tessellate(int detail) {
        Logging.RENDER.topic("tessellate")
            .kv("type", polyType)
            .kv("radius", radius)
            .kv("subdivisions", subdivisions)
            .debug("Tessellating polyhedron");
        
        // Dispatch to type-specific tessellation
        Mesh baseMesh = switch (polyType) {
            case TETRAHEDRON -> tessellateTetrahedron();
            case CUBE -> tessellateCube();
            case OCTAHEDRON -> tessellateOctahedron();
            case ICOSAHEDRON -> tessellateIcosahedron();
            case DODECAHEDRON -> tessellateDodecahedron();
        };
        
        // Apply subdivision if requested (for geodesic spheres)
        if (subdivisions > 0) {
            return subdivide(baseMesh, subdivisions);
        }
        
        return baseMesh;
    }
    
    @Override
    public int defaultDetail() {
        return 0; // Polyhedra use subdivisions, not detail level
    }
    
    @Override
    public int minDetail() {
        return 0;
    }
    
    @Override
    public int maxDetail() {
        return 8; // Maximum subdivision levels
    }
    
    // =========================================================================
    // TETRAHEDRON (4 triangular faces)
    // =========================================================================
    
    /**
     * Generates a tetrahedron mesh.
     * 
     * <p>A tetrahedron has 4 vertices at alternating corners of a cube.
     * When centered at origin with circumradius R, vertices are at (±s, ±s, ±s)
     * where s = R/√3, using alternating sign combinations.</p>
     * 
     * <pre>
     * Vertex layout (alternating cube corners):
     *        0 (+,+,+)
     *       /|\
     *      / | \
     *     /  |  \
     *    /   |   \
     *   3----+----1
     *  (-,-,+)   (+,-,-)
     *        \   /
     *         \ /
     *          2
     *       (-,+,-)
     * </pre>
     */
    private Mesh tessellateTetrahedron() {
        MeshBuilder builder = MeshBuilder.triangles();
        
        // Scale factor: circumradius to vertex coordinate
        float s = radius / SQRT_3;
        
        // 4 vertices at alternating corners of a cube
        float[][] vertices = {
            { s,  s,  s},   // 0: (+,+,+)
            { s, -s, -s},   // 1: (+,-,-)
            {-s,  s, -s},   // 2: (-,+,-)
            {-s, -s,  s}    // 3: (-,-,+)
        };
        
        // 4 triangular faces (pattern will handle vertex ordering)
        int[][] faces = {
            {0, 1, 2},  // Face 0: opposite to vertex 3
            {0, 3, 1},  // Face 1: opposite to vertex 2
            {0, 2, 3},  // Face 2: opposite to vertex 1
            {1, 3, 2}   // Face 3: opposite to vertex 0 (base)
        };
        
        // Pre-computed face normals (pointing AWAY from the opposite vertex)
        // Normal = normalized (v0 + v1 + v2) / 3 - oppositeVertex direction
        float n = 1.0f / (float) Math.sqrt(3.0); // 1/√3 for equal xyz components
        float[][] faceNormals = {
            { n,  n, -n},   // Face 0 points toward (+,+,-), away from vertex 3 at (-,-,+)
            { n, -n,  n},   // Face 1 points toward (+,-,+), away from vertex 2 at (-,+,-)
            {-n,  n,  n},   // Face 2 points toward (-,+,+), away from vertex 1 at (+,-,-)
            {-n, -n, -n}    // Face 3 points toward (-,-,-), away from vertex 0 at (+,+,+)
        };
        
        // Emit each face using emitCellFromPattern for proper pattern support
        int totalFaces = faces.length;
        for (int i = 0; i < totalFaces; i++) {
            if (!pattern.shouldRender(i, totalFaces)) {
                continue;
            }
            
            // Get vertex positions
            float[] v0 = vertices[faces[i][0]];
            float[] v1 = vertices[faces[i][1]];
            float[] v2 = vertices[faces[i][2]];
            float[] faceN = faceNormals[i];
            
            // Create front-facing vertices
            int i0 = emitVertex(builder, v0, faceN, 0, 0);
            int i1 = emitVertex(builder, v1, faceN, 1, 0);
            int i2 = emitVertex(builder, v2, faceN, 1, 1);
            
            // Use emitCellFromPattern for proper vertex ordering
            builder.emitCellFromPattern(new int[]{i0, i1, i2}, pattern);
            
            // Also emit back-facing (reversed winding)
            float[] invN = {-faceN[0], -faceN[1], -faceN[2]};
            int j0 = emitVertex(builder, v0, invN, 0, 0);
            int j1 = emitVertex(builder, v2, invN, 1, 1); // Note: swapped v1/v2 for reverse winding
            int j2 = emitVertex(builder, v1, invN, 1, 0);
            
            builder.emitCellFromPattern(new int[]{j0, j1, j2}, pattern);
        }
        
        return builder.build();
    }
    
    // =========================================================================
    // CUBE (6 square faces)
    // =========================================================================
    
    /**
     * Generates a cube mesh using QUADS for cleaner wireframe.
     * 
     * <p>A cube has 8 vertices and 6 square faces. Using QUADS means wireframe
     * draws only the 4 edges per face (no diagonal cut). Solid rendering
     * converts quads to triangles automatically.</p>
     */
    private Mesh tessellateCube() {
        MeshBuilder builder = MeshBuilder.quads();
        
        // Scale to inscribed radius (vertex distance from center)
        float r = radius / SQRT_3;
        // 8 vertices indexed by CubeVertex enum
        float[][] vertices = {
            {-r, -r, -r},  // BACK_BOTTOM_LEFT
            { r, -r, -r},  // BACK_BOTTOM_RIGHT
            { r,  r, -r},  // BACK_TOP_RIGHT
            {-r,  r, -r},  // BACK_TOP_LEFT
            {-r, -r,  r},  // FRONT_BOTTOM_LEFT
            { r, -r,  r},  // FRONT_BOTTOM_RIGHT
            { r,  r,  r},  // FRONT_TOP_RIGHT
            {-r,  r,  r}   // FRONT_TOP_LEFT
        };
        
        // Define faces as arrays for pattern support (6 faces)
        CubeFace[] faces = { CubeFace.BACK, CubeFace.FRONT, CubeFace.LEFT, 
                             CubeFace.RIGHT, CubeFace.TOP, CubeFace.BOTTOM };
        int[][] faceIndices = {
            // BACK face (-Z): counter-clockwise when viewed from -Z
            {CubeVertex.BACK_BOTTOM_RIGHT.index, CubeVertex.BACK_BOTTOM_LEFT.index,
             CubeVertex.BACK_TOP_LEFT.index, CubeVertex.BACK_TOP_RIGHT.index},
            // FRONT face (+Z)
            {CubeVertex.FRONT_BOTTOM_LEFT.index, CubeVertex.FRONT_BOTTOM_RIGHT.index,
             CubeVertex.FRONT_TOP_RIGHT.index, CubeVertex.FRONT_TOP_LEFT.index},
            // LEFT face (-X)
            {CubeVertex.BACK_BOTTOM_LEFT.index, CubeVertex.FRONT_BOTTOM_LEFT.index,
             CubeVertex.FRONT_TOP_LEFT.index, CubeVertex.BACK_TOP_LEFT.index},
            // RIGHT face (+X)
            {CubeVertex.FRONT_BOTTOM_RIGHT.index, CubeVertex.BACK_BOTTOM_RIGHT.index,
             CubeVertex.BACK_TOP_RIGHT.index, CubeVertex.FRONT_TOP_RIGHT.index},
            // TOP face (+Y)
            {CubeVertex.FRONT_TOP_LEFT.index, CubeVertex.FRONT_TOP_RIGHT.index,
             CubeVertex.BACK_TOP_RIGHT.index, CubeVertex.BACK_TOP_LEFT.index},
            // BOTTOM face (-Y)
            {CubeVertex.BACK_BOTTOM_LEFT.index, CubeVertex.BACK_BOTTOM_RIGHT.index,
             CubeVertex.FRONT_BOTTOM_RIGHT.index, CubeVertex.FRONT_BOTTOM_LEFT.index}
        };
        
        // Emit faces with pattern support
        int totalFaces = faces.length;
        for (int i = 0; i < totalFaces; i++) {
            if (!pattern.shouldRender(i, totalFaces)) {
                continue;
            }
            emitCubeQuad(builder, vertices, faces[i], faceIndices[i]);
        }
        
        return builder.build();
    }
    
    /**
     * Emits a cube face as a quad (both windings for double-sided visibility).
     * @param builder Mesh builder
     * @param vertices Vertex array
     * @param face Face enum with normal
     * @param indices 4 vertex indices forming the quad
     */
    private void emitCubeQuad(MeshBuilder builder, float[][] vertices, 
                               CubeFace face, int[] indices) {
        float[] n = face.normal();
        
        // Get the 4 vertices
        float[] v0 = vertices[indices[0]];
        float[] v1 = vertices[indices[1]];
        float[] v2 = vertices[indices[2]];
        float[] v3 = vertices[indices[3]];
        
        // Emit front-facing quad: v0→v1→v2→v3
        int i0 = emitVertex(builder, v0, n, 0, 0);
        int i1 = emitVertex(builder, v1, n, 1, 0);
        int i2 = emitVertex(builder, v2, n, 1, 1);
        int i3 = emitVertex(builder, v3, n, 0, 1);
        builder.quad(i0, i1, i2, i3);
        
        // Also emit back-facing quad (reversed winding)
        float[] invN = {-n[0], -n[1], -n[2]};
        int j0 = emitVertex(builder, v0, invN, 0, 0);
        int j1 = emitVertex(builder, v3, invN, 0, 1);
        int j2 = emitVertex(builder, v2, invN, 1, 1);
        int j3 = emitVertex(builder, v1, invN, 1, 0);
        builder.quad(j0, j1, j2, j3);
    }
    
    // =========================================================================
    // OCTAHEDRON (8 triangular faces)
    // =========================================================================
    
    /**
     * Generates an octahedron mesh.
     * 
     * <p>An octahedron has 6 vertices (one on each axis) and 8 triangular faces.
     * It's the dual of a cube - vertices where cube has face centers.</p>
     * 
     * <pre>
     *       +Y (top)
     *        |
     *   -X---+---+X
     *       /|\
     *      / | \
     *    +Z  |  -Z
     *        |
     *       -Y (bottom)
     * </pre>
     */
    private Mesh tessellateOctahedron() {
        MeshBuilder builder = MeshBuilder.triangles();
        float r = radius;
        
        // 6 vertex POSITIONS (like cube stores positions)
        float[][] vertices = {
            {0,  r, 0},   // 0: TOP (+Y)
            {0, -r, 0},   // 1: BOTTOM (-Y)
            { r, 0, 0},   // 2: +X
            {-r, 0, 0},   // 3: -X
            {0, 0,  r},   // 4: +Z
            {0, 0, -r}    // 5: -Z
        };
        
        // 8 faces as vertex index triplets (pattern will handle vertex ordering)
        int[][] faces = {
            // Top pyramid (4 faces)
            {0, 4, 2},   // TOP, +Z, +X
            {0, 2, 5},   // TOP, +X, -Z
            {0, 5, 3},   // TOP, -Z, -X
            {0, 3, 4},   // TOP, -X, +Z
            
            // Bottom pyramid (4 faces)
            {1, 2, 4},   // BOTTOM, +X, +Z
            {1, 5, 2},   // BOTTOM, -Z, +X
            {1, 3, 5},   // BOTTOM, -X, -Z
            {1, 4, 3}    // BOTTOM, +Z, -X
        };
        
        // Pre-computed face normals (each face points diagonally outward)
        float n = 1.0f / (float) Math.sqrt(3.0); // 1/√3
        float[][] faceNormals = {
            // Top pyramid normals
            { n,  n,  n},   // face 0: (+,+,+)
            { n,  n, -n},   // face 1: (+,+,-)
            {-n,  n, -n},   // face 2: (-,+,-)
            {-n,  n,  n},   // face 3: (-,+,+)
            // Bottom pyramid normals
            { n, -n,  n},   // face 4: (+,-,+)
            { n, -n, -n},   // face 5: (+,-,-)
            {-n, -n, -n},   // face 6: (-,-,-)
            {-n, -n,  n}    // face 7: (-,-,+)
        };
        
        // Emit each face using emitCellFromPattern for proper pattern support
        int totalFaces = faces.length;
        for (int i = 0; i < totalFaces; i++) {
            if (!pattern.shouldRender(i, totalFaces)) {
                continue;
            }
            
            // Get vertex positions
            float[] v0 = vertices[faces[i][0]];
            float[] v1 = vertices[faces[i][1]];
            float[] v2 = vertices[faces[i][2]];
            float[] faceN = faceNormals[i];
            
            // Create front-facing vertices
            int i0 = emitVertex(builder, v0, faceN, 0, 0);
            int i1 = emitVertex(builder, v1, faceN, 1, 0);
            int i2 = emitVertex(builder, v2, faceN, 1, 1);
            
            // Use emitCellFromPattern for proper vertex ordering
            builder.emitCellFromPattern(new int[]{i0, i1, i2}, pattern);
            
            // Also emit back-facing (reversed winding)
            float[] invN = {-faceN[0], -faceN[1], -faceN[2]};
            int j0 = emitVertex(builder, v0, invN, 0, 0);
            int j1 = emitVertex(builder, v2, invN, 1, 1); // swapped
            int j2 = emitVertex(builder, v1, invN, 1, 0); // swapped
            
            builder.emitCellFromPattern(new int[]{j0, j1, j2}, pattern);
        }
        
        return builder.build();
    }
    
    // =========================================================================
    // ICOSAHEDRON (20 triangular faces)
    // =========================================================================
    
    /**
     * Generates an icosahedron mesh.
     * 
     * <p>An icosahedron has 12 vertices and 20 triangular faces. It's constructed
     * from 3 mutually perpendicular golden rectangles (rectangles with aspect
     * ratio φ:1 where φ is the golden ratio).</p>
     * 
     * <p>The 12 vertices are at corners of these 3 rectangles:</p>
     * <ul>
     *   <li>XY plane: 4 vertices</li>
     *   <li>YZ plane: 4 vertices</li>
     *   <li>XZ plane: 4 vertices</li>
     * </ul>
     */
    private Mesh tessellateIcosahedron() {
        MeshBuilder builder = MeshBuilder.triangles();
        
        // Normalize vertices to circumradius
        float scale = radius / ICOSA_NORM;
        float a = scale;        // Short edge of golden rectangle
        float b = scale * PHI;  // Long edge of golden rectangle
        
        // 12 vertices from 3 orthogonal golden rectangles
        float[][] vertices = {
            // YZ plane rectangle (x = 0)
            {0,  a,  b},   // 0: (0, +1, +φ)
            {0,  a, -b},   // 1: (0, +1, -φ)
            {0, -a,  b},   // 2: (0, -1, +φ)
            {0, -a, -b},   // 3: (0, -1, -φ)
            
            // XY plane rectangle (z = 0)
            { a,  b, 0},   // 4: (+1, +φ, 0)
            { a, -b, 0},   // 5: (+1, -φ, 0)
            {-a,  b, 0},   // 6: (-1, +φ, 0)
            {-a, -b, 0},   // 7: (-1, -φ, 0)
            
            // XZ plane rectangle (y = 0)
            { b, 0,  a},   // 8:  (+φ, 0, +1)
            {-b, 0,  a},   // 9:  (-φ, 0, +1)
            { b, 0, -a},   // 10: (+φ, 0, -1)
            {-b, 0, -a}    // 11: (-φ, 0, -1)
        };
        
        // 20 triangular faces - verified against standard icosahedron geometry
        int[][] faces = {
            // 5 triangles around top vertex 0 (0, +1, +φ)
            {0, 2, 8},   // connects to 2, 8
            {0, 8, 4},   // connects to 8, 4
            {0, 4, 6},   // connects to 4, 6
            {0, 6, 9},   // connects to 6, 9
            {0, 9, 2},   // connects to 9, 2
            
            // 5 triangles around bottom vertex 3 (0, -1, -φ)
            {3, 10, 5},  // connects to 10, 5
            {3, 5, 7},   // connects to 5, 7
            {3, 7, 11},  // connects to 7, 11
            {3, 11, 1},  // connects to 11, 1
            {3, 1, 10},  // connects to 1, 10
            
            // 10 triangles around the middle band
            {2, 5, 8},   // top left + bottom
            {8, 5, 10},  // right side
            {8, 10, 4},  // upper right
            {4, 10, 1},  // right + back
            {4, 1, 6},   // upper back
            {6, 1, 11},  // back left
            {6, 11, 9},  // left back
            {9, 11, 7},  // left bottom
            {9, 7, 2},   // lower left
            {2, 7, 5}    // bottom front
        };
        
        // Generate triangles with pattern support
        int totalFaces = faces.length;
        for (int i = 0; i < totalFaces; i++) {
            emitTriangle(builder, vertices, faces[i], i, totalFaces);
        }
        
        return builder.build();
    }
    
    // =========================================================================
    // DODECAHEDRON (12 pentagonal faces)
    // =========================================================================
    
    /**
     * Generates a dodecahedron mesh.
     * 
     * <p>A dodecahedron has 20 vertices and 12 pentagonal faces. Since we render
     * triangles, each pentagon is tessellated into 5 triangles from its center.</p>
     * 
     * <p>The 20 vertices come from:</p>
     * <ul>
     *   <li>8 vertices of a cube</li>
     *   <li>12 vertices from 3 orthogonal golden rectangles</li>
     * </ul>
     */
    private Mesh tessellateDodecahedron() {
        MeshBuilder builder = MeshBuilder.triangles();
        
        // Dodecahedron vertices - mathematically verified via Python
        float s = radius / (float) Math.sqrt(3.0);
        float phi = PHI;         // ≈ 1.618
        float invPhi = INV_PHI;  // ≈ 0.618
        
        // 20 vertices (computed via Python with numpy)
        float[][] vertices = {
            {-s, -s, -s},  // 0
            {-s, -s,  s},  // 1
            {-s,  s, -s},  // 2
            {-s,  s,  s},  // 3
            { s, -s, -s},  // 4
            { s, -s,  s},  // 5
            { s,  s, -s},  // 6
            { s,  s,  s},  // 7
            {0, -invPhi * s, -phi * s},  // 8
            {0, -invPhi * s,  phi * s},  // 9
            {0,  invPhi * s, -phi * s},  // 10
            {0,  invPhi * s,  phi * s},  // 11
            {-invPhi * s, -phi * s, 0},  // 12
            {-invPhi * s,  phi * s, 0},  // 13
            { invPhi * s, -phi * s, 0},  // 14
            { invPhi * s,  phi * s, 0},  // 15
            {-phi * s, 0, -invPhi * s},  // 16
            {-phi * s, 0,  invPhi * s},  // 17
            { phi * s, 0, -invPhi * s},  // 18
            { phi * s, 0,  invPhi * s},  // 19
        };
        
        // 12 pentagonal faces (mathematically verified via Python DFS)
        int[][] pentagons = {
            { 0,  8,  4, 14, 12},  // Face 0
            { 0,  8, 10,  2, 16},  // Face 1
            { 0, 12,  1, 17, 16},  // Face 2
            { 1,  9,  5, 14, 12},  // Face 3
            { 1,  9, 11,  3, 17},  // Face 4
            { 2, 10,  6, 15, 13},  // Face 5
            { 2, 13,  3, 17, 16},  // Face 6
            { 3, 11,  7, 15, 13},  // Face 7
            { 4,  8, 10,  6, 18},  // Face 8
            { 4, 14,  5, 19, 18},  // Face 9
            { 5,  9, 11,  7, 19},  // Face 10
            { 6, 15,  7, 19, 18},  // Face 11
        };
        
        // Tessellate each pentagon from its center with pattern support
        // Skip pattern filtering when subdividing - we want solid base mesh
        int totalPentagons = pentagons.length;
        for (int i = 0; i < totalPentagons; i++) {
            if (subdivisions == 0 && !pattern.shouldRender(i, totalPentagons)) {
                continue;
            }
            emitPentagon(builder, vertices, pentagons[i]);
        }
        
        return builder.build();
    }
    
    /**
     * Emits a pentagon as 5 triangles radiating from center.
     * Uses emitCellFromPattern for proper pattern vertex ordering.
     */
    private void emitPentagon(MeshBuilder builder, float[][] vertices, int[] indices) {
        // Calculate pentagon center
        float cx = 0, cy = 0, cz = 0;
        for (int idx : indices) {
            cx += vertices[idx][0];
            cy += vertices[idx][1];
            cz += vertices[idx][2];
        }
        cx /= 5; cy /= 5; cz /= 5;
        float[] center = {cx, cy, cz};
        
        // Calculate face normal from first 3 vertices
        float[] v0 = vertices[indices[0]];
        float[] v1 = vertices[indices[1]];
        float[] v2 = vertices[indices[2]];
        float[] normal = crossProduct(subtract(v1, v0), subtract(v2, v0));
        normalize(normal);
        
        // Emit 5 triangles from center to each edge
        for (int i = 0; i < 5; i++) {
            int idx0 = indices[i];
            int idx1 = indices[(i + 1) % 5];
            
            // Front-facing vertices: center → idx0 → idx1
            int vi0 = emitVertex(builder, center, normal, 0.5f, 0.5f);
            int vi1 = emitVertex(builder, vertices[idx0], normal, 0, 0);
            int vi2 = emitVertex(builder, vertices[idx1], normal, 1, 0);
            builder.emitCellFromPattern(new int[]{vi0, vi1, vi2}, pattern);
            
            // Back-facing vertices: center → idx1 → idx0 (reversed)
            float[] invN = {-normal[0], -normal[1], -normal[2]};
            int vj0 = emitVertex(builder, center, invN, 0.5f, 0.5f);
            int vj1 = emitVertex(builder, vertices[idx1], invN, 1, 0);
            int vj2 = emitVertex(builder, vertices[idx0], invN, 0, 0);
            builder.emitCellFromPattern(new int[]{vj0, vj1, vj2}, pattern);
        }
    }
    
    // =========================================================================
    // Subdivision (Geodesic Sphere Effect)
    // =========================================================================
    
    /**
     * Subdivides triangles for smoother appearance (geodesic subdivision).
     * 
     * <p>Each level of subdivision splits every triangle into 4 smaller triangles
     * by adding midpoint vertices on each edge and projecting them onto the
     * circumscribed sphere.</p>
     * 
     * <h3>Subdivision Levels for Icosahedron</h3>
     * <table>
     *   <tr><th>Level</th><th>Faces</th><th>Vertices</th><th>Use Case</th></tr>
     *   <tr><td>0</td><td>20</td><td>12</td><td>Low poly style</td></tr>
     *   <tr><td>1</td><td>80</td><td>42</td><td>Distant objects</td></tr>
     *   <tr><td>2</td><td>320</td><td>162</td><td>Medium distance</td></tr>
     *   <tr><td>3</td><td>1,280</td><td>642</td><td>Close-up</td></tr>
     *   <tr><td>4</td><td>5,120</td><td>2,562</td><td>Very smooth</td></tr>
     * </table>
     * 
     * <h3>Algorithm</h3>
     * <pre>
     * Original triangle:
     *        v0
     *       /  \
     *      /    \
     *    v1 ---- v2
     * 
     * After 1 subdivision:
     *        v0
     *       / \
     *     m01--m02
     *     / \ / \
     *   v1--m12--v2
     * 
     * Creates 4 triangles: (v0,m01,m02), (m01,v1,m12), (m02,m01,m12), (m02,m12,v2)
     * </pre>
     * 
     * @param mesh Base mesh to subdivide
     * @param levels Number of subdivision iterations (1-4 recommended)
     * @return Subdivided mesh with vertices projected onto sphere
     */
    private Mesh subdivide(Mesh mesh, int levels) {
        if (levels <= 0) return mesh;
        
        Logging.RENDER.topic("tessellate")
            .kv("levels", levels)
            .kv("initialFaces", mesh.primitiveCount())
            .debug("Starting geodesic subdivision");
        
        Mesh result = mesh;
        
        // Apply subdivision iteratively
        for (int level = 0; level < levels; level++) {
            result = subdivideOnce(result);
        }
        
        Logging.RENDER.topic("tessellate")
            .kv("finalFaces", result.primitiveCount())
            .debug("Subdivision complete");
        
        return result;
    }
    
    /**
     * Performs a single subdivision pass.
     * 
     * <p>For each triangle ABC:</p>
     * <ol>
     *   <li>Find midpoints: M_AB, M_BC, M_CA</li>
     *   <li>Project midpoints onto circumscribed sphere</li>
     *   <li>Create 4 new triangles</li>
     * </ol>
     */
    private Mesh subdivideOnce(Mesh mesh) {
        MeshBuilder builder = MeshBuilder.triangles();
        
        // Handle QUADS by treating each quad as 2 triangles
        if (mesh.primitiveType() == PrimitiveType.QUADS) {
            mesh.forEachQuad((v0, v1, v2, v3) -> {
                // Quad becomes 2 triangles: (v0,v1,v2) and (v0,v2,v3)
                subdivideTriangle(builder, v0, v1, v2);
                subdivideTriangle(builder, v0, v2, v3);
            });
        } else {
            // Process each triangle
            mesh.forEachTriangle((v0, v1, v2) -> {
                subdivideTriangle(builder, v0, v1, v2);
            });
        }
        
        return builder.build();
    }
    
    /**
     * Subdivides a single triangle into 4 triangles.
     */
    private void subdivideTriangle(MeshBuilder builder, Vertex v0, Vertex v1, Vertex v2) {
        // Calculate edge midpoints
        Vertex m01 = midpoint(v0, v1);  // Midpoint of edge v0-v1
        Vertex m12 = midpoint(v1, v2);  // Midpoint of edge v1-v2
        Vertex m20 = midpoint(v2, v0);  // Midpoint of edge v2-v0
        
        // Project midpoints onto sphere surface (normalize to radius)
        m01 = projectToSphere(m01);
        m12 = projectToSphere(m12);
        m20 = projectToSphere(m20);
        
        // Emit 4 new triangles
        // Triangle 1: Top corner (v0, m01, m20)
        emitSubdividedTriangle(builder, v0, m01, m20);
        
        // Triangle 2: Left corner (m01, v1, m12)
        emitSubdividedTriangle(builder, m01, v1, m12);
        
        // Triangle 3: Center (m01, m12, m20)
        emitSubdividedTriangle(builder, m01, m12, m20);
        
        // Triangle 4: Right corner (m20, m12, v2)
        emitSubdividedTriangle(builder, m20, m12, v2);
    }
    
    /**
     * Calculates the midpoint between two vertices.
     * 
     * @param a First vertex
     * @param b Second vertex
     * @return New vertex at midpoint with interpolated UV
     */
    private Vertex midpoint(Vertex a, Vertex b) {
        // Average position
        float mx = (a.x() + b.x()) / 2;
        float my = (a.y() + b.y()) / 2;
        float mz = (a.z() + b.z()) / 2;
        
        // Average UV (for texture mapping)
        float mu = (a.u() + b.u()) / 2;
        float mv = (a.v() + b.v()) / 2;
        
        // Normal will be recalculated when projected to sphere
        return new Vertex(mx, my, mz, 0, 0, 0, mu, mv);
    }
    
    /**
     * Projects a vertex onto the circumscribed sphere.
     * 
     * <p>This is what creates the geodesic effect: midpoints are pushed
     * outward to lie on the sphere surface.</p>
     * 
     * @param v Vertex to project
     * @return New vertex at radius distance from origin, with normal = direction
     */
    private Vertex projectToSphere(Vertex v) {
        float x = v.x();
        float y = v.y();
        float z = v.z();
        
        // Calculate distance from origin
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        
        if (length < 0.0001f) {
            // Degenerate case: vertex at origin
            return v;
        }
        
        // Scale to radius (normalize then multiply)
        float scale = radius / length;
        float px = x * scale;
        float py = y * scale;
        float pz = z * scale;
        
        // Normal = direction from origin (same as normalized position)
        float nx = x / length;
        float ny = y / length;
        float nz = z / length;
        
        return new Vertex(px, py, pz, nx, ny, nz, v.u(), v.v());
    }
    
    /**
     * Emits a triangle from 3 vertices to the mesh builder.
     */
    private void emitSubdividedTriangle(MeshBuilder builder, Vertex v0, Vertex v1, Vertex v2) {
        int i0 = builder.vertex(v0.x(), v0.y(), v0.z(), v0.nx(), v0.ny(), v0.nz(), v0.u(), v0.v());
        int i1 = builder.vertex(v1.x(), v1.y(), v1.z(), v1.nx(), v1.ny(), v1.nz(), v1.u(), v1.v());
        int i2 = builder.vertex(v2.x(), v2.y(), v2.z(), v2.nx(), v2.ny(), v2.nz(), v2.u(), v2.v());
        builder.triangle(i0, i1, i2);  // Must create the triangle primitive!
    }
    
    // =========================================================================
    // Vertex Emission Helpers
    // =========================================================================
    
    /**
     * Emits a triangle with both windings (double-sided visibility).
     * Uses emitCellFromPattern for proper pattern vertex ordering.
     */
    private void emitTriangle(MeshBuilder builder, float[][] vertices, int[] face,
                               int faceIndex, int totalFaces) {
        // When subdividing, don't filter by pattern - we want a solid base mesh
        // Pattern filtering only makes sense for non-subdivided meshes
        if (subdivisions == 0 && !pattern.shouldRender(faceIndex, totalFaces)) {
            return;
        }
        
        float[] v0 = vertices[face[0]];
        float[] v1 = vertices[face[1]];
        float[] v2 = vertices[face[2]];
        
        // Calculate face normal via cross product
        float[] normal = crossProduct(subtract(v1, v0), subtract(v2, v0));
        normalize(normal);
        
        // Create front-facing vertices
        int i0 = emitVertex(builder, v0, normal, 0, 0);
        int i1 = emitVertex(builder, v1, normal, 1, 0);
        int i2 = emitVertex(builder, v2, normal, 1, 1);
        
        // Use emitCellFromPattern for proper vertex ordering
        builder.emitCellFromPattern(new int[]{i0, i1, i2}, pattern);
        
        // Also emit back-facing (reversed winding)
        float[] invNormal = {-normal[0], -normal[1], -normal[2]};
        int j0 = emitVertex(builder, v0, invNormal, 0, 0);
        int j1 = emitVertex(builder, v2, invNormal, 1, 1); // swapped
        int j2 = emitVertex(builder, v1, invNormal, 1, 0); // swapped
        
        builder.emitCellFromPattern(new int[]{j0, j1, j2}, pattern);
    }
    
    /**
     * Emits a single vertex with position, normal, and UV.
     */
    private int emitVertex(MeshBuilder builder, float[] pos, float[] normal, float u, float v) {
        return builder.vertex(pos[0], pos[1], pos[2], normal[0], normal[1], normal[2], u, v);
    }
    
    // =========================================================================
    // Vector Math Utilities
    // =========================================================================
    
    private float[] subtract(float[] a, float[] b) {
        return new float[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }
    
    private float[] crossProduct(float[] a, float[] b) {
        return new float[]{
            a[1] * b[2] - a[2] * b[1],  // x = ay*bz - az*by
            a[2] * b[0] - a[0] * b[2],  // y = az*bx - ax*bz
            a[0] * b[1] - a[1] * b[0]   // z = ax*by - ay*bx
        };
    }
    
    private void normalize(float[] v) {
        float length = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (length > 0.0001f) {
            v[0] /= length;
            v[1] /= length;
            v[2] /= length;
        }
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static final class Builder {
        private PolyType polyType = PolyType.ICOSAHEDRON;
        private float radius = 1.0f;
        private int subdivisions = 0;
        private VertexPattern pattern = null;
        
        /**
         * Sets the polyhedron type.
         * @param type One of the 5 Platonic solids
         * @see PolyType
         */
        public Builder polyType(PolyType type) {
            this.polyType = type;
            return this;
        }
        
        /**
         * Sets the circumscribed radius (distance from center to vertex).
         * @param radius Positive radius value
         */
        public Builder radius(float radius) {
            this.radius = radius;
            return this;
        }
        
        /**
         * Sets subdivision level for geodesic effect.
         * @param subdivisions 0 = no subdivision, higher = smoother
         */
        public Builder subdivisions(int subdivisions) {
            this.subdivisions = subdivisions;
            return this;
        }
        
        /**
         * Sets the vertex pattern for face rendering.
         * @param pattern Pattern for triangle visibility/winding
         */
        public Builder pattern(VertexPattern pattern) {
            this.pattern = pattern;
            return this;
        }
        
        public PolyhedronTessellator build() {
            return new PolyhedronTessellator(this);
        }
    }
}
