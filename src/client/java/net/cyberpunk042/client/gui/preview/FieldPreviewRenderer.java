package net.cyberpunk042.client.gui.preview;

import net.cyberpunk042.client.gui.preview.tessellator.PreviewSphereTessellator;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.visual.fill.FillMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * Helper for rendering field previews in GUI screens.
 * 
 * <p>Now supports fill-mode aware rendering using tessellation and rasterization
 * for solid shapes. Falls back to wireframe for unsupported shapes.</p>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // In your screen's render method:
 * FieldPreviewRenderer.drawField(context, state, x1, y1, x2, y2);
 * </pre>
 */
@Environment(EnvType.CLIENT)
public class FieldPreviewRenderer {
    
    // Simple 3D to 2D projection parameters
    private static final float PERSPECTIVE = 0.4f;  // Perspective strength
    
    /**
     * Draws a field preview using 2D projected wireframe.
     */
    public static void drawField(DrawContext context, FieldEditState state,
                                 int x1, int y1, int x2, int y2) {
        // Time in ticks (20 ticks/sec simulation)
        float timeTicks = (System.currentTimeMillis() % 100000) / 50f;  // ~20 per second
        
        // === SPIN ANIMATION ===
        float rotationY = 0f;
        try {
            var spin = state.spin();
            if (spin != null && spin.isActive()) {
                float speed = spin.speed();
                // Speed is in radians per tick, convert to degrees
                rotationY = (timeTicks * speed * 57.3f) % 360f;  // 57.3 = 180/PI
            }
        } catch (Exception ignored) {}
        
        // === WOBBLE ANIMATION ===
        float rotationX = 25f;  // Base tilt
        try {
            var wobble = state.wobble();
            if (wobble != null && wobble.isActive()) {
                var amp = wobble.amplitude();
                if (amp != null) {
                    float speed = wobble.speed();
                    // Add oscillating offset to rotation
                    rotationX += (float) Math.sin(timeTicks * speed * 0.1f) * amp.x() * 100f;
                    rotationY += (float) Math.cos(timeTicks * speed * 0.1f) * amp.z() * 100f;
                }
            }
        } catch (Exception ignored) {}
        
        // === PULSE/BREATHING ANIMATION ===
        float scale = 1.0f;
        try {
            var pulse = state.pulse();
            if (pulse != null && pulse.isActive()) {
                scale = pulse.evaluate(timeTicks);
            }
        } catch (Exception ignored) {}
        
        drawField(context, state, x1, y1, x2, y2, scale, rotationX, rotationY);
    }
    
    /**
     * Draws a field preview with specified rotation.
     */
    public static void drawField(DrawContext context, FieldEditState state,
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
