package net.cyberpunk042.client.visual._legacy.render;
import net.cyberpunk042.client.field._legacy.render.PrimitiveRenderer_old;

import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.animation.Axis;
import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.color.ColorTheme;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Renders a single field layer with all its primitives.
 * 
 * <p>Handles:
 * <ul>
 *   <li>Layer-level animations (spin, tilt, pulse)</li>
 *   <li>Color resolution from theme</li>
 *   <li>Delegates primitive rendering to PrimitiveRenderers_old</li>
 * </ul>
 */
public final class LayerRenderer_old {
    
    private LayerRenderer_old() {}
    
    /**
     * Renders a layer with the given context.
     * 
     * @param matrices Matrix stack for transformations
     * @param consumer Vertex consumer to emit to
     * @param layer The layer definition
     * @param theme Color theme for resolution
     * @param scale Base scale multiplier
     * @param time Animation time (world time + tick delta)
     * @param baseAlpha Base alpha multiplier
     */
    public static void render(
            MatrixStack matrices,
            VertexConsumer consumer,
            FieldLayer layer,
            ColorTheme theme,
            float scale,
            float time,
            float baseAlpha) {
        
        // Calculate final alpha with layer alpha
        float alpha = MathHelper.clamp(baseAlpha * layer.alpha(), 0, 1);
        if (alpha <= 0.01f) {
            return;
        }
        
        // Create color resolver with theme
        ColorResolver colorResolver = new ColorResolver(theme);
        int light = 15728880; // Full bright
        
        // Apply layer animations
        matrices.push();
        
        // Spin animation
        if (layer.spin() != 0) {
            float rotation = time * layer.spin() + layer.phaseOffset();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rotation));
        }
        
        // Tilt animation
        if (layer.tilt() != 0) {
            float tiltAngle = time * layer.tilt() * 0.5f;
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(tiltAngle));
        }
        
        // Pulse animation (scale modulation)
        float pulseScale = 1.0f;
        if (layer.pulse() > 0) {
            float pulsePhase = time * layer.pulse() + layer.phaseOffset();
            pulseScale = 1.0f + 0.1f * MathHelper.sin(pulsePhase);
        }
        
        matrices.scale(scale * pulseScale, scale * pulseScale, scale * pulseScale);
        
        // Render each primitive using the appropriate renderer
        int primCount = layer.primitives().size();
        for (Primitive primitive : layer.primitives()) {
            renderPrimitive(matrices, consumer, primitive, colorResolver, light, time);
        }
        
        Logging.RENDER.topic("layer").trace(
            "Layer '{}': {} primitives, spin={:.2f}, pulse={:.2f}, alpha={:.2f}",
            layer.id(), primCount, layer.spin(), layer.pulse(), alpha);
        
        matrices.pop();
    }
    
    /**
     * Renders a single primitive using the PrimitiveRenderers_old system.
     * Applies primitive-level animation (spin, pulse) before rendering.
     */
    private static void renderPrimitive(
            MatrixStack matrices,
            VertexConsumer consumer,
            Primitive primitive,
            ColorResolver colorResolver,
            int light,
            float time) {
        
        String type = primitive.type();
        PrimitiveRenderer_old renderer = PrimitiveRenderers_old.get(type);
        
        if (renderer != null) {
            matrices.push();
            
            // Apply primitive-level animation
            applyPrimitiveAnimation(matrices, primitive.animation(), time);
            
            renderer.render(primitive, matrices, consumer, light, time, colorResolver, null);
            
            matrices.pop();
        } else {
            Logging.RENDER.topic("layer").trace(
                "No renderer for primitive type: {}", type);
        }
    }
    
    // =========================================================================
    // Render with Overrides
    // =========================================================================
    
    /**
     * Renders a layer with runtime overrides for debugging.
     * 
     * @param matrices Matrix stack
     * @param consumer Vertex consumer
     * @param layer Layer definition
     * @param theme Color theme
     * @param scale Scale
     * @param time Animation time
     * @param baseAlpha Base alpha
     * @param overrides Runtime overrides for tessellation, algorithm, etc.
     */
    public static void renderWithOverrides(
            MatrixStack matrices,
            VertexConsumer consumer,
            FieldLayer layer,
            ColorTheme theme,
            float scale,
            float time,
            float baseAlpha,
            RenderOverrides_old overrides) {
        
        // Calculate final alpha
        float alpha = MathHelper.clamp(baseAlpha * layer.alpha(), 0, 1);
        if (alpha <= 0.01f) {
            return;
        }
        
        // Create color resolver, potentially with color override
        ColorResolver colorResolver;
        if (overrides.hasColorOverride()) {
            // Create resolver with overridden color
            colorResolver = new ColorResolver(theme, overrides.colorOverride());
        } else {
            colorResolver = new ColorResolver(theme);
        }
        
        int light = 15728880; // Full bright
        
        // Apply layer animations
        matrices.push();
        
        // Spin animation
        if (layer.spin() != 0) {
            float rotation = time * layer.spin() + layer.phaseOffset();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rotation));
        }
        
        // Tilt animation
        if (layer.tilt() != 0) {
            float tiltAngle = time * layer.tilt() * 0.5f;
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(tiltAngle));
        }
        
        // Pulse animation (scale modulation)
        float pulseScale = 1.0f;
        if (layer.pulse() > 0) {
            float pulsePhase = time * layer.pulse() + layer.phaseOffset();
            pulseScale = 1.0f + 0.1f * MathHelper.sin(pulsePhase);
        }
        
        matrices.scale(scale * pulseScale, scale * pulseScale, scale * pulseScale);
        
        // Render each primitive with overrides
        for (Primitive primitive : layer.primitives()) {
            renderPrimitiveWithOverrides(matrices, consumer, primitive, colorResolver, light, time, overrides);
        }
        
        Logging.RENDER.topic("layer").trace(
            "Layer '{}' (with overrides): {} primitives, lat={}, lon={}, algo={}",
            layer.id(), layer.primitives().size(), 
            overrides.latSteps(), overrides.lonSteps(), overrides.algorithm());
        
        matrices.pop();
    }
    
    /**
     * Renders a primitive with overrides.
     * Applies primitive-level animation (spin, pulse) before rendering.
     */
    private static void renderPrimitiveWithOverrides(
            MatrixStack matrices,
            VertexConsumer consumer,
            Primitive primitive,
            ColorResolver colorResolver,
            int light,
            float time,
            RenderOverrides_old overrides) {
        
        String type = primitive.type();
        PrimitiveRenderer_old renderer = PrimitiveRenderers_old.get(type);
        
        if (renderer != null) {
            matrices.push();
            
            // Apply primitive-level animation
            applyPrimitiveAnimation(matrices, primitive.animation(), time);
            
            // Pass overrides to renderer
            renderer.render(primitive, matrices, consumer, light, time, colorResolver, overrides);
            
            matrices.pop();
        } else {
            Logging.RENDER.topic("layer").trace(
                "No renderer for primitive type: {}", type);
        }
    }
    
    // =========================================================================
    // Primitive Animation
    // =========================================================================
    
    /**
     * Applies primitive-level animation transforms.
     * 
     * <p>Handles:
     * <ul>
     *   <li><b>Spin</b>: Rotation around specified axis</li>
     *   <li><b>Pulse</b>: Scale modulation over time</li>
     * </ul>
     * 
     * @param matrices Matrix stack to transform
     * @param animation The primitive's animation config
     * @param time Current animation time
     */
    private static void applyPrimitiveAnimation(MatrixStack matrices, Animation animation, float time) {
        if (animation == null || !animation.isAnimated()) {
            return;
        }
        
        // Spin animation - rotate around specified axis
        if (animation.hasSpinAnimation()) {
            float rotation = animation.getRotation(time);
            
            RotationAxis axis = switch (animation.spinAxis()) {
                case X -> RotationAxis.POSITIVE_X;
                case Z -> RotationAxis.POSITIVE_Z;
                default -> RotationAxis.POSITIVE_Y; // Y is default
            };
            
            matrices.multiply(axis.rotation(rotation));
        }
        
        // Pulse animation - scale modulation
        if (animation.hasScalePulse()) {
            float pulseScale = animation.getScale(time);
            matrices.scale(pulseScale, pulseScale, pulseScale);
        }
    }
    
    /**
     * Interface for renderers that support runtime overrides.
     */
    public interface OverridableRenderer {
        void renderWithOverrides(
            Primitive primitive,
            MatrixStack matrices,
            VertexConsumer consumer,
            int light,
            float time,
            ColorResolver colorResolver,
            RenderOverrides_old overrides
        );
    }
}
