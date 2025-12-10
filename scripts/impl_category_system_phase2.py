#!/usr/bin/env python3
"""
Phase 2: Update PresetRegistry and create ProfileManager.
Updates: PresetRegistry.java
Creates: ProfileManager.java
"""

from pathlib import Path

# Paths
MAIN_PACKAGE = Path("src/main/java/net/cyberpunk042")
FIELD_PACKAGE = MAIN_PACKAGE / "field"
GUI_PACKAGE = Path("src/client/java/net/cyberpunk042/client/gui")
UTIL_PACKAGE = GUI_PACKAGE / "util"
PROFILE_PACKAGE = FIELD_PACKAGE / "profile"

# =============================================================================
# PresetRegistry.java (REWRITE with category support)
# =============================================================================

PRESET_REGISTRY = '''package net.cyberpunk042.client.gui.util;

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
        
        if (!Files.exists(presetsRoot)) {
            TheVirusBlock.LOGGER.info("No presets folder found at {}", presetsRoot);
            loaded = true;
            return;
        }
        
        // Scan each category folder
        for (PresetCategory category : PresetCategory.values()) {
            Path categoryFolder = presetsRoot.resolve(category.getFolderName());
            if (!Files.isDirectory(categoryFolder)) {
                continue;
            }
            
            try (Stream<Path> files = Files.list(categoryFolder)) {
                files.filter(p -> p.toString().endsWith(".json"))
                    .forEach(file -> loadPreset(file, category));
            } catch (IOException e) {
                TheVirusBlock.LOGGER.error("Failed to scan preset folder: {}", categoryFolder, e);
            }
        }
        
        int total = PRESETS_BY_ID.size();
        TheVirusBlock.LOGGER.info("Loaded {} presets across {} categories", total, PresetCategory.values().length);
        loaded = true;
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
     * TODO: Implement full merge logic for nested paths like "layers[0].primitives"
     */
    private static void applyMergeData(FieldEditState state, JsonObject mergeData) {
        // Simple property merges
        if (mergeData.has("fillMode")) {
            state.setFillMode(mergeData.get("fillMode").getAsString());
        }
        if (mergeData.has("wireThickness")) {
            state.setWireThickness(mergeData.get("wireThickness").getAsFloat());
        }
        if (mergeData.has("glow")) {
            state.setGlow(mergeData.get("glow").getAsFloat());
        }
        if (mergeData.has("alpha")) {
            state.setAlpha(mergeData.get("alpha").getAsFloat());
        }
        if (mergeData.has("spinEnabled")) {
            state.setSpinEnabled(mergeData.get("spinEnabled").getAsBoolean());
        }
        if (mergeData.has("spinSpeed")) {
            state.setSpinSpeed(mergeData.get("spinSpeed").getAsFloat());
        }
        if (mergeData.has("pulseEnabled")) {
            state.setPulseEnabled(mergeData.get("pulseEnabled").getAsBoolean());
        }
        if (mergeData.has("primaryColor")) {
            state.setPrimaryColor(mergeData.get("primaryColor").getAsInt());
        }
        
        // Nested object merges
        if (mergeData.has("appearance") && mergeData.get("appearance").isJsonObject()) {
            JsonObject appearance = mergeData.getAsJsonObject("appearance");
            if (appearance.has("glow")) state.setGlow(appearance.get("glow").getAsFloat());
            if (appearance.has("emissive")) state.setEmissive(appearance.get("emissive").getAsFloat());
            if (appearance.has("saturation")) state.setSaturation(appearance.get("saturation").getAsFloat());
        }
        
        if (mergeData.has("animation") && mergeData.get("animation").isJsonObject()) {
            JsonObject animation = mergeData.getAsJsonObject("animation");
            if (animation.has("spinEnabled")) state.setSpinEnabled(animation.get("spinEnabled").getAsBoolean());
            if (animation.has("spinSpeed")) state.setSpinSpeed(animation.get("spinSpeed").getAsFloat());
            if (animation.has("pulseEnabled")) state.setPulseEnabled(animation.get("pulseEnabled").getAsBoolean());
        }
        
        // TODO: Handle complex merges like adding primitives to layers
        // This would require parsing paths like "layers[0].primitives" and
        // handling $append directives
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
}
'''

# =============================================================================
# ProfileManager.java (NEW)
# =============================================================================

