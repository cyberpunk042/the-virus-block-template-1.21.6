#version 150

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

// Custom UBO for Corona parameters (bound by CustomUniformBinder via Mixin)
layout(std140) uniform CoronaParams {
    vec4 CoronaColorAndPower;      // xyz = CoronaColor, w = CoronaPower
    vec4 CoronaIntensityFalloff;   // x = CoronaIntensity, y = CoronaFalloff, zw = padding
    vec4 CoronaOffsetWidthPad;     // x = Offset, y = Width, zw = padding
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
    // Extract Corona params from UBO
    vec3 coronaColor = CoronaColorAndPower.xyz;
    float coronaPower = CoronaColorAndPower.w;
    float coronaIntensity = CoronaIntensityFalloff.x;
    float coronaFalloff = CoronaIntensityFalloff.y;
    float coronaOffset = CoronaOffsetWidthPad.x;
    float coronaWidth = CoronaOffsetWidthPad.y;
    
    // Corona only outputs rim glow, not base texture
    // Use abs() to handle backfaces correctly
    float NdotV = abs(dot(vNormal, vViewDir));
    
    // Apply offset: shifts the fresnel curve center
    // Offset > 0 makes glow appear more towards edges
    // Offset < 0 makes glow appear more towards center
    float adjustedNdotV = clamp(NdotV + coronaOffset, 0.0, 1.0);
    
    // Base fresnel calculation
    float fresnel = pow(1.0 - adjustedNdotV, coronaPower);
    
    // Apply width: controls the band spread of the glow
    // Width < 1 makes the glow narrower/sharper
    // Width > 1 makes the glow wider/softer
    fresnel = smoothstep(0.0, coronaWidth, fresnel);
    
    float glow = fresnel * coronaIntensity;
    
    // Apply falloff to control spread
    glow *= exp(-coronaFalloff * (1.0 - fresnel));
    
    vec3 coronaRGB = coronaColor * glow;
    
    // Output with alpha for additive blending
    fragColor = vec4(coronaRGB, glow);
}
