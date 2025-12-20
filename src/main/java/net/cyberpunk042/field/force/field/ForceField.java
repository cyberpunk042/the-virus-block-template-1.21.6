package net.cyberpunk042.field.force.field;

import net.cyberpunk042.field.force.core.ForceContext;
import net.minecraft.util.math.Vec3d;

/**
 * Interface for force field calculations.
 * 
 * <p>A ForceField computes the force vector to apply at any point in space,
 * given the context (entity, time, etc.). This is the core abstraction for
 * different force field behaviors.
 * 
 * <h2>Implementations</h2>
 * <ul>
 *   <li>{@link RadialForceField} - Pull/push from center with zones</li>
 *   <li>{@link VortexForceField} - Spiraling force (future)</li>
 *   <li>{@link CompositeForceField} - Combines multiple fields (future)</li>
 * </ul>
 * 
 * @see RadialForceField
 * @see ForceContext
 */
@FunctionalInterface
public interface ForceField {
    
    /**
     * Calculates the force vector at the given context.
     * 
     * <p>The returned vector represents the velocity change to apply to an entity.
     * It should be scaled appropriately for per-tick application.
     * 
     * @param context The force calculation context (entity, position, time, etc.)
     * @return Force vector to apply (blocks per tick)
     */
    Vec3d calculateForce(ForceContext context);
    
    /**
     * Returns the maximum effect radius of this field.
     * Used for entity detection bounds.
     * 
     * @return Maximum radius in blocks
     */
    default float maxRadius() {
        return 16.0f;
    }
    
    /**
     * Returns true if this field affects the given distance.
     * 
     * @param distance Distance from field center
     * @return True if force should be calculated
     */
    default boolean affectsDistance(double distance) {
        return distance > 0.5 && distance <= maxRadius();
    }
}
