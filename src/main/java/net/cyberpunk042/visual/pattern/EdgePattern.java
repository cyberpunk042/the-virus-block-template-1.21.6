package net.cyberpunk042.visual.pattern;

/**
 * Patterns for edge-based rendering (cages, wireframes).
 * 
 * <p>Controls which edges are rendered in wireframe mode.
 * 
 * <h2>Available Patterns</h2>
 * <ul>
 *   <li><b>DEFAULT</b>: All edges</li>
 *   <li><b>SPARSE</b>: Fewer edges (performance)</li>
 *   <li><b>DENSE</b>: More edges (detail)</li>
 *   <li><b>LATITUDE</b>: Horizontal lines only</li>
 *   <li><b>LONGITUDE</b>: Vertical lines only</li>
 *   <li><b>DASHED</b>: Alternating segments</li>
 * </ul>
 * 
 * @see VertexPattern
 */
public enum EdgePattern implements VertexPattern {
    
    /** All edges rendered. */
    DEFAULT("default", true, true, 1),
    
    /** Latitude lines only (horizontal rings). */
    LATITUDE("latitude", true, false, 1),
    
    /** Longitude lines only (vertical arcs). */
    LONGITUDE("longitude", false, true, 1),
    
    /** Sparse - every other edge. */
    SPARSE("sparse", true, true, 2),
    
    /** Very sparse - every fourth edge. */
    MINIMAL("minimal", true, true, 4),
    
    /** Dashed - alternating segments. */
    DASHED("dashed", true, true, 2),
    
    /** Grid pattern - both lat and lon. */
    GRID("grid", true, true, 1);
    
    private final String id;
    private final boolean renderLatitude;
    private final boolean renderLongitude;
    private final int skipInterval;
    
    EdgePattern(String id, boolean renderLatitude, boolean renderLongitude, int skipInterval) {
        this.id = id;
        this.renderLatitude = renderLatitude;
        this.renderLongitude = renderLongitude;
        this.skipInterval = skipInterval;
    }
    
    @Override
    public String id() {
        return id;
    }
    
    @Override
    public PatternGeometry geometry() {
        return PatternGeometry.EDGE;
    }
    
    /**
     * Whether to render latitude (horizontal) lines.
     */
    public boolean renderLatitude() {
        return renderLatitude;
    }
    
    /**
     * Whether to render longitude (vertical) lines.
     */
    public boolean renderLongitude() {
        return renderLongitude;
    }
    
    /**
     * Determines if an edge at given index should be rendered.
     * @param index Edge index (0-based)
     * @return true if this edge should be rendered
     */
    public boolean shouldRender(int index) {
        return (index % skipInterval) == 0;
    }
    
    /**
     * Parses a pattern from ID string.
     * @return The pattern, or null if not found
     */
    public static EdgePattern fromId(String id) {
        if (id == null) return null;
        String lower = id.toLowerCase().trim();
        for (EdgePattern p : values()) {
            if (p.id.equals(lower)) return p;
        }
        return null;
    }
    
    /**
     * Lists all pattern IDs.
     */
    public static String[] ids() {
        EdgePattern[] values = values();
        String[] ids = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            ids[i] = values[i].id;
        }
        return ids;
    }
}

