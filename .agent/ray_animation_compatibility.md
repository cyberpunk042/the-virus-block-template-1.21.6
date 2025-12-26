# Ray Animation Mode Compatibility Guide

This document outlines the requirements, limitations, and compatibility of all ray animation modes.

---

## Quick Reference: Mode Requirements

### Legend
- ✅ Works with any configuration
- ⚠️ Has performance or visibility considerations  
- ❌ Requires specific setup to work
- 🔧 Requires multi-segment rays (Line Shape ≠ STRAIGHT)

---

## 1. LENGTH MODES (RayFlow)

| Mode | Requirements | Performance | Notes |
|------|--------------|-------------|-------|
| **NONE** | ✅ | Fast | No animation |
| **RADIATE** | ✅ | Fast | Rays flow outward, respawn at center. Works best with segmentLength < 1.0 |
| **ABSORB** | ✅ | Fast | Rays flow inward, respawn at outer. Works best with segmentLength < 1.0 |
| **SEGMENT** | ✅ | Fast | Sliding window visibility. segmentLength controls visible portion |
| **GROW_SHRINK** | ✅ | Fast | Rays grow from center then shrink back |
| **PULSE** | ✅ | Fast | Rays breathe (scale in/out) |

**Tips:**
- RADIATE/ABSORB: Use `segmentLength` slider (default 1.0 = full ray visible). Try 0.3-0.5 for particle-like effect.
- Per-ray staggering creates continuous flow with multiple rays.

---

## 2. TRAVEL MODES (RayFlow)

| Mode | Requirements | Performance | Notes |
|------|--------------|-------------|-------|
| **NONE** | ✅ | Fast | No alpha animation |
| **CHASE** | ✅ | Fast | Bright spots travel along ray |
| **FADE_IN_OUT** | ✅ | Fast | Alpha pulses over time |
| **DISSOLVE** | ✅ | Fast | Random dithering fade |
| **BREATHE** | ✅ | Fast | Smooth alpha oscillation |
| **GRADIENT** | ✅ | Fast | Static gradient alpha along ray |

**Tips:**
- These modulate ALPHA, not geometry. Always visible if ray count > 0.

---

## 3. MOTION MODES (RayMotion)

| Mode | Requirements | Performance | Notes |
|------|--------------|-------------|-------|
| **NONE** | ✅ | Fast | No motion |
| **RADIAL_DRIFT** | ✅ | Fast | Rays slide outward continuously |
| **RADIAL_OSCILLATE** | ✅ | Fast | Rays pulse in/out radially |
| **ANGULAR_OSCILLATE** | ✅ | Fast | Rays sway side-to-side |
| **ANGULAR_DRIFT** | ✅ | Fast | Rays rotate slowly around center |
| **ORBIT** | ✅ | Fast | Rays spiral around center |
| **FLOAT** | ✅ | Fast | Rays bob up and down |
| **SWAY** | ✅ | Medium | Pendulum effect, more visible on longer rays |
| **JITTER** | ✅ | Medium | Random noise - uses hash function |
| **PRECESS** | ⚠️ | **SLOW** | Gyroscopic wobble. **Max ~50 rays recommended** |
| **RIPPLE** | ✅ | Fast | Radial wave pulses outward |

### ⚠️ PRECESS Performance Warning
The PRECESS mode computes per-ray angular displacement based on precession direction. This involves:
- `atan2()` per ray
- Angular difference calculation with normalization
- `cos()` per ray

**Recommendations:**
- Keep ray count ≤ 50 for smooth performance
- Lower amplitude/speed helps reduce visual complexity
- Consider using ANGULAR_DRIFT or ORBIT instead for similar effects

---

## 4. WIGGLE MODES (RayWiggle)

| Mode | Requirements | Performance | Notes |
|------|--------------|-------------|-------|
| **NONE** | ✅ | Fast | No wiggle |
| **WIGGLE** | 🔧 | Fast | Snake-like wave. **Needs multi-segment rays** |
| **WOBBLE** | 🔧 | Fast | Tip wobbles. **Needs multi-segment rays** |
| **WRITHE** | 🔧 | Fast | 3D tentacle motion. **Needs multi-segment rays** |
| **SHIMMER** | 🔧 | Medium | Rapid subtle vibration. Uses hash |
| **RIPPLE** | 🔧 | Fast | Wave travels base to tip. **Needs multi-segment rays** |
| **WHIP** | 🔧 | Fast | Crack like a whip. **Needs multi-segment rays** |
| **FLUTTER** | 🔧 | Medium | Rapid chaotic motion. Uses hash |
| **SNAKE** | 🔧 | Fast | Fluid multi-frequency wave. **Needs multi-segment rays** |
| **PULSE_WAVE** | 🔧 | Fast | Thickness pulsing along ray. **Needs multi-segment rays** |

