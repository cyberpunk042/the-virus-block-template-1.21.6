package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.annotation.ShowWhen;
import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.client.gui.visibility.WidgetVisibilityResolver;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.visibility.MaskType;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.lang.reflect.Field;
import java.util.List;

/**
 * VisibilitySubPanel - Mask type and pattern controls with annotation-based visibility.
 * 
 * <p>Uses {@link ShowWhen} annotations to conditionally show controls based on mask type.
 * When mask is FULL, most pattern controls are hidden since they have no effect.</p>
 * 
 * <p>From 03_PARAMETERS.md §7 Visibility Mask Level:</p>
 * <ul>
 *   <li>mask: FULL, BANDS, STRIPES, CHECKER, RADIAL, GRADIENT</li>
 *   <li>count, thickness, offset, invert, feather</li>
 *   <li>animate, animateSpeed</li>
 * </ul>
 */
public class VisibilitySubPanel extends AbstractPanel {
    
    private int startY;
    private boolean applyingFragment = false;
    private String currentFragment = "Default";
    
    // Callback when widgets change (for re-registration with parent screen)
    private Runnable widgetChangedCallback;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ALWAYS VISIBLE widgets
    // ═══════════════════════════════════════════════════════════════════════════
    
    private CyclingButtonWidget<String> fragmentDropdown;
    private CyclingButtonWidget<MaskType> maskTypeDropdown;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONDITIONALLY VISIBLE widgets (hidden when mask = FULL)
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Band/Stripe controls - only for BANDS, STRIPES, CHECKER
    @ShowWhen(maskType = "BANDS")
    @ShowWhen(maskType = "STRIPES")
    @ShowWhen(maskType = "CHECKER")
    private LabeledSlider countSlider;
    
    @ShowWhen(maskType = "BANDS")
    @ShowWhen(maskType = "STRIPES")
    @ShowWhen(maskType = "CHECKER")
    private LabeledSlider thicknessSlider;
    
    // Common pattern controls - hidden only for FULL
    @ShowWhen(maskType = "FULL", not = true)
    private LabeledSlider offsetSlider;
    
    @ShowWhen(maskType = "FULL", not = true)
    private LabeledSlider featherSlider;
    
    @ShowWhen(maskType = "FULL", not = true)
    private CyclingButtonWidget<Boolean> invertToggle;
    
    // Animation - available for all patterns except FULL
    @ShowWhen(maskType = "FULL", not = true)
    private CyclingButtonWidget<Boolean> animateToggle;
    
    @ShowWhen(maskType = "FULL", not = true)
    private LabeledSlider animateSpeedSlider;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & INIT
    // ═══════════════════════════════════════════════════════════════════════════
    
