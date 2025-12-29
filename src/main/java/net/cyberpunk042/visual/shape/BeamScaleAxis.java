package net.cyberpunk042.visual.shape;

/**
 * Scale axis mode for Kamehameha beam transitions.
 * 
 * <p>Controls which dimension(s) of the beam are affected during scale transitions:</p>
 * <ul>
 *   <li><b>LENGTH</b> - Beam extends/retracts along its axis (like a lightsaber ignition)</li>
 *   <li><b>WIDTH</b> - Beam expands/contracts radially (like inflating/deflating)</li>
 *   <li><b>BOTH</b> - Both length and width scale together (uniform growth)</li>
 *   <li><b>LENGTH_THEN_WIDTH</b> - First extends, then expands (staged)</li>
 *   <li><b>WIDTH_THEN_LENGTH</b> - First expands radius, then extends</li>
 * </ul>
 * 
 * @see KamehamehaShape
 * @see TransitionStyle
 */
public enum BeamScaleAxis {
    /** Only beam length changes (extends/retracts along axis). */
    LENGTH("Length", "Beam extends/retracts along its axis"),
    
    /** Only beam width/radius changes (expands/contracts). */
    WIDTH("Width", "Beam expands/contracts radially"),
    
    /** Both length and width scale uniformly. */
    BOTH("Both", "Uniform length and width scaling"),
    
    /** Length scales first (0-0.5), then width (0.5-1). */
    LENGTH_THEN_WIDTH("Length→Width", "Staged: extend then expand"),
    
    /** Width scales first (0-0.5), then length (0.5-1). */
    WIDTH_THEN_LENGTH("Width→Length", "Staged: expand then extend");
    
    private final String displayName;
    private final String description;
    
    BeamScaleAxis(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String displayName() { return displayName; }
    public String description() { return description; }
    
    /**
     * Whether this mode affects beam length.
     */
    public boolean affectsLength() {
        return this != WIDTH;
    }
    
    /**
     * Whether this mode affects beam width.
     */
    public boolean affectsWidth() {
        return this != LENGTH;
    }
    
    /**
     * Calculate effective length scale for given progress (0-1).
     * @param progress Animation progress 0-1
     * @return Length scale multiplier 0-1
     */
    public float lengthScale(float progress) {
        return switch (this) {
            case LENGTH, BOTH -> progress;
            case WIDTH -> 1.0f;  // Length stays full
            case LENGTH_THEN_WIDTH -> Math.min(1.0f, progress * 2.0f);  // 0-0.5 → 0-1
            case WIDTH_THEN_LENGTH -> progress <= 0.5f ? 0.0f : (progress - 0.5f) * 2.0f;  // 0.5-1 → 0-1
        };
    }
    
    /**
     * Calculate effective width scale for given progress (0-1).
     * @param progress Animation progress 0-1
     * @return Width scale multiplier 0-1
     */
    public float widthScale(float progress) {
        return switch (this) {
            case WIDTH, BOTH -> progress;
            case LENGTH -> 1.0f;  // Width stays full
            case WIDTH_THEN_LENGTH -> Math.min(1.0f, progress * 2.0f);  // 0-0.5 → 0-1
            case LENGTH_THEN_WIDTH -> progress <= 0.5f ? 0.0f : (progress - 0.5f) * 2.0f;  // 0.5-1 → 0-1
        };
    }
}
