# GUI Architecture

> **Status:** Draft v1  
> **Created:** December 8, 2024  
> **Purpose:** Define the architectural foundation for the Field Customizer GUI

---

## 1. Design Principles

### 1.1 Core Philosophy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         GUI DESIGN PRINCIPLES                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. SEPARATION OF CONCERNS                                                 │
│     ─────────────────────                                                  │
│     • GUI only produces FieldDefinition objects                            │
│     • GUI never touches rendering directly                                 │
│     • All state flows through FieldDefinition                              │
│                                                                             │
│  2. PROGRESSIVE DISCLOSURE                                                 │
│     ────────────────────────                                               │
│     • Simple controls visible by default                                   │
│     • Complex features hidden until needed                                 │
│     • Debug features require explicit unlock                               │
│                                                                             │
│  3. LOCAL-FIRST PROFILES                                                   │
│     ─────────────────────                                                  │
│     • Player profiles stored locally (client)                              │
│     • Server provides default templates only                               │
│     • No server upload of personal customizations                          │
│                                                                             │
│  4. NON-DESTRUCTIVE EDITING                                                │
│     ──────────────────────────                                             │
│     • Changes are previewed before applying                                │
│     • Toggle between live/manual apply modes                               │
│     • Undo/redo support                                                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Access Levels

### 2.1 Three-Tier System

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ACCESS LEVELS                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ LEVEL 1: QUICK CUSTOMIZE                                            │   │
│  │ ───────────────────────────                                         │   │
│  │ Access: Always visible                                              │   │
│  │ Target: All players                                                 │   │
│  │                                                                     │   │
│  │ Features:                                                           │   │
│  │ • Shape type selection                                              │   │
│  │ • Color (theme picker + custom hex)                                 │   │
│  │ • Alpha slider                                                      │   │
│  │ • Fill mode (solid/wireframe/cage)                                  │   │
│  │ • Basic animation (spin speed)                                      │   │
│  │ • Follow mode (SNAP/SMOOTH/GLIDE)                                   │   │
│  │ • Prediction toggle + presets (low/medium/high)                     │   │
│  │ • Profile management (save/load/delete)                             │   │
│  │ • Layer navigation (prev/next/add/remove)                           │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ LEVEL 2: ADVANCED CUSTOMIZE                                         │   │
│  │ ──────────────────────────────                                      │   │
│  │ Access: Expandable section (collapsed by default)                   │   │
│  │ Target: Power users                                                 │   │
│  │                                                                     │   │
│  │ Features:                                                           │   │
│  │ • Full shape parameters (all fields per shape type)                 │   │
│  │ • Visibility masks (bands, stripes, checker, radial, gradient)      │   │
│  │ • Arrangement patterns (16+ patterns per cell type)                 │   │
│  │ • Full animation controls (pulse, wobble, phase offset)             │   │
│  │ • Appearance (glow, emissive, saturation, hue shift)                │   │
│  │ • Transform (offset, rotation, scale, anchor, facing, billboard)    │   │
│  │ • Primitive linking (radiusMatch, follow, mirror)                   │   │
│  │ • Full prediction settings (leadTicks, maxDistance, lookAhead...)   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ LEVEL 3: DEBUG MENU                                                 │   │
│  │ ─────────────────────                                               │   │
│  │ Access: Config option (enableDebugMenu) + Operator permission       │   │
│  │ Target: Developers, server admins                                   │   │
│  │                                                                     │   │
│  │ Unlock Conditions (BOTH required):                                  │   │
│  │ • Client config: debugMenuEnabled=true                              │   │
│  │ • Player has operator permission level >= 2                         │   │
│  │                                                                     │   │
│  │ Features:                                                           │   │
│  │ • Bindings panel (property ← source mapping)                        │   │
│  │ • Triggers panel (event → effect mapping)                           │   │
│  │ • Lifecycle panel (fadeIn/Out, scaleIn/Out, decay)                  │   │
│  │ • Beam config (central beam settings)                               │   │
│  │ • Raw JSON viewer/editor                                            │   │
│  │ • Performance stats (render time, vertex count)                     │   │
│  │ • Export/Import JSON files                                          │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Access Method

### 3.1 Command-Based Access

```
/field customize              → Opens GUI with DEBUG FIELD
/field customize <profile>    → Opens GUI and loads profile
```

