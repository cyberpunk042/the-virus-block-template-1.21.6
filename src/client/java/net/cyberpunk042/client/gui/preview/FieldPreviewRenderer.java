package net.cyberpunk042.client.gui.preview;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/**
 * Helper for rendering field previews in GUI screens.
 * 
 * <p>Uses simple 2D projected wireframe rendering that works reliably
 * in Minecraft's GUI context without needing complex 3D setup.</p>
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
        // Calculate animated rotation
        float time = (System.currentTimeMillis() % 10000) / 10000f * 360f;
        drawField(context, state, x1, y1, x2, y2, 1.0f, 25f, time);
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
        
        // Get field parameters
        float radius = state.getFloat("radius");
        if (radius <= 0) radius = 3f;
        float finalScale = (boundsSize / (radius * 2.5f)) * scale;
        
        // Get color from state
        int color = 0xFF00CCFF;
        try {
            color = state.appearance().color();
        } catch (Exception ignored) {}
        
        // Convert rotations to radians
        float rotX = (float) Math.toRadians(rotationX);
        float rotY = (float) Math.toRadians(rotationY);
        
        // Render based on shape type
        String shapeType = state.getString("shapeType").toLowerCase();
        
        switch (shapeType) {
            case "sphere" -> renderSphereWireframe(context, centerX, centerY, radius, finalScale, rotX, rotY, color, state);
            case "ring" -> renderRingWireframe(context, centerX, centerY, finalScale, rotX, rotY, color, state);
            case "disc" -> renderDiscWireframe(context, centerX, centerY, finalScale, rotX, rotY, color, state);
            case "cylinder", "beam" -> renderCylinderWireframe(context, centerX, centerY, radius, finalScale, rotX, rotY, color, state);
            case "prism" -> renderPrismWireframe(context, centerX, centerY, finalScale, rotX, rotY, color, state);
            default -> renderSphereWireframe(context, centerX, centerY, radius, finalScale, rotX, rotY, color, state);
        }
        
        context.disableScissor();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SPHERE WIREFRAME
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void renderSphereWireframe(DrawContext context, float cx, float cy,
                                              float radius, float scale, float rotX, float rotY,
                                              int color, FieldEditState state) {
        int latSteps = Math.min(16, Math.max(6, state.getInt("sphere.latSteps") / 4));
        int lonSteps = Math.min(24, Math.max(8, state.getInt("sphere.lonSteps") / 4));
        
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
     * Draws a line using DrawContext's fill method (1-pixel wide).
     */
    private static void drawLine(DrawContext context, float x1, float y1, float x2, float y2, int color) {
        // Use Bresenham-like approach for smooth lines
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (length < 1) {
            context.fill((int) x1, (int) y1, (int) x1 + 1, (int) y1 + 1, color);
            return;
        }
        
        // Draw line as series of small rectangles
        float steps = Math.max(length, 1);
        float stepX = dx / steps;
        float stepY = dy / steps;
        
        for (int i = 0; i <= steps; i++) {
            int px = (int) (x1 + stepX * i);
            int py = (int) (y1 + stepY * i);
            context.fill(px, py, px + 1, py + 1, color);
        }
    }
}
