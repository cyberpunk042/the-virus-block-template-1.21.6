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
     * 
     * <p>Supports three edge transition modes:
     * <ul>
     *   <li>CLIP: Geometric clipping at field boundaries (one-sided, position-based)</li>
     *   <li>FADE: Alpha modulation based on edge proximity (no geometric clipping)</li>
     *   <li>SCALE: Symmetric shrinking from both ends toward center (no geometric clipping)</li>
     * </ul>
     */
    private void tessellateSimpleLine(MeshBuilder builder, RayContext context) {
        float[] start = context.start();
        float[] end = context.end();
        float[] dir = context.direction();
        float rayLength = context.length();
        float flowAlpha = context.flowAlpha();
        float flowScale = context.flowScale();
        
        // Skip if fully invisible (FADE mode) or fully scaled down (SCALE mode)
        if (flowAlpha < 0.001f || flowScale < 0.001f) {
            return;
        }
        
        // Determine edge transition mode from flow config
        net.cyberpunk042.visual.animation.EdgeTransitionMode edgeMode = 
            net.cyberpunk042.visual.animation.EdgeTransitionMode.CLIP; // Default
        if (context.flowConfig() != null) {
            edgeMode = context.flowConfig().effectiveEdgeTransition();
        }
        
        // Get field boundaries for geometric clipping
        float innerRadius = context.innerRadius();
        float outerRadius = context.outerRadius();
        
        // Compute radial distances of start and end from center (0,0,0)
        float startDist = (float) Math.sqrt(start[0]*start[0] + start[1]*start[1] + start[2]*start[2]);
        float endDist = (float) Math.sqrt(end[0]*end[0] + end[1]*end[1] + end[2]*end[2]);
        
        // Determine which direction the ray points (inward or outward)
        boolean rayPointsOutward = endDist > startDist;
        
        // Start with t-range from context (may be modified by progressive spawn / startFullLength)
        float tStart = context.visibleTStart();
        float tEnd = context.visibleTEnd();
        
        // ========== SCALE MODE: Symmetric shrinking from center ==========
        // When flowScale < 1.0, shrink the line symmetrically from both ends
        if (edgeMode == net.cyberpunk042.visual.animation.EdgeTransitionMode.SCALE && flowScale < 0.999f) {
            float shrinkAmount = (1.0f - flowScale) / 2.0f;
            tStart = shrinkAmount;
            tEnd = 1.0f - shrinkAmount;
        }
        
        // ========== CLIP MODE: Geometric clipping at field boundaries ==========
        // ONLY apply geometric clipping for CLIP mode - NOT for SCALE or FADE
        if (edgeMode == net.cyberpunk042.visual.animation.EdgeTransitionMode.CLIP && rayLength > 0.001f) {
            if (rayPointsOutward) {
                // Ray points outward: clip END if it extends past outerRadius
                if (endDist > outerRadius) {
                    float overshoot = endDist - outerRadius;
                    float clipAmount = overshoot / rayLength;
                    tEnd = Math.min(tEnd, Math.max(0.0f, 1.0f - clipAmount));
                }
                // Clip START if it's below innerRadius
                if (startDist < innerRadius) {
                    float undershoot = innerRadius - startDist;
                    float clipAmount = undershoot / rayLength;
                    tStart = Math.max(tStart, Math.min(1.0f, clipAmount));
                }
            } else {
                // Ray points inward: clip START if it extends past outerRadius
                if (startDist > outerRadius) {
                    float overshoot = startDist - outerRadius;
                    float clipAmount = overshoot / rayLength;
                    tStart = Math.max(tStart, Math.min(1.0f, clipAmount));
                }
                // Clip END if it's below innerRadius
                if (endDist < innerRadius) {
                    float undershoot = innerRadius - endDist;
                    float clipAmount = undershoot / rayLength;
                    tEnd = Math.min(tEnd, Math.max(0.0f, 1.0f - clipAmount));
                }
            }
        }
        
        // Skip if line is completely clipped/scaled down
        if (tStart >= tEnd) {
            return;
        }
        
        // Compute actual start/end positions
        float[] actualStart;
        float[] actualEnd;
        
        if (tStart > 0.001f || tEnd < 0.999f) {
            // Clipping/scaling active - interpolate positions
            actualStart = RayGeometryUtils.interpolate(start, end, tStart);
            actualEnd = RayGeometryUtils.interpolate(start, end, tEnd);
        } else {
            // No clipping/scaling - use originals
            actualStart = start;
            actualEnd = end;
        }
        
        // u = t value for shader-based effects
        // Alpha is set from flowAlpha for FADE edge transition
        int v0 = builder.vertex(actualStart[0], actualStart[1], actualStart[2], dir[0], dir[1], dir[2], tStart, 0, flowAlpha);
        int v1 = builder.vertex(actualEnd[0], actualEnd[1], actualEnd[2], dir[0], dir[1], dir[2], tEnd, 0, flowAlpha);
        builder.line(v0, v1);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Segmented (Dashed)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Tessellates a segmented (dashed) ray.
     * <p>Supports all three edge transition modes.
     */
    private void tessellateSegmentedRay(MeshBuilder builder, RayContext context, 
                                        int segments, float segmentGap) {
        float[] start = context.start();
        float[] end = context.end();
        float[] dir = context.direction();
        float flowAlpha = context.flowAlpha();
        float flowScale = context.flowScale();
        
        // Skip entirely if invisible (FADE mode) or scaled to zero (SCALE mode)
        if (flowAlpha < 0.001f || flowScale < 0.001f) {
            return;
        }
        
        // Determine edge transition mode from flow config
        net.cyberpunk042.visual.animation.EdgeTransitionMode edgeMode = 
            net.cyberpunk042.visual.animation.EdgeTransitionMode.CLIP; // Default
        if (context.flowConfig() != null) {
            edgeMode = context.flowConfig().effectiveEdgeTransition();
        }
        
        // Get field boundaries for geometric clipping
        float innerRadius = context.innerRadius();
        float outerRadius = context.outerRadius();
        
        // Calculate segment and gap proportions
        float totalParts = segments + (segments - 1) * segmentGap;
        float segmentLength = 1.0f / totalParts;
        float gapLength = segmentLength * segmentGap;
        
        // SCALE mode: compute global t-range shrinkage
        float globalTStart = 0.0f;
        float globalTEnd = 1.0f;
        if (edgeMode == net.cyberpunk042.visual.animation.EdgeTransitionMode.SCALE && flowScale < 0.999f) {
            float shrinkAmount = (1.0f - flowScale) / 2.0f;
            globalTStart = shrinkAmount;
            globalTEnd = 1.0f - shrinkAmount;
        }
        
        float t = 0;
        for (int seg = 0; seg < segments; seg++) {
            float tStart = t;
            float tEnd = t + segmentLength;
            
            // SCALE mode: Skip segments completely outside the scaled range
            if (edgeMode == net.cyberpunk042.visual.animation.EdgeTransitionMode.SCALE) {
                if (tEnd < globalTStart || tStart > globalTEnd) {
                    t = tEnd + gapLength;
                    continue;
                }
            }
            
            // Effective t-range for this segment (accounting for SCALE)
            float effectiveTStart = Math.max(tStart, globalTStart);
            float effectiveTEnd = Math.min(tEnd, globalTEnd);
            
            // Interpolate positions for this segment
            float[] segStart = RayGeometryUtils.interpolate(start, end, effectiveTStart);
            float[] segEnd = RayGeometryUtils.interpolate(start, end, effectiveTEnd);
            
            // For CLIP mode: apply geometric clipping
            if (edgeMode == net.cyberpunk042.visual.animation.EdgeTransitionMode.CLIP) {
                // Compute radial distances
                float segStartDist = (float) Math.sqrt(segStart[0]*segStart[0] + segStart[1]*segStart[1] + segStart[2]*segStart[2]);
                float segEndDist = (float) Math.sqrt(segEnd[0]*segEnd[0] + segEnd[1]*segEnd[1] + segEnd[2]*segEnd[2]);
                
                // Skip segments completely outside the CLIP boundaries
                float minDist = Math.min(segStartDist, segEndDist);
                float maxDist = Math.max(segStartDist, segEndDist);
                if (maxDist < innerRadius || minDist > outerRadius) {
                    t = tEnd + gapLength;
                    continue;
                }
                
                // Clip segment ends to boundaries
                float segLen = (float) Math.sqrt(
                    (segEnd[0]-segStart[0])*(segEnd[0]-segStart[0]) +
                    (segEnd[1]-segStart[1])*(segEnd[1]-segStart[1]) +
                    (segEnd[2]-segStart[2])*(segEnd[2]-segStart[2]));
                
                if (segLen > 0.001f) {
                    // Clip start if below inner radius
                    if (segStartDist < innerRadius && segEndDist > segStartDist) {
                        float clipT = (innerRadius - segStartDist) / (segEndDist - segStartDist);
                        segStart = RayGeometryUtils.interpolate(segStart, segEnd, clipT);
                        effectiveTStart = effectiveTStart + clipT * (effectiveTEnd - effectiveTStart);
                    }
                    // Clip end if above outer radius
                    if (segEndDist > outerRadius && segEndDist > segStartDist) {
                        float clipT = (outerRadius - segStartDist) / (segEndDist - segStartDist);
                        segEnd = RayGeometryUtils.interpolate(segStart, segEnd, clipT);
                        effectiveTEnd = effectiveTStart + clipT * (effectiveTEnd - effectiveTStart);
                    }
                }
            }
            
            // Emit line segment with per-vertex alpha (FADE mode uses this)
            int v0 = builder.vertex(segStart[0], segStart[1], segStart[2], dir[0], dir[1], dir[2], effectiveTStart, 0, flowAlpha);
            int v1 = builder.vertex(segEnd[0], segEnd[1], segEnd[2], dir[0], dir[1], dir[2], effectiveTEnd, 0, flowAlpha);
            builder.line(v0, v1);
            
            t = tEnd + gapLength;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Shaped Ray (Multi-Segment)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Tessellates a ray with multiple segments for complex shapes and curvature.
     * <p>Supports all three edge transition modes with position-based edge detection.
     */
    private void tessellateShapedRay(MeshBuilder builder, RaysShape shape, RayContext context) {
        float[] start = context.start();
        float[] end = context.end();
        float[] dir = context.direction();
        int segments = context.shapeSegments();
        float contextFlowAlpha = context.flowAlpha();
        float contextFlowScale = context.flowScale();
        
        if (context.length() < 0.0001f) {
            // Degenerate ray
            builder.vertex(start[0], start[1], start[2], 0, 0, 0, 0.0f, 0, contextFlowAlpha);
            return;
        }
        
        // Skip entirely if invisible (FADE mode) or scaled to zero (SCALE mode)
        if (contextFlowAlpha < 0.001f || contextFlowScale < 0.001f) {
            return;
        }
        
        // Determine edge transition mode from flow config
        net.cyberpunk042.visual.animation.EdgeTransitionMode edgeMode = 
            net.cyberpunk042.visual.animation.EdgeTransitionMode.CLIP; // Default
        net.cyberpunk042.visual.animation.LengthMode lengthMode = null;
        if (context.flowConfig() != null) {
            edgeMode = context.flowConfig().effectiveEdgeTransition();
            lengthMode = context.flowConfig().length();
        }
        
        // Get field boundaries
        float innerRadius = context.innerRadius();
        float outerRadius = context.outerRadius();
        
        // Edge width for position-based transitions (half actual ray length, clamped)
        float edgeWidth = context.length() * 0.5f;
        edgeWidth = Math.max(0.1f, Math.min(edgeWidth, (outerRadius - innerRadius) * 0.15f));
        
        // Check if we need position-based edge detection (curvature active)
        // Skip for CONTINUOUS mode - all rays should be visible for 360° coverage
        boolean hasCurvature = context.curvature() != null 
            && context.curvature() != net.cyberpunk042.visual.shape.RayCurvature.NONE 
            && context.curvatureIntensity() > 0.001f;
        
        boolean isContinuous = context.flowConfig() != null 
            && context.flowConfig().effectiveWaveDistribution() == net.cyberpunk042.visual.animation.WaveDistribution.CONTINUOUS;
        
        // For CONTINUOUS, we skip the edge filtering to maintain 360° coverage
        boolean doEdgeFiltering = hasCurvature && !isContinuous;
        
        // Compute perpendicular frame using shared utility
        float[] right = new float[3];
        float[] up = new float[3];
        RayGeometryUtils.computePerpendicularFrame(dir[0], dir[1], dir[2], right, up);
        
        RayLineShape lineShape = context.lineShape();
        float shapeAmplitude = context.lineShapeAmplitude();
        float shapeFrequency = context.lineShapeFrequency();
        float curvatureIntensity = context.curvatureIntensity();
        
        // Get t-range from context (may be modified by progressive spawn / startFullLength)
        float visibleTStart = context.visibleTStart();
        float visibleTEnd = context.visibleTEnd();
        
        // DOUBLE_HELIX: emit TWO intertwined helixes
        int helixCount = (lineShape == RayLineShape.DOUBLE_HELIX) ? 2 : 1;
        
        for (int helix = 0; helix < helixCount; helix++) {
            float phaseOffset = helix * (float) Math.PI;
            
            int[] vertexIndices = new int[segments + 1];
            float[] radialDists = new float[segments + 1];
            float[] tValues = new float[segments + 1];
            float[] vertexAlphas = new float[segments + 1]; // Per-vertex alpha for FADE
            boolean[] vertexVisible = new boolean[segments + 1]; // Per-vertex visibility for SCALE
            
            for (int i = 0; i <= segments; i++) {
                float t = (float) i / segments;
                tValues[i] = t;
                
                // Apply curvature using shared utility
                // Note: start/end are already translated by flowPositionOffset in RayPositioner
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
                
                // Store radial distance (after all transformations for proper edge detection)
                float radialDist = (float) Math.sqrt(px*px + py*py + pz*pz);
                radialDists[i] = radialDist;
                
                // Compute per-vertex edge factor for curvature (position-based)
                float vertexAlpha = contextFlowAlpha;
                boolean visible = true;
                
                // Progressive spawn: check if t is within visible range (startFullLength=false clips here)
                if (t < visibleTStart || t > visibleTEnd) {
                    visible = false;
                }
                
                if (visible && doEdgeFiltering && lengthMode != null) {
                    // Position-based edge detection for curved rays
                    float edgeFactor = 1.0f; // Default: fully visible
                    
                    if (lengthMode == net.cyberpunk042.visual.animation.LengthMode.RADIATE) {
                        // RADIATE: spawning at inner, despawning at outer
                        if (radialDist < innerRadius) {
                            // Spawning zone
                            float penetration = innerRadius - radialDist;
                            edgeFactor = Math.max(0, Math.min(1, 1.0f - penetration / edgeWidth));
                        } else if (radialDist > outerRadius) {
                            // Despawning zone
                            float penetration = radialDist - outerRadius;
                            edgeFactor = Math.max(0, Math.min(1, 1.0f - penetration / edgeWidth));
                        }
                    } else if (lengthMode == net.cyberpunk042.visual.animation.LengthMode.ABSORB) {
                        // ABSORB: spawning at outer, despawning at inner
                        if (radialDist > outerRadius) {
                            // Spawning zone
                            float penetration = radialDist - outerRadius;
                            edgeFactor = Math.max(0, Math.min(1, 1.0f - penetration / edgeWidth));
                        } else if (radialDist < innerRadius) {
                            // Despawning zone
                            float penetration = innerRadius - radialDist;
                            edgeFactor = Math.max(0, Math.min(1, 1.0f - penetration / edgeWidth));
                        }
                    }
                    
                    // Apply edge factor based on mode
                    switch (edgeMode) {
                        case FADE -> vertexAlpha = edgeFactor;
                        case SCALE -> visible = (edgeFactor > 0.01f);
                        case CLIP -> visible = (radialDist >= innerRadius && radialDist <= outerRadius);
                    }
                }
                
                vertexAlphas[i] = vertexAlpha;
                vertexVisible[i] = visible;
                
                // Create vertex with per-vertex alpha
                vertexIndices[i] = builder.vertex(px, py, pz, dir[0], dir[1], dir[2], t, 0, vertexAlpha);
            }
            
            // Connect with lines (only emit visible segments)
            for (int i = 0; i < segments; i++) {
                // Skip if either endpoint is not visible (for SCALE/CLIP modes)
                if (!vertexVisible[i] || !vertexVisible[i + 1]) {
                    continue;
                }
                
                // For CLIP mode on non-curvature, skip segments outside radial boundaries
                if (!hasCurvature && edgeMode == net.cyberpunk042.visual.animation.EdgeTransitionMode.CLIP) {
                    float segStartDist = radialDists[i];
                    float segEndDist = radialDists[i + 1];
                    float minDist = Math.min(segStartDist, segEndDist);
                    float maxDist = Math.max(segStartDist, segEndDist);
                    if (maxDist < innerRadius || minDist > outerRadius) {
                        continue;
                    }
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
