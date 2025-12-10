#!/usr/bin/env python3
"""
Implement OP server profile save functionality.

Changes:
1. Add `saveToServer` boolean to ProfileSaveC2SPayload
2. Update server handler to check OP and save to server_profiles
3. Add save method to ServerProfileProvider
"""

from pathlib import Path

WORKSPACE = Path(__file__).parent.parent

def update_profile_save_payload():
    """Add saveToServer boolean to ProfileSaveC2SPayload."""
    file_path = WORKSPACE / "src/main/java/net/cyberpunk042/network/gui/ProfileSaveC2SPayload.java"
    
    new_content = '''package net.cyberpunk042.network.gui;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * G105: Client -> Server profile save request.
 * 
 * <p>Used for two purposes:</p>
 * <ul>
 *   <li>Apply profile to player's shield (saveToServer=false)</li>
 *   <li>OP saving profile to server_profiles (saveToServer=true, requires OP)</li>
 * </ul>
 */
public record ProfileSaveC2SPayload(
    String profileName,
    String profileJson,
    boolean saveToServer
) implements CustomPayload {
    
    public static final Id<ProfileSaveC2SPayload> ID = new Id<>(GuiPacketIds.PROFILE_SAVE_C2S);
    
    public static final PacketCodec<RegistryByteBuf, ProfileSaveC2SPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeString(payload.profileName);
            buf.writeString(payload.profileJson);
            buf.writeBoolean(payload.saveToServer);
        },
        buf -> new ProfileSaveC2SPayload(
            buf.readString(),
            buf.readString(),
            buf.readBoolean()
        )
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    /**
     * Create payload to apply profile to player's shield.
     */
    public static ProfileSaveC2SPayload applyToShield(String name, String json) {
        return new ProfileSaveC2SPayload(name, json, false);
    }
    
    /**
     * Create payload for OP to save profile to server.
     */
    public static ProfileSaveC2SPayload saveToServer(String name, String json) {
        return new ProfileSaveC2SPayload(name, json, true);
    }
}
'''
    file_path.write_text(new_content, encoding='utf-8')
    print(f"Updated: {file_path.name}")


def update_server_profile_provider():
    """Add saveProfile method to ServerProfileProvider."""
    file_path = WORKSPACE / "src/main/java/net/cyberpunk042/network/gui/ServerProfileProvider.java"
    content = file_path.read_text(encoding='utf-8')
    
    # Add save method before the createExampleProfile method
    save_method = '''
    /**
     * Save a profile to the server_profiles directory.
     * @param name profile name (used as filename)
     * @param json profile JSON content
     * @return true if saved successfully
     */
    public static boolean saveProfile(String name, String json) {
        Path configDir = FabricLoader.getInstance().getConfigDir()
            .resolve("the-virus-block")
            .resolve("server_profiles");
        
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            
            // Sanitize filename
            String safeName = name.replaceAll("[^a-zA-Z0-9_-]", "_");
            Path file = configDir.resolve(safeName + ".json");
            
            Files.writeString(file, json);
            
            // Update cache
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String displayName = obj.has("name") ? obj.get("name").getAsString() : name;
            String description = obj.has("description") ? obj.get("description").getAsString() : "";
            String category = obj.has("category") ? obj.get("category").getAsString() : "general";
            
            PROFILES.put(displayName, json);
            METADATA.put(displayName, new ProfileMeta(displayName, description, category));
            
            Logging.GUI.topic("profiles").info("Saved server profile: {}", displayName);
            return true;
            
        } catch (Exception e) {
            Logging.GUI.topic("profiles").error("Failed to save server profile {}: {}", name, e.getMessage());
            return false;
        }
    }

    /**
'''
    
    # Insert before createExampleProfile
    marker = "    /**\n     * Create an example server profile."
    if marker in content:
        content = content.replace(marker, save_method + "     * Create an example server profile.")
        file_path.write_text(content, encoding='utf-8')
        print(f"Updated: {file_path.name}")
    else:
        print(f"WARNING: Could not find marker in {file_path.name}")


