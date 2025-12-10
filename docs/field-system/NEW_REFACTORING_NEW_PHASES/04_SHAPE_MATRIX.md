# Complete Shape & Type Parameter Matrix

> **Purpose:** Every possible customization parameter for every shape and type  
> **Status:** Comprehensive inventory  
> **Date:** December 7, 2024  
> **Last Updated:** December 9, 2024 - Animation features complete

---

## Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Implemented in code |
| 📋 | Documented but not implemented |
| ❓ | Potential addition (not documented) |
| 🔮 | Future phase |

---

## 0. Shape/CellType/Pattern Compatibility Matrix

> **IMPORTANT:** Not all patterns work with all shapes. The GUI filters patterns by CellType.

### Shape → CellType Mapping

| Shape | Primary CellType | Parts |
|-------|-----------------|-------|
| **Sphere** | QUAD | main, poles, equator, hemisphereTop, hemisphereBottom |
| **Ring** | SEGMENT | main |
| **Disc** | SECTOR | main |
| **Prism** | QUAD | sides (QUAD), caps (SECTOR) |
| **Cylinder** | QUAD | sides (QUAD), caps (SECTOR) |
| **Polyhedron** | QUAD or TRIANGLE | Depends on polyType |

### CellType → Compatible Patterns

| CellType | Compatible Patterns | Example Use |
|----------|---------------------|-------------|
| **QUAD** | filled_1, triangle_1-4, wave_1, tooth_1, parallelogram_1-2, stripe_1 | Sphere lat/lon cells, Prism sides |
| **SEGMENT** | full, alternating, sparse, quarter, zigzag, dashed | Ring segments |
| **SECTOR** | full, half, quarters, pinwheel, trisector, spiral, crosshair | Disc wedges, caps |
| **EDGE** | full, latitude, longitude, sparse, minimal, dashed, grid | Wireframe edges |
| **TRIANGLE** | full, alternating, inverted, sparse, fan, radial | Icosphere faces |

### ⚠️ Incompatible Combinations (Will Log Error)

| Shape | Incompatible Patterns | Reason |
|-------|----------------------|--------|
| Ring | QUAD patterns (filled_1, etc.) | Ring uses SEGMENT cells |
| Disc | QUAD/SEGMENT patterns | Disc uses SECTOR cells |
| Sphere | SECTOR/SEGMENT patterns | Sphere uses QUAD cells |

> **Runtime:** `PatternResolver` validates and logs mismatches

---

## 1. SPHERE Parameters

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| **Core** |
| `radius` | float | 1.0 | 0.01-∞ | ✅ | Sphere radius |
| `latSteps` | int | 32 | 2-512 | ✅ | Latitude divisions |
| `lonSteps` | int | 32 | 4-1024 | ✅ | Longitude divisions |
| **Partial Sphere** |
| `latStart` | float | 0.0 | 0-1 | ✅ | Start latitude (0=top) |
| `latEnd` | float | 1.0 | 0-1 | ✅ | End latitude (1=bottom) |
| `lonStart` | float | 0.0 | 0-1 | ✅ | Start longitude |
| `lonEnd` | float | 1.0 | 0-1 | ✅ | End longitude |
| **Algorithm** |
| `algorithm` | enum | LAT_LON | - | ✅ | LAT_LON, TYPE_A, TYPE_E |
| **Potential Additions** |
| `subdivisions` | int | 0 | 0-5 | ❓ | For icosphere (TYPE_E) |
| `uvScale` | Vec2 | (1,1) | - | ❓ | UV texture scaling |
| `poleMode` | enum | VERTEX | - | ❓ | VERTEX, SPLIT, NONE |

### Sphere CellType: QUAD (lat/lon), TRIANGLE (icosphere)

---

