package net.cyberpunk042.field.force.field;

import net.cyberpunk042.field.force.*;
import net.cyberpunk042.field.force.core.ForceContext;
import net.cyberpunk042.field.force.mode.*;
import net.cyberpunk042.field.force.path.ForcePath;
import net.cyberpunk042.field.force.path.PathForce;
import net.cyberpunk042.field.force.phase.ForcePhase;
import net.cyberpunk042.field.force.phase.ForcePolarity;
import net.cyberpunk042.field.force.zone.ForceZone;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

/**
 * Multi-mode force field implementation.
 * 
 * <p>CLEAN SLATE - only working modes implemented:
 * <ul>
 *   <li><b>PUSH</b>: Simple outward force (WORKS)</li>
 *   <li><b>ORBIT</b>: Tangential with distance stabilization (WORKS)</li>
 *   <li><b>EXPLOSION</b>: Outward blast (WORKS - uses PUSH logic)</li>
 *   <li><b>TORNADO</b>: Spin + lift (WORKS)</li>
 *   <li>Others: STUBBED - return zero until implemented properly</li>
 * </ul>
 */
public class RadialForceField implements ForceField {
    
    private final ForceFieldConfig config;
    
    public RadialForceField(@NotNull ForceFieldConfig config) {
        this.config = config;
    }
    
