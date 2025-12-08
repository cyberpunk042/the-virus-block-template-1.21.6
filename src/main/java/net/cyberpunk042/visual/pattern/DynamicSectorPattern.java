package net.cyberpunk042.visual.pattern;

/**
 * A dynamically generated sector pattern from shuffle exploration.
 * 
 * <p>Unlike static {@link SectorPattern} enums, this allows runtime
 * exploration of all possible skip/phase/invert combinations.
 * 
 * @see ShuffleGenerator
 */
public record DynamicSectorPattern(
    int skipInterval,
    int phaseOffset,
    boolean invertSelection,
    int shuffleIndex,
    String description
) implements VertexPattern {
    
    @Override
    public String id() {
        return "shuffle_sec_" + shuffleIndex;
    }
    
    @Override
    public PatternGeometry geometry() {
        return PatternGeometry.SECTOR;
    }
    
    /**
     * Determines if a sector index should be rendered.
     */
    public boolean shouldRender(int index, int totalSectors) {
        if (skipInterval <= 1 && !invertSelection) return true;
        int adjusted = (index + phaseOffset) % totalSectors;
        boolean matches = (adjusted % skipInterval) == 0;
        return invertSelection ? !matches : matches;
    }
    
    /**
     * Creates from a ShuffleGenerator arrangement.
     */
    public static DynamicSectorPattern fromArrangement(ShuffleGenerator.SectorArrangement arr) {
        return new DynamicSectorPattern(
            arr.skipInterval(),
            arr.phaseOffset(),
            arr.invertSelection(),
            arr.index(),
            arr.describe()
        );
    }
}

