# Growth Block Collision Refactor – Lessons Learned

This document summarizes every collision strategy we’ve tried for the progressive growth block, what worked, what failed, and what to keep in mind when adding new approaches.

## 1. Manual Pushback Era

| Attempt | Description | Result | Issues |
| --- | --- | --- | --- |
| **Teleport push (`enforceCollision`)** | Checked the block entity’s `renderBounds` every tick and forcibly zeroed a player’s velocity if their AABB intersected the bounds. | “Thick wall” effect on all faces. | Felt like teleporting; jittered along edges; ignored vanilla movement rules. |
| **Axis push / nudge** | Instead of teleporting, nudged entities out along each axis using world-space boxes. | Smoother blocking while walking in; still blocked top/side most of the time. | Head/top surface still let players clip; sides jittered; still bypassed vanilla physics. |

**Takeaway:** Direct AABB pushes are easy to reason about, but they live outside Minecraft’s collision resolver. Good for prototyping, not for final behavior.

## 2. Pure `VoxelShape` Approach

| Attempt | Description | Result | Issues |
| --- | --- | --- | --- |
| **Oversized shape from `getCollisionShape`** | Tried to return a `VoxelShape` larger than the 1×1 block (mirroring pistons). | Collision only triggered in the default block column (“sweet spot”). | Minecraft clamps per-block collision shapes to the block’s own cell; anything outside the cell is ignored. |

**Takeaway:** A single block can’t return a multi-block `VoxelShape` – vanilla truncates it to the local cube. Pistons solve this by actually occupying multiple block positions (piston base + moving head) and delegating to the moving block entity.

## 3. World-Space Injection (Goal State)

| Attempt | Description | Result | Issues |
| --- | --- | --- | --- |
| **`Entity.findCollisionsForMovement` mixin** | Append each growth block’s world `Box` into the collision list returned by `findCollisionsForMovement`. | Pending | Mixin never applied because Loom could not remap the target method; no logs, no collision change. |

**Plan:** Use Yarn mappings (e.g., `method_59920`) to locate the real helper, confirm the class/descriptor via `javap`, and inject there. This mirrors how pistons influence movement—they add extra shapes after vanilla collects block voxels, so `VoxelShapes.calculateMaxOffset` blocks the player naturally.

## 4. December 2025 Mapping & Injection Sprint

### 4.1 Tooling Upgrades

- `scripts/search_native_jars.py` now supports `--javap <fqcn>` so we can disassemble classes straight out of Loom’s cached jars.  
- We used it on `minecraft-common-077c1ca8a6-1.21.6-net.fabricmc.yarn…jar` to confirm `findCollisionsForMovement` really lives on `net.minecraft.entity.Entity` (see `agent-tools/findCollision-entity-class2.txt`).
- Future collisions: point the script at `/mnt/f/minecraftNativeJars/` and it will tell you exactly which jar holds a symbol before you start guessing.

### 4.2 Dual-Mixin + Mapping Timeline (2025‑12‑04)

| Step | Symptom | Fix / Lesson |
| --- | --- | --- |
| **(a) Add intermediary name directly** | `Cannot remap method_59920…` during `./gradlew build`. | Loom auto-remaps Yarn signatures. A single mixin targeting both names confuses the remapper. |
| **(b) Need dev + prod parity** | Runtime jars still need intermediary hook. | Split into two mixins: `EntityGrowthCollisionMixin` (Yarn, remapped) and `EntityGrowthCollisionIntermediaryMixin` (targets `net.minecraft.class_1297`, `remap=false`). |
| **(c) First attempt** | `Invalid descriptor… expected …Ljava/util/List;...` crash. | When we moved logic into a helper we dropped the `List<VoxelShape>` parameter. Even unused params must stay or the injector signature mismatches. |
| **(d) Helper in mixin package** | `IllegalClassLoadError… GrowthCollisionMixinHelper is in a defined mixin package`. | Sponge forbids referencing classes inside a mixin package (`net.cyberpunk042.mixin`). Move shared helpers to a normal package (`net.cyberpunk042.collision`). |
| **(e) Map lookups** | Hard to confirm owner/signature. | Use `scripts/search_native_jars.py … --javap net.minecraft.entity.Entity` so you see the exact descriptor (`findCollisionsForMovement(Entity, World, List<VoxelShape>, Box)`). The output is archived in `agent-tools/findCollision-entity-class2.txt` for future reference. |
| **Current state** | Server runs; mixins load; collision still a 1×1 base slice with no `[GrowthCollision]` logs. | Means the injection succeeds but we never append shapes (likely zero tracker entries or no intersection with the queried `Box`). Next steps: log tracker contents, world boxes, and the incoming query box per call. |

