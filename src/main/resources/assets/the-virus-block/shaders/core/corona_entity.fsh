#version 150

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

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
    // Corona effect - HARDCODED for now to prove concept works
    // TODO: Make these dynamic via UBO once binding is fixed
    vec3 coronaColor = vec3(1.0, 0.5, 0.2);  // Orange glow
    float coronaPower = 2.0;
    float coronaIntensity = 1.5;
    float coronaFalloff = 0.5;
    
    // Corona only outputs rim glow, not base texture
    float fresnel = pow(1.0 - max(dot(vNormal, vViewDir), 0.0), coronaPower);
    float glow = fresnel * coronaIntensity;
    
    // Apply falloff to control spread
    glow *= exp(-coronaFalloff * (1.0 - fresnel));
    
    vec3 coronaRGB = coronaColor * glow;
    
    // Output with alpha for additive blending
    fragColor = vec4(coronaRGB, glow);
}
