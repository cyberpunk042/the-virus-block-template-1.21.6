# Field Renderer Pipeline Audit

## Problem Statement
- **Fast Mode** (SimplifiedFieldRenderer): All controls work (color, radius, etc.)
- **Accurate Mode** (FieldRenderer via DefinitionBuilder): Only radius works

## Key Insight
Radius works because `getCurrentShape()` returns the **actual state object** (`state.sphere()`).
Color doesn't work because we build a **new Appearance object** from state values.

---

## PHASE 1: TRACE COLOR PATH

### Fast Mode (WORKING)
```
GUI: ColorButton → state.set("appearance.primaryColor", 0xFF00FFFF)
     ↓
State: FieldEditState.appearance = AppearanceState(primaryColor=0xFF00FFFF)
     ↓
Render: state.getInt("appearance.primaryColor") → 0xFF00FFFF
     ↓
Output: sphereVertex(..., r=0, g=255, b=255, a=255)
```

### Accurate Mode (BROKEN)
```
GUI: ColorButton → state.set("appearance.primaryColor", 0xFF00FFFF)
     ↓
State: FieldEditState.appearance = AppearanceState(primaryColor=0xFF00FFFF)
     ↓
DefinitionBuilder.buildAppearance():
  - state.appearance().primaryColor() → 0xFF00FFFF
  - Convert to hex: "#00FFFF"
  - Appearance.builder().color("#00FFFF").build()
     ↓
SimplePrimitive: appearance.color() = "#00FFFF" ✓ (LOGS CONFIRM THIS)
     ↓
AbstractPrimitiveRenderer.resolveColor():
  - primitive.appearance().color() → "#00FFFF"
  - resolver.resolve("#00FFFF") → 0xFF00FFFF
     ↓
emitSolid(matrices, consumer, mesh, 0xFF00FFFF, ...)
     ↓
VertexEmitter.emit() → consumer.vertex(...).color(0, 255, 255, 255)
```

### Question: Why doesn't color show if the pipeline is correct?

