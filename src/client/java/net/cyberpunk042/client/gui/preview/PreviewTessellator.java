package net.cyberpunk042.client.gui.preview;

import net.cyberpunk042.client.gui.state.FieldEditState;

import java.util.List;

/**
 * Interface for shape-specific triangle generation in the preview renderer.
 * 
 * <p>Each shape type has its own tessellator implementation that generates
 * triangles for 2D projected rendering. These are DISTINCT from the actual
 * shape tessellators used in world rendering (in the tessellator package).</p>
 * 
 * <p>Naming convention: Preview*Tessellator (e.g., PreviewSphereTessellator)
 * to distinguish from the real tessellators (e.g., SphereTessellator).</p>
 */
public interface PreviewTessellator {
    
    /**
     * Generates projected triangles for this shape.
     * 
     * @param projector Handles 3D-to-2D projection with rotation
     * @param state Field edit state with shape parameters
     * @param color Fill color (ARGB with alpha)
     * @param detail Number of segments for tessellation quality
     * @return List of projected triangles ready for rasterization
     */
    List<PreviewTriangle> tessellate(PreviewProjector projector, 
                                      FieldEditState state, 
                                      int color, 
                                      int detail);
    
    /**
     * @return The shape type this tessellator handles (e.g., "sphere")
     */
    String getShapeType();
}
