# Complete Parameter Inventory

> **Purpose:** Every configurable parameter at every level  
> **Status:** ✅ Updated - verified against code (Dec 9, 2024)  
> **Created:** December 7, 2024

---

## Legend

- ✅ = Currently implemented
- ⚠️ = Partially implemented
- ❌ = Missing / needs implementation
- ⬜ = New (not yet started)
- 🔮 = Future consideration
- 📌 = **Phase 1 Priority**
- 📎 = Phase 2+

---

## 1. FIELD DEFINITION Level

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `id` | Identifier | required | ✅ | Unique field ID |
| `type` | FieldType | required | ✅ | SHIELD, PERSONAL, FORCE, AURA, PORTAL |
| `baseRadius` | float | 1.0 | ✅ | Base scale multiplier |
| `themeId` | string | null | ✅ | Color theme reference |
| `layers` | List<Layer> | [] | ✅ | Visual layers |

### Modifiers Block

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `visualScale` | float | 1.0 | ✅ | Overall visual scale |
| `tilt` | float | 0.0 | ✅ | Global tilt angle |
| `swirl` | float | 0.0 | ✅ | Swirl effect strength |
| `pulsing` | float | 0.0 | ⚠️ | Global pulse (verify used) |
| `bobbing` | float | 0.0 | ✅ | Vertical bob animation |
| `breathing` | float | 0.0 | ✅ | Scale breathing effect |

### Prediction Block (Personal Fields)

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `enabled` | boolean | false | ✅ | Enable prediction |
| `leadTicks` | int | 2 | ✅ | Ticks to predict ahead |
| `maxDistance` | float | 8.0 | ✅ | Max prediction distance |
| `lookAhead` | float | 0.5 | ✅ | Look direction weight |
| `verticalBoost` | float | 0.0 | ✅ | Vertical prediction boost |

### Beam Block

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `enabled` | boolean | false | ✅ | Show central beam |
| `innerRadius` | float | 0.05 | ✅ | Inner beam radius |
| `outerRadius` | float | 0.1 | ✅ | Outer beam radius |
| `color` | string | "@beam" | ✅ | Beam color |
| `height` | float | 3.0 | ✅ | Beam height |
| `glow` | float | 0.5 | ✅ | Beam glow intensity |
| `pulse` | object | null | ✅ | Beam pulse config |

### Follow Mode (Personal Fields)

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `followMode.enabled` | boolean | true | ✅ | false = static field |
| `followMode.mode` | enum | SMOOTH | ✅ | SNAP, SMOOTH, GLIDE |
| `followMode.playerOverride` | boolean | true | ✅ | Player can change in GUI |

---

## 2. LAYER Level

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `id` | string | required | ✅ | Layer identifier |
| `primitives` | List<Primitive> | [] | ✅ | Layer primitives |
| `colorRef` | string | "@primary" | ✅ | Color override for layer |
| `alpha` | float | 1.0 | ✅ | Alpha override |
| `spin` | SpinConfig | null | ✅ | Layer spin: { axis, speed } |
| `tilt` | float | 0.0 | ✅ | Layer tilt angle |
| `pulse` | float | 0.0 | ✅ | Layer pulse |
| `phaseOffset` | float | 0.0 | ✅ | Animation phase offset |
| `rotation` | Vec3 | (0,0,0) | ✅ | Static rotation (via Transform) |
| `visible` | boolean | true | ✅ | Layer visibility toggle |
| `blendMode` | enum | NORMAL | ✅ | NORMAL, ADD (Phase 1); MULTIPLY, SCREEN (Phase 2, custom shaders) |
| `order` | int | auto | ✅ | Render order |

---

## 3. PRIMITIVE Level

### 3.1 Common Primitive Fields

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `id` | string | required | ✅ | **REQUIRED** for linking/debugging |
| `type` | string | required | ✅ | Shape type |
| `shape` | object | {} | ✅ | Shape-specific params |
| `transform` | object | {} | ⚠️ | Position/rotation/scale |
| `fill` | object | {} | ⚠️ | Fill mode config |
| `visibility` | object | {} | ⚠️ | Visibility mask config |
| `arrangement` | string | "default" | ⚠️ | Vertex arrangement |
| `appearance` | object | {} | ✅ | Visual properties |
| `animation` | object | {} | ✅ | Animation config |