PROFILE_MANAGER = '''package net.cyberpunk042.field.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.field.category.ProfileCategory;
import net.cyberpunk042.field.category.ProfileSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages loading, saving, and filtering profiles from multiple sources.
 * 
 * Sources:
 * - BUNDLED: assets/the-virus-block/shield_profiles/ (read-only)
 * - LOCAL: config/the-virus-block/field_profiles/local/ (editable)
 * - SERVER: received via network (read-only)
 */
public class ProfileManager {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final List<Profile> allProfiles = new ArrayList<>();
    private final List<Profile> serverProfiles = new ArrayList<>();
    private boolean loaded = false;
    
    // Singleton
    private static ProfileManager instance;
    public static ProfileManager getInstance() {
        if (instance == null) {
            instance = new ProfileManager();
        }
        return instance;
    }
    
    private ProfileManager() {}
    
    /**
     * Load all profiles from all sources.
     */
    public void loadAll() {
        allProfiles.clear();
        
        loadBundled();
        loadLocal();
        // Server profiles are added via addServerProfiles()
        allProfiles.addAll(serverProfiles);
        
        loaded = true;
        TheVirusBlock.LOGGER.info("Loaded {} total profiles", allProfiles.size());
    }
    
    /**
     * Load bundled profiles from mod assets.
     */
    private void loadBundled() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getResourceManager() == null) {
            return;
        }
        
        // Find all JSON files in assets/the-virus-block/shield_profiles/
        String prefix = "shield_profiles";
        Map<Identifier, Resource> resources = client.getResourceManager()
            .findResources(prefix, path -> path.getPath().endsWith(".json"));
        
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            if (!entry.getKey().getNamespace().equals(TheVirusBlock.MOD_ID)) {
                continue;
            }
            
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().getInputStream())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                Profile profile = Profile.fromJson(json, ProfileSource.BUNDLED);
                allProfiles.add(profile);
                TheVirusBlock.LOGGER.debug("Loaded bundled profile: {}", profile.name());
            } catch (Exception e) {
                TheVirusBlock.LOGGER.error("Failed to load bundled profile: {}", entry.getKey(), e);
            }
        }
    }
    
    /**
     * Load local profiles from config directory.
     */
    private void loadLocal() {
        Path localDir = getLocalProfileDir();
        if (!Files.isDirectory(localDir)) {
            try {
                Files.createDirectories(localDir);
            } catch (IOException e) {
                TheVirusBlock.LOGGER.error("Failed to create local profiles directory", e);
            }
            return;
        }
        
        try (Stream<Path> files = Files.list(localDir)) {
            files.filter(p -> p.toString().endsWith(".json"))
                .forEach(this::loadLocalProfile);
        } catch (IOException e) {
            TheVirusBlock.LOGGER.error("Failed to scan local profiles", e);
        }
    }
    
    private void loadLocalProfile(Path file) {
        try {
            String content = Files.readString(file);
            JsonObject json = GSON.fromJson(content, JsonObject.class);
            Profile profile = Profile.fromJson(json, ProfileSource.LOCAL);
            allProfiles.add(profile);
            TheVirusBlock.LOGGER.debug("Loaded local profile: {}", profile.name());
        } catch (Exception e) {
            TheVirusBlock.LOGGER.error("Failed to load local profile: {}", file, e);
        }
    }
    
    /**
     * Add server profiles (called when received from network).
     */
    public void addServerProfiles(List<Profile> profiles) {
        serverProfiles.clear();
        serverProfiles.addAll(profiles);
        
        // Refresh all profiles list
        if (loaded) {
            allProfiles.removeIf(p -> p.source() == ProfileSource.SERVER);
            allProfiles.addAll(serverProfiles);
        }
    }
    
    /**
     * Get all profiles (unfiltered).
     */
    public List<Profile> getAllProfiles() {
        ensureLoaded();
        return Collections.unmodifiableList(allProfiles);
    }
    
    /**
     * Filter profiles by source and category.
     * Pass null for any filter to skip that filter.
     */
    public List<Profile> filterProfiles(ProfileSource source, ProfileCategory category, String searchText) {
        ensureLoaded();
        
        return allProfiles.stream()
            .filter(p -> source == null || p.source() == source)
            .filter(p -> category == null || p.category() == category)
            .filter(p -> searchText == null || searchText.isEmpty() || matchesSearch(p, searchText))
            .sorted(Comparator
                .comparing((Profile p) -> p.source().ordinal())
                .thenComparing(Profile::name, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
    }
    
    private boolean matchesSearch(Profile profile, String searchText) {
        String lower = searchText.toLowerCase();
        return profile.name().toLowerCase().contains(lower) ||
               profile.description().toLowerCase().contains(lower) ||
               profile.tags().stream().anyMatch(t -> t.toLowerCase().contains(lower));
    }
    
    /**
     * Get profiles grouped by source.
     */
    public Map<ProfileSource, List<Profile>> getProfilesBySource() {
        ensureLoaded();
        return allProfiles.stream()
            .collect(Collectors.groupingBy(Profile::source));
    }
    
    /**
     * Get a profile by ID.
     */
    public Optional<Profile> getProfile(String id) {
        ensureLoaded();
        return allProfiles.stream()
            .filter(p -> p.id().equals(id))
            .findFirst();
    }
    
    /**
     * Save a local profile.
     */
    public void saveProfile(Profile profile) {
        if (profile.source() != ProfileSource.LOCAL) {
            TheVirusBlock.LOGGER.warn("Cannot save non-local profile: {}", profile.name());
            return;
        }
        
        Path file = getLocalProfileDir().resolve(profile.id() + ".json");
        try {
            Files.writeString(file, GSON.toJson(profile.toJson()));
            TheVirusBlock.LOGGER.info("Saved profile: {}", profile.name());
            
            // Update in-memory list
            allProfiles.removeIf(p -> p.id().equals(profile.id()));
            allProfiles.add(profile);
        } catch (IOException e) {
            TheVirusBlock.LOGGER.error("Failed to save profile: {}", profile.name(), e);
        }
    }
    
    /**
     * Delete a local profile.
     */
    public boolean deleteProfile(String id) {
        Profile profile = getProfile(id).orElse(null);
        if (profile == null || profile.source() != ProfileSource.LOCAL) {
            TheVirusBlock.LOGGER.warn("Cannot delete non-local or missing profile: {}", id);
            return false;
        }
        
        Path file = getLocalProfileDir().resolve(id + ".json");
        try {
            Files.deleteIfExists(file);
            allProfiles.removeIf(p -> p.id().equals(id));
            TheVirusBlock.LOGGER.info("Deleted profile: {}", id);
            return true;
        } catch (IOException e) {
            TheVirusBlock.LOGGER.error("Failed to delete profile: {}", id, e);
            return false;
        }
    }
    
    /**
     * Rename a local profile.
     */
    public boolean renameProfile(String oldId, String newId, String newName) {
        Profile profile = getProfile(oldId).orElse(null);
        if (profile == null || profile.source() != ProfileSource.LOCAL) {
            return false;
        }
        
        // Delete old file
        deleteProfile(oldId);
        
        // Save with new name
        Profile renamed = profile.withName(newName, newId);
        saveProfile(renamed);
        
        return true;
    }
    
    /**
     * Duplicate a profile (always creates local copy).
     */
    public Profile duplicateProfile(String id, String newName) {
        Profile original = getProfile(id).orElse(null);
        if (original == null) {
            return null;
        }
        
        String newId = newName.toLowerCase().replace(" ", "_") + "_" + System.currentTimeMillis();
        Profile copy = original.withName(newName, newId);
        saveProfile(copy);
        
        return copy;
    }
    
    /**
     * Export a profile to a file.
     */
    public void exportProfile(String id, Path destination) throws IOException {
        Profile profile = getProfile(id).orElseThrow(() -> 
            new IOException("Profile not found: " + id));
        Files.writeString(destination, GSON.toJson(profile.toJson()));
    }
    
    /**
     * Import a profile from a file.
     */
    public Profile importProfile(Path source) throws IOException {
        String content = Files.readString(source);
        JsonObject json = GSON.fromJson(content, JsonObject.class);
        
        // Generate unique ID
        String baseName = source.getFileName().toString().replace(".json", "");
        String id = baseName + "_" + System.currentTimeMillis();
        json.addProperty("id", id);
        
        Profile profile = Profile.fromJson(json, ProfileSource.LOCAL);
        saveProfile(profile);
        
        return profile;
    }
    
    private Path getLocalProfileDir() {
        return FabricLoader.getInstance()
            .getConfigDir()
            .resolve("the-virus-block")
            .resolve("field_profiles")
            .resolve("local");
    }
    
    private void ensureLoaded() {
        if (!loaded) {
            loadAll();
        }
    }
    
    /**
     * Force reload all profiles.
     */
    public void reload() {
        loaded = false;
        loadAll();
    }
}
'''

