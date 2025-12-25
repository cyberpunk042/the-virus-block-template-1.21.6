# Ray Animation Enums - EXHAUSTIVE Senior Engineer Review

## SCOPE

This document covers **RAY-LEVEL** animations ONLY.
Field-level transforms (spin, orbit, etc.) are handled separately.

Each ray is treated as an **individual wave/line** that can be affected by multiple animation layers.

---

## COMPLETE CATEGORY BREAKDOWN

---

### CATEGORY A: RAY LINE SHAPE (Static Geometry of the Ray Itself)

What is the GEOMETRIC SHAPE of each individual ray?

| Enum | Description | Visual ASCII |
|------|-------------|--------------|
| STRAIGHT | Default straight line from start to end | `─────────` |
| CORKSCREW | Ray twists around its OWN axis (helix/spiral along length) | `🌀` along ray |
| SPRING | Ray coils like a spring/slinky (circular helix) | `ΩΩΩΩΩ` |
| SINE_WAVE | Ray undulates side-to-side (2D wave) | `∿∿∿∿∿` |
| ZIGZAG | Ray has sharp angular bends | `/\/\/\/\` |
| SAWTOOTH | Ray has sawtooth pattern | `/|/|/|/|` |
| SQUARE_WAVE | Ray has square wave pattern | `⊓⊔⊓⊔⊓` |
| ARC | Ray curves in a single arc | `⌒` |
| S_CURVE | Ray has S-shaped double curve | `∫` |
| TAPERED | Ray gets thinner toward end | `▷───────▷` (visual, not shape) |

**Note:** These define how the ray LOOKS, not how it moves.

---

### CATEGORY B: RAY FIELD CURVATURE (How Rays Curve Around Center)

How do all the rays CURVE relative to the field center? (Global arrangement)

| Enum | Description | Visual |
|------|-------------|--------|
| RADIAL | Rays point straight outward from center | `*` starburst |
| TANGENTIAL | Rays are tangent to circles around center | Perpendicular to radial |
| VORTEX | Rays curve into a vortex/whirlpool | `@` spinning disk |
| SPIRAL_ARM | Rays form spiral galaxy arms | `🌀` galaxy |
| PINWHEEL | Rays curve like windmill blades | `卍` curved |
| LOGARITHMIC | Rays follow logarithmic spiral curves | Nautilus shell |

**Note:** This is about the GLOBAL pattern, not individual ray shape.

---

### CATEGORY C: RAY LENGTH ANIMATION (How Ray Length Changes Over Time)

How does the ray's VISIBLE LENGTH change over time?

| Enum | Description | Animation |
|------|-------------|-----------|
| NONE | Ray is always full length | Static |
| RADIATE | Ray GROWS from center outward | `. → ── → ─────` |
| ABSORB | Ray SHRINKS from outer to center | `───── → ── → .` |
| PULSE | Ray length oscillates in/out | `─── ↔ ─────` |
| SEGMENT | Fixed-length segment visible on full-length ray | `..───..` static |
| GROW_SHRINK | Ray grows then shrinks (one-shot) | `. → ───── → .` |

---

### CATEGORY D: RAY TRAVEL ANIMATION (Energy Moving ALONG the Ray)

Does something TRAVEL along the ray's length?

| Enum | Description | Animation |
|------|-------------|-----------|
| NONE | Uniform brightness along ray | `═════════` |
| CHASE | Discrete bright particles move along ray | `•───•───•` → |
| SCROLL | Smooth gradient slides along ray | Gradient → |
| PULSE_WAVE | Brightness pulse(s) travel along ray | `  ▓▓░░    ▓▓░░  ` → |
| SPARK | Random bright sparks shoot along ray | Occasional flash |
| COMET | Bright head with fading tail travels | `═▓▓░░` → |

---

### CATEGORY E: RAY POSITION MOTION (How the Ray MOVES in Space)

How does EACH INDIVIDUAL RAY move in 3D space?

| Enum | Description | Animation |
|------|-------------|-----------|
| NONE | Ray stays in its original position | Static |
| RADIAL_DRIFT | Ray drifts outward/inward radially | Ray slides in/out |
| RADIAL_OSCILLATE | Ray oscillates in/out radially | Ray pulses in/out |
| ANGULAR_DRIFT | Ray slowly rotates around center | Ray orbits |
| ANGULAR_OSCILLATE | Ray sways side-to-side around center | Ray swings |
| FLOAT | Ray bobs up/down (Y axis) | Vertical wave |
| SWAY | Ray tips/pivots at its base | End waves, base fixed |
| ORBIT | Ray revolves around center (like spoke on wheel) | Continuous rotation |

---

### CATEGORY F: RAY WIGGLE/UNDULATION (Ray Itself Moves as a Wave)

Does the ray WIGGLE or UNDULATE as an animation?

| Enum | Description | Animation |
|------|-------------|-----------|
| NONE | Ray shape is static | No wiggle |
| WIGGLE | Ray wiggles side-to-side (traveling wave) | Snake swimming |
| WRITHE | Ray writhes in 3D (tentacle motion) | Octopus arm |
| SHIMMER | Ray vibrates rapidly (subtle) | Heat haze |
| WHIP | Ray cracks like a whip (one-shot) | Crack motion |
| RIPPLE | Wave travels from base to tip | Sequential ripple |

---

### CATEGORY G: RAY TWIST/ROTATION (Ray Rotates Around Its OWN Axis)

Does the ray ROTATE around its own lengthwise axis?

| Enum | Description | Animation |
|------|-------------|-----------|
| NONE | No twist | Static |
| TWIST | Ray rotates around its own axis | Constant spin |
| OSCILLATE_TWIST | Ray twists back and forth | Oscillating spin |
| WIND_UP | Ray progressively twists more over time | Winding |
| UNWIND | Ray untwists over time | Unwinding |

**Note:** This is for rays that have visible 3D shape (like CORKSCREW or SPRING)

---

### CATEGORY H: RAY ALPHA/FLICKER (Visibility Changes)

Does the ray's OVERALL VISIBILITY change?

| Enum | Description | Animation |
|------|-------------|-----------|
| NONE | Constant alpha | Solid |
| STROBE | All rays blink on/off in sync | Flash together |
| SCINTILLATE | Individual rays twinkle randomly | Sparkle |
| FADE_PULSE | Ray fades in/out smoothly | Breathing alpha |
| FLICKER | Ray flickers like candlelight | Unstable |
| LIGHTNING | Ray flashes brightly then fades | Flash then dim |

---

### CATEGORY I: RAY COLOR ANIMATION (Color Changes)

Does the ray's COLOR change?

| Enum | Description | Animation |
|------|-------------|-----------|
| NONE | Constant color | Solid |
| COLOR_CYCLE | Colors cycle through hue | Rainbow shift |
| COLOR_GRADIENT_SCROLL | Color gradient scrolls along ray | Moving gradient |
| HEAT_MAP | Color based on position (radius) | Inner=hot, outer=cold |
| RANDOM_PULSE | Random color pulses | Occasional color flash |

---

## SUMMARY: COMPLETE ENUM LIST

```
A. RayLineShape:       STRAIGHT, CORKSCREW, SPRING, SINE_WAVE, ZIGZAG, SAWTOOTH, SQUARE_WAVE, ARC, S_CURVE, TAPERED
B. RayFieldCurvature:  RADIAL, TANGENTIAL, VORTEX, SPIRAL_ARM, PINWHEEL, LOGARITHMIC
C. LengthMode:         NONE, RADIATE, ABSORB, PULSE, SEGMENT, GROW_SHRINK
D. TravelMode:         NONE, CHASE, SCROLL, PULSE_WAVE, SPARK, COMET
E. PositionMotion:     NONE, RADIAL_DRIFT, RADIAL_OSCILLATE, ANGULAR_DRIFT, ANGULAR_OSCILLATE, FLOAT, SWAY, ORBIT
F. WiggleMode:         NONE, WIGGLE, WRITHE, SHIMMER, WHIP, RIPPLE
G. TwistMode:          NONE, TWIST, OSCILLATE_TWIST, WIND_UP, UNWIND
H. FlickerMode:        NONE, STROBE, SCINTILLATE, FADE_PULSE, FLICKER, LIGHTNING
I. ColorMode:          NONE, COLOR_CYCLE, COLOR_GRADIENT_SCROLL, HEAT_MAP, RANDOM_PULSE
```

---

## WHAT'S CURRENTLY IMPLEMENTED (for comparison)

| Category | Current Enum | Current Values |
|----------|--------------|----------------|
| A | (none) | N/A |
| B | RayArrangement | RADIAL, PARALLEL, SPHERICAL |
| C | LengthMode | NONE, RADIATE, ABSORB, PULSE |
| D | TravelMode | NONE, CHASE, SCROLL |
| E | MotionMode | NONE, LINEAR, OSCILLATE, SPIRAL, RIPPLE |
| F | (none) | N/A |
| G | (none) | N/A |
| H | FlickerMode | NONE, SCINTILLATION, STROBE |
| I | ColorCycleConfig | (separate config object) |

---

## WHAT'S MISSING / NEEDS FIXING

1. **Category A (RayLineShape)** - Not implemented. All rays are straight.
2. **Category B** - Need VORTEX, SPIRAL_ARM, TANGENTIAL
3. **Category C** - Rename? Current names are fine.
4. **Category D** - Add PULSE_WAVE, SPARK, COMET
5. **Category E** - Current MotionMode is confusing. Split into clearer names.
6. **Category F (WiggleMode)** - Not implemented.
7. **Category G (TwistMode)** - Not implemented.
8. **Category H** - Add FADE_PULSE, LIGHTNING
9. **Category I** - Already has ColorCycleConfig, but could expand

---

## PRIORITY FOR IMPLEMENTATION

### Phase 1: Fix Core (Current Broken Modes)
- [ ] Fix LengthMode (RADIATE, ABSORB, PULSE) - geometry-based
- [ ] Fix MotionMode - clarify what each does

### Phase 2: Essential New Features
- [ ] Add RayFieldCurvature.VORTEX (accretion disk effect)
- [ ] Add RayLineShape.CORKSCREW
- [ ] Add RayLineShape.SPRING

### Phase 3: Polish
- [ ] Add WiggleMode
- [ ] Add TwistMode
- [ ] Expand TravelMode

---

## YOUR REVIEW NEEDED

1. Is this exhaustive enough?
2. What did I miss?
3. Which categories should be merged?
4. Which modes should be renamed?
5. What's the priority order?
