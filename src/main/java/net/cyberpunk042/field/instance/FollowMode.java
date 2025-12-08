package net.cyberpunk042.field.instance;

/**
 * Defines how a personal field follows its owner's position.
 * 
 * <h2>Modes</h2>
 * <ul>
 *   <li>{@link #SNAP} - Instant teleport to player (no interpolation)</li>
 *   <li>{@link #SMOOTH} - Smooth interpolation with slight lag</li>
 *   <li>{@link #GLIDE} - Very slow, floaty movement</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // In commands:
 * FollowMode mode = FollowMode.fromId("glide");
 * 
 * // In rendering:
 * Vec3d newPos = currentPos.lerp(targetPos, mode.lerpFactor());
 * </pre>
 */
public enum FollowMode {
    
    /**
     * Field snaps directly to player position each tick.
     * No smoothing - instant teleport.
     */
    SNAP("snap", 1.0f, "Instant teleport to player"),
    
    /**
     * Field smoothly interpolates toward player position.
     * Reduces jitter but has slight visual lag.
     */
    SMOOTH("smooth", 0.35f, "Smooth interpolation"),
    
    /**
     * Field very slowly glides toward player position.
     * Creates a floaty, ethereal effect.
     */
    GLIDE("glide", 0.2f, "Slow, floating movement");
    
    private final String id;
    private final float lerpFactor;
    private final String description;
    
    FollowMode(String id, float lerpFactor, String description) {
        this.id = id;
        this.lerpFactor = lerpFactor;
        this.description = description;
    }
    
    /**
     * String identifier for JSON/commands.
     */
    public String id() {
        return id;
    }
    
    /**
     * Interpolation factor (0-1) for lerp operations.
     * Higher = faster following.
     */
    public float lerpFactor() {
        return lerpFactor;
    }
    
    /**
     * Human-readable description for help text.
     */
    public String description() {
        return description;
    }
    
    /**
     * Interpolate from current position toward target.
     * @param current Current position
     * @param target Target position
     * @return Interpolated position
     */
    public net.minecraft.util.math.Vec3d interpolate(
            net.minecraft.util.math.Vec3d current,
            net.minecraft.util.math.Vec3d target) {
        if (this == SNAP) {
            return target;
        }
        return current.lerp(target, lerpFactor);
    }
    
    /**
     * Parse from string ID (case-insensitive).
     * @param id Mode identifier
     * @return Matching mode, or SMOOTH as default
     */
    public static FollowMode fromId(String id) {
        if (id == null || id.isEmpty()) {
            return SMOOTH;
        }
        for (FollowMode mode : values()) {
            if (mode.id.equalsIgnoreCase(id)) {
                return mode;
            }
        }
        return SMOOTH;
    }
}
