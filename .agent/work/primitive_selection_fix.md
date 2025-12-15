# Primitive Selection State Persistence - Work Document

## Problem Statement

When working with multiple primitives in the Field Customizer GUI:
1. **Edits apply to wrong primitive**: When user create a primitive and switches to it, it's like the UI doesn't update to connect on the new primitive and rather always show the information of the first primitive and influence still the first primitive
2. **UI shows wrong values**: When switching primitives, the GUI panels still display the previous primitive's values
3. **New primitive doesn't select properly**: After creating a new primitive, the first primitive appears to remain selected even though in the button it's selected

## Architecture Overview

### Key Files
- `FieldEditState.java` - Global state holder with fields like `fill`, `appearance`, `shapeType`, `transform`, etc.
- `DefinitionBuilder.java` - Builds `FieldDefinition` from state for rendering
- `FieldCustomizerScreen.java` - Main GUI screen with selection handlers
- `ContentArea.java` - Manages content panels (Fill, Appearance, etc.)
- `ContentProviderFactory.java` - Creates panel instances
- Various `*SubPanel.java` files - Individual settings panels (FillSubPanel, AppearanceSubPanel, etc.)

### Data Flow (Current Design)

```
┌─────────────────────────────────────────────────────────────────────┐
│                        FieldEditState                                │
│  - Global state fields: fill, appearance, shapeType, transform...   │
│  - fieldLayers: List<FieldLayer> with stored primitives             │
│  - selectedLayerIndex, selectedPrimitivePerLayer                    │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     DefinitionBuilder                                │
│  - rebuildLayerWithCurrentState(): For SELECTED primitive only,     │
│    uses buildCurrentPrimitive() which reads from GLOBAL STATE       │
│  - For NON-SELECTED primitives, uses origPrimitives.get(i) which    │
│    reads from the STORED layer data                                 │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     GUI Panels                                       │
│  - Read from global state at BUILD time:                            │
│    state.fill().mode(), state.getFloat("shape.radius"), etc.        │
│  - Changes call state.set("path", value) which updates global state │
└─────────────────────────────────────────────────────────────────────┘
```

### The Core Architectural Issue

**The global state represents "what we're currently editing"**, but:

1. **Edits are NOT persisted to the stored primitive**: When you edit primitive 0, those edits exist only in global state. The `fieldLayers[0].primitives[0]` object still has OLD values.

2. **Switching primitives overwrites global state**: When you switch to primitive 1, if we load primitive 1's stored values into global state, primitive 0's edits are LOST.

3. **Panels read state at build time**: The panels (FillSubPanel, etc.) read `state.fill()` when their `init()` method is called. They don't dynamically re-read state.

## What Was Attempted

### Attempt 1: Add loadSelectedPrimitive()
**Goal**: When switching primitives, load the new primitive's values into global state so panels show correct data.

**Implementation**:
```java
public void loadSelectedPrimitive() {
    Primitive prim = getSelectedPrimitive();
    if (prim == null) return;
    
    this.shapeType = prim.type();
    this.fill = prim.fill();
    this.mask = prim.visibility();
    this.arrangement = prim.arrangement();
    this.transform = prim.transform();
    // ... load appearance, animation, etc.
}
```

**Called from**: `setSelectedLayerIndex()` and `setSelectedPrimitiveIndex()`

**Why it partially failed**: This overwrites global state but doesn't SAVE the current primitive's edits first, so edits are lost.

### Attempt 2: Add saveSelectedPrimitive()
**Goal**: Before switching, save current global state back to the currently selected primitive.

**Implementation**:
```java
public void saveSelectedPrimitive() {
    int layerIdx = selectedLayerIndex;
    int primIdx = getSelectedPrimitiveIndex();
    
    // Build new primitive from current global state
    SimplePrimitive newPrim = new SimplePrimitive(
        oldPrim.id(), shapeType, currentShape(), transform,
        fill, mask, arrangement,
        buildAppearanceFromState(), buildAnimationFromState(), link
    );
    
    // Replace in layer
    List<Primitive> updatedPrimitives = new ArrayList<>(layer.primitives());
    updatedPrimitives.set(primIdx, newPrim);
    FieldLayer updatedLayer = new FieldLayer(...);
    fieldLayers.set(layerIdx, updatedLayer);
}
```

**Called from**: Beginning of `setSelectedLayerIndex()` and `setSelectedPrimitiveIndex()`

**Potential issues**:
1. Order of operations might be wrong
2. Helper methods `buildAppearanceFromState()` and `buildAnimationFromState()` may have bugs
3. The `link` field might not be properly handled

