package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ExpandableSection;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.visibility.MaskType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * VisibilitySubPanel - Mask type and pattern controls.
 * 
 * <p>From 03_PARAMETERS.md §7 Visibility Mask Level:</p>
 * <ul>
 *   <li>mask: FULL, BANDS, STRIPES, CHECKER, RADIAL, GRADIENT</li>
 *   <li>count, thickness, offset, invert, feather</li>
 *   <li>animate, animateSpeed</li>
 * </ul>
 */
public class VisibilitySubPanel extends AbstractPanel {
    
    private int startY;    private CyclingButtonWidget<String> fragmentDropdown;
    private boolean applyingFragment = false;
    private String currentFragment = "Default";
    
    /** Gradient direction. */
    public enum GradientDirection {
        VERTICAL("Vertical"),
        HORIZONTAL("Horizontal"),
        RADIAL("Radial");
        
        private final String label;
        GradientDirection(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    
    private CyclingButtonWidget<MaskType> maskTypeDropdown;
    private LabeledSlider countSlider;
    private LabeledSlider thicknessSlider;
    private LabeledSlider offsetSlider;
    private CyclingButtonWidget<Boolean> invertToggle;
    private LabeledSlider featherSlider;
    private CyclingButtonWidget<Boolean> animateToggle;
    private LabeledSlider animateSpeedSlider;
    public VisibilitySubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("VisibilitySubPanel created");
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

        // Preset dropdown - show "Custom" since we're loading existing primitive values
        List<String> visPresets = FragmentRegistry.listVisibilityFragments();
        currentFragment = "Custom";  // Loaded primitives have custom values

        fragmentDropdown = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal(v))
            .values(visPresets)
            .initially(currentFragment)
            .build(x, y, w, GuiConstants.WIDGET_HEIGHT, net.minecraft.text.Text.literal("Variant"),
                (btn, val) -> applyPreset(val));
        widgets.add(fragmentDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Mask type - read current value from state
        MaskType currentMaskType = state.mask() != null ? state.mask().mask() : MaskType.FULL;
        maskTypeDropdown = GuiWidgets.enumDropdown(x, y, w, "Mask Type", MaskType.class, currentMaskType,
            "Pattern visibility mask", v -> {
                onUserChange(() -> {
                    state.set("mask.mask", v.name());  // Field is named 'mask', not 'type'
                    rebuildForMaskType(v);
                    Logging.GUI.topic("visibility").debug("Mask: {}", v);
                });
            });
        widgets.add(maskTypeDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Count (bands/stripes)
        countSlider = LabeledSlider.builder("Count")
            .position(x, y).width(halfW)
            .range(1, 32).initial(state.getInt("mask.count")).format("%d").step(1)
            .onChange(v -> onUserChange(() -> {
                state.set("mask.count", v.intValue());
                Logging.GUI.topic("visibility").trace("Count: {}", v.intValue());
            })).build();
        widgets.add(countSlider);
        
        // Thickness
        thicknessSlider = LabeledSlider.builder("Thickness")
            .position(x + halfW + GuiConstants.PADDING, y).width(halfW)
            .range(0.01f, 1f).initial(state.getFloat("mask.thickness")).format("%.2f")
            .onChange(v -> onUserChange(() -> {
                state.set("mask.thickness", v);
                Logging.GUI.topic("visibility").trace("Thickness: {}", v);
            })).build();
        widgets.add(thicknessSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Offset
        offsetSlider = LabeledSlider.builder("Offset")
            .position(x, y).width(halfW)
            .range(0f, 1f).initial(state.getFloat("mask.offset")).format("%.2f")
            .onChange(v -> onUserChange(() -> {
                state.set("mask.offset", v);
                Logging.GUI.topic("visibility").trace("Offset: {}", v);
            })).build();
        widgets.add(offsetSlider);
        
        // Feather
        featherSlider = LabeledSlider.builder("Feather")
            .position(x + halfW + GuiConstants.PADDING, y).width(halfW)
            .range(0f, 1f).initial(state.getFloat("mask.feather")).format("%.2f")
            .onChange(v -> onUserChange(() -> {
                state.set("mask.feather", v);
                Logging.GUI.topic("visibility").trace("Feather: {}", v);
            })).build();
        widgets.add(featherSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Invert
        invertToggle = GuiWidgets.toggle(x, y, halfW, "Invert", state.getBool("mask.invert"),
            "Invert mask pattern", v -> {
                onUserChange(() -> {
                    state.set("mask.invert", v);
                    Logging.GUI.topic("visibility").debug("Invert: {}", v);
                });
            });
        widgets.add(invertToggle);
        
        // Animate
        animateToggle = GuiWidgets.toggle(x + halfW + GuiConstants.PADDING, y, halfW, "Animate",
            state.getBool("mask.animate"), "Animate mask pattern", v -> {
                onUserChange(() -> {
                    state.set("mask.animate", v);
                    updateAnimateWidgets();
                    Logging.GUI.topic("visibility").debug("Animate: {}", v);
                });
            });
        widgets.add(animateToggle);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Animate speed
        animateSpeedSlider = LabeledSlider.builder("Speed")
            .position(x, y).width(w)
            .range(0.1f, 10f).initial(state.getFloat("mask.animSpeed")).format("%.1f")
            .onChange(v -> onUserChange(() -> {
                state.set("mask.animSpeed", v);
                Logging.GUI.topic("visibility").trace("Animate speed: {}", v);
            })).build();
        widgets.add(animateSpeedSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        contentHeight = y - startY + GuiConstants.PADDING;
        updateAnimateWidgets();
        Logging.GUI.topic("panel").debug("VisibilitySubPanel initialized");
    }
    
    private void rebuildForMaskType(MaskType type) {
        // Show/hide controls based on mask type
        boolean hasBands = type == MaskType.BANDS || type == MaskType.STRIPES || type == MaskType.CHECKER;
        if (countSlider != null) countSlider.active = hasBands;
        if (thicknessSlider != null) thicknessSlider.active = hasBands;
    }
    
    private void updateAnimateWidgets() {
        boolean animated = state.getBool("mask.animate");
        if (animateSpeedSlider != null) animateSpeedSlider.active = animated;
    }
    
    @Override public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render widgets directly (no expandable section)
        for (var widget : widgets) widget.render(context, mouseX, mouseY, delta);
    }
    
    public int getHeight() { return contentHeight; }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> all = new ArrayList<>();
        // No header button (direct content)
        all.addAll(widgets);
        return all;
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
        FragmentRegistry.applyVisibilityFragment(state, name);
        syncFromState();
        applyingFragment = false;
    }

    private void syncFromState() {
        if (maskTypeDropdown != null) {
            try { maskTypeDropdown.setValue(MaskType.valueOf(state.getString("mask.mask"))); } catch (IllegalArgumentException ignored) {}
        }
        if (countSlider != null) countSlider.setValue(state.getInt("mask.count"));
        if (thicknessSlider != null) thicknessSlider.setValue(state.getFloat("mask.thickness"));
        if (offsetSlider != null) offsetSlider.setValue(state.getFloat("mask.offset"));
        if (featherSlider != null) featherSlider.setValue(state.getFloat("mask.feather"));
        if (invertToggle != null) invertToggle.setValue(state.getBool("mask.invert"));
        if (animateToggle != null) animateToggle.setValue(state.getBool("mask.animate"));
        if (animateSpeedSlider != null) animateSpeedSlider.setValue(state.getFloat("mask.animSpeed"));
        rebuildForMaskType(maskTypeDropdown.getValue());
        updateAnimateWidgets();
    }
}