---

## 4. SHAPE Level (Per Shape Type)

### 4.1 Sphere Shape

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| `radius` | float | 1.0 | 0.01-∞ | ✅ | Sphere radius |
| `latSteps` | int | 32 | 2-512 | ✅ | Latitude divisions |
| `lonSteps` | int | 64 | 4-1024 | ✅ | Longitude divisions |
| `latStart` | float | 0.0 | 0-1 | ✅ | Start latitude (0=top) |
| `latEnd` | float | 1.0 | 0-1 | ✅ | End latitude (1=bottom) |
| `algorithm` | enum | LAT_LON | - | ✅ | LAT_LON, TYPE_A, TYPE_E |
| `lonStart` | float | 0.0 | 0-1 | ✅ | Start longitude (partial sphere) |
| `lonEnd` | float | 1.0 | 0-1 | ✅ | End longitude |
| `subdivisions` | int | 0 | 0-5 | ✅ | Icosphere subdivisions (for TYPE_E) |
| `uvScale` | Vec2 | (1,1) | - | 🔮 | UV texture scaling |

### 4.2 Ring Shape

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| `innerRadius` | float | 0.8 | 0-∞ | ✅ | Inner ring radius |
| `outerRadius` | float | 1.0 | 0-∞ | ✅ | Outer ring radius |
| `segments` | int | 64 | 3-1024 | ✅ | Segment count |
| `y` | float | 0.0 | -∞-∞ | ✅ | Y position |
| `arcStart` | float | 0.0 | 0-360 | ✅ | Arc start angle (degrees) |
| `arcEnd` | float | 360.0 | 0-360 | ✅ | Arc end angle |
| `height` | float | 0.0 | 0-∞ | ✅ | Ring height (3D ring) |
| `twist` | float | 0.0 | -∞-∞ | ✅ | Twist along arc |

### 4.3 Disc Shape

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| `radius` | float | 1.0 | 0.01-∞ | ✅ | Disc radius |
| `segments` | int | 64 | 3-1024 | ✅ | Segment count |
| `y` | float | 0.0 | -∞-∞ | ✅ | Y position |
| `arcStart` | float | 0.0 | 0-360 | ✅ | Arc start (pac-man) |
| `arcEnd` | float | 360.0 | 0-360 | ✅ | Arc end |
| `innerRadius` | float | 0.0 | 0-∞ | ✅ | Inner cutout (makes ring-like) |
| `rings` | int | 1 | 1-100 | ✅ | Concentric ring divisions |

### 4.4 Prism Shape

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| `sides` | int | 6 | 3-64 | ✅ | Number of sides |
| `radius` | float | 1.0 | 0.01-∞ | ✅ | Prism radius |
| `height` | float | 1.0 | 0.01-∞ | ✅ | Prism height |
| `topRadius` | float | same | 0-∞ | ✅ | Top radius (for tapered) |
| `twist` | float | 0.0 | -360-360 | ✅ | Twist along height |
| `heightSegments` | int | 1 | 1-100 | ✅ | Vertical divisions |
| `capTop` | boolean | true | - | ✅ | Render top cap |
| `capBottom` | boolean | true | - | ✅ | Render bottom cap |

### 4.5 Polyhedron Shape

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| `polyType` | enum | CUBE | - | ✅ | CUBE, OCTAHEDRON, ICOSAHEDRON, DODECAHEDRON, TETRAHEDRON |
| `radius` | float | 1.0 | 0.01-∞ | ✅ | Circumscribed radius |
| `subdivisions` | int | 0 | 0-5 | ✅ | Subdivision level |
| `dualMode` | boolean | false | - | 🔮 | Show dual polyhedron |

### 4.6 Cylinder Shape (replaces Beam)

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| `radius` | float | 0.5 | 0.01-∞ | ✅ | Cylinder radius |
| `height` | float | 10.0 | 0.01-∞ | ✅ | Cylinder height |
| `segments` | int | 16 | 3-128 | ✅ | Radial segments |
| `topRadius` | float | same | 0-∞ | ✅ | Top radius (cone-like) |
| `heightSegments` | int | 1 | 1-100 | ✅ | Height divisions |
| `capTop` | boolean | true | - | ✅ | Render top cap |
| `capBottom` | boolean | false | - | ✅ | Render bottom cap |
| `openEnded` | boolean | true | - | ✅ | No caps (tube) |
| `arc` | float | 360 | 0-360 | ✅ | Partial cylinder |

