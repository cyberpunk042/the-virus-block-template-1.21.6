package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.annotation.ShowWhen;
import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.fill.CageOptionsAdapter;
import net.cyberpunk042.visual.fill.FillMode;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.util.List;

/**
 * FillSubPanel - Detailed fill mode options with annotation-based visibility.
 * 
 * <p>Uses {@link ShowWhen} annotations to declaratively control widget visibility
 * based on fill mode. Uses {@link CageOptionsAdapter} for shape-aware cage options.</p>
 * 
 * <p>Widget visibility rules:</p>
 * <ul>
 *   <li>Wire Thickness: CAGE, WIREFRAME modes</li>
 *   <li>See-Through: SOLID mode only</li>
 *   <li>Cage sliders: CAGE mode only</li>
 *   <li>Point Size: POINTS mode only</li>
 * </ul>
 */
public class FillSubPanel extends AbstractPanel {
    
    // Current adapter for shape-aware cage options
    private CageOptionsAdapter cageAdapter;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ALWAYS VISIBLE widgets
    // ═══════════════════════════════════════════════════════════════════════════
    
    private CyclingButtonWidget<String> fragmentDropdown;
    private CyclingButtonWidget<FillMode> fillModeDropdown;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONDITIONALLY VISIBLE widgets (visibility controlled by buildConditionalWidgets)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @ShowWhen(fillMode = "CAGE")
    @ShowWhen(fillMode = "WIREFRAME")
    private LabeledSlider wireThicknessSlider;
    
    @ShowWhen(fillMode = "SOLID")
    private CyclingButtonWidget<Boolean> depthWriteToggle;
    
    @ShowWhen(fillMode = "CAGE")
    private LabeledSlider primaryCountSlider;
    
    @ShowWhen(fillMode = "CAGE")
    private LabeledSlider secondaryCountSlider;
    
    @ShowWhen(fillMode = "CAGE")
    private CyclingButtonWidget<Boolean> allEdgesToggle;
    
    @ShowWhen(fillMode = "CAGE")
    private CyclingButtonWidget<Boolean> faceOutlinesToggle;
    
    @ShowWhen(fillMode = "POINTS")
    private LabeledSlider pointSizeSlider;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & INIT
    // ═══════════════════════════════════════════════════════════════════════════
    
