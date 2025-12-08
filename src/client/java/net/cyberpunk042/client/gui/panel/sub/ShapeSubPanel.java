package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.GuiState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ExpandableSection;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * G62-G70: Shape-specific parameter controls.
 * 
 * <p>Dynamically shows controls based on the current shape type:</p>
 * <ul>
 *   <li>G63-G64: Sphere - latSteps, lonSteps, latStart/End, algorithm</li>
 *   <li>G65-G66: Ring - inner/outer radius, segments, height, y</li>
 *   <li>G67: Disc - radius, segments, y, innerRadius</li>
 *   <li>G68: Prism - sides, radius, height, topRadius</li>
 *   <li>G69: Cylinder - radius, height, segments, topRadius</li>
 *   <li>G70: Polyhedron - type, radius, subdivisions</li>
 * </ul>
 */
public class ShapeSubPanel extends AbstractPanel {
    
    private ExpandableSection section;
    private int startY;
    
    // Common shape widgets (rebuilt per shape type)
    private final List<net.minecraft.client.gui.widget.ClickableWidget> shapeWidgets = new ArrayList<>();
    
    // Current shape state
    private String currentShapeType = "";
    
    // G63-G64: Sphere controls
    private LabeledSlider sphereLatSteps;
    private LabeledSlider sphereLonSteps;
    private LabeledSlider sphereLatStart;
    private LabeledSlider sphereLatEnd;
    private CyclingButtonWidget<SphereAlgorithm> sphereAlgorithm;
    
    // G65-G66: Ring controls
    private LabeledSlider ringInnerRadius;
    private LabeledSlider ringOuterRadius;
    private LabeledSlider ringSegments;
    private LabeledSlider ringHeight;
    private LabeledSlider ringY;
    
    // G67: Disc controls
    private LabeledSlider discRadius;
    private LabeledSlider discSegments;
    private LabeledSlider discY;
    private LabeledSlider discInnerRadius;
    
    // G68: Prism controls
    private LabeledSlider prismSides;
    private LabeledSlider prismRadius;
    private LabeledSlider prismHeight;
    private LabeledSlider prismTopRadius;
    
    // G69: Cylinder controls
    private LabeledSlider cylinderRadius;
    private LabeledSlider cylinderHeight;
    private LabeledSlider cylinderSegments;
    private LabeledSlider cylinderTopRadius;
    
    // G70: Polyhedron controls
    private CyclingButtonWidget<PolyType> polyType;
    private LabeledSlider polyRadius;
    private LabeledSlider polySubdivisions;
    
    /** Sphere tessellation algorithms. */
    public enum SphereAlgorithm {
        LAT_LON("Lat/Lon"),
        UV_SPHERE("UV Sphere"),
        ICO_SPHERE("Icosphere");
        
