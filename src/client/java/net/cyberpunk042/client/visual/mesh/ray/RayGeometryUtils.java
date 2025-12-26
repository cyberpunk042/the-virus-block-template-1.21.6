package net.cyberpunk042.client.visual.mesh.ray;

import net.cyberpunk042.visual.shape.RayCurvature;
import net.cyberpunk042.visual.shape.RayLineShape;

/**
 * Common geometry utilities for ray tessellation.
 * 
 * <p>Provides reusable functions for:
 * <ul>
 *   <li>Perpendicular frame calculation (right/up vectors)</li>
 *   <li>Line shape offsets (sine wave, corkscrew, zigzag, etc.)</li>
 *   <li>Curvature calculations (vortex, spiral)</li>
 *   <li>Position interpolation</li>
 * </ul>
 * 
 * <p>These utilities are used by all RayTypeTessellator implementations.
 * 
 * @see RayTypeTessellator
 */
public final class RayGeometryUtils {
    
    private static final float PI = (float) Math.PI;
    private static final float TWO_PI = 2 * PI;
    
    private RayGeometryUtils() {} // Utility class
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Position Utilities
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Interpolates between two positions.
     * @param start Start position [x, y, z]
     * @param end End position [x, y, z]
     * @param t Parametric value (0 = start, 1 = end)
     * @return Interpolated position [x, y, z]
     */
    public static float[] interpolate(float[] start, float[] end, float t) {
        return new float[] {
            start[0] + (end[0] - start[0]) * t,
            start[1] + (end[1] - start[1]) * t,
            start[2] + (end[2] - start[2]) * t
        };
    }
    
