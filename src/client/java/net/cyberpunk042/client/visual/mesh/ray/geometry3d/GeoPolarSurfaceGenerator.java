package net.cyberpunk042.client.visual.mesh.ray.geometry3d;

import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.client.visual.mesh.ray.flow.FlowTravelStage;
import net.cyberpunk042.visual.energy.EnergyTravel;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Generates 3D polar surfaces for ray shapes.
 * 
 * <p>Based on Ray3DGeometryUtils.generateDropletWithTravel, this generates polar
 * surfaces with full support for:
 * <ul>
 *   <li>GeoRadiusProfile-based shape definition</li>
 *   <li>Flow animation (clip start/end, base alpha)</li>
 *   <li>Travel mode effects (CHASE, SCROLL, COMET, etc.)</li>
 *   <li>Pattern and visibility mask support</li>
 * </ul>
 * </p>
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.Ray3DGeometryUtils#generateDropletWithTravel
 */
public final class GeoPolarSurfaceGenerator {
    
    private static final float PI = (float) Math.PI;
    private static final float TWO_PI = 2f * PI;
    
    private GeoPolarSurfaceGenerator() {}
    
    /**
     * Generate a polar surface mesh (simple version).
     * 
     * @param builder MeshBuilder to add geometry to
     * @param profile Radius profile function
     * @param center Center position [x, y, z]
     * @param direction Direction (axis) of the shape [x, y, z]
     * @param length Length along the axis
     * @param radius Maximum radius
     * @param rings Number of rings (θ divisions)
     * @param segments Number of segments per ring (φ divisions)
     * @param color Vertex color (ARGB)
     */
    public static void generate(
            MeshBuilder builder,
            GeoRadiusProfile profile,
            float[] center,
            float[] direction,
            float length,
            float radius,
            int rings,
            int segments,
            int color) {
        
        generateFull(builder, profile, center, direction, length, radius, 
                    rings, segments, null, null, 0f, 1f, 1f,
                    null, 0f, 1, 0.3f, color);
    }
    
