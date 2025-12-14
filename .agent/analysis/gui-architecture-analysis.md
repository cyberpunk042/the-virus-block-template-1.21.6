# GUI Architecture Analysis: FieldCustomizerScreen & Related Components

**Date**: 2025-12-14  
**Status**: 🔍 INVESTIGATION IN PROGRESS  
**Focus**: Is FieldCustomizerScreen a God Class? Can we improve the GUI architecture?

---

## Project Context

**the-virus-block** is a Minecraft mod that renders customizable 3D visual field effects. The `FieldCustomizerScreen` is the main GUI for editing field definitions.

### GUI Package Structure

```
src/client/java/net/cyberpunk042/client/gui/
├── config/        (1 file)   - Configuration handling
├── layout/        (10 files) - Layout system (Bounds, GuiMode, LayoutManager, etc.)
├── network/       (0 files)  - Network sync (empty?)
├── panel/         (9 files + 21 sub + 17 v2)
│   ├── AbstractPanel.java     - Base class for panels
│   ├── ActionPanel.java       - Action buttons
│   ├── AdvancedPanel.java     - Advanced settings
│   ├── DebugPanel.java        - Debug panel
│   ├── LayerPanel.java        - Layer management
│   ├── PrimitivePanel.java    - Primitive editing
│   ├── ProfilesPanel.java     - Profile management (40KB!)
│   ├── QuickPanel.java        - Quick settings
│   └── sub/                   - Sub-panels (21 files)
│   └── v2/                    - Version 2 panels (17 files)
├── preview/       (1 file)   - 3D preview renderer
├── render/        (1 file)   - Rendering utilities
├── screen/        (4 files)  - Screen classes
│   └── FieldCustomizerScreen.java (73KB, 1627 lines!)
├── state/         (18 files) - State management
├── util/          (7 files)  - GUI utilities (GuiConstants, GuiWidgets, etc.)
└── widget/        (14 files) - Custom widgets (LabeledSlider, ModalDialog, etc.)
```

---

## Current Problem Areas

### 1. FieldCustomizerScreen Is Massive (God Class?)

**Stats:**
- **1,627 lines** of code
- **73 KB** file size
- **77 outline items** (methods/inner classes)

**Signs of a God Class:**
- Handles BOTH fullscreen AND windowed layouts
- Manages title bar, tabs, selectors, sub-tabs, shape panel, status bar, preview
- Contains widget creation, rendering, input handling, state management
- Has 130+ fields/member variables
- Duplicates logic between `initFullscreenMode()` and `initWindowedMode()`

### 2. Widget Registration Hell

The `registerWidgets()` method at line ~908 manually:
```java
private void registerWidgets() {
    clearChildren();  // Clears ALL widgets
    
    // Then manually re-adds EACH one:
    if (modeToggleBtn != null) addDrawableChild(modeToggleBtn);
    if (fieldToggleBtn != null) addDrawableChild(fieldToggleBtn);
    if (resetBtn != null) addDrawableChild(resetBtn);  // RECENTLY ADDED - was missing!
    if (closeBtn != null) addDrawableChild(closeBtn);
    // ... many more ...
}
```

**Problems:**
- Easy to forget to add buttons → they disappear
- Must remember to add new buttons to this method
- No type safety - relies on nullable fields
- Called frequently → performance concern?

### 3. Fullscreen vs Windowed Duplication

```java
initFullscreenMode()     // 20 lines
initWindowedMode()       // 50 lines
initWindowedLeftPanel()  // 130 lines
initWindowedRightPanel() // 100 lines
initTitleBar()           // 32 lines (fullscreen only)
```

The only REAL difference:
- Fullscreen: 2×2 grid with 3D preview in top-left
- Windowed: Side panel overlay on world view

Yet we have ~300 lines of duplicated initialization logic!

### 4. Button Text Issues

Recurring problem with CyclingButtonWidget:
```java
// Without .omitKeyText(), buttons show ": Value" instead of "Value"
CyclingButtonWidget.builder(...)
    .omitKeyText()  // Must remember this EVERY time!
    .build();
```

We've had to fix this in multiple files recently.

### 5. Inconsistent Layout Patterns

- Some widgets positioned absolutely with hardcoded coordinates
- Some use the `Bounds` class
- Some use `GridPane`
- Mixed usage of `panelBounds.x()`, `panelBounds.y()` with manual offsets

### 6. SubTabPane Complexity

The SubTabPane widget is complex (415 lines) and handles:
- Tab buttons
- Content switching
- Bounds management
- Widget registration passthrough

But the screen still manually manages content providers for each tab.

---

## Existing Architecture Assets

### Things That ARE Good

1. **AbstractPanel base class** - 13KB, provides common panel functionality
2. **Bounds record** - Clean 2D rectangle abstraction
3. **GridPane** - 2×2 grid layout helper
4. **SubTabPane** - Tab switching with content providers
5. **ModalDialog** - Reusable modal system
6. **LabeledSlider** - Slider with integrated label
7. **State system** - FieldEditState with annotations

### Layout Classes

