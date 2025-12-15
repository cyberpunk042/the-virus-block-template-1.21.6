# GUI Architecture Refactoring - Comprehensive Plan

## Goal
Reduce `FieldCustomizerScreen` from **1627 lines** to **~400 lines** while building a flexible, permission-aware, renderer-mode-aware component architecture.

---

## Terminology Clarification

### Two Independent Mode Toggles

| Toggle | Current Name | **New Name** | Affects | Location in UI |
|--------|--------------|--------------|---------|----------------|
| **Renderer Mode** | "Fast" / "Accurate" | **Simplified** / **Standard** | In-game rendering + Menu visibility | TabBar |
| **Preview Mode** | (not implemented) | **Fast Preview** / **Advanced Preview** | Only the 3D preview box | Inside preview area (fullscreen only) |

**Key Distinction:**
- **Simplified/Standard** toggle: Changes _BOTH_ the in-world renderer AND hides unsupported menu controls
- **Fast/Advanced Preview** toggle: _ONLY_ changes how the 3D preview box renders (uses Standard Renderer when Advanced)

### Visibility Hierarchy

```
Rendering Mode Toggle: [Simplified] ←→ [Standard]
        │
        ├── In Simplified mode:
        │   • In-game uses SimplifiedFieldRenderer (fast, fewer features)
        │   • Advanced Tab: HIDDEN
        │   • Wave/Wobble controls: HIDDEN
        │   • Bindings controls: HIDDEN
        │   • Individual controls like "Wave Amplitude" slider: HIDDEN
        │   • 3D Preview: Always Fast Preview (no toggle visible)
        │
        └── In Standard mode:
            • In-game uses full FieldRenderer pipeline
            • All tabs visible
            • All controls visible
            • 3D Preview: Shows toggle [Fast Preview] ←→ [Advanced Preview]
                          Default: Fast Preview
                          Advanced: Uses Standard Renderer in preview box
```

---

## Current Code Structure

### Where "Fast/Accurate" Lives

| File | What | Rename To |
|------|------|-----------|
| `SimplifiedFieldRenderer.java:81` | `advancedModeEnabled` | `standardModeEnabled` |
| `SimplifiedFieldRenderer.java:105` | `isAdvancedModeEnabled()` | `isStandardModeEnabled()` |
| `RendererCapabilities.java:165` | `"Accurate" : "Fast"` | `"Standard" : "Simplified"` |
| `RendererCapabilities.java:172` | `isAccurateMode()` | `isStandardMode()` |
| `RendererCapabilities.java:179` | `isFastMode()` | `isSimplifiedMode()` |

### 3D Preview Rendering

Currently in `FieldCustomizerScreen.render3DPreview()` (lines 1344-1380):
- Uses `FieldPreviewRenderer` for the 3D box
- Does NOT have a separate preview mode toggle
- **Needs:** Add `previewModeAdvanced` boolean + toggle (visible only in Standard renderer mode)

---

## Visibility at Control Level (Not Just Panels)

The `@RequiresFeature` annotation currently works on panels. We need to extend it to work on individual controls:

### Current Pattern (Panel Level)
```java
@RequiresFeature(Feature.WAVE)
public class AnimationSubPanel extends AbstractPanel { ... }
```

### New Pattern (Control Level via Builder)
```java
// In GuiWidgets.java - conditional visibility
public static <T> CyclingButtonWidget<T> visibleWhen(
    CyclingButtonWidget<T> widget, 
    BooleanSupplier condition);

public static ButtonWidget visibleWhen(
    ButtonWidget widget, 
    BooleanSupplier condition);

// Usage:
var waveAmplitudeSlider = GuiWidgets.visibleWhen(
    GuiWidgets.slider("Wave Amplitude", 0, 10, value -> state.set("wave.amplitude", value)),
    () -> RendererCapabilities.isSupported(Feature.WAVE)
);
```

### New Pattern (Annotation on Fields)
```java
public class AnimationSubPanel extends AbstractPanel {
    
    @VisibleWhen(Feature.WAVE)
    private Slider waveAmplitudeSlider;
    
    @VisibleWhen(Feature.WOBBLE)
    private Slider wobbleIntensitySlider;
    
    // These always visible
    private Slider spinSpeedSlider;
    private Slider pulseSpeedSlider;
}
```

---

## Core Design Patterns

### 1. RendererCapabilities Enhancement

