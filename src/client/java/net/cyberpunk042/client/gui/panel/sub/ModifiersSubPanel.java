package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.visual.animation.*;
import net.cyberpunk042.client.gui.builder.Bound;
import net.cyberpunk042.client.gui.builder.BoundPanel;
import net.cyberpunk042.client.gui.builder.ContentBuilder;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.log.Logging;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * ModifiersSubPanel - Animation and effects using BoundPanel.
 * 
 * <p>Controls modifiers like bobbing, breathing, wobble, wave, precession,
 * and ray-specific animations (flow, motion).</p>
 * 
 * <p>Uses {@link BoundPanel} with {@link ContentBuilder} for bidirectional
 * state synchronization. Widgets automatically update on profile load,
 * primitive switch, etc.</p>
 */
public class ModifiersSubPanel extends BoundPanel {
    
    private static final int COMPACT_H = 16;
    
    private final int startY;
    
    public ModifiersSubPanel(Screen parent, FieldEditState state) {
        super(parent, state);
        this.startY = GuiConstants.PADDING;
        Logging.GUI.topic("panel").debug("ModifiersSubPanel created (BoundPanel version)");
    }

    @Override
    protected void buildContent() {
        ContentBuilder content = content(startY);
        
        // Check if current shape is Rays
        boolean isRaysShape = "rays".equalsIgnoreCase(state.getString("shapeType"));
        
        // ═══════════════════════════════════════════════════════════════════════
        // FIELD MODIFIERS - Bobbing & Breathing
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sliderPair(
            "Bobbing", "modifiers.bobbing", 0f, 1f,
            "Breathing", "modifiers.breathing", 0f, 1f
        );
        
        // ═══════════════════════════════════════════════════════════════════════
        // COLOR CYCLE
        // ═══════════════════════════════════════════════════════════════════════
        
        buildColorCycleSection(content);
        
        // ═══════════════════════════════════════════════════════════════════════
        // WOBBLE - Random position jitter
        // ═══════════════════════════════════════════════════════════════════════
        
        buildWobbleSection(content);
        
        // ═══════════════════════════════════════════════════════════════════════
        // WAVE DEFORMATION
        // ═══════════════════════════════════════════════════════════════════════
        
        buildWaveSection(content);
        
        // ═══════════════════════════════════════════════════════════════════════
        // PRECESSION - Axis wobble
        // ═══════════════════════════════════════════════════════════════════════
        
        buildPrecessionSection(content);
        
        // ═══════════════════════════════════════════════════════════════════════
        // RAY FLOW - Alpha/visibility animation (Rays only)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.when(isRaysShape, c -> buildRayFlowSection(c));
        
        // ═══════════════════════════════════════════════════════════════════════
        // RAY MOTION - Geometry animation (Rays only)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.when(isRaysShape, c -> buildRayMotionSection(c));
        
        contentHeight = content.getContentHeight();
        Logging.GUI.topic("panel").debug("ModifiersSubPanel built: {} widgets, isRays={}", 
            widgets.size(), isRaysShape);
    }
    
    // =========================================================================
    // COLOR CYCLE SECTION
    // =========================================================================
    
    private void buildColorCycleSection(ContentBuilder content) {
        int x = GuiConstants.PADDING;
        int y = content.getCurrentY();
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        boolean active = state.colorCycle().isActive();
        
        // Checkboxes: Color Cycle | Smooth Blend
        CyclingButtonWidget<Boolean> cycleToggle = GuiWidgets.toggle(
            x, y, halfW, "Color Cycle", active, "Enable color cycling",
            v -> {
                if (!v) {
                    state.set("colorCycle.speed", 0f);
                } else {
                    if (state.colorCycle().colors() == null || state.colorCycle().colors().isEmpty()) {
                        state.set("colorCycle", ColorCycleConfig.builder()
                            .colors("#FF0000", "#FF7F00", "#FFFF00", "#00FF00", "#0000FF", "#8B00FF")
                            .speed(1.0f).blend(true).build());
                    } else if (state.getFloat("colorCycle.speed") == 0f) {
                        state.set("colorCycle.speed", 1f);
                    }
                }
            });
        widgets.add(cycleToggle);
        
        CyclingButtonWidget<Boolean> blendToggle = GuiWidgets.toggle(
            x + halfW + GuiConstants.PADDING, y, halfW, "Smooth Blend",
            state.getBool("colorCycle.blend"), "Smooth color transitions",
            v -> state.set("colorCycle.blend", v));
        widgets.add(blendToggle);
        content.advanceRow();
        
        // Speed slider
        content.slider("Speed", "colorCycle.speed").range(0.1f, 5f).format("%.1f").add();
    }
    
    // =========================================================================
    // WOBBLE SECTION
    // =========================================================================
    
