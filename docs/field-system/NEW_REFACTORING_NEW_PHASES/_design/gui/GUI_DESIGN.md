# Field Customizer GUI Design

> **Status:** Architecture Planning  
> **Priority:** Phase 2 (considered in Phase 1, developed in Phase 2)  
> **Created:** December 7, 2024  
> **Updated:** December 8, 2024 - Added Bindings, Triggers, Lifecycle, Linking panels

---

## 1. Why It Won't Break the Architecture

The GUI is an **additional layer on top** of the existing system. It doesn't replace anything:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           INTERACTION LAYERS                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐          │
│  │  JSON Profiles  │   │ Debug Commands  │   │   GUI Panel     │ ← NEW    │
│  │   (files)       │   │ (/fieldtest)    │   │ (in-game UI)    │          │
│  └────────┬────────┘   └────────┬────────┘   └────────┬────────┘          │
│           │                     │                     │                    │
│           └─────────────────────┼─────────────────────┘                    │
│                                 ▼                                          │
│                    ┌─────────────────────────┐                             │
│                    │    FieldDefinition      │  ← SAME CORE               │
│                    │  (the single source)    │                             │
│                    └─────────────────────────┘                             │
│                                 │                                          │
│                                 ▼                                          │
│                    ┌─────────────────────────┐                             │
│                    │  Rendering Pipeline     │  ← UNCHANGED               │
│                    └─────────────────────────┘                             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Key Point:** All three interfaces (JSON, commands, GUI) produce the same `FieldDefinition`. The rendering system doesn't care where it came from.

---

## 2. How It Fits in the Class Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              GUI SYSTEM                                     │
│                      Package: client.gui.field                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    FieldCustomizerScreen                            │   │
│  │                      extends Screen                                 │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ - currentDefinition: FieldDefinition                                │   │
│  │ - selectedLayer: int                                                │   │
│  │ - selectedPrimitive: int                                            │   │
│  │ - previewRenderer: FieldPreviewRenderer                             │   │
│  │ - isDirty: boolean                                                  │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ + init()                         // Setup widgets                   │   │
│  │ + render(context, mouseX, mouseY, delta)                            │   │
│  │ + onValueChanged(field, value)   // Handle slider/dropdown change  │   │
│  │ + rebuildDefinition()            // Create new FieldDefinition     │   │
│  │ + sendToServer()                 // Sync changes                   │   │
│  │ + saveProfile(name)              // Save to file                   │   │
│  │ + loadProfile(name)              // Load from file                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                    ┌───────────────┼───────────────┐                       │
│                    ▼               ▼               ▼                       │
│  ┌─────────────────────┐ ┌─────────────────┐ ┌─────────────────────────┐  │
│  │  FieldPreview       │ │  WidgetPanel    │ │  ProfilePanel           │  │
│  │    Renderer         │ │                 │ │                         │  │
│  ├─────────────────────┤ ├─────────────────┤ ├─────────────────────────┤  │
│  │ Renders mini field  │ │ All the sliders │ │ Save/Load/Delete        │  │
│  │ in preview area     │ │ and dropdowns   │ │ profile management      │  │
│  └─────────────────────┘ └─────────────────┘ └─────────────────────────┘  │
│                                                                             │
│  Custom Widgets:                                                            │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ LabeledSlider     │ Slider with label and value display             │   │
│  │ EnumDropdown<E>   │ Dropdown for any enum (ShapeType, FillMode...)  │   │
│  │ ColorPickerWidget │ Color selection with preview                    │   │
│  │ Vec3Editor        │ X/Y/Z inputs for vectors                        │   │
│  │ RangeSlider       │ Min/max slider for alpha ranges                 │   │
│  │ PatternSelector   │ Visual pattern picker with thumbnails          │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Network Integration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         NETWORK FLOW                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  CLIENT                                    SERVER                           │
│  ──────                                    ──────                           │
│                                                                             │
│  ┌─────────────────┐                                                       │
│  │ User adjusts    │                                                       │
│  │ slider/dropdown │                                                       │
│  └────────┬────────┘                                                       │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────┐                                                       │
│  │ rebuildDef()    │ Creates new FieldDefinition                           │
│  └────────┬────────┘                                                       │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────┐    FieldUpdatePayload    ┌─────────────────────────┐ │
│  │ Local preview   │ ─────────────────────────▶│ PersonalFieldInstance  │ │
│  │ updates         │                           │ .setDefinition(def)     │ │
│  └─────────────────┘                           └─────────────────────────┘ │
│                                                            │                │
│                                                            ▼                │
│                                                 ┌─────────────────────────┐ │
│                         Broadcast to all        │ FieldManager            │ │
│                         nearby players          │ .broadcastUpdate(id)    │ │
│                                                 └─────────────────────────┘ │
│                                                                             │
│  SAVE FLOW:                                                                │
│  ┌─────────────────┐    FieldSavePayload      ┌─────────────────────────┐ │
│  │ [Save Profile]  │ ─────────────────────────▶│ FieldProfileStore       │ │
│  │ button clicked  │                           │ .saveForPlayer(uuid,    │ │
│  └─────────────────┘                           │   name, definition)     │ │
│                                                 └─────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Screen Layout (Detailed)

