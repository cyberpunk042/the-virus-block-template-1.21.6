# GUI Architecture

> **Status:** Implementation Complete (Phase 1-6)  
> **Created:** December 8, 2024  
> **Updated:** December 9, 2024 (Added category system)  
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

### 1.2 Terminology

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              TERMINOLOGY                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  PROFILE                                                                    │
│  ───────                                                                    │
│  Complete field definition. Loading a profile REPLACES all settings.       │
│  Stored in: field_profiles/ (local) or provided by server (remote)         │
│                                                                             │
│  PRESET                                                                     │
│  ──────                                                                     │
│  Multi-scope partial merge. Can add layers, modify multiple categories.    │
│  MERGES into current state (doesn't replace everything).                   │
│  Stored in: field_presets/                                                 │
│  Example: "Ethereal Glow" sets appearance.glow + animation.alphaPulse      │
│                                                                             │
│  FRAGMENT                                                                   │
│  ────────                                                                   │
│  Single-scope $ref target. Only affects ONE category.                      │
│  Stored in: field_shapes/, field_fills/, field_masks/, etc.                │
│  Example: "Thin Wire" fill fragment only sets fill properties              │
│                                                                             │
│  HIERARCHY:                                                                 │
│  ──────────                                                                 │
│    Profile  ─────►  Complete replacement (all layers, all settings)        │
│       │                                                                     │
│    Preset   ─────►  Partial merge (multiple scopes, can add structure)     │
│       │                                                                     │
│    Fragment ─────►  Single scope ($ref target for shape/fill/animation)    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```


### 1.3 Category System

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          CATEGORY SYSTEM                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  PRESET CATEGORIES (for two-tier dropdown)                                  │
│  ─────────────────────────────────────────                                  │
│                                                                             │
│    ADDITIVE     │ Add elements (rings, layers, beams)                       │
│    STYLE        │ Visual style changes (wireframe, glow)                    │
│    ANIMATION    │ Motion effects (spin, pulse, wobble)                      │
│    EFFECT       │ Composite presets (combat ready, stealth)                 │
│    PERFORMANCE  │ Detail level changes (low/high poly)                      │
│                                                                             │
│  PROFILE CATEGORIES (for filtering)                                         │
│  ──────────────────────────────────                                         │
│                                                                             │
│    COMBAT       │ Battle-focused configurations                             │
│    UTILITY      │ Functional/practical setups                               │
│    DECORATIVE   │ Pure visual/aesthetic                                     │
│    EXPERIMENTAL │ Testing/work-in-progress                                  │
│                                                                             │
│  PROFILE SOURCES (determines editability)                                   │
│  ────────────────────────────────────────                                   │
│                                                                             │
│    BUNDLED      │ Shipped with mod      │ Read-only                         │
│    LOCAL        │ User-created          │ Editable                          │
│    SERVER       │ From server           │ Read-only                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.4 Folder Structure

```
config/the-virus-block/
├── field_presets/
│   ├── additive/
│   │   ├── add_inner_ring.json
│   │   ├── add_outer_ring.json
│   │   └── add_halo.json
│   ├── style/
│   │   ├── wireframe.json
│   │   └── solid_glow.json
│   ├── animation/
│   │   ├── slow_spin.json
│   │   └── pulse_beat.json
│   ├── effect/
│   │   ├── combat_ready.json
│   │   └── stealth_mode.json
│   └── performance/
│       ├── low_detail.json
│       └── high_detail.json
│
├── field_profiles/
│   └── local/                    ← User-created profiles
│       └── my_shield.json
│
└── field_*/                      ← Fragments (existing)
    ├── field_shapes/
    ├── field_fills/
    └── ...
```

### 1.5 JSON Metadata

**Preset JSON:**
```json
{
  "name": "Add Inner Ring",
  "category": "additive",
  "description": "Adds a glowing ring inside the main shape",
  "hint": "Great for layered shields",
  "merge": {
    "layers[0].primitives": [
      {
        "$append": true,
        "id": "inner_ring",
        "type": "ring",
        "shape": { "innerRadius": 0.75, "outerRadius": 0.8 }
      }
    ]
  }
}
```

**Profile JSON:**
```json
{
  "id": "my_combat_shield",
  "name": "My Combat Shield",
  "type": "SHIELD",
  "category": "combat",
  "tags": ["animated", "glow", "multilayer"],
  "description": "Red pulsing shield for PvP",
  "layers": [...]
}
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
│  │                      FieldEditState (Client-Side)                         │   │
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

> **Progress:** Phase 1-6 Complete  
> **Last Updated:** December 9, 2024

### Phase 1: Foundation (F200-F210) ✅ COMPLETE
- [x] `FieldEditState` class (renamed from FieldEditState)
- [x] `FieldCustomizerScreen` skeleton
- [x] Preview renderer (`PreviewPanel`)
- [x] Network packets (`FieldGuiOpenC2SPayload`, `FieldUpdateC2SPayload`, `DebugFieldC2SPayload`, etc.)
- [x] Command registration (`/field customize`)

### Phase 2: Quick Customize (F211-F225) ✅ COMPLETE
- [x] Shape dropdown (`BasicPanel`)
- [x] Color picker (`ColorPicker` widget)
- [x] Alpha slider
- [x] Fill mode dropdown
- [x] Spin speed slider
- [x] Apply button

### Phase 3: Profile System (F226-F235) ✅ COMPLETE
- [x] Local profile storage (`ProfileManager`)
- [x] Save/Load buttons (`ProfilesPanel`)
- [x] Profile dropdown with server/local separation
- [x] Server defaults display (read-only, Save As to copy)
- [x] Import/Export JSON buttons
- [x] Revert to last saved
- [x] Category preset summary display
- [x] Global bottom bar (profile select + Save + Revert, hidden on Profiles tab)

### Phase 4: Advanced Customize (F236-F255) ✅ COMPLETE
- [x] Expandable sections (`AdvancedPanel`)
- [x] Full shape parameters (`ShapeSubPanel`)
- [x] Visibility masks (`VisibilitySubPanel`)
- [x] Pattern selector (`ArrangementSubPanel`)
- [x] Primitive linking (`LinkingSubPanel`)
- [x] Layer navigation (`LayerPanel`)
- [x] Primitive management (`PrimitivePanel`)

### Phase 5: Debug Menu (F256-F270) ✅ COMPLETE
- [x] Permission check
- [x] Bindings panel (`BindingsSubPanel`)
- [x] Triggers panel (`TriggerSubPanel`)
- [x] Lifecycle panel (`LifecycleSubPanel`)
- [x] Beam panel (`BeamSubPanel`)
- [ ] Raw JSON viewer (deferred)

### Phase 6: Polish (F271-F280) ✅ COMPLETE
- [x] Undo/Redo (`UndoManager` class implemented)
- [x] Tooltips (via `GuiWidgets` factory methods)
- [x] Preset system (`PresetRegistry` for shape/fill/visibility/etc.)
- [ ] Live/Manual toggle (deferred - always live for now)
- [ ] Animated preview (deferred)
- [ ] Keyboard shortcuts (not planned)

---

## 10. Open Questions → Decisions Made

| Question | Decision | Implementation |
|----------|----------|----------------|
| **Preset system?** | ✅ Yes - for prediction settings | `PredictionSubPanel` has OFF/LOW/MEDIUM/HIGH/CUSTOM presets |
| **Color themes?** | 🟡 Deferred | Using `GuiConstants` for consistent theming; field color themes TBD |
| **Multi-field?** | ✅ Single DEBUG FIELD | One debug field per GUI session; despawns on close |
| **Copy layer?** | ❌ Not yet | `LayerPanel` skeleton exists but not fully wired |
| **Template system?** | 🟡 Planned | Server defaults architecture exists, not implemented |

### Resolved Questions (December 9, 2024)

| Question | Decision |
|----------|----------|
| **PrimitivePanel?** | ✅ Implemented - manages primitives within a layer |
| **BeamSubPanel?** | ✅ Implemented - in Debug panel with presets |
| **PerformancePanel?** | 🟡 Deferred - performance hints inline on sliders instead |
| **ThemePicker widget?** | ❌ Skipped - using ColorPicker with theme refs |
| **Global bottom bar?** | ✅ Implemented - profile select + Save + Revert, hidden on Profiles tab |
| **Server profiles?** | ✅ Read-only in list, Save As creates local copy |

---

## 11. Command/GUI Unification

> **Status:** Planned  
> **Purpose:** Unified editing between `/field` commands and GUI

### 11.1 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                    SHARED STATE: FieldEditState                      │
│  (shape, fill, transform, animation, bindings, layers, etc.)        │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
        ▼                       ▼                       ▼
   GUI Panels            /field commands         Test Field
   (direct edit)         (packets → edit)        (live preview)
                                                 follows player
```

### 11.2 Command Split

**`/field` commands** - Unified with FieldEditState:
```
/field customize           - Open GUI
/field customize <profile> - Open GUI with profile

/field shape <type>        - Set shape type
/field shape radius <v>    - Set radius
/field transform ...       - Transform parameters
/field fill ...            - Fill parameters
/field visibility ...      - Visibility parameters
/field appearance ...      - Appearance parameters
/field animation ...       - Animation parameters
/field modifier ...        - Field modifiers (bobbing, breathing)
/field orbit ...           - Orbit parameters
/field layer ...           - Layer management
/field primitive ...       - Primitive management
/field binding ...         - Property bindings
/field beam ...            - Beam configuration
/field follow ...          - Follow mode
/field prediction ...      - Prediction settings
/field fragment ...        - Apply single-scope fragment
/field preset apply ...    - Apply multi-scope preset
/field profile ...         - Profile management
/field test spawn/despawn  - Test field control
/field reset               - Reset to defaults
/field status              - Show current state
```

**`/fieldtest` commands** - Debug only (NOT in GUI):
```
/fieldtest shuffle type <quad|segment|sector|edge|triangle>
/fieldtest shuffle next/prev/jump <idx>
/fieldtest vertex <pattern>
/fieldtest list [filter]
/fieldtest info <id>
/fieldtest cycle
/fieldtest spawn <id>     - Spawn from registry (bypasses FieldEditState)
/fieldtest reload
```

### 11.3 Sync Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                            CLIENT                                    │
│                                                                      │
│  ┌──────────────────┐      ┌─────────────────┐                      │
│  │  FieldEditState  │◄────►│ Test Field      │ (client-side only)   │
│  │  (editing state) │      │ Preview Renderer│                      │
│  └────────┬─────────┘      └─────────────────┘                      │
│           │                         ▲                                │
│           │ direct edit             │ reads state                    │
│           ▼                         │ (debounced 50-100ms)           │
│  ┌──────────────────┐               │                               │
│  │ FieldCustomizer  │───────────────┘                               │
│  │     Screen       │                                                │
│  └──────────────────┘                                                │
│           │                                                          │
│           │ C2S packets (profile save, apply to field)              │
└───────────┼──────────────────────────────────────────────────────────┘
            │
            ▼
┌───────────┼──────────────────────────────────────────────────────────┐
│           │                  SERVER                                   │
│           ▼                                                          │
│  ┌──────────────────┐      ┌─────────────────┐                      │
│  │ /field commands  │─────►│ FieldGuiUpdate  │──► S2C to client     │
│  │                  │      │    S2CPayload   │  (updates EditState) │
│  └──────────────────┘      └─────────────────┘                      │
│                                                                      │
│  ┌──────────────────┐      ┌─────────────────┐                      │
│  │ Profile Storage  │◄────►│  FieldRegistry  │                      │
│  │ (save/load)      │      │  (definitions)  │                      │
│  └──────────────────┘      └─────────────────┘                      │
└──────────────────────────────────────────────────────────────────────┘
```

### 11.4 Test Field System

**Test Field is CLIENT-SIDE** - just a renderer reading FieldEditState, not a server FieldManager instance.

| Feature | Description |
|---------|-------------|
| **Spawn** | `/field test spawn` or GUI button in Debug tab |
| **Despawn** | `/field test despawn` or GUI button |
| **Sync** | Reads FieldEditState with debounce (50-100ms) to avoid performance issues |
| **Position** | Follows local player (client-side only) |
| **Purpose** | Live preview of current configuration |

**Key Points:**
1. Test field does NOT go through FieldManager
2. GUI slider drags trigger debounced re-render
3. `/field` commands send S2C packet → client updates FieldEditState → test field re-renders
4. Profile save/apply sends C2S packet to server for persistence

### 11.5 Debounce Strategy

```java
// In TestFieldRenderer or FieldEditState
private long lastUpdateTime = 0;
private static final long DEBOUNCE_MS = 50;

void onFieldEditStateChanged() {
    long now = System.currentTimeMillis();
    if (now - lastUpdateTime > DEBOUNCE_MS) {
        rebuildTestField();
        lastUpdateTime = now;
    } else {
        // Schedule rebuild for later
        scheduleRebuild(DEBOUNCE_MS);
    }
}
```

---

## 12. Related Documents

- [GUI_DESIGN.md](./GUI_DESIGN.md) - Visual mockups and layouts (original design)
- [GUI_CLASS_DIAGRAM.md](./GUI_CLASS_DIAGRAM.md) - **Source of truth** for class structure ✅
- [GUI_COMPONENTS.md](./GUI_COMPONENTS.md) - Widget inventory (not created - see CLASS_DIAGRAM)
- Preset System: Presets are applied per category (shape, fill, visibility, arrangement, animation, beam, follow/prediction); dropdowns show presets + Custom; no separate reset button.

---

*Updated December 9, 2024 - Added Command/GUI Unification architecture*

