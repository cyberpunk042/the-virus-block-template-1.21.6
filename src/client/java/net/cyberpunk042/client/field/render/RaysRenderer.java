package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.animation.AnimationApplier;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.RaysTessellator;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.WaveConfig;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.appearance.ColorContext;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.shape.RaysShape;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Renders rays primitives (collections of line segments).
 * 
 * <p>Rays are pure line geometry that support various arrangement patterns:
 * RADIAL, SPHERICAL, PARALLEL, CONVERGING, DIVERGING.</p>
 * 
 * <p>Unlike solid shapes, rays always render as lines regardless of fill mode.
 * SOLID, WIREFRAME, and CAGE all render the same line geometry.</p>
 * 
 * <h3>Supported Features</h3>
 * <ul>
 *   <li>Per-vertex coloring (MESH_GRADIENT, MESH_RAINBOW, RANDOM)</li>
 *   <li>Wave animation</li>
 *   <li>All arrangement modes</li>
 *   <li>Segmented/dashed lines</li>
 *   <li>fadeStart/fadeEnd - per-ray alpha gradient</li>
 * </ul>
 * 
 * @see RaysShape
 * @see RaysTessellator
 */
public final class RaysRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "rays";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof RaysShape shape)) {
            return null;
        }
        
        Logging.RENDER.topic("tessellate").debug(
            "[RAYS] Tessellating: count={}, arrangement={}, layers={}",
            shape.count(), shape.arrangement(), shape.layers());
        
        return RaysTessellator.tessellate(shape);
    }
    
    /**
     * Override render to handle rays specially with fadeStart/fadeEnd support.
     */
    @Override
    public void render(
            Primitive primitive,
            MatrixStack matrices,
            VertexConsumer consumer,
            int light,
            float time,
            ColorResolver resolver,
            RenderOverrides overrides) {
        
        if (!(primitive.shape() instanceof RaysShape shape)) {
            return;
        }
        
        Logging.FIELD.topic("render").trace("[RAYS] Rendering: count={}, arrangement={}", 
            shape.count(), shape.arrangement());
        
        // === Tessellate ===
        WaveConfig wave = primitive.animation() != null ? primitive.animation().wave() : null;
        Mesh mesh = tessellate(primitive, wave, time);
        if (mesh == null || mesh.isEmpty()) {
            return;
        }
        
        // === Resolve Color ===
        int color = resolveColor(primitive, resolver, overrides, time);
        
        // Apply alpha from appearance
        Appearance app = primitive.appearance();
        float baseAlpha = 1.0f;
        if (app != null && app.alpha() != null) {
            baseAlpha = app.alpha().max();
        }
        
        // === Get Wave Config ===
        WaveConfig waveConfig = primitive.animation() != null && primitive.animation().hasWave() 
            ? primitive.animation().wave() : null;
        
        // === Create ColorContext for per-vertex coloring ===
        ColorContext colorCtx = null;
        if (app != null && app.isPerVertex()) {
            int primaryColor = color;
            int secondaryColor = color;
            
            String primaryRef = app.color();
            if (primaryRef != null && resolver != null) {
                primaryColor = resolver.resolve(primaryRef);
            }
            
            String secondaryRef = app.secondaryColor();
            if (secondaryRef != null && resolver != null) {
                secondaryColor = resolver.resolve(secondaryRef);
            } else if (secondaryRef != null && secondaryRef.startsWith("#")) {
                try {
                    secondaryColor = 0xFF000000 | Integer.parseInt(secondaryRef.substring(1), 16);
                } catch (NumberFormatException ignored) {}
            } else {
                secondaryColor = primaryColor;
            }
            
            colorCtx = ColorContext.from(app, primaryColor, secondaryColor, time, 
                shape.outerRadius(), shape.outerRadius());
        }
        
        // === Emit Lines with Fade + Flow Support ===
        float fadeStart = shape.fadeStart();
        float fadeEnd = shape.fadeEnd();
        boolean hasFade = fadeStart != 1.0f || fadeEnd != 1.0f;
        
        // Flow animation
        float flowSpeed = shape.flowSpeed();
        float flowWidth = Math.max(0.1f, shape.flowWidth()); // Minimum window size
        boolean hasFlow = shape.hasFlow();
        
        emitRayLines(matrices, consumer, mesh, color, baseAlpha, light, 
                     colorCtx, waveConfig, time, fadeStart, fadeEnd, hasFade,
                     flowSpeed, flowWidth, hasFlow);
        
        Logging.FIELD.topic("render").trace("[RAYS] DONE: {} vertices, fade={}, flow={}", 
            mesh.vertexCount(), hasFade, hasFlow);
    }
    
    /**
     * Emits ray line vertices with fade and flow support.
     * 
     * <p>Each vertex has a 't' value stored in UV.u (0 at ray start, 1 at ray end).
     * Alpha is interpolated: alpha = lerp(fadeStart, fadeEnd, t) * baseAlpha
     * 
     * <p>Flow animation creates a moving window of visibility:
     * <ul>
     *   <li>flowSpeed > 0: window moves from start to end (outward flow)</li>
     *   <li>flowSpeed < 0: window moves from end to start (inward flow)</li>
     *   <li>flowWidth: size of the visible window (0.1 = narrow, 1.0 = full)</li>
     * </ul>
     */
    private void emitRayLines(MatrixStack matrices, VertexConsumer consumer,
                               Mesh mesh, int baseColor, float baseAlpha, int light,
                               ColorContext colorCtx, WaveConfig waveConfig, float time,
                               float fadeStart, float fadeEnd, boolean hasFade,
                               float flowSpeed, float flowWidth, boolean hasFlow) {
        
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();
        
        // Calculate flow window position (0-1, wrapping)
        // flowSpeed is in units per second, so multiply by time
        float flowPos = hasFlow ? (time * flowSpeed * 0.1f) % 1.0f : 0f;
        if (flowPos < 0) flowPos += 1.0f; // Handle negative modulo
        
        // For each line segment (pairs of vertices)
        mesh.forEachLine((v0, v1) -> {
            emitRayVertex(consumer, v0, v1, positionMatrix, normalMatrix, 
                          baseColor, baseAlpha, light, colorCtx, waveConfig, time,
                          fadeStart, fadeEnd, hasFade,
                          flowPos, flowWidth, hasFlow);
            emitRayVertex(consumer, v1, v0, positionMatrix, normalMatrix,
                          baseColor, baseAlpha, light, colorCtx, waveConfig, time,
                          fadeStart, fadeEnd, hasFade,
                          flowPos, flowWidth, hasFlow);
        });
    }
    
    /**
     * Emits a single ray line vertex with fade applied.
     */
    private void emitRayVertex(VertexConsumer consumer, Vertex v, Vertex other,
                                Matrix4f positionMatrix, Matrix3f normalMatrix,
                                int baseColor, float baseAlpha, int light,
                                ColorContext colorCtx, WaveConfig waveConfig, float time,
                                float fadeStart, float fadeEnd, boolean hasFade) {
        float vx = v.x();
        float vy = v.y();
        float vz = v.z();
        
        // Apply wave displacement if configured
        if (waveConfig != null && waveConfig.isActive()) {
            float[] displaced = AnimationApplier.applyWaveToVertex(waveConfig, vx, vy, vz, time);
            vx = displaced[0];
            vy = displaced[1];
            vz = displaced[2];
        }
        
        // Transform position
        Vector4f pos = new Vector4f(vx, vy, vz, 1.0f);
        pos.mul(positionMatrix);
        
        // For lines, normal is the direction to other vertex
        Vector3f dir = new Vector3f(other.x() - v.x(), other.y() - v.y(), other.z() - v.z());
        dir.normalize();
        dir.mul(normalMatrix);
        
        // Calculate color
        int vertexColor;
        if (colorCtx != null && colorCtx.isPerVertex()) {
            vertexColor = colorCtx.calculateColor(v.x(), v.y(), v.z(), 0);
        } else {
            vertexColor = baseColor;
        }
        
        // Calculate alpha with fade
        float alpha = baseAlpha;
        if (hasFade) {
            // v.u() contains 't' value (0 at ray start, 1 at ray end)
            float t = v.u();
            float fadeFactor = fadeStart + (fadeEnd - fadeStart) * t;
            alpha *= fadeFactor;
        }
        
        // Apply alpha to color
        int a = (int)(alpha * 255) & 0xFF;
        int r = (vertexColor >> 16) & 0xFF;
        int g = (vertexColor >> 8) & 0xFF;
        int b = vertexColor & 0xFF;
        
        consumer.vertex(pos.x(), pos.y(), pos.z())
            .color(r, g, b, a)
            .normal(dir.x(), dir.y(), dir.z());
    }
    
    /**
     * Rays don't have a separate cage mode - they ARE lines.
     */
    @Override
    protected void emitCage(MatrixStack matrices, VertexConsumer consumer,
                            Mesh mesh, int color, int light, FillConfig fill,
                            Primitive primitive, WaveConfig waveConfig, float time) {
        // Redirects to normal render since rays are always lines
    }
    
    /**
     * For rays, solid and wireframe are the same - it's all lines.
     */
    @Override
    protected void emitWireframe(MatrixStack matrices, VertexConsumer consumer,
                                  Mesh mesh, int color, int light, FillConfig fill,
                                  WaveConfig waveConfig, float time) {
        // Redirects to normal render since rays are always lines
    }
}
