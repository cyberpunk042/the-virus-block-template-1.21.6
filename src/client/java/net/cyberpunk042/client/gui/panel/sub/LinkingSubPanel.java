package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.RendererCapabilities.Feature;
import net.cyberpunk042.client.gui.state.RequiresFeature;
import net.minecraft.client.gui.screen.Screen;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.field.primitive.PrimitiveLink;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * LinkingSubPanel - Manages the link for the current primitive.
 * 
 * <p>A link connects the current primitive to ONE target primitive,
 * then specifies HOW they are linked via boolean flags.</p>
 * 
 * <p>Structure:
 * <ul>
 *   <li>Target: Select which primitive to link to</li>
 *   <li>Link types: Boolean toggles for Follow, RadiusMatch, ScaleWith, OrbitSync, Color, Alpha</li>
 *   <li>Offsets: PhaseOffset, OrbitPhaseOffset, RadiusOffset</li>
 * </ul>
 * </p>
 */
@RequiresFeature(Feature.LINKING)
public class LinkingSubPanel extends AbstractPanel {

    private static final int COMPACT_H = 16;
    private static final int GAP = 2;
    
    private int startY;
    private TextRenderer textRenderer;
    
    // Available primitives to link to (primitives BEFORE current one)
    private List<String> availableTargets = new ArrayList<>();
    
    // Widgets
    private CyclingButtonWidget<String> targetDropdown;
    private CyclingButtonWidget<Boolean> followToggle;
    private CyclingButtonWidget<Boolean> radiusMatchToggle;
    private CyclingButtonWidget<Boolean> scaleWithToggle;
    private CyclingButtonWidget<Boolean> orbitSyncToggle;
    private CyclingButtonWidget<Boolean> colorMatchToggle;
    private CyclingButtonWidget<Boolean> alphaMatchToggle;
    private LabeledSlider phaseOffsetSlider;
    private LabeledSlider orbitPhaseSlider;
    private LabeledSlider radiusOffsetSlider;
    
    public LinkingSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        textRenderer = MinecraftClient.getInstance().textRenderer;
        
        int x = GuiConstants.PADDING;
        int y = startY + GAP;
        int w = width - GuiConstants.PADDING * 2;
        int halfW = (w - GAP) / 2;
        int thirdW = (w - GAP * 2) / 3;
        
        // ═══════════════════════════════════════════════════════════════════════
        // BUILD AVAILABLE TARGETS (primitives BEFORE current one)
        // ═══════════════════════════════════════════════════════════════════════
        
        buildAvailableTargets();
        
        // Get current primitive's link
        Primitive currentPrim = state.getSelectedPrimitive();
        PrimitiveLink link = currentPrim != null ? currentPrim.link() : null;
        
        // Get current target from link
        String currentTarget = link != null ? link.target() : null;
        
        // ═══════════════════════════════════════════════════════════════════════
        // TARGET SELECTION
        // ═══════════════════════════════════════════════════════════════════════
        
        List<String> targetOptions = new ArrayList<>();
        targetOptions.add("(none)");
        targetOptions.addAll(availableTargets);
        
        String initialTarget = currentTarget != null ? currentTarget : "(none)";
        
        targetDropdown = CyclingButtonWidget.<String>builder(v -> Text.literal("Target: " + v))
            .values(targetOptions.toArray(new String[0]))
            .initially(initialTarget)
            .omitKeyText()
            .build(x, y, w, COMPACT_H, Text.literal(""),
                (btn, val) -> {
                    String newTarget = "(none)".equals(val) ? null : val;
                    state.set("link.target", newTarget);
                    Logging.GUI.topic("link").debug("Target set to: {}", newTarget);
                });
        widgets.add(targetDropdown);
        y += COMPACT_H + GAP + 4;
        
        // ═══════════════════════════════════════════════════════════════════════
        // POSITION TOGGLES (Row 1)
        // ═══════════════════════════════════════════════════════════════════════
        
