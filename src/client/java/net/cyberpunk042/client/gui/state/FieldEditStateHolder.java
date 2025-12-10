package net.cyberpunk042.client.gui.state;

import net.cyberpunk042.log.Logging;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side singleton holder for FieldEditState.
 * 
 * <p>This allows:</p>
 * <ul>
 *   <li>Commands to update state even when GUI is closed</li>
 *   <li>Test field renderer to read state</li>
 *   <li>GUI to access/create state</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class FieldEditStateHolder {
    
    private static FieldEditState current = null;
    private static boolean testFieldActive = false;
    
    private FieldEditStateHolder() {}
    
    /**
     * Gets the current editing state, creating if needed.
     */
    public static FieldEditState getOrCreate() {
        if (current == null) {
            current = new FieldEditState();
            Logging.GUI.topic("state").debug("Created new FieldEditState");
        }
        return current;
    }
    
    /**
     * Gets the current editing state, or null if none exists.
     */
    @Nullable
    public static FieldEditState get() {
        return current;
    }
    
    /**
     * Sets the current editing state.
     */
    public static void set(FieldEditState state) {
        current = state;
        Logging.GUI.topic("state").debug("FieldEditState set");
    }
    
    /**
     * Clears the current editing state.
     * Called when GUI is closed and no test field is active.
     */
    public static void clear() {
        if (!testFieldActive) {
            current = null;
            Logging.GUI.topic("state").debug("FieldEditState cleared");
        }
    }
    
    /**
     * Resets state to defaults.
     */
    public static void reset() {
        current = new FieldEditState();
        Logging.GUI.topic("state").debug("FieldEditState reset to defaults");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEST FIELD CONTROL
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns true if test field is currently active.
     */
    public static boolean isTestFieldActive() {
        return testFieldActive;
    }
    
    /**
     * Sets test field active state.
     */
    public static void setTestFieldActive(boolean active) {
        testFieldActive = active;
        Logging.GUI.topic("state").debug("Test field active: {}", active);
    }
    
    /**
     * Spawns the test field (client-side preview).
     */
    public static void spawnTestField() {
        testFieldActive = true;
        // Ensure state exists
        getOrCreate();
        Logging.GUI.topic("state").info("Test field spawned");
        // TODO: Notify TestFieldRenderer to start rendering
    }
    
    /**
     * Despawns the test field.
     */
    public static void despawnTestField() {
        testFieldActive = false;
        Logging.GUI.topic("state").info("Test field despawned");
        // TODO: Notify TestFieldRenderer to stop rendering
    }
    
    /**
     * Toggles test field visibility.
     */
    public static void toggleTestField() {
        if (testFieldActive) {
            despawnTestField();
        } else {
            spawnTestField();
        }
    }
}

