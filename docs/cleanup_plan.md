# Cleanup & Redundancy Audit Plan

This document tracks the repo-wide spring cleaning effort: every source set, config root, and asset tree gets reviewed, classified, and refactored (when needed) so structure matches runtime responsibilities and redundant code paths are removed.

## Objectives
- Establish a reproducible sweep process that touches every file/folder exactly once.
- Classify packages into clear domains (core runtime, dimension/scenario, client-only, config/assets, build).
- Identify redundancies, misplaced classes, and stale documentation while keeping compilation fast.
- Capture observations + suggestions so we can prioritize follow‑up refactors without losing context.

## Workflow
1. **Baseline inventory (complete)** – Captured top-level tree (config, docs, src/{client,main}, stray root artifacts).
2. **Module review loop** – For each checklist entry below:
   - Walk files in lexical order.
   - Note ownership/category; flag misplacements or duplicates.
   - Record findings in “Observations” and add actionable ideas to “Suggestions”.
   - Mark the item ✔ when every file inside has either been accepted as-is or queued for follow-up work.
3. **Redundancy resolution** – Once evidence accumulates (e.g., identical scenario implementations), open targeted PRs to consolidate or abstract.
4. **Doc/readme refresh** – Update existing readmes or add new ones (like this) as directories get finalized.

## Classification Buckets
- **Core Runtime (`src/main/java/net/cyberpunk042/infection/**`)** – Infection systems, world state, services, controllers.
- **Dimension/Scenario Layer (`.../scenario`, `.../controller`)** – Encapsulates per-dimension overrides (Overworld vs Nether, future End/custom).
- **Client Runtime (`src/client/java/...`)** – Rendering, HUD, client-only mixins.
- **Assets (`src/{client,main}/resources`)** – Models, textures, shield meshes, language files.
- **Configuration (`config/the-virus-block/**`)** – User-editable JSON knobs; must mirror profile registries.
- **Docs & Tooling (`docs/**`, root build/config files)** – Developer guidance, automation scripts.

These buckets will be referenced when we re-home classes (e.g., block entities that currently sit under `block/entity` but might belong under `blockentity/`).

## Review Checklist
| Scope | Status | Notes |
| --- | --- | --- |
| `config/the-virus-block/**` | ✔ | JSON roots match registry expectations; see notes below for per-folder follow-up. |
| `docs/**` | ✔ | Architecture + refactor readmes updated for services/palettes; other docs reviewed for drift. |
| `src/client/java` packages | ☐ | Validate rendering vs HUD separation; look for misplaced shared logic. |
| `src/client/resources` assets | ☐ | Cross-check unused shield meshes/textures. |
| `src/main/java/net/cyberpunk042/block/**` | ☐ | **In progress:** catalogued top-level classes; block entities intentionally stay under `block/entity` (per owner). No relocation required; continue sweeping remaining classes for redundancies. |
| `src/main/java/net/cyberpunk042/infection/**` | ☐ | Largest surface: proceed sub-package by sub-package. |
| `src/main/java` remaining packages (`command`, `config`, `item`, etc.) | ☐ | Confirm each has minimal public surface + doc coverage. |
| `src/main/resources` (assets + data) | ☐ | Detect stale models/tags; ensure naming matches runtime registries. |

> We will tick these boxes as each scope is fully reviewed; sub-notes will cite dates/commit hashes for traceability.

