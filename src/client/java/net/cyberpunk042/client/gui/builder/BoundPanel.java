package net.cyberpunk042.client.gui.builder;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.ChangeType;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.StateChangeListener;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for panels using the bidirectional binding system.
 * 
 * <p>Extends {@link AbstractPanel} with automatic state synchronization:
 * <ul>
 *   <li>Registers as a {@link StateChangeListener} when attached</li>
 *   <li>Automatically syncs all bindings on profile load, primitive switch, etc.</li>
 *   <li>Provides binding registration for {@link Bound} and {@link Vec3Binding}</li>
 * </ul>
 * 
 * <h3>Usage</h3>
 * <pre>
 * public class MyPanel extends BoundPanel {
 *     &#64;Override
 *     protected void buildContent(ContentBuilder b) {
 *         b.slider("Radius", "sphere.radius", 0.1f, 10f)
 *          .toggle("Spin", "spin.active");
 *     }
 * }
 * </pre>
 * 
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>{@code init()} - Creates widgets via {@code buildContent()}, registers bindings</li>
 *   <li>{@code onStateChanged()} - Called by {@link FieldEditState} on external changes</li>
 *   <li>{@code dispose()} - Unregisters from state listeners</li>
 * </ol>
 * 
 * @see Bound
 * @see Vec3Binding
 * @see ContentBuilder
 */
public abstract class BoundPanel extends AbstractPanel implements StateChangeListener {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BINDINGS
    // ═══════════════════════════════════════════════════════════════════════════
    
    protected final List<Bound<?, ?, ?>> bindings = new ArrayList<>();
    protected final List<Vec3Binding> vec3Bindings = new ArrayList<>();
    
    private boolean attached = false;
    
    public BoundPanel(Screen parent, FieldEditState state) {
        super(parent, state);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        
        // Clear previous state
        bindings.clear();
        vec3Bindings.clear();
        widgets.clear();
        
        // Build content using fluent API (subclass implements this)
        buildContent();
        
        // Register as listener if not already
        if (!attached) {
            state.addStateListener(this);
            attached = true;
            Logging.GUI.topic("panel").debug("{} attached as state listener", getClass().getSimpleName());
        }
    }
    
    /**
     * Build the panel content using bindings.
     * 
     * <p>Subclasses override this to create their UI. Use {@link #content(int)}
     * to create a {@link ContentBuilder} for fluent widget creation:</p>
     * <pre>
     * &#64;Override
     * protected void buildContent() {
     *     ContentBuilder b = content(startY);
     *     b.slider("Radius", "sphere.radius").range(0.1f, 10f).add();
     *     contentHeight = b.getContentHeight();
     * }
     * </pre>
     */
    protected abstract void buildContent();
    
    /**
     * Creates a ContentBuilder for fluent widget creation.
     * 
     * @param startY Starting Y position for content
     * @return A new ContentBuilder
     */
    protected ContentBuilder content(int startY) {
        return new ContentBuilder(state, bindings, widgets, startY, panelWidth);
    }
    
