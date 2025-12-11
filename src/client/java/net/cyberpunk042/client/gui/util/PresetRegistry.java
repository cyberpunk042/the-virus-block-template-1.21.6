package net.cyberpunk042.client.gui.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.field.category.PresetCategory;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import org.joml.Vector3f;
/**
 * Registry for multi-scope presets organized by category.
 * Loads from: config/the-virus-block/field_presets/{category}/
 * 
 * Presets can MERGE into current state (add layers, modify multiple categories).
 * This is different from Fragments which only affect a single scope.
 */
public class PresetRegistry {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<PresetCategory, List<PresetEntry>> PRESETS_BY_CATEGORY = new EnumMap<>(PresetCategory.class);
    private static final Map<String, PresetEntry> PRESETS_BY_ID = new HashMap<>();
    private static boolean loaded = false;
    
    /**
     * A preset entry with metadata.
     */
    public record PresetEntry(
        String id,
        String name,
        String description,
        String hint,
        PresetCategory category,
        JsonObject mergeData,
        List<String> affectedCategories
    ) {
        public String getDisplayName() {
            return name;
        }
    }
    
    /**
     * Load all presets from disk.
     * Call this when GUI opens.
     */
    public static void loadAll() {
        if (loaded) return;
        
        PRESETS_BY_CATEGORY.clear();
        PRESETS_BY_ID.clear();
        
        // Initialize empty lists for all categories
        for (PresetCategory cat : PresetCategory.values()) {
            PRESETS_BY_CATEGORY.put(cat, new ArrayList<>());
        }
        
        Path presetsRoot = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("the-virus-block")
            .resolve("field_presets");
        Path altPresetsRoot = Path.of("config", "the-virus-block", "field_presets"); // dev fallback
        
        boolean any = false;
        if (Files.exists(presetsRoot)) {
            any |= loadPresetTree(presetsRoot);
        }
        if (Files.exists(altPresetsRoot)) {
            any |= loadPresetTree(altPresetsRoot);
        }
        
        // 3) Built-in resources (fallback for JAR distribution)
        FabricLoader.getInstance().getModContainer(TheVirusBlock.MOD_ID).ifPresent(container -> {
            container.findPath("data/" + TheVirusBlock.MOD_ID + "/field_presets")
                .ifPresent(PresetRegistry::loadPresetTree);
        });
        any |= !PRESETS_BY_ID.isEmpty();
        
        if (!any) {
            TheVirusBlock.LOGGER.info("No presets folder found at {} or {}", presetsRoot, altPresetsRoot);
            loaded = true;
            return;
        }
        
        int total = PRESETS_BY_ID.size();
        TheVirusBlock.LOGGER.info("Loaded {} presets across {} categories", total, PresetCategory.values().length);
        loaded = true;
    }

    private static boolean loadPresetTree(Path root) {
        boolean any = false;
        // Scan each category folder (field_presets/<category>/)
        for (PresetCategory category : PresetCategory.values()) {
            Path categoryFolder = root.resolve(category.getFolderName());
            if (!Files.isDirectory(categoryFolder)) {
                continue;
            }
            
            try (Stream<Path> files = Files.list(categoryFolder)) {
                files.filter(p -> p.toString().endsWith(".json"))
                    .forEach(file -> loadPreset(file, category));
                any = true;
            } catch (IOException e) {
                TheVirusBlock.LOGGER.error("Failed to scan preset folder: {}", categoryFolder, e);
            }
        }

        // Also load any presets placed directly under root (no category subfolder)
        try (Stream<Path> files = Files.list(root)) {
            files.filter(p -> p.toString().endsWith(".json"))
                .forEach(file -> loadPreset(file, PresetCategory.STYLE)); // default bucket
            any = true;
        } catch (IOException e) {
            TheVirusBlock.LOGGER.error("Failed to scan root preset folder: {}", root, e);
        }
        return any;
    }

    /** Force reload on next access. */
    public static void reset() {
        loaded = false;
        PRESETS_BY_CATEGORY.clear();
        PRESETS_BY_ID.clear();
    }
    
