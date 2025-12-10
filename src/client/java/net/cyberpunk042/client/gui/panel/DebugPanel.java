package net.cyberpunk042.client.gui.panel;

import net.cyberpunk042.client.gui.panel.sub.BindingsSubPanel;
import net.cyberpunk042.client.gui.panel.sub.BeamSubPanel;
import net.cyberpunk042.client.gui.panel.sub.LifecycleSubPanel;
import net.cyberpunk042.client.gui.panel.sub.TriggerSubPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

/**
 * G81: Debug panel (Level 3 - Operators only).
 * 
 * <p>Advanced controls for field system debugging:</p>
 * <ul>
 *   <li>Lifecycle state management (spawn, despawn, fade)</li>
 *   <li>Trigger configuration (damage, heal, death)</li>
 *   <li>Binding inspection</li>
 *   <li>Raw JSON export/import</li>
 * </ul>
 * 
 * <p>Access requires operator permissions or debug unlock.</p>
 */
public class DebugPanel extends AbstractPanel {
    
    private LifecycleSubPanel lifecyclePanel;
    private TriggerSubPanel triggerPanel;
    private BindingsSubPanel bindingsPanel;
    private BeamSubPanel beamPanel;
    
    private ButtonWidget testFieldBtn;
    
    private int scrollOffset = 0;
    private int contentHeight = 0;
    
    public DebugPanel(Screen parent, FieldEditState state) {
        super(parent, state);
        Logging.GUI.topic("panel").debug("DebugPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        
        if (!state.isDebugUnlocked()) {
            Logging.GUI.topic("panel").warn("Debug panel accessed without unlock");
        }
        
        int contentY = GuiConstants.TAB_HEIGHT + GuiConstants.PADDING;
        
        // Test Field toggle button (top of debug panel)
        int btnWidth = 140;
        int btnX = (width - btnWidth) / 2;
        testFieldBtn = GuiWidgets.button(
            btnX, contentY, btnWidth,
            getTestFieldButtonLabel(),
            "Spawn/despawn a test field following you",
            () -> FieldEditStateHolder.toggleTestField()
        );
        contentY += 28;
        
        // G82-G85: Lifecycle controls
        lifecyclePanel = new LifecycleSubPanel(parent, state, contentY);
        lifecyclePanel.init(width, height);
        contentY += lifecyclePanel.getHeight() + GuiConstants.SECTION_SPACING;
        
        // G86-G90: Trigger controls
        triggerPanel = new TriggerSubPanel(parent, state, contentY);
        triggerPanel.init(width, height);
        contentY += triggerPanel.getHeight() + GuiConstants.SECTION_SPACING;
        
        // Bindings panel (property <- source mappings)
        bindingsPanel = new BindingsSubPanel(state, 0, contentY, width);
        contentY += bindingsPanel.getHeight() + GuiConstants.SECTION_SPACING;

        // Beam config panel
        beamPanel = new BeamSubPanel(state, 0, contentY, width);
        contentY += beamPanel.getHeight() + GuiConstants.SECTION_SPACING;
        
        contentHeight = contentY;
        
        Logging.GUI.topic("panel").debug("DebugPanel initialized with 4 sub-panels");
    }
    
    @Override
    public void tick() {
        if (lifecyclePanel != null) lifecyclePanel.tick();
        if (triggerPanel != null) triggerPanel.tick();
        
        // Update test field button label
        if (testFieldBtn != null) {
            testFieldBtn.setMessage(net.minecraft.text.Text.literal(getTestFieldButtonLabel()));
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelY = GuiConstants.TAB_HEIGHT;
        context.fill(0, panelY, panelWidth, panelHeight, GuiConstants.BG_PANEL);
        
        // Warning banner if not unlocked
        if (!state.isDebugUnlocked()) {
            context.drawCenteredTextWithShadow(
                net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                "⚠ Debug Mode - Operator Access Required",
                panelWidth / 2, panelY + 4,
                0xFFFF6600
            );
        }
        
        context.enableScissor(0, panelY, panelWidth, panelHeight);
        
        // Render test field button
        if (testFieldBtn != null) {
            testFieldBtn.render(context, mouseX, mouseY, delta);
        }
        
        if (lifecyclePanel != null) {
            lifecyclePanel.setScrollOffset(scrollOffset);
            lifecyclePanel.render(context, mouseX, mouseY + scrollOffset, delta);
        }
        if (triggerPanel != null) {
            triggerPanel.setScrollOffset(scrollOffset);
            triggerPanel.render(context, mouseX, mouseY + scrollOffset, delta);
        }
        if (bindingsPanel != null) {
            bindingsPanel.render(context, mouseX, mouseY + scrollOffset, delta);
        }
        if (beamPanel != null) {
            beamPanel.render(context, mouseX, mouseY + scrollOffset, delta);
        }
        
        context.disableScissor();
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        int maxScroll = Math.max(0, contentHeight - (panelHeight - GuiConstants.TAB_HEIGHT));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(vAmount * 10)));
        return true;
    }
    
    private String getTestFieldButtonLabel() {
        return FieldEditStateHolder.isTestFieldActive() 
            ? "§c✖ Despawn Test Field" 
            : "§a▶ Spawn Test Field";
    }
    
    /**
     * Returns the test field button for Screen to add as a child widget.
     */
    public ButtonWidget getTestFieldButton() {
        return testFieldBtn;
    }
}
