package net.cyberpunk042.visual.shape;

/**
 * Defines sphere tessellation algorithms.
 * 
 * <p>Different algorithms produce different vertex distributions
 * and are suited for different use cases.</p>
 * 
 * <h3>Algorithm Comparison</h3>
 * <ul>
 *   <li><b>LAT_LON</b>: Traditional latitude/longitude grid. Best for patterns,
 *       partial spheres, and visibility masks. Has pole singularities.</li>
 *   <li><b>TYPE_A</b>: Overlapping cubes projection. More uniform distribution,
 *       best for accurate close-up rendering. Higher vertex count.</li>
 *   <li><b>TYPE_E</b>: Rotated rectangles projection. Efficient for distant
 *       rendering and LOD. Good balance of quality and performance.</li>
 * </ul>
 * 
 * @see SphereShape
 */
public enum SphereAlgorithm {
    UV("uv", "UV Sphere"),
    /** Latitude/longitude tessellation (DEFAULT) - best for patterns and partial spheres */
    LAT_LON("lat_lon", "Latitude/Longitude grid"),
    
    /** Overlapping cubes projection - best for accurate close-up rendering */
    TYPE_A("type_a", "Overlapping cubes"),
    
    /** Rotated rectangles projection - best for distant/LOD rendering */
    TYPE_E("type_e", "Rotated rectangles");
    
    private final String id;
    private final String description;
    
    SphereAlgorithm(String id, String description) {
        this.id = id;
        this.description = description;
    }
    
    /** String identifier for JSON/commands */
    public String id() { return id; }
    
    /** Human-readable description */
    public String description() { return description; }
    
    /** Returns true if this is the default algorithm */
    public boolean isDefault() { return this == LAT_LON; }
    
    /** Returns true if this algorithm supports partial spheres */
    public boolean supportsPartialSphere() {
        return this == LAT_LON;
    }
    
    /** Returns true if this algorithm supports visibility patterns */
    public boolean supportsPatterns() {
        return this == LAT_LON;
    }
    
    /**
     * Parse from string (case-insensitive).
     * @param id Algorithm identifier
     * @return Matching algorithm, or LAT_LON as default
     */
    public static SphereAlgorithm fromId(String id) {
        if (id == null || id.isEmpty()) {
            return LAT_LON;
        }
        for (SphereAlgorithm algo : values()) {
            if (algo.id.equalsIgnoreCase(id)) {
                return algo;
            }
        }
        return LAT_LON;
    }
}
