package net.cyberpunk042.client.gui.panel;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.RendererCapabilities;
import net.cyberpunk042.client.gui.state.RequiresFeature;
import net.cyberpunk042.client.gui.widget.ExpandableSection;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * G06: Base class for GUI panels.
 * 
 * <p>All panels render relative to their assigned bounds.
 * This enables the same panel code to work in both windowed and fullscreen modes.</p>
 * 
 * <h3>Template Method Pattern</h3>
 * <pre>
 * panel.setBounds(bounds);  // Set where to render
 * panel.init();             // Creates widgets at correct positions
 * panel.render(...);        // Renders within bounds
 * </pre>
 */
public abstract class AbstractPanel {
    
    protected final Screen parent;
    protected final FieldEditState state;
    
    /**
     * The bounds this panel should render within.
     * All widget positions should be relative to bounds.x, bounds.y
     */
    protected Bounds bounds = Bounds.EMPTY;
    
    /**
     * @deprecated Use bounds.width() instead. Kept for backward compatibility.
     */
    @Deprecated
    protected int panelWidth;
    
    /**
     * @deprecated Use bounds.height() instead. Kept for backward compatibility.
     */
    @Deprecated
    protected int panelHeight;
    
    /**
     * List of widgets owned by this panel.
     */
    protected final List<ClickableWidget> widgets = new ArrayList<>();
    
    /**
     * List of expandable sections owned by this panel.
     * These need to be offset along with widgets.
     */
    protected final List<ExpandableSection> sections = new ArrayList<>();
    
    /**
     * Scroll offset for panels with scrollable content.
     */
    protected int scrollOffset = 0;
    
    /**
     * Total content height (for scroll calculation).
     */
    protected int contentHeight = 0;
    
