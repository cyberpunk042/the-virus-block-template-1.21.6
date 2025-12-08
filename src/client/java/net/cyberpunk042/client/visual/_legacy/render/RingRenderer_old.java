package net.cyberpunk042.client.visual._legacy.render;

import net.cyberpunk042.client.field._legacy.render.PrimitiveRenderer_old;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual._legacy.mesh.RingTessellator_old;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.field._legacy.primitive.RingPrimitive_old;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.shape.RingShape;
import net.cyberpunk042.visual.pattern.SegmentPattern;
import net.cyberpunk042.visual.pattern.DynamicSegmentPattern;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import net.cyberpunk042.client.visual._legacy.render.RenderOverrides_old;

/**
 * Renders ring primitives (torus/donut shapes).
 * 
 * <h2>Geometry</h2>
 * <p>Rings are horizontal annuli (flat donuts) at a specific Y level:
 * <ul>
 *   <li>Inner radius = radius - thickness/2</li>
 *   <li>Outer radius = radius + thickness/2</li>
 *   <li>Normal points up (+Y)</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Halos and auras</li>
 *   <li>Field boundaries</li>
 *   <li>Targeting circles</li>
 * </ul>
 * 
 * @see RingPrimitive_old
 * @see RingTessellator_old
 * @see DiscRenderer_old
 */
public final class RingRenderer_old implements PrimitiveRenderer_old {
    
    public static final RingRenderer_old INSTANCE = new RingRenderer_old();
    private static final String TYPE = "ring";
    
    private RingRenderer_old() {
        Logging.RENDER.topic("ring").debug("RingRenderer_old initialized");
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
        if (!(primitive instanceof RingPrimitive_old ring)) {
            Logging.RENDER.topic("ring").warn("Render called with non-ring primitive");
            return;
        }
        
        RingShape shape = (RingShape) ring.shape();
        int color = colorResolver.resolve(ring.appearance().color());
        float alpha = ring.appearance().alpha().midpoint();
        
        // Get segment pattern (from overrides or appearance)
        // Can be static SegmentPattern or dynamic DynamicSegmentPattern
        SegmentPattern staticPattern = null;
        DynamicSegmentPattern dynamicPattern = null;
        boolean reverseWinding = false;
        String patternId = "default";
        
        if (overrides != null && overrides.vertexPattern() != null) {
            VertexPattern vp = overrides.vertexPattern();
            if (vp instanceof DynamicSegmentPattern dsp) {
                dynamicPattern = dsp;
                reverseWinding = dsp.reverseWinding();
                patternId = dsp.id();
            } else if (vp instanceof SegmentPattern sp) {
                staticPattern = sp;
                reverseWinding = sp.reverseWinding();
                patternId = sp.id();
            }
        } else if (ring.appearance().pattern() != null) {
            staticPattern = ring.appearance().pattern().segmentPattern();
            reverseWinding = staticPattern.reverseWinding();
            patternId = staticPattern.id();
        }
        
        // Tessellate ring as an annulus with pattern
        int segments = shape.segments();
        float innerR = shape.radius() - shape.thickness() / 2;
        float outerR = shape.radius() + shape.thickness() / 2;
        float y = shape.y();
        
        int argb = VertexEmitter.withAlpha(color, alpha);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        // Render segments based on pattern
        for (int i = 0; i < segments; i++) {
            // Skip segments based on pattern
            boolean shouldRender = true;
            if (dynamicPattern != null) {
                // Dynamic pattern uses skipInterval/phase
                int adjusted = (i + dynamicPattern.phaseOffset());
                shouldRender = (adjusted % dynamicPattern.skipInterval()) == 0;
            } else if (staticPattern != null) {
                // Static pattern uses shouldRender method
                shouldRender = staticPattern.shouldRender(i);
            }
            
            if (!shouldRender) {
                continue;
            }
            
            // Calculate segment angles
            float angle1 = (float) (2 * Math.PI * i / segments);
            float angle2 = (float) (2 * Math.PI * (i + 1) / segments);
            
            // Apply reverse winding if pattern requires
            if (reverseWinding && (i % 2 == 1)) {
                float temp = angle1;
                angle1 = angle2;
                angle2 = temp;
            }
            
            float cos1 = (float) Math.cos(angle1), sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2), sin2 = (float) Math.sin(angle2);
            
            // Four corners of the segment quad
            float x1i = innerR * cos1, z1i = innerR * sin1;
            float x1o = outerR * cos1, z1o = outerR * sin1;
            float x2i = innerR * cos2, z2i = innerR * sin2;
            float x2o = outerR * cos2, z2o = outerR * sin2;
            
            // Emit two triangles per segment
            consumer.vertex(matrix, x1i, y, z1i).color(argb)
                .texture(0, 0).overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light).normal(0, 1, 0);
            consumer.vertex(matrix, x1o, y, z1o).color(argb)
                .texture(0, 0).overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light).normal(0, 1, 0);
            consumer.vertex(matrix, x2o, y, z2o).color(argb)
                .texture(0, 0).overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light).normal(0, 1, 0);
            
            consumer.vertex(matrix, x1i, y, z1i).color(argb)
                .texture(0, 0).overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light).normal(0, 1, 0);
            consumer.vertex(matrix, x2o, y, z2o).color(argb)
                .texture(0, 0).overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light).normal(0, 1, 0);
            consumer.vertex(matrix, x2i, y, z2i).color(argb)
                .texture(0, 0).overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light).normal(0, 1, 0);
        }
        
        Logging.RENDER.topic("ring").trace(
            "Rendered ring: y={:.2f} r={:.2f} thick={:.2f} segs={} pattern={}",
            shape.y(), shape.radius(), shape.thickness(), shape.segments(), patternId);
    }
}