## Observations (running log)
1. **Scenario layer** (updated): Overworld and Nether scenarios now share `AbstractDimensionInfectionScenario` plus palette-driven effect sets, so duplication risk is addressed. Follow-up is ensuring future dimensions only supply profile/palette/controller wiring.
2. **Effect palettes** (new): JSON-backed palettes live in `config/the-virus-block/effect_palettes/` and feed `ScenarioEffectPalettes`. Need a validation pass to ensure default JSON stays in sync with `DimensionProfile.effects.palette`.
3. **Block folder layering:** `block/entity` contains `ProgressiveGrowthBlockEntity`, `VirusBlockEntity`, etc., even though there is a top-level `entity` package for mobs/projectiles. We should decide whether block entities live alongside regular blocks or under a dedicated `blockentity`/`be` namespace for clarity.
4. **VirusWorldState = god object:** The world state still owns persistence, infection scheduling, singularity orchestration, guardian FX, block shell management, and telemetry. This concentration keeps the file above 7k LOC and makes incremental cleanup risky.
5. **Virus block events hidden in state:** Teleportation, shell collapse, guardian spawning, and fuse guards are all implemented as private helpers inside `VirusWorldState`, so moving block classes around did not reduce coupling. We need a dedicated `VirusSourceService` (or similar) that the block + block entity can call through DI.
6. **Source lifecycle service (in progress):** Added `VirusSourceService` plus logging channel coverage so register/unregister/collapse/teleport operations are centralized. Remaining interior calls inside `VirusWorldState` should gradually switch to the service as we peel off more logic.
7. **Effect service tracking:** Scenario effect sets now register through `EffectService`, so installs/uninstalls are tracked outside the monolith. Future FX helpers (guardian beams, HUD overlays) can plug into the same registry.
8. **Collapse queues in service:** Singularity collapse/reset queues (and their delay counters) live inside `SingularityDestructionService`, with `VirusWorldState` only reaching them via helper accessors instead of owning raw `Deque`/`LongSet` fields.
9. **Presentation bridge:** Guardian pushes, shield notifications, and HUD payload broadcasts now run through `SingularityPresentationService`, so `VirusWorldState` no longer pulls `SingularityHudService` / `GuardianFxService` directly.
10. **Scenario registry config:** `config/the-virus-block/scenarios.json` defines scenario IDs + dimension bindings, so `InfectionServiceContainer` no longer hardcodes Overworld/Nether wiring.
11. **Guardian spawn service:** Core guardian waves moved into `GuardianSpawnService`, reducing `VirusWorldState` to a thin trigger that hands over the current source list.
12. **Guardian knockback configs:** Knockback radius/strength now live in `services.json` via `ServiceConfig.Guardian` so shell pushes and guardian beams can be tuned without editing `VirusWorldState`.
13. **Border lifecycle service:** `SingularityBorderService` now owns deployment, sync, reset countdowns, and snapshot state for the collapse barrier; `VirusWorldState` just asks it for `BorderSyncData` and wiring.
14. **Shell rebuild service:** `ShellRebuildService` handles collapse, cooldowns, and rebuild messaging so `VirusWorldState` simply delegates when reinforcing cores or collapsing shells.
15. **Matrix cube spawn service:** `MatrixCubeSpawnService` encapsulates spawn attempts/logging so the host only signals when to try, keeping spread logic cleaner.
16. **Tier/difficulty service:** `InfectionTierService` now owns infection tier progression, containment decay, health/difficulty scaling, and snapshot glue instead of `VirusWorldState` juggling those fields.
17. **Source state hook-up:** `VirusSourceService` now carries a dedicated `State` (sources + suppressed unregisters) so `VirusWorldState` no longer owns duplicate sets; services and scenarios can observe/extend source data through DI.
18. **Source lifecycle finalized:** Infection start, end, and containment reset routines now live inside `VirusSourceService`, leaving `VirusWorldState` to simply delegate while the service coordinates boobytrap rules, tier resets, boss bars, and terrain cleanup.
19. **Source ownership enforced:** `VirusWorldState` no longer caches `virusSources`; it queries `VirusSourceService` for views/snapshots so the service remains the sole owner of the active core set.

### `config/the-virus-block/**`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `dimension_profiles/` | ✅ | Overworld + Nether profiles include new `water_drain` + `pre_collapse_water_drainage` knobs and reference palette IDs. README explains legacy knobs but needs an addendum covering the new drainage modes. |
| `effect_palettes/` | ✅ | Two seed palettes (overworld/nether) map directly to `ScenarioEffectPalettes`. Add schema doc + validation command so missing particles/sounds fail fast. |
| `services.json` | 🔍 | Matches `ServiceConfig`; watchdog/singularity sections mirror new watchdog controller fields. Should document scheduler probe defaults in README. |
| `growth_blocks/` + `field_profiles/` + `force_profiles/` + `glow_profiles/` + `particle_profiles/` + `fuse_profiles/` + `explosion_profiles/` | ✅ | All align with `net.cyberpunk042.growth` registries. Consider a short top-level README linking each profile to its consuming class (`GrowthRegistry`, `FieldProfile`, etc.). |

