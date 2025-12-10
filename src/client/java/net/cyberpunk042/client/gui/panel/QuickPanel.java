package net.cyberpunk042.client.gui.panel;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiLayout;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ColorButton;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.fill.FillMode;
import net.cyberpunk042.visual.shape.ShapeRegistry;
import net.cyberpunk042.field.instance.FollowMode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * G41-G50: Quick customization panel (Level 1).
 */
public class QuickPanel extends AbstractPanel {
    
    private LayerPanel layerPanel;
    private PrimitivePanel primitivePanel;
    private CyclingButtonWidget<String> shapeDropdown;
    private LabeledSlider radiusSlider;
    private ColorButton colorButton;
    private LabeledSlider alphaSlider;
    private CyclingButtonWidget<FillMode> fillDropdown;
    private LabeledSlider spinSlider;
    private CyclingButtonWidget<FollowMode> followDropdown;
    private CyclingButtonWidget<Boolean> predictionToggle;
    private CyclingButtonWidget<PredictionPreset> predictionPreset;
    
    private GuiLayout layout;
    
    public enum PredictionPreset {
        OFF("Off"), LOW("Low"), MEDIUM("Medium"), HIGH("High"), CUSTOM("Custom");
        private final String label;
        PredictionPreset(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    
    public QuickPanel(Screen parent, FieldEditState state) {
        super(parent, state);
        Logging.GUI.topic("panel").debug("QuickPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        
        int startY = GuiConstants.TAB_HEIGHT + GuiConstants.PADDING;
        
        // Layer management row
        layerPanel = new LayerPanel(parent, state, startY);
        layerPanel.init(width, height);
        startY += layerPanel.getHeight() + GuiConstants.SECTION_SPACING;
        
        // Primitive management row
        primitivePanel = new PrimitivePanel(parent, state, startY);
        primitivePanel.init(width, height);
        layerPanel.setOnLayerChanged(() -> primitivePanel.refreshForLayerChange());
        startY += primitivePanel.getHeight() + GuiConstants.SECTION_SPACING;
        
        this.layout = new GuiLayout(GuiConstants.PADDING, startY);
        
        int widgetWidth = Math.min(width - GuiConstants.PADDING * 2, GuiConstants.SLIDER_WIDTH);
        int x = layout.getX();
        
        // G42: Shape type - dynamic from ShapeRegistry
        List<String> shapeNames = new ArrayList<>(ShapeRegistry.names());
        shapeDropdown = CyclingButtonWidget.<String>builder(Text::literal)
            .values(shapeNames)
            .initially(state.getShapeType())
            .tooltip(val -> Tooltip.of(Text.literal("Shape type")))
            .build(x, layout.nextRow(), widgetWidth, GuiConstants.WIDGET_HEIGHT, 
                   Text.literal("Shape"), (btn, val) -> onShapeChanged(val));
        
        // G43: Radius
        radiusSlider = LabeledSlider.builder("Radius")
            .position(x, layout.nextRow())
            .width(widgetWidth)
            .range(0.1f, 10.0f)
            .initial(state.getRadius())
            .format("%.1f")
            .step(0.1f)
            .onChange(this::onRadiusChanged)
            .build();
        
        // G44: Color
        colorButton = ColorButton.create(x, layout.nextRow(), "Color", 
            state.getColor(), this::onColorChanged);
        
        // G45: Alpha
        alphaSlider = LabeledSlider.builder("Alpha")
            .position(x, layout.nextRow())
            .width(widgetWidth)
            .range(0.0f, 1.0f)
            .initial(state.getAlpha())
            .format("%.2f")
            .onChange(this::onAlphaChanged)
            .build();
        
        // G46: Fill mode (fixed order: label, enumClass, initial, tooltip, onChange)
        fillDropdown = GuiWidgets.enumDropdown(
            x, layout.nextRow(), widgetWidth,
            "Fill", FillMode.class, state.getFillMode(), "Fill mode",
            this::onFillChanged
        );
        
        // G47: Spin
        spinSlider = LabeledSlider.builder("Spin")
            .position(x, layout.nextRow())
            .width(widgetWidth)
            .range(-0.5f, 0.5f)
            .initial(state.getSpinSpeed())
            .format("%.2f")
            .onChange(this::onSpinChanged)
            .build();
        
        // G48: Follow mode
        followDropdown = GuiWidgets.enumDropdown(
            x, layout.nextRow(), widgetWidth,
            "Follow", FollowMode.class, state.getFollowMode(), "Follow mode",
            this::onFollowChanged
        );
        
        // G49: Prediction toggle (fixed order: label, initial, tooltip, onChange)
        predictionToggle = GuiWidgets.toggle(
            x, layout.nextRow(), widgetWidth,
            "Prediction", state.isPredictionEnabled(), "Enable prediction",
            this::onPredictionToggled
        );
        
        // G50: Prediction preset
        predictionPreset = GuiWidgets.enumDropdown(
            x, layout.nextRow(), widgetWidth,
            "Preset", PredictionPreset.class, PredictionPreset.MEDIUM, "Prediction preset",
            this::onPresetChanged
        );
        
        Logging.GUI.topic("panel").debug("QuickPanel initialized");
    }
    
    private void onShapeChanged(String type) { state.setShapeType(type); }
    private void onRadiusChanged(float v) { state.setRadius(v); }
    private void onColorChanged(int c) { state.setColor(c); }
    private void onAlphaChanged(float v) { state.setAlpha(v); }
    private void onFillChanged(FillMode m) { state.setFillMode(m); }
    private void onSpinChanged(float v) { state.setSpinSpeed(v); }
    private void onFollowChanged(FollowMode m) { state.setFollowMode(m); }
    private void onPredictionToggled(boolean e) { state.setPredictionEnabled(e); }
    private void onPresetChanged(PredictionPreset p) { /* TODO */ }
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, GuiConstants.TAB_HEIGHT, panelWidth, layout.getCurrentY() + GuiConstants.PADDING, GuiConstants.BG_PANEL);
        
        if (layerPanel != null) layerPanel.render(context, mouseX, mouseY, delta);
        if (primitivePanel != null) primitivePanel.render(context, mouseX, mouseY, delta);
        if (shapeDropdown != null) shapeDropdown.render(context, mouseX, mouseY, delta);
        if (radiusSlider != null) radiusSlider.render(context, mouseX, mouseY, delta);
        if (colorButton != null) colorButton.render(context, mouseX, mouseY, delta);
        if (alphaSlider != null) alphaSlider.render(context, mouseX, mouseY, delta);
        if (fillDropdown != null) fillDropdown.render(context, mouseX, mouseY, delta);
        if (spinSlider != null) spinSlider.render(context, mouseX, mouseY, delta);
        if (followDropdown != null) followDropdown.render(context, mouseX, mouseY, delta);
        if (predictionToggle != null) predictionToggle.render(context, mouseX, mouseY, delta);
        if (predictionPreset != null) predictionPreset.render(context, mouseX, mouseY, delta);
    }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> list = new ArrayList<>();
        if (layerPanel != null) list.addAll(layerPanel.getWidgets());
        if (primitivePanel != null) list.addAll(primitivePanel.getWidgets());
        if (shapeDropdown != null) list.add(shapeDropdown);
        if (radiusSlider != null) list.add(radiusSlider);
        if (colorButton != null) list.add(colorButton);
        if (alphaSlider != null) list.add(alphaSlider);
        if (fillDropdown != null) list.add(fillDropdown);
        if (spinSlider != null) list.add(spinSlider);
        if (followDropdown != null) list.add(followDropdown);
        if (predictionToggle != null) list.add(predictionToggle);
        if (predictionPreset != null) list.add(predictionPreset);
        return list;
    }
}
