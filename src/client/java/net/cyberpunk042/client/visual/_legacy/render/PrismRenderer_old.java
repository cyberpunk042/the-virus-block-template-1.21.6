package net.cyberpunk042.client.visual._legacy.render;

import net.cyberpunk042.client.field._legacy.render.PrimitiveRenderer_old;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual._legacy.mesh.PrismTessellator_old;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.field._legacy.primitive.PrismPrimitive_old;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.shape.PrismShape;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import net.cyberpunk042.client.visual._legacy.render.RenderOverrides_old;

/**
 * Renders prism primitives (n-sided cylinders/columns).
 * 
 * <h2>Geometry</h2>
 * <p>Prisms are vertical cylinders with polygonal cross-sections:
 * <ul>
 *   <li>3 sides = triangular prism</li>
 *   <li>4 sides = square column</li>
 *   <li>6 sides = hexagonal prism</li>
 *   <li>High sides = cylindrical approximation</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Crystalline structures</li>
 *   <li>Energy pillars</li>
 *   <li>Architectural elements</li>
 * </ul>
 * 
 * @see PrismPrimitive_old
 * @see PrismTessellator_old
 * @see CylinderRenderer_old
 */
public final class PrismRenderer_old implements PrimitiveRenderer_old {
    
    public static final PrismRenderer_old INSTANCE = new PrismRenderer_old();
    private static final String TYPE = "prism";
    
    private PrismRenderer_old() {
        Logging.RENDER.topic("prism").debug("PrismRenderer_old initialized");
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
        if (!(primitive instanceof PrismPrimitive_old prism)) {
            Logging.RENDER.topic("prism").warn("Render called with non-prism primitive");
            return;
        }
        
        PrismShape shape = (PrismShape) prism.shape();
        int color = colorResolver.resolve(prism.appearance().color());
        float alpha = prism.appearance().alpha().midpoint();
        
        // Tessellate prism
        Mesh mesh = PrismTessellator_old.create()
            .sides(shape.sides())
            .radius(shape.radius())
            .height(shape.height())
            .caps(prism.isCapped())
            .tessellate(0);
        
        int argb = VertexEmitter.withAlpha(color, alpha);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        VertexEmitter.emitMesh(consumer, mesh, matrix, argb, light);
        
        Logging.RENDER.topic("prism").trace(
            "Rendered prism: sides={} r={:.2f} h={:.2f} capped={} verts={}",
            shape.sides(), shape.radius(), shape.height(), prism.isCapped(), mesh.vertexCount());
    }
}
