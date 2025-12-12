# Field Renderer Architecture

This document describes the dual-renderer system used for field visualization.

## Overview

The Field System uses **two renderers** with different trade-offs:

| Renderer | Purpose | Performance | Feature Coverage |
|----------|---------|-------------|------------------|
| `SimplifiedFieldRenderer` | Quick preview, responsive feedback | Fast | Partial |
| `FieldRenderer` | Production rendering, accurate output | Full | Complete |

## Renderers

### SimplifiedFieldRenderer

**Location:** `client/gui/render/SimplifiedFieldRenderer.java`

**Input:** `FieldEditState` (the GUI editing state object)

**Features:**
- Inline tessellation (no mesh caching)
- Core shapes: Sphere, Ring, Disc, Cylinder, Prism
- Basic transform: scale, offset, rotation
- Basic appearance: primary color, alpha
- Debouncing for performance

**Missing Features (compared to FieldRenderer):**
- Bindings (alpha/scale from player state)
- Trigger effects (damage reactions)
- Lifecycle states (fade in/out)
- Color themes
- Wave/Wobble animations
- Some visibility mask types
- Layer blend modes (advanced)

### FieldRenderer

**Location:** `client/field/render/FieldRenderer.java`

**Input:** `FieldDefinition` (serialized, complete field format)

**Features:**
- Full layer/primitive pipeline
- All shapes with all algorithms
- Complete transform support
- Bindings evaluation
- Trigger effects
- Lifecycle state handling
- Color theme resolution
- All animations (spin, wave, wobble, pulse)
- All visibility masks
- Layer blend modes

## Rendering Contexts

There are **two independent contexts** where field rendering occurs:

### 1. 3D Preview Panel (GUI)

The preview panel in the top-left of the Field Customizer screen.

**Control:** Checkbox inside the preview panel
- ☐ **Basic** → Uses `SimplifiedFieldRenderer`
- ☑ **Advanced** → Uses `FieldRenderer`

**Default:** Basic (for responsiveness)

### 2. Real-World DEBUG/Test Field

The actual field spawned in the game world via:
- `/field test spawn` command
- GUI Spawn/Despawn buttons
- Live Preview toggle

**Control:** Toggle near Preset dropdown (or prominent location)
- ☐ **Fast Preview** → Uses `SimplifiedFieldRenderer`
- ☑ **Accurate** → Uses `FieldRenderer`

**Default:** Accurate (to see true output)

## UI Trimming Logic

When **Simplified/Fast** mode is selected, the GUI should hide or disable panels that control unsupported features.

### Panels to Hide/Disable in Simplified Mode

| Panel/Tab | Reason |
|-----------|--------|
| Bindings | Not evaluated in simplified renderer |
| Trigger | Effects not rendered |
| Wave | Animation not supported |
| Wobble | Animation not supported |
| Color Theme | Not resolved |
| Layer Blend Modes | Only basic blending |
| Some visibility masks | Limited mask support |

### Implementation

```java
// In FieldCustomizerScreen or relevant panel
public boolean isFeatureAvailable(String feature) {
    if (isSimplifiedModeActive()) {
        return SIMPLIFIED_FEATURES.contains(feature);
    }
    return true; // Full mode supports everything
}

// When building panels
if (!isFeatureAvailable("bindings")) {
    bindingsTab.setVisible(false);
    // or: bindingsTab.setEnabled(false);
}
```

### UI Indication

When a panel is disabled due to simplified mode:
- Gray out the tab/section
- Show tooltip: "Enable Advanced mode to access this feature"
- Optionally show a small indicator icon

## Data Flow

### SimplifiedFieldRenderer

```
FieldEditState (GUI state)
    │
    └──▶ SimplifiedFieldRenderer.render()
              │
              ├── Read shape from state
              ├── Read color from state
              ├── Apply basic transform
              └── Inline tessellation & render
```

### FieldRenderer (with conversion)

```
FieldEditState (GUI state)
    │
    └──▶ FieldEditState.toFieldDefinition()
              │
              └──▶ FieldDefinition
                        │
                        └──▶ FieldRenderer.render()
                                  │
                                  ├── Resolve ColorTheme
                                  ├── For each layer:
                                  │     └── LayerRenderer.render()
                                  │           └── For each primitive:
                                  │                 └── PrimitiveRenderer.render()
                                  └── Apply effects, bindings, lifecycle
```

## Required Changes

### 1. Add Conversion Method

```java
// In FieldEditState.java
public FieldDefinition toFieldDefinition() {
    return new FieldDefinition(
        /* convert all state to definition format */
    );
}
```

### 3. Add Mode Controls

In `FieldCustomizerScreen`:
- Add checkbox to preview panel header
- Add toggle near preset dropdown

### 4. Implement UI Trimming

Create a feature availability system that panels can query.

## Future Considerations

1. **Feature parity:** Over time, `SimplifiedFieldRenderer` could gain more features while staying fast

2. **Caching:** `SimplifiedFieldRenderer` could cache tessellated meshes when state hasn't changed

3. **Progressive detail:** Could render simplified version first, then upgrade to full when idle

4. **Mobile/low-end:** Simplified mode could be forced on low-performance devices

