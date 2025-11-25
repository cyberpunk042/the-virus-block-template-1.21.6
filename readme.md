# 🦠 THE VIRUS BLOCK — Beginning of the End

**A Minecraft mod that lets you destroy your entire world… one mutation at a time.**  
Place a single block.  
Watch your dimension collapse into chaos.

Perfect for:
- Streamers  
- Challenge runs  
- SMP events  
- “Hardcore but getting worse every minute” experiences  

Once the **Virus Block** is placed, your dimension enters a progressive, escalating corruption arc that alters every part of Minecraft: mobs, blocks, worldgen, weather, cave physics, and even player behavior.

There is no going back.  
Only survival.

---

## 🧬 Core Concept

When the Virus Block is activated:

- Your dimension is flagged as **infected**  
- Mutation systems turn on  
- Chaos escalates in **five infection tiers**  
- The world becomes progressively more hostile, unstable, and unpredictable  

This mod transforms Minecraft into a living apocalypse simulator.

---

## 📈 Infection Tiers

The Virus spreads through **five escalating tiers**:

### Tier 1 — Mild Corruption

- Local block mutations around the Virus Block  
- Single-block flickers / texture glitches  
- Passive mobs stare, follow, or mirror players  
- Virus Aura: light poison ticks; stand too long on an Infectious Cube and it starts nibbling half-hearts  
- Carrying an Infectious Cube in your inventory for a few seconds gives intermittent **Hunger** + **Nausea**

### Tier 2 — Environmental Decay

- Random mutation pulses across loaded chunks  
- Fluids begin corrupting (murky water / tainted lava)  
- Small cave-ins during mining and the first **Collapse Surge** events  
- Hostile arrows occasionally fall from the sky  
- Boobytraps start appearing, but at a low density

### Tier 3 — Hostile Ecosystem

- Passive mobs become corrupted and aggressive  
- Terrain patches convert to corrupted biomes; surface carpets of corrupted sand/ice/snow spread quickly  
- TNT meteors / Skyfall begin  
- Void Tears become possible once Tier 3 extras are enabled  
- Boobytrap spread and bonus hostile spawns ramp up significantly

### Tier 4 — Reality Warping

- Random gravity collapses, sideways block falls, inversion events  
- Mobs gain random abilities: speed, crits, explosions, venom, knockback  
- Fluids misbehave in corrupted zones; Collapse Surge becomes lethal  
- Entity Duplication can trigger if Tier 4 extras stay enabled

### Tier 5 — The Endgame

- Biomes collapse or invert outright  
- Massive TNT/Arrow barrages, corruption waves, and entity duplication storms  
- Corrupted TNT now rolls volatile results (1/3 dud, 1/3 normal, 1/3 1.5× blast) to keep finales unpredictable  
- Bonus hostile mob spawns spike to their highest density  
- Once the tier bar fills, **Apocalypse Mode** begins.

### 🔻 Apocalypse Mode (Post-Tier 5)

Once the Tier 5 progress bar finishes, every Virus Block sheds its shells, the bossbar switches to a red health readout, and the corruption enters its terminal phase:

- The Virus Block becomes fully vulnerable; melee hits, arrows, TNT, and Purification option 4 all drain its health.
- Purification option 3 still halves the block’s max health, shortening the fight without rewinding progress.
- The calm/progress bossbar is hidden; only the red health bar remains while vulnerable.
- When Virus HP reaches zero *or* you finish mining the exposed block, the infection is cleansed and the world begins to recover.
- (Future roadmap: a Singularity-style fail-safe may return later if players ignore Apocalypse Mode for too long, but it is currently disabled.)

---

## 🧩 Corrupted Block Variants

Every vanilla block can spawn a **corrupted variant**:

Examples:

- **Corrupted Stone:** behaves like sand  
- **Corrupted Glass:** explodes when broken  
- **Corrupted Dirt:** spawns bitey worms  
- **Corrupted Wood:** ignites when walked on  
- **Corrupted Iron Block:** magnetic pull on items  
- **Corrupted Ice:** creates hallucinations  

Over time, your world becomes an alien landscape.

---

## 🌪️ Global Mutation Events

These occur periodically or randomly:

