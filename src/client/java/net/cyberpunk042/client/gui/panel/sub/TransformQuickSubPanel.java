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
public class TransformQuickSubPanel extends AbstractPanel {
    
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
    
    // Orbit controls
    private CyclingButtonWidget<Boolean> orbitToggle;
    private LabeledSlider orbitRadius;
    private LabeledSlider orbitSpeed;
    
    public TransformQuickSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("TransformQuickSubPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        
        int x = GuiConstants.PADDING;
        int y = startY + GAP;
        int w = width - GuiConstants.PADDING * 2;
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
        
        // Orbit values
        var orbit = transform != null ? transform.orbit() : null;
        boolean orbitEnabled = orbit != null && orbit.isActive();
        float orbitR = orbit != null ? orbit.radius() : 2.0f;
        float orbitS = orbit != null ? orbit.speed() : 0.5f;
        
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
        
        // === ORBIT SECTION ===
        orbitToggle = CyclingButtonWidget.<Boolean>builder(b -> Text.literal(b ? "Orbit: ON" : "Orbit: OFF"))
            .values(true, false)
            .initially(orbitEnabled)
            .omitKeyText()
            .build(x, y, halfW, COMPACT_H, Text.literal(""),
                (btn, val) -> {
                    state.set("orbit.enabled", val);
                    Logging.GUI.topic("transform").debug("Orbit: {}", val);
                });
        widgets.add(orbitToggle);
        
        // Orbit axis (Y axis default)
        String currentAxis = state.getString("orbit.axis");
        var orbitAxisToggle = CyclingButtonWidget.<String>builder(v -> Text.literal("Axis: " + v))
            .values("X", "Y", "Z")
            .initially(currentAxis != null && !currentAxis.isEmpty() ? currentAxis : "Y")
            .omitKeyText()
            .build(x + halfW + GAP, y, halfW, COMPACT_H, Text.literal(""),
                (btn, val) -> state.set("orbit.axis", val));
        widgets.add(orbitAxisToggle);
        y += COMPACT_H + GAP;
        
        orbitRadius = LabeledSlider.builder("Radius")
            .position(x, y).width(halfW)
            .range(0.1f, 10f).initial(orbitR).format("%.1f")
            .onChange(v -> state.set("orbit.radius", v))
            .build();
        widgets.add(orbitRadius);
        
        orbitSpeed = LabeledSlider.builder("Speed")
            .position(x + halfW + GAP, y).width(halfW)
            .range(-2f, 2f).initial(orbitS).format("%.2f")
            .onChange(v -> state.set("orbit.speed", v))
            .build();
        widgets.add(orbitSpeed);
        y += COMPACT_H + GAP;
        
        contentHeight = y - startY + GuiConstants.PADDING;
        Logging.GUI.topic("panel").debug("TransformQuickSubPanel initialized, height={}", contentHeight);
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
