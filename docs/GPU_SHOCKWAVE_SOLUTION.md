# GPU Shockwave - Solution Summary

## The Answer Was Already Documented (KI Section 12-13)

From `framegraph_post_processing.md`:

> **"The PostEffectProcessor does not automatically 'listen' to external UBO binders. 
> It creates its own internal state, which is strictly governed by the static defaults 
> in its JSON definition."**

> **"Managed processors maintain a private uniform binding lifecycle that 
> bypasses `bindDefaultUniforms`."**

---

## WHY Fresnel/Corona WORKS but Shockwave DOESN'T

| Feature | Fresnel/Corona | Shockwave |
|---------|----------------|-----------|
| Uses | `RenderPipeline` (via RenderLayer) | `PostEffectProcessor` |
| Uniform Source | `CustomUniformBinder` via mixin | JSON file |
| `bindDefaultUniforms` called? | ✅ YES | ❌ NO |
| Dynamic updates work? | ✅ YES | ❌ NO |

**Fresnel draws geometry** → Goes through RenderPipeline → Mixin catches it → Uniforms updated

**Shockwave uses PostEffectProcessor** → Has its own internal uniform binding → Ignores our mixin

---

## THE SOLUTION (from KI Section 9)

> "The ultimate solution for high-fidelity interactive VFX in the 1.21.6 architecture 
> is the **Explicit Custom Pipeline** pattern."

### What We Need To Do:

1. **Don't use PostEffectProcessor**
2. **Create a custom RenderPipeline** (like FresnelPipelines)
3. **Draw a fullscreen quad** with that pipeline
4. **Bind depth texture** manually as a sampler
5. **Use CustomUniformBinder** - it will work because we're using RenderPipeline

### Code Pattern (from KI):

```java
public static final RenderPipeline SHOCKWAVE = RenderPipelines.register(
    RenderPipeline.builder(RenderPipelines.POST_EFFECT_PROCESSOR_SNIPPET)
        .withVertexShader(ID).withFragmentShader(ID)
        .withSampler("InSampler").withSampler("DepthSampler")
        .withUniform("ShockwaveParams", UniformType.UNIFORM_BUFFER)
        .build()
);
```

---

## NEXT STEPS

1. Delete the `WorldRendererShockwaveMixin` PostEffectProcessor injection (it's the wrong approach)
2. Create `ShockwavePipelines` properly with fullscreen quad support
3. Hook rendering at `WorldRenderer.render` RETURN point
4. Draw fullscreen quad with our pipeline
5. Uniforms will work automatically via existing `CustomUniformBinder`

---

## File Reference

Full documentation in:
`C:\Users\Jean\.gemini\antigravity\knowledge\field_visual_system_architecture\artifacts\rendering_architecture\framegraph_post_processing.md`
