package net.cyberpunk042.visual.pattern;

/**
 * Patterns for radial/fan tessellation (discs, pie charts).
 * 
 * <p>Controls which sectors are rendered in a disc.
 * 
 * <h2>Available Patterns</h2>
 * <ul>
 *   <li><b>DEFAULT</b>: Full disc</li>
 *   <li><b>HALF</b>: Half disc (semicircle)</li>
 *   <li><b>QUARTERS</b>: Quarter sectors only</li>
 *   <li><b>SPIRAL</b>: Spiral arrangement</li>
 *   <li><b>PINWHEEL</b>: Pinwheel/fan pattern</li>
 *   <li><b>CROSSHAIR</b>: Cross pattern</li>
 * </ul>
 * 
 * @see VertexPattern
 */
public enum SectorPattern implements VertexPattern {
    
    /** Full disc - all sectors. */
    DEFAULT("default", 1, 0),
    
    /** Half disc (semicircle). */
    HALF("half", 2, 0),
    
    /** Quarter sectors only. */
    QUARTERS("quarters", 4, 0),
    
    /** Every other sector (pinwheel). */
    PINWHEEL("pinwheel", 2, 0),
    
    /** Every third sector. */
    TRISECTOR("trisector", 3, 0),
    
    /** Alternating with offset. */
    SPIRAL("spiral", 2, 1),
    
    /** Only cardinal directions. */
    CROSSHAIR("crosshair", 4, 0);
    
    private final String id;
    private final int skipInterval;
    private final int phaseOffset;
    
    SectorPattern(String id, int skipInterval, int phaseOffset) {
        this.id = id;
        this.skipInterval = skipInterval;
        this.phaseOffset = phaseOffset;
    }
    
    @Override
    public String id() {
        return id;
    }
    
    @Override
    public PatternGeometry geometry() {
        return PatternGeometry.SECTOR;
    }
    
    /**
     * Determines if a sector index should be rendered.
     * @param index Sector index (0-based)
     * @param totalSectors Total number of sectors
     * @return true if this sector should be rendered
     */
    public boolean shouldRender(int index, int totalSectors) {
        if (skipInterval <= 1) return true;
        int adjusted = (index + phaseOffset) % totalSectors;
        return (adjusted % skipInterval) == 0;
    }
    
    /**
     * Parses a pattern from ID string.
     * @return The pattern, or null if not found
     */
    public static SectorPattern fromId(String id) {
        if (id == null) return null;
        String lower = id.toLowerCase().trim();
        for (SectorPattern p : values()) {
            if (p.id.equals(lower)) return p;
        }
        return null;
    }
    
    /**
     * Lists all pattern IDs.
     */
    public static String[] ids() {
        SectorPattern[] values = values();
        String[] ids = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            ids[i] = values[i].id;
        }
        return ids;
    }
}

