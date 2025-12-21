package net.cyberpunk042.field.force.field;

import net.cyberpunk042.field.force.ForceFieldConfig;
import net.cyberpunk042.field.force.core.ForceContext;
import net.minecraft.util.math.Vec3d;

/**
 * Gravitational force field.
 * 
 * <p>Implements REAL gravitational physics:
 * <ul>
 *   <li>Force is ALWAYS toward center (radial only)</li>
 *   <li>Force strength follows inverse-square law: strength / r²</li>
 *   <li>Slingshot effect happens NATURALLY from trajectory bending</li>
 *   <li>No artificial tangent forces needed</li>
 * </ul>
 */
public class RadialForceField implements ForceField {
    
    private final ForceFieldConfig config;
    
    // Softening parameter to prevent infinite force at center
    private static final double SOFTENING = 0.5;
    
    public RadialForceField(ForceFieldConfig config) {
        this.config = config != null ? config : ForceFieldConfig.DEFAULT;
    }
    
    @Override
    public Vec3d calculateForce(ForceContext context) {
        double distance = context.distance();
        
        // Outside field radius - no force
        if (distance > config.radius()) {
            return Vec3d.ZERO;
        }
        
        // Direction toward center (this is gravity - always toward center)
        Vec3d towardCenter = context.directionToCenter();
        
        // ═══════════════════════════════════════════════════════════════════════
        // GRAVITATIONAL FORCE: F = strength / (r² + softening)
        // ═══════════════════════════════════════════════════════════════════════
        // Inverse square law - force gets much stronger near center
        // Softening prevents infinite force at exact center
        double distSq = distance * distance;
        double strength = config.strength() / (distSq + SOFTENING);
        
        // Cap maximum force to prevent crazy acceleration
        strength = Math.min(strength, config.strength() * 10);
        
        Vec3d force = towardCenter.multiply(strength);
        
        // Ground lift to prevent sticking to ground
        if (config.groundLift() > 0 && context.entity() != null && context.entity().isOnGround()) {
            force = force.add(0, config.groundLift(), 0);
        }
        
        return force;
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