### 3.2 DEBUG FIELD Concept

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DEBUG FIELD                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  When player opens GUI:                                                    │
│  • DEBUG FIELD spawns on player (visually appears)                         │
│  • Notification: "⚠️ DEBUG MODE - Visual only, no effect"                  │
│  • All changes apply LIVE to DEBUG FIELD                                   │
│  • Player sees changes in real-time ON THEMSELVES                          │
│                                                                             │
│  Purpose:                                                                   │
│  • Sandbox for experimentation                                             │
│  • No gameplay impact (purely visual)                                      │
│  • Preview = actual field on player in world                               │
│                                                                             │
│  On close:                                                                  │
│  • DEBUG FIELD despawns                                                    │
│  • If unsaved changes → "Discard changes?" prompt                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 Advanced Commands (Outside GUI)

Some operations are command-only (not in GUI):
```
/field shuffle corners        → Shuffle corner arrangement
/field shuffle quads          → Shuffle quad patterns
/field shuffle vertices       → Shuffle vertex order
/field layer <add|remove|...> → Layer manipulation
```

---

## 4. Profile System

### 4.1 Storage Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         PROFILE STORAGE                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  CLIENT (Local)                           SERVER                            │
│  ─────────────                            ──────                            │
│                                                                             │
│  .minecraft/                              world/data/                       │
│  └── config/                              └── field_profiles/               │
│      └── thevirusblock/                       ├── defaults/                 │
│          └── field_profiles/                  │   ├── shield_basic.json     │
│              ├── my_shield.json               │   ├── shield_combat.json    │
│              ├── my_aura.json                 │   └── aura_healing.json     │
│              └── backup/                      └── templates/                │
│                  └── *.json.bak                   └── *.json                │
│                                                                             │
│  FLOW:                                                                      │
│  ─────                                                                      │
│                                                                             │
│  1. Player opens GUI                                                        │
│     └── Load local profiles list                                           │
│     └── Request server defaults list                                       │
│                                                                             │
│  2. Player saves profile                                                    │
│     └── Write to LOCAL only                                                │
│     └── Create backup of previous version                                  │
│                                                                             │
│  3. Player loads server default                                            │
│     └── Server sends JSON                                                  │
│     └── Client applies to current definition                               │
│     └── Player can modify and save locally                                 │
│                                                                             │
│  4. Player exports profile                                                  │
│     └── Save to custom location (file dialog)                              │
│                                                                             │
│  5. Player imports profile                                                  │
│     └── Load from file dialog                                              │
│     └── Validate JSON structure                                            │
│     └── Apply to current definition                                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Profile = Visual Variant

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         PROFILE = VISUAL VARIANT                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Profiles are VISUAL CUSTOMIZATIONS only:                                  │
│                                                                             │
│  Example profiles:                                                         │
│  ├── "sphere_solid"    → Solid filled sphere                               │
│  ├── "sphere_mesh"     → Wireframe sphere                                  │
│  ├── "radar"           → Ring-based radar look                             │
│  ├── "stripes"         → Striped bands pattern                             │
│  ├── "cage"            → Cage wireframe                                    │
│  ├── "blue_variant"    → Same shape, blue theme                            │
│  └── "minimal"         → Very transparent, subtle                          │
│                                                                             │
│  ✓ Can change: Shape, pattern, color, alpha, animation, visibility,        │
│                follow mode, prediction settings                            │
│  ✗ Cannot change: Field type, gameplay effects                             │
│                                                                             │
│  Players can save multiple profiles and switch between them                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.3 Profile JSON Structure

```json
{
  "profileVersion": 1,
  "name": "sphere_mesh",
  "description": "Wireframe sphere with cyan theme",
  "created": "2024-12-08T10:30:00Z",
  "modified": "2024-12-08T14:45:00Z",
  "definition": {
    // Full FieldDefinition (visual parts only)
  }
}
```

### 4.4 Player vs Operator Profiles

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      PLAYER vs OPERATOR                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  REGULAR PLAYERS                                                            │
│  ───────────────                                                            │
│  • Edit: DEBUG FIELD only                                                  │
│  • Save: Personal profiles (local storage)                                 │
│  • Apply: To their own active field                                        │
│                                                                             │
│  OPERATORS                                                                  │
│  ─────────                                                                  │
│  • Edit: ANY field type                                                    │
│  • Save: Personal profiles + Server defaults                               │
│  • Apply: To any field, can override server defaults                       │
│  • Override: "Make this the new default for anti-virus fields"             │
│              (Affects new spawns only)                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Update Modes

