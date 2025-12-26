package net.cyberpunk042.client.visual.mesh.ray;

import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.visual.shape.RaysShape;

/**
 * Tessellator for DROPLET ray type.
 * 
 * <p>Generates teardrop-shaped 3D geometry along each ray path using
 * polar shape deformation. The droplet is a sphere with radius that
 * varies from 0 at the tip to full at the base.</p>
 * 
 * <h2>Shape Formula</h2>
 * <pre>
 *   r(θ) = sin(θ/2)^power
 * </pre>
 * <p>Where θ is the polar angle (0 = tip, π = base) and power controls sharpness.</p>
 * 
 * <h2>Parameters</h2>
 * <ul>
 *   <li><b>rayWidth</b>: Radius of the droplet at its widest point</li>
 *   <li><b>shapeSegments</b>: Controls mesh smoothness (rings = segments/2)</li>
 *   <li><b>orientation</b>: Direction the tip points (from RayContext)</li>
 * </ul>
 * 
 * @see Ray3DGeometryUtils#generateDroplet
 * @see RayTypeTessellator
 */
public class RayDropletTessellator implements RayTypeTessellator {
    
    /** Singleton instance. */
    public static final RayDropletTessellator INSTANCE = new RayDropletTessellator();
    
    /** Minimum rings for smooth droplet. */
    private static final int MIN_RINGS = 6;
    
    /** Minimum segments for round cross-section. */
    private static final int MIN_SEGMENTS = 6;
    
    private RayDropletTessellator() {} // Use INSTANCE
    
    @Override
    public void tessellate(MeshBuilder builder, RaysShape shape, RayContext context) {
        tessellate(builder, shape, context, null, null);
    }
    
    @Override
    public void tessellate(MeshBuilder builder, RaysShape shape, RayContext context,
                          net.cyberpunk042.visual.pattern.VertexPattern pattern,
                          net.cyberpunk042.visual.visibility.VisibilityMask visibility) {
        // Get droplet parameters
        float length = context.length();
        float baseRadius = length * 0.5f;  // Radius = half the ray length for an inscribed droplet
        
        // Compute center position (midpoint of ray)
        float[] start = context.start();
        float[] end = context.end();
        float[] center = new float[] {
            (start[0] + end[0]) * 0.5f,
            (start[1] + end[1]) * 0.5f,
            (start[2] + end[2]) * 0.5f
        };
        
        // Get orientation (tip direction)
        float[] direction = context.orientationVector();
        
        // Determine mesh resolution from shapeSegments
        int totalSegs = Math.max(12, context.shapeSegments());
        int rings = Math.max(MIN_RINGS, totalSegs / 2);
        int segments = Math.max(MIN_SEGMENTS, totalSegs / 2);
        
        // shapeIntensity controls blend: 0=sphere, 1=full droplet
        // shapeLength controls axial stretch
        float intensity = context.shapeIntensity();
        float axialLength = context.shapeLength();
        
        // Generate the droplet using SphereDeformation with pattern and visibility support
        Ray3DGeometryUtils.generateDroplet(
            builder, center, direction, baseRadius,
            intensity, axialLength, rings, segments, pattern, visibility
        );
    }
}