### 🔧 Multi-Segment Requirement
Most wiggle modes require **Line Shape ≠ STRAIGHT** or **Line Segments > 1** to be visible.

**Why:** Wiggle works by displacing vertices perpendicular to the ray direction. For straight rays (2 vertices), there are no intermediate points to displace - only the endpoints exist.

**To enable:**
1. Set **Line Shape** to: SINE_WAVE, ZIGZAG, CORKSCREW, SPRING, HELIX, SAWTOOTH, TRIANGLE, SQUARE, NOISE, or DOUBLE_HELIX
2. OR set **Line Segments** slider > 1 (this adds intermediate vertices even for straight rays)

---

## 5. TWIST MODES (RayTwist)

| Mode | Requirements | Performance | Notes |
|------|--------------|-------------|-------|
| **NONE** | ✅ | Fast | No twist |
| **TWIST** | 🔧 | Fast | Continuous rotation. **Needs 3D line shape** |
| **OSCILLATE_TWIST** | 🔧 | Fast | Back-and-forth oscillation. **Needs 3D line shape** |
| **WIND_UP** | 🔧 | Fast | Progressive increase. **Needs 3D line shape** |
| **UNWIND** | 🔧 | Fast | Progressive decrease. **Needs 3D line shape** |
| **SPIRAL_TWIST** | 🔧 | Fast | More twist at tip. **Needs 3D line shape** |

### 🔧 3D Line Shape Requirement
Twist rotates vertices around the ray's central axis. For **straight rays**, the vertices ARE on the axis - rotating them around the axis has no visible effect!

**To enable:**
Set **Line Shape** to a 3D shape: **CORKSCREW**, **SPRING**, **HELIX**, **DOUBLE_HELIX**

These shapes have vertices offset from the central axis, so rotating around the axis creates visible drilling/spinning effects.

---

## Incompatible Combinations

| Combination | Result | Solution |
|-------------|--------|----------|
| Any Wiggle + Line Shape: STRAIGHT | No effect | Change Line Shape or increase Line Segments |
| Any Twist + Line Shape: STRAIGHT | No effect | Use CORKSCREW, SPRING, or HELIX |
| PRECESS + Count > 100 | Severe lag | Reduce count or use ANGULAR_DRIFT |
| Curvature ≠ NONE + Many segments | Possible lag | Reduce Line Segments or ray Count |
| Multiple animation types active | Cumulative performance | Disable unused animations |

---

## Recommended Presets

### "Solar Flare" (Radiate + Writhe)
- Shape: Rays, Radial arrangement
- Count: 50, Inner/Outer: 0.5/3.0
- Line Shape: SINE_WAVE, Segments: 16
- Flow: RADIATE, segmentLength: 0.4
- Wiggle: WRITHE, Amp: 0.2, Freq: 3

### "Black Hole" (Absorb + Vortex)
- Shape: Rays, Radial arrangement
- Count: 80, Inner/Outer: 0.3/4.0
- Curvature: VORTEX, Intensity: 1.5
- Flow: ABSORB, segmentLength: 0.5
- Motion: ORBIT, Speed: 0.3

### "Drilling Laser" (Twist)
- Shape: Rays, Count: 8
- Line Shape: CORKSCREW, Amp: 0.3, Freq: 4
- Twist: TWIST, Speed: 1.0, Amount: 360°

### "Electric Field" (Jitter + Shimmer)
- Shape: Rays, Spherical arrangement
- Count: 30, Segments: 8
- Wiggle: FLUTTER, Speed: 2.0, Amp: 0.1
- Motion: JITTER, Amp: 0.3

---

## GUI Warning Implementation Notes

The following scenarios should trigger info tooltips or warning messages:

1. **Wiggle mode selected + Line Shape is STRAIGHT + Line Segments = 1**
   - Message: "⚠️ Wiggle requires multi-segment rays. Set Line Shape ≠ STRAIGHT or increase Line Segments."

2. **Twist mode selected + Line Shape is STRAIGHT**
   - Message: "⚠️ Twist requires a 3D Line Shape (CORKSCREW, SPRING, HELIX) to be visible."

3. **PRECESS mode selected + Ray Count > 50**
   - Message: "⚠️ PRECESS mode may cause lag with many rays. Consider reducing Count or using ANGULAR_DRIFT."

4. **Multiple ray animations active + Count > 100**
   - Message: "⚠️ Multiple animations with high ray count may impact performance."

5. **Curvature ≠ NONE + Line Segments > 64**
   - Message: "⚠️ High segment count with curvature may impact performance."
