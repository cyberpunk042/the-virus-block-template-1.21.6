package net.cyberpunk042.client.visual.render;

import net.minecraft.client.render.RenderLayer;

/**
 * Placeholder for GPU wave render layer.
 * 
 * <p>GPU wave requires custom shader integration. Until the 1.21.6 API
 * is confirmed, this just returns the standard translucent layer.</p>
 * 
 * <p>Wave deformation currently uses CPU mode (in tessellators).</p>
 */
public final class WaveRenderLayer {
    
    private WaveRenderLayer() {}
    
    /**
     * Gets the wave translucent render layer.
     * <p>Currently returns standard translucent layer - GPU mode not yet implemented.</p>
     * 
     * @return Standard translucent render layer
     */
    public static RenderLayer translucent() {
        // GPU shader not yet available, use standard layer
        return FieldRenderLayers.solidTranslucent();
    }
    
    /**
     * Checks if GPU wave rendering is available.
     * @return false until GPU shader is implemented
     */
    public static boolean isGpuWaveAvailable() {
        return WaveShaderRegistry.isAvailable();
    }
}
