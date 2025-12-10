#!/usr/bin/env python3
"""
Update GUI documentation to add preset/profile category system.
Updates: GUI_CLASS_DIAGRAM.md, GUI_ARCHITECTURE.md, GUI_DESIGN.md
"""

from pathlib import Path

DOCS_DIR = Path("docs/field-system/NEW_REFACTORING_NEW_PHASES/_design/gui")

# =============================================================================
# GUI_CLASS_DIAGRAM.md UPDATES
# =============================================================================

CLASS_DIAGRAM_ADDITIONS = '''
---

## 13. Category & Organization Enums

### 13.1 PresetCategory

```java
/**
 * Categories for organizing presets in the GUI.
 * Used for two-tier dropdown: [Category ▼] [Preset ▼]
 */
public enum PresetCategory {
    ADDITIVE("Additive", "Add elements to field"),      // Add rings, layers, etc.
    STYLE("Style", "Visual style changes"),             // Wireframe, solid, etc.
    ANIMATION("Animation", "Motion effects"),           // Spin, pulse, etc.
    EFFECT("Effect", "Composite effects"),              // Combat ready, stealth, etc.
    PERFORMANCE("Performance", "Detail level changes"); // Low/high detail

    private final String displayName;
    private final String description;
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
```

### 13.2 ProfileCategory

```java
/**
 * Categories for organizing profiles in the GUI.
 * Used for filtering in Profiles tab.
 */
public enum ProfileCategory {
    COMBAT("Combat", "For battle situations"),
    UTILITY("Utility", "Functional/practical"),
    DECORATIVE("Decorative", "Visual only"),
    EXPERIMENTAL("Experimental", "Testing/WIP");

    private final String displayName;
    private final String description;
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
```

### 13.3 ProfileSource

```java
/**
 * Source/origin of a profile.
 * Determines editability and storage location.
 */
public enum ProfileSource {
    BUNDLED("Bundled", false),   // Shipped with mod, read-only
    LOCAL("Local", true),        // User-created, editable
    SERVER("Server", false);     // From server, read-only

    private final String displayName;
    private final boolean editable;
    
    public String getDisplayName() { return displayName; }
    public boolean isEditable() { return editable; }
}
```

---

## 14. Updated Profile Record

```java
public record Profile(
    int version,                  // Schema version
    String name,                  // Profile name
    String description,           // User description
    FieldType type,               // Functional type (SHIELD, PERSONAL, etc.)
    ProfileCategory category,     // Organizational category
    List<String> tags,            // Additional tags for filtering
    ProfileSource source,         // Where it came from
    Instant created,              // Creation timestamp
    Instant modified,             // Last modified
    FieldDefinition definition
) {
    public static Profile fromJson(JsonObject json);
    public JsonObject toJson();
    
    /** Display format: "Profile Name (category)" */
    public String getDisplayName() {
        return name + " (" + category.getDisplayName().toLowerCase() + ")";
    }
}
```

---

## 15. Updated PresetRegistry

```java
/**
 * Registry for multi-scope presets organized by category.
 * Loads from: config/the-virus-block/field_presets/{category}/
 */
public class PresetRegistry {
    
    private static final Map<PresetCategory, List<PresetEntry>> presetsByCategory = new EnumMap<>();
    
    public record PresetEntry(
        String id,
        String name,
        String description,
        PresetCategory category,
        JsonObject mergeData
    ) {}
    
    /** Load all presets from disk, organized by category folders */
    public static void loadAll();
    
    /** Get all categories that have presets */
    public static List<PresetCategory> getCategories();
    
    /** Get presets for a specific category */
    public static List<PresetEntry> getPresets(PresetCategory category);
    
    /** Apply a preset to the current state (merges, doesn't replace) */
    public static void applyPreset(FieldEditState state, String presetId);
    
    /** Get affected categories for confirmation dialog */
    public static List<String> getAffectedCategories(String presetId);
}
```

---

## 16. Updated BottomActionBar

```java
/**
 * Global bottom action bar (hidden on Profiles tab).
 * Now includes two-tier preset selection.
 */
public class BottomActionBar {
    
    // Preset selection (two-tier)
    private CyclingButtonWidget<PresetCategory> presetCategoryDropdown;
    private CyclingButtonWidget<String> presetDropdown;
    
    // Profile selection
    private CyclingButtonWidget<String> profileDropdown;
    private ButtonWidget saveButton;
    private ButtonWidget revertButton;
    
    /** Update preset dropdown when category changes */
    private void onPresetCategoryChanged(PresetCategory category);
    
    /** Show confirmation dialog when preset selected */
    private void onPresetSelected(String presetId);
    
    /** Update button states based on dirty status */
    private void updateButtonStates();
}
```

**Bottom Action Bar Layout:**
```
┌────────────────────────────────────────────────────────────────────────┐
│ PRESETS                           │ PROFILE                            │
│ [Additive ▼] [Add Inner Ring ▼]   │ [My Shield (combat) ▼] [SAVE][REV] │
│  ↑ Category    ↑ Preset           │  ↑ Name (category)                 │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 17. Updated ProfilesPanel

```java
/**
 * Full profile management panel with filtering.
 */
public class ProfilesPanel {
    
    // Filters
    private CyclingButtonWidget<ProfileSource> sourceFilter;    // All, Bundled, Local, Server
    private CyclingButtonWidget<ProfileCategory> categoryFilter; // All, Combat, Utility, etc.
    private TextFieldWidget searchField;
    
    // Profile list
    private List<Profile> allProfiles;
    private List<Profile> filteredProfiles;
    private int selectedIndex;
    
    /** Apply filters and update visible list */
    private void applyFilters();
    
    /** Render profile entry with format: "Name (category)" */
    private void renderProfileEntry(Profile profile, int y, boolean selected);
    
    /** Get icon for source (🔒 for read-only, ✎ for editable) */
    private String getSourceIcon(ProfileSource source);
}
```

**Profiles Panel Layout:**
```
┌─────────────────────────────────────────────────────────────────┐
│ Source: [All      ▼]    Category: [All      ▼]    [🔍 ______]  │
├─────────────────────────────────────────────────────────────────┤
│ ── BUNDLED ──                                                   │
│   ○ Default Shield (utility)                                    │
│   ○ Showcase Animated (decorative)                              │
│ ── LOCAL ──                                                     │
│   ● My Combat Shield (combat) ✎                                 │
│   ○ Test Wireframe (experimental) ✎                             │
│ ── SERVER ──                                                    │
│   ○ Server Default (utility) 🔒                                 │
├─────────────────────────────────────────────────────────────────┤
│ [Load] [Save] [Save As] [Rename] [Duplicate] [Delete] [Export]  │
└─────────────────────────────────────────────────────────────────┘
```

'''

