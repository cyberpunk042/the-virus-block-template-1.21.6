---
description: Enhanced 3D Preview Renderer - Approach A (2D Projected Solid Fill)
---

# Enhanced Field Preview Renderer - Implementation Plan

## Objective
Upgrade the "Fast" 3D preview to accurately render fields based on fill mode (SOLID, WIREFRAME, CAGE, POINTS) using 2D projected rendering with proper triangle tessellation, depth sorting, and alpha support.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    FieldPreviewRenderer                          │
│  (Orchestrator - reads state, delegates to shape renderers)     │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                   PreviewTessellator                             │
│  (Interface - each shape implements triangle generation)        │
├─────────────────────────────────────────────────────────────────┤
│  SphereTessellator │ RingTessellator │ CylinderTessellator     │
│  DiscTessellator   │ PrismTessellator │ ConeTessellator        │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PreviewProjector                              │
│  (Projects 3D points to 2D, applies rotation, calculates depth) │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PreviewRasterizer                             │
│  (Sorts triangles, fills them, draws edges based on FillMode)   │
└─────────────────────────────────────────────────────────────────┘
```

---

## New Classes & Files

### 1. `net.cyberpunk042.client.gui.preview.PreviewTriangle.java`
**Purpose**: Immutable data record for a projected triangle ready for rendering.

```java
public record PreviewTriangle(
    // Screen coordinates (2D)
    float x1, float y1,
    float x2, float y2,
    float x3, float y3,
    // Depth for sorting (average Z after projection)
    float depth,
    // Colors
    int fillColor,  // ARGB with alpha
    int edgeColor   // For wireframe edges
) {
    /** Creates triangle with same fill/edge color */
    public static PreviewTriangle of(float x1, float y1, float x2, float y2, 
                                      float x3, float y3, float depth, int color) {
        return new PreviewTriangle(x1, y1, x2, y2, x3, y3, depth, color, color);
    }
}
```

### 2. `net.cyberpunk042.client.gui.preview.PreviewProjector.java`
**Purpose**: Handles 3D-to-2D projection with rotation and perspective.

```java
public class PreviewProjector {
    private final float centerX, centerY;
    private final float scale;
    private final float rotX, rotY;  // Rotation angles (radians)
    private static final float PERSPECTIVE = 0.4f;
    
    public PreviewProjector(float centerX, float centerY, float scale, 
                            float rotationPitch, float rotationYaw) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.scale = scale;
        this.rotX = (float) Math.toRadians(rotationPitch);
        this.rotY = (float) Math.toRadians(rotationYaw);
    }
    
    /**
     * Projects a 3D point to 2D screen coordinates.
     * @return float[3]: {screenX, screenY, depth}
     */
    public float[] project(float x, float y, float z) {
        // Rotate around Y axis (yaw)
        float cosY = MathHelper.cos(rotY);
        float sinY = MathHelper.sin(rotY);
        float x1 = x * cosY - z * sinY;
        float z1 = x * sinY + z * cosY;
        
        // Rotate around X axis (pitch)
        float cosX = MathHelper.cos(rotX);
        float sinX = MathHelper.sin(rotX);
        float y1 = y * cosX - z1 * sinX;
        float z2 = y * sinX + z1 * cosX;
        
        // Perspective projection
        float perspectiveFactor = 1f / (1f + z2 * PERSPECTIVE * 0.1f);
        
        return new float[] {
            centerX + x1 * scale * perspectiveFactor,
            centerY - y1 * scale * perspectiveFactor,  // Flip Y for screen
            z2  // Depth for sorting
        };
    }
    
    /** Projects and calculates average depth of triangle */
    public PreviewTriangle projectTriangle(float[] v1, float[] v2, float[] v3, int color) {
        float[] p1 = project(v1[0], v1[1], v1[2]);
        float[] p2 = project(v2[0], v2[1], v2[2]);
        float[] p3 = project(v3[0], v3[1], v3[2]);
        float avgDepth = (p1[2] + p2[2] + p3[2]) / 3f;
        return new PreviewTriangle(p1[0], p1[1], p2[0], p2[1], p3[0], p3[1], avgDepth, color, color);
    }
}
```

### 3. `net.cyberpunk042.client.gui.preview.PreviewTessellator.java`
**Purpose**: Interface for shape-specific triangle generation.

```java
public interface PreviewTessellator {
    /**
     * Generates triangles for this shape.
     * @param projector Handles 3D-to-2D projection
     * @param state Field edit state with shape parameters
     * @param color Fill color (ARGB)
     * @param detail Number of segments (configurable)
     * @return List of projected triangles
     */
    List<PreviewTriangle> tessellate(PreviewProjector projector, 
                                      FieldEditState state, 
                                      int color, 
                                      int detail);
}
```

### 4. Shape Tessellator Implementations
Each in `net.cyberpunk042.client.gui.preview.tessellator`:

| Class | Shape | Notes |
|-------|-------|-------|
| `SphereTessellator` | Sphere | Lat/lon grid → quads → triangles |
| `RingTessellator` | Ring/Torus | Outer ring + inner ring |
| `DiscTessellator` | Disc | Center + radial segments |
| `CylinderTessellator` | Cylinder | Top cap + bottom cap + wall |
| `PrismTessellator` | Prism | Top polygon + bottom polygon + walls |
| `ConeTessellator` | Cone | Base circle + apex triangles |
| `CapsuleTessellator` | Capsule | Cylinder + hemisphere caps |

### 5. `net.cyberpunk042.client.gui.preview.PreviewRasterizer.java`
**Purpose**: Handles sorting, filling triangles, and edge rendering.

```java
public class PreviewRasterizer {
    
