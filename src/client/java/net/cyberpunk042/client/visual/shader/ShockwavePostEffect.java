package net.cyberpunk042.client.visual.shader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.render.DefaultFramebufferSet;
import com.mojang.blaze3d.buffers.Std140Builder;
import net.minecraft.util.Identifier;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.*;

import java.util.Set;

/**
 * Manages the GPU shockwave post-effect.
 * Types are defined in ShockwaveTypes (RingParams, ShapeType, OriginMode, etc.)
 */
public class ShockwavePostEffect {
    
    private static final Identifier SHADER_ID = Identifier.of("the-virus-block", "shockwave_ring");
    private static final Set<Identifier> REQUIRED_TARGETS = DefaultFramebufferSet.STAGES;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static boolean enabled = false;
    private static boolean animating = false;
    private static long animationStartTime = 0;
    private static float currentRadius = 20.0f;
    
    private static RingParams ringParams = RingParams.DEFAULT;
    private static RingColor ringColor = RingColor.DEFAULT;
    private static ScreenEffects screenEffects = ScreenEffects.NONE;
    private static CameraState cameraState = CameraState.ORIGIN;
    private static ShapeConfig shapeConfig = ShapeConfig.POINT;
    private static OrbitalEffectConfig orbitalEffectConfig = OrbitalEffectConfig.DEFAULT;
    
    // Orbital animation state
    private static float orbitalPhase = 0f;
    private static float orbitalSpawnProgress = 0f;
    private static long orbitalSpawnStartTime = 0;
    private static boolean orbitalRetracting = false;
    private static float beamProgress = 0f;
    private static long beamStartTime = 0;
    private static boolean beamShrinking = false;
    private static long retractDelayStartTime = 0;
    private static boolean waitingForRetractDelay = false;
    
    // Origin mode and target
    private static OriginMode originMode = OriginMode.CAMERA;
    private static float targetX = 0, targetY = 0, targetZ = 0;
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
    
    /** Apply easing curve to a 0-1 linear value - delegates to centralized utility */
    private static float applyEasing(float t, EasingType easing) {
        return net.cyberpunk042.util.math.EasingFunctions.apply(t, easing.toFunctionType());
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
    public static boolean getCombinedMode() { return orbitalEffectConfig.combinedMode(); }
    
    // Update entire sub-configs
    public static void setOrbitalVisual(OrbitalVisualConfig v) {
        orbitalEffectConfig = new OrbitalEffectConfig(v, orbitalEffectConfig.beam(), 
            orbitalEffectConfig.timing(), orbitalEffectConfig.blendRadius(), orbitalEffectConfig.combinedMode());
    }
    public static void setBeamVisual(BeamVisualConfig v) {
        orbitalEffectConfig = new OrbitalEffectConfig(orbitalEffectConfig.orbital(), v, 
            orbitalEffectConfig.timing(), orbitalEffectConfig.blendRadius(), orbitalEffectConfig.combinedMode());
    }
    public static void setAnimationTiming(AnimationTimingConfig v) {
        orbitalEffectConfig = new OrbitalEffectConfig(orbitalEffectConfig.orbital(), 
            orbitalEffectConfig.beam(), v, orbitalEffectConfig.blendRadius(), orbitalEffectConfig.combinedMode());
    }
    public static void setBlendRadius(float v) {
        orbitalEffectConfig = new OrbitalEffectConfig(orbitalEffectConfig.orbital(), 
            orbitalEffectConfig.beam(), orbitalEffectConfig.timing(), v, orbitalEffectConfig.combinedMode());
    }
    public static void setCombinedMode(boolean v) {
        orbitalEffectConfig = new OrbitalEffectConfig(orbitalEffectConfig.orbital(), 
            orbitalEffectConfig.beam(), orbitalEffectConfig.timing(), orbitalEffectConfig.blendRadius(), v);
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
    // UNIFORM BUFFER CONSTRUCTION - Delegated to ShockwaveUBOWriter
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Buffer layout: 18 vec4s = 288 bytes */
    public static final int VEC4_COUNT = net.cyberpunk042.client.visual.shader.shockwave.ShockwaveUBOWriter.VEC4_COUNT;
    public static final int BUFFER_SIZE = net.cyberpunk042.client.visual.shader.shockwave.ShockwaveUBOWriter.BUFFER_SIZE;
    
    /**
     * Writes all shockwave state to a Std140 uniform buffer.
     * @param builder The Std140Builder to write to
     * @param aspectRatio Screen aspect ratio (width/height)  
     * @param fovRadians Field of view in radians
     */
    public static void writeUniformBuffer(Std140Builder builder, float aspectRatio, float fovRadians) {
        var snapshot = new net.cyberpunk042.client.visual.shader.shockwave.ShockwaveUBOWriter.UBOSnapshot(
            getCurrentRadius(), ringParams, ringColor, screenEffects,
            cameraState, isTargetMode(), targetX, targetY, targetZ,
            shapeConfig, orbitalPhase, orbitalSpawnProgress, beamProgress,
            orbitalEffectConfig
        );
        net.cyberpunk042.client.visual.shader.shockwave.ShockwaveUBOWriter.writeBuffer(builder, snapshot, aspectRatio, fovRadians);
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
    // TEST SEQUENCE - Delegated to ShockwaveTestPresets
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Cycles to the next test configuration.
     * Call repeatedly with /shockwavegpu test
     * @return Description of current test step
     */
    public static String cycleTest() {
        var config = net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTestPresets.cycleNext();
        ringParams = config.ringParams();
        ringColor = config.ringColor();
        screenEffects = config.screenEffects();
        shapeConfig = config.shapeConfig();
        if (config.shouldDisable()) enabled = false;
        if (config.shouldTrigger()) trigger();
        return config.name();
    }
    
    /**
     * Gets current test step name without cycling.
     */
    public static String getCurrentTestName() {
        return net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTestPresets.getCurrentName();
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

