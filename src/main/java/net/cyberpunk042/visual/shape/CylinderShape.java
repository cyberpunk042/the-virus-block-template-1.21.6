package net.cyberpunk042.visual.shape;

import net.minecraft.util.math.Box;

/**
 * A vertical cylindrical beam shape.
 * 
 * <h2>Parameters</h2>
 * <ul>
 *   <li><b>radius</b>: Beam radius</li>
 *   <li><b>height</b>: Beam height (extends up from Y=0)</li>
 *   <li><b>segments</b>: Number of segments around circumference</li>
 * </ul>
 * 
 * <p>Used for vertical light beams, energy columns, etc.
 * Extends from Y=0 upward (not centered).
 */
public record CylinderShape(
        float radius,
        float height,
        int segments
) implements Shape {
    
    public static final String TYPE = "beam";
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Factory
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static CylinderShape defaults() {
        return new CylinderShape(0.5f, 10.0f, 16);
    }
    
    public static CylinderShape of(float radius, float height) {
        return new CylinderShape(radius, height, 16);
    }
    
    public static CylinderShape of(float radius, float height, int segments) {
        return new CylinderShape(radius, height, segments);
    }
    
    /** Thin beam for visual effects. */
    public static CylinderShape thin(float height) {
        return new CylinderShape(0.1f, height, 8);
    }
    
    /** Wide beam for force fields. */
    public static CylinderShape wide(float height) {
        return new CylinderShape(1.0f, height, 24);
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
        // Beam extends from Y=0 upward
        return new Box(-radius, 0, -radius, radius, height, radius);
    }
    
    @Override
    public int estimateVertexCount() {
        return segments * 2 + 2; // Top and bottom circles
    }
    
    @Override
    public int estimateTriangleCount() {
        return segments * 2 + segments * 2; // Sides + caps
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Modifiers
    // ─────────────────────────────────────────────────────────────────────────────
    
    public CylinderShape withRadius(float newRadius) {
        return new CylinderShape(newRadius, height, segments);
    }
    
    public CylinderShape withHeight(float newHeight) {
        return new CylinderShape(radius, newHeight, segments);
    }
    
    public CylinderShape scaled(float scale) {
        return new CylinderShape(radius * scale, height * scale, segments);
    }
}
