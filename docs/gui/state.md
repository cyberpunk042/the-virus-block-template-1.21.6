# State & Adapters

> Packages: client.gui.state, client.gui.state.adapter, client.gui.layout, client.gui.screen, client.gui.preview, client.gui

**45 classes**

## Class Diagram

```mermaid
classDiagram
    class AppearanceState {
        <<record>>
        +color: int
        +alphaMin: float
        +alphaMax: float
        +glow: float
        +alpha() float
        +builder() Builder
        +toBuilder() Builder
    }
    class DefinitionBuilder {
        +fromState(...) FieldDefinition
    }
    class EditorState {
        +getSelectedLayerIndex() int
        +selectLayer(...) void
        +getSelectedPrimitiveIndex() int
        +selectPrimitive(...) void
        +reset() void
    }
    class FieldEditState {
        +livePreviewEnabled: boolean
        +autoSaveEnabled: boolean
        +debugUnlocked: boolean
        +layers() LayerManager
        +profiles() ProfileManager
        +bindings() BindingsManager
        +triggers() TriggerManager
        +serialization() SerializationManager
    }
    class FieldEditStateHolder {
        +getOrCreate() FieldEditState
        +get() FieldEditState
        +set(...) void
        +clear() void
        +reset() void
    }
    class PipelineTracer {
        +A1_PRIMARY_COLOR: String
        +A2_ALPHA: String
        +A3_GLOW: String
        +A4_EMISSIVE: String
        +enable() void
        +disable() void
        +isEnabled() boolean
        +clear() void
        +trace(...) void
    }
    class RendererCapabilities {
        +isSupported(...) boolean
        +areAllSupported() boolean
        +isAnySupported() boolean
        +getSupportedFeatures() Set
        +getUnsupportedFeatures() Set
    }
    class StateAccessor {
        +get(...) T
        +get(...) T
        +set(...) boolean
        +get(...) Object
        +getInt(...) int
    }
    class UndoManager {
        +push(...) void
        +undo(...) FieldDefinition
        +redo(...) FieldDefinition
        +canUndo() boolean
        +canRedo() boolean
    }
    class AbstractAdapter {
        <<abstract>>
        +get(...) Object
        +set(...) void
        +paths() Set
        +reset() void
    }
    class AnimationAdapter {
        +category() String
        +loadFrom(...) void
        +saveTo(...) void
        +spin() SpinConfig
        +setSpin(...) void
    }
    class AppearanceAdapter {
        +category() String
        +loadFrom(...) void
        +saveTo(...) void
        +appearance() AppearanceState
        +setAppearance(...) void
    }
    class ArrangementAdapter {
        +category() String
        +loadFrom(...) void
        +saveTo(...) void
        +get(...) Object
        +set(...) void
    }
    class FillAdapter {
        +category() String
        +loadFrom(...) void
        +saveTo(...) void
        +get(...) Object
        +set(...) void
    }
    class LinkAdapter {
        +category() String
        +loadFrom(...) void
        +saveTo(...) void
        +get(...) Object
        +set(...) void
    }
    class PrimitiveAdapter {
        <<interface>>
    }
    class PrimitiveBuilder {
        +id(...) PrimitiveBuilder
        +type(...) PrimitiveBuilder
        +shape(...) PrimitiveBuilder
        +transform(...) PrimitiveBuilder
        +fill(...) PrimitiveBuilder
    }
    class ShapeAdapter {
        +category() String
        +loadFrom(...) void
        +saveTo(...) void
        +currentShape() Shape
        +shapeType() String
    }
    class TransformAdapter {
        +category() String
        +loadFrom(...) void
        +saveTo(...) void
        +transform() Transform
        +setTransform(...) void
    }
    class TriggerAdapter {
        +category() String
        +loadFrom(...) void
        +saveTo(...) void
        +get(...) Object
        +set(...) void
    }
    class VisibilityAdapter {
        +category() String
        +loadFrom(...) void
        +saveTo(...) void
        +mask() VisibilityMask
        +setMask(...) void
    }
    class Bounds {
        <<record>>
        +x: int
        +y: int
        +width: int
        +height: int
        +right() int
        +bottom() int
        +centerX() int
        +centerY() int
        +isEmpty() boolean
    }
    class FullscreenLayout {
        +getMode() GuiMode
        +calculate(...) void
        +getPanelBounds() Bounds
        +getTitleBarBounds() Bounds
        +getPreviewBounds() Bounds
    }
    class GuiMode {
        <<enumeration>>
    }
    class LayoutFactory {
        +get(...) LayoutManager
        +getAndCalculate(...) LayoutManager
        +windowed() WindowedLayout
        +fullscreen() FullscreenLayout
    }
    class LayoutManager {
        <<interface>>
    }
    class LayoutPanel {
        <<abstract>>
        +setBounds(...) void
        +rebuild() void
        +getWidgets() List
        +getBounds() Bounds
        +tick() void
    }
    class SidePanel {
        +addTab(...) SidePanel
        +setActiveTab(...) void
        +setOnModeToggle(...) void
        +setOnClose(...) void
        +tick() void
    }
    class StatusBar {
        +setBounds(...) void
        +showMessage(...) void
        +setWarning(...) void
        +clearWarning() void
        +render(...) void
    }
    class AbstractPanel
    class Builder
    class Objectvalue
    class Feature
    class T
    class Objectstate
    class Deque
    class Primitivesource
    class Builderbuilder
    class Objectv
    class Shape
    class Screen
    class Side
    class Boundsbounds
    AbstractAdapter --> Objectvalue : uses
    AbstractAdapter --> T : returns
    AbstractAdapter --> TdefaultValue : uses
    AbstractAdapter --> Tvalue : uses
    AbstractAdapter <|-- AnimationAdapter
    AbstractAdapter <|-- AppearanceAdapter
    AbstractAdapter <|-- ArrangementAdapter
    AbstractAdapter <|-- FillAdapter
    AbstractAdapter <|-- LinkAdapter
    AbstractAdapter <|-- ShapeAdapter
    AbstractAdapter <|-- TransformAdapter
    AbstractAdapter <|-- TriggerAdapter
    AbstractAdapter <|-- VisibilityAdapter
    AbstractPanel <|-- SidePanel
    AnimationAdapter --> AlphaPulseConfig : alphaPulse
    AnimationAdapter --> PulseConfig : pulse
    AnimationAdapter --> SpinConfig : spin
    AnimationAdapter --> WobbleConfig : wobble
    AppearanceAdapter --> AppearanceState : appearance
    AppearanceAdapter --> AppearanceState : returns
    AppearanceAdapter --> PrimitiveBuilderbuilder : uses
    AppearanceAdapter --> Primitivesource : uses
    AppearanceState --> Builder : returns
    ArrangementAdapter --> ArrangementConfig : arrangement
    ArrangementAdapter --> Objectvalue : uses
    ArrangementAdapter --> PrimitiveBuilderbuilder : uses
    ArrangementAdapter --> Primitivesource : uses
    DefinitionAdapter --> Builderbuilder : uses
    DefinitionAdapter --> Objectvalue : uses
    DefinitionBuilder --> FieldDefinition : returns
    DefinitionBuilder --> FieldEditStatestate : uses
    DefinitionBuilder --> FieldLayer : returns
    DefinitionBuilder --> FieldLayeroriginal : uses
    FieldEditState --> AnimationAdapter : animationAdapter
    FieldEditState --> FillAdapter : fillAdapter
    FieldEditState --> ShapeAdapter : shapeAdapter
    FieldEditState --> TransformAdapter : transformAdapter
    FieldEditStateHolder --> FieldEditState : current
    FieldEditStateHolder --> FieldEditState : returns
    FieldEditStateHolder --> FieldEditStatestate : uses
    FillAdapter --> FillConfig : fill
    FillAdapter --> Objectvalue : uses
    FillAdapter --> PrimitiveBuilderbuilder : uses
    FillAdapter --> Primitivesource : uses
    FullscreenLayout --> Bounds : panel
    FullscreenLayout --> Bounds : preview
    FullscreenLayout --> Bounds : tabBar
    FullscreenLayout --> Bounds : titleBar
    LayoutFactory --> FullscreenLayout : FULLSCREEN
    LayoutFactory --> LayoutManager : returns
    LayoutFactory --> WindowedLayout : WINDOWED
    LayoutFactory --> WindowedLayout : returns
    LayoutManager --> Bounds : returns
    LayoutManager --> DrawContextcontext : uses
    LayoutManager --> GuiMode : returns
    LayoutManager <|.. FullscreenLayout
    LayoutPanel --> Bounds : bounds
    LayoutPanel --> FieldEditState : state
    LayoutPanel --> Screen : parent
    LayoutPanel --> TextRenderer : textRenderer
    LinkAdapter --> Objectv : uses
    LinkAdapter --> Objectvalue : uses
    LinkAdapter --> PrimitiveBuilderbuilder : uses
    LinkAdapter --> Primitivesource : uses
    PipelineTracer --> Objectvalue : uses
    PrimitiveAdapter --> Objectvalue : uses
    PrimitiveAdapter --> PrimitiveBuilderbuilder : uses
    PrimitiveAdapter --> Primitivesource : uses
    PrimitiveAdapter <|.. AnimationAdapter
    PrimitiveAdapter <|.. AppearanceAdapter
    PrimitiveAdapter <|.. ArrangementAdapter
    PrimitiveAdapter <|.. FillAdapter
    PrimitiveAdapter <|.. LinkAdapter
    PrimitiveAdapter <|.. ShapeAdapter
    PrimitiveAdapter <|.. TransformAdapter
    PrimitiveAdapter <|.. TriggerAdapter
    PrimitiveAdapter <|.. VisibilityAdapter
    PrimitiveBuilder --> FillConfig : fill
    PrimitiveBuilder --> Shape : shape
    PrimitiveBuilder --> Transform : transform
    PrimitiveBuilder --> VisibilityMask : visibility
    RendererCapabilities --> Feature : FULL_FEATURES
    RendererCapabilities --> Feature : SIMPLIFIED_FEATURES
    RendererCapabilities --> Feature : returns
    ShapeAdapter --> DiscShape : disc
    ShapeAdapter --> PrismShape : prism
    ShapeAdapter --> RingShape : ring
    ShapeAdapter --> SphereShape : sphere
    SidePanel --> Bounds : titleBarBounds
    SidePanel --> Side : side
    SidePanel --> TabEntry : tabs
    SidePanel --> TextRenderer : textRenderer
    StateAccessor --> Objectstate : uses
    StateAccessor --> Objectvalue : uses
    StateAccessor --> T : returns
    StateAccessor --> T : uses
    StatusBar --> Bounds : bounds
    StatusBar --> Boundsbounds : uses
    StatusBar --> FieldEditState : state
    StatusBar --> TextRenderer : textRenderer
    TransformAdapter --> PrimitiveBuilderbuilder : uses
    TransformAdapter --> Primitivesource : uses
    TransformAdapter --> Transform : returns
    TransformAdapter --> Transform : transform
    TriggerAdapter --> Objectv : uses
    TriggerAdapter --> Objectvalue : uses
    TriggerAdapter --> PrimitiveBuilderbuilder : uses
    TriggerAdapter --> Primitivesource : uses
    UndoManager --> Deque : redoStack
    UndoManager --> Deque : undoStack
    UndoManager --> FieldDefinition : returns
    VisibilityAdapter --> PrimitiveBuilderbuilder : uses
    VisibilityAdapter --> Primitivesource : uses
    VisibilityAdapter --> VisibilityMask : mask
    VisibilityAdapter --> VisibilityMask : returns
```

---
[Back to GUI System](../gui.md)
