package net.cyberpunk042.field.force.field;

import net.cyberpunk042.field.force.ForceFieldConfig;
import net.cyberpunk042.field.force.core.ForceContext;
import net.minecraft.util.math.Vec3d;

/**
 * Pure gravitational force field with orbital dynamics.
 * 
 * <h2>Physics Model</h2>
 * <ul>
 *   <li>Force is ONLY toward center (pure gravity)</li>
 *   <li>Natural elliptical orbits emerge</li>
 *   <li>Center perturbation breaks single-axis oscillation</li>
 * </ul>
 * 
 * <h2>Safety Features</h2>
 * <ul>
 *   <li><b>Gravity floor</b>: Minimum force at edges prevents escape</li>
 *   <li><b>Dynamic softening</b>: Prevents extreme center acceleration</li>
 *   <li><b>Center perturbation</b>: Breaks symmetry for varied orbits</li>
 * </ul>
 */
public class RadialForceField implements ForceField {
    
    private final ForceFieldConfig config;
    
    // Base softening - scales with strength
    private static final double BASE_SOFTENING = 1.0;
    
    // Gravity floor - minimum fraction at edges
    private static final double GRAVITY_FLOOR = 0.12;
    
    // Maximum force multiplier
    private static final double MAX_FORCE_MULTIPLIER = 4.0;
    
    // Center zone where perturbation is applied (fraction of radius)
    private static final double CENTER_ZONE = 0.15;
    
    public RadialForceField(ForceFieldConfig config) {
        this.config = config != null ? config : ForceFieldConfig.DEFAULT;
    }
    
    @Override
    public Vec3d calculateForce(ForceContext context) {
        double distance = context.distance();
        double radius = config.radius();
        
        // Outside field radius - no force
        if (distance > radius) {
            return Vec3d.ZERO;
        }
        
        // Direction toward center
        Vec3d towardCenter = context.directionToCenter();
        
        // ═══════════════════════════════════════════════════════════════════════
        // PURE GRAVITATIONAL FORCE
        // ═══════════════════════════════════════════════════════════════════════
        double normalizedDist = distance / radius;
        double gravityStrength = calculateGravity(distance, normalizedDist);
        
        Vec3d force = towardCenter.multiply(gravityStrength);
        
        // ═══════════════════════════════════════════════════════════════════════
        // CENTER PERTURBATION - breaks single-axis oscillation
        // When entity passes near center, add tiny perpendicular force
        // This causes each pass to exit at slightly different angle
        // ═══════════════════════════════════════════════════════════════════════
        if (normalizedDist < CENTER_ZONE && context.entity() != null) {
            Vec3d velocity = context.entity().getVelocity();
            double speed = velocity.length();
            
            if (speed > 0.1) {
                // Perpendicular to velocity in horizontal plane
                Vec3d perpendicular = velocity.crossProduct(new Vec3d(0, 1, 0));
                if (perpendicular.lengthSquared() > 0.001) {
                    perpendicular = perpendicular.normalize();
                    
                    // Small perturbation - just enough to break symmetry
                    // Scales with how close to center (stronger at exact center)
                    double perturbStrength = config.strength() * 0.08 * (1 - normalizedDist / CENTER_ZONE);
                    force = force.add(perpendicular.multiply(perturbStrength));
                }
            }
        }
        
        // Ground lift
        if (config.groundLift() > 0 && context.entity() != null && context.entity().isOnGround()) {
            force = force.add(0, config.groundLift(), 0);
        }
        
        return force;
    }
    
    /**
     * Calculates gravity strength with floor and softening.
     */
    private double calculateGravity(double distance, double normalizedDist) {
        String falloff = config.falloff();
        double strength = config.strength();
        
        // Dynamic softening
        double softening = BASE_SOFTENING * (1 + strength * 0.5);
        
        double calculatedStrength = switch (falloff) {
            case "quadratic" -> strength * (1 - normalizedDist) * (1 - normalizedDist);
            case "inverse" -> {
                double distSq = distance * distance;
                yield strength / (distSq + softening);
            }
            case "constant" -> strength;
            default -> strength * (1 - normalizedDist);  // linear
        };
        
        // Gravity floor
        double floorStrength = strength * GRAVITY_FLOOR;
        calculatedStrength = Math.max(calculatedStrength, floorStrength);
        
        // Cap maximum
        double maxStrength = strength * MAX_FORCE_MULTIPLIER;
        calculatedStrength = Math.min(calculatedStrength, maxStrength);
        
        return calculatedStrength;
    }
    
    @Override
    public boolean affectsDistance(double distance) {
        return distance <= config.radius();
    }
    
    @Override
    public float maxRadius() {
        return config.radius();
    }
}
