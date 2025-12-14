# Shape Rendering Fixes - Complete

## Root Cause
The tessellators were generating triangles with **incorrect winding order**. In OpenGL/Minecraft rendering:
- **Counter-clockwise (CCW)** winding = front face (visible with standard culling)
- **Clockwise (CW)** winding = back face (culled/invisible)

Many tessellators were generating CW triangles instead of CCW, causing faces to appear invisible or checkerboard patterns.

## Files Fixed

### 1. TorusTessellator.java
**Problem:** Quad triangles wound clockwise
**Fix:** Changed `triangle(i0, i1, i2); triangle(i0, i2, i3);` to CCW winding

### 2. MeshBuilder.quadAsTriangles()
**Problem:** Triangles split along wrong diagonal with wrong winding
**Fix:** Correct split: `triangle(topLeft, topRight, bottomRight); triangle(topLeft, bottomRight, bottomLeft);`

### 3. PrismTessellator.java (side faces)
**Problem:** Side quads wound clockwise
**Fix:** Changed to CCW: `triangle(i00, i01, i11); triangle(i00, i11, i10);`

### 4. CylinderTessellator.java (side wall)
**Problem:** Side wall quads wound clockwise
**Fix:** Same pattern as prism

### 5. PolyhedronTessellator.emitTriangle()
**Problem:** When normal pointed inward, code only flipped the normal without fixing winding
**Fix:** Now swaps v1↔v2 to correct winding, then recomputes normal

### 6. PolyhedronTessellator.tessellateIcosahedron()
**Problem:** Face indices may have been incorrect for vertex layout
**Fix:** Replaced with verified face indices organized as top 5, bottom 5, middle 10

### 7. PrimitiveRenderers.get(Shape)
**Problem:** Missing cases for TorusShape, CapsuleShape, ConeShape → returned null
**Fix:** Added all three shape types to switch statement

## Other Fixes (Earlier)
- **FieldCustomizerScreen** - Uses `FieldEditStateHolder.getOrCreate()` to preserve state
- **GuiClientHandlers** - Uses existing state when opening GUI
- **SphereRenderer.emitCage()** - Uses `emit()` for LINES meshes

## Pending Test
After rebuild, verify:
- [ ] Icosahedron renders correctly (no spikes/holes)
- [ ] Torus renders completely (no checkerboard)
- [ ] Prism renders correctly
- [ ] Cylinder renders correctly
- [ ] All other shapes work
- [ ] State persists when reopening menu
