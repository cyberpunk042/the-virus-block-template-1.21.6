package net.cyberpunk042.client.gui.panel;

import net.cyberpunk042.client.gui.state.GuiState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

/**
 * G06: Base class for GUI panels.
 */
public abstract class AbstractPanel {
    
    protected final Screen parent;
    protected final GuiState state;
    protected int panelWidth;
    protected int panelHeight;
    protected int scrollOffset = 0;
    
    protected AbstractPanel(Screen parent, GuiState state) {
        this.parent = parent;
        this.state = state;
    }
    
    /** Sets the scroll offset for this panel (used by scrollable containers). */
    public void setScrollOffset(int offset) {
        this.scrollOffset = offset;
    }
    
    public abstract void init(int width, int height);
    public abstract void tick();
    public abstract void render(DrawContext context, int mouseX, int mouseY, float delta);
}