    /**
     * Computes the direction and length from start to end.
     * @param start Start position
     * @param end End position
     * @return [dx, dy, dz, length] - normalized direction + length
     */
    public static float[] computeDirectionAndLength(float[] start, float[] end) {
        float dx = end[0] - start[0];
        float dy = end[1] - start[1];
        float dz = end[2] - start[2];
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        if (length > 0.0001f) {
            return new float[] { dx / length, dy / length, dz / length, length };
        }
        return new float[] { 0, 1, 0, 0 }; // Default up, zero length
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Frame Calculation
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Computes a stable perpendicular frame (right, up) for a given direction vector.
     * 
     * <p>Uses the "arbitrary axis algorithm" with Y-preference:
     * <ul>
     *   <li>If direction is mostly vertical, use X as reference</li>
     *   <li>Otherwise, use Y as reference</li>
     * </ul>
     * 
     * @param dx Direction X
     * @param dy Direction Y
     * @param dz Direction Z
     * @param right Output: the "right" perpendicular vector [3]
     * @param up Output: the "up" perpendicular vector [3]
     */
    public static void computePerpendicularFrame(float dx, float dy, float dz,
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
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Line Shape Offsets
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Computes the offset for a given line shape at parameter t.
     * 
     * @param lineShape The line shape type
     * @param t Parametric position (0 = start, 1 = end)
     * @param amplitude How pronounced the shape is
     * @param frequency Number of waves/coils along the ray
     * @param right The "right" perpendicular vector
     * @param up The "up" perpendicular vector
     * @return [x, y, z] offset to add to base position
     */
    public static float[] computeLineShapeOffset(RayLineShape lineShape, float t,
                                                  float amplitude, float frequency,
                                                  float[] right, float[] up) {
        return computeLineShapeOffsetWithPhase(lineShape, t, amplitude, frequency, right, up, 0f);
    }
    
    /**
     * Computes line shape offset with an additional phase offset.
     * Used for DOUBLE_HELIX to create the 180° apart second strand.
     */
    public static float[] computeLineShapeOffsetWithPhase(RayLineShape lineShape, float t,
                                                           float amplitude, float frequency,
                                                           float[] right, float[] up, float phaseOffset) {
        float[] offset = new float[3];
        
        if (lineShape == null || lineShape == RayLineShape.STRAIGHT) {
            return offset;
        }
        
        float theta = t * frequency * TWO_PI + phaseOffset;
        
        switch (lineShape) {
            case SINE_WAVE -> {
                float wave = amplitude * (float) Math.sin(theta);
                offset[0] = right[0] * wave;
                offset[1] = right[1] * wave;
                offset[2] = right[2] * wave;
            }
            
            case CORKSCREW, DOUBLE_HELIX -> {
                float cos = (float) Math.cos(theta) * amplitude;
                float sin = (float) Math.sin(theta) * amplitude;
                offset[0] = right[0] * cos + up[0] * sin;
                offset[1] = right[1] * cos + up[1] * sin;
                offset[2] = right[2] * cos + up[2] * sin;
            }
            
            case SPRING -> {
                float springTheta = t * frequency * 0.5f * TWO_PI + phaseOffset;
                float cos = (float) Math.cos(springTheta) * amplitude;
                float sin = (float) Math.sin(springTheta) * amplitude * 0.8f;
                offset[0] = right[0] * cos + up[0] * sin;
                offset[1] = right[1] * cos + up[1] * sin;
                offset[2] = right[2] * cos + up[2] * sin;
            }
            
            case ZIGZAG -> {
                float phase = (t * frequency) % 1.0f;
                float triangle = phase < 0.5f ? (phase * 4 - 1) : (3 - phase * 4);
                float wave = amplitude * triangle;
                offset[0] = right[0] * wave;
                offset[1] = right[1] * wave;
                offset[2] = right[2] * wave;
            }
            
            case SAWTOOTH -> {
                float phase = (t * frequency) % 1.0f;
                float wave = amplitude * (phase * 2 - 1);
                offset[0] = right[0] * wave;
                offset[1] = right[1] * wave;
                offset[2] = right[2] * wave;
            }
            
            case SQUARE_WAVE -> {
                float wave = amplitude * (Math.sin(theta) > 0 ? 1f : -1f);
                offset[0] = right[0] * wave;
                offset[1] = right[1] * wave;
                offset[2] = right[2] * wave;
            }
            
            case ARC -> {
                float curve = amplitude * (float) Math.sin(t * PI);
                offset[0] = up[0] * curve;
                offset[1] = up[1] * curve;
                offset[2] = up[2] * curve;
            }
            
            case S_CURVE -> {
                float curve = amplitude * (float) Math.sin(t * TWO_PI);
                offset[0] = right[0] * curve;
                offset[1] = right[1] * curve;
                offset[2] = right[2] * curve;
            }
            
            default -> {} // STRAIGHT or unknown
        }
        
        return offset;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Curvature Calculations
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Computes a curved position along the ray based on curvature mode.
     * 
     * @param start Ray start position
     * @param end Ray end position
     * @param t Parametric value (0-1)
     * @param curvature Curvature mode
     * @param intensity Curvature intensity
     * @return Curved position [x, y, z]
     */
    public static float[] computeCurvedPosition(float[] start, float[] end, float t,
                                                 RayCurvature curvature, float intensity) {
        if (curvature == null || curvature == RayCurvature.NONE || intensity < 0.001f) {
            return interpolate(start, end, t);
        }
        
        float[] pos = interpolate(start, end, t);
        float angle = computeCurvatureAngle(curvature, t) * intensity;
        
        // Rotate around Y axis (simplified curvature)
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float x = pos[0] * cos - pos[2] * sin;
        float z = pos[0] * sin + pos[2] * cos;
        pos[0] = x;
        pos[2] = z;
        
        return pos;
    }
    
    /**
     * Computes the tangent direction at a point on a curved path.
     * 
     * <p>The tangent is computed by sampling two nearby points on the curve
     * and computing the normalized direction between them.</p>
     * 
     * @param start Ray start position
     * @param end Ray end position
     * @param t Parametric value (0-1)
     * @param curvature Curvature mode
     * @param intensity Curvature intensity
     * @return Tangent direction [x, y, z] (normalized)
     */
    public static float[] computeCurvedTangent(float[] start, float[] end, float t,
                                               RayCurvature curvature, float intensity) {
        if (curvature == null || curvature == RayCurvature.NONE || intensity < 0.001f) {
            // Straight line - tangent is just the direction
            float[] dir = computeDirectionAndLength(start, end);
            return new float[] { dir[0], dir[1], dir[2] };
        }
        
        // Sample two nearby points to compute tangent
        float dt = 0.02f;
        float t1 = Math.max(0, t - dt);
        float t2 = Math.min(1, t + dt);
        
        float[] p1 = computeCurvedPosition(start, end, t1, curvature, intensity);
        float[] p2 = computeCurvedPosition(start, end, t2, curvature, intensity);
        
        // Direction from p1 to p2
        float[] tangent = new float[] {
            p2[0] - p1[0],
            p2[1] - p1[1],
            p2[2] - p1[2]
        };
        normalize(tangent);
        
        return tangent;
    }
    
    /**
     * Computes the base angle offset for a given curvature mode.
     */
    public static float computeCurvatureAngle(RayCurvature curvature, float t) {
        if (curvature == null) return 0;
        
        return switch (curvature) {
            case NONE -> 0;
            case VORTEX -> t * PI * 0.5f;
            case SPIRAL_ARM -> t * PI;
            case TANGENTIAL -> PI * 0.5f; // Always 90 degrees
            case LOGARITHMIC -> (float) Math.log(1 + t * 3) * PI * 0.5f;
            case PINWHEEL -> t * PI * 0.75f;
            case ORBITAL -> t * TWO_PI;
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Vector Math Utilities
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Normalizes a vector in-place.
     * @return The magnitude before normalization
     */
    public static float normalize(float[] v) {
        float mag = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (mag > 0.0001f) {
            v[0] /= mag;
            v[1] /= mag;
            v[2] /= mag;
        }
        return mag;
    }
    
    /**
     * Computes the dot product of two vectors.
     */
    public static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }
    
    /**
     * Computes the cross product: result = a × b
     */
    public static void cross(float[] a, float[] b, float[] result) {
        result[0] = a[1] * b[2] - a[2] * b[1];
        result[1] = a[2] * b[0] - a[0] * b[2];
        result[2] = a[0] * b[1] - a[1] * b[0];
    }
    
    /**
     * Scales a vector: result = v * scale
     */
    public static void scale(float[] v, float scale, float[] result) {
        result[0] = v[0] * scale;
        result[1] = v[1] * scale;
        result[2] = v[2] * scale;
    }
    
    /**
     * Adds two vectors: result = a + b
     */
    public static void add(float[] a, float[] b, float[] result) {
        result[0] = a[0] + b[0];
        result[1] = a[1] + b[1];
        result[2] = a[2] + b[2];
    }
}
