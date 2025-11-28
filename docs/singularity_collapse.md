# Singularity Collapse Timeline

This document covers the **world-border collapse** that follows a singularity detonation. It is separate from the shield visualization system and focuses on the server-side destruction flow, tunables, and debugging hooks.

---

## High-Level Stages

1. **Ring Preparation**
   - When the event arms, the server samples concentric chunk rings around the singularity center (outer radius → inner core).
   - Each ring stores the chunk list and the world-border radius that should activate it.

2. **Border Deployment**
   - The world border shrinks from `SINGULARITY_BORDER_START_RADIUS` toward the center once the scripted singularity event progresses into its collapse phase (either via story progression or server-side trigger).
   - As soon as the live radius drops past a ring’s threshold, that ring is marked “active”.

3. **Shell Erosion**
   - In `ring_slice` mode every active ring is eaten from the outside in: the engine processes one 16×256 stripe per chunk each tick, so a complete shell disappears in 16 passes.
   - In `ring_slice_chunk` mode the engine drops the entire 16×16×height chunk in one bulk operation, trading gradual visuals for raw throughput.

4. **Chunk Completion**
   - When the last slice (or chunk) in a ring finishes, the engine spawns the sonic veil burst and removes that chunk from the pending set.
   - Once the ring queue empties, the event transitions into the core explosion and ring shockwave phases.

---

## Engine Tunables

| Constant | Purpose | Default |
| --- | --- | --- |
| `SINGULARITY_COLLAPSE_CHUNKS_PER_STEP` | Base collapse throughput. We process `value × 64` stripes per tick now that bulk fill is in place. | `1` |
| `SINGULARITY_COLLAPSE_PARTICLE_DENSITY` | Ash particles spawned per removed block. | `6` |

Additional notes:
- The engine automatically clamps to `world.getBottomY()` / `world.getTopY()`, so custom-dimension heights require no extra config.
- Because destruction is column-based, chunk saving/unloading is gradual. If the server restarts mid-collapse, the ring snapshot is rebuilt on boot and the collapse resumes without manual intervention.

---

## Server Config & Telemetry

Collapse behavior is also governed by `config/the-virus-block/singularity.json`:

```jsonc
{
  "allowChunkGeneration": true,
  "allowOutsideBorderLoad": true,
  "debugLogging": true,
  "drainWaterAhead": true,
  "waterDrainOffset": 1,
  "multithreadCollapse": false,
  "respectProtectedBlocks": true,
  "collapseMode": "ring_slice",
  "collapseParticles": false,
  "fillMode": "air",
  "fillShape": "outline",
  "outlineThickness": 2,
  "useNativeFill": true,
  "collapseWorkerCount": 1,
  "collapseTickDelay": 1,
  "collapseEnabled": true,
  "fuseExplosionDelayTicks": 400,
  "fuseAnimationDelayTicks": 20,
  "fusePulseInterval": 8,
  "collapseViewDistance": 0,
  "collapseSimulationDistance": 0,
  "collapseBroadcastMode": "immediate",
  "collapseBroadcastRadius": 96,
  "collapseDefaultProfile": "full",
  "chunkPreGenEnabled": true,
  "chunkPreGenRadiusBlocks": 0,
  "chunkPreGenChunksPerTick": 8,
  "chunkPreloadEnabled": true,
  "chunkPreloadChunksPerTick": 4,
  "radiusDelays": [
    { "side": 1, "ticks": 150 },
    { "side": 3, "ticks": 100 },
    { "side": 9, "ticks": 40 },
    { "side": 15, "ticks": 20 }
  ],
  "barrierStartRadius": 120.0,
  "barrierEndRadius": 0.5,
  "barrierInterpolationTicks": 1000,
  "loggingWatchdog": {
    "enableSpamDetection": true,
    "perSecondThreshold": 10,
    "perMinuteThreshold": 200,
    "suppressWhenTriggered": true
  },
  "postResetEnabled": true,
  "postResetDelayTicks": 25,
  "postResetTickDelay": 1,
  "postResetChunksPerTick": 2,
  "postResetBatchRadius": 1
}
```

