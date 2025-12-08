package net.cyberpunk042.client.gui.panel;

import net.cyberpunk042.client.gui.panel.sub.*;
import net.cyberpunk042.client.gui.state.GuiState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

/**
 * AdvancedPanel (Level 2) - Full customization controls.
 * 
 * <p>Contains all expandable sub-panels for detailed editing:</p>
 * <ul>
 *   <li>Shape - per-shape parameters</li>
 *   <li>Appearance - colors, glow, emissive</li>
 *   <li>Animation - spin, pulse, alpha</li>
 *   <li>Transform - anchor, offset, rotation, scale</li>
 *   <li>Visibility - masks, patterns</li>
 *   <li>Arrangement - cell patterns</li>
 *   <li>Fill - wireframe, cage settings</li>
 * </ul>
 */
public class AdvancedPanel extends AbstractPanel {
    
    // All sub-panels
    private ShapeSubPanel shapeSubPanel;
    private AppearanceSubPanel appearanceSubPanel;
    private AnimationSubPanel animationSubPanel;
    private TransformSubPanel transformSubPanel;
    private VisibilitySubPanel visibilitySubPanel;
    private ArrangementSubPanel arrangementSubPanel;
    private FillSubPanel fillSubPanel;
    
    // Scroll state
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private static final int SCROLL_SPEED = 10;
    
    public AdvancedPanel(Screen parent, GuiState state) {
        super(parent, state);
        Logging.GUI.topic("panel").debug("AdvancedPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        
        int contentY = GuiConstants.TAB_HEIGHT + GuiConstants.PADDING;
        
        // Shape parameters (dynamic based on shape type)
        shapeSubPanel = new ShapeSubPanel(parent, state, contentY);
        shapeSubPanel.init(width, height);
        contentY += shapeSubPanel.getHeight() + GuiConstants.SECTION_SPACING;
        
        // Appearance (colors, glow, emissive)
        appearanceSubPanel = new AppearanceSubPanel(parent, state, contentY);
        appearanceSubPanel.init(width, height);
        contentY += appearanceSubPanel.getHeight() + GuiConstants.SECTION_SPACING;
        
        // Animation (spin, pulse, alpha)
        animationSubPanel = new AnimationSubPanel(parent, state, contentY);
        animationSubPanel.init(width, height);
        contentY += animationSubPanel.getHeight() + GuiConstants.SECTION_SPACING;
        
        // Transform (anchor, offset, rotation, scale)
        transformSubPanel = new TransformSubPanel(parent, state, contentY);
        transformSubPanel.init(width, height);
        contentY += transformSubPanel.getHeight() + GuiConstants.SECTION_SPACING;
        
        // Visibility (masks)
        visibilitySubPanel = new VisibilitySubPanel(parent, state, contentY);
        visibilitySubPanel.init(width, height);
        contentY += visibilitySubPanel.getHeight() + GuiConstants.SECTION_SPACING;
        
        // Arrangement (patterns)
        arrangementSubPanel = new ArrangementSubPanel(parent, state, contentY);
        arrangementSubPanel.init(width, height);
        contentY += arrangementSubPanel.getHeight() + GuiConstants.SECTION_SPACING;
        
        // Fill (wireframe, cage)
        fillSubPanel = new FillSubPanel(parent, state, contentY);
        fillSubPanel.init(width, height);
        contentY += fillSubPanel.getHeight() + GuiConstants.SECTION_SPACING;
        
        contentHeight = contentY;
        
        Logging.GUI.topic("panel").debug("AdvancedPanel initialized with 7 sub-panels, height: {}", contentHeight);
    }
    
    @Override
    public void tick() {
        if (shapeSubPanel != null) shapeSubPanel.tick();
        if (appearanceSubPanel != null) appearanceSubPanel.tick();
        if (animationSubPanel != null) animationSubPanel.tick();
        if (transformSubPanel != null) transformSubPanel.tick();
        if (visibilitySubPanel != null) visibilitySubPanel.tick();
        if (arrangementSubPanel != null) arrangementSubPanel.tick();
        if (fillSubPanel != null) fillSubPanel.tick();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelY = GuiConstants.TAB_HEIGHT;
        context.fill(0, panelY, panelWidth, panelHeight, GuiConstants.BG_PANEL);
        
        // Scissor for scroll
        context.enableScissor(0, panelY, panelWidth, panelHeight);
        
        // Render all sub-panels
        renderSubPanel(shapeSubPanel, context, mouseX, mouseY, delta);
        renderSubPanel(appearanceSubPanel, context, mouseX, mouseY, delta);
        renderSubPanel(animationSubPanel, context, mouseX, mouseY, delta);
        renderSubPanel(transformSubPanel, context, mouseX, mouseY, delta);
        renderSubPanel(visibilitySubPanel, context, mouseX, mouseY, delta);
        renderSubPanel(arrangementSubPanel, context, mouseX, mouseY, delta);
        renderSubPanel(fillSubPanel, context, mouseX, mouseY, delta);
        
        context.disableScissor();
        
        // Scroll indicator
        if (contentHeight > panelHeight - panelY) {
            renderScrollIndicator(context);
        }
    }
    
    private void renderSubPanel(AbstractPanel panel, DrawContext context, int mouseX, int mouseY, float delta) {
        if (panel != null) {
            panel.setScrollOffset(scrollOffset);
            panel.render(context, mouseX, mouseY + scrollOffset, delta);
        }
    }
    
    private void renderScrollIndicator(DrawContext context) {
        int barHeight = 4;
        int barY = GuiConstants.TAB_HEIGHT + 2;
        float scrollPercent = (float) scrollOffset / Math.max(1, contentHeight - panelHeight);
        int barWidth = (int) (panelWidth * 0.3f);
        int barX = (panelWidth - barWidth) / 2;
        
        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0x40FFFFFF);
        int indicatorWidth = Math.max(20, barWidth / 4);
        int indicatorX = barX + (int) ((barWidth - indicatorWidth) * scrollPercent);
        context.fill(indicatorX, barY, indicatorX + indicatorWidth, barY + barHeight, GuiConstants.ACCENT);
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        int maxScroll = Math.max(0, contentHeight - (panelHeight - GuiConstants.TAB_HEIGHT));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(vAmount * SCROLL_SPEED)));
        return true;
    }
    
    public void onShapeTypeChanged() {
        if (shapeSubPanel != null) {
            shapeSubPanel.rebuildForCurrentShape();
        }
    }
}