```
┌────────────────────────────────────────────────────────────────────────────┐
│ ═══════════════════════ ⚡ FIELD CUSTOMIZER ⚡ ═══════════════════════════ │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  ┌─ LAYER ──────────────────────────────────────────────────────────────┐ │
│  │  [◀] Layer 1 of 3 [▶]    [+ Add Layer]  [🗑 Delete]  [↑↓ Reorder]   │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
│                                                                            │
│  ┌─ PRIMITIVE ──────────────────┐  ┌─ PREVIEW ─────────────────────────┐ │
│  │                              │  │                                   │ │
│  │  Shape: [▼ Sphere         ]  │  │     ╭───────────────────╮        │ │
│  │                              │  │    ╱                     ╲       │ │
│  │  ── Shape Config ──          │  │   │    ◉ ◉ ◉ ◉ ◉ ◉ ◉    │       │ │
│  │  Radius:    [====●===] 1.5   │  │   │   ◉ ◉ ◉ ◉ ◉ ◉ ◉ ◉   │       │ │
│  │  Lat Steps: [======●=] 48    │  │   │  ◉ ◉ ◉ ◉ ◉ ◉ ◉ ◉ ◉  │       │ │
│  │  Lon Steps: [======●=] 96    │  │   │   ◉ ◉ ◉ ◉ ◉ ◉ ◉ ◉   │       │ │
│  │                              │  │   │    ◉ ◉ ◉ ◉ ◉ ◉ ◉    │       │ │
│  │  ── Fill Mode ──             │  │    ╲                     ╱       │ │
│  │  Mode: [▼ Solid           ]  │  │     ╰───────────────────╯        │ │
│  │  Wire Thickness: [===●==] 1  │  │                                   │ │
│  │                              │  │   [Rotate] [Zoom +] [Zoom -]     │ │
│  │  ── Visibility ──            │  │                                   │ │
│  │  Mask: [▼ Bands           ]  │  └───────────────────────────────────┘ │
│  │  Count:     [====●===] 8     │                                        │
│  │  Thickness: [=====●==] 0.5   │  ┌─ LAYER OPTIONS ───────────────────┐ │
│  │                              │  │                                   │ │
│  │  ── Arrangement ──           │  │  Rotation:  X[___] Y[___] Z[___] │ │
│  │  Pattern: [▼ filled_1     ]  │  │  Spin:      [====●===] 0.02      │ │
│  │  [◀ Prev] [▶ Next] [Shuffle] │  │  Phase:     [===●====] 0.0       │ │
│  │                              │  │  Visible:   [✓]                  │ │
│  └──────────────────────────────┘  │                                   │ │
│                                     └───────────────────────────────────┘ │
│  ┌─ APPEARANCE ─────────────────┐  ┌─ ANIMATION ───────────────────────┐ │
│  │                              │  │                                   │ │
│  │  Color: [█████] @primary [▼] │  │  Spin Axis:  [▼ Y              ] │ │
│  │  Alpha: [====●========] 0.7  │  │  Spin Speed: [====●===] 0.02     │ │
│  │  Glow:  [==●==========] 0.3  │  │  Pulse:      [==●=====] 0.1      │ │
│  │                              │  │  Phase:      [===●====] 0.0      │ │
│  └──────────────────────────────┘  └───────────────────────────────────┘ │
│                                                                            │
│  ┌─ ACTIONS ────────────────────────────────────────────────────────────┐ │
│  │                                                                      │ │
│  │  [💾 Save As...]  [📂 Load...]  [⭐ Set Default]  [↩ Reset]  [✕ Close] │
│  │                                                                      │ │
│  │  Saved Profiles: [▼ my_shield_v2                                  ]  │ │
│  │                                                                      │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

### 4.1 Advanced Tab: Bindings, Triggers, Lifecycle

```
┌────────────────────────────────────────────────────────────────────────────┐
│ ═══════════════════════ ⚡ FIELD CUSTOMIZER ⚡ ═══════════════════════════ │
│ [Basic] [🔗 Advanced] [⚙ Field Settings]                                   │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  ┌─ BINDINGS ─────────────────────────────────────────────────────────┐   │
│  │  Dynamic properties that respond to player state                    │   │
│  │                                                                     │   │
│  │  [+ Add Binding]                                                    │   │
│  │                                                                     │   │
│  │  ┌─ alpha ──────────────────────────────────────────────────────┐  │   │
│  │  │ Source: [▼ player.health_percent                          ]  │  │   │
│  │  │ Input:  [●════════] 0.0  ──to──  [════════●] 1.0            │  │   │
│  │  │ Output: [●════════] 0.3  ──to──  [════════●] 1.0            │  │   │
│  │  │ Curve:  [▼ ease_out                                       ]  │  │   │
│  │  │                                              [🗑 Remove]     │  │   │
│  │  └──────────────────────────────────────────────────────────────┘  │   │
│  │                                                                     │   │
│  │  ┌─ scale ──────────────────────────────────────────────────────┐  │   │
│  │  │ Source: [▼ player.damage_taken                            ]  │  │   │
│  │  │ Input:  [●════════] 0.0  ──to──  [════════●] 10.0           │  │   │
│  │  │ Output: [●════════] 1.0  ──to──  [════════●] 1.5            │  │   │
│  │  │ Curve:  [▼ ease_in_out                                    ]  │  │   │
│  │  │                                              [🗑 Remove]     │  │   │
│  │  └──────────────────────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                            │
│  ┌─ TRIGGERS ─────────────────────────────────────────────────────────┐   │
│  │  Visual effects that fire on game events                            │   │
│  │                                                                     │   │
│  │  [+ Add Trigger]                                                    │   │
│  │                                                                     │   │
│  │  ┌─ Damage Flash ───────────────────────────────────────────────┐  │   │
│  │  │ Event:     [▼ player.damage                               ]  │  │   │
│  │  │ Effect:    [▼ flash                                       ]  │  │   │
│  │  │ Duration:  [====●===] 6 ticks                                │  │   │
│  │  │ Color:     [█████] #FF0000                                   │  │   │
│  │  │                                              [🗑 Remove]     │  │   │
│  │  └──────────────────────────────────────────────────────────────┘  │   │
│  │                                                                     │   │
│  │  ┌─ Heal Pulse ─────────────────────────────────────────────────┐  │   │
│  │  │ Event:     [▼ player.heal                                 ]  │  │   │
│  │  │ Effect:    [▼ pulse                                       ]  │  │   │
│  │  │ Duration:  [====●===] 10 ticks                               │  │   │
│  │  │ Scale:     [====●===] 1.2                                    │  │   │
│  │  │                                              [🗑 Remove]     │  │   │
│  │  └──────────────────────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                            │
│  ┌─ LIFECYCLE ────────────────────────────────────────────────────────┐   │
│  │  Spawn/despawn animations                                           │   │
│  │                                                                     │   │
│  │  Fade In:    [====●===] 10 ticks     Scale In:  [====●===] 10 ticks│   │
│  │  Fade Out:   [====●===] 10 ticks     Scale Out: [====●===] 10 ticks│   │
│  │                                                                     │   │
│  │  ── Decay (gradual fade over time) ──                              │   │
│  │  Rate:       [==●=====] 0.001/tick   Min Alpha: [====●===] 0.3     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Primitive Linking Panel

