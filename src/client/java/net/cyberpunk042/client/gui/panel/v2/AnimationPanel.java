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
 * V2 Animation panel using new LayoutPanel architecture.
 * Controls spin, pulse, and alpha pulse animations.
 */
public class AnimationPanel extends LayoutPanel {
    
    private CyclingButtonWidget<Boolean> spinToggle;
    private LabeledSlider spinSpeedSlider;
    private CyclingButtonWidget<Boolean> pulseToggle;
    private LabeledSlider pulseSpeedSlider;
    private LabeledSlider pulseScaleSlider;
    private CyclingButtonWidget<Boolean> alphaPulseToggle;
    
    public AnimationPanel(Screen parent, FieldEditState state, TextRenderer textRenderer) {
        super(parent, state, textRenderer);
    }
    
    @Override
    protected void buildContent(DirectionalLayoutWidget layout) {
        int w = contentWidth();
        int h = 20;
        
        // Spin toggle
        boolean spinActive = state.spin() != null && state.spin().isActive();
        spinToggle = CyclingButtonWidget.onOffBuilder()
            .initially(spinActive)
            .build(0, 0, w, h, Text.literal("Spin"), (btn, val) -> {
                state.set("spin.active", val);
            });
        layout.add(spinToggle, p -> p.marginTop(4).marginLeft(4));
        
        // Spin speed
        float spinSpeed = state.spin() != null ? state.spin().speed() : 1.0f;
        spinSpeedSlider = new LabeledSlider(0, 0, w, "Spin Speed",
            -10.0f, 10.0f, spinSpeed,
            "%.1f°/s", null,
            v -> state.set("spin.speed", v)
        );
        layout.add(spinSpeedSlider, p -> p.marginTop(4).marginLeft(4));
        
        // Pulse toggle
        boolean pulseActive = state.pulse() != null && state.pulse().isActive();
        pulseToggle = CyclingButtonWidget.onOffBuilder()
            .initially(pulseActive)
            .build(0, 0, w, h, Text.literal("Pulse"), (btn, val) -> {
                state.set("pulse.active", val);
            });
        layout.add(pulseToggle, p -> p.marginTop(4).marginLeft(4));
        
        // Pulse speed
        float pulseSpeed = state.pulse() != null ? state.pulse().speed() : 1.0f;
        pulseSpeedSlider = new LabeledSlider(0, 0, w, "Pulse Speed",
            0.1f, 5.0f, pulseSpeed,
            "%.1f Hz", null,
            v -> state.set("pulse.speed", v)
        );
        layout.add(pulseSpeedSlider, p -> p.marginTop(4).marginLeft(4));
        
        // Pulse scale
        float pulseScale = state.pulse() != null ? state.pulse().scale() : 0.2f;
        pulseScaleSlider = new LabeledSlider(0, 0, w, "Pulse Scale",
            0.0f, 1.0f, pulseScale,
            "%.2f", null,
            v -> state.set("pulse.scale", v)
        );
        layout.add(pulseScaleSlider, p -> p.marginTop(4).marginLeft(4));
        
        // Alpha pulse toggle
        boolean alphaActive = state.alphaPulse() != null && state.alphaPulse().isActive();
        alphaPulseToggle = CyclingButtonWidget.onOffBuilder()
            .initially(alphaActive)
            .build(0, 0, w, h, Text.literal("Alpha Fade"), (btn, val) -> {
                state.set("alphaPulse.active", val);
            });
        layout.add(alphaPulseToggle, p -> p.marginTop(4).marginLeft(4));
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTextWithShadow(textRenderer, "Animation", bounds.x() + 4, bounds.y() + 4, 0xFF88AACC);
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void tick() {
        // Sync from state if needed
    }
}
