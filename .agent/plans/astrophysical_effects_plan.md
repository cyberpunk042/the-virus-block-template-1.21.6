# Astrophysical Visual Effects Implementation Plan

**Status**: DRAFT - In Discussion  
**Last Updated**: 2024-12-24

## Overview

This plan covers the implementation of astrophysical visual effects for cosmic phenomena:
- Black holes with accretion disks
- Relativistic jets with precession
- Absorption rays (light converging inward)
- Pulsars and magnetars

---

## Phase 1: Relativistic Jets

### 1.1 New Shape: `JetShape`

**Concept**: A pair of opposing tubes/cones emanating from a central point (like poles of a sphere).

**Geometry Approach**: Cylinder-based (tube with configurable radii)
- `tipRadius = 0` → Pure cone (pointed)
- `tipRadius > 0` → Truncated cone / tube
- This leverages existing cylinder tessellation patterns

**Properties**:
```java
record JetShape(
    // === Geometry ===
    float length,           // Length of each jet (default: 2.0)
    float baseRadius,       // Radius at the base where jet starts (default: 0.3)
    float tipRadius,        // Radius at the tip (0 = cone, >0 = tube, default: 0)
    int segments,           // Radial segments (default: 16)
    int lengthSegments,     // Segments along length for animation/patterns (default: 8)
    
    // === Configuration ===
    boolean dualJets,       // true = both poles, false = single jet (default: true)
    float separation,       // Gap between jets at center (default: 0)
    boolean hollow,         // true = tube (hollow), false = solid cone (default: false)
    float wallThickness,    // If hollow, thickness of wall (default: 0.05)
    
    // === Precession ===
    JetPrecession precession // Precession animation config (can be null)
) implements Shape
```

**Hollow vs Solid Support**:
- `hollow = false` → Solid cone/cylinder (closed ends)
- `hollow = true` → Tube with inner wall (like Ring with height)

### 1.2 Jet Precession Config

**Concept**: The jet axis slowly wobbles in a conical pattern (like a spinning top).
This is SEPARATE from SpinConfig - the parent shape can spin independently.

```java
record JetPrecession(
    boolean enabled,        // Is precession active?
    float angle,            // Cone angle of precession in degrees (default: 15)
    float speed,            // Rotation speed in revs/sec (default: 0.1)
    float phase,            // Starting phase offset 0-1 (default: 0)
    boolean syncWithParent  // Sync precession timing with parent spin? (default: false)
)
```

### 1.3 Integration with Primitives

**Decision**: **Standalone Primitive with PrimitiveLink**
- More flexible
- Consistent with existing multi-primitive architecture
- Can share or differ in appearance from parent

### 1.4 Tessellation

**JetTessellator**:
- Generate cone/cylinder geometry for each jet
- If `dualJets=true`, generate both +Y and -Y directions
- Apply precession by rotating the entire mesh
- Support patterns for visual effects (e.g., striped jets)

### 1.5 Files to Create/Modify

| File | Action | Description |
|------|--------|-------------|
| `JetShape.java` | CREATE | New shape record |
| `JetPrecession.java` | CREATE | Precession config record |
| `JetTessellator.java` | CREATE | Tessellation logic |
| `JetRenderer.java` | CREATE | Rendering logic |
| `ShapeRegistry.java` | MODIFY | Register jet shape |
| `PrimitiveRenderers.java` | MODIFY | Register jet renderer |
| `ShapeTypeAdapter.java` | MODIFY | JSON parsing |
| `ShapeSubPanel.java` | MODIFY | Add jet to GUI |

---

## Phase 2: Absorption Rays

### 2.1 Concept

Rays that converge toward the center of a shape, representing light being pulled into a gravitational source.

**Visual**: Lines emanating from outside the shape, all pointing toward center.
Can cover partial or FULL spherical coverage (360° x 360°).

### 2.2 Design Decision

**Decision**: Implement as **geometry lines** (new standalone shape)
- Uses existing `MeshBuilder.lines()` 
- No shader pipeline changes needed
- Consistent with Jets approach
- Fully configurable and reusable

### 2.3 New Shape: `AbsorptionRaysShape`