### 4.3 Implementation Reference – Split Mixins

**Why two files?** Loom remaps named (Yarn) methods automatically during compilation. If we point one mixin at both the Yarn name and the intermediary name, Loom tries to remap `method_59920` a second time and fails. The solution is:

1. A *remapped* mixin for dev builds: `src/main/java/net/cyberpunk042/mixin/EntityGrowthCollisionMixin.java`. It targets `net.minecraft.entity.Entity` and injects into the Yarn signature:  
   `findCollisionsForMovement(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;Ljava/util/List;Lnet/minecraft/util/math/Box;)Ljava/util/List;`

2. A *non-remapped* mixin for production jars: `src/main/java/net/cyberpunk042/mixin/EntityGrowthCollisionIntermediaryMixin.java`. It explicitly references intermediary names so Loom leaves it alone:

```
@Mixin(targets = "net.minecraft.class_1297", remap = false)
@Inject(
    method = "method_59920(Lnet/minecraft/class_1297;Lnet/minecraft/class_1937;Ljava/util/List;Lnet/minecraft/class_238;)Ljava/util/List;",
    at = @At("RETURN"),
    cancellable = true
)
```

- Note the full intermediary descriptor: owner `class_1297` (Entity), world `class_1937`, query box `class_238`, and the third `Ljava/util/List;` argument that **must** stay in the signature even if unused.
- Both mixins call the shared helper `net.cyberpunk042.collision.GrowthCollisionMixinHelper.appendGrowthCollisions(...)`. The helper lives *outside* the `net.cyberpunk042.mixin` package so Sponge doesn’t block the class load.

Whenever you bump Minecraft versions, re-run `scripts/search_native_jars.py … --javap net.minecraft.entity.Entity` and update both mixins’ descriptors as a pair.

### 4.3 Status & Next Checks

- **Mixins load:** both Yarn and intermediary mixins log `[GrowthCollision] Entity mixin initialized` again.
- **Collision still thin:** only a 1×1 plane at the base registers, so the appended shapes are still missing or empty.
- **No runtime logs:** the helper never prints its `[GrowthCollision] player=…` line, so either the tracker says “0 actives” or every box fails `worldBox.intersects(queryBox)`.
- **Next experiments:**  
  1. Log `actives.size()` and `worldShape.getBoundingBoxes()` even when zero appended to confirm we are iterating.  
  2. Dump the query `Box` from `findCollisionsForMovement` to see which height slice Minecraft is checking (the thin slice hints we may only be intersecting one step).  
  3. If tracker is empty server-side, double-check `GrowthCollisionTracker` sync paths (server events may never register outside the client mixin).

### 4.4 World mixin mapping fix (2025‑12‑07)

- Symptom: moving the runtime mixin from `CollisionView` to `World` produced `Cannot remap method_8600` during build and `InvalidInjectionException` with `No refMap loaded` at runtime. The concrete `World` class never overrides those default interface methods, so Sponge could not find a target.
- Diagnosis workflow:
  1. Search the active mappings for `getCollisions` to see the real owner and descriptor:
     ```
     rg -n "getCollisions" .gradle/loom-cache/source_mappings/077c1ca8a67f63c999b4b3c5adee19230b53302c.tiny
     ```
     This shows the intermediary IDs we care about:
     - `method_71395(Lnet/minecraft/class_1297;Lnet/minecraft/class_238;Lnet/minecraft/class_243;)Ljava/lang/Iterable;`
     - `method_20812(Lnet/minecraft/class_1297;Lnet/minecraft/class_238;)Ljava/lang/Iterable;`
     Both live on `net.minecraft.world.level.CollisionGetter` (intermediary `class_1941`), not `class_1937`.
  2. Retarget the runtime mixin accordingly:
     - `@Mixin(targets = "net.minecraft.class_1941", remap = false)`
     - Convert the mixin to an interface so it can inject into default interface methods.
     - Inline both injections with the full descriptor strings above so Loom never tries (and fails) to remap them.
  3. Keep the Yarn-side `CollisionView` mixin purely for instrumentation so we still get `[GrowthCollision:cv-*]` breadcrumbs in dev runs.
