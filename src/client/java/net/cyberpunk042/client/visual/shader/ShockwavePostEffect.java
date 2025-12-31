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
    // STATE RECORDS - Group related parameters for cleaner code
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Ring appearance and animation parameters.
     */
    public record RingParams(
        float thickness,      // Ring width (blocks)
        float intensity,      // Glow intensity (0-2)
        float animationSpeed, // Blocks per second
        float maxRadius,      // Auto-stop radius
        int count,            // Number of concentric rings
        float spacing,        // Distance between rings (blocks)
        float glowWidth,      // Glow falloff width (blocks)
        boolean contractMode  // false = expand, true = contract
    ) {
        public static final RingParams DEFAULT = new RingParams(
            4.0f, 1.0f, 15.0f, 400.0f, 10, 8.0f, 8.0f, false
        );
    }
    
    /**
     * Ring coloring (RGB + opacity).
     */
    public record RingColor(float r, float g, float b, float opacity) {
        public static final RingColor DEFAULT = new RingColor(0f, 1f, 1f, 1f); // Cyan
    }
    
    /**
     * Screen-wide post-processing effects.
     */
    public record ScreenEffects(
        float blackout,        // 0 = no blackout, 1 = full black
        float vignetteAmount,  // 0 = no vignette, 1 = strong
        float vignetteRadius,  // Inner radius of vignette
        float tintR, float tintG, float tintB,
        float tintAmount       // 0 = no tint, 1 = full tint
    ) {
        public static final ScreenEffects NONE = new ScreenEffects(0f, 0f, 0.5f, 1f, 1f, 1f, 0f);
    }
    
    /**
     * Camera position and orientation state.
     */
    public record CameraState(
        float x, float y, float z,             // Current camera position
        float forwardX, float forwardY, float forwardZ,  // View direction
        float frozenX, float frozenY, float frozenZ      // Frozen at raycast time
    ) {
        public static final CameraState ORIGIN = new CameraState(0,0,0, 0,0,1, 0,0,0);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════════

    
    private static boolean enabled = false;
    private static boolean animating = false;
    private static long animationStartTime = 0;
    
    // Current animated radius (separate from params because it changes during animation)
    private static float currentRadius = 20.0f;
    
    // State records - the primary state storage
    private static RingParams ringParams = RingParams.DEFAULT;
    private static RingColor ringColor = RingColor.DEFAULT;
    private static ScreenEffects screenEffects = ScreenEffects.NONE;
    private static CameraState cameraState = CameraState.ORIGIN;
    
    // Origin mode: CAMERA = rings around player, TARGET = rings around cursor hit point
    public enum OriginMode { CAMERA, TARGET }
    private static OriginMode originMode = OriginMode.CAMERA;
    
    // Target world position (for TARGET mode)
    private static float targetX = 0, targetY = 0, targetZ = 0;
    
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
            .kv("speed", ringParams.animationSpeed())
            .kv("maxRadius", ringParams.maxRadius())
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
        float t = Math.max(0.5f, Math.min(50.0f, thickness));
        ringParams = new RingParams(t, ringParams.intensity(), ringParams.animationSpeed(),
            ringParams.maxRadius(), ringParams.count(), ringParams.spacing(),
            ringParams.glowWidth(), ringParams.contractMode());
        Logging.RENDER.topic("shockwave_gpu")
            .kv("thickness", t)
            .info("Thickness set");
    }
    
    public static void setIntensity(float value) {
        float i = Math.max(0.0f, Math.min(3.0f, value));
        ringParams = new RingParams(ringParams.thickness(), i, ringParams.animationSpeed(),
            ringParams.maxRadius(), ringParams.count(), ringParams.spacing(),
            ringParams.glowWidth(), ringParams.contractMode());
        Logging.RENDER.topic("shockwave_gpu")
            .kv("intensity", i)
            .info("Intensity set");
    }
    
    public static void setSpeed(float speed) {
        float s = Math.max(1.0f, Math.min(200.0f, speed));
        ringParams = new RingParams(ringParams.thickness(), ringParams.intensity(), s,
            ringParams.maxRadius(), ringParams.count(), ringParams.spacing(),
            ringParams.glowWidth(), ringParams.contractMode());
        Logging.RENDER.topic("shockwave_gpu")
            .kv("speed", s)
            .info("Animation speed set");
    }
    
    public static void setMaxRadius(float max) {
        float m = Math.max(10.0f, Math.min(500.0f, max));
        ringParams = new RingParams(ringParams.thickness(), ringParams.intensity(), 
            ringParams.animationSpeed(), m, ringParams.count(), ringParams.spacing(),
            ringParams.glowWidth(), ringParams.contractMode());
        Logging.RENDER.topic("shockwave_gpu")
            .kv("maxRadius", m)
            .info("Max radius set");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PARAMETER GETTERS (for shader uniforms)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static float getCurrentRadius() {
        if (animating) {
            float elapsed = (System.currentTimeMillis() - animationStartTime) / 1000.0f;
            float animRadius;
            float maxR = ringParams.maxRadius();
            float speed = ringParams.animationSpeed();
            
            if (ringParams.contractMode()) {
                // Contract: start from max, go to 0
                animRadius = maxR - (elapsed * speed);
                if (animRadius <= 0.0f) {
                    animating = false;
                    enabled = false;
                    return 0.0f;
                }
            } else {
                // Expand: start from 0, go to max
                animRadius = elapsed * speed;
                if (animRadius >= maxR) {
                    animating = false;
                    enabled = false;
                    return maxR;
                }
            }
            return animRadius;
        }
        return currentRadius;
    }
    
    public static float getThickness() {
        return ringParams.thickness();
    }
    
    public static float getIntensity() {
        return ringParams.intensity();
    }
    
    public static float getSpeed() {
        return ringParams.animationSpeed();
    }
    
    public static float getMaxRadius() {
        return ringParams.maxRadius();
    }
    
    // Advanced param getters
    public static int getRingCount() {
        return ringParams.count();
    }
    
    public static float getRingSpacing() {
        return ringParams.spacing();
    }
    
    public static boolean isContractMode() {
        return ringParams.contractMode();
    }
    
    // Advanced param setters
    public static void setRingCount(int count) {
        int c = Math.max(1, Math.min(10, count));
        ringParams = new RingParams(ringParams.thickness(), ringParams.intensity(),
            ringParams.animationSpeed(), ringParams.maxRadius(), c, ringParams.spacing(),
            ringParams.glowWidth(), ringParams.contractMode());
        Logging.RENDER.topic("shockwave_gpu")
            .kv("ringCount", c)
            .info("Ring count set");
    }
    
    public static void setRingSpacing(float spacing) {
        float s = Math.max(1.0f, Math.min(50.0f, spacing));
        ringParams = new RingParams(ringParams.thickness(), ringParams.intensity(),
            ringParams.animationSpeed(), ringParams.maxRadius(), ringParams.count(), s,
            ringParams.glowWidth(), ringParams.contractMode());
        Logging.RENDER.topic("shockwave_gpu")
            .kv("ringSpacing", s)
            .info("Ring spacing set");
    }
    
    public static void setContractMode(boolean contract) {
        ringParams = new RingParams(ringParams.thickness(), ringParams.intensity(),
            ringParams.animationSpeed(), ringParams.maxRadius(), ringParams.count(), 
            ringParams.spacing(), ringParams.glowWidth(), contract);
        Logging.RENDER.topic("shockwave_gpu")
            .kv("contractMode", contract)
            .info("Contract mode set");
    }
    
    public static float getGlowWidth() { return ringParams.glowWidth(); }
    
    public static void setGlowWidth(float width) {
        float w = Math.max(1f, width);
        ringParams = new RingParams(ringParams.thickness(), ringParams.intensity(),
            ringParams.animationSpeed(), ringParams.maxRadius(), ringParams.count(), 
            ringParams.spacing(), w, ringParams.contractMode());
        Logging.RENDER.topic("shockwave_gpu")
            .kv("glowWidth", w)
            .info("Glow width set");
    }
    
    // Contracting animation
    public static void triggerContract() {
        enabled = true;
        animating = true;
        // Set contract mode via record
        ringParams = new RingParams(ringParams.thickness(), ringParams.intensity(),
            ringParams.animationSpeed(), ringParams.maxRadius(), ringParams.count(), 
            ringParams.spacing(), ringParams.glowWidth(), true);
        currentRadius = ringParams.maxRadius();  // Start from max
        animationStartTime = System.currentTimeMillis();
        Logging.RENDER.topic("shockwave_gpu")
            .kv("from", ringParams.maxRadius())
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
        
        // FREEZE camera state at this moment
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            var camEntity = client.getCameraEntity();
            if (camEntity == null) camEntity = client.player;
            var camPos = camEntity.getCameraPosVec(1.0f);
            // Update cameraState with frozen position
            cameraState = new CameraState(
                cameraState.x(), cameraState.y(), cameraState.z(),
                cameraState.forwardX(), cameraState.forwardY(), cameraState.forwardZ(),
                (float) camPos.x, (float) camPos.y, (float) camPos.z
            );
        }
        
        Logging.RENDER.topic("shockwave_gpu")
            .kv("target", String.format("%.1f, %.1f, %.1f", x, y, z))
            .kv("frozenCam", String.format("%.1f, %.1f, %.1f", 
                cameraState.frozenX(), cameraState.frozenY(), cameraState.frozenZ()))
            .info("Target position set with frozen camera");
    }
    
    public static void updateCameraPosition(float x, float y, float z) {
        cameraState = new CameraState(
            x, y, z,
            cameraState.forwardX(), cameraState.forwardY(), cameraState.forwardZ(),
            cameraState.frozenX(), cameraState.frozenY(), cameraState.frozenZ()
        );
    }
    
    public static void updateCameraForward(float x, float y, float z) {
        cameraState = new CameraState(
            cameraState.x(), cameraState.y(), cameraState.z(),
            x, y, z,
            cameraState.frozenX(), cameraState.frozenY(), cameraState.frozenZ()
        );
    }
    
    public static float getForwardX() { return cameraState.forwardX(); }
    public static float getForwardY() { return cameraState.forwardY(); }
    public static float getForwardZ() { return cameraState.forwardZ(); }
    
    public static float getTargetX() { return targetX; }
    public static float getTargetY() { return targetY; }
    public static float getTargetZ() { return targetZ; }
    public static float getCameraX() { return cameraState.x(); }
    public static float getCameraY() { return cameraState.y(); }
    public static float getCameraZ() { return cameraState.z(); }
    public static float getFrozenCamX() { return cameraState.frozenX(); }
    public static float getFrozenCamY() { return cameraState.frozenY(); }
    public static float getFrozenCamZ() { return cameraState.frozenZ(); }
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
    
    public static float getBlackoutAmount() { return screenEffects.blackout(); }
    public static float getVignetteAmount() { return screenEffects.vignetteAmount(); }
    public static float getVignetteRadius() { return screenEffects.vignetteRadius(); }
    public static float getTintR() { return screenEffects.tintR(); }
    public static float getTintG() { return screenEffects.tintG(); }
    public static float getTintB() { return screenEffects.tintB(); }
    public static float getTintAmount() { return screenEffects.tintAmount(); }
    
    public static void setBlackout(float amount) {
        float v = Math.max(0f, Math.min(1f, amount));
        screenEffects = new ScreenEffects(v, screenEffects.vignetteAmount(), 
            screenEffects.vignetteRadius(), screenEffects.tintR(), 
            screenEffects.tintG(), screenEffects.tintB(), screenEffects.tintAmount());
        Logging.RENDER.topic("shockwave_gpu")
            .kv("blackout", v)
            .info("Blackout set");
    }
    
    public static void setVignette(float amount, float radius) {
        float a = Math.max(0f, Math.min(10f, amount));
        float r = Math.max(-5f, Math.min(10f, radius));
        screenEffects = new ScreenEffects(screenEffects.blackout(), a, r,
            screenEffects.tintR(), screenEffects.tintG(), screenEffects.tintB(), 
            screenEffects.tintAmount());
        Logging.RENDER.topic("shockwave_gpu")
            .kv("vignette", a)
            .kv("radius", r)
            .info("Vignette set");
    }
    
    public static void setTint(float r, float g, float b, float amount) {
        float rr = Math.max(0f, Math.min(2f, r));
        float gg = Math.max(0f, Math.min(2f, g));
        float bb = Math.max(0f, Math.min(2f, b));
        float aa = Math.max(0f, Math.min(1f, amount));
        screenEffects = new ScreenEffects(screenEffects.blackout(), 
            screenEffects.vignetteAmount(), screenEffects.vignetteRadius(),
            rr, gg, bb, aa);
        Logging.RENDER.topic("shockwave_gpu")
            .kv("tint", String.format("%.1f,%.1f,%.1f @ %.1f", rr, gg, bb, aa))
            .info("Tint set");
    }
    
    public static void clearScreenEffects() {
        screenEffects = ScreenEffects.NONE;
        Logging.RENDER.topic("shockwave_gpu")
            .info("Screen effects cleared");
    }
    
    // Ring color
    public static float getRingR() { return ringColor.r(); }
    public static float getRingG() { return ringColor.g(); }
    public static float getRingB() { return ringColor.b(); }
    public static float getRingOpacity() { return ringColor.opacity(); }
    
    public static void setRingColor(float r, float g, float b, float opacity) {
        ringColor = new RingColor(
            Math.max(0f, Math.min(1f, r)),
            Math.max(0f, Math.min(1f, g)),
            Math.max(0f, Math.min(1f, b)),
            Math.max(0f, Math.min(1f, opacity))
        );
        Logging.RENDER.topic("shockwave_gpu")
            .kv("ringColor", String.format("%.1f,%.1f,%.1f @ %.0f%%", r, g, b, opacity * 100))
            .info("Ring color set");
    }
    
    // STATUS STRING (for HUD display)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static String getStatusString() {
        if (!enabled) return "OFF";
        if (animating) {
            return String.format("ANIM r=%.1f spd=%.1f max=%.1f", 
                getCurrentRadius(), ringParams.animationSpeed(), ringParams.maxRadius());
        }
        return String.format("STATIC r=%.1f t=%.1f i=%.1f", 
            currentRadius, ringParams.thickness(), ringParams.intensity());
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEST SEQUENCE - Cycle through configurations to verify all features
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static int testStep = -1;
    private static final String[] TEST_NAMES = {
        "1: Basic trigger (cyan)",
        "2: Red rings",
        "3: Green rings",
        "4: Multiple rings (5)",
        "5: Thick rings",
        "6: High intensity",
        "7: Blackout 50%",
        "8: Vignette effect",
        "9: Red tint",
        "10: All screen effects",
        "RESET: Back to defaults"
    };
    
    /**
     * Cycles to the next test configuration.
     * Call repeatedly with /shockwavegpu test
     * @return Description of current test step
     */
    public static String cycleTest() {
        testStep++;
        if (testStep >= TEST_NAMES.length) testStep = 0;
        
        // Reset to defaults first
        ringParams = RingParams.DEFAULT;
        ringColor = RingColor.DEFAULT;
        screenEffects = ScreenEffects.NONE;
        
        switch (testStep) {
            case 0 -> {
                // Basic trigger - default cyan
                trigger();
            }
            case 1 -> {
                // Red rings
                ringColor = new RingColor(1f, 0f, 0f, 1f);
                trigger();
            }
            case 2 -> {
                // Green rings
                ringColor = new RingColor(0f, 1f, 0f, 1f);
                trigger();
            }
            case 3 -> {
                // Multiple rings
                ringParams = new RingParams(ringParams.thickness(), ringParams.intensity(),
                    ringParams.animationSpeed(), ringParams.maxRadius(), 5, 15f,
                    ringParams.glowWidth(), ringParams.contractMode());
                trigger();
            }
            case 4 -> {
                // Thick rings
                ringParams = new RingParams(10f, ringParams.intensity(),
                    ringParams.animationSpeed(), ringParams.maxRadius(), ringParams.count(), 
                    ringParams.spacing(), 15f, ringParams.contractMode());
                trigger();
            }
            case 5 -> {
                // High intensity
                ringParams = new RingParams(ringParams.thickness(), 2.5f,
                    ringParams.animationSpeed(), ringParams.maxRadius(), ringParams.count(), 
                    ringParams.spacing(), ringParams.glowWidth(), ringParams.contractMode());
                trigger();
            }
            case 6 -> {
                // Blackout
                screenEffects = new ScreenEffects(0.5f, 0f, 0.5f, 1f, 1f, 1f, 0f);
                setRadius(50f);
            }
            case 7 -> {
                // Vignette
                screenEffects = new ScreenEffects(0f, 0.8f, 0.3f, 1f, 1f, 1f, 0f);
                setRadius(50f);
            }
            case 8 -> {
                // Red tint
                screenEffects = new ScreenEffects(0f, 0f, 0.5f, 1f, 0.3f, 0.3f, 0.7f);
                setRadius(50f);
            }
            case 9 -> {
                // All screen effects
                screenEffects = new ScreenEffects(0.3f, 0.5f, 0.4f, 1f, 0.5f, 0.8f, 0.5f);
                ringColor = new RingColor(1f, 0.5f, 0f, 1f); // Orange
                trigger();
            }
            case 10 -> {
                // Reset to defaults
                enabled = false;
            }
        }
        
        return TEST_NAMES[testStep];
    }
    
    /**
     * Gets current test step name without cycling.
     */
    public static String getCurrentTestName() {
        if (testStep < 0 || testStep >= TEST_NAMES.length) return "Use /shockwavegpu test to start";
        return TEST_NAMES[testStep];
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

