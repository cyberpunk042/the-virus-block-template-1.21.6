package net.cyberpunk042.visual.transform;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;
import net.cyberpunk042.log.Logging;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

/**
 * Complete spatial transform configuration for a primitive.
 * 
 * <h2>Position</h2>
 * <ul>
 *   <li><b>anchor</b>: Base position (CENTER, FEET, HEAD, etc.)</li>
 *   <li><b>offset</b>: Additional offset from anchor</li>
 * </ul>
 * 
 * <h2>Rotation</h2>
 * <ul>
 *   <li><b>rotation</b>: Static rotation in degrees (pitch, yaw, roll)</li>
 *   <li><b>inheritRotation</b>: Whether to inherit layer rotation</li>
 * </ul>
 * 
 * <h2>Scale</h2>
 * <ul>
 *   <li><b>scale</b>: Uniform scale multiplier</li>
 *   <li><b>scaleXYZ</b>: Per-axis scale (optional)</li>
 *   <li><b>scaleWithRadius</b>: Scale with field baseRadius</li>
 * </ul>
 * 
 * <h2>Orientation</h2>
 * <ul>
 *   <li><b>facing</b>: Dynamic facing mode (FIXED, PLAYER_LOOK, etc.)</li>
 *   <li><b>up</b>: Up vector definition</li>
 *   <li><b>billboard</b>: Billboard mode for camera-facing</li>
 * </ul>
 * 
 * <h2>Dynamic Positioning</h2>
 * <ul>
 *   <li><b>orbit</b>: Orbital motion around anchor</li>
 * </ul>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "transform": {
 *   "anchor": "CENTER",
 *   "offset": [0, 0.5, 0],
 *   "rotation": [0, 45, 0],
 *   "scale": 1.5,
 *   "facing": "PLAYER_LOOK",
 *   "billboard": "Y_AXIS",
 *   "orbit": { "enabled": true, "radius": 2.0, "speed": 0.02 }
 * }
 * </pre>
 * 
 * @see Anchor
 * @see Facing
 * @see Billboard
 * @see UpVector
 * @see OrbitConfig
 */
