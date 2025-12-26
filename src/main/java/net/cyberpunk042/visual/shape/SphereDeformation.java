package net.cyberpunk042.visual.shape;

/**
 * Controls deformation of sphere shapes into other polar forms.
 * 
 * <p>Uses polar shape functions from {@link ShapeMath} to modify 
 * the sphere's radius based on the polar angle (θ), creating droplet, 
 * egg, and other organic shapes.</p>
 * 
 * <h2>Shape Functions</h2>
 * <ul>
 *   <li><b>NONE:</b> r(θ) = 1 (standard sphere)</li>
 *   <li><b>DROPLET:</b> r(θ) = sin(θ/2)^power (teardrop, pointy at top)</li>
 *   <li><b>EGG:</b> r(θ) = 1 + asymmetry*cos(θ) (fatter at one end)</li>
 *   <li><b>BULLET:</b> r(θ) = hemisphere + cylinder (rounded tip)</li>
 * </ul>
 * 
 * @see SphereShape
 * @see ShapeMath
 */
public enum SphereDeformation {
    
    /** No deformation - standard sphere. */
    NONE("None", "Standard sphere"),
    
    /** Droplet/teardrop shape - pointy at top, fat at bottom. */
    DROPLET("Droplet", "Teardrop shape (pointy top)"),
    
    /** Droplet reversed - fat at top, pointy at bottom. */
    DROPLET_INVERTED("Droplet Inverted", "Teardrop shape (pointy bottom)"),
    
    /** Egg shape - asymmetric, fatter at one end. */
    EGG("Egg", "Egg shape (one end fatter)"),
    
    /** Bullet shape - hemispherical tip, cylindrical body. */
    BULLET("Bullet", "Rounded tip, flat base"),
    
    /** Cone-like - pointy tip, wide base. */
    CONE("Cone", "Conical taper");
    
    private final String displayName;
    private final String description;
    
    SphereDeformation(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String displayName() { return displayName; }
    public String description() { return description; }
    
    /**
     * Computes the radius multiplier for the given polar angle.
     * <p>Delegates to {@link ShapeMath} functions for the actual math.</p>
     * 
     * @param theta Polar angle (0 = top/north, π = bottom/south)
     * @param intensity Deformation intensity (0 = none, 1 = full effect)
     * @return Radius multiplier (0.0 to 1.0+)
     */
    public float computeRadiusFactor(float theta, float intensity) {
        if (intensity <= 0.0f || this == NONE) {
            return 1.0f;  // No deformation = sphere
        }
        
        // Get raw shape factor from shared math utilities
        float rawFactor = switch (this) {
            case NONE -> ShapeMath.sphere(theta);
            
            case DROPLET -> {
                // Use power based on intensity: 1.0 to 3.0
                float power = 1.0f + intensity * 2.0f;
                yield ShapeMath.droplet(theta, power);
            }
            
            case DROPLET_INVERTED -> {
                // Use power based on intensity: 1.0 to 3.0
                float power = 1.0f + intensity * 2.0f;
                yield ShapeMath.dropletInverted(theta, power);
            }
            
            case EGG -> {
                // Asymmetry based on intensity: 0 to 0.4
                float asymmetry = intensity * 0.4f;
                yield ShapeMath.egg(theta, asymmetry);
            }
            
            case BULLET -> ShapeMath.bullet(theta);
            
            case CONE -> {
                // Blend cone with sphere based on intensity
                float coneFactor = ShapeMath.cone(theta);
                yield ShapeMath.blend(coneFactor, intensity);
            }
        };
        
        return rawFactor;
    }
    
    /**
     * Whether this deformation creates a pointy end (needs special handling).
     */
    public boolean hasPointyEnd() {
        return this == DROPLET || this == DROPLET_INVERTED || this == CONE;
    }
}
