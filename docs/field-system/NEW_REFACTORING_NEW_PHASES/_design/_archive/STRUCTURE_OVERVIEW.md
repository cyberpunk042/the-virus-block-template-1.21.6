# Field System Structure Overview

> **Focus:** High-level architecture, relationships, and expansion potential  
> **Created:** December 7, 2024

---

## 1. Core Hierarchy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           FIELD DEFINITION                                  │
│                                                                             │
│   FieldDefinition                                                           │
│   ├── id, type, baseRadius, themeId                                        │
│   ├── modifiers (spin, tilt, swirl, pulse)                                 │
│   ├── prediction (for personal fields)                                     │
│   └── layers: List<FieldLayer>  ←──────────────────────────────────────┐   │
│                                                                         │   │
│   FieldLayer                                                            │   │
│   ├── id, colorRef, alpha                                               │   │
│   ├── spin, tilt, pulse, phaseOffset  (layer-level animation)          │   │
│   └── primitives: List<Primitive>  ←───────────────────────────────┐   │   │
│                                                                     │   │   │
│   Primitive (sealed interface)                                      │   │   │
│   ├── shape()      → Shape                                          │   │   │
│   ├── transform()  → Transform                                      │   │   │
│   ├── appearance() → Appearance                                     │   │   │
│   └── animation()  → Animation                                      │   │   │
│                                                                     │   │   │
└─────────────────────────────────────────────────────────────────────┼───┼───┘
                                                                      │   │
                    ┌─────────────────────────────────────────────────┘   │
                    │                                                     │
                    ▼                                                     │
