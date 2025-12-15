package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.visual.animation.WobbleConfig;
import net.cyberpunk042.visual.animation.WaveConfig;
import net.cyberpunk042.visual.animation.WaveConfig.WaveMode;
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
public class ModifiersSubPanel extends AbstractPanel {
    private int startY;
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
    private LabeledSlider waveSpeed;
    private CyclingButtonWidget<WaveDirection> waveDirection;
    private CyclingButtonWidget<WaveMode> waveMode;
    
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
        
        int fullW = width - 2 * GuiConstants.PADDING;
        int halfW = (fullW - GuiConstants.PADDING) / 2;
        int leftX = GuiConstants.PADDING;
        int rightX = leftX + halfW + GuiConstants.PADDING;
        
        int currentY = GuiConstants.PADDING;
        this.startY = currentY;
        
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        int gap = GuiConstants.COMPACT_GAP;
        int sliderH = GuiConstants.COMPACT_HEIGHT;
        int widgetH = GuiConstants.WIDGET_HEIGHT;
        
        // ═══════════════════════════════════════════════════════════════════════
        // FIELD MODIFIERS - Bobbing & Breathing side by side
        // ═══════════════════════════════════════════════════════════════════════
        
        bobbingSlider = LabeledSlider.builder("Bobbing")
            .position(leftX, currentY).width(halfW).compact()
            .range(0f, 1f).initial(state.getFloat("modifiers.bobbing")).format("%.2f")
            .onChange(v -> { state.set("modifiers.bobbing", v); })
            .build();
        widgets.add(bobbingSlider);
        
        breathingSlider = LabeledSlider.builder("Breathing")
            .position(rightX, currentY).width(halfW).compact()
            .range(0f, 1f).initial(state.getFloat("modifiers.breathing")).format("%.2f")
            .onChange(v -> { state.set("modifiers.breathing", v); })
            .build();
        widgets.add(breathingSlider);
        currentY += sliderH + gap;
        
        // ═══════════════════════════════════════════════════════════════════════
        // COLOR CYCLE - Two checkboxes on one line, slider below
        // ═══════════════════════════════════════════════════════════════════════
        
        colorCycleEnabled = GuiWidgets.checkbox(leftX, currentY, "Color Cycle", state.colorCycle().isActive(),
            "Enable color cycling animation", textRenderer, v -> {
                if (!v) {
                    state.set("colorCycle.speed", 0f);
                } else {
                    if (state.colorCycle().colors() == null || state.colorCycle().colors().isEmpty()) {
                        state.set("colorCycle", net.cyberpunk042.visual.animation.ColorCycleConfig.builder()
                            .colors("#FF0000", "#FF7F00", "#FFFF00", "#00FF00", "#0000FF", "#8B00FF")
                            .speed(1.0f)
                            .blend(true)
                            .build());
                    } else if (state.getFloat("colorCycle.speed") == 0f) {
                        state.set("colorCycle.speed", 1f);
                    }
                }
            });
        widgets.add(colorCycleEnabled);
        
        colorCycleBlend = GuiWidgets.checkbox(rightX, currentY, "Smooth Blend", state.getBool("colorCycle.blend"),
            "Smooth color transitions", textRenderer, v -> state.set("colorCycle.blend", v));
        widgets.add(colorCycleBlend);
        currentY += widgetH + gap;
        
        colorCycleSpeed = LabeledSlider.builder("Speed")
            .position(leftX, currentY).width(fullW).compact()
            .range(0.1f, 5f).initial(state.getFloat("colorCycle.speed")).format("%.1f")
            .onChange(v -> { state.set("colorCycle.speed", v); })
            .build();
        widgets.add(colorCycleSpeed);
        currentY += sliderH + gap;
        
        // ═══════════════════════════════════════════════════════════════════════
        // WOBBLE - Checkbox + amplitude/speed sliders side by side
        // ═══════════════════════════════════════════════════════════════════════
        
        WobbleConfig wobble = state.wobble();
        boolean wobbleActive = wobble != null && wobble.isActive();
        float wobbleAmp = wobble != null && wobble.amplitude() != null 
            ? wobble.amplitude().x : 0.1f;
        float wobbleSpd = wobble != null ? wobble.speed() : 1.0f;
        
        wobbleEnabled = GuiWidgets.checkbox(leftX, currentY, "Wobble", wobbleActive,
            "Random position jitter", textRenderer, v -> { 
                if (!v) {
                    state.set("wobble", WobbleConfig.NONE);
                } else {
                    WobbleConfig current = state.wobble();
                    if (current == null || current.amplitude() == null || !current.isActive()) {
                        state.set("wobble.amplitude", new org.joml.Vector3f(0.1f, 0.05f, 0.1f));
                        if (current == null || current.speed() <= 0) {
                            state.set("wobble.speed", 1.0f);
                        }
                    }
                }
            });
        widgets.add(wobbleEnabled);
        currentY += widgetH + gap;
        
