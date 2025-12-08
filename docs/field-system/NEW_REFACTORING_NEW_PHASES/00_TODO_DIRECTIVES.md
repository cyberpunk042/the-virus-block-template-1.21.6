# TODO Directives - Working Guide

> **⚠️ READ THIS BEFORE EVERY TODO**  
> **Purpose:** Context restoration and quality control between tasks  
> **Status:** Active Implementation Guide

---

## 🧭 Quick Navigation

### Core Documents (Read Before Each TODO)

| Priority | Document | Purpose | When to Read |
|----------|----------|---------|--------------|
| 🔴 1 | [01_ARCHITECTURE.md](./01_ARCHITECTURE.md) | 5 Geometry Levels, Transform, JSON structure | Every time |
| 🔴 2 | [02_CLASS_DIAGRAM.md](./02_CLASS_DIAGRAM.md) | Classes, enums, records to create | Every time |
| 🟡 3 | [03_PARAMETERS.md](./03_PARAMETERS.md) | All parameters with defaults | When adding fields |
| 🟡 4 | [04_SHAPE_MATRIX.md](./04_SHAPE_MATRIX.md) | Per-shape parameter status | When touching shapes |

### Quick Reference Tables

| What You Need | Where to Find It |
|---------------|------------------|
| What enums to create | CLASS_DIAGRAM §16-17 |
| What records to create | CLASS_DIAGRAM §18 |
| Shape parameter details | SHAPE_PARAMETER_MATRIX §1-9 |
| Transform options | ARCHITECTURE §3 |
| Fill/Visibility options | ARCHITECTURE §4-5 |
| JSON structure | ARCHITECTURE §6 |
| Resolved questions | ARCHITECTURE §10 |

---

## 📜 System Context

