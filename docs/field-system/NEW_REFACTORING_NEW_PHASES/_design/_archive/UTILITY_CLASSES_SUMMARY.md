# Utility Classes Summary & Actions

> **Date:** December 8, 2024  
> **Status:** Analysis complete, actions taken

---

## ✅ Completed Actions

### 1. FieldSystemInit
- **Status:** ✅ Added to CLASS_DIAGRAM §21
- **TODO:** ✅ Added F169 to call `FieldSystemInit.init()` in mod initializer
- **Usage:** Referenced in TODO (TheVirusBlock.java needs to call it)

### 2. FieldProfileStore
- **Status:** ✅ Added to CLASS_DIAGRAM §22
- **Usage:** ✅ Active - Used for user profile management
- **Note:** FieldDefinition has `toJson()` method (used by FieldProfileStore)

---

## 📊 Analysis Results

### FieldBuilder
- **Usage:** 0% (only self-references in javadoc)
- **Coverage:** Not used anywhere in codebase
- **Relevance:** Low - convenience wrapper over `FieldDefinition.Builder`
- **Recommendation:** Keep as optional utility (not in diagram, available if needed)

### FieldParser
- **Usage:** Only self-references in javadoc
- **Unique Methods:** 
  - `parseString()` - Parse from JSON string
  - `parseResource()` - Parse from classpath resource
  - `parseDirectory()` - Batch parse from directory
  - `toJsonString()` - Serialize to JSON
  - `writeToFile()` - Write to file
- **Comparison:** FieldLoader has `loadDefinition(Path)` and `parseDefinition(JsonObject)`, but lacks serialization and resource parsing
- **Recommendation:** Keep - Provides utilities not in FieldLoader (serialization, batch ops, resource parsing)

### FieldEffects
- **Usage:** 0% (only self-references in javadoc)
- **Status:** ❌ LEGACY - Uses old `EffectType`/`EffectConfig` system
- **Replacement:** New system uses `BindingConfig` and `TriggerConfig`
- **Action:** Move to `_reference_code/` (user confirmed)

### PresetRegistry
- **Usage:** 0% (only self-references in javadoc)
- **Issues:** References `_old` classes (`SpherePrimitive_old`, `RingPrimitive_old`)
- **Purpose (per user):** Store subset configs (not full profiles), apply variants to current profile, access variants without profiles
- **Recommendation:** Keep but update - Remove `_old` class references, or replace with JSON $ref if it covers the use case

---

## 📋 Next Steps

1. ✅ **DONE:** Add FieldSystemInit to CLASS_DIAGRAM
2. ✅ **DONE:** Add TODO for calling FieldSystemInit.init()
3. ⬜ **TODO:** Move FieldEffects to `_reference_code/`
4. ⬜ **TODO:** Review PresetRegistry - update or replace with JSON $ref
5. ⬜ **TODO:** Keep FieldBuilder as optional utility (no action needed)
6. ⬜ **TODO:** Keep FieldParser as utility (no action needed)

---

## Files Modified

- `docs/field-system/NEW_REFACTORING_NEW_PHASES/02_CLASS_DIAGRAM.md` - Added §21 (FieldSystemInit) and §22 (FieldProfileStore)
- `docs/field-system/NEW_REFACTORING_NEW_PHASES/TODO_LIST.md` - Added F169 for FieldSystemInit.init()

---

## Files Created

- `docs/field-system/NEW_REFACTORING_NEW_PHASES/_design/UTILITY_CLASSES_PROPOSAL.md` - Initial proposal
- `docs/field-system/NEW_REFACTORING_NEW_PHASES/_design/UTILITY_ANALYSIS.md` - Usage analysis
- `docs/field-system/NEW_REFACTORING_NEW_PHASES/_design/FIELD_PARSER_VS_FIELDLOADER.md` - Comparison
- `docs/field-system/NEW_REFACTORING_NEW_PHASES/_design/UTILITY_CLASSES_SUMMARY.md` - This file

