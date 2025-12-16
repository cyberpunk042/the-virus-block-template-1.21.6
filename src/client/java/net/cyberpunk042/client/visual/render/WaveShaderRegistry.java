package net.cyberpunk042.client.visual.render;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.WaveConfig;

/**
 * Placeholder for GPU wave shader registration.
 * 
 * <p><b>API Status:</b> The Fabric API's {@code CoreShaderRegistrationCallback} 
 * was removed/moved in 1.21.6. Custom shader registration now requires Mixins
 * to inject into GameRenderer's shader loading.</p>
 * 
 * <p><b>Current Status:</b> Wave deformation uses CPU mode (tessellator-level 
 * deformation), which works well and provides the same visual effect.
 * GPU mode can be implemented later using Mixins.</p>
 * 
 * <h2>GPU Implementation Path (Future)</h2>
 * <ol>
 *   <li>Create Mixin for GameRenderer.preloadPrograms()</li>
 *   <li>Load custom wave_solid shader program</li>
 *   <li>Store reference for uniform updates</li>
 *   <li>Switch render layer to use GPU shader when wave.isGpuMode()</li>
 * </ol>
 */
public final class WaveShaderRegistry {
    
    private static boolean initialized = false;
    private static boolean gpuAvailable = false;
    
    private WaveShaderRegistry() {}
    
    /**
     * Initializes the wave shader system.
     * GPU mode not yet available - requires Mixin approach in 1.21.6.
     */
    public static void register() {
        if (initialized) return;
        initialized = true;
        
        // GPU shader registration requires Mixin in 1.21.6 (CoreShaderRegistrationCallback removed)
        // For now, wave deformation uses CPU mode (in tessellators)
        Logging.GUI.topic("shader")
            .info("[WaveShader] GPU mode requires Mixin - using CPU mode (works well!)");
        
        gpuAvailable = false;
    }
    
    /**
     * Checks if GPU wave rendering is available.
     * @return false until GPU shader is implemented via Mixin
     */
    public static boolean isAvailable() {
        return gpuAvailable;
    }
    
    /**
     * Sets wave uniforms (no-op until GPU implemented).
     */
    public static void setWaveUniforms(WaveConfig wave, float time) {
        // No-op until GPU shader is implemented via Mixin
    }
}
