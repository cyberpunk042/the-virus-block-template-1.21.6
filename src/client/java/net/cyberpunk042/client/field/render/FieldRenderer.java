package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.render.FieldRenderLayers;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.field.instance.FieldInstance;
import net.cyberpunk042.field.instance.LifecycleState;
import net.cyberpunk042.field.influence.BindingConfig;
import net.cyberpunk042.field.influence.BindingResolver;
import net.cyberpunk042.field.influence.TriggerEffect;
import net.cyberpunk042.field.influence.TriggerProcessor;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.color.ColorTheme;
import net.cyberpunk042.visual.color.ColorThemeRegistry;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Map;

/**
 * Main renderer for field definitions.
 * 
 * <p>Per CLASS_DIAGRAM §8: FieldRenderer is the entry point for rendering
 * a complete field. It resolves the color theme, iterates through layers,
 * and delegates to {@link LayerRenderer} for each.
 * 
 * <h2>Rendering Pipeline</h2>
 * <pre>
 * FieldRenderer.render(definition, ...)
 *    │
 *    ├── Resolve ColorTheme
 *    ├── Create ColorResolver
 *    │
 *    └── for each layer:
 *           └── LayerRenderer.render(layer, ...)
 *                  │
 *                  └── for each primitive:
 *                         └── PrimitiveRenderer.render(primitive, ...)
 * </pre>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // In WorldRenderEvents.AFTER_ENTITIES:
 * FieldRenderer.render(
 *     matrices,
 *     vertexConsumers,
 *     definition,
 *     position,
 *     scale,
 *     worldTime,
 *     alpha
 * );
 * </pre>
 * 
 * @see LayerRenderer
 * @see PrimitiveRenderer
 */
public final class FieldRenderer {
    
    private FieldRenderer() {}
    
    // =========================================================================
    // Main Render Methods
    // =========================================================================
    
    /**
     * Renders a field definition at the given position.
     * 
     * @param matrices Matrix stack
     * @param consumers Vertex consumer provider
     * @param definition Field definition to render
     * @param position World position (camera-relative)
     * @param scale Overall scale
     * @param time Animation time (world time + tick delta)
     * @param alpha Overall alpha (0-1)
     */
    public static void render(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldDefinition definition,
            Vec3d position,
            float scale,
            float time,
            float alpha) {
        
        render(matrices, consumers, definition, position, scale, time, alpha, null);
    }
    
    /**
     * Renders a field instance with lifecycle-aware alpha.
     * 
     * <p>F181: Applies fadeProgress from the instance's lifecycle state:
     * <ul>
     *   <li>SPAWNING: alpha scales from 0 → 1 as fadeProgress increases</li>
     *   <li>ACTIVE: full alpha (fadeProgress = 1.0)</li>
     *   <li>DESPAWNING: alpha scales from 1 → 0 as fadeProgress increases</li>
     *   <li>COMPLETE: alpha = 0 (not rendered)</li>
     * </ul>
     * 
     * @param matrices Matrix stack
     * @param consumers Vertex consumer provider
     * @param definition Field definition to render
     * @param instance Field instance with lifecycle state
     * @param position World position (camera-relative)
     * @param time Animation time
     */
    public static void renderInstance(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldDefinition definition,
            FieldInstance instance,
            Vec3d position,
            float time) {
        
        if (instance == null || instance.isComplete()) {
            return;
        }
        
        // F181: Calculate effective alpha from lifecycle state
        float effectiveAlpha = calculateLifecycleAlpha(instance);
        
        if (effectiveAlpha <= 0.01f) {
            return;
        }
        
        render(matrices, consumers, definition, position, 
               instance.scale(), time, effectiveAlpha, null);
        
        Logging.FIELD.topic("render").trace(
            "Rendered instance {} (state={}, fadeProgress={:.2f}, alpha={:.2f})",
            instance.id(), instance.lifecycleState(), 
            instance.fadeProgress(), effectiveAlpha);
    }
    
    /**
     * F181: Calculates effective alpha based on lifecycle state and fadeProgress.
     * 
     * @param instance The field instance
     * @return Effective alpha (0-1)
     */
    private static float calculateLifecycleAlpha(FieldInstance instance) {
        float baseAlpha = instance.alpha();
        float fadeProgress = instance.fadeProgress();
        LifecycleState state = instance.lifecycleState();
        
        return switch (state) {
            case SPAWNING -> baseAlpha * fadeProgress;           // Fade in
            case ACTIVE -> baseAlpha;                            // Full alpha
            case DESPAWNING -> baseAlpha * (1.0f - fadeProgress); // Fade out
            case COMPLETE -> 0.0f;                               // Invisible
        };
    }
    
