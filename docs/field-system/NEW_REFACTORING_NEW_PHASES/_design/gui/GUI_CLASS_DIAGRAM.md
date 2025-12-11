# GUI Class Diagram

> **Status:** Implementation Complete ✅ (Verified by audit)  
> **Created:** December 8, 2024  
> **Updated:** December 10, 2025 (Added 13 evolved classes, audit verification)  
> **Purpose:** Define all classes needed for the Field Customizer GUI  
> **Reference:** [03_PARAMETERS.md](../03_PARAMETERS.md) for field coverage

---

## 1. Package Structure

```
net.cyberpunk042.client.gui/
├── screen/
│   ├── FieldCustomizerScreen.java       # Main GUI screen
│   └── TabType.java                     # Tab navigation enum
│
├── state/
│   ├── FieldEditState.java              # Full GUI state container
│   ├── FieldEditStateHolder.java        # Singleton access to state
│   ├── StateAccessor.java               # Reflection-based state access
│   ├── AppearanceState.java             # Appearance-specific state
│   ├── EditorState.java                 # Current editing context
│   └── UndoManager.java                 # Undo/redo stack
│
├── panel/
│   ├── QuickPanel.java                  # Level 1: Quick Customize
│   ├── AdvancedPanel.java               # Level 2: Advanced Customize
│   ├── DebugPanel.java                  # Level 3: Debug Menu
│   ├── LayerPanel.java                  # Layer navigation
│   ├── PrimitivePanel.java              # Primitive editing
│   ├── ProfilesPanel.java               # Profile management
│   ├── ActionPanel.java                 # Action buttons (Apply, Reset)
│   └── sub/
│       ├── ShapeSubPanel.java           # Shape parameters
│       ├── AppearanceSubPanel.java      # Color, alpha, glow
│       ├── AnimationSubPanel.java       # Spin, pulse, phase
│       ├── TransformSubPanel.java       # Position, rotation, scale
│       ├── VisibilitySubPanel.java      # Mask configuration
│       ├── ArrangementSubPanel.java     # Pattern selection
│       ├── FillSubPanel.java            # Fill mode config
│       ├── LinkingSubPanel.java         # Primitive linking
│       ├── BindingsSubPanel.java        # Debug: Bindings
│       ├── TriggerSubPanel.java         # Debug: Triggers
│       ├── LifecycleSubPanel.java       # Debug: Lifecycle
│       ├── BeamSubPanel.java            # Debug: Central beam
│       ├── PredictionSubPanel.java      # Prediction settings
│       ├── FollowModeSubPanel.java      # Follow mode settings
│       ├── ModifiersSubPanel.java       # Bobbing, breathing, etc.
│       └── OrbitSubPanel.java           # Orbit configuration
│
├── widget/
│   ├── LabeledSlider.java               # Slider with label + value
│   ├── ColorButton.java                 # Color with popup picker
│   ├── Vec3Editor.java                  # X/Y/Z inputs
│   ├── ExpandableSection.java           # Collapsible section
│   ├── BottomActionBar.java             # Profile/preset quick bar
│   ├── ConfirmDialog.java               # Confirmation popup
│   ├── PresetConfirmDialog.java         # Preset application dialog
│   ├── ToastNotification.java           # Toast feedback messages
│   └── LoadingIndicator.java            # Loading spinner
│   # Note: EnumDropdown, RangeSlider, ActionButton use MC's CyclingButtonWidget/SliderWidget
│
├── util/
│   ├── GuiWidgets.java                  # Widget factory methods
│   ├── GuiAnimations.java               # Animation utilities (fade, lerp)
│   ├── GuiLayout.java                   # Layout helpers (positioning)
│   ├── GuiConstants.java                # Theme constants (colors, sizes)
│   ├── GuiKeyboardNav.java              # Keyboard navigation helpers
│   ├── FragmentRegistry.java            # Single-scope fragments (shape/fill/visibility/etc.)
│   └── PresetRegistry.java              # Multi-scope presets (load from field_presets/)
│
├── profile/
│   ├── ProfileManager.java              # Load/save/list profiles
│   ├── Profile.java                     # Profile data record
│   └── ProfileValidator.java            # JSON validation
│
└── network/
    ├── FieldGuiOpenC2S.java             # Request GUI open
    ├── FieldGuiDataS2C.java             # Current definition + defaults
    ├── FieldUpdateC2S.java              # Apply changes
    ├── FieldProfileListS2C.java         # Server profile list
    ├── FieldProfileRequestC2S.java      # Request server profile
    └── FieldProfileDataS2C.java         # Profile JSON response
```

---

## 2. Core Classes

### 2.1 FieldCustomizerScreen

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        FieldCustomizerScreen                                 │
│                        extends Screen                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│ Fields:                                                                     │
│   - state: FieldEditState                                                         │
│   - quickPanel: QuickPanel                                                  │
│   - advancedPanel: AdvancedPanel                                            │
│   - debugPanel: DebugPanel (nullable)                                       │
│   - layerPanel: LayerPanel                                                  │
│   - profilePanel: ProfilePanel                                              │
│   - currentTab: TabType                                                     │
│   - debugFieldInstance: FieldInstance                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ Methods:                                                                    │
│   + init()                                                                  │
│   + render(DrawContext, int, int, float)                                    │
│   + tick()                                                                  │
│   + keyPressed(int, int, int): boolean                                      │
│   + close()                                                                 │
│   - initPanels()                                                            │
│   - initDebugField()                                                        │
│   - applyChanges()                                                          │
│   - saveProfile()                                                           │
│   - loadProfile(String)                                                     │
│   - undo()                                                                  │
│   - redo()                                                                  │
│   - switchTab(TabType)                                                      │
│   - promptUnsavedChanges(): boolean                                         │
│   - spawnDebugField()                                                       │
│   - despawnDebugField()                                                     │
└─────────────────────────────────────────────────────────────────────────────┘

TabType enum:
  QUICK, ADVANCED, DEBUG, PROFILES
