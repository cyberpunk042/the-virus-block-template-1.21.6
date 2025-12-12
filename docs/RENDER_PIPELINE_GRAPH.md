# Field Renderer Pipeline Graph

## Legend
- ✅ = Path verified complete
- ❌ = Path broken/missing
- ⚠️ = Path exists but untested
- 🔄 = Circular/redundant path detected

---

## MASTER PIPELINE FLOW

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           GUI LAYER (Panels)                                │
├─────────────────────────────────────────────────────────────────────────────┤
│  QuickPanel, ShapeSubPanel, AppearanceSubPanel, FillSubPanel,               │
│  AnimationSubPanel, TransformSubPanel, VisibilitySubPanel, etc.             │
│                                                                             │
│  ┌─────────────────┐                                                        │
│  │ state.set(path) │ ─────────────────────────────────────────────────────► │
│  └─────────────────┘                                                        │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        STATE LAYER (FieldEditState)                         │
├─────────────────────────────────────────────────────────────────────────────┤
│  Fields annotated with @StateField, @PrimitiveComponent, @DefinitionField   │
│                                                                             │
│  sphere: SphereShape          @PrimitiveComponent("sphere")                 │
│  appearance: AppearanceState  @StateField (nested)                          │
│  fill: FillConfig             @PrimitiveComponent("fill")                   │
│  transform: Transform         @PrimitiveComponent("transform")              │
│  mask: VisibilityMask         @PrimitiveComponent("mask")                   │
│  spin: SpinConfig             @StateField (for Animation)                   │
│  modifiers: Modifiers         @DefinitionField("modifiers")                 │
│  ...                                                                        │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    BUILDER LAYER (DefinitionBuilder)                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  fromState(state) ──► buildDefinition(state)                                │
│                            │                                                │
│                            ├──► collectDefinitionFields()  → @DefinitionField│
│                            │         └─► modifiers, prediction, beam, etc. │
│                            │                                                │
│                            ├──► buildLayers()                               │
│                            │         └─► buildCurrentPrimitive()            │
│                            │                   │                            │
│                            │                   ├─► collectPrimitiveComponents()│
│                            │                   │       └─► @PrimitiveComponent │
│                            │                   │           (transform, fill,   │
│                            │                   │            mask, arrangement) │
│                            │                   │                            │
│                            │                   ├─► getCurrentShape()        │
│                            │                   │       └─► state.sphere(),  │
│                            │                   │           state.ring(), etc│
│                            │                   │                            │
│                            │                   ├─► buildAppearance()        │
│                            │                   │       └─► color → "#RRGGBB"│
│                            │                   │           alpha → AlphaRange│
│                            │                   │           glow, emissive   │
│                            │                   │                            │
│                            │                   └─► buildAnimation()         │
│                            │                           └─► spin, pulse,     │
│                            │                               wave, wobble     │
│                            │                                                │
│                            └──► new FieldDefinition(...)                    │
│                                                                             │
│  OUTPUT: FieldDefinition                                                    │
│    ├── id: String                                                           │
│    ├── type: FieldType                                                      │
│    ├── baseRadius: float                                                    │
│    ├── themeId: String                                                      │
│    ├── layers: List<FieldLayer>                                             │
│    │     └── FieldLayer                                                     │
│    │           ├── id, alpha, visible, blendMode                            │
│    │           ├── transform: Transform                                     │
│    │           ├── animation: Animation                                     │
│    │           └── primitives: List<Primitive>                              │
│    │                 └── SimplePrimitive                                    │
│    │                       ├── id, type                                     │
│    │                       ├── shape: Shape                                 │
│    │                       ├── transform: Transform                         │
│    │                       ├── fill: FillConfig                             │
│    │                       ├── visibility: VisibilityMask                   │
│    │                       ├── arrangement: ArrangementConfig               │
│    │                       ├── appearance: Appearance ◄── COLOR LIVES HERE  │
│    │                       ├── animation: Animation                         │
│    │                       └── link: PrimitiveLink                          │
│    ├── modifiers: Modifiers                                                 │
│    ├── prediction: PredictionConfig                                         │
│    ├── beam: BeamConfig                                                     │
│    └── followMode: FollowModeConfig                                         │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      RENDER LAYER (FieldRenderer)                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  render(matrices, consumers, definition, pos, scale, time, alpha)           │
│     │                                                                       │
│     ├─► Early exit if definition==null or alpha<=0.01                       │
│     ├─► Early exit if layers empty                                          │
│     │                                                                       │
│     ├─► resolveTheme(definition) → ColorTheme                               │
│     ├─► ColorResolver.fromTheme(theme)                                      │
│     │                                                                       │
│     ├─► FieldRenderLayers.solidTranslucent() → RenderLayer                  │
│     ├─► consumers.getBuffer(renderLayer) → VertexConsumer                   │
│     │                                                                       │
│     ├─► matrices.push()                                                     │
│     ├─► matrices.translate(position)                                        │
│     │                                                                       │
│     ├─► Apply modifiers (bobbing, breathing) via AnimationApplier           │
│     │                                                                       │
│     └─► FOR EACH layer:                                                     │
│              └─► LayerRenderer.render(layer, resolver, ...)                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      RENDER LAYER (LayerRenderer)                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  render(matrices, consumer, layer, resolver, fieldScale, time, alpha)       │
│     │                                                                       │
│     ├─► Early exit if !layer.visible()                                      │
│     ├─► Early exit if primitives empty                                      │
│     │                                                                       │
│     ├─► matrices.push()                                                     │
│     ├─► applyLayerTransform(layer.transform, fieldScale)                    │
│     ├─► AnimationApplier.apply(layer.animation, time)                       │
│     │                                                                       │
│     ├─► effectiveAlpha = alpha * layer.alpha()                              │
│     │                                                                       │
│     └─► FOR EACH primitive:                                                 │
│              └─► renderPrimitive(primitive, resolver, ...)                  │
│                       │                                                     │
│                       ├─► matrices.push()                                   │
│                       ├─► applyPrimitiveTransform(primitive.transform)      │
│                       ├─► AnimationApplier.apply(primitive.animation, time) │
│                       ├─► Apply primitive alpha from appearance.alpha()     │
│                       │                                                     │
│                       ├─► PrimitiveRenderers.get(primitive) → renderer      │
│                       └─► renderer.render(primitive, ..., resolver, ...)    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                  RENDER LAYER (AbstractPrimitiveRenderer)                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  render(primitive, matrices, consumer, light, time, resolver, overrides)    │
│     │                                                                       │
│     ├─► tessellate(primitive) → Mesh                                        │
│     │        └─► SphereRenderer.tessellate() calls SphereTessellator        │
│     │            └─► Returns Mesh with vertices                             │
│     │                                                                       │
│     ├─► Early exit if mesh==null or mesh.isEmpty()                          │
│     │                                                                       │
│     ├─► resolveColor(primitive, resolver, overrides, time)                  │
│     │        │                                                              │
│     │        ├─► Check overrides.colorOverride()                            │
│     │        ├─► Check animation.colorCycle()                               │
│     │        ├─► Get primitive.appearance().color() → "#RRGGBB"             │
│     │        ├─► resolver.resolve(colorRef) → ARGB int                      │
│     │        │        └─► ColorMath.parseHex("#RRGGBB") → 0xFFRRGGBB        │
│     │        └─► Apply appearance.alpha() if set                            │
│     │                 └─► ColorMath.withAlpha(color, alpha)                 │
│     │                                                                       │
│     ├─► Apply animated mask alpha (if visibility.animate())                 │
│     │                                                                       │
│     ├─► Get wave config from animation.wave()                               │
│     │                                                                       │
│     ├─► Get fill mode from primitive.fill().mode()                          │
│     │                                                                       │
│     └─► SWITCH on fill mode:                                                │
│              ├─► SOLID → emitSolid(mesh, color, ...)                        │
│              ├─► WIREFRAME → emitWireframe(mesh, color, ...)                │
│              ├─► CAGE → emitCage(mesh, color, ...)                          │
│              └─► POINTS → emitPoints(mesh, color, ...)                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      EMIT LAYER (emitSolid)                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  emitSolid(matrices, consumer, mesh, color, light, waveConfig, time)        │
│     │                                                                       │
│     ├─► new VertexEmitter(matrices, consumer)                               │
│     ├─► emitter.color(color)  ◄── COLOR IS SET HERE                         │
│     ├─► emitter.light(light)                                                │
│     ├─► emitter.wave(waveConfig, time) if configured                        │
│     │                                                                       │
│     └─► emitter.emit(mesh)                                                  │
│              │                                                              │
│              └─► FOR EACH triangle/quad in mesh:                            │
│                       └─► emitVertex(vertex)                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      VERTEX LAYER (VertexEmitter.emitVertex)                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  emitVertex(vertex)                                                         │
│     │                                                                       │
│     ├─► Apply wave displacement if waveConfig set                           │
│     │                                                                       │
│     ├─► Transform position: pos.mul(positionMatrix)                         │
│     ├─► Transform normal: normal.mul(normalMatrix)                          │
│     │                                                                       │
│     ├─► Decompose color:                                                    │
│     │        a = (color >> 24) & 0xFF                                       │
│     │        r = (color >> 16) & 0xFF                                       │
│     │        g = (color >> 8) & 0xFF                                        │
│     │        b = color & 0xFF                                               │
│     │                                                                       │
│     └─► consumer.vertex(x, y, z)                                            │
│              .color(r, g, b, a)  ◄── FINAL COLOR EMISSION                   │
│              .texture(u, v)                                                 │
│              .overlay(overlay)                                              │
│              .light(light)                                                  │
│              .normal(nx, ny, nz)                                            │
│                                                                             │
│  ════════════════════════════════════════════════════════════════════════   │
│  ║                    VERTEX SUBMITTED TO GPU                           ║   │
│  ════════════════════════════════════════════════════════════════════════   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## SEGMENT PATHS (GUI → Vertex)

