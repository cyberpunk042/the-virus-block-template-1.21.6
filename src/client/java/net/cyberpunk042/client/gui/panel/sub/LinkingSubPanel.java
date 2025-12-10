package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiLayout;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.List;

import java.util.ArrayList;
import java.util.List;

/**
 * Sub-panel for primitive linking controls.
 * Allows linking primitives together for synchronized behavior.
 * 
 * Controls:
 * - primitiveId: Text input for this primitive's ID
 * - radiusOffset: How much to offset radius from linked primitive
 * - phaseOffset: Animation phase offset (0.0-1.0)
 * - mirror: Mirror axis (NONE, X, Y, Z)
 * - follow: Follow another primitive's position
 * - scaleWith: Scale with another primitive
 * 
 * @see <a href="GUI_CLASS_DIAGRAM.md §4.8">LinkingSubPanel specification</a>
 */
public class LinkingSubPanel {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ENUMS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Mirror axis options for linked primitives. */
    public enum MirrorAxis {
        NONE("None"),
        X("X Axis"),
        Y("Y Axis"),
        Z("Z Axis");
        
        private final String displayName;
        MirrorAxis(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FIELDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final FieldEditState state;
    private final List<ClickableWidget> widgets = new ArrayList<>();
    private final GuiLayout layout;
    
    // Widgets
    private TextFieldWidget primitiveIdField;
    private LabeledSlider radiusOffsetSlider;
    private LabeledSlider phaseOffsetSlider;
    private CyclingButtonWidget<MirrorAxis> mirrorButton;
    private CyclingButtonWidget<Boolean> followButton;
    private CyclingButtonWidget<Boolean> scaleWithButton;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a new LinkingSubPanel.
     * 
     * @param state The GUI state to bind to
     * @param x Starting X position
     * @param y Starting Y position
     * @param width Panel width
     */
    public LinkingSubPanel(FieldEditState state, int x, int y, int width) {
        this.state = state;
        this.layout = new GuiLayout(x, y, GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING, width);
        initWidgets();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void initWidgets() {
        MinecraftClient client = MinecraftClient.getInstance();
        int controlWidth = layout.getPanelWidth() - GuiConstants.PADDING * 2;
        int halfWidth = (controlWidth - GuiConstants.ELEMENT_SPACING) / 2;
        
        // ─────────────────────────────────────────────────────────────────────────
        // Primitive ID - text input for this primitive's identifier
        // ─────────────────────────────────────────────────────────────────────────
        primitiveIdField = new TextFieldWidget(
            client.textRenderer,
            layout.getStartX() + GuiConstants.PADDING,
            layout.getStartY(),
            controlWidth,
            GuiConstants.ELEMENT_HEIGHT,
            Text.literal("Primitive ID")
        );
        primitiveIdField.setText(state.getPrimitiveId());
        primitiveIdField.setMaxLength(32);
        primitiveIdField.setChangedListener(value -> state.setPrimitiveId(value));
        widgets.add(primitiveIdField);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Radius Offset - offset from linked primitive's radius
        // ─────────────────────────────────────────────────────────────────────────
        radiusOffsetSlider = new LabeledSlider(
            layout.getStartX() + GuiConstants.PADDING,
            layout.getStartY(),
            controlWidth,
            "Radius Offset",
            -10f, 10f,
            state.getRadiusOffset(),
            "%.1f", null,
            v -> state.setRadiusOffset(v)
        );
        widgets.add(radiusOffsetSlider);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Phase Offset - animation phase offset for linked primitives
        // ─────────────────────────────────────────────────────────────────────────
        phaseOffsetSlider = new LabeledSlider(
            layout.getStartX() + GuiConstants.PADDING,
            layout.getStartY(),
            controlWidth,
            "Phase Offset",
            0f, 1f,
            state.getPhaseOffset(),
            "%.2f", null,
            v -> state.setPhaseOffset(v)
        );
        widgets.add(phaseOffsetSlider);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Mirror Axis - mirror this primitive along an axis
        // ─────────────────────────────────────────────────────────────────────────
        mirrorButton = CyclingButtonWidget.<MirrorAxis>builder(axis -> Text.literal(axis.getDisplayName()))
            .values(MirrorAxis.values())
            .initially(MirrorAxis.valueOf(state.getMirrorAxis()))
            .build(
                layout.getStartX() + GuiConstants.PADDING,
                layout.getStartY(),
                controlWidth,
                GuiConstants.ELEMENT_HEIGHT,
                Text.literal("Mirror"),
                (button, value) -> state.setMirrorAxis(value.name())
            );
        widgets.add(mirrorButton);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Follow - follow another primitive's position
        // ─────────────────────────────────────────────────────────────────────────
        followButton = CyclingButtonWidget.<Boolean>builder(v -> Text.literal(v ? "Following" : "Independent"))
            .values(List.of(false, true))
            .initially(state.isFollowLinked())
            .build(
                layout.getStartX() + GuiConstants.PADDING,
                layout.getStartY(),
                halfWidth,
                GuiConstants.ELEMENT_HEIGHT,
                Text.literal("Follow"),
                (button, value) -> state.setFollowLinked(value)
            );
        widgets.add(followButton);
        
        // ─────────────────────────────────────────────────────────────────────────
        // Scale With - scale with another primitive
        // ─────────────────────────────────────────────────────────────────────────
        scaleWithButton = CyclingButtonWidget.<Boolean>builder(v -> Text.literal(v ? "Linked" : "Independent"))
            .values(List.of(false, true))
            .initially(state.isScaleWithLinked())
            .build(
                layout.getStartX() + GuiConstants.PADDING + halfWidth + GuiConstants.ELEMENT_SPACING,
                layout.getStartY(),
                halfWidth,
                GuiConstants.ELEMENT_HEIGHT,
                Text.literal("Scale With"),
                (button, value) -> state.setScaleWithLinked(value)
            );
        widgets.add(scaleWithButton);
        layout.nextRow();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Returns all widgets for registration with the parent screen. */
    public List<ClickableWidget> getWidgets() {
        return widgets;
    }
    
    /** Renders the sub-panel. */
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Widgets render themselves when registered with the screen
    }
    
    /** Returns the total height of this sub-panel. */
    public int getHeight() {
        return layout.getCurrentY() - layout.getStartY();
    }
}

