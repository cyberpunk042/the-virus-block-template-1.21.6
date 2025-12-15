# GUI Class Diagram

> **Updated:** December 14, 2025  
> **Total Files:** 107

---

## 1. Core Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              FieldCustomizerScreen                                       │
│                               extends Screen                                             │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│  Pattern: ORCHESTRATOR - delegates to components, layout, and state                     │
│                                                                                         │
│  Composition:                                                                           │
│    state ─────────────────► FieldEditState                                              │
│    layout ────────────────► LayoutManager (Strategy Pattern)                            │
│    components ────────────► ScreenComponent[] (Composition)                             │
│    panels ────────────────► AbstractPanel subclasses                                    │
│    modals ────────────────► ModalDialog (via ModalFactory)                              │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Layout Strategy Pattern

```
                              «interface»
                            LayoutManager
                    ┌─────────────────────────────┐
                    │ + calculate(w, h)           │
                    │ + getMode(): GuiMode        │
                    │ + getPanelBounds(): Bounds  │
                    │ + getContentBounds(): Bounds│
                    │ + renderBackground(...)     │
                    │ + renderFrame(...)          │
                    └─────────────────────────────┘
                               △
                               │
              ┌────────────────┴────────────────┐
              │                                 │
    ┌─────────────────────┐          ┌─────────────────────┐
    │   FullscreenLayout  │          │   WindowedLayout    │
    ├─────────────────────┤          ├─────────────────────┤
    │ Uses: GridPane      │          │ Uses: SidePanel     │
    │ (2x2 grid layout)   │          │ (overlay panel)     │
    │ hasPreviewWidget=T  │          │ hasPreviewWidget=F  │
    └─────────────────────┘          └─────────────────────┘
              │
              ▼
    ┌─────────────────────┐
    │      GridPane       │
    ├─────────────────────┤
    │ + topLeft(): Bounds │
    │ + topRight(): Bounds│
    │ + bottomLeft()      │
    │ + bottomRight()     │
    │ + getSpan(...)      │
    └─────────────────────┘
```

**Supporting Classes:**
- `Bounds` - 2D rectangle (x, y, width, height)
- `GuiMode` - Enum: FULLSCREEN, WINDOWED
- `StatusBar` - Status message display
- `LayoutFactory` - Creates layout instances
- `LayoutPanel` - Base for v2 panels (uses DirectionalLayoutWidget)
- `SidePanel` - Windowed mode panel

---

## 3. Component Composition Pattern

```
                           «interface»
                         ScreenComponent
                    ┌─────────────────────────────┐
                    │ + getWidgets(): List<Widget>│
                    │ + setBounds(Bounds)         │
                    │ + render(...)               │
                    │ + tick()                    │
                    │ + mouseScrolled(...)        │
                    └─────────────────────────────┘
                               △
                               │
    ┌──────────────┬───────────┼───────────┬──────────────┐
    │              │           │           │              │
┌────────┐   ┌──────────┐  ┌────────────┐  ┌───────────┐  ┌──────────────┐
│HeaderBar│  │  TabBar  │  │SelectorBar │  │ContentArea│  │VisibilityCtr│
├────────┤   ├──────────┤  ├────────────┤  ├───────────┤  ├──────────────┤
│modeBtn │   │quickTab  │  │layerDropdn │  │quickSubs  │  │state, mode   │
│fieldBtn│   │advancedT │  │primitiveD  │  │advancedS  │  │isStandardMode│
│resetBtn│   │debugTab  │  │addButtons  │  │debugSubs  │  │isDebugVisible│
│closeBtn│   │profilesT │  │editButtons │  │factory    │  └──────────────┘
└────────┘   │presetDrp │  └────────────┘  └───────────┘
             │rendererT │
             └──────────┘
```

