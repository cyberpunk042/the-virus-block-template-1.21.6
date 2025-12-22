# Future Feature Ideas

A collection of promising ideas from brainstorming sessions.

---

## 🏆 TOP PICKS

### 1. Pocket Dimension (Safe Haven)

**Concept:** A personal/shared dimension players can enter to pause the virus.

**Mechanics:**
- Right-click a special item (Pocket Key? Sanctuary Stone?) to enter
- Personal dimension - each player has their own OR shared team dimension
- When ALL online players are inside pocket dimensions, the virus PAUSES
- Creates strategic coordination: "Everyone get to safety!"
- Dimension is small, can be upgraded/customized
- Cannot bring virus-related items inside (purification)
- Exiting returns you to where you entered

**Technical considerations:**
- Custom dimension type
- Player tracking for "all players in safety" check
- Tick pausing for virus world state
- Entry/exit teleportation

**Why it's good:**
- Gives players breathing room
- Creates coordination gameplay
- Safe space for planning/crafting
- Natural pacing mechanism

---

### 2. The Glitch (Reality Breakdown)

**Concept:** The virus corrupts the game itself, not just the world.

**Possible safe implementations:**
- Texture swaps (blocks look like other blocks temporarily)
- Fake particles that don't affect gameplay
- UI corruption (hearts/hunger display wrong but mechanics correct)
- Sound glitches (wrong sounds play)
- Fake chat messages from "the virus"
- HUD elements appearing/disappearing
- Screen shake/color shifts
- Fake "Achievement Unlocked" popups with creepy messages

**What to AVOID (crash risk):**
- Actual coordinate manipulation
- Breaking block states
- Modifying entity data incorrectly
- Changing actual game rules mid-tick

**Why it's good:**
- Meta-horror (the virus knows it's in a game)
- Unique to gaming medium
- Unsettling without being unfair

---

### 3. Afterimage Haunting (Phantom Block)

**Concept:** A ghostly afterimage of the virus block periodically manifests near players and attacks them with varied, random assault patterns.

**Mechanics:**
- Random spawn timer (every 2-10 minutes based on tier)
- Afterimage appears 10-20 blocks away from player, visible but translucent
- Cannot be damaged or destroyed - it's a phantom
- Stays for 5-15 seconds before fading
- During manifestation, launches RANDOM attacks at the player:

**Attack Types (randomly selected):**
- **Psychic Pulse:** Nausea + brief blindness, no physical damage
- **Void Beam:** Straight-line damage beam, must dodge sideways
- **Corruption Blast:** AOE explosion at player's feet, knockback
- **Spectral Arrows:** Volley of homing arrows, slow but persistent
- **Gravity Pull:** Yanks player toward the afterimage
- **Gravity Push:** Shoves player away violently
- **Mind Freeze:** Player can't move for 1-2 seconds
- **Item Scramble:** Swaps held item with random inventory slot
- **Echo Scream:** Plays heartbeat LOUD, direction confusion

**Technical considerations:**
- Render as translucent entity, not solid block
- Client-side particle effects for visual flair
- Attack patterns as weighted random selection
- Distance from actual virus block affects frequency (further = more common)
- Timer persists across dimensions

**Why it's good:**
- The virus hunts you ANYWHERE
- Unpredictable - players can't memorize patterns
- Constant tension even in "safe" areas
- Reinforces the virus as a persistent, intelligent threat
- Great for jump scares without being cheap

---

### 4. Eternal Return (The Virus Never Dies)

**Concept:** Defeating the virus is never permanent. It will find its way back to you.

**Mechanics:**

**Time-Based Return:**
- After cleansing the virus, a hidden timer starts
- Every real-time hour after victory, chance of return increases by 5%
- At 24 hours: 100% chance of virus block appearing in inventory
- Appears during any inventory interaction (crafting, chest, etc.)
- Player gets a warning: "Something stirs in your belongings..."

**Dimensional Return:**
- Entering the Nether after defeating the virus: 25% chance per entry
- Entering the End: 50% chance per entry
- The virus sees dimension travel as weakness to exploit

**Escalating Curse:**
- First defeat: When it returns, you get 1 virus block
- Second defeat: Returns with 2 virus blocks
- Third defeat: Returns with 3 virus blocks
- And so on... infinitely
- Blocks appear in DIFFERENT inventory slots (can't just ignore one slot)

**Additional Escalation:**
- Each return, the virus starts at a HIGHER tier (min Tier 2 on 2nd, Tier 3 on 3rd, etc.)
- Cap at starting Tier 4 after 5th defeat

**Counter-play:**
- Special "Sealing Ritual" that requires rare items can delay return
- Staying in Pocket Dimension pauses the return timer
- Destroying blocks in creative doesn't count as "defeating" (no return trigger)

**Why it's good:**
- True roguelike tension - victory is temporary
- Escalating difficulty creates endgame challenge
- Forces players to LIVE with the virus, not just defeat it
- Multiple virus blocks = multiply the chaos
- Explains lore: the virus is bound to you specifically

---

## 📋 IDEAS TO EXPLORE

*(Add more here as we find them)*

---

## ❌ REJECTED IDEAS

*(Track what didn't work and why)*

