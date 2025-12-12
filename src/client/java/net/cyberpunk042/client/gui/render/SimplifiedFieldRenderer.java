package net.cyberpunk042.client.gui.render;

import net.cyberpunk042.client.field.render.SphereRenderer;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.cyberpunk042.client.visual.render.FieldRenderLayers;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.shape.SphereAlgorithm;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Simplified client-side field renderer for preview and debug purposes.
 * 
 * <p>Renders a preview of the current {@link FieldEditState} following the player.
 * This is a lightweight renderer with partial feature support, designed for
 * fast feedback during editing.</p>
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Reads configuration from FieldEditStateHolder</li>
 *   <li>Renders at player position (with offset)</li>
 *   <li>Updates in real-time as GUI/commands change state</li>
 *   <li>Debounced to prevent performance issues</li>
 * </ul>
 * 
 * <h2>Limitations (vs FieldRenderer)</h2>
 * <ul>
 *   <li>No binding evaluation</li>
 *   <li>No trigger effects</li>
 *   <li>No lifecycle states</li>
 *   <li>Limited animation support</li>
 * </ul>
 * 
 * @see net.cyberpunk042.client.field.render.FieldRenderer
 * @see net.cyberpunk042.client.gui.state.DefinitionBuilder
 */
@Environment(EnvType.CLIENT)
public final class SimplifiedFieldRenderer {
    
    private static boolean initialized = false;
    private static long lastRenderTime = 0;
    private static final long DEBOUNCE_MS = 16; // ~60fps max
    
    // Debounce toggle (can be disabled for immediate updates in debug)
    private static boolean debounceEnabled = true;
    
    // Advanced rendering mode (uses full FieldRenderer pipeline)
    // Enabled by default to support fill mode, visibility mask, and other pipeline features
    private static boolean advancedModeEnabled = true;
    
    // Cached mesh data (rebuilt when state changes)
    private static boolean meshDirty = true;
    private static long stateVersion = 0;
    
    /** Enable/disable render debouncing. */
    public static void setDebounceEnabled(boolean enabled) {
        debounceEnabled = enabled;
        Logging.GUI.topic("render").debug("SimplifiedFieldRenderer debounce: {}", enabled ? "ON" : "OFF");
    }
    
    /** Check if debouncing is enabled. */
    public static boolean isDebounceEnabled() {
        return debounceEnabled;
    }
    
    /** Enable/disable advanced rendering mode (uses full pipeline). */
    public static void setAdvancedModeEnabled(boolean enabled) {
        advancedModeEnabled = enabled;
        Logging.GUI.topic("render").debug("SimplifiedFieldRenderer advanced mode: {}", enabled ? "ON" : "OFF");
    }
    
    /** Check if advanced rendering mode is enabled. */
    public static boolean isAdvancedModeEnabled() {
        return advancedModeEnabled;
    }
    
    private SimplifiedFieldRenderer() {}
    
