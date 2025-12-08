# Field System

> **Last Updated:** December 6, 2024  
> **Status:** Design Complete, Implementation Pending  
> **Version:** 0.1.0

---

## Overview

Unified system for all visual fields: shields, auras, singularity effects, growth block fields, force fields.

**Goals:**
- One system for all field types
- JSON configuration for everything
- Color themes with auto-derivation
- Single `/field` command tree
- Clean, composable primitives

---

## Changelog

| Date | Version | Changes |
|------|---------|---------|
| 2024-12-06 | 0.1.0 | Initial design: architecture, commands, themes |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│  STORAGE                                                                │
│  ─────────────────────────────────────────────────────────────────────  │
│  • colors.json          → named colors                                  │
│  • color_themes.json    → theme definitions                             │
│  • fields/<type>/*.json → field profiles                                │
│  • Hot-reload via /field reload                                         │
├─────────────────────────────────────────────────────────────────────────┤
│  REGISTRY                                                               │
│  ─────────────────────────────────────────────────────────────────────  │
│  • ColorConfig          → existing, resolves named colors               │
│  • ColorThemeRegistry   → NEW, resolves themes to color sets            │
│  • FieldRegistry        → NEW, stores all FieldDefinitions by type/id   │
│  • PresetRegistry       → built-in presets per field type               │
├─────────────────────────────────────────────────────────────────────────┤
│  DEFINITION                                                             │
│  ─────────────────────────────────────────────────────────────────────  │
│  • ColorTheme           → base + derived colors (or explicit)           │
│  • FieldDefinition      → id, type, theme, List<Primitive>, modifiers   │
│  • Primitive            → Sphere, Ring, Cage, Stripes, Prism, etc.      │
│  • Modifier             → spin, tilt, pulse, swirl, glow                │
├─────────────────────────────────────────────────────────────────────────┤
│  RESOLUTION                                                             │
│  ─────────────────────────────────────────────────────────────────────  │
│  • Theme → concrete ARGB colors (auto-derive if needed)                 │
│  • @primary, @glow → resolved from current theme                        │
│  • Profile ID → FieldDefinition lookup                                  │
│  • Primitive → render parameters (radius, segments, alpha, etc.)        │
├─────────────────────────────────────────────────────────────────────────┤
│  RENDERING                                                              │
│  ─────────────────────────────────────────────────────────────────────  │
│  • FieldRenderer        → dispatches to primitive renderers             │
│  • SphereRenderer       → lat/lon tessellation                          │
│  • RingRenderer         → circular segments                             │
│  • CageRenderer         → wireframe polyhedra                           │
│  • PrismRenderer        → n-sided prisms                                │
│  • Vertex emission, RenderLayer, textures, blend modes                  │
├─────────────────────────────────────────────────────────────────────────┤
│  COMMANDS                                                               │
│  ─────────────────────────────────────────────────────────────────────  │
│  • FieldCommand         → unified /field tree                           │
│  • FieldTypeProvider    → per-type subcommand logic                     │
│  • Suggestions          → profile names, keys, colors, themes           │
│  • Feedback             → chat messages, live preview                   │
│  • Save/Load            → profile persistence                           │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Field Types

| Type | Description |
|------|-------------|
| `shield` | World shields (growth blocks, anti-virus, etc.) |
| `personal` | Personal player shield |
| `singularity` | Singularity visual effects |
| `growth` | Growth block field profiles |
| `force` | Force field profiles (push/pull) |
| `aura` | Entity/infection auras |

---

## Color Themes

### Definition (`color_themes.json`)

```json
{
  "themes": {
    "crimson": {
      "base": "#AF1E2D",
      "derive": "auto"
    },
    "nether": {
      "primary": "#FF6A00",
      "secondary": "#AA3000",
      "glow": "#FFAA44",
      "beam": "#FF8833",
      "wire": "#882200"
    }
  }
}
```

### Auto-Derivation Rules

From `base` color:
| Role | Formula |
|------|---------|
| `primary` | base |
| `secondary` | darken(base, 30%) |
| `glow` | lighten(base, 25%) |
| `accent` | lighten(saturate(base, 10%), 40%) |
| `beam` | lighten(base, 35%) |
| `wire` | darken(base, 20%) |

### Usage

```java
Field.sphere("my_shield")
    .theme("crimson")
    .register();
```

```json
{
  "id": "my_shield",
  "theme": "crimson",
  "layers": [
    { "type": "sphere", "color": "@primary" },
    { "type": "wireframe", "color": "@wire" }
  ]
}
```

---

## Commands

```
/field
├── list                              # List all field types
├── reload                            # Reload all JSON configs
├── types                             # Show available field types
│
├── theme
│   ├── list                          # List all themes
│   ├── show <name>                   # Show theme colors
│   ├── apply <name>                  # Apply to current target
│   ├── create <name> <base_color>    # Create new theme
│   └── save                          # Save themes to JSON
│
├── shield
│   ├── list                          # List shield profiles
│   ├── info <id>                     # Show profile details
│   ├── preset <name>                 # Apply preset
│   ├── cycle                         # Cycle through presets
│   ├── profiles
│   │   ├── list                      # List saved profiles
│   │   ├── save <name>               # Save current as profile
│   │   └── load <name>               # Load saved profile
│   ├── config
│   │   ├── show                      # Show current config JSON
│   │   └── set <key> <value>         # Set config value
│   ├── layer
│   │   ├── add <type>                # Add mesh layer
│   │   ├── remove <id>               # Remove mesh layer
│   │   └── set <id> <key> <value>    # Set layer property
│   ├── set
│   │   ├── lat <value>
│   │   ├── lon <value>
│   │   ├── swirl <value>
│   │   ├── scale <value>
│   │   ├── spin <value>
│   │   └── tilt <value>
│   ├── spawn [radius] [seconds]      # Preview spawn
│   └── target <world|personal>       # Switch edit target
│
├── personal
│   ├── on [radius]                   # Enable personal shield
│   ├── off                           # Disable
│   ├── sync                          # Clone from world shield
│   ├── visual <on|off>               # Toggle rendering
│   ├── follow <mode>                 # Set follow mode
│   ├── preset <name>                 # Apply preset
│   ├── profile
│   │   ├── list
│   │   ├── save <name>
│   │   └── load <name>
│   ├── color
│   │   ├── simple <name>             # Auto-derive from one color
│   │   ├── primary <value>
│   │   ├── secondary <value>
│   │   └── beam <value>
│   └── prediction
│       ├── show
│       ├── enable <bool>
│       ├── lead <ticks>
│       ├── max <blocks>
│       ├── look <offset>
│       └── vertical <boost>
│
├── singularity
│   ├── show                          # Show config JSON
│   ├── set <key> <value>             # Set value
│   ├── save                          # Save to file
│   └── reload                        # Reload from file
│
├── growth
│   ├── list                          # List field profiles
│   ├── info <id>                     # Show profile
│   ├── set <id> <key> <value>        # Modify
│   └── reload
│
├── force
│   ├── list                          # List force profiles
│   ├── info <id>
│   ├── set <id> <key> <value>
│   └── reload
│
└── aura
    ├── list
    ├── info <id>
    ├── set <id> <key> <value>
    └── reload
```

---

## File Structure

```
config/the-virus-block/
├── colors.json
├── color_themes.json
└── fields/
    ├── shields/
    │   ├── anti_virus.json
    │   └── growth_block.json
    ├── personal/
    │   ├── default.json
    │   └── rings.json
    ├── singularity/
    │   └── default.json
    ├── growth/
    │   └── default_field.json
    ├── force/
    │   ├── default_pull.json
    │   └── default_push.json
    └── aura/
        └── infection.json
```

---

## Primitives

| Primitive | Description |
|-----------|-------------|
| `sphere` | Solid or banded sphere |
| `shell` | Hollow sphere |
| `ring` | Single horizontal ring |
| `rings` | Multiple rings |
| `cage` | Wireframe polyhedron |
| `stripes` | Horizontal stripe bands |
| `prism` | N-sided vertical prism |
| `wireframe` | Wireframe overlay |

---

## Modifiers

| Modifier | Description |
|----------|-------------|
| `spin(speed)` | Y-axis rotation |
| `spin(x, y, z)` | 3-axis rotation |
| `tilt(angle)` | X-axis tilt |
| `pulse(amount)` | Size oscillation |
| `swirl(strength)` | Swirl distortion |
| `glow()` | Additive blending |
| `alpha(value)` | Fixed transparency |
| `alpha(min, max)` | Pulsing alpha |

---

## Implementation Tasks

- [ ] `ColorTheme` record
- [ ] `ColorThemeRegistry` with auto-derivation
- [ ] `FieldDefinition` record
- [ ] `FieldRegistry` with type/id lookup
- [ ] `Primitive` interface + implementations
- [ ] `FieldRenderer` dispatcher
- [ ] Primitive renderers (sphere, ring, cage, etc.)
- [ ] `FieldCommand` unified command tree
- [ ] JSON parsers for all configs
- [ ] Remove old commands (`/shieldvis`, `/shieldpersonal`, `/singularityvis`, `/meshstyle`, `/meshshape`, `/triangletype`)
- [ ] Migrate existing profiles to new format

---

## Related Documents

- [ARCHITECTURE.md](./ARCHITECTURE.md) - Detailed architecture design
- [BUILDER_API.md](./BUILDER_API.md) - Java builder API examples
- [MESH_ANALYSIS.md](./MESH_ANALYSIS.md) - Analysis of current mesh system
- [REVIEW.md](./REVIEW.md) - Review notes and findings
