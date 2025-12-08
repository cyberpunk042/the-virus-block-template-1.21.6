package net.cyberpunk042.client.visual._legacy.render;

/**
 * Visual styles for mesh rendering.
 */
public enum MeshStyle_old {
    /**
     * Solid filled triangles/quads.
     */
    SOLID,
    
    /**
     * Wireframe lines only.
     */
    WIREFRAME,
    
    /**
     * Solid with wireframe overlay.
     */
    SOLID_WIREFRAME,
    
    /**
     * Points at vertices only.
     */
    POINTS,
    
    /**
     * Translucent solid (for glow effects).
     */
    TRANSLUCENT;
    
    /**
     * Whether this style renders solid faces.
     */
    public boolean hasSolid() {
        return this == SOLID || this == SOLID_WIREFRAME || this == TRANSLUCENT;
    }
    
    /**
     * Whether this style renders wireframe lines.
     */
    public boolean hasWireframe() {
        return this == WIREFRAME || this == SOLID_WIREFRAME;
    }
    
    /**
     * Whether this style uses translucent blending.
     */
    public boolean isTranslucent() {
        return this == TRANSLUCENT;
    }
    
    /**
     * Parse from string (case-insensitive).
     */
    public static MeshStyle_old fromString(String name) {
        if (name == null || name.isEmpty()) {
            return SOLID;
        }
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SOLID;
        }
    }
}
