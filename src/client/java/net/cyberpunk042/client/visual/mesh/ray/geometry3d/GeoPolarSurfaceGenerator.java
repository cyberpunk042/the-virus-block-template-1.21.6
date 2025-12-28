package net.cyberpunk042.client.visual.mesh.ray.geometry3d;

import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.client.visual.mesh.ray.flow.FlowTravelStage;
import net.cyberpunk042.visual.energy.EnergyTravel;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.SphereDeformation;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Generates 3D polar surfaces for ray shapes using SphereDeformation.
 * 
 * <p>Uses the existing {@link SphereDeformation} infrastructure from ShapeMath
 * for proper parametric shape generation with full intensity support:
 * <ul>
 *   <li>intensity 0 = sphere</li>
 *   <li>intensity 1 = full deformation (sharp droplet, etc.)</li>
 * </ul>
 * </p>
 * 
 * @see SphereDeformation
 * @see net.cyberpunk042.visual.shape.ShapeMath
 */
public final class GeoPolarSurfaceGenerator {
    
    private static final float PI = (float) Math.PI;
    private static final float TWO_PI = 2f * PI;
    
    private GeoPolarSurfaceGenerator() {}
    
    /**
     * Generate a polar surface using SphereDeformation with full flow and travel support.
     * 
     * @param builder MeshBuilder to add geometry to
     * @param deformation The sphere deformation type (DROPLET, CONE, EGG, etc.)
     * @param intensity Deformation intensity (0 = sphere, 1 = full effect)
     * @param center Center position [x, y, z]
     * @param direction Direction (axis) of the shape [x, y, z]
     * @param length Length along the axis
     * @param radius Maximum radius
     * @param rings Number of rings
     * @param segments Number of segments
     * @param pattern Pattern for cell rendering (null = filled)
     * @param visibility Visibility mask (null = full visibility)
     * @param visibleTStart Start of visible range (0-1)
     * @param visibleTEnd End of visible range (0-1)
     * @param flowAlpha Base alpha from flow animation
     * @param travelMode EnergyTravel for per-vertex alpha
     * @param travelPhase Current travel phase (0-1)
     * @param chaseCount Number of chase particles
     * @param chaseWidth Width of each particle
     * @param fieldDeformation Optional field deformation strategy
     * @param fieldCenter Center of field deformation
     * @param fieldDeformIntensity Field deformation intensity
     * @param outerRadius Outer radius for field deformation
     * @param baseColor Base ARGB color
     */
    public static void generateWithSphereDeformation(
            MeshBuilder builder,
            SphereDeformation deformation,
            float intensity,
            float[] center,
            float[] direction,
            float length,
            float radius,
            int rings,
            int segments,
            VertexPattern pattern,
            VisibilityMask visibility,
            float visibleTStart,
            float visibleTEnd,
            float flowAlpha,
            EnergyTravel travelMode,
            float travelPhase,
            int chaseCount,
            float chaseWidth,
            GeoDeformationStrategy fieldDeformation,
            float[] fieldCenter,
            float fieldDeformIntensity,
            float outerRadius,
            int baseColor) {
        
        if (builder == null) {
            return;
        }
        
        // Default to DROPLET if null
        if (deformation == null) {
            deformation = SphereDeformation.DROPLET;
        }
        
        // Compute perpendicular frame for orientation
        float[] right = new float[3];
        float[] up = new float[3];
        computePerpendicularFrame(direction, right, up);
        
        // Finalize parameters
        final float finalVisibleTStart = visibleTStart;
        final float finalVisibleTEnd = visibleTEnd;
        final float finalFlowAlpha = flowAlpha;
        final float finalChaseWidth = Math.max(0.05f, chaseWidth);
        final int finalChaseCount = Math.max(1, chaseCount);
        final boolean hasTravelMode = travelMode != null && travelMode != EnergyTravel.NONE;
        final boolean hasFieldDeformation = fieldDeformation != null && fieldDeformation.isActive();
        final SphereDeformation finalDeformation = deformation;
        
        // Generate rings
        for (int i = 0; i < rings; i++) {
            float theta0 = PI * i / rings;
            float theta1 = PI * (i + 1) / rings;
            
            // t-value for this ring (0 at tip, 1 at base)
            float t0 = 1.0f - (theta0 / PI);
            float t1 = 1.0f - (theta1 / PI);
            
            // Compute alpha for each ring
            float alpha0 = computeVertexAlpha(t0, finalVisibleTStart, finalVisibleTEnd, 
                                              finalFlowAlpha, hasTravelMode, travelMode, 
                                              travelPhase, finalChaseCount, finalChaseWidth);
            float alpha1 = computeVertexAlpha(t1, finalVisibleTStart, finalVisibleTEnd, 
                                              finalFlowAlpha, hasTravelMode, travelMode, 
                                              travelPhase, finalChaseCount, finalChaseWidth);
            
            // Skip fully transparent rings
            if (alpha0 < 0.001f && alpha1 < 0.001f) {
                continue;
            }
            
            int color0 = applyAlpha(baseColor, alpha0);
            int color1 = applyAlpha(baseColor, alpha1);
            
            // Generate segments around this ring
            for (int j = 0; j < segments; j++) {
                float phi0 = TWO_PI * j / segments;
                float phi1 = TWO_PI * (j + 1) / segments;
                
                // Use SphereDeformation.computeVertex() for proper shape!
                // This gives us position relative to origin with Y as axis
                float[] local00 = finalDeformation.computeVertex(theta0, phi0, radius, intensity, length);
                float[] local01 = finalDeformation.computeVertex(theta0, phi1, radius, intensity, length);
                float[] local10 = finalDeformation.computeVertex(theta1, phi0, radius, intensity, length);
                float[] local11 = finalDeformation.computeVertex(theta1, phi1, radius, intensity, length);
                
                // Transform from local (Y-up) to world (direction-aligned)
                float[] v00 = transformVertex(local00, center, direction, right, up);
                float[] v01 = transformVertex(local01, center, direction, right, up);
                float[] v10 = transformVertex(local10, center, direction, right, up);
                float[] v11 = transformVertex(local11, center, direction, right, up);
                
                // Apply field deformation if present
                if (hasFieldDeformation) {
                    v00 = fieldDeformation.deform(v00, fieldCenter, fieldDeformIntensity, outerRadius);
                    v01 = fieldDeformation.deform(v01, fieldCenter, fieldDeformIntensity, outerRadius);
                    v10 = fieldDeformation.deform(v10, fieldCenter, fieldDeformIntensity, outerRadius);
                    v11 = fieldDeformation.deform(v11, fieldCenter, fieldDeformIntensity, outerRadius);
                }
                
                // Compute normals (pointing outward)
                float[] n00 = computeNormal(direction, right, up, phi0, theta0);
                float[] n01 = computeNormal(direction, right, up, phi1, theta0);
                float[] n10 = computeNormal(direction, right, up, phi0, theta1);
                float[] n11 = computeNormal(direction, right, up, phi1, theta1);
                
                // Add vertices
                int idx00 = builder.vertex(v00[0], v00[1], v00[2], n00[0], n00[1], n00[2], 0f, t0, colorAlpha(color0));
                int idx01 = builder.vertex(v01[0], v01[1], v01[2], n01[0], n01[1], n01[2], 1f, t0, colorAlpha(color0));
                int idx10 = builder.vertex(v10[0], v10[1], v10[2], n10[0], n10[1], n10[2], 0f, t1, colorAlpha(color1));
                int idx11 = builder.vertex(v11[0], v11[1], v11[2], n11[0], n11[1], n11[2], 1f, t1, colorAlpha(color1));
                
                // Emit quad using pattern
                int[] quadCell = {idx00, idx01, idx10, idx11};
                builder.emitCellFromPattern(quadCell, pattern);
            }
        }
    }
    
