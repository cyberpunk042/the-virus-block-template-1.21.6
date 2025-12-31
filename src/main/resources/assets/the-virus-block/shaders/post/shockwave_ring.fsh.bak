#version 150

// ═══════════════════════════════════════════════════════════════════════════
// TERRAIN-CONFORMING SHOCKWAVE RING - WORLD-ANCHORED VERSION
// Supports: Fixed world position, multiple rings, expand/contract
// ═══════════════════════════════════════════════════════════════════════════

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

// Custom uniform block - Layout: 8 vec4s = 128 bytes
layout(std140) uniform ShockwaveConfig {
    // vec4 0: Basic params
    float RingRadius;       // Current ring radius (blocks)
    float RingThickness;    // Thickness of each ring
    float Intensity;        // Glow intensity (0-2)
    float Time;             // Animation time
    
    // vec4 1: Ring count and spacing
    float RingCount;        // Number of concentric rings (1-10)
    float RingSpacing;      // Distance between rings (blocks)
    float ContractMode;     // 0 = expand outward, 1 = contract inward
    float Reserved1;
    
    // vec4 2: Target world position (for world-anchored mode)
    float TargetX;
    float TargetY;
    float TargetZ;
    float UseWorldOrigin;   // 0 = camera-centered, 1 = world-anchored
    
    // vec4 3: Camera world position
    float CameraX;
    float CameraY;
    float CameraZ;
    float AspectRatio;      // width / height
    
    // vec4 4: Camera forward direction (normalized)
    float ForwardX;
    float ForwardY;
    float ForwardZ;
    float Fov;              // Field of view in radians
    
    // vec4 5: Camera up direction (normalized)
    float UpX;
    float UpY;
    float UpZ;
    float Reserved2;
    
    // vec4 6: Screen blackout / vignette
    float BlackoutAmount;   // 0 = no blackout, 1 = full black
    float VignetteAmount;   // 0 = no vignette, 1 = strong vignette
    float VignetteRadius;   // Inner radius of vignette (0-1)
    float Reserved3;
    
    // vec4 7: Color tint / filter
    float TintR;            // Tint color red (0-1)
    float TintG;            // Tint color green (0-1)
    float TintB;            // Tint color blue (0-1)
    float TintAmount;       // 0 = no tint, 1 = full tint
};

out vec4 fragColor;

// ═══════════════════════════════════════════════════════════════════════════
// DEPTH LINEARIZATION
// ═══════════════════════════════════════════════════════════════════════════

float linearizeDepth(float depth, float near, float far) {
    // Convert depth buffer (0-1) to NDC Z (-1 to 1) first
    float ndcZ = depth * 2.0 - 1.0;
    return (2.0 * near * far) / (far + near - ndcZ * (far - near));
}

// ═══════════════════════════════════════════════════════════════════════════
// WORLD POSITION RECONSTRUCTION
// ═══════════════════════════════════════════════════════════════════════════

vec3 reconstructWorldPos(vec2 uv, float linearDepth) {
    // Get camera vectors
    vec3 camPos = vec3(CameraX, CameraY, CameraZ);
    vec3 forward = normalize(vec3(ForwardX, ForwardY, ForwardZ));
    
    // Prevent exact gimbal lock by clamping forward.y
    // This ensures forward.xz always has SOME horizontal component for computing right
    // At 0.999, pitch is about 87.4° - close enough but safe
    float maxAbsY = 0.999;
    if (abs(forward.y) > maxAbsY) {
        float sign = forward.y > 0.0 ? 1.0 : -1.0;
        forward.y = sign * maxAbsY;
        forward = normalize(forward);
    }
    
    // Now cross product with worldUp is always valid
    vec3 worldUp = vec3(0.0, 1.0, 0.0);
    vec3 right = normalize(cross(forward, worldUp));
    vec3 up = normalize(cross(right, forward));
    
    // Calculate ray direction from UV using perspective projection
    // NDC: (0,0) is top-left, (1,1) is bottom-right -> convert to (-1,1) range
    vec2 ndc = uv * 2.0 - 1.0;
    
    // Calculate half-sizes at unit distance from camera
    float halfFovTan = tan(Fov * 0.5);
    float halfHeight = halfFovTan;
    float halfWidth = halfFovTan * AspectRatio;
    
    // Build ray direction: forward + screen offset
    vec3 rayDir = forward + right * (ndc.x * halfWidth) + up * (ndc.y * halfHeight);
    rayDir = normalize(rayDir);
    
    // World position = camera + direction * depth
    return camPos + rayDir * linearDepth;
}