    @Override
    public Vec3d calculateForce(ForceContext context) {
        double distance = context.distance();
        
        // Outside effect range or too close to center
        if (distance > config.maxRadius() || distance < 0.3) {
            return Vec3d.ZERO;
        }
        
        // Get phase modifiers
        float normalizedTime = context.normalizedTime();
        ForcePolarity polarity = config.polarityAt(normalizedTime);
        float phaseMultiplier = config.phaseMultiplier(normalizedTime);
        
        // HOLD phase = no force
        if (polarity == ForcePolarity.HOLD) {
            return Vec3d.ZERO;
        }
        
        // Calculate base force by mode
        Vec3d baseForce = switch (config.mode()) {
            case RADIAL -> calculateRadial(context, distance);
            case PULL -> calculatePull(context, distance);
            case PUSH -> calculatePush(context, distance);
            case VORTEX -> calculateVortex(context, distance);
            case ORBIT -> calculateOrbit(context, distance);
            case TORNADO -> calculateTornado(context, distance);
            case RING -> calculateRing(context, distance);
            case IMPLOSION -> calculateImplosion(context, distance);
            case EXPLOSION -> calculateExplosion(context, distance);
            case CUSTOM -> calculateCustom(context, distance);
            case PATH -> calculatePath(context, distance);
        };
        
        // Apply phase multiplier and polarity override
        if (polarity == ForcePolarity.PUSH && config.mode() == ForceMode.PULL) {
            baseForce = baseForce.multiply(-1);
        }
        
        return baseForce.multiply(phaseMultiplier);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WORKING MODES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * PUSH: Simple outward force - WORKS
     */
    private Vec3d calculatePush(ForceContext context, double distance) {
        PushModeConfig cfg = config.push() != null ? config.push() : PushModeConfig.DEFAULT;
        
        if (distance > cfg.radius()) return Vec3d.ZERO;
        
        float strength = calculateFalloff(cfg.strength(), (float) distance, cfg.radius(), cfg.falloff());
        Vec3d force = context.directionFromCenter().multiply(strength);
        
        // Vertical boost
        if (cfg.verticalBoost() > 0) {
            float proximityFactor = 1f - (float)(distance / cfg.radius());
            force = force.add(0, cfg.verticalBoost() * proximityFactor, 0);
        }
        
        return force;
    }
    
    /**
     * ORBIT: Tangential with distance stabilization - WORKS
     */
    private Vec3d calculateOrbit(ForceContext context, double distance) {
        OrbitModeConfig cfg = config.orbit() != null ? config.orbit() : OrbitModeConfig.DEFAULT;
        
        if (distance > cfg.maxRadius()) return Vec3d.ZERO;
        
        // Find nearest orbit ring
        int nearestRing = cfg.nearestRing((float) distance);
        float targetRadius = cfg.ringRadius(nearestRing);
        float distToRing = (float) distance - targetRadius;
        
        Vec3d force = Vec3d.ZERO;
        
        // Radial correction to maintain orbit
        if (Math.abs(distToRing) > 0.1f) {
            float correctionStrength = cfg.stability() * cfg.entryForce() * Math.signum(distToRing);
            Vec3d radialDir = distToRing > 0 ? context.directionToCenter() : context.directionFromCenter();
            force = radialDir.multiply(Math.abs(correctionStrength));
        }
        
        // Tangential force for orbit motion
        Vec3d tangent = cfg.orbitAxis().tangentFor(context.directionToCenter());
        float orbitSpeed = cfg.signedOrbitSpeed(nearestRing);
        force = force.add(tangent.multiply(orbitSpeed));
        
        return force;
    }
    
    /**
     * TORNADO: Spin + lift - WORKS
     */
    private Vec3d calculateTornado(ForceContext context, double distance) {
        TornadoModeConfig cfg = config.tornado() != null ? config.tornado() : TornadoModeConfig.DEFAULT;
        
        double entityY = context.entityPosition().y;
        double centerY = context.fieldCenter().y;
        double height = entityY - centerY;
        
        if (height < 0 && distance > cfg.suckRadius()) return Vec3d.ZERO;
        if (height >= 0 && height > cfg.height()) return Vec3d.ZERO;
        
        Vec3d force = Vec3d.ZERO;
        
        if (height < 0) {
            // Below tornado base - pull toward center
            float pullStrength = cfg.groundPull() * (1f - (float)(distance / cfg.suckRadius()));
            force = context.directionToCenter().multiply(pullStrength);
        } else {
            // Inside tornado funnel
            float radiusAtHeight = cfg.radiusAtHeight((float) height);
            float distFromFunnel = (float) distance - radiusAtHeight;
            
            // Push/pull to maintain funnel wall
            if (Math.abs(distFromFunnel) > 0.5f) {
                Vec3d radialDir = distFromFunnel > 0 ? context.directionToCenter() : context.directionFromCenter();
                force = radialDir.multiply(Math.abs(distFromFunnel) * 0.1f);
            }
            
            // Spin around vertical axis
            Vec3d tangent = cfg.spinAxis().tangentFor(context.directionToCenter());
            force = force.add(tangent.multiply(cfg.signedSpinSpeed()));
            
            // Vertical lift
            float heightFactor = 1f - (float)(height / cfg.height());
            force = force.add(0, cfg.liftSpeed() * heightFactor, 0);
        }
        
        return force;
    }
    
    /**
     * EXPLOSION: Outward blast - WORKS (uses PUSH logic)
     */
    private Vec3d calculateExplosion(ForceContext context, double distance) {
        ExplosionModeConfig cfg = config.explosion() != null ? config.explosion() : ExplosionModeConfig.DEFAULT;
        
        if (distance > cfg.blastRadius()) return Vec3d.ZERO;
        
        float strength = cfg.strengthAt((float) distance);
        Vec3d force = context.directionFromCenter().multiply(strength);
        
        // Vertical boost
        float vertForce = cfg.verticalForce();
        if (vertForce > 0) {
            float proximityFactor = 1f - (float)(distance / cfg.blastRadius());
            force = force.add(0, vertForce * proximityFactor, 0);
        }
        
        return force;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STUBBED MODES - TO BE IMPLEMENTED ONE BY ONE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * RADIAL: Zone-based forces - STUBBED
     * TODO: Implement properly
     */
    private Vec3d calculateRadial(ForceContext context, double distance) {
        // For now, use PUSH logic as fallback
        ForcePolarity polarity = config.polarityAt(context.normalizedTime());
        if (polarity == ForcePolarity.PUSH) {
            return calculatePush(context, distance);
        }
        // PULL polarity - stubbed, return zero
        return Vec3d.ZERO;
    }
    
    /**
     * PULL: Simple inward force - STUBBED
     * TODO: This is the most important one to fix!
     */
    private Vec3d calculatePull(ForceContext context, double distance) {
        // STUBBED - return zero
        return Vec3d.ZERO;
    }
    
    /**
     * VORTEX: Spiral motion - STUBBED
     * TODO: Implement properly
     */
    private Vec3d calculateVortex(ForceContext context, double distance) {
        // STUBBED - return zero
        return Vec3d.ZERO;
    }
    
    /**
     * RING: Orbit band - STUBBED
     * TODO: Implement properly
     */
    private Vec3d calculateRing(ForceContext context, double distance) {
        // STUBBED - return zero
        return Vec3d.ZERO;
    }
    
    /**
     * IMPLOSION: Accelerating pull - STUBBED
     * TODO: Implement properly
     */
    private Vec3d calculateImplosion(ForceContext context, double distance) {
        // STUBBED - return zero
        return Vec3d.ZERO;
    }
    
    /**
     * CUSTOM: Zone-by-zone control - STUBBED
     */
    private Vec3d calculateCustom(ForceContext context, double distance) {
        ForceZone zone = config.zoneAt((float) distance);
        if (zone == null) return Vec3d.ZERO;
        
        Vec3d force = Vec3d.ZERO;
        
        // Only tangential for now (since radial pull is broken)
        if (zone.hasTangential()) {
            float tangentStr = zone.tangentialStrengthAt((float) distance);
            Vec3d tangent = zone.tangentAxis().tangentFor(context.directionToCenter());
            force = force.add(tangent.multiply(tangentStr));
        }
        
        // Lift works
        if (zone.hasLift()) {
            float liftStr = zone.liftStrengthAt((float) distance);
            force = force.add(0, liftStr, 0);
        }
        
        return force;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PATH MODE - The new approach!
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * PATH: Entities follow a curved path.
     * CAPTURE → FOLLOW → RELEASE
     */
    private Vec3d calculatePath(ForceContext context, double distance) {
        // SIMPLEST POSSIBLE PULL toward center
        Vec3d toCenter = context.directionToCenter();
        Vec3d force = toCenter.multiply(0.3);
        
        if (context.entity() != null && context.entity().age % 20 == 0) {
            System.out.println("[PULL] toCenter=" + toCenter + " force=" + force);
        }
        
        return force;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Utilities
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public boolean affectsDistance(double distance) {
        return distance <= config.maxRadius() && distance >= 0.3;
    }
    
    private float calculateFalloff(float baseStrength, float distance, float maxDistance, String falloffType) {
        if (maxDistance <= 0) return baseStrength;
        float normalized = Math.min(1f, distance / maxDistance);
        
        return switch (falloffType != null ? falloffType.toLowerCase() : "linear") {
            case "constant" -> baseStrength;
            case "inverse" -> baseStrength / (1f + normalized * 2f);
            case "quadratic" -> baseStrength * (1f - normalized * normalized);
            case "exponential" -> baseStrength * (float) Math.exp(-normalized * 2);
            case "gaussian" -> baseStrength * (float) Math.exp(-normalized * normalized * 2);
            default -> baseStrength * (1f - normalized); // linear
        };
    }
}
