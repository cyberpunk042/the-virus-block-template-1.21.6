#version 150

// ═══════════════════════════════════════════════════════════════════════════
// TERRAIN-CONFORMING SHOCKWAVE RING
// Fixed for Minecraft's compressed depth buffer (0.997 - 1.0 range)
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
// MINECRAFT DEPTH HANDLING
// Minecraft depth buffer uses a very compressed range (0.997 - 1.0)
// We need to remap this to get usable distance values
// ═══════════════════════════════════════════════════════════════════════════

float depthToDistance(float depth) {
    // Minecraft uses: near = 0.05, far = varies by render distance
    // The depth buffer is non-linear and very compressed
    
    // Standard perspective depth linearization with Minecraft values
    float near = 0.05;
    float far = 256.0;
    
    // Linearize depth
    float z = (2.0 * near * far) / (far + near - depth * (far - near));
    
    return z;
}

void main() {
    vec4 sceneColor = texture(InSampler, texCoord);
    float rawDepth = texture(DepthSampler, texCoord).r;
    
    // ═══════════════════════════════════════════════════════════════════════
    // SKY DETECTION
    // Sky has depth exactly 1.0 or very close
    // ═══════════════════════════════════════════════════════════════════════
    
    if (rawDepth > 0.9999) {
        // Sky - pass through unchanged
        fragColor = sceneColor;
        return;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONVERT DEPTH TO WORLD DISTANCE
    // ═══════════════════════════════════════════════════════════════════════
    
    float worldDistance = depthToDistance(rawDepth);
    
    // ═══════════════════════════════════════════════════════════════════════
    // RING PARAMETERS
    // Ring at specific distance from camera
    // ═══════════════════════════════════════════════════════════════════════
    
    float ringRadius = 15.0;      // 15 blocks from camera
    float ringThickness = 3.0;    // 3 blocks thick
    
    // How far is this pixel from the ring?
    float distFromRing = abs(worldDistance - ringRadius);
    
    // Create smooth ring mask
    float ringMask = 1.0 - smoothstep(0.0, ringThickness, distFromRing);
    
    // ═══════════════════════════════════════════════════════════════════════
    // COLORING
    // ═══════════════════════════════════════════════════════════════════════
    
    vec3 finalColor = sceneColor.rgb;
    
    // Ring effect - BRIGHT CYAN
    if (ringMask > 0.01) {
        vec3 ringColor = vec3(0.0, 1.0, 1.0);  // Cyan
        finalColor = mix(finalColor, ringColor, ringMask * 0.8);
        
        // White hot core
        float coreMask = 1.0 - smoothstep(0.0, ringThickness * 0.3, distFromRing);
        finalColor = mix(finalColor, vec3(1.0), coreMask * 0.9);
    }
    
    // DEBUG: Show distance as subtle color tint
    // Close = green tint, Far = blue tint
    float normalizedDist = clamp(worldDistance / 50.0, 0.0, 1.0);
    vec3 distTint = mix(vec3(0.2, 0.5, 0.0), vec3(0.0, 0.2, 0.5), normalizedDist);
    finalColor = mix(finalColor, distTint, 0.1);  // Very subtle
    
    fragColor = vec4(finalColor, 1.0);
}
