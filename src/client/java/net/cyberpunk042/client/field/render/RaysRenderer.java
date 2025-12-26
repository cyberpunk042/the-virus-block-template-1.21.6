package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.animation.AnimationApplier;
import net.cyberpunk042.client.visual.animation.WaveDeformer;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.RaysTessellator;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.*;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.appearance.ColorContext;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.shape.RaysShape;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Renders rays primitives (collections of line segments).
 * 
 * <p>Rays are pure line geometry that support various arrangement patterns:
 * RADIAL, SPHERICAL, PARALLEL, CONVERGING, DIVERGING.</p>
 * 
 * <p>Unlike solid shapes, rays always render as lines regardless of fill mode.
 * SOLID, WIREFRAME, and CAGE all render the same line geometry.</p>
 * 
 * <h3>Supported Features</h3>
 * <ul>
 *   <li>Per-vertex coloring (MESH_GRADIENT, MESH_RAINBOW, RANDOM)</li>
 *   <li>Wave animation</li>
 *   <li>All arrangement modes</li>
 *   <li>Segmented/dashed lines</li>
 *   <li>fadeStart/fadeEnd - per-ray alpha gradient</li>
 *   <li>Ray Flow animation (Length, Travel, Flicker)</li>
 *   <li>Ray Motion animation (Linear, Oscillate, Spiral, Ripple)</li>
 * </ul>
 * 
 * @see RaysShape
 * @see RaysTessellator
 * @see RayFlowConfig
 * @see RayMotionConfig
 */
