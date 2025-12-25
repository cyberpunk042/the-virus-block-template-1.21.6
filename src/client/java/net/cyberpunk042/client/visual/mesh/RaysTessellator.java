package net.cyberpunk042.client.visual.mesh;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.shape.RayArrangement;
import net.cyberpunk042.visual.shape.RayDistribution;
import net.cyberpunk042.visual.shape.RaysShape;

import java.util.Random;

/**
 * Tessellates rays shapes into line meshes.
 * 
 * <h2>Geometry</h2>
 * <p>Rays are line segments positioned according to the arrangement mode:</p>
 * <ul>
 *   <li>RADIAL - 2D star pattern on XZ plane, layers stack vertically</li>
 *   <li>SPHERICAL - 3D uniform distribution, layers create concentric shells</li>
 *   <li>PARALLEL - Grid of parallel rays pointing along +Y</li>
 *   <li>CONVERGING - Rays pointing toward center, layers create concentric shells</li>
 *   <li>DIVERGING - Rays pointing away from center, layers create concentric shells</li>
 * </ul>
 * 
 * <h2>Distribution Modes</h2>
 * <ul>
 *   <li><b>UNIFORM</b> - All rays have identical length and position within their pattern</li>
 *   <li><b>RANDOM</b> - Each ray has random start offset and random length within the radius range</li>
 *   <li><b>STOCHASTIC</b> - Heavily randomized with variable density and length</li>
 * </ul>
 * 
 * @see RaysShape
 * @see RayArrangement
 * @see RayDistribution
 */
public final class RaysTessellator {
    
    private static final float PI = (float) Math.PI;
    private static final float TWO_PI = 2 * PI;
    private static final float GOLDEN_RATIO = 1.618033988749895f;
    
    private RaysTessellator() {}
    
    /**
     * Tessellates rays shape into a line mesh.
     * 
     * @param shape The rays shape definition
     * @return Mesh containing line segments
     */
    public static Mesh tessellate(RaysShape shape) {
        MeshBuilder builder = MeshBuilder.lines();
        
        int count = shape.count();
        int layers = Math.max(1, shape.layers());
        float layerSpacing = shape.layerSpacing();
        RayArrangement arrangement = shape.arrangement();
        RayDistribution distribution = shape.distribution();
        
        Random rng = new Random(42); // Deterministic random for stable results
        
        Logging.FIELD.topic("tessellation").debug(
            "Tessellating rays: count={}, layers={}, arrangement={}, distribution={}", 
            count, layers, arrangement, distribution);
        
        // Generate rays for each layer
        for (int layer = 0; layer < layers; layer++) {
            // For RADIAL/PARALLEL: layerOffset is Y displacement
            // For 3D modes: layerScale creates concentric shells (innerRadius grows)
            float layerT = layers > 1 ? (float) layer / (layers - 1) : 0f; // 0 to 1
            
            for (int i = 0; i < count; i++) {
                tessellateRay(builder, shape, i, count, layer, layerT, layerSpacing, rng);
            }
        }
        
        return builder.build();
    }
    
