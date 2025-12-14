package net.cyberpunk042.client.visual.render;

import net.cyberpunk042.client.visual.animation.AnimationApplier;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.PrimitiveType;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.visual.animation.WaveConfig;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Emits mesh vertices to Minecraft's VertexConsumer.
 * Handles transformation, color, and lighting.
 * 
 * <p>Usage:
 * <pre>
 * VertexEmitter emitter = new VertexEmitter(matrices, consumer);
 * emitter.color(0x80FF0000);  // Semi-transparent red
 * emitter.emit(sphereMesh);
 * </pre>
 */
public final class VertexEmitter {
    
    private final MatrixStack.Entry entry;
    private final VertexConsumer consumer;
    private final Matrix4f positionMatrix;
    private final Matrix3f normalMatrix;
    
    private int color = 0xFFFFFFFF;
    private int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
    private int overlay = OverlayTexture.DEFAULT_UV;
    
    // Wave animation support
    private WaveConfig waveConfig = null;
    private float waveTime = 0f;
    
    /**
     * Creates an emitter with the current matrix stack entry and consumer.
     */
    public VertexEmitter(MatrixStack.Entry entry, VertexConsumer consumer) {
        this.entry = entry;
        this.consumer = consumer;
        this.positionMatrix = entry.getPositionMatrix();
        this.normalMatrix = entry.getNormalMatrix();
    }
    
    /**
     * Creates an emitter from a matrix stack (uses peek()).
     */
    public VertexEmitter(MatrixStack matrices, VertexConsumer consumer) {
        this(matrices.peek(), consumer);
    }
    
    // =========================================================================
    // Configuration
    // =========================================================================
    
    /**
     * Sets the color as ARGB int.
     */
    public VertexEmitter color(int argb) {
        this.color = argb;
        return this;
    }
    
    /**
     * Sets the color from RGBA components (0-255).
     */
    public VertexEmitter color(int r, int g, int b, int a) {
        this.color = (a << 24) | (r << 16) | (g << 8) | b;
        return this;
    }
    
    /**
     * Sets the color from RGBA components (0.0-1.0).
     */
    public VertexEmitter color(float r, float g, float b, float a) {
        return color(
            (int) (r * 255) & 0xFF,
            (int) (g * 255) & 0xFF,
            (int) (b * 255) & 0xFF,
            (int) (a * 255) & 0xFF
        );
    }
    
    /**
     * Sets the alpha only (preserves RGB).
     */
    public VertexEmitter alpha(float alpha) {
        int a = (int) (alpha * 255) & 0xFF;
        this.color = (this.color & 0x00FFFFFF) | (a << 24);
        return this;
    }
    
    /**
     * Sets the light level.
     */
    public VertexEmitter light(int light) {
        this.light = light;
        return this;
    }
    
    /**
     * Sets full brightness.
     */
    public VertexEmitter fullBright() {
        this.light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        return this;
    }
    
    /**
     * Sets the overlay.
     */
    public VertexEmitter overlay(int overlay) {
        this.overlay = overlay;
        return this;
    }
    
    /**
     * Sets wave animation configuration.
     * When set, vertices will be displaced by wave animation during emission.
     * 
     * @param wave Wave configuration
     * @param time Current time in ticks
     * @return this emitter for chaining
     */
    public VertexEmitter wave(WaveConfig wave, float time) {
        this.waveConfig = wave;
        this.waveTime = time;
        return this;
    }
    
    /**
     * Clears wave animation (no displacement).
     */
    public VertexEmitter noWave() {
        this.waveConfig = null;
        return this;
    }
    
    // =========================================================================
    // Mesh emission
    // =========================================================================
    
    /**
     * Emits all vertices from a mesh.
     */
    public void emit(Mesh mesh) {
        if (mesh.isEmpty()) {
            return;
        }
        
        switch (mesh.primitiveType()) {
            case TRIANGLES -> emitTriangles(mesh);
            case QUADS -> emitQuads(mesh);
            case LINES -> emitLines(mesh);
            default -> throw new UnsupportedOperationException(
                "Unsupported primitive type: " + mesh.primitiveType());
        }
    }
    
    private void emitTriangles(Mesh mesh) {
        mesh.forEachTriangle((v0, v1, v2) -> {
            emitVertex(v0);
            emitVertex(v1);
            emitVertex(v2);
        });
    }
    
