package net.cyberpunk042.visual.shape;

import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

/**
 * A 3D sphere shape with configurable tessellation, partial rendering, and algorithm selection.
 * 
 * <h2>Core Parameters</h2>
 * <ul>
 *   <li><b>radius</b>: Sphere radius in blocks</li>
 *   <li><b>latSteps</b>: Latitude divisions (pole to pole)</li>
 *   <li><b>lonSteps</b>: Longitude divisions (around equator)</li>
 *   <li><b>algorithm</b>: Rendering algorithm (LAT_LON, TYPE_A, TYPE_E)</li>
 * </ul>
 * 
 * <h2>Partial Sphere Parameters</h2>
 * <ul>
 *   <li><b>latStart/latEnd</b>: Latitude range (0.0 = north pole, 1.0 = south pole)</li>
 *   <li><b>lonStart/lonEnd</b>: Longitude range (0.0 = start, 1.0 = full circle)</li>
 * </ul>
 * 
 * <h2>Algorithm Selection</h2>
 * <ul>
 *   <li><b>LAT_LON</b>: Default lat/lon tessellation - best for patterns, partial spheres</li>
 *   <li><b>TYPE_A</b>: Overlapping cubes - best for close-up, accurate rendering</li>
 *   <li><b>TYPE_E</b>: Rotated rectangles - best for distant/LOD, efficient</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * <pre>
 * SphereShape.of(5.0f)                           // Full sphere, LAT_LON
 * SphereShape.of(5.0f).withAlgorithm("type_a")   // Accurate sphere
 * SphereShape.hemisphere(5.0f, true)             // Upper half dome
 * </pre>
 * 
 * @see net.cyberpunk042.client.visual.mesh.SphereTessellator_old
 * @see net.cyberpunk042.client.visual.mesh.sphere.SphereAlgorithm_old
 */
