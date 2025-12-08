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
 * FillSubPanel - Detailed fill mode options.
 * 
 * <p>From 03_PARAMETERS.md §6 Fill Level:</p>
 * <ul>
 *   <li>wireThickness, doubleSided, depthTest, depthWrite</li>
 *   <li>Cage: latitudeCount, longitudeCount, showEquator, showPoles</li>
 *   <li>Points: pointSize</li>
 * </ul>
 */
public class FillSubPanel extends AbstractPanel {
    
    private ExpandableSection section;
    private int startY;
    private final List<net.minecraft.client.gui.widget.ClickableWidget> widgets = new ArrayList<>();
    
    private LabeledSlider wireThicknessSlider;
    private CyclingButtonWidget<Boolean> doubleSidedToggle;
    private CyclingButtonWidget<Boolean> depthTestToggle;
    private CyclingButtonWidget<Boolean> depthWriteToggle;
    
    // Cage-specific
    private LabeledSlider latCountSlider;
    private LabeledSlider lonCountSlider;
    private CyclingButtonWidget<Boolean> showEquatorToggle;
    private CyclingButtonWidget<Boolean> showPolesToggle;
    
    // Points-specific
    private LabeledSlider pointSizeSlider;
    
    public FillSubPanel(Screen parent, GuiState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("FillSubPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        section = new ExpandableSection(
            GuiConstants.PADDING, startY,
            width - GuiConstants.PADDING * 2,
            "Fill Options", false
        );
        
        int x = GuiConstants.PADDING;
        int y = section.getContentY();
        int w = width - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // Wire thickness
        wireThicknessSlider = LabeledSlider.builder("Wire Thickness")
            .position(x, y).width(w)
            .range(0.1f, 5f).initial(state.getWireThickness()).format("%.1f")
            .onChange(v -> {
                state.setWireThickness(v);
                Logging.GUI.topic("fill").trace("Wire thickness: {}", v);
            }).build();
        widgets.add(wireThicknessSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Double sided
        doubleSidedToggle = GuiWidgets.toggle(x, y, halfW, "Double Sided", state.isDoubleSided(),
            "Render both faces", v -> {
                state.setDoubleSided(v);
                Logging.GUI.topic("fill").debug("Double sided: {}", v);
            });
        widgets.add(doubleSidedToggle);
        
        // Depth test
        depthTestToggle = GuiWidgets.toggle(x + halfW + GuiConstants.PADDING, y, halfW, "Depth Test",
            state.isDepthTest(), "Enable depth testing", v -> {
                state.setDepthTest(v);
                Logging.GUI.topic("fill").debug("Depth test: {}", v);
            });
        widgets.add(depthTestToggle);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Cage settings section
        latCountSlider = LabeledSlider.builder("Lat Lines")
            .position(x, y).width(halfW)
            .range(1, 32).initial(state.getCageLatCount()).format("%d").step(1)
            .onChange(v -> {
                state.setCageLatCount(v.intValue());
                Logging.GUI.topic("fill").trace("Cage lat: {}", v.intValue());
            }).build();
        widgets.add(latCountSlider);
        
        lonCountSlider = LabeledSlider.builder("Lon Lines")
            .position(x + halfW + GuiConstants.PADDING, y).width(halfW)
            .range(1, 64).initial(state.getCageLonCount()).format("%d").step(1)
            .onChange(v -> {
                state.setCageLonCount(v.intValue());
                Logging.GUI.topic("fill").trace("Cage lon: {}", v.intValue());
            }).build();
        widgets.add(lonCountSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Point size
        pointSizeSlider = LabeledSlider.builder("Point Size")
            .position(x, y).width(w)
            .range(1f, 10f).initial(state.getPointSize()).format("%.1f")
            .onChange(v -> {
                state.setPointSize(v);
                Logging.GUI.topic("fill").trace("Point size: {}", v);
            }).build();
        widgets.add(pointSizeSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        section.setContentHeight(y - section.getContentY());
        Logging.GUI.topic("panel").debug("FillSubPanel initialized");
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