- The first two booleans act as hard stops for chunk generation and outside-border loads (the gamerules simply mirror them at runtime).
- `debugLogging` prints a per-tick summary (`columnsProcessed`, `columnsCompleted`, `blocksCleared`, skips, fluids cleared) and dumps `blockedChunks=` / `broadcast=` digests so you instantly know why work stalled.
- `drainWaterAhead` and `waterDrainOffset` control the proactive fluid pass that clears water/lava a few blocks ahead of the erosion front, preventing giant waterfalls from popping into existence mid-collapse.
- `collapseMode` selects the destruction strategy:
  - `ring_slice` – perimeter-first sweep that chomps one column per chunk per tick so a whole ring collapses in ~16 passes.
  - `ring_slice_chunk` – vaporizes each chunk in a single bulk fill as soon as its turn arrives.
- `respectProtectedBlocks` keeps negative-hardness blocks (bedrock, etc.) untouched in every mode.
- `collapseParticles` gates the ash/sound spam for each cleared block; leave `false` when profiling.
- `fillMode` controls how bulk slices are cleared: `"air"` mimics `/fill ... air`, `"destroy"` behaves like `/fill ... destroy`.
- `fillShape` decides the sweep pattern inside each chunk (`matrix`, `column`, `row`, `vector`, `outline`) so you can bias visuals or throughput without touching code. We now default to the `outline` shape with a two-block thickness so the border and destruction visuals stay in lock-step.
- `outlineThickness` (only used when `fillShape` is `"outline"`) widens each pass to cover multiple columns. Set it to `> 1` to chew through thicker rings instead of the default single-column outline; the shipping config starts at `2`.
- `useNativeFill` switches between the optimized direct block writer (`false`) and a vanilla-style fill that fires block updates and spawns drops (`true`). Native `/fill` is the new default so the outline clears faster while still honoring block immunity checks.
- `collapseWorkerCount` is rounded down to the nearest multiple of four and capped by your available cores (minus one). It only matters when `multithreadCollapse` is `true`; single-threaded collapse now ships with `1` to keep the profile lightweight out of the box.
- `multithreadCollapse` toggles the collapse worker scheduler. When enabled, slices are chewed on background threads before the main thread applies the block changes, using `collapseWorkerCount` (rounded to a multiple of four, e.g., 4/8/12) to determine the pool size.
- `collapseTickDelay` sets the baseline cooldown (in ticks) between destruction batches. Increasing it slows the overall collapse cadence without touching throughput maths.
- `radiusDelays` lets you override the tick delay once the active ring shrinks to a given side length. Entries are evaluated from large to small sides; the first match replaces the cooldown computed by `collapseTickDelay`, making it easy to slow the collapse near the core without editing code.
- `collapseEnabled` is the master toggle; when `false`, the fuse still detonates but the actual collapse/ring stages are skipped (and the gamerule `virusSingularityCollapseEnabled` can override this live).
- `fuseExplosionDelayTicks` controls how long the fuse lingers (in ticks) before the detonation happens once Tier 5 finishes.
- `fuseAnimationDelayTicks` sets how long after the fuse begins before the protective shell visually collapses.
- `fusePulseInterval` governs the baseline frequency of fuse pulses (smaller numbers mean more frequent pulses; gameplay still clamps to at least every 4 ticks).
- `collapseViewDistance` / `collapseSimulationDistance` optionally override the vanilla player manager distances while the collapse runs (set to `0` to leave vanilla values untouched). We snapshot the original values and restore them once the event finishes, and every change is logged under the `singularity` channel.
- `collapseBroadcastMode` / `collapseBroadcastRadius` control how aggressively we stream collapse block updates to clients. `immediate` mirrors current behaviour, while `delayed`/`summary` skip packets for chunks farther than the configured radius (in blocks) and replay them later when a player gets close (`summary` also emits a short log entry).
- `collapseDefaultProfile` selects the per-player sync profile applied on login (`full`, `cinematic`, or `minimal`). Operators can override individuals with `/virusblock singularity profile …`.
- `chunkPreGenEnabled` toggles whether the server proactively pre-generates chunks around the singularity while it is dormant/fusing. Disable it when you know the terrain already exists on disk or want to rely on manual pre-generation.
- `chunkPreGenRadiusBlocks` overrides how far (in blocks) the pre-generation sweep should extend. Set it to `0` to fall back to `barrierStartRadius`.
- `chunkPreGenChunksPerTick` caps how many chunks the pre-generation routine attempts per tick, letting you trade time for lower TPS impact. Defaults dropped to `8` so even slower servers can keep up without falling behind the fuse timer.
- `chunkPreloadEnabled` guards the forced-chunk pass that runs right before the collapse starts. Turning it off skips the pin/load stage entirely (the collapse will simply skip missing chunks at runtime).
- `chunkPreloadChunksPerTick` applies the same “chunk budget per tick” idea to the preload queue; the default is now `4` to avoid watchdog spam unless you explicitly raise it.
- `loggingWatchdog` mirrors `logs.json` but scoped to the singularity channel; it suppresses duplicate collapse logs once they exceed the configured rate and emits `[watchdog]` summaries instead. The stock thresholds were tightened to `10` events/sec or `200`/min with suppression enabled by default, so runaway spam is automatically clamped.
- `barrierStartRadius` controls the starting world-border radius (in blocks) when the collapse begins. This value also drives the preload/pregen radius so you can start farther out without touching code.
- `barrierEndRadius` clamps the final radius that the border eases toward. It is automatically limited so it never exceeds the starting radius.
- `barrierInterpolationTicks` feeds directly into the world-border interpolation call, allowing faster or slower border animations independent of destruction throughput.
- `postResetEnabled` unlocks a final “factory reset” stage once the collapse finishes. This stage now ships enabled by default: after `postResetDelayTicks` (25 ticks), we reset `postResetChunksPerTick` entries per tick, expanding each by `postResetBatchRadius` chunks.
- `postResetTickDelay` inserts a per-batch cooldown (in ticks) between successive chunk-reset bursts so you can throttle the `/chunk reset` workload when the queue is enormous.
- `postResetChunksPerTick` and `postResetBatchRadius` control how aggressively that reset stage sweeps: `chunksPerTick` is how many entries we process each tick, and `batchRadius` expands each entry into a `(2r+1)²` chunk square (e.g., `1` resets a 3×3 around every recorded chunk).
- The border now auto-resets once the shrink animation finishes and the reset phase (if any) ends, so players are never stuck behind a 1-block radius after the event.