    /**
     * Renders triangles based on fill mode.
     */
    public static void render(DrawContext ctx, List<PreviewTriangle> triangles, 
                              FillMode mode, int edgeColor) {
        // Sort back-to-front for proper alpha compositing
        triangles.sort((a, b) -> Float.compare(b.depth, a.depth));
        
        switch (mode) {
            case SOLID -> renderSolid(ctx, triangles);
            case WIREFRAME -> renderWireframe(ctx, triangles, edgeColor);
            case CAGE -> renderCage(ctx, triangles, edgeColor);
            case POINTS -> renderPoints(ctx, triangles, edgeColor);
        }
    }
    
    private static void renderSolid(DrawContext ctx, List<PreviewTriangle> tris) {
        for (var t : tris) {
            fillTriangle(ctx, t.x1(), t.y1(), t.x2(), t.y2(), t.x3(), t.y3(), t.fillColor());
        }
    }
    
    private static void renderWireframe(DrawContext ctx, List<PreviewTriangle> tris, int edgeColor) {
        // Draw all edges
        for (var t : tris) {
            drawLine(ctx, t.x1(), t.y1(), t.x2(), t.y2(), edgeColor);
            drawLine(ctx, t.x2(), t.y2(), t.x3(), t.y3(), edgeColor);
            drawLine(ctx, t.x3(), t.y3(), t.x1(), t.y1(), edgeColor);
        }
    }
    
