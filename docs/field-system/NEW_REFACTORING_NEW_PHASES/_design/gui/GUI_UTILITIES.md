# GUI Utilities Reference

> **Purpose:** Define utility classes for GUI development  
> **Status:** Approved  
> **Created:** December 8, 2024  
> **Decisions:** Factory pattern, TRACE logging, alwaysChat errors, global theming

---

## 1. Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Widget creation | Factory methods | Pure functions, easy testing, global theming |
| State management | Screen owns state via callbacks | Single source of truth |
| Logging | TRACE for changes, ERROR with alwaysChat | Debug without spam, errors visible |
| Theming | `GuiConstants` class | Change once, apply everywhere |

---

## 2. Package Structure

```
net.cyberpunk042.client.gui/
├── util/
│   ├── GuiWidgets.java       # Factory for creating widgets
│   ├── GuiConstants.java     # Colors, dimensions, theming
│   └── GuiLayout.java        # Positioning helpers
│
├── widget/                    # Custom widgets only
│   ├── LabeledSlider.java    # Slider with label + value
│   ├── Vec3Editor.java       # X/Y/Z inputs
│   ├── ColorButton.java      # Color picker button
│   └── ExpandableSection.java # Collapsible panel
│
├── screen/
│   └── FieldCustomizerScreen.java
│
├── panel/
│   ├── QuickPanel.java
│   ├── AdvancedPanel.java
│   └── ...
│
└── state/
    ├── GuiState.java
    └── EditorState.java
```

---

## 3. GuiConstants.java

```java
package net.cyberpunk042.client.gui.util;

/**
 * Global GUI theming constants.
 * Change here → updates everywhere.
 */
public final class GuiConstants {
    
    private GuiConstants() {} // No instantiation
    
    // ═══════════════════════════════════════════════════════════════════════
    // DIMENSIONS
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Standard widget height */
    public static final int WIDGET_HEIGHT = 20;
    
    /** Standard button width */
    public static final int BUTTON_WIDTH = 120;
    
    /** Wide button width */
    public static final int BUTTON_WIDTH_WIDE = 200;
    
    /** Narrow button width */
    public static final int BUTTON_WIDTH_NARROW = 80;
    
    /** Slider default width */
    public static final int SLIDER_WIDTH = 150;
    
    /** Padding between widgets */
    public static final int PADDING = 4;
    
    /** Gap between sections */
    public static final int SECTION_GAP = 12;
    
    /** Panel margin from screen edge */
    public static final int MARGIN = 10;
    
    /** Tab bar height */
    public static final int TAB_HEIGHT = 24;
    
    /** Section header height */
    public static final int HEADER_HEIGHT = 16;
    
    // ═══════════════════════════════════════════════════════════════════════
    // COLORS (ARGB format: 0xAARRGGBB)
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Screen background - darkest */
    public static final int BG_SCREEN = 0xE0101010;
    
    /** Panel background */
    public static final int BG_PANEL = 0xFF1E1E1E;
    
    /** Widget background */
    public static final int BG_WIDGET = 0xFF2D2D2D;
    
    /** Widget background on hover */
    public static final int BG_WIDGET_HOVER = 0xFF3D3D3D;
    
    /** Widget background when focused */
    public static final int BG_WIDGET_FOCUS = 0xFF4D4D4D;
    
    /** Section header background */
    public static final int BG_HEADER = 0xFF252525;
    
    /** Primary text - white */
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    
    /** Secondary text - gray */
    public static final int TEXT_SECONDARY = 0xFFAAAAAA;
    
    /** Disabled text */
    public static final int TEXT_DISABLED = 0xFF666666;
    
    /** Label text */
    public static final int TEXT_LABEL = 0xFFCCCCCC;
    
    /** Accent color - mod theme blue */
    public static final int ACCENT = 0xFF4488FF;
    
    /** Accent hover */
    public static final int ACCENT_HOVER = 0xFF66AAFF;
    
    /** Success green */
    public static final int SUCCESS = 0xFF44DD44;
    
    /** Warning yellow */
    public static final int WARNING = 0xFFDDAA00;
    
    /** Error red */
    public static final int ERROR = 0xFFFF4444;
    
    /** Border color */
    public static final int BORDER = 0xFF404040;
    
    /** Border color when focused */
    public static final int BORDER_FOCUS = 0xFF4488FF;
    
    // ═══════════════════════════════════════════════════════════════════════
    // THEME REFERENCES (for color picker)
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Theme color references available in color picker */
    public static final String[] THEME_REFS = {
        "@primary",
        "@secondary", 
        "@accent",
        "@beam",
        "@warning",
        "@error"
    };
}
```

