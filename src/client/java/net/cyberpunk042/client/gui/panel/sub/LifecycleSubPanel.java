package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.client.gui.widget.ToastNotification;
import net.cyberpunk042.field.influence.DecayConfig;
import net.cyberpunk042.field.influence.LifecycleConfig;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Lifecycle configuration controls.
 * 
 * <p>Controls spawn/despawn animation timing:</p>
 * <ul>
 *   <li><b>Fade In</b> - ticks for alpha fade during SPAWNING</li>
 *   <li><b>Fade Out</b> - ticks for alpha fade during DESPAWNING</li>
 *   <li><b>Scale In</b> - ticks for scale animation during SPAWNING</li>
 *   <li><b>Scale Out</b> - ticks for scale animation during DESPAWNING</li>
 *   <li><b>Decay Rate</b> - per-tick decay multiplier (0.95 = 5% decay/tick)</li>
 *   <li><b>Decay Min</b> - minimum value decay stops at</li>
 * </ul>
 */
public class LifecycleSubPanel extends AbstractPanel {
    
    private int startY;
    
    // Widgets
    private LabeledSlider fadeInSlider;
    private LabeledSlider fadeOutSlider;
    private LabeledSlider scaleInSlider;
    private LabeledSlider scaleOutSlider;
    private LabeledSlider decayRateSlider;
    private LabeledSlider decayMinSlider;
    private ButtonWidget resetBtn;
    private ButtonWidget presetInstantBtn;
    private ButtonWidget presetSmoothBtn;
    
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
        
        // Get current config
        LifecycleConfig config = getLifecycleConfig();
        
        // ═══════════════════════════════════════════════════════════════════
        // SPAWN ANIMATIONS
        // ═══════════════════════════════════════════════════════════════════
        
        // Fade In
        fadeInSlider = LabeledSlider.builder("Fade In")
            .position(x, y).width(halfW)
            .range(0, 100).initial(config.fadeIn()).format("%d").step(5)
            .onChange(v -> updateLifecycle(cfg -> cfg.toBuilder().fadeIn(v.intValue()).build()))
            .build();
        widgets.add(fadeInSlider);
        
        // Scale In
        scaleInSlider = LabeledSlider.builder("Scale In")
            .position(x + halfW + GuiConstants.PADDING, y).width(halfW)
            .range(0, 100).initial(config.scaleIn()).format("%d").step(5)
            .onChange(v -> updateLifecycle(cfg -> cfg.toBuilder().scaleIn(v.intValue()).build()))
            .build();
        widgets.add(scaleInSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // ═══════════════════════════════════════════════════════════════════
        // DESPAWN ANIMATIONS
        // ═══════════════════════════════════════════════════════════════════
        
        // Fade Out
        fadeOutSlider = LabeledSlider.builder("Fade Out")
            .position(x, y).width(halfW)
            .range(0, 100).initial(config.fadeOut()).format("%d").step(5)
            .onChange(v -> updateLifecycle(cfg -> cfg.toBuilder().fadeOut(v.intValue()).build()))
            .build();
        widgets.add(fadeOutSlider);
        
        // Scale Out
        scaleOutSlider = LabeledSlider.builder("Scale Out")
            .position(x + halfW + GuiConstants.PADDING, y).width(halfW)
            .range(0, 100).initial(config.scaleOut()).format("%d").step(5)
            .onChange(v -> updateLifecycle(cfg -> cfg.toBuilder().scaleOut(v.intValue()).build()))
            .build();
        widgets.add(scaleOutSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // ═══════════════════════════════════════════════════════════════════
        // DECAY (active state gradual fade)
        // ═══════════════════════════════════════════════════════════════════
        
        DecayConfig decay = config.decay() != null ? config.decay() : DecayConfig.NONE;
        
        // Decay Rate (0.90 - 1.00)
        decayRateSlider = LabeledSlider.builder("Decay Rate")
            .position(x, y).width(halfW)
            .range(0.90f, 1.00f).initial(decay.rate()).format("%.2f").step(0.01f)
            .onChange(v -> updateDecay(d -> new DecayConfig(v, d.min())))
            .build();
        widgets.add(decayRateSlider);
        
        // Decay Min (0.0 - 1.0)
        decayMinSlider = LabeledSlider.builder("Decay Min")
            .position(x + halfW + GuiConstants.PADDING, y).width(halfW)
            .range(0.0f, 1.0f).initial(decay.min()).format("%.2f").step(0.05f)
            .onChange(v -> updateDecay(d -> new DecayConfig(d.rate(), v)))
            .build();
        widgets.add(decayMinSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // ═══════════════════════════════════════════════════════════════════
        // PRESETS
        // ═══════════════════════════════════════════════════════════════════
        
        int btnW = (w - GuiConstants.PADDING * 2) / 3;
        
        presetInstantBtn = ButtonWidget.builder(Text.literal("Instant"), btn -> {
                applyPreset(LifecycleConfig.INSTANT);
                ToastNotification.info("Lifecycle: Instant");
            })
            .dimensions(x, y, btnW, GuiConstants.WIDGET_HEIGHT)
            .build();
        widgets.add(presetInstantBtn);
        
        presetSmoothBtn = ButtonWidget.builder(Text.literal("Smooth"), btn -> {
                applyPreset(LifecycleConfig.DEFAULT);
                ToastNotification.info("Lifecycle: Smooth");
            })
            .dimensions(x + btnW + GuiConstants.PADDING, y, btnW, GuiConstants.WIDGET_HEIGHT)
            .build();
        widgets.add(presetSmoothBtn);
        
        resetBtn = ButtonWidget.builder(Text.literal("Reset"), btn -> {
                applyPreset(LifecycleConfig.DEFAULT);
                ToastNotification.info("Lifecycle reset to defaults");
            })
            .dimensions(x + (btnW + GuiConstants.PADDING) * 2, y, btnW, GuiConstants.WIDGET_HEIGHT)
            .build();
        widgets.add(resetBtn);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        contentHeight = y - startY + GuiConstants.PADDING;
        
        Logging.GUI.topic("panel").debug("LifecycleSubPanel initialized with {} widgets", widgets.size());
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private LifecycleConfig getLifecycleConfig() {
        LifecycleConfig config = state.lifecycle();
        return config != null ? config : LifecycleConfig.DEFAULT;
    }
    
    private void updateLifecycle(java.util.function.Function<LifecycleConfig, LifecycleConfig> updater) {
        LifecycleConfig current = getLifecycleConfig();
        LifecycleConfig updated = updater.apply(current);
        state.set("lifecycle", updated);
    }
    
    private void updateDecay(java.util.function.Function<DecayConfig, DecayConfig> updater) {
        LifecycleConfig config = getLifecycleConfig();
        DecayConfig currentDecay = config.decay() != null ? config.decay() : DecayConfig.NONE;
        DecayConfig updatedDecay = updater.apply(currentDecay);
        LifecycleConfig updated = config.toBuilder().decay(updatedDecay).build();
        state.set("lifecycle", updated);
    }
    
    private void applyPreset(LifecycleConfig preset) {
        state.set("lifecycle", preset);
        // Refresh sliders to show new values
        init(panelWidth, panelHeight);
    }
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render widgets
        for (var widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
    }
    
    public int getHeight() {
        return contentHeight;
    }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        return new ArrayList<>(widgets);
    }
}