// ═══════════════════════════════════════════════════════════════════════════
// GLOW FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

float glowFalloff(float dist, float radius) {
    float t = dist / radius;
    return max(0.0, 1.0 - t * t);
}

float ringContribution(float dist, float ringDist, float thickness, float intensity) {
    float distFromRing = abs(dist - ringDist);
    
    float coreWidth = thickness * 0.2;
    float coreMask = 1.0 - smoothstep(0.0, coreWidth, distFromRing);
    float innerMask = 1.0 - smoothstep(0.0, thickness * 0.5, distFromRing);
    float outerMask = glowFalloff(distFromRing, thickness * 2.0);
    
    return (coreMask * 0.9 + innerMask * 0.5 + outerMask * 0.3) * intensity;
}

void main() {
    vec4 sceneColor = texture(InSampler, texCoord);
    float rawDepth = texture(DepthSampler, texCoord).r;
    
    // Sky detection
    if (rawDepth > 0.9999) {
        fragColor = sceneColor;
        return;
    }
    
    // Linearize depth
    float near = 0.05;
    float far = 256.0;
    float linearDepth = linearizeDepth(rawDepth, near, far);
    
    // ═══════════════════════════════════════════════════════════════════════
    // CALCULATE DISTANCE TO RING ORIGIN
    // ═══════════════════════════════════════════════════════════════════════
    
    float distanceFromOrigin;
    
    if (UseWorldOrigin > 0.5) {
        // WORLD-ANCHORED MODE: Calculate distance from target world position
        vec3 worldPos = reconstructWorldPos(texCoord, linearDepth);
        vec3 targetPos = vec3(TargetX, TargetY, TargetZ);
        distanceFromOrigin = length(worldPos - targetPos);
    } else {
        // CAMERA MODE: Distance is just the linear depth
        distanceFromOrigin = linearDepth;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // MULTI-RING CALCULATION
    // ═══════════════════════════════════════════════════════════════════════
    
    float totalMask = 0.0;
    int ringCountInt = clamp(int(RingCount), 1, 10);
    float spacing = max(1.0, RingSpacing);
    
    for (int i = 0; i < ringCountInt; i++) {
        float ringOffset = float(i) * spacing;
        float thisRingRadius = RingRadius - ringOffset;
        
        if (thisRingRadius <= 0.0) continue;
        
        float ringFade = 1.0 - (float(i) / float(ringCountInt)) * 0.7;
        totalMask += ringContribution(distanceFromOrigin, thisRingRadius, RingThickness, Intensity * ringFade);
    }
    
    totalMask = clamp(totalMask, 0.0, 1.0);
    
    // ═══════════════════════════════════════════════════════════════════════
    // COLOR COMPOSITION
    // ═══════════════════════════════════════════════════════════════════════
    
    // Start with scene color
    vec3 baseColor = sceneColor.rgb;
    
    // Apply color tint
    if (TintAmount > 0.0) {
        vec3 tintColor = vec3(TintR, TintG, TintB);
        baseColor = mix(baseColor, baseColor * tintColor, TintAmount);
    }
    
    // Apply vignette
    if (VignetteAmount > 0.0) {
        vec2 uv = texCoord * 2.0 - 1.0;
        float vignette = 1.0 - smoothstep(VignetteRadius, 1.0, length(uv));
        baseColor *= mix(1.0, vignette, VignetteAmount);
    }
    
    // Apply blackout
    if (BlackoutAmount > 0.0) {
        baseColor *= (1.0 - BlackoutAmount);
    }
    
    // Ring colors
    vec3 coreColor = vec3(1.0, 1.0, 1.0);
    vec3 ringColor = vec3(0.0, 1.0, 1.0);
    vec3 outerGlow = vec3(0.2, 0.5, 1.0);
    
    vec3 effectColor = mix(outerGlow, ringColor, totalMask);
    effectColor = mix(effectColor, coreColor, totalMask * totalMask);
    
    // Composite rings on top of processed scene
    vec3 finalColor = mix(baseColor, effectColor, totalMask);
    finalColor += ringColor * totalMask * 0.2;
    
    fragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
}
