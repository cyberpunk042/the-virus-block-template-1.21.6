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

---

## Scenario Abstraction (Strategy)

```java
interface InfectionScenario {
    ResourceLocation id();
    DimensionProfile profile();
    SingularityController createSingularityController(SingularityDependencies deps);
    InfectionEffectSet createEffectSet(EffectBus bus);
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

## Ring Planner (Builder)

Current logic in `prepareSingularityChunkQueue` becomes a reusable service:

```java
interface RingPlanner {
    RingPlan build(ServerWorld world, BlockPos center, RingConstraints constraints);
}

record RingPlan(List<Ring> rings, double finalThreshold, double outerRadius) { ... }
record Ring(List<ChunkPos> chunks, double threshold) { ... }
```

**Constraints JSON**
- `maxRadiusChunks`
- `skipBelowY` / `skipAboveY`
- `pattern` = ring / spiral / checkerboard

**Targets**
- Overworld: current horizon-to-center rings.
- Nether: narrower radius, weighted toward ceiling or lava sea.

---

## Destruction Engine (Service)

`SingularityDestructionEngine` already exists—next steps:
- Accept `RingPlan` input (rather than pulling from `VirusWorldState` fields).
- Provide hooks for:
  - `onColumnStart`
  - `onColumnComplete`
  - `onRingComplete`
- Support asynchronous chunk section processing in the future (off-thread tasks).

**Modes**
- `ERODE`: current column-by-column pass.
- `SLAB`: remove 2–4 layers per tick (Nether idea).
- `SPIRAL`: consume columns in spiral order for cinematic collapse.

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

Used for timed fuse flashes, delayed shell collapse, multi-phase FX. Later, persistence hooks can serialize outstanding tasks into the world save.

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
    "mode": "erode"
  },
  "effects": {
    "beam_color": "#C600FFFF",
    "veil_particles": "minecraft:sculk_soul",
    "ring_particles": "minecraft:portal"
  },
  "physics": {
    "ring_pull_strength": 0.35,
    "push_radius": 12
  }
}
```

- Scenario loads this profile, feeds values into planner/engine/effect bus.
- Nether JSON might set `mode: "slab"`, use lava particles, change push/pull numbers, and slow the scheduler cadence.
- `config/the-virus-block/colors.json` keeps palette definitions (guardian beams, shield primaries, corrupted fire/water) so client renderers can request named slots instead of duplicating hex literals.
- `config/the-virus-block/logs.json` is the toggle board for channelized logging; helper utilities consult these flags before emitting `[Singularity]`, `[Fuse]`, or future scheduler logs, letting ops dial chatter up/down without a rebuild. Admins can also run `/virusblock logs list|set|reload` to inspect or update the active flags without touching disk.

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

Commands should talk to a facade that wraps `ScenarioRegistry`, the active controller, and context providers—no direct coupling to `VirusWorldState`.

---

## Migration Path

1. **Introduce Interfaces**  
   - `InfectionScenario`, `ScenarioRegistry`, `SingularityController`, `SingularityContext`, `RingPlanner`, `EffectBus`, `VirusScheduler`.
2. **Refactor Overworld Logic**  
   - Move existing singularity code into `OverworldSingularityController`.
   - `VirusWorldState` now holds `currentScenario`, `EffectBus`, and `VirusScheduler`.
3. **Extract Planner + Engine**  
   - Adapt ring queue + destruction engine to service APIs.
4. **Wire Commands**  
   - `/virusstats`, `/virusblock ...` query scenario/controller via new interfaces.
5. **Add JSON Profiles**  
   - `dimension_profiles/overworld.json` + loader.
6. **Pilot Alternate Scenario**  
   - Stub `NetherScenario` with minimal overrides, test `mode: "slab"`.

Each milestone maintains parity with live behavior. Once the scaffolding ships, adding new dimensions is a matter of config plus controller/effect implementations.

---

## Future Ideas

- **Async collapse tasks** using chunk section snapshots to move erosion off-thread.
- **Per-scenario loot tables** triggered by EffectBus events.
- **Replay / Telemetry**: log `InfectionEvent`s for cinematic replays.
- **Player interaction API**: datapack-friendly hooks when players interact with virus structures.

_Last updated: 2025-11-28_


