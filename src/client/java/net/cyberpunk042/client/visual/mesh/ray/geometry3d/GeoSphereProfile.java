package net.cyberpunk042.client.visual.mesh.ray.geometry3d;

/**
 * Sphere radius profile - constant radius.
 */
public final class GeoSphereProfile implements GeoRadiusProfile {
    
    public static final GeoSphereProfile INSTANCE = new GeoSphereProfile();
    
    private GeoSphereProfile() {}
    
    @Override
    public float radius(float theta) {
        // Constant radius (normalized to 1)
        return 1f;
    }
    
    @Override
    public int suggestedMinRings() {
        return 8;
    }
}
