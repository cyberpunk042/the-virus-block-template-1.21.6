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
    private static final int MAX_MODE = 4;
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
    
    // Mode names
    private static final String[] MODE_NAMES = {
        "OFF",
        "Depth Range (Histogram)",
        "Center Crosshair",
        "FBO Info",
        "Full Depth Image"
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
    
    public static void setMode(int newMode) {
        mode = Math.max(0, Math.min(newMode, MAX_MODE));
        frameCount = 0;
        Logging.RENDER.topic("direct_depth")
            .kv("mode", mode)
            .kv("name", MODE_NAMES[mode])
            .info("Direct depth mode changed");
    }
    
    public static void cycleMode() {
        mode = (mode + 1) % (MAX_MODE + 1);
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
        
        // Capture data based on mode
        switch (mode) {
            case 1 -> captureDepthRange(width, height);
            case 2 -> captureCenterPixel(width, height);
            case 3 -> captureFBOInfo(client, framebuffer);
            case 4 -> captureFullDepth(width, height);
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
