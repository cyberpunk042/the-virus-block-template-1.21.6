package net.cyberpunk042.client.visual._legacy.render;

import net.cyberpunk042.client.field._legacy.render.PrimitiveRenderer_old;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual._legacy.mesh.RingTessellator_old;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.field._legacy.primitive.RingsPrimitive_old;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.shape.RingShape;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import net.cyberpunk042.client.visual._legacy.render.RenderOverrides_old;

/**
 * Renders multiple concentric rings primitive.
 * 
 * <h2>Geometry</h2>
 * <p>Multiple rings are rendered at different radii, all sharing:
 * <ul>
 *   <li>Same Y level</li>
 *   <li>Same color/alpha (from appearance)</li>
 *   <li>Evenly distributed based on innerRadius/outerRadius</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Target/bullseye patterns</li>
 *   <li>Ripple effects</li>
 *   <li>Layered shields</li>
 * </ul>
 * 
 * @see RingsPrimitive_old
 * @see RingRenderer_old
 */
public final class RingsRenderer_old implements PrimitiveRenderer_old {
    
    public static final RingsRenderer_old INSTANCE = new RingsRenderer_old();
    private static final String TYPE = "rings";
    
    private RingsRenderer_old() {
        Logging.RENDER.topic("rings").debug("RingsRenderer_old initialized");
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
        if (!(primitive instanceof RingsPrimitive_old rings)) {
            Logging.RENDER.topic("rings").warn("Render called with non-rings primitive");
            return;
        }
        
        int color = colorResolver.resolve(rings.appearance().color());
        float alpha = rings.appearance().alpha().midpoint();
        int argb = VertexEmitter.withAlpha(color, alpha);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        int ringCount = rings.getRings().size();
        int totalVerts = 0;
        
        // Render each ring
        for (RingShape ring : rings.getRings()) {
            Mesh mesh = RingTessellator_old.create()
                .y(ring.y())
                .innerRadius(ring.radius() - ring.thickness() / 2)
                .outerRadius(ring.radius() + ring.thickness() / 2)
                .segments(ring.segments())
                .tessellate(0);
            
            VertexEmitter.emitMesh(consumer, mesh, matrix, argb, light);
            totalVerts += mesh.vertexCount();
        }
        
        Logging.RENDER.topic("rings").trace(
            "Rendered {} rings: totalVerts={}", ringCount, totalVerts);
    }
}
