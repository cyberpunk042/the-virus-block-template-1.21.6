package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.animation.AnimationApplier;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.client.visual.render.VertexEmitter;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.Animation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.fill.FillMode;
import net.cyberpunk042.field.primitive.Primitive;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Base implementation for primitive renderers.
 * 
 * <p>Provides common functionality:
 * <ul>
 *   <li>Color resolution from appearance</li>
 *   <li>Fill mode handling (solid, wireframe, cage)</li>
 *   <li>Mesh emission via VertexEmitter</li>
 * </ul>
 * 
 * <p>Subclasses only need to implement {@link #tessellate(Primitive)} to
 * create the mesh for their specific shape.
 * 
 * @see PrimitiveRenderer
 * @see VertexEmitter
 */
public abstract class AbstractPrimitiveRenderer implements PrimitiveRenderer {
    
    @Override
    public void render(
            Primitive primitive,
            MatrixStack matrices,
            VertexConsumer consumer,
            int light,
            float time,
            ColorResolver resolver,
            RenderOverrides overrides) {
        
        // === PHASE 1: Tessellate ===
        Mesh mesh = tessellate(primitive);
        if (mesh == null || mesh.isEmpty()) {
            Logging.FIELD.topic("render").trace(
                "Empty mesh for primitive '{}', skipping", primitive.id());
            return;
        }
        
        // === PHASE 2: Resolve Color (includes animation effects) ===
        int color = resolveColor(primitive, resolver, overrides, time);
        
        // === PHASE 2.5: Apply Animated Mask Alpha ===
        // Note: For animated masks, we apply alpha modulation at render time
        // since tessellation is cached. This affects overall primitive alpha.
        if (primitive.visibility() != null && primitive.visibility().animate()) {
            // Apply average mask alpha based on animation time
            // This is an approximation - true per-vertex mask would require re-tessellation
            float maskAlpha = 0.5f + 0.5f * (float)Math.sin(time * primitive.visibility().animSpeed() * 0.1f);
            color = net.cyberpunk042.visual.color.ColorMath.multiplyAlpha(color, maskAlpha);
        }
        
        // === PHASE 3: Get Wave Config for Vertex Displacement ===
        Animation animation = primitive.animation();
        net.cyberpunk042.visual.animation.WaveConfig waveConfig = 
            (animation != null && animation.hasWave()) ? animation.wave() : null;
        
        // === PHASE 4: Emit Based on Fill Mode ===
        FillConfig fill = primitive.fill();
        FillMode mode = fill != null ? fill.mode() : FillMode.SOLID;
        
        switch (mode) {
            case SOLID -> emitSolid(matrices, consumer, mesh, color, light, waveConfig, time);
            case WIREFRAME -> emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
            case CAGE -> emitCage(matrices, consumer, mesh, color, light, fill, primitive, waveConfig, time);
            case POINTS -> emitPoints(matrices, consumer, mesh, color, light, waveConfig, time);
        }
        
        Logging.FIELD.topic("render").trace(
            "Rendered {} primitive '{}': vertices={}, mode={}",
            shapeType(), primitive.id(), mesh.vertexCount(), mode);
    }
    
    // =========================================================================
    // Abstract Methods - Subclasses Implement
    // =========================================================================
    
    /**
     * Tessellates the primitive's shape into a mesh.
     * 
     * @param primitive The primitive containing the shape
     * @return The tessellated mesh, or null if invalid
     */
    protected abstract Mesh tessellate(Primitive primitive);
    
    // =========================================================================
    // Color Resolution
    // =========================================================================
    
    /**
     * Resolves the render color from appearance, animation, and overrides.
     * 
     * <p>Color resolution priority:
     * <ol>
     *   <li>Override color (from RenderOverrides)</li>
     *   <li>Animation color cycle (if active)</li>
     *   <li>Appearance color reference (resolved via ColorResolver)</li>
     *   <li>Direct hex color in appearance</li>
     *   <li>Default white</li>
     * </ol>
     */
    protected int resolveColor(Primitive primitive, ColorResolver resolver, RenderOverrides overrides, float time) {
        // Check for color override first
        if (overrides != null && overrides.colorOverride() != null) {
            return overrides.colorOverride();
        }
        
        int baseColor;
        
        // Check for animation color cycle - uses ColorHelper.lerp for smooth blending
        Animation animation = primitive.animation();
        if (animation != null && animation.hasColorCycle()) {
            baseColor = AnimationApplier.getColorCycle(animation.colorCycle(), time);
        } else {
            // Normal color resolution from appearance
            Appearance appearance = primitive.appearance();
            if (appearance == null) {
                return 0xFFFFFFFF; // Default white
            }
            
            // Resolve color reference (e.g., "primary" → actual color)
            String colorRef = appearance.color();
            
            if (colorRef != null && resolver != null) {
                baseColor = resolver.resolve(colorRef);
            } else if (colorRef != null && colorRef.startsWith("#")) {
                // Direct hex color
                baseColor = parseHexColor(colorRef);
            } else if (colorRef != null && colorRef.startsWith("0x")) {
                baseColor = Integer.parseUnsignedInt(colorRef.substring(2), 16);
            } else {
                baseColor = 0xFFFFFFFF;
            }
            
            // Apply appearance color modifiers (saturation, brightness, hueShift)
            baseColor = applyAppearanceModifiers(baseColor, appearance, resolver);
        }
        
        // Apply alpha from appearance (AlphaRange - use max value)
        Appearance appearance = primitive.appearance();
        float alpha = (appearance != null && appearance.alpha() != null) 
            ? appearance.alpha().max() : 1.0f;
        
        // Apply alpha pulse animation (from AlphaPulseConfig)
        if (animation != null && animation.hasAlphaPulse()) {
            alpha *= AnimationApplier.getAlphaPulse(animation.alphaPulse(), time);
        }
        
        // Apply pulse alpha (when PulseConfig.mode == ALPHA)
        if (animation != null && animation.hasPulse()) {
            alpha *= AnimationApplier.getPulseAlpha(animation.pulse(), time);
        }
        
        if (overrides != null) {
            alpha *= overrides.alphaMultiplier();
        }
        
        // Combine base color with alpha
        int a = (int) (alpha * 255) & 0xFF;
        return (baseColor & 0x00FFFFFF) | (a << 24);
    }
    
    /**
     * @deprecated Use {@link #resolveColor(Primitive, ColorResolver, RenderOverrides, float)} instead
     */
    @Deprecated
    protected int resolveColor(Primitive primitive, ColorResolver resolver, RenderOverrides overrides) {
        return resolveColor(primitive, resolver, overrides, 0f);
    }
    
    /**
     * Parses a hex color string (#RRGGBB or #AARRGGBB).
     */
    protected int parseHexColor(String hex) {
        if (hex == null || hex.isEmpty()) return 0xFFFFFFFF;
        
        String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            if (cleaned.length() == 6) {
                // RGB only, add full alpha
                return 0xFF000000 | Integer.parseUnsignedInt(cleaned, 16);
            } else if (cleaned.length() == 8) {
                // ARGB
                return Integer.parseUnsignedInt(cleaned, 16);
            }
        } catch (NumberFormatException e) {
            Logging.FIELD.topic("render").warn("Invalid hex color: {}", hex);
        }
        return 0xFFFFFFFF;
    }
    
    /**
     * Applies appearance color modifiers (saturation, brightness, hueShift, secondaryColor).
     * 
     * @param baseColor Base ARGB color
     * @param appearance Appearance config
     * @param resolver Color resolver for secondary color references
     * @return Modified ARGB color
     */
    protected int applyAppearanceModifiers(int baseColor, Appearance appearance, ColorResolver resolver) {
        int color = baseColor;
        
        // Apply saturation (1.0 = no change, 0.0 = grayscale, 2.0 = hyper-saturated)
        if (appearance.saturation() != 1.0f) {
            color = net.cyberpunk042.visual.color.ColorMath.multiplySaturation(color, appearance.saturation());
        }
        
        // Apply brightness (1.0 = no change)
        if (appearance.brightness() != 1.0f) {
            color = net.cyberpunk042.visual.color.ColorMath.multiplyBrightness(color, appearance.brightness());
        }
        
        // Apply hue shift (degrees)
        if (appearance.hueShift() != 0.0f) {
            color = net.cyberpunk042.visual.color.ColorMath.shiftHue(color, appearance.hueShift());
        }
        
        // Apply secondary color blending
        if (appearance.hasSecondaryColor()) {
            String secondaryRef = appearance.secondaryColor();
            int secondaryColor = 0xFFFFFFFF;
            
            if (resolver != null) {
                secondaryColor = resolver.resolve(secondaryRef);
            } else if (secondaryRef != null && secondaryRef.startsWith("#")) {
                secondaryColor = parseHexColor(secondaryRef);
            }
            
            color = net.cyberpunk042.visual.color.ColorMath.blend(color, secondaryColor, appearance.colorBlend());
            
            Logging.FIELD.topic("render").trace(
                "Applied secondary color blend: {} @ {:.2f}", secondaryRef, appearance.colorBlend());
        }
        
        return color;
    }
    
    // =========================================================================
    // Emission Methods
    // =========================================================================
    
    /**
     * Emits mesh as solid triangles/quads with optional wave animation.
     * 
     * @param matrices Transform stack
     * @param consumer Vertex output
     * @param mesh Source mesh
     * @param color ARGB color
     * @param light Light level
     * @param waveConfig Wave animation config (null for no wave)
     * @param time Current time for wave animation
     */
    protected void emitSolid(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light,
            net.cyberpunk042.visual.animation.WaveConfig waveConfig,
            float time) {
        
        VertexEmitter emitter = new VertexEmitter(matrices, consumer);
        emitter.color(color).light(light);
        
        // Apply wave animation if configured
        if (waveConfig != null && waveConfig.isActive()) {
            emitter.wave(waveConfig, time);
        }
        
        emitter.emit(mesh);
    }
    
    /**
     * Emits mesh as wireframe lines with optional wave animation.
     */
    protected void emitWireframe(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light,
            FillConfig fill,
            net.cyberpunk042.visual.animation.WaveConfig waveConfig,
            float time) {
        
        float thickness = fill != null ? fill.wireThickness() : 1.0f;
        
        // For wireframe with wave, we need instance-based emitter
        if (waveConfig != null && waveConfig.isActive()) {
            VertexEmitter emitter = new VertexEmitter(matrices, consumer);
            emitter.color(color).light(light).wave(waveConfig, time);
            emitter.emitWireframe(mesh, thickness);
        } else {
            VertexEmitter.emitWireframe(
                matrices.peek(), consumer, mesh, color, thickness, light);
        }
    }
    
    /**
     * Emits mesh as cage (lat/lon grid for spheres, edges for polyhedra).
     * Default implementation falls back to wireframe.
     */
    protected void emitCage(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light,
            FillConfig fill,
            Primitive primitive,
            net.cyberpunk042.visual.animation.WaveConfig waveConfig,
            float time) {
        
        // Default: same as wireframe
        // Subclasses can override for shape-specific cage rendering
        emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
    }
    
    /**
     * Emits mesh vertices as points using tiny billboarded quads with optional wave animation.
     * 
     * <p>Minecraft/OpenGL doesn't support GL_POINTS natively for our use case,
     * so we fake it with tiny camera-facing quads (2 triangles each).
     * 
     * @param matrices Transform stack
     * @param consumer Vertex output
     * @param mesh Source mesh (only vertices used)
     * @param color ARGB color
     * @param light Light value
     * @param waveConfig Wave animation config (null for no wave)
     * @param time Current time for wave animation
     */
    protected void emitPoints(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light,
            net.cyberpunk042.visual.animation.WaveConfig waveConfig,
            float time) {
        emitPoints(matrices, consumer, mesh, color, light, 0.02f, waveConfig, time);
    }
    
    /**
     * Emits mesh vertices as points with configurable size and optional wave animation.
     */
    protected void emitPoints(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light,
            float pointSize,
            net.cyberpunk042.visual.animation.WaveConfig waveConfig,
            float time) {
        
        if (mesh.vertices().isEmpty()) return;
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix();
        
        // Extract ARGB components
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        float half = pointSize / 2f;
        boolean hasWave = waveConfig != null && waveConfig.isActive();
        
        // For each vertex, emit a tiny billboarded quad
        for (Vertex v : mesh.vertices()) {
            float x = v.x();
            float y = v.y();
            float z = v.z();
            
            // Apply wave displacement if configured
            if (hasWave) {
                float[] displaced = AnimationApplier.applyWaveToVertex(waveConfig, x, y, z, time);
                x = displaced[0];
                y = displaced[1];
                z = displaced[2];
            }
            
            // Create a camera-facing quad (billboard)
            // Using XY plane offset - will be approximately billboarded for most views
            // For true billboarding, would need camera direction
            
            // Quad corners (tiny square centered on vertex)
            //  2---3
            //  |   |
            //  0---1
            
            // Triangle 1: 0-1-2
            emitBillboardVertex(consumer, matrix, normalMatrix, x - half, y - half, z, 0, 0, 1, r, g, b, a, light);
            emitBillboardVertex(consumer, matrix, normalMatrix, x + half, y - half, z, 0, 0, 1, r, g, b, a, light);
            emitBillboardVertex(consumer, matrix, normalMatrix, x - half, y + half, z, 0, 0, 1, r, g, b, a, light);
            
            // Triangle 2: 1-3-2
            emitBillboardVertex(consumer, matrix, normalMatrix, x + half, y - half, z, 0, 0, 1, r, g, b, a, light);
            emitBillboardVertex(consumer, matrix, normalMatrix, x + half, y + half, z, 0, 0, 1, r, g, b, a, light);
            emitBillboardVertex(consumer, matrix, normalMatrix, x - half, y + half, z, 0, 0, 1, r, g, b, a, light);
        }
        
        Logging.FIELD.topic("render").trace("Emitted {} points as billboarded quads", mesh.vertices().size());
    }
    
    /**
     * Helper method to emit a single vertex for billboarded quads.
     * 
     * @param consumer Vertex consumer
     * @param matrix Position transformation matrix
     * @param normalMatrix Normal transformation matrix
     * @param x X position
     * @param y Y position
     * @param z Z position
     * @param nx Normal X
     * @param ny Normal Y
     * @param nz Normal Z
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     * @param a Alpha component (0-255)
     * @param light Light level
     */
    private void emitBillboardVertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normalMatrix,
            float x, float y, float z,
            float nx, float ny, float nz,
            int r, int g, int b, int a,
            int light) {
        
        // Transform position
        org.joml.Vector4f pos = new org.joml.Vector4f(x, y, z, 1.0f);
        pos.mul(matrix);
        
        // Transform normal
        org.joml.Vector3f normal = new org.joml.Vector3f(nx, ny, nz);
        normal.mul(normalMatrix).normalize();
        
        // Emit vertex
        consumer.vertex(pos.x(), pos.y(), pos.z())
            .color(r, g, b, a)
            .texture(0, 0)  // No texture for points
            .overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(normal.x(), normal.y(), normal.z());
    }
}

