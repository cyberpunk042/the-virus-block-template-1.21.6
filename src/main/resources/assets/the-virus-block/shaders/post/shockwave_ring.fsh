#version 150

// SHOCKWAVE RING - uses depth to conform to terrain
uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;
in vec2 oneTexel;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

// Animation time (we'll use fragment position as hacky animation)
// In production, this would be a uniform passed from Java

void main() {
    vec4 sceneColor = texture(InSampler, texCoord);
    float depth = texture(DepthSampler, texCoord).r;
    
    // Invert depth (1.0 = near, 0.0 = far)
    float invDepth = 1.0 - depth;
    
    // ========================================================
    // SHOCKWAVE RING EFFECT
    // ========================================================
    
    // Ring center (screen center for now - would be uniform in production)
    vec2 center = vec2(0.5, 0.5);
    
    // Distance from center in screen space
    vec2 screenDist = texCoord - center;
    // Correct for aspect ratio
    float aspect = OutSize.x / OutSize.y;
    screenDist.x *= aspect;
    float dist = length(screenDist);
    
    // Ring parameters (would be uniforms in production)
    float ringRadius = 0.3;      // Current ring radius (0-1 screen space)
    float ringWidth = 0.05;      // Ring thickness
    float glowFalloff = 0.1;     // Glow extends beyond ring
    
    // Calculate ring mask
    float ringDist = abs(dist - ringRadius);
    float ringMask = 1.0 - smoothstep(0.0, ringWidth, ringDist);
    
    // Extended glow
    float glowMask = 1.0 - smoothstep(ringWidth, ringWidth + glowFalloff, ringDist);
    
    // ========================================================
    // DEPTH-BASED TERRAIN CONFORMITY
    // ========================================================
    
    // Only show ring where there's geometry (not sky)
    // Sky has depth ~1.0, terrain has depth < 1.0
    float terrainMask = smoothstep(0.99, 0.95, depth);
    
    // Modulate ring intensity by depth (closer = brighter)
    float depthIntensity = pow(invDepth, 0.3);
    
    // Combine masks
    float finalRing = ringMask * terrainMask;
    float finalGlow = glowMask * terrainMask * 0.5;
    
    // Ring color (cyan-white energy)
    vec3 ringColor = vec3(0.3, 0.9, 1.0);
    vec3 glowColor = vec3(0.1, 0.5, 0.8);
    
    // Mix
    vec3 finalColor = sceneColor.rgb;
    finalColor = mix(finalColor, glowColor, finalGlow * depthIntensity);
    finalColor = mix(finalColor, ringColor, finalRing * depthIntensity);
    
    // Add bright white core
    float coreMask = 1.0 - smoothstep(0.0, ringWidth * 0.3, ringDist);
    finalColor = mix(finalColor, vec3(1.0), coreMask * terrainMask * 0.7);
    
    fragColor = vec4(finalColor, 1.0);
}
