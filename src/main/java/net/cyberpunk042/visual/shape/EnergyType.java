package net.cyberpunk042.visual.shape;

/**
 * Visual energy type for Kamehameha orb and beam components.
 * 
 * <p>Each type affects the visual rendering style:</p>
 * <ul>
 *   <li><b>CLASSIC</b> - Standard smooth energy (original Kamehameha)</li>
 *   <li><b>RASENGAN</b> - Spiraling/rotating sphere with visible energy streams</li>
 *   <li><b>GHOST</b> - Ethereal, semi-transparent, wispy edges</li>
 *   <li><b>FROST</b> - Crystalline ice patterns, sharp geometric facets</li>
 *   <li><b>LIGHTNING</b> - Electric crackling, branching energy arcs</li>
 *   <li><b>WATER</b> - Flowing liquid appearance, wave distortion</li>
 *   <li><b>FIRE</b> - Flame/plasma, flickering hot appearance</li>
 *   <li><b>UNSTABLE</b> - Erratic flickering, dangerous overload look</li>
 *   <li><b>SPIKED</b> - Sharp protrusions radiating outward</li>
 *   <li><b>VOID</b> - Dark energy, absorbing rather than emitting</li>
 *   <li><b>HOLY</b> - Divine light, bright rays emanating</li>
 * </ul>
 * 
 * @see KamehamehaShape
 */
public enum EnergyType {
    /** Standard smooth energy beam (classic Kamehameha blue). */
    CLASSIC("Classic", "Smooth flowing energy"),
    
    /** Spiraling compressed energy (Naruto-style). */
    RASENGAN("Rasengan", "Spiraling rotating energy sphere"),
    
    /** Ethereal semi-transparent energy. */
    GHOST("Ghost", "Ethereal wispy translucent energy"),
    
    /** Crystalline ice-based energy. */
    FROST("Frost", "Crystalline frozen energy patterns"),
    
    /** Electric crackling energy with arcs. */
    LIGHTNING("Lightning", "Electric sparking energy arcs"),
    
    /** Flowing liquid-like energy. */
    WATER("Water", "Flowing liquid wave energy"),
    
    /** Flame/plasma burning energy. */
    FIRE("Fire", "Burning plasma flame energy"),
    
    /** Erratic unstable overloaded energy. */
    UNSTABLE("Unstable", "Flickering dangerous overload"),
    
    /** Sharp spiky protrusions. */
    SPIKED("Spiked", "Sharp radiating spikes"),
    
    /** Dark absorbing void energy. */
    VOID("Void", "Dark matter absorbing energy"),
    
    /** Divine radiant light. */
    HOLY("Holy", "Divine radiating light beams");
    
    private final String displayName;
    private final String description;
    
    EnergyType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String displayName() { return displayName; }
    public String description() { return description; }
    
    /**
     * Whether this type has visible rotation/spin.
     */
    public boolean hasRotation() {
        return this == RASENGAN || this == WATER || this == UNSTABLE;
    }
    
    /**
     * Whether this type has surface distortion/noise.
     */
    public boolean hasDistortion() {
        return this == GHOST || this == WATER || this == FIRE || this == UNSTABLE;
    }
    
    /**
     * Whether this type has spiky/jagged edges.
     */
    public boolean hasSpikes() {
        return this == FROST || this == LIGHTNING || this == SPIKED;
    }
    
    /**
     * Whether this type emits secondary particles/arcs.
     */
    public boolean hasSecondaryEffects() {
        return this == LIGHTNING || this == FIRE || this == UNSTABLE || this == HOLY;
    }
    
    /**
     * Base alpha multiplier for this type.
     */
    public float baseAlpha() {
        return switch (this) {
            case GHOST -> 0.5f;
            case VOID -> 0.8f;
            default -> 1.0f;
        };
    }
    
    /**
     * Suggested default color hue for this type (0-360).
     */
    public float suggestedHue() {
        return switch (this) {
            case CLASSIC -> 200f;    // Cyan/blue
            case RASENGAN -> 210f;   // Light blue
            case GHOST -> 270f;      // Purple
            case FROST -> 180f;      // Cyan
            case LIGHTNING -> 60f;   // Yellow
            case WATER -> 220f;      // Deep blue
            case FIRE -> 20f;        // Orange/red
            case UNSTABLE -> 0f;     // Red
            case SPIKED -> 300f;     // Magenta
            case VOID -> 280f;       // Dark purple
            case HOLY -> 45f;        // Gold
        };
    }
}
