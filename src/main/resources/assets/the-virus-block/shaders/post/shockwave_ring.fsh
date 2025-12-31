#version 150

// ═══════════════════════════════════════════════════════════════════════════
// TERRAIN-CONFORMING SHOCKWAVE RING - DYNAMIC VERSION
// Uses uniform block for Java-controlled parameters
// ═══════════════════════════════════════════════════════════════════════════

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

// Custom uniform block from JSON
layout(std140) uniform ShockwaveConfig {
    float RingRadius;
    float RingThickness;
    float Intensity;
    float Time;
};

out vec4 fragColor;

// ═══════════════════════════════════════════════════════════════════════════
// DEPTH LINEARIZATION
// ═══════════════════════════════════════════════════════════════════════════

float linearizeDepth(float depth, float near, float far) {
    return (2.0 * near * far) / (far + near - depth * (far - near));
}

// ═══════════════════════════════════════════════════════════════════════════
// GLOW FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

float glowFalloff(float dist, float radius) {
    float t = dist / radius;
    return max(0.0, 1.0 - t * t);
}

void main() {
    vec4 sceneColor = texture(InSampler, texCoord);
    float rawDepth = texture(DepthSampler, texCoord).r;
    
    // Sky detection
    if (rawDepth > 0.9999) {
        fragColor = sceneColor;
        return;
    }
    
    // Linearize depth to world distance
    float near = 0.05;
    float far = 256.0;
    float worldDistance = linearizeDepth(rawDepth, near, far);
    
    // ═══════════════════════════════════════════════════════════════════════
    // RING CALCULATION using uniforms
    // ═══════════════════════════════════════════════════════════════════════
    
    float distFromRing = abs(worldDistance - RingRadius);
    
    // Core mask (sharp center)
    float coreWidth = RingThickness * 0.2;
    float coreMask = 1.0 - smoothstep(0.0, coreWidth, distFromRing);
    
    // Inner ring mask
    float innerMask = 1.0 - smoothstep(0.0, RingThickness * 0.5, distFromRing);
    
    // Outer glow mask
    float outerMask = glowFalloff(distFromRing, RingThickness * 2.0);
    
    // ═══════════════════════════════════════════════════════════════════════
    // COLOR COMPOSITION
    // ═══════════════════════════════════════════════════════════════════════
    
    vec3 coreColor = vec3(1.0, 1.0, 1.0);           // White hot
    vec3 ringColor = vec3(0.0, 1.0, 1.0);           // Cyan
    vec3 outerGlow = vec3(0.2, 0.5, 1.0);           // Blue
    
    vec3 finalColor = sceneColor.rgb;
    
    // Apply layers with intensity
    finalColor = mix(finalColor, outerGlow, outerMask * 0.4 * Intensity);
    finalColor = mix(finalColor, ringColor, innerMask * 0.7 * Intensity);
    finalColor = mix(finalColor, coreColor, coreMask * 0.9 * Intensity);
    
    // Additive bloom
    finalColor += ringColor * coreMask * 0.3 * Intensity;
    
    fragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
}
