package net.cyberpunk042.client.visual._legacy.render;

import net.cyberpunk042.client.field._legacy.render.PrimitiveRenderer_old;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual._legacy.mesh.PolyhedraTessellator_old;
import net.cyberpunk042.field._legacy.primitive.PolyhedronPrimitive_old;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.appearance.FillMode;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.shape.PolyhedronShape;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import net.cyberpunk042.client.visual._legacy.render.RenderOverrides_old;

/**
 * Renders polyhedron primitives (cubes, octahedrons, icosahedrons, etc.).
 * 
 * <p>Uses {@link PolyhedraTessellator_old} to generate mesh for each polyhedron type.
 * 
 * <h2>Supported Types</h2>
 * <ul>
 *   <li>CUBE - 6 faces, classic box</li>
 *   <li>OCTAHEDRON - 8 faces, diamond</li>
 *   <li>ICOSAHEDRON - 20 faces</li>
 *   <li>DODECAHEDRON - 12 faces</li>
 *   <li>TETRAHEDRON - 4 faces</li>
 * </ul>
 */
public final class PolyhedronRenderer_old implements PrimitiveRenderer_old {
    
    private static final String TYPE = "polyhedron";
    
    public static final PolyhedronRenderer_old INSTANCE = new PolyhedronRenderer_old();
    
    private PolyhedronRenderer_old() {}
    
    @Override
    public String type() {
        return "polyhedron";
    }
    
    @Override
    public boolean supports(String type) {
        return TYPE.equals(type);
    }
    
    @Override
    public void render(
            Primitive primitive,
            MatrixStack matrices,
            VertexConsumer consumer,
            int light,
            float time,
            ColorResolver colorResolver,
            RenderOverrides_old overrides) {
        
        if (!(primitive instanceof PolyhedronPrimitive_old poly)) {
            Logging.RENDER.topic("polyhedron").warn("Expected PolyhedronPrimitive_old, got: {}", 
                primitive.getClass().getSimpleName());
            return;
        }
        
        PolyhedronShape shape = poly.getPolyShape();
        Appearance appearance = poly.appearance();
        
        // Resolve color
        int color = colorResolver.resolve(appearance.color());
        float alpha = appearance.alpha().at(time * 0.1f);
        
        // Apply alpha to color
        int a = (int) (alpha * 255) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int argb = (a << 24) | (r << 16) | (g << 8) | b;
        
        // Check fill mode
        boolean wireframe = appearance.fill() == FillMode.WIREFRAME;
        
        // Tessellate the polyhedron
        Mesh mesh = PolyhedraTessellator_old.builder()
            .type(shape.type())
            .radius(shape.size())
            .build()
            .tessellate();
        
        if (mesh == null || mesh.isEmpty()) {
            Logging.RENDER.topic("polyhedron").warn("Failed to tessellate polyhedron: {}", shape.type());
            return;
        }
        
        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        if (wireframe) {
            VertexEmitter.emitWireframe(consumer, mesh, matrix, argb, appearance.wireThickness(), light);
        } else {
            VertexEmitter.emitMesh(consumer, mesh, matrix, argb, light);
        }
        
        matrices.pop();
        
        Logging.RENDER.topic("polyhedron").trace(
            "Rendered polyhedron: type={}, size={}, fill={}", 
            shape.type(), shape.size(), appearance.fill());
    }
}

