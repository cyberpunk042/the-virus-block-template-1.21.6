package net.cyberpunk042.visual.transform;

/**
 * Defines how a primitive orients itself in the world.
 * 
 * <p>Facing determines the rotation of a primitive based on
 * player state, camera position, or fixed world orientation.</p>
 * 
 * @see Transform
 * @see Billboard
 */
public enum Facing {
    /** Stays in world orientation - no rotation adjustment (DEFAULT) */
    FIXED,
    
    /** Rotates to match the player's look direction */
    PLAYER_LOOK,
    
    /** Points in the player's movement direction */
    VELOCITY,
    
    /** Always faces the camera (useful for 2D sprites in 3D space) */
    CAMERA;
    
    /**
     * Parse from string (case-insensitive).
     * @param id The string identifier
     * @return Matching Facing, or FIXED if not found
     */
    public static Facing fromId(String id) {
        if (id == null || id.isEmpty()) return FIXED;
        try {
            return valueOf(id.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return FIXED;
        }
    }
}
