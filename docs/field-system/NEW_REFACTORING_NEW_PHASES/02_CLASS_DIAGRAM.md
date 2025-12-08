# Proposed Class Diagram

> **Status:** Target architecture  
> **Created:** December 7, 2024  
> **Based on:** 01_ARCHITECTURE.md and 03_PARAMETERS.md

---

## 1. Core Domain Model

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           FIELD DEFINITION                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      FieldDefinition                                │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ - id: Identifier                                                    │   │
│  │ - type: FieldType                                                   │   │
│  │ - baseRadius: float                                                 │   │
│  │ - themeId: String                                                   │   │
│  │ - layers: List<FieldLayer>                                          │   │
│  │ - modifiers: Modifiers                                              │   │
│  │ - prediction: PredictionConfig                                      │   │
│  │ - beam: BeamConfig                                                  │   │
│  │ - followMode: FollowModeConfig                                      │   │
│  │ - bindings: Map<String, BindingConfig>    ← NEW §16                 │   │
│  │ - triggers: List<TriggerConfig>           ← NEW §16                 │   │
│  │ - lifecycle: LifecycleConfig              ← NEW §16                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│         ┌──────────────────────────┼──────────────────────────┐            │
│         ▼                          ▼                          ▼            │
│  ┌─────────────┐          ┌──────────────┐          ┌──────────────┐      │
│  │  Modifiers  │          │ Prediction   │          │  BeamConfig  │      │
│  ├─────────────┤          │   Config     │          ├──────────────┤      │
│  │ visualScale │          ├──────────────┤          │ enabled      │      │
│  │ tilt        │          │ enabled      │          │ innerRadius  │      │
│  │ swirl       │          │ leadTicks    │          │ outerRadius  │      │
│  │ pulsing     │          │ maxDistance  │          │ color        │      │
│  │ bobbing     │          │ lookAhead    │          │ height       │      │
│  │ breathing   │          │ verticalBoost│          │ glow         │      │
│  │ pulse: Pulse│      │
│  │   Config    │      │      │
│  └─────────────┘          └──────────────┘          └──────────────┘      │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     FollowModeConfig                               │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ enabled: boolean        ← false = static field                     │   │
│  │ mode: FollowMode        ← SNAP | SMOOTH | GLIDE                    │   │
│  │ playerOverride: boolean ← can player change in GUI?                │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────┐                                                           │
│  │  FieldType  │ enum: SHIELD | PERSONAL | FORCE | AURA | PORTAL          │
│  └─────────────┘       (removed: SINGULARITY, GROWTH, BARRIER)            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Layer System

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              LAYER                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        FieldLayer                                   │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ - id: String                                                        │   │
│  │ - primitives: List<Primitive>                                       │   │
│  │ - colorRef: String                                                  │   │
│  │ - alpha: float                                                      │   │
│  │ - rotation: Vec3              ← STATIC orientation                  │   │
│  │ - spin: SpinConfig            ← ANIMATED rotation                   │   │
│  │ - tilt: float                                                       │   │
│  │ - pulse: float                                                      │   │
│  │ - phaseOffset: float                                                │   │
│  │ - visible: boolean                                                  │   │
│  │ - blendMode: BlendMode                                              │   │
│  │ - order: int                                                        │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│                          List<Primitive>                                    │
│                                                                             │
│  ┌─────────────┐                                                           │
│  │ BlendMode   │ enum: NORMAL | ADD | MULTIPLY | SCREEN                   │
│  └─────────────┘       Phase 1: NORMAL, ADD                               │
│                         Phase 2: MULTIPLY, SCREEN (custom shaders)         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Primitive System (Flattened)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            PRIMITIVES                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                     ┌──────────────────────┐                               │
│                     ┌────────────────────────────────┐                      │
│                     │       «interface»              │                      │
│                     │         Primitive              │                      │
│                     ├────────────────────────────────┤                      │
│                     │ + id(): String                 │  ← REQUIRED          │
│                     │ + type(): String               │                      │
│                     │ + shape(): Shape               │                      │
│                     │ + transform(): Transform       │                      │
│                     │ + fill(): FillConfig           │                      │
│                     │ + visibility(): VisibilityMask │                      │
│                     │ + arrangement(): ArrangementCfg│                      │
│                     │ + appearance(): Appearance     │                      │
│                     │ + animation(): Animation       │                      │
│                     │ + link(): PrimitiveLink        │  ← Phase 1 (nullable)│
│                     └────────────────────────────────┘                               │
│                                △                                            │
│       ┌────────────────────────┼────────────────────────┐                  │
│       │            │           │           │            │                  │
│  ┌────┴────┐ ┌─────┴────┐ ┌────┴────┐ ┌────┴────┐ ┌─────┴─────┐           │
│  │ Sphere  │ │   Ring   │ │  Disc   │ │  Prism  │ │Polyhedron │           │
│  │Primitive│ │ Primitive│ │Primitive│ │Primitive│ │ Primitive │           │
│  └─────────┘ └──────────┘ └─────────┘ └─────────┘ └───────────┘           │
│       │                                                  │                  │
│  ┌────┴────┐                                       ┌─────┴─────┐           │
│  │Cylinder │                                       │   Torus   │ FUTURE   │
│  │Primitive│                                       │ Primitive │           │
│  └─────────┘                                       └───────────┘           │
│       │                                                  │                  │
│  ┌────┴────┐                                       ┌─────┴─────┐           │
│  │  Cone   │ FUTURE                                │   Helix   │ FUTURE   │
│  │Primitive│                                       │ Primitive │           │
│  └─────────┘                                       └───────────┘           │
│                                                                             │
│  REMOVED (now config options):                                             │
│  ✗ StripesPrimitive  → SpherePrimitive + visibility.mask=STRIPES          │
│  ✗ CagePrimitive     → SpherePrimitive + fill.mode=CAGE                   │
│  ✗ BeamPrimitive     → CylinderPrimitive                                  │
│  ✗ RingsPrimitive    → Multiple RingPrimitive in layer                    │
│                                                                             │
│  REMOVED (unnecessary hierarchy):                                          │
│  ✗ SolidPrimitive (abstract)                                              │
│  ✗ BandPrimitive (abstract)                                               │
│  ✗ StructuralPrimitive (abstract)                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Shape System

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              SHAPES                                         │
│                        Package: visual.shape                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                      ┌───────────────────────────────┐                     │
│                      │        «interface»             │                     │
│                      │           Shape                │                     │
│                      ├───────────────────────────────┤                     │
│                      │ + getType(): String            │                     │
│                      │ + getBounds(): Box             │                     │
│                      │ + primaryCellType(): CellType  │ ← Primary cell     │
│                      │ + getParts(): Map<String,Cell> │ ← All parts        │
│                      └───────────────────────────────┘                               │
│                                △                                            │
│     ┌──────────────────────────┼──────────────────────────┐                │
│     │              │           │           │              │                │
│ ┌───┴─────┐  ┌─────┴────┐ ┌────┴────┐ ┌────┴────┐  ┌──────┴─────┐        │
│ │ Sphere  │  │   Ring   │ │  Disc   │ │  Prism  │  │ Polyhedron │        │
│ │  Shape  │  │  Shape   │ │  Shape  │ │  Shape  │  │   Shape    │        │
│ ├─────────┤  ├──────────┤ ├─────────┤ ├─────────┤  ├────────────┤        │
│ │ radius  │  │innerRad  │ │ radius  │ │ sides   │  │ polyType   │        │
│ │ latSteps│  │outerRad  │ │ segments│ │ radius  │  │ radius     │        │
│ │ lonSteps│  │ segments │ │ y       │ │ height  │  │subdivisions│        │
│ │ latStart│  │ y        │ │arcStart │ │ topRad  │  └────────────┘        │
│ │ latEnd  │  │ arcStart │ │ arcEnd  │ │ twist   │                        │
│ │ lonStart│  │ arcEnd   │ │innerRad │ │hgtSegs  │  ┌────────────┐        │
│ │ lonEnd  │  │ height   │ │ rings   │ │ capTop  │  │ Cylinder   │        │
│ │algorithm│  │ twist    │ └─────────┘ │capBottom│  │   Shape    │        │
│ └─────────┘  └──────────┘             └─────────┘  ├────────────┤        │
│                                                     │ radius     │        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐         │ height     │        │
│  │  Torus   │  │   Cone   │  │  Helix   │ FUTURE  │ segments   │        │
│  │  Shape   │  │  Shape   │  │  Shape   │         │ topRadius  │        │
│  ├──────────┤  ├──────────┤  ├──────────┤         │ hgtSegs    │        │
│  │majorRad  │  │radBottom │  │ radius   │         │ caps       │        │
│  │minorRad  │  │ radTop   │  │ height   │         │ arc        │        │
│  │majorSegs │  │ height   │  │ turns    │         └────────────┘        │
│  │minorSegs │  │ segments │  │ tubeRad  │                                │
│  │ arc      │  │ hgtSegs  │  │ segments │                                │
│  │ twist    │  │capBottom │  │ tubeSegs │                                │
│  └──────────┘  │ arc      │  │direction │                                │
│                └──────────┘  └──────────┘                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Configuration Objects

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CONFIGURATION                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌───────────────────────┐  ┌─────────────────┐  ┌─────────────────┐     │
│  │      Transform        │  │   FillConfig    │  │VisibilityMask  │     │
│  ├───────────────────────┤  ├─────────────────┤  ├─────────────────┤     │
│  │ anchor: Anchor        │  │ mode: FillMode  │  │ --- Phase 1 --- │     │
│  │ offset: Vec3          │  │ wireThickness   │  │ mask: MaskType  │     │
│  │ rotation: Vec3        │  │ doubleSided     │  │ count: int      │     │
│  │ scale: float          │  │ depthTest       │  │ thickness: float│     │
│  │ scaleXYZ: Vec3        │  │ depthWrite      │  │ --- Phase 2 --- │     │
│  │ scaleWithRadius: bool │  │ cage: CageOpts  │  │ offset: float   │     │
│  │ facing: Facing        │  └─────────────────┘  │ invert: boolean │     │
│  │ up: UpVector          │                        │ feather: float  │     │
│  │ billboard: Billboard  │  ┌─────────────────┐  │ animate: boolean│     │
│  │ inheritRot: bool      │  │   CageOptions   │  │ animSpeed: float│     │
│  │ orbit: OrbitConfig    │  │ (shape-specific)│  │ ---gradient---  │     │
│  └───────────────────────┘  ├─────────────────┤  │ direction       │     │
│                              │ --common--      │  │ falloff         │     │
│                              │ lineWidth       │  │ start, end      │     │
│                              │ showEdges       │  └─────────────────┘     │
│                              │ --sphere--      │                          │
│                              │ latitudeCount   │                          │
│                              │ longitudeCount  │                          │
│                              │ showEquator     │                          │
│                              │ showPoles       │                          │
│                              │ --prism/cyl--   │                          │
│                              │ verticalLines   │                          │
│                              │ horizontalRings │                          │
│                              │ showCaps        │                          │
│                              │ --polyhedron--  │                          │
│                              │ allEdges        │                          │
│                              │ faceOutlines    │                          │
│                              └─────────────────┘                          │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐           │
│  │    Anchor       │  │   Facing        │  │   Billboard     │           │
│  │     enum        │  │     enum        │  │     enum        │           │
│  ├─────────────────┤  ├─────────────────┤  ├─────────────────┤           │
│  │ CENTER          │  │ FIXED           │  │ NONE            │           │
│  │ FEET            │  │ PLAYER_LOOK     │  │ FULL            │           │
│  │ HEAD            │  │ VELOCITY        │  │ Y_AXIS          │           │
│  │ ABOVE           │  │ CAMERA          │  └─────────────────┘           │
│  │ BELOW           │  └─────────────────┘                                 │
│  │ FRONT           │                        ┌─────────────────┐           │
│  │ BACK            │  ┌─────────────────┐  │   MaskType      │           │
│  │ LEFT            │  │   FillMode      │  │     enum        │           │
│  │ RIGHT           │  │     enum        │  ├─────────────────┤           │
│  └─────────────────┘  ├─────────────────┤  │ FULL            │           │
│                        │ SOLID           │  │ BANDS           │           │
│  ┌─────────────────┐  │ WIREFRAME       │  │ STRIPES         │           │
│  │   OrbitConfig   │  │ CAGE            │  │ CHECKER         │           │
│  ├─────────────────┤  │ POINTS          │  │ RADIAL          │           │
│  │ enabled: bool   │  └─────────────────┘  │ GRADIENT        │           │
│  │ radius: float   │                        │ CUSTOM          │           │
│  │ speed: float    │  ┌─────────────────┐  └─────────────────┘           │
│  │ axis: Axis      │  │   UpVector      │                                 │
│  │ phase: float    │  │     enum        │                                 │
│  └─────────────────┘  ├─────────────────┤                                 │
│                        │ WORLD_UP        │                                 │
│                        │ PLAYER_UP       │                                 │
│                        │ VELOCITY        │                                 │
│                        │ CUSTOM          │                                 │
│                        └─────────────────┘                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Pattern System (5 Levels)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         PATTERN SYSTEM                                      │
│                       Package: visual.pattern                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  LEVEL 2: Cell Type (what tessellation produces)                           │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                       CellType (enum)                               │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ QUAD       - 4-corner cells (sphere lat/lon, prism sides)          │   │
│  │ SEGMENT    - Arc segments (rings)                                   │   │
│  │ SECTOR     - Radial slices (discs)                                  │   │
│  │ EDGE       - Line segments (wireframe)                              │   │
│  │ TRIANGLE   - 3-corner cells (icosphere, some polyhedra)            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  LEVEL 3: Arrangement (vertex pattern within cell)                         │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                   «interface» VertexPattern                         │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ + id(): String                                                      │   │
│  │ + displayName(): String                                             │   │
│  │ + cellType(): CellType                                              │   │
│  │ + shouldRender(index: int, total: int): boolean  ← Filter cells    │   │
│  │ + getVertexOrder(): int[][]                      ← Reorder verts   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    △                                        │
│       ┌────────────────────────────┼────────────────────────┐              │
│       │              │             │             │          │              │
│  ┌────┴─────┐  ┌─────┴────┐  ┌─────┴────┐  ┌─────┴───┐  ┌────┴────┐     │
│  │  Quad    │  │ Segment  │  │  Sector  │  │  Edge   │  │Triangle │     │
│  │ Pattern  │  │ Pattern  │  │ Pattern  │  │ Pattern │  │ Pattern │     │
│  │  (enum)  │  │  (enum)  │  │  (enum)  │  │ (enum)  │  │ (enum)  │     │
│  ├──────────┤  ├──────────┤  ├──────────┤  ├─────────┤  ├─────────┤     │
│  │filled_1  │  │ full     │  │ full     │  │ full    │  │ full    │     │
│  │triangle_1│  │alternatin│  │ half     │  │latitude │  │alternat.│     │
│  │triangle_2│  │ sparse   │  │ quarters │  │longitude│  │inverted │     │
│  │wave_1    │  │ quarter  │  │ pinwheel │  │ sparse  │  │ sparse  │     │
│  │tooth_1   │  │ reversed │  │trisector │  │ minimal │  │  fan    │     │
│  │stripe_1  │  │ zigzag   │  │ spiral   │  │ dashed  │  │ radial  │     │
│  │...16 more│  │ dashed   │  │crosshair │  │ grid    │  └─────────┘     │
│  └──────────┘  └──────────┘  └──────────┘  └─────────┘                   │
│                                                                             │
│  Dynamic Patterns (for shuffle exploration):                               │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ DynamicQuadPattern     │ DynamicSegmentPattern │ DynamicSectorPattern│  │
│  │ DynamicEdgePattern     │ DynamicTrianglePattern│ ShuffleGenerator    │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Appearance & Animation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      APPEARANCE & ANIMATION                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────┐       ┌─────────────────────────┐            │
│  │      Appearance         │       │       Animation         │            │
│  ├─────────────────────────┤       ├─────────────────────────┤            │
│  │ color: String           │       │ spin: SpinConfig        │            │
│  │ alpha: AlphaRange       │       │ pulse: PulseConfig      │            │
│  │ glow: float             │       │ phase: float            │            │
│  │ emissive: float         │       │ alphaPulse: AlphaPulse  │            │
│  │ saturation: float       │       │ colorCycle: ColorCycle  │            │
│  │ brightness: float       │       │ wobble: WobbleConfig    │            │
│  │ hueShift: float         │       │ wave: WaveConfig        │            │
│  │ secondaryColor: String  │       └─────────────────────────┘            │
│  │ colorBlend: float       │                   │                          │
│  └─────────────────────────┘                   ▼                          │
│              │                      ┌─────────────────────────┐            │
│              ▼                      │      SpinConfig         │            │
│  ┌─────────────────────────┐       ├─────────────────────────┤            │
│  │      AlphaRange         │       │ axis: Axis / Vec3       │            │
│  ├─────────────────────────┤       │ speed: float            │            │
│  │ min: float              │       │ oscillate: boolean      │            │
│  │ max: float              │       │ range: float            │            │
│  └─────────────────────────┘       └─────────────────────────┘            │
│                                                                             │
│  ┌─────────────────────────┐       ┌─────────────────────────┐            │
│  │     PulseConfig         │       │   AlphaPulseConfig      │            │
│  ├─────────────────────────┤       ├─────────────────────────┤            │
│  │ scale: float            │       │ speed: float            │            │
│  │ speed: float            │       │ min: float              │            │
│  │ waveform: Waveform      │       │ max: float              │            │
│  │ min: float              │       │ waveform: Waveform      │            │
│  │ max: float              │       └─────────────────────────┘            │
│  └─────────────────────────┘                                               │
│                                     ┌─────────────────────────┐            │
│                                     │     WobbleConfig        │ FUTURE    │
│                                     ├─────────────────────────┤            │
│                                     │ amplitude: Vec3         │            │
│                                     │ speed: float            │            │
│                                     │ randomize: boolean      │            │
│                                     └─────────────────────────┘            │
│                                                                             │
│                                     ┌─────────────────────────┐            │
│                                     │   ColorCycleConfig      │  FUTURE   │
│  ┌─────────────────────────┐       ├─────────────────────────┤            │
│  │      Waveform           │       │ colors: List<String>    │            │
│  │       enum              │       │ speed: float            │            │
│  ├─────────────────────────┤       │ blend: boolean          │            │
│  │ SINE                    │       └─────────────────────────┘            │
│  │ SQUARE                  │                                               │
│  │ TRIANGLE_WAVE           │  ← renamed (avoid shape confusion)           │
│  │ SAWTOOTH                │       ┌─────────────────────────┐            │
│  └─────────────────────────┘       │       Axis enum         │            │
│                                     ├─────────────────────────┤            │
│                                     │ X | Y | Z | CUSTOM      │            │
│                                     └─────────────────────────┘            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Rendering Pipeline (Client-Side)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        RENDERING PIPELINE                                   │
│                     Package: client.field.render                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  FieldDefinition                                                            │
│       │                                                                     │
│       ▼                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      FieldRenderer                                  │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ + render(definition, matrices, provider, light, time, resolver)    │   │
│  │ + renderWithOverrides(definition, matrices, ..., overrides)        │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                     │
│       ▼ for each layer                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      LayerRenderer                                  │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ + render(layer, matrices, consumer, light, time, resolver)         │   │
│  │ - applyLayerTransform(matrices, layer, time)                       │   │
│  │ - applyLayerAnimation(matrices, layer, time)                       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                     │
│       ▼ for each primitive                                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                  «interface» PrimitiveRenderer                      │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ + render(primitive, matrices, consumer, light, time, resolver,     │   │
│  │          overrides)                                                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    △                                        │
│       ┌────────────────────────────┼────────────────────────┐              │
│       │           │                │                │       │              │
│  ┌────┴────┐ ┌────┴────┐    ┌──────┴─────┐   ┌──────┴────┐ │              │
│  │ Sphere  │ │  Ring   │    │   Disc     │   │   Prism   │ ...           │
│  │Renderer │ │Renderer │    │  Renderer  │   │  Renderer │                │
│  └─────────┘ └─────────┘    └────────────┘   └───────────┘                │
│       │                                                                     │
│       ▼                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                       Tessellator                                   │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ + tessellate(shape, pattern, visibility): Mesh                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                     │
│       ▼                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                       VertexEmitter                                 │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ + emitMesh(consumer, mesh, matrix, color, light)                    │   │
│  │ + emitQuad(consumer, v0, v1, v2, v3, color, light)                 │   │
│  │ + emitLine(consumer, v0, v1, color, thickness)                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 9. Server-Side Management

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SERVER SIDE                                         │
│                       Package: field                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      FieldManager                                   │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ - instances: Map<Long, FieldInstance>                               │   │
│  │ - playerFields: Map<UUID, Long>                                     │   │
│  │ + get(world): FieldManager                                          │   │
│  │ + spawnAt(defId, pos, scale, lifetime): FieldInstance              │   │
│  │ + spawnForPlayer(defId, uuid, scale): PersonalFieldInstance        │   │
│  │ + remove(id): boolean                                               │   │
│  │ + tick()                                                            │   │
│  │ + all(): Collection<FieldInstance>                                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      FieldInstance                                  │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ - id: long                                                          │   │
│  │ - definitionId: Identifier                                          │   │
│  │ - position: Vec3d                                                   │   │
│  │ - scale: float                                                      │   │
│  │ - phase: float                                                      │   │
│  │ - age: int                                                          │   │
│  │ - lifecycleState: LifecycleState    ← SPAWNING|ACTIVE|DESPAWNING   │   │
│  │ - fadeProgress: float               ← 0.0 to 1.0 during transitions│   │
│  │ + tick(): boolean (true = remove)                                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    △                                        │
│                    ┌───────────────┴───────────────┐                       │
│                    │                               │                       │
│  ┌─────────────────┴─────────────┐ ┌──────────────┴──────────────────┐    │
│  │   PersonalFieldInstance       │ │     AnchoredFieldInstance       │    │
│  ├───────────────────────────────┤ ├─────────────────────────────────┤    │
│  │ - ownerUuid: UUID             │ │ - blockPos: BlockPos            │    │
│  │ - followMode: FollowMode      │ │ - blockEntity: BlockEntity      │    │
│  │ - predictionConfig            │ └─────────────────────────────────┘    │
│  │ - targetPosition: Vec3d       │                                        │
│  │ + updateFromPlayer(player)    │                                        │
│  └───────────────────────────────┘                                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 10. Arrangement System (Multi-Part)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ARRANGEMENT                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Simple form: "arrangement": "wave_1"                                       │
│  → Parser converts to: ArrangementConfig.of("wave_1")                       │
│                                                                             │
│  Multi-part form (object):                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    ArrangementConfig                                │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ default: String          ← pattern for all parts                    │   │
│  │ --- Sphere parts ---                                                │   │
│  │ main: String             ← main sphere surface                      │   │
│  │ poles: String            ← top/bottom poles                         │   │
│  │ equator: String          ← equatorial band                          │   │
│  │ hemisphereTop: String    ← top half                                 │   │
│  │ hemisphereBottom: String ← bottom half                              │   │
│  │ --- Ring parts ---                                                  │   │
│  │ surface: String          ← ring/disc main surface                   │   │
│  │ innerEdge: String        ← ring inner border                        │   │
│  │ outerEdge: String        ← ring outer border                        │   │
│  │ edge: String             ← disc outer edge                          │   │
│  │ --- Prism/Cylinder ---                                              │   │
│  │ sides: String            ← wall surfaces                            │   │
│  │ capTop: String           ← top cap                                  │   │
│  │ capBottom: String        ← bottom cap                               │   │
│  │ edges: String            ← edge lines                               │   │
│  │ --- Polyhedron ---                                                  │   │
│  │ faces: String            ← polyhedron faces                         │   │
│  │ vertices: String         ← vertex markers (future)                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  Shape Parts (camelCase for JSON):                                         │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ Sphere:  main, poles, equator, hemisphereTop, hemisphereBottom      │  │
│  │ Ring:    surface, innerEdge, outerEdge                              │  │
│  │ Disc:    surface, edge                                              │  │
│  │ Prism:   sides, capTop, capBottom, edges                            │  │
│  │ Poly:    faces, edges, vertices                                     │  │
│  │ Torus:   outer, inner (future)                                      │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 11. Primitive Linking

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        PRIMITIVE LINKING                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                       PrimitiveLink                                 │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ radiusMatch: String      ← ID of primitive to match radius          │   │
│  │ radiusOffset: float      ← offset from matched radius               │   │
│  │ follow: String           ← ID of primitive to follow position       │   │
│  │ mirror: Axis             ← X, Y, Z - mirror on axis                 │   │
│  │ phaseOffset: float       ← animation phase offset                   │   │
│  │ scaleWith: String        ← ID of primitive to scale with            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  Example (simple offset syntax - NO expression parsing):                   │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ {                                                                    │  │
│  │   "id": "main_sphere",                                               │  │
│  │   "type": "sphere",                                                  │  │
│  │   "shape": { "radius": 1.0 }                                        │  │
│  │ },                                                                   │  │
│  │ {                                                                    │  │
│  │   "id": "outer_ring",                                                │  │
│  │   "type": "ring",                                                    │  │
│  │   "link": {                                                          │  │
│  │     "radiusMatch": "main_sphere",  ← References by ID               │  │
│  │     "radiusOffset": 0.2            ← Simple offset, no expressions  │  │
│  │   }                                                                  │  │
│  │ }                                                                    │  │
│  │ // Result: ring.innerRadius = main_sphere.radius + 0.2 = 1.2        │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 12. JSON Reference System

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        JSON REFERENCE SYSTEM                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  REFERENCE FOLDERS (data/the-virus-block/):                                │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ field_definitions/     ← Complete field profiles (existing)            │ │
│  │ field_shapes/          ← Reusable shape configs                        │ │
│  │ field_appearances/     ← Reusable appearance configs                   │ │
│  │ field_transforms/      ← Reusable transform configs                    │ │
│  │ field_fills/           ← Reusable fill configs                         │ │
│  │ field_masks/           ← Reusable visibility mask configs              │ │
│  │ field_arrangements/    ← Reusable arrangement configs                  │ │
│  │ field_animations/      ← Reusable animation configs                    │ │
│  │ field_layers/          ← Reusable layer templates                      │ │
│  │ field_primitives/      ← Reusable primitive templates                  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  REFERENCE SYNTAX:                                                          │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ "$shapes/smooth_sphere"     → loads field_shapes/smooth_sphere.json   │ │
│  │ "$fills/wireframe"          → loads field_fills/wireframe.json        │ │
│  │ "$masks/horizontal_bands"   → loads field_masks/horizontal_bands.json │ │
│  │ "$layers/spinning_ring"     → loads field_layers/spinning_ring.json   │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  USAGE EXAMPLES:                                                            │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ INLINE (full):                                                         │ │
│  │ {                                                                      │ │
│  │   "shape": { "radius": 1.0, "latSteps": 32, "lonSteps": 64 }          │ │
│  │ }                                                                      │ │
│  │                                                                        │ │
│  │ REFERENCED:                                                            │ │
│  │ {                                                                      │ │
│  │   "shape": "$shapes/smooth_sphere"                                    │ │
│  │ }                                                                      │ │
│  │                                                                        │ │
│  │ REFERENCE + OVERRIDE:                                                  │ │
│  │ {                                                                      │ │
│  │   "shape": { "$ref": "$shapes/smooth_sphere", "radius": 2.0 }         │ │
│  │ }                                                                      │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 13. Smart Defaults System

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          SMART DEFAULTS                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                   DefaultsProvider                                  │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ + getDefaultShape(type: String): Shape                              │   │
│  │ + getDefaultTransform(): Transform                                  │   │
│  │ + getDefaultFill(): FillConfig                                      │   │
│  │ + getDefaultVisibility(): VisibilityMask                           │   │
│  │ + getDefaultAppearance(): Appearance                                │   │
│  │ + getDefaultAnimation(): Animation                                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  DEFAULTS PER LEVEL:                                                        │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ SHAPE (varies by type):                                                │ │
│  │   sphere: { radius: 1.0, latSteps: 32, lonSteps: 64, algorithm: LAT_LON }│
│  │   ring:   { innerRadius: 0.8, outerRadius: 1.0, segments: 64, y: 0 }   │ │
│  │   disc:   { radius: 1.0, segments: 64, y: 0 }                          │ │
│  │   prism:  { sides: 6, radius: 1.0, height: 1.0 }                       │ │
│  │   polyhedron: { polyType: CUBE, radius: 1.0 }                          │ │
│  │   cylinder: { radius: 0.5, height: 10.0, segments: 16 }                │ │
│  ├────────────────────────────────────────────────────────────────────────┤ │
│  │ TRANSFORM:                                                             │ │
│  │   anchor: CENTER, offset: (0,0,0), rotation: (0,0,0), scale: 1.0      │ │
│  │   facing: FIXED, up: WORLD_UP, billboard: NONE                        │ │
│  ├────────────────────────────────────────────────────────────────────────┤ │
│  │ FILL:                                                                  │ │
│  │   mode: SOLID, wireThickness: 1.0, doubleSided: false                 │ │
│  ├────────────────────────────────────────────────────────────────────────┤ │
│  │ VISIBILITY:                                                            │ │
│  │   mask: FULL, count: 4, thickness: 0.5, invert: false                 │ │
│  ├────────────────────────────────────────────────────────────────────────┤ │
│  │ ARRANGEMENT:                                                           │ │
│  │   default: "filled_1" (varies by CellType)                            │ │
│  ├────────────────────────────────────────────────────────────────────────┤ │
│  │ APPEARANCE:                                                            │ │
│  │   color: "@primary", alpha: 1.0, glow: 0.0                            │ │
│  ├────────────────────────────────────────────────────────────────────────┤ │
│  │ ANIMATION:                                                             │ │
│  │   spin: null, pulse: null, phase: 0.0                                 │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  SHORTHAND FORMS:                                                           │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ FULL:                          SHORTHAND:                              │ │
│  │ "alpha": { "min": 0.5,    →   "alpha": 0.5  (constant alpha)          │ │
│  │            "max": 0.5 }                                                │ │
│  │                                                                        │ │
│  │ "spin": { "axis": "Y",    →   "spin": 0.02  (Y-axis spin at speed)    │ │
│  │           "speed": 0.02 }                                              │ │
│  │                                                                        │ │
│  │ "arrangement": { "default": →  "arrangement": "wave_1"                │ │
│  │                  "wave_1" }                                            │ │
│  │                                                                        │ │
│  │ "visibility": { "mask":   →   "visibility": "bands"                   │ │
│  │                 "bands" }                                              │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 14. Color System (Existing)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          COLOR SYSTEM                                       │
│                      Package: visual.color                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      ColorResolver                                  │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ + resolve(reference: String): int   ← Returns ARGB color           │   │
│  │ + resolve(reference: String, theme: ColorTheme): int               │   │
│  │ + parseHex(hex: String): int                                        │   │
│  │ + parseRGB(r: int, g: int, b: int): int                            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        ColorTheme                                   │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ - id: String                                                        │   │
│  │ - primary: int           ← @primary resolves to this               │   │
│  │ - secondary: int         ← @secondary                              │   │
│  │ - accent: int            ← @accent                                 │   │
│  │ - beam: int              ← @beam                                   │   │
│  │ - background: int        ← @background                             │   │
│  │ - highlight: int         ← @highlight                              │   │
│  │ + getColor(role: String): int                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  COLOR REFERENCES:                                                          │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ "@primary"      → Theme primary color                                 │ │
│  │ "@secondary"    → Theme secondary color                               │ │
│  │ "@accent"       → Theme accent color                                  │ │
│  │ "@beam"         → Theme beam color                                    │ │
│  │ "#FF0000"       → Direct hex color                                    │ │
│  │ "rgb(255,0,0)"  → RGB format                                          │ │
│  │ "red"           → Named color (future)                                │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 15. Loading & Parsing

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       LOADING & PARSING                                     │
│                    Package: field.loader                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      FieldLoader                                    │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ + load(ResourceManager): void  ← Load all definitions              │   │
│  │ + reload(): void               ← Reload definitions                │   │
│  │ + loadDefinition(path): FieldDefinition                            │   │
│  │ - parseDefinition(json): FieldDefinition                           │   │
│  │ - parseLayer(json): FieldLayer                                     │   │
│  │ - parsePrimitive(json): Primitive                                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                        uses        ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    ReferenceResolver                                │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ + resolve(ref: String): JsonObject                                  │   │
│  │ + resolveWithOverrides(ref: String, overrides: Json): JsonObject   │   │
│  │ - loadReference(path: String): JsonObject                          │   │
│  │ - mergeJson(base: Json, overrides: Json): JsonObject              │   │
│  │ - cache: Map<String, JsonObject>  ← Caches loaded references       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                        uses        ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    DefaultsProvider                                 │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ + getDefaults(type: String): JsonObject                            │   │
│  │ + applyDefaults(json: JsonObject, type: String): JsonObject       │   │
│  │ - shapeDefaults: Map<String, JsonObject>                           │   │
│  │ - transformDefaults: JsonObject                                    │   │
│  │ - fillDefaults: JsonObject                                         │   │
│  │ - appearanceDefaults: JsonObject                                   │   │
│  │ - animationDefaults: JsonObject                                    │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 16. External Influences System

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      EXTERNAL INFLUENCES                                    │
│                    Package: field.influence                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Added to FieldDefinition:                                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ bindings: Map<String, BindingConfig>                                │   │
│  │ triggers: List<TriggerConfig>                                       │   │
│  │ lifecycle: LifecycleConfig                                          │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────┐       ┌─────────────────────────┐            │
│  │     BindingConfig       │       │     TriggerConfig       │            │
│  ├─────────────────────────┤       ├─────────────────────────┤            │
│  │ source: String          │       │ event: FieldEvent       │            │
│  │ inputRange: float[2]    │       │ effect: TriggerEffect   │            │
│  │ outputRange: float[2]   │       │ duration: int           │            │
│  │ curve: InterpolationCurve│      │ color: String           │            │
│  └─────────────────────────┘       │ scale: float            │            │
│                                     │ amplitude: float        │            │
│  ┌─────────────────────────┐       │ intensity: float        │            │
│  │   LifecycleConfig       │       └─────────────────────────┘            │
│  ├─────────────────────────┤                                               │
│  │ fadeIn: int             │       ┌─────────────────────────┐            │
│  │ fadeOut: int            │       │     DecayConfig         │            │
│  │ scaleIn: int            │       ├─────────────────────────┤            │
│  │ scaleOut: int           │       │ rate: float             │            │
│  │ decay: DecayConfig      │       │ min: float              │            │
│  └─────────────────────────┘       └─────────────────────────┘            │
│                                                                             │
│  ┌─────────────────────────┐       ┌─────────────────────────┐            │
│  │     FieldEvent          │       │    TriggerEffect        │            │
│  │       enum              │       │       enum              │            │
│  ├─────────────────────────┤       ├─────────────────────────┤            │
│  │ PLAYER_DAMAGE           │       │ FLASH                   │            │
│  │ PLAYER_HEAL             │       │ PULSE                   │            │
│  │ PLAYER_DEATH            │       │ SHAKE                   │            │
│  │ PLAYER_RESPAWN          │       │ GLOW                    │            │
│  │ FIELD_SPAWN             │       │ COLOR_SHIFT             │            │
│  │ FIELD_DESPAWN           │       └─────────────────────────┘            │
│  └─────────────────────────┘                                               │
│                                     ┌─────────────────────────┐            │
│  ┌─────────────────────────┐       │  InterpolationCurve     │            │
│  │  «interface»            │       │       enum              │            │
│  │   BindingSource         │       ├─────────────────────────┤            │
│  ├─────────────────────────┤       │ LINEAR                  │            │
│  │ + getId(): String       │       │ EASE_IN                 │            │
│  │ + getValue(player): float│      │ EASE_OUT                │            │
│  │ + isBoolean(): boolean  │       │ EASE_IN_OUT             │            │
│  └─────────────────────────┘       └─────────────────────────┘            │
│             △                                                              │
│             │                                                              │
│  ┌──────────┴──────────────────────────────────────────────────────────┐  │
│  │                    BindingSources (static)                          │  │
│  ├─────────────────────────────────────────────────────────────────────┤  │
│  │ PLAYER_HEALTH: BindingSource        ← player.health                 │  │
│  │ PLAYER_HEALTH_PERCENT: BindingSource← player.health_percent         │  │
│  │ PLAYER_ARMOR: BindingSource         ← player.armor                  │  │
│  │ PLAYER_FOOD: BindingSource          ← player.food                   │  │
│  │ PLAYER_SPEED: BindingSource         ← player.speed                  │  │
│  │ PLAYER_SPRINTING: BindingSource     ← player.is_sprinting           │  │
│  │ PLAYER_SNEAKING: BindingSource      ← player.is_sneaking            │  │
│  │ PLAYER_FLYING: BindingSource        ← player.is_flying              │  │
│  │ PLAYER_INVISIBLE: BindingSource     ← player.is_invisible           │  │
│  │ PLAYER_IN_COMBAT: BindingSource     ← player.in_combat              │  │
│  │ PLAYER_DAMAGE_TAKEN: BindingSource  ← player.damage_taken           │  │
│  │ FIELD_AGE: BindingSource            ← field.age                     │  │
│  │ + get(id: String): BindingSource                                    │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                    CombatTracker                                    │  │
│  ├─────────────────────────────────────────────────────────────────────┤  │
│  │ - lastDamageTaken: long (world time)                                │  │
│  │ - lastDamageDealt: long (world time)                                │  │
│  │ - lastDamageAmount: float                                           │  │
│  │ - damageDecayFactor: float = 0.95                                   │  │
│  │ + isInCombat(): boolean (within 100 ticks)                          │  │
│  │ + getDamageTakenDecayed(): float                                    │  │
│  │ + onDamageTaken(amount: float)                                      │  │
│  │ + onDamageDealt()                                                   │  │
│  │ + tick()                                                            │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 17. Summary: Classes to CREATE / REMOVE / MODIFY

