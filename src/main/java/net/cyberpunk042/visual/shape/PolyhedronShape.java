package net.cyberpunk042.visual.shape;

import net.minecraft.util.math.Box;

/**
 * A platonic solid shape.
 * 
 * <h2>Types</h2>
 * <ul>
 *   <li>{@link Type#CUBE}: 6 faces, 8 vertices</li>
 *   <li>{@link Type#OCTAHEDRON}: 8 faces, 6 vertices (diamond shape)</li>
 *   <li>{@link Type#ICOSAHEDRON}: 20 faces, 12 vertices (approximates sphere)</li>
 *   <li>{@link Type#DODECAHEDRON}: 12 pentagonal faces, 20 vertices</li>
 *   <li>{@link Type#TETRAHEDRON}: 4 faces, 4 vertices (pyramid)</li>
 * </ul>
 */
public record PolyhedronShape(
        Type type,
        float size
) implements Shape {
    
    public static final String TYPE = "polyhedron";
    
    /**
     * Platonic solid types.
     */
    public enum Type {
        CUBE(6, 8),
        OCTAHEDRON(8, 6),
        ICOSAHEDRON(20, 12),
        DODECAHEDRON(12, 20),
        TETRAHEDRON(4, 4);
        
        private final int faces;
        private final int vertices;
        
        Type(int faces, int vertices) {
            this.faces = faces;
            this.vertices = vertices;
        }
        
        public int faces() { return faces; }
        public int vertices() { return vertices; }
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Factory
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static PolyhedronShape cube(float size) {
        return new PolyhedronShape(Type.CUBE, size);
    }
    
    public static PolyhedronShape octahedron(float size) {
        return new PolyhedronShape(Type.OCTAHEDRON, size);
    }
    
    public static PolyhedronShape icosahedron(float size) {
        return new PolyhedronShape(Type.ICOSAHEDRON, size);
    }
    
    public static PolyhedronShape dodecahedron(float size) {
        return new PolyhedronShape(Type.DODECAHEDRON, size);
    }
    
    public static PolyhedronShape tetrahedron(float size) {
        return new PolyhedronShape(Type.TETRAHEDRON, size);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Shape Interface
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public String getType() {
        return TYPE + ":" + type.name().toLowerCase();
    }
    
    @Override
    public Box getBounds() {
        float half = size / 2;
        return new Box(-half, -half, -half, half, half, half);
    }
    
    @Override
    public int estimateVertexCount() {
        return type.vertices();
    }
    
    @Override
    public int estimateTriangleCount() {
        return type.faces() * 2; // Most faces need 2 triangles
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Modifiers
    // ─────────────────────────────────────────────────────────────────────────────
    
    public PolyhedronShape withSize(float newSize) {
        return new PolyhedronShape(type, newSize);
    }
    
    public PolyhedronShape scaled(float scale) {
        return new PolyhedronShape(type, size * scale);
    }
}