    public FillSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("FillSubPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        rebuildWidgets();
        Logging.GUI.topic("panel").debug("FillSubPanel initialized for shape: {}", cageAdapter.shapeType());
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET BUILDING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Rebuilds widgets based on current state.
     * Uses @ShowWhen annotations to determine visibility.
     */
    private void rebuildWidgets() {
        boolean needsOffset = bounds != null && !bounds.isEmpty();
        widgets.clear();
        
        // Initialize adapter based on current shape
        cageAdapter = CageOptionsAdapter.forShape(state.getString("shapeType"), state.fill().cage());
        
        int x = GuiConstants.PADDING;
        int y = startY + GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        FillMode currentMode = state.fill().mode();
        
        // Fragment dropdown - build at correct position and add immediately (like ShapeSubPanel)
        List<String> fillPresets = FragmentRegistry.listFillFragments();
        Logging.GUI.topic("panel").info("FillSubPanel fill presets loaded: {}", fillPresets);
        // Only reset to "Custom" if not currently applying a fragment
        if (!applyingFragment) {
            currentFragment = "Custom";
        }
        fragmentDropdown = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal(v))
            .values(fillPresets)
            .initially(currentFragment)
            .build(x, y, w, GuiConstants.WIDGET_HEIGHT, net.minecraft.text.Text.literal("Variant"),
                (btn, val) -> {
                    Logging.GUI.topic("panel").info("Fragment button clicked, val={}", val);
                    applyPreset(val);
                });
        widgets.add(fragmentDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Fill mode dropdown - build at correct position
        fillModeDropdown = GuiWidgets.enumDropdown(
            x, y, w,
            "Mode", FillMode.class, currentMode, "Select fill render mode",
            m -> onUserChange(() -> {
                state.set("fill.mode", m);
                Logging.GUI.topic("fill").info("[FILL-DEBUG] Fill mode changed to: {}", m.name());
                rebuildWidgets();
                notifyWidgetsChanged();
            }));
        widgets.add(fillModeDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Build other widgets that need visibility checks
        buildConditionalWidgets(x, y, w, halfW, currentMode);
        
        if (needsOffset) {
            applyBoundsOffset();
        }
        
        Logging.GUI.topic("panel").debug("FillSubPanel rebuilt: {} widgets visible", widgets.size());
    }
    
    /**
     * Builds conditional widgets based on current fill mode.
     * These widgets are positioned and added based on visibility rules.
     */
    private void buildConditionalWidgets(int x, int y, int w, int halfW, FillMode currentMode) {
        // Wire thickness (CAGE, WIREFRAME)
        if (currentMode == FillMode.CAGE || currentMode == FillMode.WIREFRAME) {
            wireThicknessSlider = LabeledSlider.builder("Wire Thickness")
                .position(x, y).width(w)
                .range(0.1f, 2f).initial(state.fill().wireThickness()).format("%.1f")
                .onChange(v -> onUserChange(() -> {
                    state.set("fill.wireThickness", v);
                })).build();
            widgets.add(wireThicknessSlider);
            y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        }
        
        // See-Through toggle (SOLID)
        if (currentMode == FillMode.SOLID) {
            depthWriteToggle = GuiWidgets.toggle(x, y, w, "See-Through Mode",
                !state.fill().depthWrite(), "Enable see-through translucency", v -> {
                    onUserChange(() -> state.set("fill.depthWrite", !v));
                });
            widgets.add(depthWriteToggle);
            y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        }
        
        // Cage count sliders (CAGE)
        if (currentMode == FillMode.CAGE && cageAdapter.supportsCountOptions()) {
            primaryCountSlider = LabeledSlider.builder(cageAdapter.primaryLabel())
                .position(x, y).width(halfW)
                .range(1, 128).initial(cageAdapter.primaryCount()).format("%d").step(1)
                .onChange(v -> onUserChange(() -> {
                    cageAdapter = cageAdapter.withPrimaryCount(v.intValue());
                    updateCageInState();
                })).build();
            widgets.add(primaryCountSlider);
            
            secondaryCountSlider = LabeledSlider.builder(cageAdapter.secondaryLabel())
                .position(x + halfW + GuiConstants.PADDING, y).width(halfW)
                .range(1, 256).initial(cageAdapter.secondaryCount()).format("%d").step(1)
                .onChange(v -> onUserChange(() -> {
                    cageAdapter = cageAdapter.withSecondaryCount(v.intValue());
                    updateCageInState();
                })).build();
            widgets.add(secondaryCountSlider);
            y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        }
        
        // Polyhedron extras (CAGE)
        if (currentMode == FillMode.CAGE && cageAdapter.hasPolyhedronExtras()) {
            allEdgesToggle = GuiWidgets.toggle(x, y, halfW, "All Edges",
                cageAdapter.allEdges(), "Show all edges between faces", v -> {
                    onUserChange(() -> {
                        cageAdapter = cageAdapter.withAllEdges(v);
                        updateCageInState();
                    });
                });
            widgets.add(allEdgesToggle);
            
            faceOutlinesToggle = GuiWidgets.toggle(x + halfW + GuiConstants.PADDING, y, halfW, "Face Lines",
                cageAdapter.faceOutlines(), "Show face outline patterns", v -> {
                    onUserChange(() -> {
                        cageAdapter = cageAdapter.withFaceOutlines(v);
                        updateCageInState();
                    });
                });
            widgets.add(faceOutlinesToggle);
            y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        }
        
        // Point size (POINTS)
        if (currentMode == FillMode.POINTS) {
            pointSizeSlider = LabeledSlider.builder("Point Size")
                .position(x, y).width(w)
                .range(1f, 10f).initial(state.fill().pointSize()).format("%.1f")
                .onChange(v -> onUserChange(() -> {
                    state.set("fill.pointSize", v);
                })).build();
            widgets.add(pointSizeSlider);
            y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        }
        
        contentHeight = y - startY;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void updateCageInState() {
        var newFill = state.fill().toBuilder().cage(cageAdapter.build()).build();
        state.set("fill", newFill);
    }
    
    private void onUserChange(Runnable r) {
        onUserChange(r, fragmentDropdown);
    }
    
    private void applyPreset(String name) {
        if ("Custom".equalsIgnoreCase(name)) {
            currentFragment = name;
            return;
        }
        applyingFragment = true;
        currentFragment = name;
        
        if ("Default".equalsIgnoreCase(name)) {
            // Apply actual default fill config
            state.set("fill", net.cyberpunk042.field.loader.DefaultsProvider.getDefaultFill());
        } else {
            FragmentRegistry.applyFillFragment(state, name);
        }
        
        // Rebuild widgets to show correct fields for new mode (like mode change does)
        rebuildWidgets();
        notifyWidgetsChanged();
        applyingFragment = false;
    }
    
    /** Syncs widget values from state without rebuilding widgets. */
    private void syncFromState() {
        FillMode m = state.fill().mode();
        if (fillModeDropdown != null) fillModeDropdown.setValue(m);
        if (wireThicknessSlider != null) wireThicknessSlider.setValue(state.fill().wireThickness());
        if (pointSizeSlider != null) pointSizeSlider.setValue(state.fill().pointSize());
        if (depthWriteToggle != null) depthWriteToggle.setValue(!state.fill().depthWrite());
        
        // Update cage adapter and sliders
        cageAdapter = CageOptionsAdapter.forShape(state.getString("shapeType"), state.fill().cage());
        if (primaryCountSlider != null && cageAdapter.supportsCountOptions()) {
            primaryCountSlider.setValue(cageAdapter.primaryCount());
        }
        if (secondaryCountSlider != null && cageAdapter.supportsCountOptions()) {
            secondaryCountSlider.setValue(cageAdapter.secondaryCount());
        }
        if (allEdgesToggle != null && cageAdapter.hasPolyhedronExtras()) {
            allEdgesToggle.setValue(cageAdapter.allEdges());
        }
        if (faceOutlinesToggle != null && cageAdapter.hasPolyhedronExtras()) {
            faceOutlinesToggle.setValue(cageAdapter.faceOutlines());
        }
        // NOTE: Do NOT rebuild here - just update values on existing widgets
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override 
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        for (var widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
    }
    
    /** Called when shape type changes - rebuilds widgets with new labels. */
    public void onShapeChanged() {
        init(panelWidth, panelHeight);
    }
}
