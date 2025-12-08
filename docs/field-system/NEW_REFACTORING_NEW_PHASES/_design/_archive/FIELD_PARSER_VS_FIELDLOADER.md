# FieldParser vs FieldLoader Comparison

> **Purpose:** Determine if FieldParser is redundant or provides unique functionality

---

## Method Comparison

### FieldLoader Methods:
| Method | Purpose | FieldParser Equivalent |
|--------|---------|------------------------|
| `loadDefinition(Path)` | Load from file path | `parseFile(path)` ✅ Similar |
| `parseDefinition(JsonObject)` | Parse JSON object | `parseString(json, id)` ✅ Similar |
| `load(ResourceManager)` | Load from ResourceManager | ❌ Not available |
| `reload()` | Reload definitions | ❌ Not available |
| ❌ Not available | Parse from classpath resource | `parseResource(resourcePath)` ✅ Unique |
| ❌ Not available | Batch parse directory | `parseDirectory(directory)` ✅ Unique |
| ❌ Not available | Serialize to JSON string | `toJsonString(definition)` ✅ Unique |
| ❌ Not available | Write to file | `writeToFile(definition, path)` ✅ Unique |

### FieldParser Unique Methods:
1. **`parseResource(resourcePath)`** - Parse from classpath resource
   - Useful for: Built-in configs, test resources
   - FieldLoader: Uses ResourceManager (Minecraft resource system)

2. **`parseDirectory(directory)`** - Batch parse from directory
   - Useful for: Bulk operations, migration scripts
   - FieldLoader: Loads via ResourceManager (not direct directory access)

3. **`toJsonString(definition)`** - Serialize to JSON
   - Useful for: Saving, copying, debugging
   - FieldLoader: No serialization

4. **`writeToFile(definition, path)`** - Write to file
   - Useful for: Saving custom profiles, exporting
   - FieldLoader: No file writing

---

## Recommendation

**Option A: Keep FieldParser**
- Provides utilities not in FieldLoader
- Useful for serialization, batch operations, resource parsing
- Low overhead (utility class)

**Option B: Merge into FieldLoader**
- Add `parseString()`, `parseResource()`, `parseDirectory()`, `toJsonString()`, `writeToFile()` to FieldLoader
- Consolidate all parsing in one place
- More work but cleaner architecture

**Option C: Keep both**
- FieldLoader: ResourceManager-based loading (Minecraft integration)
- FieldParser: Standalone utilities (serialization, batch ops, file I/O)

---

**Suggested:** Option C - Keep both, they serve different purposes

