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
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, 32);
            
            float radius = ShockwavePostEffect.getCurrentRadius();
            float thickness = ShockwavePostEffect.getThickness();
            float intensity = ShockwavePostEffect.getIntensity();
            float time = (System.currentTimeMillis() % 10000) / 1000.0f;
            
            // Pack as vec4
            builder.putVec4(radius, thickness, intensity, time);
            builder.putVec4(0f, 0f, 0f, 0f); // padding
            
            // Create new GPU buffer
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
                    .kv("replaced", oldBuffer != null)
                    .info("Updated ShockwaveConfig UBO!");
            }
            
        } catch (Exception e) {
            Logging.RENDER.topic("posteffect_inject")
                .kv("error", e.getMessage())
                .warn("Failed to update ShockwaveConfig");
        }
    }
}
