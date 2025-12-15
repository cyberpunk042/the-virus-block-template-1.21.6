package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.RendererCapabilities.Feature;
import net.cyberpunk042.client.gui.state.RequiresFeature;
import net.minecraft.client.gui.screen.Screen;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiLayout;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.field.instance.FollowMode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Sub-panel for follow mode settings.
 * Controls how the field follows the player's movement.
 * 
 * Features:
 * - enabled: Toggle field following on/off (false = static field)
 * - mode: How the field follows (SNAP, SMOOTH, GLIDE)
 * 
 * Mode Descriptions:
 * - SNAP: Field instantly teleports to player position
 * - SMOOTH: Field smoothly interpolates to player position
 * - GLIDE: Field has inertia, glides behind player with momentum
 * 
 * @see <a href="GUI_CLASS_DIAGRAM.md §4.10">FollowModeSubPanel specification</a>
 * 
 * <p><b>Requires Accurate renderer mode.</b></p>
 */
@RequiresFeature(Feature.FOLLOW_MODE)
public class FollowModeSubPanel extends AbstractPanel {
    // ═══════════════════════════════════════════════════════════════════════════
    // FIELDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private GuiLayout layout;
    private CyclingButtonWidget<String> fragmentDropdown;
    private boolean applyingFragment = false;
    private String currentFragment = "Default";
    
    // Widgets
    private CyclingButtonWidget<Boolean> enabledButton;
    private CyclingButtonWidget<FollowMode> modeButton;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a new FollowModeSubPanel.
     * 
     * @param state The GUI state to bind to
     * @param x Starting X position
     * @param y Starting Y position
     * @param width Panel width
     */
    public FollowModeSubPanel(Screen parent, FieldEditState state, int startY) {
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
        int controlWidth = layout.getPanelWidth() - GuiConstants.PADDING * 2;
        int halfWidth = (controlWidth - GuiConstants.ELEMENT_SPACING) / 2;

        // Preset dropdown - show "Custom" since we're loading existing values
        List<String> followPresets = FragmentRegistry.listFollowFragments();
        currentFragment = "Custom";  // Loaded values are custom

        fragmentDropdown = CyclingButtonWidget.<String>builder(v -> Text.literal(v))
            .values(followPresets)
            .initially(currentFragment)
            .build(layout.getStartX() + GuiConstants.PADDING, layout.getY(), controlWidth,
                GuiConstants.ELEMENT_HEIGHT, Text.literal("Variant"), (btn, val) -> applyPreset(val));
        widgets.add(fragmentDropdown);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Enabled Toggle - master on/off for field following
        // When disabled, field stays at a fixed world position
        // ─────────────────────────────────────────────────────────────────────────
        enabledButton = CyclingButtonWidget.<Boolean>builder(v -> 
                Text.literal(v ? "Following Player" : "Static Position"))
            .values(List.of(false, true))
            .initially(state.getBool("followEnabled"))
            .build(
                layout.getStartX() + GuiConstants.PADDING,
                layout.getY(),
                halfWidth,
                GuiConstants.ELEMENT_HEIGHT,
                Text.literal("Follow"),
                (button, value) -> {
                    onUserChange(() -> {
                        state.set("followEnabled", value);
                        // Disable mode selector when following is disabled
                        modeButton.active = value;
                    });
                }
            );
        widgets.add(enabledButton);
        
        // ─────────────────────────────────────────────────────────────────────────
        // Mode Selector - how the field follows the player
        // ─────────────────────────────────────────────────────────────────────────
        modeButton = CyclingButtonWidget.<FollowMode>builder(mode -> {
                return switch (mode) {
                    case SNAP -> Text.literal("Snap (Instant)");
                    case SMOOTH -> Text.literal("Smooth (Interpolated)");
                    case GLIDE -> Text.literal("Glide (Momentum)");
                };
            })
            .values(FollowMode.values())
            .initially(FollowMode.valueOf(state.getString("followMode")))
            .build(
                layout.getStartX() + GuiConstants.PADDING + halfWidth + GuiConstants.ELEMENT_SPACING,
                layout.getY(),
                halfWidth,
                GuiConstants.ELEMENT_HEIGHT,
                Text.literal("Mode"),
                (button, value) -> onUserChange(() -> state.set("followMode", value))
            );
        modeButton.active = state.getBool("followEnabled");
        widgets.add(modeButton);
        layout.nextRow();
        
        // Add spacing for description area
        layout.nextRow();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Returns all widgets for registration with the parent screen. */
    public List<ClickableWidget> getWidgets() {
        return widgets;
    }
    
    /** 
     * Renders the sub-panel including mode descriptions.
     */
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw mode description below the controls
        String description = getModeDescription(FollowMode.valueOf(state.getString("followMode")));
        int descY = layout.getY() + GuiConstants.ELEMENT_HEIGHT + GuiConstants.ELEMENT_SPACING;
        
        context.drawTextWithShadow(
            net.minecraft.client.MinecraftClient.getInstance().textRenderer,
            Text.literal(description).styled(s -> s.withColor(GuiConstants.TEXT_SECONDARY)),
            layout.getStartX() + GuiConstants.PADDING,
            descY,
            GuiConstants.TEXT_SECONDARY
        );
    }
    
    /**
     * Returns a description string for the given follow mode.
     */
    private String getModeDescription(FollowMode mode) {
        return switch (mode) {
            case SNAP -> "Field instantly follows player position";
            case SMOOTH -> "Field smoothly interpolates to player";
            case GLIDE -> "Field has inertia, trails behind player";
        };
    }
    
    /** Returns the total height of this sub-panel. */
    public int getHeight() {
        return layout.getCurrentY() - layout.getY();
    }

    private void onUserChange(Runnable r) {
        r.run();
        if (!applyingFragment) {
            currentFragment = "Custom";
            if (fragmentDropdown != null) fragmentDropdown.setValue("Custom");
        }
    }

    private void applyPreset(String name) {
        if ("Custom".equalsIgnoreCase(name)) {
            currentFragment = "Custom";
            return;
        }
        applyingFragment = true;
        currentFragment = name;
        FragmentRegistry.applyFollowFragment(state, name);
        syncFromState();
        applyingFragment = false;
    }

    private void syncFromState() {
        if (enabledButton != null) enabledButton.setValue(state.getBool("followEnabled"));
        if (modeButton != null) modeButton.setValue(FollowMode.valueOf(state.getString("followMode")));
        modeButton.active = state.getBool("followEnabled");
    }

    @Override
    public void tick() {
        // No per-tick updates needed
    }

}

