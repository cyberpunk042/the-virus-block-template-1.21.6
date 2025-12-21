package net.cyberpunk042.field.force;

/**
 * Defines the primary behavior mode for a force field.
 * 
 * <p>Each mode represents a distinct physical behavior pattern with its own
 * set of configurable parameters. The GUI dynamically shows controls
 * relevant to the selected mode.
 * 
 * <h2>Mode Categories</h2>
 * <ul>
 *   <li><b>Linear:</b> PULL, PUSH - Simple radial forces</li>
 *   <li><b>Rotational:</b> VORTEX, ORBIT, TORNADO - Forces with angular component</li>
 *   <li><b>Structured:</b> RING - Stable orbit bands</li>
 *   <li><b>Dramatic:</b> IMPLOSION, EXPLOSION - High-intensity events</li>
 *   <li><b>Manual:</b> CUSTOM - Full zone-by-zone control</li>
 * </ul>
 */
public enum ForceMode {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Linear Modes - Simple radial forces
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Standard zone-based radial forces (default).
     * Uses zones to define strength at different distances.
     */
    RADIAL("Radial", "Zone-based radial forces (default)", "linear"),
    
    /**
     * Simple attraction toward center.
     * Entities are pulled inward with configurable falloff.
     */
    PULL("Pull", "Simple attraction toward center", "linear"),
    
    /**
     * Simple repulsion from center.
     * Entities are pushed outward, optionally with vertical boost.
     */
    PUSH("Push", "Simple repulsion from center", "linear"),
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Rotational Modes - Forces with angular component
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Spiral inward motion.
     * Combines radial pull with tangential rotation, creating a spiral path.
     */
    VORTEX("Vortex", "Spiral inward with rotation", "rotational"),
    
    /**
     * Stable circular motion.
     * Entities orbit at fixed radii without being pulled in or pushed out.
     */
    ORBIT("Orbit", "Stable circular motion at fixed radii", "rotational"),
    
    /**
     * Lifting spiral motion.
     * Combines vertical lift with horizontal rotation, like a tornado.
     */
    TORNADO("Tornado", "Vertical lift with horizontal spin", "rotational"),
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Structured Modes - Defined force regions
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Orbit band with inner repulsion.
     * Creates a stable "orbit lane" - pushed out if too close, pulled in if too far.
     */
    RING("Ring", "Stable orbit band with inner repulsion", "structured"),
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Dramatic Modes - High-intensity events
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Intense collapse toward center.
     * Strong pull that accelerates as entities approach the core.
     */
    IMPLOSION("Implosion", "Intense collapse toward center", "dramatic"),
    
    /**
     * Explosive outward blast.
     * Powerful radial push with optional vertical boost.
     */
    EXPLOSION("Explosion", "Explosive outward blast", "dramatic"),
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Manual Mode - Full control
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Manual zone-by-zone configuration.
     * Full control over radial, tangential, and lift forces per zone.
     */
    CUSTOM("Custom", "Manual zone-by-zone configuration", "manual"),
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Path Mode - Entities follow curved paths
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Path-following mode.
     * Entities are attracted to a curved path (circle, infinity, helix, etc.)
     * and flow along it with CAPTURE→FOLLOW→RELEASE phases.
     */
    PATH("Path", "Follow curved path with capture/flow/release", "path");
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Fields
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final String displayName;
    private final String description;
    private final String category;
    
    ForceMode(String displayName, String description, String category) {
        this.displayName = displayName;
        this.description = description;
        this.category = category;
    }
    
    /**
     * Human-readable name for GUI display.
     */
    public String displayName() {
        return displayName;
    }
    
    /**
     * Short description of the mode's behavior.
     */
    public String description() {
        return description;
    }
    
    /**
     * Category for grouping in UI (linear, rotational, structured, dramatic, manual).
     */
    public String category() {
        return category;
    }
    
    /**
     * Whether this mode involves rotational/tangential forces.
     */
    public boolean isRotational() {
        return "rotational".equals(category);
    }
    
    /**
     * Whether this mode is a high-intensity event type.
     */
    public boolean isDramatic() {
        return "dramatic".equals(category);
    }
    
    /**
     * Whether this mode uses manual zone configuration.
     */
    public boolean isCustom() {
        return this == CUSTOM;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Parsing
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Parses a mode from string ID (case-insensitive).
     * Returns RADIAL if not recognized (for backward compatibility).
     */
    public static ForceMode fromId(String id) {
        if (id == null || id.isBlank()) {
            return RADIAL;
        }
        for (ForceMode mode : values()) {
            if (mode.name().equalsIgnoreCase(id)) {
                return mode;
            }
        }
        return RADIAL;
    }
    
    /**
     * Returns the string ID for serialization.
     */
    public String id() {
        return name().toLowerCase();
    }
}
