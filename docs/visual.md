# Visual System

> Shapes, patterns, colors, animations, and fill modes.

**74 classes**

## Key Classes

- **`Shape`** (interface)
- **`QuadPattern`** (enum)
- **`TrianglePattern`** (enum)
- **`ColorTheme`** (class)
- **`FillConfig`** (record)
- **`FillMode`** (enum)

## Class Diagram

```mermaid
classDiagram
    class CapsuleShape {
        <<record>>
        +radius: float
        +height: float
        +segments: int
        +rings: int
        +of(...) CapsuleShape
        +getType() String
        +getBounds() Vector3f
        +primaryCellType() CellType
        +getParts() Map
    }
    class ConeShape {
        <<record>>
        +bottomRadius: float
        +topRadius: float
        +height: float
        +segments: int
        +of(...) ConeShape
        +frustum(...) ConeShape
        +getType() String
        +getBounds() Vector3f
        +primaryCellType() CellType
    }
    class CylinderShape {
        <<record>>
        +radius: float
        +height: float
        +segments: int
        +topRadius: float
        +defaults() CylinderShape
        +thin(...) CylinderShape
        +of(...) CylinderShape
        +tapered(...) CylinderShape
        +tube(...) CylinderShape
    }
    class DiscShape {
        <<record>>
        +radius: float
        +segments: int
        +y: float
        +arcStart: float
        +of(...) DiscShape
        +at(...) DiscShape
        +defaults() DiscShape
        +ofRadius(...) DiscShape
        +pie(...) DiscShape
    }
    class PolyhedronShape {
        <<record>>
        +polyType: PolyType
        +radius: float
        +subdivisions: int
        +DEFAULT: PolyhedronShape
        +cube(...) PolyhedronShape
        +octahedron(...) PolyhedronShape
        +dodecahedron(...) PolyhedronShape
        +tetrahedron(...) PolyhedronShape
        +icosahedron(...) PolyhedronShape
    }
    class PolyType {
        <<enumeration>>
    }
    class PrismShape {
        <<record>>
        +sides: int
        +radius: float
        +height: float
        +topRadius: float
        +of(...) PrismShape
        +tapered(...) PrismShape
        +getType() String
        +getBounds() Vector3f
        +primaryCellType() CellType
    }
    class RingShape {
        <<record>>
        +innerRadius: float
        +outerRadius: float
        +segments: int
        +y: float
        +at(...) RingShape
        +getType() String
        +getBounds() Vector3f
        +primaryCellType() CellType
        +getParts() Map
    }
    class Shape {
        <<interface>>
    }
    class ShapeRegistry {
        +register(...) void
        +registerSimple(...) void
        +create(...) Shape
        +create(...) Shape
        +create(...) Shape
    }
    class SphereAlgorithm {
        <<enumeration>>
    }
    class SphereShape {
        <<record>>
        +radius: float
        +latSteps: int
        +lonSteps: int
        +latStart: float
        +of(...) SphereShape
        +defaults() SphereShape
        +ofRadius(...) SphereShape
        +hemisphereTop(...) SphereShape
        +hemisphereBottom(...) SphereShape
    }
    class TorusShape {
        <<record>>
        +majorRadius: float
        +minorRadius: float
        +majorSegments: int
        +minorSegments: int
        +of(...) TorusShape
        +getType() String
        +getBounds() Vector3f
        +primaryCellType() CellType
        +getParts() Map
    }
    class ArrangementConfig {
        <<record>>
        +defaultPattern: String
        +main: String
        +poles: String
        +equator: String
        +of(...) ArrangementConfig
        +getPattern(...) String
        +resolvePattern(...) VertexPattern
        +resolvePattern(...) VertexPattern
        +fromJson(...) ArrangementConfig
    }
    class CellType {
        <<enumeration>>
    }
    class DynamicEdgePattern {
        <<record>>
        +latitude: boolean
        +longitude: boolean
        +skipInterval: int
        +shuffleIndex: int
        +id() String
        +cellType() CellType
        +shouldRender(...) boolean
        +getVertexOrder()
        +renderLatitude() boolean
    }
    class DynamicQuadPattern {
        <<record>>
        +triangle1: Corner
        +triangle2: Corner
        +shuffleIndex: int
        +description: String
        +id() String
        +cellType() CellType
        +shouldRender(...) boolean
        +getVertexOrder()
        +describe() String
    }
    class DynamicSectorPattern {
        <<record>>
        +skipInterval: int
        +phaseOffset: int
        +invertSelection: boolean
        +shuffleIndex: int
        +id() String
        +cellType() CellType
        +shouldRender(...) boolean
        +getVertexOrder()
        +describe() String
    }
    class DynamicSegmentPattern {
        <<record>>
        +skipInterval: int
        +phaseOffset: int
        +reverseWinding: boolean
        +shuffleIndex: int
        +id() String
        +cellType() CellType
        +shouldRender(...) boolean
        +getVertexOrder()
        +describe() String
    }
    class DynamicTrianglePattern {
        <<record>>
        +vertices: Vertex
        +skipInterval: int
        +shuffleIndex: int
        +description: String
        +id() String
        +cellType() CellType
        +shouldRender(...) boolean
        +getVertexOrder()
        +isInverted() boolean
    }
    class EdgePattern {
        <<enumeration>>
    }
    class QuadPattern {
        <<enumeration>>
    }
    class SectorPattern {
        <<enumeration>>
    }
    class SegmentPattern {
        <<enumeration>>
    }
    class ShuffleGenerator {
        +quadCount() int
        +segmentCount() int
        +sectorCount() int
        +edgeCount() int
        +triangleCount() int
    }
    class ShufflePattern {
        +fromPermutation(...) ShufflePattern
        +parse(...) ShufflePattern
        +id() String
        +displayName() String
        +cellType() CellType
    }
    class TrianglePattern {
        <<enumeration>>
    }
    class VertexPattern {
        <<interface>>
    }
    class AlphaPulseConfig {
        <<record>>
        +speed: float
        +min: float
        +max: float
        +waveform: Waveform
        +between(...) AlphaPulseConfig
        +isActive() boolean
        +evaluate(...) float
        +builder() Builder
        +toBuilder() Builder
    }
    class Animation {
        <<record>>
        +spin: SpinConfig
        +pulse: PulseConfig
        +phase: float
        +alphaPulse: AlphaPulseConfig
        +none() Animation
        +spin(...) Animation
        +pulse(...) Animation
        +spinAndPulse(...) Animation
        +isActive() boolean
    }
    class ColorCycleConfig {
        <<record>>
        +colors: List
        +speed: float
        +blend: boolean
        +NONE: ColorCycleConfig
        +fromJson(...) ColorCycleConfig
        +between(...) ColorCycleConfig
        +isActive() boolean
        +toJson() JsonObject
        +builder() Builder
    }
    class FrameSlice {
        <<record>>
        +minV: float
        +maxV: float
        +scrollOffset: float
        +wrap: boolean
        +portion(...) FrameSlice
        +scrolling(...) FrameSlice
        +effectiveMinV() float
        +effectiveMaxV() float
        +height() float
    }
    class Phase {
        <<record>>
        +offset: float
        +ZERO: Phase
        +QUARTER: Phase
        +HALF: Phase
        +of(...) Phase
        +stagger(...) Phase
        +random() Phase
        +apply(...) float
        +add(...) Phase
    }
    class Builder
    class RADIUSfloatradius
    class POSITIVE_NONZEROfloatheight
    class STEPSintsubdivisions
    class SIDESintsides
    class UNBOUNDEDfloaty
    class Corner
    class Waveform
    class ALPHAfloatmin
    class ALPHAfloatmax
    class POSITIVEfloatspeed
    class Phaseother
    AlphaPulseConfig --> ALPHAfloatmax : uses
    AlphaPulseConfig --> ALPHAfloatmin : uses
    AlphaPulseConfig --> POSITIVEfloatspeed : uses
    AlphaPulseConfig --> Waveform : waveform
    Animation --> AlphaPulseConfig : alphaPulse
    Animation --> ColorCycleConfig : colorCycle
    Animation --> PulseConfig : pulse
    Animation --> SpinConfig : spin
    ArrangementConfig --> Builder : returns
    ArrangementConfig --> CellTypeexpectedCellType : uses
    ArrangementConfig --> JsonElementjson : uses
    ArrangementConfig --> VertexPattern : returns
    CapsuleShape --> Builder : returns
    CapsuleShape --> CellType : returns
    CapsuleShape --> Vector3f : returns
    ColorCycleConfig --> Builder : returns
    ColorCycleConfig --> POSITIVEfloatspeed : uses
    ConeShape --> Builder : returns
    ConeShape --> CellType : returns
    ConeShape --> Vector3f : returns
    CylinderShape --> POSITIVE_NONZEROfloatheight : uses
    CylinderShape --> POSITIVEfloattopRadius : uses
    CylinderShape --> RADIUSfloatradius : uses
    CylinderShape --> Vector3f : returns
    DiscShape --> CellType : returns
    DiscShape --> DEGREESfloatarcEnd : uses
    DiscShape --> RADIUSfloatradius : uses
    DiscShape --> Vector3f : returns
    DynamicEdgePattern --> CellType : returns
    DynamicEdgePattern --> EdgeArrangementarr : uses
    DynamicQuadPattern --> CellType : returns
    DynamicQuadPattern --> Corner : triangle1
    DynamicQuadPattern --> Corner : triangle2
    DynamicQuadPattern --> QuadArrangementarr : uses
    DynamicSectorPattern --> CellType : returns
    DynamicSectorPattern --> SectorArrangementarr : uses
    DynamicSegmentPattern --> CellType : returns
    DynamicSegmentPattern --> SegmentArrangementarr : uses
    DynamicTrianglePattern --> CellType : returns
    DynamicTrianglePattern --> TriangleArrangementarr : uses
    DynamicTrianglePattern --> Vertex : vertices
    Phase --> Phaseother : uses
    PolyhedronShape --> PolyType : polyType
    PolyhedronShape --> PolyTypetype : uses
    PolyhedronShape --> RADIUSfloatradius : uses
    PolyhedronShape --> STEPSintsubdivisions : uses
    PrismShape --> POSITIVE_NONZEROfloatheight : uses
    PrismShape --> POSITIVEfloattopRadius : uses
    PrismShape --> RADIUSfloatradius : uses
    PrismShape --> SIDESintsides : uses
    RingShape --> RADIUSfloatinnerRadius : uses
    RingShape --> RADIUSfloatouterRadius : uses
    RingShape --> UNBOUNDEDfloaty : uses
    RingShape --> Vector3f : returns
    Shape --> CellType : returns
    Shape --> Vector3f : returns
    Shape <|.. CapsuleShape
    Shape <|.. ConeShape
    Shape <|.. CylinderShape
    Shape <|.. DiscShape
    Shape <|.. PolyhedronShape
    Shape <|.. PrismShape
    Shape <|.. RingShape
    Shape <|.. SphereShape
    Shape <|.. TorusShape
    ShapeRegistry --> Shape : returns
    ShapeRegistry --> Shape : uses
    ShapeRegistry --> ShapeFactory : FACTORIES
    ShapeRegistry --> ShapeFactoryfactory : uses
    ShuffleGenerator --> EdgeArrangement : EDGE_ARRANGEMENTS
    ShuffleGenerator --> QuadArrangement : QUAD_ARRANGEMENTS
    ShuffleGenerator --> SectorArrangement : SECTOR_ARRANGEMENTS
    ShuffleGenerator --> SegmentArrangement : SEGMENT_ARRANGEMENTS
    ShufflePattern --> CellType : cellType
    ShufflePattern --> CellType : returns
    ShufflePattern --> CellTypecellType : uses
    SphereShape --> CellType : returns
    SphereShape --> RADIUSfloatradius : uses
    SphereShape --> SphereAlgorithm : algorithm
    SphereShape --> Vector3f : returns
    TorusShape --> Builder : returns
    TorusShape --> CellType : returns
    TorusShape --> Vector3f : returns
    VertexPattern --> CellType : returns
    VertexPattern --> CellTypecellType : uses
    VertexPattern --> CellTypeexpectedCellType : uses
    VertexPattern <|.. DynamicEdgePattern
    VertexPattern <|.. DynamicQuadPattern
    VertexPattern <|.. DynamicSectorPattern
    VertexPattern <|.. DynamicSegmentPattern
    VertexPattern <|.. DynamicTrianglePattern
    VertexPattern <|.. EdgePattern
    VertexPattern <|.. QuadPattern
    VertexPattern <|.. SectorPattern
    VertexPattern <|.. SegmentPattern
    VertexPattern <|.. ShufflePattern
    VertexPattern <|.. TrianglePattern
```

---
[Back to README](./README.md)
