# Primitive Selection Bug Analysis

## The Problem Statement

**User's words:** "When I change primitive (by creating a new one or using the arrows), the context of the GUI NEVER changes. It's ALWAYS at index 0."

**More specifically:** "you create a new primitive, you switch to it and then you save and then you switch back to the first primitive"

## The Current Flow (What SHOULD happen)

```
1. User clicks "Add Primitive"
2. onPrimitiveAdd() is called:
   a. addPrimitive() creates primitive_2 at index 1
   b. setSelectedPrimitiveIndex(1) is called:
      i.   saveSelectedPrimitive() → saves global state to primitive_0
      ii.  selectedPrimitivePerLayer.set(0, 1) → selection is now 1
      iii. loadSelectedPrimitive() → loads primitive_1 values into global state
      iv.  notifySelectionChanged() → triggers onSelectionChanged()
           → contentArea.rebuild() → NEW panels read from global state
   c. selectorBar.refreshPrimitives()
   d. selectorBar.selectPrimitiveIndex(newIdx)  
   e. contentArea.rebuild() (AGAIN)
3. Panels should show primitive_1's values
```

## What Actually Happens

The panels STILL show primitive_0's values, even though:
- The log shows `[PRIM-SET] Setting primitive index: requested=1, clamped=1, layerIdx=0`
- The selector button correctly shows "primitive_2"

## The Question

At step 2.b.iii, `loadSelectedPrimitive()` should load primitive_1's values into:
- `this.sphere` (the sphere shape)
- `this.fill` (the fill config)
- etc.

Then at step 2.b.iv, when panels are rebuilt, they read from `state.getFloat("sphere.radius")` which reads `this.sphere.radius()`.

**WHY is `this.sphere` still showing primitive_0's values?**

Possibilities:
1. `loadSelectedPrimitive()` isn't actually setting `this.sphere`
2. Something is resetting `this.sphere` after the load
3. The panels aren't actually reading from global state but from somewhere else
4. There's a duplicate/stale state object being used

## Next Debug Step

Add logging to `loadSelectedPrimitive()` to show:
1. Which primitive ID is being loaded
2. What the sphere.radius value is BEFORE the load
3. What the sphere.radius value is AFTER the load
