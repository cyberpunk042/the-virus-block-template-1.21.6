package net.cyberpunk042.client.gui.util;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Factory for creating GUI widgets with consistent styling.
 * 
 * <h2>Widget Categories</h2>
 * <ul>
 *   <li><b>Buttons:</b> button(), toggle(), checkbox()</li>
 *   <li><b>Input:</b> slider(), sliderInt(), editBox()</li>
 *   <li><b>Dropdowns:</b> enumDropdown()</li>
 *   <li><b>Layout:</b> grid(), horizontalLayout(), verticalLayout()</li>
 *   <li><b>Display:</b> icon(), label()</li>
 * </ul>
 * 
 * All widgets use {@link GuiConstants} for consistent theming.
 */
public final class GuiWidgets {
    
    private GuiWidgets() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUTTONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a styled button.
     */
    public static ButtonWidget button(int x, int y, int width, String label, String tooltip, Runnable onClick) {
        Logging.GUI.topic("widget").trace("Creating button: {}", label);
        
        return ButtonWidget.builder(Text.literal(label), btn -> {
            Logging.GUI.topic("button").trace("Clicked: {}", label);
            onClick.run();
        })
        .dimensions(x, y, width, GuiConstants.WIDGET_HEIGHT)
        .tooltip(Tooltip.of(Text.literal(tooltip)))
        .build();
    }
    
