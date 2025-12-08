package net.cyberpunk042.client.visual._legacy.mesh.sphere;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Type E sphere renderer using rotated rectangles.
 *
 * <h2>Algorithm</h2>
 * <p>Creates a sphere by stacking horizontal layers of thin rectangles.
 * Each layer has rectangles rotated around the Y-axis at 22.5° intervals,
 * forming a 16-sided polygon (hexadecagon) when viewed from above.</p>
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li>Fewest elements of all algorithms</li>
 *   <li>Still looks round from distance</li>
 *   <li>Excellent for animations and LOD</li>
 *   <li>Best for many simultaneous spheres</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 * TypeESphere_old renderer = TypeESphere_old.builder()
 *     .radius(0.5f)
 *     .verticalLayers(6)
 *     .color(0xFF00CCFF)
 *     .build();
 *
 * renderer.render(matrices, buffer, light, overlay);
 * </pre>
 *
 * <p>Based on MC Sphere Model Generator by Dylan Dang.</p>
 *
 * @see SphereAlgorithm_old#TYPE_E
 * @see TypeASphereRenderer
 */
public final class TypeESphere_old {

    /** Hexadecagon side ratio: tan(π/16) */
    private static final float SIDE_RATIO = (float) Math.tan(Math.PI / 16.0);

    private final float radius;
    private final int verticalLayers;
    private final float red, green, blue, alpha;

    private TypeESphere_old(Builder builder) {
        this.radius = builder.radius;
        this.verticalLayers = builder.verticalLayers;
        this.red = builder.red;
        this.green = builder.green;
        this.blue = builder.blue;
        this.alpha = builder.alpha;

        Logging.RENDER.topic("sphere").debug(
            "Created TypeESphere_old: radius={:.2f}, layers={}, color=#{:02X}{:02X}{:02X}{:02X}",
            radius, verticalLayers,
            (int)(alpha * 255), (int)(red * 255), (int)(green * 255), (int)(blue * 255));
    }

    // =========================================================================
    // Builder
    // =========================================================================

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private float radius = 0.45f;
        private int verticalLayers = 6;
        private float red = 1f, green = 1f, blue = 1f, alpha = 1f;

        private Builder() {}

        /**
         * Sets the sphere radius in blocks.
         * @param radius Radius (0.5 = half block, fits inside)
         */
        public Builder radius(float radius) {
            this.radius = Math.max(0.01f, radius);
            return this;
        }

        /**
         * Sets vertical layer count (4-10 recommended).
         * More layers = smoother sphere.
         */
        public Builder verticalLayers(int layers) {
            this.verticalLayers = Math.max(2, Math.min(16, layers));
            return this;
        }

        /**
         * Sets color from ARGB int.
         */
        public Builder color(int argb) {
            this.alpha = ((argb >> 24) & 0xFF) / 255f;
            this.red = ((argb >> 16) & 0xFF) / 255f;
            this.green = ((argb >> 8) & 0xFF) / 255f;
            this.blue = (argb & 0xFF) / 255f;
            return this;
        }

        /**
         * Sets color from RGBA floats (0-1 range).
         */
        public Builder color(float r, float g, float b, float a) {
            this.red = r;
            this.green = g;
            this.blue = b;
            this.alpha = a;
            return this;
        }

        public TypeESphere_old build() {
            return new TypeESphere_old(this);
        }
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    /**
     * Renders the sphere at the current matrix position.
     * Call matrices.translate() before this to position the sphere.
     *
     * @param matrices The matrix stack (should be centered on sphere position)
     * @param buffer The vertex consumer (use RenderLayer.getSolid() or getTranslucent())
     * @param light Lightmap coordinates
     * @param overlay Overlay coordinates
     */
    public void render(MatrixStack matrices, VertexConsumer buffer, int light, int overlay) {
        double vertStep = 90.0 / (verticalLayers + 1);

        for (int layer = 1; layer <= verticalLayers; layer++) {
            double levelDeg = vertStep * layer;
            double levelRad = Math.toRadians(levelDeg);

            float layerRadius = (float) (radius * Math.cos(levelRad));
            float y = (float) (radius * Math.sin(levelRad));
            float side = layerRadius * SIDE_RATIO;

            // Z-aligned rectangles at various rotations (-45°, -22.5°, 0°, +22.5°, +45°)
            for (float angle = -45f; angle < 67.5f; angle += 22.5f) {
                matrices.push();
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                renderCube(matrices, buffer, -side, -y, -layerRadius, side, y, layerRadius, light, overlay);
                matrices.pop();
            }

            // X-aligned rectangles at various rotations (-22.5°, 0°, +22.5°)
            for (float angle = -22.5f; angle < 45f; angle += 22.5f) {
                matrices.push();
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                renderCube(matrices, buffer, -layerRadius, -y, -side, layerRadius, y, side, light, overlay);
                matrices.pop();
            }
        }
    }

