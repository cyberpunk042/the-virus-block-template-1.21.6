# FieldEditState: Annotation-Driven Architecture Refactoring

**File:** `src/client/java/net/cyberpunk042/client/gui/state/FieldEditState.java`  
**Original Lines:** 1,505  
**Analysis Date:** December 15, 2024

---

## ✅ IMPLEMENTATION COMPLETE

| Component | Lines | Status |
|-----------|-------|--------|
| **Original FieldEditState** | 1,505 | To be replaced |
| **FieldEditStateV2** | 431 | ✓ Created |
| ShapeAdapter | ~175 | ✓ Created |
| AnimationAdapter | ~80 | ✓ Created |
| FillAdapter | ~40 | ✓ Created |
| TransformAdapter | ~40 | ✓ Created |
| AppearanceAdapter | ~90 | ✓ Created |
| VisibilityAdapter | ~40 | ✓ Created |
| ArrangementAdapter | ~40 | ✓ Created |
| Infrastructure | ~150 | ✓ Created |
| **New Total** | ~1,086 | - |

### Result: **71% reduction in FieldEditState** (1,505 → 431 lines)

**Files Created:**
```
src/client/java/net/cyberpunk042/client/gui/state/adapter/
├── StateCategory.java       # @StateCategory annotation
├── PrimitiveAdapter.java    # Interface for primitive adapters
├── DefinitionAdapter.java   # Interface for definition adapters
├── AbstractAdapter.java     # Base class with reflection helpers
├── PrimitiveBuilder.java    # Mutable builder for SimplePrimitive
├── ShapeAdapter.java        # All shape types
├── AnimationAdapter.java    # spin, pulse, wave, wobble, colorCycle
├── FillAdapter.java         # Fill configuration
├── TransformAdapter.java    # Transform
├── AppearanceAdapter.java   # Appearance (colors, glow, etc.)
├── VisibilityAdapter.java   # Visibility mask
└── ArrangementAdapter.java  # Arrangement/pattern config

src/client/java/net/cyberpunk042/client/gui/state/
└── FieldEditStateV2.java    # New lean coordinator (431 lines)
```

---

## Next Steps

1. **Migrate callers** from `FieldEditState` to `FieldEditStateV2`
2. **Add backward-compatible accessors** if needed (e.g. `sphere()` → `shape().sphere()`)
3. **Delete original** `FieldEditState.java` after migration
4. **Rename** `FieldEditStateV2` → `FieldEditState`

---

**The annotations already define the architecture.** Look at the existing code:

```java
@StateField @PrimitiveComponent("fill")
private FillConfig fill = FillConfig.SOLID;

@StateField @PrimitiveComponent("spin")
private SpinConfig spin = SpinConfig.NONE;

@StateField @DefinitionField("modifiers")
private Modifiers modifiers = Modifiers.DEFAULT;
```

The annotations tell us:
- **`@PrimitiveComponent("X")`** → This field goes into `Primitive.X()`
- **`@DefinitionField("X")`** → This field goes into `FieldDefinition.X()`
- **`@StateField`** → This field is editable via the GUI

But currently, all 50+ fields are dumped into ONE class. The annotations are used only for **reading** (in `DefinitionBuilder`), not for **structuring**.

---

## The Pattern: Annotation-Driven State Adapters

What if each **category** of fields is encapsulated in a **State Adapter** that knows how to:

1. **Hold** its related fields
2. **Load** from a `Primitive` 
3. **Save** to a `Primitive.Builder`
4. **Route** path-based access (`"spin.speed"` → `spin.setSpeed()`)