### 4.7 Torus Shape (FUTURE)

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| `majorRadius` | float | 1.0 | 0.01-∞ | ❌ | Ring radius |
| `minorRadius` | float | 0.3 | 0.01-∞ | ❌ | Tube radius |
| `majorSegments` | int | 32 | 3-256 | ❌ | Segments around ring |
| `minorSegments` | int | 16 | 3-64 | ❌ | Segments around tube |
| `arc` | float | 360 | 0-360 | ❌ | Partial torus |
| `twist` | int | 0 | -10-10 | 🔮 | Möbius-like twist |

### 4.8 Cone Shape (FUTURE)

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| `radiusBottom` | float | 1.0 | 0-∞ | ❌ | Bottom radius |
| `radiusTop` | float | 0.0 | 0-∞ | ❌ | Top radius (0=point) |
| `height` | float | 1.0 | 0.01-∞ | ❌ | Cone height |
| `segments` | int | 32 | 3-128 | ❌ | Radial segments |
| `heightSegments` | int | 1 | 1-100 | ✅ | Height divisions |
| `capBottom` | boolean | true | - | ✅ | Render bottom cap |
| `arc` | float | 360 | 0-360 | ❌ | Partial cone |

### 4.9 Helix Shape (FUTURE)

| Parameter | Type | Default | Range | Status | Notes |
|-----------|------|---------|-------|--------|-------|
| `radius` | float | 1.0 | 0.01-∞ | ❌ | Helix radius |
| `height` | float | 3.0 | 0.01-∞ | ❌ | Total height |
| `turns` | float | 3.0 | 0.1-20 | ❌ | Number of turns |
| `tubeRadius` | float | 0.1 | 0.01-∞ | ❌ | Tube thickness |
| `segments` | int | 64 | 8-256 | ❌ | Segments per turn |
| `tubeSegments` | int | 8 | 3-32 | ❌ | Tube cross-section |
| `direction` | enum | CCW | - | ❌ | CW, CCW |

---

## 5. TRANSFORM Level

### 5.1 Position

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `anchor` | enum | CENTER | ✅
| `offset` | Vec3 | (0,0,0) | ✅

### 5.2 Rotation

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `rotation` | Vec3 | (0,0,0) | ✅
| `inheritRotation` | boolean | true | ✅

### 5.3 Scale

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `scale` | float | 1.0 | ✅
| `scaleXYZ` | Vec3 | (1,1,1) | ✅
| `scaleWithRadius` | boolean | false | ✅

### 5.4 Orientation

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `facing` | enum | FIXED | ✅
| `up` | enum | WORLD_UP | ✅
| `billboard` | enum | NONE | ✅

### 5.5 Dynamic Positioning

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `orbit.enabled` | boolean | false | ✅
| `orbit.radius` | float | 2.0 | ✅
| `orbit.speed` | float | 1.0 | ✅
| `orbit.axis` | enum | Y | ✅
| `orbit.phase` | float | 0.0 | ✅

---

## 6. FILL Level

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `mode` | enum | SOLID | ✅ | SOLID, WIREFRAME, CAGE, POINTS |
| `wireThickness` | float | 1.0 | ✅ | Line thickness |
| `doubleSided` | boolean | false | ✅
| `depthTest` | boolean | true | ✅
| `depthWrite` | boolean | true | ✅

### Cage-Specific

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `latitudeCount` | int | 8 | ✅
| `longitudeCount` | int | 16 | ✅
| `showEquator` | boolean | true | ✅
| `showPoles` | boolean | true | ✅

### Points-Specific (FUTURE)

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `pointSize` | float | 2.0 | ✅ | Point size |
| `pointShape` | enum | CIRCLE | 🔮 | CIRCLE, SQUARE, STAR |

---

## 7. VISIBILITY MASK Level

