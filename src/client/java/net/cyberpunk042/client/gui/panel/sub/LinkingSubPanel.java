package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.minecraft.client.gui.screen.Screen;

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
public class LinkingSubPanel extends AbstractPanel {

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
    
    private GuiLayout layout;
    
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
    public LinkingSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
    }
    
    private int startY;
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        int x = net.cyberpunk042.client.gui.util.GuiConstants.PADDING;
        int y = startY;
        this.layout = new GuiLayout(x, y, net.cyberpunk042.client.gui.util.GuiConstants.WIDGET_HEIGHT + net.cyberpunk042.client.gui.util.GuiConstants.PADDING, width - net.cyberpunk042.client.gui.util.GuiConstants.PADDING * 2);
        
        // Original build logic...
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
            layout.getY(),
            controlWidth,
            GuiConstants.ELEMENT_HEIGHT,
            Text.literal("Primitive ID")
        );
        primitiveIdField.setText(state.getString("primitiveId"));
        primitiveIdField.setMaxLength(32);
        primitiveIdField.setChangedListener(value -> state.set("primitiveId", value));
        widgets.add(primitiveIdField);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Radius Offset - offset from linked primitive's radius
        // ─────────────────────────────────────────────────────────────────────────
        radiusOffsetSlider = new LabeledSlider(
            layout.getStartX() + GuiConstants.PADDING,
            layout.getY(),
            controlWidth,
            "Radius Offset",
            -10f, 10f,
            state.getFloat("radiusOffset"),
            "%.1f", null,
            v -> state.set("radiusOffset", v)
        );
        widgets.add(radiusOffsetSlider);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Phase Offset - animation phase offset for linked primitives
        // ─────────────────────────────────────────────────────────────────────────
        phaseOffsetSlider = new LabeledSlider(
            layout.getStartX() + GuiConstants.PADDING,
            layout.getY(),
            controlWidth,
            "Phase Offset",
            0f, 1f,
            state.getFloat("phaseOffset"),
            "%.2f", null,
            v -> state.set("phaseOffset", v)
        );
        widgets.add(phaseOffsetSlider);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Mirror Axis - mirror this primitive along an axis
        // ─────────────────────────────────────────────────────────────────────────
        mirrorButton = CyclingButtonWidget.<MirrorAxis>builder(axis -> Text.literal(axis.getDisplayName()))
            .values(MirrorAxis.values())
            .initially(MirrorAxis.valueOf(state.getString("mirrorAxis")))
            .build(
                layout.getStartX() + GuiConstants.PADDING,
                layout.getY(),
                controlWidth,
                GuiConstants.ELEMENT_HEIGHT,
                Text.literal("Mirror"),
                (button, value) -> state.set("mirrorAxis", value.name())
            );
        widgets.add(mirrorButton);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Follow - follow another primitive's position
        // ─────────────────────────────────────────────────────────────────────────
        followButton = CyclingButtonWidget.<Boolean>builder(v -> Text.literal(v ? "Following" : "Independent"))
            .values(List.of(false, true))
            .initially(state.getBool("followLinked"))
            .build(
                layout.getStartX() + GuiConstants.PADDING,
                layout.getY(),
                halfWidth,
                GuiConstants.ELEMENT_HEIGHT,
                Text.literal("Follow"),
                (button, value) -> state.set("followLinked", value)
            );
        widgets.add(followButton);
        
        // ─────────────────────────────────────────────────────────────────────────
        // Scale With - scale with another primitive
        // ─────────────────────────────────────────────────────────────────────────
        scaleWithButton = CyclingButtonWidget.<Boolean>builder(v -> Text.literal(v ? "Linked" : "Independent"))
            .values(List.of(false, true))
            .initially(state.getBool("scaleWithLinked"))
            .build(
                layout.getStartX() + GuiConstants.PADDING + halfWidth + GuiConstants.ELEMENT_SPACING,
                layout.getY(),
                halfWidth,
                GuiConstants.ELEMENT_HEIGHT,
                Text.literal("Scale With"),
                (button, value) -> state.set("scaleWithLinked", value)
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
        return layout.getCurrentY() - layout.getY();
    }

    @Override
    public void tick() {
        // No per-tick updates needed
    }

}