┌─────────────────────────────────────────────────────────────────────────┼───┐
│                           PRIMITIVE TYPES                               │   │
│                                                                         │   │
│   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────────┐  │   │
│   │ SolidPrimitive  │   │  BandPrimitive  │   │ StructuralPrimitive │  │   │
│   │   (abstract)    │   │   (abstract)    │   │     (abstract)      │  │   │
│   └────────┬────────┘   └────────┬────────┘   └──────────┬──────────┘  │   │
│            │                     │                       │              │   │
│   ┌────────┼────────┐    ┌───────┼───────┐      ┌────────┼────────┐    │   │
│   │        │        │    │       │       │      │        │        │    │   │
│   ▼        ▼        ▼    ▼       ▼       ▼      ▼        ▼        ▼    │   │
│ Sphere  Prism   Stripes Ring  Rings    Disc   Cage    Beam    ???     │   │
│ Polyhe  Disc?                                                          │   │
│                                                                         │   │
│   Note: Disc could arguably be BandPrimitive (it's ring-like)          │   │
│                                                                         │   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Shape → Primitive Mapping

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              SHAPES                                         │
│                        (Geometry definitions)                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   SphereShape ────────────► SpherePrimitive                                │
│   │                         StripesPrimitive                                │
│   └─ radius, latSteps, lonSteps, algorithm                                 │
│                                                                             │
│   RingShape ──────────────► RingPrimitive                                  │
│   │                         RingsPrimitive (multiple rings)                │
│   └─ innerRadius, outerRadius, segments, y                                 │
│                                                                             │
│   DiscShape ──────────────► DiscPrimitive                                  │
│   └─ radius, segments, y                                                   │
│                                                                             │
│   PrismShape ─────────────► PrismPrimitive                                 │
│   └─ sides, height, radius                                                 │
│                                                                             │
│   PolyhedronShape ────────► PolyhedronPrimitive                            │
│   └─ polyType (CUBE, OCTAHEDRON, ICOSAHEDRON, DODECAHEDRON, TETRAHEDRON)   │
│                                                                             │
│   BeamShape ──────────────► BeamPrimitive                                  │
│   └─ radius, height                                                        │
│                                                                             │
│   CageShape ???            CagePrimitive (uses SphereShape internally)     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Pattern System (VertexPattern)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PATTERN SYSTEM                                    │
│                    (How geometry is tessellated)                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   VertexPattern (interface)                                                 │
│   └── PatternGeometry: QUAD | SEGMENT | SECTOR | EDGE                      │
│                                                                             │
│   ┌───────────────────────────────────────────────────────────────────────┐ │
│   │ GEOMETRY       PATTERN TYPE          USED BY                          │ │
│   ├───────────────────────────────────────────────────────────────────────┤ │
│   │ QUAD           QuadPattern           Sphere, Prism, Polyhedron        │ │
│   │                └─ filled_1, triangle_1, wave_1, tooth_1...            │ │
│   │                                                                        │ │
│   │ SEGMENT        SegmentPattern        Ring, Rings                       │ │
│   │                └─ full, alternating, sparse, quarter...               │ │
│   │                                                                        │ │
│   │ SECTOR         SectorPattern         Disc                              │ │
│   │                └─ full, half, quarters, pinwheel...                   │ │
│   │                                                                        │ │
│   │ EDGE           EdgePattern           Cage, Beam (wireframe)            │ │
│   │                └─ full, latitude, longitude, sparse...                │ │
│   └───────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│   Dynamic variants: DynamicQuadPattern, DynamicSegmentPattern, etc.        │
│   ShuffleGenerator: explores all permutations                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Rendering Pipeline

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         RENDERING (Client-side)                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   FieldDefinition                                                           │
│        │                                                                    │
│        ▼                                                                    │
│   FieldRenderer                                                             │
│        │                                                                    │
│        ├──► LayerRenderer (per FieldLayer)                                 │
│        │         │                                                          │
│        │         ├──► PrimitiveRenderer (per Primitive)                    │
│        │         │         │                                                │
│        │         │         ├──► Tessellator (Shape → Mesh)                 │
│        │         │         │         │                                      │
│        │         │         │         └──► SphereTessellator                │
│        │         │         │             RingTessellator                   │
│        │         │         │             DiscTessellator                   │
│        │         │         │             PrismTessellator                  │
│        │         │         │             PolyhedraTessellator              │
│        │         │         │                                                │
│        │         │         └──► VertexEmitter (Mesh → GPU)                 │
│        │         │                                                          │
│        │         └──► Apply layer transform, color, animation              │
│        │                                                                    │
│        └──► Apply field-level modifiers                                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Current Primitive Inventory

| Primitive | Shape | Abstract Base | Pattern | Status |
|-----------|-------|---------------|---------|--------|
| SpherePrimitive | SphereShape | SolidPrimitive | QuadPattern | ✅ Works |
| StripesPrimitive | SphereShape | SolidPrimitive | QuadPattern | ⚠️ Test |
| PrismPrimitive | PrismShape | SolidPrimitive | QuadPattern | ✅ Works |
| PolyhedronPrimitive | PolyhedronShape | SolidPrimitive | QuadPattern | ✅ Works |
| DiscPrimitive | DiscShape | SolidPrimitive | SectorPattern | ✅ Works |
| RingPrimitive | RingShape | BandPrimitive | SegmentPattern | ✅ Works |
| RingsPrimitive | RingShape | BandPrimitive | SegmentPattern | ⚠️ Test |
| CagePrimitive | SphereShape | StructuralPrimitive | EdgePattern | ✅ Works |
| BeamPrimitive | BeamShape | StructuralPrimitive | EdgePattern | ⚠️ Test |

---

## 6. GAP ANALYSIS: Missing Combinations

### 6.1 Potential Missing Primitives

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     POTENTIAL NEW PRIMITIVES                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   FROM EXISTING SHAPES:                                                     │
│   ┌───────────────────────────────────────────────────────────────────────┐ │
│   │ Shape            Missing Primitive?        Notes                      │ │
│   ├───────────────────────────────────────────────────────────────────────┤ │
│   │ PrismShape       PrismCagePrimitive?       Wireframe prism            │ │
│   │ PolyhedronShape  PolyhedronCagePrimitive?  Wireframe polyhedron       │ │
│   │ DiscShape        DiscRingPrimitive?        Just the outer edge        │ │
│   │ SphereShape      HemispherePrimitive?      Half sphere (lat 0-0.5)    │ │
│   └───────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│   NEW SHAPE IDEAS:                                                          │
│   ┌───────────────────────────────────────────────────────────────────────┐ │
│   │ Shape            Primitive                 Use Case                   │ │
│   ├───────────────────────────────────────────────────────────────────────┤ │
│   │ TorusShape       TorusPrimitive            Donut-shaped field         │ │
│   │ ConeShape        ConePrimitive             Directional field          │ │
│   │ CylinderShape    CylinderPrimitive         Pillar/column field        │ │
│   │ SpiralShape      SpiralPrimitive           DNA helix, tornado         │ │
│   │ GridShape        GridPrimitive             Force field grid           │ │
│   └───────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Pattern × Shape Matrix

Which patterns work with which shapes? `✓` = implemented, `?` = possible, `-` = N/A

| Pattern | Sphere | Ring | Disc | Prism | Polyhedron | Cage | Beam |
|---------|--------|------|------|-------|------------|------|------|
| QuadPattern | ✓ | - | - | ✓ | ✓ | - | - |
| SegmentPattern | - | ✓ | - | ? | - | - | - |
| SectorPattern | - | ? | ✓ | - | - | - | - |
| EdgePattern | - | - | - | ? | ? | ✓ | ✓ |

**Gaps:**
- Could Ring use SectorPattern? (arc segments)
- Could Disc use SegmentPattern? (concentric rings)
- Could Prism/Polyhedron use EdgePattern? (wireframe mode)

### 6.3 FillMode × Primitive Matrix

| FillMode | Sphere | Ring | Disc | Prism | Polyhedron | Cage | Beam |
|----------|--------|------|------|-------|------------|------|------|
| SOLID | ✓ | ✓ | ✓ | ✓ | ✓ | - | - |
| WIREFRAME | ✓ | ? | ? | ? | ? | ✓ | ✓ |
| TRANSLUCENT | ✓ | ✓ | ✓ | ✓ | ✓ | - | - |

---

## 7. Field Type → Primitive Suggestions

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     FIELD TYPE COMPOSITIONS                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   SHIELD (defensive bubble)                                                 │
│   ├── Layer 1: Sphere (outer shell, translucent)                           │
│   ├── Layer 2: Sphere (inner core, solid)                                  │
│   ├── Layer 3: Cage (wireframe overlay)                                    │
│   └── Layer 4: Rings (latitude bands)                                      │
│                                                                             │
│   PERSONAL (player-attached)                                                │
│   ├── Layer 1: Sphere (main shield)                                        │
│   └── Layer 2: Ring (ground indicator)                                     │
│                                                                             │
│   SINGULARITY (black hole effect)                                          │
│   ├── Layer 1: Sphere (event horizon)                                      │
│   ├── Layer 2: Disc (accretion disk)                                       │
│   └── Layer 3: Beam (vertical jet)                                         │
│                                                                             │
│   GROWTH (virus expansion)                                                  │
│   ├── Layer 1: Ring (expanding wave)                                       │
│   └── Layer 2: Stripes (scanning effect)                                   │
│                                                                             │
│   FORCE (push/pull)                                                         │
│   ├── Layer 1: Sphere (affected area)                                      │
│   └── Layer 2: Stripes (direction indicator)                               │
│                                                                             │
│   AURA (ambient effect)                                                     │
│   ├── Layer 1: Sphere (glow region)                                        │
│   └── Layer 2: Polyhedron (geometric accent)                               │
│                                                                             │
│   PORTAL ???                                                                │
│   ├── Layer 1: Ring (event horizon)                                        │
│   ├── Layer 2: Disc (surface)                                              │
│   └── Layer 3: Beam (destination indicator)                                │
│                                                                             │
│   BARRIER ???                                                               │
│   ├── Layer 1: Prism (wall shape)                                          │
│   └── Layer 2: Grid??? (force field texture)                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Questions to Explore

| # | Question | Impact |
|---|----------|--------|
| 1 | Should Disc be under BandPrimitive? | Classification |
| 2 | Do we need wireframe versions of Prism/Polyhedron? | New primitives |
| 3 | Is Hemisphere useful enough to add? | New primitive |
| 4 | Should we add Torus/Cone/Cylinder shapes? | Major expansion |
| 5 | Can patterns be mixed? (e.g., quad + edge on same primitive) | Feature |
| 6 | Should we add GridPrimitive for force field walls? | New primitive |
| 7 | Do Stripes/Rings render correctly with patterns? | Bug fixing |

---

## 9. Priority Recommendations

| Priority | Action | Why |
|----------|--------|-----|
| 🔴 HIGH | Test StripesPrimitive | May be broken |
| 🔴 HIGH | Test RingsPrimitive | Untested |
| 🔴 HIGH | Test BeamPrimitive | Untested |
| 🟡 MED | Add wireframe mode to Prism/Polyhedron | Easy win |
| 🟡 MED | Consider Hemisphere | Simple addition |
| 🟢 LOW | New shapes (Torus, Cone, Cylinder) | Big effort |
| 🟢 LOW | GridPrimitive | New concept |

---

*This is a living document. Update as we discover gaps and implement fixes.*