    // ──────────────────── Vertex Transform ────────────────────
    
    /**
     * Transforms a local vertex (Y-up) to world space aligned with direction.
     */
    private static float[] transformVertex(float[] local, float[] center, 
                                           float[] dir, float[] right, float[] up) {
        // local.y is along the shape axis -> maps to dir
        // local.x maps to right
        // local.z maps to up
        return new float[] {
            center[0] + dir[0] * local[1] + right[0] * local[0] + up[0] * local[2],
            center[1] + dir[1] * local[1] + right[1] * local[0] + up[1] * local[2],
            center[2] + dir[2] * local[1] + right[2] * local[0] + up[2] * local[2]
        };
    }
    
    // ──────────────────── Alpha Computation ────────────────────
    
    /**
     * Compute per-vertex alpha based on t-position, visibility clip, and travel mode.
     */
    private static float computeVertexAlpha(
            float t, float visibleTStart, float visibleTEnd, float flowAlpha,
            boolean hasTravelMode, EnergyTravel travelMode, float travelPhase,
            int chaseCount, float chaseWidth) {
        
        float alpha = flowAlpha;
        
        // Apply visibility clipping
        if (visibleTStart > 0.0f || visibleTEnd < 1.0f) {
            if (t < visibleTStart) {
                float fadeRange = 0.1f;
                if (t < visibleTStart - fadeRange) {
                    alpha = 0.0f;
                } else {
                    alpha *= (t - (visibleTStart - fadeRange)) / fadeRange;
                }
            } else if (t > visibleTEnd) {
                float fadeRange = 0.1f;
                if (t > visibleTEnd + fadeRange) {
                    alpha = 0.0f;
                } else {
                    alpha *= 1.0f - (t - visibleTEnd) / fadeRange;
                }
            }
        }
        
        // Apply EnergyTravel alpha modulation
        if (hasTravelMode && alpha > 0.001f) {
            float travelAlpha = FlowTravelStage.computeTravelAlpha(t, travelMode, travelPhase, chaseCount, chaseWidth);
            alpha *= travelAlpha;
        }
        
        return alpha;
    }
    
