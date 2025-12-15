package net.cyberpunk042.client.gui.panel.v2;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.layout.LayoutPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.text.Text;

/**
 * V2 OrbitPanel using new LayoutPanel architecture.
 * Migrated from: OrbitSubPanel
 */
public class OrbitPanel extends LayoutPanel {
    
    private LabeledSlider radiusSlider;
    private LabeledSlider speedSlider;
    private LabeledSlider phaseSlider;
    
    public OrbitPanel(Screen parent, FieldEditState state, TextRenderer textRenderer) {
        super(parent, state, textRenderer);
    }
    
    @Override
    protected void buildContent(DirectionalLayoutWidget layout) {
        int w = contentWidth();
        int h = 20;
        
        // Get orbit from transform
        var orbit = state.transform() != null ? state.transform().orbit() : null;
        
        // Radius
        radiusSlider = new LabeledSlider(0, 0, w, "Radius",
            0.1f, 20.0f, orbit != null ? orbit.radius() : 2.0f,
            "%.1f", null,
            v -> state.set("transform.orbit.radius", v)
        );
        layout.add(radiusSlider, p -> p.marginTop(4).marginLeft(4));

        // Speed
        speedSlider = new LabeledSlider(0, 0, w, "Speed",
            -5.0f, 5.0f, orbit != null ? orbit.speed() : 0.5f,
            "%.2f rot/s", null,
            v -> state.set("transform.orbit.speed", v)
        );
        layout.add(speedSlider, p -> p.marginTop(4).marginLeft(4));

        // Phase
        phaseSlider = new LabeledSlider(0, 0, w, "Phase",
            0.0f, 360.0f, orbit != null ? orbit.phase() : 0f,
            "%.0f°", null,
            v -> state.set("transform.orbit.phase", v)
        );
        layout.add(phaseSlider, p -> p.marginTop(4).marginLeft(4));
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw section header
        context.drawTextWithShadow(textRenderer, "OrbitPanel", bounds.x() + 4, bounds.y() + 4, 0xFF88AACC);
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void tick() {
        // Sync from state if needed
    }
}
