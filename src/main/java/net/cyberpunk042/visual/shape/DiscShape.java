package net.cyberpunk042.visual.shape;

import com.google.gson.JsonObject;

import net.cyberpunk042.visual.pattern.CellType;
import org.joml.Vector3f;

import java.util.Map;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;


/**
 * Flat disc/circle shape.
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "shape": {
 *   "type": "disc",
 *   "radius": 1.0,
 *   "segments": 64,
 *   "y": 0.0,
 *   "arcStart": 0,
 *   "arcEnd": 360,
 *   "innerRadius": 0.0,
 *   "rings": 1
 * }
 * </pre>
 * 
 * <h2>Parts</h2>
 * <ul>
 *   <li><b>surface</b> (SECTOR) - Main disc surface</li>
 *   <li><b>edge</b> (EDGE) - Outer border</li>
 * </ul>
 * 
 * @see Shape
 * @see CellType
 */
public record DiscShape(
    @Range(ValueRange.RADIUS) float radius,
    @Range(ValueRange.STEPS) int segments,
    @Range(ValueRange.UNBOUNDED) @JsonField(skipIfDefault = true) float y,
    @Range(ValueRange.DEGREES) @JsonField(skipIfDefault = true) float arcStart,
    @Range(ValueRange.DEGREES) @JsonField(skipIfDefault = true, defaultValue = "360") float arcEnd,
    @Range(ValueRange.POSITIVE) @JsonField(skipIfDefault = true) float innerRadius,
    @Range(ValueRange.STEPS) @JsonField(skipIfDefault = true, defaultValue = "1") int rings
)implements Shape {
    
    /** Default full disc. */
    public static DiscShape of(float y, float radius, int segments) { 
        return new DiscShape(y, segments, 0f, radius, 0f, 360f, 1); 
    }
    public static DiscShape at(float y, float radius) { 
        return new DiscShape(y, 32, 0f, radius, 0f, 360f, 1); 
    }
    public static DiscShape defaults() { return DEFAULT; }
    
    public static final DiscShape DEFAULT = new DiscShape(
        1.0f, 64, 0, 0, 360, 0, 1);
    
    /** High-detail disc. */
    public static final DiscShape HIGH_DETAIL = new DiscShape(
        1.0f, 128, 0, 0, 360, 0, 4);
    
    /**
     * Creates a simple disc.
     * @param radius Disc radius
     */
    public static DiscShape ofRadius(@Range(ValueRange.RADIUS) float radius) {
        return new DiscShape(radius, 64, 0, 0, 360, 0, 1);
    }
    
    /**
     * Creates a pie slice.
     * @param radius Disc radius
     * @param arcEnd Arc angle (0-360)
     */
    public static DiscShape pie(@Range(ValueRange.RADIUS) float radius, @Range(ValueRange.DEGREES) float arcEnd) {
        return new DiscShape(radius, 64, 0, 0, arcEnd, 0, 1);
    }
    
    @Override
    public String getType() {
        return "disc";
    }
    
    @Override
    public Vector3f getBounds() {
        float d = radius * 2;
        return new Vector3f(d, 0.01f, d);
    }
    
    @Override
    public CellType primaryCellType() {
        return CellType.SECTOR;
    }
    
    @Override
    public Map<String, CellType> getParts() {
        return Map.of(
            "surface", CellType.SECTOR,
            "edge", CellType.EDGE
        );
    }
    
    /** Whether this is a full disc or partial arc. */
    public boolean isFullDisc() {
        return arcStart == 0 && arcEnd == 360 && innerRadius == 0;
    }
    
    /** Whether this has a hole (donut shape). */
    public boolean hasHole() {
        return innerRadius > 0;
    }
    
    @Override
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }

    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    /** Create a builder pre-populated with this shape's values. */
    public Builder toBuilder() {
        return new Builder()
            .radius(radius)
            .segments(segments)
            .y(y)
            .arcStart(arcStart)
            .arcEnd(arcEnd)
            .innerRadius(innerRadius)
            .rings(rings);
    }
    
    public static class Builder {
        private @Range(ValueRange.RADIUS) float radius = 1.0f;
        private @Range(ValueRange.STEPS) int segments = 64;
        private @Range(ValueRange.UNBOUNDED) float y = 0;
        private @Range(ValueRange.DEGREES) float arcStart = 0;
        private @Range(ValueRange.DEGREES) float arcEnd = 360;
        private @Range(ValueRange.POSITIVE) float innerRadius = 0;
        private @Range(ValueRange.STEPS) int rings = 1;
        
        public Builder radius(float r) { this.radius = r; return this; }
        public Builder segments(int s) { this.segments = s; return this; }
        public Builder y(@Range(ValueRange.UNBOUNDED) float y) { this.y = y; return this; }
        public Builder arcStart(float a) { this.arcStart = a; return this; }
        public Builder arcEnd(float a) { this.arcEnd = a; return this; }
        public Builder innerRadius(float r) { this.innerRadius = r; return this; }
        public Builder rings(int r) { this.rings = r; return this; }
        
        public DiscShape build() {
            return new DiscShape(radius, segments, y, arcStart, arcEnd, innerRadius, rings);
        }
    }
}
