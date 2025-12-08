# Current Mesh System Analysis

> Deep dive into the existing shield mesh system to inform the field refactor.

---

## Overview

The current system uses **latitude/longitude sphere tessellation** with:
- Multi-layer composition
- Per-layer mesh type filtering (BANDS, WIREFRAME, CHECKER)
- Triangle winding configuration per quad

---

## System Components

### 1. Profile (ShieldProfileConfig)
The top-level configuration containing:
- Radius, visual scale, spin speed, tilt
- Global mesh settings (lat_steps, lon_steps, swirl_strength)
- Multiple named layers
- Colors, alpha ranges
- Beam configuration

### 2. Layer (ShieldMeshLayerConfig)
Each layer defines:
```
mesh_type: SOLID | BANDS | WIREFRAME | CHECKER | HEMISPHERE
lat_steps, lon_steps          # Tessellation resolution (2-512, 4-1024)
lat_start, lat_end            # 0-1, partial sphere (vertical range)
lon_start, lon_end            # 0-1, partial sphere (horizontal range)
radius_multiplier             # Layer scale
swirl_strength                # Twist effect
phase_offset                  # Animation offset
alpha_min, alpha_max          # Alpha gradient by latitude
primary_color, secondary_color # Color gradient by latitude
band_count, band_thickness    # For BANDS mode
wire_thickness                # For WIREFRAME mode
```

### 3. The Three-Level Indirection

```
Profile → Layer references:
  - "style": "sphere_plain"     → ShieldMeshStyleStore
  - "shape": "sphere_bands"     → ShieldMeshShapeStore
  - "triangle": "meshed_4"      → ShieldTriangleTypeStore
```

| Store | Purpose | Presets |
|-------|---------|---------|
| StyleStore | Full layer config defaults | sphere_plain, sphere_line, sphere_triangle, sphere_square, sphere_wireframe, sphere_rings, sphere_spiral, sphere_core |
| ShapeStore | Mesh type + band/wire settings | sphere_plain, sphere_line, sphere_square, sphere_arrow, sphere_checker, sphere_wireframe, sphere_bands |
| TriangleStore | Quad→triangle winding order | **50+ presets** |

---

## Triangle System Analysis

### What It Actually Does

Each lat/lon cell is a **quad** (4 corners). The triangle config defines how to split that quad into triangles:

```java
public record ShieldTriangleTypeConfig(List<Corner[]> triangles) {
    enum Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
}
```

**Default winding (2 triangles per quad):**
```
Triangle 1: TOP_LEFT → TOP_RIGHT → BOTTOM_LEFT
Triangle 2: TOP_RIGHT → BOTTOM_RIGHT → BOTTOM_LEFT
```

### The 50+ Presets

| Category | Files | Purpose |
|----------|-------|---------|
| `filled_1.json` | 1 | Standard fill |
| `meshed_1` to `meshed_7` | 7 | Offset fill variants |
| `triangle_single_type_a_1-6` | 6 | Single triangle experiments |
| `triangle_single_type_b_1-6` | 6 | Single triangle experiments |
| `triangle_triple_type_1-6` | 6 | Triple triangle experiments |
| `triangle_other_type_1-36` | 36 | Various experiments |
| Named (`parallelogram`, `triangle_facing`, etc.) | ~8 | Specific patterns |

### What's Actually Useful

After analysis, most produce nearly identical or broken results. The actually useful ones:

1. **filled_1** - Standard quad fill (default)
2. **meshed_4** - Offset winding for visual variety
3. **triangle_facing** - Triangles pointing outward (chevron effect)
4. **parallelogram** - Skewed quad for motion blur effect

**The rest are exploration artifacts that should be removed.**

---

## Rendering Pipeline

```java
// From ShieldFieldVisualManager.renderLayer()
for (int lat = 0; lat < latSteps; lat++) {
    float theta0 = lat0Norm * PI;
    float theta1 = lat1Norm * PI;
    
    for (int lon = 0; lon < lonSteps; lon++) {
        // Mesh type filter (BANDS, CHECKER, etc.)
        if (!layer.shouldRenderCell(lat, latSteps, lon, lonSteps, latFrac, lonFrac)) {
            continue;
        }
        
        // Apply swirl offset
        float swirlOffset = swirl * (latFrac - 0.5);
        float phi0 = (lon0Norm * TAU) + basePhase + swirlOffset;
        float phi1 = (lon1Norm * TAU) + basePhase + swirlOffset;
        
        // Emit triangles using configured winding
        for (Corner[] tri : triangleType.triangles()) {
            addCornerVertex(matrix, ..., tri[0], theta0, theta1, phi0, phi1, ...);
            addCornerVertex(matrix, ..., tri[1], theta0, theta1, phi0, phi1, ...);
            addCornerVertex(matrix, ..., tri[2], theta0, theta1, phi0, phi1, ...);
        }
    }
}
```