    private static void loadPreset(Path file, PresetCategory category) {
        try {
            String content = Files.readString(file);
            JsonObject json = GSON.fromJson(content, JsonObject.class);
            
            String id = file.getFileName().toString().replace(".json", "");
            String name = json.has("name") ? json.get("name").getAsString() : id;
            String description = json.has("description") ? json.get("description").getAsString() : "";
            String hint = json.has("hint") ? json.get("hint").getAsString() : "";
            
            // Get merge data (the actual preset content)
            JsonObject mergeData = json.has("merge") ? json.getAsJsonObject("merge") : json;
            
            // Determine affected categories from merge data
            List<String> affected = determineAffectedCategories(mergeData);
            
            PresetEntry entry = new PresetEntry(id, name, description, hint, category, mergeData, affected);
            
            PRESETS_BY_CATEGORY.get(category).add(entry);
            PRESETS_BY_ID.put(id, entry);
            
        } catch (Exception e) {
            TheVirusBlock.LOGGER.error("Failed to load preset: {}", file, e);
        }
    }
    
    /**
     * Analyze merge data to determine what categories will be affected.
     */
    private static List<String> determineAffectedCategories(JsonObject mergeData) {
        List<String> affected = new ArrayList<>();
        
        if (mergeData.has("layers") || mergeData.has("primitives")) {
            affected.add("Layers/Primitives");
        }
        if (mergeData.has("shape") || hasNestedKey(mergeData, "shape")) {
            affected.add("Shape");
        }
        if (mergeData.has("fill") || hasNestedKey(mergeData, "fill")) {
            affected.add("Fill");
        }
        if (mergeData.has("visibility") || hasNestedKey(mergeData, "visibility")) {
            affected.add("Visibility");
        }
        if (mergeData.has("appearance") || hasNestedKey(mergeData, "appearance")) {
            affected.add("Appearance");
        }
        if (mergeData.has("animation") || hasNestedKey(mergeData, "animation")) {
            affected.add("Animation");
        }
        if (mergeData.has("transform") || hasNestedKey(mergeData, "transform")) {
            affected.add("Transform");
        }
        if (mergeData.has("beam")) {
            affected.add("Beam");
        }
        if (mergeData.has("prediction")) {
            affected.add("Prediction");
        }
        
        if (affected.isEmpty()) {
            affected.add("General settings");
        }
        
        return affected;
    }
    
