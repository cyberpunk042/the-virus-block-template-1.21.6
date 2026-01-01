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
    float BeamHeight;       // Current beam height (0 = no beam, grows when active)
    
    // vec4 11: Corona/blend config
    float CoronaWidth;      // Glow spread (blocks)
    float CoronaIntensity;  // Brightness multiplier
    float RimPower;         // Rim sharpness (1-5)
    float BlendRadius;      // Smooth min blend (0=sharp, 5+=unified)
    
    // vec4 12: Orbital body color (RGB) + rim falloff
    float OrbitalBodyR;
    float OrbitalBodyG;
    float OrbitalBodyB;
    float OrbitalRimFalloff;
    
    // vec4 13: Orbital corona color (RGBA)
    float OrbitalCoronaR;
    float OrbitalCoronaG;
    float OrbitalCoronaB;
    float OrbitalCoronaA;
    
    // vec4 14: Beam body color (RGB) + beam width scale
    float BeamBodyR;
    float BeamBodyG;
    float BeamBodyB;
    float BeamWidthScale;
    
    // vec4 15: Beam corona color (RGBA)
    float BeamCoronaR;
    float BeamCoronaG;
    float BeamCoronaB;
    float BeamCoronaA;
    
    // vec4 16: Beam geometry extras
    float BeamWidthAbs;     // Absolute width (blocks), 0 = use scale
    float BeamTaper;        // Taper factor (1 = uniform, <1 = narrow top)
    float RetractDelay;     // ms delay after beam shrinks before retract
    float CombinedMode;     // 0 = individual orbital sources, 1 = combined shockwave from center
    
    // vec4 17: Beam corona settings (separate from orbital)
    float BeamCoronaWidth;
    float BeamCoronaIntensity;
    float BeamRimPower;
    float BeamRimFalloff;
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

// ═══════════════════════════════════════════════════════════════════════════
// SDF COMBINATION OPERATIONS (from Ronja's tutorial / hg_sdf library)
// These properly handle inside/outside/boundary for correct isodistance contours
// ═══════════════════════════════════════════════════════════════════════════

// Basic merge (union) - hard minimum
float sdf_merge(float shape1, float shape2) {
    return min(shape1, shape2);
}

// Basic intersect - hard maximum
float sdf_intersect(float shape1, float shape2) {
    return max(shape1, shape2);
}

// Smooth merge (round union) - creates smooth blend at intersection
// This is the PROPER way to combine SDFs for flower patterns!
// radius = blend zone size (0 = sharp corners, larger = more rounded)
float round_merge(float shape1, float shape2, float radius) {
    if (radius < 0.001) return sdf_merge(shape1, shape2);
    
    // Grow shapes by radius, then compute smooth inside distance
    vec2 intersectionSpace = vec2(shape1 - radius, shape2 - radius);
    intersectionSpace = min(intersectionSpace, 0.0);
    float insideDistance = -length(intersectionSpace);
    
    // Compute outside distance with proper handling
    float simpleUnion = sdf_merge(shape1, shape2);
    float outsideDistance = max(simpleUnion, radius);
    
    // Combine for complete SDF
    return insideDistance + outsideDistance;
}

// Smooth intersect - rounded intersection
float round_intersect(float shape1, float shape2, float radius) {
    if (radius < 0.001) return sdf_intersect(shape1, shape2);
    
    vec2 intersectionSpace = vec2(shape1 + radius, shape2 + radius);
    intersectionSpace = max(intersectionSpace, 0.0);
    float outsideDistance = length(intersectionSpace);
    
    float simpleIntersection = sdf_intersect(shape1, shape2);
    float insideDistance = min(simpleIntersection, -radius);
    
    return outsideDistance + insideDistance;
}

// Champfer merge - creates beveled/chamfered corners instead of rounded
float champfer_merge(float shape1, float shape2, float champferSize) {
    const float SQRT_05 = 0.70710678118;
    float simpleMerge = sdf_merge(shape1, shape2);
    float champfer = (shape1 + shape2) * SQRT_05;
    champfer = champfer - champferSize;
    return sdf_merge(simpleMerge, champfer);
}

// Legacy polynomial smooth min (kept for reference/comparison)
float smin(float a, float b, float k) {
    if (k < 0.001) return min(a, b);
    float h = clamp(0.5 + 0.5*(b-a)/k, 0.0, 1.0);
    return mix(b, a, h) - k*h*(1.0-h);
}

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
// mainRadius > 0.1: Include center sphere in SDF (affects flower shape)
// mainRadius ≈ 0: Orbitals-only (pure flower/petal pattern)
// BlendRadius > 0: smin (outward bulge, petals merge smoothly)
// BlendRadius = 0: hard union (discrete orbital circles)
// BlendRadius < 0: legacy mode (bidirectional waves)

