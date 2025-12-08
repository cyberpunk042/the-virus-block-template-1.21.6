# Utility Classes Analysis - Usage & Coverage

> **Date:** December 8, 2024  
> **Purpose:** Determine which utilities are actually used and should be kept

---

## FieldBuilder - Coverage Analysis

### Usage Check:
- **Found:** Only self-references in its own file (examples in javadoc)
- **No actual usage** in codebase

### Relevance:
- Provides convenience methods: `.shield()`, `.personal()`, `.damageEnemies()`, `.healing()`
- Wraps `FieldDefinition.Builder` (which already exists)
- **Overhead:** Minimal - just a facade
- **Coverage:** 0% (not used anywhere)

### Recommendation:
- **Option A:** Keep as convenience utility (not in diagram, but available)
- **Option B:** Remove if not needed (overhead without usage)

---

## FieldParser vs FieldLoader Comparison

### FieldLoader Methods:
- `loadDefinition(Path)` - Loads from file path
- `parseDefinition(JsonObject)` - Parses JSON object
- `load(ResourceManager)` - Loads from ResourceManager
- `reload()` - Reloads definitions

### FieldParser Methods:
- `parseString(json, id)` - Parse from JSON string ✅ **NOT in FieldLoader**
- `parseFile(path)` - Parse from file ✅ **Similar to loadDefinition**
- `parseResource(resourcePath)` - Parse from classpath ✅ **NOT in FieldLoader**
- `parseDirectory(directory)` - Batch parse ✅ **NOT in FieldLoader**
- `toJsonString(definition)` - Serialize ✅ **NOT in FieldLoader**
- `writeToFile(definition, path)` - Write to file ✅ **NOT in FieldLoader**

### Analysis:
**FieldParser provides:**
- String parsing (useful for network/API)
- Resource parsing (useful for built-in configs)
- Directory batch parsing (useful for bulk operations)
- Serialization (useful for saving/copying)

**FieldLoader provides:**
- ResourceManager integration (Minecraft resource system)
- Reference resolution ($ref support)
- Defaults application
- Caching

### Recommendation:
- **Keep FieldParser** - Provides utilities not in FieldLoader
- **Or:** Add these methods to FieldLoader to consolidate

---

## FieldEffects - Usage Check

### Usage:
- Only self-references (examples in javadoc)
- Uses old `EffectType` and `EffectConfig` system
- New system uses `BindingConfig` and `TriggerConfig`

### Recommendation:
- **LEGACY** - Move to `_reference_code/`
- Replaced by bindings/triggers system

---

## PresetRegistry - Usage Check

### Usage:
- Only self-references (examples in javadoc)
- References `_old` classes (`SpherePrimitive_old`, `RingPrimitive_old`)

### Purpose (per user):
- Store subset configs (not full profiles)
- Apply variants to current profile
- Access variants that don't have profiles by default
- User can then save as profile

### Recommendation:
- **Keep but update** - Remove `_old` class references
- **Or:** Replace with JSON $ref system if it covers this use case
- **Question:** Does JSON $ref handle "apply variant to current profile" use case?

---

## FieldProfileStore - Usage Check

### Usage:
- Not checked yet (but user confirmed it's important)

### Purpose:
- User customization - save/load custom shield profiles
- Essential for user experience

### Recommendation:
- **KEEP** - Add to CLASS_DIAGRAM §22

---

## Summary

| Class | Usage | Status | Action |
|-------|-------|--------|--------|
| FieldSystemInit | TODO in TheVirusBlock | ✅ ACTIVE | Add to CLASS_DIAGRAM §21 |
| FieldProfileStore | Important for UX | ✅ ACTIVE | Add to CLASS_DIAGRAM §22 |
| FieldBuilder | 0% usage | ⚠️ REVIEW | Keep as optional utility? |
| FieldParser | Provides unique methods | ⚠️ REVIEW | Keep or merge into FieldLoader? |
| FieldEffects | Old system | ❌ LEGACY | Move to _reference_code/ |
| PresetRegistry | References _old | ⚠️ REVIEW | Update or replace? |

