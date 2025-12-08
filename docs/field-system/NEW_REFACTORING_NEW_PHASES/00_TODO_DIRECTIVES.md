# TODO Directives - Working Guide

> **⚠️ READ THIS BEFORE EVERY TODO**  
> **Purpose:** Context restoration and quality control between tasks  
> **Status:** Active Implementation Guide

---

## 🧭 Quick Navigation

### Core Documents (Read Before Each TODO)

| Priority | Document | Purpose | When to Read |
|----------|----------|---------|--------------|
| 🔴 1 | [01_ARCHITECTURE.md](./01_ARCHITECTURE.md) | 5 Geometry Levels, Transform, JSON structure | Every time |
| 🔴 2 | [02_CLASS_DIAGRAM.md](./02_CLASS_DIAGRAM.md) | Classes, enums, records to create | Every time | 
| 🟡 3 | [03_PARAMETERS.md](./03_PARAMETERS.md) | All parameters with defaults | When adding fields |
| 🟡 4 | [04_SHAPE_MATRIX.md](./04_SHAPE_MATRIX.md) | Per-shape parameter status | When touching shapes |

_The documents above can be updated when necessary but only when agreed with the user._

### Quick Reference Tables

| What You Need | Where to Find It |
|---------------|------------------|
| What enums to create | CLASS_DIAGRAM §16-17 |
| What records to create | CLASS_DIAGRAM §18-19 |
| Shape parameter details | SHAPE_PARAMETER_MATRIX §1-9 |
| Transform options | ARCHITECTURE §3 |
| Fill/Visibility options | ARCHITECTURE §4-5 |
| JSON structure | ARCHITECTURE §6 |
| Resolved questions | ARCHITECTURE §10 |
| **CommandKnob usage** | [SYSTEM_UTILITIES.md](./_design/SYSTEM_UTILITIES.md) §1 |
| **Logging patterns** | [SYSTEM_UTILITIES.md](./_design/SYSTEM_UTILITIES.md) §2 |
| **CommandFeedback** | [SYSTEM_UTILITIES.md](./_design/SYSTEM_UTILITIES.md) §3 |
| **Value validation** | CLASS_DIAGRAM §18.5 (ValueRange, @Range) |
| **GUI widget factory** | [GUI_UTILITIES.md](./_design/gui/GUI_UTILITIES.md) §4 |
| **GUI constants/theming** | [GUI_UTILITIES.md](./_design/gui/GUI_UTILITIES.md) §3 |
| **Minecraft GUI widgets** | [GUI_NATIVE_WIDGETS.md](./_design/gui/GUI_NATIVE_WIDGETS.md) |


### Scripts Reference (Development Tools)

| # | Script | Purpose | Status |
|---|--------|---------|--------|
| 00 | `search_native_jars.py` | Bytecode search in JARs | Updated |
| 01 | `query_tiny_mappings.py` | Fabric mapping lookup | Updated |
| 02 | `update_todo_batch.py` | TODO automation | ✅ Stable |
| 03 | `usage_graph.py` | Find symbol references | Untested |
| 04 | `class_tree.py` | Inheritance hierarchy | Untested |
| 05 | `method_signature_scanner.py` | Find methods by descriptor | Untested |
| 06 | `mod_interface_map.py` | Map entrypoints/mixins | Untested |
| 07 | `config_schema_extractor.py` | Generate JSON Schema | Updated |
| 08 | `config_profile_lint.py` | Validate profile JSONs | Untested |

**Full documentation:** [`scripts/000_INSTRUCTIONS_README.md`](../../../scripts/000_INSTRUCTIONS_README.md)

---

## 📜 System Context