## 2. RING Parameters

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| **Core (Current Implementation)** |
| `y` | float | 0.0 | -∞-∞ | ✅ | Y position |
| `radius` | float | 1.0 | 0.01-∞ | ✅ | Ring radius |
| `thickness` | float | 0.1 | 0.01-∞ | ✅ | Ring thickness |
| `segments` | int | 32 | 3-1024 | ✅ | Segment count |
| **Alternative (PARAMETER_INVENTORY)** |
| `innerRadius` | float | - | 0-∞ | 📋 | Inner ring radius |
| `outerRadius` | float | - | 0-∞ | 📋 | Outer ring radius |
| **Partial Arc** |
| `arcStart` | float | 0 | 0-360 | 📋 | Arc start angle (degrees) |
| `arcEnd` | float | 360 | 0-360 | 📋 | Arc end angle |
| **Advanced** |
| `height` | float | 0 | 0-∞ | 📋 | Ring height (3D ring/tube) |
| `twist` | float | 0 | -360-360 | 📋 | Twist along arc (Möbius) |
| **Potential Additions** |
| `tubeSegments` | int | - | 3-32 | ❓ | Cross-section segments (3D) |
| `profile` | enum | FLAT | - | ❓ | FLAT, ROUND, SQUARE |

### Ring CellType: SEGMENT

---

## 3. DISC Parameters

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| **Core** |
| `y` | float | 0.0 | -∞-∞ | ✅ | Y position |
| `radius` | float | 1.0 | 0.01-∞ | ✅ | Disc radius |
| `segments` | int | 32 | 3-1024 | ✅ | Segment count |
| **Partial Arc (Pac-Man)** |
| `arcStart` | float | 0 | 0-360 | 📋 | Arc start angle |
| `arcEnd` | float | 360 | 0-360 | 📋 | Arc end angle |
| **Annulus (Ring-like disc)** |
| `innerRadius` | float | 0 | 0-∞ | 📋 | Inner cutout radius |
| **Concentric** |
| `rings` | int | 1 | 1-100 | 📋 | Concentric ring divisions |
| **Potential Additions** |
| `spiralTurns` | float | 0 | 0-10 | ❓ | Spiral effect |

### Disc CellType: SECTOR

---

## 4. PRISM Parameters

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| **Core** |
| `sides` | int | 6 | 3-64 | ✅ | Number of sides |
| `radius` | float | 1.0 | 0.01-∞ | ✅ | Circumscribed radius |
| `height` | float | 2.0 | 0.01-∞ | ✅ | Prism height |
| **Taper** |
| `topRadius` | float | same | 0-∞ | 📋 | Top radius (for pyramid) |
| **Twist** |
| `twist` | float | 0 | -360-360 | 📋 | Twist along height |
| **Divisions** |
| `heightSegments` | int | 1 | 1-100 | 📋 | Vertical divisions |
| **Caps** |
| `capTop` | boolean | true | - | 📋 | Render top cap |
| `capBottom` | boolean | true | - | 📋 | Render bottom cap |
| **Potential Additions** |
| `sideAngle` | float | 0 | -45-45 | ❓ | Bevel angle |
| `rounded` | boolean | false | - | ❓ | Rounded corners |

### Prism CellType: QUAD (sides) + SECTOR (caps)

---

## 5. POLYHEDRON Parameters

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| **Core** |
| `polyType` | enum | CUBE | - | ✅ | CUBE, OCTAHEDRON, ICOSAHEDRON, DODECAHEDRON, TETRAHEDRON |
| `size` / `radius` | float | 1.0 | 0.01-∞ | ✅ | Circumscribed radius |
| **Subdivision** |
| `subdivisions` | int | 0 | 0-5 | 📋 | Subdivision level |
| **Potential Additions** |
| `dualMode` | boolean | false | - | ❓ | Show dual polyhedron |
| `edgeBevel` | float | 0 | 0-1 | ❓ | Edge beveling |
| `faceBevel` | float | 0 | 0-1 | ❓ | Face beveling |

### Polyhedron CellType: QUAD (cube) or TRIANGLE (others)

---

## 6. CYLINDER Parameters

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| **Core** |
| `radius` | float | 0.5 | 0.01-∞ | ✅ | Cylinder radius |
| `height` | float | 10.0 | 0.01-∞ | ✅ | Cylinder height |
| `segments` | int | 16 | 3-128 | ✅ | Radial segments |
| **Taper** |
| `topRadius` | float | same | 0-∞ | 📋 | Top radius (cone-like) |
| **Divisions** |
| `heightSegments` | int | 1 | 1-100 | 📋 | Height divisions |
| **Caps** |
| `capTop` | boolean | true | - | 📋 | Render top cap |
| `capBottom` | boolean | false | - | 📋 | Render bottom cap |
| `openEnded` | boolean | true | - | 📋 | No caps (tube) |
| **Partial Arc** |
| `arc` | float | 360 | 0-360 | 📋 | Partial cylinder |
| **Potential Additions** |
| `ellipse` | boolean | false | - | ❓ | Oval cross-section |
| `radiusX` | float | - | 0-∞ | ❓ | X radius for ellipse |
| `radiusZ` | float | - | 0-∞ | ❓ | Z radius for ellipse |

