package net.cyberpunk042.visual.transform;

import org.joml.Vector3f;

/**
 * Defines anchor positions relative to the player or field origin.
 * 
 * <p>Anchors determine where a primitive is positioned before any
 * offset is applied. The offset is ADDITIONAL to the anchor position.</p>
 * 
 * <h3>Example</h3>
 * <pre>
 * anchor: FEET + offset: {y: 0.5} = 0.5 blocks above feet
 * </pre>
 * 
 * @see Transform
 */
public enum Anchor {
    /** Player chest/center height (0, 1, 0) - DEFAULT */
    CENTER("Center", 0, 1, 0),
    
    /** Player feet level (0, 0, 0) */
    FEET("Feet", 0, 0, 0),
    
    /** Player head level (0, 2, 0) */
    HEAD("Head", 0, 2, 0),
    
    /** Above player head (0, 3, 0) */
    ABOVE("Above Head", 0, 3, 0),
    
    /** Below feet / underground (0, -1, 0) */
    BELOW("Below Feet", 0, -1, 0),
    
    /** In front of player (0, 1, 1) */
    FRONT("Front", 0, 1, 1),
    
    /** Behind player (0, 1, -1) */
    BACK("Back", 0, 1, -1),
    
    /** Left side of player (-1, 1, 0) */
    LEFT("Left", -1, 1, 0),
    
    /** Right side of player (1, 1, 0) */
    RIGHT("Right", 1, 1, 0);
    
    private final String label;
    private final float x, y, z;
    
    Anchor(String label, float x, float y, float z) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    /** Display label for GUI */
    public String label() {
        return label;
    }
    
    @Override
    public String toString() {
        return label;
    }
    
    /**
     * Returns the offset vector for this anchor.
     * @return A new Vector3f with the anchor's offset
     */
    public Vector3f getOffset() {
        return new Vector3f(x, y, z);
    }
    
    /**
     * Returns the X component of this anchor's offset.
     */
    public float getX() { return x; }
    
    /**
     * Returns the Y component of this anchor's offset.
     */
    public float getY() { return y; }
    
    /**
     * Returns the Z component of this anchor's offset.
     */
    public float getZ() { return z; }

    
    /**
     * Parse from string (case-insensitive).
     * @param id The string identifier
     * @return Matching Anchor, or CENTER if not found
     */
    public static Anchor fromId(String id) {
        if (id == null || id.isEmpty()) return CENTER;
        try {
            return valueOf(id.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return CENTER;
        }
    }
}
