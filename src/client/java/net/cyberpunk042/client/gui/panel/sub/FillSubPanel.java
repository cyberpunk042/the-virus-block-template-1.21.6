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
    private CyclingButtonWidget<Boolean> doubleSidedToggle;
    private CyclingButtonWidget<Boolean> depthTestToggle;
    private CyclingButtonWidget<Boolean> depthWriteToggle;
    
    // Cage-specific (dynamic labels via adapter)
    private LabeledSlider primaryCountSlider;
    private LabeledSlider secondaryCountSlider;
    private CyclingButtonWidget<Boolean> showEquatorToggle;
    private CyclingButtonWidget<Boolean> showPolesToggle;
    private CyclingButtonWidget<Boolean> showCapsToggle;
    private CyclingButtonWidget<Boolean> allEdgesToggle;
    private CyclingButtonWidget<Boolean> faceOutlinesToggle;
    
    // Points-specific
    private LabeledSlider pointSizeSlider;
    
    // Current adapter for shape-aware cage options
    private CageOptionsAdapter cageAdapter;
    
    public FillSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("FillSubPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        // Initialize adapter based on current shape
        cageAdapter = CageOptionsAdapter.forShape(state.shapeType, state.fill().cage());
        
        int x = GuiConstants.PADDING;
        int y = startY + GuiConstants.PADDING;
        int w = width - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;

        // Preset dropdown
        List<String> fillPresets = FragmentRegistry.listFillFragments();

        fragmentDropdown = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal(v))
            .values(fillPresets)
            .initially(currentFragment)
            .build(x, y, w, GuiConstants.WIDGET_HEIGHT, net.minecraft.text.Text.literal("Variant"),
                (btn, val) -> applyPreset(val));
        widgets.add(fragmentDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Fill Mode dropdown - manual selection of SOLID/WIREFRAME/CAGE/POINTS
        fillModeDropdown = GuiWidgets.enumDropdown(
            x, y, w,
            "Mode", FillMode.class, state.fill().mode(), "Select fill render mode",
            m -> onUserChange(() -> {
                state.set("fill.mode", m);
                Logging.GUI.topic("fill").info("[FILL-DEBUG] FillSubPanel changed fill.mode to: {}", m.name());
            }));
        widgets.add(fillModeDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Wire thickness
        wireThicknessSlider = LabeledSlider.builder("Wire Thickness")
            .position(x, y).width(w)
            .range(0.1f, 5f).initial(state.fill().wireThickness()).format("%.1f")
            .onChange(v -> onUserChange(() -> {
                state.set("fill.wireThickness", v);
                Logging.GUI.topic("fill").trace("Wire thickness: {}", v);
            })).build();
        widgets.add(wireThicknessSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Double sided
        doubleSidedToggle = GuiWidgets.toggle(x, y, halfW, "Double Sided", state.fill().doubleSided(),
            "Render both faces", v -> {
                onUserChange(() -> {
                    state.set("fill.doubleSided", v);
                    Logging.GUI.topic("fill").debug("Double sided: {}", v);
                });
            });
        widgets.add(doubleSidedToggle);
        
        // Depth test
        depthTestToggle = GuiWidgets.toggle(x + halfW + GuiConstants.PADDING, y, halfW, "Depth Test",
            state.fill().depthTest(), "Enable depth testing", v -> {
                onUserChange(() -> {
                    state.set("fill.depthTest", v);
                    Logging.GUI.topic("fill").debug("Depth test: {}", v);
                });
            });
        widgets.add(depthTestToggle);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Cage settings - dynamic labels from adapter
        if (cageAdapter.supportsCountOptions()) {
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
        
        // Sphere-specific extras
        if (cageAdapter.hasSphereExtras()) {
            showEquatorToggle = GuiWidgets.toggle(x, y, halfW, "Show Equator", 
                cageAdapter.showEquator(), "Show equator line", v -> {
                    onUserChange(() -> {
                        cageAdapter = cageAdapter.withShowEquator(v);
                        updateCageInState();
                    });
                });
            widgets.add(showEquatorToggle);
            
            showPolesToggle = GuiWidgets.toggle(x + halfW + GuiConstants.PADDING, y, halfW, 
                "Show Poles", cageAdapter.showPoles(), "Show pole markers", v -> {
                    onUserChange(() -> {
                        cageAdapter = cageAdapter.withShowPoles(v);
                        updateCageInState();
                    });
                });
            widgets.add(showPolesToggle);
            y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        }
        
        // Cylinder/Prism caps option
        if (cageAdapter.hasCapsOption()) {
            showCapsToggle = GuiWidgets.toggle(x, y, w, "Show Caps", 
                cageAdapter.showCaps(), "Show top/bottom caps", v -> {
                    onUserChange(() -> {
                        cageAdapter = cageAdapter.withShowCaps(v);
                        updateCageInState();
                    });
                });
            widgets.add(showCapsToggle);
            y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        }
        
        // Polyhedron-specific options
        if (cageAdapter.hasPolyhedronExtras()) {
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
        
        // Point size
        pointSizeSlider = LabeledSlider.builder("Point Size")
            .position(x, y).width(w)
            .range(1f, 10f).initial(state.pointSize).format("%.1f")
            .onChange(v -> onUserChange(() -> {
                state.set("pointSize", v);
                Logging.GUI.topic("fill").trace("Point size: {}", v);
            })).build();
        widgets.add(pointSizeSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Track content height for scrolling
        contentHeight = y - startY;
        Logging.GUI.topic("panel").debug("FillSubPanel initialized for shape: {}", cageAdapter.shapeType());
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
        // Refresh adapter for current shape
        cageAdapter = CageOptionsAdapter.forShape(state.shapeType, state.fill().cage());
        
        // Sync fill mode dropdown
        if (fillModeDropdown != null) fillModeDropdown.setValue(state.fill().mode());
        
        if (wireThicknessSlider != null) wireThicknessSlider.setValue(state.fill().wireThickness());
        if (doubleSidedToggle != null) doubleSidedToggle.setValue(state.fill().doubleSided());
        if (depthTestToggle != null) depthTestToggle.setValue(state.fill().depthTest());
        if (depthWriteToggle != null) depthWriteToggle.setValue(state.fill().depthWrite());
        
        // Cage values from adapter
        if (primaryCountSlider != null && cageAdapter.supportsCountOptions()) {
            primaryCountSlider.setValue(cageAdapter.primaryCount());
        }
        if (secondaryCountSlider != null && cageAdapter.supportsCountOptions()) {
            secondaryCountSlider.setValue(cageAdapter.secondaryCount());
        }
        if (showEquatorToggle != null) showEquatorToggle.setValue(cageAdapter.showEquator());
        if (showPolesToggle != null) showPolesToggle.setValue(cageAdapter.showPoles());
        if (showCapsToggle != null) showCapsToggle.setValue(cageAdapter.showCaps());
        if (allEdgesToggle != null) allEdgesToggle.setValue(cageAdapter.allEdges());
        if (faceOutlinesToggle != null) faceOutlinesToggle.setValue(cageAdapter.faceOutlines());
        
        if (pointSizeSlider != null) pointSizeSlider.setValue(state.pointSize);
    }
    
    /** Called when shape type changes - rebuilds widgets with new labels. */
    public void onShapeChanged() {
        // Re-init to get new labels and options for the new shape
        init(panelWidth, panelHeight);
    }
}