### 5.1 Always Live Apply, Manual Save

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         UPDATE MODEL                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  CHANGES = ALWAYS LIVE                                                      │
│  ─────────────────────                                                      │
│  • Slider changes → DEBUG FIELD updates IMMEDIATELY                        │
│  • Player sees changes in real-time on themselves                          │
│  • No "preview mode" - the field IS the preview                            │
│                                                                             │
│  SAVE = EXPLICIT ACTION                                                     │
│  ──────────────────────                                                     │
│  • Changes do NOT auto-save to profile                                     │
│  • Player must click [💾 Save Profile] to persist                          │
│  • OR enable [✓ Auto-save] checkbox                                        │
│                                                                             │
│  CLOSE BEHAVIOR                                                             │
│  ──────────────                                                             │
│  • If unsaved changes → "Discard changes?" prompt                          │
│  • DEBUG FIELD despawns when GUI closes                                    │
│                                                                             │
│  APPLY TO ACTUAL FIELD                                                      │
│  ─────────────────────                                                      │
│  • [Apply to My Shield] button                                             │
│  • Copies current DEBUG FIELD config to player's actual field              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. State Management

### 6.1 GUI State Model

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         STATE MANAGEMENT                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      GuiState (Client-Side)                         │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ originalDefinition: FieldDefinition  ← Loaded from server/file      │   │
│  │ workingDefinition: FieldDefinition   ← Current edits (mutable)      │   │
│  │ previewDefinition: FieldDefinition   ← For preview renderer         │   │
│  │ undoStack: List<FieldDefinition>     ← Previous states              │   │
│  │ redoStack: List<FieldDefinition>     ← Undone states                │   │
│  │ isDirty: boolean                     ← Has unsaved changes          │   │
│  │ updateMode: LIVE | MANUAL            ← Current mode                 │   │
│  │ selectedLayerIndex: int              ← Current layer                │   │
│  │ selectedPrimitiveIndex: int          ← Current primitive            │   │
│  │ expandedSections: Set<String>        ← Which panels are open        │   │
│  │ debugMenuUnlocked: boolean           ← Access to Level 3            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                    ┌───────────────┴───────────────┐                       │
│                    ▼                               ▼                       │
│  ┌─────────────────────────┐      ┌─────────────────────────┐             │
│  │    Preview Renderer     │      │    Active Field         │             │
│  │  (uses previewDef)      │      │  (uses workingDef in    │             │
│  │                         │      │   LIVE mode only)       │             │
│  └─────────────────────────┘      └─────────────────────────┘             │
│                                                                             │
│  STATE TRANSITIONS:                                                        │
│  ──────────────────                                                        │
│                                                                             │
│  onSliderChange(value) {                                                   │
│      pushUndo(workingDefinition);                                          │
│      workingDefinition = workingDefinition.with{field}(value);             │
│      previewDefinition = workingDefinition;                                │
│      isDirty = true;                                                       │
│      if (updateMode == LIVE) {                                             │
│          sendToServer(workingDefinition);                                  │
│      }                                                                     │
│  }                                                                         │
│                                                                             │
│  onApply() {                                                               │
│      sendToServer(workingDefinition);                                      │
│      originalDefinition = workingDefinition;                               │
│      isDirty = false;                                                      │
│  }                                                                         │
│                                                                             │
│  onUndo() {                                                                │
│      if (undoStack.isEmpty()) return;                                      │
│      pushRedo(workingDefinition);                                          │
│      workingDefinition = undoStack.pop();                                  │
│      previewDefinition = workingDefinition;                                │
│  }                                                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Network Protocol

### 7.1 Packets

| Packet | Direction | Purpose |
|--------|-----------|---------|
| `FieldGuiOpenC2S` | Client → Server | Request to open GUI |
| `FieldGuiDataS2C` | Server → Client | Send current definition + defaults list |
| `FieldUpdateC2S` | Client → Server | Apply changes (live or manual) |
| `FieldProfileListS2C` | Server → Client | List of server default profiles |
| `FieldProfileRequestC2S` | Client → Server | Request specific server profile |
| `FieldProfileDataS2C` | Server → Client | Profile JSON data |

### 7.2 Packet Flow