```
┌─ PRIMITIVE LINKING ───────────────────────────────────────────────────────┐
│  Link this primitive to another primitive's properties                     │
│                                                                            │
│  Radius Match: [▼ (none)                                               ]  │
│                [▼ sphere_1                                             ]  │
│                [▼ ring_outer                                           ]  │
│  Offset:       [====●===] +0.5                                            │
│                                                                            │
│  Follow:       [▼ (none)                  ]  ← Copy position from target  │
│  Mirror Axis:  [▼ (none) | X | Y | Z      ]  ← Mirror offset              │
│  Phase Offset: [====●===] 0.0                ← Animation sync             │
│  Scale With:   [▼ (none)                  ]  ← Scale from target          │
│                                                                            │
│  ⚠️ Can only link to primitives EARLIER in the layer (no cycles)          │
└────────────────────────────────────────────────────────────────────────────┘
```

### 4.3 Available Binding Sources

| Source ID | Description | Range |
|-----------|-------------|-------|
| `player.health` | Raw health value | 0-20 |
| `player.health_percent` | Health as 0-1 | 0-1 |
| `player.armor` | Armor points | 0-20 |
| `player.food` | Food level | 0-20 |
| `player.speed` | Movement speed | 0-∞ |
| `player.is_sprinting` | Boolean | 0/1 |
| `player.is_sneaking` | Boolean | 0/1 |
| `player.is_flying` | Boolean | 0/1 |
| `player.is_invisible` | Boolean | 0/1 |
| `player.in_combat` | In combat (100 ticks) | 0/1 |
| `player.damage_taken` | Decayed damage | 0-∞ |
| `field.age` | Field age in ticks | 0-∞ |

