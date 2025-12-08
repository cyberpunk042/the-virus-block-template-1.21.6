# Profile Migration Gap Analysis

> **Date:** December 6, 2024  
> **Purpose:** Identify missing features before creating alpha profiles

---

## Old System Features vs New System

### ShieldProfileConfig → FieldDefinition

| Old Field | Old Type | New Equivalent | Status |
|-----------|----------|----------------|--------|
| `radius` | float | `baseRadius` | ✅ Have |
| `visualScale` | float | - | ❌ **MISSING** |
| `spinSpeed` | float | `Animation.spin` | ✅ Have |
| `tiltMultiplier` | float | - | ❌ **MISSING** |
| `primaryColor` | int | `ColorTheme.primary` | ✅ Have |
| `secondaryColor` | int | `ColorTheme.secondary` | ✅ Have |
| `minAlpha` | float | - | ❌ **MISSING** (only single alpha) |
| `maxAlpha` | float | - | ❌ **MISSING** |
| `beamEnabled` | bool | `BeamConfig` | ✅ Have |
| `beamInnerRadius` | float | `BeamConfig` | ⚠️ Verify |
| `beamOuterRadius` | float | `BeamConfig` | ⚠️ Verify |
| `beamColor` | int | `ColorTheme.beam` | ✅ Have |
| `predictionEnabled` | bool | `PredictionConfig` | ✅ Have |
| `predictionLeadTicks` | int | `PredictionConfig` | ✅ Have |
| `predictionMaxDistance` | float | `PredictionConfig` | ✅ Have |
| `predictionLookAhead` | float | `PredictionConfig` | ✅ Have |
| `predictionVerticalBoost` | float | `PredictionConfig` | ✅ Have |

### ShieldMeshLayerConfig → FieldLayer/Primitive

| Old Field | Old Type | New Equivalent | Status |
|-----------|----------|----------------|--------|
| `meshType` | enum | `FillMode` | ⚠️ **PARTIAL** |
| `latSteps` | int | `SphereShape.latSteps` | ✅ Have |
| `lonSteps` | int | `SphereShape.lonSteps` | ✅ Have |
| `latStart` | float | - | ❌ **MISSING** |
| `latEnd` | float | - | ❌ **MISSING** |
| `lonStart` | float | - | ❌ **MISSING** |
| `lonEnd` | float | - | ❌ **MISSING** |
| `radiusMultiplier` | float | `Transform.scale` | ✅ Have |
| `swirlStrength` | float | - | ❌ **MISSING** |
| `phaseOffset` | float | `Animation.phase` | ✅ Have |
| `alphaMin` | float | - | ❌ **MISSING** |
| `alphaMax` | float | - | ❌ **MISSING** |
| `bandCount` | int | `RingsPrimitive` | ⚠️ Different |
| `bandThickness` | float | `RingShape.thickness` | ✅ Have |
| `wireThickness` | float | `StructuralPrimitive.wireThickness` | ✅ Have |

---

## Missing Features - CRITICAL

### 1. Visual Scale (`visualScale`)
**Purpose:** Global multiplier applied to the entire field render
**Where to add:** `FieldDefinition` or `Modifiers`
**Impact:** High - affects overall appearance

### 2. Tilt Multiplier (`tiltMultiplier`)
**Purpose:** Tilts field based on player movement direction
**Where to add:** `Modifiers` or new `MovementConfig`
**Impact:** Medium - nice effect for personal fields

### 3. Alpha Range (`minAlpha`/`maxAlpha`)
**Purpose:** Pulsing alpha between min and max
**Where to add:** `Appearance` or new `AlphaRange` record
**Current:** Single `alpha` float
**Impact:** High - critical for pulsing effects

### 4. Partial Sphere Rendering (`latStart/End`, `lonStart/End`)
**Purpose:** Render only portions of sphere (domes, segments)
**Where to add:** `SphereShape` or new `SphereSegment` shape
**Impact:** High - enables hemispheres, equator bands, etc.

### 5. Swirl Effect (`swirlStrength`)
**Purpose:** Rotational distortion on sphere surface
**Where to add:** New animation type or `SphereShape` parameter
**Impact:** Medium - visual polish

