package net.cyberpunk042.client.visual.mesh.ray;

import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.client.visual.mesh.ray.tessellation.TessEdgeModeFactory;
import net.cyberpunk042.client.visual.mesh.ray.tessellation.TessEdgeResult;
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
     * <p>Uses Stage/Phase model via TessEdgeModeFactory for animation:
     * <ul>
     *   <li>CLIP: Geometric clipping based on phase</li>
     *   <li>FADE: Alpha modulation based on phase</li>
     *   <li>SCALE: Width scaling based on phase</li>
     * </ul>
     */
    private void tessellateSimpleLine(MeshBuilder builder, RayContext context) {
        // Get edge result from Stage/Phase model
        TessEdgeResult edgeResult = context.computeEdgeResult();
        
        // Skip if completely hidden
        if (!edgeResult.isVisible()) {
            return;
        }
        
        float[] start = context.start();
        float[] end = context.end();
        float[] dir = context.direction();
        
        // Get clipping/scale/alpha from Stage/Phase model
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
        
        // u = t value for shader, alpha from Stage/Phase model
        int v0 = builder.vertex(actualStart[0], actualStart[1], actualStart[2], dir[0], dir[1], dir[2], tStart, 0, alpha);
        int v1 = builder.vertex(actualEnd[0], actualEnd[1], actualEnd[2], dir[0], dir[1], dir[2], tEnd, 0, alpha);
        builder.line(v0, v1);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Segmented (Dashed)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Tessellates a segmented (dashed) ray.
     * <p>Uses Stage/Phase model via TessEdgeModeFactory for animation.
     */
    private void tessellateSegmentedRay(MeshBuilder builder, RayContext context, 
                                        int segments, float segmentGap) {
        // Get edge result from Stage/Phase model
        TessEdgeResult edgeResult = context.computeEdgeResult();
        
        // Skip if completely hidden
        if (!edgeResult.isVisible()) {
            return;
        }
        
        float[] start = context.start();
        float[] end = context.end();
        float[] dir = context.direction();
        
        // Get clipping/scale/alpha from Stage/Phase model
        float globalTStart = edgeResult.clipStart();
        float globalTEnd = edgeResult.clipEnd();
        float scale = edgeResult.scale();
        float alpha = edgeResult.alpha();
        
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
            
            // Interpolate positions for this segment
            float[] segStart = RayGeometryUtils.interpolate(start, end, effectiveTStart);
            float[] segEnd = RayGeometryUtils.interpolate(start, end, effectiveTEnd);
            
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
        // Get edge result from Stage/Phase model
        TessEdgeResult edgeResult = context.computeEdgeResult();
        
        // Skip if completely hidden
        if (!edgeResult.isVisible()) {
            return;
        }
        
        float[] start = context.start();
        float[] end = context.end();
        float[] dir = context.direction();
        int segments = context.shapeSegments();
        
        if (context.length() < 0.0001f) {
            // Degenerate ray
            builder.vertex(start[0], start[1], start[2], 0, 0, 0, 0.0f, 0, edgeResult.alpha());
            return;
        }
        
        // Get clipping/scale/alpha from Stage/Phase model
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
     * Tessellates a ray that is BOTH segmented AND has complex shape.
     */
    private void tessellateSegmentedShapedRay(MeshBuilder builder, RaysShape shape, RayContext context) {
        // For now, delegate to shaped ray (simplified)
        // TODO: Implement proper segmented + shaped combination
        tessellateShapedRay(builder, shape, context);
    }
}
