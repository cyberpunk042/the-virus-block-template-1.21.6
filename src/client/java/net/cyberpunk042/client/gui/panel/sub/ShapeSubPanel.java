package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.shape.ShapeControlBuilder;
import net.cyberpunk042.client.gui.shape.ShapePerformanceHint;
import net.cyberpunk042.client.gui.shape.ShapeWidgetSpec;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * G62-G70: Shape-specific parameter controls.
 * 
 * <p>Dynamically shows controls based on the current shape type using a
 * data-driven approach via {@link ShapeWidgetSpec} and {@link ShapeControlBuilder}.</p>
 * 
 * <h3>Supported Shapes</h3>
 * <ul>
 *   <li>Sphere, Ring, Prism, Cylinder</li>
 *   <li>Cube, Tetrahedron, Octahedron, Dodecahedron, Icosahedron</li>
 *   <li>Torus, Capsule, Cone</li>
 * </ul>
 */
public class ShapeSubPanel extends AbstractPanel {
    
    private static final int TITLE_HEIGHT = 16;
    private int startY;
    
    // Current shape state
    private String currentShapeType = "";
    
    // Callbacks
    private BiConsumer<String, Integer> warningCallback;
    private Runnable shapeChangedCallback;
    
    // Top-level dropdowns (not part of spec-driven widgets)
    private CyclingButtonWidget<ShapeKind> shapeTypeDropdown;
    private CyclingButtonWidget<String> fragmentDropdown;
    
    // Fragment/preset state
    private boolean applyingFragment = false;
    private String currentFragment = "Default";
    
    // Pattern control widgets
    private CyclingButtonWidget<String> patternFaces;
    private CyclingButtonWidget<String> patternBody;
    private CyclingButtonWidget<String> patternTop;
    private CyclingButtonWidget<String> patternBottom;
    
    // Special: Cylinder "Open Ended" checkbox (controls both caps)
    private net.minecraft.client.gui.widget.CheckboxWidget cylinderOpenEnded;
    
    // Widget builder for shape-specific controls
    private ShapeControlBuilder controlBuilder;
    
    // Pattern options for shape arrangements
    private static final List<String> PATTERN_OPTIONS = List.of("full", "half", "filled_1");
    