## Package Review Notes
### `src/main/java/net/cyberpunk042/block`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `core/BacteriaBlock.java` | ✅ | Regular infection block; now lives under `block/core` with other infection staples. |
| `corrupted/` | ⚠️ | Folder holds 13 “CorruptedXBlock” wrappers plus `CorruptionStage`. Simple subclasses today, but likely better represented via a registry/data-driven builder; consider collapsing into a single configurable block if behavior stays identical. |
| `core/CuredInfectiousCubeBlock.java` | ✅ | Transitional block paired with `InfectiousCubeBlock`; shares the new `block/core` namespace. |
| `entity/MatrixCubeBlockEntity.java` | ⚠️ relocate | Should move under a dedicated `blockentity` package outside of `block/` for clarity. |
| `entity/ProgressiveGrowthBlockEntity.java` | ⚠️ relocate | Same concern; also has matching README under `docs/`. |
| `entity/SingularityBlockEntity.java` | ⚠️ relocate | Follows same pattern; rename package to `blockentity`. |
| `entity/VirusBlockEntity.java` | ⚠️ relocate | Ditto; part of core infection runtime so needs clearer namespace. |
| `core/InfectedBlock.java` | ✅ | Base infected state; shares `block/core`. |
| `core/InfectedGrassBlock.java` | ✅ | Derived from `InfectedBlock`; now co-located. |
| `core/InfectiousCubeBlock.java` | ✅ | Pair with cured variant; lives under `block/core`. |
| `matrix/MatrixCubeBlock.java` | 🔍 | Tightly couples to `MatrixCubeBlockEntity`; future cleanup may co-locate the entity or extract shared helpers. |
| `growth/ProgressiveGrowthBlock.java` | 🔍 | Complex orchestration block; moving it under `block/growth` keeps it near growth configs. |
| `singularity/SingularityBlock.java` | 🔍 | Connects to singularity services; new subpackage keeps singularity artifacts grouped. |
| `virus/VirusBlock.java` | ✅ | Base block for virus spread; relocated under `block/virus` alongside protections/utilities. |
| `VirusBlockProtection.java` | 🔍 | Utility class living under `block`; consider moving to `util` if not a block definition. |

#### `block/entity` subpackage
| Block Entity | Status | Notes / Follow-up |
| --- | --- | --- |
| `MatrixCubeBlockEntity` | ✅ (keep) | Pure server-side bridge that swaps the block for `FallingMatrixCubeEntity` then self-removes; tracks active cube entities per world. Structure is fine; just note it’s tightly bound to `MatrixCubeBlock`. |
| `ProgressiveGrowthBlockEntity` | 🔍 complex | 1.3k lines managing growth profiles, fuse logic, force fields, networking. Needs its own sub-readme (docs already exist). Worth splitting into components but keep under `block` for now per request. |
| `SingularityBlockEntity` | ✅ | Handles purely visual stage sequencing + client sync; references payloads/logging. No relocation needed. |
| `VirusBlockEntity` | ✅ | Manages aura tick + status effects around `VirusBlock`; ties into `VirusWorldState`. Folder placement acceptable even if parallel `entity` package exists. |

### `src/main/java/net/cyberpunk042/infection/scenario`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `GuardianBeamManager.java` | ✅ | Shared helper for guardian-style beams; used identically by both scenarios. |
| `NetherEffectSet.java` | ⚠️ duplicate structure | Logic mirrors `OverworldEffectSet` with only particle/sound constants changed—ripe for templating or parameterization. |
| `NetherInfectionScenario.java` | ⚠️ duplicate | Nearly identical to Overworld scenario; even instantiates `OverworldSingularityController`. Needs shared base or dimension-specific controller injection. |
| `OverworldEffectSet.java` | ✅ baseline | Serves as reference behavior for effect bus wiring; candidate for reuse by Nether via palette configs. |
| `OverworldInfectionScenario.java` | ✅ baseline | Current canonical implementation; should be split into reusable base + Overworld overrides when we consolidate scenarios. |

