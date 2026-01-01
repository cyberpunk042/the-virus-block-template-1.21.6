package net.cyberpunk042.client.visual.shader.shockwave;

import com.mojang.blaze3d.buffers.Std140Builder;
import net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.*;

/**
 * Handles writing shockwave state to GPU uniform buffer (UBO).
 * 
 * <p>Extracted from ShockwavePostEffect to reduce class size while keeping
 * the buffer layout logic in one place.</p>
 */
public final class ShockwaveUBOWriter {
    
    private ShockwaveUBOWriter() {} // Utility class
    
    /** Buffer layout: 18 vec4s = 288 bytes */
    public static final int VEC4_COUNT = 18;
    public static final int BUFFER_SIZE = VEC4_COUNT * 16;
    
    /**
     * Snapshot of all state needed to write the UBO.
     * ShockwavePostEffect creates this and passes it to writeBuffer().
     */
    public record UBOSnapshot(
        // Ring state
        float currentRadius,
        RingParams ringParams,
        RingColor ringColor,
        ScreenEffects screenEffects,
        
        // Camera state
        CameraState cameraState,
        boolean targetMode,
        float targetX, float targetY, float targetZ,
        
        // Shape state
        ShapeConfig shapeConfig,
        float orbitalPhase,
        float orbitalSpawnProgress,
        float beamProgress,
        
        // Orbital config
        OrbitalEffectConfig orbitalEffectConfig
    ) {}
    
    /**
     * Writes all shockwave state to a Std140 uniform buffer.
     * This is the SINGLE SOURCE OF TRUTH for buffer layout.
     * 
     * @param builder The Std140Builder to write to
     * @param snapshot Current state snapshot
     * @param aspectRatio Screen aspect ratio (width/height)  
     * @param fovRadians Field of view in radians
     */
    public static void writeBuffer(Std140Builder builder, UBOSnapshot snapshot, 
                                   float aspectRatio, float fovRadians) {
        RingParams ringParams = snapshot.ringParams();
        RingColor ringColor = snapshot.ringColor();
        ScreenEffects screenEffects = snapshot.screenEffects();
        CameraState cameraState = snapshot.cameraState();
        ShapeConfig shapeConfig = snapshot.shapeConfig();
        OrbitalEffectConfig orbitalEffectConfig = snapshot.orbitalEffectConfig();
        
        // Vec4 0: Basic params
        float time = (System.currentTimeMillis() % 10000) / 1000.0f;
        builder.putVec4(snapshot.currentRadius(), ringParams.thickness(), ringParams.intensity(), time);
        
        // Vec4 1: Ring count, spacing, contract mode, glow width
        builder.putVec4(
            (float) ringParams.count(), 
            ringParams.spacing(),
            ringParams.contractMode() ? 1.0f : 0.0f, 
            ringParams.glowWidth()
        );
        
        // Vec4 2: Target world position + UseWorldOrigin flag
        float useWorldOrigin = snapshot.targetMode() ? 1.0f : 0.0f;
        builder.putVec4(snapshot.targetX(), snapshot.targetY(), snapshot.targetZ(), useWorldOrigin);
        
        // Vec4 3: Camera world position + aspect ratio
        builder.putVec4(cameraState.x(), cameraState.y(), cameraState.z(), aspectRatio);
        
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
        float animatedOrbitDistance = shapeConfig.orbitDistance() * snapshot.orbitalSpawnProgress();
        // beamHeight=0 means infinity -> use large value
        float beamHeight = orbitalEffectConfig.timing().beamHeight();
        float effectiveBeamHeight = beamHeight <= 0.01f ? 10000f : beamHeight;
        float animatedBeamHeight = snapshot.beamProgress() * effectiveBeamHeight;
        builder.putVec4(
            (float) shapeConfig.sideCount(),
            animatedOrbitDistance,
            snapshot.orbitalPhase(),
            animatedBeamHeight
        );
        
        // Vec4 11: Shared corona config
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
        
        // Vec4 16: Beam geometry (width absolute, taper) + retractDelay + combinedMode
        builder.putVec4(
            beamVis.width(),
            beamVis.taper(),
            orbitalEffectConfig.timing().retractDelay(),
            orbitalEffectConfig.combinedMode() ? 1f : 0f
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
}
