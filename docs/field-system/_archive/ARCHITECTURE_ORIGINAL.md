# Field System Architecture

> **Last Updated:** December 6, 2024  
> **Version:** 0.2.0

---

## System Layers

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           APPLICATION LAYER                                 │
│  FieldCommand, FieldRegistry, FieldManager                                  │
│  Field-specific logic, commands, lifecycle                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                           FIELD LAYER                                       │
│  FieldDefinition, FieldInstance, FieldRenderer                              │
│  Primitives: Sphere, Ring, Cage, Stripes, Prism, Beam                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                           VISUAL LAYER (SHARED)                             │
│  Shapes, Tessellation, Transforms, Appearance, Animation                    │
│  Reusable by: fields, growth blocks, singularity, custom renderers          │
├─────────────────────────────────────────────────────────────────────────────┤
│                           RENDER UTILITIES (SHARED)                         │
│  VertexEmitter, MeshBuilder, RenderLayerFactory                             │
│  Color math, alpha blending, glow effects                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                           COLOR SYSTEM (SHARED)                             │
│  ColorConfig (existing), ColorTheme, ColorThemeRegistry                     │
│  Color math: lighten, darken, saturate, derive                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                           CONFIGURATION (SHARED)                            │
│  JSON loading, hot-reload, profile registries                               │
│  InfectionConfigRegistry integration                                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Shared Utilities

These are **not field-specific** - usable by any renderer in the project.

### Package: `net.cyberpunk042.visual`

```
visual/
├── color/
│   ├── ColorMath.java           # lighten, darken, saturate, blend
│   ├── ColorTheme.java          # theme definition record
│   ├── ColorThemeRegistry.java  # theme storage + auto-derivation
│   └── ColorResolver.java       # resolves @primary, $name, #hex
│
├── shape/
│   ├── Shape.java               # sealed interface
│   ├── SphereShape.java
│   ├── RingShape.java
│   ├── PrismShape.java
│   ├── PolyhedronShape.java     # cube, octahedron, icosahedron
│   └── ShapeRegistry.java       # lookup by name
│
├── mesh/
│   ├── Mesh.java                # vertices + indices
│   ├── MeshBuilder.java         # fluent mesh construction
│   ├── Tessellator.java         # shape → mesh conversion
│   ├── SphereTessellator.java   # lat/lon grid (current approach)
│   ├── RingTessellator.java     # circular segments
│   ├── PolyhedraTessellator.java
│   └── sphere/                  # Alternative sphere algorithms
│       ├── SphereAlgorithm.java # TYPE_A, TYPE_E, LAT_LON enum
│       ├── TypeASphere.java     # Overlapping cubes (accurate)
│       └── TypeESphere.java     # Rotated rectangles (efficient)
│
├── transform/
│   ├── Transform.java           # offset, rotation, scale
│   ├── TransformStack.java      # push/pop transforms
│   └── AnimatedTransform.java   # time-based interpolation
│
├── appearance/
│   ├── Appearance.java          # color, alphaRange, fill, pattern, glow
│   ├── AlphaRange.java          # min/max alpha for pulsing effects
│   ├── PatternConfig.java       # bands, checker patterns
│   ├── Gradient.java            # linear, radial gradients
│   └── FillMode.java            # SOLID, WIREFRAME, POINTS, TRANSLUCENT
│
├── animation/
│   ├── Animation.java           # combined animation config
│   ├── Spin.java                # rotation over time
│   ├── Pulse.java               # scale oscillation  
│   ├── Phase.java               # animation offset
│   ├── Axis.java                # X, Y, Z rotation axis enum
│   └── Animator.java            # applies animations to transforms
│
└── render/
    ├── VertexEmitter.java       # emit quads/tris to VertexConsumer
    ├── RenderLayerFactory.java  # create render layers
    ├── GlowRenderer.java        # additive blending helper
    └── WireframeRenderer.java   # edge rendering
```

### Usage Examples

**Any renderer can use these:**

```java
// Color math
int lighter = ColorMath.lighten(baseColor, 0.25f);
int darker = ColorMath.darken(baseColor, 0.30f);

// Theme resolution
ColorTheme theme = ColorThemeRegistry.get("crimson");
int primary = theme.resolve("@primary");

// Shape tessellation
Mesh mesh = Tessellator.tessellate(new SphereShape(1.0f), detail);

// Vertex emission
VertexEmitter.emitMesh(consumer, mesh, matrix, argb, light);

// Animation
Transform animated = Animator.apply(baseTransform, spin, worldTime);
```

