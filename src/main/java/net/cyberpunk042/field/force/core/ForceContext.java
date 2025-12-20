package net.cyberpunk042.field.force.core;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Context for force field calculations.
 * 
 * <p>Provides all relevant information needed to calculate force on an entity:
 * the entity itself, the force field center, current time, and field configuration.
 * 
 * @param entity The entity being affected (may be null for point calculations)
 * @param fieldCenter Center position of the force field
 * @param normalizedTime Time within field lifecycle (0.0 = start, 1.0 = end)
 * @param ticksElapsed Total ticks since field spawned
 * @param durationTicks Total field duration in ticks (-1 for infinite)
 */
public record ForceContext(
    @Nullable Entity entity,
    Vec3d fieldCenter,
    float normalizedTime,
    long ticksElapsed,
    int durationTicks
) {
    
    /**
     * Returns the entity's current position, or field center if no entity.
     */
    public Vec3d entityPosition() {
        if (entity == null) {
            return fieldCenter;
        }
        // Use center of entity, not feet
        return entity.getPos().add(0, entity.getHeight() / 2, 0);
    }
    
    /**
     * Returns the distance from entity to field center.
     */
    public double distance() {
        return entityPosition().distanceTo(fieldCenter);
    }
    
    /**
     * Returns the direction vector from entity toward field center.
     * Normalized to unit length.
     */
    public Vec3d directionToCenter() {
        Vec3d delta = fieldCenter.subtract(entityPosition());
        double len = delta.length();
        if (len < 0.001) {
            return Vec3d.ZERO;
        }
        return delta.multiply(1.0 / len);
    }
    
    /**
     * Returns the direction vector from field center toward entity.
     * Normalized to unit length.
     */
    public Vec3d directionFromCenter() {
        return directionToCenter().multiply(-1);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Factory Methods
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a context for a specific entity.
     */
    public static ForceContext forEntity(Entity entity, Vec3d center, float normalizedTime, 
                                          long ticksElapsed, int durationTicks) {
        return new ForceContext(entity, center, normalizedTime, ticksElapsed, durationTicks);
    }
    
    /**
     * Creates a context for a point in space (no entity).
     */
    public static ForceContext forPoint(Vec3d point, Vec3d center, float normalizedTime) {
        return new ForceContext(null, center, normalizedTime, 0, -1);
    }
    
    /**
     * Creates a simple context with just entity and center.
     */
    public static ForceContext simple(Entity entity, Vec3d center) {
        return new ForceContext(entity, center, 0.5f, 0, -1);
    }
}
