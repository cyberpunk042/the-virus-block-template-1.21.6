package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.transform.Anchor;
import net.cyberpunk042.visual.transform.Billboard;
import net.cyberpunk042.visual.transform.Facing;
import net.cyberpunk042.visual.transform.MotionMode;
import net.cyberpunk042.visual.transform.OrbitConfig3D;
import net.cyberpunk042.visual.transform.UpVector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified Transform + Orbit sub-panel for Quick tab.
 * 
 * <p>Compact layout with all transform controls:
 * <ul>
 *   <li>Anchor, Offset (X/Y/Z)</li>
 *   <li>Rotation (X/Y/Z)</li>
 *   <li>Scale (uniform or per-axis)</li>
 *   <li>Facing, Billboard</li>
 *   <li>Orbit (enable, radius, speed)</li>
 * </ul>
 */
public class TransformSubPanel extends AbstractPanel {
    
    private static final int COMPACT_H = 16;  // Compact widget height
    private static final int GAP = 2;
    
    private int startY;
    
    // Transform controls
    private CyclingButtonWidget<Anchor> anchorDropdown;
    private LabeledSlider offsetX, offsetY, offsetZ;
    private LabeledSlider rotX, rotY, rotZ;
    private LabeledSlider scaleSlider;
    private CyclingButtonWidget<Boolean> uniformScaleToggle;
    private LabeledSlider scaleX, scaleY, scaleZ;  // For non-uniform mode
    private boolean useUniformScale = true;
    private CyclingButtonWidget<Facing> facingDropdown;
    private CyclingButtonWidget<Billboard> billboardDropdown;
    
    public TransformSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("TransformSubPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        rebuildWidgets();
        Logging.GUI.topic("panel").debug("TransformSubPanel initialized, height={}", contentHeight);
    }
    
