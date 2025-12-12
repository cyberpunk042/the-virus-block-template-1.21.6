package net.cyberpunk042.client.field._legacy.render;
import net.cyberpunk042.client.visual._legacy.render.RenderOverrides_old;
import net.cyberpunk042.client.visual.render.FieldRenderLayers;
import net.cyberpunk042.client.visual._legacy.render.LayerRenderer_old;

import net.cyberpunk042.client.render.util.BeaconBeamRenderer;
import net.cyberpunk042.field.BeamConfig;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.color.ColorTheme;
import net.cyberpunk042.visual.color.ColorThemeRegistry;
import net.cyberpunk042.visual.pattern.VertexPattern;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.cyberpunk042.log.LogScope;
import net.cyberpunk042.log.LogLevel;

/**
 * Main renderer for field definitions.
 * 
 * <p>Coordinates the rendering of all layers in a field definition,
 * resolving themes and applying overall transformations.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // In a render method:
 * FieldRenderer_old.render(
 *     matrices,
 *     vertexConsumers,
 *     definition,
 *     position,
 *     scale,
 *     worldTime,
 *     alpha
 * );
 * </pre>
 */
public final class FieldRenderer_old {
    
    private FieldRenderer_old() {}
    
    /**
     * Renders a field definition at the given position.
     * 
     * @param matrices Matrix stack
     * @param consumers Vertex consumer provider
     * @param definition Field definition to render
     * @param position World position (already camera-relative if needed)
     * @param scale Overall scale
     * @param time Animation time (world time + tick delta)
     * @param alpha Overall alpha (0-1)
     */
    /** Counter for render stats logging (reset every 1000 renders) */
    private static int renderCount = 0;
    private static long lastStatsLog = 0;
    
    public static void render(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldDefinition definition,
            Vec3d position,
            float scale,
            float time,
            float alpha) {
        
        if (definition == null || definition.layers().isEmpty() || alpha <= 0.01f) {
            return;
        }
        
        // Resolve theme
        ColorTheme theme = resolveTheme(definition);
        
        // Get render layer
        RenderLayer layer = FieldRenderLayers.solidTranslucent();
        VertexConsumer consumer = consumers.getBuffer(layer);
        
        // Position
        matrices.push();
        matrices.translate(position.x, position.y, position.z);
        
        // Render each layer
        int layerCount = definition.layers().size();
        for (FieldLayer fieldLayer : definition.layers()) {
            LayerRenderer_old.render(
                matrices,
                consumer,
                fieldLayer,
                theme,
                scale,
                time,
                alpha
            );
        }
        
        matrices.pop();
        
        // Render beacon-style beam if enabled
        if (definition.hasBeam()) {
            renderBeam(matrices, consumers, definition.beam(), position);
        }
        
        // Stats logging (every ~5 seconds at 20 TPS)
        renderCount++;
        if (renderCount >= 100) {
            long now = System.currentTimeMillis();
            if (now - lastStatsLog > 5000) {
                Logging.RENDER.topic("stats").debug(
                    "Rendered {} fields ({} layers total)", 
                    renderCount, renderCount * layerCount);
                lastStatsLog = now;
            }
            renderCount = 0;
        }
    }
    
    /**
     * Renders a field definition with a custom texture.
     */
    public static void render(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldDefinition definition,
            Vec3d position,
            float scale,
            float time,
            float alpha,
            Identifier texture) {
        
        if (definition == null || definition.layers().isEmpty() || alpha <= 0.01f) {
            return;
        }
        
        ColorTheme theme = resolveTheme(definition);
        RenderLayer layer = FieldRenderLayers.solidTranslucent(texture);
        VertexConsumer consumer = consumers.getBuffer(layer);
        
        matrices.push();
        matrices.translate(position.x, position.y, position.z);
        
        for (FieldLayer fieldLayer : definition.layers()) {
            LayerRenderer_old.render(matrices, consumer, fieldLayer, theme, scale, time, alpha);
        }
        
        matrices.pop();
    }
    
    /**
     * Renders without position translation (caller handles positioning).
     */
    public static void renderLocal(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldDefinition definition,
            float scale,
            float time,
            float alpha) {
        
        if (definition == null || definition.layers().isEmpty() || alpha <= 0.01f) {
            return;
        }
        
        ColorTheme theme = resolveTheme(definition);
        RenderLayer layer = FieldRenderLayers.solidTranslucent();
        VertexConsumer consumer = consumers.getBuffer(layer);
        
        for (FieldLayer fieldLayer : definition.layers()) {
            LayerRenderer_old.render(matrices, consumer, fieldLayer, theme, scale, time, alpha);
        }
    }
    
    // =========================================================================
    // Render with Overrides (Debug/Test Mode)
    // =========================================================================
    
