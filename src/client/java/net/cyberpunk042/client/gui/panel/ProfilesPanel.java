package net.cyberpunk042.client.gui.panel;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditState.ProfileEntry;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ToastNotification;
import net.cyberpunk042.field.category.ProfileCategory;
import net.cyberpunk042.field.category.ProfileSource;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.cyberpunk042.client.gui.widget.ConfirmDialog;
import net.cyberpunk042.network.gui.ProfileSaveC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * G91-G100: Profiles management panel with source and category filters.
 */
public class ProfilesPanel extends AbstractPanel {
    
    private final List<ProfileEntry> allProfiles;
    private List<ProfileEntry> filteredProfiles;
    private int selectedProfileIndex = 0;
    private int listScrollOffset = 0;
    
    // Filter widgets
    private CyclingButtonWidget<String> sourceFilter;  // All, Local, Server, Bundled
    private CyclingButtonWidget<String> categoryFilter; // All, Combat, Utility, Decorative, Experimental
    private TextFieldWidget searchField;
    
    // Selected filter values (null = All)
    private String selectedSource = null;
    private String selectedCategory = null;
    
    // Action buttons
    private ButtonWidget loadBtn, saveBtn, saveAsBtn, deleteBtn;
    private ButtonWidget duplicateBtn, renameBtn;
    private ButtonWidget saveToServerBtn;  // OP only
    private TextFieldWidget nameField;
    
    private static final int FILTER_ROW_HEIGHT = 28;
    private static final int LIST_HEIGHT = 140;
    private static final int ITEM_HEIGHT = 22;
    private static final int SUMMARY_HEIGHT = 60;
    
    public ProfilesPanel(Screen parent, FieldEditState state) {
        super(parent, state);
        this.allProfiles = state.getProfiles();
        this.filteredProfiles = new ArrayList<>(allProfiles);
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        
        int x = GuiConstants.PADDING;
        int y = GuiConstants.TAB_HEIGHT + GuiConstants.PADDING;
        int w = width - GuiConstants.PADDING * 2;
        int filterWidth = 90;
        int searchWidth = w - filterWidth * 2 - GuiConstants.PADDING * 2;
        
        // ═══════════════════════════════════════════════════════════════
        // FILTER ROW: [Source▼] [Category▼] [🔍 Search...]
        // ═══════════════════════════════════════════════════════════════
        
        List<String> sources = List.of("All", "Local", "Server", "Bundled");
        sourceFilter = CyclingButtonWidget.<String>builder(Text::literal)
            .values(sources)
            .initially("All")
            .build(x, y, filterWidth, 20, Text.literal("Source"),
                (btn, val) -> {
                    selectedSource = "All".equals(val) ? null : val;
                    applyFilters();
                });
        
        List<String> categories = List.of("All", "Combat", "Utility", "Decorative", "Experimental");
        categoryFilter = CyclingButtonWidget.<String>builder(Text::literal)
            .values(categories)
            .initially("All")
            .build(x + filterWidth + GuiConstants.PADDING, y, filterWidth, 20, Text.literal("Category"),
                (btn, val) -> {
                    selectedCategory = "All".equals(val) ? null : val;
                    applyFilters();
                });
        
        searchField = new TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            x + filterWidth * 2 + GuiConstants.PADDING * 2, y, searchWidth, 20,
            Text.literal("Search")
        );
        searchField.setPlaceholder(Text.literal("🔍 Search profiles..."));
        searchField.setChangedListener(text -> applyFilters());
        
        y += FILTER_ROW_HEIGHT;
        
        // ═══════════════════════════════════════════════════════════════
        // PROFILE LIST (left side)
        // ═══════════════════════════════════════════════════════════════
        int listW = (w - GuiConstants.PADDING) / 2;
        // List is rendered in render(), position stored for mouse handling
        
