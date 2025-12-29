package net.cyberpunk042.client.visual.shader;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

/**
 * Corona shader pipeline with additive blending.
 * 
 * <p>The Corona pipeline is designed for rendering a SECOND pass
 * that adds rim glow ON TOP of existing geometry. Key differences from Fresnel:</p>
 * 
 * <ul>
 *   <li><b>Additive Blending</b>: src + dst (glow adds to scene)</li>
 *   <li><b>No Depth Write</b>: Doesn't occlude anything</li>
 *   <li><b>Depth Test LEQUAL</b>: Only renders where geometry exists</li>
 * </ul>
 * 
 * <h2>Rendering Flow</h2>
 * <pre>
 * Pass 1: Base sphere with normal/Horizon shader
 * Pass 2: Corona overlay → CORONA_ENTITY_ADDITIVE pipeline
 * </pre>
 * 
 * @see CoronaRenderLayers
 * @see CoronaUniformManager
 */
public final class CoronaPipelines {
    
    private static final Identifier SHADER_ID = 
        Identifier.of("the-virus-block", "core/corona_entity");
    
    /**
     * Corona pipeline with translucent blending for overlay glow.
     * 
     * <p>This pipeline uses translucent blend so the glow
     * adds to whatever is already rendered, creating a halo effect.</p>
     */
    public static final RenderPipeline CORONA_ENTITY_ADDITIVE;
    
    static {
        CORONA_ENTITY_ADDITIVE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                // Pipeline location
                .withLocation(SHADER_ID)
                
                // Custom shaders
                .withVertexShader(SHADER_ID)
                .withFragmentShader(SHADER_ID)
                
                // Samplers
                .withSampler("Sampler0")  // Main texture  
                .withSampler("Sampler1")  // Overlay
                .withSampler("Sampler2")  // Lightmap
                
                // Vertex format - same as entity
                .withVertexFormat(
                    VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                    VertexFormat.DrawMode.TRIANGLES
                )
                
                // Render state
                .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withCull(true)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthWrite(false)  // Don't write depth - we're an overlay
                .withColorWrite(true, true)
                
                .build()
        );
    }
    
    /**
     * Initializes the Corona pipelines.
     * Call during client initialization.
     */
    public static void init() {
        // Static initializer does the work
    }
    
    private CoronaPipelines() {}
}
