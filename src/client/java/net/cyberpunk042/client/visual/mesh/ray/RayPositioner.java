package net.cyberpunk042.client.visual.mesh.ray;

import net.cyberpunk042.visual.animation.WaveConfig;
import net.cyberpunk042.visual.shape.RayArrangement;
import net.cyberpunk042.visual.shape.RayCurvature;
import net.cyberpunk042.visual.shape.RayDistribution;
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
            .wave(wave)
            .time(time)
            .hasWave(hasWave)
            .build();
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
        
        float angle = (index * TWO_PI / count) + dist.angleJitter();
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float layerY = (layerIndex - (shape.layers() - 1) / 2.0f) * layerSpacing;
        
        float innerR = innerRadius + dist.startOffset();
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
        
        float shellOffset = layerIndex * layerSpacing;
        
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
            float outerR = (outerRadius + shellOffset) * (1 + dist.radiusJitter());
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
            float innerR = (innerRadius + shellOffset + dist.startOffset()) * (1 + dist.radiusJitter());
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