### The Old vs New Story

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SYSTEM EVOLUTION                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  LEGACY SYSTEM (Still in Codebase)                                         │
│  ─────────────────────────────────                                         │
│  • Shield/Void Tear/Singularity visuals                                    │
│  • Working but rigid: one class per visual type                            │
│  • Files in: _legacy/ folders with _old suffix                             │
│  • Status: Archived, will be removed after migration                       │
│                                                                             │
│  PARTIAL NEW SYSTEM (Exists but Incomplete)                                │
│  ──────────────────────────────────────────                                │
│  • Field definitions, layers, primitives structure                         │
│  • JSON loading, network sync, command system                              │
│  • Problems: Confusing hierarchy, missing parameters, broken patterns      │
│  • Status: Being restructured, NOT production-ready                        │
│                                                                             │
│  TARGET SYSTEM (What We're Building)                                       │
│  ───────────────────────────────────                                       │
│  • 5 Geometry Levels: Shape → CellType → Arrangement → Visibility → Fill  │
│  • Flat primitive hierarchy (no SolidPrimitive/BandPrimitive)              │
│  • Complete Transform system (anchors, facing, billboard)                  │
│  • All appearance/animation fields implemented                             │
│  • Status: Documented, ready for implementation                            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Why This Matters for Each TODO

When implementing a todo:
1. **Check if old code exists** → May need to adapt, not create from scratch
2. **Check if partial new code exists** → May need to refactor, not create
3. **Always match the TARGET architecture** → Don't copy old patterns

---

## 📋 Document Logic Guide

### What Each Document Contains

| Document | Logic | Read When |
|----------|-------|-----------|
| **ARCHITECTURE_PROPOSAL** | The "why" and "how" - explains the 5 levels, transform system, compatibility matrices | Starting any feature |
| **CLASS_DIAGRAM_PROPOSED** | The "what" - exact classes, fields, methods to implement | Writing code |
| **PARAMETER_INVENTORY** | The "all" - every parameter at every level with defaults and status | Adding/checking fields |
| **SHAPE_PARAMETER_MATRIX** | The "deep" - every parameter per shape type | Working on shapes |
| **CLEANUP_PLAN** | The "before" - files to rename/move before implementing | Before Phase 1 |
| **GUI_DESIGN** | The "later" - Phase 2 UI design | Phase 2 only |

### Document Flow

```
START TODO
    │
    ├─→ Read ARCHITECTURE (understand the "why")
    │
    ├─→ Read CLASS_DIAGRAM (understand the "what")
    │
    ├─→ Check PARAMETER_INVENTORY (get field details)
    │
    ├─→ If shape-related: Read SHAPE_PARAMETER_MATRIX
    │
    ├─→ Write code matching TARGET architecture
    │
    ├─→ Test against existing profiles
    │
    └─→ Update documents if needed
         │
         END TODO
```

---

## 🔄 Timeline & Review Mechanism

### Before Starting a TODO

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  QUICK REVIEW: Previous 5 Tasks                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  For each of the last 5 completed TODOs, verify:                           │
│                                                                             │
│  □ Does it match ARCHITECTURE_PROPOSAL structure?                          │
│  □ Does it match CLASS_DIAGRAM_PROPOSED interfaces?                        │
│  □ Are all parameters from PARAMETER_INVENTORY included?                   │
│  □ Did we update any documentation based on discoveries?                   │
│  □ Did we add any new TODO based on discoveries?                          │
│                                                                             │
│  If ANY checkbox fails → Fix before continuing                             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Current Task Context

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  CURRENT TODO CONTEXT                                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  TODO ID: ________                                                          │
│  Description: ________________________________________________              │
│                                                                             │
│  Relevant Architecture Section: _______                                     │
│  Relevant Class Diagram Section: _______                                    │
│  Relevant Parameters: _______                                               │
│                                                                             │
│  Depends on (previous TODOs that must be done):                            │
│  • ________                                                                 │
│  • ________                                                                 │
│                                                                             │
│  Blocks (future TODOs that depend on this):                                │
│  • ________                                                                 │
│  • ________                                                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### After Completing a TODO

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  COMPLETION CHECKLIST                                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  □ Code compiles with `./gradlew compileJava`                              │
│  □ Code matches CLASS_DIAGRAM structure                                    │
│  □ All fields from PARAMETER_INVENTORY are present                         │
│  □ Default values match PARAMETER_INVENTORY                                │
│  □ Comments/Javadoc added                                                  │
│  □ Logging added where appropriate                                         │
│                                                                             │
│  If discoveries made:                                                       │
│  □ Updated PARAMETER_INVENTORY with new info                               │
│  □ Updated CLASS_DIAGRAM if interface changed                              │
│  □ Updated ARCHITECTURE if behavior changed                                │
│  □ Created new TODO for follow-up work                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📊 Implementation Phases

### Phase 1: Core Restructure (Current)
| Category | Focus | Key Documents |
|----------|-------|---------------|
| Enums | CellType, Anchor, Facing, etc. | CLASS_DIAGRAM §16-17 |
| Records | FillConfig, VisibilityMask, etc. | CLASS_DIAGRAM §18 |
| Transform | Complete rewrite | ARCHITECTURE §3 |
| Primitives | Flatten hierarchy | ARCHITECTURE §5 |
| JSON | Reference system | ARCHITECTURE §6, PARAMETER_INVENTORY §14-15 |

### Phase 2: GUI & Polish
- GUI customization panel
- Player overrides
- Advanced visibility masks

### Phase 3: Advanced Features
- Primitive linking
- Orbit/dynamic positioning
- Pattern animation

### Phase 4: New Shapes
- Torus, Cone, Helix

---

## 🏗️ OOP Best Practices & Design Patterns

### Core Principles (MANDATORY)

| Principle | Description | Applied To |
|-----------|-------------|------------|
| **SRP** | Single Responsibility Principle - each class does ONE thing | Primitives, Renderers, Configs |
| **OCP** | Open/Closed - open for extension, closed for modification | Pattern system, Shape interface |
| **LSP** | Liskov Substitution - subtypes must be substitutable | All Primitive implementations |
| **ISP** | Interface Segregation - many specific interfaces > one general | Shape, VertexPattern |
| **DIP** | Dependency Inversion - depend on abstractions, not concretions | Renderers use interfaces |

### Builder Pattern (PRIMARY PATTERN)

Use the **Builder Pattern** for all complex configuration objects:

```java
// ✅ CORRECT: Builder pattern for immutable configs
public record FillConfig(
    FillMode mode,
    float wireThickness,
    boolean doubleSided,
    CageOptions cage
) {
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private FillMode mode = FillMode.SOLID;
        private float wireThickness = 1.0f;
        private boolean doubleSided = false;
        private CageOptions cage = null;
        
        public Builder mode(FillMode mode) { this.mode = mode; return this; }
        public Builder wireThickness(float t) { this.wireThickness = t; return this; }
        public Builder doubleSided(boolean d) { this.doubleSided = d; return this; }
        public Builder cage(CageOptions c) { this.cage = c; return this; }
        public FillConfig build() { 
            return new FillConfig(mode, wireThickness, doubleSided, cage); 
        }
    }
}

// ✅ Usage
FillConfig config = FillConfig.builder()
    .mode(FillMode.CAGE)
    .wireThickness(2.0f)
    .cage(CageOptions.forSphere(8, 16))
    .build();
```

**Apply Builder Pattern to:**
- [ ] `FillConfig` - complex nested structure
- [ ] `VisibilityMask` - many optional fields
- [ ] `Transform` - 15+ fields with defaults
- [ ] `ArrangementConfig` - multi-part optional fields
- [ ] `SpinConfig`, `PulseConfig` - animation configs
- [ ] `BindingConfig`, `TriggerConfig` - external influences
- [ ] `LifecycleConfig` - lifecycle options

### Factory Pattern

Use **Factory Pattern** for primitive instantiation:

```java
// ✅ CORRECT: Factory creates correct primitive type
public class PrimitiveFactory {
    public static Primitive create(String type, JsonObject json) {
        return switch (type) {
            case "sphere" -> SpherePrimitive.fromJson(json);
            case "ring" -> RingPrimitive.fromJson(json);
            case "disc" -> DiscPrimitive.fromJson(json);
            // ... other types
            default -> throw new IllegalArgumentException("Unknown: " + type);
        };
    }
}
```

### Strategy Pattern (For Patterns/Algorithms)

Use **Strategy Pattern** for interchangeable algorithms:

```java
// ✅ CORRECT: Patterns are strategies
public interface VertexPattern {
    String id();
    CellType cellType();
    boolean shouldRender(int index, int total);
}

// Each pattern implements the strategy
public enum QuadPattern implements VertexPattern {
    FILLED_1 { ... },
    WAVE_1 { ... },
    // ...
}
```

### Immutability

**ALL configuration objects must be immutable:**

```java
// ✅ CORRECT: Immutable record
public record SphereShape(
    float radius,
    int latSteps,
    int lonSteps,
    SphereAlgorithm algorithm
) implements Shape {
    // Defensive copy in constructor if needed
    public SphereShape {
        if (radius <= 0) throw new IllegalArgumentException("radius must be positive");
    }
}

// ❌ WRONG: Mutable class
public class SphereShape {
    private float radius; // Mutable!
    public void setRadius(float r) { this.radius = r; } // Setter!
}
```

### Composition Over Inheritance

```java
// ❌ WRONG: Deep inheritance hierarchy
abstract class SolidPrimitive extends AbstractPrimitive { }
class SpherePrimitive extends SolidPrimitive { }

// ✅ CORRECT: Flat implementation with composition
class SpherePrimitive implements Primitive {
    private final SphereShape shape;      // Composition
    private final Transform transform;     // Composition
    private final FillConfig fill;         // Composition
    private final Appearance appearance;   // Composition
}
```

### Null Object Pattern

Avoid null checks with Null Object Pattern:

```java
// ✅ CORRECT: Default/empty implementations
public record SpinConfig(Axis axis, float speed, boolean oscillate, float range) {
    public static final SpinConfig NONE = new SpinConfig(Axis.Y, 0, false, 360);
    
    public boolean isActive() { return speed != 0; }
}

// Usage: No null checks needed
SpinConfig spin = primitive.spin() != null ? primitive.spin() : SpinConfig.NONE;
```

### SRP Examples in This Codebase

| Class | Responsibility | What It Doesn't Do |
|-------|---------------|-------------------|
| `SphereShape` | Holds sphere geometry params | Doesn't render, doesn't tessellate |
| `SpherePrimitive` | Combines shape + config + appearance | Doesn't render directly |
| `SphereRenderer` | Renders a SpherePrimitive | Doesn't know about other shapes |
| `SphereTessellator` | Converts SphereShape to Mesh | Doesn't know about rendering |
| `FieldLoader` | Parses JSON into FieldDefinition | Doesn't manage runtime state |
| `FieldManager` | Manages field instances | Doesn't parse JSON |
| `CombatTracker` | Tracks combat state | Doesn't apply field effects |

### Anti-Patterns to Avoid

| Anti-Pattern | Example | Problem | Fix |
|--------------|---------|---------|-----|
| God Class | `FieldRenderer` doing parse + render + animate | Violates SRP | Split into focused classes |
| Primitive Obsession | `render(float r, int lat, int lon, String color...)` | Too many params | Use parameter objects |
| Leaky Abstraction | `Primitive.getTriangleList()` | Exposes implementation | Use `Tessellator` separately |
| instanceof Chains | `if (p instanceof Sphere) else if (p instanceof Ring)...` | Violates OCP | Use polymorphism |
| Magic Numbers | `alpha = 0.35f` | Unclear intent | Use named constants or config |

---

## 🛠️ Development Practices

### Batch Operations
- **Work in batches** - Group related changes together
- **Use Python scripts** for bulk file modifications (never heredocs in WSL)
- Python script workflow: 1) Write script 2) Run with `python3` 3) Compile 4) Iterate