### Phase 1 (Minimal)

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `mask` | enum | FULL | ✅ | FULL, BANDS, STRIPES, CHECKER, RADIAL, GRADIENT |
| `count` | int | 4 | ✅ | Division count |
| `thickness` | float | 0.5 | ✅ | Band/stripe thickness (0-1) |

### Phase 2 (Extended)

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `offset` | float | 0.0 | ✅ | Pattern offset/phase |
| `invert` | boolean | false | ✅ | Invert visibility |
| `feather` | float | 0.0 | ✅ | Edge softness |
| `animate` | boolean | false | ✅ | Animate pattern |
| `animateSpeed` | float | 1.0 | ✅ | Animation speed |

### Gradient-Specific (Phase 2)

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `direction` | enum | VERTICAL | ✅ | VERTICAL, HORIZONTAL, RADIAL |
| `falloff` | enum | LINEAR | ✅ | LINEAR, EASE, SMOOTH |
| `start` | float | 0.0 | ✅ | Gradient start (0-1) |
| `end` | float | 1.0 | ✅ | Gradient end (0-1) |

### Radial-Specific (Phase 2)

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `centerX` | float | 0.5 | ✅ | Center X (0-1) |
| `centerY` | float | 0.5 | ✅ | Center Y (0-1) |
| `falloff` | enum | LINEAR | ✅ | LINEAR, EASE, SMOOTH |

---

## 8. ARRANGEMENT Level

### 8.1 Simple Form
| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `arrangement` | string | "default" | ✅ | Pattern name (applies to all parts) |

### 8.2 Multi-Part Form
| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `arrangement.default` | string | "filled_1" | ✅ | Default pattern for all parts |
| `arrangement.caps` | string | null | ✅ | Pattern for cap surfaces |
| `arrangement.sides` | string | null | ✅ | Pattern for side surfaces |
| `arrangement.edges` | string | null | ✅ | Pattern for edge lines |
| `arrangement.poles` | string | null | ✅ | Pattern for sphere poles |
| `arrangement.equator` | string | null | ✅ | Pattern for sphere equator |

### 8.3 Shuffle (Debug)
| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `shuffle` | boolean | false | ✅ | Enable shuffle exploration |
| `shuffleIndex` | int | 0 | ✅ | Current shuffle index |

### 8.4 Available Patterns Per Cell Type

| Cell Type | Available Patterns | Status |
|-----------|-------------------|--------|
| QUAD | filled_1, triangle_1-4, wave_1, tooth_1, parallelogram_1-2, stripe_1, etc. | ✅ |
| SEGMENT | full, alternating, sparse, quarter, reversed, zigzag, dashed | ⚠️ |
| SECTOR | full, half, quarters, pinwheel, trisector, spiral, crosshair | ⚠️ |
| EDGE | full, latitude, longitude, sparse, minimal, dashed, grid | ⚠️ |
| TRIANGLE | full, alternating, inverted, sparse, fan, radial | ⚠️ |

### 8.5 Shape Parts Reference

| Shape | Parts Available (camelCase) |
|-------|-----------------|
| Sphere | main, poles, equator, hemisphereTop, hemisphereBottom |
| Ring | surface, innerEdge, outerEdge |
| Disc | surface, edge |
| Prism/Cylinder | sides, capTop, capBottom, edges |
| Polyhedron | faces, edges, vertices |
| Torus (future) | outer, inner |

---

## 9. APPEARANCE Level

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `color` | string | "@primary" | ✅ | Color reference |
| `alpha` | float/range | 1.0 | ✅ | { min, max } for pulsing |
| `glow` | float | 0.0 | ✅ | Glow intensity (0-1) |
| `emissive` | float | 0.0 | ✅
| `saturation` | float | 1.0 | ✅
| `brightness` | float | 1.0 | ✅
| `hueShift` | float | 0.0 | ✅
| `secondaryColor` | string | null | ✅
| `colorBlend` | float | 0.0 | ✅

---

## 10. ANIMATION Level

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `spin` | object | null | ✅ | Rotation animation |
| `pulse` | object | null | ⚠️ | Scale pulsing |
| `phase` | float | 0.0 | ✅ | Animation phase offset |
| `alphaPulse` | object | null | ⚠️ | Alpha pulsing |
| `colorCycle` | object | null | ✅ | Color animation |
| `wobble` | object | null | ✅ | Random movement |
| `wave` | object | null | ✅ | Wave deformation |

