# Script Safety Rules - MANDATORY

> **Date:** December 8, 2024  
> **Status:** CRITICAL - Never Violate These Rules

---

## 🚨 ABSOLUTE PROHIBITIONS

### Rule 1: NEVER Operate on `src/` Directly
- **NEVER** create scripts that modify, move, or delete files in `src/` without explicit, narrow targeting
- **NEVER** use wildcards or directory traversal on `src/`
- **NEVER** add "cleanup" or "empty directory removal" to scripts that touch `src/`
- **NEVER** assume a directory is "safe to delete" because it appears empty

### Rule 2: Scripts Must Be Explicitly Targeted
- Scripts MUST operate on explicitly listed files/directories
- Scripts MUST require user confirmation for ANY destructive operation
- Scripts MUST support `--dry-run` mode
- Scripts MUST show exactly what will be affected before execution

### Rule 3: No "Helpful" Cleanup
- **NEVER** add cleanup logic that wasn't explicitly requested
- **NEVER** remove "empty" directories automatically
- **NEVER** assume what the user wants cleaned up

---

## ✅ SAFE SCRIPT PATTERNS

### Pattern 1: Explicit File Lists
```python
# ✅ GOOD - Explicit list
FILES_TO_PROCESS = [
    "src/main/java/net/cyberpunk042/field/_legacy/SomeFile.java",
    "src/main/java/net/cyberpunk042/field/_legacy/AnotherFile.java",
]

# ❌ BAD - Directory traversal
for root, dirs, files in os.walk("src/"):
    # NO - too broad
```

### Pattern 2: User Confirmation Required
```python
# ✅ GOOD - Always confirm
print(f"Will process {len(files)} files:")
for f in files:
    print(f"  - {f}")
response = input("Continue? (yes/no): ")
if response != "yes":
    sys.exit(0)

# ❌ BAD - No confirmation
# Just does it automatically
```

### Pattern 3: Dry-Run Mode
```python
# ✅ GOOD - Dry-run support
if args.dry_run:
    print("DRY RUN - Would process:")
    for f in files:
        print(f"  - {f}")
    sys.exit(0)

# ❌ BAD - No dry-run
```

### Pattern 4: Backup Before Modification
```python
# ✅ GOOD - Backup first
backup_dir = Path("backup") / datetime.now().isoformat()
backup_dir.mkdir(parents=True)
shutil.copy2(file, backup_dir / Path(file).name)
# Then modify

# ❌ BAD - No backup
```

---

## 📋 SCRIPT REVIEW CHECKLIST

Before creating ANY script that touches `src/`:

- [ ] Does it operate on explicitly listed files only?
- [ ] Does it require user confirmation?
- [ ] Does it support `--dry-run` mode?
- [ ] Does it create backups before modifying?
- [ ] Does it show exactly what will be affected?
- [ ] Does it avoid directory traversal/wildcards?
- [ ] Does it avoid "cleanup" logic?
- [ ] Does it avoid deleting directories?

**If ANY answer is NO, the script is UNSAFE and must be redesigned.**

---

## 🎯 TARGETED OPERATIONS ONLY

### Safe Operations:
- ✅ Reading files from `src/` (read-only)
- ✅ Processing explicitly listed files
- ✅ Creating new files in `src/` (with confirmation)
- ✅ Modifying files that are explicitly listed

### Dangerous Operations (Require Extra Care):
- ⚠️ Moving files (must be explicit source + destination)
- ⚠️ Deleting files (must be explicit list, with confirmation)
- ⚠️ Renaming files (must be explicit old + new names)

### NEVER:
- ❌ Directory traversal with deletion
- ❌ "Cleanup empty directories"
- ❌ Wildcard operations on `src/`
- ❌ Assuming what should be deleted

---

## 📝 LESSON LEARNED

**What Happened:**
A script was created to move legacy files from `src/` to `_reference_code/`. The script included a "cleanup empty directories" section that deleted 15 directories from `src/`, including active code directories.

**Why It Was Dangerous:**
1. Operated on entire `src/` directory tree
2. Assumed empty directories were safe to delete
3. No explicit targeting
4. No user confirmation for deletions
5. No dry-run mode

**Result:**
- 15 directories deleted
- 37 files lost (later restored from git)
- Significant time lost in recovery

**Prevention:**
- Never operate on `src/` without explicit targeting
- Never add "helpful cleanup" to scripts
- Always require explicit confirmation
- Always support dry-run mode

---

**Remember: It's better to do nothing than to do something destructive without explicit user intent.**