- **Block Mutation Pulse**  
  Patches of blocks shift all at once into random variants.

- **Skyfall Event**  
  TNT / arrows / random objects rain down over players.

- **Passive Mob Revolt**  
  Cows, pigs, chickens, etc. become corrupted and hostile.

- **Mob Buff Storm**  
  All mobs gain random buffs (speed, strength, explosions, crits, etc.).

- **Liquid Mutation**  
  Water → sludge (slow + poison)  
  Lava → tainted lava (extra dangerous, smoke bursts)

- **Collapse Surge**  
  Stone caves collapse in chain reactions while mining.

- **Void Tears**  
  Temporary mini black holes that suck in nearby entities.

- **Inversion Event**  
  Falling blocks fall UP instead of down for a short duration.

- **Virus Bloom**  
  Plants and trees convert to corrupted flora instantly.

- **Entity Duplication**  
  Random mobs multiply into weaker clones for swarm chaos.

Every session becomes a unique apocalypse.

---

## 👁️ Virus Aura & Infectious Hazards

- Standing near the Virus Block applies poison, hunger, slowness, nausea, and armor corrosion. Intensity scales with tier.  
- Standing directly **on** an Infectious Cube for more than two seconds starts dealing half-hearts of damage every second until you move.  
- Carrying an Infectious Cube in your inventory for a few seconds applies intermittent Hunger + Nausea pulses—drop it or stash it to recover.

Pro tip: cure cubes with milk (shapeless recipe) or quarantine them inside obsidian shells.

---

## ⚔️ Ending the Infection

Apocalypse Mode is the last phase—there is no separate boss arena right now. Survive the Tier 5 chaos, keep accelerating the Virus Block’s progress, and once the bar flips to red health you can finally fight back:

- Every hit against the Virus Block removes health instead of rewinding progress.
- TNT, bed explosions, and projectiles all deal chunk-based damage; larger blasts chew away more HP.
- Purification totems remain powerful finishers—option 3 reduces max HP, option 4 burns current HP.
- When the health bar empties, the Virus Block disintegrates and the infection resets to zero sources.

---

## 🏛️ Player Tools & Interactions

### Accelerate / Delay Corruption

- Feed the Virus Block powerful items (config) to speed up tier progress.  
- Wrap a Cured Infectious Cube in an obsidian cocoon to spawn an **Anti-Virus Field**—a beacon-style dome that stops spread, shields players from anomalies, and detonates violently if the virus reaches it.  
- Build multiple shells to buy time; Tier 4/5 boobytraps can still tunnel through, so watch the new `/virusstats` readout.

### Temporary Reversion — Purification Totem

- **No Boobytraps** – disables trap placement and throttles corrupted worms (virus enters “dormant” mode).  
- **No Shell** – collapses every defensive shell wrapped around the Virus Block.  
- **Half HP** – halves the Virus Block’s *maximum* health for the current tier (permanent until tier change).  
- **Bleed HP** – halves the Virus Block’s *current* health immediately.  
- The totem now checks for dormant mode and refuses redundant activations, so you get clear feedback.

### End the Apocalypse

- Once Apocalypse Mode begins, the bossbar flips to red HP.  
- Hits, TNT, beds, and Purification option 4 drain health instead of rewinding progress.  
- When HP reaches zero (or you mine the exposed block), the infection is wiped and the world slowly calms down.

---

## 🧨 Why This Mod Rocks for Streamers

- Every world becomes a dynamic storyline  
- Random chaos = endless clips  
- Viewers can predict and watch the infection escalate  
- Passive mobs turning hostile is peak content  
- TNT meteors + block collapse chains = living highlight reels  
- World slowly turning alien = insane thumbnails  
- Built-in tension arc: “What tier are we at!?”  
- Endgame boss is fully cinematic and world-altering  

This is a “progressive chaos” mod designed for content creation and challenge runs.

---

## 🧪 Debug / Admin Commands