    public VisibilitySubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("VisibilitySubPanel created");
    }
    
    /** Sets callback for when widgets are rebuilt (for screen widget re-registration). */
    public void setWidgetChangedCallback(Runnable callback) {
        this.widgetChangedCallback = callback;
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        rebuildWidgets();
        Logging.GUI.topic("panel").debug("VisibilitySubPanel initialized");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET BUILDING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Rebuilds widgets based on current mask type.
     * Uses @ShowWhen annotations to determine visibility.
     */
    private void rebuildWidgets() {
        boolean needsOffset = bounds != null && !bounds.isEmpty();
        widgets.clear();
        
        // Get current mask type (resolver reads from state.mask() directly)
        MaskType currentMask = state.mask() != null ? state.mask().mask() : MaskType.FULL;
        
        int x = GuiConstants.PADDING;
        int y = startY + GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // Build all widgets
        buildAllWidgets(x, w, halfW, currentMask);
        
        // Add visible widgets with dynamic positioning
        y = addWidget(fragmentDropdown, x, y, w);
        y = addWidget(maskTypeDropdown, x, y, w);
        
        // Count + Thickness (side by side)
        if (shouldShowField("countSlider")) {
            y = addWidgetPair(countSlider, thicknessSlider, x, y, halfW);
        }
        
        // Offset + Feather (side by side)
        if (shouldShowField("offsetSlider")) {
            y = addWidgetPair(offsetSlider, featherSlider, x, y, halfW);
        }
        
        // Invert + Animate (side by side)
        if (shouldShowField("invertToggle")) {
            y = addWidgetPair(invertToggle, animateToggle, x, y, halfW);
        }
        
        // Animation speed
        y = addConditionalWidget("animateSpeedSlider", x, y, w);
        
        contentHeight = y - startY + GuiConstants.PADDING;
        
        if (needsOffset) {
            applyBoundsOffset();
        }
        
        // Update animation speed visibility based on animate toggle
        updateAnimateWidgets();
        
        Logging.GUI.topic("panel").debug("VisibilitySubPanel rebuilt: {} widgets visible", widgets.size());
    }
    
    /**
     * Builds all widget instances at (0,0).
     */
    private void buildAllWidgets(int x, int w, int halfW, MaskType currentMask) {
        // Fragment dropdown
        List<String> visPresets = FragmentRegistry.listVisibilityFragments();
        currentFragment = "Custom";
        fragmentDropdown = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal(v))
            .values(visPresets)
            .initially(currentFragment)
            .build(0, 0, w, GuiConstants.WIDGET_HEIGHT, net.minecraft.text.Text.literal("Variant"),
                (btn, val) -> applyPreset(val));
        
        // Mask type dropdown
        maskTypeDropdown = GuiWidgets.enumDropdown(0, 0, w, "Mask Type", MaskType.class, currentMask,
            "Pattern visibility mask", v -> {
                onUserChange(() -> {
                    state.set("mask.mask", v.name());
                    Logging.GUI.topic("visibility").debug("Mask type changed to: {}", v);
                    rebuildWidgets();
                    if (widgetChangedCallback != null) {
                        widgetChangedCallback.run();
                    }
                });
            });
        
        // Count slider
        countSlider = LabeledSlider.builder("Count")
            .position(0, 0).width(halfW)
            .range(1, 32).initial(state.getInt("mask.count")).format("%d").step(1)
            .onChange(v -> onUserChange(() -> {
                state.set("mask.count", v.intValue());
            })).build();
        
        // Thickness slider
        thicknessSlider = LabeledSlider.builder("Thickness")
            .position(0, 0).width(halfW)
            .range(0.01f, 1f).initial(state.getFloat("mask.thickness")).format("%.2f")
            .onChange(v -> onUserChange(() -> {
                state.set("mask.thickness", v);
            })).build();
        
        // Offset slider
        offsetSlider = LabeledSlider.builder("Offset")
            .position(0, 0).width(halfW)
            .range(0f, 1f).initial(state.getFloat("mask.offset")).format("%.2f")
            .onChange(v -> onUserChange(() -> {
                state.set("mask.offset", v);
            })).build();
        
        // Feather slider
        featherSlider = LabeledSlider.builder("Feather")
            .position(0, 0).width(halfW)
            .range(0f, 1f).initial(state.getFloat("mask.feather")).format("%.2f")
            .onChange(v -> onUserChange(() -> {
                state.set("mask.feather", v);
            })).build();
        
        // Invert toggle
        invertToggle = GuiWidgets.toggle(0, 0, halfW, "Invert", state.getBool("mask.invert"),
            "Invert mask pattern", v -> onUserChange(() -> state.set("mask.invert", v)));
        
        // Animate toggle
        animateToggle = GuiWidgets.toggle(0, 0, halfW, "Animate", state.getBool("mask.animate"),
            "Animate mask pattern", v -> {
                onUserChange(() -> {
                    state.set("mask.animate", v);
                    updateAnimateWidgets();
                });
            });
        
        // Animation speed slider
        animateSpeedSlider = LabeledSlider.builder("Speed")
            .position(0, 0).width(w)
            .range(0.1f, 10f).initial(state.getFloat("mask.animSpeed")).format("%.1f")
            .onChange(v -> onUserChange(() -> {
                state.set("mask.animSpeed", v);
            })).build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET POSITIONING HELPERS (using parent's shared methods)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private int addWidget(ClickableWidget widget, int x, int y, int w) {
        return positionAndAddWidget(widget, x, y, w);
    }
    
    private int addWidgetPair(ClickableWidget left, ClickableWidget right, int x, int y, int halfW) {
        return positionAndAddWidgetPair(left, right, x, y, halfW);
    }
    
    private int addConditionalWidget(String fieldName, int x, int y, int w) {
        if (!shouldShowField(fieldName)) return y;
        ClickableWidget widget = getWidgetByFieldName(fieldName);
        if (widget == null) return y;
        return addWidget(widget, x, y, w);
    }
    
    private boolean shouldShowField(String fieldName) {
        return WidgetVisibilityResolver.shouldShow(getClass(), fieldName, state);
    }
    
    private ClickableWidget getWidgetByFieldName(String fieldName) {
        try {
            Field field = getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (ClickableWidget) field.get(this);
        } catch (Exception e) {
            return null;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void updateAnimateWidgets() {
        // Speed slider only active when animate is on
        boolean animated = state.getBool("mask.animate");
        if (animateSpeedSlider != null) {
            animateSpeedSlider.active = animated;
        }
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
            currentFragment = name;
            return;
        }
        applyingFragment = true;
        currentFragment = name;
        
        if ("Default".equalsIgnoreCase(name)) {
            // Apply actual default visibility mask
            state.set("mask", net.cyberpunk042.field.loader.DefaultsProvider.getDefaultVisibility());
        } else {
            FragmentRegistry.applyVisibilityFragment(state, name);
        }
        
        // Rebuild widgets to show correct fields for new mask type (like mode change does)
        rebuildWidgets();
        notifyWidgetsChanged();
        applyingFragment = false;
    }
    
    /** Syncs widget values from state without rebuilding widgets. */
    private void syncFromState() {
        if (maskTypeDropdown != null) {
            try { 
                MaskType m = state.mask() != null ? state.mask().mask() : MaskType.FULL;
                maskTypeDropdown.setValue(m); 
            } catch (Exception ignored) {}
        }
        if (countSlider != null) countSlider.setValue(state.getInt("mask.count"));
        if (thicknessSlider != null) thicknessSlider.setValue(state.getFloat("mask.thickness"));
        if (offsetSlider != null) offsetSlider.setValue(state.getFloat("mask.offset"));
        if (featherSlider != null) featherSlider.setValue(state.getFloat("mask.feather"));
        if (invertToggle != null) invertToggle.setValue(state.getBool("mask.invert"));
        if (animateToggle != null) animateToggle.setValue(state.getBool("mask.animate"));
        if (animateSpeedSlider != null) animateSpeedSlider.setValue(state.getFloat("mask.animSpeed"));
        // NOTE: Do NOT rebuild here - just update values on existing widgets
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override 
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        for (var widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
    }
    
    public int getHeight() { 
        return contentHeight; 
    }
}
