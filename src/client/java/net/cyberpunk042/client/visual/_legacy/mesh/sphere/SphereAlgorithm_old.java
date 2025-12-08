package net.cyberpunk042.client.visual._legacy.mesh.sphere;

import net.cyberpunk042.log.Logging;

/**
 * Sphere rendering algorithm selection.
 *
 * <h2>Algorithm Comparison</h2>
 * <table>
 *   <tr><th>Algorithm</th><th>Visual</th><th>Performance</th><th>Best For</th></tr>
 *   <tr><td>LAT_LON</td><td>Smooth, precise</td><td>Medium</td><td>Banded/patterned effects</td></tr>
 *   <tr><td>TYPE_A</td><td>Very smooth</td><td>Lower</td><td>Close-up, static spheres</td></tr>
 *   <tr><td>TYPE_E</td><td>Good at distance</td><td>High</td><td>LOD, animations, many spheres</td></tr>
 * </table>
 *
 * <h2>Usage</h2>
 * <pre>
 * // Select algorithm based on distance
 * SphereAlgorithm_old algo = SphereAlgorithm_old.forDistance(distanceToCamera);
 * </pre>
 *
 * @see TypeASphereRenderer
 * @see TypeESphereRenderer
 */
public enum SphereAlgorithm_old {

    /**
     * Latitude/Longitude tessellation grid.
     * <ul>
     *   <li>Produces triangle mesh from lat/lon divisions</li>
     *   <li>Best for banded, striped, or patterned effects</li>
     *   <li>Easy partial sphere rendering (hemispheres, caps)</li>
     *   <li>Pole artifacts possible with low tessellation</li>
     * </ul>
     */
    LAT_LON("lat_lon", "Lat/Lon Grid"),

    /**
     * Type A: Overlapping axis-aligned cubes.
     * <ul>
     *   <li>Different aspect ratios per vertical layer</li>
     *   <li>Adaptive horizontal detail (fewer at poles)</li>
     *   <li>More elements, smoother appearance</li>
     *   <li>Best for close-up, static viewing</li>
     * </ul>
     */
    TYPE_A("type_a", "Overlapping Cubes"),

    /**
     * Type E: Rotated rectangles forming 16-sided polygon.
     * <ul>
     *   <li>Thin rectangles rotated around Y-axis</li>
     *   <li>Fewest elements, still looks round</li>
     *   <li>Best for distant objects, animations</li>
     *   <li>Ideal for LOD (Level of Detail) fallback</li>
     * </ul>
     */
    TYPE_E("type_e", "Rotated Rectangles");

    private final String id;
    private final String displayName;

    SphereAlgorithm_old(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * Parses algorithm from string ID.
     *
     * @param id The algorithm ID (e.g., "type_a", "lat_lon")
     * @return The matching algorithm, or LAT_LON if not found
     */
    public static SphereAlgorithm_old fromId(String id) {
        if (id == null || id.isEmpty()) {
            return LAT_LON;
        }
        for (SphereAlgorithm_old algo : values()) {
            if (algo.id.equalsIgnoreCase(id)) {
                return algo;
            }
        }
        Logging.RENDER.topic("sphere").warn("Unknown sphere algorithm '{}', defaulting to LAT_LON", id);
        return LAT_LON;
    }

    /**
     * Selects appropriate algorithm based on distance from camera.
     * Implements automatic LOD selection.
     *
     * @param distance Distance from camera in blocks
     * @return Recommended algorithm for that distance
     */
    public static SphereAlgorithm_old forDistance(double distance) {
        if (distance < 16.0) {
            // Close: use accurate Type A
            return TYPE_A;
        } else if (distance < 64.0) {
            // Medium: use standard lat/lon
            return LAT_LON;
        } else {
            // Far: use efficient Type E
            return TYPE_E;
        }
    }

    /**
     * Selects algorithm based on context.
     *
     * @param distance Distance from camera
     * @param needsPatterns Whether patterns (bands, checker) are needed
     * @param needsPartial Whether partial sphere (hemisphere) is needed
     * @return Best algorithm for the context
     */
    public static SphereAlgorithm_old forContext(double distance, boolean needsPatterns, boolean needsPartial) {
        // Patterns and partial spheres require LAT_LON
        if (needsPatterns || needsPartial) {
            return LAT_LON;
        }
        return forDistance(distance);
    }

    @Override
    public String toString() {
        return displayName + " (" + id + ")";
    }
}