**ContentProviderFactory:**
```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                          ContentProviderFactory                                          │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│ Creates SubTabPane.ContentProvider instances by wrapping sub-panels:                    │
│                                                                                         │
│ Quick Tab:     fill(), appearance(), visibility(), transform()                          │
│ Advanced Tab:  animation(), prediction(), linking(), modifiers(), orbit(), arrange()    │
│ Debug Tab:     beam(), trigger(), lifecycle(), bindings(), trace()                      │
│                                                                                         │
│ Each method returns: new PanelWrapper(new XxxSubPanel(...))                             │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. State Management

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              FieldEditState                                              │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│ Central state container for all GUI editing operations.                                 │
│                                                                                         │
│ Composition:                                                                            │
│   workingDefinition ──► FieldDefinition (the data model)                                │
│   originalDefinition ─► FieldDefinition (for reset/revert)                              │
│   editorState ────────► EditorState (selection tracking)                                │
│   undoManager ────────► UndoManager (undo/redo)                                         │
│   changeListeners ────► List<Runnable> (observer pattern)                               │
│                                                                                         │
│ Access Pattern:                                                                         │
│   + get(String path), set(String path, Object value)                                    │
│   + getFloat(path), getInt(path), getBool(path), getString(path)                        │
│   + getSelectedLayer(), getSelectedPrimitive()                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
         │
    ┌────┴─────────────────────────────────────┐
    │                                          │
    ▼                                          ▼
┌─────────────────────┐              ┌─────────────────────┐
│    EditorState      │              │    UndoManager      │
├─────────────────────┤              ├─────────────────────┤
│ selectedLayerIndex  │              │ undoStack: Deque    │
│ selectedPrimitiveIdx│              │ redoStack: Deque    │
│ focusedField        │              │ maxSize: int        │
├─────────────────────┤              ├─────────────────────┤
│ + selectLayer(int)  │              │ + push(definition)  │
│ + selectPrimitive() │              │ + undo(): Definition│
│ + hasSelection()    │              │ + redo(): Definition│
└─────────────────────┘              │ + canUndo/canRedo() │
                                     └─────────────────────┘
```

**Other State Classes:**
- `FieldEditStateHolder` - Singleton accessor
- `StateAccessor` - Reflection-based path access
- `DefinitionBuilder` - State → Definition conversion
- `RendererCapabilities` - Simplified/Standard mode flags
- `PipelineTracer` - Debug tracing
- `AppearanceState`, `PrimitiveComponent` - State fragments

---

## 5. Panel Hierarchy

### 5.1 AbstractPanel (primary hierarchy)

```
                          AbstractPanel
                    extends DrawableHelper
              ┌─────────────────────────────────┐
              │ parent: Screen                  │
              │ state: FieldEditState           │
              │ bounds: Bounds                  │
              │ widgets: List<ClickableWidget>  │
              ├─────────────────────────────────┤
              │ + getWidgets()                  │
              │ + setBounds(Bounds)             │
              │ + render(...)                   │
              │ + tick()                        │
              │ + mouseScrolled(...)            │
              │ # buildWidgets()                │
              └─────────────────────────────────┘
                             △
                             │
    ┌────────────────────────┼────────────────────────────┐
    │                        │                            │
    │                        │                            │
┌────────────┐      ┌────────────────┐           ┌────────────────────┐
│ProfilesPanel│     │ panel/*.java   │           │  panel/sub/*.java  │
├────────────┤      │ (top-level)    │           │  (sub-panels)      │
│profileList │      ├────────────────┤           ├────────────────────┤
│categories  │      │ QuickPanel     │           │ ShapeSubPanel      │
│actions     │      │ AdvancedPanel  │           │ FillSubPanel       │
└────────────┘      │ DebugPanel     │           │ AppearanceSubPanel │
                    │ LayerPanel     │           │ VisibilitySubPanel │
                    │ PrimitivePanel │           │ TransformQuickSub  │
                    │ ActionPanel    │           │ AnimationSubPanel  │
                    └────────────────┘           │ PredictionSubPanel │
                                                 │ LinkingSubPanel    │
                                                 │ ModifiersSubPanel  │
                                                 │ OrbitSubPanel      │
                                                 │ ArrangeSubPanel    │
                                                 │ BeamSubPanel       │
                                                 │ TriggerSubPanel    │
                                                 │ LifecycleSubPanel  │
                                                 │ BindingsSubPanel   │
                                                 │ TraceSubPanel      │
                                                 │ FollowModeSubPanel │
                                                 │ TransformSubPanel  │
                                                 │ ArrangementSubPanel│
                                                 └────────────────────┘
```

