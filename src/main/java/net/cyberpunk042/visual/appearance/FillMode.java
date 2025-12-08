package net.cyberpunk042.visual.appearance;

/**
 * Rendering fill modes for primitives.
 * 
 * <h2>Modes</h2>
 * <ul>
 *   <li>{@link #SOLID} - Filled triangles (default)</li>
 *   <li>{@link #WIREFRAME} - Edge lines only</li>
 *   <li>{@link #POINTS} - Vertex points only</li>
 *   <li>{@link #TRANSLUCENT} - Solid with alpha blending</li>
 * </ul>
 */
public enum FillMode {
    /**
     * Filled solid triangles.
     */
    SOLID("solid"),
    
    /**
     * Wireframe edges only.
     */
    WIREFRAME("wireframe"),
    
    /**
     * Vertex points only.
     */
    POINTS("points"),
    
    /**
     * Translucent (solid with alpha).
     */
    TRANSLUCENT("translucent");
    
    private final String id;
    
    FillMode(String id) {
        this.id = id;
    }
    
    public String id() {
        return id;
    }
    
    /**
     * Parses a fill mode from string.
     * @return FillMode or SOLID if not recognized
     */
    public static FillMode fromId(String id) {
        if (id == null) return SOLID;
        String lower = id.toLowerCase();
        for (FillMode mode : values()) {
            if (mode.id.equals(lower)) {
                return mode;
            }
        }
        return SOLID;
    }
    
    /**
     * Whether this mode uses filled triangles.
     */
    public boolean isFilled() {
        return this == SOLID || this == TRANSLUCENT;
    }
    
    /**
     * Whether this mode uses alpha blending.
     */
    public boolean isTranslucent() {
        return this == TRANSLUCENT;
    }
    
    /**
     * Whether this mode renders edges.
     */
    public boolean isWireframe() {
        return this == WIREFRAME;
    }
}
