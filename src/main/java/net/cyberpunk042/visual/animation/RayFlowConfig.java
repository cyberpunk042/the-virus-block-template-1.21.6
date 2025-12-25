package net.cyberpunk042.visual.animation;

import com.google.gson.JsonObject;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;

/**
 * Configuration for ray FLOW animation (alpha/visibility effects).
 * 
 * <p>Flow animation controls how visibility is distributed and animated
 * along the ray's parametric length (t=0 to t=1). Unlike Motion which
 * transforms geometry, Flow only affects alpha/color.</p>
 * 
 * <h2>Three Composable Axes</h2>
 * <ul>
 *   <li><b>Length</b>: How much of the ray is visible (RADIATE, ABSORB, PULSE)</li>
 *   <li><b>Travel</b>: Where visibility window is positioned (CHASE, SCROLL)</li>
 *   <li><b>Flicker</b>: Random/rhythmic alpha overlay (SCINTILLATION, STROBE)</li>
 * </ul>
 * 
 * <p>All three axes can be combined simultaneously for complex effects.</p>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "rayFlow": {
 *   "length": "RADIATE",
 *   "lengthSpeed": 1.0,
 *   "travel": "CHASE",
 *   "travelSpeed": 2.0,
 *   "chaseCount": 3,
 *   "chaseWidth": 0.2,
 *   "flicker": "SCINTILLATION",
 *   "flickerIntensity": 0.3,
 *   "flickerFrequency": 5.0
 * }
 * </pre>
 * 
 * @see LengthMode
 * @see TravelMode
 * @see FlickerMode
 * @see Animation
 */