- `/virusstats` – Quick diagnostics (time since mod start, infection uptime, ETA to final wave).  
- `/virustiers` – Shows the current tier, apocalypse state, and Tier Cookbook flags (great for tuning gamerules).  
- `/virusboobytraps [radiusChunks]` – Lists nearby boobytraps (defaults to 8 chunks).  
- `/virusblock waves friendlyfire <enable|disable|status>` – Toggle whether virus-spawned waves hurt one another.  
- `/virusblock teleport <enable|disable|radius|status>` – Control automatic Virus Block teleports.

### Tier Feature Defaults

- **Tier 2:** liquid corruption, corrupted sand/ice/snow surfaces, Mutation Pulse, Skyfall, Collapse Surge, Passive Revolt, Mob Buff Storm, Virus Bloom.
- **Tier 3:** Void Tear, Inversion.
- **Tier 4:** Entity Duplication.
- **Tier 5:** (Reserved – apocalypse add-ons only.)

## 🍳 Crafting Recipes

| Recipe | Inputs | Notes |
| --- | --- | --- |
| **Cured Infectious Cube** | Shapeless: 1× Infectious Cube + 1× Milk Bucket | Produces 1× Cured Infectious Cube. Combine anywhere in the crafting grid. |
| **Purification Totem** | Shaped “cross” pattern: Top = Cured Infectious Cube, Left = Corrupted Gold, Center = Totem of Undying, Right = Corrupted Iron, Bottom = Corrupted Diamond | There are four JSON variants covering every rotation; the center slot must always be a Totem of Undying. Consumed on use. |

*(Drop your PNGs—`cured_infectious_cube.png`, `purification_totem.png`, etc.—into the repo’s `docs/` folder if you want the README to display the crafting layouts directly.)*

## 🛠️ Installation

> Placeholder instructions — adjust versions when implementing.

1. Install **Fabric Loader** (0.18.x or later)  
2. Install **Fabric API**  
3. Drop `virusblock-x.x.x.jar` into your `mods` folder  
4. Launch the game

Optional but recommended:

- Use shaders for maximum visual freakiness  
- Recommended performance mods: Sodium, Indium, Iris

---

## ⚙️ Configuration Reference

Most tuning happens through gamerules. Here are the high-impact ones (defaults in parentheses):

| Gamerule | Default | Effect |
| --- | --- | --- |
| `virusSurfaceCorruptAttempts` | `640` | Attempts per tick for Tier‑2 surface mutation (sand/ice/snow carpets). Raise for faster biome takeovers. |
| `virusLiquidMutationEnabled` | `true` | Enables corrupted water/lava once Tier 2 is reached. |
| `virusCorruptSandEnabled` / `virusCorruptIceEnabled` / `virusCorruptSnowEnabled` | `true` | Opt individual surface conversions in/out. |
| `virusTier2EventsEnabled` | `true` | Master toggle for Tier‑2 global events (Mutation Pulse, Skyfall, etc.). |
| `virusTier3ExtrasEnabled` | `true` | Unlocks Tier‑3+ extras (Void Tear, Inversion, Entity Duplication). |
| `virusEventMutationPulseEnabled` … `virusEventEntityDuplicationEnabled` | `true` | Fine-grained switches for each event (Mutation Pulse, Skyfall, Collapse Surge, Passive Revolt, Mob Buff Storm, Virus Bloom, Void Tear, Inversion, Entity Duplication). Use these if you want to cherry-pick specific events without disabling the whole tier. |
| `virusMatrixCubeMaxActive` | `200` | Cap on simultaneous Matrix Cubes raining from the sky. |
| `virusBoobytrapsEnabled` | `true` | Controls spontaneous boobytrap placement/explosions. |
| `virusWormsEnabled` | `true` | Allows corrupted dirt/boobytraps to spawn corrupted worms. |

### Example Preset (`configs/virus_contained.mcfunction`)

```mcfunction
# Tier-2 visuals only, low chaos
/gamerule virusTier3ExtrasEnabled false
/gamerule virusTier2EventsEnabled true
/gamerule virusSurfaceCorruptAttempts 320
/gamerule virusLiquidMutationEnabled true
/gamerule virusBoobytrapsEnabled false
/gamerule virusMatrixCubeMaxActive 80
/gamerule virusWormsEnabled false
```

Drop this `.mcfunction` into a datapack and run `/function configs:virus_contained` to apply it instantly on a server.