### The Old vs New Story

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SYSTEM EVOLUTION                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  LEGACY SYSTEM (Still in Codebase)                                         │
│  ─────────────────────────────────                                         │
│  • Shield/Void Tear/Singularity visuals                                    │
│  • Working but rigid: one class per visual type                            │
│  • Files in: _legacy/ folders with _old suffix                             │
│  • Status: Archived, will be removed after migration                       │
│                                                                             │
│  PARTIAL NEW SYSTEM (Exists but Incomplete)                                │
│  ──────────────────────────────────────────                                │
│  • Field definitions, layers, primitives structure                         │
│  • JSON loading, network sync, command system                              │
│  • Problems: Confusing hierarchy, missing parameters, broken patterns      │
│  • Status: Being restructured, NOT production-ready                        │
│                                                                             │
│  TARGET SYSTEM (What We're Building)                                       │
│  ───────────────────────────────────                                       │
│  • 5 Geometry Levels: Shape → CellType → Arrangement → Visibility → Fill  │
│  • Flat primitive hierarchy (no SolidPrimitive/BandPrimitive)              │
│  • Complete Transform system (anchors, facing, billboard)                  │
│  • All appearance/animation fields implemented                             │
│  • Status: Documented, ready for implementation                            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Why This Matters for Each TODO

When implementing a todo:
1. **Check if old code exists** → May need to adapt, not create from scratch
2. **Check if partial new code exists** → May need to refactor, not create
3. **Always match the TARGET architecture** → Don't copy old patterns

---

## 📋 Document Logic Guide

### What Each Document Contains

| Document | Logic | Read When |
|----------|-------|-----------|
| **ARCHITECTURE_PROPOSAL** | The "why" and "how" - explains the 5 levels, transform system, compatibility matrices | Starting any feature |
| **CLASS_DIAGRAM_PROPOSED** | The "what" - exact classes, fields, methods to implement | Writing code |
| **PARAMETER_INVENTORY** | The "all" - every parameter at every level with defaults and status | Adding/checking fields |
| **SHAPE_PARAMETER_MATRIX** | The "deep" - every parameter per shape type | Working on shapes |
| **CLEANUP_PLAN** | The "before" - files to rename/move before implementing | Before Phase 1 |
| **GUI docs** | Phase 2 - See [_design/gui/](./_design/gui/) folder | Phase 2 (GUI work) |

### Document Flow

```
START TODO
    │
    ├─→ Read ARCHITECTURE (understand the "why")
    │
    ├─→ Read CLASS_DIAGRAM (understand the "what")
    │
    ├─→ Check PARAMETER_INVENTORY (get field details)
    │
    ├─→ If shape-related: Read SHAPE_PARAMETER_MATRIX
    │
    ├─→ Write code matching TARGET architecture
    │
    ├─→ Test against existing profiles
    │
    └─→ Update documents if needed
         │
         END TODO
```

---

## 🔄 Timeline & Review Mechanism

### Before Starting a TODO

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  QUICK REVIEW: Previous 5 Tasks                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  For each of the last 5 completed TODOs, verify:                           │
│                                                                             │
│  □ Does it match ARCHITECTURE_PROPOSAL structure?                          │
│  □ Does it match CLASS_DIAGRAM_PROPOSED interfaces?                        │
│  □ Are all parameters from PARAMETER_INVENTORY included?                   │
│  □ Did we update any documentation based on discoveries?                   │
│  □ Did we add any new TODO based on discoveries?                          │
│                                                                             │
│  If ANY checkbox fails → Fix before continuing                             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Current Task Context

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  CURRENT TODO CONTEXT                                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  TODO ID: ________                                                          │
│  Description: ________________________________________________              │
│                                                                             │
│  Relevant Architecture Section: _______                                     │
│  Relevant Class Diagram Section: _______                                    │
│  Relevant Parameters: _______                                               │
│                                                                             │
│  Depends on (previous TODOs that must be done):                            │
│  • ________                                                                 │
│  • ________                                                                 │
│                                                                             │
│  Blocks (future TODOs that depend on this):                                │
│  • ________                                                                 │
│  • ________                                                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### After Completing a TODO

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  COMPLETION CHECKLIST                                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  □ Code compiles with `./gradlew compileJava`                              │
│  □ Code matches CLASS_DIAGRAM structure                                    │
│  □ All fields from PARAMETER_INVENTORY are present                         │
│  □ Default values match PARAMETER_INVENTORY                                │
│  □ Comments/Javadoc added                                                  │
│  □ Logging added where appropriate                                         │
│                                                                             │
│  If discoveries made:                                                       │
│  □ Updated PARAMETER_INVENTORY with new info                               │
│  □ Updated CLASS_DIAGRAM if interface changed                              │
│  □ Updated ARCHITECTURE if behavior changed                                │
│  □ Created new TODO for follow-up work                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📊 Implementation Phases

### Phase 1: Core Restructure (Current)
| Category | Focus | Key Documents |
|----------|-------|---------------|
| Enums | CellType, Anchor, Facing, etc. | CLASS_DIAGRAM §16-17 |
| Records | FillConfig, VisibilityMask, etc. | CLASS_DIAGRAM §18 |
| Transform | Complete rewrite | ARCHITECTURE §3 |
| Primitives | Flatten hierarchy | ARCHITECTURE §5 |
| JSON | Reference system | ARCHITECTURE §6, PARAMETER_INVENTORY §14-15 |

### Phase 2: GUI & Polish

> **📁 GUI Documentation:** [_design/gui/](`./_design/gui/`)  
> **GUI Directives:** [GUI_TODO_DIRECTIVES.md](./_design/gui/GUI_TODO_DIRECTIVES.md)

| Category | Focus | Key Documents |
|----------|-------|---------------|
| Foundation | Screen, state, utilities | GUI_CLASS_DIAGRAM §2, GUI_UTILITIES |
| Quick Panel | Level 1 controls | GUI_ARCHITECTURE §2.1 |
| Advanced Panel | Level 2 controls | GUI_CLASS_DIAGRAM §4 |
| Debug Panel | Level 3 (operators) | GUI_ARCHITECTURE §2.1 L3 |
| Profiles | Save/load system | GUI_CLASS_DIAGRAM §8 |
| Network | Packets, sync | GUI_CLASS_DIAGRAM §7 |

**151 tasks across 15 batches** - See [GUI_TODO_LIST.md](./_design/gui/GUI_TODO_LIST.md)

### Phase 3: Advanced Features
- Primitive linking
- Orbit/dynamic positioning
- Pattern animation

### Phase 4: New Shapes
- Torus, Cone, Helix

---

## 🏗️ Design Patterns & OOP

**Moved to:** [_design/DESIGN_PATTERNS.md](./_design/DESIGN_PATTERNS.md)

Covers: Builder, Factory, Strategy patterns, ValueRange, Immutability, Composition, Anti-patterns

## 🛠️ Development Practices

### Batch Operations
- **Work in batches** - Group related changes together
- **Use Python scripts** for bulk file modifications (never heredocs in WSL)
- Python script workflow: 1) Write script 2) Run with `python3` 3) Compile 4) Iterate

