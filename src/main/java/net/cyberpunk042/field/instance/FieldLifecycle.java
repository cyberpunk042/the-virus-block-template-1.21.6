package net.cyberpunk042.field.instance;

import net.cyberpunk042.log.Logging;
import net.minecraft.server.world.ServerWorld;

/**
 * Handles field instance lifecycle events.
 * 
 * <h2>Lifecycle Phases</h2>
 * <ol>
 *   <li><b>Spawn</b> - Instance created and added to world</li>
 *   <li><b>Tick</b> - Per-tick update (effects, animation)</li>
 *   <li><b>Despawn</b> - Instance removed from world</li>
 * </ol>
 * 
 * <h2>Usage</h2>
 * <pre>
 * FieldLifecycle lifecycle = FieldLifecycle.get();
 * lifecycle.onSpawn(instance, world);
 * // Each tick:
 * lifecycle.onTick(instance, world);
 * // When removing:
 * lifecycle.onDespawn(instance, world);
 * </pre>
 */
public class FieldLifecycle {
    
    private static final int FADE_IN_TICKS = 10;
    private static final int FADE_OUT_TICKS = 10;
    
    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle Events
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Called when a field instance is spawned.
     * 
     * @param instance the field instance
     * @param world the server world (may be null for client-side)
     */
    public void onSpawn(FieldInstance instance, ServerWorld world) {
        Logging.REGISTRY.topic("lifecycle").debug(
            "Field spawned: {} at {}", 
            instance.definitionId(), 
            instance.position());
        
        // Apply initial effects
        applySpawnEffects(instance, world);
    }
    
    /**
     * Called each tick to update the field instance.
     * 
     * @param instance the field instance
     * @param world the server world
     */
    public void onTick(FieldInstance instance, ServerWorld world) {
        int age = instance.age();
        
        // Update fade state based on age
        updateFadeState(instance, age);
        
        // Process effects (damage, healing, etc.)
        processEffects(instance, world);
        
        // Personal fields update position via their tick method
    }
    
    /**
     * Called when a field instance is despawned.
     * 
     * @param instance the field instance
     * @param world the server world
     */
    public void onDespawn(FieldInstance instance, ServerWorld world) {
        Logging.REGISTRY.topic("lifecycle").debug(
            "Field despawned: {} (age: {} ticks)", 
            instance.definitionId(),
            instance.age());
        
        // Clean up any effects
        cleanupEffects(instance, world);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Fade Handling
    // ─────────────────────────────────────────────────────────────────────────
    
    private void updateFadeState(FieldInstance instance, int age) {
        if (age < FADE_IN_TICKS) {
            // Fading in
            float progress = (float) age / FADE_IN_TICKS;
            instance.setAlpha(progress);
        } else if (instance.isRemoved()) {
            // Already removed, fade complete
            instance.setAlpha(0);
        } else {
            instance.setAlpha(1.0f);
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Effects
    // ─────────────────────────────────────────────────────────────────────────
    
    private void applySpawnEffects(FieldInstance instance, ServerWorld world) {
        // Spawn particles, sounds, etc.
        // Can be overridden in subclasses for specific behavior
    }
    
    private void processEffects(FieldInstance instance, ServerWorld world) {
        // Process damage, healing, knockback, etc.
        // Delegates to EffectProcessor if configured
        // Effects would be processed here via FieldEffects utility
    }
    
    private void cleanupEffects(FieldInstance instance, ServerWorld world) {
        // Remove any lingering status effects, particles, etc.
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Singleton
    // ─────────────────────────────────────────────────────────────────────────
    
    private static final FieldLifecycle INSTANCE = new FieldLifecycle();
    
    public static FieldLifecycle get() {
        return INSTANCE;
    }
}