### Cylinder CellType: QUAD (sides) + SECTOR (caps)

---

## 7. TORUS Parameters (FUTURE)

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| `majorRadius` | float | 1.0 | 0.01-∞ | 🔮 | Ring radius |
| `minorRadius` | float | 0.3 | 0.01-∞ | 🔮 | Tube radius |
| `majorSegments` | int | 32 | 3-256 | 🔮 | Segments around ring |
| `minorSegments` | int | 16 | 3-64 | 🔮 | Segments around tube |
| `arc` | float | 360 | 0-360 | 🔮 | Partial torus |
| `twist` | int | 0 | -10-10 | 🔮 | Möbius-like twist |
| `tubeArc` | float | 360 | 0-360 | ❓ | Partial tube |
| `knotP` | int | 2 | 1-10 | ❓ | Torus knot P |
| `knotQ` | int | 3 | 1-10 | ❓ | Torus knot Q |

### Torus CellType: QUAD

---

## 8. CONE Parameters (FUTURE)

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| `radiusBottom` | float | 1.0 | 0-∞ | 🔮 | Bottom radius |
| `radiusTop` | float | 0.0 | 0-∞ | 🔮 | Top radius (0=point) |
| `height` | float | 1.0 | 0.01-∞ | 🔮 | Cone height |
| `segments` | int | 32 | 3-128 | 🔮 | Radial segments |
| `heightSegments` | int | 1 | 1-100 | 🔮 | Height divisions |
| `capBottom` | boolean | true | - | 🔮 | Render bottom cap |
| `arc` | float | 360 | 0-360 | 🔮 | Partial cone |

### Cone CellType: QUAD (sides) + SECTOR (base)

---

## 9. HELIX Parameters (FUTURE)

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| `radius` | float | 1.0 | 0.01-∞ | 🔮 | Helix radius |
| `height` | float | 3.0 | 0.01-∞ | 🔮 | Total height |
| `turns` | float | 3.0 | 0.1-20 | 🔮 | Number of turns |
| `tubeRadius` | float | 0.1 | 0.01-∞ | 🔮 | Tube thickness |
| `segments` | int | 64 | 8-256 | 🔮 | Segments per turn |
| `tubeSegments` | int | 8 | 3-32 | 🔮 | Tube cross-section |
| `direction` | enum | CCW | - | 🔮 | CW, CCW |
| `taper` | float | 0 | -1-1 | ❓ | Taper along height |
| `doubleHelix` | boolean | false | - | ❓ | DNA-like |

### Helix CellType: QUAD

---

## 10. TRANSFORM Parameters

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| **Position (Implemented)** |
| `offset` | Vec3 | (0,0,0) | ✅ | Offset from field center |
| **Position (Proposed)** |
| `anchor` | enum | CENTER | 📋 | CENTER, FEET, HEAD, ABOVE, BELOW, FRONT, BACK, LEFT, RIGHT |
| **Rotation (Implemented)** |
| `rotation` | Vec3 | (0,0,0) | ✅ | Static rotation (degrees) |
| **Rotation (Proposed)** |
| `facing` | enum | FIXED | 📋 | FIXED, PLAYER_LOOK, VELOCITY, CAMERA |
| `up` | enum | WORLD_UP | 📋 | WORLD_UP, PLAYER_UP, VELOCITY, CUSTOM |
| `billboard` | enum | NONE | 📋 | NONE, FULL, Y_AXIS |
| `inheritRotation` | boolean | true | 📋 | Inherit layer rotation |
| **Scale (Implemented)** |
| `scale` | float | 1.0 | ✅ | Uniform scale |
| **Scale (Proposed)** |
| `scaleXYZ` | Vec3 | (1,1,1) | 📋 | Per-axis scale |
| `scaleWithRadius` | boolean | false | 📋 | Scale with baseRadius |
| **Dynamic (Proposed)** |
| `orbit.enabled` | boolean | false | 📋 | Enable orbit |
| `orbit.radius` | float | 2.0 | 📋 | Orbit radius |
| `orbit.speed` | float | 1.0 | 📋 | Orbit speed |
| `orbit.axis` | enum | Y | 📋 | X, Y, Z |
| `orbit.phase` | float | 0.0 | 📋 | Starting phase |

