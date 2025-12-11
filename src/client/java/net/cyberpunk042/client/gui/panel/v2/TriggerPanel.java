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
 * V2 Trigger panel using new LayoutPanel architecture.
 * Controls trigger zones and effects.
 */
public class TriggerPanel extends LayoutPanel {
    
    private CyclingButtonWidget<Boolean> enabledToggle;
    private LabeledSlider innerRadiusSlider;
    private LabeledSlider outerRadiusSlider;
    
    public TriggerPanel(Screen parent, FieldEditState state, TextRenderer textRenderer) {
        super(parent, state, textRenderer);
    }
    
    @Override
    protected void buildContent(DirectionalLayoutWidget layout) {
        int w = contentWidth();
        int h = 20;
        
        // Enabled toggle
        enabledToggle = CyclingButtonWidget.onOffBuilder()
            .initially(state.getBool("trigger.enabled"))
            .build(0, 0, w, h, Text.literal("Enabled"), (btn, val) -> {
                state.set("trigger.enabled", val);
            });
        layout.add(enabledToggle, p -> p.marginTop(4).marginLeft(4));
        
        // Inner radius
        innerRadiusSlider = new LabeledSlider(0, 0, w, "Inner Radius",
            0.0f, 10.0f, state.getFloat("trigger.innerRadius"),
            "%.1f", null,
            v -> state.set("trigger.innerRadius", v)
        );
        layout.add(innerRadiusSlider, p -> p.marginTop(4).marginLeft(4));
        
        // Outer radius
        outerRadiusSlider = new LabeledSlider(0, 0, w, "Outer Radius",
            0.0f, 20.0f, state.getFloat("trigger.outerRadius"),
            "%.1f", null,
            v -> state.set("trigger.outerRadius", v)
        );
        layout.add(outerRadiusSlider, p -> p.marginTop(4).marginLeft(4));
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTextWithShadow(textRenderer, "Trigger", bounds.x() + 4, bounds.y() + 4, 0xFF88AACC);
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void tick() {
        // Sync from state if needed
    }
}
