package net.cyberpunk042.client.visual.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.effect.CoronaEffect;
import net.cyberpunk042.visual.effect.HorizonEffect;
import org.lwjgl.system.MemoryStack;

/**
 * Binds custom uniforms (FresnelParams, CoronaParams) to the RenderPass.
 * 
 * <p>This is called from the RenderLayerMultiPhaseMixin at the appropriate
 * point in the render pipeline - after DynamicTransforms is bound but before
 * geometry is drawn.</p>
 * 
 * <h2>Uniform Layout (std140)</h2>
 * <pre>
 * FresnelParams (32 bytes):
 *   vec4 RimColorAndPower;      // xyz = RimColor, w = RimPower
 *   vec4 RimIntensityAndPad;    // x = RimIntensity, yzw = padding
 * 
 * CoronaParams (48 bytes):
 *   vec4 CoronaColorAndPower;        // xyz = CoronaColor, w = CoronaPower
 *   vec4 CoronaIntensityFalloff;     // x = CoronaIntensity, y = CoronaFalloff, zw = padding
 *   vec4 CoronaOffsetWidthPad;       // x = Offset, y = Width, zw = padding
 * </pre>
 * 
 * @see FresnelPipelines
 * @see CoronaPipelines
 */
public final class CustomUniformBinder {
    
    private static final int FRESNEL_BUFFER_SIZE = 32;  // 2 x vec4 = 32 bytes
    private static final int CORONA_BUFFER_SIZE = 48;   // 3 x vec4 = 48 bytes
    
    // Thread-local storage for current effect parameters
    private static HorizonEffect currentHorizon = null;
    private static CoronaEffect currentCorona = null;
    
    /**
     * Sets the current Horizon (Fresnel) effect parameters.
     * Call this before rendering with a Fresnel RenderLayer.
     */
    public static void setHorizonParams(HorizonEffect effect) {
        currentHorizon = effect;
    }
    
    /**
     * Sets the current Corona effect parameters.
     * Call this before rendering with a Corona RenderLayer.
     */
    public static void setCoronaParams(CoronaEffect effect) {
        currentCorona = effect;
    }
    
    /**
     * Clears all effect parameters.
     * Call this after rendering is complete.
     */
    public static void reset() {
        currentHorizon = null;
        currentCorona = null;
    }
    
    /**
     * Binds custom uniforms for the given pipeline.
     * 
     * <p>Called from the Mixin injection point. Checks if the pipeline
     * matches one of our custom pipelines and binds the appropriate
     * uniform buffer.</p>
     * 
     * @param renderPass The active RenderPass
     * @param pipeline The pipeline being used for this draw call
     */
    public static void bindForPipeline(RenderPass renderPass, RenderPipeline pipeline) {
        // Debug: Log every call to see if the Mixin is working
        Logging.FIELD.topic("shader").info(
            "[MIXIN] bindForPipeline called with pipeline={}, horizon={}, corona={}",
            pipeline != null ? pipeline.toString() : "null",
            currentHorizon != null,
            currentCorona != null
        );
        
        // Check if this is our Fresnel pipeline
        if (pipeline == FresnelPipelines.FRESNEL_ENTITY_TRANSLUCENT && currentHorizon != null) {
            Logging.FIELD.topic("shader").info("[MIXIN] Binding Fresnel uniforms!");
            bindFresnelParams(renderPass, currentHorizon);
            return;
        }
        
        // Check if this is our Corona pipeline
        if (pipeline == CoronaPipelines.CORONA_ENTITY_ADDITIVE && currentCorona != null) {
            Logging.FIELD.topic("shader").info("[MIXIN] Binding Corona uniforms!");
            bindCoronaParams(renderPass, currentCorona);
            return;
        }
        
        // Not our pipeline - nothing to bind
    }
    
    /**
     * Binds custom uniforms to the RenderPass based on current effect parameters.
     * 
     * <p>Called from RenderSystemMixin after bindDefaultUniforms completes.
     * This method doesn't check the pipeline - it just binds whatever effects
     * are currently set. The effects are set by LayerRenderer before drawing.</p>
     * 
     * @param renderPass The active RenderPass
     */
    public static void bindCustomUniforms(RenderPass renderPass) {
        // Bind Fresnel/Horizon if set
        if (currentHorizon != null) {
            Logging.FIELD.topic("shader").info(
                "[MIXIN] bindCustomUniforms: Binding Horizon effect!"
            );
            bindFresnelParams(renderPass, currentHorizon);
        }
        
        // Bind Corona if set
        if (currentCorona != null) {
            Logging.FIELD.topic("shader").warn(
                "[MIXIN] bindCustomUniforms: Binding Corona effect! intensity={}",
                currentCorona.intensity()
            );
            bindCoronaParams(renderPass, currentCorona);
        }
    }
    
