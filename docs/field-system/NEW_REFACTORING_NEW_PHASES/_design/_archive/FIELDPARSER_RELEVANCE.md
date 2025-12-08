# FieldParser Relevance Analysis

> **Question:** Why do we need FieldParser when we have FieldLoader?

---

## Key Differences

### FieldLoader (Minecraft Integration)
```java
// Uses ResourceManager (Minecraft's resource system)
public void load(ResourceManager resourceManager)

// Parses with $ref resolution and defaults
public FieldDefinition parseDefinition(JsonObject json) {
    json = referenceResolver.resolveWithOverrides(json);  // ← $ref support
    // ... applies defaults via DefaultsProvider
    // ... caches results
}

// Loads from file path (but designed for ResourceManager)
public FieldDefinition loadDefinition(Path path)
```

**Features:**
- ✅ **$ref resolution** - Resolves JSON references (`"$ref": "field_shapes/sphere.json"`)
- ✅ **Defaults application** - Applies default values via DefaultsProvider
- ✅ **Caching** - Caches loaded definitions
- ✅ **ResourceManager integration** - Works with Minecraft's resource system
- ❌ **No serialization** - Can't convert FieldDefinition back to JSON
- ❌ **No string parsing** - Takes JsonObject, not String
- ❌ **No classpath resources** - Uses ResourceManager, not classpath

---

### FieldParser (Standalone Utilities)
```java
// Parses from JSON string
public static FieldDefinition parseString(String json, Identifier id) {
    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
    return FieldDefinition.fromJson(obj, id);  // ← Direct parsing, no $ref
}

// Parses from classpath resource
public static FieldDefinition parseResource(String resourcePath)

// Batch parse directory
public static List<FieldDefinition> parseDirectory(Path directory)

// Serialization
public static String toJsonString(FieldDefinition definition)
public static void writeToFile(FieldDefinition definition, Path path)
```

**Features:**
- ❌ **No $ref resolution** - Direct parsing only
- ❌ **No defaults** - Uses FieldDefinition.fromJson directly
- ❌ **No caching** - Each call parses fresh
- ✅ **String parsing** - Can parse from JSON string
- ✅ **Classpath resources** - Can load from classpath (not ResourceManager)
- ✅ **Serialization** - Can convert FieldDefinition → JSON string/file
- ✅ **Batch operations** - Can parse entire directories

---

## The Problem

**FieldParser calls `FieldDefinition.fromJson(obj, id)`** - but does this method exist?

Looking at the current `FieldDefinition.java`:
- It's a **record** with no `fromJson()` method
- It's a **record** with no `toJson()` method

**But FieldProfileStore uses:**
```java
JsonObject json = definition.toJson();  // ← This doesn't exist!
```

**And FieldParser uses:**
```java
FieldDefinition def = FieldDefinition.fromJson(obj, id);  // ← This doesn't exist!
```

---

## Use Cases for FieldParser

### 1. **Serialization** (toJsonString, writeToFile)
- **Use case:** Save custom profiles, export configurations, backup
- **Current status:** ❌ Broken - FieldDefinition.toJson() doesn't exist
- **Alternative:** Could add toJson() to FieldDefinition directly

### 2. **String Parsing** (parseString)
- **Use case:** Network payloads, API responses, user input, test data
- **Current status:** ❌ Broken - FieldDefinition.fromJson() doesn't exist
- **Alternative:** Could use FieldLoader.parseDefinition(JsonObject) after converting string

### 3. **Classpath Resources** (parseResource)
- **Use case:** Built-in configs, test resources
- **Current status:** ❌ Broken - FieldDefinition.fromJson() doesn't exist
- **Alternative:** ResourceManager already handles this

### 4. **Batch Directory Parsing** (parseDirectory)
- **Use case:** Bulk operations, migration scripts, custom config directories
- **Current status:** ❌ Broken - FieldDefinition.fromJson() doesn't exist
- **Alternative:** Could add to FieldLoader

---

## The Real Question

**Do we need FieldParser at all?**

### Option A: Remove FieldParser
- Add `toJson()` to FieldDefinition (for serialization)
- Add `fromJson()` to FieldDefinition (but this bypasses $ref/defaults)
- Use FieldLoader.parseDefinition() for all parsing (but it needs JsonObject, not String)
- **Problem:** FieldLoader.parseDefinition() does $ref/defaults, but FieldDefinition.fromJson() wouldn't

### Option B: Keep FieldParser but fix it
- Add `toJson()` to FieldDefinition
- FieldParser.parseString() → convert String to JsonObject → call FieldLoader.parseDefinition()
- **Problem:** FieldParser becomes a thin wrapper around FieldLoader

### Option C: Merge into FieldLoader
- Add `parseString(String, Identifier)` to FieldLoader
- Add `parseResource(String)` to FieldLoader
- Add `parseDirectory(Path)` to FieldLoader
- Add `toJsonString(FieldDefinition)` to FieldLoader
- Add `writeToFile(FieldDefinition, Path)` to FieldLoader
- **Result:** One class for all parsing/loading/serialization

---

## Recommendation

**Option C: Merge into FieldLoader**

**Reasons:**
1. FieldParser is currently broken (calls non-existent methods)
2. FieldParser duplicates functionality (parseFile vs loadDefinition)
3. FieldParser bypasses $ref/defaults (FieldDefinition.fromJson wouldn't have them)
4. Consolidation reduces confusion (one class for all JSON operations)

**Implementation:**
- Move serialization methods to FieldLoader
- Add string/resource/directory parsing to FieldLoader
- Keep $ref/defaults in all parsing paths
- Remove FieldParser class

**Exception:** If FieldDefinition.fromJson() should exist as a low-level parser (without $ref/defaults), then FieldParser makes sense as a convenience wrapper. But currently it's broken.

---

## Current Status

**FieldParser is NOT relevant because:**
1. ❌ It calls `FieldDefinition.fromJson()` which doesn't exist
2. ❌ It calls `definition.toJson()` which doesn't exist
3. ❌ It bypasses $ref resolution and defaults
4. ❌ It duplicates FieldLoader.loadDefinition() functionality

**FieldParser WOULD be relevant if:**
1. ✅ FieldDefinition had `fromJson()` for low-level parsing (no $ref/defaults)
2. ✅ FieldDefinition had `toJson()` for serialization
3. ✅ FieldParser was a convenience wrapper that adds string/resource/directory utilities
4. ✅ But still, these could be in FieldLoader instead

---

**Conclusion:** FieldParser is currently **not relevant** because it's broken. Either fix it (add missing methods) or merge its utilities into FieldLoader.

