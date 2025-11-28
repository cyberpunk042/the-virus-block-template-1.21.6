# Shield Field Customization Guide

Players (and modpack authors) can now tailor every visual layer of the Virus Shield field using a fully data‑driven stack. This document explains each configuration tier, how they reference one another, and which in‑game commands let you tweak them live.

---

## Directory Overview

| Layer | Purpose | Built‑ins Location | Override Location |
| --- | --- | --- | --- |
| **Shield Profiles** | Overall field presets (radius, animation, per-layer entries) | `assets/the-virus-block/shield_profiles/` | `config/the-virus-block/forcefields/` |
| **Mesh Styles** | Tessellation & color parameters (lat/lon ranges, swirl, alpha, colors) | `assets/the-virus-block/shield_meshes/` | `config/the-virus-block/meshstyles/` |
| **Mesh Shapes** | Fill masks (bands, checker, wireframe logic) | `assets/the-virus-block/shield_shapes/` | `config/the-virus-block/meshshapes/` |
| **Triangle Types** | Ordering of the two triangles that form each quad | `assets/the-virus-block/shield_triangles/` | `config/the-virus-block/triangletypes/` |

- Overrides automatically shadow the shipped assets when you save via command or drop a JSON into the config directory.
- You can add new files to the config folders without restarting; use the reload commands (see below).

---

## Shield Profiles (`shield_profiles/*.json`)

Top-level presets define the field seen in game. Key sections:

```jsonc
{
  "radius": 18.0,
  "mesh": {
    "lat_steps": 64,
    "lon_steps": 160,
    "swirl_strength": 0.6,
    "layers": {
      "shell": {
        "style": "sphere_square",
        "shape": "sphere_square",
        "triangle": "triangle_other_type_2",
        "radius_multiplier": 1.15,
        "phase_offset": 0.1,
        "colors": { "primary": "#FFF9FFFF", "secondary": "#FFCCE2FF" },
        "alpha": { "min": 0.55, "max": 0.9 }
      }
    }
  },
  "animation": { "scale": 0.30, "spin_speed": 0.03, "tilt": 0.0 },
  "color": { "primary": "#FFF5F9FF", "secondary": "#FFA4C0FF" },
  "alpha": { "min": 0.6, "max": 0.98 },
  "beam": { "enabled": true, "inner_radius": 0.045, "outer_radius": 0.06, "color": "#FFEAFAFF" }
}
```

- `style` → name of a mesh style file (see below).
- `shape` → name of a mesh shape file controlling the fill/pattern.
- `triangle` → triangle type file describing how the two triangles interlock (optional; defaults to `triangle_default`).
- Any fields (colors, alpha, radius multiplier) listed inside a layer override the referenced style on a per-layer basis.

**Switching Profiles In Game**

1. `/shieldvis list` – shows built-in & saved profiles and the current one.
2. `/shieldvis preset <name>` – switch to a built-in profile.
3. `/shieldvis profiles load <name>` – load a JSON from `config/.../forcefields/`.
4. `/shieldvis profiles save <name>` – write the active profile to the config folder for manual editing.
5. `/shieldvis target world|personal` – choose whether the subsequent `shieldvis` commands edit the world field or your personal debug field.
   - Tip: use `/shieldvis preset singularity` (with target `world`) to edit the singularity shield preset, then `profiles save singularity` to persist your custom version before switching back to the anti-virus preset.

---

## Mesh Styles (`shield_meshes/*.json`)

Styles drive the tessellation (lat/long windows, swirl, alpha/colour ramps, etc.). Example:

```jsonc
{
  "mesh_type": "solid",
  "lat_steps": 96,
  "lon_steps": 220,
  "lat_start": 0.0,
  "lat_end": 1.0,
  "lon_start": 0.0,
  "lon_end": 1.0,
  "radius_multiplier": 1.0,
  "swirl_strength": 0.35,
  "phase_offset": 0.0,
  "alpha": { "min": 0.55, "max": 0.96 },
  "colors": { "primary": "#FFF5F9FF", "secondary": "#FFA4C0FF" }
}
```

**Commands**

