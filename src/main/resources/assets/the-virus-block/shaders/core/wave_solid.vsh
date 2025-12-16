#version 150

#moj_import <minecraft:fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

// Wave deformation uniforms
uniform float WaveAmplitude;
uniform float WaveFrequency;
uniform float WaveSpeed;
uniform float WaveTime;
uniform int WaveDirection;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec2 texCoord0;
out vec3 normal;

// Apply wave deformation to vertex position
vec3 applyWave(vec3 pos) {
    // Skip if amplitude is negligible
    if (WaveAmplitude < 0.001) return pos;
    
    // Calculate phase based on wave direction
    float phase;
    if (WaveDirection == 0) {        // X direction
        phase = pos.x * WaveFrequency;
    } else if (WaveDirection == 1) { // Y direction
        phase = pos.y * WaveFrequency;
    } else {                         // Z direction
        phase = pos.z * WaveFrequency;
    }
    
    // Use modulo to prevent overflow (matches CPU implementation)
    float safeTime = mod(WaveTime, 1000.0);
    phase += safeTime * WaveSpeed;
    float disp = sin(phase) * WaveAmplitude;
    
    // Apply radial displacement perpendicular to wave direction
    if (WaveDirection == 1) { // Y - radial displacement in XZ plane
        float dist = length(pos.xz);
        if (dist > 0.001) {
            vec2 radialDir = normalize(pos.xz);
            pos.x += radialDir.x * disp;
            pos.z += radialDir.y * disp;
        }
    } else if (WaveDirection == 0) { // X - radial displacement in YZ plane
        float dist = length(pos.yz);
        if (dist > 0.001) {
            vec2 radialDir = normalize(pos.yz);
            pos.y += radialDir.x * disp;
            pos.z += radialDir.y * disp;
        }
    } else { // Z - radial displacement in XY plane
        float dist = length(pos.xy);
        if (dist > 0.001) {
            vec2 radialDir = normalize(pos.xy);
            pos.x += radialDir.x * disp;
            pos.y += radialDir.y * disp;
        }
    }
    
    return pos;
}

void main() {
    // Apply wave deformation to position
    vec3 wavePos = applyWave(Position);
    
    // Standard transformation
    gl_Position = ProjMat * ModelViewMat * vec4(wavePos, 1.0);
    
    // Fog distance calculation uses deformed position
    vertexDistance = fog_distance(wavePos, FogShape);
    
    // Pass through vertex color
    vertexColor = Color;
    
    // Light map from overlay texture
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    
    // Texture coordinates
    texCoord0 = UV0;
    
    // Pass normal (could be recalculated for proper lighting, but keeping simple for now)
    normal = Normal;
}