### Python Script Template
```python
#!/usr/bin/env python3
import os

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    # Make changes with content.replace()
    content = content.replace('old', 'new')
    with open(filepath, 'w') as f:
        f.write(content)

# Process files
for root, dirs, files in os.walk('src/main/java'):
    for f in files:
        if f.endswith('.java'):
            process_file(os.path.join(root, f))
```

---

## ⚠️ Common Mistakes to Avoid

| Mistake | Why It's Wrong | Correct Approach |
|---------|----------------|------------------|
| Copying old primitive structure | Creates same hierarchy problems | Use flat Primitive interface |
| Using `StripesPrimitive` | Should be `visibility.mask=STRIPES` | Configure, don't subclass |
| Using `CagePrimitive` | Should be `fill.mode=CAGE` | Configure, don't subclass |
| Hard-coding defaults | Makes JSON incomplete | Read from JSON with fallbacks |
| Skipping `id` field | Breaks linking and debugging | Always require primitive `id` |
| Mixing snake_case/camelCase | Inconsistent JSON | Use camelCase everywhere |
| Editing files one at a time | Slow and error-prone | Use Python scripts for batch changes |

---

## 🔗 Quick Links

### Primary Documents
- [01_ARCHITECTURE.md](./01_ARCHITECTURE.md) - Complete restructure plan
- [02_CLASS_DIAGRAM.md](./02_CLASS_DIAGRAM.md) - Target classes
- [03_PARAMETERS.md](./03_PARAMETERS.md) - All parameters
- [04_SHAPE_MATRIX.md](./04_SHAPE_MATRIX.md) - Per-shape details
- [GAP.md](./GAP.md) - Missing features (bindings, triggers)