### Classes to CREATE

**Enums:**
- CellType (QUAD, SEGMENT, SECTOR, EDGE, TRIANGLE)
- Anchor (CENTER, FEET, HEAD, ABOVE, BELOW, FRONT, BACK, LEFT, RIGHT)
- Facing (FIXED, PLAYER_LOOK, VELOCITY, CAMERA)
- Billboard (NONE, FULL, Y_AXIS)
- UpVector (WORLD_UP, PLAYER_UP, VELOCITY, CUSTOM)
- FillMode (SOLID, WIREFRAME, CAGE, POINTS)
- MaskType (FULL, BANDS, STRIPES, CHECKER, RADIAL, GRADIENT, CUSTOM)
- Axis (X, Y, Z, CUSTOM)
- Waveform (SINE, SQUARE, TRIANGLE_WAVE, SAWTOOTH)
- BlendMode (NORMAL, ADD, MULTIPLY, SCREEN)
- FieldEvent (PLAYER_DAMAGE, PLAYER_HEAL, PLAYER_DEATH, PLAYER_RESPAWN, FIELD_SPAWN, FIELD_DESPAWN)
- TriggerEffect (FLASH, PULSE, SHAKE, GLOW, COLOR_SHIFT)
- InterpolationCurve (LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT)

**Core Records:**
- FollowModeConfig (replaces enum)
- ArrangementConfig (multi-part support)
- PrimitiveLink (for linking primitives)
- OrbitConfig (dynamic positioning)