---

## Field Layer

Built on top of shared utilities.

### Package: `net.cyberpunk042.field`

```
field/
├── primitive/
│   ├── Primitive.java           # interface
│   ├── SpherePrimitive.java
│   ├── RingPrimitive.java
│   ├── RingsPrimitive.java
│   ├── StripesPrimitive.java
│   ├── CagePrimitive.java
│   ├── PrismPrimitive.java
│   ├── BeamPrimitive.java
│   └── PrimitiveBuilder.java    # fluent API
│
├── definition/
│   ├── FieldDefinition.java     # immutable config
│   ├── FieldType.java           # enum: SHIELD, PERSONAL, etc.
│   ├── FieldBuilder.java        # fluent definition builder
│   └── FieldParser.java         # JSON → FieldDefinition
│
├── instance/
│   ├── FieldInstance.java       # runtime state
│   ├── FieldLifecycle.java      # spawn, update, despawn
│   └── FieldEffect.java         # push, pull, shield, damage
│
├── registry/
│   ├── FieldRegistry.java       # all definitions by type + id
│   ├── PresetRegistry.java      # built-in presets
│   └── FieldLoader.java         # loads from JSON files
│
└── render/
    ├── FieldRenderer.java       # main entry point
    ├── PrimitiveRenderer.java   # renders single primitive
    └── FieldRenderContext.java  # camera, matrices, time
```

---

## Data Flow

```
                    JSON Files
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                     FieldLoader                              │
│  Reads JSON, resolves theme, creates FieldDefinition         │
└─────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                    FieldRegistry                             │
│  Stores definitions by type + id                             │
└─────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                    FieldManager                              │
│  Creates FieldInstance from definition                       │
│  Manages active instances, lifecycle                         │
└─────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                    FieldRenderer                             │
│  For each primitive:                                         │
│    1. Resolve colors from theme                              │
│    2. Apply animation based on worldTime                     │
│    3. Tessellate shape → mesh                                │
│    4. Emit vertices                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## Key Classes

### ColorMath (Shared)

```java
public final class ColorMath {
    public static int lighten(int argb, float amount);
    public static int darken(int argb, float amount);
    public static int saturate(int argb, float amount);
    public static int desaturate(int argb, float amount);
    public static int blend(int a, int b, float factor);
    public static int withAlpha(int argb, float alpha);
    
    public static float[] toHSL(int argb);
    public static int fromHSL(float h, float s, float l, float a);
}
```

### ColorTheme

```java
public record ColorTheme(
    Identifier id,
    @Nullable Integer base,
    boolean autoDerive,
    Map<String, Integer> roles  // primary, secondary, glow, beam, wire
) {
    public int resolve(String role);  // "@primary" → ARGB
    
    public static ColorTheme auto(Identifier id, int base);
    public static ColorTheme explicit(Identifier id, Map<String, Integer> roles);
}
```

### AlphaRange

```java
public record AlphaRange(float min, float max) {
    public static final AlphaRange OPAQUE = new AlphaRange(1.0f, 1.0f);
    public static final AlphaRange TRANSLUCENT = new AlphaRange(0.4f, 0.8f);
    public static final AlphaRange FAINT = new AlphaRange(0.1f, 0.3f);
    
    public float at(float t);           // Interpolate for animation (t: 0-1)
    public float range();               // max - min
    public boolean isPulsing();         // min != max
}
```

### PatternConfig

```java
public record PatternConfig(PatternType type, int count, float thickness) {
    public enum PatternType { NONE, BANDS, CHECKER }
    
    public static final PatternConfig NONE = new PatternConfig(PatternType.NONE, 0, 0);
    
    public static PatternConfig bands(int count, float thickness);
    public static PatternConfig checker(int count);
    
