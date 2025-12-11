package net.cyberpunk042.client.gui.panel.v2;

import net.cyberpunk042.client.gui.layout.LayoutPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.text.Text;
import org.joml.Vector3f;

/**
 * V2 Transform panel using new LayoutPanel architecture.
 * Controls scale, offset, and rotation.
 */
public class TransformPanel extends LayoutPanel {
    
    private LabeledSlider scaleSlider;
    private LabeledSlider offsetXSlider;
    private LabeledSlider offsetYSlider;
    private LabeledSlider offsetZSlider;
    private LabeledSlider rotXSlider;
    private LabeledSlider rotYSlider;
    private LabeledSlider rotZSlider;
    
    public TransformPanel(Screen parent, FieldEditState state, TextRenderer textRenderer) {
        super(parent, state, textRenderer);
    }
    
    @Override
    protected void buildContent(DirectionalLayoutWidget layout) {
        int w = contentWidth();
        int h = 20;
        
        // Scale slider
        scaleSlider = new LabeledSlider(0, 0, w, "Scale",
            0.1f, 5.0f, state.getFloat("transform.scale"),
            "%.2f", null,
            v -> state.set("transform.scale", v)
        );
        layout.add(scaleSlider, p -> p.marginTop(4).marginLeft(4));
        
        // Offset sliders
        Vector3f offset = state.transform() != null ? state.transform().offset() : null;
        float ox = offset != null ? offset.x : 0;
        float oy = offset != null ? offset.y : 0;
        float oz = offset != null ? offset.z : 0;
        
        offsetXSlider = new LabeledSlider(0, 0, w, "Offset X",
            -10f, 10f, ox, "%.1f", null,
            v -> state.set("transform.offset.x", v)
        );
        layout.add(offsetXSlider, p -> p.marginTop(4).marginLeft(4));
        
        offsetYSlider = new LabeledSlider(0, 0, w, "Offset Y",
            -10f, 10f, oy, "%.1f", null,
            v -> state.set("transform.offset.y", v)
        );
        layout.add(offsetYSlider, p -> p.marginTop(4).marginLeft(4));
        
        offsetZSlider = new LabeledSlider(0, 0, w, "Offset Z",
            -10f, 10f, oz, "%.1f", null,
            v -> state.set("transform.offset.z", v)
        );
        layout.add(offsetZSlider, p -> p.marginTop(4).marginLeft(4));
        
        // Rotation sliders
        Vector3f rot = state.transform() != null ? state.transform().rotation() : null;
        float rx = rot != null ? rot.x : 0;
        float ry = rot != null ? rot.y : 0;
        float rz = rot != null ? rot.z : 0;
        
        rotXSlider = new LabeledSlider(0, 0, w, "Rotation X",
            -180f, 180f, rx, "%.0f°", null,
            v -> state.set("transform.rotation.x", v)
        );
        layout.add(rotXSlider, p -> p.marginTop(4).marginLeft(4));
        
        rotYSlider = new LabeledSlider(0, 0, w, "Rotation Y",
            -180f, 180f, ry, "%.0f°", null,
            v -> state.set("transform.rotation.y", v)
        );
        layout.add(rotYSlider, p -> p.marginTop(4).marginLeft(4));
        
        rotZSlider = new LabeledSlider(0, 0, w, "Rotation Z",
            -180f, 180f, rz, "%.0f°", null,
            v -> state.set("transform.rotation.z", v)
        );
        layout.add(rotZSlider, p -> p.marginTop(4).marginLeft(4));
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTextWithShadow(textRenderer, "Transform", bounds.x() + 4, bounds.y() + 4, 0xFF88AACC);
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void tick() {
        // Sync from state if needed
    }
}