```

### 2.2 FieldEditState

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              FieldEditState                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│ Fields:                                                                     │
│   - originalDefinition: FieldDefinition     # Loaded from server/file       │
│   - workingDefinition: FieldDefinition      # Current edits (rebuilt)       │
│   - undoManager: UndoManager                # Undo/redo stacks              │
│   - isDirty: boolean                        # Has unsaved changes           │
│   - autoSaveEnabled: boolean                # Auto-save checkbox            │
│   - currentProfileName: String              # Loaded profile name           │
│   - isCurrentProfileServer: boolean         # True if loaded from server    │
│   - editorState: EditorState                # Selection context             │
│   - debugMenuUnlocked: boolean              # Level 3 access                │
│   - expandedSections: Set<String>           # Open panels                   │
├─────────────────────────────────────────────────────────────────────────────┤
│ Methods:                                                                    │
│   + getDefinition(): FieldDefinition                                        │
│   + updateDefinition(FieldDefinition)                                       │
│   + markDirty()                                                             │
│   + clearDirty()                                                            │
│   + canUndo(): boolean                                                      │
│   + canRedo(): boolean                                                      │
│   + undo(): FieldDefinition                                                 │
│   + redo(): FieldDefinition                                                 │
│   + reset()                                                                 │
│   + isDebugUnlocked(): boolean                                              │
│   + getSelectedLayer(): LayerDefinition                                     │
│   + getSelectedPrimitive(): Primitive                                       │
│   + getCurrentProfileName(): String                                         │
│   + isCurrentProfileServer(): boolean                                       │
│   + setCurrentProfile(String name, boolean isServer)                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 EditorState

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             EditorState                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│ Fields:                                                                     │
│   - selectedLayerIndex: int                 # -1 = none                     │
│   - selectedPrimitiveIndex: int             # -1 = none                     │
│   - focusedField: String                    # Currently focused input       │
│   - hoveredWidget: String                   # For tooltips                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ Methods:                                                                    │
│   + selectLayer(int)                                                        │
│   + selectPrimitive(int)                                                    │
│   + clearSelection()                                                        │
│   + getLayerIndex(): int                                                    │
│   + getPrimitiveIndex(): int                                                │
│   + hasLayerSelected(): boolean                                             │
│   + hasPrimitiveSelected(): boolean                                         │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.4 UndoManager

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            UndoManager                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ Fields:                                                                     │
│   - undoStack: Deque<FieldDefinition>       # Max 50 entries                │
│   - redoStack: Deque<FieldDefinition>                                       │
│   - maxSize: int                            # From config                   │
├─────────────────────────────────────────────────────────────────────────────┤
│ Methods:                                                                    │
│   + push(FieldDefinition)                   # Before change                 │
│   + undo(FieldDefinition): FieldDefinition  # Returns previous state        │
│   + redo(FieldDefinition): FieldDefinition  # Returns next state            │
│   + canUndo(): boolean                                                      │
│   + canRedo(): boolean                                                      │
│   + clear()                                                                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Panel Classes

### 3.1 QuickPanel (Level 1)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              QuickPanel                                      │
│                       extends AbstractPanel                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ Provides controls for (from 03_PARAMETERS.md):                              │
│                                                                             │
│   ┌─ SHAPE ─────────────────────────────────────────────────────────────┐   │
│   │ • shapeType: EnumDropdown<ShapeType>                                │   │
│   │   (SPHERE, RING, DISC, PRISM, CYLINDER, POLYHEDRON)                 │   │
│   │ • radius: LabeledSlider (0.1 - 10.0)                                │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│   ┌─ APPEARANCE ────────────────────────────────────────────────────────┐   │
│   │ • color: ThemePicker + ColorButton for custom                       │   │
│   │ • alpha: LabeledSlider (0.0 - 1.0)                                  │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│   ┌─ FILL ──────────────────────────────────────────────────────────────┐   │
│   │ • fillMode: EnumDropdown<FillMode>                                  │   │
│   │   (SOLID, WIREFRAME, CAGE, POINTS)                                  │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│   ┌─ ANIMATION ─────────────────────────────────────────────────────────┐   │
│   │ • spinSpeed: LabeledSlider (0.0 - 0.5)                              │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│   ┌─ BEHAVIOR ──────────────────────────────────────────────────────────┐   │
│   │ • followMode: EnumDropdown<FollowMode> (SNAP, SMOOTH, GLIDE)        │   │
│   │ • predictionEnabled: Toggle                                         │   │
│   │ • predictionPreset: EnumDropdown (LOW, MEDIUM, HIGH, CUSTOM)        │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ Methods:                                                                    │
│   + init(FieldEditState)                                                          │
│   + render(DrawContext)                                                     │
│   + onShapeTypeChanged(ShapeType)                                           │
│   + onColorChanged(int)                                                     │
│   + onAlphaChanged(float)                                                   │
│   + onFillModeChanged(FillMode)                                             │
│   + onSpinSpeedChanged(float)                                               │
│   + onFollowModeChanged(FollowMode)                                         │
│   + onPredictionToggled(boolean)                                            │
│   + onPredictionPresetChanged(PredictionPreset)                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 AdvancedPanel (Level 2)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            AdvancedPanel                                     │
│                       extends AbstractPanel                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ Contains expandable sub-panels:                                             │
│                                                                             │
│   ▸ Shape Details         → ShapeSubPanel                                   │
│   ▸ Appearance            → AppearanceSubPanel                              │
│   ▸ Animation             → AnimationSubPanel                               │
│   ▸ Transform             → TransformSubPanel                               │
│   ▸ Visibility Mask       → VisibilitySubPanel                              │
│   ▸ Arrangement           → ArrangementSubPanel                             │
│   ▸ Fill Options          → FillSubPanel                                    │
│   ▸ Primitive Linking     → LinkingSubPanel                                 │
│   ▸ Prediction Settings   → PredictionSubPanel                              │
│   ▸ Follow Mode Settings  → FollowModeSubPanel                              │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ Fields:                                                                     │
│   - subPanels: Map<String, AbstractSubPanel>                                │
│   - expandedSections: Set<String>                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│ Methods:                                                                    │
│   + init(FieldEditState)                                                          │
│   + render(DrawContext)                                                     │
│   + toggleSection(String)                                                   │
│   + collapseAll()                                                           │
│   + expandSection(String)                                                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 DebugPanel (Level 3)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             DebugPanel                                       │
│                       extends AbstractPanel                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ Contains debug-only sub-panels:                                             │
│                                                                             │
│   ▸ Bindings              → BindingsSubPanel     ✅ Implemented             │
│   ▸ Triggers              → TriggerSubPanel      ✅ Implemented             │
│   ▸ Lifecycle             → LifecycleSubPanel    ✅ Implemented             │
│   ▸ Beam Config           → BeamSubPanel         ✅ Implemented             │
│   ▸ Raw JSON              → JsonViewerPanel      ⏳ Deferred                │
│   ▸ Performance           → PerformancePanel     ⏳ Deferred (inline hints) │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ Requires:                                                                   │
│   - debugMenuEnabled=true in client config                                  │
│   - Player permission level >= 2                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│ Methods:                                                                    │
│   + isUnlocked(): boolean                                                   │
│   + checkPermissions(ClientPlayerEntity): boolean                           │
│   + showLockedMessage()                                                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.4 LayerPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             LayerPanel                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ Fields:                                                                     │
│   - layers: List<LayerEntry>                                                │
│   - selectedIndex: int                                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│ UI:                                                                         │
│   ┌──────────────────────────────────────┐                                  │
│   │  Layers                    [+] [-]   │                                  │
│   ├──────────────────────────────────────┤                                  │
│   │  ● Layer 0: "main_sphere"            │  ← Selected                      │
│   │  ○ Layer 1: "outer_ring"             │                                  │
│   │  ○ Layer 2: "inner_glow"             │                                  │
│   └──────────────────────────────────────┘                                  │
│   [▲] [▼] [👁] [🗑]                                                          │
│    ↑   ↑   ↑   ↑                                                            │
│   up down visible delete                                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ Methods:                                                                    │
│   + selectLayer(int)                                                        │
│   + addLayer()                                                              │
│   + removeLayer(int)                                                        │
│   + moveLayerUp(int)                                                        │
│   + moveLayerDown(int)                                                      │
│   + toggleLayerVisibility(int)                                              │
│   + duplicateLayer(int)                                                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.5 ProfilesPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            ProfilesPanel                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ Records:                                                                    │
│   ProfileEntry(String name, boolean isServer)                               │
│                                                                             │
│ UI:                                                                         │
│   ┌───────────────────────────────────────────────────────────────────────┐ │
│   │ Profiles (select list)        │ Category Presets (read-only)          │ │
│   │                               │                                       │ │
│   │  ● my_shield_v2   (local)     │  Shape:       Sphere Default          │ │
│   │  ○ radar_pulse    (local)     │  Visibility:  Bands                   │ │
│   │  ○ cage_wire      (local)     │  Arrangement: Wavey                   │ │
│   │  ○ shield_default (server)    │  Fill:        Wireframe               │ │
│   │  ○ aura_heal      (server)    │  Animation:   Spin Slow               │ │
│   │                               │  Beam:        None                    │ │
│   │                               │  Follow:      Smooth                  │ │
│   │                               │  Prediction:  Medium                  │ │
│   │                               │  (If no match → CUSTOM)               │ │
│   │                                                                       │ │
│   │  Name: [ my_shield_v2              ]                                  │ │
│   │  Source: Local                                                        │ │
│   ├───────────────────────────────────────────────────────────────────────┤ │
│   │ Actions (Profiles tab only):                                          │ │
│   │  Load   Save   Save As…   Revert   Rename   Duplicate   Delete        │ │
│   │  Import JSON   Export JSON   Set Default                              │ │
│   │                                                                       │ │
│   │ Status: ● Unsaved changes (local)                                     │ │
│   └───────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│ Behavior:                                                                   │
│   - Local selected: Save enabled when dirty; Revert restores last saved    │
│   - Server selected: Save disabled; Save As creates local copy             │
│                                                                             │
│ Global Bottom Bar (non-Profile tabs only):                                  │
│   [ Profile: (dropdown) ] [ SAVE ] [ REVERT ]                               │
│   - Hidden on Profiles tab                                                  │
│   - Save As behavior when server profile loaded                             │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ Methods:                                                                    │
│   + loadProfile()                                                           │
│   + saveProfile()                                                           │
│   + saveProfileAs()                                                         │
│   + revertProfile()                                                         │
│   + deleteProfile()                                                         │
│   + renameProfile()                                                         │
│   + duplicateProfile()                                                      │
│   + importJson()                                                            │
│   + exportJson()                                                            │
│   + setAsDefault()                                                          │
│   + isServerSelected(): boolean                                             │
│   + getSelectedProfile(): ProfileEntry                                      │
│   + updateButtonStates()                                                    │
│   + renderCategoryPresets(DrawContext)                                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Sub-Panel Classes (from 03_PARAMETERS.md)

### 4.1 ShapeSubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ShapeSubPanel                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│ Dynamic controls based on selected shape type:                              │
│                                                                             │
│ SPHERE (§4.1):                                                              │
│   • radius: LabeledSlider (0.01-∞)                                          │
│   • latSteps: LabeledSlider (2-512)                                         │
│   • lonSteps: LabeledSlider (4-1024)                                        │
│   • latStart: LabeledSlider (0-1)                                           │
│   • latEnd: LabeledSlider (0-1)                                             │
│   • algorithm: EnumDropdown (LAT_LON, TYPE_A, TYPE_E)                       │
│                                                                             │
│ RING (§4.2):                                                                │
│   • innerRadius: LabeledSlider (0-∞)                                        │
│   • outerRadius: LabeledSlider (0-∞)                                        │
│   • segments: LabeledSlider (3-1024)                                        │
│   • y: LabeledSlider (-∞-∞)                                                 │
│   • height: LabeledSlider (0-∞)  ← 3D ring                                  │
│                                                                             │
│ DISC (§4.3):                                                                │
│   • radius: LabeledSlider (0.01-∞)                                          │
│   • segments: LabeledSlider (3-1024)                                        │
│   • y: LabeledSlider (-∞-∞)                                                 │
│   • innerRadius: LabeledSlider (0-∞)  ← ring-like cutout                    │
│                                                                             │
│ PRISM (§4.4):                                                               │
│   • sides: LabeledSlider (3-64)                                             │
│   • radius: LabeledSlider (0.01-∞)                                          │
│   • height: LabeledSlider (0.01-∞)                                          │
│   • topRadius: LabeledSlider (0-∞)  ← tapered                               │
│   • capTop: Toggle                                                          │
│   • capBottom: Toggle                                                       │
│                                                                             │
│ CYLINDER (§4.6):                                                            │
│   • radius: LabeledSlider (0.01-∞)                                          │
│   • height: LabeledSlider (0.01-∞)                                          │
│   • segments: LabeledSlider (3-128)                                         │
│   • topRadius: LabeledSlider (0-∞)  ← cone-like                             │
│   • capTop: Toggle                                                          │
│   • capBottom: Toggle                                                       │
│                                                                             │
│ POLYHEDRON (§4.5):                                                          │
│   • polyType: EnumDropdown (CUBE, OCTAHEDRON, ICOSAHEDRON, etc.)            │
│   • radius: LabeledSlider (0.01-∞)                                          │
│   • subdivisions: LabeledSlider (0-5)                                       │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 AppearanceSubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         AppearanceSubPanel                                   │
│                         (§9 Appearance Level)                                │
├─────────────────────────────────────────────────────────────────────────────┤
│ Controls:                                                                   │
│   • color: ColorButton + hex input                                          │
│   • alpha: LabeledSlider (0.0-1.0) OR RangeSlider for { min, max }          │
│   • glow: LabeledSlider (0.0-1.0)                                           │
│   • emissive: LabeledSlider (0.0-1.0)                                       │
│   • saturation: LabeledSlider (0.0-2.0)                                     │
│   • brightness: LabeledSlider (0.0-2.0)                                     │
│   • hueShift: LabeledSlider (0-360)                                         │
│   • secondaryColor: ColorButton (optional)                                  │
│   • colorBlend: LabeledSlider (0.0-1.0)                                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.3 AnimationSubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         AnimationSubPanel                                    │
│                         (§10 Animation Level)                                │
├─────────────────────────────────────────────────────────────────────────────┤
│ SPIN:                                                                       │
│   • spin.axis: EnumDropdown (X, Y, Z) OR Vec3Editor for custom              │
│   • spin.speed: LabeledSlider (-0.5 to 0.5)                                 │
│   • spin.oscillate: Toggle                                                  │
│   • spin.range: LabeledSlider (0-360) if oscillate                          │
│                                                                             │
│ PULSE:                                                                      │
│   • pulse.enabled: Toggle                                                   │
│   • pulse.scale: LabeledSlider (0.0-1.0)                                    │
│   • pulse.speed: LabeledSlider (0.1-10.0)                                   │
│   • pulse.waveform: EnumDropdown (SINE, SQUARE, TRIANGLE, SAWTOOTH)         │
│   • pulse.min: LabeledSlider (0.0-2.0)                                      │
│   • pulse.max: LabeledSlider (0.0-2.0)                                      │
│                                                                             │
│ ALPHA PULSE:                                                                │
│   • alphaPulse.enabled: Toggle                                              │
│   • alphaPulse.speed: LabeledSlider (0.1-10.0)                              │
│   • alphaPulse.min: LabeledSlider (0.0-1.0)                                 │
│   • alphaPulse.max: LabeledSlider (0.0-1.0)                                 │
│   • alphaPulse.waveform: EnumDropdown                                       │
│                                                                             │
│ PHASE:                                                                      │
│   • phase: LabeledSlider (0.0-1.0)                                          │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.4 TransformSubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         TransformSubPanel                                    │
│                         (§5 Transform Level)                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│ POSITION:                                                                   │
│   • anchor: EnumDropdown (CENTER, FEET, HEAD, ABOVE, BELOW, etc.)           │
│   • offset: Vec3Editor (x, y, z)                                            │
│                                                                             │
│ ROTATION:                                                                   │
│   • rotation: Vec3Editor (degrees)                                          │
│   • inheritRotation: Toggle                                                 │
│                                                                             │
│ SCALE:                                                                      │
│   • scale: LabeledSlider (0.01-10.0)                                        │
│   • scaleXYZ: Vec3Editor (optional, for non-uniform)                        │
│   • scaleWithRadius: Toggle                                                 │
│                                                                             │
│ ORIENTATION:                                                                │
│   • facing: EnumDropdown (FIXED, PLAYER_LOOK, VELOCITY, CAMERA)             │
│   • up: EnumDropdown (WORLD_UP, PLAYER_UP, VELOCITY, CUSTOM)                │
│   • billboard: EnumDropdown (NONE, FULL, Y_AXIS)                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.5 VisibilitySubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        VisibilitySubPanel                                    │
│                        (§7 Visibility Mask Level)                            │
├─────────────────────────────────────────────────────────────────────────────┤
│ MASK TYPE:                                                                  │
│   • mask: EnumDropdown (FULL, BANDS, STRIPES, CHECKER, RADIAL, GRADIENT)    │
│                                                                             │
│ COMMON:                                                                     │
│   • count: LabeledSlider (1-32)                                             │
│   • thickness: LabeledSlider (0.0-1.0)                                      │
│   • offset: LabeledSlider (0.0-1.0)                                         │
│   • invert: Toggle                                                          │
│   • feather: LabeledSlider (0.0-1.0)                                        │
│   • animate: Toggle                                                         │
│   • animateSpeed: LabeledSlider (0.1-10.0)                                  │
│                                                                             │
│ GRADIENT (when mask=GRADIENT):                                              │
│   • direction: EnumDropdown (VERTICAL, HORIZONTAL, RADIAL)                  │
│   • falloff: EnumDropdown (LINEAR, EASE, SMOOTH)                            │
│   • start: LabeledSlider (0.0-1.0)                                          │
│   • end: LabeledSlider (0.0-1.0)                                            │
│                                                                             │
│ RADIAL (when mask=RADIAL):                                                  │
│   • centerX: LabeledSlider (0.0-1.0)                                        │
│   • centerY: LabeledSlider (0.0-1.0)                                        │
│   • falloff: EnumDropdown (LINEAR, EASE, SMOOTH)                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.6 ArrangementSubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       ArrangementSubPanel                                    │
│                       (§8 Arrangement Level)                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│ SIMPLE MODE:                                                                │
│   • arrangement: PatternDropdown (filtered by CellType)                     │
│                                                                             │
│ MULTI-PART MODE:                                                            │
│   • arrangement.default: PatternDropdown                                    │
│   • arrangement.caps: PatternDropdown (for prism/cylinder)                  │
│   • arrangement.sides: PatternDropdown (for prism/cylinder)                 │
│   • arrangement.edges: PatternDropdown                                      │
│   • arrangement.poles: PatternDropdown (for sphere)                         │
│   • arrangement.equator: PatternDropdown (for sphere)                       │
│                                                                             │
│ SHUFFLE (Debug only):                                                       │
│   • shuffle: Toggle                                                         │
│   • shuffleIndex: LabeledSlider (0-N)                                       │
│   • [Shuffle!] button                                                       │
│                                                                             │
│ AVAILABLE PATTERNS (per CellType):                                          │
│   QUAD: filled_1, triangle_1-4, wave_1, tooth_1, parallelogram_1-2, etc.    │
│   SEGMENT: full, alternating, sparse, quarter, zigzag, dashed              │
│   SECTOR: full, half, quarters, pinwheel, trisector, spiral, crosshair     │
│   EDGE: full, latitude, longitude, sparse, minimal, dashed, grid           │
│   TRIANGLE: full, alternating, inverted, sparse, fan, radial               │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.7 FillSubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           FillSubPanel                                       │
│                           (§6 Fill Level)                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ FILL MODE:                                                                  │
│   • mode: EnumDropdown (SOLID, WIREFRAME, CAGE, POINTS)                     │
│   • wireThickness: LabeledSlider (0.1-5.0)                                  │
│   • doubleSided: Toggle                                                     │
│   • depthTest: Toggle                                                       │
│   • depthWrite: Toggle                                                      │
│                                                                             │
│ CAGE-SPECIFIC (when mode=CAGE):                                             │
│   • latitudeCount: LabeledSlider (1-32)                                     │
│   • longitudeCount: LabeledSlider (1-64)                                    │
│   • showEquator: Toggle                                                     │
│   • showPoles: Toggle                                                       │
│                                                                             │
│ POINTS-SPECIFIC (when mode=POINTS):                                         │
│   • pointSize: LabeledSlider (1.0-10.0)                                     │
│   • pointShape: EnumDropdown (CIRCLE, SQUARE, STAR)  ← future               │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.8 LinkingSubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          LinkingSubPanel                                     │
│                          (§11 Primitive Linking)                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ PRIMITIVE ID:                                                               │
│   • id: TextInput (required for linking)                                    │
│                                                                             │
│ LINK OPTIONS:                                                               │
│   • radiusMatch: PrimitiveDropdown (list of other primitives by ID)         │
│   • radiusOffset: LabeledSlider (-10.0 to 10.0)                             │
│   • follow: PrimitiveDropdown                                               │
│   • mirror: EnumDropdown (NONE, X, Y, Z)                                    │
│   • phaseOffset: LabeledSlider (0.0-1.0)                                    │
│   • scaleWith: PrimitiveDropdown                                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.9 PredictionSubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        PredictionSubPanel                                    │
│                        (§1 Prediction Block)                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│ PRESETS:                                                                    │
│   • preset: EnumDropdown (OFF, LOW, MEDIUM, HIGH, CUSTOM)                   │
│     OFF: enabled=false                                                      │
│     LOW: leadTicks=1, maxDistance=4, lookAhead=0.2                          │
│     MEDIUM: leadTicks=2, maxDistance=8, lookAhead=0.5 (default)             │
│     HIGH: leadTicks=3, maxDistance=12, lookAhead=0.8                        │
│     CUSTOM: show all sliders                                                │
│                                                                             │
│ CUSTOM (when preset=CUSTOM):                                                │
│   • enabled: Toggle                                                         │
│   • leadTicks: LabeledSlider (1-10)                                         │
│   • maxDistance: LabeledSlider (1.0-50.0)                                   │
│   • lookAhead: LabeledSlider (0.0-1.0)                                      │
│   • verticalBoost: LabeledSlider (0.0-2.0)                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.10 FollowModeSubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        FollowModeSubPanel                                    │
│                        (§1 Follow Mode)                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│ CONTROLS:                                                                   │
│   • enabled: Toggle (false = static field)                                  │
│   • mode: EnumDropdown (SNAP, SMOOTH, GLIDE)                                │
│                                                                             │
│ DESCRIPTIONS:                                                               │
│   SNAP: Field instantly follows player position                             │
│   SMOOTH: Field smoothly interpolates to player position                    │
│   GLIDE: Field has inertia, glides behind player                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Debug Sub-Panels (Level 3)

### 5.1 BindingsSubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BindingsSubPanel                                     │
│                         (§12.1 Bindings)                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ BINDING LIST:                                                               │
│   ┌───────────────────────────────────────────────────────────────────────┐ │
│   │ Property            Source              Input     Output              │ │
│   ├───────────────────────────────────────────────────────────────────────┤ │
│   │ alpha              player.health_pct    [0-1]    [0.3-1.0]   [X]     │ │
│   │ glow               player.in_combat     [0-1]    [0-0.8]     [X]     │ │
│   │ scale              player.speed         [0-10]   [1.0-1.5]   [X]     │ │
│   └───────────────────────────────────────────────────────────────────────┘ │
│   [+ Add Binding]                                                           │
│                                                                             │
│ ADD BINDING DIALOG:                                                         │
│   • property: TextInput (path like "layers[0].alpha")                       │
│   • source: EnumDropdown (from §12.1 Available Binding Sources)             │
│   • inputRange: RangeSlider                                                 │
│   • outputRange: RangeSlider                                                │
│   • curve: EnumDropdown (LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT)            │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 TriggersSubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         TriggersSubPanel                                     │
│                         (§12.2 Triggers)                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ TRIGGER LIST:                                                               │
│   ┌───────────────────────────────────────────────────────────────────────┐ │
│   │ Event              Effect         Duration   Params         [X]      │ │
│   ├───────────────────────────────────────────────────────────────────────┤ │
│   │ PLAYER_DAMAGE      FLASH          10 ticks   color=#FF0000  [X]      │ │
│   │ PLAYER_HEAL        GLOW           20 ticks   intensity=0.8  [X]      │ │
│   │ FIELD_SPAWN        PULSE          30 ticks   scale=1.5      [X]      │ │
│   └───────────────────────────────────────────────────────────────────────┘ │
│   [+ Add Trigger]                                                           │
│                                                                             │
│ ADD TRIGGER DIALOG:                                                         │
│   • event: EnumDropdown (PLAYER_DAMAGE, PLAYER_HEAL, PLAYER_DEATH, etc.)    │
│   • effect: EnumDropdown (FLASH, PULSE, SHAKE, GLOW, COLOR_SHIFT)           │
│   • duration: LabeledSlider (1-100 ticks)                                   │
│   • params: Dynamic based on effect                                         │
│     - FLASH/COLOR_SHIFT: ColorButton                                        │
│     - PULSE: scale slider                                                   │
│     - SHAKE: amplitude slider                                               │
│     - GLOW: intensity slider                                                │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.3 LifecycleSubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         LifecycleSubPanel                                    │
│                         (§12.3 Lifecycle)                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ FADE:                                                                       │
│   • fadeIn: LabeledSlider (0-100 ticks)                                     │
│   • fadeOut: LabeledSlider (0-100 ticks)                                    │
│                                                                             │
│ SCALE:                                                                      │
│   • scaleIn: LabeledSlider (0-100 ticks)                                    │
│   • scaleOut: LabeledSlider (0-100 ticks)                                   │
│                                                                             │
│ DECAY:                                                                      │
│   • decayEnabled: Toggle                                                    │
│   • decayRate: LabeledSlider (0.001-0.1) per tick                           │
│   • decayMin: LabeledSlider (0.0-1.0)                                       │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.4 BeamSubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          BeamSubPanel                                        │
│                          (Debug: Central Beam)                               │
├─────────────────────────────────────────────────────────────────────────────┤
│ PRESETS:                                                                    │
│   • preset: EnumDropdown (DEFAULT, SUBTLE, INTENSE, PULSING, CUSTOM)        │
│                                                                             │
│ BASIC:                                                                      │
│   • enabled: Toggle                                                         │
│   • innerRadius: LabeledSlider (0.0-1.0)                                    │
│   • outerRadius: LabeledSlider (0.1-2.0)                                    │
│   • height: LabeledSlider (0.1-10.0)                                        │
│   • glow: LabeledSlider (0.0-1.0)                                           │
│   • color: ColorButton                                                      │
│                                                                             │
│ PULSE:                                                                      │
│   • pulseEnabled: Toggle                                                    │
│   • pulseScale: LabeledSlider (0.0-1.0)                                     │
│   • pulseSpeed: LabeledSlider (0.1-5.0)                                     │
│   • pulseWaveform: EnumDropdown (SINE, SQUARE, TRIANGLE, SAWTOOTH)          │
│   • pulseMin: LabeledSlider (0.0-1.0)                                       │
│   • pulseMax: LabeledSlider (0.5-2.0)                                       │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Widget Classes

### 6.1 LabeledSlider

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          LabeledSlider                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ Fields:                                                                     │
│   - label: String                                                           │
│   - value: float                                                            │
│   - min: float                                                              │
│   - max: float                                                              │
│   - step: float                                                             │
│   - format: String (e.g., "%.2f", "%d")                                     │
│   - tooltip: String                                                         │
│   - onChange: Consumer<Float>                                               │
├─────────────────────────────────────────────────────────────────────────────┤
│ UI:                                                                         │
│   ┌──────────────────────────────────────┐                                  │
│   │ Spin Speed         [====●=====] 0.02 │                                  │
│   └──────────────────────────────────────┘                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ Methods:                                                                    │
│   + setValue(float)                                                         │
│   + getValue(): float                                                       │
│   + setEnabled(boolean)                                                     │
│   + render(DrawContext)                                                     │
│   + mouseClicked(double, double, int): boolean                              │
│   + mouseDragged(double, double, int, double, double): boolean              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 RangeSlider

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           RangeSlider                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│ For min/max ranges (e.g., alpha: { min: 0.3, max: 1.0 })                    │
│                                                                             │
│ UI:                                                                         │
│   ┌──────────────────────────────────────┐                                  │
│   │ Alpha Range     [==●====●===] 0.3-1.0│                                  │
│   └──────────────────────────────────────┘                                  │
│                        ↑      ↑                                             │
│                       min    max                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│ Fields:                                                                     │
│   - minValue: float                                                         │
│   - maxValue: float                                                         │
│   - rangeMin: float (overall min)                                           │
│   - rangeMax: float (overall max)                                           │
│   - onChange: BiConsumer<Float, Float>                                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.3 EnumDropdown

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          EnumDropdown<E>                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ UI:                                                                         │
│   ┌──────────────────────────────────────┐                                  │
│   │ Fill Mode    [WIREFRAME        ▼]    │                                  │
│   │              ┌───────────────────┐   │                                  │
│   │              │ SOLID            │   │  ← dropdown                       │
│   │              │ WIREFRAME    ✓   │   │                                  │
│   │              │ CAGE             │   │                                  │
│   │              │ POINTS           │   │                                  │
│   │              └───────────────────┘   │                                  │
│   └──────────────────────────────────────┘                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ Fields:                                                                     │
│   - label: String                                                           │
│   - enumClass: Class<E>                                                     │
│   - selected: E                                                             │
│   - displayNames: Map<E, String> (optional)                                 │
│   - onChange: Consumer<E>                                                   │
│   - expanded: boolean                                                       │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.4 ColorButton

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ColorButton                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│ UI:                                                                         │
│   ┌──────────────────────────────────────┐                                  │
│   │ Color    [■■■] #4488FF  [Edit]       │                                  │
│   └──────────────────────────────────────┘                                  │
│                                                                             │
│ On click, opens color input popup:                                          │
│   ┌─────────────────────────────────────┐                                   │
│   │ Enter color:                        │                                   │
│   │ [#4488FF________________]           │                                   │
│   │ OR select from theme:               │                                   │
│   │ [@primary] [@secondary] [@accent]   │                                   │
│   │ [OK] [Cancel]                       │                                   │
│   └─────────────────────────────────────┘                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│ Fields:                                                                     │
│   - color: int (ARGB)                                                       │
│   - colorRef: String (nullable, e.g., "@primary")                           │
│   - onChange: Consumer<String>  ← hex or ref                                │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.5 Vec3Editor

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Vec3Editor                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│ UI:                                                                         │
│   ┌──────────────────────────────────────┐                                  │
│   │ Offset   X:[0.0] Y:[1.5] Z:[0.0]     │                                  │
│   └──────────────────────────────────────┘                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ Fields:                                                                     │
│   - x, y, z: float                                                          │
│   - min, max: float (per axis)                                              │
│   - onChange: Consumer<Vec3d>                                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.6 ExpandableSection

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        ExpandableSection                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ UI (collapsed):                                                             │
│   ┌──────────────────────────────────────┐                                  │
│   │ ▸ Advanced Transform                 │                                  │
│   └──────────────────────────────────────┘                                  │
│                                                                             │
│ UI (expanded):                                                              │
│   ┌──────────────────────────────────────┐                                  │
│   │ ▾ Advanced Transform                 │                                  │
│   ├──────────────────────────────────────┤                                  │
│   │   [content widgets here]             │                                  │
│   └──────────────────────────────────────┘                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ Fields:                                                                     │
│   - title: String                                                           │
│   - expanded: boolean                                                       │
│   - content: List<Widget>                                                   │
│   - onToggle: Consumer<Boolean>                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```


