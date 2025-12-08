# Complete Shape & Type Parameter Matrix

> **Purpose:** Every possible customization parameter for every shape and type  
> **Status:** Comprehensive inventory  
> **Date:** December 7, 2024

---

## Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Implemented in code |
| 📋 | Documented but not implemented |
| ❓ | Potential addition (not documented) |
| 🔮 | Future phase |

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
| **Proposed** |
| `doubleSided` | boolean | false | 📋 | Render both sides |
| `depthTest` | boolean | true | 📋 | Depth testing |
| `depthWrite` | boolean | true | 📋 | Write to depth |
| **Cage-Specific** |
| `cage.latitudeCount` | int | 8 | 📋 | Latitude lines |
| `cage.longitudeCount` | int | 16 | 📋 | Longitude lines |
| `cage.showEquator` | boolean | true | 📋 | Highlight equator |
| `cage.showPoles` | boolean | true | 📋 | Highlight poles |
| **Wireframe-Specific (Potential)** |
| `dashPattern` | array | - | ❓ | [on, off] lengths |
| `dashOffset` | float | 0 | ❓ | Dash start offset |
| **Points-Specific** |
| `pointSize` | float | 2.0 | 🔮 | Point size |
| `pointShape` | enum | CIRCLE | 🔮 | CIRCLE, SQUARE, STAR |

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
| `spinAxis` | enum | Y | ✅ | X, Y, Z |
| `pulse` | float | 0 | ✅ | Scale pulse speed |
| `pulseAmount` | float | 0 | ✅ | Scale pulse amplitude |
| `phase` | float | 0 | ✅ | Animation phase offset |
| `alphaPulse` | float | 0 | ✅ | Alpha pulse speed |
| `alphaPulseAmount` | float | 0 | ✅ | Alpha pulse amplitude |
| **Proposed SpinConfig** |
| `spin.oscillate` | boolean | false | 📋 | Back-and-forth |
| `spin.range` | float | 360 | 📋 | Oscillation range |
| **Proposed PulseConfig** |
| `pulse.waveform` | enum | SINE | 📋 | SINE, SQUARE, TRIANGLE_WAVE, SAWTOOTH |
| `pulse.min` | float | 0.9 | 📋 | Minimum scale |
| `pulse.max` | float | 1.1 | 📋 | Maximum scale |
| **Future** |
| `colorCycle` | object | null | 🔮 | Color animation |
| `wobble` | object | null | 🔮 | Random movement |
| `wave` | object | null | 🔮 | Wave deformation |

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

## 17. Summary: Implementation Priority

### Phase 1 (Core)
- All ✅ parameters (already implemented)
- All 📋 Appearance parameters (per user Q3)
- Transform: `anchor`, `facing`, `billboard`
- Fill: cage-specific options
- Visibility: `offset`, `invert`, `animate`
- TrianglePattern

### Phase 2 (Polish)
- GUI development
- Transform: orbit system
- Animation: waveform, oscillate
- Layer: visible, blendMode, order

### Phase 3 (Advanced)
- Primitive linking
- Wire dash patterns
- Band direction control

### Phase 4 (New Shapes)
- Torus, Cone, Helix
- All 🔮 parameters

---

*Complete parameter matrix - shows exactly what's implemented vs planned.*

