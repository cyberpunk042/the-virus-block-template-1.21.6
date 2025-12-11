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
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.util.ArrayList;
import java.util.List;
import net.cyberpunk042.client.network.GuiPacketSender;
import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.cyberpunk042.client.gui.widget.ToastNotification;

/**
 * G82-G85: Lifecycle state controls for the Debug panel.
 * 
 * <ul>
 *   <li>G82: Lifecycle state dropdown (SPAWNING, ACTIVE, DESPAWNING, COMPLETE)</li>
 *   <li>G83: Fade duration slider</li>
 *   <li>G84: Spawn/Despawn buttons</li>
 *   <li>G85: Force state reset button</li>
 * </ul>
 */
public class LifecycleSubPanel extends AbstractPanel {
    
    private int startY;    
    /** Lifecycle states. */
    public enum LifecycleState {
        SPAWNING("Spawning"),
        ACTIVE("Active"),
        DESPAWNING("Despawning"),
        COMPLETE("Complete");
        
        private final String label;
        LifecycleState(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    
    private CyclingButtonWidget<LifecycleState> stateDropdown;
    private LabeledSlider fadeInDuration;
    private LabeledSlider fadeOutDuration;
    private ButtonWidget spawnBtn;
    private ButtonWidget despawnBtn;
    private ButtonWidget resetBtn;
    
    public LifecycleSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("LifecycleSubPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        
        int x = GuiConstants.PADDING;
        int y = startY + GuiConstants.PADDING;
        int w = width - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // G82: Lifecycle state
        stateDropdown = GuiWidgets.enumDropdown(
            x, y, w, "State", LifecycleState.class, LifecycleState.ACTIVE,
            "Current lifecycle state", s -> {
                state.set("lifecycleState", s.name());
                Logging.GUI.topic("lifecycle").info("State changed: {}", s);
            }
        );
        widgets.add(stateDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // G83: Fade durations
        fadeInDuration = LabeledSlider.builder("Fade In (ticks)")
            .position(x, y).width(halfW)
            .range(0, 100).initial(state.getInt("fadeInTicks")).format("%d").step(1)
            .onChange(v -> {
                state.set("fadeInTicks", v.intValue());
                Logging.GUI.topic("lifecycle").trace("Fade in: {}", v.intValue());
            })
            .build();
        widgets.add(fadeInDuration);
        
        fadeOutDuration = LabeledSlider.builder("Fade Out (ticks)")
            .position(x + halfW + GuiConstants.PADDING, y).width(halfW)
            .range(0, 100).initial(state.getInt("fadeOutTicks")).format("%d").step(1)
            .onChange(v -> {
                state.set("fadeOutTicks", v.intValue());
                Logging.GUI.topic("lifecycle").trace("Fade out: {}", v.intValue());
            })
            .build();
        widgets.add(fadeOutDuration);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // G84: Spawn/Despawn buttons
        int btnW = (w - GuiConstants.PADDING * 2) / 3;
        spawnBtn = GuiWidgets.button(x, y, btnW, "Spawn", "Force spawn field", () -> {
            Logging.GUI.topic("lifecycle").info("Force spawn triggered");
            // Use FieldEditStateHolder to set active flag AND send packet
            FieldEditStateHolder.spawnTestField();
        });
        widgets.add(spawnBtn);
        
        despawnBtn = GuiWidgets.button(x + btnW + GuiConstants.PADDING, y, btnW, "Despawn", "Force despawn field", () -> {
            Logging.GUI.topic("lifecycle").info("Force despawn triggered");
            // Use FieldEditStateHolder to clear active flag AND send packet
            FieldEditStateHolder.despawnTestField();
        });
        widgets.add(despawnBtn);
        
        // G85: Reset button
        resetBtn = GuiWidgets.button(x + (btnW + GuiConstants.PADDING) * 2, y, btnW, "Reset", "Reset to defaults", () -> {
            Logging.GUI.topic("lifecycle").info("Lifecycle reset triggered");
            state.set("lifecycleState", "ACTIVE");
            state.set("fadeInTicks", 0);
            state.set("fadeOutTicks", 0);
            ToastNotification.info("Lifecycle reset to defaults");
        });
        widgets.add(resetBtn);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        contentHeight = y - startY + GuiConstants.PADDING;
        
        Logging.GUI.topic("panel").debug("LifecycleSubPanel initialized");
    }
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render widgets directly (no expandable section)
        for (var widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
    }
    
    public int getHeight() {
        return contentHeight;
    }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> all = new ArrayList<>();
        // No header button (direct content)
        all.addAll(widgets);
        return all;
    }
}
