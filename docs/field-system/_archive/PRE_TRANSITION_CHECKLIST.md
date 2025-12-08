# Pre-Transition Checklist

> **Date:** December 6, 2024  
> **Goal:** Make code match ARCHITECTURE.md specification  
> **Status:** In Progress

---

## TODO Items - Build to Match Spec

### 1. Static Tessellator Facade

**ARCHITECTURE.md says (line 112):**
```java
Mesh mesh = Tessellator.tessellate(new SphereShape(1.0f), detail);
```

**Current:** Instance-based builders only

**Action:** Add static facade method to `Tessellator.java`:
```java
public static Mesh tessellate(Shape shape, int detail);
```

---

### 2. Static VertexEmitter Methods

**ARCHITECTURE.md says (lines 274-297):**
```java
public static void emitQuad(VertexConsumer, Matrix4f, Vec3f[], int argb, int light);
public static void emitMesh(VertexConsumer, Mesh, Matrix4f, int argb, int light);
public static void emitWireframe(...);
```

**Current:** Instance methods only (except emitWireframe)

**Action:** Add static convenience methods to `VertexEmitter.java`

---

### 3. Seal Shape Interface

**ARCHITECTURE.md says (line 55):**
```java
public sealed interface Shape  // ← we have open interface
```

**Current:** `public interface Shape`

**Action:** Change to:
```java
public sealed interface Shape permits 
    SphereShape, RingShape, PrismShape, PolyhedronShape, BeamShape, DiscShape
```

**File:** `src/main/java/net/cyberpunk042/visual/shape/Shape.java`

---

### 4. Create CubeRenderer

**ARCHITECTURE.md says (line 322):**
```java
renderCube() → CubeRenderer.render() or CagePrimitive
```

**Current:** Not created

**Action:** Create `CubeRenderer.java` in `client/visual/render/`

---

### 5. Extract FrameSlice

**ARCHITECTURE.md says (line 323):**
```java
FrameSlice → visual/animation/FrameSlice
```

**Current:** Still embedded in `GlowQuadEmitter.java`

**Action:** Extract to `src/main/java/net/cyberpunk042/visual/animation/FrameSlice.java`

---

### 6. Create FieldTypeProvider

**ARCHITECTURE.md says (line 486):**
```
Phase 4: Commands
- [ ] FieldTypeProvider per-type handlers
```

**Current:** Not implemented

**Action:** Create interface + implementations:
- `FieldTypeProvider.java` interface
- Per-type handlers for SHIELD, PERSONAL, etc.

**Location:** `src/main/java/net/cyberpunk042/field/` or `command/field/`

---

### 7. InfectionConfigRegistry Integration

**ARCHITECTURE.md says (line 361):**
```
InfectionConfigRegistry | FieldLoader registers for reload
```

**Current:** Not wired

**Action:** Wire `FieldLoader` to register with `InfectionConfigRegistry` for hot-reload

---

### 8. Verify Data Flow

**ARCHITECTURE.md Data Flow (lines 166-199):**
```
JSON Files → FieldLoader → FieldRegistry → FieldManager → FieldRenderer
```

**Action:** Trace and verify the complete flow works end-to-end

---

## Summary

| # | Item | Type | Est. |
|---|------|------|------|
| 1 | Static Tessellator.tessellate() | Add method | 15 min |
| 2 | Static VertexEmitter methods | Add methods | 20 min |
| 3 | Seal Shape interface | Code fix | 5 min |
| 4 | Create CubeRenderer | New class | 30 min |
| 5 | Extract FrameSlice | Extract class | 20 min |
| 6 | Create FieldTypeProvider | New interface | 45 min |
| 7 | InfectionConfigRegistry wiring | Integration | 20 min |
| 8 | Verify data flow | Testing | 15 min |

**Total estimate:** ~3 hours

---

## NOT in Scope (Deferred)

- Phase 5: Profile Migration
- Phase 6: Cleanup (remove old renderers)
- Phase 7: Optional (TypeA/TypeE sphere, LOD)