    /**
     * Renders a field with runtime overrides for debugging.
     * 
     * <p>This method accepts additional parameters that override the definition's
     * values, allowing live editing of tessellation, algorithm, and visual properties.
     * 
     * @param matrices Matrix stack
     * @param consumers Vertex consumer provider
     * @param definition Field definition to render
     * @param position World position (camera-relative)
     * @param scale Effective scale (radius × visual scale)
     * @param time Animation time
     * @param alpha Overall alpha
     * @param glow Glow intensity override
     * @param latSteps Latitude steps override (0 = use definition)
     * @param lonSteps Longitude steps override (0 = use definition)
     * @param algorithm Algorithm override (null = use definition)
     * @param vertexPattern Vertex pattern override (null = use definition)
     * @param colorRef Color reference override (null = use definition)
     */
    public static void renderWithOverrides(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldDefinition definition,
            Vec3d position,
            float scale,
            float time,
            float alpha,
            float glow,
            int latSteps,
            int lonSteps,
            String algorithm,
            VertexPattern vertexPattern,
            String colorRef) {
        
        if (definition == null || definition.layers().isEmpty() || alpha <= 0.01f) {
            // DIAGNOSTIC: Log why we're bailing out
            if ((int) time % 20 == 0) {
                Logging.RENDER.topic("fieldtest").warn(
                    "DIAG: renderWithOverrides skipping: def={} layers={} alpha={:.2f}",
                    definition != null, 
                    definition != null ? definition.layers().size() : 0, 
                    alpha);
            }
            return;
        }
        
        // DIAGNOSTIC: Log that we're actually rendering
        if ((int) time % 60 == 0) {
            Logging.RENDER.topic("fieldtest").info(
                "DIAG: renderWithOverrides ACTIVE: def={} layers={} scale={:.2f} pos=({:.1f},{:.1f},{:.1f})",
                definition.id().getPath(), definition.layers().size(), scale,
                position.x, position.y, position.z);
        }
        
        // Build overrides
        RenderOverrides_old overrides = RenderOverrides_old.builder()
            .glow(glow)
            .latSteps(latSteps)
            .lonSteps(lonSteps)
            .algorithm(algorithm)
            .vertexPattern(vertexPattern)
            .colorOverride(colorRef)
            .build();
        
        // Resolve theme
        ColorTheme theme = resolveTheme(definition);
        
        // Get render layer
        RenderLayer layer = FieldRenderLayers.solidTranslucent();
        VertexConsumer consumer = consumers.getBuffer(layer);
        
        // Position (scale is applied in LayerRenderer_old)
        matrices.push();
        matrices.translate(position.x, position.y, position.z);
        
        // Render each layer with overrides
        try (LogScope scope = Logging.RENDER.topic("fieldtest").scope("process-layers", LogLevel.INFO)) {
            for (FieldLayer fieldLayer : definition.layers()) {
                // DIAGNOSTIC: Log each layer being rendered
                if ((int) time % 60 == 0) {
                    scope.branch("fieldLayer").kv("_lvl", "i").kv("fieldLayer", fieldLayer.id()).kv("fieldLayer", fieldLayer.primitives().size());
                }

                LayerRenderer_old.renderWithOverrides(
                    matrices,
                    consumer,
                    fieldLayer,
                    theme,
                    scale,
                    time,
                    alpha,
                    overrides
                );
            }
        }
        
        matrices.pop();
        
        // Render beam if enabled
        if (definition.hasBeam()) {
            renderBeam(matrices, consumers, definition.beam(), position);
        }
    }
    
    /**
     * Renders with a RenderOverrides_old object.
     */
    public static void renderWithOverrides(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldDefinition definition,
            Vec3d position,
            float scale,
            float time,
            float alpha,
            RenderOverrides_old overrides) {
        
        // Extract values with null safety (convert to primitives with defaults)
        float glow = overrides != null && overrides.glow() != null ? overrides.glow() : 0.0f;
        int latSteps = overrides != null && overrides.latSteps() != null ? overrides.latSteps() : 0;
        int lonSteps = overrides != null && overrides.lonSteps() != null ? overrides.lonSteps() : 0;
        String algorithm = overrides != null ? overrides.algorithm() : null;
        VertexPattern vertexPattern = overrides != null ? overrides.vertexPattern() : null;
        String colorOverride = overrides != null ? overrides.colorOverride() : null;
        
        renderWithOverrides(
            matrices, consumers, definition, position, scale, time, alpha,
            glow,
            latSteps,
            lonSteps,
            algorithm,
            vertexPattern,
            colorOverride
        );
    }
    
    /**
     * Resolves the theme for a definition.
     */
    private static ColorTheme resolveTheme(FieldDefinition definition) {
        // Look up by theme ID
        String themeId = definition.themeId();
        if (themeId != null && !themeId.isEmpty()) {
            ColorTheme theme = ColorThemeRegistry.get(themeId);
            if (theme != null) {
                return theme;
            }
            Logging.RENDER.topic("theme").trace(
                "Theme '{}' not found, using default", themeId);
        }
        
        // Fallback to cyber green
        return ColorTheme.CYBER_GREEN;
    }
    
    /**
     * Renders a beacon-style beam extending upward from the field position.
     */
    private static void renderBeam(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            BeamConfig beam,
            Vec3d position) {
        
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        if (world == null) {
            return;
        }
        
        // Calculate beam height (from field center to world ceiling)
        double startY = position.y + 1.5; // Start slightly above field center
        int topY = world.getBottomY() + world.getDimension().height();
        int height = Math.max(4, topY - MathHelper.floor(startY));
        
        // Get timing for animation
        long worldTime = world.getTime();
        float tickDelta = client.getRenderTickCounter().getTickProgress(false);
        
        // Position beam at field location
        matrices.push();
        matrices.translate(position.x, position.y + 1.5, position.z);
        
        // Render using BeaconBeamRenderer
        BeaconBeamRenderer.render(
            matrices,
            consumers,
            beam.innerRadius(),
            beam.outerRadius(),
            height,
            worldTime,
            tickDelta,
            beam.innerColor(),
            beam.color()
        );
        
        matrices.pop();
        
        Logging.RENDER.topic("beam").trace(
            "Rendered beam at ({:.1f},{:.1f},{:.1f}) height={}",
            position.x, position.y, position.z, height);
    }
}