### Supporting Documents
- [README.md](./README.md) - Entry point
- [_design/CLEANUP_PLAN.md](./_design/CLEANUP_PLAN.md) - Pre-implementation cleanup

### Review History
- [_reviews/CRITICAL_REVIEW_V1.md](./_reviews/CRITICAL_REVIEW_V1.md) - First review
- [_reviews/CRITICAL_REVIEW_V2.md](./_reviews/CRITICAL_REVIEW_V2.md) - Second review
- [_reviews/CRITICAL_REVIEW_V3.md](./_reviews/CRITICAL_REVIEW_V3.md) - Third review

---

## 📝 Template: TODO Work Session

```markdown
## Session Start
- Date: YYYY-MM-DD
- TODOs planned: [F01, F02, F03...]

## Quick Review (Last 5)
- [x] F00: Verified against architecture
- [x] F-1: Verified against architecture
- ...

## Current TODO: [ID]
- Architecture section: X
- Class diagram section: Y
- Parameters: [list]

## Implementation Notes
- ...

## Discoveries
- ...

## Documentation Updates
- Updated X because...

## Session End
- Completed: [F01, F02]
- Blocked: [F03] - reason
- New TODOs: [Fnew1, Fnew2]
```

---

*This document is the "home base" during implementation. Return here between each TODO.*


