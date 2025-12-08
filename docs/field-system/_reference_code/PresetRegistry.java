package net.cyberpunk042.field.registry;


import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.field.primitive.*;
import net.cyberpunk042.log.Logging;

import java.util.*;

/**
 * Registry for reusable field layer presets.
 * 
 * <p>Presets are named configurations that can be referenced by ID,
 * allowing reuse across multiple field definitions.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Register a preset
 * PresetRegistry.register("cyber_shell", 
 *     FieldLayer.sphere("shell", 1.0f, 32)
 *         .withSpin(0.02f)
 *         .withColor("@primary"));
 * 
 * // Use preset
 * FieldLayer layer = PresetRegistry.get("cyber_shell");
 * </pre>
 */
public final class PresetRegistry {
    
    private static final Map<String, FieldLayer> LAYER_PRESETS = new LinkedHashMap<>();
    private static final Map<String, Primitive> PRIMITIVE_PRESETS = new LinkedHashMap<>();
    
    static {
        registerDefaults();
    }
    
    private PresetRegistry() {}
    
    // ─────────────────────────────────────────────────────────────────────────
    // Layer Presets
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Registers a layer preset.
     */
    public static void registerLayer(String id, FieldLayer layer) {
        LAYER_PRESETS.put(id.toLowerCase(), layer);
        Logging.REGISTRY.topic("preset").debug("Registered layer preset: {}", id);
    }
    
    /**
     * Gets a layer preset by ID.
     */
    public static FieldLayer getLayer(String id) {
        return LAYER_PRESETS.get(id.toLowerCase());
    }
    
    /**
     * Gets a layer preset or returns default.
     */
    public static FieldLayer getLayerOrDefault(String id, FieldLayer fallback) {
        FieldLayer preset = getLayer(id);
        return preset != null ? preset : fallback;
    }
    
    /**
     * Returns all layer preset IDs.
     */
    public static Set<String> layerIds() {
        return Collections.unmodifiableSet(LAYER_PRESETS.keySet());
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Primitive Presets
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Registers a primitive preset.
     */
    public static void registerPrimitive(String id, Primitive primitive) {
        PRIMITIVE_PRESETS.put(id.toLowerCase(), primitive);
        Logging.REGISTRY.topic("preset").debug("Registered primitive preset: {}", id);
    }
    
    /**
     * Gets a primitive preset by ID.
     */
    public static Primitive getPrimitive(String id) {
        return PRIMITIVE_PRESETS.get(id.toLowerCase());
    }
    
    /**
     * Returns all primitive preset IDs.
     */
    public static Set<String> primitiveIds() {
        return Collections.unmodifiableSet(PRIMITIVE_PRESETS.keySet());
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Defaults
    // ─────────────────────────────────────────────────────────────────────────
    
    private static void registerDefaults() {
        // Layer presets
        registerLayer("shell_spin", 
            FieldLayer.sphere("shell", 1.0f, 32)
                .withSpin(0.02f)
                .withColor("@primary"));
        
        registerLayer("shell_pulse", 
            FieldLayer.sphere("shell", 1.0f, 32)
                .withPulse(0.5f)
                .withColor("@glow"));
        
        registerLayer("inner_glow", 
            FieldLayer.sphere("inner", 0.8f, 24)
                .withAlpha(0.3f)
                .withColor("@glow"));
        
        registerLayer("outer_ring", 
            FieldLayer.ring("ring", 0, 0.9f, 1.0f, 48)
                .withSpin(-0.01f)
                .withColor("@secondary"));
        
        // Primitive presets
        registerPrimitive("default_sphere", 
            null /* TODO: new primitive */);
        
        registerPrimitive("high_detail_sphere", 
            null /* TODO: new primitive */);
        
        registerPrimitive("low_detail_sphere", 
            null /* TODO: new primitive */);
        
        registerPrimitive("equator_ring", 
            null /* TODO: new primitive */);
        
        Logging.REGISTRY.topic("preset").info(
            "Registered {} layer presets, {} primitive presets",
            LAYER_PRESETS.size(), PRIMITIVE_PRESETS.size());
    }
    
    /**
     * Reloads default presets (for hot-reload).
     */
    public static void reload() {
        LAYER_PRESETS.clear();
        PRIMITIVE_PRESETS.clear();
        registerDefaults();
        Logging.REGISTRY.topic("preset").info("Preset registry reloaded");
    }
}
