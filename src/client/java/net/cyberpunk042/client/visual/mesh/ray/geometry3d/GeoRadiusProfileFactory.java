package net.cyberpunk042.client.visual.mesh.ray.geometry3d;

import net.cyberpunk042.visual.shape.RayType;

/**
 * Factory for creating GeoRadiusProfile instances.
 * 
 * <p>Maps RayType to the appropriate radius profile for 3D shape generation.</p>
 * 
 * @see net.cyberpunk042.visual.shape.RayType
 */
public final class GeoRadiusProfileFactory {
    
    private GeoRadiusProfileFactory() {}
    
    /**
     * Get the appropriate profile for a ray type.
     * 
     * @param rayType The ray type
     * @return Radius profile, or null if this is a 2D type (LINE)
     */
    public static GeoRadiusProfile get(RayType rayType) {
        if (rayType == null) {
            return GeoDropletProfile.DEFAULT;
        }
        
        // Map RayType to appropriate radius profile
        return switch (rayType) {
            case LINE -> null;  // 2D type - no profile needed
            
            // Basic geometry - use matching profiles
            case DROPLET -> GeoDropletProfile.DEFAULT;
            case CONE -> GeoConeProfile.INSTANCE;
            case ARROW -> GeoConeProfile.INSTANCE;  // Similar to cone (pointed tip)
            case CAPSULE -> GeoBulletProfile.INSTANCE;  // Capsule uses bullet (hemisphere + cylinder)
            
            // Energy effects - use droplet/sphere-like profiles
            case KAMEHAMEHA -> GeoSphereProfile.INSTANCE;  // Spherical end
            case LASER -> GeoSphereProfile.INSTANCE;  // Uniform beam
            case LIGHTNING -> null;  // Procedural, not profile-based
            case FIRE_JET -> GeoDropletProfile.DEFAULT;  // Wide base, narrow tip
            case PLASMA -> GeoDropletProfile.DEFAULT;  // Organic shape
            
            // Particle types - use sphere for individual particles
            case BEADS -> GeoSphereProfile.INSTANCE;
            case CUBES -> GeoSphereProfile.INSTANCE;  // Fallback (cubes need special handling)
            case STARS -> GeoSphereProfile.INSTANCE;  // Fallback
            case CRYSTALS -> GeoConeProfile.INSTANCE;  // Faceted requires special handling
            
            // Organic types - use egg-like profiles
            case TENDRIL -> GeoEggProfile.DEFAULT;  // Organic asymmetry
            case SPINE -> GeoBulletProfile.INSTANCE;  // Segmented with rounded ends
            case ROOT -> GeoDropletProfile.ROUND;  // Natural taper
        };
    }
    
    /**
     * Get profile with custom parameters.
     * 
     * @param rayType The ray type
     * @param intensity Custom intensity parameter (type-specific)
     * @return Customized radius profile
     */
    public static GeoRadiusProfile getCustom(RayType rayType, float intensity) {
        if (rayType == null) {
            return GeoDropletProfile.DEFAULT;
        }
        
        return switch (rayType) {
            case DROPLET, FIRE_JET, PLASMA -> new GeoDropletProfile(1f + intensity);
            case TENDRIL -> new GeoEggProfile(intensity * 0.5f);
            default -> get(rayType);
        };
    }
    
    /**
     * Whether the ray type is a 3D volumetric shape (needs profile).
     */
    public static boolean is3DType(RayType rayType) {
        return rayType != null && rayType.is3D();
    }
}