### Attempt 3: Add contentArea.rebuild() calls
**Goal**: After loading new primitive data into global state, rebuild the UI panels so they display fresh values.

**Implementation**: Added `contentArea.rebuild()` in FieldCustomizerScreen:
- `onPrimitiveSelected()`
- `onPrimitiveAdd()`
- `onLayerSelected()`
- `onLayerAdd()`

**Why this should work**: `ContentArea.rebuild()` creates NEW SubTabPanes which create NEW panels via ContentProviderFactory, and those new panels call their `init()` method which reads fresh values from global state.

## Current State of Code

### Methods Added to FieldEditState.java

1. **saveSelectedPrimitive()** (~lines 785-822): Builds a new `SimplePrimitive` from current global state and replaces it in the layer

2. **loadSelectedPrimitive()** (~lines 854-930): Loads the selected primitive's stored values into global state fields

3. **buildAppearanceFromState()** (~lines 826-839): Helper to convert `AppearanceState` to `Appearance`

4. **buildAnimationFromState()** (~lines 841-850): Helper to build `Animation` from current animation configs

5. **parseColorToInt()** (~lines 934-954): Helper to parse color strings to int

### Call Flow

```
User clicks primitive selector
    → onPrimitiveSelected(name)
        → state.setSelectedPrimitiveIndex(idx)
            → saveSelectedPrimitive()  // Save current edits
            → [change selection index]
            → loadSelectedPrimitive()  // Load new primitive's data
        → contentArea.rebuild()        // Rebuild UI panels
        → registerWidgets()
```

## Why It May Still Not Work

### Possible Issues

1. **saveSelectedPrimitive() might not be called with correct indices**: When adding a new primitive, `addPrimitive()` updates `selectedPrimitivePerLayer` BEFORE `setSelectedPrimitiveIndex()` is called. This means:
   - `addPrimitive()` sets selection to new primitive index
   - `setSelectedPrimitiveIndex()` is called
   - `saveSelectedPrimitive()` runs - but NOW it's saving to the NEW primitive (which is empty), not the OLD one

2. **buildAppearanceFromState() and buildAnimationFromState() may have mismatches**: The conversion between `AppearanceState` (GUI representation) and `Appearance` (data model) may be lossy or incorrect.

3. **loadSelectedPrimitive() may not load all fields**: Some fields might be missed or have null handling issues.

4. **Compilation errors may have prevented code from running**: Earlier in the session there were compilation errors that may not have been fully resolved.

## Suggested Next Steps

1. **Add debug logging**: Add detailed logging to `saveSelectedPrimitive()` and `loadSelectedPrimitive()` to trace exactly what's happening:
   ```java
   Logging.GUI.topic("state").info("saveSelectedPrimitive: layerIdx={}, primIdx={}, shapeType={}", 
       layerIdx, primIdx, shapeType);
   ```

2. **Verify compilation**: Run a clean build to ensure there are no compilation errors.

3. **Check call order in addPrimitive flow**: The `addPrimitive()` method already sets the selection. The subsequent `setSelectedPrimitiveIndex()` call might be saving the wrong primitive.

4. **Consider alternative architecture**: Instead of save/load on switch, consider:
   - Making panels read from `getSelectedPrimitive()` directly rather than global state
   - Or having `state.set()` immediately persist to the selected primitive

5. **Test each piece independently**:
   - Does `getSelectedPrimitive()` return the correct primitive?
   - Does `loadSelectedPrimitive()` correctly update global state?
   - Does `contentArea.rebuild()` create fresh panels?

## Key Code Locations

| File | Method/Location | Purpose |
|------|-----------------|---------|
| `FieldEditState.java` | `saveSelectedPrimitive()` | Persist global state to selected primitive |
| `FieldEditState.java` | `loadSelectedPrimitive()` | Load primitive's values into global state |
| `FieldEditState.java` | `setSelectedPrimitiveIndex()` | Called when selection changes |
| `FieldEditState.java` | `addPrimitive()` | Creates new primitive and sets selection |
| `FieldCustomizerScreen.java` | `onPrimitiveSelected()` | Handler when user clicks primitive selector |
| `FieldCustomizerScreen.java` | `onPrimitiveAdd()` | Handler when user clicks add primitive |
| `ContentArea.java` | `rebuild()` | Recreates all SubTabPanes with fresh panels |
| `DefinitionBuilder.java` | `rebuildLayerWithCurrentState()` | Uses global state for selected primitive during render |

## Files Modified in This Session

1. `FieldEditState.java` - Added save/load methods, modified setSelectedPrimitiveIndex and setSelectedLayerIndex
2. `FieldCustomizerScreen.java` - Added contentArea.rebuild() calls in selection handlers