### `src/main/java/net/cyberpunk042/infection/api`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `EffectBus`, `SimpleEffectBus`, `NoopEffectBus` | ✅ | Clear interface + two implementations; might add async/backpressure doc later but structure is fine. |
| `VirusScheduler` + `SimpleVirusScheduler`/`NoopVirusScheduler` | 🔍 | Abstractions exist but only legacy tick path uses them; consider removing Noop if never injected, or documenting when scheduler should fire. |
| `VirusSchedulerTaskRegistry`, `VirusSchedulerMetrics` | 🔍 | Provide registry/telemetry hooks not yet wired in new services—verify they survive refactors or remove dead helpers. |
| `SingularityContext`, `SingularityDependencies`, `BasicSingularityContext` | ✅ | Recently updated for destruction service injection; acts as DI container for singularity controllers. |
| `VirusWorldContext`, `BasicVirusWorldContext` | ✅ | Mirror the singularity context for world-level state; double-check we don’t reintroduce VirusWorldState coupling when new services land. |
| `InfectionScenario`, `ScenarioRegistry`, `SimpleScenarioRegistry` | ⚠️ | Registry still holds only overworld/nether; eventually move to data-driven registration. `SimpleScenarioRegistry` duplicates logic from future service container. |
| `SingularityController` | 🔍 | Interface still matches legacy controller; need to document phased state machine so new controllers (Nether, custom) can plug in cleanly. |

### `src/main/java/net/cyberpunk042/infection/service`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `ServiceConfig`, `ConfigService` | ✅ | Provide JSON-backed knobs; recently extended for drainage. Need schema docs cross-linked from `config/the-virus-block/README`. |
| `InfectionServiceContainer`, `InfectionServices` | ✅ | Central DI container that now creates one destruction service per world. Still ships a hard-coded scenario registry; plan to make it data/config driven. |
| `LoggingService`, `AlertingService` | 🔍 | Lightweight wrappers; ensure callers don’t bypass them with direct `InfectionLog` access so alerts stay centralized. |
| `WatchdogService`, `SingularityWatchdogController` | ✅ | Complementary pieces—service handles probes/log spam, controller tracks per-world counters post-refactor. Consider collapsing into a single module later. |
| `SingularityTelemetryService` | ✅ | Recently gained state-change + stall snapshot helpers; acts as the single entry point for telemetry logs. |
| `GuardianFxService`, `SingularityHudService` | 🔍 | Own guardian push FX + HUD overlays; confirm they only run where needed (client vs server). |
| `SingularityDestructionService` | 🔍 complex | Wraps the destruction engine with contexts, deferred drains, hooks. Worth extracting smaller collaborators (water drain scheduler, column processors) after audit. |

### `src/main/java/net/cyberpunk042/infection/controller`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `LegacySingularityController` | ⚠️ transitional | Simply proxies back into `VirusWorldState#tickSingularity`; should be deleted once all scenarios swap to proper controllers. |
| `OverworldSingularityController` | 🔍 | Still orchestrates the entire legacy state machine; dependent on `VirusWorldState` for most behavior. Needs slimming once more logic moves into services/phase handlers. |
| `phase/DormantPhaseHandler`, `FusingPhaseHandler`, `CollapsePhaseHandler`, `ResetPhaseHandler` | 🔍 | Encapsulate chunks of the state machine but still call into many `VirusWorldState` helpers. Good staging area for extraction into standalone services. |

### `src/main/java/net/cyberpunk042/infection/collapse`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `CollapseBroadcastManager`, `BufferedCollapseBroadcastManager` | ✅ | Manage chunk-level messaging; buffered variant is default. Ensure only one implementation is needed long-term. |
| `RingPlanner`, `DefaultRingPlanner`, `RingPlan` | 🔍 | Planning interfaces remain but only Default is used. Evaluate whether planner belongs in destruction service now that rings are service-driven. |

### `src/main/java/net/cyberpunk042/infection/singularity`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `SingularityDestructionEngine` | 🔍 | Core data structure still used via service wrapper; ensure no direct `VirusWorldState` references remain. Consider splitting column scheduling vs execution logic. |
| `SingularityCollapseScheduler` | 🔍 | Multithreaded helper now called through `VirusWorldState`; verify it only depends on service APIs. |
| `SingularityRingSlices`, `BulkFillHelper`, `SingularityChunkContext` | 🔍 | Utilities for ring slice carving and chunk bypassing; may belong under the destruction service package post-cleanup. |
| `CollapseErosionSettings`, `CollapseExecutionMode`, `SingularityExecutionSettings` | ✅ | Config/data records; recently updated for water drain modes. |
| `ColumnWorkResult`, `SingularityDiagnostics` | ✅ | Diagnostics helpers and result enums; minimal action needed aside from ensuring they’re still referenced. |

