package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.RendererCapabilities.Feature;
import net.cyberpunk042.client.gui.state.RequiresFeature;
import net.minecraft.client.gui.screen.Screen;

import net.cyberpunk042.visual.animation.PulseConfig;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiLayout;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.visual.animation.Waveform;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;


import java.util.ArrayList;
import java.util.List;

/**
 * BeamSubPanel - Debug tab: central beam configuration.
 *
 * Controls:
 * - Enable beam toggle
 * - Inner/Outer radius
 * - Height
 * - Glow
 * - Color reference/text
 * - Pulse: enable, scale, speed, waveform, min/max
 * 
 * <p>NOTE: No ExpandableSection - content displays directly in sub-tab.</p>
 * 
 * <p><b>Requires Accurate renderer mode.</b></p>
 */
@RequiresFeature(Feature.BEAM)
public class BeamSubPanel extends AbstractPanel {
    private GuiLayout layout;
    private CyclingButtonWidget<String> fragmentDropdown;
    private boolean applyingFragment = false;
    private String currentFragment = "Default";

    // Widgets
    private CyclingButtonWidget<Boolean> enableToggle;
    private LabeledSlider innerRadius;
    private LabeledSlider outerRadius;
    private LabeledSlider height;
    private LabeledSlider glow;
    private TextFieldWidget colorField;

    // Pulse
    private CyclingButtonWidget<Boolean> pulseToggle;
    private LabeledSlider pulseScale;
    private LabeledSlider pulseSpeed;
    private CyclingButtonWidget<Waveform> pulseWaveform;
    private LabeledSlider pulseMin;
    private LabeledSlider pulseMax;

