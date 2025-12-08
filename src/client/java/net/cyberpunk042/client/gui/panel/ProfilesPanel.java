package net.cyberpunk042.client.gui.panel;

import net.cyberpunk042.client.gui.state.GuiState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ConfirmDialog;
import net.cyberpunk042.client.gui.widget.ToastNotification;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * G91-G100: Profiles management panel.
 * 
 * <p>Handles player field profiles (visual variants):</p>
 * <ul>
 *   <li>G91: Profile list with selection</li>
 *   <li>G92: Load profile button</li>
 *   <li>G93: Save current as new profile</li>
 *   <li>G94: Delete profile with confirmation</li>
 *   <li>G95: Rename profile</li>
 *   <li>G96: Duplicate profile</li>
 *   <li>G97: Import from server defaults</li>
 *   <li>G98: Export to file (JSON)</li>
 *   <li>G99: Profile preview thumbnail</li>
 *   <li>G100: Set as default profile</li>
 * </ul>
 */
public class ProfilesPanel extends AbstractPanel {
    
    // Profile list
    private final List<String> profiles = new ArrayList<>();
    private int selectedProfileIndex = 0;
    private int scrollOffset = 0;
    
    // Widgets
    private ButtonWidget loadBtn;
    private ButtonWidget saveBtn;
    private ButtonWidget deleteBtn;
    private ButtonWidget renameBtn;
    private ButtonWidget duplicateBtn;
    private ButtonWidget importBtn;
    private ButtonWidget exportBtn;
    private ButtonWidget setDefaultBtn;
    private TextFieldWidget nameField;
    
    // Layout constants
    private static final int LIST_HEIGHT = 120;
    private static final int ITEM_HEIGHT = 20;
    