    // =========================================================================
    // F182: Binding Evaluation
    // =========================================================================
    
    /**
     * Renders a field with bindings evaluated against the player.
     * 
     * <p>F182: Bindings dynamically modify render properties:
     * <ul>
     *   <li>"alpha" → alpha multiplier (e.g., fade based on health)</li>
     *   <li>"scale" → scale multiplier (e.g., grow based on damage)</li>
     * </ul>
     * 
     * @param matrices Matrix stack
     * @param consumers Vertex consumer provider
     * @param definition Field definition with bindings
     * @param player Player for binding sources
     * @param position World position
     * @param scale Base scale
     * @param time Animation time
     * @param alpha Base alpha
     * @param fieldAge Field age in ticks
     */
    public static void renderWithBindings(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldDefinition definition,
            PlayerEntity player,
            Vec3d position,
            float scale,
            float time,
            float alpha,
            int fieldAge) {
        
        // Evaluate bindings
        RenderOverrides overrides = evaluateBindings(definition, player, fieldAge);
        
        // Apply binding results
        float effectiveScale = scale * overrides.scaleMultiplier();
        float effectiveAlpha = alpha * overrides.alphaMultiplier();
        
        render(matrices, consumers, definition, position, effectiveScale, time, effectiveAlpha, overrides);
        
        if (overrides.hasOverrides()) {
            Logging.FIELD.topic("binding").trace(
                "Applied bindings: alpha×{:.2f}, scale×{:.2f}", 
                overrides.alphaMultiplier(), overrides.scaleMultiplier());
        }
    }
    
    /**
     * Evaluates all bindings in a definition and returns RenderOverrides.
     * 
     * @param definition Field definition with bindings
     * @param player Player for binding sources
     * @param fieldAge Field age in ticks
     * @return RenderOverrides with evaluated binding values
     */
    public static RenderOverrides evaluateBindings(
            FieldDefinition definition, 
            PlayerEntity player, 
            int fieldAge) {
        
        Map<String, BindingConfig> bindings = definition.bindings();
        if (bindings == null || bindings.isEmpty()) {
            return RenderOverrides.NONE;
        }
        
        // Evaluate all bindings
        Map<String, Float> results = BindingResolver.evaluateAll(bindings, player, fieldAge);
        
        // Build overrides from results
        RenderOverrides.Builder builder = RenderOverrides.builder();
        
        // Apply known binding targets
        if (results.containsKey("alpha")) {
            builder.alphaMultiplier(results.get("alpha"));
        }
        if (results.containsKey("scale")) {
            builder.scaleMultiplier(results.get("scale"));
        }
        
        return builder.build();
    }
    
    // =========================================================================
    // F183: Trigger Effect Application
    // =========================================================================
    
    /**
     * Renders a field with active trigger effects applied.
     * 
     * <p>F183: Trigger effects modify the render state:
     * <ul>
     *   <li>PULSE → scale multiplier</li>
     *   <li>SHAKE → position offset</li>
     *   <li>FLASH → color overlay with intensity</li>
     *   <li>GLOW → glow intensity multiplier</li>
     *   <li>COLOR_SHIFT → temporary color override</li>
     * </ul>
     * 
     * @param matrices Matrix stack
     * @param consumers Vertex consumer provider
     * @param definition Field definition
     * @param triggerProcessor Active trigger processor
     * @param position World position
     * @param scale Base scale
     * @param time Animation time
     * @param alpha Base alpha
     */
    public static void renderWithTriggers(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldDefinition definition,
            TriggerProcessor triggerProcessor,
            Vec3d position,
            float scale,
            float time,
            float alpha) {
        
        if (triggerProcessor == null || !triggerProcessor.hasActiveTriggers()) {
            render(matrices, consumers, definition, position, scale, time, alpha, null);
            return;
        }
        
        // Apply trigger effects
        RenderOverrides overrides = applyTriggerEffects(triggerProcessor);
        
        // Apply PULSE scale
        float pulseScale = triggerProcessor.getEffectValue(TriggerEffect.PULSE);
        float effectiveScale = scale * (pulseScale > 0 ? pulseScale : 1.0f);
        
        // Apply SHAKE offset
        float[] shakeOffset = triggerProcessor.getShakeOffset();
        Vec3d effectivePosition = position.add(shakeOffset[0], shakeOffset[1], shakeOffset[2]);
        
        render(matrices, consumers, definition, effectivePosition, effectiveScale, time, alpha, overrides);
        
        Logging.FIELD.topic("trigger").trace(
            "Applied triggers: pulse={:.2f}, shake=[{:.2f},{:.2f},{:.2f}]",
            pulseScale, shakeOffset[0], shakeOffset[1], shakeOffset[2]);
    }
    
