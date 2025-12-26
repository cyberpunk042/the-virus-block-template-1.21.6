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
     * @param visibility Visibility mask (null = full visibility)
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
            net.cyberpunk042.visual.pattern.VertexPattern pattern,
            net.cyberpunk042.visual.visibility.VisibilityMask visibility) {
        // Call the full version with default flow values
        generateDroplet(builder, center, direction, radius, intensity, length,
                       rings, segments, pattern, visibility, 0.0f, 1.0f, 1.0f);
    }
    
    /**
     * Generates a droplet/teardrop shape for rays with flow animation support.
     * 
     * <p>Uses SphereDeformation.DROPLET.computeFullVertex with flow-based vertex alpha.</p>
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
     * @param visibility Visibility mask (null = full visibility)
     * @param visibleTStart Start of visible range on axis (0-1, for CLIP mode)
     * @param visibleTEnd End of visible range on axis (0-1, for CLIP mode)
     * @param flowAlpha Base alpha from flow animation (flicker, etc.)
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
            net.cyberpunk042.visual.pattern.VertexPattern pattern,
            net.cyberpunk042.visual.visibility.VisibilityMask visibility,
            float visibleTStart,
            float visibleTEnd,
            float flowAlpha) {
        
        // Use SphereDeformation.DROPLET - it handles intensity and length correctly!
        SphereDeformation deformation = SphereDeformation.DROPLET;
        
        // Wrap the vertex function to compute per-vertex alpha based on visibility range
        final float finalVisibleTStart = visibleTStart;
        final float finalVisibleTEnd = visibleTEnd;
        final float finalFlowAlpha = flowAlpha;
        
        VectorMath.FullVertexWithAlphaFunction vertexFunc = (theta, phi, r) -> {
            // Base vertex from deformation
            float[] vertex = deformation.computeFullVertex(theta, phi, r, intensity, length);
            
            // Compute t-value based on theta (0 at bottom, 1 at top)
            // theta goes from 0 (top) to PI (bottom), so invert for t
            float t = 1.0f - (theta / (float)Math.PI);
            
            // Apply visibility clipping for CLIP mode
            float alpha = finalFlowAlpha;
            if (finalVisibleTStart > 0.0f || finalVisibleTEnd < 1.0f) {
                // Check if this vertex is in the visible range
                if (t < finalVisibleTStart) {
                    // Below visible range - fade out
                    float fadeRange = 0.1f; // 10% fade zone
                    if (t < finalVisibleTStart - fadeRange) {
                        alpha = 0.0f;
                    } else {
                        alpha *= (t - (finalVisibleTStart - fadeRange)) / fadeRange;
                    }
                } else if (t > finalVisibleTEnd) {
                    // Above visible range - fade out
                    float fadeRange = 0.1f;
                    if (t > finalVisibleTEnd + fadeRange) {
                        alpha = 0.0f;
                    } else {
                        alpha *= 1.0f - (t - finalVisibleTEnd) / fadeRange;
                    }
                }
            }
            
            // Return vertex with alpha
            return new float[] { vertex[0], vertex[1], vertex[2], vertex[3], vertex[4], vertex[5], alpha };
        };
        
        // Use the grid generation with per-vertex alpha support
        VectorMath.generateLatLonGridFullOrientedWithAlpha(
            builder, center, direction, radius, rings, segments, vertexFunc, pattern, visibility);
    }
    
    /**
     * Generates a droplet with proper gravitational spaghettification.
     * 
     * <p>Applies per-vertex deformation based on each vertex's distance from the field center:
     * <ul>
     *   <li><b>Radial stretch:</b> Vertices are pulled toward/away from center (∝ 1/r³)</li>
     *   <li><b>Perpendicular compression:</b> Vertices are compressed perpendicular to radial axis</li>
     * </ul>
     * This creates the authentic "spaghettification" effect of matter near a black hole.</p>
     * 
     * @param builder MeshBuilder to add geometry to
     * @param center Center position of droplet (in world space)
     * @param direction Direction the tip points (normalized)
     * @param radius Radius at the fattest point
     * @param intensity Blend amount: 0=sphere, 1=full droplet
     * @param length Axial stretch: <1 oblate, 1 normal, >1 prolate
     * @param rings Number of latitude rings
     * @param segments Number of longitude segments
     * @param pattern Pattern for cell rendering (null = filled)
     * @param visibility Visibility mask (null = full visibility)
     * @param visibleTStart Start of visible range on axis (0-1, for CLIP mode)
     * @param visibleTEnd End of visible range on axis (0-1, for CLIP mode)
     * @param flowAlpha Base alpha from flow animation (flicker, etc.)
     * @param fieldCenter Center of gravity (origin of gravitational pull)
     * @param gravityIntensity Strength of gravitational deformation (0 = none, 1 = strong)
     * @param fieldOuterRadius Outer radius of field (for normalizing distances)
     * @param gravityMode Type of deformation: GRAVITATIONAL, REPULSION, or TIDAL
     */
    public static void generateDropletWithGravity(
            MeshBuilder builder,
            float[] center,
            float[] direction,
            float radius,
            float intensity,
            float length,
            int rings,
            int segments,
            net.cyberpunk042.visual.pattern.VertexPattern pattern,
            net.cyberpunk042.visual.visibility.VisibilityMask visibility,
            float visibleTStart,
            float visibleTEnd,
            float flowAlpha,
            float[] fieldCenter,
            float gravityIntensity,
            float fieldOuterRadius,
            net.cyberpunk042.visual.shape.FieldDeformationMode gravityMode) {
        
        // Skip if no gravity effect
        if (gravityMode == null || !gravityMode.isActive() || gravityIntensity <= 0.001f) {
            generateDroplet(builder, center, direction, radius, intensity, length,
                rings, segments, pattern, visibility, visibleTStart, visibleTEnd, flowAlpha);
            return;
        }
        
        SphereDeformation deformation = SphereDeformation.DROPLET;
        
        final float finalVisibleTStart = visibleTStart;
        final float finalVisibleTEnd = visibleTEnd;
        final float finalFlowAlpha = flowAlpha;
        final float[] finalFieldCenter = fieldCenter != null ? fieldCenter : new float[] {0, 0, 0};
        final float finalOuterRadius = Math.max(0.1f, fieldOuterRadius);
        
        VectorMath.FullVertexWithAlphaFunction vertexFunc = (theta, phi, r) -> {
            // Get base vertex from deformation
            float[] vertex = deformation.computeFullVertex(theta, phi, r, intensity, length);
            
            // The vertex is in local (shape) space - need to convert to consider world position
            // vertex[0,1,2] = local position, vertex[3,4,5] = normal
            float localX = vertex[0];
            float localY = vertex[1];
            float localZ = vertex[2];
            
            // Transform local position to world space (add droplet center)
            float worldX = center[0] + localX * direction[0] + localY * radius;
            float worldY = center[1] + localX * direction[1] + localY * radius;
            float worldZ = center[2] + localX * direction[2] + localY * radius;
            
            // Actually, for simpler math, let's work in droplet-local space but consider
            // the droplet's position relative to field center
            
            // Compute vector from field center to this vertex's world position
            // Approximate: the droplet center is at 'center', and vertices are offset from there
            float toCenterX = center[0] - finalFieldCenter[0];
            float toCenterY = center[1] - finalFieldCenter[1];
            float toCenterZ = center[2] - finalFieldCenter[2];
            
            // Distance from field center to droplet center
            float distToCenter = (float) Math.sqrt(
                toCenterX * toCenterX + toCenterY * toCenterY + toCenterZ * toCenterZ);
            
            // Normalized radial direction (toward field center)
            float radialX = 0, radialY = 0, radialZ = 0;
            if (distToCenter > 0.001f) {
                radialX = -toCenterX / distToCenter;
                radialY = -toCenterY / distToCenter;
                radialZ = -toCenterZ / distToCenter;
            }
            
            // Compute normalized distance (0 = at center, 1 = at outer radius)
            float normalizedDist = Math.max(0.01f, Math.min(1.0f, distToCenter / finalOuterRadius));
            
            // === Tidal force calculation ===
            // Tidal force ∝ 1/r³, but we use 1/r² for visual effect (less extreme)
            // The deformation gets stronger as we get closer to center
            float tidalStrength = 1.0f / (normalizedDist * normalizedDist);
            tidalStrength = (tidalStrength - 1.0f) * gravityIntensity;  // Scale by intensity
            tidalStrength = Math.min(tidalStrength, 10.0f);  // Clamp to prevent extreme values
            
            // Compute how much this specific vertex should be stretched
            // Project vertex local position onto radial direction
            float dotRadial = localX * radialX + localY * radialY + localZ * radialZ;
            
            // === Apply gravitational deformation ===
            float stretchFactor = 1.0f;
            float compressFactor = 1.0f;
            
            switch (gravityMode) {
                case GRAVITATIONAL -> {
                    // Stretch toward center, compress perpendicular
                    stretchFactor = 1.0f + tidalStrength * 0.5f;
                    compressFactor = 1.0f / (float) Math.sqrt(stretchFactor); // conserve volume-ish
                }
                case REPULSION -> {
                    // Stretch away from center
                    stretchFactor = 1.0f + tidalStrength * 0.5f;
                    compressFactor = 1.0f / (float) Math.sqrt(stretchFactor);
                    // Invert the radial displacement
                    dotRadial = -dotRadial;
                }
                case TIDAL -> {
                    // Both stretch and compress (true tidal effect)
                    stretchFactor = 1.0f + tidalStrength * 0.3f;
                    compressFactor = 1.0f - tidalStrength * 0.2f;
                    compressFactor = Math.max(0.3f, compressFactor);
                }
                default -> {}
            }
            
            // Apply radial stretch: displace vertex along radial direction
            float radialDisplacement = dotRadial * (stretchFactor - 1.0f);
            
            // Compute perpendicular component for compression
            float perpX = localX - dotRadial * radialX;
            float perpY = localY - dotRadial * radialY;
            float perpZ = localZ - dotRadial * radialZ;
            
            // Final deformed position
            float deformedX = dotRadial * radialX * stretchFactor + perpX * compressFactor;
            float deformedY = dotRadial * radialY * stretchFactor + perpY * compressFactor;
            float deformedZ = dotRadial * radialZ * stretchFactor + perpZ * compressFactor;
            
            // Blend between original and deformed based on intensity
            float blend = Math.min(1.0f, gravityIntensity);
            float finalX = localX * (1.0f - blend) + deformedX * blend;
            float finalY = localY * (1.0f - blend) + deformedY * blend;
            float finalZ = localZ * (1.0f - blend) + deformedZ * blend;
            
            // Compute t-value for visibility clipping
            float t = 1.0f - (theta / (float)Math.PI);
            
            // Apply visibility clipping for CLIP mode
            float alpha = finalFlowAlpha;
            if (finalVisibleTStart > 0.0f || finalVisibleTEnd < 1.0f) {
                if (t < finalVisibleTStart) {
                    float fadeRange = 0.1f;
                    if (t < finalVisibleTStart - fadeRange) {
                        alpha = 0.0f;
                    } else {
                        alpha *= (t - (finalVisibleTStart - fadeRange)) / fadeRange;
                    }
                } else if (t > finalVisibleTEnd) {
                    float fadeRange = 0.1f;
                    if (t > finalVisibleTEnd + fadeRange) {
                        alpha = 0.0f;
                    } else {
                        alpha *= 1.0f - (t - finalVisibleTEnd) / fadeRange;
                    }
                }
            }
            
            // Return deformed vertex (keep original normal for now - could also deform normal)
            return new float[] { finalX, finalY, finalZ, vertex[3], vertex[4], vertex[5], alpha };
        };
        
        VectorMath.generateLatLonGridFullOrientedWithAlpha(
            builder, center, direction, radius, rings, segments, vertexFunc, pattern, visibility);
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
