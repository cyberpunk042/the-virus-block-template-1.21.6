package net.cyberpunk042.client;

import net.cyberpunk042.client.network.GuiClientHandlers;
import net.cyberpunk042.log.Logging;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * G141-G145: Client-side GUI initialization.
 * 
 * <p>Wires together all GUI components:</p>
 * <ul>
 *   <li>G141: Client packet handlers</li>
 *   <li>G142: Keybind registration (future)</li>
 *   <li>G143: Config loading</li>
 *   <li>G144: Profile storage init</li>
 *   <li>G145: Debug field renderer (future)</li>
 * </ul>
 * 
 * <p>Register this in fabric.mod.json under "client" entrypoints.</p>
 */
@Environment(EnvType.CLIENT)
public class GuiClientInit implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        Logging.GUI.topic("init").info("Initializing GUI client...");
        
        // G141: Register packet handlers
        GuiClientHandlers.register();
        
        // G142: Keybinds (placeholder for future)
        // KeyBindings.register();
        
        // G143: Load client config
        // GuiConfig.load();
        
        // G144: Initialize profile storage
        // ProfileStorage.init();
        
        // G145: Register debug field renderer
        // DebugFieldRenderer.register();
        
        Logging.GUI.topic("init").info("GUI client initialized");
    }
}
