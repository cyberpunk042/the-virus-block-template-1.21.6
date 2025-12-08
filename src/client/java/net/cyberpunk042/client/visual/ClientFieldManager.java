package net.cyberpunk042.client.visual;

import net.cyberpunk042.client.field._legacy.render.FieldRenderer_old;
import net.cyberpunk042.field.ClientFieldState;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.log.Logging;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side manager for field rendering and state.
 * 
 * <p>Receives field state from server via {@link net.cyberpunk042.network.FieldSpawnPayload}
 * and handles:
 * <ul>
 *   <li>Storing synced field states</li>
 *   <li>Rendering all active fields</li>
 *   <li>Managing personal field tracker</li>
 * </ul>
 * 
 * <p>Hook into WorldRenderEvents.AFTER_ENTITIES for rendering.
 */
public final class ClientFieldManager {
    
    private static final ClientFieldManager INSTANCE = new ClientFieldManager();
    
    private final Map<Long, ClientFieldState> states = new ConcurrentHashMap<>();
    private final PersonalFieldTracker personalTracker = new PersonalFieldTracker();
    
    private ClientFieldManager() {}
    
    public static ClientFieldManager get() {
        return INSTANCE;
    }
    
    // =========================================================================
    // State management (called from network handlers)
    // =========================================================================
    
    /**
     * Adds or updates a field state (from server sync).
     */
    public void addOrUpdate(ClientFieldState state) {
        boolean isNew = !states.containsKey(state.id());
        states.put(state.id(), state);
        if (isNew) {
            Logging.RENDER.topic("field").debug(
                "Client added field {} at {}", state.id(), state.position());
        } else {
            Logging.RENDER.topic("field").trace(
                "Client updated field {} at {}", state.id(), state.position());
        }
    }
    
    /**
     * Gets a field state by ID.
     * @return the state or null if not found
     */
    public ClientFieldState get(long id) {
        return states.get(id);
    }
    
    /**
     * Removes a field state.
     */
    public void remove(long id) {
        ClientFieldState removed = states.remove(id);
        if (removed != null) {
            Logging.RENDER.topic("field").debug("Client removed field {}", id);
        }
    }
    
    /**
     * Clears all states (on disconnect).
     */
    public void clear() {
        int count = states.size();
        states.clear();
        personalTracker.setEnabled(false);
        if (count > 0) {
            Logging.RENDER.topic("field").info("Client cleared {} field states", count);
        }
    }
    
    /**
     * @return the number of active field states
     */
    public int count() {
        return states.size();
    }
    
    // =========================================================================
    // Personal field
    // =========================================================================
    
    public PersonalFieldTracker personalTracker() {
        return personalTracker;
    }
    
    public void enablePersonalField(Identifier definitionId, float scale) {
        personalTracker.setDefinition(definitionId);
        personalTracker.setScale(scale);
        personalTracker.setEnabled(true);
        personalTracker.reset();
        Logging.RENDER.topic("field").info("Personal field enabled: {}", definitionId);
    }
    
    public void disablePersonalField() {
        personalTracker.setEnabled(false);
        Logging.RENDER.topic("field").info("Personal field disabled");
    }
    
    // =========================================================================
    // Tick
    // =========================================================================
    
