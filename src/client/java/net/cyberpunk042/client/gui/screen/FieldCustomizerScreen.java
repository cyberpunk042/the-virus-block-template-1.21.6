package net.cyberpunk042.client.gui.screen;

import net.cyberpunk042.client.gui.panel.*;
import net.cyberpunk042.client.gui.state.GuiState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.widget.ToastNotification;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * G01/G111-G120: Main Field Customizer GUI screen.
 * 
 * <p>Integrates all panels with tab-based navigation:</p>
 * <ul>
 *   <li>Quick tab - Basic customization (shape, color, fill)</li>
 *   <li>Advanced tab - Detailed parameters</li>
 *   <li>Debug tab - Lifecycle, triggers (operator only)</li>
 *   <li>Profiles tab - Save/load management</li>
 * </ul>
 */
public class FieldCustomizerScreen extends Screen {
    
    private final GuiState state;
    private TabType currentTab = TabType.QUICK;
    
    // Tab buttons
    private ButtonWidget quickTabBtn;
    private ButtonWidget advancedTabBtn;
    private ButtonWidget debugTabBtn;
    private ButtonWidget profilesTabBtn;
    
    // Panels
    private QuickPanel quickPanel;
    private AdvancedPanel advancedPanel;
    private DebugPanel debugPanel;
    private ProfilesPanel profilesPanel;
    
    // Close button
    private ButtonWidget closeBtn;
    
    public FieldCustomizerScreen() {
        this(new GuiState());
    }
    
    public FieldCustomizerScreen(GuiState state) {
        super(Text.literal("Field Customizer"));
        this.state = state;
        Logging.GUI.topic("screen").info("FieldCustomizerScreen created");
    }
    
    @Override
    protected void init() {
        super.init();
        
        // G111: Initialize tab buttons
        int tabWidth = (width - GuiConstants.PADDING * 5) / 4;
        int tabY = GuiConstants.PADDING;
        int x = GuiConstants.PADDING;
        
        quickTabBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Quick"), btn -> switchTab(TabType.QUICK))
            .dimensions(x, tabY, tabWidth, GuiConstants.TAB_HEIGHT - GuiConstants.PADDING)
            .build());
        x += tabWidth + GuiConstants.PADDING;
        
        advancedTabBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Advanced"), btn -> switchTab(TabType.ADVANCED))
            .dimensions(x, tabY, tabWidth, GuiConstants.TAB_HEIGHT - GuiConstants.PADDING)
            .build());
        x += tabWidth + GuiConstants.PADDING;
        
        debugTabBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Debug"), btn -> switchTab(TabType.DEBUG))
            .dimensions(x, tabY, tabWidth, GuiConstants.TAB_HEIGHT - GuiConstants.PADDING)
            .build());
        x += tabWidth + GuiConstants.PADDING;
        
        profilesTabBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Profiles"), btn -> switchTab(TabType.PROFILES))
            .dimensions(x, tabY, tabWidth, GuiConstants.TAB_HEIGHT - GuiConstants.PADDING)
            .build());
        
        // G112: Initialize panels
        quickPanel = new QuickPanel(this, state);
        quickPanel.init(width, height);
        
        advancedPanel = new AdvancedPanel(this, state);
        advancedPanel.init(width, height);
        
        debugPanel = new DebugPanel(this, state);
        debugPanel.init(width, height);
        
        profilesPanel = new ProfilesPanel(this, state);
        profilesPanel.init(width, height);
        
        // G113: Close button
        closeBtn = addDrawableChild(ButtonWidget.builder(Text.literal("×"), btn -> close())
            .dimensions(width - 24, 4, 20, 20)
            .build());
        
        // G114: Update tab button states
        updateTabButtons();
        
        // G115: Register current panel widgets
        registerPanelWidgets();
        
        Logging.GUI.topic("screen").debug("FieldCustomizerScreen initialized");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G116: TAB SWITCHING
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void switchTab(TabType tab) {
        if (tab == TabType.DEBUG && !state.isDebugUnlocked()) {
            ToastNotification.warning("Debug mode requires operator access");
            Logging.GUI.topic("screen").warn("Debug tab access denied");
            return;
        }
        
        currentTab = tab;
        updateTabButtons();
        clearAndRegisterWidgets();
        
        Logging.GUI.topic("screen").debug("Switched to tab: {}", tab);
    }
    
    private void updateTabButtons() {
        if (quickTabBtn != null) quickTabBtn.active = currentTab != TabType.QUICK;
        if (advancedTabBtn != null) advancedTabBtn.active = currentTab != TabType.ADVANCED;
        if (debugTabBtn != null) debugTabBtn.active = currentTab != TabType.DEBUG;
        if (profilesTabBtn != null) profilesTabBtn.active = currentTab != TabType.PROFILES;
        
        // Dim debug tab if locked
        if (debugTabBtn != null && !state.isDebugUnlocked()) {
            // Visual indication that debug is locked
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G117: WIDGET REGISTRATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void clearAndRegisterWidgets() {
        // Remove old panel widgets (keep tab buttons and close)
        clearChildren();
        
        // Re-add tab buttons
        addDrawableChild(quickTabBtn);
        addDrawableChild(advancedTabBtn);
        addDrawableChild(debugTabBtn);
        addDrawableChild(profilesTabBtn);
        addDrawableChild(closeBtn);
        
        // Register current panel widgets
        registerPanelWidgets();
    }
    
    private void registerPanelWidgets() {
        switch (currentTab) {
            case QUICK -> {
                if (quickPanel != null) {
                    for (var widget : quickPanel.getWidgets()) {
                        addDrawableChild(widget);
                    }
                }
            }
            case ADVANCED -> {
                // Advanced panel handles its own widget rendering due to scroll
            }
            case DEBUG -> {
                // Debug panel handles its own widget rendering
            }
            case PROFILES -> {
                if (profilesPanel != null) {
                    for (var widget : profilesPanel.getWidgets()) {
                        addDrawableChild(widget);
                    }
                }
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G118: TICK & RENDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public void tick() {
        super.tick();
        
        switch (currentTab) {
            case QUICK -> { if (quickPanel != null) quickPanel.tick(); }
            case ADVANCED -> { if (advancedPanel != null) advancedPanel.tick(); }
            case DEBUG -> { if (debugPanel != null) debugPanel.tick(); }
            case PROFILES -> { if (profilesPanel != null) profilesPanel.tick(); }
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // G119: Background
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, width, height, GuiConstants.BG_PRIMARY);
        
        // Tab bar background
        context.fill(0, 0, width, GuiConstants.TAB_HEIGHT, GuiConstants.BG_SECONDARY);
        
        // Render current panel
        switch (currentTab) {
            case QUICK -> { if (quickPanel != null) quickPanel.render(context, mouseX, mouseY, delta); }
            case ADVANCED -> { if (advancedPanel != null) advancedPanel.render(context, mouseX, mouseY, delta); }
            case DEBUG -> { if (debugPanel != null) debugPanel.render(context, mouseX, mouseY, delta); }
            case PROFILES -> { if (profilesPanel != null) profilesPanel.render(context, mouseX, mouseY, delta); }
        }
        
        // Render widgets (tab buttons, etc.)
        super.render(context, mouseX, mouseY, delta);
        
        // Dirty indicator
        if (state.isDirty()) {
            context.drawText(textRenderer, "●", width - 40, 8, GuiConstants.WARNING, false);
        }
        
        // Toast notifications
        ToastNotification.renderAll(context, textRenderer, width, height);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G120: INPUT HANDLING
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        switch (currentTab) {
            case ADVANCED -> { if (advancedPanel != null) return advancedPanel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount); }
            case DEBUG -> { if (debugPanel != null) return debugPanel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount); }
            case PROFILES -> { if (profilesPanel != null) return profilesPanel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount); }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentTab == TabType.PROFILES && profilesPanel != null) {
            if (profilesPanel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Escape to close
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            close();
            return true;
        }
        
        // Ctrl+Z for undo
        if (keyCode == 90 && (modifiers & 2) != 0) { // GLFW_KEY_Z + CTRL
            if (state.canUndo()) {
                state.undo();
                ToastNotification.info("Undo");
                return true;
            }
        }
        
        // Ctrl+Y for redo
        if (keyCode == 89 && (modifiers & 2) != 0) { // GLFW_KEY_Y + CTRL
            if (state.canRedo()) {
                state.redo();
                ToastNotification.info("Redo");
                return true;
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean shouldPause() {
        return false; // Don't pause the game
    }
    
    @Override
    public void close() {
        if (state.isDirty()) {
            // TODO: Show confirmation dialog
            Logging.GUI.topic("screen").warn("Closing with unsaved changes");
        }
        Logging.GUI.topic("screen").info("FieldCustomizerScreen closed");
        super.close();
    }
}