- Result: the server now boots with `[GrowthCollision:world-getCollisions:i]` and `[world-getBlockCollisions:i]` logs confirmed in `session-logs/7.txt`. We still pass through the shell because those hooks simply append shapes; they expose that the helper is handing back only a thin slice.
- Outstanding cleanup:
  - Filter the `CollisionView` instrumentation logs the same way (only emit when the JVM debug flag is on and the entity is a player) so passive mobs don’t flood `session-logs`.
  - Re-run geometry checks now that the resolver sees our shapes: compare `query` vs `worldBox` overlap in the new log spam to understand why only the center slice registers.

### 4.5 Status checkpoint – December 7 (evening)

- **Latest logs (`session-logs/12.txt`)** still show `worldBox` heights of `y=[84, 88]` for the growth at `(-1, 84, -21)` and `[GrowthCollision] appended=0` until the player’s AABB drops below `y≈87.99`. The engine only reports `scenario=SIDE_NEG_Z`/`appended=1` once the AABB intersects the very bottom of that volume.
- **Diagnosis:** even with the runtime command and JVM flag forcing `ShapeMode=SHELL`, the block entity continues to populate `collisionPanels` with a single full-height column. Either `ProgressiveGrowthBlockEntity.rebuildShapes()` never sees the updated mode or it immediately falls back (e.g., by bailing out before the piston-style panels are added). The helper therefore receives only the coarse column and we stay “gluey”.
- **Next step:** instrument `rebuildShapes()` to log the current `ShapeMode`, the number of panels generated, and whether the piston-style builder bailed. That will prove whether the flag propagation is broken or the builder logic is short-circuiting. Once we see per-face boxes again we can rerun the log capture to confirm `appended` flips to `1` the instant a wall is touched instead of after sinking to `y≈88`.

### 4.4 Runtime Debug Output

- The mixin helper now logs every time a player brushes the growth collision sweep:
  - `[GrowthCollision:debug] player=… world=… query=… baseCollisions=… actives=…`
  - `[GrowthCollision:debug] … trackerEmpty=1` when no growth block entities are registered in that world.
- When we skip or accept a growth entity you’ll see:
  - `[GrowthCollision:debug] … trackerEmpty=1` when the registry has no active growth blocks for the current world.
  - Per-growth diagnostics logging block positions, whether the BE lives in the current world, and the world-space boxes being evaluated (`intersects=true/false` against the query AABB).
  - Baseline collision counts (`baseCollisions=<N>`) so you know how many shapes vanilla already considered before we append ours.
- These logs are always enabled for players while we chase this regression—no server restart or flags required. Once the collision flow is fixed we can gate or trim them again.

## How to Diagnose Quickly

1. **Mixins** – Add a static log when the mixin class loads (`[GrowthCollision] … mixin initialized`). If it never prints, the mixin didn’t apply.
2. **Collision Logs** – When injecting, log `player`, number of active growth blocks, bounding box, and appended shapes:  
   `[GrowthCollision] player=Alex actives=3 appended=6 query=Box(...)`.
3. **Mappings** – Use `.gradle/loom-cache/source_mappings/<hash>.tiny` and `grep` to find the method owner and descriptor. If Loom says “Cannot remap …”, run `javap` on the compiled jar to double-check the class name.
4. **Shapes** – When debugging `VoxelShape` math, log the bounding boxes (local vs. world) and confirm they have real thickness. A box like `Box[0.2, -0.02, 0.2 -> 0.8, 0.6, 0.8]` is a 1×1 column – anything wider needs multi-block injection.

## Checklist for Future Collision Work

- [ ] If you want precise blocking everywhere, **do not** count on over-sized block-local shapes. Instead, append world-space boxes to the collision list after vanilla runs.
- [ ] When reusing the manual pushback trick, keep the old code around for reference but only as a fallback—it should never ship.
- [ ] Always add logs when introducing a new injection or collision calculation. Silence usually means the code never fired.
- [ ] Keep `GrowthCollisionTracker` (or equivalent) so you don’t scan every block position each tick; just iterate active growth block entities.
- [ ] Once the mixin works, expect pistons-level behavior: the engine clamps movement, zeroes velocity naturally, and you can remove all manual nudges.

