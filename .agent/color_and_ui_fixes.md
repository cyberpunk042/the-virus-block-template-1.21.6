# Color Modes & UI Fixes - Analysis and Plan

## Problem Summary

### Color Mode Issues

1. **MESH_GRADIENT/MESH_RAINBOW don't animate with timePhase**
   - The timePhase exists but animation is broken because of ping-pong logic issues

2. **Only works on Sphere shape**
   - The issue is that different renderers pass coordinates differently:
   - **Sphere**: Uses actual world positions (x, y, z) from tessellator vertices
   - **Rays**: Uses hacked "centeredY = rayT - 0.5" for Y, real x/z
   - **Jet**: Likely has similar issues
   - **Other shapes**: Need verification

### Root Cause Analysis

The `GradientDirection.calculateT()` method expects:
- Y_AXIS: y ranges from -height/2 to +height/2 (centered at 0)
- RADIAL: x/z are actual positions for radial distance
- ALONG_LENGTH: y ranges from 0 to height

But different shapes provide:
- **Sphere**: y = actual position (-radius to +radius) ✓ Y_AXIS works
- **Rays**: y = rayT - 0.5 (-0.5 to +0.5), height = 1.0 ✓ Y_AXIS works
- **Jet**: Unknown, needs investigation
- **Cylinder/Prism/Capsule**: y = actual position, varies by shape

The issue is that **Y_AXIS assumes centered coordinates** but not all shapes center their Y around 0.

### UI Issues (from screenshots)

1. **Sliders partially visible** - Left edge cut off
2. **Description text wrapping incorrectly** - Wraps at start instead of end
3. **Text size too large** - Descriptions should use smaller font

## Implementation Plan

### Phase 1: Fix Color Mode Animation

**File: `ColorContext.java`**

The current MESH_GRADIENT has ping-pong animation that's not correct:
```java
float animOffset = timePhase * time / 20f;
t = t + animOffset;
// Ping-pong logic...
```

Issues:
- `timePhase * time` means animation speed increases over time (wrong!)
- Should be: `time / 20f * animSpeed + timePhase` where timePhase is a phase offset

**Fix**: Animation should use `time` for animation and `timePhase` as phase OFFSET:
```java
float animPhase = (time / 20f + timePhase) % 1f;
t = (t + animPhase) % 1f;  // MESH_RAINBOW uses modulo for cyclic colors
// OR for MESH_GRADIENT (non-cyclic):
t = Math.max(0, Math.min(1, t));  // Keep in bounds without wrapping
```

### Phase 2: Fix Shape-Specific Color Coordinates

**Problem**: Each shape has different coordinate conventions.

**Solution Options**:
1. **Normalize all shapes to same convention** - Requires changing all tessellators
2. **Add shape-specific direction handling** - Add ALONG_LENGTH direction for rays/jets
3. **Pass normalized t-value directly** - Add optional t parameter to calculateColor()

**Recommended: Option 3** - Most flexible, least code change

Add to ColorContext:
```java
public int calculateColor(float x, float y, float z, int cellIndex, float explicitT) {
    // If explicitT >= 0, use it directly instead of calculateT()
}
```

Rays renderer already has rayT, can pass it directly.

### Phase 3: Fix UI Issues

**File: `ContentBuilder.java`**

1. **Fix description text**: Use smaller font or truncate with ellipsis
2. **Fix text alignment**: Ensure left edge is at padding, not before
3. **Consider multi-line support**: TextWidget doesn't wrap, need alternatives

**Options for descriptions**:
- Use `Text.literal().styled(style -> style.withUnderline(true))` for smaller appearance
- Truncate text with "..." if too long
- Use narrower panel width calculation

## Execution Order

1. First: Fix animation formula (quick fix)
2. Second: Add explicit t-value support for renderers that know their t
3. Third: Fix UI issues (separate concern)

## Files to Modify

- `ColorContext.java` - Fix animation, add explicitT parameter
- `RaysRenderer.java` - Pass rayT directly
- `JetRenderer.java` (if exists) - Similar fix
- `ContentBuilder.java` - Fix label sizing
