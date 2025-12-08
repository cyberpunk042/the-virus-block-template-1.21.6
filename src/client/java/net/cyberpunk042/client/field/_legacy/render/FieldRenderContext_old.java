package net.cyberpunk042.client.field._legacy.render;

import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.visual.color.ColorTheme;
import net.cyberpunk042.visual.transform.TransformStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

/**
 * Context object for field rendering operations.
 * 
 * <p>Bundles all rendering parameters into a single object
 * to keep method signatures clean and allow easy extension.
 * 
 * <h2>Usage</h2>
 * <pre>
 * FieldRenderContext_old ctx = FieldRenderContext_old.builder()
 *     .matrices(matrices)
 *     .consumers(consumers)
 *     .position(fieldPos)
 *     .definition(def)
 *     .theme(theme)
 *     .tickDelta(tickDelta)
 *     .light(light)
 *     .build();
 * 
 * renderer.render(ctx);
 * </pre>
 */
public final class FieldRenderContext_old {
    
    // ─────────────────────────────────────────────────────────────────────────
    // Core Rendering State
    // ─────────────────────────────────────────────────────────────────────────
    
    private final MatrixStack matrices;
    private final VertexConsumerProvider consumers;
    private final TransformStack transformStack;
    
    // ─────────────────────────────────────────────────────────────────────────
    // Field State
    // ─────────────────────────────────────────────────────────────────────────
    
    private final Vec3d position;
    private final FieldDefinition definition;
    private final ColorTheme theme;
    
    // ─────────────────────────────────────────────────────────────────────────
    // Timing & Lighting
    // ─────────────────────────────────────────────────────────────────────────
    
    private final float tickDelta;
    private final long worldTime;
    private final int light;
    
    // ─────────────────────────────────────────────────────────────────────────
    // Optional State
    // ─────────────────────────────────────────────────────────────────────────
    
    private final float alpha;
    private final boolean debugMode;
    
    private FieldRenderContext_old(Builder builder) {
        this.matrices = builder.matrices;
        this.consumers = builder.consumers;
        this.transformStack = builder.transformStack != null 
            ? builder.transformStack 
            : new TransformStack();
        this.position = builder.position;
        this.definition = builder.definition;
        this.theme = builder.theme;
        this.tickDelta = builder.tickDelta;
        this.worldTime = builder.worldTime;
        this.light = builder.light;
        this.alpha = builder.alpha;
        this.debugMode = builder.debugMode;
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Getters
    // ─────────────────────────────────────────────────────────────────────────
    
    public MatrixStack matrices() { return matrices; }
    public VertexConsumerProvider consumers() { return consumers; }
    public TransformStack transforms() { return transformStack; }
    public Vec3d position() { return position; }
    public FieldDefinition definition() { return definition; }
    public ColorTheme theme() { return theme; }
    public float tickDelta() { return tickDelta; }
    public long worldTime() { return worldTime; }
    public int light() { return light; }
    public float alpha() { return alpha; }
    public boolean debugMode() { return debugMode; }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Computed Properties
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Returns the animation time in seconds.
     */
    public float animationTime() {
        return (worldTime + tickDelta) / 20.0f;
    }
    
    /**
     * Returns full brightness light value.
     */
    public int fullBright() {
        return 15728880;
    }
    
    /**
     * Creates a child context with adjusted alpha.
     */
    public FieldRenderContext_old withAlpha(float newAlpha) {
        return new Builder(this).alpha(newAlpha).build();
    }
    
    /**
     * Creates a child context with a transform pushed.
     */
    public FieldRenderContext_old withTransform(net.cyberpunk042.visual.transform.Transform transform) {
        FieldRenderContext_old child = new Builder(this).build();
        child.transformStack.push(transform);
        return child;
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Builder
    // ─────────────────────────────────────────────────────────────────────────
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private MatrixStack matrices;
        private VertexConsumerProvider consumers;
        private TransformStack transformStack;
        private Vec3d position = Vec3d.ZERO;
        private FieldDefinition definition;
        private ColorTheme theme;
        private float tickDelta = 0;
        private long worldTime = 0;
        private int light = 15728880;
        private float alpha = 1.0f;
        private boolean debugMode = false;
        
        public Builder() {}
        
        /** Copy constructor for child contexts */
        public Builder(FieldRenderContext_old ctx) {
            this.matrices = ctx.matrices;
            this.consumers = ctx.consumers;
            this.transformStack = ctx.transformStack;
            this.position = ctx.position;
            this.definition = ctx.definition;
            this.theme = ctx.theme;
            this.tickDelta = ctx.tickDelta;
            this.worldTime = ctx.worldTime;
            this.light = ctx.light;
            this.alpha = ctx.alpha;
            this.debugMode = ctx.debugMode;
        }
        
        public Builder matrices(MatrixStack matrices) {
            this.matrices = matrices;
            return this;
        }
        
        public Builder consumers(VertexConsumerProvider consumers) {
            this.consumers = consumers;
            return this;
        }
        
        public Builder transformStack(TransformStack stack) {
            this.transformStack = stack;
            return this;
        }
        
        public Builder position(Vec3d position) {
            this.position = position;
            return this;
        }
        
        public Builder definition(FieldDefinition definition) {
            this.definition = definition;
            return this;
        }
        
        public Builder theme(ColorTheme theme) {
            this.theme = theme;
            return this;
        }
        
        public Builder tickDelta(float tickDelta) {
            this.tickDelta = tickDelta;
            return this;
        }
        
        public Builder worldTime(long worldTime) {
            this.worldTime = worldTime;
            return this;
        }
        
        public Builder light(int light) {
            this.light = light;
            return this;
        }
        
        public Builder alpha(float alpha) {
            this.alpha = alpha;
            return this;
        }
        
        public Builder debugMode(boolean debugMode) {
            this.debugMode = debugMode;
            return this;
        }
        
        public FieldRenderContext_old build() {
            if (matrices == null) {
                throw new IllegalStateException("MatrixStack is required");
            }
            if (consumers == null) {
                throw new IllegalStateException("VertexConsumerProvider is required");
            }
            return new FieldRenderContext_old(this);
        }
    }
}