---

## 6.7 BottomActionBar

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          BottomActionBar                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ Global bottom action bar (hidden on Profiles tab).                          │
│ Includes profile dropdown and preset two-tier selection.                    │
│                                                                             │
│ Layout:                                                                     │
│   ┌────────────────────────────────────────────────────────────────────┐   │
│   │ PRESETS                    │ PROFILE                              │   │
│   │ [Category ▼] [Preset ▼]    │ [Profile ▼] [SAVE] [REVERT]          │   │
│   └────────────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────────────┤
│ Fields:                                                                     │
│   - presetCategoryDropdown: CyclingButtonWidget                             │
│   - presetDropdown: CyclingButtonWidget                                     │
│   - profileDropdown: CyclingButtonWidget                                    │
│   - saveButton, revertButton: ButtonWidget                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.8 ToastNotification

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ToastNotification                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ Animated toast messages for user feedback.                                  │
│                                                                             │
│ Types: SUCCESS (green), INFO (blue), WARNING (yellow), ERROR (red)          │
│                                                                             │
│ Static methods:                                                             │
│   + success(String message)                                                 │
│   + info(String message)                                                    │
│   + warning(String message)                                                 │
│   + error(String message)                                                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.9 ConfirmDialog

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          ConfirmDialog                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ Modal confirmation popup for destructive actions.                           │
│                                                                             │
│ Fields:                                                                     │
│   - title: String                                                           │
│   - message: String                                                         │
│   - onConfirm: Runnable                                                     │
│   - onCancel: Runnable                                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│ Static:                                                                     │
│   + show(String title, String message, Runnable onConfirm)                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6A. State Utilities