| File | Size | Purpose |
|------|------|---------|
| `Bounds.java` | 7KB | 2D rectangle with helpers |
| `GuiMode.java` | 1.5KB | FULLSCREEN/WINDOWED enum |
| `GridPane.java` | 8KB | 2×2 grid layout |
| `LayoutManager.java` | 4KB | Layout switching |
| `LayoutPanel.java` | 9KB | Panel with layout |
| `SidePanel.java` | 9KB | Side panel abstraction |
| `StatusBar.java` | 3KB | Status bar component |
| `FullscreenLayout.java` | 6KB | Fullscreen layout |
| `WindowedLayout.java` | 7KB | Windowed layout |

---

## Investigation Questions

### Is This a God Class?

**Yes, FieldCustomizerScreen exhibits many God Class symptoms:**

1. ✗ Too many responsibilities (UI layout, widget management, rendering, input)
2. ✗ Too many fields (100+)
3. ✗ Too many methods (77)
4. ✗ Duplicated logic between modes
5. ✗ Difficult to modify without introducing bugs
6. ✗ Widget visibility bugs (forgetting to re-add after clear)

### Why Is It So Hard to Work With?

1. **Cognitive overload** - 1600 lines is too much to hold in mind
2. **Hidden dependencies** - Widgets depend on registerWidgets() being correct
3. **No separation of concerns** - Layout + behavior + rendering mixed
4. **Brittle widget management** - clearChildren() + manual re-add pattern

### Why Are There Recurring Bugs?

1. **Button visibility**: registerWidgets() pattern is error-prone
2. **Button text ":"**: Minecraft's CyclingButtonWidget quirk not abstracted
3. **Layout inconsistency**: No unified layout system used throughout

---

## Potential Solutions

### Option A: Extract Layout Strategies

Create a unified layout system that handles both modes:

```java
interface ScreenLayout {
    void init(Screen screen, FieldEditState state);
    void render(DrawContext context, ...);
    Bounds getPreviewBounds();  // null for windowed
    Bounds getContentBounds();
    List<Widget> getAllWidgets();
}

class FullscreenLayout implements ScreenLayout { ... }
class WindowedLayout implements ScreenLayout { ... }
```

### Option B: Widget Container Abstraction

Instead of manually registering widgets:

```java
class WidgetContainer {
    private List<Widget> widgets = new ArrayList<>();
    
    public void add(Widget w) { widgets.add(w); }
    public void addAll(Panel panel) { widgets.addAll(panel.getWidgets()); }
    public void registerAll(Screen screen) {
        for (Widget w : widgets) screen.addDrawableChild(w);
    }
}
```

### Option C: Component-Based Architecture

Break into smaller, self-contained components:

```java
class HeaderBar extends AbstractComponent {
    // Mode toggle, field toggle, reset, close buttons
}

class ContentArea extends AbstractComponent {
    // Tabs + sub-tabs content
}

class SelectorBar extends AbstractComponent {
    // Layer + primitive selectors
}
```

### Option D: Use Builder Pattern for Widgets

Abstract CyclingButtonWidget creation:

```java
// Before:
CyclingButtonWidget.<String>builder(v -> Text.literal(v))
    .values(options)
    .initially(current)
    .tooltip(...)
    .omitKeyText()   // Easy to forget!
    .build(x, y, w, h, Text.empty(), callback);

// After:
GuiWidgets.cycler(options, current)
    .onSelect(callback)
    .tooltip("...")
    .build(x, y, w, h);  // omitKeyText automatic
```

---

## Files to Review

| File | Lines | Priority | Notes |
|------|-------|----------|-------|
| `FieldCustomizerScreen.java` | 1627 | **HIGH** | Main target |
| `AbstractPanel.java` | ~400 | HIGH | Base class - can we use more? |
| `SubTabPane.java` | 415 | MEDIUM | Tab management |
| `GridPane.java` | ~200 | MEDIUM | Layout helper |
| `LayoutManager.java` | ~100 | MEDIUM | Layout switching |
| `GuiWidgets.java` | ~200 | LOW | Widget helpers |
| `ProfilesPanel.java` | 1200 | LOW | Also large, similar issues? |

---

## Next Steps

1. **Metrics**: Count responsibilities in FieldCustomizerScreen
2. **Identify seams**: Where can we extract components?
3. **Prototype**: Try extracting HeaderBar as proof of concept
4. **Design**: Define component interface
5. **Migrate**: Gradually move code to components

---

## Related Documents

- `.agent/tasks/arrangement-system-issues.md` - Pattern system issues (partially resolved)
- `.agent/analysis/polyhedron-face-connection-analysis.md` - Tessellation fixes (resolved)

---

## Session Progress Notes

### 2025-12-14 Session

**Pattern System Fixed:**
- Updated PATTERN_OPTIONS to include patterns from ALL CellTypes
- Added `findPatternByNameForCellType()` to resolve patterns by name in expected CellType
- Fixed subdivision pattern filtering (skip filtering when subdivisions > 0)
- Reset button visibility fixed (added to registerWidgets())

**Current Focus:**
- GUI architecture analysis
- FieldCustomizerScreen refactoring investigation