    private static boolean hasNestedKey(JsonObject json, String key) {
        for (String k : json.keySet()) {
            if (k.contains(key) || k.contains("[")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get all categories that have at least one preset.
     */
    public static List<PresetCategory> getCategories() {
        ensureLoaded();
        return PRESETS_BY_CATEGORY.entrySet().stream()
            .filter(e -> !e.getValue().isEmpty())
            .map(Map.Entry::getKey)
            .toList();
    }
    
    /**
     * Get all presets for a specific category.
     */
    public static List<PresetEntry> getPresets(PresetCategory category) {
        ensureLoaded();
        return PRESETS_BY_CATEGORY.getOrDefault(category, List.of());
    }
    
    /**
     * Get a preset by ID.
     */
    public static Optional<PresetEntry> getPreset(String id) {
        ensureLoaded();
        return Optional.ofNullable(PRESETS_BY_ID.get(id));
    }
    
    /**
     * Apply a preset to the current state.
     * This MERGES the preset data into the state.
     */
    public static void applyPreset(FieldEditState state, String presetId) {
        PresetEntry preset = PRESETS_BY_ID.get(presetId);
        if (preset == null) {
            TheVirusBlock.LOGGER.warn("Preset not found: {}", presetId);
            return;
        }
        
        applyMergeData(state, preset.mergeData());
        state.markDirty();
        
        TheVirusBlock.LOGGER.info("Applied preset: {} ({})", preset.name(), preset.category().getDisplayName());
    }
    
    /**
     * Apply merge data to state.
     * Supports:
     * - Simple properties (glow, alpha, etc.)
     * - Nested objects (appearance, animation)
     * - Layer operations via $appendLayers / $mergeLayers
     * - Primitive operations via $appendPrimitives / $mergePrimitives
     */
    private static void applyMergeData(FieldEditState state, JsonObject mergeData) {
        // ═══════════════════════════════════════════════════════════════
        // SIMPLE PROPERTY MERGES
        // ═══════════════════════════════════════════════════════════════
        if (mergeData.has("fillMode")) {
            try {
                state.set("fill.mode", net.cyberpunk042.visual.fill.FillMode.valueOf(mergeData.get("fillMode").getAsString()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (mergeData.has("wireThickness")) {
            state.set("fill.wireThickness", mergeData.get("wireThickness").getAsFloat());
        }
        if (mergeData.has("glow")) {
            state.set("appearance.glow", mergeData.get("glow").getAsFloat());
        }
        if (mergeData.has("alpha")) {
            state.set("appearance.alpha", mergeData.get("alpha").getAsFloat());
        }
        // Animation via nested "spin" / "pulse" objects (handled in applyAnimation)
        if (mergeData.has("primaryColor")) {
            state.set("appearance.primaryColor", mergeData.get("primaryColor").getAsInt());
        }
        if (mergeData.has("radius")) {
            state.set("radius", mergeData.get("radius").getAsFloat());
        }
        if (mergeData.has("latSteps")) {
            state.set("sphere.latSteps", mergeData.get("latSteps").getAsInt());
        }
        if (mergeData.has("lonSteps")) {
            state.set("sphere.lonSteps", mergeData.get("lonSteps").getAsInt());
        }
        
        // ═══════════════════════════════════════════════════════════════
        // NESTED OBJECT MERGES
        // ═══════════════════════════════════════════════════════════════
        if (mergeData.has("appearance") && mergeData.get("appearance").isJsonObject()) {
            applyAppearance(state, mergeData.getAsJsonObject("appearance"));
        }
        
        if (mergeData.has("animation") && mergeData.get("animation").isJsonObject()) {
            applyAnimation(state, mergeData.getAsJsonObject("animation"));
        }
        
        if (mergeData.has("visibility") && mergeData.get("visibility").isJsonObject()) {
            applyVisibility(state, mergeData.getAsJsonObject("visibility"));
        }
        
        if (mergeData.has("transform") && mergeData.get("transform").isJsonObject()) {
            applyTransform(state, mergeData.getAsJsonObject("transform"));
        }
        
        // ═══════════════════════════════════════════════════════════════
        // LAYER OPERATIONS
        // $appendLayers: Always add new layers (rename if conflict)
        // $mergeLayers: Find by name and update, or add if not found
        // ═══════════════════════════════════════════════════════════════
        if (mergeData.has("$appendLayers") && mergeData.get("$appendLayers").isJsonArray()) {
            for (var element : mergeData.getAsJsonArray("$appendLayers")) {
                if (element.isJsonObject()) {
                    appendLayer(state, element.getAsJsonObject());
                }
            }
        }
        
        if (mergeData.has("$mergeLayers") && mergeData.get("$mergeLayers").isJsonArray()) {
            for (var element : mergeData.getAsJsonArray("$mergeLayers")) {
                if (element.isJsonObject()) {
                    mergeLayer(state, element.getAsJsonObject());
                }
            }
        }
        
        // ═══════════════════════════════════════════════════════════════
        // PRIMITIVE OPERATIONS (on current layer)
        // $appendPrimitives: Add primitives to current layer
        // $mergePrimitives: Find by id and update, or add if not found
        // ═══════════════════════════════════════════════════════════════
        if (mergeData.has("$appendPrimitives") && mergeData.get("$appendPrimitives").isJsonArray()) {
            int layerIndex = state.getSelectedLayerIndex();
            for (var element : mergeData.getAsJsonArray("$appendPrimitives")) {
                if (element.isJsonObject()) {
                    appendPrimitive(state, layerIndex, element.getAsJsonObject());
                }
            }
        }
        
        if (mergeData.has("$mergePrimitives") && mergeData.get("$mergePrimitives").isJsonArray()) {
            int layerIndex = state.getSelectedLayerIndex();
            for (var element : mergeData.getAsJsonArray("$mergePrimitives")) {
                if (element.isJsonObject()) {
                    mergePrimitive(state, layerIndex, element.getAsJsonObject());
                }
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // NESTED OBJECT APPLY HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    
    private static void applyAppearance(FieldEditState state, JsonObject json) {
        if (json.has("glow")) state.set("appearance.glow", json.get("glow").getAsFloat());
        if (json.has("emissive")) state.set("appearance.emissive", json.get("emissive").getAsFloat());
        if (json.has("saturation")) state.set("appearance.saturation", json.get("saturation").getAsFloat());
        if (json.has("primaryColor")) state.set("appearance.primaryColor", json.get("primaryColor").getAsInt());
        if (json.has("secondaryColor")) state.set("appearance.secondaryColor", json.get("secondaryColor").getAsInt());
    }
    
    private static void applyAnimation(FieldEditState state, JsonObject json) {
        // Spin
        if (json.has("spin")) {
            JsonObject spin = json.getAsJsonObject("spin");
            if (spin.has("speed")) state.set("spin.speed", spin.get("speed").getAsFloat());
            if (spin.has("axis")) state.set("spin.axis", spin.get("axis").getAsString());
        }
        // Pulse
        if (json.has("pulse")) {
            JsonObject pulse = json.getAsJsonObject("pulse");
            if (pulse.has("speed")) state.set("pulse.speed", pulse.get("speed").getAsFloat());
            if (pulse.has("amplitude")) state.set("pulse.amplitude", pulse.get("amplitude").getAsFloat());
        }
        // Alpha pulse
        if (json.has("alphaPulse")) {
            JsonObject alpha = json.getAsJsonObject("alphaPulse");
            if (alpha.has("min")) state.set("alphaPulse.min", alpha.get("min").getAsFloat());
            if (alpha.has("max")) state.set("alphaPulse.max", alpha.get("max").getAsFloat());
            if (alpha.has("speed")) state.set("alphaPulse.speed", alpha.get("speed").getAsFloat());
        }
        // Color cycle
        if (json.has("colorCycle")) {
            JsonObject cc = json.getAsJsonObject("colorCycle");
            if (cc.has("speed")) state.set("colorCycle.speed", cc.get("speed").getAsFloat());
            if (cc.has("blend")) state.set("colorCycle.blend", cc.get("blend").getAsBoolean());
        }
        // Wobble
        if (json.has("wobble")) {
            JsonObject wobble = json.getAsJsonObject("wobble");
            if (wobble.has("amplitude")) state.set("wobble.amplitude", wobble.get("amplitude").getAsFloat());
            if (wobble.has("speed")) state.set("wobble.speed", wobble.get("speed").getAsFloat());
        }
        // Wave
        if (json.has("wave")) {
            JsonObject wave = json.getAsJsonObject("wave");
            if (wave.has("amplitude")) state.set("wave.amplitude", wave.get("amplitude").getAsFloat());
            if (wave.has("frequency")) state.set("wave.frequency", wave.get("frequency").getAsFloat());
        }
    }
    
    private static void applyVisibility(FieldEditState state, JsonObject json) {
        if (json.has("maskType")) state.set("mask.type", json.get("maskType").getAsString());
        if (json.has("maskCount")) state.set("mask.count", json.get("maskCount").getAsInt());
        if (json.has("maskThickness")) state.set("mask.thickness", json.get("maskThickness").getAsFloat());
        if (json.has("maskOffset")) state.set("mask.offset", json.get("maskOffset").getAsFloat());
        if (json.has("maskInverted")) state.set("mask.invert", json.get("maskInverted").getAsBoolean());
        if (json.has("maskAnimated")) state.set("mask.animate", json.get("maskAnimated").getAsBoolean());
        if (json.has("maskAnimateSpeed")) state.set("mask.animSpeed", json.get("maskAnimateSpeed").getAsFloat());
    }
    
    private static void applyTransform(FieldEditState state, JsonObject json) {
        if (json.has("scale")) state.set("transform.scale", json.get("scale").getAsFloat());
        if (json.has("anchor")) state.set("transform.anchor", json.get("anchor").getAsString());
        if (json.has("offsetX") || json.has("offsetY") || json.has("offsetZ")) {
            // Handle nullable offset - default to 0
            Vector3f currentOffset = state.transform() != null ? state.transform().offset() : null;
            float x = json.has("offsetX") ? json.get("offsetX").getAsFloat() : (currentOffset != null ? currentOffset.x : 0);
            float y = json.has("offsetY") ? json.get("offsetY").getAsFloat() : (currentOffset != null ? currentOffset.y : 0);
            float z = json.has("offsetZ") ? json.get("offsetZ").getAsFloat() : (currentOffset != null ? currentOffset.z : 0);
            state.set("transform.offset", new Vector3f(x, y, z));
        }
        if (json.has("rotationX") || json.has("rotationY") || json.has("rotationZ")) {
            // Handle nullable rotation - default to 0
            Vector3f currentRot = state.transform() != null ? state.transform().rotation() : null;
            float x = json.has("rotationX") ? json.get("rotationX").getAsFloat() : (currentRot != null ? currentRot.x : 0);
            float y = json.has("rotationY") ? json.get("rotationY").getAsFloat() : (currentRot != null ? currentRot.y : 0);
            float z = json.has("rotationZ") ? json.get("rotationZ").getAsFloat() : (currentRot != null ? currentRot.z : 0);
            state.set("transform.rotation", new Vector3f(x, y, z));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // LAYER OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Append a new layer (always creates new, renames if conflict).
     */
    private static void appendLayer(FieldEditState state, JsonObject layerJson) {
        String name = layerJson.has("name") ? layerJson.get("name").getAsString() : "New Layer";
        int layerIndex = state.addLayerWithName(name);
        
        if (layerIndex < 0) {
            TheVirusBlock.LOGGER.warn("Could not append layer '{}' - max layers reached", name);
            return;
        }
        
        // Apply layer properties
        applyLayerProperties(state, layerIndex, layerJson);
        
        // Add primitives if specified
        if (layerJson.has("primitives") && layerJson.get("primitives").isJsonArray()) {
            for (var primElement : layerJson.getAsJsonArray("primitives")) {
                if (primElement.isJsonObject()) {
                    appendPrimitive(state, layerIndex, primElement.getAsJsonObject());
                }
            }
        }
        
        TheVirusBlock.LOGGER.debug("Appended layer: {} (index {})", state.getLayerName(layerIndex), layerIndex);
    }
    
    /**
     * Merge into existing layer by name, or create if not found.
     */
    private static void mergeLayer(FieldEditState state, JsonObject layerJson) {
        String name = layerJson.has("name") ? layerJson.get("name").getAsString() : "Layer";
        int layerIndex = state.findLayerByName(name);
        
        if (layerIndex < 0) {
            // Layer not found - create it
            layerIndex = state.addLayerWithName(name);
            if (layerIndex < 0) {
                TheVirusBlock.LOGGER.warn("Could not merge layer '{}' - max layers reached", name);
                return;
            }
        }
        
        // Apply layer properties
        applyLayerProperties(state, layerIndex, layerJson);
        
        // Merge primitives if specified
        if (layerJson.has("primitives") && layerJson.get("primitives").isJsonArray()) {
            for (var primElement : layerJson.getAsJsonArray("primitives")) {
                if (primElement.isJsonObject()) {
                    mergePrimitive(state, layerIndex, primElement.getAsJsonObject());
                }
            }
        }
        
        TheVirusBlock.LOGGER.debug("Merged layer: {} (index {})", name, layerIndex);
    }
    
    private static void applyLayerProperties(FieldEditState state, int layerIndex, JsonObject json) {
        var layers = state.getFieldLayers();
        if (layerIndex < 0 || layerIndex >= layers.size()) return;
        
        var layer = layers.get(layerIndex);
        
        // Parse new values or keep existing
        net.cyberpunk042.visual.layer.BlendMode blendMode = layer.blendMode();
        if (json.has("blendMode")) {
            try {
                blendMode = net.cyberpunk042.visual.layer.BlendMode.valueOf(json.get("blendMode").getAsString().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        
        float alpha = json.has("alpha") ? json.get("alpha").getAsFloat() : layer.alpha();
        boolean visible = json.has("visible") ? json.get("visible").getAsBoolean() : layer.visible();
        
        // Create new layer with updated properties (FieldLayer is immutable)
        net.cyberpunk042.field.FieldLayer updated = new net.cyberpunk042.field.FieldLayer(
            layer.id(), layer.primitives(), layer.transform(), layer.animation(),
            alpha, visible, blendMode
        );
        layers.set(layerIndex, updated);
        state.markDirty();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // PRIMITIVE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Append a new primitive to layer (always creates new, renames if conflict).
     */
    private static void appendPrimitive(FieldEditState state, int layerIndex, JsonObject primJson) {
        String id = primJson.has("id") ? primJson.get("id").getAsString() : "primitive";
        int primIndex = state.addPrimitiveWithId(layerIndex, id);
        
        if (primIndex < 0) {
            TheVirusBlock.LOGGER.warn("Could not append primitive '{}' to layer {}", id, layerIndex);
            return;
        }
        
        // Apply primitive properties (shape type, etc.)
        // Note: Currently primitives are simple id holders, properties are global
        // When per-primitive properties are added, apply them here
        
        TheVirusBlock.LOGGER.debug("Appended primitive: {} to layer {} (index {})", 
            state.getPrimitiveName(layerIndex, primIndex), layerIndex, primIndex);
    }
    
    /**
     * Merge into existing primitive by id, or create if not found.
     */
    private static void mergePrimitive(FieldEditState state, int layerIndex, JsonObject primJson) {
        String id = primJson.has("id") ? primJson.get("id").getAsString() : "primitive";
        int primIndex = state.findPrimitiveById(layerIndex, id);
        
        if (primIndex < 0) {
            // Primitive not found - create it
            primIndex = state.addPrimitiveWithId(layerIndex, id);
            if (primIndex < 0) {
                TheVirusBlock.LOGGER.warn("Could not merge primitive '{}' to layer {}", id, layerIndex);
                return;
            }
        }
        
        // Apply primitive properties
        // Note: Currently primitives are simple id holders
        // When per-primitive properties are added, merge them here
        
        TheVirusBlock.LOGGER.debug("Merged primitive: {} in layer {} (index {})", id, layerIndex, primIndex);
    }
    
    /**
     * Get affected categories for a preset (for confirmation dialog).
     */
    public static List<String> getAffectedCategories(String presetId) {
        return getPreset(presetId)
            .map(PresetEntry::affectedCategories)
            .orElse(List.of("Unknown"));
    }
    
    private static void ensureLoaded() {
        if (!loaded) {
            loadAll();
        }
    }
    
    /**
     * Force reload (for hot-reloading during development).
     */
    public static void reload() {
        loaded = false;
        loadAll();
    }

    /**
     * Get description for a preset.
     */
    public static String getDescription(String presetId) {
        return getPreset(presetId)
            .map(PresetEntry::description)
            .orElse("");
    }

    /**
     * List all preset names (for dropdown).
     */
    public static List<String> listPresets() {
        ensureLoaded();
        List<String> names = new ArrayList<>();
        PRESETS_BY_ID.keySet().stream().sorted().forEach(names::add);
        return names;
    }

    /**
     * Get display label for a preset id (falls back to id).
     */
    public static String getDisplayLabel(String id) {
        return getPreset(id).map(PresetEntry::name).orElse(id);
    }

}
