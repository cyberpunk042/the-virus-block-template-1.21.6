package net.cyberpunk042.field.force.phase;

/**
 * Force polarity - direction of force application.
 * 
 * <ul>
 *   <li><b>PULL</b>: Force toward center (gravity, suction)</li>
 *   <li><b>PUSH</b>: Force away from center (explosion, repulsion)</li>
 *   <li><b>HOLD</b>: No radial force (freeze in place)</li>
 * </ul>
 */
public enum ForcePolarity {
    
    /** Force toward center. */
    PULL("pull", 1.0f),
    
    /** Force away from center. */
    PUSH("push", -1.0f),
    
    /** No radial force (entities maintain current motion). */
    HOLD("hold", 0.0f);
    
    private final String id;
    private final float directionMultiplier;
    
    ForcePolarity(String id, float multiplier) {
        this.id = id;
        this.directionMultiplier = multiplier;
    }
    
    /**
     * Returns the string identifier for JSON serialization.
     */
    public String id() {
        return id;
    }
    
    /**
     * Returns the direction multiplier.
     * <ul>
     *   <li>+1.0 = toward center (PULL)</li>
     *   <li>-1.0 = away from center (PUSH)</li>
     *   <li> 0.0 = no radial force (HOLD)</li>
     * </ul>
     */
    public float directionMultiplier() {
        return directionMultiplier;
    }
    
    /**
     * Parses from string (case-insensitive).
     * 
     * @param id String identifier
     * @return Matching polarity, or PULL as default
     */
    public static ForcePolarity fromId(String id) {
        if (id == null || id.isEmpty()) {
            return PULL;
        }
        
        return switch (id.toLowerCase()) {
            case "pull", "attract", "in" -> PULL;
            case "push", "repel", "out" -> PUSH;
            case "hold", "none", "neutral" -> HOLD;
            default -> PULL;
        };
    }
}
