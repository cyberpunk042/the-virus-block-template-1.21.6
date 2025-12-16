package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Enhanced cycling button that supports right-click to cycle backward.
 * Left-click cycles forward, right-click cycles backward.
 * 
 * <p>This is a wrapper around Minecraft's CyclingButtonWidget that adds 
 * mouse button detection for bidirectional cycling.</p>
 * 
 * @param <T> The type of value being cycled
 */
public class BidirectionalCyclingButton<T> extends CyclingButtonWidget<T> {
    
    private final List<T> values;
    private int currentIndex;
    private final Consumer<T> onChange;
    
    private BidirectionalCyclingButton(
            int x, int y, int width, int height,
            Text message, Text optionText,
            List<T> values, int initialIndex,
            Function<T, Text> valueToText,
            Consumer<T> onChange) {
        super(x, y, width, height, message, optionText, initialIndex, 
              valueToText, CyclingButtonWidget.Values.of(values), 
              (btn, val) -> {
                  // This is called by super on left-click
                  onChange.accept(val);
              }, 
              val -> Tooltip.of(Text.empty()), // Default no tooltip
              false); // omitKeyText
        
        this.values = values;
        this.currentIndex = initialIndex;
        this.onChange = onChange;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible && this.isMouseOver(mouseX, mouseY)) {
            if (button == 1) { // Right-click: cycle backward
                this.playDownSound(MinecraftClient.getInstance().getSoundManager());
                cycleBackward();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void cycleBackward() {
        if (values.isEmpty()) return;
        
        currentIndex = currentIndex <= 0 ? values.size() - 1 : currentIndex - 1;
        T newValue = values.get(currentIndex);
        setValue(newValue);
        
        Logging.GUI.topic("widget").trace("BidirectionalCyclingButton cycled backward to index {}: {}", 
            currentIndex, newValue);
        
        if (onChange != null) {
            onChange.accept(newValue);
        }
    }
    
    /**
     * Builder for creating bidirectional cycling buttons.
     */
    public static class Builder<T> {
        private int x, y, width, height;
        private String label = "";
        private List<T> values;
        private T initialValue;
        private Function<T, String> formatter = Object::toString;
        private Consumer<T> onChange = v -> {};
        private String tooltip = "";
        private boolean omitLabel = false;
        
        public Builder<T> position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }
        
        public Builder<T> size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }
        
        public Builder<T> label(String label) {
            this.label = label;
            return this;
        }
        
        public Builder<T> values(List<T> values) {
            this.values = values;
            return this;
        }
        
        @SafeVarargs
        public final Builder<T> values(T... values) {
            this.values = List.of(values);
            return this;
        }
        
        public Builder<T> initial(T value) {
            this.initialValue = value;
            return this;
        }
        
        public Builder<T> formatter(Function<T, String> formatter) {
            this.formatter = formatter;
            return this;
        }
        
        public Builder<T> onChange(Consumer<T> onChange) {
            this.onChange = onChange;
            return this;
        }
        
        public Builder<T> tooltip(String tooltip) {
            this.tooltip = tooltip;
            return this;
        }
        
        public Builder<T> omitLabel() {
            this.omitLabel = true;
            return this;
        }
        
        public BidirectionalCyclingButton<T> build() {
            if (values == null || values.isEmpty()) {
                throw new IllegalStateException("Values must be provided");
            }
            
            int initialIndex = initialValue != null ? values.indexOf(initialValue) : 0;
            if (initialIndex < 0) initialIndex = 0;
            
            Function<T, Text> valueToText = omitLabel
                ? val -> Text.literal(formatter.apply(val))
                : val -> Text.literal(label + ": " + formatter.apply(val));
            
            Text optionText = omitLabel ? Text.literal("") : Text.literal(label);
            Text initialMessage = valueToText.apply(values.get(initialIndex));
            
            return new BidirectionalCyclingButton<>(
                x, y, width, height,
                initialMessage, optionText,
                values, initialIndex,
                valueToText, onChange
            );
        }
    }
    
    /**
     * Creates a new builder for a bidirectional cycling button.
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
    
    /**
     * Creates a bidirectional enum cycling button.
     */
    public static <E extends Enum<E>> BidirectionalCyclingButton<E> forEnum(
            int x, int y, int width, int height,
            String label, Class<E> enumClass, E initial,
            Consumer<E> onChange) {
        
        return BidirectionalCyclingButton.<E>builder()
            .position(x, y)
            .size(width, height)
            .label(label)
            .values(List.of(enumClass.getEnumConstants()))
            .initial(initial)
            .formatter(e -> e.name().toLowerCase().replace("_", " "))
            .onChange(onChange)
            .build();
    }
    
    /**
     * Creates a bidirectional boolean cycling button (ON/OFF).
     */
    public static BidirectionalCyclingButton<Boolean> forBoolean(
            int x, int y, int width, int height,
            String label, boolean initial,
            Consumer<Boolean> onChange) {
        
        return BidirectionalCyclingButton.<Boolean>builder()
            .position(x, y)
            .size(width, height)
            .label(label)
            .values(Boolean.TRUE, Boolean.FALSE)
            .initial(initial)
            .formatter(val -> val ? "ON" : "OFF")
            .onChange(onChange)
            .build();
    }
}