### 5.2 LayoutPanel (v2 hierarchy)

```
                          LayoutPanel
              ┌─────────────────────────────────┐
              │ Uses DirectionalLayoutWidget    │
              │ for automatic layout            │
              ├─────────────────────────────────┤
              │ # buildContent(layout)          │
              │ + recalculate()                 │
              └─────────────────────────────────┘
                             △
                             │
              ┌──────────────┴──────────────────┐
              │      panel/v2/*.java            │
              ├─────────────────────────────────┤
              │ ShapePanel                      │
              │ FillPanel                       │
              │ AppearancePanel                 │
              │ VisibilityPanel                 │
              │ TransformPanel                  │
              │ AnimationPanel                  │
              │ PredictionPanel                 │
              │ LinkingPanel                    │
              │ ModifiersPanel                  │
              │ OrbitPanel                      │
              │ ArrangePanel                    │
              │ BeamPanel                       │
              │ TriggerPanel                    │
              │ LifecyclePanel                  │
              │ BindingsPanel                   │
              │ FollowModePanel                 │
              │ ArrangementPanel                │
              └─────────────────────────────────┘
```

**Integration Point:**
```
SubTabPane supports both hierarchies:
  - addTab(String, ContentProvider)  ← PanelWrapper wraps AbstractPanel
  - addTab(String, LayoutPanel)      ← LayoutPanelContentProvider wraps LayoutPanel
```

---

## 5A. Sub-Panel Control Specifications

### ShapeSubPanel
```
Dynamic controls based on shape type:
  SPHERE:     radius, latSteps, lonSteps, latStart, latEnd, algorithm
  RING:       innerRadius, outerRadius, segments, height, y, arcStart, arcEnd, twist
  DISC:       radius, segments, y, innerRadius, arcStart, arcEnd, rings
  PRISM:      sides, radius, height, topRadius, twist, heightSegments, capTop, capBottom
  CYLINDER:   radius, height, segments, topRadius, arc, heightSegments, capTop, capBottom
  POLYHEDRON: radius, subdivisions (type from PolyType: CUBE, TETRA, OCTA, etc.)
  TORUS:      majorRadius, minorRadius, majorSegments, minorSegments
  CAPSULE:    radius, height, segments
  CONE:       bottomRadius, topRadius, height, segments

+ Fragment/Preset dropdown per shape type
+ Pattern controls (faces/body/top/bottom)
```

### FillSubPanel
```
• mode: Dropdown (SOLID, WIREFRAME, CAGE, POINTS)
• wireThickness: Slider (0.1-5.0)
• doubleSided: Toggle
• depthTest: Toggle
• depthWrite: Toggle
```

### AppearanceSubPanel
```
• color: ColorButton + hex input
• alpha: Slider (0.0-1.0)
• glow: Slider (0.0-1.0)
• emissive: Slider (0.0-1.0)
• saturation: Slider (0.0-2.0)
• brightness: Slider (0.0-2.0)
• hueShift: Slider (0-360)
• secondaryColor: ColorButton
• colorBlend: Slider (0.0-1.0)
```

### VisibilitySubPanel
```
• mask: Dropdown (FULL, BANDS, STRIPES, CHECKER, RADIAL, GRADIENT)
• count: Slider (1-32)
• thickness: Slider (0.0-1.0)
• offset: Slider (0.0-1.0)
• invert: Toggle
• feather: Slider (0.0-1.0)
• animate: Toggle
• animateSpeed: Slider (0.1-10.0)
```

### TransformQuickSubPanel
```
• anchor: Dropdown (CENTER, FEET, HEAD, ABOVE, BELOW)
• offset: Vec3Editor (x, y, z)
• rotation: Vec3Editor
• scale: Slider (0.01-10.0)
• facing: Dropdown (FIXED, PLAYER_LOOK, VELOCITY, CAMERA)
```

### AnimationSubPanel
```
SPIN:
• axis: Dropdown (X, Y, Z) or Vec3Editor
• speed: Slider (-0.5 to 0.5)
• oscillate: Toggle
• range: Slider (0-360)

PULSE:
• enabled: Toggle
• scale: Slider (0.0-1.0)
• speed: Slider (0.1-10.0)
• waveform: Dropdown (SINE, SQUARE, TRIANGLE, SAWTOOTH)
• min/max: Sliders

PHASE:
• phase: Slider (0.0-1.0)
```

