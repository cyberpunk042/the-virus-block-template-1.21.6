package net.cyberpunk042.client.gui.util;

/**
 * G15: Layout positioning helper for GUI panels.
 */
public class GuiLayout {
    
    private int startX;
    private int startY;
    private int rowHeight;
    private int currentX;
    private int currentY;
    
    public GuiLayout(int startX, int startY, int rowHeight) {
        this.startX = startX;
        this.startY = startY;
        this.rowHeight = rowHeight;
        this.currentX = startX;
        this.currentY = startY;
    }
    
    public GuiLayout(int startX, int startY) {
        this(startX, startY, GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING);
    }
    
    public void reset() {
        currentX = startX;
        currentY = startY;
    }
    
    public void reset(int newStartX, int newStartY) {
        this.startX = newStartX;
        this.startY = newStartY;
        reset();
    }
    
    public int getX() { return currentX; }
    public int getY() { return currentY; }
    public int getCurrentY() { return currentY; }
    
    public int nextRow() {
        int y = currentY;
        currentY += rowHeight;
        return y;
    }
    
    public int nextSection() {
        currentY += GuiConstants.SECTION_SPACING;
        return currentY;
    }
    
    public void setRowHeight(int height) {
        this.rowHeight = height;
    }
}
