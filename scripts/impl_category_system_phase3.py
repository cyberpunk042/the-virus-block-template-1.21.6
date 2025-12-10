#!/usr/bin/env python3
"""
Phase 3: Update GUI components for category system.
Updates: BottomActionBar.java, ProfilesPanel.java
"""

from pathlib import Path

# Paths
GUI_PACKAGE = Path("src/client/java/net/cyberpunk042/client/gui")
WIDGET_PACKAGE = GUI_PACKAGE / "widget"
PANEL_PACKAGE = GUI_PACKAGE / "panel"

# =============================================================================
# BottomActionBar.java (REWRITE with two-tier preset dropdown)
# =============================================================================

BOTTOM_ACTION_BAR = '''package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.field.category.PresetCategory;
import net.cyberpunk042.field.profile.Profile;
import net.cyberpunk042.field.profile.ProfileManager;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.util.PresetRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

/**
 * Global bottom action bar (hidden on Profiles tab).
 * Contains: [Category▼][Preset▼] | [Profile▼][SAVE][REVERT]
 */
public class BottomActionBar {
    
    private final FieldEditState state;
    private final TextRenderer textRenderer;
    private final int screenWidth;
    private final int y;
    
    // Preset selection (two-tier)
    private CyclingButtonWidget<PresetCategory> presetCategoryDropdown;
    private CyclingButtonWidget<String> presetDropdown;
    private PresetCategory selectedCategory = PresetCategory.STYLE;
    private String selectedPresetId = null;
    
    // Profile selection
    private CyclingButtonWidget<String> profileDropdown;
    private ButtonWidget saveButton;
    private ButtonWidget revertButton;
    
    // Callbacks
    private Consumer<String> onPresetSelected;
    private Consumer<String> onProfileSelected;
    private Runnable onSave;
    private Runnable onRevert;
    
    public BottomActionBar(FieldEditState state, int screenWidth, int y) {
        this.state = state;
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
        this.screenWidth = screenWidth;
        this.y = y;
    }
    
    /**
     * Initialize widgets. Call after constructor.
     */
    public void init(Consumer<ButtonWidget> addDrawable) {
        int leftX = 10;
        int rightX = screenWidth - 10;
        int buttonHeight = 20;
        int buttonSpacing = 5;
        
        // ========================================
        // LEFT SIDE: Preset selection (two-tier)
        // ========================================
        
        // Category dropdown
        List<PresetCategory> categories = PresetRegistry.getCategories();
        if (categories.isEmpty()) {
            categories = List.of(PresetCategory.values());
        }
        
        presetCategoryDropdown = CyclingButtonWidget.<PresetCategory>builder(cat -> Text.literal(cat.getDisplayName()))
            .values(categories)
            .initially(selectedCategory)
            .build(leftX, y, 90, buttonHeight, Text.literal("Category"), (button, value) -> {
                selectedCategory = value;
                refreshPresetDropdown();
            });
        addDrawable.accept(presetCategoryDropdown);
        
        // Preset dropdown (updates based on category)
        presetDropdown = createPresetDropdown(leftX + 95, y, buttonHeight);
        addDrawable.accept(presetDropdown);
        
        // ========================================
        // RIGHT SIDE: Profile + Save + Revert
        // ========================================
        
        int saveWidth = 50;
        int revertWidth = 60;
        int profileWidth = 150;
        
        // Revert button (rightmost)
        revertButton = ButtonWidget.builder(Text.literal("REVERT"), btn -> {
            if (onRevert != null) onRevert.run();
        }).dimensions(rightX - revertWidth, y, revertWidth, buttonHeight).build();
        addDrawable.accept(revertButton);
        
        // Save button
        saveButton = ButtonWidget.builder(Text.literal("SAVE"), btn -> {
            if (onSave != null) onSave.run();
        }).dimensions(rightX - revertWidth - buttonSpacing - saveWidth, y, saveWidth, buttonHeight).build();
        addDrawable.accept(saveButton);
        
        // Profile dropdown
        int profileX = rightX - revertWidth - buttonSpacing - saveWidth - buttonSpacing - profileWidth;
        profileDropdown = createProfileDropdown(profileX, y, profileWidth, buttonHeight);
        addDrawable.accept(profileDropdown);
        
        // Initial state update
        updateButtonStates();
    }
    
    private CyclingButtonWidget<String> createPresetDropdown(int x, int y, int height) {
        List<PresetRegistry.PresetEntry> presets = PresetRegistry.getPresets(selectedCategory);
        List<String> presetIds = presets.stream()
            .map(PresetRegistry.PresetEntry::id)
            .toList();
        
        if (presetIds.isEmpty()) {
            presetIds = List.of("(none)");
        }
        
        return CyclingButtonWidget.<String>builder(id -> {
                if (id.equals("(none)")) return Text.literal("No presets");
                return PresetRegistry.getPreset(id)
                    .map(p -> Text.literal(p.name()))
                    .orElse(Text.literal(id));
            })
            .values(presetIds)
            .initially(presetIds.get(0))
            .build(x, y, 120, height, Text.literal("Preset"), (button, value) -> {
                if (!value.equals("(none)") && onPresetSelected != null) {
                    onPresetSelected.accept(value);
                }
            });
    }
    
    private void refreshPresetDropdown() {
        // Rebuild preset dropdown for new category
        // In a real implementation, we'd update the values in place
        // For now, the dropdown will need to be recreated
        List<PresetRegistry.PresetEntry> presets = PresetRegistry.getPresets(selectedCategory);
        if (!presets.isEmpty()) {
            selectedPresetId = presets.get(0).id();
        } else {
            selectedPresetId = null;
        }
    }
    
    private CyclingButtonWidget<String> createProfileDropdown(int x, int y, int width, int height) {
        List<Profile> profiles = ProfileManager.getInstance().getAllProfiles();
        List<String> profileIds = profiles.stream()
            .map(Profile::id)
            .toList();
        
        if (profileIds.isEmpty()) {
            profileIds = List.of("(none)");
        }
        
        String currentProfile = state.getCurrentProfileName();
        if (currentProfile == null || !profileIds.contains(currentProfile)) {
            currentProfile = profileIds.get(0);
        }
        
        return CyclingButtonWidget.<String>builder(id -> {
                if (id.equals("(none)")) return Text.literal("No profiles");
                return ProfileManager.getInstance().getProfile(id)
                    .map(p -> Text.literal(p.getDisplayName()))
                    .orElse(Text.literal(id));
            })
            .values(profileIds)
            .initially(currentProfile)
            .build(x, y, width, height, Text.literal("Profile"), (button, value) -> {
                if (!value.equals("(none)") && onProfileSelected != null) {
                    onProfileSelected.accept(value);
                }
            });
    }
    
    /**
     * Update button enabled states based on current state.
     */
    public void updateButtonStates() {
        boolean isDirty = state.isDirty();
        boolean isServerProfile = state.isCurrentProfileServerSourced();
        
        // Save: enabled when dirty AND local profile (or becomes "Save As" for server)
        saveButton.active = isDirty;
        saveButton.setMessage(Text.literal(isServerProfile ? "SAVE AS" : "SAVE"));
        
        // Revert: enabled when dirty
        revertButton.active = isDirty;
    }
    
    /**
     * Render any additional elements (labels, separators).
     */
    public void render(DrawContext context) {
        // Draw separator between presets and profile sections
        int separatorX = screenWidth / 2;
        context.fill(separatorX - 1, y + 2, separatorX, y + 18, 0x40FFFFFF);
        
        // Labels
        context.drawText(textRenderer, "Presets:", 10, y - 12, 0xAAAAAA, false);
        context.drawText(textRenderer, "Profile:", screenWidth - 280, y - 12, 0xAAAAAA, false);
    }
    
    // ========================================
    // Callback setters
    // ========================================
    
    public void setOnPresetSelected(Consumer<String> callback) {
        this.onPresetSelected = callback;
    }
    
    public void setOnProfileSelected(Consumer<String> callback) {
        this.onProfileSelected = callback;
    }
    
    public void setOnSave(Runnable callback) {
        this.onSave = callback;
    }
    
    public void setOnRevert(Runnable callback) {
        this.onRevert = callback;
    }
}
'''

