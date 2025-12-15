package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.shape.PolyType;
import net.cyberpunk042.visual.shape.SphereAlgorithm;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.util.ArrayList;
import java.util.List;

import net.cyberpunk042.visual.pattern.ArrangementConfig;

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
    
    private static final int TITLE_HEIGHT = 16;  // Compact title bar
    private int startY;
    
    // Shape widgets are stored in inherited 'widgets' list from AbstractPanel
    
    // Current shape state
    private String currentShapeType = "";
    
    // Warning callback - called when performance hint changes
    private java.util.function.BiConsumer<String, Integer> warningCallback;
    // Shape change callback - called when shape type changes and widgets are rebuilt
    private Runnable shapeChangedCallback;
    private CyclingButtonWidget<String> fragmentDropdown;
    private boolean applyingFragment = false;
    private String currentFragment = "Default";
    
    // G63-G64: Sphere controls
    private LabeledSlider sphereRadius;
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
    // open-ended is derived: capTop=false && capBottom=false
    private net.minecraft.client.gui.widget.CheckboxWidget cylinderOpenEnded;
    
    // G70: Polyhedron controls (Type is determined by shape selection, not dropdown)
    private LabeledSlider polyRadius;
    private LabeledSlider polySubdivisions;
    
    // Pattern controls (for face pattern selection)
    private CyclingButtonWidget<String> patternFaces;
    private CyclingButtonWidget<String> patternBody;
    private CyclingButtonWidget<String> patternTop;
    private CyclingButtonWidget<String> patternBottom;
    
    // Available pattern options - includes patterns from ALL CellTypes:
    // SEGMENT (rings): full, alternating, sparse, quarter
    // SECTOR (disc, caps): full, half, quarters, pinwheel
    // TRIANGLE (polyhedra): full, alternating, sparse
    // QUAD (spheres, cylinder sides): filled_1, triangle_1, tooth_1
    private static final List<String> PATTERN_OPTIONS = List.of(
        "full",         // Universal: SEGMENT, SECTOR, TRIANGLE
        "alternating",  // SEGMENT, TRIANGLE
        "sparse",       // SEGMENT, TRIANGLE
        "half",         // SECTOR (disc, caps): every other
        "pinwheel",     // SECTOR: alternating pattern
        "filled_1",     // QUAD: standard filled
        "triangle_1",   // QUAD: triangle variation
        "tooth_1"       // QUAD: tooth/sawtooth
    );
    
    // Shape type selector
    private CyclingButtonWidget<ShapeKind> shapeTypeDropdown;
    
    /**
     * Available shape types for the field primitive.
     */
    public enum ShapeKind {
        SPHERE("sphere", "Sphere"),
        RING("ring", "Ring"),
        DISC("disc", "Disc"),
        PRISM("prism", "Prism"),
        CYLINDER("cylinder", "Cylinder"),
        CUBE("cube", "Cube"),
        TETRAHEDRON("tetrahedron", "Tetrahedron"),
        OCTAHEDRON("octahedron", "Octahedron"),
        DODECAHEDRON("dodecahedron", "Dodecahedron"),
        ICOSAHEDRON("icosahedron", "Icosahedron"),
        TORUS("torus", "Torus"),
        CAPSULE("capsule", "Capsule"),
        CONE("cone", "Cone");
        
        private final String id;
        private final String label;
        
        ShapeKind(String id, String label) {
            this.id = id;
            this.label = label;
        }
        
        public String getId() { return id; }
        @Override public String toString() { return label; }
        
        public static ShapeKind fromId(String id) {
            for (ShapeKind k : values()) {
                if (k.id.equalsIgnoreCase(id)) return k;
            }
            return SPHERE;  // Default
        }
    }
    
    public ShapeSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("ShapeSubPanel created");
    }
    
    /**
     * Sets a callback for when the performance warning changes.
     * Callback receives (warningText, color) or (null, 0) to clear.
     */
    public void setWarningCallback(java.util.function.BiConsumer<String, Integer> callback) {
        this.warningCallback = callback;
    }
    
    /**
     * Sets a callback for when the shape type changes and widgets are rebuilt.
     * The screen should use this to re-register widgets.
     */
    public void setShapeChangedCallback(Runnable callback) {
        this.shapeChangedCallback = callback;
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        
        rebuildForCurrentShape();
        
        Logging.GUI.topic("panel").debug("ShapeSubPanel initialized for shape: {}", state.getString("shapeType"));
    }
    
    /**
     * Rebuilds controls for the current shape type.
     */
    public void rebuildForCurrentShape() {
        // Remember if we need to reapply bounds offset after rebuild
        boolean needsOffset = bounds != null && !bounds.isEmpty();
        
        widgets.clear();
        
        String shapeType = state.getString("shapeType");
        
        if (!shapeType.equals(currentShapeType)) {
            currentShapeType = shapeType;
            currentFragment = "Default";
        }
        
        int x = GuiConstants.PADDING;
        int y = startY + TITLE_HEIGHT;  // Start after title bar
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;

        // Shape TYPE selector (left half)
        ShapeKind currentKind = ShapeKind.fromId(shapeType);
        shapeTypeDropdown = CyclingButtonWidget.<ShapeKind>builder(k -> net.minecraft.text.Text.literal(k.toString()))
            .values(ShapeKind.values())
            .initially(currentKind)
            .build(x, y, halfW, GuiConstants.COMPACT_HEIGHT, net.minecraft.text.Text.literal("Shape"),
                (btn, val) -> {
                    state.set("shapeType", val.getId());
                    rebuildForCurrentShape();  // Rebuild with new shape type
                    // Notify screen to re-register widgets
                    if (shapeChangedCallback != null) {
                        shapeChangedCallback.run();
                    }
                });
        widgets.add(shapeTypeDropdown);

        // Preset dropdown for this shape (right half)
        // Show "Custom" since we're loading existing primitive values
        List<String> presets = FragmentRegistry.listShapeFragments(shapeType);
        currentFragment = "Custom";  // Loaded primitives have custom values
        fragmentDropdown = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal(v))
            .values(presets)
            .initially(currentFragment)
            .build(x + halfW + GuiConstants.PADDING, y, halfW, GuiConstants.COMPACT_HEIGHT, net.minecraft.text.Text.literal("Variant"),
                (btn, val) -> applyPreset(val));
        widgets.add(fragmentDropdown);
        y += GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        
        // ═══════════════════════════════════════════════════════════════════════
        // PATTERN SECTION - Per-part face patterns
        // ═══════════════════════════════════════════════════════════════════════
        y = buildPatternControls(x, y, w, shapeType);
        
        switch (shapeType.toLowerCase()) {
            case "sphere" -> buildSphereControls(x, y, w);
            case "ring" -> buildRingControls(x, y, w);
            case "disc" -> buildDiscControls(x, y, w);
            case "prism" -> buildPrismControls(x, y, w);
            case "cylinder", "beam" -> buildCylinderControls(x, y, w);
            case "cube", "tetrahedron", "octahedron", "dodecahedron", "icosahedron" -> buildPolyhedronControls(x, y, w);
            case "torus" -> buildTorusControls(x, y, w);
            case "capsule" -> buildCapsuleControls(x, y, w);
            case "cone" -> buildConeControls(x, y, w);
            default -> Logging.GUI.topic("panel").warn("Unknown shape type: {}", shapeType);
        }
        
        // Track content height based on final y
        contentHeight = y - startY;
        
        // Compute and send warning to callback
        computePerformanceHint();
        
        // If bounds are already set (rebuild scenario), reapply offset to position widgets correctly
        if (needsOffset) {
            applyBoundsOffset();
        }
        
        Logging.GUI.topic("panel").debug("Built {} controls for shape: {}", widgets.size(), shapeType);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G63-G64: SPHERE CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildSphereControls(int x, int y, int w) {
        int step = GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        int halfW = (w - GuiConstants.COMPACT_GAP) / 2;
        
        // Row 1: Radius + Algorithm dropdown
        sphereRadius = LabeledSlider.builder("Radius")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0.1f, 20.0f).initial(state.getFloat("sphere.radius")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("sphere.radius", v)))
            .build();
        widgets.add(sphereRadius);
        
        sphereAlgorithm = GuiWidgets.enumDropdown(
            x + halfW + GuiConstants.COMPACT_GAP, y, halfW, GuiConstants.COMPACT_HEIGHT, "Algo", SphereAlgorithm.class, SphereAlgorithm.LAT_LON,
            "Tessellation algorithm", v -> onUserChange(() -> state.set("sphere.algorithm", v))
        );
        try {
            sphereAlgorithm.setValue(SphereAlgorithm.valueOf(state.getString("sphere.algorithm")));
        } catch (IllegalArgumentException ignored) {}
        widgets.add(sphereAlgorithm);
        y += step;
        
        // Row 2: Lat Steps + Lon Steps
        sphereLatSteps = LabeledSlider.builder("Lat")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(4, 256).initial(state.getInt("sphere.latSteps")).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.set("sphere.latSteps", Math.round(v))))
            .build();
        widgets.add(sphereLatSteps);
        
        sphereLonSteps = LabeledSlider.builder("Lon")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(4, 256).initial(state.getInt("sphere.lonSteps")).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.set("sphere.lonSteps", Math.round(v))))
            .build();
        widgets.add(sphereLonSteps);
        y += step;
        
        // Row 3: Lat Start + Lat End
        sphereLatStart = LabeledSlider.builder("Start")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0f, 1f).initial(state.getFloat("sphere.latStart")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("sphere.latStart", v)))
            .build();
        widgets.add(sphereLatStart);
        
        sphereLatEnd = LabeledSlider.builder("End")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0f, 1f).initial(state.getFloat("sphere.latEnd")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("sphere.latEnd", v)))
            .build();
        widgets.add(sphereLatEnd);
        y += step;
        
        // Row 4: Quad Pattern (cell-level arrangement for sphere quads)
        // Convert enum name to pattern name (e.g., FILLED -> "filled_1", TRIANGLE_1 -> "triangle_1")
        var quadPatternDropdown = GuiWidgets.enumDropdown(
            x, y, w, GuiConstants.COMPACT_HEIGHT, "Quad Pattern", 
            ArrangementSubPanel.QuadPattern.class, ArrangementSubPanel.QuadPattern.FILLED,
            "Cell-level pattern for quad tessellation", 
            v -> onUserChange(() -> {
                // Convert enum to pattern name (e.g., FILLED -> "filled_1", TRIANGLE_1 -> "triangle_1")
                String patternName = v.name().toLowerCase() + "_1";
                if (patternName.equals("filled_1")) patternName = "filled_1";  // already correct
                state.set("arrangement.defaultPattern", patternName);
            })
        );
        widgets.add(quadPatternDropdown);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G65-G66: RING CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildRingControls(int x, int y, int w) {
        int step = GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        int halfW = (w - GuiConstants.COMPACT_GAP) / 2;
        
        // Row 1: Inner R + Outer R
        ringInnerRadius = LabeledSlider.builder("Inner R")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0.0f, 10.0f).initial(state.getFloat("ring.innerRadius")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("ring.innerRadius", v))).build();
        widgets.add(ringInnerRadius);
        
        ringOuterRadius = LabeledSlider.builder("Outer R")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0.0f, 20.0f).initial(state.getFloat("ring.outerRadius")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("ring.outerRadius", v))).build();
        widgets.add(ringOuterRadius);
        y += step;
        
        // Row 2: Segments + Height
        ringSegments = LabeledSlider.builder("Segs")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(3, 512).initial(state.getInt("ring.segments")).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.set("ring.segments", Math.round(v)))).build();
        widgets.add(ringSegments);
        
        ringHeight = LabeledSlider.builder("Height")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0f, 10f).initial(state.getFloat("ring.height")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("ring.height", v))).build();
        widgets.add(ringHeight);
        y += step;
        
        // Row 3: Y Pos + Twist
        ringY = LabeledSlider.builder("Y Pos")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(-5f, 5f).initial(state.getFloat("ring.y")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("ring.y", v))).build();
        widgets.add(ringY);
        
        ringTwist = LabeledSlider.builder("Twist")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(-360f, 360f).initial(state.getFloat("ring.twist")).format("%.0f°")
            .onChange(v -> onUserChange(() -> state.set("ring.twist", v))).build();
        widgets.add(ringTwist);
        y += step;
        
        // Row 4: Arc Start + Arc End
        ringArcStart = LabeledSlider.builder("Arc St")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0f, 360f).initial(state.getFloat("ring.arcStart")).format("%.0f°")
            .onChange(v -> onUserChange(() -> state.set("ring.arcStart", v))).build();
        widgets.add(ringArcStart);
        
        ringArcEnd = LabeledSlider.builder("Arc End")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0f, 360f).initial(state.getFloat("ring.arcEnd")).format("%.0f°")
            .onChange(v -> onUserChange(() -> state.set("ring.arcEnd", v))).build();
        widgets.add(ringArcEnd);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G67: DISC CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildDiscControls(int x, int y, int w) {
        int step = GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        int halfW = (w - GuiConstants.COMPACT_GAP) / 2;

        // Row 1: Radius + Inner Radius
        discRadius = LabeledSlider.builder("Radius")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0.0f, 20.0f).initial(state.getFloat("disc.radius")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("disc.radius", v))).build();
        widgets.add(discRadius);
        
        discInnerRadius = LabeledSlider.builder("Inner R")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0f, 10f).initial(state.getFloat("disc.innerRadius")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("disc.innerRadius", v))).build();
        widgets.add(discInnerRadius);
        y += step;
        
        // Row 2: Segments + Rings
        discSegments = LabeledSlider.builder("Segs")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(3, 512).initial(state.getInt("disc.segments")).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.set("disc.segments", Math.round(v)))).build();
        widgets.add(discSegments);
        
        discRings = LabeledSlider.builder("Rings")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(1, 100).initial(state.getInt("disc.rings")).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.set("disc.rings", Math.round(v)))).build();
        widgets.add(discRings);
        y += step;
        
        // Row 3: Y Position (full width doesn't have pair)
        discY = LabeledSlider.builder("Y Pos")
            .position(x, y).width(w).height(GuiConstants.COMPACT_HEIGHT).range(-5f, 5f).initial(state.getFloat("disc.y")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("disc.y", v))).build();
        widgets.add(discY);
        y += step;
        
        // Row 4: Arc Start + Arc End
        discArcStart = LabeledSlider.builder("Arc St")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0f, 360f).initial(state.getFloat("disc.arcStart")).format("%.0f°")
            .onChange(v -> onUserChange(() -> state.set("disc.arcStart", v))).build();
        widgets.add(discArcStart);
        
        discArcEnd = LabeledSlider.builder("Arc End")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0f, 360f).initial(state.getFloat("disc.arcEnd")).format("%.0f°")
            .onChange(v -> onUserChange(() -> state.set("disc.arcEnd", v))).build();
        widgets.add(discArcEnd);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G68: PRISM CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildPrismControls(int x, int y, int w) {
        int step = GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        int halfW = (w - GuiConstants.COMPACT_GAP) / 2;

        // Row 1: Sides + Radius
        prismSides = LabeledSlider.builder("Sides")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(3, 64).initial(state.getInt("prism.sides")).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.set("prism.sides", Math.round(v)))).build();
        widgets.add(prismSides);
        
        prismRadius = LabeledSlider.builder("Radius")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0.1f, 20.0f).initial(state.getFloat("prism.radius")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("prism.radius", v))).build();
        widgets.add(prismRadius);
        y += step;
        
        // Row 2: Height + Top Radius
        prismHeight = LabeledSlider.builder("Height")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0.1f, 20.0f).initial(state.getFloat("prism.height")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("prism.height", v))).build();
        widgets.add(prismHeight);
        
        prismTopRadius = LabeledSlider.builder("Top R")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0.0f, 20.0f).initial(state.getFloat("prism.topRadius")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("prism.topRadius", v))).build();
        widgets.add(prismTopRadius);
        y += step;
        
        // Row 3: Twist + Height Segments
        prismTwist = LabeledSlider.builder("Twist")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(-360f, 360f).initial(state.getFloat("prism.twist")).format("%.0f°")
            .onChange(v -> onUserChange(() -> state.set("prism.twist", v))).build();
        widgets.add(prismTwist);
        
        prismHeightSegments = LabeledSlider.builder("H Segs")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(1, 100).initial(state.getInt("prism.heightSegments")).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.set("prism.heightSegments", Math.round(v)))).build();
        widgets.add(prismHeightSegments);
        y += step;
        
        // Row 4: Cap Top + Cap Bottom
        var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        
        prismCapTop = GuiWidgets.checkbox(x, y, "Cap Top", state.getBool("prism.capTop"), "Render top cap",
            textRenderer, v -> onUserChange(() -> state.set("prism.capTop", v)));
        widgets.add(prismCapTop);
        
        prismCapBottom = GuiWidgets.checkbox(x + halfW + GuiConstants.COMPACT_GAP, y, "Cap Bottom", state.getBool("prism.capBottom"), "Render bottom cap",
            textRenderer, v -> onUserChange(() -> state.set("prism.capBottom", v)));
        widgets.add(prismCapBottom);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G69: CYLINDER CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildCylinderControls(int x, int y, int w) {
        int step = GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        int halfW = (w - GuiConstants.COMPACT_GAP) / 2;

        // Row 1: Radius + Top Radius
        cylinderRadius = LabeledSlider.builder("Radius")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0.1f, 20.0f).initial(state.getFloat("cylinder.radius")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("cylinder.radius", v))).build();
        widgets.add(cylinderRadius);
        
        cylinderTopRadius = LabeledSlider.builder("Top R")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0f, 20f).initial(state.getFloat("cylinder.topRadius")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("cylinder.topRadius", v))).build();
        widgets.add(cylinderTopRadius);
        y += step;
        
        // Row 2: Height + Segments
        cylinderHeight = LabeledSlider.builder("Height")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0.1f, 256f).initial(state.getFloat("cylinder.height")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("cylinder.height", v))).build();
        widgets.add(cylinderHeight);
        
        cylinderSegments = LabeledSlider.builder("Segs")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(3, 256).initial(state.getInt("cylinder.segments")).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.set("cylinder.segments", Math.round(v)))).build();
        widgets.add(cylinderSegments);
        y += step;
        
        // Row 3: Arc + Height Segments
        cylinderArc = LabeledSlider.builder("Arc")
            .position(x, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(0f, 360f).initial(state.getFloat("cylinder.arc")).format("%.0f°")
            .onChange(v -> onUserChange(() -> state.set("cylinder.arc", v))).build();
        widgets.add(cylinderArc);
        
        cylinderHeightSegments = LabeledSlider.builder("H Segs")
            .position(x + halfW + GuiConstants.COMPACT_GAP, y).width(halfW).height(GuiConstants.COMPACT_HEIGHT).range(1, 100).initial(state.getInt("cylinder.heightSegments")).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.set("cylinder.heightSegments", Math.round(v)))).build();
        widgets.add(cylinderHeightSegments);
        y += step;
        
        // Row 4: Cap Top + Cap Bottom
        var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        
        boolean capTopInitial = state.getBool("cylinder.capTop");
        boolean capBottomInitial = state.getBool("cylinder.capBottom");
        boolean openEndedInitial = !capTopInitial && !capBottomInitial;

        cylinderCapTop = GuiWidgets.checkbox(x, y, "Cap Top", capTopInitial, "Render top cap",
            textRenderer, v -> onUserChange(() -> state.set("cylinder.capTop", v)));
        widgets.add(cylinderCapTop);
        
        cylinderCapBottom = GuiWidgets.checkbox(x + halfW + GuiConstants.COMPACT_GAP, y, "Cap Bot", capBottomInitial, "Render bottom cap",
            textRenderer, v -> onUserChange(() -> state.set("cylinder.capBottom", v)));
        widgets.add(cylinderCapBottom);
        y += step;
        
        // Row 5: Open Ended
        cylinderOpenEnded = GuiWidgets.checkbox(x, y, "Open Ended (Tube)", openEndedInitial, "No caps (tube mode)",
            textRenderer, v -> onUserChange(() -> {
                // When open-ended is checked, disable both caps; otherwise restore both caps on
                state.set("cylinder.capTop", !v);
                state.set("cylinder.capBottom", !v);
                // Rebuild to reflect UI state instead of calling non-existent setters
                rebuildForCurrentShape();
            }));
        widgets.add(cylinderOpenEnded);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G70: POLYHEDRON CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildPolyhedronControls(int x, int y, int w) {
        // No need for Type dropdown - the poly type is determined by which shape 
        // the user selected (cube, tetrahedron, octahedron, dodecahedron, icosahedron)
        
        polyRadius = LabeledSlider.builder("Size")
            .position(x, y).width(w).range(0.5f, 10f).initial(state.getFloat("polyhedron.radius")).format("%.1f")
            .onChange(v -> onUserChange(() -> state.set("polyhedron.radius", v))).build();
        widgets.add(polyRadius);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        polySubdivisions = LabeledSlider.builder("Subdivisions")
            .position(x, y).width(w).range(0, 4).initial(state.getInt("polyhedron.subdivisions")).format("%d").step(1)
            .onChange(v -> onUserChange(() -> state.set("polyhedron.subdivisions", Math.round(v)))).build();
        widgets.add(polySubdivisions);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TORUS CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildTorusControls(int x, int y, int w) {
        int step = GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        
        // Major radius (ring radius)
        var majorRadius = LabeledSlider.builder("Ring Radius")
            .position(x, y).width(w).range(0.5f, 5f).initial(state.getFloat("torus.majorRadius")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("torus.majorRadius", v))).build();
        widgets.add(majorRadius);
        y += step;
        
        // Minor radius (tube radius)
        var minorRadius = LabeledSlider.builder("Tube Radius")
            .position(x, y).width(w).range(0.1f, 2f).initial(state.getFloat("torus.minorRadius")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("torus.minorRadius", v))).build();
        widgets.add(minorRadius);
        y += step;
        
        // Segments
        var majorSegments = LabeledSlider.builder("Ring Segments")
            .position(x, y).width(w).range(8, 64).initial(state.getInt("torus.majorSegments")).format("%d").step(4)
            .onChange(v -> onUserChange(() -> state.set("torus.majorSegments", Math.round(v)))).build();
        widgets.add(majorSegments);
        y += step;
        
        var minorSegments = LabeledSlider.builder("Tube Segments")
            .position(x, y).width(w).range(4, 32).initial(state.getInt("torus.minorSegments")).format("%d").step(2)
            .onChange(v -> onUserChange(() -> state.set("torus.minorSegments", Math.round(v)))).build();
        widgets.add(minorSegments);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CAPSULE CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildCapsuleControls(int x, int y, int w) {
        int step = GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        
        // Radius
        var radius = LabeledSlider.builder("Radius")
            .position(x, y).width(w).range(0.1f, 3f).initial(state.getFloat("capsule.radius")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("capsule.radius", v))).build();
        widgets.add(radius);
        y += step;
        
        // Height
        var height = LabeledSlider.builder("Height")
            .position(x, y).width(w).range(0.5f, 10f).initial(state.getFloat("capsule.height")).format("%.1f")
            .onChange(v -> onUserChange(() -> state.set("capsule.height", v))).build();
        widgets.add(height);
        y += step;
        
        // Segments
        var segments = LabeledSlider.builder("Segments")
            .position(x, y).width(w).range(8, 64).initial(state.getInt("capsule.segments")).format("%d").step(4)
            .onChange(v -> onUserChange(() -> state.set("capsule.segments", Math.round(v)))).build();
        widgets.add(segments);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONE CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildConeControls(int x, int y, int w) {
        int step = GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        
        // Bottom radius
        var bottomRadius = LabeledSlider.builder("Bottom Radius")
            .position(x, y).width(w).range(0.1f, 5f).initial(state.getFloat("cone.bottomRadius")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("cone.bottomRadius", v))).build();
        widgets.add(bottomRadius);
        y += step;
        
        // Top radius (0 for pointed cone, > 0 for frustum)
        var topRadius = LabeledSlider.builder("Top Radius")
            .position(x, y).width(w).range(0f, 5f).initial(state.getFloat("cone.topRadius")).format("%.2f")
            .onChange(v -> onUserChange(() -> state.set("cone.topRadius", v))).build();
        widgets.add(topRadius);
        y += step;
        
        // Height
        var height = LabeledSlider.builder("Height")
            .position(x, y).width(w).range(0.5f, 10f).initial(state.getFloat("cone.height")).format("%.1f")
            .onChange(v -> onUserChange(() -> state.set("cone.height", v))).build();
        widgets.add(height);
        y += step;
        
        // Segments
        var segments = LabeledSlider.builder("Segments")
            .position(x, y).width(w).range(8, 64).initial(state.getInt("cone.segments")).format("%d").step(4)
            .onChange(v -> onUserChange(() -> state.set("cone.segments", Math.round(v)))).build();
        widgets.add(segments);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PATTERN CONTROLS - Face pattern selection per shape part
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Builds pattern controls based on shape type.
     * @return Updated Y position after adding controls
     */
    private int buildPatternControls(int x, int y, int w, String shapeType) {
        // Clear previous pattern widgets
        patternFaces = null;
        patternBody = null;
        patternTop = null;
        patternBottom = null;
        
        ArrangementConfig arr = state.arrangement();
        String shape = shapeType.toLowerCase();
        
        // Shapes with per-part patterns (body, top, bottom)
        if (shape.equals("cylinder") || shape.equals("prism") || shape.equals("beam")) {
            // Three compact buttons in a row: [Body] [Top] [Bot]
            int padding = 2;
            int btnWidth = (w - padding * 2) / 3;
            
            patternBody = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal("B " + capitalize(v)))
                .values(PATTERN_OPTIONS)
                .initially(arr.getPattern("sides"))
                .tooltip(v -> net.minecraft.client.gui.tooltip.Tooltip.of(net.minecraft.text.Text.literal("Body pattern: " + v)))
                .omitKeyText()  // Prevents colon prefix
                .build(x, y, btnWidth, GuiConstants.COMPACT_HEIGHT, net.minecraft.text.Text.empty(),
                    (btn, val) -> onPatternChanged("sides", val));
            widgets.add(patternBody);
            
            patternTop = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal("T " + capitalize(v)))
                .values(PATTERN_OPTIONS)
                .initially(arr.getPattern("capTop"))
                .tooltip(v -> net.minecraft.client.gui.tooltip.Tooltip.of(net.minecraft.text.Text.literal("Top cap pattern: " + v)))
                .omitKeyText()  // Prevents colon prefix
                .build(x + btnWidth + padding, y, btnWidth, GuiConstants.COMPACT_HEIGHT, net.minecraft.text.Text.empty(),
                    (btn, val) -> onPatternChanged("capTop", val));
            widgets.add(patternTop);
            
            patternBottom = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal("B " + capitalize(v)))
                .values(PATTERN_OPTIONS)
                .initially(arr.getPattern("capBottom"))
                .tooltip(v -> net.minecraft.client.gui.tooltip.Tooltip.of(net.minecraft.text.Text.literal("Bottom cap pattern: " + v)))
                .omitKeyText()  // Prevents colon prefix
                .build(x + (btnWidth + padding) * 2, y, btnWidth, GuiConstants.COMPACT_HEIGHT, net.minecraft.text.Text.empty(),
                    (btn, val) -> onPatternChanged("capBottom", val));
            widgets.add(patternBottom);
            
            y += GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        } else {
            // Single faces pattern button (full width)
            patternFaces = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal("Pattern " + capitalize(v)))
                .values(PATTERN_OPTIONS)
                .initially(arr.getPattern("faces"))
                .tooltip(v -> net.minecraft.client.gui.tooltip.Tooltip.of(net.minecraft.text.Text.literal("Face pattern: " + v)))
                .omitKeyText()  // Prevents colon prefix
                .build(x, y, w, GuiConstants.COMPACT_HEIGHT, net.minecraft.text.Text.empty(),
                    (btn, val) -> onPatternChanged("faces", val));
            widgets.add(patternFaces);
            
            y += GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        }
        
        return y;
    }
    
    /** Updates arrangement when pattern changes. */
    private void onPatternChanged(String partName, String patternName) {
        ArrangementConfig current = state.arrangement();
        ArrangementConfig.Builder builder = current.toBuilder();
        
        switch (partName) {
            case "faces" -> {
                // Set ALL part keys that different renderers use so pattern works for ALL shapes:
                // - defaultPattern: fallback for any unset part
                // - faces: polyhedra (PolyhedronRenderer)
                // - main: spheres (SphereRenderer)  
                // - surface: rings, discs, cones, torus (RingRenderer, DiscRenderer, ConeRenderer, TorusRenderer)
                builder.defaultPattern(patternName);
                builder.faces(patternName);
                builder.main(patternName);
                builder.surface(patternName);
            }
            case "sides" -> builder.sides(patternName);
            case "capTop" -> builder.capTop(patternName);
            case "capBottom" -> builder.capBottom(patternName);
        }
        
        state.set(builder.build());
        Logging.GUI.topic("panel").debug("Pattern changed: {} = {}", partName, patternName);
    }
    
    /** Capitalizes first letter of pattern name for display. */
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        
        // Use bounds for positioning
        int bx = bounds.x();
        int by = bounds.y();
        
        // Draw simple title bar
        int titleX = bx + GuiConstants.PADDING;
        int titleY = by + (TITLE_HEIGHT - 8) / 2;
        context.drawTextWithShadow(tr, "Shape", titleX, titleY, 0xFF88AACC);
        
        // Render all widgets directly (no collapse)
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
            if (fragmentDropdown != null && FragmentRegistry.listShapeFragments(currentShapeType).contains("Custom")) {
                fragmentDropdown.setValue("Custom");
            }
        }
        // Update performance warning after any change
        computePerformanceHint();
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
        if (sphereRadius != null) sphereRadius.setValue(state.getFloat("sphere.radius"));
        if (sphereLatSteps != null) sphereLatSteps.setValue(state.getInt("sphere.latSteps"));
        if (sphereLonSteps != null) sphereLonSteps.setValue(state.getInt("sphere.lonSteps"));
        if (sphereLatStart != null) sphereLatStart.setValue(state.getFloat("sphere.latStart"));
        if (sphereLatEnd != null) sphereLatEnd.setValue(state.getFloat("sphere.latEnd"));
        if (sphereAlgorithm != null) {
            try { sphereAlgorithm.setValue(SphereAlgorithm.valueOf(state.getString("sphere.algorithm"))); } catch (IllegalArgumentException ignored) {}
        }
        if (ringInnerRadius != null) ringInnerRadius.setValue(state.getFloat("ring.innerRadius"));
        if (ringOuterRadius != null) ringOuterRadius.setValue(state.getFloat("ring.outerRadius"));
        if (ringSegments != null) ringSegments.setValue(state.getInt("ring.segments"));
        if (ringHeight != null) ringHeight.setValue(state.getFloat("ring.height"));
        if (ringY != null) ringY.setValue(state.getFloat("ring.y"));
        if (discRadius != null) discRadius.setValue(state.getFloat("disc.radius"));
        if (discSegments != null) discSegments.setValue(state.getInt("disc.segments"));
        if (discY != null) discY.setValue(state.getFloat("disc.y"));
        if (discInnerRadius != null) discInnerRadius.setValue(state.getFloat("disc.innerRadius"));
        if (prismSides != null) prismSides.setValue(state.getInt("prism.sides"));
        if (prismRadius != null) prismRadius.setValue(state.getFloat("prism.radius"));
        if (prismHeight != null) prismHeight.setValue(state.getFloat("prism.height"));
        if (prismTopRadius != null) prismTopRadius.setValue(state.getFloat("prism.topRadius"));
        if (cylinderRadius != null) cylinderRadius.setValue(state.getFloat("cylinder.radius"));
        if (cylinderHeight != null) cylinderHeight.setValue(state.getFloat("cylinder.height"));
        if (cylinderSegments != null) cylinderSegments.setValue(state.getInt("cylinder.segments"));
        if (cylinderTopRadius != null) cylinderTopRadius.setValue(state.getFloat("cylinder.topRadius"));
        if (polyRadius != null) polyRadius.setValue(state.getFloat("polyhedron.radius"));
        if (polySubdivisions != null) polySubdivisions.setValue(state.getInt("polyhedron.subdivisions"));
    }

    private void computePerformanceHint() {
        String shape = state.getString("shapeType").toLowerCase();
        switch (shape) {
            case "sphere" -> {
                int lat = state.getInt("sphere.latSteps");
                int lon = state.getInt("sphere.lonSteps");
                int tess = lat * lon;
                sendWarning(tess, 640, 1280, "~" + tess + " tris");
            }
            case "ring" -> {
                int seg = state.getInt("ring.segments");
                sendWarning(seg, 256, 512, "~" + seg + " segs");
            }
            case "disc" -> {
                int seg = state.getInt("disc.segments");
                sendWarning(seg, 256, 512, "~" + seg + " segs");
            }
            case "prism" -> {
                int seg = state.getInt("prism.sides");
                sendWarning(seg, 64, 128, "~" + seg + " sides");
            }
            case "cylinder", "beam" -> {
                int seg = state.getInt("cylinder.segments");
                sendWarning(seg, 128, 256, "~" + seg + " segs");
            }
            case "cube", "octahedron", "icosahedron" -> {
                int sub = state.getInt("polyhedron.subdivisions");
                int base = shape.equals("cube") ? 6 : (shape.equals("octahedron") ? 8 : 20);
                int tess = (int) (base * Math.pow(4, sub));
                sendWarning(tess, 200, 800, "~" + tess + " faces");
            }
            default -> {
                if (warningCallback != null) warningCallback.accept(null, 0);
            }
        }
    }

    private void sendWarning(int value, int warn, int high, String label) {
        if (warningCallback == null) return;
        
        if (value >= high) {
            warningCallback.accept("⚠ High: " + label, GuiConstants.ERROR);
        } else if (value >= warn) {
            warningCallback.accept("⚠ Med: " + label, GuiConstants.WARNING);
        } else {
            warningCallback.accept(null, 0);
        }
    }
}
