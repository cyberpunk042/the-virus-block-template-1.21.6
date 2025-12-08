# Pre-Implementation Cleanup Plan

> **Purpose:** Archive old field system code to `_legacy/` folders before refactoring  
> **Date:** December 7, 2024

---

## Overview

Before implementing the new architecture, we archive ALL old code that will be replaced.
This allows us to:
1. Reference old code while building new versions
2. Avoid confusion between old and new during refactoring
3. Keep the codebase compiling throughout

---

## Files to Archive

### 1. Primitives (14 files → `field/_legacy/primitive/`)

| File | Reason |
|------|--------|
| `SolidPrimitive.java` | Abstract class → flat hierarchy |
| `BandPrimitive.java` | Abstract class → flat hierarchy |
| `StructuralPrimitive.java` | Abstract class → flat hierarchy |
| `SpherePrimitive.java` | Will be rebuilt |
| `RingPrimitive.java` | Will be rebuilt |
| `DiscPrimitive.java` | Will be rebuilt |
| `PrismPrimitive.java` | Will be rebuilt |
| `PolyhedronPrimitive.java` | Will be rebuilt |
| `CylinderPrimitive.java` | Will be rebuilt |
| `StripesPrimitive.java` | Becomes visibility config |
| `CagePrimitive.java` | Becomes fill mode config |
| `RingsPrimitive.java` | Becomes multiple RingPrimitive |
| `PrimitiveBuilder.java` | Will be rebuilt |

### 2. Renderers (15 files → `client/visual/_legacy/render/`)

| File | Reason |
|------|--------|
| `SphereRenderer.java` | Will be rebuilt |
| `RingRenderer.java` | Will be rebuilt |
| `DiscRenderer.java` | Will be rebuilt |
| `PrismRenderer.java` | Will be rebuilt |
| `PolyhedronRenderer.java` | Will be rebuilt |
| `CylinderRenderer.java` | Will be rebuilt |
| `StripesRenderer.java` | Becomes part of SphereRenderer |
| `CageRenderer.java` | Becomes fill mode in any renderer |
| `RingsRenderer.java` | Will be removed |
| `CubeRenderer.java` | Will be rebuilt |
| `LayerRenderer.java` | Will be rebuilt |
| `WireframeRenderer.java` | Will be rebuilt |
| `PrimitiveRenderers.java` | Registry, will be rebuilt |
| `RenderOverrides.java` | Will be rebuilt |
| `MeshStyle.java` | Will be rebuilt |

### 3. Tessellators (8 files → `client/visual/_legacy/mesh/`)

| File | Reason |
|------|--------|
| `SphereTessellator.java` | Will be rebuilt |
| `RingTessellator.java` | Will be rebuilt |
| `DiscTessellator.java` | Will be rebuilt |
| `PrismTessellator.java` | Will be rebuilt |
| `PolyhedraTessellator.java` | Will be rebuilt |
| `sphere/SphereAlgorithm.java` | Will be rebuilt |
| `sphere/TypeASphere.java` | Will be rebuilt |
| `sphere/TypeESphere.java` | Will be rebuilt |

### 4. Field Renderers (3 files → `client/field/_legacy/render/`)

| File | Reason |
|------|--------|
| `FieldRenderer.java` | Will be rebuilt |
| `PrimitiveRenderer.java` | Interface, will be rebuilt |
| `FieldRenderContext.java` | Will be rebuilt |

### 5. Old Patterns (1 file → `visual/_legacy/mesh/`)

| File | Reason |
|------|--------|
| `TrianglePattern.java` | Deprecated, replaced by VertexPattern |

---

## Files to KEEP (Utilities & Interfaces)

These are kept in place as they're useful foundations:

| File | Reason |
|------|--------|
| `Primitive.java` | Interface, will be modified |
| `Mesh.java` | Data structure |
| `MeshBuilder.java` | Utility |
| `Vertex.java` | Data structure |
| `Tessellator.java` | Interface |
| `PrimitiveType.java` | Enum |
| `FieldRenderLayers.java` | Render layer utility |
| `RenderLayerFactory.java` | Utility |
| `GlowRenderer.java` | Utility (may keep) |
| `VertexEmitter.java` | Utility (may keep) |

---

## Legacy Folder Structure

After running the script:

```
src/main/java/net/cyberpunk042/
├── field/
│   ├── _legacy/
│   │   └── primitive/
│   │       ├── SolidPrimitive_old.java
│   │       ├── SpherePrimitive_old.java
│   │       └── ...
│   └── primitive/
│       └── Primitive.java  (kept - interface)
│
├── visual/
│   └── _legacy/
│       └── mesh/
│           └── TrianglePattern_old.java

src/client/java/net/cyberpunk042/client/
├── field/
│   └── _legacy/
│       └── render/
│           ├── FieldRenderer_old.java
│           └── PrimitiveRenderer_old.java
│
├── visual/
│   └── _legacy/
│       ├── mesh/
│       │   ├── SphereTessellator_old.java
│       │   └── sphere/
│       │       └── TypeASphere_old.java
│       └── render/
│           ├── SphereRenderer_old.java
│           └── ...
```

---

## How the Script Works

1. **Moves** each file to `_legacy/` folder
2. **Renames** with `_old` suffix (e.g., `SphereRenderer.java` → `SphereRenderer_old.java`)
3. **Updates package** declarations to include `_legacy`
4. **Updates class names** to include `_old`
5. **Updates all imports** across the codebase to point to new locations

---

## Running the Script

```bash
# From project root
python3 scripts/archive_old_field_system.py

# Then compile to check for errors
./gradlew compileJava
```

---

## After Archiving

1. ✅ Old code is in `_legacy/` folders for reference
2. ✅ Codebase should still compile (imports updated)
3. ✅ Ready to implement new architecture
4. ✅ Can reference `*_old.java` files while building new versions

---

## Total Files

| Category | Count |
|----------|-------|
| Primitives | 13 |
| Renderers | 15 |
| Tessellators | 8 |
| Field Renderers | 3 |
| Patterns | 1 |
| **Total Archived** | **40 files** |
| **Kept (utilities)** | ~10 files |

---

*Ready to execute when confirmed.*
