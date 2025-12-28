package net.cyberpunk042.client.visual.mesh.ray.geometry3d;

/**
 * Droplet radius profile - teardrop shape.
 * 
 * <p>Based on ShapeMath.dropletVertex formula:
 * profile = (1 - cos(θ)) * |sin(θ)|^power
 * Creates pointed tip at θ=0, rounded base at θ=π.</p>
 * 
 * <p>power > 1 = sharper tip, power < 1 = rounder tip</p>
 * 
 * @see net.cyberpunk042.visual.shape.ShapeMath#dropletVertex
 */
public final class GeoDropletProfile implements GeoRadiusProfile {
    
    private final float power;
    
    public static final GeoDropletProfile DEFAULT = new GeoDropletProfile(1.0f);
    public static final GeoDropletProfile SHARP = new GeoDropletProfile(2.0f);
    public static final GeoDropletProfile ROUND = new GeoDropletProfile(0.5f);
    
    public GeoDropletProfile(float power) {
        this.power = Math.max(0.1f, power);
    }
    
    @Override
    public float radius(float theta) {
        // From ShapeMath.dropletVertex line 253-255:
        // profile = (1 - cos(θ)) * |sin(θ)|^power
        // This creates: pointed tip at θ=0, rounded base at θ=π
        float cosTheta = (float) Math.cos(theta);
        float sinTheta = (float) Math.sin(theta);
        
        float profile = (1.0f - cosTheta) * (float) Math.pow(Math.abs(sinTheta), power);
        // Normalize to 0-1 range (max is approximately 2 at θ=π)
        return profile * 0.5f;
    }
    
    @Override
    public int suggestedMinRings() {
        return 12;  // Droplets need more rings for smooth tip
    }
}
