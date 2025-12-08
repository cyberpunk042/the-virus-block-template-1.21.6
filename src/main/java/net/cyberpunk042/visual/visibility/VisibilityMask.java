package net.cyberpunk042.visual.visibility;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;

/**
 * Controls which parts of a shape are visible.
 * 
 * <h2>Mask Types</h2>
 * <ul>
 *   <li>{@link MaskType#FULL} - Everything visible (default)</li>
 *   <li>{@link MaskType#BANDS} - Horizontal bands</li>
 *   <li>{@link MaskType#STRIPES} - Vertical stripes</li>
 *   <li>{@link MaskType#CHECKER} - Checkerboard pattern</li>
 *   <li>{@link MaskType#RADIAL} - Radial segments</li>
 *   <li>{@link MaskType#GRADIENT} - Alpha gradient</li>
 *   <li>{@link MaskType#CUSTOM} - Custom mask function</li>
 * </ul>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "visibility": {
 *   "mask": "BANDS",
 *   "count": 4,
 *   "thickness": 0.5
 * }
 * </pre>
 * 
 * <p>Shorthand: {@code "visibility": "bands"} uses defaults.</p>
 * 
 * @see MaskType
 */
public record VisibilityMask(
    // Phase 1 fields
    MaskType mask,
    @Range(ValueRange.STEPS) int count,
    @Range(ValueRange.POSITIVE) float thickness,
    
    // Phase 2 fields (future)
    @Range(ValueRange.NORMALIZED) float offset,
    boolean invert,
    @Range(ValueRange.NORMALIZED) float feather,
    boolean animate,
    @Range(ValueRange.POSITIVE) float animSpeed,
    
    // Gradient options (future)
    @Nullable String direction,
    float falloff,
    float gradientStart,
    float gradientEnd
) {
    /** Full visibility (no masking). */
    public static final VisibilityMask FULL = new VisibilityMask(
        MaskType.FULL, 1, 1.0f, 0, false, 0, false, 0, null, 0, 0, 1);
    
    /** Default band mask (4 bands, 50% thickness). */
    public static final VisibilityMask BANDS = new VisibilityMask(
        MaskType.BANDS, 4, 0.5f, 0, false, 0, false, 0, null, 0, 0, 1);
    
    /** Default stripe mask (8 stripes, 50% thickness). */
    public static final VisibilityMask STRIPES = new VisibilityMask(
        MaskType.STRIPES, 8, 0.5f, 0, false, 0, false, 0, null, 0, 0, 1);
    
    /** Default checker mask (4x4 grid). */
    public static final VisibilityMask CHECKER = new VisibilityMask(
        MaskType.CHECKER, 4, 0.5f, 0, false, 0, false, 0, null, 0, 0, 1);
    
    // =========================================================================
    // Factory Methods
    // =========================================================================
    
    /**
     * Creates a band mask.
     * @param count Number of bands
     * @param thickness Band thickness (0-1)
     */
    public static VisibilityMask bands(@Range(ValueRange.STEPS) int count, @Range(ValueRange.POSITIVE) float thickness) {
        return builder().mask(MaskType.BANDS).count(count).thickness(thickness).build();
    }
    
    /**
     * Creates a stripe mask.
     * @param count Number of stripes
     * @param thickness Stripe thickness (0-1)
     */
    public static VisibilityMask stripes(@Range(ValueRange.STEPS) int count, @Range(ValueRange.POSITIVE) float thickness) {
        return builder().mask(MaskType.STRIPES).count(count).thickness(thickness).build();
    }
    
    /** Whether any masking is applied. */
    public boolean hasMask() {
        return mask != MaskType.FULL;
    }
    
    /**
     * Checks if a cell at the given UV coordinates should be visible.
     * 
     * <p>UV coordinates are normalized (0-1) representing position on the surface:
     * <ul>
     *   <li>u: horizontal position (longitude fraction for spheres, angle fraction for rings)</li>
     *   <li>v: vertical position (latitude fraction for spheres, radius fraction for rings)</li>
     * </ul>
     * 
     * @param u horizontal coordinate (0-1)
     * @param v vertical coordinate (0-1)
     * @return true if the cell should be rendered
     */
    public boolean isVisible(float u, float v) {
        if (mask == MaskType.FULL) {
            return true;
        }
        
        // Apply offset
        float adjustedU = (u + offset) % 1.0f;
        float adjustedV = (v + offset) % 1.0f;
        
        boolean visible = switch (mask) {
            case FULL -> true;
            
            case BANDS -> {
                // Horizontal bands based on v coordinate
                float bandPos = (adjustedV * count) % 1.0f;
                yield bandPos < thickness;
            }
            
            case STRIPES -> {
                // Vertical stripes based on u coordinate
                float stripePos = (adjustedU * count) % 1.0f;
                yield stripePos < thickness;
            }
            
            case CHECKER -> {
                // Checkerboard pattern
                int uCell = (int) (adjustedU * count);
                int vCell = (int) (adjustedV * count);
                yield (uCell + vCell) % 2 == 0;
            }
            
            case RADIAL -> {
                // Radial segments based on u (angle)
                float segmentPos = (adjustedU * count) % 1.0f;
                yield segmentPos < thickness;
            }
            
            case GRADIENT -> {
                // Gradient visibility based on position
                float gradientPos = direction != null && direction.equals("horizontal") ? adjustedU : adjustedV;
                yield gradientPos >= gradientStart && gradientPos <= gradientEnd;
            }
            
            case CUSTOM -> true; // Custom masks need external handling
        };
        
        return invert ? !visible : visible;
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    /**
     * Serializes this visibility mask to JSON.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (mask != MaskType.FULL) json.addProperty("mask", mask.name());
        if (count != FULL.count) json.addProperty("count", count);
        if (thickness != FULL.thickness) json.addProperty("thickness", thickness);
        if (offset != 0) json.addProperty("offset", offset);
        if (invert) json.addProperty("invert", true);
        if (feather != 0) json.addProperty("feather", feather);
        if (animate) json.addProperty("animate", true);
        if (animSpeed != 0) json.addProperty("animSpeed", animSpeed);
        if (direction != null) json.addProperty("direction", direction);
        if (falloff != 0) json.addProperty("falloff", falloff);
        if (gradientStart != 0) json.addProperty("gradientStart", gradientStart);
        if (gradientEnd != 1) json.addProperty("gradientEnd", gradientEnd);
        return json;
    }


    
    public static class Builder {
        private MaskType mask = MaskType.FULL;
        private @Range(ValueRange.STEPS) int count = 4;
        private @Range(ValueRange.POSITIVE) float thickness = 0.5f;
        private @Range(ValueRange.NORMALIZED) float offset = 0;
        private boolean invert = false;
        private @Range(ValueRange.NORMALIZED) float feather = 0;
        private boolean animate = false;
        private @Range(ValueRange.POSITIVE) float animSpeed = 0;
        private @Nullable String direction = null;
        private float falloff = 0;
        private float gradientStart = 0;
        private float gradientEnd = 1;
        
        public Builder mask(MaskType m) { this.mask = m; return this; }
        public Builder count(int c) { this.count = c; return this; }
        public Builder thickness(float t) { this.thickness = t; return this; }
        public Builder offset(float o) { this.offset = o; return this; }
        public Builder invert(boolean i) { this.invert = i; return this; }
        public Builder feather(float f) { this.feather = f; return this; }
        public Builder animate(boolean a) { this.animate = a; return this; }
        public Builder animSpeed(float s) { this.animSpeed = s; return this; }
        public Builder direction(String d) { this.direction = d; return this; }
        public Builder falloff(float f) { this.falloff = f; return this; }
        public Builder gradientStart(float s) { this.gradientStart = s; return this; }
        public Builder gradientEnd(float e) { this.gradientEnd = e; return this; }
        
        public VisibilityMask build() {
            return new VisibilityMask(mask, count, thickness, offset, invert, 
                feather, animate, animSpeed, direction, falloff, gradientStart, gradientEnd);
        }
    }
}