### Spin Config

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `axis` | enum/Vec3 | Y | ✅ | X, Y, Z or custom axis |
| `speed` | float | 0.0 | ✅ | Rotation speed |
| `oscillate` | boolean | false | ✅
| `range` | float | 360 | ✅

### Pulse Config

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `scale` | float | 1.0 | ✅
| `speed` | float | 1.0 | ⚠️ | Pulse speed |
| `waveform` | enum | SINE | ✅ | SINE, SQUARE, TRIANGLE_WAVE, SAWTOOTH |
| `min` | float | 0.9 | ✅ | Minimum scale |
| `max` | float | 1.1 | ✅ | Maximum scale |

### AlphaPulse Config

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `speed` | float | 1.0 | ⚠️ | Pulse speed |
| `min` | float | 0.3 | ⚠️ | Minimum alpha |
| `max` | float | 1.0 | ⚠️ | Maximum alpha |
| `waveform` | enum | SINE | ✅ | SINE, SQUARE, TRIANGLE_WAVE, SAWTOOTH |

### Color Cycle Config

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `colors` | List<string> | [] | ✅ | Colors to cycle through |
| `speed` | float | 1.0 | ✅ | Cycle speed |
| `blend` | boolean | true | ✅ | Smooth blend vs instant |

### Wobble Config

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `amplitude` | Vec3 | (0.1,0.1,0.1) | ✅ | Wobble amount per axis |
| `speed` | float | 1.0 | ✅ | Wobble speed |
| `randomize` | boolean | true | ✅ | Randomize movement |

---

## 11. PRIMITIVE LINKING Level

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `id` | string | required | ✅ | Primitive identifier for linking |
| `link.radiusMatch` | string | null | ✅
| `link.radiusOffset` | float | 0.0 | ✅
| `link.follow` | string | null | ✅
| `link.mirror` | enum | null | ✅
| `link.phaseOffset` | float | 0.0 | ✅
| `link.scaleWith` | string | null | ✅

**Example:**
```json
"primitives": [
  { "id": "main", "type": "sphere", "shape": { "radius": 1.0 } },
  { 
    "id": "ring", 
    "type": "ring",
    "link": { "radiusMatch": "main", "radiusOffset": 0.2 }
  }
]
```

---

## 12. EXTERNAL INFLUENCES Level

### 12.1 Bindings

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `bindings` | Map<String, BindingConfig> | {} | ✅

#### BindingConfig

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `source` | string | required | ✅
| `inputRange` | float[2] | [0, 1] | ✅
| `outputRange` | float[2] | [0, 1] | ✅
| `curve` | enum | LINEAR | ✅ | LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT |

#### Available Binding Sources

| Source ID | Type | Range | Status |
|-----------|------|-------|--------|
| `player.health` | float | 0-20 | ⬜ |
| `player.health_percent` | float | 0-1 | ⬜ |
| `player.armor` | int | 0-20 | ⬜ |
| `player.food` | int | 0-20 | ⬜ |
| `player.speed` | float | 0-∞ | ⬜ |
| `player.is_sprinting` | bool | 0/1 | ⬜ |
| `player.is_sneaking` | bool | 0/1 | ⬜ |
| `player.is_flying` | bool | 0/1 | ⬜ |
| `player.is_invisible` | bool | 0/1 | ⬜ |
| `player.in_combat` | bool | 0/1 | ⬜ |
| `player.damage_taken` | float | 0-∞ (decays) | ⬜ |
| `field.age` | int | 0-∞ | ⬜ |

---

### 12.2 Triggers

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `triggers` | List<TriggerConfig> | [] | ✅

#### TriggerConfig

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `event` | enum | required | ✅
| `effect` | enum | required | ✅
| `duration` | int | 10 | ✅
| `color` | string | null | ✅
| `scale` | float | 1.2 | ✅
| `amplitude` | float | 0.1 | ✅
| `intensity` | float | 0.5 | ✅

#### FieldEvent Enum

| Value | Description |
|-------|-------------|
| `PLAYER_DAMAGE` | Player takes damage |
| `PLAYER_HEAL` | Player heals |
| `PLAYER_DEATH` | Player dies |
| `PLAYER_RESPAWN` | Player respawns |
| `FIELD_SPAWN` | Field spawns |
| `FIELD_DESPAWN` | Field despawns |

