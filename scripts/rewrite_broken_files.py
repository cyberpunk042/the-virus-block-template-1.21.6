#!/usr/bin/env python3
"""
Rewrite ProfilesPanel and BottomActionBar to match existing API.
"""

from pathlib import Path

GUI_PATH = Path("src/client/java/net/cyberpunk042/client/gui")

# =============================================================================
# ProfilesPanel - matches AbstractPanel(Screen, FieldEditState)
# =============================================================================

PROFILES_PANEL = '''package net.cyberpunk042.client.gui.panel;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditState.ProfileEntry;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ToastNotification;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * G91-G100: Profiles management panel.
 */
public class ProfilesPanel extends AbstractPanel {
    
    private final List<ProfileEntry> profiles;
    private int selectedProfileIndex = 0;
    private int listScrollOffset = 0;
    
    private ButtonWidget loadBtn, saveBtn, deleteBtn;
    private TextFieldWidget nameField;
    
    private static final int LIST_HEIGHT = 120;
    private static final int ITEM_HEIGHT = 20;
    
    public ProfilesPanel(Screen parent, FieldEditState state) {
        super(parent, state);
        this.profiles = state.getProfiles();
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        
        int x = GuiConstants.PADDING;
        int y = GuiConstants.TAB_HEIGHT + GuiConstants.PADDING;
        int w = width - GuiConstants.PADDING * 2;
        int btnW = (w - GuiConstants.PADDING * 2) / 3;
        
        nameField = new TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            x, y, w, GuiConstants.WIDGET_HEIGHT,
            net.minecraft.text.Text.literal("Profile Name")
        );
        nameField.setMaxLength(32);
        nameField.setText(getSelectedProfileName());
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING + LIST_HEIGHT + GuiConstants.PADDING;
        
        loadBtn = GuiWidgets.button(x, y, btnW, "Load", "Load selected profile", this::loadProfile);
        saveBtn = GuiWidgets.button(x + btnW + GuiConstants.PADDING, y, btnW, "Save", "Save current settings", this::saveProfile);
        deleteBtn = GuiWidgets.button(x + (btnW + GuiConstants.PADDING) * 2, y, btnW, "Delete", "Delete selected profile", this::deleteProfile);
    }
    
    @Override
    public void tick() {
        // Nothing to tick
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelY = GuiConstants.TAB_HEIGHT;
        context.fill(0, panelY, panelWidth, panelHeight, GuiConstants.BG_PANEL);
        
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        
        int listX = GuiConstants.PADDING;
        int listY = panelY + GuiConstants.PADDING + GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        int listW = panelWidth - GuiConstants.PADDING * 2;
        
        context.fill(listX, listY, listX + listW, listY + LIST_HEIGHT, 0xFF1A1A1A);
        context.drawBorder(listX, listY, listW, LIST_HEIGHT, GuiConstants.BORDER);
        
        int itemY = listY + 2 - listScrollOffset;
        for (int i = 0; i < profiles.size(); i++) {
            if (itemY >= listY && itemY + ITEM_HEIGHT <= listY + LIST_HEIGHT) {
                boolean selected = i == selectedProfileIndex;
                if (selected) {
                    context.fill(listX + 2, itemY, listX + listW - 2, itemY + ITEM_HEIGHT - 2, GuiConstants.ACCENT);
                }
                ProfileEntry entry = profiles.get(i);
                String name = entry.name() + (entry.isServer() ? " (server)" : " (local)");
                context.drawText(textRenderer, name, listX + 6, itemY + 5, selected ? 0xFFFFFFFF : GuiConstants.TEXT_PRIMARY, false);
            }
            itemY += ITEM_HEIGHT;
        }
        
        if (nameField != null) nameField.render(context, mouseX, mouseY, delta);
        if (loadBtn != null) loadBtn.render(context, mouseX, mouseY, delta);
        if (saveBtn != null) saveBtn.render(context, mouseX, mouseY, delta);
        if (deleteBtn != null) deleteBtn.render(context, mouseX, mouseY, delta);
    }
    
    private void loadProfile() {
        String name = getSelectedProfileName();
        state.setCurrentProfile(name, isServerSelected());
        state.clearDirty();
        ToastNotification.success("Loaded: " + name);
    }
    
    private void saveProfile() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            ToastNotification.warning("Enter a profile name");
            return;
        }
        state.setCurrentProfile(name, false);
        state.clearDirty();
        ToastNotification.success("Saved: " + name);
    }
    
    private void deleteProfile() {
        if (profiles.size() <= 1) {
            ToastNotification.warning("Cannot delete last profile");
            return;
        }
        profiles.remove(selectedProfileIndex);
        if (selectedProfileIndex >= profiles.size()) {
            selectedProfileIndex = profiles.size() - 1;
        }
        ToastNotification.info("Deleted profile");
    }
    
    private String getSelectedProfileName() {
        if (selectedProfileIndex >= 0 && selectedProfileIndex < profiles.size()) {
            return profiles.get(selectedProfileIndex).name();
        }
        return "Default";
    }
    
    private boolean isServerSelected() {
        if (selectedProfileIndex >= 0 && selectedProfileIndex < profiles.size()) {
            return profiles.get(selectedProfileIndex).isServer();
        }
        return false;
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listX = GuiConstants.PADDING;
        int listY = GuiConstants.TAB_HEIGHT + GuiConstants.PADDING + GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        int listW = panelWidth - GuiConstants.PADDING * 2;
        
        if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + LIST_HEIGHT) {
            int clickedIndex = (int) ((mouseY - listY + listScrollOffset) / ITEM_HEIGHT);
            if (clickedIndex >= 0 && clickedIndex < profiles.size()) {
                selectedProfileIndex = clickedIndex;
                nameField.setText(getSelectedProfileName());
                return true;
            }
        }
        return false;
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        int maxScroll = Math.max(0, profiles.size() * ITEM_HEIGHT - LIST_HEIGHT);
        listScrollOffset = Math.max(0, Math.min(maxScroll, listScrollOffset - (int)(vAmount * ITEM_HEIGHT)));
        return true;
    }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> list = new ArrayList<>();
        if (nameField != null) list.add(nameField);
        if (loadBtn != null) list.add(loadBtn);
        if (saveBtn != null) list.add(saveBtn);
        if (deleteBtn != null) list.add(deleteBtn);
        return list;
    }
    
    public List<String> getProfileNames() {
        List<String> names = new ArrayList<>();
        for (ProfileEntry entry : profiles) {
            names.add(entry.name());
        }
        return names;
    }
}
'''