    public boolean shouldRender(float latFrac, float lonFrac);  // For tessellation
}
```

### Appearance

```java
public record Appearance(
    String color,          // Theme reference (@primary) or hex (#FF0000)
    AlphaRange alpha,      // Min/max for pulsing
    FillMode fill,
    PatternConfig pattern, // Bands, checker, etc.
    float glow,
    float wireThickness
) {
    public static Appearance solid(String color);
    public static Appearance translucent(String color, float min, float max);
    public static Appearance banded(String color, int bands, float thickness);
    public static Appearance wireframe(String color, float thickness);
    public static Appearance glowing(String color, float glow);
}
```

### SphereShape

```java
public record SphereShape(
    float radius,
    int latSteps,
    int lonSteps,
    float latStart,    // 0.0 = north pole (default)
    float latEnd,      // 1.0 = south pole (default)
    float lonStart,    // 0.0 = start angle (default)
    float lonEnd       // 1.0 = full circle (default)
) implements Shape {
    public static SphereShape of(float radius);
    public static SphereShape of(float radius, int steps);
    public static SphereShape hemisphere(float radius, boolean top);
    public static SphereShape band(float radius, float start, float end);
    public static SphereShape arc(float radius, float lonStart, float lonEnd);
}
```

### Modifiers

```java
public record Modifiers(
    float radiusMultiplier,
    float strengthMultiplier,
    float alphaMultiplier,
    float spinMultiplier,
    float visualScale,      // Global render scale (default: 1.0)
    float tiltMultiplier,   // Movement-based tilt (default: 0.0)
    float swirlStrength,    // Surface distortion (default: 0.0)
    boolean inverted,
    boolean pulsing
) {
    public static final Modifiers DEFAULT = ...;
    
    public float applyRadius(float base);
    public float applyStrength(float base);
    public float applyAlpha(float base);
    public Modifiers combine(Modifiers other);
}
```

### Primitive

```java
public sealed interface Primitive permits 
    SpherePrimitive, RingPrimitive, RingsPrimitive,
    StripesPrimitive, CagePrimitive, PrismPrimitive, BeamPrimitive {
    
    Shape shape();
    Transform transform();
    Appearance appearance();
    Animation animation();
}
```

### Animation

```java
public record Animation(
    float spin,
    float pulse,
    float pulseAmount,
    float phase,
    float alphaPulse,       // Alpha oscillation speed
    float alphaPulseAmount, // Alpha oscillation amplitude
    Axis spinAxis           // Y (default), X, or Z
) {
    public static Animation none();
    public static Animation spinning(float speed);
    public static Animation pulsing(float speed, float amount);
    public static Animation spinningAndPulsing(float spin, float pulse, float amount);
    
    public float getRotation(float time);
    public float getScale(float time);
    public float getAlphaMultiplier(float time);  // For alpha pulsing
}

public enum Axis { X, Y, Z }
```

### FieldDefinition

```java
public record FieldDefinition(
    Identifier id,
    FieldType type,
    float baseRadius,
    @Nullable String themeId,
    List<FieldLayer> layers,
    Modifiers modifiers,
    List<FieldEffect> effects,
    @Nullable PredictionConfig prediction
) {
    public float effectiveRadius();
    public ColorTheme effectiveTheme();
    public static Builder builder(String id, FieldType type);
}
```

### VertexEmitter (Shared)

```java
public final class VertexEmitter {
    public static void emitQuad(
        VertexConsumer consumer,
        Matrix4f matrix,
        Vec3f[] corners,  // 4 vertices
        int argb,
        int light
    );
    
    public static void emitMesh(
        VertexConsumer consumer,
        Mesh mesh,
        Matrix4f matrix,
        int argb,
        int light
    );
    
