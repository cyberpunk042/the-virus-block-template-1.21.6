package net.cyberpunk042.client.visual._legacy.render;

import net.cyberpunk042.client.field._legacy.render.PrimitiveRenderer_old;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual._legacy.mesh.SphereTessellator_old;
import net.cyberpunk042.client.visual._legacy.mesh.sphere.TypeASphere_old;
import net.cyberpunk042.client.visual._legacy.mesh.sphere.TypeESphere_old;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.field._legacy.primitive.SpherePrimitive_old;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.appearance.FillMode;
import net.cyberpunk042.visual.appearance.PatternConfig;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.shape.SphereShape;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * Renders sphere primitives using SphereTessellator_old.
 * 
 * <p>Supports all appearance properties:
 * <ul>
 *   <li>FillMode.SOLID - filled sphere</li>
 *   <li>FillMode.WIREFRAME - wireframe sphere</li>
 *   <li>glow - additive glow effect</li>
 * </ul>
 */
public final class SphereRenderer_old implements PrimitiveRenderer_old {
    
    private static final String TYPE = "sphere";
    
    public static final SphereRenderer_old INSTANCE = new SphereRenderer_old();
    
    private SphereRenderer_old() {}
    
    @Override
    public String type() {
        return "sphere";
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
        
        if (!(primitive instanceof SpherePrimitive_old sphere)) {
            Logging.RENDER.topic("sphere").warn("Expected SpherePrimitive_old, got: {}", 
                primitive.getClass().getSimpleName());
            return;
        }
        
        SphereShape shape = (SphereShape) sphere.shape();
        Appearance appearance = sphere.appearance();
        
        // Resolve color
        int color = colorResolver.resolve(appearance.color());
        float alpha = appearance.alpha().at(time * 0.1f); // Animate alpha if pulsing
        
        // Apply overrides
        if (overrides != null && overrides.alpha() != null) {
            alpha = overrides.alpha();
        }
        
        // DIAGNOSTIC: Log sphere rendering details
        if ((int) time % 60 == 0) {
            Logging.RENDER.topic("sphere").info(
                "DIAG: SphereRenderer_old.render() called: radius={:.2f}, color=0x{}, alpha={:.2f}, algo={}, override={}",
                shape.radius(), String.format("%08X", color), alpha, shape.algorithm(),
                overrides != null && overrides.vertexPattern() != null ? overrides.vertexPattern().id() : "none");
        }
        
        // Apply alpha to color (ARGB format)
        int a = (int) (alpha * 255) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int argb = (a << 24) | (r << 16) | (g << 8) | b;
        
        matrices.push();
        
        // Dispatch based on algorithm
        if (shape.isTypeA()) {
            // Use TypeA sphere renderer (overlapping cubes - accurate)
            renderTypeA(matrices, consumer, shape, r / 255f, g / 255f, b / 255f, alpha, light);
        } else if (shape.isTypeE()) {
            // Use TypeE sphere renderer (rotated rectangles - efficient)
            renderTypeE(matrices, consumer, shape, r / 255f, g / 255f, b / 255f, alpha, light);
        } else {
            // Default: LAT_LON tessellation (supports pattern overrides)
            renderLatLon(matrices, consumer, shape, appearance, argb, light, overrides);
        }
        
        matrices.pop();
        
        Logging.RENDER.topic("sphere").trace(
            "Rendered sphere: r={}, algo={}, fill={}", 
            shape.radius(), shape.algorithm(), appearance.fill());
    }
    
    /**
     * Renders using LAT_LON tessellation (default).
     * Supports pattern overrides from RenderOverrides_old.
     */
    private void renderLatLon(
            MatrixStack matrices, VertexConsumer consumer,
            SphereShape shape, Appearance appearance, int argb, int light,
            RenderOverrides_old overrides) {
        
        boolean wireframe = appearance.fill() == FillMode.WIREFRAME;
        
        // Determine which pattern to use (override takes precedence)
        PatternConfig patternToUse = appearance.pattern();
        if (overrides != null && overrides.vertexPattern() != null) {
            // Create a new PatternConfig with the overridden vertex pattern
            patternToUse = PatternConfig.withVertexPattern(overrides.vertexPattern());
            Logging.RENDER.topic("sphere").debug("Using pattern override: {}", 
                overrides.vertexPattern().id());
        }
        
        // Build tessellator with all appearance properties including pattern
        SphereTessellator_old tessellator = SphereTessellator_old.create()
            .radius(shape.radius())
            .latSteps(shape.latSteps())
            .lonSteps(shape.lonSteps())
            .latRange(shape.latStart(), shape.latEnd())
            .lonRange(shape.lonStart(), shape.lonEnd())
            .wireframe(wireframe)
            .pattern(patternToUse)
            .build();
        
        Mesh mesh = tessellator.tessellate(shape.latSteps());
        
        if (mesh == null || mesh.isEmpty()) {
            return;
        }
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        if (wireframe) {
            VertexEmitter.emitWireframe(consumer, mesh, matrix, argb, appearance.wireThickness(), light);
        } else {
            VertexEmitter.emitMesh(consumer, mesh, matrix, argb, light);
        }
    }
    
    /**
     * Renders using TYPE_A algorithm (overlapping cubes).
     */
    private void renderTypeA(
            MatrixStack matrices, VertexConsumer consumer,
            SphereShape shape, float r, float g, float b, float a, int light) {
        
        TypeASphere_old renderer = TypeASphere_old.builder()
            .radius(shape.radius())
            .verticalLayers(Math.max(4, shape.latSteps() / 2))
            .horizontalDetail(Math.max(2, shape.latSteps() / 4))
            .color(r, g, b, a)
            .build();
        
        renderer.render(matrices, consumer, light, 0);
    }
    
    /**
     * Renders using TYPE_E algorithm (rotated rectangles).
     */
    private void renderTypeE(
            MatrixStack matrices, VertexConsumer consumer,
            SphereShape shape, float r, float g, float b, float a, int light) {
        
        TypeESphere_old renderer = TypeESphere_old.builder()
            .radius(shape.radius())
            .verticalLayers(Math.max(4, shape.latSteps() / 2))
            .color(r, g, b, a)
            .build();
        
        renderer.render(matrices, consumer, light, 0);
    }
    
    /**
     * Renders a sphere with glow effect.
     * Called when appearance.glow() > 0.
     */
    public void renderWithGlow(
            SpherePrimitive_old sphere,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            int light,
            float time,
            ColorResolver colorResolver) {
        
        SphereShape shape = (SphereShape) sphere.shape();
        Appearance appearance = sphere.appearance();
        
        int color = colorResolver.resolve(appearance.color());
        float alpha = appearance.alpha().at(time * 0.1f);
        int a = (int) (alpha * 255) & 0xFF;
        int argb = (a << 24) | (color & 0x00FFFFFF);
        
        // Generate mesh with pattern support
        SphereTessellator_old tessellator = SphereTessellator_old.create()
            .radius(shape.radius())
            .latSteps(shape.latSteps())
            .lonSteps(shape.lonSteps())
            .latRange(shape.latStart(), shape.latEnd())
            .lonRange(shape.lonStart(), shape.lonEnd())
            .wireframe(false)
            .pattern(appearance.pattern())
            .build();
        Mesh mesh = tessellator.tessellate(shape.latSteps());
        
        if (mesh == null || mesh.isEmpty()) {
            return;
        }
        
        // Render with glow
        GlowRenderer.render(matrices, consumers, mesh, argb, appearance.glow(), light);
    }
}
