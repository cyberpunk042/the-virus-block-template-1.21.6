package net.cyberpunk042.client.gui.state;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.cyberpunk042.field.BeamConfig;
import net.cyberpunk042.field.influence.BindingConfig;
import net.cyberpunk042.field.Modifiers;
import net.cyberpunk042.field.instance.FollowMode;
import net.cyberpunk042.field.instance.FollowModeConfig;
import net.cyberpunk042.field.instance.PredictionConfig;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.field.primitive.PrimitiveLink;
import net.cyberpunk042.field.loader.SimplePrimitive;
import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.visual.layer.BlendMode;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.*;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.shape.*;
import net.cyberpunk042.visual.transform.OrbitConfig;
import net.cyberpunk042.visual.transform.Transform;
import net.cyberpunk042.visual.visibility.VisibilityMask;

import java.util.ArrayList;
import java.util.List;

/**
 * G02: Manages the shared editing state for both GUI and /field commands.
 * 
 * <p>Uses @StateField annotation + StateAccessor for reflection-based access,
 * eliminating hundreds of lines of repetitive getter/setter boilerplate.</p>
 * 
 * @see StateField
 * @see StateAccessor
 */
public class FieldEditState {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE RECORDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField @PrimitiveComponent(value = "shape", shapeSpecific = true)
    private SphereShape sphere = SphereShape.DEFAULT;
    @StateField @PrimitiveComponent(value = "shape", shapeSpecific = true)
    private RingShape ring = RingShape.DEFAULT;
    @StateField @PrimitiveComponent(value = "shape", shapeSpecific = true)
    private DiscShape disc = DiscShape.DEFAULT;
    @StateField @PrimitiveComponent(value = "shape", shapeSpecific = true)
    private PrismShape prism = PrismShape.builder().build();
    @StateField @PrimitiveComponent(value = "shape", shapeSpecific = true)
    private CylinderShape cylinder = CylinderShape.builder().build();
    @StateField @PrimitiveComponent(value = "shape", shapeSpecific = true)
    private PolyhedronShape polyhedron = PolyhedronShape.DEFAULT;
    @StateField @PrimitiveComponent(value = "shape", shapeSpecific = true)
    private TorusShape torus = TorusShape.DEFAULT;
    @StateField @PrimitiveComponent(value = "shape", shapeSpecific = true)
    private CapsuleShape capsule = CapsuleShape.DEFAULT;
    @StateField @PrimitiveComponent(value = "shape", shapeSpecific = true)
    private ConeShape cone = ConeShape.DEFAULT;
    
    @StateField public String shapeType = "sphere";
    @StateField public float radius = 3.0f;
    
    public Shape currentShape() {
        return switch (shapeType.toLowerCase()) {
            case "sphere" -> sphere;
            case "ring" -> ring;
            case "disc" -> disc;
            case "prism" -> prism;
            case "cylinder" -> cylinder;
            case "polyhedron", "cube", "octahedron", "icosahedron", 
                 "tetrahedron", "dodecahedron" -> polyhedron;
            case "torus" -> torus;
            case "capsule" -> capsule;
            case "cone" -> cone;
            default -> sphere;
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TRANSFORM & ORBIT
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField @PrimitiveComponent("transform")
    private Transform transform = Transform.IDENTITY;
    @StateField private OrbitConfig orbit = OrbitConfig.NONE;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // APPEARANCE & FILL
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField @PrimitiveComponent("fill")
    private FillConfig fill = FillConfig.SOLID;
    @StateField @PrimitiveComponent("mask")
    private VisibilityMask mask = VisibilityMask.FULL;
    @StateField @PrimitiveComponent("arrangement")
    private ArrangementConfig arrangement = ArrangementConfig.DEFAULT;
    @StateField @PrimitiveComponent("appearance")
    private AppearanceState appearance = AppearanceState.DEFAULT;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATION (primitive-level animations)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField @PrimitiveComponent("spin")
    private SpinConfig spin = SpinConfig.NONE;
    @StateField @PrimitiveComponent("pulse")
    private PulseConfig pulse = PulseConfig.NONE;
    @StateField @PrimitiveComponent("alphaPulse")
    private AlphaPulseConfig alphaPulse = AlphaPulseConfig.NONE;
    @StateField @PrimitiveComponent("wobble")
    private WobbleConfig wobble = WobbleConfig.NONE;
    @StateField @PrimitiveComponent("wave")
    private WaveConfig wave = WaveConfig.NONE;
    @StateField @PrimitiveComponent("colorCycle")
    private ColorCycleConfig colorCycle = ColorCycleConfig.NONE;
    @StateField @DefinitionField("modifiers")
    private Modifiers modifiers = Modifiers.DEFAULT;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FOLLOW & PREDICTION (definition-level)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField @DefinitionField("followMode")
    private FollowModeConfig followConfig = FollowModeConfig.DEFAULT;
    @StateField @DefinitionField("prediction")
    private PredictionConfig prediction = PredictionConfig.DEFAULT;
    @StateField public FollowMode followMode = FollowMode.SMOOTH;
    @StateField public boolean predictionEnabled = true;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LINKING & BEAM (definition-level)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private PrimitiveLink link = null;
    @StateField @DefinitionField("beam")
    private BeamConfig beam = BeamConfig.NONE;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FILL EXTRAS (cage, point size)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField public float pointSize = 2f;
    // Note: cageLatCount/cageLonCount removed - now handled by CageOptionsAdapter via fill().cage()
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LINKING STATE (legacy fields for primitive linking)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private String primitiveId = "";
    @StateField private float radiusOffset = 0f;
    @StateField private float phaseOffset = 0f;
    @StateField private String mirrorAxis = "NONE";
    @StateField private boolean followLinked = false;
    @StateField private boolean scaleWithLinked = false;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FOLLOW ENABLED
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField public boolean followEnabled = true;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private String lifecycleState = "ACTIVE";
    @StateField private int fadeInTicks = 0;
    @StateField private int fadeOutTicks = 0;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TRIGGER STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private String triggerType = "NONE";
    @StateField private String triggerEffect = "PULSE";
    @StateField private float triggerIntensity = 1.0f;
    @StateField private int triggerDuration = 20;
    
    // Active trigger processor for preview
    private final net.cyberpunk042.field.influence.TriggerProcessor triggerProcessor = 
        new net.cyberpunk042.field.influence.TriggerProcessor(java.util.List.of());
    
    /**
     * Fires a test trigger for preview purposes.
     * The trigger will be rendered in the GUI preview.
     */
    public void fireTestTrigger(net.cyberpunk042.field.influence.TriggerConfig config) {
        // Add the trigger config temporarily and fire it
        triggerProcessor.fireEvent(config.event());
        // Note: Since we create a temp processor, we directly add an active trigger
        // For now, use the effect values directly for preview
        this.testTriggerConfig = config;
        this.testTriggerStartTime = System.currentTimeMillis();
        net.cyberpunk042.log.Logging.GUI.topic("trigger").debug(
            "Test trigger started: {} -> {} for {}ms", 
            config.event(), config.effect(), config.duration() * 50);
    }
    
    // Transient test trigger state (not serialized)
    private transient net.cyberpunk042.field.influence.TriggerConfig testTriggerConfig;
    private transient long testTriggerStartTime;
    
    /**
     * Gets the active test trigger config if still active.
     */
    public net.cyberpunk042.field.influence.TriggerConfig getActiveTestTrigger() {
        if (testTriggerConfig == null) return null;
        long elapsed = System.currentTimeMillis() - testTriggerStartTime;
        long durationMs = testTriggerConfig.duration() * 50L; // ticks to ms
        if (elapsed > durationMs) {
            testTriggerConfig = null;
            return null;
        }
        return testTriggerConfig;
    }
    
    /**
     * Gets the test trigger progress (0-1, or -1 if no active trigger).
     */
    public float getTestTriggerProgress() {
        if (testTriggerConfig == null) return -1f;
        long elapsed = System.currentTimeMillis() - testTriggerStartTime;
        long durationMs = testTriggerConfig.duration() * 50L;
        if (elapsed > durationMs) {
            testTriggerConfig = null;
            return -1f;
        }
        return elapsed / (float) durationMs;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYER MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    private int selectedLayerIndex = 0;
    private int selectedPrimitiveIndex = 0;
    private List<FieldLayer> fieldLayers = new ArrayList<>();
    private List<Integer> selectedPrimitivePerLayer = new ArrayList<>();
    
    {
        // Initialize with one default layer containing one default primitive
        Shape defaultShape = SphereShape.builder().radius(1.0f).latSteps(32).lonSteps(64).build();
        Primitive defaultPrimitive = SimplePrimitive.of("primitive_1", "sphere", defaultShape);
        FieldLayer defaultLayer = FieldLayer.of("Layer 1", List.of(defaultPrimitive));
        fieldLayers.add(defaultLayer);
        selectedPrimitivePerLayer.add(0);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GUI SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField public boolean livePreviewEnabled = true;
    @StateField public boolean autoSaveEnabled = false;
    @StateField public boolean debugUnlocked = false;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CORE STATE (dirty tracking, profiles)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private boolean isDirty = false;
    private String currentProfileName = "default";
    private boolean currentProfileServer = false;
    private String snapshotJson = null;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROFILE ENTRY (for profile list display)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public record ProfileEntry(String name, boolean isServer, String description) {
        public ProfileEntry(String name, boolean isServer) {
            this(name, isServer, "");
        }
    }
    
    public FieldEditState() {
        Logging.GUI.topic("state").debug("FieldEditState created");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DIRTY TRACKING & CHANGE LISTENERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final java.util.List<Runnable> changeListeners = new java.util.ArrayList<>();
    
    public boolean isDirty() { return isDirty; }
    
    public void markDirty() { 
        isDirty = true; 
        notifyListeners();
    }
    
    public void clearDirty() { isDirty = false; }
    
    /** Add a listener called when state changes. */
    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }
    
    /** Remove a change listener. */
    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }
    
    private void notifyListeners() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                Logging.GUI.topic("state").error("Error in state change listener", e);
            }
        }
    }
    