public record SphereShape(
        float radius,
        int latSteps,
        int lonSteps,
        float latStart,
        float latEnd,
        float lonStart,
        float lonEnd,
        String algorithm
) implements Shape {
    
    public static final String TYPE = "sphere";
    
    /** Default algorithm: LAT_LON tessellation */
    public static final String DEFAULT_ALGORITHM = "lat_lon";
    
    // =========================================================================
    // Compact Constructor (Validation)
    // =========================================================================
    
    public SphereShape {
        radius = Math.max(0.01f, radius);
        latSteps = Math.max(2, latSteps);
        lonSteps = Math.max(4, lonSteps);
        latStart = MathHelper.clamp(latStart, 0.0f, 1.0f);
        latEnd = MathHelper.clamp(latEnd, 0.0f, 1.0f);
        lonStart = MathHelper.clamp(lonStart, 0.0f, 1.0f);
        lonEnd = MathHelper.clamp(lonEnd, 0.0f, 1.0f);
        
        // Ensure start <= end
        if (latStart > latEnd) {
            float temp = latStart;
            latStart = latEnd;
            latEnd = temp;
        }
        if (lonStart > lonEnd) {
            float temp = lonStart;
            lonStart = lonEnd;
            lonEnd = temp;
        }
        
        // Default algorithm if null/empty
        if (algorithm == null || algorithm.isEmpty()) {
            algorithm = DEFAULT_ALGORITHM;
        }
    }
    
    // =========================================================================
    // Factory Methods - Full Spheres
    // =========================================================================
    
    /** Default sphere: 1 block radius, medium quality, LAT_LON algorithm. */
    public static SphereShape defaults() {
        return new SphereShape(1.0f, 16, 32, 0.0f, 1.0f, 0.0f, 1.0f, DEFAULT_ALGORITHM);
    }
    
    /** Quick sphere with just radius, default tessellation and algorithm. */
    public static SphereShape of(float radius) {
        return new SphereShape(radius, 16, 32, 0.0f, 1.0f, 0.0f, 1.0f, DEFAULT_ALGORITHM);
    }
    
    /** Sphere with custom quality (steps for both lat/lon). */
    public static SphereShape of(float radius, int steps) {
        return new SphereShape(radius, steps, steps * 2, 0.0f, 1.0f, 0.0f, 1.0f, DEFAULT_ALGORITHM);
    }
    
    /** Sphere with full control over tessellation. */
    public static SphereShape of(float radius, int latSteps, int lonSteps) {
        return new SphereShape(radius, latSteps, lonSteps, 0.0f, 1.0f, 0.0f, 1.0f, DEFAULT_ALGORITHM);
    }
    
    /** Sphere with specified algorithm. */
    public static SphereShape of(float radius, String algorithm) {
        return new SphereShape(radius, 16, 32, 0.0f, 1.0f, 0.0f, 1.0f, algorithm);
    }
    
    // =========================================================================
    // Factory Methods - Partial Spheres
    // =========================================================================
    
    /**
     * Creates a hemisphere (half sphere).
     * @param radius Sphere radius
     * @param top true for upper half (dome), false for lower half (bowl)
     */
    public static SphereShape hemisphere(float radius, boolean top) {
        if (top) {
            return new SphereShape(radius, 16, 32, 0.0f, 0.5f, 0.0f, 1.0f, DEFAULT_ALGORITHM);
        } else {
            return new SphereShape(radius, 16, 32, 0.5f, 1.0f, 0.0f, 1.0f, DEFAULT_ALGORITHM);
        }
    }
    
    /**
     * Creates a latitude band (ring around sphere).
     * @param radius Sphere radius
     * @param latStart Start latitude (0.0 = north pole)
     * @param latEnd End latitude (1.0 = south pole)
     */
    public static SphereShape band(float radius, float latStart, float latEnd) {
        return new SphereShape(radius, 16, 32, latStart, latEnd, 0.0f, 1.0f, DEFAULT_ALGORITHM);
    }
    
    /**
     * Creates a longitude arc (slice of sphere).
     * @param radius Sphere radius
     * @param lonStart Start longitude (0.0 = start angle)
     * @param lonEnd End longitude (1.0 = full circle)
     */
    public static SphereShape arc(float radius, float lonStart, float lonEnd) {
        return new SphereShape(radius, 16, 32, 0.0f, 1.0f, lonStart, lonEnd, DEFAULT_ALGORITHM);
    }
    
    /**
     * Creates an equator ring.
     * @param radius Sphere radius
     * @param thickness Band thickness (0.0 - 1.0 of full latitude range)
     */
    public static SphereShape equator(float radius, float thickness) {
        float halfThick = thickness / 2.0f;
        return band(radius, 0.5f - halfThick, 0.5f + halfThick);
    }
    
    // =========================================================================
    // Shape Interface
    // =========================================================================
    
    @Override
    public String getType() {
        return TYPE;
    }
    
    @Override
    public Box getBounds() {
        return new Box(-radius, -radius, -radius, radius, radius, radius);
    }
    
    @Override
    public int estimateVertexCount() {
        int effectiveLatSteps = (int)(latSteps * (latEnd - latStart));
        int effectiveLonSteps = (int)(lonSteps * (lonEnd - lonStart));
        return (effectiveLatSteps + 1) * (effectiveLonSteps + 1);
    }
    
    @Override
    public int estimateTriangleCount() {
        int effectiveLatSteps = (int)(latSteps * (latEnd - latStart));
        int effectiveLonSteps = (int)(lonSteps * (lonEnd - lonStart));
        return effectiveLatSteps * effectiveLonSteps * 2;
    }
    
    // =========================================================================
    // Computed Properties
    // =========================================================================
    
    /**
     * Checks if this is a full sphere (no partial parameters).
     */
    public boolean isFullSphere() {
        return latStart <= 0.001f && latEnd >= 0.999f && 
               lonStart <= 0.001f && lonEnd >= 0.999f;
    }
    
    /**
     * Checks if this shape requires LAT_LON algorithm (has patterns or partial sphere).
     */
    public boolean requiresLatLon() {
        return !isFullSphere();
    }
    
    /**
     * Gets the latitude coverage fraction.
     */
    public float latCoverage() {
        return latEnd - latStart;
    }
    
    /**
     * Gets the longitude coverage fraction.
     */
    public float lonCoverage() {
        return lonEnd - lonStart;
    }
    
    /**
     * Checks if using TYPE_A algorithm.
     */
    public boolean isTypeA() {
        return "type_a".equalsIgnoreCase(algorithm);
    }
    
    /**
     * Checks if using TYPE_E algorithm.
     */
    public boolean isTypeE() {
        return "type_e".equalsIgnoreCase(algorithm);
    }
    
    /**
     * Checks if using LAT_LON algorithm.
     */
    public boolean isLatLon() {
        return "lat_lon".equalsIgnoreCase(algorithm) || DEFAULT_ALGORITHM.equalsIgnoreCase(algorithm);
    }
    
    // =========================================================================
    // Builder-style modifiers
    // =========================================================================
    
    public SphereShape withRadius(float newRadius) {
        return new SphereShape(newRadius, latSteps, lonSteps, latStart, latEnd, lonStart, lonEnd, algorithm);
    }
    
    public SphereShape withSteps(int newLatSteps, int newLonSteps) {
        return new SphereShape(radius, newLatSteps, newLonSteps, latStart, latEnd, lonStart, lonEnd, algorithm);
    }
    
    public SphereShape withLatRange(float start, float end) {
        return new SphereShape(radius, latSteps, lonSteps, start, end, lonStart, lonEnd, algorithm);
    }
    
    public SphereShape withLonRange(float start, float end) {
        return new SphereShape(radius, latSteps, lonSteps, latStart, latEnd, start, end, algorithm);
    }
    
    public SphereShape withAlgorithm(String newAlgorithm) {
        return new SphereShape(radius, latSteps, lonSteps, latStart, latEnd, lonStart, lonEnd, newAlgorithm);
    }
    
    public SphereShape scaled(float scale) {
        return new SphereShape(radius * scale, latSteps, lonSteps, latStart, latEnd, lonStart, lonEnd, algorithm);
    }
    
    // =========================================================================
    // JSON Serialization
    // =========================================================================
    
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", TYPE);
        json.addProperty("radius", radius);
        json.addProperty("latSteps", latSteps);
        json.addProperty("lonSteps", lonSteps);
        
        // Only include partial params if not full sphere
        if (!isFullSphere()) {
            json.addProperty("latStart", latStart);
            json.addProperty("latEnd", latEnd);
            json.addProperty("lonStart", lonStart);
            json.addProperty("lonEnd", lonEnd);
        }
        
        // Only include algorithm if not default
        if (!DEFAULT_ALGORITHM.equalsIgnoreCase(algorithm)) {
            json.addProperty("algorithm", algorithm);
        }
        
        return json;
    }
    
    public static SphereShape fromJson(JsonObject json) {
        if (json == null) {
            return defaults();
        }
        
        float radius = json.has("radius") ? json.get("radius").getAsFloat() : 1.0f;
        int latSteps = json.has("latSteps") ? json.get("latSteps").getAsInt() : 16;
        int lonSteps = json.has("lonSteps") ? json.get("lonSteps").getAsInt() : 32;
        float latStart = json.has("latStart") ? json.get("latStart").getAsFloat() : 0.0f;
        float latEnd = json.has("latEnd") ? json.get("latEnd").getAsFloat() : 1.0f;
        float lonStart = json.has("lonStart") ? json.get("lonStart").getAsFloat() : 0.0f;
        float lonEnd = json.has("lonEnd") ? json.get("lonEnd").getAsFloat() : 1.0f;
        String algorithm = json.has("algorithm") ? json.get("algorithm").getAsString() : DEFAULT_ALGORITHM;
        
        SphereShape result = new SphereShape(radius, latSteps, lonSteps, latStart, latEnd, lonStart, lonEnd, algorithm);
        
        Logging.RENDER.topic("shape").trace(
            "Parsed SphereShape: radius={:.2f}, algo={}, partial={}",
            radius, algorithm, !result.isFullSphere());
        
        return result;
    }
}
