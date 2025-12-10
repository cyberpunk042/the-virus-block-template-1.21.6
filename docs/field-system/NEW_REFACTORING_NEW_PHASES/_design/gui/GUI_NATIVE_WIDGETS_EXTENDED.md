# Extended Native Widgets Discovery

> **Discovered:** December 8, 2024  
> **Source:** `agent-tools/gui.widget.txt` (185 widget classes!)

---

## 🌟 High-Value Unexplored Widgets

### Layout Systems (Priority: HIGH)

| Widget | Description | Use Case |
|--------|-------------|----------|
| `GridWidget` | Full grid layout with row/column spanning | Complex forms, option grids |
| `GridWidget.Adder` | Fluent grid builder | Sequential widget adding |
| `DirectionalLayoutWidget` | Horizontal/Vertical layouts | Toolbars, button rows |
| `ThreePartsLayoutWidget` | Header/Content/Footer | Screen layouts |
| `AxisGridWidget` | Single-axis grid | Simple rows/columns |
| `LayoutWidgets.createLabeledWidget()` | Auto-labeled widgets | Form fields |
| `SimplePositioningWidget` | Positioning container | Centering, alignment |

### Advanced Input Widgets (Priority: HIGH)

| Widget | Description | Use Case |
|--------|-------------|----------|
| `EditBoxWidget` | Multiline text input with builder! | JSON input, notes |
| `EditBoxWidget.Builder` | Fluent builder with placeholder, colors | Custom text areas |
| `RangeSliderWidget` | Min/Max range slider | Value ranges |
| `CheckboxWidget` | Checkbox with builder pattern | Boolean options |
| `CheckboxWidget.Builder` | Fluent checkbox builder | Styled checkboxes |
| `LockButtonWidget` | Lock/unlock toggle with icon | Protection toggles |
| `ToggleButtonWidget` | Visual toggle button | On/off states |

### Navigation & Tabs (Priority: HIGH)

| Widget | Description | Use Case |
|--------|-------------|----------|
| `TabNavigationWidget` | Complete tab system! | Multi-tab screens |
| `TabNavigationWidget.Builder` | Fluent tab builder | Tab configuration |
| `TabButtonWidget` | Individual tab button | Custom tabs |
| `PageTurnWidget` | Page turning arrows | Paginated content |

### Display Widgets (Priority: MEDIUM)

| Widget | Description | Use Case |
|--------|-------------|----------|
| `IconWidget` | Icon display | Status icons |
| `IconWidget.Simple` | Simple icon variant | Basic icons |
| `IconWidget.Texture` | Textured icon | Custom icons |
| `TextIconButtonWidget` | Button with icon + text | Action buttons |
| `ItemStackWidget` | Display ItemStack | Item previews |
| `PlayerSkinWidget` | Player skin preview | Player display |
| `LoadingWidget` | Loading indicator | Async operations |
| `ImageWidget` | Image display | Backgrounds, sprites |

### Text Widgets (Priority: MEDIUM)

| Widget | Description | Use Case |
|--------|-------------|----------|
| `MultilineTextWidget` | Multiline text display | Descriptions |
| `ScrollableTextWidget` | Scrolling text | Long descriptions |
| `FittingMultiLineTextWidget` | Auto-fitting text | Dynamic content |
| `FocusableTextWidget` | Focusable text | Accessible text |
| `StringWidget` | Single-line text | Labels |

### List & Scrolling (Priority: MEDIUM)

| Widget | Description | Use Case |
|--------|-------------|----------|
| `ScrollableWidget` | Base scrollable container | Custom scrolling |
| `EntryListWidget` | Full list with entries | Layer lists, profiles |
| `EntryListWidget.Entry` | List entry base | Custom entries |
| `ElementListWidget` | Element-based list | Complex lists |
| `AlwaysSelectedEntryListWidget` | List with persistent selection | Selection lists |
| `OptionListWidget` | Options list | Settings pages |
| `ScrollableLayoutWidget` | Scrollable layout container | Scrolling forms |

---

