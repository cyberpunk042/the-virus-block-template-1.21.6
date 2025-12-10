package net.cyberpunk042.visual.shape;

import com.google.gson.JsonObject;

import net.cyberpunk042.visual.pattern.CellType;
import org.joml.Vector3f;

import java.util.Map;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;

/**
 * Ring/torus shape - a band with inner and outer radius.
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "shape": {
 *   "type": "ring",
 *   "innerRadius": 0.8,
 *   "outerRadius": 1.0,
 *   "segments": 64,
 *   "y": 0.0,
 *   "arcStart": 0,
 *   "arcEnd": 360,
 *   "height": 0.0,
 *   "twist": 0.0
 * }
 * </pre>
 * 
 * <h2>Parts</h2>
 * <ul>
 *   <li><b>surface</b> (SEGMENT) - Main ring surface</li>
 *   <li><b>innerEdge</b> (EDGE) - Inner border</li>
 *   <li><b>outerEdge</b> (EDGE) - Outer border</li>
 * </ul>
 * 
 * @see Shape
 * @see CellType
 */
public record RingShape(
    @Range(ValueRange.RADIUS) float innerRadius,
    @Range(ValueRange.RADIUS) float outerRadius,
    @Range(ValueRange.STEPS) int segments,
    @Range(ValueRange.UNBOUNDED) float y,
    @Range(ValueRange.DEGREES) float arcStart,
    @Range(ValueRange.DEGREES) float arcEnd,
    @Range(ValueRange.POSITIVE) float height,
    @Range(ValueRange.DEGREES_FULL) float twist
) implements Shape {
    
    /** Default ring (0.8 inner, 1.0 outer). */
    public static final RingShape DEFAULT = new RingShape(
        0.8f, 1.0f, 64, 0, 0, 360, 0, 0);
    
    /** Thin ring. */
    public static final RingShape THIN = new RingShape(
        0.95f, 1.0f, 64, 0, 0, 360, 0, 0);
    
    /** Thick band. */
    public static final RingShape THICK = new RingShape(
        0.5f, 1.0f, 64, 0, 0, 360, 0, 0);
    
    /**
     * Creates a ring at the specified Y height.
     * @param innerRadius Inner radius
     * @param outerRadius Outer radius
     * @param y Y offset
     */
    public static RingShape at(@Range(ValueRange.RADIUS) float innerRadius, @Range(ValueRange.RADIUS) float outerRadius, @Range(ValueRange.UNBOUNDED) float y) {
        return new RingShape(innerRadius, outerRadius, 64, y, 0, 360, 0, 0);
    }
    
    @Override
    public String getType() {
        return "ring";
    }
    
    @Override
    public Vector3f getBounds() {
        float d = outerRadius * 2;
        return new Vector3f(d, Math.max(0.1f, height), d);
    }
    
    @Override
    public CellType primaryCellType() {
        return CellType.SEGMENT;
    }
    
    @Override
    public Map<String, CellType> getParts() {
        return Map.of(
            "surface", CellType.SEGMENT,
            "innerEdge", CellType.EDGE,
            "outerEdge", CellType.EDGE
        );
    }
    
    @Override
    public float getRadius() {
        return outerRadius;
    }
    
    /** Ring thickness (outer - inner). */
    public float thickness() {
        return outerRadius - innerRadius;
    }
    
    /** Whether this is a full ring or arc. */
    public boolean isFullRing() {
        return arcStart == 0 && arcEnd == 360;
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "ring");
        json.addProperty("innerRadius", innerRadius);
        json.addProperty("outerRadius", outerRadius);
        json.addProperty("segments", segments);
        if (y != 0) json.addProperty("y", y);
        if (arcStart != 0) json.addProperty("arcStart", arcStart);
        if (arcEnd != 360) json.addProperty("arcEnd", arcEnd);
        if (height != 0) json.addProperty("height", height);
        if (twist != 0) json.addProperty("twist", twist);
        return json;
    }

    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private @Range(ValueRange.RADIUS) float innerRadius = 0.8f;
        private @Range(ValueRange.RADIUS) float outerRadius = 1.0f;
        private @Range(ValueRange.STEPS) int segments = 64;
        private @Range(ValueRange.UNBOUNDED) float y = 0;
        private @Range(ValueRange.DEGREES) float arcStart = 0;
        private @Range(ValueRange.DEGREES) float arcEnd = 360;
        private @Range(ValueRange.POSITIVE) float height = 0;
        private @Range(ValueRange.DEGREES_FULL) float twist = 0;
        
        public Builder innerRadius(float r) { this.innerRadius = r; return this; }
        public Builder outerRadius(float r) { this.outerRadius = r; return this; }
        public Builder segments(int s) { this.segments = s; return this; }
        public Builder y(@Range(ValueRange.UNBOUNDED) float y) { this.y = y; return this; }
        public Builder arcStart(float a) { this.arcStart = a; return this; }
        public Builder arcEnd(float a) { this.arcEnd = a; return this; }
        public Builder height(float h) { this.height = h; return this; }
        public Builder twist(float t) { this.twist = t; return this; }
        
        public RingShape build() {
            return new RingShape(innerRadius, outerRadius, segments, y, arcStart, arcEnd, height, twist);
        }
    }
}
