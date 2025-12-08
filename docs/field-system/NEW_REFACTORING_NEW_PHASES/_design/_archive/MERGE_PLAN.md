# FieldParser → FieldLoader Merge Plan

> **Date:** December 8, 2024  
> **Status:** Planning

---

## What's Missing

### 1. FieldDefinition.toJson() ❌
- **Status:** Missing
- **Dependencies:** Needs nested types to have toJson()
- **Action:** Add toJson() method

### 2. Nested Types - toJson() Status

| Type | Has toJson()? | Used in FieldDefinition |
|------|---------------|------------------------|
| FieldLayer | ❌ No | ✅ Yes (layers) |
| Modifiers | ✅ Yes | ✅ Yes (modifiers) |
| PredictionConfig | ❌ No | ✅ Yes (prediction) |
| BeamConfig | ❌ No | ✅ Yes (beam) |
| FollowModeConfig | ❌ No | ✅ Yes (followMode) |
| BindingConfig | ❌ No | ✅ Yes (bindings) |
| TriggerConfig | ❌ No | ✅ Yes (triggers) |
| LifecycleConfig | ❌ No | ✅ Yes (lifecycle) |
| Primitive | ❌ No | ✅ Yes (via FieldLayer.primitives) |
| Transform | ❌ No | ✅ Yes (via FieldLayer.transform) |
| Animation | ❌ No | ✅ Yes (via FieldLayer.animation) |

### 3. FieldLoader Methods to Add

| Method | Source | Status |
|--------|--------|--------|
| `parseString(String, Identifier)` | FieldParser | ❌ Missing |
| `parseResource(String)` | FieldParser | ❌ Missing |
| `parseDirectory(Path)` | FieldParser | ❌ Missing |
| `toJsonString(FieldDefinition)` | FieldParser | ❌ Missing |
| `writeToFile(FieldDefinition, Path)` | FieldParser | ❌ Missing |

### 4. FieldParser References

- **Files using FieldParser:** Need to check
- **Action:** Update to use FieldLoader

---

## Implementation Strategy

### Phase 1: Add Missing toJson() Methods (If Needed)

**Option A:** Add toJson() to all nested types
- **Pros:** Complete serialization
- **Cons:** Lots of work, may not be needed yet

**Option B:** Add toJson() to FieldDefinition with TODOs for missing types
- **Pros:** Quick, can add others later
- **Cons:** Incomplete serialization

**Option C:** Add toJson() to FieldDefinition, handle missing types gracefully
- **Pros:** Works now, can improve later
- **Cons:** May serialize incompletely

**Recommendation:** Option C - Add toJson() to FieldDefinition, check for toJson() on nested types, skip if missing (with TODO comments)

### Phase 2: Add Methods to FieldLoader

1. `parseString()` - Convert string to JsonObject, call `parseDefinition()`
2. `parseResource()` - Load from classpath, call `parseString()`
3. `parseDirectory()` - Batch version of `loadDefinition()`
4. `toJsonString()` - Static method, calls `definition.toJson()`
5. `writeToFile()` - Static method, calls `toJsonString()`

### Phase 3: Update References

- Find all `FieldParser.*` references
- Replace with `FieldLoader.*` or `new FieldLoader().*`
- Update imports

### Phase 4: Delete FieldParser

- Remove `FieldParser.java`
- Update documentation

---

## Script Plan

The Python script will:

1. ✅ Add `toJson()` to FieldDefinition (with null checks for missing nested toJson())
2. ✅ Add FieldParser methods to FieldLoader
3. ✅ Find and update FieldParser references
4. ✅ Delete FieldParser.java
5. ✅ Update CLASS_DIAGRAM.md
6. ✅ Update TODO_LIST.md

**Safety:**
- Never deletes directories
- Only modifies specific files
- Creates backups (optional)
- Reports all changes

---

## Next Steps

1. Run analysis to confirm missing toJson() methods
2. Run merge script
3. Fix compilation errors (add missing toJson() methods as needed)
4. Test serialization
5. Update documentation

