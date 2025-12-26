package net.cyberpunk042.visual.shape;

import net.cyberpunk042.visual.pattern.CellType;
import org.joml.Vector3f;

import java.util.Map;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;
import net.cyberpunk042.log.Logging;
import com.google.gson.JsonObject;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;


/**
 * Sphere shape with configurable tessellation.
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "shape": {
 *   "type": "sphere",
 *   "radius": 1.0,
 *   "latSteps": 32,
 *   "lonSteps": 64,
 *   "latStart": 0.0,
 *   "latEnd": 1.0,
 *   "algorithm": "LAT_LON"
 * }
 * </pre>
 * 
 * <h2>Lat/Lon Range (0-1 normalized)</h2>
 * <ul>
 *   <li>latStart=0.0 → top (north pole)</li>
 *   <li>latEnd=1.0 → bottom (south pole)</li>
 *   <li>lonStart=0.0 → 0°</li>
 *   <li>lonEnd=1.0 → 360°</li>
 * </ul>
 * 
 * <h2>Parts</h2>
 * <ul>
 *   <li><b>main</b> (QUAD) - Main sphere surface</li>
 *   <li><b>poles</b> (TRIANGLE) - Top/bottom pole caps</li>
 *   <li><b>equator</b> (QUAD) - Equatorial band</li>
 *   <li><b>hemisphereTop</b> (QUAD) - Top half</li>
 *   <li><b>hemisphereBottom</b> (QUAD) - Bottom half</li>
 * </ul>
 * 
 * @see SphereAlgorithm
 */
