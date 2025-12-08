package net.cyberpunk042.visual.animation;

import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.Axis;

/**
 * Animation properties for a primitive.
 * 
 * <h2>Rotation Animation</h2>
 * <ul>
 *   <li><b>spin</b>: Rotation speed (radians per tick)</li>
 *   <li><b>spinAxis</b>: Axis to rotate around (X, Y, Z)</li>
 * </ul>
 * 
 * <h2>Scale Animation</h2>
 * <ul>
 *   <li><b>pulse</b>: Scale pulsing speed</li>
 *   <li><b>pulseAmount</b>: Scale pulsing amplitude</li>
 * </ul>
 * 
 * <h2>Alpha Animation</h2>
 * <ul>
 *   <li><b>alphaPulse</b>: Alpha oscillation speed</li>
 *   <li><b>alphaPulseAmount</b>: Alpha oscillation amplitude</li>
 * </ul>
 * 
 * <h2>Timing</h2>
 * <ul>
 *   <li><b>phase</b>: Initial phase offset (radians)</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * <pre>
 * Animation.none()                           // No animation
 * Animation.spinning(0.05f)                  // Slow Y-axis spin
 * Animation.spinning(0.05f, Axis.X)          // Pitch rotation
 * Animation.pulsing(0.1f, 0.2f)              // Scale pulse
 * Animation.alphaPulsing(0.1f, 0.3f)         // Fade in/out
 * </pre>
 */
