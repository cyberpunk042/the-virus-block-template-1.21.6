package net.cyberpunk042.client.visual.shader;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Depth buffer diagnostic with VISUAL OUTPUT for ALL modes.
 * 
 * Mode 1: Depth Range - Shows histogram bar + min/max values
 * Mode 2: Center Pixel - Shows crosshair with depth value that changes color  
 * Mode 3: FBO Info - Shows FBO binding status as colored indicators
 * Mode 4: Full Visualization - Shows complete depth buffer as grayscale image
 */
public class DirectDepthRenderer {
    
    private static final Identifier DEPTH_TEXTURE_ID = Identifier.of("the-virus-block", "debug/depth_buffer");
    
    private static boolean initialized = false;
    private static int mode = 0;
    private static int frameCount = 0;
    
    // Depth capture resources
    private static NativeImage depthImage;
    private static NativeImageBackedTexture depthTexture;
    private static int lastWidth = 0;
    private static int lastHeight = 0;
    private static boolean textureRegistered = false;
    
    // Stats for HUD display (all modes use these)
    private static float lastMinDepth = 1.0f;
    private static float lastMaxDepth = 0.0f;
    private static float lastCenterDepth = 1.0f;
    private static float lastCornerDepth = 1.0f;
    private static float lastRange = 0.0f;
    private static int lastReadFBO = 0;
    private static int lastDrawFBO = 0;
    private static boolean lastHasDepthTex = false;
    private static boolean lastHasDepthView = false;
    private static String lastDepthFormat = "?";
    private static int[] depthHistogram = new int[16];  // 16 buckets for histogram
    
    // Mode 5: Distance visualization stats
    private static float lastCenterDistance = 0.0f;
    private static float lastMinDistance = 0.0f;
    private static float lastMaxDistance = 0.0f;
    private static int lastSkyPixels = 0;
    private static boolean reversedZ = false;  // Toggle for reversed-Z depth buffer
    
    // Depth conversion constants
    private static final float NEAR_PLANE = 0.05f;
    private static float farPlane = 1000.0f;  // Will be updated based on render distance
    
    // Mode 6-8: Ring configuration
    private static float ringDistance = 15.0f;    // Distance in blocks for ring
    private static float ringThickness = 2.0f;    // Width of the ring in blocks
    private static int ringPixelCount = 0;        // How many pixels are in the ring
    
    // Mode 7: World point for ring origin
    private static org.joml.Vector3d ringWorldOrigin = new org.joml.Vector3d(0, 0, 0);
    private static boolean usePlayerAsOrigin = true;  // If true, use player position as origin
    
    // Mode 8: Animation state
    private static boolean animating = false;
    private static float animationRadius = 0.0f;
    private static float animationSpeed = 20.0f;  // blocks per second
    private static float maxAnimationRadius = 100.0f;
    private static long animationStartTime = 0;
    
    // Mode names
    private static final String[] MODE_NAMES = {
        "OFF",
        "Depth Range (Histogram)",
        "Center Crosshair",
        "FBO Info",
        "Full Depth Image",
        "Distance Visualization",
        "Ring at Fixed Distance",
        "Ring from World Point",
        "Animated Expanding Ring"
    };
    
    public static void init() {
        initialized = true;
        Logging.RENDER.topic("direct_depth").info("DirectDepthRenderer initialized");
    }
    
    public static boolean isEnabled() {
        return mode > 0;
    }
    
    public static int getMode() {
        return mode;
    }
    
    public static int getMaxMode() {
        return MODE_NAMES.length - 1;
    }
    
    public static void setMode(int newMode) {
        mode = Math.max(0, Math.min(newMode, getMaxMode()));
        frameCount = 0;
        Logging.RENDER.topic("direct_depth")
            .kv("mode", mode)
            .kv("name", MODE_NAMES[mode])
            .info("Direct depth mode changed");
    }
    
    public static void cycleMode() {
        mode = (mode + 1) % MODE_NAMES.length;
        frameCount = 0;
        Logging.RENDER.topic("direct_depth")
            .kv("mode", mode)
            .kv("name", MODE_NAMES[mode])
            .info("Direct depth mode cycled");
    }
    
    public static String getModeName() {
        return MODE_NAMES[mode];
    }
    
