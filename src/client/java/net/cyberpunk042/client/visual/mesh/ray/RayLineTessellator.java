package net.cyberpunk042.client.visual.mesh.ray;

import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.client.visual.mesh.ray.tessellation.TessEdgeModeFactory;
import net.cyberpunk042.client.visual.mesh.ray.tessellation.TessEdgeResult;
import net.cyberpunk042.visual.shape.RayCurvature;
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
     * <p>Uses RadiativeInteraction + EdgeMode for animation:
     * <ul>
     *   <li>RadiativeInteraction: WHERE the segment is positioned</li>
     *   <li>EdgeMode CLIP: Geometric clipping at edges</li>
     *   <li>EdgeMode FADE: Alpha modulation at edges</li>
     *   <li>EdgeMode SCALE: Width scaling at edges</li>
     * </ul>
     */
    private void tessellateSimpleLine(MeshBuilder builder, RayContext context) {
        // Compute clipRange first (where the segment is)
        TessEdgeResult edgeResult = computeEdgeResult(context, null);
        
        // Skip if completely hidden
        if (!edgeResult.isVisible()) {
            return;
        }
        
        float[] start = context.start();
        float[] end = context.end();
        float[] dir = context.direction();
        
        // Get clipping/scale/alpha from EdgeMode
        float tStart = edgeResult.clipStart();
        float tEnd = edgeResult.clipEnd();
        float scale = edgeResult.scale();
        float alpha = edgeResult.alpha();
        
        // Skip if line is completely clipped
        if (tStart >= tEnd) {
            return;
        }
        
        // Compute actual start/end positions based on clipping
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
        
        // Apply width scaling if SCALE mode is active
        // (For simple lines, scaling affects how the renderer interprets width)
        float effectiveWidth = context.width() * scale;
        
        // u = t value for shader, alpha from EdgeMode
        int v0 = builder.vertex(actualStart[0], actualStart[1], actualStart[2], dir[0], dir[1], dir[2], tStart, 0, alpha);
        int v1 = builder.vertex(actualEnd[0], actualEnd[1], actualEnd[2], dir[0], dir[1], dir[2], tEnd, 0, alpha);
        builder.line(v0, v1);
    }
    
    /**
     * Compute edge result using the proper flow:
     * 1. Get rayPhase from flow animation
     * 2. Get clipRange from RadiativeInteractionFactory
     * 3. Apply EdgeMode via TessEdgeModeFactory
     */
    private TessEdgeResult computeEdgeResult(RayContext context, net.cyberpunk042.visual.shape.RaysShape shape) {
        // Get flow config
        var flowConfig = context.flowConfig();
        
        // Get radiative mode first (needed to determine if animation should play)
        var radiativeMode = shape != null ? shape.effectiveRadiativeInteraction() 
            : net.cyberpunk042.visual.energy.RadiativeInteraction.NONE;
        
        // Phase is now pre-computed in RayPositioner.computeContexts() which handles:
        // - Manual phase (from user slider)
        // - Animated phase (from time * radiativeSpeed)
        // - Multi-copy phase offsets (for CONTINUOUS + waveCount > 1)
        float userPhase = context.effectiveShapeState().phase();
        
        // Check if radiative animation is conceptually active (for wave distribution offset)
        boolean animationPlaying = flowConfig != null 
            && flowConfig.hasRadiative() 
            && radiativeMode.isActive();
        
        // Add wave distribution offset for this ray (ONLY during animation)
        // In manual mode, all rays should have the same phase for consistent user control
        float waveOffset = 0f;
        if (animationPlaying) {
            waveOffset = net.cyberpunk042.client.visual.mesh.ray.flow.FlowPhaseStage.computeRayPhaseOffset(
                shape, context.index(), context.count());
        }
        
        // Use the pre-computed phase + wave offset
        float rayPhase = (userPhase + waveOffset) % 1.0f;
        if (rayPhase < 0) rayPhase += 1.0f;
        
        // Get clipRange from RadiativeInteraction
        float segmentLength = shape != null ? shape.effectiveSegmentLength() : 1.0f;
        boolean startFullLength = shape != null && shape.startFullLength();
        
        var clipRange = net.cyberpunk042.client.visual.mesh.ray.tessellation.RadiativeInteractionFactory.compute(
            radiativeMode, rayPhase, segmentLength, startFullLength);
        
        // Apply EdgeMode
        var edgeMode = context.effectiveShapeState().edgeMode();
        float edgeIntensity = context.effectiveShapeState().edgeIntensity();
        return TessEdgeModeFactory.compute(edgeMode, clipRange, edgeIntensity);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Segmented (Dashed)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Tessellates a segmented (dashed) ray.
     * <p>Uses RadiativeInteraction + EdgeMode for animation.
     * <p>Applies curvature if the ray has field curvature configured.
     */
    private void tessellateSegmentedRay(MeshBuilder builder, RayContext context, 
                                        int segments, float segmentGap) {
        // Compute edge result from RadiativeInteraction + EdgeMode
        TessEdgeResult edgeResult = computeEdgeResult(context, null);
        
        // Skip if completely hidden
        if (!edgeResult.isVisible()) {
            return;
        }
        
        float[] start = context.start();
        float[] end = context.end();
        float[] dir = context.direction();
        
        // Get clipping/scale/alpha from EdgeMode
        float globalTStart = edgeResult.clipStart();
        float globalTEnd = edgeResult.clipEnd();
        float scale = edgeResult.scale();
        float alpha = edgeResult.alpha();
        
        // Check if we need to apply curvature
        RayCurvature curvature = context.curvature();
        float curvatureIntensity = context.curvatureIntensity();
        boolean hasCurvature = curvature != null && curvature != RayCurvature.NONE && curvatureIntensity > 0;
        
        // Calculate segment and gap proportions
        float totalParts = segments + (segments - 1) * segmentGap;
        float segmentLength = 1.0f / totalParts;
        float gapLength = segmentLength * segmentGap;
        
        float t = 0;
        for (int seg = 0; seg < segments; seg++) {
            float tStart = t;
            float tEnd = t + segmentLength;
            
            // Skip segments completely outside the visible range
            if (tEnd < globalTStart || tStart > globalTEnd) {
                t = tEnd + gapLength;
                continue;
            }
            
            // Effective t-range for this segment (accounting for clipping)
            float effectiveTStart = Math.max(tStart, globalTStart);
            float effectiveTEnd = Math.min(tEnd, globalTEnd);
            
            // Compute positions for this segment - with or without curvature
            float[] segStart;
            float[] segEnd;
            if (hasCurvature) {
                // Apply curvature to follow the curved path
                segStart = RayGeometryUtils.computeCurvedPosition(start, end, effectiveTStart, curvature, curvatureIntensity);
                segEnd = RayGeometryUtils.computeCurvedPosition(start, end, effectiveTEnd, curvature, curvatureIntensity);
            } else {
                // Linear interpolation for straight lines
                segStart = RayGeometryUtils.interpolate(start, end, effectiveTStart);
                segEnd = RayGeometryUtils.interpolate(start, end, effectiveTEnd);
            }
            
            // Emit line segment with alpha from Stage/Phase model
            int v0 = builder.vertex(segStart[0], segStart[1], segStart[2], dir[0], dir[1], dir[2], effectiveTStart, 0, alpha);
            int v1 = builder.vertex(segEnd[0], segEnd[1], segEnd[2], dir[0], dir[1], dir[2], effectiveTEnd, 0, alpha);
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
        // Compute edge result from RadiativeInteraction + EdgeMode
        TessEdgeResult edgeResult = computeEdgeResult(context, shape);
        
        // Skip if completely hidden
        if (!edgeResult.isVisible()) {
            return;
        }
        
        float[] start = context.start();
        float[] end = context.end();
        float[] dir = context.direction();
        int segments = context.lineResolution();
        
        if (context.length() < 0.0001f) {
            // Degenerate ray
            builder.vertex(start[0], start[1], start[2], 0, 0, 0, 0.0f, 0, edgeResult.alpha());
            return;
        }
        
        // Get clipping/scale/alpha from EdgeMode
        float visibleTStart = edgeResult.clipStart();
        float visibleTEnd = edgeResult.clipEnd();
        float scale = edgeResult.scale();
        float alpha = edgeResult.alpha();
        
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
            boolean[] vertexVisible = new boolean[segments + 1];
            
            for (int i = 0; i <= segments; i++) {
                float t = (float) i / segments;
                
                // Check if t is within visible range (from Stage/Phase clipping)
                boolean visible = (t >= visibleTStart && t <= visibleTEnd);
                vertexVisible[i] = visible;
                
                if (!visible) {
                    // Still create vertex but mark as hidden
                    vertexIndices[i] = builder.vertex(0, 0, 0, 0, 0, 0, t, 0, 0);
                    continue;
                }
                
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
                
                // Apply field deformation if active (gravitational lensing effect)
                // This stretches/compresses the vertex radially based on distance from center
                if (context.fieldDeformation().isActive()) {
                    float innerR = context.innerRadius();
                    float outerR = context.outerRadius();
                    
                    // Compute radial distance of this vertex from center
                    float dist = (float) Math.sqrt(px * px + py * py + pz * pz);
                    
                    if (dist > 0.001f && outerR > innerR) {
                        // Normalize distance (0 = at inner, 1 = at outer)
                        float normalizedDist = Math.clamp((dist - innerR) / (outerR - innerR), 0.01f, 1f);
                        
                        // Compute stretch factor for this vertex position
                        float stretch = context.fieldDeformation().computeStretch(
                            normalizedDist, context.fieldDeformationIntensity());
                        
                        // Apply radial stretch (move vertex toward/away from center)
                        float stretchedDist = dist * stretch;
                        float scaleFactor = stretchedDist / dist;
                        px *= scaleFactor;
                        py *= scaleFactor;
                        pz *= scaleFactor;
                    }
                }
                
                // Create vertex with alpha from Stage/Phase model
                vertexIndices[i] = builder.vertex(px, py, pz, dir[0], dir[1], dir[2], t, 0, alpha);
            }
            
            // Connect with lines (only emit visible segments)
            for (int i = 0; i < segments; i++) {
                // Skip if either endpoint is not visible
                if (!vertexVisible[i] || !vertexVisible[i + 1]) {
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
     * Tessellates a ray that is BOTH segmented (dashed) AND has complex shape.
     * Creates dashed segments along the shaped/curved path.
     */
    private void tessellateSegmentedShapedRay(MeshBuilder builder, RaysShape shape, RayContext context) {
        TessEdgeResult edgeResult = computeEdgeResult(context, shape);
        if (!edgeResult.isVisible()) {
            return;
        }
        
        float[] start = context.start();
        float[] end = context.end();
        float[] dir = context.direction();
        int segments = shape.segments();
        float segmentGap = shape.segmentGap();
        
        // Get clipping/scale/alpha from EdgeMode
        float globalTStart = edgeResult.clipStart();
        float globalTEnd = edgeResult.clipEnd();
        float alpha = edgeResult.alpha();
        
        // Calculate segment and gap proportions
        float totalParts = segments + (segments - 1) * segmentGap;
        float segLen = 1.0f / totalParts;
        float gapLen = segLen * segmentGap;
        
        // Compute perpendicular frame
        float[] right = new float[3];
        float[] up = new float[3];
        RayGeometryUtils.computePerpendicularFrame(dir[0], dir[1], dir[2], right, up);
        
        RayLineShape lineShape = context.lineShape();
        float shapeAmplitude = context.lineShapeAmplitude();
        float shapeFrequency = context.lineShapeFrequency();
        float curvatureIntensity = context.curvatureIntensity();
        
        float t = 0;
        for (int seg = 0; seg < segments; seg++) {
            float tStart = t;
            float tEnd = t + segLen;
            
            // Skip segments completely outside the visible range
            if (tEnd < globalTStart || tStart > globalTEnd) {
                t = tEnd + gapLen;
                continue;
            }
            
            // Clip to visible range
            float effectiveTStart = Math.max(tStart, globalTStart);
            float effectiveTEnd = Math.min(tEnd, globalTEnd);
            
            // Compute shaped positions for this segment
            float[] posStart = computeShapedPosition(start, end, effectiveTStart, context, right, up, 
                lineShape, shapeAmplitude, shapeFrequency, curvatureIntensity);
            float[] posEnd = computeShapedPosition(start, end, effectiveTEnd, context, right, up,
                lineShape, shapeAmplitude, shapeFrequency, curvatureIntensity);
            
            int v0 = builder.vertex(posStart[0], posStart[1], posStart[2], dir[0], dir[1], dir[2], effectiveTStart, 0, alpha);
            int v1 = builder.vertex(posEnd[0], posEnd[1], posEnd[2], dir[0], dir[1], dir[2], effectiveTEnd, 0, alpha);
            builder.line(v0, v1);
            
            t = tEnd + gapLen;
        }
    }
    
    /**
     * Compute a shaped position at parametric t.
     */
    private float[] computeShapedPosition(float[] start, float[] end, float t, RayContext context,
            float[] right, float[] up, RayLineShape lineShape, float amplitude, float frequency, float curvatureIntensity) {
        // Apply curvature
        float[] curved = RayGeometryUtils.computeCurvedPosition(start, end, t, context.curvature(), curvatureIntensity);
        
        // Apply line shape offset
        float[] offset = RayGeometryUtils.computeLineShapeOffset(lineShape, t, amplitude, frequency, right, up);
        
        float px = curved[0] + offset[0];
        float py = curved[1] + offset[1];
        float pz = curved[2] + offset[2];
        
        // Apply field deformation if active
        if (context.fieldDeformation().isActive()) {
            float innerR = context.innerRadius();
            float outerR = context.outerRadius();
            float dist = (float) Math.sqrt(px * px + py * py + pz * pz);
            
            if (dist > 0.001f && outerR > innerR) {
                float normalizedDist = Math.clamp((dist - innerR) / (outerR - innerR), 0.01f, 1f);
                float stretch = context.fieldDeformation().computeStretch(
                    normalizedDist, context.fieldDeformationIntensity());
                float scaleFactor = (dist * stretch) / dist;
                px *= scaleFactor;
                py *= scaleFactor;
                pz *= scaleFactor;
            }
        }
        
        return new float[]{px, py, pz};
    }
}
