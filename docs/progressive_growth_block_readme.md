# Progressive Growth Block Spec

## Owner Directions & Guardrails
- Every behaviour must be owner-defined. If a flag’s meaning is unclear, **ask first**; never invent or reuse logic without permission.
- “Growth” is literal. The block’s bounding box and rendered size must match the configured scale; if growth is disabled we freeze at the current size.
- `glowProfile` defaults to the Singularity magma glow (lava/beam/magma/glowstone are the legal presets unless the owner adds more). Glow has nothing to do with rate/targets/bounds.
- Wobble is its own system: the block drifts around its center (X/Y/Z) but always returns. Do not tie wobble to glow or collision unless the owner says so.
- New helper flags (`particleProfile`, `fieldProfile`, `hasGrowthEnabled`) exist solely to keep concerns separate; do not blend their responsibilities.
- Singularity integration remains off until every config flag is implemented, tested, and explicitly approved.
- Explosion charges are finite: every successful detonation consumes one charge, and blocks cannot be refilled once they run out.

---

## Attribute Inventory & Status

| Flag | Owner definition | Status |
| --- | --- | --- |
| `hasGrowthEnabled` | Master toggle. When `false`, freeze the block at its **current** scale (not `start`), skip growth/fuse/force logic, but keep other systems (damage, etc.) functional. | ✅ Implemented: growth + force ticks pause, but already-armed fuses keep collapsing/detonating while hazards stay active. |
| `rate`, `scaleByRate` | Tick cadence for moving `current` toward `currentTarget`. | ✅ Implemented. |
| `start`, `current`, `currentTarget` | Initial/live/target scales (owner may change at runtime). | ✅ Stored & synced (editing commands pending). |
| `min`, `max` | Hard bounds on growth. | ✅ Enforced every tick. |
| `hasCollision` | Whether entities can collide with the block; applies even when collision damage/destruction are enabled. | ✅ Shapes toggle with scale and immediately rebuild when the flag flips (even if growth is paused). |
| `isWobbly` | Independent wobble system: block oscillates around its origin on X/Y/Z but always returns. Scheduler can toggle it. | ⚠️ Bounding box + render now drift in sync; amplitude constants await owner tuning. |
| `glowProfile` | Purely visual glow bundle (legal presets: lava, beam, magma, glowstone). No particles/scheduling implied. | ⚠️ Template magma/lava/beam/glowstone profiles ship with editable textures **and** per-layer tints/animations/light levels. **Atlas contract:** every glow PNG must be listed in `assets/the-virus-block/atlases/blocks.json` so native (`use_mesh:false`) layers stitch correctly; mesh layers pull textures directly and can override animation timing in JSON. |
| `particleProfile` | Separate particle/sound preset for ambient FX (distinct from glow). | ⚠️ Profile now drives spawn interval, shape (sphere/shell/ring/column), radius/height, offsets, jitter, and optional follow-scale. Awaiting owner values. |
| `fieldProfile` | Visual aura/mesh drawn around the block (sphere, ring, etc.) independent from glow. | ⚠️ Sphere + ring meshes render with profile-defined color, scale multiplier, and spin speed; additional meshes pending owner art. |
| `hasFuse`, `fuseProfile` | Fuse countdown + visuals (default magma look); scheduler can toggle. | ⚠️ Fuse color pulses + shell collapse animation respect profile, and armed fuses now finish even if growth is disabled mid-countdown; explosion scripting still pending owner input. |
| `doesDestruction` | When true, destroy any world blocks intersecting the growth volume **even if** `hasCollision=false`. | ✅ Intersecting blocks are cleared every tick; optional fuse explosion remains. |
| `touchDamage` | Apply damage to entities intersecting the block even when `hasCollision=false`. | ✅ Volume-based damage hits entities with cooldown regardless of collision shapes. |
| `isPulling`, `pullProfile`, `pullingForce` | Owner-defined pull field (brand-new system, not the existing guardian push). `pullProfile` defines the maths, `pullingForce` scales it. | ⚠️ Force pulses now run through the dedicated solver, emit guardian beams via `GrowthBeamPayload`, and can optionally apply `impact_damage` with per-entity cooldowns; waiting on owner presets to flip to ✅. |
| `isPushing`, `pushProfile`, `pushingForce` | Same as pull but outward (new math/FX). | ⚠️ Shockwave presets (`default_push`, `shock_push`) ship with guardian beams + optional pulse damage; behaviour still needs owner tuning. |
| `explosionProfile` | Resource pointing at a detonation preset (radius, charges, damage, fire/block rules). | ⚠️ Default profile added; custom presets can scale radius + damage, set how many times the block can be re-lit (charges), and control fire/break behaviour. |
| Commands / reload | `/virusblock growth …` commands for spawn/set/info/reload plus JSON hot-reload. | ⏳ Not started. |
| Scheduler scripting | Ability to flip any flag (wobble, glow, growth, forces) from scenarios/schedulers. | ⚠️ `GrowthScheduler` + `GrowthMutation` let scenarios enqueue delayed per-block overrides (see “Scheduler Hooks”). Commands/UI still pending. |
| QA harness / singularity toggle | Dev-only workflow to test definitions before wiring to the Singularity Block. | ⏳ Not started. |

Legend: ✅ complete · ⚠️ partial/needs owner work · ⏳ not started

---

## Feature Flag Buckets (ownership recap)

