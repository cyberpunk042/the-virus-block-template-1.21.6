package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.fill.CageOptionsAdapter;
import net.cyberpunk042.visual.fill.FillMode;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

/**
 * FillSubPanel - Detailed fill mode options.
 * 
 * <p>Uses {@link CageOptionsAdapter} for shape-aware cage option editing
 * with dynamic labels based on the current shape type.</p>
 * 
 * <p>From 03_PARAMETERS.md §6 Fill Level:</p>
 * <ul>
 *   <li>wireThickness, doubleSided, depthTest, depthWrite</li>
 *   <li>Cage: shape-specific options via CageOptionsAdapter</li>
 *   <li>Points: pointSize</li>
 * </ul>
 * 
 * <p>Note: This panel is displayed inside a sub-tab, so it renders content
 * directly without an ExpandableSection header.</p>
 */
public class FillSubPanel extends AbstractPanel {
    
    private int startY;
    private CyclingButtonWidget<String> fragmentDropdown;
    private CyclingButtonWidget<FillMode> fillModeDropdown;
    private boolean applyingFragment = false;
    private String currentFragment = "Default";
    
    // Common fill options
    private LabeledSlider wireThicknessSlider;
    private CyclingButtonWidget<Boolean> depthWriteToggle;
    
    // Cage-specific (dynamic labels via adapter)
    private LabeledSlider primaryCountSlider;
    private LabeledSlider secondaryCountSlider;
    private CyclingButtonWidget<Boolean> allEdgesToggle;
    private CyclingButtonWidget<Boolean> faceOutlinesToggle;
    
    // Points-specific
    private LabeledSlider pointSizeSlider;
    
    // Current adapter for shape-aware cage options
    private CageOptionsAdapter cageAdapter;
    
    // Callback when widgets change (for re-registration with parent screen)
    private Runnable widgetChangedCallback;
    
