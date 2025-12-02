# VirusWorldState Refactor Plan (Meta Blueprint)

> **Goal:** extract the infection finale (spread upkeep, shell defenses, singularity, FX, commands) from the monolithic `VirusWorldState` into testable components that can be swapped per dimension (Overworld, Nether, future biomes).

---

## Vision

| Layer | Responsibility | Output / Pattern |
| --- | --- | --- |
| Scenario Registry | Maps `DimensionKey/Biome` to `InfectionScenario` | Strategy factory / service locator |
| Infection Scenario | Encapsulates per-dimension behavior, assets, metastate | Provides controllers, effect sets, config |
| Singularity Controller | State machine for `DORMANT → … → DISSIPATION` | Drives collapse + FX via State pattern |
| Ring Planner | Builds concentric chunk shells | `RingPlan` (Builder) |
| Destruction Engine | Consumes ring metadata column-by-column | Column tasks, FX callbacks |
| Effect Bus / Scheduler | Dispatches particles, beams, buffs, audio; schedule delayed work | Observer hub + task queue |
| Command & Config Facade | Exposes admin/player tooling | Facade over registry/controller |

## Progress Snapshot _(2025‑12‑02)_

| Status | Highlights |
| --- | --- |
| ✅ Complete | Scenario/Controller/Context/EffectBus/VirusScheduler scaffolding; `AbstractSingularityController` + phase handlers now drive every state (DORMANT → RESET); `CollapseBroadcastManager`, palette-driven EffectBus listeners (`ConfiguredScenarioEffectSet`, guardian beams, HUD), scheduler persistence, command facade, dimension profile loader, and `InfectionServiceContainer` are live. Collapse system fully simplified: `CollapseProcessor` with bi-directional support, `CollapseFillProfile` presets, shape-aware throughput scaling, operations budget, and thickness support for all shapes. Legacy destruction engine, multithreading, and ring management removed. |
| ⚒️ In Progress | Nether still mirrors Overworld behaviour (needs profile-driven deltas), block/entity relocation is pending. |
| 🔜 Next | Finalize Nether-specific overrides, add palette validators, keep slimming `VirusWorldState` helpers into services, and begin the physical package cleanup (block entity move, README stubs) promised in the cleanup plan. |

---

## Scenario Abstraction (Strategy)

```java
interface InfectionScenario {
    ResourceLocation id();
    DimensionProfile profile();
    SingularityController createSingularityController(SingularityDependencies deps);
    ScenarioEffectSet createEffectSet(VirusWorldContext ctx);
    void tick(VirusWorldContext ctx);
}
```

- `DimensionProfile` is JSON-driven: collapse speed, ambient palette, knockback tuning, scheduler defaults, etc.
- `ScenarioRegistry` is a factory/service locator: map `<DimensionKey, InfectionScenarioFactory>`. Default entry = `OverworldScenario`, later add `NetherScenario`, warped variants, etc.
- `VirusWorldState` becomes the host: it resolves the scenario once per dimension, supplies world context each tick, and delegates.

**Benefits**
- Zero dimension-specific `if` branches inside `VirusWorldState`.
- Scenario swaps become “register a new factory and profile”.
- Tests can stand up a scenario with fake contexts to verify state transitions without booting the entire infection system.

---

## Singularity Controller (State Machine)

### States
- `DORMANT`
- `FUSING`
- `COLLAPSE`
- `CORE`
- `RING`
- `DISSIPATION`

### Interface

```java
interface SingularityController {
    void start(SingularityContext ctx);
    void tick(SingularityContext ctx);
    void abort(SingularityContext ctx);
}
```

- Each state can be its own class (`SingularityState` pattern) or implemented via enum with behavior methods.
- `SingularityContext` bundles: `ServerWorld world`, `EffectBus`, `RingPlanner`, `SingularityDestructionEngine`, `DimensionProfile`, `VirusScheduler`, plus hooks back to `VirusWorldState` for persistence.
- Reusable countdown logic (`FUSING`), ring activation, guardian beam orchestration, border deployment, and clean-up steps move out of the persistent state.