#### TriggerEffect Enum

| Value | Params | Description |
|-------|--------|-------------|
| `FLASH` | color, duration | Brief color overlay |
| `PULSE` | scale, duration | Scale up then back |
| `SHAKE` | amplitude, duration | Position jitter |
| `GLOW` | intensity, duration | Glow boost |
| `COLOR_SHIFT` | color, duration | Temporary color |

---

### 12.3 Lifecycle

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `lifecycle` | LifecycleConfig | null | ✅

#### LifecycleConfig

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `fadeIn` | int | 0 | ✅
| `fadeOut` | int | 0 | ✅
| `scaleIn` | int | 0 | ✅
| `scaleOut` | int | 0 | ✅
| `decay` | DecayConfig | null | ✅

#### DecayConfig

| Parameter | Type | Default | Status | Notes |
|-----------|------|---------|--------|-------|
| `rate` | float | 0.01 | ✅
| `min` | float | 0.0 | ⬜ | Minimum alpha |

---

### 12.4 Combat Tracking

| Parameter | Type | Default | Notes |
|-----------|------|---------|-------|
| `combatTimeout` | int | 100 | Ticks until "out of combat" |
| `damageDecayFactor` | float | 0.95 | Per-tick decay multiplier |

---

## 13. Summary: Missing Parameters Count

| Level | Implemented | Missing | Future |
|-------|-------------|---------|--------|
| Field Definition | 21 | 0 | 0 |
| Layer | 12 | 0 | 0 |
| Transform | 18 | 0 | 0 |
| Fill | 10 | 0 | 1 |
| Visibility | 14 | 0 | 0 |
| Arrangement | 10 | 0 | 0 |
| Appearance | 9 | 0 | 0 |
| Animation | 19 | 0 | 0 |
| Primitive Linking | 7 | 0 | 0 |
| **Shapes** | | | |
| - Sphere | 9 | 0 | 1 |
| - Ring | 8 | 0 | 0 |
| - Disc | 7 | 0 | 0 |
| - Prism | 8 | 0 | 0 |
| - Polyhedron | 3 | 0 | 1 |
| - Cylinder | 9 | 0 | 0 |
| - Torus | 0 | 6 | 1 |
| - Cone | 0 | 7 | 0 |
| - Helix | 0 | 8 | 0 |
| **TOTAL** | ~164 | ~21 | ~4 |

> Note: Torus, Cone, and Helix are planned future shapes (Phase 4).
> Missing count reflects only these future shape parameters.
---

## 14. Priority Implementation Order

### Phase 1: Core Restructure
1. Flatten primitive hierarchy
2. Complete Transform system (all anchors, facing, billboard)
3. Implement FillConfig and VisibilityMask records
4. Multi-part arrangement support
5. FollowMode as config object
6. **External Influences: Bindings, Triggers, Lifecycle, CombatTracker**
7. Primitive Linking (simple offset syntax)

### Phase 2: GUI & Polish
1. Design GUI customization panel
2. Visibility: stripes, radial, gradient masks
3. Complete all pattern variants
4. Player-configurable followMode

### Phase 3: Advanced Features
1. Orbit and dynamic positioning
2. Pattern animation
3. Procedural patterns

### Phase 4: New Shapes
1. Torus
2. Cone  
3. Helix

---

## 15. JSON REFERENCE SYSTEM

### Reference Folders

| Folder | Purpose | Example |
|--------|---------|---------|
| `field_definitions/` | Complete field profiles | `quad_shield_default.json` |
| `field_shapes/` | Reusable shape configs | `smooth_sphere.json` |
| `field_appearances/` | Reusable appearance configs | `glowing_blue.json` |
| `field_transforms/` | Reusable transform configs | `above_head.json` |
| `field_fills/` | Reusable fill configs | `wireframe_thin.json` |
| `field_masks/` | Reusable visibility masks | `horizontal_bands.json` |
| `field_arrangements/` | Reusable arrangements | `wave_pattern.json` |
| `field_animations/` | Reusable animation configs | `slow_spin.json` |
| `field_layers/` | Complete layer templates | `spinning_ring.json` |
| `field_primitives/` | Complete primitive templates | `glowing_sphere.json` |
| `growth_field_profiles/` | Legacy growth profiles | (renamed for clarity) |