| Category | Flags | Ownership notes |
| --- | --- | --- |
| Growth scalars | `rate`, `scaleByRate`, `start`, `current`, `currentTarget`, `min`, `max` | Only determine how big the block is and how fast it gets there. No other subsystem borrows these unless the owner explicitly says so. |
| Master toggles & hazards | `hasGrowthEnabled`, `hasCollision`, `doesDestruction`, `touchDamage` | Directly toggle growth, collision, destruction, and touch damage. They do not reconfigure fuse timing or visual FX. |
| Visual systems | `glowProfile`, `particleProfile`, `fieldProfile`, `isWobbly` | Purely cosmetic bundles. Each profile owns its own data (textures, alpha, meshes, sounds) and has zero gameplay implications unless stated. |
| Fuse system | `hasFuse`, `fuseProfile` | Governs arming triggers, collapse rules, required tools, pulse timing, and detonation FX. Fuse logic does **not** re-use growth scalars or force data. |
| Explosion system | `doesDestruction`, `explosionProfile` | `doesDestruction` decides whether to chew the world; `explosionProfile` defines the detonation charge/fire/break rules once destruction is enabled. |
| Force fields | `isPulling`, `pullProfile`, `pullingForce`, `isPushing`, `pushProfile`, `pushingForce` | Pull/push math and FX come strictly from the selected profile plus the scalar multiplier. |
| Tooling / control | `/growthblock …` commands, scheduler toggles, QA harness, Singularity gate | Operational controls that flip the flags above; they don’t redefine feature semantics. |

> Need another knob? Add it to the responsible profile/flag. Never piggyback on an unrelated config.

---

## Dependencies & Interactions
- `hasGrowthEnabled` gates **all** growth-only behaviour (`rate`, `scaleByRate`, `currentTarget`, `isWobbly`, `glowProfile` scaling, fuse arming, pull/push scripts). When false, the block stays at its current size, hazards remain active, and any fuse that was already armed keeps collapsing/detonating so we never strand a live shell.
- `glowProfile` is purely visual and never drives particles, wobble, or growth math. That is what `particleProfile`, `fieldProfile`, and the growth parameters are for.
- `doesDestruction` and `touchDamage` operate off the current collision volume regardless of whether `hasCollision` is enabled—the owner can keep the block intangible but still harmful.
- `particleProfile` and `fieldProfile` are new; they must be kept independent from glow so profiles remain clear (glow = emissive texture, particleProfile = ambient FX, fieldProfile = aura mesh).
- `hasCollision` is hot-swappable now: scheduler/definition flips trigger a shape rebuild even if the block is otherwise idle, so intangible vs. solid states never desync.
- Definition swaps (commands or future schedulers) now reset cooldowns, wobble offsets, and fuse state so we never inherit stale timers when the owner hot swaps a profile.
- Min/max guardrails widen any zero-span config by `1e-4` internally so growth math, force progress windows, and collapse timing never divide by zero—even if the config is malformed.
- Ambient FX respect the profile exactly: particles only spawn when `count > 0`, sound loops require an explicit `soundId`, and you can now run sound-only or fully silent profiles without dummy particles.
- Scheduler support (future) can toggle any flag, so implementations must assume flags may change mid-growth.
- Fuse logic reads only `hasFuse` plus the active `fuseProfile`; all triggers, delays, and interactions (flint-and-steel, right click, attack, auto) are data-driven through that profile, not borrowed from growth.
- `explosionProfile` is consulted whenever the fuse completes. `doesDestruction` only gates whether the explosion is allowed to break blocks (and the growth chew), not whether it explodes—damage, fire, and multi-burst behaviour always run, while block breaking only occurs when `doesDestruction=true`.
- `GrowthScheduler` is the canonical hook for scenarios/tasks. Build a `GrowthMutation`, schedule it, and the scheduler will persist/apply overrides even across restarts.

---

## Current Implementation Snapshot _(2025‑11‑29)_

### Physical Growth
| Aspect | Status | Notes |
| --- | --- | --- |
| Bounding box + collision | ✅ | Shapes rebuild every time `current` changes. |
| Visible scale | ✅ | Renderer scales glow meshes with interpolated `currentScale`. |
| Growth timing | ✅ | `rate` + `scaleByRate` drive easing with clamps. |
| Freeze behaviour (`hasGrowthEnabled`) | ✅ | Toggle freezes growth + force ticks at the live scale, keeps hazards/ambient FX active, and lets any already-armed fuse finish collapsing/detonating even if growth stays off. |
| QA coverage | ⚠️ | Commands/tests pending owner direction. |

### Config Flag Matrix
| Flag | Status | Notes |
| --- | --- | --- |
| `hasGrowthEnabled` | ✅ | Growth/fuse/force pause at current scale. |
| `hasCollision` | ✅ | Active. |
| `doesDestruction` | ✅ | Destroys overlapping blocks each tick; fuse detonation optional. |
| `hasFuse` / `fuseProfile` | ⚠️ | Baseline pulses done; profile editing + custom timing not. |
| `glowProfile` | ⚠️ | Template magma/lava/beam/glowstone presets load; awaiting owner-provided textures/values. Remember to update the glow atlas (`assets/the-virus-block/atlases/blocks.json`) whenever a glow PNG is added/replaced so the native renderer doesn’t fall back to the debug sprite. |
| `particleProfile` | ⚠️ | Interval, shape, offsets, jitter, and follow-scale now come from the profile; awaiting owner-tuned numbers/assets. |
| `fieldProfile` | ⚠️ | Sphere + ring meshes render with per-profile tint/scale; additional meshes and art pending owner input. |
| `isWobbly` | ⚠️ | Bounding boxes + renderer now wobble via sine offsets; constants pending owner tuning/scheduler control. |
| `isPulling` / `pullProfile` / `pullingForce` | ⚠️ | Dedicated solver + guardian beams + optional `impact_damage` per profile, plus new ring behaviors + field bindings; still needs owner-curated presets. |
| `isPushing` / `pushProfile` / `pushingForce` | ⚠️ | Shockwave + ring-capable presets exist (guardian beams, damage pulses, growth window gating); waiting on owner sign-off. |
| `touchDamage` | ✅ | Damage + cooldown live. |
| Commands / reload | ⏳ | Not started. |
| Scheduler scripting | ⏳ | Not started. |
| QA harness & dev toggle | ⏳ | Not started. |
| Singularity integration | 🚫 | Blocked until QA harness + owner approval. |