def update_class_diagram():
    filepath = DOCS_DIR / "GUI_CLASS_DIAGRAM.md"
    content = filepath.read_text(encoding='utf-8')
    
    # Find the end marker and insert before it
    end_marker = "*Draft v1 - Maps to 03_PARAMETERS.md v5.1*"
    if end_marker in content:
        content = content.replace(end_marker, CLASS_DIAGRAM_ADDITIONS + "\n" + end_marker)
    else:
        # Just append
        content += CLASS_DIAGRAM_ADDITIONS
    
    # Update the class count summary
    old_count = "| **Total** | **~47** | |"
    new_count = "| **Total** | **~50** | (+3 enums, updated Profile, PresetRegistry, BottomActionBar, ProfilesPanel) |"
    content = content.replace(old_count, new_count)
    
    # Update date
    content = content.replace(
        "> **Updated:** December 9, 2024",
        "> **Updated:** December 9, 2024 (Added category system)"
    )
    
    filepath.write_text(content, encoding='utf-8')
    print(f"✅ Updated {filepath.name}")


# =============================================================================
# GUI_ARCHITECTURE.md UPDATES
# =============================================================================

ARCHITECTURE_TERMINOLOGY_UPDATE = '''
### 1.3 Category System

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          CATEGORY SYSTEM                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  PRESET CATEGORIES (for two-tier dropdown)                                  │
│  ─────────────────────────────────────────                                  │
│                                                                             │
│    ADDITIVE     │ Add elements (rings, layers, beams)                       │
│    STYLE        │ Visual style changes (wireframe, glow)                    │
│    ANIMATION    │ Motion effects (spin, pulse, wobble)                      │
│    EFFECT       │ Composite presets (combat ready, stealth)                 │
│    PERFORMANCE  │ Detail level changes (low/high poly)                      │
│                                                                             │
│  PROFILE CATEGORIES (for filtering)                                         │
│  ──────────────────────────────────                                         │
│                                                                             │
│    COMBAT       │ Battle-focused configurations                             │
│    UTILITY      │ Functional/practical setups                               │
│    DECORATIVE   │ Pure visual/aesthetic                                     │
│    EXPERIMENTAL │ Testing/work-in-progress                                  │
│                                                                             │
│  PROFILE SOURCES (determines editability)                                   │
│  ────────────────────────────────────────                                   │
│                                                                             │
│    BUNDLED      │ Shipped with mod      │ Read-only                         │
│    LOCAL        │ User-created          │ Editable                          │
│    SERVER       │ From server           │ Read-only                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.4 Folder Structure

```
config/the-virus-block/
├── field_presets/
│   ├── additive/
│   │   ├── add_inner_ring.json
│   │   ├── add_outer_ring.json
│   │   └── add_halo.json
│   ├── style/
│   │   ├── wireframe.json
│   │   └── solid_glow.json
│   ├── animation/
│   │   ├── slow_spin.json
│   │   └── pulse_beat.json
│   ├── effect/
│   │   ├── combat_ready.json
│   │   └── stealth_mode.json
│   └── performance/
│       ├── low_detail.json
│       └── high_detail.json
│
├── field_profiles/
│   └── local/                    ← User-created profiles
│       └── my_shield.json
│
└── field_*/                      ← Fragments (existing)
    ├── field_shapes/
    ├── field_fills/
    └── ...
```

### 1.5 JSON Metadata

**Preset JSON:**
```json
{
  "name": "Add Inner Ring",
  "category": "additive",
  "description": "Adds a glowing ring inside the main shape",
  "hint": "Great for layered shields",
  "merge": {
    "layers[0].primitives": [
      {
        "$append": true,
        "id": "inner_ring",
        "type": "ring",
        "shape": { "innerRadius": 0.75, "outerRadius": 0.8 }
      }
    ]
  }
}
```

**Profile JSON:**
```json
{
  "id": "my_combat_shield",
  "name": "My Combat Shield",
  "type": "SHIELD",
  "category": "combat",
  "tags": ["animated", "glow", "multilayer"],
  "description": "Red pulsing shield for PvP",
  "layers": [...]
}
```

'''

