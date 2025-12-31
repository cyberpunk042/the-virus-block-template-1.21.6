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
    float GlowWidth;        // Glow falloff width (blocks)
    
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
    
    // vec4 8: Custom ring color
    float RingR;            // Ring color red (0-1)
    float RingG;            // Ring color green (0-1)
    float RingB;            // Ring color blue (0-1)
    float RingOpacity;      // Ring opacity (0-1)
    
    // vec4 9: Shape configuration
    float ShapeType;        // 0=point, 1=sphere, 2=torus, 3=polygon, 4=orbital
    float ShapeRadius;      // Main radius for sphere/polygon
    float ShapeMajorR;      // Torus major radius
    float ShapeMinorR;      // Torus minor radius
    
    // vec4 10: Shape extras (polygon sides / orbital params)
    float ShapeSideCount;   // Polygon sides OR orbital count
    float OrbitDistance;    // Distance from center to orbital centers
    float OrbitalPhase;     // Current orbital rotation angle (radians)
    float ShapeReserved;    // Reserved
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
    
    // Compute camera's LOCAL right and up vectors
    // Note: Java side clamps pitch to ±89° to prevent exact gimbal lock
    vec3 worldUp = vec3(0.0, 1.0, 0.0);
    
    // Standard cross product - works because pitch is clamped
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
    // World position = camera + direction * distance
    // linearDepth is Z-distance (along forward), convert to ray-distance
    float rayDistance = linearDepth / dot(rayDir, forward);
    return camPos + rayDir * rayDistance;
}

// ═══════════════════════════════════════════════════════════════════════════
// SDF LIBRARY - Distance functions for all shape types
// ═══════════════════════════════════════════════════════════════════════════

#define SHAPE_POINT    0
#define SHAPE_SPHERE   1
#define SHAPE_TORUS    2
#define SHAPE_POLYGON  3
#define SHAPE_ORBITAL  4

// Point/Sphere: distance from center (POINT has radius=0, SPHERE has radius>0)
float sdfSphere(vec3 p, vec3 center, float radius) {
    return length(p - center) - radius;
}

// Torus: donut shape in XZ plane
float sdfTorus(vec3 p, vec3 center, float majorR, float minorR) {
    vec3 q = p - center;
    vec2 t = vec2(length(q.xz) - majorR, q.y);
    return length(t) - minorR;
}

// Polygon: n-sided shape in XZ plane (distance to edges)
float sdfPolygon(vec2 p, int sides, float radius) {
    float angle = atan(p.y, p.x);
    float segmentAngle = 6.28318 / float(sides);
    float d = cos(floor(0.5 + angle / segmentAngle) * segmentAngle - angle) * length(p);
    return d - radius;
}

// Compute orbital position around center in XZ plane
vec3 getOrbitalPosition(vec3 center, int index, int count, float distance, float phase) {
    int safeCount = max(1, count);  // Prevent division by zero
    float angle = phase + (float(index) / float(safeCount)) * 6.28318;
    return center + vec3(cos(angle) * distance, 0.0, sin(angle) * distance);
}

// Orbital system: main sphere + N orbiting spheres
// Returns minimum distance to any surface
float sdfOrbitalSystem(vec3 p, vec3 center, float mainRadius, float orbitalRadius,
                       float orbitDistance, int count, float phase) {
    // Distance to main sphere
    float d = length(p - center) - mainRadius;
    
    // Union with each orbital sphere
    for (int i = 0; i < count && i < 8; i++) {
        vec3 orbPos = getOrbitalPosition(center, i, count, orbitDistance, phase);
        d = min(d, length(p - orbPos) - orbitalRadius);
    }
    return d;
}

// Main dispatch - returns distance from worldPos to the shape surface
float getShapeDistance(vec3 worldPos, vec3 shapeCenter) {
    int shapeType = int(ShapeType);
    
    if (shapeType == SHAPE_POINT) {
        // Point: just distance to center (no surface offset)
        return length(worldPos - shapeCenter);
    }
    else if (shapeType == SHAPE_SPHERE) {
        return sdfSphere(worldPos, shapeCenter, ShapeRadius);
    }
    else if (shapeType == SHAPE_TORUS) {
        return sdfTorus(worldPos, shapeCenter, ShapeMajorR, ShapeMinorR);
    }
    else if (shapeType == SHAPE_POLYGON) {
        // Project to XZ plane relative to center
        vec2 planar = worldPos.xz - shapeCenter.xz;
        return sdfPolygon(planar, int(ShapeSideCount), ShapeRadius);
    }
    else if (shapeType == SHAPE_ORBITAL) {
        // Main sphere (ShapeRadius) + orbiting spheres (ShapeMinorR)
        // ShapeSideCount = orbital count, OrbitDistance = distance from center
        return sdfOrbitalSystem(worldPos, shapeCenter, ShapeRadius, ShapeMinorR,
                                OrbitDistance, int(ShapeSideCount), OrbitalPhase);
    }
    
    // Fallback: point distance
    return length(worldPos - shapeCenter);
}

