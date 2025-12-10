package net.cyberpunk042.client.network;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.network.gui.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * G135-G138: Utilities for sending GUI packets to server.
 */
@Environment(EnvType.CLIENT)
public final class GuiPacketSender {
    
    private GuiPacketSender() {}
    
    /**
     * G135: Send profile save request.
     */
    public static void saveProfile(String name, String json) {
        Logging.GUI.topic("network").debug("Sending profile save: {}", name);
        ClientPlayNetworking.send(ProfileSaveC2SPayload.applyToShield(name, json));
    }
    
    /**
     * G136: Send profile load request.
     */
    public static void loadProfile(String name) {
        Logging.GUI.topic("network").debug("Sending profile load: {}", name);
        ClientPlayNetworking.send(new ProfileLoadC2SPayload(name));
    }
    
    /**
     * G137: Send debug field update.
     */
    public static void updateDebugField(String json) {
        Logging.GUI.topic("network").trace("Sending debug field update");
        ClientPlayNetworking.send(DebugFieldC2SPayload.updateField(json));
    }
    
    /**
     * Spawn debug field.
     */
    public static void spawnDebugField(String json) {
        Logging.GUI.topic("network").debug("Sending debug field spawn");
        ClientPlayNetworking.send(DebugFieldC2SPayload.spawnField(json));
    }
    
    /**
     * Despawn debug field.
     */
    public static void despawnDebugField() {
        Logging.GUI.topic("network").debug("Sending debug field despawn");
        ClientPlayNetworking.send(DebugFieldC2SPayload.despawnField());
    }
    
    /**
     * G138: Request server profiles list.
     */
    public static void requestServerProfiles() {
        Logging.GUI.topic("network").debug("Requesting server profiles");
        ClientPlayNetworking.send(new RequestProfilesC2SPayload());
    }
}