def update_architecture():
    filepath = DOCS_DIR / "GUI_ARCHITECTURE.md"
    content = filepath.read_text(encoding='utf-8')
    
    # Insert after Section 1.2 Terminology (find the --- after it)
    # Look for "---\n\n## 2. Access Levels"
    marker = "---\n\n## 2. Access Levels"
    if marker in content:
        content = content.replace(marker, ARCHITECTURE_TERMINOLOGY_UPDATE + "\n" + marker)
    
    # Update date
    content = content.replace(
        "> **Updated:** December 9, 2024",
        "> **Updated:** December 9, 2024 (Added category system)"
    )
    
    filepath.write_text(content, encoding='utf-8')
    print(f"✅ Updated {filepath.name}")


# =============================================================================
# GUI_DESIGN.md UPDATES
# =============================================================================

DESIGN_BOTTOM_BAR_UPDATE = '''
## 12. Global Bottom Action Bar

The bottom action bar appears on all tabs EXCEPT the Profiles tab.

```
┌────────────────────────────────────────────────────────────────────────────┐
│                          BOTTOM ACTION BAR                                  │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  ┌─ PRESETS (Two-Tier) ────────────────┐  ┌─ PROFILE ──────────────────┐  │
│  │                                     │  │                            │  │
│  │  [Additive      ▼] [Add Ring    ▼]  │  │  [My Shield (combat)   ▼]  │  │
│  │   ↑ Category        ↑ Preset        │  │   ↑ Name (category)        │  │
│  │                                     │  │                            │  │
│  │  Categories:                        │  │  [SAVE]  [REVERT]          │  │
│  │  • Additive - Add elements          │  │   ↑        ↑               │  │
│  │  • Style - Visual changes           │  │  Enabled   Enabled when    │  │
│  │  • Animation - Motion effects       │  │  when      dirty           │  │
│  │  • Effect - Composite presets       │  │  dirty                     │  │
│  │  • Performance - Detail levels      │  │                            │  │
│  │                                     │  │  Note: SAVE becomes        │  │
│  └─────────────────────────────────────┘  │  "Save As" for server      │  │
│                                           │  profiles                  │  │
│                                           └────────────────────────────┘  │
│                                                                            │
│  Preset Selection Flow:                                                    │
│  1. User selects category → Preset dropdown updates                        │
│  2. User selects preset → Confirmation dialog appears                      │
│  3. Dialog shows: Name, Description, Affected categories                   │
│  4. User confirms → Preset merges into current state                       │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 13. Profiles Tab (Updated)

The Profiles tab has its own full management UI with filtering.

```
┌────────────────────────────────────────────────────────────────────────────┐
│                           PROFILES TAB                                      │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  ┌─ FILTERS ───────────────────────────────────────────────────────────┐  │
│  │                                                                      │  │
│  │  Source: [All      ▼]    Category: [All        ▼]    [🔍 search...] │  │
│  │           ├─ All                    ├─ All                          │  │
│  │           ├─ Bundled                ├─ Combat                       │  │
│  │           ├─ Local                  ├─ Utility                      │  │
│  │           └─ Server                 ├─ Decorative                   │  │
│  │                                     └─ Experimental                 │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
│  ┌─ PROFILE LIST ──────────────────────────────────────────────────────┐  │
│  │                                                                      │  │
│  │  ── BUNDLED ──                                                       │  │
│  │    ○ Default Shield (utility)                                        │  │
│  │    ○ Showcase Animated (decorative)                                  │  │
│  │    ○ Showcase Layered (decorative)                                   │  │
│  │                                                                      │  │
│  │  ── LOCAL ──                                                         │  │
│  │    ● My Combat Shield (combat) ✎                    ← Selected       │  │
│  │    ○ Test Wireframe (experimental) ✎                                 │  │
│  │    ○ Stealth Mode (utility) ✎                                        │  │
│  │                                                                      │  │
│  │  ── SERVER ──                                                        │  │
│  │    ○ Server Default (utility) 🔒                                     │  │
│  │    ○ PvP Arena Shield (combat) 🔒                                    │  │
│  │                                                                      │  │
│  │  Legend: ✎ = editable (local)  🔒 = read-only (bundled/server)       │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
│  ┌─ FRAGMENT SUMMARY (for selected profile) ───────────────────────────┐  │
│  │                                                                      │  │
│  │  Shape:       sphere_highpoly      Animation:   pulse_beat          │  │
│  │  Fill:        wireframe_thin       Prediction:  CUSTOM              │  │
│  │  Visibility:  bands_animated       Follow:      smooth              │  │
│  │  Arrangement: segment_alternating  Beam:        None                │  │
│  │                                                                      │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
│  ┌─ ACTIONS ───────────────────────────────────────────────────────────┐  │
│  │                                                                      │  │
│  │  [Load] [Save] [Save As] [Rename] [Duplicate] [Delete]              │  │
│  │  [Import JSON] [Export JSON] [Set Default]                          │  │
│  │                                                                      │  │
│  │  Button States:                                                      │  │
│  │  • Save: Enabled for local profiles when dirty                      │  │
│  │  • Delete/Rename: Disabled for bundled/server                       │  │
│  │  • Save As: Always enabled (creates local copy)                     │  │
│  │                                                                      │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
│  Status: ● Unsaved changes                                                 │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 14. Category Descriptions

### Preset Categories

| Category | Icon | Description | Example Presets |
|----------|------|-------------|-----------------|
| **Additive** | ➕ | Add new elements to field | Add Inner Ring, Add Halo, Add Beacon |
| **Style** | 🎨 | Change visual appearance | Wireframe, Solid Glow, Hologram |
| **Animation** | 🔄 | Add/modify motion | Slow Spin, Pulse Beat, Wobble |
| **Effect** | ✨ | Composite transformations | Combat Ready, Stealth Mode, Power Surge |
| **Performance** | ⚡ | Adjust detail level | Low Detail, High Detail, Ultra |

### Profile Categories

| Category | Icon | Description | Use Case |
|----------|------|-------------|----------|
| **Combat** | ⚔️ | Battle-focused | PvP shields, damage indicators |
| **Utility** | 🔧 | Functional/practical | Navigation aids, status displays |
| **Decorative** | 🌟 | Pure aesthetics | Fashion, roleplay, screenshots |
| **Experimental** | 🧪 | Testing/WIP | New designs, performance testing |

'''

