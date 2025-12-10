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
        PayloadTypeRegistry.playS2C().register(FieldEditUpdateS2CPayload.ID, FieldEditUpdateS2CPayload.CODEC);
        
        // Client -> Server
        PayloadTypeRegistry.playC2S().register(ProfileSaveC2SPayload.ID, ProfileSaveC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ProfileLoadC2SPayload.ID, ProfileLoadC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DebugFieldC2SPayload.ID, DebugFieldC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestProfilesC2SPayload.ID, RequestProfilesC2SPayload.CODEC);
        
        Logging.GUI.topic("network").info("GUI packets registered (5 S2C, 4 C2S)");
    }
    
    /**
     * G140: Register server-side packet handlers.
     * Call this after registerAll() on server.
     */
    public static void registerServerHandlers() {
        // Load server profiles on registration
        ServerProfileProvider.load();
        
        // Profile save handler - apply to shield OR OP save to server
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            ProfileSaveC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    
                    if (payload.saveToServer()) {
                        // OP saving profile to server_profiles
                        if (!player.hasPermissionLevel(2)) {
                            Logging.GUI.topic("network").warn("Non-OP {} tried to save server profile", 
                                player.getName().getString());
                            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                                player, ProfileSyncS2CPayload.error("Operator permission required"));
                            return;
                        }
                        
                        Logging.GUI.topic("network").info("OP {} saving server profile: {}", 
                            player.getName().getString(), payload.profileName());
                        
                        boolean saved = ServerProfileProvider.saveProfile(payload.profileName(), payload.profileJson());
                        if (saved) {
                            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                                player, ProfileSyncS2CPayload.success(payload.profileName(), "Server profile saved"));
                        } else {
                            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                                player, ProfileSyncS2CPayload.error("Failed to save server profile"));
                        }
                    } else {
                        // Apply profile to player's shield (future gameplay)
                        Logging.GUI.topic("network").info("Player {} applying profile: {}", 
                            player.getName().getString(), payload.profileName());
                        // Future: Apply profile to player's actual shield gameplay
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                            player, ProfileSyncS2CPayload.success(payload.profileName(), ""));
                    }
                });
            }
        );
        
        // Profile load handler - load a server profile by name
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            ProfileLoadC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    String profileName = payload.profileName();
                    Logging.GUI.topic("network").debug("Player {} requesting server profile: {}", 
                        player.getName().getString(), profileName);
                    
                    String json = ServerProfileProvider.getProfile(profileName);
                    if (json != null) {
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                            player, ProfileSyncS2CPayload.success(profileName, json));
                    } else {
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                            player, ProfileSyncS2CPayload.error("Profile not found: " + profileName));
                    }
                });
            }
        );
        
        // Debug field handler - spawn/update/despawn preview fields
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            DebugFieldC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    if (payload.spawn()) {
                        Logging.GUI.topic("network").debug("Player {} spawning debug field", player.getName().getString());
                        boolean success = DebugFieldTracker.spawnOrUpdate(player, payload.fieldJson());
                        // Send confirmation
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                            player, new DebugFieldS2CPayload("", success, success ? "Debug field spawned" : "Failed to spawn"));
                    } else if (payload.despawn()) {
                        Logging.GUI.topic("network").debug("Player {} despawning debug field", player.getName().getString());
                        boolean success = DebugFieldTracker.despawn(player);
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                            player, new DebugFieldS2CPayload("", success, success ? "Debug field removed" : "No debug field to remove"));
                    } else {
                        // Update existing debug field
                        Logging.GUI.topic("network").trace("Player {} updating debug field", player.getName().getString());
                        DebugFieldTracker.spawnOrUpdate(player, payload.fieldJson());
                    }
                });
            }
        );
        
        // Request profiles handler - send list of available server profiles
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            RequestProfilesC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    Logging.GUI.topic("network").debug("Player {} requesting server profiles list", player.getName().getString());
                    
                    java.util.List<String> profiles = ServerProfileProvider.listProfiles();
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                        player, new ServerProfilesS2CPayload(profiles));
                });
            }
        );
        
        Logging.GUI.topic("network").info("GUI server handlers registered");
    }
}
