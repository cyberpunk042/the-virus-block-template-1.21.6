# Singularity Collapse Timeline

This document covers the **world-border collapse** that follows a singularity detonation. It is separate from the shield visualization system and focuses on the server-side destruction flow, tunables, and debugging hooks.

---

## High-Level Stages

1. **Preparation**
   - When the event arms, the server prepares the collapse around the singularity center.
   - The `CollapseProcessor` is initialized with the target radius and duration.

2. **Border Deployment**
   - The world border shrinks from `barrier_start_radius` toward the center once the scripted singularity event progresses into its collapse phase.
   - As the border shrinks, the `CollapseProcessor` fills blocks in expanding/contracting rings.

3. **Radius-Based Fill**
   - The `CollapseProcessor` uses a simple radius-based fill loop.
   - Each tick, it calculates the current radius based on elapsed time and fills a ring-shaped slice.
   - Fill behavior is controlled by `fill_mode` (air/destroy) and `fill_shape` (outline/matrix/column/row/vector).

4. **Completion**
   - When the collapse duration expires, the event transitions into the core explosion and ring shockwave phases.

---

## CollapseProcessor

The collapse system uses `CollapseProcessor` (~530 lines) for radius-based fills:

- **Ring slice processing** - fills only the ring band at current radius, not entire area
- **Bi-directional** - supports both inward (outside→center) and outward (center→outside)
- **Fill profiles** - predefined batching strategies (default, column_by_column, row_by_row, etc.)
- **Shape-aware throughput** - lighter shapes (column/row) get higher operation multipliers
- **Operations budget** - `max_operations_per_tick` caps block changes per tick
- **Configurable thickness** - all shapes respect the `thickness` parameter
- **Water drainage** - immediate or deferred drainage ahead/behind collapse front
- **Radius delays** - variable speed based on current radius

---

## Server Config & Telemetry

Collapse behavior is split between `config/the-virus-block/services.json` (global defaults) and `config/the-virus-block/dimension_profiles/<dimension>.json` (per-dimension collapse physics/FX).

### services.json

```jsonc
{
  "singularity": {
    "execution": {
      "collapseEnabled": true,
      "allowChunkGeneration": true,
      "allowOutsideBorderLoad": true
    }
  },
  "diagnostics": {
    "enabled": true,
    "logChunkSamples": true,
    "logBypasses": true,
    "logSampleIntervalTicks": 20,
    "logSpam": {
      "enableSpamDetection": true,
      "perSecondThreshold": 10,
      "perMinuteThreshold": 200,
      "suppressWhenTriggered": true
    }
  }
}
```

### dimension_profiles/overworld.json

```jsonc
{
  "collapse": {
    "columns_per_tick": 1,
    "tick_interval": 20,
    "max_radius_chunks": 12,
    "barrier_start_radius": 120.0,
    "barrier_end_radius": 0.5,
    "barrier_duration_ticks": 1000,
    "water_drain": {
      "mode": "ahead",
      "offset": 1,
      "deferred": {
        "enabled": false,
        "initial_delay_ticks": 20,
        "columns_per_tick": 16
      }
    },
    "pre_collapse_water_drainage": {
      "enabled": false,
      "mode": "facing_center",
      "tick_rate": 5,
      "batch_size": 8,
      "start_delay_ticks": 60,
      "start_from_center": false
    },
    "collapse_particles": false,
    "fill_profile": "default",
    "fill_mode": "air",
    "collapse_inward": true,
    "use_native_fill": true,
    "respect_protected_blocks": true,
    "radius_delays": [
      { "side": 1, "ticks": 150 },
      { "side": 3, "ticks": 100 },
      { "side": 9, "ticks": 40 },
      { "side": 15, "ticks": 20 }
    ]
  },
  "effects": {
    "beam_color": "#C600FFFF",
    "veil_particles": "minecraft:sculk_soul",
    "ring_particles": "minecraft:portal",
    "palette": "the-virus-block:overworld"
  }
}
```

---

## Configuration Reference

### Execution Settings (services.json)

| Setting | Purpose |
|---------|---------|
| `collapseEnabled` | Master toggle for collapse; when `false`, fuse detonates but collapse is skipped |
| `allowChunkGeneration` | Allow generating new chunks during collapse |
| `allowOutsideBorderLoad` | Allow loading chunks outside the world border |

### Fill Settings (dimension_profiles)

| Setting | Values | Purpose |
|---------|--------|---------|
| `fill_profile` | See profiles below | Predefined batching strategy |
| `fill_mode` | `air`, `destroy` | How blocks are removed |
| `fill_shape` | `outline`, `matrix`, `column`, `row`, `vector` | Pattern within each slice (overrides profile) |
| `thickness` | Integer (default: 1) | Width of fill pattern (all shapes) |
| `max_operations_per_tick` | Integer (default: 1) | Block change budget per tick |
| `collapse_inward` | Boolean (default: true) | true=outside→center, false=center→outside |
| `use_native_fill` | Boolean | Use MC's native fill vs direct block writer |
| `respect_protected_blocks` | Boolean | Skip bedrock, barriers, etc. |

### Fill Profiles

Profiles bundle shape + thickness + throughput. Override individual values as needed.

