package net.cyberpunk042.field._legacy.primitive;

import net.cyberpunk042.field.primitive.Primitive;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.shape.DiscShape;
import net.cyberpunk042.visual.shape.Shape;
import net.cyberpunk042.visual.transform.Transform;

/**
 * A flat circular disc primitive (filled circle).
 * 
 * <p>Unlike RingPrimitive_old, a disc is solid - useful for:
 * <ul>
 *   <li>Platform indicators</li>
 *   <li>Landing pads</li>
 *   <li>Shield bases</li>
 *   <li>Floor markers</li>
 * </ul>
 * 
 * <h2>Geometry</h2>
 * <p>The disc is rendered as a triangle fan from a center vertex to edge vertices,
 * all at the same Y level. Normal points up (+Y).
 * 
 * <h2>Comparison with Ring</h2>
 * <table border="1">
 *   <tr><th>Property</th><th>Disc</th><th>Ring</th></tr>
 *   <tr><td>Center</td><td>Filled</td><td>Hollow</td></tr>
 *   <tr><td>Vertices</td><td>segments + 1</td><td>segments × 4</td></tr>
 *   <tr><td>Triangles</td><td>segments</td><td>segments × 4</td></tr>
 * </table>
 * 
 * @see DiscShape
 * @see RingPrimitive_old
 * @see net.cyberpunk042.client.visual.mesh.DiscTessellator_old
 */
public class DiscPrimitive_old extends SolidPrimitive_old {
    
    public static final String TYPE = "disc";
    
    private final DiscShape shape;
    
    /**
     * Creates a disc primitive with full configuration.
     * 
     * @param shape      disc geometry (y, radius, segments)
     * @param transform  position/rotation/scale
     * @param appearance color/alpha/fill settings
     * @param animation  spin/pulse animations
     */
    public DiscPrimitive_old(DiscShape shape, Transform transform, Appearance appearance, Animation animation) {
        super(shape, transform, appearance, animation);
        this.shape = shape != null ? shape : DiscShape.defaults();
        
        Logging.REGISTRY.topic("disc").trace(
            "Created DiscPrimitive_old: y={:.2f} radius={:.2f} segs={}",
            this.shape.y(), this.shape.radius(), this.shape.segments());
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Factory Methods
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static DiscPrimitive_old of(float radius) {
        return new DiscPrimitive_old(
            DiscShape.at(0, radius),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none()
        );
    }
    
    public static DiscPrimitive_old at(float y, float radius) {
        return new DiscPrimitive_old(
            DiscShape.at(y, radius),
            Transform.identity(),
            Appearance.defaults(),
            Animation.none()
        );
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Primitive Interface
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public String type() {
        return TYPE;
    }
    
    @Override
    public Shape shape() {
        return shape;
    }
    
    public DiscShape discShape() {
        return shape;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // With Methods
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public DiscPrimitive_old withTransform(Transform newTransform) {
        return new DiscPrimitive_old(shape, newTransform, appearance, animation);
    }
    
    @Override
    public DiscPrimitive_old withAppearance(Appearance newAppearance) {
        return new DiscPrimitive_old(shape, transform, newAppearance, animation);
    }
    
    @Override
    public DiscPrimitive_old withAnimation(Animation newAnimation) {
        return new DiscPrimitive_old(shape, transform, appearance, newAnimation);
    }
    
    public DiscPrimitive_old withShape(DiscShape newShape) {
        return new DiscPrimitive_old(newShape, transform, appearance, animation);
    }
}