### Vertex Position Calculation

```java
float sinTheta = sin(theta);
float x = sinTheta * cos(phi);
float y = cos(theta);
float z = sinTheta * sin(phi);
// Apply radius scale, then matrix transform
```

This is standard **spherical coordinate → Cartesian** conversion.

---

## Comparison: Current vs External Generator

### Current System (Lat/Lon Tessellation)
```
+ Precise control over sphere regions
+ Built-in swirl effect
+ Easy gradient mapping (by latitude)
- Higher triangle count
- Complex configuration
- Pole artifacts (triangle collapse at poles)
```

### External Generator (Type A - Overlapping Cubes)
```
+ Fewer elements for same visual quality
+ No pole artifacts
+ Simple API
- Less precise control
- Harder to do partial spheres
- No built-in gradients
```

### External Generator (Type E - Rotated Rectangles)
```
+ Most efficient element count
+ Looks round from distance
+ Good for animated/distant spheres
- Not truly spherical up close
- Limited gradient control
```

---

## What To Keep

### Definitely Keep
1. **Multi-layer composition** - Essential for complex visuals
2. **Mesh type filtering** - BANDS, WIREFRAME, CHECKER are useful
3. **Lat/lon range control** - Partial spheres (caps, bands)
4. **Per-layer color/alpha** - Gradient effects
5. **Swirl effect** - Unique visual
6. **Phase offsets** - Independent animation per layer

### Simplify
1. **Triangle system** → Replace with `FillPattern` enum:
   - `QUADS` (default)
   - `TRIANGLES_OUT` (facing outward)
   - `TRIANGLES_ALT` (alternating)
   
2. **Style/Shape indirection** → Merge into layer config

3. **Mutable classes** → Immutable records

---

## New Field Layer Proposal

```java
public record FieldLayer(
    String name,
    
    // Tessellation
    int latSteps,           // default 64
    int lonSteps,           // default 128
    float latStart,         // 0.0 (top pole)
    float latEnd,           // 1.0 (bottom pole)
    float lonStart,         // 0.0
    float lonEnd,           // 1.0
    
    // Fill
    FillMode fillMode,      // SOLID, BANDS, WIREFRAME, CHECKER
    FillPattern pattern,    // QUADS, TRIANGLES_OUT, TRIANGLES_ALT
    int bandCount,          // for BANDS/CHECKER
    float bandThickness,    // for BANDS
    float wireThickness,    // for WIREFRAME
    
    // Appearance
    Color color,            // or Gradient
    Alpha alpha,            // animated alpha
    boolean glow,
    
    // Motion
    Spin spin,
    float swirl,
    float phaseOffset,
    float radiusMultiplier
) {
    public static Builder builder(String name);
}
```

This **collapses 3 concepts into 1** while keeping all the useful functionality.

---

## Triangle Presets to Migrate

| Old Preset | New Pattern | Notes |
|------------|-------------|-------|
| filled_1 | `QUADS` | Default |
| meshed_4 | `QUADS` | Same visual, different winding |
| triangle_facing | `TRIANGLES_OUT` | Chevron effect |
| parallelogram | (custom) | Could be its own pattern if needed |

**All 50+ others:** DELETE

---

## Profile JSON Migration

### Old Format
```json
{
  "mesh": {
    "lat_steps": 72,
    "lon_steps": 200,
    "swirl_strength": 0.45,
    "layers": {
      "core": {
        "style": "sphere_plain",
        "shape": "sphere_plain",
        "triangle": "meshed_4",
        "radius_multiplier": 0.95,
        ...
      }
    }
  }
}
```

### New Format
```json
{
  "appearance": {
    "layers": [
      {
        "name": "core",
        "tessellation": { "lat": 72, "lon": 200 },
        "swirl": 0.45,
        "fillMode": "SOLID",
        "fillPattern": "QUADS",
        "radiusMultiplier": 0.95,
        "color": "#FFF5F9FF",
        "alpha": { "base": 0.5, "pulse": 0.2 }
      }
    ]
  }
}
```

**Simpler, flatter, no indirection.**

