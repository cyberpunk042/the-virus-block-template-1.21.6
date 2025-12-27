package net.cyberpunk042.client.visual.mesh.ray;

import net.cyberpunk042.visual.animation.WaveConfig;
import net.cyberpunk042.visual.shape.RayArrangement;
import net.cyberpunk042.visual.shape.RayCurvature;
import net.cyberpunk042.visual.shape.RayDistribution;
import net.cyberpunk042.visual.shape.RayLayerMode;
import net.cyberpunk042.visual.shape.RayLineShape;
import net.cyberpunk042.visual.shape.RaysShape;

import java.util.Random;

/**
 * Computes ray positions based on arrangement and distribution.
 * 
 * <p>This extracts the common positioning logic from RaysTessellator,
 * computing start/end positions for each ray based on the shape's
 * arrangement mode, distribution, and randomness settings.
 * 
 * <h2>Usage</h2>
 * <pre>
 * RayContext context = RayPositioner.computeContext(shape, index, layerIndex, rng);
 * tessellator.tessellate(builder, shape, context);
 * </pre>
 * 
 * @see RayContext
 * @see RayTypeTessellator
 */
public final class RayPositioner {
    
    private static final float TWO_PI = (float) (Math.PI * 2);
    private static final float GOLDEN_RATIO = 1.618033988749895f;
    
    private RayPositioner() {} // Utility class
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Main API
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Computes the context for a single ray.
     * 
     * @param shape The rays shape configuration
     * @param index Ray index (0 to count-1)
     * @param layerIndex Layer index (0 to layers-1)
     * @param rng Random number generator for distribution randomness
     * @param wave Optional wave configuration (null if no wave)
     * @param time Current animation time
     * @return Computed RayContext with position and shape data
     */
    public static RayContext computeContext(
            RaysShape shape, 
            int index, 
            int layerIndex, 
            Random rng,
            WaveConfig wave,
            float time) {
        return computeContext(shape, index, layerIndex, rng, wave, time, null);
    }
    
    /**
     * Computes contexts for a single ray, returning MULTIPLE when in edge transition.
     * 
     * <p>During RADIATE/ABSORB edge transitions, we need TWO separate shapes:
     * <ul>
     *   <li>The "outgoing" shape at the far edge (despawning)</li>
     *   <li>The "incoming" shape at the near edge (spawning)</li>
     * </ul>
     * This prevents vertex sharing artifacts between the two.</p>
     * 
     * @param shape The rays shape configuration
     * @param index Ray index (0 to count-1)
     * @param layerIndex Layer index (0 to layers-1)
     * @param rng Random number generator for distribution randomness
     * @param wave Optional wave configuration (null if no wave)
     * @param time Current animation time
     * @param flowConfig Optional flow animation config (null if no flow)
     * @return List of RayContexts (1 normally, 2 during edge transitions)
     */
    public static java.util.List<RayContext> computeContexts(
            RaysShape shape, 
            int index, 
            int layerIndex, 
            Random rng,
            WaveConfig wave,
            float time,
            net.cyberpunk042.visual.animation.RayFlowConfig flowConfig) {
        
        java.util.List<RayContext> result = new java.util.ArrayList<>(2);
        
        // Check if we're in edge transition (need two shapes) - only for 3D ray types
        // LINE rays don't need the dual-context pattern since they don't have linking artifacts
        boolean is3DRayType = shape.effectiveRayType().is3D();
        
        if (is3DRayType && flowConfig != null && flowConfig.isActive()) {
            net.cyberpunk042.visual.animation.LengthMode lengthMode = flowConfig.length();
            if (lengthMode == net.cyberpunk042.visual.animation.LengthMode.RADIATE ||
                lengthMode == net.cyberpunk042.visual.animation.LengthMode.ABSORB) {
                
                // Compute edge width based on sweepCopies (same logic as computeFlowAnimation)
                float sweepCopies = Math.max(0.1f, flowConfig.waveCount());
                float edgeWidth = 0.1f / sweepCopies;
                edgeWidth = Math.max(0.01f, Math.min(0.1f, edgeWidth));
                
                // Compute the phase for this ray
                float rayPhase = computeRayPhase(flowConfig, index, shape.count(), time);
                
                // During edge transitions, generate two shapes to prevent linking artifacts
                if (rayPhase < edgeWidth || rayPhase > (1.0f - edgeWidth)) {
                    // Generate both the main shape AND the wrapped shape
                    RayContext primaryContext = computeContextWithPhase(
                        shape, index, layerIndex, rng, wave, time, flowConfig, rayPhase, false);
                    RayContext wrappedContext = computeContextWithPhase(
                        shape, index, layerIndex, rng, wave, time, flowConfig, rayPhase, true);
                    
                    result.add(primaryContext);
                    result.add(wrappedContext);
                    return result;
                }
            }
        }
        
        // Normal case: single context
        result.add(computeContext(shape, index, layerIndex, rng, wave, time, flowConfig));
        return result;
    }
    
