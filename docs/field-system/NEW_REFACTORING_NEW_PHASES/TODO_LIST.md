# Implementation TODO List

> **Purpose:** Master tracking of all Phase 1 implementation tasks  
> **Status:** Pre-implementation planning  
> **Created:** December 8, 2024  
> **⚠️ Use with [00_TODO_DIRECTIVES.md](./00_TODO_DIRECTIVES.md) for EVERY task**

---

## How to Use

### Option A: One Task at a Time
```
F01 → F01-CHK → F02 → F02-CHK → ...
```
- Complete F01
- Do F01-CHK (follow TODO_DIRECTIVES)
- Move to F02

### Option B: Batch with Python Script (Preferred)
```
[Python script does F01, F02, F03, F04, F05] → ONE combined CHK
```
- Write Python script that implements F01-F05
- Run script
- Mark F01-F05 AND F01-CHK through F05-CHK as ✅ together
- **ONE return to TODO_DIRECTIVES** after script execution
- No repetitive checks in the void!

### Batch-End CHK (CHK-xx)
- Full [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo)
- Compile check: `./gradlew compileJava`
- Review OBSERVATIONS.md
- Commit if clean

### Status Markers
- ⬜ → 🔄 → ✅
- Reference by ID (e.g., "see F01")

### Code Quality
- Every class gets Javadoc + FIELD channel logging
- Per [00_TODO_DIRECTIVES.md - Code Quality Standards](./00_TODO_DIRECTIVES.md#code-quality-standards)

> **💡 The CHK steps exist to ensure we don't skip the directives review.**  
> **When batching, one CHK covers all tasks in that batch execution.**

---

## Quick Stats

| Status | Count |
|--------|-------|
| ✅ Done | 4 |
| 🔄 In Progress | 0 |
| ⬜ Pending | ~430 |

> **Every Fxx task has a matching Fxx-CHK step = ~430 rows total**

---

## Pre-Implementation (Completed) ✅

| ID | Task | Status |
|----|------|--------|
| PRE-01 | Add `Logging.FIELD` channel | ✅ |
| PRE-02 | Add `Context.alwaysChat()` method | ✅ |
| PRE-03 | Add `FormattedContext.alwaysChat()` | ✅ |
| PRE-04 | Update `LogOutput.emit()` forceChat | ✅ |

---

## Phase 1: Core Restructure

---

### Batch 1: Foundation Enums Part 1 (F01-F10)

> **Ref:** CLASS_DIAGRAM §18  
> **Package:** `net.cyberpunk042.visual.*`

| ID | Task | Status | Package |
|----|------|--------|---------|
| F01 | `CellType` enum: QUAD, SEGMENT, SECTOR, EDGE, TRIANGLE | ⬜ | visual.pattern |
| F01-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F02 | `Anchor` enum: CENTER, FEET, HEAD, ABOVE, BELOW, FRONT, BACK, LEFT, RIGHT | ⬜ | visual.transform |
| F02-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F03 | `Facing` enum: FIXED, PLAYER_LOOK, VELOCITY, CAMERA | ⬜ | visual.transform |
| F03-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F04 | `Billboard` enum: NONE, FULL, Y_AXIS | ⬜ | visual.transform |
| F04-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F05 | `UpVector` enum: WORLD_UP, PLAYER_UP, VELOCITY, CUSTOM | ⬜ | visual.transform |
| F05-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F06 | `FillMode` enum: SOLID, WIREFRAME, CAGE, POINTS | ⬜ | visual.fill |
| F06-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F07 | `MaskType` enum: FULL, BANDS, STRIPES, CHECKER, RADIAL, GRADIENT, CUSTOM | ⬜ | visual.visibility |
| F07-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F08 | `Axis` enum: X, Y, Z, CUSTOM | ⬜ | visual.animation |
| F08-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F09 | `Waveform` enum: SINE, SQUARE, TRIANGLE_WAVE, SAWTOOTH | ⬜ | visual.animation |
| F09-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F10 | `BlendMode` enum: NORMAL, ADD (Phase 2: MULTIPLY, SCREEN) | ⬜ | visual.layer |
| F10-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-01 | ⚠️ **BATCH 1 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 2: Foundation Enums Part 2 (F11-F20)

> **Ref:** CLASS_DIAGRAM §18  
> **Package:** Various

| ID | Task | Status | Package |
|----|------|--------|---------|
| F11 | `PolyType` enum: CUBE, OCTAHEDRON, ICOSAHEDRON, DODECAHEDRON, TETRAHEDRON | ⬜ | visual.shape |
| F11-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F12 | `SphereAlgorithm` enum: LAT_LON, TYPE_A, TYPE_E | ⬜ | visual.shape |
| F12-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F13 | `FieldType` enum: SHIELD, PERSONAL, FORCE, AURA, PORTAL (remove SINGULARITY, GROWTH, BARRIER) | ⬜ | field |
| F13-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F14 | `FollowMode` enum: SNAP, SMOOTH, GLIDE | ⬜ | field.instance |
| F14-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F15 | `LifecycleState` enum: SPAWNING, ACTIVE, DESPAWNING, COMPLETE | ⬜ | field.instance |
| F15-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F16 | `FieldEvent` enum: PLAYER_DAMAGE, PLAYER_HEAL, PLAYER_DEATH, PLAYER_RESPAWN, FIELD_SPAWN, FIELD_DESPAWN | ⬜ | field.influence |
| F16-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F17 | `TriggerEffect` enum: FLASH, PULSE, SHAKE, GLOW, COLOR_SHIFT + `completesNaturally()` method | ⬜ | field.influence |
| F17-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F18 | `InterpolationCurve` enum: LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT + `apply(float t)` method | ⬜ | field.influence |
| F18-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F19 | `HeightDirection` enum: CW, CCW (for helix) | ⬜ | visual.shape |
| F19-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F20 | Review: Verify all 18 enums created match CLASS_DIAGRAM §18 | ⬜ | - |
| F20-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-02 | ⚠️ **BATCH 2 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 3: Pattern Enums & Interface (F21-F30)

> **Ref:** CLASS_DIAGRAM §6, ARCHITECTURE §Level 3  
> **Package:** `net.cyberpunk042.visual.pattern`

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F21 | `VertexPattern` interface: id(), displayName(), cellType(), shouldRender(index, total), getVertexOrder() | ⬜ | Core interface |
| F21-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F22 | `QuadPattern` enum: filled_1, triangle_1-4, wave_1, tooth_1, parallelogram_1-2, stripe_1, etc. (16 patterns) | ⬜ | Implements VertexPattern |
| F22-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F23 | `SegmentPattern` enum: full, alternating, sparse, quarter, reversed, zigzag, dashed | ⬜ | For rings |
| F23-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F24 | `SectorPattern` enum: full, half, quarters, pinwheel, trisector, spiral, crosshair | ⬜ | For discs |
| F24-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F25 | `EdgePattern` enum: full, latitude, longitude, sparse, minimal, dashed, grid | ⬜ | For wireframe |
| F25-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F26 | `TrianglePattern` enum: full, alternating, inverted, sparse, fan, radial | ⬜ | For polyhedra |
| F26-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F27 | `DynamicQuadPattern` class for shuffle exploration | ⬜ | Runtime permutations |
| F27-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F28 | `DynamicSegmentPattern` class | ⬜ | Runtime permutations |
| F28-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F29 | `DynamicSectorPattern` class | ⬜ | Runtime permutations |
| F29-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F30 | `DynamicEdgePattern`, `DynamicTrianglePattern`, `ShuffleGenerator` | ⬜ | Complete set |
| F30-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-03 | ⚠️ **BATCH 3 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 4: Transform & Position Config (F31-F40)

> **Ref:** CLASS_DIAGRAM §5, ARCHITECTURE §3  
> **Package:** `net.cyberpunk042.visual.transform`

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F31 | `Vec3` record or utility (if not using Minecraft's Vec3d) | ⬜ | For offset, rotation |
| F31-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F32 | `OrbitConfig` record: enabled, radius, speed, axis, phase | ⬜ | Dynamic positioning |
| F32-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F33 | `Transform` record: anchor, offset, rotation, scale, scaleXYZ, scaleWithRadius, facing, up, billboard, inheritRotation, orbit | ⬜ | Complete rewrite |
| F33-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F34 | `Transform.Builder` for fluent construction | ⬜ | Builder pattern |
| F34-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F35 | `Transform.DEFAULT` static constant | ⬜ | Smart default |
| F35-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F36 | `TransformApplier` utility: applies Transform to MatrixStack | ⬜ | Runtime application |
| F36-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F37 | `AnchorResolver`: converts Anchor enum to Vec3 offset | ⬜ | Anchor → position |
| F37-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F38 | `FacingResolver`: calculates facing rotation from player/camera | ⬜ | Dynamic facing |
| F38-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F39 | `BillboardResolver`: applies billboard rotation | ⬜ | Billboard mode |
| F39-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F40 | `OrbitAnimator`: calculates orbit position over time | ⬜ | Orbit animation |
| F40-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-04 | ⚠️ **BATCH 4 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 5: Fill & Visibility Config (F41-F50)

> **Ref:** CLASS_DIAGRAM §5, ARCHITECTURE §Level 4-5  
> **Package:** `net.cyberpunk042.visual.fill`, `net.cyberpunk042.visual.visibility`

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F41 | `CageOptions` interface (common: lineWidth, showEdges) | ⬜ | Base interface |
| F41-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F42 | `SphereCageOptions` record: latitudeCount, longitudeCount, showEquator, showPoles | ⬜ | Sphere-specific |
| F42-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F43 | `PrismCageOptions` record: verticalLines, horizontalRings, showCaps | ⬜ | Prism-specific |
| F43-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F44 | `CylinderCageOptions` record: verticalLines, horizontalRings, showCaps | ⬜ | Cylinder-specific |
| F44-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F45 | `PolyhedronCageOptions` record: allEdges, faceOutlines | ⬜ | Polyhedron-specific |
| F45-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F46 | `FillConfig` record: mode, wireThickness, doubleSided, depthTest, depthWrite, cage | ⬜ | Nested cage options |
| F46-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F47 | `FillConfig.Builder` + `FillConfig.SOLID_DEFAULT` | ⬜ | Builder + default |
| F47-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F48 | `VisibilityMask` record: mask, count, thickness (Phase 1 fields) | ⬜ | Phase 2 adds offset, invert |
| F48-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F49 | `VisibilityMask.FULL` static constant | ⬜ | Default |
| F49-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F50 | `VisibilityMaskApplier`: shouldRenderCell(index, total, mask) | ⬜ | Runtime filtering |
| F50-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-05 | ⚠️ **BATCH 5 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 6: Arrangement & Pattern Matching (F51-F60)

> **Ref:** ARCHITECTURE §Level 3, CLASS_DIAGRAM §10  
> **Package:** `net.cyberpunk042.visual.pattern`

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F51 | `ArrangementConfig` record: default + all 15 shape parts | ⬜ | Multi-part support |
| F51-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F52 | `ArrangementConfig.of(String)` factory for simple form | ⬜ | "wave_1" → ArrangementConfig |
| F52-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F53 | `ArrangementConfig.getPatternFor(String part, CellType)` | ⬜ | Resolve per-part |
| F53-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F54 | Pattern mismatch handler: log error + alwaysChat | ⬜ | Per ARCH §Pattern Mismatch |
| F54-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F55 | `PatternResolver`: maps String → VertexPattern for CellType | ⬜ | "filled_1" → QuadPattern.FILLED_1 |
| F55-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F56 | Pattern fallback: if part not specified, use `default` | ⬜ | Fallback logic |
| F56-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F57 | All 15 shape parts documented in ArrangementConfig Javadoc | ⬜ | Documentation |
| F57-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F58 | Test: Sphere with main=wave_1, poles=filled_1 | ⬜ | Multi-part test |
| F58-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F59 | Test: Pattern mismatch shows chat message | ⬜ | Error handling test |
| F59-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F60 | Review: All pattern enums have proper getVertexOrder() | ⬜ | Verification |
| F60-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-06 | ⚠️ **BATCH 6 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 7: Animation Config Records (F61-F70)

> **Ref:** CLASS_DIAGRAM §7, PARAMETERS §10  
> **Package:** `net.cyberpunk042.visual.animation`

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F61 | `SpinConfig` record: axis, speed, oscillate, range | ⬜ | + `SpinConfig.NONE` |
| F61-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F62 | `PulseConfig` record: scale, speed, waveform, min, max | ⬜ | + `PulseConfig.NONE` |
| F62-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F63 | `AlphaPulseConfig` record: speed, min, max, waveform | ⬜ | Alpha animation |
| F63-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F64 | `WobbleConfig` record: amplitude (Vec3), speed, randomize | ⬜ | Random jitter (FUTURE) |
| F64-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F65 | `WaveConfig` record: amplitude, frequency, direction | ⬜ | Surface ripple (FUTURE) |
| F65-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F66 | `ColorCycleConfig` record: colors (List), speed, blend | ⬜ | Color animation (FUTURE) |
| F66-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F67 | `Animation` record: spin, pulse, phase, alphaPulse, colorCycle, wobble, wave | ⬜ | Container for all |
| F67-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F68 | `Animation.NONE` static constant | ⬜ | Default |
| F68-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F69 | `AnimationApplier`: applies spin/pulse to MatrixStack over time | ⬜ | Runtime |
| F69-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F70 | Additive animation: Layer spin + Primitive spin combine | ⬜ | Per ARCH §10.5 |
| F70-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-07 | ⚠️ **BATCH 7 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 8: Appearance Config Records (F71-F80)

> **Ref:** CLASS_DIAGRAM §7, PARAMETERS §9  
> **Package:** `net.cyberpunk042.visual.appearance`

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F71 | `AlphaRange` record: min, max | ⬜ | For pulsing alpha |
| F71-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F72 | `AlphaRange.constant(float)` factory | ⬜ | Single value → range |
| F72-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F73 | `Appearance` record: color, alpha, glow, emissive, saturation, brightness, hueShift, secondaryColor, colorBlend | ⬜ | All visual properties |
| F73-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F74 | `Appearance.Builder` + `Appearance.DEFAULT` | ⬜ | Builder + default |
| F74-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F75 | Override logic: Primitive appearance overrides Layer appearance | ⬜ | Per ARCH §10.5 |
| F75-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F76 | `AppearanceResolver`: merges layer + primitive appearance | ⬜ | Runtime merge |
| F76-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F77 | Color reference resolution: "@primary" → theme color | ⬜ | Uses ColorResolver |
| F77-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F78 | HSV modifiers: saturation, brightness, hueShift application | ⬜ | Color manipulation |
| F78-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F79 | Secondary color blending: colorBlend interpolation | ⬜ | Gradient support |
| F79-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F80 | Test: Appearance with all fields renders correctly | ⬜ | Visual test |
| F80-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-08 | ⚠️ **BATCH 8 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 9: Field Definition Config (F81-F90)

> **Ref:** CLASS_DIAGRAM §1, §9  
> **Package:** `net.cyberpunk042.field`, `net.cyberpunk042.field.instance`

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F81 | `FollowModeConfig` record: enabled, mode, playerOverride | ⬜ | Wraps FollowMode |
| F81-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F82 | `PredictionConfig` record: enabled, leadTicks, maxDistance, lookAhead, verticalBoost | ⬜ | Movement prediction |
| F82-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F83 | `BeamConfig` record: enabled, innerRadius, outerRadius, color, height, glow, pulse (PulseConfig) | ⬜ | Central beam |
| F83-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F84 | `Modifiers` record: visualScale, tilt, swirl, pulsing, bobbing, breathing | ⬜ | Global modifiers |
| F84-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F85 | `Modifiers.DEFAULT` static constant | ⬜ | Default values |
| F85-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F86 | Update `FieldDefinition`: add bindings, triggers, lifecycle | ⬜ | New fields |
| F86-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F87 | Update `FieldLayer`: add rotation (static), visible, blendMode, order | ⬜ | New fields |
| F87-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F88 | Layer combination: Additive spin, Override appearance | ⬜ | Per ARCH §10.5 |
| F88-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F89 | `FieldInstance`: add lifecycleState, fadeProgress | ⬜ | State tracking |
| F89-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F90 | `PersonalFieldInstance`: update to use FollowModeConfig | ⬜ | Config-based |
| F90-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-09 | ⚠️ **BATCH 9 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 10: Shape Records Part 1 (F91-F100)

> **Ref:** CLASS_DIAGRAM §4, PARAMETERS §4  
> **Package:** `net.cyberpunk042.visual.shape`

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F91 | `Shape` interface: getType(), getBounds(), primaryCellType(), getParts() | ⬜ | Base interface |
| F91-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F92 | `SphereShape` record: radius, latSteps, lonSteps, latStart, latEnd, lonStart, lonEnd, algorithm, subdivisions | ⬜ | Primary: QUAD, Parts: main, poles, equator, hemisphereTop, hemisphereBottom |
| F92-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F93 | `SphereShape.DEFAULT` and `SphereShape.Builder` | ⬜ | Convenience |
| F93-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F94 | `RingShape` record: innerRadius, outerRadius, segments, y, arcStart, arcEnd, height, twist | ⬜ | Primary: SEGMENT |
| F94-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F95 | `DiscShape` record: radius, segments, y, arcStart, arcEnd, innerRadius, rings | ⬜ | Primary: SECTOR |
| F95-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F96 | `PrismShape` record: sides, radius, height, topRadius, twist, heightSegments, capTop, capBottom | ⬜ | Primary: QUAD |
| F96-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F97 | `PolyhedronShape` record: polyType, radius, subdivisions | ⬜ | Primary: QUAD or TRIANGLE |
| F97-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F98 | `CylinderShape` record: radius, height, segments, topRadius, heightSegments, capTop, capBottom, openEnded, arc | ⬜ | Primary: QUAD |
| F98-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F99 | All Shape.getParts() return correct Map<String, CellType> | ⬜ | Verification |
| F99-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F100 | All Shape.primaryCellType() return correct CellType | ⬜ | Verification |
| F100-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-10 | ⚠️ **BATCH 10 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 11: Primitive Interface & Core Implementations (F101-F110)

> **Ref:** CLASS_DIAGRAM §3, ARCHITECTURE §5  
> **Package:** `net.cyberpunk042.field.primitive`

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F101 | `Primitive` interface: id(), type(), shape(), transform(), fill(), visibility(), arrangement(), appearance(), animation(), link() | ⬜ | Flat hierarchy, id REQUIRED, link() nullable |
| F101-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F102 | `AbstractPrimitive` base implementation (if useful) OR each impl standalone | ⬜ | Design decision |
| F102-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F103 | `SpherePrimitive` implementing Primitive | ⬜ | Uses SphereShape |
| F103-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F104 | `RingPrimitive` implementing Primitive | ⬜ | Uses RingShape |
| F104-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F105 | `DiscPrimitive` implementing Primitive | ⬜ | Uses DiscShape |
| F105-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F106 | `PrismPrimitive` implementing Primitive | ⬜ | Uses PrismShape |
| F106-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F107 | `PolyhedronPrimitive` implementing Primitive | ⬜ | Uses PolyhedronShape |
| F107-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F108 | `CylinderPrimitive` implementing Primitive | ⬜ | Uses CylinderShape |
| F108-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F109 | All primitives have proper equals(), hashCode(), toString() | ⬜ | Record or implement |
| F109-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F110 | All primitives log construction via FIELD channel DEBUG | ⬜ | Code Quality Standard |
| F110-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-11 | ⚠️ **BATCH 11 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 12: Primitive Linking (F111-F115)

> **Ref:** ARCHITECTURE §9, CLASS_DIAGRAM §11  
> **Package:** `net.cyberpunk042.field.primitive`

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F111 | `PrimitiveLink` record: radiusMatch, radiusOffset, follow, mirror, phaseOffset, scaleWith | ⬜ | All nullable |
| F111-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F112 | Link resolution: primitives can only link to EARLIER primitives (cycle prevention) | ⬜ | Per ARCH §9 |
| F112-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F113 | `LinkResolver`: resolves links at parse time, computes final values | ⬜ | Runtime resolver |
| F113-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F114 | Test: ring links to sphere radius + offset | ⬜ | Basic link test |
| F114-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F115 | Test: invalid link (forward reference) logs error | ⬜ | Error case |
| F115-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-12 | ⚠️ **BATCH 12 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 13: Remove/Archive Old Classes (F116-F122)

> **Ref:** ARCHITECTURE §5  
> **Action:** Archive to `_legacy/` or convert

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F116 | Archive `SolidPrimitive` abstract class → _legacy/ | ⬜ | Unnecessary hierarchy |
| F116-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F117 | Archive `BandPrimitive` abstract class → _legacy/ | ⬜ | Unnecessary hierarchy |
| F117-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F118 | Archive `StructuralPrimitive` abstract class → _legacy/ | ⬜ | Unnecessary hierarchy |
| F118-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F119 | Convert `StripesPrimitive` → SpherePrimitive + visibility.mask=STRIPES | ⬜ | Config, not class |
| F119-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F120 | Convert `CagePrimitive` → SpherePrimitive + fill.mode=CAGE | ⬜ | Config, not class |
| F120-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F121 | Convert `RingsPrimitive` → multiple RingPrimitive in layer | ⬜ | Layer composition |
| F121-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F122 | Update all imports/references after archival | ⬜ | Cleanup |
| F122-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-13 | ⚠️ **BATCH 13 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 14: JSON Loading System (F123-F132)

> **Ref:** CLASS_DIAGRAM §15, ARCHITECTURE §12  
> **Package:** `net.cyberpunk042.field.loader`

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F123 | Create reference folders: field_shapes/, field_appearances/, etc. (9 folders) | ⬜ | Per CLASS_DIAGRAM §12 |
| F123-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F124 | `ReferenceResolver` class: resolve(), resolveWithOverrides(), cache | ⬜ | $ref resolution |
| F124-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F125 | Reference syntax: "$shapes/smooth_sphere" → field_shapes/smooth_sphere.json | ⬜ | Path mapping |
| F125-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F126 | Override syntax: { "$ref": "...", "radius": 2.0 } merges | ⬜ | Merge logic |
| F126-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F127 | `DefaultsProvider` class: getDefaultShape(type), getDefaultTransform(), etc. | ⬜ | Smart defaults |
| F127-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F128 | Defaults per shape type: sphere, ring, disc, prism, polyhedron, cylinder | ⬜ | Per CLASS_DIAGRAM §13 |
| F128-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F129 | Update `FieldLoader` to use ReferenceResolver | ⬜ | Integration |
| F129-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F130 | Update `FieldLoader` to use DefaultsProvider | ⬜ | Integration |
| F130-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F131 | Shorthand parsing: alpha: 0.5 → AlphaRange, spin: 0.02 → SpinConfig | ⬜ | User convenience |
| F131-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F132 | Test: Load field with $ref and override | ⬜ | End-to-end test |
| F132-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-14 | ⚠️ **BATCH 14 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 15: JSON Parsing - All Fields (F133-F142)

> **Ref:** ARCHITECTURE §6, PARAMETERS  
> **Package:** `net.cyberpunk042.field.layer`, `net.cyberpunk042.field.loader`

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F133 | Parse Transform: anchor, offset, rotation, scale, scaleXYZ, facing, billboard, orbit | ⬜ | All fields |
| F133-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F134 | Parse FillConfig: mode, wireThickness, cage (shape-specific) | ⬜ | Nested cage |
| F134-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F135 | Parse VisibilityMask: mask, count, thickness | ⬜ | Phase 1 only |
| F135-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F136 | Parse ArrangementConfig: string OR object with 15 parts | ⬜ | Multi-part |
| F136-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F137 | Parse Animation: spin, pulse, alphaPulse, phase | ⬜ | Phase 1 anims |
| F137-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F138 | Parse Appearance: color, alpha, glow, emissive, saturation, brightness, hueShift, secondaryColor | ⬜ | All fields |
| F138-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F139 | Parse PrimitiveLink: radiusMatch, radiusOffset, follow, mirror, phaseOffset, scaleWith | ⬜ | Linking |
| F139-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F140 | Parse all Shape types with all parameters | ⬜ | Per PARAMETERS §4 |
| F140-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F141 | Validation: Required fields present, ranges valid | ⬜ | Error handling |
| F141-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F142 | Test: Load complex field with all config types | ⬜ | Integration test |
| F142-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-15 | ⚠️ **BATCH 15 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 16: External Influences - Bindings (F143-F150)

> **Ref:** ARCHITECTURE §12.1, CLASS_DIAGRAM §16  
> **Package:** `net.cyberpunk042.field.influence`

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F143 | `BindingSource` interface: getId(), getValue(player), isBoolean() | ⬜ | Abstract source |
| F143-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F144 | `BindingSources` class: 12 static sources (health, armor, speed, etc.) | ⬜ | Per ARCH §12.1 |
| F144-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F145 | `BindingSources.get(id)` returns Optional<BindingSource> | ⬜ | Lookup |
| F145-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F146 | Invalid source handling: log warning, default to 0.0 | ⬜ | Per Q3 answer |
| F146-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F147 | `BindingConfig` record: source, inputRange, outputRange, curve | ⬜ | Per binding |
| F147-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F148 | `BindingResolver`: evaluates binding, applies curve, maps ranges | ⬜ | Runtime |
| F148-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F149 | `InterpolationCurve.apply(float t)` implementation | ⬜ | Curve math |
| F149-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F150 | Parse `"bindings"` block in FieldDefinition JSON | ⬜ | Integration |
| F150-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-16 | ⚠️ **BATCH 16 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 17: External Influences - Triggers (F151-F158)

> **Ref:** ARCHITECTURE §12.2, CLASS_DIAGRAM §16  
> **Package:** `net.cyberpunk042.field.influence`

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F151 | `TriggerConfig` record: event, effect, duration, color, scale, amplitude, intensity | ⬜ | Per trigger |
| F151-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F152 | `ActiveTrigger` class: tracks active trigger state, tick countdown | ⬜ | Runtime state |
| F152-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F153 | `TriggerEffect.completesNaturally()`: PULSE,SHAKE→true, FLASH,GLOW,COLOR_SHIFT→false | ⬜ | Per Q4 answer |
| F153-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F154 | Trigger/Binding conflict: Trigger temporarily overrides binding | ⬜ | Per Q1 answer |
| F154-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F155 | `TriggerProcessor`: listens for events, creates ActiveTriggers | ⬜ | Event handling |
| F155-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F156 | Event listeners: damage, heal, death, respawn, field spawn/despawn | ⬜ | Mixin or listener |
| F156-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F157 | Parse `"triggers"` array in FieldDefinition JSON | ⬜ | Integration |
| F157-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F158 | Test: Trigger fires on damage, effect visible | ⬜ | Visual test |
| F158-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-17 | ⚠️ **BATCH 17 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 18: External Influences - Lifecycle & Combat (F159-F168)

> **Ref:** ARCHITECTURE §12.3-12.4, CLASS_DIAGRAM §16  
> **Package:** `net.cyberpunk042.field.influence`

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F159 | `DecayConfig` record: rate, min | ⬜ | Decay settings |
| F159-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F160 | `LifecycleConfig` record: fadeIn, fadeOut, scaleIn, scaleOut, decay | ⬜ | Lifecycle settings |
| F160-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F161 | `LifecycleManager`: handles fade/scale animations on spawn/despawn | ⬜ | Runtime manager |
| F161-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F162 | FieldInstance.lifecycleState transitions: SPAWNING→ACTIVE→DESPAWNING→COMPLETE | ⬜ | State machine |
| F162-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F163 | FieldInstance.fadeProgress: 0.0→1.0 during transitions | ⬜ | Animation progress |
| F163-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F164 | `CombatTracker`: per-player singleton (per Q2 answer) | ⬜ | Combat state |
| F164-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F165 | CombatTracker: isInCombat() (within 100 ticks), getDamageTakenDecayed() | ⬜ | Combat sources |
| F165-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F166 | Hook damage events: onDamageTaken(amount), onDamageDealt() | ⬜ | Mixin or listener |
| F166-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F167 | Parse `"lifecycle"` block in FieldDefinition JSON | ⬜ | Integration |
| F167-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F168 | Test: Field fades in on spawn, fades out on despawn | ⬜ | Visual test |
| F168-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-18 | ⚠️ **BATCH 18 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 19: Rendering Updates Part 1 (F169-F178)

> **Ref:** CLASS_DIAGRAM §8  
> **Package:** `net.cyberpunk042.client.visual.render`, `net.cyberpunk042.client.field.render`

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F169 | Update `LayerRenderer`: apply all Transform options (anchor, facing, billboard) | ⬜ | Complete transform |
| F169-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F170 | Update `LayerRenderer`: apply layer spin + primitive spin (additive) | ⬜ | Per ARCH §10.5 |
| F170-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F171 | Update `LayerRenderer`: apply visibility toggle | ⬜ | layer.visible |
| F171-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F172 | Update `SphereRenderer`: use FillConfig, VisibilityMask, ArrangementConfig | ⬜ | Full integration |
| F172-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F173 | Update `SphereRenderer`: shouldRender() from VertexPattern | ⬜ | Pattern filtering |
| F173-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F174 | Update `SphereRenderer`: getVertexOrder() from VertexPattern | ⬜ | Vertex reordering |
| F174-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F175 | Update `RingRenderer`: all config integration | ⬜ | Full integration |
| F175-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F176 | Update `DiscRenderer`: all config integration | ⬜ | Full integration |
| F176-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F177 | Update `PrismRenderer`: all config integration + multi-part | ⬜ | Sides vs caps |
| F177-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F178 | Update `PolyhedronRenderer`: all config integration | ⬜ | Full integration |
| F178-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-19 | ⚠️ **BATCH 19 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 20: Rendering Updates Part 2 (F179-F188)

> **Ref:** CLASS_DIAGRAM §8  
> **Package:** `net.cyberpunk042.client.*`

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F179 | Update `CylinderRenderer`: all config integration + multi-part | ⬜ | Sides vs caps |
| F179-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F180 | Update `CageRenderer`: use shape-specific CageOptions | ⬜ | Cage rendering |
| F180-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F181 | Update `FieldRenderer`: apply lifecycle fadeProgress to alpha | ⬜ | Lifecycle visual |
| F181-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F182 | Update `FieldRenderer`: apply binding results before rendering | ⬜ | Binding visual |
| F182-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F183 | Update `FieldRenderer`: apply active trigger effects | ⬜ | Trigger visual |
| F183-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F184 | Update `FieldRenderer`: resolve primitive links before rendering | ⬜ | Link resolution |
| F184-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F185 | Verify all renderers use FIELD logging channel | ⬜ | Code Quality |
| F185-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F186 | Verify all renderers have proper class Javadoc | ⬜ | Code Quality |
| F186-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F187 | Verify all renderers have section markers for long methods | ⬜ | Code Quality |
| F187-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F188 | Performance: Add trace logging for expensive operations | ⬜ | Per 00_TODO |
| F188-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-20 | ⚠️ **BATCH 20 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 21: Integration Testing (F189-F198)

> **Ref:** In-game testing  
> **Type:** Verification

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F189 | Test sphere with all fill modes: solid, wireframe, cage | ⬜ | Visual check |
| F189-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F190 | Test all 9 anchor positions | ⬜ | Position check |
| F190-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F191 | Test multi-part arrangements: different patterns on caps vs sides | ⬜ | Prism/cylinder |
| F191-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F192 | Test visibility masks: full, bands, stripes, checker | ⬜ | Mask check |
| F192-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F193 | Test all 6 primitives render correctly | ⬜ | All shapes |
| F193-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F194 | Test primitive linking: ring matches sphere radius | ⬜ | Link check |
| F194-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F195 | Test binding: alpha follows health | ⬜ | Binding check |
| F195-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F196 | Test trigger: flash on damage | ⬜ | Trigger check |
| F196-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F197 | Test lifecycle: fade in/out | ⬜ | Lifecycle check |
| F197-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F198 | Test JSON reference: load field with $ref | ⬜ | Reference check |
| F198-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-21 | ⚠️ **BATCH 21 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Batch 22: Final Verification (F199-F205)

> **Ref:** All documents  
> **Type:** Final audit

| ID | Task | Status | Notes |
|----|------|--------|-------|
| F199 | Audit: All enums from CLASS_DIAGRAM §18 created | ⬜ | 18 enums |
| F199-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F200 | Audit: All records from CLASS_DIAGRAM §19 created | ⬜ | 18 records |
| F200-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F201 | Audit: Logging uses FIELD channel throughout | ⬜ | Code Quality |
| F201-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F202 | Audit: alwaysChat() used for critical errors | ⬜ | Code Quality |
| F202-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F203 | Audit: All classes have proper Javadoc | ⬜ | Code Quality |
| F203-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F204 | Audit: All public methods have comments | ⬜ | Code Quality |
| F204-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| F205 | Full integration test: load profile → spawn → edit → live reload → verify | ⬜ | End-to-end |
| F205-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| CHK-22 | ⚠️ **BATCH 22 COMPLETE** - [Directives Checklist](./00_TODO_DIRECTIVES.md#after-completing-a-todo) | ⬜ | - |

---

### Phase 1 Final Checkpoint

| ID | Task | Status | Notes |
|----|------|--------|-------|
| P1-FINAL | ⚠️ **PHASE 1 COMPLETE** - Full review of all tasks against ARCHITECTURE v5.1, CLASS_DIAGRAM v7.1, PARAMETERS v5.1 | ⬜ | - |

---

## Phase 2: GUI & Polish (Draft Only)

> **⛔ HARD STOP AFTER P2-DRAFT-03**  
> **Only create design docs, NO implementation**

| ID | Task | Status | Notes |
|----|------|--------|-------|
| P2-DRAFT-01 | GUI panel wireframe design (paper/figma sketch) | ⬜ | Design only |
| P2-DRAFT-01-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| P2-DRAFT-02 | GUI component inventory (what controls needed) | ⬜ | Design only |
| P2-DRAFT-02-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| P2-DRAFT-03 | GUI data flow diagram (how it connects to field system) | ⬜ | Design only |
| P2-DRAFT-03-CHK | ↳ [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md) check | ⬜ | - |
| P2-STOP | ⛔ **HARD STOP** - Re-evaluate Phase 1, review results, plan Phase 2 scope | ⬜ | STOP HERE |

---

## Status Legend

| Symbol | Meaning |
|--------|---------|
| ⬜ | Pending |
| 🔄 | In Progress |
| ✅ | Completed |
| 🚧 | Blocked |

---

## Document References

| Abbrev | Document | When to Use |
|--------|----------|-------------|
| ARCH | [01_ARCHITECTURE.md](./01_ARCHITECTURE.md) v5.1 | Why & How |
| CD | [02_CLASS_DIAGRAM.md](./02_CLASS_DIAGRAM.md) v7.1 | What to create |
| PARAM | [03_PARAMETERS.md](./03_PARAMETERS.md) v5.1 | All field details |
| SHAPE | [04_SHAPE_MATRIX.md](./04_SHAPE_MATRIX.md) | Per-shape params |
| OBS | [OBSERVATIONS.md](./OBSERVATIONS.md) | Issues found |
| Q | [QUESTIONS.md](./QUESTIONS.md) | Open questions |
| REVIEW | [SENIOR_REVIEW_FINAL.md](./_reviews/SENIOR_REVIEW_FINAL.md) | Approved decisions |

---

## Related Documents

- [00_TODO_DIRECTIVES.md](./00_TODO_DIRECTIVES.md) - **Follow for EVERY task**
- [OBSERVATIONS.md](./OBSERVATIONS.md) - Discoveries and issues
- [QUESTIONS.md](./QUESTIONS.md) - Open questions

---

## Summary

| Batch | Focus | Tasks | +CHK | Total |
|-------|-------|-------|------|-------|
| 1-2 | Foundation Enums | 20 | 20 | 40 |
| 3 | Pattern System | 10 | 10 | 20 |
| 4 | Transform | 10 | 10 | 20 |
| 5 | Fill & Visibility | 10 | 10 | 20 |
| 6 | Arrangement | 10 | 10 | 20 |
| 7 | Animation | 10 | 10 | 20 |
| 8 | Appearance | 10 | 10 | 20 |
| 9 | Field Config | 10 | 10 | 20 |
| 10 | Shape Records | 10 | 10 | 20 |
| 11 | Primitive Interface | 10 | 10 | 20 |
| 12 | Primitive Linking | 5 | 5 | 10 |
| 13 | Archive Old | 7 | 7 | 14 |
| 14 | JSON Loading | 10 | 10 | 20 |
| 15 | JSON Parsing | 10 | 10 | 20 |
| 16 | Bindings | 8 | 8 | 16 |
| 17 | Triggers | 8 | 8 | 16 |
| 18 | Lifecycle/Combat | 10 | 10 | 20 |
| 19-20 | Rendering | 20 | 20 | 40 |
| 21 | Integration Tests | 10 | 10 | 20 |
| 22 | Final Audit | 7 | 7 | 14 |
| + Batch CHK | (end of batch) | 22 | - | 22 |
| **TOTAL** | | **~207** | **~207** | **~430** |

**Every task has a matching `-CHK` step to follow [TODO_DIRECTIVES](./00_TODO_DIRECTIVES.md)**

---

*Last updated: December 8, 2024*  
*Phase 1: 209 tasks + 208 CHK = 417 rows*  
*Phase 2 Draft: 4 tasks*  
*Total: 421 rows*
