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
 *   <li>/shockwavegpu radius <n> - set static radius</li>
 *   <li>/shockwavegpu thickness <n> - set ring thickness</li>
 *   <li>/shockwavegpu intensity <n> - set glow intensity</li>
 *   <li>/shockwavegpu speed <n> - set animation speed</li>
 *   <li>/shockwavegpu maxradius <n> - set max animation radius</li>
 * </ul>
 */
public class ShockwavePostEffect {
    
    private static final Identifier SHADER_ID = 
        Identifier.of("the-virus-block", "shockwave_ring");
    
    // Use the full STAGES set to ensure depth is bound
    private static final Set<Identifier> REQUIRED_TARGETS = DefaultFramebufferSet.STAGES;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static boolean enabled = false;
    private static boolean animating = false;
    private static long animationStartTime = 0;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURABLE PARAMETERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static float currentRadius = 20.0f;      // Current ring distance (blocks)
    private static float ringThickness = 4.0f;       // Ring width (blocks)
    private static float intensity = 1.0f;           // Glow intensity (0-2)
    private static float animationSpeed = 15.0f;     // Blocks per second
    private static float maxRadius = 100.0f;         // Auto-stop radius
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void init() {
        Logging.RENDER.topic("shockwave_gpu")
            .info("ShockwavePostEffect initialized (Modern FrameGraph API)");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ENABLE/DISABLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void toggle() {
        enabled = !enabled;
        Logging.RENDER.topic("shockwave_gpu")
            .kv("enabled", enabled)
            .info("ShockwavePostEffect toggled");
    }
    
    public static void setEnabled(boolean state) {
        enabled = state;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void trigger() {
        enabled = true;
        animating = true;
        currentRadius = 0.0f;
        animationStartTime = System.currentTimeMillis();
        Logging.RENDER.topic("shockwave_gpu")
            .kv("speed", animationSpeed)
            .kv("maxRadius", maxRadius)
            .info("Shockwave triggered");
    }
    
    public static boolean isAnimating() {
        return animating;
    }
    
    public static void stopAnimation() {
        animating = false;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PARAMETER SETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void setRadius(float radius) {
        enabled = true;
        animating = false;
        currentRadius = Math.max(0.0f, radius);
        Logging.RENDER.topic("shockwave_gpu")
            .kv("radius", currentRadius)
            .info("Static radius set");
    }
    
    public static void setThickness(float thickness) {
        ringThickness = Math.max(0.5f, Math.min(50.0f, thickness));
        Logging.RENDER.topic("shockwave_gpu")
            .kv("thickness", ringThickness)
            .info("Thickness set");
    }
    
    public static void setIntensity(float value) {
        intensity = Math.max(0.0f, Math.min(3.0f, value));
        Logging.RENDER.topic("shockwave_gpu")
            .kv("intensity", intensity)
            .info("Intensity set");
    }
    
    public static void setSpeed(float speed) {
        animationSpeed = Math.max(1.0f, Math.min(200.0f, speed));
        Logging.RENDER.topic("shockwave_gpu")
            .kv("speed", animationSpeed)
            .info("Animation speed set");
    }
    
    public static void setMaxRadius(float max) {
        maxRadius = Math.max(10.0f, Math.min(500.0f, max));
        Logging.RENDER.topic("shockwave_gpu")
            .kv("maxRadius", maxRadius)
            .info("Max radius set");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PARAMETER GETTERS (for shader uniforms)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static float getCurrentRadius() {
        if (animating) {
            float elapsed = (System.currentTimeMillis() - animationStartTime) / 1000.0f;
            float animRadius = elapsed * animationSpeed;
            
            // Auto-stop when reaching max radius
            if (animRadius >= maxRadius) {
                animating = false;
                enabled = false;
                return maxRadius;
            }
            return animRadius;
        }
        return currentRadius;
    }
    
    public static float getThickness() {
        return ringThickness;
    }
    
    public static float getIntensity() {
        return intensity;
    }
    
    public static float getSpeed() {
        return animationSpeed;
    }
    
    public static float getMaxRadius() {
        return maxRadius;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS STRING (for HUD display)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static String getStatusString() {
        if (!enabled) return "OFF";
        if (animating) {
            return String.format("ANIM r=%.1f spd=%.1f max=%.1f", 
                getCurrentRadius(), animationSpeed, maxRadius);
        }
        return String.format("STATIC r=%.1f t=%.1f i=%.1f", 
            currentRadius, ringThickness, intensity);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHADER LOADING
    // ═══════════════════════════════════════════════════════════════════════════
    
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

