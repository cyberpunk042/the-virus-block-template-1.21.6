package net.cyberpunk042.client.field;

import net.cyberpunk042.client.visual.ClientFieldManager;
import net.cyberpunk042.field.ClientFieldState;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.field.FieldType;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.network.FieldRemovePayload;
import net.cyberpunk042.network.FieldSpawnPayload;
import net.cyberpunk042.network.FieldUpdatePayload;
import net.cyberpunk042.network.FieldDefinitionSyncPayload;
import net.cyberpunk042.network.ShieldFieldSpawnPayload;
import net.cyberpunk042.network.ShieldFieldRemovePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Client-side initialization for the field system.
 * 
 * <p>Call {@link #init()} during client mod initialization to:
 * <ul>
 *   <li>Register network payload receivers</li>
 *   <li>Initialize client-side field state</li>
 * </ul>
 */
public final class FieldClientInit {
    
    private static boolean initialized = false;
    
    private FieldClientInit() {}
    
    /**
     * Initializes client-side field system.
     * Safe to call multiple times.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        
        Logging.RENDER.topic("field").info("Initializing field client...");
        
        // Register payload receivers
        ClientPlayNetworking.registerGlobalReceiver(FieldSpawnPayload.ID, (payload, context) ->
            context.client().execute(() -> handleSpawn(payload)));
        
        ClientPlayNetworking.registerGlobalReceiver(FieldRemovePayload.ID, (payload, context) ->
            context.client().execute(() -> handleRemove(payload)));
        
        ClientPlayNetworking.registerGlobalReceiver(FieldUpdatePayload.ID, (payload, context) ->
            context.client().execute(() -> handleUpdate(payload)));
        
        ClientPlayNetworking.registerGlobalReceiver(FieldDefinitionSyncPayload.ID, (payload, context) ->
            context.client().execute(() -> handleDefinitionSync(payload)));
        
        // Legacy shield payloads (anti-virus fields from ShieldFieldService)
        ClientPlayNetworking.registerGlobalReceiver(ShieldFieldSpawnPayload.ID, (payload, context) ->
            context.client().execute(() -> handleLegacyShieldSpawn(payload)));
        
        ClientPlayNetworking.registerGlobalReceiver(ShieldFieldRemovePayload.ID, (payload, context) ->
            context.client().execute(() -> handleLegacyShieldRemove(payload)));
        
        Logging.RENDER.topic("field").info("Registered field payload receivers (incl. legacy shield payloads)");
        
        // Register render event
        WorldRenderEvents.AFTER_ENTITIES.register(context -> 
            ClientFieldManager.get().render(context));
        
        // Register tick event
        ClientTickEvents.END_CLIENT_TICK.register(client -> 
            ClientFieldManager.get().tick());
        
        // Clear on disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> 
            client.execute(() -> ClientFieldManager.get().clear()));
        
        Logging.RENDER.topic("field").info("Field client initialized (render + tick events registered)");
    }
    
    /**
     * Handles field spawn payload from server.
     */
    private static void handleSpawn(FieldSpawnPayload payload) {
        Identifier defId = payload.definitionIdentifier();
        if (defId == null) {
            Logging.RENDER.topic("field").warn(
                "Invalid definition ID in spawn payload: {}", payload.definitionId());
            return;
        }
        
        FieldDefinition def = FieldRegistry.get(defId);
        if (def == null) {
            Logging.RENDER.topic("field").warn(
                "Unknown field definition: {}", defId);
            return;
        }
        
        Vec3d pos = new Vec3d(payload.x(), payload.y(), payload.z());
        ClientFieldState state = ClientFieldState.atPosition(
            payload.id(), defId, def.type(), pos)
            .withScale(payload.scale())
            .withPhase(payload.phase())
            .withLifetime(payload.lifetimeTicks());
        
        ClientFieldManager.get().addOrUpdate(state);
        
        Logging.RENDER.topic("field").info(
            "Spawned field: id={} def={} pos=({:.1f},{:.1f},{:.1f})",
            payload.id(), defId.getPath(), pos.x, pos.y, pos.z);
    }
    
    /**
     * Handles field removal payload from server.
     */
    private static void handleRemove(FieldRemovePayload payload) {
        ClientFieldManager.get().remove(payload.id());
        
        Logging.RENDER.topic("field").debug(
            "Removed field: id={}", payload.id());
    }
    
    /**
     * Handles field update payload from server.
     */
    private static void handleUpdate(FieldUpdatePayload payload) {
        ClientFieldState existing = ClientFieldManager.get().get(payload.id());
        if (existing == null) {
            Logging.RENDER.topic("field").trace(
                "Update for unknown field: id={}", payload.id());
            return;
        }
        
        Vec3d newPos = new Vec3d(payload.x(), payload.y(), payload.z());
        existing.withPosition(newPos).withAlpha(payload.alpha());
        
        // Apply shuffle override if present (always apply if shuffleType is non-empty)
        String shuffleType = payload.shuffleType();
        int shuffleIndex = payload.shuffleIndex();
        
        if (shuffleType != null && !shuffleType.isEmpty()) {
            existing.withShuffleOverride(shuffleType, shuffleIndex);
            
            // Enhanced diagnostic - always log pattern updates
            Logging.RENDER.topic("pattern").info(
                ">>> PATTERN UPDATE: id={} type='{}' index={} (static={})", 
                payload.id(), shuffleType, shuffleIndex, payload.isStaticPattern());
        }
        
        // Apply follow mode and prediction if present
        existing.withFollowMode(payload.followMode())
            .withPrediction(payload.predictionEnabled(), payload.predictionLeadTicks(),
                payload.predictionMaxDistance(), payload.predictionLookAhead(),
                payload.predictionVerticalBoost());
        
        Logging.RENDER.topic("field").debug(
            "Updated field: id={} shuffle={}:{} follow={} predict={}",
            payload.id(), shuffleType, shuffleIndex, 
            payload.followMode(), payload.predictionEnabled());
    }
    
    /**
     * Handles field definition sync from server.
     * Stores the definition in client-side registry so fields can be rendered.
     */
    private static void handleDefinitionSync(FieldDefinitionSyncPayload payload) {
        Identifier id = payload.definitionIdentifier();
        if (id == null) {
            Logging.RENDER.topic("field").warn("Invalid definition ID: {}", payload.definitionId());
            return;
        }
        
        try {
            var json = com.google.gson.JsonParser.parseString(payload.definitionJson()).getAsJsonObject();
            var definition = FieldDefinition.fromJson(json, id);
            FieldRegistry.register(definition);
            Logging.RENDER.topic("field").debug("Received definition: {}", id);
        } catch (Exception e) {
            Logging.RENDER.topic("field").error("Failed to parse definition {}: {}", id, e.getMessage());
        }
    }

    // =========================================================================
    // Legacy Shield Payload Handlers
    // =========================================================================
    
    /**
     * Default definition ID for anti-virus shield fields.
     * This maps legacy ShieldFieldSpawnPayload to the new field system.
     */
    private static final Identifier ANTI_VIRUS_FIELD_ID = 
        Identifier.of("the-virus-block", "alpha_antivirus");
    
    /**
     * Handles legacy shield spawn payload from ShieldFieldService.
     * Maps to an anti-virus field definition in the new system.
     */
    private static void handleLegacyShieldSpawn(ShieldFieldSpawnPayload payload) {
        FieldDefinition def = FieldRegistry.get(ANTI_VIRUS_FIELD_ID);
        if (def == null) {
            Logging.RENDER.topic("field").warn(
                "Anti-virus field definition not found: {}", ANTI_VIRUS_FIELD_ID);
            return;
        }
        
        Vec3d pos = new Vec3d(payload.x(), payload.y(), payload.z());
        
        // Scale based on radius (legacy system used configurable radius)
        // Base radius in definition is typically 1.0, so scale by payload radius
        float scale = payload.radius() / 12.0f; // 12.0 was SHIELD_FIELD_RADIUS in old system
        
        ClientFieldState state = ClientFieldState.atPosition(
            payload.id(), ANTI_VIRUS_FIELD_ID, FieldType.SHIELD, pos)
            .withScale(scale)
            .withLifetime(-1); // Indefinite until server removes
        
        ClientFieldManager.get().addOrUpdate(state);
        
        Logging.RENDER.topic("field").info(
            "Spawned anti-virus field: id={} pos=({:.1f},{:.1f},{:.1f}) radius={}",
            payload.id(), pos.x, pos.y, pos.z, payload.radius());
    }
    
    /**
     * Handles legacy shield removal payload.
     */
    private static void handleLegacyShieldRemove(ShieldFieldRemovePayload payload) {
        ClientFieldManager.get().remove(payload.id());
        
        Logging.RENDER.topic("field").debug(
            "Removed anti-virus field: id={}", payload.id());
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
