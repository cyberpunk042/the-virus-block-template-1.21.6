package net.cyberpunk042.visual.pattern;

/**
 * A dynamically generated edge pattern from shuffle exploration.
 * 
 * <p>Unlike static {@link EdgePattern} enums, this allows runtime
 * exploration of all possible lat/lon/skip combinations.
 * 
 * @see ShuffleGenerator
 */
public record DynamicEdgePattern(
    boolean renderLatitude,
    boolean renderLongitude,
    int skipInterval,
    int shuffleIndex,
    String description
) implements VertexPattern {
    
    @Override
    public String id() {
        return "shuffle_edge_" + shuffleIndex;
    }
    
    @Override
    public PatternGeometry geometry() {
        return PatternGeometry.EDGE;
    }
    
    /**
     * Whether to render latitude (horizontal) lines.
     */
    public boolean renderLatitude() {
        return renderLatitude;
    }
    
    /**
     * Whether to render longitude (vertical) lines.
     */
    public boolean renderLongitude() {
        return renderLongitude;
    }
    
    /**
     * Determines if an edge at given index should be rendered.
     */
    public boolean shouldRender(int index) {
        return (index % skipInterval) == 0;
    }
    
    /**
     * Creates from a ShuffleGenerator arrangement.
     */
    public static DynamicEdgePattern fromArrangement(ShuffleGenerator.EdgeArrangement arr) {
        return new DynamicEdgePattern(
            arr.latitude(),
            arr.longitude(),
            arr.skipInterval(),
            arr.index(),
            arr.describe()
        );
    }
}

