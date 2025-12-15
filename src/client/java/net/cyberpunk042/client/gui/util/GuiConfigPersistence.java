package net.cyberpunk042.client.gui.util;

import net.cyberpunk042.client.gui.layout.GuiMode;
import net.cyberpunk042.log.Logging;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Handles persistence of GUI preferences (mode, etc.).
 * 
 * <p>Preferences are stored in the Fabric config directory at:
 * {@code config/the-virus-block/gui_preferences.properties}</p>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Load saved mode (or default to FULLSCREEN)
 * GuiMode mode = GuiConfigPersistence.loadSavedMode();
 * 
 * // Save mode when changed
 * GuiConfigPersistence.saveMode(GuiMode.WINDOWED);
 * </pre>
 */
public final class GuiConfigPersistence {
    
    private GuiConfigPersistence() {}
    
    /**
     * Path to the GUI preferences file.
     */
    private static final Path GUI_CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("the-virus-block")
        .resolve("gui_preferences.properties");
    
    /**
     * Loads the saved GUI mode from preferences.
     * 
     * @return The saved mode, or {@link GuiMode#FULLSCREEN} if not found or error
     */
    public static GuiMode loadSavedMode() {
        try {
            if (Files.exists(GUI_CONFIG_PATH)) {
                Properties props = new Properties();
                try (var reader = Files.newBufferedReader(GUI_CONFIG_PATH)) {
                    props.load(reader);
                }
                String modeStr = props.getProperty("gui.mode", "FULLSCREEN");
                return GuiMode.valueOf(modeStr);
            }
        } catch (Exception e) {
            Logging.GUI.topic("config").warn("Failed to load GUI preferences: {}", e.getMessage());
        }
        return GuiMode.FULLSCREEN;
    }
    
    /**
     * Saves the current GUI mode to preferences.
     * 
     * @param mode The mode to save
     */
    public static void saveMode(GuiMode mode) {
        try {
            Files.createDirectories(GUI_CONFIG_PATH.getParent());
            Properties props = new Properties();
            props.setProperty("gui.mode", mode.name());
            try (var writer = Files.newBufferedWriter(GUI_CONFIG_PATH)) {
                props.store(writer, "GUI Preferences");
            }
        } catch (IOException e) {
            Logging.GUI.topic("config").warn("Failed to save GUI preferences: {}", e.getMessage());
        }
    }
    
    /**
     * Returns the path to the GUI config file.
     */
    public static Path getConfigPath() {
        return GUI_CONFIG_PATH;
    }
}
