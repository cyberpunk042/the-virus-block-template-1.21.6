package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.gui.state.StateField;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.*;
import org.joml.Vector3f;

import java.util.Set;

/**
 * Adapter for animation-related state (spin, pulse, wave, wobble, colorCycle).
 * 
 * <p>Handles paths like "spin.speed", "pulse.scale", "alphaPulse.min".</p>
 */
@StateCategory("animation")
public class AnimationAdapter extends AbstractAdapter implements PrimitiveAdapter {
    
    @StateField private SpinConfig spin = SpinConfig.NONE;
    @StateField private PulseConfig pulse = PulseConfig.NONE;
    @StateField private AlphaPulseConfig alphaPulse = AlphaPulseConfig.NONE;
    @StateField private WobbleConfig wobble = WobbleConfig.NONE;
    @StateField private WaveConfig wave = WaveConfig.NONE;
    @StateField private ColorCycleConfig colorCycle = ColorCycleConfig.NONE;
    
    @Override
    public String category() { return "animation"; }
    
    @Override
    public void loadFrom(Primitive source) {
        Animation anim = source.animation();
        if (anim != null) {
            this.spin = orDefault(anim.spin(), SpinConfig.NONE);
            this.pulse = orDefault(anim.pulse(), PulseConfig.NONE);
            this.alphaPulse = orDefault(anim.alphaPulse(), AlphaPulseConfig.NONE);
            this.wobble = orDefault(anim.wobble(), WobbleConfig.NONE);
            this.wave = orDefault(anim.wave(), WaveConfig.NONE);
            this.colorCycle = orDefault(anim.colorCycle(), ColorCycleConfig.NONE);
        } else {
            reset();
        }
        Logging.GUI.topic("adapter").trace("AnimationAdapter loaded");
    }
    
    @Override
    public void saveTo(PrimitiveBuilder builder) {
        builder.animation(Animation.builder()
            .spin(spin)
            .pulse(pulse)
            .alphaPulse(alphaPulse)
            .wobble(wobble)
            .wave(wave)
            .colorCycle(colorCycle)
            .build());
    }
    
    // Typed accessors
    public SpinConfig spin() { return spin; }
    public void setSpin(SpinConfig spin) { this.spin = spin; }
    
    public PulseConfig pulse() { return pulse; }
    public void setPulse(PulseConfig pulse) { this.pulse = pulse; }
    
    public AlphaPulseConfig alphaPulse() { return alphaPulse; }
    public void setAlphaPulse(AlphaPulseConfig alphaPulse) { this.alphaPulse = alphaPulse; }
    
    public WobbleConfig wobble() { return wobble; }
    public void setWobble(WobbleConfig wobble) { this.wobble = wobble; }
    
    public WaveConfig wave() { return wave; }
    public void setWave(WaveConfig wave) { this.wave = wave; }
    
    public ColorCycleConfig colorCycle() { return colorCycle; }
    public void setColorCycle(ColorCycleConfig colorCycle) { this.colorCycle = colorCycle; }
    
    /**
     * Override get to handle paths like "spin.speed", "pulse.scale", etc.
     */
    @Override
    public Object get(String path) {
        String[] parts = path.split("\\.", 2);
        String field = parts[0];
        String prop = parts.length > 1 ? parts[1] : null;
        
        return switch (field) {
            case "spin" -> prop != null ? getSpinProperty(prop) : spin;
            case "pulse" -> prop != null ? getPulseProperty(prop) : pulse;
            case "alphaPulse" -> prop != null ? getAlphaPulseProperty(prop) : alphaPulse;
            case "wobble" -> prop != null ? getWobbleProperty(prop) : wobble;
            case "wave" -> prop != null ? getWaveProperty(prop) : wave;
            case "colorCycle" -> prop != null ? getColorCycleProperty(prop) : colorCycle;
            default -> super.get(path);
        };
    }
    
    private Object getSpinProperty(String prop) {
        return switch (prop) {
            case "speed" -> spin.speed();
            case "axis" -> spin.axis().name();
            case "oscillate" -> spin.oscillate();
            case "range" -> spin.range();
            default -> null;
        };
    }
    
    private Object getPulseProperty(String prop) {
        return switch (prop) {
            case "speed" -> pulse.speed();
            case "scale" -> pulse.scale();
            default -> null;
        };
    }
    
    private Object getAlphaPulseProperty(String prop) {
        return switch (prop) {
            case "speed" -> alphaPulse.speed();
            case "min" -> alphaPulse.min();
            case "max" -> alphaPulse.max();
            default -> null;
        };
    }
    
