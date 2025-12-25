package net.cyberpunk042.client.visual.mesh;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.shape.RayArrangement;
import net.cyberpunk042.visual.shape.RayCurvature;
import net.cyberpunk042.visual.shape.RayDistribution;
import net.cyberpunk042.visual.shape.RayLineShape;
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
        
        // Determine if we need multi-segment tessellation
        // Needed when: lineShape is not STRAIGHT, curvature is not NONE, or shapeSegments > 1
        RayLineShape lineShape = shape.effectiveLineShape();
        RayCurvature curvature = shape.effectiveCurvature();
        int shapeSegments = shape.effectiveShapeSegments();
        
        boolean needsMultiSegment = lineShape != RayLineShape.STRAIGHT 
                                 || curvature != RayCurvature.NONE 
                                 || shapeSegments > 1;
        boolean isSegmented = shape.isSegmented();
        
        if (isSegmented && needsMultiSegment) {
            // COMBINED: Segmented (dashed) ray WITH complex shape/curvature
            // Each dash segment gets its own shaped path
            tessellateSegmentedShapedRay(builder, shape, start, end, lineShape, curvature, shapeSegments);
        } else if (isSegmented) {
            // Simple dashed ray (straight line segments with gaps)
            tessellateSegmentedRay(builder, start, end, shape.segments(), shape.segmentGap());
        } else if (needsMultiSegment) {
            // Multi-segment ray for complex shapes/curvature (continuous, no gaps)
            tessellateShapedRay(builder, shape, start, end, lineShape, curvature, shapeSegments);
        } else {
            // Simple single line segment (2 vertices)
            // u = t value (0 at start, 1 at end) for alpha interpolation
            // Normal stores the ray axis direction for Twist animation
            float dx = end[0] - start[0];
            float dy = end[1] - start[1];
            float dz = end[2] - start[2];
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > 0.0001f) {
                dx /= len; dy /= len; dz /= len;
            }
            int v0 = builder.vertex(start[0], start[1], start[2], dx, dy, dz, 0.0f, 0);
            int v1 = builder.vertex(end[0], end[1], end[2], dx, dy, dz, 1.0f, 0);
            builder.line(v0, v1);
        }
    }
    
    // =========================================================================
    // Segmented + Shaped Ray Tessellation
    // =========================================================================
    
    /**
     * Tessellates a ray that is BOTH segmented (dashed) AND has a complex shape.
     * 
     * <p>This emits multiple separate line strips, each following the shape path
     * but with gaps between them.
     */
    private static void tessellateSegmentedShapedRay(MeshBuilder builder, RaysShape shape,
                                                      float[] start, float[] end,
                                                      RayLineShape lineShape, RayCurvature curvature,
                                                      int verticesPerSegment) {
        int numSegments = shape.segments();
        float gap = shape.segmentGap();
        
        // Calculate t-range for each segment
        float totalGap = gap * (numSegments - 1);
        float segmentFraction = (1.0f - totalGap) / numSegments;
        
        // Compute ray properties once
        float dx = end[0] - start[0];
        float dy = end[1] - start[1];
        float dz = end[2] - start[2];
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.0001f) return;
        
        dx /= length; dy /= length; dz /= length;
        
        float[] right = new float[3];
        float[] up = new float[3];
        computePerpendicularFrame(dx, dy, dz, right, up);
        
        float shapeAmplitude = shape.lineShapeAmplitude();
        float shapeFrequency = shape.lineShapeFrequency();
        float curvatureIntensity = shape.curvatureIntensity();
        
        // How many vertices per dash segment (minimum 2)
        int vertsPerDash = Math.max(2, verticesPerSegment / numSegments);
        
        // DOUBLE_HELIX: emit TWO intertwined helixes
        int helixCount = (lineShape == RayLineShape.DOUBLE_HELIX) ? 2 : 1;
        
        for (int helix = 0; helix < helixCount; helix++) {
            float phaseOffset = helix * PI;
            
            float tOffset = 0;
            for (int seg = 0; seg < numSegments; seg++) {
                float tStart = tOffset;
                float tEnd = tOffset + segmentFraction;
                
                // Emit vertices for this dash segment
                int[] verts = new int[vertsPerDash];
                for (int i = 0; i < vertsPerDash; i++) {
                    float localT = (float) i / (vertsPerDash - 1); // 0 to 1 within this dash
                    float globalT = tStart + localT * segmentFraction; // Global t along entire ray
                    
                    // Apply curvature
                    float[] curvedPos = computeCurvedPosition(start, end, globalT, curvature, curvatureIntensity);
                    float px = curvedPos[0];
                    float py = curvedPos[1];
                    float pz = curvedPos[2];
                    
                    // Apply line shape offset with phase for double helix
                    float[] offset = computeLineShapeOffsetWithPhase(
                        lineShape, globalT, shapeAmplitude, shapeFrequency, right, up, phaseOffset
                    );
                    px += offset[0];
                    py += offset[1];
                    pz += offset[2];
                    
                    // Store ray axis direction in normal for Twist animation
                    verts[i] = builder.vertex(px, py, pz, dx, dy, dz, globalT, 0);
                }
                
                // Connect vertices within this dash
                for (int i = 0; i < vertsPerDash - 1; i++) {
                    builder.line(verts[i], verts[i + 1]);
                }
                
                tOffset = tEnd + gap;
            }
        }
    }
    
    // =========================================================================
    // Multi-Segment Ray Tessellation
    // =========================================================================
    
    /**
     * Tessellates a ray with multiple segments for complex shapes and curvature.
     * 
     * <p>This creates (shapeSegments + 1) vertices along the ray, applying:
     * <ul>
     *   <li>RayCurvature: Modifies ray direction (VORTEX, SPIRAL_ARM, etc.)</li>
     *   <li>RayLineShape: Applies offset perpendicular to ray (CORKSCREW, SINE_WAVE, etc.)</li>
     * </ul>
     * 
     * <p>Each vertex stores its parametric t-value in UV.u for animation use.
     */
    private static void tessellateShapedRay(MeshBuilder builder, RaysShape shape,
                                             float[] start, float[] end,
                                             RayLineShape lineShape, RayCurvature curvature,
                                             int segments) {
        // Compute ray direction
        float dx = end[0] - start[0];
        float dy = end[1] - start[1];
        float dz = end[2] - start[2];
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        if (length < 0.0001f) {
            // Degenerate ray, just emit a point
            builder.vertex(start[0], start[1], start[2], 0, 0, 0, 0.0f, 0);
            return;
        }
        
        // Normalize direction
        dx /= length;
        dy /= length;
        dz /= length;
        
        // Compute perpendicular frame (right, up) for shape offsets
        float[] right = new float[3];
        float[] up = new float[3];
        computePerpendicularFrame(dx, dy, dz, right, up);
        
        // Get shape parameters
        float shapeAmplitude = shape.lineShapeAmplitude();
        float shapeFrequency = shape.lineShapeFrequency();
        float curvatureIntensity = shape.curvatureIntensity();
        
        // DOUBLE_HELIX: emit TWO intertwined helixes (like DNA)
        int helixCount = (lineShape == RayLineShape.DOUBLE_HELIX) ? 2 : 1;
        
        for (int helix = 0; helix < helixCount; helix++) {
            // Phase offset: 0 for first helix, PI for second (180° apart)
            float phaseOffset = helix * PI;
            
            // Generate vertices for each segment point
            int[] vertexIndices = new int[segments + 1];
            
            for (int i = 0; i <= segments; i++) {
                float t = (float) i / segments; // 0 to 1
                
                // Apply curvature to compute curved ray path
                float[] curvedPos = computeCurvedPosition(
                    start, end, t, curvature, curvatureIntensity
                );
                float px = curvedPos[0];
                float py = curvedPos[1];
                float pz = curvedPos[2];
                
                // Apply line shape offset with phase offset for double helix
                float[] offset = computeLineShapeOffsetWithPhase(
                    lineShape, t, shapeAmplitude, shapeFrequency, right, up, phaseOffset
                );
                px += offset[0];
                py += offset[1];
                pz += offset[2];
                
                // Store vertex with t in UV.u and RAY AXIS DIRECTION in normal
                // The normal stores the ray's central axis direction for Twist animation
                vertexIndices[i] = builder.vertex(px, py, pz, dx, dy, dz, t, 0);
            }
            
            // Connect vertices with line segments
            for (int i = 0; i < segments; i++) {
                builder.line(vertexIndices[i], vertexIndices[i + 1]);
            }
        }
    }
    
    /**
     * Computes a curved position along the ray based on curvature mode.
     * 
     * <p>For NONE, returns linear interpolation between start and end.
     * Other modes curve the ray path around the origin.
     * 
     * @param start Ray start position
     * @param end Ray end position
     * @param t Parametric position (0-1)
     * @param curvature Curvature mode
     * @param intensity How much curvature to apply (0 = straight, 1 = full)
     * @return Curved position [x, y, z]
     */
    private static float[] computeCurvedPosition(float[] start, float[] end, float t,
                                                  RayCurvature curvature, float intensity) {
        // Linear interpolation (no curvature)
        float linearX = start[0] + (end[0] - start[0]) * t;
        float linearY = start[1] + (end[1] - start[1]) * t;
        float linearZ = start[2] + (end[2] - start[2]) * t;
        
        if (curvature == null || curvature == RayCurvature.NONE || intensity <= 0.0001f) {
            return new float[]{linearX, linearY, linearZ};
        }
        
        // For curvature, we bend the ray around the Y axis
        // Start position determines the base angle
        float startAngle = (float) Math.atan2(start[2], start[0]);
        float endAngle = (float) Math.atan2(end[2], end[0]);
        float startRadius = (float) Math.sqrt(start[0] * start[0] + start[2] * start[2]);
        float endRadius = (float) Math.sqrt(end[0] * end[0] + end[2] * end[2]);
        
        // Interpolated radius and y-position
        float radius = startRadius + (endRadius - startRadius) * t;
        float y = start[1] + (end[1] - start[1]) * t;
        
        // Compute angle change based on curvature mode
        // NOTE: intensity is applied ONCE here, not again in computeCurvatureAngle
        float angleDelta = computeCurvatureAngle(curvature, t, endRadius - startRadius);
        float angle = startAngle + (endAngle - startAngle) * t + angleDelta * intensity;
        
        // Curved position
        float curvedX = radius * (float) Math.cos(angle);
        float curvedZ = radius * (float) Math.sin(angle);
        
        // Blend between linear and curved based on intensity
        float x = linearX + (curvedX - linearX) * intensity;
        float z = linearZ + (curvedZ - linearZ) * intensity;
        
        return new float[]{x, y, z};
    }
    
    /**
     * Computes the angle offset for a given curvature mode.
     */
    /**
     * Computes the base angle offset for a given curvature mode.
     * Intensity is applied by the caller, not here.
     */
    private static float computeCurvatureAngle(RayCurvature curvature, float t, float rayLength) {
        return switch (curvature) {
            case VORTEX -> 
                // Whirlpool spiral - angle increases along ray
                t * PI * 0.5f;
            case SPIRAL_ARM -> 
                // Galaxy arm - logarithmic spiral
                t * PI * 0.3f * (1 + t);
            case TANGENTIAL -> 
                // 90° from radial - constant perpendicular bend
                PI * 0.25f * t;
            case LOGARITHMIC -> {
                // Golden ratio spiral - exponential angle growth
                float logT = (float) Math.log(1 + t * (GOLDEN_RATIO - 1));
                yield logT * PI * 0.5f;
            }
            case PINWHEEL -> 
                // Windmill blades - constant angle offset
                PI * 0.2f;
            case ORBITAL -> 
                // Circular orbit - full curve around
                t * PI * 0.75f;
            default -> 0f;
        };
    }
    
    /**
     * Computes a stable perpendicular frame (right, up) for a given direction vector.
     * 
     * <p>Uses the "arbitrary axis algorithm" with Y-preference:
     * <ul>
     *   <li>If direction is mostly vertical, use X as reference</li>
     *   <li>Otherwise, use Y as reference</li>
     * </ul>
     */
    private static void computePerpendicularFrame(float dx, float dy, float dz,
                                                   float[] right, float[] up) {
        // Choose reference axis (avoid parallel to direction)
        float refX, refY, refZ;
        if (Math.abs(dy) > 0.9f) {
            // Direction is mostly vertical, use X axis as reference
            refX = 1; refY = 0; refZ = 0;
        } else {
            // Use Y axis as reference
            refX = 0; refY = 1; refZ = 0;
        }
        
        // right = normalize(direction × reference)
        float rx = dy * refZ - dz * refY;
        float ry = dz * refX - dx * refZ;
        float rz = dx * refY - dy * refX;
        float rmag = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rmag > 0.0001f) {
            right[0] = rx / rmag;
            right[1] = ry / rmag;
            right[2] = rz / rmag;
        } else {
            right[0] = 1; right[1] = 0; right[2] = 0;
        }
        
        // up = normalize(right × direction)
        up[0] = right[1] * dz - right[2] * dy;
        up[1] = right[2] * dx - right[0] * dz;
        up[2] = right[0] * dy - right[1] * dx;
        float umag = (float) Math.sqrt(up[0] * up[0] + up[1] * up[1] + up[2] * up[2]);
        if (umag > 0.0001f) {
            up[0] /= umag;
            up[1] /= umag;
            up[2] /= umag;
        }
    }
    
    /**
     * Computes the offset for a given line shape at parameter t.
     * 
     * <p>The offset is perpendicular to the ray direction, using right and up vectors.
     * 
     * @param lineShape The line shape type
     * @param t Parametric position (0 = start, 1 = end)
     * @param amplitude How pronounced the shape is
     * @param frequency Number of waves/coils along the ray
     * @param right The "right" perpendicular vector
     * @param up The "up" perpendicular vector
     * @return [x, y, z] offset to add to base position
     */
    private static float[] computeLineShapeOffset(RayLineShape lineShape, float t,
                                                   float amplitude, float frequency,
                                                   float[] right, float[] up) {
        float[] offset = new float[3];
        
        if (lineShape == null || lineShape == RayLineShape.STRAIGHT) {
            return offset; // No offset for straight lines
        }
        
        float theta = t * frequency * TWO_PI; // Angle based on position and frequency
        
        switch (lineShape) {
            case SINE_WAVE -> {
                // 2D wave using 'right' vector only
                float wave = amplitude * (float) Math.sin(theta);
                offset[0] = right[0] * wave;
                offset[1] = right[1] * wave;
                offset[2] = right[2] * wave;
            }
            
            case CORKSCREW -> {
                // 3D helix using both right and up
                float cos = (float) Math.cos(theta) * amplitude;
                float sin = (float) Math.sin(theta) * amplitude;
                offset[0] = right[0] * cos + up[0] * sin;
                offset[1] = right[1] * cos + up[1] * sin;
                offset[2] = right[2] * cos + up[2] * sin;
            }
            
            case SPRING -> {
                // SPRING: looser coils than CORKSCREW - uses 0.5x frequency for wider loops
                // Also slightly squashed in one axis for more "bouncy spring" look
                float springTheta = t * frequency * 0.5f * TWO_PI; // Half frequency = wider coils
                float cos = (float) Math.cos(springTheta) * amplitude;
                float sin = (float) Math.sin(springTheta) * amplitude * 0.8f; // Slight squash
                offset[0] = right[0] * cos + up[0] * sin;
                offset[1] = right[1] * cos + up[1] * sin;
                offset[2] = right[2] * cos + up[2] * sin;
            }
            
            case ZIGZAG -> {
                // Triangle wave
                float phase = (t * frequency) % 1.0f;
                float triangle = phase < 0.5f ? (phase * 4 - 1) : (3 - phase * 4);
                float wave = amplitude * triangle;
                offset[0] = right[0] * wave;
                offset[1] = right[1] * wave;
                offset[2] = right[2] * wave;
            }
            
            case SAWTOOTH -> {
                // Sawtooth wave (ramp up, drop)
                float phase = (t * frequency) % 1.0f;
                float wave = amplitude * (phase * 2 - 1);
                offset[0] = right[0] * wave;
                offset[1] = right[1] * wave;
                offset[2] = right[2] * wave;
            }
            
            case SQUARE_WAVE -> {
                // Step function
                float wave = amplitude * (Math.sin(theta) > 0 ? 1f : -1f);
                offset[0] = right[0] * wave;
                offset[1] = right[1] * wave;
                offset[2] = right[2] * wave;
            }
            
            case ARC -> {
                // Single bow curve: sin(t * π)
                float curve = amplitude * (float) Math.sin(t * PI);
                offset[0] = up[0] * curve; // Arc in the 'up' direction
                offset[1] = up[1] * curve;
                offset[2] = up[2] * curve;
            }
            
            case S_CURVE -> {
                // S-shape: sin(t * 2π)
                float curve = amplitude * (float) Math.sin(t * TWO_PI);
                offset[0] = right[0] * curve;
                offset[1] = right[1] * curve;
                offset[2] = right[2] * curve;
            }
            

            case DOUBLE_HELIX -> {
                // DOUBLE_HELIX: Two interleaved helixes 180° apart, like DNA
                // This ray is helix A. For helix B, the arrangement should emit a second ray
                // with a phaseOffset of PI (handled at arrangement level, not here).
                // The tessellator emits ONE helix; the RaysShape.count effectively doubles
                // if double-helix is desired (count/2 pairs of rays).
                float cos = (float) Math.cos(theta) * amplitude;
                float sin = (float) Math.sin(theta) * amplitude;
                offset[0] = right[0] * cos + up[0] * sin;
                offset[1] = right[1] * cos + up[1] * sin;
                offset[2] = right[2] * cos + up[2] * sin;
            }
            
            default -> {
                // STRAIGHT or unknown - no offset
            }
        }
        
        return offset;
    }
    
    /**
     * Computes line shape offset with an additional phase offset.
     * Used for DOUBLE_HELIX to create the 180° apart second strand.
     */
    private static float[] computeLineShapeOffsetWithPhase(RayLineShape lineShape, float t,
                                                            float amplitude, float frequency,
                                                            float[] right, float[] up, float phaseOffset) {
        float[] offset = new float[3];
        
        if (lineShape == null || lineShape == RayLineShape.STRAIGHT) {
            return offset;
        }
        
        // Base theta with phase offset (for double helix: 0 or PI)
        float theta = t * frequency * TWO_PI + phaseOffset;
        
        // For DOUBLE_HELIX, treat it as CORKSCREW with phase offset
        if (lineShape == RayLineShape.DOUBLE_HELIX) {
            float cos = (float) Math.cos(theta) * amplitude;
            float sin = (float) Math.sin(theta) * amplitude;
            offset[0] = right[0] * cos + up[0] * sin;
            offset[1] = right[1] * cos + up[1] * sin;
            offset[2] = right[2] * cos + up[2] * sin;
            return offset;
        }
        
        // For other shapes, delegate to the original function (no phase)
        return computeLineShapeOffset(lineShape, t, amplitude, frequency, right, up);
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
        
        // Compute normalized ray axis direction for normal storage
        float len = (float) Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
        float nx = 0, ny = 0, nz = 0;
        if (len > 0.0001f) {
            nx = dir[0] / len;
            ny = dir[1] / len;
            nz = dir[2] / len;
        }
        
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
            // Normal stores ray axis direction for Twist animation
            int v0 = builder.vertex(x0, y0, z0, nx, ny, nz, segStart, 0);
            int v1 = builder.vertex(x1, y1, z1, nx, ny, nz, segEnd, 0);
            builder.line(v0, v1);
            
            t = segEnd + gapFraction;
        }
    }
}