    /**
     * Reset all fields to their default values.
     */
    public void reset() {
        // Reset shape records
        this.sphere = SphereShape.DEFAULT;
        this.ring = RingShape.DEFAULT;
        this.disc = DiscShape.DEFAULT;
        this.prism = PrismShape.builder().build();
        this.cylinder = CylinderShape.builder().build();
        this.polyhedron = PolyhedronShape.DEFAULT;
        this.torus = TorusShape.DEFAULT;
        this.capsule = CapsuleShape.DEFAULT;
        this.cone = ConeShape.DEFAULT;
        this.shapeType = "sphere";
        this.radius = 3.0f;
        
        // Reset transform & orbit
        this.transform = Transform.IDENTITY;
        this.orbit = OrbitConfig.NONE;
        
        // Reset appearance & fill
        this.fill = FillConfig.SOLID;
        this.mask = VisibilityMask.FULL;
        this.arrangement = ArrangementConfig.DEFAULT;
        this.appearance = AppearanceState.DEFAULT;
        
        // Reset animation
        this.spin = SpinConfig.NONE;
        this.pulse = PulseConfig.NONE;
        this.alphaPulse = AlphaPulseConfig.NONE;
        this.wobble = WobbleConfig.NONE;
        this.wave = WaveConfig.NONE;
        this.colorCycle = ColorCycleConfig.NONE;
        this.modifiers = Modifiers.DEFAULT;
        
        // Reset follow & prediction
        this.followConfig = FollowModeConfig.DEFAULT;
        this.prediction = PredictionConfig.DEFAULT;
        this.followMode = FollowMode.SMOOTH;
        this.followEnabled = true;
        this.predictionEnabled = true;
        
        // Reset layers/primitives to single default
        this.fieldLayers.clear();
        Shape defaultShape = SphereShape.builder().radius(1.0f).latSteps(32).lonSteps(64).build();
        Primitive defaultPrimitive = SimplePrimitive.of("primitive_1", "sphere", defaultShape);
        this.fieldLayers.add(FieldLayer.of("Layer 1", List.of(defaultPrimitive)));
        this.selectedLayerIndex = 0;
        this.selectedPrimitivePerLayer.clear();
        this.selectedPrimitivePerLayer.add(0);
        
        markDirty();
        Logging.GUI.topic("state").info("FieldEditState reset to defaults");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GENERIC ACCESSORS (via StateAccessor reflection)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Get a state record by type. */
    public <T> T get(Class<T> type) { 
        return StateAccessor.get(this, type); 
    }
    
    /** Get a state record by name. */
    public <T> T get(String name, Class<T> type) { 
        return StateAccessor.get(this, name, type); 
    }
    
    /** Set a state record (auto-detects field by type). */
    public <T> void set(T value) { 
        StateAccessor.set(this, value); 
        markDirty(); 
    }
    
    /** 
     * Set a state field or nested property using dot notation.
     * <pre>
     * set("radius", 5.0f);           // Direct field
     * set("spin.speed", 0.5f);       // Record property (auto toBuilder)
     * set("beam.pulse.speed", 1.0f); // Nested record
     * </pre>
     */
    public void set(String path, Object value) { 
        StateAccessor.set(this, path, value);
        
        // CP1: GUI sets value in state - trace the path to segment mapping
        traceCP1(path, value);
        
        markDirty(); 
    }
    
    /**
     * Traces CP1 (GUI → State) for the given path.
     */
    private void traceCP1(String path, Object value) {
        if (!PipelineTracer.isEnabled()) return;
        
        String segment = mapPathToSegment(path);
        if (segment != null) {
            PipelineTracer.trace(segment, 1, path, value);
        }
    }
    
    /**
     * Maps a state path to a PipelineTracer segment ID.
     * Comprehensive mapping for ALL 100+ segments.
     */
    private static String mapPathToSegment(String path) {
        return switch (path) {
            // =====================================================================
            // APPEARANCE (A1-A11)
            // =====================================================================
            case "appearance.primaryColor", "primaryColor" -> PipelineTracer.A1_PRIMARY_COLOR;
            case "appearance.alpha" -> PipelineTracer.A2_ALPHA;
            case "appearance.glow" -> PipelineTracer.A3_GLOW;
            case "appearance.emissive" -> PipelineTracer.A4_EMISSIVE;
            case "appearance.saturation" -> PipelineTracer.A5_SATURATION;
            case "appearance.secondaryColor", "secondaryColor" -> PipelineTracer.A6_SECONDARY_COLOR;
            case "appearance.brightness" -> PipelineTracer.A7_BRIGHTNESS;
            case "appearance.hueShift" -> PipelineTracer.A8_HUE_SHIFT;
            case "appearance.colorBlend" -> PipelineTracer.A9_COLOR_BLEND;
            case "appearance.alpha.min", "alpha.min" -> PipelineTracer.A10_ALPHA_MIN;
            case "appearance.alpha.max", "alpha.max" -> PipelineTracer.A11_ALPHA_MAX;
            
            // =====================================================================
            // SHAPE (S1-S18)
            // =====================================================================
            case "shapeType" -> PipelineTracer.S1_SHAPE_TYPE;
            case "radius", "sphere.radius", "disc.radius", "cylinder.radius", "prism.radius" -> PipelineTracer.S2_RADIUS;
            case "sphere.latSteps", "latSteps" -> PipelineTracer.S3_LAT_STEPS;
            case "sphere.lonSteps", "lonSteps" -> PipelineTracer.S4_LON_STEPS;
            case "sphere.algorithm", "algorithm" -> PipelineTracer.S5_ALGORITHM;
            case "ring.innerRadius", "disc.innerRadius", "innerRadius" -> PipelineTracer.S6_INNER_RADIUS;
            case "ring.outerRadius", "outerRadius" -> PipelineTracer.S7_OUTER_RADIUS;
            case "ring.height", "cylinder.height", "prism.height", "height" -> PipelineTracer.S8_HEIGHT;
            case "ring.segments", "cylinder.segments", "disc.segments", "segments" -> PipelineTracer.S9_SEGMENTS;
            case "prism.sides", "sides" -> PipelineTracer.S10_SIDES;
            case "polyhedron.type", "polyType" -> PipelineTracer.S11_POLY_TYPE;
            case "polyhedron.faceCount", "faceCount" -> PipelineTracer.S12_FACE_COUNT;
            case "cylinder.openTop", "openTop" -> PipelineTracer.S13_OPEN_TOP;
            case "cylinder.openBottom", "openBottom" -> PipelineTracer.S14_OPEN_BOTTOM;
            case "ring.thickness", "thickness" -> PipelineTracer.S15_THICKNESS;
            case "hollow" -> PipelineTracer.S16_HOLLOW;
            case "ringsCount" -> PipelineTracer.S17_RINGS_COUNT;
            case "stacksCount" -> PipelineTracer.S18_STACKS_COUNT;
            
            // =====================================================================
            // FILL (F1-F6)
            // =====================================================================
            case "fill.mode", "fillMode" -> PipelineTracer.F1_FILL_MODE;
            case "fill.wireThickness", "wireThickness" -> PipelineTracer.F2_WIRE_THICKNESS;
            case "fill.doubleSided", "doubleSided" -> PipelineTracer.F3_DOUBLE_SIDED;
            case "fill.depthTest", "depthTest" -> PipelineTracer.F4_DEPTH_TEST;
            case "fill.depthWrite", "depthWrite" -> PipelineTracer.F5_DEPTH_WRITE;
            case "fill.cage", "cage", "cageOptions" -> PipelineTracer.F6_CAGE_OPTIONS;
            
            // =====================================================================
            // ANIMATION (N1-N16)
            // =====================================================================
            case "spin.speed", "spinSpeed" -> PipelineTracer.N1_SPIN_SPEED;
            case "spin.axis", "spinAxis" -> PipelineTracer.N2_SPIN_AXIS;
            case "pulse.speed", "pulseSpeed" -> PipelineTracer.N3_PULSE_SPEED;
            case "pulse.scale", "pulseScale" -> PipelineTracer.N4_PULSE_SCALE;
            case "pulse.mode", "pulseMode" -> PipelineTracer.N5_PULSE_MODE;
            case "alphaPulse.speed", "alphaPulseSpeed" -> PipelineTracer.N6_ALPHA_PULSE_SPEED;
            case "alphaPulse.min", "alphaPulseMin" -> PipelineTracer.N7_ALPHA_PULSE_MIN;
            case "alphaPulse.max", "alphaPulseMax" -> PipelineTracer.N8_ALPHA_PULSE_MAX;
            case "wave.frequency", "wave.speed", "waveFrequency" -> PipelineTracer.N9_WAVE_SPEED;
            case "wave.amplitude", "waveAmplitude" -> PipelineTracer.N10_WAVE_AMPLITUDE;
            case "wobble.speed", "wobbleSpeed" -> PipelineTracer.N11_WOBBLE_SPEED;
            case "colorCycle.active", "colorCycleActive" -> PipelineTracer.N12_COLOR_CYCLE;
            case "phase", "animation.phase" -> PipelineTracer.N13_PHASE;
            case "wobble.amplitude", "wobbleAmplitude" -> PipelineTracer.N14_WOBBLE_AMPLITUDE;
            case "colorCycle.colors" -> PipelineTracer.N15_COLOR_CYCLE_COLORS;
            case "colorCycle.speed" -> PipelineTracer.N16_COLOR_CYCLE_SPEED;
            
            // =====================================================================
            // TRANSFORM (T1-T13)
            // =====================================================================
            case "transform.offset", "transform.offsetX", "transform.offsetY", "transform.offsetZ", "offset" -> PipelineTracer.T1_OFFSET;
            case "transform.rotation", "transform.rotX", "transform.rotY", "transform.rotZ", "rotation" -> PipelineTracer.T2_ROTATION;
            case "transform.scale", "scale" -> PipelineTracer.T3_SCALE;
            case "transform.scaleX", "transform.scaleY", "transform.scaleZ", "scaleXYZ" -> PipelineTracer.T4_SCALE_XYZ;
            case "transform.anchor", "anchor" -> PipelineTracer.T5_ANCHOR;
            case "transform.billboard", "billboard" -> PipelineTracer.T6_BILLBOARD;
            case "transform.orbit", "orbit" -> PipelineTracer.T7_ORBIT;
            case "transform.inheritRotation", "inheritRotation" -> PipelineTracer.T8_INHERIT_ROTATION;
            case "transform.scaleWithRadius", "scaleWithRadius" -> PipelineTracer.T9_SCALE_WITH_RADIUS;
            case "transform.facing", "facing" -> PipelineTracer.T10_FACING;
            case "transform.up", "upVector" -> PipelineTracer.T11_UP_VECTOR;
            case "orbit.radius", "orbitRadius" -> PipelineTracer.T12_ORBIT_RADIUS;
            case "orbit.speed", "orbitSpeed" -> PipelineTracer.T13_ORBIT_SPEED;
            
            // =====================================================================
            // VISIBILITY/MASK (V1-V14)
            // =====================================================================
            case "mask.type", "visibility.mask", "maskType" -> PipelineTracer.V1_MASK_TYPE;
            case "mask.count", "visibility.count", "maskCount" -> PipelineTracer.V2_MASK_COUNT;
            case "mask.thickness", "visibility.thickness", "maskThickness" -> PipelineTracer.V3_MASK_THICKNESS;
            case "mask.offset", "visibility.offset", "maskOffset" -> PipelineTracer.V4_MASK_OFFSET;
            case "mask.animate", "visibility.animate", "maskAnimate" -> PipelineTracer.V5_MASK_ANIMATE;
            case "mask.animSpeed", "visibility.animSpeed", "maskAnimSpeed" -> PipelineTracer.V6_MASK_ANIM_SPEED;
            case "mask.invert", "visibility.invert", "maskInvert" -> PipelineTracer.V7_MASK_INVERT;
            case "mask.feather", "visibility.feather", "maskFeather" -> PipelineTracer.V8_MASK_FEATHER;
            case "mask.direction", "visibility.direction" -> PipelineTracer.V9_MASK_DIRECTION;
            case "mask.falloff", "visibility.falloff" -> PipelineTracer.V10_MASK_FALLOFF;
            case "mask.gradientStart", "gradientStart" -> PipelineTracer.V11_GRADIENT_START;
            case "mask.gradientEnd", "gradientEnd" -> PipelineTracer.V12_GRADIENT_END;
            case "mask.centerX", "visibility.centerX" -> PipelineTracer.V13_CENTER_X;
            case "mask.centerY", "visibility.centerY" -> PipelineTracer.V14_CENTER_Y;
            
            // =====================================================================
            // LAYER (L1-L6)
            // =====================================================================
            case "layer.alpha", "selectedLayer.alpha", "layerAlpha" -> PipelineTracer.L1_LAYER_ALPHA;
            case "layer.visible", "selectedLayer.visible", "layerVisible" -> PipelineTracer.L2_LAYER_VISIBLE;
            case "layer.blendMode", "selectedLayer.blendMode", "blendMode" -> PipelineTracer.L3_BLEND_MODE;
            case "layer.id", "layerId" -> PipelineTracer.L4_LAYER_ID;
            case "layer.transform" -> PipelineTracer.L5_LAYER_TRANSFORM;
            case "layer.animation" -> PipelineTracer.L6_LAYER_ANIMATION;
            
            // =====================================================================
            // FIELD-LEVEL (D1-D7)
            // =====================================================================
            case "modifiers.bobbing", "bobbing" -> PipelineTracer.D1_BOBBING;
            case "modifiers.breathing", "breathing" -> PipelineTracer.D2_BREATHING;
            case "prediction.enabled", "prediction" -> PipelineTracer.D3_PREDICTION;
            case "followMode" -> PipelineTracer.D4_FOLLOW_MODE;
            case "baseRadius" -> PipelineTracer.D5_BASE_RADIUS;
            case "fieldType", "type" -> PipelineTracer.D6_FIELD_TYPE;
            case "themeId" -> PipelineTracer.D7_THEME_ID;
            
            // =====================================================================
            // MODIFIERS (M1-M9)
            // =====================================================================
            case "modifiers.radiusMultiplier", "radiusMultiplier" -> PipelineTracer.M1_RADIUS_MULT;
            case "modifiers.strengthMultiplier", "strengthMultiplier" -> PipelineTracer.M2_STRENGTH_MULT;
            case "modifiers.alphaMultiplier", "alphaMultiplier" -> PipelineTracer.M3_ALPHA_MULT;
            case "modifiers.spinMultiplier", "spinMultiplier" -> PipelineTracer.M4_SPIN_MULT;
            case "modifiers.visualScale", "visualScale" -> PipelineTracer.M5_VISUAL_SCALE;
            case "modifiers.tiltMultiplier", "tiltMultiplier" -> PipelineTracer.M6_TILT_MULT;
            case "modifiers.swirlStrength", "swirlStrength" -> PipelineTracer.M7_SWIRL_STRENGTH;
            case "modifiers.inverted", "inverted" -> PipelineTracer.M8_INVERTED;
            case "modifiers.pulsing" -> PipelineTracer.M9_PULSING;
            
            // =====================================================================
            // BEAM (B1-B7)
            // =====================================================================
            case "beam.enabled", "beamEnabled" -> PipelineTracer.B1_BEAM_ENABLED;
            case "beam.innerRadius", "beamInnerRadius" -> PipelineTracer.B2_BEAM_INNER_RADIUS;
            case "beam.outerRadius", "beamOuterRadius" -> PipelineTracer.B3_BEAM_OUTER_RADIUS;
            case "beam.color", "beamColor" -> PipelineTracer.B4_BEAM_COLOR;
            case "beam.height", "beamHeight" -> PipelineTracer.B5_BEAM_HEIGHT;
            case "beam.glow", "beamGlow" -> PipelineTracer.B6_BEAM_GLOW;
            case "beam.pulse", "beamPulse" -> PipelineTracer.B7_BEAM_PULSE;
            
            // =====================================================================
            // PRIMITIVE (P1-P4)
            // =====================================================================
            case "primitiveId", "primitive.id" -> PipelineTracer.P1_PRIMITIVE_ID;
            case "primitiveType", "primitive.type" -> PipelineTracer.P2_PRIMITIVE_TYPE;
            case "arrangement", "primitive.arrangement" -> PipelineTracer.P3_ARRANGEMENT;
            case "link", "primitive.link" -> PipelineTracer.P4_LINK;
            
            default -> null;
        };
    }
    
    /**
     * Set a value from a string, attempting to parse it to the correct type.
     * Supports: boolean, int, float, and enum values.
     * 
     * @param path  The state path (e.g., "fill.mode", "radius")
     * @param value The string representation of the value
     * @return true if successful, false if parsing failed
     */
    public boolean setFromString(String path, String value) {
        try {
            // Try to get current value to determine type
            Object current = get(path);
            Object parsed = parseValue(value, current);
            set(path, parsed);
            return true;
        } catch (Exception e) {
            net.cyberpunk042.log.Logging.GUI.topic("state").warn(
                "Failed to set {} = {}: {}", path, value, e.getMessage());
            return false;
        }
    }
    
    private Object parseValue(String value, Object currentValue) {
        if (currentValue == null) {
            // Guess from string content
            return parseGuess(value);
        }
        
        Class<?> type = currentValue.getClass();
        
        // Boolean
        if (type == Boolean.class || type == boolean.class) {
            return parseBool(value);
        }
        
        // Integer
        if (type == Integer.class || type == int.class) {
            return Integer.parseInt(value);
        }
        
        // Float
        if (type == Float.class || type == float.class) {
            return Float.parseFloat(value);
        }
        
        // Double
        if (type == Double.class || type == double.class) {
            return Double.parseDouble(value);
        }
        
        // Enum
        if (type.isEnum()) {
            return parseEnum(value, type);
        }
        
        // String
        return value;
    }
    
    private Object parseGuess(String value) {
        // Try boolean
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }
        // Try integer
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}
        // Try float
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {}
        // Default to string
        return value;
    }
    
    private boolean parseBool(String value) {
        return value.equalsIgnoreCase("true") 
            || value.equals("1") 
            || value.equalsIgnoreCase("on")
            || value.equalsIgnoreCase("yes");
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object parseEnum(String value, Class<?> enumClass) {
        // Try by name (case-insensitive)
        for (Object constant : enumClass.getEnumConstants()) {
            Enum<?> e = (Enum<?>) constant;
            if (e.name().equalsIgnoreCase(value)) {
                return e;
            }
        }
        // Try by id() method if present
        try {
            var fromIdMethod = enumClass.getMethod("fromId", String.class);
            Object result = fromIdMethod.invoke(null, value.toLowerCase());
            if (result != null) return result;
        } catch (Exception ignored) {}
        
        throw new IllegalArgumentException("Unknown enum value: " + value + " for " + enumClass.getSimpleName());
    }
    
    /** Update a record using a modifier function (for immutable records with toBuilder). */
    public <T> void update(String name, java.util.function.Function<T, T> modifier) {
        StateAccessor.update(this, name, modifier);
        markDirty();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PATH-BASED GETTERS (consistent with setters)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Get any value by path. */
    public Object get(String path) { return StateAccessor.get(this, path); }
    
    /** Get an int value by path. */
    public int getInt(String path) { return StateAccessor.getInt(this, path); }
    
    /** Get a float value by path. */
    public float getFloat(String path) { return StateAccessor.getFloat(this, path); }
    
    /** Get a boolean value by path. */
    public boolean getBool(String path) { return StateAccessor.getBool(this, path); }
    
    /** Get a String value by path (enums return their name). */
    public String getString(String path) { return StateAccessor.getString(this, path); }
    
    /** Get a typed value by path. */
    public <T> T getTyped(String path, Class<T> type) { return StateAccessor.getTyped(this, path, type); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TYPED RECORD ACCESSORS (kept for backward compatibility)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public SphereShape sphere() { return sphere; }
    public RingShape ring() { return ring; }
    public DiscShape disc() { return disc; }
    public PrismShape prism() { return prism; }
    public CylinderShape cylinder() { return cylinder; }
    public PolyhedronShape polyhedron() { return polyhedron; }
    public TorusShape torus() { return torus; }
    public CapsuleShape capsule() { return capsule; }
    public ConeShape cone() { return cone; }

    public Transform transform() { return transform; }
    public OrbitConfig orbit() { return orbit; }

    public FillConfig fill() { return fill; }
    public VisibilityMask mask() { return mask; }
    public ArrangementConfig arrangement() { return arrangement; }
    public AppearanceState appearance() { return appearance; }

    public SpinConfig spin() { return spin; }
    public PulseConfig pulse() { return pulse; }
    public AlphaPulseConfig alphaPulse() { return alphaPulse; }
    public WobbleConfig wobble() { return wobble; }
    public WaveConfig wave() { return wave; }
    public ColorCycleConfig colorCycle() { return colorCycle; }

    public FollowModeConfig followConfig() { return followConfig; }
    public PredictionConfig prediction() { return prediction; }
    public BeamConfig beam() { return beam; }
    public PrimitiveLink link() { return link; }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYER MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    public int getSelectedLayerIndex() { return selectedLayerIndex; }
    public void setSelectedLayerIndex(int index) { 
        this.selectedLayerIndex = Math.max(0, Math.min(index, fieldLayers.size() - 1));
        clampPrimitiveSelection(selectedLayerIndex);
    }
    
    public int getLayerCount() { return fieldLayers.size(); }
    public List<FieldLayer> getFieldLayers() { return fieldLayers; }
    
    /** Get the currently selected layer. */
    public FieldLayer getSelectedLayer() {
        if (fieldLayers.isEmpty()) return null;
        return fieldLayers.get(selectedLayerIndex);
    }
    
    /** Get the currently selected primitive. */
    public Primitive getSelectedPrimitive() {
        FieldLayer layer = getSelectedLayer();
        if (layer == null || layer.primitives().isEmpty()) return null;
        int primIdx = getSelectedPrimitiveIndex();
        if (primIdx < 0 || primIdx >= layer.primitives().size()) return null;
        return layer.primitives().get(primIdx);
    }
    
    public int addLayer() {
        if (fieldLayers.size() >= 10) return -1;
        String name = "Layer " + (fieldLayers.size() + 1);
        // Create default primitive for new layer
        Shape defaultShape = SphereShape.builder().radius(1.0f).latSteps(32).lonSteps(64).build();
        Primitive defaultPrimitive = SimplePrimitive.of("primitive_1", "sphere", defaultShape);
        FieldLayer newLayer = FieldLayer.of(name, List.of(defaultPrimitive));
        fieldLayers.add(newLayer);
        selectedPrimitivePerLayer.add(0);
        markDirty();
        return fieldLayers.size() - 1;
    }
    
    public boolean removeLayer(int index) {
        if (fieldLayers.size() <= 1 || index < 0 || index >= fieldLayers.size()) return false;
        fieldLayers.remove(index);
        selectedPrimitivePerLayer.remove(index);
        markDirty();
        return true;
    }
    
    public boolean swapLayers(int a, int b) {
        if (a < 0 || a >= fieldLayers.size() || b < 0 || b >= fieldLayers.size()) return false;
        java.util.Collections.swap(fieldLayers, a, b);
        java.util.Collections.swap(selectedPrimitivePerLayer, a, b);
        markDirty();
        return true;
    }
    
    public int addLayerWithName(String name) {
        if (fieldLayers.size() >= 10) return -1;
        String finalName = resolveLayerName(name);
        Shape defaultShape = SphereShape.builder().radius(1.0f).latSteps(32).lonSteps(64).build();
        Primitive defaultPrimitive = SimplePrimitive.of("primitive_1", "sphere", defaultShape);
        FieldLayer newLayer = FieldLayer.of(finalName, List.of(defaultPrimitive));
        fieldLayers.add(newLayer);
        selectedPrimitivePerLayer.add(0);
        markDirty();
        return fieldLayers.size() - 1;
    }
    
    public int findLayerByName(String name) {
        for (int i = 0; i < fieldLayers.size(); i++) {
            if (fieldLayers.get(i).id().equals(name)) return i;
        }
        return -1;
    }
    
    public String getLayerName(int index) {
        if (index < 0 || index >= fieldLayers.size()) return "";
        return fieldLayers.get(index).id();
    }
    
    /** Rename a layer (creates new FieldLayer since it's immutable). */
    public void renameLayer(int index, String newName) {
        if (index < 0 || index >= fieldLayers.size()) return;
        FieldLayer old = fieldLayers.get(index);
        FieldLayer renamed = new FieldLayer(
            newName, old.primitives(), old.transform(), old.animation(),
            old.alpha(), old.visible(), old.blendMode()
        );
        fieldLayers.set(index, renamed);
        markDirty();
    }

    /** Rename a primitive (creates new Primitive since it's immutable). */
    public void renamePrimitive(int layerIndex, int primitiveIndex, String newName) {
        if (!isValidPrimitiveIndex(layerIndex, primitiveIndex)) return;
        FieldLayer layer = fieldLayers.get(layerIndex);
        Primitive oldPrim = layer.primitives().get(primitiveIndex);

        // Create renamed primitive (SimplePrimitive is a record, so recreate with new id)
        Primitive renamedPrim;
        if (oldPrim instanceof net.cyberpunk042.field.loader.SimplePrimitive sp) {
            renamedPrim = new net.cyberpunk042.field.loader.SimplePrimitive(
                newName, sp.type(), sp.shape(), sp.transform(), sp.fill(),
                sp.visibility(), sp.arrangement(), sp.appearance(), sp.animation(), sp.link()
            );
        } else {
            // For other Primitive implementations, just log and skip
            Logging.GUI.warn("Cannot rename non-SimplePrimitive: {}", oldPrim.getClass().getSimpleName());
            return;
        }

        List<Primitive> updatedPrimitives = new ArrayList<>(layer.primitives());
        updatedPrimitives.set(primitiveIndex, renamedPrim);

        FieldLayer updatedLayer = new FieldLayer(
            layer.id(), updatedPrimitives, layer.transform(), layer.animation(),
            layer.alpha(), layer.visible(), layer.blendMode()
        );
        fieldLayers.set(layerIndex, updatedLayer);
        markDirty();
    }
    
    private String resolveLayerName(String baseName) {
        if (findLayerByName(baseName) < 0) return baseName;
        for (int i = 2; i <= 100; i++) {
            String candidate = baseName + " " + i;
            if (findLayerByName(candidate) < 0) return candidate;
        }
        return baseName + "_" + System.currentTimeMillis();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMITIVE MANAGEMENT (per layer)
    // ═══════════════════════════════════════════════════════════════════════════

    public int getPrimitiveCount(int layerIndex) {
        if (layerIndex < 0 || layerIndex >= fieldLayers.size()) return 0;
        return fieldLayers.get(layerIndex).primitives().size();
    }
    
    /** Get primitive IDs for a layer (for UI display). */
    public List<String> getPrimitivesForLayer(int layerIndex) {
        if (layerIndex < 0 || layerIndex >= fieldLayers.size()) return java.util.Collections.emptyList();
        return fieldLayers.get(layerIndex).primitives().stream()
            .map(Primitive::id)
            .toList();
    }
    
    /** Get actual Primitive objects for a layer. */
    public List<Primitive> getPrimitiveObjectsForLayer(int layerIndex) {
        if (layerIndex < 0 || layerIndex >= fieldLayers.size()) return java.util.Collections.emptyList();
        return fieldLayers.get(layerIndex).primitives();
    }

    public int getSelectedPrimitiveIndex() {
        if (selectedPrimitivePerLayer.isEmpty()) return 0;
        return selectedPrimitivePerLayer.get(selectedLayerIndex);
    }

    public void setSelectedPrimitiveIndex(int index) {
        if (selectedPrimitivePerLayer.isEmpty()) return;
        int clamped = clampPrimitiveIndex(selectedLayerIndex, index);
        selectedPrimitivePerLayer.set(selectedLayerIndex, clamped);
    }

    public int addPrimitive(int layerIndex) {
        if (layerIndex < 0 || layerIndex >= fieldLayers.size()) return -1;
        FieldLayer layer = fieldLayers.get(layerIndex);
        String id = "primitive_" + (layer.primitives().size() + 1);
        id = resolvePrimitiveId(layerIndex, id);
        
        // Create new primitive with default shape
        Shape defaultShape = SphereShape.builder().radius(1.0f).latSteps(32).lonSteps(64).build();
        Primitive newPrimitive = SimplePrimitive.of(id, "sphere", defaultShape);
        
        // Create updated primitives list
        List<Primitive> updatedPrimitives = new ArrayList<>(layer.primitives());
        updatedPrimitives.add(newPrimitive);
        
        // Create new layer with updated primitives (FieldLayer is immutable)
        FieldLayer updatedLayer = new FieldLayer(
            layer.id(), updatedPrimitives, layer.transform(), layer.animation(),
            layer.alpha(), layer.visible(), layer.blendMode()
        );
        fieldLayers.set(layerIndex, updatedLayer);
        
        selectedPrimitivePerLayer.set(layerIndex, updatedPrimitives.size() - 1);
        markDirty();
        return updatedPrimitives.size() - 1;
    }

    public boolean removePrimitive(int layerIndex, int primitiveIndex) {
        if (!isValidPrimitiveIndex(layerIndex, primitiveIndex)) return false;
        FieldLayer layer = fieldLayers.get(layerIndex);
        if (layer.primitives().size() <= 1) return false;
        
        List<Primitive> updatedPrimitives = new ArrayList<>(layer.primitives());
        updatedPrimitives.remove(primitiveIndex);
        
        FieldLayer updatedLayer = new FieldLayer(
            layer.id(), updatedPrimitives, layer.transform(), layer.animation(),
            layer.alpha(), layer.visible(), layer.blendMode()
        );
        fieldLayers.set(layerIndex, updatedLayer);
        
        clampPrimitiveSelection(layerIndex);
        markDirty();
        return true;
    }

    public boolean swapPrimitives(int layerIndex, int a, int b) {
        if (!isValidPrimitiveIndex(layerIndex, a) || !isValidPrimitiveIndex(layerIndex, b)) return false;
        FieldLayer layer = fieldLayers.get(layerIndex);
        
        List<Primitive> updatedPrimitives = new ArrayList<>(layer.primitives());
        java.util.Collections.swap(updatedPrimitives, a, b);
        
        FieldLayer updatedLayer = new FieldLayer(
            layer.id(), updatedPrimitives, layer.transform(), layer.animation(),
            layer.alpha(), layer.visible(), layer.blendMode()
        );
        fieldLayers.set(layerIndex, updatedLayer);
        
        int selected = getSelectedPrimitiveIndex();
        if (selected == a) selectedPrimitivePerLayer.set(layerIndex, b);
        else if (selected == b) selectedPrimitivePerLayer.set(layerIndex, a);
        markDirty();
        return true;
    }
    
    public int addPrimitiveWithId(int layerIndex, String id) {
        if (layerIndex < 0 || layerIndex >= fieldLayers.size()) return -1;
        FieldLayer layer = fieldLayers.get(layerIndex);
        String finalId = resolvePrimitiveId(layerIndex, id);
        
        Shape defaultShape = SphereShape.builder().radius(1.0f).latSteps(32).lonSteps(64).build();
        Primitive newPrimitive = SimplePrimitive.of(finalId, "sphere", defaultShape);
        
        List<Primitive> updatedPrimitives = new ArrayList<>(layer.primitives());
        updatedPrimitives.add(newPrimitive);
        
        FieldLayer updatedLayer = new FieldLayer(
            layer.id(), updatedPrimitives, layer.transform(), layer.animation(),
            layer.alpha(), layer.visible(), layer.blendMode()
        );
        fieldLayers.set(layerIndex, updatedLayer);
        
        selectedPrimitivePerLayer.set(layerIndex, updatedPrimitives.size() - 1);
        markDirty();
        return updatedPrimitives.size() - 1;
    }
    
    public int findPrimitiveById(int layerIndex, String id) {
        if (layerIndex < 0 || layerIndex >= fieldLayers.size()) return -1;
        List<Primitive> primitives = fieldLayers.get(layerIndex).primitives();
        for (int i = 0; i < primitives.size(); i++) {
            if (primitives.get(i).id().equals(id)) return i;
        }
        return -1;
    }
    
    public String getPrimitiveName(int layerIndex, int primitiveIndex) {
        if (!isValidPrimitiveIndex(layerIndex, primitiveIndex)) return "";
        return fieldLayers.get(layerIndex).primitives().get(primitiveIndex).id();
    }
    
    private String resolvePrimitiveId(int layerIndex, String baseId) {
        if (findPrimitiveById(layerIndex, baseId) < 0) return baseId;
        for (int i = 2; i <= 100; i++) {
            String candidate = baseId + "_" + i;
            if (findPrimitiveById(layerIndex, candidate) < 0) return candidate;
        }
        return baseId + "_" + System.currentTimeMillis();
    }

    private boolean isValidPrimitiveIndex(int layerIndex, int primitiveIndex) {
        if (layerIndex < 0 || layerIndex >= fieldLayers.size()) return false;
        List<Primitive> primitives = fieldLayers.get(layerIndex).primitives();
        return primitiveIndex >= 0 && primitiveIndex < primitives.size();
    }

    private int clampPrimitiveIndex(int layerIndex, int index) {
        if (layerIndex < 0 || layerIndex >= fieldLayers.size()) return 0;
        List<Primitive> primitives = fieldLayers.get(layerIndex).primitives();
        return Math.max(0, Math.min(index, Math.max(0, primitives.size() - 1)));
    }

    private void clampPrimitiveSelection(int layerIndex) {
        if (selectedPrimitivePerLayer.isEmpty()) return;
        int clamped = clampPrimitiveIndex(layerIndex, getSelectedPrimitiveIndex());
        selectedPrimitivePerLayer.set(layerIndex, clamped);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // JSON SERIALIZATION (via StateAccessor)
    // ═══════════════════════════════════════════════════════════════════════════

    public JsonObject toJson() {
        return StateAccessor.toJson(this);
    }
    
    public void fromJson(JsonObject json) {
        StateAccessor.fromJson(this, json);
        clearDirty();
    }
    
    public String toStateJson() {
        return toJson().toString();
    }
    
    public void fromStateJson(String jsonStr) {
        JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
        fromJson(json);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SNAPSHOT (for revert functionality)
    // ═══════════════════════════════════════════════════════════════════════════

    public void saveSnapshot() {
        snapshotJson = toStateJson();
        Logging.GUI.topic("state").debug("Snapshot saved");
    }
    
    public void restoreSnapshot() {
        if (snapshotJson != null) {
            fromStateJson(snapshotJson);
            Logging.GUI.topic("state").debug("Snapshot restored");
        }
    }
    
    public boolean hasSnapshot() { return snapshotJson != null; }

    // ═══════════════════════════════════════════════════════════════════════════
    // PROFILE
    // ═══════════════════════════════════════════════════════════════════════════

    private List<ProfileEntry> profiles = new ArrayList<>();
    {
        // Seed with some default profiles
        profiles.add(new ProfileEntry("default", false));
        profiles.add(new ProfileEntry("shield_default", true));
    }

    public String getCurrentProfileName() { return currentProfileName; }
    public void setCurrentProfileName(String name) { currentProfileName = name; }
    public boolean isCurrentProfileServer() { return currentProfileServer; }
    public boolean isCurrentProfileServerSourced() { return currentProfileServer; } // Alias
    public List<ProfileEntry> getProfiles() { return profiles; }
    public void setCurrentProfile(String name, boolean server) {
        currentProfileName = name;
        currentProfileServer = server;
    }
    
    /**
     * Updates the list of server profiles.
     * Called when ServerProfilesS2CPayload is received.
     * 
     * @param names List of server profile names
     */
    public void setServerProfiles(List<String> names) {
        // Remove existing server profiles
        profiles.removeIf(ProfileEntry::isServer);
        // Add new server profiles
        for (String name : names) {
            profiles.add(new ProfileEntry(name, true));
        }
        Logging.GUI.topic("state").debug("Updated server profiles: {} profiles", names.size());
    }
    
    // Aliases for snapshot methods (backward compatibility)
    public void saveProfileSnapshot() { saveSnapshot(); }
    public void restoreFromSnapshot() { restoreSnapshot(); }
    
    /** Snapshot wrapper for network sync with value accessors. */
    public static class ProfileSnapshot {
        private final JsonObject json;
        
        public ProfileSnapshot(String jsonStr) {
            this.json = (jsonStr != null && !jsonStr.isEmpty()) 
                ? JsonParser.parseString(jsonStr).getAsJsonObject() 
                : new JsonObject();
        }
        
        public float radius() { return json.has("radius") ? json.get("radius").getAsFloat() : 3.0f; }
        public int latSteps() { return json.has("sphere") && json.getAsJsonObject("sphere").has("latSteps") 
            ? json.getAsJsonObject("sphere").get("latSteps").getAsInt() : 16; }
        public int lonSteps() { return json.has("sphere") && json.getAsJsonObject("sphere").has("lonSteps") 
            ? json.getAsJsonObject("sphere").get("lonSteps").getAsInt() : 32; }
        public int maskCount() { return json.has("mask") && json.getAsJsonObject("mask").has("count") 
            ? json.getAsJsonObject("mask").get("count").getAsInt() : 1; }
    }
    public ProfileSnapshot getProfileSnapshot() { return new ProfileSnapshot(snapshotJson); }
    
    public void fromProfileJson(String jsonStr) {
        JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
        if (json.has("state")) {
            fromJson(json.getAsJsonObject("state"));
        }
    }
    
    public String toProfileJson(String profileName) {
        JsonObject json = new JsonObject();
        json.addProperty("name", profileName);
        json.addProperty("version", "1.0");
        json.add("state", toJson());
        return json.toString();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UNDO/REDO (simple stubs)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private java.util.Deque<String> undoStack = new java.util.ArrayDeque<>();
    private java.util.Deque<String> redoStack = new java.util.ArrayDeque<>();
    
    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
    public void undo() { if (canUndo()) { redoStack.push(undoStack.pop()); } }
    public void redo() { if (canRedo()) { undoStack.push(redoStack.pop()); } }
    public void pushUndo() { undoStack.push(toStateJson()); redoStack.clear(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYER ACCESSORS (for alpha, blendMode, visibility)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public float getLayerAlpha(int index) {
        if (index < 0 || index >= fieldLayers.size()) return 1.0f;
        return fieldLayers.get(index).alpha();
    }
    
    public void setLayerAlpha(int index, float alpha) {
        if (index >= 0 && index < fieldLayers.size()) {
            FieldLayer old = fieldLayers.get(index);
            FieldLayer updated = new FieldLayer(old.id(), old.primitives(), old.transform(), old.animation(),
                alpha, old.visible(), old.blendMode());
            fieldLayers.set(index, updated);
            markDirty();
        }
    }
    
    public String getLayerBlendMode(int index) {
        if (index < 0 || index >= fieldLayers.size()) return "NORMAL";
        return fieldLayers.get(index).blendMode().name();
    }
    
    public void setLayerBlendMode(int index, String mode) {
        if (index >= 0 && index < fieldLayers.size()) {
            FieldLayer old = fieldLayers.get(index);
            BlendMode blendMode;
            try {
                blendMode = BlendMode.valueOf(mode.toUpperCase());
            } catch (IllegalArgumentException e) {
                blendMode = BlendMode.NORMAL;
            }
            FieldLayer updated = new FieldLayer(old.id(), old.primitives(), old.transform(), old.animation(),
                old.alpha(), old.visible(), blendMode);
            fieldLayers.set(index, updated);
            markDirty();
        }
    }
    
    public boolean isLayerVisible(int index) {
        if (index < 0 || index >= fieldLayers.size()) return true;
        return fieldLayers.get(index).visible();
    }
    
    public boolean toggleLayerVisibility(int index) {
        if (index < 0 || index >= fieldLayers.size()) return true;
        FieldLayer old = fieldLayers.get(index);
        boolean newVisible = !old.visible();
        FieldLayer updated = new FieldLayer(old.id(), old.primitives(), old.transform(), old.animation(),
            old.alpha(), newVisible, old.blendMode());
        fieldLayers.set(index, updated);
        markDirty();
        return newVisible;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FRAGMENT TRACKING
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String currentShapeFragment = "";
    private String currentFillFragment = "";
    private String currentAnimationFragment = "";
    
    public String getCurrentShapeFragmentName() { return currentShapeFragment; }
    public String getCurrentFillFragmentName() { return currentFillFragment; }
    public String getCurrentAnimationFragmentName() { return currentAnimationFragment; }
    public void setCurrentShapeFragment(String name) { currentShapeFragment = name; }
    public void setCurrentFillFragment(String name) { currentFillFragment = name; }
    public void setCurrentAnimationFragment(String name) { currentAnimationFragment = name; }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BINDINGS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final List<BindingConfig> bindings = new ArrayList<>();
    
    public void addBinding(BindingConfig binding) {
        // Remove existing binding for same property if any
        bindings.removeIf(b -> b.property().equals(binding.property()));
        bindings.add(binding);
        Logging.GUI.topic("state").debug("Added binding: {} <- {}", binding.property(), binding.source());
        markDirty();
    }
    
    public boolean removeBinding(String property) {
        boolean removed = bindings.removeIf(b -> b.property().equals(property));
        if (removed) {
            Logging.GUI.topic("state").debug("Removed binding for: {}", property);
            markDirty();
        }
        return removed;
    }
    
    public List<BindingConfig> getBindings() {
        return java.util.Collections.unmodifiableList(bindings);
    }
    
    public void clearBindings() {
        int count = bindings.size();
        bindings.clear();
        Logging.GUI.topic("state").debug("Cleared {} bindings", count);
        markDirty();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODIFIERS ACCESSOR
    // ═══════════════════════════════════════════════════════════════════════════
    
    public Modifiers modifiers() { return modifiers; }
}