    /**
     * Tessellates a single ray.
     */
    private static void tessellateRay(MeshBuilder builder, RaysShape shape, 
                                       int index, int count, int layerIndex, float layerT, 
                                       float layerSpacing, Random rng) {
        RayArrangement arrangement = shape.arrangement();
        RayDistribution distribution = shape.distribution();
        float innerRadius = shape.innerRadius();
        float outerRadius = shape.outerRadius();
        float rayLength = shape.rayLength();
        float randomness = shape.randomness();
        float lengthVariation = shape.lengthVariation();
        
        // Calculate distribution-based offsets
        float startOffset = 0f;
        float lengthMod = 1f;
        float angleJitter = 0f;
        float radiusJitter = 0f;
        
        switch (distribution) {
            case UNIFORM -> {
                // Perfectly uniform - all rays identical
                lengthMod = 1f - lengthVariation * rng.nextFloat(); // Use lengthVariation for slight variation
                angleJitter = randomness * (rng.nextFloat() - 0.5f) * TWO_PI / count * 0.5f; // Slight angle jitter
            }
            case RANDOM -> {
                // Random start position and length within the available range
                float availableRange = outerRadius - innerRadius;
                float maxLength = Math.min(rayLength, availableRange);
                
                // Random length between 50% and 100% of rayLength
                lengthMod = 0.5f + 0.5f * rng.nextFloat();
                float actualLength = maxLength * lengthMod;
                
                // Random start offset within remaining space
                float maxStartOffset = availableRange - actualLength;
                if (maxStartOffset > 0) {
                    startOffset = maxStartOffset * rng.nextFloat();
                }
                
                // More angle jitter
                angleJitter = randomness * (rng.nextFloat() - 0.5f) * TWO_PI / count;
                radiusJitter = randomness * (rng.nextFloat() - 0.5f) * 0.3f;
            }
            case STOCHASTIC -> {
                // Heavily randomized - chaotic distribution
                float availableRange = outerRadius - innerRadius;
                
                // Very random length - from 20% to 100%
                lengthMod = 0.2f + 0.8f * rng.nextFloat();
                float actualLength = rayLength * lengthMod;
                
                // Random start anywhere within the range
                float maxStartOffset = Math.max(0, availableRange - actualLength * 0.3f);
                startOffset = maxStartOffset * rng.nextFloat();
                
                // Strong angular jitter
                angleJitter = (rng.nextFloat() - 0.5f) * TWO_PI / count * 2f;
                radiusJitter = (rng.nextFloat() - 0.5f) * 0.5f;
            }
        }
        
        // Calculate ray start and end positions based on arrangement
        float[] start = new float[3];
        float[] end = new float[3];
        
        switch (arrangement) {
            case RADIAL -> {
                // 2D star pattern on XZ plane - layers stack vertically
                float angle = (index * TWO_PI / count) + angleJitter;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                float layerY = (layerIndex - (shape.layers() - 1) / 2.0f) * layerSpacing;
                
                float innerR = innerRadius + startOffset;
                float outerR = innerR + rayLength * lengthMod;
                innerR *= (1 + radiusJitter);
                outerR *= (1 + radiusJitter);
                
                start[0] = cos * innerR;
                start[1] = layerY;
                start[2] = sin * innerR;
                
                end[0] = cos * outerR;
                end[1] = layerY;
                end[2] = sin * outerR;
            }
            
            case SPHERICAL, DIVERGING -> {
                // 3D fibonacci lattice for uniform sphere distribution
                // Layers create concentric shells with increasing radius
                float shellOffset = layerIndex * layerSpacing;
                
                float phi = (float) Math.acos(1 - 2 * (index + 0.5f) / count);
                float theta = TWO_PI * index / GOLDEN_RATIO + angleJitter;
                
                float sinPhi = (float) Math.sin(phi);
                float cosPhi = (float) Math.cos(phi);
                float cosTheta = (float) Math.cos(theta);
                float sinTheta = (float) Math.sin(theta);
                
                // Direction vector
                float dx = sinPhi * cosTheta;
                float dy = cosPhi;
                float dz = sinPhi * sinTheta;
                
                float innerR = (innerRadius + shellOffset + startOffset) * (1 + radiusJitter);
                float outerR = innerR + rayLength * lengthMod;
                
                // Start at inner radius, end at outer radius (diverging outward)
                start[0] = dx * innerR;
                start[1] = dy * innerR;
                start[2] = dz * innerR;
                
                end[0] = dx * outerR;
                end[1] = dy * outerR;
                end[2] = dz * outerR;
            }
            
            case CONVERGING -> {
                // Same as SPHERICAL but rays point inward
                float shellOffset = layerIndex * layerSpacing;
                
                float phi = (float) Math.acos(1 - 2 * (index + 0.5f) / count);
                float theta = TWO_PI * index / GOLDEN_RATIO + angleJitter;
                
                float sinPhi = (float) Math.sin(phi);
                float cosPhi = (float) Math.cos(phi);
                float cosTheta = (float) Math.cos(theta);
                float sinTheta = (float) Math.sin(theta);
                
                float dx = sinPhi * cosTheta;
                float dy = cosPhi;
                float dz = sinPhi * sinTheta;
                
                float outerR = (outerRadius + shellOffset) * (1 + radiusJitter);
                float innerR = outerR - rayLength * lengthMod;
                
                // Apply startOffset (pushes the whole ray outward)
                outerR += startOffset;
                innerR += startOffset;
                
                if (innerR < 0) innerR = 0;
                
                // Start at outer, end at inner (converging inward)
                start[0] = dx * outerR;
                start[1] = dy * outerR;
                start[2] = dz * outerR;
                
                end[0] = dx * innerR;
                end[1] = dy * innerR;
                end[2] = dz * innerR;
            }
            
            case PARALLEL -> {
                // Grid of parallel rays pointing along +Y
                // Layers stack in the Z direction
                int gridSize = (int) Math.ceil(Math.sqrt(count));
                int gx = index % gridSize;
                int gz = index / gridSize;
                
                float spacing = outerRadius * 2 / gridSize;
                float x = (gx - gridSize / 2.0f + 0.5f) * spacing;
                float z = (gz - gridSize / 2.0f + 0.5f) * spacing + layerIndex * layerSpacing;
                
                // Apply randomness to position
                x += angleJitter * spacing; // Reuse angleJitter for X position
                z += radiusJitter * spacing; // Reuse radiusJitter for Z position
                
                float yStart = -rayLength * lengthMod / 2 + startOffset;
                float yEnd = rayLength * lengthMod / 2 + startOffset;
                
                start[0] = x;
                start[1] = yStart;
                start[2] = z;
                
                end[0] = x;
                end[1] = yEnd;
                end[2] = z;
            }
        }
        
        // Handle segmentation (dashed lines)
        if (shape.isSegmented()) {
            tessellateSegmentedRay(builder, start, end, shape.segments(), shape.segmentGap());
        } else {
            // Single line segment with UV for fade support
            // u = t value (0 at start, 1 at end) for alpha interpolation
            int v0 = builder.vertex(start[0], start[1], start[2], 0, 0, 0, 0.0f, 0);
            int v1 = builder.vertex(end[0], end[1], end[2], 0, 0, 0, 1.0f, 0);
            builder.line(v0, v1);
        }
    }
    