### 6A.1 StateAccessor

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          StateAccessor                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ Reflection-based accessor for FieldEditState paths.                         │
│ Enables state.set("path.to.field", value) and state.get("path.to.field")    │
│                                                                             │
│ Supports:                                                                   │
│   - Dot notation: "spin.speed", "orbit.radius"                              │
│   - Array indices: "layers[0].primitives[1].fill.mode"                      │
│   - @StateField annotations for path validation                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ Methods:                                                                    │
│   + set(Object target, String path, Object value)                           │
│   + get(Object target, String path): Object                                 │
│   + getType(Object target, String path): Class<?>                           │
│   + listPaths(Object target): List<String>                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6A.2 FieldEditStateHolder

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       FieldEditStateHolder                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│ Singleton holder for the current FieldEditState instance.                   │
│ Used by commands and network handlers to access GUI state.                  │
│                                                                             │
│ Static methods:                                                             │
│   + getInstance(): FieldEditState                                           │
│   + setInstance(FieldEditState)                                             │
│   + hasInstance(): boolean                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6B. Additional Sub-Panels

### 6B.1 ModifiersSubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        ModifiersSubPanel                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ Controls for visual modifiers:                                              │
│   • bobbing: LabeledSlider (0-1)                                            │
│   • breathing: LabeledSlider (0-1)                                          │
│   • alphaMultiplier: LabeledSlider (0-1)                                    │
│   • tiltMultiplier: LabeledSlider (0-1)                                     │
│   • swirlStrength: LabeledSlider (0-1)                                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6B.2 OrbitSubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          OrbitSubPanel                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ Controls for orbit configuration:                                           │
│   • enabled: Toggle                                                         │
│   • radius: LabeledSlider (0.1-10)                                          │
│   • speed: LabeledSlider (0-2)                                              │
│   • axis: EnumDropdown (X, Y, Z, CUSTOM)                                    │
│   • offset: Vec3Editor                                                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Network Classes