### Supporting Infrastructure
| Area | Status | Notes |
| --- | --- | --- |
| Registry + defaults | ✅ | `GrowthRegistry` loads/writes JSON defaults. |
| State persistence | ✅ | Scale/force/fuse + ambient cooldowns and wobble offsets now serialize so reloads resume mid-phase. |
| Effect bus | ✅ | `GrowthForceEvent` / `GrowthFuseEvent` emit metadata. |
| Renderer | ✅ (baseline) | Magma glow + wobble + fuse pulse, `GrowthBeamRenderer` for guardian beams, and `GrowthRingFieldRenderer` for force-bound field meshes. |
| Commands & diagnostics | ⏳ | Awaiting owner-defined UX. |
| Scheduler / diagnostics | ⏳ | To be added after owner specifies scripts. |

---

## Next Steps (awaiting owner direction)
1. Expose `hasGrowthEnabled` + profile toggles through `/growthblock …` commands and JSON reload tooling.
2. Let the owner tune particle/field/force presets (now that the runtime honors every knob) and capture golden configs.
3. Build `/growthblock …` diagnostics/hot-reload + scheduler helpers so scenarios can swap profiles mid-run.
4. Create the QA harness/dev toggle before touching the Singularity Block.
5. **Investigate beam mesh animation**: `scroll_speed` overrides + UV offsets are still not producing visible motion. Need a top-down render pass review (layer state, buffer contents, UV math) before assuming the shader path works.
6. Always escalate questions—no feature is assumed without explicit owner instructions.
# Progressive Growth Block Spec

This document captures the canonical requirements for the new progressive growth
block. The Singularity Block is **only** used as a late-stage harness once this
system is fully abstracted and independently verified; we do **not** wire the
new logic into the Singularity Block until the feature is complete and ready for
integration testing. All code that lands for this feature should reference this
README so we stay aligned on behaviour and attribute semantics.

> **Important guardrails**
> - “Growth” means the **physical block model and bounding box change size** according to the profile. There is no symbolic growth; the block must visibly scale and adjust collision bounds exactly as defined.
> - `glowProfile` defaults to the **singularity-style magma glow** (same texture bundle + emissive effect) until additional profiles are authored. Reuse those textures/animations unless explicitly directed otherwise.
> - All feature definitions come from the owner. **Do not invent behaviour.** If a field’s meaning is unclear, stop and ask for direction before coding.

---

## Attribute Inventory

| Attribute | Definition |
| --- | --- |
| `rate` | Base number of ticks between scale updates. Lower = faster growth. |
| `scaleByRate` | Multiplier applied to `rate` so dimension/game-mode tuning can throttle growth globally. |
| `start` | Initial normalized scale (0–1) when the block entity spawns. |
| `current` | Live scale value used for collision boxes, render size, and FX. Synced to clients every tick. |
| `currentTarget` | The scale the block is currently easing toward. Updated whenever scripts or player interactions advance stages. |
| `min` | Minimum allowed scale. Prevents negative shrinkage and sets the smallest bounding box. |
| `max` | Maximum allowed scale. Hitting this usually arms the fuse when `hasFuse` is true. |
| `hasCollision` | When true, hitboxes expand/shrink with `current`. False turns the block into a non-solid hazard that still deals touch damage. |
| `doesDestruction` | Toggles whether the block triggers world destruction (Singularity mini-plan, explosions, etc.) when the fuse finishes. |
| `hasFuse` | Enables the fuse subsystem. When true, we consult the active `fuseProfile.trigger` (auto, tool use, attack, scheduler, etc.) to decide how/when to arm—never by borrowing growth scalars. |
| `glowProfile` | Resource identifier pointing at glow materials/particles (ex: `the-virus-block:magma`). Profiles pick textures, emissive colors, mesh-only animation overrides, and the emitted light level so we can swap glows per block. |
| `isWobbly` | Adds a sine-based wobble to both visuals and collision boxes. Think guardian laser “charging” shimmy. |
| `isPulling` | Enables inward forces while the block grows or fuses. Behaviour comes from the selected `pullProfile` plus the `pullingForce` scalar. |
| `isPushing` | Enables outward knockback bursts. Behaviour comes from the selected `pushProfile` plus the `pushingForce` scalar. |
| `pullingForce` | Scalar (0–1+) layered on top of the pull profile. Higher numbers yank harder and affect a wider radius. |
| `pushingForce` | Scalar multiplier applied to the push profile impulse. |
| `pullProfile` | Resource ID referencing `config/the-virus-block/field_profiles/<name>.json` (or the shared `field_profile`) that defines falloff curves, particles, guardian-beam usage, etc., for pull fields. |
| `pushProfile` | Same as `pullProfile` but for outward forces. Lets us define “shockwave”, “gentle nudge”, or any custom behaviour. |
| `fuseProfile` | Resource ID referencing `config/the-virus-block/fuse_profiles/<name>.json`. Profiles own triggers, required tools, countdown timings (`explosion_delay`, `pulse_interval`, `shell_collapse_ticks`), collapse targets, colors, particles, and sounds. Falls back to `ServiceConfig.Fuse` for missing values. |
| `explosionProfile` | Resource ID referencing `config/the-virus-block/explosion_profiles/<name>.json`. Profiles define blast radius/damage, how many times the block can be re-lit (charges, no refill), how many explosions fire per charge (`amount` + `amount_delay_ticks`), and whether the blast lights fires or breaks blocks when `doesDestruction=true`. |
| `touchDamage` | Amount of damage applied per tick to entities touching the block (even if collision is off). Scales with difficulty unless explicitly disabled. |

All attributes must be exposed in the data definition so modpack authors can
author new blocks without recompiling.

---

## Data Model