```java
public final class RendererCapabilities {
    
    // RENAMED methods
    public static boolean isStandardModeEnabled() {
        return SimplifiedFieldRenderer.isStandardModeEnabled();
    }
    
    public static boolean isSimplifiedMode() {
        return !isStandardModeEnabled();
    }
    
    public static String getCurrentModeName() {
        return isStandardModeEnabled() ? "Standard" : "Simplified";
    }
    
    // For menu display labels
    public static String getModeToggleLabel() {
        return isStandardModeEnabled() ? "⚙ Standard" : "⚡ Simplified";
    }
}
```

### 2. Preview Mode State

```java
// In FieldEditState or new PreviewState
public class PreviewModeState {
    private static boolean advancedPreviewEnabled = false;
    
    public static boolean isAdvancedPreviewEnabled() {
        // Only allowed when Standard renderer mode is on
        return advancedPreviewEnabled && RendererCapabilities.isStandardModeEnabled();
    }
    
    public static void setAdvancedPreviewEnabled(boolean enabled) {
        advancedPreviewEnabled = enabled;
    }
    
    public static boolean isAdvancedPreviewToggleVisible() {
        return RendererCapabilities.isStandardModeEnabled();
    }
}
```

### 3. Control-Level Visibility

```java
// New: VisibilityPredicate for individual controls
@FunctionalInterface
public interface VisibilityPredicate {
    boolean isVisible();
}

// In AbstractPanel or new base class
protected void addVisibleWhen(ClickableWidget widget, VisibilityPredicate pred) {
    widgetVisibility.put(widget, pred);
    if (pred.isVisible()) {
        widgets.add(widget);
    }
}

// Called when renderer mode changes
public void refreshControlVisibility() {
    widgets.clear();
    for (var entry : widgetVisibility.entrySet()) {
        if (entry.getValue().isVisible()) {
            widgets.add(entry.getKey());
        }
    }
}
```

---

## Updated Visibility Matrix

| Element | Show When | Hide When |
|---------|-----------|-----------|
| **Advanced Tab** | Standard mode | Simplified mode |
| **Debug Tab** | `debugUnlocked` (OP) | Not OP |
| **3D Preview Mode Toggle** | Standard mode + Fullscreen | Simplified mode OR Windowed |
| **Wave controls** | Standard mode | Simplified mode |
| **Wobble controls** | Standard mode | Simplified mode |
| **Bindings panel** | Standard mode | Simplified mode |
| **Linking panel** | Standard mode | Simplified mode |
| **Save to Server btn** | OP | Not OP |

### Per-Tab Control Visibility

| Tab | Sub-Tab | Controls Hidden in Simplified |
|-----|---------|-------------------------------|
| **Quick** | Fill | (all visible) |
| **Quick** | Appearance | Glow, Color Cycle |
| **Quick** | Visibility | (all visible) |
| **Quick** | Transform | (all visible) |
| **Advanced** | Animation | Wave, Wobble (entire sub-panel might be simpler) |
| **Advanced** | Prediction | ENTIRE TAB HIDDEN |
| **Advanced** | Orbit | (all visible) |
| **Advanced** | Modifiers | Wave-based modifiers |
| **Debug** | Bindings | ENTIRE TAB HIDDEN |
| **Debug** | Trigger | (all visible) |
| **Debug** | Lifecycle | ENTIRE TAB HIDDEN |

---

## Phase 1: Rename Fast/Accurate → Simplified/Standard

### 1.1 SimplifiedFieldRenderer.java

```diff
- private static boolean advancedModeEnabled = true;
+ private static boolean standardModeEnabled = true;

- public static void setAdvancedModeEnabled(boolean enabled) {
-     advancedModeEnabled = enabled;
+ public static void setStandardModeEnabled(boolean enabled) {
+     standardModeEnabled = enabled;

- public static boolean isAdvancedModeEnabled() {
-     return advancedModeEnabled;
+ public static boolean isStandardModeEnabled() {
+     return standardModeEnabled;
```

### 1.2 RendererCapabilities.java

```diff
- public static String getCurrentModeName() {
-     return SimplifiedFieldRenderer.isAdvancedModeEnabled() ? "Accurate" : "Fast";
- }
+ public static String getCurrentModeName() {
+     return SimplifiedFieldRenderer.isStandardModeEnabled() ? "Standard" : "Simplified";
+ }

- public static boolean isAccurateMode() {
-     return SimplifiedFieldRenderer.isAdvancedModeEnabled();
- }
+ public static boolean isStandardMode() {
+     return SimplifiedFieldRenderer.isStandardModeEnabled();
+ }

- public static boolean isFastMode() {
-     return !SimplifiedFieldRenderer.isAdvancedModeEnabled();
- }
+ public static boolean isSimplifiedMode() {
+     return !SimplifiedFieldRenderer.isStandardModeEnabled();
+ }
```