---

## 11. FILL Parameters

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| **Core** |
| `mode` | enum | SOLID | ✅ | SOLID, WIREFRAME, CAGE, POINTS |
| `wireThickness` | float | 1.0 | ✅ | Line thickness |
| **Render Options** |
| `doubleSided` | boolean | false | ✅ | Render both sides |
| `depthTest` | boolean | true | ✅ | Depth testing |
| `depthWrite` | boolean | false | ✅ | Write to depth |
| **Cage-Specific (SphereCageOptions)** |
| `cage.lineWidth` | float | 1.0 | ✅ | Line width |
| `cage.latitudeCount` | int | 8 | ✅ | Latitude lines |
| `cage.longitudeCount` | int | 16 | ✅ | Longitude lines |
| `cage.showEquator` | boolean | true | ✅ | Highlight equator |
| `cage.showPoles` | boolean | true | ✅ | Highlight poles |
| `cage.showEdges` | boolean | true | ✅ | Show edge lines |
| **Wireframe-Specific (Potential)** |
| `dashPattern` | array | - | ❓ | [on, off] lengths |
| `dashOffset` | float | 0 | ❓ | Dash start offset |
| **Points-Specific** |
| `pointSize` | float | 0.02 | ✅ | Point size (billboarded quads) |
| `pointShape` | enum | SQUARE | 🔮 | CIRCLE, SQUARE, STAR |

---

## 12. VISIBILITY MASK Parameters

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| **Core** |
| `mask` | enum | FULL | ⚠️ | FULL, BANDS, STRIPES, CHECKER, RADIAL, GRADIENT |
| `count` | int | 4 | ⚠️ | Division count |
| `thickness` | float | 0.5 | ⚠️ | Band/stripe thickness |
| **Proposed** |
| `offset` | float | 0.0 | 📋 | Pattern offset/phase |
| `invert` | boolean | false | 📋 | Invert visibility |
| `feather` | float | 0.0 | 📋 | Edge softness |
| `animate` | boolean | false | 📋 | Animate pattern |
| `animateSpeed` | float | 1.0 | 📋 | Animation speed |
| **Potential** |
| `direction` | float | 0 | ❓ | Bands/stripes angle (degrees) |
| `curve` | enum | LINEAR | ❓ | LINEAR, EASE, SMOOTH |
| **Gradient-Specific** |
| `gradientDir` | enum | VERTICAL | 📋 | VERTICAL, HORIZONTAL, RADIAL |
| `falloff` | enum | LINEAR | 📋 | LINEAR, EASE, SMOOTH |
| `start` | float | 0.0 | 📋 | Gradient start |
| `end` | float | 1.0 | 📋 | Gradient end |
| **Radial-Specific** |
| `centerX` | float | 0.5 | 📋 | Center X (0-1) |
| `centerY` | float | 0.5 | 📋 | Center Y (0-1) |

---

## 13. APPEARANCE Parameters

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| **Core (Implemented)** |
| `color` | string | "@primary" | ✅ | Color reference |
| `alpha` | range | 1.0 | ✅ | { min, max } or float |
| `glow` | float | 0.0 | ✅ | Glow intensity |
| **Phase 1 (Per user Q3)** |
| `emissive` | float | 0.0 | 📋 | Self-illumination |
| `saturation` | float | 1.0 | 📋 | Color saturation |
| `brightness` | float | 1.0 | 📋 | Brightness modifier |
| `hueShift` | float | 0.0 | 📋 | Hue rotation (0-360) |
| `secondaryColor` | string | null | 📋 | For gradients |
| `colorBlend` | float | 0.0 | 📋 | Primary↔Secondary |
| **Potential** |
| `fresnel` | float | 0 | ❓ | Edge glow effect |
| `metallicness` | float | 0 | ❓ | Metallic reflection |
| `roughness` | float | 0.5 | ❓ | Surface roughness |

---

