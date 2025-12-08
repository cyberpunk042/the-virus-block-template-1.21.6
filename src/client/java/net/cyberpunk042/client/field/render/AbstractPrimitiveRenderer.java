package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.client.visual.render.VertexEmitter;
import net.cyberpunk042.log.Logging;
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
        
        // === PHASE 2: Resolve Color ===
        int color = resolveColor(primitive, resolver, overrides);
        
        // === PHASE 3: Emit Based on Fill Mode ===
        FillConfig fill = primitive.fill();
        FillMode mode = fill != null ? fill.mode() : FillMode.SOLID;
        
        switch (mode) {
            case SOLID -> emitSolid(matrices, consumer, mesh, color, light);
            case WIREFRAME -> emitWireframe(matrices, consumer, mesh, color, light, fill);
            case CAGE -> emitCage(matrices, consumer, mesh, color, light, fill, primitive);
            case POINTS -> emitPoints(matrices, consumer, mesh, color, light);
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
     * Resolves the render color from appearance and overrides.
     */
    protected int resolveColor(Primitive primitive, ColorResolver resolver, RenderOverrides overrides) {
        // Check for color override
        if (overrides != null && overrides.colorOverride() != null) {
            return overrides.colorOverride();
        }
        
        Appearance appearance = primitive.appearance();
        if (appearance == null) {
            return 0xFFFFFFFF; // Default white
        }
        
        // Resolve color reference (e.g., "primary" → actual color)
        String colorRef = appearance.color();
        int baseColor;
        
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
        
        // Apply alpha from appearance (AlphaRange - use max value)
        float alpha = appearance.alpha() != null ? appearance.alpha().max() : 1.0f;
        if (overrides != null) {
            alpha *= overrides.alphaMultiplier();
        }
        
        // Combine base color with alpha
        int a = (int) (alpha * 255) & 0xFF;
        return (baseColor & 0x00FFFFFF) | (a << 24);
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
    
    // =========================================================================
    // Emission Methods
    // =========================================================================
    
    /**
     * Emits mesh as solid triangles/quads.
     */
    protected void emitSolid(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light) {
        
        VertexEmitter emitter = new VertexEmitter(matrices, consumer);
        emitter.color(color).light(light);
        emitter.emit(mesh);
    }
    
    /**
     * Emits mesh as wireframe lines.
     */
    protected void emitWireframe(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light,
            FillConfig fill) {
        
        float thickness = fill != null ? fill.wireThickness() : 1.0f;
        VertexEmitter.emitWireframe(
            matrices.peek(), consumer, mesh, color, thickness, light);
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
            Primitive primitive) {
        
        // Default: same as wireframe
        // Subclasses can override for shape-specific cage rendering
        emitWireframe(matrices, consumer, mesh, color, light, fill);
    }
    
    /**
     * Emits mesh vertices as points using tiny billboarded quads.
     * 
     * <p>Minecraft/OpenGL doesn't support GL_POINTS natively for our use case,
     * so we fake it with tiny camera-facing quads (2 triangles each).
     * 
     * @param matrices Transform stack
     * @param consumer Vertex output
     * @param mesh Source mesh (only vertices used)
     * @param color ARGB color
     * @param light Light value
     * @param pointSize Size of each point quad (default 0.02)
     */
    protected void emitPoints(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light) {
        emitPoints(matrices, consumer, mesh, color, light, 0.02f);
    }
    
    /**
     * Emits mesh vertices as points with configurable size.
     */
    protected void emitPoints(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light,
            float pointSize) {
        
        if (mesh.vertices().isEmpty()) return;
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix();
        
        // Extract ARGB components
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        float half = pointSize / 2f;
        
        // For each vertex, emit a tiny billboarded quad
        for (Vertex v : mesh.vertices()) {
            float x = v.x();
            float y = v.y();
            float z = v.z();
            
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