    /**
     * Ticks all client-side state.
     * Call from client tick event.
     */
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            // Apply follow mode to test fields that should follow the player
            for (ClientFieldState state : states.values()) {
                tickFollowMode(state, client.player);
            }
        }
        
        states.values().removeIf(ClientFieldState::tick);
        personalTracker.tick();
    }
    
    /**
     * Applies follow mode interpolation to a field state.
     * Fields with follow mode enabled will smoothly track the player's position.
     * Prediction is applied to ALL modes including snap.
     */
    private void tickFollowMode(ClientFieldState state, net.minecraft.entity.player.PlayerEntity player) {
        String mode = state.followMode();
        if (mode == null || mode.isEmpty()) {
            mode = "snap"; // Default to snap
        }
        
        // Get target position (player center)
        Vec3d targetPos = player.getBoundingBox().getCenter();
        
        // Apply prediction if enabled (applies to ALL modes including snap)
        if (state.predictionEnabled()) {
            targetPos = applyPrediction(player, targetPos, state);
        }
        
        // For snap mode, immediately set to target position
        if ("snap".equals(mode)) {
            state.withInterpolatedPosition(targetPos);
            return;
        }
        
        // Get current interpolated position (or use target if first tick)
        Vec3d currentPos = state.interpolatedPosition();
        if (currentPos == null || currentPos.equals(Vec3d.ZERO)) {
            state.withInterpolatedPosition(targetPos);
            return;
        }
        
        // Interpolate based on follow mode
        float lerpFactor = switch (mode.toLowerCase()) {
            case "smooth" -> 0.35f;
            case "glide" -> 0.2f;
            default -> 1.0f; // snap
        };
        
        Vec3d nextPos = currentPos.lerp(targetPos, lerpFactor);
        state.withInterpolatedPosition(nextPos);
    }
    
    /**
     * Applies prediction offset based on player velocity and look direction.
     */
    private Vec3d applyPrediction(net.minecraft.entity.player.PlayerEntity player, Vec3d base, ClientFieldState state) {
        // Apply velocity prediction
        int leadTicks = state.predictionLeadTicks();
        Vec3d predicted = base.add(player.getVelocity().multiply(leadTicks));
        
        // Apply look-ahead offset
        float lookAhead = state.predictionLookAhead();
        if (Math.abs(lookAhead) > 0.001f) {
            Vec3d look = player.getRotationVector();
            predicted = predicted.add(look.multiply(lookAhead));
        }
        
        // Apply vertical boost
        float verticalBoost = state.predictionVerticalBoost();
        if (Math.abs(verticalBoost) > 0.001f) {
            predicted = predicted.add(0.0, verticalBoost, 0.0);
        }
        
        // Clamp to max distance
        float maxDist = state.predictionMaxDistance();
        if (maxDist > 0) {
            Vec3d delta = predicted.subtract(base);
            double dist = delta.length();
            if (dist > maxDist) {
                predicted = base.add(delta.normalize().multiply(maxDist));
            }
        }
        
        return predicted;
    }
    
    // =========================================================================
    // Rendering
    // =========================================================================
    
    /**
     * Renders all active fields.
     * Call from WorldRenderEvents.AFTER_ENTITIES.
     */
    public void render(WorldRenderContext context) {
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            return;
        }
        
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        MatrixStack matrices = context.matrixStack();
        
        float tickDelta = MinecraftClient.getInstance()
            .getRenderTickCounter()
            .getTickProgress(false);
        float worldTime = context.world().getTime() + tickDelta;
        
        // Render synced states (includes fields spawned via /fieldtest)
        for (ClientFieldState state : states.values()) {
            renderState(matrices, consumers, state, camPos, worldTime);
        }
        
        // Render personal field
        if (personalTracker.isVisible()) {
            renderPersonalField(matrices, consumers, camPos, worldTime);
        }
    }
    
    private void renderState(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            ClientFieldState state,
            Vec3d camPos,
            float worldTime) {
        
        FieldDefinition def = FieldRegistry.get(state.definitionId());
        if (def == null) {
            Logging.RENDER.topic("field").trace(
                "No definition for field {}: {}", state.id(), state.definitionId());
            return;
        }
        
        // Use interpolated position if follow mode is active (includes snap with prediction)
        String followMode = state.followMode();
        Vec3d fieldPos;
        Vec3d interpPos = state.interpolatedPosition();
        if (interpPos != null && !interpPos.equals(Vec3d.ZERO) && 
            (followMode != null && !followMode.isEmpty())) {
            // Use interpolated position for all follow modes (snap/smooth/glide)
            fieldPos = interpPos;
        } else {
            // Use raw position if no follow mode or no interpolation yet
            fieldPos = state.position();
        }
        
        Vec3d pos = fieldPos.subtract(camPos);
        float alpha = state.effectiveAlpha();
        float time = worldTime + state.phase();
        
        // Check for shuffle override
        if (state.hasShuffleOverride()) {
            // Reconstruct the pattern from shuffle type + index
            net.cyberpunk042.visual.pattern.VertexPattern pattern = reconstructPattern(
                state.shuffleType(), state.shuffleIndex());
            
            // DIAGNOSTIC: Log pattern override being applied
            if ((int) worldTime % 60 == 0) {
                Logging.RENDER.topic("shuffle").info(
                    "DIAG: Rendering with shuffle override: {}:{} -> {}", 
                    state.shuffleType(), state.shuffleIndex(), 
                    pattern != null ? pattern.id() : "null");
            }
            
            net.cyberpunk042.client.visual.render.RenderOverrides_old overrides = 
                net.cyberpunk042.client.visual.render.RenderOverrides_old.builder()
                    .vertexPattern(pattern)
                    .build();
            
            FieldRenderer_old.renderWithOverrides(
                matrices,
                consumers,
                def,
                pos,
                state.scale(),
                time,
                alpha,
                overrides
            );
        } else {
            FieldRenderer_old.render(
                matrices,
                consumers,
                def,
                pos,
                state.scale(),
                time,
                alpha
            );
        }
    }
    
    /**
     * Reconstructs a VertexPattern from shuffle type + index, or static pattern ID.
     * Uses ShuffleGenerator to get the same permutation as the server.
     */
    private static net.cyberpunk042.visual.pattern.VertexPattern reconstructPattern(String type, int index) {
        if (type == null || type.isEmpty()) {
            return null;
        }
        
        // Check for static pattern (prefixed with "static:")
        if (type.startsWith("static:")) {
            String patternId = type.substring(7); // Remove "static:" prefix
            return net.cyberpunk042.visual.pattern.VertexPattern.fromString(patternId);
        }
        
        // Dynamic shuffle pattern (index must be >= 0)
        if (index < 0) {
            return null;
        }
        
        return switch (type.toLowerCase()) {
            case "quad" -> {
                var arr = net.cyberpunk042.visual.pattern.ShuffleGenerator.getQuad(index);
                yield net.cyberpunk042.visual.pattern.DynamicQuadPattern.fromArrangement(arr);
            }
            case "segment" -> {
                var arr = net.cyberpunk042.visual.pattern.ShuffleGenerator.getSegment(index);
                yield net.cyberpunk042.visual.pattern.DynamicSegmentPattern.fromArrangement(arr);
            }
            case "sector" -> {
                var arr = net.cyberpunk042.visual.pattern.ShuffleGenerator.getSector(index);
                yield net.cyberpunk042.visual.pattern.DynamicSectorPattern.fromArrangement(arr);
            }
            case "edge" -> {
                var arr = net.cyberpunk042.visual.pattern.ShuffleGenerator.getEdge(index);
                yield net.cyberpunk042.visual.pattern.DynamicEdgePattern.fromArrangement(arr);
            }
            default -> null;
        };
    }
    
    private void renderPersonalField(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Vec3d camPos,
            float worldTime) {
        
        FieldDefinition def = personalTracker.definition();
        if (def == null) {
            return;
        }
        
        Vec3d pos = personalTracker.position().subtract(camPos);
        float time = worldTime + personalTracker.phase();
        
        FieldRenderer_old.render(
            matrices,
            consumers,
            def,
            pos,
            personalTracker.scale(),
            time,
            1.0f
        );
    }
}
