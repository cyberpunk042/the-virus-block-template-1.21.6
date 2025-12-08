# Field Builder API

> **Simple, readable, configurable**

---

## Two Ways to Define Fields

### 1. JSON Files (config/the-virus-block/fields/*.json)
For users, modpack makers, and runtime tweaking.

### 2. Java Builders (in code)
For builtins and programmatic creation.

**Both produce the same `FieldDefinition` - they're interchangeable.**

---

## Colors: Two Approaches

### Approach 1: Themes (Recommended)
Define once, get primary/secondary/glow/beam/wire auto-derived:

```java
// Just pick a theme - all colors auto-derived
Field.sphere("my_shield")
    .theme("crimson")
    .register();
```

```json
{
  "id": "my_shield",
  "theme": "crimson"
}
```

#### Built-in Themes
```
crimson, nether, void, shield_blue, toxic_green, royal_purple
```

#### Create Custom Theme
In `color_themes.json`:
```json
{
  "themes": {
    "my_custom": {
      "base": "#00FF88",
      "derive": "auto"
    }
  }
}
```

Or with full control:
```json
{
  "themes": {
    "my_exact": {
      "primary": "#00FF88",
      "secondary": "#00AA55",
      "glow": "#66FFBB",
      "beam": "#AAFFDD",
      "wire": "#008844"
    }
  }
}
```

#### Use Theme Roles in Layers
```json
{
  "theme": "crimson",
  "layers": [
    { "type": "sphere", "color": "@primary" },
    { "type": "wireframe", "color": "@wire" },
    { "type": "glow", "color": "@glow" }
  ]
}
```

### Approach 2: Direct Colors
For fine-grained control:

```java
// Named color from colors.json
.color("$shieldFieldPrimary")
.color("$singularityBeamPrimary")

// Basic colors
.color("cyan")
.color("purple")
.color("gold")

// Hex (fallback)
.color("#00FF88")
.color("#FF00AAFF")  // with alpha
```

```json
{
  "color": "$shieldFieldPrimary",
  "secondary": "$singularityBeamSecondary"
}
```

---

## Level 1: One-Liners

```java
// Basic sphere with named color
Field.sphere("my_shield")
    .color("$shieldFieldPrimary")
    .spin(0.5f)
    .register();

// Using basic color
Field.hex("hex_field")
    .color("cyan")
    .register();
```

**Equivalent JSON** (`config/the-virus-block/fields/my_shield.json`):
```json
{
  "id": "the-virus-block:my_shield",
  "type": "sphere",
  "color": "$shieldFieldPrimary",
  "spin": 0.5
}
```

---

## Level 2: Modifiers

```java
Field.sphere("striped")
    .stripes(8)
    .colors("$shieldFieldPrimary", "$shieldFieldSecondary")
    .register();

Field.sphere("glowing")
    .color("$singularityBeamPrimary")
    .glow()
    .pulse(0.1f)
    .register();
```

**Equivalent JSON:**
```json
{
  "id": "the-virus-block:striped",
  "type": "sphere",
  "stripes": 8,
  "colors": {
    "primary": "$shieldFieldPrimary",
    "secondary": "$shieldFieldSecondary"
  }
}
```

---

## Level 3: Layered Composition

```java
Field.define("caged_core")
    .add(sphere().glow().color("$singularityBorderCore"))
    .add(cage().wire(0.02f).color("purple").radius(1.2f))
    .spin(0.3f)
    .register();
```

**Equivalent JSON:**
```json
{
  "id": "the-virus-block:caged_core",
  "primitives": [
    {
      "type": "sphere",
      "glow": true,
      "color": "$singularityBorderCore"
    },
    {
      "type": "cage",
      "shape": "sphere",
      "wireThickness": 0.02,
      "color": "purple",
      "radius": 1.2
    }
  ],
  "spin": 0.3
}
```

---

## Level 4: Zones & Regions

```java
Field.sphere("zoned")
    .zone(0f, 0.35f).stripes(4).color("cyan")
    .zone(0.35f, 0.65f).stripes(6).color("teal")
    .register();

Field.sphere("sliced")
    .slice(0f, 0.125f).mirrored().color("$myFieldGlow")
    .register();
```

**Equivalent JSON:**
```json
{
  "id": "the-virus-block:zoned",
  "type": "sphere",
  "zones": [
    { "lat": [0, 0.35], "stripes": 4, "color": "cyan" },
    { "lat": [0.35, 0.65], "stripes": 6, "color": "teal" }
  ]
}
```

---

## Level 5: Complex (Full Control)

```java
Field.define("singularity_visual")
    .add(sphere()
        .radius(1.15f)
        .swirl(0.9f)
        .colors("$singularityBorderCore", "$singularityBorderFlare")
        .alpha(0.45f, 0.95f))
    .add(stripes(8)
        .radius(1.28f)
        .swirl(0.5f)
        .color("$singularityBeamSecondary"))
    .add(wireframe()
        .radius(1.35f)
        .wire(0.12f)
        .color("$singularityBorderFlare"))
    .spin(0.05f)
    .tilt(0.25f)
    .beam("$singularityBeamPrimary")
    .register();
```

