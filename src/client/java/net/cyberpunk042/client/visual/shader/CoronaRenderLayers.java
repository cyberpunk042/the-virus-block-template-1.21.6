package net.cyberpunk042.client.visual.shader;

import net.minecraft.client.render.RenderLayer;

/**
 * RenderLayers for the Corona additive overlay effect.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // After rendering base geometry...
 * 
 * // Set corona parameters
 * CoronaUniformManager.setParams(coronaEffect);
 * 
 * // Get corona overlay layer
 * RenderLayer layer = CoronaRenderLayers.coronaAdditive();
 * VertexConsumer consumer = vertexConsumers.getBuffer(layer);
 * 
 * // Render same geometry again for glow overlay
 * </pre>
 * 
 * @see CoronaPipelines
 * @see CoronaUniformManager
 */
public final class CoronaRenderLayers {
    
    private static RenderLayer CORONA_ADDITIVE;
    
    /**
     * Returns additive corona overlay layer.
     * 
     * <p>This layer uses additive blending (src + dst) and doesn't
     * write to the depth buffer, making it suitable for glow overlays.</p>
     * 
     * <p>Uses Z-offset layering to render in front of base geometry.</p>
     */
    public static RenderLayer coronaAdditive() {
        if (CORONA_ADDITIVE == null) {
            CORONA_ADDITIVE = RenderLayer.of(
                "corona_entity_additive",
                256,
                CoronaPipelines.CORONA_ENTITY_ADDITIVE,
                RenderLayer.MultiPhaseParameters.builder()
                    .layering(net.minecraft.client.render.RenderPhase.VIEW_OFFSET_Z_LAYERING_FORWARD)
                    .build(false)
            );
        }
        return CORONA_ADDITIVE;
    }
    
    /**
     * Returns additive corona overlay layer without culling.
     * Use for double-sided geometry.
     */
    public static RenderLayer coronaAdditiveNoCull() {
        // TODO: Create separate pipeline without cull
        return coronaAdditive();
    }
    
    private CoronaRenderLayers() {}
}
