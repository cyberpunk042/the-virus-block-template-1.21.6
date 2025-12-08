# Field System Architecture Proposal

> **Status:** Draft for Review  
> **Created:** December 7, 2024  
> **Purpose:** Complete restructure of the field visual system

---

## 1. Configuration Hierarchy

```
FieldDefinition
├── id, type, baseRadius, themeId
├── modifiers: { visualScale, tilt, swirl, pulsing, bobbing, breathing }
├── prediction: { enabled, leadTicks, maxDistance, lookAhead, verticalBoost }
├── beam: { enabled, innerRadius, outerRadius, color, height, glow, pulse }
├── followMode: FollowModeConfig { enabled, mode, playerOverride }
├── bindings: Map<String, BindingConfig>         ← External influences
├── triggers: List<TriggerConfig>                ← External influences
├── lifecycle: LifecycleConfig                   ← External influences
│
└── layers: List<FieldLayer>
    │
    ├── id: string
    ├── rotation: { x, y, z }          ← STATIC orientation (for mirror/90° effects)
    ├── spin: { axis, speed }          ← ANIMATED rotation
    ├── colorRef: string
    ├── alpha: float
    │
    └── primitives: List<Primitive>
        │
        ├── id: String                 ← REQUIRED for linking/debugging
        ├── type: ShapeType            ← LEVEL 1: What geometry
        ├── shape: ShapeConfig         ← Shape-specific params + cellType via shape.primaryCellType()
        ├── transform: Transform       ← Position, orientation, scale
        ├── fill: FillConfig           ← LEVEL 5: How to render
        ├── visibility: VisibilityMask ← LEVEL 4: Which cells to show
        ├── arrangement: Arrangement   ← LEVEL 3: Vertex pattern within cells
        ├── appearance: Appearance     ← Color, alpha, glow
        └── animation: Animation       ← Spin, pulse, phase
```

---

## 2. The Five Geometry Levels

### LEVEL 1: SHAPE TYPE (Base Geometry)

| Shape | Description | Key Parameters (Summary) |
|-------|-------------|--------------------------|
| `sphere` | 3D ball | radius, latSteps, lonSteps, algorithm |
| `ring` | Circle with thickness | innerRadius, outerRadius, segments, y |
| `disc` | Filled circle | radius, segments, y |
| `prism` | N-sided polygon extruded | sides, radius, height |
| `polyhedron` | Platonic solids | polyType, radius |
| `cylinder` | Circular column | radius, height, segments |
| `torus` | Donut shape | majorRadius, minorRadius, segments |
| `cone` | Pointed cylinder | radiusBottom, radiusTop, height |
| `helix` | Spiral | radius, height, turns, tubeRadius |

> **📋 Full Parameters:** See `04_SHAPE_MATRIX.md` and `03_PARAMETERS.md` for complete parameter lists including partial arcs, caps, twist, subdivisions, etc.

**Notes:**
- `BeamPrimitive` is replaced by `CylinderPrimitive` with vertical orientation
- `BeamConfig` at FieldDefinition level is SEPARATE - it's an optional central column effect
- `torus`, `cone`, `helix` are future additions (marked for implementation)
- All shapes can have orientation via transform.rotation

---

### LEVEL 2: CELL TYPE (What Tessellation Produces)

| Cell Type | Used By | Description |
|-----------|---------|-------------|
| `QUAD` | sphere (lat/lon), prism sides, cylinder sides, polyhedron faces | 4-corner cells |
| `SEGMENT` | ring, cylinder caps (as ring edge) | Arc segments around a circle |
| `SECTOR` | disc, cylinder caps, cone base | Radial pie slices |
| `EDGE` | Any shape in wireframe/cage mode | Line segments |
| `TRIANGLE` | polyhedron (some), icosphere | 3-corner cells |

**Mixed shapes:** Some shapes produce multiple cell types:
- `prism` → QUAD (sides) + SECTOR (caps)
- `cylinder` → QUAD (sides) + SECTOR (caps)
- `cone` → QUAD (sides) + SECTOR (base)

**Multi-part arrangement:** For shapes with distinct parts:

**Simple form (applies to all):**
```json
"arrangement": "wave_1"
```

**Detailed form (per-part):**
```json
"arrangement": {
  "default": "wave_1",
  "capTop": "pinwheel",
  "capBottom": "pinwheel",
  "edges": "dashed"
}
```

### Shape Parts Reference