### APPEARANCE SEGMENTS

| # | GUI Path | State Field | DefinitionBuilder | Primitive Field | Renderer Method | Vertex Output |
|---|----------|-------------|-------------------|-----------------|-----------------|---------------|
| A1 | `appearance.primaryColor` | `AppearanceState.primaryColor` | `buildAppearance()` → `color="#RRGGBB"` | `Appearance.color()` | `resolveColor()` → ARGB | `consumer.color(r,g,b,a)` |
| A2 | `appearance.alpha` | `AppearanceState.alpha` | `buildAppearance()` → `AlphaRange.of(alpha)` | `Appearance.alpha()` | `resolveColor()` → `withAlpha()` | `consumer.color(r,g,b,a)` |
| A3 | `appearance.glow` | `AppearanceState.glow` | `buildAppearance()` → `glow(float)` | `Appearance.glow()` | ⚠️ NOT USED | ❌ NO OUTPUT |
| A4 | `appearance.emissive` | `AppearanceState.emissive` | `buildAppearance()` → `emissive(float)` | `Appearance.emissive()` | ⚠️ NOT USED | ❌ NO OUTPUT |
| A5 | `appearance.saturation` | `AppearanceState.saturation` | `buildAppearance()` → `saturation(float)` | `Appearance.saturation()` | ⚠️ NOT USED | ❌ NO OUTPUT |
| A6 | `appearance.secondaryColor` | `AppearanceState.secondaryColor` | `buildAppearance()` → `secondaryColor="#RRGGBB"` | `Appearance.secondaryColor()` | ⚠️ NOT USED | ❌ NO OUTPUT |