    // ──────────────────── Helpers ────────────────────
    
    private static void computePerpendicularFrame(float[] dir, float[] outRight, float[] outUp) {
        float refX, refY, refZ;
        if (Math.abs(dir[1]) > 0.9f) {
            refX = 1; refY = 0; refZ = 0;
        } else {
            refX = 0; refY = 1; refZ = 0;
        }
        
        // right = dir × ref
        float rx = dir[1] * refZ - dir[2] * refY;
        float ry = dir[2] * refX - dir[0] * refZ;
        float rz = dir[0] * refY - dir[1] * refX;
        float rmag = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rmag > 0.0001f) {
            outRight[0] = rx / rmag;
            outRight[1] = ry / rmag;
            outRight[2] = rz / rmag;
        }
        
        // up = right × dir
        outUp[0] = outRight[1] * dir[2] - outRight[2] * dir[1];
        outUp[1] = outRight[2] * dir[0] - outRight[0] * dir[2];
        outUp[2] = outRight[0] * dir[1] - outRight[1] * dir[0];
    }
    
    private static int applyAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }
    
    private static float[] computeNormal(float[] dir, float[] right, float[] up, float phi, float theta) {
        float cosPhi = (float) Math.cos(phi);
        float sinPhi = (float) Math.sin(phi);
        float cosTheta = (float) Math.cos(theta);
        float sinTheta = (float) Math.sin(theta);
        
        float radialWeight = sinTheta;
        float axialWeight = cosTheta;
        
        float nx = (right[0] * cosPhi + up[0] * sinPhi) * radialWeight + dir[0] * axialWeight;
        float ny = (right[1] * cosPhi + up[1] * sinPhi) * radialWeight + dir[1] * axialWeight;
        float nz = (right[2] * cosPhi + up[2] * sinPhi) * radialWeight + dir[2] * axialWeight;
        
        float mag = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (mag > 0.0001f) {
            return new float[] { nx / mag, ny / mag, nz / mag };
        }
        return new float[] { 0, 1, 0 };
    }
    
    private static float colorAlpha(int color) {
        return ((color >> 24) & 0xFF) / 255f;
    }
}
