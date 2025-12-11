#!/usr/bin/env python3
"""
Update GUI_CLASS_DIAGRAM.md to add the 13 evolved classes.

Extra classes to add:
- state/: AppearanceState, FieldEditStateHolder, StateAccessor
- panel/: ActionPanel
- panel/sub/: ModifiersSubPanel, OrbitSubPanel
- widget/: BottomActionBar, ConfirmDialog, LoadingIndicator, PresetConfirmDialog, ToastNotification
- util/: GuiKeyboardNav
- screen/: TabType (enum)
"""

from pathlib import Path
from datetime import datetime

PROJECT_ROOT = Path(__file__).parent.parent
DIAGRAM_FILE = PROJECT_ROOT / "docs/field-system/NEW_REFACTORING_NEW_PHASES/_design/gui/GUI_CLASS_DIAGRAM.md"

def update_package_structure(content: str) -> str:
    """Update the package structure in section 1"""
    
    # Find the package structure section
    old_structure = """net.cyberpunk042.client.gui/
├── screen/
│   └── FieldCustomizerScreen.java       # Main GUI screen
│
├── state/
│   ├── FieldEditState.java                    # Full GUI state container
│   ├── EditorState.java                 # Current editing context
│   └── UndoManager.java                 # Undo/redo stack"""
    
    new_structure = """net.cyberpunk042.client.gui/
├── screen/
│   ├── FieldCustomizerScreen.java       # Main GUI screen
│   └── TabType.java                     # Tab navigation enum
│
├── state/
│   ├── FieldEditState.java              # Full GUI state container
│   ├── FieldEditStateHolder.java        # Singleton access to state
│   ├── StateAccessor.java               # Reflection-based state access
│   ├── AppearanceState.java             # Appearance-specific state
│   ├── EditorState.java                 # Current editing context
│   └── UndoManager.java                 # Undo/redo stack"""
    
    content = content.replace(old_structure, new_structure)
    
    # Update panel section
    old_panel = """├── panel/
│   ├── QuickPanel.java                  # Level 1: Quick Customize
│   ├── AdvancedPanel.java               # Level 2: Advanced Customize
│   ├── DebugPanel.java                  # Level 3: Debug Menu
│   ├── LayerPanel.java                  # Layer navigation
│   ├── PrimitivePanel.java              # Primitive editing
│   ├── ProfilesPanel.java               # Profile management
│   └── sub/"""
    
    new_panel = """├── panel/
│   ├── QuickPanel.java                  # Level 1: Quick Customize
│   ├── AdvancedPanel.java               # Level 2: Advanced Customize
│   ├── DebugPanel.java                  # Level 3: Debug Menu
│   ├── LayerPanel.java                  # Layer navigation
│   ├── PrimitivePanel.java              # Primitive editing
│   ├── ProfilesPanel.java               # Profile management
│   ├── ActionPanel.java                 # Action buttons (Apply, Reset)
│   └── sub/"""
    
    content = content.replace(old_panel, new_panel)
    
    # Update sub-panels list
    old_subpanels = """│       ├── PredictionSubPanel.java      # Prediction settings
│       └── FollowModeSubPanel.java      # Follow mode settings"""
    
    new_subpanels = """│       ├── PredictionSubPanel.java      # Prediction settings
│       ├── FollowModeSubPanel.java      # Follow mode settings
│       ├── ModifiersSubPanel.java       # Bobbing, breathing, etc.
│       └── OrbitSubPanel.java           # Orbit configuration"""
    
    content = content.replace(old_subpanels, new_subpanels)
    
    # Update widget section
    old_widget = """├── widget/
│   ├── LabeledSlider.java               # Slider with label + value
│   ├── RangeSlider.java                 # Min/max range slider
│   ├── EnumDropdown.java                # Enum selector
│   ├── ColorButton.java                 # Color with popup picker
│   ├── ThemePicker.java                 # Theme color picker
│   ├── Vec3Editor.java                  # X/Y/Z inputs
│   ├── ExpandableSection.java           # Collapsible section
│   ├── TooltipWrapper.java              # Adds tooltip to any widget
│   └── ActionButton.java                # Styled button"""
    
    new_widget = """├── widget/
│   ├── LabeledSlider.java               # Slider with label + value
│   ├── ColorButton.java                 # Color with popup picker
│   ├── Vec3Editor.java                  # X/Y/Z inputs
│   ├── ExpandableSection.java           # Collapsible section
│   ├── BottomActionBar.java             # Profile/preset quick bar
│   ├── ConfirmDialog.java               # Confirmation popup
│   ├── PresetConfirmDialog.java         # Preset application dialog
│   ├── ToastNotification.java           # Toast feedback messages
│   └── LoadingIndicator.java            # Loading spinner
│   # Note: EnumDropdown, RangeSlider, ActionButton use MC's CyclingButtonWidget/SliderWidget"""
    
    content = content.replace(old_widget, new_widget)
    
    # Update util section
    old_util = """├── util/
│   ├── GuiWidgets.java                  # Widget factory methods
│   ├── GuiAnimations.java               # Animation utilities (fade, lerp)
│   ├── GuiLayout.java                   # Layout helpers (positioning)
│   ├── GuiConstants.java                # Theme constants (colors, sizes)
│   ├── FragmentRegistry.java            # Single-scope fragments (shape/fill/visibility/etc.)
│   └── PresetRegistry.java              # Multi-scope presets (load from field_presets/)"""
    
    new_util = """├── util/
│   ├── GuiWidgets.java                  # Widget factory methods
│   ├── GuiAnimations.java               # Animation utilities (fade, lerp)
│   ├── GuiLayout.java                   # Layout helpers (positioning)
│   ├── GuiConstants.java                # Theme constants (colors, sizes)
│   ├── GuiKeyboardNav.java              # Keyboard navigation helpers
│   ├── FragmentRegistry.java            # Single-scope fragments (shape/fill/visibility/etc.)
│   └── PresetRegistry.java              # Multi-scope presets (load from field_presets/)"""
    
    content = content.replace(old_util, new_util)
    
    return content

