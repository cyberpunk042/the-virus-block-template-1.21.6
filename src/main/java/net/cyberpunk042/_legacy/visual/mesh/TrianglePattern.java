package net.cyberpunk042.visual.mesh;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Curated triangle patterns for mesh rendering.
 * 
 * <p>Each pattern defines how a quad cell is divided into triangles.
 * The corners are: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
 * 
 * <h2>Patterns</h2>
 * <ul>
 *   <li><b>DEFAULT</b>: Standard quad (2 triangles)</li>
 *   <li><b>MESHED</b>: Mesh-style pattern (creates grid look)</li>
 *   <li><b>FACING</b>: Both triangles face same direction</li>
 *   <li><b>DIAGONAL</b>: Diagonal split pattern</li>
 *   <li><b>SINGLE</b>: Only one triangle (sparse)</li>
 *   <li><b>ARROW</b>: Arrow-like pattern</li>
 *   <li><b>PARALLELOGRAM</b>: Parallelogram shape</li>
 * </ul>
 */
public enum TrianglePattern {
    
    /**
     * Standard two-triangle quad fill.
     * BL-TL-TR and BL-TR-BR
     */
    DEFAULT(
        new Corner[]{Corner.BOTTOM_LEFT, Corner.TOP_LEFT, Corner.TOP_RIGHT},
        new Corner[]{Corner.BOTTOM_LEFT, Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT}
    ),
    
    /**
     * Mesh pattern - creates a grid appearance.
     * TR-BL-TL and BR-TR-BL
     */
    MESHED(
        new Corner[]{Corner.TOP_RIGHT, Corner.BOTTOM_LEFT, Corner.TOP_LEFT},
        new Corner[]{Corner.BOTTOM_RIGHT, Corner.TOP_RIGHT, Corner.BOTTOM_LEFT}
    ),
    
    /**
     * Both triangles face the same direction.
     * BL-TR-TL and BL-TR-BR
     */
    FACING(
        new Corner[]{Corner.BOTTOM_LEFT, Corner.TOP_RIGHT, Corner.TOP_LEFT},
        new Corner[]{Corner.BOTTOM_LEFT, Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT}
    ),
    
    /**
     * Diagonal split from top-left to bottom-right.
     * TL-BR-BL and TL-TR-BR
     */
    DIAGONAL(
        new Corner[]{Corner.TOP_LEFT, Corner.BOTTOM_RIGHT, Corner.BOTTOM_LEFT},
        new Corner[]{Corner.TOP_LEFT, Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT}
    ),
    
    /**
     * Only one triangle - creates sparse effect.
     * TL-TR-BL only
     */
    SINGLE(
        new Corner[]{Corner.TOP_LEFT, Corner.TOP_RIGHT, Corner.BOTTOM_LEFT}
    ),
    
    /**
     * Arrow pointing down-right.
     * TL-TR-BR only (half quad)
     */
    ARROW(
        new Corner[]{Corner.TOP_LEFT, Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT}
    ),
    
    /**
     * Parallelogram shape.
     * TL-TR-BL and TR-BR-BL
     */
    PARALLELOGRAM(
        new Corner[]{Corner.TOP_LEFT, Corner.TOP_RIGHT, Corner.BOTTOM_LEFT},
        new Corner[]{Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT, Corner.BOTTOM_LEFT}
    ),
    
    /**
     * Spaced pattern - creates gaps.
     * BL-TL-TR only (sparse coverage)
     */
    SPACED(
        new Corner[]{Corner.BOTTOM_LEFT, Corner.TOP_LEFT, Corner.TOP_RIGHT}
    ),
    
    /**
     * Hole pattern - inverted triangle.
     * Creates a hole effect when combined with other layers.
     */
    HOLE(
        new Corner[]{Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT, Corner.BOTTOM_LEFT}
    );
    
    private final List<Corner[]> triangles;
    
    TrianglePattern(Corner[]... triangles) {
        this.triangles = Arrays.asList(triangles);
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
     * Parses a pattern from string (case-insensitive).
     */
    public static TrianglePattern fromString(String value) {
        if (value == null || value.isEmpty()) {
            return DEFAULT;
        }
        try {
            return TrianglePattern.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
    }
    
    /**
     * Gets pattern by name, or null if not found.
     */
    public static TrianglePattern getOrNull(String name) {
        try {
            return TrianglePattern.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Lists all pattern names.
     */
    public static String[] names() {
        TrianglePattern[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].name().toLowerCase(Locale.ROOT);
        }
        return names;
    }
    
    /**
     * Corner positions within a quad cell.
     */
    public enum Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT;
        
        public static Corner fromString(String value) {
            if (value == null || value.isEmpty()) {
                return TOP_LEFT;
            }
            try {
                return Corner.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return TOP_LEFT;
            }
        }
    }
}
