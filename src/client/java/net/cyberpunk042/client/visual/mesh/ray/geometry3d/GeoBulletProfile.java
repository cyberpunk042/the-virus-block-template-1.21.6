package net.cyberpunk042.client.visual.mesh.ray.geometry3d;

/**
 * Bullet radius profile - hemisphere tip + cylinder body.
 * 
 * <p>Based on ShapeMath.bulletVertex formula:
 * - Top hemisphere (θ < π/2): radius = sin(θ)
 * - Bottom cylinder (θ >= π/2): radius = 1.0 (constant)
 * </p>
 * 
 * @see net.cyberpunk042.visual.shape.ShapeMath#bulletVertex
 */
public final class GeoBulletProfile implements GeoRadiusProfile {
    
    private static final float HALF_PI = (float) (Math.PI * 0.5);
    
    public static final GeoBulletProfile INSTANCE = new GeoBulletProfile();
    
    private GeoBulletProfile() {}
    
    @Override
    public float radius(float theta) {
        // From ShapeMath.bulletVertex lines 285-300:
        // Top hemisphere (0 to π/2): sin(θ)
        // Bottom cylinder (π/2 to π): constant 1.0
        if (theta < HALF_PI) {
            return (float) Math.sin(theta);
        }
        return 1.0f;
    }
    
    @Override
    public int suggestedMinRings() {
        return 10;
    }
}