**Fully Configurable** - supports massive ray fields:

```java
record AbsorptionRaysShape(
    // === Geometry ===
    float innerRadius,          // Where rays end (near center, default: 0.5)
    float outerRadius,          // Where rays start (far from center, default: 3.0)
    float rayWidth,             // Line thickness (default: 1.0)
    float rayLength,            // Individual ray length (if < outer-inner, rays are segments)
    
    // === Distribution ===
    RayDistribution distribution, // RADIAL_UNIFORM, RADIAL_RANDOM, SPHERICAL_UNIFORM, SPHERICAL_RANDOM
    int rayCountTheta,          // Rays around Y axis (horizontal, 0-360°, default: 24)
    int rayCountPhi,            // Rays around X axis (vertical, 0-180°, default: 12)
    float thetaMin,             // Min horizontal angle in degrees (default: 0)
    float thetaMax,             // Max horizontal angle in degrees (default: 360)
    float phiMin,               // Min vertical angle in degrees (default: 0)
    float phiMax,               // Max vertical angle in degrees (default: 180)
    float randomness,           // 0 = perfect grid, 1 = fully random positions (default: 0)
    
    // === Animation ===
    boolean animated,           // Do rays move inward? (default: true)
    float animSpeed,            // Speed of inward motion (default: 1.0)
    float spawnRate,            // Rays per second (0 = static rays, default: 0)
    float rayLifetime,          // How long each ray lives before respawning (default: 2.0)
    boolean fadeIn,             // Rays fade in at outer edge (default: true)
    boolean fadeOut,            // Rays fade out at inner edge (default: true)
    
    // === Appearance (shape-level, Appearance record handles color/glow) ===
    boolean convergeTocenter,   // true = to center, false = from center (emission rays)
    float density               // Ray density multiplier (default: 1.0)
) implements Shape
```

**Distribution Modes**:
```java
enum RayDistribution {
    RADIAL_UNIFORM,     // Even spacing in 2D ring pattern (like Disc)
    RADIAL_RANDOM,      // Random angles in 2D ring pattern
    SPHERICAL_UNIFORM,  // Even lat/lon grid over sphere surface
    SPHERICAL_RANDOM    // Random positions on sphere surface
}
```

**Example Configurations**:
```json
// Massive 360x360 ray field
{
  "type": "absorptionRays",
  "outerRadius": 5.0,
  "rayCountTheta": 360,
  "rayCountPhi": 180,
  "distribution": "SPHERICAL_UNIFORM",
  "animated": true,
  "animSpeed": 0.5
}

// Sparse random rays (more organic)
{
  "type": "absorptionRays",
  "outerRadius": 4.0,
  "rayCountTheta": 24,
  "rayCountPhi": 12,
  "distribution": "SPHERICAL_RANDOM",
  "randomness": 0.8
}

// Emission rays (outward from center)
{
  "type": "absorptionRays",
  "convergeToCenter": false,
  "outerRadius": 3.0,
  "animSpeed": 2.0
}
```

### 2.4 Files to Create/Modify

| File | Action | Description |
|------|--------|-------------|
| `AbsorptionRaysShape.java` | CREATE | New shape record |
| `RayDistribution.java` | CREATE | Distribution enum |
| `AbsorptionRaysTessellator.java` | CREATE | Line generation |
| `AbsorptionRaysRenderer.java` | CREATE | Rendering + animation |
| Registry/Adapter files | MODIFY | Registration |

---

## Phase 3: Presets and Compositions

### 3.1 Black Hole Preset

Once all components exist, create a preset that combines them:

```json
{
  "id": "black_hole",
  "layers": [{
    "primitives": [
      { 
        "id": "event_horizon",
        "shape": { "type": "sphere", "radius": 0.5 },
        "appearance": { "color": "#000000", "glow": 0.8 }
      },
      {
        "id": "accretion_disk", 
        "shape": { "type": "ring", "innerRadius": 0.6, "outerRadius": 2.0, "height": 0.1 },
        "animation": { "spin": { "speedY": 0.5 } }
      },
      {
        "id": "jets",
        "shape": { "type": "jet", "length": 5.0, "precession": { "angle": 10 } },
        "link": { "parentId": "event_horizon" }
      },
      {
        "id": "rays",
        "shape": { "type": "absorptionRays", "outerRadius": 4.0, "rayCount": 24 }
      }
    ]
  }]
}
```