**Config Records:**
- FillConfig
- VisibilityMask
- SpinConfig
- PulseConfig

**External Influences:**
- BindingConfig (source, inputRange, outputRange, curve)
- TriggerConfig (event, effect, duration, params)
- LifecycleConfig (fadeIn, fadeOut, scaleIn, scaleOut, decay)
- DecayConfig (rate, min)
- CombatTracker (tracks in_combat, damage_taken)

**Loading:**
- ReferenceResolver (JSON $ref resolution)
- DefaultsProvider (smart defaults per type)

**Patterns (Phase 1):**
- TrianglePattern (for polyhedra with triangle faces)

**Shapes (Future - Phase 4):**
- TorusShape, TorusPrimitive
- ConeShape, ConePrimitive
- HelixShape, HelixPrimitive

### Classes to REMOVE
- StripesPrimitive → becomes visibility.mask=STRIPES
- CagePrimitive → becomes fill.mode=CAGE
- BeamPrimitive, BeamShape → CylinderPrimitive already exists
- RingsPrimitive → multiple RingPrimitive in layer
- SolidPrimitive (abstract)
- BandPrimitive (abstract)
- StructuralPrimitive (abstract)

### Classes to MODIFY
- Transform: complete rewrite with all new options
- All Shape classes: add missing parameters
- FieldLayer: add rotation (static), visible, blendMode, order
- Appearance: add emissive, saturation, brightness, hueShift, secondaryColor
- Animation: expand spin/pulse configs

