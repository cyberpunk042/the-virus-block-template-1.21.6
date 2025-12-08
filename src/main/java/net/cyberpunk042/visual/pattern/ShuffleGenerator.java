package net.cyberpunk042.visual.pattern;

import net.cyberpunk042.log.Logging;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates ALL permutations of vertex patterns for exploration.
 * 
 * <p>Use this to discover new interesting patterns by shuffling through
 * all possible vertex arrangements for each geometry type.
 * 
 * <h2>Usage</h2>
 * <pre>
 * /fieldtest shuffle next     - Cycle to next permutation
 * /fieldtest shuffle prev     - Go back one
 * /fieldtest shuffle jump 42  - Jump to permutation #42
 * /fieldtest shuffle type quad - Switch to quad patterns
 * </pre>
 * 
 * <h2>Geometry Types</h2>
 * <ul>
 *   <li><b>QUAD</b>: All ways to arrange 4 corners into triangles</li>
 *   <li><b>SEGMENT</b>: All skip/phase combinations for rings</li>
 *   <li><b>SECTOR</b>: All skip/phase combinations for discs</li>
 *   <li><b>EDGE</b>: All lat/lon/skip combinations for wireframes</li>
 * </ul>
 */
public final class ShuffleGenerator {
    
    // =========================================================================
    // Corner Definition (for quads)
    // =========================================================================
    
    public enum Corner {
        TL("TopLeft"),
        TR("TopRight"),
        BL("BottomLeft"),
        BR("BottomRight");
        
        private final String display;
        Corner(String display) { this.display = display; }
        public String display() { return display; }
        public String shortName() { return name(); }
    }
    
    // =========================================================================
    // Generated Quad Patterns
    // =========================================================================
    
    /**
     * A single generated quad arrangement.
     */
    public record QuadArrangement(
        Corner[] tri1,      // First triangle (3 corners)
        Corner[] tri2,      // Second triangle (3 corners, can be null for single-tri)
        int index,          // Index in the full list
        int total           // Total arrangements
    ) {
        public String describe() {
            StringBuilder sb = new StringBuilder();
            sb.append("Tri1: ");
            sb.append(tri1[0].shortName()).append("→");
            sb.append(tri1[1].shortName()).append("→");
            sb.append(tri1[2].shortName());
            
            if (tri2 != null) {
                sb.append(" | Tri2: ");
                sb.append(tri2[0].shortName()).append("→");
                sb.append(tri2[1].shortName()).append("→");
                sb.append(tri2[2].shortName());
            } else {
                sb.append(" | (single triangle)");
            }
            return sb.toString();
        }
        
        public String toLogString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n  Triangle 1: ");
            sb.append(tri1[0].display()).append(" → ");
            sb.append(tri1[1].display()).append(" → ");
            sb.append(tri1[2].display());
            
            if (tri2 != null) {
                sb.append("\n  Triangle 2: ");
                sb.append(tri2[0].display()).append(" → ");
                sb.append(tri2[1].display()).append(" → ");
                sb.append(tri2[2].display());
            } else {
                sb.append("\n  (Single triangle only)");
            }
            return sb.toString();
        }
        
        /**
         * Converts to a QuadPattern.Corner array for use with MeshBuilder.
         */
        public List<QuadPattern.Corner[]> toQuadPatternTriangles() {
            List<QuadPattern.Corner[]> result = new ArrayList<>();
            result.add(new QuadPattern.Corner[]{
                mapCorner(tri1[0]), mapCorner(tri1[1]), mapCorner(tri1[2])
            });
            if (tri2 != null) {
                result.add(new QuadPattern.Corner[]{
                    mapCorner(tri2[0]), mapCorner(tri2[1]), mapCorner(tri2[2])
                });
            }
            return result;
        }
        
