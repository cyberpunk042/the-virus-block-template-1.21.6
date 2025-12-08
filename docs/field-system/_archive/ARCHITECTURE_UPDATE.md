# Architecture Update - Profile Migration Gaps

> **Date:** December 6, 2024  
> **Purpose:** Detailed mapping of old profile features to new architecture

---

## Summary of Changes Required

Based on analysis of `ShieldProfileConfig` and `ShieldMeshLayerConfig`, we need to extend several classes to support all the features from the old system.

---

## 1. Modifiers Enhancement

### Current State
```java
public record Modifiers(
    float radiusMultiplier,
    float strengthMultiplier,
    float alphaMultiplier,
    float spinMultiplier,
    boolean inverted,
    boolean pulsing
)
```

### Proposed Changes
```java
public record Modifiers(
    // Existing
    float radiusMultiplier,
    float strengthMultiplier,
    float alphaMultiplier,
    float spinMultiplier,
    boolean inverted,
    boolean pulsing,
    
    // NEW: Visual scale (global render scale, different from radius)
    float visualScale,
    
    // NEW: Movement-based tilt (tilts field in movement direction)
    float tiltMultiplier,
    
    // NEW: Swirl distortion on surface
    float swirlStrength
)
```

### Rationale
- `visualScale`: Global render multiplier (old: `visualScale`)
- `tiltMultiplier`: Tilts field based on velocity (old: `tiltMultiplier`)
- `swirlStrength`: Surface texture rotation (old: `swirlStrength`)

---

## 2. Appearance Enhancement

### Current State
```java
public record Appearance(
    String color,
    float alpha,
    boolean fill,
    float glow,
    float wireThickness
)
```

### Proposed Changes
```java
public record Appearance(
    String color,
    float alphaMin,     // RENAMED from alpha
    float alphaMax,     // NEW: for pulsing alpha
    boolean fill,
    float glow,
    float wireThickness
)
```

### Alternative: Add AlphaRange record
```java
// In visual/appearance/AlphaRange.java
public record AlphaRange(float min, float max) {
    public static final AlphaRange FULL = new AlphaRange(1.0f, 1.0f);
    public static final AlphaRange TRANSLUCENT = new AlphaRange(0.4f, 0.8f);
    
    public float at(float t) {
        return min + (max - min) * t;  // For pulsing
    }
}

// Updated Appearance
public record Appearance(
    String color,
    AlphaRange alpha,   // CHANGED from float
    boolean fill,
    float glow,
    float wireThickness
)
```

### Rationale
- Old system had `minAlpha` / `maxAlpha` for pulsing effects
- Single alpha doesn't support this
- `AlphaRange` is cleaner than two separate floats

---

## 3. SphereShape Enhancement (Partial Spheres)

### Current State
```java
public record SphereShape(
    float radius,
    int latSteps,
    int lonSteps
) implements Shape
```

### Proposed Changes
```java
public record SphereShape(
    float radius,
    int latSteps,
    int lonSteps,
    // NEW: Partial sphere parameters
    float latStart,     // 0.0 = north pole, default
    float latEnd,       // 1.0 = south pole, default
    float lonStart,     // 0.0 = start angle, default
    float lonEnd        // 1.0 = full circle, default
) implements Shape
```

### Usage Examples
```java
// Full sphere (default)
SphereShape.of(5.0f)

// Upper hemisphere (dome)
SphereShape.hemisphere(5.0f, true)  // top half
SphereShape.hemisphere(5.0f, false) // bottom half

// Equator band
SphereShape.band(5.0f, 0.4f, 0.6f)  // middle 20%

// Partial arc
SphereShape.arc(5.0f, 0.0f, 0.25f)  // quarter sphere longitude
```

### Rationale
- Old `HEMISPHERE` mesh type was really partial sphere
- Enables domes, bands, segments without new shape types
- Tessellator already supports this concept

---

## 4. Animation Enhancement

### Current State
```java
public record Animation(
    float spin,
    float pulse,
    float pulseAmount,
    float phase
)
```

### Proposed Changes
```java
public record Animation(
    float spin,
    float pulse,
    float pulseAmount,
    float phase,
    // NEW: Additional animation features
    float alphaPulse,       // Alpha oscillation speed
    float alphaPulseAmount, // Alpha oscillation amplitude (uses Appearance.alphaRange)
    Axis spinAxis           // Y (default), X, or Z
)

public enum Axis { X, Y, Z }
```

### Rationale
- Separates scale pulsing from alpha pulsing
- Allows spin around different axes
- Old system had implicit alpha pulsing

---

## 5. FillMode Enhancement

### Current State
```java
public enum FillMode {
    SOLID, WIREFRAME, POINTS, TRANSLUCENT
}
```

### Proposed Changes
```java
public enum FillMode {
    // Existing
    SOLID("solid"),
    WIREFRAME("wireframe"),
    POINTS("points"),
    TRANSLUCENT("translucent"),
    
    // NEW: Patterned fills
    BANDS("bands"),          // Horizontal stripes
    CHECKER("checker"),      // Checkerboard pattern
    
    // Configuration for patterns
    int patternCount;        // For BANDS/CHECKER
    float patternThickness;  // For BANDS
}
```

