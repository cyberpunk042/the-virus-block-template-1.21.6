---
description: Full 3D Preview via Framebuffer - Implementation Plan
---

# 🎯 MISSION: Full 3D Preview via Framebuffer

**Goal:** Render fields to an offscreen framebuffer using the real world rendering pipeline, then display as a texture in the GUI. This provides 100% visual fidelity with world rendering.

**Why Framebuffer?** Direct rendering with FieldRenderer failed because entity shaders expect world-space projection/camera uniforms that aren't set up in GUI context. Framebuffer approach isolates the rendering and gives us full control.

---

## ⚠️ RULES TO FOLLOW

1. **Complete each phase before moving to the next**
2. **Test after each phase** - don't accumulate untested code
3. **Errors must crash** - no silent failures (user's explicit request)
4. **Fall back gracefully** - if framebuffer fails, use Fast mode
5. **Don't skip steps** - even if they seem obvious

---

## Phase 0: Research & Preparation
**Status:** ✅ COMPLETE

### 0.1 Investigate Minecraft's Framebuffer API
- [x] Find `SimpleFramebuffer` or `Framebuffer` class in 1.21
- [x] Understand create/bind/render/read lifecycle
- [x] Check existing examples (world borders, portals, etc.)

### 0.2 Understand Projection Matrix Setup
- [x] How to set up perspective projection for framebuffer
- [x] How to position a virtual camera
- [x] What uniforms entity shaders need

### 0.3 Deliverable
- [x] Document findings in comments or notes
- [x] Confirm approach is viable before proceeding

### Research Findings:

**SimpleFramebuffer API (1.21 Yarn):**
- **Package:** `net.minecraft.client.gl.SimpleFramebuffer`
- **Constructor:** `SimpleFramebuffer(String name, int width, int height, boolean useDepth)`

**Key Methods:**
- `framebuffer.beginWrite(true)` - Start rendering to framebuffer
- `framebuffer.endWrite()` - Stop rendering to framebuffer
- `framebuffer.getColorAttachment()` - Get texture ID for display

**Display in GUI:**
- Use `DrawContext.drawTexture()` with the texture ID from framebuffer
- Use `RenderPipelines.GUI_TEXTURED` for proper rendering

**Approach confirmed viable!**

---

## Phase 1: Framebuffer Manager
**Status:** ✅ COMPLETE

### 1.1 Create `PreviewFramebuffer.java`
**Location:** `src/client/java/net/cyberpunk042/client/gui/preview/PreviewFramebuffer.java`

**Actual 1.21 API (different from initial research):**
```java
public class PreviewFramebuffer {
    private SimpleFramebufferFactory factory;
    private Framebuffer framebuffer;
    
    public void ensureSize(int width, int height);
    public void prepare();  // Prepares for rendering
    public GpuTexture getColorTexture();  // Returns GpuTexture, not int!
    public Framebuffer getFramebuffer();  // For direct access
    public void close();
}
```

### 1.2 Handle Lifecycle
- [x] Create lazily on first use (via ensureSize)
- [x] Resize when bounds change (creates new factory)
- [x] Dispose on screen close (via close())

### 1.3 Deliverable
- [x] PreviewFramebuffer class compiles
- [x] API matches 1.21 patterns

---

## Phase 2: Render Context Setup
**Status:** [ ] Not Started

### 2.1 Create Virtual Camera
- [ ] Position at distance based on field radius
- [ ] Apply rotation from user input (spin, wobble)
- [ ] Look at origin (0, 0, 0)

### 2.2 Set Up Projection Matrix
- [ ] Perspective projection matching aspect ratio
- [ ] FOV ~70° for natural look
- [ ] Near/far planes for field size

### 2.3 Set Up View Matrix
- [ ] Camera position: rotated from (0, 0, distance)
- [ ] Look-at: origin
- [ ] Up vector: (0, 1, 0)

### 2.4 Deliverable
- [ ] Matrix setup code ready
- [ ] Can apply to MatrixStack

---

## Phase 3: Render to Framebuffer
**Status:** [ ] Not Started

### 3.1 Render Pipeline
```
1. framebuffer.beginWrite()
2. Clear framebuffer (transparent background)
3. Set up projection + view matrices
4. Build FieldDefinition from state
5. Call FieldRenderer.render()
6. Flush vertex consumers
7. framebuffer.endWrite()
```

### 3.2 Key Considerations
- [ ] Get VertexConsumerProvider.Immediate
- [ ] Handle light values (use fixed or calculate)
- [ ] Pass correct time for animations

### 3.3 Deliverable
- [ ] Can render a field to framebuffer
- [ ] Verify by checking framebuffer contents

---

## Phase 4: Display Framebuffer in GUI
**Status:** [ ] Not Started

### 4.1 Draw Framebuffer Texture
- [ ] Use DrawContext.drawTexture() or similar
- [ ] Map framebuffer texture to preview bounds
- [ ] Handle aspect ratio correctly

### 4.2 Blend with GUI
- [ ] Proper alpha blending
- [ ] Respect scissor bounds
- [ ] Correct depth ordering

### 4.3 Deliverable
- [ ] Framebuffer contents visible in GUI
- [ ] Properly positioned and sized

---

## Phase 5: Integration
**Status:** [ ] Not Started

### 5.1 Update `FieldPreviewRenderer.drawFieldFull()`
- [ ] Implement the full framebuffer path
- [ ] Handle all animations (spin, pulse, wobble)
- [ ] Build definition from state

### 5.2 Wire Up Toggle
- [ ] useFullRenderer = true → Framebuffer path
- [ ] useFullRenderer = false → Fast 2D path

### 5.3 Lifecycle Management
- [ ] Create framebuffer when Full mode toggled
- [ ] Dispose when screen closes
- [ ] Handle resize events

### 5.4 Deliverable
- [ ] Full/Fast toggle works
- [ ] No resource leaks

---

## Phase 6: Testing & Polish
**Status:** [ ] Not Started

### 6.1 Test Cases
- [ ] Basic sphere rendering
- [ ] All fill modes (SOLID, WIREFRAME, CAGE, POINTS)
- [ ] Animations (spin, pulse, wobble, alpha pulse)
- [ ] Visibility masks (BANDS, STRIPES, etc.)
- [ ] Patterns and arrangements
- [ ] Multiple layers
- [ ] Different shapes
- [ ] Resize preview bounds
- [ ] Toggle Full/Fast mode rapidly

### 6.2 Edge Cases
- [ ] Empty state (no primitives)
- [ ] Very large/small fields
- [ ] Screen resize
- [ ] Tab switching
- [ ] Performance check

### 6.3 Deliverable
- [ ] All tests pass
- [ ] No visual glitches
- [ ] Acceptable performance

---

## File Changes Summary

| File | Action | Purpose |
|------|--------|---------|
| `PreviewFramebuffer.java` | **NEW** | Framebuffer lifecycle management |
| `FieldPreviewRenderer.java` | **MODIFY** | Add framebuffer rendering path |
| `FieldCustomizerScreen.java` | **MODIFY** | Handle framebuffer lifecycle |

---

## Fallback Strategy

If any phase fails:
1. Log the error clearly
2. Fall back to Fast preview mode
3. Don't crash the game
4. But DO surface the error for debugging

---

## Progress Tracker

| Phase | Status | Notes |
|-------|--------|-------|
| Phase 0 | ✅ | Research - SimpleFramebuffer API found |
| Phase 1 | ⬜ | Framebuffer Manager |
| Phase 2 | ⬜ | Render Context |
| Phase 3 | ⬜ | Render to FB |
| Phase 4 | ⬜ | Display in GUI |
| Phase 5 | ⬜ | Integration |
| Phase 6 | ⬜ | Testing |

Legend: ⬜ Not Started | 🟡 In Progress | ✅ Complete | ❌ Blocked

---

**Remember:** One phase at a time. Test before moving on. Never give up! 🚀
