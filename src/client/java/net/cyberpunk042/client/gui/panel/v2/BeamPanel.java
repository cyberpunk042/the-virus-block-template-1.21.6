package net.cyberpunk042.client.gui.panel.v2;

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
 * V2 Beam panel using new LayoutPanel architecture.
 * Controls central beam effect settings.
 */
public class BeamPanel extends LayoutPanel {
    
    private CyclingButtonWidget<Boolean> enabledToggle;
    private LabeledSlider innerRadiusSlider;
    private LabeledSlider outerRadiusSlider;
    private LabeledSlider heightSlider;
    private LabeledSlider glowSlider;
    
    public BeamPanel(Screen parent, FieldEditState state, TextRenderer textRenderer) {
        super(parent, state, textRenderer);
    }
    
    @Override
    protected void buildContent(DirectionalLayoutWidget layout) {
        int w = contentWidth();
        int h = 20;
        
        // Enabled toggle
        boolean beamEnabled = state.beam() != null && state.beam().enabled();
        enabledToggle = CyclingButtonWidget.onOffBuilder()
            .initially(beamEnabled)
            .build(0, 0, w, h, Text.literal("Beam Enabled"), (btn, val) -> {
                state.set("beam.enabled", val);
            });
        layout.add(enabledToggle, p -> p.marginTop(4).marginLeft(4));
        
        // Inner radius
        float innerRadius = state.beam() != null ? state.beam().innerRadius() : 0.1f;
        innerRadiusSlider = new LabeledSlider(0, 0, w, "Inner Radius",
            0.0f, 2.0f, innerRadius,
            "%.2f", null,
            v -> state.set("beam.innerRadius", v)
        );
        layout.add(innerRadiusSlider, p -> p.marginTop(4).marginLeft(4));
        
        // Outer radius
        float outerRadius = state.beam() != null ? state.beam().outerRadius() : 0.5f;
        outerRadiusSlider = new LabeledSlider(0, 0, w, "Outer Radius",
            0.0f, 5.0f, outerRadius,
            "%.2f", null,
            v -> state.set("beam.outerRadius", v)
        );
        layout.add(outerRadiusSlider, p -> p.marginTop(4).marginLeft(4));
        
        // Height
        float beamHeight = state.beam() != null ? state.beam().height() : 10.0f;
        heightSlider = new LabeledSlider(0, 0, w, "Height",
            1.0f, 256.0f, beamHeight,
            "%.0f", null,
            v -> state.set("beam.height", v)
        );
        layout.add(heightSlider, p -> p.marginTop(4).marginLeft(4));
        
        // Glow
        float beamGlow = state.beam() != null ? state.beam().glow() : 1.0f;
        glowSlider = new LabeledSlider(0, 0, w, "Glow",
            0.0f, 2.0f, beamGlow,
            "%.2f", null,
            v -> state.set("beam.glow", v)
        );
        layout.add(glowSlider, p -> p.marginTop(4).marginLeft(4));
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTextWithShadow(textRenderer, "Beam", bounds.x() + 4, bounds.y() + 4, 0xFF88AACC);
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void tick() {
        // Sync from state if needed
    }
}
