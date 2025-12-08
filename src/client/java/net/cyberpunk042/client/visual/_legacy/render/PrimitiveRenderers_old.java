package net.cyberpunk042.client.visual._legacy.render;

import net.cyberpunk042.client.field._legacy.render.PrimitiveRenderer_old;
import net.cyberpunk042.log.Logging;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for primitive renderers.
 * Maps primitive type strings to renderer implementations.
 * 
 * <p>All renderers are registered at class load time via static initializer.
 * New renderers can be added dynamically via {@link #register(PrimitiveRenderer_old)}.
 * 
 * <h2>Registered Types</h2>
 * <ul>
 *   <li>sphere - {@link SphereRenderer_old}</li>
 *   <li>beam - {@link CylinderRenderer_old}</li>
 *   <li>ring - {@link RingRenderer_old}</li>
 *   <li>rings - {@link RingsRenderer_old}</li>
 *   <li>prism - {@link PrismRenderer_old}</li>
 *   <li>cage - {@link CageRenderer_old}</li>
 *   <li>stripes - {@link StripesRenderer_old}</li>
 *   <li>polyhedron - {@link PolyhedronRenderer_old}</li>
 *   <li>disc - {@link DiscRenderer_old}</li>
 * </ul>
 */
public final class PrimitiveRenderers_old {

    private static final Map<String, PrimitiveRenderer_old> RENDERERS = new HashMap<>();

    static {
        // Register all primitive renderers
        register(SphereRenderer_old.INSTANCE);
        register(CylinderRenderer_old.INSTANCE);
        register(RingRenderer_old.INSTANCE);
        register(RingsRenderer_old.INSTANCE);
        register(PrismRenderer_old.INSTANCE);
        register(CageRenderer_old.INSTANCE);
        register(StripesRenderer_old.INSTANCE);
        register(PolyhedronRenderer_old.INSTANCE);
        register(DiscRenderer_old.INSTANCE);
        
        Logging.RENDER.topic("registry").info(
            "Registered {} primitive renderers: sphere, beam, ring, rings, prism, cage, stripes, polyhedron, disc", 
            RENDERERS.size());
    }

    private PrimitiveRenderers_old() {}

    /**
     * Registers a renderer for its type.
     * @param renderer the renderer to register
     */
    public static void register(PrimitiveRenderer_old renderer) {
        String type = renderer.type();
        RENDERERS.put(type, renderer);
        Logging.RENDER.topic("registry").debug("Registered renderer: {}", type);
    }

    /**
     * Gets the renderer for a primitive type.
     * @param type the primitive type string (e.g., "sphere", "ring")
     * @return the renderer, or null if none registered for this type
     */
    public static PrimitiveRenderer_old get(String type) {
        return RENDERERS.get(type);
    }

    /**
     * Checks if a renderer exists for a type.
     * @param type the primitive type string
     * @return true if a renderer is registered
     */
    public static boolean hasRenderer(String type) {
        return RENDERERS.containsKey(type);
    }
    
    /**
     * Gets all registered renderer types.
     * @return iterable of type strings
     */
    public static Iterable<String> types() {
        return RENDERERS.keySet();
    }
    
    /**
     * Gets the count of registered renderers.
     */
    public static int count() {
        return RENDERERS.size();
    }
}
