# Ray Types Implementation Plan

## Overview
Add a RayType selector to allow rays to be rendered as different shapes (lines, droplets, energy beams, etc.) instead of only flat ribbon lines.

## Architecture

### Current Flow
```
RaysShape (config) → RaysTessellator (mesh) → RaysRenderer (GPU)
```

### New Flow
```
RaysShape (config + rayType + type-specific params)
    ↓
RaysTessellator (delegates to specific tessellator)
    ├── RayLineTessellator
    ├── RayDropletTessellator
    ├── RayKamehamehaTessellator
    └── ... more
    ↓
RaysRenderer (unchanged)
```

## Design Decisions
1. **Separate tessellators** per ray type for clean separation
2. **Flat fields** in RaysShape (all type-specific params, unused ones keep defaults)
3. **Animation compatibility** - existing ray animations apply to all types
4. **GUI** - one panel with dynamic fields based on selected type

---

## Phase 1: Foundation

### Step 1.1: Create RayType Enum
**File:** `src/main/java/net/cyberpunk042/visual/shape/RayType.java`

```java
public enum RayType {
    LINE("Line", "Default flat ribbon rays"),
    DROPLET("Droplet", "Teardrop shapes"),
    KAMEHAMEHA("Kamehameha", "Energy beam with bulbous end"),
    ARROW("Arrow", "Line with arrowhead"),
    CONE("Cone", "3D conical rays"),
    CAPSULE("Capsule", "Cylinder with rounded ends"),
    LIGHTNING("Lightning", "Jagged bolt"),
    BEADS("Beads", "Chain of spheres");
    
    private final String displayName;
    private final String description;
    
    // ... standard enum methods
}
```

### Step 1.2: Extend RaysShape
**File:** `src/main/java/net/cyberpunk042/visual/shape/RaysShape.java`

Add new fields:
```java
// Type selector
RayType rayType,  // default: LINE

// Droplet params
float dropletHeadRadius,    // default: 0.2
float dropletTailTaper,     // default: 0.8

// Kamehameha params
float kamehamehaBeamWidth,  // default: 0.3
float kamehamehaEndBulb,    // default: 0.5

// Arrow params
float arrowHeadWidth,       // default: 0.2
float arrowHeadLength,      // default: 0.3

// Beads params
float beadRadius,           // default: 0.1
int beadCount,              // default: 5
float beadSpacing,          // default: 0.2

// Lightning params
float lightningJagged,      // default: 0.3
int lightningSegments,      // default: 8
```

---

## Phase 2: Tessellator Infrastructure

### Step 2.1: Create RayTypeTessellator Interface
**File:** `src/client/java/net/cyberpunk042/client/visual/mesh/ray/RayTypeTessellator.java`

```java
public interface RayTypeTessellator {
    void tessellateRay(
        MeshBuilder builder,
        RaysShape shape,
        int rayIndex,
        Vec3d start,
        Vec3d end,
        float width,
        float t  // normalized position 0-1 along ray
    );
}
```

### Step 2.2: Refactor Current Line Logic
**File:** `src/client/java/net/cyberpunk042/client/visual/mesh/ray/RayLineTessellator.java`

Extract current quad emission logic from RaysTessellator into this class.

### Step 2.3: Update RaysTessellator to Delegate
**File:** `src/client/java/net/cyberpunk042/client/visual/mesh/RaysTessellator.java`

```java
private static final Map<RayType, RayTypeTessellator> TESSELLATORS = Map.of(
    RayType.LINE, new RayLineTessellator(),
    RayType.DROPLET, new RayDropletTessellator(),
    // ... etc
);

// In tessellate():
RayTypeTessellator typeTess = TESSELLATORS.get(shape.rayType());
typeTess.tessellateRay(builder, shape, i, start, end, width, t);
```

---

## Phase 3: Type Implementations

### Step 3.1: RayDropletTessellator
Tessellates teardrop shapes:
- Sphere at head (near center)
- Tapered cone/point at tail

### Step 3.2: RayKamehamehaTessellator
Tessellates energy beam:
- Cylinder beam
- Larger sphere at the end
- Maybe glow vertices

### Step 3.3: RayArrowTessellator
Tessellates arrows:
- Thin shaft quad
- Triangular arrowhead at tip

### Step 3.4: Additional Types (Future)
- RayCapsuleTessellator
- RayLightningTessellator
- RayBeadsTessellator
- RayConeTessellator

---

## Phase 4: GUI Integration

### Step 4.1: Add RayType Dropdown to ShapeSubPanel
In the Rays shape section, add:
```java
content.dropdown("Ray Type", "rays.rayType", RayType.class);
```

### Step 4.2: Conditional Fields Based on Type
```java
content.when(rayType == DROPLET, builder -> {
    builder.slider("Head Radius", "rays.dropletHeadRadius").range(0.05f, 1f).add();
    builder.slider("Tail Taper", "rays.dropletTailTaper").range(0f, 1f).add();
});

content.when(rayType == KAMEHAMEHA, builder -> {
    builder.slider("Beam Width", "rays.kamehamehaBeamWidth").range(0.1f, 1f).add();
    builder.slider("End Bulb", "rays.kamehamehaEndBulb").range(0f, 2f).add();
});
// ... etc
```

---

## Phase 5: Testing & Polish

### Step 5.1: Test Each Ray Type
- Verify tessellation is correct
- Test with animations (flow, wiggle, twist)
- Test with different counts, arrangements

### Step 5.2: Performance Check
- Ensure complex types don't cause FPS drops at high ray counts

### Step 5.3: Presets
- Create preset JSON files for each ray type

---

## File Summary

| File | Action |
|------|--------|
| `visual/shape/RayType.java` | CREATE - Enum |
| `visual/shape/RaysShape.java` | MODIFY - Add fields |
| `client/visual/mesh/ray/RayTypeTessellator.java` | CREATE - Interface |
| `client/visual/mesh/ray/RayLineTessellator.java` | CREATE - Extract from current |
| `client/visual/mesh/ray/RayDropletTessellator.java` | CREATE |
| `client/visual/mesh/ray/RayKamehamehaTessellator.java` | CREATE |
| `client/visual/mesh/ray/RayArrowTessellator.java` | CREATE |
| `client/visual/mesh/RaysTessellator.java` | MODIFY - Delegate to type tessellators |
| `client/gui/panel/sub/ShapeSubPanel.java` | MODIFY - Add type selector + conditional fields |
| `client/gui/state/adapter/ShapeAdapter.java` | MODIFY - Add ray type fields |

---

## Priority Order

1. **Foundation** (Phase 1) - RayType enum, RaysShape fields
2. **Line Refactor** (Phase 2.2) - Extract current logic
3. **Droplet** (Phase 3.1) - First new type
4. **GUI** (Phase 4) - Selector and fields
5. **More Types** (Phase 3.2+) - Kamehameha, Arrow, etc.