    public static void emitWireframe(
        VertexConsumer consumer,
        Mesh mesh,
        Matrix4f matrix,
        int argb,
        float thickness,
        int light
    );
}
```

---

## Mesh Renderer Refactoring

### Existing Renderers to Refactor

| File | Current | Becomes |
|------|---------|---------|
| `GlowQuadEmitter.java` | Cube/quad emission | → `VertexEmitter` (shared) |
| `FieldMeshRenderer.java` | Shield mesh rendering | → Uses shared Tessellator + VertexEmitter |
| `GrowthRingFieldRenderer.java` | Ring rendering | → Uses `RingRenderer` (shared) |
| `GrowthBeamRenderer.java` | Beam quads | → Uses `BeamRenderer` (shared) |
| `BeaconBeamRenderer.java` | Beacon beams | → Uses `BeamRenderer` (shared) |
| `SingularityBlockEntityRenderer.java` | Singularity visuals | → Uses shared primitives |

### What Gets Extracted

From `GlowQuadEmitter`:
```java
emitQuad()       → VertexEmitter.emitQuad()
emitVertex()     → VertexEmitter.emitVertex()
renderCube()     → CubeRenderer.render() or CagePrimitive
FrameSlice       → visual/animation/FrameSlice
```

From `GrowthBeamRenderer`:
```java
addQuad()        → BeamRenderer.emitBeamQuad()
beam logic       → BeamPrimitive + BeamRenderer
```

From `GrowthRingFieldRenderer`:
```java
emitRingQuad()   → RingRenderer.emitSegment()
ring logic       → RingPrimitive + RingRenderer
```

From `FieldMeshRenderer`:
```java
tessellation     → SphereTessellator
layer rendering  → PrimitiveRenderer dispatches to shape renderers
```

### Migration Steps

1. Create `visual/render/VertexEmitter.java` - extract from GlowQuadEmitter
2. Create `visual/render/BeamRenderer.java` - extract from GrowthBeamRenderer
3. Create `visual/render/RingRenderer.java` - extract from GrowthRingFieldRenderer
4. Create `visual/mesh/SphereTessellator.java` - extract from FieldMeshRenderer
5. Update existing renderers to use shared utilities
6. Create new Primitive implementations using shared renderers

---

## Integration Points

### With Existing Systems

| System | Integration |
|--------|-------------|
| `ColorConfig` | ColorResolver wraps it, adds theme support |
| `InfectionConfigRegistry` | FieldLoader registers for reload |
| `ShieldFieldVisualManager` | Replaced by FieldManager + FieldRenderer |
| `SingularityVisualManager` | Config migrated to FieldDefinition |
| `GlowQuadEmitter` | Refactored into VertexEmitter (shared) |
| `GrowthBeamRenderer` | Uses shared BeamRenderer |
| `GrowthRingFieldRenderer` | Uses shared RingRenderer |

### Shared by Other Renderers

| Renderer | Uses |
|----------|------|
| Growth block visuals | Tessellator, VertexEmitter, ColorMath |
| Singularity effects | ColorTheme, Animation, GlowRenderer, TypeA/TypeE sphere |
| Guardian beams | BeamRenderer, VertexEmitter, ColorMath |
| Custom block entities | Shape, Mesh, Transform, VertexEmitter |
| Force field visuals | RingRenderer, BeamRenderer |
| Block sphere effects | SphereWorldGenerator (place/clear blocks) |
| LOD rendering | TypeE sphere for distant fields |

---

## Alpha Profiles

Initial field definitions to create, inspired by old profile system:

### Shield Types
| ID | Description | Key Features |
|----|-------------|--------------|
| `alpha_shield_default` | Basic protective bubble | Translucent sphere, subtle glow |
| `alpha_shield_cyber` | High-tech cyan field | Wireframe + solid layers, bright glow |
| `alpha_shield_crimson` | Aggressive red shield | Pulsing alpha, bands pattern |
| `alpha_shield_aurora` | Shifting colors | Multi-layer, phase offset between layers |

### Personal Types
| ID | Description | Key Features |
|----|-------------|--------------|
| `alpha_personal_aura` | Subtle player aura | Small radius, faint alpha, no spin |
| `alpha_personal_bubble` | Full protective bubble | Larger, prediction enabled, slight tilt |
| `alpha_personal_rings` | Orbital rings | Multiple RingPrimitives, different spin speeds |

### Force Types
| ID | Description | Key Features |
|----|-------------|--------------|
| `alpha_force_push` | Repelling force | PUSH effect, outward beam, pulsing |
| `alpha_force_pull` | Attracting force | PULL effect, inward glow |
| `alpha_force_barrier` | Wall/barrier | Hemisphere shape, high alpha |

### Growth Types
| ID | Description | Key Features |
|----|-------------|--------------|
| `alpha_growth_pulse` | Pulsing growth block | Slow pulse, organic colors |
| `alpha_growth_active` | Active growth state | Fast pulse, brighter, particle hints |

### Example Definition
```json
{
  "id": "alpha_shield_cyber",
  "type": "shield",
  "baseRadius": 12.0,
  "theme": "cyber_blue",
  "layers": [
    {
      "id": "wireframe",
      "primitive": "sphere",
      "appearance": {
        "color": "@wire",
        "alpha": { "min": 0.6, "max": 0.9 },
        "fill": "wireframe",
        "wireThickness": 1.5
      },
      "animation": { "spin": 0.02, "spinAxis": "Y" }
    },
    {
      "id": "glow",
      "primitive": "sphere",
      "shape": { "radius": 0.98 },
      "appearance": {
        "color": "@glow",
        "alpha": { "min": 0.1, "max": 0.3 },
        "fill": "translucent",
        "glow": 0.8
      },
      "animation": { "spin": -0.01, "pulse": 0.05, "pulseAmount": 0.02 }
    }
  ],
  "modifiers": {
    "visualScale": 1.0,
    "swirlStrength": 0.3
  },
  "prediction": {
    "enabled": true,
    "leadTicks": 2
  }
}
```

---

## Sphere Algorithms ✅ IMPLEMENTED

Three approaches for sphere rendering, selectable per primitive:

| Algorithm | Method | Elements | Best For | Status |
|-----------|--------|----------|----------|--------|
| `LAT_LON` | Lat/lon tessellation | Medium | Banded/patterned effects | ✅ `SphereTessellator` |
| `TYPE_A` | Overlapping cubes | 10-15 | Close-up, static | ✅ `TypeASphereRenderer` |
| `TYPE_E` | Rotated rectangles | 40-60 | Distant, LOD, animations | ✅ `TypeESphereRenderer` |

### Implementation Files

```
client/visual/render/sphere/
├── SphereAlgorithm.java      # Enum: LAT_LON, TYPE_A, TYPE_E + auto-LOD
├── TypeASphereRenderer.java  # Overlapping cubes (accurate)
└── TypeESphereRenderer.java  # Rotated rectangles (efficient)