    private void buildWobbleSection(ContentBuilder content) {
        int x = GuiConstants.PADDING;
        int y = content.getCurrentY();
        int w = panelWidth - GuiConstants.PADDING * 2;
        
        WobbleConfig wobble = state.wobble();
        boolean active = wobble != null && wobble.isActive();
        
        CyclingButtonWidget<Boolean> toggle = GuiWidgets.toggle(
            x, y, w, "Wobble", active, "Random position jitter",
            v -> {
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
        widgets.add(toggle);
        content.advanceRow();
        
        // Amplitude + Speed
        content.sliderPair(
            "Amplitude", "wobble.amplitude", 0f, 1f,
            "Speed", "wobble.speed", 0.1f, 5f
        );
    }
    
    // =========================================================================
    // WAVE SECTION
    // =========================================================================
    
    private void buildWaveSection(ContentBuilder content) {
        int x = GuiConstants.PADDING;
        int y = content.getCurrentY();
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        WaveConfig wave = state.wave();
        boolean active = wave != null && wave.isActive();
        
        CyclingButtonWidget<Boolean> toggle = GuiWidgets.toggle(
            x, y, w, "Wave Deformation", active, "Surface wave animation",
            v -> { if (!v) state.set("wave", WaveConfig.NONE); });
        widgets.add(toggle);
        content.advanceRow();
        
        // Amplitude + Frequency
        content.sliderPair(
            "Amplitude", "wave.amplitude", 0f, 1f,
            "Frequency", "wave.frequency", 0.1f, 10f
        );
        
        // Speed + Direction dropdown
        // Create Speed slider at left half
        LabeledSlider speedSlider = LabeledSlider.builder("Speed")
            .position(x, content.getCurrentY()).width(halfW)
            .range(0.1f, 5f).initial(state.getFloat("wave.speed")).format("%.1f")
            .onChange(v -> state.set("wave.speed", v))
            .build();
        widgets.add(speedSlider);
        
        // Direction dropdown at right half
        CyclingButtonWidget<Axis> dirDropdown = CyclingButtonWidget.<Axis>builder(
                a -> Text.literal("Dir: " + a.name()))
            .values(Axis.values())
            .initially(wave != null && wave.direction() != null ? wave.direction() : Axis.Y)
            .omitKeyText()
            .build(x + halfW + GuiConstants.PADDING, content.getCurrentY(), halfW, COMPACT_H, Text.literal(""),
                (btn, val) -> state.set("wave.direction", val.name()));
        widgets.add(dirDropdown);
        content.advanceRow();
    }
    
    // =========================================================================
    // PRECESSION SECTION
    // =========================================================================
    
    private void buildPrecessionSection(ContentBuilder content) {
        int x = GuiConstants.PADDING;
        int y = content.getCurrentY();
        int w = panelWidth - GuiConstants.PADDING * 2;
        
        PrecessionConfig prec = state.precession();
        boolean active = prec != null && prec.isActive();
        
        CyclingButtonWidget<Boolean> toggle = GuiWidgets.toggle(
            x, y, w, "Precession", active, "Axis wobble (lighthouse effect)",
            v -> {
                if (!v) state.set("precession", null);
                else state.set("precession", PrecessionConfig.DEFAULT);
            });
        widgets.add(toggle);
        content.advanceRow();
        
        // Tilt + Speed
        content.sliderPair(
            "Tilt°", "precession.tiltAngle", 1f, 90f,
            "Speed", "precession.speed", 0.01f, 2f
        );
    }
    
    // =========================================================================
    // RAY FLOW SECTION (Rays only)
    // =========================================================================
    
    private void buildRayFlowSection(ContentBuilder c) {
        int x = GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // Section header via gap
        c.gap();
        
        RayFlowConfig flow = state.rayFlow();
        LengthMode curLength = flow != null ? flow.length() : LengthMode.NONE;
        TravelMode curTravel = flow != null ? flow.travel() : TravelMode.NONE;
        FlickerMode curFlicker = flow != null ? flow.flicker() : FlickerMode.NONE;
        
        // Length dropdown + Speed (pair)
        CyclingButtonWidget<LengthMode> lengthDropdown = CyclingButtonWidget.<LengthMode>builder(
                m -> Text.literal("Len: " + m.displayName()))
            .values(LengthMode.values())
            .initially(curLength)
            .omitKeyText()
            .build(x, c.getCurrentY(), halfW, COMPACT_H, Text.literal(""),
                (btn, val) -> state.set("rayFlow.length", val));
        widgets.add(lengthDropdown);
        
        LabeledSlider lenSpeedSlider = LabeledSlider.builder("LSpd")
            .position(x + halfW + GuiConstants.PADDING, c.getCurrentY()).width(halfW)
            .range(0.1f, 5f).initial(flow != null ? flow.lengthSpeed() : 1f).format("%.1f")
            .onChange(v -> state.set("rayFlow.lengthSpeed", v))
            .build();
        widgets.add(lenSpeedSlider);
        c.advanceRow();
        
        // Travel dropdown + Speed
        CyclingButtonWidget<TravelMode> travelDropdown = CyclingButtonWidget.<TravelMode>builder(
                m -> Text.literal("Trav: " + m.displayName()))
            .values(TravelMode.values())
            .initially(curTravel)
            .omitKeyText()
            .build(x, c.getCurrentY(), halfW, COMPACT_H, Text.literal(""),
                (btn, val) -> state.set("rayFlow.travel", val));
        widgets.add(travelDropdown);
        
        LabeledSlider travelSpeedSlider = LabeledSlider.builder("TSpd")
            .position(x + halfW + GuiConstants.PADDING, c.getCurrentY()).width(halfW)
            .range(0.1f, 5f).initial(flow != null ? flow.travelSpeed() : 1f).format("%.1f")
            .onChange(v -> state.set("rayFlow.travelSpeed", v))
            .build();
        widgets.add(travelSpeedSlider);
        c.advanceRow();
        
        // Chase count + width
        c.sliderPair(
            "Count", "rayFlow.chaseCount", 1f, 10f,
            "Width", "rayFlow.chaseWidth", 0.05f, 0.5f
        );
        
        // Flicker dropdown + Intensity
        CyclingButtonWidget<FlickerMode> flickerDropdown = CyclingButtonWidget.<FlickerMode>builder(
                m -> Text.literal("Flkr: " + m.displayName()))
            .values(FlickerMode.values())
            .initially(curFlicker)
            .omitKeyText()
            .build(x, c.getCurrentY(), halfW, COMPACT_H, Text.literal(""),
                (btn, val) -> state.set("rayFlow.flicker", val));
        widgets.add(flickerDropdown);
        
        LabeledSlider intensitySlider = LabeledSlider.builder("Int")
            .position(x + halfW + GuiConstants.PADDING, c.getCurrentY()).width(halfW)
            .range(0f, 1f).initial(flow != null ? flow.flickerIntensity() : 0.3f).format("%.1f")
            .onChange(v -> state.set("rayFlow.flickerIntensity", v))
            .build();
        widgets.add(intensitySlider);
        c.advanceRow();
        
        c.slider("Freq", "rayFlow.flickerFrequency").range(1f, 20f).format("%.1f").add();
    }
    
    // =========================================================================
    // RAY MOTION SECTION (Rays only)
    // =========================================================================
    
    private void buildRayMotionSection(ContentBuilder c) {
        int x = GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // Section header via gap
        c.gap();
        
        RayMotionConfig motion = state.rayMotion();
        MotionMode curMode = motion != null ? motion.mode() : MotionMode.NONE;
        
        // Mode dropdown + Speed
        CyclingButtonWidget<MotionMode> modeDropdown = CyclingButtonWidget.<MotionMode>builder(
                m -> Text.literal("Mot: " + m.displayName()))
            .values(MotionMode.values())
            .initially(curMode)
            .omitKeyText()
            .build(x, c.getCurrentY(), halfW, COMPACT_H, Text.literal(""),
                (btn, val) -> state.set("rayMotion.mode", val));
        widgets.add(modeDropdown);
        
        LabeledSlider speedSlider = LabeledSlider.builder("Spd")
            .position(x + halfW + GuiConstants.PADDING, c.getCurrentY()).width(halfW)
            .range(0.1f, 5f).initial(motion != null ? motion.speed() : 1f).format("%.1f")
            .onChange(v -> state.set("rayMotion.speed", v))
            .build();
        widgets.add(speedSlider);
        c.advanceRow();
        
        // Direction X, Y
        c.sliderPair(
            "Dir X", "rayMotion.directionX", -1f, 1f,
            "Dir Y", "rayMotion.directionY", -1f, 1f
        );
        
        // Direction Z, Amplitude
        c.sliderPair(
            "Dir Z", "rayMotion.directionZ", -1f, 1f,
            "Amp", "rayMotion.amplitude", 0f, 1f
        );
        
        c.slider("Frequency", "rayMotion.frequency").range(0.1f, 10f).format("%.1f").add();
    }
    
    // =========================================================================
    // LIFECYCLE
    // =========================================================================
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderWithScroll(context, mouseX, mouseY, delta);
    }
    
    @Override
    protected boolean needsRebuildOnFragmentApply() {
        // Ray sections depend on shape type
        return true;
    }
    
    /** Called when shape type changes - rebuilds widgets */
    public void onShapeChanged() {
        rebuildContent();
    }
    
    public int getContentHeight() {
        return contentHeight;
    }
    
    public List<ClickableWidget> getWidgets() {
        return new ArrayList<>(widgets);
    }
}
