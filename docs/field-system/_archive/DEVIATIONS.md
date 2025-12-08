# Precise Deviations from ARCHITECTURE.md

> **Date:** December 6, 2024

---

## 1. Package Structure Deviations

| ARCHITECTURE.md | Actual Location | Impact |
|-----------------|-----------------|--------|
| `visual/mesh/Mesh.java` | `client/visual/mesh/Mesh.java` | ⚠️ Client-only (acceptable) |
| `visual/mesh/MeshBuilder.java` | `client/visual/mesh/MeshBuilder.java` | ⚠️ Client-only (acceptable) |
| `visual/mesh/Tessellator.java` | `client/visual/tessellate/Tessellator.java` | ⚠️ Different subpackage |
| `visual/animation/Animation.java` | `field/primitive/Animation.java` | ❌ **Wrong location** |
| `visual/appearance/FillMode.java` | `visual/render/FillMode.java` | ⚠️ Different subpackage |
| `field/definition/FieldDefinition.java` | `field/FieldDefinition.java` | ⚠️ In parent package |
| `field/definition/FieldType.java` | `field/FieldType.java` | ⚠️ In parent package |
| `field/registry/FieldRegistry.java` | `field/FieldRegistry.java` | ⚠️ In parent package |
| `field/registry/FieldLoader.java` | `field/FieldLoader.java` | ⚠️ In parent package |

## 2. Missing Files

| File | Status | Priority |
|------|--------|----------|
| `visual/render/RenderLayerFactory.java` | ❌ Not created | Low (FieldRenderLayers exists) |
| `visual/mesh/sphere/SphereAlgorithm.java` | 🔮 P3 deferred | Low |
| `visual/mesh/sphere/TypeASphere.java` | 🔮 P3 deferred | Low |
| `visual/mesh/sphere/TypeESphere.java` | 🔮 P3 deferred | Low |

## 3. API Deviations

### Appearance Record
| ARCHITECTURE.md | Actual | 
|-----------------|--------|
| `FillMode fill` | `boolean fill` | ❌ Different type |

### FieldDefinition
| ARCHITECTURE.md | Actual |
|-----------------|--------|
| `List<Primitive> primitives` | `List<FieldLayer> layers` | ⚠️ Different abstraction |

---

## 4. Missing Alpha Profiles (Based on Old Profiles)

### Shield Profiles NOT Converted
| Old Profile | Old File | New Alpha |
|-------------|----------|-----------|
| anti-virus | `anti_virus_field.json` | ❌ NOT CREATED |
| singularity | `singularity_field.json` | ❌ NOT CREATED |
| minimal | `minimal_field.json` | ❌ NOT CREATED |
| dual-layer | `dual_layer_field.json` | ❌ NOT CREATED |
| checker-frame | `checker_frame_field.json` | ❌ NOT CREATED |

### Personal Profiles NOT Converted
| Old Profile | Old File | New Alpha |
|-------------|----------|-----------|
| personal-default | `personal_default.json` | ❌ NOT CREATED |
| striped | `personal/striped.json` | ❌ NOT CREATED |
| meshed | `personal/meshed.json` | ❌ NOT CREATED |
| rings | `personal/rings.json` | ❌ NOT CREATED |
| fraction-8 | `personal/fraction-8.json` | ❌ NOT CREATED |
| fraction-16 | `personal/fraction-16.json` | ❌ NOT CREATED |

### Singularity NOT Converted
| Old Config | New Alpha |
|------------|-----------|
| `SingularityVisualConfig` | ❌ NOT CREATED |

### Growth Field Profiles NOT Converted  
| Old Config | New Alpha |
|------------|-----------|
| `FieldProfile` | ❌ NOT CREATED |
| `ForceProfile` | ❌ NOT CREATED |

---

## 5. Actions Required

### HIGH PRIORITY - Fix Deviations
1. Move `Animation.java` from `field/primitive/` to `visual/animation/`
2. Change `Appearance.fill` from `boolean` to `FillMode`

### MEDIUM PRIORITY - Create Real Alpha Profiles
Based on actual old profiles:
- `alpha_antivirus.json` ← from `anti_virus_field.json`
- `alpha_singularity.json` ← from `singularity_field.json`  
- `alpha_minimal.json` ← from `minimal_field.json`
- `alpha_dual_layer.json` ← from `dual_layer_field.json`
- `alpha_checker_frame.json` ← from `checker_frame_field.json`
- `alpha_personal_default.json` ← from `personal_default.json`
- `alpha_personal_striped.json` ← from `personal/striped.json`
- `alpha_personal_meshed.json` ← from `personal/meshed.json`
- `alpha_personal_rings.json` ← from `personal/rings.json`
- `alpha_personal_fraction.json` ← from `personal/fraction-*.json`
- `alpha_singularity_orb.json` ← from `SingularityVisualConfig`
- `alpha_growth_shell.json` ← from `FieldProfile`