```
   ┌─────────────────────────────────────────────────────────────────┐
   │                     FieldEditState                              │
   │                    (Thin Coordinator)                           │
   │                                                                 │
   │  - Holds adapters[]                                             │
   │  - Routes get/set to correct adapter                            │
   │  - Manages document (layers/primitives)                         │
   └───────────────────────────┬─────────────────────────────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        ▼                      ▼                      ▼
┌───────────────┐    ┌────────────────┐     ┌────────────────┐
│ ShapeAdapter  │    │AnimationAdapter│     │ FillAdapter    │
│               │    │                │     │                │
│ sphere,ring,  │    │ spin,pulse,    │     │ fill config    │
│ disc,prism... │    │ wave,wobble... │     │                │
│               │    │                │     │                │
│ loadFrom(p)   │    │ loadFrom(p)    │     │ loadFrom(p)    │
│ saveTo(b)     │    │ saveTo(b)      │     │ saveTo(b)      │
└───────────────┘    └────────────────┘     └────────────────┘
```

---

## The Adapter Interface

```java
/**
 * Adapter for a category of Primitive/Definition fields.
 * Each adapter encapsulates related fields and knows how to sync with Primitive.
 */
public interface StateAdapter {
    
    /** Category name (matches @PrimitiveComponent value) */
    String category();
    
    /** Load this category's data from a Primitive */
    void loadFrom(Primitive source);
    
    /** Save this category's data to a Primitive builder */
    void saveTo(SimplePrimitive.Builder builder);
    
    /** Get a value by relative path (e.g., "speed" for spin.speed) */
    Object get(String path);
    
    /** Set a value by relative path */
    void set(String path, Object value);
    
    /** Get all field paths this adapter handles */
    Set<String> paths();
}
```

---

## Concrete Adapters

### ShapeAdapter

```java
@StateCategory("shape")
public class ShapeAdapter implements StateAdapter {
    
    @StateField private SphereShape sphere = SphereShape.DEFAULT;
    @StateField private RingShape ring = RingShape.DEFAULT;
    @StateField private DiscShape disc = DiscShape.DEFAULT;
    @StateField private PrismShape prism = PrismShape.builder().build();
    @StateField private CylinderShape cylinder = CylinderShape.builder().build();
    @StateField private PolyhedronShape polyhedron = PolyhedronShape.DEFAULT;
    @StateField private TorusShape torus = TorusShape.DEFAULT;
    @StateField private CapsuleShape capsule = CapsuleShape.DEFAULT;
    @StateField private ConeShape cone = ConeShape.DEFAULT;
    
    @StateField private String shapeType = "sphere";
    
    @Override
    public String category() { return "shape"; }
    
    @Override
    public void loadFrom(Primitive source) {
        this.shapeType = source.type();
        Shape shape = source.shape();
        // Dispatch to correct field based on type
        switch (shape) {
            case SphereShape s -> this.sphere = s;
            case RingShape r -> this.ring = r;
            // ... etc
        }
    }
    
    @Override
    public void saveTo(SimplePrimitive.Builder builder) {
        builder.type(shapeType);
        builder.shape(currentShape());
    }
    
    public Shape currentShape() {
        return switch (shapeType) {
            case "sphere" -> sphere;
            case "ring" -> ring;
            // ... etc
        };
    }
    
    // Path-based access uses StateAccessor internally
    @Override
    public Object get(String path) {
        return StateAccessor.get(this, path);
    }
    
    @Override
    public void set(String path, Object value) {
        StateAccessor.set(this, path, value);
    }
}
```

### AnimationAdapter

```java
@StateCategory("animation")
public class AnimationAdapter implements StateAdapter {
    
    @StateField private SpinConfig spin = SpinConfig.NONE;
    @StateField private PulseConfig pulse = PulseConfig.NONE;
    @StateField private AlphaPulseConfig alphaPulse = AlphaPulseConfig.NONE;
    @StateField private WobbleConfig wobble = WobbleConfig.NONE;
    @StateField private WaveConfig wave = WaveConfig.NONE;
    @StateField private ColorCycleConfig colorCycle = ColorCycleConfig.NONE;
    
    @Override
    public String category() { return "animation"; }
    
    @Override
    public void loadFrom(Primitive source) {
        Animation anim = source.animation();
        if (anim != null) {
            this.spin = anim.spin() != null ? anim.spin() : SpinConfig.NONE;
            this.pulse = anim.pulse() != null ? anim.pulse() : PulseConfig.NONE;
            // ... etc
        }
    }
    
    @Override
    public void saveTo(SimplePrimitive.Builder builder) {
        builder.animation(Animation.builder()
            .spin(spin)
            .pulse(pulse)
            .alphaPulse(alphaPulse)
            .wobble(wobble)
            .wave(wave)
            .colorCycle(colorCycle)
            .build());
    }
}
```