### 3.2 Pulsar Preset

```json
{
  "id": "pulsar",
  "layers": [{
    "primitives": [
      {
        "id": "star",
        "shape": { "type": "sphere", "radius": 0.3 },
        "appearance": { "color": "#FFFFFF", "glow": 1.0 }
      },
      {
        "id": "beams",
        "shape": { "type": "jet", "length": 8.0, "baseRadius": 0.1 },
        "animation": { "spin": { "speedY": 2.0 } }
      }
    ]
  }]
}
```

---

## Implementation Order

### Step 1: JetShape Foundation
1. Create `JetShape.java` record
2. Create `JetTessellator.java` (basic cone geometry)
3. Create `JetRenderer.java`
4. Register in ShapeRegistry, PrimitiveRenderers
5. Add JSON parsing
6. Test basic jet rendering

### Step 2: Jet Features
1. Add `dualJets` toggle
2. Add `separation` parameter
3. Add pattern support for jet surface

### Step 3: Jet Precession
1. Create `JetPrecession.java`
2. Integrate precession into renderer (rotates jet axis over time)
3. Add GUI controls

### Step 4: AbsorptionRays
1. Create `AbsorptionRaysShape.java`
2. Create line tessellation
3. Add animation (rays moving inward)
4. Register and test

### Step 5: GUI & Presets
1. Add JetShape controls to ShapeSubPanel
2. Add AbsorptionRays controls
3. Create black_hole.json preset
4. Create pulsar.json preset

---

## Resolved Design Decisions

| Question | Decision |
|----------|----------|
| Jet geometry | **Cylinder-based** - `tipRadius=0` gives cone, `tipRadius>0` gives tube |
| Jet hollow/solid | Support **BOTH** via `hollow` parameter |
| Absorption rays distribution | **FULLY CONFIGURABLE** - radial/spherical, uniform/random |
| Rays as geometry or shader | **Geometry lines** - simpler, fits existing architecture |
| Jet appearance | **Own appearance** - it's a standalone shape with own Appearance config |
| Jet precession | **Separate from SpinConfig** - lives in JetPrecession config |

---

## Summary

### New Files to Create

**Phase 1 - Jets:**
- `src/main/java/net/cyberpunk042/visual/shape/JetShape.java`
- `src/main/java/net/cyberpunk042/visual/shape/JetPrecession.java`
- `src/client/java/net/cyberpunk042/client/visual/mesh/JetTessellator.java`
- `src/client/java/net/cyberpunk042/client/field/render/JetRenderer.java`

**Phase 2 - Absorption Rays:**
- `src/main/java/net/cyberpunk042/visual/shape/AbsorptionRaysShape.java`
- `src/main/java/net/cyberpunk042/visual/shape/RayDistribution.java`
- `src/client/java/net/cyberpunk042/client/visual/mesh/AbsorptionRaysTessellator.java`
- `src/client/java/net/cyberpunk042/client/field/render/AbsorptionRaysRenderer.java`

### Files to Modify
- `ShapeRegistry.java` - register new shapes
- `PrimitiveRenderers.java` - register new renderers
- `ShapeTypeAdapter.java` - JSON parsing
- `ShapeSubPanel.java` - GUI controls
- `ShapeAdapter.java` - state management
- `FieldEditState.java` - state bindings
- `DefaultsProvider.java` - default values

---

## Notes

- Ring shape is now working with proper CellType.QUAD ✅
- DiscShape has been removed ✅
- PrimitiveLink system exists for parent-child relationships
- Existing animation system (SpinConfig) is separate from jet precession
- Cone shape may have tessellation issues (winding bug) - Jets will use cylinder-based approach

---

## Next Steps

For the next conversation, start with:
1. Implement `JetShape.java` record
2. Implement `JetTessellator.java` 
3. Test basic jet rendering
4. Then add hollow/solid, dual jets, precession
5. Finally implement AbsorptionRays
