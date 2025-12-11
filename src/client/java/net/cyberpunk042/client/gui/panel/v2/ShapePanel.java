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

import java.util.List;

/**
 * V2 Shape panel using new LayoutPanel architecture.
 * Controls shape type and parameters.
 */
public class ShapePanel extends LayoutPanel {
    
    private static final List<String> SHAPE_TYPES = List.of(
        "sphere", "ring", "disc", "prism", "cylinder", "polyhedron"
    );
    
    private CyclingButtonWidget<String> shapeTypeButton;
    private LabeledSlider radiusSlider;
    private LabeledSlider segmentsSlider;
    
    public ShapePanel(Screen parent, FieldEditState state, TextRenderer textRenderer) {
        super(parent, state, textRenderer);
    }
    
    @Override
    protected void buildContent(DirectionalLayoutWidget layout) {
        int w = contentWidth();
        int h = 20;
        
        // Shape type dropdown
        String currentShape = state.shapeType != null ? state.shapeType : "sphere";
        shapeTypeButton = CyclingButtonWidget.<String>builder(Text::literal)
            .values(SHAPE_TYPES)
            .initially(currentShape)
            .build(0, 0, w, h, Text.literal("Shape"), (btn, val) -> {
                state.set("shapeType", val);
            });
        layout.add(shapeTypeButton, p -> p.marginTop(4).marginLeft(4));
        
        // Radius slider
        radiusSlider = new LabeledSlider(
            0, 0, w, "Radius",
            0.5f, 20.0f, state.getFloat("radius"),
            "%.1f", null,
            v -> state.set("radius", v)
        );
        layout.add(radiusSlider, p -> p.marginTop(4).marginLeft(4));
        
        // Segments slider
        segmentsSlider = new LabeledSlider(
            0, 0, w, "Segments",
            4.0f, 64.0f, state.getFloat("shape.segments"),
            "%.0f", null,
            v -> state.set("shape.segments", v)
        );
        layout.add(segmentsSlider, p -> p.marginTop(4).marginLeft(4));
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTextWithShadow(textRenderer, "Shape", bounds.x() + 4, bounds.y() + 4, 0xFF88AACC);
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void tick() {
        // Sync from state if needed
    }
}