    /**
     * Tessellates a segmented ray (dashed line effect).
     */
    private static void tessellateSegmentedRay(MeshBuilder builder, float[] start, float[] end,
                                                int segments, float gap) {
        // Calculate segment length accounting for gaps
        float totalGap = gap * (segments - 1);
        float segmentFraction = (1.0f - totalGap) / segments;
        float gapFraction = gap;
        
        float[] dir = new float[] {
            end[0] - start[0],
            end[1] - start[1],
            end[2] - start[2]
        };
        
        float t = 0;
        for (int seg = 0; seg < segments; seg++) {
            float segStart = t;
            float segEnd = t + segmentFraction;
            
            float x0 = start[0] + dir[0] * segStart;
            float y0 = start[1] + dir[1] * segStart;
            float z0 = start[2] + dir[2] * segStart;
            
            float x1 = start[0] + dir[0] * segEnd;
            float y1 = start[1] + dir[1] * segEnd;
            float z1 = start[2] + dir[2] * segEnd;
            
            // Store t values in UV for fade support (u = position along ray 0-1)
            int v0 = builder.vertex(x0, y0, z0, 0, 0, 0, segStart, 0);
            int v1 = builder.vertex(x1, y1, z1, 0, 0, 0, segEnd, 0);
            builder.line(v0, v1);
            
            t = segEnd + gapFraction;
        }
    }
}