    /**
     * Binds FresnelParams uniform buffer with BOTH Horizon AND Corona parameters.
     * Since Corona UBO binding doesn't work as a separate pass, we combine them.
     */
    private static void bindFresnelParams(RenderPass renderPass, HorizonEffect effect) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Combined buffer: 6 x vec4 = 96 bytes
            // 2 for Horizon + 4 for Corona
            Std140Builder builder = Std140Builder.onStack(stack, 96);
            
            // === HORIZON (Rim Lighting) ===
            // vec4 RimColorAndPower (rgb = color, w = power)
            builder.putVec4(
                effect.red(),
                effect.green(),
                effect.blue(),
                effect.power()
            );
            
            // vec4 RimIntensityAndPad (x = intensity, yzw = padding)
            builder.putVec4(
                effect.intensity(),
                0.0f,
                0.0f,
                0.0f
            );
            
            // === CORONA (Outer Glow) ===
            // Get corona params (may be null if not enabled)
            CoronaEffect corona = currentCorona;
            if (corona != null && corona.enabled()) {
                // vec4 CoronaColorAndPower
                builder.putVec4(
                    corona.red(),
                    corona.green(),
                    corona.blue(),
                    corona.power()
                );
                
                // vec4 CoronaIntensityFalloff
                builder.putVec4(
                    corona.intensity(),
                    corona.falloff(),
                    0.0f,
                    0.0f
                );
                
                // vec4 CoronaOffsetWidthPad
                builder.putVec4(
                    corona.offset(),
                    corona.width(),
                    0.0f,
                    0.0f
                );
            } else {
                // Corona disabled - write zeros
                builder.putVec4(0f, 0f, 0f, 0f);
                builder.putVec4(0f, 0f, 0f, 0f);
                builder.putVec4(0f, 0f, 0f, 0f);
            }
            
            // Upload buffer to GPU and bind
            GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(
                () -> "FresnelParams UBO (combined)",
                16, // usage: uniform buffer
                builder.get()
            );
            renderPass.setUniform("FresnelParams", gpuBuffer);
            
            if (corona != null && corona.enabled()) {
                Logging.FIELD.topic("shader").info(
                    "[FRESNEL+CORONA] Bound combined uniforms: horizon=({},{},{}) corona=({},{},{})",
                    effect.red(), effect.green(), effect.blue(),
                    corona.red(), corona.green(), corona.blue()
                );
            } else {
                Logging.FIELD.topic("shader").info(
                    "[FRESNEL] Bound uniforms: color=({},{},{}), power={}, intensity={}",
                    effect.red(), effect.green(), effect.blue(),
                    effect.power(), effect.intensity()
                );
            }
        } catch (Exception e) {
            Logging.FIELD.topic("shader").warn("[FRESNEL] Failed to bind uniforms: {}", e.getMessage());
        }
    }
    
    /**
     * Binds CoronaParams uniform buffer.
     */
    private static void bindCoronaParams(RenderPass renderPass, CoronaEffect effect) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, CORONA_BUFFER_SIZE);
            
            // vec4 CoronaColorAndPower (rgb = color, w = power)
            builder.putVec4(
                effect.red(),
                effect.green(),
                effect.blue(),
                effect.power()
            );
            
            // vec4 CoronaIntensityFalloff (x = intensity, y = falloff, zw = padding)
            builder.putVec4(
                effect.intensity(),
                effect.falloff(),
                0.0f,
                0.0f
            );
            
            // vec4 CoronaOffsetWidthPad (x = offset, y = width, zw = padding)
            builder.putVec4(
                effect.offset(),
                effect.width(),
                0.0f,
                0.0f
            );
            
            // Upload buffer to GPU and bind
            GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(
                () -> "CoronaParams UBO",
                16, // usage: uniform buffer
                builder.get()
            );
            renderPass.setUniform("CoronaParams", gpuBuffer);
            
            Logging.FIELD.topic("shader").info(
                "[CORONA] Bound uniforms: color=({},{},{}), power={}, intensity={}, falloff={}, offset={}, width={}",
                effect.red(), effect.green(), effect.blue(),
                effect.power(), effect.intensity(), effect.falloff(), effect.offset(), effect.width()
            );
        } catch (Exception e) {
            Logging.FIELD.topic("shader").warn("[CORONA] Failed to bind uniforms: {}", e.getMessage());
        }
    }
    
    private CustomUniformBinder() {}
}
