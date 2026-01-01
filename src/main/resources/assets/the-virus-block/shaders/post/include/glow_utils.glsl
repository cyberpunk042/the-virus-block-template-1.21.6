// ═══════════════════════════════════════════════════════════════════════════
// GLOW & RING UTILITIES - Ring contribution and glow falloff functions
// ═══════════════════════════════════════════════════════════════════════════
// Include: #include "include/glow_utils.glsl"

// Quadratic glow falloff
float glowFalloff(float dist, float radius) {
    float t = dist / radius;
    return max(0.0, 1.0 - t * t);
}

// Core thickness = sharp center line width
// Glow width = soft falloff around the core
float ringContribution(float dist, float ringDist, float coreThickness, float glowWidth, float intensity) {
    float distFromRing = abs(dist - ringDist);
    
    // Core - the bright sharp center (uses coreThickness)
    float coreMask = 1.0 - smoothstep(0.0, coreThickness * 0.5, distFromRing);
    
    // Inner glow - medium falloff (uses glowWidth)
    float innerMask = 1.0 - smoothstep(0.0, glowWidth * 0.5, distFromRing);
    
    // Outer glow - soft falloff (uses glowWidth)
    float outerMask = glowFalloff(distFromRing, glowWidth);
    
    return (coreMask * 0.9 + innerMask * 0.4 + outerMask * 0.2) * intensity;
}
