package net.cyberpunk042.visual.transform;

/**
 * Defines billboard behavior for primitives.
 * 
 * <p>Billboarding makes a primitive always face the camera,
 * which is useful for flat effects that should always be visible.</p>
 * 
 * @see Transform
 * @see Facing
 */
public enum Billboard {
    /** No billboarding - primitive maintains its orientation (DEFAULT) */
    NONE,
    
    /** Fully faces the camera on all axes */
    FULL,
    
    /** Rotates around Y-axis only to face camera (like a tree in old games) */
    Y_AXIS;
    
    /**
     * Parse from string (case-insensitive).
     * @param id The string identifier
     * @return Matching Billboard, or NONE if not found
     */
    public static Billboard fromId(String id) {
        if (id == null || id.isEmpty()) return NONE;
        try {
            return valueOf(id.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
