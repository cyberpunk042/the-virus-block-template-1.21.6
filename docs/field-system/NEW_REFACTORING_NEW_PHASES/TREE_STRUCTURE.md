# Project Tree Structure

> **Purpose:** Complete file inventory with locations, status tracking, and notes  
> **Status:** Phase 1 Planning  
> **Created:** December 8, 2024  
> **Ref:** 01_ARCHITECTURE.md, 02_CLASS_DIAGRAM.md, 03_PARAMETERS.md

---

## Status Legend

| Symbol | Meaning |
|--------|---------|
| ⬜ | To Create |
| 🔄 | In Progress |
| ✅ | Finished |
| ✏️ | To Modify (exists) |
| 🗑️ | To Archive/Remove |
| 📁 | Folder |
| 📄 | File |
| 🔮 | Future (Phase 4+) |

---

## Source Tree

```
src/main/java/net/cyberpunk042/
├── 📁 visual/                              ← NEW package structure
│   ├── 📁 pattern/
│   ├── 📁 transform/
│   ├── 📁 fill/
│   ├── 📁 visibility/
│   ├── 📁 animation/
│   ├── 📁 appearance/
│   ├── 📁 shape/
│   └── 📁 validation/                      ← ValueRange, @Range
├── 📁 field/
│   ├── 📁 primitive/
│   ├── 📁 instance/
│   ├── 📁 influence/
│   └── 📁 loader/
└── 📁 client/
    ├── 📁 field/render/
    └── 📁 visual/render/
```

---

## 1. ENUMS (`visual.*`)

### 1.1 Pattern Enums (`visual.pattern`)

| File | Status | TODO | Notes |
|------|--------|------|-------|
| 📄 `CellType.java` | ⬜ | F01 | QUAD, SEGMENT, SECTOR, EDGE, TRIANGLE |
| 📄 `VertexPattern.java` | ⬜ | F21 | Interface: id(), displayName(), cellType(), shouldRender(), getVertexOrder() |
| 📄 `QuadPattern.java` | ⬜ | F22 | 16 patterns: filled_1, triangle_1-4, wave_1, tooth_1, etc. |
| 📄 `SegmentPattern.java` | ⬜ | F23 | full, alternating, sparse, quarter, reversed, zigzag, dashed |
| 📄 `SectorPattern.java` | ⬜ | F24 | full, half, quarters, pinwheel, trisector, spiral, crosshair |
| 📄 `EdgePattern.java` | ⬜ | F25 | full, latitude, longitude, sparse, minimal, dashed, grid |
| 📄 `TrianglePattern.java` | ⬜ | F26 | full, alternating, inverted, sparse, fan, radial |
| 📄 `DynamicQuadPattern.java` | ⬜ | F27 | Runtime shuffle exploration |
| 📄 `DynamicSegmentPattern.java` | ⬜ | F28 | Runtime shuffle exploration |
| 📄 `DynamicSectorPattern.java` | ⬜ | F29 | Runtime shuffle exploration |
| 📄 `DynamicEdgePattern.java` | ⬜ | F30 | Runtime shuffle exploration |
| 📄 `DynamicTrianglePattern.java` | ⬜ | F30 | Runtime shuffle exploration |
| 📄 `ShuffleGenerator.java` | ⬜ | F30 | Generates permutations for dynamic patterns |
| 📄 `ArrangementConfig.java` | ⬜ | F51 | Record: default + 15 shape parts |
| 📄 `PatternResolver.java` | ⬜ | F55 | Maps String → VertexPattern for CellType |

### 1.2 Transform Enums (`visual.transform`)