    private void emitQuads(Mesh mesh) {
        // Render quads as 2 triangles (Minecraft expects triangles)
        mesh.forEachQuad((v0, v1, v2, v3) -> {
            // First triangle: v0, v1, v2
            emitVertex(v0);
            emitVertex(v1);
            emitVertex(v2);
            // Second triangle: v0, v2, v3
            emitVertex(v0);
            emitVertex(v2);
            emitVertex(v3);
        });
    }
    
    private void emitLines(Mesh mesh) {
        mesh.forEachLine((v0, v1) -> {
            // Lines in Minecraft require special handling
            // Each line vertex needs position, color, and normal
            emitLineVertex(v0, v1);
            emitLineVertex(v1, v0);
        });
    }
    
    // =========================================================================
    // Single vertex emission
    // =========================================================================
    
    /**
     * Emits a single vertex with full transform and optional wave displacement.
     * 
     * <p>If wave animation is configured via {@link #wave(WaveConfig, float)},
     * the vertex position will be displaced before transformation.</p>
     */
    public void emitVertex(Vertex vertex) {
        float vx = vertex.x();
        float vy = vertex.y();
        float vz = vertex.z();
        
        // Apply wave displacement if configured
        if (waveConfig != null && waveConfig.isActive()) {
            float[] displaced = AnimationApplier.applyWaveToVertex(
                waveConfig, vx, vy, vz, waveTime);
            vx = displaced[0];
            vy = displaced[1];
            vz = displaced[2];
        }
        
        // Transform position
        Vector4f pos = new Vector4f(vx, vy, vz, 1.0f);
        pos.mul(positionMatrix);
        
        // Transform normal
        Vector3f normal = new Vector3f(vertex.nx(), vertex.ny(), vertex.nz());
        normal.mul(normalMatrix);
        
        // Decompose color
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        consumer.vertex(pos.x(), pos.y(), pos.z())
            .color(r, g, b, a)
            .texture(vertex.u(), vertex.v())
            .overlay(overlay)
            .light(light)
            .normal(normal.x(), normal.y(), normal.z());
    }
    
    /**
     * Emits a line vertex (for LINES render layer).
     */
    private void emitLineVertex(Vertex v, Vertex other) {
        // Transform position
        Vector4f pos = new Vector4f(v.x(), v.y(), v.z(), 1.0f);
        pos.mul(positionMatrix);
        
        // For lines, normal is the direction
        Vector3f dir = new Vector3f(
            other.x() - v.x(),
            other.y() - v.y(),
            other.z() - v.z()
        );
        dir.normalize();
        dir.mul(normalMatrix);
        
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        consumer.vertex(pos.x(), pos.y(), pos.z())
            .color(r, g, b, a)
            .normal(dir.x(), dir.y(), dir.z());
    }
    
    // =========================================================================
    // Direct vertex emission (without Mesh)
    // =========================================================================
    
    /**
     * Emits a single vertex at the given position with current settings.
     */
    public void vertex(float x, float y, float z, float nx, float ny, float nz, float u, float v) {
        emitVertex(new Vertex(x, y, z, nx, ny, nz, u, v));
    }
    
    /**
     * Emits a triangle from 3 vertices.
     */
    public void triangle(Vertex v0, Vertex v1, Vertex v2) {
        emitVertex(v0);
        emitVertex(v1);
        emitVertex(v2);
    }
    
    /**
     * Emits a quad from 4 vertices.
     */
    public void quad(Vertex v0, Vertex v1, Vertex v2, Vertex v3) {
        emitVertex(v0);
        emitVertex(v1);
        emitVertex(v2);
        emitVertex(v3);
    }
    
    // =========================================================================
    // Static utilities
    // =========================================================================
    