## 📋 Key Method Signatures

### GridWidget (NEW - Very Useful!)
```java
GridWidget grid = new GridWidget(x, y);
grid.setSpacing(4);
grid.setRowSpacing(4);
grid.setColumnSpacing(4);

// Add widgets at row, col
grid.add(widget, row, col);
grid.add(widget, row, col, rowSpan, colSpan);

// Fluent adder (fills rows automatically)
GridWidget.Adder adder = grid.createAdder(2); // 2 columns
adder.add(widget1);
adder.add(widget2);
adder.add(widget3); // auto-wraps to next row
```

### EditBoxWidget (Multiline Text Input!)
```java
EditBoxWidget editBox = EditBoxWidget.builder()
    .x(10).y(10)
    .placeholder(Text.literal("Enter text..."))
    .textColor(0xFFFFFF)
    .cursorColor(0x00FFFF)
    .textShadow(true)
    .hasBackground(true)
    .build(textRenderer, width, height, Text.literal("Label"));

editBox.setMaxLength(500);
editBox.setMaxLines(10);
editBox.setChangeListener(text -> { ... });
String text = editBox.getText();
```

### TabNavigationWidget (Complete Tab System!)
```java
TabNavigationWidget tabs = TabNavigationWidget.builder(tabManager, width)
    .tabs(tab1, tab2, tab3)
    .build();

tabs.selectTab(0, true);
tabs.setTabTooltip(0, Tooltip.of(Text.literal("...")));
tabs.setTabActive(1, false); // Disable tab
int current = tabs.getCurrentTabIndex();
```

### CheckboxWidget (With Builder!)
```java
CheckboxWidget checkbox = CheckboxWidget.builder(Text.literal("Option"), textRenderer)
    .pos(x, y)
    .selected(true)
    .callback((cb, selected) -> { ... })
    .build();
```

### ThreePartsLayoutWidget (Screen Layout!)
```java
ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);
layout.setFooterHeight(40);

layout.addHeader(Text.literal("Title"), textRenderer);
layout.addBody(contentWidget);
layout.addFooter(buttonWidget);

layout.arrangeElements();
```

---

## 🎯 Recommendations for Our GUI

### Immediate Adoption (Batch 6+)

1. **Replace manual layout with `GridWidget`**
   - QuickPanel → GridWidget with 1-2 columns
   - AdvancedPanel → GridWidget with labeled rows

2. **Use `EditBoxWidget` for JSON/text input**
   - Profile descriptions
   - Custom value input

3. **Use native `TabNavigationWidget`**
   - Already have TabType enum
   - Replace manual tab buttons

4. **Use `LayoutWidgets.createLabeledWidget()`**
   - Automatic label + widget alignment
   - Consistent spacing

### Future Enhancements

1. **ScrollableLayoutWidget** for Advanced Panel
2. **EntryListWidget** for layer/primitive lists
3. **CheckboxWidget.Builder** for boolean options
4. **IconWidget** for status indicators
5. **PlayerSkinWidget** for preview? (interesting!)

---

## 🔍 Full Class Count by Category

| Category | Count | Notes |
|----------|-------|-------|
| Base widgets | 12 | AbstractWidget, ClickableWidget, etc. |
| Buttons | 15 | Button, Toggle, Checkbox, Lock |
| Text | 10 | String, Multiline, Scrollable |
| Input | 8 | TextField, EditBox, Slider |
| Lists | 12 | Entry, Element, Option lists |
| Layout | 18 | Grid, Axis, Directional, Three-part |
| Navigation | 6 | Tab, Page, Scroll |
| Display | 10 | Icon, Image, Item, Player |
| Specialized | 94 | Screen-specific widgets |
| **TOTAL** | **185** | |

---

## Next Steps

1. ✅ Document created
2. ⬜ Update `GuiWidgets.java` with factory methods for discovered widgets
3. ⬜ Refactor QuickPanel to use GridWidget
4. ⬜ Consider TabNavigationWidget for main screen

---

*v1.0 - Initial discovery*




