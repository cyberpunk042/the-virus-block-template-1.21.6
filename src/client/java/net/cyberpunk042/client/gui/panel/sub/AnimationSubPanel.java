package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ExpandableSection;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.AlphaPulseConfig;
import net.cyberpunk042.visual.animation.PulseConfig;
import net.cyberpunk042.visual.animation.PulseMode;
import net.cyberpunk042.visual.animation.SpinConfig;
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
    
    private int startY;    private CyclingButtonWidget<String> fragmentDropdown;
    private boolean applyingFragment = false;
    private String currentFragment = "Default";
    
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
    
    public AnimationSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("AnimationSubPanel created");
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

        // Preset dropdown - show "Custom" since we're loading existing primitive values
        List<String> animPresets = FragmentRegistry.listAnimationFragments();
        currentFragment = "Custom";  // Loaded primitives have custom values

        fragmentDropdown = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal(v))
            .values(animPresets)
            .initially(currentFragment)
            .build(x, y, w, GuiConstants.COMPACT_HEIGHT, net.minecraft.text.Text.literal("Variant"),
                (btn, val) -> applyPreset(val));
        widgets.add(fragmentDropdown);
        y += GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        
        // ═══════════════════════════════════════════════════════════════════════
        // G76-G77: SPIN
        // ═══════════════════════════════════════════════════════════════════════
        
        // Handle nullable spin config
        SpinConfig spin = state.spin();
        boolean spinActive = spin != null && spin.isActive();
        float spinSpd = spin != null ? spin.speed() : 45f;
        
        spinToggle = GuiWidgets.compactToggle(x, y, halfW, "Spin", spinActive, "Enable rotation", enabled -> {
            onUserChange(() -> {
                // Toggle by setting speed to 0 (off) or default (on)
                if (!enabled) state.set("spin.speed", 0f);
                else if (state.getFloat("spin.speed") == 0f) state.set("spin.speed", 45f);
                updateSpinWidgets();
                Logging.GUI.topic("animation").debug("Spin: {}", enabled);
            });
        });
        widgets.add(spinToggle);
        
        spinAxis = GuiWidgets.compactEnumDropdown(x + halfW + GuiConstants.PADDING, y, halfW, SpinAxis.class, SpinAxis.Y, "Rotation axis", axis -> {
            onUserChange(() -> {
                state.set("spin.axis", axis.name());
                Logging.GUI.topic("animation").trace("Spin axis: {}", axis);
            });
        });
        widgets.add(spinAxis);
        y += GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        
        spinSpeed = LabeledSlider.builder("Speed (°/s)")
            .position(x, y).width(w).compact()
            .range(-360f, 360f).initial(spinSpd).format("%.0f")
            .onChange(v -> onUserChange(() -> {
                state.set("spin.speed", v);
                Logging.GUI.topic("animation").trace("Spin speed: {}", v);
            }))
            .build();
        widgets.add(spinSpeed);
        y += GuiConstants.COMPACT_HEIGHT + GuiConstants.PADDING;
        
        // ═══════════════════════════════════════════════════════════════════════
        // G78-G79: PULSE
        // ═══════════════════════════════════════════════════════════════════════
        
        // Handle nullable pulse config
        PulseConfig pulse = state.pulse();
        boolean pulseActive = pulse != null && pulse.isActive();
        
        pulseToggle = GuiWidgets.compactToggle(x, y, halfW, "Pulse", pulseActive, "Enable pulsing", enabled -> {
            onUserChange(() -> {
                // Toggle by setting speed to 0 (off) or default (on)
                if (!enabled) state.set("pulse.speed", 0f);
                else if (state.getFloat("pulse.speed") == 0f) state.set("pulse.speed", 1f);
                updatePulseWidgets();
                Logging.GUI.topic("animation").debug("Pulse: {}", enabled);
            });
        });
        widgets.add(pulseToggle);
        
        // Get current pulse mode from state (default to SCALE if not set)
        PulseMode currentPulseMode = pulse != null ? pulse.mode() : PulseMode.SCALE;
        pulseMode = GuiWidgets.compactEnumDropdown(x + halfW + GuiConstants.PADDING, y, halfW, PulseMode.class, currentPulseMode, "Pulse effect", mode -> {
            onUserChange(() -> {
                state.set("pulse.mode", mode.name());
                Logging.GUI.topic("animation").trace("Pulse mode: {}", mode);
            });
        });
        widgets.add(pulseMode);
        y += GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        
        pulseFrequency = LabeledSlider.builder("Freq (Hz)")
            .position(x, y).width(halfW).compact()
            .range(0.1f, 5f).initial(state.getFloat("pulse.speed")).format("%.1f")
            .onChange(v -> onUserChange(() -> {
                state.set("pulse.speed", v);
                Logging.GUI.topic("animation").trace("Pulse freq: {}", v);
            }))
            .build();
        widgets.add(pulseFrequency);
        
        pulseAmplitude = LabeledSlider.builder("Scale")
            .position(x + halfW + GuiConstants.PADDING, y).width(halfW).compact()
            .range(0f, 2f).initial(state.getFloat("pulse.scale")).format("%.2f")
            .onChange(v -> onUserChange(() -> {
                state.set("pulse.scale", v);
                Logging.GUI.topic("animation").trace("Pulse scale: {}", v);
            }))
            .build();
        widgets.add(pulseAmplitude);
        y += GuiConstants.COMPACT_HEIGHT + GuiConstants.PADDING;
        
        // ═══════════════════════════════════════════════════════════════════════
        // G80: ALPHA ANIMATION
        // ═══════════════════════════════════════════════════════════════════════
        
        alphaFadeToggle = GuiWidgets.compactToggle(x, y, w, "Alpha", state.alphaPulse().isActive(), "Enable alpha cycling", enabled -> {
            onUserChange(() -> {
                // Toggle by setting speed to 0 (off) or default (on)
                if (!enabled) state.set("alphaPulse.speed", 0f);
                else if (state.getFloat("alphaPulse.speed") == 0f) state.set("alphaPulse.speed", 1f);
                updateAlphaWidgets();
                Logging.GUI.topic("animation").debug("Alpha fade: {}", enabled);
            });
        });
        widgets.add(alphaFadeToggle);
        y += GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        
        alphaMin = LabeledSlider.builder("Min")
            .position(x, y).width(halfW).compact()
            .range(0f, 1f).initial(state.getFloat("alphaPulse.min")).format("%.2f")
            .onChange(v -> onUserChange(() -> {
                state.set("alphaPulse.min", v);
                Logging.GUI.topic("animation").trace("Alpha min: {}", v);
            }))
            .build();
        widgets.add(alphaMin);
        
        alphaMax = LabeledSlider.builder("Max")
            .position(x + halfW + GuiConstants.PADDING, y).width(halfW).compact()
            .range(0f, 1f).initial(state.getFloat("alphaPulse.max")).format("%.2f")
            .onChange(v -> onUserChange(() -> {
                state.set("alphaPulse.max", v);
                Logging.GUI.topic("animation").trace("Alpha max: {}", v);
            }))
            .build();
        widgets.add(alphaMax);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        contentHeight = y - startY + GuiConstants.PADDING;
        
        updateSpinWidgets();
        updatePulseWidgets();
        updateAlphaWidgets();
        
        Logging.GUI.topic("panel").debug("AnimationSubPanel initialized");
    }
    
    private void updateSpinWidgets() {
        SpinConfig spin = state.spin();
        boolean enabled = spin != null && spin.isActive();
        if (spinAxis != null) spinAxis.active = enabled;
        if (spinSpeed != null) spinSpeed.active = enabled;
    }
    
    private void updatePulseWidgets() {
        PulseConfig pulse = state.pulse();
        boolean enabled = pulse != null && pulse.isActive();
        if (pulseMode != null) pulseMode.active = enabled;
        if (pulseFrequency != null) pulseFrequency.active = enabled;
        if (pulseAmplitude != null) pulseAmplitude.active = enabled;
    }
    
    private void updateAlphaWidgets() {
        AlphaPulseConfig alphaPulse = state.alphaPulse();
        boolean enabled = alphaPulse != null && alphaPulse.isActive();
        if (alphaMin != null) alphaMin.active = enabled;
        if (alphaMax != null) alphaMax.active = enabled;
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

    private void onUserChange(Runnable r) {
        r.run();
        if (!applyingFragment) {
            currentFragment = "Custom";
            if (fragmentDropdown != null) fragmentDropdown.setValue("Custom");
        }
    }

    private void applyPreset(String name) {
        if ("Custom".equalsIgnoreCase(name)) {
            currentFragment = "Custom";
            return;
        }
        applyingFragment = true;
        currentFragment = name;
        FragmentRegistry.applyAnimationFragment(state, name);
        syncFromState();
        applyingFragment = false;
    }

    private void syncFromState() {
        // Handle nullable configs
        SpinConfig spin = state.spin();
        PulseConfig pulse = state.pulse();
        
        if (spinToggle != null) spinToggle.setValue(spin != null && spin.isActive());
        if (spinAxis != null) {
            try { spinAxis.setValue(SpinAxis.valueOf(state.getString("spin.axis"))); } catch (IllegalArgumentException ignored) {}
        }
        if (spinSpeed != null) spinSpeed.setValue(spin != null ? spin.speed() : 45f);
        if (pulseToggle != null) pulseToggle.setValue(pulse != null && pulse.isActive());
        if (pulseMode != null) {
            try { pulseMode.setValue(pulse != null ? pulse.mode() : PulseMode.SCALE); } catch (Exception ignored) {}
        }
        if (pulseFrequency != null) pulseFrequency.setValue(pulse != null ? pulse.speed() : 1f);
        if (pulseAmplitude != null) pulseAmplitude.setValue(pulse != null ? pulse.scale() : 0.1f);
        if (alphaFadeToggle != null) alphaFadeToggle.setValue(state.alphaPulse().isActive());
        if (alphaMin != null) alphaMin.setValue(state.getFloat("alphaPulse.min"));
        if (alphaMax != null) alphaMax.setValue(state.getFloat("alphaPulse.max"));
        updateSpinWidgets();
        updatePulseWidgets();
        updateAlphaWidgets();
    }
}
