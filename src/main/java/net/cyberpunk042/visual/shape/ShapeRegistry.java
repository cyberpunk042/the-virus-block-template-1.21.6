package net.cyberpunk042.visual.shape;

import net.cyberpunk042.log.Logging;

import java.util.*;
import java.util.function.Supplier;

/**
 * Registry for shape factories, allowing shape lookup by name.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Get a shape by name
 * Shape sphere = ShapeRegistry.create("sphere", 1.0f);
 * 
 * // Get with parameters
 * Shape ring = ShapeRegistry.create("ring", Map.of(
 *     "radius", 1.0f,
 *     "thickness", 0.1f,
 *     "segments", 48
 * ));
 * </pre>
 * 
 * @see Shape
 */
public final class ShapeRegistry {
    
    // ─────────────────────────────────────────────────────────────────────────
    // Shape Factories
    // ─────────────────────────────────────────────────────────────────────────
    
    @FunctionalInterface
    public interface ShapeFactory {
        Shape create(Map<String, Object> params);
    }
    
    private static final Map<String, ShapeFactory> FACTORIES = new LinkedHashMap<>();
    
    static {
        registerDefaults();
    }
    
    private ShapeRegistry() {}
    
    // ─────────────────────────────────────────────────────────────────────────
    // Registration
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Registers a shape factory.
     * 
     * @param name Shape name (lowercase)
     * @param factory Factory function
     */
    public static void register(String name, ShapeFactory factory) {
        FACTORIES.put(name.toLowerCase(), factory);
        Logging.REGISTRY.topic("shape").debug("Registered shape: {}", name);
    }
    
    /**
     * Registers a simple shape (no parameters needed).
     */
    public static void registerSimple(String name, Supplier<Shape> supplier) {
        register(name, params -> supplier.get());
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Creation
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Creates a shape by name with parameters.
     * 
     * @param name Shape name
     * @param params Shape parameters
     * @return The shape, or null if not found
     */
    public static Shape create(String name, Map<String, Object> params) {
        ShapeFactory factory = FACTORIES.get(name.toLowerCase());
        if (factory == null) {
            Logging.REGISTRY.topic("shape").warn(
                "Unknown shape: '{}'. Available: {}", name, FACTORIES.keySet());
            return null;
        }
        
        try {
            Shape shape = factory.create(params);
            Logging.REGISTRY.topic("shape").trace(
                "Created shape: {} with {} params", name, params.size());
            return shape;
        } catch (Exception e) {
            Logging.REGISTRY.topic("shape").error(
                "Failed to create shape '{}': {}", name, e.getMessage());
            return null;
        }
    }
    
    /**
     * Creates a shape with a single radius parameter.
     */
    public static Shape create(String name, float radius) {
        return create(name, Map.of("radius", radius));
    }
    
    /**
     * Creates a shape with default parameters.
     */
    public static Shape create(String name) {
        return create(name, Map.of());
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Checks if a shape is registered.
     */
    public static boolean exists(String name) {
        return FACTORIES.containsKey(name.toLowerCase());
    }
    
    /**
     * Returns all registered shape names.
     */
    public static Set<String> names() {
        return Collections.unmodifiableSet(FACTORIES.keySet());
    }
    
    /**
     * Returns the number of registered shapes.
     */
    public static int count() {
        return FACTORIES.size();
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Default Shapes
    // ─────────────────────────────────────────────────────────────────────────
    
    private static void registerDefaults() {
        // Sphere (supports partial sphere with latStart/End, lonStart/End, algorithm selection)
        register("sphere", params -> {
            float radius = getFloat(params, "radius", 1.0f);
            int latSteps = getInt(params, "latSteps", 32);
            int lonSteps = getInt(params, "lonSteps", latSteps * 2);
            float latStart = getFloat(params, "latStart", 0.0f);
            float latEnd = getFloat(params, "latEnd", 1.0f);
            float lonStart = getFloat(params, "lonStart", 0.0f);
            float lonEnd = getFloat(params, "lonEnd", 1.0f);
            String algorithm = getString(params, "algorithm", SphereShape.DEFAULT_ALGORITHM);
            return new SphereShape(radius, latSteps, lonSteps, latStart, latEnd, lonStart, lonEnd, algorithm);
        });
        
        // Ring
        register("ring", params -> {
            float y = getFloat(params, "y", 0);
            float radius = getFloat(params, "radius", 1.0f);
            float thickness = getFloat(params, "thickness", 0.1f);
            int segments = getInt(params, "segments", 48);
            return new RingShape(y, radius, thickness, segments);
        });
        
        // Disc (filled circle)
        register("disc", params -> {
            float y = getFloat(params, "y", 0);
            float radius = getFloat(params, "radius", 1.0f);
            int segments = getInt(params, "segments", 48);
            return new DiscShape(y, radius, segments);
        });
        
        // Prism
        register("prism", params -> {
            int sides = getInt(params, "sides", 6);
            float radius = getFloat(params, "radius", 1.0f);
            float height = getFloat(params, "height", 1.0f);
            return new PrismShape(sides, radius, height);
        });
        
        // Beam
        register("beam", params -> {
            float radius = getFloat(params, "radius", 0.05f);
            float height = getFloat(params, "height", 256.0f);
            int segments = getInt(params, "segments", 8);
            return new CylinderShape(radius, height, segments);
        });
        
        // Polyhedron
        register("cube", params -> {
            float size = getFloat(params, "size", 1.0f);
            return PolyhedronShape.cube(size);
        });
        
        register("octahedron", params -> {
            float size = getFloat(params, "size", 1.0f);
            return PolyhedronShape.octahedron(size);
        });
        
        register("icosahedron", params -> {
            float size = getFloat(params, "size", 1.0f);
            return PolyhedronShape.icosahedron(size);
        });
        
        Logging.REGISTRY.topic("shape").info(
            "Registered {} default shapes: {}", 
            FACTORIES.size(), FACTORIES.keySet());
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Parameter Helpers
    // ─────────────────────────────────────────────────────────────────────────
    
    private static float getFloat(Map<String, Object> params, String key, float defaultValue) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return defaultValue;
    }
    
    private static int getInt(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    private static String getString(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }
}
