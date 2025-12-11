package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.visual.animation.WobbleConfig;
import net.cyberpunk042.visual.animation.WaveConfig;
import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Field Modifiers and Animation Extras sub-panel.
 * 
 * <p>Controls:</p>
 * <ul>
 *   <li>Modifiers: bobbing, breathing</li>
 *   <li>Color Cycle: enabled, speed, blend</li>
 *   <li>Wobble: enabled, amplitude, speed</li>
 *   <li>Wave: enabled, amplitude, frequency, direction</li>
 * </ul>
 */
public class ModifiersSubPanel extends AbstractPanel {    private int startY;
    private int panelW;
    
    // Field Modifiers
    private LabeledSlider bobbingSlider;
    private LabeledSlider breathingSlider;
    
    // Color Cycle
    private CheckboxWidget colorCycleEnabled;
    private LabeledSlider colorCycleSpeed;
    private CheckboxWidget colorCycleBlend;
    
    // Wobble
    private CheckboxWidget wobbleEnabled;
    private LabeledSlider wobbleAmplitude;
    private LabeledSlider wobbleSpeed;
    
    // Wave
    private CheckboxWidget waveEnabled;
    private LabeledSlider waveAmplitude;
    private LabeledSlider waveFrequency;
    private CyclingButtonWidget<WaveDirection> waveDirection;
    
