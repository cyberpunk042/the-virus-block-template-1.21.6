package net.cyberpunk042.client.visual.mesh.ray;

import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.client.visual.mesh.VectorMath;
import net.cyberpunk042.visual.shape.ShapeMath;
import net.cyberpunk042.visual.shape.SphereDeformation;

/**
 * Ray-specific 3D geometry utilities.
 * 
 * <p><b>Delegates to {@link VectorMath} for shared polar surface generation.</b></p>
 * 
 * <p>This class provides ray-specific convenience methods that use the core
 * polar surface algorithm from VectorMath with ray-appropriate defaults.</p>
 * 
 * @see VectorMath#generatePolarSurface
 * @see RayDropletTessellator
 * @see ShapeMath
 */
public final class Ray3DGeometryUtils {
    
    private Ray3DGeometryUtils() {} // Utility class
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Shape Functions (delegates to ShapeMath)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Sphere: constant radius. Delegates to {@link ShapeMath#sphere}. */
    public static float shapeSphere(float theta) {
        return ShapeMath.sphere(theta);
    }
    
    /** Droplet: sin(θ/2)^power. Delegates to {@link ShapeMath#droplet}. */
    public static float shapeDroplet(float theta, float power) {
        return ShapeMath.droplet(theta, power);
    }
    
    /** Egg/oval: 1 + asymmetry × cos(θ). Delegates to {@link ShapeMath#egg}. */
    public static float shapeEgg(float theta, float asymmetry) {
        return ShapeMath.egg(theta, asymmetry);
    }
    
    /** Bullet: hemisphere tip + cylinder. Delegates to {@link ShapeMath#bullet}. */
    public static float shapeBullet(float theta) {
        return ShapeMath.bullet(theta);
    }
    
    /** Cone: θ/π (linear). Delegates to {@link ShapeMath#cone}. */
    public static float shapeCone(float theta) {
        return ShapeMath.cone(theta);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Ray-Specific Convenience Methods (delegate to VectorMath)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Generates a droplet/teardrop shape for rays.
     * 
     * <p>Uses SphereDeformation.DROPLET.computeFullVertex - same approach as sphere!</p>
     * 
     * @param builder MeshBuilder to add geometry to
     * @param center Center position
     * @param direction Direction the tip points (normalized)
     * @param radius Radius at the fattest point
     * @param intensity Blend amount: 0=sphere, 1=full droplet
     * @param length Axial stretch: <1 oblate, 1 normal, >1 prolate
     * @param rings Number of latitude rings
     * @param segments Number of longitude segments
     * @param pattern Pattern for cell rendering (null = filled)
     */
    public static void generateDroplet(
            MeshBuilder builder,
            float[] center,
            float[] direction,
            float radius,
            float intensity,
            float length,
            int rings,
            int segments,
            net.cyberpunk042.visual.pattern.VertexPattern pattern) {
        
        // Use SphereDeformation.DROPLET - it handles intensity and length correctly!
        SphereDeformation deformation = SphereDeformation.DROPLET;
        
        VectorMath.FullVertexFunction vertexFunc = (theta, phi, r) -> 
            deformation.computeFullVertex(theta, phi, r, intensity, length);
        
        // Use the same grid generation as sphere - with pattern support
        VectorMath.generateLatLonGridFullOriented(
            builder, center, direction, radius, rings, segments, vertexFunc, pattern);
    }
    
    /**
     * Generates an egg/oval shape for rays.
     * 
     * <p>Delegates to {@link VectorMath#generatePolarSurface} with egg radius function.</p>
     */
    public static void generateEgg(
            MeshBuilder builder,
            float[] center,
            float[] direction,
            float radius,
            float asymmetry,
            int rings,
            int segments) {
        
        VectorMath.generatePolarSurface(builder, center, direction, radius, rings, segments,
            theta -> ShapeMath.egg(theta, asymmetry));
    }
    
    /**
     * Generates a sphere for rays.
     * 
     * <p>Delegates to {@link VectorMath#generatePolarSurface} with constant radius.</p>
     */
    public static void generateSphere(
            MeshBuilder builder,
            float[] center,
            float[] direction,
            float radius,
            int rings,
            int segments) {
        
        VectorMath.generatePolarSurface(builder, center, direction, radius, rings, segments, null);
    }
}