---

## 4. GuiWidgets.java

```java
package net.cyberpunk042.client.gui.util;

import net.cyberpunk042.util.Logging;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Factory class for creating GUI widgets with consistent styling.
 * 
 * <p>All factories:
 * <ul>
 *   <li>Return native Minecraft widgets (no wrapper classes)</li>
 *   <li>Apply consistent dimensions from {@link GuiConstants}</li>
 *   <li>Add TRACE logging for value changes</li>
 *   <li>Accept callbacks for state management</li>
 * </ul>
 * 
 * <p>Usage pattern:
 * <pre>{@code
 * addDrawableChild(GuiWidgets.toggle(x, y, 120,
 *     "Spin Enabled", state.isSpinEnabled(),
 *     "Enable rotation animation",
 *     val -> {
 *         state.setSpinEnabled(val);
 *         state.markDirty();
 *     }
 * ));
 * }</pre>
 */
public final class GuiWidgets {
    
    private GuiWidgets() {} // No instantiation
    
    // ═══════════════════════════════════════════════════════════════════════
    // ENUM DROPDOWN
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Create an enum dropdown (cycling button).
     *
     * @param x        X position
     * @param y        Y position
     * @param width    Widget width
     * @param label    Display label
     * @param enumClass Enum class
     * @param initial  Initial value
     * @param tooltip  Tooltip text
     * @param onChange Callback when value changes
     * @return CyclingButtonWidget configured for the enum
     */
    public static <E extends Enum<E>> CyclingButtonWidget<E> enumDropdown(
            int x, int y, int width,
            String label, Class<E> enumClass, E initial,
            String tooltip, Consumer<E> onChange
    ) {
        return CyclingButtonWidget.<E>builder(e -> Text.literal(formatEnumName(e)))
            .values(enumClass.getEnumConstants())
            .initially(initial)
            .tooltip(e -> Tooltip.of(Text.literal(tooltip)))
            .build(x, y, width, GuiConstants.WIDGET_HEIGHT, Text.literal(label),
                (btn, val) -> {
                    Logging.GUI.trace("{} → {}", label, val);
                    onChange.accept(val);
                });
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // TOGGLE (Boolean)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Create a boolean toggle button.
     *
     * @param x        X position
     * @param y        Y position
     * @param width    Widget width
     * @param label    Display label
     * @param initial  Initial value
     * @param tooltip  Tooltip text
     * @param onChange Callback when value changes
     * @return CyclingButtonWidget for boolean toggle
     */
    public static CyclingButtonWidget<Boolean> toggle(
            int x, int y, int width,
            String label, boolean initial,
            String tooltip, Consumer<Boolean> onChange
    ) {
        return CyclingButtonWidget.onOffBuilder()
            .initially(initial)
            .tooltip(v -> Tooltip.of(Text.literal(tooltip)))
            .build(x, y, width, GuiConstants.WIDGET_HEIGHT, Text.literal(label),
                (btn, val) -> {
                    Logging.GUI.trace("{} → {}", label, val ? "ON" : "OFF");
                    onChange.accept(val);
                });
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // BUTTON
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Create an action button.
     *
     * @param x       X position
     * @param y       Y position
     * @param width   Widget width
     * @param label   Button text
     * @param tooltip Tooltip text
     * @param onClick Action to perform
     * @return ButtonWidget
     */
    public static ButtonWidget button(
            int x, int y, int width,
            String label, String tooltip,
            Runnable onClick
    ) {
        return ButtonWidget.builder(Text.literal(label), btn -> {
                Logging.GUI.trace("Button pressed: {}", label);
                onClick.run();
            })
            .position(x, y)
            .size(width, GuiConstants.WIDGET_HEIGHT)
            .tooltip(Tooltip.of(Text.literal(tooltip)))
            .build();
    }
    
    /**
     * Create an action button without tooltip.
     */
    public static ButtonWidget button(
            int x, int y, int width,
            String label, Runnable onClick
    ) {
        return ButtonWidget.builder(Text.literal(label), btn -> {
                Logging.GUI.trace("Button pressed: {}", label);
                onClick.run();
            })
            .position(x, y)
            .size(width, GuiConstants.WIDGET_HEIGHT)
            .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // LABELED SLIDER
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Create a slider with label and value display.
     *
     * @param x        X position
     * @param y        Y position
     * @param width    Widget width
     * @param label    Display label
     * @param min      Minimum value
     * @param max      Maximum value
     * @param initial  Initial value
     * @param tooltip  Tooltip text
     * @param onChange Callback when value changes
     * @return LabeledSlider widget
     */
    public static LabeledSlider slider(
            int x, int y, int width,
            String label, float min, float max, float initial,
            String tooltip, Consumer<Float> onChange
    ) {
        return new LabeledSlider(x, y, width, GuiConstants.WIDGET_HEIGHT,
            label, min, max, initial, tooltip,
            val -> {
                Logging.GUI.trace("{} → {}", label, String.format("%.3f", val));
                onChange.accept(val);
            });
    }
    
    /**
     * Create an integer slider.
     */
    public static LabeledSlider sliderInt(
            int x, int y, int width,
            String label, int min, int max, int initial,
            String tooltip, Consumer<Integer> onChange
    ) {
        return new LabeledSlider(x, y, width, GuiConstants.WIDGET_HEIGHT,
            label, min, max, initial, tooltip,
            val -> {
                int intVal = Math.round(val);
                Logging.GUI.trace("{} → {}", label, intVal);
                onChange.accept(intVal);
            });
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Format enum name for display.
     * SNAKE_CASE → Title Case
     */
    private static String formatEnumName(Enum<?> e) {
        String name = e.name().toLowerCase().replace('_', ' ');
        // Capitalize first letter of each word
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                result.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
```