**Extension Points**
- Nether scenario swaps beam color, audio set, collapse order, or inserts an `ASH_STORM` phase before collapse.
- Controllers can request alternate planner modes (spiral, slab) for dramatic variants.

---

## Collapse Flow (Simplified)

The collapse system uses a radius-based approach via `CollapseProcessor`:

1. **Start**: `CollapseProcessor.start(center, startRadius, endRadius, durationTicks)` initializes collapse
   - Direction from config (`collapse_inward: true` = outside→center, `false` = center→outside)
2. **Tick**: Each tick:
   - Check `radius_delays` for current tick interval
   - Calculate current radius from elapsed time
   - Process up to `columns_per_tick` radius slices (scaled by shape weight)
   - Each slice respects `max_operations_per_tick` budget
3. **Fill**: Uses `BulkFillHelper` for block removal
   - Shape from `fill_profile` or explicit `fill_shape`
   - All shapes respect `thickness` parameter
   - Throughput scaling: COLUMN/ROW get 4x, OUTLINE gets 2x multiplier
4. **Water Drain**: Optional drainage ahead/behind collapse front (immediate or deferred)
5. **Complete**: When duration expires, processor marks itself inactive

All configuration comes from dimension profiles (`collapse.*` settings) with `fill_profile` providing defaults.

---

## Collapse System (Simplified)

The legacy `SingularityDestructionEngine`, `CollapseSchedulerService`, `CollapseExecutionMode`, and related classes were removed in December 2025. The collapse system now uses `CollapseProcessor` + `CollapseFillProfile`:

- **Ring slice processing**: Only fills the ring band at current radius (not entire area)
- **Bi-directional**: Supports inward (outside→center) and outward (center→outside)
- **Fill profiles**: `CollapseFillProfile` enum provides batching presets (default, column_by_column, row_by_row, vector, full_matrix, thick variants)
- **Shape-aware throughput**: Lighter shapes get higher operation multipliers
- **Operations budget**: `max_operations_per_tick` caps block changes per tick
- **Thickness support**: All shapes (except MATRIX) respect the `thickness` parameter
- **Radius delays**: Variable speed based on current radius

The `CollapseProcessor` is managed by `SingularityModule` and configured through dimension profiles with profile defaults.

---

## Effect Bus & Scheduler

### Effect Bus (Observer)

```java
interface InfectionEvent {}
final class GuardianBeamEvent implements InfectionEvent { ... }
final class CollapseVeilEvent implements InfectionEvent { ... }

interface EffectBus {
    void post(InfectionEvent event);
    <T extends InfectionEvent> void register(Class<T> type, Consumer<T> handler);
}
```

Listeners:
- `GuardianBeamManager`
- `CollapseFXEmitter`
- `PersonalShieldIntegrator`
- `AmbientAudioController`

Each listener can be scenario-specific or shared. Events are emitted by controllers, scenario ticks, or scheduler jobs.

### VirusScheduler

Generic task queue:

```java
interface VirusScheduler {
    void schedule(int delayTicks, Runnable task);
    void tick();
}
```

Used for timed fuse flashes, delayed shell collapse, multi-phase FX. `SimpleVirusScheduler` now backs the host implementation so controllers can schedule work without touching `VirusWorldState`; the remaining step is persisting/restoring outstanding tasks so they survive server restarts.

---

## Configuration Layer (JSON)

### DimensionProfile JSON

```jsonc
{
  "id": "the-virus-block:overworld",
  "collapse": {
    "columns_per_tick": 8,
    "tick_interval": 20,
    "max_radius_chunks": 12,
    "mode": "erode",
    "ring_start_delay_ticks": 40,
    "ring_duration_ticks": 200,
    "barrier_start_radius": 120.0,
    "barrier_end_radius": 0.5,
    "barrier_duration_ticks": 1000,
    "barrier_auto_reset": false,
    "barrier_reset_delay_ticks": 200,
    "chunk_pregen_enabled": true,
    "chunk_pregen_radius_blocks": 0,
    "chunk_pregen_chunks_per_tick": 8,
    "chunk_preload_enabled": true,
    "chunk_preload_chunks_per_tick": 4,
    "view_distance_chunks": 0,
    "simulation_distance_chunks": 0,
    "broadcast_mode": "immediate",
    "broadcast_radius_blocks": 96,
    "default_sync_profile": "full",
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
    "mode": "full_per_chunk",
    "tick_rate": 5,
    "batch_size": 8,
    "start_delay_ticks": 60,
    "start_from_center": false
  },
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
  },
  "physics": {
    "ring_pull_strength": 0.35,
    "push_radius": 12
  }
}
```

