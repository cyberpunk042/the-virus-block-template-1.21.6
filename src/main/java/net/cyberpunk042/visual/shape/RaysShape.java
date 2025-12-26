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
 * Rays shape - a collection of straight line segments in 3D space.
 * 
 * <h2>Concept</h2>
 * <p>Rays are pure LINE geometry (1D rendered with thickness). Unlike jets which are
 * 3D volumes (cones/cylinders), rays are straight lines that can be arranged in
 * various patterns.</p>
 * 
 * <h2>Arrangement Patterns</h2>
 * <pre>
 * RADIAL (XZ plane):        SPHERICAL (3D):          PARALLEL:
 *          ↑                      ↑                   →  →  →
 *         /|\                   ↗ | ↖                 →  →  →
 *      ←─●─→                 ←──●──→                  →  →  →
 *         \|/                   ↙ | ↘
 *          ↓                      ↓
 * 
 * CONVERGING (inward):      DIVERGING (outward):
 *     ↘   ↓   ↙                ↗   ↑   ↖
 *         ●                        ●
 *     ↗   ↑   ↖                ↘   ↓   ↙
 * </pre>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "shape": {
 *   "type": "rays",
 *   "rayLength": 2.0,
 *   "rayWidth": 1.0,
 *   "count": 12,
 *   "arrangement": "RADIAL",
 *   "innerRadius": 0.5,
 *   "outerRadius": 3.0,
 *   "layers": 1,
 *   "layerSpacing": 0.5,
 *   "randomness": 0.0,
 *   "lengthVariation": 0.0,
 *   "fadeStart": 1.0,
 *   "fadeEnd": 1.0,
 *   "segments": 1,
 *   "segmentGap": 0.0
 * }
 * </pre>
 * 
 * @see RayArrangement
 * @see Shape
 */
