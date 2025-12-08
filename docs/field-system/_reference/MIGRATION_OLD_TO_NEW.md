# Hard Switch Analysis: Old Shield System → New Field System

## Executive Summary

Transitioning from **~4000 lines** of legacy shield code to the new field system.
This document identifies gaps, improvements, and migration tasks.

**STATUS:** ✅ NEW SYSTEM IS NEARLY COMPLETE - Only minor gaps remain!

---

## 1. FEATURE COMPARISON

### 1.1 Mesh Types / Fill Modes ✅ COVERED

**Old:** `ShieldMeshLayerConfig.MeshType` enum
```java
SOLID, BANDS, WIREFRAME, CHECKER, HEMISPHERE
```

**New:**
- `FillMode`: SOLID, WIREFRAME, TRANSPARENT ✅
- `PatternConfig`: NONE, BANDS, CHECKER ✅ with `shouldRender()` logic
- HEMISPHERE: Use `SphereShape.latStart(0.5)` for hemisphere ✅

---

### 1.2 Cell Rendering Control ✅ COVERED

**Old:** `shouldRenderCell()` logic

**New:** `PatternConfig.shouldRender(latFraction, lonFraction)` ✅
```java
case BANDS -> {
    float scaled = latFraction * count;
    float frac = scaled - MathHelper.floor(scaled);
    yield frac <= thickness;
}
case CHECKER -> {
    int latCell = MathHelper.floor(latFraction * band);
    int lonCell = MathHelper.floor(lonFraction * band);
    yield ((latCell + lonCell) & 1) == 0;
}
```

**Wire thickness:** In `Appearance.wireThickness` ✅

---

### 1.3 Triangle Type System (50+ Patterns)

**Old:** `ShieldTriangleTypeStore` with 50+ vertex patterns

**New:** No equivalent

**Decision:** ⏸️ DEFER - This is a nice-to-have for advanced customization.
Can add later as custom tessellator variants if needed.

---

### 1.4 Personal Shield Follow Modes ✅ ALREADY EXISTS

**New:** `PersonalFieldTracker.FollowMode` enum ✅
```java
SNAP(1.0f),      // Instant follow
SMOOTH(0.35f),   // Smooth interpolation  
GLIDE(0.2f);     // Very smooth, floaty
```

---

### 1.5 Prediction Controls ✅ COMPLETE

**New:** `PredictionConfig` has ALL fields ✅
```java
boolean enabled,
int leadTicks,
float maxDistance,
float lookAhead,      // ✅ EXISTS
float verticalBoost   // ✅ EXISTS
```

Used in `PersonalFieldInstance.applyPrediction()` ✅

---

### 1.6 Profile Save to Config Directory

**Old:** Users save to `config/the-virus-block/forcefields/`

**New:** Read-only from `data/`

**Gap:**
- [ ] Add user profile save/load to config directory (OPTIONAL)
- **Workaround:** Use `/fieldtest edit` for live tweaking

---

### 1.7 Swirl Strength ✅ EXISTS

**New:** `Modifiers.swirlStrength()` ✅

**Gap:**
- [ ] Verify swirl is wired in `SphereTessellator`

---

### 1.8 Beam Rendering ✅ EXISTS

**New:** `BeamPrimitive` + `BeamRenderer` exist

**Gap:**
- [ ] Verify `BeamRenderer` is actually called in render pipeline

---

## 2. IMPROVEMENTS IN NEW SYSTEM ✓

| Feature | Benefit |
|---------|---------|
| Type-safe Primitive hierarchy | No string-based type dispatch |
| Sealed interfaces | Compile-time exhaustiveness |
| ColorTheme with roles | @primary, @secondary, @glow |
| ColorResolver | Hex, slot, reference support |
| Multiple sphere algorithms | LAT_LON, TYPE_A, TYPE_E |
| CommandKnob integration | Uniform command system |
| Client/server separation | Clean architecture |
| Hot-reload via registry | No restart needed |
| FieldTestCommand | Live editing in-game |
| Logging system | Beautiful tables, topics |
| ListFormatter/ReportBuilder | Consistent command output |

---

## 3. FILES TO DELETE

### Old Shield System (~4000 lines)

| File | Lines | Replacement |
|------|-------|-------------|
| `ShieldProfileConfig.java` | 1080 | `FieldDefinition` ✅ |
| `ShieldFieldVisualManager.java` | 1076 | `ClientFieldManager` ✅ |
| `ShieldMeshLayerConfig.java` | 510 | `FieldLayer` + `Primitive` ✅ |
| `ShieldProfileStore.java` | ~200 | `FieldRegistry` ✅ |
| `PersonalShieldProfileStore.java` | ~160 | `PersonalFieldTracker` ✅ |
| `ShieldMeshStyleStore.java` | ~150 | Not needed ✅ |
| `ShieldMeshShapeStore.java` | ~150 | `ShapeRegistry` ✅ |
| `ShieldMeshShapeConfig.java` | ~100 | `SphereShape` ✅ |
| `ShieldTriangleTypeStore.java` | ~170 | Deferred |
| `ShieldTriangleTypeConfig.java` | ~150 | Deferred |
| `PersonalFollowMode.java` | ~50 | In `PersonalFieldTracker` ✅ |

