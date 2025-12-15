package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.RendererCapabilities.Feature;
import net.cyberpunk042.client.gui.state.RequiresFeature;
import net.minecraft.client.gui.screen.Screen;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiLayout;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.field.instance.FollowConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Sub-panel for follow/movement settings.
 * Controls how the field follows the player's movement.
 * 
 * The new unified FollowConfig replaces the old separate prediction and follow mode systems.
 * 
 * Features:
 * - Preset selector (LOCKED, SMOOTH, GLIDE, LEAD, CUSTOM)
 * - enabled: Toggle following on/off (false = static field)
 * - leadOffset: Position offset in movement direction (-1 = trail, +1 = lead)
 * - responsiveness: How quickly field catches up (0.1 = floaty, 1.0 = instant)
 * - lookAhead: Offset toward player's look direction (0 = none, 0.5 = half block)
 * 
 * @see <a href="GUI_CLASS_DIAGRAM.md §4.9">PredictionSubPanel specification</a>
 * 
 * <p><b>Requires Accurate renderer mode.</b></p>
 */
@RequiresFeature(Feature.PREDICTION)
public class PredictionSubPanel extends AbstractPanel {
    // ═══════════════════════════════════════════════════════════════════════════
    // FIELDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private GuiLayout layout;
    
    // Widgets
    private CyclingButtonWidget<FollowConfig.Preset> presetButton;
    private CyclingButtonWidget<Boolean> enabledButton;
    private LabeledSlider leadOffsetSlider;
    private LabeledSlider responsivenessSlider;
    private LabeledSlider lookAheadSlider;
    