#### Sphere Parts
| Part | Cell Type | Description |
|------|-----------|-------------|
| `main` | QUAD | Main surface (DEFAULT) |
| `poles` | TRIANGLE | Top/bottom poles (triangles converge) |
| `equator` | QUAD | Equatorial band (horizontal ring of quads) |
| `hemisphereTop` | QUAD | Top half only |
| `hemisphereBottom` | QUAD | Bottom half only |

#### Ring Parts
| Part | Cell Type | Description |
|------|-----------|-------------|
| `surface` | SEGMENT | Main ring surface (DEFAULT) |
| `innerEdge` | EDGE | Inner border |
| `outerEdge` | EDGE | Outer border |

#### Disc Parts
| Part | Cell Type | Description |
|------|-----------|-------------|
| `surface` | SECTOR | Main disc surface (DEFAULT) |
| `edge` | EDGE | Outer edge |

#### Prism / Cylinder Parts
| Part | Cell Type | Description |
|------|-----------|-------------|
| `sides` | QUAD | Wall surface (DEFAULT) |
| `capTop` | SECTOR | Top cap |
| `capBottom` | SECTOR | Bottom cap |
| `edges` | EDGE | Vertical edge lines |

#### Polyhedron Parts
| Part | Cell Type | Description |
|------|-----------|-------------|
| `faces` | QUAD/TRIANGLE | Main faces (DEFAULT) |
| `edges` | EDGE | Edge lines |
| `vertices` | POINT | Vertex markers (future) |

#### Torus Parts (Future)
| Part | Cell Type | Description |
|------|-----------|-------------|
| `outer` | QUAD | Outside surface |
| `inner` | QUAD | Inside surface (different curvature) |

---

### LEVEL 3: ARRANGEMENT (Vertex Pattern Within Cells)

Each cell type has its own arrangement options:

#### QUAD Arrangements (QuadPattern)
| Arrangement | Description | Shuffle # |
|-------------|-------------|-----------|
| `filled_1` | Standard filled quad (DEFAULT) | #37 |
| `triangle_1` | Triangle arrangement 1 | #43 |
| `triangle_2` | Triangle arrangement 2 | #53 |
| `triangle_3` | Triangle arrangement 3 | #147 |
| `triangle_4` | Triangle arrangement 4 | #156 |
| `tooth_1` | Sawtooth pattern | #54 |
| `triangle_meshed_1` | Meshed triangles 1 | #62 |
| `triangle_meshed_2` | Meshed triangles 2 | #81 |
| `triangle_slim_1` | Slim triangle | #63 |
| `parallelogram_1` | Parallelogram 1 | #66 |
| `parallelogram_2` | Parallelogram 2 | #74 |
| `three_quarter_slim_1` | Three quarter slim | #69 |
| `filled_with_triangle_1` | Filled with triangle | #71 |
| `stripe_1` | Stripe effect | #83 |
| `square_overlap_1` | Overlapping squares | #99 |
| `wave_1` | Wave pattern | #114 |

#### SEGMENT Arrangements (SegmentPattern)
| Arrangement | Description |
|-------------|-------------|
| `full` | All segments visible |
| `alternating` | Every other segment |
| `sparse` | Every 3rd segment |
| `quarter` | 25% of segments |
| `reversed` | Reversed winding |
| `zigzag` | Zigzag pattern |
| `dashed` | Dashed line effect |

#### SECTOR Arrangements (SectorPattern)
| Arrangement | Description |
|-------------|-------------|
| `full` | All sectors visible |
| `half` | Half circle |
| `quarters` | 4 quarter sections |
| `pinwheel` | Pinwheel effect |
| `trisector` | 3 sections |
| `spiral` | Spiral pattern |
| `crosshair` | Cross pattern |

#### EDGE Arrangements (EdgePattern)
| Arrangement | Description |
|-------------|-------------|
| `full` | All edges visible |
| `latitude` | Horizontal lines only |
| `longitude` | Vertical lines only |
| `sparse` | Reduced edge count |
| `minimal` | Minimum edges |
| `dashed` | Dashed lines |
| `grid` | Grid pattern |

#### TRIANGLE Arrangements (TrianglePattern)
| Arrangement | Description |
|-------------|-------------|
| `full` | All triangles visible (DEFAULT) |
| `alternating` | Every other triangle |
| `inverted` | Flipped orientation |
| `sparse` | Reduced density |
| `fan` | Fan from center |
| `radial` | Radial pattern |

**Note:** TrianglePattern values may be refined after shuffle exploration.

### Pattern Mismatch Handling

