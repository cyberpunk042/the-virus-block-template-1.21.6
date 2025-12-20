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
        Logging.GUI.topic("field").info("test 00000");
        if (initialized) {
            Logging.GUI.topic("field").info("Initializing field client... already initialized");
            return;
        }
        initialized = true;
        
        Logging.GUI.topic("field").info("Initializing field client...");
        
        // Register payload receivers
        Logging.GUI.topic("field").info("Registering receiver for FieldSpawnPayload.ID = {}", FieldSpawnPayload.ID.id());
        ClientPlayNetworking.registerGlobalReceiver(FieldSpawnPayload.ID, (payload, context) -> {
            try {
                Logging.GUI.topic("field").info(">>> CLIENT RECEIVED FieldSpawnPayload id={} <<<", payload.id());
                context.client().execute(() -> handleSpawn(payload));
            } catch (Exception e) {
                Logging.RENDER.topic("field").error(">>> ERROR receiving FieldSpawnPayload <<<", e);
            }
        });
        
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
        
        Logging.GUI.topic("field").info("Registered field payload receivers (incl. legacy shield payloads)");
        
        Logging.GUI.topic("field").info("DEBUG: Before WorldRenderEvents registration");
        // Register render event
        WorldRenderEvents.AFTER_ENTITIES.register(context -> 
            ClientFieldManager.get().render(context));
        
        Logging.GUI.topic("field").info("DEBUG: Before ClientTickEvents registration");
        // Register tick event
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientFieldManager.get().tick();
            processPendingSpawns();
            ClientForceApplicator.tick();
        });
        
        Logging.GUI.topic("field").info("DEBUG: Before DISCONNECT registration");
        // Clear on disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> 
            client.execute(() -> ClientFieldManager.get().clear()));
        
        Logging.GUI.topic("field").info("DEBUG: Before registerBundledServerProfiles");
        // Register bundled SERVER profiles into FieldRegistry for rendering
        // This bridges ProfileManager (client GUI) with FieldRegistry (renderer)
        registerBundledServerProfiles();
        
        Logging.GUI.topic("field").info("DEBUG: Before JoinWarmupManager.init");
        // Initialize join warmup manager (handles warmup on server join)
        JoinWarmupManager.init();
        
        Logging.GUI.topic("field").info("DEBUG: Before warmup tick registration");
        // Tick the warmup manager
        ClientTickEvents.END_CLIENT_TICK.register(client -> JoinWarmupManager.tick());
        
        Logging.GUI.topic("field").info("DEBUG: Before WarmupOverlay.init");
        // Register warmup overlay HUD
        WarmupOverlay.init();
        
        Logging.GUI.topic("field").info("Field client initialized (render + tick events registered)");
    }
    
    /**
     * Handles field spawn payload from server.
     */
    private static void handleSpawn(FieldSpawnPayload payload) {
        try {
            Logging.GUI.topic("field").info(">>> handleSpawn() CALLED for field {} <<<", payload.id());
            Logging.GUI.topic("field").info("    definitionId string = '{}'", payload.definitionId());
            
            Identifier defId = payload.definitionIdentifier();
            Logging.GUI.topic("field").info("    parsed defId = {}", defId);
            
            if (defId == null) {
                Logging.GUI.topic("field").warn(
                    "Invalid definition ID in spawn payload: {}", payload.definitionId());
                return;
            }
            
            FieldDefinition def = FieldRegistry.get(defId);
            Logging.GUI.topic("field").info("    FieldRegistry.get({}) = {}", defId, def != null ? def.id() : "NULL");
            
            if (def == null) {
                Logging.GUI.topic("field").info(
                    "Definition {} not found yet, queuing retry...", defId);
                queueSpawnRetry(payload, 5);
                return;
            }
            
            Logging.GUI.topic("field").info("    Calling spawnFieldFromPayload...");
            spawnFieldFromPayload(payload, defId, def);
        } catch (Exception e) {
            Logging.GUI.topic("field").error(">>> EXCEPTION in handleSpawn: {} <<<", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static final java.util.List<PendingSpawn> pendingSpawns = new java.util.ArrayList<>();
    
    private static void queueSpawnRetry(FieldSpawnPayload payload, int ticksDelay) {
        pendingSpawns.add(new PendingSpawn(payload, ticksDelay));
    }
    
    private record PendingSpawn(FieldSpawnPayload payload, int ticksRemaining) {}
    
    // Called from tick handler to process pending spawns
    private static void processPendingSpawns() {
        if (pendingSpawns.isEmpty()) return;
        
        java.util.List<PendingSpawn> toRequeue = new java.util.ArrayList<>();
        var it = pendingSpawns.iterator();
        
        while (it.hasNext()) {
            PendingSpawn pending = it.next();
            it.remove(); // Always remove from current list
            
            if (pending.ticksRemaining <= 0) {
                // Timer expired, try to spawn
                Identifier defId = pending.payload.definitionIdentifier();
                if (defId != null) {
                    FieldDefinition def = FieldRegistry.get(defId);
                    if (def != null) {
                        spawnFieldFromPayload(pending.payload, defId, def);
                    } else {
                        Logging.RENDER.topic("field").warn(
                            "Definition {} still not found after retry, giving up", defId);
                    }
                }
            } else {
                // Timer not expired, requeue with decremented counter
                toRequeue.add(new PendingSpawn(pending.payload, pending.ticksRemaining - 1));
            }
        }
        
        // Add back the items that need to wait longer
        pendingSpawns.addAll(toRequeue);
    }
    
    private static void spawnFieldFromPayload(FieldSpawnPayload payload, Identifier defId, FieldDefinition def) {
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
        Logging.GUI.topic("field").info(">>> handleDefinitionSync CALLED: id='{}' <<<", payload.definitionId());
        
        Identifier id = payload.definitionIdentifier();
        if (id == null) {
            Logging.GUI.topic("field").warn("Invalid definition ID: {}", payload.definitionId());
            return;
        }
        
        try {
            var json = com.google.gson.JsonParser.parseString(payload.definitionJson()).getAsJsonObject();
            net.cyberpunk042.field.loader.FieldLoader loader = new net.cyberpunk042.field.loader.FieldLoader();
            var definition = loader.parseDefinition(json);
            FieldRegistry.register(definition);
            Logging.GUI.topic("field").info(">>> Definition REGISTERED: {} <<<", id);
        } catch (Exception e) {
            Logging.GUI.topic("field").error("Failed to parse definition {}: {}", id, e.getMessage());
            e.printStackTrace();
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
        Identifier.of("the-virus-block", "anti_virus");
    
    /**
     * Handles legacy shield spawn payload from ShieldFieldService.
     * Maps to an anti-virus field definition in the new system.
     */
    private static void handleLegacyShieldSpawn(ShieldFieldSpawnPayload payload) {
        Logging.GUI.topic("field").info(">>> handleLegacyShieldSpawn CALLED id={} <<<", payload.id());
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
    
    // =========================================================================
    // Profile → Registry Bridge
    // =========================================================================
    
    /**
     * Registers bundled SERVER profiles from ProfileManager into FieldRegistry.
     * 
     * <p>This bridges the gap between:
     * <ul>
     *   <li>ProfileManager - loads profiles from JAR (field_profiles/*.json)</li>
     *   <li>FieldRegistry - provides definitions for rendering</li>
     * </ul>
     * 
     * <p>Only SERVER-source profiles are registered (bundled with mod JAR).
     * Network-synced definitions are handled separately via handleDefinitionSync().
     */
    private static void registerBundledServerProfiles() {
        var profileManager = net.cyberpunk042.client.profile.ProfileManager.getInstance();
        
        // Ensure profiles are loaded
        profileManager.loadAll();
        
        int registered = 0;
        for (var profile : profileManager.getAllProfiles()) {
            // Only register SERVER source (bundled with JAR, not local or network)
            if (profile.source() != net.cyberpunk042.field.category.ProfileSource.SERVER) {
                continue;
            }
            
            // Get the definition from the profile
            net.cyberpunk042.field.FieldDefinition definition = profile.definition();
            if (definition == null) {
                Logging.RENDER.topic("field").debug(
                    "Profile '{}' has no definition, skipping registry", profile.id());
                continue;
            }
            
            // Register in FieldRegistry for rendering lookup
            FieldRegistry.register(definition);
            registered++;
            
            Logging.RENDER.topic("field").debug(
                "Registered bundled profile: {} (def={})", profile.id(), definition.id());
        }
        
        Logging.RENDER.topic("field").info(
            "Registered {} bundled SERVER profiles into FieldRegistry", registered);
        
        // Warm up the rendering pipeline so first spawn is fast
        warmupRenderingPipeline();
    }
    
    /**
     * Pre-tessellates common shapes to trigger JIT compilation.
     * This ensures the first actual field spawn renders instantly.
     */
    private static void warmupRenderingPipeline() {
        long start = System.currentTimeMillis();
        
        try {
            // Warm up sphere tessellation (most common shape)
            // Using similar params to anti_virus profile
            var sphereShape = net.cyberpunk042.visual.shape.SphereShape.builder()
                .radius(12.0f)
                .latSteps(19)
                .lonSteps(23)
                .algorithm(net.cyberpunk042.visual.shape.SphereAlgorithm.LAT_LON)
                .build();
            
            // Tessellate without pattern (warmup vertex generation)
            net.cyberpunk042.client.visual.mesh.Mesh mesh1 = 
                net.cyberpunk042.client.visual.mesh.SphereTessellator.tessellate(sphereShape);
            
            // Also warm up with a pattern (different code path)
            net.cyberpunk042.client.visual.mesh.Mesh mesh2 = 
                net.cyberpunk042.client.visual.mesh.SphereTessellator.tessellate(
                    sphereShape, 
                    net.cyberpunk042.visual.pattern.QuadPattern.STRIPE_1,
                    null, null, 0);
            
            long took = System.currentTimeMillis() - start;
            Logging.RENDER.topic("field").info(
                "Rendering pipeline warmup complete: {}ms (mesh1={} verts, mesh2={} verts)",
                took, 
                mesh1 != null ? mesh1.vertexCount() : 0,
                mesh2 != null ? mesh2.vertexCount() : 0);
                
        } catch (Exception e) {
            Logging.RENDER.topic("field").warn("Warmup failed (non-critical): {}", e.getMessage());
        }
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
