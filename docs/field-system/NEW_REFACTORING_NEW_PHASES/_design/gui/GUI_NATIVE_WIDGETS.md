# Minecraft 1.21 Native GUI Widgets

> **Purpose:** Reference for native Minecraft widgets we can use directly  
> **Status:** Reference document  
> **Created:** December 8, 2024  
> **Source:** Queried from tiny mappings via `01_query_tiny_mappings.py`

---

## 1. Widget Availability Summary

| Our Need | Minecraft Class | Build Custom? | Notes |
|----------|-----------------|---------------|-------|
| Enum selector | `CyclingButtonWidget` | ❌ Use native | Perfect for enums! |
| Toggle on/off | `CyclingButtonWidget.onOffBuilder()` | ❌ Use native | Built-in toggle |
| Button | `ButtonWidget` | ❌ Use native | With builder + tooltip |
| Base slider | `SliderWidget` | ✅ Extend | Add label + value display |
| Range slider | `RangeSliderWidget` | ❌ Use native | New in 1.21! |
| Tabs | `TabButtonWidget` + `TabManager` | ❌ Use native | Full tab system |
| Icon button | `TexturedButtonWidget` | ❌ Use native | For toolbar |
| Toggle button | `ToggleButtonWidget` | ❌ Use native | Visual toggle |
| Color picker | - | ✅ Build | No native equivalent |
| Vec3 editor | - | ✅ Build | 3x inputs |
| Expandable section | - | ✅ Build | Collapsible panel |

---

## 2. ButtonWidget

**Package:** `net.minecraft.client.gui.widget.ButtonWidget`

### Builder Pattern (Recommended)

```java
ButtonWidget button = ButtonWidget.builder(
    Text.literal("Click Me"),           // Button text
    btn -> { /* onPress handler */ }    // Press action
)
.position(x, y)           // Position
.size(width, height)      // Dimensions
.tooltip(Tooltip.of(Text.literal("Hover text")))  // Tooltip!
.build();

addDrawableChild(button);
```

### Key Methods

| Method | Purpose |
|--------|---------|
| `builder(Text, PressAction)` | Create builder |
| `Builder.position(int x, int y)` | Set position |
| `Builder.size(int w, int h)` | Set dimensions |
| `Builder.dimensions(x, y, w, h)` | All at once |
| `Builder.tooltip(Tooltip)` | Add tooltip |
| `Builder.build()` | Create widget |

### PressAction Interface

```java
@FunctionalInterface
public interface PressAction {
    void onPress(ButtonWidget button);
}
```

---

## 3. CyclingButtonWidget (ENUM DROPDOWN!)

**Package:** `net.minecraft.client.gui.widget.CyclingButtonWidget`

### For Enum Selection

```java
CyclingButtonWidget<FillMode> fillModeButton = CyclingButtonWidget
    .<FillMode>builder(mode -> Text.literal(mode.name()))  // Value to text
    .values(FillMode.values())                              // All enum values
    .initially(FillMode.SOLID)                              // Default
    .tooltip(mode -> Tooltip.of(Text.literal(mode.getDescription())))
    .build(
        x, y, width, height,
        Text.literal("Fill Mode"),                          // Label
        (btn, value) -> onFillModeChanged(value)            // On change
    );

addDrawableChild(fillModeButton);
```

### For On/Off Toggle

```java
CyclingButtonWidget<Boolean> toggleButton = CyclingButtonWidget
    .onOffBuilder(Text.literal("ON"), Text.literal("OFF"))  // Custom labels
    .initially(true)
    .build(
        x, y, width, height,
        Text.literal("Spin Enabled"),
        (btn, value) -> onSpinToggled(value)
    );

// Or simpler:
CyclingButtonWidget<Boolean> simpleToggle = CyclingButtonWidget
    .onOffBuilder()  // Uses default "ON"/"OFF"
    .initially(false)
    .build(x, y, w, h, Text.literal("Feature"), this::onToggle);
```

### Key Methods