```python
#!/usr/bin/env python3
import os

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    # Make changes with content.replace()
    content = content.replace('old', 'new')
    with open(filepath, 'w') as f:
        f.write(content)

# Process files
for root, dirs, files in os.walk('src/main/java'):
    for f in files:
        if f.endswith('.java'):
            process_file(os.path.join(root, f))
```

---

## ⚠️ Common Mistakes to Avoid

| Mistake | Why It's Wrong | Correct Approach |
|---------|----------------|------------------|
| Copying old primitive structure | Creates same hierarchy problems | Use flat Primitive interface |
| Using `StripesPrimitive` | Should be `visibility.mask=STRIPES` | Configure, don't subclass |
| Using `CagePrimitive` | Should be `fill.mode=CAGE` | Configure, don't subclass |
| Hard-coding defaults | Makes JSON incomplete | Read from JSON with fallbacks |
| Skipping `id` field | Breaks linking and debugging | Always require primitive `id` |
| Mixing snake_case/camelCase | Inconsistent JSON | Use camelCase everywhere |
| Editing files one at a time | Slow and error-prone | Use Python scripts for batch changes |

---

## 🔗 Quick Links

### Primary Documents
- [01_ARCHITECTURE.md](./01_ARCHITECTURE.md) - Complete restructure plan
- [02_CLASS_DIAGRAM.md](./02_CLASS_DIAGRAM.md) - Target classes
- [03_PARAMETERS.md](./03_PARAMETERS.md) - All parameters
- [04_SHAPE_MATRIX.md](./04_SHAPE_MATRIX.md) - Per-shape details
- [GAP.md](./GAP.md) - Missing features (bindings, triggers)

### GUI Documents (Phase 2)
- [GUI_TODO_DIRECTIVES.md](./_design/gui/GUI_TODO_DIRECTIVES.md) - **GUI working guide**
- [GUI_TODO_LIST.md](./_design/gui/GUI_TODO_LIST.md) - 151 tasks in 15 batches
- [GUI_ARCHITECTURE.md](./_design/gui/GUI_ARCHITECTURE.md) - Design & flow
- [GUI_CLASS_DIAGRAM.md](./_design/gui/GUI_CLASS_DIAGRAM.md) - GUI classes
- [GUI_UTILITIES.md](./_design/gui/GUI_UTILITIES.md) - Factory & theming
- [GUI_NATIVE_WIDGETS.md](./_design/gui/GUI_NATIVE_WIDGETS.md) - MC widgets

### Supporting Documents
- [README.md](./README.md) - Entry point
- [_design/CLEANUP_PLAN.md](./_design/CLEANUP_PLAN.md) - Pre-implementation cleanup

- [_design/DESIGN_PATTERNS.md](./_design/DESIGN_PATTERNS.md) - OOP patterns & principles
- [_design/CODE_QUALITY.md](./_design/CODE_QUALITY.md) - Commenting & logging standards
- [README.md](./README.md) - Entry point
- [_design/CLEANUP_PLAN.md](./_design/CLEANUP_PLAN.md) - Pre-implementation cleanup

### Review History
- [_reviews/CRITICAL_REVIEW_V1.md](./_reviews/CRITICAL_REVIEW_V1.md) - First review
- [_reviews/CRITICAL_REVIEW_V2.md](./_reviews/CRITICAL_REVIEW_V2.md) - Second review
- [_reviews/CRITICAL_REVIEW_V3.md](./_reviews/CRITICAL_REVIEW_V3.md) - Third review

---

## 📝 Template: TODO Work Session

```markdown
## Session Start
- Date: YYYY-MM-DD
- TODOs planned: [F01, F02, F03...]

## Quick Review (Last 5)
- [x] F00: Verified against architecture
- [x] F-1: Verified against architecture
- ...

## Current TODO: [ID]
- Architecture section: X
- Class diagram section: Y
- Parameters: [list]

## Implementation Notes
- ...

## Discoveries
- ...

## Documentation Updates
- Updated X because...

## Session End
- Completed: [F01, F02]
- Blocked: [F03] - reason
- New TODOs: [Fnew1, Fnew2]
```

---

*This document is the "home base" during implementation. Return here between each TODO.*


---

## 📝 Code Quality Standards

**Moved to:** [_design/CODE_QUALITY.md](./_design/CODE_QUALITY.md)

Covers: Javadoc, Comments, Logging standards, NOTE patterns, System utilities integration