### PredictionSubPanel
```
• preset: Dropdown (OFF, LOW, MEDIUM, HIGH, CUSTOM)
• enabled: Toggle
• leadTicks: Slider (1-10)
• maxDistance: Slider (1.0-50.0)
• lookAhead: Slider (0.0-1.0)
• verticalBoost: Slider (0.0-2.0)
```

### LinkingSubPanel
```
• id: TextInput
• radiusMatch: PrimitiveDropdown
• radiusOffset: Slider (-10.0 to 10.0)
• follow: PrimitiveDropdown
• mirror: Dropdown (NONE, X, Y, Z)
• phaseOffset: Slider (0.0-1.0)
• scaleWith: PrimitiveDropdown
```

### ModifiersSubPanel
```
• bobbing: Slider (0-1)
• breathing: Slider (0-1)
• alphaMultiplier: Slider (0-1)
• tiltMultiplier: Slider (0-1)
• swirlStrength: Slider (0-1)
```

### OrbitSubPanel
```
• enabled: Toggle
• radius: Slider (0.1-10)
• speed: Slider (0-2)
• axis: Dropdown (X, Y, Z, CUSTOM)
• offset: Vec3Editor
```

### ArrangeSubPanel
```
• arrangement: PatternDropdown (filtered by CellType)
• Multi-part patterns: default, caps, sides, edges, poles, equator
• shuffle: Toggle (Debug only)
• shuffleIndex: Slider (0-N)
```

### BeamSubPanel (Debug)
```
• preset: Dropdown (DEFAULT, SUBTLE, INTENSE, PULSING, CUSTOM)
• enabled: Toggle
• innerRadius, outerRadius: Sliders
• height: Slider (0.1-10.0)
• glow: Slider (0.0-1.0)
• color: ColorButton
• pulseEnabled: Toggle
• pulseScale, pulseSpeed: Sliders
• pulseWaveform: Dropdown
```

### TriggerSubPanel (Debug)
```
• Trigger list: event, effect, duration, params
• Add trigger dialog: event, effect, duration, dynamic params
  - FLASH/COLOR_SHIFT: ColorButton
  - PULSE: scale slider
  - SHAKE: amplitude slider
  - GLOW: intensity slider
```

### LifecycleSubPanel (Debug)
```
• fadeIn, fadeOut: Sliders (0-100 ticks)
• scaleIn, scaleOut: Sliders (0-100 ticks)
• decayEnabled: Toggle
• decayRate: Slider (0.001-0.1)
• decayMin: Slider (0.0-1.0)
```

### BindingsSubPanel (Debug)
```
• Binding list: property, source, input range, output range
• Add binding: property path, source dropdown, ranges, curve
```

### TraceSubPanel (Debug)
```
• Pipeline tracing visualization
• Debug output display
```

### FollowModeSubPanel
```
• enabled: Toggle
• mode: Dropdown (SNAP, SMOOTH, GLIDE)
```

### ProfilesPanel
```
Left side: Profile list (local/server, selection, filtering)
Right side: Category presets display
Actions: Load, Save, Save As, Revert, Rename, Duplicate, Delete, Import, Export
```

---