### SHAPE SEGMENTS (Sphere)

| # | GUI Path | State Field | DefinitionBuilder | Primitive Field | Renderer Method | Vertex Output |
|---|----------|-------------|-------------------|-----------------|-----------------|---------------|
| S1 | `sphere.radius` | `SphereShape.radius` | `getCurrentShape()` → direct | `Shape` (SphereShape) | `tessellate()` | vertex positions |
| S2 | `sphere.latSteps` | `SphereShape.latSteps` | `getCurrentShape()` → direct | `Shape` (SphereShape) | `tessellate()` | mesh resolution |
| S3 | `sphere.lonSteps` | `SphereShape.lonSteps` | `getCurrentShape()` → direct | `Shape` (SphereShape) | `tessellate()` | mesh resolution |
| S4 | `sphere.algorithm` | `SphereShape.algorithm` | `getCurrentShape()` → direct | `Shape` (SphereShape) | `tessellate()` | tessellation method |

### FILL SEGMENTS

| # | GUI Path | State Field | DefinitionBuilder | Primitive Field | Renderer Method | Vertex Output |
|---|----------|-------------|-------------------|-----------------|-----------------|---------------|
| F1 | `fill.mode` | `FillConfig.mode` | `collectPrimitiveComponents()` | `FillConfig` | `emitSolid/Wireframe/Cage/Points` | render mode |
| F2 | `fill.wireThickness` | `FillConfig.wireThickness` | `collectPrimitiveComponents()` | `FillConfig` | `emitWireframe()` | line width |
| F3 | `fill.doubleSided` | `FillConfig.doubleSided` | `collectPrimitiveComponents()` | `FillConfig` | ⚠️ cull mode | ❌ |
| F4 | `fill.depthTest` | `FillConfig.depthTest` | `collectPrimitiveComponents()` | `FillConfig` | ⚠️ GL state | ❌ |