## 14. ANIMATION Parameters

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| **Core (Implemented)** |
| `spin` | float | 0 | ✅ | Rotation speed |
| `spinAxis` | enum | Y | ✅ | X, Y, Z, CUSTOM |
| `pulse` | float | 0 | ✅ | Scale pulse speed |
| `pulseAmount` | float | 0 | ✅ | Scale pulse amplitude |
| `phase` | float | 0 | ✅ | Animation phase offset |
| `alphaPulse` | float | 0 | ✅ | Alpha pulse speed |
| `alphaPulseAmount` | float | 0 | ✅ | Alpha pulse amplitude |
| **SpinConfig (Implemented)** |
| `spin.axis` | enum | Y | ✅ | X, Y, Z, CUSTOM |
| `spin.speed` | float | 0.02 | ✅ | Rotation speed |
| `spin.oscillate` | boolean | false | ✅ | Back-and-forth mode |
| `spin.range` | float | 360 | ✅ | Oscillation range |
| `spin.customAxis` | Vec3 | null | ✅ | Custom rotation axis |
| **PulseConfig (Implemented)** |
| `pulse.waveform` | enum | SINE | ✅ | SINE, SQUARE, TRIANGLE_WAVE, SAWTOOTH |
| `pulse.speed` | float | 1.0 | ✅ | Pulse speed |
| `pulse.min` | float | 0.9 | ✅ | Minimum scale |
| `pulse.max` | float | 1.1 | ✅ | Maximum scale |
| **AlphaPulseConfig (Implemented)** |
| `alphaPulse.waveform` | enum | SINE | ✅ | SINE, SQUARE, TRIANGLE_WAVE, SAWTOOTH |
| `alphaPulse.speed` | float | 1.0 | ✅ | Pulse speed |
| `alphaPulse.min` | float | 0.5 | ✅ | Minimum alpha |
| `alphaPulse.max` | float | 1.0 | ✅ | Maximum alpha |
| **ColorCycleConfig (Implemented)** |
| `colorCycle.colors` | array | null | ✅ | List of hex colors ["#FF0000", "#00FF00"] |
| `colorCycle.speed` | float | 1.0 | ✅ | Cycle speed |
| `colorCycle.blend` | boolean | true | ✅ | Smooth blend vs hard cut |
| **WobbleConfig (Implemented)** |
| `wobble.amplitude` | Vec3 | (0.1,0.05,0.1) | ✅ | Wobble amplitude per axis |
| `wobble.speed` | float | 1.0 | ✅ | Wobble speed |
| **WaveConfig (Implemented)** |
| `wave.amplitude` | float | 0.1 | ✅ | Wave displacement amount |
| `wave.frequency` | float | 2.0 | ✅ | Wave frequency |
| `wave.direction` | enum | Y | ✅ | X, Y, Z - displacement axis |

> **Note:** All animation uses `MathHelper.sin()` (fast lookup table) and `ColorHelper.lerp()` for performance.

---

## 15. LAYER Parameters

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| **Implemented** |
| `id` | string | required | ✅ | Layer identifier |
| `primitives` | array | [] | ✅ | Layer primitives |
| `colorRef` | string | "@primary" | ✅ | Color override |
| `alpha` | float | 1.0 | ✅ | Alpha override |
| `spin` | object | null | ✅ | Layer spin |
| `tilt` | float | 0.0 | ✅ | Layer tilt |
| `pulse` | float | 0.0 | ✅ | Layer pulse |
| `phaseOffset` | float | 0.0 | ✅ | Animation phase |
| **Proposed** |
| `rotation` | Vec3 | (0,0,0) | 📋 | Static rotation |
| `visible` | boolean | true | 📋 | Layer visibility |
| `blendMode` | enum | NORMAL | 📋 | ADD, MULTIPLY, SCREEN |
| `order` | int | auto | 📋 | Render order |

---