### `src/main/java/net/cyberpunk042/infection/profile`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `DimensionProfile` | ✅ | Central immutable profile with nested collapse/effects/physics records; already includes new water drain + pre-collapse drainage knobs. |
| `DimensionProfileRegistry` | 🔍 | Handles JSON IO + command mutations. Still hardcodes default overworld/nether files and mix of Gson/manual parsing; consider splitting serialization helpers or migrating to Codec. |
| `CollapseBroadcastMode`, `CollapseFillMode`, `CollapseFillShape`, `CollapseSyncProfile` | ✅ | Enum-like configs used by commands + registry; no action beyond doc references. |
| `WaterDrainMode` | ✅ | New enum for directional draining; ensure commands/docs stay in sync when adding modes. |

### `src/main/java/net/cyberpunk042/infection/events`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `InfectionEvent` | ✅ | Base record for effect bus payloads; simple data holders with getters. |
| `CoreChargeTickEvent`, `CoreDetonationEvent`, `RingChargeTickEvent`, `RingPulseEvent`, `DissipationTickEvent` | ✅ | Cover the main singularity phases; used by effect sets + guardian FX. |
| `CollapseChunkVeilEvent`, `CollapseColumnStart/CompleteEvent`, `CollapseRingActivatedEvent` | 🔍 | Consumed by effect bus + telemetry; confirm these are fired through the new destruction service rather than `VirusWorldState` directly. |
| `GrowthBeamEvent`, `GrowthForceEvent`, `GrowthFuseEvent`, `GuardianBeamEvent` | 🔍 | Growth/guardian-specific events; ensure their producers remain up to date with scheduler/fx service changes. |

### `src/main/java/net/cyberpunk042/infection` (root classes)
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `VirusWorldState` | ⚠️ monolith | Still ~7k lines handling everything (tiers, shields, singularity, watchdogs, growth). Cleanup path: keep carving logic into services/controllers; document remaining responsibilities. |
| `VirusInfectionSystem`, `VirusTierBossBar`, `VirusInventoryAnnouncements`, `VirusItemAlerts`, `VirusEventType` | 🔍 | Gameplay systems layered on top of world state; once state is slimmer, see if these can depend on higher-level APIs instead of internal fields. |
| `VirusDifficulty`, `InfectionTier`, `TierFeature`, `TierFeatureGroup`, `TierCookbook` | ✅ | Represent progression/tier metadata; mostly data-driven. Need to confirm JSON/command hooks cover new features. |
| `BoobytrapHelper`, `GlobalTerrainCorruption`, `CorruptionProfiler` | 🔍 | Utility singletons; consider moving under `util/` or splitting responsibilities so infection root isn’t a dumping ground. |
| `mutation/BlockMutationHelper` | 🔍 | Still a static helper; may belong under a `mutation` service once we address redundancy. |

### `src/main/java/net/cyberpunk042/growth`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `GrowthBlockDefinition`, `GrowthRegistry` | ✅ | Drive progressive growth blocks via JSON profiles; registry loads from `config/the-virus-block/growth_blocks`. |
| `ExplosionProfile`, `FieldProfile`, `ForceProfile`, `FuseProfile`, `GlowProfile`, `ParticleProfile` | ✅ | Data containers used by growth blocks; each has defaults + sanitization logic. |
| `scheduler/` (`GrowthScheduler`, `GrowthMutation`, `GrowthOverrides`, `GrowthField`, `GrowthMutationTask`) | 🔍 | Handles growth animations/timing. Tightly coupled to `ProgressiveGrowthBlockEntity`; consider exposing hooks for reuse elsewhere. |

### `src/main/java/net/cyberpunk042/network`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `Singularity*Payload`, `SkyTintPayload`, `VoidTear*Payload`, `Growth*Payload`, `ShieldField*Payload`, `PurificationTotemSelectPayload`, `VirusDifficultySelectPayload`, `DifficultySyncPayload` | ✅ | All payloads extend Fabric networking records; serialization looks consistent. Need to ensure each has a matching handler and is documented in networking README (if any). |

### `src/main/java/net/cyberpunk042/registry`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `ModBlocks`, `ModItems`, `ModEntities`, `ModBlockEntities`, `ModStatusEffects`, `ModItemGroups` | ✅ | Standard Fabric registries; ensure block entities stay in sync if we relocate packages. |

### `src/main/java/net/cyberpunk042/command`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `VirusCommand`, `GrowthBlockCommands`, `VirusDebugCommands`, `VirusDifficultyCommand`, `VirusStatsCommand` | 🔍 | Command surface keeps expanding (water drain, pre-collapse). Check for duplication across commands and confirm `/virusblock` help text lists new flags. |