    /**
     * Computes the context for a single ray with flow animation support.
     * 
     * @param shape The rays shape configuration
     * @param index Ray index (0 to count-1)
     * @param layerIndex Layer index (0 to layers-1)
     * @param rng Random number generator for distribution randomness
     * @param wave Optional wave configuration (null if no wave)
     * @param time Current animation time
     * @param flowConfig Optional flow animation config (null if no flow)
     * @return Computed RayContext with position, shape, and animation data
     */
    public static RayContext computeContext(
            RaysShape shape, 
            int index, 
            int layerIndex, 
            Random rng,
            WaveConfig wave,
            float time,
            net.cyberpunk042.visual.animation.RayFlowConfig flowConfig) {
        
        int count = shape.count();
        float layerSpacing = shape.layerSpacing();
        
        // Compute distribution offsets
        DistributionResult dist = computeDistribution(shape, index, count, rng);
        
        // Compute positions based on arrangement
        float[] start = new float[3];
        float[] end = new float[3];
        computePosition(shape, index, count, layerIndex, layerSpacing, dist, start, end);
        
        // Compute direction and length
        float dx = end[0] - start[0];
        float dy = end[1] - start[1];
        float dz = end[2] - start[2];
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float[] direction = new float[3];
        if (length > 0.0001f) {
            direction[0] = dx / length;
            direction[1] = dy / length;
            direction[2] = dz / length;
        } else {
            direction[1] = 1.0f; // Default up
        }
        
        // Determine shape segments
        RayLineShape lineShape = shape.effectiveLineShape();
        RayCurvature curvature = shape.effectiveCurvature();
        int shapeSegments = shape.effectiveShapeSegments();
        
        boolean hasWave = wave != null && wave.isActive() && wave.isCpuMode();
        if (hasWave && shapeSegments < 16) {
            shapeSegments = 16;
        }
        
        // Compute orientation for 3D ray types
        net.cyberpunk042.visual.shape.RayOrientation orientation = shape.effectiveRayOrientation();
        float[] orientationVector = computeOrientationVector(orientation, start, direction);
        
        // === Compute flow animation values ===
        float flowPositionOffset = 0.0f;
        float flowScale = 1.0f;
        float visibleTStart = 0.0f;
        float visibleTEnd = 1.0f;
        float flowAlpha = 1.0f;
        
        if (flowConfig != null && flowConfig.isActive()) {
            FlowAnimationResult flowResult = computeFlowAnimation(
                flowConfig, index, count, time, shape.innerRadius(), shape.outerRadius());
            flowPositionOffset = flowResult.positionOffset;
            // Note: We will recompute scale/alpha/clip based on ACTUAL position below
            
            // Apply position offset to start/end positions
            // This TRANSLATES the ray segment along the Travel Axis (innerRadius to outerRadius)
            // BUT: When curvature is active, the tessellator handles curved path positioning
            boolean hasCurvature = curvature != null 
                && curvature != RayCurvature.NONE 
                && shape.curvatureIntensity() > 0.001f;
            
            if (!hasCurvature && Math.abs(flowPositionOffset) > 0.001f) {
                start[0] += direction[0] * flowPositionOffset;
                start[1] += direction[1] * flowPositionOffset;
                start[2] += direction[2] * flowPositionOffset;
                end[0] += direction[0] * flowPositionOffset;
                end[1] += direction[1] * flowPositionOffset;
                end[2] += direction[2] * flowPositionOffset;
            }
            
            // === POSITION-BASED EDGE TRANSITIONS ===
            // Now that the ray is at its final position, compute edge transitions
            // based on where the ray ACTUALLY IS relative to field boundaries.
            // NOTE: Skip for curvature - tessellator handles edge detection using actual curved positions
            if (!hasCurvature) {
                float innerRadius = shape.innerRadius();
                float outerRadius = shape.outerRadius();
                float edgeWidth = shape.rayLength() * 0.5f; // Transition zone is half the ray length
                edgeWidth = Math.max(0.1f, Math.min(edgeWidth, (outerRadius - innerRadius) * 0.15f));
                
                // Compute radial distances of start and end from center
                float startDist = (float) Math.sqrt(start[0]*start[0] + start[1]*start[1] + start[2]*start[2]);
                float endDist = (float) Math.sqrt(end[0]*end[0] + end[1]*end[1] + end[2]*end[2]);
                
                // Determine leading and trailing edges based on ray direction
                net.cyberpunk042.visual.animation.LengthMode lengthMode = flowConfig.length();
                net.cyberpunk042.visual.animation.EdgeTransitionMode edgeMode = flowConfig.effectiveEdgeTransition();
                
                if (lengthMode == net.cyberpunk042.visual.animation.LengthMode.RADIATE) {
                    // RADIATE: ray moves outward, leading edge is END, trailing edge is START
                    float leadingEdge = endDist;
                    float trailingEdge = startDist;
                    
                    // Spawning: trailing edge is below innerRadius
                    if (trailingEdge < innerRadius) {
                        float penetration = innerRadius - trailingEdge;
                        float edgeFactor = Math.max(0, Math.min(1, 1.0f - penetration / edgeWidth));
                        switch (edgeMode) {
                            case SCALE -> flowScale = edgeFactor;
                            case CLIP -> visibleTStart = 1.0f - edgeFactor;
                            case FADE -> flowAlpha = edgeFactor;
                        }
                    }
                    // Despawning: leading edge is above outerRadius
                    else if (leadingEdge > outerRadius) {
                        float penetration = leadingEdge - outerRadius;
                        float edgeFactor = Math.max(0, Math.min(1, 1.0f - penetration / edgeWidth));
                        switch (edgeMode) {
                            case SCALE -> flowScale = edgeFactor;
                            case CLIP -> visibleTEnd = edgeFactor;
                            case FADE -> flowAlpha = edgeFactor;
                        }
                    }
                } else if (lengthMode == net.cyberpunk042.visual.animation.LengthMode.ABSORB) {
                    // ABSORB: ray moves inward, leading edge is START, trailing edge is END
                    float leadingEdge = startDist;
                    float trailingEdge = endDist;
                    
                    // Spawning: trailing edge is above outerRadius
                    if (trailingEdge > outerRadius) {
                        float penetration = trailingEdge - outerRadius;
                        float edgeFactor = Math.max(0, Math.min(1, 1.0f - penetration / edgeWidth));
                        switch (edgeMode) {
                            case SCALE -> flowScale = edgeFactor;
                            case CLIP -> visibleTEnd = edgeFactor;
                            case FADE -> flowAlpha = edgeFactor;
                        }
                    }
                    // Despawning: leading edge is below innerRadius
                    else if (leadingEdge < innerRadius) {
                        float penetration = innerRadius - leadingEdge;
                        float edgeFactor = Math.max(0, Math.min(1, 1.0f - penetration / edgeWidth));
                        switch (edgeMode) {
                            case SCALE -> flowScale = edgeFactor;
                            case CLIP -> visibleTStart = 1.0f - edgeFactor;
                            case FADE -> flowAlpha = edgeFactor;
                        }
                    }
                }
            }
            // For curvature: tessellator will handle edge detection using flowPositionOffset
        }
        
        // === Compute field deformation values ===
        net.cyberpunk042.visual.shape.FieldDeformationMode fieldDeformation = shape.effectiveFieldDeformation();
        float fieldDeformationIntensity = shape.fieldDeformationIntensity();
        float normalizedDistance = 0.5f; // Default to middle if can't compute
        float fieldStretch = 1.0f;
        
        if (fieldDeformation.isActive()) {
            // Compute distance from center based on the ray's position
            // Use start position to determine how far from center this ray is
            float distFromCenter = (float) Math.sqrt(
                start[0] * start[0] + start[1] * start[1] + start[2] * start[2]);
            
            // Normalize based on inner/outer radius
            float innerR = shape.innerRadius();
            float outerR = shape.outerRadius();
            if (outerR > innerR) {
                normalizedDistance = Math.max(0.01f, Math.min(1.0f, 
                    (distFromCenter - innerR) / (outerR - innerR)));
            }
            
            // Compute stretch based on deformation mode and distance
            fieldStretch = fieldDeformation.computeStretch(normalizedDistance, fieldDeformationIntensity);
        }
        
        return RayContext.builder()
            .start(start)
            .end(end)
            .direction(direction)
            .length(length)
            .index(index)
            .count(count)
            .layerIndex(layerIndex)
            .t(count > 1 ? (float) index / (count - 1) : 0f)
            .width(shape.rayWidth())
            .fadeStart(shape.fadeStart())
            .fadeEnd(shape.fadeEnd())
            .lineShape(lineShape)
            .lineShapeAmplitude(shape.lineShapeAmplitude())
            .lineShapeFrequency(shape.lineShapeFrequency())
            .curvature(curvature)
            .curvatureIntensity(shape.curvatureIntensity())
            .shapeSegments(shapeSegments)
            .orientation(orientation)
            .orientationVector(orientationVector)
            .shapeIntensity(shape.shapeIntensity())
            .shapeLength(shape.shapeLength())
            .wave(wave)
            .time(time)
            .hasWave(hasWave)
            .flowConfig(flowConfig)
            .flowPositionOffset(flowPositionOffset)
            .travelRange(shape.outerRadius() - shape.innerRadius())
            .innerRadius(shape.innerRadius())
            .outerRadius(shape.outerRadius())
            .flowScale(flowScale)
            .visibleTStart(visibleTStart)
            .visibleTEnd(visibleTEnd)
            .flowAlpha(flowAlpha)
            .fieldDeformation(fieldDeformation)
            .fieldDeformationIntensity(fieldDeformationIntensity)
            .normalizedDistance(normalizedDistance)
            .fieldStretch(fieldStretch)
            .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Flow Animation Computation
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Result of flow animation computation. */
    private record FlowAnimationResult(
        float positionOffset,
        float scale,
        float visibleTStart,
        float visibleTEnd,
        float alpha
    ) {}
    
    /**
     * Computes the phase for a ray in flow animation.
     */
    private static float computeRayPhase(
            net.cyberpunk042.visual.animation.RayFlowConfig flow,
            int rayIndex, int rayCount, float time) {
        
        // Compute per-ray phase offset based on wave distribution
        float rayPhaseOffset;
        net.cyberpunk042.visual.animation.WaveDistribution waveDist = flow.effectiveWaveDistribution();
        if (waveDist == net.cyberpunk042.visual.animation.WaveDistribution.RANDOM) {
            rayPhaseOffset = (rayIndex * GOLDEN_RATIO) % 1.0f;
        } else {
            rayPhaseOffset = rayCount > 1 ? (float) rayIndex / rayCount : 0f;
        }
        
        // Apply wave scale
        rayPhaseOffset *= flow.effectiveWaveArc();
        
        // Combine with time-based phase
        float basePhase = (time * flow.lengthSpeed()) % 1.0f;
        return (basePhase + rayPhaseOffset) % 1.0f;
    }
    
    /**
     * Computes context for a ray at a specific phase, with optional wrapping.
     * 
     * @param wrapped If true, compute the "wrapped" shape (at the opposite edge)
     */
    private static RayContext computeContextWithPhase(
            RaysShape shape,
            int index,
            int layerIndex,
            Random rng,
            WaveConfig wave,
            float time,
            net.cyberpunk042.visual.animation.RayFlowConfig flowConfig,
            float phase,
            boolean wrapped) {
        
        int count = shape.count();
        float layerSpacing = shape.layerSpacing();
        
        // Compute distribution offsets
        DistributionResult dist = computeDistribution(shape, index, count, rng);
        
        // Compute positions based on arrangement
        float[] start = new float[3];
        float[] end = new float[3];
        computePosition(shape, index, count, layerIndex, layerSpacing, dist, start, end);
        
        // Compute direction and length
        float dx = end[0] - start[0];
        float dy = end[1] - start[1];
        float dz = end[2] - start[2];
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float[] direction = new float[3];
        if (length > 0.0001f) {
            direction[0] = dx / length;
            direction[1] = dy / length;
            direction[2] = dz / length;
        } else {
            direction[1] = 1.0f;
        }
        
        // Determine line shape and curvature
        net.cyberpunk042.visual.shape.RayLineShape lineShape = shape.effectiveLineShape();
        net.cyberpunk042.visual.shape.RayCurvature curvature = shape.effectiveCurvature();
        int shapeSegments = shape.shapeSegments();
        
        // Check for wave animation
        boolean hasWave = wave != null && wave.isActive() && wave.isCpuMode();
        
        // Compute orientation
        net.cyberpunk042.visual.shape.RayOrientation orientation = shape.effectiveRayOrientation();
        float[] orientationVector = computeOrientationVector(orientation, start, direction);
        
        // Compute flow animation for this specific phase
        // Travel range is from innerRadius to outerRadius (the field span)
        float rayLength = shape.outerRadius() - shape.innerRadius();
        float posOffset = 0.0f;
        float scale = 1.0f;
        float visibleTStart = 0.0f;
        float visibleTEnd = 1.0f;
        float flowAlpha = 1.0f;
        
        net.cyberpunk042.visual.animation.LengthMode lengthMode = flowConfig.length();
        net.cyberpunk042.visual.animation.EdgeTransitionMode edgeMode = flowConfig.effectiveEdgeTransition();
        
        // === STEP 1: Compute position offset based on phase ===
        // The position offset determines WHERE the ray is based on animation phase
        if (wrapped) {
            // WRAPPED shape: positioned at the opposite edge from primary
            float sweepCopies = Math.max(0.1f, flowConfig.waveCount());
            float phaseEdgeWidth = 0.1f / sweepCopies;
            phaseEdgeWidth = Math.max(0.01f, Math.min(0.1f, phaseEdgeWidth));
            
            if (phase < phaseEdgeWidth) {
                // Primary is spawning -> wrapped is at far edge (finishing despawn)
                if (lengthMode == net.cyberpunk042.visual.animation.LengthMode.RADIATE) {
                    posOffset = rayLength; // At outer edge
                } else { // ABSORB
                    posOffset = 0.0f; // At inner edge
                }
            } else { // phase > (1 - phaseEdgeWidth)
                // Primary is despawning -> wrapped is at near edge (starting spawn)
                if (lengthMode == net.cyberpunk042.visual.animation.LengthMode.RADIATE) {
                    posOffset = 0.0f; // At inner edge
                } else { // ABSORB
                    posOffset = rayLength; // At outer edge
                }
            }
        } else {
            // PRIMARY shape: position based on phase
            if (lengthMode == net.cyberpunk042.visual.animation.LengthMode.RADIATE) {
                // RADIATE: phase 0 = inner, phase 1 = outer
                posOffset = phase * rayLength;
            } else { // ABSORB
                // ABSORB: phase 0 = outer, phase 1 = inner
                float reversed = 1.0f - phase;
                posOffset = reversed * rayLength;
            }
        }
        
        // === STEP 2: Apply position offset to start/end positions ===
        boolean hasCurvature = curvature != null 
            && curvature != net.cyberpunk042.visual.shape.RayCurvature.NONE 
            && shape.curvatureIntensity() > 0.001f;
        
        if (!hasCurvature && Math.abs(posOffset) > 0.001f) {
            start[0] += direction[0] * posOffset;
            start[1] += direction[1] * posOffset;
            start[2] += direction[2] * posOffset;
            end[0] += direction[0] * posOffset;
            end[1] += direction[1] * posOffset;
            end[2] += direction[2] * posOffset;
        }
        
        // === STEP 3: POSITION-BASED EDGE TRANSITIONS ===
        // Now compute edge transitions based on ACTUAL position
        float innerRadius = shape.innerRadius();
        float outerRadius = shape.outerRadius();
        float edgeWidth = shape.rayLength() * 0.5f; // Transition zone is half the ray length
        edgeWidth = Math.max(0.1f, Math.min(edgeWidth, (outerRadius - innerRadius) * 0.15f));
        
        // Compute radial distances of start and end from center
        float startDist = (float) Math.sqrt(start[0]*start[0] + start[1]*start[1] + start[2]*start[2]);
        float endDist = (float) Math.sqrt(end[0]*end[0] + end[1]*end[1] + end[2]*end[2]);
        
        if (lengthMode == net.cyberpunk042.visual.animation.LengthMode.RADIATE) {
            // RADIATE: ray moves outward, leading edge is END, trailing edge is START
            float leadingEdge = endDist;
            float trailingEdge = startDist;
            
            // Spawning: trailing edge is below innerRadius
            if (trailingEdge < innerRadius) {
                float penetration = innerRadius - trailingEdge;
                float edgeFactor = Math.max(0, Math.min(1, 1.0f - penetration / edgeWidth));
                switch (edgeMode) {
                    case SCALE -> scale = edgeFactor;
                    case CLIP -> visibleTStart = 1.0f - edgeFactor;
                    case FADE -> flowAlpha = edgeFactor;
                }
            }
            // Despawning: leading edge is above outerRadius
            else if (leadingEdge > outerRadius) {
                float penetration = leadingEdge - outerRadius;
                float edgeFactor = Math.max(0, Math.min(1, 1.0f - penetration / edgeWidth));
                switch (edgeMode) {
                    case SCALE -> scale = edgeFactor;
                    case CLIP -> visibleTEnd = edgeFactor;
                    case FADE -> flowAlpha = edgeFactor;
                }
            }
        } else if (lengthMode == net.cyberpunk042.visual.animation.LengthMode.ABSORB) {
            // ABSORB: ray moves inward, leading edge is START, trailing edge is END
            float leadingEdge = startDist;
            float trailingEdge = endDist;
            
            // Spawning: trailing edge is above outerRadius
            if (trailingEdge > outerRadius) {
                float penetration = trailingEdge - outerRadius;
                float edgeFactor = Math.max(0, Math.min(1, 1.0f - penetration / edgeWidth));
                switch (edgeMode) {
                    case SCALE -> scale = edgeFactor;
                    case CLIP -> visibleTEnd = edgeFactor;
                    case FADE -> flowAlpha = edgeFactor;
                }
            }
            // Despawning: leading edge is below innerRadius
            else if (leadingEdge < innerRadius) {
                float penetration = innerRadius - leadingEdge;
                float edgeFactor = Math.max(0, Math.min(1, 1.0f - penetration / edgeWidth));
                switch (edgeMode) {
                    case SCALE -> scale = edgeFactor;
                    case CLIP -> visibleTStart = 1.0f - edgeFactor;
                    case FADE -> flowAlpha = edgeFactor;
                }
            }
        }
        
        // Compute field deformation
        net.cyberpunk042.visual.shape.FieldDeformationMode fieldDeformation = shape.effectiveFieldDeformation();
        float fieldDeformationIntensity = shape.fieldDeformationIntensity();
        float normalizedDistance = 0.5f;
        float fieldStretch = 1.0f;
        
        if (fieldDeformation.isActive()) {
            float distFromCenter = (float) Math.sqrt(
                start[0] * start[0] + start[1] * start[1] + start[2] * start[2]);
            float innerR = shape.innerRadius();
            float outerR = shape.outerRadius();
            if (outerR > innerR) {
                normalizedDistance = Math.max(0.01f, Math.min(1.0f, 
                    (distFromCenter - innerR) / (outerR - innerR)));
            }
            fieldStretch = fieldDeformation.computeStretch(normalizedDistance, fieldDeformationIntensity);
        }
        
        return RayContext.builder()
            .start(start)
            .end(end)
            .direction(direction)
            .length(length)
            .index(index)
            .count(count)
            .layerIndex(layerIndex)
            .t(count > 1 ? (float) index / (count - 1) : 0f)
            .width(shape.rayWidth())
            .fadeStart(shape.fadeStart())
            .fadeEnd(shape.fadeEnd())
            .lineShape(lineShape)
            .lineShapeAmplitude(shape.lineShapeAmplitude())
            .lineShapeFrequency(shape.lineShapeFrequency())
            .curvature(curvature)
            .curvatureIntensity(shape.curvatureIntensity())
            .shapeSegments(shapeSegments)
            .orientation(orientation)
            .orientationVector(orientationVector)
            .shapeIntensity(shape.shapeIntensity())
            .shapeLength(shape.shapeLength())
            .wave(wave)
            .time(time)
            .hasWave(hasWave)
            .flowConfig(flowConfig)
            .flowPositionOffset(posOffset)
            .travelRange(shape.outerRadius() - shape.innerRadius())
            .innerRadius(shape.innerRadius())
            .outerRadius(shape.outerRadius())
            .flowScale(scale)
            .visibleTStart(visibleTStart)
            .visibleTEnd(visibleTEnd)
            .flowAlpha(flowAlpha)
            .fieldDeformation(fieldDeformation)
            .fieldDeformationIntensity(fieldDeformationIntensity)
            .normalizedDistance(normalizedDistance)
            .fieldStretch(fieldStretch)
            .build();
    }
    
    /**
     * Computes flow animation values for a single ray.
     * 
     * The travel range is from innerRadius to outerRadius (the field span).
     */
    private static FlowAnimationResult computeFlowAnimation(
            net.cyberpunk042.visual.animation.RayFlowConfig flow,
            int rayIndex, int rayCount, float time,
            float innerRadius, float outerRadius) {
        
        float posOffset = 0.0f;
        float scale = 1.0f;
        float tStart = 0.0f;
        float tEnd = 1.0f;
        float alpha = 1.0f;
        
        // Travel range is from innerRadius to outerRadius
        float rayLength = outerRadius - innerRadius;
        
        // Compute per-ray angular position (0-1 = 0°-360°)
        float rayAngle;
        net.cyberpunk042.visual.animation.WaveDistribution waveDist = flow.effectiveWaveDistribution();
        if (waveDist == net.cyberpunk042.visual.animation.WaveDistribution.RANDOM) {
            // Golden ratio for even-ish random distribution
            rayAngle = (rayIndex * 0.618033988749895f) % 1.0f;
        } else {
            rayAngle = rayCount > 1 ? (float) rayIndex / rayCount : 0f;
        }
        
        // Apply wave arc scaling
        float waveArc = flow.effectiveWaveArc();
        float scaledAngle = rayAngle * waveArc;
        
        // === Sweep Copies (waveCount) ===
        // < 1: TRIM - reduces the visible wedge size (fewer rays visible at once)
        // = 1: Normal sweep behavior
        // > 1: DUPLICATE - creates N copies of the sweep wedge around the circle
        float sweepCopies = Math.max(0.1f, flow.waveCount());
        
        // === Length Mode Animation ===
        net.cyberpunk042.visual.animation.LengthMode lengthMode = flow.length();
        if (lengthMode != null && lengthMode != net.cyberpunk042.visual.animation.LengthMode.NONE) {
            float basePhase = (time * flow.lengthSpeed()) % 1.0f;
            
            // Compute the animation phase for this ray (0-1)
            float phase = (basePhase + scaledAngle) % 1.0f;
            
            // Edge width for spawn/despawn transitions
            // sweepCopies affects edge width: higher = narrower transitions = more rays visible at full size
            float edgeWidth = 0.1f / sweepCopies;
            edgeWidth = Math.max(0.01f, Math.min(0.1f, edgeWidth));
            
            switch (lengthMode) {
                case RADIATE -> {
                    // Ray travels outward: starts at inner, moves to outer
                    // Position offset is based on phase (0 = inner, 1 = outer)
                    // Edge transitions are computed in computeContext based on actual position
                    posOffset = phase * rayLength;
                }
                case ABSORB -> {
                    // Ray travels inward: starts at outer, moves to inner
                    // Position offset is based on reversed phase (0 = outer, 1 = inner)
                    // Edge transitions are computed in computeContext based on actual position
                    float reversed = 1.0f - phase;
                    posOffset = reversed * rayLength;
                }
                case PULSE -> {
                    // Breathing effect - scale oscillates
                    scale = 0.5f + 0.5f * (float) Math.sin(phase * Math.PI * 2);
                }
                case SEGMENT -> {
                    // Fixed segment visible
                    float segLen = flow.segmentLength();
                    tStart = phase;
                    tEnd = phase + segLen;
                    if (tEnd > 1.0f) {
                        // Wrap around
                        tEnd = tEnd - 1.0f;
                    }
                }
                case GROW_SHRINK -> {
                    // Grows then shrinks
                    if (phase < 0.5f) {
                        scale = phase * 2.0f;
                    } else {
                        scale = (1.0f - phase) * 2.0f;
                    }
                }
                default -> {}
            }
        }
        
        // === Flicker Mode Animation ===
        net.cyberpunk042.visual.animation.FlickerMode flickerMode = flow.flicker();
        if (flickerMode != null && flickerMode != net.cyberpunk042.visual.animation.FlickerMode.NONE) {
            float intensity = flow.flickerIntensity();
            float freq = flow.flickerFrequency();
            float flickerPhase = time * freq + rayIndex * 1.618033988749895f;
            
            switch (flickerMode) {
                case SCINTILLATION -> {
                    // Random twinkling
                    float noise = (float) Math.sin(flickerPhase * 7.3) * 
                                  (float) Math.sin(flickerPhase * 13.7) * 
                                  (float) Math.sin(flickerPhase * 23.1);
                    alpha = 1.0f - intensity * (0.5f + 0.5f * noise);
                }
                case STROBE -> {
                    // On/off blinking
                    alpha = ((flickerPhase % 1.0f) < 0.5f) ? 1.0f : 1.0f - intensity;
                }
                case FADE_PULSE -> {
                    // Sine wave alpha (breathing)
                    alpha = 1.0f - intensity * (0.5f + 0.5f * (float) Math.sin(flickerPhase * Math.PI * 2));
                }
                case FLICKER -> {
                    // Candlelight-style random flickering
                    int seed = (int)(flickerPhase * 1000) + rayIndex * 12345;
                    java.util.Random flickerRng = new java.util.Random(seed);
                    alpha = 1.0f - intensity * flickerRng.nextFloat();
                }
                case LIGHTNING -> {
                    // Flash bright then fade
                    float flashPhase = flickerPhase % 1.0f;
                    alpha = 1.0f - intensity * Math.min(1.0f, flashPhase * 3.0f);
                }
                case HEARTBEAT -> {
                    // Double-pulse rhythm
                    float beatPhase = flickerPhase % 1.0f;
                    float pulse = beatPhase < 0.15f ? beatPhase / 0.15f :
                                  beatPhase < 0.3f ? (0.3f - beatPhase) / 0.15f :
                                  beatPhase < 0.4f ? (beatPhase - 0.3f) / 0.1f :
                                  beatPhase < 0.5f ? (0.5f - beatPhase) / 0.1f : 0f;
                    alpha = 1.0f - intensity * (1.0f - pulse);
                }
                default -> {}
            }
        }
        
        return new FlowAnimationResult(posOffset, scale, tStart, tEnd, alpha);
    }
    
    /**
     * Computes the orientation vector based on the orientation mode.
     * 
     * @param orientation Orientation mode
     * @param start Ray start position (used for OUTWARD/INWARD/TANGENT)
     * @param direction Ray direction (used for ALONG_RAY/AGAINST_RAY)
     * @return Normalized orientation direction [x, y, z]
     */
    private static float[] computeOrientationVector(
            net.cyberpunk042.visual.shape.RayOrientation orientation, 
            float[] start, 
            float[] direction) {
        
        return switch (orientation) {
            case ALONG_RAY -> new float[] { direction[0], direction[1], direction[2] };
            
            case AGAINST_RAY -> new float[] { -direction[0], -direction[1], -direction[2] };
            
            case UPWARD -> new float[] { 0, 1, 0 };
            
            case DOWNWARD -> new float[] { 0, -1, 0 };
            
            case OUTWARD -> {
                // Direction from center (0,0,0) to ray start
                float len = (float) Math.sqrt(start[0]*start[0] + start[1]*start[1] + start[2]*start[2]);
                if (len > 0.0001f) {
                    yield new float[] { start[0]/len, start[1]/len, start[2]/len };
                }
                yield new float[] { 0, 1, 0 }; // Fallback if at center
            }
            
            case INWARD -> {
                // Direction from ray start to center (0,0,0)
                float len = (float) Math.sqrt(start[0]*start[0] + start[1]*start[1] + start[2]*start[2]);
                if (len > 0.0001f) {
                    yield new float[] { -start[0]/len, -start[1]/len, -start[2]/len };
                }
                yield new float[] { 0, -1, 0 }; // Fallback if at center
            }
            
            case TANGENT -> {
                // Perpendicular to radial direction in XZ plane (for circular motion effect)
                float len = (float) Math.sqrt(start[0]*start[0] + start[2]*start[2]);
                if (len > 0.0001f) {
                    // Tangent is perpendicular to radial: (-z, 0, x) normalized
                    yield new float[] { -start[2]/len, 0, start[0]/len };
                }
                yield new float[] { 1, 0, 0 }; // Fallback if on Y axis
            }
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Distribution Calculation
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Result from distribution calculation. */
    public record DistributionResult(
        float startOffset,
        float lengthMod,
        float angleJitter,
        float radiusJitter
    ) {}
    
    /**
     * Computes distribution-based offsets for a ray.
     */
    public static DistributionResult computeDistribution(
            RaysShape shape, 
            int index, 
            int count, 
            Random rng) {
        
        RayDistribution distribution = shape.distribution();
        float innerRadius = shape.innerRadius();
        float outerRadius = shape.outerRadius();
        float rayLength = shape.rayLength();
        float randomness = shape.randomness();
        float lengthVariation = shape.lengthVariation();
        
        float startOffset = 0f;
        float lengthMod = 1f;
        float angleJitter = 0f;
        float radiusJitter = 0f;
        
        switch (distribution) {
            case UNIFORM -> {
                lengthMod = 1f - lengthVariation * rng.nextFloat();
                angleJitter = randomness * (rng.nextFloat() - 0.5f) * TWO_PI / count * 0.5f;
            }
            case RANDOM -> {
                float availableRange = outerRadius - innerRadius;
                float maxLength = Math.min(rayLength, availableRange);
                
                lengthMod = 0.5f + 0.5f * rng.nextFloat();
                float actualLength = maxLength * lengthMod;
                
                float maxStartOffset = availableRange - actualLength;
                if (maxStartOffset > 0) {
                    startOffset = maxStartOffset * rng.nextFloat();
                }
                
                angleJitter = randomness * (rng.nextFloat() - 0.5f) * TWO_PI / count;
                radiusJitter = randomness * (rng.nextFloat() - 0.5f) * 0.3f;
            }
            case STOCHASTIC -> {
                float availableRange = outerRadius - innerRadius;
                
                lengthMod = 0.2f + 0.8f * rng.nextFloat();
                float actualLength = rayLength * lengthMod;
                
                float maxStartOffset = Math.max(0, availableRange - actualLength * 0.3f);
                startOffset = maxStartOffset * rng.nextFloat();
                
                angleJitter = (rng.nextFloat() - 0.5f) * TWO_PI / count * 2f;
                radiusJitter = (rng.nextFloat() - 0.5f) * 0.5f;
            }
        }
        
        return new DistributionResult(startOffset, lengthMod, angleJitter, radiusJitter);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Position Calculation
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Computes start and end positions based on arrangement mode.
     */
    public static void computePosition(
            RaysShape shape, 
            int index, 
            int count,
            int layerIndex, 
            float layerSpacing,
            DistributionResult dist, 
            float[] start, 
            float[] end) {
        
        RayArrangement arrangement = shape.arrangement();
        float innerRadius = shape.innerRadius();
        float outerRadius = shape.outerRadius();
        float rayLength = shape.rayLength();
        
        switch (arrangement) {
            case RADIAL -> computeRadial(shape, index, count, layerIndex, layerSpacing, dist, start, end);
            case SPHERICAL, DIVERGING -> computeSpherical(shape, index, count, layerIndex, layerSpacing, dist, start, end, false);
            case CONVERGING -> computeSpherical(shape, index, count, layerIndex, layerSpacing, dist, start, end, true);
            case PARALLEL -> computeParallel(shape, index, count, layerIndex, layerSpacing, dist, start, end);
        }
    }
    
    private static void computeRadial(
            RaysShape shape, int index, int count, int layerIndex, float layerSpacing,
            DistributionResult dist, float[] start, float[] end) {
        
        float innerRadius = shape.innerRadius();
        float rayLength = shape.rayLength();
        RayLayerMode layerMode = shape.effectiveLayerMode();
        
        float angle = (index * TWO_PI / count) + dist.angleJitter();
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        
        // Compute layer offset based on layer mode
        float layerY = 0;
        float layerRadiusOffset = 0;
        float layerAngleOffset = 0;
        
        switch (layerMode) {
            case VERTICAL -> {
                // Stack layers vertically
                layerY = (layerIndex - (shape.layers() - 1) / 2.0f) * layerSpacing;
            }
            case RADIAL -> {
                // Layers extend radially outward (each layer starts where previous ends)
                layerRadiusOffset = layerIndex * rayLength;
            }
            case SHELL -> {
                // Concentric shells (same as RADIAL but adds to inner radius)
                layerRadiusOffset = layerIndex * layerSpacing;
            }
            case SPIRAL -> {
                // Spiral: both angular and radial offset
                layerAngleOffset = layerIndex * TWO_PI / 8; // 45° between layers
                layerRadiusOffset = layerIndex * layerSpacing;
            }
        }
        
        // Apply angular offset for spiral mode
        if (layerMode == RayLayerMode.SPIRAL) {
            float newAngle = angle + layerAngleOffset;
            cos = (float) Math.cos(newAngle);
            sin = (float) Math.sin(newAngle);
        }
        
        // rayLength defines how long each ray is
        float innerR = innerRadius + layerRadiusOffset + dist.startOffset();
        float outerR = innerR + rayLength * dist.lengthMod();
        innerR *= (1 + dist.radiusJitter());
        outerR *= (1 + dist.radiusJitter());
        
        start[0] = cos * innerR;
        start[1] = layerY;
        start[2] = sin * innerR;
        
        end[0] = cos * outerR;
        end[1] = layerY;
        end[2] = sin * outerR;
    }
    
    private static void computeSpherical(
            RaysShape shape, int index, int count, int layerIndex, float layerSpacing,
            DistributionResult dist, float[] start, float[] end, boolean converging) {
        
        float innerRadius = shape.innerRadius();
        float outerRadius = shape.outerRadius();
        float rayLength = shape.rayLength();
        RayLayerMode layerMode = shape.effectiveLayerMode();
        
        // Compute layer offset based on layer mode
        float shellOffset = 0;
        float layerRadiusExt = 0;
        
        switch (layerMode) {
            case SHELL -> {
                // Concentric shells at increasing radii
                shellOffset = layerIndex * layerSpacing;
            }
            case RADIAL -> {
                // Rays extend further outward (each layer continues where previous ended)
                layerRadiusExt = layerIndex * rayLength;
            }
            case VERTICAL, SPIRAL -> {
                // For spherical, VERTICAL and SPIRAL don't make as much sense
                // Fall back to SHELL behavior
                shellOffset = layerIndex * layerSpacing;
            }
        }
        
        float phi = (float) Math.acos(1 - 2 * (index + 0.5f) / count);
        float theta = TWO_PI * index / GOLDEN_RATIO + dist.angleJitter();
        
        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        float cosTheta = (float) Math.cos(theta);
        float sinTheta = (float) Math.sin(theta);
        
        float dx = sinPhi * cosTheta;
        float dy = cosPhi;
        float dz = sinPhi * sinTheta;
        
        if (converging) {
            float outerR = (outerRadius + shellOffset + layerRadiusExt) * (1 + dist.radiusJitter());
            float innerR = outerR - rayLength * dist.lengthMod();
            outerR += dist.startOffset();
            innerR += dist.startOffset();
            if (innerR < 0) innerR = 0;
            
            start[0] = dx * outerR;
            start[1] = dy * outerR;
            start[2] = dz * outerR;
            
            end[0] = dx * innerR;
            end[1] = dy * innerR;
            end[2] = dz * innerR;
        } else {
            float innerR = (innerRadius + shellOffset + layerRadiusExt + dist.startOffset()) * (1 + dist.radiusJitter());
            float outerR = innerR + rayLength * dist.lengthMod();
            
            start[0] = dx * innerR;
            start[1] = dy * innerR;
            start[2] = dz * innerR;
            
            end[0] = dx * outerR;
            end[1] = dy * outerR;
            end[2] = dz * outerR;
        }
    }
    
    private static void computeParallel(
            RaysShape shape, int index, int count, int layerIndex, float layerSpacing,
            DistributionResult dist, float[] start, float[] end) {
        
        float outerRadius = shape.outerRadius();
        float rayLength = shape.rayLength();
        
        int gridSize = (int) Math.ceil(Math.sqrt(count));
        int gx = index % gridSize;
        int gz = index / gridSize;
        
        float spacing = outerRadius * 2 / gridSize;
        float x = (gx - gridSize / 2.0f + 0.5f) * spacing;
        float z = (gz - gridSize / 2.0f + 0.5f) * spacing + layerIndex * layerSpacing;
        
        x += dist.angleJitter() * spacing;
        z += dist.radiusJitter() * spacing;
        
        float yStart = -rayLength * dist.lengthMod() / 2 + dist.startOffset();
        float yEnd = rayLength * dist.lengthMod() / 2 + dist.startOffset();
        
        start[0] = x;
        start[1] = yStart;
        start[2] = z;
        
        end[0] = x;
        end[1] = yEnd;
        end[2] = z;
    }
}
