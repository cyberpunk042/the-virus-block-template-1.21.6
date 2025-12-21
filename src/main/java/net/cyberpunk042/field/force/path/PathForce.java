package net.cyberpunk042.field.force.path;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/**
 * Calculates forces that make entities follow a path.
 * 
 * <h2>How It Works</h2>
 * <ol>
 *   <li>Find nearest point on path to entity</li>
 *   <li>Apply ATTRACTION force toward that point</li>
 *   <li>Apply FLOW force along the path tangent</li>
 *   <li>Optional: RELEASE force at the end</li>
 * </ol>
 */
public final class PathForce {
    
    private PathForce() {}
    
    /**
     * Calculate force to make entity follow a path.
     * 
     * @param entity The entity to affect
     * @param path The path to follow
     * @param attractStrength How hard to pull toward the path
     * @param flowSpeed Speed along the path
     * @param captureRadius Distance at which attraction kicks in
     * @return Force vector
     */
    public static Vec3d calculate(
            Entity entity,
            ForcePath path,
            float attractStrength,
            float flowSpeed,
            float captureRadius
    ) {
        if (entity == null || path == null) return Vec3d.ZERO;
        
        Vec3d entityPos = entity.getPos().add(0, entity.getHeight() / 2, 0);
        
        // Find nearest point on path
        float nearestT = path.nearestT(entityPos);
        Vec3d nearestPoint = path.positionAt(nearestT);
        Vec3d tangent = path.tangentAt(nearestT);
        
        // Distance to path
        double distToPath = entityPos.distanceTo(nearestPoint);
        
        // Outside capture radius - no effect
        if (distToPath > captureRadius) {
            return Vec3d.ZERO;
        }
        
        Vec3d force = Vec3d.ZERO;
        
        // ═══════════════════════════════════════════════════════════════════════
        // ATTRACTION: Pull toward the path
        // Stronger when far from path, weaker when on it
        // ═══════════════════════════════════════════════════════════════════════
        if (distToPath > 0.3) {
            Vec3d toPath = nearestPoint.subtract(entityPos).normalize();
            // Attraction gets stronger as you approach but stops when on path
            float attractFactor = (float)(distToPath / captureRadius);
            force = toPath.multiply(attractStrength * attractFactor);
        }
        
        // ═══════════════════════════════════════════════════════════════════════
        // FLOW: Push along the path tangent
        // Stronger when closer to the path
        // ═══════════════════════════════════════════════════════════════════════
        float onPathFactor = 1f - (float)(distToPath / captureRadius);
        onPathFactor = Math.max(0, onPathFactor);
        force = force.add(tangent.multiply(flowSpeed * onPathFactor));
        
        return force;
    }
    
    /**
     * Calculate force with release at end of path.
     * 
     * @param entity The entity
     * @param path The path (should be non-looping for release to work)
     * @param attractStrength Pull toward path
     * @param flowSpeed Speed along path
     * @param captureRadius Attraction distance
     * @param releaseStrength How hard to throw at end
     * @param releaseThreshold T value after which release kicks in (0.8 = last 20%)
     * @return Force vector
     */
    public static Vec3d calculateWithRelease(
            Entity entity,
            ForcePath path,
            float attractStrength,
            float flowSpeed,
            float captureRadius,
            float releaseStrength,
            float releaseThreshold
    ) {
        if (entity == null || path == null) return Vec3d.ZERO;
        
        Vec3d entityPos = entity.getPos().add(0, entity.getHeight() / 2, 0);
        
        float nearestT = path.nearestT(entityPos);
        Vec3d nearestPoint = path.positionAt(nearestT);
        Vec3d tangent = path.tangentAt(nearestT);
        
        double distToPath = entityPos.distanceTo(nearestPoint);
        
        if (distToPath > captureRadius) {
            return Vec3d.ZERO;
        }
        
        // Check if in release zone
        if (nearestT > releaseThreshold && !path.isLoop()) {
            // ═══════════════════════════════════════════════════════════════════
            // RELEASE: Eject along tangent direction
            // ═══════════════════════════════════════════════════════════════════
            float releaseProgress = (nearestT - releaseThreshold) / (1f - releaseThreshold);
            return tangent.multiply(releaseStrength * releaseProgress);
        }
        
        // Normal path following
        Vec3d force = Vec3d.ZERO;
        
        // Attraction
        if (distToPath > 0.3) {
            Vec3d toPath = nearestPoint.subtract(entityPos).normalize();
            float attractFactor = (float)(distToPath / captureRadius);
            force = toPath.multiply(attractStrength * attractFactor);
        }
        
        // Flow
        float onPathFactor = 1f - (float)(distToPath / captureRadius);
        onPathFactor = Math.max(0, onPathFactor);
        force = force.add(tangent.multiply(flowSpeed * onPathFactor));
        
        return force;
    }
    
