# GUI TODO Directives - Working Guide

> **⚠️ READ THIS BEFORE EVERY GUI TODO**  
> **Purpose:** Context restoration and quality control for GUI tasks  
> **Status:** Active Implementation Guide  
> **Parent:** [../../00_TODO_DIRECTIVES.md](../../00_TODO_DIRECTIVES.md) (inherits process rules)

---

## 🧭 Quick Navigation

### Core Documents (Read Before Each TODO)

| Priority | Document | Purpose | When to Read |
|----------|----------|---------|--------------|
| 🔴 1 | [GUI_ARCHITECTURE.md](./GUI_ARCHITECTURE.md) | Access levels, DEBUG FIELD, state model | Every time |
| 🔴 2 | [GUI_CLASS_DIAGRAM.md](./GUI_CLASS_DIAGRAM.md) | Screens, panels, widgets, network | Every time | 
| 🟡 3 | [GUI_UTILITIES.md](./GUI_UTILITIES.md) | GuiWidgets factory, GuiConstants, logging | When writing widgets |
| 🟡 4 | [GUI_NATIVE_WIDGETS.md](./GUI_NATIVE_WIDGETS.md) | Minecraft widgets to use | When adding controls |
| 🟢 5 | [../../03_PARAMETERS.md](../../03_PARAMETERS.md) | All field parameters | When adding sliders/inputs |

### Quick Reference Tables

| What You Need | Where to Find It |
|---------------|------------------|
| Screen/state classes | GUI_CLASS_DIAGRAM §2 |
| Panel classes | GUI_CLASS_DIAGRAM §3 |
| Sub-panel classes | GUI_CLASS_DIAGRAM §4-5 |
| Widget classes | GUI_CLASS_DIAGRAM §6 |
| Network packets | GUI_CLASS_DIAGRAM §7 |
| Profile system | GUI_CLASS_DIAGRAM §8 |
| Access levels | GUI_ARCHITECTURE §2 |
| DEBUG FIELD flow | GUI_ARCHITECTURE §3 |
| Update model | GUI_ARCHITECTURE §5 |
| **GuiWidgets factory** | GUI_UTILITIES §4 |
| **GuiConstants colors** | GUI_UTILITIES §3 |
| **Logging.GUI** | GUI_UTILITIES §7 |
| **Native widgets** | GUI_NATIVE_WIDGETS §2-9 |
| Shape parameters | 03_PARAMETERS §4 |
| All visual params | 03_PARAMETERS §5-11 |

### Scripts Reference

| # | Script | Purpose | Use For |
|---|--------|---------|---------|
| 01 | `query_tiny_mappings.py` | Find MC widget classes | Research native widgets |
| 02 | `update_todo_batch.py` | TODO automation | Mark tasks complete |

---

## 📜 System Context

### GUI System Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         GUI SYSTEM OVERVIEW                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  MINECRAFT NATIVE WIDGETS (Use Directly)                                   │
│  ───────────────────────────────────────                                   │
│  • CyclingButtonWidget - Enum dropdowns, toggles                           │
│  • ButtonWidget - Action buttons                                           │
│  • SliderWidget - Base for our LabeledSlider                               │
│  • TabButtonWidget + TabManager - Tab navigation                           │
│  • Tooltip - Hover help                                                    │
│                                                                             │
│  OUR UTILITIES (Wrap Native)                                               │
│  ───────────────────────────                                               │
│  • GuiWidgets - Factory methods, TRACE logging                             │
│  • GuiConstants - Colors, dimensions, theming                              │
│  • GuiLayout - Positioning helpers                                         │
│                                                                             │
│  OUR CUSTOM WIDGETS (Build New)                                            │
│  ──────────────────────────────                                            │
│  • LabeledSlider - Slider with label + value display                       │
│  • Vec3Editor - X/Y/Z inputs                                               │
│  • ColorButton - Color swatch + popup                                      │
│  • ExpandableSection - Collapsible panel                                   │
│                                                                             │
│  STATE MANAGEMENT                                                          │
│  ────────────────                                                          │
│  • GuiState - Original/working definition, dirty flag                      │
│  • EditorState - Layer/primitive selection                                 │
│  • UndoManager - Undo/redo stack                                           │
│  • Screen owns state, widgets use callbacks                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Why This Matters for Each TODO

When implementing a GUI todo:
1. **Check if MC provides it** → Use native widget via GuiWidgets factory
2. **Check GuiConstants** → Use consistent colors/dimensions
3. **Use callbacks** → Screen handles state, widgets just notify
4. **Add logging** → `Logging.GUI.trace()` for changes, `.alwaysChat()` for errors

---

## 📋 Document Logic Guide

### What Each Document Contains