### 7.1 Packet Records

```java
// Client → Server: Open GUI request
public record FieldGuiOpenC2S() {}

// Server → Client: Current definition + defaults list
public record FieldGuiDataS2C(
    FieldDefinition currentDefinition,
    List<String> serverDefaultNames,
    boolean debugMenuAllowed
) {}

// Client → Server: Apply definition changes
public record FieldUpdateC2S(
    FieldDefinition definition,
    boolean applyToActiveField  // true = also update gameplay field
) {}

// Server → Client: List of server profile names
public record FieldProfileListS2C(
    List<ProfileEntry> profiles
) {
    public record ProfileEntry(String name, String description) {}
}

// Client → Server: Request server profile
public record FieldProfileRequestC2S(String profileName) {}

// Server → Client: Profile JSON data
public record FieldProfileDataS2C(
    String profileName,
    FieldDefinition definition
) {}
```

---

## 8. Profile Classes

### 8.1 Profile Record

```java
public record Profile(
    int version,            // Schema version
    String name,            // Profile name
    String description,     // User description
    Instant created,        // Creation timestamp
    Instant modified,       // Last modified
    FieldDefinition definition
) {
    public static Profile fromJson(JsonObject json);
    public JsonObject toJson();
}
```

### 8.2 ProfileManager

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          ProfileManager                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│ Fields:                                                                     │
│   - profileDir: Path                    # .minecraft/config/thevirusblock/  │
│   - profiles: Map<String, Profile>      # Loaded profiles                   │
├─────────────────────────────────────────────────────────────────────────────┤
│ Methods:                                                                    │
│   + loadProfiles()                      # Scan directory                    │
│   + getProfileNames(): List<String>                                         │
│   + getProfile(String): Profile                                             │
│   + saveProfile(Profile)                # Write to disk                     │
│   + deleteProfile(String)               # Remove file                       │
│   + renameProfile(String, String)       # Rename file                       │
│   + exportProfile(String, Path)         # Copy to location                  │
│   + importProfile(Path): Profile        # Load from location                │
│   + createBackup(String)                # Backup before save                │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 9. Enums