### `src/main/java/net/cyberpunk042/item`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `armor/` (`AntiVirusArmorAssets`, `AugmentedHelmetItem`, `CompositeElytraItem`, `HeavyPantsItem`, `RubberBootsItem`) | ✅ | Armor items with custom effects; verify models/textures documented under assets. |
| `PurificationTotemItem`, `PurificationTotemUtil`, `PurificationOption` | 🔍 | Totem logic ties into infection commands + payloads; ensure util stays in sync with network handlers and config knobs. |

### `src/main/java/net/cyberpunk042/config`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `ModConfigBootstrap`, `InfectionConfigRegistry` | ✅ | Entry points for loading configs; hook into Fabric loader. |
| `SingularityConfig`, `ColorConfig`, `InfectionLogConfig` | 🔍 | Provide user-facing config toggles; cross-reference `config/the-virus-block/services.json` + docs to keep knobs discoverable. |

### `src/main/java/net/cyberpunk042/util`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `InfectionLog`, `LogSpamWatchdog` | ✅ | Central logging helpers; keep them the single source to avoid ad-hoc logging. |
| `DelayedServerTasks`, `ServerRef` | 🔍 | Utility queue + server pointer; ensure tasks don’t duplicate scheduler functionality. |
| `SilkTouchFallbacks`, `VirusEquipmentHelper`, `VirusMobAllyHelper` | 🔍 | Gameplay helpers; consider relocating infection-specific logic under the infection namespace. |

### `src/main/java/net/cyberpunk042/mixin`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `*Mixin` classes | 🔍 | Mixins target core vanilla behavior (world border, chunks, entities). Need audit to ensure injection points still match Fabric 1.21.6 mappings; add README summarizing mixin intent. |

### `src/client/java/net/cyberpunk042`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `client/command/*` | ✅ | Client-only debug commands (shield mesh/visuals); ensure they remain behind dev toggles if necessary. |
| `client/render/*` | 🔍 | Large set of renderers and visual managers (growth beams, shields, singularity). Consider grouping related configs/stores under subpackages with README. |
| `client/screen/*` | ✅ | Totem/difficulty screens; double-check they sync with new config options (water drain, etc.) if needed. |
| `client/state/*` | ✅ | Client copies of scheduler/difficulty state; ensure they receive new payloads. |
| `client/sound`, `client/color` | 🔍 | Minimal/empty; confirm if sound package is placeholder. |
| `mixin/client/*` | 🔍 | Rendering mixins (weather, particles). Need mapping audit similar to main mixins. |
| `TheVirusBlockClient` | ✅ | Entry point hooking up client-side registrations. |
## Suggestions / Next Actions
1. **Scenario consolidation (complete):** Base scenario + palette registry in place; keep adding dimensions via data and shared controller hooks.
2. **Controller specializations:** Implement a profile-driven controller strategy (Nether and future dimensions can supply different erosion/drain configs without copying logic).
3. **Folder re-homing:** During the block package review, move block entities into a consistent namespace and add short README stubs to explain package intent (important for onboarding and compilation clarity).
4. **Documentation cadence:** Expand `docs/virusworld_state_refactor.md` (or add a new section) to describe the service-oriented layout we’ve moved to, so future contributors know where to put new services vs controllers.
5. **Palette validation tooling:** Add a lightweight verifier (command or gradle task) that checks `effect_palettes/*.json` against registry IDs so missing particles/sounds fail fast.
6. **VirusWorldState extraction plan:** Follow the staged plan below so singularity, scheduler, and block-source responsibilities migrate into services owned by `InfectionServiceContainer`.
7. **VirusWorldSnapshot DTO:** Persistence now flows through `VirusWorldSnapshot`, so codec load/save no longer mutate the live host directly. Future service extractions should build on that DTO instead of poking `VirusWorldState`.
8. **Collapse queues in service:** Singularity collapse/reset queues (and their delay counters) live inside `SingularityDestructionService` now, so `VirusWorldState` only reaches them through helper accessors instead of storing raw `Deque`/`LongSet` fields.
9. **Effect service tracking:** Scenario effect sets now register through `EffectService`, keeping installs/uninstalls (and logs) out of the world state. Future FX helpers can attach to the same service instead of wiring themselves manually.

