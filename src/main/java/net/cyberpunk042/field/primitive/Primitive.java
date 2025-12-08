package net.cyberpunk042.field.primitive;

import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.transform.Transform;
import net.cyberpunk042.visual.shape.Shape;

/**
 * Base interface for field visual primitives.
 * 
 * <p>Implementations are in the _legacy package during refactoring.
 * New implementations will be flat (no hierarchy) per ARCHITECTURE_PROPOSAL.md.
 * 
 * <h2>Tessellation</h2>
 * <p>Tessellation is handled client-side via the tessellator classes.
 * Use the primitive's shape to create the appropriate tessellator.
 * 
 * @see net.cyberpunk042.client.visual.mesh.Tessellator
 */
public interface Primitive {
    
    /**
     * Returns the geometric shape of this primitive.
     * Per ARCHITECTURE.md naming convention.
     */
    Shape shape();
    
    /**
     * Returns the transform (offset, rotation, scale).
     * Per ARCHITECTURE.md naming convention.
     */
    Transform transform();
    
    /**
     * Returns the appearance (color, alpha, fill mode).
     * Per ARCHITECTURE.md naming convention.
     */
    Appearance appearance();
    
    /**
     * Returns the animation configuration.
     * Per ARCHITECTURE.md naming convention.
     */
    Animation animation();
    
    /**
     * Returns the primitive type identifier.
     */
    String type();
    
    /**
     * Creates a copy with a new transform.
     */
    Primitive withTransform(Transform transform);
    
    /**
     * Creates a copy with a new appearance.
     */
    Primitive withAppearance(Appearance appearance);
    
    /**
     * Creates a copy with a new animation.
     */
    Primitive withAnimation(Animation animation);
}