## 5. Player Collision Pipeline (December 2025)

### 5.1 Active mixins & helper

- `EntityGrowthCollisionMixin` (Yarn) injects into `Entity.findCollisionsForMovement`, `adjustMovementForCollisions`, and `move`. It appends tracker shapes, logs `[GrowthCollision:hook]`/`[GrowthCollision:sweep]`/`[GrowthCollision:move]`, and proves that dev builds are actually executing the vanilla pipeline.
- `EntityGrowthCollisionIntermediaryMixin` mirrors the same injections with intermediary names so production jars behave identically. The duplicate logging makes it obvious when a remap mismatch sneaks back in.
- `CollisionViewGrowthCollisionMixin` intercepts `CollisionView.getCollisions(Entity, Box, Vec3d)` once the caller is a real `World`. Every server-side sweep for a player now emits `[GrowthCollision:cv]` and receives the aggregated growth boxes before `VoxelShapes.calculateMaxOffset` runs.
- `ServerPlayNetworkHandlerMixin` keeps the interaction guard rails we already had and now wraps the `WorldView.getCollisions(...)` invocation inside `isEntityNotCollidingWithBlocks`. This is the same call the anti-cheat path relies on, so forcing it to include our shapes finally let the server reject “phantom” player motion.
- `GrowthCollisionMixinHelper` gathers tracked block entities, logs why a shape was skipped, limits per-tick debug spam (`DEBUG_BOX_LIMIT`), and merges vanilla vs. growth `VoxelShape`s without duplicating code across mixins.

### 5.2 What almost fully solved the issue

Players were bypassing `Entity.findCollisionsForMovement` entirely on dedicated servers because their packets are validated inside `ServerPlayNetworkHandler.isEntityNotCollidingWithBlocks`. The two fixes that finally made collisions “stick” were:

1. **`CollisionViewGrowthCollisionMixin`** – proved that `CollisionView.getCollisions` is part of the player stack and let us log every sweep the server performs. Once `[GrowthCollision:cv]` spam showed up we knew the view owner was accessible.
2. **`ServerPlayNetworkHandlerMixin#theVirusBlock$injectGrowthCollisionsIntoValidation`** – wrapping the `WorldView.getCollisions` call meant the anti-cheat validator and the standard movement resolver received the exact same merged iterable of `VoxelShape`s. After this change, survival and creative players can no longer “sweet spot” through the growth wall; only tracker gaps (no active BE registered) let someone slip.

With both hooks active, logs now show `[GrowthCollision:hook]`, `[GrowthCollision:cv]`, and `[GrowthCollision] player=… appended=<N>` whenever a player presses into the shell. That confirms the helper is feeding world-space boxes into every stage the server inspects.

### 5.3 Remaining edge cases / next verifications

- Tracker coverage: if `[GrowthCollision:debug] … trackerEmpty=1` shows up, investigate `GrowthCollisionTracker.active(world)` rather than the mixins—no shapes will ever be appended without registered block entities.
- Shape accuracy: we still log up to four world boxes per growth block. If the player only collides with the base slice, inspect the individual `worldShape` bounding boxes for missing height/width.
- Elytra / high-speed cases: keep an eye on `[GrowthCollision:sweep]` counts when the `movement` vector is large. Those code paths are now instrumented, but we have not exhaustively profiled glide boosts or teleport desyncs yet.

## 6. Root Cause & Definitive Fix

### 6.1 Why the collision box felt empty

- Client and server both call `Entity.findCollisionsForMovement`, but **server-side players take a separate path**: `ServerPlayNetworkHandler.onPlayerMove → isEntityNotCollidingWithBlocks → WorldView.getCollisions`.  
- Our first mixins (`EntityGrowthCollisionMixin`, `EntityGrowthCollisionIntermediaryMixin`) only touched the shared helper. They added logging, but the server never evaluated those results while validating player packets, so you could still “sweet spot” through the growth wall even though mobs collided correctly.

### 6.2 The change that fixed it

- The decisive change was adding `@WrapOperation` in `ServerPlayNetworkHandlerMixin#theVirusBlock$injectGrowthCollisionsIntoValidation`, targeting:  
  `Lnet/minecraft/world/WorldView;getCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;Lnet/minecraft/util/math/Vec3d;)Ljava/lang/Iterable;`
