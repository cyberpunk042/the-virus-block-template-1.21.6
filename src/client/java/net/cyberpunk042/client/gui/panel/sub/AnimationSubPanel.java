package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.GuiState;
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
 * G76-G80: Animation controls for the Advanced panel.
 * 
 * <p>Controls for animated effects:</p>
 * <ul>
 *   <li>G76: Spin enable toggle + axis dropdown</li>
 *   <li>G77: Spin speed slider (degrees/sec)</li>
 *   <li>G78: Pulse enable toggle + mode dropdown</li>
 *   <li>G79: Pulse frequency and amplitude sliders</li>
 *   <li>G80: Alpha animation (fade in/out)</li>
 * </ul>
 */
public class AnimationSubPanel extends AbstractPanel {
    
    private ExpandableSection section;
    private int startY;
    
    private final List<net.minecraft.client.gui.widget.ClickableWidget> widgets = new ArrayList<>();
    
    /** Spin axis options. */
    public enum SpinAxis {
        Y("Y (Vertical)"),
        X("X (Horizontal)"),
        Z("Z (Roll)"),
        XY("XY (Tumble)");
        
        private final String label;
        SpinAxis(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    
    /** Pulse mode options. */
    public enum PulseMode {
        SCALE("Scale"),
        ALPHA("Alpha"),
        GLOW("Glow"),
        COLOR("Color");
        
        private final String label;
        PulseMode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    
    // G76-G77: Spin
    private CyclingButtonWidget<Boolean> spinToggle;
    private CyclingButtonWidget<SpinAxis> spinAxis;
    private LabeledSlider spinSpeed;
    
    // G78-G79: Pulse
    private CyclingButtonWidget<Boolean> pulseToggle;
    private CyclingButtonWidget<PulseMode> pulseMode;
    private LabeledSlider pulseFrequency;
    private LabeledSlider pulseAmplitude;
    
    // G80: Alpha animation
    private CyclingButtonWidget<Boolean> alphaFadeToggle;
    private LabeledSlider alphaMin;
    private LabeledSlider alphaMax;
    
    public AnimationSubPanel(Screen parent, GuiState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("AnimationSubPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        section = new ExpandableSection(
            GuiConstants.PADDING, startY,
            width - GuiConstants.PADDING * 2,
            "Animation", false // Start collapsed
        );
        
        int x = GuiConstants.PADDING;
        int y = section.getContentY();
        int w = width - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // ═══════════════════════════════════════════════════════════════════════
        // G76-G77: SPIN
        // ═══════════════════════════════════════════════════════════════════════
        
        spinToggle = GuiWidgets.toggle(x, y, halfW, "Spin", state.isSpinEnabled(), "Enable rotation", enabled -> {
            state.setSpinEnabled(enabled);
            updateSpinWidgets();
            Logging.GUI.topic("animation").debug("Spin: {}", enabled);
        });
        widgets.add(spinToggle);
        
        spinAxis = GuiWidgets.enumDropdown(x + halfW + GuiConstants.PADDING, y, halfW, "Axis", SpinAxis.class, SpinAxis.Y, "Rotation axis", axis -> {
            state.setSpinAxis(axis.name());
            Logging.GUI.topic("animation").trace("Spin axis: {}", axis);
        });
        widgets.add(spinAxis);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        spinSpeed = LabeledSlider.builder("Speed (°/s)")
            .position(x, y).width(w)
            .range(-360f, 360f).initial(state.getSpinSpeed()).format("%.0f")
            .onChange(v -> {
                state.setSpinSpeed(v);
                Logging.GUI.topic("animation").trace("Spin speed: {}", v);
            })
            .build();
        widgets.add(spinSpeed);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.SECTION_SPACING;
        
        // ═══════════════════════════════════════════════════════════════════════
        // G78-G79: PULSE
        // ═══════════════════════════════════════════════════════════════════════
        
        pulseToggle = GuiWidgets.toggle(x, y, halfW, "Pulse", state.isPulseEnabled(), "Enable pulsing", enabled -> {
            state.setPulseEnabled(enabled);
            updatePulseWidgets();
            Logging.GUI.topic("animation").debug("Pulse: {}", enabled);
        });
        widgets.add(pulseToggle);
        
        pulseMode = GuiWidgets.enumDropdown(x + halfW + GuiConstants.PADDING, y, halfW, "Mode", PulseMode.class, PulseMode.SCALE, "Pulse effect", mode -> {
            state.setPulseMode(mode.name());
            Logging.GUI.topic("animation").trace("Pulse mode: {}", mode);
        });
        widgets.add(pulseMode);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        pulseFrequency = LabeledSlider.builder("Frequency (Hz)")
            .position(x, y).width(halfW)
            .range(0.1f, 5f).initial(state.getPulseFrequency()).format("%.1f")
            .onChange(v -> {
                state.setPulseFrequency(v);
                Logging.GUI.topic("animation").trace("Pulse freq: {}", v);
            })
            .build();
        widgets.add(pulseFrequency);
        
        pulseAmplitude = LabeledSlider.builder("Amplitude")
            .position(x + halfW + GuiConstants.PADDING, y).width(halfW)
            .range(0f, 1f).initial(state.getPulseAmplitude()).format("%.2f")
            .onChange(v -> {
                state.setPulseAmplitude(v);
                Logging.GUI.topic("animation").trace("Pulse amp: {}", v);
            })
            .build();
        widgets.add(pulseAmplitude);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.SECTION_SPACING;
        
        // ═══════════════════════════════════════════════════════════════════════
        // G80: ALPHA ANIMATION
        // ═══════════════════════════════════════════════════════════════════════
        
        alphaFadeToggle = GuiWidgets.toggle(x, y, w, "Alpha Fade", state.isAlphaFadeEnabled(), "Enable alpha cycling", enabled -> {
            state.setAlphaFadeEnabled(enabled);
            updateAlphaWidgets();
            Logging.GUI.topic("animation").debug("Alpha fade: {}", enabled);
        });
        widgets.add(alphaFadeToggle);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        alphaMin = LabeledSlider.builder("Min Alpha")
            .position(x, y).width(halfW)
            .range(0f, 1f).initial(state.getAlphaMin()).format("%.2f")
            .onChange(v -> {
                state.setAlphaMin(v);
                Logging.GUI.topic("animation").trace("Alpha min: {}", v);
            })
            .build();
        widgets.add(alphaMin);
        
        alphaMax = LabeledSlider.builder("Max Alpha")
            .position(x + halfW + GuiConstants.PADDING, y).width(halfW)
            .range(0f, 1f).initial(state.getAlphaMax()).format("%.2f")
            .onChange(v -> {
                state.setAlphaMax(v);
                Logging.GUI.topic("animation").trace("Alpha max: {}", v);
            })
            .build();
        widgets.add(alphaMax);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        section.setContentHeight(y - section.getContentY());
        
        updateSpinWidgets();
        updatePulseWidgets();
        updateAlphaWidgets();
        
        Logging.GUI.topic("panel").debug("AnimationSubPanel initialized");
    }
    
    private void updateSpinWidgets() {
        boolean enabled = state.isSpinEnabled();
        if (spinAxis != null) spinAxis.active = enabled;
        if (spinSpeed != null) spinSpeed.active = enabled;
    }
    
    private void updatePulseWidgets() {
        boolean enabled = state.isPulseEnabled();
        if (pulseMode != null) pulseMode.active = enabled;
        if (pulseFrequency != null) pulseFrequency.active = enabled;
        if (pulseAmplitude != null) pulseAmplitude.active = enabled;
    }
    
    private void updateAlphaWidgets() {
        boolean enabled = state.isAlphaFadeEnabled();
        if (alphaMin != null) alphaMin.active = enabled;
        if (alphaMax != null) alphaMax.active = enabled;
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
