package net.cyberpunk042.client.field._legacy.render;

import net.cyberpunk042.client.visual._legacy.render.RenderOverrides_old;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.color.ColorResolver;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Interface for rendering field primitives.
 * 
 * <h2>Renderer Hierarchy</h2>
 * <pre>
 * PrimitiveRenderer_old (interface)
 * ├── SphereRenderer_old
 * ├── RingRenderer_old
 * ├── CylinderRenderer_old
 * └── CageRenderer_old
 * </pre>
 * 
 * <h2>Usage</h2>
 * <pre>
 * PrimitiveRenderer_old renderer = PrimitiveRenderers_old.get(primitive.type());
 * renderer.render(primitive, matrices, consumer, light, time, resolver, overrides);
 * </pre>
 * 
 * @see PrimitiveRenderers_old
 */
public interface PrimitiveRenderer_old {
    
    /**
     * Renders a primitive with optional overrides.
     * 
     * @param primitive the primitive to render
     * @param matrices transformation matrix stack
     * @param consumer vertex consumer for output
     * @param light packed light value
     * @param time animation time in ticks
     * @param colorResolver resolves color references
     * @param overrides runtime overrides (null = use primitive defaults)
     */
    void render(Primitive primitive, MatrixStack matrices, VertexConsumer consumer,
                int light, float time, ColorResolver colorResolver, RenderOverrides_old overrides);
    
    /**
     * Renders a primitive without overrides (convenience method).
     */
    default void render(Primitive primitive, MatrixStack matrices, VertexConsumer consumer,
                int light, float time, ColorResolver colorResolver) {
        render(primitive, matrices, consumer, light, time, colorResolver, null);
    }
    
    /**
     * Checks if this renderer supports the given primitive type.
     * @param type primitive type string
     */
    boolean supports(String type);
    
    /**
     * Gets the primitive type this renderer handles.
     */
    String type();
}