### Factory Reset Stage (optional)

When `postResetEnabled` is true, the singularity flow adds a final state after dissipation:

1. We snapshot every chunk that collapsed.
2. After `postResetDelayTicks`, we process `postResetChunksPerTick` entries per tick.
3. Each entry resets the chunk (and its `postResetBatchRadius` neighbourhood) via vanilla `/chunk reset`, rebuilding it from the world seed and wiping lingering debris.
4. Once the queue empties, we restore the original world border, release tickets, and broadcast the usual “singularity dissipated” message.

This is optional and safe to leave off—collapse already removes terrain—but it’s convenient when you want the area pristine for scripted encounters or repeated playtests.

### Sync Profiles

Use `/virusblock singularity profile set <player> <full|minimal|cinematic>` to override an individual’s collapse experience (`full` streams everything, `cinematic` enables predictive payloads, `minimal` suppresses off-screen updates). `/virusblock singularity profile get <player>` reports the current value, and `/virusblock singularity profile default <profile>` updates the default applied on login.

---

## Debugging Tips

- Watch the log for `[Singularity]` entries if the engine can’t find a center or if chunk preparation fails.
- `[Singularity] blockedChunks=...` lines list the first few chunks that were skipped plus the reason (outside border, missing, exception). If they show up constantly, double-check your chunk budgets or border/generation flags.
- `[Singularity] broadcast=...` lines summarize how many chunk updates were buffered or flushed when running the new `delayed`/`summary` broadcast modes—great for confirming that far-away work is intentionally deferred.
- When testing locally, trigger the singularity event through the normal story or dev command sequence you already use for the block—`/virusblock singularity start`
- Temporarily raise `SINGULARITY_COLLAPSE_CHUNKS_PER_STEP` when profiling on high-end hardware to verify the destruction engine scales.

Feel free to expand this doc with additional gamerule hooks or command references as new tooling lands.*** End Patch

