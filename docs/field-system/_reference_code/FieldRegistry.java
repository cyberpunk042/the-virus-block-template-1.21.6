package net.cyberpunk042.field;

import net.cyberpunk042.field.instance.PredictionConfig;

import net.cyberpunk042.log.Logging;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Central registry for field definitions.
 * 
 * <p>Field definitions are loaded from JSON assets and can also be
 * registered programmatically at runtime.
 * 
 * <p>The registry is thread-safe and supports hot-reloading.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Get a definition
 * FieldDefinition shield = FieldRegistry.get(
 *     Identifier.of("the-virus-block", "anti_virus_shield"));
 * 
 * // Get all shields
 * List&lt;FieldDefinition&gt; shields = FieldRegistry.byType(FieldType.SHIELD);
 * 
 * // Register a custom definition
 * FieldRegistry.register(myDefinition);
 * </pre>
 * 
 * @see FieldDefinition
 * @see FieldType
 */
public final class FieldRegistry {
    
    private static final Map<Identifier, FieldDefinition> DEFINITIONS = new ConcurrentHashMap<>();
    private static final Map<FieldType, List<Identifier>> BY_TYPE = new ConcurrentHashMap<>();
    
    private FieldRegistry() {}
    
    // =========================================================================
    // Registration
    // =========================================================================
    
    /**
     * Registers a field definition.
     * Overwrites if ID already exists.
     * 
     * @param definition The definition to register
     */
    public static void register(FieldDefinition definition) {
        if (definition == null || definition.id() == null) {
            Logging.REGISTRY.topic("field").warn("Attempted to register null definition");
            return;
        }
        
        Identifier id = definition.id();
        FieldDefinition old = DEFINITIONS.put(id, definition);
        
        // Update type index
        if (old != null && old.type() != definition.type()) {
            // Type changed - remove from old type list
            List<Identifier> oldTypeList = BY_TYPE.get(old.type());
            if (oldTypeList != null) {
                oldTypeList.remove(id);
            }
        }
        
        BY_TYPE.computeIfAbsent(definition.type(), t -> new ArrayList<>())
               .add(id);
        
        Logging.REGISTRY.topic("field").info(
            "Registered field: {} (type={})", id, definition.type().id());
    }
    
    /**
     * Registers multiple definitions.
     */
    public static void registerAll(Collection<FieldDefinition> definitions) {
        for (FieldDefinition def : definitions) {
            register(def);
        }
    }
    
    /**
     * Unregisters a field definition.
     * 
     * @param id The definition ID to remove
     * @return true if removed, false if not found
     */
    public static boolean unregister(Identifier id) {
        if (id == null) {
            return false;
        }
        
        FieldDefinition removed = DEFINITIONS.remove(id);
        if (removed != null) {
            List<Identifier> typeList = BY_TYPE.get(removed.type());
            if (typeList != null) {
                typeList.remove(id);
            }
            Logging.REGISTRY.topic("field").info("Unregistered field: {}", id);
            return true;
        }
        return false;
    }
    
    // =========================================================================
    // Lookup
    // =========================================================================
    
    /**
     * Gets a field definition by ID.
     * 
     * @param id The definition ID
     * @return Definition or null if not found
     */
    public static FieldDefinition get(Identifier id) {
        return id != null ? DEFINITIONS.get(id) : null;
    }
    
    /**
     * Gets a field definition by ID string.
     * 
     * @param id The definition ID as "namespace:path"
     * @return Definition or null if not found
     */
    public static FieldDefinition get(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return get(Identifier.tryParse(id));
    }
    
    /**
     * Gets a field definition with fallback.
     */
    public static FieldDefinition getOrDefault(Identifier id, FieldDefinition fallback) {
        FieldDefinition def = get(id);
        return def != null ? def : fallback;
    }
    
    /**
     * Checks if a definition exists.
     */
    public static boolean exists(Identifier id) {
        return id != null && DEFINITIONS.containsKey(id);
    }
    
    // =========================================================================
    // Type queries
    // =========================================================================
    