If a pattern's CellType doesn't match the shape's primary CellType:
1. **Log error** to render channel
2. **Render nothing** for that primitive
3. **Send chat message** to player (using logging builder)

Example: Setting `arrangement: "pinwheel"` (SECTOR) on a sphere (QUAD) is invalid.

---

### LEVEL 4: VISIBILITY MASK (Which Cells to Show)

**Phase 1 (Minimal):** `mask`, `count`, `thickness`
**Phase 2:** `offset`, `invert`, `feather`, `animate`, `animSpeed`

| Mask | Description | Parameters |
|------|-------------|------------|
| `full` | All cells visible | - |
| `bands` | Horizontal stripes (latitude-based) | count, thickness |
| `stripes` | Vertical stripes (longitude-based) | count, thickness |
| `checker` | Checkerboard pattern | count |
| `radial` | Radial gradient visibility | - (Phase 2) |
| `gradient` | Linear gradient visibility | direction, falloff (Phase 2) |
| `custom` | Custom mask function | (future) |

**Note:** `StripesPrimitive` should become `sphere + visibility: stripes`, not a separate class.

---

### LEVEL 5: FILL MODE (How to Render)

| Mode | Description | Parameters |
|------|-------------|------------|
| `solid` | Filled triangles | - |
| `wireframe` | All edges of tessellated mesh | wireThickness |
| `cage` | Structured lines (lat/lon for sphere) | wireThickness, cage.* |
| `points` | Vertices only (future) | pointSize |

**FillConfig Structure (nested for cage):**
```json
"fill": {
  "mode": "cage",
  "wireThickness": 2.0,
  "cage": {
    "latitudeCount": 8,
    "longitudeCount": 16,
    "showEquator": true,
    "showPoles": false
  }
}
```

**Note:** `CagePrimitive` should become `sphere + fill: cage`, not a separate class.

---

## 3. Transform System

### 3.1 Complete Transform Structure

```json
"transform": {
  "anchor": "center",
  "offset": { "x": 0, "y": 0, "z": 0 },
  "rotation": { "x": 0, "y": 45, "z": 0 },
  "scale": 1.0,
  "scaleXYZ": { "x": 1, "y": 1, "z": 1 },
  "facing": "fixed",
  "up": "world_up",
  "billboard": "none",
  "orbit": null
}
```

### 3.2 Position Anchors

| Anchor | Position | Description |
|--------|----------|-------------|
| `center` | (0, 1, 0) | Player chest height (DEFAULT) |
| `feet` | (0, 0, 0) | Player feet level |
| `head` | (0, 2, 0) | Player head level |
| `above` | (0, 3, 0) | Above player head |
| `below` | (0, -1, 0) | Below feet (underground) |
| `front` | (0, 1, 1) | In front of player |
| `back` | (0, 1, -1) | Behind player |
| `left` | (-1, 1, 0) | Left side |
| `right` | (1, 1, 0) | Right side |

**Note:** `offset` is ADDITIONAL to anchor. So `anchor: "feet"` + `offset: {y: 0.5}` = 0.5 blocks above feet.

### 3.3 Orientation (Facing)

| Facing | Description |
|--------|-------------|
| `fixed` | Stays in world orientation (DEFAULT) |
| `player_look` | Rotates to match player's look direction |
| `velocity` | Points in player's movement direction |
| `camera` | Always faces the camera |

### 3.4 Up Vector

| Up | Description |
|----|-------------|
| `world_up` | Y-axis is always up (DEFAULT) |
| `player_up` | Matches player's orientation |
| `velocity` | Up vector follows movement |
| `custom` | Uses rotation values |

### 3.5 Billboard Mode

| Billboard | Description |
|-----------|-------------|
| `none` | No billboarding (DEFAULT) |
| `full` | Always fully faces camera |
| `y_axis` | Rotates around Y to face camera |

### 3.6 Scale Options

```json
"scale": 1.0,              // Uniform scale
"scaleXYZ": { "x": 1, "y": 2, "z": 1 },  // Per-axis scale
"scaleWithRadius": true    // Scale proportional to field baseRadius
```

### 3.7 Dynamic Positioning (Advanced)

```json
"orbit": {
  "enabled": false,
  "radius": 2.0,
  "speed": 1.0,
  "axis": "Y",
  "phase": 0.0
}
```

| Property | Description |
|----------|-------------|
| `orbit` | Orbit around the anchor point |
| `trail` | (Future) Leave trail behind |

---

## 4. Compatibility Matrix