        int fourthW = (w - GAP * 3) / 4;
        
        // Row 1: Follow, FollowDyn, RadMatch, ScaleWith
        boolean hasFollow = link != null && link.follow();
        followToggle = createLinkToggle(x, y, fourthW, "Follow", hasFollow, 
            v -> state.set("link.follow", v));
        widgets.add(followToggle);
        
        boolean hasFollowDyn = link != null && link.followDynamic();
        var followDynToggle = createLinkToggle(x + fourthW + GAP, y, fourthW, "FollwDyn", hasFollowDyn,
            v -> state.set("link.followDynamic", v));
        widgets.add(followDynToggle);
        
        boolean hasRadMatch = link != null && link.radiusMatch();
        radiusMatchToggle = createLinkToggle(x + (fourthW + GAP) * 2, y, fourthW, "RadMtch", hasRadMatch,
            v -> state.set("link.radiusMatch", v));
        widgets.add(radiusMatchToggle);
        
        boolean hasScaleWith = link != null && link.scaleWith();
        scaleWithToggle = createLinkToggle(x + (fourthW + GAP) * 3, y, fourthW, "ScaleW", hasScaleWith,
            v -> state.set("link.scaleWith", v));
        widgets.add(scaleWithToggle);
        y += COMPACT_H + GAP;
        
        // ═══════════════════════════════════════════════════════════════════════
        // ORBIT + APPEARANCE TOGGLES (Row 2)
        // ═══════════════════════════════════════════════════════════════════════
        
        // Row 2: OrbitSync, Color, Alpha
        boolean hasOrbitSync = link != null && link.orbitSync();
        orbitSyncToggle = createLinkToggle(x, y, thirdW, "OrbitSync", hasOrbitSync,
            v -> state.set("link.orbitSync", v));
        widgets.add(orbitSyncToggle);
        
        boolean hasColorMatch = link != null && link.colorMatch();
        colorMatchToggle = createLinkToggle(x + thirdW + GAP, y, thirdW, "Color", hasColorMatch,
            v -> state.set("link.colorMatch", v));
        widgets.add(colorMatchToggle);
        
        boolean hasAlphaMatch = link != null && link.alphaMatch();
        alphaMatchToggle = createLinkToggle(x + (thirdW + GAP) * 2, y, thirdW, "Alpha", hasAlphaMatch,
            v -> state.set("link.alphaMatch", v));
        widgets.add(alphaMatchToggle);
        y += COMPACT_H + GAP + 4;
        
        // ═══════════════════════════════════════════════════════════════════════
        // PHASE PARAMETERS
        // ═══════════════════════════════════════════════════════════════════════
        
        // Phase offset (0-360 degrees, converted to 0-1 internally)
        float phaseDeg = link != null ? link.phaseOffset() * 360f : 0f;
        phaseOffsetSlider = LabeledSlider.builder("Phase°")
            .position(x, y).width(halfW)
            .range(0f, 360f).initial(phaseDeg).format("%.0f")
            .onChange(v -> state.set("link.phaseOffset", v / 360f))
            .build();
        widgets.add(phaseOffsetSlider);
        
        // Orbit phase offset (0-360 degrees, converted to 0-1 internally)
        float orbitPhaseDeg = link != null ? link.orbitPhaseOffset() * 360f : 0f;
        orbitPhaseSlider = LabeledSlider.builder("OrbPh°")
            .position(x + halfW + GAP, y).width(halfW)
            .range(0f, 360f).initial(orbitPhaseDeg).format("%.0f")
            .onChange(v -> state.set("link.orbitPhaseOffset", v / 360f))
            .build();
        widgets.add(orbitPhaseSlider);
        y += COMPACT_H + GAP;
        
        // ═══════════════════════════════════════════════════════════════════════
        // ORBIT PARAMETERS (NEW!)
        // ═══════════════════════════════════════════════════════════════════════
        