| Document | Logic | Read When |
|----------|-------|-----------|
| **GUI_ARCHITECTURE** | The "design" - access levels, state flow, update model | Starting any feature |
| **GUI_CLASS_DIAGRAM** | The "what" - exact classes to create | Writing code |
| **GUI_UTILITIES** | The "how" - factory patterns, constants | Writing widgets |
| **GUI_NATIVE_WIDGETS** | The "reuse" - MC widgets available | Adding controls |
| **03_PARAMETERS** | The "fields" - parameters to expose | Adding sliders/inputs |

### Document Flow

```
START GUI TODO
    │
    ├─→ Read GUI_ARCHITECTURE (understand access level)
    │
    ├─→ Read GUI_CLASS_DIAGRAM (understand what to create)
    │
    ├─→ Check GUI_NATIVE_WIDGETS (can MC do this?)
    │
    ├─→ Use GUI_UTILITIES patterns (factory, constants)
    │
    ├─→ Check 03_PARAMETERS (what fields to expose?)
    │
    ├─→ Write code using GuiWidgets factory
    │
    └─→ Test in-game
         │
         END TODO
```

---

## 🔄 Timeline & Review Mechanism

### Before Starting a TODO

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  GUI QUICK REVIEW: Previous 5 Tasks                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  For each of the last 5 completed GUI TODOs, verify:                       │
│                                                                             │
│  □ Does it use GuiWidgets factory (not raw MC widgets)?                    │
│  □ Does it use GuiConstants colors/dimensions?                             │
│  □ Does it log via Logging.GUI?                                            │
│  □ Does it match GUI_CLASS_DIAGRAM structure?                              │
│  □ Are state changes via callbacks to screen?                              │
│                                                                             │
│  If ANY checkbox fails → Fix before continuing                             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Current Task Context

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  CURRENT GUI TODO CONTEXT                                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  TODO ID: G___                                                              │
│  Description: ________________________________________________              │
│                                                                             │
│  Access Level: □ Quick (L1)  □ Advanced (L2)  □ Debug (L3)                 │
│  Panel: _______________                                                     │
│  Widget Type: □ Native  □ Custom  □ Factory                                │
│                                                                             │
│  Relevant Docs:                                                             │
│  • GUI_ARCHITECTURE section: _______                                        │
│  • GUI_CLASS_DIAGRAM section: _______                                       │
│  • Parameters needed: _______                                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### After Completing a TODO

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  GUI COMPLETION CHECKLIST                                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  □ Code compiles with `./gradlew compileJava`                              │
│  □ Used GuiWidgets factory methods where possible                          │
│  □ Used GuiConstants for colors/dimensions                                 │
│  □ Added Logging.GUI.trace() for value changes                             │
│  □ Errors use Logging.GUI.alwaysChat()                                     │
│  □ State changes via callbacks, not direct mutation                        │
│  □ Tooltips added for user-facing controls                                 │
│                                                                             │
│  If discoveries made:                                                       │
│  □ Updated GUI_CLASS_DIAGRAM if structure changed                          │
│  □ Updated GUI_UTILITIES if new pattern found                              │
│  □ Created new TODO for follow-up work                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📊 Implementation Phases

### Phase 1: Foundation & Utilities (Batches 1-4)
| Category | Focus | Key Documents |
|----------|-------|---------------|
| Core classes | Screen, GuiState, UndoManager | GUI_CLASS_DIAGRAM §2 |
| Utilities | GuiWidgets, GuiConstants, GuiLayout | GUI_UTILITIES §3-5 |
| Custom widgets | LabeledSlider, Vec3Editor, ColorButton | GUI_CLASS_DIAGRAM §6 |
| DEBUG FIELD | Spawn, despawn, notification | GUI_ARCHITECTURE §3 |

### Phase 2: Quick Panel - Level 1 (Batches 5-6)
| Category | Focus | Key Documents |
|----------|-------|---------------|
| Shape controls | Type, radius | GUI_CLASS_DIAGRAM §3.1 |
| Appearance | Color, alpha, fill | 03_PARAMETERS §6,9 |
| Behavior | Follow mode, prediction | 03_PARAMETERS §1 |
| Layers | Navigation, add/remove | GUI_ARCHITECTURE §2.1 |

### Phase 3: Advanced Panel - Level 2 (Batches 7-10)
| Category | Focus | Key Documents |
|----------|-------|---------------|
| Shape details | Per-shape parameters | 03_PARAMETERS §4 |
| Appearance | Glow, emissive, etc. | 03_PARAMETERS §9 |
| Animation | Spin, pulse, phase | 03_PARAMETERS §10 |
| Transform | Anchor, offset, rotation | 03_PARAMETERS §5 |
| Visibility | Masks, patterns | 03_PARAMETERS §7-8 |
| Linking | Primitive linking | 03_PARAMETERS §11 |

