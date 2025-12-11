# GUI Implementation TODO List

> **Purpose:** Master tracking of Field Customizer GUI implementation tasks  
> **Status:** ✅ Near Complete - Batches 1-15 done, ~8 items remaining  
> **Updated:** December 10, 2024 (Command system refactored)  
> **Parent:** [../../TODO_LIST.md](../../TODO_LIST.md)  
> **⚠️ Use with [GUI_TODO_DIRECTIVES.md](./GUI_TODO_DIRECTIVES.md) for EVERY task**

---

### New TODO: Preset System (cross-panels)
| ID | Task | Status | Notes |
|----|------|--------|-------|
| G-PRESET-01 | ~~Implement preset registry~~ → Now split: FragmentRegistry (single-scope) + PresetRegistry (multi-scope) | ✅ | Refactored Dec 9: Preset→Fragment rename |
| G-PRESET-02 | ~~Wire preset dropdowns into panels~~ → Fragment dropdowns in sub-panels + Preset dropdown in BottomActionBar | ✅ | Multi-scope presets load from field_presets/ |
| G-PRESET-03 | Create example presets: Ethereal Glow, Tech Grid, Shield Ring, Danger Pulse, Stealth Fade | ✅ | config/the-virus-block/field_presets/ |

---

### New TODO: Command/GUI Unification (Architecture Refactor)
> **Ref:** [GUI_ARCHITECTURE.md §11](./GUI_ARCHITECTURE.md#11-commandgui-unification)

| ID | Task | Status | Priority | Notes |
|----|------|--------|----------|-------|
| G-CMD-01 | Create `FieldEditUpdateS2CPayload` for command→client sync | ✅ | High | Uses JSON for flexibility |
| G-CMD-02 | Refactor `/field` commands to send S2C packets (not static state) | ✅ | High | 16+ usages in FieldEditSubcommand |
| G-CMD-03 | Split `/fieldtest` - move FieldEditState-linked commands to `/field` | ✅ | High | `/field edit` vs `/fieldtest` |
| G-CMD-04 | Keep `/fieldtest` for debug-only: shuffle, vertex, cycle, spawn-from-registry | ✅ | Medium | Separate state from GUI |
| G-CMD-05 | Create `TestFieldRenderer` (client-side preview field) | ✅ | High | Reads FieldEditStateHolder |
| G-CMD-06 | Add test field spawn/despawn button to Debug tab | ✅ | Medium | LifecycleSubPanel buttons |
| G-CMD-07 | Implement debounce in FieldEditState for test field updates | ✅ | Medium | 16ms in TestFieldRenderer |
| G-CMD-08 | Add `/field test spawn/despawn/toggle` commands | ✅ | Medium | In FieldCommand |
| G-CMD-09 | Add `/field status` command (show current FieldEditState summary) | ✅ | Low | Implemented Dec 10 | Low | Debug aid |
| G-CMD-10 | Add `/field reset` command (reset FieldEditState to defaults) | ✅ | Low | Implemented Dec 10 |

---

### New TODO: /field Command Coverage
> Complete list of `/field` commands needed

| ID | Task | Status | Priority | Notes |
|----|------|--------|----------|-------|
| G-FCMD-01 | `/field edit shape <type>` + latSteps, lonSteps | ✅ | High | With $ref support |
| G-FCMD-02 | `/field edit` transform (anchor, scale, offset, rotation) | ✅ | High | With $ref support |
| G-FCMD-03 | `/field orbit` params (enabled, radius, speed, axis, phase) | ✅ | Medium | GUI controls in OrbitSubPanel |
| G-FCMD-04 | `/field edit fill <mode>` | ✅ | High | With $ref support |
| G-FCMD-05 | `/field edit` visibility (mask, count) | ✅ | Medium | With $ref support |
| G-FCMD-06 | `/field edit` appearance (color, alpha, glow, emissive) | ✅ | High | With $ref support |
| G-FCMD-07 | `/field edit spin` + animation $ref | ✅ | Medium | spin off, animation $ref |
| G-FCMD-08 | `/field modifier` params (bobbing, breathing, colorCycle, wobble, wave) | ✅ | Low | Via CommandScanner Dec 10 |
| G-FCMD-09 | `/field layer` management (select, add, remove, blend, alpha) | ✅ | Medium | Implemented Dec 10 |
| G-FCMD-10 | `/field primitive` management (select, add, remove) | ✅ | Medium | Implemented Dec 10 |
| G-FCMD-11 | `/field binding` management (add, remove, clear) | ✅ | Low | Implemented Dec 10 |
| G-FCMD-12 | `/field beam` params (enabled, radius, height, etc.) | ⬜ | Low | |
| G-FCMD-13 | `/field edit follow` and `/field edit predict` | ✅ | Medium | on/off supported |
| G-FCMD-14 | `/field fragment <category> <name>` | ✅ | Low | Implemented Dec 10 | Low | Apply single-scope |
| G-FCMD-15 | `/field preset apply <name>` | ✅ | Low | Implemented Dec 10 | Low | Apply multi-scope |
| G-FCMD-16 | `/field profile load/save/list` | ✅ | Medium | Implemented Dec 10 | Medium | Profile management |

---

### New TODO: Remaining Shape Parameters (from SHAPE_MATRIX.md §2-6)
> **Ref:** [../../04_SHAPE_MATRIX.md](../../04_SHAPE_MATRIX.md) §2-6
> **Updated Dec 10:** Most shape params already implemented in ShapeSubPanel!

| ID | Task | Status | Priority | Notes |
|----|------|--------|----------|-------|
| G-SHAPE-01 | Ring: `arcStart`, `arcEnd` sliders (0-360°) | ✅ | Medium | In ShapeSubPanel |
| G-SHAPE-02 | Ring: `height` slider for 3D tube mode | ✅ | Medium | In ShapeSubPanel |
| G-SHAPE-03 | Ring: `twist` slider (-360 to 360°) | ✅ | Low | In ShapeSubPanel |
| G-SHAPE-04 | Disc: `arcStart`, `arcEnd` sliders (Pac-Man) | ✅ | Medium | In ShapeSubPanel |
| G-SHAPE-05 | Disc: `innerRadius` slider (annulus) | ✅ | Medium | In ShapeSubPanel |
| G-SHAPE-06 | Disc: `rings` slider (concentric divisions) | ✅ | Low | In ShapeSubPanel |
| G-SHAPE-07 | Prism: `topRadius` slider (taper/pyramid) | ✅ | Medium | In ShapeSubPanel |
| G-SHAPE-08 | Prism: `twist` slider | ✅ | Low | Already in ShapeSubPanel |
| G-SHAPE-09 | Prism: `capTop`, `capBottom` toggles | ✅ | Low | Already in ShapeSubPanel |
| G-SHAPE-10 | Cylinder: `arc` slider (partial cylinder) | ✅ | Medium | Already in ShapeSubPanel |
| G-SHAPE-11 | Cylinder: `topRadius` slider (cone-like) | ✅ | Medium | In ShapeSubPanel |
| G-SHAPE-12 | Cylinder: `capTop`, `capBottom`, `openEnded` toggles | ✅ | Low | openEnded in ShapeSubPanel |
| G-SHAPE-13 | Polyhedron: `subdivisions` slider (0-5) | ✅ | Low | In ShapeSubPanel |

---

### New TODO: Transform Orbit System (from SHAPE_MATRIX.md §10)
> **Ref:** [../../04_SHAPE_MATRIX.md](../../04_SHAPE_MATRIX.md) §10
> **Updated Dec 10:** All orbit params implemented in OrbitSubPanel!

| ID | Task | Status | Priority | Notes |
|----|------|--------|----------|-------|
| G-ORBIT-01 | `orbit.enabled` toggle | ✅ | Low | In OrbitSubPanel |
| G-ORBIT-02 | `orbit.radius` slider | ✅ | Low | In OrbitSubPanel |
| G-ORBIT-03 | `orbit.speed` slider | ✅ | Low | In OrbitSubPanel |
| G-ORBIT-04 | `orbit.axis` dropdown (X, Y, Z) | ✅ | Low | In OrbitSubPanel |
| G-ORBIT-05 | `orbit.phase` slider (0-1) | ✅ | Low | In OrbitSubPanel |

---

### New TODO: Layer Advanced Options (from SHAPE_MATRIX.md §15)
> **Ref:** [../../04_SHAPE_MATRIX.md](../../04_SHAPE_MATRIX.md) §15

| ID | Task | Status | Priority | Notes |
|----|------|--------|----------|-------|
| G-LAYER-01 | `blendMode` dropdown (NORMAL, ADD, MULTIPLY, SCREEN) | ✅ | Low | Implemented Dec 10 - GUI + Renderer |
| G-LAYER-02 | `order` slider (render order) | ⏭️ | Low | Skip - not used by renderer |

---

### New TODO: Field Modifiers (from SHAPE_MATRIX.md §16)
> **Ref:** [../../04_SHAPE_MATRIX.md](../../04_SHAPE_MATRIX.md) §16

| ID | Task | Status | Priority | Notes |
|----|------|--------|----------|-------|
| G-MOD-01 | `modifiers.bobbing` slider | ✅ | Low | In ModifiersSubPanel + commands |
| G-MOD-02 | `modifiers.breathing` slider | ✅ | Low | In ModifiersSubPanel + commands |

---

## How to Use

### Option A: One Task at a Time
```
G01 → G01-CHK → G02 → G02-CHK → ...
```

### Option B: Batch with Python Script (Preferred)
```
[Python script does G01-G10] → ONE combined CHK
```
- Write Python script that implements batch
- Run script
- Mark all tasks AND CHKs as ✅ together
- **ONE return to TODO_DIRECTIVES** after script execution

### Status Markers
- ⬜ Pending
- 🔄 In Progress  
- ✅ Done

---

## Quick Stats

| Status | Count |
|--------|-------|
| ✅ Done | 175+ |
| 🔄 In Progress | 0 |
| ⬜ Pending | 24 (mostly low-priority commands, client config) |

> **Batches 1-15:** ✅ Complete  
> **G-CMD-*:** ✅ 9/10 Complete (status pending)  
> **G-FCMD-*:** ✅ 15/16 Complete (binding, beam, fragment/preset/profile pending)  
> **G-SHAPE-*:** ✅ 13/13 Complete  
> **G-ORBIT-*:** ✅ 5/5 Complete  
> **G-LAYER-*:** ✅ 1/2 Complete (blendMode done, order skipped)  
> **G-MOD-*:** ✅ 2/2 Complete (GUI + commands)  
> **Custom Widgets (G21-G40):** ✅ 18/20 Complete (client config G39-G40 pending)  
> **Preset System:** ✅ Complete  
> **FragmentRegistry:** ✅ Updated Dec 10 - Added 7 new folders, fixed field_follow→field_follows  
> **Command Infrastructure:** ✅ Dec 10 - CommandScanner, FieldEditKnob, ValueRange.unit(), path-based set/get

---

## Phase 1: Foundation & Utilities

---

### Batch 1: Core Classes (G01-G10)

> **Ref:** GUI_CLASS_DIAGRAM §2  
> **Package:** `net.cyberpunk042.client.gui`

| ID | Task | Status | Package |
|----|------|--------|---------|
| G01 | `FieldCustomizerScreen extends Screen` - basic structure, close on ESC | ✅ | screen |
| G01-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G02 | `FieldEditState` class - originalDefinition, workingDefinition, isDirty | ✅ | state |
| G02-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G03 | `EditorState` class - selectedLayerIndex, selectedPrimitiveIndex | ✅ | state |
| G03-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G04 | `UndoManager` class - push, undo, redo, maxSize=50 | ✅ | state |
| G04-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G05 | `TabType` enum - QUICK, ADVANCED, DEBUG, PROFILES | ✅ | screen |
| G05-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G06 | `AbstractPanel` base class - init(), render(), tick() | ✅ | panel |
| G06-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G07 | Tab navigation using native `TabManager` + `TabButtonWidget` | ✅ | screen |
| G07-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G08 | Dark background rendering + panel layout | ✅ | screen |
| G08-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G09 | `/field customize` command registration | ✅ | - |
| G09-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G10 | `/field customize <profile>` variant | ✅ | - |
| G10-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| CHK-01 | ⚠️ **BATCH 1 COMPLETE** - [Directives Checklist](./GUI_TODO_DIRECTIVES.md#after-completing-a-todo) | ✅ | - |

---

### Batch 2: Utilities & Constants (G11-G20)

> **Ref:** GUI_UTILITIES.md  
> **Package:** `net.cyberpunk042.client.gui.util`

| ID | Task | Status | Package |
|----|------|--------|---------|
| G11 | Add `Logging.GUI` channel to Logging.java | ✅ | util |
| G11-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G12 | `GuiConstants` - WIDGET_HEIGHT, BUTTON_WIDTH, PADDING | ✅ | util |
| G12-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G13 | `GuiConstants` - BG_SCREEN, BG_PANEL, BG_WIDGET colors | ✅ | util |
| G13-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G14 | `GuiConstants` - TEXT_PRIMARY, ACCENT, ERROR colors | ✅ | util |
| G14-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G15 | `GuiLayout` class - positioning helpers, nextRow(), nextSection() | ✅ | util |
| G15-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G16 | `GuiWidgets.enumDropdown()` factory method | ✅ | util |
| G16-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G17 | `GuiWidgets.toggle()` factory method | ✅ | util |
| G17-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G18 | `GuiWidgets.button()` factory method | ✅ | util |
| G18-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G19 | `GuiWidgets.slider()` factory method | ✅ | util |
| G19-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G20 | `GuiWidgets.sliderInt()` factory method | ✅ | util |
| G20-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| CHK-02 | ⚠️ **BATCH 2 COMPLETE** - [Directives Checklist](./GUI_TODO_DIRECTIVES.md#after-completing-a-todo) | ✅ | - |

---

### Batch 3: Custom Widgets Part 1 (G21-G30)

> **Ref:** GUI_CLASS_DIAGRAM §6  
> **Package:** `net.cyberpunk042.client.gui.widget`

| ID | Task | Status | Package |
|----|------|--------|---------|
| G21 | `LabeledSlider extends SliderWidget` - basic structure | ✅ | widget |
| G21-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G22 | `LabeledSlider` - min/max range mapping | ✅ | widget |
| G22-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G23 | `LabeledSlider` - format string (%.2f, %d) | ✅ | widget |
| G23-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G24 | `LabeledSlider` - optional step/snap support | ✅ | widget |
| G24-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G25 | `Vec3Editor` - 3x TextFieldWidget composite | ✅ | widget |
| G25-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G26 | `Vec3Editor` - linked value update, parse/validate | ✅ | widget |
| G26-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G27 | `ColorButton` - color swatch display | ✅ | widget |
| G27-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G28 | `ColorButton` - hex input popup | ✅ | widget |
| G28-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G29 | `ColorButton` - theme color buttons (@primary, @secondary) | ✅ | widget |
| G29-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G30 | `ExpandableSection` - header with ▸/▾, content toggle | ✅ | widget |
| G30-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| CHK-03 | ⚠️ **BATCH 3 COMPLETE** - [Directives Checklist](./GUI_TODO_DIRECTIVES.md#after-completing-a-todo) | ✅ | - |

---

### Batch 4: Custom Widgets Part 2 & DEBUG Field (G31-G40)

> **Ref:** GUI_CLASS_DIAGRAM §6, GUI_ARCHITECTURE §3  
> **Package:** `net.cyberpunk042.client.gui.widget`, field

| ID | Task | Status | Package |
|----|------|--------|---------|
| G31 | `ExpandableSection` - state persistence to FieldEditState | ✅ | widget |
| G31-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G32 | `ConfirmDialog` utility - reusable yes/no dialog | ✅ | widget |
| G32-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G33 | `ToastNotification` - success/error/warning toasts | ✅ | widget |
| G33-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G34 | `LoadingIndicator` - spinner for async operations | ✅ | widget |
| G34-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G35 | DEBUG FIELD spawn - `TestFieldRenderer`, client-side | ✅ | field |
| G35-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G36 | DEBUG FIELD notification - toast on spawn | ✅ | field |
| G36-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G37 | DEBUG FIELD despawn on screen close | ✅ | field |
| G37-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G38 | Unsaved changes prompt on close | ✅ | screen |
| G38-CHK | ↳ State persists per architecture - no dialog needed | ✅ | - |
| G39 | Client config - maxUndoSteps, showTooltips | ⬜ | config |
| G39-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ⬜ | - |
| G40 | Client config - rememberTabState, debugMenuEnabled | ⬜ | config |
| G40-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-04 | ⚠️ **BATCH 4 PARTIAL** - Client config pending | ⬜ | - |

---

## Phase 2: Quick Panel (Level 1)

---

### Batch 5: Quick Panel - Shape & Appearance (G41-G50)

> **Ref:** GUI_ARCHITECTURE §2.1, 03_PARAMETERS §1-4  
> **Package:** `net.cyberpunk042.client.gui.panel`
> **Updated Dec 10:** All implemented in QuickPanel.java

| ID | Task | Status | Package |
|----|------|--------|---------|
| G41 | `QuickPanel extends AbstractPanel` - layout structure | ✅ | panel |
| G41-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G42 | Shape type dropdown - SPHERE, RING, DISC, PRISM, CYLINDER, POLYHEDRON | ✅ | panel |
| G42-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G43 | Radius slider - 0.1 to 10.0 | ✅ | panel |
| G43-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G44 | Color button - with theme picker popup | ✅ | panel |
| G44-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G45 | Alpha slider - 0.0 to 1.0 | ✅ | panel |
| G45-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G46 | Fill mode dropdown - SOLID, WIREFRAME, CAGE | ✅ | panel |
| G46-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G47 | Spin speed slider - -0.5 to 0.5 | ✅ | panel |
| G47-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G48 | Follow mode dropdown - SNAP, SMOOTH, GLIDE | ✅ | panel |
| G48-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G49 | Prediction toggle - enable/disable | ✅ | panel |
| G49-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G50 | Prediction preset dropdown - OFF, LOW, MEDIUM, HIGH, CUSTOM | ✅ | panel |
| G50-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| CHK-05 | ⚠️ **BATCH 5 COMPLETE** - [Directives Checklist](./GUI_TODO_DIRECTIVES.md#after-completing-a-todo) | ✅ | - |

---

### Batch 6: Quick Panel - Layers & Actions (G51-G60)

> **Ref:** GUI_ARCHITECTURE §2.1  
> **Package:** `net.cyberpunk042.client.gui.panel`

| ID | Task | Status | Package |
|----|------|--------|---------|
| G51 | `LayerPanel` - layer list display | ✅ | panel |
| G51-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G52 | Layer selector - prev/next buttons | ✅ | panel |
| G52-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G53 | Layer add button [+] - creates new layer | ✅ | panel |
| G53-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G54 | Layer remove button [-] - with confirm dialog | ✅ | panel |
| G54-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G55 | Layer visibility toggle [👁] | ✅ | panel |
| G55-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G56 | Layer reorder [▲][▼] buttons | ✅ | panel |
| G56-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G57 | Connect Quick Panel widgets to FieldEditState | ✅ | panel |
| G57-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G58 | Live apply changes to DEBUG FIELD | ✅ | panel |
| G58-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G59 | "Apply to My Shield" button | ✅ | panel |
| G59-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G60 | Auto-save checkbox toggle | ✅ | panel |
| G60-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| CHK-06 | ⚠️ **BATCH 6 COMPLETE** - [Directives Checklist](./GUI_TODO_DIRECTIVES.md#after-completing-a-todo) | ✅ | - |

---

## Phase 3: Advanced Panel (Level 2)

---

### Batch 7: Advanced Panel - Shape Details (G61-G70)

> **Ref:** GUI_CLASS_DIAGRAM §4.1, 03_PARAMETERS §4  
> **Package:** `net.cyberpunk042.client.gui.panel.sub`

| ID | Task | Status | Package |
|----|------|--------|---------|
| G61 | `AdvancedPanel extends AbstractPanel` - scrollable layout | ✅ | panel |
| G61-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G62 | `ShapeSubPanel` - dynamic controls based on shape type | ✅ | panel.sub |
| G62-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G63 | Sphere controls - latSteps, lonSteps, latStart, latEnd | ✅ | panel.sub |
| G63-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G64 | Sphere controls - algorithm dropdown (LAT_LON, TYPE_A, TYPE_E) | ✅ | panel.sub |
| G64-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G65 | Ring controls - innerRadius, outerRadius, segments | ✅ | panel.sub |
| G65-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G66 | Ring controls - height (3D ring!), y position | ✅ | panel.sub |
| G66-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G67 | Disc controls - radius, segments, y, innerRadius | ✅ | panel.sub |
| G67-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G68 | Prism controls - sides, radius, height, topRadius | ✅ | panel.sub |
| G68-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G69 | Cylinder controls - radius, height, segments, topRadius | ✅ | panel.sub |
| G69-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G70 | Polyhedron controls - polyType dropdown, radius, subdivisions | ✅ | panel.sub |
| G70-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| CHK-07 | ⚠️ **BATCH 7 COMPLETE** - [Directives Checklist](./GUI_TODO_DIRECTIVES.md#after-completing-a-todo) | ✅ | - |

---

### Batch 8: Advanced Panel - Appearance & Animation (G71-G80)

> **Ref:** GUI_CLASS_DIAGRAM §4.2-4.3, 03_PARAMETERS §9-10  
> **Package:** `net.cyberpunk042.client.gui.panel.sub`

| ID | Task | Status | Package |
|----|------|--------|---------|
| G71 | `AppearanceSubPanel` - structure | ✅ | panel.sub |
| G71-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G72 | Appearance - glow, emissive sliders | ✅ | panel.sub |
| G72-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G73 | Appearance - saturation, brightness, hueShift | ✅ | panel.sub |
| G73-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G74 | `AnimationSubPanel` - structure | ✅ | panel.sub |
| G74-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G75 | Spin config - axis dropdown, speed slider, oscillate toggle | ✅ | panel.sub |
| G75-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G76 | Pulse config - scale, speed, waveform dropdown | ✅ | panel.sub |
| G76-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G77 | Alpha pulse config - speed, min, max, waveform | ✅ | panel.sub |
| G77-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G78 | Phase offset slider | ✅ | panel.sub |
| G78-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G79 | `PrimitivePanel` - select primitive within layer | ✅ | panel |
| G79-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G80 | Primitive selector - prev/next/add/remove | ✅ | panel |
| G80-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| CHK-08 | ⚠️ **BATCH 8 COMPLETE** - [Directives Checklist](./GUI_TODO_DIRECTIVES.md#after-completing-a-todo) | ✅ | - |

---

### Batch 9: Advanced Panel - Transform & Visibility (G81-G90)

> **Ref:** GUI_CLASS_DIAGRAM §4.4-4.5, 03_PARAMETERS §5,7  
> **Package:** `net.cyberpunk042.client.gui.panel.sub`

| ID | Task | Status | Package |
|----|------|--------|---------|
| G81 | `TransformSubPanel` - structure | ✅ | panel.sub |
| G81-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G82 | Anchor dropdown - 9 positions | ✅ | panel.sub |
| G82-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G83 | Offset Vec3Editor | ✅ | panel.sub |
| G83-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G84 | Rotation Vec3Editor | ✅ | panel.sub |
| G84-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G85 | Scale slider + non-uniform toggle | ✅ | panel.sub |
| G85-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G86 | `VisibilitySubPanel` - structure | ✅ | panel.sub |
| G86-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G87 | Mask type dropdown - FULL, BANDS, STRIPES, CHECKER, RADIAL, GRADIENT | ✅ | panel.sub |
| G87-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G88 | Visibility - count, thickness, offset sliders | ✅ | panel.sub |
| G88-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G89 | Visibility - invert toggle, feather slider | ✅ | panel.sub |
| G89-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G90 | Visibility - dynamic GRADIENT/RADIAL fields | ✅ | panel.sub |
| G90-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| CHK-09 | ⚠️ **BATCH 9 COMPLETE** - [Directives Checklist](./GUI_TODO_DIRECTIVES.md#after-completing-a-todo) | ✅ | - |

---

### Batch 10: Advanced Panel - Arrangement, Fill, Linking (G91-G100)

> **Ref:** GUI_CLASS_DIAGRAM §4.6-4.8, 03_PARAMETERS §6,8,11  
> **Package:** `net.cyberpunk042.client.gui.panel.sub`

| ID | Task | Status | Package |
|----|------|--------|---------|
| G91 | `ArrangementSubPanel` - structure | ✅ | panel.sub |
| G91-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G92 | Pattern dropdown - filtered by current CellType | ✅ | panel.sub |
| G92-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G93 | Multi-part arrangement - caps, sides, edges dropdowns | ✅ | panel.sub |
| G93-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G94 | `FillSubPanel` - extended fill options | ✅ | panel.sub |
| G94-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G95 | Fill - wireThickness slider, doubleSided toggle | ✅ | panel.sub |
| G95-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G96 | Fill - cage-specific: latitudeCount, longitudeCount | ✅ | panel.sub |
| G96-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G97 | `LinkingSubPanel` - primitive linking | ✅ | panel.sub |
| G97-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G98 | Linking - primitive ID input | ✅ | panel.sub |
| G98-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G99 | Linking - radiusMatch, follow, mirror dropdowns | ✅ | panel.sub |
| G99-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G100 | Linking - phaseOffset, scaleWith | ✅ | panel.sub |
| G100-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| CHK-10 | ⚠️ **BATCH 10 COMPLETE** - [Directives Checklist](./GUI_TODO_DIRECTIVES.md#after-completing-a-todo) | ✅ | - |

---

## Phase 4: Debug Panel & Profiles (Level 3)

---

### Batch 11: Debug Panel (G101-G110)

> **Ref:** GUI_ARCHITECTURE §2.1 Level 3, 03_PARAMETERS §12  
> **Package:** `net.cyberpunk042.client.gui.panel`

| ID | Task | Status | Package |
|----|------|--------|---------|
| G101 | `DebugPanel extends AbstractPanel` - permission check | ✅ | panel |
| G101-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G102 | "Debug menu requires operator" locked message | ✅ | panel |
| G102-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G103 | `BindingsSubPanel` - list existing bindings | ✅ | panel.sub |
| G103-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G104 | Binding editor - property path, source, input/output range | ✅ | panel.sub |
| G104-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G105 | `TriggersSubPanel` - list existing triggers | ✅ | panel.sub |
| G105-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G106 | Trigger editor - event, effect, duration, params | ✅ | panel.sub |
| G106-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G107 | `LifecycleSubPanel` - fadeIn/Out, scaleIn/Out sliders | ✅ | panel.sub |
| G107-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G108 | Lifecycle - decay config (enable, rate, min) | ✅ | panel.sub |
| G108-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G109 | `BeamSubPanel` - enable toggle, beam parameters | ✅ | panel.sub |
| G109-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G110 | `PerformancePanel` - render time, vertex count | ✅ | panel.sub |
| G110-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| CHK-11 | ⚠️ **BATCH 11 COMPLETE** - [Directives Checklist](./GUI_TODO_DIRECTIVES.md#after-completing-a-todo) | ✅ | - |

---

### Batch 12: Debug Panel - JSON & Profiles (G111-G121)

> **Ref:** GUI_ARCHITECTURE §4, GUI_CLASS_DIAGRAM §8  
> **Package:** `net.cyberpunk042.client.gui.panel`, profile

| ID | Task | Status | Package |
|----|------|--------|---------|
| G111 | JSON viewer panel - read-only display | ✅ | panel.sub |
| G111-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G112 | JSON viewer - copy button | ✅ | panel.sub |
| G112-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G113 | Export JSON to file | ✅ | panel.sub |
| G113-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G114 | Import JSON from file | ✅ | panel.sub |
| G114-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G115 | `ProfileManager` class - scan local profiles | ✅ | profile |
| G115-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G116 | `Profile` record - version, name, description, definition | ✅ | profile |
| G116-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G117 | `ProfileValidator` - validate JSON structure | ✅ | profile |
| G117-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G118 | `ProfilePanel` - dropdown + action buttons | ✅ | panel |
| G118-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G119 | Profile Save button - with backup | ✅ | panel |
| G119-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G120 | Profile Save As dialog - name + description | ✅ | panel |
| G120-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G121 | Profile Load/Delete/Rename buttons | ✅ | panel |
| G121-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| CHK-12 | ⚠️ **BATCH 12 COMPLETE** - [Directives Checklist](./GUI_TODO_DIRECTIVES.md#after-completing-a-todo) | ✅ | - |

---

## Phase 5: Network & Polish

---

### Batch 13: Network Packets (G122-G131)

> **Ref:** GUI_ARCHITECTURE §7  
> **Package:** `net.cyberpunk042.client.gui.network`, `net.cyberpunk042.network`

| ID | Task | Status | Package |
|----|------|--------|---------|
| G122 | `FieldGuiOpenC2S` packet - request GUI open | ✅ | network |
| G122-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G123 | `FieldGuiDataS2C` packet - definition + defaults list | ✅ | network |
| G123-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G124 | `FieldUpdateC2S` packet - send definition changes | ✅ | network |
| G124-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G125 | `FieldProfileListS2C` packet - server profile names | ✅ | network |
| G125-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G126 | `FieldProfileRequestC2S` packet - request server profile | ✅ | network |
| G126-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G127 | `FieldProfileDataS2C` packet - profile JSON response | ✅ | network |
| G127-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G128 | Server-side packet handlers | ✅ | network |
| G128-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G129 | Client-side packet handlers | ✅ | network |
| G129-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G130 | Rate limiting for live updates (100ms) | ✅ | network |
| G130-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G131 | Handle disconnect gracefully | ✅ | network |
| G131-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| CHK-13 | ⚠️ **BATCH 13 COMPLETE** - [Directives Checklist](./GUI_TODO_DIRECTIVES.md#after-completing-a-todo) | ✅ | - |

---

### Batch 14: Server Profiles & Scrolling (G132-G141)

> **Ref:** GUI_ARCHITECTURE §4  
> **Package:** `net.cyberpunk042.client.gui.panel`

| ID | Task | Status | Package |
|----|------|--------|---------|
| G132 | Request server default list on GUI open | ✅ | panel |
| G132-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G133 | Display server defaults in profile dropdown | ✅ | panel |
| G133-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G134 | Load server default on selection | ✅ | panel |
| G134-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G135 | Scroll container for Advanced Panel | ✅ | panel |
| G135-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G136 | Mouse wheel scrolling | ✅ | panel |
| G136-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G137 | Scroll bar widget (optional) | ✅ | widget |
| G137-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G138 | Recalculate scroll height on section expand | ✅ | panel |
| G138-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G139 | Auto-scroll to expanded section | ✅ | panel |
| G139-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G140 | Prediction full controls in Advanced | ✅ | panel.sub |
| G140-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G141 | FollowMode full controls in Advanced | ✅ | panel.sub |
| G141-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| CHK-14 | ⚠️ **BATCH 14 COMPLETE** - [Directives Checklist](./GUI_TODO_DIRECTIVES.md#after-completing-a-todo) | ✅ | - |

---

### Batch 15: Polish & Final (G142-G151)

> **Ref:** GUI_ARCHITECTURE §9  
> **Package:** `net.cyberpunk042.client.gui`

| ID | Task | Status | Package |
|----|------|--------|---------|
| G142 | Undo hotkey (Ctrl+Z) | ✅ | screen |
| G142-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G143 | Redo hotkey (Ctrl+Y / Ctrl+Shift+Z) | ✅ | screen |
| G143-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G144 | Toolbar with Undo/Redo/Save/Reset buttons | ✅ | screen |
| G144-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G145 | Keyboard navigation - Tab between widgets | ✅ | screen |
| G145-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G146 | Narration support for accessibility | ✅ | screen |
| G146-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G147 | Section headers/dividers styling | ✅ | panel |
| G147-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G148 | Consistent spacing audit | ✅ | - |
| G148-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G149 | Test all shape types render | ✅ | - |
| G149-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G150 | Test edge cases (empty, invalid, disconnect) | ✅ | - |
| G150-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| G151 | GUI-FINAL: Full integration test | ✅ | - |
| G151-CHK | ↳ [GUI_TODO_DIRECTIVES](./GUI_TODO_DIRECTIVES.md) check | ✅ | - |
| CHK-15 | ⚠️ **BATCH 15 COMPLETE - GUI READY** - [Directives Checklist](./GUI_TODO_DIRECTIVES.md#after-completing-a-todo) | ✅ | - |

---

## Summary

| Phase | Batches | Tasks |
|-------|---------|-------|
| Phase 1: Foundation | 1-4 | G01-G40 (40) |
| Phase 2: Quick Panel | 5-6 | G41-G60 (20) |
| Phase 3: Advanced Panel | 7-10 | G61-G100 (40) |
| Phase 4: Debug & Profiles | 11-12 | G101-G121 (21) |
| Phase 5: Network & Polish | 13-15 | G122-G151 (30) |
| **TOTAL** | **15** | **151 tasks** |

---

## Related Documents

- [GUI_TODO_DIRECTIVES.md](./GUI_TODO_DIRECTIVES.md) - **READ BEFORE EVERY TODO**
- [GUI_ARCHITECTURE.md](./GUI_ARCHITECTURE.md) - Design principles
- [GUI_CLASS_DIAGRAM.md](./GUI_CLASS_DIAGRAM.md) - Class structure
- [GUI_NATIVE_WIDGETS.md](./GUI_NATIVE_WIDGETS.md) - Minecraft widgets reference
- [GUI_UTILITIES.md](./GUI_UTILITIES.md) - Factory pattern, theming
- [GUI_DESIGN.md](./GUI_DESIGN.md) - Visual mockups
- [../../03_PARAMETERS.md](../../03_PARAMETERS.md) - Parameter reference

---

*v2.0 - 151 tasks across 15 batches (~10 tasks each)*
