#version 150

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

// Custom UBO for Fresnel parameters (bound by CustomUniformBinder via Mixin)
layout(std140) uniform FresnelParams {
    vec4 RimColorAndPower;      // xyz = RimColor, w = RimPower
    vec4 RimIntensityAndPad;    // x = RimIntensity, yzw = padding
};

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec3 vNormal;
in vec3 vViewDir;

out vec4 fragColor;

void main() {
    // Start with vertex color directly (ignore texture when no texture is bound)
    // Sampler0 may not have a valid texture, so use vertexColor as base
    vec4 color = vertexColor * ColorModulator;
    
// Skip overlay - we don't use it for field geometry
// Skip lightmap - we use full bright (0xF000F0)
// These checks caused black output when samplers weren't properly bound

    // Extract Fresnel params from UBO
    vec3 rimColor = RimColorAndPower.xyz;
    float rimPower = RimColorAndPower.w;
    float rimIntensity = RimIntensityAndPad.x;
    
    // Fresnel rim effect
    // Use abs() to handle backfaces - they'll have negative dot product
    float NdotV = abs(dot(vNormal, vViewDir));
    float fresnel = pow(1.0 - NdotV, rimPower);
    vec3 rimContribution = rimColor * fresnel * rimIntensity;
    color.rgb += rimContribution;

    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
