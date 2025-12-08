package net.cyberpunk042.visual.pattern;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

/**
 * Configuration for vertex arrangement patterns, supporting multi-part shapes.
 * 
 * <p>Each shape can have different patterns for different parts.
 * If a part is not specified, the default pattern is used.</p>
 * 
 * <h2>Shape Parts</h2>
 * <ul>
 *   <li><b>Sphere:</b> main, poles, equator, hemisphereTop, hemisphereBottom</li>
 *   <li><b>Ring:</b> surface, innerEdge, outerEdge</li>
 *   <li><b>Disc:</b> surface, edge</li>
 *   <li><b>Prism:</b> sides, capTop, capBottom, edges</li>
 *   <li><b>Polyhedron:</b> faces, edges, vertices</li>
 * </ul>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * // Simple (all parts use same pattern)
 * "arrangement": "wave_1"
 * 
 * // Multi-part
 * "arrangement": {
 *   "default": "filled_1",
 *   "poles": "sparse",
 *   "equator": "alternating"
 * }
 * </pre>
 */
public record ArrangementConfig(
    // Default for all parts
    String defaultPattern,
    
    // Sphere parts
    @Nullable String main,
    @Nullable String poles,
    @Nullable String equator,
    @Nullable String hemisphereTop,
    @Nullable String hemisphereBottom,
    
    // Ring parts
    @Nullable String surface,
    @Nullable String innerEdge,
    @Nullable String outerEdge,
    
    // Disc parts
    String discEdge,
    
    // Prism/Cylinder parts
    @Nullable String sides,
    @Nullable String capTop,
    @Nullable String capBottom,
    String prismEdges,
    
    // Polyhedron parts
    @Nullable String faces,
    String polyEdges,
    @Nullable String vertices
) {
    /** Default arrangement (filled_1 for all parts). */
    public static final ArrangementConfig DEFAULT = of("filled_1");
    
    /**
     * Creates a simple arrangement where all parts use the same pattern.
     * @param pattern Pattern name (e.g., "filled_1", "wave_1")
     */
    public static ArrangementConfig of(String pattern) {
        return new ArrangementConfig(
            pattern,
            null, null, null, null, null,  // sphere
            null, null, null,               // ring
            null,                           // disc
            null, null, null, null,         // prism
            null, null, null                // polyhedron
        );
    }
    
    /**
     * Gets the pattern for a named part, or default if not specified.
     * @param partName The part name (e.g., "poles", "surface")
     * @return The pattern name for that part
     */
    public String getPattern(String partName) {
        String specific = switch (partName) {
            case "main" -> main;
            case "poles" -> poles;
            case "equator" -> equator;
            case "hemisphereTop" -> hemisphereTop;
            case "hemisphereBottom" -> hemisphereBottom;
            case "surface" -> surface;
            case "innerEdge" -> innerEdge;
            case "outerEdge" -> outerEdge;
            case "edge", "discEdge" -> discEdge;
            case "sides" -> sides;
            case "capTop" -> capTop;
            case "capBottom" -> capBottom;
            case "edges", "prismEdges" -> prismEdges;
            case "faces" -> faces;
            case "polyEdges" -> polyEdges;
            case "vertices" -> vertices;
            default -> null;
        };
        return specific != null ? specific : defaultPattern;
    }
    
    /**
     * Resolves the pattern for a part to an actual VertexPattern.
     * @param partName The part name
     * @return The VertexPattern for that part
     */
    public VertexPattern resolvePattern(String partName) {
        String patternName = getPattern(partName);
        return VertexPattern.fromString(patternName);
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    /**
     * Serializes this arrangement config to JSON.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("default", defaultPattern);
        // Sphere parts
        if (main != null) json.addProperty("main", main);
        if (poles != null) json.addProperty("poles", poles);
        if (equator != null) json.addProperty("equator", equator);
        if (hemisphereTop != null) json.addProperty("hemisphereTop", hemisphereTop);
        if (hemisphereBottom != null) json.addProperty("hemisphereBottom", hemisphereBottom);
        // Ring parts
        if (surface != null) json.addProperty("surface", surface);
        if (innerEdge != null) json.addProperty("innerEdge", innerEdge);
        if (outerEdge != null) json.addProperty("outerEdge", outerEdge);
        // Disc parts
        if (discEdge != null) json.addProperty("discEdge", discEdge);
        // Prism/Cylinder parts
        if (sides != null) json.addProperty("sides", sides);
        if (capTop != null) json.addProperty("capTop", capTop);
        if (capBottom != null) json.addProperty("capBottom", capBottom);
        if (prismEdges != null) json.addProperty("prismEdges", prismEdges);
        // Polyhedron parts
        if (faces != null) json.addProperty("faces", faces);
        if (polyEdges != null) json.addProperty("polyEdges", polyEdges);
        if (vertices != null) json.addProperty("vertices", vertices);
        return json;
    }


    
    public static class Builder {
        private String defaultPattern = "filled_1";
        private @Nullable String main, poles, equator, hemisphereTop, hemisphereBottom;
        private @Nullable String surface, innerEdge, outerEdge;
        private String discEdge;
        private @Nullable String sides, capTop, capBottom, prismEdges;
        private @Nullable String faces, polyEdges, vertices;
        
        public Builder defaultPattern(String p) { this.defaultPattern = p; return this; }
        public Builder main(String p) { this.main = p; return this; }
        public Builder poles(String p) { this.poles = p; return this; }
        public Builder equator(String p) { this.equator = p; return this; }
        public Builder hemisphereTop(String p) { this.hemisphereTop = p; return this; }
        public Builder hemisphereBottom(String p) { this.hemisphereBottom = p; return this; }
        public Builder surface(String p) { this.surface = p; return this; }
        public Builder innerEdge(String p) { this.innerEdge = p; return this; }
        public Builder outerEdge(String p) { this.outerEdge = p; return this; }
        public Builder discEdge(String p) { this.discEdge = p; return this; }
        public Builder sides(String p) { this.sides = p; return this; }
        public Builder capTop(String p) { this.capTop = p; return this; }
        public Builder capBottom(String p) { this.capBottom = p; return this; }
        public Builder prismEdges(String p) { this.prismEdges = p; return this; }
        public Builder faces(String p) { this.faces = p; return this; }
        public Builder polyEdges(String p) { this.polyEdges = p; return this; }
        public Builder vertices(String p) { this.vertices = p; return this; }
        
        public ArrangementConfig build() {
            return new ArrangementConfig(
                defaultPattern,
                main, poles, equator, hemisphereTop, hemisphereBottom,
                surface, innerEdge, outerEdge,
                discEdge,
                sides, capTop, capBottom, prismEdges,
                faces, polyEdges, vertices
            );
        }
    }
}