### `docs/**`
| Path | Status | Notes / Follow-up |
| --- | --- | --- |
| `architecture_readme.md` | ✅ | Updated implementation status + fluid drain/palette sections to describe `SingularityDestructionService`, effect palettes, and new drainage knobs. |
| `virusworld_state_refactor.md` | ✅ | Progress snapshot, config sample, and migration steps now reflect controller abstraction, destruction service, and water-drain/palette JSON. |
| `singularity_collapse.md` | ✅ | Already covers water drain, pre-collapse drainage, and palette config—no changes needed this pass. |
| `progressive_growth_block_readme.md` | ✅ | Still aligned with current implementation; no edits required. |
| `shield_customization.md` | ✅ | Matches current command surface; keep an eye on any future mesh tooling changes. |

## VirusWorldState Cleanup Plan
1. **Phase 0 – Inventory & safeguards**
   - Freeze current behavior by documenting responsibilities + adding trace logs or metrics for shell collapse, teleportation, guardian spawns, and scheduler activity.
   - Catalog every public method that external systems call (`VirusInfectionSystem`, commands, block entities) so we know which APIs require shims during the split.
2. **Phase 1 – Persistence vs runtime split**
   - Introduce a `VirusWorldSnapshot` (or similar record) to hold only serialized data (tier flags, health, virus sources, singularity snapshots, scheduler tasks).
   - Move transient collaborators (`ScenarioRegistry`, `EffectBus`, `RingPlanner`, `CollapseBroadcastManager`, watchdog controller) into services resolved via `InfectionServiceContainer`, with `VirusWorldContext` providing accessors.
3. **Phase 2 – Source & shell services**
   - Extract teleportation, shell management, guardian spawning, and fuse cooldown logic into a `VirusSourceService`; keep `VirusWorldState` responsible only for storing source positions.
   - Update `VirusBlock` and `VirusBlockEntity` so they delegate placement/break/tick hooks to that service instead of reaching into the world state directly.
4. **Phase 3 – Singularity ownership**
   - Finalize controller abstraction by letting scenarios request `SingularityController` implementations from DI; retire `LegacySingularityController`.
   - Move collapse queues, preload/pre-gen bookkeeping, and watchdog counters into `SingularityDestructionService` (or sub-services), leaving the world state with high-level progress markers.
5. **Phase 4 – Scheduler & effect bus**
   - Re-home `SimpleVirusScheduler` and `EffectBus` wiring into standalone services so scenarios/controllers can attach/detach cleanly; ensure `AbstractDimensionInfectionScenario` becomes the only place that installs effect handlers.
   - Provide lifecycle APIs for commands/tests to inspect scheduler queues without poking `VirusWorldState`.
6. **Phase 5 – Regression + cleanup**
   - Add targeted debug commands/tests (shell collapse dry run, teleport sample, guardian spawn preview, singularity phase tick) to prove each extracted service behaves as before.
   - Delete orphaned helpers inside `VirusWorldState`, update docs/diagrams, and tick the `src/main/java/net/cyberpunk042/infection/**` checklist entry once the monolith is trimmed down.

## Upcoming Targets
- **Guardian/HUD orchestration:** Move guardian push FX + HUD updates fully into dedicated services so the host only fires events.
- **Collapse scheduler hooks:** Encapsulate `SingularityCollapseScheduler` submission/result plumbing in a service (or in `SingularityDestructionService`) so `VirusWorldState` simply triggers phase transitions.
- **Scenario registry config:** Teach `InfectionServiceContainer` to load scenario bindings from data/config to avoid hard-coded factories.
- **Effect telemetry expansion:** With `EffectService` in place, consider migrating guardian beams and future visual listeners to that registry for consistency.
- **Border/HUD service split:** Move border syncing + HUD payloads into a presentation module (partially done via `SingularityPresentationService`). Next step is extracting the ring/border timers so `VirusWorldState` just pushes state changes while the service manages boss bars + payload queuing.
- **Scheduler diagnostics:** With `VirusSchedulerService` tracking backlog, plan to expose a scheduler diagnostics command + metrics hook so we can delete the legacy watchdog plumbing inside `VirusWorldState`.

### Block Package Follow-up
- Keep block entities where they are; the cleanup focus now shifts to scanning the remaining block classes (corrupted variants, utility helpers) for redundancies/misplacements.
- Document any findings directly in this plan’s observations/suggestions rather than adding extra READMEs.

_Next step: resume the code sweep per the original plan (block subtree → remaining Java packages → assets)._ 