Possible reasons:
1. `emitSolid()` isn't being called
2. Mesh is empty/null (but then radius wouldn't work either)
3. Fill mode is not SOLID
4. VertexConsumer is different
5. Render layer/blend mode issue

---

## PHASE 2: SEGMENT AUDIT CHECKLIST

### SHAPE SEGMENTS (Probably Working)
| Segment | GUI Path | DefinitionBuilder | FieldRenderer | Status |
|---------|----------|-------------------|---------------|--------|
| sphere.radius | ✓ state.sphere() | getCurrentShape() | SphereRenderer | ✓ WORKS |
| sphere.latSteps | ✓ | getCurrentShape() | ? | ? |
| sphere.lonSteps | ✓ | getCurrentShape() | ? | ? |
| sphere.algorithm | ✓ | getCurrentShape() | ? | ? |

### APPEARANCE SEGMENTS (Need Verification)
| Segment | GUI Path | DefinitionBuilder | FieldRenderer | Status |
|---------|----------|-------------------|---------------|--------|
| appearance.primaryColor | ✓ | buildAppearance() | resolveColor() | ✗ NOT WORKING |
| appearance.alpha | ✓ | buildAppearance() | AlphaRange.max() | ? |
| appearance.glow | ✓ | buildAppearance() | Appearance.glow() | ? |
| appearance.emissive | ✓ | buildAppearance() | Appearance.emissive() | ? |
| appearance.saturation | ✓ | buildAppearance() | ? | ? |
| appearance.secondaryColor | ✓ | buildAppearance() | ? | ? |

### FILL SEGMENTS
| Segment | GUI Path | DefinitionBuilder | FieldRenderer | Status |
|---------|----------|-------------------|---------------|--------|
| fill.mode | ✓ | collectPrimitiveComponents | primitive.fill().mode() | ? |
| fill.wireThickness | ✓ | collectPrimitiveComponents | ? | ? |
| fill.doubleSided | ✓ | ? | ? | ? |
| fill.depthTest | ✓ | ? | ? | ? |

### ANIMATION SEGMENTS
| Segment | GUI Path | DefinitionBuilder | FieldRenderer | Status |
|---------|----------|-------------------|---------------|--------|
| spin.speed | ✓ | buildAnimation() | AnimationApplier.apply() | ? |
| spin.axis | ✓ | buildAnimation() | ? | ? |
| pulse.speed | ✓ | buildAnimation() | ? | ? |
| pulse.scale | ✓ | buildAnimation() | ? | ? |
| alphaPulse.* | ✓ | buildAnimation() | ? | ? |
| wave.* | ✓ | buildAnimation() | ? | ? |
| wobble.* | ✓ | buildAnimation() | ? | ? |
| colorCycle.* | ✓ | buildAnimation() | ? | ? |

### TRANSFORM SEGMENTS
| Segment | GUI Path | DefinitionBuilder | FieldRenderer | Status |
|---------|----------|-------------------|---------------|--------|
| transform.offset | ✓ | collectPrimitiveComponents | applyPrimitiveTransform | ? |
| transform.rotation | ✓ | collectPrimitiveComponents | ? | ? |
| transform.scale | ✓ | collectPrimitiveComponents | ? | ? |
| transform.anchor | ✓ | collectPrimitiveComponents | ? | ? |
| transform.billboard | ✓ | collectPrimitiveComponents | ? | ? |

### VISIBILITY/MASK SEGMENTS
| Segment | GUI Path | DefinitionBuilder | FieldRenderer | Status |
|---------|----------|-------------------|---------------|--------|
| mask.mask | ✓ | collectPrimitiveComponents | SphereTessellator | ? |
| mask.count | ✓ | ? | ? | ? |
| mask.thickness | ✓ | ? | ? | ? |
| mask.offset | ✓ | ? | ? | ? |
| mask.feather | ✓ | ? | ? | ? |
| mask.invert | ✓ | ? | ? | ? |
| mask.animate | ✓ | ? | animated mask alpha | ? |

### ARRANGEMENT SEGMENTS
| Segment | GUI Path | DefinitionBuilder | FieldRenderer | Status |
|---------|----------|-------------------|---------------|--------|
| arrangement.defaultPattern | ✓ | collectPrimitiveComponents | resolvePattern() | ? |
| arrangement.poles | ✓ | ? | ? | ? |
| arrangement.equator | ✓ | ? | ? | ? |

### LAYER-LEVEL SEGMENTS
| Segment | GUI Path | DefinitionBuilder | FieldRenderer | Status |
|---------|----------|-------------------|---------------|--------|
| layer.alpha | ✓ | buildLayers() | effectiveAlpha | ? |
| layer.visible | ✓ | buildLayers() | layer.visible() | ? |
| layer.blendMode | ✓ | buildLayers() | ? | ? |

### FIELD-LEVEL SEGMENTS
| Segment | GUI Path | DefinitionBuilder | FieldRenderer | Status |
|---------|----------|-------------------|---------------|--------|
| modifiers.bobbing | ✓ | collectDefinitionFields | applyModifiers | ? |
| modifiers.breathing | ✓ | collectDefinitionFields | applyModifiers | ? |
| beam.* | ✓ | collectDefinitionFields | ? | ? |
| prediction.* | ✓ | collectDefinitionFields | ? | ? |
| followMode.* | ✓ | collectDefinitionFields | ? | ? |

---

## PHASE 3: ACTION ITEMS

1. Add diagnostic log to `emitSolid()` to verify it's called ✓ (DONE)
2. Verify `resolveColor()` returns correct value
3. Verify VertexEmitter actually emits vertices
4. Check if mesh has vertices
5. Compare RenderLayer between Fast and Accurate modes

---

## Notes
- Fast mode uses inline tessellation in `SimplifiedFieldRenderer`
- Accurate mode uses `SphereTessellator` via `SphereRenderer`
- Both should use `FieldRenderLayers.solidTranslucent()`


