package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ExpandableSection;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * ArrangementSubPanel - Cell pattern selection.
 * 
 * <p>From 03_PARAMETERS.md §8 Arrangement Level:</p>
 * <ul>
 *   <li>Pattern per CellType (QUAD, SEGMENT, SECTOR, EDGE, TRIANGLE)</li>
 *   <li>Multi-part arrangements (caps, sides, poles, equator)</li>
 * </ul>
 */
public class ArrangementSubPanel extends AbstractPanel {
    
    private int startY;
    private CyclingButtonWidget<String> fragmentDropdown;
    private boolean applyingFragment = false;
    private String currentFragment = "Default";
    
    /** Quad patterns (for filled shapes). */
    public enum QuadPattern {
        FILLED("Filled"),
        TRIANGLE_1("Triangle 1"),
        TRIANGLE_2("Triangle 2"),
        WAVE("Wave"),
        TOOTH("Tooth"),
        PARALLELOGRAM("Parallelogram");
        
        private final String label;
        QuadPattern(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    
    /** Segment patterns (for lines). */
    public enum SegmentPattern {
        FULL("Full"),
        ALTERNATING("Alternating"),
        SPARSE("Sparse"),
        DASHED("Dashed"),
        DOTTED("Dotted");
        
        private final String label;
        SegmentPattern(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    
    /** Sector patterns (for pie slices). */
    public enum SectorPattern {
        FULL("Full"),
        HALF("Half"),
        QUARTERS("Quarters"),
        PINWHEEL("Pinwheel"),
        SPIRAL("Spiral");
        
        private final String label;
        SectorPattern(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    
    private CyclingButtonWidget<QuadPattern> quadDropdown;
    private CyclingButtonWidget<SegmentPattern> segmentDropdown;
    private CyclingButtonWidget<SectorPattern> sectorDropdown;
    private ButtonWidget multiPartToggle;
    
    // UI state - controls visibility of per-part pattern options
    private boolean showPerPartControls = false;
    
    public ArrangementSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("ArrangementSubPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        
        int x = GuiConstants.PADDING;
        int y = startY + GuiConstants.PADDING;
        int w = width - GuiConstants.PADDING * 2;

        // Preset dropdown
        List<String> arrPresets = FragmentRegistry.listArrangementFragments();

        fragmentDropdown = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal(v))
            .values(arrPresets)
            .initially(currentFragment)
            .build(x, y, w, GuiConstants.WIDGET_HEIGHT, net.minecraft.text.Text.literal("Variant"),
                (btn, val) -> applyPreset(val));
        widgets.add(fragmentDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Quad pattern
        quadDropdown = GuiWidgets.enumDropdown(x, y, w, "Quad Pattern", QuadPattern.class, QuadPattern.FILLED,
            "Pattern for filled quads", v -> onUserChange(() -> {
                state.set("arrangement.quadPattern", v.name());
                Logging.GUI.topic("arrangement").debug("Quad: {}", v);
            }));
        widgets.add(quadDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Segment pattern
        segmentDropdown = GuiWidgets.enumDropdown(x, y, w, "Line Pattern", SegmentPattern.class, SegmentPattern.FULL,
            "Pattern for line segments", v -> onUserChange(() -> {
                state.set("arrangement.segmentPattern", v.name());
                Logging.GUI.topic("arrangement").debug("Segment: {}", v);
            }));
        widgets.add(segmentDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Sector pattern
        sectorDropdown = GuiWidgets.enumDropdown(x, y, w, "Sector Pattern", SectorPattern.class, SectorPattern.FULL,
            "Pattern for pie sectors", v -> onUserChange(() -> {
                state.set("arrangement.sectorPattern", v.name());
                Logging.GUI.topic("arrangement").debug("Sector: {}", v);
            }));
        widgets.add(sectorDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Multi-part toggle (UI state only - toggles per-region controls)
        multiPartToggle = ButtonWidget.builder(
                net.minecraft.text.Text.literal(showPerPartControls ? "▼ Per-Region Patterns" : "▶ Per-Region Patterns"),
                btn -> {
                    showPerPartControls = !showPerPartControls;
                    btn.setMessage(net.minecraft.text.Text.literal(showPerPartControls ? "▼ Per-Region Patterns" : "▶ Per-Region Patterns"));
                    reflow(); // Rebuild UI
                })
            .dimensions(x, y, w, GuiConstants.WIDGET_HEIGHT)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(net.minecraft.text.Text.literal("Enable per-region patterns (caps, sides, poles)")))
            .build();
        widgets.add(multiPartToggle);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        contentHeight = y - startY + GuiConstants.PADDING;
        Logging.GUI.topic("panel").debug("ArrangementSubPanel initialized");
    }
    
    @Override public void tick() {}

    @Override
    protected void reflow() {
        clearWidgets();
        scrollOffset = 0;
        if (panelWidth > 0 && panelHeight > 0) {
            init(panelWidth, panelHeight);
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render widgets directly (no expandable section)
        for (var widget : widgets) widget.render(context, mouseX, mouseY, delta);
    }
    
    public int getHeight() { return contentHeight; }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> all = new ArrayList<>();
        // No header button (direct content)
        all.addAll(widgets);
        return all;
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
        FragmentRegistry.applyArrangementFragment(state, name);
        syncFromState();
        applyingFragment = false;
    }

    private void syncFromState() {
        if (quadDropdown != null) {
            try { quadDropdown.setValue(QuadPattern.valueOf(state.getString("arrangement.quadPattern"))); } catch (IllegalArgumentException ignored) {}
        }
        if (segmentDropdown != null) {
            try { segmentDropdown.setValue(SegmentPattern.valueOf(state.getString("arrangement.segmentPattern"))); } catch (IllegalArgumentException ignored) {}
        }
        if (sectorDropdown != null) {
            try { sectorDropdown.setValue(SectorPattern.valueOf(state.getString("arrangement.sectorPattern"))); } catch (IllegalArgumentException ignored) {}
        }
        // multiPartToggle is UI-only state, not synced from config
    }
}