1. **Definition JSON**  
   - Stored under `config/the-virus-block/growth_blocks/*.json`.  
   - Every definition declares the attributes above plus optional cosmetics (textures, particles, sounds).  
   - Blocks reference a definition by `ResourceLocation`, allowing us to reuse the same runtime class for multiple behaviours.

2. **Runtime State**  
   - `start`, `min`, `max` seed the block entity the moment it loads.  
   - `current` is clamped to `[min, max]` and synced to the client.  
   - `currentTarget` drives easing; the server lerps `current` toward the target using `rate * scaleByRate` as the tick interval.  
   - Growth stages (idle → growing → fusing → destruct) live in the block entity so they survive chunk unloads.
   - Force/fuse/pull/push/ambient cooldowns plus wobble offsets now serialize so reloads resume exactly where the block left off.

3. **Profile Libraries**  
   - **Force:** `config/the-virus-block/force_profiles/*.json` continue to drive pull/push behaviour (radius, falloff, guardian beams, FX) and now include `start_progress` / `end_progress` plus `edge_falloff` to taper impulses near the perimeter. `pullingForce` / `pushingForce` simply scale the resolved impulse.  
   - **Field (visual shell):** `config/the-virus-block/field_profiles/*.json` describe the aura mesh/texture combos rendered around the block (independent of glow).  
   - **Particles:** `config/the-virus-block/particle_profiles/*.json` keep ambient particle/audio bundles separate from glow, including per-profile sound IDs + intervals so loops can be swapped without code changes.  
   - **Fuse:** `config/the-virus-block/fuse_profiles/*.json` pick countdown colors, textures, particles, audio, and per-phase timings, inheriting any missing fields from `ServiceConfig.Fuse`.  
   - **Glow:** `config/the-virus-block/glow_profiles/*.json` now ship with editable templates (`magma.json`, `lava.json`, `beam.json`, `glowstone.json`) that point at extracted textures under `assets/the-virus-block/textures/block/glow_*.png`. Replace those files or adjust alpha/spin to author new looks.

4. **Scheduler Integration**  
   - Rates and fuse timers use `VirusScheduler` tasks so they persist across saves.  
   - The block entity only recalculates scale when its scheduled tick fires, reducing per-tick overhead when many blocks exist.

---

## Profile Schemas & Planned Fields

Every profile is the single source of truth for its subsystem. If the owner needs another knob, we extend the matching profile here rather than borrowing growth configs.

### Glow Profile (`config/.../glow_profiles/*.json`)
| Field | Purpose |
| --- | --- |
| `id` | Resource location (`the-virus-block:magma`). |
| `primary_texture`, `secondary_texture` | Emissive layers used by the renderer. |
| `primary_color`, `secondary_color` | Hex RGB tint applied to each layer (affects both mesh and native paths). Defaults to white if omitted. |
| `primary_mesh_animation`, `secondary_mesh_animation` | Optional overrides for mesh layers only (frame time, interpolate flag, explicit frame list). If omitted, mesh layers follow the texture’s `.mcmeta` just like native layers. Native (`use_mesh:false`) paths always use the atlas animation; set this only when you intentionally run a layer through the mesh renderer. |
| `light_level` | Emitted block light (0–15). Stored back onto the block state so both mesh/native paths share the same brightness. |
| `primary_alpha`, `secondary_alpha` | Opacity per layer (lives in the opacity profile today, but glow entries may override in the future). |
| `spin_speed` | Rotation speed. |
### Glow Pipeline History & Gotchas

1. **Atlas stitching (initial breakage).** `use_mesh:false` layers rely on the block atlas (`SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE`). When we first promoted the custom glow strips, they lived under `textures/misc/...`, never entered the block atlas, and the native cube immediately drew the magenta debug texture (`minecraft:missingno`). Fix: move the PNGs under `assets/the-virus-block/textures/block/` and list each one in `assets/the-virus-block/atlases/blocks.json`.  
   - Sanity step: the renderer logs `"[GrowthRenderer] Atlas contains ..."` at startup. If you ever see `minecraft:missingno`, a PNG was moved/renamed without updating the atlas.

2. **use_mesh:true vs. .mcmeta.** After the move, mesh layers started rendering from `textures/block/...` but still respected the `.mcmeta` inside each PNG. Copying vanilla magma/lava strips gave us animation, but we quickly hit the “works here, breaks there” loop:  
   - `use_mesh:true` worked because the renderer slices the strip manually.  
   - `use_mesh:false` still relied on atlas animations.  
   - Any tweak to the JSON required editing PNG metadata, reloading resources, and praying we didn’t desync with the atlas.  
   To break that loop we added `primary_mesh_animation` / `secondary_mesh_animation` so mesh layers can override frame timing in JSON (frametime, interpolation, explicit frame list). **These overrides only affect mesh layers**; native layers still trust the `.mcmeta` baked into the atlas.

3. **Beam confusion.** Vanilla beacon textures (`entity/beacon_beam.png`) aren’t frame strips; the shader scrolls their UVs. When we copied them to our block namespace and removed the `.mcmeta`, their animation appeared to break even though they were static gradients all along.  
   - If you want motion, set `primary_use_mesh=true` and add a mesh animation override (e.g., 16-frame scroll).  
   - If you leave them native, the atlas will keep them static (matching vanilla).