---

## Code Quality Standards

### Commenting & Documentation

Every class must have:

1. **Class-level Javadoc:**
   ```java
   /**
    * Renders sphere primitives using the tessellation pipeline.
    * 
    * <p>Supports all FillModes (SOLID, WIREFRAME, CAGE, POINTS) and 
    * applies VisibilityMask filtering before emission.</p>
    * 
    * @see SphereTessellator
    * @see VertexEmitter
    */
   public class SphereRenderer implements PrimitiveRenderer {
   ```

2. **Method-level comments for non-trivial logic:**
   ```java
   // Apply layer spin first (rotates entire layer around anchor)
   // Then apply primitive spin (rotates individual shape)
   // This enables "orbiting orbs that also spin" effect
   matrices.multiply(layerRotation);
   matrices.multiply(primitiveRotation);
   ```

3. **Section markers for long methods:**
   ```java
   // === PHASE 1: Tessellation ===
   Mesh mesh = tessellator.tessellate(shape, pattern, visibility);
   
   // === PHASE 2: Transform ===
   applyTransform(matrices, primitive.transform(), time);
   
   // === PHASE 3: Emission ===
   emitter.emitMesh(consumer, mesh, matrices.peek(), color, light);
   ```

### Logging Standards

Every significant class must integrate with the Logging system:

1. **Use the FIELD channel:**
   ```java
   private static final Channel LOG = Logging.FIELD;
   ```

2. **Constructor logging (DEBUG level):**
   ```java
   Logging.of(LOG).topic("init").debug("SphereRenderer initialized");
   ```

3. **Operation logging (TRACE level for hot paths):**
   ```java
   Logging.of(LOG).topic("render")
       .with("primitive", primitive.id())
       .with("fillMode", fill.mode())
       .trace("Rendering sphere");
   ```

4. **Error logging with context:**
   ```java
   Logging.of(LOG).topic("pattern")
       .with("expected", shape.primaryCellType())
       .with("got", pattern.cellType())
       .alwaysChat()  // Shows in player chat
       .error("Pattern CellType mismatch");
   ```

5. **Performance logging (for expensive operations):**
   ```java
   long start = System.nanoTime();
   // ... expensive operation ...
   Logging.of(LOG).topic("perf")
       .with("operation", "tessellate")
       .with("ms", (System.nanoTime() - start) / 1_000_000.0)
       .debug("Tessellation complete");
   ```

### Notes in Complex Areas

Add `// NOTE:` comments for:
- Design decisions that aren't obvious
- Workarounds for Minecraft API quirks
- Performance considerations
- Future improvement opportunities

```java
// NOTE: We use QUADS render layer even for triangles because
// Minecraft's line rendering has Z-fighting issues with translucent
// surfaces. Converting to degenerate quads avoids this.

// NOTE: This could be optimized by caching the mesh, but we
// regenerate each frame to support dynamic patterns. Consider
// caching for static patterns in Phase 2.

// NOTE: Minecraft's VertexConsumer expects ARGB, not RGBA.
// Always pack colors with (alpha << 24) | (red << 16) | ...
```