### 4.4 Available Trigger Events

| Event | Fires When |
|-------|------------|
| `player.damage` | Player takes damage |
| `player.heal` | Player heals |
| `player.death` | Player dies |
| `player.respawn` | Player respawns |
| `field.spawn` | Field is created |
| `field.despawn` | Field is removed |

### 4.5 Available Trigger Effects

| Effect | Parameters | Behavior |
|--------|------------|----------|
| `flash` | color, duration | Brief color overlay |
| `pulse` | scale, duration | Scale up then back |
| `shake` | amplitude, duration | Rapid position jitter |
| `glow` | intensity, duration | Temporary glow boost |
| `color_shift` | color, duration | Temporary color change |

---

## 5. Data Flow

```
User Action                    GUI                      Core System
───────────                    ───                      ───────────

Adjust slider ──────────▶ onValueChanged()
                               │
                               ▼
                         currentValues.radius = 1.5
                               │
                               ▼
                         rebuildDefinition() ──────▶ FieldDefinition
                               │                           │
                               ▼                           │
                         previewRenderer.update() ◀────────┘
                               │
                               ▼
                         (Live preview updates)
                               │
[Apply] clicked ───────▶ sendToServer() ────────────▶ FieldUpdatePayload
                                                           │
                                                           ▼
                                                  PersonalFieldInstance
                                                           │
                                                           ▼
                                                  (Field updates in world)

[Save] clicked ─────────▶ saveProfile() ────────────▶ FieldSavePayload
                                                           │
                                                           ▼
                                                  FieldProfileStore
                                                           │
                                                           ▼
                                                  (Saved to player data)
```

---

## 6. Implementation Order

### Phase 1: Basic GUI (Minimum Viable)
1. `FieldCustomizerScreen` with basic layout
2. Shape type dropdown
3. Key shape parameters (radius, steps)
4. Fill mode dropdown
5. Preview renderer (static, no rotation)
6. Apply button → sends to server

### Phase 2: Full Basic Controls
1. All shape parameters
2. All fill, visibility, arrangement controls
3. Appearance controls (color, alpha, glow)
4. Animation controls (spin, pulse)
5. Live preview (auto-apply as you change)

### Phase 3: Layer Management
1. Layer tabs/navigation
2. Add/remove layers
3. Layer-specific settings (rotation, visibility)
4. Primitive navigation within layer
5. **Primitive linking UI** (radiusMatch, follow, mirror)

### Phase 4: Advanced Tab (Bindings, Triggers, Lifecycle)
1. **Bindings panel** - source dropdown, input/output ranges, curve
2. **Triggers panel** - event, effect, duration, parameters
3. **Lifecycle panel** - fadeIn/Out, scaleIn/Out, decay
4. Add/remove bindings and triggers dynamically

### Phase 5: Profile Management
1. Save/Load profiles
2. Named presets
3. Set as default
4. Import/Export JSON

### Phase 6: Polish
1. Animated preview (rotating field)
2. Pattern thumbnails
3. Color picker with theme colors
4. Real-time binding preview (show health → alpha mapping)
5. Trigger preview (test flash effect)
6. Undo/Redo
7. Keyboard shortcuts

---

## 7. New Classes Needed

### Core GUI Classes

| Class | Package | Purpose |
|-------|---------|---------|
| `FieldCustomizerScreen` | client.gui.field | Main screen with tabs |
| `FieldPreviewRenderer` | client.gui.field | Preview area renderer |
| `BasicTab` | client.gui.field.tab | Shape, fill, visibility, appearance |
| `AdvancedTab` | client.gui.field.tab | Bindings, triggers, lifecycle |
| `FieldSettingsTab` | client.gui.field.tab | Follow mode, prediction, beam |
| `ProfilePanel` | client.gui.field | Save/load UI |

### Custom Widgets

