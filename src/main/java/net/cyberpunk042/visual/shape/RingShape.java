package net.cyberpunk042.visual.shape;

import net.minecraft.util.math.Box;

/**
 * A horizontal ring/torus shape at a specific Y level.
 * 
 * <h2>Parameters</h2>
 * <ul>
 *   <li><b>y</b>: Y offset from center (0 = equator level)</li>
 *   <li><b>radius</b>: Ring radius (center to middle of tube)</li>
 *   <li><b>thickness</b>: Tube thickness</li>
 *   <li><b>segments</b>: Number of segments around the ring</li>
 * </ul>
 * 
 * @see net.cyberpunk042.client.visual.mesh.RingTessellator_old
 */
public record RingShape(
        float y,
        float radius,
        float thickness,
        int segments
) implements Shape {
    
    public static final String TYPE = "ring";
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Defaults & Factory
    // ─────────────────────────────────────────────────────────────────────────────
    
    /** Default ring at equator. */
    public static RingShape defaults() {
        return new RingShape(0.0f, 1.0f, 0.1f, 32);
    }
    
    /** Ring at specific Y with radius. */
    public static RingShape at(float y, float radius) {
        return new RingShape(y, radius, 0.1f, 32);
    }
    
    /** Full customization. */
    public static RingShape of(float y, float radius, float thickness, int segments) {
        return new RingShape(y, radius, thickness, segments);
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
        float halfThick = thickness / 2;
        float outer = radius + halfThick;
        return new Box(-outer, y - halfThick, -outer, outer, y + halfThick, outer);
    }
    
    @Override
    public int estimateVertexCount() {
        return segments * 4; // 4 vertices per segment for tube
    }
    
    @Override
    public int estimateTriangleCount() {
        return segments * 4; // 4 triangles per segment
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Builder-style modifiers
    // ─────────────────────────────────────────────────────────────────────────────
    
    public RingShape atY(float newY) {
        return new RingShape(newY, radius, thickness, segments);
    }
    
    public RingShape withRadius(float newRadius) {
        return new RingShape(y, newRadius, thickness, segments);
    }
    
    public RingShape withThickness(float newThickness) {
        return new RingShape(y, radius, newThickness, segments);
    }
    
    public RingShape scaled(float scale) {
        return new RingShape(y * scale, radius * scale, thickness * scale, segments);
    }
}
