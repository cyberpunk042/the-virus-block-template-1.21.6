package net.cyberpunk042.visual.animation._legacy;

import net.cyberpunk042.visual.transform.Transform;
import net.cyberpunk042.visual.transform.AnimatedTransform;
import org.joml.Vector3f;

/**
 * Applies animation effects to transforms.
 * 
 * <p>Handles time-based modifications like spin, pulse, and wobble.</p>
 * 
 * @see Animation
 * @see Transform
 */
/**
 * @deprecated Moved to _legacy. Use AnimationApplier instead.
 */
@Deprecated
public final class Animator {
    
    private Animator() {}
    
    /**
     * Applies animation effects to a transform.
     * @param transform Base transform
     * @param animation Animation configuration
     * @param time Current time in ticks
     * @return Modified transform with animation applied
     */
    public static Transform animate(Transform transform, Animation animation, float time) {
        if (animation == null || !animation.isActive()) {
            return transform;
        }
        
        Transform result = transform;
        
        // Apply phase offset to time
        float effectiveTime = time + (animation.phase() * 100);
        
        // Apply spin
        if (animation.hasSpin()) {
            result = applySpin(result, animation.spin(), effectiveTime);
        }
        
        // Apply pulse (scale)
        if (animation.hasPulse()) {
            result = applyPulse(result, animation.pulse(), effectiveTime);
        }
        
        // Apply wobble (offset)
        if (animation.hasWobble()) {
            result = applyWobble(result, animation.wobble(), effectiveTime);
        }
        
        return result;
    }
    
    /**
     * Applies spin rotation to a transform.
     */
    private static Transform applySpin(Transform t, SpinConfig spin, float time) {
        if (spin == null || !spin.isActive()) return t;
        
        float angle;
        if (spin.oscillate()) {
            float progress = (float) Math.sin(time * spin.speed());
            angle = progress * spin.range() / 2;
        } else {
            angle = time * spin.speed();
        }
        
        // Convert to degrees and apply to appropriate axis
        float degrees = (float) Math.toDegrees(angle);
        Vector3f currentRot = t.rotation() != null ? new Vector3f(t.rotation()) : new Vector3f();
        
        switch (spin.axis()) {
            case X -> currentRot.x += degrees;
            case Y -> currentRot.y += degrees;
            case Z -> currentRot.z += degrees;
            case CUSTOM -> {
                if (spin.customAxis() != null) {
                    // For custom axis, apply evenly to all components based on axis direction
                    Vector3f axis = spin.customAxis();
                    currentRot.add(axis.x * degrees, axis.y * degrees, axis.z * degrees);
                } else {
                    currentRot.y += degrees;
                }
            }
        }
        
        return Transform.builder()
            .anchor(t.anchor())
            .offset(t.offset())
            .rotation(currentRot)
            .inheritRotation(t.inheritRotation())
            .scale(t.scale())
            .scaleXYZ(t.scaleXYZ())
            .scaleWithRadius(t.scaleWithRadius())
            .facing(t.facing())
            .up(t.up())
            .billboard(t.billboard())
            .orbit(t.orbit())
            .build();
    }
    
    /**
     * Applies pulse scaling to a transform.
     */
    private static Transform applyPulse(Transform t, PulseConfig pulse, float time) {
        if (pulse == null || !pulse.isActive()) return t;
        
        float waveValue = pulse.waveform().evaluate(time * pulse.speed());
        float scale = pulse.min() + waveValue * (pulse.max() - pulse.min());
        
        return Transform.builder()
            .anchor(t.anchor())
            .offset(t.offset())
            .rotation(t.rotation())
            .inheritRotation(t.inheritRotation())
            .scale(t.scale() * scale)
            .scaleXYZ(t.scaleXYZ() != null ? new Vector3f(t.scaleXYZ()).mul(scale) : null)
            .scaleWithRadius(t.scaleWithRadius())
            .facing(t.facing())
            .up(t.up())
            .billboard(t.billboard())
            .orbit(t.orbit())
            .build();
    }
    
    /**
     * Applies wobble offset to a transform.
     */
    private static Transform applyWobble(Transform t, WobbleConfig wobble, float time) {
        if (wobble == null || !wobble.isActive()) return t;
        
        Vector3f amplitude = wobble.amplitude();
        if (amplitude == null) return t;
        
        float wobbleX = (float) Math.sin(time * wobble.speed() * 1.0) * amplitude.x;
        float wobbleY = (float) Math.sin(time * wobble.speed() * 1.3) * amplitude.y;
        float wobbleZ = (float) Math.sin(time * wobble.speed() * 0.7) * amplitude.z;
        
        Vector3f newOffset = t.offset() != null ? 
            new Vector3f(t.offset()).add(wobbleX, wobbleY, wobbleZ) :
            new Vector3f(wobbleX, wobbleY, wobbleZ);
        
        return Transform.builder()
            .anchor(t.anchor())
            .offset(newOffset)
            .rotation(t.rotation())
            .inheritRotation(t.inheritRotation())
            .scale(t.scale())
            .scaleXYZ(t.scaleXYZ())
            .scaleWithRadius(t.scaleWithRadius())
            .facing(t.facing())
            .up(t.up())
            .billboard(t.billboard())
            .orbit(t.orbit())
            .build();
    }
    
    /**
     * Interpolates between two transforms.
     * @param a Starting transform
     * @param b Ending transform
     * @param t Interpolation factor (0-1)
     * @return Interpolated transform
     */
    public static Transform lerp(Transform a, Transform b, float t) {
        return AnimatedTransform.lerp(a, b, t);
    }
}