- That wrapper calls the original `WorldView.getCollisions`, merges its iterable with `GrowthCollisionMixinHelper.gatherGrowthCollisions(...)`, and hands the combined list back to `isEntityNotCollidingWithBlocks`.
- Because `isEntityNotCollidingWithBlocks` immediately feeds those shapes into `VoxelShapes.calculateMaxOffset`, the server now clamps movement exactly like pistons: players cannot advance once the merged AABB intersects any growth block box. Creative and survival behave the same because both packets take this path.
- Log evidence: every blocked player movement now prints `[GrowthCollision:cv]` followed by `[GrowthCollision] player=… appended=<N>` and the validator no longer accepts zero-offset sweeps.

### 6.3 Why the other mixins stay

- `EntityGrowthCollision*` mixins keep parity between dev/runtime jars and give us precise logging at each engine layer. If one of them stops firing, we immediately know Loom/mapping drift broke something before it hits production.
- `CollisionViewGrowthCollisionMixin` verified that the view exposed by `ServerPlayNetworkHandler` was actually a `World`. Without it we wouldn’t have traced the call stack to `WorldView` or had logs proving the server queried collisions at all.
- `GrowthCollisionMixinHelper` is the reusable merge code; without it every mixin would duplicate tracker iteration and we’d lose the detailed debug breadcrumbs that narrowed the issue down to the validator path.

If collisions regress again, check the `ServerPlayNetworkHandlerMixin` first: if `[GrowthCollision:cv]` stops, the wrap didn’t run. Everything else is supporting instrumentation or shared logic.

### 6.4 Debug logging toggle

All `[GrowthCollision:*]` logs (including `[GrowthCollision:debug]`, `[GrowthCollision:hook]`, `[GrowthCollision:cv]`, etc.) are now **disabled by default** to keep the console clean.  
Re-enable them only when you need to inspect collision sweeps by launching the game/server with:

```
-Dthevirusblock.growthCollisionDebug=true
```

Once the JVM property is set, every mixin/helper restores the previous logging behavior without needing a rebuild.

### 6.5 Top-surface freezing (December 6)

- **Symptom:** Walking onto a full-height growth block felt like rubber-banding or being frozen in place.
- **Reason:** `ProgressiveGrowthBlockEntity.rebuildShapes` added `INTERACTION_EPSILON` to *every* max Y, so the world-space collision boxes extended above the block’s top. The server anti-cheat validator interpreted that as the player clipping through a solid block and repeatedly reset motion.
- **Fix:** Only pad `maxY` with the epsilon when the shape is *shorter* than a block (sub-1.0 scale). Full-height shells now stop exactly at the block top, letting players stand and walk while still blocking horizontal penetration.

### 6.6 Standing on a growth block still felt laggy

- Even after clamping the Y padding, players on top would occasionally get stuck because the validator still saw the growth box that lives directly below their feet (the `WorldView` query AABB overlaps by epsilon).
- We now skip any growth `Box` whose `maxY` is **below** the player’s `minY`, within a `1e-4` tolerance. Vanilla block collisions already keep the player perched there, so duplicating the shape only confuses the anti-cheat path.
- Debug logs (`-Dthevirusblock.growthCollisionDebug=true`) will show `[GrowthCollision:debug] … skipAbove` entries whenever a box is filtered out this way, so you can confirm the filter only runs when the entity is fully above the growth slice.
- Side/bottom “glue” was caused by the old `INTERACTION_EPSILON` expanding every growth box by 0.02 blocks in every direction. Full-size shells were therefore “fatter” than the block cell, so once you touched them the engine considered you embedded. We now only pad horizontal/vertical bounds when the scale is **smaller than one block**, keeping full-size shells aligned with real block faces while still giving sub-block shells enough volume to collide.

### 6.7 Diagnostics workflow (Dec 6)

1. **Enable collision logging**  
   Launch with `-Dthevirusblock.growthCollisionDebug=true`. The helper now prints overlap per axis (`overlap=(x,y,z)`), making it obvious when the growth box actually intersects the player AABB vs. merely touches it.

2. **Toggle the generic `CollisionView` hook**  
   To rule out duplicate injections, add `-Dthevirusblock.disableCollisionViewHook=true`. That leaves only the `ServerPlayNetworkHandler` wrap active. If movement suddenly feels normal, we know double-appends were the issue; otherwise the validator path itself needs slimmer shapes.