    /**
     * Creates RenderOverrides from active trigger effects.
     */
    private static RenderOverrides applyTriggerEffects(TriggerProcessor processor) {
        RenderOverrides.Builder builder = RenderOverrides.builder();
        
        // FLASH: color overlay with intensity as alpha multiplier
        float flashIntensity = processor.getFlashIntensity();
        String flashColor = processor.getFlashColor();
        if (flashIntensity > 0 && flashColor != null) {
            int color = parseFlashColor(flashColor, flashIntensity);
            builder.colorOverride(color);
        }
        
        // GLOW: Would need glow support in RenderOverrides (future)
        // float glowIntensity = processor.getEffectValue(TriggerEffect.GLOW);
        
        return builder.build();
    }
    
    /**
     * Parses a flash color string and applies intensity to alpha.
     */
    private static int parseFlashColor(String colorStr, float intensity) {
        try {
            String hex = colorStr.startsWith("#") ? colorStr.substring(1) : colorStr;
            int rgb = Integer.parseUnsignedInt(hex, 16);
            int alpha = (int) (intensity * 255) & 0xFF;
            return (alpha << 24) | (rgb & 0x00FFFFFF);
        } catch (NumberFormatException e) {
            return 0x80FF0000; // Default red with 50% alpha
        }
    }
    
    /**
     * Renders a field definition with optional overrides.
     */
    public static void render(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldDefinition definition,
            Vec3d position,
            float scale,
            float time,
            float alpha,
            RenderOverrides overrides) {
        
        // Early exit checks
        if (definition == null || alpha <= 0.01f) {
            return;
        }
        
        if (definition.layers() == null || definition.layers().isEmpty()) {
            Logging.FIELD.topic("render").trace(
                "Definition '{}' has no layers, skipping", definition.id());
            return;
        }
        
        // === PHASE 1: Resolve Theme ===
        ColorTheme theme = resolveTheme(definition);
        ColorResolver resolver = theme != null 
            ? ColorResolver.fromTheme(theme) 
            : ColorResolver.DEFAULT;
        
        // === PHASE 2: Get Render Layer ===
        RenderLayer renderLayer = FieldRenderLayers.solidTranslucent();
        VertexConsumer consumer = consumers.getBuffer(renderLayer);
        
        // === PHASE 3: Position ===
        matrices.push();
        matrices.translate(position.x, position.y, position.z);
        
        // === PHASE 3.5: Apply Field Modifiers (bobbing, breathing) ===
        if (definition.modifiers() != null && definition.modifiers().hasAnimationModifiers()) {
            net.cyberpunk042.client.visual.animation.AnimationApplier.applyModifiers(
                matrices, definition.modifiers(), time);
        }
        
        // === PHASE 4: Render Each Layer ===
        int layerCount = 0;
        for (FieldLayer layer : definition.layers()) {
            LayerRenderer.render(
                matrices,
                consumer,
                layer,
                resolver,
                scale,
                time,
                alpha,
                overrides
            );
            layerCount++;
        }
        
        matrices.pop();
        
        Logging.FIELD.topic("render").trace(
            "Rendered field '{}' with {} layers at {}", 
            definition.id(), layerCount, position);
    }
    
    // =========================================================================
    // Theme Resolution
    // =========================================================================
    
    /**
     * Resolves the color theme for a field definition.
     */
    private static ColorTheme resolveTheme(FieldDefinition definition) {
        // Try definition's explicit theme
        String themeId = definition.themeId();
        if (themeId != null && !themeId.isEmpty()) {
            ColorTheme theme = ColorThemeRegistry.get(themeId);
            if (theme != null) {
                return theme;
            }
            Logging.FIELD.topic("render").trace(
                "Theme '{}' not found, using default", themeId);
        }
        
        // Fall back to default theme
        return ColorThemeRegistry.getOrDefault("cyber_green", ColorTheme.CYBER_GREEN);
    }
    
    // =========================================================================
    // Static Utilities
    // =========================================================================
    
    /**
     * Checks if a definition can be rendered.
     */
    public static boolean canRender(FieldDefinition definition) {
        return definition != null 
            && definition.layers() != null 
            && !definition.layers().isEmpty();
    }
}

