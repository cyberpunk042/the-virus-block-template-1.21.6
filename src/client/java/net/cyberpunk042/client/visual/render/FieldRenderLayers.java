package net.cyberpunk042.client.visual.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.util.Identifier;
import net.cyberpunk042.visual.layer.BlendMode;
import org.lwjgl.opengl.GL14;

/**
 * Custom render layers and blend utilities for field visual effects.
 * 
 * <h2>Available Blend Modes</h2>
 * <ul>
 *   <li><b>NORMAL</b> - Standard alpha blending (src*alpha + dst*(1-alpha))</li>
 *   <li><b>ADD</b> - Additive blending (src*alpha + dst) - glows, light effects</li>
 *   <li><b>MULTIPLY</b> - Multiplicative blending (src*dst) - shadows, darkening</li>
 *   <li><b>SCREEN</b> - Screen blending (1 - (1-src)*(1-dst)) - lightening</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Before rendering:
 * FieldRenderLayers.applyBlendMode("ADD");
 * // ... render geometry ...
 * FieldRenderLayers.resetBlendMode();
 * </pre>
 */
public final class FieldRenderLayers extends RenderPhase {
    
    private FieldRenderLayers() {
        super("field_render_layers", () -> {}, () -> {});
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Render Layers (using Minecraft's built-in layers)
    // ─────────────────────────────────────────────────────────────────────────────
    
    private static final RenderLayer SOLID_TRANSLUCENT = RenderLayer.getEntityTranslucent(
        Identifier.of("minecraft", "textures/misc/white.png")
    );
    
    private static final RenderLayer LINES_LAYER = RenderLayer.getLines();
    
    /**
     * Translucent solid layer for field geometry.
     */
    public static RenderLayer solidTranslucent() {
        return SOLID_TRANSLUCENT;
    }
    
    /**
     * Translucent solid layer with custom texture.
     */
    public static RenderLayer solidTranslucent(Identifier texture) {
        return RenderLayer.getEntityTranslucent(texture);
    }
    
    /**
     * Lines layer for wireframe rendering.
     */
    public static RenderLayer lines() {
        return LINES_LAYER;
    }
    
    // Legacy aliases
    public static RenderLayer glowTranslucent() { return SOLID_TRANSLUCENT; }
    public static RenderLayer glowAdditive() { return SOLID_TRANSLUCENT; }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Blend Mode Control (call before/after rendering)
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Applies a blend mode before rendering.
     * Call {@link #resetBlendMode()} after rendering!
     * 
     * @param blendMode One of: "NORMAL", "ADD", "MULTIPLY", "SCREEN"
     */
    public static void applyBlendMode(String blendMode) {
        if (blendMode == null) blendMode = "NORMAL";
        
        GlStateManager._enableBlend();
        
        switch (blendMode.toUpperCase()) {
            case "ADD", "ADDITIVE" -> {
                // Additive: src*alpha + dst (glow effect)
                GlStateManager._blendFuncSeparate(
                    GL14.GL_SRC_ALPHA,  // srcRGB
                    GL14.GL_ONE,        // dstRGB
                    GL14.GL_ONE,        // srcAlpha
                    GL14.GL_ONE         // dstAlpha
                );
            }
            case "MULTIPLY", "MULT" -> {
                // Multiply: src * dst (darkening)
                GlStateManager._blendFuncSeparate(
                    GL14.GL_DST_COLOR,  // srcRGB
                    GL14.GL_ZERO,       // dstRGB
                    GL14.GL_ONE,        // srcAlpha
                    GL14.GL_ZERO        // dstAlpha
                );
            }
            case "SCREEN" -> {
                // Screen: 1 - (1-src)*(1-dst) (lightening)
                GlStateManager._blendFuncSeparate(
                    GL14.GL_ONE,                    // srcRGB
                    GL14.GL_ONE_MINUS_SRC_COLOR,   // dstRGB
                    GL14.GL_ONE,                    // srcAlpha
                    GL14.GL_ONE_MINUS_SRC_ALPHA    // dstAlpha
                );
            }
            default -> {
                // Normal alpha: src*alpha + dst*(1-alpha)
                GlStateManager._blendFuncSeparate(
                    GL14.GL_SRC_ALPHA,              // srcRGB
                    GL14.GL_ONE_MINUS_SRC_ALPHA,   // dstRGB
                    GL14.GL_ONE,                    // srcAlpha
                    GL14.GL_ONE_MINUS_SRC_ALPHA    // dstAlpha
                );
            }
        }
    }
    
    /**
     * Applies a blend mode before rendering.
     */
    public static void applyBlendMode(BlendMode mode) {
        applyBlendMode(mode.name());
    }
    
    /**
     * Resets blend mode to default after rendering.
     */
    public static void resetBlendMode() {
        // Reset to default alpha blend
        GlStateManager._blendFuncSeparate(
            GL14.GL_SRC_ALPHA,
            GL14.GL_ONE_MINUS_SRC_ALPHA,
            GL14.GL_ONE,
            GL14.GL_ONE_MINUS_SRC_ALPHA
        );
        GlStateManager._disableBlend();
    }
    
    /**
     * Gets a render layer for the blend mode.
     * Note: All modes use the same base layer - blend is applied via applyBlendMode().
     */
    public static RenderLayer forBlendMode(String blendMode) {
        return SOLID_TRANSLUCENT;
    }
    
}