// 2D Orbital system - computes SDF in XZ plane only (like Ronja's tutorial)
// This produces proper flower-shaped isodistance contours!
float sdfOrbitalSystem2D(vec2 p, vec2 center, float mainRadius, float orbitalRadius,
                         float orbitDistance, int count, float phase) {
    
    // Start with either main circle or first orbital
    float combined;
    bool hasMainCircle = mainRadius > 0.1;
    
    if (hasMainCircle) {
        // Start with main circle
        combined = length(p - center) - mainRadius;
    } else {
        // Start with first orbital (no main circle)
        int safeCount = max(1, count);
        float angle = phase + (0.0 / float(safeCount)) * 6.28318;
        vec2 firstOrbPos = center + vec2(cos(angle), sin(angle)) * orbitDistance;
        combined = length(p - firstOrbPos) - orbitalRadius;
    }
    
    // Determine which combination function to use
    bool useRoundMerge = BlendRadius > 0.001;
    
    // Combine all orbitals into the system
    int startIdx = hasMainCircle ? 0 : 1;
    int safeCount = max(1, count);
    for (int i = startIdx; i < count && i < 32; i++) {
        float angle = phase + (float(i) / float(safeCount)) * 6.28318;
        vec2 orbPos = center + vec2(cos(angle), sin(angle)) * orbitDistance;
        float orbDist = length(p - orbPos) - orbitalRadius;
        
        if (useRoundMerge) {
            // Proper smooth union - creates correct flower contours!
            combined = round_merge(combined, orbDist, BlendRadius);
        } else {
            // Hard union (BlendRadius = 0 or negative)
            combined = sdf_merge(combined, orbDist);
        }
    }
    
    return combined;
}

// COMBINED MODE: Distance from center, with flower-shaped radial modulation
// The center is the SOURCE. Orbitals define the shape of the expanding wave.
// This creates a single unified shockwave that follows petal curves.
// 
// KEY INSIGHT: To preserve flower shape at ALL distances, we compute a "normalized"
// distance where 1.0 = on the flower boundary. The RingRadius then scales this.
float sdfCombinedFlower2D(vec2 p, vec2 center, float mainRadius, float orbitalRadius,
                          float orbitDistance, int count, float phase) {
    
    vec2 rel = p - center;
    float distFromCenter = length(rel);
    
    // Handle center point
    if (distFromCenter < 0.001) {
        return -(orbitDistance + orbitalRadius);  // Deeply inside
    }
    
    float angle = atan(rel.y, rel.x);
    
    // Compute angular distance to nearest petal (orbital)
    // Orbitals are at angles: phase, phase + 2π/count, phase + 4π/count, ...
    float angularPeriod = 6.28318 / float(max(1, count));
    float relativeAngle = angle - phase;
    
    // Find distance to nearest petal center (wraps around)
    float nearestPetalAngle = round(relativeAngle / angularPeriod) * angularPeriod;
    float angularDistToPetal = abs(relativeAngle - nearestPetalAngle);
    
    // Normalize to [0, 1]: 0 = at petal center, 1 = between petals
    float normalizedAngularDist = angularDistToPetal / (angularPeriod * 0.5);
    normalizedAngularDist = clamp(normalizedAngularDist, 0.0, 1.0);
    
    // Compute flower radius at this angle
    // At petal (orbital center): radius = orbitDistance + orbitalRadius (outer edge of orbital)
    // Between petals: radius = innerRadius (either mainRadius or valley depth)
    float outerRadius = orbitDistance + orbitalRadius;  // Petal tip
    float innerRadius = mainRadius > 0.1 ? mainRadius : max(0.1, orbitDistance - orbitalRadius);  // Valley
    
    // Smooth interpolation from outer (petal) to inner (valley)
    float t = normalizedAngularDist;
    if (BlendRadius > 0.001) {
        // Smoother transition with blend
        t = smoothstep(0.0, 1.0, t);
    }
    float flowerRadius = mix(outerRadius, innerRadius, t);
    
    // PRESERVE FLOWER SHAPE: Instead of simple distance-to-boundary,
    // compute normalized distance where 1.0 = on the flower surface
    // This way, as RingRadius grows, the shape stays consistent
    float normalizedDist = distFromCenter / flowerRadius;
    
    // Convert back to "SDF-like" value for compatibility with ring system
    // Ring appears where: normalizedDist * outerRadius == RingRadius
    // So we return: (normalizedDist - 1.0) * flowerRadius
    // At boundary: (1-1)*R = 0, Inside: negative, Outside: positive
    return (normalizedDist - 1.0) * outerRadius;
}

