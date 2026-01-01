package net.cyberpunk042.client.visual.shader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.render.DefaultFramebufferSet;
import com.mojang.blaze3d.buffers.Std140Builder;
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
    
    /**
     * Shape type for shockwave emission source.
     * POINT is the current/default behavior.
     */
    public enum ShapeType {
        POINT(0),      // Current behavior - rings from single point
        SPHERE(1),     // Rings from sphere surface  
        TORUS(2),      // Rings from torus (donut) surface
        POLYGON(3),    // N-sided polygon rings (triangle, square, hex...)
        ORBITAL(4);    // Main sphere + orbiting spheres
        
        private final int shaderCode;
        ShapeType(int code) { this.shaderCode = code; }
        public int getShaderCode() { return shaderCode; }
    }
    
    /**
     * Shape configuration for advanced shockwave emission.
     */
    public record ShapeConfig(
        ShapeType type,
        float radius,           // Main radius (sphere/polygon)
        float majorRadius,      // Torus major
        float minorRadius,      // Torus minor / orbital sphere radius
        int sideCount,          // Polygon sides / orbital count
        float orbitDistance     // Distance from center to orbital centers
    ) {
        public static final ShapeConfig POINT = new ShapeConfig(
            ShapeType.POINT, 0f, 0f, 0f, 0, 0f
        );
        
        public static ShapeConfig sphere(float radius) {
            return new ShapeConfig(ShapeType.SPHERE, radius, 0, 0, 0, 0);
        }
        
        public static ShapeConfig torus(float major, float minor) {
            return new ShapeConfig(ShapeType.TORUS, 0, major, minor, 0, 0);
        }
        
        public static ShapeConfig polygon(int sides, float radius) {
            return new ShapeConfig(ShapeType.POLYGON, radius, 0, 0, sides, 0);
        }
        
        /**
         * Creates an orbital configuration: main sphere + orbiting spheres.
         * @param mainRadius Radius of the central sphere
         * @param orbitalRadius Radius of each orbiting sphere
         * @param orbitDistance Distance from center to orbital centers
         * @param count Number of orbiting spheres (1-32)
         */
        public static ShapeConfig orbital(float mainRadius, float orbitalRadius, 
                                          float orbitDistance, int count) {
            return new ShapeConfig(ShapeType.ORBITAL, mainRadius, 0, orbitalRadius, 
                                   Math.max(1, Math.min(32, count)), orbitDistance);  // Match shader cap of 32
        }
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
    private static ShapeConfig shapeConfig = ShapeConfig.POINT;  // Default: current behavior
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ORBITAL EFFECT CONFIGURATION - Complete control over all visual parameters
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** RGB color (0-1 per channel) */
    public record Color3f(float r, float g, float b) {
        public static final Color3f BLACK = new Color3f(0f, 0f, 0f);
        public static final Color3f WHITE = new Color3f(1f, 1f, 1f);
        public static final Color3f CYAN = new Color3f(0f, 1f, 1f);
    }
    
    /** RGBA color with opacity (0-1 per channel) */
    public record Color4f(float r, float g, float b, float a) {
        public static final Color4f CYAN_FULL = new Color4f(0f, 1f, 1f, 1f);
        public static final Color4f WHITE_FULL = new Color4f(1f, 1f, 1f, 1f);
    }
    
    /** Corona/rim glow effect configuration */
    public record CoronaConfig(
        Color4f color,      // Corona RGBA
        float width,        // Glow spread (blocks)
        float intensity,    // Brightness multiplier
        float rimPower,     // Rim sharpness (0.5-5)
        float rimFalloff    // Rim fade curve (0.5-3)
    ) {
        public static final CoronaConfig DEFAULT = new CoronaConfig(
            Color4f.CYAN_FULL, 2.0f, 1.0f, 2.0f, 1.0f
        );
    }
    
    /** Orbital sphere visual configuration */
    public record OrbitalVisualConfig(
        Color3f bodyColor,      // Sphere body color (usually black)
        CoronaConfig corona     // Corona/rim glow settings
    ) {
        public static final OrbitalVisualConfig DEFAULT = new OrbitalVisualConfig(
            Color3f.BLACK, CoronaConfig.DEFAULT
        );
    }
    
    /** Beam visual configuration */
    public record BeamVisualConfig(
        Color3f bodyColor,      // Beam body color (usually black)
        CoronaConfig corona,    // Corona/rim glow settings
        float width,            // Absolute width (blocks), 0 = use widthScale
        float widthScale,       // Width as ratio of orbital radius (0.1-1.0)
        float taper             // Taper factor (1=uniform, <1=narrow top)
    ) {
        public static final BeamVisualConfig DEFAULT = new BeamVisualConfig(
            Color3f.BLACK, CoronaConfig.DEFAULT, 0f, 0.3f, 1.0f
        );
    }
    
    /** Easing curve types */
    public enum EasingType { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }
    
    /** Animation timing configuration */
    public record AnimationTimingConfig(
        // Orbital animation
        float orbitalSpeed,           // Rotation speed (rad/frame)
        float orbitalSpawnDuration,   // ms to spawn
        float orbitalRetractDuration, // ms to retract
        EasingType orbitalSpawnEasing,
        EasingType orbitalRetractEasing,
        
        // Beam animation
        float beamHeight,             // Max height (0 = infinity)
        float beamGrowDuration,       // ms to grow
        float beamShrinkDuration,     // ms to shrink
        float beamHoldDuration,       // ms at full height before shrink
        float beamWidthGrowFactor,    // 0 = fixed, 1 = animated
        float beamLengthGrowFactor,   // 0 = instant, 1 = gradual
        EasingType beamGrowEasing,
        EasingType beamShrinkEasing,
        
        // Delays
        float orbitalSpawnDelay,      // ms delay before spawn
        float beamStartDelay,         // ms delay after spawn before beam
        float retractDelay,           // ms delay after beam shrink
        
        // Triggers
        boolean autoRetractOnRingEnd  // Auto-shrink when rings finish
    ) {
        public static final AnimationTimingConfig DEFAULT = new AnimationTimingConfig(
            0.008f,   // orbitalSpeed
            2500f,    // orbitalSpawnDuration
            1500f,    // orbitalRetractDuration
            EasingType.EASE_OUT,
            EasingType.EASE_IN,
            100f,     // beamHeight (0 = infinity)
            1500f,    // beamGrowDuration
            800f,     // beamShrinkDuration
            0f,       // beamHoldDuration
            0f,       // beamWidthGrowFactor (fixed width)
            1f,       // beamLengthGrowFactor (gradual)
            EasingType.EASE_OUT,
            EasingType.EASE_IN,
            0f,       // orbitalSpawnDelay
            0f,       // beamStartDelay
            0f,       // retractDelay
            true      // autoRetractOnRingEnd
        );
    }
    
    /** Master configuration combining all orbital effect settings */
    public record OrbitalEffectConfig(
        OrbitalVisualConfig orbital,
        BeamVisualConfig beam,
        AnimationTimingConfig timing,
        float blendRadius           // Shape blending (0=sharp, 5+=unified)
    ) {
        public static final OrbitalEffectConfig DEFAULT = new OrbitalEffectConfig(
            OrbitalVisualConfig.DEFAULT,
            BeamVisualConfig.DEFAULT,
            AnimationTimingConfig.DEFAULT,
            3.0f  // blendRadius
        );
    }
    
    // Current orbital effect config
    private static OrbitalEffectConfig orbitalEffectConfig = OrbitalEffectConfig.DEFAULT;
    
    // Orbital animation state (runtime, not config)
    private static float orbitalPhase = 0f;           // Current rotation angle
    private static float orbitalSpawnProgress = 0f;   // 0 = hidden, 1 = full
    private static long orbitalSpawnStartTime = 0;
    private static boolean orbitalRetracting = false;
    
    // Beam animation state (runtime, not config)
    private static float beamProgress = 0f;
    private static long beamStartTime = 0;
    private static boolean beamShrinking = false;
    
    // Retract delay state
    private static long retractDelayStartTime = 0;
    private static boolean waitingForRetractDelay = false;
    
    // Origin mode: CAMERA = rings around player, TARGET = rings around cursor hit point
    public enum OriginMode { CAMERA, TARGET }
    private static OriginMode originMode = OriginMode.CAMERA;
    
    // Target world position (for TARGET mode)
    private static float targetX = 0, targetY = 0, targetZ = 0;
    
    // Follow camera flag - when true, the target position updates to camera position each frame
    private static boolean followCamera = false;
    
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
        
        // Start orbital spawn animation if using orbital shape
        if (shapeConfig.type() == ShapeType.ORBITAL) {
            startOrbitalSpawn();
        }
        
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
                    // For orbital, start beam shrink (which will then trigger retract)
                    if (shapeConfig.type() == ShapeType.ORBITAL && !orbitalRetracting && !beamShrinking) {
                        beamShrinking = true;
                        beamStartTime = System.currentTimeMillis();
                        // Keep enabled for beam shrink + retract
                    } else if (shapeConfig.type() != ShapeType.ORBITAL) {
                        enabled = false;
                    }
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
        int c = Math.max(1, Math.min(50, count));  // Match shader cap of 50
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
    // FOLLOW CAMERA MODE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Enable or disable follow camera mode.
     * When enabled, the shockwave target position tracks the camera/player each frame.
     */
    public static void setFollowCamera(boolean follow) {
        followCamera = follow;
        if (follow) {
            // Switch to TARGET mode so position updates are used
            originMode = OriginMode.TARGET;
        }
        Logging.RENDER.topic("shockwave_gpu")
            .kv("followCamera", follow)
            .info("Follow camera mode {}", follow ? "enabled" : "disabled");
    }
    
    public static boolean isFollowCamera() {
        return followCamera;
    }
    
    /**
     * Called each frame from the render mixin to update position if following.
     * @param camX Camera X position
     * @param camY Camera Y position  
     * @param camZ Camera Z position
     */
    public static void tickFollowPosition(float camX, float camY, float camZ) {
        if (followCamera && originMode == OriginMode.TARGET) {
            // Update target to follow camera/player
            targetX = camX;
            targetY = camY;
            targetZ = camZ;
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
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE CONFIGURATION - For future shape types
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static ShapeConfig getShapeConfig() { return shapeConfig; }
    
    public static void setShape(ShapeConfig config) {
        shapeConfig = config;
        Logging.RENDER.topic("shockwave_gpu")
            .kv("shapeType", config.type())
            .kv("radius", config.radius())
            .info("Shape config set");
    }
    
    public static void setShapePoint() {
        setShape(ShapeConfig.POINT);
    }
    
    public static void setShapeSphere(float radius) {
        setShape(ShapeConfig.sphere(radius));
    }
    
    public static void setShapeTorus(float major, float minor) {
        setShape(ShapeConfig.torus(major, minor));
    }
    
    public static void setShapePolygon(int sides, float radius) {
        setShape(ShapeConfig.polygon(sides, radius));
    }
    
    public static void setShapeOrbital(float mainRadius, float orbitalRadius, 
                                       float orbitDistance, int count) {
        setShape(ShapeConfig.orbital(mainRadius, orbitalRadius, orbitDistance, count));
    }
    
    /**
     * Updates orbital animations each frame. Call from render loop.
     * Handles: phase rotation, spawn progress, retract progress.
     */
    public static void tickOrbitalPhase() {
        // Orbital phase rotation - runs even when disabled for live preview
        if (shapeConfig.type() == ShapeType.ORBITAL) {
            AnimationTimingConfig timing = orbitalEffectConfig.timing();
            
            orbitalPhase += timing.orbitalSpeed();
            if (orbitalPhase > 6.28318f) orbitalPhase -= 6.28318f;
            if (orbitalPhase < 0f) orbitalPhase += 6.28318f;
        }
        
        // Animation (spawn/retract) only runs when enabled
        if (!enabled) return;
        
        // Spawn/retract animation (only when orbital shape active and enabled)
        if (shapeConfig.type() == ShapeType.ORBITAL) {
            AnimationTimingConfig timing = orbitalEffectConfig.timing();
            long now = System.currentTimeMillis();
            
            // Spawn/retract animation
            if (orbitalRetracting) {
                float elapsed = now - orbitalSpawnStartTime;
                float linear = Math.min(1f, elapsed / timing.orbitalRetractDuration());
                // Apply easing based on config
                orbitalSpawnProgress = applyEasing(1f - linear, timing.orbitalRetractEasing());
                
                // Retract complete - disable effect
                if (orbitalSpawnProgress <= 0.01f) {
                    orbitalSpawnProgress = 0f;
                    orbitalRetracting = false;
                    enabled = false;
                }
            } else if (orbitalSpawnProgress < 1f) {
                float elapsed = now - orbitalSpawnStartTime;
                float linear = Math.min(1f, elapsed / timing.orbitalSpawnDuration());
                // Apply easing based on config
                orbitalSpawnProgress = applyEasing(linear, timing.orbitalSpawnEasing());
                
                // Start beam when spawn completes
                if (orbitalSpawnProgress >= 0.99f && beamProgress == 0f && !beamShrinking) {
                    beamStartTime = now + (long) timing.beamStartDelay();
                }
            }
            
            // Beam animation
            if (beamShrinking) {
                // Shrinking beam
                float elapsed = now - beamStartTime;
                float linear = Math.min(1f, elapsed / timing.beamShrinkDuration());
                beamProgress = 1f - applyEasing(linear, timing.beamShrinkEasing());
                
                // Beam shrink complete - start retract delay
                if (beamProgress <= 0.01f) {
                    beamProgress = 0f;
                    beamShrinking = false;
                    
                    // Check if retract delay is needed
                    if (timing.retractDelay() > 0) {
                        waitingForRetractDelay = true;
                        retractDelayStartTime = now;
                    } else {
                        startOrbitalRetract();
                    }
                }
            } else if (waitingForRetractDelay) {
                // Waiting for retract delay to complete
                float elapsed = now - retractDelayStartTime;
                if (elapsed >= timing.retractDelay()) {
                    waitingForRetractDelay = false;
                    startOrbitalRetract();
                }
            } else if (orbitalSpawnProgress >= 0.99f && beamProgress < 1f && !orbitalRetracting && now >= beamStartTime) {
                // Growing beam (spawn complete, delay passed, not yet full, not retracting)
                float elapsed = now - beamStartTime;
                float linear = Math.min(1f, elapsed / timing.beamGrowDuration());
                beamProgress = applyEasing(linear, timing.beamGrowEasing());
            }
        }
    }
    
    /** Apply easing curve to a 0-1 linear value */
    private static float applyEasing(float t, EasingType easing) {
        return switch (easing) {
            case LINEAR -> t;
            case EASE_IN -> t * t;
            case EASE_OUT -> 1f - (1f - t) * (1f - t);
            case EASE_IN_OUT -> t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2f) / 2f;
        };
    }
    
    /**
     * Starts orbital spawn animation (orbitals emerge from center).
     */
    public static void startOrbitalSpawn() {
        orbitalSpawnProgress = 0f;
        orbitalRetracting = false;
        orbitalSpawnStartTime = System.currentTimeMillis();
    }
    
    /**
     * Starts orbital retract animation (orbitals return to center).
     */
    public static void startOrbitalRetract() {
        orbitalRetracting = true;
        orbitalSpawnStartTime = System.currentTimeMillis();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ORBITAL EFFECT CONFIG ACCESSORS - Full control over all parameters
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Master config access
    public static OrbitalEffectConfig getOrbitalEffectConfig() { return orbitalEffectConfig; }
    public static void setOrbitalEffectConfig(OrbitalEffectConfig config) { orbitalEffectConfig = config; }
    
    // Sub-config access
    public static OrbitalVisualConfig getOrbitalVisual() { return orbitalEffectConfig.orbital(); }
    public static BeamVisualConfig getBeamVisual() { return orbitalEffectConfig.beam(); }
    public static AnimationTimingConfig getAnimationTiming() { return orbitalEffectConfig.timing(); }
    public static float getBlendRadius() { return orbitalEffectConfig.blendRadius(); }
    
    // Update entire sub-configs
    public static void setOrbitalVisual(OrbitalVisualConfig v) {
        orbitalEffectConfig = new OrbitalEffectConfig(v, orbitalEffectConfig.beam(), 
            orbitalEffectConfig.timing(), orbitalEffectConfig.blendRadius());
    }
    public static void setBeamVisual(BeamVisualConfig v) {
        orbitalEffectConfig = new OrbitalEffectConfig(orbitalEffectConfig.orbital(), v, 
            orbitalEffectConfig.timing(), orbitalEffectConfig.blendRadius());
    }
    public static void setAnimationTiming(AnimationTimingConfig v) {
        orbitalEffectConfig = new OrbitalEffectConfig(orbitalEffectConfig.orbital(), 
            orbitalEffectConfig.beam(), v, orbitalEffectConfig.blendRadius());
    }
    public static void setBlendRadius(float v) {
        orbitalEffectConfig = new OrbitalEffectConfig(orbitalEffectConfig.orbital(), 
            orbitalEffectConfig.beam(), orbitalEffectConfig.timing(), v);
    }
    
    // Convenience setters for frequently-used timing params
    public static void setOrbitalSpeed(float v) {
        var t = orbitalEffectConfig.timing();
        setAnimationTiming(new AnimationTimingConfig(v, t.orbitalSpawnDuration(), t.orbitalRetractDuration(),
            t.orbitalSpawnEasing(), t.orbitalRetractEasing(), t.beamHeight(), t.beamGrowDuration(),
            t.beamShrinkDuration(), t.beamHoldDuration(), t.beamWidthGrowFactor(), t.beamLengthGrowFactor(),
            t.beamGrowEasing(), t.beamShrinkEasing(), t.orbitalSpawnDelay(), t.beamStartDelay(),
            t.retractDelay(), t.autoRetractOnRingEnd()));
    }
    public static void setBeamHeight(float v) {
        var t = orbitalEffectConfig.timing();
        setAnimationTiming(new AnimationTimingConfig(t.orbitalSpeed(), t.orbitalSpawnDuration(), 
            t.orbitalRetractDuration(), t.orbitalSpawnEasing(), t.orbitalRetractEasing(), v, 
            t.beamGrowDuration(), t.beamShrinkDuration(), t.beamHoldDuration(), t.beamWidthGrowFactor(), 
            t.beamLengthGrowFactor(), t.beamGrowEasing(), t.beamShrinkEasing(), t.orbitalSpawnDelay(), 
            t.beamStartDelay(), t.retractDelay(), t.autoRetractOnRingEnd()));
    }
    
    // Convenience setters for corona params
    public static void setOrbitalCoronaColor(float r, float g, float b, float a) {
        var ov = orbitalEffectConfig.orbital();
        var corona = new CoronaConfig(new Color4f(r, g, b, a), ov.corona().width(), 
            ov.corona().intensity(), ov.corona().rimPower(), ov.corona().rimFalloff());
        setOrbitalVisual(new OrbitalVisualConfig(ov.bodyColor(), corona));
    }
    public static void setBeamCoronaColor(float r, float g, float b, float a) {
        var bv = orbitalEffectConfig.beam();
        var corona = new CoronaConfig(new Color4f(r, g, b, a), bv.corona().width(), 
            bv.corona().intensity(), bv.corona().rimPower(), bv.corona().rimFalloff());
        setBeamVisual(new BeamVisualConfig(bv.bodyColor(), corona, bv.width(), bv.widthScale(), 
            bv.taper()));
    }
    
    // Runtime state getters
    public static float getOrbitalPhase() { return orbitalPhase; }
    public static float getOrbitalSpawnProgress() { return orbitalSpawnProgress; }
    public static float getBeamProgress() { return beamProgress; }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UNIFORM BUFFER CONSTRUCTION - Single source of truth for shader data
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Buffer layout: 18 vec4s = 288 bytes (extended for separate beam corona + geometry) */
    public static final int VEC4_COUNT = 18;
    public static final int BUFFER_SIZE = VEC4_COUNT * 16;
    
    /**
     * Writes all shockwave state to a Std140 uniform buffer.
     * This is the SINGLE SOURCE OF TRUTH for buffer layout.
     * 
     * @param builder The Std140Builder to write to
     * @param aspectRatio Screen aspect ratio (width/height)  
     * @param fovRadians Field of view in radians
     */
    public static void writeUniformBuffer(Std140Builder builder, float aspectRatio, float fovRadians) {
        // Vec4 0: Basic params
        float time = (System.currentTimeMillis() % 10000) / 1000.0f;
        builder.putVec4(getCurrentRadius(), ringParams.thickness(), ringParams.intensity(), time);
        
        // Vec4 1: Ring count, spacing, contract mode, glow width
        builder.putVec4(
            (float) ringParams.count(), 
            ringParams.spacing(),
            ringParams.contractMode() ? 1.0f : 0.0f, 
            ringParams.glowWidth()
        );
        
        // Vec4 2: Target world position + UseWorldOrigin flag
        float useWorldOrigin = isTargetMode() ? 1.0f : 0.0f;
        builder.putVec4(targetX, targetY, targetZ, useWorldOrigin);
        
        // Vec4 3: Camera world position + aspect ratio
        // ALWAYS use CURRENT camera position for ray origin
        // The TARGET position (for orbital center) is fixed, but rays must originate
        // from where the camera actually is RIGHT NOW
        float camX = cameraState.x();
        float camY = cameraState.y();
        float camZ = cameraState.z();
        builder.putVec4(camX, camY, camZ, aspectRatio);
        
        // Vec4 4: Camera forward direction + FOV
        builder.putVec4(
            cameraState.forwardX(), 
            cameraState.forwardY(), 
            cameraState.forwardZ(), 
            fovRadians
        );
        
        // Vec4 5: Camera up direction (simplified - always world up)
        builder.putVec4(0f, 1f, 0f, 0f);
        
        // Vec4 6: Screen blackout / vignette
        builder.putVec4(
            screenEffects.blackout(),
            screenEffects.vignetteAmount(),
            screenEffects.vignetteRadius(),
            0f
        );
        
        // Vec4 7: Color tint
        builder.putVec4(
            screenEffects.tintR(),
            screenEffects.tintG(),
            screenEffects.tintB(),
            screenEffects.tintAmount()
        );
        
        // Vec4 8: Ring color
        builder.putVec4(
            ringColor.r(),
            ringColor.g(),
            ringColor.b(),
            ringColor.opacity()
        );
        
        // Vec4 9: Shape configuration
        builder.putVec4(
            (float) shapeConfig.type().getShaderCode(),
            shapeConfig.radius(),
            shapeConfig.majorRadius(),
            shapeConfig.minorRadius()
        );
        
        // Vec4 10: Shape extras (polygon sides / orbital params)
        float animatedOrbitDistance = shapeConfig.orbitDistance() * orbitalSpawnProgress;
        // beamHeight=0 means infinity -> use large value
        float beamHeight = orbitalEffectConfig.timing().beamHeight();
        float effectiveBeamHeight = beamHeight <= 0.01f ? 10000f : beamHeight;
        float animatedBeamHeight = beamProgress * effectiveBeamHeight;
        builder.putVec4(
            (float) shapeConfig.sideCount(),  // Polygon sides OR orbital count
            animatedOrbitDistance,            // Animated distance (0 at spawn, full at end)
            orbitalPhase,                     // Current rotation angle (radians)
            animatedBeamHeight                // Beam height (0 when hidden, full when grown)
        );
        
        // Vec4 11: Shared corona config (orbital corona for now, beam will override in shader)
        CoronaConfig orbCorona = orbitalEffectConfig.orbital().corona();
        builder.putVec4(
            orbCorona.width(),
            orbCorona.intensity(),
            orbCorona.rimPower(),
            orbitalEffectConfig.blendRadius()
        );
        
        // Vec4 12: Orbital body color (RGB) + rim falloff
        Color3f orbBody = orbitalEffectConfig.orbital().bodyColor();
        builder.putVec4(
            orbBody.r(), orbBody.g(), orbBody.b(),
            orbCorona.rimFalloff()
        );
        
        // Vec4 13: Orbital corona color (RGBA)
        Color4f orbCoronaColor = orbCorona.color();
        builder.putVec4(
            orbCoronaColor.r(), orbCoronaColor.g(), orbCoronaColor.b(), orbCoronaColor.a()
        );
        
        // Vec4 14: Beam body color (RGB) + beam width scale
        Color3f beamBody = orbitalEffectConfig.beam().bodyColor();
        BeamVisualConfig beamVis = orbitalEffectConfig.beam();
        builder.putVec4(
            beamBody.r(), beamBody.g(), beamBody.b(),
            beamVis.widthScale()
        );
        
        // Vec4 15: Beam corona color (RGBA)
        Color4f beamCoronaColor = beamVis.corona().color();
        builder.putVec4(
            beamCoronaColor.r(), beamCoronaColor.g(), beamCoronaColor.b(), beamCoronaColor.a()
        );
        
        // Vec4 16: Beam geometry (width absolute, taper) + retractDelay/padding
        builder.putVec4(
            beamVis.width(),   // Absolute width (0 = use widthScale)
            beamVis.taper(),   // Taper factor (1 = uniform)
            orbitalEffectConfig.timing().retractDelay(),  // Delay after beam shrink
            0f  // padding
        );
        
        // Vec4 17: Beam corona settings (separate from orbital)
        CoronaConfig beamCorona = beamVis.corona();
        builder.putVec4(
            beamCorona.width(),
            beamCorona.intensity(),
            beamCorona.rimPower(),
            beamCorona.rimFalloff()
        );
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
        "11: SPHERE shape (r=5)",
        "12: TORUS shape (20/3)",
        "13: POLYGON hex (6 sides)",
        "14: ORBITAL (4 spheres)",
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
        shapeConfig = ShapeConfig.POINT;  // Reset shape so each test starts fresh
        
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
                // Blackout with animation
                screenEffects = new ScreenEffects(0.5f, 0f, 0.5f, 1f, 1f, 1f, 0f);
                trigger();
            }
            case 7 -> {
                // Vignette with animation
                screenEffects = new ScreenEffects(0f, 0.8f, 0.3f, 1f, 1f, 1f, 0f);
                trigger();
            }
            case 8 -> {
                // Red tint with animation
                screenEffects = new ScreenEffects(0f, 0f, 0.5f, 1f, 0.3f, 0.3f, 0.7f);
                trigger();
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
            case 11 -> {
                // Sphere shape
                shapeConfig = ShapeConfig.sphere(5f);
                trigger();
            }
            case 12 -> {
                // Torus shape
                shapeConfig = ShapeConfig.torus(20f, 3f);
                trigger();
            }
            case 13 -> {
                // Polygon (hexagon)
                shapeConfig = ShapeConfig.polygon(6, 15f);
                trigger();
            }
            case 14 -> {
                // Orbital (4 spheres)
                shapeConfig = ShapeConfig.orbital(5f, 2f, 15f, 4);
                trigger();
            }
            case 15 -> {
                // Reset to defaults
                shapeConfig = ShapeConfig.POINT;
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

