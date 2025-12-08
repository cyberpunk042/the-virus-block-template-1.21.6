package net.cyberpunk042.visual.pattern;

/**
 * Patterns for segment-based tessellation (rings, arcs).
 * 
 * <p>Controls which segments are rendered and how they connect.
 * 
 * <h2>Available Patterns</h2>
 * <ul>
 *   <li><b>DEFAULT</b>: All segments rendered</li>
 *   <li><b>ALTERNATING</b>: Every other segment (dashed look)</li>
 *   <li><b>SPARSE</b>: Every third segment</li>
 *   <li><b>DOUBLED</b>: Double-width segments</li>
 *   <li><b>REVERSED</b>: Reverse winding order</li>
 *   <li><b>TAPERED</b>: Segments taper toward one end</li>
 * </ul>
 * 
 * @see VertexPattern
 */
public enum SegmentPattern implements VertexPattern {
    
    /** All segments rendered normally. */
    DEFAULT("default", 1, false),
    
    /** Every other segment (dashed ring). */
    ALTERNATING("alternating", 2, false),
    
    /** Every third segment (sparse ring). */
    SPARSE("sparse", 3, false),
    
    /** Every fourth segment. */
    QUARTER("quarter", 4, false),
    
    /** Reverse winding order (flip normals). */
    REVERSED("reversed", 1, true),
    
    /** Alternating with reversed winding. */
    ZIGZAG("zigzag", 2, true);
    
    private final String id;
    private final int skipInterval;
    private final boolean reverseWinding;
    
    SegmentPattern(String id, int skipInterval, boolean reverseWinding) {
        this.id = id;
        this.skipInterval = skipInterval;
        this.reverseWinding = reverseWinding;
    }
    
    @Override
    public String id() {
        return id;
    }
    
    @Override
    public PatternGeometry geometry() {
        return PatternGeometry.SEGMENT;
    }
    
    /**
     * Determines if a segment index should be rendered.
     * @param index Segment index (0-based)
     * @return true if this segment should be rendered
     */
    public boolean shouldRender(int index) {
        return (index % skipInterval) == 0;
    }
    
    /**
     * Whether to reverse winding order for this pattern.
     */
    public boolean reverseWinding() {
        return reverseWinding;
    }
    
    /**
     * Skip interval (1 = all, 2 = every other, etc.)
     */
    public int skipInterval() {
        return skipInterval;
    }
    
    /**
     * Parses a pattern from ID string.
     * @return The pattern, or null if not found
     */
    public static SegmentPattern fromId(String id) {
        if (id == null) return null;
        String lower = id.toLowerCase().trim();
        for (SegmentPattern p : values()) {
            if (p.id.equals(lower)) return p;
        }
        return null;
    }
    
    /**
     * Lists all pattern IDs.
     */
    public static String[] ids() {
        SegmentPattern[] values = values();
        String[] ids = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            ids[i] = values[i].id;
        }
        return ids;
    }
}