## 6. Widget Classes

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                    Widgets                                               │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  SubTabPane ──────────► Tab switching with ContentProvider pattern                      │
│  DropdownWidget ──────► Generic dropdown selector                                       │
│  LabeledSlider ───────► Slider with label and value display                             │
│  ModalDialog ─────────► Modal overlay with buttons                                      │
│  ModalFactory ────────► Factory for creating layer/primitive modals                     │
│  GridPane ────────────► 2x2 grid layout helper                                          │
│  PanelWrapper ────────► ContentProvider adapter for AbstractPanel                       │
│  PresetConfirmDialog ─► Preset application confirmation                                 │
│  ToastNotification ───► Animated toast messages                                         │
│  BottomActionBar ─────► Preset/profile action bar                                       │
│  ColorButton ─────────► Color picker button                                             │
│  CompactSelector ─────► Compact dropdown                                                │
│  ConfirmDialog ───────► Confirmation dialog                                             │
│  ExpandableSection ───► Collapsible section                                             │
│  LoadingIndicator ────► Loading spinner                                                 │
│  Vec3Editor ──────────► X/Y/Z vector editor                                             │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Utility Classes

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                    Utilities                                             │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  GuiWidgets ──────────► Widget factory methods (slider, toggle, button, checkbox...)    │
│  GuiConstants ────────► Theme constants (colors, heights, padding)                      │
│  WidgetVisibility ────► Control-level visibility management                             │
│  WidgetCollector ─────► Collects widgets from components                                │
│  RowLayout ───────────► Horizontal layout helper                                        │
│  FragmentRegistry ────► Single-scope preset fragments                                   │
│  PresetRegistry ──────► Multi-scope presets                                             │
│  GuiAnimations ───────► Animation utilities                                             │
│  GuiLayout ───────────► Layout utilities                                                │
│  GuiKeyboardNav ──────► Keyboard navigation                                             │
│  GuiConfigPersistence ► Config save/load                                                │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Other Classes

```
┌────────────────────────┐    ┌────────────────────────┐    ┌────────────────────────┐
│       screen/          │    │       preview/         │    │       render/          │
├────────────────────────┤    ├────────────────────────┤    ├────────────────────────┤
│ FieldCustomizerScreen  │    │ FieldPreviewRenderer   │    │ SimplifiedFieldRenderer│
│ LogViewerScreen        │    │   + drawField(...)     │    │   (mode toggle)        │
│ TabType (enum)         │    └────────────────────────┘    └────────────────────────┘
└────────────────────────┘

┌────────────────────────┐    ┌────────────────────────┐
│      profile/          │    │       config/          │
├────────────────────────┤    ├────────────────────────┤
│ ProfileManager         │    │ GuiConfig              │
│ Profile (record)       │    └────────────────────────┘
└────────────────────────┘
```

---

## 9. Design Patterns Summary

| Pattern | Usage |
|---------|-------|
| **Strategy** | LayoutManager (FullscreenLayout vs WindowedLayout) |
| **Composition** | ScreenComponent instances in FieldCustomizerScreen |
| **Factory** | ContentProviderFactory, ModalFactory, LayoutFactory |
| **Observer** | FieldEditState.changeListeners |
| **Template Method** | AbstractPanel.buildWidgets(), LayoutPanel.buildContent() |
| **Adapter** | PanelWrapper, LayoutPanelContentProvider |
| **Singleton** | FieldEditStateHolder |
| **Command** | UndoManager (undo/redo stack) |

---

## 10. Package Summary

| Package | Files | Purpose |
|---------|-------|---------|
| `screen/` | 3 | Main screens |
| `component/` | 7 | UI components (new architecture) |
| `layout/` | 9 | Layout strategies and helpers |
| `state/` | 13 | State management |
| `panel/` | 8 | Top-level panels |
| `panel/sub/` | 19 | Sub-panels (AbstractPanel-based) |
| `panel/v2/` | 17 | V2 panels (LayoutPanel-based) |
| `widget/` | 16 | UI widgets |
| `util/` | 11 | Utilities |
| `preview/` | 1 | 3D preview |
| `render/` | 1 | Render mode |
| `profile/` | 2 | Profile management |
| `config/` | 1 | Configuration |
| **Total** | **107** | |

---

## 11. Current Integration

**What's Currently Wired:**
- `FieldCustomizerScreen` uses `ContentProviderFactory`
- `ContentProviderFactory` creates `PanelWrapper(new XxxSubPanel(...))`
- Sub-panels from `panel/sub/` are active
- `component/` classes are in use (HeaderBar, TabBar, etc.)

**Not Currently Wired:**
- `panel/*.java` (QuickPanel, AdvancedPanel, etc.) - not imported
- `panel/v2/*.java` - not imported (LayoutPanel-based alternative)

**Integration Decision Pending:**
After FieldCustomizerScreen integration is complete and tested, decide whether to:
1. Keep `panel/sub/` (current approach)
2. Migrate to `panel/v2/` (LayoutPanel approach)
3. Consolidate both into one hierarchy