# =============================================================================
# BottomActionBar - matches existing API expected by FieldCustomizerScreen
# =============================================================================

BOTTOM_ACTION_BAR = '''package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditState.ProfileEntry;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.util.PresetRegistry;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Global bottom action bar.
 */
public class BottomActionBar {

    private final FieldEditState state;
    private final Consumer<String> onSave;
    private final Consumer<String> onRevert;
    private final Runnable onProfileChanged;

    private CyclingButtonWidget<String> presetDropdown;
    private CyclingButtonWidget<String> profileDropdown;
    private ButtonWidget saveBtn;
    private ButtonWidget revertBtn;
    
    private String currentPreset = "None";
    private int x, y, width;
    private int screenWidth, screenHeight;
    private boolean visible = true;

    public BottomActionBar(FieldEditState state, Consumer<String> onSave, Consumer<String> onRevert, Runnable onProfileChanged) {
        this.state = state;
        this.onSave = onSave;
        this.onRevert = onRevert;
        this.onProfileChanged = onProfileChanged;
    }

    public void init(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.width = screenWidth;
        this.x = 0;
        this.y = screenHeight - GuiConstants.BOTTOM_BAR_HEIGHT;

        int padding = GuiConstants.PADDING;
        int dropdownWidth = 150;
        int btnWidth = 70;

        List<String> presetNames = PresetRegistry.listPresets();
        presetDropdown = GuiWidgets.stringDropdown(
            padding, y + 4, dropdownWidth,
            "Preset", presetNames, currentPreset,
            "Select preset to apply",
            this::onPresetSelect
        );

        int revertX = width - padding - btnWidth;
        int saveX = revertX - padding - btnWidth;
        int profileX = saveX - padding - dropdownWidth;

        List<String> profileNames = state.getProfiles().stream().map(ProfileEntry::name).toList();
        if (profileNames.isEmpty()) {
            profileNames = List.of("Default");
        }
        
        profileDropdown = GuiWidgets.stringDropdown(
            profileX, y + 4, dropdownWidth,
            "Profile", profileNames, state.getCurrentProfileName(),
            "Select profile to load",
            this::onProfileSelect
        );

        saveBtn = GuiWidgets.button(saveX, y + 4, btnWidth, "Save", "Save current profile", this::doSave);
        revertBtn = GuiWidgets.button(revertX, y + 4, btnWidth, "Revert", "Revert to last saved", this::doRevert);

        updateButtonStates();
    }

    private void onPresetSelect(String presetName) {
        if (!"None".equals(presetName)) {
            PresetRegistry.applyPreset(state, presetName);
            currentPreset = presetName;
            state.markDirty();
            ToastNotification.success("Applied preset: " + presetName);
        }
        updateButtonStates();
    }

    private void onProfileSelect(String profileName) {
        if (!profileName.equals(state.getCurrentProfileName())) {
            boolean isServer = state.getProfiles().stream()
                .filter(p -> p.name().equals(profileName))
                .findFirst()
                .map(ProfileEntry::isServer)
                .orElse(false);
            state.setCurrentProfile(profileName, isServer);
            currentPreset = "None";
            updateButtonStates();
            onProfileChanged.run();
        }
    }

    private void doSave() {
        String name = state.getCurrentProfileName();
        onSave.accept(name);
        state.clearDirty();
        updateButtonStates();
    }

    private void doRevert() {
        onRevert.accept(state.getCurrentProfileName());
        state.clearDirty();
        currentPreset = "None";
        updateButtonStates();
    }

    public void updateButtonStates() {
        boolean dirty = state.isDirty();
        boolean serverProfile = state.isCurrentProfileServerSourced();
        if (saveBtn != null) {
            saveBtn.active = dirty;
            saveBtn.setMessage(net.minecraft.text.Text.literal(serverProfile ? "Save As" : "Save"));
        }
        if (revertBtn != null) {
            revertBtn.active = dirty && !serverProfile;
        }
    }

    public String getCurrentPreset() { return currentPreset; }
    public void resetPreset() {
        currentPreset = "None";
        if (presetDropdown != null) presetDropdown.setValue("None");
    }
    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean isVisible() { return visible; }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        context.fill(x, y, x + width, y + GuiConstants.BOTTOM_BAR_HEIGHT, GuiConstants.BG_SECONDARY);
        context.drawHorizontalLine(x, x + width - 1, y, GuiConstants.BORDER);
        
        if (presetDropdown != null) presetDropdown.render(context, mouseX, mouseY, delta);
        if (profileDropdown != null) profileDropdown.render(context, mouseX, mouseY, delta);
        if (saveBtn != null) saveBtn.render(context, mouseX, mouseY, delta);
        if (revertBtn != null) revertBtn.render(context, mouseX, mouseY, delta);
    }

    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> list = new ArrayList<>();
        if (presetDropdown != null) list.add(presetDropdown);
        if (profileDropdown != null) list.add(profileDropdown);
        if (saveBtn != null) list.add(saveBtn);
        if (revertBtn != null) list.add(revertBtn);
        return list;
    }

    public int getHeight() { return GuiConstants.BOTTOM_BAR_HEIGHT; }
}
'''