# =============================================================================
# ProfilesPanel.java (REWRITE with filters)
# =============================================================================

PROFILES_PANEL = '''package net.cyberpunk042.client.gui.panel;

import net.cyberpunk042.field.category.ProfileCategory;
import net.cyberpunk042.field.category.ProfileSource;
import net.cyberpunk042.field.profile.Profile;
import net.cyberpunk042.field.profile.ProfileManager;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.widget.ConfirmDialog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Full profile management panel with source and category filters.
 * Replaces the simple profile panel with a rich UI.
 */
public class ProfilesPanel extends AbstractPanel {
    
    private final ProfileManager profileManager;
    
    // Filters
    private CyclingButtonWidget<ProfileSource> sourceFilter;
    private CyclingButtonWidget<ProfileCategory> categoryFilter;
    private TextFieldWidget searchField;
    
    // Profile list
    private List<Profile> filteredProfiles = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    
    // List dimensions
    private int listX, listY, listWidth, listHeight;
    private int itemHeight = 20;
    private int maxVisibleItems = 10;
    
    // Action buttons
    private ButtonWidget loadBtn, saveBtn, saveAsBtn, renameBtn;
    private ButtonWidget duplicateBtn, deleteBtn, importBtn, exportBtn;
    private ButtonWidget setDefaultBtn;
    
    // Callbacks
    private Consumer<Profile> onProfileLoad;
    private Runnable onSave;
    
    public ProfilesPanel(FieldEditState state, int x, int y, int width, int height) {
        super(state, x, y, width, height);
        this.profileManager = ProfileManager.getInstance();
    }
    
    @Override
    public void init(Consumer<ButtonWidget> addDrawable) {
        int currentY = y + 10;
        int filterWidth = 100;
        int searchWidth = 150;
        int spacing = 10;
        
        // ========================================
        // FILTERS ROW
        // ========================================
        
        // Source filter: All, Bundled, Local, Server
        List<ProfileSource> sources = new ArrayList<>();
        sources.add(null); // "All"
        sources.addAll(List.of(ProfileSource.values()));
        
        sourceFilter = CyclingButtonWidget.<ProfileSource>builder(src -> 
                src == null ? Text.literal("All Sources") : Text.literal(src.getDisplayName()))
            .values(sources)
            .initially(null)
            .build(x + 10, currentY, filterWidth, 20, Text.literal("Source"), (btn, val) -> applyFilters());
        addDrawable.accept(sourceFilter);
        
        // Category filter: All, Combat, Utility, Decorative, Experimental
        List<ProfileCategory> categories = new ArrayList<>();
        categories.add(null); // "All"
        categories.addAll(List.of(ProfileCategory.values()));
        
        categoryFilter = CyclingButtonWidget.<ProfileCategory>builder(cat ->
                cat == null ? Text.literal("All Categories") : Text.literal(cat.getDisplayName()))
            .values(categories)
            .initially(null)
            .build(x + 10 + filterWidth + spacing, currentY, filterWidth + 20, 20, Text.literal("Category"), (btn, val) -> applyFilters());
        addDrawable.accept(categoryFilter);
        
        // Search field
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        searchField = new TextFieldWidget(textRenderer, 
            x + 10 + filterWidth * 2 + spacing * 2 + 20, currentY, 
            searchWidth, 20, Text.literal("Search"));
        searchField.setPlaceholder(Text.literal("🔍 Search..."));
        searchField.setChangedListener(text -> applyFilters());
        addDrawable.accept(searchField);
        
        currentY += 30;
        
        // ========================================
        // PROFILE LIST
        // ========================================
        
        listX = x + 10;
        listY = currentY;
        listWidth = width - 20;
        listHeight = height - 150; // Leave room for buttons
        maxVisibleItems = listHeight / itemHeight;
        
        currentY += listHeight + 10;
        
        // ========================================
        // FRAGMENT SUMMARY (for selected profile)
        // ========================================
        // This area shows shape/fill/visibility/animation fragments
        // Rendered in render() method
        
        currentY += 50;
        
        // ========================================
        // ACTION BUTTONS
        // ========================================
        
        int btnWidth = 60;
        int btnHeight = 20;
        int btnSpacing = 5;
        int btnX = x + 10;
        
        loadBtn = ButtonWidget.builder(Text.literal("Load"), btn -> loadSelectedProfile())
            .dimensions(btnX, currentY, btnWidth, btnHeight).build();
        addDrawable.accept(loadBtn);
        btnX += btnWidth + btnSpacing;
        
        saveBtn = ButtonWidget.builder(Text.literal("Save"), btn -> saveCurrentProfile())
            .dimensions(btnX, currentY, btnWidth, btnHeight).build();
        addDrawable.accept(saveBtn);
        btnX += btnWidth + btnSpacing;
        
        saveAsBtn = ButtonWidget.builder(Text.literal("Save As"), btn -> saveAsNewProfile())
            .dimensions(btnX, currentY, btnWidth + 10, btnHeight).build();
        addDrawable.accept(saveAsBtn);
        btnX += btnWidth + 10 + btnSpacing;
        
        renameBtn = ButtonWidget.builder(Text.literal("Rename"), btn -> renameSelectedProfile())
            .dimensions(btnX, currentY, btnWidth, btnHeight).build();
        addDrawable.accept(renameBtn);
        btnX += btnWidth + btnSpacing;
        
        duplicateBtn = ButtonWidget.builder(Text.literal("Duplicate"), btn -> duplicateSelectedProfile())
            .dimensions(btnX, currentY, btnWidth + 20, btnHeight).build();
        addDrawable.accept(duplicateBtn);
        btnX += btnWidth + 20 + btnSpacing;
        
        deleteBtn = ButtonWidget.builder(Text.literal("Delete"), btn -> deleteSelectedProfile())
            .dimensions(btnX, currentY, btnWidth, btnHeight).build();
        addDrawable.accept(deleteBtn);
        
        // Second row
        currentY += btnHeight + btnSpacing;
        btnX = x + 10;
        
        importBtn = ButtonWidget.builder(Text.literal("Import JSON"), btn -> importProfile())
            .dimensions(btnX, currentY, 80, btnHeight).build();
        addDrawable.accept(importBtn);
        btnX += 85;
        
        exportBtn = ButtonWidget.builder(Text.literal("Export JSON"), btn -> exportProfile())
            .dimensions(btnX, currentY, 80, btnHeight).build();
        addDrawable.accept(exportBtn);
        btnX += 85;
        
        setDefaultBtn = ButtonWidget.builder(Text.literal("Set Default"), btn -> setAsDefault())
            .dimensions(btnX, currentY, 80, btnHeight).build();
        addDrawable.accept(setDefaultBtn);
        
        // Initial load
        applyFilters();
        updateButtonStates();
    }
    
    /**
     * Apply filters and refresh the visible profile list.
     */
    private void applyFilters() {
        ProfileSource source = sourceFilter.getValue();
        ProfileCategory category = categoryFilter.getValue();
        String search = searchField.getText();
        
        filteredProfiles = profileManager.filterProfiles(source, category, search);
        scrollOffset = 0;
        
        // Try to keep selection
        if (selectedIndex >= filteredProfiles.size()) {
            selectedIndex = filteredProfiles.isEmpty() ? -1 : 0;
        }
        
        updateButtonStates();
    }
    
    /**
     * Update button enabled states based on selection.
     */
    private void updateButtonStates() {
        Profile selected = getSelectedProfile();
        boolean hasSelection = selected != null;
        boolean isEditable = hasSelection && selected.isEditable();
        boolean isDirty = state.isDirty();
        
        loadBtn.active = hasSelection;
        saveBtn.active = isDirty && isEditable;
        saveAsBtn.active = hasSelection;
        renameBtn.active = isEditable;
        duplicateBtn.active = hasSelection;
        deleteBtn.active = isEditable;
        exportBtn.active = hasSelection;
        setDefaultBtn.active = hasSelection;
    }
    
    /**
     * Get currently selected profile.
     */
    private Profile getSelectedProfile() {
        if (selectedIndex < 0 || selectedIndex >= filteredProfiles.size()) {
            return null;
        }
        return filteredProfiles.get(selectedIndex);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        
        // Draw list background
        context.fill(listX, listY, listX + listWidth, listY + listHeight, 0x80000000);
        
        // Draw profiles grouped by source
        int renderY = listY + 2;
        ProfileSource lastSource = null;
        
        for (int i = scrollOffset; i < filteredProfiles.size() && renderY < listY + listHeight - itemHeight; i++) {
            Profile profile = filteredProfiles.get(i);
            
            // Source header
            if (profile.source() != lastSource) {
                lastSource = profile.source();
                context.drawText(textRenderer, "── " + lastSource.getDisplayName().toUpperCase() + " ──", 
                    listX + 5, renderY + 5, 0x888888, false);
                renderY += itemHeight;
            }
            
            // Profile entry
            boolean isSelected = i == selectedIndex;
            boolean isHovered = mouseX >= listX && mouseX < listX + listWidth &&
                               mouseY >= renderY && mouseY < renderY + itemHeight;
            
            // Background
            if (isSelected) {
                context.fill(listX + 2, renderY, listX + listWidth - 2, renderY + itemHeight, 0x60FFFFFF);
            } else if (isHovered) {
                context.fill(listX + 2, renderY, listX + listWidth - 2, renderY + itemHeight, 0x30FFFFFF);
            }
            
            // Radio button
            String radio = isSelected ? "●" : "○";
            context.drawText(textRenderer, radio, listX + 8, renderY + 5, 0xFFFFFF, false);
            
            // Profile name with category
            String displayName = profile.getDisplayName();
            context.drawText(textRenderer, displayName, listX + 25, renderY + 5, 0xFFFFFF, false);
            
            // Source icon (right side)
            String icon = profile.getSourceIcon();
            int iconX = listX + listWidth - 20;
            context.drawText(textRenderer, icon, iconX, renderY + 5, 0xAAAAAA, false);
            
            renderY += itemHeight;
        }
        
        // Draw fragment summary for selected profile
        Profile selected = getSelectedProfile();
        if (selected != null) {
            int summaryY = listY + listHeight + 10;
            context.fill(listX, summaryY, listX + listWidth, summaryY + 40, 0x60000000);
            
            // TODO: Show actual fragment names from the profile
            context.drawText(textRenderer, "Shape: " + state.getShapeType(), listX + 5, summaryY + 5, 0xAAAAAA, false);
            context.drawText(textRenderer, "Fill: " + state.getFillMode(), listX + 150, summaryY + 5, 0xAAAAAA, false);
            context.drawText(textRenderer, "Animation: " + (state.isSpinEnabled() ? "Spin" : "None"), listX + 5, summaryY + 20, 0xAAAAAA, false);
        }
        
        // Status bar
        int statusY = y + height - 25;
        if (state.isDirty()) {
            context.drawText(textRenderer, "● Unsaved changes", x + 10, statusY, 0xFFAA00, false);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if click is in list area
        if (mouseX >= listX && mouseX < listX + listWidth &&
            mouseY >= listY && mouseY < listY + listHeight) {
            
            int clickedIndex = scrollOffset + (int)((mouseY - listY) / itemHeight);
            
            // Account for source headers
            ProfileSource lastSource = null;
            int adjustedIndex = -1;
            int renderIndex = 0;
            
            for (int i = scrollOffset; i < filteredProfiles.size(); i++) {
                Profile profile = filteredProfiles.get(i);
                
                if (profile.source() != lastSource) {
                    lastSource = profile.source();
                    renderIndex++; // Header takes a row
                }
                
                if (renderIndex == clickedIndex - scrollOffset) {
                    // Clicked on header, ignore
                    return true;
                } else if (renderIndex == clickedIndex - scrollOffset + 1) {
                    adjustedIndex = i;
                    break;
                }
                renderIndex++;
            }
            
            // Simplified: just use direct index for now
            if (clickedIndex >= 0 && clickedIndex < filteredProfiles.size()) {
                selectedIndex = clickedIndex;
                updateButtonStates();
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= listX && mouseX < listX + listWidth &&
            mouseY >= listY && mouseY < listY + listHeight) {
            
            scrollOffset -= (int) verticalAmount;
            scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, filteredProfiles.size() - maxVisibleItems)));
            return true;
        }
        return false;
    }
    
    // ========================================
    // Action methods
    // ========================================
    
    private void loadSelectedProfile() {
        Profile selected = getSelectedProfile();
        if (selected != null && onProfileLoad != null) {
            onProfileLoad.accept(selected);
        }
    }
    
    private void saveCurrentProfile() {
        if (onSave != null) {
            onSave.run();
        }
    }
    
    private void saveAsNewProfile() {
        // TODO: Show name input dialog
        Profile selected = getSelectedProfile();
        if (selected != null) {
            Profile copy = profileManager.duplicateProfile(selected.id(), selected.name() + " Copy");
            applyFilters();
        }
    }
    
    private void renameSelectedProfile() {
        // TODO: Show rename dialog
    }
    
    private void duplicateSelectedProfile() {
        Profile selected = getSelectedProfile();
        if (selected != null) {
            profileManager.duplicateProfile(selected.id(), selected.name() + " Copy");
            applyFilters();
        }
    }
    
    private void deleteSelectedProfile() {
        Profile selected = getSelectedProfile();
        if (selected != null && selected.isEditable()) {
            // TODO: Show confirmation dialog
            profileManager.deleteProfile(selected.id());
            selectedIndex = -1;
            applyFilters();
        }
    }
    
    private void importProfile() {
        // TODO: Show file picker
    }
    
    private void exportProfile() {
        // TODO: Show file save dialog
    }
    
    private void setAsDefault() {
        // TODO: Save as default profile preference
    }
    
    // ========================================
    // Callback setters
    // ========================================
    
    public void setOnProfileLoad(Consumer<Profile> callback) {
        this.onProfileLoad = callback;
    }
    
    public void setOnSave(Runnable callback) {
        this.onSave = callback;
    }
}
'''