### Folders to CREATE (for JSON references)

| Folder | Purpose | Example File |
|--------|---------|--------------|
| `field_shapes/` | Reusable shape configs | `smooth_sphere.json`, `dense_ring.json` |
| `field_appearances/` | Reusable appearance configs | `glowing_blue.json`, `translucent_red.json` |
| `field_transforms/` | Reusable transform configs | `above_head.json`, `orbit_around.json` |
| `field_fills/` | Reusable fill configs | `wireframe_thin.json`, `cage_dense.json` |
| `field_masks/` | Reusable visibility masks | `horizontal_bands.json`, `vertical_stripes.json` |
| `field_arrangements/` | Reusable arrangements | `wave_pattern.json`, `alternating.json` |
| `field_animations/` | Reusable animation configs | `slow_spin.json`, `gentle_pulse.json` |
| `field_layers/` | Complete layer templates | `spinning_ring.json`, `pulsing_sphere.json` |
| `field_primitives/` | Complete primitive templates | `glowing_sphere.json`, `wireframe_cube.json` |

**Also rename for clarity:**
- `growth_field_profiles/` ← Move old growth-related profiles here to avoid confusion

### Phase Summary

| Phase | Focus | Key Deliverables |
|-------|-------|------------------|
| 1 | Core restructure | Enums, Config records, Transform, FillConfig, VisibilityMask, JSON refs, TrianglePattern, **Bindings, Triggers, Lifecycle** |
| 2 | GUI design | Customization panel, pattern completeness, player overrides |
| 3 | Primitive linking | Link system, orbit, advanced features |
| 4 | New shapes | Torus, Cone, Helix |