sphere/
└── SphereModelGenerator.java # Static JSON model generation
```

### Usage: Runtime Rendering

```java
// Auto-select algorithm based on distance (LOD)
SphereAlgorithm algo = SphereAlgorithm.forDistance(distanceToCamera);
// < 16 blocks: TYPE_A, 16-64: LAT_LON, > 64: TYPE_E

// Context-aware selection (respects patterns/partial spheres)
SphereAlgorithm algo = SphereAlgorithm.forContext(distance, needsPatterns, needsPartial);

// Direct Type A rendering (accurate, close-up)
TypeASphereRenderer.builder()
    .radius(5.0f)
    .verticalLayers(6)
    .horizontalDetail(4)
    .color(0xFFFF6600)  // ARGB
    .build()
    .render(matrices, buffer, light, overlay);

// Direct Type E rendering (efficient, many spheres)
TypeESphereRenderer.builder()
    .radius(5.0f)
    .verticalLayers(6)
    .color(0xFF00CCFF)
    .build()
    .render(matrices, buffer, light, overlay);
```

### Usage: Static JSON Models

```java
// Generate sphere JSON model for blocks/items
JsonObject model = SphereModelGenerator.builder()
    .typeA()
    .radius(0.8)
    .verticalLayers(6)
    .horizontalDetail(4)
    .texture("the-virus-block:block/sphere_texture")
    .shade(false)
    .build();

// Save to resources
SphereModelGenerator.saveToFile(model,
    Path.of("src/main/resources/assets/.../models/block/sphere.json"));

// Quick generation for growth blocks
JsonObject growthSphere = SphereModelGenerator.forGrowthBlock(
    "the-virus-block:block/glow_magma", 0.8);
```

### World Generation Helper

`SphereWorldGenerator` for placing actual blocks (future):
- Force field that places blocks
- Explosion/clearing effects
- Terraforming features

```java
// Fill sphere with blocks
SphereWorldGenerator.filled(world, center, radius, state);

// Hollow sphere (dome)
SphereWorldGenerator.hollow(world, center, radius, thickness, state);

