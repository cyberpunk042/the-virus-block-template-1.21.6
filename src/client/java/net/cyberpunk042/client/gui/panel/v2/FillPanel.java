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
 * V2 Fill Options using new LayoutPanel architecture.
 * Migrated from: FillSubPanel
 */
public class FillPanel extends LayoutPanel {
    
    private CyclingButtonWidget<Boolean> doubleSidedToggle;
    private CyclingButtonWidget<Boolean> depthTestToggle;
    
    public FillPanel(Screen parent, FieldEditState state, TextRenderer textRenderer) {
        super(parent, state, textRenderer);
    }
    
    @Override
    protected void buildContent(DirectionalLayoutWidget layout) {
        int w = contentWidth();
        int h = 20;
        
        // Double Sided
        doubleSidedToggle = CyclingButtonWidget.onOffBuilder()
            .initially(state.getBool("fill().doubleSided()"))
            .build(0, 0, w, h, Text.literal("Double Sided"), (btn, val) -> {
                state.set("fill().doubleSided()", val);
            });
        layout.add(doubleSidedToggle, p -> p.marginTop(4).marginLeft(4));

        // Depth Test
        depthTestToggle = CyclingButtonWidget.onOffBuilder()
            .initially(state.getBool("fill().depthTest()"))
            .build(0, 0, w, h, Text.literal("Depth Test"), (btn, val) -> {
                state.set("fill().depthTest()", val);
            });
        layout.add(depthTestToggle, p -> p.marginTop(4).marginLeft(4));
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw section header
        context.drawTextWithShadow(textRenderer, "Fill Options", bounds.x() + 4, bounds.y() + 4, 0xFF88AACC);
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void tick() {
        // Sync from state if needed
    }
}
