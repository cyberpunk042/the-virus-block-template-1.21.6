package net.cyberpunk042.client.visual.animation;

import net.cyberpunk042.visual.animation.WaveConfig;
import net.minecraft.util.math.MathHelper;

/**
 * Applies wave deformation to vertex positions.
 * 
 * <p>Wave deformation displaces vertices based on a sine wave function,
 * creating ripple/pulse effects on mesh surfaces.</p>
 * 
 * <h2>Wave Direction</h2>
 * <ul>
 *   <li><b>Y</b>: Wave propagates vertically, displacement is radial in XZ plane</li>
 *   <li><b>X</b>: Wave propagates along X, displacement is in YZ plane</li>
 *   <li><b>Z</b>: Wave propagates along Z, displacement is in XY plane</li>
 * </ul>
 * 
 * @see WaveConfig
 */
public final class WaveDeformer {
    
    private WaveDeformer() {} // Static utility class
    
    /**
     * Applies wave deformation to a vertex position.
     * 
     * @param x Original X position
     * @param y Original Y position
     * @param z Original Z position
     * @param wave Wave configuration
     * @param time Current time in ticks
     * @return Array of 3 floats [x, y, z] with wave applied
     */
    public static float[] apply(float x, float y, float z, WaveConfig wave, float time) {
        if (wave == null || !wave.isActive()) {
            return new float[]{x, y, z};
        }
        
        // Only apply if CPU mode
        if (!wave.isCpuMode()) {
            return new float[]{x, y, z};
        }
        
        // Use modulo to prevent sin overflow with large time values
        float safeTime = time % 1000f;
        
        // Calculate wave direction vector
        float dirX, dirY, dirZ;
        switch (wave.direction()) {
            case X -> { dirX = 1; dirY = 0; dirZ = 0; }
            case Z -> { dirX = 0; dirY = 0; dirZ = 1; }
            default -> { dirX = 0; dirY = 1; dirZ = 0; } // Y is default
        }
        
        // Calculate phase: position along wave direction × frequency + time × speed
        float phase = (x * dirX + y * dirY + z * dirZ) * wave.frequency() + safeTime * wave.speed();
        float displacement = MathHelper.sin(phase) * wave.amplitude();
        
        // Apply displacement radially (perpendicular to wave direction)
        return applyRadialDisplacement(x, y, z, displacement, wave.direction());
    }
    
    /**
     * Applies radial displacement based on wave direction.
     */
    private static float[] applyRadialDisplacement(float x, float y, float z, 
                                                    float displacement, 
                                                    net.cyberpunk042.visual.animation.Axis direction) {
        switch (direction) {
            case Y -> {
                // Wave travels vertically, displacement is radial in XZ plane
                float distXZ = (float) Math.sqrt(x * x + z * z);
                if (distXZ > 0.001f) {
                    float nx = x / distXZ;
                    float nz = z / distXZ;
                    return new float[]{
                        x + nx * displacement,
                        y,
                        z + nz * displacement
                    };
                }
            }
            case X -> {
                // Wave travels along X, displacement is radial in YZ plane
                float distYZ = (float) Math.sqrt(y * y + z * z);
                if (distYZ > 0.001f) {
                    float ny = y / distYZ;
                    float nz = z / distYZ;
                    return new float[]{
                        x,
                        y + ny * displacement,
                        z + nz * displacement
                    };
                }
            }
            case Z -> {
                // Wave travels along Z, displacement is radial in XY plane
                float distXY = (float) Math.sqrt(x * x + y * y);
                if (distXY > 0.001f) {
                    float nx = x / distXY;
                    float ny = y / distXY;
                    return new float[]{
                        x + nx * displacement,
                        y + ny * displacement,
                        z
                    };
                }
            }
        }
        
        // Fallback: no displacement if near axis
        return new float[]{x, y, z};
    }
    
    /**
     * Convenience method that returns a new Vertex with wave applied.
     * Preserves normal direction.
     */
    public static net.cyberpunk042.client.visual.mesh.Vertex applyToVertex(
            net.cyberpunk042.client.visual.mesh.Vertex vertex, 
            WaveConfig wave, 
            float time) {
        if (wave == null || !wave.isActive() || !wave.isCpuMode()) {
            return vertex;
        }
        
        float[] deformed = apply(vertex.x(), vertex.y(), vertex.z(), wave, time);
        return net.cyberpunk042.client.visual.mesh.Vertex
            .pos(deformed[0], deformed[1], deformed[2])
            .withNormal(vertex.nx(), vertex.ny(), vertex.nz())
            .withUV(vertex.u(), vertex.v());
    }
}
