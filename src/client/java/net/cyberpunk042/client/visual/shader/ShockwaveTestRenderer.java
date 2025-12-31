package net.cyberpunk042.client.visual.shader;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * TEST: Renders a fullscreen overlay to verify HUD rendering works.
 * 
 * <p>Uses DrawContext.fill() which is proven to work (DirectDepthRenderer uses it).
 * 
 * <p>Command: /shockwavetest
 */
public final class ShockwaveTestRenderer {
    
    private static boolean enabled = false;
    private static float testRadius = 20.0f;
    private static int frameCount = 0;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void toggle() {
        enabled = !enabled;
        Logging.RENDER.topic("shockwave_test")
            .kv("enabled", enabled)
            .info("ShockwaveTestRenderer toggled");
    }
    
    public static void setRadius(float r) {
        testRadius = r;
        enabled = true;
        Logging.RENDER.topic("shockwave_test")
            .kv("radius", r)
            .info("Test radius set");
    }
    
    public static float getRadius() {
        return testRadius;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void init() {
        ShockwaveTestPipeline.init();
        Logging.RENDER.topic("shockwave_test")
            .info("ShockwaveTestRenderer initialized");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RENDERING - Using DrawContext (proven to work)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Render a test fullscreen overlay using DrawContext.
     * Called from ShockwaveTestMixin at end of HUD render.
     */
    public static void render(DrawContext context) {
        if (!enabled) return;
        
        frameCount++;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Use scaled window dimensions (like DirectDepthRenderer does)
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        
        // Log every 60 frames
        if (frameCount % 60 == 1) {
            Logging.RENDER.topic("shockwave_test")
                .kv("radius", testRadius)
                .kv("screen", sw + "x" + sh)
                .info("Rendering test overlay");
        }
        
        // Draw a VERY VISIBLE magenta border around the screen
        // This proves DrawContext.fill() works from our mixin
        int borderSize = 10;
        int color = 0x99FF00FF; // Magenta with alpha
        
        // Top border
        context.fill(0, 0, sw, borderSize, color);
        // Bottom border
        context.fill(0, sh - borderSize, sw, sh, color);
        // Left border
        context.fill(0, 0, borderSize, sh, color);
        // Right border
        context.fill(sw - borderSize, 0, sw, sh, color);
        
        // Show radius value in center
        String text = String.format("TEST RADIUS: %.1f", testRadius);
        int textWidth = client.textRenderer.getWidth(text);
        context.drawText(client.textRenderer, text, 
            (sw - textWidth) / 2, sh / 2, 0xFFFF00FF, true);
    }
    
    private ShockwaveTestRenderer() {}
}