// Clear blocks in sphere
SphereWorldGenerator.clear(world, center, radius);
```

---

## Auto-Derivation Algorithm

```java
public static ColorTheme derive(int base) {
    float[] hsl = ColorMath.toHSL(base);
    
    return new ColorTheme(
        id,
        base,
        true,
        Map.of(
            "primary",   base,
            "secondary", ColorMath.darken(base, 0.30f),
            "glow",      ColorMath.lighten(base, 0.25f),
            "accent",    ColorMath.lighten(ColorMath.saturate(base, 0.10f), 0.40f),
            "beam",      ColorMath.lighten(base, 0.35f),
            "wire",      ColorMath.darken(base, 0.20f)
        )
    );
}
```

---

## Migration Plan

### Phase 1: Shared Utilities (Extract & Refactor)
- [ ] `ColorMath.java` - lighten, darken, saturate, blend
- [ ] `ColorTheme.java` + `ColorThemeRegistry.java`
- [ ] `VertexEmitter.java` - extract from `GlowQuadEmitter`
- [ ] `BeamRenderer.java` - extract from `GrowthBeamRenderer`
- [ ] `RingRenderer.java` - extract from `GrowthRingFieldRenderer`
- [ ] `SphereTessellator.java` - extract from `FieldMeshRenderer`
- [ ] `Transform.java`, `Appearance.java`, `Animation.java`

### Phase 2: Field Primitives
- [ ] `Primitive` interface
- [ ] `SpherePrimitive`, `RingPrimitive`, `RingsPrimitive`
- [ ] `StripesPrimitive`, `CagePrimitive`, `PrismPrimitive`
- [ ] `BeamPrimitive`
- [ ] `PrimitiveBuilder` fluent API

### Phase 3: Field System
- [ ] `FieldDefinition` record
- [ ] `FieldRegistry` - stores by type + id
- [ ] `FieldRenderer` - dispatches to primitive renderers
- [ ] `FieldParser` - JSON → FieldDefinition
- [ ] `FieldManager` - lifecycle, instances

### Phase 4: Commands
- [ ] `FieldCommand` unified tree
- [ ] `FieldTypeProvider` per-type handlers
- [ ] Remove old commands

### Phase 5: Profile Migration
- [ ] Convert `ShieldProfileConfig` → `FieldDefinition`
- [ ] Convert singularity config → `FieldDefinition`
- [ ] Convert `FieldProfile`, `ForceProfile` → unified format
- [ ] Update existing JSON files

### Phase 6: Cleanup
- [ ] Remove old managers
- [ ] Remove old config classes
- [ ] Remove unused mesh stores

### Phase 7: Optional Enhancements
- [ ] `TypeASphere.java`, `TypeESphere.java` - alternative sphere algorithms
- [ ] `SphereWorldGenerator.java` - block placement for force field effects
- [ ] LOD system - distance-based tessellation
- [ ] Mesh caching - performance optimization

---

## Open Design Questions

1. **Mesh caching:** Cache tessellated meshes per shape+detail?
2. **Instancing:** When to use instanced rendering for many primitives?
3. **Shader support:** Custom shaders for glow/distortion effects?
4. **LOD:** Distance-based tessellation reduction?

---

## Implementation Additions

The following classes were added during implementation beyond the original spec:

### Package Structure Note

Due to Minecraft's client/server split, rendering classes are in the client module:
- `client/visual/mesh/` - Mesh, tessellators (client-only due to vertex APIs)
- `client/visual/render/` - Renderers, vertex emission
- `client/field/render/` - Field-specific rendering

Shared data classes remain in main module:
- `visual/color/`, `visual/shape/`, `visual/transform/`, `visual/appearance/`, `visual/animation/`

### Additional Mesh Classes
```
client/visual/mesh/
├── PrimitiveType.java      # Enum for mesh primitive types (TRIANGLES, QUADS, LINES)
├── Vertex.java             # Vertex data structure with position, normal, UV, color
└── PrismTessellator.java   # Tessellates prism shapes
```

### Additional Renderers
```
client/visual/render/
├── PrimitiveRenderers.java # Registry mapping type strings to renderer instances
├── BeamRenderer.java       # Renders BeamPrimitive
├── RingRenderer.java       # Renders RingPrimitive  
├── RingsRenderer.java      # Renders RingsPrimitive (multiple rings)
├── PrismRenderer.java      # Renders PrismPrimitive
├── SphereRenderer.java     # Renders SpherePrimitive
├── CubeRenderer.java       # Cube/box rendering helper
├── LayerRenderer.java      # Renders FieldLayer with multiple primitives
├── FieldRenderLayers.java  # Custom RenderLayer definitions
├── MeshStyle.java          # Mesh rendering style enum
└── RenderOverrides.java    # Runtime override values for debugging
```

### Additional Shapes
```
visual/shape/
├── BeamShape.java          # Beam/cylinder shape
└── DiscShape.java          # Flat disc shape
```

### Additional Appearance/Animation
```
visual/appearance/
└── Alpha.java              # Alpha value wrapper

visual/animation/
└── FrameSlice.java         # Animation frame slice (extracted from GlowQuadEmitter)

visual/mesh/
└── TrianglePattern.java    # Triangle rendering pattern enum
```

### Field Client Classes
```
client/visual/
├── ClientFieldManager.java     # Client-side field state management
└── PersonalFieldTracker.java   # Tracks personal field for local player
```

---

## Changelog

| Date | Version | Changes |
|------|---------|---------|
| 2024-12-06 | 0.3.0 | Fixed deviations, documented implementation additions |
| 2024-12-06 | 0.2.0 | Added shared utilities layer, class definitions |
| 2024-12-06 | 0.1.0 | Initial design |