    public enum WaveDirection {
        X("X Axis"), Y("Y Axis"), Z("Z Axis"), RADIAL("Radial");
        private final String label;
        WaveDirection(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public ModifiersSubPanel(Screen parent, FieldEditState state) {
        super(parent, state);
    }

    @Override
    public void init(int width, int height) {
        widgets.clear();
        this.panelW = width;
        
        int w = width - 2 * GuiConstants.PADDING;
        int currentY = GuiConstants.TAB_HEIGHT + GuiConstants.PADDING;
        int sliderX = GuiConstants.PADDING;
        this.startY = currentY;
        
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        
        // ═══════════════════════════════════════════════════════════════════════
        // FIELD MODIFIERS
        // ═══════════════════════════════════════════════════════════════════════
        
        // Section label drawn in render()
        currentY += 14; // Space for label
        
        bobbingSlider = LabeledSlider.builder("Bobbing")
            .position(sliderX, currentY).width(w)
            .range(0f, 1f).initial(state.getFloat("modifiers.bobbing")).format("%.2f")
            .onChange(v -> { state.set("modifiers.bobbing", v); })
            .build();
        widgets.add(bobbingSlider);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        breathingSlider = LabeledSlider.builder("Breathing")
            .position(sliderX, currentY).width(w)
            .range(0f, 1f).initial(state.getFloat("modifiers.breathing")).format("%.2f")
            .onChange(v -> { state.set("modifiers.breathing", v); })
            .build();
        widgets.add(breathingSlider);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING * 2;
        
        // ═══════════════════════════════════════════════════════════════════════
        // COLOR CYCLE
        // ═══════════════════════════════════════════════════════════════════════
        
        currentY += 14; // Section label
        
        colorCycleEnabled = GuiWidgets.checkbox(sliderX, currentY, "Color Cycle", state.colorCycle().isActive(),
            "Enable color cycling animation", textRenderer, v -> {
                if (!v) state.set("colorCycle.speed", 0f);
                else if (state.getFloat("colorCycle.speed") == 0f) state.set("colorCycle.speed", 1f);
            });
        widgets.add(colorCycleEnabled);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        colorCycleSpeed = LabeledSlider.builder("Speed")
            .position(sliderX, currentY).width(w)
            .range(0.1f, 5f).initial(state.getFloat("colorCycle.speed")).format("%.1f")
            .onChange(v -> { state.set("colorCycle.speed", v); })
            .build();
        widgets.add(colorCycleSpeed);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        colorCycleBlend = GuiWidgets.checkbox(sliderX, currentY, "Smooth Blend", state.getBool("colorCycle.blend"),
            "Smooth color transitions", textRenderer, v -> state.set("colorCycle.blend", v));
        widgets.add(colorCycleBlend);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING * 2;
        
        // ═══════════════════════════════════════════════════════════════════════
        // WOBBLE
        // ═══════════════════════════════════════════════════════════════════════
        
        currentY += 14; // Section label
        
        // Handle nullable wobble config
        WobbleConfig wobble = state.wobble();
        boolean wobbleActive = wobble != null && wobble.isActive();
        float wobbleAmp = wobble != null && wobble.amplitude() != null 
            ? wobble.amplitude().x : 0.1f;
        float wobbleSpd = wobble != null ? wobble.speed() : 1.0f;
        
        wobbleEnabled = GuiWidgets.checkbox(sliderX, currentY, "Wobble", wobbleActive,
            "Random position jitter", textRenderer, v -> { if (!v) state.set("wobble", WobbleConfig.NONE); });
        widgets.add(wobbleEnabled);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        wobbleAmplitude = LabeledSlider.builder("Amplitude")
            .position(sliderX, currentY).width(w)
            .range(0f, 1f).initial(wobbleAmp).format("%.2f")
            .onChange(v -> { 
                // Set as uniform Vector3f
                state.set("wobble.amplitude", new org.joml.Vector3f(v, v * 0.5f, v)); 
            })
            .build();
        widgets.add(wobbleAmplitude);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        wobbleSpeed = LabeledSlider.builder("Speed")
            .position(sliderX, currentY).width(w)
            .range(0.1f, 5f).initial(wobbleSpd).format("%.1f")
            .onChange(v -> { state.set("wobble.speed", v); })
            .build();
        widgets.add(wobbleSpeed);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING * 2;
        
        // ═══════════════════════════════════════════════════════════════════════
        // WAVE
        // ═══════════════════════════════════════════════════════════════════════
        
        currentY += 14; // Section label
        
        // Handle nullable wave config
        WaveConfig wave = state.wave();
        boolean waveActive = wave != null && wave.isActive();
        float waveAmp = wave != null ? wave.amplitude() : 0.1f;
        
        waveEnabled = GuiWidgets.checkbox(sliderX, currentY, "Wave Deformation", waveActive,
            "Surface wave animation", textRenderer, v -> { if (!v) state.set("wave", WaveConfig.NONE); });
        widgets.add(waveEnabled);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        waveAmplitude = LabeledSlider.builder("Amplitude")
            .position(sliderX, currentY).width(w)
            .range(0f, 1f).initial(waveAmp).format("%.2f")
            .onChange(v -> { state.set("wave.amplitude", v); })
            .build();
        widgets.add(waveAmplitude);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        waveFrequency = LabeledSlider.builder("Frequency")
            .position(sliderX, currentY).width(w)
            .range(0.1f, 10f).initial(state.getFloat("wave.frequency")).format("%.1f")
            .onChange(v -> { state.set("wave.frequency", v); })
            .build();
        widgets.add(waveFrequency);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        waveDirection = GuiWidgets.enumDropdown(
            sliderX, currentY, w, "Direction", WaveDirection.class, WaveDirection.Y,
            "Wave propagation direction",
            v -> state.set("wave.direction", v.name())
        );
        try {
            waveDirection.setValue(WaveDirection.valueOf(state.getString("wave.direction")));
        } catch (IllegalArgumentException ignored) {}
        widgets.add(waveDirection);
    }

    @Override
    public void tick() {
        // No tick logic needed for this panel
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        
        // Section labels
        int labelX = GuiConstants.PADDING + 4;
        int labelY = startY;
        context.drawText(textRenderer, "§e§lField Modifiers", labelX, labelY, GuiConstants.TEXT_PRIMARY, false);
        
        // Find color cycle section
        if (colorCycleEnabled != null) {
            int ccY = colorCycleEnabled.getY() - 14;
            context.drawText(textRenderer, "§b§lColor Cycle", labelX, ccY, GuiConstants.TEXT_PRIMARY, false);
        }
        
        // Find wobble section
        if (wobbleEnabled != null) {
            int wobbleY = wobbleEnabled.getY() - 14;
            context.drawText(textRenderer, "§d§lWobble", labelX, wobbleY, GuiConstants.TEXT_PRIMARY, false);
        }
        
        // Find wave section
        if (waveEnabled != null) {
            int waveY = waveEnabled.getY() - 14;
            context.drawText(textRenderer, "§a§lWave", labelX, waveY, GuiConstants.TEXT_PRIMARY, false);
        }
        
        // Render all widgets
        for (var widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
    }

    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        return widgets;
    }

    public int getContentHeight() {
        // Approximate: 4 sections, each with ~3 controls
        return 4 * (14 + 3 * (GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING) + GuiConstants.PADDING);
    }
}
