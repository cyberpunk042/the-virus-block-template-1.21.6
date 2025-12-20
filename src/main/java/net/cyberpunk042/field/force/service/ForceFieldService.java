package net.cyberpunk042.field.force.service;

import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.force.ForceFieldConfig;
import net.cyberpunk042.field.force.core.ForceContext;
import net.cyberpunk042.field.force.field.ForceField;
import net.cyberpunk042.field.force.field.RadialForceField;
import net.cyberpunk042.field.force.phase.ForcePhase;
import net.cyberpunk042.field.force.phase.ForcePolarity;
import net.cyberpunk042.log.Logging;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Service that manages and ticks force fields.
 * 
 * <p>This service:
 * <ul>
 *   <li>Tracks active force field instances</li>
 *   <li>Ticks each field and applies forces to entities</li>
 *   <li>Handles phase transitions and notifications</li>
 *   <li>Removes expired fields</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Spawning a force field
 * ForceFieldService.spawn(world, center, definition, durationTicks);
 * 
 * // Called each server tick
 * ForceFieldService.tick(world);
 * </pre>
 */
public final class ForceFieldService {
    
    // Active force fields per world
    private static final ConcurrentHashMap<UUID, ActiveForceField> activeFields = new ConcurrentHashMap<>();
    
    private ForceFieldService() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Spawning
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Spawns a force field at the given position.
     * 
     * @param world The world to spawn in
     * @param center Center position
     * @param definition Field definition (must have forceConfig)
     * @param durationTicks Duration in ticks (-1 for infinite)
     * @return The field ID, or null if spawn failed
     */
    public static UUID spawn(ServerWorld world, Vec3d center, FieldDefinition definition, int durationTicks) {
        if (definition == null || !definition.hasForceConfig()) {
            Logging.FIELD.topic("force").warn("Cannot spawn force field - no forceConfig");
            return null;
        }
        
        ForceFieldConfig config = definition.forceConfig();
        ForceField field = new RadialForceField(config);
        
        UUID id = UUID.randomUUID();
        ActiveForceField active = new ActiveForceField(
            id,
            world.getRegistryKey().getValue(),
            center,
            field,
            config,
            durationTicks,
            world.getTime()
        );
        
        activeFields.put(id, active);
        
        Logging.FIELD.topic("force").info("Spawned force field {} at {} with radius {}", 
            id, center, config.maxRadius());
        
        return id;
    }
    