### Alternative: Separate PatternConfig
```java
// Keep FillMode simple, add PatternConfig
public record PatternConfig(
    PatternType type,
    int count,
    float thickness
) {
    public enum PatternType { NONE, BANDS, CHECKER }
    public static final PatternConfig NONE = new PatternConfig(PatternType.NONE, 0, 0);
}

// Add to Appearance
public record Appearance(
    String color,
    AlphaRange alpha,
    FillMode fill,
    PatternConfig pattern,  // NEW
    float glow,
    float wireThickness
)
```

### Rationale
- BANDS and CHECKER are patterns on top of fill mode
- Separate config is cleaner than overloading enum
- Old system had `meshType: BANDS` with `bandCount`, `bandThickness`

---

## Architecture Diagram Update

### Updated Appearance Package
```
visual/appearance/
├── Appearance.java          # UPDATED: uses AlphaRange, PatternConfig
├── AlphaRange.java          # NEW: min/max alpha for pulsing
├── PatternConfig.java       # NEW: bands/checker patterns
├── FillMode.java            # Keep simple enum
└── Gradient.java            # Existing (future use)
```

### Updated Animation Package
```
visual/animation/
├── Animation.java           # MOVED from field/primitive/
├── Spin.java                # Existing
├── Pulse.java               # Existing  
├── Phase.java               # Existing
├── Animator.java            # Existing
└── Axis.java                # NEW: spin axis enum
```

### Updated Modifiers
```
field/
├── Modifiers.java           # UPDATED: +visualScale, +tiltMultiplier, +swirlStrength
├── FieldDefinition.java
├── FieldType.java
└── ...
```

### Updated SphereShape
```
visual/shape/
├── Shape.java
├── SphereShape.java         # UPDATED: +latStart/End, +lonStart/End
├── RingShape.java
└── ...
```

---

## ARCHITECTURE.md Updates Required

### Section: Appearance (Line ~79)
```diff
 ├── appearance/
 │   ├── Appearance.java          # color, alpha, fill, glow
+│   ├── AlphaRange.java          # min/max alpha for pulsing
+│   ├── PatternConfig.java       # bands, checker patterns
 │   ├── Gradient.java            # linear, radial gradients
-│   ├── Alpha.java               # static or pulsing
 │   └── FillMode.java            # SOLID, WIREFRAME, POINTS
```

### Section: Animation (Line ~85)
```diff
 ├── animation/
 │   ├── Spin.java                # rotation over time
 │   ├── Pulse.java               # scale oscillation
 │   ├── Phase.java               # animation offset
+│   ├── Axis.java                # X, Y, Z rotation axis
 │   └── Animator.java            # applies animations to transforms
```

### Section: Key Classes - Appearance (NEW)
```java
public record Appearance(
    String color,
    AlphaRange alpha,     // min/max for pulsing
    FillMode fill,
    PatternConfig pattern,
    float glow,
    float wireThickness
) {
    // Factory methods
    public static Appearance solid(String color);
    public static Appearance translucent(String color, float min, float max);
    public static Appearance banded(String color, int bands, float thickness);
}

public record AlphaRange(float min, float max) {
    public float at(float t);  // Interpolate for animation
}

public record PatternConfig(PatternType type, int count, float thickness) {
    public enum PatternType { NONE, BANDS, CHECKER }
}
```

### Section: Key Classes - SphereShape (UPDATE)
```java
public record SphereShape(
    float radius,
    int latSteps,
    int lonSteps,
    float latStart,    // 0.0 = north pole
    float latEnd,      // 1.0 = south pole
    float lonStart,    // 0.0 = start angle
    float lonEnd       // 1.0 = full circle
) implements Shape {
    // Factory methods
    public static SphereShape of(float radius);
    public static SphereShape hemisphere(float radius, boolean top);
    public static SphereShape band(float radius, float start, float end);
}
```

### Section: Key Classes - Modifiers (UPDATE)
```java
public record Modifiers(
    float radiusMultiplier,
    float strengthMultiplier,
    float alphaMultiplier,
    float spinMultiplier,
    float visualScale,      // NEW: global render scale
    float tiltMultiplier,   // NEW: movement-based tilt
    float swirlStrength,    // NEW: surface distortion
    boolean inverted,
    boolean pulsing
) {
    public static final Modifiers DEFAULT = ...;
}
```

---

## Implementation Order

1. **AlphaRange** - New record, no dependencies
2. **PatternConfig** - New record, no dependencies
3. **Appearance** - Update to use new records
4. **SphereShape** - Add partial sphere parameters
5. **Modifiers** - Add visual/tilt/swirl parameters
6. **Animation** - Add alphaPulse, spinAxis
7. **Tessellators** - Update to handle partial spheres
8. **Renderers** - Update to handle patterns

---

## Migration Notes

- Old `Alpha.java` (listed in ARCHITECTURE.md but not implemented) → replaced by `AlphaRange`
- Old `meshType: HEMISPHERE` → `SphereShape.hemisphere()`
- Old `meshType: BANDS` → `PatternConfig(BANDS, count, thickness)`
- Old `meshType: CHECKER` → `PatternConfig(CHECKER, count, 0)`

