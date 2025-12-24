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
 *   <li>Sphere, Ring, Disc, Prism, Cylinder</li>
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
        
        // Track content height
        contentHeight = y - startY;
        
        // Performance warning
        ShapePerformanceHint.compute(state, shapeType, warningCallback);
        
        // Reapply bounds offset if rebuilding
        if (needsOffset) {
            applyBoundsOffset();
        }
        
        Logging.GUI.topic("panel").debug("Built {} controls for shape: {}", widgets.size(), shapeType);
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
        
        // Shapes with per-part patterns (body, top, bottom)
        if (shape.equals("cylinder") || shape.equals("prism") || shape.equals("beam")) {
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
