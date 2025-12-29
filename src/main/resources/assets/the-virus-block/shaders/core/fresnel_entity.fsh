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
    vec4 color = texture(Sampler0, texCoord0);
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
#endif
    color *= vertexColor * ColorModulator;
#ifndef NO_OVERLAY
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
#endif
#ifndef EMISSIVE
    color *= lightMapColor;
#endif

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