    private Object getWobbleProperty(String prop) {
        return switch (prop) {
            case "speed" -> wobble.speed();
            case "randomize" -> wobble.randomize();
            // amplitude is a Vector3f - return x for scalar access
            case "amplitude" -> wobble.amplitude() != null ? wobble.amplitude().x : 0f;
            default -> null;
        };
    }
    
    private Object getWaveProperty(String prop) {
        return switch (prop) {
            case "speed" -> wave.speed();
            case "amplitude" -> wave.amplitude();
            case "frequency" -> wave.frequency();
            case "direction" -> wave.direction() != null ? wave.direction().name() : "OUTWARD";
            default -> null;
        };
    }
    
    private Object getColorCycleProperty(String prop) {
        return switch (prop) {
            case "speed" -> colorCycle.speed();
            case "blend" -> colorCycle.blend();
            case "colors" -> colorCycle.colors();
            default -> null;
        };
    }
    
    /**
     * Override set to handle paths like "spin.speed", "pulse.scale", etc.
     */
    @Override
    public void set(String path, Object value) {
        String[] parts = path.split("\\.", 2);
        String field = parts[0];
        String prop = parts.length > 1 ? parts[1] : null;
        
        if (prop == null) {
            super.set(path, value);
            return;
        }
        
        switch (field) {
            case "spin" -> setSpinProperty(prop, value);
            case "pulse" -> setPulseProperty(prop, value);
            case "alphaPulse" -> setAlphaPulseProperty(prop, value);
            case "wobble" -> setWobbleProperty(prop, value);
            case "wave" -> setWaveProperty(prop, value);
            case "colorCycle" -> setColorCycleProperty(prop, value);
            default -> super.set(path, value);
        }
    }
    
    private void setSpinProperty(String prop, Object value) {
        SpinConfig.Builder b = spin.toBuilder();
        switch (prop) {
            case "speed" -> b.speed(toFloat(value));
            case "axis" -> b.axis(Axis.fromId(value.toString()));
            case "oscillate" -> b.oscillate(toBool(value));
            case "range" -> b.range(toFloat(value));
        }
        this.spin = b.build();
    }
    
    private void setPulseProperty(String prop, Object value) {
        PulseConfig.Builder b = pulse.toBuilder();
        switch (prop) {
            case "speed" -> b.speed(toFloat(value));
            case "scale" -> b.scale(toFloat(value));
        }
        this.pulse = b.build();
    }
    
    private void setAlphaPulseProperty(String prop, Object value) {
        AlphaPulseConfig.Builder b = alphaPulse.toBuilder();
        switch (prop) {
            case "speed" -> b.speed(toFloat(value));
            case "min" -> b.min(toFloat(value));
            case "max" -> b.max(toFloat(value));
        }
        this.alphaPulse = b.build();
    }
    
    private void setWobbleProperty(String prop, Object value) {
        WobbleConfig.Builder b = wobble.toBuilder();
        switch (prop) {
            case "speed" -> b.speed(toFloat(value));
            case "randomize" -> b.randomize(toBool(value));
            // For scalar amplitude, set uniform across all axes
            case "amplitude" -> {
                float v = toFloat(value);
                b.amplitude(new Vector3f(v, v * 0.5f, v));
            }
        }
        this.wobble = b.build();
    }
    
    private void setWaveProperty(String prop, Object value) {
        WaveConfig.Builder b = wave.toBuilder();
        switch (prop) {
            case "speed" -> b.speed(toFloat(value));
            case "amplitude" -> b.amplitude(toFloat(value));
            case "frequency" -> b.frequency(toFloat(value));
            case "direction" -> b.direction(Axis.fromId(value.toString()));
        }
        this.wave = b.build();
    }
    
    private void setColorCycleProperty(String prop, Object value) {
        ColorCycleConfig.Builder b = colorCycle.toBuilder();
        switch (prop) {
            case "speed" -> b.speed(toFloat(value));
            case "blend" -> b.blend(toBool(value));
            // colors would need special handling for list types
        }
        this.colorCycle = b.build();
    }
    
    private float toFloat(Object v) { return v instanceof Number n ? n.floatValue() : 0f; }
    private boolean toBool(Object v) { return v instanceof Boolean b ? b : Boolean.parseBoolean(v.toString()); }
    
    @Override
    public void reset() {
        this.spin = SpinConfig.NONE;
        this.pulse = PulseConfig.NONE;
        this.alphaPulse = AlphaPulseConfig.NONE;
        this.wobble = WobbleConfig.NONE;
        this.wave = WaveConfig.NONE;
        this.colorCycle = ColorCycleConfig.NONE;
    }
}