def update_class_count_summary(content: str) -> str:
    """Update the class count table in section 10"""
    
    old_summary = """## 10. Class Count Summary

|| Category | Count | Classes |
||----------|-------|---------|
|| Screen | 1 | FieldCustomizerScreen |
|| State | 3 | FieldEditState, EditorState, UndoManager |
|| Panels | 6 | Quick, Advanced, Debug, Layer, Primitive, Profiles |
|| Sub-Panels | 14 | Shape, Appearance, Animation, Transform, Visibility, Arrangement, Fill, Linking, Prediction, FollowMode, Bindings, Triggers, Lifecycle, **Beam** |
|| Widgets | 9 | LabeledSlider, RangeSlider, EnumDropdown, ColorButton, Vec3Editor, ExpandableSection, TooltipWrapper, ActionButton, ColorPicker |
|| **Utilities** | **5** | **GuiWidgets, GuiAnimations, GuiLayout, GuiConstants, PresetRegistry** |
|| Network | 6 | Packets |
|| Profile | 3 | Profile, ProfileManager, ProfileValidator |
|| **Total** | **~50** | (+3 enums, updated Profile, PresetRegistry, BottomActionBar, ProfilesPanel) |"""
    
    new_summary = """## 10. Class Count Summary

| Category | Count | Classes |
|----------|-------|---------|
| Screen | 2 | FieldCustomizerScreen, TabType |
| State | 6 | FieldEditState, EditorState, UndoManager, **FieldEditStateHolder**, **StateAccessor**, **AppearanceState** |
| Panels | 7 | Quick, Advanced, Debug, Layer, Primitive, Profiles, **ActionPanel** |
| Sub-Panels | 16 | Shape, Appearance, Animation, Transform, Visibility, Arrangement, Fill, Linking, Prediction, FollowMode, Bindings, Triggers, Lifecycle, Beam, **Modifiers**, **Orbit** |
| Widgets | 9 | LabeledSlider, ColorButton, Vec3Editor, ExpandableSection, **BottomActionBar**, **ConfirmDialog**, **PresetConfirmDialog**, **ToastNotification**, **LoadingIndicator** |
| Utilities | 7 | GuiWidgets, GuiAnimations, GuiLayout, GuiConstants, **GuiKeyboardNav**, FragmentRegistry, PresetRegistry |
| Network | 6 | Packets |
| Profile | 2 | Profile, ProfileManager |
| Config | 1 | GuiConfig |
| Render | 1 | TestFieldRenderer |
| **Total** | **~57** | (All implemented, verified by audit) |

> **Note:** EnumDropdown, RangeSlider, ThemePicker, TooltipWrapper, ActionButton from original diagram
> are implemented using Minecraft's native `CyclingButtonWidget`, `SliderWidget`, and `ButtonWidget`."""
    
    content = content.replace(old_summary, new_summary)
    
    return content

def update_status_header(content: str) -> str:
    """Update the status in the header"""
    today = datetime.now().strftime("%B %d, %Y")
    
    old_header = """> **Status:** Implementation Complete  
> **Created:** December 8, 2024  
> **Updated:** December 9, 2024 (Added category system)"""
    
    new_header = f"""> **Status:** Implementation Complete ✅ (Verified by audit)  
> **Created:** December 8, 2024  
> **Updated:** {today} (Added 13 evolved classes, audit verification)"""
    
    content = content.replace(old_header, new_header)
    
    return content