### Which Arrangements Apply to Which Shapes

| Shape | Produces Cells | Available Arrangements |
|-------|---------------|------------------------|
| sphere | QUAD | QuadPattern (filled_1, triangle_1, wave_1...) |
| ring | SEGMENT | SegmentPattern (full, alternating, sparse...) |
| disc | SECTOR | SectorPattern (full, half, quarters...) |
| prism (sides) | QUAD | QuadPattern |
| prism (caps) | SECTOR | SectorPattern |
| polyhedron | QUAD/TRIANGLE | QuadPattern or TrianglePattern |
| cylinder (sides) | QUAD | QuadPattern |
| cylinder (caps) | SECTOR | SectorPattern |

### Which Visibility Masks Apply to Which Shapes

| Shape | full | bands | stripes | checker | radial | gradient |
|-------|:----:|:-----:|:-------:|:-------:|:------:|:--------:|
| sphere | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| ring | ✓ | ✓ | - | ✓ | - | ✓ |
| disc | ✓ | ✓ | - | ✓ | ✓ | ✓ |
| prism | ✓ | ✓ | ✓ | ✓ | - | ✓ |
| polyhedron | ✓ | ✓ | ✓ | ✓ | - | - |
| cylinder | ✓ | ✓ | ✓ | ✓ | - | ✓ |

**Legend:** ✓ = supported, - = doesn't apply to this shape

**Notes:**
- `radial`: Works for shapes with a center (sphere, disc). For sphere, fades from poles.
- `gradient`: Works for most shapes. Direction determines fade axis.
- `bands/stripes`: Bands = horizontal, Stripes = vertical (relative to shape orientation)

### Which Fill Modes Apply to Which Shapes

| Shape | solid | wireframe | cage | points |
|-------|:-----:|:---------:|:----:|:------:|
| sphere | ✓ | ✓ | ✓ | ✓ |
| ring | ✓ | ✓ | - | ✓ |
| disc | ✓ | ✓ | - | ✓ |
| prism | ✓ | ✓ | ✓ | ✓ |
| polyhedron | ✓ | ✓ | ✓ | ✓ |
| cylinder | ✓ | ✓ | ✓ | ✓ |

**Notes:**
- `cage`: Structured grid lines. Only for shapes with natural lat/lon or edge grids.
- `points`: Render vertices only. Future feature but supported on all shapes.

---

## 5. Primitive Class Restructure

### Current (Problematic)
```
Primitive (sealed interface)
├── SolidPrimitive (abstract)
│   ├── SpherePrimitive
│   ├── PrismPrimitive
│   ├── StripesPrimitive    ← Should be config, not class
│   ├── DiscPrimitive
│   └── PolyhedronPrimitive
├── BandPrimitive (abstract)
│   ├── RingPrimitive
│   └── RingsPrimitive
└── StructuralPrimitive (abstract)
    ├── CagePrimitive       ← Should be config, not class
    └── BeamPrimitive       ← Replace with CylinderPrimitive
```

### Proposed (Clean)
```
Primitive (interface)
├── SpherePrimitive     → uses SphereShape
├── RingPrimitive       → uses RingShape
├── DiscPrimitive       → uses DiscShape
├── PrismPrimitive      → uses PrismShape
├── PolyhedronPrimitive → uses PolyhedronShape
├── CylinderPrimitive   → uses CylinderShape (replaces Beam)
├── TorusPrimitive      → uses TorusShape (future)
├── ConePrimitive       → uses ConeShape (future)
└── HelixPrimitive      → uses HelixShape (future)

REMOVED:
- StripesPrimitive → becomes SpherePrimitive + visibility: stripes
- CagePrimitive    → becomes SpherePrimitive + fill: cage
- BeamPrimitive    → becomes CylinderPrimitive
- RingsPrimitive   → becomes multiple RingPrimitive in layer

REMOVED HIERARCHY:
- SolidPrimitive, BandPrimitive, StructuralPrimitive
- These abstract classes add confusion without benefit
- All primitives now implement Primitive directly
```

---

## 6. JSON Structure (Proposed)

