package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ExpandableSection;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.client.gui.widget.Vec3Editor;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.transform.Anchor;
import net.cyberpunk042.visual.transform.Billboard;
import net.cyberpunk042.visual.transform.Facing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * TransformSubPanel - Position, rotation, scale, and orientation controls.
 * 
 * <p>From 03_PARAMETERS.md §5 Transform Level:</p>
 * <ul>
 *   <li>anchor: CENTER, FEET, HEAD, ABOVE, BELOW, etc.</li>
 *   <li>offset: Vec3 (x, y, z)</li>
 *   <li>rotation: Vec3 (degrees)</li>
 *   <li>scale: uniform or per-axis</li>
 *   <li>facing: FIXED, PLAYER_LOOK, VELOCITY, CAMERA</li>
 *   <li>billboard: NONE, FULL, Y_AXIS</li>
 * </ul>
 */
public class TransformSubPanel extends AbstractPanel {
    
    private int startY;    
    private CyclingButtonWidget<Anchor> anchorDropdown;
    private Vec3Editor offsetEditor;
    private Vec3Editor rotationEditor;
    private LabeledSlider scaleSlider;
    private CyclingButtonWidget<Boolean> uniformScaleToggle;
    private Vec3Editor scaleXYZEditor;
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
        widgets.clear();
        
        
        int x = GuiConstants.PADDING;
        int y = startY + GuiConstants.PADDING;
        int w = width - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // Anchor
        anchorDropdown = GuiWidgets.enumDropdown(x, y, w, "Anchor", Anchor.class, Anchor.CENTER,
            "Field attachment point", v -> {
                state.set("transform.anchor", v.name());
                Logging.GUI.topic("transform").debug("Anchor: {}", v);
            });
        widgets.add(anchorDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Offset (nullable Vector3f - default to 0,0,0)
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        Vector3f offset = state.transform() != null ? state.transform().offset() : null;
        Vector3f offsetInit = offset != null ? new Vector3f(offset) : new Vector3f(0, 0, 0);
        offsetEditor = new Vec3Editor(tr, x, y, "Offset", offsetInit, v -> {
            state.set("transform.offset", v);
            Logging.GUI.topic("transform").trace("Offset: ({}, {}, {})", v.x, v.y, v.z);
        });
        // Vec3Editor renders itself, not added to widgets
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Rotation (nullable Vector3f - default to 0,0,0)
        Vector3f rotation = state.transform() != null ? state.transform().rotation() : null;
        Vector3f rotInit = rotation != null ? new Vector3f(rotation) : new Vector3f(0, 0, 0);
        rotationEditor = new Vec3Editor(tr, x, y, "Rotation", rotInit, v -> {
            state.set("transform.rotation", v);
            Logging.GUI.topic("transform").trace("Rotation: ({}, {}, {})", v.x, v.y, v.z);
        });
        // Vec3Editor renders itself, not added to widgets
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Scale
        scaleSlider = LabeledSlider.builder("Scale")
            .position(x, y).width(halfW)
            .range(0.1f, 5f).initial(state.getFloat("transform.scale")).format("%.2f")
            .onChange(v -> {
                state.set("transform.scale", v);
                Logging.GUI.topic("transform").trace("Scale: {}", v);
            }).build();
        widgets.add(scaleSlider);
        
        uniformScaleToggle = GuiWidgets.toggle(x + halfW + GuiConstants.PADDING, y, halfW,
            "Uniform", true, "Use uniform scale", v -> {
                // Toggle between uniform and per-axis scale
            });
        widgets.add(uniformScaleToggle);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Facing
        facingDropdown = GuiWidgets.enumDropdown(x, y, halfW, "Facing", Facing.class, Facing.FIXED,
            "How field orients", v -> {
                state.set("transform.facing", v.name());
                Logging.GUI.topic("transform").debug("Facing: {}", v);
            });
        widgets.add(facingDropdown);
        
        // Billboard
        billboardDropdown = GuiWidgets.enumDropdown(x + halfW + GuiConstants.PADDING, y, halfW,
            "Billboard", Billboard.class, Billboard.NONE,
            "Camera-facing mode", v -> {
                state.set("transform.billboard", v.name());
                Logging.GUI.topic("transform").debug("Billboard: {}", v);
            });
        widgets.add(billboardDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        contentHeight = y - startY + GuiConstants.PADDING;
        Logging.GUI.topic("panel").debug("TransformSubPanel initialized");
    }
    
    @Override public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        // Render all widgets directly (no expandable section)
        for (var widget : widgets) widget.render(context, mouseX, mouseY, delta);
        // Vec3Editors render separately (need TextRenderer)
        if (offsetEditor != null) offsetEditor.render(context, tr, mouseX, mouseY, delta);
        if (rotationEditor != null) rotationEditor.render(context, tr, mouseX, mouseY, delta);
    }
    
    public int getHeight() { return contentHeight; }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> all = new ArrayList<>();
        // No header button (direct content)
        all.addAll(widgets);
        return all;
    }
}
