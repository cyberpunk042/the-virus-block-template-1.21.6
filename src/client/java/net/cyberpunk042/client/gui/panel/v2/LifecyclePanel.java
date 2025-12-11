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
 * V2 Lifecycle panel using new LayoutPanel architecture.
 * Controls field spawn/despawn behavior.
 */
public class LifecyclePanel extends LayoutPanel {
    
    private LabeledSlider fadeInSlider;
    private LabeledSlider fadeOutSlider;
    private CyclingButtonWidget<Boolean> persistentToggle;
    
    public LifecyclePanel(Screen parent, FieldEditState state, TextRenderer textRenderer) {
        super(parent, state, textRenderer);
    }
    
    @Override
    protected void buildContent(DirectionalLayoutWidget layout) {
        int w = contentWidth();
        int h = 20;
        
        // Fade in duration
        fadeInSlider = new LabeledSlider(0, 0, w, "Fade In",
            0.0f, 5.0f, state.getFloat("lifecycle.fadeIn"),
            "%.1fs", null,
            v -> state.set("lifecycle.fadeIn", v)
        );
        layout.add(fadeInSlider, p -> p.marginTop(4).marginLeft(4));
        
        // Fade out duration
        fadeOutSlider = new LabeledSlider(0, 0, w, "Fade Out",
            0.0f, 5.0f, state.getFloat("lifecycle.fadeOut"),
            "%.1fs", null,
            v -> state.set("lifecycle.fadeOut", v)
        );
        layout.add(fadeOutSlider, p -> p.marginTop(4).marginLeft(4));
        
        // Persistent toggle
        persistentToggle = CyclingButtonWidget.onOffBuilder()
            .initially(state.getBool("lifecycle.persistent"))
            .build(0, 0, w, h, Text.literal("Persistent"), (btn, val) -> {
                state.set("lifecycle.persistent", val);
            });
        layout.add(persistentToggle, p -> p.marginTop(4).marginLeft(4));
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTextWithShadow(textRenderer, "Lifecycle", bounds.x() + 4, bounds.y() + 4, 0xFF88AACC);
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void tick() {
        // Sync from state if needed
    }
}