```json
{
  "id": "main_sphere",
  "type": "sphere",
  
  "shape": {
    "radius": 1.0,
    "latSteps": 32,
    "lonSteps": 64,
    "latStart": 0.0,
    "latEnd": 1.0
  },
  
  "transform": {
    "anchor": "center",
    "offset": { "x": 0, "y": 0, "z": 0 },
    "rotation": { "x": 0, "y": 0, "z": 0 },
    "scale": 1.0
  },
  
  "fill": {
    "mode": "solid",
    "wireThickness": 1.0
  },
  
  "visibility": {
    "mask": "bands",
    "count": 8,
    "thickness": 0.5
  },
  
  "arrangement": "filled_1",
  
  "appearance": {
    "color": "@primary",
    "alpha": { "min": 0.6, "max": 0.8 },
    "glow": 0.3
  },
  
  "animation": {
    "spin": { "axis": "Y", "speed": 0.02 },
    "pulse": { "scale": 0.1, "speed": 1.0 },
    "phase": 0.0
  }
}
```

---

## 7. Debug Command Knobs

All parameters exposed via `/fieldtest edit`:

### Shape-specific
```
/fieldtest edit shape.radius <value>
/fieldtest edit shape.latSteps <value>
/fieldtest edit shape.lonSteps <value>
/fieldtest edit shape.segments <value>
/fieldtest edit shape.sides <value>
/fieldtest edit shape.height <value>
/fieldtest edit shape.innerRadius <value>
/fieldtest edit shape.outerRadius <value>
```

### Transform
```
/fieldtest edit transform.anchor center|feet|head
/fieldtest edit transform.offset <x> <y> <z>
/fieldtest edit transform.rotation <x> <y> <z>
/fieldtest edit transform.scale <value>
```

### Fill
```
/fieldtest edit fill.mode solid|wireframe|cage|points
/fieldtest edit fill.wireThickness <value>
```

### Visibility
```
/fieldtest edit visibility.mask full|bands|stripes|checker
/fieldtest edit visibility.count <value>
/fieldtest edit visibility.thickness <value>
```

### Arrangement
```
/fieldtest edit arrangement filled_1|triangle_1|wave_1|...
/fieldtest shuffle next|prev|jump <index>|reset
/fieldtest shuffle type quad|segment|sector|edge
```

### Appearance & Animation
```
/fieldtest edit appearance.color <ref>
/fieldtest edit appearance.alpha <value>
/fieldtest edit appearance.glow <value>
/fieldtest edit animation.spin <speed>
/fieldtest edit animation.pulse <scale>
```

---

## 8. FollowMode Configuration

For personal fields attached to players:

