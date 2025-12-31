#version 150

// ═══════════════════════════════════════════════════════════════════════════
// TERRAIN-CONFORMING SHOCKWAVE RING - DEBUG VERSION
// Shows depth buffer as obvious color gradient
// ═══════════════════════════════════════════════════════════════════════════

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

void main() {
    vec4 sceneColor = texture(InSampler, texCoord);
    float rawDepth = texture(DepthSampler, texCoord).r;
    
    // ═══════════════════════════════════════════════════════════════════════
    // AGGRESSIVE DEBUG: Color by depth value
    // If depth is working: varied colors
    // If depth is dead (all 1.0): solid white/grey
    // ═══════════════════════════════════════════════════════════════════════
    
    // Split screen into 4 quadrants for diagnosis
    bool rightHalf = texCoord.x > 0.5;
    bool topHalf = texCoord.y > 0.5;
    
    vec3 debugColor;
    
    if (!rightHalf && topHalf) {
        // TOP-LEFT: Raw depth as grayscale (should show terrain if working)
        debugColor = vec3(rawDepth);
    }
    else if (rightHalf && topHalf) {
        // TOP-RIGHT: Inverted depth (terrain = white, sky = black if working)
        debugColor = vec3(1.0 - rawDepth);
    }
    else if (!rightHalf && !topHalf) {
        // BOTTOM-LEFT: Super-contrast depth (amplify the 0.997-1.0 range)
        // If depth works, this will show varied grays
        // If depth is dead, this will be solid
        float contrast = (rawDepth - 0.99) * 100.0;
        contrast = clamp(contrast, 0.0, 1.0);
        debugColor = vec3(contrast);
    }
    else {
        // BOTTOM-RIGHT: Original scene with ring overlay
        vec3 finalColor = sceneColor.rgb;
        
        // Ring parameters
        float ringRadius = 15.0;
        float ringThickness = 3.0;
        
        // Linearize depth
        float near = 0.05;
        float far = 256.0;
        float worldDistance = (2.0 * near * far) / (far + near - rawDepth * (far - near));
        
        // Ring mask
        float distFromRing = abs(worldDistance - ringRadius);
        float ringMask = 1.0 - smoothstep(0.0, ringThickness, distFromRing);
        
        // Apply cyan ring
        if (ringMask > 0.01 && rawDepth < 0.9999) {
            vec3 ringColor = vec3(0.0, 1.0, 1.0);
            finalColor = mix(finalColor, ringColor, ringMask * 0.9);
        }
        
        debugColor = finalColor;
    }
    
    fragColor = vec4(debugColor, 1.0);
}