- `/meshstyle list`
- `/meshstyle show <name>`
- `/meshstyle set <name> <key> <value>` (keys match the JSON fields)
- `/meshstyle save <name>`

Saving writes to `config/the-virus-block/meshstyles/<name>.json`. Changes trigger a reload of the active shield profile so you can see them instantly.

**Shipped Mesh Style Catalog**

- `sphere_plain` – baseline full sphere with gentle swirl (good starting point).
- `sphere_line` – thin equatorial ribbon with higher longitude density.
- `sphere_triangle` – three swirling bands for “arrow” style petals.
- `sphere_square` – dense checker grid suitable for lattice shells.
- `sphere_wireframe` – evenly spaced lat/lon wire grid.
- `sphere_rings` – twelve stacked latitude bands for layered rings.
- `sphere_spiral` – high-swirl solid shell for corkscrew effects.
- `sphere_core` – shrunken inner bubble for multi-layer cores.

---

## Mesh Shapes (`shield_shapes/*.json`)

Shapes are small presets that tell the renderer which parts of the sphere quad should be filled (bands, checker, wireframe). Typical keys:

```jsonc
{
  "type": "checker",        // SOLID | BANDS | WIREFRAME | CHECKER | HEMISPHERE
  "band_count": 8,
  "band_thickness": 0.5,
  "wire_thickness": 0.07
}
```

**Commands**

- `/meshshape list`
- `/meshshape show <name>`
- `/meshshape set <name> <key> <value>` (e.g., `band_count`, `band_thickness`, `wire_thickness`, `type`)
- `/meshshape save <name>`

Edits are stored under `config/the-virus-block/meshshapes/`.

---

## Triangle Types (`shield_triangles/*.json`)

Each sphere quad is rendered as two triangles. Triangle type files specify the order of the three corners for each triangle, letting you achieve perfect squares, lines, arrows, or any other interlocking pattern.

```jsonc
{
  "triangles": [
    ["top_left", "top_right", "bottom_right"],
    ["top_left", "bottom_right", "bottom_left"]
  ]
}
```

Corner names: `top_left`, `top_right`, `bottom_left`, `bottom_right`.

- Shipped permutations: `triangle_other_type_1` … `triangle_other_type_36` cover every ordering of the two base triangles.
- Single-triangle variants: `triangle_single_type_a_*` / `triangle_single_type_b_*`.
- Triple-triangle samples: `triangle_triple_type_1` … `triangle_triple_type_6` for layered fills.

**Commands**

- `/triangletype list`
- `/triangletype show <name>`
- `/triangletype set <name> triangle0 <cornerA,cornerB,cornerC>`
- `/triangletype set <name> triangle1 <cornerA,cornerB,cornerC>`
- `/triangletype save <name>`

Saved files live under `config/the-virus-block/triangletypes/`. Profiles reference triangle types via the `triangle` field per layer (default is `triangle_default`).

---

## Putting It All Together

Each shield layer is resolved in the following order:

1. **Profile layer entry** → defines layer name, per-layer overrides, and references to `style`, `shape`, and `triangle`.
2. **Mesh style** (required) → loads tessellation parameters and base colors.
3. **Mesh shape** (optional) → overrides the fill mask (bands/checker/wire thickness).
4. **Triangle type** (optional) → controls how the two triangles are drawn inside each quad; use this to achieve full squares, arrows, or lines without re-authoring the tessellation.

During game play, use the commands to tweak each layer live:

```text
/shieldvis config set mesh.shell.style sphere_square
/shieldvis config set mesh.shell.shape sphere_square
/shieldvis config set mesh.shell.triangle triangle_other_type_3
/meshstyle set sphere_square lat_steps 80
/meshshape set sphere_square band_thickness 0.4
/triangletype set triangle_other_type_3 triangle0 top_left, top_right, bottom_left
/shieldvis profiles save custom_square
```

Save once you’re happy; the profile (and any referenced styles/shapes/triangles) will be written to their respective config folders, ready to be copied into modpacks or shared with other players.

---

## Personal Protection Field (debug command)

For testing, you can spawn the active profile as a personal protection field around your player:

```
/shieldpersonal on [radius]
/shieldpersonal off
```