4. **Legacy `textures/misc/...` references.** Early glow profiles (and some configs in the wild) still pointed at `the-virus-block:textures/misc/glow_*`. Once the PNGs moved under `textures/block`, those paths immediately returned `missingno` even though the atlas was correct. The registry now ships updated defaults, but any *existing* config files under `config/the-virus-block/glow_profiles` must be updated or deleted so they regenerate. The renderer logs `"[GrowthRenderer] Atlas returned missing sprite for ... textures/misc/glow_*"` whenever a legacy config slips through—treat those logs as “fix the JSON,” not “tweak code.”  
5. **Config coverage.** The registry now writes defaults for the new glow profiles (`lava_primary_mesh`, `beam_primary_mesh`) and the corresponding growth blocks (`lava_block`, `lava_mesh_block`, `beam_block`, `beam_mesh_block`, plus the two supersized variants). If `/growthblock give …` ever reports “unknown definition,” first look at `GrowthRegistry.ensureDefaults()`—if it doesn’t emit the JSON, neither dev nor prod clients will pick it up.
6. **Special item renderer (in-hand / dropped).** The progressive growth item now piggybacks the block-entity renderer through the 1.21 special-model pipeline:
   - The JSON under `assets/the-virus-block/items/progressive_growth_block.json` still needs a `base` model (`assets/the-virus-block/item/progressive_growth_block.json`). When that file was missing the item never reached our renderer and stayed invisible.  
   - `BlockEntityRenderDispatcher.render(..)` performs a distance check using the block entity’s world position. Our fake entity lives at `(0,0,0)` while the player can be thousands of blocks away, so the dispatcher constantly skipped rendering. We now fetch the registered `BlockEntityRenderer` directly and call `render(..., Vec3d.ZERO)` to bypass the cull.  
   - Special renderers do **not** apply the standard display transforms, so the block looked gigantic both in-hand and in the inventory icon. We now apply the same transforms vanilla uses in `assets/minecraft/models/block/block.json` (GUI = 0.625 scale @ 30°/225°, ground = 0.25 scale with a +3px lift, third-person = 0.375 scale @ 75°/±45°, first-person = 0.4 scale @ 45°/225°). This single helper fixed the icon, hand pose, and dropped-block scale in one shot.
7. **Beam mesh animation ≠ frame slicing (still unsolved).** Vanilla beacon beams do not ship as tall strips; the shader scrolls a single 16×16 gradient down the column. Our first mesh renderer only knew how to slice stacked frames, so the beam either froze or smeared when we reused the vanilla art.  
   - Glow profile overrides now support `scroll_speed` alongside `frame_time`/`frames`, so mesh layers can request a shader-style UV scroll instead of frame hopping.  
   - `beam_primary_mesh.json` / `beam_secondary_mesh.json` ship with `scroll_speed: 0.02` to mimic the vanilla downward motion.  
   - **Known gap:** even with scroll offsets wired up, the in-game beam still renders static. Atlas paths, JSON overrides, and mapV math all look correct, so there’s another missing piece (likely render state or how the mesh layer batches vertices). Leave this as an open bug; do not expect beam mesh animation to work until we diagnose the remaining issue.

4. **Light levels.** Glow brightness used to be hardcoded (`.luminance(state -> 10)`). Now `GlowProfile.light_level` writes directly to the block state property `ProgressiveGrowthBlock.LIGHT_LEVEL`, so swapping glow profiles adjusts both mesh and native paths’ brightness instantly. This also keeps `/growthblock …` definitions self-contained: glow profile selection now controls textures, mesh animation, tint, and lighting in one place.

5. **Config coverage.** The registry now writes defaults for the new glow profiles (`lava_primary_mesh`, `beam_primary_mesh`) and the corresponding growth blocks (`lava_mesh_block`, `beam_mesh_block`). If you add another preset, extend `ensureDefaults()` so dev clients auto-generate the JSON; otherwise `/growthblock give …` will keep returning “unknown”.

> **Troubleshooting loop (documented “aha” moments):**  
> - “It works here but not there” → check atlas logs; native layers need the PNG listed in `atlases/blocks.json`.  
> - “Mesh layer is stuck/mis-timed” → confirm `.mcmeta` matches the strip; if you want custom timing, add `primary_mesh_animation` or `secondary_mesh_animation`.  
> - “Beam texture stopped animating” → vanilla beams rely on shader UV scrolls, not `.mcmeta`; switch to mesh mode + animation override if you need movement.  
> - “Light level won’t change” → ensure the glow profile’s `light_level` is set and the block entity can write to `LIGHT_LEVEL` (state property must exist; we added it in `ProgressiveGrowthBlock`).  
> - “Native layer still shows `misc/glow_*` errors” → delete/regen the old glow profile JSON so the registry writes the new `textures/block/...` paths.  
> - “New growth block can’t be summoned” → verify `GrowthRegistry.ensureDefaults()` emits the JSON under `config/the-virus-block/growth_blocks` (we now include `lava_block`, `lava_mesh_block`, `beam_block`, `beam_mesh_block`, `magma_supersized_destruction`, `magma_supersized_no_collision`).  

### Sphere Model Generation (Non-Cubic Blocks)

The progressive growth block can optionally use a **spherical JSON model** instead of a cube for item/particle rendering. This is useful for orb-like growth blocks or items.

#### `SphereModelGenerator` Usage

```java
import net.cyberpunk042.sphere.SphereModelGenerator;
import com.google.gson.JsonObject;
import java.nio.file.Path;

// Generate a spherical block model for growth blocks
JsonObject model = SphereModelGenerator.forGrowthBlock(
    "the-virus-block:block/growth_glow",  // texture
    0.8                                    // radius (1.0 = full block)
);

// Save to resources
SphereModelGenerator.saveToFile(model,
    Path.of("src/main/resources/assets/the-virus-block/models/block/growth_sphere.json"));
```

#### Algorithm Types

| Type | Method | Elements | Best For |
|------|--------|----------|----------|
| `TYPE_A` | Overlapping cubes | 10-15 | Accurate, close-up viewing |
| `TYPE_E` | Rotated rectangles | 40-60 | Efficient, many instances |

