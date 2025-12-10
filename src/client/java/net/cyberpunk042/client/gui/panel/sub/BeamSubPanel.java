package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiLayout;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ExpandableSection;
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
 */
public class BeamSubPanel {

    private final FieldEditState state;
    private final ExpandableSection section;
    private final GuiLayout layout;
    private final List<net.minecraft.client.gui.widget.ClickableWidget> widgets = new ArrayList<>();
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

    public BeamSubPanel(FieldEditState state, int x, int y, int width) {
        this.state = state;
        this.section = new ExpandableSection(x, y, width - GuiConstants.PADDING * 2, "Beam (Debug)", true);
        this.layout = new GuiLayout(x + GuiConstants.PADDING, section.getContentY(), GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING, width - GuiConstants.PADDING * 2);
        build();
    }

    private void build() {
        int controlWidth = layout.getPanelWidth();
        int halfWidth = (controlWidth - GuiConstants.ELEMENT_SPACING) / 2;

        fragmentDropdown = CyclingButtonWidget.<String>builder(net.minecraft.text.Text::literal)
            .values(FragmentRegistry.listBeamFragments())
            .initially(currentFragment)
            .build(
                layout.getStartX(), layout.getStartY(),
                controlWidth, GuiConstants.WIDGET_HEIGHT,
                net.minecraft.text.Text.literal("Preset"),
                (btn, val) -> applyPreset(val)
            );
        widgets.add(fragmentDropdown);
        layout.nextRow();

        enableToggle = GuiWidgets.toggle(
            layout.getStartX(), layout.getStartY(), controlWidth,
            "Beam Enabled", state.isBeamEnabled(), "Toggle central beam",
            v -> onUserChange(() -> state.setBeamEnabled(v))
        );
        widgets.add(enableToggle);
        layout.nextRow();

        innerRadius = LabeledSlider.builder("Inner Radius")
            .position(layout.getStartX(), layout.getStartY())
            .width(controlWidth)
            .range(0.01f, 5.0f)
            .initial(state.getBeamInnerRadius())
            .format("%.3f")
            .onChange(v -> onUserChange(() -> state.setBeamInnerRadius(v)))
            .build();
        widgets.add(innerRadius);
        layout.nextRow();

        outerRadius = LabeledSlider.builder("Outer Radius")
            .position(layout.getStartX(), layout.getStartY())
            .width(controlWidth)
            .range(0.02f, 10.0f)
            .initial(state.getBeamOuterRadius())
            .format("%.3f")
            .onChange(v -> onUserChange(() -> state.setBeamOuterRadius(v)))
            .build();
        widgets.add(outerRadius);
        layout.nextRow();

        height = LabeledSlider.builder("Height")
            .position(layout.getStartX(), layout.getStartY())
            .width(controlWidth)
            .range(0.5f, 64f)
            .initial(state.getBeamHeight())
            .format("%.2f")
            .onChange(v -> onUserChange(() -> state.setBeamHeight(v)))
            .build();
        widgets.add(height);
        layout.nextRow();

        glow = LabeledSlider.builder("Glow")
            .position(layout.getStartX(), layout.getStartY())
            .width(controlWidth)
            .range(0f, 2f)
            .initial(state.getBeamGlow())
            .format("%.2f")
            .onChange(v -> onUserChange(() -> state.setBeamGlow(v)))
            .build();
        widgets.add(glow);
        layout.nextRow();

        colorField = new TextFieldWidget(
            net.minecraft.client.MinecraftClient.getInstance().textRenderer,
            layout.getStartX(), layout.getStartY(),
            controlWidth, GuiConstants.WIDGET_HEIGHT,
            net.minecraft.text.Text.literal("Beam Color")
        );
        colorField.setText(state.getBeamColor());
        colorField.setChangedListener(v -> onUserChange(() -> state.setBeamColor(v)));
        widgets.add(colorField);
        layout.nextRow();

        // Pulse section header (simple text)
        layout.nextRow(); // small spacer

        pulseToggle = GuiWidgets.toggle(
            layout.getStartX(), layout.getStartY(), controlWidth,
            "Pulse Enabled", state.isBeamPulseEnabled(), "Enable beam pulsing",
            v -> onUserChange(() -> state.setBeamPulseEnabled(v))
        );
        widgets.add(pulseToggle);
        layout.nextRow();

        pulseScale = LabeledSlider.builder("Pulse Scale")
            .position(layout.getStartX(), layout.getStartY())
            .width(controlWidth)
            .range(0f, 1f)
            .initial(state.getBeamPulseScale())
            .format("%.2f")
            .onChange(v -> onUserChange(() -> state.setBeamPulseScale(v)))
            .build();
        widgets.add(pulseScale);
        layout.nextRow();

        pulseSpeed = LabeledSlider.builder("Pulse Speed")
            .position(layout.getStartX(), layout.getStartY())
            .width(controlWidth)
            .range(0f, 5f)
            .initial(state.getBeamPulseSpeed())
            .format("%.2f")
            .onChange(v -> onUserChange(() -> state.setBeamPulseSpeed(v)))
            .build();
        widgets.add(pulseSpeed);
        layout.nextRow();

        pulseWaveform = CyclingButtonWidget.<Waveform>builder(w -> net.minecraft.text.Text.literal(w.name()))
            .values(Waveform.values())
            .initially(Waveform.valueOf(state.getBeamPulseWaveform().toUpperCase()))
            .build(
                layout.getStartX(),
                layout.getStartY(),
                halfWidth,
                GuiConstants.WIDGET_HEIGHT,
                net.minecraft.text.Text.literal("Waveform"),
                (btn, v) -> onUserChange(() -> state.setBeamPulseWaveform(v.name()))
            );
        widgets.add(pulseWaveform);

        pulseMin = LabeledSlider.builder("Min")
            .position(layout.getStartX() + halfWidth + GuiConstants.ELEMENT_SPACING, layout.getStartY())
            .width(halfWidth)
            .range(0f, 1f)
            .initial(state.getBeamPulseMin())
            .format("%.2f")
            .onChange(v -> onUserChange(() -> state.setBeamPulseMin(v)))
            .build();
        widgets.add(pulseMin);
        layout.nextRow();

        pulseMax = LabeledSlider.builder("Max")
            .position(layout.getStartX(), layout.getStartY())
            .width(halfWidth)
            .range(0f, 2f)
            .initial(state.getBeamPulseMax())
            .format("%.2f")
            .onChange(v -> onUserChange(() -> state.setBeamPulseMax(v)))
            .build();
        widgets.add(pulseMax);
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        section.render(context, net.minecraft.client.MinecraftClient.getInstance().textRenderer, mouseX, mouseY, delta);
        if (section.isExpanded()) {
            for (var w : widgets) {
                w.render(context, mouseX, mouseY, delta);
            }
        }
    }

    public int getHeight() {
        return section.getTotalHeight();
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
        if (enableToggle != null) enableToggle.setValue(state.isBeamEnabled());
        if (innerRadius != null) innerRadius.setValue(state.getBeamInnerRadius());
        if (outerRadius != null) outerRadius.setValue(state.getBeamOuterRadius());
        if (height != null) height.setValue(state.getBeamHeight());
        if (glow != null) glow.setValue(state.getBeamGlow());
        if (pulseToggle != null) pulseToggle.setValue(state.isBeamPulseEnabled());
        if (pulseScale != null) pulseScale.setValue(state.getBeamPulseScale());
        if (pulseSpeed != null) pulseSpeed.setValue(state.getBeamPulseSpeed());
        if (pulseWaveform != null) {
            try { pulseWaveform.setValue(Waveform.valueOf(state.getBeamPulseWaveform().toUpperCase())); } catch (IllegalArgumentException ignored) {}
        }
        if (pulseMin != null) pulseMin.setValue(state.getBeamPulseMin());
        if (pulseMax != null) pulseMax.setValue(state.getBeamPulseMax());
        if (colorField != null) colorField.setText(state.getBeamColor());
    }
}