    /**
     * Generate a polar surface with full flow and travel support.
     * 
     * <p>Based on Ray3DGeometryUtils.generateDropletWithTravel lines 152-225.</p>
     * 
     * @param builder MeshBuilder to add geometry to
     * @param profile Radius profile function
     * @param center Center position
     * @param direction Direction (axis) of the shape
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
     * @param baseColor Base ARGB color
     */
    public static void generateFull(
            MeshBuilder builder,
            GeoRadiusProfile profile,
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
            int baseColor) {
        
        if (profile == null || builder == null) {
            return;
        }
        
        // Compute perpendicular frame
        float[] right = new float[3];
        float[] up = new float[3];
        computePerpendicularFrame(direction, right, up);
        
        // Validate parameters
        final float finalVisibleTStart = visibleTStart;
        final float finalVisibleTEnd = visibleTEnd;
        final float finalFlowAlpha = flowAlpha;
        final float finalChaseWidth = Math.max(0.05f, chaseWidth);
        final int finalChaseCount = Math.max(1, chaseCount);
        final boolean hasTravelMode = travelMode != null && travelMode != EnergyTravel.NONE;
        
        // Generate rings
        for (int i = 0; i < rings; i++) {
            float theta0 = PI * i / rings;
            float theta1 = PI * (i + 1) / rings;
            
            // t-value for this ring (0 at tip, 1 at base)
            // From Ray3DGeometryUtils line 188: t = 1.0f - (theta / PI)
            float t0 = 1.0f - (theta0 / PI);
            float t1 = 1.0f - (theta1 / PI);
            
            float r0 = profile.radius(theta0) * radius;
            float r1 = profile.radius(theta1) * radius;
            
            // Position along axis
            float z0 = (1f - (float) Math.cos(theta0)) * length / 2f;
            float z1 = (1f - (float) Math.cos(theta1)) * length / 2f;
            
            // Compute alpha for each ring (from lines 191-216)
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
                
                // Four corners of quad
                float[] v00 = computeVertex(center, direction, right, up, r0, z0, phi0);
                float[] v01 = computeVertex(center, direction, right, up, r0, z0, phi1);
                float[] v10 = computeVertex(center, direction, right, up, r1, z1, phi0);
                float[] v11 = computeVertex(center, direction, right, up, r1, z1, phi1);
                
                // Compute normals (pointing outward from center)
                float[] n00 = computeNormal(direction, right, up, phi0, theta0);
                float[] n01 = computeNormal(direction, right, up, phi1, theta0);
                float[] n10 = computeNormal(direction, right, up, phi0, theta1);
                float[] n11 = computeNormal(direction, right, up, phi1, theta1);
                
                // Add vertices with colors embedded
                int idx00 = builder.vertex(v00[0], v00[1], v00[2], n00[0], n00[1], n00[2], 0f, t0, colorAlpha(color0));
                int idx01 = builder.vertex(v01[0], v01[1], v01[2], n01[0], n01[1], n01[2], 1f, t0, colorAlpha(color0));
                int idx10 = builder.vertex(v10[0], v10[1], v10[2], n10[0], n10[1], n10[2], 0f, t1, colorAlpha(color1));
                int idx11 = builder.vertex(v11[0], v11[1], v11[2], n11[0], n11[1], n11[2], 1f, t1, colorAlpha(color1));
                
                // Emit two triangles using indices
                builder.triangle(idx00, idx01, idx11);
                builder.triangle(idx00, idx11, idx10);
            }
        }
    }
    
    // ──────────────────── Alpha Computation ────────────────────
    
    /**
     * Compute per-vertex alpha based on t-position, visibility clip, and travel mode.
     * From Ray3DGeometryUtils.generateDropletWithTravel lines 191-216.
     */
    private static float computeVertexAlpha(
            float t, float visibleTStart, float visibleTEnd, float flowAlpha,
            boolean hasTravelMode, EnergyTravel travelMode, float travelPhase,
            int chaseCount, float chaseWidth) {
        
        float alpha = flowAlpha;
        
        // Apply visibility clipping (from lines 194-210)
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
        
        // Apply EnergyTravel alpha modulation (from lines 213-216)
        if (hasTravelMode && alpha > 0.001f) {
            float travelAlpha = FlowTravelStage.computeTravelAlpha(t, travelMode, travelPhase, chaseCount, chaseWidth);
            alpha *= travelAlpha;
        }
        
        return alpha;
    }
    
    // ──────────────────── Vertex Helpers ────────────────────
    
    private static float[] computeVertex(
            float[] center, float[] dir, float[] right, float[] up,
            float radius, float axisOffset, float phi) {
        
        float cosPhi = (float) Math.cos(phi);
        float sinPhi = (float) Math.sin(phi);
        
        return new float[] {
            center[0] + dir[0] * axisOffset + (right[0] * cosPhi + up[0] * sinPhi) * radius,
            center[1] + dir[1] * axisOffset + (right[1] * cosPhi + up[1] * sinPhi) * radius,
            center[2] + dir[2] * axisOffset + (right[2] * cosPhi + up[2] * sinPhi) * radius
        };
    }
    
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
        // Normal points outward from the surface center
        float cosPhi = (float) Math.cos(phi);
        float sinPhi = (float) Math.sin(phi);
        float cosTheta = (float) Math.cos(theta);
        float sinTheta = (float) Math.sin(theta);
        
        // Blend between radial (perpendicular) and axial direction based on theta
        float radialWeight = sinTheta;
        float axialWeight = cosTheta;
        
        float nx = (right[0] * cosPhi + up[0] * sinPhi) * radialWeight + dir[0] * axialWeight;
        float ny = (right[1] * cosPhi + up[1] * sinPhi) * radialWeight + dir[1] * axialWeight;
        float nz = (right[2] * cosPhi + up[2] * sinPhi) * radialWeight + dir[2] * axialWeight;
        
        // Normalize
        float mag = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (mag > 0.0001f) {
            return new float[] { nx / mag, ny / mag, nz / mag };
        }
        return new float[] { 0, 1, 0 };
    }
    
    private static float colorAlpha(int color) {
        return ((color >> 24) & 0xFF) / 255f;
    }
    
    /**
     * Generate a polar surface with deformation applied.
     * 
     * @param deformation Deformation strategy to apply
     * @param fieldCenter Center of the deformation field
     * @param deformIntensity Intensity of deformation (0-1+)
     * @param outerRadius Outer radius of the field (for normalization)
     */
    public static void generateWithDeformation(
            MeshBuilder builder,
            GeoRadiusProfile profile,
            float[] center,
            float[] direction,
            float length,
            float radius,
            int rings,
            int segments,
            net.cyberpunk042.visual.pattern.VertexPattern pattern,
            VisibilityMask visibility,
            float visibleTStart,
            float visibleTEnd,
            float flowAlpha,
            EnergyTravel travelMode,
            float travelPhase,
            int chaseCount,
            float chaseWidth,
            GeoDeformationStrategy deformation,
            float[] fieldCenter,
            float deformIntensity,
            float outerRadius,
            int baseColor) {
        
        if (profile == null || builder == null) {
            return;
        }
        
        // Compute perpendicular frame
        float[] right = new float[3];
        float[] up = new float[3];
        computePerpendicularFrame(direction, right, up);
        
        // Validate parameters
        final float finalVisibleTStart = visibleTStart;
        final float finalVisibleTEnd = visibleTEnd;
        final float finalFlowAlpha = flowAlpha;
        final float finalChaseWidth = Math.max(0.05f, chaseWidth);
        final int finalChaseCount = Math.max(1, chaseCount);
        final boolean hasTravelMode = travelMode != null && travelMode != EnergyTravel.NONE;
        
        // Generate rings with deformation
        for (int i = 0; i < rings; i++) {
            float theta0 = PI * i / rings;
            float theta1 = PI * (i + 1) / rings;
            
            float t0 = 1.0f - (theta0 / PI);
            float t1 = 1.0f - (theta1 / PI);
            
            float r0 = profile.radius(theta0) * radius;
            float r1 = profile.radius(theta1) * radius;
            
            float z0 = (1f - (float) Math.cos(theta0)) * length / 2f;
            float z1 = (1f - (float) Math.cos(theta1)) * length / 2f;
            
            float alpha0 = computeVertexAlpha(t0, finalVisibleTStart, finalVisibleTEnd, 
                                              finalFlowAlpha, hasTravelMode, travelMode, 
                                              travelPhase, finalChaseCount, finalChaseWidth);
            float alpha1 = computeVertexAlpha(t1, finalVisibleTStart, finalVisibleTEnd, 
                                              finalFlowAlpha, hasTravelMode, travelMode, 
                                              travelPhase, finalChaseCount, finalChaseWidth);
            
            if (alpha0 < 0.001f && alpha1 < 0.001f) {
                continue;
            }
            
            int color0 = applyAlpha(baseColor, alpha0);
            int color1 = applyAlpha(baseColor, alpha1);
            
            for (int j = 0; j < segments; j++) {
                float phi0 = TWO_PI * j / segments;
                float phi1 = TWO_PI * (j + 1) / segments;
                
                // Compute base vertices
                float[] v00 = computeVertex(center, direction, right, up, r0, z0, phi0);
                float[] v01 = computeVertex(center, direction, right, up, r0, z0, phi1);
                float[] v10 = computeVertex(center, direction, right, up, r1, z1, phi0);
                float[] v11 = computeVertex(center, direction, right, up, r1, z1, phi1);
                
                // Apply deformation
                v00 = deformation.deform(v00, fieldCenter, deformIntensity, outerRadius);
                v01 = deformation.deform(v01, fieldCenter, deformIntensity, outerRadius);
                v10 = deformation.deform(v10, fieldCenter, deformIntensity, outerRadius);
                v11 = deformation.deform(v11, fieldCenter, deformIntensity, outerRadius);
                
                float[] n00 = computeNormal(direction, right, up, phi0, theta0);
                float[] n01 = computeNormal(direction, right, up, phi1, theta0);
                float[] n10 = computeNormal(direction, right, up, phi0, theta1);
                float[] n11 = computeNormal(direction, right, up, phi1, theta1);
                
                int idx00 = builder.vertex(v00[0], v00[1], v00[2], n00[0], n00[1], n00[2], 0f, t0, colorAlpha(color0));
                int idx01 = builder.vertex(v01[0], v01[1], v01[2], n01[0], n01[1], n01[2], 1f, t0, colorAlpha(color0));
                int idx10 = builder.vertex(v10[0], v10[1], v10[2], n10[0], n10[1], n10[2], 0f, t1, colorAlpha(color1));
                int idx11 = builder.vertex(v11[0], v11[1], v11[2], n11[0], n11[1], n11[2], 1f, t1, colorAlpha(color1));
                
                builder.triangle(idx00, idx01, idx11);
                builder.triangle(idx00, idx11, idx10);
            }
        }
    }
}

