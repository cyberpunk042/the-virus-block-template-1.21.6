package net.cyberpunk042.client.gui.preview;

import com.mojang.blaze3d.textures.GpuTexture;
import net.cyberpunk042.client.field.render.FieldRenderer;
import net.cyberpunk042.client.gui.preview.tessellator.PreviewSphereTessellator;
import net.cyberpunk042.client.gui.state.DefinitionBuilder;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.visual.fill.FillMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Helper for rendering field previews in GUI screens.
 * 
 * <p>Supports two rendering modes:
 * <ul>
 *   <li><b>Fast</b> - 2D rasterization with tessellation (default)</li>
 *   <li><b>Full</b> - Real FieldRenderer via offscreen framebuffer</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // In your screen's render method:
 * FieldPreviewRenderer.drawField(context, state, x1, y1, x2, y2, useFullRenderer);
 * </pre>
 */
@Environment(EnvType.CLIENT)
public class FieldPreviewRenderer {
    
    // Simple 3D to 2D projection parameters
    private static final float PERSPECTIVE = 0.4f;  // Perspective strength
    
    // Cached framebuffer for full preview mode
    private static PreviewFramebuffer previewFramebuffer = null;
    
    // Debug: only log once
    private static boolean debugLogged = false;
    
    /**
     * Main entry point for field preview rendering.
     * 
     * <p>Uses the full 3D renderer via framebuffer for best visual quality and performance.
     * 
     * @param useFullRenderer Deprecated - ignored, always uses full 3D rendering.
     */
    public static void drawField(DrawContext context, FieldEditState state,
                                 int x1, int y1, int x2, int y2,
                                 boolean useFullRenderer) {
        // Time in ticks (20 ticks/sec simulation)
        float timeTicks = (System.currentTimeMillis() % 100000) / 50f;  // ~20 per second
        
        // === SPIN ANIMATION ===
        float rotationY = 0f;
        var spin = state.spin();
        if (spin != null && spin.isActive()) {
            float speed = spin.speed();
            rotationY = (timeTicks * speed * 57.3f) % 360f;  // 57.3 = 180/PI
        }
        
        // === WOBBLE ANIMATION ===
        float rotationX = 25f;  // Base tilt
        var wobble = state.wobble();
        if (wobble != null && wobble.isActive()) {
            var amp = wobble.amplitude();
            if (amp != null) {
                float speed = wobble.speed();
                rotationX += (float) Math.sin(timeTicks * speed * 0.1f) * amp.x() * 100f;
                rotationY += (float) Math.cos(timeTicks * speed * 0.1f) * amp.z() * 100f;
            }
        }
        
        // === PULSE/BREATHING ANIMATION ===
        float scale = 1.0f;
        var pulse = state.pulse();
        if (pulse != null && pulse.isActive()) {
            scale = pulse.evaluate(timeTicks);
        }
        
        // Always use full 3D renderer - it has better performance and visual quality
        drawFieldFull(context, state, x1, y1, x2, y2, scale, rotationX, rotationY, timeTicks);
    }
    
    /**
     * Draws a field preview using 2D rasterization (backward compatible).
     */
    public static void drawField(DrawContext context, FieldEditState state,
                                 int x1, int y1, int x2, int y2) {
        drawField(context, state, x1, y1, x2, y2, false);
    }
    
