# Legacy References That Need Fixing

## Summary
Compilation errors show references to old/legacy code that doesn't exist in the current refactored system.

## Issues Found

### 1. Missing `FieldRegistry` Class
**Files affected:**
- `src/client/java/net/cyberpunk042/client/field/FieldClientInit.java` (line 5, 97, 181, 204)
- `src/client/java/net/cyberpunk042/client/visual/ClientFieldManager.java` (line 6, 261)
- `src/client/java/net/cyberpunk042/client/visual/PersonalFieldTracker.java` (line 4, 82)

**Problem:** Code references `FieldRegistry.get()` and `FieldRegistry.register()` but this class doesn't exist in the current codebase.

**Solution:** 
- `FieldRegistry` was replaced by `FieldLoader` which loads definitions from JSON
- Client-side should receive definitions via `FieldDefinitionSyncPayload` network packets
- Need to store definitions in a client-side map or get them from the server

### 2. Missing `FieldDefinition.fromJson()` Method
**Files affected:**
- `src/client/java/net/cyberpunk042/client/field/FieldClientInit.java` (line 180)

**Problem:** `FieldDefinition` is a record, not a class with static `fromJson()` method.

**Solution:**
- Use `FieldLoader.parseFieldDefinition()` or similar method
- Or receive definitions from server via network payloads

### 3. Wrong Package for `PredictionConfig`
**Files affected:**
- `src/client/java/net/cyberpunk042/client/visual/PersonalFieldTracker.java` (line 5, 127, 854)

**Problem:** Imports `net.cyberpunk042.field.PredictionConfig` but it's actually in `net.cyberpunk042.field.instance.PredictionConfig`

**Solution:** Fix import statement

### 4. Legacy `_old` Classes Being Referenced
**Files affected:**
- Multiple files in `src/client/java/net/cyberpunk042/client/visual/_legacy/`
- `src/client/java/net/cyberpunk042/client/visual/ClientFieldManager.java` (line 299-300, 607-614)

**Problem:** Code references `RenderOverrides_old` and other legacy classes that don't exist.

**Solution:** 
- Remove references to `_old` classes
- Use new rendering system instead

### 5. Missing Methods on Current Classes
**Issues:**
- `FieldDefinition.type()` - doesn't exist (should be `fieldType()`)
- `FieldDefinition.prediction()` - doesn't exist
- `Transform.isIdentity()`, `scaleX()`, `scaleY()`, `scaleZ()` - don't exist
- `Primitive.shapeType()` - doesn't exist
- `Appearance.alpha()` returns `AlphaRange` not `float`
- `QuadPattern.triangles()` - doesn't exist

**Solution:** Update code to use correct method names or add missing methods

### 6. Legacy Primitive Classes
**Files affected:**
- Multiple renderers trying to import `net.cyberpunk042.field._legacy.primitive.*`

**Problem:** These classes don't exist - they were replaced by the new primitive system.

**Solution:** Update renderers to use new primitive system (`net.cyberpunk042.field.primitive.Primitive`)

## Priority Fixes

1. **High Priority:** Fix `FieldRegistry` references - this breaks client initialization
2. **High Priority:** Fix `PredictionConfig` import path
3. **Medium Priority:** Remove `_old` class references
4. **Medium Priority:** Fix method name mismatches
5. **Low Priority:** Clean up legacy renderer code

## Next Steps

1. Create a client-side field definition store (or use network payloads)
2. Fix import paths
3. Update method calls to match current API
4. Remove or update legacy code references