public final class RaysRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "rays";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof RaysShape shape)) {
            return null;
        }
        
        Logging.RENDER.topic("tessellate").debug(
            "[RAYS] Tessellating: count={}, arrangement={}, layers={}, wave={}",
            shape.count(), shape.arrangement(), shape.layers(), wave != null && wave.isActive());
        
        return RaysTessellator.tessellate(shape, wave, time);
    }
    
    /**
     * Override render to handle rays specially with fadeStart/fadeEnd support
     * and ray-specific animations (RayFlowConfig, RayMotionConfig).
     */
    @Override
    public void render(
            Primitive primitive,
            MatrixStack matrices,
            VertexConsumer consumer,
            int light,
            float time,
            ColorResolver resolver,
            RenderOverrides overrides) {
        
        if (!(primitive.shape() instanceof RaysShape shape)) {
            return;
        }
        
        Logging.FIELD.topic("render").trace("[RAYS] Rendering: count={}, arrangement={}", 
            shape.count(), shape.arrangement());
        
        // === Tessellate ===
        WaveConfig wave = primitive.animation() != null ? primitive.animation().wave() : null;
        Mesh mesh = tessellate(primitive, wave, time);
        if (mesh == null || mesh.isEmpty()) {
            return;
        }
        
        // === Resolve Color ===
        int color = resolveColor(primitive, resolver, overrides, time);
        
        // Apply alpha from appearance
        Appearance app = primitive.appearance();
        float baseAlpha = 1.0f;
        if (app != null && app.alpha() != null) {
            baseAlpha = app.alpha().max();
        }
        
        // === Get Animation Configs ===
        Animation anim = primitive.animation();
        WaveConfig waveConfig = anim != null ? anim.wave() : null;
        RayFlowConfig flowConfig = anim != null ? anim.rayFlow() : null;
        RayMotionConfig motionConfig = anim != null ? anim.rayMotion() : null;
        RayWiggleConfig wiggleConfig = anim != null ? anim.rayWiggle() : null;
        RayTwistConfig twistConfig = anim != null ? anim.rayTwist() : null;
        
        // === Create ColorContext for per-vertex coloring ===
        ColorContext colorCtx = null;
        if (app != null && app.isPerVertex()) {
            int primaryColor = color;
            int secondaryColor = color;
            
            String primaryRef = app.color();
            if (primaryRef != null && resolver != null) {
                primaryColor = resolver.resolve(primaryRef);
            }
            
            String secondaryRef = app.secondaryColor();
            if (secondaryRef != null && resolver != null) {
                secondaryColor = resolver.resolve(secondaryRef);
            } else if (secondaryRef != null && secondaryRef.startsWith("#")) {
                try {
                    secondaryColor = 0xFF000000 | Integer.parseInt(secondaryRef.substring(1), 16);
                } catch (NumberFormatException ignored) {}
            } else {
                secondaryColor = primaryColor;
            }
            
            colorCtx = ColorContext.from(app, primaryColor, secondaryColor, time, 
                shape.outerRadius(), shape.outerRadius());
        }
        
        // === Emit Lines with Fade + Animation Support ===
        float fadeStart = shape.fadeStart();
        float fadeEnd = shape.fadeEnd();
        boolean hasFade = fadeStart != 1.0f || fadeEnd != 1.0f;
        
        emitRayLines(matrices, consumer, mesh, color, baseAlpha, light, 
                     colorCtx, waveConfig, time, fadeStart, fadeEnd, hasFade,
                     flowConfig, motionConfig, wiggleConfig, twistConfig);
        
        Logging.FIELD.topic("render").trace("[RAYS] DONE: {} vertices, fade={}, flow={}, motion={}, wiggle={}, twist={}", 
            mesh.vertexCount(), hasFade, flowConfig != null, motionConfig != null, 
            wiggleConfig != null, twistConfig != null);
    }
    
    /**
     * Emits ray line vertices with fade and animation support.
     * 
     * <p>Each vertex has a 't' value stored in UV.u (0 at ray start, 1 at ray end).
     * Alpha is modulated by: fade gradient, flow animation (Length, Travel, Flicker).</p>
     */
    private void emitRayLines(MatrixStack matrices, VertexConsumer consumer,
                               Mesh mesh, int baseColor, float baseAlpha, int light,
                               ColorContext colorCtx, WaveConfig waveConfig, float time,
                               float fadeStart, float fadeEnd, boolean hasFade,
                               RayFlowConfig flowConfig, RayMotionConfig motionConfig,
                               RayWiggleConfig wiggleConfig, RayTwistConfig twistConfig) {
        
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();
        
        // Pre-calculate flow animation phase (0-1)
        final float baseLengthPhase = calculateLengthPhase(flowConfig, time);
        final float travelOffset = calculateTravelOffset(flowConfig, time);
        
        // Get ray count for per-ray phase staggering (creates continuous flow effect)
        final int rayCount = Math.max(1, mesh.primitiveCount());
        final boolean staggerPhase = flowConfig != null && 
            (flowConfig.length() == LengthMode.RADIATE || flowConfig.length() == LengthMode.ABSORB);
        
        // Wave configuration for RADIATE/ABSORB
        final float waveArc = flowConfig != null ? flowConfig.effectiveWaveArc() : 1.0f;
        final WaveDistribution waveDist = flowConfig != null ? flowConfig.effectiveWaveDistribution() : WaveDistribution.SEQUENTIAL;
        final float sweepCopies = flowConfig != null ? Math.max(0.1f, flowConfig.waveCount()) : 1.0f;
        
        // Pre-compute random offsets for RANDOM distribution (seeded by ray index)
        // This ensures consistent randomization per ray across frames
        
        // For each line segment (pairs of vertices: v0=start/center, v1=end/outer)
        final int[] rayIndex = {0};
        mesh.forEachLine((v0, v1) -> {
            int idx = rayIndex[0]++;
            
            // Get base positions
            float x0 = v0.x(), y0 = v0.y(), z0 = v0.z();
            float x1 = v1.x(), y1 = v1.y(), z1 = v1.z();
            
            // Get t values for each vertex (parametric position along ray 0-1)
            float t0 = v0.u();
            float t1 = v1.u();
            
            // Calculate per-ray phase for RADIATE/ABSORB (staggered for continuous flow)
            float rayLengthPhase;
            if (staggerPhase) {
                // Calculate ray phase offset based on distribution mode
                float rayOffset;
                if (waveDist == WaveDistribution.RANDOM) {
                    // Use a hash function to get consistent random offset per ray
                    // Golden ratio-based hash for good distribution
                    rayOffset = (idx * 0.618033988749895f) % 1.0f;
                } else {
                    // SEQUENTIAL: evenly distributed by index
                    rayOffset = (float) idx / rayCount;
                }
                
                // Apply wave scale (compression/frequency)
                float scaledOffset = rayOffset * waveArc;
                
                // Apply sweep copies - trim below 1, duplicate above 1
                // For TRIM (< 1): compress the ray distribution
                // For DUPLICATE (> 1): we handle this below by checking multiple windows
                if (sweepCopies < 1.0f) {
                    // Trim mode: compress visible region
                    // sweepCopies=0.5 means rays are spread over only half the circle
                    scaledOffset = rayOffset * sweepCopies * waveArc;
                }
                
                float phase = (baseLengthPhase + scaledOffset) % 1.0f;
                rayLengthPhase = phase;
            } else {
                rayLengthPhase = baseLengthPhase;
            }
            
            // === Handle LengthMode animations ===
            // All modes use t-based geometry clipping for proper continuous flow
            if (flowConfig != null && flowConfig.hasLength()) {
                LengthMode lengthMode = flowConfig.length();
                
                // Default: full ray visible
                float windowStart = 0f;
                float windowEnd = 1f;
                
                // Segment length controls how much of the ray is visible at once
                // For SEGMENT mode, use the configured value
                // For RADIATE/ABSORB, use full ray length but POSITION slides
                float segmentLength = flowConfig.segmentLength();
                if (segmentLength <= 0 || segmentLength > 1.0f) {
                    segmentLength = 1.0f; // Default: full ray visible
                }
                
                switch (lengthMode) {
                    case RADIATE -> {
                        // Ray flows OUTWARD: visible window starts at inner, moves toward outer
                        float travelRange = 1.0f + segmentLength;
                        float windowCenter = rayLengthPhase * travelRange;
                        windowStart = windowCenter - segmentLength * 0.5f;
                        windowEnd = windowCenter + segmentLength * 0.5f;
                        
                        // SWEEP DUPLICATION: create multiple visibility windows
                        // sweepCopies=2 means 2 full-size windows 180° apart
                        // sweepCopies=3 means 3 full-size windows 120° apart
                        if (sweepCopies > 1.0f) {
                            int copies = (int) Math.ceil(sweepCopies);
                            // Check if ray falls in ANY of the duplicated windows
                            // We iterate through each copy position and check visibility
                            boolean inAnyWindow = false;
                            for (int c = 0; c < copies; c++) {
                                float copyOffset = (float) c / copies;
                                float copyPhase = (rayLengthPhase + copyOffset) % 1.0f;
                                float copyCenter = copyPhase * travelRange;
                                float copyStart = copyCenter - segmentLength * 0.5f;
                                float copyEnd = copyCenter + segmentLength * 0.5f;
                                // Check if ANY point of the ray (t from 0 to 1) is in this window
                                if (copyEnd >= 0 && copyStart <= 1) {
                                    inAnyWindow = true;
                                    // Use the best matching window
                                    if (copyStart <= t0 && t1 <= copyEnd) {
                                        windowStart = copyStart;
                                        windowEnd = copyEnd;
                                        break;
                                    }
                                }
                            }
                            if (!inAnyWindow) {
                                // Ray is not in any window - hide it
                                windowStart = -1;
                                windowEnd = -1;
                            }
                        }
                    }
                    case ABSORB -> {
                        // Ray flows INWARD: visible window starts at outer, moves toward inner
                        float travelRange = 1.0f + segmentLength;
                        float windowCenter = 1.0f - (rayLengthPhase * travelRange);
                        windowStart = windowCenter - segmentLength * 0.5f;
                        windowEnd = windowCenter + segmentLength * 0.5f;
                        
                        // SWEEP DUPLICATION: create multiple visibility windows
                        if (sweepCopies > 1.0f) {
                            int copies = (int) Math.ceil(sweepCopies);
                            boolean inAnyWindow = false;
                            for (int c = 0; c < copies; c++) {
                                float copyOffset = (float) c / copies;
                                float copyPhase = (rayLengthPhase + copyOffset) % 1.0f;
                                float copyCenter = 1.0f - (copyPhase * travelRange);
                                float copyStart = copyCenter - segmentLength * 0.5f;
                                float copyEnd = copyCenter + segmentLength * 0.5f;
                                if (copyEnd >= 0 && copyStart <= 1) {
                                    inAnyWindow = true;
                                    if (copyStart <= t0 && t1 <= copyEnd) {
                                        windowStart = copyStart;
                                        windowEnd = copyEnd;
                                        break;
                                    }
                                }
                            }
                            if (!inAnyWindow) {
                                windowStart = -1;
                                windowEnd = -1;
                            }
                        }
                    }
                    case SEGMENT -> {
                        // SEGMENT: sliding window (original behavior)
                        segmentLength = Math.min(1.0f, Math.max(0.1f, flowConfig.segmentLength()));
                        windowStart = Math.max(0, rayLengthPhase - segmentLength * 0.5f);
                        windowEnd = Math.min(1, rayLengthPhase + segmentLength * 0.5f);
                    }
                    case GROW_SHRINK -> {
                        // GROW_SHRINK: ray grows from t=0, then shrinks back
                        float effectivePhase;
                        boolean growing = rayLengthPhase < 0.5f;
                        if (growing) {
                            effectivePhase = rayLengthPhase * 2f; // 0→1
                        } else {
                            effectivePhase = 2f - rayLengthPhase * 2f; // 1→0
                        }
                        windowStart = 0f;
                        windowEnd = effectivePhase;
                    }
                    case PULSE -> {
                        // PULSE is handled by applyLengthFlow (scaling)
                        // Don't use clipping, use the old geometry modification
                        float[] animated = applyLengthFlow(flowConfig, x0, y0, z0, x1, y1, z1, rayLengthPhase);
                        x0 = animated[0]; y0 = animated[1]; z0 = animated[2];
                        x1 = animated[3]; y1 = animated[4]; z1 = animated[5];
                        windowStart = 0f;
                        windowEnd = 1f; // Don't clip
                    }
                    default -> {
                        // NONE or unknown - show full ray
                        windowStart = 0f;
                        windowEnd = 1f;
                    }
                }
                
                // Apply clipping (for modes that use it)
                if (lengthMode != LengthMode.PULSE && lengthMode != LengthMode.NONE) {
                    // Check if segment is outside the visible window
                    if (t1 <= windowStart || t0 >= windowEnd) {
                        return; // Skip this segment entirely
                    }
                    
                    // Clip segment to visible window if partially visible
                    if (t0 < windowStart && windowStart < t1) {
                        // Interpolate start position to window boundary
                        float clipT = (windowStart - t0) / (t1 - t0);
                        x0 = x0 + (x1 - x0) * clipT;
                        y0 = y0 + (y1 - y0) * clipT;
                        z0 = z0 + (z1 - z0) * clipT;
                        t0 = windowStart;
                    }
                    if (t1 > windowEnd && windowEnd > t0) {
                        // Interpolate end position to window boundary
                        float clipT = (windowEnd - t0) / (t1 - t0);
                        x1 = x0 + (x1 - x0) * clipT;
                        y1 = y0 + (y1 - y0) * clipT;
                        z1 = z0 + (z1 - z0) * clipT;
                        t1 = windowEnd;
                    }
                }
            }
            
            // === Apply Motion Animation (geometry transform) ===
            if (motionConfig != null && motionConfig.isActive()) {
                float[] m0 = applyMotion(motionConfig, x0, y0, z0, time, idx);
                float[] m1 = applyMotion(motionConfig, x1, y1, z1, time, idx);
                x0 = m0[0]; y0 = m0[1]; z0 = m0[2];
                x1 = m1[0]; y1 = m1[1]; z1 = m1[2];
            }
            
            // === Apply Wiggle Animation (per-vertex deformation) ===
            // Wiggle deforms the ray shape itself - requires t parameter
            if (wiggleConfig != null && wiggleConfig.isActive()) {
                // Compute ray direction for perpendicular displacement
                float dx = x1 - x0;
                float dy = y1 - y0;
                float dz = z1 - z0;
                float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len > 0.0001f) {
                    dx /= len; dy /= len; dz /= len;
                    
                    float[] w0 = applyWiggle(wiggleConfig, x0, y0, z0, dx, dy, dz, t0, time, idx);
                    float[] w1 = applyWiggle(wiggleConfig, x1, y1, z1, dx, dy, dz, t1, time, idx);
                    x0 = w0[0]; y0 = w0[1]; z0 = w0[2];
                    x1 = w1[0]; y1 = w1[1]; z1 = w1[2];
                }
            }
            
            // === Apply Twist Animation (axial rotation) ===
            // Twist rotates vertices around the ray's CENTRAL axis (stored in normal)
            // Not the shaped segment direction!
            if (twistConfig != null && twistConfig.isActive()) {
                // The ray's central axis direction is stored in the vertex normal
                float dx = v0.nx();
                float dy = v0.ny();
                float dz = v0.nz();
                float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len > 0.0001f) {
                    dx /= len; dy /= len; dz /= len;
                    
                    float[] tw0 = applyTwist(twistConfig, x0, y0, z0, dx, dy, dz, t0, time, idx);
                    float[] tw1 = applyTwist(twistConfig, x1, y1, z1, dx, dy, dz, t1, time, idx);
                    x0 = tw0[0]; y0 = tw0[1]; z0 = tw0[2];
                    x1 = tw1[0]; y1 = tw1[1]; z1 = tw1[2];
                }
            }
            
            // NOTE: Wave deformation is now applied at tessellation time in RaysTessellator
            // This ensures proper multi-segment tessellation for smooth wave effects
            
            // Emit both vertices of the line
            emitLineVertex(consumer, x0, y0, z0, x1, y1, z1, positionMatrix, normalMatrix,
                          baseColor, baseAlpha, light, colorCtx, v0, flowConfig, time, idx, 
                          travelOffset, rayLengthPhase, fadeStart, fadeEnd, hasFade);
            emitLineVertex(consumer, x1, y1, z1, x0, y0, z0, positionMatrix, normalMatrix,
                          baseColor, baseAlpha, light, colorCtx, v1, flowConfig, time, idx, 
                          travelOffset, rayLengthPhase, fadeStart, fadeEnd, hasFade);
        });
    }
    
    /**
     * Applies Length flow animation by modifying ray geometry.
     * 
     * <p>Only PULSE mode uses this - other modes use t-based clipping.</p>
     * 
     * <p>Returns [x0, y0, z0, x1, y1, z1] - the animated start and end positions.</p>
     */
    private float[] applyLengthFlow(RayFlowConfig flow, 
                                     float x0, float y0, float z0,
                                     float x1, float y1, float z1,
                                     float phase) {
        if (flow.length() == LengthMode.PULSE) {
            // Ray breathes: both endpoints scale toward/away from center
            // phase oscillates (0.2 to 1.0) for breathing effect
            float centerX = (x0 + x1) * 0.5f;
            float centerY = (y0 + y1) * 0.5f;
            float centerZ = (z0 + z1) * 0.5f;
            // Scale both endpoints relative to center
            float newX0 = centerX + (x0 - centerX) * phase;
            float newY0 = centerY + (y0 - centerY) * phase;
            float newZ0 = centerZ + (z0 - centerZ) * phase;
            float newX1 = centerX + (x1 - centerX) * phase;
            float newY1 = centerY + (y1 - centerY) * phase;
            float newZ1 = centerZ + (z1 - centerZ) * phase;
            return new float[]{newX0, newY0, newZ0, newX1, newY1, newZ1};
        }
        // All other modes use t-based clipping in the render loop
        return new float[]{x0, y0, z0, x1, y1, z1};
    }
    
    /**
     * Emits a single line vertex with color, fade, and all flow animations.
     */
    private void emitLineVertex(VertexConsumer consumer, 
                                 float x, float y, float z,
                                 float otherX, float otherY, float otherZ,
                                 Matrix4f positionMatrix, Matrix3f normalMatrix,
                                 int baseColor, float baseAlpha, int light,
                                 ColorContext colorCtx, Vertex originalVertex,
                                 RayFlowConfig flowConfig, float time, int rayIndex, 
                                 float travelOffset, float lengthPhase,
                                 float fadeStart, float fadeEnd, boolean hasFade) {
        
        // Transform position
        Vector4f pos = new Vector4f(x, y, z, 1.0f);
        pos.mul(positionMatrix);
        
        // Normal is direction to other vertex
        Vector3f dir = new Vector3f(otherX - x, otherY - y, otherZ - z);
        if (dir.lengthSquared() > 0.0001f) {
            dir.normalize();
        } else {
            dir.set(0, 1, 0);
        }
        dir.mul(normalMatrix);
        
        // Calculate color
        int vertexColor;
        if (colorCtx != null && colorCtx.isPerVertex()) {
            // Pass rayIndex as cellIndex so each ray can get a different color
            vertexColor = colorCtx.calculateColor(originalVertex.x(), originalVertex.y(), originalVertex.z(), rayIndex);
        } else {
            vertexColor = baseColor;
        }
        
        // Calculate alpha with all modulations
        float t = originalVertex.u(); // Parametric position along original ray (0-1)
        float alpha = baseAlpha;
        
        // 1. Apply base fade gradient (fadeStart at t=0, fadeEnd at t=1)
        if (hasFade) {
            float fadeFactor = fadeStart + (fadeEnd - fadeStart) * t;
            alpha *= fadeFactor;
        }
        
        // NOTE: LengthMode (RADIATE/ABSORB/PULSE/SEGMENT/GROW_SHRINK) is GEOMETRY ONLY
        // It is handled by applyLengthFlow() which modifies vertex positions.
        // Alpha is controlled by: fadeStart/fadeEnd, TravelMode, and FlickerMode.
        
        // 3. Apply Travel animation (CHASE/SCROLL/COMET/SPARK/PULSE_WAVE/REVERSE_CHASE)
        alpha *= calculateTravelAlpha(flowConfig, t, travelOffset, rayIndex);
        
        // 4. Apply Flicker animation (SCINTILLATION/STROBE/FADE_PULSE/LIGHTNING/HEARTBEAT)
        alpha *= calculateFlickerAlpha(flowConfig, time, rayIndex);
        
        // Apply alpha to color
        int a = (int)(alpha * 255) & 0xFF;
        int r = (vertexColor >> 16) & 0xFF;
        int g = (vertexColor >> 8) & 0xFF;
        int b = vertexColor & 0xFF;
        
        consumer.vertex(pos.x(), pos.y(), pos.z())
            .color(r, g, b, a)
            .normal(dir.x(), dir.y(), dir.z());
    }
    
    /**
     * Calculates the current length animation phase (0-1).
     * Used by RADIATE, ABSORB, and PULSE modes.
     * 
     * <p>The phase controls what portion of the ray is visible:</p>
     * <ul>
     *   <li>RADIATE: phase=0.3 means first 30% of ray is visible (growing outward)</li>
     *   <li>ABSORB: phase=0.3 means last 30% of ray is visible (shrinking inward)</li>
     *   <li>PULSE: phase oscillates, creating breathing effect</li>
     * </ul>
     */
    private float calculateLengthPhase(RayFlowConfig flow, float time) {
        if (flow == null || !flow.hasLength()) return 1f;
        
        float speed = flow.lengthSpeed();
        // One full cycle per ~20 seconds at speed=1 (slower base rate)
        // speed=0.01 → ~2000 seconds per cycle (very slow)
        // speed=1.0 → ~20 seconds per cycle
        float phase = (time * speed * 0.05f) % 1.0f;
        if (phase < 0) phase += 1.0f;
        
        return switch (flow.length()) {
            // RADIATE and ABSORB both use sawtooth phase (0→1 repeating)
            // The DIRECTION of motion is controlled in applyLengthFlow
            case RADIATE, ABSORB -> phase;
            case PULSE -> {
                // Oscillate between 0.2 and 1.0 (never fully invisible)
                float sine = (float)Math.sin(time * speed * 0.1f * Math.PI);
                yield 0.6f + 0.4f * sine;
            }
            case SEGMENT -> phase; // 0→1 slides the segment position
            case GROW_SHRINK -> phase; // 0→1 for full grow-shrink cycle
            default -> 1f;
        };
    }
    
    /**
     * Calculates the travel animation offset for the visibility window.
     * Used by CHASE and SCROLL modes.
     */
    private float calculateTravelOffset(RayFlowConfig flow, float time) {
        if (flow == null || !flow.hasTravel()) return 0f;
        
        float speed = Math.max(0.1f, flow.travelSpeed());
        float offset = (time * speed * 0.3f) % 1.0f;
        if (offset < 0) offset += 1.0f;
        
        return offset;
    }
    
    // NOTE: There is NO calculateLengthAlpha function.
    // LengthMode (RADIATE/ABSORB/PULSE/SEGMENT/GROW_SHRINK) is GEOMETRY ONLY.
    // It is handled by applyLengthFlow() which modifies vertex positions.
    // Alpha is controlled by: TravelMode (calculateTravelAlpha) and FlickerMode (calculateFlickerAlpha).
    /**
     * Calculates alpha multiplier for Travel animation.
     * Controls how brightness moves along the ray over time.
     */
    private float calculateTravelAlpha(RayFlowConfig flow, float t, float travelOffset, int rayIndex) {
        if (flow == null || !flow.hasTravel()) return 1f;
        
        return switch (flow.travel()) {
            case CHASE -> {
                int count = Math.max(1, flow.chaseCount());
                float width = Math.max(0.05f, flow.chaseWidth());
                float spacing = 1f / count;
                
                // Check each "particle" window
                for (int i = 0; i < count; i++) {
                    float center = (travelOffset + i * spacing) % 1f;
                    float dist = Math.min(Math.abs(t - center), Math.min(
                        Math.abs(t - center - 1f), Math.abs(t - center + 1f)));
                    if (dist <= width / 2f) {
                        // Smooth falloff at edges
                        float falloff = 1f - (dist / (width / 2f));
                        yield falloff * falloff; // Quadratic falloff
                    }
                }
                yield 0f;
            }
            case SCROLL -> {
                // Offset t and create a smooth gradient
                float scrolledT = (t + travelOffset) % 1f;
                // Create a triangular wave: bright in center, dark at edges
                yield 1f - Math.abs(scrolledT - 0.5f) * 2f;
            }
            case COMET -> {
                // Bright head at front, fading tail behind
                // travelOffset is the head position (0-1 along ray)
                float headPos = travelOffset;
                float tailLength = Math.max(0.1f, flow.chaseWidth());
                float tailStart = headPos - tailLength;
                
                // Calculate distance behind head (with wrap handling)
                float distBehind;
                if (t > headPos) {
                    // Ahead of comet - check if we're in the wrapped tail
                    if (tailStart < 0) {
                        // Tail wraps around: check if t is in [tailStart+1, 1]
                        float wrappedTailStart = tailStart + 1f;
                        if (t >= wrappedTailStart) {
                            distBehind = (1f - t) + headPos; // Distance wrapping around
                        } else {
                            yield 0f; // Truly ahead, not in wrapped tail
                        }
                    } else {
                        yield 0f; // No wrap, truly ahead
                    }
                } else if (t >= tailStart || tailStart < 0) {
                    // In the normal tail section
                    distBehind = headPos - t;
                } else {
                    yield 0f; // Behind tail
                }
                
                // Apply quadratic falloff  
                if (distBehind >= 0 && distBehind <= tailLength) {
                    float tailAlpha = 1f - (distBehind / tailLength);
                    yield tailAlpha * tailAlpha;
                } else {
                    yield 0f;
                }
            }
            case SPARK -> {
                // Random bright flashes at different positions per ray
                int sparkCount = Math.max(1, flow.chaseCount());
                float sparkWidth = Math.max(0.05f, flow.chaseWidth());
                
                // Per-ray random spark positions and timing
                float maxAlpha = 0f;
                for (int i = 0; i < sparkCount; i++) {
                    // Use rayIndex for per-ray variation
                    float sparkPhase = hash(rayIndex * 7919 + i, (int)(travelOffset * 10));
                    float sparkPos = hash(rayIndex * 7919 + i + 1, (int)(travelOffset * 3));
                    
                    // Random on/off for each spark
                    if (sparkPhase > 0.5f) {
                        float dist = Math.abs(t - sparkPos);
                        if (dist < sparkWidth) {
                            float spark = 1f - (dist / sparkWidth);
                            maxAlpha = Math.max(maxAlpha, spark * spark);
                        }
                    }
                }
                yield maxAlpha;
            }
            case PULSE_WAVE -> {
                // Traveling wave of brightness (like heartbeat pulse along ray)
                float waveWidth = Math.max(0.1f, flow.chaseWidth());
                int waveCount = Math.max(1, flow.chaseCount());
                
                float maxAlpha = 0f;
                for (int i = 0; i < waveCount; i++) {
                    float waveCenter = (travelOffset + (float)i / waveCount) % 1f;
                    
                    // Wrap-aware distance
                    float dist = Math.min(Math.abs(t - waveCenter), 
                                 Math.min(Math.abs(t - waveCenter - 1f), 
                                          Math.abs(t - waveCenter + 1f)));
                    
                    if (dist < waveWidth) {
                        // Gaussian-like pulse shape
                        float normalized = dist / waveWidth;
                        float pulse = (float) Math.exp(-normalized * normalized * 4);
                        maxAlpha = Math.max(maxAlpha, pulse);
                    }
                }
                yield maxAlpha;
            }
            case REVERSE_CHASE -> {
                // Same as CHASE but moves inward (from tip to base)
                int count = Math.max(1, flow.chaseCount());
                float width = Math.max(0.05f, flow.chaseWidth());
                float spacing = 1f / count;
                
                // Travel offset is inverted (1 - offset) for reverse direction
                float reverseOffset = 1f - travelOffset;
                
                for (int i = 0; i < count; i++) {
                    float center = (reverseOffset + i * spacing) % 1f;
                    float dist = Math.min(Math.abs(t - center), Math.min(
                        Math.abs(t - center - 1f), Math.abs(t - center + 1f)));
                    if (dist <= width / 2f) {
                        float falloff = 1f - (dist / (width / 2f));
                        yield falloff * falloff;
                    }
                }
                yield 0f;
            }
            default -> 1f;
        };
    }
    
    /**
     * Calculates alpha multiplier for Flicker animation.
     * Controls random or rhythmic brightness variations.
     */
    private float calculateFlickerAlpha(RayFlowConfig flow, float time, int rayIndex) {
        if (flow == null || !flow.hasFlicker()) return 1f;
        
        float intensity = Math.max(0.1f, flow.flickerIntensity());
        float freq = Math.max(0.5f, flow.flickerFrequency());
        
        return switch (flow.flicker()) {
            case SCINTILLATION -> {
                // Per-ray random flicker using hash (star-like twinkling)
                float hash = hash(rayIndex, (int)(time * freq));
                float flicker = 0.5f + 0.5f * hash; // 0.5 to 1.0 range
                yield 1f - intensity * (1f - flicker);
            }
            case STROBE -> {
                // Synchronized on/off (hard edges)
                float wave = (float)Math.sin(time * freq * Math.PI * 2);
                yield wave > 0 ? 1f : (1f - intensity);
            }
            case FADE_PULSE -> {
                // Smooth sine wave fading (breathing effect)
                float wave = (float)Math.sin(time * freq * Math.PI * 2);
                // Map -1 to 1 range to (1-intensity) to 1
                float alpha = 0.5f + 0.5f * wave; // 0 to 1
                yield (1f - intensity) + intensity * alpha;
            }
            case FLICKER -> {
                // Irregular noise-based flicker (like a dying light bulb)
                // Combine multiple hash samples for more organic feel
                float h1 = hash(rayIndex, (int)(time * freq));
                float h2 = hash(rayIndex + 1000, (int)(time * freq * 0.7f));
                float h3 = hash(rayIndex + 2000, (int)(time * freq * 1.3f));
                float noise = (h1 + h2 * 0.5f + h3 * 0.25f) / 1.75f;
                yield (1f - intensity) + intensity * noise;
            }
            case LIGHTNING -> {
                // Spike with exponential decay (electric flash)
                // Create periodic spikes with decay trails
                float cycleTime = (time * freq) % 1.0f;
                
                // Random spike timing per ray
                float spikePoint = hash(rayIndex, (int)(time * freq / 2)) * 0.3f;
                
                if (cycleTime < spikePoint + 0.05f && cycleTime >= spikePoint) {
                    // Sharp spike: full brightness
                    yield 1f;
                } else if (cycleTime > spikePoint + 0.05f && cycleTime < spikePoint + 0.3f) {
                    // Exponential decay after spike
                    float elapsed = cycleTime - spikePoint - 0.05f;
                    float decay = (float) Math.exp(-elapsed * 15);
                    yield (1f - intensity) + intensity * decay;
                } else {
                    // Mostly dark between spikes
                    yield 1f - intensity * 0.8f;
                }
            }
            case HEARTBEAT -> {
                // Double-pulse pattern (like a heartbeat: lub-dub)
                float cycleTime = (time * freq * 0.5f) % 1.0f; // Slower cycle
                
                // First beat at 0.0-0.15 (lub)
                // Second beat at 0.2-0.35 (dub)
                // Rest from 0.35-1.0
                
                float pulse = 0f;
                if (cycleTime < 0.15f) {
                    // First pulse (lub) - stronger
                    float t = cycleTime / 0.15f;
                    pulse = (float) Math.sin(t * Math.PI);
                } else if (cycleTime >= 0.2f && cycleTime < 0.35f) {
                    // Second pulse (dub) - slightly weaker
                    float t = (cycleTime - 0.2f) / 0.15f;
                    pulse = 0.7f * (float) Math.sin(t * Math.PI);
                }
                
                yield (1f - intensity) + intensity * pulse;
            }
            default -> 1f;
        };
    }
    
    /**
     * Simple hash function for deterministic pseudo-random values.
     */
    private float hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        return (h & 0x7FFFFFFF) / (float)0x7FFFFFFF;
    }
    
    // =========================================================================
    // Motion Animation
    // =========================================================================
    
    /**
     * Applies geometry motion to vertex position.
     * 
     * <p>Motion modes transform ray geometry in world space:</p>
     * <ul>
     *   <li>RADIAL_DRIFT - Rays move outward continuously</li>
     *   <li>RADIAL_OSCILLATE - Rays oscillate in/out radially</li>
     *   <li>ANGULAR_OSCILLATE - Rays sway side-to-side angularly</li>
     *   <li>ANGULAR_DRIFT - Rays slowly rotate around center</li>
     *   <li>ORBIT - Rays revolve around center axis</li>
     *   <li>FLOAT - Rays bob up and down</li>
     *   <li>SWAY - Ray tips wave while base stays fixed</li>
     *   <li>JITTER - Random position noise</li>
     *   <li>PRECESS - Ray axis traces a cone pattern</li>
     *   <li>RIPPLE - Radial wave pulses outward</li>
     * </ul>
     */
    private float[] applyMotion(RayMotionConfig motion, float x, float y, float z, float time, int rayIndex) {
        if (motion == null || !motion.isActive()) {
            return new float[]{x, y, z};
        }
        
        float speed = Math.max(0.1f, motion.speed());
        float amp = Math.max(0.1f, motion.amplitude());
        float freq = Math.max(0.5f, motion.frequency());
        
        return switch (motion.mode()) {
            case RADIAL_DRIFT -> {
                // Ray slides outward along radial direction
                float dist = (float)Math.sqrt(x * x + z * z);
                if (dist < 0.001f) {
                    yield new float[]{x, y, z};
                }
                float nx = x / dist;
                float nz = z / dist;
                float offset = (time * speed * 0.5f) % 1.0f;
                float displacement = offset * amp * 2.0f;
                yield new float[]{x + nx * displacement, y, z + nz * displacement};
            }
            case RADIAL_OSCILLATE -> {
                // Ray oscillates in/out radially (sine wave)
                float dist = (float)Math.sqrt(x * x + z * z);
                if (dist < 0.001f) {
                    yield new float[]{x, y, z};
                }
                float nx = x / dist;
                float nz = z / dist;
                float wave = (float)Math.sin(time * speed * Math.PI) * amp;
                yield new float[]{x + nx * wave, y, z + nz * wave};
            }
            case ANGULAR_OSCILLATE -> {
                // Ray sways side-to-side angularly around center
                float dist = (float)Math.sqrt(x * x + z * z);
                if (dist < 0.001f) {
                    yield new float[]{x, y, z};
                }
                // Rotate by oscillating angle
                float angle = (float)Math.sin(time * speed * Math.PI) * amp * 0.5f;
                float cos = (float)Math.cos(angle);
                float sin = (float)Math.sin(angle);
                yield new float[]{x * cos - z * sin, y, x * sin + z * cos};
            }
            case ANGULAR_DRIFT -> {
                // Ray slowly rotates around center (constant angular velocity)
                float angle = time * speed * 0.5f;
                float cos = (float)Math.cos(angle);
                float sin = (float)Math.sin(angle);
                yield new float[]{x * cos - z * sin, y, x * sin + z * cos};
            }
            case ORBIT -> {
                // Ray revolves around center - position moves in circle
                float dist = (float)Math.sqrt(x * x + z * z);
                float spiralAngle = dist * freq * 2.0f + time * speed * 0.5f;
                float cos = (float)Math.cos(spiralAngle);
                float sin = (float)Math.sin(spiralAngle);
                yield new float[]{x * cos - z * sin, y, x * sin + z * cos};
            }
            case FLOAT -> {
                // Ray bobs up and down on Y axis
                float wave = (float)Math.sin(time * speed * Math.PI + rayIndex * 0.5f) * amp;
                yield new float[]{x, y + wave, z};
            }
            case SWAY -> {
                // Ray tip waves while base stays fixed (t-dependent rotation)
                // This works best with multi-segment rays where t varies
                // For now, apply a simple pendulum effect based on distance from center
                float dist = (float)Math.sqrt(x * x + z * z);
                if (dist < 0.001f) {
                    yield new float[]{x, y, z}; // Base doesn't move
                }
                // Farther points sway more (like a pendulum tip)
                float swayAmount = dist * 0.3f; // Scale factor
                float angle = (float)Math.sin(time * speed * Math.PI) * amp * swayAmount;
                float cos = (float)Math.cos(angle);
                float sin = (float)Math.sin(angle);
                yield new float[]{x * cos - z * sin, y, x * sin + z * cos};
            }
            case JITTER -> {
                // Random position noise (jittery, unstable)
                float hx = hash(rayIndex, (int)(time * speed * 10)) * 2 - 1;
                float hy = hash(rayIndex + 1000, (int)(time * speed * 10)) * 2 - 1;
                float hz = hash(rayIndex + 2000, (int)(time * speed * 10)) * 2 - 1;
                yield new float[]{
                    x + hx * amp * 0.3f,
                    y + hy * amp * 0.3f,
                    z + hz * amp * 0.3f
                };
            }
            case PRECESS -> {
                // Ray axis traces a cone pattern (gyroscopic wobble)
                // Each ray tilts toward a rotating point, creating a wobbling star effect
                float dist = (float)Math.sqrt(x * x + z * z);
                if (dist < 0.001f) {
                    yield new float[]{x, y, z}; // Center doesn't move
                }
                
                // Get the ray's current angle from center  
                float rayAngle = (float)Math.atan2(z, x);
                
                // Precession: the tilt direction rotates around the center over time
                float precAngle = time * speed;
                
                // Only tilt rays that are roughly aligned with the precession direction
                // This creates a "wave" of tilting that sweeps around the field
                float angleDiff = rayAngle - precAngle;
                // Normalize to -PI to PI
                while (angleDiff > Math.PI) angleDiff -= TWO_PI;
                while (angleDiff < -Math.PI) angleDiff += TWO_PI;
                
                // Rays aligned with precession direction tilt outward, opposite tilt inward
                float tiltFactor = (float)Math.cos(angleDiff);
                float displacement = tiltFactor * amp * 1.0f;
                
                // Apply displacement radially
                float nx = x / dist;
                float nz = z / dist;
                yield new float[]{
                    x + nx * displacement,
                    y,
                    z + nz * displacement
                };
            }
            case RIPPLE -> {
                // Radial wave pulses outward from center
                float dist = (float)Math.sqrt(x * x + z * z);
                if (dist < 0.001f) {
                    yield new float[]{x, y, z};
                }
                float phase = dist * freq - time * speed * 2.0f;
                float wave = (float)Math.sin(phase * Math.PI * 2.0f) * amp * 0.8f;
                float nx = x / dist;
                float nz = z / dist;
                yield new float[]{x + nx * wave, y, z + nz * wave};
            }
            default -> new float[]{x, y, z};
        };
    }
    
    // =========================================================================
    // Wiggle Animation
    // =========================================================================
    
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    
    /**
     * Applies wiggle animation to a vertex position.
     * 
     * <p>Wiggle deforms the ray shape by displacing vertices perpendicular to the ray direction.
     * Different modes create different wave patterns:</p>
     * <ul>
     *   <li>WIGGLE: Snake-like side-to-side motion</li>
     *   <li>WOBBLE: Ray tips back and forth around base</li>
     *   <li>WRITHE: 3D tentacle-like motion</li>
     *   <li>SHIMMER: Rapid subtle vibration</li>
     *   <li>RIPPLE: Wave travels from base to tip</li>
     *   <li>WHIP: Ray cracks like a whip</li>
     *   <li>FLUTTER: Rapid chaotic motion</li>
     *   <li>SNAKE: Fluid slithering motion</li>
     *   <li>PULSE_WAVE: Thickness pulsing along ray</li>
     * </ul>
     */
    private float[] applyWiggle(RayWiggleConfig wiggle, 
                                 float x, float y, float z,
                                 float dx, float dy, float dz,
                                 float t, float time, int rayIndex) {
        if (wiggle == null || !wiggle.isActive()) {
            return new float[]{x, y, z};
        }
        
        float speed = Math.max(0.1f, wiggle.speed());
        float amp = Math.max(0.02f, wiggle.amplitude()); // Small min for subtle effects
        float freq = Math.max(0.5f, wiggle.frequency());
        float phase = wiggle.phaseOffset();
        
        // Compute perpendicular vectors for displacement
        float[] perp = computePerpendicular(dx, dy, dz);
        float px = perp[0], py = perp[1], pz = perp[2];
        float ux = perp[3], uy = perp[4], uz = perp[5];
        
        return switch (wiggle.mode()) {
            case WIGGLE -> {
                // Snake-like: traveling sine wave in perpendicular direction
                float wave = amp * (float) Math.sin((t * freq + time * speed + phase) * TWO_PI);
                yield new float[]{
                    x + px * wave,
                    y + py * wave,
                    z + pz * wave
                };
            }
            case WOBBLE -> {
                // Tip wobbles: amplitude increases with t
                float wave = amp * t * (float) Math.sin((time * speed + phase) * TWO_PI);
                yield new float[]{
                    x + px * wave,
                    y + py * wave,
                    z + pz * wave
                };
            }
            case WRITHE -> {
                // 3D tentacle: combine two perpendicular sine waves
                float wave1 = amp * (float) Math.sin((t * freq + time * speed + phase) * TWO_PI);
                float wave2 = amp * (float) Math.cos((t * freq * 0.7f + time * speed * 1.3f) * TWO_PI);
                yield new float[]{
                    x + px * wave1 + ux * wave2,
                    y + py * wave1 + uy * wave2,
                    z + pz * wave1 + uz * wave2
                };
            }
            case SHIMMER -> {
                // High-frequency, small amplitude noise
                float hash = hash(rayIndex, (int)(t * 100 + time * speed * 50));
                float wave = amp * 0.3f * (hash * 2 - 1);
                yield new float[]{
                    x + px * wave,
                    y + py * wave,
                    z + pz * wave
                };
            }
            case RIPPLE -> {
                // Wave travels from base to tip (phase based on t)
                float wave = amp * (float) Math.sin((time * speed - t * freq + phase) * TWO_PI);
                yield new float[]{
                    x + px * wave,
                    y + py * wave,
                    z + pz * wave
                };
            }
            case WHIP -> {
                // Whip crack: sharp wave that travels and decays
                float progress = (time * speed) % 1.0f;
                float dist = Math.abs(t - progress);
                float envelope = Math.max(0, 1 - dist * 5) * t; // Stronger at tip
                float wave = amp * envelope * (float) Math.sin(dist * 10 * TWO_PI);
                yield new float[]{
                    x + px * wave,
                    y + py * wave,
                    z + pz * wave
                };
            }
            case FLUTTER -> {
                // Rapid chaotic motion - multiple hash-based noise
                float h1 = hash(rayIndex, (int)(time * speed * 30 + t * 10));
                float h2 = hash(rayIndex + 1000, (int)(time * speed * 30 + t * 10));
                float wave1 = amp * 0.5f * (h1 * 2 - 1);
                float wave2 = amp * 0.5f * (h2 * 2 - 1);
                yield new float[]{
                    x + px * wave1 + ux * wave2,
                    y + py * wave1 + uy * wave2,
                    z + pz * wave1 + uz * wave2
                };
            }
            case SNAKE -> {
                // Fluid slithering: multi-frequency blend
                float wave1 = amp * (float) Math.sin((t * freq + time * speed) * TWO_PI);
                float wave2 = amp * 0.5f * (float) Math.sin((t * freq * 2 + time * speed * 0.8f) * TWO_PI);
                float wave = wave1 + wave2;
                yield new float[]{
                    x + px * wave,
                    y + py * wave,
                    z + pz * wave
                };
            }
            case PULSE_WAVE -> {
                // Radial pulsing: expand/contract perpendicular to ray
                float pulse = amp * (float) Math.sin((t * freq - time * speed) * TWO_PI);
                // Apply in both perpendicular directions (circular expansion)
                yield new float[]{
                    x + px * pulse + ux * pulse,
                    y + py * pulse + uy * pulse,
                    z + pz * pulse + uz * pulse
                };
            }
            default -> new float[]{x, y, z};
        };
    }
    
    // =========================================================================
    // Twist Animation
    // =========================================================================
    
    /**
     * Applies twist animation - rotates vertex around the ray's local axis.
     * 
     * <p>Twist is most visible when the ray has a 3D shape (CORKSCREW, SPRING, etc.)
     * because it rotates the shape around the ray's lengthwise axis.</p>
     */
    private float[] applyTwist(RayTwistConfig twist,
                                float x, float y, float z,
                                float dx, float dy, float dz,
                                float t, float time, int rayIndex) {
        if (twist == null || !twist.isActive()) {
            return new float[]{x, y, z};
        }
        
        float speed = Math.max(0.1f, twist.speed());
        float amountRad = twist.amountRadians();
        float phaseRad = twist.phaseOffsetRadians();
        
        // Calculate the twist angle based on mode
        float angle = switch (twist.mode()) {
            case TWIST -> 
                // Continuous rotation
                (time * speed + phaseRad) * amountRad / TWO_PI;
            case OSCILLATE_TWIST -> 
                // Back and forth oscillation
                (float) Math.sin(time * speed * TWO_PI + phaseRad) * amountRad;
            case WIND_UP -> 
                // Progressive increase
                Math.min(time * speed * amountRad, amountRad * 10); // Cap at 10 full rotations
            case UNWIND -> 
                // Progressive decrease from wound state
                Math.max(amountRad * 10 - time * speed * amountRad, 0);
            case SPIRAL_TWIST -> 
                // Twist varies along length: more twist at tip
                t * (time * speed + phaseRad) * amountRad / TWO_PI;
            default -> 0f;
        };
        
        if (Math.abs(angle) < 0.0001f) {
            return new float[]{x, y, z};
        }
        
        // To rotate around the ray axis, we need to:
        // 1. Find the point on the ray axis closest to this vertex
        // 2. Compute the offset from that point
        // 3. Rotate the offset around the ray axis
        // 4. Add back to the axis point
        
        // For lines, the "center" of the ray segment is the midpoint
        // But for twist, we rotate around the axis - the position already
        // incorporates the shape offset from tessellation.
        // We need to rotate that offset around the ray direction.
        
        // The ray direction is (dx, dy, dz). We rotate the vertex position 
        // around this axis by `angle`.
        
        // Rodrigues' rotation formula: v_rot = v*cos(θ) + (k×v)*sin(θ) + k*(k·v)*(1-cos(θ))
        // where k is the axis (dx, dy, dz), v is the position
        
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float oneMinusCos = 1 - cos;
        
        // k × v (cross product)
        float kCrossX = dy * z - dz * y;
        float kCrossY = dz * x - dx * z;
        float kCrossZ = dx * y - dy * x;
        
        // k · v (dot product)
        float kDotV = dx * x + dy * y + dz * z;
        
        // Rodrigues formula
        float newX = x * cos + kCrossX * sin + dx * kDotV * oneMinusCos;
        float newY = y * cos + kCrossY * sin + dy * kDotV * oneMinusCos;
        float newZ = z * cos + kCrossZ * sin + dz * kDotV * oneMinusCos;
        
        return new float[]{newX, newY, newZ};
    }
    
    /**
     * Computes two perpendicular vectors to a direction.
     * Returns [px, py, pz, ux, uy, uz] - two perpendicular unit vectors.
     */
    private float[] computePerpendicular(float dx, float dy, float dz) {
        // Choose reference axis (avoid parallel)
        float refX, refY, refZ;
        if (Math.abs(dy) > 0.9f) {
            refX = 1; refY = 0; refZ = 0;
        } else {
            refX = 0; refY = 1; refZ = 0;
        }
        
        // p = normalize(d × ref)
        float px = dy * refZ - dz * refY;
        float py = dz * refX - dx * refZ;
        float pz = dx * refY - dy * refX;
        float plen = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (plen > 0.0001f) {
            px /= plen; py /= plen; pz /= plen;
        }
        
        // u = normalize(p × d)
        float ux = py * dz - pz * dy;
        float uy = pz * dx - px * dz;
        float uz = px * dy - py * dx;
        float ulen = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (ulen > 0.0001f) {
            ux /= ulen; uy /= ulen; uz /= ulen;
        }
        
        return new float[]{px, py, pz, ux, uy, uz};
    }
    /**
     * Empty implementation - RaysRenderer overrides render() completely,
     * so this method is never called by the parent class.
     */
    @Override
    protected void emitCage(MatrixStack matrices, VertexConsumer consumer,
                            Mesh mesh, int color, int light, FillConfig fill,
                            Primitive primitive, WaveConfig waveConfig, float time) {
        // Not used - see render() override
    }
    
    /**
     * Empty implementation - RaysRenderer overrides render() completely,
     * so this method is never called by the parent class.
     */
    @Override
    protected void emitWireframe(MatrixStack matrices, VertexConsumer consumer,
                                  Mesh mesh, int color, int light, FillConfig fill,
                                  WaveConfig waveConfig, float time) {
        // Not used - see render() override
    }
}
