package net.cyberpunk042.client.visual.mesh.ray.flow;

import net.cyberpunk042.visual.animation.RayFlowConfig;
import net.cyberpunk042.visual.energy.EnergyTravel;
import net.cyberpunk042.visual.energy.TravelBlendMode;

/**
 * Computes travel alpha for per-vertex modulation.
 * 
 * <p>Travel modes create moving effects along the ray by modulating per-vertex alpha.
 * The travel phase (position) is computed here; actual vertex alpha is computed
 * during tessellation using computeTravelAlpha(t, travelPhase).</p>
 * 
 * @see EnergyTravel
 */
public final class FlowTravelStage implements FlowStage {
    
    public static final FlowTravelStage INSTANCE = new FlowTravelStage();
    
    private FlowTravelStage() {}
    
    @Override
    public AnimationState process(AnimationState state, FlowContext ctx) {
        RayFlowConfig config = ctx.config();
        if (config == null || !config.hasTravel()) {
            return state;
        }
        
        EnergyTravel travelMode = config.effectiveTravel();
        if (travelMode == EnergyTravel.NONE) {
            return state;
        }
        
        // Travel phase is stored separately - not combined with base phase
        float travelSpeed = Math.max(0.1f, config.travelSpeed());
        float travelPhase = (ctx.time() * travelSpeed * 0.3f) % 1.0f;
        if (travelPhase < 0) travelPhase += 1.0f;
        
        return state.withTravelPhase(travelPhase);
    }
    
    /**
     * Compute per-vertex travel alpha based on vertex position.
     * 
     * <p>This static method is called during tessellation to get the alpha
     * modifier for a specific vertex based on its position along the ray.</p>
     * 
     * @param t Vertex position along axis (0 = base, 1 = tip)
     * @param mode EnergyTravel type
     * @param phase Current animation phase (0-1)
     * @param chaseCount Number of chase particles
     * @param chaseWidth Width of each particle
     * @return Alpha multiplier (0-1)
     */
    public static float computeTravelAlpha(float t, EnergyTravel mode, 
            float phase, int chaseCount, float chaseWidth) {
        
        if (mode == null || mode == EnergyTravel.NONE) {
            return 1.0f;
        }
        
        final int count = Math.max(1, chaseCount);
        final float width = Math.max(0.05f, chaseWidth);
        
        return switch (mode) {
            case NONE -> 1.0f;
            case CHASE, BIPOLAR_CHASE_SYNC, BIPOLAR_CHASE_ALT, BIPOLAR -> {
                // Multiple particles evenly spaced
                float spacing = 1f / count;
                float maxAlpha = 0f;
                for (int i = 0; i < count; i++) {
                    float center = (phase + i * spacing) % 1f;
                    float dist = Math.min(Math.abs(t - center), Math.min(
                        Math.abs(t - center - 1f), Math.abs(t - center + 1f)));
                    if (dist <= width / 2f) {
                        float falloff = 1f - (dist / (width / 2f));
                        maxAlpha = Math.max(maxAlpha, falloff * falloff);
                    }
                }
                yield maxAlpha;
            }
            case REVERSE_CHASE -> {
                // Same as CHASE but moves inward (from tip to base)
                float reverseOffset = 1f - phase;
                float spacing = 1f / count;
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
            case SCROLL, BIPOLAR_SCROLL_SYNC, BIPOLAR_SCROLL_ALT -> {
                // Continuous gradient scrolls along the ray
                float scrolledT = (t + phase) % 1f;
                yield 1f - Math.abs(scrolledT - 0.5f) * 2f;
            }
            case REVERSE_SCROLL -> {
                float scrolledT = (t - phase + 1f) % 1f;
                yield 1f - Math.abs(scrolledT - 0.5f) * 2f;
            }
            case COMET, BIPOLAR_COMET_SYNC, BIPOLAR_COMET_ALT -> {
                // Bright head with fading tail
                float headPos = phase;
                float tailLength = Math.max(0.1f, width);
                float tailStart = headPos - tailLength;
                
                float distBehind;
                if (t > headPos) {
                    if (tailStart < 0) {
                        float wrappedTailStart = tailStart + 1f;
                        if (t >= wrappedTailStart) {
                            distBehind = (1f - t) + headPos;
                        } else {
                            yield 0f;
                        }
                    } else {
                        yield 0f;
                    }
                } else if (t >= tailStart || tailStart < 0) {
                    distBehind = headPos - t;
                } else {
                    yield 0f;
                }
                
                if (distBehind >= 0 && distBehind <= tailLength) {
                    float tailAlpha = 1f - (distBehind / tailLength);
                    yield tailAlpha * tailAlpha;
                } else {
                    yield 0f;
                }
            }
            case REVERSE_COMET -> {
                // Comet traveling inward
                float headPos = 1f - phase;
                float tailLength = Math.max(0.1f, width);
                float tailEnd = headPos + tailLength;
                
                float distBehind;
                if (t < headPos) {
                    yield 0f;
                } else if (t <= tailEnd || tailEnd > 1f) {
                    distBehind = t - headPos;
                    if (distBehind > tailLength && tailEnd > 1f) {
                        // Wrapped case
                        if (t < tailEnd - 1f) {
                            distBehind = t + (1f - headPos);
                        } else {
                            yield 0f;
                        }
                    }
                } else {
                    yield 0f;
                }
                
                if (distBehind >= 0 && distBehind <= tailLength) {
                    float tailAlpha = 1f - (distBehind / tailLength);
                    yield tailAlpha * tailAlpha;
                } else {
                    yield 0f;
                }
            }
            case SPARK, BIPOLAR_SPARK_SYNC, BIPOLAR_SPARK_ALT -> {
                // Random bright flashes
                float maxAlpha = 0f;
                for (int i = 0; i < count; i++) {
                    float sparkPhase = hash(i, (int)(phase * 10));
                    float sparkPos = hash(i + 1000, (int)(phase * 3));
                    if (sparkPhase > 0.5f) {
                        float dist = Math.abs(t - sparkPos);
                        if (dist < width) {
                            float spark = 1f - (dist / width);
                            maxAlpha = Math.max(maxAlpha, spark * spark);
                        }
                    }
                }
                yield maxAlpha;
            }
            case PULSE_WAVE, BIPOLAR_PULSE_SYNC, BIPOLAR_PULSE_ALT -> {
                // Traveling wave of brightness
                float waveWidth = Math.max(0.1f, width);
                float maxAlpha = 0f;
                for (int i = 0; i < count; i++) {
                    float waveCenter = (phase + (float)i / count) % 1f;
                    float dist = Math.min(Math.abs(t - waveCenter),
                                 Math.min(Math.abs(t - waveCenter - 1f),
                                          Math.abs(t - waveCenter + 1f)));
                    if (dist < waveWidth) {
                        float normalized = dist / waveWidth;
                        float pulse = (float) Math.exp(-normalized * normalized * 4);
                        maxAlpha = Math.max(maxAlpha, pulse);
                    }
                }
                yield maxAlpha;
            }
            case REVERSE_PULSE -> {
                // Pulse wave traveling inward
                float waveWidth = Math.max(0.1f, width);
                float maxAlpha = 0f;
                float reversePhase = 1f - phase;
                for (int i = 0; i < count; i++) {
                    float waveCenter = (reversePhase + (float)i / count) % 1f;
                    float dist = Math.min(Math.abs(t - waveCenter),
                                 Math.min(Math.abs(t - waveCenter - 1f),
                                          Math.abs(t - waveCenter + 1f)));
                    if (dist < waveWidth) {
                        float normalized = dist / waveWidth;
                        float pulse = (float) Math.exp(-normalized * normalized * 4);
                        maxAlpha = Math.max(maxAlpha, pulse);
                    }
                }
                yield maxAlpha;
            }
            case BIPOLAR_ALTERNATE -> {
                // Bipolar jet with ALTERNATING phase - one side leads, other trails
                // Creates a ping-pong effect where the phase on one side is offset
                
                // Convert t to signed distance from center (-0.5 to +0.5)
                float signedDist = t - 0.5f;
                
                // Determine which "side" we're on and apply appropriate phase
                float effectivePhase;
                if (signedDist >= 0) {
                    effectivePhase = phase;  // Upper jet uses normal phase
                } else {
                    effectivePhase = (phase + 0.5f) % 1f;  // Lower jet is 180° out of phase
                }
                
                // Distance from center normalized to 0-1
                float normalizedDist = Math.abs(signedDist) * 2f;
                
                // Apply chase-like effect
                float spacing = 1f / count;
                float maxAlpha = 0f;
                for (int i = 0; i < count; i++) {
                    float center = (effectivePhase + i * spacing) % 1f;
                    float dist = Math.min(Math.abs(normalizedDist - center), 
                        Math.min(Math.abs(normalizedDist - center - 1f), 
                                 Math.abs(normalizedDist - center + 1f)));
                    if (dist <= width / 2f) {
                        float falloff = 1f - (dist / (width / 2f));
                        maxAlpha = Math.max(maxAlpha, falloff * falloff);
                    }
                }
                yield maxAlpha;
            }
        };
    }
    
    /**
     * Simple hash function for deterministic pseudo-random values.
     */
    public static float hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        return (h & 0x7FFFFFFF) / (float)0x7FFFFFFF;
    }
    
