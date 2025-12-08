package net.cyberpunk042.client.visual._legacy.render;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Renders axis-aligned cubes and boxes.
 * 
 * <p>Extracted from GlowQuadEmitter for shared use across the field system.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Simple cube render
 * CubeRenderer_old.render(entry, consumer, box, argb, light);
 * 
 * // With sprite
 * CubeRenderer_old.renderWithSprite(entry, consumer, box, alpha, sprite, color);
 * </pre>
 * 
 * @see VertexEmitter
 */
public final class CubeRenderer_old {
    
    private CubeRenderer_old() {} // Static utility class
    
    // =========================================================================
    // Static rendering methods
    // =========================================================================
    
    /**
     * Renders a cube/box with solid color.
     * 
     * @param entry    matrix stack entry for transformations
     * @param consumer vertex consumer to emit to
     * @param box      the bounding box to render
     * @param argb     color as ARGB int
     * @param light    light level
     */
    public static void render(
            MatrixStack.Entry entry,
            VertexConsumer consumer,
            Box box,
            int argb,
            int light) {
        
        if (box == null) {
            return;
        }
        
        Logging.RENDER.topic("cube").trace(
            "Rendering cube: min=[{:.2f},{:.2f},{:.2f}] max=[{:.2f},{:.2f},{:.2f}]",
            box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        
        Matrix4f posMatrix = entry.getPositionMatrix();
        Matrix3f normMatrix = entry.getNormalMatrix();
        
        float minX = (float) box.minX;
        float maxX = (float) box.maxX;
        float minY = (float) box.minY;
        float maxY = (float) box.maxY;
        float minZ = (float) box.minZ;
        float maxZ = (float) box.maxZ;
        
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        
        // Front face (+Z)
        emitQuadFace(consumer, posMatrix, normMatrix,
            minX, minY, maxZ,
            maxX, minY, maxZ,
            maxX, maxY, maxZ,
            minX, maxY, maxZ,
            0, 0, 1,
            r, g, b, a, light);
        
        // Back face (-Z)
        emitQuadFace(consumer, posMatrix, normMatrix,
            maxX, minY, minZ,
            minX, minY, minZ,
            minX, maxY, minZ,
            maxX, maxY, minZ,
            0, 0, -1,
            r, g, b, a, light);
        
        // Left face (-X)
        emitQuadFace(consumer, posMatrix, normMatrix,
            minX, minY, minZ,
            minX, minY, maxZ,
            minX, maxY, maxZ,
            minX, maxY, minZ,
            -1, 0, 0,
            r, g, b, a, light);
        
        // Right face (+X)
        emitQuadFace(consumer, posMatrix, normMatrix,
            maxX, minY, maxZ,
            maxX, minY, minZ,
            maxX, maxY, minZ,
            maxX, maxY, maxZ,
            1, 0, 0,
            r, g, b, a, light);
        
        // Top face (+Y)
        emitQuadFace(consumer, posMatrix, normMatrix,
            minX, maxY, maxZ,
            maxX, maxY, maxZ,
            maxX, maxY, minZ,
            minX, maxY, minZ,
            0, 1, 0,
            r, g, b, a, light);
        
        // Bottom face (-Y)
        emitQuadFace(consumer, posMatrix, normMatrix,
            minX, minY, minZ,
            maxX, minY, minZ,
            maxX, minY, maxZ,
            minX, minY, maxZ,
            0, -1, 0,
            r, g, b, a, light);
    }
    
    /**
     * Renders a cube/box with a sprite texture.
     * 
     * @param entry    matrix stack entry
     * @param consumer vertex consumer
     * @param box      bounding box to render
     * @param alpha    alpha value (0-1)
     * @param sprite   texture sprite
     * @param color    RGB color as float array [r, g, b]
     */
    public static void renderWithSprite(
            MatrixStack.Entry entry,
            VertexConsumer consumer,
            Box box,
            float alpha,
            Sprite sprite,
            float[] color) {
        
        if (box == null || sprite == null || alpha <= 0.01f) {
            return;
        }
        
        Matrix4f posMatrix = entry.getPositionMatrix();
        Matrix3f normMatrix = entry.getNormalMatrix();
        
        float minX = (float) box.minX;
        float maxX = (float) box.maxX;
        float minY = (float) box.minY;
        float maxY = (float) box.maxY;
        float minZ = (float) box.minZ;
        float maxZ = (float) box.maxZ;
        
        float minU = sprite.getFrameU(0.0f);
        float maxU = sprite.getFrameU(1.0f);
        float minV = sprite.getFrameV(0.0f);
        float maxV = sprite.getFrameV(1.0f);
        
        int alphaInt = MathHelper.clamp((int) (alpha * 255.0f), 0, 255);
        int r = (int) (color[0] * 255) & 0xFF;
        int g = (int) (color[1] * 255) & 0xFF;
        int b = (int) (color[2] * 255) & 0xFF;
        
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        
        // Front face (+Z)
        emitQuadFaceUV(consumer, posMatrix, normMatrix,
            minX, minY, maxZ, minU, maxV,
            maxX, minY, maxZ, maxU, maxV,
            maxX, maxY, maxZ, maxU, minV,
            minX, maxY, maxZ, minU, minV,
            0, 0, 1,
            r, g, b, alphaInt, light);
        
        // Back face (-Z)
        emitQuadFaceUV(consumer, posMatrix, normMatrix,
            maxX, minY, minZ, minU, maxV,
            minX, minY, minZ, maxU, maxV,
            minX, maxY, minZ, maxU, minV,
            maxX, maxY, minZ, minU, minV,
            0, 0, -1,
            r, g, b, alphaInt, light);
        
        // Left face (-X)
        emitQuadFaceUV(consumer, posMatrix, normMatrix,
            minX, minY, minZ, minU, maxV,
            minX, minY, maxZ, maxU, maxV,
            minX, maxY, maxZ, maxU, minV,
            minX, maxY, minZ, minU, minV,
            -1, 0, 0,
            r, g, b, alphaInt, light);
        
        // Right face (+X)
        emitQuadFaceUV(consumer, posMatrix, normMatrix,
            maxX, minY, maxZ, minU, maxV,
            maxX, minY, minZ, maxU, maxV,
            maxX, maxY, minZ, maxU, minV,
            maxX, maxY, maxZ, minU, minV,
            1, 0, 0,
            r, g, b, alphaInt, light);
        
        // Top face (+Y)
        emitQuadFaceUV(consumer, posMatrix, normMatrix,
            minX, maxY, maxZ, minU, maxV,
            maxX, maxY, maxZ, maxU, maxV,
            maxX, maxY, minZ, maxU, minV,
            minX, maxY, minZ, minU, minV,
            0, 1, 0,
            r, g, b, alphaInt, light);
        
        // Bottom face (-Y)
        emitQuadFaceUV(consumer, posMatrix, normMatrix,
            minX, minY, minZ, minU, maxV,
            maxX, minY, minZ, maxU, maxV,
            maxX, minY, maxZ, maxU, minV,
            minX, minY, maxZ, minU, minV,
            0, -1, 0,
            r, g, b, alphaInt, light);
    }
    
    /**
     * Renders a wireframe cube (edges only).
     * 
     * @param entry    matrix stack entry
     * @param consumer vertex consumer (should be from a lines render layer)
     * @param box      bounding box
     * @param argb     color as ARGB
     */
    public static void renderWireframe(
            MatrixStack.Entry entry,
            VertexConsumer consumer,
            Box box,
            int argb) {
        
        if (box == null) {
            return;
        }
        
        Matrix4f posMatrix = entry.getPositionMatrix();
        
        float minX = (float) box.minX;
        float maxX = (float) box.maxX;
        float minY = (float) box.minY;
        float maxY = (float) box.maxY;
        float minZ = (float) box.minZ;
        float maxZ = (float) box.maxZ;
        
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        
        // 12 edges of a cube
        // Bottom face edges
        emitLine(consumer, posMatrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        emitLine(consumer, posMatrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        emitLine(consumer, posMatrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        emitLine(consumer, posMatrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
        
        // Top face edges
        emitLine(consumer, posMatrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        emitLine(consumer, posMatrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        emitLine(consumer, posMatrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        emitLine(consumer, posMatrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        
        // Vertical edges
        emitLine(consumer, posMatrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        emitLine(consumer, posMatrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        emitLine(consumer, posMatrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        emitLine(consumer, posMatrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }
    
    // =========================================================================
    // Private helpers
    // =========================================================================
    
    private static void emitQuadFace(
            VertexConsumer consumer,
            Matrix4f posMatrix,
            Matrix3f normMatrix,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float nx, float ny, float nz,
            int r, int g, int b, int a,
            int light) {
        
        // Transform normal
        Vector3f normal = new Vector3f(nx, ny, nz);
        normal.mul(normMatrix);
        
        emitVertex(consumer, posMatrix, x0, y0, z0, 0, 1, normal, r, g, b, a, light);
        emitVertex(consumer, posMatrix, x1, y1, z1, 1, 1, normal, r, g, b, a, light);
        emitVertex(consumer, posMatrix, x2, y2, z2, 1, 0, normal, r, g, b, a, light);
        emitVertex(consumer, posMatrix, x3, y3, z3, 0, 0, normal, r, g, b, a, light);
    }
    
    private static void emitQuadFaceUV(
            VertexConsumer consumer,
            Matrix4f posMatrix,
            Matrix3f normMatrix,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            float nx, float ny, float nz,
            int r, int g, int b, int a,
            int light) {
        
        Vector3f normal = new Vector3f(nx, ny, nz);
        normal.mul(normMatrix);
        
        emitVertex(consumer, posMatrix, x0, y0, z0, u0, v0, normal, r, g, b, a, light);
        emitVertex(consumer, posMatrix, x1, y1, z1, u1, v1, normal, r, g, b, a, light);
        emitVertex(consumer, posMatrix, x2, y2, z2, u2, v2, normal, r, g, b, a, light);
        emitVertex(consumer, posMatrix, x3, y3, z3, u3, v3, normal, r, g, b, a, light);
    }
    
    private static void emitVertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x, float y, float z,
            float u, float v,
            Vector3f normal,
            int r, int g, int b, int a,
            int light) {
        
        consumer.vertex(matrix, x, y, z)
            .color(r, g, b, a)
            .texture(u, v)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(normal.x(), normal.y(), normal.z());
    }
    
    private static void emitLine(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            int r, int g, int b, int a) {
        
        // Direction for line normal
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0) {
            dx /= len;
            dy /= len;
            dz /= len;
        }
        
        consumer.vertex(matrix, x0, y0, z0)
            .color(r, g, b, a)
            .normal(dx, dy, dz);
        
        consumer.vertex(matrix, x1, y1, z1)
            .color(r, g, b, a)
            .normal(dx, dy, dz);
    }
}

