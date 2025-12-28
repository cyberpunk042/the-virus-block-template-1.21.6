package net.cyberpunk042.client.visual.mesh.ray.geometry3d;

/**
 * Egg radius profile - asymmetric oval.
 * 
 * <p>Based on ShapeMath.eggVertex formula:
 * radiusFactor = 1 + asymmetry * cos(θ)
 * Positive asymmetry = wider at bottom (θ=π), narrower at top (θ=0)
 * </p>
 * 
 * @see net.cyberpunk042.visual.shape.ShapeMath#eggVertex
 */
public final class GeoEggProfile implements GeoRadiusProfile {
    
    private final float asymmetry;
    
    public static final GeoEggProfile DEFAULT = new GeoEggProfile(0.3f);
    public static final GeoEggProfile MILD = new GeoEggProfile(0.15f);
    public static final GeoEggProfile STRONG = new GeoEggProfile(0.5f);
    
    public GeoEggProfile(float asymmetry) {
        this.asymmetry = Math.clamp(asymmetry, -0.8f, 0.8f);
    }
    
    @Override
    public float radius(float theta) {
        // From ShapeMath.eggVertex line 196:
        // radiusFactor = 1 + asymmetry * cos(θ)
        // Wider at bottom (θ=π where cos=-1), narrower at top (θ=0 where cos=1)
        return 1.0f + asymmetry * (float) Math.cos(theta);
    }
    
    @Override
    public int suggestedMinRings() {
        return 10;
    }
}