    public FillSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("FillSubPanel created");
    }
    
    /** Sets callback for when widgets are rebuilt (for screen widget re-registration). */
    public void setWidgetChangedCallback(Runnable callback) {
        this.widgetChangedCallback = callback;
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        rebuildWidgets();
        Logging.GUI.topic("panel").debug("FillSubPanel initialized for shape: {}", cageAdapter.shapeType());
    }
    
    /**
     * Rebuilds widgets for current fill mode and shape.
     * Called by init() and when mode changes.
     */
    private void rebuildWidgets() {
        // Track if we need to reapply offset (rebuilding vs first init)
        boolean needsOffset = bounds != null && !bounds.isEmpty();
        
        widgets.clear();
        
        // Initialize adapter based on current shape - use getString to get live value
        cageAdapter = CageOptionsAdapter.forShape(state.getString("shapeType"), state.fill().cage());
        
        int x = GuiConstants.PADDING;
        int y = startY + GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;

        // Preset dropdown - show "Custom" since we're loading existing primitive values
        List<String> fillPresets = FragmentRegistry.listFillFragments();
        currentFragment = "Custom";  // Loaded primitives have custom values

        fragmentDropdown = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal(v))
            .values(fillPresets)
            .initially(currentFragment)
            .build(x, y, w, GuiConstants.WIDGET_HEIGHT, net.minecraft.text.Text.literal("Variant"),
                (btn, val) -> applyPreset(val));
        widgets.add(fragmentDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Fill Mode dropdown - manual selection of SOLID/WIREFRAME/CAGE/POINTS
        FillMode currentMode = state.fill().mode();
        fillModeDropdown = GuiWidgets.enumDropdown(
            x, y, w,
            "Mode", FillMode.class, currentMode, "Select fill render mode",
            m -> onUserChange(() -> {
                state.set("fill.mode", m);
                Logging.GUI.topic("fill").info("[FILL-DEBUG] FillSubPanel changed fill.mode to: {}", m.name());
                // Rebuild widgets for new mode
                rebuildWidgets();
                // Notify parent to re-register widgets
                if (widgetChangedCallback != null) {
                    widgetChangedCallback.run();
                }
            }));
        widgets.add(fillModeDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Wire thickness - only for CAGE and WIREFRAME modes
        boolean showWireControls = currentMode == FillMode.CAGE || currentMode == FillMode.WIREFRAME;
        if (showWireControls) {
            wireThicknessSlider = LabeledSlider.builder("Wire Thickness")
                .position(x, y).width(w)
                .range(0.1f, 2f).initial(state.fill().wireThickness()).format("%.1f")
                .onChange(v -> onUserChange(() -> {
                    state.set("fill.wireThickness", v);
                    Logging.GUI.topic("fill").trace("Wire thickness: {}", v);
                })).build();
            widgets.add(wireThicknessSlider);
            y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        }
        
        // See-Through mode - only for SOLID mode (controls whether mesh blocks objects behind it)
        if (currentMode == FillMode.SOLID) {
            depthWriteToggle = GuiWidgets.toggle(x, y, w, "See-Through Mode",
                !state.fill().depthWrite(), "Enable see-through translucency (disable depth write)", v -> {
                    onUserChange(() -> {
                        // Toggle is inverted: "See-Through Mode ON" = depthWrite false
                        state.set("fill.depthWrite", !v);
                        Logging.GUI.topic("fill").debug("Depth write: {} (see-through: {})", !v, v);
                    });
                });
            widgets.add(depthWriteToggle);
            y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        }
        
        // Cage settings - only for CAGE mode, dynamic labels from adapter
        boolean showCageControls = currentMode == FillMode.CAGE;
        if (showCageControls && cageAdapter.supportsCountOptions()) {
            primaryCountSlider = LabeledSlider.builder(cageAdapter.primaryLabel())
                .position(x, y).width(halfW)
                .range(1, 32).initial(cageAdapter.primaryCount()).format("%d").step(1)
                .onChange(v -> onUserChange(() -> {
                    cageAdapter = cageAdapter.withPrimaryCount(v.intValue());
                    updateCageInState();
                    Logging.GUI.topic("fill").trace("Cage primary: {}", v.intValue());
                })).build();
            widgets.add(primaryCountSlider);
            
            secondaryCountSlider = LabeledSlider.builder(cageAdapter.secondaryLabel())
                .position(x + halfW + GuiConstants.PADDING, y).width(halfW)
                .range(1, 64).initial(cageAdapter.secondaryCount()).format("%d").step(1)
                .onChange(v -> onUserChange(() -> {
                    cageAdapter = cageAdapter.withSecondaryCount(v.intValue());
                    updateCageInState();
                    Logging.GUI.topic("fill").trace("Cage secondary: {}", v.intValue());
                })).build();
            widgets.add(secondaryCountSlider);
            y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        }
        
        // Polyhedron-specific options - only for CAGE mode
        if (showCageControls && cageAdapter.hasPolyhedronExtras()) {
            allEdgesToggle = GuiWidgets.toggle(x, y, halfW, "All Edges", 
                cageAdapter.allEdges(), "Show all structural edges", v -> {
                    onUserChange(() -> {
                        cageAdapter = cageAdapter.withAllEdges(v);
                        updateCageInState();
                    });
                });
            widgets.add(allEdgesToggle);
            
            faceOutlinesToggle = GuiWidgets.toggle(x + halfW + GuiConstants.PADDING, y, halfW, 
                "Face Outlines", cageAdapter.faceOutlines(), "Show face outlines only", v -> {
                    onUserChange(() -> {
                        cageAdapter = cageAdapter.withFaceOutlines(v);
                        updateCageInState();
                    });
                });
            widgets.add(faceOutlinesToggle);
            y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        }
        
        // Point size - only for POINTS mode
        if (currentMode == FillMode.POINTS) {
            pointSizeSlider = LabeledSlider.builder("Point Size")
                .position(x, y).width(w)
                .range(1f, 10f).initial(state.fill().pointSize()).format("%.1f")
                .onChange(v -> onUserChange(() -> {
                    state.set("fill.pointSize", v);
                    Logging.GUI.topic("fill").trace("Point size: {}", v);
                })).build();
            widgets.add(pointSizeSlider);
            y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        }
        
        // Track content height for scrolling
        contentHeight = y - startY;
        
        // Reapply bounds offset if rebuilding (not first init)
        if (needsOffset) {
            applyBoundsOffset();
        }
    }
    
    /** Updates the cage options in state's fill config. */
    private void updateCageInState() {
        var newFill = state.fill().toBuilder().cage(cageAdapter.build()).build();
        state.set("fill", newFill);
    }
    
    @Override public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render all widgets directly (no expandable section)
        for (var widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
    }
    
    public int getHeight() { 
        return contentHeight; 
    }

    private void onUserChange(Runnable r) {
        r.run();
        if (!applyingFragment) {
            currentFragment = "Custom";
            if (fragmentDropdown != null) fragmentDropdown.setValue("Custom");
        }
    }

    private void applyPreset(String name) {
        if ("Custom".equalsIgnoreCase(name)) {
            currentFragment = "Custom";
            return;
        }
        applyingFragment = true;
        currentFragment = name;
        FragmentRegistry.applyFillFragment(state, name);
        syncFromState();
        applyingFragment = false;
    }

    private void syncFromState() {
        // Refresh adapter for current shape - use getString to get live value
        cageAdapter = CageOptionsAdapter.forShape(state.getString("shapeType"), state.fill().cage());
        
        // Sync fill mode dropdown
        if (fillModeDropdown != null) fillModeDropdown.setValue(state.fill().mode());
        
        if (wireThicknessSlider != null) wireThicknessSlider.setValue(state.fill().wireThickness());
        if (depthWriteToggle != null) depthWriteToggle.setValue(!state.fill().depthWrite()); // Inverted: See-Through = !depthWrite
        
        // Cage values from adapter
        if (primaryCountSlider != null && cageAdapter.supportsCountOptions()) {
            primaryCountSlider.setValue(cageAdapter.primaryCount());
        }
        if (secondaryCountSlider != null && cageAdapter.supportsCountOptions()) {
            secondaryCountSlider.setValue(cageAdapter.secondaryCount());
        }
        if (allEdgesToggle != null) allEdgesToggle.setValue(cageAdapter.allEdges());
        if (faceOutlinesToggle != null) faceOutlinesToggle.setValue(cageAdapter.faceOutlines());
        
        if (pointSizeSlider != null) pointSizeSlider.setValue(state.fill().pointSize());
    }
    
    /** Called when shape type changes - rebuilds widgets with new labels. */
    public void onShapeChanged() {
        // Re-init to get new labels and options for the new shape
        init(panelWidth, panelHeight);
    }
}
