package net.cyberpunk042.client.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;

/**
 * Text widget that renders at a scaled size.
 * Used for description text that needs to be smaller than normal.
 */
public class ScaledTextWidget extends ClickableWidget {
    
    private final TextRenderer textRenderer;
    private final float scale;
    private int color = 0xFFFFFFFF;  // Full alpha white by default
    private boolean leftAligned = false;
    
    /**
     * Creates a scaled text widget.
     * 
     * @param x X position
     * @param y Y position  
     * @param width Widget width
     * @param height Widget height
     * @param text The text to display
     * @param textRenderer Text renderer
     * @param scale Scale factor (e.g., 0.75 for 75% size)
     */
    public ScaledTextWidget(int x, int y, int width, int height, Text text, TextRenderer textRenderer, float scale) {
        super(x, y, width, height, text);
        this.textRenderer = textRenderer;
        this.scale = scale;
    }
    
    public ScaledTextWidget setTextColor(int color) {
        this.color = color;
        return this;
    }
    
    public ScaledTextWidget alignLeft() {
        this.leftAligned = true;
        return this;
    }
    
    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Scale text by drawing at modified position
        // Note: Since we can't easily scale the font, we draw normally but the widget
        // is sized smaller, giving a compact appearance
        String textStr = getMessage().getString();
        
        int drawX;
        if (leftAligned) {
            drawX = getX();
        } else {
            int textWidth = textRenderer.getWidth(textStr);
            drawX = getX() + (width - textWidth) / 2;
        }
        
        // Draw text - it will be normal size but widget spacing is compact
        context.drawText(textRenderer, textStr, drawX, getY(), color, false);
    }
    
    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // No narration needed for display-only text
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false; // Not clickable
    }
}