**Equivalent JSON:**
```json
{
  "id": "the-virus-block:singularity_visual",
  "primitives": [
    {
      "type": "sphere",
      "radius": 1.15,
      "swirl": 0.9,
      "colors": {
        "primary": "$singularityBorderCore",
        "secondary": "$singularityBorderFlare"
      },
      "alpha": { "min": 0.45, "max": 0.95 }
    },
    {
      "type": "stripes",
      "count": 8,
      "radius": 1.28,
      "swirl": 0.5,
      "color": "$singularityBeamSecondary"
    },
    {
      "type": "wireframe",
      "radius": 1.35,
      "wireThickness": 0.12,
      "color": "$singularityBorderFlare"
    }
  ],
  "spin": 0.05,
  "tilt": 0.25,
  "beam": {
    "color": "$singularityBeamPrimary"
  }
}
```

---

## JSON File Structure

```
config/the-virus-block/
├── colors.json                    # Color palette (existing)
├── fields/                        # Field definitions
│   ├── anti_virus_shield.json
│   ├── singularity_visual.json
│   ├── hex_shield.json
│   └── personal/
│       ├── default.json
│       ├── rings.json
│       └── fraction-8.json
└── field_presets/                 # Quick presets
    ├── sphere_glow.json
    └── cage_octahedron.json
```

---

## Color Reference Quick Guide

| Syntax | Source |
|--------|--------|
| `"$slotName"` | ColorSlot enum |
| `"$myCustom"` | colors.json namedColors |
| `"red"`, `"cyan"` | Basic colors |
| `"#RRGGBB"` | Hex (6-digit) |
| `"#AARRGGBB"` | Hex with alpha |

### Available ColorSlots (from existing ColorConfig):
```
$shieldFieldPrimary
$shieldFieldSecondary
$singularityBorderCore
$singularityBorderFlare
$singularityBorderCollapse
$singularityBorderDissipate
$singularityBeamPrimary
$singularityBeamSecondary
```

### Basic Colors:
```
white, black, gray, light_gray, dark_gray
red, crimson, orange, gold, yellow
lime, green, teal, cyan, blue, navy
purple, magenta, pink
```

---

## Commands (for runtime tweaking)

```
/virus field list                     - List all fields
/virus field info <id>                - Show field details
/virus field reload                   - Reload all JSON files
/virus field create <id>              - Start building interactively
/virus field set <id> <key> <value>   - Modify field property
/virus field save <id>                - Save changes to JSON
```

---

## Shortcut Reference

### Quick Starters
| Method | Creates |
|--------|---------|
| `Field.sphere(name)` | Solid sphere |
| `Field.shell(name)` | Hollow sphere |
| `Field.cube(name)` | Solid cube |
| `Field.hex(name)` | Hexagonal prism |
| `Field.prism(name, sides)` | N-sided prism |
| `Field.rings(name, count)` | Multiple rings |
| `Field.axialRings(name)` | X/Y/Z axis rings |
| `Field.cage(name)` | Wireframe |
| `Field.define(name)` | Custom composition |

### Shape Modifiers
| Method | Effect |
|--------|--------|
| `.wireframe()` | Wireframe mode |
| `.stripes(n)` | Horizontal stripes |
| `.glow()` | Additive blending |
| `.topHalf()` / `.bottomHalf()` | Hemisphere |
| `.zone(start, end)` | Latitude range |
| `.slice(start, end)` | Longitude range |
| `.mirrored()` | Add opposite side |

### Appearance
| Method | Effect |
|--------|--------|
| `.color("$name")` | Named/slot color |
| `.color("basic")` | Basic color |
| `.color("#hex")` | Hex color |
| `.colors(primary, secondary)` | Gradient |
| `.alpha(value)` | Fixed transparency |
| `.alpha(min, max)` | Pulsing alpha |
| `.wire(thickness)` | Wireframe width |

### Transform & Animation
| Method | Effect |
|--------|--------|
| `.radius(r)` | Size |
| `.spin(speed)` | Y rotation |
| `.spin(x, y, z)` | 3-axis rotation |
| `.pulse(amount)` | Size oscillation |
| `.tilt(angle)` | X-axis tilt |
| `.swirl(strength)` | Swirl distortion |

### Effects
| Method | Effect |
|--------|--------|
| `.push(strength)` | Push force |
| `.pull(strength)` | Pull force |
| `.shield()` | Block infection |
| `.damage(amount)` | Deal damage |
| `.beam(color)` | Vertical beam |

---

## Design Principles

1. **JSON-first** - Everything definable in files
2. **Color references** - Use existing ColorConfig system
3. **Hot-reload** - `/virus field reload`
4. **Command control** - Tweak at runtime
5. **Fluent API** - Clean code when needed
6. **Equivalence** - JSON ↔ Builder produce same result