# =============================================================================
# Fix PresetRegistry.setFillMode
# =============================================================================

def fix_preset_registry():
    path = GUI_PATH / "util/PresetRegistry.java"
    if not path.exists():
        return
    
    content = path.read_text(encoding='utf-8')
    
    # Fix setFillMode - convert String to FillMode enum
    old = 'state.setFillMode(mergeData.get("fillMode").getAsString());'
    new = '''try {
                state.setFillMode(net.cyberpunk042.visual.fill.FillMode.valueOf(mergeData.get("fillMode").getAsString()));
            } catch (IllegalArgumentException ignored) {}'''
    
    if old in content:
        content = content.replace(old, new)
        path.write_text(content, encoding='utf-8')
        print("✅ Fixed PresetRegistry.setFillMode")


def main():
    print("=" * 60)
    print("REWRITING BROKEN FILES")
    print("=" * 60)
    
    # Write ProfilesPanel
    profiles_path = GUI_PATH / "panel/ProfilesPanel.java"
    profiles_path.write_text(PROFILES_PANEL, encoding='utf-8')
    print(f"✅ Rewrote: {profiles_path}")
    
    # Write BottomActionBar
    bar_path = GUI_PATH / "widget/BottomActionBar.java"
    bar_path.write_text(BOTTOM_ACTION_BAR, encoding='utf-8')
    print(f"✅ Rewrote: {bar_path}")
    
    # Fix PresetRegistry
    fix_preset_registry()
    
    print()
    print("=" * 60)
    print("Done! Run: ./gradlew build")
    print("=" * 60)


if __name__ == "__main__":
    main()

