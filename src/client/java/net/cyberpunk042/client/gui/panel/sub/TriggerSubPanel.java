package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ExpandableSection;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * G86-G90: Trigger configuration for the Debug panel.
 * 
 * <ul>
 *   <li>G86: Trigger type dropdown (DAMAGE, HEAL, DEATH, RESPAWN)</li>
 *   <li>G87: Trigger effect dropdown (FLASH, PULSE, SHAKE, COLOR_SHIFT)</li>
 *   <li>G88: Effect intensity slider</li>
 *   <li>G89: Effect duration slider</li>
 *   <li>G90: Test trigger button</li>
 * </ul>
 */
public class TriggerSubPanel extends AbstractPanel {
    
    private ExpandableSection section;
    private int startY;
    
    private final List<net.minecraft.client.gui.widget.ClickableWidget> widgets = new ArrayList<>();
    
    /** Trigger event types. */
    public enum TriggerType {
        DAMAGE("On Damage"),
        HEAL("On Heal"),
        DEATH("On Death"),
        RESPAWN("On Respawn"),
        COMBAT_ENTER("Combat Enter"),
        COMBAT_EXIT("Combat Exit");
        
        private final String label;
        TriggerType(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    
    /** Trigger effect types. */
    public enum TriggerEffect {
        FLASH("Flash"),
        PULSE("Pulse"),
        SHAKE("Shake"),
        COLOR_SHIFT("Color Shift"),
        SCALE_BUMP("Scale Bump"),
        ALPHA_FADE("Alpha Fade");
        
        private final String label;
        TriggerEffect(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    
    private CyclingButtonWidget<TriggerType> triggerType;
    private CyclingButtonWidget<TriggerEffect> triggerEffect;
    private LabeledSlider effectIntensity;
    private LabeledSlider effectDuration;
    private net.minecraft.client.gui.widget.ButtonWidget testBtn;
    
    public TriggerSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("TriggerSubPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        section = new ExpandableSection(
            GuiConstants.PADDING, startY,
            width - GuiConstants.PADDING * 2,
            "Triggers", false // Start collapsed
        );
        
        int x = GuiConstants.PADDING;
        int y = section.getContentY();
        int w = width - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // G86: Trigger type
        triggerType = GuiWidgets.enumDropdown(
            x, y, halfW, "Event", TriggerType.class, TriggerType.DAMAGE,
            "Trigger event type", t -> {
                state.setTriggerType(t.name());
                Logging.GUI.topic("trigger").debug("Trigger type: {}", t);
            }
        );
        widgets.add(triggerType);
        
        // G87: Trigger effect
        triggerEffect = GuiWidgets.enumDropdown(
            x + halfW + GuiConstants.PADDING, y, halfW, "Effect", TriggerEffect.class, TriggerEffect.FLASH,
            "Visual effect", e -> {
                state.setTriggerEffect(e.name());
                Logging.GUI.topic("trigger").debug("Trigger effect: {}", e);
            }
        );
        widgets.add(triggerEffect);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // G88: Effect intensity
        effectIntensity = LabeledSlider.builder("Intensity")
            .position(x, y).width(halfW)
            .range(0f, 2f).initial(state.getTriggerIntensity()).format("%.2f")
            .onChange(v -> {
                state.setTriggerIntensity(v);
                Logging.GUI.topic("trigger").trace("Intensity: {}", v);
            })
            .build();
        widgets.add(effectIntensity);
        
        // G89: Effect duration
        effectDuration = LabeledSlider.builder("Duration (ticks)")
            .position(x + halfW + GuiConstants.PADDING, y).width(halfW)
            .range(1, 60).initial(state.getTriggerDuration()).format("%d").step(1)
            .onChange(v -> {
                state.setTriggerDuration(v.intValue());
                Logging.GUI.topic("trigger").trace("Duration: {}", v.intValue());
            })
            .build();
        widgets.add(effectDuration);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // G90: Test button
        testBtn = GuiWidgets.button(x, y, w, "Test Trigger", "Fire test trigger effect", () -> {
            Logging.GUI.topic("trigger").info("Test trigger fired: {} -> {}", 
                triggerType.getValue(), triggerEffect.getValue());
            // TODO: Send trigger test command
        });
        widgets.add(testBtn);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        section.setContentHeight(y - section.getContentY());
        
        Logging.GUI.topic("panel").debug("TriggerSubPanel initialized");
    }
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        section.render(context, net.minecraft.client.MinecraftClient.getInstance().textRenderer, mouseX, mouseY, delta);
        
        if (section.isExpanded()) {
            for (var widget : widgets) {
                widget.render(context, mouseX, mouseY, delta);
            }
        }
    }
    
    public int getHeight() {
        return section.getTotalHeight();
    }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> all = new ArrayList<>();
        all.add(section.getHeaderButton());
        all.addAll(widgets);
        return all;
    }
}
