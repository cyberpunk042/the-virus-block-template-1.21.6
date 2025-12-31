package net.cyberpunk042.mixin.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.cyberpunk042.client.visual.shader.ShockwavePostEffect;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.util.Handle;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Mixin into PostEffectPass to inject dynamic uniform values.
 * 
 * <p>This intercepts the render() call and updates the uniformBuffers map
 * with current values from ShockwavePostEffect BEFORE the pass executes.
 */
@Mixin(PostEffectPass.class)
public class PostEffectPassMixin {
    
    @Shadow @Final private Map<String, GpuBuffer> uniformBuffers;
    @Shadow @Final private String id;
    
    private static int injectCount = 0;
    
    /**
     * Inject at HEAD of render() to update uniforms before the pass runs.
     */
    @Inject(
        method = "render",
        at = @At("HEAD")
    )
    private void theVirusBlock$updateShockwaveUniforms(
            FrameGraphBuilder frameGraphBuilder,
            Map<Identifier, Handle<Framebuffer>> targets,
            GpuBufferSlice bufferSlice,
            CallbackInfo ci
    ) {
        // Only process if shockwave is enabled
        if (!ShockwavePostEffect.isEnabled()) return;
        
        // Only process shockwave pass (check id contains our shader name)
        if (id == null || !id.contains("shockwave")) return;
        
        injectCount++;
        
        // Log periodically
        if (injectCount % 60 == 1) {
            Logging.RENDER.topic("posteffect_inject")
                .kv("id", id)
                .kv("hasConfig", uniformBuffers.containsKey("ShockwaveConfig"))
                .info("Intercepting shockwave pass");
        }
        
        // Check if this pass has our uniform block
        if (!uniformBuffers.containsKey("ShockwaveConfig")) {
            return;
        }
        
        // Create new buffer with current Java values
        // Layout: 9 vec4s = 144 bytes (full feature set)
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, 160);
            
            // Vec4 0: Basic params
            float radius = ShockwavePostEffect.getCurrentRadius();
            float thickness = ShockwavePostEffect.getThickness();
            float intensity = ShockwavePostEffect.getIntensity();
            float time = (System.currentTimeMillis() % 10000) / 1000.0f;
            builder.putVec4(radius, thickness, intensity, time);
            
            // Vec4 1: Ring count, spacing, contract mode, glow width
            float ringCount = (float) ShockwavePostEffect.getRingCount();
            float ringSpacing = ShockwavePostEffect.getRingSpacing();
            float contractMode = ShockwavePostEffect.isContractMode() ? 1.0f : 0.0f;
            float glowWidth = ShockwavePostEffect.getGlowWidth();
            builder.putVec4(ringCount, ringSpacing, contractMode, glowWidth);
            
            // Vec4 2: Target world position + UseWorldOrigin flag
            float useWorldOrigin = ShockwavePostEffect.isTargetMode() ? 1.0f : 0.0f;
            float targetX = ShockwavePostEffect.getTargetX();
            float targetY = ShockwavePostEffect.getTargetY();
            float targetZ = ShockwavePostEffect.getTargetZ();
            builder.putVec4(targetX, targetY, targetZ, useWorldOrigin);
            
            // Vec4 3: Camera world position + aspect ratio
            // In TARGET mode, use FROZEN camera position from raycast time
            float camX, camY, camZ;
            if (ShockwavePostEffect.isTargetMode()) {
                camX = ShockwavePostEffect.getFrozenCamX();
                camY = ShockwavePostEffect.getFrozenCamY();
                camZ = ShockwavePostEffect.getFrozenCamZ();
            } else {
                camX = ShockwavePostEffect.getCameraX();
                camY = ShockwavePostEffect.getCameraY();
                camZ = ShockwavePostEffect.getCameraZ();
            }
            var client = net.minecraft.client.MinecraftClient.getInstance();
            float aspect = (float) client.getWindow().getFramebufferWidth() / 
                          (float) client.getWindow().getFramebufferHeight();
            builder.putVec4(camX, camY, camZ, aspect);
            
            // Vec4 4: Camera forward direction + FOV
            // Use forward vector computed from Camera object (in WorldRendererShockwaveMixin)
            float fov = (float) Math.toRadians(client.options.getFov().getValue());
            float forwardX = ShockwavePostEffect.getForwardX();
            float forwardY = ShockwavePostEffect.getForwardY();
            float forwardZ = ShockwavePostEffect.getForwardZ();
            builder.putVec4(forwardX, forwardY, forwardZ, fov);
            
            // Vec4 5: Camera up direction (simplified - always world up)
            builder.putVec4(0f, 1f, 0f, 0f);
            
            // Vec4 6: Screen blackout / vignette
            builder.putVec4(
                ShockwavePostEffect.getBlackoutAmount(),
                ShockwavePostEffect.getVignetteAmount(),
                ShockwavePostEffect.getVignetteRadius(),
                0f
            );
            
            // Vec4 7: Color tint
            builder.putVec4(
                ShockwavePostEffect.getTintR(),
                ShockwavePostEffect.getTintG(),
                ShockwavePostEffect.getTintB(),
                ShockwavePostEffect.getTintAmount()
            );
            
            // Vec4 8: Ring color
            builder.putVec4(
                ShockwavePostEffect.getRingR(),
                ShockwavePostEffect.getRingG(),
                ShockwavePostEffect.getRingB(),
                ShockwavePostEffect.getRingOpacity()
            );
            
            GpuBuffer newBuffer = RenderSystem.getDevice().createBuffer(
                () -> "ShockwaveConfig Dynamic",
                16, // uniform buffer usage
                builder.get()
            );
            
            // Replace in map
            GpuBuffer oldBuffer = uniformBuffers.put("ShockwaveConfig", newBuffer);
            
            if (injectCount % 60 == 1) {
                Logging.RENDER.topic("posteffect_inject")
                    .kv("radius", String.format("%.1f", radius))
                    .kv("worldMode", useWorldOrigin > 0.5f)
                    .kv("target", String.format("%.0f,%.0f,%.0f", targetX, targetY, targetZ))
                    .info("Updated ShockwaveConfig UBO!");
            }
            
        } catch (Exception e) {
            Logging.RENDER.topic("posteffect_inject")
                .kv("error", e.getMessage())
                .warn("Failed to update ShockwaveConfig");
        }
    }
}
