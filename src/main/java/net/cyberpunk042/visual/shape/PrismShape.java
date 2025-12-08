package net.cyberpunk042.visual.shape;

import net.minecraft.util.math.Box;

/**
 * A vertical prism with N sides.
 * 
 * <h2>Parameters</h2>
 * <ul>
 *   <li><b>sides</b>: Number of sides (3=triangle, 4=square, 6=hexagon, etc.)</li>
 *   <li><b>height</b>: Prism height</li>
 *   <li><b>radius</b>: Radius from center to vertices</li>
 * </ul>
 * 
 * <p>Common configurations:
 * <ul>
 *   <li>sides=3: Triangular prism</li>
 *   <li>sides=4: Square prism (rectangular column)</li>
 *   <li>sides=6: Hexagonal prism</li>
 *   <li>sides=8+: Approximates cylinder</li>
 * </ul>
 * 
 * @see net.cyberpunk042.client.visual.mesh.PrismTessellator_old
 */
public record PrismShape(
        int sides,
        float height,
        float radius
) implements Shape {
    
    public static final String TYPE = "prism";
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Defaults & Factory
    // ─────────────────────────────────────────────────────────────────────────────
    
    /** Default hexagonal prism. */
    public static PrismShape defaults() {
        return new PrismShape(6, 2.0f, 1.0f);
    }
    
    /** Quick prism with sides and radius. */
    public static PrismShape of(int sides, float radius) {
        return new PrismShape(sides, 2.0f, radius);
    }
    
    /** Triangular prism. */
    public static PrismShape triangle(float radius, float height) {
        return new PrismShape(3, height, radius);
    }
    
    /** Square prism. */
    public static PrismShape square(float radius, float height) {
        return new PrismShape(4, height, radius);
    }
    
    /** Hexagonal prism. */
    public static PrismShape hexagon(float radius, float height) {
        return new PrismShape(6, height, radius);
    }
    
    /** Cylinder approximation (16 sides). */
    public static PrismShape cylinder(float radius, float height) {
        return new PrismShape(16, height, radius);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Shape Interface
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public String getType() {
        return TYPE;
    }
    
    @Override
    public Box getBounds() {
        float halfHeight = height / 2;
        return new Box(-radius, -halfHeight, -radius, radius, halfHeight, radius);
    }
    
    @Override
    public int estimateVertexCount() {
        return sides * 4 + 2; // Top and bottom faces + sides
    }
    
    @Override
    public int estimateTriangleCount() {
        return sides * 4; // 2 triangles per side face + top/bottom
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Builder-style modifiers
    // ─────────────────────────────────────────────────────────────────────────────
    
    public PrismShape withSides(int newSides) {
        return new PrismShape(newSides, height, radius);
    }
    
    public PrismShape withHeight(float newHeight) {
        return new PrismShape(sides, newHeight, radius);
    }
    
    public PrismShape withRadius(float newRadius) {
        return new PrismShape(sides, height, newRadius);
    }
    
    public PrismShape scaled(float scale) {
        return new PrismShape(sides, height * scale, radius * scale);
    }
}
