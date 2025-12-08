package net.cyberpunk042.client.gui.screen;

/**
 * Tab types for the Field Customizer GUI.
 */
public enum TabType {
    /** Level 1: Basic controls for all players */
    QUICK("Quick", "Basic field customization"),
    
    /** Level 2: Advanced controls for detailed editing */
    ADVANCED("Advanced", "Detailed parameters"),
    
    /** Level 3: Debug controls (operator only) */
    DEBUG("Debug", "Internal field controls"),
    
    /** Profile management */
    PROFILES("Profiles", "Save and load profiles");
    
    private final String label;
    private final String tooltip;
    
    TabType(String label, String tooltip) {
        this.label = label;
        this.tooltip = tooltip;
    }
    
    public String label() { return label; }
    public String tooltip() { return tooltip; }
}
