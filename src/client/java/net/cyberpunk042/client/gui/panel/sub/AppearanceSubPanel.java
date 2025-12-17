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
 * G71-G80: Appearance controls for the Advanced panel.
 * 
 * <p>Controls for visual appearance:</p>
 * <ul>
 *   <li>G71: Glow intensity slider (0.0 - 2.0)</li>
 *   <li>G72: Emissive intensity slider (0.0 - 1.0)</li>
 *   <li>G73: Saturation modifier (0.0 to 2.0)</li>
 *   <li>G74: Brightness modifier (0.0 to 2.0)</li>
 *   <li>G75: Hue shift slider (0 - 360 degrees)</li>
 *   <li>G76: Alpha min/max sliders</li>
 *   <li>G77: Primary color button with picker</li>
 *   <li>G78: Secondary color button (gradient end)</li>
 *   <li>G79: Color blend slider (0.0 - 1.0)</li>
 * </ul>
 */
public class AppearanceSubPanel extends AbstractPanel {
    
    private int startY;    
    // Glow and Emissive
    private LabeledSlider glowSlider;
    private LabeledSlider emissiveSlider;
    
    // Color adjustments
    private LabeledSlider saturationSlider;
    private LabeledSlider brightnessSlider;
    private LabeledSlider hueShiftSlider;
    
    // Alpha range
    private LabeledSlider alphaMinSlider;
    private LabeledSlider alphaMaxSlider;
    
    // Colors
    private ColorButton primaryColorBtn;
    private ColorButton secondaryColorBtn;
    private LabeledSlider colorBlendSlider;
    
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
        int halfW = (w - GuiConstants.COMPACT_GAP) / 2;
        int step = GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        
        // Row 1: Glow + Emissive
        glowSlider = LabeledSlider.builder("Glow")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT)
            .range(0f, 2f).initial(state.getFloat("appearance.glow")).format("%.2f")
            .onChange(v -> {
                state.set("appearance.glow", v);
                Logging.GUI.topic("appearance").trace("Glow: {}", v);
            })
            .build();
        widgets.add(glowSlider);
        
        emissiveSlider = LabeledSlider.builder("Emissive")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT)
            .range(0f, 1f).initial(state.getFloat("appearance.emissive")).format("%.2f")
            .onChange(v -> {
                state.set("appearance.emissive", v);
                Logging.GUI.topic("appearance").trace("Emissive: {}", v);
            })
            .build();
        widgets.add(emissiveSlider);
        y += step;
        
        // Row 2: Saturation + Brightness
        saturationSlider = LabeledSlider.builder("Sat")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT)
            .range(0f, 2f).initial(state.getFloat("appearance.saturation")).format("%.2f")
            .onChange(v -> {
                state.set("appearance.saturation", v);
                Logging.GUI.topic("appearance").trace("Saturation: {}", v);
            })
            .build();
        widgets.add(saturationSlider);
        
        brightnessSlider = LabeledSlider.builder("Bright")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT)
            .range(0f, 2f).initial(state.getFloat("appearance.brightness")).format("%.2f")
            .onChange(v -> {
                state.set("appearance.brightness", v);
                Logging.GUI.topic("appearance").trace("Brightness: {}", v);
            })
            .build();
        widgets.add(brightnessSlider);
        y += step;
        
        // Row 3: Hue Shift (full width)
        hueShiftSlider = LabeledSlider.builder("Hue Shift")
            .position(x, y).width(w).height(GuiConstants.COMPACT_HEIGHT)
            .range(0f, 360f).initial(state.getFloat("appearance.hueShift")).format("%.0f°")
            .onChange(v -> {
                state.set("appearance.hueShift", v);
                Logging.GUI.topic("appearance").trace("Hue Shift: {}", v);
            })
            .build();
        widgets.add(hueShiftSlider);
        y += step;
        
        // Row 4: Alpha Min + Alpha Max
        float alphaMin = state.getFloat("appearance.alphaMin");
        float alphaMax = state.getFloat("appearance.alphaMax");
        // Default to 1.0 if not set
        if (alphaMin == 0.0f && alphaMax == 0.0f) {
            alphaMin = 1.0f;
            alphaMax = 1.0f;
        }
        
        alphaMinSlider = LabeledSlider.builder("α Min")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT)
            .range(0f, 1f).initial(alphaMin).format("%.2f")
            .onChange(v -> {
                state.set("appearance.alphaMin", v);
                Logging.GUI.topic("appearance").trace("Alpha Min: {}", v);
            })
            .build();
        widgets.add(alphaMinSlider);
        
        alphaMaxSlider = LabeledSlider.builder("α Max")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT)
            .range(0f, 1f).initial(alphaMax).format("%.2f")
            .onChange(v -> {
                state.set("appearance.alphaMax", v);
                Logging.GUI.topic("appearance").trace("Alpha Max: {}", v);
            })
            .build();
        widgets.add(alphaMaxSlider);
        y += step;
        
        // Row 5: Primary + Secondary color buttons
        int colorBtnWidth = (w - GuiConstants.COMPACT_GAP) / 2;
        primaryColorBtn = new ColorButton(x, y, colorBtnWidth, "Primary", state.getInt("appearance.primaryColor"), color -> {
            state.set("appearance.primaryColor", color);
            Logging.GUI.topic("appearance").debug("Primary color: #{}", Integer.toHexString(color));
        });
        // Wire up right-click to show color input modal
        if (parent instanceof net.cyberpunk042.client.gui.screen.FieldCustomizerScreen fcs) {
            primaryColorBtn.setRightClickHandler(() -> 
                fcs.showColorInputModal(primaryColorBtn.getColorString(), colorString -> {
                    primaryColorBtn.setColorString(colorString);
                }));
        }
        widgets.add(primaryColorBtn);
        
        secondaryColorBtn = new ColorButton(x + colorBtnWidth + GuiConstants.COMPACT_GAP, y, colorBtnWidth, "Secondary", state.getInt("appearance.secondaryColor"), color -> {
            state.set("appearance.secondaryColor", color);
            Logging.GUI.topic("appearance").debug("Secondary color: #{}", Integer.toHexString(color));
        });
        // Wire up right-click to show color input modal
        if (parent instanceof net.cyberpunk042.client.gui.screen.FieldCustomizerScreen fcs) {
            secondaryColorBtn.setRightClickHandler(() -> 
                fcs.showColorInputModal(secondaryColorBtn.getColorString(), colorString -> {
                    secondaryColorBtn.setColorString(colorString);
                }));
        }
        widgets.add(secondaryColorBtn);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.COMPACT_GAP;
        
        // Row 6: Color Blend (full width)
        colorBlendSlider = LabeledSlider.builder("Color Blend")
            .position(x, y).width(w).height(GuiConstants.COMPACT_HEIGHT)
            .range(0f, 1f).initial(state.getFloat("appearance.colorBlend")).format("%.2f")
            .onChange(v -> {
                state.set("appearance.colorBlend", v);
                Logging.GUI.topic("appearance").trace("Color Blend: {}", v);
            })
            .build();
        widgets.add(colorBlendSlider);
        y += step;
        
        contentHeight = y - startY + GuiConstants.PADDING;
        
        Logging.GUI.topic("panel").debug("AppearanceSubPanel initialized with {} widgets", widgets.size());
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