    /**
     * Registers the test field renderer with world render events.
     * Call once during client initialization.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        
        WorldRenderEvents.AFTER_ENTITIES.register(SimplifiedFieldRenderer::render);
        Logging.GUI.topic("render").info("SimplifiedFieldRenderer initialized");
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
        
        // Track render time (debounce now only affects mesh rebuilding, not rendering)
        long now = System.currentTimeMillis();
        lastRenderTime = now;
        
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        MatrixStack matrices = context.matrixStack();
        
        float tickDelta = client.getRenderTickCounter().getTickProgress(false);
        float worldTime = context.world().getTime() + tickDelta;
        
        // Calculate position (follow player with offset)
        Vec3d playerPos = player.getPos().add(0, 1.0, 0); // Center at chest height
        Vec3d renderPos = playerPos.subtract(camPos);
        
        // Render using selected mode
        if (advancedModeEnabled) {
            // Advanced mode: Use full FieldRenderer pipeline
            renderAdvanced(matrices, consumers, state, renderPos, worldTime);
        } else {
            // Fast mode: Use simplified inline rendering  
            renderTestField(matrices, consumers, state, renderPos, worldTime);
        }
    }
    
    /**
     * Renders using the full FieldRenderer pipeline.
     * Converts FieldEditState to FieldDefinition and uses complete rendering.
     */
    private static void renderAdvanced(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldEditState state,
            Vec3d pos,
            float time) {
        
        // Convert state to definition
        var definition = net.cyberpunk042.client.gui.state.DefinitionBuilder.fromState(state);
        
        if (definition.layers() == null || definition.layers().isEmpty()) {
            Logging.GUI.topic("render").warn("[ACCURATE] No layers to render!");
            return;
        }
        
        // Use the FULL FieldRenderer pipeline (includes all CP4+ traces)
        // No try-catch fallback - if this fails, we need to fix the renderer
        net.cyberpunk042.client.field.render.FieldRenderer.render(
            matrices,
            consumers,
            definition,
            pos,
            1.0f,  // scale
            time,
            0.8f   // alpha
        );
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
        
        // Get transform from state
        var transform = state.transform();
        
        // Apply anchor offset (relative to player center at ~1 block height)
        if (transform != null && transform.anchor() != null) {
            var anchor = transform.anchor();
            // Anchor offset is relative to player feet, adjust to center
            matrices.translate(anchor.getX(), anchor.getY() - 1.0, anchor.getZ());
        }
        
        // Apply custom offset
        if (transform != null && transform.offset() != null) {
            var offset = transform.offset();
            matrices.translate(offset.x, offset.y, offset.z);
        }
        
        // Apply billboard (face camera)
        if (transform != null && transform.billboard() != net.cyberpunk042.visual.transform.Billboard.NONE) {
            Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            if (transform.billboard() == net.cyberpunk042.visual.transform.Billboard.FULL) {
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-camera.getPitch()));
            }
        }
        
        // Apply base rotation
        if (transform != null && transform.rotation() != null) {
            var rot = transform.rotation();
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(rot.x));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(rot.y));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(rot.z));
        }
        
        // Apply scale
        float scale = state.getFloat("transform.scale");
        matrices.scale(scale, scale, scale);
        
        // Apply non-uniform scale
        if (transform != null && transform.scaleXYZ() != null) {
            var s = transform.scaleXYZ();
            matrices.scale(s.x, s.y, s.z);
        }
        
        // Apply orbit animation
        if (transform != null && transform.orbit() != null && transform.orbit().isActive()) {
            var orbit = transform.orbit();
            float orbitAngle = (time * orbit.speed()) % 360f;
            float orbitRadius = orbit.radius();
            matrices.translate(
                MathHelper.cos((float) Math.toRadians(orbitAngle)) * orbitRadius,
                0,
                MathHelper.sin((float) Math.toRadians(orbitAngle)) * orbitRadius
            );
        }
        
        // Apply spin animation
        if (state.spin().isActive()) {
            float spinSpeed = state.getFloat("spin.speed");
            float rotation = (time * spinSpeed) % 360f;
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        }
        
        // Apply modifiers (bobbing, breathing)
        if (state.modifiers() != null && state.modifiers().hasAnimationModifiers()) {
            net.cyberpunk042.client.visual.animation.AnimationApplier.applyModifiers(
                matrices, state.modifiers(), time);
        }
        
        // Apply test trigger effects
        var triggerConfig = state.getActiveTestTrigger();
        float triggerAlpha = 1.0f;
        float triggerScale = 1.0f;
        if (triggerConfig != null) {
            float progress = state.getTestTriggerProgress();
            float intensity = triggerConfig.intensity();
            
            switch (triggerConfig.effect()) {
                case PULSE -> {
                    // Scale up then down
                    float pulsePhase = (float) Math.sin(progress * Math.PI);
                    triggerScale = 1.0f + (pulsePhase * intensity * 0.3f);
                    matrices.scale(triggerScale, triggerScale, triggerScale);
                }
                case SHAKE -> {
                    // Random jitter
                    float shakeAmt = intensity * 0.1f * (1.0f - progress);
                    float dx = (float) ((Math.random() - 0.5) * 2 * shakeAmt);
                    float dy = (float) ((Math.random() - 0.5) * 2 * shakeAmt);
                    float dz = (float) ((Math.random() - 0.5) * 2 * shakeAmt);
                    matrices.translate(dx, dy, dz);
                }
                case FLASH, GLOW -> {
                    // Flash/glow fades out
                    triggerAlpha = Math.min(1.0f, intensity * (1.0f - progress) + 0.5f);
                }
                case COLOR_SHIFT -> {
                    // Color shift handled below
                }
            }
        }
        
        // Get render layer and apply blend mode
        String blendMode = state.getLayerBlendMode(0); // Use first layer's blend mode for now
        RenderLayer layer = FieldRenderLayers.solidTranslucent();
        VertexConsumer consumer = consumers.getBuffer(layer);
        
        // Apply blend mode (will be reset after rendering)
        FieldRenderLayers.applyBlendMode(blendMode);
        
        // Get color (try primaryColor first, fall back to color for compatibility)
        int color = state.getInt("appearance.primaryColor");
        if (color == 0) {
            color = state.getInt("appearance.color");
        }
        if (color == 0) {
            color = 0x00FFFF; // Default cyan if no color set
        }
        float alpha = state.getFloat("appearance.alpha") * triggerAlpha;
        if (alpha <= 0) alpha = 0.8f; // Default alpha if not set
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int)(alpha * 255);
        
        // Render based on shape type - each shape uses its own radius field
        String shapeType = state.getString("shapeType").toLowerCase();
        float fallbackRadius = state.getFloat("radius");
        if (fallbackRadius <= 0) fallbackRadius = 1.0f;

        switch (shapeType) {
            case "sphere" -> {
                float r2 = state.getFloat("sphere.radius");
                if (r2 <= 0) r2 = fallbackRadius;
                renderSphere(matrices, consumer, r2, r, g, b, a, state);
            }
            case "ring" -> {
                // Ring uses innerRadius/outerRadius, not a single radius
                renderRing(matrices, consumer, fallbackRadius, r, g, b, a, state);
            }
            case "disc" -> {
                float r2 = state.getFloat("disc.radius");
                if (r2 <= 0) r2 = fallbackRadius;
                renderDisc(matrices, consumer, r2, r, g, b, a, state);
            }
            case "cylinder" -> renderCylinder(matrices, consumer, r, g, b, a, state);
            case "prism" -> {
                float r2 = state.getFloat("prism.radius");
                if (r2 <= 0) r2 = fallbackRadius;
                renderPrism(matrices, consumer, r2, r, g, b, a, state);
            }
            default -> renderSphere(matrices, consumer, fallbackRadius, r, g, b, a, state);
        }
        
        // Reset blend mode
        FieldRenderLayers.resetBlendMode();
        
        matrices.pop();
    }
    
    /**
     * Renders a sphere shape using the selected algorithm.
     * 
     * <p>Supports all 5 algorithms:
     * <ul>
     *   <li>LAT_LON: Inline lat/lon tessellation</li>
     *   <li>UV_SPHERE, ICO_SPHERE: Mesh-based tessellation via SphereTessellator</li>
     *   <li>TYPE_A: Direct overlapping cubes (accurate)</li>
     *   <li>TYPE_E: Direct rotated rectangles (efficient)</li>
     * </ul>
     */
    private static void renderSphere(
            MatrixStack matrices,
            VertexConsumer consumer,
            float radius,
            int r, int g, int b, int a,
            FieldEditState state) {
        
        // Get algorithm from state
        String algoStr = state.getString("sphere.algorithm");
        SphereAlgorithm algorithm;
        try {
            algorithm = SphereAlgorithm.valueOf(algoStr);
        } catch (IllegalArgumentException e) {
            algorithm = SphereAlgorithm.LAT_LON;
        }
        
        int latSteps = state.getInt("sphere.latSteps");
        int lonSteps = state.getInt("sphere.lonSteps");
        
        // Pack color as ARGB int
        int color = (a << 24) | (r << 16) | (g << 8) | b;
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        
        // Dispatch based on algorithm
        switch (algorithm) {
            case TYPE_A -> {
                // Direct rendering with overlapping cubes
                SphereRenderer.renderTypeA(matrices, consumer, radius, color, light);
            }
            case TYPE_E -> {
                // Direct rendering with rotated rectangles
                SphereRenderer.renderTypeE(matrices, consumer, radius, color, light);
            }
            case UV_SPHERE, ICO_SPHERE -> {
                // Proper mesh-based tessellation with mask support
                var shape = net.cyberpunk042.visual.shape.SphereShape.builder()
                    .radius(radius)
                    .latSteps(latSteps)
                    .lonSteps(lonSteps)
                    .algorithm(algorithm)
                    .build();
                var mask = state.mask();
                var mesh = net.cyberpunk042.client.visual.mesh.SphereTessellator.tessellate(shape, null, mask);
                renderMesh(matrices, consumer, mesh, r, g, b, a);
            }
            default -> {
                // LAT_LON: Use inline rendering with mask support
                var mask = state.mask();
                renderSphereLatLon(matrices, consumer, radius, r, g, b, a, latSteps, lonSteps, mask);
            }
        }
    }
    
    /**
     * Renders a tessellated mesh.
     */
    private static void renderMesh(
            MatrixStack matrices,
            VertexConsumer consumer,
            net.cyberpunk042.client.visual.mesh.Mesh mesh,
            int r, int g, int b, int a) {
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        mesh.forEachTriangle((v0, v1, v2) -> {
            vertexWithNormal(consumer, matrix, v0.x(), v0.y(), v0.z(), r, g, b, a, v0.nx(), v0.ny(), v0.nz());
            vertexWithNormal(consumer, matrix, v1.x(), v1.y(), v1.z(), r, g, b, a, v1.nx(), v1.ny(), v1.nz());
            vertexWithNormal(consumer, matrix, v2.x(), v2.y(), v2.z(), r, g, b, a, v2.nx(), v2.ny(), v2.nz());
        });
    }
    
    /**
     * Renders sphere using lat/lon tessellation (inline for preview).
     * Applies visibility mask if provided.
     */
    private static void renderSphereLatLon(
            MatrixStack matrices,
            VertexConsumer consumer,
            float radius,
            int r, int g, int b, int a,
            int latSteps, int lonSteps,
            net.cyberpunk042.visual.visibility.VisibilityMask mask) {
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        for (int lat = 0; lat < latSteps; lat++) {
            float latFrac = (float) lat / latSteps;  // For mask UV lookup
            float lat0 = (float) Math.PI * (-0.5f + latFrac);
            float lat1 = (float) Math.PI * (-0.5f + (float) (lat + 1) / latSteps);
            
            float y0 = MathHelper.sin(lat0) * radius;
            float y1 = MathHelper.sin(lat1) * radius;
            float r0 = MathHelper.cos(lat0) * radius;
            float r1 = MathHelper.cos(lat1) * radius;
            
            for (int lon = 0; lon < lonSteps; lon++) {
                float lonFrac = (float) lon / lonSteps;  // For mask UV lookup
                
                // Check visibility mask - skip if not visible
                if (mask != null && mask.hasMask() && !mask.isVisible(lonFrac, latFrac)) {
                    continue;
                }
                
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
                
                // Triangle 1 - with proper outward normals
                sphereVertex(consumer, matrix, x00, y0, z00, radius, r, g, b, a);
                sphereVertex(consumer, matrix, x10, y0, z10, radius, r, g, b, a);
                sphereVertex(consumer, matrix, x01, y1, z01, radius, r, g, b, a);
                
                // Triangle 2 - with proper outward normals
                sphereVertex(consumer, matrix, x10, y0, z10, radius, r, g, b, a);
                sphereVertex(consumer, matrix, x11, y1, z11, radius, r, g, b, a);
                sphereVertex(consumer, matrix, x01, y1, z01, radius, r, g, b, a);
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
        
        float innerRadius = state.getFloat("ring.innerRadius");
        float outerRadius = state.getFloat("ring.outerRadius");
        int segments = state.getInt("ring.segments");
        
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
        
        int segments = state.getInt("disc.segments");
        
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
     * Renders a cylinder shape.
     */
    private static void renderCylinder(
            MatrixStack matrices,
            VertexConsumer consumer,
            int r, int g, int b, int a,
            FieldEditState state) {

        float radius = state.getFloat("cylinder.radius");
        float height = state.getFloat("cylinder.height");
        int segments = state.getInt("cylinder.segments");
        float topRadius = state.getFloat("cylinder.topRadius");
        boolean capTop = state.getBool("cylinder.capTop");
        boolean capBottom = state.getBool("cylinder.capBottom");

        // Defaults
        if (radius <= 0) radius = 0.5f;
        if (height <= 0) height = 2.0f;
        if (segments <= 0) segments = 32;
        if (topRadius <= 0) topRadius = radius;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float halfH = height / 2;

        // Render cylinder sides
        for (int i = 0; i < segments; i++) {
            float angle0 = 2 * (float) Math.PI * i / segments;
            float angle1 = 2 * (float) Math.PI * (i + 1) / segments;

            float x0b = MathHelper.cos(angle0) * radius;
            float z0b = MathHelper.sin(angle0) * radius;
            float x1b = MathHelper.cos(angle1) * radius;
            float z1b = MathHelper.sin(angle1) * radius;

            float x0t = MathHelper.cos(angle0) * topRadius;
            float z0t = MathHelper.sin(angle0) * topRadius;
            float x1t = MathHelper.cos(angle1) * topRadius;
            float z1t = MathHelper.sin(angle1) * topRadius;

            // Side quad as two triangles
            vertex(consumer, matrix, x0b, -halfH, z0b, r, g, b, a);
            vertex(consumer, matrix, x1b, -halfH, z1b, r, g, b, a);
            vertex(consumer, matrix, x0t, halfH, z0t, r, g, b, a);

            vertex(consumer, matrix, x1b, -halfH, z1b, r, g, b, a);
            vertex(consumer, matrix, x1t, halfH, z1t, r, g, b, a);
            vertex(consumer, matrix, x0t, halfH, z0t, r, g, b, a);
        }

        // Top cap
        if (capTop && topRadius > 0) {
            for (int i = 0; i < segments; i++) {
                float angle0 = 2 * (float) Math.PI * i / segments;
                float angle1 = 2 * (float) Math.PI * (i + 1) / segments;

                float x0 = MathHelper.cos(angle0) * topRadius;
                float z0 = MathHelper.sin(angle0) * topRadius;
                float x1 = MathHelper.cos(angle1) * topRadius;
                float z1 = MathHelper.sin(angle1) * topRadius;

                vertex(consumer, matrix, 0, halfH, 0, r, g, b, a);
                vertex(consumer, matrix, x0, halfH, z0, r, g, b, a);
                vertex(consumer, matrix, x1, halfH, z1, r, g, b, a);
            }
        }

        // Bottom cap
        if (capBottom) {
            for (int i = 0; i < segments; i++) {
                float angle0 = 2 * (float) Math.PI * i / segments;
                float angle1 = 2 * (float) Math.PI * (i + 1) / segments;

                float x0 = MathHelper.cos(angle0) * radius;
                float z0 = MathHelper.sin(angle0) * radius;
                float x1 = MathHelper.cos(angle1) * radius;
                float z1 = MathHelper.sin(angle1) * radius;

                vertex(consumer, matrix, 0, -halfH, 0, r, g, b, a);
                vertex(consumer, matrix, x1, -halfH, z1, r, g, b, a);
                vertex(consumer, matrix, x0, -halfH, z0, r, g, b, a);
            }
        }
    }

    /**
     * Renders a prism shape (simple triangular prism for now).
     */
    private static void renderPrism(
            MatrixStack matrices,
            VertexConsumer consumer,
            float radius,
            int r, int g, int b, int a,
            FieldEditState state) {

        int sides = state.getInt("prism.sides");
        float height = state.getFloat("prism.height");

        if (sides < 3) sides = 3;
        if (height <= 0) height = 2.0f;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float halfH = height / 2;

        // Render prism sides
        for (int i = 0; i < sides; i++) {
            float angle0 = 2 * (float) Math.PI * i / sides;
            float angle1 = 2 * (float) Math.PI * (i + 1) / sides;

            float x0 = MathHelper.cos(angle0) * radius;
            float z0 = MathHelper.sin(angle0) * radius;
            float x1 = MathHelper.cos(angle1) * radius;
            float z1 = MathHelper.sin(angle1) * radius;

            // Side quad as two triangles
            vertex(consumer, matrix, x0, -halfH, z0, r, g, b, a);
            vertex(consumer, matrix, x1, -halfH, z1, r, g, b, a);
            vertex(consumer, matrix, x0, halfH, z0, r, g, b, a);

            vertex(consumer, matrix, x1, -halfH, z1, r, g, b, a);
            vertex(consumer, matrix, x1, halfH, z1, r, g, b, a);
            vertex(consumer, matrix, x0, halfH, z0, r, g, b, a);
        }

        // Top cap
        for (int i = 0; i < sides; i++) {
            float angle0 = 2 * (float) Math.PI * i / sides;
            float angle1 = 2 * (float) Math.PI * (i + 1) / sides;

            float x0 = MathHelper.cos(angle0) * radius;
            float z0 = MathHelper.sin(angle0) * radius;
            float x1 = MathHelper.cos(angle1) * radius;
            float z1 = MathHelper.sin(angle1) * radius;

            vertex(consumer, matrix, 0, halfH, 0, r, g, b, a);
            vertex(consumer, matrix, x0, halfH, z0, r, g, b, a);
            vertex(consumer, matrix, x1, halfH, z1, r, g, b, a);
        }

        // Bottom cap
        for (int i = 0; i < sides; i++) {
            float angle0 = 2 * (float) Math.PI * i / sides;
            float angle1 = 2 * (float) Math.PI * (i + 1) / sides;

            float x0 = MathHelper.cos(angle0) * radius;
            float z0 = MathHelper.sin(angle0) * radius;
            float x1 = MathHelper.cos(angle1) * radius;
            float z1 = MathHelper.sin(angle1) * radius;

            vertex(consumer, matrix, 0, -halfH, 0, r, g, b, a);
            vertex(consumer, matrix, x1, -halfH, z1, r, g, b, a);
            vertex(consumer, matrix, x0, -halfH, z0, r, g, b, a);
        }
    }

    /**
     * Helper to emit a vertex with default normal.
     */
    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
                               float x, float y, float z, int r, int g, int b, int a) {
        vertexWithNormal(consumer, matrix, x, y, z, r, g, b, a, 0, 1, 0);
    }
    
    /**
     * Helper to emit a vertex with explicit normal.
     */
    private static void vertexWithNormal(VertexConsumer consumer, Matrix4f matrix,
                               float x, float y, float z, int r, int g, int b, int a,
                               float nx, float ny, float nz) {
        consumer.vertex(matrix, x, y, z)
            .color(r, g, b, a)
            .normal(nx, ny, nz)
            .texture(0, 0)
            .overlay(0)
            .light(15728880); // Full bright
    }
    
    /**
     * Helper to emit a sphere vertex with outward-facing normal.
     */
    private static void sphereVertex(VertexConsumer consumer, Matrix4f matrix,
                               float x, float y, float z, float radius,
                               int r, int g, int b, int a) {
        // Normal points outward from center (normalize the position)
        float invLen = 1.0f / radius;
        vertexWithNormal(consumer, matrix, x, y, z, r, g, b, a, 
                        x * invLen, y * invLen, z * invLen);
    }
}