- `on` uses the current profile (auto-scaling to 50% of its radius unless you specify a radius override) to render a client-side field that follows you.
- `off` removes it. Eventually this hook will be driven by items/abilities; for now it helps debug custom profiles.
- Personal shields start from the `sphere_rings` mesh style with the `filled_1` triangle type and continuously mirror any `/shieldvis` tweaks—change layers, styles, or triangles and the aura will update on the next tick.
- Use `/shieldvis target personal` to run the full suite of `/shieldvis config|mesh|profiles|set ...` commands against the personal profile without touching the world field. Swap back with `/shieldvis target world`.
- Quickly clone the current world preset into the personal editor with `/shieldpersonal sync`.
- Apply the actual gameplay buff using `/effect give <player> the-virus-block:personal_shield <duration>`; this status effect grants immunity to Matrix Cube impacts and heavily reduces virus defense beam damage.
- `/shieldpersonal color simple <name>` – snaps the aura to a built-in color (red, blue, teal, lime, etc.) and auto-derives highlight/beam accents.
- `/shieldpersonal color primary|secondary|beam <color>` – fine-tune specific channels. Accepts hex codes (`#FF66FFAA`), palette names, or any simple color name.
- Manage personal-only presets via `/shieldpersonal profile ...`:
  - `/shieldpersonal profile list` – built-in + saved names plus the active preset
  - `/shieldpersonal profile preset <name>` – load a built-in personal preset
  - `/shieldpersonal profile save <name>` – persist the current personal config to `config/.../personal-forcefields/`
  - `/shieldpersonal profile load <name>` – load a saved preset from disk
- Hide/show the rendered sphere locally with `/shieldpersonal visual off` or `/shieldpersonal visual on` while keeping the gameplay buff active.
- Adjust how the aura tracks your movement via `/shieldpersonal follow <mode>`: `snap` sticks to you instantly, `smooth` (default) eases into place, and `glide` adds a slower, floaty interpolation.

### Personal Shield Prediction (new `/personalshield` command)

Prediction tuning now has its own root so you don’t have to dive through the generic `shieldvis` tree:

```
/personalshield prediction show
/personalshield prediction enable <true|false>
/personalshield prediction lead <ticks>
/personalshield prediction max <blocks>
/personalshield prediction look <offset>
/personalshield prediction vertical <boost>
```

- `show` echoes the current values (`enabled`, lead ticks, max distance, look-ahead offset, vertical boost).
- `enable` toggles whether the field tries to guess where you’ll be in a few ticks (handy for high-latency clients).
- `lead` defines how many ticks ahead we sample your future position. Higher values = more aggressive prediction.
- `max` clamps how far from the player the projected shield may spawn; keep this near your radius to stop runaway offsets.
- `look` lets you bias the prediction along your look vector (positive pushes forward, negative keeps it closer).
- `vertical` adds a Y boost so gliding/flying players don’t outpace the projected bubble.

Every change is saved into the personal shield profile, so `/shieldpersonal profile save <name>` captures the new prediction tuning automatically.

---

## Singularity Block Field

The Singularity block’s twin-sphere visual (plus the vertical beam) now has its own editable config and command:

- `/singularityvis show` – dumps the current JSON (primary/core sphere curves, beam timings, tessellation counts).
- `/singularityvis set <key> <value>` – tweak any field (`general.lat_steps`, `primary.max_scale`, `beam.inner_radius`, etc.).
- `/singularityvis save` – writes the edited config to `config/the-virus-block/singularity/visual.json`.
- `/singularityvis reload` – reloads from disk (useful after manual edits).

Changes apply instantly to any active singularity visuals.

> Looking for details about the world-border collapse and destruction engine? See `docs/singularity_collapse.md` for the full timeline and tuning notes.

---

## Quick Tips

- **Testing locally:** `/shieldvis spawn <radius> <seconds>` drops a preview of the current profile at your position for rapid iteration.
- **Defaults:** If you omit `shape` or `triangle`, the layer falls back to the profile’s baked-in settings (shape defaults to the style’s mesh_type; triangle defaults to `triangle_default`).
- **Sharing:** Copy the JSONs from `config/the-virus-block/...` into a datapack/resource pack to distribute them with your modpack or server.

Happy customizing!

