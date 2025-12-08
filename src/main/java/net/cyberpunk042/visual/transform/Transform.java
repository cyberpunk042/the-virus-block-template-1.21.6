package net.cyberpunk042.visual.transform;

import com.google.gson.JsonObject;
import net.minecraft.util.math.Vec3d;

/**
 * Spatial transform for a primitive.
 * 
 * <h2>Components</h2>
 * <ul>
 *   <li><b>offset</b>: Position offset from field center</li>
 *   <li><b>rotation</b>: Euler angles in degrees (pitch, yaw, roll)</li>
 *   <li><b>scale</b>: Uniform scale multiplier</li>
 * </ul>
 */
public record Transform(
        Vec3d offset,
        Vec3d rotation,
        float scale
) {
    
    /** Identity transform (no offset, no rotation, scale=1). */
    public static final Transform IDENTITY = new Transform(Vec3d.ZERO, Vec3d.ZERO, 1.0f);
    
    public static Transform identity() {
        return IDENTITY;
    }
    
    public static Transform offset(double x, double y, double z) {
        return new Transform(new Vec3d(x, y, z), Vec3d.ZERO, 1.0f);
    }
    
    public static Transform offset(Vec3d offset) {
        return new Transform(offset, Vec3d.ZERO, 1.0f);
    }
    
    public static Transform scaled(float scale) {
        return new Transform(Vec3d.ZERO, Vec3d.ZERO, scale);
    }
    
    public static Transform rotated(float pitch, float yaw, float roll) {
        return new Transform(Vec3d.ZERO, new Vec3d(pitch, yaw, roll), 1.0f);
    }
    
    // Builder-style
    public Transform withOffset(Vec3d newOffset) {
        return new Transform(newOffset, rotation, scale);
    }
    
    public Transform withRotation(Vec3d newRotation) {
        return new Transform(offset, newRotation, scale);
    }
    
    public Transform withScale(float newScale) {
        return new Transform(offset, rotation, newScale);
    }
    
    public Transform addOffset(Vec3d delta) {
        return new Transform(offset.add(delta), rotation, scale);
    }
    
    public Transform multiplyScale(float factor) {
        return new Transform(offset, rotation, scale * factor);
    }
    
    /**
     * Parse transform from JSON.
     * 
     * <p>Supports:
     * <ul>
     *   <li>"offset": [x, y, z] or {"x": 0, "y": 0, "z": 0}</li>
     *   <li>"rotationX", "rotationY", "rotationZ": degrees</li>
     *   <li>"scale": float</li>
     * </ul>
     */
    public static Transform fromJson(JsonObject json) {
        if (json == null) return IDENTITY;
        
        // Parse offset
        Vec3d offset = Vec3d.ZERO;
        if (json.has("offset")) {
            var off = json.get("offset");
            if (off.isJsonArray()) {
                var arr = off.getAsJsonArray();
                offset = new Vec3d(
                    arr.get(0).getAsDouble(),
                    arr.get(1).getAsDouble(),
                    arr.get(2).getAsDouble()
                );
            } else if (off.isJsonObject()) {
                var obj = off.getAsJsonObject();
                offset = new Vec3d(
                    obj.has("x") ? obj.get("x").getAsDouble() : 0,
                    obj.has("y") ? obj.get("y").getAsDouble() : 0,
                    obj.has("z") ? obj.get("z").getAsDouble() : 0
                );
            }
        }
        
        // Parse rotation (individual axes)
        double rotX = json.has("rotationX") ? json.get("rotationX").getAsDouble() : 0;
        double rotY = json.has("rotationY") ? json.get("rotationY").getAsDouble() : 0;
        double rotZ = json.has("rotationZ") ? json.get("rotationZ").getAsDouble() : 0;
        Vec3d rotation = new Vec3d(rotX, rotY, rotZ);
        
        // Parse scale
        float scale = json.has("scale") ? json.get("scale").getAsFloat() : 1.0f;
        
        return new Transform(offset, rotation, scale);
    }
}
