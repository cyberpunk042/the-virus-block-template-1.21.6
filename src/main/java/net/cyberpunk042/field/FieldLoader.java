package net.cyberpunk042.field;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.cyberpunk042.config.InfectionConfigRegistry;
import net.cyberpunk042.config.InfectionConfigRegistry.ConfigHandle;
import net.cyberpunk042.log.Logging;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Loads field definitions from JSON resource files.
 * 
 * <p>Integrates with {@link InfectionConfigRegistry} for hot-reload support.
 * Call {@link #initialize()} during mod initialization to register.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // During mod init
 * FieldLoader.initialize();
 * 
 * // When resource manager is available
 * FieldLoader.load(resourceManager);
 * 
 * // Hot reload (uses cached resource manager)
 * InfectionConfigRegistry.loadCommon(); // Will trigger FieldLoader.reloadFromCache()
 * </pre>
 */
public final class FieldLoader {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "field_definitions";
    
    /** Cached resource manager for hot-reload support */
    private static ResourceManager cachedResourceManager;
    
    /** Whether initialize() has been called */
    private static boolean initialized = false;
    
    private FieldLoader() {}
    
    /**
     * Initializes the loader and registers with InfectionConfigRegistry.
     * Should be called once during mod initialization.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }
        
        InfectionConfigRegistry.register(ConfigHandle.common(
            "field_definitions",
            FieldLoader::reloadFromCache,
            () -> {} // No save needed - definitions are read-only
        ));
        
        initialized = true;
        Logging.REGISTRY.topic("field").info("FieldLoader registered with config registry");
    }
    
    /**
     * Reloads using the cached resource manager.
     * Called by InfectionConfigRegistry during hot-reload.
     */
    private static void reloadFromCache() {
        if (cachedResourceManager == null) {
            Logging.REGISTRY.topic("field").warn(
                "Cannot reload field definitions: no resource manager cached");
            return;
        }
        reload(cachedResourceManager);
    }
    
    public static int load(ResourceManager resourceManager) {
        // Cache for hot-reload support
        cachedResourceManager = resourceManager;
        
        Logging.REGISTRY.topic("field").info("Loading field definitions...");
        
        int loaded = 0;
        Map<Identifier, Resource> resources = resourceManager.findResources(
            DIRECTORY,
            id -> id.getPath().endsWith(".json")
        );
        
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            try {
                FieldDefinition def = loadResource(entry.getKey(), entry.getValue());
                if (def != null) {
                    FieldRegistry.register(def);
                    loaded++;
                }
            } catch (Exception e) {
                Logging.REGISTRY.topic("field").error(
                    "Failed to load {}: {}", entry.getKey(), e.getMessage());
            }
        }
        
        Logging.REGISTRY.topic("field").info("Loaded {} field definitions", loaded);
        return loaded;
    }
    
    private static FieldDefinition loadResource(Identifier resourceId, Resource resource) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) return null;
            
            String path = resourceId.getPath();
            path = path.substring(DIRECTORY.length() + 1, path.length() - 5);
            Identifier defId = Identifier.of(resourceId.getNamespace(), path);
            
            return FieldDefinition.fromJson(json, defId);
        }
    }
    
    public static int reload(ResourceManager resourceManager) {
        FieldRegistry.clear();
        FieldRegistry.registerDefaults();
        return load(resourceManager);
    }
}
