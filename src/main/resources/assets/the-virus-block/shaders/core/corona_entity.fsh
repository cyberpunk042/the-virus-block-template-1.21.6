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
    // Extract Corona params from UBO (may be zero if not bound!)
    vec3 coronaColor = CoronaColorAndPower.xyz;
    float coronaPower = CoronaColorAndPower.w;
    float coronaIntensity = CoronaIntensityFalloff.x;
    float coronaFalloff = CoronaIntensityFalloff.y;
    float coronaWidth = CoronaOffsetWidthPad.y;
    
    // DEBUG: Check if UBO is bound (all zeros = not bound)
    // If not bound, use hardcoded visible values
    if (coronaIntensity < 0.001 && coronaPower < 0.001) {
        // UBO NOT BOUND - use bright cyan debug color
        coronaColor = vec3(0.0, 1.0, 1.0);  // Cyan
        coronaPower = 2.0;
        coronaIntensity = 2.0;  // VERY bright
        coronaFalloff = 0.3;
        coronaWidth = 1.0;
    }
    
    // Calculate edge factor - now this shell is DISPLACED outward
    float NdotV = abs(dot(vNormal, vViewDir));
    float edgeFactor = 1.0 - NdotV;
    
    // Apply power for sharpness control
    float rim = pow(edgeFactor, coronaPower);
    
    // Apply width
    rim = pow(rim, 1.0 / max(coronaWidth, 0.1));
    
    // Apply intensity
    float glow = rim * coronaIntensity;
    
    // Apply falloff for soft fade
    glow *= exp(-coronaFalloff * (1.0 - rim));
    
    // Output corona color
    vec3 coronaRGB = coronaColor * glow;
    
    // For additive blending
    fragColor = vec4(coronaRGB, glow);
}

