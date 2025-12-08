# Gap Analysis

> **Purpose:** Track gaps between current architecture and actual requirements  
> **Type:** Analysis document (not a backlog)  
> **Created:** December 8, 2024

---

## How to Use This Document

**This is a GAP ANALYSIS** - it identifies what's MISSING from our architecture.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  GAP ANALYSIS WORKFLOW                                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. GAP IDENTIFIED → We discover something missing from architecture        │
│                                                                             │
│  2. GAP ANALYZED → Discuss, define structure, answer questions              │
│                                                                             │
│  3. GAP CLOSED → Update main documents:                                     │
│     • 01_ARCHITECTURE.md                                                    │
│     • 02_CLASS_DIAGRAM.md                                                   │
│     • 03_PARAMETERS.md                                                      │
│     • TODO_LIST.md                                                          │
│                                                                             │
│  4. GAP REMOVED → Delete from this file (it's no longer a gap)              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Current Gaps Summary

| # | Gap | Status | Target Phase |
|---|-----|--------|--------------|
| 1 | Reactive Visual Bindings | ✅ CLOSED | Phase 1 |
| 2 | Event Triggers | ✅ CLOSED | Phase 1 |
| 3 | Lifecycle (Fade/Scale/Decay) | ✅ CLOSED | Phase 1 |
| 4 | Combat Tracking | ✅ CLOSED | Phase 1 |
| 5 | Visual Effect Overlays | Open | Phase 2 |
| 6 | Particle Integration | Open | Phase 2 |
| 7 | Sound Integration | Open | Phase 3 |
| 8 | World Interaction (Light/Weather) | Open | Phase 3 |
| 9 | Collision | Open | Phase 4 |
| 10 | Performance (LOD) | Open | Phase 3 |
| 11 | Inter-Field Interaction | Open | Phase 5 |

**Closed gaps have been integrated into:**
- `01_ARCHITECTURE.md` §12
- `02_CLASS_DIAGRAM.md` §16
- `03_PARAMETERS.md` §12

---

---

## 1. Reactive Visual Bindings ✅ CLOSED

> **Integrated into:** ARCHITECTURE §12.1, CLASS_DIAGRAM §16, PARAMETERS §12.1

### What Was Missing
Field properties that automatically update based on external values.

### Proposed Structure
```json
"bindings": {
  "alpha": { 
    "source": "player.health",
    "inputRange": [0, 20],      // Health 0-20
    "outputRange": [0.3, 1.0]   // Alpha 0.3-1.0
  },
  "scale": {
    "source": "player.speed",
    "multiplier": 0.1,
    "base": 1.0
  },
  "wobble.amplitude": {
    "source": "damage_taken",
    "decay": 0.95               // Fades over time
  }
}
```

### Available Sources (Phase 1)

| Source | Type | Description |
|--------|------|-------------|
| `player.health` | float (0-20) | Current health |
| `player.health_percent` | float (0-1) | Health as percentage |
| `player.armor` | int (0-20) | Armor points |
| `player.food` | int (0-20) | Food level |
| `player.speed` | float | Current movement speed |
| `player.is_sprinting` | bool | Sprinting state |
| `player.is_sneaking` | bool | Sneaking state |
| `player.is_flying` | bool | Flying state |
| `player.in_combat` | bool | Has dealt/taken damage recently |
| `player.last_damage_time` | ticks | Ticks since last damage |

### Available Sources (Phase 2+)

| Source | Type | Description |
|--------|------|-------------|
| `entity.hostile_nearby` | int | Count of hostile mobs in range |
| `entity.nearest_hostile` | float | Distance to nearest hostile |
| `time.day_progress` | float (0-1) | Time of day (0=dawn, 0.5=dusk) |
| `time.is_night` | bool | Is it night? |
| `weather.rain` | float (0-1) | Rain intensity |
| `weather.thunder` | bool | Is thundering? |
| `biome.temperature` | float | Biome temperature |
| `light.block` | int (0-15) | Block light level |
| `light.sky` | int (0-15) | Sky light level |

### Bindable Properties

| Property | Type | What it does |
|----------|------|--------------|
| `alpha` | float | Transparency |
| `scale` | float | Overall size |
| `scaleXYZ.x/y/z` | float | Per-axis size |
| `glow` | float | Glow intensity |
| `color` | lerp | Blend between colors |
| `spin.speed` | float | Rotation speed |
| `pulse.scale` | float | Pulse amplitude |
| `wobble.amplitude` | float | Wobble intensity |
| `visibility.offset` | float | Band/stripe position |

### Priority: **Phase 1** (core system), expand sources in Phase 2

---

## 2. Event Triggers ✅ CONFIRMED → 📝 INTEGRATING

### What's Missing
One-time game events causing temporary visual effects.

### Proposed Structure
```json
"triggers": [
  {
    "event": "player.damage",
    "effect": "flash",
    "params": {
      "color": "#FF0000",
      "duration": 0.3
    }
  },
  {
    "event": "player.heal",
    "effect": "pulse",
    "params": {
      "scale": 1.2,
      "color": "#00FF00",
      "duration": 0.5
    }
  },
  {
    "event": "player.death",
    "effect": "fadeOut",
    "params": {
      "duration": 2.0
    }
  }
]
```

### Available Events (Phase 1)

| Event | When it fires |
|-------|---------------|
| `player.damage` | Player takes any damage |
| `player.heal` | Player heals |
| `player.death` | Player dies |
| `player.respawn` | Player respawns |
| `field.spawn` | Field is created |
| `field.despawn` | Field is removed |

### Available Events (Phase 2+)

| Event | When it fires |
|-------|---------------|
| `player.attack` | Player attacks |
| `player.kill` | Player kills entity |
| `player.block_break` | Player breaks block |
| `player.block_place` | Player places block |
| `player.item_use` | Player uses item |
| `player.enter_combat` | Combat starts |
| `player.exit_combat` | Combat ends |
| `ability.cast` | Custom ability used |
| `ability.cooldown` | Ability goes on cooldown |

### Available Effects

| Effect | Description | Params |
|--------|-------------|--------|
| `flash` | Brief color overlay | color, duration |
| `pulse` | Scale up then back | scale, duration |
| `shake` | Rapid position offset | amplitude, duration |
| `ripple` | Wave from point | origin, speed, amplitude |
| `fadeOut` | Alpha to 0 | duration |
| `fadeIn` | Alpha to 1 | duration |
| `colorShift` | Temporary color | color, duration, blend |
| `glow` | Temporary glow boost | intensity, duration |

### Priority: **Phase 1** (basic), expand events in Phase 2

---

## 3. Visual Effect Overlays

### What's Missing
Status effects (buffs/debuffs) modifying field appearance.

### Proposed Structure
```json
"effectOverlays": {
  "poison": {
    "colorTint": "#00FF00",
    "wobble": { "amplitude": 0.15, "speed": 2.0 }
  },
  "wither": {
    "colorTint": "#333333",
    "alpha": 0.5,
    "pulse": { "scale": 0.2, "speed": 0.5 }
  },
  "shield_boost": {
    "scale": 1.2,
    "glow": 0.8,
    "colorTint": "#FFFFFF"
  },
  "invisibility": {
    "alpha": 0.1
  }
}
```

### Standard Minecraft Effects to Support

| Effect | Suggested Visual |
|--------|------------------|
| `poison` | Green tint, wobble |
| `wither` | Dark tint, decay pulse |
| `regeneration` | Soft glow, gentle pulse |
| `fire_resistance` | Orange tint, warm glow |
| `invisibility` | Near-transparent |
| `speed` | Stretch effect, trail |
| `slowness` | Compressed, dim |
| `strength` | Larger, brighter |
| `weakness` | Smaller, dimmer |
| `resistance` | Solid, less transparent |
| `absorption` | Golden overlay |
| `glowing` | Strong glow |
| `levitation` | Floaty wobble |

### Custom Effects
Users can define custom effect overlays for:
- Mod-added status effects
- Custom ability states
- Our own `PersonalShieldStatusEffect`

### Priority: **Phase 2** (needs effect system integration)

---

## 4. Sound Integration

### What's Missing
Fields that make sounds.

### Proposed Structure
```json
"sound": {
  "ambient": {
    "sound": "the-virus-block:field.hum",
    "volume": 0.3,
    "pitch": 1.0,
    "fadeIn": 1.0
  },
  "events": {
    "spawn": "the-virus-block:field.activate",
    "despawn": "the-virus-block:field.deactivate",
    "damage_blocked": "the-virus-block:field.impact"
  },
  "reactive": {
    "source": "pulse.scale",
    "affects": "pitch",
    "range": [0.8, 1.2]
  }
}
```

### Priority: **Phase 3** (nice-to-have)

---

## 5. Particle Integration

### What's Missing
Fields emitting particles.

### Proposed Structure
```json
"particles": {
  "surface": {
    "type": "minecraft:end_rod",
    "rate": 5,              // per second
    "distribution": "random" // or "uniform"
  },
  "edge": {
    "type": "minecraft:electric_spark",
    "rate": 20
  },
  "burst": {
    "trigger": "player.damage",
    "type": "minecraft:flash",
    "count": 50
  }
}
```

### Priority: **Phase 2** (medium effort, high visual impact)

---

## 6. Field Lifecycle ⭐ CONFIRMED

### What's Missing
Fields that change over time without player input.

### Use Case (Confirmed)
> Player fields will be temporary (~30 seconds), triggered by a buff effect.  
> Field appears when buff activates, fades when buff expires.

### Proposed Structure
```json
"lifecycle": {
  "decay": {
    "enabled": true,
    "rate": 0.01,           // alpha loss per second
    "minAlpha": 0.2,
    "restoreOnDamage": true // taking damage restores field
  },
  "charge": {
    "enabled": true,
    "maxCharge": 100,
    "drainRate": 1.0,       // per second
    "visualProperty": "glow" // glow = charge %
  },
  "lifetime": {
    "duration": 600,        // ticks (30 seconds)
    "fadeOutDuration": 40   // last 2 seconds fade
  },
  "trigger": {
    "type": "status_effect",
    "effect": "the-virus-block:personal_shield",
    "spawnOnApply": true,
    "despawnOnRemove": true
  }
}
```

### Priority: **Phase 2** (integrates with buff system)

---

## 7. Performance (LOD)

### What's Missing
Field simplification at distance.

### Proposed Structure
```json
"lod": {
  "enabled": true,
  "levels": [
    { "distance": 32, "latSteps": 16, "lonSteps": 32 },
    { "distance": 64, "latSteps": 8, "lonSteps": 16 },
    { "distance": 128, "mode": "billboard" }
  ]
}
```

### Priority: **Phase 3** (performance optimization)

---

## 8. World Interaction ⭐ CONFIRMED

### What's Missing
Fields affected by or affecting the world.

### Confirmed Interest
> User likes: collision with projectiles, weather, lighting

### 8.1 Collision ⭐
```json
"collision": {
  "enabled": true,
  "blocks": ["projectiles", "items"],  // what it stops
  "passThrough": ["owner", "allies"],  // who can pass
  "visual": {
    "onImpact": "ripple",              // visual effect
    "impactColor": "#FFFFFF"
  },
  "gameplay": {
    "reflectProjectiles": false,
    "damageOnContact": 0
  }
}
```

**Implementation Notes:**
- Projectile detection via mixin or entity collision
- Visual ripple at impact point
- Sound on impact optional

### 8.2 Weather ⭐
```json
"weather": {
  "rain": {
    "enabled": true,
    "dripsOnSurface": true,      // particle effect
    "distortsField": 0.1,        // wobble intensity
    "reduceAlpha": 0.1           // slightly more transparent
  },
  "wind": {
    "enabled": true,
    "affectsShape": true,        // field leans with wind
    "maxDistortion": 0.2
  },
  "thunder": {
    "flashOnStrike": true,       // field flashes during lightning
    "glowBoost": 0.5
  }
}
```

### 8.3 Lighting ⭐
```json
"lighting": {
  "emitsLight": true,
  "lightLevel": 8,               // 0-15
  "coloredLight": true,          // if mod supports
  "affectedByWorldLight": true,  // glow brighter in dark
  "dynamicRange": [0.5, 1.5]     // glow multiplier based on light
}
```

### Priority Summary
| Feature | Priority | Effort |
|---------|----------|--------|
| Lighting | Phase 2 | Low (just block light) |
| Weather visuals | Phase 3 | Medium |
| Collision | Phase 3 | High (gameplay impact) |
| Wind physics | Phase 4 | Medium |

---

## 9. Inter-Field Interaction

### What's Missing
Fields affecting each other.

### Proposed Concepts
```json
"fieldInteraction": {
  "onOverlap": {
    "with": "enemy_field",
    "effect": "repel",
    "strength": 2.0
  },
  "chargeFrom": {
    "source": "ally_field",
    "property": "glow",
    "rate": 0.1
  }
}
```

### Priority: **Phase 5** (very complex)

---

## Priority Summary

### Phase 1 (Add to current TODO) ✅ CONFIRMED
- [ ] Reactive Bindings (core system, player sources, curves)
- [ ] Event Triggers (core system, all events including block)
- [ ] Combat tracking (in_combat, damage_taken with decay)

### Phase 2 ✅ CONFIRMED
- [ ] Reactive Bindings (expand sources: entities, time, weather)
- [ ] Event Triggers (expand effects)
- [ ] Visual Effect Overlays (stacking, duration)
- [ ] Field Lifecycle (decay, buff-triggered spawn/despawn)
- [ ] Lighting (emit light)
- [ ] Particle Integration

### Phase 3
- [ ] Sound Integration
- [ ] Weather visuals (rain drips, thunder flash)
- [ ] Collision (block projectiles, ripple effect)
- [ ] Performance LOD

### Phase 4+
- [ ] Wind physics (field distortion)
- [ ] Block awareness
- [ ] Inter-field interaction

---

## Questions Answered

| # | Question | Answer |
|---|----------|--------|
| G1 | Should bindings use linear interpolation or support curves? | ✅ **Support curves** - ease-in, ease-out, etc. |
| G2 | Should events queue or override each other? | ✅ **Override** - no queuing, newest wins |
| G3 | How do we detect "in combat" state? | ✅ **Track ourselves** - Minecraft has no built-in flag. Track "dealt/took damage within last 100 ticks". Use `LivingEntity.hurtTime` for damage detection. |
| G4 | Should effect overlays stack or use priority? | ✅ **Stack** - effects can combine (color + alpha). Each has duration. |
| G5 | Do we need server-side tracking for bindings? | ✅ **Yes, at least partial** - for multiplayer, other players need to see your field. Server tracks state, syncs to clients. |

## Design Notes

### Combat Detection (G3 Detail)
```java
// Minecraft provides:
// - LivingEntity.hurtTime (resets to 10 when hit, counts down)
// - LivingEntity.timeUntilRegen (invulnerability frames)
// - No "dealt damage" tracking built-in

// Our approach:
// 1. Hook into damage events (mixin or listener)
// 2. Track lastDamageTakenTime and lastDamageDealtTime per player
// 3. "in_combat" = either within last 100 ticks (5 seconds)

// For "damage_taken" binding:
// - Store last damage amount
// - Apply decay over time (multiply by 0.95 each tick)
// - Binding reads this decaying value
```

### Effect Stacking (G4 Detail)
```java
// Multiple effects can be active:
// - Poison (green tint, wobble)
// - Shield boost (larger, glow)
// Result: Green-tinted, wobbling, larger, glowing field

// Stacking rules:
// - Color: Blend all tints together (weighted average)
// - Alpha: Multiply all values
// - Scale: Multiply all values
// - Wobble: Add amplitudes
// - Glow: Max of all values
```

### Server-Side Bindings (G5 Detail)
```
Client A (owner):
  - Reads own player state (health, speed, etc.)
  - Applies bindings locally for responsive feel
  - Sends state updates to server if significant change

Server:
  - Validates state
  - Broadcasts to other clients

Client B (observer):
  - Receives state from server
  - Applies to owner's field
  - Slight delay acceptable (visual only)
```

---

## Related Documents

- [TODO_LIST.md](./TODO_LIST.md) - Implementation tracking
- [01_ARCHITECTURE.md](./01_ARCHITECTURE.md) - Core architecture
- [QUESTIONS.md](./QUESTIONS.md) - Open questions

---

*Created: December 8, 2024*  
*Status: Gaps identified, pending prioritization*

