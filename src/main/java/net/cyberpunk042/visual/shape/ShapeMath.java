package net.cyberpunk042.visual.shape;

/**
 * Pure math functions for polar shape deformation.
 * 
 * <p>These functions define how radius varies with polar angle (θ) to create
 * various organic shapes from a sphere. θ=0 is the "tip" (top/north pole),
 * θ=π is the "base" (bottom/south pole).</p>
 * 
 * <p>All functions return a radius factor in range [0, 1+] that multiplies
 * the base radius.</p>
 * 
 * <h2>Usage</h2>
 * <pre>
 * float factor = ShapeMath.droplet(theta, 2.0f);
 * float deformedRadius = baseRadius * factor;
 * </pre>
 * 
 * @see SphereDeformation
 */
public final class ShapeMath {
    
    private static final float PI = (float) Math.PI;
    private static final float HALF_PI = (float) (Math.PI * 0.5);
    
    private ShapeMath() {} // Utility class
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Polar Shape Functions
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Sphere: constant radius.
     * r(θ) = 1
     */
    public static float sphere(float theta) {
        return 1.0f;
    }
    
    /**
     * Droplet/teardrop shape.
     * r(θ) = sin(θ/2)^power
     * 
     * <p>Creates a shape that's pointed at θ=0 (tip) and full radius at θ=π (base).</p>
     * 
     * @param theta Polar angle (0 = tip, π = base)
     * @param power Sharpness: 1.0 = hemisphere, 2.0 = teardrop, 4.0+ = very pointy
     * @return Radius factor (0 to 1)
     */
    public static float droplet(float theta, float power) {
        float base = (float) Math.sin(theta * 0.5f);
        return (float) Math.pow(base, power);
    }
    
    /**
     * Egg/oval shape (asymmetric).
     * r(θ) = 1 + asymmetry × cos(θ)
     * 
     * <p>Creates a shape that's fatter at one end (θ=0 is wider).</p>
     * 
     * @param theta Polar angle (0 to π)
     * @param asymmetry Degree of asymmetry: 0 = sphere, 0.3 = egg shape
     * @return Radius factor (> 0)
     */
    public static float egg(float theta, float asymmetry) {
        return 1.0f + asymmetry * (float) Math.cos(theta);
    }
    
    /**
     * Bullet/capsule tip shape.
     * r(θ) = sin(θ) for θ in [0, π/2], then 1 for θ in [π/2, π]
     * 
     * @param theta Polar angle (0 to π)
     * @return Radius factor (0 to 1)
     */
    public static float bullet(float theta) {
        if (theta < HALF_PI) {
            return (float) Math.sin(theta);
        }
        return 1.0f;
    }
    
    /**
     * Cone shape.
     * r(θ) = θ/π (linear from 0 at tip to 1 at base)
     * 
     * @param theta Polar angle (0 = tip, π = base)
     * @return Radius factor (0 to 1)
     */
    public static float cone(float theta) {
        return theta / PI;
    }
    
    /**
     * Inverted droplet (pointy at bottom instead of top).
     * 
     * @param theta Polar angle (0 to π)
     * @param power Sharpness: 1.0 = hemisphere, 2.0 = teardrop
     * @return Radius factor (0 to 1)
     */
    public static float dropletInverted(float theta, float power) {
        float flippedTheta = PI - theta;
        return droplet(flippedTheta, power);
    }
    
    /**
     * Blends between sphere (factor=1) and another shape.
     * 
     * @param shapeFactor The raw shape factor
     * @param intensity Blend amount: 0 = sphere, 1 = full shape
     * @return Blended factor
     */
    public static float blend(float shapeFactor, float intensity) {
        return 1.0f + (shapeFactor - 1.0f) * Math.min(1.0f, intensity);
    }
}
