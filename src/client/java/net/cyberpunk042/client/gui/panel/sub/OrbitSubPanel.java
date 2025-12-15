package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * G-ORBIT: Transform orbit controls sub-panel.
 * 
 * <p>Controls orbital motion around the anchor point:</p>
 * <ul>
 *   <li>Enable - Toggle orbit on/off</li>
 *   <li>Radius - Distance from anchor</li>
 *   <li>Speed - Rotation speed (rotations per second)</li>
 *   <li>Axis - X, Y, or Z axis</li>
 *   <li>Phase - Starting phase offset (0-360°)</li>
 * </ul>
 */
public class OrbitSubPanel extends AbstractPanel {    private int startY;
    
    private CheckboxWidget enabledCheckbox;
    private LabeledSlider radiusSlider;
    private LabeledSlider speedSlider;
    private CyclingButtonWidget<OrbitAxis> axisDropdown;
    private LabeledSlider phaseSlider;
    
    public enum OrbitAxis {
        X("X Axis"), Y("Y Axis"), Z("Z Axis");
        private final String label;
        OrbitAxis(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public OrbitSubPanel(Screen parent, FieldEditState state) {
        super(parent, state);
    }

    @Override
    public void init(int width, int height) {
        widgets.clear();
        
        int w = width - 2 * GuiConstants.PADDING;
        int currentY = GuiConstants.TAB_HEIGHT + GuiConstants.PADDING;
        int sliderX = GuiConstants.PADDING;
        this.startY = currentY;
        
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        
        // Section header drawn in render()
        currentY += 14;
        
        // Get orbit from transform
        var orbit = state.transform() != null ? state.transform().orbit() : null;
        boolean orbitEnabled = orbit != null && orbit.isActive();
        
        enabledCheckbox = GuiWidgets.checkbox(sliderX, currentY, "Enable Orbit", orbitEnabled,
            "Shape orbits around anchor point", textRenderer, v -> state.set("transform.orbit.enabled", v));
        widgets.add(enabledCheckbox);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        radiusSlider = LabeledSlider.builder("Radius")
            .position(sliderX, currentY).width(w)
            .range(0.1f, 20f).initial(orbit != null ? orbit.radius() : 2.0f).format("%.1f")
            .onChange(v -> state.set("transform.orbit.radius", v))
            .build();
        widgets.add(radiusSlider);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        speedSlider = LabeledSlider.builder("Speed")
            .position(sliderX, currentY).width(w)
            .range(-5f, 5f).initial(orbit != null ? orbit.speed() : 0.5f).format("%.2f rot/s")
            .onChange(v -> state.set("transform.orbit.speed", v))
            .build();
        widgets.add(speedSlider);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        axisDropdown = GuiWidgets.enumDropdown(
            sliderX, currentY, w, "Axis", OrbitAxis.class, OrbitAxis.Y,
            "Orbit rotation axis",
            v -> state.set("transform.orbit.axis", v.name())
        );
        try {
            if (orbit != null) {
                axisDropdown.setValue(OrbitAxis.valueOf(orbit.axis().name()));
            }
        } catch (IllegalArgumentException ignored) {}
        widgets.add(axisDropdown);
        currentY += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        phaseSlider = LabeledSlider.builder("Phase")
            .position(sliderX, currentY).width(w)
            .range(0f, 360f).initial(orbit != null ? orbit.phase() : 0f).format("%.0f°")
            .onChange(v -> state.set("transform.orbit.phase", v))
            .build();
        widgets.add(phaseSlider);
    }

    @Override
    public void tick() {
        // No tick needed
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        
        // Section header
        context.drawText(textRenderer, "§6§lOrbit", GuiConstants.PADDING + 4, startY, GuiConstants.TEXT_PRIMARY, false);
        
        // Render all widgets
        for (var widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
    }

    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        return widgets;
    }

    public int getContentHeight() {
        return 14 + 5 * (GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING);
    }
}

