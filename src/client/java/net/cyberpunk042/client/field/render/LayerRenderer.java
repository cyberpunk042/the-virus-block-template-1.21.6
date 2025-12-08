package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.animation.AnimationApplier;
import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.field.primitive.LinkResolver;
import net.cyberpunk042.field.primitive.PrimitiveLink;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.transform.Transform;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;

/**
 * Renders a single layer of a field definition.
 * 
 * <p>Per CLASS_DIAGRAM §8: LayerRenderer applies layer-level transforms
 * and animations, then delegates to appropriate PrimitiveRenderer for
 * each primitive in the layer.
 * 
 * <h2>Rendering Flow</h2>
 * <pre>
 * LayerRenderer.render(layer, ...)
 *    │
 *    ├── Apply layer transform (position, rotation, scale)
 *    ├── Apply layer animation (AnimationApplier)
 *    │
 *    └── for each primitive:
 *           └── PrimitiveRenderers.get(type).render(primitive, ...)
 * </pre>
 * 
 * <h2>Layer Visibility</h2>
 * <p>Layers have a {@code visible} flag. If false, the layer is skipped entirely.</p>
 * 
 * @see FieldRenderer
 * @see PrimitiveRenderer
 * @see AnimationApplier
 */
public final class LayerRenderer {
    
    private LayerRenderer() {}
    
    /**
     * Renders all primitives in a layer.
     * 
     * @param matrices Matrix stack (positioned at field origin)
     * @param consumer Vertex consumer
     * @param layer The layer to render
     * @param resolver Color resolver for theme
     * @param fieldScale Overall field scale
     * @param time Animation time
     * @param alpha Overall alpha
     * @param overrides Optional render overrides
     */
    public static void render(
            MatrixStack matrices,
            VertexConsumer consumer,
            FieldLayer layer,
            ColorResolver resolver,
            float fieldScale,
            float time,
            float alpha,
            RenderOverrides overrides) {
        
        // Check visibility
        if (!layer.visible()) {
            Logging.FIELD.topic("render").trace("Layer '{}' not visible, skipping", layer.id());
            return;
        }
        
        // Skip empty layers
        if (layer.primitives() == null || layer.primitives().isEmpty()) {
            return;
        }
        
        matrices.push();
        
        // === PHASE 1: Apply Layer Transform ===
        applyLayerTransform(matrices, layer, fieldScale);
        
        // === PHASE 2: Apply Layer Animation ===
        if (layer.animation() != null && layer.animation().isActive()) {
            AnimationApplier.apply(matrices, layer.animation(), time);
        }
        
        // === PHASE 3: Render Each Primitive ===
        int light = 0xF000F0; // Full bright for fields
        float effectiveAlpha = alpha * layer.alpha();
        
        for (Primitive primitive : layer.primitives()) {
            renderPrimitive(matrices, consumer, primitive, resolver, light, time, effectiveAlpha, overrides);
        }
        
        matrices.pop();
        
        Logging.FIELD.topic("render").trace(
            "Rendered layer '{}' with {} primitives", 
            layer.id(), layer.primitives().size());
    }
    
    /**
     * Convenience method without overrides.
     */
    public static void render(
            MatrixStack matrices,
            VertexConsumer consumer,
            FieldLayer layer,
            ColorResolver resolver,
            float fieldScale,
            float time,
            float alpha) {
        render(matrices, consumer, layer, resolver, fieldScale, time, alpha, null);
    }
    
    // =========================================================================
    // Transform Application
    // =========================================================================
    
    /**
     * Applies layer transform to the matrix stack.
     * 
     * <p>Transform order: translate → rotate → scale
     */
    private static void applyLayerTransform(MatrixStack matrices, FieldLayer layer, float fieldScale) {
        Transform transform = layer.transform();
        if (transform == null || transform == Transform.IDENTITY) {
            return;
        }
        
        // 1. Translation (offset)
        if (transform.offset() != null) {
            matrices.translate(
                transform.offset().x * fieldScale,
                transform.offset().y * fieldScale,
                transform.offset().z * fieldScale
            );
        }
        
        // 2. Rotation
        if (transform.rotation() != null) {
            Quaternionf rotation = new Quaternionf()
                .rotateX((float) Math.toRadians(transform.rotation().x))
                .rotateY((float) Math.toRadians(transform.rotation().y))
                .rotateZ((float) Math.toRadians(transform.rotation().z));
            matrices.multiply(rotation);
        }
        
        // 3. Scale
        float scaleX = (transform.scaleXYZ() != null ? transform.scaleXYZ().x : transform.scale()) * fieldScale;
        float scaleY = (transform.scaleXYZ() != null ? transform.scaleXYZ().y : transform.scale()) * fieldScale;
        float scaleZ = (transform.scaleXYZ() != null ? transform.scaleXYZ().z : transform.scale()) * fieldScale;
        matrices.scale(scaleX, scaleY, scaleZ);
        
        Logging.FIELD.topic("render").trace(
            "Applied transform: offset={}, rotation={}, scale=({}, {}, {})",
            transform.offset(), transform.rotation(), scaleX, scaleY, scaleZ);
    }
    