```java
// Tab navigation
public enum TabType {
    QUICK, ADVANCED, DEBUG, PROFILES
}

// Prediction presets for Quick Customize
public enum PredictionPreset {
    OFF(false, 0, 0, 0, 0),
    LOW(true, 1, 4.0f, 0.2f, 0.0f),
    MEDIUM(true, 2, 8.0f, 0.5f, 0.0f),
    HIGH(true, 3, 12.0f, 0.8f, 0.0f),
    CUSTOM(true, 2, 8.0f, 0.5f, 0.0f);
    
    public final boolean enabled;
    public final int leadTicks;
    public final float maxDistance;
    public final float lookAhead;
    public final float verticalBoost;
}
```

---

## 10. Class Count Summary

| Category | Count | Classes |
|----------|-------|---------|
| Screen | 1 | FieldCustomizerScreen |
| State | 3 | FieldEditState, EditorState, UndoManager |
| Panels | 6 | Quick, Advanced, Debug, Layer, Primitive, Profiles |
| Sub-Panels | 14 | Shape, Appearance, Animation, Transform, Visibility, Arrangement, Fill, Linking, Prediction, FollowMode, Bindings, Triggers, Lifecycle, **Beam** |
| Widgets | 9 | LabeledSlider, RangeSlider, EnumDropdown, ColorButton, Vec3Editor, ExpandableSection, TooltipWrapper, ActionButton, ColorPicker |
| **Utilities** | **5** | **GuiWidgets, GuiAnimations, GuiLayout, GuiConstants, PresetRegistry** |
| Network | 6 | Packets |
| Profile | 3 | Profile, ProfileManager, ProfileValidator |
| **Total** | **~50** | (+3 enums, updated Profile, PresetRegistry, BottomActionBar, ProfilesPanel) |