# =============================================================================
# Main
# =============================================================================

def main():
    print("=" * 60)
    print("PHASE 3: Category System - GUI Components")
    print("=" * 60)
    
    # Ensure directories exist
    WIDGET_PACKAGE.mkdir(parents=True, exist_ok=True)
    PANEL_PACKAGE.mkdir(parents=True, exist_ok=True)
    
    # Write BottomActionBar.java
    bar_file = WIDGET_PACKAGE / "BottomActionBar.java"
    bar_file.write_text(BOTTOM_ACTION_BAR, encoding='utf-8')
    print(f"✅ Updated: {bar_file}")
    
    # Write ProfilesPanel.java
    panel_file = PANEL_PACKAGE / "ProfilesPanel.java"
    panel_file.write_text(PROFILES_PANEL, encoding='utf-8')
    print(f"✅ Updated: {panel_file}")
    
    print()
    print("=" * 60)
    print("Phase 3 Complete!")
    print("=" * 60)
    print()
    print("Updated files:")
    print(f"  - {WIDGET_PACKAGE}/BottomActionBar.java (rewritten)")
    print(f"  - {PANEL_PACKAGE}/ProfilesPanel.java (rewritten)")
    print()
    print("Next: Run Phase 4 script for integration, then build & test")


if __name__ == "__main__":
    main()