---

## 5. GuiLayout.java

```java
package net.cyberpunk042.client.gui.util;

/**
 * Layout helpers for consistent widget positioning.
 */
public final class GuiLayout {
    
    private GuiLayout() {}
    
    private int x;
    private int y;
    private final int startX;
    private final int columnWidth;
    
    /**
     * Create a layout helper.
     *
     * @param startX Starting X position
     * @param startY Starting Y position
     * @param columnWidth Width for widgets
     */
    public static GuiLayout create(int startX, int startY, int columnWidth) {
        GuiLayout layout = new GuiLayout();
        layout.x = startX;
        layout.y = startY;
        layout.startX = startX;
        layout.columnWidth = columnWidth;
        return layout;
    }
    
    /** Get current X */
    public int x() { return x; }
    
    /** Get current Y */
    public int y() { return y; }
    
    /** Get column width */
    public int width() { return columnWidth; }
    
    /** Move to next row */
    public GuiLayout nextRow() {
        x = startX;
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        return this;
    }
    
    /** Move to next row with extra gap */
    public GuiLayout nextSection() {
        x = startX;
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.SECTION_GAP;
        return this;
    }
    
    /** Skip rows */
    public GuiLayout skip(int rows) {
        y += rows * (GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING);
        return this;
    }
    
    /** Add gap */
    public GuiLayout gap(int pixels) {
        y += pixels;
        return this;
    }
    
    /** Move to next column (for side-by-side widgets) */
    public GuiLayout nextColumn(int width) {
        x += width + GuiConstants.PADDING;
        return this;
    }
    
    /** Reset to start of row */
    public GuiLayout resetColumn() {
        x = startX;
        return this;
    }
}
```

---

## 6. LabeledSlider.java (Custom Widget)