| Method | Purpose |
|--------|---------|
| `builder(Function<T, Text>)` | Create builder with value-to-text mapper |
| `onOffBuilder()` | Boolean toggle builder |
| `onOffBuilder(Text on, Text off)` | Custom on/off labels |
| `Builder.values(T...)` | Set available values |
| `Builder.values(Collection<T>)` | Set from collection |
| `Builder.initially(T)` | Set initial value |
| `Builder.tooltip(TooltipFactory)` | Dynamic tooltip per value |
| `getValue()` | Get current value |
| `setValue(T)` | Set value programmatically |
| `cycle(int)` | Cycle by delta (+1 or -1) |

### UpdateCallback Interface

```java
@FunctionalInterface
public interface UpdateCallback<T> {
    void onValueChange(CyclingButtonWidget<T> button, T value);
}
```

---

## 4. SliderWidget (BASE - EXTEND THIS)

**Package:** `net.minecraft.client.gui.widget.SliderWidget`

### Constructor

```java
public SliderWidget(
    int x, int y,           // Position
    int width, int height,  // Size
    Text text,              // Label text
    double value            // Initial value (0.0 - 1.0)
)
```

### Abstract Methods (MUST Override)

```java
// Called when slider value changes - update your backing field
protected abstract void updateMessage();

// Called when user releases slider - apply the value
protected abstract void applyValue();
```

### Example: LabeledSlider

```java
public class LabeledSlider extends SliderWidget {
    private final String label;
    private final double min, max;
    private final Consumer<Double> onChange;
    
    public LabeledSlider(int x, int y, int w, int h, 
                         String label, double min, double max, 
                         double initial, Consumer<Double> onChange) {
        super(x, y, w, h, Text.empty(), (initial - min) / (max - min));
        this.label = label;
        this.min = min;
        this.max = max;
        this.onChange = onChange;
        updateMessage();
    }
    
    @Override
    protected void updateMessage() {
        double actual = min + value * (max - min);
        setMessage(Text.literal(label + ": " + String.format("%.2f", actual)));
    }
    
    @Override
    protected void applyValue() {
        double actual = min + value * (max - min);
        onChange.accept(actual);
    }
    
    // Public getter for actual value
    public double getActualValue() {
        return min + value * (max - min);
    }
}
```

### Key Methods

| Method | Purpose |
|--------|---------|
| `setValue(double)` | Set normalized value (0-1) |
| `value` (field) | Current normalized value |
| `setValueFromMouse(double)` | Update from mouse drag |
| `getTexture()` | Background texture |
| `getHandleTexture()` | Slider handle texture |

---

## 5. RangeSliderWidget (NEW in 1.21!)

**Package:** `net.minecraft.client.gui.screen.dialog.InputControlHandlers$NumberRangeInputControlHandler$RangeSliderWidget`

### Key Methods

| Method | Purpose |
|--------|---------|
| `getActualValue()` | Get current float value |
| `valueToString(float)` | Format value for display |
| `getLabel()` | Get label string |
| `getFormattedLabel(control, value)` | Get formatted label Text |

### Usage Pattern

This is used internally by Minecraft's dialog system. For our needs, we may want to extend `SliderWidget` to create a dual-handle range slider.

---

## 6. ToggleButtonWidget

**Package:** `net.minecraft.client.gui.widget.ToggleButtonWidget`

### Constructor

```java
public ToggleButtonWidget(
    int x, int y,
    int width, int height,
    boolean toggled          // Initial state
)
```

### Key Methods

| Method | Purpose |
|--------|---------|
| `isToggled()` | Get current state |
| `setToggled(boolean)` | Set state |
| `setTextures(ButtonTextures)` | Set on/off textures |

### Example

```java
ToggleButtonWidget toggle = new ToggleButtonWidget(x, y, 20, 20, false);
toggle.setTextures(new ButtonTextures(
    Identifier.of("mymod", "toggle_off"),
    Identifier.of("mymod", "toggle_on"),
    Identifier.of("mymod", "toggle_off_hover"),
    Identifier.of("mymod", "toggle_on_hover")
));
addDrawableChild(toggle);
```

