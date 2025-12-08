# Utility Classes Proposal for CLASS_DIAGRAM

> **Purpose:** Review utility classes and propose additions to CLASS_DIAGRAM  
> **Date:** December 8, 2024

---

## Analysis of Utility Classes

### 1. FieldSystemInit ✅ **SHOULD BE IN DIAGRAM**

**Purpose:** Central initialization for the field system  
**Status:** Active - TODO in TheVirusBlock.java to call it

**What it does:**
- Registers network payloads
- Registers commands (`/field`, `/fieldtest`)
- Registers resource reload listener for JSON definitions
- Wires FieldManager to FieldNetworking
- Syncs definitions/instances to players on join

**Proposal:** Add to CLASS_DIAGRAM §15 (Loading & Parsing) or new §21 (System Initialization)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    System Initialization                            │
│                    Package: field                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                  FieldSystemInit                           │   │
│  ├─────────────────────────────────────────────────────────────┤   │
│  │ + init(): void              ← Call during mod initialization │   │
│  │ + isInitialized(): boolean  ← Check if already initialized  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  Responsibilities:                                                  │
│  - Register network payloads (FieldSpawnPayload, etc.)            │
│  - Register commands (FieldCommand, FieldTestCommand)             │
│  - Register resource reload listener for JSON definitions          │
│  - Wire FieldManager ↔ FieldNetworking                            │
│  - Sync definitions/instances to players on join                   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

### 2. FieldProfileStore ⚠️ **MAYBE - User Profile Management**

**Purpose:** Saves/loads custom field profiles to `config/the-virus-block/profiles/`  
**Status:** Utility for user-created profiles

**What it does:**
- `save(name, definition)` - Save custom profile
- `load(name)` - Load custom profile
- `loadAndRegister(name)` - Load and register in FieldRegistry
- `list()` - List all saved profiles
- `delete(name)` - Delete a profile

**Proposal:** Add as utility section if user profiles are part of the design

**Question:** Are user-created profiles part of the new architecture, or should everything be JSON-based?

---

### 3. FieldBuilder ⚠️ **MAYBE - Convenience Builder**

**Purpose:** Convenience facade over `FieldDefinition.Builder` with preset methods  
**Status:** Utility - provides shortcuts

**What it does:**
- Factory methods: `.shield(id)`, `.personal(id)`, `.force(id)`
- Shortcuts: `.damageEnemies(damage)`, `.healing(amount)`, `.pushBack(strength)`
- Wraps `FieldDefinition.Builder` with convenience methods

**Proposal:** 
- Option A: Keep as utility (not in diagram - it's just a convenience wrapper)
- Option B: Add to diagram if preset configurations are important

**Note:** `FieldDefinition.Builder` is already in the diagram (nested class). This is just a convenience wrapper.

---

### 4. FieldParser ⚠️ **MAYBE - Standalone Parser**

**Purpose:** Standalone JSON parser for field definitions  
**Status:** Utility - may be redundant

**What it does:**
- `parseString(json, id)` - Parse from JSON string
- `parseFile(path)` - Parse from file
- `parseResource(resourcePath)` - Parse from classpath resource
- `parseDirectory(directory)` - Batch parse from directory
- `toJsonString(definition)` - Serialize to JSON
- `writeToFile(definition, path)` - Write to file

**Comparison with FieldLoader:**
- `FieldLoader.parseDefinition(json)` - Already does parsing
- `FieldLoader.loadDefinition(path)` - Already loads from path
- `FieldParser` adds: parseResource, parseDirectory, toJsonString, writeToFile

**Proposal:**
- If `FieldLoader` covers all use cases → FieldParser is redundant (LEGACY)
- If `FieldParser` provides useful utilities (serialization, resource parsing) → Keep as utility

**Question:** Do we need standalone parsing outside of FieldLoader?

---

### 5. FieldEffects ❌ **LIKELY LEGACY**

**Purpose:** Registry for effects associated with field definitions  
**Status:** Uses old effect system

**What it does:**
- Registers `EffectConfig` for field IDs
- Uses `EffectType` enum (damage, heal, push, pull, slow)
- Associates effects with field definitions by ID

**Issues:**
- References `FieldEffect` and `EffectType` which may be old system
- New system might handle effects differently (bindings/triggers?)
- Not in CLASS_DIAGRAM

**Proposal:** Likely LEGACY - Move to _reference_code/ unless effects are still used

---

### 6. PresetRegistry ❌ **LIKELY LEGACY**

**Purpose:** Registry for reusable layer/primitive presets  
**Status:** May be replaced by JSON $ref system

**What it does:**
- Stores `FieldLayer` and `Primitive` presets by ID
- References `SpherePrimitive_old` and `RingPrimitive_old` (LEGACY!)
- Provides `getLayer(id)`, `getPrimitive(id)`

**Issues:**
- References `_old` classes (definitely legacy)
- New system uses JSON `$ref` for reusable configs (CLASS_DIAGRAM §12)
- PresetRegistry is programmatic, JSON $ref is declarative

**Proposal:** LEGACY - Replaced by JSON reference system

---

## Recommendations

### ✅ Add to CLASS_DIAGRAM:
1. **FieldSystemInit** - Add as §21 "System Initialization" (or extend §15)

### ⚠️ Keep as Utilities (Not in Diagram):
2. **FieldProfileStore** - User profile management (if user profiles are supported)
3. **FieldBuilder** - Convenience wrapper (optional, not core)
4. **FieldParser** - Standalone parser utilities (if needed beyond FieldLoader)

### ❌ Move to _reference_code/:
5. **FieldEffects** - Old effect system (likely replaced by bindings/triggers)
6. **PresetRegistry** - Replaced by JSON $ref system, references _old classes

---

## Proposed CLASS_DIAGRAM Addition

Add new section after §15:

### 21. System Initialization

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    SYSTEM INITIALIZATION                                    │
│                    Package: field                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                  FieldSystemInit                                     │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ + init(): void              ← Call during mod initialization         │   │
│  │ + isInitialized(): boolean  ← Check initialization status           │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  Initialization Steps:                                                      │
│  1. Register network payloads (FieldSpawnPayload, FieldRemovePayload, etc.)│
│  2. Register commands (/field, /fieldtest)                                │
│  3. Register resource reload listener for JSON definitions                │
│  4. Initialize FieldLoader                                                 │
│  5. Wire FieldManager ↔ FieldNetworking for automatic sync                 │
│  6. Register player join handler for definition/instance sync             │
│  7. Load built-in color themes                                             │
│  8. Register default field definitions                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

**Next Steps:**
1. Review if FieldProfileStore, FieldBuilder, FieldParser should be kept
2. Confirm FieldEffects and PresetRegistry are legacy
3. Add FieldSystemInit to CLASS_DIAGRAM if approved

