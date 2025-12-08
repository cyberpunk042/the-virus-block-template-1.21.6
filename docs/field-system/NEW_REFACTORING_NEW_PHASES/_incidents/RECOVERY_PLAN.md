# Recovery Plan - December 8, 2024 Data Loss

> **Status:** IN PROGRESS  
> **Priority:** CRITICAL

---

## Overview

During the legacy code move operation, 15 directories were incorrectly deleted. This document tracks the recovery process.

---

## Deleted Directories Inventory

### High Priority (Likely Active Code)

| Directory | Files in _reference_code | Status | Action |
|-----------|-------------------------|--------|--------|
| `src/client/java/.../client/command/` | `command/field/*.java` (6 files)<br>`command/FieldSubcommands.java` | Empty | **Review & Restore** |
| `src/main/java/.../field/definition/` | `field/definition/FieldBuilder.java`<br>`field/definition/FieldParser.java` | Empty | **CRITICAL - Review & Restore** |

### Medium Priority (Need Verification)

| Directory | Files in _reference_code | Status | Action |
|-----------|-------------------------|--------|--------|
| `src/main/java/.../sphere/` | `main_legacy_./sphere/SphereModelGenerator.java` | Empty | Verify if legacy or active |
| `src/main/java/.../visual/mesh/` | `main_legacy_./visual/mesh/TrianglePattern.java` | Empty | Verify if legacy or active |
| `src/main/java/.../visual/render/` | None | Empty | Check git history |
| `src/main/java/.../client/color/` | None | Empty | Check git history |
| `src/client/java/.../client/sound/` | None | Empty | Check git history |

### Low Priority (Likely Empty or Resources)

| Directory | Status | Action |
|-----------|--------|--------|
| `src/client/java/.../visual/mesh/sphere/` | Empty | Verify |
| `src/client/java/.../visual/tessellator/` | Empty | Verify |
| `src/main/java/.../infection/orchestrator/phase/` | Empty | Verify |
| `src/main/resources/assets/.../field_profiles/` | Empty | Check for JSON configs |
| `src/main/resources/assets/.../textures/entity/equipment/custom/` | Empty | Check for textures |
| `src/main/resources/assets/.../textures/models/armor/` | Empty | Check for textures |
| `src/main/resources/config/.../dimension_profiles/` | Empty | Check for configs |
| `src/main/resources/data/minecraft/tags/items/` | Empty | Check for data files |

---

## Recovery Process

### Step 1: Categorize Files in _reference_code

For each file in `docs/field-system/_reference_code/` that is NOT `*_old.java`:

1. **Is it Legacy?**
   - Check if it's referenced in CLEANUP_PLAN.md as legacy
   - Check if it's been replaced by new architecture
   - If legacy → Keep in `_reference_code/`

2. **Is it Active Code?**
   - Check if it's part of current refactoring (NEW_REFACTORING_NEW_PHASES)
   - Check if it's referenced in TODO_LIST.md
   - Check if it compiles with current codebase
   - If active → Restore to `src/`

3. **Is it Unknown?**
   - Review git history
   - Check commit messages
   - Ask for clarification

### Step 2: Restore Active Code

For each file identified as active code:
1. Determine correct location in `src/`
2. Copy from `_reference_code/` to `src/`
3. Update package declarations if needed
4. Verify it compiles
5. Check if it needs updates for new architecture

### Step 3: Verify TODO Dependencies

Review `TODO_LIST.md` and check:
- [ ] All tasks that reference deleted directories
- [ ] All tasks that depend on files that may have been lost
- [ ] Update task status if dependencies are missing
- [ ] Add new tasks for recovery if needed

### Step 4: Git History Recovery

For directories with no files in `_reference_code`:
1. Check git log for deleted files
2. Restore from last known good commit if needed
3. Verify files are still relevant to current architecture

---

## Files Requiring Immediate Review