3. **Capture focused logs**  
   While the debug flag is on, reproduce three scenarios (side push, standing on top, crouching underneath) and capture the `[GrowthCollision:debug]` lines. Because we cap logs per tick (`DEBUG_BOX_LIMIT`), focus on one growth block to keep the output readable.
4. **Scenario tags**  
   Those `[GrowthCollision:debug]` lines now include `scenario=TOP|BOTTOM|SIDE_POS_X|SIDE_NEG_X|SIDE_POS_Z|SIDE_NEG_Z|INSIDE|UNKNOWN`. Grab at least one snippet for `TOP`, `BOTTOM`, and one of the `SIDE_*` tags so we can diff how far the `query` box overlaps each `worldBox`. The logger only records four entries per sweep unless a new scenario shows up, so you should see each axis-specific tag at least once per reproduction run.

### 6.8 Scenario tagging & slimmer-shape prototype

- **Scenario tagging:** `GrowthCollisionMixinHelper` now classifies every logged box by contact type. Example log:

  ```
  [GrowthCollision:debug] player=Jean growth=BlockPos{x=23, y=64, z=12} scenario=SIDE_POS_X worldBox=Box[(22, 63, 11) -> (23, 65, 12)] query=Box[(22.98, 63.01, 11.31) -> (23.98, 64.87, 12.31)] intersects=true overlap=(0.02, 0.86, 0.69)
  ```

  Filter by `scenario=TOP` / `scenario=BOTTOM` / `scenario=SIDE_*` to capture the three required viewpoints without drowning in noise. The helper also ensures each scenario logs even if the four-entry cap is reached, so you no longer need to spam extra sweeps hoping to see a bottom collision.

- **Top clearance tuning:** pass `-Dthevirusblock.growthTopClearance=0.15` (default `0.02`) to let the helper treat anyone within that many blocks of the top face as “above” the shell. Increasing the value toward `0.2` prevents false positives while you walk across a growth roof; lowering it (the default) keeps collisions strict so players immediately rest on the surface instead of sinking before the validator kicks in.
- **Runtime shape switch:** use `/growthcollision shape shell|solid` (permission level ≥2) to flip between the original solid column collider and the new thin-shell collider without restarting the server. The command forces every active growth block to rebuild its `VoxelShape` immediately so you can A/B test movement in the same session. `shell` uses thin top/bottom caps plus 0.05-block-thick walls; `solid` falls back to the original behavior.
- **Shape dump:** `/growthcollision dump` logs each tracked growth block’s world-space bounding boxes (and rebuilds them) so you can capture the exact geometry currently feeding the collision pipeline.

## 7. Mixin usefulness quick reference

| Mixin | Method / Injection | Purpose | Impact if removed |
| --- | --- | --- | --- |
| `EntityGrowthCollisionMixin` | class (Yarn dev) | Mirrors runtime hooks + logging | Optional (diagnostic only) |
| `EntityGrowthCollisionMixin` | `findCollisionsForMovement` | Log + parity with helper | Optional |
| `EntityGrowthCollisionMixin` | `adjustMovementForCollisions` | Sweep logging for offsets | Optional |
| `EntityGrowthCollisionMixin` | `move` | Bounding-box movement trace | Optional |
| `EntityGrowthCollisionIntermediaryMixin` | class (runtime) | Ensures intermediary descriptor stays valid | Optional, but useful sanity check |
| `EntityGrowthCollisionIntermediaryMixin` | `method_59920` | Runtime logging parity | Optional |
| `EntityGrowthCollisionIntermediaryMixin` | `method_20736` | Runtime sweep logging | Optional |
| `CollisionViewGrowthCollisionMixin` | class/method | Adds growth shapes inside generic `CollisionView` sweeps; breadcrumbs via `[GrowthCollision:cv]` | Helpful but not the fix |
| `ServerPlayNetworkHandlerMixin` | `onPlayerInteractBlock` | Keeps growth interactions server-authoritative | Gameplay critical (existing behavior) |
| `ServerPlayNetworkHandlerMixin` | `isEntityNotCollidingWithBlocks` wrap | Injects growth shapes into the anti-cheat validator | **Critical: actual collision fix** |

Document updated: 2025-12-06 (collision refactor investigation).