        wobbleAmplitude = LabeledSlider.builder("Amplitude")
            .position(leftX, currentY).width(halfW).compact()
            .range(0f, 1f).initial(wobbleAmp).format("%.2f")
            .onChange(v -> { 
                state.set("wobble.amplitude", new org.joml.Vector3f(v, v * 0.5f, v)); 
            })
            .build();
        widgets.add(wobbleAmplitude);
        
        wobbleSpeed = LabeledSlider.builder("Speed")
            .position(rightX, currentY).width(halfW).compact()
            .range(0.1f, 5f).initial(wobbleSpd).format("%.1f")
            .onChange(v -> { state.set("wobble.speed", v); })
            .build();
        widgets.add(wobbleSpeed);
        currentY += sliderH + gap;
        
        // ═══════════════════════════════════════════════════════════════════════
        // WAVE - Checkbox, amplitude/frequency, speed/direction, mode
        // ═══════════════════════════════════════════════════════════════════════
        
        WaveConfig wave = state.wave();
        boolean waveActive = wave != null && wave.isActive();
        float waveAmp = wave != null ? wave.amplitude() : 0.1f;
        float waveSpd = wave != null ? wave.speed() : 1.0f;
        
        waveEnabled = GuiWidgets.checkbox(leftX, currentY, "Wave Deformation", waveActive,
            "Surface wave animation", textRenderer, v -> { if (!v) state.set("wave", WaveConfig.NONE); });
        widgets.add(waveEnabled);
        currentY += widgetH + gap;
        
        waveAmplitude = LabeledSlider.builder("Amplitude")
            .position(leftX, currentY).width(halfW).compact()
            .range(0f, 1f).initial(waveAmp).format("%.2f")
            .onChange(v -> { state.set("wave.amplitude", v); })
            .build();
        widgets.add(waveAmplitude);
        
        waveFrequency = LabeledSlider.builder("Frequency")
            .position(rightX, currentY).width(halfW).compact()
            .range(0.1f, 10f).initial(state.getFloat("wave.frequency")).format("%.1f")
            .onChange(v -> { state.set("wave.frequency", v); })
            .build();
        widgets.add(waveFrequency);
        currentY += sliderH + gap;
        
        waveSpeed = LabeledSlider.builder("Speed")
            .position(leftX, currentY).width(halfW).compact()
            .range(0.1f, 5f).initial(waveSpd).format("%.1f")
            .onChange(v -> { state.set("wave.speed", v); })
            .build();
        widgets.add(waveSpeed);
        
        // Direction dropdown (right side)
        waveDirection = GuiWidgets.enumDropdown(
            rightX, currentY, halfW, "Direction", WaveDirection.class, WaveDirection.Y,
            "Wave propagation direction",
            v -> state.set("wave.direction", v.name())
        );
        try {
            waveDirection.setValue(WaveDirection.valueOf(state.getString("wave.direction")));
        } catch (IllegalArgumentException ignored) {}
        widgets.add(waveDirection);
        currentY += widgetH + gap;
        
        // Mode dropdown (full width)
        WaveMode currentMode = wave != null && wave.mode() != null ? wave.mode() : WaveMode.GPU;
        waveMode = CyclingButtonWidget.<WaveMode>builder(m -> net.minecraft.text.Text.literal(m.displayName()))
            .values(WaveMode.values())
            .initially(currentMode)
            .build(
                leftX,
                currentY,
                fullW,
                widgetH,
                net.minecraft.text.Text.literal("Mode"),
                (btn, v) -> state.set("wave.mode", v.name())
            );
        widgets.add(waveMode);
    }

    @Override
    public void tick() {
        // No tick logic needed for this panel
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render all widgets
        for (var widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
    }

    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        return widgets;
    }

    public int getContentHeight() {
        // Compact layout calculation:
        // Using COMPACT_HEIGHT (14) for sliders, WIDGET_HEIGHT (20) for checkboxes/dropdowns
        // Gap is COMPACT_GAP (2)
        int sliderH = GuiConstants.COMPACT_HEIGHT;
        int widgetH = GuiConstants.WIDGET_HEIGHT;
        int gap = GuiConstants.COMPACT_GAP;
        
        // Modifiers: bobbing/breathing row
        // ColorCycle: checkbox row + speed slider
        // Wobble: checkbox row + amp/speed row
        // Wave: checkbox row + amp/freq row + speed/dir row + mode row
        return GuiConstants.PADDING 
            + sliderH + gap  // bobbing/breathing
            + widgetH + gap  // color cycle checkboxes
            + sliderH + gap  // color cycle speed
            + widgetH + gap  // wobble checkbox
            + sliderH + gap  // wobble sliders
            + widgetH + gap  // wave checkbox
            + sliderH + gap  // wave amp/freq
            + widgetH + gap  // wave speed/direction (direction is widget height)
            + widgetH        // wave mode
            + GuiConstants.PADDING;
    }
}