    // State
    private FollowConfig.Preset currentPreset = FollowConfig.Preset.LOCKED;
    private boolean applyingPreset = false;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a new PredictionSubPanel.
     * 
     * @param parent Parent screen
     * @param state The GUI state to bind to
     * @param startY Starting Y position
     */
    public PredictionSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
    }
    
    private int startY;
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        int x = GuiConstants.PADDING;
        int y = startY;
        this.layout = new GuiLayout(
            x,
            y,
            GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING,
            width - GuiConstants.PADDING * 2);
        
        initWidgets();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void initWidgets() {
        int controlWidth = layout.getPanelWidth() - GuiConstants.PADDING * 2;
        int halfWidth = (controlWidth - GuiConstants.ELEMENT_SPACING) / 2;
        
        // ─────────────────────────────────────────────────────────────────────────
        // Preset Selector - quick presets for common configurations
        // ─────────────────────────────────────────────────────────────────────────
        int rowY = layout.getCurrentY();
        presetButton = CyclingButtonWidget.<FollowConfig.Preset>builder(
                p -> Text.literal(p.label() + " - " + p.description())
            )
            .values(FollowConfig.Preset.values())
            .initially(currentPreset)
            .build(layout.getStartX() + GuiConstants.PADDING, rowY, controlWidth,
                GuiConstants.ELEMENT_HEIGHT, Text.literal("Preset"), 
                (button, value) -> applyPreset(value));
        widgets.add(presetButton);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Enabled Toggle - master on/off for following
        // ─────────────────────────────────────────────────────────────────────────
        rowY = layout.getCurrentY();
        enabledButton = CyclingButtonWidget.<Boolean>builder(
                v -> Text.literal(v ? "Following" : "Static")
            )
            .values(List.of(false, true))
            .initially(state.getBool("follow.enabled"))
            .build(
                layout.getStartX() + GuiConstants.PADDING,
                rowY,
                controlWidth,
                GuiConstants.ELEMENT_HEIGHT,
                Text.literal("Follow"),
                (button, value) -> onUserChange(() -> {
                    state.set("follow.enabled", value);
                    updateSliderStates(value);
                })
            );
        widgets.add(enabledButton);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Lead Offset - trail/lead in movement direction
        // -1 = trail behind, 0 = locked, +1 = lead ahead
        // ─────────────────────────────────────────────────────────────────────────
        rowY = layout.getCurrentY();
        leadOffsetSlider = new LabeledSlider(
            layout.getStartX() + GuiConstants.PADDING,
            rowY,
            controlWidth,
            "Lead/Trail",
            -1.0f, 1.0f,
            state.getFloat("follow.leadOffset"),
            "%.2f", null,
            v -> onUserChange(() -> state.set("follow.leadOffset", v))
        );
        widgets.add(leadOffsetSlider);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Responsiveness - how quickly field catches up
        // 0.1 = very floaty/slow, 1.0 = instant snap
        // ─────────────────────────────────────────────────────────────────────────
        rowY = layout.getCurrentY();
        responsivenessSlider = new LabeledSlider(
            layout.getStartX() + GuiConstants.PADDING,
            rowY,
            controlWidth,
            "Responsiveness",
            0.1f, 1.0f,
            state.getFloat("follow.responsiveness"),
            "%.2f", null,
            v -> onUserChange(() -> state.set("follow.responsiveness", v))
        );
        widgets.add(responsivenessSlider);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Look Ahead - offset toward look direction
        // 0 = none, 0.5 = half block in look direction
        // ─────────────────────────────────────────────────────────────────────────
        rowY = layout.getCurrentY();
        lookAheadSlider = new LabeledSlider(
            layout.getStartX() + GuiConstants.PADDING,
            rowY,
            controlWidth,
            "Look Ahead",
            0.0f, 0.5f,
            state.getFloat("follow.lookAhead"),
            "%.2f", null,
            v -> onUserChange(() -> state.set("follow.lookAhead", v))
        );
        widgets.add(lookAheadSlider);
        layout.nextRow();
        
        // Initialize slider states
        updateSliderStates(state.getBool("follow.enabled"));
    }
    
    private void updateSliderStates(boolean enabled) {
        if (leadOffsetSlider != null) leadOffsetSlider.active = enabled;
        if (responsivenessSlider != null) responsivenessSlider.active = enabled;
        if (lookAheadSlider != null) lookAheadSlider.active = enabled;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Returns all widgets for registration with the parent screen. */
    public List<ClickableWidget> getWidgets() {
        return widgets;
    }
    
    /** 
     * Renders the sub-panel including description.
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render widgets
        for (ClickableWidget widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
        
        // Draw description based on current values
        float leadOffset = state.getFloat("follow.leadOffset");
        String description;
        if (!state.getBool("follow.enabled")) {
            description = "Field is static (not following)";
        } else if (Math.abs(leadOffset) < 0.05f) {
            description = "Locked to player position";
        } else if (leadOffset < 0) {
            description = "Trailing behind player movement";
        } else {
            description = "Leading ahead of player movement";
        }
        
        int descY = layout.getCurrentY() + GuiConstants.ELEMENT_SPACING;
        context.drawTextWithShadow(
            net.minecraft.client.MinecraftClient.getInstance().textRenderer,
            Text.literal(description).styled(s -> s.withColor(GuiConstants.TEXT_SECONDARY)),
            layout.getStartX() + GuiConstants.PADDING,
            descY,
            GuiConstants.TEXT_SECONDARY
        );
    }
    
    /** Returns the total height of this sub-panel. */
    public int getHeight() {
        return layout.getCurrentY() - startY + GuiConstants.WIDGET_HEIGHT;
    }

    private void onUserChange(Runnable r) {
        r.run();
        if (!applyingPreset) {
            currentPreset = FollowConfig.Preset.CUSTOM;
            if (presetButton != null) presetButton.setValue(currentPreset);
        }
    }

    private void applyPreset(FollowConfig.Preset preset) {
        currentPreset = preset;
        if (preset == FollowConfig.Preset.CUSTOM) {
            return; // Don't change values for custom
        }
        
        applyingPreset = true;
        FollowConfig config = preset.config();
        if (config != null) {
            state.set("follow.enabled", config.enabled());
            state.set("follow.leadOffset", config.leadOffset());
            state.set("follow.responsiveness", config.responsiveness());
            state.set("follow.lookAhead", config.lookAhead());
            syncFromState();
        }
        applyingPreset = false;
    }

    private void syncFromState() {
        if (enabledButton != null) enabledButton.setValue(state.getBool("follow.enabled"));
        if (leadOffsetSlider != null) leadOffsetSlider.setValue(state.getFloat("follow.leadOffset"));
        if (responsivenessSlider != null) responsivenessSlider.setValue(state.getFloat("follow.responsiveness"));
        if (lookAheadSlider != null) lookAheadSlider.setValue(state.getFloat("follow.lookAhead"));
        updateSliderStates(state.getBool("follow.enabled"));
    }

    @Override
    public void tick() {
        // No per-tick updates needed
    }
}
