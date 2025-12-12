# Fill Mode Rendering - RESOLVED

## Issue
Fill mode (SOLID/WIREFRAME/CAGE/POINTS) was not being properly applied when:
1. Cycling through fill variants
2. Manually selecting fill mode from dropdown

## Root Causes Found and Fixed

### 1. Missing fill.mode in FragmentRegistry.applyFillFragment
- **File**: `FragmentRegistry.java`
- **Fix**: Added `state.set("fill.mode", json.get("mode").getAsString())` to apply mode from variant JSON

### 2. Missing FillMode dropdown in FillSubPanel
- **File**: `FillSubPanel.java`  
- **Fix**: Added dedicated `fillModeDropdown` widget for manual mode selection

### 3. Matrix Stack Crash (Pose stack not empty)
- **Files**: `FieldRenderer.java`, `LayerRenderer.java`
- **Fix**: Wrapped `matrices.push()`/`pop()` in try-finally blocks to ensure stack balance

### 4. Cage rendering crash (Mesh is not TRIANGLES type)
- **File**: `SphereRenderer.java` line 258
- **Problem**: `emitCage()` built a LINES mesh but passed it to `emitWireframe()` which expects TRIANGLES
- **Fix**: Changed to use `emitter.emit(cageMesh)` which correctly handles LINES meshes

### 5. Removed fallback to SimplifiedFieldRenderer
- **File**: `SimplifiedFieldRenderer.java`
- **Fix**: Removed try-catch fallback that was hiding real errors

## Status
**RESOLVED** - Fill modes (SOLID, WIREFRAME, CAGE, POINTS) now work correctly for sphere shapes.