---

## 18. Complete Enum List

| Enum | Values | Package |
|------|--------|---------|
| CellType | QUAD, SEGMENT, SECTOR, EDGE, TRIANGLE | visual.pattern |
| Anchor | CENTER, FEET, HEAD, ABOVE, BELOW, FRONT, BACK, LEFT, RIGHT | visual.transform |
| Facing | FIXED, PLAYER_LOOK, VELOCITY, CAMERA | visual.transform |
| Billboard | NONE, FULL, Y_AXIS | visual.transform |
| UpVector | WORLD_UP, PLAYER_UP, VELOCITY, CUSTOM | visual.transform |
| FillMode | SOLID, WIREFRAME, CAGE, POINTS | visual.fill |
| MaskType | FULL, BANDS, STRIPES, CHECKER, RADIAL, GRADIENT, CUSTOM | visual.visibility |
| Axis | X, Y, Z, CUSTOM | visual.animation |
| Waveform | SINE, SQUARE, TRIANGLE_WAVE, SAWTOOTH | visual.animation |
| BlendMode | NORMAL, ADD, MULTIPLY, SCREEN | visual.layer |
| PolyType | CUBE, OCTAHEDRON, ICOSAHEDRON, DODECAHEDRON, TETRAHEDRON | visual.shape |
| SphereAlgorithm | LAT_LON, TYPE_A, TYPE_E | visual.shape |
| FieldType | SHIELD, PERSONAL, FORCE, AURA, PORTAL | field |
| FollowMode | SNAP, SMOOTH, GLIDE | field.instance |
| FieldEvent | PLAYER_DAMAGE, PLAYER_HEAL, PLAYER_DEATH, PLAYER_RESPAWN, FIELD_SPAWN, FIELD_DESPAWN | field.influence |
| TriggerEffect | FLASH, PULSE, SHAKE, GLOW, COLOR_SHIFT | field.influence |
| InterpolationCurve | LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT | field.influence |