    public ProfilesPanel(Screen parent, GuiState state) {
        super(parent, state);
        
        // Initialize with some default profiles
        profiles.add("Default");
        profiles.add("Sphere Solid");
        profiles.add("Radar Pulse");
        profiles.add("Cage Wireframe");
        profiles.add("Stealth Minimal");
        
        Logging.GUI.topic("panel").debug("ProfilesPanel created with {} profiles", profiles.size());
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        
        int x = GuiConstants.PADDING;
        int y = GuiConstants.TAB_HEIGHT + GuiConstants.PADDING;
        int w = width - GuiConstants.PADDING * 2;
        int btnW = (w - GuiConstants.PADDING * 2) / 3;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // G95: Profile name field
        nameField = new TextFieldWidget(
            net.minecraft.client.MinecraftClient.getInstance().textRenderer,
            x, y, w, GuiConstants.WIDGET_HEIGHT,
            net.minecraft.text.Text.literal("Profile Name")
        );
        nameField.setMaxLength(32);
        nameField.setText(getSelectedProfileName());
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Profile list area (G91) - rendered manually
        y += LIST_HEIGHT + GuiConstants.PADDING;
        
        // G92: Load button
        loadBtn = GuiWidgets.button(x, y, btnW, "Load", "Load selected profile", this::loadProfile);
        
        // G93: Save button
        saveBtn = GuiWidgets.button(x + btnW + GuiConstants.PADDING, y, btnW, "Save", "Save current settings", this::saveProfile);
        
        // G94: Delete button
        deleteBtn = GuiWidgets.button(x + (btnW + GuiConstants.PADDING) * 2, y, btnW, "Delete", "Delete selected profile", this::confirmDelete);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // G96: Duplicate
        duplicateBtn = GuiWidgets.button(x, y, halfW, "Duplicate", "Copy selected profile", this::duplicateProfile);
        
        // G95: Rename (uses name field above)
        renameBtn = GuiWidgets.button(x + halfW + GuiConstants.PADDING, y, halfW, "Rename", "Rename selected profile", this::renameProfile);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // G97: Import from server
        importBtn = GuiWidgets.button(x, y, halfW, "Import Server", "Load server defaults", this::importFromServer);
        
        // G98: Export to file
        exportBtn = GuiWidgets.button(x + halfW + GuiConstants.PADDING, y, halfW, "Export JSON", "Export to file", this::exportToFile);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // G100: Set as default
        setDefaultBtn = GuiWidgets.button(x, y, w, "Set as Default", "Use this profile by default", this::setAsDefault);
        
        updateButtonStates();
        
        Logging.GUI.topic("panel").debug("ProfilesPanel initialized");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G92: LOAD PROFILE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void loadProfile() {
        String profileName = getSelectedProfileName();
        state.setCurrentProfileName(profileName);
        // TODO: Actually load profile data into state
        ToastNotification.success("Loaded: " + profileName);
        Logging.GUI.topic("profile").info("Loaded profile: {}", profileName);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G93: SAVE PROFILE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void saveProfile() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            ToastNotification.warning("Enter a profile name");
            return;
        }
        
        if (!profiles.contains(name)) {
            profiles.add(name);
            selectedProfileIndex = profiles.size() - 1;
        }
        
        // TODO: Save current state to profile storage
        state.clearDirty();
        ToastNotification.success("Saved: " + name);
        Logging.GUI.topic("profile").info("Saved profile: {}", name);
        updateButtonStates();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G94: DELETE PROFILE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void confirmDelete() {
        if (profiles.size() <= 1) {
            ToastNotification.warning("Cannot delete last profile");
            return;
        }
        
        String name = getSelectedProfileName();
        ConfirmDialog.show(parent, "Delete Profile", "Delete '" + name + "'?", this::deleteProfile);
    }
    
    private void deleteProfile() {
        String name = getSelectedProfileName();
        profiles.remove(selectedProfileIndex);
        if (selectedProfileIndex >= profiles.size()) {
            selectedProfileIndex = profiles.size() - 1;
        }
        nameField.setText(getSelectedProfileName());
        ToastNotification.info("Deleted: " + name);
        Logging.GUI.topic("profile").info("Deleted profile: {}", name);
        updateButtonStates();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G95: RENAME PROFILE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void renameProfile() {
        String newName = nameField.getText().trim();
        if (newName.isEmpty()) {
            ToastNotification.warning("Enter a name");
            return;
        }
        
        String oldName = getSelectedProfileName();
        profiles.set(selectedProfileIndex, newName);
        ToastNotification.info("Renamed: " + oldName + " → " + newName);
        Logging.GUI.topic("profile").info("Renamed profile: {} -> {}", oldName, newName);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G96: DUPLICATE PROFILE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void duplicateProfile() {
        String original = getSelectedProfileName();
        String copy = original + " (Copy)";
        profiles.add(copy);
        selectedProfileIndex = profiles.size() - 1;
        nameField.setText(copy);
        ToastNotification.info("Duplicated: " + original);
        Logging.GUI.topic("profile").info("Duplicated profile: {} -> {}", original, copy);
        updateButtonStates();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G97: IMPORT FROM SERVER
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void importFromServer() {
        // TODO: Request server default profiles
        ToastNotification.info("Requesting server profiles...");
        Logging.GUI.topic("profile").info("Import from server requested");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G98: EXPORT TO FILE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void exportToFile() {
        String name = getSelectedProfileName();
        // TODO: Export to JSON file
        ToastNotification.success("Exported: " + name + ".json");
        Logging.GUI.topic("profile").info("Exported profile: {}", name);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G100: SET AS DEFAULT
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void setAsDefault() {
        String name = getSelectedProfileName();
        // TODO: Store as default profile
        ToastNotification.success("Default: " + name);
        Logging.GUI.topic("profile").info("Set default profile: {}", name);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UI HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String getSelectedProfileName() {
        if (selectedProfileIndex >= 0 && selectedProfileIndex < profiles.size()) {
            return profiles.get(selectedProfileIndex);
        }
        return "Default";
    }
    
    private void updateButtonStates() {
        boolean hasSelection = selectedProfileIndex >= 0 && selectedProfileIndex < profiles.size();
        boolean canDelete = profiles.size() > 1;
        
        if (loadBtn != null) loadBtn.active = hasSelection;
        if (deleteBtn != null) deleteBtn.active = hasSelection && canDelete;
        if (renameBtn != null) renameBtn.active = hasSelection;
        if (duplicateBtn != null) duplicateBtn.active = hasSelection;
        if (exportBtn != null) exportBtn.active = hasSelection;
        if (setDefaultBtn != null) setDefaultBtn.active = hasSelection;
    }
    
    @Override
    public void tick() {
        // TextFieldWidget doesn't need tick in 1.21.6
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelY = GuiConstants.TAB_HEIGHT;
        context.fill(0, panelY, panelWidth, panelHeight, GuiConstants.BG_PANEL);
        
        var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        
        // G91: Profile list
        int listX = GuiConstants.PADDING;
        int listY = panelY + GuiConstants.PADDING + GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        int listW = panelWidth - GuiConstants.PADDING * 2;
        
        // List background
        context.fill(listX, listY, listX + listW, listY + LIST_HEIGHT, 0xFF1A1A1A);
        context.drawBorder(listX, listY, listW, LIST_HEIGHT, GuiConstants.BORDER);
        
        // G99: Profile items (with selection highlight)
        int itemY = listY + 2 - scrollOffset;
        for (int i = 0; i < profiles.size(); i++) {
            if (itemY >= listY && itemY + ITEM_HEIGHT <= listY + LIST_HEIGHT) {
                boolean selected = i == selectedProfileIndex;
                boolean hovered = mouseX >= listX && mouseX < listX + listW && 
                                  mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
                
                // Background
                if (selected) {
                    context.fill(listX + 2, itemY, listX + listW - 2, itemY + ITEM_HEIGHT - 2, GuiConstants.ACCENT);
                } else if (hovered) {
                    context.fill(listX + 2, itemY, listX + listW - 2, itemY + ITEM_HEIGHT - 2, 0x40FFFFFF);
                }
                
                // Profile name
                String name = profiles.get(i);
                int textColor = selected ? 0xFFFFFFFF : GuiConstants.TEXT_PRIMARY;
                context.drawText(textRenderer, name, listX + 6, itemY + 5, textColor, false);
                
                // Default indicator
                if (name.equals(state.getCurrentProfileName())) {
                    context.drawText(textRenderer, "★", listX + listW - 14, itemY + 5, GuiConstants.SUCCESS, false);
                }
            }
            itemY += ITEM_HEIGHT;
        }
        
        // Render widgets
        if (nameField != null) nameField.render(context, mouseX, mouseY, delta);
        if (loadBtn != null) loadBtn.render(context, mouseX, mouseY, delta);
        if (saveBtn != null) saveBtn.render(context, mouseX, mouseY, delta);
        if (deleteBtn != null) deleteBtn.render(context, mouseX, mouseY, delta);
        if (duplicateBtn != null) duplicateBtn.render(context, mouseX, mouseY, delta);
        if (renameBtn != null) renameBtn.render(context, mouseX, mouseY, delta);
        if (importBtn != null) importBtn.render(context, mouseX, mouseY, delta);
        if (exportBtn != null) exportBtn.render(context, mouseX, mouseY, delta);
        if (setDefaultBtn != null) setDefaultBtn.render(context, mouseX, mouseY, delta);
    }
    
    /**
     * Handle mouse click for profile selection.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listX = GuiConstants.PADDING;
        int listY = GuiConstants.TAB_HEIGHT + GuiConstants.PADDING + GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        int listW = panelWidth - GuiConstants.PADDING * 2;
        
        if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + LIST_HEIGHT) {
            int clickedIndex = (int) ((mouseY - listY + scrollOffset) / ITEM_HEIGHT);
            if (clickedIndex >= 0 && clickedIndex < profiles.size()) {
                selectedProfileIndex = clickedIndex;
                nameField.setText(getSelectedProfileName());
                updateButtonStates();
                Logging.GUI.topic("profile").trace("Selected profile: {}", getSelectedProfileName());
                return true;
            }
        }
        return false;
    }
    
    /**
     * Handle mouse scroll for profile list.
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        int maxScroll = Math.max(0, profiles.size() * ITEM_HEIGHT - LIST_HEIGHT);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(vAmount * ITEM_HEIGHT)));
        return true;
    }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> list = new ArrayList<>();
        if (nameField != null) list.add(nameField);
        if (loadBtn != null) list.add(loadBtn);
        if (saveBtn != null) list.add(saveBtn);
        if (deleteBtn != null) list.add(deleteBtn);
        if (duplicateBtn != null) list.add(duplicateBtn);
        if (renameBtn != null) list.add(renameBtn);
        if (importBtn != null) list.add(importBtn);
        if (exportBtn != null) list.add(exportBtn);
        if (setDefaultBtn != null) list.add(setDefaultBtn);
        return list;
    }
}