    /**
     * Renders with color override.
     */
    public void render(MatrixStack matrices, VertexConsumer buffer, int light, int overlay,
                       float r, float g, float b, float a) {
        TypeESphere_old temp = builder()
            .radius(radius)
            .verticalLayers(verticalLayers)
            .color(r, g, b, a)
            .build();
        temp.render(matrices, buffer, light, overlay);
    }

    /**
     * Renders at a specific position (convenience method).
     */
    public void renderAt(MatrixStack matrices, VertexConsumer buffer, int light, int overlay,
                         double x, double y, double z) {
        matrices.push();
        matrices.translate(x, y, z);
        render(matrices, buffer, light, overlay);
        matrices.pop();
    }

    // =========================================================================
    // Cube Rendering
    // =========================================================================

    private void renderCube(MatrixStack matrices, VertexConsumer buffer,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            int light, int overlay) {
        Matrix4f pos = matrices.peek().getPositionMatrix();
        Matrix3f norm = matrices.peek().getNormalMatrix();

        // North face (-Z)
        quad(buffer, pos, norm,
             x2, y1, z1,  x1, y1, z1,  x1, y2, z1,  x2, y2, z1,
             0, 0, -1, light, overlay);

        // South face (+Z)
        quad(buffer, pos, norm,
             x1, y1, z2,  x2, y1, z2,  x2, y2, z2,  x1, y2, z2,
             0, 0, 1, light, overlay);

        // West face (-X)
        quad(buffer, pos, norm,
             x1, y1, z1,  x1, y1, z2,  x1, y2, z2,  x1, y2, z1,
             -1, 0, 0, light, overlay);

        // East face (+X)
        quad(buffer, pos, norm,
             x2, y1, z2,  x2, y1, z1,  x2, y2, z1,  x2, y2, z2,
             1, 0, 0, light, overlay);

        // Down face (-Y)
        quad(buffer, pos, norm,
             x1, y1, z1,  x2, y1, z1,  x2, y1, z2,  x1, y1, z2,
             0, -1, 0, light, overlay);

        // Up face (+Y)
        quad(buffer, pos, norm,
             x1, y2, z2,  x2, y2, z2,  x2, y2, z1,  x1, y2, z1,
             0, 1, 0, light, overlay);
    }

    private void quad(VertexConsumer buffer, Matrix4f pos, Matrix3f norm,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float x3, float y3, float z3,
                      float x4, float y4, float z4,
                      float nx, float ny, float nz,
                      int light, int overlay) {
        vertex(buffer, pos, norm, x1, y1, z1, nx, ny, nz, 0, 1, light, overlay);
        vertex(buffer, pos, norm, x2, y2, z2, nx, ny, nz, 1, 1, light, overlay);
        vertex(buffer, pos, norm, x3, y3, z3, nx, ny, nz, 1, 0, light, overlay);
        vertex(buffer, pos, norm, x4, y4, z4, nx, ny, nz, 0, 0, light, overlay);
    }

    private void vertex(VertexConsumer buffer, Matrix4f pos, Matrix3f norm,
                        float x, float y, float z,
                        float nx, float ny, float nz,
                        float u, float v,
                        int light, int overlay) {
        buffer.vertex(pos, x, y, z)
              .color(red, green, blue, alpha)
              .texture(u, v)
              .overlay(overlay)
              .light(light)
              .normal(nx, ny, nz);
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    /**
     * Estimates element count for given parameters.
     * Type E has fixed 8 elements per layer (5 Z-aligned + 3 X-aligned).
     */
    public static int estimateElementCount(int verticalLayers) {
        return verticalLayers * 8;
    }

    /**
     * Creates a simple white sphere renderer.
     */
    public static TypeESphere_old simple(float radius) {
        return builder().radius(radius).build();
    }

    // =========================================================================
    // Getters
    // =========================================================================

    public float radius() { return radius; }
    public int verticalLayers() { return verticalLayers; }
}