        // ═══════════════════════════════════════════════════════════════
        // NAME FIELD (below list, full width of left side)
        // ═══════════════════════════════════════════════════════════════
        int nameFieldY = y + LIST_HEIGHT + GuiConstants.PADDING;
        nameField = new TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            x, nameFieldY, listW, GuiConstants.WIDGET_HEIGHT,
            Text.literal("Profile Name")
        );
        nameField.setMaxLength(32);
        nameField.setText(getSelectedProfileName());
        
        // ═══════════════════════════════════════════════════════════════
        // ACTION BUTTONS (below name field)
        // ═══════════════════════════════════════════════════════════════
        int btnY = nameFieldY + GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        int btnW = (listW - GuiConstants.PADDING * 2) / 3;
        
        loadBtn = GuiWidgets.button(x, btnY, btnW, "Load", "Load selected profile", this::loadProfile);
        saveBtn = GuiWidgets.button(x + btnW + GuiConstants.PADDING, btnY, btnW, "Save", "Save changes", this::saveProfile);
        deleteBtn = GuiWidgets.button(x + (btnW + GuiConstants.PADDING) * 2, btnY, btnW, "Delete", "Delete profile", this::deleteProfile);
        
        btnY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        saveAsBtn = GuiWidgets.button(x, btnY, btnW, "Save As", "Save as new profile", this::saveAsProfile);
        duplicateBtn = GuiWidgets.button(x + btnW + GuiConstants.PADDING, btnY, btnW, "Duplicate", "Copy profile", this::duplicateProfile);
        renameBtn = GuiWidgets.button(x + (btnW + GuiConstants.PADDING) * 2, btnY, btnW, "Rename", "Rename profile", this::renameProfile);
        
        // OP-only: Save to Server button (third row)
        btnY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        saveToServerBtn = GuiWidgets.button(x, btnY, listW, "⚡ Save to Server", "Save as server profile (OP only)", this::promptSaveToServer);
        
        updateButtonStates();
    }
    
    private void applyFilters() {
        String searchText = searchField != null ? searchField.getText().toLowerCase() : "";
        
        filteredProfiles = allProfiles.stream()
            .filter(p -> {
                // Source filter
                if (selectedSource != null) {
                    boolean isLocal = !p.isServer();
                    if ("Local".equals(selectedSource) && !isLocal) return false;
                    if ("Server".equals(selectedSource) && isLocal) return false;
                    // "Bundled" would need ProfileSource info - for now treat as Local
                }
                return true;
            })
            .filter(p -> {
                // Category filter - would need ProfileCategory info on ProfileEntry
                // For now, all profiles pass category filter
                return true;
            })
            .filter(p -> {
                // Search filter
                if (searchText.isEmpty()) return true;
                return p.name().toLowerCase().contains(searchText);
            })
            .collect(Collectors.toList());
        
        // Clamp selection
        if (selectedProfileIndex >= filteredProfiles.size()) {
            selectedProfileIndex = Math.max(0, filteredProfiles.size() - 1);
        }
        listScrollOffset = 0;
        
        if (nameField != null) {
            nameField.setText(getSelectedProfileName());
        }
        updateButtonStates();
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
        
        int x = GuiConstants.PADDING;
        int y = panelY + GuiConstants.PADDING + FILTER_ROW_HEIGHT;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int listW = (w - GuiConstants.PADDING) / 2;
        int summaryX = x + listW + GuiConstants.PADDING;
        int summaryW = w - listW - GuiConstants.PADDING;
        
        // ═══════════════════════════════════════════════════════════════
        // PROFILE LIST (left side)
        // ═══════════════════════════════════════════════════════════════
        context.fill(x, y, x + listW, y + LIST_HEIGHT, 0xFF1A1A1A);
        context.drawBorder(x, y, listW, LIST_HEIGHT, GuiConstants.BORDER);
        
        // List header
        context.drawText(textRenderer, "Profiles (" + filteredProfiles.size() + ")", x + 6, y + 4, GuiConstants.TEXT_SECONDARY, false);
        
        int itemY = y + 18 - listScrollOffset;
        for (int i = 0; i < filteredProfiles.size(); i++) {
            if (itemY >= y + 16 && itemY + ITEM_HEIGHT <= y + LIST_HEIGHT) {
                boolean selected = i == selectedProfileIndex;
                boolean hovered = mouseX >= x && mouseX < x + listW && 
                                  mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
                
                // Background
                if (selected) {
                    context.fill(x + 2, itemY, x + listW - 2, itemY + ITEM_HEIGHT - 2, GuiConstants.ACCENT);
                } else if (hovered) {
                    context.fill(x + 2, itemY, x + listW - 2, itemY + ITEM_HEIGHT - 2, 0x40FFFFFF);
                }
                
                ProfileEntry entry = filteredProfiles.get(i);
                
                // Source icon
                String icon = entry.isServer() ? "🔒" : "✎";
                context.drawText(textRenderer, icon, x + 6, itemY + 5, GuiConstants.TEXT_SECONDARY, false);
                
                // Profile name
                String name = entry.name();
                int textColor = selected ? 0xFFFFFFFF : GuiConstants.TEXT_PRIMARY;
                context.drawText(textRenderer, name, x + 22, itemY + 5, textColor, false);
                
                // Source label (right side)
                String sourceLabel = entry.isServer() ? "server" : "local";
                int labelWidth = textRenderer.getWidth(sourceLabel);
                context.drawText(textRenderer, sourceLabel, x + listW - labelWidth - 8, itemY + 5, GuiConstants.TEXT_SECONDARY, false);
            }
            itemY += ITEM_HEIGHT;
        }
        
        // ═══════════════════════════════════════════════════════════════
        // CATEGORY SUMMARY (right side)
        // ═══════════════════════════════════════════════════════════════
        context.fill(summaryX, y, summaryX + summaryW, y + SUMMARY_HEIGHT, GuiConstants.BG_SECONDARY);
        context.drawBorder(summaryX, y, summaryW, SUMMARY_HEIGHT, GuiConstants.BORDER);
        context.drawText(textRenderer, "Current Configuration", summaryX + 6, y + 4, GuiConstants.TEXT_PRIMARY, false);
        
        int lineY = y + 18;
        lineY = drawSummaryLine(context, textRenderer, summaryX + 6, lineY, "Shape", state.getCurrentShapeFragmentName());
        lineY = drawSummaryLine(context, textRenderer, summaryX + 6, lineY, "Fill", state.getCurrentFillFragmentName());
        lineY = drawSummaryLine(context, textRenderer, summaryX + 6, lineY, "Animation", state.getCurrentAnimationFragmentName());
        
        // Stats below summary
        int statsY = y + SUMMARY_HEIGHT + GuiConstants.PADDING;
        context.fill(summaryX, statsY, summaryX + summaryW, statsY + 60, GuiConstants.BG_SECONDARY);
        context.drawBorder(summaryX, statsY, summaryW, 60, GuiConstants.BORDER);
        context.drawText(textRenderer, "Stats", summaryX + 6, statsY + 4, GuiConstants.TEXT_PRIMARY, false);
        
        int statLineY = statsY + 18;
        context.drawText(textRenderer, "latSteps: " + state.getSphereLatSteps(), summaryX + 6, statLineY, GuiConstants.TEXT_SECONDARY, false);
        context.drawText(textRenderer, "lonSteps: " + state.getSphereLonSteps(), summaryX + 80, statLineY, GuiConstants.TEXT_SECONDARY, false);
        statLineY += 12;
        context.drawText(textRenderer, "radius: " + String.format("%.1f", state.getRadius()), summaryX + 6, statLineY, GuiConstants.TEXT_SECONDARY, false);
        context.drawText(textRenderer, "alpha: " + String.format("%.2f", state.getAlpha()), summaryX + 80, statLineY, GuiConstants.TEXT_SECONDARY, false);
        
        // Render filter widgets
        if (sourceFilter != null) sourceFilter.render(context, mouseX, mouseY, delta);
        if (categoryFilter != null) categoryFilter.render(context, mouseX, mouseY, delta);
        if (searchField != null) searchField.render(context, mouseX, mouseY, delta);
        
        // Render other widgets
        if (nameField != null) nameField.render(context, mouseX, mouseY, delta);
        if (loadBtn != null) loadBtn.render(context, mouseX, mouseY, delta);
        if (saveBtn != null) saveBtn.render(context, mouseX, mouseY, delta);
        if (saveAsBtn != null) saveAsBtn.render(context, mouseX, mouseY, delta);
        if (deleteBtn != null) deleteBtn.render(context, mouseX, mouseY, delta);
        if (duplicateBtn != null) duplicateBtn.render(context, mouseX, mouseY, delta);
        if (renameBtn != null) renameBtn.render(context, mouseX, mouseY, delta);
        if (saveToServerBtn != null && saveToServerBtn.visible) saveToServerBtn.render(context, mouseX, mouseY, delta);
        
        // Dirty indicator
        if (state.isDirty()) {
            context.drawText(textRenderer, "● Unsaved changes", x, panelHeight - 20, GuiConstants.WARNING, false);
        }
    }
    
    private int drawSummaryLine(DrawContext ctx, net.minecraft.client.font.TextRenderer renderer, int x, int y, String label, String value) {
        ctx.drawText(renderer, label + ":", x, y, GuiConstants.TEXT_SECONDARY, false);
        ctx.drawText(renderer, value, x + 70, y, GuiConstants.TEXT_PRIMARY, false);
        return y + 12;
    }
    
    private void loadProfile() {
        ProfileEntry entry = getSelectedProfile();
        if (entry == null) return;
        
        state.setCurrentProfile(entry.name(), entry.isServer());
        state.clearDirty();
        state.saveProfileSnapshot();
        ToastNotification.success("Loaded: " + entry.name());
        Logging.GUI.topic("profile").info("Loaded profile: {}", entry.name());
    }
    
    private void saveProfile() {
        ProfileEntry entry = getSelectedProfile();
        if (entry == null || entry.isServer()) {
            ToastNotification.warning("Cannot save server profile - use Save As");
            return;
        }
        
        state.clearDirty();
        ToastNotification.success("Saved: " + entry.name());
    }
    
    private void saveAsProfile() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            ToastNotification.warning("Enter a profile name");
            return;
        }
        
        // Add new profile entry
        boolean exists = allProfiles.stream().anyMatch(p -> p.name().equals(name));
        if (!exists) {
            allProfiles.add(new ProfileEntry(name, false));
            applyFilters();
        }
        
        state.setCurrentProfile(name, false);
        state.clearDirty();
        ToastNotification.success("Saved as: " + name);
    }
    
    private void deleteProfile() {
        ProfileEntry entry = getSelectedProfile();
        if (entry == null) return;
        
        if (entry.isServer()) {
            ToastNotification.warning("Cannot delete server profile");
            return;
        }
        
        if (allProfiles.size() <= 1) {
            ToastNotification.warning("Cannot delete last profile");
            return;
        }
        
        String name = entry.name();
        allProfiles.remove(entry);
        applyFilters();
        ToastNotification.info("Deleted: " + name);
    }
    
    private void duplicateProfile() {
        ProfileEntry entry = getSelectedProfile();
        if (entry == null) return;
        
        String newName = entry.name() + " (Copy)";
        allProfiles.add(new ProfileEntry(newName, false));
        applyFilters();
        
        // Select the new profile
        for (int i = 0; i < filteredProfiles.size(); i++) {
            if (filteredProfiles.get(i).name().equals(newName)) {
                selectedProfileIndex = i;
                nameField.setText(newName);
                break;
            }
        }
        
        ToastNotification.info("Duplicated: " + entry.name());
    }
    
    private void renameProfile() {
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
            "This will save '" + name + "' as a server profile. All players will be able to use this profile.",
            () -> doSaveToServer(name)
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
    
    private ProfileEntry getSelectedProfile() {
        if (selectedProfileIndex >= 0 && selectedProfileIndex < filteredProfiles.size()) {
            return filteredProfiles.get(selectedProfileIndex);
        }
        return null;
    }
    
    private String getSelectedProfileName() {
        ProfileEntry entry = getSelectedProfile();
        return entry != null ? entry.name() : "Default";
    }
    
    private void updateButtonStates() {
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
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = GuiConstants.PADDING;
        int y = GuiConstants.TAB_HEIGHT + GuiConstants.PADDING + FILTER_ROW_HEIGHT;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int listW = (w - GuiConstants.PADDING) / 2;
        
        if (mouseX >= x && mouseX < x + listW && mouseY >= y + 16 && mouseY < y + LIST_HEIGHT) {
            int clickedIndex = (int) ((mouseY - y - 16 + listScrollOffset) / ITEM_HEIGHT);
            if (clickedIndex >= 0 && clickedIndex < filteredProfiles.size()) {
                selectedProfileIndex = clickedIndex;
                nameField.setText(getSelectedProfileName());
                updateButtonStates();
                return true;
            }
        }
        return false;
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        int maxScroll = Math.max(0, filteredProfiles.size() * ITEM_HEIGHT - (LIST_HEIGHT - 18));
        listScrollOffset = Math.max(0, Math.min(maxScroll, listScrollOffset - (int)(vAmount * ITEM_HEIGHT)));
        return true;
    }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> list = new ArrayList<>();
        if (sourceFilter != null) list.add(sourceFilter);
        if (categoryFilter != null) list.add(categoryFilter);
        if (searchField != null) list.add(searchField);
        if (nameField != null) list.add(nameField);
        if (loadBtn != null) list.add(loadBtn);
        if (saveBtn != null) list.add(saveBtn);
        if (saveAsBtn != null) list.add(saveAsBtn);
        if (deleteBtn != null) list.add(deleteBtn);
        if (duplicateBtn != null) list.add(duplicateBtn);
        if (renameBtn != null) list.add(renameBtn);
        if (saveToServerBtn != null && saveToServerBtn.visible) list.add(saveToServerBtn);
        return list;
    }
    
    public List<String> getProfileNames() {
        List<String> names = new ArrayList<>();
        for (ProfileEntry entry : allProfiles) {
            names.add(entry.name());
        }
        return names;
    }
}
