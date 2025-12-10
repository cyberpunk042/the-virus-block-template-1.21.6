package net.cyberpunk042.field;

import net.cyberpunk042.command.field.FieldCommand;
import net.cyberpunk042.command.field.FieldTestCommand;
import net.cyberpunk042.field.FieldManager;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.network.FieldNetworking;
import net.cyberpunk042.network.FieldRemovePayload;
import net.cyberpunk042.network.FieldSpawnPayload;
import net.cyberpunk042.network.FieldUpdatePayload;
import net.cyberpunk042.network.FieldDefinitionSyncPayload;
import net.cyberpunk042.visual.color.ColorThemeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Central initialization for the field system.
 * 
 * <p>Call {@link #init()} during mod initialization to:
 * <ul>
 *   <li>Register field network payloads</li>
 *   <li>Register field commands</li>
 *   <li>Register resource reload listener for JSON definitions</li>
 *   <li>Load built-in field definitions</li>
 *   <li>Load built-in color themes</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * In your mod initializer:
 * <pre>
 * public void onInitialize() {
 *     FieldSystemInit.init();
 *     // ...
 * }
 * </pre>
 */
public final class FieldSystemInit {
    
    private static boolean initialized = false;
    
    private FieldSystemInit() {}
    
    /**
     * Initializes the field system.
     * Safe to call multiple times (only first call has effect).
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        
        Logging.REGISTRY.topic("field").info("Initializing field system...");
        
        // Register network payloads (server -> client)
        PayloadTypeRegistry.playS2C().register(FieldSpawnPayload.ID, FieldSpawnPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FieldRemovePayload.ID, FieldRemovePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FieldUpdatePayload.ID, FieldUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FieldDefinitionSyncPayload.ID, FieldDefinitionSyncPayload.CODEC);
        Logging.REGISTRY.topic("field").info("Registered field payloads");
        
        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            FieldCommand.register(dispatcher);
            FieldTestCommand.register(dispatcher);
        });
        Logging.REGISTRY.topic("field").info("Registered /field and /fieldtest command trees");
        
        // Initialize FieldLoader for hot-reload support
        // FieldLoader initialized via FieldRegistry
        
        // Register resource reload listener for JSON field definitions
        // This loads definitions from data/the-virus-block/field_definitions/*.json
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(
            new SimpleSynchronousResourceReloadListener() {
                @Override
                public Identifier getFabricId() {
                    return Identifier.of("the-virus-block", "field_definitions");
                }
                
                @Override
                public void reload(ResourceManager manager) {
                    Logging.REGISTRY.topic("field").info("Reloading field definitions from resources...");
                    FieldRegistry.clear();
                    FieldRegistry.registerDefaults();
                    var loader = new net.cyberpunk042.field.loader.FieldLoader();
                    loader.load(manager);
                    int loaded = FieldRegistry.count();
                    Logging.REGISTRY.topic("field").info("Loaded {} field definitions from JSON", loaded);
                    Logging.FIELD.topic("registry").info("Registry: {} fields loaded", FieldRegistry.count());
                }
            }
        );
        Logging.REGISTRY.topic("field").info("Registered field definition reload listener");
        
        // Wire FieldManager to FieldNetworking for automatic sync
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            FieldManager manager = FieldManager.get(world);
            // Wire callbacks if not already done
            if (manager != null) {
                manager.onSpawn(instance -> FieldNetworking.sendSpawn(world, instance));
                manager.onRemove(id -> FieldNetworking.sendRemove(world, id));
                manager.onUpdate(instance -> FieldNetworking.sendUpdate(world, instance));
                manager.tick();
            }
        });
        
        // Sync existing fields to players who join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            server.execute(() -> {
                // Sync definitions first so client knows how to render
                FieldNetworking.syncDefinitionsTo(player);
                
                // Then sync any active field instances
                if (player.getWorld() != null) {
                    FieldManager manager = FieldManager.get(player.getWorld());
                    if (manager != null) {
                        FieldNetworking.syncAllTo(player, manager.all());
                    }
                }
            });
        });
        
        Logging.REGISTRY.topic("field").info("Wired FieldManager to FieldNetworking");
        
        // Load built-in color themes
        int themeCount = ColorThemeRegistry.count(); // Force class load
        Logging.REGISTRY.topic("field").info("Color themes loaded: {}", themeCount);
        
        // Register defaults (will be supplemented by JSON on resource load)
        FieldRegistry.registerDefaults();
        Logging.FIELD.topic("registry").info("Registry: {} fields loaded", FieldRegistry.count());
        
        Logging.REGISTRY.topic("field").info("Field system initialized");
    }
    
    /**
     * Returns true if the field system has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