### ANIMATION SEGMENTS

| # | GUI Path | State Field | DefinitionBuilder | Primitive Field | Renderer Method | Vertex Output |
|---|----------|-------------|-------------------|-----------------|-----------------|---------------|
| N1 | `spin.speed` | `SpinConfig.speed` | `buildAnimation()` | `Animation.spin()` | `AnimationApplier.apply()` | matrix rotation |
| N2 | `spin.axis` | `SpinConfig.axis` | `buildAnimation()` | `Animation.spin()` | `AnimationApplier.apply()` | rotation axis |
| N3 | `pulse.speed` | `PulseConfig.speed` | `buildAnimation()` | `Animation.pulse()` | `AnimationApplier.apply()` | scale animation |
| N4 | `pulse.scale` | `PulseConfig.scale` | `buildAnimation()` | `Animation.pulse()` | `AnimationApplier.apply()` | scale amplitude |
| N5 | `alphaPulse.*` | `AlphaPulseConfig.*` | `buildAnimation()` | `Animation.alphaPulse()` | ⚠️ alpha modulation | ❌ |
| N6 | `wave.*` | `WaveConfig.*` | `buildAnimation()` | `Animation.wave()` | `emitter.wave()` | vertex displacement |
| N7 | `wobble.*` | `WobbleConfig.*` | `buildAnimation()` | `Animation.wobble()` | `AnimationApplier.apply()` | rotation wobble |
| N8 | `colorCycle.*` | `ColorCycleConfig.*` | `buildAnimation()` | `Animation.colorCycle()` | `resolveColor()` | animated color |

### TRANSFORM SEGMENTS

| # | GUI Path | State Field | DefinitionBuilder | Primitive Field | Renderer Method | Vertex Output |
|---|----------|-------------|-------------------|-----------------|-----------------|---------------|
| T1 | `transform.offset` | `Transform.offset` | `collectPrimitiveComponents()` | `Transform` | `applyPrimitiveTransform()` | translate |
| T2 | `transform.rotation` | `Transform.rotation` | `collectPrimitiveComponents()` | `Transform` | `applyPrimitiveTransform()` | rotate |
| T3 | `transform.scale` | `Transform.scale` | `collectPrimitiveComponents()` | `Transform` | `applyPrimitiveTransform()` | scale uniform |
| T4 | `transform.scaleXYZ` | `Transform.scaleXYZ` | `collectPrimitiveComponents()` | `Transform` | `applyPrimitiveTransform()` | scale non-uniform |
| T5 | `transform.anchor` | `Transform.anchor` | `collectPrimitiveComponents()` | `Transform` | ⚠️ offset calc | ❌ |
| T6 | `transform.billboard` | `Transform.billboard` | `collectPrimitiveComponents()` | `Transform` | ⚠️ camera-facing | ❌ |
| T7 | `transform.orbit.*` | `OrbitConfig.*` | `collectPrimitiveComponents()` | `Transform.orbit()` | ⚠️ orbit animation | ❌ |

### VISIBILITY/MASK SEGMENTS