| File | Status | TODO | Notes |
|------|--------|------|-------|
| 📄 `Anchor.java` | ⬜ | F02 | CENTER, FEET, HEAD, ABOVE, BELOW, FRONT, BACK, LEFT, RIGHT |
| 📄 `Facing.java` | ⬜ | F03 | FIXED, PLAYER_LOOK, VELOCITY, CAMERA |
| 📄 `Billboard.java` | ⬜ | F04 | NONE, FULL, Y_AXIS |
| 📄 `UpVector.java` | ⬜ | F05 | WORLD_UP, PLAYER_UP, VELOCITY, CUSTOM |
| 📄 `OrbitConfig.java` | ⬜ | F32 | Record: enabled, radius, speed, axis, phase |
| 📄 `Transform.java` | ⬜ | F33 | Record: anchor, offset, rotation, scale, facing, billboard, orbit |
| 📄 `TransformApplier.java` | ⬜ | F36 | Utility: applies Transform to MatrixStack |
| 📄 `AnchorResolver.java` | ⬜ | F37 | Converts Anchor enum to Vec3 offset |
| 📄 `FacingResolver.java` | ⬜ | F38 | Calculates facing rotation |
| 📄 `BillboardResolver.java` | ⬜ | F39 | Applies billboard rotation |
| 📄 `OrbitAnimator.java` | ⬜ | F40 | Calculates orbit position over time |

### 1.3 Fill Enums & Config (`visual.fill`)

| File | Status | TODO | Notes |
|------|--------|------|-------|
| 📄 `FillMode.java` | ⬜ | F06 | SOLID, WIREFRAME, CAGE, POINTS |
| 📄 `FillConfig.java` | ⬜ | F46 | Record: mode, wireThickness, doubleSided, depthTest, depthWrite, cage |
| 📄 `CageOptions.java` | ⬜ | F41 | Interface: common cage options |
| 📄 `SphereCageOptions.java` | ⬜ | F42 | Record: latitudeCount, longitudeCount, showEquator, showPoles |
| 📄 `PrismCageOptions.java` | ⬜ | F43 | Record: verticalLines, horizontalRings, showCaps |
| 📄 `CylinderCageOptions.java` | ⬜ | F44 | Record: verticalLines, horizontalRings, showCaps |
| 📄 `PolyhedronCageOptions.java` | ⬜ | F45 | Record: allEdges, faceOutlines |

### 1.4 Visibility Enums & Config (`visual.visibility`)

| File | Status | TODO | Notes |
|------|--------|------|-------|
| 📄 `MaskType.java` | ⬜ | F07 | FULL, BANDS, STRIPES, CHECKER, RADIAL, GRADIENT, CUSTOM |
| 📄 `VisibilityMask.java` | ⬜ | F48 | Record: mask, count, thickness (Phase 1) |
| 📄 `VisibilityMaskApplier.java` | ⬜ | F50 | Utility: shouldRenderCell(index, total, mask) |

### 1.5 Animation Enums & Config (`visual.animation`)

| File | Status | TODO | Notes |
|------|--------|------|-------|
| 📄 `Axis.java` | ⬜ | F08 | X, Y, Z, CUSTOM |
| 📄 `Waveform.java` | ⬜ | F09 | SINE, SQUARE, TRIANGLE_WAVE, SAWTOOTH |
| 📄 `SpinConfig.java` | ⬜ | F61 | Record: axis, speed, oscillate, range + NONE constant |
| 📄 `PulseConfig.java` | ⬜ | F62 | Record: scale, speed, waveform, min, max + NONE constant |
| 📄 `AlphaPulseConfig.java` | ⬜ | F63 | Record: speed, min, max, waveform |
| 📄 `WobbleConfig.java` | ⬜ | F64 | Record: amplitude (Vec3), speed, randomize (FUTURE) |
| 📄 `WaveConfig.java` | ⬜ | F65 | Record: amplitude, frequency, direction (FUTURE) |
| 📄 `ColorCycleConfig.java` | ⬜ | F66 | Record: colors (List), speed, blend (FUTURE) |
| 📄 `Animation.java` | ⬜ | F67 | Record: spin, pulse, phase, alphaPulse, colorCycle, wobble, wave |
| 📄 `AnimationApplier.java` | ⬜ | F69 | Utility: applies spin/pulse to MatrixStack |

### 1.6 Appearance Config (`visual.appearance`)

