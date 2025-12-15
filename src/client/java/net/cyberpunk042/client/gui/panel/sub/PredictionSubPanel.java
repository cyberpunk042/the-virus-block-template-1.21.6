package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.RendererCapabilities.Feature;
import net.cyberpunk042.client.gui.state.RequiresFeature;
import net.minecraft.client.gui.screen.Screen;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiLayout;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Sub-panel for movement prediction settings.
 * Controls how the field anticipates player movement.
 * 
 * Features:
 * - Preset selector (OFF, LOW, MEDIUM, HIGH, CUSTOM)
 * - Custom mode exposes all parameters:
 *   - enabled: Toggle prediction on/off
 *   - leadTicks: How many ticks ahead to predict (1-10)
 *   - maxDistance: Maximum prediction distance (1-50)
 *   - lookAhead: How much to weight velocity (0-1)
 *   - verticalBoost: Extra vertical compensation (0-2)
 * 
 * @see <a href="GUI_CLASS_DIAGRAM.md §4.9">PredictionSubPanel specification</a>
 * 
 * <p><b>Requires Accurate renderer mode.</b></p>
 */
@RequiresFeature(Feature.PREDICTION)
public class PredictionSubPanel extends AbstractPanel {
    // ═══════════════════════════════════════════════════════════════════════════
    // ENUMS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Prediction presets for quick configuration. */
    public enum PredictionPreset {
        OFF("Off", false, 0, 0f, 0f, 0f),
        LOW("Low", true, 1, 4f, 0.2f, 0f),
        MEDIUM("Medium", true, 2, 8f, 0.5f, 0f),
        HIGH("High", true, 3, 12f, 0.8f, 0f),
        CUSTOM("Custom", true, 2, 8f, 0.5f, 0f);
        
        private final String displayName;
        public final boolean enabled;
        public final int leadTicks;
        public final float maxDistance;
        public final float lookAhead;
        public final float verticalBoost;
        
