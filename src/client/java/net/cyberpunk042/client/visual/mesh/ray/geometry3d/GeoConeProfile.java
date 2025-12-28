package net.cyberpunk042.client.visual.mesh.ray.geometry3d;

/**
 * Cone radius profile - linear taper.
 * 
 * <p>Based on ShapeMath.coneVertex formula:
 * taper = θ/π (linear from 0 at tip to 1 at base)
 * </p>
 * 
 * @see net.cyberpunk042.visual.shape.ShapeMath#coneVertex
 */
public final class GeoConeProfile implements GeoRadiusProfile {
    
    private static final float PI = (float) Math.PI;
    
    public static final GeoConeProfile INSTANCE = new GeoConeProfile();
    
    private GeoConeProfile() {}
    
    @Override
    public float radius(float theta) {
        // From ShapeMath.coneVertex line 313:
        // Linear taper: 0 at tip (θ=0), 1 at base (θ=π)
        return theta / PI;
    }
    
    @Override
    public int suggestedMinRings() {
        return 6;  // Linear profile needs fewer rings
    }
}