        private QuadPattern.Corner mapCorner(Corner c) {
            return switch (c) {
                case TL -> QuadPattern.Corner.TOP_LEFT;
                case TR -> QuadPattern.Corner.TOP_RIGHT;
                case BL -> QuadPattern.Corner.BOTTOM_LEFT;
                case BR -> QuadPattern.Corner.BOTTOM_RIGHT;
            };
        }
    }
    
    /**
     * A generated segment arrangement for rings.
     */
    public record SegmentArrangement(
        int skipInterval,
        int phaseOffset,
        boolean reverseWinding,
        int index,
        int total
    ) {
        public String describe() {
            return String.format("skip=%d phase=%d %s", 
                skipInterval, phaseOffset, reverseWinding ? "reversed" : "normal");
        }
        
        public String toLogString() {
            return String.format(
                "\n  Skip Interval: %d (render every %s segment)" +
                "\n  Phase Offset: %d" +
                "\n  Winding: %s",
                skipInterval,
                skipInterval == 1 ? "" : ordinal(skipInterval),
                phaseOffset,
                reverseWinding ? "REVERSED" : "normal"
            );
        }
        
        private String ordinal(int n) {
            return switch (n) {
                case 1 -> "1st";
                case 2 -> "2nd";
                case 3 -> "3rd";
                default -> n + "th";
            };
        }
    }
    
    /**
     * A generated sector arrangement for discs.
     */
    public record SectorArrangement(
        int skipInterval,
        int phaseOffset,
        boolean invertSelection,
        int index,
        int total
    ) {
        public String describe() {
            return String.format("skip=%d phase=%d %s", 
                skipInterval, phaseOffset, invertSelection ? "inverted" : "normal");
        }
        
        public String toLogString() {
            return String.format(
                "\n  Skip Interval: %d" +
                "\n  Phase Offset: %d" +
                "\n  Selection: %s",
                skipInterval, phaseOffset,
                invertSelection ? "INVERTED" : "normal"
            );
        }
    }
    
    /**
     * A generated edge arrangement for wireframes.
     */
    public record EdgeArrangement(
        boolean latitude,
        boolean longitude,
        int skipInterval,
        int index,
        int total
    ) {
        public String describe() {
            String dirs = (latitude ? "LAT" : "") + (latitude && longitude ? "+" : "") + (longitude ? "LON" : "");
            return String.format("%s skip=%d", dirs, skipInterval);
        }
        
        public String toLogString() {
            return String.format(
                "\n  Latitude Lines: %s" +
                "\n  Longitude Lines: %s" +
                "\n  Skip Interval: %d",
                latitude ? "YES" : "no",
                longitude ? "YES" : "no",
                skipInterval
            );
        }
    }
    
    // =========================================================================
    // Pre-generated Lists
    // =========================================================================
    
    private static final List<QuadArrangement> QUAD_ARRANGEMENTS = new ArrayList<>();
    private static final List<SegmentArrangement> SEGMENT_ARRANGEMENTS = new ArrayList<>();
    private static final List<SectorArrangement> SECTOR_ARRANGEMENTS = new ArrayList<>();
    private static final List<EdgeArrangement> EDGE_ARRANGEMENTS = new ArrayList<>();
    
    static {
        generateQuadArrangements();
        generateSegmentArrangements();
        generateSectorArrangements();
        generateEdgeArrangements();
        
        Logging.RENDER.topic("shuffle").info(
            "Generated shuffle permutations: {} quads, {} segments, {} sectors, {} edges",
            QUAD_ARRANGEMENTS.size(), 
            SEGMENT_ARRANGEMENTS.size(),
            SECTOR_ARRANGEMENTS.size(),
            EDGE_ARRANGEMENTS.size()
        );
    }
    
    // =========================================================================
    // Quad Generation (All corner permutations)
    // =========================================================================
    
    private static void generateQuadArrangements() {
        Corner[] corners = Corner.values();
        int idx = 0;
        
        // Generate all 2-triangle combinations
        // Pick 3 corners for tri1, remaining logic for tri2
        for (int a = 0; a < 4; a++) {
            for (int b = 0; b < 4; b++) {
                if (b == a) continue;
                for (int c = 0; c < 4; c++) {
                    if (c == a || c == b) continue;
                    
                    Corner[] tri1 = {corners[a], corners[b], corners[c]};
                    
                    // For tri2, try different combinations
                    for (int d = 0; d < 4; d++) {
                        for (int e = 0; e < 4; e++) {
                            if (e == d) continue;
                            for (int f = 0; f < 4; f++) {
                                if (f == d || f == e) continue;
                                
                                Corner[] tri2 = {corners[d], corners[e], corners[f]};
                                QUAD_ARRANGEMENTS.add(new QuadArrangement(tri1, tri2, idx++, 0));
                            }
                        }
                    }
                }
            }
        }
        
        // Also add single-triangle variants
        for (int a = 0; a < 4; a++) {
            for (int b = 0; b < 4; b++) {
                if (b == a) continue;
                for (int c = 0; c < 4; c++) {
                    if (c == a || c == b) continue;
                    Corner[] tri1 = {corners[a], corners[b], corners[c]};
                    QUAD_ARRANGEMENTS.add(new QuadArrangement(tri1, null, idx++, 0));
                }
            }
        }
        
        // Update totals
        int total = QUAD_ARRANGEMENTS.size();
        for (int i = 0; i < total; i++) {
            QuadArrangement old = QUAD_ARRANGEMENTS.get(i);
            QUAD_ARRANGEMENTS.set(i, new QuadArrangement(old.tri1, old.tri2, i, total));
        }
    }
    
    // =========================================================================
    // Segment Generation (skip/phase/winding)
    // =========================================================================
    
    private static void generateSegmentArrangements() {
        int idx = 0;
        
        // Skip intervals 1-8
        for (int skip = 1; skip <= 8; skip++) {
            // Phase offsets 0 to skip-1
            for (int phase = 0; phase < skip; phase++) {
                // Both winding directions
                for (boolean reverse : new boolean[]{false, true}) {
                    SEGMENT_ARRANGEMENTS.add(new SegmentArrangement(skip, phase, reverse, idx++, 0));
                }
            }
        }
        
        // Update totals
        int total = SEGMENT_ARRANGEMENTS.size();
        for (int i = 0; i < total; i++) {
            SegmentArrangement old = SEGMENT_ARRANGEMENTS.get(i);
            SEGMENT_ARRANGEMENTS.set(i, new SegmentArrangement(
                old.skipInterval, old.phaseOffset, old.reverseWinding, i, total));
        }
    }
    
    // =========================================================================
    // Sector Generation (skip/phase/invert)
    // =========================================================================
    
    private static void generateSectorArrangements() {
        int idx = 0;
        
        // Skip intervals 1-8
        for (int skip = 1; skip <= 8; skip++) {
            // Phase offsets 0 to skip-1
            for (int phase = 0; phase < skip; phase++) {
                // Invert selection
                for (boolean invert : new boolean[]{false, true}) {
                    SECTOR_ARRANGEMENTS.add(new SectorArrangement(skip, phase, invert, idx++, 0));
                }
            }
        }
        
        // Update totals
        int total = SECTOR_ARRANGEMENTS.size();
        for (int i = 0; i < total; i++) {
            SectorArrangement old = SECTOR_ARRANGEMENTS.get(i);
            SECTOR_ARRANGEMENTS.set(i, new SectorArrangement(
                old.skipInterval, old.phaseOffset, old.invertSelection, i, total));
        }
    }
    
    // =========================================================================
    // Edge Generation (lat/lon/skip)
    // =========================================================================
    
    private static void generateEdgeArrangements() {
        int idx = 0;
        
        // All lat/lon combinations (except both false)
        for (boolean lat : new boolean[]{false, true}) {
            for (boolean lon : new boolean[]{false, true}) {
                if (!lat && !lon) continue; // Skip empty
                
                // Skip intervals 1-6
                for (int skip = 1; skip <= 6; skip++) {
                    EDGE_ARRANGEMENTS.add(new EdgeArrangement(lat, lon, skip, idx++, 0));
                }
            }
        }
        
        // Update totals
        int total = EDGE_ARRANGEMENTS.size();
        for (int i = 0; i < total; i++) {
            EdgeArrangement old = EDGE_ARRANGEMENTS.get(i);
            EDGE_ARRANGEMENTS.set(i, new EdgeArrangement(
                old.latitude, old.longitude, old.skipInterval, i, total));
        }
    }
    
    // =========================================================================
    // Public API
    // =========================================================================
    
    public static int quadCount() { return QUAD_ARRANGEMENTS.size(); }
    public static int segmentCount() { return SEGMENT_ARRANGEMENTS.size(); }
    public static int sectorCount() { return SECTOR_ARRANGEMENTS.size(); }
    public static int edgeCount() { return EDGE_ARRANGEMENTS.size(); }
    
    public static QuadArrangement getQuad(int index) {
        return QUAD_ARRANGEMENTS.get(Math.floorMod(index, QUAD_ARRANGEMENTS.size()));
    }
    
    public static SegmentArrangement getSegment(int index) {
        return SEGMENT_ARRANGEMENTS.get(Math.floorMod(index, SEGMENT_ARRANGEMENTS.size()));
    }
    
    public static SectorArrangement getSector(int index) {
        return SECTOR_ARRANGEMENTS.get(Math.floorMod(index, SECTOR_ARRANGEMENTS.size()));
    }
    
    public static EdgeArrangement getEdge(int index) {
        return EDGE_ARRANGEMENTS.get(Math.floorMod(index, EDGE_ARRANGEMENTS.size()));
    }
    
    /**
     * Logs a quad arrangement in detail.
     */
    public static void logQuad(int index) {
        QuadArrangement arr = getQuad(index);
        Logging.RENDER.topic("shuffle").info(
            "[Shuffle] Quad #{}/{}" + arr.toLogString(),
            arr.index + 1, arr.total
        );
    }
    
    /**
     * Logs a segment arrangement in detail.
     */
    public static void logSegment(int index) {
        SegmentArrangement arr = getSegment(index);
        Logging.RENDER.topic("shuffle").info(
            "[Shuffle] Segment #{}/{}" + arr.toLogString(),
            arr.index + 1, arr.total
        );
    }
    
    /**
     * Logs a sector arrangement in detail.
     */
    public static void logSector(int index) {
        SectorArrangement arr = getSector(index);
        Logging.RENDER.topic("shuffle").info(
            "[Shuffle] Sector #{}/{}" + arr.toLogString(),
            arr.index + 1, arr.total
        );
    }
    
    /**
     * Logs an edge arrangement in detail.
     */
    public static void logEdge(int index) {
        EdgeArrangement arr = getEdge(index);
        Logging.RENDER.topic("shuffle").info(
            "[Shuffle] Edge #{}/{}" + arr.toLogString(),
            arr.index + 1, arr.total
        );
    }
}