    /**
     * Rebuilds widgets based on current state.
     * Called when mode changes to show/hide secondary parameters.
     */
    private void rebuildWidgets() {
        boolean needsOffset = bounds != null && !bounds.isEmpty();
        widgets.clear();
        
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        
        int x = GuiConstants.PADDING;
        int y = startY + GAP;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int thirdW = (w - GAP * 2) / 3;
        int halfW = (w - GAP) / 2;
        
        // Get current transform values
        var transform = state.transform();
        Anchor currentAnchor = transform != null ? transform.anchor() : Anchor.CENTER;
        Vector3f offset = transform != null && transform.offset() != null ? transform.offset() : new Vector3f();
        Vector3f rotation = transform != null && transform.rotation() != null ? transform.rotation() : new Vector3f();
        float scale = transform != null ? transform.scale() : 1.0f;
        Vector3f scaleXYZ = transform != null ? transform.scaleXYZ() : null;
        Facing currentFacing = transform != null ? transform.facing() : Facing.FIXED;
        Billboard currentBillboard = transform != null ? transform.billboard() : Billboard.NONE;
        
        // === ANCHOR ===
        anchorDropdown = CyclingButtonWidget.<Anchor>builder(a -> Text.literal(a.label()))
            .values(Anchor.values())
            .initially(currentAnchor)
            .build(x, y, w, COMPACT_H, Text.literal("Anchor"),
                (btn, val) -> {
                    state.set("transform.anchor", val.name());
                    Logging.GUI.topic("transform").debug("Anchor: {}", val);
                });
        widgets.add(anchorDropdown);
        y += COMPACT_H + GAP;
        
        // === OFFSET X/Y/Z (3 compact sliders) ===
        offsetX = LabeledSlider.builder("X")
            .position(x, y).width(thirdW)
            .range(-10f, 10f).initial(offset.x).format("%.1f")
            .onChange(v -> updateOffset(v, null, null))
            .build();
        widgets.add(offsetX);
        
        offsetY = LabeledSlider.builder("Y")
            .position(x + thirdW + GAP, y).width(thirdW)
            .range(-10f, 10f).initial(offset.y).format("%.1f")
            .onChange(v -> updateOffset(null, v, null))
            .build();
        widgets.add(offsetY);
        
        offsetZ = LabeledSlider.builder("Z")
            .position(x + (thirdW + GAP) * 2, y).width(thirdW)
            .range(-10f, 10f).initial(offset.z).format("%.1f")
            .onChange(v -> updateOffset(null, null, v))
            .build();
        widgets.add(offsetZ);
        y += COMPACT_H + GAP;
        
        // === ROTATION X/Y/Z (3 compact sliders) ===
        rotX = LabeledSlider.builder("Rx")
            .position(x, y).width(thirdW)
            .range(-180f, 180f).initial(rotation.x).format("%.0f°")
            .onChange(v -> updateRotation(v, null, null))
            .build();
        widgets.add(rotX);
        
        rotY = LabeledSlider.builder("Ry")
            .position(x + thirdW + GAP, y).width(thirdW)
            .range(-180f, 180f).initial(rotation.y).format("%.0f°")
            .onChange(v -> updateRotation(null, v, null))
            .build();
        widgets.add(rotY);
        
        rotZ = LabeledSlider.builder("Rz")
            .position(x + (thirdW + GAP) * 2, y).width(thirdW)
            .range(-180f, 180f).initial(rotation.z).format("%.0f°")
            .onChange(v -> updateRotation(null, null, v))
            .build();
        widgets.add(rotZ);
        y += COMPACT_H + GAP;
        
        // === SCALE (with uniform toggle) ===
        useUniformScale = scaleXYZ == null;  // If no per-axis scale, use uniform
        
        scaleSlider = LabeledSlider.builder("Scale")
            .position(x, y).width(halfW - 30)
            .range(0.1f, 5f).initial(scale).format("%.2f")
            .onChange(v -> {
                state.set("transform.scale", v);
                Logging.GUI.topic("transform").debug("Scale: {}", v);
            })
            .build();
        widgets.add(scaleSlider);
        
        // Use GuiWidgets.toggle for proper label formatting
        uniformScaleToggle = CyclingButtonWidget.<Boolean>builder(b -> Text.literal(b ? "Uni" : "XYZ"))
            .values(true, false)
            .initially(useUniformScale)
            .omitKeyText()
            .build(x + halfW - 28, y, 30, COMPACT_H, Text.literal(""),
                (btn, val) -> {
                    useUniformScale = val;
                    rebuildScaleControls();
                });
        widgets.add(uniformScaleToggle);
        
        // ScaleWithRadius toggle (small button) - Show R text always but colored
        boolean scaleWithR = transform != null && transform.scaleWithRadius();
        var scaleWithRadiusToggle = CyclingButtonWidget.<Boolean>builder(b -> Text.literal(b ? "§aR" : "§7R"))
            .values(true, false)
            .initially(scaleWithR)
            .omitKeyText()
            .build(x + halfW + GAP, y, 20, COMPACT_H, Text.literal(""),
                (btn, val) -> state.set("transform.scaleWithRadius", val));
        widgets.add(scaleWithRadiusToggle);
        
        // InheritRotation toggle (small button) - Show ↻ always but colored
        boolean inheritRot = transform == null || transform.inheritRotation();
        var inheritRotToggle = CyclingButtonWidget.<Boolean>builder(b -> Text.literal(b ? "§a↻" : "§7↻"))
            .values(true, false)
            .initially(inheritRot)
            .omitKeyText()
            .build(x + halfW + GAP + 22, y, 20, COMPACT_H, Text.literal(""),
                (btn, val) -> state.set("transform.inheritRotation", val));
        widgets.add(inheritRotToggle);
        y += COMPACT_H + GAP;
        
        // === FACING + BILLBOARD (side by side) ===
        // Include label in value formatter to avoid ': ' prefix from label param
        facingDropdown = CyclingButtonWidget.<Facing>builder(f -> Text.literal("Face: " + f.name()))
            .values(Facing.values())
            .initially(currentFacing)
            .omitKeyText()
            .build(x, y, halfW, COMPACT_H, Text.literal(""),
                (btn, val) -> {
                    state.set("transform.facing", val.name());
                    Logging.GUI.topic("transform").debug("Facing: {}", val);
                });
        widgets.add(facingDropdown);
        
        billboardDropdown = CyclingButtonWidget.<Billboard>builder(b -> Text.literal("Bill: " + b.name()))
            .values(Billboard.values())
            .initially(currentBillboard)
            .omitKeyText()
            .build(x + halfW + GAP, y, halfW, COMPACT_H, Text.literal(""),
                (btn, val) -> {
                    state.set("transform.billboard", val.name());
                    Logging.GUI.topic("transform").debug("Billboard: {}", val);
                });
        widgets.add(billboardDropdown);
        y += COMPACT_H + GAP + 4;  // Extra gap before orbit section
        
        // === 3D ORBIT - PER-AXIS CONTROLS ===
        // Each axis has: Mode | Radius (amplitude) | Speed (frequency) | Phase
        var orbit3d = transform != null ? transform.orbit3d() : null;
        y = addAxisOrbitRow(x, y, w, "X", orbit3d != null ? orbit3d.x() : null, "transform.orbit3d.x");
        y = addAxisOrbitRow(x, y, w, "Y", orbit3d != null ? orbit3d.y() : null, "transform.orbit3d.y");
        y = addAxisOrbitRow(x, y, w, "Z", orbit3d != null ? orbit3d.z() : null, "transform.orbit3d.z");
        
        contentHeight = y - startY + GuiConstants.PADDING;
        
        if (needsOffset) {
            applyBoundsOffset();
        }
    }
    