---

## New FieldEditState (Thin Coordinator)

```java
public class FieldEditState {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ADAPTERS (handle field categories)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final ShapeAdapter shape = new ShapeAdapter();
    private final AnimationAdapter animation = new AnimationAdapter();
    private final FillAdapter fill = new FillAdapter();
    private final TransformAdapter transform = new TransformAdapter();
    private final AppearanceAdapter appearance = new AppearanceAdapter();
    private final VisibilityAdapter visibility = new VisibilityAdapter();
    
    private final List<StateAdapter> adapters = List.of(
        shape, animation, fill, transform, appearance, visibility
    );
    
    private final Map<String, StateAdapter> adapterByCategory = adapters.stream()
        .collect(Collectors.toMap(StateAdapter::category, a -> a));
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DOCUMENT (layers + selection)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final List<FieldLayer> layers = new ArrayList<>();
    private int selectedLayerIndex = 0;
    private int selectedPrimitiveIndex = 0;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PATH-BASED ACCESS (routes to adapters)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void set(String path, Object value) {
        String[] parts = path.split("\\.", 2);
        String category = parts[0];
        
        StateAdapter adapter = adapterByCategory.get(category);
        if (adapter != null && parts.length > 1) {
            adapter.set(parts[1], value);
        } else {
            // Direct field on FieldEditState
            StateAccessor.set(this, path, value);
        }
        markDirty();
    }
    
    public Object get(String path) {
        String[] parts = path.split("\\.", 2);
        String category = parts[0];
        
        StateAdapter adapter = adapterByCategory.get(category);
        if (adapter != null && parts.length > 1) {
            return adapter.get(parts[1]);
        }
        return StateAccessor.get(this, path);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMITIVE SYNC (via adapters)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void loadSelectedPrimitive() {
        Primitive prim = getSelectedPrimitive();
        if (prim == null) return;
        
        // Each adapter loads its piece
        for (StateAdapter adapter : adapters) {
            adapter.loadFrom(prim);
        }
    }
    
    public void saveSelectedPrimitive() {
        SimplePrimitive.Builder builder = SimplePrimitive.builder()
            .id(getSelectedPrimitive().id());
        
        // Each adapter saves its piece
        for (StateAdapter adapter : adapters) {
            adapter.saveTo(builder);
        }
        
        // Replace primitive in layer
        replacePrimitive(selectedLayerIndex, selectedPrimitiveIndex, builder.build());
    }
    
    // ... minimal layer management methods ...
}
```

---

## The Revelation: No More Massive Methods

### Before: saveSelectedPrimitive() (50+ lines)
```java
public void saveSelectedPrimitive() {
    // Build new primitive from current global state
    SimplePrimitive newPrim = new SimplePrimitive(
        oldPrim.id(),
        shapeType,
        currentShape(),
        transform,
        fill,
        mask,
        arrangement,
        buildAppearanceFromState(),
        buildAnimationFromState(),
        link
    );
    // ... replace in layer ...
}
```

### After: saveSelectedPrimitive() (10 lines)
```java
public void saveSelectedPrimitive() {
    SimplePrimitive.Builder builder = SimplePrimitive.builder()
        .id(getSelectedPrimitive().id());
    
    for (StateAdapter adapter : adapters) {
        adapter.saveTo(builder);
    }
    
    replacePrimitive(selectedLayerIndex, selectedPrimitiveIndex, builder.build());
}
```

---

## The Revelation: No More loadSelectedPrimitive() Monster

