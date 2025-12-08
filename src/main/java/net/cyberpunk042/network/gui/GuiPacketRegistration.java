package net.cyberpunk042.network.gui;

import net.cyberpunk042.log.Logging;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * G139-G140: Packet type registration for GUI networking.
 * 
 * <p>Call {@link #registerAll()} from mod initializer.</p>
 */
public final class GuiPacketRegistration {
    
    private GuiPacketRegistration() {}
    
    /**
     * G139: Register all payload types.
     */
    public static void registerAll() {
        // Server -> Client
        PayloadTypeRegistry.playS2C().register(GuiOpenS2CPayload.ID, GuiOpenS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ProfileSyncS2CPayload.ID, ProfileSyncS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DebugFieldS2CPayload.ID, DebugFieldS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerProfilesS2CPayload.ID, ServerProfilesS2CPayload.CODEC);
        
        // Client -> Server
        PayloadTypeRegistry.playC2S().register(ProfileSaveC2SPayload.ID, ProfileSaveC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ProfileLoadC2SPayload.ID, ProfileLoadC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DebugFieldC2SPayload.ID, DebugFieldC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestProfilesC2SPayload.ID, RequestProfilesC2SPayload.CODEC);
        
        Logging.GUI.topic("network").info("GUI packets registered (4 S2C, 4 C2S)");
    }
    
    /**
     * G140: Register server-side packet handlers.
     * Call this after registerAll() on server.
     */
    public static void registerServerHandlers() {
        // Profile save handler
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            ProfileSaveC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    Logging.GUI.topic("network").info("Player {} saving profile: {}", 
                        player.getName().getString(), payload.profileName());
                    // TODO: Save profile to storage
                    // TODO: Send ProfileSyncS2CPayload response
                });
            }
        );
        
        // Profile load handler
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            ProfileLoadC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    Logging.GUI.topic("network").info("Player {} loading profile: {}", 
                        player.getName().getString(), payload.profileName());
                    // TODO: Load profile from storage
                    // TODO: Send ProfileSyncS2CPayload response
                });
            }
        );
        
        // Debug field handler
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            DebugFieldC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    if (payload.spawn()) {
                        Logging.GUI.topic("network").debug("Player {} spawning debug field", player.getName().getString());
                        // TODO: Spawn debug field
                    } else if (payload.despawn()) {
                        Logging.GUI.topic("network").debug("Player {} despawning debug field", player.getName().getString());
                        // TODO: Despawn debug field
                    } else {
                        Logging.GUI.topic("network").trace("Player {} updating debug field", player.getName().getString());
                        // TODO: Update debug field
                    }
                });
            }
        );
        
        // Request profiles handler
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            RequestProfilesC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    Logging.GUI.topic("network").debug("Player {} requesting server profiles", player.getName().getString());
                    // TODO: Send ServerProfilesS2CPayload with available profiles
                });
            }
        );
        
        Logging.GUI.topic("network").info("GUI server handlers registered");
    }
}