```java
// Type A (default) - smoother, more accurate
JsonObject sphereA = SphereModelGenerator.builder()
    .typeA()
    .radius(0.8)
    .verticalLayers(6)      // 4-8 recommended
    .horizontalDetail(4)    // 2-4 recommended
    .texture("the-virus-block:block/glow_magma_primary")
    .shade(false)           // Usually looks better off
    .build();

// Type E - fewer elements, still round
JsonObject sphereE = SphereModelGenerator.builder()
    .typeE()
    .radius(0.8)
    .verticalLayers(6)
    .texture("the-virus-block:block/glow_magma_primary")
    .build();

// Item with display transforms
JsonObject itemSphere = SphereModelGenerator.builder()
    .typeA()
    .radius(0.5)
    .verticalLayers(4)
    .texture("the-virus-block:item/orb")
    .scale(0.6, 0.6, 0.6)      // GUI/hand scale
    .translation(0, 4, 0)       // Position offset
    .build();
```

#### Element Count Estimation

```java
// Before generating, estimate complexity
int typeACount = SphereModelGenerator.estimateTypeAElements(6, 4);  // ~10-15
int typeECount = SphereModelGenerator.estimateTypeEElements(6);     // ~48
```

#### Integration with Growth Block

1. Generate the model during build or at runtime
2. Reference it in your blockstate JSON:
   ```json
   {
     "variants": {
       "": { "model": "the-virus-block:block/growth_sphere" }
     }
   }
   ```
3. The `BlockEntityRenderer` still handles dynamic rendering; the JSON model is used for:
   - Item form (inventory, dropped)
   - Particles
   - Block breaking animation

> **Note:** The `SphereModelGenerator` creates **static JSON models**. For dynamic runtime sphere rendering (fields, shields), use `TypeASphereRenderer` or `TypeESphereRenderer` from `net.cyberpunk042.client.visual.render.sphere`.

---

### Asset Sourcing & Path Hygiene
- **Vanilla capture:** All glow strips (magma, lava still/flow) and beam/glowstone textures are copied directly from the Minecraft client jar (`minecraft-clientOnly-...jar`). Each PNG sits under `assets/the-virus-block/textures/block/` with matching `.mcmeta` for the strips we animate. When updating or adding art, pull it from the same jar so animation dimensions stay correct.  
- **Reference paths:** Every glow profile must reference `the-virus-block:textures/block/<name>.png` for both `primary_texture` and `secondary_texture`. Legacy `textures/misc/...` references will render magenta even though the PNG exists.  
- **Atlas updates:** Whenever you add/rename a glow PNG, update `assets/the-virus-block/atlases/blocks.json` so the native path can stitch it. Mesh layers do not need this, but native ones do.  
- **Config regeneration:** `GrowthRegistry.ensureDefaults()` writes canonical JSON under `config/the-virus-block` when files are missing. If you change a glow profile/growth block in the repo, delete the corresponding file from your runtime `config` directory so the new defaults regenerate; otherwise old files keep the wrong texture paths or IDs.  
- **Runtime checks:** The renderer logs `Layer ... mode=BLOCK_NO_TEXTURE` with `texture=the-virus-block:textures/misc/...` whenever a config falls out of sync. Treat those logs as “update the JSON,” not “patch the renderer.”  
- **use_mesh toggles:** Only set `primary_use_mesh=true` / `secondary_use_mesh=true` when you intentionally want manual UV slicing. Mesh layers read textures directly (no atlas), honor JSON animation overrides, and support per-layer tints; native layers require atlas registration and `.mcmeta` timing.

| *(Future)* `pulse_curve` | Additional owner-driven cosmetics. |

### Particle Profile (`config/.../particle_profiles/*.json`)
| Field | Purpose |
| --- | --- |
| `particle` | Ambient particle identifier. |
| `count`, `speed` | Spawn amount + motion. |
| `sound`, `sound_interval` | Optional looping ambience. |
| `interval_ticks` | How often the profile spawns particles. |
| `shape` | `sphere`, `shell`, `ring`, or `column` for spawn geometry. |
| `radius`, `height` | Geometry parameters (ring/cylinder radius, column/ring height). |
| `offset_x/y/z` | Base offset from block center. |
| `jitter_x/y/z` | Randomized perturbation per axis. |
| `follow_scale` | Whether radius/height scale with the block’s current size. |
| *(Future)* `volume_curve` | Reserved for sound falloff once the owner requests it. |

### Field Profile (`config/.../field_profiles/*.json`)
| Field | Purpose |
| --- | --- |
| `mesh` | Aura geometry (sphere, ring, lattice, etc.). |
| `texture` | Texture applied to the mesh. |
| `alpha`, `spin_speed` | Visual tuning. |
| `scale_multiplier` | Scales the mesh relative to the block’s size. |
| `color` | Hex tint applied before blending (ring meshes respect this too). |

### Force Profile (`config/.../force_profiles/*.json`)
| Field | Purpose |
| --- | --- |
| `interval_ticks` | Cooldown between pulses. |
| `radius`, `strength`, `vertical_boost`, `falloff` | Core force math. |
| `start_progress`, `end_progress` | Optional growth windows when the force is active. |
| `edge_falloff` | Additional taper near the boundary. |
| `guardian_beams`, `beam_color` | Whether to emit guardian-style beams + their tint. |
| `particle`, `particle_count`, `particle_speed`, `sound` | FX when the pulse fires. |
| `impact_damage`, `impact_cooldown_ticks` | Optional per-pulse damage and its cooldown so forces can sting independently of `touchDamage`. |
| `ring_behavior` | `none`, `keep_on_ring`, `keep_inside`, or `keep_outside`. Determines how the solver reacts when entities leave the allowed band. |
| `ring_count`, `ring_spacing`, `ring_width`, `ring_strength` | Describe how many concentric bands exist, how far apart they sit, how thick each band is, and how hard the solver pushes entities back toward compliance. |
| `ring_field_profiles` | Optional list of `fieldProfile` IDs to render (one per ring) whenever this force fires. |
| *(Future)* `trigger` | Owner-defined conditional windows (daytime, fuse stage, etc.). |

> Default bundles now include `default_pull`, `default_push`, `vortex_pull` (guardian beam + damage pulses), `shock_push`, and the new `ring_hold` profile that keeps entities locked to a configurable annulus. All of them remain editable JSON templates under `config/the-virus-block/force_profiles`.