```java
package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.util.GuiConstants;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Slider with label and formatted value display.
 */
public class LabeledSlider extends SliderWidget {
    
    private final String label;
    private final float min;
    private final float max;
    private final String tooltip;
    private final Consumer<Float> onChange;
    private final boolean integerMode;
    
    public LabeledSlider(int x, int y, int width, int height,
                        String label, float min, float max, float initial,
                        String tooltip, Consumer<Float> onChange) {
        super(x, y, width, height, Text.empty(), normalize(initial, min, max));
        this.label = label;
        this.min = min;
        this.max = max;
        this.tooltip = tooltip;
        this.onChange = onChange;
        this.integerMode = (min == (int) min && max == (int) max);
        
        if (tooltip != null && !tooltip.isEmpty()) {
            setTooltip(Tooltip.of(Text.literal(tooltip)));
        }
        
        updateMessage();
    }
    
    private static double normalize(float value, float min, float max) {
        return (value - min) / (max - min);
    }
    
    @Override
    protected void updateMessage() {
        float actual = getActualValue();
        String valueStr = integerMode 
            ? String.valueOf(Math.round(actual))
            : String.format("%.2f", actual);
        setMessage(Text.literal(label + ": " + valueStr));
    }
    
    @Override
    protected void applyValue() {
        onChange.accept(getActualValue());
    }
    
    /**
     * Get the actual value (mapped from 0-1 to min-max).
     */
    public float getActualValue() {
        return (float) (min + value * (max - min));
    }
    
    /**
     * Set value (in actual units, not normalized).
     */
    public void setActualValue(float actual) {
        this.value = normalize(actual, min, max);
        updateMessage();
    }
}
```

---

## 7. Logging Integration

Add to `Logging.java`:

```java
// New channel for GUI
public static final Channel GUI = new Channel("GUI", "gui", LogLevel.INFO);
```

Usage in error handling:

```java
// In panel or screen:
private void safeUpdate(String field, Runnable update) {
    try {
        update.run();
        state.markDirty();
        applyToDebugField();
    } catch (Exception e) {
        Logging.GUI.topic("error")
            .alwaysChat()  // ← Player sees this!
            .kv("field", field)
            .exception(e)
            .error("Invalid value: {}", e.getMessage());
        
        // Show in-GUI error (optional)
        showError(field + ": " + e.getMessage());
    }
}
```

---

## 8. Usage Example

```java
public class QuickPanel extends AbstractPanel {
    
    @Override
    public void init(GuiState state, int x, int y, int width) {
        GuiLayout layout = GuiLayout.create(x + GuiConstants.MARGIN, y, width - 2 * GuiConstants.MARGIN);
        
        // Shape type dropdown
        addWidget(GuiWidgets.enumDropdown(
            layout.x(), layout.y(), layout.width(),
            "Shape", ShapeType.class, state.getShapeType(),
            "Primary shape geometry",
            val -> safeUpdate("shape", () -> state.setShapeType(val))
        ));
        layout.nextRow();
        
        // Radius slider
        addWidget(GuiWidgets.slider(
            layout.x(), layout.y(), layout.width(),
            "Radius", 0.1f, 10f, state.getRadius(),
            "Shape radius in blocks",
            val -> safeUpdate("radius", () -> state.setRadius(val))
        ));
        layout.nextSection();
        
        // Fill mode
        addWidget(GuiWidgets.enumDropdown(
            layout.x(), layout.y(), layout.width(),
            "Fill Mode", FillMode.class, state.getFillMode(),
            "How the shape is rendered",
            val -> safeUpdate("fill", () -> state.setFillMode(val))
        ));
        layout.nextRow();
        
        // Spin toggle
        addWidget(GuiWidgets.toggle(
            layout.x(), layout.y(), layout.width(),
            "Spin", state.isSpinEnabled(),
            "Enable rotation animation",
            val -> safeUpdate("spin", () -> state.setSpinEnabled(val))
        ));
    }
}
```

---

## 9. Summary

| Class | Purpose |
|-------|---------|
| `GuiConstants` | Colors, dimensions, theming - change once, apply everywhere |
| `GuiWidgets` | Factory methods returning native widgets with logging |
| `GuiLayout` | Positioning helpers for consistent spacing |
| `LabeledSlider` | Custom slider extending `SliderWidget` |
| `Logging.GUI` | New channel for GUI events (TRACE + alwaysChat errors) |

---

*Approved design - Ready for implementation*