### 6. Additional Fill Modes
**Current FillMode:** `SOLID`, `WIREFRAME`, `POINTS`
**Missing:**
- `BANDS` - horizontal stripes
- `CHECKER` - checkerboard pattern
- `HEMISPHERE` - half sphere

---

## Features We Have But Different

### Band Rendering
- **Old:** `bandCount` + `bandThickness` on any layer
- **New:** `RingsPrimitive` with explicit ring list
- **Gap:** No automatic band generation

### Mesh Types
- **Old:** Enum-based switch in render logic
- **New:** Explicit primitive types
- **Gap:** Less flexible but cleaner

---

## Recommendations

### P0 - Must Have Before Alpha Profiles

1. **Add `visualScale` to Modifiers**
```java
public record Modifiers(
    float visualScale,     // NEW
    boolean playerRelative,
    boolean facesPlayer,
    boolean matchesPlayerRotation
) {}
```

2. **Add `AlphaRange` to Appearance**
```java
// Option A: New record
public record AlphaRange(float min, float max) {}

// Option B: Extend Appearance
public record Appearance(
    String color,
    float alphaMin,    // renamed from alpha
    float alphaMax,    // NEW
    FillMode fill,
    boolean glow
) {}
```

3. **Add `tiltMultiplier` to PredictionConfig or Modifiers**

4. **Add partial sphere support**
```java
public record SphereShape(
    float radius,
    int latSteps,
    int lonSteps,
    float latStart,  // NEW: 0.0 = north pole
    float latEnd,    // NEW: 1.0 = south pole
    float lonStart,  // NEW: 0.0 = start angle
    float lonEnd     // NEW: 1.0 = full circle
) {}
```

### P1 - Nice to Have

5. **Add `swirlStrength` to Animation or new AnimationEffect**

6. **Add BANDS, CHECKER fill modes**

---

## Alpha Profile Types to Create

Based on old profiles, we should create:

| Profile ID | Type | Description |
|------------|------|-------------|
| `alpha_shield_default` | SHIELD | Basic protective bubble |
| `alpha_shield_cyber` | SHIELD | High-tech cyan appearance |
| `alpha_shield_crimson` | SHIELD | Red aggressive shield |
| `alpha_personal_aura` | PERSONAL | Subtle player aura |
| `alpha_personal_bubble` | PERSONAL | Full bubble around player |
| `alpha_growth_pulse` | GROWTH | Pulsing growth block field |
| `alpha_force_push` | FORCE | Repelling force field |
| `alpha_force_pull` | FORCE | Attracting force field |

---

## Action Items

> See `ARCHITECTURE_UPDATE.md` for detailed implementation specs

### P0 - Core Features ✅ COMPLETE
- [x] Create `AlphaRange.java` record
- [x] Create `PatternConfig.java` record  
- [x] Update `Appearance.java` to use AlphaRange and PatternConfig
- [x] Update `SphereShape.java` with partial sphere parameters
- [x] Update `Modifiers.java` with visualScale, tiltMultiplier, swirlStrength
- [x] Create `Axis.java` enum
- [x] Update `Animation.java` with alphaPulse, spinAxis

### P1 - Tessellation Support ✅ COMPLETE
- [x] Update `SphereTessellator` to handle partial spheres
- [x] Add pattern support to tessellation (bands, checker)

### P2 - Alpha Profiles ✅ COMPLETE
- [x] Create `alpha_shield_default.json`
- [x] Create `alpha_shield_cyber.json`
- [x] Create `alpha_shield_crimson.json`
- [x] Create `alpha_personal_aura.json`
- [x] Create `alpha_personal_bubble.json`
- [x] Create `alpha_personal_rings.json`
- [x] Create `alpha_force_push.json`
- [x] Create `alpha_force_pull.json`
- [x] Create `alpha_growth_pulse.json`
- [x] Create `alpha_growth_active.json`

### P3 - Migration
- [ ] Wire up old profile loading to new system
- [ ] Deprecate old config classes