public record RayFlowConfig(
    // === Length Axis ===
    @JsonField(skipIfDefault = true) LengthMode length,
    @Range(ValueRange.UNBOUNDED) @JsonField(skipIfDefault = true) float lengthSpeed,
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true, defaultValue = "0.3") float segmentLength,
    
    // === Travel Axis ===
    @JsonField(skipIfDefault = true) TravelMode travel,
    @Range(ValueRange.UNBOUNDED) @JsonField(skipIfDefault = true) float travelSpeed,
    @Range(ValueRange.STEPS) @JsonField(skipIfDefault = true, defaultValue = "1") int chaseCount,
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true, defaultValue = "0.3") float chaseWidth,
    
    // === Flicker Axis ===
    @JsonField(skipIfDefault = true) FlickerMode flicker,
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true) float flickerIntensity,
    @Range(ValueRange.POSITIVE) @JsonField(skipIfDefault = true, defaultValue = "5.0") float flickerFrequency
) {
    // =========================================================================
    // Static Constants
    // =========================================================================
    
    /** No flow animation. */
    public static final RayFlowConfig NONE = new RayFlowConfig(
        LengthMode.NONE, 0f, 0.3f,
        TravelMode.NONE, 0f, 1, 0.3f,
        FlickerMode.NONE, 0f, 5f
    );
    
    /** Default radiate effect (rays grow outward). */
    public static final RayFlowConfig RADIATE = new RayFlowConfig(
        LengthMode.RADIATE, 1f, 0.3f,
        TravelMode.NONE, 0f, 1, 0.3f,
        FlickerMode.NONE, 0f, 5f
    );
    
    /** Default absorb effect (rays shrink inward). */
    public static final RayFlowConfig ABSORB = new RayFlowConfig(
        LengthMode.ABSORB, 1f, 0.3f,
        TravelMode.NONE, 0f, 1, 0.3f,
        FlickerMode.NONE, 0f, 5f
    );
    
    /** Chase particles flowing along rays. */
    public static final RayFlowConfig CHASE = new RayFlowConfig(
        LengthMode.NONE, 0f, 0.3f,
        TravelMode.CHASE, 1.5f, 3, 0.2f,
        FlickerMode.NONE, 0f, 5f
    );
    
    /** Scrolling energy flow. */
    public static final RayFlowConfig SCROLL = new RayFlowConfig(
        LengthMode.NONE, 0f, 0.3f,
        TravelMode.SCROLL, 1f, 1, 0.3f,
        FlickerMode.NONE, 0f, 5f
    );
    
    /** Twinkling stars effect. */
    public static final RayFlowConfig SCINTILLATE = new RayFlowConfig(
        LengthMode.NONE, 0f, 0.3f,
        TravelMode.NONE, 0f, 1, 0.3f,
        FlickerMode.SCINTILLATION, 0.5f, 8f
    );
    
    // =========================================================================
    // Queries
    // =========================================================================
    
    /** Whether any flow animation is active. */
    public boolean isActive() {
        return length != LengthMode.NONE || 
               travel != TravelMode.NONE || 
               flicker != FlickerMode.NONE;
    }
    
    /** Whether length animation is active. */
    public boolean hasLength() {
        return length != null && length != LengthMode.NONE;
    }
    
    /** Whether travel animation is active. */
    public boolean hasTravel() {
        return travel != null && travel != TravelMode.NONE;
    }
    
    /** Whether flicker animation is active. */
    public boolean hasFlicker() {
        return flicker != null && flicker != FlickerMode.NONE;
    }
    
    // =========================================================================
    // JSON Parsing
    // =========================================================================
    
    /**
     * Parses a RayFlowConfig from JSON.
     */
    public static RayFlowConfig fromJson(JsonObject json) {
        if (json == null) return NONE;
        
        LengthMode length = LengthMode.NONE;
        if (json.has("length")) {
            length = LengthMode.fromString(json.get("length").getAsString());
        }
        float lengthSpeed = json.has("lengthSpeed") ? json.get("lengthSpeed").getAsFloat() : 1f;
        float segmentLength = json.has("segmentLength") ? json.get("segmentLength").getAsFloat() : 0.3f;
        
        TravelMode travel = TravelMode.NONE;
        if (json.has("travel")) {
            travel = TravelMode.fromString(json.get("travel").getAsString());
        }
        float travelSpeed = json.has("travelSpeed") ? json.get("travelSpeed").getAsFloat() : 1f;
        int chaseCount = json.has("chaseCount") ? json.get("chaseCount").getAsInt() : 1;
        float chaseWidth = json.has("chaseWidth") ? json.get("chaseWidth").getAsFloat() : 0.3f;
        
        FlickerMode flicker = FlickerMode.NONE;
        if (json.has("flicker")) {
            flicker = FlickerMode.fromString(json.get("flicker").getAsString());
        }
        float flickerIntensity = json.has("flickerIntensity") ? json.get("flickerIntensity").getAsFloat() : 0.3f;
        float flickerFrequency = json.has("flickerFrequency") ? json.get("flickerFrequency").getAsFloat() : 5f;
        
        return new RayFlowConfig(
            length, lengthSpeed, segmentLength,
            travel, travelSpeed, chaseCount, chaseWidth,
            flicker, flickerIntensity, flickerFrequency
        );
    }
    
    /**
     * Serializes this config to JSON.
     */
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    
    public Builder toBuilder() {
        return new Builder()
            .length(length).lengthSpeed(lengthSpeed).segmentLength(segmentLength)
            .travel(travel).travelSpeed(travelSpeed).chaseCount(chaseCount).chaseWidth(chaseWidth)
            .flicker(flicker).flickerIntensity(flickerIntensity).flickerFrequency(flickerFrequency);
    }
    
    public static class Builder {
        private LengthMode length = LengthMode.NONE;
        private float lengthSpeed = 1f;
        private float segmentLength = 0.3f;
        private TravelMode travel = TravelMode.NONE;
        private float travelSpeed = 1f;
        private int chaseCount = 1;
        private float chaseWidth = 0.3f;
        private FlickerMode flicker = FlickerMode.NONE;
        private float flickerIntensity = 0.3f;
        private float flickerFrequency = 5f;
        
        public Builder length(LengthMode m) { this.length = m; return this; }
        public Builder lengthSpeed(float s) { this.lengthSpeed = s; return this; }
        public Builder segmentLength(float s) { this.segmentLength = s; return this; }
        public Builder travel(TravelMode m) { this.travel = m; return this; }
        public Builder travelSpeed(float s) { this.travelSpeed = s; return this; }
        public Builder chaseCount(int c) { this.chaseCount = c; return this; }
        public Builder chaseWidth(float w) { this.chaseWidth = w; return this; }
        public Builder flicker(FlickerMode m) { this.flicker = m; return this; }
        public Builder flickerIntensity(float i) { this.flickerIntensity = i; return this; }
        public Builder flickerFrequency(float f) { this.flickerFrequency = f; return this; }
        
        public RayFlowConfig build() {
            return new RayFlowConfig(
                length, lengthSpeed, segmentLength,
                travel, travelSpeed, chaseCount, chaseWidth,
                flicker, flickerIntensity, flickerFrequency
            );
        }
    }
}
