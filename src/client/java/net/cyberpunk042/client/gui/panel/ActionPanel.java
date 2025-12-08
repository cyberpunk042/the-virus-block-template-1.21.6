package net.cyberpunk042.client.gui.panel;

import net.cyberpunk042.client.gui.state.GuiState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ToastNotification;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * G57-G60: Action buttons for Quick Panel.
 * 
 * <ul>
 *   <li>G57: Connect widgets to GuiState</li>
 *   <li>G58: Live apply to DEBUG FIELD</li>
 *   <li>G59: "Apply to My Shield" button</li>
 *   <li>G60: Auto-save checkbox</li>
 * </ul>
 */
public class ActionPanel extends AbstractPanel {
    
    private CyclingButtonWidget<Boolean> livePreviewToggle;
    private ButtonWidget applyButton;
    private CyclingButtonWidget<Boolean> autoSaveToggle;
    private ButtonWidget resetButton;
    
    private int startY;
    
    public ActionPanel(Screen parent, GuiState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("ActionPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        
        int x = GuiConstants.PADDING;
        int y = startY;
        int btnWidth = (width - GuiConstants.PADDING * 3) / 2;
        
        // G58: Live preview toggle
        livePreviewToggle = GuiWidgets.toggle(
            x, y, btnWidth,
            "Live Preview", state.isLivePreviewEnabled(), "Apply changes in real-time",
            this::onLivePreviewChanged
        );
        
        // G60: Auto-save toggle
        autoSaveToggle = GuiWidgets.toggle(
            x + btnWidth + GuiConstants.PADDING, y, btnWidth,
            "Auto-save", state.isAutoSaveEnabled(), "Save changes automatically",
            this::onAutoSaveChanged
        );
        
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // G59: Apply button
        applyButton = GuiWidgets.button(
            x, y, btnWidth,
            "Apply to Shield", "Apply current settings to your shield",
            this::applyToShield
        );
        
        // Reset button
        resetButton = GuiWidgets.button(
            x + btnWidth + GuiConstants.PADDING, y, btnWidth,
            "Reset", "Reset to original values",
            this::resetChanges
        );
        
        Logging.GUI.topic("panel").debug("ActionPanel initialized");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G58: LIVE PREVIEW
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void onLivePreviewChanged(boolean enabled) {
        state.setLivePreviewEnabled(enabled);
        if (enabled) {
            ToastNotification.info("Live preview enabled");
            // TODO: Spawn/update DEBUG field
        } else {
            ToastNotification.info("Live preview disabled");
            // TODO: Despawn DEBUG field
        }
        Logging.GUI.topic("action").debug("Live preview: {}", enabled);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G59: APPLY TO SHIELD
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void applyToShield() {
        // TODO: Send packet to server to update player's shield
        state.clearDirty();
        ToastNotification.success("Applied to shield!");
        Logging.GUI.topic("action").info("Applied settings to shield");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G60: AUTO-SAVE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void onAutoSaveChanged(boolean enabled) {
        state.setAutoSaveEnabled(enabled);
        if (enabled) {
            ToastNotification.info("Auto-save enabled");
        }
        Logging.GUI.topic("action").debug("Auto-save: {}", enabled);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RESET
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void resetChanges() {
        // TODO: Reset state to original
        state.clearDirty();
        ToastNotification.info("Reset to original");
        Logging.GUI.topic("action").info("Reset changes");
    }
    
    @Override
    public void tick() {
        // Update apply button state based on dirty flag
        if (applyButton != null) {
            applyButton.active = state.isDirty();
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Section header
        context.drawText(
            net.minecraft.client.MinecraftClient.getInstance().textRenderer,
            "Actions",
            GuiConstants.PADDING,
            startY - 12,
            GuiConstants.TEXT_SECONDARY,
            false
        );
        
        if (livePreviewToggle != null) livePreviewToggle.render(context, mouseX, mouseY, delta);
        if (autoSaveToggle != null) autoSaveToggle.render(context, mouseX, mouseY, delta);
        if (applyButton != null) applyButton.render(context, mouseX, mouseY, delta);
        if (resetButton != null) resetButton.render(context, mouseX, mouseY, delta);
    }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> list = new ArrayList<>();
        if (livePreviewToggle != null) list.add(livePreviewToggle);
        if (autoSaveToggle != null) list.add(autoSaveToggle);
        if (applyButton != null) list.add(applyButton);
        if (resetButton != null) list.add(resetButton);
        return list;
    }
    
    public int getHeight() {
        return (GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING) * 2 + 12;
    }
}
