package net.cyberpunk042.client.gui.panel;

import net.cyberpunk042.client.gui.layout.Bounds;
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
 * G91-G100: Profiles management panel with dual-panel layout.
 * 
 * <p><b>Dual-Panel Layout:</b></p>
 * <ul>
 *   <li><b>LEFT Panel:</b> Filters + Profile List + Name Field</li>
 *   <li><b>RIGHT Panel:</b> Current Config Summary + Stats + Action Buttons</li>
 * </ul>
 * 
 * <p>Call {@link #setDualBounds(Bounds, Bounds)} before {@link #init(int, int)} to enable
 * dual-panel mode. If only single bounds are set, falls back to cramped single-panel mode.</p>
 */
public class ProfilesPanel extends AbstractPanel {
    
    private final List<ProfileEntry> allProfiles;
    private List<ProfileEntry> filteredProfiles;
    private int selectedProfileIndex = 0;
    private int listScrollOffset = 0;
    
    // Dual-panel bounds
    private Bounds leftBounds;
    private Bounds rightBounds;
    
    // Filter widgets (LEFT panel)
    private CyclingButtonWidget<String> sourceFilter;
    private CyclingButtonWidget<String> categoryFilter;
    private TextFieldWidget searchField;
    private TextFieldWidget nameField;
    
    // Selected filter values (null = All)
    private String selectedSource = null;
    private String selectedCategory = null;
    
    // Action buttons (RIGHT panel)
    private ButtonWidget loadBtn, saveBtn, saveAsBtn, deleteBtn;
    private ButtonWidget duplicateBtn, renameBtn;
    private ButtonWidget saveToServerBtn;
    
    private static final int FILTER_HEIGHT = 20;
    private static final int FILTER_GAP = 4;
    private static final int LIST_HEADER_HEIGHT = 20;
    private static final int ITEM_HEIGHT = 22;
    private static final int SECTION_GAP = 8;
    private static final int BTN_HEIGHT = 20;
    private static final int BTN_GAP = 4;
    
    public ProfilesPanel(Screen parent, FieldEditState state) {
        super(parent, state);
        this.allProfiles = state.getProfiles();
        this.filteredProfiles = new ArrayList<>(allProfiles);
    }
    
    /**
     * Sets both left and right panel bounds for dual-panel layout.
     * Call this BEFORE init() to enable proper dual-panel rendering.
     */
    public void setDualBounds(Bounds left, Bounds right) {
        this.leftBounds = left;
        this.rightBounds = right;
        // Set the base bounds to left for compatibility
        this.bounds = left;
    }
    
    /**
     * Returns true if dual-panel mode is enabled.
     */
    public boolean isDualMode() {
        return leftBounds != null && rightBounds != null;
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        
        if (isDualMode()) {
            initDualMode();
        } else {
            initSingleMode(width, height);
        }
        
        updateButtonStates();
    }
    
    /**
     * Initializes widgets for dual-panel mode (proper layout).
     */
    private void initDualMode() {
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        
        // ═══════════════════════════════════════════════════════════════════
        // LEFT PANEL: Filters + Profile List + Name Field
        // ═══════════════════════════════════════════════════════════════════
        int lx = leftBounds.x() + GuiConstants.PADDING;
        int ly = leftBounds.y() + GuiConstants.PADDING;
        int lw = leftBounds.width() - GuiConstants.PADDING * 2;
        
        // Filter row: [Source] [Category]
        int filterW = (lw - GuiConstants.PADDING) / 2;
        
        List<String> sources = List.of("All", "Local", "Server", "Bundled");
        sourceFilter = CyclingButtonWidget.<String>builder(Text::literal)
            .values(sources)
            .initially("All")
            .build(lx, ly, filterW, FILTER_HEIGHT, Text.literal("Source"),
                (btn, val) -> {
                    selectedSource = "All".equals(val) ? null : val;
                    applyFilters();
                });
        
        categoryFilter = CyclingButtonWidget.<String>builder(Text::literal)
            .values(List.of("All", "Combat", "Utility", "Decorative", "Experimental"))
            .initially("All")
            .build(lx + filterW + GuiConstants.PADDING, ly, filterW, FILTER_HEIGHT, Text.literal("Category"),
                (btn, val) -> {
                    selectedCategory = "All".equals(val) ? null : val;
                    applyFilters();
                });
        
        ly += FILTER_HEIGHT + FILTER_GAP;
        
        // Search field (full width)
        searchField = new TextFieldWidget(textRenderer, lx, ly, lw, FILTER_HEIGHT, Text.literal("Search"));
        searchField.setPlaceholder(Text.literal("🔍 Search profiles..."));
        searchField.setChangedListener(text -> applyFilters());
        
        ly += FILTER_HEIGHT + SECTION_GAP;
        
        // Profile list is rendered manually (not a widget)
        // Calculate list height to leave room for name field
        int listHeight = leftBounds.bottom() - ly - FILTER_HEIGHT - SECTION_GAP - GuiConstants.PADDING;
        
        // Name field at bottom of left panel
        int nameY = leftBounds.bottom() - GuiConstants.PADDING - FILTER_HEIGHT;
        nameField = new TextFieldWidget(textRenderer, lx, nameY, lw, FILTER_HEIGHT, Text.literal("Profile Name"));
        nameField.setMaxLength(32);
        nameField.setText(getSelectedProfileName());
        
        // ═══════════════════════════════════════════════════════════════════
        // RIGHT PANEL: Summary + Stats + Action Buttons
        // ═══════════════════════════════════════════════════════════════════
        int rx = rightBounds.x() + GuiConstants.PADDING;
        int ry = rightBounds.y() + GuiConstants.PADDING;
        int rw = rightBounds.width() - GuiConstants.PADDING * 2;
        
        // Summary and stats are rendered manually (below)
        
        // Action buttons - at bottom of right panel
        // Calculate button layout: 2 rows of 3 buttons + 1 full-width button
        int btnW = (rw - BTN_GAP * 2) / 3;
        int btnY = rightBounds.bottom() - GuiConstants.PADDING - BTN_HEIGHT * 3 - BTN_GAP * 2;
        
        // Row 1: Load, Save, Delete
        loadBtn = ButtonWidget.builder(Text.literal("Load"), btn -> loadProfile())
            .dimensions(rx, btnY, btnW, BTN_HEIGHT)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Load selected profile")))
            .build();
        
        saveBtn = ButtonWidget.builder(Text.literal("Save"), btn -> saveProfile())
            .dimensions(rx + btnW + BTN_GAP, btnY, btnW, BTN_HEIGHT)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Save current changes")))
            .build();
        
        deleteBtn = ButtonWidget.builder(Text.literal("Delete"), btn -> deleteProfile())
            .dimensions(rx + (btnW + BTN_GAP) * 2, btnY, btnW, BTN_HEIGHT)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Delete selected profile")))
            .build();
        
        btnY += BTN_HEIGHT + BTN_GAP;
        
        // Row 2: Save As, Duplicate, Rename
        saveAsBtn = ButtonWidget.builder(Text.literal("Save As"), btn -> saveAsProfile())
            .dimensions(rx, btnY, btnW, BTN_HEIGHT)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Save as new profile")))
            .build();
        
        duplicateBtn = ButtonWidget.builder(Text.literal("Copy"), btn -> duplicateProfile())
            .dimensions(rx + btnW + BTN_GAP, btnY, btnW, BTN_HEIGHT)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Duplicate profile")))
            .build();
        
        renameBtn = ButtonWidget.builder(Text.literal("Rename"), btn -> renameProfile())
            .dimensions(rx + (btnW + BTN_GAP) * 2, btnY, btnW, BTN_HEIGHT)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Rename profile")))
            .build();
        
        btnY += BTN_HEIGHT + BTN_GAP;
        
        // Row 3: Save to Server (full width, OP only)
        saveToServerBtn = ButtonWidget.builder(Text.literal("⚡ Save to Server"), btn -> promptSaveToServer())
            .dimensions(rx, btnY, rw, BTN_HEIGHT)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Save as server profile (OP only)")))
            .build();
    }
    
    /**
     * Fallback initialization for single-panel mode (legacy/cramped).
     */
    private void initSingleMode(int width, int height) {
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        
        int x = GuiConstants.PADDING;
        int y = GuiConstants.PADDING;
        int w = width - GuiConstants.PADDING * 2;
        int filterW = Math.min(80, (w - GuiConstants.PADDING * 2) / 3);
        
        List<String> sources = List.of("All", "Local", "Server", "Bundled");
        sourceFilter = CyclingButtonWidget.<String>builder(Text::literal)
            .values(sources)
            .initially("All")
            .build(x, y, filterW, FILTER_HEIGHT, Text.literal("Src"),
                (btn, val) -> {
                    selectedSource = "All".equals(val) ? null : val;
                    applyFilters();
                });
        
        categoryFilter = CyclingButtonWidget.<String>builder(Text::literal)
            .values(List.of("All", "Combat", "Utility", "Deco", "Exp"))
            .initially("All")
            .build(x + filterW + 2, y, filterW, FILTER_HEIGHT, Text.literal("Cat"),
                (btn, val) -> {
                    selectedCategory = "All".equals(val) ? null : val;
                    applyFilters();
                });
        
        int searchW = w - filterW * 2 - 4;
        searchField = new TextFieldWidget(textRenderer, 
            x + filterW * 2 + 4, y, Math.max(40, searchW), FILTER_HEIGHT, 
            Text.literal("Search"));
        searchField.setPlaceholder(Text.literal("🔍"));
        searchField.setChangedListener(text -> applyFilters());
        
        y += FILTER_HEIGHT + SECTION_GAP;
        
        // Name field
        nameField = new TextFieldWidget(textRenderer, x, height - GuiConstants.PADDING - FILTER_HEIGHT, 
            w, FILTER_HEIGHT, Text.literal("Name"));
        nameField.setMaxLength(32);
        nameField.setText(getSelectedProfileName());
        
        // Buttons (compact)
        int btnW = (w - 4) / 3;
        int btnY = height - GuiConstants.PADDING - FILTER_HEIGHT - BTN_GAP - BTN_HEIGHT * 2 - BTN_GAP;
        
        loadBtn = ButtonWidget.builder(Text.literal("Load"), btn -> loadProfile())
            .dimensions(x, btnY, btnW, BTN_HEIGHT).build();
        saveBtn = ButtonWidget.builder(Text.literal("Save"), btn -> saveProfile())
            .dimensions(x + btnW + 2, btnY, btnW, BTN_HEIGHT).build();
        deleteBtn = ButtonWidget.builder(Text.literal("Del"), btn -> deleteProfile())
            .dimensions(x + (btnW + 2) * 2, btnY, btnW, BTN_HEIGHT).build();
        
        btnY += BTN_HEIGHT + 2;
        saveAsBtn = ButtonWidget.builder(Text.literal("As"), btn -> saveAsProfile())
            .dimensions(x, btnY, btnW, BTN_HEIGHT).build();
        duplicateBtn = ButtonWidget.builder(Text.literal("Dup"), btn -> duplicateProfile())
            .dimensions(x + btnW + 2, btnY, btnW, BTN_HEIGHT).build();
        renameBtn = ButtonWidget.builder(Text.literal("Ren"), btn -> renameProfile())
            .dimensions(x + (btnW + 2) * 2, btnY, btnW, BTN_HEIGHT).build();
        
        saveToServerBtn = null; // Skip in cramped mode
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
                }
                // Category filter - check description for category keyword (simplified)
                if (selectedCategory != null && p.description() != null) {
                    if (!p.description().toLowerCase().contains(selectedCategory.toLowerCase())) return false;
                }
                // Search filter
                if (!searchText.isEmpty()) {
                    if (!p.name().toLowerCase().contains(searchText)) return false;
                }
                return true;
            })
            .collect(Collectors.toList());
        
        selectedProfileIndex = Math.min(selectedProfileIndex, Math.max(0, filteredProfiles.size() - 1));
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
        if (isDualMode()) {
            renderDualMode(context, mouseX, mouseY, delta);
        } else {
            renderSingleMode(context, mouseX, mouseY, delta);
        }
    }
    
    /**
     * Renders dual-panel layout.
     */
    private void renderDualMode(DrawContext context, int mouseX, int mouseY, float delta) {
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        
        // ═══════════════════════════════════════════════════════════════════
        // LEFT PANEL BACKGROUND
        // ═══════════════════════════════════════════════════════════════════
        context.fill(leftBounds.x(), leftBounds.y(), leftBounds.right(), leftBounds.bottom(), GuiConstants.BG_PANEL);
        context.drawBorder(leftBounds.x(), leftBounds.y(), leftBounds.width(), leftBounds.height(), GuiConstants.BORDER);
        
        // LEFT PANEL TITLE
        context.drawText(textRenderer, "Profiles", leftBounds.x() + GuiConstants.PADDING, 
            leftBounds.y() + 4, GuiConstants.TEXT_PRIMARY, false);
        
        // ═══════════════════════════════════════════════════════════════════
        // RIGHT PANEL BACKGROUND
        // ═══════════════════════════════════════════════════════════════════
        context.fill(rightBounds.x(), rightBounds.y(), rightBounds.right(), rightBounds.bottom(), GuiConstants.BG_PANEL);
        context.drawBorder(rightBounds.x(), rightBounds.y(), rightBounds.width(), rightBounds.height(), GuiConstants.BORDER);
        
        // RIGHT PANEL TITLE
        context.drawText(textRenderer, "Details", rightBounds.x() + GuiConstants.PADDING, 
            rightBounds.y() + 4, GuiConstants.TEXT_PRIMARY, false);
        
        // ═══════════════════════════════════════════════════════════════════
        // PROFILE LIST (LEFT PANEL, below filters)
        // ═══════════════════════════════════════════════════════════════════
        int listX = leftBounds.x() + GuiConstants.PADDING;
        int listY = leftBounds.y() + GuiConstants.PADDING + FILTER_HEIGHT * 2 + FILTER_GAP * 2 + SECTION_GAP;
        int listW = leftBounds.width() - GuiConstants.PADDING * 2;
        int listH = leftBounds.bottom() - listY - FILTER_HEIGHT - SECTION_GAP - GuiConstants.PADDING;
        
        // List background
        context.fill(listX, listY, listX + listW, listY + listH, 0xFF1A1A1A);
        context.drawBorder(listX, listY, listW, listH, GuiConstants.BORDER);
        
        // List header
        context.drawText(textRenderer, "Profiles (" + filteredProfiles.size() + ")", 
            listX + 4, listY + 4, GuiConstants.TEXT_SECONDARY, false);
        
        // Enable scissor for list items
        context.enableScissor(listX, listY + LIST_HEADER_HEIGHT, listX + listW, listY + listH);
        
        int itemY = listY + LIST_HEADER_HEIGHT - listScrollOffset;
        for (int i = 0; i < filteredProfiles.size(); i++) {
            if (itemY + ITEM_HEIGHT > listY + LIST_HEADER_HEIGHT && itemY < listY + listH) {
                boolean selected = i == selectedProfileIndex;
                boolean hovered = mouseX >= listX && mouseX < listX + listW && 
                                  mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
                
                // Background
                if (selected) {
                    context.fill(listX + 2, itemY, listX + listW - 2, itemY + ITEM_HEIGHT - 2, GuiConstants.ACCENT);
                } else if (hovered) {
                    context.fill(listX + 2, itemY, listX + listW - 2, itemY + ITEM_HEIGHT - 2, 0x40FFFFFF);
                }
                
                ProfileEntry entry = filteredProfiles.get(i);
                
                // Source icon
                String icon = entry.isServer() ? "🔒" : "✎";
                context.drawText(textRenderer, icon, listX + 6, itemY + 5, GuiConstants.TEXT_SECONDARY, false);
                
                // Profile name
                int textColor = selected ? 0xFFFFFFFF : GuiConstants.TEXT_PRIMARY;
                context.drawText(textRenderer, entry.name(), listX + 22, itemY + 5, textColor, false);
                
                // Source label (right side)
                String sourceLabel = entry.isServer() ? "server" : "local";
                int labelWidth = textRenderer.getWidth(sourceLabel);
                context.drawText(textRenderer, sourceLabel, listX + listW - labelWidth - 8, itemY + 5, 
                    GuiConstants.TEXT_SECONDARY, false);
            }
            itemY += ITEM_HEIGHT;
        }
        
        context.disableScissor();
        
        // ═══════════════════════════════════════════════════════════════════
        // CONFIGURATION SUMMARY (RIGHT PANEL)
        // ═══════════════════════════════════════════════════════════════════
        int rx = rightBounds.x() + GuiConstants.PADDING;
        int ry = rightBounds.y() + GuiConstants.PADDING + 16; // Below title
        int rw = rightBounds.width() - GuiConstants.PADDING * 2;
        
        // Current Configuration section
        int summaryH = 80;
        context.fill(rx, ry, rx + rw, ry + summaryH, GuiConstants.BG_SECONDARY);
        context.drawBorder(rx, ry, rw, summaryH, GuiConstants.BORDER);
        context.drawText(textRenderer, "Current Configuration", rx + 4, ry + 4, GuiConstants.TEXT_PRIMARY, false);
        
        int lineY = ry + 18;
        lineY = drawSummaryLine(context, textRenderer, rx + 6, lineY, "Shape", state.getCurrentShapeFragmentName());
        lineY = drawSummaryLine(context, textRenderer, rx + 6, lineY, "Fill", state.getCurrentFillFragmentName());
        lineY = drawSummaryLine(context, textRenderer, rx + 6, lineY, "Animation", state.getCurrentAnimationFragmentName());
        
        ry += summaryH + SECTION_GAP;
        
        // Stats section
        int statsH = 60;
        context.fill(rx, ry, rx + rw, ry + statsH, GuiConstants.BG_SECONDARY);
        context.drawBorder(rx, ry, rw, statsH, GuiConstants.BORDER);
        context.drawText(textRenderer, "Stats", rx + 4, ry + 4, GuiConstants.TEXT_PRIMARY, false);
        
        int statY = ry + 18;
        context.drawText(textRenderer, "latSteps: " + state.getInt("sphere.latSteps"), rx + 6, statY, GuiConstants.TEXT_SECONDARY, false);
        context.drawText(textRenderer, "lonSteps: " + state.getInt("sphere.lonSteps"), rx + 90, statY, GuiConstants.TEXT_SECONDARY, false);
        statY += 12;
        context.drawText(textRenderer, "radius: " + String.format("%.1f", state.getFloat("radius")), rx + 6, statY, GuiConstants.TEXT_SECONDARY, false);
        context.drawText(textRenderer, "alpha: " + String.format("%.2f", state.getFloat("appearance.alpha")), rx + 90, statY, GuiConstants.TEXT_SECONDARY, false);
        
        // Dirty indicator (right panel bottom area, above buttons)
        if (state.isDirty()) {
            int indicatorY = rightBounds.bottom() - GuiConstants.PADDING - BTN_HEIGHT * 3 - BTN_GAP * 2 - 20;
            context.drawText(textRenderer, "● Unsaved changes", rx, indicatorY, GuiConstants.WARNING, false);
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // RENDER WIDGETS
        // ═══════════════════════════════════════════════════════════════════
        if (sourceFilter != null) sourceFilter.render(context, mouseX, mouseY, delta);
        if (categoryFilter != null) categoryFilter.render(context, mouseX, mouseY, delta);
        if (searchField != null) searchField.render(context, mouseX, mouseY, delta);
        if (nameField != null) nameField.render(context, mouseX, mouseY, delta);
        
        if (loadBtn != null) loadBtn.render(context, mouseX, mouseY, delta);
        if (saveBtn != null) saveBtn.render(context, mouseX, mouseY, delta);
        if (deleteBtn != null) deleteBtn.render(context, mouseX, mouseY, delta);
        if (saveAsBtn != null) saveAsBtn.render(context, mouseX, mouseY, delta);
        if (duplicateBtn != null) duplicateBtn.render(context, mouseX, mouseY, delta);
        if (renameBtn != null) renameBtn.render(context, mouseX, mouseY, delta);
        if (saveToServerBtn != null) saveToServerBtn.render(context, mouseX, mouseY, delta);
    }
    
    /**
     * Renders single-panel layout (fallback).
     */
    private void renderSingleMode(DrawContext context, int mouseX, int mouseY, float delta) {
        int bx = bounds.x();
        int by = bounds.y();
        int bw = bounds.width();
        int bh = bounds.height();
        
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        
        // Panel background
        context.fill(bx, by, bx + bw, by + bh, GuiConstants.BG_PANEL);
        context.drawBorder(bx, by, bw, bh, GuiConstants.BORDER);
        
        // Profile list
        int listY = by + GuiConstants.PADDING + FILTER_HEIGHT + SECTION_GAP;
        int listH = bh - GuiConstants.PADDING * 2 - FILTER_HEIGHT * 2 - BTN_HEIGHT * 2 - SECTION_GAP * 3;
        int listW = bw - GuiConstants.PADDING * 2;
        
        context.fill(bx + GuiConstants.PADDING, listY, bx + bw - GuiConstants.PADDING, listY + listH, 0xFF1A1A1A);
        context.drawBorder(bx + GuiConstants.PADDING, listY, listW, listH, GuiConstants.BORDER);
        
        context.drawText(textRenderer, "(" + filteredProfiles.size() + ")", bx + GuiConstants.PADDING + 4, listY + 4, 
            GuiConstants.TEXT_SECONDARY, false);
        
        // Enable scissor for list
        context.enableScissor(bx + GuiConstants.PADDING, listY + LIST_HEADER_HEIGHT, 
            bx + bw - GuiConstants.PADDING, listY + listH);
        
        int itemY = listY + LIST_HEADER_HEIGHT - listScrollOffset;
        for (int i = 0; i < filteredProfiles.size(); i++) {
            if (itemY + ITEM_HEIGHT > listY + LIST_HEADER_HEIGHT && itemY < listY + listH) {
                boolean selected = i == selectedProfileIndex;
                boolean hovered = mouseX >= bx + GuiConstants.PADDING && mouseX < bx + bw - GuiConstants.PADDING && 
                                  mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
                
                if (selected) {
                    context.fill(bx + GuiConstants.PADDING + 2, itemY, 
                        bx + bw - GuiConstants.PADDING - 2, itemY + ITEM_HEIGHT - 2, GuiConstants.ACCENT);
                } else if (hovered) {
                    context.fill(bx + GuiConstants.PADDING + 2, itemY, 
                        bx + bw - GuiConstants.PADDING - 2, itemY + ITEM_HEIGHT - 2, 0x40FFFFFF);
                }
                
                ProfileEntry entry = filteredProfiles.get(i);
                String icon = entry.isServer() ? "🔒" : "✎";
                int textColor = selected ? 0xFFFFFFFF : GuiConstants.TEXT_PRIMARY;
                context.drawText(textRenderer, icon + " " + entry.name(), bx + GuiConstants.PADDING + 4, itemY + 5, textColor, false);
            }
            itemY += ITEM_HEIGHT;
        }
        
        context.disableScissor();
        
        // Dirty indicator
        if (state.isDirty()) {
            context.drawText(textRenderer, "●", bx + bw - 16, by + 4, GuiConstants.WARNING, false);
        }
        
        // Render widgets
        if (sourceFilter != null) sourceFilter.render(context, mouseX, mouseY, delta);
        if (categoryFilter != null) categoryFilter.render(context, mouseX, mouseY, delta);
        if (searchField != null) searchField.render(context, mouseX, mouseY, delta);
        if (nameField != null) nameField.render(context, mouseX, mouseY, delta);
        if (loadBtn != null) loadBtn.render(context, mouseX, mouseY, delta);
        if (saveBtn != null) saveBtn.render(context, mouseX, mouseY, delta);
        if (deleteBtn != null) deleteBtn.render(context, mouseX, mouseY, delta);
        if (saveAsBtn != null) saveAsBtn.render(context, mouseX, mouseY, delta);
        if (duplicateBtn != null) duplicateBtn.render(context, mouseX, mouseY, delta);
        if (renameBtn != null) renameBtn.render(context, mouseX, mouseY, delta);
    }
    
    private int drawSummaryLine(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer,
                               int x, int y, String label, String value) {
        context.drawText(textRenderer, label + ":", x, y, GuiConstants.TEXT_SECONDARY, false);
        context.drawText(textRenderer, value != null ? value : "—", x + 70, y, GuiConstants.TEXT_PRIMARY, false);
        return y + 14;
    }
    
    @Override
    public void applyBoundsOffset() {
        // In dual mode, widgets are already at absolute positions
        // In single mode, apply offset from base bounds
        if (!isDualMode()) {
            super.applyBoundsOffset();
            
            int dx = bounds.x();
            int dy = bounds.y();
            
            // Offset our custom widgets
            offsetWidget(sourceFilter, dx, dy);
            offsetWidget(categoryFilter, dx, dy);
            offsetWidget(searchField, dx, dy);
            offsetWidget(nameField, dx, dy);
            offsetWidget(loadBtn, dx, dy);
            offsetWidget(saveBtn, dx, dy);
            offsetWidget(saveAsBtn, dx, dy);
            offsetWidget(deleteBtn, dx, dy);
            offsetWidget(duplicateBtn, dx, dy);
            offsetWidget(renameBtn, dx, dy);
            offsetWidget(saveToServerBtn, dx, dy);
        }
    }
    
    private void offsetWidget(net.minecraft.client.gui.widget.ClickableWidget widget, int dx, int dy) {
        if (widget != null) {
            widget.setX(widget.getX() + dx);
            widget.setY(widget.getY() + dy);
        }
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
        if (saveToServerBtn != null) list.add(saveToServerBtn);
        return list;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MOUSE HANDLING
    // ═══════════════════════════════════════════════════════════════════════════
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        
        // Determine list bounds
        int listX, listY, listW, listH;
        if (isDualMode()) {
            listX = leftBounds.x() + GuiConstants.PADDING;
            listY = leftBounds.y() + GuiConstants.PADDING + FILTER_HEIGHT * 2 + FILTER_GAP * 2 + SECTION_GAP;
            listW = leftBounds.width() - GuiConstants.PADDING * 2;
            listH = leftBounds.bottom() - listY - FILTER_HEIGHT - SECTION_GAP - GuiConstants.PADDING;
        } else {
            listX = bounds.x() + GuiConstants.PADDING;
            listY = bounds.y() + GuiConstants.PADDING + FILTER_HEIGHT + SECTION_GAP;
            listW = bounds.width() - GuiConstants.PADDING * 2;
            listH = bounds.height() - GuiConstants.PADDING * 2 - FILTER_HEIGHT * 2 - BTN_HEIGHT * 2 - SECTION_GAP * 3;
        }
        
        if (mouseX >= listX && mouseX < listX + listW && 
            mouseY >= listY + LIST_HEADER_HEIGHT && mouseY < listY + listH) {
            
            int relY = (int) mouseY - listY - LIST_HEADER_HEIGHT + listScrollOffset;
            int idx = relY / ITEM_HEIGHT;
            
            if (idx >= 0 && idx < filteredProfiles.size()) {
                selectedProfileIndex = idx;
                if (nameField != null) {
                    nameField.setText(getSelectedProfileName());
                }
                updateButtonStates();
                return true;
            }
        }
        return false;
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        // Determine list bounds
        int listH;
        if (isDualMode()) {
            int listY = leftBounds.y() + GuiConstants.PADDING + FILTER_HEIGHT * 2 + FILTER_GAP * 2 + SECTION_GAP;
            listH = leftBounds.bottom() - listY - FILTER_HEIGHT - SECTION_GAP - GuiConstants.PADDING;
        } else {
            listH = bounds.height() - GuiConstants.PADDING * 2 - FILTER_HEIGHT * 2 - BTN_HEIGHT * 2 - SECTION_GAP * 3;
        }
        
        int contentH = filteredProfiles.size() * ITEM_HEIGHT;
        int maxScroll = Math.max(0, contentH - (listH - LIST_HEADER_HEIGHT));
        listScrollOffset = Math.max(0, Math.min(maxScroll, listScrollOffset - (int)(vAmount * ITEM_HEIGHT)));
        return true;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROFILE ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String getSelectedProfileName() {
        if (selectedProfileIndex >= 0 && selectedProfileIndex < filteredProfiles.size()) {
            return filteredProfiles.get(selectedProfileIndex).name();
        }
        return "";
    }
    
    private ProfileEntry getSelectedProfile() {
        if (selectedProfileIndex >= 0 && selectedProfileIndex < filteredProfiles.size()) {
            return filteredProfiles.get(selectedProfileIndex);
        }
        return null;
    }
    
    private void updateButtonStates() {
        ProfileEntry selected = getSelectedProfile();
        boolean hasSelection = selected != null;
        boolean isServerProfile = hasSelection && selected.isServer();
        boolean isDefaultProfile = hasSelection && selected.name().equalsIgnoreCase("default");
        boolean canModify = hasSelection && !isServerProfile;
        boolean canDelete = canModify && !isDefaultProfile;

        if (loadBtn != null) loadBtn.active = hasSelection;
        if (saveBtn != null) saveBtn.active = canModify && state.isDirty();
        if (deleteBtn != null) deleteBtn.active = canDelete;
        if (saveAsBtn != null) saveAsBtn.active = true;
        if (duplicateBtn != null) duplicateBtn.active = hasSelection;
        if (renameBtn != null) renameBtn.active = canModify;
        
        if (saveToServerBtn != null) {
            var player = MinecraftClient.getInstance().player;
            boolean isOp = player != null && player.hasPermissionLevel(2);
            saveToServerBtn.visible = isOp;
            saveToServerBtn.active = isOp && hasSelection;
        }
    }
    
    private void loadProfile() {
        ProfileEntry entry = getSelectedProfile();
        if (entry != null) {
            // Set as current profile (actual loading happens via network for server profiles)
            state.setCurrentProfile(entry.name(), entry.isServer());
            ToastNotification.success("Loaded: " + entry.name());
            Logging.GUI.info("Loaded profile: {}", entry.name());
        }
    }
    
    private void saveProfile() {
        ProfileEntry entry = getSelectedProfile();
        if (entry != null && !entry.isServer()) {
            // Mark as saved (actual persistence is external)
            state.setCurrentProfile(entry.name(), false);
            state.clearDirty();
            ToastNotification.success("Saved: " + entry.name());
            Logging.GUI.info("Saved profile: {}", entry.name());
            updateButtonStates();
        }
    }
    
    private void saveAsProfile() {
        String name = nameField != null ? nameField.getText().trim() : "";
        if (name.isEmpty()) {
            ToastNotification.warning("Enter a profile name");
            return;
        }
        
        boolean exists = allProfiles.stream().anyMatch(p -> p.name().equals(name));
        if (exists) {
            ToastNotification.warning("Profile already exists");
            return;
        }
        
        // Add to local profiles list and set as current
        allProfiles.add(new ProfileEntry(name, false, ""));
        state.setCurrentProfile(name, false);
        state.clearDirty();
        applyFilters();
        ToastNotification.success("Created: " + name);
        Logging.GUI.info("Created profile: {}", name);
    }
    
    private void deleteProfile() {
        ProfileEntry entry = getSelectedProfile();
        if (entry != null && !entry.isServer()) {
            // Prevent deleting the default profile
            if (entry.name().equalsIgnoreCase("default")) {
                ToastNotification.warning("Cannot delete default profile");
                return;
            }
            allProfiles.remove(entry);
            applyFilters();
            ToastNotification.success("Deleted: " + entry.name());
            Logging.GUI.info("Deleted profile: {}", entry.name());
        }
    }
    
    private void duplicateProfile() {
        ProfileEntry entry = getSelectedProfile();
        if (entry != null) {
            String baseName = entry.name() + " (copy)";
            String finalName = baseName;
            int counter = 1;
            while (profileNameExists(finalName)) {
                finalName = entry.name() + " (copy " + (++counter) + ")";
            }
            // Duplicate in list with same description
            allProfiles.add(new ProfileEntry(finalName, false, entry.description()));
            applyFilters();
            ToastNotification.success("Duplicated: " + finalName);
            Logging.GUI.info("Duplicated profile {} to {}", entry.name(), finalName);
        }
    }
    
    private boolean profileNameExists(String name) {
        for (ProfileEntry p : allProfiles) {
            if (p.name().equals(name)) return true;
        }
        return false;
    }
    
    private void renameProfile() {
        ProfileEntry entry = getSelectedProfile();
        String newName = nameField != null ? nameField.getText().trim() : "";
        
        if (entry == null || entry.isServer() || newName.isEmpty()) return;
        if (newName.equals(entry.name())) return;
        
        boolean exists = allProfiles.stream().anyMatch(p -> p.name().equals(newName));
        if (exists) {
            ToastNotification.warning("Name already exists");
            return;
        }
        
        // Update in list
        int idx = allProfiles.indexOf(entry);
        if (idx >= 0) {
            allProfiles.set(idx, new ProfileEntry(newName, entry.isServer(), entry.description()));
        }
        applyFilters();
        ToastNotification.success("Renamed to: " + newName);
        Logging.GUI.info("Renamed profile {} to {}", entry.name(), newName);
    }
    
    private void promptSaveToServer() {
        ProfileEntry entry = getSelectedProfile();
        if (entry == null) return;
        
        // Send to server using proper constructor
        String jsonStr = state.toProfileJson(entry.name());
        ClientPlayNetworking.send(ProfileSaveC2SPayload.saveToServer(entry.name(), jsonStr));
        ToastNotification.info("Sent to server: " + entry.name());
        Logging.GUI.info("Sent profile to server: {}", entry.name());
    }
    
    public List<String> getProfileNames() {
        List<String> names = new ArrayList<>();
        for (ProfileEntry entry : allProfiles) {
            names.add(entry.name());
        }
        return names;
    }
}