// 3D wrapper - projects to XZ plane for flower-shaped shockwave contours
// The Y difference from center adds to the distance for proper 3D falloff
float sdfOrbitalSystem(vec3 p, vec3 center, float mainRadius, float orbitalRadius,
                       float orbitDistance, int count, float phase) {
    
    vec2 p2D = p.xz;
    vec2 center2D = center.xz;
    float dist2D;
    
    // CombinedMode > 0.5 = Combined shockwave from center (flower-shaped)
    // CombinedMode <= 0.5 = Individual orbital sources (legacy SDF union)
    if (CombinedMode > 0.5) {
        // COMBINED MODE: Single shockwave from center, shaped like a flower
        dist2D = sdfCombinedFlower2D(p2D, center2D, mainRadius, orbitalRadius,
                                      orbitDistance, count, phase);
    } else {
        // INDIVIDUAL MODE: Each orbital is its own source (legacy behavior)
        dist2D = sdfOrbitalSystem2D(p2D, center2D, mainRadius, orbitalRadius,
                                     orbitDistance, count, phase);
    }
    
    // Add Y-distance contribution for proper 3D behavior
    float yDiff = abs(p.y - center.y);
    
    if (dist2D > 0.0) {
        // Outside the shape - add 3D falloff
        return sqrt(dist2D * dist2D + yDiff * yDiff);
    } else {
        // Inside the shape - adjust for Y distance
        return dist2D + yDiff;
    }
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
        float rawDist = sdfOrbitalSystem(worldPos, shapeCenter, ShapeRadius, ShapeMinorR,
                                OrbitDistance, int(ShapeSideCount), OrbitalPhase);
        
        // OUTWARD-ONLY WAVE MODE (BlendRadius >= 0):
        // Clamp SDF to max(0, dist) so pixels INSIDE the combined shape
        // return distance=0. This suppresses inward-propagating shockwave rings.
        // The flower pattern still forms because the combined boundary is still computed,
        // but rings only expand OUTWARD from that boundary.
        //
        // LEGACY MODE (BlendRadius < 0):
        // Keep full SDF with negative values inside - waves propagate both directions.
        if (BlendRadius >= 0.0) {
            return max(0.0, rawDist);  // Outward waves only
        } else {
            return rawDist;  // Full bidirectional waves (legacy)
        }
    }
    
    // Fallback: point distance
    return length(worldPos - shapeCenter);
}

// ═══════════════════════════════════════════════════════════════════════════
// RAYMARCHING - Render solid shapes with corona effect
// ═══════════════════════════════════════════════════════════════════════════

#define MAX_RAYMARCH_STEPS 48
#define RAYMARCH_EPSILON 0.05
// CORONA_WIDTH is now a uniform (CoronaWidth)

// SDF for orbital spheres only (for raymarching the visible orbs)
float sdfOrbitalSpheresOnly(vec3 p, vec3 center, float orbitalRadius,
                            float orbitDistance, int count, float phase) {
    float d = 1e10;  // Start far away
    for (int i = 0; i < count && i < 32; i++) {
        vec3 orbPos = getOrbitalPosition(center, i, count, orbitDistance, phase);
        d = min(d, length(p - orbPos) - orbitalRadius);
    }
    return d;
}

// Capsule SDF (line segment with radius) - for beams
float sdfCapsule(vec3 p, vec3 a, vec3 b, float r) {
    vec3 ab = b - a;
    vec3 ap = p - a;
    float t = clamp(dot(ap, ab) / dot(ab, ab), 0.0, 1.0);
    vec3 closest = a + t * ab;
    return length(p - closest) - r;
}

// SDF for tapered capsule (cone-like with caps)
float sdfTaperedCapsule(vec3 p, vec3 a, vec3 b, float rBottom, float rTop) {
    vec3 ab = b - a;
    vec3 ap = p - a;
    float t = clamp(dot(ap, ab) / dot(ab, ab), 0.0, 1.0);
    float radius = mix(rBottom, rTop, t);  // Interpolate radius along beam
    vec3 closest = a + t * ab;
    return length(p - closest) - radius;
}