def update_gui_packet_registration():
    """Update the ProfileSaveC2SPayload handler to support OP saves."""
    file_path = WORKSPACE / "src/main/java/net/cyberpunk042/network/gui/GuiPacketRegistration.java"
    content = file_path.read_text(encoding='utf-8')
    
    # Find and replace the profile save handler
    old_handler = '''        // Profile save handler - for future "apply to my shield" gameplay action
        // Local profile saving is handled client-side by ProfileManager
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            ProfileSaveC2SPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    var player = context.player();
                    Logging.GUI.topic("network").info("Player {} requesting profile application: {}", 
                        player.getName().getString(), payload.profileName());
                    // Future: Apply profile to player's actual shield gameplay
                    // For now, just acknowledge
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                        player, ProfileSyncS2CPayload.success(payload.profileName(), ""));
                });
            }
        );'''
    
    new_handler = '''        // Profile save handler - apply to shield OR OP save to server
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
        );'''
    
    if old_handler in content:
        content = content.replace(old_handler, new_handler)
        file_path.write_text(content, encoding='utf-8')
        print(f"Updated: {file_path.name}")
    else:
        print(f"WARNING: Could not find old handler in {file_path.name}")


def update_profiles_panel():
    """Add 'Save to Server' button for OPs in ProfilesPanel."""
    file_path = WORKSPACE / "src/client/java/net/cyberpunk042/client/gui/panel/ProfilesPanel.java"
    content = file_path.read_text(encoding='utf-8')
    
    # Add imports for networking
    old_imports = '''import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;'''
    
    new_imports = '''import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.cyberpunk042.client.gui.widget.ConfirmDialog;
import net.cyberpunk042.network.gui.ProfileSaveC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;'''
    
    content = content.replace(old_imports, new_imports)
    
    # Add saveToServerBtn field
    old_fields = '''    // Action buttons
    private ButtonWidget loadBtn, saveBtn, saveAsBtn, deleteBtn;
    private ButtonWidget duplicateBtn, renameBtn;'''
    
    new_fields = '''    // Action buttons
    private ButtonWidget loadBtn, saveBtn, saveAsBtn, deleteBtn;
    private ButtonWidget duplicateBtn, renameBtn;
    private ButtonWidget saveToServerBtn;  // OP only'''
    
    content = content.replace(old_fields, new_fields)
    
    # Add Save to Server button in init - after the second row of buttons
    old_buttons = '''        saveAsBtn = GuiWidgets.button(x, btnY, btnW, "Save As", "Save as new profile", this::saveAsProfile);
        duplicateBtn = GuiWidgets.button(x + btnW + GuiConstants.PADDING, btnY, btnW, "Duplicate", "Copy profile", this::duplicateProfile);
        renameBtn = GuiWidgets.button(x + (btnW + GuiConstants.PADDING) * 2, btnY, btnW, "Rename", "Rename profile", this::renameProfile);
        
        updateButtonStates();'''
    
    new_buttons = '''        saveAsBtn = GuiWidgets.button(x, btnY, btnW, "Save As", "Save as new profile", this::saveAsProfile);
        duplicateBtn = GuiWidgets.button(x + btnW + GuiConstants.PADDING, btnY, btnW, "Duplicate", "Copy profile", this::duplicateProfile);
        renameBtn = GuiWidgets.button(x + (btnW + GuiConstants.PADDING) * 2, btnY, btnW, "Rename", "Rename profile", this::renameProfile);
        
        // OP-only: Save to Server button (third row)
        btnY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        saveToServerBtn = GuiWidgets.button(x, btnY, listW, "⚡ Save to Server", "Save as server profile (OP only)", this::promptSaveToServer);
        
        updateButtonStates();'''
    
    content = content.replace(old_buttons, new_buttons)
    
    # Update updateButtonStates to handle saveToServerBtn
    old_states = '''    private void updateButtonStates() {
        ProfileEntry entry = getSelectedProfile();
        boolean hasSelection = entry != null;
        boolean isLocal = hasSelection && !entry.isServer();
        boolean isDirty = state.isDirty();
        
        if (loadBtn != null) loadBtn.active = hasSelection;
        if (saveBtn != null) saveBtn.active = isDirty && isLocal;
        if (saveAsBtn != null) saveAsBtn.active = true;
        if (deleteBtn != null) deleteBtn.active = isLocal && allProfiles.size() > 1;
        if (duplicateBtn != null) duplicateBtn.active = hasSelection;
        if (renameBtn != null) renameBtn.active = isLocal;
    }'''
    
    new_states = '''    private void updateButtonStates() {
        ProfileEntry entry = getSelectedProfile();
        boolean hasSelection = entry != null;
        boolean isLocal = hasSelection && !entry.isServer();
        boolean isDirty = state.isDirty();
        boolean isOp = isPlayerOp();
        
        if (loadBtn != null) loadBtn.active = hasSelection;
        if (saveBtn != null) saveBtn.active = isDirty && isLocal;
        if (saveAsBtn != null) saveAsBtn.active = true;
        if (deleteBtn != null) deleteBtn.active = isLocal && allProfiles.size() > 1;
        if (duplicateBtn != null) duplicateBtn.active = hasSelection;
        if (renameBtn != null) renameBtn.active = isLocal;
        
        // OP-only button
        if (saveToServerBtn != null) {
            saveToServerBtn.active = isOp;
            saveToServerBtn.visible = isOp;
        }
    }
    
    /**
     * Check if current player has OP permissions.
     */
    private boolean isPlayerOp() {
        var player = MinecraftClient.getInstance().player;
        return player != null && player.hasPermissionLevel(2);
    }'''
    
    content = content.replace(old_states, new_states)
    
    # Add getWidgets update for saveToServerBtn
    old_widgets = '''        if (renameBtn != null) list.add(renameBtn);
        return list;'''
    
    new_widgets = '''        if (renameBtn != null) list.add(renameBtn);
        if (saveToServerBtn != null && saveToServerBtn.visible) list.add(saveToServerBtn);
        return list;'''
    
    content = content.replace(old_widgets, new_widgets)
    
    # Add render for saveToServerBtn - after renameBtn render
    old_render = '''        if (renameBtn != null) renameBtn.render(context, mouseX, mouseY, delta);
        
        // Dirty indicator'''
    
    new_render = '''        if (renameBtn != null) renameBtn.render(context, mouseX, mouseY, delta);
        if (saveToServerBtn != null && saveToServerBtn.visible) saveToServerBtn.render(context, mouseX, mouseY, delta);
        
        // Dirty indicator'''
    
    content = content.replace(old_render, new_render)
    
    # Add promptSaveToServer and doSaveToServer methods after renameProfile
    old_rename = '''    private void renameProfile() {
        ProfileEntry entry = getSelectedProfile();
        if (entry == null || entry.isServer()) {
            ToastNotification.warning("Cannot rename server profile");
            return;
        }
        
        String newName = nameField.getText().trim();
        if (newName.isEmpty()) {
            ToastNotification.warning("Enter a name");
            return;
        }
        
        // Remove old, add new
        int idx = allProfiles.indexOf(entry);
        if (idx >= 0) {
            allProfiles.set(idx, new ProfileEntry(newName, false));
            applyFilters();
            ToastNotification.info("Renamed: " + entry.name() + " → " + newName);
        }
    }
    
    private ProfileEntry getSelectedProfile()'''
    
    new_rename = '''    private void renameProfile() {
        ProfileEntry entry = getSelectedProfile();
        if (entry == null || entry.isServer()) {
            ToastNotification.warning("Cannot rename server profile");
            return;
        }
        
        String newName = nameField.getText().trim();
        if (newName.isEmpty()) {
            ToastNotification.warning("Enter a name");
            return;
        }
        
        // Remove old, add new
        int idx = allProfiles.indexOf(entry);
        if (idx >= 0) {
            allProfiles.set(idx, new ProfileEntry(newName, false));
            applyFilters();
            ToastNotification.info("Renamed: " + entry.name() + " → " + newName);
        }
    }
    
    /**
     * Prompt before saving to server (OP only).
     */
    private void promptSaveToServer() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            ToastNotification.warning("Enter a profile name");
            return;
        }
        
        // Show warning dialog
        ConfirmDialog.show(parent,
            "Save to Server",
            "This will save '" + name + "' as a server profile.\\n\\nAll players will be able to use this profile.\\n\\nContinue?",
            confirmed -> {
                if (confirmed) {
                    doSaveToServer(name);
                }
            }
        );
    }
    
    /**
     * Send profile to server for OP save.
     */
    private void doSaveToServer(String name) {
        // Build profile JSON from current state
        String json = state.toProfileJson(name);
        
        // Send to server
        ClientPlayNetworking.send(ProfileSaveC2SPayload.saveToServer(name, json));
        ToastNotification.info("Saving to server: " + name);
        Logging.GUI.topic("profile").info("OP saving server profile: {}", name);
    }
    
    private ProfileEntry getSelectedProfile()'''
    
    content = content.replace(old_rename, new_rename)
    
    file_path.write_text(content, encoding='utf-8')
    print(f"Updated: {file_path.name}")