---

## 10.1 Shared Utility Classes

These utilities live outside the GUI package but are used by it:

| Class | Package | Purpose |
|-------|---------|---------|
| `FieldMath` | `visual.util` | Math utilities (lerp, smoothStep, catmullRom, etc.) |
| `FieldColor` | `visual.util` | Color manipulation (lerp, mix, withAlpha, etc.) |

---

## 11. Dependencies

```
FieldCustomizerScreen
    ├── FieldEditState
    │   ├── EditorState
    │   ├── UndoManager
    │   └── FieldDefinition (from field system)
    │
    ├── QuickPanel
    │   └── (widgets)
    │
    ├── AdvancedPanel
    │   ├── ShapeSubPanel
    │   ├── AppearanceSubPanel
    │   ├── AnimationSubPanel
    │   ├── TransformSubPanel
    │   ├── VisibilitySubPanel
    │   ├── ArrangementSubPanel
    │   ├── FillSubPanel
    │   ├── LinkingSubPanel
    │   ├── PredictionSubPanel
    │   └── FollowModeSubPanel
    │
    ├── DebugPanel
    │   ├── BindingsSubPanel
    │   ├── TriggersSubPanel
    │   └── LifecycleSubPanel
    │
    ├── LayerPanel
    ├── ProfilePanel
    │   └── ProfileManager
    │
    └── Network Packets
```

