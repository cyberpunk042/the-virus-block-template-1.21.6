package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.widget.ColorButton;
import net.cyberpunk042.client.gui.widget.ExpandableSection;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * G71-G75: Appearance controls for the Advanced panel.
 * 
 * <p>Controls for visual appearance:</p>
 * <ul>
 *   <li>G71: Glow intensity slider (0.0 - 2.0)</li>
 *   <li>G72: Emissive intensity slider (0.0 - 1.0)</li>
 *   <li>G73: Saturation modifier (-1.0 to 1.0)</li>
 *   <li>G74: Primary color button with picker</li>
 *   <li>G75: Secondary color button (gradient end)</li>
 * </ul>
 */
public class AppearanceSubPanel extends AbstractPanel {
    
    private int startY;    
    // G71: Glow
    private LabeledSlider glowSlider;
    
    // G72: Emissive
    private LabeledSlider emissiveSlider;
    
    // G73: Saturation
    private LabeledSlider saturationSlider;
    
    // G74-G75: Colors
    private ColorButton primaryColorBtn;
    private ColorButton secondaryColorBtn;
    
    public AppearanceSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("AppearanceSubPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        
        int x = GuiConstants.PADDING;
        int y = startY + GuiConstants.PADDING;
        int w = width - GuiConstants.PADDING * 2;
        
        // G71: Glow intensity
        glowSlider = LabeledSlider.builder("Glow")
            .position(x, y).width(w)
            .range(0f, 2f).initial(state.getFloat("appearance.glow")).format("%.2f")
            .onChange(v -> {
                state.set("appearance.glow", v);
                Logging.GUI.topic("appearance").trace("Glow: {}", v);
            })
            .build();
        widgets.add(glowSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // G72: Emissive intensity
        emissiveSlider = LabeledSlider.builder("Emissive")
            .position(x, y).width(w)
            .range(0f, 1f).initial(state.getFloat("appearance.emissive")).format("%.2f")
            .onChange(v -> {
                state.set("appearance.emissive", v);
                Logging.GUI.topic("appearance").trace("Emissive: {}", v);
            })
            .build();
        widgets.add(emissiveSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // G73: Saturation modifier
        saturationSlider = LabeledSlider.builder("Saturation")
            .position(x, y).width(w)
            .range(-1f, 1f).initial(state.getFloat("appearance.saturation")).format("%+.2f")
            .onChange(v -> {
                state.set("appearance.saturation", v);
                Logging.GUI.topic("appearance").trace("Saturation: {}", v);
            })
            .build();
        widgets.add(saturationSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // G74: Primary color
        int colorBtnWidth = (w - GuiConstants.PADDING) / 2;
        primaryColorBtn = new ColorButton(x, y, colorBtnWidth, "Primary", state.getInt("appearance.primaryColor"), color -> {
            state.set("appearance.primaryColor", color);
            Logging.GUI.topic("appearance").debug("Primary color: #{}", Integer.toHexString(color));
        });
        widgets.add(primaryColorBtn);
        
        // G75: Secondary color
        secondaryColorBtn = new ColorButton(x + colorBtnWidth + GuiConstants.PADDING, y, colorBtnWidth, "Secondary", state.getInt("appearance.secondaryColor"), color -> {
            state.set("appearance.secondaryColor", color);
            Logging.GUI.topic("appearance").debug("Secondary color: #{}", Integer.toHexString(color));
        });
        widgets.add(secondaryColorBtn);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        contentHeight = y - startY + GuiConstants.PADDING;
        
        Logging.GUI.topic("panel").debug("AppearanceSubPanel initialized");
    }
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render widgets directly (no expandable section)
        for (var widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
    }
    
    public int getHeight() {
        return contentHeight;
    }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> all = new ArrayList<>();
        // No header button (direct content)
        all.addAll(widgets);
        return all;
    }
}