---

## 7. TabButtonWidget + TabManager

**Package:** `net.minecraft.client.gui.widget.TabButtonWidget`

### TabManager

```java
TabManager tabManager = new TabManager(this::addDrawableChild, this::remove);
```

### TabButtonWidget

```java
Tab myTab = new Tab() {
    @Override
    public Text getTitle() { return Text.literal("Quick"); }
    
    @Override
    public void forEachChild(Consumer<ClickableWidget> consumer) {
        // Add all widgets for this tab
        consumer.accept(mySlider);
        consumer.accept(myButton);
    }
    
    @Override
    public void refreshGrid(ScreenRect rect) {
        // Position widgets within rect
    }
};

TabButtonWidget tabButton = new TabButtonWidget(tabManager, myTab, 60, 20);
addDrawableChild(tabButton);
```

### Key Methods

| Method | Purpose |
|--------|---------|
| `getTab()` | Get associated Tab |
| `isCurrentTab()` | Check if selected |
| `drawCurrentTabLine(...)` | Draw selection indicator |

---

## 8. TexturedButtonWidget (Icon Buttons)

**Package:** `net.minecraft.client.gui.widget.TexturedButtonWidget`

### Constructor

```java
public TexturedButtonWidget(
    int x, int y,
    int width, int height,
    ButtonTextures textures,        // Normal/hovered/disabled textures
    PressAction pressAction
)
```

### With Tooltip

```java
TexturedButtonWidget iconButton = new TexturedButtonWidget(
    x, y, 20, 20,
    new ButtonTextures(
        Identifier.of("mymod", "button_normal"),
        Identifier.of("mymod", "button_hover")
    ),
    btn -> onIconClicked()
);
iconButton.setTooltip(Tooltip.of(Text.literal("Click me!")));
addDrawableChild(iconButton);
```

---

## 9. TextIconButtonWidget (Text + Icon)

**Package:** `net.minecraft.client.gui.widget.TextIconButtonWidget`

### Builder Pattern

```java
TextIconButtonWidget button = TextIconButtonWidget
    .builder(Text.literal("Save"), btn -> onSave(), true)  // showText
    .texture(Identifier.of("mymod", "save_icon"), 16, 16)
    .width(80)
    .build();

addDrawableChild(button);
```

### Key Methods

| Method | Purpose |
|--------|---------|
| `builder(Text, PressAction, boolean)` | Create builder |
| `Builder.texture(Identifier, w, h)` | Set icon texture |
| `Builder.width(int)` | Set button width |
| `Builder.dimension(w, h)` | Set both dimensions |
| `Builder.build()` | Create widget |

---

## 10. Tooltip System

**Package:** `net.minecraft.client.gui.tooltip.Tooltip`

### Creating Tooltips

```java
// Simple tooltip
Tooltip simple = Tooltip.of(Text.literal("This is a tooltip"));

// Multi-line tooltip
Tooltip multiLine = Tooltip.of(
    Text.literal("Line 1\nLine 2\nLine 3")
);

// With narration (accessibility)
Tooltip withNarration = Tooltip.of(
    Text.literal("Display text"),
    Text.literal("Screen reader text")
);
```

### Applying to Widgets

```java
// On ButtonWidget via builder
ButtonWidget.builder(text, action)
    .tooltip(Tooltip.of(Text.literal("Help text")))
    .build();

// On any widget after creation
widget.setTooltip(Tooltip.of(Text.literal("Help text")));

// Dynamic tooltip on CyclingButtonWidget
CyclingButtonWidget.<MyEnum>builder(...)
    .tooltip(value -> Tooltip.of(Text.literal(value.getDescription())))
    .build(...);
```

---

## 11. DrawContext (Rendering)

**Package:** `net.minecraft.client.gui.DrawContext`

### Common Methods for Custom Widgets