    // =========================================================================
    // Primitive Rendering
    // =========================================================================
    
    /**
     * Renders a single primitive with its own transform and animation.
     */
    private static void renderPrimitive(
            MatrixStack matrices,
            VertexConsumer consumer,
            Primitive primitive,
            ColorResolver resolver,
            int light,
            float time,
            float alpha,
            RenderOverrides overrides) {
        
        matrices.push();
        
        // Apply primitive transform
        Transform transform = primitive.transform();
        if (transform != null && transform != Transform.IDENTITY) {
            applyPrimitiveTransform(matrices, transform);
        }
        
        // F184: Apply primitive animation with link phase offset
        if (primitive.animation() != null && primitive.animation().isActive()) {
            float effectiveTime = time + getLinkPhaseOffset(primitive);
            AnimationApplier.apply(matrices, primitive.animation(), effectiveTime);
        }
        
        // Apply alpha from primitive appearance
        float effectiveAlpha = alpha;
        if (primitive.appearance() != null && primitive.appearance().alpha() != null) {
            // AlphaRange - use max value for now (could use min or average)
            float alphaValue = primitive.appearance().alpha().max();
            if (alphaValue < 1.0f) {
                effectiveAlpha *= alphaValue;
            }
        }
        
        // Apply overrides alpha if present
        if (overrides != null) {
            effectiveAlpha *= overrides.alphaMultiplier();
        }
        
        // Get renderer and render
        PrimitiveRenderer renderer = PrimitiveRenderers.get(primitive);
        if (renderer != null) {
            // Create effective overrides with alpha
            RenderOverrides effectiveOverrides = overrides;
            if (effectiveAlpha != alpha && overrides == null) {
                effectiveOverrides = RenderOverrides.withAlpha(effectiveAlpha / alpha);
            }
            
            renderer.render(primitive, matrices, consumer, light, time, resolver, effectiveOverrides);
        } else {
            Logging.FIELD.topic("render")
                .reason("Missing renderer")
                .warn("No renderer for primitive type: {}", primitive.type());
        }
        
        matrices.pop();
    }
    
    /**
     * Applies primitive-level transform.
     */
    private static void applyPrimitiveTransform(MatrixStack matrices, Transform transform) {
        // Translation
        if (transform.offset() != null) {
            matrices.translate(
                transform.offset().x,
                transform.offset().y,
                transform.offset().z
            );
        }
        
        // Rotation
        if (transform.rotation() != null) {
            Quaternionf rotation = new Quaternionf()
                .rotateX((float) Math.toRadians(transform.rotation().x))
                .rotateY((float) Math.toRadians(transform.rotation().y))
                .rotateZ((float) Math.toRadians(transform.rotation().z));
            matrices.multiply(rotation);
        }
        
        // Scale
        if (transform.scaleXYZ() != null) {
            matrices.scale(transform.scaleXYZ().x, transform.scaleXYZ().y, transform.scaleXYZ().z);
        } else {
            matrices.scale(transform.scale(), transform.scale(), transform.scale());
        }
    }
    
    // =========================================================================
    // F184: Link Resolution
    // =========================================================================
    
    /**
     * Gets the phase offset from a primitive's link configuration.
     * 
     * <p>F184: Primitives can specify a phase offset relative to a linked
     * primitive. This offset is added to the animation time to create
     * synchronized or offset animations between linked primitives.
     * 
     * @param primitive The primitive to check
     * @return Phase offset in ticks (0 if no link or no phase offset)
     */
    private static float getLinkPhaseOffset(Primitive primitive) {
        PrimitiveLink link = primitive.link();
        if (link == null) {
            return 0;
        }
        return LinkResolver.resolvePhaseOffset(link);
    }
}