### 1.3 UI Labels

```diff
// In FieldCustomizerScreen or TabBar
- .values(List.of(false, true))  // false=Fast, true=Accurate
+ .values(List.of(false, true))  // false=Simplified, true=Standard

- .valueToText(v -> Text.literal(v ? "Accurate ⚙" : "Fast ⚡"))
+ .valueToText(v -> Text.literal(v ? "Standard ⚙" : "Simplified ⚡"))
```

---

## Phase 2: Foundation (Options B + D)

### 2.1 Widget Builder Extensions

**File:** `GuiWidgets.java`

```java
// Value-only cycler (no label, just shows value)
public static <T> CyclingButtonWidget<T> valueCycler(...);

// Boolean cycler without label prefix  
public static CyclingButtonWidget<Boolean> boolCycler(...);

// Row splitter
public static RowLayout row(Bounds bounds, int columns);

// Conditional visibility wrapper
public static <W extends ClickableWidget> W visibleWhen(W widget, BooleanSupplier condition) {
    // Store condition for later refresh
    WidgetVisibility.register(widget, condition);
    widget.visible = condition.getAsBoolean();
    return widget;
}
```

### 2.2 WidgetCollector with Visibility

```java
public class WidgetCollector {
    public interface WidgetProvider {
        List<ClickableWidget> getWidgets();
        default boolean isVisible() { return true; }
    }
    
    public static List<ClickableWidget> collectVisible(WidgetProvider... providers) {
        List<ClickableWidget> all = new ArrayList<>();
        for (WidgetProvider p : providers) {
            if (p != null && p.isVisible()) {
                for (ClickableWidget w : p.getWidgets()) {
                    if (w.visible) {  // Also check individual widget visibility
                        all.add(w);
                    }
                }
            }
        }
        return all;
    }
}
```

---

## Phase 3: Component Extraction

(Same as before - HeaderBar, TabBar, SelectorBar, ContentArea, etc.)

### TabBar Changes

```java
public class TabBar implements ScreenComponent {
    private ButtonWidget quickTab, advancedTab, debugTab, profilesTab;
    private CyclingButtonWidget<Boolean> rendererModeToggle;  // Simplified/Standard
    
    public void refreshVisibility() {
        // Advanced tab hidden in Simplified mode
        advancedTab.visible = RendererCapabilities.isStandardMode();
        
        // Debug tab hidden if not OP
        debugTab.visible = state.getBool("debugUnlocked");
    }
}
```

### ContentArea Changes

```java
public class ContentArea implements ScreenComponent {
    
    public void refreshForRendererMode() {
        // Rebuild sub-tabs - they'll auto-hide based on @RequiresFeature
        quickSubTabs.rebuildWidgets();
        advancedSubTabs.rebuildWidgets();
        debugSubTabs.rebuildWidgets();
        
        // Also refresh individual control visibility
        notifyControlVisibilityChanged();
    }
}
```

---

## Phase 4: Layout Integration

(Same as before - wire LayoutManager, add render methods)

---

## Phase 5: Advanced Preview Feature (NEW - Future Work)

### Goal
When in **Standard** renderer mode, add a toggle in the 3D preview area that switches between:
- **Fast Preview**: Uses current inline tessellation (existing code)
- **Advanced Preview**: Uses the full `FieldRenderer` pipeline in the preview box

### Why This Matters
The 3D preview box currently uses `FieldPreviewRenderer` which does its own simplified rendering.
In Advanced Preview mode, we want to render using the same `FieldRenderer` that renders in-world, 
so the preview matches exactly what the player sees.

### Implementation Sketch

#### 5.1 Preview Mode State

**New File:** `gui/state/PreviewModeState.java`

```java
public class PreviewModeState {
    private static boolean advancedPreviewEnabled = false;
    
    public static boolean isAdvancedPreviewEnabled() {
        // Only works when Standard renderer mode is active
        return advancedPreviewEnabled && RendererCapabilities.isStandardMode();
    }
    
    public static void setAdvancedPreviewEnabled(boolean enabled) {
        advancedPreviewEnabled = enabled;
    }
    
    public static boolean isToggleVisible() {
        // Only show toggle in fullscreen + Standard mode
        return RendererCapabilities.isStandardMode();
    }
}
```

#### 5.2 Preview Toggle Widget

**In:** `component/TabBar.java` or new `PreviewArea.java`

