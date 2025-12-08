package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * G21-G24: Slider with label, min/max range mapping, format string, and optional snap.
 */
public class LabeledSlider extends SliderWidget {
    
    private final String label;
    private final float minValue;
    private final float maxValue;
    private final String format;
    private final Float step;
    private final Consumer<Float> onChange;
    
    /**
     * Creates a labeled slider.
     * @param x X position
     * @param y Y position
     * @param width Widget width
     * @param label Display label
     * @param minValue Minimum value
     * @param maxValue Maximum value
     * @param initialValue Starting value
     * @param format Format string (e.g., "%.2f" or "%d")
     * @param step Optional snap step, null for continuous
     * @param onChange Callback when value changes
     */
    public LabeledSlider(int x, int y, int width, String label, 
                         float minValue, float maxValue, float initialValue,
                         String format, Float step, Consumer<Float> onChange) {
        super(x, y, width, GuiConstants.WIDGET_HEIGHT, 
              Text.literal(label + ": " + String.format(format, initialValue)),
              normalizeValue(initialValue, minValue, maxValue));
        this.label = label;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.format = format;
        this.step = step;
        this.onChange = onChange;
        
        Logging.GUI.topic("widget").trace(
            "LabeledSlider created: {} [{}-{}]", label, minValue, maxValue);
    }
    
    private static double normalizeValue(float value, float min, float max) {
        return (value - min) / (max - min);
    }
    
    /**
     * Gets the current value in the min-max range.
     */
    public float getValue() {
        float raw = (float) (minValue + value * (maxValue - minValue));
        if (step != null && step > 0) {
            raw = Math.round(raw / step) * step;
        }
        return raw;
    }
    
    /**
     * Sets the value programmatically.
     */
    public void setValue(float newValue) {
        this.value = normalizeValue(newValue, minValue, maxValue);
        updateMessage();
    }
    
    @Override
    protected void updateMessage() {
        float val = getValue();
        setMessage(Text.literal(label + ": " + String.format(format, val)));
    }
    
    @Override
    protected void applyValue() {
        float val = getValue();
        Logging.GUI.topic("widget").trace("LabeledSlider {} -> {}", label, val);
        if (onChange != null) {
            onChange.accept(val);
        }
    }
    
    // Builder pattern for convenience
    public static Builder builder(String label) {
        return new Builder(label);
    }
    
    public static class Builder {
        private final String label;
        private int x, y, width = GuiConstants.SLIDER_WIDTH;
        private float min = 0f, max = 1f, initial = 0.5f;
        private String format = "%.2f";
        private Float step = null;
        private Consumer<Float> onChange = v -> {};
        
        public Builder(String label) { this.label = label; }
        public Builder position(int x, int y) { this.x = x; this.y = y; return this; }
        public Builder width(int w) { this.width = w; return this; }
        public Builder range(float min, float max) { this.min = min; this.max = max; return this; }
        public Builder initial(float v) { this.initial = v; return this; }
        public Builder format(String f) { this.format = f; return this; }
        public Builder step(float s) { this.step = s; return this; }
        public Builder onChange(Consumer<Float> c) { this.onChange = c; return this; }
        
        public LabeledSlider build() {
            return new LabeledSlider(x, y, width, label, min, max, initial, format, step, onChange);
        }
    }
}