// ═══════════════════════════════════════════════════════════════════════════
// RAYMARCHING - Render solid shapes with corona effect
// ═══════════════════════════════════════════════════════════════════════════

#define MAX_RAYMARCH_STEPS 48
#define RAYMARCH_EPSILON 0.05
#define CORONA_WIDTH 2.0

// SDF for orbital spheres only (for raymarching the visible orbs)
float sdfOrbitalSpheresOnly(vec3 p, vec3 center, float orbitalRadius,
                            float orbitDistance, int count, float phase) {
    float d = 1e10;  // Start far away
    for (int i = 0; i < count && i < 8; i++) {
        vec3 orbPos = getOrbitalPosition(center, i, count, orbitDistance, phase);
        d = min(d, length(p - orbPos) - orbitalRadius);
    }
    return d;
}

// Calculate surface normal from SDF gradient
vec3 calcNormal(vec3 p, vec3 center, float orbitalRadius, 
                float orbitDistance, int count, float phase) {
    float eps = 0.01;
    float d = sdfOrbitalSpheresOnly(p, center, orbitalRadius, orbitDistance, count, phase);
    vec3 grad = vec3(
        sdfOrbitalSpheresOnly(p + vec3(eps,0,0), center, orbitalRadius, orbitDistance, count, phase) - d,
        sdfOrbitalSpheresOnly(p + vec3(0,eps,0), center, orbitalRadius, orbitDistance, count, phase) - d,
        sdfOrbitalSpheresOnly(p + vec3(0,0,eps), center, orbitalRadius, orbitDistance, count, phase) - d
    );
    float len = length(grad);
    return len > 0.0001 ? grad / len : vec3(0.0, 1.0, 0.0);  // Fallback to up if zero gradient
}

// Raymarch to find orbital sphere hit
// Returns: vec4(hitDist, rimAmount, 0, 0) - hitDist < 0 means no hit
vec4 raymarchOrbitalSpheres(vec3 rayOrigin, vec3 rayDir, float maxDist,
                            vec3 center, float orbitalRadius, float orbitDistance,
                            int count, float phase) {
    float t = 0.0;
    
    for (int i = 0; i < MAX_RAYMARCH_STEPS; i++) {
        vec3 p = rayOrigin + rayDir * t;
        float d = sdfOrbitalSpheresOnly(p, center, orbitalRadius, orbitDistance, count, phase);
        
        // Hit surface
        if (d < RAYMARCH_EPSILON) {
            // Calculate rim/corona based on view angle to normal
            vec3 normal = calcNormal(p, center, orbitalRadius, orbitDistance, count, phase);
            float rim = 1.0 - abs(dot(normal, -rayDir));
            rim = pow(rim, 2.0);  // Sharpen the rim
            return vec4(t, rim, 0.0, 1.0);
        }
        
        // Too far
        if (t > maxDist) break;
        
        t += d * 0.8;  // Step forward (0.8 for safety)
    }
    
    // Check if we're NEAR an orbital sphere even without hitting (for corona glow)
    float nearestDist = sdfOrbitalSpheresOnly(rayOrigin + rayDir * min(t, maxDist * 0.5), 
                                              center, orbitalRadius, orbitDistance, count, phase);
    if (nearestDist < CORONA_WIDTH) {
        float coronaAmount = 1.0 - (nearestDist / CORONA_WIDTH);
        return vec4(-1.0, coronaAmount * 0.5, 0.0, 0.0);  // No hit, but corona glow
    }
    
    return vec4(-1.0, 0.0, 0.0, 0.0);  // No hit
}

// ═══════════════════════════════════════════════════════════════════════════
// GLOW FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

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

