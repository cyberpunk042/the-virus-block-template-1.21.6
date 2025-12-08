# File Review Summary - December 8, 2024

> **Purpose:** Determine which restored files are ACTIVE vs LEGACY  
> **Based on:** CLASS_DIAGRAM.md and current codebase

---

## ✅ ACTIVE - Keep in src/ (1 file)

1. **`src/main/java/net/cyberpunk042/field/FieldManager.java`**
   - ✓ In CLASS_DIAGRAM §9 (Server-Side Management)
   - **Action:** KEEP

---

## ❌ LEGACY - Move to _reference_code/ (3 files)

### Animation Files (Replaced by Config records)

1. **`src/main/java/net/cyberpunk042/visual/animation/Pulse.java`**
   - ❌ NOT in CLASS_DIAGRAM (shows `PulseConfig` instead)
   - OBSERVATIONS.md notes: "Spin.java, Pulse.java redundant - old runtime classes, configs now used directly"
   - **Action:** LEGACY - Move to _reference_code/

2. **`src/main/java/net/cyberpunk042/visual/animation/Spin.java`**
   - ❌ NOT in CLASS_DIAGRAM (shows `SpinConfig` instead)
   - OBSERVATIONS.md notes: "Spin.java, Pulse.java redundant - old runtime classes, configs now used directly"
   - **Action:** LEGACY - Move to _reference_code/

3. **`src/main/java/net/cyberpunk042/visual/animation/Animator.java`**
   - ❌ NOT in CLASS_DIAGRAM
   - CLASS_DIAGRAM shows `AnimationApplier` (TODO F69), not `Animator`
   - **Action:** LEGACY - Move to _reference_code/

---

## ⚠️ REVIEW - Command System (12 files)

### Client Commands (6 files)
These are CLIENT-side commands for visual testing:

1. `src/client/java/net/cyberpunk042/client/command/MeshShapeCommand.java`
2. `src/client/java/net/cyberpunk042/client/command/MeshStyleCommand.java`
3. `src/client/java/net/cyberpunk042/client/command/ShieldPersonalCommand.java`
4. `src/client/java/net/cyberpunk042/client/command/ShieldVisualCommand.java`
5. `src/client/java/net/cyberpunk042/client/command/SingularityVisualCommand.java`
6. `src/client/java/net/cyberpunk042/client/command/TriangleTypeCommand.java`

**Current Status:** No equivalent client commands found  
**Action:** REVIEW - Are these still needed for testing, or replaced by server commands?

### Server Commands (6 files)
These are SERVER-side commands:

1. `src/main/java/net/cyberpunk042/command/FieldSubcommands.java`
2. `src/main/java/net/cyberpunk042/command/field/FieldCommand.java`
3. `src/main/java/net/cyberpunk042/command/field/FieldTestCommand.java`
4. `src/main/java/net/cyberpunk042/command/field/FieldTypeProvider.java`
5. `src/main/java/net/cyberpunk042/command/field/FieldTypeProviders.java`
6. `src/main/java/net/cyberpunk042/command/field/ShieldSubcommand.java`

**Current Status:** 
- `FieldTestCommand.java` EXISTS in current codebase
- `PersonalSubcommand.java` EXISTS (may replace ShieldSubcommand)
- Other files not found

**Action:** REVIEW - Check if these are old versions or still needed

---

## ✅ ACTIVE - Keep in src/ (1 file)

1. **`src/main/java/net/cyberpunk042/field/FieldSystemInit.java`**
   - ⚠️ NOT in CLASS_DIAGRAM (utility class, not a core domain class)
   - **BUT:** There's a TODO in TheVirusBlock.java line 227: `// TODO: New field system init - net.cyberpunk042.field.FieldSystemInit.init();`
   - **Status:** ACTIVE - This is the initialization class that needs to be called
   - **Action:** KEEP - This is needed for system initialization

## ⚠️ REVIEW - Utility Classes (5 files)

These are utility/helper classes not shown in CLASS_DIAGRAM (which focuses on core domain model):

1. **`src/main/java/net/cyberpunk042/field/FieldProfileStore.java`**
   - Utility for saving/loading custom field profiles to config directory
   - Not in CLASS_DIAGRAM (utility, not core domain)
   - **Action:** REVIEW - May still be useful for user-created profiles

2. **`src/main/java/net/cyberpunk042/field/definition/FieldBuilder.java`**
   - Convenience facade over `FieldDefinition.Builder` with preset methods
   - Not in CLASS_DIAGRAM (utility, not core domain)
   - `FieldDefinition` has nested `Builder`, but this adds convenience methods
   - **Action:** REVIEW - May be useful convenience class

3. **`src/main/java/net/cyberpunk042/field/definition/FieldParser.java`**
   - Standalone JSON parser for field definitions
   - Not in CLASS_DIAGRAM (utility, not core domain)
   - `FieldLoader.parseDefinition()` already does this
   - **Action:** REVIEW - May be redundant if FieldLoader covers all use cases

4. **`src/main/java/net/cyberpunk042/field/effect/FieldEffects.java`**
   - Registry for effects associated with field definitions
   - Not in CLASS_DIAGRAM (utility, not core domain)
   - **Action:** REVIEW - Check if new system handles effects differently

5. **`src/main/java/net/cyberpunk042/field/registry/PresetRegistry.java`**
   - Registry for reusable field layer/primitive presets
   - Not in CLASS_DIAGRAM (utility, not core domain)
   - New system uses JSON references (`$ref`) instead
   - **Action:** REVIEW - May be legacy if JSON references replace this

## ⚠️ REVIEW - May Still Be Needed (1 file)

1. **`src/main/java/net/cyberpunk042/network/FieldNetworking.java`**
   - ❌ NOT in CLASS_DIAGRAM
   - **Action:** REVIEW - May still be needed for networking (check if used)

---

## 📋 FINAL RECOMMENDATIONS

### ✅ KEEP in src/ (2 files):
- FieldManager.java ✓ (in CLASS_DIAGRAM §9)
- FieldSystemInit.java ✓ (needed for initialization, TODO in TheVirusBlock.java)

### ❌ MOVE to _reference_code/ (3 files):
- Pulse.java (replaced by PulseConfig - OBSERVATIONS.md M8)
- Spin.java (replaced by SpinConfig - OBSERVATIONS.md M8)
- Animator.java (replaced by AnimationApplier - OBSERVATIONS.md M6)

### ⚠️ REVIEW (18 files):
- FieldProfileStore.java (utility - may still be useful)
- FieldBuilder.java (convenience facade - may still be useful)
- FieldParser.java (may be redundant if FieldLoader covers all cases)
- FieldEffects.java (check if new system handles effects differently)
- PresetRegistry.java (may be replaced by JSON $ref system)
- FieldNetworking.java (check if still needed)
- All 12 command files (check if replaced by current command system)

---

**Next Step:** Review each file manually to confirm recommendations.

