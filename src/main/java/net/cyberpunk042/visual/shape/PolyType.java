package net.cyberpunk042.visual.shape;

/**
 * Defines the types of Platonic solids for polyhedron shapes.
 * 
 * <p>Each type has a specific number of faces, edges, and vertices.
 * The dualOf() method returns the dual polyhedron (faces ↔ vertices swapped).</p>
 * 
 * <h3>Platonic Solids</h3>
 * <ul>
 *   <li>CUBE - 6 faces (squares), 12 edges, 8 vertices</li>
 *   <li>TETRAHEDRON - 4 faces (triangles), 6 edges, 4 vertices</li>
 *   <li>OCTAHEDRON - 8 faces (triangles), 12 edges, 6 vertices</li>
 *   <li>ICOSAHEDRON - 20 faces (triangles), 30 edges, 12 vertices</li>
 *   <li>DODECAHEDRON - 12 faces (pentagons), 30 edges, 20 vertices</li>
 * </ul>
 * 
 * @see PolyhedronShape
 */
public enum PolyType {
    /** 6 square faces, dual of octahedron */
    CUBE(6, 12, 8, "OCTAHEDRON", false),
    
    /** 4 triangular faces, self-dual */
    TETRAHEDRON(4, 6, 4, "TETRAHEDRON", true),
    
    /** 8 triangular faces, dual of cube */
    OCTAHEDRON(8, 12, 6, "CUBE", true),
    
    /** 20 triangular faces, dual of dodecahedron */
    ICOSAHEDRON(20, 30, 12, "DODECAHEDRON", true),
    
    /** 12 pentagonal faces, dual of icosahedron */
    DODECAHEDRON(12, 30, 20, "ICOSAHEDRON", false);
    
    private final int faces;
    private final int edges;
    private final int vertices;
    private final String dualName;
    private final boolean triangleFaces;
    
    PolyType(int faces, int edges, int vertices, String dualName, boolean triangleFaces) {
        this.faces = faces;
        this.edges = edges;
        this.vertices = vertices;
        this.dualName = dualName;
        this.triangleFaces = triangleFaces;
    }
    
    /** Number of faces */
    public int getFaces() { return faces; }
    
    /** Number of edges */
    public int getEdges() { return edges; }
    
    /** Number of vertices */
    public int getVertices() { return vertices; }
    
    /** Returns true if faces are triangles (affects CellType) */
    public boolean hasTriangleFaces() { return triangleFaces; }
    
    /** Returns the dual polyhedron type */
    public PolyType getDual() {
        return valueOf(dualName);
    }
    
    /**
     * Parse from string (case-insensitive).
     * @param name Type name
     * @return Matching type, or CUBE as default
     */
    public static PolyType fromName(String name) {
        if (name == null || name.isEmpty()) {
            return CUBE;
        }
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CUBE;
        }
    }
}