| Class | Package | Purpose |
|-------|---------|---------|
| `LabeledSlider` | client.gui.widget | Slider with label |
| `EnumDropdown<E>` | client.gui.widget | Generic enum dropdown |
| `ColorPickerWidget` | client.gui.widget | Color selection |
| `Vec3Editor` | client.gui.widget | XYZ input |
| `RangeSlider` | client.gui.widget | Min/max range |
| `PatternSelector` | client.gui.widget | Pattern picker |
| `BindingEditor` | client.gui.widget | Single binding config |
| `TriggerEditor` | client.gui.widget | Single trigger config |
| `LifecycleEditor` | client.gui.widget | Lifecycle config |
| `LinkEditor` | client.gui.widget | Primitive link config |
| `SourceDropdown` | client.gui.widget | Binding source picker |
| `EventDropdown` | client.gui.widget | Trigger event picker |
| `EffectDropdown` | client.gui.widget | Trigger effect picker |
| `CurveDropdown` | client.gui.widget | Interpolation curve picker |

### Network Packets

| Class | Package | Purpose |
|-------|---------|---------|
| `FieldUpdatePayload` | network | Live field update |
| `FieldSavePayload` | network | Save profile packet |
| `FieldLoadPayload` | network | Load profile packet |

**Total: ~20 new classes** (up from 12)

---

## 8. Integration Points (No Architecture Changes)

The GUI integrates at these existing points:

| Component | Integration | Changes Required |
|-----------|-------------|------------------|
| `FieldDefinition` | GUI reads/writes | **None** |
| `FieldManager` | Apply changes | **None** |
| `FieldNetworking` | Sync to server | Add 2 new payloads |
| `FieldProfileStore` | Save/load | **None** (already exists) |
| `FieldRenderer` | Preview uses it | **None** |
| `ClientFieldManager` | Preview uses it | **None** |

**Total architecture impact: 2 new network payloads, ~10 new GUI classes**

---

## 9. Opening the GUI

Options:
1. **Keybind:** Press `G` while holding personal field item
2. **Command:** `/field customize`
3. **Right-click:** On personal field item
4. **From totem screen:** Tab/button in existing UI

---

## 10. Summary

✅ **Doable:** Yes, Minecraft/Fabric fully supports custom screens  
✅ **Clean:** GUI is additive, doesn't change core architecture  
✅ **Reuses:** Same `FieldDefinition`, same rendering, same networking  
✅ **Priority:** Phase 2 - considered during Phase 1 design, implemented in Phase 2  

**The GUI is just another way to build a `FieldDefinition` - the core system doesn't need to know or care where it came from.**

---

## 11. Phase 1 Considerations

While the GUI is developed in Phase 2, Phase 1 should ensure:

1. ✅ **FieldDefinition is immutable/clonable** - GUI will need to create modified copies
2. ✅ **All parameters are serializable** - GUI will need to read/write JSON (`toJson()` methods)
3. ✅ **Commands expose same knobs** - `/fieldtest edit` tests the same values GUI will control
4. ✅ **Network payloads support live updates** - GUI will send partial updates
5. ✅ **Bindings system complete** - 12 sources, curve interpolation, range mapping
6. ✅ **Triggers system complete** - 6 events, 5 effects, duration tracking
7. ✅ **Lifecycle system complete** - fadeIn/Out, scaleIn/Out, decay
8. ✅ **Primitive linking complete** - radiusMatch, follow, mirror, phaseOffset, scaleWith
9. ✅ **VisibilityMask complete** - FULL, BANDS, STRIPES, CHECKER, RADIAL, GRADIENT

**All Phase 1 prerequisites are now complete!** ✅

---

## 12. FieldDefinition Coverage

The GUI needs to expose all 12 top-level fields:

| Field | GUI Location | Status |
|-------|--------------|--------|
| `id` | Header (read-only) | ✅ Planned |
| `type` | Field Settings tab | ✅ Planned |
| `baseRadius` | Basic tab | ✅ Planned |
| `themeId` | Appearance section | ✅ Planned |
| `layers` | Layer navigation | ✅ Planned |
| `modifiers` | Field Settings tab | ✅ Planned |
| `prediction` | Field Settings tab | ✅ Planned |
| `beam` | Field Settings tab | ✅ Planned |
| `followMode` | Field Settings tab | ✅ Planned |
| `bindings` | Advanced tab | ✅ Planned |
| `triggers` | Advanced tab | ✅ Planned |
| `lifecycle` | Advanced tab | ✅ Planned |

---

*Ready for implementation in Phase 2 - all core systems are complete!*

