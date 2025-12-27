package net.cyberpunk042.client.visual.mesh.ray;

import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.visual.shape.RayLineShape;
import net.cyberpunk042.visual.shape.RaysShape;

/**
 * Tessellates LINE type rays as flat ribbon/line geometry.
 * 
 * <p>This is the default ray tessellator, producing the classic line-segment
 * appearance. Supports:
 * <ul>
 *   <li>Simple line (2 vertices)</li>
 *   <li>Segmented/dashed lines</li>
 *   <li>Shaped lines (wavy, corkscrew, etc.)</li>
 *   <li>Combined (segmented + shaped)</li>
 * </ul>
 * 
 * @see RayTypeTessellator
 * @see RayContext
 * @see RayGeometryUtils
 */
public class RayLineTessellator implements RayTypeTessellator {
    
    public static final RayLineTessellator INSTANCE = new RayLineTessellator();
    
    @Override
    public void tessellate(MeshBuilder builder, RaysShape shape, RayContext context) {
        // Determine which tessellation path to use
        boolean isSegmented = shape.isSegmented();
        boolean needsMultiSegment = context.needsMultiSegment();
        
        if (isSegmented && needsMultiSegment) {
            // Combined: segmented + shaped
            tessellateSegmentedShapedRay(builder, shape, context);
        } else if (isSegmented) {
            // Simple dashed ray
            tessellateSegmentedRay(builder, context, shape.segments(), shape.segmentGap());
        } else if (needsMultiSegment) {
            // Multi-segment for complex shapes/curvature/wave
            tessellateShapedRay(builder, shape, context);
        } else {
            // Simple line (2 vertices)
            tessellateSimpleLine(builder, context);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Simple Line
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Tessellates a simple 2-vertex line.
     */
    private void tessellateSimpleLine(MeshBuilder builder, RayContext context) {
        float[] start = context.start();
        float[] end = context.end();
        float[] dir = context.direction();
        float rayLength = context.length();
        
        // Get field boundaries for geometric clipping
        float innerRadius = context.innerRadius();
        float outerRadius = context.outerRadius();
        
        // Compute radial distances of start and end from center (0,0,0)
        // For RADIAL arrangement, the ray direction points outward from center
        float startDist = (float) Math.sqrt(start[0]*start[0] + start[1]*start[1] + start[2]*start[2]);
        float endDist = (float) Math.sqrt(end[0]*end[0] + end[1]*end[1] + end[2]*end[2]);
        
        // Determine which direction the ray points (inward or outward)
        boolean rayPointsOutward = endDist > startDist;
        
        // Compute geometric clipping based on actual positions vs boundaries
        float tStart = 0.0f;
        float tEnd = 1.0f;
        
        if (rayLength > 0.001f) {
            if (rayPointsOutward) {
                // Ray points outward: clip END if it extends past outerRadius
                if (endDist > outerRadius) {
                    // How much of the ray extends past the boundary?
                    float overshoot = endDist - outerRadius;
                    float clipAmount = overshoot / rayLength;
                    tEnd = Math.max(0.0f, 1.0f - clipAmount);
                }
                // Clip START if it's below innerRadius
                if (startDist < innerRadius) {
                    float undershoot = innerRadius - startDist;
                    float clipAmount = undershoot / rayLength;
                    tStart = Math.min(1.0f, clipAmount);
                }
            } else {
                // Ray points inward: clip START if it extends past outerRadius
                if (startDist > outerRadius) {
                    float overshoot = startDist - outerRadius;
                    float clipAmount = overshoot / rayLength;
                    tStart = Math.min(1.0f, clipAmount);
                }
                // Clip END if it's below innerRadius
                if (endDist < innerRadius) {
                    float undershoot = innerRadius - endDist;
                    float clipAmount = undershoot / rayLength;
                    tEnd = Math.max(0.0f, 1.0f - clipAmount);
                }
            }
        }
        
        // Skip if line is completely clipped
        if (tStart >= tEnd) {
            return;
        }
        
        // Compute actual start/end positions
        float[] actualStart;
        float[] actualEnd;
        
        if (tStart > 0.001f || tEnd < 0.999f) {
            // Clipping active - interpolate positions
            actualStart = RayGeometryUtils.interpolate(start, end, tStart);
            actualEnd = RayGeometryUtils.interpolate(start, end, tEnd);
        } else {
            // No clipping - use originals
            actualStart = start;
            actualEnd = end;
        }
        
        // u = t value (0 at start, 1 at end) for alpha interpolation
        // Normal stores the ray axis direction for Twist animation
        int v0 = builder.vertex(actualStart[0], actualStart[1], actualStart[2], dir[0], dir[1], dir[2], tStart, 0);
        int v1 = builder.vertex(actualEnd[0], actualEnd[1], actualEnd[2], dir[0], dir[1], dir[2], tEnd, 0);
        builder.line(v0, v1);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Segmented (Dashed)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Tessellates a segmented (dashed) ray.
     */
    private void tessellateSegmentedRay(MeshBuilder builder, RayContext context, 
                                        int segments, float segmentGap) {
        float[] start = context.start();
        float[] end = context.end();
        float[] dir = context.direction();
        
        // Get field boundaries for geometric clipping
        float innerRadius = context.innerRadius();
        float outerRadius = context.outerRadius();
        
        // Calculate segment and gap proportions
        float totalParts = segments + (segments - 1) * segmentGap;
        float segmentLength = 1.0f / totalParts;
        float gapLength = segmentLength * segmentGap;
        
        float t = 0;
        for (int seg = 0; seg < segments; seg++) {
            float tStart = t;
            float tEnd = t + segmentLength;
            
            // Interpolate positions for this segment
            float[] segStart = RayGeometryUtils.interpolate(start, end, tStart);
            float[] segEnd = RayGeometryUtils.interpolate(start, end, tEnd);
            
            // Compute radial distances
            float segStartDist = (float) Math.sqrt(segStart[0]*segStart[0] + segStart[1]*segStart[1] + segStart[2]*segStart[2]);
            float segEndDist = (float) Math.sqrt(segEnd[0]*segEnd[0] + segEnd[1]*segEnd[1] + segEnd[2]*segEnd[2]);
            
            // Skip segments completely outside the boundaries
            float minDist = Math.min(segStartDist, segEndDist);
            float maxDist = Math.max(segStartDist, segEndDist);
            if (maxDist < innerRadius || minDist > outerRadius) {
                t = tEnd + gapLength;
                continue;
            }
            
            // Clip segment ends to boundaries
            float[] clippedStart = segStart;
            float[] clippedEnd = segEnd;
            float clippedTStart = tStart;
            float clippedTEnd = tEnd;
            
            float segLen = (float) Math.sqrt(
                (segEnd[0]-segStart[0])*(segEnd[0]-segStart[0]) +
                (segEnd[1]-segStart[1])*(segEnd[1]-segStart[1]) +
                (segEnd[2]-segStart[2])*(segEnd[2]-segStart[2]));
            
            if (segLen > 0.001f) {
                // Clip start if below inner radius
                if (segStartDist < innerRadius && segEndDist > segStartDist) {
                    float clipT = (innerRadius - segStartDist) / (segEndDist - segStartDist);
                    clippedStart = RayGeometryUtils.interpolate(segStart, segEnd, clipT);
                    clippedTStart = tStart + clipT * (tEnd - tStart);
                }
                // Clip end if above outer radius
                if (segEndDist > outerRadius && segEndDist > segStartDist) {
                    float clipT = (outerRadius - segStartDist) / (segEndDist - segStartDist);
                    clippedEnd = RayGeometryUtils.interpolate(segStart, segEnd, clipT);
                    clippedTEnd = tStart + clipT * (tEnd - tStart);
                }
            }
            
            // Emit line segment
            int v0 = builder.vertex(clippedStart[0], clippedStart[1], clippedStart[2], dir[0], dir[1], dir[2], clippedTStart, 0);
            int v1 = builder.vertex(clippedEnd[0], clippedEnd[1], clippedEnd[2], dir[0], dir[1], dir[2], clippedTEnd, 0);
            builder.line(v0, v1);
            
            t = tEnd + gapLength;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Shaped Ray (Multi-Segment)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Tessellates a ray with multiple segments for complex shapes and curvature.
     */
    private void tessellateShapedRay(MeshBuilder builder, RaysShape shape, RayContext context) {
        float[] start = context.start();
        float[] end = context.end();
        float[] dir = context.direction();
        int segments = context.shapeSegments();
        
        if (context.length() < 0.0001f) {
            // Degenerate ray
            builder.vertex(start[0], start[1], start[2], 0, 0, 0, 0.0f, 0);
            return;
        }
        
        // Get field boundaries for geometric clipping
        float innerRadius = context.innerRadius();
        float outerRadius = context.outerRadius();
        
        // Compute perpendicular frame using shared utility
        float[] right = new float[3];
        float[] up = new float[3];
        RayGeometryUtils.computePerpendicularFrame(dir[0], dir[1], dir[2], right, up);
        
        RayLineShape lineShape = context.lineShape();
        float shapeAmplitude = context.lineShapeAmplitude();
        float shapeFrequency = context.lineShapeFrequency();
        float curvatureIntensity = context.curvatureIntensity();
        
        // DOUBLE_HELIX: emit TWO intertwined helixes
        int helixCount = (lineShape == RayLineShape.DOUBLE_HELIX) ? 2 : 1;
        
        for (int helix = 0; helix < helixCount; helix++) {
            float phaseOffset = helix * (float) Math.PI;
            
            int[] vertexIndices = new int[segments + 1];
            float[] radialDists = new float[segments + 1];
            
            for (int i = 0; i <= segments; i++) {
                float t = (float) i / segments;
                
                // Apply curvature using shared utility
                float[] curvedPos = RayGeometryUtils.computeCurvedPosition(
                    start, end, t, context.curvature(), curvatureIntensity);
                float px = curvedPos[0];
                float py = curvedPos[1];
                float pz = curvedPos[2];
                
                // Apply line shape offset using shared utility
                float[] offset = RayGeometryUtils.computeLineShapeOffsetWithPhase(
                    lineShape, t, shapeAmplitude, shapeFrequency, right, up, phaseOffset);
                px += offset[0];
                py += offset[1];
                pz += offset[2];
                
                // Apply wave deformation if active
                if (context.hasWave() && context.wave() != null) {
                    float[] deformed = net.cyberpunk042.client.visual.animation.WaveDeformer.applyLinear(
                        px, py, pz, context.wave(), context.time()
                    );
                    px = deformed[0];
                    py = deformed[1];
                    pz = deformed[2];
                }
                
                // Store radial distance for geometric clipping
                radialDists[i] = (float) Math.sqrt(px*px + py*py + pz*pz);
                
                vertexIndices[i] = builder.vertex(px, py, pz, dir[0], dir[1], dir[2], t, 0);
            }
            
            // Connect with lines (only emit segments within field boundaries)
            for (int i = 0; i < segments; i++) {
                float segStartDist = radialDists[i];
                float segEndDist = radialDists[i + 1];
                
                // Skip segments completely outside the boundaries
                float minDist = Math.min(segStartDist, segEndDist);
                float maxDist = Math.max(segStartDist, segEndDist);
                if (maxDist < innerRadius || minDist > outerRadius) {
                    continue;
                }
                
                builder.line(vertexIndices[i], vertexIndices[i + 1]);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Segmented + Shaped (Combined)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Tessellates a ray that is BOTH segmented AND has complex shape.
     */
    private void tessellateSegmentedShapedRay(MeshBuilder builder, RaysShape shape, RayContext context) {
        // For now, delegate to shaped ray (simplified)
        // TODO: Implement proper segmented + shaped combination
        tessellateShapedRay(builder, shape, context);
    }
}