### Before: loadSelectedPrimitive() (100+ lines)
```java
public void loadSelectedPrimitive() {
    Primitive prim = getSelectedPrimitive();
    this.shapeType = prim.type();
    
    Shape shape = prim.shape();
    if (shape != null) {
        switch (shape) {
            case SphereShape s -> this.sphere = s;
            case RingShape r -> this.ring = r;
            // ... 9 more cases ...
        }
    }
    
    this.fill = prim.fill() != null ? prim.fill() : FillConfig.SOLID;
    this.mask = prim.visibility() != null ? ...
    // ... 50 more lines of loading each field ...
}
```

### After: loadSelectedPrimitive() (5 lines)
```java
public void loadSelectedPrimitive() {
    Primitive prim = getSelectedPrimitive();
    if (prim == null) return;
    
    for (StateAdapter adapter : adapters) {
        adapter.loadFrom(prim);
    }
}
```

---

## The @StateCategory Annotation

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface StateCategory {
    /** The category name, matching @PrimitiveComponent values */
    String value();
    
    /** If true, this adapter handles @DefinitionField instead of @PrimitiveComponent */
    boolean definitionLevel() default false;
}
```

This enables **auto-discovery** of adapters:

```java
public class StateAdapterRegistry {
    private static final List<StateAdapter> ADAPTERS = discoverAdapters();
    
    private static List<StateAdapter> discoverAdapters() {
        // Scan for @StateCategory classes and instantiate them
        // Or use ServiceLoader
    }
}
```

---

## Line Count Analysis

| Component | Before | After |
|-----------|--------|-------|
| FieldEditState | 1,505 | ~250 |
| ShapeAdapter | - | ~120 |
| AnimationAdapter | - | ~80 |
| FillAdapter | - | ~40 |
| TransformAdapter | - | ~60 |
| AppearanceAdapter | - | ~70 |
| VisibilityAdapter | - | ~50 |
| StateAdapter interface | - | ~30 |
| **Total** | **1,505** | **~700** |

But more importantly:
- **Each file is focused** (SRP achieved)
- **Adding new adapters is trivial** (Open/Closed)
- **Testing is isolated** (each adapter tests independently)
- **DefinitionBuilder can use adapters too** (code reuse)

---

## Bonus: DefinitionBuilder Simplification

Currently `DefinitionBuilder` has to scan for `@PrimitiveComponent` annotations and manually collect values. With adapters:

```java
// Before: 679 lines with complex reflection logic
private static SimplePrimitive buildCurrentPrimitive(FieldEditState state) {
    Map<String, Object> components = collectPrimitiveComponents(state);
    // ... lots of manual wiring ...
}

// After: Adapters build themselves
private static SimplePrimitive buildCurrentPrimitive(FieldEditState state) {
    return state.buildPrimitive();  // Delegates to adapters
}
```

---

## Implementation Order

1. **Create `StateAdapter` interface**
2. **Create `@StateCategory` annotation**
3. **Extract `ShapeAdapter`** - biggest & clearest boundary
4. **Extract `AnimationAdapter`** - 6 fields, self-contained
5. **Extract remaining adapters** (Fill, Transform, Appearance, Visibility)
6. **Refactor `FieldEditState`** to coordinator
7. **Simplify `DefinitionBuilder`** to use adapters

---

## Why This Works

1. **Annotations drive structure** - `@PrimitiveComponent("X")` tells us the category
2. **Adapters encapsulate categories** - No more 50 fields in one class
3. **Polymorphic save/load** - Each adapter handles its own type
4. **Path routing is trivial** - `"spin.speed"` → find `animation` adapter → delegate
5. **No god class** - Coordinator + small focused adapters
6. **Mediator pattern is implicit** - FieldEditState mediates between adapters and document

---

## Questions for You

Before implementing, do you want to:

1. **Adjust the adapter granularity?** (e.g., one adapter for ALL animation vs separate per-config)
2. **Keep or remove undo/history?** (You mentioned removing it)
3. **Handle @DefinitionField adapters separately?** (modifiers, beam, follow are definition-level)

Ready to start extracting the first adapter?
