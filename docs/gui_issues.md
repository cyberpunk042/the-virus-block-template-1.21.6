# Field Customizer GUI – Outstanding Issues (Dec 2025)

This document tracks the user-reported problems that are still unresolved. Keep it updated as fixes land.

## Functional
- **Fragments/presets missing**: ✅ FIXED - Gradle tasks copy fragments to run/config for dev and src/main/resources for distribution.
- **Preset dropdown (top bar)**: ✅ FIXED - Added `field_presets` to Gradle copy tasks and JAR resource fallback. Presets load from config/JAR.
- **Layer/Primitive "+"**: ✅ FIXED - Now creates actual `FieldLayer` and `SimplePrimitive` objects with proper default settings.
- **Rename modal**: ✅ FIXED - Focus tracking restored after widget registration.
- **Debug field toggle**: ✅ FIXED - Toggle now uses FieldEditStateHolder.toggleTestField() which updates testFieldActive flag.
- **Test field command hint**: ✅ FIXED - TestFieldRenderer.init() now called from TheVirusBlockClient.

## UI/UX
- **Labels**: ✅ FIXED - Fragment dropdowns now show "Variant: <value>", preset dropdown shows "Select Preset", toggle labels cleaned up.
- **Shape panel robustness**: ✅ FIXED - Added `shapeChangedCallback` so screen re-registers widgets when shape type changes.
- **Background/layout (not urgent per user)**: Right-panel background in fullscreen; windowed panel width/center space/blur toggle; status bar position; clipping in windowed right panel.
- **Beam section**: ✅ FIXED - Layout bug fixed (`layout.getStartY()` → `layout.getY()`). Same fix applied to PredictionSubPanel, FollowModeSubPanel, BindingsSubPanel, LinkingSubPanel.
- **Debug tab content**: ⚠️ NOTE - "Patterns/Explorer tabs" don't exist in codebase. Current Debug tabs: Beam, Trigger, Life, Mods, Arr. Clarify if this refers to planned features or existing content.

## Already OK (keep stable)
- Wireframe preview renders (no black screen); preview refresh appears immediate.
- Sphere duplicate removed from status bar.
- Animation section fits.
- Predict section shows full controls.
- Cylinder open-ended crash resolved.


