package net.cyberpunk042.client.gui.panel.v2;

import net.cyberpunk042.client.gui.layout.LayoutPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.widget.ColorButton;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.text.Text;

/**
 * V2 Appearance panel using new LayoutPanel architecture.
 * 
 * Controls for visual appearance:
 * - Glow intensity (0.0 - 2.0)
 * - Emissive intensity (0.0 - 1.0)
 * - Saturation modifier (-1.0 to 1.0)
 * - Primary/Secondary colors
 */
public class AppearancePanel extends LayoutPanel {
    
    private LabeledSlider glowSlider;
    private LabeledSlider emissiveSlider;
    private LabeledSlider saturationSlider;
    private ColorButton primaryColorBtn;
    private ColorButton secondaryColorBtn;
    
    public AppearancePanel(Screen parent, FieldEditState state, TextRenderer textRenderer) {
        super(parent, state, textRenderer);
    }
    
    @Override
    protected void buildContent(DirectionalLayoutWidget layout) {
        int w = contentWidth();
        int h = 20;
        
        // Glow intensity
        glowSlider = new LabeledSlider(0, 0, w, "Glow",
            0.0f, 2.0f, state.getFloat("appearance.glow"),
            "%.2f", null,
            v -> state.set("appearance.glow", v)
        );
        layout.add(glowSlider, p -> p.marginTop(4).marginLeft(4));

        // Emissive intensity
        emissiveSlider = new LabeledSlider(0, 0, w, "Emissive",
            0.0f, 1.0f, state.getFloat("appearance.emissive"),
            "%.2f", null,
            v -> state.set("appearance.emissive", v)
        );
        layout.add(emissiveSlider, p -> p.marginTop(4).marginLeft(4));

        // Saturation modifier
        saturationSlider = new LabeledSlider(0, 0, w, "Saturation",
            -1.0f, 1.0f, state.getFloat("appearance.saturation"),
            "%+.2f", null,
            v -> state.set("appearance.saturation", v)
        );
        layout.add(saturationSlider, p -> p.marginTop(4).marginLeft(4));

        // Color buttons in a horizontal row
        DirectionalLayoutWidget colorRow = row();
        int colorBtnW = (w - 8) / 2;
        
        primaryColorBtn = new ColorButton(0, 0, colorBtnW, "Primary", 
            state.getInt("appearance.primaryColor"),
            color -> state.set("appearance.primaryColor", color));
        colorRow.add(primaryColorBtn);
        
        secondaryColorBtn = new ColorButton(0, 0, colorBtnW, "Secondary",
            state.getInt("appearance.secondaryColor"),
            color -> state.set("appearance.secondaryColor", color));
        colorRow.add(secondaryColorBtn);
        
        layout.add(colorRow, p -> p.marginTop(8).marginLeft(4));
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTextWithShadow(textRenderer, "Appearance", bounds.x() + 4, bounds.y() + 4, 0xFF88AACC);
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void tick() {
        // Sync from state if needed
    }
}