public record Transform(
    // Position
    Anchor anchor,
    @Nullable Vector3f offset,
    
    // Rotation
    @Nullable Vector3f rotation,
    boolean inheritRotation,
    
    // Scale
    @Range(ValueRange.SCALE) float scale,
    @Nullable Vector3f scaleXYZ,
    boolean scaleWithRadius,
    
    // Orientation
    Facing facing,
    UpVector up,
    Billboard billboard,
    
    // Dynamic
    @Nullable OrbitConfig orbit
) {
    // =========================================================================
    // Static Constants
    // =========================================================================
    
    /** Identity transform (CENTER anchor, no offset, no rotation, scale=1). */
    public static final Transform IDENTITY = new Transform(
        Anchor.CENTER, null, null, true, 1.0f, null, false,
        Facing.FIXED, UpVector.WORLD_UP, Billboard.NONE, null);
    
    /** Feet-anchored transform. */
    public static final Transform AT_FEET = new Transform(
        Anchor.FEET, null, null, true, 1.0f, null, false,
        Facing.FIXED, UpVector.WORLD_UP, Billboard.NONE, null);
    
    /** Head-anchored transform. */
    public static final Transform AT_HEAD = new Transform(
        Anchor.HEAD, null, null, true, 1.0f, null, false,
        Facing.FIXED, UpVector.WORLD_UP, Billboard.NONE, null);
    
    /** Billboard transform (always faces camera). */
    public static final Transform BILLBOARD = new Transform(
        Anchor.CENTER, null, null, true, 1.0f, null, false,
        Facing.CAMERA, UpVector.WORLD_UP, Billboard.FULL, null);
    
    // =========================================================================
    // Factory Methods
    // =========================================================================
    
    /**
     * Creates an identity transform.
     */
    public static Transform identity() {
        return IDENTITY;
    }
    
    /**
     * Creates a transform at the specified anchor.
     * @param anchor Position anchor
     */
    public static Transform at(Anchor anchor) {
        return new Transform(anchor, null, null, true, 1.0f, null, false,
            Facing.FIXED, UpVector.WORLD_UP, Billboard.NONE, null);
    }
    
    /**
     * Creates a transform with offset from CENTER.
     * @param x X offset
     * @param y Y offset
     * @param z Z offset
     */
    public static Transform offset(float x, float y, float z) {
        return new Transform(Anchor.CENTER, new Vector3f(x, y, z), null, true, 1.0f, null, false,
            Facing.FIXED, UpVector.WORLD_UP, Billboard.NONE, null);
    }
    
    /**
     * Creates a scaled transform.
     * @param scale Uniform scale
     */
    public static Transform scaled(@Range(ValueRange.SCALE) float scale) {
        return new Transform(Anchor.CENTER, null, null, true, scale, null, false,
            Facing.FIXED, UpVector.WORLD_UP, Billboard.NONE, null);
    }
    
    /**
     * Creates a rotated transform.
     * @param pitch X rotation in degrees
     * @param yaw Y rotation in degrees
     * @param roll Z rotation in degrees
     */
    public static Transform rotated(float pitch, float yaw, float roll) {
        return new Transform(Anchor.CENTER, null, new Vector3f(pitch, yaw, roll), true, 1.0f, null, false,
            Facing.FIXED, UpVector.WORLD_UP, Billboard.NONE, null);
    }
    
    // =========================================================================
    // Convenience Methods
    // =========================================================================
    
    /** Gets the effective offset (anchor offset + additional offset). */
    public Vector3f getEffectiveOffset() {
        Vector3f result = anchor.getOffset();
        if (offset != null) {
            result.add(offset);
        }
        return result;
    }
    
    /** Gets the effective rotation (or zero if null). */
    public Vector3f getEffectiveRotation() {
        return rotation != null ? new Vector3f(rotation) : new Vector3f();
    }
    
    /** Gets the effective scale vector. */
    public Vector3f getEffectiveScaleXYZ() {
        if (scaleXYZ != null) {
            return new Vector3f(scaleXYZ);
        }
        return new Vector3f(scale, scale, scale);
    }
    
    /** Whether this transform has orbit enabled. */
    public boolean hasOrbit() {
        return orbit != null && orbit.isActive();
    }
    
    /** Whether this transform uses billboard. */
    public boolean hasBillboard() {
        return billboard != Billboard.NONE;
    }
    
    /** Whether this transform has dynamic facing. */
    public boolean hasDynamicFacing() {
        return facing != Facing.FIXED;
    }
    
    // =========================================================================
    // Immutable Modifiers (return new instances)
    // =========================================================================
    
    /**
     * Returns a new Transform with the specified offset.
     */
    public Transform withOffset(Vector3f newOffset) {
        return new Transform(anchor, newOffset, rotation, inheritRotation, scale,
            scaleXYZ, scaleWithRadius, facing, up, billboard, orbit);
    }
    
    /**
     * Returns a new Transform with the specified uniform scale.
     */
    public Transform withScale(float newScale) {
        return new Transform(anchor, offset, rotation, inheritRotation, newScale,
            scaleXYZ, scaleWithRadius, facing, up, billboard, orbit);
    }
    
    /**
     * Returns a new Transform with the specified rotation.
     */
    public Transform withRotation(Vector3f newRotation) {
        return new Transform(anchor, offset, newRotation, inheritRotation, scale,
            scaleXYZ, scaleWithRadius, facing, up, billboard, orbit);
    }
    
    /**
     * Returns a new Transform with the specified anchor.
     */
    public Transform withAnchor(Anchor newAnchor) {
        return new Transform(newAnchor, offset, rotation, inheritRotation, scale,
            scaleXYZ, scaleWithRadius, facing, up, billboard, orbit);
    }
    
    /**
     * Gets the offset, never null (returns zero vector if null).
     */
    public Vector3f offset() {
        return offset != null ? offset : new Vector3f();
    }

    // =========================================================================
    // JSON Parsing
    // =========================================================================
    
    /**
     * Parses a Transform from JSON.
     * @param json The JSON object
     * @return Parsed transform
     */
    public static Transform fromJson(JsonObject json) {
        if (json == null) return IDENTITY;
        
        Logging.FIELD.topic("parse").trace("Parsing Transform...");
        
        Anchor anchor = Anchor.CENTER;
        if (json.has("anchor")) {
            anchor = Anchor.fromId(json.get("anchor").getAsString());
        }
        
        Vector3f offset = null;
        if (json.has("offset")) {
            offset = parseVec3(json.get("offset").getAsJsonArray());
        }
        
        Vector3f rotation = null;
        if (json.has("rotation")) {
            rotation = parseVec3(json.get("rotation").getAsJsonArray());
        }
        
        boolean inheritRotation = json.has("inheritRotation") ? 
            json.get("inheritRotation").getAsBoolean() : true;
        
        float scale = json.has("scale") ? json.get("scale").getAsFloat() : 1.0f;
        
        Vector3f scaleXYZ = null;
        if (json.has("scaleXYZ")) {
            scaleXYZ = parseVec3(json.get("scaleXYZ").getAsJsonArray());
        }
        
        boolean scaleWithRadius = json.has("scaleWithRadius") ? 
            json.get("scaleWithRadius").getAsBoolean() : false;
        
        Facing facing = Facing.FIXED;
        if (json.has("facing")) {
            facing = Facing.fromId(json.get("facing").getAsString());
        }
        
        UpVector up = UpVector.WORLD_UP;
        if (json.has("up")) {
            up = UpVector.fromId(json.get("up").getAsString());
        }
        
        Billboard billboard = Billboard.NONE;
        if (json.has("billboard")) {
            billboard = Billboard.fromId(json.get("billboard").getAsString());
        }
        
        OrbitConfig orbit = null;
        if (json.has("orbit")) {
            orbit = OrbitConfig.fromJson(json.getAsJsonObject("orbit"));
        }
        
        Transform result = new Transform(anchor, offset, rotation, inheritRotation, scale, 
            scaleXYZ, scaleWithRadius, facing, up, billboard, orbit);
        Logging.FIELD.topic("parse").trace("Parsed Transform: anchor={}, scale={}, facing={}, billboard={}", 
            anchor, scale, facing, billboard);
        return result;
    }
    
    private static Vector3f parseVec3(JsonArray arr) {
        if (arr == null || arr.size() < 3) return null;
        return new Vector3f(arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat());
    }
    
    /**
     * Serializes this transform to JSON.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        
        if (anchor != Anchor.CENTER) {
            json.addProperty("anchor", anchor.name());
        }
        
        if (offset != null) {
            JsonArray offsetArr = new JsonArray();
            offsetArr.add(offset.x);
            offsetArr.add(offset.y);
            offsetArr.add(offset.z);
            json.add("offset", offsetArr);
        }
        
        if (rotation != null) {
            JsonArray rotArr = new JsonArray();
            rotArr.add(rotation.x);
            rotArr.add(rotation.y);
            rotArr.add(rotation.z);
            json.add("rotation", rotArr);
        }
        
        if (!inheritRotation) {
            json.addProperty("inheritRotation", false);
        }
        
        if (scale != 1.0f) {
            json.addProperty("scale", scale);
        }
        
        if (scaleXYZ != null) {
            JsonArray scaleArr = new JsonArray();
            scaleArr.add(scaleXYZ.x);
            scaleArr.add(scaleXYZ.y);
            scaleArr.add(scaleXYZ.z);
            json.add("scaleXYZ", scaleArr);
        }
        
        if (scaleWithRadius) {
            json.addProperty("scaleWithRadius", true);
        }
        
        if (facing != Facing.FIXED) {
            json.addProperty("facing", facing.name());
        }
        
        if (up != UpVector.WORLD_UP) {
            json.addProperty("up", up.name());
        }
        
        if (billboard != Billboard.NONE) {
            json.addProperty("billboard", billboard.name());
        }
        
        if (orbit != null) {
            json.add("orbit", orbit.toJson());
        }
        
        return json;
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private Anchor anchor = Anchor.CENTER;
        private Vector3f offset = null;
        private Vector3f rotation = null;
        private boolean inheritRotation = true;
        private @Range(ValueRange.SCALE) float scale = 1.0f;
        private Vector3f scaleXYZ = null;
        private boolean scaleWithRadius = false;
        private Facing facing = Facing.FIXED;
        private UpVector up = UpVector.WORLD_UP;
        private Billboard billboard = Billboard.NONE;
        private OrbitConfig orbit = null;
        
        public Builder anchor(Anchor a) { this.anchor = a; return this; }
        public Builder offset(Vector3f o) { this.offset = o; return this; }
        public Builder offset(float x, float y, float z) { this.offset = new Vector3f(x, y, z); return this; }
        public Builder rotation(Vector3f r) { this.rotation = r; return this; }
        public Builder rotation(float pitch, float yaw, float roll) { 
            this.rotation = new Vector3f(pitch, yaw, roll); return this; 
        }
        public Builder inheritRotation(boolean i) { this.inheritRotation = i; return this; }
        public Builder scale(float s) { this.scale = s; return this; }
        public Builder scaleXYZ(Vector3f s) { this.scaleXYZ = s; return this; }
        public Builder scaleXYZ(float x, float y, float z) { this.scaleXYZ = new Vector3f(x, y, z); return this; }
        public Builder scaleWithRadius(boolean s) { this.scaleWithRadius = s; return this; }
        public Builder facing(Facing f) { this.facing = f; return this; }
        public Builder up(UpVector u) { this.up = u; return this; }
        public Builder billboard(Billboard b) { this.billboard = b; return this; }
        public Builder orbit(OrbitConfig o) { this.orbit = o; return this; }
        
        public Transform build() {
            return new Transform(anchor, offset, rotation, inheritRotation, scale, scaleXYZ,
                scaleWithRadius, facing, up, billboard, orbit);
        }
    }
}