```json
"followMode": {
  "enabled": true,
  "mode": "smooth",
  "playerOverride": true
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | boolean | true | false = static field at spawn position |
| `mode` | enum | "smooth" | SNAP, SMOOTH, GLIDE |
| `playerOverride` | boolean | true | Can player change mode in GUI? |

| Mode | Lerp Factor | Description |
|------|-------------|-------------|
| `snap` | 1.0 | Instant teleport to player |
| `smooth` | 0.35 | Smooth interpolation |
| `glide` | 0.2 | Slow, floating movement |

---

## 9. Primitive Linking

Primitives within a layer can be linked for coordinated behavior:

```json
"primitives": [
  {
    "id": "main_sphere",
    "type": "sphere",
    "shape": { "radius": 1.0 }
  },
  {
    "id": "outer_ring",
    "type": "ring",
    "link": {
      "radiusMatch": "main_sphere",
      "radiusOffset": 0.2
    }
  }
]
// Result: outer_ring.innerRadius = main_sphere.radius + 0.2 = 1.2
```

### Link Types

| Link | Description |
|------|-------------|
| `radiusMatch` | Match another primitive's radius + offset |
| `follow` | Follow another primitive's position |
| `mirror` | Mirror on specified axis |
| `phaseOffset` | Animation phase offset from another |
| `scaleWith` | Scale proportionally with another |

**Note:** Linking is included in Phase 1.

**Cycle Prevention:** Links are resolved in primitive declaration order. A primitive can only link to primitives declared BEFORE it in the `primitives` array. This makes circular references impossible by design.

---

## 10. Resolved Questions

| # | Question | Decision | Rationale |
|---|----------|----------|-----------|
| 1 | Keep abstract primitive hierarchy? | **NO** | Flatten - all implement Primitive directly |
| 2 | cage vs wireframe difference? | **Different** | Cage = structured grid, wireframe = all tessellation edges |
| 3 | Multi-part shapes (prism caps)? | **Separate config** | Per-part arrangement allows different patterns |
| 4 | Primitive linking? | **Phase 3** | Good idea, but adds complexity |
| 5 | GUI customization panel? | **Phase 2** | Keep in mind, but focus on code first |
| 6 | TrianglePattern for icosphere? | **Yes, Phase 1** | Needed for polyhedra with triangle faces |
| 7 | Dynamic patterns (procedural)? | **Phase 5** | Noise-based patterns are advanced |
| 8 | Pattern animation? | **Phase 3** | Moving bands/stripes |
| 9 | FollowMode structure? | **FollowModeConfig record** | With enabled, mode, playerOverride |
| 10 | FillConfig cage options? | **Nested: fill.cage.{}** | Cleaner separation |
| 11 | Primitive id required? | **Yes, required** | For linking and debugging |
| 12 | Expression parsing in linking? | **No** | Use simple offset fields |
| 13 | cellType on Primitive? | **No** | Get from shape.primaryCellType() |
| 14 | All Appearance fields Phase 1? | **Yes** | emissive, saturation, hueShift, etc. all Phase 1 |
| 15 | alphaPulse vs pulse separate? | **Yes** | pulse = scale, alphaPulse = alpha |


---

## 10.5 Layer/Primitive Combination Rules

When both Layer and Primitive define the same property:

### Animation (Spin/Rotation)
| Layer | Primitive | Result |
|-------|-----------|--------|
| `spin: { speed: 0.01 }` | `spin: { speed: 0.05 }` | **Additive**: Combined rotation |

Both spins apply simultaneously. Layer spin rotates the whole layer around the anchor. Primitive spin rotates the individual shape. This enables effects like "orbiting orbs that also spin."

### Appearance (Color/Alpha)
| Layer | Primitive | Result |
|-------|-----------|--------|
| `colorRef: "@primary"` | `color: "#FF0000"` | **Primitive overrides**: Red |
| `alpha: 0.8` | `alpha: 0.5` | **Primitive overrides**: 0.5 |
| `alpha: 0.8` | (not set) | **Layer default**: 0.8 |

Layer values are **defaults**. Primitive values **override** when present.

---

## 10.6 Color System

The color system provides flexible color references and theme support for all visual elements.

### Color Reference Formats

Appearance colors (`color`, `secondaryColor`) accept multiple formats:

| Format | Example | Description |
|--------|---------|-------------|
| `@role` | `"@primary"`, `"@glow"` | Theme role reference |
| `#hex` | `"#FF00AA"`, `"#F0A"` | Hex color (RGB or RRGGBB) |
| `#hex8` | `"#80FF00AA"` | Hex with alpha (AARRGGBB) |
| `name` | `"cyan"`, `"gold"` | Basic color name |
| `$slot` | `"$primaryColor"` | ColorConfig slot reference |

### ColorTheme

Defines a set of role-based colors:

```java
ColorTheme theme = ColorTheme.builder("cyber_green")
    .base(0xFF00FF88)
    .primary(0xFF00FF88)    // Main color
    .secondary(0xFF008844)  // Complementary
    .glow(0xFF44FFAA)       // Emissive/glow
    .beam(0xFF00FF88)       // Vertical beam
    .wire(0xFF00CC66)       // Wireframe
    .accent(0xFF88FFCC)     // Highlights
    .build();

// Or auto-derive from a single base color:
ColorTheme derived = ColorTheme.derive("custom", 0xFFFF00AA);
```

**Built-in Themes:** `CYBER_GREEN`, `CYBER_BLUE`, `CYBER_RED`, `CYBER_PURPLE`, `SINGULARITY`, `WHITE`

### ColorResolver

Resolves color references at render time:

```java
ColorResolver resolver = new ColorResolver(theme);

int color1 = resolver.resolve("@primary");     // → theme's primary
int color2 = resolver.resolve("@glow");        // → theme's glow
int color3 = resolver.resolve("#FF00AA");      // → parsed hex
int color4 = resolver.resolve("cyan");         // → 0xFF00FFFF
```

### Integration with Appearance

```
Appearance.primaryColor ("@glow")
        ↓
ColorResolver.resolve("@glow")
        ↓
0xFF44FFAA (actual ARGB int)
        ↓
Renderer uses this for vertex colors
```

### ColorMath Utilities

Static utilities for color manipulation:

| Method | Description |
|--------|-------------|
| `lighten(color, 0.2f)` | Lighten by 20% toward white |
| `darken(color, 0.3f)` | Darken by 30% toward black |
| `saturate(color, 0.1f)` | Increase saturation |
| `blend(a, b, 0.5f)` | Blend two colors |
| `withAlpha(color, 0.8f)` | Set alpha to 80% |
| `parseHex("#FF00AA")` | Parse hex string |
| `toHSL(color)` | Convert to HSL |

