package net.cyberpunk042.client.visual._legacy.render;

import net.cyberpunk042.client.field._legacy.render.PrimitiveRenderer_old;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual._legacy.mesh.PrismTessellator_old;
import net.cyberpunk042.field._legacy.primitive.CylinderPrimitive_old;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.shape.CylinderShape;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import net.cyberpunk042.client.visual._legacy.render.RenderOverrides_old;

/**
 * Renders beam primitives (vertical prism columns).
 * 
 * <h2>Geometry</h2>
 * <p>Beams are rendered as vertical prisms using PrismTessellator_old:
 * <ul>
 *   <li>Circular cross-section (high segment count)</li>
 *   <li>Extends from y=0 upward by height</li>
 *   <li>No end caps (open cylinder)</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Beacon-style light beams</li>
 *   <li>Energy pillars</li>
 *   <li>Teleport effects</li>
 * </ul>
 * 
 * @see CylinderPrimitive_old
 * @see PrismTessellator_old
 */
public final class CylinderRenderer_old implements PrimitiveRenderer_old {
    
    public static final CylinderRenderer_old INSTANCE = new CylinderRenderer_old();
    private static final String TYPE = "beam";
    
    private CylinderRenderer_old() {
        Logging.RENDER.topic("beam").debug("CylinderRenderer_old initialized");
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
        if (!(primitive instanceof CylinderPrimitive_old beam)) {
            Logging.RENDER.topic("beam").warn("Render called with non-beam primitive");
            return;
        }
        
        CylinderShape shape = (CylinderShape) beam.shape();
        int color = colorResolver.resolve(beam.appearance().color());
        float alpha = beam.appearance().alpha().midpoint();
        
        // Tessellate beam as a prism (vertical cylinder)
        Mesh mesh = PrismTessellator_old.create()
            .sides(shape.segments())
            .radius(shape.radius())
            .height(shape.height())
            .caps(false)  // Beams are open-ended
            .tessellate(0);
        
        // Combine color and alpha into ARGB
        int argb = VertexEmitter.withAlpha(color, alpha);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        // Emit vertices
        VertexEmitter.emitMesh(consumer, mesh, matrix, argb, light);
        
        Logging.RENDER.topic("beam").trace(
            "Rendered beam: r={:.2f} h={:.2f} segs={} verts={}",
            shape.radius(), shape.height(), shape.segments(), mesh.vertexCount());
    }
}