    /**
     * Create a standard width button.
     */
    public static ButtonWidget button(int x, int y, String label, String tooltip, Runnable onClick) {
        return button(x, y, GuiConstants.BUTTON_WIDTH, label, tooltip, onClick);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TOGGLE (Boolean)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a toggle button (ON/OFF).
     */
    public static CyclingButtonWidget<Boolean> toggle(
            int x, int y, int width,
            String label, boolean initial, String tooltip,
            Consumer<Boolean> onChange) {
        
        Logging.GUI.topic("widget").trace("Creating toggle: {} = {}", label, initial);
        
        return CyclingButtonWidget.<Boolean>builder(val -> 
                Text.literal(label + ": " + (val ? "ON" : "OFF")))
            .values(Boolean.TRUE, Boolean.FALSE)
            .initially(initial)
            .tooltip(val -> Tooltip.of(Text.literal(tooltip)))
            .build(x, y, width, GuiConstants.WIDGET_HEIGHT, Text.literal(label), (btn, val) -> {
                Logging.GUI.topic("toggle").trace("{} → {}", label, val);
                onChange.accept(val);
            });
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CHECKBOX (NEW!)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a checkbox with callback.
     */
    public static CheckboxWidget checkbox(
            int x, int y, 
            String label, boolean initial, String tooltip,
            TextRenderer textRenderer,
            Consumer<Boolean> onChange) {
        
        Logging.GUI.topic("widget").trace("Creating checkbox: {} = {}", label, initial);
        
        return CheckboxWidget.builder(Text.literal(label), textRenderer)
            .pos(x, y)
            .checked(initial)
            .callback((cb, isChecked) -> {
                Logging.GUI.topic("checkbox").trace("{} → {}", label, isChecked);
                onChange.accept(isChecked);
            })
            .tooltip(Tooltip.of(Text.literal(tooltip)))
            .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ENUM DROPDOWN
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create an enum dropdown selector.
     */
    public static <E extends Enum<E>> CyclingButtonWidget<E> enumDropdown(
            int x, int y, int width,
            String label, Class<E> enumClass, E initial, String tooltip,
            Consumer<E> onChange) {
        
        Logging.GUI.topic("widget").trace("Creating enum dropdown: {} = {}", label, initial);
        
        return CyclingButtonWidget.<E>builder(val -> 
                Text.literal(label + ": " + formatEnumName(val)))
            .values(enumClass.getEnumConstants())
            .initially(initial)
            .tooltip(val -> Tooltip.of(Text.literal(tooltip)))
            .build(x, y, width, GuiConstants.WIDGET_HEIGHT, Text.literal(label), (btn, val) -> {
                Logging.GUI.topic("enum").trace("{} → {}", label, val);
                onChange.accept(val);
            });
    }
    
    private static <E extends Enum<E>> String formatEnumName(E val) {
        return val.name().toLowerCase().replace("_", " ");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SLIDERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a float slider with label.
     */
    public static SliderWidget slider(
            int x, int y, int width,
            String label, float min, float max, float initial, String format, String tooltip,
            Consumer<Float> onChange) {
        
        Logging.GUI.topic("widget").trace("Creating slider: {} = {} [{}, {}]", label, initial, min, max);
        
        double normalized = (initial - min) / (max - min);
        
        return new SliderWidget(x, y, width, GuiConstants.WIDGET_HEIGHT, 
                Text.literal(label + ": " + String.format(format, initial)), normalized) {
            
            private final float minVal = min;
            private final float maxVal = max;
            private final String fmt = format;
            private final String lbl = label;
            
            @Override
            protected void updateMessage() {
                float val = (float) (minVal + value * (maxVal - minVal));
                setMessage(Text.literal(lbl + ": " + String.format(fmt, val)));
            }
            
            @Override
            protected void applyValue() {
                float val = (float) (minVal + value * (maxVal - minVal));
                Logging.GUI.topic("slider").trace("{} → {}", lbl, val);
                onChange.accept(val);
            }
        };
    }
    
    /**
     * Create an integer slider.
     */
    public static SliderWidget sliderInt(
            int x, int y, int width,
            String label, int min, int max, int initial, String tooltip,
            Consumer<Integer> onChange) {
        
        Logging.GUI.topic("widget").trace("Creating int slider: {} = {} [{}, {}]", label, initial, min, max);
        
        double normalized = (double) (initial - min) / (max - min);
        
        return new SliderWidget(x, y, width, GuiConstants.WIDGET_HEIGHT,
                Text.literal(label + ": " + initial), normalized) {
            
            private final int minVal = min;
            private final int maxVal = max;
            private final String lbl = label;
            
            @Override
            protected void updateMessage() {
                int val = minVal + (int) Math.round(value * (maxVal - minVal));
                setMessage(Text.literal(lbl + ": " + val));
            }
            
            @Override
            protected void applyValue() {
                int val = minVal + (int) Math.round(value * (maxVal - minVal));
                Logging.GUI.topic("slider").trace("{} → {}", lbl, val);
                onChange.accept(val);
            }
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EDIT BOX (Multiline Text - NEW!)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a multiline text edit box.
     */
    public static EditBoxWidget editBox(
            int x, int y, int width, int height,
            String placeholder, TextRenderer textRenderer,
            Consumer<String> onChange) {
        
        Logging.GUI.topic("widget").trace("Creating edit box");
        
        EditBoxWidget box = EditBoxWidget.builder()
            .placeholder(Text.literal(placeholder))
            .textColor(GuiConstants.TEXT_PRIMARY)
            .cursorColor(GuiConstants.ACCENT)
            .textShadow(false)
            .hasBackground(true)
            .build(textRenderer, width, height, Text.literal(""));
        
        box.setChangeListener(text -> {
            Logging.GUI.topic("editbox").trace("Text changed: {} chars", text.length());
            onChange.accept(text);
        });
        
        return box;
    }
    
    /**
     * Create a single-line text field.
     */
    public static TextFieldWidget textField(
            int x, int y, int width,
            String placeholder, TextRenderer textRenderer,
            Consumer<String> onChange) {
        
        Logging.GUI.topic("widget").trace("Creating text field");
        
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, width, GuiConstants.WIDGET_HEIGHT, Text.literal(""));
        field.setPlaceholder(Text.literal(placeholder));
        field.setChangedListener(text -> {
            Logging.GUI.topic("textfield").trace("Text: {}", text);
            onChange.accept(text);
        });
        
        return field;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYOUT - GRID (NEW!)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a grid layout widget.
     * 
     * @param x X position
     * @param y Y position
     * @param columns Number of columns
     * @param spacing Spacing between cells
     * @return GridWidget.Adder for fluent widget adding
     */
    public static GridWidget.Adder grid(int x, int y, int columns, int spacing) {
        Logging.GUI.topic("layout").trace("Creating grid: {}x cols, spacing {}", columns, spacing);
        
        GridWidget grid = new GridWidget(x, y);
        grid.setSpacing(spacing);
        return grid.createAdder(columns);
    }
    
    /**
     * Create a grid with default spacing.
     */
    public static GridWidget.Adder grid(int x, int y, int columns) {
        return grid(x, y, columns, GuiConstants.PADDING);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYOUT - DIRECTIONAL (NEW!)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a horizontal layout.
     */
    public static DirectionalLayoutWidget horizontalLayout(int spacing) {
        Logging.GUI.topic("layout").trace("Creating horizontal layout, spacing {}", spacing);
        return DirectionalLayoutWidget.horizontal().spacing(spacing);
    }
    
    /**
     * Create a vertical layout.
     */
    public static DirectionalLayoutWidget verticalLayout(int spacing) {
        Logging.GUI.topic("layout").trace("Creating vertical layout, spacing {}", spacing);
        return DirectionalLayoutWidget.vertical().spacing(spacing);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DISPLAY WIDGETS (NEW!)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a text label.
     */
    public static TextWidget label(int x, int y, int width, String text, TextRenderer textRenderer) {
        Logging.GUI.topic("widget").trace("Creating label: {}", text);
        return new TextWidget(x, y, width, GuiConstants.WIDGET_HEIGHT, Text.literal(text), textRenderer);
    }
    
    /**
     * Create a text label with color.
     */
    public static TextWidget label(int x, int y, int width, String text, int color, TextRenderer textRenderer) {
        TextWidget widget = label(x, y, width, text, textRenderer);
        widget.setTextColor(color);
        return widget;
    }
    
    /**
     * Create an icon widget from sprite.
     */
    public static IconWidget icon(int x, int y, int size, net.minecraft.util.Identifier sprite) {
        Logging.GUI.topic("widget").trace("Creating icon: {}", sprite);
        return IconWidget.create(size, size, sprite, size, size);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LOADING INDICATOR (NEW!)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a loading widget.
     */
    public static LoadingWidget loading(String label, TextRenderer textRenderer) {
        Logging.GUI.topic("widget").trace("Creating loading: {}", label);
        return new LoadingWidget(textRenderer, Text.literal(label));
    }
}