    /**
     * Available shape types for the field primitive.
     */
    public enum ShapeKind {
        SPHERE("sphere", "Sphere"),
        RING("ring", "Ring"),
        PRISM("prism", "Prism"),
        CYLINDER("cylinder", "Cylinder"),
        CUBE("cube", "Cube"),
        TETRAHEDRON("tetrahedron", "Tetrahedron"),
        OCTAHEDRON("octahedron", "Octahedron"),
        DODECAHEDRON("dodecahedron", "Dodecahedron"),
        ICOSAHEDRON("icosahedron", "Icosahedron"),
        TORUS("torus", "Torus"),
        CAPSULE("capsule", "Capsule"),
        CONE("cone", "Cone"),
        JET("jet", "Jet"),
        RAYS("rays", "Rays");
        
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
            return SPHERE;
        }
    }
    
    public ShapeSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        this.controlBuilder = new ShapeControlBuilder(state, this::onUserChange);
        Logging.GUI.topic("panel").debug("ShapeSubPanel created");
    }
    
    /**
     * Sets a callback for when the performance warning changes.
     */
    public void setWarningCallback(BiConsumer<String, Integer> callback) {
        this.warningCallback = callback;
    }
    
    /**
     * Sets a callback for when the shape type changes and widgets are rebuilt.
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
        boolean needsOffset = bounds != null && !bounds.isEmpty();
        
        // Reset scroll offset to avoid position corruption
        scrollOffset = 0;
        
        widgets.clear();
        controlBuilder.clear();
        
        String shapeType = state.getString("shapeType");
        Logging.GUI.topic("panel").info("ShapeSubPanel rebuild: shapeType='{}', currentShapeType='{}'", 
            shapeType, currentShapeType);
        
        if (!shapeType.equals(currentShapeType)) {
            currentShapeType = shapeType;
            currentFragment = "Default";
        }
        
        int x = GuiConstants.PADDING;
        int y = startY + TITLE_HEIGHT;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        int step = GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        
        // ═══════════════════════════════════════════════════════════════════════
        // ROW 1: Shape Type + Preset Dropdowns
        // ═══════════════════════════════════════════════════════════════════════
        
        ShapeKind currentKind = ShapeKind.fromId(shapeType);
        shapeTypeDropdown = CyclingButtonWidget.<ShapeKind>builder(k -> net.minecraft.text.Text.literal(k.toString()))
            .values(ShapeKind.values())
            .initially(currentKind)
            .build(x, y, halfW, GuiConstants.COMPACT_HEIGHT, net.minecraft.text.Text.literal("Shape"),
                (btn, val) -> {
                    state.set("shapeType", val.getId());
                    rebuildForCurrentShape();
                    if (shapeChangedCallback != null) {
                        shapeChangedCallback.run();
                    }
                });
        widgets.add(shapeTypeDropdown);
        
        List<String> presets = FragmentRegistry.listShapeFragments(shapeType);
        Logging.GUI.topic("panel").info("ShapeSubPanel shape presets for '{}': {}", shapeType, presets);
        // Only reset to "Custom" if not currently applying a fragment
        if (!applyingFragment) {
            currentFragment = "Custom";
        }
        fragmentDropdown = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal(v))
            .values(presets)
            .initially(currentFragment)
            .build(x + halfW + GuiConstants.PADDING, y, halfW, GuiConstants.COMPACT_HEIGHT, 
                net.minecraft.text.Text.literal("Variant"),
                (btn, val) -> applyPreset(val));
        widgets.add(fragmentDropdown);
        y += step;
        
        // ═══════════════════════════════════════════════════════════════════════
        // ROW 2+: Pattern Controls (shape-dependent)
        // ═══════════════════════════════════════════════════════════════════════
        
        y = buildPatternControls(x, y, w, shapeType);
        
        // ═══════════════════════════════════════════════════════════════════════
        // SHAPE-SPECIFIC CONTROLS (data-driven)
        // ═══════════════════════════════════════════════════════════════════════
        
        List<Object> specs = ShapeWidgetSpec.forShape(shapeType);
        y = controlBuilder.build(specs, x, y, w);
        widgets.addAll(controlBuilder.getWidgets());
        
        // ═══════════════════════════════════════════════════════════════════════
        // SPECIAL: Quad Pattern Dropdown (for shapes that use QUAD cells)
        // ═══════════════════════════════════════════════════════════════════════
        
        // Sphere, Ring, and 3D Rays all use QUAD cells and need pattern control
        boolean needsQuadPattern = shapeType.equalsIgnoreCase("sphere") 
            || shapeType.equalsIgnoreCase("ring");
        
        // For rays, check if it's a 3D type (which uses QUAD cells)
        if (shapeType.equalsIgnoreCase("rays")) {
            String rayTypeStr = state.getString("rays.rayType");
            try {
                var rayType = net.cyberpunk042.visual.shape.RayType.valueOf(
                    rayTypeStr != null ? rayTypeStr : "LINE");
                needsQuadPattern = rayType.is3D();
            } catch (IllegalArgumentException ignored) {}
        }
        
        if (needsQuadPattern) {
            // Get current pattern from state
            String patternId = state.getString("arrangement.defaultPattern");
            net.cyberpunk042.visual.pattern.QuadPattern currentPattern = 
                net.cyberpunk042.visual.pattern.QuadPattern.FILLED_1;
            if (patternId != null) {
                var found = net.cyberpunk042.visual.pattern.QuadPattern.fromId(patternId);
                if (found != null) {
                    currentPattern = found;
                }
            }
            
            var quadPatternDropdown = GuiWidgets.enumDropdown(
                x, y, w, GuiConstants.COMPACT_HEIGHT, "Quad Pattern",
                net.cyberpunk042.visual.pattern.QuadPattern.class,
                currentPattern,
                "Cell-level pattern for quad tessellation",
                v -> onUserChange(() -> state.set("arrangement.defaultPattern", v.id()))
            );
            widgets.add(quadPatternDropdown);
            y += step;
        }
        
        // ═══════════════════════════════════════════════════════════════════════
        // SPECIAL: Cylinder "Open Ended" Checkbox
        // ═══════════════════════════════════════════════════════════════════════
        
        if (shapeType.equalsIgnoreCase("cylinder") || shapeType.equalsIgnoreCase("beam")) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;
            boolean capTop = state.getBool("cylinder.capTop");
            boolean capBottom = state.getBool("cylinder.capBottom");
            boolean openEnded = !capTop && !capBottom;
            
            cylinderOpenEnded = GuiWidgets.checkbox(x, y, "Open Ended (Tube)", openEnded, 
                "No caps (tube mode)", textRenderer, v -> onUserChange(() -> {
                    state.set("cylinder.capTop", !v);
                    state.set("cylinder.capBottom", !v);
                    rebuildForCurrentShape();
                }));
            widgets.add(cylinderOpenEnded);
            y += step;
        }
        
        // ═══════════════════════════════════════════════════════════════════════
        // SPECIAL: Jet "Hollow" Checkbox + Conditional Inner Radii
        // ═══════════════════════════════════════════════════════════════════════
        
        if (shapeType.equalsIgnoreCase("jet")) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;
            boolean hollow = state.getBool("jet.hollow");
            
            // Row 1: Hollow checkbox + Unified toggle (side by side)
            // NOTE: Uses same pattern as shape dropdown - direct set + rebuild, no onUserChange
            var jetHollowCheckbox = GuiWidgets.checkbox(x, y, "Hollow", hollow, 
                "Create hollow tube with inner wall", textRenderer, v -> {
                    state.set("jet.hollow", v);
                    rebuildForCurrentShape();  // Rebuild to show/hide inner radii controls
                    if (shapeChangedCallback != null) {
                        shapeChangedCallback.run();  // Notify parent like shape change does
                    }
                });
            widgets.add(jetHollowCheckbox);
            
            // Unified toggle only visible when hollow is enabled
            if (hollow) {
                boolean unified = state.getBool("jet.unifiedInner");
                var unifiedToggle = GuiWidgets.checkbox(x + halfW + GuiConstants.PADDING, y, 
                    "Unified", unified, "Use wall thickness instead of custom radii", textRenderer, 
                    v -> {
                        state.set("jet.unifiedInner", v);
                        rebuildForCurrentShape();  // Rebuild to swap slider types
                        if (shapeChangedCallback != null) {
                            shapeChangedCallback.run();
                        }
                    });
                widgets.add(unifiedToggle);
            }
            y += step;
            
            
            // Row 2: Inner radii controls (only when hollow)
            if (hollow) {
                boolean unified = state.getBool("jet.unifiedInner");
                
                // Get outer radii to calculate safe ranges
                float baseR = state.getFloat("jet.baseRadius");
                float topTipR = state.getFloat("jet.topTipRadius");
                float bottomTipR = state.getFloat("jet.bottomTipRadius");
                float minOuterR = Math.min(baseR, Math.min(topTipR, bottomTipR));
                
                if (unified) {
                    // Single wall thickness slider - max is 95% of smallest outer radius
                    float maxThickness = Math.max(0.01f, minOuterR * 0.95f);
                    float wallThick = Math.min(state.getFloat("jet.innerWallThickness"), maxThickness);
                    var thicknessSlider = GuiWidgets.slider(x, y, w,
                        "Wall Thickness", 0.01f, maxThickness, wallThick, "%.2f", 
                        "Inner wall thickness (max: " + String.format("%.2f", maxThickness) + ")",
                        v -> onUserChange(() -> state.set("jet.innerWallThickness", v)));
                    widgets.add(thicknessSlider);
                } else {
                    // Separate inner radius sliders - max is (outer - 0.01)
                    float maxInnerBase = Math.max(0.01f, baseR - 0.01f);
                    float innerBaseR = Math.min(state.getFloat("jet.innerBaseRadius"), maxInnerBase);
                    var innerBaseSlider = GuiWidgets.slider(x, y, halfW,
                        "Inner Base R", 0f, maxInnerBase, innerBaseR, "%.2f", 
                        "Max: " + String.format("%.2f", maxInnerBase),
                        v -> onUserChange(() -> state.set("jet.innerBaseRadius", v)));
                    widgets.add(innerBaseSlider);
                    
                    // Use larger of top/bottom tip for inner tip max
                    float tipR = Math.max(topTipR, bottomTipR);
                    float maxInnerTip = Math.max(0.01f, tipR - 0.01f);
                    float innerTipR = Math.min(state.getFloat("jet.innerTipRadius"), maxInnerTip);
                    var innerTipSlider = GuiWidgets.slider(x + halfW + GuiConstants.PADDING, y, halfW,
                        "Inner Tip R", 0f, maxInnerTip, innerTipR, "%.2f", 
                        "Max: " + String.format("%.2f", maxInnerTip),
                        v -> onUserChange(() -> state.set("jet.innerTipRadius", v)));
                    widgets.add(innerTipSlider);
                }
                y += step;
            }
        }
        
        // ═══════════════════════════════════════════════════════════════════════
        // SPECIAL: Rays - Conditional controls based on ray type
        // ═══════════════════════════════════════════════════════════════════════
        
        if (shapeType.equalsIgnoreCase("rays")) {
            // === RAY TYPE SECTION (shown first!) ===
            String rayTypeStr = state.getString("rays.rayType");
            net.cyberpunk042.visual.shape.RayType rayType;
            try {
                rayType = net.cyberpunk042.visual.shape.RayType.valueOf(
                    rayTypeStr != null ? rayTypeStr : "LINE");
            } catch (IllegalArgumentException e) {
                rayType = net.cyberpunk042.visual.shape.RayType.LINE;
            }
            
            // Ray Type dropdown (full width) - FIRST control for rays
            var rayTypeDropdown = GuiWidgets.enumDropdown(
                x, y, w, GuiConstants.COMPACT_HEIGHT, "Ray Type",
                net.cyberpunk042.visual.shape.RayType.class,
                rayType, "Visual appearance of each ray (LINE, DROPLET, etc.)",
                v -> {
                    state.set("rays.rayType", v.name());
                    rebuildForCurrentShape();
                    if (shapeChangedCallback != null) {
                        shapeChangedCallback.run();
                    }
                });
            widgets.add(rayTypeDropdown);
            y += step;
            
            boolean isLineType = (rayType == net.cyberpunk042.visual.shape.RayType.LINE);
            boolean is3DType = rayType.is3D();
            
            // ═══════════════════════════════════════════════════════════════════
            // LINE-ONLY CONTROLS
            // ═══════════════════════════════════════════════════════════════════
            
            if (isLineType) {
                // Ray Width (LINE only)
                float rayWidth = state.getFloat("rays.rayWidth");
                var widthSlider = GuiWidgets.slider(x, y, w,
                    "Ray Width", 0.01f, 10f, rayWidth, "%.2f", 
                    "Thickness of line rays",
                    v -> onUserChange(() -> state.set("rays.rayWidth", v)));
                widgets.add(widthSlider);
                y += step;
                
                // === FADING (LINE only) ===
                // Fade Start + Fade End
                float fadeStart = state.getFloat("rays.fadeStart");
                var fadeStartSlider = GuiWidgets.slider(x, y, halfW,
                    "Fade Start", 0f, 1f, fadeStart, "%.2f", 
                    "Alpha at ray start (0=transparent, 1=opaque)",
                    v -> onUserChange(() -> state.set("rays.fadeStart", v)));
                widgets.add(fadeStartSlider);
                
                float fadeEnd = state.getFloat("rays.fadeEnd");
                var fadeEndSlider = GuiWidgets.slider(x + halfW + GuiConstants.PADDING, y, halfW,
                    "Fade End", 0f, 1f, fadeEnd, "%.2f", 
                    "Alpha at ray end (0=transparent, 1=opaque)",
                    v -> onUserChange(() -> state.set("rays.fadeEnd", v)));
                widgets.add(fadeEndSlider);
                y += step;
                
                // === DASHED LINES (LINE only) ===
                // Dash Count + Dash Gap
                int segs = state.getInt("rays.segments");
                var segsSlider = GuiWidgets.slider(x, y, halfW,
                    "Dash Count", 1, 10, segs, "%d", 
                    "Number of dashed segments per ray (1 = solid)",
                    v -> onUserChange(() -> state.set("rays.segments", Math.round(v))));
                widgets.add(segsSlider);
                
                float segGap = state.getFloat("rays.segmentGap");
                var segGapSlider = GuiWidgets.slider(x + halfW + GuiConstants.PADDING, y, halfW,
                    "Dash Gap", 0f, 0.5f, segGap, "%.2f", 
                    "Gap between dashes (0 = no gap)",
                    v -> onUserChange(() -> state.set("rays.segmentGap", v)));
                widgets.add(segGapSlider);
                y += step;
                
                // === LINE SHAPE (LINE only) ===
                String lineShapeStr = state.getString("rays.lineShape");
                net.cyberpunk042.visual.shape.RayLineShape lineShape;
                try {
                    lineShape = net.cyberpunk042.visual.shape.RayLineShape.valueOf(
                        lineShapeStr != null ? lineShapeStr : "STRAIGHT");
                } catch (IllegalArgumentException e) {
                    lineShape = net.cyberpunk042.visual.shape.RayLineShape.STRAIGHT;
                }
                
                var lineShapeDropdown = GuiWidgets.enumDropdown(
                    x, y, w, GuiConstants.COMPACT_HEIGHT, "Line Shape",
                    net.cyberpunk042.visual.shape.RayLineShape.class,
                    lineShape, "Shape of individual rays (affects curvature per-ray)",
                    v -> {
                        state.set("rays.lineShape", v.name());
                        rebuildForCurrentShape();
                        if (shapeChangedCallback != null) {
                            shapeChangedCallback.run();
                        }
                    });
                widgets.add(lineShapeDropdown);
                y += step;
                
                // Conditional: Show amplitude/frequency only when lineShape != STRAIGHT
                if (lineShape != net.cyberpunk042.visual.shape.RayLineShape.STRAIGHT) {
                    float amp = state.getFloat("rays.lineShapeAmplitude");
                    var ampSlider = GuiWidgets.slider(x, y, halfW,
                        "Amplitude", 0.01f, 1f, amp, "%.2f", 
                        "How far rays deviate from straight",
                        v -> onUserChange(() -> state.set("rays.lineShapeAmplitude", v)));
                    widgets.add(ampSlider);
                    
                    float freq = state.getFloat("rays.lineShapeFrequency");
                    var freqSlider = GuiWidgets.slider(x + halfW + GuiConstants.PADDING, y, halfW,
                        "Frequency", 0.5f, 10f, freq, "%.1f", 
                        "Number of wave cycles along ray",
                        v -> onUserChange(() -> state.set("rays.lineShapeFrequency", v)));
                    widgets.add(freqSlider);
                    y += step;
                }
                
                // Curve Resolution (vertex count for curved/animated rays)
                int lineSegs = state.getInt("rays.shapeSegments");
                var lineSegsSlider = GuiWidgets.slider(x, y, w,
                    "Curve Res", 1, 256, lineSegs, "%d", 
                    "Vertex count per ray (more = smoother curves)",
                    v -> onUserChange(() -> state.set("rays.shapeSegments", Math.round(v))));
                widgets.add(lineSegsSlider);
                y += step;
            }
            
            // ═══════════════════════════════════════════════════════════════════
            // 3D-ONLY CONTROLS
            // ═══════════════════════════════════════════════════════════════════
            
            if (is3DType) {
                // Orientation (tip direction)
                String orientStr = state.getString("rays.rayOrientation");
                net.cyberpunk042.visual.shape.RayOrientation orientation;
                try {
                    orientation = net.cyberpunk042.visual.shape.RayOrientation.valueOf(
                        orientStr != null ? orientStr : "ALONG_RAY");
                } catch (IllegalArgumentException e) {
                    orientation = net.cyberpunk042.visual.shape.RayOrientation.ALONG_RAY;
                }
                
                var orientDropdown = GuiWidgets.enumDropdown(
                    x, y, w, GuiConstants.COMPACT_HEIGHT, "Orientation",
                    net.cyberpunk042.visual.shape.RayOrientation.class,
                    orientation, "Direction the 3D shape points (tip direction)",
                    v -> {
                        state.set("rays.rayOrientation", v.name());
                        rebuildForCurrentShape();
                        if (shapeChangedCallback != null) {
                            shapeChangedCallback.run();
                        }
                    });
                widgets.add(orientDropdown);
                y += step;
                
                // Intensity + Shape Length
                float intensity = state.getFloat("rays.shapeIntensity");
                var intensitySlider = GuiWidgets.slider(x, y, halfW,
                    "Intensity", 0f, 1f, intensity, "%.2f", 
                    "Blend amount (0=sphere, 1=full shape)",
                    v -> onUserChange(() -> state.set("rays.shapeIntensity", v)));
                widgets.add(intensitySlider);
                
                float shapeLen = state.getFloat("rays.shapeLength");
                var shapeLenSlider = GuiWidgets.slider(x + halfW + GuiConstants.PADDING, y, halfW,
                    "Shape Len", 0.2f, 3f, shapeLen, "%.2f", 
                    "Axial stretch (<1 oblate, 1 normal, >1 prolate)",
                    v -> onUserChange(() -> state.set("rays.shapeLength", v)));
                widgets.add(shapeLenSlider);
                y += step;
                
                // 3D Segments (for mesh resolution)
                int segs3D = state.getInt("rays.shapeSegments");
                var segs3DSlider = GuiWidgets.slider(x, y, w,
                    "3D Segments", 12, 128, segs3D, "%d", 
                    "Mesh resolution for 3D shapes (more = smoother)",
                    v -> onUserChange(() -> state.set("rays.shapeSegments", Math.round(v))));
                widgets.add(segs3DSlider);
                y += step;
            }
            
            // ═══════════════════════════════════════════════════════════════════
            // ENERGY INTERACTION (radiative mode - defines WHAT shape looks like)
            // ═══════════════════════════════════════════════════════════════════
            
            // RadiativeInteraction dropdown
            String radiativeStr = state.getString("rays.radiativeInteraction");
            net.cyberpunk042.visual.energy.RadiativeInteraction radiative;
            try {
                radiative = net.cyberpunk042.visual.energy.RadiativeInteraction.fromString(
                    radiativeStr != null ? radiativeStr : "NONE");
            } catch (IllegalArgumentException e) {
                radiative = net.cyberpunk042.visual.energy.RadiativeInteraction.NONE;
            }
            
            var radiativeDropdown = GuiWidgets.enumDropdown(
                x, y, w, GuiConstants.COMPACT_HEIGHT, "Energy Mode",
                net.cyberpunk042.visual.energy.RadiativeInteraction.class,
                radiative, "How energy interacts with rays (animates on Modifiers panel)",
                v -> {
                    state.set("rays.radiativeInteraction", v.name());
                    rebuildForCurrentShape();
                    if (shapeChangedCallback != null) {
                        shapeChangedCallback.run();
                    }
                });
            widgets.add(radiativeDropdown);
            y += step;
            
            // Conditional: Show segment length + wave params when radiative != NONE
            if (radiative.isActive()) {
                
                // Segment Length (visible portion)
                float segLen = state.getFloat("rays.segmentLength");
                var segLenSlider = GuiWidgets.slider(x, y, halfW,
                    "Seg Len", 0.1f, 1f, segLen, "%.2f", 
                    "Visible ray portion (1 = full ray)",
                    v -> onUserChange(() -> state.set("rays.segmentLength", v)));
                widgets.add(segLenSlider);
                
                // Wave Arc (phase scaling)
                float waveArc = state.getFloat("rays.waveArc");
                var waveArcSlider = GuiWidgets.slider(x + halfW + GuiConstants.PADDING, y, halfW,
                    "Wave Arc", 0.1f, 10f, waveArc, "%.1f", 
                    "Phase scale (higher = faster cycling)",
                    v -> onUserChange(() -> state.set("rays.waveArc", v)));
                widgets.add(waveArcSlider);
                y += step;
                
                // Wave Count (sweep copies)
                float waveCount = state.getFloat("rays.waveCount");
                var waveCountSlider = GuiWidgets.slider(x, y, halfW,
                    "Sweep #", 0.1f, 5f, waveCount, "%.1f", 
                    "Arc copies (2 = two sweeps 180° apart)",
                    v -> onUserChange(() -> state.set("rays.waveCount", v)));
                widgets.add(waveCountSlider);
                
                // Wave Distribution dropdown (SEQUENTIAL vs RANDOM vs CONTINUOUS)
                String waveDistStr = state.getString("rays.waveDistribution");
                net.cyberpunk042.visual.animation.WaveDistribution waveDist;
                try {
                    waveDist = net.cyberpunk042.visual.animation.WaveDistribution.fromString(
                        waveDistStr != null ? waveDistStr : "SEQUENTIAL");
                } catch (IllegalArgumentException e) {
                    waveDist = net.cyberpunk042.visual.animation.WaveDistribution.SEQUENTIAL;
                }
                
                var waveDistDropdown = GuiWidgets.enumDropdown(
                    x + halfW + GuiConstants.PADDING, y, halfW, GuiConstants.COMPACT_HEIGHT, "Wave",
                    net.cyberpunk042.visual.animation.WaveDistribution.class,
                    waveDist, "Wave pattern (sequential/random/continuous)",
                    v -> onUserChange(() -> state.set("rays.waveDistribution", v.name())));
                widgets.add(waveDistDropdown);
                y += step;
                
                // Start Full Length + Follow Curve (side by side)
                boolean startFull = state.getBool("rays.startFullLength");
                var startFullToggle = GuiWidgets.toggle(
                    x, y, halfW, "Full Length",
                    startFull, "ON = rays spawn at full length",
                    v -> onUserChange(() -> state.set("rays.startFullLength", v)));
                widgets.add(startFullToggle);
                
                boolean followCurve = state.getBool("rays.followCurve");
                var followCurveToggle = GuiWidgets.toggle(
                    x + halfW + GuiConstants.PADDING, y, halfW, "Follow Curve",
                    followCurve, "Segment follows the curve path",
                    v -> onUserChange(() -> state.set("rays.followCurve", v)));
                widgets.add(followCurveToggle);
                y += step;
                
                // === STAGE/PHASE CONTROL ===
                // Read current shapeState (or create default)
                Object shapeStateObj = state.get("rays.shapeState");
                net.cyberpunk042.visual.shape.ShapeState<net.cyberpunk042.visual.shape.RayFlowStage> currentShapeState;
                if (shapeStateObj instanceof net.cyberpunk042.visual.shape.ShapeState<?>) {
                    @SuppressWarnings("unchecked")
                    var cast = (net.cyberpunk042.visual.shape.ShapeState<net.cyberpunk042.visual.shape.RayFlowStage>) shapeStateObj;
                    currentShapeState = cast;
                } else {
                    // Default: ACTIVE stage, phase 1.0 (full visibility)
                    currentShapeState = new net.cyberpunk042.visual.shape.ShapeState<>(
                        net.cyberpunk042.visual.shape.RayFlowStage.ACTIVE,
                        1.0f,
                        net.cyberpunk042.visual.shape.EdgeTransitionMode.CLIP
                    );
                }
                
                boolean isStageActive = currentShapeState.stage() == net.cyberpunk042.visual.shape.RayFlowStage.ACTIVE;
                float phase = currentShapeState.phase();
                
                var stageToggle = GuiWidgets.toggle(
                    x, y, halfW, "Stage: Active",
                    isStageActive, "ACTIVE = full shape, OFF = SPAWNING stage",
                    v -> onUserChange(() -> {
                        // Get current state, create new with modified stage
                        Object curObj = state.get("rays.shapeState");
                        net.cyberpunk042.visual.shape.RayFlowStage newStage = v 
                            ? net.cyberpunk042.visual.shape.RayFlowStage.ACTIVE 
                            : net.cyberpunk042.visual.shape.RayFlowStage.SPAWNING;
                        float curPhase = 0.5f;
                        net.cyberpunk042.visual.shape.EdgeTransitionMode curEdge = net.cyberpunk042.visual.shape.EdgeTransitionMode.CLIP;
                        if (curObj instanceof net.cyberpunk042.visual.shape.ShapeState<?> ss) {
                            curPhase = ss.phase();
                            curEdge = ss.edgeMode();
                        }
                        state.set("rays.shapeState", new net.cyberpunk042.visual.shape.ShapeState<>(newStage, curPhase, curEdge));
                    }));
                widgets.add(stageToggle);
                
                var phaseSlider = GuiWidgets.slider(x + halfW + GuiConstants.PADDING, y, halfW,
                    "Phase", 0f, 1f, phase, "%.2f", 
                    "Animation phase (0=start, 1=end)",
                    v -> onUserChange(() -> {
                        // Get current state, create new with modified phase
                        Object curObj = state.get("rays.shapeState");
                        net.cyberpunk042.visual.shape.RayFlowStage curStage = net.cyberpunk042.visual.shape.RayFlowStage.ACTIVE;
                        net.cyberpunk042.visual.shape.EdgeTransitionMode curEdge = net.cyberpunk042.visual.shape.EdgeTransitionMode.CLIP;
                        if (curObj instanceof net.cyberpunk042.visual.shape.ShapeState<?> ss) {
                            if (ss.stage() instanceof net.cyberpunk042.visual.shape.RayFlowStage rfs) {
                                curStage = rfs;
                            }
                            curEdge = ss.edgeMode();
                        }
                        state.set("rays.shapeState", new net.cyberpunk042.visual.shape.ShapeState<>(curStage, v, curEdge));
                    }));
                widgets.add(phaseSlider);
                y += step;
            }
            
            // ═══════════════════════════════════════════════════════════════════
            // FIELD CURVATURE (ALL ray types)
            // ═══════════════════════════════════════════════════════════════════
            
            String curvatureStr = state.getString("rays.curvature");
            net.cyberpunk042.visual.shape.RayCurvature curvature;
            try {
                curvature = net.cyberpunk042.visual.shape.RayCurvature.valueOf(
                    curvatureStr != null ? curvatureStr : "NONE");
            } catch (IllegalArgumentException e) {
                curvature = net.cyberpunk042.visual.shape.RayCurvature.NONE;
            }
            
            var curvatureDropdown = GuiWidgets.enumDropdown(
                x, y, w, GuiConstants.COMPACT_HEIGHT, "Field Curvature",
                net.cyberpunk042.visual.shape.RayCurvature.class,
                curvature, "How rays curve around the field center",
                v -> {
                    state.set("rays.curvature", v.name());
                    rebuildForCurrentShape();
                    if (shapeChangedCallback != null) {
                        shapeChangedCallback.run();
                    }
                });
            widgets.add(curvatureDropdown);
            y += step;
            
            // Conditional: Show intensity only when curvature != NONE
            if (curvature != net.cyberpunk042.visual.shape.RayCurvature.NONE) {
                float curveIntensity = state.getFloat("rays.curvatureIntensity");
                var curveIntensitySlider = GuiWidgets.slider(x, y, w,
                    "Curve Intensity", 0f, 3f, curveIntensity, "%.2f", 
                    "Strength of curvature effect (0 = straight, 1 = max curve)",
                    v -> onUserChange(() -> state.set("rays.curvatureIntensity", v)));
                widgets.add(curveIntensitySlider);
                y += step;
            }
            
            // === Field Deformation (gravitational distortion) ===
            String deformStr = state.getString("rays.fieldDeformation");
            net.cyberpunk042.visual.shape.FieldDeformationMode deformation;
            try {
                deformation = net.cyberpunk042.visual.shape.FieldDeformationMode.valueOf(
                    deformStr != null ? deformStr : "NONE");
            } catch (IllegalArgumentException e) {
                deformation = net.cyberpunk042.visual.shape.FieldDeformationMode.NONE;
            }
            
            var deformDropdown = GuiWidgets.enumDropdown(
                x, y, w, GuiConstants.COMPACT_HEIGHT, "Field Deform",
                net.cyberpunk042.visual.shape.FieldDeformationMode.class,
                deformation, "Gravitational distortion based on distance from center",
                v -> {
                    state.set("rays.fieldDeformation", v.name());
                    rebuildForCurrentShape();
                    if (shapeChangedCallback != null) {
                        shapeChangedCallback.run();
                    }
                });
            widgets.add(deformDropdown);
            y += step;
            
            // Conditional: Show intensity only when deformation != NONE
            if (deformation != net.cyberpunk042.visual.shape.FieldDeformationMode.NONE) {
                float deformIntensity = state.getFloat("rays.fieldDeformationIntensity");
                var deformIntensitySlider = GuiWidgets.slider(x, y, w,
                    "Deform Int", 0f, 2f, deformIntensity, "%.2f", 
                    "Strength of gravitational stretch (0 = none, 1 = strong)",
                    v -> onUserChange(() -> state.set("rays.fieldDeformationIntensity", v)));
                widgets.add(deformIntensitySlider);
                y += step;
            }
        }
        
        // Track content height
        contentHeight = y - startY;
        
        // Performance warning
        ShapePerformanceHint.compute(state, shapeType, warningCallback);
        
        // Reapply bounds offset if rebuilding
        if (needsOffset) {
            applyBoundsOffset();
        }
        
        Logging.GUI.topic("panel").debug("Built {} controls for shape: {}", widgets.size(), shapeType);
        
        // Notify parent container that widgets have changed
        notifyWidgetsChanged();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PATTERN CONTROLS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private int buildPatternControls(int x, int y, int w, String shapeType) {
        patternFaces = null;
        patternBody = null;
        patternTop = null;
        patternBottom = null;
        
        ArrangementConfig arr = state.arrangement();
        String shape = shapeType.toLowerCase();
        int step = GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        
        // Rays are lines - they don't have faces that need patterns
        if (shape.equals("rays")) {
            return y; // No patterns for rays
        }
        
        // Shapes with per-part patterns (body, top, bottom)
        if (shape.equals("cylinder") || shape.equals("prism") || shape.equals("beam") || shape.equals("jet")) {
            int padding = 2;
            int btnWidth = (w - padding * 2) / 3;
            
            patternBody = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal("B " + capitalize(v)))
                .values(PATTERN_OPTIONS)
                .initially(arr.getPattern("sides"))
                .tooltip(v -> net.minecraft.client.gui.tooltip.Tooltip.of(net.minecraft.text.Text.literal("Body pattern: " + v)))
                .omitKeyText()
                .build(x, y, btnWidth, GuiConstants.COMPACT_HEIGHT, net.minecraft.text.Text.empty(),
                    (btn, val) -> onPatternChanged("sides", val));
            widgets.add(patternBody);
            
            patternTop = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal("T " + capitalize(v)))
                .values(PATTERN_OPTIONS)
                .initially(arr.getPattern("capTop"))
                .tooltip(v -> net.minecraft.client.gui.tooltip.Tooltip.of(net.minecraft.text.Text.literal("Top cap pattern: " + v)))
                .omitKeyText()
                .build(x + btnWidth + padding, y, btnWidth, GuiConstants.COMPACT_HEIGHT, net.minecraft.text.Text.empty(),
                    (btn, val) -> onPatternChanged("capTop", val));
            widgets.add(patternTop);
            
            patternBottom = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal("B " + capitalize(v)))
                .values(PATTERN_OPTIONS)
                .initially(arr.getPattern("capBottom"))
                .tooltip(v -> net.minecraft.client.gui.tooltip.Tooltip.of(net.minecraft.text.Text.literal("Bottom cap pattern: " + v)))
                .omitKeyText()
                .build(x + (btnWidth + padding) * 2, y, btnWidth, GuiConstants.COMPACT_HEIGHT, net.minecraft.text.Text.empty(),
                    (btn, val) -> onPatternChanged("capBottom", val));
            widgets.add(patternBottom);
            
            y += step;
        } else {
            // Single faces pattern button
            patternFaces = CyclingButtonWidget.<String>builder(v -> net.minecraft.text.Text.literal("Pattern " + capitalize(v)))
                .values(PATTERN_OPTIONS)
                .initially(arr.getPattern("faces"))
                .tooltip(v -> net.minecraft.client.gui.tooltip.Tooltip.of(net.minecraft.text.Text.literal("Face pattern: " + v)))
                .omitKeyText()
                .build(x, y, w, GuiConstants.COMPACT_HEIGHT, net.minecraft.text.Text.empty(),
                    (btn, val) -> onPatternChanged("faces", val));
            widgets.add(patternFaces);
            
            y += step;
        }
        
        return y;
    }
    
    private void onPatternChanged(String partName, String patternName) {
        ArrangementConfig current = state.arrangement();
        ArrangementConfig.Builder builder = current.toBuilder();
        
        switch (partName) {
            case "faces" -> {
                builder.defaultPattern(patternName);
                builder.faces(patternName);
                builder.main(patternName);
                builder.surface(patternName);
            }
            case "sides" -> builder.sides(patternName);
            case "capTop" -> builder.capTop(patternName);
            case "capBottom" -> builder.capBottom(patternName);
        }
        
        state.set("arrangement", builder.build());
        Logging.GUI.topic("panel").debug("Pattern changed: {} = {}", partName, patternName);
    }
    
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        var tr = MinecraftClient.getInstance().textRenderer;
        
        int bx = bounds.x();
        int by = bounds.y();
        
        // Title bar (rendered above scroll area)
        int titleX = bx + GuiConstants.PADDING;
        int titleY = by + (TITLE_HEIGHT - 8) / 2;
        context.drawTextWithShadow(tr, "Shape", titleX, titleY, 0xFF88AACC);
        
        // Render widgets with scroll support (handles scissor + offset + restore)
        renderWithScroll(context, mouseX, mouseY, delta);
    }
    
    public int getHeight() {
        return contentHeight;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void onUserChange(Runnable r) {
        r.run();
        if (!applyingFragment) {
            currentFragment = "Custom";
            if (fragmentDropdown != null && FragmentRegistry.listShapeFragments(currentShapeType).contains("Custom")) {
                fragmentDropdown.setValue("Custom");
            }
        }
        ShapePerformanceHint.compute(state, currentShapeType, warningCallback);
    }
    
    private void applyPreset(String name) {
        if ("Custom".equalsIgnoreCase(name)) {
            currentFragment = "Custom";
            return;
        }
        applyingFragment = true;
        currentFragment = name;
        
        if ("Default".equalsIgnoreCase(name)) {
            // Apply actual default shape config for current shape type
            net.cyberpunk042.visual.shape.Shape defaultShape = 
                net.cyberpunk042.field.loader.DefaultsProvider.getDefaultShape(currentShapeType);
            state.set("shape", defaultShape);
        } else {
            FragmentRegistry.applyShapeFragment(state, currentShapeType, name);
        }
        
        // Rebuild widgets to show correct fields for new values (like FillSubPanel and VisibilitySubPanel do)
        rebuildForCurrentShape();
        notifyWidgetsChanged();
        applyingFragment = false;
    }
}