        PredictionPreset(String displayName, boolean enabled, int leadTicks, 
                         float maxDistance, float lookAhead, float verticalBoost) {
            this.displayName = displayName;
            this.enabled = enabled;
            this.leadTicks = leadTicks;
            this.maxDistance = maxDistance;
            this.lookAhead = lookAhead;
            this.verticalBoost = verticalBoost;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FIELDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private GuiLayout layout;
    
    // Widgets
    private CyclingButtonWidget<PredictionPreset> presetButton;
    private CyclingButtonWidget<Boolean> enabledButton;
    private CyclingButtonWidget<net.cyberpunk042.field.instance.FollowMode> followModeButton;
    private LabeledSlider leadTicksSlider;
    private LabeledSlider maxDistanceSlider;
    private LabeledSlider lookAheadSlider;
    private LabeledSlider verticalBoostSlider;
    
    // State
    private PredictionPreset currentFragment = PredictionPreset.MEDIUM;
    private boolean applyingFragment = false;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a new PredictionSubPanel.
     * 
     * @param state The GUI state to bind to
     * @param x Starting X position
     * @param y Starting Y position
     * @param width Panel width
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
        
        int x = net.cyberpunk042.client.gui.util.GuiConstants.PADDING;
        int y = startY;
        this.layout = new GuiLayout(
            x,
            y,
            GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING,
            width - GuiConstants.PADDING * 2);
        
        // Continue with original build logic...
        initWidgets();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void initWidgets() {
        int controlWidth = layout.getPanelWidth() - GuiConstants.PADDING * 2;
        
        // ─────────────────────────────────────────────────────────────────────────
        // Preset Selector - quick presets for common configurations
        // ─────────────────────────────────────────────────────────────────────────
        int rowY = layout.getCurrentY();
        presetButton = CyclingButtonWidget.<PredictionPreset>builder(p -> Text.literal(p.getDisplayName()))
            .values(PredictionPreset.values())
            .initially(currentFragment)
            .build(layout.getStartX() + GuiConstants.PADDING, rowY, controlWidth,
                GuiConstants.ELEMENT_HEIGHT, Text.literal("Variant"), (button, value) -> applyPreset(value));
        widgets.add(presetButton);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Enabled Toggle - master on/off for prediction
        // ─────────────────────────────────────────────────────────────────────────
        rowY = layout.getCurrentY();
        enabledButton = CyclingButtonWidget.<Boolean>builder(v -> Text.literal(v ? "Enabled" : "Disabled"))
            .values(List.of(false, true))
            .initially(state.getBool("prediction.enabled"))
            .build(
                layout.getStartX() + GuiConstants.PADDING,
                rowY,
                controlWidth,
                GuiConstants.ELEMENT_HEIGHT,
                Text.literal("Prediction"),
                (button, value) -> onUserChange(() -> state.set("prediction.enabled", value))
            );
        widgets.add(enabledButton);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Follow Mode - how the field follows the player (SNAP/SMOOTH/GLIDE)
        // ─────────────────────────────────────────────────────────────────────────
        rowY = layout.getCurrentY();
        net.cyberpunk042.field.instance.FollowMode initialMode = 
            state.getTyped("followConfig.mode", net.cyberpunk042.field.instance.FollowMode.class);
        if (initialMode == null) initialMode = net.cyberpunk042.field.instance.FollowMode.SMOOTH;
        followModeButton = CyclingButtonWidget.<net.cyberpunk042.field.instance.FollowMode>builder(
                mode -> Text.literal(mode.id().substring(0, 1).toUpperCase() + mode.id().substring(1) + " - " + mode.description())
            )
            .values(net.cyberpunk042.field.instance.FollowMode.values())
            .initially(initialMode)
            .build(
                layout.getStartX() + GuiConstants.PADDING,
                rowY,
                controlWidth,
                GuiConstants.ELEMENT_HEIGHT,
                Text.literal("Follow Mode"),
                (button, value) -> onUserChange(() -> state.set("followConfig.mode", value))
            );
        widgets.add(followModeButton);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Lead Ticks - how many game ticks ahead to predict
        // ─────────────────────────────────────────────────────────────────────────
        rowY = layout.getCurrentY();
        leadTicksSlider = new LabeledSlider(
            layout.getStartX() + GuiConstants.PADDING,
            rowY,
            controlWidth,
            "Lead Ticks",
            1f, 10f,
            (float) state.getInt("prediction.leadTicks"),
            "%d", 1f,
            v -> onUserChange(() -> state.set("prediction.leadTicks", v.intValue()))
        );
        widgets.add(leadTicksSlider);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Max Distance - maximum prediction distance in blocks
        // ─────────────────────────────────────────────────────────────────────────
        rowY = layout.getCurrentY();
        maxDistanceSlider = new LabeledSlider(
            layout.getStartX() + GuiConstants.PADDING,
            rowY,
            controlWidth,
            "Max Distance",
            1f, 50f,
            state.getFloat("prediction.maxDistance"),
            "%.1f", null,
            v -> onUserChange(() -> state.set("prediction.maxDistance", v))
        );
        widgets.add(maxDistanceSlider);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Look Ahead - velocity weighting factor (0 = position only, 1 = full velocity)
        // ─────────────────────────────────────────────────────────────────────────
        rowY = layout.getCurrentY();
        lookAheadSlider = new LabeledSlider(
            layout.getStartX() + GuiConstants.PADDING,
            rowY,
            controlWidth,
            "Look Ahead",
            0f, 1f,
            state.getFloat("prediction.lookAhead"),
            "%.2f", null,
            v -> onUserChange(() -> state.set("prediction.lookAhead", v))
        );
        widgets.add(lookAheadSlider);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Vertical Boost - extra compensation for vertical movement (jumping, falling)
        // ─────────────────────────────────────────────────────────────────────────
        rowY = layout.getCurrentY();
        verticalBoostSlider = new LabeledSlider(
            layout.getStartX() + GuiConstants.PADDING,
            rowY,
            controlWidth,
            "Vertical Boost",
            0f, 2f,
            state.getFloat("prediction.verticalBoost"),
            "%.2f", null,
            v -> onUserChange(() -> state.set("prediction.verticalBoost", v))
        );
        widgets.add(verticalBoostSlider);
        layout.nextRow();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRESET APPLICATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Applies a prediction preset, updating all controls and state.
     * CUSTOM preset does not change values, just enables manual editing.
     */
    private void applyPreset(PredictionPreset preset) {
        applyingFragment = true;
        currentFragment = preset;
        if (preset != PredictionPreset.CUSTOM) {
            state.set("prediction.enabled", preset.enabled);
            state.set("prediction.leadTicks", preset.leadTicks);
            state.set("prediction.maxDistance", preset.maxDistance);
            state.set("prediction.lookAhead", preset.lookAhead);
            state.set("prediction.verticalBoost", preset.verticalBoost);
        }
        boolean isCustom = (preset == PredictionPreset.CUSTOM);
        enabledButton.active = isCustom || preset == PredictionPreset.OFF;
        leadTicksSlider.active = isCustom;
        maxDistanceSlider.active = isCustom;
        lookAheadSlider.active = isCustom;
        verticalBoostSlider.active = isCustom;
        syncFromState();
        applyingFragment = false;
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

    private void syncFromState() {
        if (enabledButton != null) enabledButton.setValue(state.getBool("prediction.enabled"));
        if (followModeButton != null) {
            net.cyberpunk042.field.instance.FollowMode mode = 
                state.getTyped("followConfig.mode", net.cyberpunk042.field.instance.FollowMode.class);
            if (mode == null) mode = net.cyberpunk042.field.instance.FollowMode.SMOOTH;
            followModeButton.setValue(mode);
        }
        if (leadTicksSlider != null) leadTicksSlider.setValue(state.getInt("prediction.leadTicks"));
        if (maxDistanceSlider != null) maxDistanceSlider.setValue(state.getFloat("prediction.maxDistance"));
        if (lookAheadSlider != null) lookAheadSlider.setValue(state.getFloat("prediction.lookAhead"));
        if (verticalBoostSlider != null) verticalBoostSlider.setValue(state.getFloat("prediction.verticalBoost"));
        if (presetButton != null) presetButton.setValue(currentFragment);
    }

    private void onUserChange(Runnable r) {
        r.run();
        if (!applyingFragment) {
            currentFragment = PredictionPreset.CUSTOM;
            if (presetButton != null) presetButton.setValue(currentFragment);
        }
    }

    @Override
    public void tick() {
        // No per-tick updates needed
    }

}