public record RaysShape(
    // === Individual Ray Geometry ===
    @Range(ValueRange.POSITIVE_NONZERO) float rayLength,
    @Range(ValueRange.POSITIVE_NONZERO) float rayWidth,
    
    // === Distribution & Count ===
    @Range(ValueRange.STEPS) int count,
    RayArrangement arrangement,
    RayDistribution distribution, // UNIFORM, RANDOM, STOCHASTIC
    @Range(ValueRange.POSITIVE) float innerRadius,
    @Range(ValueRange.POSITIVE_NONZERO) float outerRadius,
    
    // === Multi-Layer Support ===
    @Range(ValueRange.STEPS) @JsonField(skipIfDefault = true, defaultValue = "1") int layers,
    @Range(ValueRange.POSITIVE) @JsonField(skipIfDefault = true) float layerSpacing,
    
    // === Randomness ===
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true) float randomness,
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true) float lengthVariation,
    
    // === Fading (per-ray alpha gradient) ===
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true, defaultValue = "1.0") float fadeStart,
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true, defaultValue = "1.0") float fadeEnd,
    
    // === Segmentation (for dashed/dotted effects) ===
    @Range(ValueRange.STEPS) @JsonField(skipIfDefault = true, defaultValue = "1") int segments,
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true) float segmentGap,
    
    // === Ray Line Shape (NEW: how individual rays are curved) ===
    @JsonField(skipIfDefault = true) RayLineShape lineShape,
    @Range(ValueRange.POSITIVE) @JsonField(skipIfDefault = true, defaultValue = "0.1") float lineShapeAmplitude,
    @Range(ValueRange.POSITIVE) @JsonField(skipIfDefault = true, defaultValue = "2.0") float lineShapeFrequency,
    @Range(ValueRange.STEPS) @JsonField(skipIfDefault = true, defaultValue = "16") int shapeSegments,
    
    // === Field Curvature (how rays curve around center) ===
    @JsonField(skipIfDefault = true) RayCurvature curvature,
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true) float curvatureIntensity,
    
    // === Ray Type (visual appearance: LINE, DROPLET, KAMEHAMEHA, etc.) ===
    @JsonField(skipIfDefault = true) RayType rayType,
    
    // === Ray Orientation (direction for 3D ray types) ===
    @JsonField(skipIfDefault = true) RayOrientation rayOrientation
) implements Shape {
    
    // =========================================================================
    // Presets
    // =========================================================================
    
    /** Default rays (radial sun-ray pattern). */
    public static final RaysShape DEFAULT = new RaysShape(
        2.0f,           // rayLength
        1.0f,           // rayWidth
        12,             // count
        RayArrangement.RADIAL, // arrangement
        RayDistribution.UNIFORM, // distribution
        0.5f,           // innerRadius
        3.0f,           // outerRadius
        1,              // layers
        0.5f,           // layerSpacing
        0.0f,           // randomness
        0.0f,           // lengthVariation
        1.0f,           // fadeStart
        1.0f,           // fadeEnd
        1,              // segments
        0.0f,           // segmentGap
        // Shape modifiers
        RayLineShape.STRAIGHT, // lineShape
        0.1f,           // lineShapeAmplitude
        2.0f,           // lineShapeFrequency
        16,             // shapeSegments
        RayCurvature.NONE, // curvature
        0.0f,           // curvatureIntensity
        RayType.LINE,   // rayType
        RayOrientation.ALONG_RAY  // rayOrientation
    );
    
    /** Spherical absorption rays (converging to center). */
    public static final RaysShape ABSORPTION = new RaysShape(
        2.0f, 1.0f, 48, RayArrangement.CONVERGING, RayDistribution.RANDOM, 0.5f, 3.5f,
        8, 0.3f, 0.1f, 0.1f, 1.0f, 0.2f, 1, 0.0f,
        RayLineShape.STRAIGHT, 0.1f, 2.0f, 16, RayCurvature.NONE, 0.0f, RayType.LINE, RayOrientation.ALONG_RAY);
    
    /** Spherical emission rays (diverging from center). */
    public static final RaysShape EMISSION = new RaysShape(
        2.0f, 1.0f, 48, RayArrangement.DIVERGING, RayDistribution.RANDOM, 0.5f, 3.5f,
        8, 0.3f, 0.1f, 0.1f, 0.2f, 1.0f, 1, 0.0f,
        RayLineShape.STRAIGHT, 0.1f, 2.0f, 16, RayCurvature.NONE, 0.0f, RayType.LINE, RayOrientation.ALONG_RAY);
    
    /** Parallel laser grid. */
    public static final RaysShape LASER_GRID = new RaysShape(
        5.0f, 1.5f, 16, RayArrangement.PARALLEL, RayDistribution.UNIFORM, 0.0f, 2.0f,
        4, 0.3f, 0.0f, 0.0f, 1.0f, 1.0f, 1, 0.0f,
        RayLineShape.STRAIGHT, 0.1f, 2.0f, 16, RayCurvature.NONE, 0.0f, RayType.LINE, RayOrientation.ALONG_RAY);
    
    /** Dashed pulse rays. */
    public static final RaysShape PULSE = new RaysShape(
        3.0f, 1.0f, 8, RayArrangement.RADIAL, RayDistribution.UNIFORM, 0.3f, 2.5f,
        1, 0.5f, 0.0f, 0.0f, 1.0f, 0.5f, 4, 0.2f,
        RayLineShape.STRAIGHT, 0.1f, 2.0f, 16, RayCurvature.NONE, 0.0f, RayType.LINE, RayOrientation.ALONG_RAY);
    
    /** Sparse random stars. */
    public static final RaysShape STARS = new RaysShape(
        1.5f, 0.5f, 24, RayArrangement.SPHERICAL, RayDistribution.STOCHASTIC, 0.8f, 2.0f,
        1, 0.5f, 0.3f, 0.3f, 0.8f, 0.3f, 1, 0.0f,
        RayLineShape.STRAIGHT, 0.1f, 2.0f, 16, RayCurvature.NONE, 0.0f, RayType.LINE, RayOrientation.ALONG_RAY);
    
    /** Vortex rays (spiraling into center). */
    public static final RaysShape VORTEX = new RaysShape(
        2.5f, 1.0f, 24, RayArrangement.RADIAL, RayDistribution.UNIFORM, 0.3f, 3.0f,
        1, 0.5f, 0.0f, 0.0f, 1.0f, 0.8f, 1, 0.0f,
        RayLineShape.STRAIGHT, 0.1f, 2.0f, 32, RayCurvature.VORTEX, 0.5f, RayType.LINE, RayOrientation.ALONG_RAY);
    
    /** Corkscrew rays (helical shape). */
    public static final RaysShape CORKSCREW = new RaysShape(
        2.0f, 1.0f, 12, RayArrangement.RADIAL, RayDistribution.UNIFORM, 0.5f, 2.5f,
        1, 0.5f, 0.0f, 0.0f, 1.0f, 1.0f, 1, 0.0f,
        RayLineShape.CORKSCREW, 0.15f, 3.0f, 32, RayCurvature.NONE, 0.0f, RayType.LINE, RayOrientation.ALONG_RAY);
    
    public static RaysShape defaults() { return DEFAULT; }
    
    // =========================================================================
    // Factory Methods
    // =========================================================================
    
    /**
     * Creates simple radial rays.
     */
    public static RaysShape radial(int count, float length) {
        return builder().count(count).rayLength(length).arrangement(RayArrangement.RADIAL).build();
    }
    
    /**
     * Creates spherical rays emanating from center.
     */
    public static RaysShape spherical(int count, float length, int layers) {
        return builder().count(count).rayLength(length).layers(layers)
            .arrangement(RayArrangement.SPHERICAL).build();
    }
    
    /**
     * Creates converging absorption rays.
     */
    public static RaysShape converging(int count, float innerRadius, float outerRadius) {
        return builder().count(count).innerRadius(innerRadius).outerRadius(outerRadius)
            .arrangement(RayArrangement.CONVERGING).build();
    }
    
    // =========================================================================
    // Shape Interface
    // =========================================================================
    
    @Override
    public String getType() {
        return "rays";
    }
    
    @Override
    public Vector3f getBounds() {
        float size = outerRadius * 2;
        return new Vector3f(size, layers > 1 ? layerSpacing * (layers - 1) : rayLength, size);
    }
    
    @Override
    public CellType primaryCellType() {
        return CellType.EDGE; // Rays are line segments
    }
    
    @Override
    public Map<String, CellType> getParts() {
        return Map.of(
            "rays", CellType.EDGE
        );
    }
    
    @Override
    public float getRadius() {
        return outerRadius;
    }
    
    // =========================================================================
    // Utility Methods
    // =========================================================================
    
    /** Total number of rays (count * layers). */
    public int totalRayCount() {
        return count * layers;
    }
    
    /** Whether rays fade along their length. */
    public boolean hasFade() {
        return fadeStart != fadeEnd || fadeStart < 1.0f || fadeEnd < 1.0f;
    }
    
    /** Whether rays are segmented (dashed). */
    public boolean isSegmented() {
        return segments > 1 && segmentGap > 0;
    }
    
    /** Ray length accounting for inner/outer radius. */
    public float effectiveRayLength() {
        if (arrangement == RayArrangement.CONVERGING || arrangement == RayArrangement.DIVERGING) {
            return outerRadius - innerRadius;
        }
        return rayLength;
    }
    
    /** Whether rays have a non-straight line shape. */
    public boolean hasLineShape() {
        return lineShape != null && lineShape != RayLineShape.STRAIGHT;
    }
    
    /** Returns the line shape, defaulting to STRAIGHT if null. */
    public RayLineShape effectiveLineShape() {
        return lineShape != null ? lineShape : RayLineShape.STRAIGHT;
    }
    
    /** Whether rays have field curvature (spiral/vortex). */
    public boolean hasCurvature() {
        return curvature != null && curvature != RayCurvature.NONE && curvatureIntensity > 0;
    }
    
    /** Returns the curvature mode, defaulting to NONE if null. */
    public RayCurvature effectiveCurvature() {
        return curvature != null ? curvature : RayCurvature.NONE;
    }
    
    /** Returns the ray type, defaulting to LINE if null. */
    public RayType effectiveRayType() {
        return rayType != null ? rayType : RayType.LINE;
    }
    
    /** Returns the ray orientation, defaulting to ALONG_RAY if null. */
    public RayOrientation effectiveRayOrientation() {
        return rayOrientation != null ? rayOrientation : RayOrientation.ALONG_RAY;
    }
    
    /** Number of segments needed to render this ray shape properly. */
    public int effectiveShapeSegments() {
        if (hasLineShape()) {
            // Complex line shape - use at least the suggested minimum
            return Math.max(shapeSegments, lineShape.suggestedMinSegments());
        } else if (hasCurvature()) {
            // Curvature needs multiple segments for smooth curves
            return Math.max(shapeSegments, 16);
        }
        // For straight rays, respect user's shapeSegments setting
        // (needed for travel animations like CHASE/SCROLL to work smoothly)
        return Math.max(1, shapeSegments);
    }
    
    @Override
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    
    public Builder toBuilder() {
        return new Builder()
            .rayLength(rayLength)
            .rayWidth(rayWidth)
            .count(count)
            .arrangement(arrangement)
            .distribution(distribution)
            .innerRadius(innerRadius)
            .outerRadius(outerRadius)
            .layers(layers)
            .layerSpacing(layerSpacing)
            .randomness(randomness)
            .lengthVariation(lengthVariation)
            .fadeStart(fadeStart)
            .fadeEnd(fadeEnd)
            .segments(segments)
            .segmentGap(segmentGap)
            // Shape modifiers
            .lineShape(lineShape)
            .lineShapeAmplitude(lineShapeAmplitude)
            .lineShapeFrequency(lineShapeFrequency)
            .shapeSegments(shapeSegments)
            .curvature(curvature)
            .curvatureIntensity(curvatureIntensity)
            .rayType(rayType)
            .rayOrientation(rayOrientation);
    }
    
    public static class Builder {
        private float rayLength = 2.0f;
        private float rayWidth = 1.0f;
        private int count = 12;
        private RayArrangement arrangement = RayArrangement.RADIAL;
        private RayDistribution distribution = RayDistribution.UNIFORM;
        private float innerRadius = 0.5f;
        private float outerRadius = 3.0f;
        private int layers = 1;
        private float layerSpacing = 0.5f;
        private float randomness = 0.0f;
        private float lengthVariation = 0.0f;
        private float fadeStart = 1.0f;
        private float fadeEnd = 1.0f;
        private int segments = 1;
        private float segmentGap = 0.0f;
        // Shape modifiers
        private RayLineShape lineShape = RayLineShape.STRAIGHT;
        private float lineShapeAmplitude = 0.1f;
        private float lineShapeFrequency = 2.0f;
        private int shapeSegments = 16;
        private RayCurvature curvature = RayCurvature.NONE;
        private float curvatureIntensity = 0.0f;
        private RayType rayType = RayType.LINE;
        private RayOrientation rayOrientation = RayOrientation.ALONG_RAY;
        
        public Builder rayLength(float v) { this.rayLength = v; return this; }
        public Builder rayWidth(float v) { this.rayWidth = v; return this; }
        public Builder count(int v) { this.count = v; return this; }
        public Builder arrangement(RayArrangement v) { this.arrangement = v; return this; }
        public Builder distribution(RayDistribution v) { this.distribution = v; return this; }
        public Builder innerRadius(float v) { this.innerRadius = v; return this; }
        public Builder outerRadius(float v) { this.outerRadius = v; return this; }
        public Builder layers(int v) { this.layers = v; return this; }
        public Builder layerSpacing(float v) { this.layerSpacing = v; return this; }
        public Builder randomness(float v) { this.randomness = v; return this; }
        public Builder lengthVariation(float v) { this.lengthVariation = v; return this; }
        public Builder fadeStart(float v) { this.fadeStart = v; return this; }
        public Builder fadeEnd(float v) { this.fadeEnd = v; return this; }
        public Builder segments(int v) { this.segments = v; return this; }
        public Builder segmentGap(float v) { this.segmentGap = v; return this; }
        // Shape modifier setters
        public Builder lineShape(RayLineShape v) { this.lineShape = v != null ? v : RayLineShape.STRAIGHT; return this; }
        public Builder lineShapeAmplitude(float v) { this.lineShapeAmplitude = v; return this; }
        public Builder lineShapeFrequency(float v) { this.lineShapeFrequency = v; return this; }
        public Builder shapeSegments(int v) { this.shapeSegments = v; return this; }
        public Builder curvature(RayCurvature v) { this.curvature = v != null ? v : RayCurvature.NONE; return this; }
        public Builder curvatureIntensity(float v) { this.curvatureIntensity = v; return this; }
        public Builder rayType(RayType v) { this.rayType = v != null ? v : RayType.LINE; return this; }
        public Builder rayOrientation(RayOrientation v) { this.rayOrientation = v != null ? v : RayOrientation.ALONG_RAY; return this; }
        
        public RaysShape build() {
            return new RaysShape(
                rayLength, rayWidth, count, arrangement, distribution, innerRadius, outerRadius,
                layers, layerSpacing, randomness, lengthVariation,
                fadeStart, fadeEnd, segments, segmentGap,
                lineShape, lineShapeAmplitude, lineShapeFrequency, shapeSegments,
                curvature, curvatureIntensity, rayType, rayOrientation
            );
        }
    }
}