| Profile | Shape | Thickness | Cols/Tick | Max Ops |
|---------|-------|-----------|-----------|---------|
| `default` | OUTLINE | 1 | 1 | 1 |
| `column_by_column` | COLUMN | 1 | 1 | 4 |
| `row_by_row` | ROW | 1 | 1 | 4 |
| `vector` | VECTOR | 1 | 1 | 4 |
| `full_matrix` | MATRIX | 1 | 1 | 1 |
| `outline_thick` | OUTLINE | 2 | 1 | 2 |
| `column_thick` | COLUMN | 2 | 1 | 8 |
| `row_thick` | ROW | 2 | 1 | 8 |

### Fill Shapes

All shapes respect `thickness`:

| Shape | Description | Thickness Effect |
|-------|-------------|------------------|
| `matrix` | All blocks in slice | Ignored |
| `outline` | Shell only | Shell depth |
| `column` | Vertical line | Width in X/Z |
| `row` | Horizontal line | Width in Y/Z |
| `vector` | Along longest axis | Perpendicular width |

### Water Drainage Settings

| Setting | Values | Purpose |
|---------|--------|---------|
| `water_drain.mode` | `off`, `ahead`, `behind`, `both` | When to drain fluids during collapse |
| `water_drain.offset` | Integer | Blocks ahead/behind to drain |

### Pre-Collapse Water Drainage

Drains water BEFORE collapse begins, during the fusing phase.

| Setting | Values | Purpose |
|---------|--------|---------|
| `pre_collapse_water_drainage.enabled` | Boolean | Enable pre-drain |
| `pre_collapse_water_drainage.profile` | See profiles below | Preset (mode + thickness + maxOps) |
| `pre_collapse_water_drainage.mode` | See modes below | Override drainage pattern |
| `pre_collapse_water_drainage.tick_rate` | Integer | Ticks between batches |
| `pre_collapse_water_drainage.batch_size` | Integer | Chunks per batch |
| `pre_collapse_water_drainage.start_delay_ticks` | Integer | Delay before starting |
| `pre_collapse_water_drainage.start_from_center` | Boolean | Process center chunks first |
| `pre_collapse_water_drainage.max_operations_per_tick` | Integer | Override block change budget |
| `pre_collapse_water_drainage.thickness` | Integer | Override face thickness |

**Pre-Drain Profiles:**

| Profile | Mode | Thickness | Max Ops |
|---------|------|-----------|---------|
| `default` | OUTLINE | 1 | 32 |
| `row_by_row` | ROWS | 1 | 32 |
| `full` | FULL_PER_CHUNK | 1 | 16 |
| `facing` | FACING_CENTER | 1 | 32 |
| `outline_thick` | OUTLINE | 2 | 64 |
| `row_thick` | ROWS | 2 | 64 |

**Pre-Drain Modes:**

All modes except `full_*` are **direction-aware** - they automatically detect the singularity center and only process the chunk face closest to it.

| Mode | Description |
|------|-------------|
| `full_per_chunk` | Drain entire chunk (not direction-aware) |
| `full_instant` | Same as full_per_chunk |
| `outline` | **Direction-aware** - drain only the face closest to singularity |
| `rows` | **Direction-aware** - drain horizontal rows on the face closest to singularity |
| `facing_center` | Explicit direction-aware face drain (same as outline) |

Use `default` or `outline_thick` profiles for natural direction-aware drainage.

### Runtime Commands

Use `/virusblock singularity erosion …` to change settings at runtime:
- `drainwater <off|ahead|behind|both>`
- `wateroffset <blocks>`
- `particles enable|disable`
- `fillmode <air|destroy>`
- `fillshape <matrix|column|row|vector|outline>`
- `outline <thickness>`
- `nativefill enable|disable`
- `protectedblocks enable|disable`

---

## Effect Palettes

Each dimension profile references an effect palette (`effects.palette`). Palettes define particles and sounds for collapse events:

```jsonc
{
  "id": "my-mod:custom",
  "core_charge": {
    "particle": "minecraft:enchant",
    "sound": { "id": "minecraft:block.beacon.ambient", ... }
  },
  "ring_pulse": {
    "primary_particle": "minecraft:portal",
    "secondary_particle": "minecraft:reverse_portal",
    ...
  }
}
```

Palettes are stored in `config/the-virus-block/effect_palettes/`.

---

## Factory Reset Stage (optional)

When `postResetEnabled` is true in services.json, a final reset stage runs after dissipation:

1. Snapshot every chunk that collapsed
2. After `postResetDelayTicks`, process `postResetChunksPerTick` entries per tick
3. Each entry resets the chunk via vanilla `/chunk reset`
4. Once complete, restore the world border and broadcast "singularity dissipated"

---

## Debugging Tips

- Watch the log for `[Singularity]` entries if preparation fails
- Use `/virusblock singularity diagnostics …` to adjust telemetry
- Temporarily enable `collapse_particles` to visualize the fill pattern
- Check `fill_shape` if blocks aren't clearing as expected

---

## Architecture Notes

The collapse system was simplified in late 2025:
- **Removed**: Legacy chunk-based destruction engine, multithreading, ring management, CollapseExecutionMode enum
- **Added**: `CollapseProcessor` with radius-based fill loop, `CollapseFillProfile` presets
- **Result**: ~530 lines with more features vs ~1,500+ lines legacy, easier to maintain and debug

Key components:
- `CollapseProcessor` - radius-based fill with operations budget (`net.cyberpunk042.infection.service`)
- `CollapseFillProfile` - batching strategy presets (`net.cyberpunk042.infection.profile`)
- `BulkFillHelper` - shape-aware block filling with thickness support
- `CollapseConfigurationService` - config loading with profile defaults

The `CollapseProcessor` is managed by `SingularityModule` and configured through dimension profiles.