### Fuse Profile (`config/.../fuse_profiles/*.json`)
| Field | Purpose |
| --- | --- |
| `trigger` | How the fuse arms (`auto`, `flint_and_steel`, `right_click`, `attack`, scheduler hook, etc.). |
| `auto_progress` | Growth progress (0–1) required before an `auto` trigger can arm. Lives in the fuse profile so it stays fuse-owned. |
| `requires_item`, `allowed_items` | Optional tool gating (e.g., flint-and-steel only). |
| `consume_item` | Whether to consume/damage the arming item when the fuse starts. |
| `explosion_delay` | Ticks from arming to detonation. |
| `pulse_interval` | Ticks between cosmetic pulses. |
| `shell_collapse_ticks`, `collapse_target` | Controls the implosion animation (target scale defaults to `min`). |
| `primary_color`, `secondary_color` | Pulse tinting. |
| `particle`, `sound` | FX used during pulses/detonation. |
| *(Future)* `post_delay_actions`, `multi_stage` | Hooks for chaining additional owner-defined behaviors. |

> Fuse behavior is entirely data-driven by this profile. If we need a new interaction (e.g., “requires redstone pulse”), we will add a field here instead of touching growth scalars.

### Explosion Profile (`config/.../explosion_profiles/*.json`)
| Field | Purpose |
| --- | --- |
| `radius` | Vanilla explosion radius/power passed to the engine. |
| `charges` | Number of times the block can be re-lit before it burns out (legal range: 1 → ∞, no refill). |
| `amount` | Number of individual explosions that fire per charge. |
| `amount_delay_ticks` | Ticks between each explosion in a single charge. |
| `max_damage` | Maximum entity damage applied at the center. Can be any positive value (including `∞`). |
| `damage_scaling` | Controls how quickly damage falls off with distance (1 = linear, >1 = faster falloff). |
| `causes_fire` | Whether the detonation ignites surrounding blocks. |
| `breaks_blocks` | Whether the explosion actually removes world blocks (vs. pure cosmetic). |
| *(Future)* `damage_types`, `post_actions` | Additional knobs once the owner specifies them. |

---

## Scheduler Hooks

- `GrowthScheduler.registerSchedulerTasks()` is invoked during mod init. Scenarios can call `GrowthScheduler.schedule(world, pos, mutation, delayTicks)` to enqueue mutations on the infection scheduler; tasks persist via `VirusSchedulerTaskRegistry`.
- Use `GrowthMutation` + `GrowthField` to describe the overrides you want. Every field from `GrowthBlockDefinition` is addressable (`GROWTH_ENABLED`, `HAS_COLLISION`, `GLOW_PROFILE`, `PULLING_FORCE`, `EXPLOSION_PROFILE`, etc.). Call `setBoolean`, `setDouble`, `setIdentifier`, or `clear(field)` to revert back to the definition value.
- `GrowthScheduler.applyNow(world, pos, mutation)` exists for immediate toggles (no scheduler delay) but still writes overrides so the change persists.
- Example (Java):
  ```java
  GrowthMutation mutation = new GrowthMutation()
      .setBoolean(GrowthField.GROWTH_ENABLED, false)
      .setIdentifier(GrowthField.GLOW_PROFILE, Identifier.of("the-virus-block", "beam"))
      .setDouble(GrowthField.PULLING_FORCE, 2.5D);
  GrowthScheduler.schedule(world, blockPos, mutation, 40); // apply after 2 seconds
  ```
- Overrides live per block-entity; swapping definitions or reloading JSON keeps the scheduler state intact so long as the block stays loaded.

## Behaviour Buckets

### Existing Features We Will Reuse

| Capability | Helper | Notes |
| --- | --- | --- |
| Pull maths | `ProgressiveGrowthBlockEntity.applyForce` | Custom solver samples radius/falloff windows, edge easing, and applies guardian beams + optional damage pulses per profile. |
| Push maths | `ProgressiveGrowthBlockEntity.applyForce` | Same solver handles outward shocks; helper methods merely differentiate direction and growth gating. |
| Wobble animation | `SingularityBlockEntity` + client renderer (`client/resources/assets/the-virus-block/singularity/visual.json`) | The singularity renderer already scales/wobbles; we’ll generalize it so growth blocks ride the same matrices. |
| Glow textures | Block models like `assets/the-virus-block/models/block/corrupted_stone.json` (uses `#glow` emissive layers) and the singularity textures (`textures/misc/singularity_sphere_*.png`) | `glowProfile` points at one of these preset bundles (magma, void, radiant, etc.) or any new JSON entry we add under `config/.../glow_profiles/`. |
| Fuse timing | `ServiceConfig.Fuse` (loaded via `config/the-virus-block/services.json`) | Serves as the default backing values for any `fuseProfile` that omits fields. |
| Scheduler persistence | `SimpleVirusScheduler` + `VirusSchedulerTaskRegistry` | Growth ticks/fuse timers enqueue tasks here so they survive restarts. |
| Effect buses | `GuardianBeamManager` listens for `GuardianBeamEvent` | We’ll add `GrowthPulseEvent`/`GrowthFuseEvent` to the same bus so FX stay data-driven. |

### Collision & Damage

- Collision boxes lerp with `current`. When `hasCollision=false`, the block acts
  like a harmful cloud: no physical barrier, but `touchDamage` still fires via
  `onEntityCollision`.
- Damage type defaults to a custom “viral burn” source. Definitions can opt into
  vanilla damage sources for compatibility with datapacks.

### Visual Flags

- `glowProfile` selects the material/texture set for the renderer and spawns the
  appropriate particle/sound preset while `current` is above `start`. The profile
  names map to existing emissive bundles (corrupted block glow JSON, singularity
  sphere textures) or new entries we drop alongside the definition. Unless told
  otherwise, **use the magma-style singularity bundle for defaults** so we keep
  continuity with the current look.