    /**
     * Calculate force with phases: CAPTURE → FOLLOW → RELEASE
     * 
     * @param entity Entity to affect
     * @param path Path to follow
     * @param config Configuration for the phases
     * @return Force vector
     */
    public static Vec3d calculatePhased(
            Entity entity,
            ForcePath path,
            PathForceConfig config
    ) {
        if (entity == null || path == null) return Vec3d.ZERO;
        
        Vec3d entityPos = entity.getPos().add(0, entity.getHeight() / 2, 0);
        
        float nearestT = path.nearestT(entityPos);
        Vec3d nearestPoint = path.positionAt(nearestT);
        Vec3d tangent = path.tangentAt(nearestT);
        
        double distToPath = entityPos.distanceTo(nearestPoint);
        
        // DEBUG: Log every second
        if (entity.age % 20 == 0) {
            System.out.println("[PathForce] entity=" + entityPos + 
                " pathPoint=" + nearestPoint +
                " t=" + nearestT +
                " distToPath=" + distToPath +
                " captureRadius=" + config.captureRadius());
        }
        
        // Outside capture radius
        if (distToPath > config.captureRadius()) {
            // Outer attraction zone?
            if (distToPath < config.captureRadius() * 2) {
                // Gentle pull toward path
                Vec3d toPath = nearestPoint.subtract(entityPos).normalize();
                float outerFactor = 1f - (float)((distToPath - config.captureRadius()) / config.captureRadius());
                Vec3d force = toPath.multiply(config.attractStrength() * 0.3f * outerFactor);
                
                if (entity.age % 20 == 0) {
                    System.out.println("[PathForce] OUTER ZONE toPath=" + toPath + " force=" + force);
                }
                return force;
            }
            return Vec3d.ZERO;
        }
        
        Vec3d force = Vec3d.ZERO;
        
        // For LOOPING paths, don't use T-based phases - just attract + flow
        if (path.isLoop()) {
            // Simple attract + flow for loops
            if (distToPath > 0.3) {
                Vec3d toPath = nearestPoint.subtract(entityPos).normalize();
                float attractFactor = (float) Math.min(1.0, distToPath / 3.0);
                force = toPath.multiply(config.attractStrength() * attractFactor);
            }
            // Flow along path
            float onPathFactor = 1f - (float)(distToPath / config.captureRadius());
            force = force.add(tangent.multiply(config.flowSpeed() * onPathFactor));
            
            if (entity.age % 20 == 0) {
                System.out.println("[PathForce] LOOP force=" + force);
            }
            return force;
        }
        
        // Non-looping paths use phase-based logic
        if (nearestT < config.capturePhaseEnd()) {
            // CAPTURE PHASE
            if (distToPath > 0.3) {
                Vec3d toPath = nearestPoint.subtract(entityPos).normalize();
                force = toPath.multiply(config.attractStrength());
            }
            force = force.add(tangent.multiply(config.flowSpeed() * 0.3f));
            
        } else if (nearestT < config.releasePhaseStart()) {
            // FOLLOW PHASE
            if (distToPath > 0.5) {
                Vec3d toPath = nearestPoint.subtract(entityPos).normalize();
                force = toPath.multiply(config.attractStrength() * 0.5f);
            }
            force = force.add(tangent.multiply(config.flowSpeed()));
            
        } else {
            // RELEASE PHASE
            float releaseProgress = (nearestT - config.releasePhaseStart()) / 
                                   (1f - config.releasePhaseStart());
            force = tangent.multiply(config.releaseStrength() * (1f + releaseProgress));
            
            Vec3d outward = entityPos.subtract(nearestPoint);
            if (outward.lengthSquared() > 0.01) {
                outward = outward.normalize();
                force = force.add(outward.multiply(config.releaseStrength() * releaseProgress * 0.5f));
            }
        }
        
        if (entity.age % 20 == 0) {
            System.out.println("[PathForce] force=" + force);
        }
        
        return force;
    }
    
    /**
     * Configuration for phased path forces.
     */
    public record PathForceConfig(
        float attractStrength,    // Pull toward path
        float flowSpeed,          // Speed along path
        float captureRadius,      // Distance at which capture starts
        float capturePhaseEnd,    // T value where capture ends (0.1 = first 10%)
        float releasePhaseStart,  // T value where release starts (0.85 = last 15%)
        float releaseStrength     // Ejection force
    ) {
        public static final PathForceConfig DEFAULT = new PathForceConfig(
            0.3f,   // attractStrength
            0.2f,   // flowSpeed
            8f,     // captureRadius
            0.15f,  // capturePhaseEnd
            0.85f,  // releasePhaseStart
            0.5f    // releaseStrength
        );
    }
}
