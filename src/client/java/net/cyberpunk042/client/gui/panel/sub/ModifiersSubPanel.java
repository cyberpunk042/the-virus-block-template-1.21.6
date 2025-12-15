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
        
        int fullW = width - 2 * GuiConstants.PADDING;
        int halfW = (fullW - GuiConstants.PADDING) / 2;  // Two sliders with gap
        int leftX = GuiConstants.PADDING;
        int rightX = leftX + halfW + GuiConstants.PADDING;
        
        int currentY = GuiConstants.PADDING;
        this.startY = currentY;
        
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        
        // ═══════════════════════════════════════════════════════════════════════
        // FIELD MODIFIERS - Bobbing & Breathing side by side
        // ═══════════════════════════════════════════════════════════════════════
        
        currentY += 12; // Section label space
        
        bobbingSlider = LabeledSlider.builder("Bobbing")
            .position(leftX, currentY).width(halfW)
            .range(0f, 1f).initial(state.getFloat("modifiers.bobbing")).format("%.2f")
            .onChange(v -> { state.set("modifiers.bobbing", v); })
            .build();
        widgets.add(bobbingSlider);
        
        breathingSlider = LabeledSlider.builder("Breathing")
            .position(rightX, currentY).width(halfW)
            .range(0f, 1f).initial(state.getFloat("modifiers.breathing")).format("%.2f")
            .onChange(v -> { state.set("modifiers.breathing", v); })
            .build();
        widgets.add(breathingSlider);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // ═══════════════════════════════════════════════════════════════════════
        // COLOR CYCLE - Two checkboxes on one line, slider below
        // ═══════════════════════════════════════════════════════════════════════
        
        currentY += 10; // Section label
        
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
        currentY += GuiConstants.WIDGET_HEIGHT + 2;
        
        colorCycleSpeed = LabeledSlider.builder("Speed")
            .position(leftX, currentY).width(fullW)
            .range(0.1f, 5f).initial(state.getFloat("colorCycle.speed")).format("%.1f")
            .onChange(v -> { state.set("colorCycle.speed", v); })
            .build();
        widgets.add(colorCycleSpeed);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // ═══════════════════════════════════════════════════════════════════════
        // WOBBLE - Checkbox + amplitude/speed sliders side by side
        // ═══════════════════════════════════════════════════════════════════════
        
        currentY += 10; // Section label
        
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
        currentY += GuiConstants.WIDGET_HEIGHT + 2;
        
        wobbleAmplitude = LabeledSlider.builder("Amplitude")
            .position(leftX, currentY).width(halfW)
            .range(0f, 1f).initial(wobbleAmp).format("%.2f")
            .onChange(v -> { 
                state.set("wobble.amplitude", new org.joml.Vector3f(v, v * 0.5f, v)); 
            })
            .build();
        widgets.add(wobbleAmplitude);
        
        wobbleSpeed = LabeledSlider.builder("Speed")
            .position(rightX, currentY).width(halfW)
            .range(0.1f, 5f).initial(wobbleSpd).format("%.1f")
            .onChange(v -> { state.set("wobble.speed", v); })
            .build();
        widgets.add(wobbleSpeed);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // ═══════════════════════════════════════════════════════════════════════
        // WAVE - Checkbox, then amplitude/frequency side by side, then direction
        // ═══════════════════════════════════════════════════════════════════════
        
        currentY += 10; // Section label
        
        WaveConfig wave = state.wave();
        boolean waveActive = wave != null && wave.isActive();
        float waveAmp = wave != null ? wave.amplitude() : 0.1f;
        
        waveEnabled = GuiWidgets.checkbox(leftX, currentY, "Wave Deformation", waveActive,
            "Surface wave animation", textRenderer, v -> { if (!v) state.set("wave", WaveConfig.NONE); });
        widgets.add(waveEnabled);
        currentY += GuiConstants.WIDGET_HEIGHT + 2;
        
        waveAmplitude = LabeledSlider.builder("Amplitude")
            .position(leftX, currentY).width(halfW)
            .range(0f, 1f).initial(waveAmp).format("%.2f")
            .onChange(v -> { state.set("wave.amplitude", v); })
            .build();
        widgets.add(waveAmplitude);
        
        waveFrequency = LabeledSlider.builder("Frequency")
            .position(rightX, currentY).width(halfW)
            .range(0.1f, 10f).initial(state.getFloat("wave.frequency")).format("%.1f")
            .onChange(v -> { state.set("wave.frequency", v); })
            .build();
        widgets.add(waveFrequency);
        currentY += GuiConstants.WIDGET_HEIGHT + 2;
        
        waveDirection = GuiWidgets.enumDropdown(
            leftX, currentY, fullW, "Direction", WaveDirection.class, WaveDirection.Y,
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
            int ccY = colorCycleEnabled.getY() - 12;
            context.drawText(textRenderer, "§b§lColor Cycle", labelX, ccY, GuiConstants.TEXT_PRIMARY, false);
        }
        
        // Find wobble section
        if (wobbleEnabled != null) {
            int wobbleY = wobbleEnabled.getY() - 12;
            context.drawText(textRenderer, "§d§lWobble", labelX, wobbleY, GuiConstants.TEXT_PRIMARY, false);
        }
        
        // Find wave section
        if (waveEnabled != null) {
            int waveY = waveEnabled.getY() - 12;
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
        // More compact calculation:
        // 4 sections: each section header (12) + rows
        // Modifiers: 1 row (sliders)
        // ColorCycle: 2 rows (checkboxes + slider)
        // Wobble: 2 rows (checkbox + sliders)
        // Wave: 3 rows (checkbox + sliders + dropdown)
        int rows = 1 + 2 + 2 + 3;
        return 4 * 12 + rows * (GuiConstants.WIDGET_HEIGHT + 2) + 3 * GuiConstants.PADDING;
    }
}
