#version 150

// ═══════════════════════════════════════════════════════════════════════════
// TERRAIN-CONFORMING SHOCKWAVE RING - PRODUCTION VERSION
// GPU shader with proper depth access via FrameGraph integration
// ═══════════════════════════════════════════════════════════════════════════

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

// ═══════════════════════════════════════════════════════════════════════════
// CONFIGURATION - These would ideally be uniforms but we hardcode for now
// ═══════════════════════════════════════════════════════════════════════════

// Ring parameters
const float RING_RADIUS = 20.0;           // Distance from camera (blocks)
const float RING_THICKNESS = 4.0;         // Width of the ring (blocks)
const float INTENSITY = 1.0;              // Overall intensity (0-2)

// Colors
const vec3 CORE_COLOR = vec3(1.0, 1.0, 1.0);           // White hot core
const vec3 RING_COLOR = vec3(0.0, 1.0, 1.0);           // Cyan glow
const vec3 OUTER_GLOW_COLOR = vec3(0.3, 0.6, 1.0);     // Blue outer glow

// Depth parameters
const float NEAR_PLANE = 0.05;
const float FAR_PLANE = 256.0;

// ═══════════════════════════════════════════════════════════════════════════
// DEPTH LINEARIZATION
// ═══════════════════════════════════════════════════════════════════════════

float linearizeDepth(float depth) {
    return (2.0 * NEAR_PLANE * FAR_PLANE) / (FAR_PLANE + NEAR_PLANE - depth * (FAR_PLANE - NEAR_PLANE));
}

// ═══════════════════════════════════════════════════════════════════════════
// GLOW FUNCTION - Multi-layer bloom
// ═══════════════════════════════════════════════════════════════════════════

float glowFalloff(float dist, float radius) {
    // Quadratic falloff for soft glow
    float t = dist / radius;
    return max(0.0, 1.0 - t * t);
}

void main() {
    vec4 sceneColor = texture(InSampler, texCoord);
    float rawDepth = texture(DepthSampler, texCoord).r;
    
    // ═══════════════════════════════════════════════════════════════════════
    // SKY DETECTION - Skip sky pixels
    // ═══════════════════════════════════════════════════════════════════════
    
    if (rawDepth > 0.9999) {
        fragColor = sceneColor;
        return;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONVERT DEPTH TO WORLD DISTANCE
    // ═══════════════════════════════════════════════════════════════════════
    
    float worldDistance = linearizeDepth(rawDepth);
    
    // ═══════════════════════════════════════════════════════════════════════
    // RING CALCULATION
    // ═══════════════════════════════════════════════════════════════════════
    
    float distFromRing = abs(worldDistance - RING_RADIUS);
    
    // Core mask (sharp center)
    float coreWidth = RING_THICKNESS * 0.2;
    float coreMask = 1.0 - smoothstep(0.0, coreWidth, distFromRing);
    
    // Inner ring mask
    float innerMask = 1.0 - smoothstep(0.0, RING_THICKNESS * 0.5, distFromRing);
    
    // Outer glow mask (extends beyond ring)
    float outerGlowRadius = RING_THICKNESS * 2.0;
    float outerMask = glowFalloff(distFromRing, outerGlowRadius);
    
    // ═══════════════════════════════════════════════════════════════════════
    // COLOR COMPOSITION
    // ═══════════════════════════════════════════════════════════════════════
    
    vec3 finalColor = sceneColor.rgb;
    
    // Layer 1: Outer glow (subtle, wide)
    finalColor = mix(finalColor, OUTER_GLOW_COLOR, outerMask * 0.3 * INTENSITY);
    
    // Layer 2: Main ring (medium intensity)
    finalColor = mix(finalColor, RING_COLOR, innerMask * 0.7 * INTENSITY);
    
    // Layer 3: Hot core (bright, narrow)
    finalColor = mix(finalColor, CORE_COLOR, coreMask * 0.9 * INTENSITY);
    
    // ═══════════════════════════════════════════════════════════════════════
    // BLOOM SIMULATION (fake HDR bloom)
    // ═══════════════════════════════════════════════════════════════════════
    
    float bloomIntensity = coreMask * 0.3 * INTENSITY;
    finalColor += RING_COLOR * bloomIntensity;  // Additive bloom
    
    // Clamp to prevent over-saturation
    finalColor = clamp(finalColor, 0.0, 1.0);
    
    fragColor = vec4(finalColor, 1.0);
}
