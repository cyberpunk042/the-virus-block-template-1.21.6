package net.cyberpunk042.visual.pattern;

/**
 * Abstract pattern for controlling vertex arrangement in tessellation.
 * 
 * <p>Different geometry types have different pattern implementations:
 * <ul>
 *   <li>{@link QuadPattern} - For quad-based tessellation (spheres, prism sides)</li>
 *   <li>{@link SegmentPattern} - For segment-based tessellation (rings)</li>
 *   <li>{@link SectorPattern} - For radial/fan tessellation (discs)</li>
 *   <li>{@link EdgePattern} - For edge-based rendering (cages, wireframes)</li>
 * </ul>
 * 
 * <p>Each tessellator interprets patterns specific to its geometry type.
 * If a pattern doesn't match the geometry, it falls back to DEFAULT.
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "appearance": {
 *   "pattern": {
 *     "type": "bands",
 *     "count": 4,
 *     "vertexPattern": "meshed"
 *   }
 * }
 * </pre>
 * 
 * <h2>Live Editing</h2>
 * <pre>
 * /fieldtest edit vertex meshed
 * /fieldtest edit vertex spiral
 * </pre>
 */
public interface VertexPattern {
    
    /**
     * Pattern identifier (lowercase).
     */
    String id();
    
    /**
     * Display name for UI/commands.
     */
    default String displayName() {
        return id();
    }
    
    /**
     * Which geometry type this pattern is designed for.
     */
    PatternGeometry geometry();
    
    /**
     * Parses any pattern from string, auto-detecting type.
     * @param value Pattern name (e.g., "meshed", "spiral", "alternating")
     * @return The pattern, or null if not found
     */
    static VertexPattern fromString(String value) {
        if (value == null || value.isEmpty()) {
            return QuadPattern.DEFAULT;
        }
        
        String lower = value.toLowerCase().trim();
        
        // Try each pattern type
        VertexPattern pattern = QuadPattern.fromId(lower);
        if (pattern != null) return pattern;
        
        pattern = SegmentPattern.fromId(lower);
        if (pattern != null) return pattern;
        
        pattern = SectorPattern.fromId(lower);
        if (pattern != null) return pattern;
        
        pattern = EdgePattern.fromId(lower);
        if (pattern != null) return pattern;
        
        return QuadPattern.DEFAULT;
    }
    
    /**
     * Returns all pattern names across all types.
     */
    static String[] allNames() {
        var names = new java.util.ArrayList<String>();
        for (QuadPattern p : QuadPattern.values()) names.add(p.id());
        for (SegmentPattern p : SegmentPattern.values()) names.add(p.id());
        for (SectorPattern p : SectorPattern.values()) names.add(p.id());
        for (EdgePattern p : EdgePattern.values()) names.add(p.id());
        return names.toArray(new String[0]);
    }
    
    /**
     * Geometry types that patterns apply to.
     */
    enum PatternGeometry {
        /** Quad-based: spheres (lat/lon), prism sides */
        QUAD,
        /** Segment-based: rings, arcs */
        SEGMENT,
        /** Radial/fan: discs, pie charts */
        SECTOR,
        /** Edge-based: cages, wireframes */
        EDGE,
        /** Universal: applies to any geometry */
        ANY
    }
}