    /**
     * Gets all definitions of a specific type.
     */
    public static List<FieldDefinition> byType(FieldType type) {
        if (type == null) {
            return Collections.emptyList();
        }
        List<Identifier> ids = BY_TYPE.get(type);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
            .map(DEFINITIONS::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all definition IDs of a specific type.
     */
    public static List<Identifier> idsByType(FieldType type) {
        if (type == null) {
            return Collections.emptyList();
        }
        List<Identifier> ids = BY_TYPE.get(type);
        return ids != null ? List.copyOf(ids) : Collections.emptyList();
    }
    
    /**
     * Gets definitions matching a predicate.
     */
    public static List<FieldDefinition> filter(Predicate<FieldDefinition> predicate) {
        return DEFINITIONS.values().stream()
            .filter(predicate)
            .collect(Collectors.toList());
    }
    
    // =========================================================================
    // All definitions
    // =========================================================================
    
    /**
     * Returns all registered definition IDs.
     */
    public static Set<Identifier> ids() {
        return Collections.unmodifiableSet(DEFINITIONS.keySet());
    }
    
    /**
     * Returns all registered definitions.
     */
    public static Collection<FieldDefinition> all() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }
    
    /**
     * Returns the total count of registered definitions.
     */
    public static int count() {
        return DEFINITIONS.size();
    }
    
    /**
     * Returns count by type.
     */
    public static int count(FieldType type) {
        List<Identifier> ids = BY_TYPE.get(type);
        return ids != null ? ids.size() : 0;
    }
    
    // =========================================================================
    // Lifecycle
    // =========================================================================
    
    /**
     * Clears all registered definitions.
     * Call before reloading from resources.
     */
    public static void clear() {
        DEFINITIONS.clear();
        BY_TYPE.clear();
        Logging.REGISTRY.topic("field").info("Cleared all field definitions");
    }
    
    /**
     * Called after resource reload to log status.
     */
    public static void logStatus() {
        Logging.REGISTRY.topic("field").info(
            "Field registry: {} definitions ({} shield, {} personal, {} growth, {} force, {} singularity, {} aura)",
            count(),
            count(FieldType.SHIELD),
            count(FieldType.PERSONAL),
            count(FieldType.AURA),
            count(FieldType.FORCE),
            count(FieldType.SHIELD),
            count(FieldType.AURA)
        );
    }
    
    // =========================================================================
    // Built-in defaults
    // =========================================================================
    
    /**
     * Registers built-in default definitions.
     * Call during mod initialization.
     */
    public static void registerDefaults() {
        // Default shield
        register(FieldDefinition.builder(
                Identifier.of("the-virus-block", "default_shield"),
                FieldType.SHIELD)
            .theme("cyber_green")
            .layer(FieldLayer.sphere("shell", 1.0f, 32)
                .withColor("@primary")
                .withAlpha(0.8f)
                .withSpin(0.02f))
            .build());
        
        // Default personal field
        register(FieldDefinition.builder(
                Identifier.of("the-virus-block", "default_personal"),
                FieldType.PERSONAL)
            .theme("cyber_blue")
            .layer(FieldLayer.sphere("aura", 0.8f, 24)
                .withColor("@primary")
                .withAlpha(0.4f))
            .prediction(PredictionConfig.defaults())
            .build());
        
        // Default growth field
        register(FieldDefinition.builder(
                Identifier.of("the-virus-block", "default_growth"),
                FieldType.AURA)
            .theme("cyber_purple")
            .layer(FieldLayer.sphere("pulse", 1.2f, 16)
                .withColor("@glow")
                .withAlpha(0.3f)
                .withPulse(0.5f))
            .build());
        
        // Test sphere - for /fieldtest debugging
        register(FieldDefinition.builder(
                Identifier.of("the-virus-block", "test_sphere"),
                FieldType.TEST)
            .theme("cyber_green")
            .baseRadius(3.0f)
            .layer(FieldLayer.sphere("main", 1.0f, 32)
                .withColor("@primary")
                .withAlpha(0.7f)
                .withSpin(0.02f))
            .build());
        
        Logging.REGISTRY.topic("field").info("Registered default field definitions");
    }
}
