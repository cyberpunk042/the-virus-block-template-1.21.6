package net.cyberpunk042.visual.transform;

import net.cyberpunk042.log.Logging;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Provides time-based interpolation between transforms.
 * 
 * <h2>Usage</h2>
 * <pre>
 * AnimatedTransform anim = AnimatedTransform.between(start, end, 20); // 20 ticks
 * Transform current = anim.getTransform(worldTime);
 * </pre>
 */
public final class AnimatedTransform {
    
    private final Transform start;
    private final Transform end;
    private final float startTime;
    private final float duration;
    private final EasingFunction easing;
    
    private AnimatedTransform(Transform start, Transform end, float startTime, float duration, EasingFunction easing) {
        this.start = start;
        this.end = end;
        this.startTime = startTime;
        this.duration = duration;
        this.easing = easing;
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Factory Methods
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Creates an animation between two transforms.
     * 
     * @param start starting transform
     * @param end ending transform
     * @param duration duration in ticks
     */
    public static AnimatedTransform between(Transform start, Transform end, float duration) {
        return new AnimatedTransform(start, end, 0, duration, EasingFunction.LINEAR);
    }
    
    /**
     * Creates an animation with custom start time.
     */
    public static AnimatedTransform between(Transform start, Transform end, float startTime, float duration) {
        return new AnimatedTransform(start, end, startTime, duration, EasingFunction.LINEAR);
    }
    
    /**
     * Creates an animation with easing.
     */
    public static AnimatedTransform between(Transform start, Transform end, float duration, EasingFunction easing) {
        return new AnimatedTransform(start, end, 0, duration, easing);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Animation
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Gets the interpolated transform at the given time.
     * 
     * @param time current world time (ticks + partial)
     * @return interpolated transform
     */
    public Transform getTransform(float time) {
        float elapsed = time - startTime;
        float t = MathHelper.clamp(elapsed / duration, 0, 1);
        float easedT = easing.apply(t);
        
        Logging.RENDER.topic("anim-transform").trace(
            "Interpolating: time={:.1f}, progress={:.2f}, eased={:.2f}",
            time, t, easedT);
        
        return lerp(start, end, easedT);
    }
    
    /**
     * Checks if the animation is complete.
     */
    public boolean isComplete(float time) {
        return time >= startTime + duration;
    }
    
    /**
     * Gets progress (0.0 to 1.0).
     */
    public float getProgress(float time) {
        float elapsed = time - startTime;
        return MathHelper.clamp(elapsed / duration, 0, 1);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Interpolation
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Linearly interpolates between two transforms.
     */
    public static Transform lerp(Transform a, Transform b, float t) {
        Vec3d offsetA = a.offset() != null ? a.offset() : Vec3d.ZERO;
        Vec3d offsetB = b.offset() != null ? b.offset() : Vec3d.ZERO;
        Vec3d offset = offsetA.lerp(offsetB, t);
        
        Vec3d rotA = a.rotation() != null ? a.rotation() : Vec3d.ZERO;
        Vec3d rotB = b.rotation() != null ? b.rotation() : Vec3d.ZERO;
        Vec3d rotation = rotA.lerp(rotB, t);
        
        float scale = MathHelper.lerp(t, a.scale(), b.scale());
        
        return new Transform(offset, rotation, scale);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Easing Functions
    // ─────────────────────────────────────────────────────────────────────────
    
    @FunctionalInterface
    public interface EasingFunction {
        float apply(float t);
        
        EasingFunction LINEAR = t -> t;
        EasingFunction EASE_IN = t -> t * t;
        EasingFunction EASE_OUT = t -> 1 - (1 - t) * (1 - t);
        EasingFunction EASE_IN_OUT = t -> t < 0.5f 
            ? 2 * t * t 
            : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
        EasingFunction BOUNCE = t -> {
            float n1 = 7.5625f;
            float d1 = 2.75f;
            if (t < 1 / d1) return n1 * t * t;
            if (t < 2 / d1) return n1 * (t -= 1.5f / d1) * t + 0.75f;
            if (t < 2.5 / d1) return n1 * (t -= 2.25f / d1) * t + 0.9375f;
            return n1 * (t -= 2.625f / d1) * t + 0.984375f;
        };
    }
}
