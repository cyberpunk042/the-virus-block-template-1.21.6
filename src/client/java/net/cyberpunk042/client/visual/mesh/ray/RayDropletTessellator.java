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
        
        // === APPLY FLOW SCALE ===
        // flowScale modifies the droplet size for SCALE edge transition mode
        float flowScale = context.flowScale();
        if (flowScale < 0.01f) {
            // Too small to render - skip this droplet entirely
            return;
        }
        baseRadius *= flowScale;
        
        // Get ray endpoints
        float[] start = context.start();
        float[] end = context.end();
        
        // === APPLY CURVATURE TO POSITION ===
        // Curvature affects WHERE the droplet is placed (center position)
        // Orientation is ALWAYS respected for which way the tip points
        net.cyberpunk042.visual.shape.RayCurvature curvature = context.curvature();
        float curvatureIntensity = context.curvatureIntensity();
        
        float[] center;
        
        if (curvature != null && curvature != net.cyberpunk042.visual.shape.RayCurvature.NONE 
            && curvatureIntensity > 0.001f) {
            // Place center at curved midpoint (t=0.5)
            center = RayGeometryUtils.computeCurvedPosition(start, end, 0.5f, curvature, curvatureIntensity);
        } else {
            // No curvature - use simple midpoint
            center = new float[] {
                (start[0] + end[0]) * 0.5f,
                (start[1] + end[1]) * 0.5f,
                (start[2] + end[2]) * 0.5f
            };
        }
        
        // Orientation always comes from context (respects user's RayOrientation setting)
        float[] direction = context.orientationVector();
        
        // Determine mesh resolution from shapeSegments
        int totalSegs = Math.max(12, context.shapeSegments());
        int rings = Math.max(MIN_RINGS, totalSegs / 2);
        int segments = Math.max(MIN_SEGMENTS, totalSegs / 2);
        
        // shapeIntensity controls blend: 0=sphere, 1=full droplet
        // shapeLength controls axial stretch (also scaled by flowScale)
        float intensity = context.shapeIntensity();
        float axialLength = context.shapeLength() * flowScale;
        
        // Get visibility range for CLIP mode
        float visibleTStart = context.visibleTStart();
        float visibleTEnd = context.visibleTEnd();
        float flowAlpha = context.flowAlpha();
        
        // Check for field deformation (gravitational spaghettification)
        net.cyberpunk042.visual.shape.FieldDeformationMode gravityMode = context.fieldDeformation();
        float gravityIntensity = context.fieldDeformationIntensity();
        
        if (gravityMode != null && gravityMode.isActive() && gravityIntensity > 0.001f) {
            // Use the new per-vertex gravity deformation
            // Field center is at origin (0,0,0) for radial arrangements
            float[] fieldCenter = new float[] {0, 0, 0};
            
            // Get outer radius from shape or use a reasonable default
            float outerRadius = shape != null ? shape.outerRadius() : 3.0f;
            
            Ray3DGeometryUtils.generateDropletWithGravity(
                builder, center, direction, baseRadius,
                intensity, axialLength, rings, segments, pattern, visibility,
                visibleTStart, visibleTEnd, flowAlpha,
                fieldCenter, gravityIntensity, outerRadius, gravityMode
            );
        } else {
            // No gravity - use standard droplet generation
            Ray3DGeometryUtils.generateDroplet(
                builder, center, direction, baseRadius,
                intensity, axialLength, rings, segments, pattern, visibility,
                visibleTStart, visibleTEnd, flowAlpha
            );
        }
    }
}