### Color in JSON

```json
{
  "appearance": {
    "color": "@primary",           // Use theme's primary
    "secondaryColor": "#FF8800",   // Explicit orange
    "alpha": 0.8,
    "glow": 0.5
  }
}
```

**Priority:** If both `colorRef` (on layer) and `color` (on primitive) are set, primitive wins.



---

## 11. Implementation Priority

### Phase 1: Core Restructure
1. Flatten primitive hierarchy (remove SolidPrimitive, BandPrimitive, StructuralPrimitive)
2. Remove StripesPrimitive → becomes visibility.mask=STRIPES on sphere
3. Remove CagePrimitive → becomes fill.mode=CAGE on any shape
4. Keep CylinderPrimitive (already renamed from Beam)
5. Implement complete Transform system (anchors, facing, billboard)
6. Implement FillConfig and VisibilityMask
7. Implement multi-part arrangement support
8. Implement TrianglePattern (for polyhedra with triangle faces)
9. **Implement External Influences: Bindings, Triggers, Lifecycle, CombatTracker**
10. Implement Primitive Linking (simple offset syntax)

### Phase 2: GUI & Polish
1. Design GUI customization panel
2. Add `stripes` (vertical) visibility mask
3. Add `radial` and `gradient` masks
4. Complete all pattern variants
5. Player-configurable followMode

### Phase 3: Advanced Features
1. Orbit and dynamic positioning
3. Pattern animation (moving bands)

### Phase 4: New Shapes (Future)
1. TorusShape / TorusPrimitive
2. ConeShape / ConePrimitive
3. HelixShape / HelixPrimitive

### Phase 5: Procedural (Future)
1. Noise-based patterns
2. Fractal patterns
3. Dynamic/animated masks

---

## 12. External Influences

Fields can react to external factors through three systems:

### 12.1 Reactive Bindings

Bind any field property to an external value source. **One binding per property.**

```json
"bindings": {
  "alpha": { 
    "source": "player.health_percent",
    "inputRange": [0, 1],
    "outputRange": [0.3, 1.0],
    "curve": "ease_out"
  },
  "scale": {
    "source": "player.speed",
    "inputRange": [0, 0.3],
    "outputRange": [1.0, 1.3]
  }
}
```

#### Available Sources (Phase 1)

| Source | Type | Description |
|--------|------|-------------|
| `player.health` | float (0-20) | Current health |
| `player.health_percent` | float (0-1) | Health as percentage |
| `player.armor` | int (0-20) | Armor points |
| `player.food` | int (0-20) | Food level |
| `player.speed` | float | Current movement speed |
| `player.is_sprinting` | bool | Sprinting state |
| `player.is_sneaking` | bool | Sneaking state |
| `player.is_flying` | bool | Flying state |
| `player.is_invisible` | bool | Has invisibility effect |
| `player.in_combat` | bool | Dealt/took damage within 5 seconds |
| `player.damage_taken` | float | Decaying damage amount |
| `field.age` | int | Ticks since field spawned |

#### Interpolation Curves

| Curve | Description |
|-------|-------------|
| `linear` | Straight line (default) |
| `ease_in` | Slow start, fast end |
| `ease_out` | Fast start, slow end |
| `ease_in_out` | Slow start and end |

#### Property Paths

Any property can be bound using dot notation:
- `alpha` - top-level alpha
- `appearance.glow` - nested glow
- `animation.spin.speed` - deeply nested

---

### 12.2 Event Triggers

React to game events with temporary visual effects.

```json
"triggers": [
  {
    "event": "player.damage",
    "effect": "flash",
    "color": "#FF0000",
    "duration": 6
  },
  {
    "event": "player.heal",
    "effect": "pulse",
    "scale": 1.2,
    "duration": 10
  }
]
```

#### Available Events (Phase 1)

| Event | When it fires |
|-------|---------------|
| `player.damage` | Player takes any damage |
| `player.heal` | Player heals |
| `player.death` | Player dies |
| `player.respawn` | Player respawns |
| `field.spawn` | Field is created |
| `field.despawn` | Field is removed |

#### Available Effects

| Effect | Params | Description |
|--------|--------|-------------|
| `flash` | color, duration | Brief color overlay |
| `pulse` | scale, duration | Scale up then back |
| `shake` | amplitude, duration | Rapid position jitter |
| `glow` | intensity, duration | Temporary glow boost |
| `colorShift` | color, duration | Temporary color change |

**Note:** Triggers run independently from bindings. Both can affect the field simultaneously.

