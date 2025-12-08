# Field System Review - Senior Sweep

> **Status:** Deep Analysis  
> **Focus:** Color system improvements + Command unification

---

## Current State Analysis

### ✅ What's Already Good

1. **ColorConfig** - Solid foundation with:
   - Named colors in `colors.json`
   - ColorSlot enum for system colors
   - Basic colors (red, cyan, etc.)
   - `resolveNamedColor()` and `parseUserColor()`

2. **ShieldPersonalCommand** has `color simple <name>` - auto-derives primary/secondary/beam from ONE color

3. **Profile loading** - Already loading from JSON files (shields, palettes, etc.)

4. **Command structure** - Already has list/set/save/reload pattern

---

## 🔴 Issues Identified

### Issue 1: Color Entry is Too Manual

**Current Flow:**
1. Edit `colors.json` manually
2. Add entry: `"myCustomGlow": "#00FFAA"`
3. Reference it: `"$myCustomGlow"`
4. Repeat for every variant (primary, secondary, glow, accent, beam...)

**Pain points:**
- 5+ entries needed for one "theme"
- No relationships between colors
- No auto-derivation
- Easy to create inconsistent palettes

### Issue 2: Commands are Fragmented

**Currently:**
- `/shieldvis` - world shields
- `/shieldpersonal` - personal shields
- `/singularityvis` - singularity
- `/meshstyle` - low-level mesh
- `/meshshape` - low-level shape
- `/triangletype` - low-level triangles

**Problems:**
- Different syntax per field type
- No unified discoverability
- Hard to remember which command for what
- Auras, growth block fields, force fields have no commands

### Issue 3: No Theme/Palette System

Currently colors are **atomic** - each color stands alone. No way to say:
- "Apply the 'crimson' theme to this shield"
- "Use the 'nether' color scheme"

---

## 💡 Proposed Solutions

### Solution 1: Color Themes

Add a layer above individual colors:

```json
// config/the-virus-block/color_themes.json
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
      "accent": "#FFD088",
      "beam": "#FF8833"
    },
    "virus": {
      "base": "$singularityBorderCore",
      "derive": "auto"
    }
  }
}
```

**Auto-derivation rules:**
```java
// From base color, derive:
primary   = base
secondary = darken(base, 30%)
glow      = lighten(base, 25%)
accent    = lighten(saturate(base, 10%), 40%)
beam      = lighten(base, 35%)
wire      = darken(base, 20%)
```

**Usage in field JSON:**
```json
{
  "theme": "crimson"
}
```

Or in Java:
```java
Field.sphere("my_shield")
    .theme("crimson")
    .register();
```

### Solution 2: Unified Field Command

One command tree for ALL field types:

```
/field list                           - List all field types
/field <type> list                    - List fields of type
/field <type> info <id>               - Show field details
/field <type> set <id> <key> <value>  - Modify
/field <type> reload                  - Reload from JSON
/field <type> save <id>               - Save to JSON

Field types:
- shield      (world shields)
- personal    (personal shield)
- aura        (infection auras)
- singularity (singularity visuals)
- growth      (growth block fields)
- force       (force fields)
```

**Examples:**
```
/field shield list
/field personal theme crimson
/field singularity set visual.swirl 0.5
/field aura reload
```

### Solution 3: Color Derivation Functions

Support inline color math:

```json
{
  "primary": "crimson",
  "secondary": "darken(crimson, 30%)",
  "glow": "lighten(crimson, 25%)"
}
```

Or theme-relative:
```json
{
  "theme": "crimson",
  "wire": "darker(@primary, 20%)"
}
```

### Solution 4: Semantic Color Roles

In field definitions, use roles not raw colors:

```json
{
  "layers": [
    { "type": "sphere", "color": "@primary" },
    { "type": "wireframe", "color": "@wire" },
    { "type": "glow", "color": "@glow" }
  ]
}
```

Resolved at render time based on current theme.

---

## Command Architecture Proposal

```
/field
├── list                          # All field types
├── types                         # Available types
├── reload                        # Reload all
│
├── shield
│   ├── list
│   ├── info <id>
│   ├── preset <name>
│   ├── set <key> <value>
│   ├── layer add <type>
│   ├── layer remove <id>
│   ├── layer set <id> <key> <value>
│   ├── save <name>
│   └── spawn [radius] [seconds]
│
├── personal
│   ├── on [radius]
│   ├── off
│   ├── theme <name>
│   ├── color <channel> <value>
│   ├── preset <name>
│   ├── save <name>
│   └── follow <mode>
│
├── singularity
│   ├── show
│   ├── set <key> <value>
│   ├── save
│   └── reload
│
├── aura
│   ├── list
│   ├── set <id> <key> <value>
│   └── reload
│
├── growth
│   ├── list
│   ├── set <id> <key> <value>
│   └── reload
│
├── force
│   ├── list
│   ├── set <id> <key> <value>
│   └── reload
│
└── theme
    ├── list
    ├── show <name>
    ├── create <name> <base_color>
    ├── set <name> <role> <value>
    ├── apply <name>              # Apply to current target
    └── save
```

---

## Implementation Priority

### Phase 1: Color Themes (High Impact, Medium Effort)
1. Add `ColorTheme` record with role → color mapping
2. Add `ColorThemeRegistry` with auto-derivation
3. Add `color_themes.json` loading
4. Update field configs to support `"theme": "name"`

### Phase 2: Unified Command (Medium Impact, High Effort)
1. Create `FieldCommand` with unified tree
2. Migrate existing commands as subcommand providers
3. Add `FieldTypeRegistry` for extensibility
4. Deprecate old commands (redirect for compatibility)

### Phase 3: Color Functions (Nice to Have)
1. Add color math parser: `darken()`, `lighten()`, `saturate()`
2. Support `@role` references
3. Support inline derivation in JSON

---

## File Structure (Updated)

```
config/the-virus-block/
├── colors.json                     # Raw named colors
├── color_themes.json               # Theme definitions
│
├── fields/
│   ├── shields/
│   │   ├── anti_virus.json
│   │   └── growth_block.json
│   ├── auras/
│   │   └── tier1.json
│   ├── singularity/
│   │   └── default.json
│   ├── personal/
│   │   ├── default.json
│   │   └── rings.json
│   └── force/
│       └── repel.json
│
└── effect_palettes/                # Already exists
    ├── overworld.json
    └── nether.json
```

---

## Quick Wins

These require minimal changes but improve UX significantly:

1. **Add more auto-derivation to `color simple`** - already works, expand it

2. **Add theme presets** - define 5-10 built-in themes in code:
   ```java
   CRIMSON, NETHER, VOID, SHIELD_BLUE, TOXIC_GREEN, ROYAL_PURPLE
   ```

3. **Add `/field` as alias** - just register `/field` that redirects to `/shieldvis` for now, expand later

4. **Document existing commands better** - many players don't know about `color simple`

---

## Open Questions

1. Should themes be per-field-type or global?
2. How to handle theme inheritance? (`nether extends virus`?)
3. Live preview when changing themes?
4. Should auto-derivation be configurable per theme?