// SDF for beams from orbitals to sky
float sdfBeams(vec3 p, vec3 center, float orbitalRadius, float orbitDistance,
               int count, float phase, float beamHeight) {
    if (beamHeight < 0.1) return 1e10;  // No beam
    
    float d = 1e10;
    
    // Beam width: use absolute if provided, otherwise scale
    float baseRadius = (BeamWidthAbs > 0.01) ? BeamWidthAbs : (orbitalRadius * BeamWidthScale);
    
    // Taper: 1.0 = uniform, <1 = narrow at top, >1 = wide at top
    float topRadius = baseRadius * BeamTaper;
    
    for (int i = 0; i < count && i < 32; i++) {
        vec3 orbPos = getOrbitalPosition(center, i, count, orbitDistance, phase);
        vec3 beamStart = orbPos;
        vec3 beamEnd = orbPos + vec3(0.0, beamHeight, 0.0);  // Straight up
        d = min(d, sdfTaperedCapsule(p, beamStart, beamEnd, baseRadius, topRadius));
    }
    return d;
}

// Combined SDF for orbitals + beams
float sdfOrbitalAndBeams(vec3 p, vec3 center, float orbitalRadius, float orbitDistance,
                         int count, float phase, float beamHeight) {
    float orbDist = sdfOrbitalSpheresOnly(p, center, orbitalRadius, orbitDistance, count, phase);
    float beamDist = sdfBeams(p, center, orbitalRadius, orbitDistance, count, phase, beamHeight);
    return min(orbDist, beamDist);
}

// Calculate surface normal from SDF gradient (for orbitals + beams)
vec3 calcNormal(vec3 p, vec3 center, float orbitalRadius, 
                float orbitDistance, int count, float phase, float beamHeight) {
    float eps = 0.01;
    float d = sdfOrbitalAndBeams(p, center, orbitalRadius, orbitDistance, count, phase, beamHeight);
    vec3 grad = vec3(
        sdfOrbitalAndBeams(p + vec3(eps,0,0), center, orbitalRadius, orbitDistance, count, phase, beamHeight) - d,
        sdfOrbitalAndBeams(p + vec3(0,eps,0), center, orbitalRadius, orbitDistance, count, phase, beamHeight) - d,
        sdfOrbitalAndBeams(p + vec3(0,0,eps), center, orbitalRadius, orbitDistance, count, phase, beamHeight) - d
    );
    float len = length(grad);
    return len > 0.0001 ? grad / len : vec3(0.0, 1.0, 0.0);  // Fallback to up if zero gradient
}