    /**
     * Called when disposing the panel. Unregisters state listeners.
     */
    public void dispose() {
        if (attached) {
            state.removeStateListener(this);
            attached = false;
            Logging.GUI.topic("panel").debug("{} detached from state listeners", getClass().getSimpleName());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BINDING REGISTRATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Registers a binding and adds its widget to the panel.
     * 
     * @param binding The binding to register
     * @return The binding (for chaining)
     */
    protected <W extends net.minecraft.client.gui.widget.ClickableWidget, S, D> 
            Bound<W, S, D> registerBinding(Bound<W, S, D> binding) {
        bindings.add(binding);
        widgets.add(binding.widget());
        return binding;
    }
    
    /**
     * Registers a Vec3 binding and adds all three sliders to the panel.
     * 
     * @param binding The Vec3 binding to register
     * @return The binding (for chaining)
     */
    protected Vec3Binding registerVec3Binding(Vec3Binding binding) {
        vec3Bindings.add(binding);
        for (var slider : binding.widgets()) {
            if (slider != null) widgets.add(slider);
        }
        return binding;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE CHANGE LISTENER
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public void onStateChanged(ChangeType changeType) {
        switch (changeType) {
            case PROFILE_LOADED, PRIMITIVE_SWITCHED, LAYER_SWITCHED, FULL_RESET -> {
                // Major change: sync all bindings
                syncAllFromState();
            }
            case FRAGMENT_APPLIED -> {
                // Might need rebuild if mode changed, otherwise just sync
                if (needsRebuildOnFragmentApply()) {
                    rebuildContent();
                } else {
                    syncAllFromState();
                }
            }
            case PROPERTY_CHANGED -> {
                // Single property: sync all (could be more targeted in future)
                syncAllFromState();
            }
        }
    }
    
    /**
     * Syncs all registered bindings from state.
     * Call this when state changes externally.
     */
    protected void syncAllFromState() {
        for (Bound<?, ?, ?> binding : bindings) {
            binding.syncFromState();
        }
        for (Vec3Binding v3 : vec3Bindings) {
            v3.syncFromState();
        }
        
        Logging.GUI.topic("panel").trace("{} synced {} bindings + {} vec3 bindings", 
            getClass().getSimpleName(), bindings.size(), vec3Bindings.size());
    }
    
    /**
     * Rebuilds the panel content. Call when structural changes require new widgets.
     * Override if special behavior is needed.
     */
    protected void rebuildContent() {
        init(panelWidth, panelHeight);
        // Apply bounds offset so widgets are positioned correctly relative to panel
        if (bounds != null && !bounds.isEmpty()) {
            applyBoundsOffset();
        }
        notifyWidgetsChanged();
    }
    
    /**
     * Override in panels that need rebuild when a fragment is applied.
     * 
     * <p>Examples:</p>
     * <ul>
     *   <li>FillSubPanel: Fill mode change requires different widgets</li>
     *   <li>TransformSubPanel: Motion mode change requires different sliders</li>
     * </ul>
     * 
     * @return true if fragment application should trigger widget rebuild
     */
    protected boolean needsRebuildOnFragmentApply() {
        return false;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SCROLLING SUPPORT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Renders widgets with scroll offset applied.
     * Use this in render() for panels with scrollable content.
     * 
     * @param context The draw context
     * @param mouseX Mouse X
     * @param mouseY Mouse Y
     * @param delta Frame delta
     */
    /**
     * Applies scroll offset to widget positions for rendering.
     * Call this BEFORE super.render() and call restoreScrollPositions() AFTER.
     * 
     * <p>This method does NOT render widgets - that's done by Screen.render().
     * It only repositions widgets so they appear scrolled when rendered.</p>
     */
    protected void applyScrollPositions() {
        if (scrollOffset != 0) {
            for (var widget : widgets) {
                widget.setY(widget.getY() - scrollOffset);
            }
        }
    }
    
    /**
     * Restores widget positions after scrolled rendering.
     * Call this AFTER super.render() to undo the scroll offset.
     */
    protected void restoreScrollPositions() {
        if (scrollOffset != 0) {
            for (var widget : widgets) {
                widget.setY(widget.getY() + scrollOffset);
            }
        }
    }
    
    /**
     * Legacy renderWithScroll - applies scroll, renders widgets, restores.
     * Use applyScrollPositions/restoreScrollPositions instead for proper integration.
     * 
     * @deprecated Use applyScrollPositions() + super.render() + restoreScrollPositions()
     */
    @Deprecated
    protected void renderWithScroll(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        if (bounds == null || bounds.isEmpty()) return;
        
        // Clip to panel bounds
        context.enableScissor(bounds.x(), bounds.y(), bounds.right(), bounds.bottom());
        
        applyScrollPositions();
        
        // Render all widgets
        for (var widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
        
        restoreScrollPositions();
        
        context.disableScissor();
        
        // Render scroll indicator if scrollable
        if (isScrollable()) {
            renderScrollIndicator(context);
        }
    }
    
    /**
     * Renders a simple scroll indicator bar.
     */
    protected void renderScrollIndicator(net.minecraft.client.gui.DrawContext context) {
        int barWidth = 3;
        int barX = bounds.right() - barWidth - 1;
        int trackHeight = bounds.height() - 4;
        int trackY = bounds.y() + 2;
        
        // Track
        context.fill(barX, trackY, barX + barWidth, trackY + trackHeight, 0x40FFFFFF);
        
        // Thumb
        int maxScroll = Math.max(1, contentHeight - bounds.height());
        float scrollPercent = (float) scrollOffset / maxScroll;
        int thumbHeight = Math.max(10, trackHeight * bounds.height() / contentHeight);
        int thumbY = trackY + (int) ((trackHeight - thumbHeight) * scrollPercent);
        
        context.fill(barX, thumbY, barX + barWidth, thumbY + thumbHeight, 0xFFAAAAAA);
    }
}