```java
// Fill rectangle
context.fill(x1, y1, x2, y2, color);

// Draw border
context.drawBorder(x, y, width, height, color);

// Draw text
context.drawText(textRenderer, "Hello", x, y, color, shadow);
context.drawTextWithShadow(textRenderer, text, x, y, color);
context.drawCenteredTextWithShadow(textRenderer, text, centerX, y, color);

// Draw texture
context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, u, v, width, height, texWidth, texHeight);

// Draw 9-slice (for scalable backgrounds)
context.drawGuiTexture(RenderLayer::getGuiTextured, texture, x, y, width, height);

// Scissor (clip rendering)
context.enableScissor(x1, y1, x2, y2);
// ... render clipped content ...
context.disableScissor();
```

---

## 12. Screen Base Class

**Package:** `net.minecraft.client.gui.screen.Screen`

### Lifecycle

```java
public class MyScreen extends Screen {
    public MyScreen() {
        super(Text.literal("My Screen"));
    }
    
    @Override
    protected void init() {
        // Called when screen opens or resizes
        // Add all widgets here
        addDrawableChild(myButton);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render background
        renderBackground(context, mouseX, mouseY, delta);
        
        // Render widgets (automatic)
        super.render(context, mouseX, mouseY, delta);
        
        // Custom rendering
        context.drawText(textRenderer, "Custom", 10, 10, 0xFFFFFF, true);
    }
    
    @Override
    public void tick() {
        // Called every game tick (20/sec)
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle keyboard
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public void close() {
        // Cleanup before closing
        client.setScreen(parent);
    }
}
```

### Key Methods

| Method | Purpose |
|--------|---------|
| `init()` | Setup widgets |
| `render(...)` | Draw screen |
| `tick()` | Game tick update |
| `close()` | Close screen |
| `addDrawableChild(T)` | Add widget (renders + handles input) |
| `addDrawable(Drawable)` | Add render-only element |
| `remove(Element)` | Remove widget |
| `clearChildren()` | Remove all widgets |
| `setFocused(Element)` | Set keyboard focus |

---

## 13. Color Constants

```java
// Common colors (ARGB format)
int WHITE = 0xFFFFFFFF;
int BLACK = 0xFF000000;
int RED = 0xFFFF0000;
int GREEN = 0xFF00FF00;
int BLUE = 0xFF0000FF;
int GRAY = 0xFF808080;
int DARK_GRAY = 0xFF404040;
int TRANSPARENT = 0x00000000;

// Semi-transparent
int SEMI_BLACK = 0x80000000;  // 50% black
int SEMI_WHITE = 0x80FFFFFF;  // 50% white

// From ColorConfig (our mod)
ColorConfig.argb(ColorSlot.UI_BACKGROUND);
```

---

## 14. Summary: What We Use vs Build

### ✅ USE NATIVE (No custom code needed)

| Widget | Class | Usage |
|--------|-------|-------|
| Button | `ButtonWidget.builder()` | Actions |
| Enum dropdown | `CyclingButtonWidget` | Shape type, fill mode, etc. |
| Toggle | `CyclingButtonWidget.onOffBuilder()` | Enable/disable features |
| Tabs | `TabButtonWidget` + `TabManager` | Quick/Advanced/Debug tabs |
| Icon button | `TexturedButtonWidget` | Toolbar icons |
| Tooltip | `Tooltip.of()` | Hover help |

### ✅ EXTEND NATIVE (Small wrapper)

| Widget | Extends | Add |
|--------|---------|-----|
| `LabeledSlider` | `SliderWidget` | Label + formatted value |
| `RangeSlider` | `SliderWidget` | Dual handles for min/max |

### ✅ BUILD CUSTOM (New widgets)

| Widget | Why | Complexity |
|--------|-----|------------|
| `ColorButton` | Color swatch + hex popup | Medium |
| `Vec3Editor` | 3x linked inputs | Low |
| `ExpandableSection` | Collapsible panel | Medium |
| `ProfileDropdown` | Profile list + actions | Medium |

---

*Reference document - Query more widgets with:*
```bash
python3 scripts/01_query_tiny_mappings.py <WidgetName> --search-jars
```