// Raymarch to find orbital/beam hit
// Returns: vec4(hitDist, rimAmount, hitType, didHit) 
// hitType: 0 = orbital sphere, 1 = beam
// hitDist < 0 means no hit (but may have corona glow in rimAmount)
vec4 raymarchOrbitalSpheres(vec3 rayOrigin, vec3 rayDir, float maxDist,
                            vec3 center, float orbitalRadius, float orbitDistance,
                            int count, float phase, float beamHeight) {
    float t = 0.0;
    
    for (int i = 0; i < MAX_RAYMARCH_STEPS; i++) {
        vec3 p = rayOrigin + rayDir * t;
        float d = sdfOrbitalAndBeams(p, center, orbitalRadius, orbitDistance, count, phase, beamHeight);
        
        // Hit surface
        if (d < RAYMARCH_EPSILON) {
            // Determine if we hit beam or orbital by comparing distances
            float orbDist = sdfOrbitalSpheresOnly(p, center, orbitalRadius, orbitDistance, count, phase);
            float beamDist = sdfBeams(p, center, orbitalRadius, orbitDistance, count, phase, beamHeight);
            float hitType = (beamDist < orbDist) ? 1.0 : 0.0;  // 0=orbital, 1=beam
            
            // Calculate rim/corona based on view angle to normal
            vec3 normal = calcNormal(p, center, orbitalRadius, orbitDistance, count, phase, beamHeight);
            float rim = 1.0 - abs(dot(normal, -rayDir));
            
            // Use different rim power for beams vs orbitals
            float rimPwr = (hitType > 0.5) ? BeamRimPower : RimPower;
            rim = pow(rim, rimPwr);
            
            return vec4(t, rim, hitType, 1.0);
        }
        
        // Too far
        if (t > maxDist) break;
        
        t += d * 0.8;  // Step forward (0.8 for safety)
    }
    
    // Check if we're NEAR an orbital/beam even without hitting (for corona glow)
    float nearestDist = sdfOrbitalAndBeams(rayOrigin + rayDir * min(t, maxDist * 0.5), 
                                           center, orbitalRadius, orbitDistance, count, phase, beamHeight);
    float cWidth = max(0.1, CoronaWidth);  // Use uniform, minimum 0.1 to avoid div by zero
    if (nearestDist < cWidth) {
        float coronaAmount = 1.0 - (nearestDist / cWidth);
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
    
    // For sky pixels with NON-ORBITAL shapes, output now (no rings on sky)
    // BUT for ORBITAL shapes, we need to continue to raymarch the orbitals!
    if (isSky && int(ShapeType) != SHAPE_ORBITAL) {
        fragColor = vec4(baseColor, 1.0);
        return;
    }
    
    // Linearize depth - use large far value to handle fog/distant terrain
    float near = 0.05;
    float far = 1000.0;  // Increased from 256 to handle beams beyond fog
    float linearDepth = linearizeDepth(rawDepth, near, far);
    
    // Detect if we're looking at sky/far distance (depth at or near far plane)
    bool isAtFarPlane = rawDepth > 0.9999;
    
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
            
            // Raymarch for orbital spheres and beams
            // Calculate max possible reach: distance to target + orbit + beam height
            float distToTarget = length(targetPos - camPos);
            float maxEffectReach = distToTarget + OrbitDistance + BeamHeight + 100.0;  // +100 safety margin
            
            // When looking at terrain, limit to terrain depth + margin
            // When looking at sky, use full reach to catch distant beams
            float maxRaymarchDist = isAtFarPlane ? maxEffectReach : max(linearDepth + 50.0, maxEffectReach);
            
            vec4 hitInfo = raymarchOrbitalSpheres(
                camPos, rayDir, maxRaymarchDist,
                targetPos, ShapeMinorR, OrbitDistance,
                int(ShapeSideCount), OrbitalPhase, BeamHeight
            );
        
            if (hitInfo.w > 0.5) {
                // HIT! Check if orbital is in front of scene geometry (depth test)
                float hitDist = hitInfo.x;
                float hitType = hitInfo.z;  // 0=orbital, 1=beam
                
                // Only render if orbital is closer than scene OR we're looking at sky
                bool inFrontOfScene = isAtFarPlane || (hitDist < linearDepth);
                
                if (inFrontOfScene) {
                    // Draw sphere/beam with body color and rim corona
                    float rimAmount = hitInfo.y;
                    
                    // Select colors and alpha based on hit type
                    vec3 bodyColor, coronaColor;
                    float alpha, intensity;
                    
                    if (hitType > 0.5) {
                        // BEAM hit - use beam colors and alpha
                        bodyColor = vec3(BeamBodyR, BeamBodyG, BeamBodyB);
                        coronaColor = vec3(BeamCoronaR, BeamCoronaG, BeamCoronaB);
                        alpha = BeamCoronaA;
                        intensity = BeamCoronaIntensity;
                    } else {
                        // ORBITAL hit - use orbital colors and alpha
                        bodyColor = vec3(OrbitalBodyR, OrbitalBodyG, OrbitalBodyB);
                        coronaColor = vec3(OrbitalCoronaR, OrbitalCoronaG, OrbitalCoronaB);
                        alpha = OrbitalCoronaA;
                        intensity = CoronaIntensity;
                    }
                    
                    // Corona rim effect (additive glow, uses alpha)
                    orbitalColor = coronaColor * rimAmount * 2.0 * intensity;
                    orbitalAlpha = alpha;
                    
                    // Body is SOLID (fully opaque) - not blended with scene
                    // Corona rim adds color at the edges
                    vec3 solidColor = mix(bodyColor, coronaColor, rimAmount * 0.8);
                    baseColor = solidColor;  // Fully replace scene color when hit
                }
            } else if (hitInfo.y > 0.01) {
                // Near miss corona glow - also needs depth check
                // Corona glow should only show if we're at sky or the glow area is in front
                if (isAtFarPlane) {
                    vec3 orbCoronaColor = vec3(OrbitalCoronaR, OrbitalCoronaG, OrbitalCoronaB);
                    orbitalColor = orbCoronaColor * hitInfo.y * CoronaIntensity;
                    orbitalAlpha = hitInfo.y * OrbitalCoronaA;
                }
            }
        }  // end safeguard if
    }  // end SHAPE_ORBITAL if
    
    // ═══════════════════════════════════════════════════════════════════════
    // MULTI-RING CALCULATION
    // ═══════════════════════════════════════════════════════════════════════
    
    float totalMask = 0.0;
    int ringCountInt = clamp(int(RingCount), 1, 50);
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