---

## 19. Complete Record List

| Record | Fields | Package |
|--------|--------|---------|
| FillConfig | mode, wireThickness, doubleSided, depthTest, depthWrite, **cage: CageOptions** | visual.fill |
| CageOptions | lineWidth, showEdges, (shape-specific options) | visual.fill |
| VisibilityMask | **Phase 1:** mask, count, thickness; **Phase 2:** offset, invert, feather, animate, animSpeed | visual.visibility |
| SpinConfig | axis, speed, oscillate, range | visual.animation |
| PulseConfig | scale, speed, waveform, min, max | visual.animation |
| AlphaPulseConfig | speed, min, max, waveform | visual.animation |
| OrbitConfig | enabled, radius, speed, axis, phase | visual.transform |
| ArrangementConfig | default, (15 shape parts) | visual.pattern |
| FollowModeConfig | enabled, mode, playerOverride | field.instance |
| PrimitiveLink | radiusMatch, radiusOffset, follow, mirror, phaseOffset, scaleWith | field.primitive |
| AlphaRange | min, max | visual.appearance |
| BeamConfig | enabled, innerRadius, outerRadius, color, height, glow, pulse | field |
| PredictionConfig | enabled, leadTicks, maxDistance, lookAhead, verticalBoost | field.instance |
| Modifiers | visualScale, tilt, swirl, pulsing, bobbing, breathing | field |
| **BindingConfig** | source, inputRange, outputRange, curve | field.influence |
| **TriggerConfig** | event, effect, duration, color, scale, amplitude, intensity | field.influence |
| **LifecycleConfig** | fadeIn, fadeOut, scaleIn, scaleOut, decay | field.influence |
| **DecayConfig** | rate, min | field.influence |

---

*Class diagram v7.1 - Final review fixes applied.*

