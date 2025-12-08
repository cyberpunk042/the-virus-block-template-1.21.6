package net.cyberpunk042.field.instance;

import net.cyberpunk042.log.Logging;

import net.cyberpunk042.field.FieldType;
import net.cyberpunk042.field.instance.PredictionConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * A field instance attached to a player.
 * 
 * <h2>Follow Modes</h2>
 * <ul>
 *   <li>{@link FollowMode#SNAP}: Instant teleport to player position</li>
 *   <li>{@link FollowMode#SMOOTH}: Smooth interpolation (slight lag)</li>
 *   <li>{@link FollowMode#GLIDE}: Very slow, floaty movement</li>
 * </ul>
 * 
 * <h2>Prediction</h2>
 * <p>Prediction is controlled separately via {@link PredictionConfig},
 * and can be combined with any follow mode.
 */
public class PersonalFieldInstance extends FieldInstance {
    
    private final UUID ownerUuid;
    private FollowMode followMode;
    private PredictionConfig predictionConfig;
    
    // Smoothing state
    private Vec3d targetPosition;
    private Vec3d velocity;
    private float smoothFactor;
    
    public PersonalFieldInstance(long id, Identifier definitionId, FieldType type,
                                  UUID ownerUuid, Vec3d initialPosition) {
        super(id, definitionId, type, initialPosition);
        this.ownerUuid = ownerUuid;
        this.followMode = FollowMode.SMOOTH;
        this.predictionConfig = PredictionConfig.defaults();
        this.targetPosition = initialPosition;
        this.velocity = Vec3d.ZERO;
        this.smoothFactor = 0.3f;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Configuration
    // ─────────────────────────────────────────────────────────────────────────────
    
    public FollowMode getFollowMode() {
        return followMode;
    }
    
    public void setFollowMode(FollowMode mode) {
        if (this.followMode != mode) {
            Logging.REGISTRY.topic("personal").debug(
                "Follow mode changed: {} -> {} for player {}", 
                this.followMode.id(), mode.id(), ownerUuid);
        }
        this.followMode = mode;
    }
    
    public PredictionConfig getPredictionConfig() {
        return predictionConfig;
    }
    
    public void setPredictionConfig(PredictionConfig config) {
        Logging.REGISTRY.topic("personal").debug(
            "Prediction config updated for player {}: enabled={}, leadTicks={}",
            ownerUuid, config.enabled(), config.leadTicks());
        this.predictionConfig = config;
    }
    
    public float getSmoothFactor() {
        return smoothFactor;
    }
    
    public void setSmoothFactor(float factor) {
        this.smoothFactor = Math.max(0.05f, Math.min(1.0f, factor));
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Position Update
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Updates the field position based on player position.
     * Called from server when player moves.
     */
    public void updateFromPlayer(PlayerEntity player) {
        if (player == null) {
            return;
        }
        
        Vec3d playerPos = player.getPos().add(0, 1.0, 0); // Center on player
        
        // Apply prediction if enabled (works with any follow mode)
        Vec3d targetPos = predictionConfig.enabled() 
            ? predictPosition(player) 
            : playerPos;
        
        switch (followMode) {
            case SNAP:
                // Instant teleport - no interpolation
                position = targetPos;
                targetPosition = targetPos;
                break;
                
            case SMOOTH:
            case GLIDE:
                // Set target, interpolation happens in tickInstance
                targetPosition = targetPos;
                break;
        }
    }
    
    /**
     * Predicts where the player will be based on velocity and look direction.
     */
    private Vec3d predictPosition(PlayerEntity player) {
        if (!predictionConfig.enabled()) {
            return player.getPos().add(0, 1.0, 0);
        }
        
        Vec3d playerPos = player.getPos().add(0, 1.0, 0);
        Vec3d playerVel = player.getVelocity();
        
        // Clamp velocity
        double speed = playerVel.length();
        if (speed > predictionConfig.maxDistance()) {
            playerVel = playerVel.normalize().multiply(predictionConfig.maxDistance());
        }
        
        // Lead based on velocity
        Vec3d leadPos = playerPos.add(playerVel.multiply(predictionConfig.leadTicks()));
        
        // Add look-ahead component
        Vec3d lookVec = player.getRotationVector();
        leadPos = leadPos.add(lookVec.multiply(predictionConfig.lookAhead()));
        
        // Vertical boost when jumping/falling
        if (Math.abs(playerVel.y) > 0.1) {
            leadPos = leadPos.add(0, playerVel.y * predictionConfig.verticalBoost(), 0);
        }
        
        return leadPos;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Tick
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    protected void tickInstance() {
        // Smooth/glide interpolation toward target (SNAP handled in updateFromPlayer)
        if (followMode == FollowMode.SMOOTH || followMode == FollowMode.GLIDE) {
            // Use the follow mode's lerp factor for consistency
            float lerpFactor = followMode.lerpFactor();
            position = position.lerp(targetPosition, lerpFactor);
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Overrides
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public UUID ownerUuid() {
        return ownerUuid;
    }
}