---

## 12. Related Documents

- [GUI_ARCHITECTURE.md](./GUI_ARCHITECTURE.md) - Design principles and flow
- [GUI_DESIGN.md](./GUI_DESIGN.md) - Visual mockups
- [03_PARAMETERS.md](../03_PARAMETERS.md) - Parameter reference

---


---

## 13. Category & Organization Enums

### 13.1 PresetCategory

```java
/**
 * Categories for organizing presets in the GUI.
 * Used for two-tier dropdown: [Category ▼] [Preset ▼]
 */
public enum PresetCategory {
    ADDITIVE("Additive", "Add elements to field"),      // Add rings, layers, etc.
    STYLE("Style", "Visual style changes"),             // Wireframe, solid, etc.
    ANIMATION("Animation", "Motion effects"),           // Spin, pulse, etc.
    EFFECT("Effect", "Composite effects"),              // Combat ready, stealth, etc.
    PERFORMANCE("Performance", "Detail level changes"); // Low/high detail

    private final String displayName;
    private final String description;
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
```

### 13.2 ProfileCategory

```java
/**
 * Categories for organizing profiles in the GUI.
 * Used for filtering in Profiles tab.
 */
public enum ProfileCategory {
    COMBAT("Combat", "For battle situations"),
    UTILITY("Utility", "Functional/practical"),
    DECORATIVE("Decorative", "Visual only"),
    EXPERIMENTAL("Experimental", "Testing/WIP");

    private final String displayName;
    private final String description;
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
```

### 13.3 ProfileSource

```java
/**
 * Source/origin of a profile.
 * Determines editability and storage location.
 */
public enum ProfileSource {
    BUNDLED("Bundled", false),   // Shipped with mod, read-only
    LOCAL("Local", true),        // User-created, editable
    SERVER("Server", false);     // From server, read-only

    private final String displayName;
    private final boolean editable;
    
    public String getDisplayName() { return displayName; }
    public boolean isEditable() { return editable; }
}
```

---

## 14. Updated Profile Record

```java
public record Profile(
    int version,                  // Schema version
    String name,                  // Profile name
    String description,           // User description
    FieldType type,               // Functional type (SHIELD, PERSONAL, etc.)
    ProfileCategory category,     // Organizational category
    List<String> tags,            // Additional tags for filtering
    ProfileSource source,         // Where it came from
    Instant created,              // Creation timestamp
    Instant modified,             // Last modified
    FieldDefinition definition
) {
    public static Profile fromJson(JsonObject json);
    public JsonObject toJson();
    
    /** Display format: "Profile Name (category)" */
    public String getDisplayName() {
        return name + " (" + category.getDisplayName().toLowerCase() + ")";
    }
}
```

---

## 15. Updated PresetRegistry

```java
/**
 * Registry for multi-scope presets organized by category.
 * Loads from: config/the-virus-block/field_presets/{category}/
 */
public class PresetRegistry {
    
    private static final Map<PresetCategory, List<PresetEntry>> presetsByCategory = new EnumMap<>();
    
    public record PresetEntry(
        String id,
        String name,
        String description,
        PresetCategory category,
        JsonObject mergeData
    ) {}
    
    /** Load all presets from disk, organized by category folders */
    public static void loadAll();
    
    /** Get all categories that have presets */
    public static List<PresetCategory> getCategories();
    
    /** Get presets for a specific category */
    public static List<PresetEntry> getPresets(PresetCategory category);
    
    /** Apply a preset to the current state (merges, doesn't replace) */
    public static void applyPreset(FieldEditState state, String presetId);
    
    /** Get affected categories for confirmation dialog */
    public static List<String> getAffectedCategories(String presetId);
}
```

---

## 16. Updated BottomActionBar

```java
/**
 * Global bottom action bar (hidden on Profiles tab).
 * Now includes two-tier preset selection.
 */
public class BottomActionBar {
    
    // Preset selection (two-tier)
    private CyclingButtonWidget<PresetCategory> presetCategoryDropdown;
    private CyclingButtonWidget<String> presetDropdown;
    
    // Profile selection
    private CyclingButtonWidget<String> profileDropdown;
    private ButtonWidget saveButton;
    private ButtonWidget revertButton;
    
    /** Update preset dropdown when category changes */
    private void onPresetCategoryChanged(PresetCategory category);
    
    /** Show confirmation dialog when preset selected */
    private void onPresetSelected(String presetId);
    
    /** Update button states based on dirty status */
    private void updateButtonStates();
}
```

**Bottom Action Bar Layout:**
```
┌────────────────────────────────────────────────────────────────────────┐
│ PRESETS                           │ PROFILE                            │
│ [Additive ▼] [Add Inner Ring ▼]   │ [My Shield (combat) ▼] [SAVE][REV] │
│  ↑ Category    ↑ Preset           │  ↑ Name (category)                 │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 17. Updated ProfilesPanel

```java
/**
 * Full profile management panel with filtering.
 */
public class ProfilesPanel {
    
    // Filters
    private CyclingButtonWidget<ProfileSource> sourceFilter;    // All, Bundled, Local, Server
    private CyclingButtonWidget<ProfileCategory> categoryFilter; // All, Combat, Utility, etc.
    private TextFieldWidget searchField;
    
    // Profile list
    private List<Profile> allProfiles;
    private List<Profile> filteredProfiles;
    private int selectedIndex;
    
    /** Apply filters and update visible list */
    private void applyFilters();
    
    /** Render profile entry with format: "Name (category)" */
    private void renderProfileEntry(Profile profile, int y, boolean selected);
    
    /** Get icon for source (🔒 for read-only, ✎ for editable) */
    private String getSourceIcon(ProfileSource source);
}
```

**Profiles Panel Layout:**
```
┌─────────────────────────────────────────────────────────────────┐
│ Source: [All      ▼]    Category: [All      ▼]    [🔍 ______]  │
├─────────────────────────────────────────────────────────────────┤
│ ── BUNDLED ──                                                   │
│   ○ Default Shield (utility)                                    │
│   ○ Showcase Animated (decorative)                              │
│ ── LOCAL ──                                                     │
│   ● My Combat Shield (combat) ✎                                 │
│   ○ Test Wireframe (experimental) ✎                             │
│ ── SERVER ──                                                    │
│   ○ Server Default (utility) 🔒                                 │
├─────────────────────────────────────────────────────────────────┤
│ [Load] [Save] [Save As] [Rename] [Duplicate] [Delete] [Export]  │
└─────────────────────────────────────────────────────────────────┘
```


*Draft v1 - Maps to 03_PARAMETERS.md v5.1*

