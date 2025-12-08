package net.cyberpunk042.client.network;

import net.cyberpunk042.client.gui.screen.FieldCustomizerScreen;
import net.cyberpunk042.client.gui.state.GuiState;
import net.cyberpunk042.client.gui.widget.ToastNotification;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.network.gui.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

/**
 * G131-G134: Client-side packet handlers for GUI.
 */
@Environment(EnvType.CLIENT)
public final class GuiClientHandlers {
    
    private GuiClientHandlers() {}
    
    /**
     * G131: Register all client packet handlers.
     * Call this from client mod initializer.
     */
    public static void register() {
        // G132: Handle GUI open command from server
        ClientPlayNetworking.registerGlobalReceiver(GuiOpenS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Logging.GUI.topic("network").info("Received GUI open: profile={}, debug={}", 
                    payload.profileName(), payload.debugUnlocked());
                
                GuiState state = new GuiState();
                state.setCurrentProfileName(payload.profileName());
                state.setDebugUnlocked(payload.debugUnlocked());
                
                MinecraftClient.getInstance().setScreen(new FieldCustomizerScreen(state));
            });
        });
        
        // G133: Handle profile sync from server
        ClientPlayNetworking.registerGlobalReceiver(ProfileSyncS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (payload.success()) {
                    Logging.GUI.topic("network").debug("Profile synced: {}", payload.profileName());
                    ToastNotification.success("Profile loaded: " + payload.profileName());
                    // TODO: Apply profile JSON to current state
                } else {
                    Logging.GUI.topic("network").warn("Profile sync failed: {}", payload.message());
                    ToastNotification.error(payload.message());
                }
            });
        });
        
        // G134: Handle debug field response
        ClientPlayNetworking.registerGlobalReceiver(DebugFieldS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Logging.GUI.topic("network").debug("Debug field update: active={}", payload.active());
                if (!payload.status().isEmpty()) {
                    ToastNotification.info(payload.status());
                }
            });
        });
        
        // Handle server profiles list
        ClientPlayNetworking.registerGlobalReceiver(ServerProfilesS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Logging.GUI.topic("network").info("Received {} server profiles", payload.profileNames().size());
                // TODO: Update profiles panel with server profiles
            });
        });
        
        Logging.GUI.topic("network").info("GUI client handlers registered");
    }
}