    /**
     * Compute blended alpha using TravelBlendMode.
     * 
     * <p>This combines the raw travel alpha with base alpha according to the blend mode,
     * allowing for OVERLAY, ADDITIVE, and MODULATE effects instead of just REPLACE.</p>
     * 
     * @param baseAlpha The base vertex alpha before travel effect
     * @param t Vertex position along axis (0 = base, 1 = tip)
     * @param mode EnergyTravel type
     * @param phase Current animation phase (0-1)
     * @param chaseCount Number of chase particles
     * @param chaseWidth Width of each particle
     * @param blendMode How to blend travel with base alpha
     * @param intensity Blend intensity (for ADDITIVE and MODULATE modes)
     * @param minAlpha Minimum alpha floor (0-1), prevents going fully invisible
     * @return Final blended alpha, guaranteed to be at least minAlpha
     */
    public static float computeBlendedAlpha(float baseAlpha, float t, EnergyTravel mode,
            float phase, int chaseCount, float chaseWidth, 
            TravelBlendMode blendMode, float intensity, float minAlpha) {
        
        // Get raw travel alpha (0-1)
        float travelAlpha = computeTravelAlpha(t, mode, phase, chaseCount, chaseWidth);
        
        // Apply blend mode
        if (blendMode == null) {
            blendMode = TravelBlendMode.REPLACE;
        }
        
        float blendedAlpha = blendMode.blend(baseAlpha, travelAlpha, intensity);
        
        // Enforce minimum alpha floor
        return Math.max(minAlpha, blendedAlpha);
    }
    
    @Override
    public boolean shouldRun(FlowContext ctx) {
        if (ctx.config() == null) return false;
        return ctx.config().hasTravel();
    }
    
    @Override
    public String name() {
        return "TravelStage";
    }
}