    /**
     * Captures depth buffer data - call from mixin after world render.
     */
    public static void render() {
        if (!isEnabled() || !initialized) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        Framebuffer framebuffer = client.getFramebuffer();
        if (framebuffer == null) return;
        
        int width = framebuffer.textureWidth;
        int height = framebuffer.textureHeight;
        
        frameCount++;
        
        // Update far plane based on render distance
        if (client.options != null) {
            int renderDist = client.options.getViewDistance().getValue();
            farPlane = Math.max(192.0f, renderDist * 16.0f);
        }
        
        // Capture data based on mode
        switch (mode) {
            case 1 -> captureDepthRange(width, height);
            case 2 -> captureCenterPixel(width, height);
            case 3 -> captureFBOInfo(client, framebuffer);
            case 4 -> captureFullDepth(width, height);
            case 5 -> captureDistanceVisualization(width, height);
            case 6 -> captureRingAtDistance(width, height);
            case 7 -> captureRingFromWorldPoint(client, width, height);
            case 8 -> captureAnimatedRing(client, width, height);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 1: Depth Range - Builds histogram
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void captureDepthRange(int width, int height) {
        int sampleWidth = width / 10;
        int sampleHeight = height / 10;
        
        FloatBuffer depthBuffer = BufferUtils.createFloatBuffer(sampleWidth * sampleHeight);
        GL11.glReadPixels(0, 0, sampleWidth, sampleHeight, 
            GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthBuffer);
        
        if (GL11.glGetError() != GL11.GL_NO_ERROR) return;
        
        // Reset histogram
        for (int i = 0; i < 16; i++) depthHistogram[i] = 0;
        
        float minDepth = 1.0f, maxDepth = 0.0f;
        int totalPixels = sampleWidth * sampleHeight;
        
        for (int i = 0; i < totalPixels; i++) {
            float depth = depthBuffer.get(i);
            if (depth < minDepth) minDepth = depth;
            if (depth > maxDepth) maxDepth = depth;
        }
        
        lastMinDepth = minDepth;
        lastMaxDepth = maxDepth;
        lastRange = maxDepth - minDepth;
        
        // Build histogram based on actual range
        depthBuffer.rewind();
        float range = lastRange > 0.0001f ? lastRange : 0.0001f;
        for (int i = 0; i < totalPixels; i++) {
            float depth = depthBuffer.get(i);
            float normalized = (depth - minDepth) / range;
            int bucket = Math.min(15, Math.max(0, (int)(normalized * 16)));
            depthHistogram[bucket]++;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 2: Center Pixel - Samples center and corners
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void captureCenterPixel(int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;
        
        FloatBuffer depthBuffer = BufferUtils.createFloatBuffer(1);
        GL11.glReadPixels(centerX, centerY, 1, 1, 
            GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthBuffer);
        
        if (GL11.glGetError() != GL11.GL_NO_ERROR) return;
        lastCenterDepth = depthBuffer.get(0);
        
        // Also sample corner
        FloatBuffer cornerBuffer = BufferUtils.createFloatBuffer(1);
        GL11.glReadPixels(10, 10, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, cornerBuffer);
        if (GL11.glGetError() == GL11.GL_NO_ERROR) {
            lastCornerDepth = cornerBuffer.get(0);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 3: FBO Info - Checks framebuffer binding
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void captureFBOInfo(MinecraftClient client, Framebuffer framebuffer) {
        IntBuffer currentReadFbo = BufferUtils.createIntBuffer(1);
        IntBuffer currentDrawFbo = BufferUtils.createIntBuffer(1);
        
        GL11.glGetIntegerv(GL30.GL_READ_FRAMEBUFFER_BINDING, currentReadFbo);
        GL11.glGetIntegerv(GL30.GL_DRAW_FRAMEBUFFER_BINDING, currentDrawFbo);
        
        lastReadFBO = currentReadFbo.get(0);
        lastDrawFBO = currentDrawFbo.get(0);
        
        @Nullable GpuTexture depthTex = framebuffer.getDepthAttachment();
        @Nullable GpuTextureView depthView = framebuffer.getDepthAttachmentView();
        
        lastHasDepthTex = depthTex != null;
        lastHasDepthView = depthView != null;
        lastDepthFormat = depthTex != null ? depthTex.getFormat().toString() : "N/A";
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 4: Full Depth Image
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void captureFullDepth(int width, int height) {
        int captureWidth = width / 4;
        int captureHeight = height / 4;
        
        // Ensure texture exists
        if (depthImage == null || lastWidth != captureWidth || lastHeight != captureHeight) {
            if (depthImage != null) depthImage.close();
            depthImage = new NativeImage(NativeImage.Format.RGBA, captureWidth, captureHeight, false);
            lastWidth = captureWidth;
            lastHeight = captureHeight;
            
            if (depthTexture != null) depthTexture.close();
            depthTexture = new NativeImageBackedTexture(DEPTH_TEXTURE_ID::toString, depthImage);
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getTextureManager() != null) {
                client.getTextureManager().registerTexture(DEPTH_TEXTURE_ID, depthTexture);
                textureRegistered = true;
            }
        }
        
        FloatBuffer depthBuffer = BufferUtils.createFloatBuffer(captureWidth * captureHeight);
        GL11.glReadPixels(0, 0, captureWidth, captureHeight, 
            GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthBuffer);
        
        if (GL11.glGetError() != GL11.GL_NO_ERROR) return;
        
        // First pass: find range
        float minDepth = 1.0f, maxDepth = 0.0f;
        for (int i = 0; i < captureWidth * captureHeight; i++) {
            float depth = depthBuffer.get(i);
            if (depth < minDepth) minDepth = depth;
            if (depth > maxDepth) maxDepth = depth;
        }
        lastMinDepth = minDepth;
        lastMaxDepth = maxDepth;
        lastRange = maxDepth - minDepth;
        
        // Second pass: convert to image
        depthBuffer.rewind();
        float range = lastRange > 0.0001f ? lastRange : 0.0001f;
        
        for (int y = 0; y < captureHeight; y++) {
            for (int x = 0; x < captureWidth; x++) {
                int srcY = captureHeight - 1 - y;
                float depth = depthBuffer.get(srcY * captureWidth + x);
                
                float normalized = 1.0f - ((depth - minDepth) / range);
                normalized = (float) Math.pow(normalized, 0.5);
                
                int gray = Math.max(0, Math.min(255, (int)(normalized * 255)));
                depthImage.setColorArgb(x, y, (255 << 24) | (gray << 16) | (gray << 8) | gray);
            }
        }
        
        depthTexture.upload();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VISUAL RENDERING - All modes show something visual
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void renderOverlay(DrawContext context, int screenWidth, int screenHeight) {
        if (!isEnabled()) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;
        
        try {
            switch (mode) {
                case 1 -> renderMode1_Histogram(context, client, screenWidth, screenHeight);
                case 2 -> renderMode2_Crosshair(context, client, screenWidth, screenHeight);
                case 3 -> renderMode3_FBOInfo(context, client, screenWidth, screenHeight);
                case 4 -> renderMode4_FullImage(context, client, screenWidth, screenHeight);
                case 5 -> renderMode5_DistanceVis(context, client, screenWidth, screenHeight);
                case 6 -> renderMode6_RingAtDistance(context, client, screenWidth, screenHeight);
                case 7 -> renderMode7_WorldPointRing(context, client, screenWidth, screenHeight);
                case 8 -> renderMode8_AnimatedRing(context, client, screenWidth, screenHeight);
            }
        } catch (Exception e) {
            // Silently fail
        }
    }
    
    private static void renderMode1_Histogram(DrawContext context, MinecraftClient client, int sw, int sh) {
        int x = 10, y = 10;
        int barWidth = 200, barHeight = 60;
        
        // Background
        context.fill(x - 2, y - 2, x + barWidth + 4, y + barHeight + 40, 0xC0000000);
        
        // Find max bucket for scaling
        int maxBucket = 1;
        for (int i = 0; i < 16; i++) {
            if (depthHistogram[i] > maxBucket) maxBucket = depthHistogram[i];
        }
        
        // Draw histogram bars
        int bucketWidth = barWidth / 16;
        for (int i = 0; i < 16; i++) {
            int bucketHeight = (int)((float)depthHistogram[i] / maxBucket * barHeight);
            int bx = x + i * bucketWidth;
            int by = y + barHeight - bucketHeight;
            
            // Color gradient: green (near) to red (far)
            int r = (int)(255 * (i / 15.0f));
            int g = (int)(255 * (1 - i / 15.0f));
            int color = 0xFF000000 | (r << 16) | (g << 8);
            
            context.fill(bx, by, bx + bucketWidth - 1, y + barHeight, color);
        }
        
        // Border
        context.fill(x - 1, y - 1, x + barWidth + 1, y, 0xFFFFFFFF);
        context.fill(x - 1, y + barHeight, x + barWidth + 1, y + barHeight + 1, 0xFFFFFFFF);
        
        // Labels
        context.drawText(client.textRenderer, 
            String.format("Min: %.6f  Max: %.6f", lastMinDepth, lastMaxDepth),
            x, y + barHeight + 5, 0xFFFFFF, true);
        context.drawText(client.textRenderer,
            String.format("Range: %.6f", lastRange),
            x, y + barHeight + 18, 0xAAFFAA, true);
    }
    
    private static void renderMode2_Crosshair(DrawContext context, MinecraftClient client, int sw, int sh) {
        int cx = sw / 2;
        int cy = sh / 2;
        int size = 20;
        
        // Color based on depth: green = near, red = far
        float depthFactor = Math.min(1.0f, Math.max(0.0f, (lastCenterDepth - 0.99f) / 0.01f));
        int r = (int)(255 * depthFactor);
        int g = (int)(255 * (1 - depthFactor));
        int crossColor = 0xFF000000 | (r << 16) | (g << 8);
        
        // Draw crosshair
        context.fill(cx - size, cy - 1, cx + size, cy + 2, crossColor);
        context.fill(cx - 1, cy - size, cx + 2, cy + size, crossColor);
        
        // Circle outline
        for (int i = 0; i < 32; i++) {
            double angle = i * Math.PI * 2 / 32;
            int px = cx + (int)(Math.cos(angle) * 15);
            int py = cy + (int)(Math.sin(angle) * 15);
            context.fill(px, py, px + 2, py + 2, crossColor);
        }
        
        // Info box
        int bx = 10, by = 10;
        context.fill(bx - 2, by - 2, bx + 180, by + 50, 0xC0000000);
        
        context.drawText(client.textRenderer, "CENTER DEPTH", bx, by, 0xFFFF00, true);
        context.drawText(client.textRenderer, 
            String.format("Raw: %.6f", lastCenterDepth),
            bx, by + 12, 0xFFFFFF, true);
        context.drawText(client.textRenderer,
            String.format("Corner: %.6f", lastCornerDepth),
            bx, by + 24, 0xAAAAAA, true);
        context.drawText(client.textRenderer,
            String.format("Diff: %.6f", Math.abs(lastCenterDepth - lastCornerDepth)),
            bx, by + 36, 0xAAFFAA, true);
    }
    
    private static void renderMode3_FBOInfo(DrawContext context, MinecraftClient client, int sw, int sh) {
        int x = 10, y = 10;
        int w = 220, h = 100;
        
        // Background
        context.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xC0000000);
        
        // Title
        context.drawText(client.textRenderer, "FRAMEBUFFER INFO", x, y, 0xFFFF00, true);
        
        // FBO IDs with status indicators
        int statusColor = (lastReadFBO == lastDrawFBO) ? 0xFF00FF00 : 0xFFFF0000;
        context.fill(x, y + 14, x + 8, y + 22, statusColor);
        context.drawText(client.textRenderer, 
            String.format("Read FBO: %d  Draw FBO: %d", lastReadFBO, lastDrawFBO),
            x + 12, y + 14, 0xFFFFFF, true);
        
        // Depth texture status
        int depthTexColor = lastHasDepthTex ? 0xFF00FF00 : 0xFFFF0000;
        context.fill(x, y + 28, x + 8, y + 36, depthTexColor);
        context.drawText(client.textRenderer,
            "Depth Texture: " + (lastHasDepthTex ? "YES" : "NO"),
            x + 12, y + 28, 0xFFFFFF, true);
        
        // Depth view status
        int depthViewColor = lastHasDepthView ? 0xFF00FF00 : 0xFFFF0000;
        context.fill(x, y + 42, x + 8, y + 50, depthViewColor);
        context.drawText(client.textRenderer,
            "Depth View: " + (lastHasDepthView ? "YES" : "NO"),
            x + 12, y + 42, 0xFFFFFF, true);
        
        // Format
        context.drawText(client.textRenderer,
            "Format: " + lastDepthFormat,
            x, y + 58, 0xAAFFAA, true);
        
        // Summary
        boolean allGood = lastHasDepthTex && lastHasDepthView && (lastReadFBO == lastDrawFBO);
        int summaryColor = allGood ? 0xFF00FF00 : 0xFFFF8800;
        context.drawText(client.textRenderer,
            allGood ? "Status: ALL GOOD" : "Status: CHECK ISSUES",
            x, y + 78, summaryColor, true);
    }
    
    private static void renderMode4_FullImage(DrawContext context, MinecraftClient client, int sw, int sh) {
        if (!textureRegistered || depthTexture == null) return;
        
        int x = 10, y = 10;
        int displayWidth = lastWidth;
        int displayHeight = lastHeight;
        
        // Background
        context.fill(x - 2, y - 2, x + displayWidth + 2, y + displayHeight + 30, 0xC0000000);
        
        // Depth image
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            DEPTH_TEXTURE_ID,
            x, y,
            0.0f, 0.0f,
            displayWidth, displayHeight,
            displayWidth, displayHeight
        );
        
        // Stats
        context.drawText(client.textRenderer,
            String.format("Depth: %.4f - %.4f (range: %.6f)", lastMinDepth, lastMaxDepth, lastRange),
            x, y + displayHeight + 4, 0xFFFFFF, true);
        context.drawText(client.textRenderer,
            "White=Near  Black=Far",
            x, y + displayHeight + 16, 0xAAAAAA, true);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 5: Distance Visualization
    // Converts depth to world-space distance and visualizes as color gradient
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void captureDistanceVisualization(int width, int height) {
        int captureWidth = width / 4;
        int captureHeight = height / 4;
        
        // Ensure texture exists
        if (depthImage == null || lastWidth != captureWidth || lastHeight != captureHeight) {
            if (depthImage != null) depthImage.close();
            depthImage = new NativeImage(NativeImage.Format.RGBA, captureWidth, captureHeight, false);
            lastWidth = captureWidth;
            lastHeight = captureHeight;
            
            if (depthTexture != null) depthTexture.close();
            depthTexture = new NativeImageBackedTexture(DEPTH_TEXTURE_ID::toString, depthImage);
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getTextureManager() != null) {
                client.getTextureManager().registerTexture(DEPTH_TEXTURE_ID, depthTexture);
                textureRegistered = true;
            }
        }
        
        // Read depth buffer
        FloatBuffer depthBuffer = BufferUtils.createFloatBuffer(captureWidth * captureHeight);
        GL11.glReadPixels(0, 0, captureWidth, captureHeight, 
            GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthBuffer);
        
        if (GL11.glGetError() != GL11.GL_NO_ERROR) return;
        
        // Stats tracking
        float minDist = Float.MAX_VALUE;
        float maxDist = 0.0f;
        int skyPixels = 0;
        int centerIdx = (captureHeight / 2) * captureWidth + (captureWidth / 2);
        float centerDepth = 0.0f;
        
        // First pass: analyze and convert to distances
        for (int y = 0; y < captureHeight; y++) {
            for (int x = 0; x < captureWidth; x++) {
                int srcY = captureHeight - 1 - y;
                int idx = srcY * captureWidth + x;
                float depth = depthBuffer.get(idx);
                
                // Check for center pixel
                if (idx == centerIdx) {
                    centerDepth = depth;
                }
                
                // Sky detection (depth very close to 1.0)
                if (depth >= 0.9999f) {
                    skyPixels++;
                    // Sky = black
                    depthImage.setColorArgb(x, y, 0xFF000000);
                    continue;
                }
                
                // Convert depth to distance
                float distance = depthToDistance(depth);
                
                if (distance < minDist) minDist = distance;
                if (distance > maxDist) maxDist = distance;
                
                // Convert distance to color
                int color = distanceToColor(distance);
                depthImage.setColorArgb(x, y, color);
            }
        }
        
        // Update stats
        lastCenterDistance = depthToDistance(centerDepth);
        lastMinDistance = (minDist == Float.MAX_VALUE) ? 0.0f : minDist;
        lastMaxDistance = maxDist;
        lastSkyPixels = skyPixels;
        
        depthTexture.upload();
        
        // Log diagnostics every second
        if (frameCount % 60 == 1) {
            Logging.RENDER.topic("mode5_distance")
                .kv("nearPlane", NEAR_PLANE)
                .kv("farPlane", farPlane)
                .kv("reversedZ", reversedZ)
                .kv("centerDepth", String.format("%.6f", centerDepth))
                .kv("centerDist", String.format("%.2f", lastCenterDistance))
                .kv("minDist", String.format("%.2f", lastMinDistance))
                .kv("maxDist", String.format("%.2f", lastMaxDistance))
                .kv("skyPixels", skyPixels)
                .info("Distance analysis");
        }
    }
    
    /**
     * Converts depth buffer value to world-space distance in blocks.
     */
    private static float depthToDistance(float depth) {
        // Handle edge cases
        if (depth <= 0.0f) return NEAR_PLANE;
        if (depth >= 1.0f) return farPlane;
        
        // Apply reversed-Z if enabled
        float d = reversedZ ? (1.0f - depth) : depth;
        
        // Standard perspective depth linearization:
        // z_linear = (far * near) / (far - depth * (far - near))
        float distance = (farPlane * NEAR_PLANE) / (farPlane - d * (farPlane - NEAR_PLANE));
        
        // Clamp to valid range
        return Math.max(NEAR_PLANE, Math.min(farPlane, distance));
    }
    
    /**
     * Converts distance in blocks to a color on the gradient.
     * Red = very close (0-5), Orange (5-15), Yellow (15-30), 
     * Green (30-60), Cyan (60-100), Blue (100+)
     */
    private static int distanceToColor(float distance) {
        int r, g, b;
        
        if (distance < 5.0f) {
            // Red zone (0-5 blocks)
            float t = distance / 5.0f;
            r = 255;
            g = (int)(t * 100);
            b = 0;
        } else if (distance < 15.0f) {
            // Orange zone (5-15 blocks)
            float t = (distance - 5.0f) / 10.0f;
            r = 255;
            g = 100 + (int)(t * 155);
            b = 0;
        } else if (distance < 30.0f) {
            // Yellow zone (15-30 blocks)
            float t = (distance - 15.0f) / 15.0f;
            r = 255 - (int)(t * 55);
            g = 255;
            b = 0;
        } else if (distance < 60.0f) {
            // Green zone (30-60 blocks)
            float t = (distance - 30.0f) / 30.0f;
            r = 0;
            g = 255 - (int)(t * 55);
            b = (int)(t * 200);
        } else if (distance < 100.0f) {
            // Cyan zone (60-100 blocks)
            float t = (distance - 60.0f) / 40.0f;
            r = 0;
            g = 200 - (int)(t * 100);
            b = 200 + (int)(t * 55);
        } else {
            // Blue zone (100+ blocks)
            r = 0;
            g = 100;
            b = 255;
        }
        
        return (255 << 24) | (r << 16) | (g << 8) | b;
    }
    
    private static void renderMode5_DistanceVis(DrawContext context, MinecraftClient client, int sw, int sh) {
        if (!textureRegistered || depthTexture == null) return;
        
        int x = 10, y = 10;
        int displayWidth = lastWidth;
        int displayHeight = lastHeight;
        
        // Background
        context.fill(x - 2, y - 2, x + displayWidth + 2, y + displayHeight + 80, 0xC0000000);
        
        // Distance image
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            DEPTH_TEXTURE_ID,
            x, y,
            0.0f, 0.0f,
            displayWidth, displayHeight,
            displayWidth, displayHeight
        );
        
        // Stats text
        context.drawText(client.textRenderer,
            String.format("Center: %.1f blocks", lastCenterDistance),
            x, y + displayHeight + 4, 0xFFFFFF, true);
        context.drawText(client.textRenderer,
            String.format("Range: %.1f - %.1f blocks", lastMinDistance, lastMaxDistance),
            x, y + displayHeight + 16, 0xAAFFAA, true);
        context.drawText(client.textRenderer,
            String.format("Sky pixels: %d | Far: %.0f | RevZ: %s", lastSkyPixels, farPlane, reversedZ ? "ON" : "OFF"),
            x, y + displayHeight + 28, 0xAAAAFF, true);
        
        // Legend (color scale)
        int legendX = x;
        int legendY = y + displayHeight + 44;
        int legendWidth = 150;
        int legendHeight = 12;
        
        // Draw gradient legend
        for (int i = 0; i < legendWidth; i++) {
            float dist = (i / (float)legendWidth) * 100.0f;
            int color = distanceToColor(dist);
            context.fill(legendX + i, legendY, legendX + i + 1, legendY + legendHeight, color);
        }
        
        // Legend labels
        context.drawText(client.textRenderer, "0", legendX, legendY + legendHeight + 2, 0xFFFFFF, true);
        context.drawText(client.textRenderer, "50", legendX + legendWidth/2 - 6, legendY + legendHeight + 2, 0xFFFFFF, true);
        context.drawText(client.textRenderer, "100+", legendX + legendWidth - 20, legendY + legendHeight + 2, 0xFFFFFF, true);
    }
    
    /**
     * Toggle reversed-Z mode for testing.
     */
    public static void toggleReversedZ() {
        reversedZ = !reversedZ;
        Logging.RENDER.topic("direct_depth")
            .kv("reversedZ", reversedZ)
            .info("Reversed-Z toggled");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 6: Ring at Fixed Distance from Camera
    // Highlights all pixels at a specific distance
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void captureRingAtDistance(int width, int height) {
        int captureWidth = width / 4;
        int captureHeight = height / 4;
        
        // Ensure texture exists
        if (depthImage == null || lastWidth != captureWidth || lastHeight != captureHeight) {
            if (depthImage != null) depthImage.close();
            depthImage = new NativeImage(NativeImage.Format.RGBA, captureWidth, captureHeight, false);
            lastWidth = captureWidth;
            lastHeight = captureHeight;
            
            if (depthTexture != null) depthTexture.close();
            depthTexture = new NativeImageBackedTexture(DEPTH_TEXTURE_ID::toString, depthImage);
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getTextureManager() != null) {
                client.getTextureManager().registerTexture(DEPTH_TEXTURE_ID, depthTexture);
                textureRegistered = true;
            }
        }
        
        // Read depth buffer
        FloatBuffer depthBuffer = BufferUtils.createFloatBuffer(captureWidth * captureHeight);
        GL11.glReadPixels(0, 0, captureWidth, captureHeight, 
            GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthBuffer);
        
        if (GL11.glGetError() != GL11.GL_NO_ERROR) return;
        
        // Stats
        int skyPixels = 0;
        int localRingPixels = 0;
        float innerRing = ringDistance - ringThickness / 2.0f;
        float outerRing = ringDistance + ringThickness / 2.0f;
        
        // Process pixels
        for (int y = 0; y < captureHeight; y++) {
            for (int x = 0; x < captureWidth; x++) {
                int srcY = captureHeight - 1 - y;
                int idx = srcY * captureWidth + x;
                float depth = depthBuffer.get(idx);
                
                // Sky detection
                if (depth >= 0.9999f) {
                    skyPixels++;
                    depthImage.setColorArgb(x, y, 0xFF000000);
                    continue;
                }
                
                // Convert to distance
                float distance = depthToDistance(depth);
                
                // Check if within ring
                if (distance >= innerRing && distance <= outerRing) {
                    localRingPixels++;
                    // Bright cyan for ring
                    depthImage.setColorArgb(x, y, 0xFF00FFFF);
                } else {
                    // Dim gray for background
                    float brightness = 1.0f - Math.min(1.0f, distance / 100.0f);
                    int gray = (int)(brightness * 80);
                    depthImage.setColorArgb(x, y, (255 << 24) | (gray << 16) | (gray << 8) | gray);
                }
            }
        }
        
        ringPixelCount = localRingPixels;
        lastSkyPixels = skyPixels;
        
        depthTexture.upload();
        
        // Log diagnostics
        if (frameCount % 60 == 1) {
            Logging.RENDER.topic("mode6_ring")
                .kv("ringDistance", String.format("%.1f", ringDistance))
                .kv("ringThickness", String.format("%.1f", ringThickness))
                .kv("ringPixels", ringPixelCount)
                .kv("skyPixels", skyPixels)
                .info("Ring analysis");
        }
    }
    
    private static void renderMode6_RingAtDistance(DrawContext context, MinecraftClient client, int sw, int sh) {
        if (!textureRegistered || depthTexture == null) return;
        
        int x = 10, y = 10;
        int displayWidth = lastWidth;
        int displayHeight = lastHeight;
        
        // Background
        context.fill(x - 2, y - 2, x + displayWidth + 2, y + displayHeight + 65, 0xC0000000);
        
        // Ring image
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            DEPTH_TEXTURE_ID,
            x, y,
            0.0f, 0.0f,
            displayWidth, displayHeight,
            displayWidth, displayHeight
        );
        
        // Stats
        context.drawText(client.textRenderer,
            String.format("Ring at %.1f blocks (±%.1f)", ringDistance, ringThickness / 2),
            x, y + displayHeight + 4, 0x00FFFF, true);
        context.drawText(client.textRenderer,
            String.format("Ring pixels: %d", ringPixelCount),
            x, y + displayHeight + 16, 0xAAFFAA, true);
        context.drawText(client.textRenderer,
            String.format("Sky: %d | Far: %.0f", lastSkyPixels, farPlane),
            x, y + displayHeight + 28, 0xAAAAFF, true);
        
        // Instructions
        context.drawText(client.textRenderer,
            "/directdepth ring <dist> <thick>",
            x, y + displayHeight + 44, 0x888888, true);
    }
    
    /**
     * Set ring distance and thickness for modes 6-8.
     */
    public static void setRingParams(float distance, float thickness) {
        ringDistance = Math.max(1.0f, distance);
        ringThickness = Math.max(0.5f, thickness);
        Logging.RENDER.topic("direct_depth")
            .kv("ringDistance", ringDistance)
            .kv("ringThickness", ringThickness)
            .info("Ring parameters updated");
    }
    
    public static float getRingDistance() {
        return ringDistance;
    }
    
    public static float getRingThickness() {
        return ringThickness;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 7: Ring from World Point
    // Uses player position or custom world point as ring origin
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void captureRingFromWorldPoint(MinecraftClient client, int width, int height) {
        int captureWidth = width / 4;
        int captureHeight = height / 4;
        
        // Get camera position
        var camera = client.gameRenderer.getCamera();
        if (camera == null) return;
        
        var cameraPos = camera.getPos();
        
        // Get ring origin (player feet or custom)
        org.joml.Vector3d origin;
        if (usePlayerAsOrigin && client.player != null) {
            origin = new org.joml.Vector3d(
                client.player.getX(),
                client.player.getY(),
                client.player.getZ()
            );
        } else {
            origin = ringWorldOrigin;
        }
        
        // Ensure texture exists
        if (depthImage == null || lastWidth != captureWidth || lastHeight != captureHeight) {
            if (depthImage != null) depthImage.close();
            depthImage = new NativeImage(NativeImage.Format.RGBA, captureWidth, captureHeight, false);
            lastWidth = captureWidth;
            lastHeight = captureHeight;
            
            if (depthTexture != null) depthTexture.close();
            depthTexture = new NativeImageBackedTexture(DEPTH_TEXTURE_ID::toString, depthImage);
            
            if (client.getTextureManager() != null) {
                client.getTextureManager().registerTexture(DEPTH_TEXTURE_ID, depthTexture);
                textureRegistered = true;
            }
        }
        
        // Read depth buffer
        FloatBuffer depthBuffer = BufferUtils.createFloatBuffer(captureWidth * captureHeight);
        GL11.glReadPixels(0, 0, captureWidth, captureHeight, 
            GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthBuffer);
        
        if (GL11.glGetError() != GL11.GL_NO_ERROR) return;
        
        // Get camera direction vectors
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        
        // Forward direction
        double fx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double fy = -Math.sin(pitchRad);
        double fz = Math.cos(yawRad) * Math.cos(pitchRad);
        
        // Right direction
        double rx = Math.cos(yawRad);
        double rz = Math.sin(yawRad);
        
        // Up direction (cross product)
        double ux = fy * rz;
        double uy = fz * rx - fx * rz;
        double uz = -fy * rx;
        
        // FOV
        double fov = Math.toRadians(client.options.getFov().getValue());
        double aspectRatio = (double) captureWidth / captureHeight;
        
        int skyPixels = 0;
        int localRingPixels = 0;
        float innerRing = ringDistance - ringThickness / 2.0f;
        float outerRing = ringDistance + ringThickness / 2.0f;
        
        for (int y = 0; y < captureHeight; y++) {
            for (int x = 0; x < captureWidth; x++) {
                int srcY = captureHeight - 1 - y;
                float depth = depthBuffer.get(srcY * captureWidth + x);
                
                if (depth >= 0.9999f) {
                    skyPixels++;
                    depthImage.setColorArgb(x, y, 0xFF000000);
                    continue;
                }
                
                // Convert depth to eye-space distance
                float eyeDist = depthToDistance(depth);
                
                // Calculate ray direction for this pixel
                double ndcX = (2.0 * x / captureWidth - 1.0) * aspectRatio * Math.tan(fov / 2);
                double ndcY = (1.0 - 2.0 * y / captureHeight) * Math.tan(fov / 2);
                
                // Ray direction = forward + ndcX * right + ndcY * up
                double dirX = fx + ndcX * rx + ndcY * ux;
                double dirY = fy + ndcY * uy;
                double dirZ = fz + ndcX * rz + ndcY * uz;
                
                // Normalize
                double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                dirX /= len; dirY /= len; dirZ /= len;
                
                // World position = camera + direction * distance
                double worldX = cameraPos.x + dirX * eyeDist;
                double worldY = cameraPos.y + dirY * eyeDist;
                double worldZ = cameraPos.z + dirZ * eyeDist;
                
                // Distance from origin
                double dx = worldX - origin.x;
                double dy = worldY - origin.y;
                double dz = worldZ - origin.z;
                float distFromOrigin = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                
                // Check if within ring
                if (distFromOrigin >= innerRing && distFromOrigin <= outerRing) {
                    localRingPixels++;
                    depthImage.setColorArgb(x, y, 0xFFFF00FF);  // Magenta ring
                } else {
                    // Dim coloring based on distance from origin
                    float normalized = Math.min(1.0f, distFromOrigin / 50.0f);
                    int gray = (int)((1.0f - normalized) * 100);
                    depthImage.setColorArgb(x, y, (255 << 24) | (gray << 16) | (gray << 8) | gray);
                }
            }
        }
        
        ringPixelCount = localRingPixels;
        lastSkyPixels = skyPixels;
        depthTexture.upload();
        
        if (frameCount % 60 == 1) {
            Logging.RENDER.topic("mode7_world")
                .kv("origin", String.format("%.1f, %.1f, %.1f", origin.x, origin.y, origin.z))
                .kv("ringDist", ringDistance)
                .kv("ringPixels", ringPixelCount)
                .info("World ring analysis");
        }
    }
    
    private static void renderMode7_WorldPointRing(DrawContext context, MinecraftClient client, int sw, int sh) {
        if (!textureRegistered || depthTexture == null) return;
        
        int x = 10, y = 10;
        int displayWidth = lastWidth;
        int displayHeight = lastHeight;
        
        context.fill(x - 2, y - 2, x + displayWidth + 2, y + displayHeight + 80, 0xC0000000);
        
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            DEPTH_TEXTURE_ID,
            x, y,
            0.0f, 0.0f,
            displayWidth, displayHeight,
            displayWidth, displayHeight
        );
        
        String originStr = usePlayerAsOrigin ? "Player Position" : 
            String.format("%.1f, %.1f, %.1f", ringWorldOrigin.x, ringWorldOrigin.y, ringWorldOrigin.z);
        
        context.drawText(client.textRenderer,
            "Origin: " + originStr,
            x, y + displayHeight + 4, 0xFF00FF, true);
        context.drawText(client.textRenderer,
            String.format("Ring at %.1f blocks (±%.1f)", ringDistance, ringThickness / 2),
            x, y + displayHeight + 16, 0xFFFFFF, true);
        context.drawText(client.textRenderer,
            String.format("Ring pixels: %d | Sky: %d", ringPixelCount, lastSkyPixels),
            x, y + displayHeight + 28, 0xAAFFAA, true);
        context.drawText(client.textRenderer,
            "Magenta = Ring | Gray = World",
            x, y + displayHeight + 44, 0x888888, true);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 8: Animated Expanding Ring
    // Full shockwave simulation with expanding radius
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void captureAnimatedRing(MinecraftClient client, int width, int height) {
        // Update animation
        if (animating) {
            float elapsed = (System.currentTimeMillis() - animationStartTime) / 1000.0f;
            animationRadius = elapsed * animationSpeed;
            if (animationRadius > maxAnimationRadius) {
                animating = false;
                animationRadius = maxAnimationRadius;
            }
        }
        
        // Temporarily override ring distance with animation radius
        float savedRingDist = ringDistance;
        ringDistance = animationRadius;
        
        // Reuse Mode 7's logic
        captureRingFromWorldPoint(client, width, height);
        
        // Restore
        ringDistance = savedRingDist;
    }
    
    private static void renderMode8_AnimatedRing(DrawContext context, MinecraftClient client, int sw, int sh) {
        if (!textureRegistered || depthTexture == null) return;
        
        int x = 10, y = 10;
        int displayWidth = lastWidth;
        int displayHeight = lastHeight;
        
        context.fill(x - 2, y - 2, x + displayWidth + 2, y + displayHeight + 95, 0xC0000000);
        
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            DEPTH_TEXTURE_ID,
            x, y,
            0.0f, 0.0f,
            displayWidth, displayHeight,
            displayWidth, displayHeight
        );
        
        String originStr = usePlayerAsOrigin ? "Player Position" : 
            String.format("%.1f, %.1f, %.1f", ringWorldOrigin.x, ringWorldOrigin.y, ringWorldOrigin.z);
        
        context.drawText(client.textRenderer,
            "Origin: " + originStr,
            x, y + displayHeight + 4, 0xFF00FF, true);
        context.drawText(client.textRenderer,
            String.format("Radius: %.1f / %.1f blocks", animationRadius, maxAnimationRadius),
            x, y + displayHeight + 16, animating ? 0x00FF00 : 0xFFFF00, true);
        context.drawText(client.textRenderer,
            String.format("Speed: %.1f blocks/sec", animationSpeed),
            x, y + displayHeight + 28, 0xAAAAFF, true);
        context.drawText(client.textRenderer,
            String.format("Status: %s", animating ? "EXPANDING" : "STOPPED"),
            x, y + displayHeight + 40, animating ? 0x00FF00 : 0xFF0000, true);
        context.drawText(client.textRenderer,
            String.format("Ring pixels: %d", ringPixelCount),
            x, y + displayHeight + 52, 0xAAFFAA, true);
        
        // Instructions
        context.drawText(client.textRenderer,
            "/directdepth trigger | /directdepth speed <n>",
            x, y + displayHeight + 68, 0x888888, true);
    }
    
    /**
     * Start the shockwave animation.
     */
    public static void triggerAnimation() {
        animating = true;
        animationRadius = 0.0f;
        animationStartTime = System.currentTimeMillis();
        Logging.RENDER.topic("direct_depth")
            .kv("speed", animationSpeed)
            .kv("maxRadius", maxAnimationRadius)
            .info("Animation triggered");
    }
    
    /**
     * Set animation speed.
     */
    public static void setAnimationSpeed(float speed) {
        animationSpeed = Math.max(1.0f, speed);
        Logging.RENDER.topic("direct_depth")
            .kv("animationSpeed", animationSpeed)
            .info("Animation speed updated");
    }
    
    /**
     * Set max animation radius.
     */
    public static void setMaxRadius(float radius) {
        maxAnimationRadius = Math.max(10.0f, radius);
        Logging.RENDER.topic("direct_depth")
            .kv("maxAnimationRadius", maxAnimationRadius)
            .info("Max radius updated");
    }
    
    public static void dispose() {
        if (depthTexture != null) {
            depthTexture.close();
            depthTexture = null;
        }
        if (depthImage != null) {
            depthImage.close();
            depthImage = null;
        }
        textureRegistered = false;
    }
}
