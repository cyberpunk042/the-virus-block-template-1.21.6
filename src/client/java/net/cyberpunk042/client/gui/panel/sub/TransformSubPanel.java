package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.GuiState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ExpandableSection;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.client.gui.widget.Vec3Editor;
import net.cyberpunk042.log.Logging;
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
    
    private ExpandableSection section;
    private int startY;
    private final List<net.minecraft.client.gui.widget.ClickableWidget> widgets = new ArrayList<>();
    
    /** Anchor points for field positioning. */
    public enum Anchor {
        CENTER("Center"),
        FEET("Feet"),
        HEAD("Head"),
        ABOVE("Above Head"),
        BELOW("Below Feet"),
        CHEST("Chest"),
        BACK("Back");
        
        private final String label;
        Anchor(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    
    /** Facing modes for orientation. */
    public enum FacingMode {
        FIXED("Fixed"),
        PLAYER_LOOK("Player Look"),
        VELOCITY("Movement Direction"),
        CAMERA("Always Face Camera");
        
        private final String label;
        FacingMode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    
    /** Billboard modes. */
    public enum BillboardMode {
        NONE("None"),
        FULL("Full Billboard"),
        Y_AXIS("Y-Axis Only");
        
        private final String label;
        BillboardMode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    
    private CyclingButtonWidget<Anchor> anchorDropdown;
    private Vec3Editor offsetEditor;
    private Vec3Editor rotationEditor;
    private LabeledSlider scaleSlider;
    private CyclingButtonWidget<Boolean> uniformScaleToggle;
    private Vec3Editor scaleXYZEditor;
    private CyclingButtonWidget<FacingMode> facingDropdown;
    private CyclingButtonWidget<BillboardMode> billboardDropdown;
    
    public TransformSubPanel(Screen parent, GuiState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("TransformSubPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        section = new ExpandableSection(
            GuiConstants.PADDING, startY,
            width - GuiConstants.PADDING * 2,
            "Transform", false
        );
        
        int x = GuiConstants.PADDING;
        int y = section.getContentY();
        int w = width - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // Anchor
        anchorDropdown = GuiWidgets.enumDropdown(x, y, w, "Anchor", Anchor.class, Anchor.CENTER,
            "Field attachment point", v -> {
                state.setAnchor(v.name());
                Logging.GUI.topic("transform").debug("Anchor: {}", v);
            });
        widgets.add(anchorDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Offset
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        Vector3f offsetInit = new Vector3f(state.getOffsetX(), state.getOffsetY(), state.getOffsetZ());
        offsetEditor = new Vec3Editor(tr, x, y, "Offset", offsetInit, v -> {
            state.setOffset(v.x, v.y, v.z);
            Logging.GUI.topic("transform").trace("Offset: ({}, {}, {})", v.x, v.y, v.z);
        });
        // Vec3Editor renders itself, not added to widgets
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Rotation
        Vector3f rotInit = new Vector3f(state.getRotationX(), state.getRotationY(), state.getRotationZ());
        rotationEditor = new Vec3Editor(tr, x, y, "Rotation", rotInit, v -> {
            state.setRotation(v.x, v.y, v.z);
            Logging.GUI.topic("transform").trace("Rotation: ({}, {}, {})", v.x, v.y, v.z);
        });
        // Vec3Editor renders itself, not added to widgets
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Scale
        scaleSlider = LabeledSlider.builder("Scale")
            .position(x, y).width(halfW)
            .range(0.1f, 5f).initial(state.getScale()).format("%.2f")
            .onChange(v -> {
                state.setScale(v);
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
        facingDropdown = GuiWidgets.enumDropdown(x, y, halfW, "Facing", FacingMode.class, FacingMode.FIXED,
            "How field orients", v -> {
                state.setFacing(v.name());
                Logging.GUI.topic("transform").debug("Facing: {}", v);
            });
        widgets.add(facingDropdown);
        
        // Billboard
        billboardDropdown = GuiWidgets.enumDropdown(x + halfW + GuiConstants.PADDING, y, halfW,
            "Billboard", BillboardMode.class, BillboardMode.NONE,
            "Camera-facing mode", v -> {
                state.setBillboard(v.name());
                Logging.GUI.topic("transform").debug("Billboard: {}", v);
            });
        widgets.add(billboardDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        section.setContentHeight(y - section.getContentY());
        Logging.GUI.topic("panel").debug("TransformSubPanel initialized");
    }
    
    @Override public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        section.render(context, tr, mouseX, mouseY, delta);
        if (section.isExpanded()) {
            for (var widget : widgets) widget.render(context, mouseX, mouseY, delta);
            // Vec3Editors render separately (need TextRenderer)
            if (offsetEditor != null) offsetEditor.render(context, tr, mouseX, mouseY, delta);
            if (rotationEditor != null) rotationEditor.render(context, tr, mouseX, mouseY, delta);
        }
    }
    
    public int getHeight() { return section.getTotalHeight(); }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> all = new ArrayList<>();
        all.add(section.getHeaderButton());
        all.addAll(widgets);
        return all;
    }
}