---

### 12.3 Lifecycle

Controls how fields appear, disappear, and decay. External system calls spawn/despawn.

```json
"lifecycle": {
  "fadeIn": 20,
  "fadeOut": 40,
  "scaleIn": 20,
  "scaleOut": 40,
  "decay": {
    "rate": 0.01,
    "min": 0.2
  }
}
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `fadeIn` | int | 0 | Ticks to fade alpha from 0 to 1 |
| `fadeOut` | int | 0 | Ticks to fade alpha from 1 to 0 |
| `scaleIn` | int | 0 | Ticks to scale from 0 to 1 |
| `scaleOut` | int | 0 | Ticks to scale from 1 to 0 |
| `decay.rate` | float | 0 | Alpha loss per tick |
| `decay.min` | float | 0 | Minimum alpha (don't decay below) |

#### Lifecycle Flow

```
External: spawn() called
    │
    ▼
Field: fadeIn + scaleIn animation
    │
    ▼
Field: Active (bindings, triggers, decay running)
    │
    ▼
External: despawn() called
    │
    ▼
Field: fadeOut + scaleOut animation
    │
    ▼
Field: Removed
```

**Note:** Field doesn't know WHY it exists. External systems (buff manager, ability system) decide when to spawn/despawn. Field just knows HOW to appear/disappear gracefully.

---

### 12.4 Combat Tracking

For `player.in_combat` and `player.damage_taken` sources:

```java
// Player is "in combat" if:
// - Dealt damage within last 100 ticks (5 seconds), OR
// - Took damage within last 100 ticks (5 seconds)

// damage_taken source:
// - Stores last damage amount
// - Decays by multiplying 0.95 each tick
// - Resets on new damage
```

**Server-side:** Combat state is tracked on server, synced to clients for multiplayer. Other players see your field react to your health.

---

### 12.5 Future: World Interaction (Phase 3-4)

Documented for future reference:

| Feature | Phase | Description |
|---------|-------|-------------|
| Lighting | 3 | Field emits block light |
| Particles | 3 | Field emits particles at surface |
| Weather | 3 | Rain/wind affects field visually |
| Collision | 4 | Field blocks projectiles |

---

## 13. Summary

### The 5 Geometry Levels
1. **Shape Type** - base geometry (sphere, ring, disc, prism, polyhedron, cylinder)
2. **Cell Type** - what tessellation produces (QUAD, SEGMENT, SECTOR, EDGE, TRIANGLE)
3. **Arrangement** - vertex pattern within cells (filled_1, wave_1, pinwheel, etc.)
4. **Visibility Mask** - which cells to show (full, bands, stripes, checker, radial)
5. **Fill Mode** - how to render (solid, wireframe, cage, points)

### External Influence Systems
1. **Bindings** - property ← external value (health→alpha)
2. **Triggers** - event → temporary effect (damage→flash)
3. **Lifecycle** - fadeIn, fadeOut, scaleIn, scaleOut, decay

### Key Architecture Changes
- ✅ Stripes, Cage, Beam become **CONFIG** not **CLASSES**
- ✅ Flatten primitive hierarchy (all implement Primitive directly)
- ✅ Multi-part arrangement support (sides vs caps)
- ✅ Complete Transform system (anchors, facing, billboard, orbit)
- ✅ FollowMode as configurable object with playerOverride
- ✅ BeamConfig separate from CylinderPrimitive
- ✅ Multiple primitives per layer
- ✅ Primitive linking (Phase 1 - simple offset syntax)
- ✅ Reactive bindings (property ← external source)
- ✅ Event triggers (event → visual effect)
- ✅ Lifecycle with fade/scale in/out

### JSON Structure Overview
```
FieldDefinition
├── modifiers, prediction, beam, followMode
├── bindings: { property: { source, inputRange, outputRange, curve } }
├── triggers: [ { event, effect, params... } ]
├── lifecycle: { fadeIn, fadeOut, scaleIn, scaleOut, decay }
└── layers[]
    ├── rotation (static), spin (animated)
    └── primitives[]
        ├── type, shape
        ├── transform: { anchor, offset, rotation, scale, facing, billboard, orbit }
        ├── fill: { mode, wireThickness }
        ├── visibility: { mask, count, thickness }
        ├── arrangement: string | { default, caps, edges }
        ├── appearance: { color, alpha, glow }
        └── animation: { spin, pulse, phase }
```

---

*Architecture proposal v5.1 - Final review fixes applied.*