        private final String label;
        SphereAlgorithm(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    
    /** Polyhedron base types. */
    public enum PolyType {
        CUBE("Cube"),
        OCTAHEDRON("Octahedron"),
        ICOSAHEDRON("Icosahedron"),
        DODECAHEDRON("Dodecahedron");
        
        private final String label;
        PolyType(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    
    public ShapeSubPanel(Screen parent, GuiState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("ShapeSubPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        
        // Expandable section header
        section = new ExpandableSection(
            GuiConstants.PADDING, startY, 
            width - GuiConstants.PADDING * 2,
            "Shape Parameters", true // Start expanded
        );
        
        rebuildForCurrentShape();
        
        Logging.GUI.topic("panel").debug("ShapeSubPanel initialized for shape: {}", state.getShapeType());
    }
    
    /**
     * Rebuilds controls for the current shape type.
     */
    public void rebuildForCurrentShape() {
        shapeWidgets.clear();
        String shapeType = state.getShapeType();
        
        if (shapeType.equals(currentShapeType)) return;
        currentShapeType = shapeType;
        
        int x = GuiConstants.PADDING;
        int y = section.getContentY();
        int w = panelWidth - GuiConstants.PADDING * 2;
        
        switch (shapeType.toLowerCase()) {
            case "sphere" -> buildSphereControls(x, y, w);
            case "ring" -> buildRingControls(x, y, w);
            case "disc" -> buildDiscControls(x, y, w);
            case "prism" -> buildPrismControls(x, y, w);
            case "cylinder", "beam" -> buildCylinderControls(x, y, w);
            case "cube", "octahedron", "icosahedron" -> buildPolyhedronControls(x, y, w);
            default -> Logging.GUI.topic("panel").warn("Unknown shape type: {}", shapeType);
        }
        
        // Update section content height
        int controlsHeight = shapeWidgets.size() * (GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING);
        section.setContentHeight(controlsHeight);
        
        Logging.GUI.topic("panel").debug("Built {} controls for shape: {}", shapeWidgets.size(), shapeType);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G63-G64: SPHERE CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildSphereControls(int x, int y, int w) {
        sphereLatSteps = LabeledSlider.builder("Lat Steps")
            .position(x, y).width(w).range(4, 64).initial(32).format("%d").step(1)
            .onChange(v -> { /* TODO: Update shape */ })
            .build();
        shapeWidgets.add(sphereLatSteps);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        sphereLonSteps = LabeledSlider.builder("Lon Steps")
            .position(x, y).width(w).range(4, 128).initial(64).format("%d").step(1)
            .onChange(v -> { /* TODO: Update shape */ })
            .build();
        shapeWidgets.add(sphereLonSteps);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        sphereLatStart = LabeledSlider.builder("Lat Start")
            .position(x, y).width(w).range(0f, 1f).initial(0f).format("%.2f")
            .onChange(v -> { /* TODO: Update shape */ })
            .build();
        shapeWidgets.add(sphereLatStart);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        sphereLatEnd = LabeledSlider.builder("Lat End")
            .position(x, y).width(w).range(0f, 1f).initial(1f).format("%.2f")
            .onChange(v -> { /* TODO: Update shape */ })
            .build();
        shapeWidgets.add(sphereLatEnd);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        sphereAlgorithm = GuiWidgets.enumDropdown(
            x, y, w, "Algorithm", SphereAlgorithm.class, SphereAlgorithm.LAT_LON,
            "Tessellation algorithm", v -> { /* TODO: Update shape */ }
        );
        shapeWidgets.add(sphereAlgorithm);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G65-G66: RING CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildRingControls(int x, int y, int w) {
        ringInnerRadius = LabeledSlider.builder("Inner Radius")
            .position(x, y).width(w).range(0.1f, 9f).initial(2f).format("%.1f")
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(ringInnerRadius);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        ringOuterRadius = LabeledSlider.builder("Outer Radius")
            .position(x, y).width(w).range(0.5f, 10f).initial(3f).format("%.1f")
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(ringOuterRadius);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        ringSegments = LabeledSlider.builder("Segments")
            .position(x, y).width(w).range(8, 128).initial(48).format("%d").step(1)
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(ringSegments);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        ringHeight = LabeledSlider.builder("Height (3D)")
            .position(x, y).width(w).range(0f, 2f).initial(0.1f).format("%.2f")
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(ringHeight);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        ringY = LabeledSlider.builder("Y Position")
            .position(x, y).width(w).range(-2f, 2f).initial(0f).format("%.1f")
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(ringY);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G67: DISC CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildDiscControls(int x, int y, int w) {
        discRadius = LabeledSlider.builder("Radius")
            .position(x, y).width(w).range(0.5f, 10f).initial(3f).format("%.1f")
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(discRadius);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        discSegments = LabeledSlider.builder("Segments")
            .position(x, y).width(w).range(8, 128).initial(48).format("%d").step(1)
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(discSegments);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        discY = LabeledSlider.builder("Y Position")
            .position(x, y).width(w).range(-2f, 2f).initial(0f).format("%.1f")
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(discY);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        discInnerRadius = LabeledSlider.builder("Inner Radius")
            .position(x, y).width(w).range(0f, 5f).initial(0f).format("%.1f")
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(discInnerRadius);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G68: PRISM CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildPrismControls(int x, int y, int w) {
        prismSides = LabeledSlider.builder("Sides")
            .position(x, y).width(w).range(3, 12).initial(6).format("%d").step(1)
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(prismSides);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        prismRadius = LabeledSlider.builder("Radius")
            .position(x, y).width(w).range(0.5f, 10f).initial(3f).format("%.1f")
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(prismRadius);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        prismHeight = LabeledSlider.builder("Height")
            .position(x, y).width(w).range(0.5f, 10f).initial(2f).format("%.1f")
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(prismHeight);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        prismTopRadius = LabeledSlider.builder("Top Radius")
            .position(x, y).width(w).range(0f, 10f).initial(3f).format("%.1f")
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(prismTopRadius);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G69: CYLINDER CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildCylinderControls(int x, int y, int w) {
        cylinderRadius = LabeledSlider.builder("Radius")
            .position(x, y).width(w).range(0.1f, 10f).initial(1f).format("%.1f")
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(cylinderRadius);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        cylinderHeight = LabeledSlider.builder("Height")
            .position(x, y).width(w).range(0.5f, 256f).initial(2f).format("%.1f")
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(cylinderHeight);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        cylinderSegments = LabeledSlider.builder("Segments")
            .position(x, y).width(w).range(4, 64).initial(16).format("%d").step(1)
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(cylinderSegments);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        cylinderTopRadius = LabeledSlider.builder("Top Radius")
            .position(x, y).width(w).range(0f, 10f).initial(1f).format("%.1f")
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(cylinderTopRadius);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G70: POLYHEDRON CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildPolyhedronControls(int x, int y, int w) {
        polyType = GuiWidgets.enumDropdown(
            x, y, w, "Type", PolyType.class, PolyType.CUBE,
            "Polyhedron type", v -> { /* TODO */ }
        );
        shapeWidgets.add(polyType);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        polyRadius = LabeledSlider.builder("Size")
            .position(x, y).width(w).range(0.5f, 10f).initial(1f).format("%.1f")
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(polyRadius);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        polySubdivisions = LabeledSlider.builder("Subdivisions")
            .position(x, y).width(w).range(0, 4).initial(0).format("%d").step(1)
            .onChange(v -> { /* TODO */ }).build();
        shapeWidgets.add(polySubdivisions);
    }
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render section header
        section.render(context, net.minecraft.client.MinecraftClient.getInstance().textRenderer, mouseX, mouseY, delta);
        
        // Render shape controls if expanded
        if (section.isExpanded()) {
            for (var widget : shapeWidgets) {
                widget.render(context, mouseX, mouseY, delta);
            }
        }
    }
    
    public int getHeight() {
        return section.getTotalHeight();
    }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> all = new ArrayList<>();
        all.add(section.getHeaderButton());
        all.addAll(shapeWidgets);
        return all;
    }
}
