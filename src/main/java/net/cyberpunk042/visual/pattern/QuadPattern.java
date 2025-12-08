package net.cyberpunk042.visual.pattern;

import java.util.Arrays;
import java.util.List;

/**
 * Patterns for quad-based tessellation (spheres, prism sides).
 * 
 * <p>Controls how each quad cell is divided into triangles.
 * The corners are: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
 * 
 * <h2>Available Patterns (16 user-curated from shuffle exploration)</h2>
 * <ul>
 *   <li><b>FILLED_1</b>: Standard filled quad (default)</li>
 *   <li><b>TRIANGLE_1-4</b>: Various triangle arrangements</li>
 *   <li><b>TRIANGLE_MESHED_1-2</b>: Meshed triangle patterns</li>
 *   <li><b>PARALLELOGRAM_1-2</b>: Parallelogram shapes</li>
 *   <li><b>STRIPE_1</b>: Stripe effect</li>
 *   <li><b>WAVE_1</b>: Wave pattern</li>
 *   <li><b>TOOTH_1</b>: Tooth/sawtooth pattern</li>
 * </ul>
 * 
 * @see VertexPattern
 */
public enum QuadPattern implements VertexPattern {
    
    // =========================================================================
    // User-Curated Patterns (from shuffle exploration)
    // These are the 16 "magic" patterns discovered during development
    // =========================================================================
    
    /** Filled pattern variant 1 (#37) - Standard filled quad. */
    FILLED_1("filled_1",
        new Corner[]{Corner.TOP_LEFT, Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT},
        new Corner[]{Corner.BOTTOM_LEFT, Corner.TOP_LEFT, Corner.TOP_RIGHT}
    ),
    
    /** Triangle pattern 1 (#43). */
    TRIANGLE_1("triangle_1",
        new Corner[]{Corner.TOP_LEFT, Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT},
        new Corner[]{Corner.BOTTOM_RIGHT, Corner.TOP_LEFT, Corner.TOP_RIGHT}
    ),
    
    /** Triangle pattern 2 (#53). */
    TRIANGLE_2("triangle_2",
        new Corner[]{Corner.TOP_LEFT, Corner.BOTTOM_LEFT, Corner.TOP_RIGHT},
        new Corner[]{Corner.TOP_LEFT, Corner.BOTTOM_RIGHT, Corner.TOP_RIGHT}
    ),
    
    /** Tooth pattern 1 (#54). */
    TOOTH_1("tooth_1",
        new Corner[]{Corner.TOP_LEFT, Corner.BOTTOM_LEFT, Corner.TOP_RIGHT},
        new Corner[]{Corner.TOP_LEFT, Corner.BOTTOM_RIGHT, Corner.BOTTOM_LEFT}
    ),
    
    /** Triangle meshed pattern 1 (#62). */
    TRIANGLE_MESHED_1("triangle_meshed_1",
        new Corner[]{Corner.TOP_LEFT, Corner.BOTTOM_LEFT, Corner.TOP_RIGHT},
        new Corner[]{Corner.BOTTOM_LEFT, Corner.TOP_LEFT, Corner.BOTTOM_RIGHT}
    ),
    
    /** Triangle slim pattern 1 (#63). */
    TRIANGLE_SLIM_1("triangle_slim_1",
        new Corner[]{Corner.TOP_LEFT, Corner.BOTTOM_LEFT, Corner.TOP_RIGHT},
        new Corner[]{Corner.BOTTOM_LEFT, Corner.TOP_RIGHT, Corner.TOP_LEFT}
    ),
    
    /** Parallelogram pattern 1 (#66). */
    PARALLELOGRAM_1("parallelogram_1",
        new Corner[]{Corner.TOP_LEFT, Corner.BOTTOM_LEFT, Corner.TOP_RIGHT},
        new Corner[]{Corner.BOTTOM_LEFT, Corner.BOTTOM_RIGHT, Corner.TOP_RIGHT}
    ),
    
    /** Three quarter slim pattern 1 (#69). */
    THREE_QUARTER_SLIM_1("three_quarter_slim_1",
        new Corner[]{Corner.TOP_LEFT, Corner.BOTTOM_LEFT, Corner.TOP_RIGHT},
        new Corner[]{Corner.BOTTOM_RIGHT, Corner.TOP_RIGHT, Corner.TOP_LEFT}
    ),
    
