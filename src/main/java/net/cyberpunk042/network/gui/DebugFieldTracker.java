package net.cyberpunk042.network.gui;

import com.google.gson.JsonParser;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldManager;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.field.instance.PersonalFieldInstance;
import net.cyberpunk042.field.loader.FieldLoader;
import net.cyberpunk042.log.Logging;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks debug fields spawned by players from the GUI.
 * Debug fields are temporary preview fields that follow the player.
 */
public final class DebugFieldTracker {
    
    // Player UUID -> debug field instance ID
    private static final Map<UUID, Long> DEBUG_FIELDS = new ConcurrentHashMap<>();
    
    // Counter for generating unique debug definition IDs
    private static long debugCounter = 0;
    
    private DebugFieldTracker() {}
    
    /**
     * Spawn or update a debug field for a player.
     * @param player the player
     * @param definitionJson JSON string of the field definition
     * @return true if spawned/updated successfully
     */
    public static boolean spawnOrUpdate(ServerPlayerEntity player, String definitionJson) {
        UUID playerUuid = player.getUuid();
        ServerWorld world = player.getWorld();
        FieldManager manager = FieldManager.get(world);
        
        try {
            // Parse the definition
            FieldLoader loader = new FieldLoader();
            FieldDefinition definition = loader.parseDefinition(
                JsonParser.parseString(definitionJson).getAsJsonObject()
            );
            
            if (definition == null) {
                Logging.GUI.topic("debug").warn("Failed to parse debug field definition");
                return false;
            }
            
            // Register temporarily with unique ID embedded in the definition
            String debugId = "debug_" + playerUuid.toString().substring(0, 8) + "_" + (++debugCounter);
            FieldDefinition debugDef = FieldDefinition.builder(debugId)
                .type(definition.type())
                .baseRadius(definition.baseRadius())
                .themeId(definition.themeId())
                .layers(definition.layers())
                .modifiers(definition.modifiers())
                .prediction(definition.prediction())
                .beam(definition.beam())
                .followMode(definition.followMode())
                .bindings(definition.bindings())
                .triggers(definition.triggers())
                .lifecycle(definition.lifecycle())
                .build();
            FieldRegistry.register(debugDef);
            Identifier defId = Identifier.of("the-virus-block", debugId);
            
            // Remove existing debug field if any
            Long existingId = DEBUG_FIELDS.get(playerUuid);
            if (existingId != null) {
                manager.remove(existingId);
            }
            
            // Spawn new debug field following the player
            PersonalFieldInstance instance = manager.spawnForPlayer(defId, playerUuid, 1.0f);
            if (instance != null) {
                DEBUG_FIELDS.put(playerUuid, instance.id());
                Logging.GUI.topic("debug").debug("Spawned debug field {} for player {}", 
                    instance.id(), player.getName().getString());
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            Logging.GUI.topic("debug").error("Failed to spawn debug field: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Despawn the debug field for a player.
     */
    public static boolean despawn(ServerPlayerEntity player) {
        UUID playerUuid = player.getUuid();
        Long fieldId = DEBUG_FIELDS.remove(playerUuid);
        
        if (fieldId == null) {
            return false;
        }
        
        ServerWorld world = player.getWorld();
        FieldManager manager = FieldManager.get(world);
        boolean removed = manager.remove(fieldId);
        
        if (removed) {
            Logging.GUI.topic("debug").debug("Despawned debug field for player {}", 
                player.getName().getString());
        }
        
        return removed;
    }
    
    /**
     * Check if a player has a debug field.
     */
    public static boolean hasDebugField(UUID playerUuid) {
        return DEBUG_FIELDS.containsKey(playerUuid);
    }
    
    /**
     * Get the debug field ID for a player.
     */
    public static Long getDebugFieldId(UUID playerUuid) {
        return DEBUG_FIELDS.get(playerUuid);
    }
    
    /**
     * Clean up all debug fields (call on server stop).
     */
    public static void cleanup() {
        DEBUG_FIELDS.clear();
    }
}