    // ... fillTriangle using scanline algorithm
}
```

### 6. `PreviewConfig.java` (or add to existing config)
**Purpose**: Stores preview quality setting.

```java
// In TraceSubPanel or a dedicated config:
public static int previewDetail = 24;  // Default detail level (8-64)
```

---

## Modified Classes

### `FieldPreviewRenderer.java`
Becomes a thin orchestrator that:
1. Reads `FillMode` from `state.fill().mode()`
2. Reads color/alpha from `state.appearance()`
3. Creates `PreviewProjector` with rotation
4. Delegates to appropriate `PreviewTessellator`
5. Passes triangles to `PreviewRasterizer`

```java
public static void drawField(DrawContext context, FieldEditState state,
                             int x1, int y1, int x2, int y2,
                             float scale, float rotationX, float rotationY) {
    // Setup
    context.enableScissor(x1, y1, x2, y2);
    
    float centerX = (x1 + x2) / 2f;
    float centerY = (y1 + y2) / 2f;
    float boundsSize = Math.min(x2 - x1, y2 - y1) - 20;
    
    // Get shape parameters
    String shapeType = state.getString("shapeType");
    float radius = getRadiusForShape(state, shapeType);
    float finalScale = (boundsSize / (radius * 2.5f)) * scale;
    
    // Get appearance
    FillMode mode = state.fill().mode();
    int color = state.appearance().color();
    float alpha = state.appearance().alpha() != null 
        ? state.appearance().alpha().max() : 1.0f;
    int colorWithAlpha = applyAlpha(color, alpha);
    
    // Create projector
    PreviewProjector projector = new PreviewProjector(
        centerX, centerY, finalScale, rotationX, rotationY);
    
    // Get tessellator for shape
    PreviewTessellator tessellator = getTessellatorFor(shapeType);
    int detail = PreviewConfig.previewDetail;
    
    // Generate and render
    List<PreviewTriangle> triangles = tessellator.tessellate(
        projector, state, colorWithAlpha, detail);
    PreviewRasterizer.render(context, triangles, mode, color);
    
    context.disableScissor();
}
```

---

## Implementation Order

### Phase 1: Core Infrastructure (Foundation)
1. ✅ Create `PreviewTriangle` record
2. ✅ Create `PreviewProjector` class  
3. ✅ Create `PreviewRasterizer` with `fillTriangle()` algorithm
4. ✅ Create `PreviewTessellator` interface

### Phase 2: Shape Tessellators
5. ✅ Implement `SphereTessellator` (most common)
6. ✅ Implement `CylinderTessellator`
7. ✅ Implement `PrismTessellator`
8. ✅ Implement `RingTessellator`
9. ✅ Implement `DiscTessellator`
10. ✅ Implement `ConeTessellator`

### Phase 3: Integration
11. ✅ Refactor `FieldPreviewRenderer.drawField()` to use new system
12. ✅ Add fallback for unknown shapes (current wireframe)

### Phase 4: Configuration
13. ✅ Add "Preview Detail" slider to Debug/Trace panel
14. ✅ Store setting in PreviewConfig

### Phase 5: Polish
15. ✅ Test all shapes × all fill modes
16. ✅ Optimize hot paths if needed
17. ✅ Add cage-specific rendering (structured lines only)

---

## Testing Matrix

| Shape | SOLID | WIREFRAME | CAGE | POINTS |
|-------|-------|-----------|------|--------|
| Sphere | ☐ | ☐ | ☐ | ☐ |
| Ring | ☐ | ☐ | ☐ | ☐ |
| Disc | ☐ | ☐ | ☐ | ☐ |
| Cylinder | ☐ | ☐ | ☐ | ☐ |
| Prism | ☐ | ☐ | ☐ | ☐ |
| Cone | ☐ | ☐ | ☐ | ☐ |
| Capsule | ☐ | ☐ | ☐ | ☐ |

---

## File Structure

```
src/client/java/net/cyberpunk042/client/gui/preview/
├── FieldPreviewRenderer.java       # Existing - refactor
├── PreviewTriangle.java            # NEW
├── PreviewProjector.java           # NEW
├── PreviewRasterizer.java          # NEW
├── PreviewTessellator.java         # NEW (interface)
├── PreviewConfig.java              # NEW (or add to existing)
└── tessellator/
    ├── SphereTessellator.java      # NEW
    ├── RingTessellator.java        # NEW
    ├── DiscTessellator.java        # NEW
    ├── CylinderTessellator.java    # NEW
    ├── PrismTessellator.java       # NEW
    ├── ConeTessellator.java        # NEW
    └── CapsuleTessellator.java     # NEW
```

---

## Key Algorithm: Triangle Fill (Scanline)

```java
private static void fillTriangle(DrawContext ctx, 
                                  float x1, float y1,
                                  float x2, float y2, 
                                  float x3, float y3, int color) {
    // Sort vertices by Y (top to bottom)
    if (y1 > y2) { float t = x1; x1 = x2; x2 = t; t = y1; y1 = y2; y2 = t; }
    if (y1 > y3) { float t = x1; x1 = x3; x3 = t; t = y1; y1 = y3; y3 = t; }
    if (y2 > y3) { float t = x2; x2 = x3; x3 = t; t = y2; y2 = y3; y3 = t; }
    
    int minY = (int) Math.ceil(y1);
    int maxY = (int) Math.floor(y3);
    
    for (int y = minY; y <= maxY; y++) {
        float leftX, rightX;
        
        if (y < y2) {
            // Top half
            leftX = interpolateX(y, y1, x1, y2, x2);
            rightX = interpolateX(y, y1, x1, y3, x3);
        } else {
            // Bottom half
            leftX = interpolateX(y, y2, x2, y3, x3);
            rightX = interpolateX(y, y1, x1, y3, x3);
        }
        
        if (leftX > rightX) { float t = leftX; leftX = rightX; rightX = t; }
        
        ctx.fill((int) leftX, y, (int) rightX + 1, y + 1, color);
    }
}

private static float interpolateX(float y, float y1, float x1, float y2, float x2) {
    if (Math.abs(y2 - y1) < 0.001f) return x1;
    return x1 + (x2 - x1) * (y - y1) / (y2 - y1);
}
```

---

## Ready to Implement?

When you're ready, we'll start with Phase 1 (Core Infrastructure) by creating the new files one by one.

