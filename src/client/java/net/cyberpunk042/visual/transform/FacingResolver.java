package net.cyberpunk042.client.visual.transform;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.cyberpunk042.visual.transform.Facing;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Resolves {@link Facing} modes to rotation quaternions.
 * 
 * <p>Facing modes determine what direction the primitive looks:</p>
 * <ul>
 *   <li>FIXED - No rotation (uses transform rotation)</li>
 *   <li>PLAYER_LOOK - Faces where player is looking</li>
 *   <li>VELOCITY - Faces movement direction</li>
 *   <li>CAMERA - Faces the camera (viewer)</li>
 * </ul>
 * 
 * @see Facing
 * @see Transform
 * @see TransformApplier
 */
public final class FacingResolver {
    
    private FacingResolver() {}
    
    // =========================================================================
    // Resolution
    // =========================================================================
    
    /**
     * Resolves a facing mode to a rotation quaternion.
     * 
     * @param facing The facing mode
     * @param primitivePos Position of the primitive
     * @param player The player (for look direction, velocity)
     * @param camera The camera (for camera facing)
     * @return Rotation quaternion
     */
    public static Quaternionf resolve(Facing facing, Vec3d primitivePos, 
                                       PlayerEntity player, Camera camera) {
        if (facing == null || facing == Facing.FIXED) {
            return new Quaternionf();  // Identity
        }
        
        Quaternionf rotation = switch (facing) {
            case FIXED -> new Quaternionf();
            
            case PLAYER_LOOK -> {
                if (player == null) yield new Quaternionf();
                // Face same direction as player
                float yaw = (float) Math.toRadians(-player.getYaw());
                float pitch = (float) Math.toRadians(-player.getPitch());
                yield new Quaternionf().rotateY(yaw).rotateX(pitch);
            }
            
            case VELOCITY -> {
                if (player == null) yield new Quaternionf();
                Vec3d velocity = player.getVelocity();
                if (velocity.lengthSquared() < 0.001) yield new Quaternionf();
                // Face velocity direction
                yield lookAt(velocity.normalize());
            }
            
            case CAMERA -> {
                if (camera == null) yield new Quaternionf();
                // Face toward camera
                Vec3d cameraPos = camera.getPos();
                Vec3d toCamera = cameraPos.subtract(primitivePos).normalize();
                yield lookAt(toCamera.multiply(-1));  // Look away from camera = face camera
            }
        };
        
        Logging.FIELD.topic("facing").trace("Resolved facing {}", facing);
        return rotation;
    }
    
    /**
     * Creates a rotation that looks in the given direction.
     * 
     * @param direction Normalized direction vector
     * @return Rotation quaternion
     */
    private static Quaternionf lookAt(Vec3d direction) {
        // Calculate yaw and pitch from direction
        double yaw = Math.atan2(-direction.x, direction.z);
        double pitch = Math.asin(-direction.y);
        
        return new Quaternionf()
            .rotateY((float) yaw)
            .rotateX((float) pitch);
    }
    
    /**
     * Resolves facing without context (returns identity).
     * 
     * @param facing The facing mode
     * @return Rotation quaternion (identity for non-dynamic modes)
     */
    public static Quaternionf resolve(Facing facing) {
        return resolve(facing, Vec3d.ZERO, null, null);
    }
}