| File | Status | TODO | Notes |
|------|--------|------|-------|
| 📄 `AlphaRange.java` | ⬜ | F71 | Record: min, max + constant() factory |
| 📄 `Appearance.java` | ⬜ | F73 | Record: color, alpha, glow, emissive, saturation, brightness, hueShift, secondaryColor, colorBlend |
| 📄 `AppearanceResolver.java` | ⬜ | F76 | Utility: merges layer + primitive appearance |

### 1.7 Layer Config (`visual.layer`)

| File | Status | TODO | Notes |
|------|--------|------|-------|
| 📄 `BlendMode.java` | ⬜ | F10 | NORMAL, ADD (Phase 2: MULTIPLY, SCREEN) |

### 1.8 Validation Utilities (`visual.validation`)

| File | Status | TODO | Notes |
|------|--------|------|-------|
| 📄 `ValueRange.java` | ✅ | - | Enum: ALPHA, NORMALIZED, DEGREES, POSITIVE, SCALE, RADIUS, STEPS, SIDES, etc. |
| 📄 `Range.java` | ✅ | - | Annotation: @Range(ValueRange.ALPHA) for documenting valid ranges |

---

## 2. SHAPES (`visual.shape`)

| File | Status | TODO | Notes |
|------|--------|------|-------|
| 📄 `Shape.java` | ⬜ | F91 | Interface: getType(), getBounds(), primaryCellType(), getParts() |
| 📄 `PolyType.java` | ✅ | - | Exists: CUBE, OCTAHEDRON, ICOSAHEDRON, DODECAHEDRON, TETRAHEDRON |
| 📄 `SphereAlgorithm.java` | ✅ | - | Exists: LAT_LON, TYPE_A, TYPE_E |
| 📄 `SphereShape.java` | ✏️ | F92 | Add: latStart, latEnd, lonStart, lonEnd, subdivisions, getParts() |
| 📄 `RingShape.java` | ✏️ | F94 | Add: arcStart, arcEnd, height, twist, getParts() |
| 📄 `DiscShape.java` | ✏️ | F95 | Add: arcStart, arcEnd, innerRadius, rings, getParts() |
| 📄 `PrismShape.java` | ✏️ | F96 | Add: topRadius, twist, heightSegments, capTop, capBottom, getParts() |
| 📄 `PolyhedronShape.java` | ✏️ | F97 | Add: subdivisions, getParts() |
| 📄 `CylinderShape.java` | ✏️ | F98 | Add: topRadius, heightSegments, capTop, capBottom, openEnded, arc, getParts() |
| 📄 `TorusShape.java` | 🔮 | - | Phase 4: majorRadius, minorRadius, majorSegments, minorSegments, arc, twist |
| 📄 `ConeShape.java` | 🔮 | - | Phase 4: radiusBottom, radiusTop, height, segments, heightSegments, capBottom, arc |
| 📄 `HelixShape.java` | 🔮 | - | Phase 4: radius, height, turns, tubeRadius, segments, tubeSegments, direction |

---

## 3. FIELD (`field.*`)

### 3.1 Core Field (`field`)

| File | Status | TODO | Notes |
|------|--------|------|-------|
| 📄 `FieldType.java` | ✏️ | F13 | Remove: SINGULARITY, GROWTH, BARRIER |
| 📄 `FieldDefinition.java` | ✏️ | F86 | Add: bindings, triggers, lifecycle |
| 📄 `FieldLayer.java` | ✏️ | F87 | Add: rotation (static), visible, blendMode, order |
| 📄 `BeamConfig.java` | ⬜ | F83 | Record: enabled, innerRadius, outerRadius, color, height, glow, pulse |
| 📄 `Modifiers.java` | ⬜ | F84 | Record: visualScale, tilt, swirl, pulsing, bobbing, breathing |

### 3.2 Primitives (`field.primitive`)

