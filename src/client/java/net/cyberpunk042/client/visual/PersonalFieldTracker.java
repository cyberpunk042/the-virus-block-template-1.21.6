package net.cyberpunk042.client.visual;

import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.field.instance.PredictionConfig;
import net.cyberpunk042.field.instance.FollowMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Tracks and smooths the position of a personal field attached to a player.
 * 
 * <p>Features:
 * <ul>
 *   <li>Movement prediction (anticipates player movement)</li>
 *   <li>Smooth interpolation (prevents jitter)</li>
 *   <li>Look-ahead (field leads movement direction)</li>
 * </ul>
 */
public final class PersonalFieldTracker {
    
    // FollowMode is now in net.cyberpunk042.field.instance.FollowMode
    
    // Configuration
    private Identifier definitionId;
    private FollowMode followMode = FollowMode.SMOOTH;
    private float scale = 1.0f;
    private boolean enabled = false;
    private boolean visible = true;
    
    // State
    private Vec3d currentPosition = Vec3d.ZERO;
    private Vec3d targetPosition = Vec3d.ZERO;
    private float phase = 0;
    private int age = 0;
    
    public PersonalFieldTracker() {}
    
    // =========================================================================
    // Configuration
    // =========================================================================
    
    public void setDefinition(Identifier id) {
        this.definitionId = id;
    }
    
    public void setFollowMode(FollowMode mode) {
        this.followMode = mode != null ? mode : FollowMode.SMOOTH;
    }
    
    public void setScale(float scale) {
        this.scale = Math.max(0.1f, scale);
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            age = 0;
        }
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    // =========================================================================
    // State queries
    // =========================================================================
    
    public boolean isEnabled() { return enabled; }
    public boolean isVisible() { return visible && enabled; }
    public Identifier definitionId() { return definitionId; }
    public Vec3d position() { return currentPosition; }
    public float scale() { return scale; }
    public float phase() { return phase; }
    public int age() { return age; }
    
    public FieldDefinition definition() {
        return definitionId != null ? FieldRegistry.get(definitionId) : null;
    }
    
    // =========================================================================
    // Update
    // =========================================================================
    
    /**
     * Updates the tracker for the current tick.
     * Call once per client tick.
     */
    public void tick() {
        if (!enabled) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        
        if (player == null) {
            return;
        }
        
        age++;
        
        // Calculate target position
        targetPosition = calculateTargetPosition(player);
        
        // Interpolate current position
        currentPosition = followMode.interpolate(currentPosition, targetPosition);
    }
    
    /**
     * Calculates the target position for the field.
     */
    private Vec3d calculateTargetPosition(PlayerEntity player) {
        // Base: center of player bounding box, slightly down
        Vec3d base = player.getBoundingBox().getCenter().add(0, -0.1, 0);
        
        // Get prediction config from definition
        FieldDefinition def = definition();
        if (def == null) {
            return base;
        }
        
        PredictionConfig pred = def.prediction();
        if (pred == null || !pred.enabled()) {
            return base;
        }
        
        // Apply velocity prediction
        Vec3d predicted = base;
        if (pred.leadTicks() > 0) {
            Vec3d velocity = player.getVelocity();
            predicted = base.add(velocity.multiply(pred.leadTicks()));
        }
        
        // Apply look-ahead
        if (pred.lookAhead() != 0) {
            Vec3d look = player.getRotationVec(1.0f).normalize();
            predicted = predicted.add(look.multiply(pred.lookAhead()));
        }
        
        // Apply vertical boost
        if (pred.verticalBoost() != 0) {
            predicted = predicted.add(0, pred.verticalBoost(), 0);
        }
        
        // Clamp to max distance
        if (pred.maxDistance() > 0.01f) {
            Vec3d delta = predicted.subtract(base);
            double distSq = delta.lengthSquared();
            double maxDistSq = pred.maxDistance() * pred.maxDistance();
            
            if (distSq > maxDistSq) {
                predicted = base.add(delta.normalize().multiply(pred.maxDistance()));
            }
        }
        
        return predicted;
    }
    
    /**
     * Resets the tracker state.
     */
    public void reset() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            currentPosition = client.player.getBoundingBox().getCenter();
            targetPosition = currentPosition;
        }
        phase = (float) (Math.random() * Math.PI * 2);
        age = 0;
    }
}