    public BeamSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        int x = GuiConstants.PADDING;
        int y = GuiConstants.PADDING;
        this.layout = new GuiLayout(x, y, GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING, width - GuiConstants.PADDING * 2);
        build();
    }

    private void build() {
        int controlWidth = layout.getPanelWidth();
        int halfWidth = (controlWidth - GuiConstants.ELEMENT_SPACING) / 2;

        // Preset dropdown - show "Custom" since we're loading existing values
        List<String> beamPresets = FragmentRegistry.listBeamFragments();
        currentFragment = "Custom";  // Loaded values are custom

        fragmentDropdown = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal(v))
            .values(beamPresets)
            .initially(currentFragment)
            .build(layout.getStartX(), layout.getY(), controlWidth, GuiConstants.WIDGET_HEIGHT,
                net.minecraft.text.Text.literal("Variant"), (btn, val) -> applyPreset(val));
        widgets.add(fragmentDropdown);
        layout.nextRow();

        enableToggle = GuiWidgets.toggle(
            layout.getStartX(), layout.getY(), controlWidth,
            "Beam Enabled", state.getBool("beam.enabled"), "Toggle central beam",
            v -> onUserChange(() -> state.set("beam.enabled", v))
        );
        widgets.add(enableToggle);
        layout.nextRow();

        innerRadius = LabeledSlider.builder("Inner Radius")
            .position(layout.getStartX(), layout.getY())
            .width(controlWidth)
            .range(0.01f, 5.0f)
            .initial(state.getFloat("beam.innerRadius"))
            .format("%.3f")
            .onChange(v -> onUserChange(() -> state.set("beam.innerRadius", v)))
            .build();
        widgets.add(innerRadius);
        layout.nextRow();

        outerRadius = LabeledSlider.builder("Outer Radius")
            .position(layout.getStartX(), layout.getY())
            .width(controlWidth)
            .range(0.02f, 10.0f)
            .initial(state.getFloat("beam.outerRadius"))
            .format("%.3f")
            .onChange(v -> onUserChange(() -> state.set("beam.outerRadius", v)))
            .build();
        widgets.add(outerRadius);
        layout.nextRow();

        height = LabeledSlider.builder("Height")
            .position(layout.getStartX(), layout.getY())
            .width(controlWidth)
            .range(0.5f, 64f)
            .initial(state.getFloat("beam.height"))
            .format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("beam.height", v)))
            .build();
        widgets.add(height);
        layout.nextRow();

        glow = LabeledSlider.builder("Glow")
            .position(layout.getStartX(), layout.getY())
            .width(controlWidth)
            .range(0f, 2f)
            .initial(state.getFloat("beam.glow"))
            .format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("beam.glow", v)))
            .build();
        widgets.add(glow);
        layout.nextRow();

        colorField = new TextFieldWidget(
            net.minecraft.client.MinecraftClient.getInstance().textRenderer,
            layout.getStartX(), layout.getY(),
            controlWidth, GuiConstants.WIDGET_HEIGHT,
            net.minecraft.text.Text.literal("Beam Color")
        );
        // beam.color is a String reference (e.g., "@beam", "#FF00FF")
        String beamColor = state.getString("beam.color");
        colorField.setText(beamColor != null ? beamColor : "@primary");
        colorField.setChangedListener(v -> onUserChange(() -> state.set("beam.color", v)));
        widgets.add(colorField);
        layout.nextRow();

        // Pulse section header (simple text)
        layout.nextRow(); // small spacer

        // Handle nullable pulse config
        PulseConfig pulse = state.beam() != null ? state.beam().pulse() : null;
        boolean pulseActive = pulse != null && pulse.isActive();
        pulseToggle = GuiWidgets.toggle(
            layout.getStartX(), layout.getY(), controlWidth,
            "Pulse Enabled", pulseActive, "Enable beam pulsing",
            v -> onUserChange(() -> { if (!v) state.set("beam.pulse", PulseConfig.NONE); })
        );
        widgets.add(pulseToggle);
        layout.nextRow();

        // Use safe defaults when pulse is null
        float initScale = pulse != null ? pulse.scale() : 0.1f;
        float initSpeed = pulse != null ? pulse.speed() : 1.0f;
        float initMin = pulse != null ? pulse.min() : 0.9f;
        float initMax = pulse != null ? pulse.max() : 1.1f;
        Waveform initWaveform = pulse != null ? pulse.waveform() : Waveform.SINE;
        
        pulseScale = LabeledSlider.builder("Pulse Scale")
            .position(layout.getStartX(), layout.getY())
            .width(controlWidth)
            .range(0f, 1f)
            .initial(initScale)
            .format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("beam.pulse.scale", v)))
            .build();
        widgets.add(pulseScale);
        layout.nextRow();

        pulseSpeed = LabeledSlider.builder("Pulse Speed")
            .position(layout.getStartX(), layout.getY())
            .width(controlWidth)
            .range(0f, 5f)
            .initial(initSpeed)
            .format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("beam.pulse.speed", v)))
            .build();
        widgets.add(pulseSpeed);
        layout.nextRow();

        pulseWaveform = CyclingButtonWidget.<Waveform>builder(w -> net.minecraft.text.Text.literal(w.name()))
            .values(Waveform.values())
            .initially(initWaveform)
            .build(
                layout.getStartX(),
                layout.getY(),
                controlWidth,  // Full width for waveform dropdown
                GuiConstants.WIDGET_HEIGHT,
                net.minecraft.text.Text.literal("Waveform"),
                (btn, v) -> onUserChange(() -> state.set("beam.pulse.waveform", v.name()))
            );
        widgets.add(pulseWaveform);
        layout.nextRow();

        // Min and Max on same row, each half width
        pulseMin = LabeledSlider.builder("Min")
            .position(layout.getStartX(), layout.getY())
            .width(halfWidth)
            .range(0f, 1f)
            .initial(initMin)
            .format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("beam.pulse.min", v)))
            .build();
        widgets.add(pulseMin);

        pulseMax = LabeledSlider.builder("Max")
            .position(layout.getStartX() + halfWidth + GuiConstants.ELEMENT_SPACING, layout.getY())
            .width(halfWidth)
            .range(0f, 2f)
            .initial(initMax)
            .format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("beam.pulse.max", v)))
            .build();
        widgets.add(pulseMax);
        layout.nextRow();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render all widgets directly (no expandable section wrapper)
        for (var w : widgets) {
            w.render(context, mouseX, mouseY, delta);
        }
    }

    public int getHeight() {
        return layout != null ? layout.getY() + GuiConstants.WIDGET_HEIGHT : panelHeight;
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
        FragmentRegistry.applyBeamFragment(state, name);
        syncFromState();
        applyingFragment = false;
    }

    private void syncFromState() {
        if (enableToggle != null) enableToggle.setValue(state.getBool("beam.enabled"));
        if (innerRadius != null) innerRadius.setValue(state.getFloat("beam.innerRadius"));
        if (outerRadius != null) outerRadius.setValue(state.getFloat("beam.outerRadius"));
        if (height != null) height.setValue(state.getFloat("beam.height"));
        if (glow != null) glow.setValue(state.getFloat("beam.glow"));
        // Handle nullable pulse config in sync
        PulseConfig syncPulse = state.beam() != null ? state.beam().pulse() : null;
        if (pulseToggle != null) pulseToggle.setValue(syncPulse != null && syncPulse.isActive());
        if (pulseScale != null) pulseScale.setValue(syncPulse != null ? syncPulse.scale() : 0.1f);
        if (pulseSpeed != null) pulseSpeed.setValue(syncPulse != null ? syncPulse.speed() : 1.0f);
        if (pulseWaveform != null) pulseWaveform.setValue(syncPulse != null ? syncPulse.waveform() : Waveform.SINE);
        if (pulseMin != null) pulseMin.setValue(syncPulse != null ? syncPulse.min() : 0.9f);
        if (pulseMax != null) pulseMax.setValue(syncPulse != null ? syncPulse.max() : 1.1f);
        if (colorField != null) {
            String beamColor = state.getString("beam.color");
            colorField.setText(beamColor != null ? beamColor : "@primary");
        }
    }

    @Override
    public void tick() {
        // No per-tick updates needed
    }

}