    /**
     * Spawns a force field using a pre-built ForceFieldConfig.
     */
    public static UUID spawn(ServerWorld world, Vec3d center, ForceFieldConfig config, int durationTicks) {
        if (config == null) {
            return null;
        }
        
        ForceField field = new RadialForceField(config);
        
        UUID id = UUID.randomUUID();
        ActiveForceField active = new ActiveForceField(
            id,
            world.getRegistryKey().getValue(),
            center,
            field,
            config,
            durationTicks,
            world.getTime()
        );
        
        activeFields.put(id, active);
        
        Logging.FIELD.topic("force").info("Spawned force field {} at {} with radius {}", 
            id, center, config.maxRadius());
        
        return id;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Removal
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Removes a force field by ID.
     */
    public static void remove(UUID id) {
        ActiveForceField removed = activeFields.remove(id);
        if (removed != null) {
            Logging.FIELD.topic("force").info("Removed force field {}", id);
        }
    }
    
    /**
     * Removes all force fields in a world.
     */
    public static void removeAll(ServerWorld world) {
        var worldKey = world.getRegistryKey().getValue();
        activeFields.entrySet().removeIf(entry -> entry.getValue().worldId.equals(worldKey));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Tick
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Ticks all force fields in the given world.
     * Should be called once per server tick.
     */
    public static void tick(ServerWorld world) {
        var worldKey = world.getRegistryKey().getValue();
        long currentTime = world.getTime();
        
        List<UUID> toRemove = new ArrayList<>();
        
        for (ActiveForceField active : activeFields.values()) {
            // Skip fields in other worlds
            if (!active.worldId.equals(worldKey)) {
                continue;
            }
            
            // Check expiry
            if (active.isExpired(currentTime)) {
                // Apply final push if configured
                if (active.config.polarityAt(1.0f) == ForcePolarity.PUSH) {
                    applyForces(world, active, true);
                }
                toRemove.add(active.id);
                continue;
            }
            
            // Update phase
            float normalizedTime = active.getNormalizedTime(currentTime);
            active.updatePhase(normalizedTime);
            
            // Apply forces to entities
            applyForces(world, active, false);
        }
        
        // Remove expired fields
        for (UUID id : toRemove) {
            remove(id);
        }
    }
    
    /**
     * Applies forces to all entities near a force field.
     */
    private static void applyForces(ServerWorld world, ActiveForceField active, boolean finalBurst) {
        float maxRadius = active.field.maxRadius();
        Box searchBox = new Box(active.center, active.center).expand(maxRadius);
        
        long currentTime = world.getTime();
        float normalizedTime = finalBurst ? 1.0f : active.getNormalizedTime(currentTime);
        
        // Find entities in range
        Predicate<Entity> filter = entity -> 
            entity instanceof LivingEntity && 
            entity.isAlive() &&
            !entity.isSpectator();
        
        for (Entity entity : world.getEntitiesByClass(LivingEntity.class, searchBox, filter)) {
            applyForceToEntity(world, active, (LivingEntity) entity, normalizedTime);
        }
    }
    
    /**
     * Applies force to a single entity.
     */
    private static void applyForceToEntity(ServerWorld world, ActiveForceField active, 
                                            LivingEntity entity, float normalizedTime) {
        // Create context
        ForceContext context = ForceContext.forEntity(
            entity, 
            active.center, 
            normalizedTime,
            world.getTime() - active.spawnTime,
            active.durationTicks
        );
        
        // Check if in range
        double distance = context.distance();
        if (!active.field.affectsDistance(distance)) {
            return;
        }
        
        // Calculate force
        Vec3d force = active.field.calculateForce(context);
        if (force.lengthSquared() < 0.0001) {
            return;
        }
        
        // Apply damping to existing velocity
        Vec3d currentVel = entity.getVelocity();
        if (active.config.damping() > 0) {
            currentVel = currentVel.multiply(1.0 - active.config.damping());
        }
        
        // Add force
        Vec3d newVel = currentVel.add(force);
        
        // Cap velocity
        float maxVel = active.config.maxVelocity();
        double hSpeed = newVel.horizontalLength();
        if (hSpeed > maxVel) {
            double scale = maxVel / hSpeed;
            newVel = new Vec3d(newVel.x * scale, newVel.y, newVel.z * scale);
        }
        newVel = new Vec3d(newVel.x, MathHelper.clamp(newVel.y, -maxVel, maxVel), newVel.z);
        
        // Apply
        entity.setVelocity(newVel);
        entity.velocityModified = true;
        entity.velocityDirty = true;
        entity.fallDistance = 0.0f;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Queries
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns the number of active force fields.
     */
    public static int activeCount() {
        return activeFields.size();
    }
    
    /**
     * Returns the number of active force fields in a world.
     */
    public static int activeCount(ServerWorld world) {
        var worldKey = world.getRegistryKey().getValue();
        return (int) activeFields.values().stream()
            .filter(f -> f.worldId.equals(worldKey))
            .count();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Active Field Record
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Tracks an active force field instance.
     */
    private static class ActiveForceField {
        final UUID id;
        final net.minecraft.util.Identifier worldId;
        final Vec3d center;
        final ForceField field;
        final ForceFieldConfig config;
        final int durationTicks;
        final long spawnTime;
        
        ForcePhase currentPhase = null;
        
        ActiveForceField(UUID id, net.minecraft.util.Identifier worldId, Vec3d center,
                        ForceField field, ForceFieldConfig config, int durationTicks, long spawnTime) {
            this.id = id;
            this.worldId = worldId;
            this.center = center;
            this.field = field;
            this.config = config;
            this.durationTicks = durationTicks;
            this.spawnTime = spawnTime;
        }
        
        boolean isExpired(long currentTime) {
            if (durationTicks < 0) return false;
            return (currentTime - spawnTime) >= durationTicks;
        }
        
        float getNormalizedTime(long currentTime) {
            if (durationTicks <= 0) return 0.5f;
            long elapsed = currentTime - spawnTime;
            return MathHelper.clamp((float) elapsed / durationTicks, 0f, 1f);
        }
        
        void updatePhase(float normalizedTime) {
            ForcePhase newPhase = config.phaseAt(normalizedTime);
            if (newPhase != null && newPhase != currentPhase) {
                // Phase changed - could fire notification here
                Logging.FIELD.topic("force").debug("Force field {} entering {} phase", 
                    id, newPhase.polarity());
                currentPhase = newPhase;
            }
        }
    }
}
