package net.cyberpunk042.visual.pattern;

/**
 * A dynamically generated segment pattern from shuffle exploration.
 * 
 * <p>Unlike static {@link SegmentPattern} enums, this allows runtime
 * exploration of all possible skip/phase/winding combinations.
 * 
 * @see ShuffleGenerator
 */
public record DynamicSegmentPattern(
    int skipInterval,
    int phaseOffset,
    boolean reverseWinding,
    int shuffleIndex,
    String description
) implements VertexPattern {
    
    @Override
    public String id() {
        return "shuffle_seg_" + shuffleIndex;
    }
    
    @Override
    public PatternGeometry geometry() {
        return PatternGeometry.SEGMENT;
    }
    
    /**
     * Determines if a segment index should be rendered.
     */
    public boolean shouldRender(int index) {
        int adjusted = (index + phaseOffset);
        return (adjusted % skipInterval) == 0;
    }
    
    /**
     * Whether to reverse winding order for this pattern.
     */
    public boolean reverseWinding() {
        return reverseWinding;
    }
    
    /**
     * Creates from a ShuffleGenerator arrangement.
     */
    public static DynamicSegmentPattern fromArrangement(ShuffleGenerator.SegmentArrangement arr) {
        return new DynamicSegmentPattern(
            arr.skipInterval(),
            arr.phaseOffset(),
            arr.reverseWinding(),
            arr.index(),
            arr.describe()
        );
    }
}