def add_to_profile_json_method():
    """Add toProfileJson method to FieldEditState if not present."""
    file_path = WORKSPACE / "src/client/java/net/cyberpunk042/client/gui/state/FieldEditState.java"
    content = file_path.read_text(encoding='utf-8')
    
    # Check if method already exists
    if "toProfileJson" in content:
        print("FieldEditState.toProfileJson already exists")
        return
    
    # Find a good place to add - before the closing brace
    # Look for the last method and add after it
    marker = "    // End of FieldEditState"
    if marker not in content:
        # Add before final closing brace
        marker = "\n}"
        insert_pos = content.rfind(marker)
        if insert_pos > 0:
            method = '''
    /**
     * Export current state as profile JSON.
     */
    public String toProfileJson(String profileName) {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("name", profileName);
        json.addProperty("version", "1.0");
        json.addProperty("description", "Saved from GUI");
        json.addProperty("category", "general");
        
        // Add definition from current state
        com.google.gson.JsonObject def = new com.google.gson.JsonObject();
        def.addProperty("id", profileName.toLowerCase().replace(" ", "_"));
        def.addProperty("type", "SHIELD");
        def.addProperty("baseRadius", getRadius());
        // Add more fields as needed from state
        
        json.add("definition", def);
        
        return new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json);
    }
'''
            content = content[:insert_pos] + method + content[insert_pos:]
            file_path.write_text(content, encoding='utf-8')
            print(f"Updated: {file_path.name} - added toProfileJson")
    else:
        print(f"WARNING: Could not find insertion point in {file_path.name}")


def main():
    print("=== Implementing OP Server Profile Save ===\n")
    
    update_profile_save_payload()
    update_server_profile_provider()
    update_gui_packet_registration()
    update_profiles_panel()
    add_to_profile_json_method()
    
    print("\n=== Done ===")
    print("\nChanges made:")
    print("1. ProfileSaveC2SPayload - added saveToServer boolean + factory methods")
    print("2. ServerProfileProvider - added saveProfile() method")
    print("3. GuiPacketRegistration - handler checks OP and saves to server if authorized")
    print("4. ProfilesPanel - added 'Save to Server' button (OP only) with warning dialog")
    print("5. FieldEditState - added toProfileJson() method for export")


if __name__ == "__main__":
    main()

