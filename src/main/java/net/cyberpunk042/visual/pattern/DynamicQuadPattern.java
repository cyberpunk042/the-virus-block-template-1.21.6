package net.cyberpunk042.visual.pattern;

import java.util.List;

/**
 * A dynamically generated quad pattern from shuffle exploration.
 * 
 * <p>Unlike static {@link QuadPattern} enums, this allows runtime
 * exploration of all possible corner arrangements.
 * 
 * @see ShuffleGenerator
 */
public record DynamicQuadPattern(
    QuadPattern.Corner[] tri1,
    QuadPattern.Corner[] tri2,
    int shuffleIndex,
    String description
) implements VertexPattern {
    
    @Override
    public String id() {
        return "shuffle_" + shuffleIndex;
    }
    
    @Override
    public PatternGeometry geometry() {
        return PatternGeometry.QUAD;
    }
    
    /**
     * Gets the triangles for this pattern.
     */
    public List<QuadPattern.Corner[]> triangles() {
        List<QuadPattern.Corner[]> result = new java.util.ArrayList<>();
        result.add(tri1);
        if (tri2 != null) {
            result.add(tri2);
        }
        return result;
    }
    
    /**
     * Number of triangles.
     */
    public int triangleCount() {
        return tri2 == null ? 1 : 2;
    }
    
    /**
     * Creates from a ShuffleGenerator arrangement.
     */
    public static DynamicQuadPattern fromArrangement(ShuffleGenerator.QuadArrangement arr) {
        List<QuadPattern.Corner[]> tris = arr.toQuadPatternTriangles();
        return new DynamicQuadPattern(
            tris.get(0),
            tris.size() > 1 ? tris.get(1) : null,
            arr.index(),
            arr.describe()
        );
    }
}