    /**
     * Disposes the cached framebuffer. Call when closing the screen.
     */
    public static void disposeFramebuffer() {
        if (previewFramebuffer != null) {
            previewFramebuffer.close();
            previewFramebuffer = null;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FULL PREVIEW (Real FieldRenderer via Framebuffer)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Draws field using real FieldRenderer to an offscreen framebuffer.
     * This provides 100% visual fidelity with world rendering.
     */
    private static void drawFieldFull(DrawContext context, FieldEditState state,
                                       int x1, int y1, int x2, int y2,
                                       float scale, float rotationX, float rotationY,
                                       float timeTicks) {
        // Calculate preview dimensions - use 4x resolution for better quality
        int width = x2 - x1;
        int height = y2 - y1;
        if (width <= 0 || height <= 0) return;
        
        int fboScale = 4;  // Render at 4x resolution for sharper preview
        int fboWidth = width * fboScale;
        int fboHeight = height * fboScale;
        
        // Ensure framebuffer exists and is correct size
        if (previewFramebuffer == null) {
            previewFramebuffer = new PreviewFramebuffer();
        }
        previewFramebuffer.ensureSize(fboWidth, fboHeight);
        
        var framebuffer = previewFramebuffer.getFramebuffer();
        if (framebuffer == null) {
            context.fill(x1, y1, x2, y2, 0xFFFF0000);  // Red = no framebuffer
            return;
        }
        
        // Build definition from current state
        FieldDefinition definition = DefinitionBuilder.fromState(state);
        if (definition == null || definition.layers() == null || definition.layers().isEmpty()) {
            // Fall back to fast preview if no valid definition
            drawFieldFast(context, state, x1, y1, x2, y2, scale, rotationX, rotationY);
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        
        // ═══════════════════════════════════════════════════════════════════════════
        // SAVE CURRENT STATE
        // ═══════════════════════════════════════════════════════════════════════════
        
        // Save current viewport
        int[] savedViewport = new int[4];
        org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_VIEWPORT, savedViewport);
        
        // ═══════════════════════════════════════════════════════════════════════════
        // SETUP FBO STATE
        // ═══════════════════════════════════════════════════════════════════════════
        
        // Prepare and bind framebuffer
        previewFramebuffer.prepare();
        try {
            var bindMethod = framebuffer.getClass().getMethod("iris$bindFramebuffer");
            bindMethod.invoke(framebuffer);
            if (!debugLogged) System.out.println("[FieldPreviewRenderer] Bound preview FBO");
        } catch (Exception e) {
            if (!debugLogged) System.out.println("[FieldPreviewRenderer] Failed to bind FBO: " + e);
        }
        
        // Set viewport to FBO dimensions (fboWidth/fboHeight defined earlier)
        org.lwjgl.opengl.GL11.glViewport(0, 0, fboWidth, fboHeight);
        
        // Clear FBO with OPAQUE dark background
        // Must disable blending for clear to work as expected
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glClearColor(0.12f, 0.12f, 0.18f, 1.0f);  // Opaque dark blue-gray
        org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT | org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
        
        // ═══════════════════════════════════════════════════════════════════════════
        // SETUP VIEW TRANSFORM
        // ═══════════════════════════════════════════════════════════════════════════
        
        // Use FIXED camera distance so shape size changes with radius
        // Distance of 2.5 = 4x zoom compared to 10.0
        float fixedCameraDistance = 2.5f;  // Zoomed in for better visibility
        
        // Account for aspect ratio (narrower preview = need more distance)
        float aspect = (float) fboWidth / (float) fboHeight;
        float aspectFactor = aspect < 1.0f ? 1.0f / aspect : 1.0f;
        float cameraDistance = fixedCameraDistance * aspectFactor;
        
        // ═══════════════════════════════════════════════════════════════════════════
        // RENDER OPAQUE BACKGROUND QUAD (STATIC - no rotation)
        // ═══════════════════════════════════════════════════════════════════════════
        
        // Create a separate matrix stack for the background (only camera distance, no rotation)
        MatrixStack bgMatrices = new MatrixStack();
        bgMatrices.push();
        bgMatrices.translate(0, 0, -cameraDistance);  // Only camera distance, no rotation!
        
        // Get vertex buffer provider for background
        VertexConsumerProvider.Immediate bgImmediate = client.getBufferBuilders().getEntityVertexConsumers();
        
        // Use a solid render layer for the background
        var solidLayer = net.minecraft.client.render.RenderLayer.getEntityTranslucent(
            net.minecraft.util.Identifier.of("minecraft", "textures/misc/white.png")
        );
        var bgConsumer = bgImmediate.getBuffer(solidLayer);
        
        // Draw a large quad behind the shape
        float bgSize = 50f;
        float bgZ = -5f;  // Behind the shape (in model space)
        int bgR = 0x1E, bgG = 0x1E, bgB = 0x2E, bgA = 0xFF;
        int maxLight = net.minecraft.client.render.LightmapTextureManager.MAX_LIGHT_COORDINATE;
        
        // Get the static matrix entry (no rotation)
        var bgEntry = bgMatrices.peek();
        var bgMatrix = bgEntry.getPositionMatrix();
        
        // Emit 4 vertices for the quad with ALL required elements
        bgConsumer.vertex(bgMatrix, -bgSize, -bgSize, bgZ)
            .color(bgR, bgG, bgB, bgA)
            .texture(0f, 0f)
            .overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
            .light(maxLight)
            .normal(bgEntry, 0f, 0f, 1f);
        bgConsumer.vertex(bgMatrix, -bgSize,  bgSize, bgZ)
            .color(bgR, bgG, bgB, bgA)
            .texture(0f, 1f)
            .overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
            .light(maxLight)
            .normal(bgEntry, 0f, 0f, 1f);
        bgConsumer.vertex(bgMatrix,  bgSize,  bgSize, bgZ)
            .color(bgR, bgG, bgB, bgA)
            .texture(1f, 1f)
            .overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
            .light(maxLight)
            .normal(bgEntry, 0f, 0f, 1f);
        bgConsumer.vertex(bgMatrix,  bgSize, -bgSize, bgZ)
            .color(bgR, bgG, bgB, bgA)
            .texture(1f, 0f)
            .overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
            .light(maxLight)
            .normal(bgEntry, 0f, 0f, 1f);
        
        // Flush background
        bgImmediate.draw();
        bgMatrices.pop();
        
        // ═══════════════════════════════════════════════════════════════════════════
        // SETUP SHAPE MATRIX (with rotation for the spinning shape)
        // ═══════════════════════════════════════════════════════════════════════════
        
        MatrixStack matrices = new MatrixStack();
        matrices.push();
        
        // View transform: camera looking at origin from fixed distance
        matrices.translate(0, 0, -cameraDistance);
        
        // Apply rotations (for spinning shape)
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotationX));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationY));
        
        // Apply user scale
        matrices.scale(scale, scale, scale);
        
        // ═══════════════════════════════════════════════════════════════════════════
        // RENDER THE FIELD
        // ═══════════════════════════════════════════════════════════════════════════
        
        // Get vertex buffer provider
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        
        // Get alpha
        float alpha = 1.0f;
        var appearance = state.appearance();
        if (appearance != null) {
            alpha = appearance.alphaMin();
        }
        
        // Render field at origin
        FieldRenderer.render(
            matrices,
            immediate,
            definition,
            Vec3d.ZERO,
            1.0f,
            timeTicks,
            alpha
        );
        
        // Flush vertex buffers
        immediate.draw();
        matrices.pop();
        
        if (!debugLogged) {
            System.out.println("[FieldPreviewRenderer] Rendered field to FBO");
        }
        
        // ═══════════════════════════════════════════════════════════════════════════
        // CAPTURE FBO CONTENT
        // ═══════════════════════════════════════════════════════════════════════════
        
        PreviewDynamicTexture.getInstance().captureFromBoundFbo(fboWidth, fboHeight);
        
        // ═══════════════════════════════════════════════════════════════════════════
        // RESTORE PREVIOUS STATE
        // ═══════════════════════════════════════════════════════════════════════════
        
        // Unbind FBO (bind back to main screen)
        try {
            var mainFb = client.getFramebuffer();
            var unbindMethod = mainFb.getClass().getMethod("iris$bindFramebuffer");
            unbindMethod.invoke(mainFb);
        } catch (Exception e) {
            // Fallback: bind FBO 0
            org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER, 0);
        }
        
        // Restore viewport
        org.lwjgl.opengl.GL11.glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
        
        if (!debugLogged) {
            System.out.println("[FieldPreviewRenderer] State restored, drawing to GUI");
            debugLogged = true;
        }
        
        // ═══════════════════════════════════════════════════════════════════════════
        // DRAW CAPTURED TEXTURE TO GUI
        // ═══════════════════════════════════════════════════════════════════════════
        
        drawFramebufferToGui(context, framebuffer, x1, y1, x2, y2);
    }
    
    /**
     * Draws a Framebuffer to the GUI at the specified bounds.
     * Uses the captured texture from PreviewDynamicTexture.
     */
    private static void drawFramebufferToGui(DrawContext context, net.minecraft.client.gl.Framebuffer framebuffer, 
                                              int x1, int y1, int x2, int y2) {
        PreviewDynamicTexture dynamicTexture = PreviewDynamicTexture.getInstance();
        
        if (!dynamicTexture.isReady()) {
            context.fill(x1, y1, x2, y2, 0xFFFF0000);  // Red = texture not ready
            if (!debugLogged) {
                System.out.println("[FieldPreviewRenderer] Dynamic texture not ready");
            }
            return;
        }
        
        try {
            int width = x2 - x1;
            int height = y2 - y1;
            
            // Draw opaque background first (prevents world showing through)
            context.fill(x1, y1, x2, y2, 0xFF1E1E2E);  // Dark background matching FBO clear color
            
            // Draw using DrawContext with the captured texture
            context.drawTexture(
                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                PreviewDynamicTexture.getTextureId(),
                x1, y1,
                0.0f, 0.0f,  // u, v offset
                width, height,
                width, height  // texture dimensions (match display size)
            );
            
            if (!debugLogged) {
                System.out.println("[FieldPreviewRenderer] Texture draw call made!");
                debugLogged = true;
            }
            
        } catch (Exception e) {
            if (!debugLogged) {
                System.out.println("[FieldPreviewRenderer] Error: " + e.getMessage());
                e.printStackTrace();
                debugLogged = true;
            }
            context.fill(x1, y1, x2, y2, 0xFFFF8800);  // Orange = error
        }
    }
    
    /**
     * Attempts to extract the OpenGL texture ID from a GpuTexture using reflection.
     * Returns -1 if unable to get the ID.
     */
    private static int getGlTextureId(GpuTexture texture) {
        // Debug: Log the actual class name (only once)
        if (!debugLogged) {
            debugLogged = true;
            String className = texture.getClass().getName();
            System.out.println("[FieldPreviewRenderer] GpuTexture class: " + className);
            
            // Debug: List all methods
            System.out.println("[FieldPreviewRenderer] Methods:");
            for (var method : texture.getClass().getMethods()) {
                if (method.getParameterCount() == 0 && !method.getName().startsWith("wait") 
                    && !method.getName().equals("toString") && !method.getName().equals("hashCode")
                    && !method.getName().equals("getClass") && !method.getName().equals("notify")
                    && !method.getName().equals("notifyAll")) {
                    System.out.println("  - " + method.getName() + "() -> " + method.getReturnType().getSimpleName());
                }
            }
        }
        
        // Try method_68427 (obfuscated name for glId in 1.21)
        try {
            var method = texture.getClass().getMethod("method_68427");
            Object result = method.invoke(texture);
            if (result instanceof Integer id && id > 0) {
                if (!debugLogged) System.out.println("[FieldPreviewRenderer] method_68427() returned: " + id);
                return id;
            }
        } catch (Exception e) {
            // Method not found or failed
        }
        
        // Try iris$getGlId (Iris shaders compatibility)
        try {
            var method = texture.getClass().getMethod("iris$getGlId");
            Object result = method.invoke(texture);
            if (result instanceof Integer id && id > 0) {
                if (!debugLogged) System.out.println("[FieldPreviewRenderer] iris$getGlId() returned: " + id);
                return id;
            }
        } catch (Exception e) {
            // Method not found or failed
        }
        
        // Try glId (deobfuscated name, in case yarn mappings work)
        try {
            var method = texture.getClass().getMethod("glId");
            Object result = method.invoke(texture);
            if (result instanceof Integer id && id > 0) {
                if (!debugLogged) System.out.println("[FieldPreviewRenderer] glId() returned: " + id);
                return id;
            }
        } catch (Exception e) {
            // Method not found
        }
        
        if (!debugLogged) System.out.println("[FieldPreviewRenderer] Could not get GL texture ID");
        return -1;
    }
    
    /**
     * Draws a textured quad using the given texture ID.
     * 
     * NOTE: In 1.21, the rendering pipeline is complex and requires proper shader setup.
     * For now, we just log success. The actual texture display needs more research.
     * 
     * TODO: Research proper way to draw a raw GL texture in 1.21:
     * - Maybe use Framebuffer.blitToScreen() with scissor
     * - Or create a custom RenderPipeline/RenderPass
     * - Or register the texture with TextureManager
     */
    private static void drawTexturedQuad(DrawContext context, int x1, int y1, int x2, int y2, int textureId) {
        // Log success (once)
        if (!debugLogged) {
            System.out.println("[FieldPreviewRenderer] SUCCESS! Got GL texture ID: " + textureId);
            System.out.println("[FieldPreviewRenderer] Note: Actual texture rendering not yet implemented.");
        }
        
        // TODO: Use the textureId to actually render the framebuffer content
        // For now, the caller draws a green rectangle to show success
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FAST PREVIEW (2D Rasterization)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Draws a field preview with fast 2D rasterization.
     */
    private static void drawFieldFast(DrawContext context, FieldEditState state,
                                       int x1, int y1, int x2, int y2,
                                       float scale, float rotationX, float rotationY) {
        
        // Enable scissor for bounds
        context.enableScissor(x1, y1, x2, y2);
        
        // Calculate center and scale
        float centerX = (x1 + x2) / 2f;
        float centerY = (y1 + y2) / 2f;
        float boundsSize = Math.min(x2 - x1, y2 - y1) - 20;
        
        // Get field parameters - get radius from current shape (default to sphere.radius)
        String shapeType = state.getString("shapeType");
        if (shapeType == null || shapeType.isEmpty()) shapeType = "sphere";
        float radius = getRadiusForShape(state, shapeType);
        if (radius <= 0) radius = 3f;
        float finalScale = (boundsSize / (radius * 2.5f)) * scale;
        
        // Get fill mode
        FillMode mode = FillMode.SOLID;
        try {
            mode = state.fill().mode();
        } catch (Exception ignored) {}
        
        // Time for alpha pulse
        float timeTicks = (System.currentTimeMillis() % 100000) / 50f;
        
        // Get color from state with alpha
        // Note: UI controls 'primaryColor', not 'color' - use primaryColor!
        int color = 0xFF00CCFF;  // Default cyan with full alpha
        try {
            var appearance = state.appearance();
            if (appearance != null) {
                color = appearance.primaryColor();  // Use primaryColor, not color()!
                // Apply base alpha (alphaMin is the minimum opacity, matching world rendering)
                float alpha = appearance.alphaMin();
                
                // === ALPHA PULSE ANIMATION ===
                try {
                    var alphaPulse = state.alphaPulse();
                    if (alphaPulse != null && alphaPulse.isActive()) {
                        // Alpha pulse modulates the alpha value
                        float pulseAlpha = alphaPulse.evaluate(timeTicks);
                        alpha *= pulseAlpha;
                    }
                } catch (Exception ignored) {}
                
                color = PreviewRasterizer.applyAlpha(color, alpha);
            }
        } catch (Exception e) {
            // Keep default color on error
        }
        
        // Rendering based on shape and fill mode
        float rotX = (float) Math.toRadians(rotationX);
        float rotY = (float) Math.toRadians(rotationY);
        
        if ("sphere".equalsIgnoreCase(shapeType)) {
            switch (mode) {
                case SOLID -> {
                    // SOLID: use tessellation with filled triangles, detail from slider
                    PreviewProjector projector = new PreviewProjector(
                        centerX, centerY, finalScale, rotationX, rotationY);
                    int detail = PreviewConfig.getDetail();
                    List<PreviewTriangle> triangles = PreviewSphereTessellator.getInstance()
                        .tessellate(projector, state, color, detail);
                    PreviewRasterizer.render(context, triangles, mode, color);
                }
                case WIREFRAME -> {
                    // WIREFRAME: use tessellation with edges, detail-1 for better performance
                    PreviewProjector projector = new PreviewProjector(
                        centerX, centerY, finalScale, rotationX, rotationY);
                    int wireframeDetail = Math.max(1, PreviewConfig.getDetail() - 1);
                    List<PreviewTriangle> triangles = PreviewSphereTessellator.getInstance()
                        .tessellate(projector, state, color, wireframeDetail);
                    PreviewRasterizer.render(context, triangles, mode, color);
                }
                case CAGE -> {
                    // CAGE: use legacy lat/lon lines (sparse, clean look)
                    renderSphereWireframe(context, centerX, centerY, radius, finalScale, rotX, rotY, color, state, false);
                }
                case POINTS -> {
                    // POINTS: use tessellation for vertex positions
                    PreviewProjector projector = new PreviewProjector(
                        centerX, centerY, finalScale, rotationX, rotationY);
                    int detail = Math.min(8, PreviewConfig.getDetail());  // Cap detail for points
                    List<PreviewTriangle> triangles = PreviewSphereTessellator.getInstance()
                        .tessellate(projector, state, color, detail);
                    PreviewRasterizer.render(context, triangles, mode, color);
                }
            }
        } else {
            // Non-sphere shapes: use legacy wireframe
            switch (shapeType.toLowerCase()) {
                case "ring" -> renderRingWireframe(context, centerX, centerY, finalScale, rotX, rotY, color, state);
                case "disc" -> renderDiscWireframe(context, centerX, centerY, finalScale, rotX, rotY, color, state);
                case "cylinder", "beam" -> renderCylinderWireframe(context, centerX, centerY, radius, finalScale, rotX, rotY, color, state);
                case "prism" -> renderPrismWireframe(context, centerX, centerY, finalScale, rotX, rotY, color, state);
                default -> renderSphereWireframe(context, centerX, centerY, radius, finalScale, rotX, rotY, color, state, false);
            }
        }
        
        context.disableScissor();
    }
    
    /**
     * Gets the appropriate radius for a shape type.
     */
    private static float getRadiusForShape(FieldEditState state, String shapeType) {
        return switch (shapeType.toLowerCase()) {
            case "sphere" -> state.getFloat("sphere.radius");
            case "ring" -> state.getFloat("ring.outerRadius");
            case "disc" -> state.getFloat("disc.radius");
            case "cylinder" -> state.getFloat("cylinder.radius");
            case "prism" -> state.getFloat("prism.radius");
            case "capsule" -> state.getFloat("capsule.radius");
            case "cone" -> state.getFloat("cone.radius");
            default -> state.getFloat("sphere.radius");
        };
    }
    
    /**
     * Gets a tessellator for the given shape type, or null if not yet implemented.
     */
    private static PreviewTessellator getTessellatorFor(String shapeType) {
        return switch (shapeType.toLowerCase()) {
            case "sphere" -> PreviewSphereTessellator.getInstance();
            // Add more as implemented:
            // case "cylinder" -> PreviewCylinderTessellator.getInstance();
            // case "prism" -> PreviewPrismTessellator.getInstance();
            default -> null;  // Falls back to legacy wireframe
        };
    }

    
    // ═══════════════════════════════════════════════════════════════════════════
    // SPHERE WIREFRAME
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Renders sphere wireframe with configurable density.
     * @param dense If true, uses PreviewConfig.getDetail() for denser wireframe.
     *              If false, uses shape's lat/lon settings for sparse cage lines.
     */
    private static void renderSphereWireframe(DrawContext context, float cx, float cy,
                                              float radius, float scale, float rotX, float rotY,
                                              int color, FieldEditState state, boolean dense) {
        int latSteps, lonSteps;
        
        if (dense) {
            // Wireframe mode: use detail setting for denser lines
            int detail = PreviewConfig.getDetail();
            latSteps = detail;
            lonSteps = detail * 2;
        } else {
            // Cage mode: use sparse lines from shape settings
            latSteps = Math.min(16, Math.max(6, state.getInt("sphere.latSteps") / 4));
            lonSteps = Math.min(24, Math.max(8, state.getInt("sphere.lonSteps") / 4));
        }
        
        // Draw latitude lines (horizontal circles)
        for (int lat = 1; lat < latSteps; lat++) {
            float latAngle = (float) Math.PI * lat / latSteps - (float) Math.PI / 2;
            float latRadius = MathHelper.cos(latAngle) * radius;
            float latY = MathHelper.sin(latAngle) * radius;
            
            float lastX = 0, lastY = 0;
            boolean first = true;
            
            for (int lon = 0; lon <= lonSteps; lon++) {
                float lonAngle = 2 * (float) Math.PI * lon / lonSteps;
                float x = MathHelper.cos(lonAngle) * latRadius;
                float z = MathHelper.sin(lonAngle) * latRadius;
                
                // Rotate and project
                float[] projected = project3Dto2D(x, latY, z, rotX, rotY, scale);
                float screenX = cx + projected[0];
                float screenY = cy + projected[1];
                
                if (!first) {
                    drawLine(context, lastX, lastY, screenX, screenY, color);
                }
                lastX = screenX;
                lastY = screenY;
                first = false;
            }
        }
        
        // Draw longitude lines (vertical arcs)
        for (int lon = 0; lon < lonSteps / 2; lon++) {
            float lonAngle = 2 * (float) Math.PI * lon / (lonSteps / 2);
            
            float lastX = 0, lastY = 0;
            boolean first = true;
            
            for (int lat = 0; lat <= latSteps; lat++) {
                float latAngle = (float) Math.PI * lat / latSteps - (float) Math.PI / 2;
                float x = MathHelper.cos(lonAngle) * MathHelper.cos(latAngle) * radius;
                float y = MathHelper.sin(latAngle) * radius;
                float z = MathHelper.sin(lonAngle) * MathHelper.cos(latAngle) * radius;
                
                float[] projected = project3Dto2D(x, y, z, rotX, rotY, scale);
                float screenX = cx + projected[0];
                float screenY = cy + projected[1];
                
                if (!first) {
                    drawLine(context, lastX, lastY, screenX, screenY, color);
                }
                lastX = screenX;
                lastY = screenY;
                first = false;
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RING WIREFRAME
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void renderRingWireframe(DrawContext context, float cx, float cy,
                                            float scale, float rotX, float rotY,
                                            int color, FieldEditState state) {
        float innerRadius = state.getFloat("ring.innerRadius");
        float outerRadius = state.getFloat("ring.outerRadius");
        int segments = Math.min(32, Math.max(8, state.getInt("ring.segments") / 2));
        
        if (innerRadius <= 0) innerRadius = 1f;
        if (outerRadius <= 0) outerRadius = 3f;
        
        // Outer circle
        drawCircleWireframe(context, cx, cy, outerRadius, segments, scale, rotX, rotY, color);
        // Inner circle
        drawCircleWireframe(context, cx, cy, innerRadius, segments, scale, rotX, rotY, color);
        
        // Spokes
        for (int i = 0; i < 8; i++) {
            float angle = 2 * (float) Math.PI * i / 8;
            float x1 = MathHelper.cos(angle) * innerRadius;
            float z1 = MathHelper.sin(angle) * innerRadius;
            float x2 = MathHelper.cos(angle) * outerRadius;
            float z2 = MathHelper.sin(angle) * outerRadius;
            
            float[] p1 = project3Dto2D(x1, 0, z1, rotX, rotY, scale);
            float[] p2 = project3Dto2D(x2, 0, z2, rotX, rotY, scale);
            drawLine(context, cx + p1[0], cy + p1[1], cx + p2[0], cy + p2[1], color);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DISC WIREFRAME
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void renderDiscWireframe(DrawContext context, float cx, float cy,
                                            float scale, float rotX, float rotY,
                                            int color, FieldEditState state) {
        float radius = state.getFloat("disc.radius");
        if (radius <= 0) radius = 3f;
        int segments = Math.min(32, Math.max(8, state.getInt("disc.segments") / 2));
        
        // Outer circle
        drawCircleWireframe(context, cx, cy, radius, segments, scale, rotX, rotY, color);
        
        // Concentric circles
        for (int ring = 1; ring < 3; ring++) {
            float r = radius * ring / 3;
            drawCircleWireframe(context, cx, cy, r, segments / 2, scale, rotX, rotY, color);
        }
        
        // Radial lines
        for (int i = 0; i < 8; i++) {
            float angle = 2 * (float) Math.PI * i / 8;
            float x = MathHelper.cos(angle) * radius;
            float z = MathHelper.sin(angle) * radius;
            
            float[] p1 = project3Dto2D(0, 0, 0, rotX, rotY, scale);
            float[] p2 = project3Dto2D(x, 0, z, rotX, rotY, scale);
            drawLine(context, cx + p1[0], cy + p1[1], cx + p2[0], cy + p2[1], color);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CYLINDER WIREFRAME
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void renderCylinderWireframe(DrawContext context, float cx, float cy,
                                                float radius, float scale, float rotX, float rotY,
                                                int color, FieldEditState state) {
        float height = state.getFloat("cylinder.height");
        int segments = Math.min(24, Math.max(8, state.getInt("cylinder.segments") / 2));
        
        if (height <= 0) height = 4f;
        float halfH = height / 2;
        
        // Top circle
        drawCircleWireframe(context, cx, cy, radius, segments, scale, rotX, rotY, color, halfH);
        // Bottom circle
        drawCircleWireframe(context, cx, cy, radius, segments, scale, rotX, rotY, color, -halfH);
        
        // Vertical lines
        for (int i = 0; i < 8; i++) {
            float angle = 2 * (float) Math.PI * i / 8;
            float x = MathHelper.cos(angle) * radius;
            float z = MathHelper.sin(angle) * radius;
            
            float[] p1 = project3Dto2D(x, halfH, z, rotX, rotY, scale);
            float[] p2 = project3Dto2D(x, -halfH, z, rotX, rotY, scale);
            drawLine(context, cx + p1[0], cy + p1[1], cx + p2[0], cy + p2[1], color);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRISM WIREFRAME
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void renderPrismWireframe(DrawContext context, float cx, float cy,
                                             float scale, float rotX, float rotY,
                                             int color, FieldEditState state) {
        int sides = Math.max(3, state.getInt("prism.sides"));
        float radius = state.getFloat("prism.radius");
        float height = state.getFloat("prism.height");
        
        if (radius <= 0) radius = 2f;
        if (height <= 0) height = 3f;
        float halfH = height / 2;
        
        // Top polygon
        float lastTopX = 0, lastTopY = 0;
        float firstTopX = 0, firstTopY = 0;
        float lastBotX = 0, lastBotY = 0;
        float firstBotX = 0, firstBotY = 0;
        
        for (int i = 0; i <= sides; i++) {
            float angle = 2 * (float) Math.PI * i / sides;
            float x = MathHelper.cos(angle) * radius;
            float z = MathHelper.sin(angle) * radius;
            
            float[] topP = project3Dto2D(x, halfH, z, rotX, rotY, scale);
            float[] botP = project3Dto2D(x, -halfH, z, rotX, rotY, scale);
            
            float topScreenX = cx + topP[0];
            float topScreenY = cy + topP[1];
            float botScreenX = cx + botP[0];
            float botScreenY = cy + botP[1];
            
            if (i == 0) {
                firstTopX = topScreenX; firstTopY = topScreenY;
                firstBotX = botScreenX; firstBotY = botScreenY;
            } else {
                // Top edge
                drawLine(context, lastTopX, lastTopY, topScreenX, topScreenY, color);
                // Bottom edge
                drawLine(context, lastBotX, lastBotY, botScreenX, botScreenY, color);
                // Vertical edge
                drawLine(context, topScreenX, topScreenY, botScreenX, botScreenY, color);
            }
            
            lastTopX = topScreenX; lastTopY = topScreenY;
            lastBotX = botScreenX; lastBotY = botScreenY;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Projects a 3D point to 2D screen coordinates with rotation.
     */
    private static float[] project3Dto2D(float x, float y, float z, float rotX, float rotY, float scale) {
        // Rotate around Y axis (yaw)
        float cosY = MathHelper.cos(rotY);
        float sinY = MathHelper.sin(rotY);
        float x1 = x * cosY - z * sinY;
        float z1 = x * sinY + z * cosY;
        
        // Rotate around X axis (pitch)
        float cosX = MathHelper.cos(rotX);
        float sinX = MathHelper.sin(rotX);
        float y1 = y * cosX - z1 * sinX;
        float z2 = y * sinX + z1 * cosX;
        
        // Simple perspective projection
        float perspectiveFactor = 1f / (1f + z2 * PERSPECTIVE * 0.1f);
        
        return new float[] {
            x1 * scale * perspectiveFactor,
            -y1 * scale * perspectiveFactor  // Flip Y for screen coordinates
        };
    }
    
    /**
     * Draws a circle at y=height.
     */
    private static void drawCircleWireframe(DrawContext context, float cx, float cy,
                                            float radius, int segments, float scale,
                                            float rotX, float rotY, int color) {
        drawCircleWireframe(context, cx, cy, radius, segments, scale, rotX, rotY, color, 0);
    }
    
    private static void drawCircleWireframe(DrawContext context, float cx, float cy,
                                            float radius, int segments, float scale,
                                            float rotX, float rotY, int color, float y) {
        float lastX = 0, lastY = 0;
        boolean first = true;
        
        for (int i = 0; i <= segments; i++) {
            float angle = 2 * (float) Math.PI * i / segments;
            float x = MathHelper.cos(angle) * radius;
            float z = MathHelper.sin(angle) * radius;
            
            float[] projected = project3Dto2D(x, y, z, rotX, rotY, scale);
            float screenX = cx + projected[0];
            float screenY = cy + projected[1];
            
            if (!first) {
                drawLine(context, lastX, lastY, screenX, screenY, color);
            }
            lastX = screenX;
            lastY = screenY;
            first = false;
        }
    }
    
    /**
     * Draws a line using the efficient rasterizer implementation.
     */
    private static void drawLine(DrawContext context, float x1, float y1, float x2, float y2, int color) {
        PreviewRasterizer.drawLine(context, x1, y1, x2, y2, color);
    }
}
