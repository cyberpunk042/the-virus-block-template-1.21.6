package net.cyberpunk042.client.gui.state;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.cyberpunk042.field.BeamConfig;
import net.cyberpunk042.field.instance.FollowMode;
import net.cyberpunk042.field.instance.FollowModeConfig;
import net.cyberpunk042.field.instance.PredictionConfig;
import net.cyberpunk042.field.primitive.PrimitiveLink;
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
    
    @StateField private SphereShape sphere = SphereShape.DEFAULT;
    @StateField private RingShape ring = RingShape.DEFAULT;
    @StateField private DiscShape disc = DiscShape.DEFAULT;
    @StateField private PrismShape prism = PrismShape.builder().build();
    @StateField private CylinderShape cylinder = CylinderShape.builder().build();
    @StateField private PolyhedronShape polyhedron = PolyhedronShape.DEFAULT;
    
    @StateField private String shapeType = "sphere";
    @StateField private float radius = 3.0f;
    
    public Shape currentShape() {
        return switch (shapeType.toLowerCase()) {
            case "sphere" -> sphere;
            case "ring" -> ring;
            case "disc" -> disc;
            case "prism" -> prism;
            case "cylinder" -> cylinder;
            case "polyhedron" -> polyhedron;
            default -> sphere;
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TRANSFORM & ORBIT
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private Transform transform = Transform.IDENTITY;
    @StateField private OrbitConfig orbit = OrbitConfig.NONE;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // APPEARANCE & FILL
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private FillConfig fill = FillConfig.SOLID;
    @StateField private VisibilityMask mask = VisibilityMask.FULL;
    @StateField private ArrangementConfig arrangement = ArrangementConfig.DEFAULT;
    
    @StateField private int color = 0xFF00FFFF;
    @StateField private float alpha = 0.8f;
    @StateField private float glow = 0.5f;
    @StateField private float emissive = 0f;
    @StateField private float saturation = 0f;
    @StateField private int primaryColor = 0xFF00FFFF;
    @StateField private int secondaryColor = 0xFFFF00FF;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private SpinConfig spin = SpinConfig.NONE;
    @StateField private PulseConfig pulse = PulseConfig.NONE;
    @StateField private AlphaPulseConfig alphaPulse = AlphaPulseConfig.NONE;
    @StateField private WobbleConfig wobble = WobbleConfig.builder().build();
    @StateField private WaveConfig wave = WaveConfig.builder().build();
    @StateField private ColorCycleConfig colorCycle = ColorCycleConfig.builder().build();
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FOLLOW & PREDICTION
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private FollowModeConfig followConfig = FollowModeConfig.DEFAULT;
    @StateField private PredictionConfig prediction = PredictionConfig.DEFAULT;
    @StateField private FollowMode followMode = FollowMode.SMOOTH;
    @StateField private boolean predictionEnabled = true;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LINKING & BEAM
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private PrimitiveLink link = null;
    @StateField private BeamConfig beam = BeamConfig.NONE;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FILL EXTRAS (cage, point size)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private int cageLatCount = 8;
    @StateField private int cageLonCount = 16;
    @StateField private float pointSize = 2f;
    
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
    
    @StateField private boolean followEnabled = true;
    
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
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYER MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    private int selectedLayerIndex = 0;
    private int selectedPrimitiveIndex = 0;
    private List<LayerState> layers = new ArrayList<>();
    
    /** Simple layer state holder. */
    public static class LayerState {
        public boolean visible = true;
        public String name = "Layer";
        public String blendMode = "NORMAL";
        public int order = 0;
        public float alpha = 1.0f;
        public LayerState(String name) { this.name = name; }
    }
    
    private List<List<String>> primitivesPerLayer = new ArrayList<>();
    private List<Integer> selectedPrimitivePerLayer = new ArrayList<>();
    
    {
        // Initialize with one default layer
        layers.add(new LayerState("Layer 1"));
        List<String> primitives = new ArrayList<>();
        primitives.add("primitive_1");
        primitivesPerLayer.add(primitives);
        selectedPrimitivePerLayer.add(0);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GUI SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private boolean livePreviewEnabled = true;
    @StateField private boolean autoSaveEnabled = false;
    
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
    // DIRTY TRACKING
    // ═══════════════════════════════════════════════════════════════════════════
    
    public boolean isDirty() { return isDirty; }
    public void markDirty() { isDirty = true; }
    public void clearDirty() { isDirty = false; }
    
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
        markDirty(); 
    }
    
    /** Update a record using a modifier function (for immutable records with toBuilder). */
    public <T> void update(String name, java.util.function.Function<T, T> modifier) {
        StateAccessor.update(this, name, modifier);
        markDirty();
    }
    // ═══════════════════════════════════════════════════════════════════════════
    // TYPED RECORD ACCESSORS (for fluent API: state.sphere().latSteps())
    // ═══════════════════════════════════════════════════════════════════════════

    public SphereShape sphere() { return sphere; }
    public RingShape ring() { return ring; }
    public DiscShape disc() { return disc; }
    public PrismShape prism() { return prism; }
    public CylinderShape cylinder() { return cylinder; }
    public PolyhedronShape polyhedron() { return polyhedron; }

    public Transform transform() { return transform; }
    public OrbitConfig orbit() { return orbit; }

    public FillConfig fill() { return fill; }
    public VisibilityMask mask() { return mask; }
    public ArrangementConfig arrangement() { return arrangement; }

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
        this.selectedLayerIndex = Math.max(0, Math.min(index, layers.size() - 1));
        clampPrimitiveSelection(selectedLayerIndex);
    }
    
    public int getLayerCount() { return layers.size(); }
    public List<LayerState> getLayers() { return layers; }
    
    public int addLayer() {
        if (layers.size() >= 10) return -1;
        layers.add(new LayerState("Layer " + (layers.size() + 1)));
        List<String> primitives = new ArrayList<>();
        primitives.add("primitive_1");
        primitivesPerLayer.add(primitives);
        selectedPrimitivePerLayer.add(0);
        markDirty();
        return layers.size() - 1;
    }
    
    public boolean removeLayer(int index) {
        if (layers.size() <= 1 || index < 0 || index >= layers.size()) return false;
        layers.remove(index);
        primitivesPerLayer.remove(index);
        selectedPrimitivePerLayer.remove(index);
        markDirty();
        return true;
    }
    
    public boolean swapLayers(int a, int b) {
        if (a < 0 || a >= layers.size() || b < 0 || b >= layers.size()) return false;
        java.util.Collections.swap(layers, a, b);
        java.util.Collections.swap(primitivesPerLayer, a, b);
        java.util.Collections.swap(selectedPrimitivePerLayer, a, b);
        markDirty();
        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMITIVE MANAGEMENT (per layer)
    // ═══════════════════════════════════════════════════════════════════════════

    public int getPrimitiveCount(int layerIndex) {
        if (layerIndex < 0 || layerIndex >= primitivesPerLayer.size()) return 0;
        return primitivesPerLayer.get(layerIndex).size();
    }
    public List<String> getPrimitivesForLayer(int layerIndex) { return layerIndex >= 0 && layerIndex < primitivesPerLayer.size() ? primitivesPerLayer.get(layerIndex) : java.util.Collections.emptyList(); }

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
        if (layerIndex < 0 || layerIndex >= primitivesPerLayer.size()) return -1;
        var list = primitivesPerLayer.get(layerIndex);
        String id = "primitive_" + (list.size() + 1);
        list.add(id);
        selectedPrimitivePerLayer.set(layerIndex, list.size() - 1);
        markDirty();
        return list.size() - 1;
    }

    public boolean removePrimitive(int layerIndex, int primitiveIndex) {
        if (!isValidPrimitiveIndex(layerIndex, primitiveIndex)) return false;
        var list = primitivesPerLayer.get(layerIndex);
        if (list.size() <= 1) return false;
        list.remove(primitiveIndex);
        clampPrimitiveSelection(layerIndex);
        markDirty();
        return true;
    }

    public boolean swapPrimitives(int layerIndex, int a, int b) {
        if (!isValidPrimitiveIndex(layerIndex, a) || !isValidPrimitiveIndex(layerIndex, b)) return false;
        java.util.Collections.swap(primitivesPerLayer.get(layerIndex), a, b);
        int selected = getSelectedPrimitiveIndex();
        if (selected == a) selectedPrimitivePerLayer.set(layerIndex, b);
        else if (selected == b) selectedPrimitivePerLayer.set(layerIndex, a);
        markDirty();
        return true;
    }

    private boolean isValidPrimitiveIndex(int layerIndex, int primitiveIndex) {
        if (layerIndex < 0 || layerIndex >= primitivesPerLayer.size()) return false;
        var list = primitivesPerLayer.get(layerIndex);
        return primitiveIndex >= 0 && primitiveIndex < list.size();
    }

    private int clampPrimitiveIndex(int layerIndex, int index) {
        if (layerIndex < 0 || layerIndex >= primitivesPerLayer.size()) return 0;
        var list = primitivesPerLayer.get(layerIndex);
        return Math.max(0, Math.min(index, Math.max(0, list.size() - 1)));
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
    
    public String getCurrentProfileName() { return currentProfileName; }
    public void setCurrentProfileName(String name) { currentProfileName = name; }
    public boolean isCurrentProfileServer() { return currentProfileServer; }
    public void setCurrentProfile(String name, boolean server) {
        currentProfileName = name;
        currentProfileServer = server;
    }
    
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
}