| # | GUI Path | State Field | DefinitionBuilder | Primitive Field | Renderer Method | Vertex Output |
|---|----------|-------------|-------------------|-----------------|-----------------|---------------|
| V1 | `mask.mask` | `VisibilityMask.mask` | `collectPrimitiveComponents()` | `VisibilityMask` | `SphereTessellator` | vertex filtering |
| V2 | `mask.count` | `VisibilityMask.count` | `collectPrimitiveComponents()` | `VisibilityMask` | `SphereTessellator` | stripe count |
| V3 | `mask.thickness` | `VisibilityMask.thickness` | `collectPrimitiveComponents()` | `VisibilityMask` | `SphereTessellator` | stripe width |
| V4 | `mask.animate` | `VisibilityMask.animate` | `collectPrimitiveComponents()` | `VisibilityMask` | `render()` alpha mod | animated alpha |

### LAYER SEGMENTS

| # | GUI Path | State Field | DefinitionBuilder | Layer Field | Renderer Method | Vertex Output |
|---|----------|-------------|-------------------|-------------|-----------------|---------------|
| L1 | layer alpha | `FieldLayer.alpha` | `buildLayers()` | `FieldLayer.alpha()` | `effectiveAlpha` | alpha multiply |
| L2 | layer visible | `FieldLayer.visible` | `buildLayers()` | `FieldLayer.visible()` | early exit | skip render |
| L3 | layer blendMode | `FieldLayer.blendMode` | `buildLayers()` | `FieldLayer.blendMode()` | ⚠️ GL blend | ❌ |

### FIELD-LEVEL SEGMENTS

| # | GUI Path | State Field | DefinitionBuilder | Definition Field | Renderer Method | Vertex Output |
|---|----------|-------------|-------------------|------------------|-----------------|---------------|
| D1 | `modifiers.bobbing` | `Modifiers.bobbing` | `collectDefinitionFields()` | `Modifiers` | `AnimationApplier.applyModifiers()` | translate Y |
| D2 | `modifiers.breathing` | `Modifiers.breathing` | `Modifiers` | `Modifiers` | `AnimationApplier.applyModifiers()` | scale |

---

## VERIFICATION CHECKPOINTS

For each segment, we need to verify at these checkpoints:

```
[GUI] ──► [STATE] ──► [BUILDER] ──► [DEFINITION] ──► [RENDERER] ──► [EMITTER] ──► [VERTEX]
  │         │           │              │               │              │            │
  CP1       CP2         CP3            CP4             CP5            CP6          CP7
```

### Checkpoint Definitions:
- **CP1**: GUI widget calls `state.set(path, value)`
- **CP2**: State field holds correct value
- **CP3**: DefinitionBuilder reads and converts value
- **CP4**: FieldDefinition/Primitive contains correct value
- **CP5**: Renderer reads value from primitive
- **CP6**: Emitter receives correct value (color, transform, etc.)
- **CP7**: VertexConsumer.color/vertex called with correct args

---

## AUTOMATED VERIFICATION APPROACH

```java
// Add to FieldEditState
public interface PipelineCheckpoint {
    void checkpoint(String segment, int cp, String value);
}

// Usage in each layer:
// GUI: checkpoint("A1", 1, "color=" + value)
// State: checkpoint("A1", 2, "primaryColor=" + primaryColor)
// Builder: checkpoint("A1", 3, "hex=" + colorHex)
// Definition: checkpoint("A1", 4, "appearance.color=" + appearance.color())
// Renderer: checkpoint("A1", 5, "resolved=" + Integer.toHexString(color))
// Emitter: checkpoint("A1", 6, "emitColor=" + Integer.toHexString(color))
// Vertex: checkpoint("A1", 7, "r=" + r + ",g=" + g + ",b=" + b + ",a=" + a)
```

---

## KNOWN ISSUES

### Issue 1: Appearance.glow/emissive/saturation NOT USED
These fields are collected but never applied in the renderer.

### Issue 2: Billboard/Anchor NOT FULLY IMPLEMENTED  
Transform.billboard and anchor are set but may not be applied in PrimitiveRenderer.

### Issue 3: Layer blendMode NOT APPLIED
BlendMode is stored but not used in accurate mode (only Fast mode applies it).

### Issue 4: alphaPulse NOT INTEGRATED
AlphaPulse config exists but is not applied to vertex alpha.