### Phase 4: Debug & Profiles (Batches 11-12)
| Category | Focus | Key Documents |
|----------|-------|---------------|
| Debug access | Permission check | GUI_ARCHITECTURE §2.1 L3 |
| Bindings | Property ← source | 03_PARAMETERS §12.1 |
| Triggers | Event → effect | 03_PARAMETERS §12.2 |
| Lifecycle | Fade, scale, decay | 03_PARAMETERS §12.3 |
| Profiles | Save/load/manage | GUI_CLASS_DIAGRAM §8 |

### Phase 5: Network & Polish (Batches 13-15)
| Category | Focus | Key Documents |
|----------|-------|---------------|
| Packets | C2S/S2C communication | GUI_CLASS_DIAGRAM §7 |
| Server profiles | Request/receive | GUI_ARCHITECTURE §4 |
| Scrolling | Advanced panel scroll | GUI_CLASS_DIAGRAM §3.2 |
| Polish | Undo, keyboard, a11y | GUI_ARCHITECTURE §9 |

---

## 🏗️ Design Patterns

### Widget Creation Pattern

```java
// ✅ CORRECT: Use factory
addDrawableChild(GuiWidgets.enumDropdown(
    layout.x(), layout.y(), layout.width(),
    "Fill Mode", FillMode.class, state.getFillMode(),
    "How the shape is rendered",
    val -> {
        state.setFillMode(val);
        state.markDirty();
    }
));

// ❌ WRONG: Raw Minecraft widget
addDrawableChild(CyclingButtonWidget.<FillMode>builder(...)
    .values(...)
    .build(...));  // No logging, no consistent styling
```

### State Change Pattern

```java
// ✅ CORRECT: Callback to screen, screen updates state
Consumer<Float> onChange = val -> {
    state.setRadius(val);  // Update state
    state.markDirty();     // Mark dirty
    applyToDebugField();   // Live preview
};

// ❌ WRONG: Widget directly modifies state
// (No undo support, no dirty tracking)
```

### Error Handling Pattern

```java
// ✅ CORRECT: Errors visible to player
try {
    state.setRadius(value);
} catch (Exception e) {
    Logging.GUI.topic("error")
        .alwaysChat()  // Player sees this!
        .exception(e)
        .error("Invalid radius: {}", e.getMessage());
}
```

---

## ⚠️ Common Mistakes to Avoid

| Mistake | Why It's Wrong | Correct Approach |
|---------|----------------|------------------|
| Raw MC widgets | No logging, inconsistent styling | Use GuiWidgets factory |
| Hard-coded colors | Can't theme globally | Use GuiConstants |
| Widget modifies state | No undo, no dirty tracking | Use callbacks to screen |
| Silent errors | Player confused why it didn't work | Use `.alwaysChat()` |
| Custom widget for native | Reinventing the wheel | Check GUI_NATIVE_WIDGETS first |
| Skipping tooltips | Bad UX | Always add tooltip param |

---

## 🔗 Quick Links

### GUI Documents
- [GUI_ARCHITECTURE.md](./GUI_ARCHITECTURE.md) - Design & flow
- [GUI_CLASS_DIAGRAM.md](./GUI_CLASS_DIAGRAM.md) - Classes to create
- [GUI_UTILITIES.md](./GUI_UTILITIES.md) - Factory & constants
- [GUI_NATIVE_WIDGETS.md](./GUI_NATIVE_WIDGETS.md) - MC widgets
- [GUI_DESIGN.md](./GUI_DESIGN.md) - Visual mockups
- [GUI_TODO_LIST.md](./GUI_TODO_LIST.md) - Task list

### Parent Documents (Inherited)
- [../../00_TODO_DIRECTIVES.md](../../00_TODO_DIRECTIVES.md) - Process rules
- [../../03_PARAMETERS.md](../../03_PARAMETERS.md) - Field parameters
- [../_design/CODE_QUALITY.md](../_design/CODE_QUALITY.md) - Logging standards
- [../_design/DESIGN_PATTERNS.md](../_design/DESIGN_PATTERNS.md) - OOP patterns

---

## 📝 Template: GUI TODO Work Session

```markdown
## Session Start
- Date: YYYY-MM-DD
- TODOs planned: [G01, G02, G03...]

## Quick Review (Last 5)
- [x] G00: Uses GuiWidgets factory
- [x] G-1: Uses GuiConstants
- ...

## Current TODO: G___
- Access Level: Quick/Advanced/Debug
- Panel: _______________
- Architecture section: X
- Class diagram section: Y

## Implementation Notes
- Used: GuiWidgets.slider()
- Constants: GuiConstants.SLIDER_WIDTH
- ...

## Discoveries
- ...

## Session End
- Completed: [G01, G02]
- New TODOs: [Gnew1]
```

---

*This document is the "home base" during GUI implementation. Return here between each TODO.*

