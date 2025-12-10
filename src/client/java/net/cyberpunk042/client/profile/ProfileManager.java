package net.cyberpunk042.client.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.field.category.ProfileCategory;
import net.cyberpunk042.field.category.ProfileSource;
import net.cyberpunk042.field.profile.Profile;
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