- Scenario loads this profile, feeds values into planner/engine/effect bus.
- Nether JSON might set `mode: "slab"`, use lava particles, change push/pull numbers, and slow the scheduler cadence.
- Barrier behaviour now lives in the same JSON (`barrier_start_radius`, `barrier_end_radius`, `barrier_duration_ticks`, `barrier_auto_reset`, `barrier_reset_delay_ticks`) so each scenario can decide how aggressive its border collapse feels, when it auto-resets, and how long it lingers.
- Chunk pre-generation and preload knobs (`chunk_pregen_enabled`, `chunk_pregen_radius_blocks`, `chunk_pregen_chunks_per_tick`, `chunk_preload_enabled`, `chunk_preload_chunks_per_tick`) are also per-profile so cinematics can scale their staging workload per dimension without editing code.
- Collapse "visibility" knobs (`view_distance_chunks`, `simulation_distance_chunks`, `broadcast_mode`, `broadcast_radius_blocks`, `default_sync_profile`, `radius_delays`) now live in the same profile, so admins tune bossbar/view-distance overrides, broadcast radius/mode, and client sync defaults per dimension—Brigadier commands simply rewrite the active profile through `CommandFacade`, keeping DI + JSON as the source of truth.
- `config/the-virus-block/colors.json` keeps palette definitions (guardian beams, shield primaries, corrupted fire/water) so client renderers can request named slots instead of duplicating hex literals.
- `config/the-virus-block/logs.json` is the toggle board for channelized logging; helper utilities consult these flags before emitting `[Singularity]`, `[Fuse]`, or future scheduler logs, letting ops dial chatter up/down without a rebuild. Admins can also run `/virusblock logs list|set|reload` to inspect or update the active flags without touching disk.
- `config/the-virus-block/effect_palettes/*.json` store the data-driven FX/audio bundles (core charge/detonation, ring pulse, dissipation, collapse veil). `ScenarioEffectPalettes` parses them into runtime handlers, and `DimensionProfile.effects.palette` decides which palette a scenario installs.
- `config/the-virus-block/services.json` now owns service-level knobs: guardian push strength/duration, FX toggles, the `audio` block that lets ops mute core/ring/dissipation/collapse sounds independently while leaving particle FX enabled, and a `fuse` block (explosion delay, shell collapse ticks, pulse interval). Effect sets read both `effects` and `audio` to decide which listeners to register.
- The same `services.json` exposes a `singularity` timings block (collapse bar delay, completion hold ticks, core charge countdown, reset delay) plus `postReset` (enable toggle, queue delay, cadence, batch radius) and watchdog blocks (`watchdog.singularity`, `watchdog.scheduler`) so the host/controller and `WatchdogService` can be retuned without spelunking constants in `VirusWorldState`.
- A new `diagnostics` block (enabled flag, chunk/border sample toggles, sampling interval, log-spam thresholds) now feeds the `SingularityDiagnostics` helper, and `/virusblock singularity diagnostics …` mirrors those knobs so ops can dial collapse verbosity without touching `SingularityConfig`.
- `dimension_profiles/*` now capture erosion/fill behaviour (`water_drain.mode`, `water_drain.offset`, `water_drain.deferred.*`, `pre_collapse_water_drainage.*`, `collapse_particles`, `fill_mode`, `fill_shape`, `outline_thickness`, `use_native_fill`, `respect_protected_blocks`). The profile loader materialises a `CollapseErosionSettings` snapshot so both the main-thread collapse loop and the destruction service consume identical heuristics without touching `SingularityConfig`, and `/virusblock singularity erosion …` writes straight back to those profile files for live tuning.
- A sibling `singularity.execution` block now captures the global collapse safety knobs (`collapseEnabled`, `allowChunkGeneration`, `allowOutsideBorderLoad`) and execution strategy (mode, multithread toggle, worker count). `/virusblock singularity collapse|chunkgeneration|outsideborder|multithreaded|mode|workers` commands mutate that JSON through `CommandFacade` so operators can live-tune without touching `SingularityConfig`.

