package net.cyberpunk042.field.force.field;

import net.cyberpunk042.field.force.ForceFieldConfig;
import net.cyberpunk042.field.force.core.ForceContext;
import net.cyberpunk042.field.force.phase.ForcePhase;
import net.cyberpunk042.field.force.phase.ForcePolarity;
import net.cyberpunk042.field.force.zone.ForceZone;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

/**
 * Radial force field implementation.
 * 
 * <p>Applies force toward or away from a center point based on:
 * <ul>
 *   <li><b>Zones</b>: Different strengths at different radii</li>
 *   <li><b>Phases</b>: Time-based polarity changes (pull → push)</li>
 *   <li><b>Falloff</b>: Strength decreases with distance</li>
 * </ul>
 * 
 * <p>This is the primary force field type for gravity wells, repulsors,
 * singularities, and similar effects.
 * 
 * <h2>How It Works</h2>
 * <ol>
 *   <li>Determine which zone contains the entity</li>
 *   <li>Calculate base strength from zone + falloff</li>
 *   <li>Determine polarity from current phase</li>
 *   <li>Apply strength multiplier from phase</li>
 *   <li>Return force vector (direction × strength)</li>
 * </ol>
 * 
 * @see ForceFieldConfig
 * @see ForceZone
 * @see ForcePhase
 */
public class RadialForceField implements ForceField {
    
    private final ForceFieldConfig config;
    
    /**
     * Creates a radial force field from configuration.
     * 
     * @param config The force field configuration
     */
    public RadialForceField(@NotNull ForceFieldConfig config) {
        this.config = config;
    }
    
    @Override
    public Vec3d calculateForce(ForceContext context) {
        double distance = context.distance();
        
        // Outside all zones → no force
        if (distance > config.maxRadius() || distance < 0.5) {
            return Vec3d.ZERO;
        }
        
        // Find the applicable zone
        ForceZone zone = config.zoneAt((float) distance);
        if (zone == null) {
            return Vec3d.ZERO;
        }
        
        // Calculate base strength from zone (includes falloff)
        float baseStrength = zone.strengthAt((float) distance);
        
        // Get current phase
        ForcePhase phase = config.phaseAt(context.normalizedTime());
        ForcePolarity polarity = phase != null ? phase.polarity() : ForcePolarity.PULL;
        float phaseMultiplier = phase != null ? phase.strengthMultiplier() : 1.0f;
        
        // Calculate final strength
        float finalStrength = baseStrength * phaseMultiplier;
        
        // Get direction based on polarity
        Vec3d direction;
        switch (polarity) {
            case PULL -> direction = context.directionToCenter();
            case PUSH -> direction = context.directionFromCenter();
            case HOLD -> { return Vec3d.ZERO; }
            default -> direction = context.directionToCenter();
        }
        
        // Apply force vector
        Vec3d force = direction.multiply(finalStrength);
        
        // Add vertical boost on push phase
        if (polarity == ForcePolarity.PUSH && config.verticalBoost() > 0) {
            // Stronger vertical boost when closer to center
            float proximityFactor = 1.0f - (float)(distance / config.maxRadius());
            force = force.add(0, config.verticalBoost() * proximityFactor, 0);
        }
        
        return force;
    }
    
    @Override
    public float maxRadius() {
        return config.maxRadius();
    }
    
    @Override
    public boolean affectsDistance(double distance) {
        return distance >= 0.5 && distance <= config.maxRadius();
    }
    
    /**
     * Returns the underlying configuration.
     */
    public ForceFieldConfig config() {
        return config;
    }
    
    /**
     * Returns the maximum velocity cap from config.
     */
    public float maxVelocity() {
        return config.maxVelocity();
    }
    
    /**
     * Returns the damping factor from config.
     */
    public float damping() {
        return config.damping();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Factory
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a simple gravity well.
     * 
     * @param radius Effect radius
     * @param strength Pull strength
     */
    public static RadialForceField gravityWell(float radius, float strength) {
        ForceFieldConfig config = ForceFieldConfig.builder()
            .zone(radius, strength, "quadratic")
            .pullPhase(0, 100)
            .build();
        return new RadialForceField(config);
    }
    
    /**
     * Creates a repulsor field (push only).
     * 
     * @param radius Effect radius
     * @param strength Push strength
     */
    public static RadialForceField repulsor(float radius, float strength) {
        ForceFieldConfig config = ForceFieldConfig.builder()
            .zone(radius, strength, "linear")
            .pushPhase(0, 100, 1.0f)
            .build();
        return new RadialForceField(config);
    }
    
    /**
     * Creates a void tear style field (pull then push).
     * 
     * @param radius Effect radius
     * @param pullStrength Pull phase strength
     * @param pushStrength Push phase strength
     */
    public static RadialForceField voidTear(float radius, float pullStrength, float pushStrength) {
        ForceFieldConfig config = ForceFieldConfig.builder()
            .zone(radius, pullStrength, "quadratic")
            .zone(radius * 0.5f, pullStrength * 1.5f, "quadratic")
            .zone(radius * 0.2f, pullStrength * 2.5f, "constant")
            .pullPhase(0, 75)
            .holdPhase(75, 90)
            .pushPhase(90, 100, pushStrength / pullStrength)
            .verticalBoost(0.3f)
            .maxVelocity(1.5f)
            .build();
        return new RadialForceField(config);
    }
}