| File | Status | TODO | Notes |
|------|--------|------|-------|
| 📄 `Primitive.java` | ✏️ | F101 | Interface: add link(), id() REQUIRED |
| 📄 `PrimitiveLink.java` | ⬜ | F111 | Record: radiusMatch, radiusOffset, follow, mirror, phaseOffset, scaleWith |
| 📄 `LinkResolver.java` | ⬜ | F113 | Resolves links at parse time |
| 📄 `SpherePrimitive.java` | ✏️ | F103 | Update to new Primitive interface |
| 📄 `RingPrimitive.java` | ✏️ | F104 | Update to new Primitive interface |
| 📄 `DiscPrimitive.java` | ✏️ | F105 | Update to new Primitive interface |
| 📄 `PrismPrimitive.java` | ✏️ | F106 | Update to new Primitive interface |
| 📄 `PolyhedronPrimitive.java` | ✏️ | F107 | Update to new Primitive interface |
| 📄 `CylinderPrimitive.java` | ✏️ | F108 | Update to new Primitive interface |
| 📄 `TorusPrimitive.java` | 🔮 | - | Phase 4 |
| 📄 `ConePrimitive.java` | 🔮 | - | Phase 4 |
| 📄 `HelixPrimitive.java` | 🔮 | - | Phase 4 |

### 3.3 To Archive (`field.primitive` → `_legacy/`)

| File | Status | TODO | Notes |
|------|--------|------|-------|
| 📄 `SolidPrimitive.java` | 🗑️ | F116 | Archive: unnecessary abstract class |
| 📄 `BandPrimitive.java` | 🗑️ | F117 | Archive: unnecessary abstract class |
| 📄 `StructuralPrimitive.java` | 🗑️ | F118 | Archive: unnecessary abstract class |
| 📄 `StripesPrimitive.java` | 🗑️ | F119 | Convert to: SpherePrimitive + visibility.mask=STRIPES |
| 📄 `CagePrimitive.java` | 🗑️ | F120 | Convert to: SpherePrimitive + fill.mode=CAGE |
| 📄 `RingsPrimitive.java` | 🗑️ | F121 | Convert to: multiple RingPrimitive in layer |

### 3.4 Field Instance (`field.instance`)

| File | Status | TODO | Notes |
|------|--------|------|-------|
| 📄 `FollowMode.java` | ⬜ | F14 | Enum: SNAP, SMOOTH, GLIDE |
| 📄 `LifecycleState.java` | ⬜ | F15 | Enum: SPAWNING, ACTIVE, DESPAWNING, COMPLETE |
| 📄 `FollowModeConfig.java` | ⬜ | F81 | Record: enabled, mode, playerOverride |
| 📄 `PredictionConfig.java` | ⬜ | F82 | Record: enabled, leadTicks, maxDistance, lookAhead, verticalBoost |
| 📄 `FieldInstance.java` | ✏️ | F89 | Add: lifecycleState, fadeProgress |
| 📄 `PersonalFieldInstance.java` | ✏️ | F90 | Update to use FollowModeConfig |
| 📄 `AnchoredFieldInstance.java` | ✏️ | - | May need updates |

### 3.5 External Influences (`field.influence`)

| File | Status | TODO | Notes |
|------|--------|------|-------|
| 📄 `FieldEvent.java` | ⬜ | F16 | Enum: PLAYER_DAMAGE, PLAYER_HEAL, PLAYER_DEATH, PLAYER_RESPAWN, FIELD_SPAWN, FIELD_DESPAWN |
| 📄 `TriggerEffect.java` | ⬜ | F17 | Enum: FLASH, PULSE, SHAKE, GLOW, COLOR_SHIFT + completesNaturally() |
| 📄 `InterpolationCurve.java` | ⬜ | F18 | Enum: LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT + apply(float t) |
| 📄 `BindingSource.java` | ⬜ | F143 | Interface: getId(), getValue(player), isBoolean() |
| 📄 `BindingSources.java` | ⬜ | F144 | Class: 12 static sources (health, armor, speed, etc.) |
| 📄 `BindingConfig.java` | ⬜ | F147 | Record: source, inputRange, outputRange, curve |
| 📄 `BindingResolver.java` | ⬜ | F148 | Evaluates binding, applies curve, maps ranges |
| 📄 `TriggerConfig.java` | ⬜ | F151 | Record: event, effect, duration, color, scale, amplitude, intensity |
| 📄 `ActiveTrigger.java` | ⬜ | F152 | Tracks active trigger state, tick countdown |
| 📄 `TriggerProcessor.java` | ⬜ | F155 | Listens for events, creates ActiveTriggers |
| 📄 `DecayConfig.java` | ⬜ | F159 | Record: rate, min |
| 📄 `LifecycleConfig.java` | ⬜ | F160 | Record: fadeIn, fadeOut, scaleIn, scaleOut, decay |
| 📄 `LifecycleManager.java` | ⬜ | F161 | Handles fade/scale animations on spawn/despawn |
| 📄 `CombatTracker.java` | ⬜ | F164 | Per-player: isInCombat(), getDamageTakenDecayed() |