```java
// Checkbox or cycling button for preview mode
previewModeToggle = GuiWidgets.boolCycler(
    bounds.x(), bounds.y(), 120, 20,
    PreviewModeState.isAdvancedPreviewEnabled(),
    "⚙ Advanced Preview", "⚡ Fast Preview",
    PreviewModeState::setAdvancedPreviewEnabled
);

// Only visible when Standard mode AND fullscreen
previewModeToggle = GuiWidgets.visibleWhen(previewModeToggle, 
    () -> PreviewModeState.isToggleVisible() && layout.hasPreviewWidget()
);
```

#### 5.3 render3DPreview() Changes

**In:** `FieldCustomizerScreen.java`

```java
private void render3DPreview(DrawContext context, Bounds bounds, float delta) {
    if (PreviewModeState.isAdvancedPreviewEnabled()) {
        // Use full FieldRenderer pipeline (same as in-world)
        renderAdvancedPreview(context, bounds, delta);
    } else {
        // Use current fast preview (existing code)
        renderFastPreview(context, bounds, delta);
    }
}

private void renderAdvancedPreview(DrawContext context, Bounds bounds, float delta) {
    // Convert FieldEditState to FieldDefinition
    var definition = DefinitionBuilder.fromState(state);
    
    // Set up framebuffer/offscreen rendering to preview area
    // This is complex - may need to render to texture then blit
    
    // Use FieldRenderer.render() with camera positioned for preview
    FieldRenderer.render(
        matrices, consumers, definition,
        previewCameraPos, 1.0f, time, 0.8f
    );
}
```

#### 5.4 Challenges for Advanced Preview

1. **Camera Setup**: Need a separate camera position/rotation for preview box
2. **Render Target**: May need to render to a framebuffer and blit to the preview area
3. **Performance**: Running full FieldRenderer in a GUI box could be expensive
4. **Scissoring**: Need to clip to preview bounds

This is a significant feature and should be tracked separately.

---

## Summary: Files to Create/Modify

| File | Action | Purpose |
|------|--------|---------|
| `SimplifiedFieldRenderer.java` | MODIFY | Rename Fast/Accurate → Simplified/Standard |
| `RendererCapabilities.java` | MODIFY | Rename methods + labels |
| `util/WidgetCollector.java` | NEW | Auto-registration with visibility |
| `util/RowLayout.java` | NEW | Row/column splitting |
| `util/WidgetVisibility.java` | NEW | Control-level visibility tracking |
| `state/PreviewModeState.java` | NEW | Advanced preview toggle state |
| `component/ScreenComponent.java` | NEW | Component interface |
| `component/VisibilityController.java` | NEW | Centralized visibility logic |
| `component/HeaderBar.java` | NEW | Title bar |
| `component/TabBar.java` | NEW | Main tabs + toggles |
| `component/SelectorBar.java` | NEW | Layer/primitive |
| `component/ContentArea.java` | NEW | Sub-tabs management |
| `widget/ModalFactory.java` | NEW | Modal dialogs |
| `layout/LayoutManager.java` | MODIFY | Add render methods |
| `screen/FieldCustomizerScreen.java` | **REDUCE** | ~400 lines thin orchestrator |

---

## Implementation Order

```
Phase 1: Rename Fast/Accurate → Simplified/Standard [LOW RISK]
    ↓
Phase 2.1: Widget builders (valueCycler, boolCycler, row) [LOW RISK]
    ↓
Phase 2.2: WidgetCollector + WidgetVisibility [LOW RISK]
    ↓
Phase 3: Component extraction (HeaderBar, TabBar, etc.) [MEDIUM RISK]
    ↓
Phase 4: Layout integration [MEDIUM RISK]
    ↓
Phase 5: Advanced Preview feature [FUTURE WORK]
```

---

## Verification Checklist

### Terminology Rename:
- [ ] UI shows "Simplified" / "Standard" (not Fast/Accurate)
- [ ] Code uses `isStandardMode()` / `isSimplifiedMode()`

### Visibility Testing:
- [ ] Simplified mode → Advanced tab hidden
- [ ] Simplified mode → Wave/Wobble controls hidden in Animation panel
- [ ] Simplified mode → Bindings tab hidden in Debug
- [ ] Standard mode → All tabs and controls visible
- [ ] Non-OP → Debug tab hidden
- [ ] Standard mode + Fullscreen → Preview mode toggle visible
- [ ] Simplified mode → Preview mode toggle hidden

### Control-Level Visibility:
- [ ] Individual sliders hide/show based on renderer mode
- [ ] Layout adjusts when controls are hidden (no gaps)
