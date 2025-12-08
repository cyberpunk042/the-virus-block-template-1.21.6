package net.cyberpunk042.client.visual._legacy.render;

import net.cyberpunk042.visual.pattern.VertexPattern;

/**
 * Holds runtime override values for field rendering.
 * Used by FieldTestCommand for live editing.
 * 
 * <h2>Usage</h2>
 * <pre>
 * RenderOverrides_old overrides = RenderOverrides_old.builder()
 *     .radius(10.0f)
 *     .vertexPattern(QuadPattern.MESHED)
 *     .build();
 * </pre>
 */
public record RenderOverrides_old(
    Float radius,
    Float scale,
    Float alpha,
    Float glow,
    Integer latSteps,
    Integer lonSteps,
    String algorithm,
    VertexPattern vertexPattern,
    String colorOverride
) {
    
    public static final RenderOverrides_old NONE = new RenderOverrides_old(
        null, null, null, null, null, null, null, null, null);
    
    public static Builder builder() {
        return new Builder();
    }
    
    public boolean hasOverrides() {
        return radius != null || scale != null || alpha != null || glow != null ||
               latSteps != null || lonSteps != null || algorithm != null ||
               vertexPattern != null || colorOverride != null;
    }
    
    public boolean hasColorOverride() {
        return colorOverride != null && !colorOverride.isEmpty();
    }
    
    public static class Builder {
        private Float radius;
        private Float scale;
        private Float alpha;
        private Float glow;
        private Integer latSteps;
        private Integer lonSteps;
        private String algorithm;
        private VertexPattern vertexPattern;
        private String colorOverride;
        
        public Builder radius(Float radius) { this.radius = radius; return this; }
        public Builder scale(Float scale) { this.scale = scale; return this; }
        public Builder alpha(Float alpha) { this.alpha = alpha; return this; }
        public Builder glow(Float glow) { this.glow = glow; return this; }
        public Builder latSteps(Integer latSteps) { this.latSteps = latSteps; return this; }
        public Builder lonSteps(Integer lonSteps) { this.lonSteps = lonSteps; return this; }
        public Builder algorithm(String algorithm) { this.algorithm = algorithm; return this; }
        public Builder vertexPattern(VertexPattern vertexPattern) { this.vertexPattern = vertexPattern; return this; }
        public Builder colorOverride(String colorOverride) { this.colorOverride = colorOverride; return this; }
        
        public RenderOverrides_old build() {
            return new RenderOverrides_old(radius, scale, alpha, glow, latSteps, lonSteps, 
                                       algorithm, vertexPattern, colorOverride);
        }
    }
}