    /**
     * Creates an ARGB color from components.
     */
    public static int argb(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Creates an ARGB color from float components.
     */
    public static int argb(float a, float r, float g, float b) {
        return argb(
            (int) (a * 255) & 0xFF,
            (int) (r * 255) & 0xFF,
            (int) (g * 255) & 0xFF,
            (int) (b * 255) & 0xFF
        );
    }
    
    /**
     * Applies alpha to an existing color.
     */
    public static int withAlpha(int rgb, float alpha) {
        int a = (int) (alpha * 255) & 0xFF;
        return (rgb & 0x00FFFFFF) | (a << 24);
    }
    
    /**
     * Linearly interpolates between two colors.
     */
    public static int lerpColor(int start, int end, float t) {
        int sa = (start >> 24) & 0xFF;
        int sr = (start >> 16) & 0xFF;
        int sg = (start >> 8) & 0xFF;
        int sb = start & 0xFF;
        
        int ea = (end >> 24) & 0xFF;
        int er = (end >> 16) & 0xFF;
        int eg = (end >> 8) & 0xFF;
        int eb = end & 0xFF;
        
        int a = (int) (sa + (ea - sa) * t) & 0xFF;
        int r = (int) (sr + (er - sr) * t) & 0xFF;
        int g = (int) (sg + (eg - sg) * t) & 0xFF;
        int b = (int) (sb + (eb - sb) * t) & 0xFF;
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // =========================================================================
    // Wireframe emission
    // =========================================================================
    
    /**
     * Emits a mesh as wireframe (edges only).
     * 
     * @param mesh the mesh to render as wireframe
     * @param thickness line thickness (visual only, actual thickness depends on render layer)
     */
    public void emitWireframe(Mesh mesh, float thickness) {
        if (mesh.isEmpty()) {
            return;
        }
        
        PrimitiveType primType = mesh.primitiveType();
        
        if (primType == PrimitiveType.QUADS) {
            // For quads, draw only the 4 edges (not the internal diagonal)
            mesh.forEachQuad((v0, v1, v2, v3) -> {
                emitLineVertex(v0, v1);
                emitLineVertex(v1, v0);
                
                emitLineVertex(v1, v2);
                emitLineVertex(v2, v1);
                
                emitLineVertex(v2, v3);
                emitLineVertex(v3, v2);
                
                emitLineVertex(v3, v0);
                emitLineVertex(v0, v3);
            });
        } else {
            // For triangles, draw all 3 edges per triangle
            mesh.forEachTriangle((v0, v1, v2) -> {
                emitLineVertex(v0, v1);
                emitLineVertex(v1, v0);
                
                emitLineVertex(v1, v2);
                emitLineVertex(v2, v1);
                
                emitLineVertex(v2, v0);
                emitLineVertex(v0, v2);
            });
        }
    }
    
    /**
     * Static convenience method for emitting wireframe.
     */
    public static void emitWireframe(
            MatrixStack.Entry entry,
            VertexConsumer consumer,
            Mesh mesh,
            int argb,
            float thickness,
            int light) {
        VertexEmitter emitter = new VertexEmitter(entry, consumer);
        emitter.color(argb).light(light).emitWireframe(mesh, thickness);
    }
    
    /**
     * Static wireframe emission per ARCHITECTURE.md signature.
     * 
     * <p>Per ARCHITECTURE.md (lines 290-297):
     * <pre>
     * VertexEmitter.emitWireframe(consumer, mesh, matrix, argb, thickness, light);
     * </pre>
     */
    public static void emitWireframe(
            VertexConsumer consumer,
            Mesh mesh,
            Matrix4f matrix,
            int argb,
            float thickness,
            int light) {
        
        if (mesh == null || mesh.isEmpty()) {
            return;
        }
        
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        
        // Handle different primitive types for proper wireframe
        PrimitiveType primType = mesh.primitiveType();
        
        if (primType == PrimitiveType.QUADS) {
            // For quads, draw only the 4 edges (not the internal diagonal)
            mesh.forEachQuad((v0, v1, v2, v3) -> {
                emitLineSegment(consumer, matrix, v0, v1, r, g, b, a);
                emitLineSegment(consumer, matrix, v1, v2, r, g, b, a);
                emitLineSegment(consumer, matrix, v2, v3, r, g, b, a);
                emitLineSegment(consumer, matrix, v3, v0, r, g, b, a);
            });
        } else {
            // For triangles, draw all 3 edges per triangle
            mesh.forEachTriangle((v0, v1, v2) -> {
                emitLineSegment(consumer, matrix, v0, v1, r, g, b, a);
                emitLineSegment(consumer, matrix, v1, v2, r, g, b, a);
                emitLineSegment(consumer, matrix, v2, v0, r, g, b, a);
            });
        }
    }
    
    private static void emitLineSegment(
            VertexConsumer consumer,
            Matrix4f matrix,
            Vertex v0, Vertex v1,
            int r, int g, int b, int a) {
        
        // Direction for line normal
        float dx = v1.x() - v0.x();
        float dy = v1.y() - v0.y();
        float dz = v1.z() - v0.z();
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0) {
            dx /= len;
            dy /= len;
            dz /= len;
        }
        
        consumer.vertex(matrix, v0.x(), v0.y(), v0.z())
            .color(r, g, b, a)
            .normal(dx, dy, dz);
        
        consumer.vertex(matrix, v1.x(), v1.y(), v1.z())
            .color(r, g, b, a)
            .normal(dx, dy, dz);
    }
    
    // =========================================================================
    // Static API (per ARCHITECTURE.md)
    // =========================================================================
    
    /**
     * Emits a quad from 4 corner positions.
     * 
     * <p>Per ARCHITECTURE.md specification:
     * <pre>
     * VertexEmitter.emitQuad(consumer, matrix, corners, argb, light);
     * </pre>
     * 
     * @param consumer the vertex consumer to emit to
     * @param matrix   transformation matrix
     * @param corners  4 corner positions [v0, v1, v2, v3]
     * @param argb     color as ARGB int
     * @param light    light level
     */
    public static void emitQuad(
            VertexConsumer consumer,
            Matrix4f matrix,
            Vector3f[] corners,
            int argb,
            int light) {
        
        if (corners == null || corners.length != 4) {
            throw new IllegalArgumentException("Quad requires exactly 4 corners");
        }
        
        // Decompose color
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        
        // Calculate face normal from corners
        Vector3f edge1 = new Vector3f(corners[1]).sub(corners[0]);
        Vector3f edge2 = new Vector3f(corners[3]).sub(corners[0]);
        Vector3f normal = new Vector3f();
        edge1.cross(edge2, normal).normalize();
        
        // Emit 4 vertices
        for (int i = 0; i < 4; i++) {
            Vector4f pos = new Vector4f(corners[i].x, corners[i].y, corners[i].z, 1.0f);
            pos.mul(matrix);
            
            consumer.vertex(pos.x(), pos.y(), pos.z())
                .color(r, g, b, a)
                .texture(i == 0 || i == 3 ? 0 : 1, i < 2 ? 0 : 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normal.x(), normal.y(), normal.z());
        }
    }
    
    /**
     * Emits an entire mesh with transformation and color.
     * 
     * <p>Per ARCHITECTURE.md specification:
     * <pre>
     * VertexEmitter.emitMesh(consumer, mesh, matrix, argb, light);
     * </pre>
     * 
     * @param consumer the vertex consumer to emit to
     * @param mesh     the mesh to emit
     * @param matrix   transformation matrix
     * @param argb     color as ARGB int
     * @param light    light level
     */
    public static void emitMesh(
            VertexConsumer consumer,
            Mesh mesh,
            Matrix4f matrix,
            int argb,
            int light) {
        
        if (mesh == null || mesh.isEmpty()) {
            return;
        }
        
        // Decompose color
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        
        // Create a simple normal matrix (assuming uniform scale)
        Matrix3f normalMatrix = new Matrix3f(matrix);
        
        // Consumer for each vertex
        java.util.function.Consumer<Vertex> emitVertex = (v) -> {
            // Transform position
            Vector4f pos = new Vector4f(v.x(), v.y(), v.z(), 1.0f);
            pos.mul(matrix);
            
            // Transform normal
            Vector3f normal = new Vector3f(v.nx(), v.ny(), v.nz());
            normal.mul(normalMatrix).normalize();
            
            consumer.vertex(pos.x(), pos.y(), pos.z())
                .color(r, g, b, a)
                .texture(v.u(), v.v())
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normal.x(), normal.y(), normal.z());
        };
        
        // Emit based on primitive type
        switch (mesh.primitiveType()) {
            case TRIANGLES -> mesh.forEachTriangle((v0, v1, v2) -> {
                emitVertex.accept(v0);
                emitVertex.accept(v1);
                emitVertex.accept(v2);
            });
            case QUADS -> mesh.forEachQuad((v0, v1, v2, v3) -> {
                emitVertex.accept(v0);
                emitVertex.accept(v1);
                emitVertex.accept(v2);
                emitVertex.accept(v3);
            });
            default -> throw new UnsupportedOperationException(
                "Static emitMesh does not support: " + mesh.primitiveType());
        }
    }

}