public record Animation(
        float spin,
        float pulse,
        float pulseAmount,
        float phase,
        float alphaPulse,
        float alphaPulseAmount,
        Axis spinAxis
) {
    
    /** No animation. */
    public static final Animation NONE = new Animation(0, 0, 0, 0, 0, 0, Axis.Y);
    
    // =========================================================================
    // Factory Methods
    // =========================================================================
    
    public static Animation none() {
        return NONE;
    }
    
    /**
     * Creates a spin animation around Y axis.
     */
    public static Animation spinning(float speed) {
        return new Animation(speed, 0, 0, 0, 0, 0, Axis.Y);
    }
    
    /**
     * Creates a spin animation around specified axis.
     */
    public static Animation spinning(float speed, Axis axis) {
        return new Animation(speed, 0, 0, 0, 0, 0, axis);
    }
    
    /**
     * Creates a scale pulsing animation.
     */
    public static Animation pulsing(float speed, float amount) {
        return new Animation(0, speed, amount, 0, 0, 0, Axis.Y);
    }
    
    /**
     * Creates an alpha pulsing animation.
     */
    public static Animation alphaPulsing(float speed, float amount) {
        return new Animation(0, 0, 0, 0, speed, amount, Axis.Y);
    }
    
    /**
     * Creates combined spin and scale pulse.
     */
    public static Animation spinningAndPulsing(float spinSpeed, float pulseSpeed, float pulseAmount) {
        return new Animation(spinSpeed, pulseSpeed, pulseAmount, 0, 0, 0, Axis.Y);
    }
    
    /**
     * Creates a full animation with all parameters.
     */
    public static Animation full(float spin, Axis axis, float pulse, float pulseAmount, 
                                  float alphaPulse, float alphaPulseAmount, float phase) {
        return new Animation(spin, pulse, pulseAmount, phase, alphaPulse, alphaPulseAmount, axis);
    }
    
    // =========================================================================
    // Builder-style
    // =========================================================================
    
    public Animation withSpin(float newSpin) {
        return new Animation(newSpin, pulse, pulseAmount, phase, alphaPulse, alphaPulseAmount, spinAxis);
    }
    
    public Animation withSpinAxis(Axis axis) {
        return new Animation(spin, pulse, pulseAmount, phase, alphaPulse, alphaPulseAmount, axis);
    }
    
    public Animation withPulse(float newPulse, float newAmount) {
        return new Animation(spin, newPulse, newAmount, phase, alphaPulse, alphaPulseAmount, spinAxis);
    }
    
    public Animation withAlphaPulse(float newPulse, float newAmount) {
        return new Animation(spin, pulse, pulseAmount, phase, newPulse, newAmount, spinAxis);
    }
    
    public Animation withPhase(float newPhase) {
        return new Animation(spin, pulse, pulseAmount, newPhase, alphaPulse, alphaPulseAmount, spinAxis);
    }
    
    // =========================================================================
    // Computed Values
    // =========================================================================
    
    /**
     * Calculates current rotation at given time.
     * @param time elapsed time in ticks
     * @return rotation in radians
     */
    public float getRotation(float time) {
        return (spin * time + phase) % (float)(Math.PI * 2);
    }
    
    /**
     * Calculates current scale multiplier at given time.
     * @param time elapsed time in ticks
     * @return scale multiplier (1.0 = no change)
     */
    public float getScale(float time) {
        if (pulse == 0 || pulseAmount == 0) {
            return 1.0f;
        }
        return 1.0f + (float)Math.sin(pulse * time + phase) * pulseAmount;
    }
    
    /**
     * Calculates current alpha multiplier at given time.
     * @param time elapsed time in ticks
     * @return alpha multiplier (0.0 - 1.0 range adjustment)
     */
    public float getAlphaMultiplier(float time) {
        if (alphaPulse == 0 || alphaPulseAmount == 0) {
            return 1.0f;
        }
        // Sin gives -1 to 1, we want 0 to 1 for interpolation into AlphaRange
        float sinValue = (float)Math.sin(alphaPulse * time + phase);
        return 0.5f + sinValue * 0.5f * alphaPulseAmount;
    }
    
    /**
     * Checks if this animation has any effect.
     */
    public boolean isAnimated() {
        return spin != 0 || pulse != 0 || alphaPulse != 0;
    }
    
    /**
     * Checks if this has spin animation.
     */
    public boolean hasSpinAnimation() {
        return spin != 0;
    }
    
    /**
     * Checks if this has scale pulse.
     */
    public boolean hasScalePulse() {
        return pulse != 0 && pulseAmount != 0;
    }
    
    /**
     * Checks if this has alpha pulse.
     */
    public boolean hasAlphaPulse() {
        return alphaPulse != 0 && alphaPulseAmount != 0;
    }
    
    // =========================================================================
    // JSON Serialization
    // =========================================================================
    
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        
        // Only include non-default values
        if (spin != 0) {
            json.addProperty("spin", spin);
            if (spinAxis != Axis.Y) {
                json.addProperty("spinAxis", spinAxis.id());
            }
        }
        if (pulse != 0) json.addProperty("pulse", pulse);
        if (pulseAmount != 0) json.addProperty("pulseAmount", pulseAmount);
        if (phase != 0) json.addProperty("phase", phase);
        if (alphaPulse != 0) json.addProperty("alphaPulse", alphaPulse);
        if (alphaPulseAmount != 0) json.addProperty("alphaPulseAmount", alphaPulseAmount);
        
        return json;
    }
    
    public static Animation fromJson(JsonObject json) {
        if (json == null) {
            return NONE;
        }
        
        float spin = json.has("spin") ? json.get("spin").getAsFloat() : 0;
        float pulse = json.has("pulse") ? json.get("pulse").getAsFloat() : 0;
        float pulseAmount = json.has("pulseAmount") ? json.get("pulseAmount").getAsFloat() : 0;
        float phase = json.has("phase") ? json.get("phase").getAsFloat() : 0;
        float alphaPulse = json.has("alphaPulse") ? json.get("alphaPulse").getAsFloat() : 0;
        float alphaPulseAmount = json.has("alphaPulseAmount") ? json.get("alphaPulseAmount").getAsFloat() : 0;
        Axis spinAxis = json.has("spinAxis") ? Axis.fromId(json.get("spinAxis").getAsString()) : Axis.Y;
        
        Animation result = new Animation(spin, pulse, pulseAmount, phase, alphaPulse, alphaPulseAmount, spinAxis);
        
        if (result.isAnimated()) {
            Logging.RENDER.topic("animation").trace(
                "Parsed Animation: spin={:.3f} ({}), pulse={:.3f}, alphaPulse={:.3f}",
                spin, spinAxis.id(), pulse, alphaPulse);
        }
        
        return result;
    }
}
