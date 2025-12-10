package net.cyberpunk042.client.gui.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.field.instance.FollowMode;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FragmentRegistry - loads GUI presets from JSON config files.
 *
 * <p>Scans the following config folders:
 * <ul>
 *   <li>{@code config/the-virus-block/field_shapes/} - Shape presets (grouped by type)</li>
 *   <li>{@code config/the-virus-block/field_fills/} - Fill mode presets</li>
 *   <li>{@code config/the-virus-block/field_masks/} - Visibility mask presets</li>
 *   <li>{@code config/the-virus-block/field_arrangements/} - Pattern arrangement presets</li>
 *   <li>{@code config/the-virus-block/field_animations/} - Animation presets</li>
 *   <li>{@code config/the-virus-block/field_beams/} - Debug beam presets</li>
 *   <li>{@code config/the-virus-block/field_follow/} - Follow mode presets</li>
 * </ul>
 *
 * <p>Each JSON file represents one preset. The "name" field in JSON becomes the dropdown label.
 * Files are loaded on first access and cached. Call {@link #reload()} to refresh after config changes.
 *
 * <p>This integrates with the existing {@code $ref} system - presets ARE the target fragments
 * that {@code $ref} points to. The GUI just provides a convenient dropdown interface.
 */
public final class FragmentRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("FragmentRegistry");
    private static final Path CONFIG_ROOT = FabricLoader.getInstance().getConfigDir().resolve("the-virus-block");

    // Folder names
    private static final String SHAPES_FOLDER = "field_shapes";
    private static final String FILLS_FOLDER = "field_fills";
    private static final String MASKS_FOLDER = "field_masks";
    private static final String ARRANGEMENTS_FOLDER = "field_arrangements";
    private static final String ANIMATIONS_FOLDER = "field_animations";
    private static final String BEAMS_FOLDER = "field_beams";
    private static final String FOLLOW_FOLDER = "field_follow";

    // Caches: presetName -> JsonObject
    private static final Map<String, Map<String, JsonObject>> shapePresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> fillPresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> maskPresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> arrangementPresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> animationPresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> beamPresets = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> followPresets = new ConcurrentHashMap<>();

    private static boolean loaded = false;

    private FragmentRegistry() {}

    // ═══════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Reload all presets from disk. Call this after config files change.
     */
    public static synchronized void reload() {
        LOGGER.info("Reloading presets from config...");
        shapePresets.clear();
        fillPresets.clear();
        maskPresets.clear();
        arrangementPresets.clear();
        animationPresets.clear();
        beamPresets.clear();
        followPresets.clear();
        loaded = false;
        ensureLoaded();
    }

    private static synchronized void ensureLoaded() {
        if (loaded) return;

        // Create folders if they don't exist
        ensureFolder(SHAPES_FOLDER);
        ensureFolder(FILLS_FOLDER);
        ensureFolder(MASKS_FOLDER);
        ensureFolder(ARRANGEMENTS_FOLDER);
        ensureFolder(ANIMATIONS_FOLDER);
        ensureFolder(BEAMS_FOLDER);
        ensureFolder(FOLLOW_FOLDER);

        // Load each category
        loadShapeFragments();
        loadSimplePresets(FILLS_FOLDER, fillPresets);
        loadSimplePresets(MASKS_FOLDER, maskPresets);
        loadSimplePresets(ARRANGEMENTS_FOLDER, arrangementPresets);
        loadSimplePresets(ANIMATIONS_FOLDER, animationPresets);
        loadSimplePresets(BEAMS_FOLDER, beamPresets);
        loadSimplePresets(FOLLOW_FOLDER, followPresets);

        LOGGER.info("Presets loaded: shapes={}, fills={}, masks={}, arrangements={}, animations={}, beams={}, follow={}",
            shapePresets.values().stream().mapToInt(Map::size).sum(),
            fillPresets.size(), maskPresets.size(), arrangementPresets.size(),
            animationPresets.size(), beamPresets.size(), followPresets.size());

        loaded = true;
    }

    private static void ensureFolder(String folder) {
        Path path = CONFIG_ROOT.resolve(folder);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                LOGGER.debug("Created config folder: {}", path);
            } catch (IOException e) {
                LOGGER.warn("Failed to create folder {}: {}", path, e.getMessage());
            }
        }
    }

    private static void loadShapeFragments() {
        Path folder = CONFIG_ROOT.resolve(SHAPES_FOLDER);
        if (!Files.exists(folder)) return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.json")) {
            for (Path file : stream) {
                try {
                    String content = Files.readString(file);
                    JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                    String name = json.has("name") ? json.get("name").getAsString() : file.getFileName().toString().replace(".json", "");
                    String type = json.has("type") ? json.get("type").getAsString().toLowerCase() : "sphere";

                    shapePresets.computeIfAbsent(type, k -> new LinkedHashMap<>()).put(name, json);
                    LOGGER.debug("Loaded shape preset: {} (type={})", name, type);
                } catch (Exception e) {
                    LOGGER.warn("Failed to load shape preset {}: {}", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to scan shapes folder: {}", e.getMessage());
        }
    }

    private static void loadSimplePresets(String folder, Map<String, JsonObject> target) {
        Path path = CONFIG_ROOT.resolve(folder);
        if (!Files.exists(path)) return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.json")) {
            for (Path file : stream) {
                try {
                    String content = Files.readString(file);
                    JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                    String name = json.has("name") ? json.get("name").getAsString() : file.getFileName().toString().replace(".json", "");
                    target.put(name, json);
                    LOGGER.debug("Loaded preset from {}: {}", folder, name);
                } catch (Exception e) {
                    LOGGER.warn("Failed to load preset {}: {}", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to scan {} folder: {}", folder, e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SHAPE PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listShapeFragments(String shapeType) {
        ensureLoaded();
        Map<String, JsonObject> map = shapePresets.get(shapeType.toLowerCase());
        List<String> list = new ArrayList<>();
        if (map != null) {
            list.addAll(map.keySet());
        }
        // Always include Default and Custom
        if (!list.contains("Default")) list.add(0, "Default");
        if (!list.contains("Custom")) list.add("Custom");
        return list;
    }

    public static void applyShapeFragment(FieldEditState state, String shapeType, String presetName) {
        ensureLoaded();
        if ("Custom".equals(presetName)) return; // Custom = user-defined, don't overwrite

        Map<String, JsonObject> map = shapePresets.get(shapeType.toLowerCase());
        if (map == null) return;
        JsonObject json = map.get(presetName);
        if (json == null) return;

        // Apply based on shape type
        switch (shapeType.toLowerCase()) {
            case "sphere" -> applySpherePreset(state, json);
            case "ring" -> applyRingPreset(state, json);
            case "disc" -> applyDiscPreset(state, json);
            case "prism" -> applyPrismPreset(state, json);
            case "cylinder", "beam" -> applyCylinderPreset(state, json);
            case "cube", "octahedron", "icosahedron" -> applyPolyPreset(state, json);
        }
    }

    private static void applySpherePreset(FieldEditState state, JsonObject json) {
        if (json.has("latSteps")) state.setSphereLatSteps(json.get("latSteps").getAsInt());
        if (json.has("lonSteps")) state.setSphereLonSteps(json.get("lonSteps").getAsInt());
        if (json.has("latStart")) state.setSphereLatStart(json.get("latStart").getAsFloat());
        if (json.has("latEnd")) state.setSphereLatEnd(json.get("latEnd").getAsFloat());
        if (json.has("algorithm")) state.setSphereAlgorithm(json.get("algorithm").getAsString());
    }

    private static void applyRingPreset(FieldEditState state, JsonObject json) {
        if (json.has("innerRadius")) state.setRingInnerRadius(json.get("innerRadius").getAsFloat());
        if (json.has("outerRadius")) state.setRingOuterRadius(json.get("outerRadius").getAsFloat());
        if (json.has("segments")) state.setRingSegments(json.get("segments").getAsInt());
        if (json.has("height")) state.setRingHeight(json.get("height").getAsFloat());
        if (json.has("y")) state.setRingY(json.get("y").getAsFloat());
    }

    private static void applyDiscPreset(FieldEditState state, JsonObject json) {
        if (json.has("radius")) state.setDiscRadius(json.get("radius").getAsFloat());
        if (json.has("segments")) state.setDiscSegments(json.get("segments").getAsInt());
        if (json.has("y")) state.setDiscY(json.get("y").getAsFloat());
        if (json.has("innerRadius")) state.setDiscInnerRadius(json.get("innerRadius").getAsFloat());
    }

    private static void applyPrismPreset(FieldEditState state, JsonObject json) {
        if (json.has("sides")) state.setPrismSides(json.get("sides").getAsInt());
        if (json.has("radius")) state.setPrismRadius(json.get("radius").getAsFloat());
        if (json.has("height")) state.setPrismHeight(json.get("height").getAsFloat());
        if (json.has("topRadius")) state.setPrismTopRadius(json.get("topRadius").getAsFloat());
    }

    private static void applyCylinderPreset(FieldEditState state, JsonObject json) {
        if (json.has("radius")) state.setCylinderRadius(json.get("radius").getAsFloat());
        if (json.has("height")) state.setCylinderHeight(json.get("height").getAsFloat());
        if (json.has("segments")) state.setCylinderSegments(json.get("segments").getAsInt());
        if (json.has("topRadius")) state.setCylinderTopRadius(json.get("topRadius").getAsFloat());
    }

    private static void applyPolyPreset(FieldEditState state, JsonObject json) {
        if (json.has("type")) state.setPolyType(json.get("type").getAsString());
        if (json.has("radius")) state.setPolyRadius(json.get("radius").getAsFloat());
        if (json.has("subdivisions")) state.setPolySubdivisions(json.get("subdivisions").getAsInt());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FILL PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listFillFragments() {
        ensureLoaded();
        return withDefaults(fillPresets.keySet());
    }

    public static void applyFillFragment(FieldEditState state, String presetName) {
        ensureLoaded();
        if ("Custom".equals(presetName)) return;

        JsonObject json = fillPresets.get(presetName);
        if (json == null) return;

        if (json.has("wireThickness")) state.setWireThickness(json.get("wireThickness").getAsFloat());
        if (json.has("doubleSided")) state.setDoubleSided(json.get("doubleSided").getAsBoolean());
        if (json.has("depthTest")) state.setDepthTest(json.get("depthTest").getAsBoolean());
        if (json.has("depthWrite")) state.setDepthWrite(json.get("depthWrite").getAsBoolean());
        if (json.has("pointSize")) state.setPointSize(json.get("pointSize").getAsFloat());

        // Cage settings
        if (json.has("cage")) {
            JsonObject cage = json.getAsJsonObject("cage");
            if (cage.has("latitudeCount")) state.setCageLatCount(cage.get("latitudeCount").getAsInt());
            if (cage.has("longitudeCount")) state.setCageLonCount(cage.get("longitudeCount").getAsInt());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VISIBILITY PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listVisibilityFragments() {
        ensureLoaded();
        return withDefaults(maskPresets.keySet());
    }

    public static void applyVisibilityFragment(FieldEditState state, String presetName) {
        ensureLoaded();
        if ("Custom".equals(presetName)) return;

        JsonObject json = maskPresets.get(presetName);
        if (json == null) return;

        if (json.has("mask")) state.setMaskType(json.get("mask").getAsString());
        if (json.has("count")) state.setMaskCount(json.get("count").getAsInt());
        if (json.has("thickness")) state.setMaskThickness(json.get("thickness").getAsFloat());
        if (json.has("offset")) state.setMaskOffset(json.get("offset").getAsFloat());
        if (json.has("invert")) state.setMaskInverted(json.get("invert").getAsBoolean());
        if (json.has("feather")) state.setMaskFeather(json.get("feather").getAsFloat());
        if (json.has("animate")) state.setMaskAnimated(json.get("animate").getAsBoolean());
        if (json.has("animateSpeed")) state.setMaskAnimateSpeed(json.get("animateSpeed").getAsFloat());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ARRANGEMENT PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listArrangementFragments() {
        ensureLoaded();
        return withDefaults(arrangementPresets.keySet());
    }

    public static void applyArrangementFragment(FieldEditState state, String presetName) {
        ensureLoaded();
        if ("Custom".equals(presetName)) return;

        JsonObject json = arrangementPresets.get(presetName);
        if (json == null) return;

        if (json.has("quad")) state.setQuadPattern(json.get("quad").getAsString());
        if (json.has("segment")) state.setSegmentPattern(json.get("segment").getAsString());
        if (json.has("sector")) state.setSectorPattern(json.get("sector").getAsString());
        if (json.has("multiPart")) state.setMultiPartArrangement(json.get("multiPart").getAsBoolean());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ANIMATION PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listAnimationFragments() {
        ensureLoaded();
        return withDefaults(animationPresets.keySet());
    }

    public static void applyAnimationFragment(FieldEditState state, String presetName) {
        ensureLoaded();
        if ("Custom".equals(presetName)) return;

        JsonObject json = animationPresets.get(presetName);
        if (json == null) return;

        // Spin
        if (json.has("spin")) {
            JsonObject spin = json.getAsJsonObject("spin");
            if (spin.has("enabled")) state.setSpinEnabled(spin.get("enabled").getAsBoolean());
            if (spin.has("axis")) state.setSpinAxis(spin.get("axis").getAsString());
            if (spin.has("speed")) state.setSpinSpeed(spin.get("speed").getAsFloat());
        }

        // Pulse
        if (json.has("pulse")) {
            JsonObject pulse = json.getAsJsonObject("pulse");
            if (pulse.has("enabled")) state.setPulseEnabled(pulse.get("enabled").getAsBoolean());
            if (pulse.has("mode")) state.setPulseMode(pulse.get("mode").getAsString());
            if (pulse.has("frequency")) state.setPulseFrequency(pulse.get("frequency").getAsFloat());
            if (pulse.has("amplitude")) state.setPulseAmplitude(pulse.get("amplitude").getAsFloat());
        }

        // Alpha pulse
        if (json.has("alphaPulse")) {
            JsonObject alpha = json.getAsJsonObject("alphaPulse");
            if (alpha.has("enabled")) state.setAlphaFadeEnabled(alpha.get("enabled").getAsBoolean());
            if (alpha.has("min")) state.setAlphaMin(alpha.get("min").getAsFloat());
            if (alpha.has("max")) state.setAlphaMax(alpha.get("max").getAsFloat());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BEAM PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listBeamFragments() {
        ensureLoaded();
        return withDefaults(beamPresets.keySet());
    }

    public static void applyBeamFragment(FieldEditState state, String presetName) {
        ensureLoaded();
        if ("Custom".equals(presetName)) return;

        JsonObject json = beamPresets.get(presetName);
        if (json == null) return;

        if (json.has("enabled")) state.setBeamEnabled(json.get("enabled").getAsBoolean());
        if (json.has("innerRadius")) state.setBeamInnerRadius(json.get("innerRadius").getAsFloat());
        if (json.has("outerRadius")) state.setBeamOuterRadius(json.get("outerRadius").getAsFloat());
        if (json.has("height")) state.setBeamHeight(json.get("height").getAsFloat());
        if (json.has("glow")) state.setBeamGlow(json.get("glow").getAsFloat());

        // Pulse settings
        if (json.has("pulse")) {
            JsonObject pulse = json.getAsJsonObject("pulse");
            if (pulse.has("enabled")) state.setBeamPulseEnabled(pulse.get("enabled").getAsBoolean());
            if (pulse.has("scale")) state.setBeamPulseScale(pulse.get("scale").getAsFloat());
            if (pulse.has("speed")) state.setBeamPulseSpeed(pulse.get("speed").getAsFloat());
            if (pulse.has("waveform")) state.setBeamPulseWaveform(pulse.get("waveform").getAsString());
            if (pulse.has("min")) state.setBeamPulseMin(pulse.get("min").getAsFloat());
            if (pulse.has("max")) state.setBeamPulseMax(pulse.get("max").getAsFloat());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FOLLOW PRESETS
    // ═══════════════════════════════════════════════════════════════════════

    public static List<String> listFollowFragments() {
        ensureLoaded();
        return withDefaults(followPresets.keySet());
    }

    public static void applyFollowFragment(FieldEditState state, String presetName) {
        ensureLoaded();
        if ("Custom".equals(presetName)) return;

        JsonObject json = followPresets.get(presetName);
        if (json == null) return;

        if (json.has("enabled")) state.setFollowEnabled(json.get("enabled").getAsBoolean());
        if (json.has("mode")) {
            try {
                state.setFollowMode(FollowMode.valueOf(json.get("mode").getAsString()));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Unknown follow mode: {}", json.get("mode").getAsString());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get the raw JSON for a preset (for advanced usage or $ref integration).
     */
    public static Optional<JsonObject> getPresetJson(String category, String presetName) {
        ensureLoaded();
        return Optional.ofNullable(switch (category.toLowerCase()) {
            case "fill", "fills" -> fillPresets.get(presetName);
            case "mask", "masks", "visibility" -> maskPresets.get(presetName);
            case "arrangement", "arrangements" -> arrangementPresets.get(presetName);
            case "animation", "animations" -> animationPresets.get(presetName);
            case "beam", "beams" -> beamPresets.get(presetName);
            case "follow" -> followPresets.get(presetName);
            default -> null;
        });
    }

    /**
     * Get shape preset JSON (separate because shapes are grouped by type).
     */
    public static Optional<JsonObject> getShapeFragmentJson(String shapeType, String presetName) {
        ensureLoaded();
        Map<String, JsonObject> map = shapePresets.get(shapeType.toLowerCase());
        if (map == null) return Optional.empty();
        return Optional.ofNullable(map.get(presetName));
    }

    private static List<String> withDefaults(Collection<String> names) {
        List<String> list = new ArrayList<>(names);
        // Ensure Default first, Custom last
        list.remove("Default");
        list.remove("Custom");
        list.add(0, "Default");
        list.add("Custom");
        return list;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // GENERIC $REF LOADING (for commands)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Apply a fragment from a $ref path to state.
     * Routes to the appropriate category-specific apply method.
     * 
     * @param state The state to modify
     * @param category Category name (shape, fill, visibility, appearance, animation, transform)
     * @param ref Reference path like "$field_shapes/simple_sphere" or "field_shapes/simple_sphere"
     */
    public static void applyFragment(FieldEditState state, String category, String ref) {
        // Parse the ref: "$field_shapes/preset_name" or "field_shapes/preset_name"
        String cleanRef = ref.startsWith("$") ? ref.substring(1) : ref;
        String[] parts = cleanRef.split("/", 2);
        if (parts.length < 2) {
            LOGGER.warn("Invalid fragment ref format: {}", ref);
            return;
        }
        
        String folder = parts[0];
        String presetName = parts[1];
        
        LOGGER.debug("Applying fragment: category={}, folder={}, preset={}", category, folder, presetName);
        
        switch (category.toLowerCase()) {
            case "shape" -> {
                // Shape presets might be in field_shapes/<shape_type>/<preset>.json
                // or just field_shapes/<preset>.json for generic shape settings
                String shapeType = state.getShapeType();
                applyShapeFragment(state, shapeType, presetName);
            }
            case "fill" -> applyFillFragment(state, presetName);
            case "visibility" -> applyVisibilityFragment(state, presetName);
            case "appearance" -> {
                // Appearance might be in various folders
                // For now, try animation as it contains appearance-like settings
                applyAnimationFragment(state, presetName);
            }
            case "animation" -> applyAnimationFragment(state, presetName);
            case "transform" -> {
                // Transform isn't a separate folder - apply from JSON directly
                getPresetJson("transform", presetName).ifPresent(json -> {
                    if (json.has("anchor")) state.setAnchor(json.get("anchor").getAsString());
                    if (json.has("scale")) state.setScale(json.get("scale").getAsFloat());
                    // Offset and rotation would need composite setters
                });
            }
            case "arrangement" -> applyArrangementFragment(state, presetName);
            case "beam" -> applyBeamFragment(state, presetName);
            case "follow" -> applyFollowFragment(state, presetName);
            default -> LOGGER.warn("Unknown fragment category: {}", category);
        }
    }
}