- `isWobbly` now shifts both the rendered glow and the live voxel shapes using a
  sine-based offset so the block physically jitters around its center. Amplitudes
  are constant placeholders until the owner dials them in.
- `fieldProfile` will draw the larger aura/mesh (sphere, beam lattice, etc.) around the block once the owner specifies meshes/textures—separate from glow so visuals stay modular.
- `particleProfile` now decides interval, spawn shape (sphere, shell, ring, or column), radius/height, offsets, jitter, and whether the effect scales with the block. Setting `count=0` still disables particles while keeping the sound loop alive.
- `fieldProfile` controls the larger aura/mesh (sphere or ring today) with its own texture, alpha, spin speed, scale multiplier, and color tint. Multiple force profiles can temporarily bind additional field visuals via `ring_field_profiles` without replacing the block’s base aura.


### Forces

- `pullProfile`/`pushProfile` now drive a *dedicated* force-field solver inside `ProgressiveGrowthBlockEntity`. Each pulse samples the configured radius, falloff, edge easing, start/end progress gates, and `pullingForce`/`pushingForce` scalar to compute a smooth impulse. Nothing calls the legacy guardian push helpers anymore.
- Guardian-style beams now travel over a dedicated `GrowthBeamPayload` channel; the client-side `GrowthBeamRenderer` listens for the packet and draws a beam that tracks the target entity as it moves (with fallback positions when nothing is present). EffectBus still receives `GrowthBeamEvent` for server-side listeners.
- Ring bands: set `ring_behavior` + (`ring_count`, `ring_spacing`, `ring_width`, `ring_strength`) to keep players on, inside, or outside an annulus. Bands override the default inward/outward direction and can coexist with guardian beams + impact damage.
- Each ring may optionally emit its own field visuals via `ring_field_profiles`. The server sends `GrowthRingFieldPayload` and the client-side `GrowthRingFieldRenderer` draws the bound meshes so rings remain visible without replacing the block’s primary `fieldProfile`.
- Profiles gain optional `impact_damage` + `impact_cooldown_ticks` fields so a force pulse can apply damage on contact without reusing `touchDamage`. The runtime enforces per-entity cooldowns so pulses do not spam damage every tick.
- `GrowthForceEvent` still fires once per pulse so downstream FX/audio can keep in sync with the active profile.

### Fuse & Destruction

- When `hasFuse=true`, the runtime defers entirely to the active `fuseProfile.trigger` to decide how/when to arm (auto, scheduler, flint-and-steel, right-click, attack, etc.). Growth scalars only change the block’s size; they never auto-arm the fuse unless the profile explicitly says so. Profiles also own the countdown numbers (`explosion_delay`, `pulse_interval`, `shell_collapse_ticks`) and any required items.  
- `shellCollapseTicks` now squeezes the block toward `min` scale during the fuse so the aura visibly implodes before detonation.  
- If `doesDestruction=true`, completion triggers either:  
  1. A miniature singulary-style shell chew via `SingularityDestructionEngine`.  
  2. A configured explosion/break radius when a plan is overkill.  
- Fuse events emit an EffectBus event so audio/particles stay data-driven.
- Once armed, the fuse ignores `hasGrowthEnabled=false` so countdowns never stall mid-collapse; only unarmed states respect the freeze toggle.
- Tool interactions (e.g., requiring flint-and-steel) and special behaviours (e.g., multi-stage collapse) are added via fuse profile fields, never by hijacking growth or hazard flags.
- When destruction is enabled, the runtime consults `explosionProfile` to decide blast radius, how many total re-lights (charges) the block has, the damage envelope (`max_damage` + `damage_scaling`), and whether to light fires or break blocks. No other subsystem overrides these values.

---

## Progress Snapshot _(2025-11-29)_

### Main Feature: Physical Growth
| Aspect | Status | Notes |
| --- | --- | --- |
| Bounding box scaling | ✅ | Collision/outline/camera/raycast boxes recomputed every tick to match `currentScale`. |
| Visual scale | ✅ | Client renderer scales the magma glow mesh using interpolated `currentScale`; block model itself is hidden. |
| Growth timing | ✅ | `rate` + `scaleByRate` control easing toward `currentTarget`; clamps to `min/max`. |
| QA coverage | ⚠️ Pending | Needs dedicated command suite + test plan to verify every profile (requested). |

### Supporting Features
| Feature | Status | Notes |
| --- | --- | --- |
| Registry & configs | ✅ | `GrowthRegistry` loads glow/force/fuse/growth JSON and seeds defaults under `config/the-virus-block`. |
| Runtime scaffolding | ✅ | Block/entity read definitions, resize bounding boxes, apply touch damage, and run fuse/force logic. |
| Effect bus hooks | ✅ | `GrowthForceEvent` / `GrowthFuseEvent` dispatched for every push/pull/fuse stage with full profile metadata. |
| Client renderer | ✅ | Magma-style glow + wobble + fuse pulse, plus `GrowthBeamRenderer` + `GrowthRingFieldRenderer` to visualize guardian beams and optional ring-bound field profiles. |
| Commands / reload | ⏳ | `/virusblock growth …` command suite, live reload, and inspection tools still outstanding. |
| Scheduler integration | ⏳ | Need explicit scheduler tasks for scripted phases + diagnostics. |
| QA harness & dev toggle | ⏳ | Admin/testing commands and singularity harness gating still outstanding. |
| Singularity integration | 🚫 | Blocked until QA harness validates the system end-to-end. |

> We are still in the “infrastructure” phase: renderer + runtime scaffolding exist, but commands, reload tooling, scheduler hooks, and the dev harness remain outstanding. Do **not** wire this into actual gameplay until those tasks are complete.

This README should be treated as the source of truth for the feature while we
implement the code paths and data files described in the broader architecture
docs.