**Related Command Files:**
| File | Replacement |
|------|-------------|
| `ShieldVisualCommand.java` | `/fieldtest` |
| `MeshStyleCommand.java` | `/fieldtest edit` |
| `MeshShapeCommand.java` | `/fieldtest edit` |
| `TriangleTypeCommand.java` | Deferred |
| `ShieldPersonalCommand.java` | `/fieldtest` |

**Related Asset Files:**
| Directory | Status |
|-----------|--------|
| `shield_profiles/*.json` | Migrate to `field_definitions/` |
| `shield_triangles/*.json` | Defer (50+ files) |

---

## 4. MIGRATION TASKS

### Phase 1: Verify Wiring ✅
- [x] PatternConfig.shouldRender() exists
- [x] Appearance.wireThickness exists
- [x] PredictionConfig has all fields
- [x] PersonalFieldTracker.FollowMode exists
- [x] Modifiers.swirlStrength exists
- [ ] Verify swirl is used in tessellator
- [ ] Verify BeamRenderer is called

### Phase 2: Convert Builtin Profiles
- [ ] `shield_profiles/anti_virus_field.json` → already as `alpha_anti_virus.json`
- [ ] `shield_profiles/singularity_field.json` → already as `alpha_singularity.json`
- [ ] `shield_profiles/personal_default.json` → already as `alpha_personal_default.json`
- [ ] `shield_profiles/personal/rings.json` → already as `alpha_rings.json`
- [ ] Other personal profiles (striped, meshed, fraction-*)
- [ ] Verify all old profiles have new equivalents

### Phase 3: Find & Update Callers
- [ ] `grep ShieldFieldVisualManager` → migrate to ClientFieldManager
- [ ] `grep ShieldProfileConfig` → migrate to FieldDefinition
- [ ] `grep PersonalShieldProfileStore` → migrate to FieldRegistry
- [ ] Network payloads: already have FieldSpawnPayload etc.

### Phase 4: Delete Old Code
- [ ] Create backup branch
- [ ] Delete Shield*.java files
- [ ] Delete old command files
- [ ] Delete shield_profiles/ assets
- [ ] Clean up TheVirusBlockClient.java init calls

### Phase 5: Cleanup
- [ ] Remove unused imports
- [ ] Compile test
- [ ] In-game visual test

---

## 5. REMAINING IMPROVEMENTS (Optional)

### 5.1 User Profile Save/Load
Allow users to save custom profiles to config directory.
**Implementation:** Add `FieldProfileStore.save()` method.

### 5.2 Triangle Types
50+ vertex arrangement patterns.
**Decision:** Defer. Not critical for core functionality.

### 5.3 More Fill Modes
Could add DOTS, GRID, SPIRAL patterns to PatternConfig.

---

## 6. VERIFICATION CHECKLIST

After migration, verify:

- [ ] Sphere renders correctly (solid, wireframe)
- [ ] Bands pattern works (`PatternConfig.bands()`)
- [ ] Checker pattern works (`PatternConfig.checker()`)
- [ ] Alpha pulsing works (`AlphaRange` + `Animation.alphaPulse`)
- [ ] Spin animation works (`Animation.spin`)
- [ ] Swirl distortion works (`Modifiers.swirlStrength`)
- [ ] Personal field follows player (`PersonalFieldTracker`)
- [ ] Personal field uses follow modes (SNAP/SMOOTH/GLIDE)
- [ ] Prediction offset works (`PredictionConfig`)
- [ ] Beam renders (if `BeamPrimitive` in layers)
- [ ] Multiple layers composite correctly
- [ ] Color themes apply correctly (`ColorTheme` + `@primary`)
- [ ] All alpha profiles render
- [ ] `/fieldtest spawn` works
- [ ] `/fieldtest edit` modifies live

---

## 7. SUMMARY

### What We Have ✅
| Feature | Status |
|---------|--------|
| Sphere rendering | ✅ 3 algorithms |
| Ring/Rings/Stripes | ✅ Primitives |
| Cage/Beam/Prism | ✅ Primitives |
| Color themes | ✅ @primary, @secondary, etc. |
| Patterns (bands/checker) | ✅ PatternConfig |
| Alpha pulsing | ✅ AlphaRange |
| Spin/animation | ✅ Animation |
| Swirl distortion | ✅ Modifiers |
| Personal field | ✅ PersonalFieldTracker |
| Follow modes | ✅ SNAP/SMOOTH/GLIDE |
| Prediction | ✅ Full config |
| Live editing | ✅ /fieldtest |
| Hot reload | ✅ FieldRegistry |

### What to Delete 🗑️
~4000 lines of legacy code

### Remaining Work 🔧
1. Verify swirl is wired in tessellator
2. Verify BeamRenderer is called
3. Find/update callers of old system
4. Delete old files
5. In-game testing

