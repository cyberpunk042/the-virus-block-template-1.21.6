package net.cyberpunk042.client.visual._legacy.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.color.ColorMath;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * Utility for rendering wireframe/edge views of meshes.
 * 
 * <p>Useful for debugging, design visualization, and
 * stylized effects.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Render mesh as wireframe
 * WireframeRenderer_old.render(matrices, consumers, mesh, color, light);
 * 
 * // Render with custom line thickness
 * WireframeRenderer_old.render(matrices, consumers, mesh, color, 2.0f, light);
 * </pre>
 */
public final class WireframeRenderer_old {
    
    /** Default line thickness */
    public static final float DEFAULT_THICKNESS = 1.0f;
    
    private WireframeRenderer_old() {}
    
    // ─────────────────────────────────────────────────────────────────────────
    // Wireframe Rendering
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Renders a mesh as wireframe.
     * 
     * @param matrices Matrix stack
     * @param consumers Vertex consumer provider
     * @param mesh Mesh to render
     * @param color Line color (ARGB)
     * @param thickness Line thickness
     * @param light Light level
     */
    public static void render(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Mesh mesh,
            int color,
            float thickness,
            int light) {
        
        if (mesh == null || mesh.isEmpty()) {
            return;
        }
        
        RenderLayer layer = FieldRenderLayers.lines();
        VertexConsumer consumer = consumers.getBuffer(layer);
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        float a = ColorMath.alphaF(color);
        float r = ColorMath.redF(color);
        float g = ColorMath.greenF(color);
        float b = ColorMath.blueF(color);
        
        // Draw edges of each triangle
        mesh.forEachTriangle((v0, v1, v2) -> {
            // Edge 0-1
            emitLine(consumer, matrix, v0, v1, r, g, b, a, light);
            // Edge 1-2
            emitLine(consumer, matrix, v1, v2, r, g, b, a, light);
            // Edge 2-0
            emitLine(consumer, matrix, v2, v0, r, g, b, a, light);
        });
        
        Logging.RENDER.topic("wireframe").trace(
            "Rendered wireframe: {} triangles, {} edges", 
            mesh.primitiveCount(), mesh.primitiveCount() * 3);
    }
    
    /**
     * Renders with default thickness.
     */
    public static void render(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Mesh mesh,
            int color,
            int light) {
        render(matrices, consumers, mesh, color, DEFAULT_THICKNESS, light);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Bounding Box
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Renders a wireframe bounding box.
     * 
     * @param minX Min X coordinate
     * @param minY Min Y coordinate
     * @param minZ Min Z coordinate
     * @param maxX Max X coordinate
     * @param maxY Max Y coordinate
     * @param maxZ Max Z coordinate
     */
    public static void renderBox(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            int color,
            int light) {
        
        RenderLayer layer = FieldRenderLayers.lines();
        VertexConsumer consumer = consumers.getBuffer(layer);
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        float a = ColorMath.alphaF(color);
        float r = ColorMath.redF(color);
        float g = ColorMath.greenF(color);
        float b = ColorMath.blueF(color);
        
        // Bottom face
        emitLineCoords(consumer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a, light);
        emitLineCoords(consumer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a, light);
        emitLineCoords(consumer, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a, light);
        emitLineCoords(consumer, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a, light);
        
        // Top face
        emitLineCoords(consumer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a, light);
        emitLineCoords(consumer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a, light);
        emitLineCoords(consumer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a, light);
        emitLineCoords(consumer, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a, light);
        
        // Vertical edges
        emitLineCoords(consumer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a, light);
        emitLineCoords(consumer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a, light);
        emitLineCoords(consumer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a, light);
        emitLineCoords(consumer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a, light);
        
        Logging.RENDER.topic("wireframe").trace("Rendered bounding box");
    }
    
    /**
     * Renders a wireframe sphere (latitude/longitude lines).
     */
    public static void renderSphereOutline(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            float radius,
            int latLines,
            int lonLines,
            int color,
            int light) {
        
        RenderLayer layer = FieldRenderLayers.lines();
        VertexConsumer consumer = consumers.getBuffer(layer);
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        float a = ColorMath.alphaF(color);
        float r = ColorMath.redF(color);
        float g = ColorMath.greenF(color);
        float b = ColorMath.blueF(color);
        
        // Latitude lines (horizontal circles)
        for (int lat = 1; lat < latLines; lat++) {
            float theta = (float) Math.PI * lat / latLines;
            float y = radius * (float) Math.cos(theta);
            float ringRadius = radius * (float) Math.sin(theta);
            
            for (int lon = 0; lon < lonLines; lon++) {
                float phi1 = (float) (2 * Math.PI * lon / lonLines);
                float phi2 = (float) (2 * Math.PI * (lon + 1) / lonLines);
                
                float x1 = ringRadius * (float) Math.cos(phi1);
                float z1 = ringRadius * (float) Math.sin(phi1);
                float x2 = ringRadius * (float) Math.cos(phi2);
                float z2 = ringRadius * (float) Math.sin(phi2);
                
                emitLineCoords(consumer, matrix, x1, y, z1, x2, y, z2, r, g, b, a, light);
            }
        }
        
        // Longitude lines (vertical arcs)
        for (int lon = 0; lon < lonLines; lon++) {
            float phi = (float) (2 * Math.PI * lon / lonLines);
            float cosPhi = (float) Math.cos(phi);
            float sinPhi = (float) Math.sin(phi);
            
            for (int lat = 0; lat < latLines; lat++) {
                float theta1 = (float) Math.PI * lat / latLines;
                float theta2 = (float) Math.PI * (lat + 1) / latLines;
                
                float x1 = radius * (float) Math.sin(theta1) * cosPhi;
                float y1 = radius * (float) Math.cos(theta1);
                float z1 = radius * (float) Math.sin(theta1) * sinPhi;
                
                float x2 = radius * (float) Math.sin(theta2) * cosPhi;
                float y2 = radius * (float) Math.cos(theta2);
                float z2 = radius * (float) Math.sin(theta2) * sinPhi;
                
                emitLineCoords(consumer, matrix, x1, y1, z1, x2, y2, z2, r, g, b, a, light);
            }
        }
        
        Logging.RENDER.topic("wireframe").trace(
            "Rendered sphere outline: radius={:.2f}, lat={}, lon={}", 
            radius, latLines, lonLines);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────────────────
    
    private static void emitLine(
            VertexConsumer consumer,
            Matrix4f matrix,
            Vertex v0, Vertex v1,
            float r, float g, float b, float a,
            int light) {
        emitLineCoords(consumer, matrix, 
            v0.x(), v0.y(), v0.z(), 
            v1.x(), v1.y(), v1.z(),
            r, g, b, a, light);
    }
    
    private static void emitLineCoords(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float r, float g, float b, float a,
            int light) {
        
        // Calculate normal for the line (perpendicular to line direction)
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len > 0) {
            dx /= len; dy /= len; dz /= len;
        }
        
        consumer.vertex(matrix, x1, y1, z1)
            .color(r, g, b, a)
            .texture(0, 0)
            .overlay(0)
            .light(light)
            .normal(dx, dy, dz);
            
        consumer.vertex(matrix, x2, y2, z2)
            .color(r, g, b, a)
            .texture(1, 1)
            .overlay(0)
            .light(light)
            .normal(dx, dy, dz);
    }
}
