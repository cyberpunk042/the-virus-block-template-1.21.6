package net.cyberpunk042.client.gui.render;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.cyberpunk042.client.visual.render.FieldRenderLayers;
import net.cyberpunk042.log.Logging;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Client-side renderer for the test/preview field.
 * 
 * <p>Renders a preview of the current {@link FieldEditState} following the player.
 * This is purely client-side and doesn't require server communication.</p>
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Reads configuration from FieldEditStateHolder</li>
 *   <li>Renders at player position (with offset)</li>
 *   <li>Updates in real-time as GUI/commands change state</li>
 *   <li>Debounced to prevent performance issues</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class TestFieldRenderer {
    
    private static boolean initialized = false;
    private static long lastRenderTime = 0;
    private static final long DEBOUNCE_MS = 16; // ~60fps max
    
    // Cached mesh data (rebuilt when state changes)
    private static boolean meshDirty = true;
    private static long stateVersion = 0;
    
    private TestFieldRenderer() {}
    
    /**
     * Registers the test field renderer with world render events.
     * Call once during client initialization.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        
        WorldRenderEvents.AFTER_ENTITIES.register(TestFieldRenderer::render);
        Logging.GUI.topic("render").info("TestFieldRenderer initialized");
    }
    
    /**
     * Marks the mesh as needing rebuild (call when FieldEditState changes).
     */
    public static void markDirty() {
        meshDirty = true;
    }
    
    /**
     * Main render callback.
     */
    private static void render(WorldRenderContext context) {
        // Check if test field is active
        if (!FieldEditStateHolder.isTestFieldActive()) {
            return;
        }
        
        FieldEditState state = FieldEditStateHolder.get();
        if (state == null) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            return;
        }
        
        // Debounce
        long now = System.currentTimeMillis();
        if (now - lastRenderTime < DEBOUNCE_MS) {
            return;
        }
        lastRenderTime = now;
        
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        MatrixStack matrices = context.matrixStack();
        
        float tickDelta = client.getRenderTickCounter().getTickProgress(false);
        float worldTime = context.world().getTime() + tickDelta;
        
        // Calculate position (follow player with offset)
        Vec3d playerPos = player.getPos().add(0, 1.0, 0); // Center at chest height
        Vec3d renderPos = playerPos.subtract(camPos);
        
        // Render the test field
        renderTestField(matrices, consumers, state, renderPos, worldTime);
    }
    
    /**
     * Renders the test field based on current FieldEditState.
     */
    private static void renderTestField(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldEditState state,
            Vec3d pos,
            float time) {
        
        matrices.push();
        matrices.translate(pos.x, pos.y, pos.z);
        
        // Apply transform from state
        float scale = state.getScale();
        matrices.scale(scale, scale, scale);
        
        // Apply spin animation
        if (state.isSpinEnabled()) {
            float spinSpeed = state.getSpinSpeed();
            float rotation = (time * spinSpeed) % 360f;
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        }
        
        // Get render layer (translucent for all test fields)
        RenderLayer layer = FieldRenderLayers.solidTranslucent();
        VertexConsumer consumer = consumers.getBuffer(layer);
        
        // Get color
        int color = state.getColor();
        float alpha = state.getAlpha();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int)(alpha * 255);
        
        // Render based on shape type
        String shapeType = state.getShapeType().toLowerCase();
        float radius = state.getRadius();
        
        switch (shapeType) {
            case "sphere" -> renderSphere(matrices, consumer, radius, r, g, b, a, state);
            case "ring" -> renderRing(matrices, consumer, radius, r, g, b, a, state);
            case "disc" -> renderDisc(matrices, consumer, radius, r, g, b, a, state);
            default -> renderSphere(matrices, consumer, radius, r, g, b, a, state); // Default to sphere
        }
        
        matrices.pop();
    }
    
    /**
     * Renders a sphere shape.
     */
    private static void renderSphere(
            MatrixStack matrices,
            VertexConsumer consumer,
            float radius,
            int r, int g, int b, int a,
            FieldEditState state) {
        
        int latSteps = state.getSphereLatSteps();
        int lonSteps = state.getSphereLonSteps();
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        // Simple lat/lon sphere tessellation
        for (int lat = 0; lat < latSteps; lat++) {
            float lat0 = (float) Math.PI * (-0.5f + (float) lat / latSteps);
            float lat1 = (float) Math.PI * (-0.5f + (float) (lat + 1) / latSteps);
            
            float y0 = MathHelper.sin(lat0) * radius;
            float y1 = MathHelper.sin(lat1) * radius;
            float r0 = MathHelper.cos(lat0) * radius;
            float r1 = MathHelper.cos(lat1) * radius;
            
            for (int lon = 0; lon < lonSteps; lon++) {
                float lon0 = 2 * (float) Math.PI * lon / lonSteps;
                float lon1 = 2 * (float) Math.PI * (lon + 1) / lonSteps;
                
                float x00 = MathHelper.cos(lon0) * r0;
                float z00 = MathHelper.sin(lon0) * r0;
                float x10 = MathHelper.cos(lon1) * r0;
                float z10 = MathHelper.sin(lon1) * r0;
                float x01 = MathHelper.cos(lon0) * r1;
                float z01 = MathHelper.sin(lon0) * r1;
                float x11 = MathHelper.cos(lon1) * r1;
                float z11 = MathHelper.sin(lon1) * r1;
                
                // Triangle 1
                vertex(consumer, matrix, x00, y0, z00, r, g, b, a);
                vertex(consumer, matrix, x10, y0, z10, r, g, b, a);
                vertex(consumer, matrix, x01, y1, z01, r, g, b, a);
                
                // Triangle 2
                vertex(consumer, matrix, x10, y0, z10, r, g, b, a);
                vertex(consumer, matrix, x11, y1, z11, r, g, b, a);
                vertex(consumer, matrix, x01, y1, z01, r, g, b, a);
            }
        }
    }
    
    /**
     * Renders a ring shape.
     */
    private static void renderRing(
            MatrixStack matrices,
            VertexConsumer consumer,
            float radius,
            int r, int g, int b, int a,
            FieldEditState state) {
        
        float innerRadius = state.getRingInnerRadius();
        float outerRadius = state.getRingOuterRadius();
        int segments = state.getRingSegments();
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        for (int i = 0; i < segments; i++) {
            float angle0 = 2 * (float) Math.PI * i / segments;
            float angle1 = 2 * (float) Math.PI * (i + 1) / segments;
            
            float x0i = MathHelper.cos(angle0) * innerRadius;
            float z0i = MathHelper.sin(angle0) * innerRadius;
            float x0o = MathHelper.cos(angle0) * outerRadius;
            float z0o = MathHelper.sin(angle0) * outerRadius;
            float x1i = MathHelper.cos(angle1) * innerRadius;
            float z1i = MathHelper.sin(angle1) * innerRadius;
            float x1o = MathHelper.cos(angle1) * outerRadius;
            float z1o = MathHelper.sin(angle1) * outerRadius;
            
            // Top face quad as two triangles
            vertex(consumer, matrix, x0i, 0, z0i, r, g, b, a);
            vertex(consumer, matrix, x0o, 0, z0o, r, g, b, a);
            vertex(consumer, matrix, x1i, 0, z1i, r, g, b, a);
            
            vertex(consumer, matrix, x0o, 0, z0o, r, g, b, a);
            vertex(consumer, matrix, x1o, 0, z1o, r, g, b, a);
            vertex(consumer, matrix, x1i, 0, z1i, r, g, b, a);
        }
    }
    
    /**
     * Renders a disc shape.
     */
    private static void renderDisc(
            MatrixStack matrices,
            VertexConsumer consumer,
            float radius,
            int r, int g, int b, int a,
            FieldEditState state) {
        
        int segments = state.getDiscSegments();
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        for (int i = 0; i < segments; i++) {
            float angle0 = 2 * (float) Math.PI * i / segments;
            float angle1 = 2 * (float) Math.PI * (i + 1) / segments;
            
            float x0 = MathHelper.cos(angle0) * radius;
            float z0 = MathHelper.sin(angle0) * radius;
            float x1 = MathHelper.cos(angle1) * radius;
            float z1 = MathHelper.sin(angle1) * radius;
            
            // Triangle from center to edge
            vertex(consumer, matrix, 0, 0, 0, r, g, b, a);
            vertex(consumer, matrix, x0, 0, z0, r, g, b, a);
            vertex(consumer, matrix, x1, 0, z1, r, g, b, a);
        }
    }
    
    /**
     * Helper to emit a vertex.
     */
    private static void vertex(VertexConsumer consumer, Matrix4f matrix, 
                               float x, float y, float z, int r, int g, int b, int a) {
        consumer.vertex(matrix, x, y, z)
            .color(r, g, b, a)
            .normal(0, 1, 0)
            .texture(0, 0)
            .overlay(0)
            .light(15728880); // Full bright
    }
}

