package net.cyberpunk042.visual.fill;

import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;

/**
 * Cage options for disc shapes.
 * 
 * <p>Renders radial lines from center to edge and concentric rings.</p>
 * 
 * <h2>Visual Pattern</h2>
 * <pre>
 *       __
 *     /    \
 *    /  \|  \   ← radial lines from center
 *   |----+---|  ← concentric rings
 *    \  /|\  /
 *     \__/
 * </pre>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "cage": {
 *   "lineWidth": 1.0,
 *   "radialLines": 8,
 *   "concentricRings": 3,
 *   "showCenter": true
 * }
 * </pre>
 * 
 * @see CageOptions
 * @see net.cyberpunk042.visual.shape.DiscShape
 */
public record DiscCageOptions(
    @Range(ValueRange.POSITIVE_NONZERO) float lineWidth,
    boolean showEdges,
    @Range(ValueRange.STEPS) int radialLines,
    @Range(ValueRange.STEPS) int concentricRings,
    boolean showCenter
) implements CageOptions {
    
    /** Default disc cage (8 radials, 3 rings, show center). */
    public static final DiscCageOptions DEFAULT = new DiscCageOptions(
        CageOptions.DEFAULT_LINE_WIDTH, true, 8, 3, true);
    
    /** Minimal disc cage (4 radials, 1 ring). */
    public static final DiscCageOptions MINIMAL = new DiscCageOptions(
        CageOptions.DEFAULT_LINE_WIDTH, true, 4, 1, false);
    
    /** Dense disc cage (16 radials, 5 rings). */
    public static final DiscCageOptions DENSE = new DiscCageOptions(
        CageOptions.DEFAULT_LINE_WIDTH, true, 16, 5, true);
    
    /**
     * Creates disc cage options.
     * @param radials Number of radial lines from center
     * @param rings Number of concentric rings
     */
    public static DiscCageOptions of(int radials, int rings) {
        return new DiscCageOptions(CageOptions.DEFAULT_LINE_WIDTH, true, radials, rings, true);
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    
    /** Create a builder pre-populated with this record's values. */
    public Builder toBuilder() {
        return new Builder()
            .lineWidth(lineWidth)
            .showEdges(showEdges)
            .radialLines(radialLines)
            .concentricRings(concentricRings)
            .showCenter(showCenter);
    }
    
    public static class Builder {
        private float lineWidth = CageOptions.DEFAULT_LINE_WIDTH;
        private boolean showEdges = true;
        private int radialLines = 8;
        private int concentricRings = 3;
        private boolean showCenter = true;
        
        public Builder lineWidth(float w) { this.lineWidth = w; return this; }
        public Builder showEdges(boolean s) { this.showEdges = s; return this; }
        public Builder radialLines(int c) { this.radialLines = c; return this; }
        public Builder concentricRings(int c) { this.concentricRings = c; return this; }
        public Builder showCenter(boolean s) { this.showCenter = s; return this; }
        
        public DiscCageOptions build() {
            return new DiscCageOptions(lineWidth, showEdges, radialLines, 
                concentricRings, showCenter);
        }
    }
}