void main() {
    vec4 sceneColor = texture(InSampler, texCoord);
    float rawDepth = texture(DepthSampler, texCoord).r;
    bool isSky = (rawDepth > 0.9999);
    
    // ═══════════════════════════════════════════════════════════════════════
    // SCREEN EFFECTS - Apply to ALL pixels including sky
    // ═══════════════════════════════════════════════════════════════════════
    
    vec3 baseColor = sceneColor.rgb;
    
    // Apply color tint (multiplicative blend)
    if (TintAmount > 0.001) {
        vec3 tintColor = vec3(TintR, TintG, TintB);
        baseColor = mix(baseColor, baseColor * tintColor, TintAmount);
    }
    
    // Apply vignette
    if (VignetteAmount > 0.001) {
        vec2 uv = texCoord * 2.0 - 1.0;
        float dist = length(uv);
        float vignette = 1.0 - smoothstep(VignetteRadius, 1.0 + VignetteRadius, dist);
        baseColor *= mix(1.0, vignette, VignetteAmount);
    }
    
    // Apply blackout (darken everything)
    if (BlackoutAmount > 0.001) {
        baseColor *= (1.0 - BlackoutAmount);
    }
    
    // For sky pixels, output now (no rings on sky)
    if (isSky) {
        fragColor = vec4(baseColor, 1.0);
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
        // Use SDF library for shape-aware distance calculation
        distanceFromOrigin = getShapeDistance(worldPos, targetPos);
    } else {
        // CAMERA MODE: Distance is just the linear depth (always point-based)
        distanceFromOrigin = linearDepth;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // ORBITAL SPHERE RAYMARCHING - Render solid black spheres with corona
    // ═══════════════════════════════════════════════════════════════════════
    
    vec3 orbitalColor = vec3(0.0);
    float orbitalAlpha = 0.0;
    
    if (int(ShapeType) == SHAPE_ORBITAL && UseWorldOrigin > 0.5) {
        // Get camera info for raymarching
        vec3 camPos = vec3(CameraX, CameraY, CameraZ);
        vec3 rawForward = vec3(ForwardX, ForwardY, ForwardZ);
        float fwdLen = length(rawForward);
        
        // Skip raymarching if we have invalid data
        if (fwdLen > 0.001 && OrbitDistance > 0.1 && ShapeSideCount > 0.5) {
            vec3 forward = rawForward / fwdLen;
            vec3 targetPos = vec3(TargetX, TargetY, TargetZ);
            
            // Reconstruct ray direction for this pixel
            vec2 ndc = texCoord * 2.0 - 1.0;
            float halfHeight = tan(Fov * 0.5);
            float halfWidth = halfHeight * AspectRatio;
            vec3 worldUp = vec3(0.0, 1.0, 0.0);
            
            // Handle case where forward is parallel to worldUp
            vec3 right = cross(forward, worldUp);
            float rightLen = length(right);
            if (rightLen < 0.001) {
                right = vec3(1.0, 0.0, 0.0);
            } else {
                right = right / rightLen;
            }
            vec3 up = cross(right, forward);
            vec3 rayDir = normalize(forward + right * (ndc.x * halfWidth) + up * (ndc.y * halfHeight));
            
            // Raymarch for orbital spheres
            vec4 hitInfo = raymarchOrbitalSpheres(
                camPos, rayDir, linearDepth + 10.0,
                targetPos, ShapeMinorR, OrbitDistance,
                int(ShapeSideCount), OrbitalPhase
            );
        
            if (hitInfo.w > 0.5) {
                // HIT! - Draw black sphere with rim corona
                float rimAmount = hitInfo.y;
                vec3 coronaColor = vec3(RingR, RingG, RingB);  // Use ring color for corona
                
                // Black center with colored rim
                orbitalColor = coronaColor * rimAmount * 2.0;
                orbitalAlpha = 1.0;
                
                // Override base color with black (sphere body)
                baseColor = mix(vec3(0.0), coronaColor, rimAmount * 0.8);
            } else if (hitInfo.y > 0.01) {
                // Near miss - add corona glow
                vec3 coronaColor = vec3(RingR, RingG, RingB);
                orbitalColor = coronaColor * hitInfo.y;
                orbitalAlpha = hitInfo.y;
            }
        }  // end safeguard if
    }  // end SHAPE_ORBITAL if
    
    // ═══════════════════════════════════════════════════════════════════════
    // MULTI-RING CALCULATION
    // ═══════════════════════════════════════════════════════════════════════
    
    float totalMask = 0.0;
    int ringCountInt = clamp(int(RingCount), 1, 10);
    float spacing = max(1.0, RingSpacing);
    float glowW = max(1.0, GlowWidth);  // Glow width from uniform
    
    for (int i = 0; i < ringCountInt; i++) {
        float ringOffset = float(i) * spacing;
        float thisRingRadius = RingRadius - ringOffset;
        
        if (thisRingRadius <= 0.0) continue;
        
        float ringFade = 1.0 - (float(i) / float(ringCountInt)) * 0.7;
        totalMask += ringContribution(distanceFromOrigin, thisRingRadius, RingThickness, glowW, Intensity * ringFade);
    }
    
    totalMask = clamp(totalMask, 0.0, 1.0);
    
    // ═══════════════════════════════════════════════════════════════════════
    // COLOR COMPOSITION
    // (Screen effects already applied to baseColor above)
    // ═══════════════════════════════════════════════════════════════════════
    
    // Ring colors - use custom colors from uniforms
    vec3 ringColor = vec3(RingR, RingG, RingB);
    vec3 coreColor = vec3(1.0, 1.0, 1.0);  // Core is always white-hot
    vec3 outerGlow = ringColor * 0.5;      // Outer glow is dimmer version
    
    vec3 effectColor = mix(outerGlow, ringColor, totalMask);
    effectColor = mix(effectColor, coreColor, totalMask * totalMask);
    
    // Apply ring opacity
    float ringAlpha = totalMask * RingOpacity;
    
    // Composite rings on top of processed scene
    vec3 finalColor = mix(baseColor, effectColor, ringAlpha);
    finalColor += ringColor * ringAlpha * 0.2;  // Additive bloom
    
    // Add orbital sphere corona glow
    if (orbitalAlpha > 0.01) {
        finalColor += orbitalColor * orbitalAlpha;
    }
    
    fragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
}
