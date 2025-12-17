package net.cyberpunk042.client.gui.preview;

/**
 * Immutable data record for a projected triangle ready for 2D rendering.
 * 
 * <p>Used by the preview renderer to store screen-space triangles
 * after 3D projection, with depth information for back-to-front sorting.</p>
 */
public record PreviewTriangle(
    // Screen coordinates (2D)
    float x1, float y1,
    float x2, float y2,
    float x3, float y3,
    // Depth for sorting (average Z after projection, larger = further back)
    float depth,
    // Colors (ARGB)
    int fillColor,
    int edgeColor
) {
    /**
     * Creates a triangle with the same fill and edge color.
     */
    public static PreviewTriangle of(float x1, float y1, 
                                      float x2, float y2, 
                                      float x3, float y3, 
                                      float depth, int color) {
        return new PreviewTriangle(x1, y1, x2, y2, x3, y3, depth, color, color);
    }
    
    /**
     * Creates a triangle with separate fill and edge colors.
     */
    public static PreviewTriangle of(float x1, float y1, 
                                      float x2, float y2, 
                                      float x3, float y3, 
                                      float depth, int fillColor, int edgeColor) {
        return new PreviewTriangle(x1, y1, x2, y2, x3, y3, depth, fillColor, edgeColor);
    }
}