### Reference Syntax

| Syntax | Description | Example |
|--------|-------------|---------|
| `"$shapes/name"` | Load from field_shapes/ | `"shape": "$shapes/smooth_sphere"` |
| `"$fills/name"` | Load from field_fills/ | `"fill": "$fills/wireframe_thin"` |
| `"$masks/name"` | Load from field_masks/ | `"visibility": "$masks/horizontal_bands"` |
| `"$appearances/name"` | Load from field_appearances/ | `"appearance": "$appearances/glowing_blue"` |
| `"$animations/name"` | Load from field_animations/ | `"animation": "$animations/slow_spin"` |
| `"$transforms/name"` | Load from field_transforms/ | `"transform": "$transforms/above_head"` |
| `"$layers/name"` | Load from field_layers/ | `"layers": ["$layers/spinning_ring"]` |
| `"$primitives/name"` | Load from field_primitives/ | `"primitives": ["$primitives/glowing_sphere"]` |

### Reference with Override

```json
{
  "shape": {
    "$ref": "$shapes/smooth_sphere",
    "radius": 2.0
  }
}
```

This loads `smooth_sphere.json` and overrides its `radius` to `2.0`.

---

## 16. SMART DEFAULTS

### Per-Type Shape Defaults

| Shape | Default Parameters |
|-------|-------------------|
| sphere | `{ radius: 1.0, latSteps: 32, lonSteps: 64, algorithm: "LAT_LON" }` |
| ring | `{ innerRadius: 0.8, outerRadius: 1.0, segments: 64, y: 0 }` |
| disc | `{ radius: 1.0, segments: 64, y: 0 }` |
| prism | `{ sides: 6, radius: 1.0, height: 1.0 }` |
| polyhedron | `{ polyType: "CUBE", radius: 1.0 }` |
| cylinder | `{ radius: 0.5, height: 10.0, segments: 16 }` |

### General Defaults

| Level | Defaults |
|-------|----------|
| Transform | `{ anchor: "center", offset: [0,0,0], rotation: [0,0,0], scale: 1.0, facing: "fixed", billboard: "none" }` |
| Fill | `{ mode: "solid", wireThickness: 1.0, doubleSided: false }` |
| Visibility | `{ mask: "full", count: 4, thickness: 0.5, invert: false }` |
| Arrangement | `"filled_1"` (varies by CellType) |
| Appearance | `{ color: "@primary", alpha: 1.0, glow: 0.0 }` |
| Animation | `{ spin: null, pulse: null, phase: 0.0 }` |

### Shorthand Forms

| Full Form | Shorthand |
|-----------|-----------|
| `"alpha": { "min": 0.5, "max": 0.5 }` | `"alpha": 0.5` |
| `"spin": { "axis": "Y", "speed": 0.02 }` | `"spin": 0.02` |
| `"arrangement": { "default": "wave_1" }` | `"arrangement": "wave_1"` |
| `"visibility": { "mask": "bands" }` | `"visibility": "bands"` |
| `"fill": { "mode": "wireframe" }` | `"fill": "wireframe"` |

---

## 17. Example Reference Files

### field_shapes/smooth_sphere.json
```json
{
  "radius": 1.0,
  "latSteps": 48,
  "lonSteps": 96,
  "algorithm": "LAT_LON"
}
```

### field_fills/wireframe_thin.json
```json
{
  "mode": "wireframe",
  "wireThickness": 0.5,
  "doubleSided": false
}
```

### field_masks/horizontal_bands.json
```json
{
  "mask": "bands",
  "count": 8,
  "thickness": 0.5,
  "offset": 0.0,
  "animate": false
}
```

### field_appearances/glowing_blue.json
```json
{
  "color": "#4488FF",
  "alpha": 0.8,
  "glow": 0.6,
  "emissive": 0.3
}
```

### field_animations/slow_spin.json
```json
{
  "spin": {
    "axis": "Y",
    "speed": 0.01
  }
}
```

---

*Parameter inventory v5.1 - Final review fixes applied.*

