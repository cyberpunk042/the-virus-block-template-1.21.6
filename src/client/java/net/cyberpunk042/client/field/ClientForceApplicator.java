package net.cyberpunk042.client.field;

import net.cyberpunk042.field.ClientFieldState;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.field.force.ForceFieldConfig;
import net.cyberpunk042.field.force.core.ForceContext;
import net.cyberpunk042.field.force.field.RadialForceField;
import net.cyberpunk042.client.visual.ClientFieldManager;
import net.cyberpunk042.log.Logging;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Client-side force field applicator for immediate force feedback.
 * 
 * <p>This applies forces to the LOCAL player on the client side, providing
 * immediate and smooth force feedback. The server still applies forces
 * authoritatively, but client-side application:
 * <ul>
 *   <li>Eliminates network latency feel</li>
 *   <li>Allows for tickDelta interpolation</li>
 *   <li>Matches the smoothness of visual rendering</li>
 * </ul>
 * 
 * <p>This is similar to how Minecraft handles player movement - client
 * predicts, server validates.
 */
public final class ClientForceApplicator {
    
    private static boolean enabled = true;
    
    private ClientForceApplicator() {}
    
    /**
     * Enables or disables client-side force application.
     */
    public static void setEnabled(boolean enable) {
        enabled = enable;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Applies forces to the local player from all active force fields.
     * Call this from the client tick event.
     */
    public static void tick() {
        if (!enabled) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        
        ClientPlayerEntity player = client.player;
        
        // Skip if player is not alive or in spectator mode
        if (!player.isAlive() || player.isSpectator()) return;
        
        // Get all active field states from ClientFieldManager
        var allStates = ClientFieldManager.get().allStates();
        
        // Log periodically (once per second) to help debug
        if (client.world.getTime() % 20 == 0) {
            Logging.GUI.topic("force").info(
                "ClientForceApplicator: {} field states active", allStates.size());
        }
        
        // Apply forces from each field that has a forceConfig
        for (ClientFieldState state : allStates) {
            applyForceFromField(player, state);
        }
    }
    
    /**
     * Applies force to the local player from a single field.
     */
    private static void applyForceFromField(ClientPlayerEntity player, ClientFieldState state) {
        // Get field definition
        Identifier defId = state.definitionId();
        FieldDefinition def = FieldRegistry.get(defId);
        
        // Log ALL fields once per second to see what's being processed
        if (state.age() % 20 == 0) {
            String defIdStr = defId != null ? defId.toString() : "NULL";
            String defStr = def != null ? def.id() : "NULL";
            ForceFieldConfig fc = def != null ? def.forceConfig() : null;
            Logging.GUI.topic("force").info(
                "Field {} check: defId={} def={} forceConfig={}", 
                state.id(), defIdStr, defStr, fc != null ? "EXISTS" : "NULL");
        }
        
        if (def == null) {
            return;
        }
        
        // Check if field has force config
        ForceFieldConfig forceConfig = def.forceConfig();
        if (forceConfig == null) {
            return;
        }
        
        // Get field position - use base position (interpolation is for rendering only)
        Vec3d fieldPos = state.position();
        if (fieldPos == null) {
            Logging.GUI.topic("force").warn("Field {} has null position!", state.id());
            return;
        }
        
        // Calculate player position (center of body)
        Vec3d playerPos = player.getPos().add(0, player.getHeight() / 2.0, 0);
        
        // Log positions for debugging
        if (state.age() % 20 == 0) {
            Logging.GUI.topic("force").info(
                "Field {} positions: field=({},{},{}) player=({},{},{})",
                state.id(),
                String.format("%.1f", fieldPos.x), String.format("%.1f", fieldPos.y), String.format("%.1f", fieldPos.z),
                String.format("%.1f", playerPos.x), String.format("%.1f", playerPos.y), String.format("%.1f", playerPos.z));
        }
        
        // Calculate normalized time for phase effects
        float normalizedTime = state.maxLifeTicks() > 0 
            ? (float) state.age() / state.maxLifeTicks() 
            : 0.5f;
        
        // Create force field calculator
        RadialForceField forceField = new RadialForceField(forceConfig);
        
        // Create force context
        ForceContext context = ForceContext.forEntity(
            player, fieldPos, normalizedTime, state.age(), state.maxLifeTicks());
        
        // Check if in range
        double distance = context.distance();
        if (state.age() % 20 == 0) {
            Logging.GUI.topic("force").info(
                "Distance check: dist={} affectsDistance={}", 
                String.format("%.1f", distance), forceField.affectsDistance(distance));
        }
        if (!forceField.affectsDistance(distance)) {
            return;
        }
        
        // Calculate force
        Vec3d force = forceField.calculateForce(context);
        if (state.age() % 20 == 0) {
            Logging.GUI.topic("force").info(
                "Force calculated: ({}, {}, {}) lengthSq={}", 
                String.format("%.3f", force.x), 
                String.format("%.3f", force.y), 
                String.format("%.3f", force.z),
                String.format("%.4f", force.lengthSquared()));
        }
        if (force.lengthSquared() < 0.0001) {
            return;
        }
        
        // Apply damping to existing velocity (like server does)
        Vec3d currentVel = player.getVelocity();
        if (forceConfig.damping() > 0) {
            currentVel = currentVel.multiply(1.0 - forceConfig.damping());
        }
        
        // Add force to velocity
        Vec3d newVel = currentVel.add(force);
        
        // Cap velocity (like server does)
        float maxVel = forceConfig.maxVelocity();
        double hSpeed = newVel.horizontalLength();
        if (hSpeed > maxVel) {
            double scale = maxVel / hSpeed;
            newVel = new Vec3d(newVel.x * scale, newVel.y, newVel.z * scale);
        }
        newVel = new Vec3d(
            newVel.x, 
            net.minecraft.util.math.MathHelper.clamp(newVel.y, -maxVel, maxVel), 
            newVel.z
        );
        
        // Apply final velocity
        player.setVelocity(newVel);
        
        // Log occasionally for debugging
        if (state.age() % 20 == 0) {
            Logging.GUI.topic("force").info(
                "FORCE APPLIED: dist={} force=({},{},{}) time={}%",
                String.format("%.1f", distance), 
                String.format("%.3f", force.x), 
                String.format("%.3f", force.y), 
                String.format("%.3f", force.z), 
                String.format("%.0f", normalizedTime * 100));
        }
    }
}