## 16. FIELD DEFINITION Parameters

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| **Core** |
| `id` | Identifier | required | ✅ | Unique field ID |
| `type` | FieldType | required | ✅ | SHIELD, PERSONAL, FORCE, AURA, PORTAL |
| `baseRadius` | float | 1.0 | ✅ | Base scale |
| `themeId` | string | null | ✅ | Color theme |
| `layers` | array | [] | ✅ | Visual layers |
| **Modifiers** |
| `modifiers.visualScale` | float | 1.0 | ✅ | Overall scale |
| `modifiers.tilt` | float | 0.0 | ✅ | Global tilt |
| `modifiers.swirl` | float | 0.0 | ✅ | Swirl effect |
| `modifiers.pulsing` | float | 0.0 | ⚠️ | Global pulse |
| `modifiers.bobbing` | float | 0.0 | 📋 | Vertical bob |
| `modifiers.breathing` | float | 0.0 | 📋 | Scale breathing |
| **Prediction** |
| `prediction.enabled` | boolean | false | ✅ | Enable prediction |
| `prediction.leadTicks` | int | 2 | ✅ | Ticks ahead |
| `prediction.maxDistance` | float | 8.0 | ✅ | Max distance |
| `prediction.lookAhead` | float | 0.5 | ✅ | Look weight |
| `prediction.verticalBoost` | float | 0.0 | ✅ | Vertical boost |
| **Beam** |
| `beam.enabled` | boolean | false | ✅ | Show beam |
| `beam.innerRadius` | float | 0.05 | ✅ | Inner radius |
| `beam.outerRadius` | float | 0.1 | ✅ | Outer radius |
| `beam.color` | string | "@beam" | ✅ | Beam color |
| `beam.height` | float | auto | 📋 | Beam height |
| `beam.glow` | float | 0.5 | 📋 | Beam glow |
| `beam.pulse` | float | 0.0 | 📋 | Beam pulse |
| **Follow Mode** |
| `followMode.enabled` | boolean | true | ✅ | Follow enabled |
| `followMode.mode` | enum | SMOOTH | ✅ | SNAP, SMOOTH, GLIDE |
| `followMode.playerOverride` | boolean | true | 📋 | Player can change |

---

## 17. Summary: Implementation Status

### ✅ COMPLETE (December 2024)

| Category | Items |
|----------|-------|
| **All Core Shapes** | Sphere, Ring, Disc, Prism, Cylinder, Polyhedron |
| **Fill Modes** | SOLID, WIREFRAME, CAGE (with SphereCageOptions), POINTS |
| **Animation** | Spin, Pulse, AlphaPulse, ColorCycle, Wobble, Wave |
| **Transform** | Offset, Rotation, Scale, Anchor, Facing, Billboard |
| **Visibility** | FULL, BANDS, STRIPES, CHECKER, RADIAL, GRADIENT + offset/invert/feather |
| **Appearance** | Color, Alpha, Glow, Emissive, Saturation |
| **Prediction** | Enable, LeadTicks, MaxDistance, LookAhead, VerticalBoost |
| **Follow Mode** | SNAP, SMOOTH, GLIDE |
| **Beam** | Enable, Inner/Outer radius, Color, Height, Glow, Pulse |

### 📋 REMAINING (Phase 2-3)

| Priority | Category | Items |
|----------|----------|-------|
| **Medium** | Shape: Ring | `arcStart`, `arcEnd`, `height` (3D tube) |
| **Medium** | Shape: Disc | `arcStart`, `arcEnd`, `innerRadius` |
| **Medium** | Shape: Cylinder | `arc` (partial), `topRadius` (taper) |
| **Low** | Transform | `orbit` system |
| **Low** | Fill | `dashPattern` for wireframe |
| **Low** | Shape: Prism/Cylinder | `twist`, `heightSegments`, `capTop/capBottom` |

### 🔮 FUTURE (Phase 4+)

| Category | Items |
|----------|-------|
| **New Shapes** | Torus, Cone, Helix |
| **Fill** | `pointShape` (CIRCLE, STAR variants) |
| **Advanced** | Fresnel, Metallicness, Roughness |

---

## 18. Minecraft Native Utilities Used

> Performance optimizations using Minecraft's built-in utilities

| Utility | Usage | Performance Benefit |
|---------|-------|---------------------|
| `MathHelper.sin()` | Waveform, Spin, Wobble, Wave | Fast lookup table vs Math.sin() |
| `MathHelper.cos()` | Sphere cage rendering | Fast lookup table |
| `MathHelper.lerp()` | Alpha interpolation | Optimized linear interp |
| `MathHelper.floor()` | Waveform normalization | Integer conversion |
| `ColorHelper.lerp()` | ColorCycle blending | Per-channel color interp |

---

*Complete parameter matrix - updated December 9, 2024*

