package net.cyberpunk042.client.visual.mesh.ray;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.shape.RayType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registry mapping RayType values to their tessellator implementations.
 * 
 * <p>This provides a central location for registering and retrieving
 * ray type tessellators.
 * 
 * <h2>Usage</h2>
 * <pre>
 * RayTypeTessellator tess = RayTypeTessellatorRegistry.get(RayType.DROPLET);
 * tess.tessellate(builder, shape, context);
 * </pre>
 * 
 * @see RayTypeTessellator
 * @see RayType
 */
public final class RayTypeTessellatorRegistry {
    
    private static final Map<RayType, RayTypeTessellator> TESSELLATORS = new EnumMap<>(RayType.class);
    
    static {
        // Register default tessellators
        register(RayType.LINE, RayLineTessellator.INSTANCE);
        
        // 3D Ray Types - all use RayDropletTessellator with GeoRadiusProfile selection
        // The profile is selected per-RayType by GeoRadiusProfileFactory
        
        // Basic Geometry Category
        register(RayType.DROPLET, RayDropletTessellator.INSTANCE);
        register(RayType.CONE, RayDropletTessellator.INSTANCE);
        register(RayType.ARROW, RayDropletTessellator.INSTANCE);
        register(RayType.CAPSULE, RayDropletTessellator.INSTANCE);
        
        // Energy Effects Category
        register(RayType.KAMEHAMEHA, RayDropletTessellator.INSTANCE);
        register(RayType.LASER, RayDropletTessellator.INSTANCE);
        register(RayType.FIRE_JET, RayDropletTessellator.INSTANCE);
        register(RayType.PLASMA, RayDropletTessellator.INSTANCE);
        
        // Particle Types
        register(RayType.BEADS, RayDropletTessellator.INSTANCE);
        
        // Organic Types
        register(RayType.TENDRIL, RayDropletTessellator.INSTANCE);
        register(RayType.SPINE, RayDropletTessellator.INSTANCE);
        register(RayType.ROOT, RayDropletTessellator.INSTANCE);
        
        // Types that need special handling stay as LINE for now
        // (LIGHTNING, CUBES, STARS, CRYSTALS need procedural generation)
        
        // Fill remaining with LINE fallback
        for (RayType type : RayType.values()) {
            if (!TESSELLATORS.containsKey(type)) {
                TESSELLATORS.put(type, RayLineTessellator.INSTANCE);
            }
        }
        
        Logging.FIELD.topic("tessellation").debug(
            "RayTypeTessellatorRegistry initialized with {} entries", TESSELLATORS.size());
    }
    
    private RayTypeTessellatorRegistry() {} // Utility class
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Gets the tessellator for the specified ray type.
     * 
     * @param type The ray type
     * @return The tessellator (never null, falls back to LINE if not registered)
     */
    public static RayTypeTessellator get(RayType type) {
        if (type == null) {
            return RayLineTessellator.INSTANCE;
        }
        return TESSELLATORS.getOrDefault(type, RayLineTessellator.INSTANCE);
    }
    
    /**
     * Registers a tessellator for a ray type.
     * 
     * @param type The ray type
     * @param tessellator The tessellator implementation
     */
    public static void register(RayType type, RayTypeTessellator tessellator) {
        if (type != null && tessellator != null) {
            TESSELLATORS.put(type, tessellator);
            Logging.FIELD.topic("tessellation").debug(
                "Registered tessellator {} for RayType.{}", tessellator.name(), type);
        }
    }
    
    /**
     * Checks if a specific tessellator is registered (not just fallback).
     */
    public static boolean isImplemented(RayType type) {
        if (type == null || type == RayType.LINE) {
            return true;
        }
        RayTypeTessellator tess = TESSELLATORS.get(type);
        return tess != null && tess != RayLineTessellator.INSTANCE;
    }
    
    /**
     * Returns all registered ray types.
     */
    public static RayType[] registeredTypes() {
        return TESSELLATORS.keySet().toArray(new RayType[0]);
    }
}