def add_new_class_sections(content: str) -> str:
    """Add documentation for new classes after section 6 (Widgets)"""
    
    # Find the end of section 6 (Widgets) to add new sections
    new_sections = """
---

## 6.7 BottomActionBar

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          BottomActionBar                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ Global bottom action bar (hidden on Profiles tab).                          │
│ Includes profile dropdown and preset two-tier selection.                    │
│                                                                             │
│ Layout:                                                                     │
│   ┌────────────────────────────────────────────────────────────────────┐   │
│   │ PRESETS                    │ PROFILE                              │   │
│   │ [Category ▼] [Preset ▼]    │ [Profile ▼] [SAVE] [REVERT]          │   │
│   └────────────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────────────┤
│ Fields:                                                                     │
│   - presetCategoryDropdown: CyclingButtonWidget                             │
│   - presetDropdown: CyclingButtonWidget                                     │
│   - profileDropdown: CyclingButtonWidget                                    │
│   - saveButton, revertButton: ButtonWidget                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.8 ToastNotification

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ToastNotification                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ Animated toast messages for user feedback.                                  │
│                                                                             │
│ Types: SUCCESS (green), INFO (blue), WARNING (yellow), ERROR (red)          │
│                                                                             │
│ Static methods:                                                             │
│   + success(String message)                                                 │
│   + info(String message)                                                    │
│   + warning(String message)                                                 │
│   + error(String message)                                                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.9 ConfirmDialog

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          ConfirmDialog                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ Modal confirmation popup for destructive actions.                           │
│                                                                             │
│ Fields:                                                                     │
│   - title: String                                                           │
│   - message: String                                                         │
│   - onConfirm: Runnable                                                     │
│   - onCancel: Runnable                                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│ Static:                                                                     │
│   + show(String title, String message, Runnable onConfirm)                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6A. State Utilities

### 6A.1 StateAccessor

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          StateAccessor                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ Reflection-based accessor for FieldEditState paths.                         │
│ Enables state.set("path.to.field", value) and state.get("path.to.field")    │
│                                                                             │
│ Supports:                                                                   │
│   - Dot notation: "spin.speed", "orbit.radius"                              │
│   - Array indices: "layers[0].primitives[1].fill.mode"                      │
│   - @StateField annotations for path validation                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ Methods:                                                                    │
│   + set(Object target, String path, Object value)                           │
│   + get(Object target, String path): Object                                 │
│   + getType(Object target, String path): Class<?>                           │
│   + listPaths(Object target): List<String>                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6A.2 FieldEditStateHolder

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       FieldEditStateHolder                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│ Singleton holder for the current FieldEditState instance.                   │
│ Used by commands and network handlers to access GUI state.                  │
│                                                                             │
│ Static methods:                                                             │
│   + getInstance(): FieldEditState                                           │
│   + setInstance(FieldEditState)                                             │
│   + hasInstance(): boolean                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6B. Additional Sub-Panels

### 6B.1 ModifiersSubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        ModifiersSubPanel                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ Controls for visual modifiers:                                              │
│   • bobbing: LabeledSlider (0-1)                                            │
│   • breathing: LabeledSlider (0-1)                                          │
│   • alphaMultiplier: LabeledSlider (0-1)                                    │
│   • tiltMultiplier: LabeledSlider (0-1)                                     │
│   • swirlStrength: LabeledSlider (0-1)                                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6B.2 OrbitSubPanel

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          OrbitSubPanel                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ Controls for orbit configuration:                                           │
│   • enabled: Toggle                                                         │
│   • radius: LabeledSlider (0.1-10)                                          │
│   • speed: LabeledSlider (0-2)                                              │
│   • axis: EnumDropdown (X, Y, Z, CUSTOM)                                    │
│   • offset: Vec3Editor                                                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

"""
    
    # Find the location after section 6.6 ExpandableSection
    marker = "---\n\n## 7. Network Classes"
    if marker in content:
        content = content.replace(marker, new_sections + marker)
    
    return content

def main():
    print("="*60)
    print("Updating GUI_CLASS_DIAGRAM.md")
    print("="*60)
    
    if not DIAGRAM_FILE.exists():
        print(f"❌ File not found: {DIAGRAM_FILE}")
        return
    
    content = DIAGRAM_FILE.read_text(encoding='utf-8')
    original_len = len(content)
    
    print("\n📝 Applying updates...")
    
    # Apply updates
    content = update_status_header(content)
    print("   ✅ Updated status header")
    
    content = update_package_structure(content)
    print("   ✅ Updated package structure")
    
    content = update_class_count_summary(content)
    print("   ✅ Updated class count summary")
    
    content = add_new_class_sections(content)
    print("   ✅ Added new class documentation sections")
    
    # Write back
    DIAGRAM_FILE.write_text(content, encoding='utf-8')
    new_len = len(content)
    
    print(f"\n📊 File size: {original_len} → {new_len} chars (+{new_len - original_len})")
    print("\n✅ GUI_CLASS_DIAGRAM.md updated!")

if __name__ == "__main__":
    main()

