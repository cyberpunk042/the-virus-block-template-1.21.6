package net.cyberpunk042.client.visual._legacy.render;

import net.cyberpunk042.client.field._legacy.render.PrimitiveRenderer_old;
import net.cyberpunk042.client.visual._legacy.mesh.DiscTessellator_old;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.field._legacy.primitive.DiscPrimitive_old;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.appearance.FillMode;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.shape.DiscShape;
import net.cyberpunk042.visual.pattern.SectorPattern;
import net.cyberpunk042.visual.pattern.DynamicSectorPattern;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import net.cyberpunk042.client.visual._legacy.render.RenderOverrides_old;

/**
 * Renders disc primitives (flat filled circles).
 * 
 * <h2>Rendering Pipeline</h2>
 * <ol>
 *   <li>Extract shape parameters from DiscPrimitive_old</li>
 *   <li>Resolve color from theme/palette via ColorResolver</li>
 *   <li>Tessellate disc via DiscTessellator_old (triangle fan)</li>
 *   <li>Emit mesh vertices with VertexEmitter</li>
 * </ol>
 * 
 * <h2>Rendering Modes</h2>
 * <ul>
 *   <li><b>Solid</b>: Filled circle with triangle fan</li>
 *   <li><b>Wireframe</b>: Circle outline with radial spokes</li>
 * </ul>
 * 
 * @see DiscPrimitive_old
 * @see DiscTessellator_old
 * @see RingRenderer_old
 */
public final class DiscRenderer_old implements PrimitiveRenderer_old {
    
    public static final DiscRenderer_old INSTANCE = new DiscRenderer_old();
    private static final String TYPE = "disc";
    
    private DiscRenderer_old() {
        Logging.RENDER.topic("disc").debug("DiscRenderer_old initialized");
    }
    
    @Override
    public String type() {
        return TYPE;
    }
    
    @Override
    public boolean supports(String type) {
        return TYPE.equals(type);
    }
    
    @Override
    public void render(Primitive primitive, MatrixStack matrices, VertexConsumer consumer,
                       int light, float time, ColorResolver colorResolver, RenderOverrides_old overrides) {
        if (!(primitive instanceof DiscPrimitive_old disc)) {
            Logging.RENDER.topic("disc").warn("Render called with non-disc primitive: {}", 
                primitive != null ? primitive.type() : "null");
            return;
        }
        
        DiscShape shape = disc.discShape();
        int color = colorResolver.resolve(disc.appearance().color());
        float alpha = disc.appearance().alpha().midpoint();
        
        // Get sector pattern (from overrides or appearance)
        // Can be static SectorPattern or dynamic DynamicSectorPattern
        SectorPattern staticPattern = null;
        DynamicSectorPattern dynamicPattern = null;
        String patternId = "default";
        
        if (overrides != null && overrides.vertexPattern() != null) {
            VertexPattern vp = overrides.vertexPattern();
            if (vp instanceof DynamicSectorPattern dsp) {
                dynamicPattern = dsp;
                patternId = dsp.id();
            } else if (vp instanceof SectorPattern sp) {
                staticPattern = sp;
                patternId = sp.id();
            }
        } else if (disc.appearance().pattern() != null) {
            staticPattern = disc.appearance().pattern().sectorPattern();
            patternId = staticPattern.id();
        }
        
        // Check fill mode for wireframe rendering
        FillMode fillMode = disc.appearance().fill();
        boolean wireframe = fillMode == FillMode.WIREFRAME;
        
        int argb = VertexEmitter.withAlpha(color, alpha);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        // Render disc as triangle fan with pattern
        float radius = shape.radius();
        float y = shape.y();
        int segments = shape.segments();
        
        // Render sectors based on pattern
        for (int i = 0; i < segments; i++) {
            // Skip sectors based on pattern
            boolean shouldRender = true;
            if (dynamicPattern != null) {
                // Dynamic pattern uses skipInterval/phase/invert
                int adjusted = (i + dynamicPattern.phaseOffset()) % segments;
                boolean matches = (adjusted % dynamicPattern.skipInterval()) == 0;
                shouldRender = dynamicPattern.invertSelection() ? !matches : matches;
            } else if (staticPattern != null) {
                // Static pattern uses shouldRender method
                shouldRender = staticPattern.shouldRender(i, segments);
            }
            
            if (!shouldRender) {
                continue;
            }
            
            float angle1 = (float) (2 * Math.PI * i / segments);
            float angle2 = (float) (2 * Math.PI * (i + 1) / segments);
            
            float cos1 = (float) Math.cos(angle1), sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2), sin2 = (float) Math.sin(angle2);
            
            float x1 = radius * cos1, z1 = radius * sin1;
            float x2 = radius * cos2, z2 = radius * sin2;
            
            // Triangle from center to edge
            consumer.vertex(matrix, 0, y, 0).color(argb)
                .texture(0.5f, 0.5f).overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light).normal(0, 1, 0);
            consumer.vertex(matrix, x1, y, z1).color(argb)
                .texture(0, 0).overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light).normal(0, 1, 0);
            consumer.vertex(matrix, x2, y, z2).color(argb)
                .texture(1, 0).overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light).normal(0, 1, 0);
        }
        
        Logging.RENDER.topic("disc").trace(
            "Rendered disc: y={:.2f} r={:.2f} alpha={:.2f} segs={} pattern={}",
            shape.y(), shape.radius(), alpha, segments, patternId);
    }
}

