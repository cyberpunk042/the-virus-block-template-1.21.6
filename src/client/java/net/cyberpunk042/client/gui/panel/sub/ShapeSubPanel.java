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
        // SPECIAL: Sphere Quad Pattern Dropdown
        // ═══════════════════════════════════════════════════════════════════════
        
        if (shapeType.equalsIgnoreCase("sphere") || shapeType.equalsIgnoreCase("ring")) {
            var quadPatternDropdown = GuiWidgets.enumDropdown(
                x, y, w, GuiConstants.COMPACT_HEIGHT, "Quad Pattern",
                net.cyberpunk042.visual.pattern.QuadPattern.class,
                net.cyberpunk042.visual.pattern.QuadPattern.FILLED_1,
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
        // SPECIAL: Rays Line Shape + Curvature (Conditional Controls)
        // ═══════════════════════════════════════════════════════════════════════
        
        if (shapeType.equalsIgnoreCase("rays")) {
            // Get current line shape and curvature values
            String lineShapeStr = state.getString("rays.lineShape");
            net.cyberpunk042.visual.shape.RayLineShape lineShape;
            try {
                lineShape = net.cyberpunk042.visual.shape.RayLineShape.valueOf(
                    lineShapeStr != null ? lineShapeStr : "STRAIGHT");
            } catch (IllegalArgumentException e) {
                lineShape = net.cyberpunk042.visual.shape.RayLineShape.STRAIGHT;
            }
            
            String curvatureStr = state.getString("rays.curvature");
            net.cyberpunk042.visual.shape.RayCurvature curvature;
            try {
                curvature = net.cyberpunk042.visual.shape.RayCurvature.valueOf(
                    curvatureStr != null ? curvatureStr : "NONE");
            } catch (IllegalArgumentException e) {
                curvature = net.cyberpunk042.visual.shape.RayCurvature.NONE;
            }
            
            // === LINE SHAPE SECTION ===
            // Line Shape dropdown (full width)
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
            
            // Conditional: Show amplitude/frequency/segments only when lineShape != STRAIGHT
            if (lineShape != net.cyberpunk042.visual.shape.RayLineShape.STRAIGHT) {
                // Row: Amplitude + Frequency
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
                
                // Row: Shape Segments (full width)
                int segs = state.getInt("rays.shapeSegments");
                var segsSlider = GuiWidgets.slider(x, y, w,
                    "Line Segments", 4, 64, segs, "%d", 
                    "Smoothness of curved rays (more = smoother)",
                    v -> onUserChange(() -> state.set("rays.shapeSegments", Math.round(v))));
                widgets.add(segsSlider);
                y += step;
            }
            
            // === FIELD CURVATURE SECTION ===
            // Curvature dropdown (full width)
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
                float intensity = state.getFloat("rays.curvatureIntensity");
                var intensitySlider = GuiWidgets.slider(x, y, w,
                    "Intensity", 0f, 1f, intensity, "%.2f", 
                    "Strength of curvature effect (0 = straight, 1 = max curve)",
                    v -> onUserChange(() -> state.set("rays.curvatureIntensity", v)));
                widgets.add(intensitySlider);
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
