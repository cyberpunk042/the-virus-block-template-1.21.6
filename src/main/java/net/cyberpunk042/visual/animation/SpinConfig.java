package net.cyberpunk042.visual.animation;

import org.jetbrains.annotations.Nullable;

import org.joml.Vector3f;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Configuration for continuous rotation animation.
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "spin": {
 *   "axis": "Y",
 *   "speed": 0.02,
 *   "oscillate": false,
 *   "range": 360
 * }
 * </pre>
 * 
 * <p>Shorthand: {@code "spin": 0.02} creates Y-axis spin at that speed.</p>
 * 
 * @see Axis
 */
public record SpinConfig(
    Axis axis,
    @Range(ValueRange.UNBOUNDED) float speed,
    @JsonField(skipIfDefault = true) boolean oscillate,
    @JsonField(skipIfDefault = true, defaultValue = "360") @Range(ValueRange.DEGREES_FULL) float range,
    @JsonField(skipIfNull = true) @Nullable Vector3f customAxis
) {
    /** No spin (static). */
    public static final SpinConfig NONE = new SpinConfig(Axis.Y, 0, false, 360, null);
    
    /** Default spin (slow Y-axis rotation). */
    public static final SpinConfig DEFAULT = new SpinConfig(Axis.Y, 0.02f, false, 360, null);
    
    /**
     * Creates a simple spin around an axis.
     * @param axis Rotation axis
     * @param speed Rotation speed (radians per tick)
     */
    public static SpinConfig around(Axis axis, @Range(ValueRange.UNBOUNDED) float speed) {
        return new SpinConfig(axis, speed, false, 360, null);
    }
    
    /**
     * Creates an oscillating spin (back and forth).
     * @param axis Rotation axis
     * @param speed Oscillation speed
     * @param range Maximum rotation angle
     */
    public static SpinConfig oscillating(Axis axis, @Range(ValueRange.UNBOUNDED) float speed, @Range(ValueRange.DEGREES_FULL) float range) {
        return new SpinConfig(axis, speed, true, range, null);
    }
    
    /** Whether this spin is active. */
    public boolean isActive() {
        return speed != 0;
    }
    
    /** Gets the rotation axis as a vector. */
    public Vector3f getAxisVector() {
        if (axis == Axis.CUSTOM && customAxis != null) {
            return new Vector3f(customAxis);
        }
        return axis.toVector();
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    /** Create a builder pre-populated with this record's values. */
    public Builder toBuilder() {
        return new Builder()
            .axis(axis)
            .speed(speed)
            .oscillate(oscillate)
            .range(range)
            .customAxis(customAxis);
    }
    
    public static class Builder {
        private Axis axis = Axis.Y;
        private @Range(ValueRange.UNBOUNDED) float speed = 0.02f;
        private boolean oscillate = false;
        private @Range(ValueRange.DEGREES_FULL) float range = 360;
        private @Nullable Vector3f customAxis = null;
        
        public Builder axis(Axis a) { this.axis = a; return this; }
        public Builder speed(float s) { this.speed = s; return this; }
        public Builder oscillate(boolean o) { this.oscillate = o; return this; }
        public Builder range(float r) { this.range = r; return this; }
        public Builder customAxis(Vector3f v) { this.customAxis = v; return this; }
        
        public SpinConfig build() {
            return new SpinConfig(axis, speed, oscillate, range, customAxis);
        }
    }

    // =========================================================================
    // JSON Parsing
    // =========================================================================
    
    /**
     * Parses a SpinConfig from JSON.
     * @param json The JSON object
     * @return Parsed config
     */
    public static SpinConfig fromJson(JsonObject json) {
        if (json == null) return NONE;
        
        Logging.FIELD.topic("parse").trace("Parsing SpinConfig...");
        
        Axis axis = Axis.Y;
        if (json.has("axis")) {
            axis = Axis.fromId(json.get("axis").getAsString());
        }
        
        float speed = json.has("speed") ? json.get("speed").getAsFloat() : 0.02f;
        boolean oscillate = json.has("oscillate") ? json.get("oscillate").getAsBoolean() : false;
        float range = json.has("range") ? json.get("range").getAsFloat() : 360.0f;
        
        SpinConfig result = new SpinConfig(axis, speed, oscillate, range, null);
        Logging.FIELD.topic("parse").trace("Parsed SpinConfig: axis={}, speed={}, oscillate={}", axis, speed, oscillate);
        return result;
    }
    
    /**
     * Serializes this spin config to JSON.
     * Uses reflection-based serialization with @JsonField annotations.
     */
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }

}