def update_design():
    filepath = DOCS_DIR / "GUI_DESIGN.md"
    content = filepath.read_text(encoding='utf-8')
    
    # Find "## 12." or end of document to insert
    # Look for the summary section or end
    if "## Summary" in content:
        content = content.replace("## Summary", DESIGN_BOTTOM_BAR_UPDATE + "\n## Summary")
    elif "## Remaining TODO" in content:
        content = content.replace("## Remaining TODO", DESIGN_BOTTOM_BAR_UPDATE + "\n## Remaining TODO")
    else:
        # Append before the last ---
        content = content.rstrip() + "\n\n" + DESIGN_BOTTOM_BAR_UPDATE
    
    # Update date
    content = content.replace(
        "> **Updated:** December 9, 2024",
        "> **Updated:** December 9, 2024 (Added category system)"
    )
    
    filepath.write_text(content, encoding='utf-8')
    print(f"✅ Updated {filepath.name}")


# =============================================================================
# MAIN
# =============================================================================

def main():
    print("=" * 60)
    print("UPDATING GUI DOCUMENTATION - Category System")
    print("=" * 60)
    
    print("\n1. Updating GUI_CLASS_DIAGRAM.md...")
    update_class_diagram()
    
    print("\n2. Updating GUI_ARCHITECTURE.md...")
    update_architecture()
    
    print("\n3. Updating GUI_DESIGN.md...")
    update_design()
    
    print("\n" + "=" * 60)
    print("✅ All documentation updated!")
    print("=" * 60)


if __name__ == "__main__":
    main()