public record SphereShape(
    @Range(ValueRange.RADIUS) float radius,
    @Range(ValueRange.STEPS) int latSteps,
    @Range(ValueRange.STEPS) int lonSteps,
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true) float latStart,
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true, defaultValue = "1") float latEnd,
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true) float lonStart,
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true, defaultValue = "1") float lonEnd,
    @JsonField(skipIfEqualsConstant = "LAT_LON") SphereAlgorithm algorithm,
    // === Deformation (transform sphere into droplet, egg, etc.) ===
    @JsonField(skipIfDefault = true) SphereDeformation deformation,
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true) float deformationIntensity
)implements Shape {
    public static final String DEFAULT_ALGORITHM = "uv";

    
    /** Default sphere (1.0 radius, medium detail, full sphere). */
    public static SphereShape of(float radius) { 
        return new SphereShape(radius, 16, 32, 0f, 1f, 0f, 1f, SphereAlgorithm.values()[0], 
            SphereDeformation.NONE, 0f); 
    }
    public static SphereShape defaults() { return DEFAULT; }
    
    public static final SphereShape DEFAULT = new SphereShape(
        1.0f, 32, 64, 0.0f, 1.0f, 0.0f, 1.0f, SphereAlgorithm.LAT_LON,
        SphereDeformation.NONE, 0f);
    
    /** Low-poly sphere for performance. */
    public static final SphereShape LOW_POLY = new SphereShape(
        1.0f, 8, 16, 0.0f, 1.0f, 0.0f, 1.0f, SphereAlgorithm.LAT_LON,
        SphereDeformation.NONE, 0f);
    
    /** High-detail sphere. */
    public static final SphereShape HIGH_DETAIL = new SphereShape(
        1.0f, 64, 128, 0.0f, 1.0f, 0.0f, 1.0f, SphereAlgorithm.LAT_LON,
        SphereDeformation.NONE, 0f);
    
    /**
     * Creates a simple sphere with default tessellation.
     * @param radius Sphere radius
     */
    public static SphereShape ofRadius(@Range(ValueRange.RADIUS) float radius) {
        return new SphereShape(radius, 32, 64, 0.0f, 1.0f, 0.0f, 1.0f, SphereAlgorithm.LAT_LON,
            SphereDeformation.NONE, 0f);
    }
    
    /**
     * Creates a top hemisphere (top half of sphere).
     * @param radius Sphere radius
     */
    public static SphereShape hemisphereTop(@Range(ValueRange.RADIUS) float radius) {
        return new SphereShape(radius, 16, 64, 0.0f, 0.5f, 0.0f, 1.0f, SphereAlgorithm.LAT_LON,
            SphereDeformation.NONE, 0f);
    }
    
    /**
     * Creates a bottom hemisphere (bottom half of sphere).
     * @param radius Sphere radius
     */
    public static SphereShape hemisphereBottom(@Range(ValueRange.RADIUS) float radius) {
        return new SphereShape(radius, 16, 64, 0.5f, 1.0f, 0.0f, 1.0f, SphereAlgorithm.LAT_LON,
            SphereDeformation.NONE, 0f);
    }
    
    @Override
    public String getType() {
        return "sphere";
    }
    
    @Override
    public Vector3f getBounds() {
        float d = radius * 2;
        return new Vector3f(d, d, d);
    }
    
    @Override
    public CellType primaryCellType() {
        return CellType.QUAD;
    }
    
    @Override
    public Map<String, CellType> getParts() {
        return Map.of(
            "main", CellType.QUAD,
            "poles", CellType.TRIANGLE,
            "equator", CellType.QUAD,
            "hemisphereTop", CellType.QUAD,
            "hemisphereBottom", CellType.QUAD
        );
    }
    
    @Override
    public float getRadius() {
        return radius;
    }
    
    /** Whether this is a full sphere (lat 0-1, lon 0-1). */
    public boolean isFullSphere() {
        return latStart == 0.0f && latEnd == 1.0f && lonStart == 0.0f && lonEnd == 1.0f;
    }
    

    // =========================================================================
    // JSON Parsing
    // =========================================================================
    
    /**
     * Parses a SphereShape from JSON.
     * @param json The JSON object
     * @return Parsed shape
     */
    public static SphereShape fromJson(JsonObject json) {
        Logging.FIELD.topic("parse").trace("Parsing SphereShape...");
        
        float radius = json.has("radius") ? json.get("radius").getAsFloat() : 1.0f;
        int latSteps = json.has("latSteps") ? json.get("latSteps").getAsInt() : 32;
        int lonSteps = json.has("lonSteps") ? json.get("lonSteps").getAsInt() : 64;
        float latStart = json.has("latStart") ? json.get("latStart").getAsFloat() : 0.0f;
        float latEnd = json.has("latEnd") ? json.get("latEnd").getAsFloat() : 1.0f;
        float lonStart = json.has("lonStart") ? json.get("lonStart").getAsFloat() : 0.0f;
        float lonEnd = json.has("lonEnd") ? json.get("lonEnd").getAsFloat() : 1.0f;
        
        SphereAlgorithm algorithm = SphereAlgorithm.LAT_LON;
        if (json.has("algorithm")) {
            String algStr = json.get("algorithm").getAsString();
            try {
                algorithm = SphereAlgorithm.valueOf(algStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                Logging.FIELD.topic("parse").warn("Invalid sphere algorithm '{}', using LAT_LON", algStr);
            }
        }
        
        SphereShape result = new SphereShape(radius, latSteps, lonSteps, latStart, latEnd, lonStart, lonEnd, algorithm, SphereDeformation.NONE, 0f);
        Logging.FIELD.topic("parse").trace("Parsed SphereShape: radius={}, latSteps={}, lonSteps={}, algorithm={}", 
            radius, latSteps, lonSteps, algorithm);
        return result;
    }
    
    @Override
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }

    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    /** Create a builder pre-populated with this shape's values. */
    public Builder toBuilder() {
        return new Builder()
            .radius(radius)
            .latSteps(latSteps)
            .lonSteps(lonSteps)
            .latStart(latStart)
            .latEnd(latEnd)
            .lonStart(lonStart)
            .lonEnd(lonEnd)
            .algorithm(algorithm)
            .deformation(deformation)
            .deformationIntensity(deformationIntensity);
    }
    
    /** Returns effective deformation, defaulting to NONE if null. */
    public SphereDeformation effectiveDeformation() {
        return deformation != null ? deformation : SphereDeformation.NONE;
    }
    
    /** Whether deformation is active. */
    public boolean hasDeformation() {
        return deformation != null && deformation != SphereDeformation.NONE && deformationIntensity > 0;
    }
    
    public static class Builder {
        private @Range(ValueRange.RADIUS) float radius = 1.0f;
        private @Range(ValueRange.STEPS) int latSteps = 32;
        private @Range(ValueRange.STEPS) int lonSteps = 64;
        private @Range(ValueRange.NORMALIZED) float latStart = 0.0f;
        private @Range(ValueRange.NORMALIZED) float latEnd = 1.0f;
        private @Range(ValueRange.NORMALIZED) float lonStart = 0.0f;
        private @Range(ValueRange.NORMALIZED) float lonEnd = 1.0f;
        private SphereAlgorithm algorithm = SphereAlgorithm.LAT_LON;
        private SphereDeformation deformation = SphereDeformation.NONE;
        private float deformationIntensity = 0f;
        
        public Builder radius(float r) { this.radius = r; return this; }
        public Builder latSteps(int s) { this.latSteps = s; return this; }
        public Builder lonSteps(int s) { this.lonSteps = s; return this; }
        public Builder latStart(float l) { this.latStart = l; return this; }
        public Builder latEnd(float l) { this.latEnd = l; return this; }
        public Builder lonStart(float l) { this.lonStart = l; return this; }
        public Builder lonEnd(float l) { this.lonEnd = l; return this; }
        public Builder algorithm(SphereAlgorithm a) { this.algorithm = a != null ? a : SphereAlgorithm.LAT_LON; return this; }
        public Builder deformation(SphereDeformation d) { this.deformation = d != null ? d : SphereDeformation.NONE; return this; }
        public Builder deformationIntensity(float i) { this.deformationIntensity = i; return this; }
        
        public SphereShape build() {
            return new SphereShape(radius, latSteps, lonSteps, latStart, latEnd, lonStart, lonEnd, algorithm,
                deformation, deformationIntensity);
        }
    }
}