### 3.6 Loading (`field.loader`)

| File | Status | TODO | Notes |
|------|--------|------|-------|
| 📄 `ReferenceResolver.java` | ⬜ | F124 | JSON $ref resolution with cache |
| 📄 `DefaultsProvider.java` | ⬜ | F127 | Smart defaults per type |
| 📄 `FieldLoader.java` | ✏️ | F129-130 | Integrate ReferenceResolver, DefaultsProvider |

---

## 4. CLIENT-SIDE (`client.*`)

### 4.1 Field Rendering (`client.field.render`)

| File | Status | TODO | Notes |
|------|--------|------|-------|
| 📄 `FieldRenderer.java` | ✏️ | F181-184 | Add: lifecycle fadeProgress, binding results, trigger effects, link resolution |
| 📄 `LayerRenderer.java` | ✏️ | F169-171 | Add: all Transform options, additive spin, visibility toggle |

### 4.2 Primitive Rendering (`client.visual.render`)

| File | Status | TODO | Notes |
|------|--------|------|-------|
| 📄 `PrimitiveRenderer.java` | ✏️ | - | Interface may need updates |
| 📄 `SphereRenderer.java` | ✏️ | F172-174 | Add: FillConfig, VisibilityMask, ArrangementConfig, VertexPattern |
| 📄 `RingRenderer.java` | ✏️ | F175 | Full config integration |
| 📄 `DiscRenderer.java` | ✏️ | F176 | Full config integration |
| 📄 `PrismRenderer.java` | ✏️ | F177 | Full config integration + multi-part |
| 📄 `PolyhedronRenderer.java` | ✏️ | F178 | Full config integration |
| 📄 `CylinderRenderer.java` | ✏️ | F179 | Full config integration + multi-part |
| 📄 `CageRenderer.java` | ✏️ | F180 | Use shape-specific CageOptions |
| 📄 `TorusRenderer.java` | 🔮 | - | Phase 4 |
| 📄 `ConeRenderer.java` | 🔮 | - | Phase 4 |
| 📄 `HelixRenderer.java` | 🔮 | - | Phase 4 |

---

## 5. DATA FOLDERS

### 5.1 JSON Reference Folders (`data/the-virus-block/`)

| Folder | Status | TODO | Purpose |
|--------|--------|------|---------|
| 📁 `field_definitions/` | ✅ | - | Complete field profiles (exists) |
| 📁 `field_shapes/` | ⬜ | F123 | Reusable shape configs |
| 📁 `field_appearances/` | ⬜ | F123 | Reusable appearance configs |
| 📁 `field_transforms/` | ⬜ | F123 | Reusable transform configs |
| 📁 `field_fills/` | ⬜ | F123 | Reusable fill configs |
| 📁 `field_masks/` | ⬜ | F123 | Reusable visibility mask configs |
| 📁 `field_arrangements/` | ⬜ | F123 | Reusable arrangement configs |
| 📁 `field_animations/` | ⬜ | F123 | Reusable animation configs |
| 📁 `field_layers/` | ⬜ | F123 | Complete layer templates |
| 📁 `field_primitives/` | ⬜ | F123 | Complete primitive templates |

### 5.2 Example Reference Files