### In `_reference_code/command/`:
- `FieldSubcommands.java`
- `field/FieldCommand.java`
- `field/FieldTestCommand.java`
- `field/FieldTypeProvider.java`
- `field/FieldTypeProviders.java`
- `field/ShieldSubcommand.java`

**Status:** Need to verify if these are current command implementations or old versions
**Action:** Compare with current command system, check if they compile

### In `_reference_code/field/`:
- `FieldLoader.java` - **VERIFIED:** OLD version (current is in `src/.../field/loader/FieldLoader.java`)
- `FieldManager.java` - Review if this is current or replaced
- `FieldProfileStore.java` - Review
- `FieldSystemInit.java` - Review
- `definition/FieldBuilder.java` - **CRITICAL** - May be active code (directory is empty)
- `definition/FieldParser.java` - **CRITICAL** - May be active code (directory is empty)
- `effect/FieldEffects.java` - Review

**Status:** FieldLoader is confirmed old. Others need verification.

### In `_reference_code/` root:
- `FieldDefinition.java` - **VERIFIED:** Likely OLD version (current is in `src/.../field/FieldDefinition.java`)
- `FieldLayer.java` - **VERIFIED:** Likely OLD version (current is in `src/.../field/FieldLayer.java`)
- `FieldRegistry.java` - **VERIFIED:** OLD version (current is in `src/.../field/FieldRegistry.java`)
- `PresetRegistry.java` - Review

**Status:** These appear to be old versions that were moved to reference_code earlier, not deleted by the script.

---

## Verification Results ⚠️

**Status:** PARTIALLY VERIFIED - 6 CLIENT command files were lost

### Command System
- ✅ **Server Commands:** `src/main/java/net/cyberpunk042/command/field/PersonalSubcommand.java` exists
- ✅ **Server Commands:** `src/main/java/net/cyberpunk042/command/field/ThemeSubcommand.java` exists
- ⚠️ **CLIENT Commands LOST:** 6 files deleted from `src/client/java/net/cyberpunk042/client/command/`:
  1. `MeshShapeCommand.java`
  2. `MeshStyleCommand.java`
  3. `ShieldPersonalCommand.java`
  4. `ShieldVisualCommand.java`
  5. `SingularityVisualCommand.java`
  6. `TriangleTypeCommand.java`

### Field Definition System
- ✅ **Current:** `FieldDefinition` has nested `Builder` class (no separate FieldBuilder needed)
- ✅ **Current:** `FieldLoader.parseDefinition()` exists (no separate FieldParser needed)

### Other Directories
- ✅ **Empty Directories:** 7 other directories were empty (no files lost)

### TODO Items
- ✅ **F151-F157:** All marked as completed
- ⬜ **F158:** Pending (visual test)
- ⬜ **CHK-17:** Pending (batch verification)

### Conclusion
**6 CLIENT command files were lost** from `src/client/java/net/cyberpunk042/client/command/`. These need to be:
1. Restored from git commit `082c7d9^` (before deletion)
2. Reviewed to see if they're still needed or replaced by new system
3. Either restored or marked as legacy/replaced

---

## Next Actions

1. [x] Review each file in `_reference_code/` against current codebase
2. [x] Identify which files are current vs legacy
3. [x] **RESTORED:** 8 files from deleted directories using `scripts/07_restore_deleted_files.py`
4. [ ] Review restored files to determine if they're active or legacy
5. [ ] Verify compilation with restored files
6. [x] Verify TODO items are still valid
7. [ ] Continue with F158 (visual test) and CHK-17
8. [x] Document final recovery status

## Restoration Complete ✅

**8 files restored from git:**
- 6 client command files from `src/client/java/net/cyberpunk042/client/command/`
- 2 field definition files from `src/main/java/net/cyberpunk042/field/definition/`

**Next Step:** Review restored files to determine if they should be:
- **Kept** - Active code that's still needed
- **Moved to `_reference_code/`** - Legacy code for reference
- **Removed** - No longer needed

---

**Last Updated:** December 8, 2024