    private int addAxisOrbitRow(int x, int y, int w, String axis, 
            net.cyberpunk042.visual.transform.AxisMotionConfig config, String basePath) {
        // Layout: Full-width rows
        // Row 1: Mode (60) + Rad (rest of width)
        // Row 2: Spd (half) + Ph° (half)
        // Row 3 (complex): Amp2 + Freq2 (+ Swing/Ph2)
        // Row 4 (EPICYCLIC): TiltX + TiltY + TiltZ
        
        int halfW = (w - GAP) / 2;
        int thirdW = (w - GAP * 2) / 3;
        int modeW = 90;  // Wide enough for full mode name
        int radW = w - modeW - GAP;  // Rad gets rest of Row 1
        
        MotionMode currentMode = config != null ? config.mode() : MotionMode.NONE;
        
        // ===================== ROW 1: Mode + Rad (full width) =====================
        var modeDropdown = CyclingButtonWidget.<MotionMode>builder(
                m -> Text.literal(axis + ":" + m.name()))  // Full mode name
            .values(MotionMode.values())
            .initially(currentMode)
            .omitKeyText()
            .build(x, y, modeW, COMPACT_H, Text.literal(""),
                (btn, val) -> {
                    state.set(basePath + ".mode", val.name());
                    Logging.GUI.topic("transform").info("[TRANSFORM-DEBUG] Mode changed to: {}", val.name());
                    rebuildWidgets();
                    notifyWidgetsChanged();
                });
        widgets.add(modeDropdown);
        
        var radSlider = LabeledSlider.builder("Rad")
            .position(x + modeW + GAP, y).width(radW)
            .range(0f, 10f).initial(config != null ? config.amplitude() : 1.0f).format("%.1f")
            .onChange(v -> state.set(basePath + ".amplitude", v))
            .build();
        widgets.add(radSlider);
        
        int nextY = y + COMPACT_H + GAP;
        
        // ===================== ROW 2: Spd + Ph° (half width each) =====================
        var spdSlider = LabeledSlider.builder("Spd")
            .position(x, nextY).width(halfW)
            .range(-3f, 3f).initial(config != null ? config.frequency() : 0.5f).format("%.1f")
            .onChange(v -> state.set(basePath + ".frequency", v))
            .build();
        widgets.add(spdSlider);
        
        float phaseDegrees = config != null ? config.phase() * 360f : 0f;
        var phSlider = LabeledSlider.builder("Ph°")
            .position(x + halfW + GAP, nextY).width(halfW)
            .range(0f, 360f).initial(phaseDegrees).format("%.0f")
            .onChange(v -> state.set(basePath + ".phase", v / 360f))
            .build();
        widgets.add(phSlider);
        
        nextY += COMPACT_H + GAP;
        
        // ===================== ROW 3: Secondary Params (complex modes) =====================
        if (currentMode.needsSecondaryParams()) {
            // halfW already defined at top of method
            // Amplitude2 label
            String amp2Label = switch (currentMode) {
                case HELIX -> "Wave";
                case EPICYCLIC -> "Rad2";
                case WOBBLE -> "Tilt";
                case FLOWER -> "Petal";
                case ORBIT_BOUNCE -> "Bnc";
                case ELLIPTIC -> "Ratio";  // Ellipse ratio (1.0 = circle)
                default -> "Amp2";
            };
            
            // Frequency2 label
            String freq2Label = switch (currentMode) {
                case FLOWER -> "Petals";
                case WOBBLE -> "Rock";
                case HELIX -> "Coils";
                case ORBIT_BOUNCE -> "Bnc/s";
                case EPICYCLIC -> "Spd2";
                case PENDULUM -> "Wbl";  // Wobble speed
                default -> "Freq2";
            };
            
            // Frequency2 ranges - all support negative for opposite direction
            float freq2Min = switch (currentMode) {
                case EPICYCLIC -> -20f;
                case HELIX -> -20f;
                case WOBBLE -> -5f;
                case ORBIT_BOUNCE -> -3f;
                case PENDULUM -> -5f;      // Pendulum wobble can be negative
                default -> 0.1f;           // FLOWER stays positive (petal count)
            };
            float freq2Max = switch (currentMode) {
                case FLOWER -> 20f;
                case HELIX -> 20f;
                case WOBBLE -> 5f;
                case ORBIT_BOUNCE -> 3f;
                case PENDULUM -> 5f;
                default -> 20f;
            };
            
            // Determine if 3rd control is needed
            boolean hasThirdControl = (currentMode == MotionMode.PENDULUM || currentMode == MotionMode.EPICYCLIC);
            int secSliderW = hasThirdControl ? thirdW : halfW;
            
            // Default amp2: PENDULUM = 0 (no wobble), others = 0.5
            // For PENDULUM, ALWAYS use 0 regardless of existing config
            float defaultAmp2 = currentMode == MotionMode.PENDULUM ? 0f : 0.5f;
            float initialAmp2 = (currentMode == MotionMode.PENDULUM) ? 0f 
                : (config != null ? config.amplitude2() : defaultAmp2);
            
            // Amplitude2 range: ORBIT_BOUNCE allows negative (bounce direction)
            float amp2Min = currentMode == MotionMode.ORBIT_BOUNCE ? -10f : 0f;
            float amp2Max = 10f;
            
            var amp2Slider = LabeledSlider.builder(amp2Label)
                .position(x, nextY).width(secSliderW)
                .range(amp2Min, amp2Max).initial(initialAmp2).format("%.1f")
                .onChange(v -> state.set(basePath + ".amplitude2", v))
                .build();
            widgets.add(amp2Slider);
            
            // Default freq2: 1.0 for most modes (sensible starting point)
            float defaultFreq2 = 1.0f;
            var freq2Slider = LabeledSlider.builder(freq2Label)
                .position(x + secSliderW + GAP, nextY).width(secSliderW)
                .range(freq2Min, freq2Max).initial(config != null ? config.frequency2() : defaultFreq2).format("%.1f")
                .onChange(v -> state.set(basePath + ".frequency2", v))
                .build();
            widgets.add(freq2Slider);
            
            // Third control: Swing° for PENDULUM, Ph2° for EPICYCLIC
            if (currentMode == MotionMode.PENDULUM) {
                var swingSlider = LabeledSlider.builder("Swing°")
                    .position(x + (secSliderW + GAP) * 2, nextY).width(secSliderW)
                    .range(1f, 1000f).initial(config != null ? config.swingAngle() : 90f).format("%.0f")
                    .onChange(v -> state.set(basePath + ".swingAngle", v))
                    .build();
                widgets.add(swingSlider);
            } else if (currentMode == MotionMode.EPICYCLIC) {
                float phase2Deg = config != null ? config.phase2() * 360f : 0f;
                var ph2Slider = LabeledSlider.builder("Ph2°")
                    .position(x + (secSliderW + GAP) * 2, nextY).width(secSliderW)
                    .range(0f, 360f).initial(phase2Deg).format("%.0f")
                    .onChange(v -> state.set(basePath + ".phase2", v / 360f))
                    .build();
                widgets.add(ph2Slider);
            }
            
            nextY += COMPACT_H + GAP;
            
            // ===================== ROW 4: EPICYCLIC Tilts =====================
            if (currentMode == MotionMode.EPICYCLIC) {
                var tiltXSlider = LabeledSlider.builder("TiltX")
                    .position(x, nextY).width(thirdW)
                    .range(-180f, 180f).initial(config != null ? config.orbit2TiltX() : 0f).format("%.0f°")
                    .onChange(v -> state.set(basePath + ".orbit2TiltX", v))
                    .build();
                widgets.add(tiltXSlider);
                
                var tiltYSlider = LabeledSlider.builder("TiltY")
                    .position(x + thirdW + GAP, nextY).width(thirdW)
                    .range(-180f, 180f).initial(config != null ? config.orbit2TiltY() : 0f).format("%.0f°")
                    .onChange(v -> state.set(basePath + ".orbit2TiltY", v))
                    .build();
                widgets.add(tiltYSlider);
                
                var tiltZSlider = LabeledSlider.builder("TiltZ")
                    .position(x + (thirdW + GAP) * 2, nextY).width(thirdW)
                    .range(-180f, 180f).initial(config != null ? config.orbit2TiltZ() : 0f).format("%.0f°")
                    .onChange(v -> state.set(basePath + ".orbit2TiltZ", v))
                    .build();
                widgets.add(tiltZSlider);
                
                nextY += COMPACT_H + GAP;
            }
        }
        
        return nextY;
    }
    
    private void updateOffset(Float x, Float y, Float z) {
        var transform = state.transform();
        Vector3f current = transform != null && transform.offset() != null 
            ? new Vector3f(transform.offset()) 
            : new Vector3f();
        if (x != null) current.x = x;
        if (y != null) current.y = y;
        if (z != null) current.z = z;
        state.set("transform.offset", current);
    }
    
    private void updateRotation(Float x, Float y, Float z) {
        var transform = state.transform();
        Vector3f current = transform != null && transform.rotation() != null 
            ? new Vector3f(transform.rotation()) 
            : new Vector3f();
        if (x != null) current.x = x;
        if (y != null) current.y = y;
        if (z != null) current.z = z;
        state.set("transform.rotation", current);
    }
    
    private void rebuildScaleControls() {
        // TODO: Rebuild to show either uniform slider or XYZ sliders
        // For now, just log the toggle
        Logging.GUI.topic("transform").debug("Uniform scale: {}", useUniformScale);
    }
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Background
        int bgY = startY + scrollOffset;
        context.fill(bounds.x(), bgY, bounds.x() + panelWidth, bgY + contentHeight, GuiConstants.BG_PANEL);
        
        // Render all widgets
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
