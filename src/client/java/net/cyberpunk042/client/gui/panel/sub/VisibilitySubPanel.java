package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.GuiState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ExpandableSection;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.log.Logging;
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
    
    private ExpandableSection section;
    private int startY;
    private final List<net.minecraft.client.gui.widget.ClickableWidget> widgets = new ArrayList<>();
    
    /** Visibility mask types. */
    public enum MaskType {
        FULL("Full (No Mask)"),
        BANDS("Horizontal Bands"),
        STRIPES("Vertical Stripes"),
        CHECKER("Checkerboard"),
        RADIAL("Radial Gradient"),
        GRADIENT("Linear Gradient");
        
        private final String label;
        MaskType(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    
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
    private CyclingButtonWidget<GradientDirection> gradientDirDropdown;
    
    public VisibilitySubPanel(Screen parent, GuiState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("VisibilitySubPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        section = new ExpandableSection(
            GuiConstants.PADDING, startY,
            width - GuiConstants.PADDING * 2,
            "Visibility Mask", false
        );
        
        int x = GuiConstants.PADDING;
        int y = section.getContentY();
        int w = width - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // Mask type
        maskTypeDropdown = GuiWidgets.enumDropdown(x, y, w, "Mask Type", MaskType.class, MaskType.FULL,
            "Pattern visibility mask", v -> {
                state.setMaskType(v.name());
                rebuildForMaskType(v);
                Logging.GUI.topic("visibility").debug("Mask: {}", v);
            });
        widgets.add(maskTypeDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Count (bands/stripes)
        countSlider = LabeledSlider.builder("Count")
            .position(x, y).width(halfW)
            .range(1, 32).initial(state.getMaskCount()).format("%d").step(1)
            .onChange(v -> {
                state.setMaskCount(v.intValue());
                Logging.GUI.topic("visibility").trace("Count: {}", v.intValue());
            }).build();
        widgets.add(countSlider);
        
        // Thickness
        thicknessSlider = LabeledSlider.builder("Thickness")
            .position(x + halfW + GuiConstants.PADDING, y).width(halfW)
            .range(0.01f, 1f).initial(state.getMaskThickness()).format("%.2f")
            .onChange(v -> {
                state.setMaskThickness(v);
                Logging.GUI.topic("visibility").trace("Thickness: {}", v);
            }).build();
        widgets.add(thicknessSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Offset
        offsetSlider = LabeledSlider.builder("Offset")
            .position(x, y).width(halfW)
            .range(0f, 1f).initial(state.getMaskOffset()).format("%.2f")
            .onChange(v -> {
                state.setMaskOffset(v);
                Logging.GUI.topic("visibility").trace("Offset: {}", v);
            }).build();
        widgets.add(offsetSlider);
        
        // Feather
        featherSlider = LabeledSlider.builder("Feather")
            .position(x + halfW + GuiConstants.PADDING, y).width(halfW)
            .range(0f, 1f).initial(state.getMaskFeather()).format("%.2f")
            .onChange(v -> {
                state.setMaskFeather(v);
                Logging.GUI.topic("visibility").trace("Feather: {}", v);
            }).build();
        widgets.add(featherSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Invert
        invertToggle = GuiWidgets.toggle(x, y, halfW, "Invert", state.isMaskInverted(),
            "Invert mask pattern", v -> {
                state.setMaskInverted(v);
                Logging.GUI.topic("visibility").debug("Invert: {}", v);
            });
        widgets.add(invertToggle);
        
        // Animate
        animateToggle = GuiWidgets.toggle(x + halfW + GuiConstants.PADDING, y, halfW, "Animate",
            state.isMaskAnimated(), "Animate mask pattern", v -> {
                state.setMaskAnimated(v);
                updateAnimateWidgets();
                Logging.GUI.topic("visibility").debug("Animate: {}", v);
            });
        widgets.add(animateToggle);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Animate speed
        animateSpeedSlider = LabeledSlider.builder("Speed")
            .position(x, y).width(w)
            .range(0.1f, 10f).initial(state.getMaskAnimateSpeed()).format("%.1f")
            .onChange(v -> {
                state.setMaskAnimateSpeed(v);
                Logging.GUI.topic("visibility").trace("Animate speed: {}", v);
            }).build();
        widgets.add(animateSpeedSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        section.setContentHeight(y - section.getContentY());
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
        boolean animated = state.isMaskAnimated();
        if (animateSpeedSlider != null) animateSpeedSlider.active = animated;
    }
    
    @Override public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        section.render(context, net.minecraft.client.MinecraftClient.getInstance().textRenderer, mouseX, mouseY, delta);
        if (section.isExpanded()) {
            for (var widget : widgets) widget.render(context, mouseX, mouseY, delta);
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