        // Orbit radius offset + speed multiplier
        float orbRadOff = link != null ? link.orbitRadiusOffset() : 0f;
        var orbRadOffSlider = LabeledSlider.builder("OrbRad")
            .position(x, y).width(halfW)
            .range(-10f, 10f).initial(orbRadOff).format("%.1f")
            .onChange(v -> state.set("link.orbitRadiusOffset", v))
            .build();
        widgets.add(orbRadOffSlider);
        
        float spdMult = link != null ? link.orbitSpeedMult() : 1f;
        var spdMultSlider = LabeledSlider.builder("SpdMul")
            .position(x + halfW + GAP, y).width(halfW)
            .range(0.1f, 10f).initial(spdMult).format("%.1f")
            .onChange(v -> state.set("link.orbitSpeedMult", v))
            .build();
        widgets.add(spdMultSlider);
        y += COMPACT_H + GAP;
        
        // Inclination offset + precession offset (in degrees)
        float incOff = link != null ? link.orbitInclinationOffset() * 360f : 0f;
        var incOffSlider = LabeledSlider.builder("Inc°")
            .position(x, y).width(halfW)
            .range(-180f, 180f).initial(incOff).format("%.0f")
            .onChange(v -> state.set("link.orbitInclinationOffset", v / 360f))
            .build();
        widgets.add(incOffSlider);
        
        float precOff = link != null ? link.orbitPrecessionOffset() * 360f : 0f;
        var precOffSlider = LabeledSlider.builder("Prec°")
            .position(x + halfW + GAP, y).width(halfW)
            .range(-180f, 180f).initial(precOff).format("%.0f")
            .onChange(v -> state.set("link.orbitPrecessionOffset", v / 360f))
            .build();
        widgets.add(precOffSlider);
        y += COMPACT_H + GAP;
        
        // Shape radius offset
        radiusOffsetSlider = LabeledSlider.builder("ShapeRadOff")
            .position(x, y).width(w)
            .range(-10f, 10f).initial(link != null ? link.radiusOffset() : 0f).format("%.1f")
            .onChange(v -> state.set("link.radiusOffset", v))
            .build();
        widgets.add(radiusOffsetSlider);
        y += COMPACT_H + GAP;
        
        contentHeight = y - startY + GuiConstants.PADDING;
    }
    
    private CyclingButtonWidget<Boolean> createLinkToggle(int x, int y, int w, String label, 
            boolean initial, java.util.function.Consumer<Boolean> onChange) {
        return CyclingButtonWidget.<Boolean>builder(v -> Text.literal(v ? "§a✓" + label : "§7" + label))
            .values(true, false)
            .initially(initial)
            .omitKeyText()
            .build(x, y, w, COMPACT_H, Text.literal(""),
                (btn, val) -> onChange.accept(val));
    }
    
    private void buildAvailableTargets() {
        availableTargets.clear();
        
        FieldLayer layer = state.getSelectedLayer();
        if (layer == null || layer.primitives() == null) return;
        
        int currentIdx = state.getSelectedPrimitiveIndex();
        
        // Only primitives BEFORE current one can be linked to
        for (int i = 0; i < currentIdx && i < layer.primitives().size(); i++) {
            Primitive p = layer.primitives().get(i);
            String id = p.id() != null ? p.id() : "prim_" + i;
            availableTargets.add(id);
        }
        
        Logging.GUI.topic("link").debug("Available targets: {} (current idx={})", 
            availableTargets.size(), currentIdx);
    }
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int bgY = startY + scrollOffset;
        context.fill(bounds.x(), bgY, bounds.x() + panelWidth, bgY + contentHeight, GuiConstants.BG_PANEL);
        
        for (var widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
    }
    
    public int getHeight() {
        return contentHeight;
    }
    
    public List<ClickableWidget> getWidgets() {
        return widgets;
    }
}
