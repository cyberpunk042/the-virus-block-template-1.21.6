package net.cyberpunk042.client.visual.shader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.util.Identifier;
import net.cyberpunk042.log.Logging;

import java.util.Set;

/**
 * Manages the GPU shockwave post-effect.
 * 
 * <p>This uses the MODERN FrameGraphBuilder API to properly access the depth buffer.
 * Unlike the legacy API, this passes depth to the shader correctly.
 * 
 * <p>Commands:
 * <ul>
 *   <li>/shockwavegpu - toggle</li>
 *   <li>/shockwavegpu trigger - start animation</li>
 * </ul>
 */
public class ShockwavePostEffect {
    
    private static final Identifier SHADER_ID = 
        Identifier.of("the-virus-block", "shockwave_ring");
    
    // Use the full STAGES set to ensure depth is bound
    private static final Set<Identifier> REQUIRED_TARGETS = DefaultFramebufferSet.STAGES;
    
    // State
    private static boolean enabled = false;
    private static boolean animating = false;
    private static float currentRadius = 15.0f;
    private static float ringThickness = 3.0f;
    private static float animationSpeed = 10.0f;
    private static long animationStartTime = 0;
    
    /**
     * Initialize the post effect system.
     */
    public static void init() {
        Logging.RENDER.topic("shockwave_gpu")
            .info("ShockwavePostEffect initialized (Modern FrameGraph API)");
    }
    
    /**
     * Check if enabled.
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Toggle enable state.
     */
    public static void toggle() {
        enabled = !enabled;
        Logging.RENDER.topic("shockwave_gpu")
            .kv("enabled", enabled)
            .info("ShockwavePostEffect toggled");
    }
    
    /**
     * Set enabled directly.
     */
    public static void setEnabled(boolean state) {
        enabled = state;
    }
    
    /**
     * Trigger the shockwave animation.
     */
    public static void trigger() {
        enabled = true;
        animating = true;
        currentRadius = 0.0f;
        animationStartTime = System.currentTimeMillis();
        Logging.RENDER.topic("shockwave_gpu")
            .kv("speed", animationSpeed)
            .info("Shockwave triggered");
    }
    
    /**
     * Set static radius (no animation).
     */
    public static void setRadius(float radius) {
        enabled = true;
        animating = false;
        currentRadius = Math.max(0.0f, radius);
        Logging.RENDER.topic("shockwave_gpu")
            .kv("radius", currentRadius)
            .info("Static radius set");
    }
    
    /**
     * Get current radius for shader uniforms.
     */
    public static float getCurrentRadius() {
        if (animating) {
            float elapsed = (System.currentTimeMillis() - animationStartTime) / 1000.0f;
            return elapsed * animationSpeed;
        }
        return currentRadius;
    }
    
    /**
     * Get ring thickness for shader uniforms.
     */
    public static float getThickness() {
        return ringThickness;
    }
    
    /**
     * Load the post effect processor.
     */
    public static PostEffectProcessor loadProcessor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return null;
        }
        
        ShaderLoader shaderLoader = client.getShaderLoader();
        if (shaderLoader == null) {
            return null;
        }
        
        try {
            return shaderLoader.loadPostEffect(SHADER_ID, REQUIRED_TARGETS);
        } catch (Exception e) {
            Logging.RENDER.topic("shockwave_gpu")
                .kv("shader", SHADER_ID.toString())
                .kv("error", e.getMessage())
                .error("Failed to load post effect");
            return null;
        }
    }
}