### Scenario Metadata

- `scenarios/the-virus-block/nether.json`
  - references `dimension_profile`
  - lists commands to unlock
  - optional per-tier overrides

---

## Command & Tooling Plan

- `/infection scenario list|set <dimension> <scenario>` – admin command to swap scenario.
- `/infection singularity state` – proxies into the active `SingularityController`.
- `/infection effects dump` – prints listeners & queue status (for debugging).
- `/infection profile reload` – reloads JSON configs at runtime.

Commands now route through the `CommandFacade`, which wraps `ScenarioRegistry` and
`VirusWorldState` helpers so Brigadier commands never poke host internals directly.
Scenario binding, profile reload, and singularity state inspection are already wired
under `/virusblock infection …`, and new commands can reuse the same facade.

`DimensionProfileRegistry` loads `config/the-virus-block/dimension_profiles/*.json`
on boot (creating a default Overworld profile if needed) and injects the active profile
into every `SingularityContext`. The stubbed `NetherInfectionScenario` is already
registered so Nether-specific controllers/effects can be introduced without touching
the server host once we author the new profile + effect set.

---

## Migration Path

1. ✅ **Introduce Interfaces** – core scaffolding landed (`InfectionScenario`, `ScenarioRegistry`, `SingularityController`, `SingularityContext`, `RingPlanner`, `EffectBus`, `VirusScheduler`).  
2. ✅ **Refactor Overworld Logic** – `OverworldSingularityController` plus collapse/fusing/reset handlers now drive every phase; `VirusWorldState` only exposes helper primitives.  
3. ✅ **Extract Planner + Engine** – `CollapseBroadcastManager` live. Legacy destruction engine replaced with simple `CollapseProcessor` (radius-based fill).  
4. ✅ **Wire Commands** – `/virusblock infection scenario|profile|singularity` commands flow through `CommandFacade`.  
5. ✅ **Add JSON Profiles** – `DimensionProfileRegistry` loads/creates per-dimension JSON, injects them into `SingularityContext`, and supports runtime reload.  
6. ⚒️ **Pilot Alternate Scenario** – Nether scenario scaffolding exists; needs unique controllers/effect sets/overrides.  
7. ⚒️ **Service Container & Instrumentation** – `InfectionServiceContainer`, logging/watchdog/alerting services, telemetry helpers, and config reload hooks exist; remaining work is tightening docs plus adding validation/command surfaces for new JSON (effect palettes, drainage).

---

## Phase 0 – Entry Point Inventory _(2025‑11‑30)_