    /** Filled with triangle pattern 1 (#71). */
    FILLED_WITH_TRIANGLE_1("filled_with_triangle_1",
        new Corner[]{Corner.TOP_LEFT, Corner.BOTTOM_LEFT, Corner.TOP_RIGHT},
        new Corner[]{Corner.BOTTOM_RIGHT, Corner.BOTTOM_LEFT, Corner.TOP_LEFT}
    ),
    
    /** Parallelogram pattern 2 (#74). */
    PARALLELOGRAM_2("parallelogram_2",
        new Corner[]{Corner.TOP_LEFT, Corner.BOTTOM_LEFT, Corner.BOTTOM_RIGHT},
        new Corner[]{Corner.TOP_LEFT, Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT}
    ),
    
    /** Triangle meshed pattern 2 (#81). */
    TRIANGLE_MESHED_2("triangle_meshed_2",
        new Corner[]{Corner.TOP_LEFT, Corner.BOTTOM_LEFT, Corner.BOTTOM_RIGHT},
        new Corner[]{Corner.TOP_RIGHT, Corner.BOTTOM_LEFT, Corner.TOP_LEFT}
    ),
    
    /** Stripe pattern 1 (#83). */
    STRIPE_1("stripe_1",
        new Corner[]{Corner.TOP_LEFT, Corner.BOTTOM_LEFT, Corner.BOTTOM_RIGHT},
        new Corner[]{Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT, Corner.TOP_LEFT}
    ),
    
    /** Square overlap pattern 1 (#99). */
    SQUARE_OVERLAP_1("square_overlap_1",
        new Corner[]{Corner.TOP_LEFT, Corner.BOTTOM_RIGHT, Corner.TOP_RIGHT},
        new Corner[]{Corner.TOP_LEFT, Corner.BOTTOM_LEFT, Corner.TOP_RIGHT}
    ),
    
    /** Wave pattern 1 (#114). */
    WAVE_1("wave_1",
        new Corner[]{Corner.TOP_LEFT, Corner.BOTTOM_RIGHT, Corner.TOP_RIGHT},
        new Corner[]{Corner.BOTTOM_LEFT, Corner.BOTTOM_RIGHT, Corner.TOP_RIGHT}
    ),
    
    /** Triangle pattern 3 (#147). */
    TRIANGLE_3("triangle_3",
        new Corner[]{Corner.TOP_RIGHT, Corner.TOP_LEFT, Corner.BOTTOM_LEFT},
        new Corner[]{Corner.TOP_LEFT, Corner.BOTTOM_LEFT, Corner.TOP_RIGHT}
    ),
    
    /** Triangle pattern 4 (#156). */
    TRIANGLE_4("triangle_4",
        new Corner[]{Corner.TOP_RIGHT, Corner.TOP_LEFT, Corner.BOTTOM_LEFT},
        new Corner[]{Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT, Corner.BOTTOM_LEFT}
    );
    
    // =========================================================================
    // Default alias
    // =========================================================================
    
    /** Default pattern (alias for FILLED_1). */
    public static final QuadPattern DEFAULT = FILLED_1;
    
    // =========================================================================
    // Fields and Constructor
    // =========================================================================
    
    private final String id;
    private final List<Corner[]> triangles;
    
    QuadPattern(String id, Corner[]... triangles) {
        this.id = id;
        this.triangles = Arrays.asList(triangles);
    }
    
    @Override
    public String id() {
        return id;
    }
    
    @Override
    public PatternGeometry geometry() {
        return PatternGeometry.QUAD;
    }
    
    /**
     * Gets the triangles that make up this pattern.
     */
    public List<Corner[]> triangles() {
        return triangles;
    }
    
    /**
     * Number of triangles in this pattern.
     */
    public int triangleCount() {
        return triangles.size();
    }
    
    /**
     * Parses a pattern from ID string.
     * @return The pattern, or null if not found
     */
    public static QuadPattern fromId(String id) {
        if (id == null) return null;
        String lower = id.toLowerCase().trim();
        for (QuadPattern p : values()) {
            if (p.id.equals(lower)) return p;
        }
        return null;
    }
    
    /**
     * Lists all pattern IDs.
     */
    public static String[] ids() {
        QuadPattern[] values = values();
        String[] ids = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            ids[i] = values[i].id;
        }
        return ids;
    }
    
    /**
     * Corner positions within a quad cell.
     */
    public enum Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }
}
