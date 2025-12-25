package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.animation.AnimationApplier;
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
            "[RAYS] Tessellating: count={}, arrangement={}, layers={}",
            shape.count(), shape.arrangement(), shape.layers());
        
        return RaysTessellator.tessellate(shape);
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
                     flowConfig, motionConfig);
        
        Logging.FIELD.topic("render").trace("[RAYS] DONE: {} vertices, fade={}, flow={}, motion={}", 
            mesh.vertexCount(), hasFade, flowConfig != null, motionConfig != null);
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
                               RayFlowConfig flowConfig, RayMotionConfig motionConfig) {
        
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();
        
        // Pre-calculate flow animation phase (0-1)
        final float lengthPhase = calculateLengthPhase(flowConfig, time);
        final float travelOffset = calculateTravelOffset(flowConfig, time);
        
        // For each line segment (pairs of vertices: v0=start/center, v1=end/outer)
        final int[] rayIndex = {0};
        mesh.forEachLine((v0, v1) -> {
            int idx = rayIndex[0]++;
            
            // Get base positions
            float x0 = v0.x(), y0 = v0.y(), z0 = v0.z();
            float x1 = v1.x(), y1 = v1.y(), z1 = v1.z();
            
            // === Apply Flow Animation (RADIATE/ABSORB move vertices) ===
            if (flowConfig != null && flowConfig.hasLength()) {
                float[] animated = applyLengthFlow(flowConfig, x0, y0, z0, x1, y1, z1, lengthPhase);
                x0 = animated[0]; y0 = animated[1]; z0 = animated[2];
                x1 = animated[3]; y1 = animated[4]; z1 = animated[5];
            }
            
            // === Apply Motion Animation (geometry transform) ===
            if (motionConfig != null && motionConfig.isActive()) {
                float[] m0 = applyMotion(motionConfig, x0, y0, z0, time, idx);
                float[] m1 = applyMotion(motionConfig, x1, y1, z1, time, idx);
                x0 = m0[0]; y0 = m0[1]; z0 = m0[2];
                x1 = m1[0]; y1 = m1[1]; z1 = m1[2];
            }
            
            // === Apply Wave Animation ===
            if (waveConfig != null && waveConfig.isActive()) {
                float[] w0 = AnimationApplier.applyWaveToVertex(waveConfig, x0, y0, z0, time);
                float[] w1 = AnimationApplier.applyWaveToVertex(waveConfig, x1, y1, z1, time);
                x0 = w0[0]; y0 = w0[1]; z0 = w0[2];
                x1 = w1[0]; y1 = w1[1]; z1 = w1[2];
            }
            
            // Emit both vertices of the line
            emitLineVertex(consumer, x0, y0, z0, x1, y1, z1, positionMatrix, normalMatrix,
                          baseColor, baseAlpha, light, colorCtx, v0, flowConfig, time, idx, travelOffset);
            emitLineVertex(consumer, x1, y1, z1, x0, y0, z0, positionMatrix, normalMatrix,
                          baseColor, baseAlpha, light, colorCtx, v1, flowConfig, time, idx, travelOffset);
        });
    }
    
    /**
     * Applies Length flow animation by moving ray endpoints.
     * 
     * <p>Returns [x0, y0, z0, x1, y1, z1] - the animated start and end positions.</p>
     */
    private float[] applyLengthFlow(RayFlowConfig flow, 
                                     float x0, float y0, float z0,
                                     float x1, float y1, float z1,
                                     float phase) {
        // Direction from start to end
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        
        return switch (flow.length()) {
            case RADIATE -> {
                // Ray GROWS outward: start stays at center, end moves from center toward full length
                // At phase=0: end is at start (no ray visible)
                // At phase=1: end is at full position
                float newX1 = x0 + dx * phase;
                float newY1 = y0 + dy * phase;
                float newZ1 = z0 + dz * phase;
                yield new float[]{x0, y0, z0, newX1, newY1, newZ1};
            }
            case ABSORB -> {
                // Ray SHRINKS inward: end stays at outer, start moves from outer toward center
                // At phase=0: start is at end (no ray visible)
                // At phase=1: start is at center (full ray)
                float newX0 = x1 - dx * phase;
                float newY0 = y1 - dy * phase;
                float newZ0 = z1 - dz * phase;
                yield new float[]{newX0, newY0, newZ0, x1, y1, z1};
            }
            case PULSE -> {
                // Ray breathes: both endpoints oscillate
                float halfPhase = 0.5f + 0.5f * phase; // 0.5 to 1.0 range
                float centerX = (x0 + x1) * 0.5f;
                float centerY = (y0 + y1) * 0.5f;
                float centerZ = (z0 + z1) * 0.5f;
                float newX0 = centerX + (x0 - centerX) * halfPhase;
                float newY0 = centerY + (y0 - centerY) * halfPhase;
                float newZ0 = centerZ + (z0 - centerZ) * halfPhase;
                float newX1 = centerX + (x1 - centerX) * halfPhase;
                float newY1 = centerY + (y1 - centerY) * halfPhase;
                float newZ1 = centerZ + (z1 - centerZ) * halfPhase;
                yield new float[]{newX0, newY0, newZ0, newX1, newY1, newZ1};
            }
            default -> new float[]{x0, y0, z0, x1, y1, z1};
        };
    }
    
    /**
     * Emits a single line vertex with color and travel animation.
     */
    private void emitLineVertex(VertexConsumer consumer, 
                                 float x, float y, float z,
                                 float otherX, float otherY, float otherZ,
                                 Matrix4f positionMatrix, Matrix3f normalMatrix,
                                 int baseColor, float baseAlpha, int light,
                                 ColorContext colorCtx, Vertex originalVertex,
                                 RayFlowConfig flowConfig, float time, int rayIndex, float travelOffset) {
        
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
            vertexColor = colorCtx.calculateColor(originalVertex.x(), originalVertex.y(), originalVertex.z(), 0);
        } else {
            vertexColor = baseColor;
        }
        
        // Apply travel animation to alpha (CHASE/SCROLL still use alpha for particle effect)
        float alpha = baseAlpha;
        float t = originalVertex.u(); // Parametric position along original ray
        alpha *= calculateTravelAlpha(flowConfig, t, travelOffset);
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
        
        float speed = Math.max(0.1f, flow.lengthSpeed()); // Ensure visible speed
        // Slow down the cycle - one full cycle per 2 seconds at speed=1
        float phase = (time * speed * 0.25f) % 1.0f;
        if (phase < 0) phase += 1.0f;
        
        return switch (flow.length()) {
            case RADIATE -> phase; // 0→1 growth (ray extends outward)
            case ABSORB -> 1.0f - phase; // 1→0 shrink (ray retracts inward)
            case PULSE -> {
                // Oscillate between 0.2 and 1.0 (never fully invisible)
                float sine = (float)Math.sin(time * speed * Math.PI);
                yield 0.6f + 0.4f * sine;
            }
            default -> 1f;
        };
    }
    
    /**
     * Calculates the travel animation offset for the visibility window.
     * Used by CHASE and SCROLL modes.
     */
    private float calculateTravelOffset(RayFlowConfig flow, float time) {
        if (flow == null || !flow.hasTravel()) return 0f;
        
        float speed = flow.travelSpeed();
        float offset = (time * speed * 0.3f) % 1.0f;
        if (offset < 0) offset += 1.0f;
        
        return offset;
    }
    
    /**
     * Emits a single ray line vertex with fade and animation applied.
     */
    private void emitRayVertex(VertexConsumer consumer, Vertex v, Vertex other,
                                Matrix4f positionMatrix, Matrix3f normalMatrix,
                                int baseColor, float baseAlpha, int light,
                                ColorContext colorCtx, WaveConfig waveConfig, float time,
                                float fadeStart, float fadeEnd, boolean hasFade,
                                RayFlowConfig flowConfig, RayMotionConfig motionConfig,
                                float lengthPhase, float travelOffset, int rayIndex) {
        float vx = v.x();
        float vy = v.y();
        float vz = v.z();
        
        // === Apply Motion Animation (geometry transform) ===
        if (motionConfig != null && motionConfig.isActive()) {
            float[] displaced = applyMotion(motionConfig, vx, vy, vz, time, rayIndex);
            vx = displaced[0];
            vy = displaced[1];
            vz = displaced[2];
        }
        
        // === Apply Wave displacement ===
        if (waveConfig != null && waveConfig.isActive()) {
            float[] displaced = AnimationApplier.applyWaveToVertex(waveConfig, vx, vy, vz, time);
            vx = displaced[0];
            vy = displaced[1];
            vz = displaced[2];
        }
        
        // Transform position
        Vector4f pos = new Vector4f(vx, vy, vz, 1.0f);
        pos.mul(positionMatrix);
        
        // For lines, normal is the direction to other vertex
        Vector3f dir = new Vector3f(other.x() - v.x(), other.y() - v.y(), other.z() - v.z());
        dir.normalize();
        dir.mul(normalMatrix);
        
        // Calculate color
        int vertexColor;
        if (colorCtx != null && colorCtx.isPerVertex()) {
            vertexColor = colorCtx.calculateColor(v.x(), v.y(), v.z(), 0);
        } else {
            vertexColor = baseColor;
        }
        
        // === Calculate Alpha ===
        float t = v.u(); // Parametric position along ray (0 = start, 1 = end)
        float alpha = baseAlpha;
        
        // 1. Apply base fade gradient
        if (hasFade) {
            float fadeFactor = fadeStart + (fadeEnd - fadeStart) * t;
            alpha *= fadeFactor;
        }
        
        // 2. Apply Length animation (RADIATE/ABSORB/PULSE)
        alpha *= calculateLengthAlpha(flowConfig, t, lengthPhase);
        
        // 3. Apply Travel animation (CHASE/SCROLL)
        alpha *= calculateTravelAlpha(flowConfig, t, travelOffset);
        
        // 4. Apply Flicker animation (SCINTILLATION/STROBE)
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
    
    // =========================================================================
    // Flow Animation Calculations
    // =========================================================================
    
    /**
     * Calculates alpha multiplier for Length animation.
     * 
     * <p>These modes control how much of the ray's LENGTH is visible:</p>
     * <ul>
     *   <li>RADIATE: Visible from origin outward to lengthPhase (ray grows)</li>
     *   <li>ABSORB: Visible from (1-lengthPhase) to end (ray shrinks)</li>
     *   <li>PULSE: Visible window oscillates</li>
     * </ul>
     * 
     * @param t parametric position along ray (0=origin, 1=end)
     * @param lengthPhase current animation phase (0-1)
     */
    private float calculateLengthAlpha(RayFlowConfig flow, float t, float lengthPhase) {
        if (flow == null || !flow.hasLength()) return 1f;
        
        // Edge softness - prevents hard cutoffs
        final float EDGE_WIDTH = 0.1f;
        
        return switch (flow.length()) {
            case RADIATE -> {
                // Visible from t=0 to t=lengthPhase, with soft edge at front
                if (t <= lengthPhase - EDGE_WIDTH) {
                    yield 1f; // Fully visible
                } else if (t <= lengthPhase) {
                    // Fade out at leading edge
                    yield 1f - (t - (lengthPhase - EDGE_WIDTH)) / EDGE_WIDTH;
                } else {
                    yield 0f; // Invisible beyond phase
                }
            }
            case ABSORB -> {
                // Visible from t=(1-lengthPhase) to t=1, with soft edge at back
                float threshold = 1f - lengthPhase;
                if (t >= threshold + EDGE_WIDTH) {
                    yield 1f; // Fully visible
                } else if (t >= threshold) {
                    // Fade in at trailing edge
                    yield (t - threshold) / EDGE_WIDTH;
                } else {
                    yield 0f; // Invisible before threshold
                }
            }
            case PULSE -> {
                // Visible window oscillates around center
                float center = 0.5f;
                float halfLen = lengthPhase * 0.5f;
                float distFromVisible = Math.abs(t - center) - halfLen;
                if (distFromVisible <= 0) {
                    yield 1f; // Inside visible window
                } else if (distFromVisible < EDGE_WIDTH) {
                    yield 1f - distFromVisible / EDGE_WIDTH; // Soft edge
                } else {
                    yield 0f;
                }
            }
            default -> 1f;
        };
    }
    
    /**
     * Calculates alpha multiplier for Travel animation.
     * CHASE: Discrete particles moving along ray
     * SCROLL: Continuous gradient scrolling
     */
    private float calculateTravelAlpha(RayFlowConfig flow, float t, float travelOffset) {
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
            default -> 1f;
        };
    }
    
    /**
     * Calculates alpha multiplier for Flicker animation.
     * SCINTILLATION: Random per-ray flickering (star-like)
     * STROBE: Synchronized on/off blinking
     */
    private float calculateFlickerAlpha(RayFlowConfig flow, float time, int rayIndex) {
        if (flow == null || !flow.hasFlicker()) return 1f;
        
        float intensity = flow.flickerIntensity();
        float freq = flow.flickerFrequency();
        
        return switch (flow.flicker()) {
            case SCINTILLATION -> {
                // Per-ray random flicker using hash
                float hash = hash(rayIndex, (int)(time * freq));
                float flicker = 0.5f + 0.5f * hash; // 0.5 to 1.0 range
                yield 1f - intensity * (1f - flicker);
            }
            case STROBE -> {
                // Synchronized on/off
                float wave = (float)Math.sin(time * freq * Math.PI * 2);
                yield wave > 0 ? 1f : (1f - intensity);
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
     *   <li>SPIRAL - Rotates entire ray field around Y axis continuously</li>
     *   <li>LINEAR - Rays drift along direction vector (wrapping)</li>
     *   <li>OSCILLATE - Rays sway back and forth along direction</li>
     *   <li>RIPPLE - Radial wave pulses outward from center</li>
     * </ul>
     */
    private float[] applyMotion(RayMotionConfig motion, float x, float y, float z, float time, int rayIndex) {
        if (motion == null || !motion.isActive()) {
            return new float[]{x, y, z};
        }
        
        float speed = motion.speed();
        float amp = Math.max(0.1f, motion.amplitude()); // Ensure visible amplitude
        float freq = Math.max(0.5f, motion.frequency()); // Ensure visible frequency
        Vector3f dir = motion.normalizedDirection();
        
        return switch (motion.mode()) {
            case LINEAR -> {
                // Ray segment slides along its OWN axis (radial direction from center)
                // Each ray travels outward from center, wrapping when it reaches outer edge
                float dist = (float)Math.sqrt(x * x + z * z);
                if (dist < 0.001f) {
                    yield new float[]{x, y, z}; // Center point doesn't move
                }
                
                // Normalize to get radial direction
                float nx = x / dist;
                float nz = z / dist;
                
                // Offset along radial direction (wrapping)
                float offset = (time * speed * 0.5f) % 1.0f;
                float displacement = offset * amp * 2.0f;
                
                yield new float[]{
                    x + nx * displacement,
                    y,
                    z + nz * displacement
                };
            }
            case OSCILLATE -> {
                // Ray segment oscillates along its OWN axis (back and forth radially)
                float dist = (float)Math.sqrt(x * x + z * z);
                if (dist < 0.001f) {
                    yield new float[]{x, y, z}; // Center point doesn't move
                }
                
                // Normalize to get radial direction
                float nx = x / dist;
                float nz = z / dist;
                
                // Sine wave oscillation along radial direction
                float wave = (float)Math.sin(time * speed * Math.PI) * amp;
                
                yield new float[]{
                    x + nx * wave,
                    y,
                    z + nz * wave
                };
            }
            case SPIRAL -> {
                // CURVE rays into spiral shape (Archimedean spiral)
                // Each vertex is rotated by an angle proportional to its distance from center
                // This bends straight radial rays into spiral arms
                float dist = (float)Math.sqrt(x * x + z * z);
                
                // Spiral tightness: how much rotation per unit distance
                // Higher freq = tighter spiral, speed controls animation
                float spiralAngle = dist * freq * 2.0f + time * speed * 0.5f;
                float cos = (float)Math.cos(spiralAngle);
                float sin = (float)Math.sin(spiralAngle);
                
                // Rotate this vertex around Y axis by spiralAngle
                yield new float[]{
                    x * cos - z * sin,
                    y,
                    x * sin + z * cos
                };
            }
            case RIPPLE -> {
                // Radial wave pulses outward from center
                float dist = (float)Math.sqrt(x * x + z * z);
                if (dist < 0.001f) {
                    yield new float[]{x, y, z}; // Don't displace center
                }
                // Wave travels outward: inner parts move first
                float phase = dist * freq - time * speed * 2.0f;
                float wave = (float)Math.sin(phase * Math.PI * 2.0f) * amp * 0.3f;
                // Displace radially outward/inward
                float nx = x / dist;
                float nz = z / dist;
                yield new float[]{
                    x + nx * wave,
                    y,
                    z + nz * wave
                };
            }
            default -> new float[]{x, y, z};
        };
    }
    
    /**
     * Rays don't have a separate cage mode - they ARE lines.
     */
    @Override
    protected void emitCage(MatrixStack matrices, VertexConsumer consumer,
                            Mesh mesh, int color, int light, FillConfig fill,
                            Primitive primitive, WaveConfig waveConfig, float time) {
        // Redirects to normal render since rays are always lines
    }
    
    /**
     * For rays, solid and wireframe are the same - it's all lines.
     */
    @Override
    protected void emitWireframe(MatrixStack matrices, VertexConsumer consumer,
                                  Mesh mesh, int color, int light, FillConfig fill,
                                  WaveConfig waveConfig, float time) {
        // Redirects to normal render since rays are always lines
    }
}