| Domain | Primary call sites | Coupled responsibilities |
| --- | --- | --- |
| **Blocks & block entities** | `block/virus/VirusBlock`, `block/virus/VirusBlockProtection`, `block/core/{InfectedBlock, InfectedGrassBlock, CuredInfectiousCubeBlock}`, `block/matrix/MatrixCubeBlock`, `block/entity/{ProgressiveGrowthBlockEntity, VirusBlockEntity}`, `block/corrupted/{CorruptedStoneBlock, CorruptedGlassBlock, CorruptedDirtBlock}` | Handles virus source lifecycle (placement, registration, shield evaluation, removal), shell cooldown bookkeeping, teleport/guardian spawn triggers, progressive growth fuse orchestration, and corrupted block stage lookups. Every handler calls `VirusWorldState.get(world)` directly instead of delegating through services. |
| **Commands & gameplay systems** | `command/{VirusCommand, VirusStatsCommand, VirusDifficultyCommand, VirusDebugCommands}`, `infection/command/CommandFacade`, `item/{PurificationTotemItem, PurificationOption}`, `infection/{VirusInfectionSystem, VirusTierBossBar, VirusInventoryAnnouncements, VirusItemAlerts, BoobytrapHelper}`, `growth/scheduler/GrowthScheduler`, `TheVirusBlock` | Admin tooling, boss bars, announcements, purification totems, growth scheduler, and the mod bootstrapper read/write tier state, difficulty, event history, and scheduler queues through the monolith. |
| **Services, controllers, helpers** | `infection/controller/{AbstractSingularityController, OverworldSingularityController, phase/*}`, `infection/service/{InfectionServiceContainer, CollapseProcessor, SingularityWatchdogController, SingularityTelemetryService, GuardianFxService, SingularityHudService}`, `infection/collapse/BufferedCollapseBroadcastManager`, `infection/api/{BasicSingularityContext, SingularityContext, SingularityDependencies, BasicVirusWorldContext, VirusScheduler, EffectBus, ScenarioRegistry, SimpleScenarioRegistry}`, `infection/mutation/BlockMutationHelper`, `infection/GlobalTerrainCorruption` | Services now use DI; collapse simplified to `CollapseProcessor`. |
| **Mixins & entity hooks** | `mixin/{LivingEntityMixin, BedBlockMixin, ExplosionMixin, ZombieEntityMixin, AbstractSkeletonEntityMixin}`, `entity/FallingMatrixCubeEntity`, `block/matrix/MatrixCubeBlock` (falling entity spawn) | Gameplay mixins and custom entities query infection flags or helper methods via the singleton accessor; any API shift needs compatibility shims. |

### Safeguards & telemetry to add immediately
1. **Structured source/shell logs:** Instrument `registerSource`, `removeSource`, `collapseShells`, `stripShells`, `maybeTeleportVirusSources`, and `spawnCoreGuardians` with `InfectionLog` counters (per world + per tick) before lifting them into `VirusSourceService`.
2. **Scheduler diagnostics:** Surface `SimpleVirusScheduler` queue depth, tick duration, and pending task identifiers via `/virusblock infection scheduler dump` (CommandFacade) and emit periodic samples through `SingularityTelemetryService`.
3. **Collapse checkpoints:** Log when `CollapseProcessor` advances radii and water-drain batches finish to verify service behavior.
4. **Effect bus lifecycle tracing:** During scenario attach/detach, trace which handlers (`GuardianBeamManager`, HUD, palette listeners) register/unregister on the `EffectBus` to ensure nothing silently drops when the bus leaves the world state.
5. **Command facade assertions:** Add temporary assertions/logging inside `CommandFacade` to capture which commands mutate infection state; these breadcrumbs guide service shims later.

_With the inventory + safeguards documented we can proceed to Phase 1 (persistence/runtime split) with confidence._

## Phase 1–3 Snapshot _(2025‑11‑30 update)_

### Phase 1 – Persistence vs Runtime Split ✅
1. `VirusWorldSnapshot` DTO now owns codec persistence.
2. `VirusSchedulerService` holds the scheduler instance + fallback snapshots.
3. Singularity controllers arrive via DI (`InfectionServiceContainer#createSingularityController`).
4. Collapse simplified to `CollapseProcessor` (radius-based fill). Reset queues in `CollapseQueueService`.
5. Effect bus telemetry + guardian FX/knockback services keep FX wiring out of the host.

### Phase 2 – Service Hardening ✅
1. `EffectService` routes effect-set registration + telemetry.
2. `CollapseProcessor` handles radius-based block fills (replaces async column tasks).
3. `SingularityPresentationService` handles HUD/beams/shield messaging.
4. Scenario registry is data-driven (`config/the-virus-block/scenarios.json`).
5. Guardian knockback + spawn behavior is configurable via `services.json`.
6. Border sync now flows through the presentation layer (`createBorderSyncData` shared between host + broadcast manager).