    protected AbstractPanel(Screen parent, FieldEditState state) {
        this.parent = parent;
        this.state = state;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RENDERER CAPABILITY CHECKS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Checks if this panel's required features are supported by the current renderer.
     * 
     * <p>Uses the {@link RequiresFeature} annotation on the class to determine
     * which features are needed.</p>
     * 
     * @return true if all required features are supported
     */
    public boolean isFeatureSupported() {
        RequiresFeature annotation = getClass().getAnnotation(RequiresFeature.class);
        if (annotation == null) {
            return true; // No requirements = always supported
        }
        
        RendererCapabilities.Feature[] required = annotation.value();
        if (required.length == 0) {
            return true;
        }
        
        if (annotation.requireAll()) {
            return RendererCapabilities.areAllSupported(required);
        } else {
            return RendererCapabilities.isAnySupported(required);
        }
    }
    
    /**
     * Gets the tooltip to show when this panel is disabled due to renderer mode.
     * 
     * @return Tooltip text, or null if panel is enabled
     */
    public String getDisabledTooltip() {
        if (isFeatureSupported()) {
            return null;
        }
        
        RequiresFeature annotation = getClass().getAnnotation(RequiresFeature.class);
        if (annotation == null || annotation.value().length == 0) {
            return null;
        }
        
        // Return tooltip for first unsupported feature
        for (RendererCapabilities.Feature f : annotation.value()) {
            String tooltip = RendererCapabilities.getDisabledTooltip(f);
            if (tooltip != null) {
                return tooltip;
            }
        }
        return "§cRequires Accurate mode";
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BOUNDS MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Sets the bounds for this panel and triggers reflow.
     */
    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
        reflow();
    }
    
    /**
     * Sets bounds WITHOUT triggering reflow.
     * Use this when widgets are already initialized and positioned.
     */
    public void setBoundsQuiet(Bounds bounds) {
        this.bounds = bounds;
    }
    
    /**
     * @return Current bounds of this panel.
     */
    public Bounds getBounds() {
        return bounds;
    }
    
    /**
     * @return Available width for content (bounds width).
     */
    protected int getWidth() {
        return bounds.width();
    }
    
    /**
     * @return Available height for content (bounds height).
     */
    protected int getHeight() {
        return bounds.height();
    }
    
    /**
     * Converts a local X coordinate to screen coordinate.
     */
    protected int toScreenX(int localX) {
        return bounds.x() + localX;
    }
    
    /**
     * Converts a local Y coordinate to screen coordinate.
     */
    protected int toScreenY(int localY) {
        return bounds.y() + localY - scrollOffset;
    }
    
    /**
     * Converts screen coordinates to local coordinates.
     */
    protected int toLocalX(int screenX) {
        return screenX - bounds.x();
    }
    
    /**
     * Converts screen coordinates to local coordinates.
     */
    protected int toLocalY(int screenY) {
        return screenY - bounds.y() + scrollOffset;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * @return Unmodifiable list of widgets owned by this panel.
     */
    public List<ClickableWidget> getWidgets() {
        return Collections.unmodifiableList(widgets);
    }
    
    /**
     * Offsets all widgets and sections by the given delta.
     * This is used to move widgets created at (0,0) to their actual bounds position.
     * Call this AFTER init() if widgets were created at local coordinates.
     */
    public void offsetWidgets(int dx, int dy) {
        for (ClickableWidget widget : widgets) {
            widget.setX(widget.getX() + dx);
            widget.setY(widget.getY() + dy);
        }
        for (ExpandableSection section : sections) {
            section.offsetInternalPosition(dx, dy);
        }
    }
    
    /**
     * Moves all widgets and sections to be relative to the current bounds.
     * Assumes widgets were created starting at (0,0).
     */
    public void applyBoundsOffset() {
        offsetWidgets(bounds.x(), bounds.y());
    }
    
    /**
     * Registers an ExpandableSection to be offset along with widgets.
     */
    protected void registerSection(ExpandableSection section) {
        if (section != null && !sections.contains(section)) {
            sections.add(section);
        }
    }
    
    /**
     * Adds a widget to this panel.
     */
    protected <T extends ClickableWidget> T addWidget(T widget) {
        widgets.add(widget);
        return widget;
    }
    
    /**
     * Clears all widgets from this panel.
     */
    protected void clearWidgets() {
        widgets.clear();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SCROLL MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Sets the scroll offset for this panel.
     */
    public void setScrollOffset(int offset) {
        int maxScroll = Math.max(0, contentHeight - bounds.height());
        this.scrollOffset = Math.max(0, Math.min(offset, maxScroll));
    }
    
    /**
     * @return Current scroll offset.
     */
    public int getScrollOffset() {
        return scrollOffset;
    }
    
    /**
     * @return Maximum scroll offset.
     */
    public int getMaxScroll() {
        return Math.max(0, contentHeight - bounds.height());
    }
    
    /**
     * @return Whether this panel has scrollable content.
     */
    public boolean isScrollable() {
        return contentHeight > bounds.height();
    }
    
    /**
     * Handles mouse scroll input.
     * @return true if scroll was consumed.
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!bounds.contains(mouseX, mouseY)) return false;
        if (!isScrollable()) return false;
        
        int scrollDelta = (int)(verticalAmount * 10);
        setScrollOffset(scrollOffset - scrollDelta);
        return true;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE (Template Method Pattern)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Initializes the panel with given dimensions.
     * @deprecated Use {@link #setBounds(Bounds)} and {@link #init()} instead.
     */
    @Deprecated
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        setBounds(new Bounds(0, 0, width, height));
    }
    
    /**
     * Initializes the panel within current bounds.
     * Override this to create widgets.
     */
    protected void init() {
        // Default: do nothing, subclasses override
    }
    
    /**
     * Called when bounds change to reposition widgets.
     * Default implementation clears and re-inits.
     */
    protected void reflow() {
        clearWidgets();
        scrollOffset = 0;
        if (!bounds.isEmpty()) {
            init();
        }
    }
    
    /**
     * Called each tick for updates.
     */
    public abstract void tick();
    
    /**
     * Renders the panel within its bounds.
     * 
     * @param context Draw context
     * @param mouseX Mouse X in screen coordinates
     * @param mouseY Mouse Y in screen coordinates
     * @param delta Frame delta
     */
    public abstract void render(DrawContext context, int mouseX, int mouseY, float delta);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Checks if a point (in screen coordinates) is within this panel's bounds.
     */
    public boolean containsPoint(double screenX, double screenY) {
        return bounds.contains(screenX, screenY);
    }
    
    /**
     * Draws a debug outline around this panel's bounds.
     */
    protected void drawDebugBounds(DrawContext context, int color) {
        context.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), color);
    }
}