# =============================================================================
# Main
# =============================================================================

def main():
    print("=" * 60)
    print("PHASE 2: Category System - Registries")
    print("=" * 60)
    
    # Ensure directories exist
    UTIL_PACKAGE.mkdir(parents=True, exist_ok=True)
    PROFILE_PACKAGE.mkdir(parents=True, exist_ok=True)
    
    # Write PresetRegistry.java
    preset_registry_file = UTIL_PACKAGE / "PresetRegistry.java"
    preset_registry_file.write_text(PRESET_REGISTRY, encoding='utf-8')
    print(f"✅ Updated: {preset_registry_file}")
    
    # Write ProfileManager.java
    profile_manager_file = PROFILE_PACKAGE / "ProfileManager.java"
    profile_manager_file.write_text(PROFILE_MANAGER, encoding='utf-8')
    print(f"✅ Created: {profile_manager_file}")
    
    print()
    print("=" * 60)
    print("Phase 2 Complete!")
    print("=" * 60)
    print()
    print("Updated/Created files:")
    print(f"  - {UTIL_PACKAGE}/PresetRegistry.java (rewritten)")
    print(f"  - {PROFILE_PACKAGE}/ProfileManager.java (new)")
    print()
    print("Next: Run Phase 3 script for GUI updates")


if __name__ == "__main__":
    main()

