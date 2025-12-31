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
    
    // Basic params
    private static float currentRadius = 20.0f;      // Current ring distance (blocks)
    private static float ringThickness = 4.0f;       // Ring width (blocks)
    private static float intensity = 1.0f;           // Glow intensity (0-2)
    private static float animationSpeed = 15.0f;     // Blocks per second
    private static float maxRadius = 400.0f;         // Auto-stop radius
    
    // Advanced params
    private static int ringCount = 10;               // Number of concentric rings
    private static float ringSpacing = 8.0f;         // Distance between rings (blocks)
    private static boolean contractMode = false;     // false = expand, true = contract
    
    // Origin mode: CAMERA = rings around player, TARGET = rings around cursor hit point
    public enum OriginMode { CAMERA, TARGET }
    private static OriginMode originMode = OriginMode.CAMERA;
    
    // Target world position (for TARGET mode)
    private static float targetX = 0, targetY = 0, targetZ = 0;
    private static float cameraX = 0, cameraY = 0, cameraZ = 0;
    
    // Screen effects
    private static float blackoutAmount = 0.0f;      // 0 = no blackout, 1 = full black
    private static float vignetteAmount = 0.0f;      // 0 = no vignette, 1 = strong
    private static float vignetteRadius = 0.5f;      // Inner radius of vignette
    private static float tintR = 1.0f, tintG = 1.0f, tintB = 1.0f;  // Tint color
    private static float tintAmount = 0.0f;          // 0 = no tint, 1 = full tint
    
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
            float animRadius;
            
            if (contractMode) {
                // Contract: start from max, go to 0
                animRadius = maxRadius - (elapsed * animationSpeed);
                if (animRadius <= 0.0f) {
                    animating = false;
                    enabled = false;
                    return 0.0f;
                }
            } else {
                // Expand: start from 0, go to max
                animRadius = elapsed * animationSpeed;
                if (animRadius >= maxRadius) {
                    animating = false;
                    enabled = false;
                    return maxRadius;
                }
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
    
    // Advanced param getters
    public static int getRingCount() {
        return ringCount;
    }
    
    public static float getRingSpacing() {
        return ringSpacing;
    }
    
    public static boolean isContractMode() {
        return contractMode;
    }
    
    // Advanced param setters
    public static void setRingCount(int count) {
        ringCount = Math.max(1, Math.min(10, count));
        Logging.RENDER.topic("shockwave_gpu")
            .kv("ringCount", ringCount)
            .info("Ring count set");
    }
    
    public static void setRingSpacing(float spacing) {
        ringSpacing = Math.max(1.0f, Math.min(50.0f, spacing));
        Logging.RENDER.topic("shockwave_gpu")
            .kv("ringSpacing", ringSpacing)
            .info("Ring spacing set");
    }
    
    public static void setContractMode(boolean contract) {
        contractMode = contract;
        Logging.RENDER.topic("shockwave_gpu")
            .kv("contractMode", contractMode)
            .info("Contract mode set");
    }
    
    // Contracting animation
    public static void triggerContract() {
        enabled = true;
        animating = true;
        contractMode = true;
        currentRadius = maxRadius;  // Start from max
        animationStartTime = System.currentTimeMillis();
        Logging.RENDER.topic("shockwave_gpu")
            .kv("from", maxRadius)
            .info("Contract animation triggered");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ORIGIN MODE
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static OriginMode getOriginMode() {
        return originMode;
    }
    
    public static void setOriginMode(OriginMode mode) {
        originMode = mode;
        Logging.RENDER.topic("shockwave_gpu")
            .kv("mode", mode)
            .info("Origin mode set");
    }
    
    public static void setTargetPosition(float x, float y, float z) {
        targetX = x;
        targetY = y;
        targetZ = z;
        originMode = OriginMode.TARGET;
        Logging.RENDER.topic("shockwave_gpu")
            .kv("target", String.format("%.1f, %.1f, %.1f", x, y, z))
            .info("Target position set");
    }
    
    public static void updateCameraPosition(float x, float y, float z) {
        cameraX = x;
        cameraY = y;
        cameraZ = z;
    }
    
    public static float getTargetX() { return targetX; }
    public static float getTargetY() { return targetY; }
    public static float getTargetZ() { return targetZ; }
    public static float getCameraX() { return cameraX; }
    public static float getCameraY() { return cameraY; }
    public static float getCameraZ() { return cameraZ; }
    public static boolean isTargetMode() { return originMode == OriginMode.TARGET; }
    
    /**
     * Trigger at cursor - performs raycast and sets target position.
     * Should be called from command handler with raycast result.
     */
    public static void triggerAtCursor(net.minecraft.util.hit.HitResult hit) {
        if (hit != null && hit.getType() != net.minecraft.util.hit.HitResult.Type.MISS) {
            var pos = hit.getPos();
            setTargetPosition((float)pos.x, (float)pos.y, (float)pos.z);
            trigger();
        } else {
            // No hit - use camera mode
            originMode = OriginMode.CAMERA;
            trigger();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SCREEN EFFECTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static float getBlackoutAmount() { return blackoutAmount; }
    public static float getVignetteAmount() { return vignetteAmount; }
    public static float getVignetteRadius() { return vignetteRadius; }
    public static float getTintR() { return tintR; }
    public static float getTintG() { return tintG; }
    public static float getTintB() { return tintB; }
    public static float getTintAmount() { return tintAmount; }
    
    public static void setBlackout(float amount) {
        blackoutAmount = Math.max(0f, Math.min(1f, amount));
        Logging.RENDER.topic("shockwave_gpu")
            .kv("blackout", blackoutAmount)
            .info("Blackout set");
    }
    
    public static void setVignette(float amount, float radius) {
        vignetteAmount = Math.max(0f, Math.min(1f, amount));
        vignetteRadius = Math.max(0f, Math.min(1f, radius));
        Logging.RENDER.topic("shockwave_gpu")
            .kv("vignette", vignetteAmount)
            .kv("radius", vignetteRadius)
            .info("Vignette set");
    }
    
    public static void setTint(float r, float g, float b, float amount) {
        tintR = Math.max(0f, Math.min(2f, r));
        tintG = Math.max(0f, Math.min(2f, g));
        tintB = Math.max(0f, Math.min(2f, b));
        tintAmount = Math.max(0f, Math.min(1f, amount));
        Logging.RENDER.topic("shockwave_gpu")
            .kv("tint", String.format("%.1f,%.1f,%.1f @ %.1f", tintR, tintG, tintB, tintAmount))
            .info("Tint set");
    }
    
    public static void clearScreenEffects() {
        blackoutAmount = 0f;
        vignetteAmount = 0f;
        tintAmount = 0f;
        Logging.RENDER.topic("shockwave_gpu")
            .info("Screen effects cleared");
    }
    
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

