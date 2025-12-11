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
 * V2 ModifiersPanel using new LayoutPanel architecture.
 * Migrated from: ModifiersSubPanel
 */
public class ModifiersPanel extends LayoutPanel {
    
    private LabeledSlider bobbingSlider;
    private LabeledSlider breathingSlider;
    
    public ModifiersPanel(Screen parent, FieldEditState state, TextRenderer textRenderer) {
        super(parent, state, textRenderer);
    }
    
    @Override
    protected void buildContent(DirectionalLayoutWidget layout) {
        int w = contentWidth();
        int h = 20;
        
        // Bobbing
        bobbingSlider = new LabeledSlider(0, 0, w, "Bobbing",
            0.0f, 1.0f, state.getFloat("modifiers.bobbing"),
            "%.2f", null,
            v -> state.set("modifiers.bobbing", v)
        );
        layout.add(bobbingSlider, p -> p.marginTop(4).marginLeft(4));

        // Breathing
        breathingSlider = new LabeledSlider(0, 0, w, "Breathing",
            0.0f, 1.0f, state.getFloat("modifiers.breathing"),
            "%.2f", null,
            v -> state.set("modifiers.breathing", v)
        );
        layout.add(breathingSlider, p -> p.marginTop(4).marginLeft(4));
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw section header
        context.drawTextWithShadow(textRenderer, "ModifiersPanel", bounds.x() + 4, bounds.y() + 4, 0xFF88AACC);
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void tick() {
        // Sync from state if needed
    }
}
