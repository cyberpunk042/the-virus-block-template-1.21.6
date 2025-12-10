package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
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
    private String perfHint = null;
    private int perfColor = GuiConstants.TEXT_SECONDARY;
    private CyclingButtonWidget<String> fragmentDropdown;
    private boolean applyingFragment = false;
    private String currentFragment = "Default";
    
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
    private LabeledSlider ringArcStart;
    private LabeledSlider ringArcEnd;
    private LabeledSlider ringTwist;
    
    // G67: Disc controls
    private LabeledSlider discRadius;
    private LabeledSlider discSegments;
    private LabeledSlider discY;
    private LabeledSlider discInnerRadius;
    private LabeledSlider discArcStart;
    private LabeledSlider discArcEnd;
    private LabeledSlider discRings;
    
    // G68: Prism controls
    private LabeledSlider prismSides;
    private LabeledSlider prismRadius;
    private LabeledSlider prismHeight;
    private LabeledSlider prismTopRadius;
    private LabeledSlider prismTwist;
    private LabeledSlider prismHeightSegments;
    private net.minecraft.client.gui.widget.CheckboxWidget prismCapTop;
    private net.minecraft.client.gui.widget.CheckboxWidget prismCapBottom;
    
    // G69: Cylinder controls
    private LabeledSlider cylinderRadius;
    private LabeledSlider cylinderHeight;
    private LabeledSlider cylinderSegments;
    private LabeledSlider cylinderTopRadius;
    private LabeledSlider cylinderArc;
    private LabeledSlider cylinderHeightSegments;
    private net.minecraft.client.gui.widget.CheckboxWidget cylinderCapTop;
    private net.minecraft.client.gui.widget.CheckboxWidget cylinderCapBottom;
    private net.minecraft.client.gui.widget.CheckboxWidget cylinderOpenEnded;
    
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
    
    public ShapeSubPanel(Screen parent, FieldEditState state, int startY) {
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
        
        if (!shapeType.equals(currentShapeType)) {
            currentShapeType = shapeType;
            currentFragment = "Default";
        }
        
        int x = GuiConstants.PADDING;
        int y = section.getContentY();
        int w = panelWidth - GuiConstants.PADDING * 2;

        // Preset dropdown for this shape
        List<String> presets = FragmentRegistry.listShapeFragments(shapeType);
        if (!presets.contains(currentFragment)) {
            currentFragment = presets.contains("Default") ? "Default" : presets.get(0);
        }
        fragmentDropdown = CyclingButtonWidget.<String>builder(net.minecraft.text.Text::literal)
            .values(presets)
            .initially(currentFragment)
            .build(
                x, y, w, GuiConstants.WIDGET_HEIGHT,
                net.minecraft.text.Text.literal("Preset"),
                (btn, val) -> applyPreset(val)
            );
        shapeWidgets.add(fragmentDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
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
            .position(x, y).width(w).range(4, 256).initial(state.getSphereLatSteps()).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.setSphereLatSteps(Math.round(v))))
            .build();
        shapeWidgets.add(sphereLatSteps);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        sphereLonSteps = LabeledSlider.builder("Lon Steps")
            .position(x, y).width(w).range(4, 256).initial(state.getSphereLonSteps()).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.setSphereLonSteps(Math.round(v))))
            .build();
        shapeWidgets.add(sphereLonSteps);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        sphereLatStart = LabeledSlider.builder("Lat Start")
            .position(x, y).width(w).range(0f, 1f).initial(state.getSphereLatStart()).format("%.2f")
            .onChange(v -> onUserChange(() -> state.setSphereLatStart(v)))
            .build();
        shapeWidgets.add(sphereLatStart);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        sphereLatEnd = LabeledSlider.builder("Lat End")
            .position(x, y).width(w).range(0f, 1f).initial(state.getSphereLatEnd()).format("%.2f")
            .onChange(v -> onUserChange(() -> state.setSphereLatEnd(v)))
            .build();
        shapeWidgets.add(sphereLatEnd);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        sphereAlgorithm = GuiWidgets.enumDropdown(
            x, y, w, "Algorithm", SphereAlgorithm.class, SphereAlgorithm.LAT_LON,
            "Tessellation algorithm", v -> onUserChange(() -> state.setSphereAlgorithm(v.name()))
        );
        try {
            sphereAlgorithm.setValue(SphereAlgorithm.valueOf(state.getSphereAlgorithm()));
        } catch (IllegalArgumentException ignored) {}
        shapeWidgets.add(sphereAlgorithm);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G65-G66: RING CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildRingControls(int x, int y, int w) {
        ringInnerRadius = LabeledSlider.builder("Inner Radius")
            .position(x, y).width(w).range(0.0f, 10.0f).initial(state.getRingInnerRadius()).format("%.2f")
            .onChange(v -> onUserChange(() -> state.setRingInnerRadius(v))).build();
        shapeWidgets.add(ringInnerRadius);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        ringOuterRadius = LabeledSlider.builder("Outer Radius")
            .position(x, y).width(w).range(0.0f, 20.0f).initial(state.getRingOuterRadius()).format("%.2f")
            .onChange(v -> onUserChange(() -> state.setRingOuterRadius(v))).build();
        shapeWidgets.add(ringOuterRadius);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        ringSegments = LabeledSlider.builder("Segments")
            .position(x, y).width(w).range(3, 512).initial(state.getRingSegments()).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.setRingSegments(Math.round(v)))).build();
        shapeWidgets.add(ringSegments);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        ringHeight = LabeledSlider.builder("Height (3D)")
            .position(x, y).width(w).range(0f, 10f).initial(state.getRingHeight()).format("%.2f")
            .onChange(v -> onUserChange(() -> state.setRingHeight(v))).build();
        shapeWidgets.add(ringHeight);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        ringY = LabeledSlider.builder("Y Position")
            .position(x, y).width(w).range(-5f, 5f).initial(state.getRingY()).format("%.2f")
            .onChange(v -> onUserChange(() -> state.setRingY(v))).build();
        shapeWidgets.add(ringY);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        ringArcStart = LabeledSlider.builder("Arc Start")
            .position(x, y).width(w).range(0f, 360f).initial(state.getRingArcStart()).format("%.0f°")
            .onChange(v -> onUserChange(() -> state.setRingArcStart(v))).build();
        shapeWidgets.add(ringArcStart);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        ringArcEnd = LabeledSlider.builder("Arc End")
            .position(x, y).width(w).range(0f, 360f).initial(state.getRingArcEnd()).format("%.0f°")
            .onChange(v -> onUserChange(() -> state.setRingArcEnd(v))).build();
        shapeWidgets.add(ringArcEnd);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        ringTwist = LabeledSlider.builder("Twist")
            .position(x, y).width(w).range(-360f, 360f).initial(state.getRingTwist()).format("%.0f°")
            .onChange(v -> onUserChange(() -> state.setRingTwist(v))).build();
        shapeWidgets.add(ringTwist);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G67: DISC CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildDiscControls(int x, int y, int w) {
        discRadius = LabeledSlider.builder("Radius")
            .position(x, y).width(w).range(0.0f, 20.0f).initial(state.getDiscRadius()).format("%.2f")
            .onChange(v -> onUserChange(() -> state.setDiscRadius(v))).build();
        shapeWidgets.add(discRadius);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        discSegments = LabeledSlider.builder("Segments")
            .position(x, y).width(w).range(3, 512).initial(state.getDiscSegments()).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.setDiscSegments(Math.round(v)))).build();
        shapeWidgets.add(discSegments);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        discY = LabeledSlider.builder("Y Position")
            .position(x, y).width(w).range(-5f, 5f).initial(state.getDiscY()).format("%.2f")
            .onChange(v -> onUserChange(() -> state.setDiscY(v))).build();
        shapeWidgets.add(discY);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        discInnerRadius = LabeledSlider.builder("Inner Radius")
            .position(x, y).width(w).range(0f, 10f).initial(state.getDiscInnerRadius()).format("%.2f")
            .onChange(v -> onUserChange(() -> state.setDiscInnerRadius(v))).build();
        shapeWidgets.add(discInnerRadius);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        discArcStart = LabeledSlider.builder("Arc Start")
            .position(x, y).width(w).range(0f, 360f).initial(state.getDiscArcStart()).format("%.0f°")
            .onChange(v -> onUserChange(() -> state.setDiscArcStart(v))).build();
        shapeWidgets.add(discArcStart);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        discArcEnd = LabeledSlider.builder("Arc End")
            .position(x, y).width(w).range(0f, 360f).initial(state.getDiscArcEnd()).format("%.0f°")
            .onChange(v -> onUserChange(() -> state.setDiscArcEnd(v))).build();
        shapeWidgets.add(discArcEnd);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        discRings = LabeledSlider.builder("Rings")
            .position(x, y).width(w).range(1, 100).initial(state.getDiscRings()).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.setDiscRings(Math.round(v)))).build();
        shapeWidgets.add(discRings);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G68: PRISM CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildPrismControls(int x, int y, int w) {
        prismSides = LabeledSlider.builder("Sides")
            .position(x, y).width(w).range(3, 64).initial(state.getPrismSides()).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.setPrismSides(Math.round(v)))).build();
        shapeWidgets.add(prismSides);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        prismRadius = LabeledSlider.builder("Radius")
            .position(x, y).width(w).range(0.1f, 20.0f).initial(state.getPrismRadius()).format("%.2f")
            .onChange(v -> onUserChange(() -> state.setPrismRadius(v))).build();
        shapeWidgets.add(prismRadius);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        prismHeight = LabeledSlider.builder("Height")
            .position(x, y).width(w).range(0.1f, 20.0f).initial(state.getPrismHeight()).format("%.2f")
            .onChange(v -> onUserChange(() -> state.setPrismHeight(v))).build();
        shapeWidgets.add(prismHeight);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        prismTopRadius = LabeledSlider.builder("Top Radius")
            .position(x, y).width(w).range(0.0f, 20.0f).initial(state.getPrismTopRadius()).format("%.2f")
            .onChange(v -> onUserChange(() -> state.setPrismTopRadius(v))).build();
        shapeWidgets.add(prismTopRadius);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        prismTwist = LabeledSlider.builder("Twist")
            .position(x, y).width(w).range(-360f, 360f).initial(state.getPrismTwist()).format("%.0f°")
            .onChange(v -> onUserChange(() -> state.setPrismTwist(v))).build();
        shapeWidgets.add(prismTwist);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        prismHeightSegments = LabeledSlider.builder("Height Segs")
            .position(x, y).width(w).range(1, 100).initial(state.getPrismHeightSegments()).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.setPrismHeightSegments(Math.round(v)))).build();
        shapeWidgets.add(prismHeightSegments);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        int halfW = (w - GuiConstants.PADDING) / 2;
        var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        
        prismCapTop = GuiWidgets.checkbox(x, y, "Cap Top", state.isPrismCapTop(), "Render top cap",
            textRenderer, v -> onUserChange(() -> state.setPrismCapTop(v)));
        shapeWidgets.add(prismCapTop);
        
        prismCapBottom = GuiWidgets.checkbox(x + halfW + GuiConstants.PADDING, y, "Cap Bottom", state.isPrismCapBottom(), "Render bottom cap",
            textRenderer, v -> onUserChange(() -> state.setPrismCapBottom(v)));
        shapeWidgets.add(prismCapBottom);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G69: CYLINDER CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildCylinderControls(int x, int y, int w) {
        cylinderRadius = LabeledSlider.builder("Radius")
            .position(x, y).width(w).range(0.1f, 20.0f).initial(state.getCylinderRadius()).format("%.2f")
            .onChange(v -> onUserChange(() -> state.setCylinderRadius(v))).build();
        shapeWidgets.add(cylinderRadius);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        cylinderHeight = LabeledSlider.builder("Height")
            .position(x, y).width(w).range(0.1f, 256f).initial(state.getCylinderHeight()).format("%.2f")
            .onChange(v -> onUserChange(() -> state.setCylinderHeight(v))).build();
        shapeWidgets.add(cylinderHeight);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        cylinderSegments = LabeledSlider.builder("Segments")
            .position(x, y).width(w).range(3, 256).initial(state.getCylinderSegments()).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.setCylinderSegments(Math.round(v)))).build();
        shapeWidgets.add(cylinderSegments);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        cylinderTopRadius = LabeledSlider.builder("Top Radius")
            .position(x, y).width(w).range(0f, 20f).initial(state.getCylinderTopRadius()).format("%.2f")
            .onChange(v -> onUserChange(() -> state.setCylinderTopRadius(v))).build();
        shapeWidgets.add(cylinderTopRadius);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        cylinderArc = LabeledSlider.builder("Arc")
            .position(x, y).width(w).range(0f, 360f).initial(state.getCylinderArc()).format("%.0f°")
            .onChange(v -> onUserChange(() -> state.setCylinderArc(v))).build();
        shapeWidgets.add(cylinderArc);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        cylinderHeightSegments = LabeledSlider.builder("Height Segs")
            .position(x, y).width(w).range(1, 100).initial(state.getCylinderHeightSegments()).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.setCylinderHeightSegments(Math.round(v)))).build();
        shapeWidgets.add(cylinderHeightSegments);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        int halfW = (w - GuiConstants.PADDING) / 2;
        var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        
        cylinderCapTop = GuiWidgets.checkbox(x, y, "Cap Top", state.isCylinderCapTop(), "Render top cap",
            textRenderer, v -> onUserChange(() -> state.setCylinderCapTop(v)));
        shapeWidgets.add(cylinderCapTop);
        
        cylinderCapBottom = GuiWidgets.checkbox(x + halfW + GuiConstants.PADDING, y, "Cap Bottom", state.isCylinderCapBottom(), "Render bottom cap",
            textRenderer, v -> onUserChange(() -> state.setCylinderCapBottom(v)));
        shapeWidgets.add(cylinderCapBottom);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        cylinderOpenEnded = GuiWidgets.checkbox(x, y, "Open Ended (Tube)", state.isCylinderOpenEnded(), "No caps (tube mode)",
            textRenderer, v -> onUserChange(() -> state.setCylinderOpenEnded(v)));
        shapeWidgets.add(cylinderOpenEnded);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G70: POLYHEDRON CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildPolyhedronControls(int x, int y, int w) {
        polyType = GuiWidgets.enumDropdown(
            x, y, w, "Type", PolyType.class, PolyType.CUBE,
            "Polyhedron type", v -> onUserChange(() -> state.setPolyType(v.name()))
        );
        try {
            polyType.setValue(PolyType.valueOf(state.getPolyType()));
        } catch (IllegalArgumentException ignored) {}
        shapeWidgets.add(polyType);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        polyRadius = LabeledSlider.builder("Size")
            .position(x, y).width(w).range(0.5f, 10f).initial(state.getPolyRadius()).format("%.1f")
            .onChange(v -> onUserChange(() -> state.setPolyRadius(v))).build();
        shapeWidgets.add(polyRadius);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        polySubdivisions = LabeledSlider.builder("Subdivisions")
            .position(x, y).width(w).range(0, 4).initial(state.getPolySubdivisions()).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.setPolySubdivisions(Math.round(v)))).build();
        shapeWidgets.add(polySubdivisions);
    }
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render section header
        section.render(context, net.minecraft.client.MinecraftClient.getInstance().textRenderer, mouseX, mouseY, delta);
        
        // Performance hint (draw just under header if present)
        computePerformanceHint();
        if (perfHint != null && section.isExpanded()) {
            var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            int hintY = section.getContentY() - GuiConstants.PADDING;
            context.drawText(tr, perfHint, GuiConstants.PADDING + 2, hintY, perfColor, false);
        }
        
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

    private void onUserChange(Runnable r) {
        r.run();
        if (!applyingFragment) {
            currentFragment = "Custom";
            if (fragmentDropdown != null && FragmentRegistry.listShapeFragments(currentShapeType).contains("Custom")) {
                fragmentDropdown.setValue("Custom");
            }
        }
    }

    private void applyPreset(String name) {
        if ("Custom".equalsIgnoreCase(name)) {
            currentFragment = "Custom";
            return;
        }
        applyingFragment = true;
        currentFragment = name;
        FragmentRegistry.applyShapeFragment(state, currentShapeType, name);
        syncSlidersFromState();
        applyingFragment = false;
    }

    private void syncSlidersFromState() {
        if (sphereLatSteps != null) sphereLatSteps.setValue(state.getSphereLatSteps());
        if (sphereLonSteps != null) sphereLonSteps.setValue(state.getSphereLonSteps());
        if (sphereLatStart != null) sphereLatStart.setValue(state.getSphereLatStart());
        if (sphereLatEnd != null) sphereLatEnd.setValue(state.getSphereLatEnd());
        if (sphereAlgorithm != null) {
            try { sphereAlgorithm.setValue(SphereAlgorithm.valueOf(state.getSphereAlgorithm())); } catch (IllegalArgumentException ignored) {}
        }
        if (ringInnerRadius != null) ringInnerRadius.setValue(state.getRingInnerRadius());
        if (ringOuterRadius != null) ringOuterRadius.setValue(state.getRingOuterRadius());
        if (ringSegments != null) ringSegments.setValue(state.getRingSegments());
        if (ringHeight != null) ringHeight.setValue(state.getRingHeight());
        if (ringY != null) ringY.setValue(state.getRingY());
        if (discRadius != null) discRadius.setValue(state.getDiscRadius());
        if (discSegments != null) discSegments.setValue(state.getDiscSegments());
        if (discY != null) discY.setValue(state.getDiscY());
        if (discInnerRadius != null) discInnerRadius.setValue(state.getDiscInnerRadius());
        if (prismSides != null) prismSides.setValue(state.getPrismSides());
        if (prismRadius != null) prismRadius.setValue(state.getPrismRadius());
        if (prismHeight != null) prismHeight.setValue(state.getPrismHeight());
        if (prismTopRadius != null) prismTopRadius.setValue(state.getPrismTopRadius());
        if (cylinderRadius != null) cylinderRadius.setValue(state.getCylinderRadius());
        if (cylinderHeight != null) cylinderHeight.setValue(state.getCylinderHeight());
        if (cylinderSegments != null) cylinderSegments.setValue(state.getCylinderSegments());
        if (cylinderTopRadius != null) cylinderTopRadius.setValue(state.getCylinderTopRadius());
        if (polyType != null) {
            try { polyType.setValue(PolyType.valueOf(state.getPolyType())); } catch (IllegalArgumentException ignored) {}
        }
        if (polyRadius != null) polyRadius.setValue(state.getPolyRadius());
        if (polySubdivisions != null) polySubdivisions.setValue(state.getPolySubdivisions());
    }

    private void computePerformanceHint() {
        String shape = state.getShapeType().toLowerCase();
        switch (shape) {
            case "sphere" -> {
                int lat = state.getSphereLatSteps();
                int lon = state.getSphereLonSteps();
                int tess = lat * lon;
                setHintForTessellation(tess, 640, 1280, "Sphere tessellation ~" + tess + " (lat×lon)");
            }
            case "ring" -> {
                int seg = state.getRingSegments();
                setHintForTessellation(seg, 256, 512, "Ring segments ~" + seg);
            }
            case "disc" -> {
                int seg = state.getDiscSegments();
                setHintForTessellation(seg, 256, 512, "Disc segments ~" + seg);
            }
            case "prism" -> {
                int seg = state.getPrismSides();
                setHintForTessellation(seg, 64, 128, "Prism sides ~" + seg);
            }
            case "cylinder", "beam" -> {
                int seg = state.getCylinderSegments();
                setHintForTessellation(seg, 128, 256, "Cylinder segments ~" + seg);
            }
            case "cube", "octahedron", "icosahedron" -> {
                int sub = state.getPolySubdivisions();
                // crude estimate: base faces 6/8/20 times 4^sub
                int base = shape.equals("cube") ? 6 : (shape.equals("octahedron") ? 8 : 20);
                int tess = (int) (base * Math.pow(4, sub));
                setHintForTessellation(tess, 200, 800, "Poly faces ~" + tess);
            }
            default -> {
                perfHint = null;
            }
        }
    }

    private void setHintForTessellation(int value, int warn, int high, String label) {
        if (value >= high) {
            perfHint = "⚠ High complexity: " + label;
            perfColor = GuiConstants.ERROR;
        } else if (value >= warn) {
            perfHint = "⚠ Medium complexity: " + label;
            perfColor = GuiConstants.WARNING;
        } else {
            perfHint = null;
        }
    }
}
