package net.cyberpunk042.client.visual.mesh.ray.geometry3d;

/**
 * Strategy for computing radius profiles for 3D ray shapes.
 * 
 * Different ray types (DROPLET, EGG, SPHERE, CONE, etc.) have different
 * radius functions based on the polar angle theta.
 */
public interface GeoRadiusProfile {
    
    /**
     * Compute radius at a given polar angle.
     * 
     * @param theta Polar angle from 0 (base) to PI (tip)
     * @return Radius multiplier (relative to base radius)
     */
    float radius(float theta);
    
    /**
     * Whether this profile needs multiple rings for smooth rendering.
     */
    default boolean needsMultipleRings() {
        return true;
    }
    
    /**
     * Suggested minimum rings for smooth rendering.
     */
    default int suggestedMinRings() {
        return 8;
    }
    
    /**
     * Name for debugging.
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
