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
 * V2 Visibility panel using new LayoutPanel architecture.
 * Controls mask visibility settings.
 */
public class VisibilityPanel extends LayoutPanel {
    
    private CyclingButtonWidget<Boolean> invertToggle;
    private CyclingButtonWidget<Boolean> animateToggle;
    private LabeledSlider offsetSlider;
    private LabeledSlider featherSlider;
    
    public VisibilityPanel(Screen parent, FieldEditState state, TextRenderer textRenderer) {
        super(parent, state, textRenderer);
    }
    
    @Override
    protected void buildContent(DirectionalLayoutWidget layout) {
        int w = contentWidth();
        int h = 20;
        
        // Invert mask
        invertToggle = CyclingButtonWidget.onOffBuilder()
            .initially(state.getBool("mask.invert"))
            .build(0, 0, w, h, Text.literal("Invert"), (btn, val) -> {
                state.set("mask.invert", val);
            });
        layout.add(invertToggle, p -> p.marginTop(4).marginLeft(4));

        // Animate mask
        animateToggle = CyclingButtonWidget.onOffBuilder()
            .initially(state.getBool("mask.animate"))
            .build(0, 0, w, h, Text.literal("Animate"), (btn, val) -> {
                state.set("mask.animate", val);
            });
        layout.add(animateToggle, p -> p.marginTop(4).marginLeft(4));
        
        // Offset slider
        offsetSlider = new LabeledSlider(0, 0, w, "Offset",
            -1.0f, 1.0f, state.getFloat("mask.offset"),
            "%+.2f", null,
            v -> state.set("mask.offset", v)
        );
        layout.add(offsetSlider, p -> p.marginTop(4).marginLeft(4));
        
        // Feather slider
        featherSlider = new LabeledSlider(0, 0, w, "Feather",
            0.0f, 1.0f, state.getFloat("mask.feather"),
            "%.2f", null,
            v -> state.set("mask.feather", v)
        );
        layout.add(featherSlider, p -> p.marginTop(4).marginLeft(4));
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTextWithShadow(textRenderer, "Visibility", bounds.x() + 4, bounds.y() + 4, 0xFF88AACC);
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void tick() {
        // Sync from state if needed
    }
}