```
CLIENT                                    SERVER
──────                                    ──────

/field customize
    │
    ├── FieldGuiOpenC2S ──────────────────▶ Validate permission
    │                                           │
    │   ◀───────────────────────────────── FieldGuiDataS2C
    │       (currentDefinition, defaultsList)
    │
    │   Open GUI Screen
    │   Load local profiles
    │
User makes changes (LIVE mode)
    │
    ├── FieldUpdateC2S ───────────────────▶ Apply to FieldInstance
    │       (delta or full definition)         │
    │                                          │
    │                                     Broadcast to nearby players
    │
User clicks [Apply] (MANUAL mode)
    │
    └── FieldUpdateC2S ───────────────────▶ Same as above
```

---

## 8. Configuration

### 8.1 Client Config (`thevirusblock-client.toml`)

```toml
[gui]
# Enable debug menu (still requires operator permission)
debugMenuEnabled = false

# Max undo history
maxUndoSteps = 50

# Show tooltips (hover info on sliders/controls)
showTooltips = true

# Remember last open tab when reopening GUI
rememberTabState = true
```

### 8.2 Tooltips

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         TOOLTIPS                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Hover over any control to see description:                                │
│                                                                             │
│  [====●===] Spin Speed                                                     │
│        ┌─────────────────────────────────────────┐                         │
│        │ Spin Speed                              │                         │
│        │ ─────────────                           │                         │
│        │ Rotation speed in radians per tick.     │                         │
│        │ 0.02 = slow rotation                    │                         │
│        │ 0.1 = fast rotation                     │                         │
│        │                                         │                         │
│        │ Range: 0.0 - 0.5                        │                         │
│        └─────────────────────────────────────────┘                         │
│                                                                             │
│  Tooltips are lightweight (just text rendering on hover)                   │
│  Can be disabled in config                                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 8.2 Server Config (`thevirusblock-server.toml`)

```toml
[gui]
# Allow GUI access
guiEnabled = true

# Minimum permission level for GUI
minPermissionLevel = 0  # 0 = all, 2 = op only

# Minimum permission for debug menu
debugMenuPermissionLevel = 2  # 2 = op

# Rate limit for live updates (ms between updates)
liveUpdateRateLimitMs = 100

# Max profile name length
maxProfileNameLength = 32
```

---

## 9. Implementation Phases

### Phase 1: Foundation (F200-F210)
- [ ] `GuiState` class
- [ ] `FieldCustomizerScreen` skeleton
- [ ] Preview renderer
- [ ] Network packets
- [ ] Command registration

### Phase 2: Quick Customize (F211-F225)
- [ ] Shape dropdown
- [ ] Color picker
- [ ] Alpha slider
- [ ] Fill mode dropdown
- [ ] Spin speed slider
- [ ] Apply button

### Phase 3: Profile System (F226-F235)
- [ ] Local profile storage
- [ ] Save/Load buttons
- [ ] Profile dropdown
- [ ] Server defaults request
- [ ] Import/Export

### Phase 4: Advanced Customize (F236-F255)
- [ ] Expandable sections
- [ ] Full shape parameters
- [ ] Visibility masks
- [ ] Pattern selector
- [ ] Layer navigation
- [ ] Primitive linking

### Phase 5: Debug Menu (F256-F270)
- [ ] Permission check
- [ ] Bindings panel
- [ ] Triggers panel
- [ ] Lifecycle panel
- [ ] Raw JSON viewer

### Phase 6: Polish (F271-F280)
- [ ] Undo/Redo
- [ ] Live/Manual toggle
- [ ] Animated preview
- [ ] Keyboard shortcuts
- [ ] Tooltips

---

## 10. Open Questions

1. **Preset system?** Should we have quick-apply presets like "Combat", "Stealth", "Healing"?
2. **Color themes?** Should the GUI match the field's color theme?
3. **Multi-field?** Can a player have multiple fields? Edit which one?
4. **Copy layer?** Duplicate layer with all primitives?
5. **Template system?** Server-provided starting points beyond "defaults"?

---

## 11. Related Documents

- [GUI_DESIGN.md](./GUI_DESIGN.md) - Visual mockups and layouts
- [GUI_CLASS_DIAGRAM.md](./GUI_CLASS_DIAGRAM.md) - Class structure (TODO)
- [GUI_COMPONENTS.md](./GUI_COMPONENTS.md) - Widget inventory (TODO)

---

*Draft v1 - Awaiting review and iteration*