| File | Status | Purpose |
|------|--------|---------|
| 📄 `field_shapes/smooth_sphere.json` | ⬜ | High-detail sphere |
| 📄 `field_shapes/dense_ring.json` | ⬜ | High-segment ring |
| 📄 `field_fills/wireframe_thin.json` | ⬜ | Thin wireframe preset |
| 📄 `field_fills/cage_dense.json` | ⬜ | Dense cage preset |
| 📄 `field_masks/horizontal_bands.json` | ⬜ | Horizontal bands preset |
| 📄 `field_masks/vertical_stripes.json` | ⬜ | Vertical stripes preset |
| 📄 `field_appearances/glowing_blue.json` | ⬜ | Blue glow preset |
| 📄 `field_appearances/translucent_red.json` | ⬜ | Red translucent preset |
| 📄 `field_animations/slow_spin.json` | ⬜ | Slow Y-axis spin |
| 📄 `field_animations/gentle_pulse.json` | ⬜ | Subtle pulse effect |

---

## 6. DOCUMENTATION

| File | Status | Notes |
|------|--------|-------|
| 📄 `00_TODO_DIRECTIVES.md` | ✅ | Guidelines for every task |
| 📄 `01_ARCHITECTURE.md` | ✅ | v5.1 - Architecture proposal |
| 📄 `02_CLASS_DIAGRAM.md` | ✅ | v7.1 - Class structure |
| 📄 `03_PARAMETERS.md` | ✅ | v5.1 - All parameters |
| 📄 `04_SHAPE_MATRIX.md` | ✅ | Shape parameter matrix |
| 📄 `TODO_LIST.md` | ✅ | ~430 row task list |
| 📄 `TREE_STRUCTURE.md` | ✅ | This file |
| 📄 `OBSERVATIONS.md` | ✅ | Discoveries and issues |
| 📄 `QUESTIONS.md` | ✅ | Open questions |
| 📄 `GAP.md` | ✅ | Gap analysis (closed) |

---

## 7. SUMMARY COUNTS

### By Status

| Status | Count | Description |
|--------|-------|-------------|
| ⬜ To Create | ~65 | New files |
| ✏️ To Modify | ~20 | Existing files need updates |
| 🗑️ To Archive | 6 | Move to _legacy/ |
| ✅ Exists | ~10 | Already done |
| 🔮 Future | ~9 | Phase 4+ |
| **Total** | **~110** | Java files |

### By Package

| Package | Files | Status |
|---------|-------|--------|
| `visual.pattern` | 15 | ⬜ All new |
| `visual.transform` | 11 | ⬜ All new |
| `visual.fill` | 7 | ⬜ All new |
| `visual.visibility` | 3 | ⬜ All new |
| `visual.animation` | 10 | ⬜ All new |
| `visual.appearance` | 3 | ⬜ All new |
| `visual.layer` | 1 | ⬜ BlendMode |
| `visual.validation` | 2 | ✅ ValueRange, @Range |
| `visual.shape` | 12 | ✏️ Mostly modify |
| `field` | 5 | ✏️ Mostly modify |
| `field.primitive` | 12 | ✏️/🗑️ Mix |
| `field.instance` | 6 | ⬜/✏️ Mix |
| `field.influence` | 14 | ⬜ All new |
| `field.loader` | 3 | ⬜/✏️ Mix |
| `client.field.render` | 2 | ✏️ Modify |
| `client.visual.render` | 10 | ✏️ Modify |

---

## 8. PHASE BREAKDOWN

### Phase 1 (Current)

| Category | Create | Modify | Archive |
|----------|--------|--------|---------|
| Enums | 18 | 1 | 0 |
| Records | 25 | 0 | 0 |
| Interfaces | 3 | 1 | 0 |
| Classes | 15 | 15 | 6 |
| Utilities | 10 | 0 | 0 |
| **Total** | **71** | **17** | **6** |

### Phase 4 (Future)

| Shape | Primitive | Renderer | Shape Record |
|-------|-----------|----------|--------------|
| Torus | TorusPrimitive | TorusRenderer | TorusShape |
| Cone | ConePrimitive | ConeRenderer | ConeShape |
| Helix | HelixPrimitive | HelixRenderer | HelixShape |

---

*Last updated: December 8, 2024*  
*Linked to: TODO_LIST.md, 02_CLASS_DIAGRAM.md*

