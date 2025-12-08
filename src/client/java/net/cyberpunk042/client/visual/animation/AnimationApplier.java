package net.cyberpunk042.client.visual.animation;

import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.animation.SpinConfig;
import net.cyberpunk042.visual.animation.PulseConfig;
import net.cyberpunk042.visual.animation.WobbleConfig;
import net.cyberpunk042.visual.animation.WaveConfig;
import net.cyberpunk042.visual.animation.AlphaPulseConfig;
import net.cyberpunk042.visual.animation.Waveform;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Applies {@link Animation} effects to a {@link MatrixStack}.
 * 
 * <p>Animation effects:</p>
 * <ul>
 *   <li>Spin - Continuous rotation</li>
 *   <li>Pulse - Scale oscillation</li>
 *   <li>Wobble - Random offset (future)</li>
 *   <li>Wave - Sinusoidal deformation (future)</li>
 * </ul>
 * 
 * @see Animation
 * @see SpinConfig
 * @see PulseConfig
 */
public final class AnimationApplier {
    
    private AnimationApplier() {}
    
    // =========================================================================
    // Full Application
    // =========================================================================
    
    /**
     * Applies all animation effects to the matrix stack.
     * 
     * @param matrices The matrix stack to modify
     * @param animation The animation configuration
     * @param time Current time in ticks
     */
    public static void apply(MatrixStack matrices, Animation animation, float time) {
        if (animation == null || !animation.isActive()) {
            return;
        }
        
        // Apply phase offset
        float effectiveTime = time + (animation.phase() * 100);  // phase is 0-1
        
        // 1. Spin
        if (animation.hasSpin()) {
            applySpin(matrices, animation.spin(), effectiveTime);
        }
        
        // 2. Pulse (scale)
        if (animation.hasPulse()) {
            applyPulse(matrices, animation.pulse(), effectiveTime);
        }
        
        // 3. Wobble (future)
        if (animation.hasWobble()) {
            applyWobble(matrices, animation.wobble(), effectiveTime);
        }
        
        // 4. Wave (future)
        if (animation.hasWave()) {
            // Wave affects vertices, not matrix - handled in renderer
        }
    }
    
    // =========================================================================
    // Individual Effects
    // =========================================================================
    
    /**
     * Applies spin rotation.
     */
    public static void applySpin(MatrixStack matrices, SpinConfig spin, float time) {
        if (spin == null || !spin.isActive()) return;
        
        float angle;
        if (spin.oscillate()) {
            // Oscillate within range
            float progress = (float) Math.sin(time * spin.speed());
            angle = progress * spin.range() / 2;
        } else {
            // Continuous rotation
            angle = time * spin.speed();
        }
        
        Quaternionf rotation = switch (spin.axis()) {
            case X -> new Quaternionf().rotateX(angle);
            case Y -> new Quaternionf().rotateY(angle);
            case Z -> new Quaternionf().rotateZ(angle);
            case CUSTOM -> {
                if (spin.customAxis() != null) {
                    Vector3f axis = spin.customAxis();
                    yield new Quaternionf().rotateAxis(angle, axis.x, axis.y, axis.z);
                }
                yield new Quaternionf().rotateY(angle);
            }
        };
        
        matrices.multiply(rotation);
        Logging.ANIMATION.topic("spin").trace("Spin: axis={}, angle={}", spin.axis(), Math.toDegrees(angle));
    }
    
    /**
     * Applies pulse scaling.
     * Uses {@link Waveform#evaluate(float)} for waveform calculation.
     */
    public static void applyPulse(MatrixStack matrices, PulseConfig pulse, float time) {
        if (pulse == null || !pulse.isActive()) return;
        
        // Use Waveform.evaluate() for clean waveform calculation (returns 0-1)
        float normalizedWave = pulse.waveform().evaluate(time * pulse.speed());
        
        // Map wave (0 to 1) to scale range (min to max)
        float scale = pulse.min() + normalizedWave * (pulse.max() - pulse.min());
        
        matrices.scale(scale, scale, scale);
        Logging.ANIMATION.topic("pulse").trace("Pulse: scale={}", scale);
    }
    
    /**
     * Applies wobble offset.
     */
    public static void applyWobble(MatrixStack matrices, WobbleConfig wobble, float time) {
        if (wobble == null || !wobble.isActive()) return;
        
        Vector3f amplitude = wobble.amplitude();
        if (amplitude == null) return;
        
        // Generate pseudo-random wobble
        float wobbleX = (float) Math.sin(time * wobble.speed() * 1.0) * amplitude.x;
        float wobbleY = (float) Math.sin(time * wobble.speed() * 1.3) * amplitude.y;
        float wobbleZ = (float) Math.sin(time * wobble.speed() * 0.7) * amplitude.z;
        
        matrices.translate(wobbleX, wobbleY, wobbleZ);
        Logging.ANIMATION.topic("wobble").trace("Wobble: ({}, {}, {})", wobbleX, wobbleY, wobbleZ);
    }
    
    // =========================================================================
    // Alpha Pulse
    // =========================================================================
    
    /**
     * Calculates alpha pulse value (doesn't affect matrix).
     * Uses {@link Waveform#evaluate(float)} for waveform calculation.
     * 
     * @param alphaPulse The alpha pulse config
     * @param time Current time
     * @return Alpha multiplier (min to max)
     */
    public static float getAlphaPulse(AlphaPulseConfig alphaPulse, float time) {
        if (alphaPulse == null || !alphaPulse.isActive()) {
            return 1.0f;
        }
        
        // Use Waveform.evaluate() for clean calculation (returns 0-1)
        float normalizedWave = alphaPulse.waveform().evaluate(time * alphaPulse.speed());
        
        return alphaPulse.min() + normalizedWave * (alphaPulse.max() - alphaPulse.min());
    }
}