### Phase 3 – Final Cuts (in progress)
* **Scheduler diagnostics** – command already wired to `VirusSchedulerService.diagnostics()`.
* **Remaining targets**
  - Border lifecycle service (track shrink timers + restore logic).
  - Matrix cube + shell rebuild helpers → dedicated services.
  - Tier/difficulty effects → `VirusTierService`.
  - Update `docs/cleanup_plan.md` + this blueprint with the final service map.

Once these land, `VirusWorldState` should shrink to: persistence (snapshot), DI bootstrap, and thin coordination helpers. All runtime behavior will live in services/controllers driven by the scenario.

---

## Service Inventory (current state)

| Service | Key Responsibilities | Notes |
| --- | --- | --- |
| `VirusSourceService` | Register/unregister virus cores, teleport swaps, shell collapse, guardian spawn triggers | Blocks and block entities now call into this service instead of `VirusWorldState` helpers. |
| `GuardianSpawnService` | Spawns guardian waves around cores, marks allies, logs per wave | Used by `VirusSourceService.spawnCoreGuardians`. |
| `GuardianFxService` | Knockback pulses, guardian beams, shield status/failure messaging | Pulls tuning from `services.json` (radius, strength, duration). |
| `EffectService` | Scenario effect-set registration + telemetry | Central point for effect lifecycle logging. |
| `SingularityPresentationService` | HUD/border payloads, guardian FX hot-paths | `VirusWorldState` hands it `BorderSyncData`, effect bus, etc. |
| `CollapseProcessor` | Radius-based block fill during collapse | Ring slice processing with bi-directional support, fill profiles, shape-aware throughput, operations budget, and thickness support. Managed by SingularityModule. |
| `CollapseFillProfile` | Batching strategy presets | Enum bundling shape + thickness + throughput settings. Profiles: default, column_by_column, row_by_row, vector, full_matrix, thick variants. |
| `CollapseQueueService` | Reset queues for post-collapse cleanup | Manages chunk reset after collapse completes. |
| `VirusSchedulerService` | Active scheduler instance, fallback snapshot persistence, diagnostics | `/infection scheduler status` already reports via this service. |
| `InfectionServiceContainer` | DI bootstrap / config loader | Creates the above services per-world (or globally) as needed. |

Next documentation pass will add diagrams showing how these services connect (DI container → world state → scenario/controller).

## OOP / Design Pattern Sweep Plan

1. **Service container:** Build `InfectionServiceContainer` that owns `ConfigService`, `LoggingService`, `WatchdogService`, `AlertingService`, `EffectBusFactory`, `SchedulerFactory`, etc. `VirusWorldState`, `ScenarioRegistry`, and controllers receive dependencies exclusively from this container.
2. **Config discoverability:** Replace hidden constants (ring start delay, ring duration, watchdog thresholds, audio toggle booleans) with JSON files in `config/the-virus-block/`. Each file gets reload commands + documentation.
3. **Logging/alerting:** Standardize on `LoggingService` for structured logs, `WatchdogService` for critical-path stall detection (e.g., collapse backlog, scheduler starvation), and `AlertingService` for out-of-band notifications (EffectBus events + optional webhook).
4. **Pattern sweep:** Ensure each major subsystem follows a clear pattern (Controller = State, Planner = Builder, Services = Facades, Observers via EffectBus). Remove ad-hoc singletons and move helpers behind interfaces for testability.
5. **File persistence:** Where services hold runtime state (scheduler, watchdog snapshots, alert queues), persist snapshots alongside world saves so restarts keep continuity.

Each milestone maintains parity with live behavior. Once the scaffolding ships, adding new dimensions is a matter of config plus controller/effect implementations.

---

## Future Ideas

- **Async collapse tasks** using chunk section snapshots to move erosion off-thread.
- **Per-scenario loot tables** triggered by EffectBus events.
- **Replay / Telemetry**: log `InfectionEvent`s for cinematic replays.
- **Player interaction API**: datapack-friendly hooks when players interact with virus structures.

_Last updated: 2025-12-02 (CollapseProcessor + CollapseFillProfile enhancements)_


