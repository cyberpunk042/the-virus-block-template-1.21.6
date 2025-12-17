package net.cyberpunk042.client.gui.preview;

import net.minecraft.util.math.MathHelper;

/**
 * Handles 3D-to-2D projection for the preview renderer.
 * 
 * <p>Projects 3D world-space coordinates to 2D screen coordinates
 * with rotation and perspective applied. Also calculates depth
 * for back-to-front triangle sorting.</p>
 */
public class PreviewProjector {
    
    private static final float PERSPECTIVE = 0.4f;
    
    private final float centerX;
    private final float centerY;
    private final float scale;
    private final float rotX;  // Pitch rotation in radians
    private final float rotY;  // Yaw rotation in radians
    
    // Precomputed trig values for rotation
    private final float cosX, sinX, cosY, sinY;
    
    /**
     * Creates a projector with the specified parameters.
     * 
     * @param centerX Screen X center point
     * @param centerY Screen Y center point
     * @param scale Scale factor for projection
     * @param rotationPitch Pitch rotation in degrees
     * @param rotationYaw Yaw rotation in degrees
     */
    public PreviewProjector(float centerX, float centerY, float scale, 
                            float rotationPitch, float rotationYaw) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.scale = scale;
        this.rotX = (float) Math.toRadians(rotationPitch);
        this.rotY = (float) Math.toRadians(rotationYaw);
        
        // Precompute trig
        this.cosX = MathHelper.cos(rotX);
        this.sinX = MathHelper.sin(rotX);
        this.cosY = MathHelper.cos(rotY);
        this.sinY = MathHelper.sin(rotY);
    }
    
    /**
     * Projects a 3D point to 2D screen coordinates.
     * 
     * @param x World X coordinate
     * @param y World Y coordinate  
     * @param z World Z coordinate
     * @return float[3]: {screenX, screenY, depth}
     */
    public float[] project(float x, float y, float z) {
        // Rotate around Y axis (yaw)
        float x1 = x * cosY - z * sinY;
        float z1 = x * sinY + z * cosY;
        
        // Rotate around X axis (pitch)
        float y1 = y * cosX - z1 * sinX;
        float z2 = y * sinX + z1 * cosX;
        
        // Perspective projection
        float perspectiveFactor = 1f / (1f + z2 * PERSPECTIVE * 0.1f);
        
        return new float[] {
            centerX + x1 * scale * perspectiveFactor,
            centerY - y1 * scale * perspectiveFactor,  // Flip Y for screen coords
            z2  // Depth for sorting (positive = further from camera)
        };
    }
    
    /**
     * Projects a triangle and returns a PreviewTriangle with depth.
     * 
     * @param v1 First vertex {x, y, z}
     * @param v2 Second vertex {x, y, z}
     * @param v3 Third vertex {x, y, z}
     * @param color Fill and edge color (ARGB)
     * @return Projected triangle ready for rasterization
     */
    public PreviewTriangle projectTriangle(float[] v1, float[] v2, float[] v3, int color) {
        float[] p1 = project(v1[0], v1[1], v1[2]);
        float[] p2 = project(v2[0], v2[1], v2[2]);
        float[] p3 = project(v3[0], v3[1], v3[2]);
        
        // Average depth for sorting
        float avgDepth = (p1[2] + p2[2] + p3[2]) / 3f;
        
        return PreviewTriangle.of(
            p1[0], p1[1], 
            p2[0], p2[1], 
            p3[0], p3[1], 
            avgDepth, color
        );
    }
    
    /**
     * Projects a triangle with separate fill and edge colors.
     */
    public PreviewTriangle projectTriangle(float[] v1, float[] v2, float[] v3, 
                                           int fillColor, int edgeColor) {
        float[] p1 = project(v1[0], v1[1], v1[2]);
        float[] p2 = project(v2[0], v2[1], v2[2]);
        float[] p3 = project(v3[0], v3[1], v3[2]);
        
        float avgDepth = (p1[2] + p2[2] + p3[2]) / 3f;
        
        return PreviewTriangle.of(
            p1[0], p1[1], 
            p2[0], p2[1], 
            p3[0], p3[1], 
            avgDepth, fillColor, edgeColor
        );
    }
    
    // Accessors
    public float getCenterX() { return centerX; }
    public float getCenterY() { return centerY; }
    public float getScale() { return scale; }
}
