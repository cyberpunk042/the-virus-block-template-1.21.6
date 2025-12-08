package net.cyberpunk042.visual.shape;

import net.minecraft.util.math.Box;

/**
 * A flat circular disc at a specific Y level.
 * 
 * <h2>Parameters</h2>
 * <ul>
 *   <li><b>y</b>: Y offset from center</li>
 *   <li><b>radius</b>: Disc radius</li>
 *   <li><b>segments</b>: Number of segments (higher = smoother circle)</li>
 * </ul>
 * 
 * <p>Unlike RingShape, a disc is solid (filled circle).
 */
public record DiscShape(
        float y,
        float radius,
        int segments
) implements Shape {
    
    public static final String TYPE = "disc";
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Factory
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static DiscShape defaults() {
        return new DiscShape(0.0f, 1.0f, 32);
    }
    
    public static DiscShape at(float y, float radius) {
        return new DiscShape(y, radius, 32);
    }
    
    public static DiscShape of(float y, float radius, int segments) {
        return new DiscShape(y, radius, segments);
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
        // Thin bounding box at Y level
        return new Box(-radius, y - 0.01, -radius, radius, y + 0.01, radius);
    }
    
    @Override
    public int estimateVertexCount() {
        return segments + 1; // Center + edge vertices
    }
    
    @Override
    public int estimateTriangleCount() {
        return segments; // Triangle fan from center
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Modifiers
    // ─────────────────────────────────────────────────────────────────────────────
    
    public DiscShape atY(float newY) {
        return new DiscShape(newY, radius, segments);
    }
    
    public DiscShape withRadius(float newRadius) {
        return new DiscShape(y, newRadius, segments);
    }
    
    public DiscShape scaled(float scale) {
        return new DiscShape(y * scale, radius * scale, segments);
    }
}
